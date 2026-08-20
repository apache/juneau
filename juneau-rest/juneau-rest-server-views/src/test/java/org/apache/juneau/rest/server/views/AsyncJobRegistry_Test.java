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

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit test for the in-memory {@link AsyncJobRegistry} (design doc §6.3; {@code TODO-425}, Q2).
 *
 * <p>
 * Pins the load-bearing server-side controls: the unguessable {@value AsyncJobRegistry#CAPABILITY_BITS}-bit
 * capability id (HIGH-4), the concurrent-job cap, the enumeration-resistant lookup, and the deterministic
 * hard-timeout sweep + retention reaping (driven by an injected clock rather than wall time).
 */
@SuppressWarnings({
	"resource" // AsyncJobRegistry is AutoCloseable; test-fixture lifecycle is managed by the test, not a real leak (mixed-module resource analysis on test code).
})
class AsyncJobRegistry_Test extends TestBase {

	/** A test clock whose instant is advanced explicitly, so timeout sweeps are deterministic (no wall-clock waits). */
	private static final class TestClock extends Clock {
		private Instant now;
		TestClock(Instant now) { this.now = now; }
		void set(Instant v) { now = v; }
		@Override public Instant instant() { return now; }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId z) { return this; }
	}

	private static final Instant T0 = Instant.parse("2026-08-19T00:00:00Z");

	/** A registry with no background scheduler (timeouts are enforced only by explicit sweeps) on the given clock. */
	private static AsyncJobRegistry registry(TestClock clock) {
		return new AsyncJobRegistry(clock, Duration.ofSeconds(120), AsyncJobRegistry.MAX_OUTPUT_BYTES,
			AsyncJobRegistry.MAX_SUBSCRIBERS_PER_JOB, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The capability id is unguessable: 256-bit SecureRandom, hex, unique (HIGH-4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_capabilityIdIs256BitHex() {
		var id = registry(new TestClock(T0)).create().id();
		assertEquals(64, id.length(), id);                 // 32 bytes -> 64 lowercase hex chars
		assertTrue(id.matches("[0-9a-f]{64}"), id);
		assertEquals(256, AsyncJobRegistry.CAPABILITY_BITS);
	}

	@Test void a02_capabilityIdsAreUnique() {
		var r = registry(new TestClock(T0));
		// Settle each job as it is minted so the concurrency cap never blocks the sample (a terminal job frees its
		// running slot); we are pinning id uniqueness, not the cap (that is c01).
		var seen = Stream.generate(() -> {
				var job = r.create();
				job.complete(ActionResult.success(null));
				return job.id();
			}).limit(50).collect(Collectors.toSet());
		assertEquals(50, seen.size(), "minted capability ids must be unique (SecureRandom)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Enumeration-resistant lookup: an unknown id is empty, indistinguishable from any other (HIGH-4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getUnknownIsEmpty() {
		var r = registry(new TestClock(T0));
		var job = r.create();
		assertTrue(r.get(job.id()).isPresent());
		assertTrue(r.get("deadbeef").isEmpty());
		assertTrue(r.get("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").isEmpty());
		assertTrue(r.get(null).isEmpty());
		assertTrue(r.get("  ").isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Concurrent-job cap: the 9th is refused; a freed slot re-opens it (MED-6)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_concurrentJobCapRefusesNinth() {
		var r = registry(new TestClock(T0));
		var jobs = new ArrayList<AsyncJob>();
		for (var i = 0; i < AsyncJobRegistry.MAX_CONCURRENT_JOBS; i++)
			jobs.add(r.tryCreate().orElseThrow());
		assertEquals(8, AsyncJobRegistry.MAX_CONCURRENT_JOBS);
		assertEquals(8, r.runningCount());
		assertTrue(r.tryCreate().isEmpty(), "9th concurrent job must be refused");
		// create() (the throwing form) signals the same cap.
		assertThrows(IllegalStateException.class, r::create);
		// Settling one frees a slot (terminal jobs do not count as concurrent).
		jobs.get(0).complete(ActionResult.success(null));
		assertEquals(7, r.runningCount());
		assertTrue(r.tryCreate().isPresent(), "a settled job frees a concurrency slot");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Hard-timeout sweep (deterministic via the injected clock) - a data-egress bound (HIGH-4/MED-6/Task-11)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_sweepTimesOutExpiredJobs() {
		var clock = new TestClock(T0);
		var r = registry(clock);
		var job = r.create();
		clock.set(T0.plusSeconds(121));
		r.sweepTimeouts();
		assertTrue(job.isTerminal());
		assertEquals("unknown", job.result().outcome);
	}

	@Test void d02_tryCreateSweepsSoAnExpiredJobFreesItsSlot() {
		var clock = new TestClock(T0);
		var r = registry(clock);
		for (var i = 0; i < AsyncJobRegistry.MAX_CONCURRENT_JOBS; i++)
			r.tryCreate().orElseThrow();
		assertTrue(r.tryCreate().isEmpty());               // full
		clock.set(T0.plusSeconds(121));                    // every job is now past its deadline
		assertTrue(r.tryCreate().isPresent(), "tryCreate must sweep expired jobs and reopen a slot");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Retention reaping: a terminal job is kept for re-attach, then reaped
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_terminalJobRetainedThenReaped() {
		var clock = new TestClock(T0);
		var r = registry(clock);
		var job = r.create();
		job.complete(ActionResult.success(null));
		// Within retention: still present (a page reload can re-attach and read the result).
		clock.set(T0.plusSeconds(30));
		r.sweepTimeouts();
		assertTrue(r.get(job.id()).isPresent());
		// Past retention: reaped from the heap.
		clock.set(T0.plus(AsyncJobRegistry.RETENTION).plusSeconds(1));
		r.sweepTimeouts();
		assertTrue(r.get(job.id()).isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Cancel by id (Q4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_cancelById() {
		var r = registry(new TestClock(T0));
		var job = r.create();
		var cancelled = r.cancel(job.id());
		assertTrue(cancelled.isPresent());
		assertEquals("cancelled", cancelled.get().result().outcome);
		assertTrue(r.cancel("no-such-id").isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) A registry-minted job carries the configured subscriber cap (MED-6)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_mintedJobCarriesSubscriberCap() {
		var job = registry(new TestClock(T0)).create();
		assertEquals(AsyncJobRegistry.MAX_SUBSCRIBERS_PER_JOB, job.maxSubscribers());
		assertEquals(AsyncJobRegistry.MAX_OUTPUT_BYTES, job.maxOutputBytes());
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) The default (production) registry mints a private daemon scheduler that closes cleanly
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_defaultRegistryCreatesAndCloses() {
		try (var r = new AsyncJobRegistry()) {
			var job = r.create();
			assertNotNull(job.id());
			assertEquals(120, AsyncJobRegistry.HARD_TIMEOUT.getSeconds());
		}
	}

	@Test void h02_publicTimeoutConstructorHonorsLongerDuration() {
		try (var r = new AsyncJobRegistry(Duration.ofMinutes(10))) {
			var job = r.create();
			assertEquals(Duration.ofMinutes(10), Duration.between(job.createdAt(), job.deadline()));
			assertEquals(120, AsyncJobRegistry.HARD_TIMEOUT.getSeconds(), "global default must stay 120s");
		}
	}

	@Test void h03_publicTimeoutConstructorRejectsNonPositive() {
		assertThrows(IllegalArgumentException.class, () -> new AsyncJobRegistry((Duration) null));
		assertThrows(IllegalArgumentException.class, () -> new AsyncJobRegistry(Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> new AsyncJobRegistry(Duration.ofSeconds(-1)));
	}
}
