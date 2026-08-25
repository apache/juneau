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
package org.apache.juneau.rest.reactive;

import static org.apache.juneau.rest.server.logging.RestDebugDumpGateTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity coverage for {@link org.apache.juneau.rest.server.reactive.ReactiveResponseProcessor}
 * on the true {@code AsyncContext} streaming path.
 *
 * <p>
 * The {@code MockServletRequest} harness used by {@code ReactiveResponseProcessor_Test} reports
 * {@code isAsyncSupported() == false}, so it only exercises the synchronous-fallback stream. This class boots a real
 * Jetty microservice to drive the async-dispatch streaming branch and prove that the single access/debug record is
 * emitted on the <b>stream-termination</b> path (after the final frame), not during synchronous {@code finish()}.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class ReactiveResponseProcessor_JettyMicroservice_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Publishers.
	// -----------------------------------------------------------------------------------------------------------------

	/** Emits a fixed list synchronously on demand (trampolined so re-request does not recurse). */
	static final class ListPublisher<T> implements Flow.Publisher<T> {
		private final List<T> items;
		ListPublisher(List<T> items) { this.items = items; }

		@SafeVarargs
		static <T> ListPublisher<T> of(T... items) { return new ListPublisher<>(List.of(items)); }

		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				private int idx;
				private final AtomicLong demand = new AtomicLong();
				private boolean draining;
				private volatile boolean cancelled;

				@Override public void request(long n) {
					if (cancelled)
						return;
					demand.addAndGet(n);
					if (draining)
						return;
					draining = true;
					try {
						while (! cancelled && demand.get() > 0 && idx < items.size()) {
							demand.decrementAndGet();
							sub.onNext(items.get(idx++));
						}
						if (! cancelled && idx >= items.size())
							sub.onComplete();
					} finally {
						draining = false;
					}
				}

				@Override public void cancel() { cancelled = true; }
			});
		}
	}

	/** Emits one element then errors. */
	static final class ErrorPublisher<T> implements Flow.Publisher<T> {
		private final T first;
		ErrorPublisher(T first) { this.first = first; }

		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				private boolean done;
				@Override public void request(long n) {
					if (done)
						return;
					done = true;
					sub.onNext(first);
					sub.onError(new RuntimeException("reactive-boom"));
				}
				@Override public void cancel() { done = true; }
			});
		}
	}

	/**
	 * Emits nothing until an external gate future completes, then emits one frame and completes — <b>off</b> the request
	 * thread. This defers stream termination past the request thread's {@code finish()}/{@code @RestEndCall}, opening the
	 * post-finish/pre-termination window the red/green gate observes.
	 */
	static final class GatedPublisher<T> implements Flow.Publisher<T> {
		private final CompletableFuture<Void> gate;
		private final T frame;
		GatedPublisher(CompletableFuture<Void> gate, T frame) { this.gate = gate; this.frame = frame; }

		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				@Override public void request(long n) { /* demand ignored — emission is gated */ }
				@Override public void cancel() { /* no-op */ }
			});
			gate.thenRunAsync(() -> {
				sub.onNext(frame);
				sub.onComplete();
			});
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Resource.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/reactiveCap/*", serializers=JsonSerializer.class)
	public static class ReactiveCaptureServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		/** Gate future for the delayed stream; completed by the test to trigger termination + completion-path emit. */
		static volatile CompletableFuture<Void> gate;

		/** Counted down by {@code @RestEndCall} on the request thread — i.e. AFTER {@code RestSession.finish()} returns. */
		static volatile CountDownLatch finishLatch;

		@RestGet("/delayed")
		public Flow.Publisher<SseEvent> delayed(RestResponse res) {
			res.setContentType("text/event-stream");
			return new GatedPublisher<>(gate, new SseEvent("tick", "late-frame"));
		}

		@RestGet("/normal")
		public Flow.Publisher<SseEvent> normal(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new SseEvent("tick", "one"), new SseEvent("tick", "two"));
		}

		@RestGet("/error")
		public Flow.Publisher<SseEvent> error(RestResponse res) {
			res.setContentType("text/event-stream");
			return new ErrorPublisher<>(new SseEvent("tick", "before-error"));
		}

		@RestGet("/big")
		public Flow.Publisher<SseEvent> big(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new SseEvent("tick", "B".repeat(20_000)));  // > 8KB default cap → truncation
		}

		@RestEndCall
		public void endCall() {
			var l = finishLatch;
			if (l != null)
				l.countDown();
		}
	}

	@Configuration
	public static class Config {
		@Bean(name="reactiveCaptureServlet")
		public Servlet reactiveCaptureServlet() { return new ReactiveCaptureServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(Config.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	// -----------------------------------------------------------------------------------------------------------------
	// Capture helpers (single op logger, useParentHandlers=false so only that op's records are collected).
	// -----------------------------------------------------------------------------------------------------------------

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

	private static String opLoggerName(String method) {
		return ReactiveCaptureServlet.class.getName() + "." + method;
	}

	private static HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Tests.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void r01_delayedStream_emitsOnTerminationPath_notOnFinish() throws Exception {
		var opLogger = Logger.getLogger(opLoggerName("delayed"));
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.INFO);
			ReactiveCaptureServlet.gate = new CompletableFuture<>();
			ReactiveCaptureServlet.finishLatch = new CountDownLatch(1);

			var respFuture = HTTP.sendAsync(
				HttpRequest.newBuilder()
					.uri(URI.create(fixture.getRootUrl() + "/reactiveCap/delayed"))
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build(),
				BodyHandlers.ofString());

			// Barrier: the request thread has returned from RestSession.finish() and run @RestEndCall.
			assertTrue(ReactiveCaptureServlet.finishLatch.await(10, TimeUnit.SECONDS),
				"request thread did not reach @RestEndCall");

			// RED on synchronous-finish emit: finish() would already have emitted here.
			// GREEN on completion-path emit: nothing is emitted until the stream terminates below.
			assertEquals(0, handler.forLogger(opLogger.getName()).size(),
				"no debug record may be emitted between synchronous finish() and stream termination");

			ReactiveCaptureServlet.gate.complete(null);  // fire the stream off the request thread

			var resp = respFuture.get(15, TimeUnit.SECONDS);
			assertEquals(200, resp.statusCode(), "body: " + resp.body());
			assertTrue(resp.body().contains("late-frame"), "body: " + resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one termination-path record expected");
			assertEquals(Level.INFO, recs.get(0).getLevel());
			assertTrue(recs.get(0).getMessage().contains("[200]"), recs.get(0).getMessage());
		} finally {
			var g = ReactiveCaptureServlet.gate;
			if (g != null)
				g.complete(null);  // never leave a request hung if an assertion failed before termination
			ReactiveCaptureServlet.gate = null;
			ReactiveCaptureServlet.finishLatch = null;
			state.restore();
		}
	}

	@Test void r02_normalStream_emitsOneInfoRecordOnCompletion() throws Exception {
		var opLogger = Logger.getLogger(opLoggerName("normal"));
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.INFO);
			var resp = get("/reactiveCap/normal");
			assertEquals(200, resp.statusCode(), resp.body());
			assertTrue(resp.body().contains("data: one"), resp.body());
			assertTrue(resp.body().contains("data: two"), resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one termination-path record expected");
			assertEquals(Level.INFO, recs.get(0).getLevel());
			assertTrue(recs.get(0).getMessage().contains("[200]"), recs.get(0).getMessage());
		} finally {
			state.restore();
		}
	}

	@Test void r03_streamError_emitsOneRecordWithThrown() throws Exception {
		var opLogger = Logger.getLogger(opLoggerName("error"));
		var state = new LoggerState(opLogger);
		try {
			var handler = attach(opLogger, Level.INFO);
			var resp = get("/reactiveCap/error");
			// The first frame is already committed (200) before the error, so SSE cannot retract the status.
			assertEquals(200, resp.statusCode(), resp.body());
			assertTrue(resp.body().contains("before-error"), resp.body());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "stream error must still emit exactly one termination-path record");
			assertEquals(Level.INFO, recs.get(0).getLevel());
			assertNotNull(recs.get(0).getThrown(), "stream-error record must carry the terminal cause as thrown");
		} finally {
			state.restore();
		}
	}

	@Test void r04_finestOverCapStream_rendersBoundedPrefixWithTruncationMarker() throws Exception {
		var opLogger = Logger.getLogger(opLoggerName("big"));
		var state = new LoggerState(opLogger);
		try {
			forceOn();  // Body-dump gate (test-only seam) so the FINEST response body renders.
			var handler = attach(opLogger, Level.FINEST);
			var resp = get("/reactiveCap/big");
			assertEquals(200, resp.statusCode());

			var recs = handler.forLogger(opLogger.getName());
			assertEquals(1, recs.size(), "exactly one termination-path record expected");
			var msg = recs.get(0).getMessage();
			assertEquals(Level.INFO, recs.get(0).getLevel());
			assertTrue(msg.contains("BBBB"), "FINEST tier must render the captured stream prefix: " + msg.substring(0, Math.min(msg.length(), 200)));
			assertTrue(msg.contains("truncated"),
				"an over-cap stream body must render a visible truncation marker");
		} finally {
			reset();
			state.restore();
		}
	}
}
