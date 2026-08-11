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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.UriUtils.*;

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.token.*;

/**
 * Refresh-token grant (RFC 6749 &sect;6) flow helper.
 *
 * <p>
 * Supports RFC 8707 resource-indicator binding via {@link Builder#resource(URI)}.
 *
 * <p>
 * Wraps Nimbus's {@link RefreshTokenGrant}.  The IdP may return a rotated refresh token; callers should
 * persist the {@link OAuthToken#refreshToken()} value if present.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OAuth protocol parameter names (e.g. "grant_type", "refresh_token"); intentional
})
public class OAuthRefreshTokenFlow {

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
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private String refreshToken;
		private Set<String> scopes = st();
		private URI resource;
		private Duration httpTimeout = Flows.DEFAULT_HTTP_TIMEOUT;
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the token endpoint URL.  Required.
		 *
		 * @param value The endpoint.
		 * @return This object.
		 */
		public Builder tokenEndpoint(URI value) {
			tokenEndpoint = assertSecureOrLoopback(assertArgNotNull("value", value));
			return this;
		}

		/**
		 * Sets the OAuth client ID.  Required.
		 *
		 * @param value The client ID.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client secret.  May be omitted for public clients.
		 *
		 * @param value The secret.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank("value", value);
			clientSecretSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the OAuth client secret via a supplier.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the refresh-token value.  Required.
		 *
		 * @param value The refresh token.
		 * @return This object.
		 */
		public Builder refreshToken(String value) {
			refreshToken = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Adds optional scope narrowing.
		 *
		 * @param values The scopes.
		 * @return This object.
		 */
		public Builder scope(String... values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				scopes.add(v);
			}
			return this;
		}

		/**
		 * Sets the RFC 8707 {@code resource} indicator sent on the token request.
		 *
		 * @param value The canonical resource URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder resource(URI value) {
			resource = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the token request.  Default 10 seconds.
		 *
		 * @param value The timeout.  Must not be <jk>null</jk> and must be positive.
		 * @return This object.
		 */
		public Builder httpTimeout(Duration value) {
			assertArgNotNull("value", value);
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
			httpRequestConfigurator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the flow.
		 *
		 * @return A new {@link OAuthRefreshTokenFlow}.
		 */
		public OAuthRefreshTokenFlow build() {
			if (tokenEndpoint == null)
				throw new IllegalStateException("OAuthRefreshTokenFlow requires tokenEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("OAuthRefreshTokenFlow requires clientId(...)");
			if (refreshToken == null)
				throw new IllegalStateException("OAuthRefreshTokenFlow requires refreshToken(...)");
			return new OAuthRefreshTokenFlow(this);
		}
	}

	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final String refreshToken;
	private final Set<String> scopes;
	private final URI resource;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthRefreshTokenFlow(Builder b) {
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.refreshToken = b.refreshToken;
		this.scopes = u(cp(b.scopes));
		this.resource = b.resource;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Acquires a fresh access token.
	 *
	 * @return The acquired token.
	 */
	public OAuthToken acquire() {
		var grant = new RefreshTokenGrant(new RefreshToken(refreshToken));
		Scope nimbusScope = scopes.isEmpty() ? null : new Scope(scopes.toArray(new String[0]));
		TokenRequest.Builder reqBuilder;
		if (clientSecretSupplier != null) {
			var clientAuth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecretSupplier.get()));
			reqBuilder = new TokenRequest.Builder(tokenEndpoint, clientAuth, grant).scope(nimbusScope);
		} else {
			reqBuilder = new TokenRequest.Builder(tokenEndpoint, new ClientID(clientId), grant).scope(nimbusScope);
		}
		if (resource != null)
			reqBuilder.resource(resource);
		return Flows.send(reqBuilder.build(), httpTimeout, httpRequestConfigurator);
	}
}
