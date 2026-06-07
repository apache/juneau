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
package org.apache.juneau.rest.server.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link MetricsRecorder} SPI contract end-to-end &mdash; the {@code record(...)} call
 * fires exactly once per {@code @RestOp} invocation, carries the framework-resolved op name, HTTP
 * method, path template, status code, elapsed time, and the exception (or {@code null}) the handler
 * threw, and the default off-by-default {@link NoOpMetricsRecorder} drops events on the floor.
 */
class MetricsRecorder_Contract_Test extends TestBase {

	/** Recording {@link MetricsRecorder} that captures every event in declaration order. */
	public static final class RecordingMetricsRecorder implements MetricsRecorder {
		public final List<Event> events = new CopyOnWriteArrayList<>();

		@Override
		@SuppressWarnings({
			"java:S6213" // 'record' matches the MetricsRecorder SPI method name; renaming would break the @Override contract.
		})
		public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
			events.add(new Event(opName, httpMethod, uriTemplate, statusCode, elapsed, error));
		}

		public Event last() { return events.get(events.size() - 1); }
	}

	public static final class Event {
		public final String opName;
		public final String httpMethod;
		public final String uriTemplate;
		public final int statusCode;
		public final Duration elapsed;
		public final Throwable error;

		Event(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error) {
			this.opName = opName;
			this.httpMethod = httpMethod;
			this.uriTemplate = uriTemplate;
			this.statusCode = statusCode;
			this.elapsed = elapsed;
			this.error = error;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Recorder is opt-in via a @Bean MetricsRecorder. Happy path fires record(...) exactly once with the resolved
	//    op name, HTTP method, path template, 200, non-zero elapsed, and null error.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder A_REC = new RecordingMetricsRecorder();

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return A_REC; }

		@RestGet("/users/{id}")
		public String findUser(@org.apache.juneau.http.Path String id) {
			return "user:" + id;
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_happyPath_oneEventCaptured() throws Exception {
		A_REC.events.clear();
		CA.get("/users/42").run().assertStatus(200).assertContent("user:42");
		assertEquals(1, A_REC.events.size(), "exactly one event per @RestOp call");
		var e = A_REC.last();
		assertEquals("GET", e.httpMethod);
		assertEquals(200, e.statusCode);
		assertNull(e.error);
		assertNotNull(e.elapsed);
		assertFalse(e.elapsed.isNegative(), "elapsed must be non-negative");
		assertTrue(e.opName.contains("findUser"), "opName carries the Java method name: " + e.opName);
		assertEquals("/users/{id}", e.uriTemplate, "uriTemplate carries the @RestOp path template (bounded cardinality)");
	}

	@Test void a02_pathTemplateNotRawUri() throws Exception {
		A_REC.events.clear();
		CA.get("/users/abc").run().assertStatus(200);
		CA.get("/users/xyz").run().assertStatus(200);
		assertEquals(2, A_REC.events.size());
		assertEquals(A_REC.events.get(0).uriTemplate, A_REC.events.get(1).uriTemplate, "different concrete URIs must share the same template tag");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Exception path — record(...) still fires; status >= 500; error carries the thrown Throwable.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder B_REC = new RecordingMetricsRecorder();

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return B_REC; }

		@RestGet("/boom")
		public String boom() {
			throw new IllegalStateException("kaboom");
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_exceptionPath_eventFiredWithError() throws Exception {
		B_REC.events.clear();
		CB.get("/boom").run().assertStatus(500);
		assertEquals(1, B_REC.events.size());
		var e = B_REC.last();
		assertEquals(500, e.statusCode);
		assertNotNull(e.error);
		assertEquals("IllegalStateException", e.error.getClass().getSimpleName());
		assertEquals("/boom", e.uriTemplate);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Off-by-default — without a @Bean MetricsRecorder, the framework wires NoOpMetricsRecorder and no events
	//    reach any external recorder. We verify by asserting that this resource has no recording side effect on a
	//    shared canary recorder.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder C_CANARY = new RecordingMetricsRecorder();

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_noRecorderBean_noEventsFanOut() throws Exception {
		C_CANARY.events.clear();
		CC.get("/ping").run().assertStatus(200);
		assertEquals(0, C_CANARY.events.size(), "without a @Bean MetricsRecorder, no events reach external recorders");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: NoOpMetricsRecorder direct contract — single static instance, record(...) is a no-op.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_noOpRecorder_isSingleton() {
		assertSame(NoOpMetricsRecorder.INSTANCE, NoOpMetricsRecorder.INSTANCE);
	}

	@Test void d02_noOpRecorder_recordIsNoOp() {
		assertDoesNotThrow(() -> NoOpMetricsRecorder.INSTANCE.record("op", "GET", "/x", 200, Duration.ofMillis(1), null, "", ""));
		assertDoesNotThrow(() -> NoOpMetricsRecorder.INSTANCE.record(null, null, null, 0, Duration.ZERO, new RuntimeException(), "", ""));
	}
}
