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
package org.apache.juneau.rest.auth.oidc.rp;

import static org.apache.juneau.rest.auth.oidc.rp.OidcTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.jwk.*;

import jakarta.servlet.http.*;

/**
 * End-to-end tests for {@link OidcRelyingParty} driving login &rarr; callback &rarr; authorized-request
 * &rarr; logout against an in-JVM {@link StubIdp}, plus refresh-token rotation and back-channel logout.
 *
 * @since 10.0.0
 */
class OidcRelyingParty_Test extends TestBase {

	private static final String CID = "web-app";
	private static final URI REDIRECT_URI = URI.create("https://app.example.com/auth/callback");
	private static final URI AUTHZ = URI.create("https://stub-idp.example.com/authorize");
	private static final URI END_SESSION = URI.create("https://stub-idp.example.com/logout");

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
		return OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(store)
			.jwkSet(publicJwks(key))
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
		var rp = rp(InMemorySessionStore.create());
		var cookie = login(rp, "alice", "sess-1", null);
		assertTrue(authn(rp, cookie).isPresent());
		var logoutToken = signLogoutToken(key, idp.issuer, CID, null, "sess-1");
		assertEquals(1, rp.backChannelLogout(logoutToken));
		assertTrue(authn(rp, cookie).isEmpty());
	}

	@Test void e02_backChannelLogout_bySub_invalidatesAllSessions() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		var cookie1 = login(rp, "alice", "sess-1", null);
		var cookie2 = login(rp, "alice", "sess-2", null);
		var logoutToken = signLogoutToken(key, idp.issuer, CID, "alice", null);
		assertEquals(2, rp.backChannelLogout(logoutToken));
		assertTrue(authn(rp, cookie1).isEmpty());
		assertTrue(authn(rp, cookie2).isEmpty());
	}

	@Test void e03_backChannelLogout_badSignature_isRejected() throws Exception {
		var rp = rp(InMemorySessionStore.create());
		login(rp, "alice", "sess-1", null);
		var attacker = generateRsa("k1");
		var forged = signLogoutToken(attacker, idp.issuer, CID, null, "sess-1");
		assertThrows(AuthenticationException.class, () -> rp.backChannelLogout(forged));
	}

	@Test void e04_backChannelLogout_signedCookieStore_throws() throws Exception {
		var rp = rp(SignedCookieSessionStore.create().signingKey("0123456789abcdef0123456789abcdef").build());
		var logoutToken = signLogoutToken(key, idp.issuer, CID, null, "sess-1");
		assertThrows(IllegalStateException.class, () -> rp.backChannelLogout(logoutToken));
	}
}
