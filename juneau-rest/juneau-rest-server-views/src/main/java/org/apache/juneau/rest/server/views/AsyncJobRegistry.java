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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * An in-memory, per-process registry of {@link AsyncJob async jobs} &mdash; the single-process job store a future
 * async write variant runs on (design doc §6.3, Q2).
 *
 * <h5 class='section'>Single-process, by decision</h5>
 * <p>
 * Job state lives only in this object's heap, which is enough for a single-user loopback console and a documented
 * limitation for anything else (Q2): a process restart drops every in-flight job, exactly as a restart invalidates
 * every {@link org.apache.juneau.rest.server.filter.SynchronizerToken}.  There is deliberately no store abstraction.
 *
 * <h5 class='section'>The id is the capability; the caps are load-bearing</h5>
 * <p>
 * Each job is keyed by an unguessable {@value #CAPABILITY_BITS}-bit {@link SecureRandom} id
 * ({@value #CAPABILITY_BYTES} bytes, hex-encoded), matching
 * {@link org.apache.juneau.rest.server.filter.SynchronizerToken}'s width &mdash; the id <b>is</b> the stream's
 * access control (HIGH-4).  {@link #get(String)} of an unknown id returns empty, so a miss is indistinguishable from
 * any other and the id space cannot be enumerated.
 * <p>
 * Because jobs keep running after the page navigates away (Q3) and stream customer-adjacent content, the following
 * caps are enforced server-side and are security controls, not tuning knobs (MED-6, a Task-11 disclosure bound):
 * <ul class='spaced-list'>
 * 	<li>{@link #MAX_CONCURRENT_JOBS} concurrent (non-terminal) jobs &mdash; {@link #tryCreate()} refuses beyond it;
 * 	<li>{@link AsyncJobRegistry#HARD_TIMEOUT} hard per-job timeout &mdash; scheduled here and also swept by
 * 		{@link #sweepTimeouts()};
 * 	<li>{@link AsyncJob#maxOutputBytes()} of streamed output per job &mdash; enforced in {@link AsyncJob#progress};
 * 	<li>{@link AsyncJob#maxSubscribers()} concurrent subscribers per job &mdash; enforced in
 * 		{@link AsyncJob#acquireSubscriber()}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AsyncJob}
 * 	<li class='jc'>{@link AsyncJobsMixin}
 * </ul>
 *
 * @since 10.0.0
 */
public final class AsyncJobRegistry implements AutoCloseable {

	/** Number of random bytes behind a capability id.  {@value #CAPABILITY_BITS} bits, matching {@code SynchronizerToken}. */
	public static final int CAPABILITY_BYTES = 32;

	/** Width of a capability id, in bits &mdash; well beyond the &ge;128-bit bar. */
	public static final int CAPABILITY_BITS = CAPABILITY_BYTES * 8;

	/** Maximum number of concurrent (non-terminal) jobs; {@link #tryCreate()} refuses beyond this (MED-6). */
	public static final int MAX_CONCURRENT_JOBS = 8;

	/** Maximum number of concurrent subscribers per job (a reload plus one straggler) (MED-6). */
	public static final int MAX_SUBSCRIBERS_PER_JOB = 2;

	/** Maximum streamed output per job, in bytes (1 MiB); a data-egress bound, not only a heap bound (MED-6). */
	public static final long MAX_OUTPUT_BYTES = 1024L * 1024L;

	/** Hard per-job timeout; caps how long triage keeps streaming customer-adjacent content (HIGH-4/MED-6/Task-11). */
	public static final Duration HARD_TIMEOUT = Duration.ofSeconds(120);

	/** How long a terminal job is retained (for a page-reload re-attach) before it is reaped from the heap. */
	static final Duration RETENTION = Duration.ofSeconds(60);

	private final Map<String,AsyncJob> jobs = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();
	private final Clock clock;
	private final Duration timeout;
	private final long maxOutputBytes;
	private final int maxSubscribers;
	private final ScheduledExecutorService scheduler;
	private final boolean ownsScheduler;

	/**
	 * Creates a registry with the production defaults: a system clock, the {@link #HARD_TIMEOUT} (120s) timeout,
	 * {@link #MAX_OUTPUT_BYTES} / {@link #MAX_SUBSCRIBERS_PER_JOB} caps, and a private daemon scheduler that enforces
	 * each job's hard timeout.
	 *
	 * <p>
	 * The 120s default is a disclosure bound for jobs that are not long-running agent dispatches.  Callers that need
	 * a longer bound (for example a Claude-length create) use {@link #AsyncJobRegistry(Duration)} rather than raising
	 * this global default.
	 * </p>
	 */
	public AsyncJobRegistry() {
		this(HARD_TIMEOUT);
	}

	/**
	 * Creates a registry with a caller-chosen hard per-job timeout and the production caps / scheduler.
	 *
	 * <p>
	 * Use this when a job may run longer than {@link #HARD_TIMEOUT} (120s).  The no-arg constructor keeps that
	 * default; this overload does not change it.
	 * </p>
	 *
	 * @param timeout The hard per-job timeout.  Must be a positive duration.
	 * @throws IllegalArgumentException If {@code timeout} is <jk>null</jk>, zero, or negative.
	 */
	public AsyncJobRegistry(Duration timeout) {
		this(Clock.systemUTC(), requirePositiveTimeout(timeout), MAX_OUTPUT_BYTES, MAX_SUBSCRIBERS_PER_JOB, defaultScheduler(), true);
	}

	/**
	 * Test/advanced constructor allowing an injected clock, timeout, caps and scheduler.
	 *
	 * @param clock The clock supplying job-creation and timeout-check instants.  Must not be <jk>null</jk>.
	 * @param timeout The hard per-job timeout.  Must be a positive duration.
	 * @param maxOutputBytes The per-job streamed-output cap, in bytes.
	 * @param maxSubscribers The per-job concurrent-subscriber cap.
	 * @param scheduler The scheduler that enforces each job's hard timeout, or <jk>null</jk> to rely solely on
	 * 	{@link #sweepTimeouts()} (used by deterministic tests).
	 */
	AsyncJobRegistry(Clock clock, Duration timeout, long maxOutputBytes, int maxSubscribers, ScheduledExecutorService scheduler) {
		this(clock, timeout, maxOutputBytes, maxSubscribers, scheduler, false);
	}

	private AsyncJobRegistry(Clock clock, Duration timeout, long maxOutputBytes, int maxSubscribers, ScheduledExecutorService scheduler, boolean ownsScheduler) {
		this.clock = assertArgNotNull("clock", clock);
		this.timeout = requirePositiveTimeout(timeout);
		this.maxOutputBytes = maxOutputBytes;
		this.maxSubscribers = maxSubscribers;
		this.scheduler = scheduler;
		this.ownsScheduler = ownsScheduler;
	}

	private static Duration requirePositiveTimeout(Duration timeout) {
		assertArgNotNull("timeout", timeout);
		if (timeout.isZero() || timeout.isNegative())
			throw iaex("AsyncJobRegistry timeout must be a positive duration, not %s.", timeout);
		return timeout;
	}

	private static ScheduledExecutorService defaultScheduler() {
		return Executors.newSingleThreadScheduledExecutor(r -> {
			var t = new Thread(r, "juneau-async-jobs");
			t.setDaemon(true);
			return t;
		});
	}

	/**
	 * Attempts to create and register a new job with a freshly-minted capability id.
	 *
	 * <p>
	 * Sweeps timeouts first (so a slot held only by an expired job is freed), then refuses when the running-job cap
	 * is already reached &mdash; an unbounded set of keep-running jobs would be unbounded egress of customer-adjacent
	 * content (MED-6), so this refusal is load-bearing.
	 *
	 * @return The new job, or empty if {@link #MAX_CONCURRENT_JOBS} concurrent jobs are already running.
	 */
	public Optional<AsyncJob> tryCreate() {
		sweepTimeouts();
		if (runningCount() >= MAX_CONCURRENT_JOBS)
			return Optional.empty();
		var job = new AsyncJob(mintId(), clock.instant(), timeout, maxOutputBytes, maxSubscribers);
		jobs.put(job.id(), job);
		scheduleTimeout(job);
		return Optional.of(job);
	}

	/**
	 * Creates and registers a new job, throwing when the concurrency cap is reached.
	 *
	 * @return The new job.
	 * @throws IllegalStateException If {@link #MAX_CONCURRENT_JOBS} concurrent jobs are already running.
	 */
	public AsyncJob create() {
		return tryCreate().orElseThrow(() -> new IllegalStateException(
			"Refusing to create an async job: the " + MAX_CONCURRENT_JOBS + "-concurrent-job cap is reached."));
	}

	/**
	 * Looks up a job by its capability id.
	 *
	 * <p>
	 * An unknown id returns empty &mdash; a miss is indistinguishable from any other, so the unguessable id space
	 * cannot be enumerated (HIGH-4).
	 *
	 * @param id The capability id.  Can be <jk>null</jk>/blank (returns empty).
	 * @return The job, or empty if no such id is registered.
	 */
	public Optional<AsyncJob> get(String id) {
		if (id == null || id.isBlank())
			return Optional.empty();
		return Optional.ofNullable(jobs.get(id));
	}

	/**
	 * Cancels the job with the given id (see {@link AsyncJob#cancel()}).
	 *
	 * @param id The capability id.  Can be <jk>null</jk>/blank.
	 * @return The (now terminal) job, or empty if no such id is registered.
	 */
	public Optional<AsyncJob> cancel(String id) {
		var job = get(id);
		job.ifPresent(AsyncJob::cancel);
		return job;
	}

	/** @return The number of concurrent (non-terminal) jobs currently registered. */
	public int runningCount() {
		var n = 0;
		for (var job : jobs.values())
			if (! job.isTerminal())
				n++;
		return n;
	}

	/** @return The total number of jobs currently registered (running plus retained-terminal). */
	public int size() { return jobs.size(); }

	/**
	 * Enforces the hard timeout on every job at the current clock instant and reaps terminal jobs past their
	 * retention window.  Called on every {@link #tryCreate()}, scheduled per-job by the private scheduler, and callable
	 * directly by deterministic tests (with a mutable clock) instead of waiting on wall time.
	 */
	public void sweepTimeouts() {
		var now = clock.instant();
		var reapBefore = now.minus(RETENTION);
		var it = jobs.values().iterator();
		while (it.hasNext()) {
			var job = it.next();
			job.enforceTimeout(now);
			if (job.isTerminal() && job.createdAt().isBefore(reapBefore))
				it.remove();
		}
	}

	private void scheduleTimeout(AsyncJob job) {
		if (scheduler == null)
			return;
		var delay = Duration.between(clock.instant(), job.deadline()).toMillis();
		try {
			scheduler.schedule(() -> job.enforceTimeout(clock.instant()), Math.max(0L, delay), TimeUnit.MILLISECONDS);
		} catch (RejectedExecutionException e) {
			// Scheduler shut down (registry closed); the sweep on the next tryCreate() still enforces the timeout.
		}
	}

	private String mintId() {
		var bytes = new byte[CAPABILITY_BYTES];
		random.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	/** Shuts down the private scheduler (if this registry created one).  Registered jobs are left as-is. */
	@Override /* AutoCloseable */
	public void close() {
		if (ownsScheduler && scheduler != null)
			scheduler.shutdownNow();
	}
}
