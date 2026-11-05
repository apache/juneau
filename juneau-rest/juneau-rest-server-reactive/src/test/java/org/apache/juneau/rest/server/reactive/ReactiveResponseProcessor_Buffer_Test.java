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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests the bounded buffer of {@link ReactiveResponseProcessor}: the element limit, the overflow failure, and
 * subscription cancellation on overflow and on timeout.
 */
@SuppressWarnings({
	"resource" // Test doubles; nothing here holds an OS resource.
})
class ReactiveResponseProcessor_Buffer_Test extends TestBase {

	/** Hard stop for the test publishers so a regression cannot make the test run unbounded. */
	private static final int GUARD = 100_000;

	/** Publisher that emits {@code count} integers (an unbounded run when {@code count} is negative, capped by {@link #GUARD}). */
	static final class CountingPublisher implements Flow.Publisher<Integer> {
		final long count;
		final AtomicBoolean cancelled = new AtomicBoolean();
		final AtomicInteger emitted = new AtomicInteger();

		CountingPublisher(long count) { this.count = count; }

		@Override public void subscribe(Flow.Subscriber<? super Integer> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				private long demand;
				private boolean draining;

				@Override public void request(long n) {
					demand += n;
					if (draining)
						return;
					draining = true;
					try {
						while (! cancelled.get() && demand > 0 && emitted.get() < GUARD && (count < 0 || emitted.get() < count)) {
							demand--;
							sub.onNext(emitted.getAndIncrement());
						}
						if (! cancelled.get() && count >= 0 && emitted.get() >= count)
							sub.onComplete();
					} finally {
						draining = false;
					}
				}

				@Override public void cancel() { cancelled.set(true); }
			});
		}
	}

	/** Publisher that subscribes but never emits. */
	static final class SilentPublisher implements Flow.Publisher<Integer> {
		final AtomicBoolean cancelled = new AtomicBoolean();

		@Override public void subscribe(Flow.Subscriber<? super Integer> sub) {
			sub.onSubscribe(new Flow.Subscription() {
				@Override public void request(long n) { /* never emits */ }
				@Override public void cancel() { cancelled.set(true); }
			});
		}
	}

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		static volatile CountingPublisher pub;
		static volatile SilentPublisher silent;

		@RestGet("/counting")
		public Flow.Publisher<Integer> counting() { return pub; }

		@RestOp(method = "GET", path = "/silent", asyncTimeoutMillis = "50")
		public Flow.Publisher<Integer> silent() { return silent; }
	}

	// MockRestClient caches one RestContext per resource class, so each cap value needs its own class.
	@Rest public static class A1 extends A {}
	@Rest public static class A3 extends A {}
	@Rest public static class A50 extends A {}

	private static MockRestClient client(String max) {
		if (max == null)
			return MockRestClient.buildLax(A.class);
		// The properties bean is bound on the first request, so the setting stays in place until the test ends.
		Settings.get().setLocal("ReactiveResponse.maxBufferedElements", max);
		return MockRestClient.buildLax(switch (max) {
			case "1" -> A1.class;
			case "3" -> A3.class;
			default -> A50.class;
		});
	}

	@AfterEach void clearSetting() {
		Settings.get().unsetLocal("ReactiveResponse.maxBufferedElements");
	}

	@Test void a01_underDefaultCap_serializesAsBefore() throws Exception {
		A.pub = new CountingPublisher(3);
		client(null).get("/counting").accept("application/json").run()
			.assertStatus(200)
			.assertContent().is("[0,1,2]");
		assertFalse(A.pub.cancelled.get());
	}

	@Test void a02_exactlyAtCap_succeeds() throws Exception {
		A.pub = new CountingPublisher(3);
		client("3").get("/counting").accept("application/json").run()
			.assertStatus(200)
			.assertContent().is("[0,1,2]");
	}

	@Test void a03_overCap_failsWith500AndCancels() throws Exception {
		A.pub = new CountingPublisher(10);
		var r = client("3").get("/counting").accept("application/json").run();
		r.assertStatus(500);
		var body = r.getContent().asString();
		assertTrue(body.contains("limit of 3 buffered elements"), body);
		assertTrue(A.pub.cancelled.get());
		assertEquals(4, A.pub.emitted.get());
	}

	@Test void a04_infinitePublisher_isStoppedAtCap() throws Exception {
		A.pub = new CountingPublisher(-1);
		client("50").get("/counting").accept("application/json").run().assertStatus(500);
		assertTrue(A.pub.cancelled.get());
		assertEquals(51, A.pub.emitted.get());
	}

	@Test void a05_infinitePublisher_defaultCapIsTenThousand() throws Exception {
		A.pub = new CountingPublisher(-1);
		client(null).get("/counting").accept("application/json").run().assertStatus(500);
		assertTrue(A.pub.cancelled.get());
		assertEquals(10_001, A.pub.emitted.get());
	}

	@Test void a06_smallCapSetting_isHonored() throws Exception {
		A.pub = new CountingPublisher(2);
		client("1").get("/counting").accept("application/json").run().assertStatus(500);
		assertTrue(A.pub.cancelled.get());
	}

	@Test void a07_timeout_cancelsSubscription() throws Exception {
		A.silent = new SilentPublisher();
		client(null).get("/silent").accept("application/json").run().assertStatus(504);
		assertTrue(A.silent.cancelled.get());
	}
}
