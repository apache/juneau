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

import static org.apache.juneau.rest.server.auth.oidc.rp.OidcTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.jwk.*;

import jakarta.servlet.http.*;

/**
 * End-to-end tests for {@link OidcRelyingParty} driving login &rarr; callback &rarr; authorized-request
 * &rarr; logout against an in-JVM {@link StubIdp}, plus refresh-token rotation and back-channel logout.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S8692", // Nimbus oauth2-oidc-sdk 11.37.2 exposes no clock hook on the ID-token path (IDTokenClaimsVerifier reads new Date() internally), so injecting a clock there would require cloning Nimbus internals; the logout-token path IS clock-injectable and is tested deterministically.
	"java:S1130", // Several test methods declare throws Exception for checked exceptions that MockServletResponse may propagate in other implementations; the declarations are intentionally broad.
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
	"java:S5976", // Similar-shaped login/redirect/cookie tests assert distinct authentication/session outcomes; parameterizing would obscure intent.
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class OidcRelyingParty_Test extends TestBase {

	private static final String CID = "web-app";
	private static final URI REDIRECT_URI = URI.create("https://app.example.com/auth/callback");
	private static final URI AUTHZ = URI.create("https://stub-idp.example.com/authorize");
	private static final URI END_SESSION = URI.create("https://stub-idp.example.com/logout");

	// Fixed clock for the logout-token path, which IS clock-injectable; the matching store clock keeps
	// session-expiry deterministic so the login() preamble survives the back-dated instant.
	private static final Instant FIXED_NOW = Instant.parse("2024-06-01T12:00:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

	private StubIdp idp;
	private RSAKey key;

	@BeforeEach void setup() throws Exception {
		key = generateRsa("k1");
		idp = new StubIdp();
	}

	@AfterEach void teardown() {
		if (idp != null)
			idp.close();
	}

	private OidcRelyingParty rp(SessionStore store) {
		return rp(store, Clock.systemUTC());
	}

	private OidcRelyingParty rp(SessionStore store, Clock clock) {
		return OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(store)
			.jwkSet(publicJwks(key))
			.clock(clock)
			.build();
	}

	/** Drives startLogin + completeLogin; returns the session cookie value. */
	private String login(OidcRelyingParty rp, String sub, String sid, String redirectParam) throws Exception {
		var loginUri = "/auth/login" + (redirectParam == null ? "" : "?redirect=" + redirectParam);
		var req1 = MockServletRequest.create("GET", loginUri);
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var loc = res1.getHeader("Location");
		assertNotNull(loc, "startLogin must redirect to the IdP");
		var state = queryParam(loc, "state");
		var nonce = queryParam(loc, "nonce");
		assertNotNull(state);
		assertNotNull(nonce);
		idp.idToken = signIdToken(key, idp.issuer, CID, sub, sid, nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", "openid profile"));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc123&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("no-store", res2.getHeader("Cache-Control"));
		return cookieValue(res2.getHeader("Set-Cookie"));
	}

	private Optional<AuthResult> authn(OidcRelyingParty rp, String cookie) throws Exception {
		var req = MockServletRequest.create("GET", "/api/resource")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		return rp.authFilter().authenticate(req);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Happy-path login
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_fullLoginFlow_inMemoryStore() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		assertNotNull(cookie);
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals("alice", auth.get().getPrincipal().getName());
		assertTrue(auth.get().getRoles().contains("openid"));
		assertTrue(auth.get().getRoles().contains("profile"));
	}

	@Test void a02_fullLoginFlow_signedCookieStore() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build());
		var cookie = login(rp, "alice", "sess-1", null);
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals("alice", auth.get().getPrincipal().getName());
	}

	@Test void a03_completeLogin_setsSecureCookieFlags() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		var setCookie = res2.getHeader("Set-Cookie");
		assertTrue(setCookie.contains("HttpOnly"));
		assertTrue(setCookie.contains("Secure"));
		assertTrue(setCookie.contains("SameSite=Lax"));
	}

	@Test void a04_redirectTarget_isHonored() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=/dashboard");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/dashboard", res2.getHeader("Location"));
	}

	@Test void a05_openRedirect_isBlocked() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=https://evil.example.com");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"), "absolute redirect target must be rejected (open-redirect defense)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Callback failure modes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_replayedState_isRejected() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5), null);
		rp.completeLogin(MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state), MockServletResponse.create());
		// Second callback with the same (already-consumed) state must fail.
		var replayReq = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var replayRes = MockServletResponse.create();
		assertThrows(AuthenticationException.class, () -> rp.completeLogin(replayReq, replayRes));
	}

	@Test void b02_unknownState_isRejected() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=never-issued");
		var res = MockServletResponse.create();
		assertThrows(AuthenticationException.class, () -> rp.completeLogin(req, res));
	}

	@Test void b03_badNonce_isRejected() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", "WRONG-NONCE", Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		assertThrows(AuthenticationException.class, () -> rp.completeLogin(req2, res2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Logout
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_logout_clearsCookieAndRedirectsThroughEndSession() throws Exception {
		var store = InMemorySessionStore.create();
		var rp = rp(store);
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/logout").cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertTrue(res.getHeader("Set-Cookie").contains("Max-Age=0"), "logout must clear the cookie");
		assertTrue(res.getHeader("Location").startsWith(END_SESSION.toString()), "logout should redirect through the IdP end-session endpoint");
		assertTrue(authn(rp, cookie).isEmpty(), "session must be invalidated after logout");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Refresh-token rotation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_refresh_rotatesTokenAndSession() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/refresh").cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		var res = MockServletResponse.create();
		var refreshed = rp.refresh(req, res);
		assertTrue(refreshed.isPresent());
		assertEquals("rt-2", refreshed.get().token().orElseThrow().refreshToken().orElse(null), "rotating refresh token must be replaced");
		assertNotNull(res.getHeader("Set-Cookie"));
	}

	@Test void d02_refresh_reuseRejectedInvalidatesSession() throws Exception {
		var store = InMemorySessionStore.create();
		var rp = rp(store);
		var cookie = login(rp, "alice", "sess-1", null);
		idp.failToken = true;  // IdP rejects the refresh (rotated token reuse / revocation)
		var req = MockServletRequest.create("GET", "/auth/refresh").cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		var res = MockServletResponse.create();
		var refreshed = rp.refresh(req, res);
		assertTrue(refreshed.isEmpty());
		assertTrue(authn(rp, cookie).isEmpty(), "a rejected refresh must invalidate the session (fail-closed)");
	}

	@Test void d03_refresh_signedCookieStore_noToken_returnsEmpty() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/refresh").cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		assertTrue(rp.refresh(req, MockServletResponse.create()).isEmpty(), "cookie store retains no token, so there is nothing to refresh");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Back-channel logout
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_backChannelLogout_bySid_invalidatesSession() throws Exception {
		var rp = rp(new InMemorySessionStore(InMemorySessionStore.DEFAULT_MAX_ENTRIES, FIXED_CLOCK), FIXED_CLOCK);
		var cookie = login(rp, "alice", "sess-1", null);
		assertTrue(authn(rp, cookie).isPresent());
		var logoutToken = signLogoutToken(key, idp.issuer, CID, null, "sess-1", FIXED_NOW);
		assertEquals(1, rp.backChannelLogout(logoutToken));
		assertTrue(authn(rp, cookie).isEmpty());
	}

	@Test void e02_backChannelLogout_bySub_invalidatesAllSessions() throws Exception {
		var rp = rp(new InMemorySessionStore(InMemorySessionStore.DEFAULT_MAX_ENTRIES, FIXED_CLOCK), FIXED_CLOCK);
		var cookie1 = login(rp, "alice", "sess-1", null);
		var cookie2 = login(rp, "alice", "sess-2", null);
		var logoutToken = signLogoutToken(key, idp.issuer, CID, "alice", null, FIXED_NOW);
		assertEquals(2, rp.backChannelLogout(logoutToken));
		assertTrue(authn(rp, cookie1).isEmpty());
		assertTrue(authn(rp, cookie2).isEmpty());
	}

	@Test void e03_backChannelLogout_badSignature_isRejected() throws Exception {
		var rp = rp(new InMemorySessionStore(InMemorySessionStore.DEFAULT_MAX_ENTRIES, FIXED_CLOCK), FIXED_CLOCK);
		login(rp, "alice", "sess-1", null);
		var attacker = generateRsa("k1");
		var forged = signLogoutToken(attacker, idp.issuer, CID, null, "sess-1", FIXED_NOW);
		assertThrows(AuthenticationException.class, () -> rp.backChannelLogout(forged));
	}

	@Test void e04_backChannelLogout_signedCookieStore_throws() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build(), FIXED_CLOCK);
		var logoutToken = signLogoutToken(key, idp.issuer, CID, null, "sess-1", FIXED_NOW);
		assertThrows(IllegalStateException.class, () -> rp.backChannelLogout(logoutToken));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// completeLogin error branches
	//-----------------------------------------------------------------------------------------------------------------

	/** Callback carries error=access_denied — line 614 true branch. */
	@Test void f01_completeLogin_errorCallback_throws() {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", REDIRECT_URI + "?error=access_denied&state=x");
		var res = MockServletResponse.create();
		assertThrows(AuthenticationException.class, () -> rp.completeLogin(req, res));
	}

	/** No authorization code in the success response — line 619 true branch. */
	@Test void f02_completeLogin_noAuthCode_throws() throws Exception {
		// An implicit-response form has token= but no code=, so the success response carries no code.
		var rp = rp(InMemorySessionStore.create());
		// Start a login to get a valid state value.
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		// Construct a response_type=token implicit-grant URL — no code param, but Nimbus parses it as an
		// implicit success.  Providing access_token=x makes it parse as a success response.
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?access_token=x&token_type=Bearer&state=" + state);
		var res2 = MockServletResponse.create();
		assertThrows(AuthenticationException.class, () -> rp.completeLogin(req2, res2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// logout branches
	//-----------------------------------------------------------------------------------------------------------------

	/** No session cookie present — line 673 false branch (idTokenHint stays null, cookie cleared anyway). */
	@Test void g01_logout_noCookie_clearsAndRedirects() throws IOException {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", "/auth/logout");  // no cookies
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertTrue(res.getHeader("Set-Cookie").contains("Max-Age=0"), "cookie must be cleared even without a session");
		assertNotNull(res.getHeader("Location"), "must redirect");
	}

	/**
	 * Cookie present but session already expired/unknown — idTokenHintFromSession line 1023 true branch.
	 * logout must still clear the cookie and redirect.
	 */
	@Test void g01b_logout_staleSession_clearsAndRedirects() throws IOException {
		var store = InMemorySessionStore.create();
		var rp = rp(store);
		// Use a cookie value that doesn't correspond to any live session.
		var req = MockServletRequest.create("GET", "/auth/logout")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), "stale-value") });
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertTrue(res.getHeader("Set-Cookie").contains("Max-Age=0"));
		assertNotNull(res.getHeader("Location"));
	}

	/**
	 * Cookie present, session found, but session carries no token (signed-cookie store) —
	 * idTokenHintFromSession line 1026 true branch.
	 */
	@Test void g01c_logout_sessionNoToken_clearsAndRedirects() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/logout")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertTrue(res.getHeader("Set-Cookie").contains("Max-Age=0"));
		assertNotNull(res.getHeader("Location"));
	}

	/**
	 * Session has a token with an id_token present — happy path through idTokenHintFromSession
	 * (lines 1023/1026/1029 all take the false branch), id_token_hint passed to end-session endpoint.
	 */
	@Test void g01d_logout_sessionWithIdToken_hintIncludedInRedirect() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/logout")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertTrue(res.getHeader("Location").startsWith(END_SESSION.toString()));
		assertTrue(res.getHeader("Location").contains("id_token_hint="), "end-session redirect must include id_token_hint");
	}

	/** No endSession endpoint — line 681 false branch → redirect to postLogoutRedirectUri. */
	@Test void g02_logout_noEndSessionEndpoint_redirectsToPostLogoutUri() throws IOException {
		// Build metadata without an end-session endpoint.
		var meta = new OidcMetadata(
			URI.create(idp.issuer),
			idp.tokenEndpoint(),
			AUTHZ,
			null,
			URI.create(idp.baseUri() + "/jwks"),
			idp.userinfoEndpoint(),
			null,    // <-- no endSession
			Set.of("openid"),
			Map.of());
		var postLogout = URI.create("https://app.example.com/logged-out");
		var rp = OidcRelyingParty.create()
			.metadata(meta)
			.clientId(CID)
			.redirectUri(REDIRECT_URI)
			.postLogoutRedirectUri(postLogout)
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.build();
		var req = MockServletRequest.create("GET", "/auth/logout");
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertEquals(postLogout.toString(), res.getHeader("Location"));
	}

	/** No endSession endpoint and no postLogoutRedirectUri — line 685 null ternary. */
	@Test void g03_logout_noEndSession_noPostLogoutUri_redirectsToPostLoginRedirect() throws IOException {
		var meta = new OidcMetadata(
			URI.create(idp.issuer),
			idp.tokenEndpoint(),
			AUTHZ,
			null,
			URI.create(idp.baseUri() + "/jwks"),
			idp.userinfoEndpoint(),
			null,
			Set.of("openid"),
			Map.of());
		var rp = OidcRelyingParty.create()
			.metadata(meta)
			.clientId(CID)
			.redirectUri(REDIRECT_URI)
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.build();
		var req = MockServletRequest.create("GET", "/auth/logout");
		var res = MockServletResponse.create();
		rp.logout(req, res);
		assertEquals("/", res.getHeader("Location"), "must fall back to postLoginRedirect default '/'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// refresh cascade
	//-----------------------------------------------------------------------------------------------------------------

	/** No cookie present — line 706 true branch. */
	@Test void h01_refresh_noCookie_returnsEmpty() {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", "/auth/refresh");  // no cookies
		var res = MockServletResponse.create();
		assertTrue(rp.refresh(req, res).isEmpty());
	}

	/** Cookie present but no matching session — line 709 true branch. */
	@Test void h02_refresh_unknownCookie_returnsEmpty() {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", "/auth/refresh")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), "no-such-session") });
		var res = MockServletResponse.create();
		assertTrue(rp.refresh(req, res).isEmpty());
	}

	/** Session found but token absent (signed-cookie store) — line 712 true branch. */
	@Test void h03_refresh_noToken_returnsEmpty() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/refresh")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		assertTrue(rp.refresh(req, MockServletResponse.create()).isEmpty());
	}

	/**
	 * Session found, token present, but no refreshToken field — line 716 true branch.
	 *
	 * <p>Drive this by configuring the stub to return no refresh_token on the initial code exchange;
	 * InMemorySessionStore retains the full token so the session token is present but refreshToken is empty.
	 */
	@Test void h04_refresh_noRefreshToken_returnsEmpty() throws Exception {
		idp.refreshToken = null;  // stub returns no refresh_token in the code-exchange response
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		var req = MockServletRequest.create("GET", "/auth/refresh")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), cookie) });
		assertTrue(rp.refresh(req, MockServletResponse.create()).isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// maybeBackfillUserInfo branches
	//-----------------------------------------------------------------------------------------------------------------

	/** userInfoClaims is empty → UserInfo endpoint is never called — line 959 true branch. */
	@Test void i01_backfill_noClaims_skipsUserInfo() throws Exception {
		// Default RP has no userInfoClaims → backfill is skipped regardless of the endpoint presence.
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		// email was not backfilled because no userInfoClaims configured.
		assertTrue(((ClaimsPrincipal) auth.get().getPrincipal()).getClaim("email", String.class).isEmpty());
	}

	/**
	 * UserInfo returns a non-success response (stub returns 401) — line 972 true branch.
	 * Login must still succeed (backfill is best-effort).
	 */
	@Test void i01b_backfill_userinfoErrorResponse_loginSucceeds() throws Exception {
		// Replace the /userinfo handler with one that returns 401.
		idp.userInfoFail = true;
		idp.userInfo = new LinkedHashMap<>(Map.of("email", "alice@example.com"));
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.userInfoClaims("email")
			.build();
		var cookie = login(rp, "alice", "sess-1", null);
		// Login must succeed even though userinfo returned an error — backfill is best-effort.
		assertTrue(authn(rp, cookie).isPresent());
	}

	/** userInfoClaims non-empty, endpoint present, all claims already in id_token — line 965 true branch. */
	@Test void i02_backfill_allClaimsPresent_skipsUserInfo() throws Exception {
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.userInfoClaims("email")  // claim requested
			.build();
		// Sign id_token that ALREADY carries email → backfill should be skipped.
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("email", "alice@example.com", "scope", "openid profile"));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		var auth = authn(rp, cookieValue(res2.getHeader("Set-Cookie")));
		assertEquals("alice@example.com",
			((ClaimsPrincipal) auth.get().getPrincipal()).getClaim("email", String.class).orElse(null));
	}

	/**
	 * httpRequestConfigurator is applied to the UserInfo request — line 969 true branch.
	 * Also covers the ui.containsKey(c)==false sub-branch in line 977 (requested claim not in ui response).
	 */
	@Test void i02b_backfill_httpRequestConfigurator_applied() throws Exception {
		var configuratorCalled = new boolean[]{false};
		idp.userInfo = new LinkedHashMap<>(Map.of("email", "alice@example.com"));
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.userInfoClaims("email", "phone")  // phone not in userInfo → ui.containsKey false branch
			.httpRequestConfigurator(req -> configuratorCalled[0] = true)
			.build();
		var cookie = login(rp, "alice", "sess-1", null);
		assertTrue(authn(rp, cookie).isPresent());
		assertTrue(configuratorCalled[0], "httpRequestConfigurator must be called for the UserInfo request");
	}

	/** userInfoClaims non-empty, claim missing, UserInfo returns the extra claim — line 964 + 978 branches. */
	@Test void i03_backfill_missingClaim_backfilledFromUserInfo() throws Exception {
		idp.userInfo = new LinkedHashMap<>(Map.of("email", "alice@example.com"));
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.userInfoClaims("email")
			.build();
		var cookie = login(rp, "alice", "sess-1", null);
		var auth = authn(rp, cookie);
		assertEquals("alice@example.com",
			((ClaimsPrincipal) auth.get().getPrincipal()).getClaim("email", String.class).orElse(null));
	}

	/** userInfoClaims non-empty, endpoint null — line 962 true branch. */
	@Test void i04_backfill_noEndpoint_skipsUserInfo() throws Exception {
		var metaNoUserInfo = new OidcMetadata(
			URI.create(idp.issuer),
			idp.tokenEndpoint(),
			AUTHZ,
			null,
			URI.create(idp.baseUri() + "/jwks"),
			null,    // <-- no userinfo endpoint
			END_SESSION,
			Set.of("openid"),
			Map.of());
		var rp = OidcRelyingParty.create()
			.metadata(metaNoUserInfo)
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.userInfoClaims("email")
			.build();
		var cookie = login(rp, "alice", "sess-1", null);
		// No email in id_token, no endpoint → backfill silently skipped, login succeeds.
		assertTrue(authn(rp, cookie).isPresent());
	}

	/**
	 * Some requested userInfoClaims are already present in the id_token — line 977:
	 * {@code !merged.containsKey(c)} false branch when the id_token already carries the claim.
	 */
	@Test void i05_backfill_someClaimsAlreadyPresent_onlyMissingBackfilled() throws Exception {
		idp.userInfo = new LinkedHashMap<>(Map.of("email", "alice@example.com"));
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			// "email" missing from id_token → backfill fetches UserInfo; "sub" already present → skip merge for sub.
			.userInfoClaims("email", "sub")
			.build();
		var cookie = login(rp, "alice", "sess-1", null);
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals("alice@example.com",
			((ClaimsPrincipal) auth.get().getPrincipal()).getClaim("email", String.class).orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// extractRoles branches
	//-----------------------------------------------------------------------------------------------------------------

	/** scope claim is a Collection<String> — line 994 true branch + line 996 true branch. */
	@Test void j01_extractRoles_collectionClaim_collected() throws Exception {
		var store = InMemorySessionStore.create();
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.sessionStore(store)
			.jwkSet(publicJwks(key))
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		// scope as a JSON array — Nimbus deserialises it to List<String> in the claims set.
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", List.of("read", "write")));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		// Use the cookie produced by this completeLogin directly — don't call login() again which would
		// overwrite idp.idToken with a string-scope token.
		var cookie = cookieValue(res2.getHeader("Set-Cookie"));
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals(Set.of("read", "write"), auth.get().getRoles());
	}

	/** scope claim is a String with blank pieces — line 992 false branch (blank check). */
	@Test void j02_extractRoles_blankPiece_skipped() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		// Scope string with extra whitespace that produces blank split pieces.
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", "  read   write  "));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		var cookie = cookieValue(res2.getHeader("Set-Cookie"));
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals(Set.of("read", "write"), auth.get().getRoles());
	}

	/** scope claim is not a String or Collection (e.g. Integer) — line 994 false branch: both isinstance checks fail → empty roles. */
	@Test void j03_extractRoles_unknownClaimType_emptyRoles() throws Exception {
		var store = InMemorySessionStore.create();
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.sessionStore(store)
			.jwkSet(publicJwks(key))
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		// Integer claim: serialised as JSON number, round-tripped back as Long — neither String nor Collection.
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", 42L));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		var cookie = cookieValue(res2.getHeader("Set-Cookie"));
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertTrue(auth.get().getRoles().isEmpty());
	}

	/** scope claim is a Collection containing a non-String item — line 996 false branch: item instanceof String fails. */
	@Test void j04_extractRoles_collectionWithNonStringItem_skipped() throws Exception {
		var store = InMemorySessionStore.create();
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.sessionStore(store)
			.jwkSet(publicJwks(key))
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		// List with a number element: after JWT round-trip the number comes back as Long → not a String.
		var scopeList = new java.util.ArrayList<>();
		scopeList.add("read");
		scopeList.add(99L);
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", scopeList));
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		var cookie = cookieValue(res2.getHeader("Set-Cookie"));
		var auth = authn(rp, cookie);
		assertTrue(auth.isPresent());
		assertEquals(Set.of("read"), auth.get().getRoles());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// buildSetCookie flag permutations
	//-----------------------------------------------------------------------------------------------------------------

	/** cookieHttpOnly=false — line 1007 false branch. */
	@Test void k01_buildSetCookie_noHttpOnly() throws Exception {
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.cookieHttpOnly(false)
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertFalse(res2.getHeader("Set-Cookie").contains("HttpOnly"));
	}

	/** cookieSecure=false — line 1009 false branch. */
	@Test void k02_buildSetCookie_noSecure() throws Exception {
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.cookieSecure(false)
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertFalse(res2.getHeader("Set-Cookie").contains("Secure"));
	}

	/** cookieSameSite=Strict — verifies the SameSite attribute value in the Set-Cookie header. */
	@Test void k03_buildSetCookie_customSameSite() throws Exception {
		var rp = OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.cookieSameSite("Strict")
			.build();
		var req1 = MockServletRequest.create("GET", "/auth/login");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertTrue(res2.getHeader("Set-Cookie").contains("SameSite=Strict"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// safeRelativePath + readCookie edge cases
	//-----------------------------------------------------------------------------------------------------------------

	/** blank redirect param → open-redirect defense returns null → falls back to postLoginRedirect "/". */
	@Test void l01_safeRelativePath_blank_usesDefault() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"));
	}

	/** redirect doesn't start with "/" → null → falls back to postLoginRedirect. */
	@Test void l02_safeRelativePath_noLeadingSlash_usesDefault() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=dashboard");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"));
	}

	/** redirect starts with "//" — protocol-relative open-redirect detected → null → default. */
	@Test void l03_safeRelativePath_doubleSlash_blocked() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=//evil.example.com/x");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"));
	}

	/** redirect contains "\\" — backslash open-redirect → null → default (line 1070 contains("\\") branch). */
	@Test void l04_safeRelativePath_backslash_blocked() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=/evil\\x");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"));
	}

	/** redirect contains "://" — scheme in path open-redirect → null → default. */
	@Test void l05_safeRelativePath_schemeInPath_blocked() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var req1 = MockServletRequest.create("GET", "/auth/login?redirect=/evil://x");
		var res1 = MockServletResponse.create();
		rp.startLogin(req1, res1);
		var state = queryParam(res1.getHeader("Location"), "state");
		var nonce = queryParam(res1.getHeader("Location"), "nonce");
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", null, nonce, Instant.now(), Duration.ofMinutes(5), null);
		var req2 = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc&state=" + state);
		var res2 = MockServletResponse.create();
		rp.completeLogin(req2, res2);
		assertEquals("/", res2.getHeader("Location"));
	}

	/** Cookie value is blank — line 1043 isBlank true branch → cookie treated as absent → refresh returns empty. */
	@Test void l06_readCookie_blankValue_treatedAsAbsent() {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", "/auth/refresh")
			.cookies(new Cookie[]{ new Cookie(rp.cookieName(), "   ") });
		var res = MockServletResponse.create();
		assertTrue(rp.refresh(req, res).isEmpty());
	}

	/** Cookie with same name but null value — line 1043 null check branch → skipped, returns null. */
	@Test void l07_readCookie_nullValue_treatedAsAbsent() {
		var rp = rp(InMemorySessionStore.create());
		// Cookie.setValue(null) produces a null value; Cookie constructor with "" produces blank.
		var c = new Cookie(rp.cookieName(), "x");
		c.setValue(null);
		var req = MockServletRequest.create("GET", "/auth/refresh").cookies(new Cookie[]{ c });
		var res = MockServletResponse.create();
		assertTrue(rp.refresh(req, res).isEmpty());
	}

	/** Cookie with different name — line 1043 cookieName.equals false branch: cookie skipped by the loop. */
	@Test void l08_readCookie_wrongName_treatedAsAbsent() {
		var rp = rp(InMemorySessionStore.create());
		var req = MockServletRequest.create("GET", "/auth/refresh")
			.cookies(new Cookie[]{ new Cookie("OTHER_COOKIE", "some-value") });
		var res = MockServletResponse.create();
		assertTrue(rp.refresh(req, res).isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder guards
	//-----------------------------------------------------------------------------------------------------------------

	/** sessionTtl = zero → IllegalArgumentException — line 316 false branch. */
	@Test void m01_sessionTtl_zero_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> OidcRelyingParty.create().sessionTtl(Duration.ZERO));
	}

	/** sessionTtl = negative → IllegalArgumentException. */
	@Test void m02_sessionTtl_negative_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> OidcRelyingParty.create().sessionTtl(Duration.ofSeconds(-1)));
	}
}
