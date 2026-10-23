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
package org.apache.juneau.commons.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link InMemoryReplayCache}: first-seen/replay outcomes, atomicity under concurrent submission of
 * the same identifier, and self-eviction of already-expired records.
 */
class InMemoryReplayCache_Test extends TestBase {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

	/** A test clock the test can advance manually. */
	static final class MutableClock extends Clock {
		private Instant now;
		MutableClock(Instant start) { now = start; }
		void advance(Duration d) { now = now.plus(d); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}

	@Test void a01_firstSeenIdReturnsTrue() {
		var cache = new InMemoryReplayCache();
		assertTrue(cache.checkAndRecord("id-1", System.currentTimeMillis() + 60_000L));
	}

	@Test void a02_replayOfUnexpiredIdReturnsFalse() {
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertTrue(cache.checkAndRecord("id-1", expiresAtMs));
		assertFalse(cache.checkAndRecord("id-1", expiresAtMs));
	}

	@Test void a03_distinctIdsAreIndependent() {
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertTrue(cache.checkAndRecord("id-1", expiresAtMs));
		assertTrue(cache.checkAndRecord("id-2", expiresAtMs));
	}

	@Test void a04_concurrentSubmitOfSameId_exactlyOneReturnsTrue() throws InterruptedException {
		// Atomic check-and-record: of many concurrent calls racing on the same id, exactly one must observe
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
				if (cache.checkAndRecord("id-race", expiresAtMs))
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
		// A record's own expiresAtMs bounds its retention: once expired it is swept out by a later call, and an
		// id resubmitted after that sweep is observed as first-seen again rather than as a replay. Uses a
		// sweep-every-call cache (interval 0) so the eviction is deterministic in-test.
		var cache = new InMemoryReplayCache(Clock.fixed(T0, ZoneOffset.UTC), 0);
		var now = T0.toEpochMilli();
		assertTrue(cache.checkAndRecord("id-1", now - 1_000L));  // recorded already-expired
		// With interval 0 every later call sweeps expired records before recording its own.
		assertTrue(cache.checkAndRecord("id-2", now + 60_000L));
		assertTrue(cache.checkAndRecord("id-1", now - 1_000L));  // id-1 was evicted; this is first-seen again
	}

	@Test void a06_nullIdRejected() {
		// A null id is a contract violation, never a legitimate identifier: the cache rejects it at the door with
		// IllegalArgumentException rather than letting it reach putIfAbsent (which would NPE).
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertThrows(IllegalArgumentException.class, () -> cache.checkAndRecord(null, expiresAtMs));
	}

	@Test void a07_evictionIsThrottledExpiredRecordSurvivesUntilSweepWindow() {
		// The sweep is throttled: with a large interval, an expired record is NOT swept on the immediately
		// following call, so a still-recorded (though expired) id is observed as a replay until the window
		// elapses. This documents the throttle honestly -- eviction bounds memory opportunistically.
		var cache = new InMemoryReplayCache(Clock.fixed(T0, ZoneOffset.UTC), 600_000L);  // time never reaches the next sweep
		var now = T0.toEpochMilli();
		assertTrue(cache.checkAndRecord("id-1", now - 1_000L));  // first call arms the window (sweeps an empty map)
		assertFalse(cache.checkAndRecord("id-1", now - 1_000L));  // still present: throttle suppressed the sweep
	}

	@Test void a08_negativeSweepIntervalRejected() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryReplayCache(-1L));
	}

	@Test void a09_nullClockRejected() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryReplayCache(null, 0L));
	}

	@Test void a10_recordIsEvictedOnceClockPassesItsExpiry() {
		// Clock-driven: an unexpired record is a replay until the clock reaches its expiresAtMs, after which the next
		// call sweeps it and the same id is first-seen again. No sleeping, so the outcome never depends on machine speed.
		var clock = new MutableClock(T0);
		var cache = new InMemoryReplayCache(clock, 0);
		var expiresAtMs = T0.plusSeconds(60).toEpochMilli();
		assertTrue(cache.checkAndRecord("id-1", expiresAtMs));
		clock.advance(Duration.ofSeconds(59));
		assertFalse(cache.checkAndRecord("id-1", expiresAtMs));  // still unexpired: replay
		clock.advance(Duration.ofSeconds(1));
		assertTrue(cache.checkAndRecord("id-1", expiresAtMs));  // expiresAtMs <= now: swept, so first-seen again
	}

	@Test void a11_throttledSweepRunsOnceTheIntervalElapses() {
		// The throttle window is measured on the injected clock: an expired record survives calls made inside the
		// window and is swept by the first call at or after it.
		var clock = new MutableClock(T0);
		var cache = new InMemoryReplayCache(clock, 1_000L);
		var expiredAtMs = T0.toEpochMilli() - 1L;
		assertTrue(cache.checkAndRecord("id-1", expiredAtMs));  // arms the window at T0 + 1s
		clock.advance(Duration.ofMillis(999));
		assertFalse(cache.checkAndRecord("id-1", expiredAtMs));  // inside the window: no sweep yet
		clock.advance(Duration.ofMillis(1));
		assertTrue(cache.checkAndRecord("id-1", expiredAtMs));  // window elapsed: swept, so first-seen again
	}
}
