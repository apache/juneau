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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link InMemorySavedViewStore} &mdash; the non-durable in-memory default {@link SavedViewStore}
 * (TODO-444 §3.3): CRUD, the count quotas (per-scope, per-user aggregate, and the race-safe aggregate under parallel
 * creates in different scopes), replace-does-not-count, dangling-active resolution, per-user isolation, and the
 * derived {@code getActive}.
 */
class InMemorySavedViewStore_Test extends TestBase {

	private static final String A = "alice", B = "bob";
	private static final String PAGE = "orders", VIEW = "grid";
	private static final String BLOB = "{\"schemaVersion\":1}";

	//------------------------------------------------------------------------------------------------------------------
	// a) Basic CRUD + listing
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_saveThenLoadThenList() {
		var s = new InMemorySavedViewStore();
		s.save(A, PAGE, VIEW, "v1", BLOB);
		assertEquals(BLOB, s.load(A, PAGE, VIEW, "v1"));
		var listing = s.list(A, PAGE, VIEW);
		assertEquals(List.of("v1"), listing.names());
		assertNull(listing.active());
	}

	@Test void a02_loadMissingIsNull() {
		var s = new InMemorySavedViewStore();
		assertNull(s.load(A, PAGE, VIEW, "nope"));
		assertNull(s.load(A, PAGE, VIEW, "nope"));  // unknown scope path too
	}

	@Test void a03_unknownScopeListsEmpty() {
		var s = new InMemorySavedViewStore();
		var listing = s.list(A, PAGE, VIEW);
		assertNull(listing.active());
		assertTrue(listing.names().isEmpty());
	}

	@Test void a04_deleteRemovesAndIsIdempotent() {
		var s = new InMemorySavedViewStore();
		s.save(A, PAGE, VIEW, "v1", BLOB);
		s.delete(A, PAGE, VIEW, "v1");
		assertNull(s.load(A, PAGE, VIEW, "v1"));
		assertDoesNotThrow(() -> s.delete(A, PAGE, VIEW, "v1"));  // no-op on missing
		assertDoesNotThrow(() -> s.delete(A, "other", VIEW, "v1"));  // no-op on missing scope
	}

	@Test void a05_replaceOverwritesBlobKeepsSingleName() {
		var s = new InMemorySavedViewStore();
		s.save(A, PAGE, VIEW, "v1", BLOB);
		s.save(A, PAGE, VIEW, "v1", "{\"schemaVersion\":1,\"x\":2}");
		assertEquals("{\"schemaVersion\":1,\"x\":2}", s.load(A, PAGE, VIEW, "v1"));
		assertEquals(List.of("v1"), s.list(A, PAGE, VIEW).names());
	}

	@Test void a06_standaloneNullPageIsADistinctScopeFromPageQualified() {
		var s = new InMemorySavedViewStore();
		s.save(A, null, VIEW, "v1", BLOB);
		s.save(A, PAGE, VIEW, "v2", BLOB);
		assertEquals(List.of("v1"), s.list(A, null, VIEW).names());
		assertEquals(List.of("v2"), s.list(A, PAGE, VIEW).names());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Active pointer: set / clear / saveAndActivate / dangling resolution
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_saveAndActivateStoresBlobAndFlipsActive() {
		var s = new InMemorySavedViewStore();
		s.saveAndActivate(A, PAGE, VIEW, "v1", BLOB);
		var listing = s.list(A, PAGE, VIEW);
		assertEquals("v1", listing.active());
		assertEquals(List.of("v1"), listing.names());
		assertEquals(BLOB, s.load(A, PAGE, VIEW, "v1"));
	}

	@Test void b02_setActiveThenClear() {
		var s = new InMemorySavedViewStore();
		s.save(A, PAGE, VIEW, "v1", BLOB);
		s.setActive(A, PAGE, VIEW, "v1");
		assertEquals("v1", s.list(A, PAGE, VIEW).active());
		s.setActive(A, PAGE, VIEW, null);
		assertNull(s.list(A, PAGE, VIEW).active());
	}

	@Test void b03_danglingActiveNeverPersistedResolvesToNull() {
		var s = new InMemorySavedViewStore();
		s.setActive(A, PAGE, VIEW, "ghost");  // activate a name that was never saved
		assertNull(s.list(A, PAGE, VIEW).active());  // resolves to Default (null), not an error
	}

	@Test void b04_danglingActiveAfterDeleteResolvesToNull() {
		var s = new InMemorySavedViewStore();
		s.saveAndActivate(A, PAGE, VIEW, "v1", BLOB);
		s.delete(A, PAGE, VIEW, "v1");
		var listing = s.list(A, PAGE, VIEW);
		assertNull(listing.active());
		assertTrue(listing.names().isEmpty());
	}

	@Test void b05_getActiveDerivesFromListWithDanglingResolution() {
		var s = new InMemorySavedViewStore();
		assertNull(s.getActive(A, PAGE, VIEW));
		s.saveAndActivate(A, PAGE, VIEW, "v1", BLOB);
		assertEquals("v1", s.getActive(A, PAGE, VIEW));
		s.setActive(A, PAGE, VIEW, "ghost");
		assertNull(s.getActive(A, PAGE, VIEW));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Quotas: per-scope count, replace-does-not-count, per-user aggregate
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_perScopeCountCapTripsOnCreate() {
		var s = new InMemorySavedViewStore(3, 100);
		for (var i = 0; i < 3; i++)
			s.save(A, PAGE, VIEW, "v" + i, BLOB);
		var e = assertThrows(SavedViewQuotaException.class, () -> s.save(A, PAGE, VIEW, "v3", BLOB));
		assertEquals(SavedViewQuotaException.Kind.SCOPE, e.kind());
	}

	@Test void c02_replaceDoesNotTripPerScopeCap() {
		var s = new InMemorySavedViewStore(3, 100);
		for (var i = 0; i < 3; i++)
			s.save(A, PAGE, VIEW, "v" + i, BLOB);
		// Overwriting an existing name at the cap must succeed (replace, not create).
		assertDoesNotThrow(() -> s.save(A, PAGE, VIEW, "v0", "{\"schemaVersion\":1,\"y\":1}"));
	}

	@Test void c03_perUserAggregateCapAcrossDifferentScopes() {
		var s = new InMemorySavedViewStore(100, 3);
		s.save(A, "p1", VIEW, "v", BLOB);
		s.save(A, "p2", VIEW, "v", BLOB);
		s.save(A, "p3", VIEW, "v", BLOB);
		var e = assertThrows(SavedViewQuotaException.class, () -> s.save(A, "p4", VIEW, "v", BLOB));
		assertEquals(SavedViewQuotaException.Kind.AGGREGATE, e.kind());
	}

	@Test void c04_aggregateCapCountsAcrossScopesNotPerScope() {
		// Aggregate=3, per-scope=100: three creates spread over scopes fill the aggregate, a 4th anywhere trips it.
		var s = new InMemorySavedViewStore(100, 3);
		s.save(A, "p1", VIEW, "a", BLOB);
		s.save(A, "p1", VIEW, "b", BLOB);
		s.save(A, "p2", VIEW, "c", BLOB);
		assertThrows(SavedViewQuotaException.class, () -> s.save(A, "p2", VIEW, "d", BLOB));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Race-safety: parallel creates in DIFFERENT scopes for the same user cannot both bypass the aggregate cap
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parallelDifferentScopeCreatesRespectAggregateCap() throws Exception {
		var cap = 20;
		var threads = 64;
		var s = new InMemorySavedViewStore(1000, cap);
		var pool = Executors.newFixedThreadPool(16);
		var start = new CountDownLatch(1);
		var ok = new AtomicInteger();
		var quota = new AtomicInteger();
		var futures = new ArrayList<Future<?>>();
		try {
			for (var i = 0; i < threads; i++) {
				var scope = "p" + i;  // every thread writes in its OWN (page,view) scope
				futures.add(pool.submit(() -> {
					await(start);
					try {
						s.save(A, scope, VIEW, "v", BLOB);
						ok.incrementAndGet();
					} catch (SavedViewQuotaException e) {
						quota.incrementAndGet();
					}
				}));
			}
			start.countDown();
			for (var f : futures)
				f.get(30, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
		assertEquals(cap, ok.get(), "exactly the cap should have been created");
		assertEquals(threads - cap, quota.get(), "the rest should have been rejected as quota");
		var total = 0;
		for (var i = 0; i < threads; i++)
			total += s.list(A, "p" + i, VIEW).names().size();
		assertEquals(cap, total, "the store must hold no more than the aggregate cap");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Per-user isolation: one user's data is invisible to another
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_usersAreIsolated() {
		var s = new InMemorySavedViewStore();
		s.saveAndActivate(A, PAGE, VIEW, "mine", BLOB);
		assertTrue(s.list(B, PAGE, VIEW).names().isEmpty());
		assertNull(s.load(B, PAGE, VIEW, "mine"));
		assertNull(s.list(B, PAGE, VIEW).active());
		// B writing the same (page,view,name) does not disturb A.
		s.save(B, PAGE, VIEW, "mine", "{\"schemaVersion\":1,\"who\":\"bob\"}");
		assertEquals(BLOB, s.load(A, PAGE, VIEW, "mine"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Argument guards
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_constructorRejectsNonPositiveCaps() {
		assertThrows(IllegalArgumentException.class, () -> new InMemorySavedViewStore(0, 1));
		assertThrows(IllegalArgumentException.class, () -> new InMemorySavedViewStore(1, 0));
	}

	@Test void f02_nullRequiredKeysRejected() {
		var s = new InMemorySavedViewStore();
		assertThrows(IllegalArgumentException.class, () -> s.list(null, PAGE, VIEW));
		assertThrows(IllegalArgumentException.class, () -> s.list(A, PAGE, null));
		assertThrows(IllegalArgumentException.class, () -> s.save(A, PAGE, VIEW, null, BLOB));
		assertThrows(IllegalArgumentException.class, () -> s.save(A, PAGE, VIEW, "v", null));
	}

	private static void await(CountDownLatch l) {
		try {
			l.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
}
