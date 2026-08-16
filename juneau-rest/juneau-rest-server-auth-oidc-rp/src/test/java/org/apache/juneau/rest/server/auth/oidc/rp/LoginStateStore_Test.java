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

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.auth.oidc.rp.LoginStateStore.PendingLogin;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.jwk.*;

/**
 * Security gates for the injectable {@link LoginStateStore} SPI: injected-store wiring, fail-closed store
 * outages, consume-time redirect re-validation, the framework expiry ceiling (enforced before the token
 * exchange), {@link PendingLogin} redaction, and the default impl's concurrent single-use race.
 *
 * <p>
 * These are the substance ("red-on-broken") gates for TODO-397: each is constructed so a no-op setter,
 * a mis-ordered check, or a class-only assertion would fail it.  The recording fake plus distinctive
 * hardcoded values ensure the injected instance (not the hidden default store) is actually exercised.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S8692", // Nimbus oauth2-oidc-sdk exposes no clock hook on the ID-token path; the login-state expiry re-check IS clock-injectable and is tested deterministically.
	"java:S1130", // Test methods declare throws Exception for checked exceptions MockServletResponse may propagate; declarations are intentionally broad.
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
	"java:S5976", // Similar-shaped gates assert distinct security outcomes; parameterizing would obscure intent.
	"resource"    // Closeable StubIdp fixture; lifecycle managed by @BeforeEach/@AfterEach, not a real leak.
})
class LoginStateStore_Test extends TestBase {

	private static final String CID = "web-app";
	private static final URI REDIRECT_URI = URI.create("https://app.example.com/auth/callback");
	private static final URI AUTHZ = URI.create("https://stub-idp.example.com/authorize");
	private static final URI END_SESSION = URI.create("https://stub-idp.example.com/logout");

	// A fixed clock shared by the RP AND the fixture timestamps (mandatory — mixing clocks makes a correct
	// expiry check look like a failure).  The ID-token exp uses wall-clock time internally in Nimbus, so ID
	// tokens are still signed with Instant.now().
	private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	// Distinctive hardcoded values the hidden default store could NOT have produced (S8 / gate 1).
	private static final String FAKE_NONCE = "fake-nonce-distinctive-8f3a";
	private static final String FAKE_VERIFIER = "fakeVERIFIERfakeVERIFIERfakeVERIFIERfakeVER1"; // 44 chars, PKCE-valid [A-Za-z0-9]
	private static final String FAKE_REDIRECT = "/from-fake-store";

	// A distinctive secret marker embedded in a backend exception message (gate 4 / gate 5), so "the error
	// surface does not echo the secret" is an observable assertion, not vacuously true.
	private static final String LEAK_SENTINEL = "code_verifier=SUPERSECRET-LEAK-SENTINEL-9x7q";

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

	private OidcRelyingParty rp(LoginStateStore loginStateStore) {
		return OidcRelyingParty.create()
			.metadata(idp.metadata(AUTHZ, END_SESSION))
			.clientId(CID)
			.clientSecret("client-secret")
			.redirectUri(REDIRECT_URI)
			.scope("openid", "profile")
			.sessionStore(InMemorySessionStore.create())
			.jwkSet(publicJwks(key))
			.loginStateStore(loginStateStore)
			.clock(CLOCK)
			.build();
	}

	private String startLoginGetState(OidcRelyingParty rp) throws Exception {
		var req = MockServletRequest.create("GET", "/auth/login");
		var res = MockServletResponse.create();
		rp.startLogin(req, res);
		var loc = res.getHeader("Location");
		assertNotNull(loc, "startLogin must redirect to the IdP");
		return queryParam(loc, "state");
	}

	private MockServletResponse completeLogin(OidcRelyingParty rp, String state) throws Exception {
		var req = MockServletRequest.create("GET", REDIRECT_URI + "?code=abc123&state=" + state);
		var res = MockServletResponse.create();
		rp.completeLogin(req, res);
		return res;
	}

	private void signIdTokenWith(String nonce) throws Exception {
		idp.idToken = signIdToken(key, idp.issuer, CID, "alice", "sess-1", nonce, Instant.now(), Duration.ofMinutes(5),
			Map.of("scope", "openid profile"));
	}

	private static PendingLogin pending(String redirect, Instant createdAt, Instant expiresAt) {
		return new PendingLogin(FAKE_NONCE, FAKE_VERIFIER, redirect, createdAt, expiresAt);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A recording fake LoginStateStore configurable per gate.
	//-----------------------------------------------------------------------------------------------------------------

	static final class FakeStore implements LoginStateStore {
		final IntegerHolder storeCount = IntegerHolder.create();
		final IntegerHolder consumeCount = IntegerHolder.create();
		volatile String lastStoreState;
		volatile String lastConsumeState;
		volatile Optional<PendingLogin> lastConsumeReturned = Optional.empty();

		RuntimeException storeThrows;
		RuntimeException consumeThrows;
		Optional<PendingLogin> consumeResult = Optional.empty();

		@Override public void store(String state, PendingLogin p) {
			storeCount.increment();
			lastStoreState = state;
			if (storeThrows != null)
				throw storeThrows;
		}

		@Override public Optional<PendingLogin> consume(String state) {
			consumeCount.increment();
			lastConsumeState = state;
			if (consumeThrows != null)
				throw consumeThrows;
			lastConsumeReturned = consumeResult;
			return consumeResult;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 1 — injected store honored on both legs, with observable distinctive outputs + mandatory call-counts.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_injectedStore_honoredOnBothLegs() throws Exception {
		var fake = new FakeStore();
		fake.consumeResult = Optional.of(pending(FAKE_REDIRECT, NOW, NOW.plus(Duration.ofMinutes(5))));
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		assertTrue(fake.storeCount.is(1), "store must be invoked once on the injected instance");
		assertEquals(state, fake.lastStoreState, "store must receive the generated state");

		signIdTokenWith(FAKE_NONCE);  // sign with the fake's distinctive nonce; a mismatch fails validation
		var res = completeLogin(rp, state);

		assertTrue(fake.consumeCount.is(1), "consume must be invoked once on the injected instance");
		assertEquals(state, fake.lastConsumeState, "consume must receive the callback state");
		assertNotNull(cookieValue(res.getHeader("Set-Cookie")), "login must succeed (session cookie set)");
		assertEquals(FAKE_REDIRECT, res.getHeader("Location"), "Location must be the fake's distinctive redirectTarget");
		// The verifier exchanged with the IdP is the fake's distinctive one (record-only capture on the stub).
		assertEquals(FAKE_VERIFIER, idp.lastCodeVerifier, "the fake's distinctive code_verifier must be exchanged");
		// Login succeeding with an ID token signed by FAKE_NONCE proves the fake's nonce was the one validated.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 3 — consume miss on the INJECTED store is the CSRF/replay decision (fails against a no-op setter).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g03_consumeMiss_onInjectedStore_rejected() throws Exception {
		var fake = new FakeStore();
		fake.consumeResult = Optional.empty();  // injected consume misses
		var rp = rp(fake);

		// startLogin FIRST: a hidden default store would hold a live entry and login would succeed if the fake
		// were ignored.  So a no-op setter is caught two ways below (consume count stays 0; no miss).
		var state = startLoginGetState(rp);
		assertTrue(fake.storeCount.is(1));
		assertEquals(state, fake.lastStoreState);

		signIdTokenWith(FAKE_NONCE);
		var ex = assertThrows(AuthenticationException.class, () -> completeLogin(rp, state));

		assertTrue(fake.consumeCount.is(1), "the injected consume must be the CSRF/replay decision point");
		assertEquals(state, fake.lastConsumeState);
		assertTrue(ex.getMessage().contains("missing, expired, or already used"),
			"must reject with the consume-miss message, got: " + ex.getMessage());
		assertEquals(0, idp.tokenHits.get(), "a consume miss must not reach the token exchange");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 4 — store outage on startLogin → fail closed (no redirect) + no secret leak in the error surface.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g04_storeOutage_onStartLogin_failsClosed_noLeak() {
		var fake = new FakeStore();
		fake.storeThrows = new RuntimeException("backend echoed " + LEAK_SENTINEL);
		var rp = rp(fake);

		var req = MockServletRequest.create("GET", "/auth/login");
		var res = MockServletResponse.create();
		var ex = assertThrows(AuthenticationException.class, () -> rp.startLogin(req, res));

		assertNull(res.getHeader("Location"), "no authorization redirect may be issued on a store outage (fail closed)");
		assertFalse(String.valueOf(ex.getMessage()).contains(LEAK_SENTINEL),
			"the propagated error must not echo the backend secret");
		assertNull(ex.getCause(), "the raw backend exception (carrying the secret) must not be chained as the cause");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 5 — consume outage → rejected with the DISTINCT "unavailable" message (vs the CSRF-miss message).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g05_consumeOutage_rejectedAsUnavailable() throws Exception {
		var fake = new FakeStore();
		fake.consumeThrows = new RuntimeException("backend down " + LEAK_SENTINEL);
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		signIdTokenWith(FAKE_NONCE);
		var ex = assertThrows(AuthenticationException.class, () -> completeLogin(rp, state));

		assertTrue(ex.getMessage().contains("unavailable"),
			"consume outage must be rejected as unavailable, got: " + ex.getMessage());
		assertFalse(ex.getMessage().contains("missing, expired, or already used"),
			"the infra-outage signal must be distinct from the CSRF/replay miss");
		assertFalse(String.valueOf(ex.getMessage()).contains(LEAK_SENTINEL), "must not echo the backend secret");
		assertEquals(0, idp.tokenHits.get(), "a consume outage must not reach the token exchange");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 6 — consume-time redirect re-validation: an evil redirectTarget from the store is discarded → "/".
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g06_evilRedirectTarget_fromStore_isDiscarded() throws Exception {
		var fake = new FakeStore();
		fake.consumeResult = Optional.of(pending("https://evil.example.com", NOW, NOW.plus(Duration.ofMinutes(5))));
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		signIdTokenWith(FAKE_NONCE);
		var res = completeLogin(rp, state);

		assertEquals("/", res.getHeader("Location"),
			"a writable-store absolute redirect must be re-validated away in favor of the post-login default");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 7 — framework expiry ceiling beats an over-age store record, BEFORE the token exchange.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g07_overTtlRecord_rejected_beforeExchange() throws Exception {
		// Past its minted expiresAt (now-1min) but well within MAX_TTL of createdAt (only 6 min old): a
		// MAX_TTL-only implementation would accept it, so this pins the minted-expiresAt re-check.
		var overTtl = pending(FAKE_REDIRECT, NOW.minus(Duration.ofMinutes(6)), NOW.minus(Duration.ofMinutes(1)));
		var fake = new FakeStore();
		fake.consumeResult = Optional.of(overTtl);
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		signIdTokenWith(FAKE_NONCE);  // would-succeed setup: the ONLY thing between success and rejection is the expiry re-check
		var ex = assertThrows(AuthenticationException.class, () -> completeLogin(rp, state));

		// (2) message-specific, not class-only — distinct from the post-exchange throws.
		assertTrue(ex.getMessage().contains("missing, expired, or already used"),
			"over-age record must be rejected with the expiry/miss message, got: " + ex.getMessage());
		// (3) token endpoint never hit — proves the re-check runs BEFORE codeFlow().exchange.
		assertEquals(0, idp.tokenHits.get(), "the expiry re-check must run before the token exchange (code not burned)");
		// consume-miss disambiguation — the present over-TTL record WAS consumed once, then rejected for age.
		assertTrue(fake.consumeCount.is(1), "the injected consume must have been invoked once");
		assertEquals(state, fake.lastConsumeState);
		assertTrue(fake.lastConsumeReturned.isPresent(), "the record was present (not a consume miss)");
		assertEquals(NOW.minus(Duration.ofMinutes(1)), fake.lastConsumeReturned.get().expiresAt(),
			"the rejected record is the over-TTL one the fake returned");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 7b — null timestamps fail closed (rejected as expired, NOT an NPE→500).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g07b_nullExpiresAt_failsClosed() throws Exception {
		var fake = new FakeStore();
		fake.consumeResult = Optional.of(pending(FAKE_REDIRECT, NOW, null));
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		signIdTokenWith(FAKE_NONCE);
		var ex = assertThrows(AuthenticationException.class, () -> completeLogin(rp, state));

		assertTrue(ex.getMessage().contains("missing, expired, or already used"),
			"null expiresAt must be treated as expired, got: " + ex.getMessage());
		assertEquals(0, idp.tokenHits.get(), "must reject before the token exchange");
	}

	@Test void g07c_nullCreatedAt_failsClosed() throws Exception {
		var fake = new FakeStore();
		fake.consumeResult = Optional.of(pending(FAKE_REDIRECT, null, NOW.plus(Duration.ofMinutes(5))));
		var rp = rp(fake);

		var state = startLoginGetState(rp);
		signIdTokenWith(FAKE_NONCE);
		var ex = assertThrows(AuthenticationException.class, () -> completeLogin(rp, state));

		assertTrue(ex.getMessage().contains("missing, expired, or already used"),
			"null createdAt must be treated as expired, got: " + ex.getMessage());
		assertEquals(0, idp.tokenHits.get(), "must reject before the token exchange");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 8 — PendingLogin.toString() redacts both secrets.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g08_pendingLogin_toString_redactsSecrets() {
		var p = new PendingLogin("nonce123", "verifierXYZ", "/home", NOW, NOW.plus(Duration.ofMinutes(5)));
		var s = p.toString();
		assertFalse(s.contains("nonce123"), "nonce must be redacted");
		assertFalse(s.contains("verifierXYZ"), "code_verifier must be redacted");
		assertTrue(s.contains("<redacted>"), "toString must mark redacted secrets");
		assertTrue(s.contains("/home"), "non-secret redirectTarget may be shown for diagnostics");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Gate 9 — default impl atomic single-use survives a concurrent-consume race (in-process only).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g09_defaultStore_concurrentConsume_isSingleUse() throws Exception {
		var store = new InMemoryLoginStateStore(Duration.ofMinutes(5), 1000, CLOCK);
		var pool = Executors.newFixedThreadPool(2);
		try {
			for (var i = 0; i < 300; i++) {
				var stateKey = "state-" + i;
				store.store(stateKey, pending(null, NOW, NOW.plus(Duration.ofMinutes(5))));
				var present = new AtomicInteger();
				var start = new CountDownLatch(1);
				Callable<Void> task = () -> {
					start.await();
					if (store.consume(stateKey).isPresent())
						present.incrementAndGet();
					return null;
				};
				var f1 = pool.submit(task);
				var f2 = pool.submit(task);
				start.countDown();
				f1.get();
				f2.get();
				assertEquals(1, present.get(), "exactly one concurrent consume may win the single-use race");
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
