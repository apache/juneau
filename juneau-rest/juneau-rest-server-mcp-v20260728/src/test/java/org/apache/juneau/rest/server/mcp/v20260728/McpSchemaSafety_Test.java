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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpResponseResult;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpSchemaSafety}: no-fetch handling of external {@code $ref}s and bounded
 * (depth / node / CPU-time) schema validation (Resolution B2).
 */
class McpSchemaSafety_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpToolHandler tool(String name, McpSchema schema) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name).setInputSchema(schema); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text("ok"); }
		};
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		var p = params instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(p);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		m.put("Mcp-Method", method);
		m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r, Map<String,String> headers) {
		var result = new McpRevision(null).dispatch(new McpExchange(r, headers::get), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	private static Map<String,Object> nest(int depth) {
		Map<String,Object> node = new LinkedHashMap<>();
		for (var i = 1; i < depth; i++) {
			var parent = new LinkedHashMap<String,Object>();
			parent.put("child", node);
			node = parent;
		}
		return node;
	}

	private static Map<String,Object> flat(int nodeCount) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < nodeCount - 1; i++)  // root map (1) + (nodeCount-1) scalar entries == nodeCount nodes
			m.put("k" + i, i);
		return m;
	}

	// -------- no JsonSchemaMap / no external fetch ---------

	@Test
	void a01_noAdapterClassExtendsJsonSchemaMap() {
		for (var c : List.of(McpSchemaSafety.class, McpWire.class, McpRevision.class, McpRestServlet.class, McpEndpoint.class))
			assertFalse(JsonSchemaMap.class.isAssignableFrom(c), () -> c.getName() + " must not extend JsonSchemaMap");
	}

	@Test
	void a02_externalRef_serializesListsAndDispatchesWithoutFetch() {
		// An unresolvable host: if any code path tried to dereference this $ref, DNS resolution would throw.
		var schema = McpSchema.of(JsonMap.of("$ref", "https://never.invalid/schema.json"));
		var config = new McpServerConfig().addTool(tool("refTool", schema));

		// serialize: the $ref survives verbatim as opaque data.
		var wireTool = McpWire.toWire(new McpToolSpec().setName("refTool").setInputSchema(schema));
		assertContains("https://never.invalid/schema.json", Json.of(wireTool));

		// list: dispatch tools/list and confirm the $ref round-trips.
		var listed = (ListToolsResult) send(config, req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		assertContains("https://never.invalid/schema.json", Json.of(listed));

		// dispatch: tools/call runs input validation against the $ref schema and completes without a fetch.
		var params = JsonMap.of("name", "refTool", "arguments", JsonMap.of("anything", 1));
		var resp = send(config, req(2, "tools/call", params), hdrs("tools/call", "refTool"));
		assertNull(resp.getError());
		assertInstanceOf(CallToolResult.class, resp.getResult());
	}

	// -------- bounded depth ---------

	@Test
	void b01_depth64_passes() {
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), nest(McpSchemaSafety.MAX_DEPTH)));
	}

	@Test
	void b02_depth65_fails() {
		var schema = McpSchema.of(new JsonMap());
		var args = nest(McpSchemaSafety.MAX_DEPTH + 1);
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(schema, args));
		assertEquals(-32602, e.getCode());
		assertContains("nesting depth", e.getMessage());
	}

	// -------- bounded node count ---------

	@Test
	void c01_nodes10000_passes() {
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), flat(McpSchemaSafety.MAX_NODES)));
	}

	@Test
	void c02_nodes10001_fails() {
		var schema = McpSchema.of(new JsonMap());
		var args = flat(McpSchemaSafety.MAX_NODES + 1);
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(schema, args));
		assertEquals(-32602, e.getCode());
		assertContains("node count", e.getMessage());
	}

	// -------- bounded compute (thread CPU time, with wall-clock fallback) ---------

	@Test
	void d01_adversarialValidation_terminatesWithinDeadline() {
		// A catastrophically-backtracking pattern applied to a non-matching input hangs the validator.
		var schema = McpSchema.of(JsonMap.of(
			"type", "object",
			"properties", JsonMap.of("s", JsonMap.of("type", "string", "pattern", "^(.*a){25}$"))));
		var args = new LinkedHashMap<String,Object>();
		args.put("s", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!");  // 40 'a's then a non-matching char

		var start = System.nanoTime();
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(schema, args));
		var elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertEquals(-32602, e.getCode());
		assertContains("exceeded " + McpSchemaSafety.MAX_VALIDATION_MILLIS + " ms", e.getMessage());
		assertTrue(elapsedMs < McpSchemaSafety.MAX_VALIDATION_MILLIS + 5000, () -> "validation did not terminate promptly: elapsed=" + elapsedMs + "ms");
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep here simulates the deterministic scheduling latency under test, not a wait-and-hope synchronization delay.
	})
	@Test
	void d02_schedulingLatency_notCountedAgainstComputeBudget() {
		// Exercises McpSchemaSafety.awaitBounded() directly (rather than saturating the shared
		// VALIDATION_POOL) with a task-local executor that
		// deliberately delays counting down `started` well past MAX_VALIDATION_MILLIS before doing its
		// (instantaneous) "work". If scheduling latency were - the regression this guards against - counted
		// against the compute budget, awaitBounded() would throw a timeout error despite the task's own
		// work costing nothing; with the root-cause fix, only compute time (measured from the task's start
		// snapshot) counts.
		var schedulingDelayMillis = 3 * McpSchemaSafety.MAX_VALIDATION_MILLIS;
		assertTrue(schedulingDelayMillis < McpSchemaSafety.MAX_SCHEDULING_MILLIS, "test fixture assumption");

		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<McpSchemaSafety.TaskStart>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				Thread.sleep(schedulingDelayMillis);  // simulates thread-pool queueing/scheduling delay
				taskStart.set(McpSchemaSafety.TaskStart.capture());
				started.countDown();
				return null;  // the "validation" work itself is instantaneous
			});

			assertDoesNotThrow(() -> McpSchemaSafety.awaitBounded(
				future, started, taskStart, TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS)));
		} finally {
			executor.shutdownNow();
		}
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep here deterministically models a validation that elapses wall-clock without consuming CPU, not a wait-and-hope delay.
	})
	@Test
	void d03_sleepingValidation_doesNotTripCpuBudget() {
		// The core of the wall-clock->CPU-time fix: a "validation" that lets a lot of wall-clock elapse but
		// consumes ~no CPU (here, by sleeping *after* its start snapshot) must NOT trip the compute budget,
		// because only real CPU work is the DoS threat. Under the old wall-clock measurement this would have
		// tripped MAX_VALIDATION_MILLIS. Only meaningful when CPU timing is active (otherwise awaitBounded
		// legitimately falls back to wall-clock, under which a sleep does count).
		Assumptions.assumeTrue(McpSchemaSafety.cpuTimeBudgetEnabled(), "per-thread CPU timing unavailable on this JVM");
		var sleepMillis = 4 * McpSchemaSafety.MAX_VALIDATION_MILLIS;  // far past the budget in wall-clock terms
		assertTrue(sleepMillis < McpSchemaSafety.MAX_SCHEDULING_MILLIS, "test fixture assumption");

		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<McpSchemaSafety.TaskStart>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				taskStart.set(McpSchemaSafety.TaskStart.capture());
				started.countDown();
				Thread.sleep(sleepMillis);  // wall-clock elapses well past the budget, but burns ~no CPU
				return null;
			});

			assertDoesNotThrow(() -> McpSchemaSafety.awaitBounded(
				future, started, taskStart, TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS)));
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void d04_cpuBurningValidation_tripsBudget() {
		// The complement of d03: a "validation" that actually burns CPU past the budget MUST still trip it and
		// raise the same -32602 error. A generous CPU-burn margin (many multiples of the budget) keeps this
		// robust rather than racing a tight wall-clock bound.
		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<McpSchemaSafety.TaskStart>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				taskStart.set(McpSchemaSafety.TaskStart.capture());
				started.countDown();
				var sink = 0L;
				while (! Thread.currentThread().isInterrupted())  // spins until awaitBounded trips the budget and cancels us
					for (var i = 1; i < 5_000_000; i++)
						sink += (long) Math.sqrt(i) * i;
				return sink;  // returned only so the JIT can't elide the loop
			});

			var budgetNanos = TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS);
			var e = assertThrows(McpException.class, () -> McpSchemaSafety.awaitBounded(future, started, taskStart, budgetNanos));
			assertEquals(-32602, e.getCode());
			assertContains("exceeded " + McpSchemaSafety.MAX_VALIDATION_MILLIS + " ms", e.getMessage());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void d05_cpuTimeUnavailable_fallsBackToWallClock() {
		// When per-thread CPU timing is unavailable (cpuNanos == -1 in the start snapshot), awaitBounded must
		// fall back to the original wall-clock budget so the guard still functions on such JVMs. Simulated here
		// by handing awaitBounded a snapshot with cpuNanos == -1; the instantaneous task completes well within
		// the wall-clock window.
		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<McpSchemaSafety.TaskStart>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				taskStart.set(new McpSchemaSafety.TaskStart(Thread.currentThread().getId(), System.nanoTime(), -1L));
				started.countDown();
				return null;  // instantaneous work, well within the wall-clock window
			});

			assertDoesNotThrow(() -> McpSchemaSafety.awaitBounded(
				future, started, taskStart, TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS)));
		} finally {
			executor.shutdownNow();
		}
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep here lets the match descend into backtracking before the interrupt, not a wait-and-hope synchronization delay.
	})
	@Test
	void d06_interruptAbortsCatastrophicMatch() throws Exception {
		// Root-cause proof for the DoS-residual fix. A catastrophically-backtracking pattern applied to a
		// non-matching input runs effectively forever, and java.util.regex.Matcher ignores Thread.interrupt().
		// With JsonSchemaValidator now feeding the matched string through an interruptible CharSequence,
		// interrupting the matching thread aborts the match promptly; without the fix, worker.join(...) below
		// would time out with the thread still pinning a core.
		var validator = JsonSchemaValidator.of(JsonMap.of("type", "string", "pattern", "^(.*a){25}$"));
		var input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!";  // 40 'a's then a non-matching char

		var worker = new Thread(() -> {
			try {
				validator.validate(input);
			} catch (@SuppressWarnings("unused") RuntimeException ignored) {
				// aborted (interrupt) or a legitimate validation failure - either way the thread unwinds
			}
		}, "d06-interruptible-match");
		worker.setDaemon(true);
		worker.start();

		Thread.sleep(200);  // let the matcher get well into backtracking
		worker.interrupt();
		worker.join(5000);

		assertFalse(worker.isAlive(), "interrupt did not abort the catastrophically-backtracking match");
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep here samples the pool threads' CPU over a fixed window; it is the measurement window, not a wait-and-hope delay.
	})
	@Test
	void d07_poolWorkerStopsBurningAfterBudgetTrip() throws Exception {
		// End-to-end proof through the real DoS guard: after validateInput trips the compute budget and cancels
		// the worker, the pool thread must stop burning CPU rather than keep spinning on the runaway regex. We
		// sample the validation-pool threads' aggregate CPU time across a window after the trip; a still-running
		// match would accrue ~a full core of CPU over that window, while the fix drives it to ~0. Gated on
		// per-thread CPU timing (as d03) since the measurement relies on it; the margin is generous to stay robust.
		Assumptions.assumeTrue(McpSchemaSafety.cpuTimeBudgetEnabled(), "per-thread CPU timing unavailable on this JVM");

		var schema = McpSchema.of(JsonMap.of(
			"type", "object",
			"properties", JsonMap.of("s", JsonMap.of("type", "string", "pattern", "^(.*a){25}$"))));
		var args = new LinkedHashMap<String,Object>();
		args.put("s", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!");

		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(schema, args));
		assertEquals(-32602, e.getCode());

		var threadMx = ManagementFactory.getThreadMXBean();
		var before = poolCpuNanos(threadMx);
		Thread.sleep(500);  // measurement window
		var after = poolCpuNanos(threadMx);
		var burnedMs = (after - before) / 1_000_000;

		assertTrue(burnedMs < 200, () -> "validation-pool worker kept burning CPU after budget trip: " + burnedMs + "ms");
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep deterministically exhausts the simulated pre-check deadline; burnCpuFor's timer-bounded loop models a bounded compute cost. Neither is a wait-and-hope delay.
	})
	@Test
	void d08_staleSharedDeadlineBudget_falselyTripsTrivialValidation_freshBudgetDoesNot() {
		// Regression test for the flaky Characterization_Test false-positive (McpSchemaSafety.validateInput ->
		// validateBounded): the OLD code derived the validation task's compute budget from
		// JsonValueSafety.remainingNanos(sharedDeadline) - the caller's remaining share of the SAME wall-clock
		// deadline already partially spent by the two structural JsonValueSafety.check() calls and the
		// schema-to-bean conversion. Under heavy concurrent load those pre-phases can consume the entire 100ms
		// deadline, collapsing "remaining" to ~0 - at which point even bounded, genuinely-trivial validation
		// work trips -32602 purely from external scheduling pressure, not from anything about the schema or
		// arguments being validated. The fix (validateBounded) instead hands the task a FRESH
		// MAX_VALIDATION_MILLIS budget, measured from when the task itself starts running, so the identical
		// pre-delay has zero effect.
		//
		// Reproduces this deterministically (no dependence on machine load) via the same JsonValueSafety
		// deadline/remaining arithmetic the OLD code used, feeding the result into the still-live
		// awaitBounded/TaskStart seam: the SAME bounded ~30ms "validation" work is run twice - once under the
		// literal OLD-style stale/collapsed budget (a real deadline slept past, then remainingNanos()'d down to
		// 0) and once under the CURRENT fresh budget - proving the outcome now depends only on the fresh
		// budget, not on how much wall-clock some unrelated earlier phase had already spent.
		var boundedWorkMillis = 30;
		assertTrue(boundedWorkMillis < McpSchemaSafety.MAX_SCHEDULING_MILLIS, "test fixture assumption");

		// Simulates the pre-check phase (structural checks + schema conversion) fully consuming the shared
		// deadline under load - the exact wall-clock arithmetic the OLD validateBounded fed into awaitBounded.
		var simulatedPreCheckDeadline = JsonValueSafety.deadlineNanos();
		sleepPastDeadline(simulatedPreCheckDeadline);
		var staleRemaining = JsonValueSafety.remainingNanos(simulatedPreCheckDeadline);
		assertEquals(0, staleRemaining, "test fixture assumption: simulated pre-check delay must fully exhaust the shared deadline");

		// (a) OLD behavior: a budget derived from an already-exhausted shared deadline (remaining == 0) trips
		// the guard even though the validation work itself is well within the real MAX_VALIDATION_MILLIS budget.
		var e = assertThrows(McpException.class, () -> runBoundedTask(boundedWorkMillis, staleRemaining));
		assertEquals(-32602, e.getCode());
		assertContains("exceeded " + McpSchemaSafety.MAX_VALIDATION_MILLIS + " ms", e.getMessage());

		// (b) FIX: the identical bounded work succeeds under a fresh MAX_VALIDATION_MILLIS budget - exactly as
		// validateBounded now computes it - unaffected by how much wall-clock a caller's OTHER deadline had
		// already spent.
		assertDoesNotThrow(() -> runBoundedTask(boundedWorkMillis,
			TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS)));
	}

	/**
	 * Blocks until {@code deadlineNanos} ({@link System#nanoTime()} units) has passed, re-checking the
	 * remaining time on each iteration rather than sleeping for a single fixed guess.
	 *
	 * <p>
	 * Uses {@link LockSupport#parkNanos} rather than {@code Thread.sleep} - both may return before the
	 * requested duration elapses, but only the loop (not a caught {@code InterruptedException}) is needed to
	 * guard against that here, since {@code parkNanos} declares no checked exception.
	 */
	private static void sleepPastDeadline(long deadlineNanos) {
		while (System.nanoTime() < deadlineNanos) {
			LockSupport.parkNanos(deadlineNanos - System.nanoTime());
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				throw new AssertionError(new InterruptedException());
			}
		}
	}

	/** Submits a task that burns CPU for {@code workMillis} and awaits it under {@code budgetNanos} via the real {@code awaitBounded} seam. */
	private static void runBoundedTask(long workMillis, long budgetNanos) {
		var started = new CountDownLatch(1);
		var taskStart = new AtomicReference<McpSchemaSafety.TaskStart>();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				taskStart.set(McpSchemaSafety.TaskStart.capture());
				started.countDown();
				burnCpuFor(workMillis);
				return null;
			});
			McpSchemaSafety.awaitBounded(future, started, taskStart, budgetNanos);
		} finally {
			executor.shutdownNow();
		}
	}

	/** Busy-spins for (approximately) {@code millis}, so the calling thread actually consumes CPU rather than sleeping. */
	private static void burnCpuFor(long millis) {
		var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
		var sink = 0L;
		while (System.nanoTime() < deadline)
			for (var i = 1; i < 100_000; i++)
				sink += (long) Math.sqrt(i) * i;
		if (sink == Long.MIN_VALUE)  // never true; prevents the JIT from eliding the loop
			throw new AssertionError();
	}

	private static long poolCpuNanos(ThreadMXBean threadMx) {
		var total = 0L;
		for (var id : threadMx.getAllThreadIds()) {
			var info = threadMx.getThreadInfo(id);
			if (info != null && info.getThreadName().startsWith("mcp-2026-07-28-schema-validation")) {
				var cpu = threadMx.getThreadCpuTime(id);
				if (cpu > 0)
					total += cpu;
			}
		}
		return total;
	}

	// -------- shared JsonValueSafety delegation now supports arrays ---------

	@Test
	void f01_argumentContainingList_passesUnderPermissiveSchema() {
		var args = JsonMap.of("arr", JsonList.of(1, 2, 3));
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), args));
	}

	@Test
	void f02_argumentContainingPrimitiveArray_isWalkedWithoutError() {
		// The old local checkBounds() only recursed into Map/Collection, never arrays; delegating to
		// JsonValueSafety (which also walks java.lang.reflect.Array elements) must not regress this.
		var args = new LinkedHashMap<String,Object>();
		args.put("arr", new int[]{1, 2, 3});
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), args));
	}

	// -------- v1 finiteness anchor is untouched ---------

	@Test
	void e01_v1FinitenessTest_hasZeroDiff() throws Exception {
		var relPath = "juneau-bean/juneau-bean-mcp-v20250618/src/test/java/org/apache/juneau/bean/mcp/v20250618/JsonSchema_Finiteness_Test.java";
		var root = repoRoot();
		Assumptions.assumeTrue(root != null, "git repository root not resolvable");
		var diff = new ProcessBuilder("git", "diff", "--exit-code", "--", relPath);
		diff.directory(root);
		diff.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		diff.redirectError(ProcessBuilder.Redirect.DISCARD);
		assertEquals(0, diff.start().waitFor(), "v1 JsonSchema_Finiteness_Test.java must have zero diff");
	}

	private static File repoRoot() {
		try {
			var pb = new ProcessBuilder("git", "rev-parse", "--show-toplevel");
			pb.directory(new File(System.getProperty("user.dir")));
			var p = pb.start();
			String out;
			try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				out = r.readLine();
			}
			return p.waitFor() == 0 && out != null ? new File(out.trim()) : null;
		} catch (IOException | InterruptedException e) {
			return null;
		}
	}
}
