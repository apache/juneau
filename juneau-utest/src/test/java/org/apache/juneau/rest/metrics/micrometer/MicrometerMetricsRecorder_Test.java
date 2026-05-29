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
package org.apache.juneau.rest.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.*;
import io.micrometer.prometheusmetrics.*;
import io.prometheus.metrics.model.registry.*;

/**
 * Validates {@link MicrometerMetricsRecorder} &mdash; direct {@code record(...)} produces a
 * Micrometer {@link Timer} with the Spring-Boot-style tags, the bridge picks up via a
 * {@code @Bean MetricsRecorder} on a real {@code @Rest}, and the {@link PrometheusMeterRegistry}
 * scrape output carries the {@code method} / {@code uri} / {@code status} / {@code exception} tags.
 */
class MicrometerMetricsRecorder_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Unit tests against a SimpleMeterRegistry.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultTimerName_isHttpServerRequests() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry);
		assertEquals("http.server.requests", r.getTimerName());
		assertSame(registry, r.getRegistry());
	}

	@Test void a02_customTimerName_isHonored() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry, "my.timer");
		assertEquals("my.timer", r.getTimerName());
	}

	@Test void a03_nullRegistry_throws() {
		assertThrows(IllegalArgumentException.class, () -> new MicrometerMetricsRecorder(null));
	}

	@Test void a04_blankTimerName_throws() {
		var registry = new SimpleMeterRegistry();
		assertThrows(IllegalArgumentException.class, () -> new MicrometerMetricsRecorder(registry, ""));
		assertThrows(IllegalArgumentException.class, () -> new MicrometerMetricsRecorder(registry, "   "));
	}

	@Test void a05_happyPath_registersTimerWithExpectedTags() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry);
		r.record("MyResource.get(java.lang.String)", "GET", "/users/{id}", 200, Duration.ofMillis(7), null, "", "");

		var timer = registry.find("http.server.requests")
			.tag("method", "GET")
			.tag("uri", "/users/{id}")
			.tag("status", "200")
			.tag("exception", "None")
			.timer();
		assertNotNull(timer, "timer with full tag set should exist");
		assertEquals(1, timer.count());
		assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) > 0);
	}

	@Test void a06_exceptionPath_recordsExceptionSimpleNameTag() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry);
		r.record("op", "POST", "/orders", 500, Duration.ofMillis(3), new IllegalStateException(), "", "");

		var timer = registry.find("http.server.requests")
			.tag("exception", "IllegalStateException")
			.timer();
		assertNotNull(timer);
		assertEquals(1, timer.count());
	}

	@Test void a07_separateTagSets_produceSeparateTimers() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry);
		r.record("op", "GET", "/a", 200, Duration.ofMillis(1), null, "", "");
		r.record("op", "GET", "/b", 200, Duration.ofMillis(2), null, "", "");

		assertEquals(1, registry.find("http.server.requests").tag("uri", "/a").timer().count());
		assertEquals(1, registry.find("http.server.requests").tag("uri", "/b").timer().count());
	}

	@Test void a08_blankUri_emitsEmptyStringTag() {
		var registry = new SimpleMeterRegistry();
		var r = new MicrometerMetricsRecorder(registry);
		r.record("op", "GET", "", 200, Duration.ofMillis(1), null, "", "");
		assertNotNull(registry.find("http.server.requests").tag("uri", "").timer());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: End-to-end test wiring the bridge into a real @Rest as a @Bean MetricsRecorder.
	// -----------------------------------------------------------------------------------------------------------------

	private static final SimpleMeterRegistry B_REGISTRY = new SimpleMeterRegistry();

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public org.apache.juneau.rest.metrics.MetricsRecorder recorder() {
			return new MicrometerMetricsRecorder(B_REGISTRY);
		}

		@RestGet("/users/{id}")
		public String get(@org.apache.juneau.http.annotation.Path String id) { return "u:" + id; }

		@RestGet("/boom")
		public String boom() { throw new IllegalStateException("kaboom"); }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_endToEnd_happyPath_recordsTimerSample() throws Exception {
		CB.get("/users/1").run().assertStatus(200);

		var timer = B_REGISTRY.find("http.server.requests")
			.tag("method", "GET")
			.tag("uri", "/users/{id}")
			.tag("status", "200")
			.tag("exception", "None")
			.timer();
		assertNotNull(timer, "end-to-end timer should be registered after one happy-path call");
		assertTrue(timer.count() >= 1);
	}

	@Test void b02_endToEnd_exceptionPath_recordsExceptionTag() throws Exception {
		CB.get("/boom").run().assertStatus(500);

		var timer = B_REGISTRY.find("http.server.requests")
			.tag("method", "GET")
			.tag("uri", "/boom")
			.tag("status", "500")
			.tag("exception", "IllegalStateException")
			.timer();
		assertNotNull(timer);
		assertTrue(timer.count() >= 1);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Prometheus scrape output verification — the bridge produces text Prometheus understands.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_prometheusScrapeContainsExpectedSample() {
		var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, new PrometheusRegistry(), io.micrometer.core.instrument.Clock.SYSTEM);
		var r = new MicrometerMetricsRecorder(registry);
		r.record("op", "GET", "/users/{id}", 200, Duration.ofMillis(5), null, "", "");

		String scrape = registry.scrape();
		assertTrue(scrape.contains("http_server_requests_seconds_count"), "scrape output: " + scrape);
		assertTrue(scrape.contains("uri=\"/users/{id}\""), "scrape output should carry uri tag verbatim");
		assertTrue(scrape.contains("status=\"200\""));
		assertTrue(scrape.contains("exception=\"None\""));
		assertTrue(scrape.contains("method=\"GET\""));
	}
}
