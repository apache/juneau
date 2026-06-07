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
package org.apache.juneau.rest.observability;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.metrics.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests SVL variable support in observability annotation attributes.
 *
 * <p>
 * The resource-level {@code @Rest(observability)} attribute is resolved at {@code RestContext} build time
 * (the constructor pre-warms the memoizer via the startup-fail check). Op-level attributes
 * ({@code metricName}, {@code metricTags}) are resolved lazily on first request.
 *
 * <p>
 * Note: {@code MockRestClient} caches the {@link org.apache.juneau.rest.RestContext} per resource class.
 * Each scenario uses a distinct static inner class so the SVL state at build time is deterministic.
 */
class RestObservabilitySvl_Test extends TestBase {

	public static final class RecordingMetricsRecorder implements MetricsRecorder {
		public final List<Event> events = new CopyOnWriteArrayList<>();

		@SuppressWarnings({
			"java:S6213" // 'record' mirrors the MetricsRecorder SPI method name; renaming would break the @Override contract.
		})
		@Override
		public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
			events.add(new Event(metricName, metricTags));
		}

		public Event last() { return events.get(events.size() - 1); }
		public void clear() { events.clear(); }
	}

	public static final class Event {
		public final String metricName, metricTags;
		Event(String metricName, String metricTags) {
			this.metricName = metricName; this.metricTags = metricTags;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: metricName and metricTags with literal values flow correctly to record().
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder A_REC = new RecordingMetricsRecorder();

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return A_REC; }

		@RestGet(path = "/op", metricName = "my.service.requests", metricTags = "svc=orders")
		public String op() { return "ok"; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_literalMetricNameAndTags_resolvedCorrectly() throws Exception {
		A_REC.clear();
		CA.get("/op").run().assertStatus(200);
		assertEquals("my.service.requests", A_REC.last().metricName,
			"literal metricName must reach MetricsRecorder.record() unchanged");
		assertEquals("svc=orders", A_REC.last().metricTags,
			"literal metricTags must reach MetricsRecorder.record() unchanged");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: SVL variable in @Rest(observability) is resolved at RestContext build time.
	//    When the system property resolves to "false", metrics are suppressed for the entire resource.
	//    The static initializer sets the property before building the client and then clears it;
	//    this works because the resource-level observability attribute IS resolved eagerly (the
	//    startup-fail check pre-warms it inside the RestContext constructor).
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder B_REC = new RecordingMetricsRecorder();

	@Rest(observability = "$S{test.svl.obs.b,}")
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return B_REC; }

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	// Build the client while the property is "false" so the context captures observability=false.
	private static final MockRestClient CB;
	static {
		System.setProperty("test.svl.obs.b", "false");
		CB = MockRestClient.buildLax(B.class);
		System.clearProperty("test.svl.obs.b");
	}

	@Test void b01_svlObservability_resolvedAtBuildTime_suppressesMetrics() throws Exception {
		B_REC.clear();
		CB.get("/ping").run().assertStatus(200);
		assertEquals(0, B_REC.events.size(),
			"$S{} SVL variable resolving to 'false' at context build time must suppress observability block");
	}
}
