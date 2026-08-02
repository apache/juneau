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
package org.apache.juneau.rest.server.tracing;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.tracing.otel.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

import io.opentelemetry.api.trace.propagation.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.sdk.*;
import io.opentelemetry.sdk.testing.exporter.*;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.trace.export.*;

/**
 * Tests for {@link TraceContextResponseProcessor} — outgoing-response W3C trace-context
 * header injection.
 *
 * <p>
 * Reuses the OpenTelemetry SDK + {@link InMemorySpanExporter} test-fixture pattern: a
 * statically-wired SDK is installed before the {@code @Rest} resources build their
 * {@link MockRestClient}s so the {@link OtelTracerHook} bridge captures the server-started span's
 * context and stashes the rendered headers for the response processor to write.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class TraceContextResponseProcessor_Test extends TestBase {

	/** W3C {@code traceparent} value format: {@code version-traceId-spanId-flags}. */
	private static final String W3C_TRACEPARENT_REGEX = "[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}";

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

	@AfterEach
	void cleanup() {
		System.clearProperty("RestContext.responseTraceparent");
		Settings.get().unsetGlobal("RestContext.responseTraceparent");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Happy path — active tracer ⇒ traceparent header present and well-formed.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@RestGet("/users/{id}")
		public String get(@Path String id) { return "u:" + id; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_happyPath_traceparentHeaderPresent() throws Exception {
		CA.get("/users/42").run()
			.assertStatus(200)
			.assertHeader("traceparent").isExists();
	}

	@Test void a02_headerFormat_matchesW3cRegex() throws Exception {
		CA.get("/users/99").run()
			.assertStatus(200)
			.assertHeader("traceparent").isPattern(W3C_TRACEPARENT_REGEX);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: NoOp tracer — no @Bean TracerHook ⇒ no traceparent header.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_noTracer_noTraceparentHeader() throws Exception {
		CB.get("/ping").run()
			.assertStatus(200)
			.assertHeader("traceparent").isNull();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Opt-out — RestContext.responseTraceparent=false keeps the processor unregistered even with a tracer.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@RestGet("/opt")
		public String opt() { return "x"; }
	}

	@Test void c01_optOut_noTraceparentHeaderDespiteActiveTracer() throws Exception {
		// Set BEFORE building the resource so the @Value-driven default is resolved as false at init.
		System.setProperty("RestContext.responseTraceparent", "false");
		var c = MockRestClient.buildLax(C.class);
		c.get("/opt").run()
			.assertStatus(200)
			.assertHeader("traceparent").isNull();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: tracestate companion header — emitted only when the active trace state is non-empty.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_incomingTracestate_emittedOnResponse() throws Exception {
		var traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
		CA.get("/users/7")
			.header("traceparent", traceparent)
			.header("tracestate", "foo=bar")
			.run()
			.assertStatus(200)
			.assertHeader("traceparent").isPattern(W3C_TRACEPARENT_REGEX)
			.assertHeader("tracestate").is("foo=bar");
	}

	@Test void d02_emptyTracestate_headerAbsent() throws Exception {
		// No incoming tracestate ⇒ server span carries an empty trace state ⇒ no tracestate header written.
		CA.get("/users/8").run()
			.assertStatus(200)
			.assertHeader("traceparent").isExists()
			.assertHeader("tracestate").isNull();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Response-already-committed edge — processor skips without throwing (MockServletResponse.isCommitted() is
	// hard-wired false, so this branch is exercised with mocks).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_responseCommitted_skipsWithoutThrowing() {
		var req = mock(RestRequest.class);
		var res = mock(RestResponse.class);
		var hsr = mock(jakarta.servlet.http.HttpServletResponse.class);
		var opSession = mock(RestOpSession.class);

		var tpAttr = mock(RequestAttribute.class);
		when(tpAttr.as(String.class)).thenReturn(o("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
		when(req.getAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT)).thenReturn(tpAttr);

		when(opSession.getRequest()).thenReturn(req);
		when(opSession.getResponse()).thenReturn(res);
		when(res.getHttpServletResponse()).thenReturn(hsr);
		when(hsr.isCommitted()).thenReturn(true);

		var p = new TraceContextResponseProcessor();
		var rc = assertDoesNotThrow(() -> p.process(opSession));
		assertEquals(ResponseProcessor.FINISHED, rc);
		// Committed ⇒ no header write attempted.
		verify(res, never()).setHeader(eq("traceparent"), anyString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: ATTR_BAGGAGE is a request-attribute-only stash — this processor never reads it and never emits a
	// "baggage" HTTP response header, even when both traceparent (which drives the rest of the emission path)
	// and baggage are present on the request.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_baggageAttribute_neverBecomesResponseHeader() {
		var req = mock(RestRequest.class);
		var res = mock(RestResponse.class);
		var hsr = mock(jakarta.servlet.http.HttpServletResponse.class);
		var opSession = mock(RestOpSession.class);

		var tpAttr = mock(RequestAttribute.class);
		when(tpAttr.as(String.class)).thenReturn(o("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
		when(req.getAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT)).thenReturn(tpAttr);

		var tsAttr = mock(RequestAttribute.class);
		when(tsAttr.as(String.class)).thenReturn(o(null));
		when(req.getAttribute(TraceContextResponseProcessor.ATTR_TRACESTATE)).thenReturn(tsAttr);

		var bgAttr = mock(RequestAttribute.class);
		when(bgAttr.as(String.class)).thenReturn(o("userId=alice"));
		when(req.getAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE)).thenReturn(bgAttr);

		when(opSession.getRequest()).thenReturn(req);
		when(opSession.getResponse()).thenReturn(res);
		when(res.getHttpServletResponse()).thenReturn(hsr);
		when(hsr.isCommitted()).thenReturn(false);

		var p = new TraceContextResponseProcessor();
		var rc = assertDoesNotThrow(() -> p.process(opSession));
		assertEquals(ResponseProcessor.NEXT, rc);
		verify(res).setHeader(eq("traceparent"), anyString());
		verify(res, never()).setHeader(eq("baggage"), anyString());
		verify(req, never()).getAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: End-to-end — a non-HTTP TraceContextCarrier (standing in for a future MCP params._meta carrier)
	// recognized by a registered TraceContextExtractor wins over a differing HTTP traceparent header, and
	// that winning value is what this processor echoes back as the response header.
	// -----------------------------------------------------------------------------------------------------------------

	private static final class H_MapCarrier implements TraceContextCarrier {
		private final Map<String,String> map;
		H_MapCarrier(Map<String,String> map) { this.map = map; }
		@Override public String get(String key) { return map.get(key); }
		@Override public Iterable<String> keys() { return map.keySet(); }
		@Override public void set(String key, String value) { map.put(key, value); }
	}

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

		@Bean
		public TracerHook tracer() { return new OtelTracerHook(OTEL_SDK); }

		@Bean
		public TraceContextExtractor extractor() { return new H_Extractor(); }

		@RestGet("/op")
		public String op() { return "ok"; }
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_metadataTraceparent_echoedOnResponseInsteadOfHttpHeader() throws Exception {
		String metaTraceId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		String metaSpanId = "aaaaaaaaaaaaaaaa";
		H_CARRIER.set(new H_MapCarrier(new LinkedHashMap<>(Map.of("traceparent", "00-" + metaTraceId + "-" + metaSpanId + "-01"))));

		var traceparent = CH.get("/op")
			.header("traceparent", "00-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb-bbbbbbbbbbbbbbbb-01")
			.run()
			.assertStatus(200)
			.getHeader("traceparent").asString().orElseThrow();

		assertTrue(traceparent.contains(metaTraceId), "response traceparent must carry the params._meta trace id, not the HTTP header's");
	}
}
