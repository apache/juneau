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
package org.apache.juneau.rest.server.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.observation.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.*;

/**
 * Tests for the custom (non-request) {@code record(name, tags, elapsed, error)} override on
 * {@link MicrometerMetricsRecorder}, driven both directly and through the {@link Observer} facade.
 */
class MicrometerCustomObservation_Test extends TestBase {

	private SimpleMeterRegistry registry;
	private MicrometerMetricsRecorder recorder;

	@BeforeEach
	void setup() {
		registry = new SimpleMeterRegistry();
		recorder = new MicrometerMetricsRecorder(registry);
	}

	private Timer timer(String name) {
		return registry.find(name).timer();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: direct record(name, tags, elapsed, error) calls.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_recordsTimerWithExceptionTagNone() {
		recorder.record("order.load", "", Duration.ofMillis(5), null);
		var t = timer("order.load");
		assertNotNull(t);
		assertEquals(1, t.count());
		assertEquals("None", t.getId().getTag("exception"));
	}

	@Test void a02_appliesCustomTags() {
		recorder.record("order.load", "team=payments,region=us-east", Duration.ofMillis(3), null);
		var t = registry.find("order.load").tag("team", "payments").tag("region", "us-east").timer();
		assertNotNull(t);
		assertEquals(1, t.count());
	}

	@Test void a03_errorTagFromThrowable() {
		recorder.record("order.load", "", Duration.ofMillis(1), new IllegalStateException("x"));
		var t = registry.find("order.load").tag("exception", "IllegalStateException").timer();
		assertNotNull(t);
		assertEquals(1, t.count());
	}

	@Test void a04_noHttpTagsOnCustomTimer() {
		recorder.record("order.load", "", Duration.ofMillis(1), null);
		var t = timer("order.load");
		assertNull(t.getId().getTag("method"));
		assertNull(t.getId().getTag("uri"));
		assertNull(t.getId().getTag("status"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: through the Observer facade (metrics-only).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_observerHappyPath() {
		var observer = new Observer(recorder, NoOpTracerHook.INSTANCE);
		try (var o = observer.start("svc.call", "k=v")) {
			assertNotSame(Observation.NOOP, o);
		}
		var t = registry.find("svc.call").tag("k", "v").tag("exception", "None").timer();
		assertNotNull(t);
		assertEquals(1, t.count());
	}

	@Test void b02_observerErrorPath() {
		var observer = new Observer(recorder, NoOpTracerHook.INSTANCE);
		try (var o = observer.start("svc.call", "")) {
			o.setError(new RuntimeException("boom"));
		}
		var t = registry.find("svc.call").tag("exception", "RuntimeException").timer();
		assertNotNull(t);
		assertEquals(1, t.count());
	}
}
