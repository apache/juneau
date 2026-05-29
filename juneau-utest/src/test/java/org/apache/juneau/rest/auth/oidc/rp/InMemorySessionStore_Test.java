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
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.auth.ClaimsPrincipal;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemorySessionStore} &mdash; server-side indexing + back-channel revocation.
 *
 * @since 9.5.0
 */
class InMemorySessionStore_Test extends TestBase {

	private static OidcSession session(String id, String subject, String sid) {
		return session(id, subject, sid, Instant.now());
	}

	private static OidcSession session(String id, String subject, String sid, Instant base) {
		return new OidcSession(id, subject, Optional.ofNullable(sid),
			new ClaimsPrincipal(subject, Map.of("sub", subject)),
			Set.of("user"), Optional.empty(), base, base.plus(Duration.ofHours(8)));
	}

	@Test void a01_createThenLookup_roundTrips() {
		var store = InMemorySessionStore.create();
		var cookie = store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		assertEquals("id-1", cookie);
		assertTrue(store.lookup("id-1").isPresent());
		assertEquals("alice", store.lookup("id-1").get().subject());
	}

	@Test void a02_lookupUnknown_isEmpty() {
		assertTrue(InMemorySessionStore.create().lookup("nope").isEmpty());
	}

	@Test void a03_invalidate_removesSession() {
		var store = InMemorySessionStore.create();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.invalidate("id-1");
		assertTrue(store.lookup("id-1").isEmpty());
	}

	@Test void b01_supportsServerSideRevocation() {
		assertTrue(InMemorySessionStore.create().supportsServerSideRevocation());
	}

	@Test void b02_invalidateBySessionId_targetsSingleSession() {
		var store = InMemorySessionStore.create();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "alice", "sess-2"));
		assertEquals(1, store.invalidateBySessionId("sess-1"));
		assertTrue(store.lookup("id-1").isEmpty());
		assertTrue(store.lookup("id-2").isPresent());
	}

	@Test void b03_invalidateBySubject_targetsAllOfSubject() {
		var store = InMemorySessionStore.create();
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "alice", "sess-2"));
		store.createSessionCookieValue(session("id-3", "bob", "sess-3"));
		assertEquals(2, store.invalidateBySubject("alice"));
		assertTrue(store.lookup("id-1").isEmpty());
		assertTrue(store.lookup("id-2").isEmpty());
		assertTrue(store.lookup("id-3").isPresent());
	}

	@Test void b04_invalidateByUnknownSubjectOrSid_returnsZero() {
		var store = InMemorySessionStore.create();
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
		var store = InMemorySessionStore.create(2);
		store.createSessionCookieValue(session("id-1", "alice", "sess-1"));
		store.createSessionCookieValue(session("id-2", "bob", "sess-2"));
		store.createSessionCookieValue(session("id-3", "carol", "sess-3"));  // evicts id-1
		assertEquals(2, store.size());
		assertTrue(store.lookup("id-1").isEmpty());
		assertEquals(0, store.invalidateBySubject("alice"), "evicted session must also be unindexed");
	}
}
