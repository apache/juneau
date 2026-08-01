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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;

import io.opentelemetry.api.*;
import io.opentelemetry.api.baggage.*;
import io.opentelemetry.api.baggage.propagation.*;
import io.opentelemetry.api.common.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.trace.propagation.*;
import io.opentelemetry.context.*;
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
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class OtelTracerHook_Test extends TestBase {

	// Static init so the SDK is wired before MockRestClient.buildLax() in the @Rest classes runs.
	static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();
	static final OpenTelemetrySdk OTEL_SDK = OpenTelemetrySdk.builder()
		.setTracerProvider(SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(EXPORTER))
			.build())
		.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
		.build();

	// A second SDK wired with a baggage-aware composite propagator, for the baggage-precedence and
	// baggage-inject tests below. Kept separate from OTEL_SDK so the non-baggage tests' propagator
	// configuration (traceparent/tracestate only) is unaffected.
	static final InMemorySpanExporter BAGGAGE_EXPORTER = InMemorySpanExporter.create();
	static final OpenTelemetrySdk OTEL_SDK_BAGGAGE = OpenTelemetrySdk.builder()
		.setTracerProvider(SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(BAGGAGE_EXPORTER))
			.build())
		.setPropagators(ContextPropagators.create(TextMapPropagator.composite(
			W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
		.build();

	@BeforeEach
	void resetExporter() {
		EXPORTER.reset();
		BAGGAGE_EXPORTER.reset();
	}

	/**
	 * Minimal {@link TraceContextCarrier} backed by an in-memory map &mdash; stands in for a future
	 * MCP {@code params._meta} carrier without depending on MCP.
	 */
	private static final class MapCarrier implements TraceContextCarrier {
		final Map<String,String> map;
		MapCarrier(Map<String,String> map) { this.map = map; }
		@Override public String get(String key) { return map.get(key); }
		@Override public Iterable<String> keys() { return map.keySet(); }
		@Override public void set(String key, String value) { map.put(key, value); }
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

	// Captured only to exercise the single-arg startSpan(RestRequest) overload directly below — this
	// resource registers no TracerHook bean, so its own dispatch never reaches OtelTracerHook.
	private static final AtomicReference<RestRequest> A05_CAPTURED = new AtomicReference<>();

	@Rest
	public static class A05 extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/capture")
		public String capture(RestRequest req) { A05_CAPTURED.set(req); return "ok"; }
	}

	private static final MockRestClient CA05 = MockRestClient.buildLax(A05.class);

	@Test void a05_startSpanSingleArg_delegatesToThreeArgOverloadWithNullCarrierAndDefaultOperation() throws Exception {
		CA05.get("/capture").run().assertStatus(200);
		var req = A05_CAPTURED.get();
		assertNotNull(req);

		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan(req)) {
			scope.setStatusCode(200);
		}
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size(), "A05 registers no TracerHook bean, so this is the only span — from the direct call below");
		assertEquals("GET", spans.get(0).getName(), "startSpan(request) must still name the span after the HTTP method (TraceOperation.DEFAULT has no override)");
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
		public String get(@org.apache.juneau.http.Path String id) { return "u:" + id; }
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

	// -----------------------------------------------------------------------------------------------------------------
	// H: Composite carrier extraction — a non-HTTP TraceContextCarrier (standing in for a future MCP
	//    params._meta carrier) takes precedence over the HTTP traceparent/tracestate/baggage headers when
	//    present, and falls back to the HTTP header when the carrier has nothing for that key. Invalid
	//    carrier values must not fail dispatch.
	// -----------------------------------------------------------------------------------------------------------------

	private static final String META_TRACE_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	private static final String META_SPAN_ID = "aaaaaaaaaaaaaaaa";
	private static final String HTTP_TRACE_ID = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
	private static final String HTTP_SPAN_ID = "bbbbbbbbbbbbbbbb";
	private static final String HTTP_TRACEPARENT = "00-" + HTTP_TRACE_ID + "-" + HTTP_SPAN_ID + "-01";

	private static final AtomicReference<TraceContextCarrier> H_CARRIER = new AtomicReference<>();

	public static final class H_Extractor implements TraceContextExtractor {
		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			return Optional.ofNullable(H_CARRIER.get());
		}
	}

	@Rest
	public static class H extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return new H_Extractor(); }

		@RestGet("/op")
		public String op() { return "ok"; }
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_metadataTraceparent_winsOverHttpHeader() throws Exception {
		H_CARRIER.set(new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "00-" + META_TRACE_ID + "-" + META_SPAN_ID + "-01"))));

		CH.get("/op").header("traceparent", HTTP_TRACEPARENT).run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals(META_TRACE_ID, spans.get(0).getTraceId(), "params._meta traceparent must win over the HTTP header");
		assertEquals(META_SPAN_ID, spans.get(0).getParentSpanId());
	}

	@Test void h02_absentMetadataTraceparent_fallsBackToHttpHeader() throws Exception {
		// Extractor recognizes a carrier (e.g. _meta was present), but it carries no trace fields.
		H_CARRIER.set(new MapCarrier(new LinkedHashMap<>()));

		CH.get("/op").header("traceparent", HTTP_TRACEPARENT).run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals(HTTP_TRACE_ID, spans.get(0).getTraceId(), "absent metadata traceparent must fall back to the HTTP header");
		assertEquals(HTTP_SPAN_ID, spans.get(0).getParentSpanId());
	}

	@Test void h03_noCarrierRecognized_httpHeaderStillApplies() throws Exception {
		H_CARRIER.set(null);

		CH.get("/op").header("traceparent", HTTP_TRACEPARENT).run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals(HTTP_TRACE_ID, spans.get(0).getTraceId(), "no recognized carrier must extract HTTP headers only");
	}

	@Test void h04_metadataTracestate_travelsWithWinningTraceparent() throws Exception {
		H_CARRIER.set(new MapCarrier(new LinkedHashMap<>(Map.of(
			"traceparent", "00-" + META_TRACE_ID + "-" + META_SPAN_ID + "-01",
			"tracestate", "meta=1"))));

		CH.get("/op")
			.header("traceparent", HTTP_TRACEPARENT)
			.header("tracestate", "http=1")
			.run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var traceState = spans.get(0).getSpanContext().getTraceState();
		assertEquals("1", traceState.get("meta"), "metadata tracestate must win alongside its winning traceparent");
		assertNull(traceState.get("http"), "the HTTP header's tracestate must not leak through when metadata wins");
	}

	@Test void h05_baggage_metadataWinsOverHttpHeader() throws Exception {
		BAGGAGE_H_CARRIER.set(new MapCarrier(new LinkedHashMap<>(Map.of("baggage", "userId=meta-alice"))));
		BAGGAGE_H_SEEN.set(null);

		CHB.get("/op").header("baggage", "userId=http-bob").run().assertStatus(200);

		assertEquals("userId=meta-alice", BAGGAGE_H_SEEN.get(), "metadata baggage must win over the HTTP baggage header");
	}

	@Test void h06_baggage_absentFromMetadata_fallsBackToHttpHeader() throws Exception {
		// Extractor recognizes a carrier, but it carries no baggage key.
		BAGGAGE_H_CARRIER.set(new MapCarrier(new LinkedHashMap<>()));
		BAGGAGE_H_SEEN.set(null);

		CHB.get("/op").header("baggage", "userId=http-bob").run().assertStatus(200);

		assertEquals("userId=http-bob", BAGGAGE_H_SEEN.get(), "absent metadata baggage must fall back to the HTTP header");
	}

	@Test void h07_invalidMetadataTraceparent_fallsBackWithoutFailingDispatch() throws Exception {
		H_CARRIER.set(new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "not-a-valid-traceparent"))));

		CH.get("/op").header("traceparent", HTTP_TRACEPARENT).run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals(HTTP_TRACE_ID, spans.get(0).getTraceId(), "an invalid metadata value must fall back to the valid HTTP header per propagator contract");
	}

	// Baggage fixtures use OTEL_SDK_BAGGAGE (a separate propagator/exporter) since OTEL_SDK above is not
	// configured with a baggage propagator.
	private static final AtomicReference<TraceContextCarrier> BAGGAGE_H_CARRIER = new AtomicReference<>();
	private static final AtomicReference<String> BAGGAGE_H_SEEN = new AtomicReference<>();

	public static final class HB_Extractor implements TraceContextExtractor {
		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			return Optional.ofNullable(BAGGAGE_H_CARRIER.get());
		}
	}

	@Rest
	public static class HB extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK_BAGGAGE); }

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return new HB_Extractor(); }

		@RestGet("/op")
		public String op(RestRequest req) {
			BAGGAGE_H_SEEN.set(req.getAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE).as(String.class).orElse(null));
			return "ok";
		}
	}

	private static final MockRestClient CHB = MockRestClient.buildLax(HB.class);

	// -----------------------------------------------------------------------------------------------------------------
	// I: Carrier-aware span naming/attributes — a TraceOperation with a span name/attributes overrides the
	//    HTTP-derived default; TraceOperation.DEFAULT leaves ordinary HTTP behavior unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	private static final AtomicReference<TraceOperation> I_OPERATION = new AtomicReference<>(TraceOperation.DEFAULT);

	public static final class I_Extractor implements TraceContextExtractor {
		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			return Optional.empty();
		}

		@Override
		public TraceOperation operation(RestRequest request, Object[] resolvedArguments) {
			return I_OPERATION.get();
		}
	}

	@Rest
	public static class I extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return new I_Extractor(); }

		@RestGet("/rpc")
		public String rpc() { return "ok"; }
	}

	private static final MockRestClient CI = MockRestClient.buildLax(I.class);

	@Test void i01_operationSpanNameAndAttributes_appliedToSpan() throws Exception {
		var attrs = new LinkedHashMap<String,String>();
		attrs.put(TraceOperation.ATTR_MCP_METHOD_NAME, "tools/call");
		attrs.put(TraceOperation.ATTR_MCP_PROTOCOL_VERSION, "2026-07-28");
		attrs.put(TraceOperation.ATTR_JSONRPC_REQUEST_ID, "7");
		attrs.put(TraceOperation.ATTR_GEN_AI_TOOL_NAME, "echo");
		attrs.put(TraceOperation.ATTR_GEN_AI_OPERATION_NAME, "execute_tool");
		I_OPERATION.set(TraceOperation.of("tools/call echo", attrs));

		CI.get("/rpc").run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals("tools/call echo", s.getName());
		assertEquals("tools/call", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_MCP_METHOD_NAME)));
		assertEquals("2026-07-28", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_MCP_PROTOCOL_VERSION)));
		assertEquals("7", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_JSONRPC_REQUEST_ID)));
		assertEquals("echo", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_GEN_AI_TOOL_NAME)));
		assertEquals("execute_tool", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_GEN_AI_OPERATION_NAME)));
		// Ordinary HTTP semconv attributes remain present alongside the MCP/GenAI attributes.
		assertEquals("GET", s.getAttributes().get(OtelTracerHook.ATTR_HTTP_REQUEST_METHOD));
	}

	@Test void i02_defaultOperation_ordinaryHttpNameAndAttributesUnchanged() throws Exception {
		I_OPERATION.set(TraceOperation.DEFAULT);

		CI.get("/rpc").run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals("GET", s.getName(), "TraceOperation.DEFAULT must not override the HTTP-derived span name");
		assertNull(s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_MCP_METHOD_NAME)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: Context activation — the carrier-extracted context is active through handler execution, and a
	//    downstream span opened from inside the handler becomes a child of the server span.
	// -----------------------------------------------------------------------------------------------------------------

	private static final AtomicReference<TraceContextCarrier> J_CARRIER = new AtomicReference<>();

	public static final class J_Extractor implements TraceContextExtractor {
		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			return Optional.ofNullable(J_CARRIER.get());
		}
	}

	@Rest
	public static class J extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return new J_Extractor(); }

		@RestGet("/op")
		public String op() {
			var child = OTEL_SDK.getTracer("t").spanBuilder("child").setParent(Context.current()).startSpan();
			child.end();
			return "ok";
		}
	}

	private static final MockRestClient CJ = MockRestClient.buildLax(J.class);

	@Test void j01_carrierExtractedContext_activeThroughHandler_downstreamChildRelationship() throws Exception {
		J_CARRIER.set(new MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "00-" + META_TRACE_ID + "-" + META_SPAN_ID + "-01"))));

		CJ.get("/op").run().assertStatus(200);

		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(2, spans.size(), "server span + downstream child span");
		var server = spans.stream().filter(x -> ! "child".equals(x.getName())).findFirst().orElseThrow();
		var child = spans.stream().filter(x -> "child".equals(x.getName())).findFirst().orElseThrow();
		assertEquals(META_TRACE_ID, server.getTraceId(), "server span must be parented by the extracted carrier context");
		assertEquals(META_SPAN_ID, server.getParentSpanId());
		assertEquals(server.getTraceId(), child.getTraceId(), "downstream child must share the server span's trace id");
		assertEquals(server.getSpanId(), child.getParentSpanId(), "downstream child must be parented by the active server span");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// K: inject(TraceContextCarrier) — renders the current trace context (and baggage) into a neutral
	//    carrier; writes nothing when there's no active context to render.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_inject_noActiveContext_writesNothing() {
		var hook = new OtelTracerHook(OTEL_SDK);
		var carrier = new MapCarrier(new LinkedHashMap<>());
		hook.inject(carrier);
		assertTrue(carrier.map.isEmpty(), "no active span/context means inject(...) writes nothing");
	}

	@Test void k02_inject_activeSpan_writesTraceparent() {
		var hook = new OtelTracerHook(OTEL_SDK);
		Span span = hook.getTracer().spanBuilder("manual").startSpan();
		try (var scope = span.makeCurrent()) {
			var carrier = new MapCarrier(new LinkedHashMap<>());
			hook.inject(carrier);
			assertNotNull(carrier.map.get("traceparent"));
			assertTrue(carrier.map.get("traceparent").contains(span.getSpanContext().getTraceId()));
		} finally {
			span.end();
		}
	}

	@Test void k03_inject_activeBaggage_writesBaggageKey() {
		var hook = new OtelTracerHook(OTEL_SDK_BAGGAGE);
		var baggage = Baggage.builder().put("userId", "alice").build();
		try (var scope = baggage.makeCurrent()) {
			var carrier = new MapCarrier(new LinkedHashMap<>());
			hook.inject(carrier);
			assertEquals("userId=alice", carrier.map.get("baggage"));
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// L: Scope.recordRpcError(...) — maps a JSON-RPC error code to a low-cardinality error.type category,
	//    stamps rpc.response.status_code, and sets the span status to ERROR with the JSON-RPC message as
	//    its description.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void l01_methodNotFound_mapsToErrorTypeAndStatus() {
		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan("rpc-op")) {
			scope.recordRpcError(-32601, "Method not found");
		}
		var s = EXPORTER.getFinishedSpanItems().get(0);
		assertEquals(StatusCode.ERROR, s.getStatus().getStatusCode());
		assertEquals("Method not found", s.getStatus().getDescription());
		assertEquals("-32601", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_RPC_RESPONSE_STATUS_CODE)));
		assertEquals("method_not_found", s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_ERROR_TYPE)));
	}

	@Test void l02_parseError_mapsToParseErrorType() {
		assertErrorType(-32700, "parse_error");
	}

	@Test void l03_invalidRequest_mapsToInvalidRequestType() {
		assertErrorType(-32600, "invalid_request");
	}

	@Test void l04_invalidParams_mapsToInvalidParamsType() {
		assertErrorType(-32602, "invalid_params");
	}

	@Test void l05_internalError_mapsToInternalErrorType() {
		assertErrorType(-32603, "internal_error");
	}

	@Test void l06_reservedServerErrorRange_mapsToServerErrorType() {
		assertErrorType(-32050, "server_error");
	}

	@Test void l07_applicationCode_fallsBackToNumericErrorType() {
		assertErrorType(1, "1");
	}

	@Test void l07b_codeBelowReservedServerErrorRange_fallsBackToNumericErrorType() {
		assertErrorType(-32100, "-32100");
	}

	private static void assertErrorType(int code, String expectedType) {
		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan("rpc-op")) {
			scope.recordRpcError(code, "msg");
		}
		var spans = EXPORTER.getFinishedSpanItems();
		var s = spans.get(spans.size() - 1);
		assertEquals(String.valueOf(code), s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_RPC_RESPONSE_STATUS_CODE)));
		assertEquals(expectedType, s.getAttributes().get(AttributeKey.stringKey(TraceOperation.ATTR_ERROR_TYPE)));
	}

	@Test void l08_nullMessage_statusDescriptionEmpty() {
		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan("rpc-op")) {
			scope.recordRpcError(-32602, null);
		}
		var s = EXPORTER.getFinishedSpanItems().get(0);
		assertEquals(StatusCode.ERROR, s.getStatus().getStatusCode());
		assertEquals("", s.getStatus().getDescription());
	}
}
