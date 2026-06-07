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

import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.auth.oauth.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;

/**
 * Resource-owner password-credentials grant (RFC 6749 &sect;4.3) flow helper.
 *
 * <p>
 * <b>Discouraged.</b>  This grant type was removed from OAuth 2.1 due to long-standing security concerns
 * (the client sees the user's credentials).  Legitimate use is limited to first-party trusted clients that
 * already have the user's credentials (e.g. legacy intranet apps mid-migration).  Prefer
 * {@link OAuthAuthorizationCodeFlow}.
 *
 * <p>
 * Filed under {@link Deprecated &#64;Deprecated(since = "10.0.0")} from day-1 to surface the warning at IDE
 * compile time to surface the deprecation warning.  Wraps Nimbus's {@link ResourceOwnerPasswordCredentialsGrant}.
 *
 * @since 10.0.0
 * @deprecated Use {@link OAuthAuthorizationCodeFlow} (with PKCE) wherever possible.  The
 * 	resource-owner password-credentials grant was removed from OAuth 2.1.
 */
@Deprecated(since = "10.0.0", forRemoval = false)
@SuppressWarnings({
	"java:S1192", // Duplicate string literals are OAuth protocol parameter names (e.g. "grant_type", "username"); intentional
	"java:S1133" // Intentional deprecation retained for backward compatibility until the documented removal; the reminder is not actionable now.
})
public class OAuthResourceOwnerFlow {

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	public static class Builder {
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private String username;
		private Supplier<String> passwordSupplier;
		private Set<String> scopes = new LinkedHashSet<>();
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		@Deprecated(since = "10.0.0", forRemoval = false)
		protected Builder() {}

		/**
		 * Sets the token endpoint URL.  Required.
		 *
		 * @param value The endpoint.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder tokenEndpoint(URI value) {
			tokenEndpoint = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client ID.  Required.
		 *
		 * @param value The client ID.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client secret as a literal string.
		 *
		 * @param value The secret.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
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
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the resource-owner username.  Required.
		 *
		 * @param value The username.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder username(String value) {
			username = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the resource-owner password as a literal string.
		 *
		 * @param value The password.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder password(String value) {
			assertArgNotNullOrBlank("value", value);
			passwordSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the resource-owner password via a supplier.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder passwordSupplier(Supplier<String> value) {
			passwordSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Adds requested scopes.
		 *
		 * @param values The scopes.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder scope(String... values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				scopes.add(v);
			}
			return this;
		}

		/**
		 * Sets the HTTP request configurator.
		 *
		 * @param value The callback.
		 * @return This object.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the flow.
		 *
		 * @return A new {@link OAuthResourceOwnerFlow}.
		 */
		@Deprecated(since = "10.0.0", forRemoval = false)
		public OAuthResourceOwnerFlow build() {
			if (tokenEndpoint == null)
				throw new IllegalStateException("OAuthResourceOwnerFlow requires tokenEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("OAuthResourceOwnerFlow requires clientId(...)");
			if (clientSecretSupplier == null)
				throw new IllegalStateException("OAuthResourceOwnerFlow requires clientSecret(...) or clientSecretSupplier(...)");
			if (username == null)
				throw new IllegalStateException("OAuthResourceOwnerFlow requires username(...)");
			if (passwordSupplier == null)
				throw new IllegalStateException("OAuthResourceOwnerFlow requires password(...) or passwordSupplier(...)");
			return new OAuthResourceOwnerFlow(this);
		}
	}

	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final String username;
	private final Supplier<String> passwordSupplier;
	private final Set<String> scopes;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	protected OAuthResourceOwnerFlow(Builder b) {
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.username = b.username;
		this.passwordSupplier = b.passwordSupplier;
		this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(b.scopes));
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Acquires a token from the IdP.
	 *
	 * @return The acquired token.
	 */
	@Deprecated(since = "10.0.0", forRemoval = false)
	public OAuthToken acquire() {
		var clientAuth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecretSupplier.get()));
		var grant = new ResourceOwnerPasswordCredentialsGrant(username, new Secret(passwordSupplier.get()));
		Scope nimbusScope = scopes.isEmpty() ? null : new Scope(scopes.toArray(new String[0]));
		var req = new TokenRequest.Builder(tokenEndpoint, clientAuth, grant)
			.scope(nimbusScope)
			.build();
		return Flows.send(req, httpRequestConfigurator);
	}
}
