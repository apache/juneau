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
package org.apache.juneau.rest.tracing.otel;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.tracing.*;
import org.junit.jupiter.api.*;

import io.opentelemetry.api.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.trace.propagation.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.sdk.*;
import io.opentelemetry.sdk.testing.exporter.*;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.trace.export.*;

/**
 * End-to-end tests for {@link OtelTracerHook} using the OpenTelemetry SDK's in-memory span exporter
 * to capture and assert against real {@link io.opentelemetry.sdk.trace.data.SpanData SpanData}
 * produced by the bridge during {@code @RestOp} invocations.
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class OtelTracerHook_Test extends TestBase {

	// Static init so the SDK is wired before MockRestClient.buildLax() in the @Rest classes runs.
	static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();
	static final OpenTelemetrySdk OTEL_SDK = OpenTelemetrySdk.builder()
		.setTracerProvider(SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(EXPORTER))
			.build())
		.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
		.build();

	@BeforeEach
	void resetExporter() {
		EXPORTER.reset();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Construction surface.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullOpenTelemetry_throws() {
		assertThrows(IllegalArgumentException.class, () -> new OtelTracerHook((OpenTelemetry) null));
	}

	@Test void a02_nullTracer_throws() {
		assertThrows(IllegalArgumentException.class, () -> new OtelTracerHook(null, W3CTraceContextPropagator.getInstance()));
	}

	@Test void a03_nullPropagator_throws() {
		var t = OTEL_SDK.getTracer("t");
		assertThrows(IllegalArgumentException.class, () -> new OtelTracerHook(t, null));
	}

	@Test void a04_accessors_returnInjectedInstances() {
		var t = OTEL_SDK.getTracer("t");
		var p = W3CTraceContextPropagator.getInstance();
		var hook = new OtelTracerHook(t, p);
		assertSame(t, hook.getTracer());
		assertSame(p, hook.getPropagator());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: End-to-end happy path — one SERVER span per request, with HTTP semantic attributes set.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@RestGet("/users/{id}")
		public String get(@org.apache.juneau.http.annotation.Path String id) { return "u:" + id; }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_happyPath_serverSpanWithHttpSemconvAttributes() throws Exception {
		CB.get("/users/42").run().assertStatus(200);
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals(SpanKind.SERVER, s.getKind());
		assertEquals("GET", s.getName());
		assertEquals("GET", s.getAttributes().get(OtelTracerHook.ATTR_HTTP_REQUEST_METHOD));
		assertEquals(Long.valueOf(200), s.getAttributes().get(OtelTracerHook.ATTR_HTTP_RESPONSE_STATUS_CODE));
		assertEquals("/users/{id}", s.getAttributes().get(OtelTracerHook.ATTR_HTTP_ROUTE));
		assertEquals(StatusCode.UNSET, s.getStatus().getStatusCode(), "2xx maps to UNSET per OTel HTTP semconv");
	}

	@Test void b02_pathTemplate_notRawUri() throws Exception {
		CB.get("/users/aaa").run().assertStatus(200);
		CB.get("/users/bbb").run().assertStatus(200);
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(2, spans.size());
		assertEquals(spans.get(0).getAttributes().get(OtelTracerHook.ATTR_HTTP_ROUTE),
			spans.get(1).getAttributes().get(OtelTracerHook.ATTR_HTTP_ROUTE),
			"different concrete URIs share the same http.route attribute");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Exception path — error recorded, span status set to ERROR, exception.type set.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@RestGet("/boom")
		public String boom() { throw new IllegalStateException("kaboom"); }
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_exceptionPath_spanCarriesErrorStatusAndExceptionType() throws Exception {
		CC.get("/boom").run().assertStatus(500);
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals(Long.valueOf(500), s.getAttributes().get(OtelTracerHook.ATTR_HTTP_RESPONSE_STATUS_CODE));
		assertEquals(StatusCode.ERROR, s.getStatus().getStatusCode());
		assertEquals("IllegalStateException", s.getAttributes().get(OtelTracerHook.ATTR_EXCEPTION_TYPE));
		assertFalse(s.getEvents().isEmpty(), "recordException should add an event with the throwable's stack trace");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: W3C traceparent propagation — incoming traceparent continues an existing trace.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_incomingTraceparent_continuesTrace() throws Exception {
		// Build a valid W3C traceparent: 00-<32 hex traceId>-<16 hex spanId>-01
		String traceId = "0af7651916cd43dd8448eb211c80319c";
		String parentSpanId = "b7ad6b7169203331";
		String traceparent = "00-" + traceId + "-" + parentSpanId + "-01";

		CB.get("/users/9").header("traceparent", traceparent).run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals(traceId, s.getTraceId(), "server span should inherit the incoming trace id");
		assertEquals(parentSpanId, s.getParentSpanId(), "server span should be a child of the incoming span");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Off-by-default — no @Bean TracerHook means no spans are emitted by the framework.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void e01_noTracerBean_noSpansEmitted() throws Exception {
		CE.get("/ping").run().assertStatus(200);
		assertEquals(0, EXPORTER.getFinishedSpanItems().size(), "without a @Bean TracerHook, no spans reach the SDK exporter");
	}
}
