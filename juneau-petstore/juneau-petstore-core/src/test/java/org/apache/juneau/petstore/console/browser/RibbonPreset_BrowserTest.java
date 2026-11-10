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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The standard ribbon preset in the real browser: slot order, a single column button inside the ribbon, distinct filter
 * icons, the export buttons, and the conditional collapse and columns items.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class RibbonPreset_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static String ids(String body) {
		return "[...document.querySelectorAll('#" + body + "-body [data-juneau-ribbon-id]')].map(e => e.getAttribute('data-juneau-ribbon-id')).join(',')";
	}

	private static String icons(String body) {
		return "[...document.querySelectorAll('#" + body + "-body .juneau-view-ribbon-btn use')].map(u => u.getAttribute('href') || u.getAttribute('xlink:href'))";
	}

	private static Map<String,Object> page(String name, String path, String body) {
		return Map.of("name", name, "path", path,
			"actions", List.of(Map.of("waitFor", "document.querySelector('#" + body + " tbody tr[data-juneau-row-id]')")),
			"queries", Map.of(
				"ids", ids(body),
				"icons", icons(body),
				"strayChooser", "document.querySelectorAll('#" + body + "-body .juneau-config-chooser-btn').length",
				"chooserInRibbon", "document.querySelectorAll('#" + body + "-body [data-juneau-ribbon-id=columns]').length"));
	}

	private static final String HEADERS = "[...document.querySelectorAll('#%s thead tr:first-child th')].map(t => t.textContent.trim()).join('|')";

	private static final String GEOM = "(() => {"
		+ "const r = id => { const b = document.querySelector('#%s-body [data-juneau-ribbon-id=' + id + ']'); const g = b && b.closest('.juneau-view-ribbon-group'); if (!g) return null;"
		+ " const k = [...g.querySelectorAll('.juneau-view-ribbon-btn')]; const f = getComputedStyle(k[0]), l = getComputedStyle(k[k.length - 1]);"
		+ " return [f.borderTopLeftRadius, f.borderBottomLeftRadius, l.borderTopRightRadius, l.borderBottomRightRadius, k.length > 1 ? getComputedStyle(k[1]).borderTopLeftRadius : '0px'].join(','); };"
		+ "return { exportGroup: r('copy'), viewingGroup: r('columns') }; })()";

	/** Opens the ribbon's columns button, removes a column in the dialog, applies, and reloads. */
	private static Map<String,Object> columnsCase(String name, String path, String body, String col, String label) {
		var rows = "document.querySelector('#" + body + " tbody tr[data-juneau-row-id]')";
		var headers = String.format(HEADERS, body);
		return Map.of("name", name, "path", path,
			"actions", List.of(
				Map.of("waitFor", rows),
				Map.of("click", "#" + body + "-body [data-juneau-ribbon-id=columns]"),
				Map.of("waitFor", "document.querySelector('.juneau-config-dialog')"),
				Map.of("click", ".juneau-config-col-row[data-col=\"" + col + "\"] .juneau-config-col-remove"),
				Map.of("click", ".juneau-config-apply"),
				Map.of("waitFor", "!" + headers + ".split('|').includes('" + label + "')"),
				Map.of("evaluate", "window.__hiddenOk = true"),
				Map.of("reload", true),
				Map.of("waitFor", rows)),
			"queries", Map.of("headers", headers));
	}

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("ribbon-preset", base, List.of(
			page("pets", "/console/pets", "pets"),
			page("petsBuilder", "/console/pets?builder", "pets"),
			page("store", "/console/store", "stats"),
			page("groom", "/console/ops/jobs", "groom-runs"),
			columnsCase("petsColumns", "/console/pets", "pets", "price", "Price (USD)"),
			columnsCase("storeColumns", "/console/store", "stats", "status", "Status"),
			geometry("pets", "/console/pets", "pets")));
	}

	private static Map<String,Object> geometry(String name, String path, String body) {
		return Map.of("name", name + "Geometry", "path", path,
			"actions", List.of(Map.of("waitFor", "document.querySelector('#" + body + " tbody tr[data-juneau-row-id]')")),
			"queries", Map.of(
				"geom", String.format(GEOM, body),
				"collapseAnywhere", "document.querySelectorAll('[data-juneau-ribbon-id=collapse]').length",
				"stale", "(() => { const e = document.querySelector('#" + body + "-body .juneau-view-staleness'); return e ? [e.getAttribute('role'), e.getAttribute('aria-label'), e.getAttribute('data-jc-tip')].join('|') : null; })()",
				"filterIcons", "[...document.querySelectorAll('#" + body + "-body .juneau-view-ribbon-btn[data-juneau-ribbon-id]')].filter(b => b.querySelector('use') && !['copy','csv','excel','pdf','link','columns','reset','refresh','search','add-pet'].includes(b.getAttribute('data-juneau-ribbon-id'))).map(b => b.getAttribute('data-juneau-ribbon-id') + '=' + b.querySelector('use').getAttribute('href'))"));
	}

	private static String q(String name, String key) {
		return String.valueOf(query(assertClean(report, name), key));
	}

	@Test void a01_petsSlotOrderHasNoCollapse() {
		for (var n : new String[] {"pets", "petsBuilder"}) {
			var ids = q(n, "ids");
			assertFalse(ids.contains("collapse"), () -> n + ": " + ids);
			var order = List.of("search", "add-pet", "status", "species", "copy", "csv", "excel", "pdf", "link", "columns", "reset", "refresh");
			var last = -1;
			for (var id : order) {
				var i = Arrays.asList(ids.split(",")).indexOf(id);
				if (i < 0 && (id.equals("status") || id.equals("species")))
					continue;
				assertTrue(i > last, () -> n + ": '" + id + "' out of order in " + ids);
				last = i;
			}
		}
	}

	@Test void a02_oneColumnButtonInsideTheRibbon() {
		assertEquals("1", q("pets", "chooserInRibbon"));
		assertEquals("1", q("pets", "strayChooser"), "the only chooser button is the ribbon's own");
	}

	@Test void a03_groomShowsCollapse() {
		assertTrue(q("groom", "ids").contains("collapse"), () -> q("groom", "ids"));
		assertTrue(q("groom", "ids").contains("groom-refresh"), () -> q("groom", "ids"));
	}

	@Test void a04_storeShowsColumns() {
		assertTrue(q("store", "ids").contains("columns"), () -> q("store", "ids"));
	}

	@Test void a05_iconsAreDistinctAndExportButtonsPresent() {
		for (var n : new String[] {"pets", "petsBuilder"}) {
			var ids = q(n, "ids");
			assertTrue(ids.contains("excel") && ids.contains("pdf"), () -> n + ": " + ids);
			var icons = (List<?>)query(assertClean(report, n), "icons");
			assertFalse(icons.isEmpty(), n);
			assertTrue(icons.stream().allMatch(x -> x != null && ! String.valueOf(x).isEmpty()), () -> n + ": " + icons);
		}
	}

	@Test void a06_ribbonColumnsButtonOpensDialogAndHideSurvivesReload() {
		for (var n : new String[] {"petsColumns", "storeColumns"}) {
			var c = assertClean(report, n);
			assertFalse(c.get("timeouts") != null && ! ((List<?>)c.get("timeouts")).isEmpty(), () -> n + " timeouts: " + c.get("timeouts"));
			var h = String.valueOf(query(c, "headers"));
			assertFalse(h.contains("Price (USD)") && n.startsWith("pets"), () -> n + ": " + h);
			assertFalse(h.contains("Status") && n.startsWith("store"), () -> n + ": " + h);
		}
	}

	@Test void a07_exportGroupEdgesMirrorViewing() {
		var g = (Map<?,?>)query(assertClean(report, "petsGeometry"), "geom");
		assertEquals(g.get("viewingGroup"), g.get("exportGroup"), () -> "group edge radii differ: " + g);
		assertNotNull(g.get("exportGroup"));
	}

	@Test void a08_hiddenItemsLeaveNoHandleAndPollingDotIsLabelled() {
		var c = assertClean(report, "petsGeometry");
		assertEquals("0", String.valueOf(query(c, "collapseAnywhere")));
		var stale = String.valueOf(query(c, "stale"));
		assertTrue(stale.startsWith("img|"), stale);
		assertFalse(stale.endsWith("|null"), stale);
	}

	@Test void a09_filterIconsAreDistinctInTheBrowser() {
		var icons = (List<?>)query(assertClean(report, "petsGeometry"), "filterIcons");
		assertFalse(icons.isEmpty());
		var hrefs = icons.stream().map(x -> String.valueOf(x).split("=", 2)[1]).toList();
		assertEquals(hrefs.size(), new HashSet<>(hrefs).size(), () -> "duplicate filter icons: " + icons);
	}
}
