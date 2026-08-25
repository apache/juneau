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
package org.apache.juneau.rest.server.processor;

import static org.apache.juneau.rest.server.logging.RestDebugDumpGateTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.logging.LogContext;
import org.apache.juneau.commons.logging.LogRecordContext;
import org.apache.juneau.commons.logging.RichLogger;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity coverage for {@link AsyncResponseProcessor}.
 *
 * <p>
 * The {@code MockServletRequest} test harness reports {@code isAsyncSupported() == false}, so the
 * sibling unit-test class {@code AsyncResponseProcessor_Test} only exercises the synchronous-fallback
 * path. This class boots a real Jetty microservice on an ephemeral port to drive the async-dispatch
 * branch ({@code req.startAsync()}, the {@code AsyncListener} callbacks, the {@code finalizeAsync}
 * pipeline, and the {@code unwrap} static helper) so that the {@code AsyncContext}-driven path is
 * actually exercised at coverage time.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class AsyncResponseProcessor_JettyMicroservice_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Resource — exercises success, failed-future, timeout, completion-stage, and bare-Future rejection on the
	// real async-dispatch path.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/async/*", asyncTimeoutMillis="200")
	public static class AsyncTestServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ok")
		public CompletableFuture<String> ok() {
			return CompletableFuture.supplyAsync(() -> "async-ok");
		}

		@RestGet("/okStage")
		public CompletionStage<String> okStage() {
			return CompletableFuture.supplyAsync(() -> "async-stage-ok");
		}

		@RestGet("/failed")
		public CompletableFuture<String> failed() {
			return CompletableFuture.supplyAsync(() -> {
				throw new IllegalStateException("kaboom-async");
			});
		}

		@RestGet("/failedDirect")
		public CompletableFuture<String> failedDirect() {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new IllegalStateException("direct-failure"));
			return f;
		}

		@RestGet("/timeout")
		public CompletableFuture<String> timeout() {
			return new CompletableFuture<>();  // Never completes — fires the AsyncContext timeout.
		}

		@RestGet("/bareFuture")
		public Future<String> bareFuture() {
			// FutureTask is not a CompletionStage — must be rejected with a 500.
			return new FutureTask<>(() -> "should-not-block");
		}

		@RestGet("/sync")
		public String sync() {
			return "still-sync";
		}

		@RestGet("/failedExecutionException")
		public CompletableFuture<String> failedExecutionException() {
			// Complete with an ExecutionException directly (not the usual CompletionException)
			// so that unwrap(...) hits its ExecutionException-stripping branch.
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new ExecutionException("wrapped", new IllegalStateException("inner-cause")));
			return f;
		}
	}

	@Configuration
	public static class Config {
		@Bean(name="asyncTestServlet")
		public Servlet asyncTestServlet() { return new AsyncTestServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(Config.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: 200 responses — happy-path async (CompletableFuture, CompletionStage) and the synchronous pass-through
	// (backward-compat smoke test that non-future content is returned unchanged on the real container).
	// -----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@CsvSource({
		"/async/ok,      async-ok",        // CompletableFuture unwrapped
		"/async/okStage, async-stage-ok",  // CompletionStage unwrapped
		"/async/sync,    still-sync"        // synchronous handler passed through unchanged
	})
	void a01_successResponses_return200WithBody(String path, String expected) throws Exception {
		var resp = get(path);
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains(expected), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Failed-future paths — exercise the unwrap(...) static helper for CompletionException-wrapped causes
	// (supplyAsync wraps the thrown exception in CompletionException) AND the direct-completeExceptionally case.
	// -----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		// supplyAsync → exception thrown in supplier wrapped in CompletionException — unwrap strips that layer.
		"/async/failed",
		// completeExceptionally with a plain throwable — unwrap's "no CompletionException wrapping" fall-through.
		"/async/failedDirect",
		// completeExceptionally(ExecutionException) — unwrap strips the ExecutionException wrapper (line 297-298).
		"/async/failedExecutionException",
		// Bare Future that is not a CompletionStage — rejected outright (category D).
		"/async/bareFuture"
	})
	void b01_errorResponses_return500(String path) throws Exception {
		var resp = get(path);
		assertEquals(500, resp.statusCode(), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Async timeout — the AsyncContext fires onTimeout() after the configured 200ms ceiling.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_neverCompleting_returns504() throws Exception {
		var before = System.currentTimeMillis();
		var resp = get("/async/timeout");
		var elapsed = System.currentTimeMillis() - before;
		assertEquals(504, resp.statusCode(), "body: " + resp.body());
		assertTrue(elapsed < 5_000, "200ms timeout should fire well before the 5s client timeout — actual " + elapsed + "ms");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Resource G — async-completion executor configured exercises the whenCompleteAsync branch
	// (line 237 true, line 238 invocation).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/asyncExec/*", asyncTimeoutMillis="200", asyncCompletionExecutor="execPool")
	public static class AsyncExecServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		// The bean is declared on the resource class itself so the resource's bean store
		// resolves the executor name at startup.
		@Bean(name="execPool")
		public Executor execPool() {
			return Executors.newSingleThreadExecutor(r -> {
				var t = new Thread(r, "async-exec-pool");
				t.setDaemon(true);
				return t;
			});
		}

		@RestGet("/value")
		public CompletableFuture<String> value() {
			return CompletableFuture.supplyAsync(() -> "exec-pool-async");
		}
	}

	@Configuration
	public static class ConfigExec {
		@Bean(name="asyncExecServlet")
		public Servlet asyncExecServlet() { return new AsyncExecServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture execFixture = MicroserviceTestFixture.create()
		.configurations(ConfigExec.class);

	@Test void g01_completionExecutor_routesCallback() throws Exception {
		// asyncCompletionExecutor configured — exercises whenCompleteAsync(callback, executor)
		// path (line 237 true / line 238).
		var port = execFixture.getPort();
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/asyncExec/value"))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofString());
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("exec-pool-async"), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: Completion-path emission — proves async debug records are emitted on the response-completion path (after the
	// late headers/body exist), NOT during synchronous finish().  These are the completion-path redesign gates: the
	// delayed test's post-finish/pre-completion zero-capture assertion is RED on a synchronous-finish emit and GREEN
	// only once emission moves to completion.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/asyncCap/*", asyncTimeoutMillis="1000")
	public static class AsyncCaptureServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		/** Test-controlled gate future for the delayed endpoint; completed by the test to trigger completion-path emit. */
		static volatile CompletableFuture<String> gate;

		/** Test-controlled gate future for the ad-hoc-log endpoint (async-carry Phase 6 tripwire). */
		static volatile CompletableFuture<String> adhocGate;

		/** Counted down by {@code @RestEndCall} on the request thread — i.e. AFTER {@code RestSession.finish()} returns. */
		static volatile CountDownLatch finishLatch;

		/**
		 * When non-<jk>null</jk>, the async handlers open a {@link LogContext} scope carrying {@code corrId=carryValue} on
		 * the request thread. Async-carry v1 leaves request-id/{@code LogContext}-scope ownership to a future
		 * {@code RequestIdFilter}, so these Phase-6 async-carry tests establish their own request-thread scope instead of
		 * relying on any framework-opened scope. The scope stays open across the async hand-off (so
		 * {@code RestDebugPipeline.captureAsyncSnapshot(...)} snapshots it) and is closed in {@link #endCall()} on the same
		 * request thread — no thread-pool leak.
		 */
		static volatile String carryValue;

		/** The request-thread {@link LogContext} scope opened from {@link #carryValue}; closed in {@link #endCall()}. */
		static volatile LogContext.Scope carryScope;

		private void openCarryScopeIfRequested() {
			var cv = carryValue;
			if (cv != null)
				carryScope = RichLogger.context().with("corrId", cv);
		}

		@RestGet("/delayed")
		public CompletableFuture<String> delayed(RestResponse res) {
			openCarryScopeIfRequested();
			var g = new CompletableFuture<String>();
			gate = g;
			// The header + content-type are set on the COMPLETION thread (inside thenApply), so a record that captured
			// them proves rendering happened after async completion, not during synchronous finish().
			return g.thenApply(body -> {
				res.setContentType("text/plain");
				res.setHeader("X-Late-Header", "late-header-value");
				return body;
			});
		}

		@RestGet("/completed")
		public CompletableFuture<String> completed(RestResponse res) {
			res.setContentType("text/plain");
			return CompletableFuture.completedFuture("completed-body-value");  // already complete → inline callback
		}

		@RestGet("/finestError")
		public CompletableFuture<String> finestError() {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new IllegalStateException("finest-error-cause"));
			return f;
		}

		@RestGet("/neverCompletes")
		public CompletableFuture<String> neverCompletes() {
			return new CompletableFuture<>();  // never completes → fires the AsyncContext timeout
		}

		@RestGet("/adhoc")
		public CompletableFuture<String> adhoc(RestRequest req, RestResponse res) {
			openCarryScopeIfRequested();
			var g = new CompletableFuture<String>();
			adhocGate = g;
			// The ad-hoc app log runs inside the completion callback (the COMPLETION thread), where the request-thread
			// LogContext scope has already closed — documented Phase 6 async-carry v1 edge.
			return g.thenApply(body -> {
				res.setContentType("text/plain");
				req.fine("adhoc-completion-log");
				return body;
			});
		}

		@RestEndCall
		public void endCall() {
			// Close the request-thread carry scope here — on the request thread, AFTER captureAsyncSnapshot(...) has
			// already snapshotted it during response processing — so the thread-local is restored and never leaks into a
			// pooled Jetty worker.
			var sc = carryScope;
			if (sc != null) {
				sc.close();
				carryScope = null;
			}
			var l = finishLatch;
			if (l != null)
				l.countDown();
		}
	}

	@Configuration
	public static class ConfigCap {
		@Bean(name="asyncCaptureServlet")
		public Servlet asyncCaptureServlet() { return new AsyncCaptureServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture capFixture = MicroserviceTestFixture.create()
		.configurations(ConfigCap.class);

	/** JUL handler that records only the events for a single op logger (attached with {@code useParentHandlers=false}). */
	private static final class CollectingHandler extends Handler {
		private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

		@Override public void publish(LogRecord rec) {
			// LogRecord's own "record" identifier is a restricted identifier since records were added; use "rec" instead.
			if (isLoggable(rec))
				records.add(rec);
		}

		@Override public void flush() {
			// No buffering to flush — records are added directly to the in-memory list.
		}
		@Override public void close() {
			// Nothing to release — no external resources are held by this in-memory handler.
		}

		List<LogRecord> forLogger(String name) {
			synchronized (records) {
				return records.stream().filter(x -> name.equals(x.getLoggerName())).toList();
			}
		}
	}

	/** Snapshot/restore of a JUL logger's mutable level/handler state so tests never leak elevated levels. */
	private static final class LoggerState {
		private final Logger logger;
		private final Level level;
		private final boolean useParentHandlers;
		private final Handler[] handlers;

		LoggerState(Logger logger) {
			this.logger = logger;
			level = logger.getLevel();
			useParentHandlers = logger.getUseParentHandlers();
			handlers = logger.getHandlers();
		}

		void restore() {
			for (var h : logger.getHandlers())
				logger.removeHandler(h);
			for (var h : handlers)
				logger.addHandler(h);
			logger.setUseParentHandlers(useParentHandlers);
			logger.setLevel(level);
		}
	}

	private static CollectingHandler attach(Logger logger, Level tier) {
		for (var h : logger.getHandlers())
			logger.removeHandler(h);
		logger.setUseParentHandlers(false);
		logger.setLevel(tier);
		var handler = new CollectingHandler();
		handler.setLevel(Level.INFO);
		logger.addHandler(handler);
		return handler;
	}

	private static HttpResponse<String> capGet(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(capFixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	@Test void h01_delayedAsync_emitsOnCompletionPath_notOnFinish() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".delayed");
		var state = new LoggerState(opLogger);
		try {
			forceOn();  // Body-dump gate (test-only seam) so the FINEST response body renders.
			var handler = attach(opLogger, Level.FINEST);
			AsyncCaptureServlet.gate = null;
			AsyncCaptureServlet.finishLatch = new CountDownLatch(1);

			var respFuture = HTTP.sendAsync(
				HttpRequest.newBuilder()
					.uri(URI.create(capFixture.getRootUrl() + "/asyncCap/delayed"))
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build(),
				BodyHandlers.ofString());

			// Barrier: the request thread has returned from RestSession.finish() and run @RestEndCall.
			assertTrue(AsyncCaptureServlet.finishLatch.await(10, TimeUnit.SECONDS),
				"request thread did not reach @RestEndCall");

			// RED on synchronous-finish emit: finish() would already have emitted an incomplete record here.
			// GREEN on completion-path emit: nothing is emitted until the future completes below.
			assertEquals(0, handler.forLogger(opLogger.getName()).size(),
				"no debug record may be emitted between synchronous finish() and async completion");

			// Complete on the completion thread with a non-empty body; the endpoint sets a late header there too.
			assertNotNull(AsyncCaptureServlet.gate, "gate future should have been published by the handler");
			AsyncCaptureServlet.gate.complete("delayed-body-value");

			var resp = respFuture.get(15, TimeUnit.SECONDS);
			assertEquals(200, resp.statusCode(), "body: " + resp.body());
			assertTrue(resp.body().contains("delayed-body-value"), "body: " + resp.body());

			// Exactly one INFO record, emitted only after completion, carrying the late header AND the late body.
			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one completion-path record expected");
			var rec = recs.get(0);
			assertEquals(Level.INFO, rec.getLevel(), "records remain INFO-stamped; tier controls detail only");
			assertTrue(rec.getMessage().contains("X-Late-Header"), rec.getMessage());
			assertTrue(rec.getMessage().contains("late-header-value"), rec.getMessage());
			assertTrue(rec.getMessage().contains("delayed-body-value"),
				"completion-path record must contain the body written during completion: " + rec.getMessage());
		} finally {
			reset();
			AsyncCaptureServlet.finishLatch = null;
			var g = AsyncCaptureServlet.gate;
			if (g != null)
				g.complete("cleanup");  // never leave a request hung if an assertion failed before completion
			AsyncCaptureServlet.gate = null;
			state.restore();
		}
	}

	@Test void h02_alreadyCompletedFuture_recordIncludesExecTime() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".completed");
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.FINE);  // FINE tier → headers section, which carries "Exec time:".
			var resp = capGet("/asyncCap/completed");
			assertEquals(200, resp.statusCode(), resp.body());
			assertTrue(resp.body().contains("completed-body-value"), resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one completion-path record expected");
			var rec = recs.get(0);
			assertEquals(Level.INFO, rec.getLevel());
			assertTrue(rec.getMessage().contains("Exec time:"),
				"an inline-completed future must still carry finish-time attributes: " + rec.getMessage());
		} finally {
			state.restore();
		}
	}

	@Test void h03_futureError_finest_emitsOnceWithThrown() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".finestError");
		var state = new LoggerState(opLogger);
		try {
			forceOn();
			var handler = attach(opLogger, Level.FINEST);
			var resp = capGet("/asyncCap/finestError");
			assertEquals(500, resp.statusCode(), resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "error path must still emit exactly one completion-path record");
			var rec = recs.get(0);
			assertEquals(Level.INFO, rec.getLevel());
			assertNotNull(rec.getThrown(), "error record must carry the terminal cause as thrown");
			assertTrue(rec.getMessage().contains("[500]"), rec.getMessage());
		} finally {
			reset();
			state.restore();
		}
	}

	@Test void h04_timeout_finest_emitsOnceWithThrown() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".neverCompletes");
		var state = new LoggerState(opLogger);
		try {
			forceOn();
			var handler = attach(opLogger, Level.FINEST);
			var resp = capGet("/asyncCap/neverCompletes");
			assertEquals(504, resp.statusCode(), resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "timeout path must still emit exactly one completion-path record");
			var rec = recs.get(0);
			assertEquals(Level.INFO, rec.getLevel());
			assertNotNull(rec.getThrown(), "timeout record must carry the GatewayTimeout as thrown");
			assertTrue(rec.getMessage().contains("[504]"), rec.getMessage());
		} finally {
			reset();
			state.restore();
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H05/H06 (async-carry Phase 6): the async-completion debug record carries the request-thread's LogContext across the
	// async hop (RestDebugSnapshot carry + emit pre-seed), while ad-hoc app logging on the completion thread does NOT
	// (the documented v1 thread-confined boundary).
	//
	// Async-carry v1 does NOT open any framework LogContext scope itself (request-id / scope ownership is deferred to a
	// future RequestIdFilter, opened after @RestStartCall). These tests therefore establish their own request-thread scope
	// in the handler (carrying a generic "corrId"), left open across the async hand-off and closed in @RestEndCall — which
	// is exactly the shape that future filter will produce — to exercise the async-carry mechanism in isolation.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h05_asyncCompletionRecord_carriesRequestThreadContextField() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".delayed");
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.FINE);
			AsyncCaptureServlet.gate = null;
			AsyncCaptureServlet.carryValue = "async-corr-1";  // handler opens a request-thread LogContext scope from this
			AsyncCaptureServlet.finishLatch = new CountDownLatch(1);

			var respFuture = HTTP.sendAsync(
				HttpRequest.newBuilder()
					.uri(URI.create(capFixture.getRootUrl() + "/asyncCap/delayed"))
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build(),
				BodyHandlers.ofString());

			// The request thread has returned from finish() and @RestEndCall closed the carry scope — so the completion
			// record can only carry corrId if it was snapshotted on the request thread and re-established at emit (carry).
			assertTrue(AsyncCaptureServlet.finishLatch.await(10, TimeUnit.SECONDS),
				"request thread did not reach @RestEndCall");
			assertNotNull(AsyncCaptureServlet.gate, "gate future should have been published by the handler");
			AsyncCaptureServlet.gate.complete("delayed-body-value");

			var resp = respFuture.get(15, TimeUnit.SECONDS);
			assertEquals(200, resp.statusCode(), "body: " + resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one completion-path record expected");
			var rec = recs.get(0);
			assertEquals("async-corr-1", LogRecordContext.of(rec).get("corrId"),
				"the async completion record must carry the LogContext captured on the request thread (design §8.2 carry)");

			// Deferred (Phase 10) async gate: the always-on requestId scope (opened in RestSession's constructor) is
			// carried across the async hop via the same 364 snapshot, AND the completion-thread rendered message
			// carries the [requestId=<id>] prefix (statusLine reading session.getRequestId()) — proving 364's carry
			// and 402's statusLine read compose on the completion thread, where the live LogContext is empty.
			var rid = LogRecordContext.of(rec).get("requestId");
			assertNotNull(rid, "the async completion record must also carry the always-on framework requestId");
			assertTrue(rec.getMessage().contains("[requestId=" + rid + "] "),
				"the async completion rendered message must carry the requestId prefix: " + rec.getMessage());
		} finally {
			AsyncCaptureServlet.carryValue = null;
			AsyncCaptureServlet.finishLatch = null;
			var g = AsyncCaptureServlet.gate;
			if (g != null)
				g.complete("cleanup");
			AsyncCaptureServlet.gate = null;
			state.restore();
		}
	}

	@Test void h06_adHocCompletionThreadLog_doesNotCarryContext_documentedV1Edge() throws Exception {
		var opLogger = Logger.getLogger(AsyncCaptureServlet.class.getName() + ".adhoc");
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.FINE);
			handler.setLevel(Level.ALL);  // also collect the FINE ad-hoc record, not just the INFO debug record
			AsyncCaptureServlet.adhocGate = null;
			AsyncCaptureServlet.carryValue = "async-corr-adhoc";  // handler opens a request-thread LogContext scope
			AsyncCaptureServlet.finishLatch = new CountDownLatch(1);

			var respFuture = HTTP.sendAsync(
				HttpRequest.newBuilder()
					.uri(URI.create(capFixture.getRootUrl() + "/asyncCap/adhoc"))
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build(),
				BodyHandlers.ofString());

			assertTrue(AsyncCaptureServlet.finishLatch.await(10, TimeUnit.SECONDS),
				"request thread did not reach @RestEndCall");
			assertNotNull(AsyncCaptureServlet.adhocGate, "adhoc gate future should have been published by the handler");
			AsyncCaptureServlet.adhocGate.complete("adhoc-body");

			var resp = respFuture.get(15, TimeUnit.SECONDS);
			assertEquals(200, resp.statusCode(), "body: " + resp.body());

			// The documented v1 edge: ad-hoc req.fine(...) run on the completion thread does NOT carry the context (the
			// request-thread scope is closed and nothing re-opens a general scope there). Update this test AND the docs
			// scope line if general async LogContext propagation is ever added.
			var adhocRec = handler.forLogger(opLogger.getName()).stream()
				.filter(r -> "adhoc-completion-log".equals(r.getMessage()))
				.findFirst().orElse(null);
			assertNotNull(adhocRec, "expected the ad-hoc FINE record emitted on the completion thread");
			assertNull(LogRecordContext.of(adhocRec).get("corrId"),
				"documented v1 edge: ad-hoc completion-thread logging must NOT carry the request-thread context");

			// Contrast: the debug-pipeline completion record on the SAME logger DOES carry it (the Phase 6 carry).
			var debugRec = handler.forLogger(opLogger.getName()).stream()
				.filter(r -> r.getLevel() == Level.INFO)
				.findFirst().orElse(null);
			assertNotNull(debugRec, "expected the INFO debug completion record");
			assertEquals("async-corr-adhoc", LogRecordContext.of(debugRec).get("corrId"),
				"the debug-pipeline completion record must still carry the context via the Phase 6 carry");
		} finally {
			AsyncCaptureServlet.carryValue = null;
			AsyncCaptureServlet.finishLatch = null;
			var g = AsyncCaptureServlet.adhocGate;
			if (g != null)
				g.complete("cleanup");
			AsyncCaptureServlet.adhocGate = null;
			state.restore();
		}
	}
}
