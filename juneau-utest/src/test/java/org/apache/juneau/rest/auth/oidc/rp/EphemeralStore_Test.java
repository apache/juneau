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

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EphemeralStore} &mdash; single-use, TTL-bounded, size-bounded state/nonce storage.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class EphemeralStore_Test extends TestBase {

	private static EphemeralStore store(Clock clock) {
		return new EphemeralStore(Duration.ofMinutes(5), 100, clock);
	}

	@Test void a01_storeThenConsume_roundTrips() {
		var s = store(Clock.systemUTC());
		s.store("state-1", "nonce-1", "verifier-1", "/dashboard");
		var p = s.consume("state-1");
		assertTrue(p.isPresent());
		assertEquals("nonce-1", p.get().nonce());
		assertEquals("verifier-1", p.get().codeVerifier());
		assertEquals("/dashboard", p.get().redirectTarget());
	}

	@Test void a02_consume_isSingleUse() {
		var s = store(Clock.systemUTC());
		s.store("state-1", "nonce-1", "verifier-1", null);
		assertTrue(s.consume("state-1").isPresent());
		assertTrue(s.consume("state-1").isEmpty(), "second consume of same state must miss (replay defense)");
	}

	@Test void a03_consume_unknownState_isEmpty() {
		var s = store(Clock.systemUTC());
		assertTrue(s.consume("never-stored").isEmpty());
	}

	@Test void a04_consume_nullState_isEmpty() {
		var s = store(Clock.systemUTC());
		assertTrue(s.consume(null).isEmpty());
	}

	@Test void b01_expiredEntry_isMissed() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new MutableClock(base);
		var s = new EphemeralStore(Duration.ofMinutes(5), 100, clock);
		s.store("state-1", "nonce-1", "verifier-1", null);
		clock.advance(Duration.ofMinutes(6));
		assertTrue(s.consume("state-1").isEmpty(), "entry past TTL must be treated as a miss");
	}

	@Test void b02_notYetExpired_survives() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new MutableClock(base);
		var s = new EphemeralStore(Duration.ofMinutes(5), 100, clock);
		s.store("state-1", "nonce-1", "verifier-1", null);
		clock.advance(Duration.ofMinutes(4));
		assertTrue(s.consume("state-1").isPresent());
	}

	@Test void c01_sizeCap_evictsEldest() {
		var s = new EphemeralStore(Duration.ofMinutes(5), 3, Clock.systemUTC());
		s.store("s1", "n", "v", null);
		s.store("s2", "n", "v", null);
		s.store("s3", "n", "v", null);
		s.store("s4", "n", "v", null);  // evicts s1
		assertEquals(3, s.size());
		assertTrue(s.consume("s1").isEmpty());
		assertTrue(s.consume("s4").isPresent());
	}

	@Test void d01_rejectsNonPositiveTtl() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ZERO, 100, Clock.systemUTC()));
	}

	@Test void d02_rejectsTtlAbove30Minutes() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ofMinutes(31), 100, Clock.systemUTC()));
	}

	@Test void d03_rejectsNonPositiveMaxEntries() {
		assertThrows(IllegalArgumentException.class, () -> new EphemeralStore(Duration.ofMinutes(5), 0, Clock.systemUTC()));
	}

	@Test void d04_rejectsBlankStateOnStore() {
		var s = store(Clock.systemUTC());
		assertThrows(IllegalArgumentException.class, () -> s.store("", "n", "v", null));
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
