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
 * Bulk selection on P2 Pets in the real browser: the selection adds up across pages and survives a search, and the
 * per-row Mark sold action runs at most four writes at a time and ends in one summary toast.
 */
@Covers(value="bulk-select", pages="/console/pets")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreBulkSelect_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String READY = "document.querySelector('#pets tbody tr[data-juneau-row-id]')";
	private static final String HEADER_BOX = "#pets thead input.juneau-view-select-all-checkbox";
	private static final String ROW_BOX = "input.juneau-view-select-checkbox";

	/** The selected count, from the table's retained selection topic. */
	private static final String COUNT = "(window.JuneauViews.bus.get('selection:pets') || {}).count";

	/** The header checkbox state: mixed, true or false. */
	private static final String HEADER = "(() => { const h = document.querySelector('" + HEADER_BOX + "'); return h.indeterminate ? 'mixed' : String(h.checked); })()";

	/** Wraps fetch to record the peak and total number of sell requests in flight. */
	private static final String COUNT_SELLS = "window.__inflight = 0; window.__peak = 0; window.__sells = 0; const f = window.fetch;"
		+ " window.fetch = async (...a) => { const sell = String(a[0]).endsWith('/sell'); if (sell) { window.__sells++; window.__inflight++;"
		+ " window.__peak = Math.max(window.__peak, window.__inflight); } try { return await f(...a); } finally { if (sell) window.__inflight--; } }";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("bulk-select", base, List.of(
			Map.of("name", "across-pages", "path", "/console/pets", "capture", "/console/pets/query",
				"actions", List.of(
					Map.of("waitFor", READY),
					Map.of("click", HEADER_BOX),
					Map.of("evaluate", "window.__pageOne = " + COUNT + "; window.__rows = document.querySelectorAll('#pets tbody tr[data-juneau-row-id]').length"),
					Map.of("click", "#pets-body .juneau-view-pagingpill-btn[aria-label=\"Next page\"]"),
					Map.of("waitForCaptured", 2),
					Map.of("waitFor", "document.querySelector('#pets tbody " + ROW_BOX + ":not(:checked)')"),
					Map.of("click", "#pets tbody tr:first-child " + ROW_BOX),
					Map.of("evaluate", "window.__count = " + COUNT + "; window.__header = " + HEADER),
					Map.of("fill", List.of("#pets-body input[type=search]", "a")),
					Map.of("waitForCaptured", 3, "timeoutMs", 5000),
					Map.of("waitFor", READY)),
				"queries", Map.of(
					"pageOne", "window.__pageOne",
					"rows", "window.__rows",
					"count", "window.__count",
					"header", "window.__header",
					"countAfterSearch", COUNT,
					"toolbar", "document.querySelector('#pets-body .juneau-view-bulk-count').textContent")),
			Map.of("name", "mark-sold", "path", "/console/pets",
				"actions", List.of(
					Map.of("waitFor", READY),
					Map.of("evaluate", COUNT_SELLS),
					Map.of("click", HEADER_BOX),
					Map.of("evaluate", "window.__selected = " + COUNT),
					Map.of("click", "#pets-body .juneau-view-bulk-action-btn"),
					Map.of("waitFor", "document.querySelector('[role=dialog]')"),
					Map.of("evaluate", "window.__title = document.querySelector('[role=dialog] .juneau-view-dialog-title').textContent"),
					Map.of("click", "[role=dialog] .juneau-view-dialog-confirm"),
					Map.of("waitFor", "document.querySelector('.juneau-view-bulk-toast[role=status]')")),
				"queries", Map.of(
					"selected", "window.__selected",
					"title", "window.__title",
					"peak", "window.__peak",
					"sells", "window.__sells",
					"toast", "document.querySelector('.juneau-view-bulk-toast[role=status]').textContent",
					"left", COUNT))));
	}

	/** Page one's rows, which the header checkbox selects, plus the one row picked on page two. */
	private static int expected(Map<String,Object> c) {
		var pageOne = ((Number) query(c, "pageOne")).intValue();
		assertTrue(pageOne > 0, "the header checkbox selected nothing");
		assertEquals(((Number) query(c, "rows")).intValue(), pageOne);
		return pageOne + 1;
	}

	@Test void a01_selectionAddsUpAcrossPages() {
		var c = assertClean(report, "across-pages");
		assertString("mixed", query(c, "header"));
		assertEquals(expected(c), ((Number) query(c, "count")).intValue());
	}

	@Test void a02_selectionSurvivesSearch() {
		var c = assertClean(report, "across-pages");
		var n = expected(c);
		assertEquals(n, ((Number) query(c, "countAfterSearch")).intValue());
		assertString(n + " selected ·", query(c, "toolbar"));
	}

	@Test void a03_perRowMarkSoldIsAtMostFourConcurrent() {
		var c = assertClean(report, "mark-sold");
		var peak = ((Number) query(c, "peak")).intValue();
		assertTrue(peak >= 1 && peak <= 4, () -> "peak " + peak);
		assertEquals(((Number) query(c, "selected")).intValue(), ((Number) query(c, "sells")).intValue());
	}

	@Test void a04_runEndsInOneSummaryAndClearsTheSelection() {
		var c = assertClean(report, "mark-sold");
		assertString("Mark pets sold", query(c, "title"));
		assertString("Mark sold: " + query(c, "selected") + " succeeded", query(c, "toast"));
		assertEquals(0, ((Number) query(c, "left")).intValue());
	}
}
