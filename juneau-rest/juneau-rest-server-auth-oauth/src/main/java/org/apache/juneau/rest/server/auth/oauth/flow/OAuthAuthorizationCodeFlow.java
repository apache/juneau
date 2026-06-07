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
package org.apache.juneau.rest.server.auth.oauth.flow;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.auth.oauth.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.pkce.*;
import com.nimbusds.openid.connect.sdk.*;

/**
 * Authorization-code grant (RFC 6749 &sect;4.1) flow helper with mandatory PKCE per RFC 7636.
 *
 * <p>
 * Wraps Nimbus's {@link AuthorizationRequest} (for URL construction) + {@link AuthorizationCodeGrant} +
 * {@link TokenRequest} (for the exchange) + {@link CodeChallenge#compute(CodeChallengeMethod, CodeVerifier)
 * PKCE S256 challenge generation}.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> flow = OAuthAuthorizationCodeFlow.<jsm>create</jsm>()
 * 		.authorizationEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/authorize"</js>))
 * 		.tokenEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/token"</js>))
 * 		.clientId(<js>"web-app"</js>)
 * 		.clientSecret(<js>"..."</js>)
 * 		.redirectUri(URI.<jsm>create</jsm>(<js>"https://app.example.com/callback"</js>))
 * 		.scope(<js>"openid"</js>, <js>"profile"</js>)
 * 		.build();
 *
 * 	<jk>var</jk> verifier = <jk>new</jk> CodeVerifier();
 * 	<jk>var</jk> challenge = CodeChallenge.<jsm>compute</jsm>(CodeChallengeMethod.<jsf>S256</jsf>, verifier);
 * 	<jk>var</jk> authUrl = flow.buildAuthorizationUrl(<js>"some-state"</js>, challenge);
 *
 * 	<jc>// After the IdP redirects back with ?code=...</jc>
 * 	<jk>var</jk> token = flow.exchange(<jv>code</jv>, verifier);
 * </p>
 *
 * <p>
 * The flow does NOT manage state / PKCE-verifier storage; that's the caller's responsibility per OIDC RP
 * charter (see {@code OidcDiscoveryClient} javadoc).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OAuth protocol parameter names (e.g. "code", "grant_type"); intentional
})
public class OAuthAuthorizationCodeFlow {

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
		private URI authorizationEndpoint;
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private URI redirectUri;
		private Set<String> scopes = new LinkedHashSet<>();
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the authorization endpoint URL.  Required.
		 *
		 * @param value The endpoint URL.
		 * @return This object.
		 */
		public Builder authorizationEndpoint(URI value) {
			authorizationEndpoint = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the token endpoint URL.  Required.
		 *
		 * @param value The endpoint URL.
		 * @return This object.
		 */
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
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the client secret.  May be omitted for public clients (PKCE-only).
		 *
		 * @param value The client secret.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank("value", value);
			clientSecretSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the client secret via a supplier.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the redirect URI.  Required.
		 *
		 * @param value The redirect URI.
		 * @return This object.
		 */
		public Builder redirectUri(URI value) {
			redirectUri = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Adds requested scopes.
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
		 * @return A new {@link OAuthAuthorizationCodeFlow}.
		 */
		public OAuthAuthorizationCodeFlow build() {
			if (authorizationEndpoint == null)
				throw new IllegalStateException("OAuthAuthorizationCodeFlow requires authorizationEndpoint(...)");
			if (tokenEndpoint == null)
				throw new IllegalStateException("OAuthAuthorizationCodeFlow requires tokenEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("OAuthAuthorizationCodeFlow requires clientId(...)");
			if (redirectUri == null)
				throw new IllegalStateException("OAuthAuthorizationCodeFlow requires redirectUri(...)");
			return new OAuthAuthorizationCodeFlow(this);
		}
	}

	private final URI authorizationEndpoint;
	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final URI redirectUri;
	private final Set<String> scopes;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthAuthorizationCodeFlow(Builder b) {
		this.authorizationEndpoint = b.authorizationEndpoint;
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.redirectUri = b.redirectUri;
		this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(b.scopes));
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Builds the {@code authorize?...} URL the user-agent should be redirected to.
	 *
	 * @param state The opaque {@code state} parameter (CSRF protection).  Must not be <jk>null</jk> or
	 * 	blank.
	 * @param codeChallenge The PKCE code challenge.  Must not be <jk>null</jk>.  Always uses S256.
	 * @return The authorization URL.
	 */
	@SuppressWarnings({
		"deprecation" // Nimbus CodeChallenge overload is deprecated; no simpler alternative yet.
	})
	public URI buildAuthorizationUrl(String state, CodeChallenge codeChallenge) {
		assertArgNotNullOrBlank("state", state);
		assertArgNotNull("codeChallenge", codeChallenge);
		Scope nimbusScope = scopes.isEmpty() ? null : new Scope(scopes.toArray(new String[0]));
		var request = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), new ClientID(clientId))
			.endpointURI(authorizationEndpoint)
			.redirectionURI(redirectUri)
			.scope(nimbusScope)
			.state(new State(state))
			.codeChallenge(codeChallenge, CodeChallengeMethod.S256)
			.build();
		return request.toURI();
	}

	/**
	 * Builds an OpenID Connect {@code authorize?...} URL carrying an OIDC {@code nonce} parameter.
	 *
	 * <p>
	 * Unlike {@link #buildAuthorizationUrl(String, CodeChallenge)} (which emits a plain OAuth 2.0
	 * {@link AuthorizationRequest} with no {@code nonce}), this method emits an OIDC
	 * {@link AuthenticationRequest} so the IdP echoes the supplied {@code nonce} back inside the issued
	 * ID token.  An OpenID Connect relying party stores the {@code nonce} before the redirect and
	 * verifies the {@code nonce} claim on the returned ID token to defeat token replay.
	 *
	 * <p>
	 * The configured {@code scope(...)} set is used as-is; OIDC requires it to contain {@code openid}.
	 * The optional {@code customizer} receives the underlying {@link com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder} so
	 * callers can set OIDC-specific parameters (e.g. {@code prompt}, {@code max_age}, {@code acr_values})
	 * without this module needing to surface each one.
	 *
	 * @param state The opaque {@code state} parameter (CSRF protection).  Must not be <jk>null</jk> or
	 * 	blank.
	 * @param codeChallenge The PKCE code challenge.  Must not be <jk>null</jk>.  Always uses S256.
	 * @param nonce The OIDC {@code nonce} value.  Must not be <jk>null</jk> or blank.
	 * @param customizer Optional hook on the {@link com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder} for OIDC-specific
	 * 	parameters.  May be <jk>null</jk>.
	 * @return The authorization URL.
	 */
	@SuppressWarnings({
		"deprecation" // Nimbus CodeChallenge overload is deprecated; no simpler alternative yet.
	})
	public URI buildAuthenticationUrl(String state, CodeChallenge codeChallenge, String nonce, Consumer<AuthenticationRequest.Builder> customizer) {
		assertArgNotNullOrBlank("state", state);
		assertArgNotNull("codeChallenge", codeChallenge);
		assertArgNotNullOrBlank("nonce", nonce);
		Scope nimbusScope = scopes.isEmpty() ? new Scope("openid") : new Scope(scopes.toArray(new String[0]));
		var builder = new AuthenticationRequest.Builder(new ResponseType(ResponseType.Value.CODE), nimbusScope, new ClientID(clientId), redirectUri)
			.endpointURI(authorizationEndpoint)
			.state(new State(state))
			.nonce(new Nonce(nonce))
			.codeChallenge(codeChallenge, CodeChallengeMethod.S256);
		if (customizer != null)
			customizer.accept(builder);
		return builder.build().toURI();
	}

	/**
	 * Exchanges the authorization code returned by the IdP for an access token.
	 *
	 * @param code The {@code code} query parameter from the IdP redirect.  Must not be <jk>null</jk> or
	 * 	blank.
	 * @param codeVerifier The PKCE verifier matching the challenge passed to
	 * 	{@link #buildAuthorizationUrl(String, CodeChallenge)}.  Must not be <jk>null</jk>.
	 * @return The acquired token.
	 */
	public OAuthToken exchange(String code, CodeVerifier codeVerifier) {
		assertArgNotNullOrBlank("code", code);
		assertArgNotNull("codeVerifier", codeVerifier);
		var grant = new AuthorizationCodeGrant(new AuthorizationCode(code), redirectUri, codeVerifier);
		TokenRequest req;
		if (clientSecretSupplier != null) {
			var clientAuth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecretSupplier.get()));
			req = new TokenRequest.Builder(tokenEndpoint, clientAuth, grant).build();
		} else {
			req = new TokenRequest.Builder(tokenEndpoint, new ClientID(clientId), grant).build();
		}
		return Flows.send(req, httpRequestConfigurator);
	}

	/**
	 * Returns the authorization endpoint URL.
	 *
	 * @return The endpoint.
	 */
	public URI getAuthorizationEndpoint() {
		return authorizationEndpoint;
	}

	/**
	 * Returns the token endpoint URL.
	 *
	 * @return The endpoint.
	 */
	public URI getTokenEndpoint() {
		return tokenEndpoint;
	}

	/**
	 * Returns the client ID.
	 *
	 * @return The client ID.
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Returns the redirect URI.
	 *
	 * @return The redirect URI.
	 */
	public URI getRedirectUri() {
		return redirectUri;
	}

	/**
	 * Returns the requested scopes.
	 *
	 * @return An unmodifiable view.
	 */
	public Set<String> getScopes() {
		return scopes;
	}
}
