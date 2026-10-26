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
package org.apache.juneau.console.views;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit test for the in-memory {@link AsyncJobRegistry}.
 *
 * <p>
 * Pins the load-bearing server-side controls: the unguessable {@value AsyncJobRegistry#CAPABILITY_BITS}-bit
 * capability id, the concurrent-job cap, the enumeration-resistant lookup, and the deterministic
 * hard-timeout sweep + retention reaping (driven by an injected clock rather than wall time).
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambda may invoke helpers that also throw; splitting would obscure the LNN case.
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
	// a) The capability id is unguessable: 256-bit SecureRandom, hex, unique
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
		var seen = new HashSet<String>();
		for (var i = 0; i < 50; i++) {
			var job = r.create();
			job.complete(ActionResult.success(null));
			seen.add(job.id());
		}
		assertSize(() -> "minted capability ids must be unique (SecureRandom)", 50, seen);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Enumeration-resistant lookup: an unknown id is empty, indistinguishable from any other
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
	// c) Concurrent-job cap: the 9th is refused; a freed slot re-opens it
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
	// d) Hard-timeout sweep (deterministic via the injected clock) - a data-egress bound
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
	// f) Cancel by id
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
	// g) A registry-minted job carries the configured subscriber cap
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

	//------------------------------------------------------------------------------------------------------------------
	// i) De-duplication by IdempotencyKey
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_duplicateSubmitReturnsSameJob() {
		var r = registry(new TestClock(T0));
		var key = IdempotencyKey.mintSelfTargeted("create");
		var first = r.create(key);
		assertSame(first, r.create(key));
		assertSame(first, r.tryCreate(key).orElseThrow());
		assertSame(first, r.get(first.id()).orElseThrow());
		assertSize(1, r);
	}

	@Test void i02_distinctKeysGiveDistinctJobs() {
		var r = registry(new TestClock(T0));
		var a = r.create(IdempotencyKey.mintSelfTargeted("create"));
		var b = r.create(IdempotencyKey.mintSelfTargeted("create"));
		assertNotSame(a, b);
		assertSize(2, r);
	}

	@Test void i03_concurrentDuplicateSubmitsYieldOneJob() throws Exception {
		var r = registry(new TestClock(T0));
		var key = IdempotencyKey.mintSelfTargeted("create");
		var threads = 16;
		var start = new java.util.concurrent.CountDownLatch(1);
		var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
		try {
			var futures = new ArrayList<java.util.concurrent.Future<AsyncJob>>();
			for (var i = 0; i < threads; i++)
				futures.add(pool.submit(() -> { start.await(); return r.create(key); }));
			start.countDown();
			var ids = new HashSet<String>();
			for (var f : futures)
				ids.add(f.get().id());
			assertSize(1, ids);
			assertSize(1, r);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void i04_finishedKeyReplaysTerminalJobThenForgottenAfterReap() {
		var clock = new TestClock(T0);
		var r = registry(clock);
		var key = IdempotencyKey.mintSelfTargeted("create");
		var job = r.create(key);
		job.complete(ActionResult.success(null));
		// Within retention: the duplicate gets the finished job (and its result) back; nothing new runs.
		clock.set(T0.plusSeconds(30));
		var replay = r.create(key);
		assertSame(job, replay);
		assertTrue(replay.isTerminal());
		// Past retention: the job is reaped and the key is forgotten, so the same key starts a fresh job.
		clock.set(T0.plus(AsyncJobRegistry.RETENTION).plusSeconds(1));
		var fresh = r.create(key);
		assertNotSame(job, fresh);
		assertFalse(fresh.isTerminal());
	}

	@Test void i05_duplicateIsNotRefusedByConcurrencyCap() {
		var r = registry(new TestClock(T0));
		var key = IdempotencyKey.mintSelfTargeted("create");
		var job = r.create(key);
		for (var i = 1; i < AsyncJobRegistry.MAX_CONCURRENT_JOBS; i++)
			r.create();
		assertEquals(AsyncJobRegistry.MAX_CONCURRENT_JOBS, r.runningCount());
		assertSame(job, r.tryCreate(key).orElseThrow(), "a duplicate creates nothing, so the cap must not refuse it");
		assertTrue(r.tryCreate(IdempotencyKey.mintSelfTargeted("create")).isEmpty(), "a new key is still subject to the cap");
		assertThrows(IllegalStateException.class, () -> r.create(IdempotencyKey.mintSelfTargeted("create")));
		assertThrows(IllegalArgumentException.class, () -> r.create((IdempotencyKey) null));
	}
	//------------------------------------------------------------------------------------------------------------------
	// j) The concurrent-job cap is atomic under contention
	//------------------------------------------------------------------------------------------------------------------

	private static final int RACE_THREADS = 32;
	private static final int RACE_ROUNDS = 200;

	/** Races {@link #RACE_THREADS} callers (released together by a latch) against a fresh registry, {@link #RACE_ROUNDS} times, and asserts exactly {@link AsyncJobRegistry#MAX_CONCURRENT_JOBS} were admitted. */
	private static void race(java.util.function.BiFunction<AsyncJobRegistry,Integer,Optional<AsyncJob>> call) throws Exception {
		var pool = java.util.concurrent.Executors.newFixedThreadPool(RACE_THREADS);
		try {
			for (var round = 0; round < RACE_ROUNDS; round++) {
				var r = registry(new TestClock(T0));
				var ready = new java.util.concurrent.CountDownLatch(RACE_THREADS);
				var start = new java.util.concurrent.CountDownLatch(1);
				var futures = new ArrayList<java.util.concurrent.Future<Optional<AsyncJob>>>();
				for (var i = 0; i < RACE_THREADS; i++) {
					var n = i;
					futures.add(pool.submit(() -> { ready.countDown(); start.await(); return call.apply(r, n); }));
				}
				ready.await();
				start.countDown();
				var admitted = 0;
				for (var f : futures)
					if (f.get().isPresent())
						admitted++;
				assertBean(Map.of("admitted", admitted, "running", r.runningCount(), "size", r.size()), "admitted,running,size", "8,8,8");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void j01_concurrentUnkeyedTryCreateNeverExceedsCap() throws Exception {
		race((r, n) -> r.tryCreate());
	}

	@Test void j02_concurrentDistinctKeysNeverExceedCap() throws Exception {
		race((r, n) -> r.tryCreate(IdempotencyKey.mintSelfTargeted("create")));
	}

	@Test void j03_concurrentKeyedAndUnkeyedMixNeverExceedCap() throws Exception {
		race((r, n) -> n % 2 == 0 ? r.tryCreate() : r.tryCreate(IdempotencyKey.mintSelfTargeted("create")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// k) The scheduled hard timeout settles the job even when its task fires at (or a tick before) the deadline
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Captures scheduled tasks and their delays instead of running them, so a test fires each one at a chosen clock
	 * instant.  The registry schedules a {@code Callable} (its lambda returns a value), so both overloads capture.
	 */
	private static final class CapturingScheduler extends ScheduledThreadPoolExecutor {
		final List<Runnable> tasks = new ArrayList<>();
		final List<Long> delays = new ArrayList<>();
		CapturingScheduler() { super(1); }
		@Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
			tasks.add(command);
			delays.add(unit.toNanos(delay));
			return null;
		}
		@Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
			schedule(() -> {
				try {
					callable.call();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}, delay, unit);
			return null;
		}
	}

	private static AsyncJobRegistry registry(TestClock clock, CapturingScheduler s) {
		return new AsyncJobRegistry(clock, Duration.ofSeconds(5), AsyncJobRegistry.MAX_OUTPUT_BYTES,
			AsyncJobRegistry.MAX_SUBSCRIBERS_PER_JOB, s);
	}

	private static long millis(long nanos) {
		return TimeUnit.NANOSECONDS.toMillis(nanos);
	}

	@Test void k01_wholeMillisecondDeadlineSchedulesOnePast() {
		var clock = new TestClock(T0);
		var s = new CapturingScheduler();
		try {
			var job = registry(clock, s).create();
			// The deadline is exactly 5000 ms away; a task at 5000 ms would land on it, where nothing settles.
			assertEquals(List.of(5001L), s.delays.stream().map(AsyncJobRegistry_Test::millis).toList());
			clock.set(job.deadline());                     // timer granularity can still fire it here
			s.tasks.get(0).run();
			assertFalse(job.isTerminal());
			assertEquals(List.of(5001L, 1L), s.delays.stream().map(AsyncJobRegistry_Test::millis).toList(),
				"a timeout task that fires on the deadline must re-arm itself one millisecond past it");
			clock.set(job.deadline().plusMillis(1));
			s.tasks.get(1).run();
			assertEquals("app:hard-timeout-exceeded", job.result().message);
		} finally {
			s.shutdownNow();
		}
	}

	@Test void k02_earlyFireReArmsRoundedUp() {
		var clock = new TestClock(T0);
		var s = new CapturingScheduler();
		try {
			var job = registry(clock, s).create();
			clock.set(job.deadline().minusNanos(2_400_000));  // fires 2.4 ms early
			s.tasks.get(0).run();
			assertFalse(job.isTerminal());
			assertSize(() -> "a timeout task that fires early must re-arm itself", 2, s.tasks);
			assertEquals(3L, millis(s.delays.get(1)), "2.4 ms to the deadline must round up to 3 ms, not down to 2");
			clock.set(clock.instant().plusMillis(3));
			s.tasks.get(1).run();
			assertEquals("app:hard-timeout-exceeded", job.result().message);
			assertSize(2, s.tasks);
		} finally {
			s.shutdownNow();
		}
	}

	@Test void k03_reArmedTaskStopsOnceTheJobSettles() {
		var clock = new TestClock(T0);
		var s = new CapturingScheduler();
		try {
			var job = registry(clock, s).create();
			clock.set(job.deadline().minusMillis(1));
			s.tasks.get(0).run();
			assertSize(() -> "a timeout task that fires early must re-arm itself", 2, s.tasks);
			job.complete(ActionResult.success(null));
			clock.set(job.deadline());                     // the re-armed task also fires a tick early
			s.tasks.get(1).run();
			assertSize(() -> "a settled job must not keep re-arming its timeout", 2, s.tasks);
			assertEquals("success", job.result().outcome);
		} finally {
			s.shutdownNow();
		}
	}
}
