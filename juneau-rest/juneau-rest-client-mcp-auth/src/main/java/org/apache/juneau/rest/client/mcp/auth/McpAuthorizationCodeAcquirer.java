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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.UriUtils.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.client.mcp.auth.flow.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.pkce.*;

/**
 * Interactive authorization-code + PKCE (S256) token acquisition for headless / CLI use (F1 Phase B).
 *
 * <p>
 * Builds on the relocated {@link OAuthAuthorizationCodeFlow}: opens a {@link LoopbackRedirectReceiver}, generates the
 * {@code state} and PKCE {@code code_verifier}/{@code code_challenge}, hands the authorization URL to a caller-supplied
 * browser launcher, waits for the loopback redirect, and exchanges the returned code for a token.
 *
 * <p>
 * Security checks on the callback:
 * <ul>
 * 	<li><b>CSRF</b> &mdash; the returned {@code state} must match the one that was issued (single-use via
 * 		{@link EphemeralStore}).
 * 	<li><b>SEP-2468 ({@code iss}, RFC 9207)</b> &mdash; the callback {@code iss} parameter must equal the
 * 		discovered/expected issuer; a mismatch is rejected.  The check gates BOTH the success and error paths (a client
 * 		MUST NOT act on an error response whose issuer doesn't match).  {@link Builder#expectedIssuer(URI)} is required
 * 		by default (opt out with {@link Builder#skipIssuerValidation(boolean)}); a missing {@code iss} is accepted
 * 		unless {@link Builder#requireIssuerResponseParameter(boolean)} is set.
 * </ul>
 *
 * <p>
 * {@link Builder#resource(URI)} (the RFC 8707 resource indicator) is required so the acquired token's audience is bound
 * to the target MCP server.
 *
 * <p>
 * The optional {@link Builder#offlineAccess(boolean)} adds the {@code offline_access} scope so the IdP issues a refresh
 * token that can subsequently drive an {@link McpTokenProvider} refresh-token provider (SEP-2207).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class McpAuthorizationCodeAcquirer {

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
		private URI authorizationEndpoint;
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private final Set<String> scopes = st();
		private boolean offlineAccess;
		private URI resource;
		private URI expectedIssuer;
		private boolean skipIssuerValidation;
		private boolean requireIssuerResponseParameter;
		private String redirectPath = DEFAULT_REDIRECT_PATH;
		private int redirectPort;  // 0 == ephemeral / port-agnostic (the default).
		private Consumer<URI> browserLauncher = McpAuthorizationCodeAcquirer::printAuthorizationUrl;
		private EphemeralStore store;
		private Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
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
			authorizationEndpoint = assertSecureOrLoopback(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Sets the token endpoint URL.  Required.
		 *
		 * @param value The endpoint URL.
		 * @return This object.
		 */
		public Builder tokenEndpoint(URI value) {
			tokenEndpoint = assertSecureOrLoopback(assertArgNotNull(ARG_value, value));
			return this;
		}

		/**
		 * Sets the OAuth client ID.  Required.
		 *
		 * @param value The client ID.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank(ARG_value, value);
			return this;
		}

		/**
		 * Sets the client secret (omit for a public/native PKCE-only client).
		 *
		 * @param value The client secret.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank(ARG_value, value);
			clientSecretSupplier = () -> value;
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
		 * Requests the {@code offline_access} scope so the IdP issues a refresh token (SEP-2207).
		 *
		 * @param value <jk>true</jk> to add {@code offline_access}.
		 * @return This object.
		 */
		public Builder offlineAccess(boolean value) {
			offlineAccess = value;
			return this;
		}

		/**
		 * Sets the RFC 8707 {@code resource} indicator (the canonical MCP server URI).
		 *
		 * @param value The canonical resource URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder resource(URI value) {
			resource = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the expected issuer the callback {@code iss} parameter is validated against (SEP-2468).
		 *
		 * <p>
		 * Typically the {@code issuer} returned by {@code OidcDiscoveryClient} for the discovered authorization
		 * server.
		 *
		 * @param value The expected issuer.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder expectedIssuer(URI value) {
			expectedIssuer = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Explicitly opts out of the SEP-2468 {@code iss} validation.
		 *
		 * <p>
		 * {@code iss} validation is on by default; {@link #build()} requires {@link #expectedIssuer(URI)} unless this
		 * is set to <jk>true</jk>.  Only disable this against an authorization server known not to emit {@code iss}.
		 *
		 * @param value <jk>true</jk> to skip the {@code iss} check.
		 * @return This object.
		 */
		public Builder skipIssuerValidation(boolean value) {
			skipIssuerValidation = value;
			return this;
		}

		/**
		 * Requires the authorization response to carry an {@code iss} parameter, rejecting a response that omits it.
		 *
		 * <p>
		 * Set this when the discovered authorization-server metadata advertises
		 * {@code authorization_response_iss_parameter_supported} (RFC 9207); see
		 * {@link org.apache.juneau.rest.client.mcp.auth.oidc.OidcMetadata#authorizationResponseIssParameterSupported()}.
		 *
		 * @param value <jk>true</jk> to reject a response with no {@code iss} parameter.
		 * @return This object.
		 */
		public Builder requireIssuerResponseParameter(boolean value) {
			requireIssuerResponseParameter = value;
			return this;
		}

		/**
		 * Sets the loopback redirect path.  Default {@code /callback}.
		 *
		 * @param value The path (must start with {@code /}).  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder redirectPath(String value) {
			redirectPath = assertArgNotNullOrBlank(ARG_value, value);
			assertArg(redirectPath.startsWith("/"), "redirectPath must start with '/' (was '%s')", redirectPath);
			return this;
		}

		/**
		 * Sets a fixed loopback redirect port so the receiver binds exactly that port (the "bind-first" strategy for
		 * strict exact-match authorization servers).
		 *
		 * <p>
		 * Defaults to {@code 0}, meaning an ephemeral port (the RFC 8252 &sect;7.3 port-agnostic strategy) &mdash; the
		 * simplest interoperable choice.  Set a non-zero port only when the authorization server registration used a
		 * bind-first {@link LoopbackRedirectUris#forPort(int, String) forPort} redirect; {@link McpClientRegistrations#configure}
		 * wires this automatically from such a registration.
		 *
		 * @param value The fixed loopback port in {@code 1..65535}, or {@code 0} for an ephemeral port.
		 * @return This object.
		 */
		public Builder redirectPort(int value) {
			assertArg(value == 0 || (value >= 1 && value <= 65535), "redirectPort must be 0 (ephemeral) or in 1..65535 (was %s)", value);
			redirectPort = value;
			return this;
		}

		/**
		 * Sets the callback invoked with the authorization URL so the caller can open it in a browser (or print it).
		 *
		 * @param value The launcher.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder browserLauncher(Consumer<URI> value) {
			browserLauncher = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the ephemeral state/verifier store.  Defaults to a fresh {@link EphemeralStore}.
		 *
		 * @param value The store.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder store(EphemeralStore value) {
			store = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the token-exchange request.  Default 10 seconds.
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
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} for the token exchange.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the acquirer.
		 *
		 * @return A new {@link McpAuthorizationCodeAcquirer}.
		 */
		public McpAuthorizationCodeAcquirer build() {
			if (authorizationEndpoint == null)
				throw isex("McpAuthorizationCodeAcquirer requires authorizationEndpoint(...)");
			if (tokenEndpoint == null)
				throw isex("McpAuthorizationCodeAcquirer requires tokenEndpoint(...)");
			if (clientId == null)
				throw isex("McpAuthorizationCodeAcquirer requires clientId(...)");
			if (resource == null)
				throw isex("McpAuthorizationCodeAcquirer requires resource(...) (RFC 8707 resource indicator)");
			if (expectedIssuer == null && ! skipIssuerValidation)
				throw isex("McpAuthorizationCodeAcquirer requires expectedIssuer(...) (SEP-2468); call skipIssuerValidation(true) to opt out");
			if (store == null)
				store = EphemeralStore.create();
			return new McpAuthorizationCodeAcquirer(this);
		}
	}

	/** Default connect/read timeout applied to the token-exchange request when the caller sets none. */
	static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * Default loopback callback path.  Not RFC 8252-mandated (only the loopback address itself is spec-mandated) and
	 * already fully caller-overridable via {@link Builder#redirectPath(String)}; this is merely the conventional
	 * default.
	 */
	@SuppressWarnings({
		"java:S1075" // Default value for an already caller-configurable Builder#redirectPath(String); not a hardcoded endpoint.
	})
	static final String DEFAULT_REDIRECT_PATH = "/callback";

	/** Default HTML body served on the loopback callback so the browser tab shows a friendly confirmation. */
	private static final String DEFAULT_CALLBACK_HTML =
		"<html><body><h3>Authorization received</h3><p>You may close this window and return to the application.</p></body></html>";

	private final URI authorizationEndpoint;
	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final Set<String> scopes;
	private final URI resource;
	private final URI expectedIssuer;
	private final boolean skipIssuerValidation;
	private final boolean requireIssuerResponseParameter;
	private final String redirectPath;
	private final int redirectPort;
	private final Consumer<URI> browserLauncher;
	private final EphemeralStore store;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpAuthorizationCodeAcquirer(Builder b) {
		this.authorizationEndpoint = b.authorizationEndpoint;
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		var s = new LinkedHashSet<>(b.scopes);
		if (b.offlineAccess)
			s.add("offline_access");
		this.scopes = u(s);
		this.resource = b.resource;
		this.expectedIssuer = b.expectedIssuer;
		this.skipIssuerValidation = b.skipIssuerValidation;
		this.requireIssuerResponseParameter = b.requireIssuerResponseParameter;
		this.redirectPath = b.redirectPath;
		this.redirectPort = b.redirectPort;
		this.browserLauncher = b.browserLauncher;
		this.store = b.store;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Default {@link Builder#browserLauncher(Consumer)}: prints the authorization URL to standard output so a headless
	 * user can open it manually (rather than silently doing nothing).
	 *
	 * @param authUrl The authorization URL.
	 */
	@SuppressWarnings({
		"java:S106" // Intentional user-facing console prompt for headless CLI authorization; not diagnostic logging.
	})
	private static void printAuthorizationUrl(URI authUrl) {
		System.out.println("Open the following URL in a browser to authorize:\n  " + authUrl);
	}

	/**
	 * Runs the full interactive acquisition: starts the loopback listener, opens the authorization URL, waits for the
	 * redirect, validates it, and exchanges the code for a token.
	 *
	 * @param timeout The maximum time to wait for the user to complete authorization at the IdP.  Must not be
	 * 	<jk>null</jk>.
	 * @return The acquired token.
	 * @throws McpAuthException If the listener cannot start, the wait times out, or the callback fails validation.
	 * @throws OAuthFlowException If the token-exchange HTTP round-trip fails or the IdP returns an error.
	 */
	public OAuthToken acquire(Duration timeout) {
		assertArgNotNull("timeout", timeout);
		LoopbackRedirectReceiver receiver;
		try {
			receiver = new LoopbackRedirectReceiver(redirectPath, redirectPort, DEFAULT_CALLBACK_HTML);
		} catch (IOException e) {
			throw new McpAuthException("Failed to start the loopback redirect listener", e);
		}
		try (receiver) {
			var flow = buildFlow(receiver.redirectUri());
			var verifier = new CodeVerifier();
			var state = new State().getValue();
			var authUrl = buildAuthorizationUrl(flow, state, verifier);
			store.store(state, verifier.getValue(), expectedIssuer);
			browserLauncher.accept(authUrl);
			var callbackUri = receiver.awaitCallback(timeout);
			return handleCallback(flow, callbackUri);
		}
	}

	/**
	 * Builds the authorization URL for a given state and PKCE verifier (exposed for callers driving their own
	 * user-agent interaction).
	 *
	 * @param flow The configured flow.  Must not be <jk>null</jk>.
	 * @param state The opaque state.  Must not be <jk>null</jk> or blank.
	 * @param verifier The PKCE verifier.  Must not be <jk>null</jk>.
	 * @return The authorization URL.
	 */
	public URI buildAuthorizationUrl(OAuthAuthorizationCodeFlow flow, String state, CodeVerifier verifier) {
		assertArgNotNull("flow", flow);
		assertArgNotNull("verifier", verifier);
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		return flow.buildAuthorizationUrl(state, challenge);
	}

	/**
	 * Validates a loopback callback and exchanges the authorization code for a token.
	 *
	 * <p>
	 * Consumes the pending {@code state} entry (single-use), performs the SEP-2468 {@code iss} check against the issuer
	 * recorded when the request was started, then exchanges the code.
	 *
	 * @param flow The configured flow.  Must not be <jk>null</jk>.
	 * @param callbackUri The full loopback callback URI.  Must not be <jk>null</jk>.
	 * @return The acquired token.
	 * @throws McpAuthException If the callback is an error response, the state is unknown/replayed, or the {@code iss}
	 * 	check fails.
	 */
	public OAuthToken handleCallback(OAuthAuthorizationCodeFlow flow, URI callbackUri) {
		assertArgNotNull("flow", flow);
		assertArgNotNull("callbackUri", callbackUri);
		var resp = parse(callbackUri);
		var state = resp.getState() == null ? null : resp.getState().getValue();
		if (state == null)
			throw new McpAuthException("Authorization callback is missing the state parameter");
		var pending = store.consume(state)
			.orElseThrow(() -> new McpAuthException("Authorization callback state is unknown, expired, or replayed"));
		var code = extractCode(resp, pending.expectedIssuer(), skipIssuerValidation, requireIssuerResponseParameter);
		return flow.exchange(code, new CodeVerifier(pending.codeVerifier()));
	}

	/**
	 * Validates an authorization-code callback and returns the authorization code, without any token exchange or
	 * state-store interaction.
	 *
	 * <p>
	 * This is the pure SEP-2468 / CSRF validation step.  The {@code iss} comparison gates BOTH success and error
	 * responses (per the MCP spec a client MUST NOT act on an {@code error}/{@code error_description}/{@code error_uri}
	 * from a response whose {@code iss} does not match), so an error response with a mismatched issuer is reported as
	 * an issuer mismatch rather than a denial.  A missing/mismatched {@code state} is rejected as CSRF.  When
	 * {@code iss} is absent it is accepted (future-proofed for the eventual "reject if absent" MUST); use
	 * {@link #validateAuthorizationResponse(URI, String, URI, boolean)} to require it.
	 *
	 * @param callbackUri The full callback URI.  Must not be <jk>null</jk>.
	 * @param expectedState The state issued when the authorization request was built.  Must not be <jk>null</jk>.
	 * @param expectedIssuer The discovered issuer to validate {@code iss} against.  Must not be <jk>null</jk>.
	 * @return The authorization code.
	 * @throws McpAuthException If validation fails.
	 */
	public static String validateAuthorizationResponse(URI callbackUri, String expectedState, URI expectedIssuer) {
		return validateAuthorizationResponse(callbackUri, expectedState, expectedIssuer, false);
	}

	/**
	 * Validates an authorization-code callback and returns the authorization code, without any token exchange or
	 * state-store interaction.
	 *
	 * @param callbackUri The full callback URI.  Must not be <jk>null</jk>.
	 * @param expectedState The state issued when the authorization request was built.  Must not be <jk>null</jk>.
	 * @param expectedIssuer The discovered issuer to validate {@code iss} against.  Must not be <jk>null</jk>.
	 * @param requireIssuerParameter <jk>true</jk> to reject a response that omits the {@code iss} parameter (RFC 9207
	 * 	{@code authorization_response_iss_parameter_supported}).
	 * @return The authorization code.
	 * @throws McpAuthException If validation fails.
	 */
	public static String validateAuthorizationResponse(URI callbackUri, String expectedState, URI expectedIssuer, boolean requireIssuerParameter) {
		assertArgNotNull("callbackUri", callbackUri);
		assertArgNotNullOrBlank("expectedState", expectedState);
		assertArgNotNull("expectedIssuer", expectedIssuer);
		var resp = parse(callbackUri);
		var state = resp.getState() == null ? null : resp.getState().getValue();
		if (!expectedState.equals(state))
			throw new McpAuthException("Authorization callback state mismatch (possible CSRF)");
		return extractCode(resp, expectedIssuer, false, requireIssuerParameter);
	}

	private static AuthorizationResponse parse(URI callbackUri) {
		try {
			return AuthorizationResponse.parse(callbackUri);
		} catch (ParseException e) {
			throw new McpAuthException("Authorization callback could not be parsed", e);
		}
	}

	private static String extractCode(AuthorizationResponse resp, URI expectedIssuer, boolean skipIssuerValidation, boolean requireIssuerParameter) {
		// SEP-2468: the iss check gates BOTH the success and error paths — a client MUST NOT act on an error response
		// whose issuer doesn't match, so this runs before the indicatesSuccess() branch.
		if (! skipIssuerValidation) {
			var iss = resp.getIssuer();
			if (iss == null) {
				if (requireIssuerParameter)
					throw new McpAuthException("Authorization response is missing the required iss parameter (SEP-2468 / RFC 9207)");
			} else if (expectedIssuer == null || ! iss.getValue().equals(expectedIssuer.toString())) {
				throw new McpAuthException("Authorization response issuer mismatch (SEP-2468): expected '"
					+ expectedIssuer + "' but got '" + iss.getValue() + "'");
			}
		}
		if (! resp.indicatesSuccess()) {
			var err = resp.toErrorResponse().getErrorObject();
			throw new McpAuthException("Authorization request was denied: " + (err == null ? "unknown_error" : err.getCode()));
		}
		return resp.toSuccessResponse().getAuthorizationCode().getValue();
	}

	private OAuthAuthorizationCodeFlow buildFlow(URI redirectUri) {
		var b = OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(authorizationEndpoint)
			.tokenEndpoint(tokenEndpoint)
			.clientId(clientId)
			.redirectUri(redirectUri)
			.httpTimeout(httpTimeout);
		if (clientSecretSupplier != null)
			b.clientSecretSupplier(clientSecretSupplier);
		scopes.forEach(b::scope);
		if (resource != null)
			b.resource(resource);
		if (httpRequestConfigurator != null)
			b.httpRequestConfigurator(httpRequestConfigurator);
		return b.build();
	}

	/**
	 * The configured fixed loopback redirect port ({@code 0} means ephemeral).  Package-private for test verification of
	 * the bind-first port carry-through.
	 *
	 * @return The redirect port.
	 */
	int redirectPort() {
		return redirectPort;
	}
}
