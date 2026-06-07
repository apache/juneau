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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link SignedCookieSessionStore} &mdash; stateless HMAC-signed cookie round-trip, tamper
 * rejection, expiry, and the size cap.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class SignedCookieSessionStore_Test extends TestBase {

	private static final String KEY = "0123456789abcdef0123456789abcdef";  // 32 bytes

	private static OidcSession session(Instant now, String sid) {
		return new OidcSession("id-1", "alice", Optional.ofNullable(sid),
			new ClaimsPrincipal("alice", Map.of("sub", "alice", "email", "alice@example.com")),
			new LinkedHashSet<>(List.of("user", "admin")),
			Optional.empty(), now, now.plus(Duration.ofHours(8)));
	}

	@Test void a01_roundTrip_recoversClaimsAndRoles() {
		var store = SignedCookieSessionStore.create().signingKey(KEY).build();
		var now = Instant.now();
		var cookie = store.createSessionCookieValue(session(now, "sess-1"));
		var s = store.lookup(cookie);
		assertTrue(s.isPresent());
		assertEquals("alice", s.get().subject());
		assertEquals("alice", s.get().principal().getName());
		assertEquals(Set.of("user", "admin"), s.get().roles());
		assertEquals("sess-1", s.get().sid().orElse(null));
		assertEquals("alice@example.com", s.get().principal().getClaim("email", String.class).orElse(null));
		assertTrue(s.get().token().isEmpty(), "cookie store must NOT retain raw tokens");
	}

	@Test void b01_tamperedCookie_isRejected() {
		var store = SignedCookieSessionStore.create().signingKey(KEY).build();
		var cookie = store.createSessionCookieValue(session(Instant.now(), null));
		var tampered = cookie.substring(0, cookie.length() - 3) + "AAA";
		assertTrue(store.lookup(tampered).isEmpty());
	}

	@Test void b02_wrongKey_isRejected() {
		var signer = SignedCookieSessionStore.create().signingKey(KEY).build();
		var cookie = signer.createSessionCookieValue(session(Instant.now(), null));
		var other = SignedCookieSessionStore.create().signingKey("ffffffffffffffffffffffffffffffff").build();
		assertTrue(other.lookup(cookie).isEmpty());
	}

	@Test void b03_garbageCookie_isRejected() {
		var store = SignedCookieSessionStore.create().signingKey(KEY).build();
		assertTrue(store.lookup("not-a-jwt").isEmpty());
	}

	@Test void c01_expiredCookie_isRejected() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new EphemeralStore_Test.MutableClock(base);
		var store = SignedCookieSessionStore.create().signingKey(KEY).clock(clock).build();
		var cookie = store.createSessionCookieValue(session(base, null));
		clock.advance(Duration.ofHours(9));
		assertTrue(store.lookup(cookie).isEmpty());
	}

	@Test void d01_doesNotSupportServerSideRevocation() {
		var store = SignedCookieSessionStore.create().signingKey(KEY).build();
		assertFalse(store.supportsServerSideRevocation());
		assertThrows(UnsupportedOperationException.class, () -> store.invalidateBySubject("alice"));
		assertThrows(UnsupportedOperationException.class, () -> store.invalidateBySessionId("sess-1"));
	}

	@Test void e01_overCapPayload_throws() {
		var store = SignedCookieSessionStore.create().signingKey(KEY).maxCookieBytes(128).build();
		assertThrows(IllegalStateException.class,
			() -> store.createSessionCookieValue(session(Instant.now(), "sess-1")));
	}

	@Test void f01_shortKey_rejected() {
		assertThrows(IllegalArgumentException.class, () -> SignedCookieSessionStore.create().signingKey("too-short"));
	}

	@Test void f02_missingKey_rejected() {
		assertThrows(IllegalStateException.class, () -> SignedCookieSessionStore.create().build());
	}
}
