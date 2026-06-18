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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.observation.*;
import org.junit.jupiter.api.*;

import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.sdk.*;
import io.opentelemetry.sdk.testing.exporter.*;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.trace.export.*;

/**
 * Tests for the custom (non-request) {@code startSpan(String)} override on {@link OtelTracerHook},
 * verifying an {@code INTERNAL} span is produced (and nested under an active parent), driven both
 * directly and through the {@link Observer} facade.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test, not a real leak.
})
class OtelCustomObservation_Test extends TestBase {

	static final InMemorySpanExporter EXPORTER = InMemorySpanExporter.create();
	static final OpenTelemetrySdk OTEL_SDK = OpenTelemetrySdk.builder()
		.setTracerProvider(SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(EXPORTER))
			.build())
		.setPropagators(ContextPropagators.create(io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance()))
		.build();

	@BeforeEach
	void resetExporter() {
		EXPORTER.reset();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: direct startSpan(name).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_internalSpanProducedAndClosed() {
		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan("loadOrder")) {
			scope.setStatusCode(200);
		}
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		var s = spans.get(0);
		assertEquals("loadOrder", s.getName());
		assertEquals(SpanKind.INTERNAL, s.getKind());
	}

	@Test void a02_errorRecordedOnSpan() {
		var hook = new OtelTracerHook(OTEL_SDK);
		try (var scope = hook.startSpan("loadOrder")) {
			scope.setError(new IllegalStateException("boom"));
		}
		var s = EXPORTER.getFinishedSpanItems().get(0);
		assertEquals(StatusCode.ERROR, s.getStatus().getStatusCode());
		assertEquals("IllegalStateException", s.getAttributes().get(OtelTracerHook.ATTR_EXCEPTION_TYPE));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: through the Observer facade (tracing-only).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_observerProducesSpan() {
		var observer = new Observer(NoOpMetricsRecorder.INSTANCE, new OtelTracerHook(OTEL_SDK));
		try (var o = observer.start("svc.call", "")) {
			assertNotSame(Observation.NOOP, o);
		}
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(1, spans.size());
		assertEquals("svc.call", spans.get(0).getName());
		assertEquals(SpanKind.INTERNAL, spans.get(0).getKind());
	}

	@Test void b02_nestedObservationsShareTrace() {
		var hook = new OtelTracerHook(OTEL_SDK);
		var observer = new Observer(NoOpMetricsRecorder.INSTANCE, hook);
		try (var outer = observer.start("outer", "")) {
			assertNotSame(Observation.NOOP, outer);
			try (var inner = observer.start("inner", "")) {
				assertNotSame(Observation.NOOP, inner);
			}
		}
		var spans = EXPORTER.getFinishedSpanItems();
		assertEquals(2, spans.size());
		// Inner finishes first; both share one trace id (inner is parented to outer).
		var inner = spans.stream().filter(x -> x.getName().equals("inner")).findFirst().orElseThrow();
		var outer = spans.stream().filter(x -> x.getName().equals("outer")).findFirst().orElseThrow();
		assertEquals(outer.getTraceId(), inner.getTraceId());
		assertEquals(outer.getSpanId(), inner.getParentSpanId());
	}
}
