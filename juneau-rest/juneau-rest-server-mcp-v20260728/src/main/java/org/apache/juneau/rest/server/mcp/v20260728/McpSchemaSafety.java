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

import static java.util.concurrent.TimeUnit.*;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.commons.utils.JsonValueSafety;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpSchema;

/**
 * Bounded, no-fetch validation of {@code tools/call} arguments against a tool's declared input schema
 * (Resolution B2).
 *
 * <p>
 * The neutral {@link McpSchema} is an unconstrained JSON object carrier, and a client can send an
 * arbitrarily large or deep argument object. Before any validation runs, both the schema graph and the
 * argument graph are checked against the shared {@link JsonValueSafety} depth/node/deadline bounds,
 * so a hostile or accidentally-huge input is rejected up front rather than blowing the stack or spinning
 * indefinitely. The two checks share a single deadline, so a slow schema graph cannot buy the argument
 * graph extra time (or vice versa).
 *
 * <p>
 * <b>No external fetches.</b> The schema is converted to a {@link JsonSchema} bean and validated as-is. No
 * schema-resolution map is ever installed on the schema, so an external {@code $ref} is inert data:
 * {@link JsonSchemaValidator} does not resolve references, and with no resolution map installed there is no
 * code path that opens a network connection or a file to dereference one.
 *
 * <p>
 * Validation itself runs on a fixed-size pool of daemon threads and is bounded by its own independent compute
 * budget ({@link #MAX_VALIDATION_MILLIS}), measured fresh from when the validation task actually starts running
 * &mdash; <b>not</b> from the structural-traversal deadline above, so wall-clock time spent in the structural
 * pre-checks or the schema-bean conversion never shrinks it (see {@link #validateBounded} for why). If a
 * pathological schema (for example a catastrophically-backtracking {@code pattern}) fails to complete in time,
 * the task is cancelled and a {@code -32602} error is raised instead of hanging the request thread.
 *
 * <p>
 * <b>Cancellation actually reclaims the worker thread.</b> When the budget trips, {@link #awaitBounded} calls
 * {@link Future#cancel(boolean) future.cancel(true)}, which interrupts the validating thread. A
 * {@link java.util.regex.Matcher} does not observe {@link Thread#interrupt()} on its own, so historically a runaway
 * regex kept burning a core in the background even though the client already had its fast {@code -32602} - a mild DoS
 * residual where a flood of such requests could pin a core each. {@link JsonSchemaValidator} now feeds the matched
 * string through an interruptible {@link CharSequence} (see its class notes), so an interrupt aborts a
 * catastrophically-backtracking match promptly and the pool thread returns to idle rather than spinning. The external
 * contract is unchanged: a genuine overrun still returns the same {@code -32602}, and the CPU-time budget with its
 * wall-clock fallback is untouched.
 *
 * <p>
 * <b>The compute budget is charged against the validating thread's actual CPU time, not wall-clock time.</b>
 * The DoS threat being defended against is a schema that burns CPU (catastrophic backtracking, quadratic
 * blowups); the amount of CPU a validation consumes is exactly what that budget should cap. Measuring
 * wall-clock instead would wrongly count time the validating thread was <i>not</i> running - time it spent
 * preempted by the OS scheduler, or queued behind sibling work - as if it were validation cost. Under heavy
 * concurrent load (for example {@code juneau-integration-tests} running Surefire with {@code forkCount=8}) that
 * false accounting can trip the budget on trivial input, and the same false-positive can bite a saturated
 * production deployment. Sampling {@link ThreadMXBean#getThreadCpuTime(long) thread CPU time} on the thread
 * that actually does the work means only real compute counts: preemption and scheduling latency no longer
 * shrink the budget, while a genuinely expensive validation still trips it and returns the same {@code -32602}
 * error. On a JVM where per-thread CPU timing is unavailable, {@link #awaitBounded} transparently falls back to
 * the original wall-clock measurement so the guard still functions everywhere.
 *
 * <p>
 * Independently of the compute budget, time spent waiting for a free thread in {@link #VALIDATION_POOL} is
 * scheduling latency, not validation cost, so it is never charged against {@link #MAX_VALIDATION_MILLIS}: the
 * budget clock (CPU or wall-clock) only starts once the task is actually running. A separate, much more
 * generous {@link #MAX_SCHEDULING_MILLIS} backstop bounds the scheduling wait itself, purely so a wedged or
 * saturated pool cannot block the caller forever.
 */
final class McpSchemaSafety {

	/** Maximum nesting depth permitted in either the schema graph or the argument graph. */
	static final int MAX_DEPTH = 64;

	/** Maximum number of nodes permitted in either the schema graph or the argument graph. */
	static final int MAX_NODES = 10_000;

	/**
	 * Maximum compute time permitted for a single schema validation, in milliseconds, measured as the
	 * validating thread's actual CPU time (see class-level notes) - not from submission, and not as wall-clock.
	 * This is the DoS bound: it caps how much CPU a pathological schema can burn, and is deliberately
	 * unaffected both by how long the task had to wait for a free thread and by any OS preemption while it runs.
	 */
	static final long MAX_VALIDATION_MILLIS = 100;

	/**
	 * Maximum time permitted for a submitted validation task to begin executing on {@link #VALIDATION_POOL},
	 * in milliseconds. This is a generous circuit breaker against a wedged or fully-saturated pool - not part
	 * of the {@link #MAX_VALIDATION_MILLIS} DoS budget - so ordinary scheduling delay (queueing behind other
	 * validations under heavy concurrent load) never gets mistaken for an expensive schema.
	 */
	static final long MAX_SCHEDULING_MILLIS = 2_000;

	private static final ThreadMXBean THREAD_MX = ManagementFactory.getThreadMXBean();

	/**
	 * Whether per-thread CPU-time measurement is available (and enabled) on this JVM. When <jk>true</jk>, the
	 * compute budget is charged against the validating thread's actual CPU time; when <jk>false</jk> (a JVM that
	 * cannot report per-thread CPU time), {@link #awaitBounded} falls back to the original wall-clock measurement
	 * so the DoS guard still functions everywhere.
	 */
	private static final boolean CPU_TIME_SUPPORTED = initCpuTimeSupport();

	/**
	 * How often the waiting thread re-samples the validating thread's accumulated CPU time while a validation is
	 * still running. Small enough that a runaway validation is stopped promptly once it burns past the budget,
	 * large enough that the sampling overhead is negligible.
	 */
	private static final long CPU_POLL_INTERVAL_NANOS = MILLISECONDS.toNanos(5);

	private static boolean initCpuTimeSupport() {
		if (! THREAD_MX.isThreadCpuTimeSupported())
			return false;  // HTT: CI JVMs support per-thread CPU time; this fallback path isn't reachable there.
		if (! THREAD_MX.isThreadCpuTimeEnabled()) {
			try {
				THREAD_MX.setThreadCpuTimeEnabled(true);  // HTT: HotSpot enables thread CPU time by default; not reachable in CI.
			} catch (@SuppressWarnings("unused") SecurityException | UnsupportedOperationException e) {
				return false;  // HTT: enabling is rejected only under a restrictive SecurityManager; not reachable in CI.
			}
		}
		return true;
	}

	private static final ExecutorService VALIDATION_POOL = newValidationPool();

	private static ExecutorService newValidationPool() {
		var pool = new ThreadPoolExecutor(
			Math.max(2, Runtime.getRuntime().availableProcessors()),
			Math.max(2, Runtime.getRuntime().availableProcessors()),
			0, MILLISECONDS,
			new LinkedBlockingQueue<>(),
			r -> {
				var t = new Thread(r, "mcp-2026-07-28-schema-validation");
				t.setDaemon(true);
				return t;
			});
		pool.prestartAllCoreThreads(); // Warm the pool so the first validation after startup doesn't pay thread-creation cost.
		return pool;
	}

	private McpSchemaSafety() {}

	/**
	 * Validates a {@code tools/call} argument object against a tool's declared input schema, subject to the
	 * B2 bounds.
	 *
	 * @param schema The tool's neutral input schema, or <jk>null</jk> to skip validation entirely.
	 * @param args The argument object to validate. Never <jk>null</jk> (an absent argument object is an empty map).
	 * @throws McpException {@code -32602} if either graph exceeds {@link #MAX_DEPTH}/{@link #MAX_NODES}, if the
	 * 	arguments do not satisfy the schema, or if validation exceeds {@link #MAX_VALIDATION_MILLIS}.
	 */
	static void validateInput(McpSchema schema, Map<String,Object> args) {
		if (schema == null)
			return;
		var schemaMap = schema.toJsonMap();
		var deadline = JsonValueSafety.deadlineNanos();
		try {
			JsonValueSafety.check(schemaMap, "Tool input schema", deadline);
			JsonValueSafety.check(args, "Tool input", deadline);
		} catch (IllegalArgumentException e) {
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, e.getMessage());
		}
		var jsonSchema = Json.to(Json.of(schemaMap), JsonSchema.class);
		validateBounded(jsonSchema, args);
	}

	/**
	 * Runs {@link JsonSchemaValidator} on a daemon thread and enforces a fresh {@link #MAX_VALIDATION_MILLIS}
	 * compute budget against the task's own compute time - deliberately <b>not</b> the caller's remaining share
	 * of the structural-traversal deadline computed in {@link #validateInput}. That deadline is a wall-clock
	 * budget that is already partially spent by the two {@link JsonValueSafety#check} calls and the
	 * schema-to-bean conversion above, both of which run on the (possibly preempted) request thread under load;
	 * deriving the validation budget from whatever wall-clock happened to remain would let purely-external
	 * scheduling pressure - not any property of the schema or arguments being validated - trip a false-positive
	 * {@code -32602} on a trivial validation. Anchoring a fresh budget here, measured from when the pool task
	 * actually starts (see {@link TaskStart#capture}), keeps the DoS guard (validation CPU still capped at
	 * {@link #MAX_VALIDATION_MILLIS}) independent of that pre-check wall-clock entirely. Also never against
	 * however long the task had to wait in {@link #VALIDATION_POOL} for a free thread, and (when CPU timing is
	 * available) never against wall-clock time it spent preempted rather than running.
	 */
	private static void validateBounded(JsonSchema<?> schema, Object value) {
		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<TaskStart>();
		var future = VALIDATION_POOL.submit(() -> {
			taskStart.set(TaskStart.capture());
			started.countDown();
			JsonSchemaValidator.of(schema).validate(value);
			return null;
		});
		awaitBounded(future, started, taskStart, MILLISECONDS.toNanos(MAX_VALIDATION_MILLIS));
	}

	/**
	 * Snapshot of when a validation task actually began running, captured on the validating thread itself as its
	 * first instruction.
	 *
	 * @param threadId The validating thread's id, so the waiting thread can sample that same thread's CPU time.
	 * @param wallNanos The {@link System#nanoTime()} baseline, used by the wall-clock fallback.
	 * @param cpuNanos The validating thread's CPU-time baseline in nanoseconds ({@link ThreadMXBean#getThreadCpuTime(long)}),
	 * 	or {@code -1} if per-thread CPU timing is unavailable on this JVM.
	 */
	record TaskStart(long threadId, long wallNanos, long cpuNanos) {

		/** Captures the current thread's start snapshot; must be called on the thread that runs the validation. */
		static TaskStart capture() {
			var id = Thread.currentThread().getId();
			return new TaskStart(id, System.nanoTime(), CPU_TIME_SUPPORTED ? THREAD_MX.getThreadCpuTime(id) : -1L);
		}
	}

	/**
	 * Waits for a submitted validation task to complete, charging only its own compute time against
	 * {@code remaining} (the caller's compute budget, a fresh interval unrelated to any other deadline the
	 * caller may separately be tracking) - never however long it had to wait in {@link #VALIDATION_POOL} for a
	 * free thread.
	 *
	 * <p>
	 * This waits in two phases. First, {@code started} is awaited so the calling thread learns exactly when the
	 * task begins running; this wait is bounded only by the generous {@link #MAX_SCHEDULING_MILLIS} backstop,
	 * since scheduling delay under load (queueing behind other work in {@link #VALIDATION_POOL}) is not the thing
	 * being defended against. Second, once the task is running, its compute is bounded: when per-thread CPU
	 * timing is available (the normal case) the waiting thread polls the validating thread's accumulated
	 * <i>CPU</i> time and trips only if that exceeds {@code remaining}, so preemption and scheduling latency
	 * never shrink the budget - only real CPU work does. When CPU timing is unavailable, it falls back to the
	 * original wall-clock window anchored to the task's actual start time.
	 *
	 * <p>
	 * Package-private (rather than private) purely so this can be exercised directly against a test-local
	 * {@link Future}/latch pair - deterministically simulating scheduling delay, a CPU burn, or a sleep that
	 * elapses wall-clock without consuming CPU - without needing to saturate the shared {@link #VALIDATION_POOL}.
	 *
	 * @param future The in-flight (or already-complete) validation task.
	 * @param started Counted down by the task as its first instruction, once it actually begins running.
	 * @param taskStart Set by the task (before counting down {@code started}) to its {@link TaskStart} snapshot.
	 * @param remaining The caller's compute budget, in nanoseconds, captured fresh before submission (see
	 * 	{@link #validateBounded} for why this must be independent of any pre-existing wall-clock deadline).
	 */
	static void awaitBounded(Future<?> future, CountDownLatch started, AtomicReference<TaskStart> taskStart, long remaining) {
		try {
			if (! started.await(MAX_SCHEDULING_MILLIS, MILLISECONDS)) {
				future.cancel(true);
				throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation could not be scheduled within " + MAX_SCHEDULING_MILLIS + " ms");
			}
			var start = taskStart.get();
			if (CPU_TIME_SUPPORTED && start.cpuNanos() >= 0)
				awaitByCpuTime(future, start.threadId(), start.cpuNanos(), remaining);
			else
				future.get(Math.max(0, remaining - (System.nanoTime() - start.wallNanos())), NANOSECONDS);  // HTT: wall-clock fallback; CI JVMs use the CPU-time path.
		} catch (TimeoutException e) {
			future.cancel(true);
			throw validationTimeoutException();
		} catch (ExecutionException e) {
			var cause = e.getCause();
			if (cause instanceof McpException me)
				throw me;
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, cause == null ? e.getMessage() : cause.getMessage());
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation was interrupted");
		}
	}

	/**
	 * Bounds a running validation by the validating thread's actual CPU time.
	 *
	 * <p>
	 * The waiting thread polls at {@link #CPU_POLL_INTERVAL_NANOS} intervals: each time the task hasn't finished
	 * yet, it re-samples thread CPU time and keeps waiting as long as the CPU consumed since {@code baseCpuNanos}
	 * is within {@code budgetNanos}. Wall-clock elapsed while the thread was preempted (or deliberately sleeping)
	 * accrues no CPU, so it never trips the budget; a schema that genuinely burns CPU does. Validation is pure
	 * in-memory work with no I/O or blocking, so CPU accrual is a sufficient bound and no wall-clock ceiling is
	 * needed here - a task that never returns would necessarily be burning CPU and will trip the budget.
	 *
	 * @param future The running validation task.
	 * @param threadId The validating thread's id.
	 * @param baseCpuNanos The validating thread's CPU-time baseline captured at task start.
	 * @param budgetNanos The maximum CPU time, in nanoseconds, the validation may consume.
	 * @throws TimeoutException If the CPU budget is exceeded while the task is still running.
	 */
	private static void awaitByCpuTime(Future<?> future, long threadId, long baseCpuNanos, long budgetNanos)
			throws InterruptedException, ExecutionException, TimeoutException {
		while (true) {
			try {
				future.get(CPU_POLL_INTERVAL_NANOS, NANOSECONDS);
				return;
			} catch (TimeoutException poll) {
				var cpuNow = THREAD_MX.getThreadCpuTime(threadId);
				if (cpuNow >= 0 && cpuNow - baseCpuNanos <= budgetNanos)
					continue;  // real CPU work still within budget; preemption/scheduling doesn't count against it
				if (future.isDone())
					continue;  // finished inside the sampling race window; the next get() harvests its result/exception
				throw poll;  // CPU work exceeded the DoS budget (or CPU timing was lost) while the task is still running
			}
		}
	}

	/** Whether the compute budget is charged against thread CPU time (vs. the wall-clock fallback); for tests. */
	static boolean cpuTimeBudgetEnabled() {
		return CPU_TIME_SUPPORTED;
	}

	private static McpException validationTimeoutException() {
		return new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation exceeded " + MAX_VALIDATION_MILLIS + " ms");
	}
}
