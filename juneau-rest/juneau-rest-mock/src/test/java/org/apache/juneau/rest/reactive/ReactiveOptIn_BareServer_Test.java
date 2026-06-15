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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that a <b>bare</b> {@code juneau-rest-server} (no {@code juneau-rest-server-reactive} /
 * {@code juneau-rest-server-reactive-reactor} module on the classpath) has <b>zero</b> reactive behavior.
 *
 * <p>
 * This module's test classpath intentionally has no reactive module, so {@code RestContext}'s
 * {@link java.util.ServiceLoader}-based response-processor discovery finds no provider and
 * {@code ReactiveResponseProcessor} is never added to the chain. A {@code @RestOp} returning a
 * {@link java.util.concurrent.Flow.Publisher Flow.Publisher} is therefore treated as an ordinary POJO
 * return value and serialized through the normal serializer chain &mdash; it is NOT collected into a
 * JSON array (the reactive buffer shape) and NOT streamed.
 *
 * <p>
 * The positive activation tests live in {@code juneau-integration-tests} (where the reactive module IS on the
 * classpath); this negative test cannot live there because the module's presence auto-activates the
 * processor for every resource. See {@code FINISHED-119}/{@code FINISHED-120} for the opt-in design.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures are intentionally held/unassigned; lifecycle is managed by the test framework.
})
class ReactiveOptIn_BareServer_Test {

	public static final class Pojo {
		public final String name;
		public final int value;
		public Pojo(String name, int value) { this.name = name; this.value = value; }
	}

	/** Minimal JDK Flow.Publisher; on a bare server it is never subscribed to (no reactive processor). */
	static final class ListPublisher<T> implements Flow.Publisher<T> {
		@Override public void subscribe(Flow.Subscriber<? super T> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				@Override public void request(long n) { /* no-op */ }
				@Override public void cancel() { /* no-op */ }
			});
		}
	}

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/flux")
		public Flow.Publisher<Pojo> flux() {
			return new ListPublisher<>();
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_bareServer_doesNotProcessFlowPublisherReactively() throws Exception {
		String content;
		try (var req = CA.get("/flux")) {
			req.accept("application/json");
			try (var res = req.run()) {
				content = res.getContent().asString();
			}
		}
		// Reactive (buffer-shape) handling would have subscribed to the publisher and produced a JSON
		// array of the emitted Pojo elements. With no reactive module on the classpath the publisher is
		// instead serialized as a plain (property-less) bean — so the element payload must be absent.
		assertFalse(content.contains("\"name\""),
			"Bare juneau-rest-server must not reactively process Flow.Publisher returns. Body was: " + content);
		assertFalse(content.startsWith("["),
			"Bare juneau-rest-server must not buffer a Flow.Publisher into a JSON array. Body was: " + content);
	}
}
