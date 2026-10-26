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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Server-mode tables in the real browser.
 *
 * <p>
 * P7 Audit: the table POSTs a DataTables request to the BeanQuery endpoint, and a global search sends a new request
 * carrying the search value.  P2 Pets: the ribbon's default option is in the first request, a header click posts the
 * sort order, a user search is sent alongside the ribbon's column filter, and the ribbon's CSV button downloads
 * the rows the table shows.
 */
@Covers(value="export", pages="/console/pets")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreDatatable_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	/** An audit entity the seed data guarantees (its 300 audit rows are Pet, Order and User entries), so the search has hits. */
	private static final String SEARCH = "Order";

	private static final String ROWS = "[...document.querySelectorAll('#audit-body tbody tr')].filter(tr => !tr.querySelector('.dt-empty')).map(tr => tr.textContent)";

	private static final String PETS_QUERY = "/console/pets/query";

	private static final String PETS_ROW_COUNT = "document.querySelectorAll('#pets tbody tr[data-juneau-row-id]').length";

	/** The Price column's index in the posted order (column 0 is the selection checkbox). */
	private static final int PRICE_COLUMN = 5;

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("datatable", base, List.of(
			Map.of("name", "audit", "path", "/console/ops/audit", "capture", "/console/ops/audit/query",
				"actions", List.of(
					Map.of("waitForCaptured", 1),
					Map.of("waitFor", "document.querySelector('#audit-body tbody tr td')"),
					Map.of("fill", List.of("#audit-body input[type=search]", SEARCH)),
					Map.of("waitForCaptured", 2, "timeoutMs", 5000),
					// The POST returning is not the redraw: wait until the table shows only rows the search matched.
					Map.of("waitFor", "(() => { const r = [...document.querySelectorAll('#audit-body tbody tr')]; return r.length > 0 && r.every(tr => !tr.querySelector('.dt-empty') && tr.textContent.includes('" + SEARCH + "')); })()")),
				"queries", Map.of("rows", ROWS)),
			Map.of("name", "pets-default", "path", "/console/pets", "capture", PETS_QUERY,
				"actions", List.of(Map.of("waitForCaptured", 1))),
			Map.of("name", "pets-sort", "path", "/console/pets", "capture", PETS_QUERY,
				"actions", List.of(
					Map.of("waitForCaptured", 1),
					Map.of("waitFor", "document.querySelector('#pets tbody tr td')"),
					Map.of("click", "#pets thead .juneau-view-col-sort-icon[aria-label=\"Sort Price (USD)\"]"),
					Map.of("waitForCaptured", 2))),
			Map.of("name", "pets-merge", "path", "/console/pets", "capture", PETS_QUERY,
				"actions", List.of(
					Map.of("waitForCaptured", 1),
					Map.of("waitFor", "document.querySelector('#pets tbody tr td')"),
					Map.of("fill", List.of("#pets-body input[type=search]", "rex")),
					Map.of("waitForCaptured", 2, "timeoutMs", 5000))),
			Map.of("name", "pets-settings", "path", "/console/pets", "capture", PETS_QUERY,
				"actions", List.of(
					Map.of("waitFor", PETS_ROW_COUNT + " > 0"),
					Map.of("click", "#pets-body .juneau-config-chooser-btn"),
					Map.of("waitFor", "document.querySelector('.juneau-config-dialog')"),
					Map.of("click", ".juneau-config-tab[data-tab=options]"),
					Map.of("evaluate", "(() => { const s = document.querySelector('.juneau-config-opt-pagesize select'); s.value = '50'; s.dispatchEvent(new Event('change', {bubbles: true})); })()"),
					Map.of("click", ".juneau-config-apply"),
					Map.of("waitFor", PETS_ROW_COUNT + " === 50"),
					Map.of("reload", true),
					Map.of("waitFor", PETS_ROW_COUNT + " === 50")),
				"queries", Map.of("count", PETS_ROW_COUNT)),
			Map.of("name", "pets-export", "path", "/console/pets",
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('#pets tbody tr[data-juneau-row-id]')"),
					Map.of("download", "#pets-body .juneau-view-ribbon-btn[aria-label=\"csv\"]", "as", "csv")),
				"queries", Map.of(
					"rows", "document.querySelectorAll('#pets tbody tr[data-juneau-row-id]').length",
					"buttons", "[...document.querySelectorAll('#pets-body .juneau-view-ribbon-btn')].map(b => b.getAttribute('aria-label'))"))));
	}

	@SuppressWarnings({
		"unchecked" // requests is a JSON array of {url, method, postData}.
	})
	private static List<Map<String,Object>> requests() {
		return (List<Map<String,Object>>) assertClean(report, "audit").get("requests");
	}

	private static Map<?,?> body(Map<String,Object> request) throws Exception {
		return Json.to((String) request.get("postData"), Map.class);
	}

	@Test void a01_firstLoadPostsADataTablesRequest() throws Exception {
		var r = requests().get(0);
		assertString("POST", r.get("method"));
		var b = body(r);
		assertBean(b, "draw,start,length", "1,0,25");  // views toolkit default page size: juneau-views.js PAGE_SIZE_OPTIONS[0] = 25
		assertContainsAll(String.valueOf(b.get("columns")), "at", "actor", "action", "entity", "entityId", "detail");
	}

	@Test void a02_globalSearchSendsTheValue() throws Exception {
		var reqs = requests();
		assertContains(SEARCH, String.valueOf(body(reqs.get(reqs.size() - 1)).get("search")));
	}

	@Test void a03_onlyRowsMatchingTheSearchRender() {
		var rows = (List<?>) query(assertClean(report, "audit"), "rows");  // DataTables' "no data" row is filtered out
		assertNotEmpty(rows);
		for (var row : rows)
			assertContains(SEARCH, row);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - P2 pets, server mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_ribbonDefaultOnFirstLoad() throws Exception {
		var req = lastCaptured(assertClean(report, "pets-default"), PETS_QUERY);
		assertContainsAll(Json.of(req.get("columns")), "$eq(AVAILABLE)", "$in(DOG,CAT,MOUSE,RABBIT)");
	}

	@Test void b02_sortPostsOrder() throws Exception {
		var req = lastCaptured(assertClean(report, "pets-sort"), PETS_QUERY);
		assertBean(req, "order{0{column}}", "{{" + PRICE_COLUMN + "}}");
	}

	@Test void b03_ribbonAndUserSearchMerge() throws Exception {
		var req = lastCaptured(assertClean(report, "pets-merge"), PETS_QUERY);
		assertContains("rex", String.valueOf(req.get("search")));
		assertContainsAll(Json.of(req.get("columns")), "$eq(AVAILABLE)", "$in(DOG,CAT,MOUSE,RABBIT)");
	}

	/** The gear opens View Settings; page size 50 on the Options tab is applied, and survives a reload (localStorage). */
	@SuppressWarnings({
		"unchecked" // requests is a JSON array of {url, method, postData}.
	})
	@Test void b04_viewSettingsPageSizePersistsAcrossReload() throws Exception {
		var c = assertClean(report, "pets-settings");
		var first = Json.to((String) ((List<Map<String,Object>>) c.get("requests")).get(0).get("postData"), Map.class);
		assertEquals(25, ((Number) first.get("length")).intValue());
		assertEquals(50, ((Number) query(c, "count")).intValue());
		assertEquals(50, ((Number) lastCaptured(c, PETS_QUERY).get("length")).intValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - P2 export
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_exportButtonsRender() {
		var buttons = String.valueOf(query(assertClean(report, "pets-export"), "buttons"));
		assertContainsAll(buttons, "copy", "csv");
		assertNotContains(() -> buttons, "excel", buttons);
	}

	@SuppressWarnings({
		"unchecked" // downloads values are {name, bytes, head}.
	})
	@Test void c02_csvExportDownloadsTheShownRows() {
		var c = assertClean(report, "pets-export");
		var d = (Map<String,Object>) ((Map<String,Object>) c.get("downloads")).get("csv");
		assertNotNull(d, () -> "no csv download: " + c.get("downloads"));
		assertTrue(String.valueOf(d.get("name")).endsWith(".csv"), () -> "download name: " + d.get("name"));
		var lines = String.valueOf(d.get("head")).strip().split("\\R");
		assertContainsAll(lines[0], "\"Id\"", "\"Name\"", "\"Species\"", "\"Price (USD)\"");
		var rows = ((Number) query(c, "rows")).intValue();
		assertTrue(rows > 0, "the table showed no rows");
		assertEquals(rows + 1, lines.length, () -> String.valueOf(d.get("head")));
		for (var i = 1; i < lines.length; i++)
			assertContains("AVAILABLE", lines[i]);
	}
}
