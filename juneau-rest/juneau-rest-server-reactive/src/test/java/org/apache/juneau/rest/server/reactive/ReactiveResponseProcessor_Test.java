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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ReactiveResponseProcessor} using only the JDK-native
 * {@link Flow.Publisher Flow.Publisher} return type (no external reactive
 * library). This is the spine: {@code Flow.Publisher<SseEvent>} SSE streaming plus the
 * shared buffer / NDJSON shapes that all reactive types funnel through.
 *
 * <p>
 * Under {@code MockRestClient} the underlying {@code MockServletRequest} reports
 * {@code isAsyncSupported() == false}, so streaming exercises its synchronous-fallback path (subscribe
 * and block until the publisher terminates, writing frames as they arrive) and buffering exercises the
 * {@code AsyncResponseProcessor} synchronous fallback.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ReactiveResponseProcessor_Test extends TestBase {

	public static final class Pojo {
		public final String name;
		public final int value;
		public Pojo(String name, int value) { this.name = name; this.value = value; }
	}

	/**
	 * Trampolined synchronous {@link Flow.Publisher Flow.Publisher} that replays a
	 * fixed list on subscribe, honoring {@code request(n)} without recursing on re-request (so the
	 * one-at-a-time streaming subscriber is exercised deterministically on a single thread).
	 */
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

	/** Publisher that emits one element then errors. */
	static final class ErrorPublisher<T> implements Flow.Publisher<T> {
		private final T first;
		private final boolean emitFirst;
		ErrorPublisher(T first, boolean emitFirst) { this.first = first; this.emitFirst = emitFirst; }

		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				private boolean done;
				@Override public void request(long n) {
					if (done)
						return;
					done = true;
					if (emitFirst)
						sub.onNext(first);
					sub.onError(new RuntimeException("boom"));
				}
				@Override public void cancel() { done = true; }
			});
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Buffer shape — default media type collects the stream into a JSON array.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/flux")
		public Flow.Publisher<Pojo> flux() {
			return ListPublisher.of(new Pojo("foo", 1), new Pojo("bar", 2));
		}

		@RestGet("/empty")
		public Flow.Publisher<Pojo> empty() {
			return ListPublisher.of();
		}

		@RestGet("/sse")
		public Flow.Publisher<SseEvent> sse(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new SseEvent("tick", "one"), new SseEvent("tick", "two"));
		}

		@RestGet("/sseNonEvent")
		public Flow.Publisher<Pojo> sseNonEvent(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new Pojo("foo", 1));
		}

		@RestGet("/ndjson")
		public Flow.Publisher<Pojo> ndjson(RestResponse res) {
			res.setContentType("application/x-ndjson");
			return ListPublisher.of(new Pojo("foo", 1), new Pojo("bar", 2));
		}

		@RestGet("/errStream")
		public Flow.Publisher<SseEvent> errStream(RestResponse res) {
			res.setContentType("text/event-stream");
			return new ErrorPublisher<>(new SseEvent("tick", "one"), true);
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_flux_buffersToJsonArray() throws Exception {
		CA.get("/flux").accept("application/json").run()
			.assertStatus(200)
			.assertContent().isContains("\"name\":\"foo\"", "\"value\":1", "\"name\":\"bar\"", "\"value\":2");
	}

	@Test void a02_emptyFlux_buffersToEmptyArray() throws Exception {
		CA.get("/empty").accept("application/json").run().assertStatus(200).assertContent().is("[]");
	}

	@Test void a03_flowPublisherOfSseEvent_streamsSse() throws Exception {
		var r = CA.get("/sse").run();
		r.assertStatus(200);
		r.assertHeader("Content-Type").isContains("text/event-stream");
		var c = r.getContent().asString();
		assertTrue(c.contains("event: tick"), c);
		assertTrue(c.contains("data: one"), c);
		assertTrue(c.contains("data: two"), c);
	}

	@Test void a04_sseWithNonEventElement_jsonEncodesData() throws Exception {
		var c = CA.get("/sseNonEvent").run().assertStatus(200).getContent().asString();
		assertTrue(c.contains("data:"), c);
		assertTrue(c.contains("\"name\":\"foo\""), c);
	}

	@Test void a05_ndjson_writesOneJsonObjectPerLine() throws Exception {
		var c = CA.get("/ndjson").run().assertStatus(200).getContent().asString();
		var lines = c.strip().split("\n");
		assertEquals(2, lines.length, c);
		assertTrue(lines[0].contains("\"name\":\"foo\""), c);
		assertTrue(lines[1].contains("\"name\":\"bar\""), c);
	}

	@Test void a06_streamErrorAfterFirstFrame_stillDeliversFirstFrame() throws Exception {
		// First frame is committed before the error; SSE has no way to retract it, so the first
		// event is delivered and the stream simply ends.
		var c = CA.get("/errStream").run().getContent().asString();
		assertTrue(c.contains("data: one"), c);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Accept-header-driven SSE opt-in (serializer registered so negotiation passes).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class B {
		@RestGet("/events")
		public Flow.Publisher<SseEvent> events() {
			return ListPublisher.of(new SseEvent("msg", "hello"));
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_acceptHeaderSelectsSse() throws Exception {
		var r = CB.get("/events").header("Accept", "text/event-stream").run();
		r.assertStatus(200);
		r.assertHeader("Content-Type").isContains("text/event-stream");
		assertTrue(r.getContent().asString().contains("data: hello"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Non-reactive return values are untouched (processor is a transparent no-op).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class C {
		@RestGet("/plain")
		public Pojo plain() {
			return new Pojo("plain", 99);
		}

		@RestGet("/future")
		public CompletableFuture<Pojo> future() {
			return CompletableFuture.completedFuture(new Pojo("fut", 7));
		}
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_plainPojo_unaffected() throws Exception {
		CC.get("/plain").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"plain\"", "\"value\":99");
	}

	@Test void c02_completableFuture_stillHandledByAsyncProcessor() throws Exception {
		CC.get("/future").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"fut\"", "\"value\":7");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: SSE null-element — writeSseFrame skips null; NDJSON null-element — writeNdjsonFrame skips null.
	// These cover the early-return branches on lines 282 and 296.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class D {
		@RestGet("/sseWithNull")
		public Flow.Publisher<SseEvent> sseWithNull(RestResponse res) {
			res.setContentType("text/event-stream");
			// null element is sandwiched between two real events — only the non-null ones should appear.
			return new Flow.Publisher<>() {
				@Override public void subscribe(Flow.Subscriber<? super SseEvent> sub) {
					sub.onSubscribe(new Flow.Subscription() {
						int step;
						@Override public void request(long n) {
							if (step == 0) { step++; sub.onNext(new SseEvent("tick", "before")); }
							else if (step == 1) { step++; sub.onNext(null); }
							else if (step == 2) { step++; sub.onNext(new SseEvent("tick", "after")); }
							else sub.onComplete();
						}
						@Override public void cancel() { /* test fixture — no cleanup needed */ }
					});
				}
			};
		}

		@RestGet("/ndjsonWithNull")
		public Flow.Publisher<Pojo> ndjsonWithNull(RestResponse res) {
			res.setContentType("application/x-ndjson");
			return new Flow.Publisher<>() {
				@Override public void subscribe(Flow.Subscriber<? super Pojo> sub) {
					sub.onSubscribe(new Flow.Subscription() {
						int step;
						@Override public void request(long n) {
							if (step == 0) { step++; sub.onNext(new Pojo("first", 1)); }
							else if (step == 1) { step++; sub.onNext(null); }
							else if (step == 2) { step++; sub.onNext(new Pojo("last", 3)); }
							else sub.onComplete();
						}
						@Override public void cancel() { /* test fixture — no cleanup needed */ }
					});
				}
			};
		}

		// NDJSON with content-type already set (non-null ct path in prepareStreamingHeaders line 233).
		@RestGet("/ndjsonCtAlreadySet")
		public Flow.Publisher<Pojo> ndjsonCtAlreadySet(RestResponse res) {
			res.setContentType("application/x-ndjson");
			return ListPublisher.of(new Pojo("x", 42));
		}
	}

	private static final MockRestClient CD = MockRestClient.buildLax(D.class);

	@Test void d01_sseNullElement_skipped() throws Exception {
		var c = CD.get("/sseWithNull").run().assertStatus(200).getContent().asString();
		assertTrue(c.contains("data: before"), c);
		assertTrue(c.contains("data: after"), c);
	}

	@Test void d02_ndjsonNullElement_skipped() throws Exception {
		var c = CD.get("/ndjsonWithNull").run().assertStatus(200).getContent().asString();
		var lines = Arrays.stream(c.strip().split("\n"))
			.filter(l -> !l.isBlank()).toList();
		assertEquals(2, lines.size(), "Expected 2 non-null frames; got: " + c);
		assertTrue(lines.get(0).contains("\"name\":\"first\""), c);
		assertTrue(lines.get(1).contains("\"name\":\"last\""), c);
	}

	@Test void d03_ndjsonCtAlreadySet_contentTypePreserved() throws Exception {
		var r = CD.get("/ndjsonCtAlreadySet").run().assertStatus(200);
		r.assertHeader("Content-Type").isContains("ndjson");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: StreamingSubscriber.terminate() error-before-write path — error arrives before any frame is
	// written, triggering the sendError(500) branch (line 441).  MockServletResponse.isCommitted()
	// returns false until a write, so sendError is reachable here.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class E {
		@RestGet("/errorBeforeWrite")
		public Flow.Publisher<SseEvent> errorBeforeWrite(RestResponse res) {
			res.setContentType("text/event-stream");
			// ErrorPublisher with emitFirst=false: error arrives without any prior frame.
			return new ErrorPublisher<>(new SseEvent("tick", "never"), false);
		}
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void e01_errorBeforeWrite_returns500() throws Exception {
		// The subscriber fires sendError(500) because no frame was written before the error.
		CE.get("/errorBeforeWrite").run().assertStatus(500);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: NDJSON shape selected via jsonl/json-seq keywords in resolveShape (line 315).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class F {
		@RestGet("/ndjsonViaJsonl")
		public Flow.Publisher<Pojo> ndjsonViaJsonl() {
			return ListPublisher.of(new Pojo("p", 7));
		}
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_acceptJsonl_triggersNdjsonShape() throws Exception {
		var c = CF.get("/ndjsonViaJsonl").header("Accept", "application/jsonl").run()
			.assertStatus(200).getContent().asString();
		assertTrue(c.contains("\"name\":\"p\""), c);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: CharSequence elements — writeSseFrame (line 288) and writeNdjsonFrame (line 298) each have a
	// CharSequence branch that writes the element's toString() directly instead of JSON-encoding it.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class G {
		@RestGet("/sseCharSeq")
		public Flow.Publisher<CharSequence> sseCharSeq(RestResponse res) {
			res.setContentType("text/event-stream");
			return ListPublisher.of(new StringBuilder("hello-char"), new StringBuilder("world-char"));
		}

		@RestGet("/ndjsonCharSeq")
		public Flow.Publisher<CharSequence> ndjsonCharSeq(RestResponse res) {
			res.setContentType("application/x-ndjson");
			return ListPublisher.of(new StringBuilder("line-one"), new StringBuilder("line-two"));
		}
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test void g01_sseCharSequenceElement_writtenVerbatim() throws Exception {
		var c = CG.get("/sseCharSeq").run().assertStatus(200).getContent().asString();
		assertTrue(c.contains("data: hello-char"), c);
		assertTrue(c.contains("data: world-char"), c);
	}

	@Test void g02_ndjsonCharSequenceElement_writtenVerbatim() throws Exception {
		var c = CG.get("/ndjsonCharSeq").run().assertStatus(200).getContent().asString();
		assertTrue(c.contains("line-one"), c);
		assertTrue(c.contains("line-two"), c);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: resolveShape — remaining NDJSON keyword branches: json-seq in Accept header (line 315).
	//    SSE with ct already containing "event-stream" (line 230 false branch — ct not overridden).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class H {
		@RestGet("/byJsonSeq")
		public Flow.Publisher<Pojo> byJsonSeq() {
			return ListPublisher.of(new Pojo("q", 1));
		}

		// SSE: handler sets event-stream ct; prepareStreamingHeaders should NOT override it.
		@RestGet("/sseCtAlreadyEventStream")
		public Flow.Publisher<SseEvent> sseCtAlreadyEventStream(RestResponse res) {
			res.setContentType("text/event-stream; charset=utf-8");
			return ListPublisher.of(new SseEvent("msg", "already-set"));
		}

		// SSE shape via Accept header, but handler sets a non-event-stream ct (line 230: ct != null && !ct.contains("event-stream")).
		@RestGet("/sseCtNonEventStream")
		public Flow.Publisher<SseEvent> sseCtNonEventStream(RestResponse res) {
			res.setContentType("text/plain");
			return ListPublisher.of(new SseEvent("msg", "overridden"));
		}
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_acceptJsonSeq_triggersNdjsonShape() throws Exception {
		var c = CH.get("/byJsonSeq").header("Accept", "application/json-seq").run()
			.assertStatus(200).getContent().asString();
		assertTrue(c.contains("\"name\":\"q\""), c);
	}

	@Test void h02_sseCtAlreadyContainsEventStream_notOverridden() throws Exception {
		var r = CH.get("/sseCtAlreadyEventStream").run().assertStatus(200);
		// Content-Type should still contain "event-stream" and "already-set" data should arrive.
		r.assertHeader("Content-Type").isContains("event-stream");
		assertTrue(r.getContent().asString().contains("data: already-set"));
	}

	@Test void h03_sseShapeWithNonEventStreamCt_ctOverridden() throws Exception {
		// Accept: text/event-stream selects SSE; ct="text/plain" does not contain "event-stream"
		// so prepareStreamingHeaders overwrites it (line 230 true branch: ct != null && !ct.contains).
		var r = CH.get("/sseCtNonEventStream").header("Accept", "text/event-stream").run().assertStatus(200);
		r.assertHeader("Content-Type").isContains("event-stream");
		assertTrue(r.getContent().asString().contains("data: overridden"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: resolveShape — json5l keyword in Accept header (line 315 third || branch).
	//    syncTimeoutMillis — op-level timeout positive (line 321 false branch: t > 0, restContext lookup skipped).
	//    Both branches are covered by sending Accept: application/json5l to an endpoint with
	//    asyncTimeoutMillis="5000"; the op-level timeout fires inside the sync fallback path.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class I {
		@RestOp(method="GET", path="/json5l", asyncTimeoutMillis="5000")
		public Flow.Publisher<Pojo> json5l() {
			return ListPublisher.of(new Pojo("r", 5));
		}
	}

	private static final MockRestClient CI = MockRestClient.buildLax(I.class);

	@Test void i01_acceptJson5l_triggersNdjsonShape_withOpLevelTimeout() throws Exception {
		// Covers line 315 (json5l branch) and line 321 false (op-level asyncTimeoutMillis=5000 > 0).
		var c = CI.get("/json5l").header("Accept", "application/json5l").run()
			.assertStatus(200).getContent().asString();
		assertTrue(c.contains("\"name\":\"r\""), c);
	}
}
