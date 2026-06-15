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
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.reactive.reactor.*;
import org.junit.jupiter.api.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import reactor.core.publisher.*;

/**
 * End-to-end tests for the {@code juneau-rest-server-reactive-reactor} bridge adapters
 * ({@link ReactorReactiveAdapter}, {@link RxJavaReactiveAdapter},
 * {@link ReactiveStreamsPublisherAdapter}) wired through the core
 * {@link org.apache.juneau.rest.server.server.reactive.ReactiveResponseProcessor} via ServiceLoader.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ReactiveBridge_Test extends TestBase {

	public static final class Pojo {
		public final String name;
		public final int value;
		public Pojo(String name, int value) { this.name = name; this.value = value; }
	}

	/** Minimal trampolined Reactive-Streams publisher, used to exercise the generic Publisher adapter. */
	static final class RsListPublisher implements org.reactivestreams.Publisher<Pojo> {
		private final List<Pojo> items;
		RsListPublisher(List<Pojo> items) { this.items = items; }

		@Override public void subscribe(org.reactivestreams.Subscriber<? super Pojo> sub) {
			sub.onSubscribe(new org.reactivestreams.Subscription() {
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

	@Rest(serializers = JsonSerializer.class)
	public static class A {

		// ---- Reactor ----
		@RestGet("/mono")
		public Mono<Pojo> mono() { return Mono.just(new Pojo("mono", 1)); }

		@RestGet("/monoEmpty")
		public Mono<Pojo> monoEmpty() { return Mono.empty(); }

		@RestGet("/flux")
		public Flux<Pojo> flux() { return Flux.just(new Pojo("a", 1), new Pojo("b", 2)); }

		@RestGet("/fluxSse")
		public Flux<SseEvent> fluxSse(RestResponse res) {
			res.setContentType("text/event-stream");
			return Flux.just(new SseEvent("tick", "one"), new SseEvent("tick", "two"));
		}

		// ---- RxJava 3 ----
		@RestGet("/single")
		public Single<Pojo> single() { return Single.just(new Pojo("single", 3)); }

		@RestGet("/maybe")
		public Maybe<Pojo> maybe() { return Maybe.just(new Pojo("maybe", 4)); }

		@RestGet("/maybeEmpty")
		public Maybe<Pojo> maybeEmpty() { return Maybe.empty(); }

		@RestGet("/completable")
		public Completable completable() { return Completable.complete(); }

		@RestGet("/flowable")
		public Flowable<Pojo> flowable() { return Flowable.just(new Pojo("x", 5), new Pojo("y", 6)); }

		@RestGet("/observable")
		public Observable<Pojo> observable() { return Observable.just(new Pojo("o", 7), new Pojo("p", 8)); }

		// ---- Generic Reactive-Streams Publisher ----
		@RestGet("/publisher")
		public org.reactivestreams.Publisher<Pojo> publisher() {
			return new RsListPublisher(List.of(new Pojo("pub", 9)));
		}
	}

	private static final MockRestClient C = MockRestClient.buildLax(A.class);

	// ---- Reactor ----

	@Test void a01_mono_single() throws Exception {
		C.get("/mono").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"mono\"", "\"value\":1");
	}

	@Test void a02_monoEmpty_nullBody() throws Exception {
		C.get("/monoEmpty").accept("application/json").run().assertStatus(200);
	}

	@Test void a03_flux_buffersToArray() throws Exception {
		C.get("/flux").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"a\"", "\"name\":\"b\"");
	}

	@Test void a04_flux_streamsSse() throws Exception {
		var c = C.get("/fluxSse").run().assertStatus(200).getContent().asString();
		assertTrue(c.contains("event: tick"), c);
		assertTrue(c.contains("data: one"), c);
		assertTrue(c.contains("data: two"), c);
	}

	// ---- RxJava 3 ----

	@Test void b01_single() throws Exception {
		C.get("/single").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"single\"", "\"value\":3");
	}

	@Test void b02_maybe() throws Exception {
		C.get("/maybe").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"maybe\"", "\"value\":4");
	}

	@Test void b03_maybeEmpty_nullBody() throws Exception {
		C.get("/maybeEmpty").accept("application/json").run().assertStatus(200);
	}

	@Test void b04_completable_nullBody() throws Exception {
		C.get("/completable").accept("application/json").run().assertStatus(200);
	}

	@Test void b05_flowable_buffersToArray() throws Exception {
		C.get("/flowable").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"x\"", "\"name\":\"y\"");
	}

	@Test void b06_observable_buffersToArray() throws Exception {
		C.get("/observable").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"o\"", "\"name\":\"p\"");
	}

	// ---- Generic Publisher ----

	@Test void c01_reactiveStreamsPublisher_buffersToArray() throws Exception {
		C.get("/publisher").accept("application/json").run().assertStatus(200)
			.assertContent().isContains("\"name\":\"pub\"", "\"value\":9");
	}
}
