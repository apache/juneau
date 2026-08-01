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
package org.apache.juneau.rest.server.tracing.otel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;

import io.opentelemetry.api.trace.propagation.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.sdk.*;
import io.opentelemetry.sdk.testing.exporter.*;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.trace.export.*;

/**
 * Exercises {@link RestRequestTextMapGetter} against a live {@link RestRequest} captured inside a
 * {@code @RestOp} handler so that header names, present/absent semantics, and the OpenTelemetry
 * {@code TextMapGetter} contract are all validated against the real request shape.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class RestRequestTextMapGetter_Test extends TestBase {

	private static final AtomicReference<RestRequest> CAPTURED = new AtomicReference<>();

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/capture")
		public String capture(RestRequest req) {
			CAPTURED.set(req);
			return "ok";
		}
	}

	private static final MockRestClient C = MockRestClient.buildLax(A.class);

	@BeforeEach
	void resetCaptured() { CAPTURED.set(null); }

	@Test void a01_get_returnsPresentHeaderValue() throws Exception {
		C.get("/capture")
			.header("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
			.run().assertStatus(200);

		var req = CAPTURED.get();
		assertNotNull(req);
		var g = RestRequestTextMapGetter.INSTANCE;
		assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", g.get(req, "traceparent"));
	}

	@Test void a02_get_returnsNullForAbsentHeader() throws Exception {
		C.get("/capture").run().assertStatus(200);
		var req = CAPTURED.get();
		assertNull(RestRequestTextMapGetter.INSTANCE.get(req, "x-not-present"));
	}

	@Test void a03_get_isCaseInsensitiveForHttpHeaders() throws Exception {
		C.get("/capture").header("X-Foo", "bar").run().assertStatus(200);
		var req = CAPTURED.get();
		// HTTP header names are case-insensitive; RequestHeader lookup must honor that.
		assertEquals("bar", RestRequestTextMapGetter.INSTANCE.get(req, "x-foo"));
		assertEquals("bar", RestRequestTextMapGetter.INSTANCE.get(req, "X-FOO"));
	}

	@Test void a04_get_nullCarrier_returnsNull() {
		assertNull(RestRequestTextMapGetter.INSTANCE.get(null, "anything"));
	}

	@Test void a05_keys_includesSentHeader() throws Exception {
		C.get("/capture").header("X-Trace-Id", "abc").run().assertStatus(200);
		var req = CAPTURED.get();
		var keys = RestRequestTextMapGetter.INSTANCE.keys(req);
		assertNotNull(keys);
		boolean found = false;
		for (var k : keys) {
			if ("X-Trace-Id".equalsIgnoreCase(k)) { found = true; break; }
		}
		assertTrue(found, "keys() should include sent header name (any case)");
	}

	@Test void a06_singleton_isStable() {
		assertSame(RestRequestTextMapGetter.INSTANCE, RestRequestTextMapGetter.INSTANCE);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Regression — this getter remains the sole HTTP-header source OtelTracerHook falls back to for an
	//    ordinary request that has no recognized non-HTTP TraceContextCarrier (no registered
	//    TraceContextExtractor at all). Carrier-aware composite extraction (a registered extractor
	//    supplying a non-HTTP carrier) is exercised in OtelTracerHook_Test.
	// -----------------------------------------------------------------------------------------------------------------

	static final InMemorySpanExporter B_EXPORTER = InMemorySpanExporter.create();
	static final OpenTelemetrySdk B_OTEL_SDK = OpenTelemetrySdk.builder()
		.setTracerProvider(SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(B_EXPORTER))
			.build())
		.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
		.build();

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(B_OTEL_SDK); }

		@RestGet("/op")
		public String op() { return "ok"; }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_noExtractorRegistered_httpHeaderStillFallsThroughThisGetter() throws Exception {
		B_EXPORTER.reset();
		String traceId = "0af7651916cd43dd8448eb211c80319c";
		String spanId = "b7ad6b7169203331";
		CB.get("/op").header("traceparent", "00-" + traceId + "-" + spanId + "-01").run().assertStatus(200);

		var spans = B_EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals(traceId, spans.get(0).getTraceId(), "with no TraceContextExtractor registered, the HTTP header read through this getter is the only trace-context source");
	}
}
