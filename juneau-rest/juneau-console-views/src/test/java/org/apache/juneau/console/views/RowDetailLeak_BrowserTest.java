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
package org.apache.juneau.console.views;

import static org.apache.juneau.console.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Row-detail tabs that own live resources (a chart, a nested table) in a real browser: collapsing a row and resetting
 * a tab must both release what the tab built, so repeated cycles leave the live chart, table and region counts where
 * they started.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
class RowDetailLeak_BrowserTest extends TestBase {

	private static final String CHARTS = "/juneau-console/juneau-charts.js";
	private static final String ECHARTS = "/webjars/echarts/6.1.0/dist/echarts.min.js";
	private static final String ECHARTS_RESOURCE = "/META-INF/resources" + ECHARTS;

	private static final String HELPERS = "window.__w = function (fn) { return new Promise(function (res) { var n = 0; (function t() { if (fn()) res(true); else if (n++ > 200) res(false); else setTimeout(t, 50); })(); }); };"
		+ "window.__tabs = function () { return Array.from(document.querySelectorAll('#pets-body [role=tab]')); };"
		+ "window.__charts = function () { return window.JuneauCharts.__liveCount(); };"
		+ "window.__tables = function () { return window.jQuery.fn.dataTable.tables().length; };"
		+ "window.__regions = function () { return document.querySelectorAll('[data-juneau-region]').length; };";

	private static String page() throws Exception {
		var orders = TableSpec.create("pet-orders").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/orders")
			.columns(Column.create("id").label("Id"));
		var trend = RegionDef.chart("trend", "/api/trend").x("day").series("placed", "Placed").height(160);
		var detail = RowDetail.create("/api/pets/{id}/detail").title("Pet {id}")
			.tab(DetailTab.fields("Info", "name").id("info"))
			.tab(DetailTab.chart("Trend", trend).id("trend"))
			.tab(DetailTab.table("Orders", orders).id("orders").scopeParam("petId"));
		var t = TableSpec.create("pets").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/pets")
			.selection(SelectionDef.create("id"))
			.columns(Column.create("id").label("Id"), Column.create("name").label("Name"))
			.detail(detail);
		var card = new LinkedHashMap<String,Object>();
		card.put("id", "pets");
		card.put("type", "datatables");
		card.put("title", "Pets");
		card.put("table", t.toCardBody());
		var page = new LinkedHashMap<String,Object>();
		page.put("contractVersion", "1");
		page.put("modelVersion", "1");
		page.put("title", "Pets");
		page.put("cards", List.of(card));
		return contractPage(Json.of(page), "", "", viewsPack()
			+ "<script src=\"" + ECHARTS + "\"></script>\n<script src=\"" + CHARTS + "\"></script>\n");
	}

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void run() throws Exception {
		var cycle = "var p = window.__probe = window.__probe || {}; (async function () { try {"
			+ "  var tog = function () { document.querySelector('#pets-body .juneau-view-detail-toggle').click(); };"
			+ "  p.base = { c: __charts(), t: __tables() };"
			+ "  p.afterCollapse = [];"
			+ "  for (var i = 0; i < 4; i++) {"
			+ "    tog(); await __w(function () { return __tabs().length === 3; });"
			+ "    __tabs()[1].click(); await __w(function () { return __charts() > p.base.c; });"
			+ "    __tabs()[2].click(); await __w(function () { return document.querySelector('#pets-body .juneau-view-detail-panel table.dataTable'); });"
			+ "    await new Promise(function (r) { setTimeout(r, 200); });"
			+ "    tog(); await __w(function () { return __tabs().length === 0; });"
			+ "    await new Promise(function (r) { setTimeout(r, 200); });"
			+ "    p.afterCollapse.push({ c: __charts(), t: __tables(), r: __regions() });"
			+ "  }"
			+ "  p.collapseDone = true;"
			+ "} catch (e) { p.err = String(e && e.stack || e); } })();";
		var reset = "var p = window.__probe = window.__probe || {}; (async function () { try {"
			+ "  document.querySelector('#pets-body .juneau-view-detail-toggle').click();"
			+ "  await __w(function () { return __tabs().length === 3; });"
			+ "  __tabs()[1].click(); await __w(function () { return __charts() >= 1; });"
			+ "  await new Promise(function (r) { setTimeout(r, 300); });"
			+ "  p.paneTail = String((document.querySelector('#pets-body [role=tabpanel]:not([hidden])') || {}).textContent).slice(0, 300); p.hasEc = typeof window.echarts + typeof window.JuneauCharts;"
			+ "  p.resetBase = { c: __charts(), r: __regions() }; p.resetSeries = [];"
			+ "  for (var i = 0; i < 4; i++) {"
			+ "    document.querySelector('#pets-body .juneau-view-detail-panel').invalidateAll();"
			+ "    await new Promise(function (r) { setTimeout(r, 500); });"
			+ "    p.resetSeries.push({ c: __charts(), r: __regions() });"
			+ "  }"
			+ "  __tabs()[2].click(); await __w(function () { return document.querySelector('#pets-body .juneau-view-detail-panel table.dataTable'); });"
			+ "  await new Promise(function (r) { setTimeout(r, 300); });"
			+ "  p.tblBase = __tables(); p.tblSeries = [];"
			+ "  for (var j = 0; j < 4; j++) {"
			+ "    document.querySelector('#pets-body .juneau-view-detail-panel').invalidateAll();"
			+ "    await new Promise(function (r) { setTimeout(r, 600); });"
			+ "    p.tblSeries.push(__tables());"
			+ "  }"
			+ "  p.resetDone = true;"
			+ "} catch (e) { p.err = String(e && e.stack || e); } })();";
		var nested = "var p = window.__probe = window.__probe || {}; (async function () { try {"
			+ "  document.querySelector('#pets-body .juneau-view-detail-toggle').click();"
			+ "  await __w(function () { return __tabs().length === 3; });"
			+ "  __tabs()[2].click(); await __w(function () { return document.querySelector('#pets-body .juneau-view-detail-panel table.dataTable'); });"
			+ "  var pane = document.querySelector('#pets-body .juneau-view-detail-panel table.dataTable').closest('[role=tabpanel]');"
			+ "  var bar = document.createElement('div'); bar.className = 'juneau-model-actions'; bar.id = 'nested-bar'; pane.appendChild(bar);"
			+ "  var all = []; (function w(h) { all.push(h); h.children.forEach(w); })(JuneauViews.model.page());"
			+ "  var det = all.find(function (h) { return h.kind === 'rowdetail' && !h.template && h._el && h._el.contains(bar); });"
			+ "  p.found = !!det; p.contains = !!(det._el && det._el.contains(bar)); p.attached = det._attached; det._paintActions();"
			+ "  p.barKept = !!document.getElementById('nested-bar');"
			+ "  p.nestedDone = true;"
			+ "} catch (e) { p.err = String(e && e.stack || e); } })();";
		var f = ConsoleBrowserFixture.create("row-detail-leak")
			.asset(CHARTS, resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE), "text/javascript")
			.asset(ECHARTS, resource(ECHARTS_RESOURCE), "text/javascript")
			.asset("/api/pets", "[{\"id\":\"p1\",\"name\":\"Rex\"}]", "application/json")
			.asset("/api/orders", "[{\"id\":\"o1\"}]", "application/json")
			.asset("/api/trend", "{\"contractVersion\":\"1\",\"fields\":[{\"day\":\"Mon\",\"placed\":3},{\"day\":\"Tue\",\"placed\":5}]}", "application/json")
			.asset("/api/pets/p1/detail", "{\"contractVersion\":\"1\",\"fields\":{\"name\":\"Rex\"}}", "application/json");
		f.page("collapse", ORIGIN + "/collapse", page(), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").eval(HELPERS).eval(cycle).waitFor("body");
		f.page("reset", ORIGIN + "/reset", page(), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").eval(HELPERS).eval(reset).waitFor("body");
		f.page("nested", ORIGIN + "/nested", page(), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").eval(HELPERS).eval(nested).waitFor("body");
		report = f.run();
	}

	private static Map<String,Object> p(String name, String done) {
		var r = report.get(name);
		assertEquals(List.of(), r.get("pageErrors"), r::toString);
		var m = ConsoleBrowserFixture.probe(r);
		assertEquals(Boolean.TRUE, m.get(done), () -> String.valueOf(m.get("err")) + " " + m);
		return m;
	}

	@SuppressWarnings("unchecked")
	@Test void a01_collapsingRowsRepeatedly_releasesCharts_tablesAndRegions() {
		var m = p("collapse", "collapseDone");
		for (var s : (List<Map<String,Object>>)m.get("afterCollapse")) {
			assertEquals(0L, ((Number)s.get("c")).longValue(), m::toString);
			assertEquals(0L, ((Number)s.get("r")).longValue(), m::toString);
			assertEquals(((Number)((Map<String,Object>)m.get("base")).get("t")).longValue(), ((Number)s.get("t")).longValue(), m::toString);
		}
	}

	@SuppressWarnings("unchecked")
	@Test void a02_resettingChartTab_keepsOneLiveChart() {
		var m = p("reset", "resetDone");
		var base = ((Number)((Map<String,Object>)m.get("resetBase")).get("c")).longValue();
		for (var s : (List<Map<String,Object>>)m.get("resetSeries"))
			assertEquals(base, ((Number)s.get("c")).longValue(), m::toString);
	}

	@Test void a03_resettingTableTab_keepsLiveTableCountFlat() {
		var m = p("reset", "resetDone");
		var base = ((Number)m.get("tblBase")).longValue();
		for (var n : (List<?>)m.get("tblSeries"))
			assertEquals(base, ((Number)n).longValue(), m::toString);
	}

	@Test void a04_repaintingParentDetailActions_keepsNestedHandleActionBar() {
		var m = p("nested", "nestedDone");
		assertEquals(Boolean.TRUE, m.get("found"), m::toString);
		assertEquals(Boolean.TRUE, m.get("contains"), m::toString);
		assertEquals(Boolean.TRUE, m.get("barKept"), m::toString);
	}
}
