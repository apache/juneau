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
 * Tests for {@link InMemorySessionStore} &mdash; server-side indexing + back-channel revocation.
 *
 * @since 10.0.0
 */
class InMemorySessionStore_Test extends TestBase {

	// Fixed time seam: the store checks expiry against its Clock, so the session base and the
	// store clock are both pinned to a constant instead of reading the system clock (java:S8692).
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private static InMemorySessionStore store() {
		return new InMemorySessionStore(100, CLOCK);
	}

	private static OidcSession session(String id, String subject, String sid) {
		return session(id, subject, sid, NOW);
	}

	private static OidcSession session(String id, String subject, String sid, Instant base) {
		return new OidcSession(id, subject, Optional.ofNullable(sid),
			new ClaimsPrincipal(subject, Map.of("sub", subject)),
			Set.of("user"), Optional.empty(), base, base.plus(Duration.ofHours(8)));
	}

	@Test void a01_createThenLookup_roundTrips() {
		var store = store();
		var cookie = store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		assertEquals("id-1", cookie);
		assertTrue(store.lookup("id-1").isPresent());
		assertEquals("alice", store.lookup("id-1").get().subject());
	}

	@Test void a02_lookupUnknown_isEmpty() {
		assertTrue(InMemorySessionStore.create().lookup("nope").isEmpty());
	}

	@Test void a03_invalidate_removesSession() {
		var store = store();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.invalidate("id-1");
		assertTrue(store.lookup("id-1").isEmpty());
	}

	@Test void b01_supportsServerSideRevocation() {
		assertTrue(InMemorySessionStore.create().supportsServerSideRevocation());
	}

	@Test void b02_invalidateBySessionId_targetsSingleSession() {
		var store = store();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "alice", "sess-2"));
		assertEquals(1, store.invalidateBySessionId("sess-1"));
		assertTrue(store.lookup("id-1").isEmpty());
		assertTrue(store.lookup("id-2").isPresent());
	}

	@Test void b03_invalidateBySubject_targetsAllOfSubject() {
		var store = store();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "alice", "sess-2"));
		store.createSessionCookieValue(session("id-3", "bob", "sess-3"));
		assertEquals(2, store.invalidateBySubject("alice"));
		assertTrue(store.lookup("id-1").isEmpty());
		assertTrue(store.lookup("id-2").isEmpty());
		assertTrue(store.lookup("id-3").isPresent());
	}

	@Test void b04_invalidateByUnknownSubjectOrSid_returnsZero() {
		var store = store();
		assertEquals(0, store.invalidateBySubject("ghost"));
		assertEquals(0, store.invalidateBySessionId("ghost"));
	}

	@Test void c01_expiredSession_missedAndRemoved() {
		var base = Instant.parse("2026-01-01T00:00:00Z");
		var clock = new EphemeralStore_Test.MutableClock(base);
		var store = new InMemorySessionStore(100, clock);
		store.createSessionCookieValue(session("id-1", "alice", "sess-1", base));
		clock.advance(Duration.ofHours(9));
		assertTrue(store.lookup("id-1").isEmpty());
		assertEquals(0, store.size());
	}

	@Test void d01_sizeCap_evictsEldestAndUnindexes() {
		var store = new InMemorySessionStore(2, CLOCK);
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "bob", "sess-2"));
		store.createSessionCookieValue(session("id-3", "carol", "sess-3"));  // evicts id-1
		assertEquals(2, store.size());
		assertTrue(store.lookup("id-1").isEmpty());
		assertEquals(0, store.invalidateBySubject("alice"), "evicted session must also be unindexed");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: constructor guard — maxEntries <= 0 is rejected (line 81 false branch).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_constructor_zeroMaxEntries_throws() {
		assertThrows(IllegalArgumentException.class, () -> new InMemorySessionStore(0, CLOCK));
	}

	@Test void e02_constructor_negativeMaxEntries_throws() {
		assertThrows(IllegalArgumentException.class, () -> new InMemorySessionStore(-1, CLOCK));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: removeById on nonexistent id returns false (line 177 true branch).
	// invalidate() is a no-op when the id was never registered or already removed.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_invalidate_unknownId_doesNotThrow() {
		var store = store();
		// id never registered — removeById returns false; no exception expected.
		store.invalidate("nonexistent");
		assertEquals(0, store.size());
	}

	@Test void f02_invalidate_idRemovedTwice_doesNotThrow() {
		var store = store();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.invalidate("id-1");
		// Second invalidate — id no longer in byId; removeById returns false.
		store.invalidate("id-1");
		assertEquals(0, store.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: two sessions sharing the same sid — invalidating one leaves sid index entry with one remaining id
	//    (line 194 false branch: sids.isEmpty() == false).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_twoSessionsSameSid_invalidateOne_sidIndexRetained() {
		var store = store();
		store.createSessionCookieValue(session("id-1", "alice", "shared-sid"));
		store.createSessionCookieValue(session("id-2", "bob", "shared-sid"));
		store.invalidate("id-1");
		// sid index must still contain id-2 — invalidateBySessionId should remove it.
		assertEquals(1, store.invalidateBySessionId("shared-sid"));
		assertEquals(0, store.size());
	}
}
