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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link InMemoryReplayCache}: first-seen/replay outcomes, atomicity under concurrent submission of
 * the same {@code jti}, and self-eviction of already-expired records.
 */
class InMemoryReplayCache_Test {

	@Test void a01_firstSeenJtiReturnsTrue() {
		var cache = new InMemoryReplayCache();
		assertTrue(cache.checkAndRecord("jti-1", System.currentTimeMillis() + 60_000L));
	}

	@Test void a02_replayOfUnexpiredJtiReturnsFalse() {
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertTrue(cache.checkAndRecord("jti-1", expiresAtMs));
		assertFalse(cache.checkAndRecord("jti-1", expiresAtMs));
	}

	@Test void a03_distinctJtisAreIndependent() {
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertTrue(cache.checkAndRecord("jti-1", expiresAtMs));
		assertTrue(cache.checkAndRecord("jti-2", expiresAtMs));
	}

	@Test void a04_concurrentSubmitOfSameJti_exactlyOneReturnsTrue() throws InterruptedException {
		// Atomic check-and-record: of many concurrent calls racing on the same jti, exactly one must observe
		// first-seen (true) -- proves the ConcurrentHashMap#putIfAbsent-based implementation has no TOCTOU window.
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		var threadCount = 32;
		var firstSeenCount = new AtomicInteger();
		var start = new CountDownLatch(1);
		var threads = new ArrayList<Thread>();
		for (var i = 0; i < threadCount; i++) {
			var t = new Thread(() -> {
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				if (cache.checkAndRecord("jti-race", expiresAtMs))
					firstSeenCount.incrementAndGet();
			});
			threads.add(t);
			t.start();
		}
		start.countDown();
		for (var t : threads)
			t.join();
		assertEquals(1, firstSeenCount.get());
	}

	@Test void a05_alreadyExpiredRecordIsEvictedAndResubmissionIsFirstSeenAgain() {
		// A record's own expiresAtMs bounds its retention: once expired it is swept out by a later call, and a
		// jti resubmitted after that sweep is observed as first-seen again rather than as a replay. Correctness
		// does not depend on this -- an expired token is separately rejected by the dispatcher's own expiry
		// check before a ReplayCache is ever consulted -- eviction exists purely to bound memory. Uses a
		// sweep-every-call cache (interval 0) so the eviction is deterministic in-test.
		var cache = new InMemoryReplayCache(0);
		var now = System.currentTimeMillis();
		assertTrue(cache.checkAndRecord("jti-1", now - 1_000L));  // recorded already-expired
		// With interval 0 every later call sweeps expired records before recording its own.
		assertTrue(cache.checkAndRecord("jti-2", now + 60_000L));
		assertTrue(cache.checkAndRecord("jti-1", now - 1_000L));  // jti-1 was evicted; this is first-seen again
	}

	@Test void a06_nullJtiRejected() {
		// A null jti is a codec/contract violation, never a legitimate token: the cache rejects it at the door
		// with IllegalArgumentException rather than letting it reach putIfAbsent (which would NPE and, via the
		// dispatcher's fail-open catch, silently disable replay protection).
		var cache = new InMemoryReplayCache();
		assertThrows(IllegalArgumentException.class, () -> cache.checkAndRecord(null, System.currentTimeMillis() + 60_000L));
	}

	@Test void a07_evictionIsThrottledExpiredRecordSurvivesUntilSweepWindow() {
		// The sweep is throttled: with a large interval, an expired record is NOT swept on the immediately
		// following call, so a still-recorded (though expired) jti is observed as a replay until the window
		// elapses. This documents the throttle honestly -- eviction bounds memory opportunistically, it is not a
		// per-call correctness mechanism (the dispatcher's own expiry check is).
		var cache = new InMemoryReplayCache(600_000L);  // 10-minute sweep window: no sweep will fire during this test
		var now = System.currentTimeMillis();
		assertTrue(cache.checkAndRecord("jti-1", now - 1_000L));  // first call arms the window (sweeps an empty map)
		assertFalse(cache.checkAndRecord("jti-1", now - 1_000L));  // still present: throttle suppressed the sweep
	}
}
