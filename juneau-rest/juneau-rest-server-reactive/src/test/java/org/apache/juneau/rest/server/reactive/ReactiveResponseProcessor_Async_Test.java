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
package org.apache.juneau.rest.server.reactive;

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.reactive.ReactiveResponseProcessor_Test.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Tests the {@link jakarta.servlet.AsyncContext}-driven streaming path of {@link ReactiveResponseProcessor}.
 *
 * <p>
 * {@code MockRestClient} never reports async support, so these tests drive {@link RestContext#execute} directly with
 * a request whose {@code startAsync()} hands back a recording {@link AsyncContext}, and a response that can be made
 * to fail on flush, write or {@code sendError}. Every publisher signals inline on the calling thread, so no test
 * waits on another thread.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Test doubles; nothing here holds an OS resource.
})
class ReactiveResponseProcessor_Async_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Test doubles.
	// -----------------------------------------------------------------------------------------------------------------

	/** {@link AsyncContext} that records what the processor does to it. */
	static final class FakeAsyncContext implements AsyncContext {
		final List<AsyncListener> listeners = new ArrayList<>();
		final AtomicInteger completes = new AtomicInteger();
		volatile long timeout = -1;
		boolean completeThrows;

		@Override public ServletRequest getRequest() { return null; }
		@Override public ServletResponse getResponse() { return null; }
		@Override public boolean hasOriginalRequestAndResponse() { return true; }
		@Override public void dispatch() { /* unused */ }
		@Override public void dispatch(String path) { /* unused */ }
		@Override public void dispatch(ServletContext context, String path) { /* unused */ }
		@Override public void complete() {
			completes.incrementAndGet();
			if (completeThrows)
				throw new IllegalStateException("already completed");
		}
		@Override public void start(Runnable run) { /* unused */ }
		@Override public void addListener(AsyncListener listener) { listeners.add(listener); }
		@Override public void addListener(AsyncListener listener, ServletRequest req, ServletResponse res) { listeners.add(listener); }
		@Override public <T extends AsyncListener> T createListener(Class<T> clazz) { return null; }
		@Override public void setTimeout(long ms) { timeout = ms; }
		@Override public long getTimeout() { return timeout; }
	}

	/** Request that reports async support and hands out a {@link FakeAsyncContext}. */
	static final class AsyncRequest extends MockServletRequest {
		final FakeAsyncContext async = new FakeAsyncContext();
		boolean supported = true;
		boolean startAsyncThrows;

		@Override public boolean isAsyncSupported() { return supported; }
		@Override public AsyncContext startAsync() {
			if (startAsyncThrows)
				throw new IllegalStateException("response already committed");
			return async;
		}
	}

	/** Response whose commit state, flush, write and {@code sendError} behaviour can be controlled. */
	static final class TestResponse extends MockServletResponse {
		boolean committed;
		boolean failFlush;
		boolean failWrite;
		boolean sendErrorThrows;
		int sendErrors;

		@Override public boolean isCommitted() { return committed; }
		@Override public void flushBuffer() throws IOException {
			if (failFlush)
				throw new IOException("flush failed");
		}
		@Override public void sendError(int sc) throws IOException {
			sendErrors++;
			if (sendErrorThrows)
				throw new IOException("sendError failed");
			super.sendError(sc);
		}
		@Override public ServletOutputStream getOutputStream() throws IOException {
			if (! failWrite)
				return super.getOutputStream();
			return new ServletOutputStream() {
				@Override public boolean isReady() { return true; }
				@Override public void setWriteListener(WriteListener l) { /* unused */ }
				@Override public void write(int b) throws IOException { throw new IOException("client gone"); }
			};
		}
	}

	/** Publisher that records its subscription and hands the subscriber to a callback. */
	static final class ScriptedPublisher<T> implements Flow.Publisher<T> {
		final AtomicBoolean cancelled = new AtomicBoolean();
		private final java.util.function.Consumer<Flow.Subscriber<? super T>> script;
		ScriptedPublisher(java.util.function.Consumer<Flow.Subscriber<? super T>> script) { this.script = script; }

		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				@Override public void request(long n) { /* script drives emission */ }
				@Override public void cancel() { cancelled.set(true); }
			});
			script.accept(sub);
		}
	}

	/** Emits one event on the first request, then throws the supplied exception from the second request. */
	static Flow.Publisher<SseEvent> failingSecondRequest(RuntimeException failure) {
		return sub -> sub.onSubscribe(new Flow.Subscription() {
			private int requests;
			@Override public void request(long n) {
				if (++requests == 1)
					sub.onNext(new SseEvent("tick", "first"));
				else
					throw failure;
			}
			@Override public void cancel() { /* nothing to release */ }
		});
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Resource.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class A {
		static final ScriptedPublisher<SseEvent> SILENT = new ScriptedPublisher<>(s -> { /* never signals */ });
		static final ScriptedPublisher<SseEvent> ERROR_TWICE = new ScriptedPublisher<>(s -> {
			s.onError(new IllegalStateException("first"));
			s.onError(new IllegalStateException("second"));
		});

		static final ScriptedPublisher<SseEvent> SILENT_SHORT = new ScriptedPublisher<>(s -> { /* never signals */ });

		@RestOp(method="GET", path="/silentShort", asyncTimeoutMillis="50")
		public Flow.Publisher<SseEvent> silentShort(RestResponse res) {
			res.setContentType("text/event-stream");
			return SILENT_SHORT;
		}

		@RestOp(method="GET", path="/unsubscribed", asyncTimeoutMillis="50")
		public Flow.Publisher<SseEvent> unsubscribed(RestResponse res) {
			res.setContentType("text/event-stream");
			return sub -> { /* never calls onSubscribe */ };
		}

		@RestGet("/unserializable")
		public Flow.Publisher<Object> unserializable(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new Unserializable());
		}

		@RestGet("/requestFailsCompletion")
		public Flow.Publisher<SseEvent> requestFailsCompletion(RestResponse res) {
			res.setContentType("text/event-stream");
			return failingSecondRequest(new CompletionException(new IOException("wrapped")));
		}

		@RestGet("/requestFailsIllegalState")
		public Flow.Publisher<SseEvent> requestFailsIllegalState(RestResponse res) {
			res.setContentType("text/event-stream");
			return failingSecondRequest(new IllegalStateException("no cause"));
		}

		@RestGet("/requestFailsOther")
		public Flow.Publisher<SseEvent> requestFailsOther(RestResponse res) {
			res.setContentType("text/event-stream");
			return failingSecondRequest(new IllegalArgumentException("bad demand"));
		}

		@RestGet("/list")
		public Flow.Publisher<SseEvent> list(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new SseEvent("tick", "one"), new SseEvent("tick", "two"));
		}

		@RestGet("/empty")
		public Flow.Publisher<SseEvent> empty(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of();
		}

		@RestGet("/silent")
		public Flow.Publisher<SseEvent> silent(RestResponse res) {
			res.setContentType("text/event-stream");
			return SILENT;
		}

		@RestGet("/errorBeforeWrite")
		public Flow.Publisher<SseEvent> errorBeforeWrite(RestResponse res) {
			res.setContentType("text/event-stream");
			return new ErrorPublisher<>(new SseEvent("tick", "never"), false);
		}

		@RestGet("/errorTwice")
		public Flow.Publisher<SseEvent> errorTwice(RestResponse res) {
			res.setContentType("text/event-stream");
			return ERROR_TWICE;
		}

		@RestGet("/subscribeThrows")
		public Flow.Publisher<SseEvent> subscribeThrows(RestResponse res) {
			res.setContentType("text/event-stream");
			return sub -> { throw new IllegalArgumentException("subscribe refused"); };
		}
	}

	private static RestContext context;

	private static final Logger LOG = Logger.getLogger(ReactiveResponseProcessor.class.getName());
	private final List<LogRecord> records = new ArrayList<>();
	private final Handler handler = new Handler() {
		@Override public void publish(LogRecord r) { records.add(r); }
		@Override public void flush() { /* nothing buffered */ }
		@Override public void close() { /* nothing to release */ }
	};
	private Level savedLevel;

	@BeforeEach void captureLog() {
		savedLevel = LOG.getLevel();
		LOG.setLevel(Level.ALL);
		handler.setLevel(Level.ALL);
		LOG.addHandler(handler);
	}

	@AfterEach void releaseLog() {
		LOG.removeHandler(handler);
		LOG.setLevel(savedLevel);
	}

	/** Message of the first captured record containing the text, or null. */
	private String logged(String text) {
		return records.stream().map(r -> r.getMessage() == null ? "" : r.getMessage()).filter(m -> m.contains(text)).findFirst().orElse(null);
	}

	@BeforeAll static void beforeAll() throws Exception {
		var resource = new A();
		context = new RestContext(new RestContext.Args(A.class, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	private static AsyncRequest request(String path) {
		var req = new AsyncRequest();
		req.method("GET").uri(path);
		req.contextPath("").servletPath("");
		return req;
	}

	private static void run(AsyncRequest req, TestResponse res) throws Exception {
		context.execute(context.getResource(), req, res);
	}

	private static String body(TestResponse res) throws Exception {
		return new String(res.getContent(), "UTF-8");
	}

	private static AsyncEvent event(AsyncRequest req, Throwable t) {
		return new AsyncEvent(req.async, t);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Normal completion.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_inlinePublisher_streamsThenCompletesAsyncContextOnce() throws Exception {
		var req = request("/list");
		var res = new TestResponse();
		run(req, res);

		assertEquals("event: tick\ndata: one\n\nevent: tick\ndata: two\n\n", body(res));
		assertEquals(0L, req.async.timeout, "stream must not use a container timeout");
		assertSize(1, req.async.listeners);
		assertEquals(1, req.async.completes.get());
		assertEquals(0, res.sendErrors);

		// The container's own onComplete notification is a gated no-op.
		req.async.listeners.get(0).onComplete(event(req, null));
		req.async.listeners.get(0).onStartAsync(event(req, null));
		assertEquals(1, req.async.completes.get());
	}

	@Test void a02_flushFailureDuringCompletion_stillCompletesAsyncContext() throws Exception {
		var req = request("/empty");
		var res = new TestResponse();
		res.failFlush = true;
		run(req, res);

		assertEquals("", body(res));
		assertEquals(1, req.async.completes.get());
		assertEquals("Flush during async stream completion failed: flush failed", logged("Flush during async"));
	}

	@Test void a03_asyncContextCompleteRaces_isSwallowed() throws Exception {
		var req = request("/list");
		req.async.completeThrows = true;
		var res = new TestResponse();
		run(req, res);

		assertEquals(1, req.async.completes.get());
		assertTrue(body(res).contains("data: two"));
		assertEquals("AsyncContext.complete() raced with the container: already completed", logged("raced with the container"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Container-driven termination of a stream the publisher never ends.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_containerTimeout_completesOnceEvenIfRepeated() throws Exception {
		var req = request("/silent");
		var res = new TestResponse();
		run(req, res);
		assertEquals(0, req.async.completes.get(), "stream is still open");

		var l = req.async.listeners.get(0);
		l.onTimeout(event(req, null));
		assertEquals(1, req.async.completes.get());

		l.onError(event(req, new IOException("late")));
		l.onTimeout(event(req, null));
		assertEquals(1, req.async.completes.get(), "completion must be exactly-once");
	}

	@Test void b02_containerError_completesOnce() throws Exception {
		var req = request("/silent");
		var res = new TestResponse();
		run(req, res);

		req.async.listeners.get(0).onError(event(req, new IOException("client reset")));
		assertEquals(1, req.async.completes.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Failures before the first frame.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_errorBeforeFirstFrame_sendsError500AndCompletes() throws Exception {
		var req = request("/errorBeforeWrite");
		var res = new TestResponse();
		run(req, res);

		assertEquals(500, res.getStatus());
		assertEquals(1, res.sendErrors);
		assertEquals(1, req.async.completes.get());
		assertEquals("Reactive stream terminated with error: boom", logged("terminated with error"));
	}

	@Test void c02_errorBeforeFirstFrame_responseAlreadyCommitted_noSendError() throws Exception {
		var req = request("/errorBeforeWrite");
		var res = new TestResponse();
		res.committed = true;
		run(req, res);

		assertEquals(0, res.sendErrors);
		assertEquals(1, req.async.completes.get());
	}

	@Test void c03_sendErrorFails_stillCompletes() throws Exception {
		var req = request("/errorBeforeWrite");
		var res = new TestResponse();
		res.sendErrorThrows = true;
		run(req, res);

		assertEquals(1, res.sendErrors);
		assertEquals(1, req.async.completes.get());
		assertEquals("sendError on pre-write stream failure failed: sendError failed", logged("sendError on pre-write"));
	}

	@Test void c04_secondTerminalSignalIsIgnored() throws Exception {
		var req = request("/errorTwice");
		var res = new TestResponse();
		run(req, res);

		assertEquals(1, res.sendErrors, "only the first terminal signal may act");
		assertEquals(1, req.async.completes.get());
	}

	@Test void c05_subscribeThrowsAfterHandoff_completesThroughSameHook() throws Exception {
		var req = request("/subscribeThrows");
		var res = new TestResponse();
		run(req, res);

		assertEquals(1, req.async.completes.get());
		assertSize(1, req.async.listeners);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Async not actually available.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_startAsyncRefused_fallsBackToSynchronousStreaming() throws Exception {
		var req = request("/list");
		req.startAsyncThrows = true;
		var res = new TestResponse();
		run(req, res);

		assertEquals("event: tick\ndata: one\n\nevent: tick\ndata: two\n\n", body(res));
		assertEquals(0, req.async.completes.get());
		assertTrue(req.async.listeners.isEmpty());
	}

	@Test void d02_synchronousFallback_finalFlushFailure_isSwallowed() throws Exception {
		var req = request("/empty");
		req.supported = false;
		var res = new TestResponse();
		res.failFlush = true;
		run(req, res);

		assertEquals("", body(res));
		assertEquals(0, req.async.completes.get());
		assertEquals("Final flush of synchronous reactive stream failed: flush failed", logged("Final flush"));
	}

	@Test void d03_synchronousFallback_publisherNeverTerminates_timesOutLogsAndCancels() throws Exception {
		var req = request("/silentShort");
		req.supported = false;
		var res = new TestResponse();
		run(req, res);

		assertTrue(A.SILENT_SHORT.cancelled.get());
		assertEquals(0, res.sendErrors);
		assertNotNull(logged("Synchronous reactive stream timed out"));
	}

	@Test void d04_synchronousFallback_publisherErrors_logsAndSendsError() throws Exception {
		var req = request("/errorBeforeWrite");
		req.supported = false;
		var res = new TestResponse();
		run(req, res);

		assertEquals(500, res.getStatus());
		assertEquals("Synchronous reactive stream did not complete cleanly: java.lang.RuntimeException: boom", logged("did not complete cleanly"));
	}

	@Test void d05_synchronousFallback_publisherNeverSubscribes_timeoutHandlesMissingSubscription() throws Exception {
		var req = request("/unsubscribed");
		req.supported = false;
		var res = new TestResponse();
		run(req, res);

		assertEquals(200, res.getStatus());
		assertNotNull(logged("timed out"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: A subscription that fails mid-stream is cancelled; the failure that is reported is the root cause when the
	// subscriber saw it wrapped.
	// -----------------------------------------------------------------------------------------------------------------

	private String syncFailureLog(String path) throws Exception {
		var req = request(path);
		req.supported = false;
		var res = new TestResponse();
		run(req, res);
		assertEquals("event: tick\ndata: first\n\n", body(res), "the committed first frame stays");
		return logged("did not complete cleanly");
	}

	@Test void e01_completionExceptionFromRequest_reportsCause() throws Exception {
		assertEquals("Synchronous reactive stream did not complete cleanly: java.io.IOException: wrapped", syncFailureLog("/requestFailsCompletion"));
	}

	@Test void e02_illegalStateWithoutCause_reportedAsIs() throws Exception {
		assertEquals("Synchronous reactive stream did not complete cleanly: java.lang.IllegalStateException: no cause", syncFailureLog("/requestFailsIllegalState"));
	}

	@Test void e03_otherRuntimeException_reportedAsIs() throws Exception {
		assertEquals("Synchronous reactive stream did not complete cleanly: java.lang.IllegalArgumentException: bad demand", syncFailureLog("/requestFailsOther"));
	}

	@Test void e04_unserializableFrame_reportsSerializerFailureNotItsWrapper() throws Exception {
		var req = request("/unserializable");
		req.supported = false;
		var res = new TestResponse();
		run(req, res);

		assertEquals(500, res.getStatus());
		var m = logged("did not complete cleanly");
		assertNotNull(m);
		assertTrue(m.startsWith("Synchronous reactive stream did not complete cleanly: org.apache.juneau.marshall.serializer.SerializeException"), m);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: A client that has gone away.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_writeFailure_sendsError500() throws Exception {
		var req = request("/list");
		req.supported = false;
		var res = new TestResponse();
		res.failWrite = true;
		run(req, res);

		assertEquals(500, res.getStatus());
		assertEquals(1, res.sendErrors);
	}
}
