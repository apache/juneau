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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oidc.rp.LoginStateStore.PendingLogin;
import org.apache.juneau.rest.server.auth.oidc.rp.OidcTestSupport.MutableClock;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link InMemoryLoginStateStore} &mdash; the shipped single-node, single-use, TTL-bounded,
 * size-bounded default {@link LoginStateStore}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class InMemoryLoginStateStore_Test extends TestBase {

	// Fixed clock seam: these cases don't depend on wall-clock time, so a deterministic clock
	// replaces the system clock (java:S8692) without changing behavior.
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
	private static final Duration TTL = Duration.ofMinutes(5);

	private static InMemoryLoginStateStore store(Clock clock) {
		return new InMemoryLoginStateStore(TTL, 100, clock);
	}

	/** Builds a framework-shaped record stamped from the given clock (createdAt=now, expiresAt=now+TTL). */
	private static PendingLogin pending(Clock clock, String nonce, String verifier, String redirect) {
		var now = clock.instant();
		return new PendingLogin(nonce, verifier, redirect, now, now.plus(TTL));
	}

	@Test void a01_storeThenConsume_roundTrips() {
		var s = store(CLOCK);
		s.store("state-1", pending(CLOCK, "nonce-1", "verifier-1", "/dashboard"));
		var p = s.consume("state-1");
		assertTrue(p.isPresent());
		assertEquals("nonce-1", p.get().nonce());
		assertEquals("verifier-1", p.get().codeVerifier());
		assertEquals("/dashboard", p.get().redirectTarget());
	}

	@Test void a02_consume_isSingleUse() {
		var s = store(CLOCK);
		s.store("state-1", pending(CLOCK, "nonce-1", "verifier-1", null));
		assertTrue(s.consume("state-1").isPresent());
		assertTrue(s.consume("state-1").isEmpty(), "second consume of same state must miss (replay defense)");
	}

	@Test void a03_consume_unknownState_isEmpty() {
		var s = store(CLOCK);
		assertTrue(s.consume("never-stored").isEmpty());
	}

	@Test void a04_consume_nullState_isEmpty() {
		var s = store(CLOCK);
		assertTrue(s.consume(null).isEmpty());
	}

	@Test void b01_expiredEntry_isMissed() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new MutableClock(base);
		var s = new InMemoryLoginStateStore(TTL, 100, clock);
		s.store("state-1", pending(clock, "nonce-1", "verifier-1", null));
		clock.advance(Duration.ofMinutes(6));
		assertTrue(s.consume("state-1").isEmpty(), "entry past expiresAt must be treated as a miss");
	}

	@Test void b02_notYetExpired_survives() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new MutableClock(base);
		var s = new InMemoryLoginStateStore(TTL, 100, clock);
		s.store("state-1", pending(clock, "nonce-1", "verifier-1", null));
		clock.advance(Duration.ofMinutes(4));
		assertTrue(s.consume("state-1").isPresent());
	}

	/** A stored record with a {@code null} expiresAt fails closed as expired (defensive against a foreign blob). */
	@Test void b03_nullExpiresAt_isTreatedAsExpired() {
		var s = store(CLOCK);
		var now = CLOCK.instant();
		s.store("state-1", new PendingLogin("nonce-1", "verifier-1", null, now, null));
		assertTrue(s.consume("state-1").isEmpty(), "null expiresAt must fail closed as expired");
	}

	@Test void c01_sizeCap_evictsEldest() {
		var s = new InMemoryLoginStateStore(TTL, 3, CLOCK);
		s.store("s1", pending(CLOCK, "n", "v", null));
		s.store("s2", pending(CLOCK, "n", "v", null));
		s.store("s3", pending(CLOCK, "n", "v", null));
		s.store("s4", pending(CLOCK, "n", "v", null));  // evicts s1
		assertEquals(3, s.size());
		assertTrue(s.consume("s1").isEmpty());
		assertTrue(s.consume("s4").isPresent());
	}

	@Test void d01_rejectsNonPositiveTtl() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginStateStore(Duration.ZERO, 100, CLOCK));
	}

	/** Negative TTL rejected — second bytecode branch of {@code !isZero && !isNegative}. */
	@Test void d05_rejectsNegativeTtl() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginStateStore(Duration.ofSeconds(-1), 100, CLOCK));
	}

	@Test void d02_rejectsTtlAbove30Minutes() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginStateStore(Duration.ofMinutes(31), 100, CLOCK));
	}

	@Test void d03_rejectsNonPositiveMaxEntries() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryLoginStateStore(Duration.ofMinutes(5), 0, CLOCK));
	}

	@Test void d04_rejectsBlankStateOnStore() {
		var s = store(CLOCK);
		var p = pending(CLOCK, "n", "v", null);
		assertThrows(IllegalArgumentException.class, () -> s.store("", p));
	}

	@Test void d06_rejectsNullPendingOnStore() {
		var s = store(CLOCK);
		assertThrows(IllegalArgumentException.class, () -> s.store("state-1", null));
	}
}
