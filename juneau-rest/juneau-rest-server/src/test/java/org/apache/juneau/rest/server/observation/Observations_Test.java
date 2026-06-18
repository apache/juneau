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
package org.apache.juneau.rest.server.observation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.tracing.*;
import org.apache.juneau.rest.server.tracing.Scope;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the explicit programmatic observation API ({@link Observations} / {@link Observer} /
 * {@link Observation}) using in-memory fakes of the {@link MetricsRecorder} / {@link TracerHook} SPIs.
 */
@SuppressWarnings({
	"resource" // FakeTracer.startSpan(...) returns a FakeScope by SPI contract; the observation handle owns and closes it.
})
class Observations_Test {

	// -----------------------------------------------------------------------------------------------------------------
	// Fakes capturing the non-request SPI default-method calls.
	// -----------------------------------------------------------------------------------------------------------------

	static final class FakeRecorder implements MetricsRecorder {
		final List<String> events = new ArrayList<>();

		@Override /* MetricsRecorder */
		public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
			// Request-path method unused by these tests.
		}

		@Override /* MetricsRecorder */
		public void record(String metricName, String metricTags, Duration elapsed, Throwable error) {
			assertNotNull(elapsed);
			assertFalse(elapsed.isNegative());
			events.add(metricName + "|" + metricTags + "|err=" + (error == null ? "None" : error.getClass().getSimpleName()));
		}
	}

	static final class FakeScope implements Scope {
		final String name;
		final List<String> log;

		FakeScope(String name, List<String> log) {
			this.name = name;
			this.log = log;
		}

		@Override /* Scope */ public void setStatusCode(int statusCode) { /* unused */ }
		@Override /* Scope */ public void setError(Throwable error) { log.add(name + ":error=" + error.getClass().getSimpleName()); }
		@Override /* Scope */ public void close() { log.add(name + ":close"); }
	}

	static final class FakeTracer implements TracerHook {
		final List<String> log = new ArrayList<>();

		@Override /* TracerHook */
		public Scope startSpan(RestRequest request) { return NoOpTracerHook.NoOpScope.INSTANCE; }

		@Override /* TracerHook */
		public Scope startSpan(String spanName) {
			log.add("start:" + spanName);
			return new FakeScope(spanName, log);
		}
	}

	@AfterEach
	void resetGlobal() {
		Observations.reset();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: No-backend short-circuit (zero-alloc NOOP path).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_noBackend_observerIsInactive() {
		assertFalse(Observer.NOOP.isActive());
	}

	@Test void a02_noBackend_returnsNoopHandle() {
		try (var o = Observer.NOOP.start("x", "a=b")) {
			assertSame(Observation.NOOP, o);
		}
	}

	@Test void a03_facadeDefaultsToNoop() {
		assertSame(Observer.NOOP, Observations.observer());
		try (var o = Observations.observe("x")) {
			assertSame(Observation.NOOP, o);
		}
	}

	@Test void a04_noopHandle_setErrorAndCloseAreSafe() {
		var o = Observation.NOOP;
		o.setError(new RuntimeException("ignored"));
		o.close();  // must not throw
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Active observer — metrics only, tracing only, both.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_metricsOnly_recordsTimerWithNameAndTags() {
		var r = new FakeRecorder();
		var observer = new Observer(r, NoOpTracerHook.INSTANCE);
		assertTrue(observer.isActive());
		try (var o = observer.start("order.load", "team=payments")) {
			assertNotSame(Observation.NOOP, o);
		}
		assertEquals(List.of("order.load|team=payments|err=None"), r.events);
	}

	@Test void b02_tracingOnly_opensAndClosesSpan() {
		var t = new FakeTracer();
		var observer = new Observer(NoOpMetricsRecorder.INSTANCE, t);
		try (var o = observer.start("order.load", null)) {
			assertNotSame(Observation.NOOP, o);
		}
		assertEquals(List.of("start:order.load", "order.load:close"), t.log);
	}

	@Test void b03_both_recordTimerAndSpan() {
		var r = new FakeRecorder();
		var t = new FakeTracer();
		var observer = new Observer(r, t);
		try (var o = observer.start("svc.call", "region=us")) {
			assertNotSame(Observation.NOOP, o);
		}
		assertEquals(List.of("svc.call|region=us|err=None"), r.events);
		assertEquals(List.of("start:svc.call", "svc.call:close"), t.log);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Error path — error tag + span error recorded.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_error_taggedOnTimerAndSpan() {
		var r = new FakeRecorder();
		var t = new FakeTracer();
		var observer = new Observer(r, t);
		var boom = new IllegalStateException("boom");
		try (var o = observer.start("svc.call", "")) {
			o.setError(boom);
		}
		assertEquals(List.of("svc.call||err=IllegalStateException"), r.events);
		// Span: start, then error recorded, then close.
		assertEquals(List.of("start:svc.call", "svc.call:error=IllegalStateException", "svc.call:close"), t.log);
	}

	@Test void c02_nullErrorTreatedAsSuccess() {
		var r = new FakeRecorder();
		var observer = new Observer(r, NoOpTracerHook.INSTANCE);
		try (var o = observer.start("x", "")) {
			o.setError(null);
		}
		assertEquals(List.of("x||err=None"), r.events);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Global facade install / observe / reset.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_install_thenObserveUsesInstalledBackend() {
		var r = new FakeRecorder();
		Observations.install(new Observer(r, NoOpTracerHook.INSTANCE));
		try (var o = Observations.observe("biz.metric", "k=v")) {
			assertNotSame(Observation.NOOP, o);
		}
		assertEquals(List.of("biz.metric|k=v|err=None"), r.events);
	}

	@Test void d02_observeNoTags_passesEmptyTags() {
		var r = new FakeRecorder();
		Observations.install(new Observer(r, NoOpTracerHook.INSTANCE));
		try (var o = Observations.observe("biz.metric")) {
			assertNotSame(Observation.NOOP, o);
		}
		assertEquals(List.of("biz.metric||err=None"), r.events);
	}

	@Test void d03_reset_restoresNoop() {
		Observations.install(new Observer(new FakeRecorder(), NoOpTracerHook.INSTANCE));
		assertTrue(Observations.observer().isActive());
		Observations.reset();
		assertSame(Observer.NOOP, Observations.observer());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Argument validation.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_nullRecorder_throws() {
		assertThrows(IllegalArgumentException.class, () -> new Observer(null, NoOpTracerHook.INSTANCE));
	}

	@Test void e02_nullTracer_throws() {
		assertThrows(IllegalArgumentException.class, () -> new Observer(NoOpMetricsRecorder.INSTANCE, null));
	}

	@Test void e03_blankName_throws() {
		var observer = new Observer(new FakeRecorder(), NoOpTracerHook.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> observer.start("  ", ""));
	}

	@Test void e04_nullName_throws() {
		var observer = new Observer(new FakeRecorder(), NoOpTracerHook.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> observer.start(null, ""));
	}

	@Test void e05_install_null_throws() {
		assertThrows(IllegalArgumentException.class, () -> Observations.install(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Default SPI methods are no-ops on the NoOp singletons (zero-cost contract).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_noopTracer_startSpanByName_returnsNoopScope() {
		assertSame(NoOpTracerHook.NoOpScope.INSTANCE, NoOpTracerHook.INSTANCE.startSpan("x"));
	}

	@Test void f02_noopRecorder_recordCustom_doesNothing() {
		var called = new AtomicBoolean(false);
		// Default method on the NoOp recorder must simply do nothing (no throw, no side effect we can observe).
		NoOpMetricsRecorder.INSTANCE.record("x", "", Duration.ofMillis(1), null);
		assertFalse(called.get());
	}
}
