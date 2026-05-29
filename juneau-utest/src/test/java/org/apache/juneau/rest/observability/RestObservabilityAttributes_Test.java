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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.metrics.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for TODO-115: per-resource / per-op observability opt-in attributes.
 *
 * <ul>
 *   <li>A — {@code @Rest(observability="false")} suppresses all metrics for the resource.
 *   <li>B — {@code @RestGet(observability="false")} suppresses metrics for that op only.
 *   <li>C — {@code @RestGet(observability="true")} on a resource that has no explicit resource-level setting
 *            still fires metrics when a backend is wired.
 *   <li>D — {@code @Rest(observability="false")} resource + {@code @RestGet(observability="true")} op override
 *            re-enables observability for that specific op.
 *   <li>E — Startup-fail when {@code @Rest(observability="true")} is set but no backend is wired.
 *   <li>F — {@code @RestGet(metricName)} and {@code @RestGet(metricTags)} reach {@code record(...)}.
 * </ul>
 */
class RestObservabilityAttributes_Test extends TestBase {

	/** Recording MetricsRecorder that captures every event including metricName/metricTags. */
	public static final class RecordingMetricsRecorder implements MetricsRecorder {
		public final List<Event> events = new CopyOnWriteArrayList<>();

		@Override
		public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
			events.add(new Event(opName, httpMethod, uriTemplate, statusCode, elapsed, error, metricName, metricTags));
		}

		public Event last() { return events.get(events.size() - 1); }
		public void clear() { events.clear(); }
	}

	public static final class Event {
		public final String opName, httpMethod, uriTemplate, metricName, metricTags;
		public final int statusCode;
		public final Duration elapsed;
		public final Throwable error;

		Event(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
			this.opName = opName; this.httpMethod = httpMethod; this.uriTemplate = uriTemplate;
			this.statusCode = statusCode; this.elapsed = elapsed; this.error = error;
			this.metricName = metricName; this.metricTags = metricTags;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: @Rest(observability="false") — metrics are suppressed for the entire resource.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder A_REC = new RecordingMetricsRecorder();

	@Rest(observability = "false")
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return A_REC; }

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_resourceLevelOptOut_suppressesMetrics() throws Exception {
		A_REC.clear();
		CA.get("/ping").run().assertStatus(200);
		assertEquals(0, A_REC.events.size(), "@Rest(observability='false') must suppress the observability block");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: @RestGet(observability="false") — metrics are suppressed for that specific op only.
	//    Other ops on the same resource remain observable.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder B_REC = new RecordingMetricsRecorder();

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return B_REC; }

		@RestGet(path = "/silent", observability = "false")
		public String silent() { return "no-metrics"; }

		@RestGet("/loud")
		public String loud() { return "metrics-fire"; }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_opLevelOptOut_suppressesMetricsForThatOp() throws Exception {
		B_REC.clear();
		CB.get("/silent").run().assertStatus(200);
		assertEquals(0, B_REC.events.size(), "@RestGet(observability='false') must suppress that op's metrics");
	}

	@Test void b02_opLevelOptOut_doesNotAffectSiblingOps() throws Exception {
		B_REC.clear();
		CB.get("/loud").run().assertStatus(200);
		assertEquals(1, B_REC.events.size(), "sibling op without observability opt-out must still record metrics");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Default behavior (no observability attribute set) — metrics fire when a backend is wired.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder C_REC = new RecordingMetricsRecorder();

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return C_REC; }

		@RestGet("/check")
		public String check() { return "ok"; }
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_defaultBehavior_metricsFireWhenBackendWired() throws Exception {
		C_REC.clear();
		CC.get("/check").run().assertStatus(200);
		assertEquals(1, C_REC.events.size(), "default (no observability attribute) must fire metrics when backend wired");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: @Rest(observability="false") resource + @RestGet(observability="true") op override.
	//    The op-level "true" re-enables observability for that specific op.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder D_REC = new RecordingMetricsRecorder();

	@Rest(observability = "false")
	public static class D extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return D_REC; }

		@RestGet(path = "/silent", observability = "false")
		public String silent() { return "still-silent"; }

		@RestGet(path = "/loud", observability = "true")
		public String loud() { return "re-enabled"; }
	}

	private static final MockRestClient CD = MockRestClient.buildLax(D.class);

	@Test void d01_opLevelOptIn_overridesResourceLevelOptOut() throws Exception {
		D_REC.clear();
		CD.get("/loud").run().assertStatus(200);
		assertEquals(1, D_REC.events.size(), "@RestGet(observability='true') must re-enable metrics even when resource says false");
	}

	@Test void d02_opLevelOptOut_still_suppressesWhenResourceAlreadySilent() throws Exception {
		D_REC.clear();
		CD.get("/silent").run().assertStatus(200);
		assertEquals(0, D_REC.events.size(), "@RestGet(observability='false') on resource with observability='false' must suppress");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Startup-fail — @Rest(observability="true") but no backend wired must throw during context build.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_startupFail_whenObservabilityTrueAndNoBackend() {
		@Rest(observability = "true")
		class EResource extends RestServlet {
			private static final long serialVersionUID = 1L;
			@RestGet("/x") public String x() { return "x"; }
		}
		assertThrows(Exception.class, () -> MockRestClient.buildLax(EResource.class),
			"@Rest(observability='true') with no MetricsRecorder or TracerHook must fail at build time");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: metricName and metricTags are wired through to record().
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingMetricsRecorder F_REC = new RecordingMetricsRecorder();

	@Rest
	public static class F extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public MetricsRecorder recorder() { return F_REC; }

		@RestGet(path = "/tagged", metricName = "custom.timer", metricTags = "team=payments,region=us-east")
		public String tagged() { return "tagged"; }

		@RestGet("/plain")
		public String plain() { return "plain"; }
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_metricName_reachesRecord() throws Exception {
		F_REC.clear();
		CF.get("/tagged").run().assertStatus(200);
		assertEquals(1, F_REC.events.size());
		assertEquals("custom.timer", F_REC.last().metricName, "metricName must reach MetricsRecorder.record()");
	}

	@Test void f02_metricTags_reachesRecord() throws Exception {
		F_REC.clear();
		CF.get("/tagged").run().assertStatus(200);
		assertEquals(1, F_REC.events.size());
		assertEquals("team=payments,region=us-east", F_REC.last().metricTags, "metricTags must reach MetricsRecorder.record()");
	}

	@Test void f03_plainOp_emptyMetricNameAndTags() throws Exception {
		F_REC.clear();
		CF.get("/plain").run().assertStatus(200);
		assertEquals(1, F_REC.events.size());
		assertEquals("", F_REC.last().metricName, "metricName must be empty when not set");
		assertEquals("", F_REC.last().metricTags, "metricTags must be empty when not set");
	}
}
