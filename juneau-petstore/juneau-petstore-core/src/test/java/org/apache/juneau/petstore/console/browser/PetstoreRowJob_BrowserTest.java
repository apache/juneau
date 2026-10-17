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
package org.apache.juneau.petstore.console.browser;

import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Row jobs in the real browser: the species table's job-log detail region keys, submits and streams a restock for
 * its own row, holds the poll (the pill says Paused) while the open panel's job runs, writes the result into the
 * panel's {@code available} slot, re-attaches after a collapse, and cancels.  A key for one row is refused for another.  A
 * groom run's "Groom again" button runs through {@code JuneauViews.jobs} and starts a new run.
 */
@Covers(value="async-job", pages="/console/ops/jobs")
@HookCovers("jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreRowJob_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	private static final String CSRF = "{'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Csrf-Token': document.body.dataset.juneauCsrf}";

	/** JS: the species table's DOG row. */
	private static final String DOG_ROW = "[...document.querySelectorAll('#species-body tbody tr')].find(tr => [...tr.cells].some(td => td.textContent === 'DOG'))";

	/** The DOG panel's job-log region. */
	private static final String R = "#species-body .juneau-view-detail-panel .jc-joblog-region";

	private static final String PILL = ":is(#species, #species-body) .juneau-view-staleness";

	private static final String AVAILABLE = "fetch('/console/ops/jobs/species', {headers: {Accept: 'application/json'}}).then(r => r.json()).then(rows => rows.find(x => x.id === 'DOG').available)";

	// The first tab's DOG key, replayed for CAT the way a devtools fetch would.
	private static final String RETARGET = "(async () => {"
		+ " const k = await (await fetch('/console/ops/jobs/species/DOG/key', {method: 'POST', credentials: 'same-origin', headers: " + CSRF + ", body: '{}'})).json();"
		+ " const r = await fetch('/console/ops/jobs/species/CAT/restock', {method: 'POST', credentials: 'same-origin', headers: " + CSRF + ","
		+ "   body: JSON.stringify({action: 'restock-species', targetId: 'CAT', idempotencyKey: k.idempotencyKey, fields: {count: 1}})});"
		+ " return {status: r.status, code: (await r.json()).refusalCode};"
		+ "})()";

	private static final String GROOM_COUNT = "fetch('/console/ops/jobs/groom-rows', {headers: {Accept: 'application/json'}}).then(r => r.json()).then(r => r.length)";

	private static Map<String,Object> wait(String js, int ms) {
		return Map.of("waitFor", js, "timeoutMs", ms);
	}

	private static Map<String,Object> phase(String p, int ms) {
		return wait("document.querySelector('" + R + "[data-phase=" + p + "]')", ms);
	}

	private static Map<String,Object> toggleDog() {
		return Map.of("evaluate", DOG_ROW + ".querySelector('.juneau-view-detail-toggle').click()");
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("row-job", base, List.of(
			// The retargeted submit's 409 is expected.
			Map.of("name", "species", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(
					wait(DOG_ROW, 8000),
					Map.of("evaluate", AVAILABLE + ".then(n => { window.__before = n; })"), wait("window.__before != null", 8000),
					toggleDog(), phase("idle", 8000),
					Map.of("fill", List.of(R + " .jc-joblog-input[name=count]", "3")),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("running", 8000),
					// Decision 12: while the open panel's job runs, the timer's draw is held and the pill says so.
					wait("document.querySelector('" + PILL + "[data-state=paused]')", 4000),
					Map.of("evaluate", "window.__during = {marker: " + DOG_ROW + ".hasAttribute('data-juneau-job'), pill: document.querySelector('" + PILL + "').dataset.state}"),
					phase("done", 15000),
					wait("document.querySelector('" + PILL + "[data-state=fresh]')", 8000),
					Map.of("evaluate", RETARGET + ".then(v => { window.__retarget = v; })"), wait("window.__retarget && window.__retarget.status", 8000)),
				"queries", Map.of(
					"before", "window.__before",
					"during", "window.__during",
					"lines", "[...document.querySelectorAll('" + R + " .jc-joblog-line')].map(l => l.textContent)",
					"slot", "document.querySelector('" + R + " [data-juneau-field=available]')?.textContent ?? null",
					"markerAfter", DOG_ROW + ".hasAttribute('data-juneau-job')",
					"retarget", "window.__retarget")),
			Map.of("name", "reattach", "path", "/console/ops/jobs",
				"actions", List.of(
					wait(DOG_ROW, 8000), toggleDog(), phase("idle", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("running", 8000),
					toggleDog(), wait("!document.querySelector('" + R + "')", 4000),
					toggleDog(), phase("done", 15000)),
				"queries", Map.of("lines", "[...document.querySelectorAll('" + R + " .jc-joblog-line')].map(l => l.textContent)")),
			// The running form's submit button is its cancel; the second click lands in the restock's hold.
			Map.of("name", "cancel", "path", "/console/ops/jobs",
				"actions", List.of(
					wait(DOG_ROW, 8000), toggleDog(), phase("idle", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("running", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("done", 15000)),
				"queries", Map.of("banner", "document.querySelector('" + R + " .jc-joblog-banner')?.textContent ?? null")),
			Map.of("name", "regroom", "path", "/console/ops/jobs",
				"actions", List.of(
					wait("document.querySelector('#groom-runs-body tbody tr .juneau-view-detail-toggle')", 8000),
					Map.of("evaluate", GROOM_COUNT + ".then(n => { window.__runs = n; })"), wait("window.__runs != null", 8000),
					Map.of("click", "#groom-runs-body tbody tr .juneau-view-detail-toggle"),
					wait("document.querySelector('#groom-runs-body .juneau-view-detail-panel .juneau-view-helper-job-btn')", 8000),
					Map.of("click", "#groom-runs-body .juneau-view-detail-panel .juneau-view-helper-job-btn"),
					Map.of("evaluate", "window.__poll = setInterval(() => " + GROOM_COUNT + ".then(n => { window.__after = n; }), 200)"),
					wait("window.__after > window.__runs", 8000),
					Map.of("evaluate", "clearInterval(window.__poll)")),
				"queries", Map.of("runs", "[window.__runs, window.__after]",
					"label", "document.querySelector('#groom-runs-body .juneau-view-helper-job-btn')?.textContent ?? null"))));
	}

	@Test void a01_aPanelRestockStreamsHoldsThePollAndWritesItsResult() {
		var c = assertClean(report, "species");
		var before = ((Number)query(c, "before")).intValue();
		assertBean(query(c, "during"), "marker,pill", "true,paused");
		assertContains("Restocking 3 pets", String.valueOf(query(c, "lines")));
		assertString(String.valueOf(before + 3), query(c, "slot"));
		assertEquals(false, query(c, "markerAfter"));
	}

	@Test void a02_aKeyForOneRowIsRefusedForAnother() {
		assertBean(query(assertClean(report, "species"), "retarget"), "status,code", "409,write-guard:idempotency-binding-mismatch");
	}

	@Test void a03_collapseAndReexpandReattaches() {
		assertContains("Restocking 1 pets", String.valueOf(query(assertClean(report, "reattach"), "lines")));
	}

	@Test void a05_cancelEndsTheRowJob() {
		assertContains("ancel", query(assertClean(report, "cancel"), "banner"));
	}

	@Test void a04_groomAgainStartsANewRun() {
		var c = assertClean(report, "regroom");
		assertString("Groom again", query(c, "label"));
		@SuppressWarnings("unchecked")
		var runs = (List<Number>)query(c, "runs");
		assertTrue(runs.get(1).intValue() > runs.get(0).intValue(), runs::toString);
	}
}
