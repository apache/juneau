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
 * The row-selection + bulk-mutation half of the module's <b>JavaScript-execution harness</b> ({@code TODO-428}):
 * runs the REAL served {@code juneau-views.js} in a real headless browser and asserts, as a user would experience
 * it, that:
 * <ul>
 * 	<li>per-row selection and select-all toggle a live selection set keyed by the STABLE row id (never a DOM index);
 * 	<li>a poll/sort/page draw silently DROPS any selected id no longer on screen (MED-11's persistence rule);
 * 	<li>row-selection and bulk-mutation are two INDEPENDENT opt-ins - a selection-only (e.g. export) table never
 * 		carries the bulk marker, even though both are declared via the same {@code hasSelection}/{@code hasBulk}
 * 		DOM-attribute mechanism (HIGH-5);
 * 	<li>a bulk action fires N INDEPENDENT per-row writes - never one aggregate request - each with its OWN
 * 		in-flight marker and its OWN typed {@code ActionResult}, so one target's failure/refusal can never be
 * 		hidden behind an overall "success" (MED-4);
 * 	<li>a selected id whose row is no longer present (went off-screen between the click and execution) is silently
 * 		skipped rather than targeted;
 * 	<li>the bulk toolbar's buttons are live-gated on the selection count, and the independently-versioned bulk
 * 		sidecar is read/contract-checked correctly at runtime (R2).
 * </ul>
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape test):</h5>
 * <p>
 * {@link ViewsJs_Selection_Test} proves the shipped script <i>contains</i> the selection/bulk logic in the right
 * shape; it cannot prove a checkbox click actually updates the live selection set, that a draw actually prunes an
 * off-screen id, or that a bulk action actually issues N independent requests with N independent settled outcomes.
 * This canary drives the real runtime in Chromium against a stubbed {@code fetch}, so those user-visible facts are
 * measured rather than inferred.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code row-selection-bulk.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ModalResult_BrowserTest} &mdash; the sibling TODO-416/417 canary this reuses the settle
 * 		path from.
 * </ul>
 */
@EnabledIfSystemProperty(named=RowSelectionBulk_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RowSelectionBulk_BrowserTest extends TestBase {

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
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("row-selection-bulk.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("row-selection-bulk.html");
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
		var stdout = dir.resolve("row-selection-bulk-stdout.json");
		var stderr = dir.resolve("row-selection-bulk-stderr.txt");
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
	// a) The runtime loaded, at the bulk contract's OWN version (independent of VIEW_META's)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedAtBulkContractVersion() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(BulkMutateDef.CONTRACT_VERSION, report.get("bulkContractVersion"), () -> report.toString());
		assertEquals("1", BulkMutateDef.CONTRACT_VERSION);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Per-row selection + select-all, keyed by the stable row id
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_checkingTwoRowsSelectsExactlyThoseStableIds() {
		assertEquals(List.of("1", "2"), report.get("afterTwoChecked"), () -> report.toString());
	}

	@Test void b02_selectAllSelectsEveryRowCurrentlyOnScreen() {
		assertEquals(Boolean.TRUE, report.get("hasSelectAllCheckbox"), () -> report.toString());
		assertEquals(List.of("1", "2", "3"), report.get("afterSelectAll"), () -> report.toString());
	}

	@Test void b03_deselectAllClearsTheSelection() {
		assertEquals(List.of(), report.get("afterDeselectAll"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The off-screen-id-drop persistence rule (MED-11/Q2) actually prunes on a draw
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_aDrawDropsAnIdThatLeftTheScreen() {
		// Row '3' was removed from the DOM (simulating it leaving the current page/sort/poll draw) before the
		// draw.dt tick fired - the persistence rule must drop it, keeping only '1' and '2'.
		assertEquals(List.of("1", "2"), report.get("selectedAfterOffScreenDraw"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Two INDEPENDENT opt-ins (HIGH-5) - verified against the ACTUAL DOM-attribute detection at runtime
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_selectionOnlyTableNeverCarriesTheBulkMarker() {
		assertEquals(Boolean.TRUE, report.get("selectOnlyHasSelection"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("selectOnlyHasBulk"),
			() -> "a selection-only (export) table must never surface a bulk-mutate control: " + report);
	}

	@Test void d02_bulkTableCarriesBothMarkers() {
		assertEquals(Boolean.TRUE, report.get("withBulkHasSelection"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("withBulkHasBulk"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Bulk = N INDEPENDENT per-row writes; per-target typed result; per-row in-flight marker; off-screen skip
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_bulkIssuesExactlyOneRequestPerSelectedOnScreenTarget() {
		var bulk = sub("bulk");
		// Four ids were "selected" but only three ('1','2','3') have an on-screen row; '9' must be silently
		// skipped rather than targeted - never a fourth, aggregate, or missing-target request.
		assertEquals(3L, ((Number) bulk.get("fetchCount")).longValue(), () -> report.toString());
		assertEquals(List.of("1", "2", "3"), bulk.get("targetIds"), () -> report.toString());
		assertEquals(List.of("ack", "ack", "ack"), bulk.get("actionIds"), () -> report.toString());
	}

	@Test void e02_eachTargetWasMarkedInFlightBeforeItsOwnRequest() {
		var bulk = sub("bulk");
		@SuppressWarnings("unchecked")
		var atFetch = (Map<String,Object>) bulk.get("inflightAtFetchTime");
		assertEquals(Boolean.TRUE, atFetch.get("1"), () -> report.toString());
		assertEquals(Boolean.TRUE, atFetch.get("2"), () -> report.toString());
		assertEquals(Boolean.TRUE, atFetch.get("3"), () -> report.toString());
	}

	@Test void e03_everyTargetsInFlightMarkerClearedOnItsOwnTerminalOutcome() {
		// MED-4: per-target marker clearing on every terminal outcome - success, failure, AND refusal.
		var bulk = sub("bulk");
		@SuppressWarnings("unchecked")
		var after = (Map<String,Object>) bulk.get("inflightAfter");
		assertEquals(Boolean.FALSE, after.get("1"), () -> report.toString());
		assertEquals(Boolean.FALSE, after.get("2"), () -> report.toString());
		assertEquals(Boolean.FALSE, after.get("3"), () -> report.toString());
	}

	@Test void e04_perTargetResultsAreIndependent_noAggregateSuccessMasksAFailure() {
		// The load-bearing HIGH-5/MED-4 case: target '1' succeeds, '2' fails, '3' is refused - all three render
		// their OWN outcome; none is hidden behind (or overwritten by) another target's result.
		var bulk = sub("bulk");
		@SuppressWarnings("unchecked")
		var o1 = (Map<String,Object>) bulk.get("outcome1");
		@SuppressWarnings("unchecked")
		var o2 = (Map<String,Object>) bulk.get("outcome2");
		@SuppressWarnings("unchecked")
		var o3 = (Map<String,Object>) bulk.get("outcome3");
		assertEquals("success", o1.get("state"), () -> report.toString());
		assertTrue(String.valueOf(o1.get("text")).contains("Done"), () -> report.toString());
		assertEquals("failure", o2.get("state"), () -> report.toString());
		assertTrue(String.valueOf(o2.get("text")).contains("nope"), () -> report.toString());
		assertEquals("refusal", o3.get("state"), () -> report.toString());
		assertTrue(String.valueOf(o3.get("text")).contains("write-guard:not-armed"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) The bulk toolbar live-gates its buttons on the selection count
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_toolbarButtonsAreDisabledUntilSomethingIsSelected() {
		var toolbar = sub("toolbar");
		assertEquals(Boolean.TRUE, toolbar.get("disabledInitially"), () -> report.toString());
		assertEquals(Boolean.FALSE, toolbar.get("disabledWithSelection"), () -> report.toString());
		assertTrue(String.valueOf(toolbar.get("countTextWithSelection")).contains("2"), () -> report.toString());
		assertEquals(Boolean.TRUE, toolbar.get("disabledAfterCleared"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The independently-versioned bulk sidecar is actually read + contract-checked at runtime (R2)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_bulkSidecarIsReadAndContractChecked() {
		var sidecar = sub("bulkSidecar");
		assertEquals(BulkMutateDef.CONTRACT_VERSION, sidecar.get("contractVersion"), () -> report.toString());
		assertEquals(1L, ((Number) sidecar.get("actionCount")).longValue(), () -> report.toString());
		assertEquals(Boolean.TRUE, sidecar.get("missingSidecarReturnsNull"), () -> report.toString());
	}
}
