/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.auth.oauth.oidc;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.as.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.openid.connect.sdk.op.*;

/**
 * Fetches a Juneau-native {@link OidcMetadata} record describing an authorization server, supporting both the RFC 8414
 * OAuth Authorization Server Metadata endpoint ({@code /.well-known/oauth-authorization-server}) and the OpenID Connect
 * Discovery endpoint ({@code /.well-known/openid-configuration}).
 *
 * <p>
 * {@link #discover()} tries {@code AuthorizationServerMetadata.resolve(...)} (RFC 8414) first and falls back to
 * {@code OIDCProviderMetadata.resolve(...)} (OIDC Discovery); both perform the RFC 8414 / OIDC Discovery issuer identity
 * check (the returned document's {@code issuer} must exactly equal the requested issuer).  The metadata is fetched on
 * each call to {@link #discover()}; the result is not cached internally &mdash; callers cache themselves if needed.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> oidc = OidcDiscoveryClient.<jsm>create</jsm>()
 * 		.issuer(URI.<jsm>create</jsm>(<js>"https://login.example.com/realms/api"</js>))
 * 		.build()
 * 		.discover();
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class OidcDiscoveryClient {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder.
	 */
	public static class Builder {
		private URI issuer;
		private Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the issuer URL.  Required.
		 *
		 * @param value The issuer URL.
		 * @return This object.
		 */
		public Builder issuer(URI value) {
			issuer = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the discovery request.  Default 10 seconds.
		 *
		 * @param value The timeout.  Must not be <jk>null</jk> and must be positive.
		 * @return This object.
		 */
		public Builder httpTimeout(Duration value) {
			assertArgNotNull(ARG_value, value);
			assertArg(!value.isZero() && !value.isNegative(), "httpTimeout must be positive (was %s)", value);
			httpTimeout = value;
			return this;
		}

		/**
		 * Sets the HTTP request configurator.
		 *
		 * @param value The callback.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the client.
		 *
		 * @return A new {@link OidcDiscoveryClient}.
		 */
		public OidcDiscoveryClient build() {
			if (issuer == null)
				throw new IllegalStateException("OidcDiscoveryClient requires issuer(...)");
			return new OidcDiscoveryClient(this);
		}
	}

	/** Default connect/read timeout applied to a discovery request when the caller sets none. */
	static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

	private final URI issuer;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OidcDiscoveryClient(Builder b) {
		this.issuer = b.issuer;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Returns the configured issuer URL.
	 *
	 * @return The issuer URL.
	 */
	public URI getIssuer() {
		return issuer;
	}

	/**
	 * Fetches the authorization server's metadata, trying RFC 8414 first then OIDC Discovery.
	 *
	 * @return The parsed metadata record.
	 * @throws IOException If both discovery HTTP fetches fail.
	 * @throws OidcDiscoveryException If the metadata cannot be parsed or the issuer identity check fails.
	 */
	public OidcMetadata discover() throws IOException, OidcDiscoveryException {
		var iss = new Issuer(issuer.toString());
		HTTPRequestModifier cfg = http -> {
			http.setConnectTimeout((int) httpTimeout.toMillis());
			http.setReadTimeout((int) httpTimeout.toMillis());
			if (httpRequestConfigurator != null)
				httpRequestConfigurator.accept(http);
			return http;
		};
		// RFC 8414 OAuth Authorization Server Metadata first, OIDC Discovery as fallback (spec requires both).
		try {
			return toMetadata(AuthorizationServerMetadata.resolve(iss, null, cfg, false));
		} catch (IOException | GeneralException asFailure) {
			try {
				return toMetadata(OIDCProviderMetadata.resolve(iss, null, cfg, false));
			} catch (GeneralException oidcFailure) {
				throw new OidcDiscoveryException("Failed to resolve authorization-server metadata for " + issuer
					+ " via RFC 8414 or OIDC Discovery", oidcFailure);
			}
		}
	}

	private static OidcMetadata toMetadata(AuthorizationServerMetadata md) {
		var extras = new LinkedHashMap<String,Object>();
		var json = md.toJSONObject();
		for (var e : json.entrySet()) {
			var k = e.getKey();
			if (!STANDARD_FIELDS.contains(k))
				extras.put(k, e.getValue());
		}
		URI userInfo = null;
		URI endSession = null;
		if (md instanceof OIDCProviderMetadata md2) {
			userInfo = md2.getUserInfoEndpointURI();
			endSession = md2.getEndSessionEndpointURI();
		}
		return new OidcMetadata(
			URI.create(md.getIssuer().getValue()),
			md.getTokenEndpointURI(),
			md.getAuthorizationEndpointURI(),
			md.getIntrospectionEndpointURI(),
			md.getJWKSetURI(),
			userInfo,
			endSession,
			toScopeSet(md),
			extras
		);
	}

	private static Set<String> toScopeSet(AuthorizationServerMetadata md) {
		var s = md.getScopes();
		if (s == null)
			return Collections.emptySet();
		var out = new LinkedHashSet<String>();
		for (var v : s.toStringList())
			out.add(v);
		return out;
	}

	private static final Set<String> STANDARD_FIELDS = Set.of(
		"issuer", "token_endpoint", "authorization_endpoint", "introspection_endpoint",
		"jwks_uri", "userinfo_endpoint", "end_session_endpoint", "scopes_supported"
	);
}
