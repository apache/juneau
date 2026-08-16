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

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oidc.rp.LoginStateStore.PendingLogin;
import org.junit.jupiter.api.*;

/**
 * Builder-guard tests for {@link OidcRelyingParty} — covers the missing-required-field branches
 * and the validator branches on setters that accept bad values.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class OidcRelyingPartyBuilder_Test extends TestBase {

	// Minimal valid base builder — all required fields supplied.
	private static OidcRelyingParty.Builder base() {
		return OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: build() missing required fields (line 408 guard branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_build_requiresIssuerOrMetadata() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a02_build_requiresClientId() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a03_build_requiresRedirectUri() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a04_build_requiresSessionStore() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: sessionTtl — zero and negative are rejected (line 316 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_sessionTtl_zeroRejected() {
		assertThrows(Exception.class, () -> base().sessionTtl(Duration.ZERO));
	}

	@Test void b02_sessionTtl_negativeRejected() {
		assertThrows(Exception.class, () -> base().sessionTtl(Duration.ofHours(-1)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: clockSkewSeconds — negative is rejected (line 408 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_clockSkewSeconds_negativeRejected() {
		assertThrows(Exception.class, () -> base().clockSkewSeconds(-1));
	}

	@Test void c02_clockSkewSeconds_zeroAccepted() {
		assertDoesNotThrow(() -> base().clockSkewSeconds(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: userInfoClaims — blank and null values are rejected (line 448 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_userInfoClaims_blankRejected() {
		assertThrows(Exception.class, () -> base().userInfoClaims("openid", ""));
	}

	@Test void d02_userInfoClaims_validAccumulates() {
		assertDoesNotThrow(() -> base().userInfoClaims("email", "name"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: issuer transport guard — https/loopback required, plaintext http to a remote host rejected.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_issuer_plaintextHttpNonLoopback_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			OidcRelyingParty.create().issuer(URI.create("http://idp.example.com")));
	}

	@Test void e02_issuer_loopbackHttp_allowed() {
		var rp = OidcRelyingParty.create()
			.issuer(URI.create("http://127.0.0.1:8080"))
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create())
			.build();
		assertNotNull(rp);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: stateNonceTtl (0, MAX_TTL] cap enforced on a site that runs for CUSTOM stores + loginStateStore hook.
	// -----------------------------------------------------------------------------------------------------------------

	// A TTL-oblivious no-op store, constructed independently of any TTL value (S1): it does NOT implement the
	// (0, MAX_TTL] cap, so a rejection with this store injected proves the cap comes from the builder side, not
	// from the default InMemoryLoginStateStore constructor (which is skipped when a custom store is supplied).
	private static final LoginStateStore NOOP_STORE = new LoginStateStore() {
		@Override public void store(String state, PendingLogin pending) { /* no-op */ }
		@Override public Optional<PendingLogin> consume(String state) { return Optional.empty(); }
	};

	/**
	 * Over-30-min TTL rejected even when a TTL-oblivious custom store is injected — proves the cap runs on the
	 * custom-store path (Phase 2 gate).  On main the cap lives only in the default impl ctor, which a custom
	 * store skips, so this over-TTL config would build successfully → gate red.
	 */
	@Test void f01_stateNonceTtl_over30Min_rejected_customStorePath() {
		assertThrows(IllegalArgumentException.class,
			() -> base().loginStateStore(NOOP_STORE).stateNonceTtl(Duration.ofMinutes(31)).build());
	}

	/** Zero TTL rejected on the custom-store path (fail-closed = immediately-expired), sibling of the 31-min case. */
	@Test void f02_stateNonceTtl_zero_rejected_customStorePath() {
		assertThrows(IllegalArgumentException.class,
			() -> base().loginStateStore(NOOP_STORE).stateNonceTtl(Duration.ZERO).build());
	}

	/** Negative TTL rejected — second bytecode branch of {@code !isZero && !isNegative}. */
	@Test void f03_stateNonceTtl_negative_rejected() {
		assertThrows(IllegalArgumentException.class, () -> base().stateNonceTtl(Duration.ofSeconds(-1)));
	}

	/** A valid TTL with a custom store is accepted. */
	@Test void f04_stateNonceTtl_valid_customStore_accepted() {
		assertDoesNotThrow(() -> base().loginStateStore(NOOP_STORE).stateNonceTtl(Duration.ofMinutes(10)).build());
	}

	/** The optional loginStateStore hook rejects null. */
	@Test void f05_loginStateStore_null_rejected() {
		assertThrows(IllegalArgumentException.class, () -> base().loginStateStore(null));
	}

	/**
	 * Over-30-min TTL rejected on the DEFAULT-store path too (S5 throw-site test).  Same
	 * {@link IllegalArgumentException} type as on main; only the throw <i>site</i> moves (impl ctor at
	 * {@code build()} &rarr; the {@code stateNonceTtl(...)} setter), pinning the Decision-3 delta.
	 */
	@Test void f06_stateNonceTtl_over30Min_rejected_defaultStorePath() {
		assertThrows(IllegalArgumentException.class, () -> base().stateNonceTtl(Duration.ofMinutes(31)).build());
	}
}
