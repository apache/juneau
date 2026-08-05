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

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link EphemeralStore} &mdash; single-use, TTL-bounded, size-bounded {@code state} &rarr;
 * {@code (codeVerifier, expectedIssuer)} storage (CSRF/replay guard).  Ported and adapted from the
 * {@code juneau-rest-server-auth-oidc-rp} original to the headless {@link EphemeralStore.PendingAuthorization} shape.
 *
 * @since 10.0.0
 */
class EphemeralStore_Test extends TestBase {

	private static final URI ISS = URI.create("https://as.example.com");

	// Fixed clock seam: these cases don't depend on wall-clock time, so a deterministic clock replaces the system clock.
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	private static EphemeralStore store(Clock clock) {
		return new EphemeralStore(Duration.ofMinutes(5), 100, clock);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: store / consume single-use
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_storeThenConsume_roundTrips() {
		var s = store(CLOCK);
		s.store("state-1", "verifier-1", ISS);
		var p = s.consume("state-1");
		assertTrue(p.isPresent());
		assertEquals("verifier-1", p.get().codeVerifier());
		assertEquals(ISS, p.get().expectedIssuer());
		assertEquals(Instant.parse("2026-01-01T00:00:00Z"), p.get().createdAt());
	}

	@Test void a02_consume_isSingleUse() {
		var s = store(CLOCK);
		s.store("state-1", "verifier-1", ISS);
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

	@Test void a05_nullExpectedIssuer_allowed() {
		var s = store(CLOCK);
		s.store("state-1", "verifier-1", null);
		var p = s.consume("state-1");
		assertTrue(p.isPresent());
		assertNull(p.get().expectedIssuer());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: TTL expiry
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_expiredEntry_isMissed() {
		var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
		var s = new EphemeralStore(Duration.ofMinutes(5), 100, clock);
		s.store("state-1", "verifier-1", ISS);
		clock.advance(Duration.ofMinutes(6));
		assertTrue(s.consume("state-1").isEmpty(), "entry past TTL must be treated as a miss");
	}

	@Test void b02_notYetExpired_survives() {
		var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
		var s = new EphemeralStore(Duration.ofMinutes(5), 100, clock);
		s.store("state-1", "verifier-1", ISS);
		clock.advance(Duration.ofMinutes(4));
		assertTrue(s.consume("state-1").isPresent());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: LRU size cap
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_sizeCap_evictsEldest() {
		var s = new EphemeralStore(Duration.ofMinutes(5), 3, CLOCK);
		s.store("s1", "v", ISS);
		s.store("s2", "v", ISS);
		s.store("s3", "v", ISS);
		s.store("s4", "v", ISS);  // evicts s1
		assertEquals(3, s.size());
		assertTrue(s.consume("s1").isEmpty());
		assertTrue(s.consume("s4").isPresent());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: constructor / argument validation
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_rejectsNonPositiveTtl() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ZERO, 100, CLOCK));
	}

	@Test void d02_rejectsNegativeTtl() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ofSeconds(-1), 100, CLOCK));
	}

	@Test void d03_rejectsTtlAboveMax() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(EphemeralStore.MAX_TTL.plusSeconds(1), 100, CLOCK));
	}

	@Test void d04_ttlAtMaxAllowed() {
		var s = new EphemeralStore(EphemeralStore.MAX_TTL, 100, CLOCK);
		s.store("state-1", "verifier-1", ISS);
		assertTrue(s.consume("state-1").isPresent());
	}

	@Test void d05_rejectsNonPositiveMaxEntries() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ofMinutes(5), 0, CLOCK));
	}

	@Test void d06_rejectsBlankStateOnStore() {
		var s = store(CLOCK);
		assertThrows(IllegalArgumentException.class, () -> s.store("", "v", ISS));
	}

	@Test void d07_rejectsBlankVerifierOnStore() {
		var s = store(CLOCK);
		assertThrows(IllegalArgumentException.class, () -> s.store("state-1", "", ISS));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: redaction (M3)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_toStringRedactsCodeVerifier() {
		var p = new EphemeralStore.PendingAuthorization("super-secret-verifier", ISS, Instant.parse("2026-01-01T00:00:00Z"));
		assertFalse(p.toString().contains("super-secret-verifier"), p::toString);
		assertTrue(p.toString().contains("<redacted>"), p::toString);
		assertTrue(p.toString().contains("as.example.com"), p::toString);
	}

	/** A test clock the test can advance manually. */
	static final class MutableClock extends Clock {
		private Instant now;
		MutableClock(Instant start) { now = start; }
		void advance(Duration d) { now = now.plus(d); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}
}
