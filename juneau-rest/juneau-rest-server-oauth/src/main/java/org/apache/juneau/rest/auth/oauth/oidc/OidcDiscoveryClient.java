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

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * Fetches a Juneau-native {@link OidcMetadata} record from an OpenID Connect provider's
 * {@code /.well-known/openid-configuration} document.
 *
 * <p>
 * Wraps Nimbus's {@code OIDCProviderMetadata.resolve(Issuer)}.  The metadata is fetched once on each call
 * to {@link #discover()}; result is not cached internally &mdash; callers cache themselves if needed.
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
public class OidcDiscoveryClient {

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
			issuer = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the HTTP request configurator.
		 *
		 * @param value The callback.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull("value", value);
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

	private final URI issuer;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OidcDiscoveryClient(Builder b) {
		this.issuer = b.issuer;
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
	 * Fetches the IdP's OIDC metadata.
	 *
	 * @return The parsed metadata record.
	 * @throws IOException If the HTTP fetch fails.
	 * @throws OidcDiscoveryException If the metadata cannot be parsed.
	 */
	public OidcMetadata discover() throws IOException, OidcDiscoveryException {
		OIDCProviderMetadata md;
		try {
			md = OIDCProviderMetadata.resolve(new Issuer(issuer.toString()), http -> {
				if (httpRequestConfigurator != null)
					httpRequestConfigurator.accept(http);
			});
		} catch (GeneralException e) {
			throw new OidcDiscoveryException("Failed to parse OIDC metadata for " + issuer, e);
		}
		var extras = new LinkedHashMap<String,Object>();
		var json = md.toJSONObject();
		for (var e : json.entrySet()) {
			var k = e.getKey();
			if (!STANDARD_FIELDS.contains(k))
				extras.put(k, e.getValue());
		}
		return new OidcMetadata(
			URI.create(md.getIssuer().getValue()),
			md.getTokenEndpointURI(),
			md.getAuthorizationEndpointURI(),
			md.getIntrospectionEndpointURI(),
			md.getJWKSetURI(),
			md.getUserInfoEndpointURI(),
			md.getEndSessionEndpointURI(),
			toScopeSet(md),
			extras
		);
	}

	private static Set<String> toScopeSet(OIDCProviderMetadata md) {
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
