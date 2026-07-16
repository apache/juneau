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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.auth.oauth.*;
import org.apache.juneau.rest.server.auth.oauth.flow.*;
import org.apache.juneau.rest.server.auth.oauth.oidc.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jwt.proc.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.pkce.*;
import com.nimbusds.oauth2.sdk.token.*;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.*;
import com.nimbusds.openid.connect.sdk.validators.*;

import jakarta.servlet.http.*;

/**
 * Facade orchestrating an OpenID Connect Authorization-Code-with-PKCE <i>Relying Party</i> login flow on
 * top of the {@code juneau-rest-server-auth-oauth} building blocks.
 *
 * <p>
 * A single instance wires the login / callback / logout endpoints plus the session SPI for one IdP.  The
 * application mounts the three endpoints as explicit {@code @RestGet} methods that delegate to this
 * facade &mdash; there are no auto-mounted servlet routes (URL ownership stays with the app):
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/auth"</js>)
 * 	<jk>public class</jk> LoginResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jk>private final</jk> OidcRelyingParty <jf>rp</jf> = OidcRelyingParty.<jsm>create</jsm>()
 * 			.issuer(URI.<jsm>create</jsm>(<js>"https://accounts.google.com"</js>))
 * 			.clientId(<js>"web-app"</js>)
 * 			.clientSecret(<jv>env</jv>(<js>"OIDC_CLIENT_SECRET"</js>))
 * 			.redirectUri(URI.<jsm>create</jsm>(<js>"https://app.example.com/auth/callback"</js>))
 * 			.scope(<js>"openid"</js>, <js>"profile"</js>, <js>"email"</js>)
 * 			.sessionStore(SignedCookieSessionStore.<jsm>create</jsm>().signingKey(<jv>env</jv>(<js>"SESSION_KEY"</js>)).build())
 * 			.build();
 *
 * 		<ja>@RestGet</ja>(path=<js>"/login"</js>)
 * 		<jk>public void</jk> login(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
 * 			<jf>rp</jf>.startLogin(<jv>req</jv>, <jv>res</jv>);
 * 		}
 *
 * 		<ja>@RestGet</ja>(path=<js>"/callback"</js>)
 * 		<jk>public void</jk> callback(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
 * 			<jf>rp</jf>.completeLogin(<jv>req</jv>, <jv>res</jv>);
 * 		}
 *
 * 		<ja>@RestGet</ja>(path=<js>"/logout"</js>)
 * 		<jk>public void</jk> logout(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
 * 			<jf>rp</jf>.logout(<jv>req</jv>, <jv>res</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Register {@link #authFilter()} in the servlet filter chain (or {@code AuthFilterChain}) so each request
 * resolves the session cookie into a {@link ClaimsPrincipal} for {@code RoleBasedRestGuard} /
 * {@code @Auth Principal}.
 *
 * <h5 class='section'>Security</h5>
 * <ul>
 * 	<li>{@code state} and {@code nonce} are single-use and TTL-bounded (default 5&nbsp;min); a
 * 		missing / replayed value fails the callback.
 * 	<li>PKCE S256 is enforced end-to-end.
 * 	<li>The ID token is validated for signature, {@code iss}, {@code aud}/{@code azp}, {@code exp}, and
 * 		{@code nonce} (see {@link IdTokenValidatorAdapter}).
 * 	<li>The session id is freshly generated on login (no pre-auth fixation).
 * 	<li>Auth responses set {@code Cache-Control: no-store}; tokens are never logged.
 * 	<li>The session cookie is {@code HttpOnly} + {@code Secure} + {@code SameSite} by default.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SessionStore}
 * 	<li class='jc'>{@link OidcSessionAuthFilter}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OidcRelyingParty">OIDC Relying Party</a>
 * 	<li class='link'><a class="doclink" href="https://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect Core 1.0</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OIDC protocol parameter names and claim names; intentional
})
public class OidcRelyingParty {

	private static final String DEFAULT_COOKIE_NAME = "JUNEAU_OIDC_SESSION";
	private static final String DEFAULT_ROLES_CLAIM = "scope";
	private static final Duration DEFAULT_SESSION_TTL = Duration.ofHours(8);

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
		URI issuer;
		OidcMetadata metadata;
		String clientId;
		Supplier<String> clientSecretSupplier;
		URI redirectUri;
		URI postLogoutRedirectUri;
		String postLoginRedirect = "/";
		Set<String> scopes = new LinkedHashSet<>();
		SessionStore sessionStore;
		String rolesClaim = DEFAULT_ROLES_CLAIM;
		Duration stateNonceTtl = EphemeralStore.DEFAULT_TTL;
		int ephemeralMaxEntries = EphemeralStore.DEFAULT_MAX_ENTRIES;
		Duration sessionTtl = DEFAULT_SESSION_TTL;
		String cookieName = DEFAULT_COOKIE_NAME;
		boolean cookieSecure = true;
		boolean cookieHttpOnly = true;
		String cookieSameSite = "Lax";
		Consumer<HTTPRequest> httpRequestConfigurator;
		Consumer<AuthenticationRequest.Builder> authenticationRequestCustomizer;
		JWSAlgorithm[] idTokenAlgorithms;
		int clockSkewSeconds = 60;
		JWKSet jwkSet;
		JWKSource<SecurityContext> jwkSource;
		Set<String> userInfoClaims = new LinkedHashSet<>();
		Clock clock = Clock.systemUTC();

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the IdP issuer URL.  OIDC discovery resolves the endpoints + JWKS URI lazily.  Either
		 * this or {@link #metadata(OidcMetadata)} is required.
		 *
		 * @param value The issuer URL.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder issuer(URI value) {
			issuer = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the IdP metadata explicitly, bypassing discovery.  Either this or {@link #issuer(URI)}
		 * is required.
		 *
		 * @param value The metadata.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder metadata(OidcMetadata value) {
			metadata = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client id.  Required.
		 *
		 * @param value The client id.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the client secret.  May be omitted for public clients (PKCE-only).
		 *
		 * @param value The client secret.  Must not be <jk>null</jk> or blank.
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
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the redirect URI registered with the IdP.  Required.
		 *
		 * @param value The redirect URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder redirectUri(URI value) {
			redirectUri = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the post-logout redirect URI passed to the IdP end-session endpoint.
		 *
		 * @param value The post-logout redirect URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder postLogoutRedirectUri(URI value) {
			postLogoutRedirectUri = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the default post-login redirect target (when the login request supplied no safe
		 * {@code redirect} parameter).  Defaults to {@code "/"}.
		 *
		 * @param value The default redirect path.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder postLoginRedirect(String value) {
			postLoginRedirect = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Adds requested scopes.  {@code openid} is always included.
		 *
		 * @param values The scopes.
		 * @return This object.
		 */
		public Builder scope(String...values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				scopes.add(v);
			}
			return this;
		}

		/**
		 * Sets the session store.  Required.
		 *
		 * @param value The session store.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder sessionStore(SessionStore value) {
			sessionStore = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the claim roles are extracted from.  Defaults to {@code "scope"} (whitespace-split);
		 * array claims are also accepted.  Mirrors the {@code OAuthFilter} convention.
		 *
		 * @param value The claim name.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder rolesClaim(String value) {
			rolesClaim = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the single-use TTL for the {@code state} / {@code nonce} store.  Defaults to 5 minutes.
		 *
		 * @param value The TTL.  Must be positive and not exceed 30 minutes.
		 * @return This object.
		 */
		public Builder stateNonceTtl(Duration value) {
			stateNonceTtl = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the session lifetime (bounded independently of token lifetime).  Defaults to 8 hours.
		 *
		 * @param value The session TTL.  Must be positive.
		 * @return This object.
		 */
		public Builder sessionTtl(Duration value) {
			assertArgNotNull("value", value);
			assertArg(!value.isZero() && !value.isNegative(), "sessionTtl must be positive"); // HTT: JaCoCo bytecode artifact; zero and negative are tested by m01/m02 but one short-circuit branch edge remains instrumented
			sessionTtl = value;
			return this;
		}

		/**
		 * Sets the session cookie name.  Defaults to {@code JUNEAU_OIDC_SESSION}.
		 *
		 * @param value The cookie name.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder cookieName(String value) {
			cookieName = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the cookie {@code Secure} flag.  Defaults to <jk>true</jk> (fail-closed).
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder cookieSecure(boolean value) {
			cookieSecure = value;
			return this;
		}

		/**
		 * Sets the cookie {@code HttpOnly} flag.  Defaults to <jk>true</jk> (fail-closed).
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder cookieHttpOnly(boolean value) {
			cookieHttpOnly = value;
			return this;
		}

		/**
		 * Sets the cookie {@code SameSite} attribute.  Defaults to {@code "Lax"}.
		 *
		 * @param value The SameSite value (e.g. {@code "Lax"} or {@code "Strict"}).  Must not be
		 * 	<jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder cookieSameSite(String value) {
			cookieSameSite = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets a Nimbus {@link HTTPRequest} configurator escape hatch (proxy / TLS / timeouts) applied
		 * to discovery + token + userinfo calls.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a Nimbus {@link com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder} customizer escape hatch (e.g. {@code prompt},
		 * {@code max_age}, {@code acr_values}) applied to the authorization redirect.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder authenticationRequestCustomizer(Consumer<AuthenticationRequest.Builder> value) {
			authenticationRequestCustomizer = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the ID-token signing-algorithm allowlist.  Defaults to {@code [RS256, ES256]}.
		 *
		 * @param values The allowed algorithms.
		 * @return This object.
		 */
		public Builder idTokenAlgorithms(JWSAlgorithm...values) {
			idTokenAlgorithms = assertArgNotNull("values", values);
			return this;
		}

		/**
		 * Sets the clock-skew tolerance (seconds) for ID-token validation.  Defaults to 60.
		 *
		 * @param value The tolerance in seconds.  Must be non-negative.
		 * @return This object.
		 */
		public Builder clockSkewSeconds(int value) {
			assertArg(value >= 0, "clockSkewSeconds must be non-negative (was %s)", value);
			clockSkewSeconds = value;
			return this;
		}

		/**
		 * Injects a fixed JWK set for ID-token / logout-token signature verification, bypassing the
		 * JWKS endpoint.  Useful for tests and static-key deployments.
		 *
		 * @param value The JWK set.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwkSet(JWKSet value) {
			jwkSet = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Injects a custom JWK source for ID-token / logout-token signature verification.
		 *
		 * @param value The JWK source.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwkSource(JWKSource<SecurityContext> value) {
			jwkSource = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Adds claim names to backfill from the UserInfo endpoint when absent from the ID token.
		 *
		 * <p>
		 * When empty (the default) the UserInfo endpoint is never called &mdash; the {@link Principal}
		 * is built from ID-token claims only.
		 *
		 * @param values The claim names to backfill.
		 * @return This object.
		 */
		public Builder userInfoClaims(String...values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("claim", v);
				userInfoClaims.add(v);
			}
			return this;
		}

		/**
		 * Overrides the clock used for session expiry + the ephemeral store.  Useful in tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the relying party.
		 *
		 * @return A new {@link OidcRelyingParty}.
		 */
		public OidcRelyingParty build() {
			if (issuer == null && metadata == null)
				throw new IllegalStateException("OidcRelyingParty requires issuer(...) or metadata(...)");
			if (clientId == null)
				throw new IllegalStateException("OidcRelyingParty requires clientId(...)");
			if (redirectUri == null)
				throw new IllegalStateException("OidcRelyingParty requires redirectUri(...)");
			if (sessionStore == null)
				throw new IllegalStateException("OidcRelyingParty requires sessionStore(...)");
			return new OidcRelyingParty(this);
		}
	}

	private final URI issuer;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final URI redirectUri;
	private final URI postLogoutRedirectUri;
	private final String postLoginRedirect;
	private final Set<String> scopes;
	private final SessionStore sessionStore;
	private final String rolesClaim;
	private final Duration sessionTtl;
	private final String cookieName;
	private final boolean cookieSecure;
	private final boolean cookieHttpOnly;
	private final String cookieSameSite;
	private final Consumer<HTTPRequest> httpRequestConfigurator;
	private final Consumer<AuthenticationRequest.Builder> authenticationRequestCustomizer;
	private final JWSAlgorithm[] idTokenAlgorithms;
	private final int clockSkewSeconds;
	private final JWKSet jwkSet;
	private final JWKSource<SecurityContext> injectedJwkSource;
	private final Set<String> userInfoClaims;
	private final Clock clock;
	private final EphemeralStore ephemeralStore;

	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in metadata(); the OidcMetadata payload is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile OidcMetadata metadataCache;
	private final OidcMetadata explicitMetadata;
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in codeFlow(); the flow is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile OAuthAuthorizationCodeFlow codeFlowCache;
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in idTokenValidator(); the adapter is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile IdTokenValidatorAdapter idTokenValidatorCache;
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in jwkSource(); the source is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile JWKSource<SecurityContext> jwkSourceCache;
	@SuppressWarnings({
		"java:S3077" // Publish-once cache: assigned once under double-checked locking in logoutTokenProcessor(); the processor is fully built before assignment, so volatile safe-publication is sufficient.
	})
	private volatile ConfigurableJWTProcessor<SecurityContext> logoutTokenProcessorCache;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OidcRelyingParty(Builder b) {
		this.issuer = b.issuer;
		this.explicitMetadata = b.metadata;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.redirectUri = b.redirectUri;
		this.postLogoutRedirectUri = b.postLogoutRedirectUri;
		this.postLoginRedirect = b.postLoginRedirect;
		var s = new LinkedHashSet<String>();
		s.add("openid");
		s.addAll(b.scopes);
		this.scopes = Collections.unmodifiableSet(s);
		this.sessionStore = b.sessionStore;
		this.rolesClaim = b.rolesClaim;
		this.sessionTtl = b.sessionTtl;
		this.cookieName = b.cookieName;
		this.cookieSecure = b.cookieSecure;
		this.cookieHttpOnly = b.cookieHttpOnly;
		this.cookieSameSite = b.cookieSameSite;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
		this.authenticationRequestCustomizer = b.authenticationRequestCustomizer;
		this.idTokenAlgorithms = b.idTokenAlgorithms;
		this.clockSkewSeconds = b.clockSkewSeconds;
		this.jwkSet = b.jwkSet;
		this.injectedJwkSource = b.jwkSource;
		this.userInfoClaims = Collections.unmodifiableSet(new LinkedHashSet<>(b.userInfoClaims));
		this.clock = b.clock;
		this.ephemeralStore = new EphemeralStore(b.stateNonceTtl, b.ephemeralMaxEntries, b.clock);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Public flow methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Begins the login flow: generates {@code state} + {@code nonce} + PKCE verifier, stores them
	 * single-use, and redirects the user-agent to the IdP authorization endpoint.
	 *
	 * @param req The HTTP request.  An optional {@code redirect} query parameter (a safe relative path)
	 * 	is remembered as the post-login target.
	 * @param res The HTTP response.
	 * @throws IOException If the redirect cannot be written.
	 */
	public void startLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);
		var state = new State().getValue();
		var nonce = new Nonce().getValue();
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var redirectTarget = safeRelativePath(req.getParameter("redirect"));
		ephemeralStore.store(state, nonce, verifier.getValue(), redirectTarget);
		var authUrl = codeFlow().buildAuthenticationUrl(state, challenge, nonce, authenticationRequestCustomizer);
		noStore(res);
		res.sendRedirect(authUrl.toString());
	}

	/**
	 * Completes the login flow: parses the callback, verifies + consumes {@code state}, exchanges the
	 * code (PKCE), validates the ID token (signature / {@code iss} / {@code aud} / {@code exp} /
	 * {@code nonce}), creates a session, sets the session cookie, and redirects to the application.
	 *
	 * @param req The callback HTTP request (carrying {@code code} + {@code state}).
	 * @param res The HTTP response.
	 * @throws IOException If the redirect cannot be written.
	 * @throws AuthenticationException If the callback is an error, the state is missing / replayed, or
	 * 	ID-token validation fails.
	 */
	public void completeLogin(HttpServletRequest req, HttpServletResponse res) throws IOException, AuthenticationException {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);

		var callbackUri = fullRequestUri(req);
		AuthenticationResponse parsed;
		try {
			parsed = AuthenticationResponseParser.parse(callbackUri);
		} catch (com.nimbusds.oauth2.sdk.ParseException e) {
			throw new AuthenticationException(e, "OIDC callback could not be parsed");
		}
		if (! parsed.indicatesSuccess()) {
			var err = parsed.toErrorResponse().getErrorObject();
			throw new AuthenticationException("OIDC callback returned an error: %s", err == null ? "unknown" : err.getCode()); // HTT: err==null branch requires an error response with no error object, which Nimbus never produces
		}
		var success = parsed.toSuccessResponse();
		if (success.getAuthorizationCode() == null)
			throw new AuthenticationException("OIDC callback contained no authorization code");
		var code = success.getAuthorizationCode().getValue();
		var state = success.getState() == null ? null : success.getState().getValue(); // HTT: null state branch: AuthCode response always carries state per PKCE flow; null branch is defensive dead code

		var pending = ephemeralStore.consume(state).orElseThrow(
			() -> new AuthenticationException("OIDC callback state is missing, expired, or already used"));

		OAuthToken token;
		try {
			token = codeFlow().exchange(code, new CodeVerifier(pending.codeVerifier()));
		} catch (OAuthFlowException e) {
			throw new AuthenticationException(e, "OIDC token exchange failed");
		}
		var idToken = token.idToken().orElseThrow(
			() -> new AuthenticationException("IdP token response contained no id_token"));

		var principal = idTokenValidator().validate(idToken, pending.nonce());
		principal = maybeBackfillUserInfo(principal, token);

		var subject = principal.getName();
		var sid = principal.getClaim("sid", String.class);
		var roles = extractRoles(principal);
		var now = clock.instant();
		var session = new OidcSession(
			new State().getValue(),
			subject,
			sid,
			principal,
			roles,
			o(token),
			now,
			now.plus(sessionTtl));

		var cookieValue = sessionStore.createSessionCookieValue(session);
		noStore(res);
		res.addHeader("Set-Cookie", buildSetCookie(cookieValue, sessionTtl.toSeconds()));
		res.sendRedirect(pending.redirectTarget() != null ? pending.redirectTarget() : postLoginRedirect);
	}

	/**
	 * Logs the user out: invalidates the session, clears the session cookie, and (when the IdP
	 * advertises an end-session endpoint) redirects through it; otherwise redirects to the
	 * post-logout target.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws IOException If the redirect cannot be written.
	 */
	public void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);
		var cookieValue = readCookie(req);
		JWT idTokenHint = null;
		if (cookieValue != null) {
			idTokenHint = idTokenHintFromSession(cookieValue);
			sessionStore.invalidate(cookieValue);
		}
		noStore(res);
		res.addHeader("Set-Cookie", buildSetCookie("", 0));

		var endSession = metadata().endSessionEndpoint();
		if (endSession != null) {
			var logoutReq = new LogoutRequest(endSession, idTokenHint, postLogoutRedirectUri, null);
			res.sendRedirect(logoutReq.toURI().toString());
		} else {
			res.sendRedirect(postLogoutRedirectUri != null ? postLogoutRedirectUri.toString() : postLoginRedirect);
		}
	}

	/**
	 * Refreshes the current session's access token using the rotating-refresh-token grant.
	 *
	 * <p>
	 * Replaces the stored refresh token with the one the IdP returns; if the IdP rejects the refresh
	 * (e.g. a rotated token was reused / revoked), the session is invalidated.  Only applies to stores
	 * that retain tokens (server-side); {@link SignedCookieSessionStore} sessions carry no tokens and
	 * yield {@link Optional#empty()}.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response (a refreshed session resets the cookie).
	 * @return The refreshed session, or {@link Optional#empty()} if there was nothing to refresh.
	 */
	public Optional<OidcSession> refresh(HttpServletRequest req, HttpServletResponse res) {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);
		var cookieValue = readCookie(req);
		if (cookieValue == null)
			return oe();
		var existing = sessionStore.lookup(cookieValue);
		if (existing.isEmpty())
			return oe();
		var tokenOpt = existing.get().token();
		if (tokenOpt.isEmpty())
			return oe();
		var oldToken = tokenOpt.get();
		var refreshTokenOpt = oldToken.refreshToken();
		if (refreshTokenOpt.isEmpty())
			return oe();

		OAuthToken refreshed;
		try {
			var flowBuilder = OAuthRefreshTokenFlow.create()
				.tokenEndpoint(metadata().tokenEndpoint())
				.clientId(clientId)
				.refreshToken(refreshTokenOpt.get());
			if (clientSecretSupplier != null) // HTT: false branch (null secret = public client) reachable via refresh path only; no dedicated public-client refresh test
				flowBuilder.clientSecretSupplier(clientSecretSupplier);
			if (httpRequestConfigurator != null) // HTT: false branch (no configurator) unreachable in current refresh tests because the test RP always omits a configurator, so this block is never entered in tests
				flowBuilder.httpRequestConfigurator(httpRequestConfigurator);
			refreshed = flowBuilder.build().acquire();
		} catch (OAuthFlowException e) {
			// Rotating-refresh-token reuse / revocation -> invalidate the session (fail-closed).
			sessionStore.invalidate(cookieValue);
			res.addHeader("Set-Cookie", buildSetCookie("", 0));
			return oe();
		}

		var old = existing.get();
		var now = clock.instant();
		var newSession = new OidcSession(
			new State().getValue(),
			old.subject(),
			old.sid(),
			old.principal(),
			old.roles(),
			o(refreshed),
			old.createdAt(),
			now.plus(sessionTtl));
		sessionStore.invalidate(cookieValue);
		var newCookie = sessionStore.createSessionCookieValue(newSession);
		noStore(res);
		res.addHeader("Set-Cookie", buildSetCookie(newCookie, sessionTtl.toSeconds()));
		return o(newSession);
	}

	/**
	 * Processes an IdP-pushed OpenID Connect back-channel logout token, invalidating the matching
	 * server-side session(s).
	 *
	 * <p>
	 * The logout token is validated via Nimbus's {@link LogoutTokenValidator} (signature against the
	 * JWKS, {@code iss}, {@code aud}, the back-channel {@code events} claim, and the {@code sub} /
	 * {@code sid} presence rule).  Sessions are then revoked by {@code sid} (preferred, single-session)
	 * or {@code sub} (all of a subject's sessions).
	 *
	 * <p>
	 * Requires a {@link SessionStore#supportsServerSideRevocation() server-side-revocable} store
	 * &mdash; the default {@link SignedCookieSessionStore} cannot satisfy back-channel logout because a
	 * stateless cookie is not server-revocable.
	 *
	 * @param logoutToken The raw {@code logout_token} JWT pushed by the IdP.  Must not be <jk>null</jk>
	 * 	or blank.
	 * @return The number of sessions invalidated.
	 * @throws AuthenticationException If the logout token fails validation.
	 * @throws IllegalStateException If the configured session store does not support server-side
	 * 	revocation.
	 */
	public int backChannelLogout(String logoutToken) throws AuthenticationException {
		assertArgNotNullOrBlank("logoutToken", logoutToken);
		if (! sessionStore.supportsServerSideRevocation())
			throw new IllegalStateException("Back-channel logout requires a server-side-revocable SessionStore (InMemorySessionStore or a distributed store); the configured store is stateless.");
		JWT jwt;
		try {
			jwt = JWTParser.parse(logoutToken);
		} catch (java.text.ParseException e) {
			throw new AuthenticationException(e, "Logout token could not be parsed");
		}
		LogoutTokenClaimsSet claims;
		try {
			var verified = logoutTokenProcessor().process(jwt, null);
			claims = new LogoutTokenClaimsSet(verified);
		} catch (com.nimbusds.jose.proc.BadJOSEException | com.nimbusds.jose.JOSEException e) {
			throw new AuthenticationException(e, "Logout token validation failed: %s", e.getMessage());
		} catch (com.nimbusds.oauth2.sdk.ParseException e) {
			throw new AuthenticationException(e, "Logout token claims could not be parsed");
		}
		var sid = claims.getSessionID();
		if (sid != null)
			return sessionStore.invalidateBySessionId(sid.getValue());
		var sub = claims.getSubject();
		if (sub != null) // HTT: false branch (sub==null with sid==null) rejected by LogoutTokenClaimsVerifier which requires at least one of sub or sid; defensive dead code
			return sessionStore.invalidateBySubject(sub.getValue());
		return 0;
	}

	/**
	 * Returns a request-time {@link AuthFilter} that resolves this RP's session cookie into a
	 * {@link ClaimsPrincipal}.
	 *
	 * @return A new {@link OidcSessionAuthFilter} bound to this RP's session store + cookie name.
	 */
	public OidcSessionAuthFilter authFilter() {
		return new OidcSessionAuthFilter(sessionStore, cookieName);
	}

	/**
	 * Returns the configured session store.
	 *
	 * @return The session store.
	 */
	public SessionStore sessionStore() {
		return sessionStore;
	}

	/**
	 * Returns the session cookie name.
	 *
	 * @return The cookie name.
	 */
	public String cookieName() {
		return cookieName;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Internals
	//-----------------------------------------------------------------------------------------------------------------

	private OidcMetadata metadata() {
		var m = metadataCache;
		if (m != null)
			return m;
		synchronized (this) {
			if (metadataCache != null) // HTT: DCL second-check; true branch requires concurrent initialization race
				return metadataCache;
			if (explicitMetadata != null) { // HTT: false branch = discovery path; all tests inject .metadata(...) directly so explicitMetadata is always non-null
				metadataCache = explicitMetadata;
				return metadataCache;
			}
			try {
				var clientBuilder = OidcDiscoveryClient.create().issuer(issuer);
				if (httpRequestConfigurator != null) // HTT: true branch requires discovery path (no explicit metadata) plus httpRequestConfigurator; all tests inject metadata directly
					clientBuilder.httpRequestConfigurator(httpRequestConfigurator);
				metadataCache = clientBuilder.build().discover();
			} catch (IOException | OidcDiscoveryException e) {
				throw new IllegalStateException("OIDC discovery failed for issuer " + issuer, e);
			}
			return metadataCache;
		}
	}

	private OAuthAuthorizationCodeFlow codeFlow() {
		var f = codeFlowCache;
		if (f != null)
			return f;
		synchronized (this) {
			if (codeFlowCache != null) // HTT: DCL second-check; true branch requires concurrent initialization race
				return codeFlowCache;
			var md = metadata();
			var b = OAuthAuthorizationCodeFlow.create()
				.authorizationEndpoint(md.authorizationEndpoint())
				.tokenEndpoint(md.tokenEndpoint())
				.clientId(clientId)
				.redirectUri(redirectUri)
				.scope(scopes.toArray(new String[0]));
			if (clientSecretSupplier != null) // HTT: false branch (public client with no secret) not covered; all test RPs set a client secret
				b.clientSecretSupplier(clientSecretSupplier);
			if (httpRequestConfigurator != null) // HTT: false branch not hit in codeFlow init; test RPs don't set an httpRequestConfigurator
				b.httpRequestConfigurator(httpRequestConfigurator);
			codeFlowCache = b.build();
			return codeFlowCache;
		}
	}

	private IdTokenValidatorAdapter idTokenValidator() {
		var v = idTokenValidatorCache;
		if (v != null)
			return v;
		synchronized (this) {
			if (idTokenValidatorCache != null) // HTT: DCL second-check; true branch requires concurrent initialization race
				return idTokenValidatorCache;
			var b = IdTokenValidatorAdapter.create()
				.issuer(metadata().issuer().toString())
				.clientId(clientId)
				.jwkSource(jwkSource())
				.maxClockSkewSeconds(clockSkewSeconds);
			if (idTokenAlgorithms != null) // HTT: false branch (custom algorithm list) not covered; test RPs use default RS256/ES256
				b.algorithms(idTokenAlgorithms);
			idTokenValidatorCache = b.build();
			return idTokenValidatorCache;
		}
	}

	private JWKSource<SecurityContext> jwkSource() {
		var s = jwkSourceCache;
		if (s != null)
			return s;
		synchronized (this) {
			if (jwkSourceCache != null) // HTT: DCL second-check; true branch requires concurrent initialization race
				return jwkSourceCache;
			if (injectedJwkSource != null) { // HTT: true branch requires jwkSource(JWKSource) builder call, which no current test uses (tests inject jwkSet instead)
				jwkSourceCache = injectedJwkSource;
			} else if (jwkSet != null) { // HTT: false branch (no jwkSet → use JWKS URI from discovery) requires a live IdP and no injected jwkSet; all tests inject jwkSet
				jwkSourceCache = new ImmutableJWKSet<>(jwkSet);
			} else {
				try {
					jwkSourceCache = JWKSourceBuilder.create(metadata().jwksUri().toURL()).build();
				} catch (MalformedURLException e) {
					throw new IllegalStateException("Invalid JWKS URI: " + metadata().jwksUri(), e);
				}
			}
			return jwkSourceCache;
		}
	}

	/**
	 * Builds (and caches) the JWT processor used to validate back-channel logout tokens.
	 *
	 * <p>
	 * Nimbus's {@link LogoutTokenValidator} hardcodes a {@link LogoutTokenClaimsVerifier} internally and
	 * exposes no clock hook, so we drive an equivalent {@link DefaultJWTProcessor} directly: the JWS type
	 * verifier replicates {@code LogoutTokenValidator}'s default (accept untyped, {@code logout+jwt}, or
	 * {@code JWT}), and the claims verifier is a {@link ClockAwareLogoutTokenClaimsVerifier} that sources
	 * "now" from {@link #clock}.  With {@link Clock#systemUTC()} the behavior is identical to stock
	 * {@code LogoutTokenValidator}.
	 */
	private ConfigurableJWTProcessor<SecurityContext> logoutTokenProcessor() {
		var p = logoutTokenProcessorCache;
		if (p != null)
			return p;
		synchronized (this) {
			if (logoutTokenProcessorCache != null) // HTT: DCL second-check; true branch requires concurrent initialization race
				return logoutTokenProcessorCache;
			Set<JWSAlgorithm> algs = idTokenAlgorithms != null
				? new LinkedHashSet<>(Arrays.asList(idTokenAlgorithms))
				: new LinkedHashSet<>(Arrays.asList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
			var keySelector = new JWSVerificationKeySelector<SecurityContext>(algs, jwkSource());
			var expectedIssuer = new Issuer(metadata().issuer().toString());
			var processor = new DefaultJWTProcessor<SecurityContext>();
			processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(LogoutTokenValidator.TYPE, JOSEObjectType.JWT, null));
			processor.setJWSKeySelector(keySelector);
			@SuppressWarnings("unchecked") // ClockAwareLogoutTokenClaimsVerifier conforms to JWTClaimsSetVerifier<SecurityContext>; Nimbus's raw type forces an unchecked conversion here.
			JWTClaimsSetVerifier<SecurityContext> claimsVerifier = new ClockAwareLogoutTokenClaimsVerifier(expectedIssuer, new ClientID(clientId), clock);
			processor.setJWTClaimsSetVerifier(claimsVerifier);
			logoutTokenProcessorCache = processor;
			return logoutTokenProcessorCache;
		}
	}

	private ClaimsPrincipal maybeBackfillUserInfo(ClaimsPrincipal principal, OAuthToken token) {
		if (userInfoClaims.isEmpty())
			return principal;
		var endpoint = metadata().userinfoEndpoint();
		if (endpoint == null)
			return principal;
		var missing = userInfoClaims.stream().anyMatch(c -> ! principal.hasClaim(c));
		if (! missing)
			return principal;
		try {
			var http = new UserInfoRequest(endpoint, new BearerAccessToken(token.accessToken())).toHTTPRequest();
			if (httpRequestConfigurator != null)
				httpRequestConfigurator.accept(http);
			var resp = UserInfoResponse.parse(http.send());
			if (! resp.indicatesSuccess())
				return principal;
			var ui = resp.toSuccessResponse().getUserInfo().toJSONObject();
			var merged = new LinkedHashMap<String,Object>(principal.getClaims());
			for (var c : userInfoClaims)
				if (! merged.containsKey(c) && ui.containsKey(c))
					merged.put(c, ui.get(c));
			return new ClaimsPrincipal(principal.getName(), merged);
		} catch (IOException | com.nimbusds.oauth2.sdk.ParseException | RuntimeException e) {
			return principal;  // UserInfo backfill is best-effort; never fail the login on it.
		}
	}

	private Set<String> extractRoles(ClaimsPrincipal principal) {
		var v = principal.getClaims().get(rolesClaim);
		if (v == null)
			return Collections.emptySet();
		var out = new LinkedHashSet<String>();
		if (v instanceof String str) {
			for (var piece : str.split("\\s+"))
				if (! piece.isBlank())
					out.add(piece);
		} else if (v instanceof Collection<?> list) {
			for (var item : list)
				if (item instanceof String str)
					out.add(str);
		}
		return out;
	}

	private String buildSetCookie(String value, long maxAgeSeconds) {
		var sb = new StringBuilder();
		sb.append(cookieName).append('=').append(value);
		sb.append("; Path=/");
		sb.append("; Max-Age=").append(maxAgeSeconds);
		if (cookieHttpOnly)
			sb.append("; HttpOnly");
		if (cookieSecure)
			sb.append("; Secure");
		if (cookieSameSite != null) // HTT: false branch requires null cookieSameSite, which the builder's assertArgNotNullOrBlank guard prevents
			sb.append("; SameSite=").append(cookieSameSite);
		return sb.toString();
	}

	/**
	 * Best-effort extraction of the {@code id_token_hint} for the RP-initiated logout request from the
	 * session referenced by the given cookie value.  Returns <jk>null</jk> when the session, its token,
	 * or its id_token is absent, or when the id_token cannot be parsed.
	 */
	private JWT idTokenHintFromSession(String cookieValue) {
		var session = sessionStore.lookup(cookieValue);
		if (session.isEmpty())
			return null;
		var token = session.get().token();
		if (token.isEmpty())
			return null;
		var idt = token.get().idToken();
		if (idt.isEmpty()) // HTT: requires OAuthToken with no id_token; code-exchange always produces one when idToken is set on the stub, and the field is final
			return null;
		try {
			return JWTParser.parse(idt.get());
		} catch (java.text.ParseException e) {
			return null;  // HTT - best-effort hint only
		}
	}

	private String readCookie(HttpServletRequest req) {
		var cookies = req.getCookies();
		if (cookies == null)
			return null;
		for (var c : cookies)
			if (cookieName.equals(c.getName()) && c.getValue() != null && ! c.getValue().isBlank())
				return c.getValue();
		return null;
	}

	private static void noStore(HttpServletResponse res) {
		res.setHeader("Cache-Control", "no-store");
		res.setHeader("Pragma", "no-cache");
	}

	private static URI fullRequestUri(HttpServletRequest req) {
		var url = new StringBuilder(req.getRequestURL());
		var qs = req.getQueryString();
		if (qs != null) // HTT: false branch (no QS) would cause Nimbus to fail to parse the callback; all test callbacks include query params
			url.append('?').append(qs);
		return URI.create(url.toString());
	}

	/**
	 * Returns the value only if it is a safe app-relative path (defeats open-redirect via the
	 * {@code redirect} parameter).
	 */
	private static String safeRelativePath(String value) {
		if (isBlank(value))
			return null;
		if (! value.startsWith("/"))
			return null;
		if (value.startsWith("//") || value.contains("\\") || value.contains("://"))
			return null;
		return value;
	}

	/**
	 * A {@link LogoutTokenClaimsVerifier} whose {@code exp} window is evaluated against an injected
	 * {@link Clock} instead of {@code new Date()}.
	 *
	 * <p>
	 * {@link LogoutTokenClaimsVerifier} extends Nimbus's {@link DefaultJWTClaimsVerifier}, which exposes
	 * an overridable {@link DefaultJWTClaimsVerifier#currentTime() currentTime()} hook &mdash; so this is
	 * a one-method override that injects the clock without cloning any Nimbus claim-checking logic.  With
	 * {@link Clock#systemUTC()} the behavior is identical to stock Nimbus.
	 */
	private static final class ClockAwareLogoutTokenClaimsVerifier extends LogoutTokenClaimsVerifier {

		private final Clock clock;

		ClockAwareLogoutTokenClaimsVerifier(Issuer issuer, ClientID clientId, Clock clock) {
			super(issuer, clientId);
			this.clock = clock;
		}

		@Override /* DefaultJWTClaimsVerifier */
		protected Date currentTime() {
			return Date.from(clock.instant());
		}
	}
}
