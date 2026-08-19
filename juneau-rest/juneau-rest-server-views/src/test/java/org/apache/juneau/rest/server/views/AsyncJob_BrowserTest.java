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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The async-job + SSE-streaming half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} in a real headless browser and asserts the TODO-425 behavior as a user would experience it
 * &mdash; an async row action's start POST returns a job pointer, the row picks up the DISTINCT
 * {@code data-juneau-job} affordance (never {@code data-juneau-inflight}, so the table KEEPS polling), streamed
 * progress renders in a visible {@code textContent} banner, the single terminal {@code result} event settles the row
 * to success / cancelled / cancelled-after-effect, a stream error settles it to a non-optimistic unknown, and Cancel
 * issues a fail-closed CSRF POST to the job's cancel URL.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape test):</h5>
 * <p>
 * {@link ViewsJs_AsyncJobs_Test} proves the shipped script <i>contains</i> the async/streaming logic; it cannot
 * prove that at runtime a running job leaves table polling live (the load-bearing HIGH-9 fact), that progress and the
 * terminal outcome actually paint, or that a stream error degrades to a visible unknown.  This canary drives the real
 * runtime in Chromium against a controllable fake {@code EventSource}, so those user-visible facts are measured
 * rather than inferred.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code async-job.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is needed.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ModalResult_BrowserTest} &mdash; the sibling declarative-modal/typed-result canary.
 * </ul>
 */
@EnabledIfSystemProperty(named=AsyncJob_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class AsyncJob_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("async-job.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("async-job.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("async-job-stdout.json");
		var stderr = dir.resolve("async-job-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> sub(String key) {
		return (Map<String,Object>) report.get(key);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The runtime loaded, at the typed-action-result contract version, with no console errors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoaded() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(ActionResult.CONTRACT_VERSION, report.get("actionResultContractVersion"), () -> report.toString());
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) HIGH-9: a running job uses the DISTINCT marker and does NOT freeze the table's polling
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_runningJobUsesDistinctMarker_andDoesNotFreezePolling() {
		var r = sub("running");
		assertEquals(Boolean.TRUE, r.get("hasJobMarker"), () -> "row did not pick up data-juneau-job: " + report);
		// The whole point of HIGH-9: a job must NOT set the synchronous in-flight marker that freezes polling.
		assertEquals(Boolean.FALSE, r.get("hasInflightMarker"), () -> "job wrongly set data-juneau-inflight: " + report);
		assertEquals(Boolean.FALSE, r.get("pollingFrozen"), () -> "polling was frozen during the job (HIGH-9 violation): " + report);
		assertEquals(Boolean.TRUE, r.get("progressVisible"), () -> "job progress banner not visible: " + report);
		assertEquals("Working\u2026", r.get("progressText"), () -> report.toString());
		assertEquals(Boolean.TRUE, r.get("triggerDisabled"), () -> "trigger not disabled while job runs: " + report);
		assertEquals(Boolean.TRUE, r.get("esOpened"), () -> "no EventSource opened: " + report);
		assertEquals("/juneau-jobs/cap/stream", r.get("esUrl"), () -> "EventSource opened on the wrong (capability) URL: " + report);
	}

	@Test void b02_progressEventUpdatesBanner_andPollingStaysLive() {
		var p = sub("progress");
		assertEquals("step 2 of 3", p.get("text"), () -> "streamed progress did not render: " + report);
		assertEquals(Boolean.FALSE, p.get("pollingFrozenDuringProgress"), () -> "polling froze during progress: " + report);
		assertEquals(Boolean.FALSE, p.get("outcomeYet"), () -> "an outcome rendered before the terminal event: " + report);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The terminal `result` event settles the row (success): banner cleared, marker removed, trigger re-enabled
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_terminalResultSettlesRowToSuccess() {
		var s = sub("settledSuccess");
		assertEquals(Boolean.TRUE, s.get("esClosed"), () -> "EventSource not closed on settle: " + report);
		assertEquals(Boolean.TRUE, s.get("jobMarkerCleared"), () -> "data-juneau-job not cleared on settle: " + report);
		assertEquals(Boolean.TRUE, s.get("progressCleared"), () -> "progress banner not cleared on settle: " + report);
		assertEquals(Boolean.TRUE, s.get("triggerReEnabled"), () -> "trigger not re-enabled on settle: " + report);
		assertEquals(Boolean.FALSE, s.get("pollingFrozen"), () -> "polling frozen after settle: " + report);
		assertNotNull(s.get("mergedRow"), () -> "success did not re-render from the authoritative result row: " + report);
		@SuppressWarnings("unchecked")
		var outcome = (Map<String,Object>) s.get("outcome");
		assertEquals(Boolean.TRUE, outcome.get("visible"), () -> "success banner not visible: " + report);
		assertEquals("success", outcome.get("state"), () -> report.toString());
		assertEquals("status", outcome.get("role"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Cancel issues a fail-closed CSRF POST; the SERVER-authoritative terminal outcome arrives over SSE (Q4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_cancelIssuesFailClosedCsrfPost() {
		var cancel = sub("cancel");
		assertEquals(Boolean.TRUE, cancel.get("cancelBtnVisible"), () -> "cancel button not visible: " + report);
		assertEquals(Boolean.TRUE, cancel.get("postIssued"), () -> "cancel did not issue a POST: " + report);
		assertEquals("/juneau-jobs/cap/cancel", cancel.get("url"), () -> "cancel POST hit the wrong URL: " + report);
		assertEquals("POST", cancel.get("method"), () -> "cancel must be a non-safe POST: " + report);
		assertEquals("tok-xyz", cancel.get("csrfHeader"), () -> "cancel POST did not carry the CSRF token: " + report);
	}

	@Test void d02_cancelledOutcomeArrivesOverStream() {
		var cancel = sub("cancel");
		@SuppressWarnings("unchecked")
		var outcome = (Map<String,Object>) cancel.get("outcome");
		assertEquals("cancelled", outcome.get("state"), () -> report.toString());
		assertEquals(Boolean.TRUE, outcome.get("visible"), () -> "cancelled banner not visible: " + report);
		assertTrue(String.valueOf(outcome.get("text")).contains("Cancelled"), () -> report.toString());
		assertEquals(Boolean.TRUE, cancel.get("jobMarkerCleared"), () -> "job marker not cleared on cancel: " + report);
	}

	@Test void d03_cancelledAfterEffectIsADistinctOutcome() {
		// Q4: cancelled-after-effect must NOT be collapsed into plain cancelled - it is its own rendered outcome.
		var o = sub("cancelledAfterEffect");
		assertEquals("cancelled-after-effect", o.get("state"), () -> report.toString());
		assertEquals(Boolean.TRUE, o.get("visible"), () -> "cancelled-after-effect banner not visible: " + report);
		assertTrue(String.valueOf(o.get("text")).contains("partial effect"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) A stream error is itself a non-optimistic terminal outcome; polling was never frozen
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_streamErrorRendersVisibleUnknown() {
		var s = sub("streamError");
		@SuppressWarnings("unchecked")
		var outcome = (Map<String,Object>) s.get("outcome");
		assertEquals("unknown", outcome.get("state"), () -> report.toString());
		assertEquals(Boolean.TRUE, outcome.get("visible"), () -> "stream-error unknown banner not visible: " + report);
		assertEquals(Boolean.TRUE, s.get("jobMarkerCleared"), () -> "job marker not cleared on stream error: " + report);
		assertEquals(Boolean.FALSE, s.get("pollingFrozen"), () -> "polling frozen after stream error: " + report);
	}
}
