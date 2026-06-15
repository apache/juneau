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
 * {@link java.util.concurrent.Flow.Publisher Flow.Publisher} return type (no external reactive
 * library). This is the MAYBE-120 spine: {@code Flow.Publisher<SseEvent>} SSE streaming plus the
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
	 * Trampolined synchronous {@link java.util.concurrent.Flow.Publisher Flow.Publisher} that replays a
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
}
