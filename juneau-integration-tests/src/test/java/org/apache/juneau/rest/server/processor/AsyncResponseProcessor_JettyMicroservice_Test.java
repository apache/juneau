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

		/** Counted down by {@code @RestEndCall} on the request thread — i.e. AFTER {@code RestSession.finish()} returns. */
		static volatile CountDownLatch finishLatch;

		@RestGet("/delayed")
		public CompletableFuture<String> delayed(RestResponse res) {
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

		@RestEndCall
		public void endCall() {
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

		@Override public void publish(LogRecord record) {
			if (isLoggable(record))
				records.add(record);
		}

		@Override public void flush() {}
		@Override public void close() {}

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
}
