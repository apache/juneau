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
package org.apache.juneau.rest.guard;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.guard.RateLimitGuard.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RateLimitGuard.Storage#snapshot()} default-vs-override behavior and the
 * shape of the {@link BucketState} records the bundled in-memory storage emits.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class RateLimitGuard_Snapshot_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// In-memory storage surfaces live bucket state after activity.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_inMemorySnapshotReportsTokensAndRemainingAfterPartialDrain() {
		var s = RateLimitGuard.Storage.inMemory();
		s.tryAcquire("k", 5, 1.0);
		s.tryAcquire("k", 5, 1.0);
		s.tryAcquire("k", 5, 1.0);
		var snapshot = s.snapshot();
		assertEquals(1, snapshot.size());
		var b = snapshot.get("k");
		assertNotNull(b);
		assertEquals("k", b.key());
		// Started full (5), three tokens consumed → at-most ~2 left, refill barely measurable in the test
		// window so the actual fraction sits in (2.0, 2.0 + ε].
		assertTrue(b.tokens() >= 2.0 && b.tokens() < 3.0,
			"expected tokens in [2.0, 3.0), got " + b.tokens());
		assertEquals((int) Math.floor(b.tokens()), b.remaining());
		assertFalse(b.throttled(), "bucket should not be throttled with 2+ tokens left");
		assertNotNull(b.lastRequest());
		// Wall-clock instant should be within a few seconds of now.
		var skewMillis = Math.abs(System.currentTimeMillis() - b.lastRequest().toEpochMilli());
		assertTrue(skewMillis < 5_000L, "lastRequest drift too large: " + skewMillis + "ms");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exhausted bucket flips the throttled flag.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02_exhaustedBucketReportsThrottledTrueAndZeroRemaining() {
		var s = RateLimitGuard.Storage.inMemory();
		// burst of 1, drain it, then poke once more to fail (consumes the refill but stays empty).
		s.tryAcquire("k", 1, 0.01);
		s.tryAcquire("k", 1, 0.01);
		var b = s.snapshot().get("k");
		assertNotNull(b);
		assertTrue(b.tokens() < 1.0, "expected tokens < 1.0, got " + b.tokens());
		assertEquals(0, b.remaining());
		assertTrue(b.throttled(), "expected throttled=true on empty bucket");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Multi-key snapshot reports one entry per unique key.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_multiKeySnapshotHasOneEntryPerKey() {
		var s = RateLimitGuard.Storage.inMemory();
		s.tryAcquire("alpha", 3, 1.0);
		s.tryAcquire("beta", 3, 1.0);
		s.tryAcquire("gamma", 3, 1.0);
		var snapshot = s.snapshot();
		assertEquals(3, snapshot.size());
		assertEquals(Set.of("alpha", "beta", "gamma"), snapshot.keySet());
		for (var e : snapshot.entrySet())
			assertEquals(e.getKey(), e.getValue().key());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Empty storage returns an empty (but non-null) snapshot.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_emptyStorageSnapshotIsEmptyMap() {
		var s = RateLimitGuard.Storage.inMemory();
		var snapshot = s.snapshot();
		assertNotNull(snapshot);
		assertTrue(snapshot.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Snapshot returns an immutable map (defensive copy).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_snapshotIsImmutable() {
		var s = RateLimitGuard.Storage.inMemory();
		s.tryAcquire("k", 1, 1.0);
		var snapshot = s.snapshot();
		assertThrows(UnsupportedOperationException.class,
			() -> snapshot.put("nope", new BucketState("nope", 0.0, 0, true, Instant.EPOCH)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Backwards-compat: a custom Storage impl that does NOT override snapshot()
	// inherits the default which returns Map.of() without throwing.
	//------------------------------------------------------------------------------------------------------------------

	/** Custom storage that only implements the mandatory SPI methods. */
	static final class C01_NoSnapshotStorage implements RateLimitGuard.Storage {
		@Override public Storage.AcquireResult tryAcquire(String key, int capacity, double permitsPerSecond) {
			return new Storage.AcquireResult(true, capacity - 1, 0L);
		}
		@Override public void evict(Duration ttl) { /* intentionally empty */ }
	}

	@Test void c01_customStorageWithoutSnapshotOverrideReturnsEmptyMap() {
		RateLimitGuard.Storage s = new C01_NoSnapshotStorage();
		var snapshot = s.snapshot();
		assertNotNull(snapshot);
		assertTrue(snapshot.isEmpty());
		// Calling it twice should be safe.
		assertEquals(Map.of(), s.snapshot());
	}

	//------------------------------------------------------------------------------------------------------------------
	// RateLimitGuard.snapshot() convenience delegates to the configured storage.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_guardSnapshotDelegatesToStorage() {
		var g = RateLimitGuard.create()
			.permitsPerSecond(1)
			.burst(3)
			.build();
		assertSame(g.getStorage(), g.getStorage());  // sanity — same instance
		assertTrue(g.snapshot().isEmpty());
		g.getStorage().tryAcquire("k1", 3, 1.0);
		g.getStorage().tryAcquire("k2", 3, 1.0);
		var snapshot = g.snapshot();
		assertEquals(2, snapshot.size());
		assertEquals(Set.of("k1", "k2"), snapshot.keySet());
	}

	//------------------------------------------------------------------------------------------------------------------
	// New public config accessors on RateLimitGuard reflect builder inputs.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d02_guardConfigAccessorsReflectBuilderState() {
		var g = RateLimitGuard.create()
			.permitsPerMinute(60)
			.burst(42)
			.xForwardedForAware(true)
			.exemptPaths("/healthz", "/livez")
			.build();
		assertEquals(42, g.getCapacity());
		assertEquals(1.0, g.getPermitsPerSecond(), 1e-9);
		assertTrue(g.isXForwardedForAware());
		assertEquals(Set.of("/healthz", "/livez"), g.getExemptPaths());
		assertNotNull(g.getStorage());
	}
}
