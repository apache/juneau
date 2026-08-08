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

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.client.mcp.*;
import org.apache.juneau.rest.client.mcp.auth.flow.*;

import com.nimbusds.oauth2.sdk.http.*;

/**
 * A headless bearer-token provider that feeds the {@link McpAuthInterceptor} token seam ({@code Supplier<String>}).
 *
 * <p>
 * This is the F1 client-side token-manager: it acquires, caches, and refreshes an access token and returns the current
 * valid bearer credential from {@link #get()}.  Three modes are supported:
 * <ul>
 * 	<li><b>Pre-provisioned / static</b> ({@link #ofStaticToken(String)}) &mdash; always returns a fixed token.
 * 	<li><b>Client-credentials</b> ({@link Builder#clientCredentials()}) &mdash; RFC 6749 &sect;4.4 grant, re-acquired
 * 		on expiry.
 * 	<li><b>Refresh-token</b> ({@link Builder#refreshToken(String)}, SEP-2207) &mdash; RFC 6749 &sect;6 grant; rotated
 * 		refresh tokens returned by the IdP are captured and used for the next refresh without re-consent, and the
 * 		refreshed access token is fed back to the supplier.
 * </ul>
 *
 * <p>
 * The dynamic modes (client-credentials, refresh-token) require {@link Builder#resource(URI)} and carry that RFC 8707
 * {@code resource} indicator on every token request, binding the token's audience to the target MCP server.  The static
 * mode issues no token request, so it takes no resource indicator.
 *
 * <p>
 * {@link #get()} never returns a blank token: dynamic modes return the IdP-issued (non-blank) access token or throw
 * {@link McpAuthException}/{@link OAuthFlowException} on failure (which aborts the intercepted request), and the static
 * mode rejects a blank token at construction.  This satisfies the {@code McpAuthInterceptor} "supply a non-blank token
 * or suppress the header" contract.
 *
 * <p>
 * Thread-safe.  <b>Never logged:</b> {@link #toString()} discloses only non-secret configuration.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class McpTokenProvider implements Supplier<String> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";

	private enum Mode { STATIC, CLIENT_CREDENTIALS, REFRESH }

	/**
	 * Creates a provider that always returns the supplied pre-provisioned bearer token.
	 *
	 * @param token The static bearer token.  Must not be <jk>null</jk> or blank.
	 * @return A new provider.
	 */
	public static McpTokenProvider ofStaticToken(String token) {
		assertArgNotNullOrBlank("token", token);
		return new Builder(Mode.STATIC).staticToken(token).build();
	}

	/**
	 * Creates a builder for the client-credentials grant.
	 *
	 * @return A new builder.
	 */
	public static Builder clientCredentials() {
		return new Builder(Mode.CLIENT_CREDENTIALS);
	}

	/**
	 * Creates a builder for the refresh-token grant, seeded with the initial refresh token.
	 *
	 * @param initialRefreshToken The initial refresh token.  Must not be <jk>null</jk> or blank.
	 * @return A new builder.
	 */
	public static Builder refreshToken(String initialRefreshToken) {
		assertArgNotNullOrBlank("initialRefreshToken", initialRefreshToken);
		return new Builder(Mode.REFRESH).initialRefreshToken(initialRefreshToken);
	}

	/**
	 * Builder.
	 */
	public static class Builder {
		private final Mode mode;
		private String staticToken;
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private String initialRefreshToken;
		private final Set<String> scopes = st();
		private URI resource;
		private Duration expirySkew = Duration.ofSeconds(30);
		private Clock clock = Clock.systemUTC();
		private Duration httpTimeout = Duration.ofSeconds(10);
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/**
		 * Constructor.
		 *
		 * @param mode The grant mode.
		 */
		protected Builder(Mode mode) {
			this.mode = mode;
		}

		Builder staticToken(String value) {
			staticToken = value;
			return this;
		}

		Builder initialRefreshToken(String value) {
			initialRefreshToken = value;
			return this;
		}

		/**
		 * Sets the token endpoint URL.  Required for the client-credentials and refresh-token modes.
		 *
		 * @param value The endpoint URL.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder tokenEndpoint(URI value) {
			tokenEndpoint = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the OAuth client ID.  Required for the client-credentials and refresh-token modes.
		 *
		 * @param value The client ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank(ARG_value, value);
			return this;
		}

		/**
		 * Sets the OAuth client secret as a literal string.
		 *
		 * @param value The client secret.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank(ARG_value, value);
			clientSecretSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the OAuth client secret via a supplier.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Adds requested OAuth scopes.
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
		 * Sets the RFC 8707 {@code resource} indicator (the canonical MCP server URI) sent on every token request.
		 *
		 * @param value The canonical resource URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder resource(URI value) {
			resource = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the skew subtracted from a cached token's expiry before it is treated as expired.  Default 30s.
		 *
		 * @param value The skew.  Must be non-negative.
		 * @return This object.
		 */
		public Builder expirySkew(Duration value) {
			assertArgNotNull(ARG_value, value);
			assertArg(!value.isNegative(), "expirySkew must be non-negative (was %s)", value);
			expirySkew = value;
			return this;
		}

		/**
		 * Sets the clock used for token-expiry comparisons.  Primarily for tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to each token request.  Default 10 seconds.
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
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} for each token request.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the provider.
		 *
		 * @return A new {@link McpTokenProvider}.
		 */
		public McpTokenProvider build() {
			if (mode != Mode.STATIC) {
				if (tokenEndpoint == null)
					throw isex("McpTokenProvider requires tokenEndpoint(...)");
				if (clientId == null)
					throw isex("McpTokenProvider requires clientId(...)");
				if (resource == null)
					throw isex("McpTokenProvider requires resource(...) (RFC 8707 resource indicator)");
			}
			if (mode == Mode.CLIENT_CREDENTIALS && clientSecretSupplier == null)
				throw isex("McpTokenProvider (client-credentials) requires clientSecret(...) or clientSecretSupplier(...)");
			return new McpTokenProvider(this);
		}
	}

	private final Mode mode;
	private final String staticToken;
	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final Set<String> scopes;
	private final URI resource;
	private final Duration expirySkew;
	private final Clock clock;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	private final Object lock = new Object();
	private OAuthToken currentToken;
	private String currentRefreshToken;
	private boolean refreshTerminallyRejected;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpTokenProvider(Builder b) {
		this.mode = b.mode;
		this.staticToken = b.staticToken;
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.scopes = u(cp(b.scopes));
		this.resource = b.resource;
		this.expirySkew = b.expirySkew;
		this.clock = b.clock;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
		this.currentRefreshToken = b.initialRefreshToken;
	}

	/**
	 * Returns the current valid bearer token, acquiring or refreshing one on demand.
	 *
	 * <p>
	 * Dynamic modes acquire under a single-flight monitor so concurrent callers don't stampede the IdP; the per-request
	 * HTTP timeout ({@link Builder#httpTimeout(Duration)}, default 10s) is the backstop that prevents one hung IdP from
	 * wedging the monitor indefinitely.  In refresh mode a terminal {@code invalid_grant} latches the provider: every
	 * subsequent call throws a distinguishable {@link McpAuthException} rather than re-hammering the IdP with a dead
	 * refresh token (SEP-2207).
	 *
	 * @return A non-blank bearer token.  Never {@code null} for a successfully configured provider.
	 * @throws OAuthFlowException If a token acquisition/refresh HTTP round-trip fails or the IdP returns an error.
	 * @throws McpAuthException If a prior refresh was terminally rejected ({@code invalid_grant}); re-authorization is
	 * 	required.
	 */
	@Override
	public String get() {
		if (mode == Mode.STATIC)
			return staticToken;
		synchronized (lock) {
			if (mode == Mode.REFRESH && refreshTerminallyRejected)
				throw new McpAuthException("Refresh token was rejected by the authorization server (invalid_grant); re-authorization required");
			if (currentToken != null && !currentToken.isExpired(clock.instant(), expirySkew))
				return currentToken.accessToken();
			OAuthToken t;
			try {
				t = acquire();
			} catch (OAuthFlowException e) {
				if (mode == Mode.REFRESH && e.errorCode().filter("invalid_grant"::equals).isPresent()) {
					refreshTerminallyRejected = true;
					throw new McpAuthException("Refresh token was rejected by the authorization server (invalid_grant); re-authorization required", e);
				}
				throw e;
			}
			currentToken = t;
			if (mode == Mode.REFRESH)
				t.refreshToken().ifPresent(rt -> currentRefreshToken = rt);
			return t.accessToken();
		}
	}

	private OAuthToken acquire() {
		if (mode == Mode.CLIENT_CREDENTIALS) {
			var f = OAuthClientCredentialsFlow.create()
				.tokenEndpoint(tokenEndpoint)
				.clientId(clientId)
				.clientSecretSupplier(clientSecretSupplier);
			f.httpTimeout(httpTimeout);
			scopes.forEach(f::scope);
			if (resource != null)
				f.resource(resource);
			if (httpRequestConfigurator != null)
				f.httpRequestConfigurator(httpRequestConfigurator);
			return f.build().acquire();
		}
		// REFRESH
		var f = OAuthRefreshTokenFlow.create()
			.tokenEndpoint(tokenEndpoint)
			.clientId(clientId)
			.refreshToken(currentRefreshToken);
		if (clientSecretSupplier != null)
			f.clientSecretSupplier(clientSecretSupplier);
		f.httpTimeout(httpTimeout);
		scopes.forEach(f::scope);
		if (resource != null)
			f.resource(resource);
		if (httpRequestConfigurator != null)
			f.httpRequestConfigurator(httpRequestConfigurator);
		return f.build().acquire();
	}

	/**
	 * Returns the refresh token currently held (the most recently rotated value in refresh mode).
	 *
	 * <p>
	 * Callers persisting refresh tokens across process restarts read this after each {@link #get()} to capture a
	 * rotated token (SEP-2207).
	 *
	 * @return The current refresh token, or {@link Optional#empty()} if none is held.
	 */
	public Optional<String> currentRefreshToken() {
		synchronized (lock) {
			return o(currentRefreshToken);
		}
	}

	/**
	 * Creates an {@link McpAuthInterceptor} backed by this provider, ready to attach to an MCP client builder.
	 *
	 * @return A new interceptor.
	 */
	public McpAuthInterceptor interceptor() {
		return new McpAuthInterceptor(this);
	}

	/**
	 * Redacts token/secret material so nothing sensitive reaches logs via this provider's {@code toString()}.
	 *
	 * @return A redacted string form disclosing only non-secret configuration.
	 */
	@Override
	public String toString() {
		return "McpTokenProvider[mode=" + mode
			+ ", tokenEndpoint=" + tokenEndpoint
			+ ", clientId=" + clientId
			+ ", scopes=" + scopes
			+ ", resource=" + resource
			+ ", staticToken=" + (staticToken != null ? "<redacted>" : "<none>")
			+ ", refreshToken=" + (currentRefreshToken != null ? "<redacted>" : "<none>")
			+ "]";
	}
}
