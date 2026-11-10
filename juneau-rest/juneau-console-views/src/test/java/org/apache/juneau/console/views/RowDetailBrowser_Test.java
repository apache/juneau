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

import org.apache.juneau.marshall.marshaller.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Tabbed row details in a real browser: a datatables card whose inline catalog declares a row detail with two
 * {@code fields} tabs.  Expanding a row paints the tab strip; selecting the second tab shows its pane and hides the first.
 *
 * <p>
 * Off by default: runs only under the {@code js-tests} profile.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
class RowDetailBrowser_Test extends org.apache.juneau.TestBase {

	private static final String PAGE = """
		{"contractVersion":"1","title":"Pets","cards":[
		 {"id":"t1","type":"datatables","table":{
		   "rows":[{"id":"p1","name":"Rex","species":"dog"}],
		   "columns":[{"key":"id","label":"Id"},{"key":"name","label":"Name"},{"key":"species","label":"Species"}],
		   "detail":{"mode":"inline","title":"Pet {id}","tabs":[
		     {"id":"main","kind":"fields","title":"Main","fields":["name"]},
		     {"id":"more","kind":"fields","title":"More","fields":["species"]}]}}}]}
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void run() throws Exception {
		var views = viewsPack();
		report = ConsoleBrowserFixture.create("row-detail-tabs")
			.page("tabs", ORIGIN + "/tabs", contractPage(PAGE, "", "", views),
				"row", "#t1-body table[data-juneau-view=\"t1\"] tbody td")
			.waitFor("#t1-body tbody td.sorting_1")
			.click("#t1-body .juneau-view-detail-toggle")
			.eval("window.__probe = window.__probe || {}; window.__probe.tabs = Array.from(document.querySelectorAll('#t1-body [role=tab]')).map(function (t) { return t.textContent; });")
			.click("#t1-body [role=tab] >> nth=1")
			.eval("window.__probe.selected = Array.from(document.querySelectorAll('#t1-body [role=tab]')).map(function (t) { return t.getAttribute('aria-selected'); });"
				+ "window.__probe.visiblePanes = Array.from(document.querySelectorAll('#t1-body [role=tabpanel]')).filter(function (p) { return !p.hidden; }).length;")
			.run();
		tables();
	}

	@Test void a01_expandPaintsTabStrip_andSelectingSecondTabSwitchesPane() {
		var r = report.get("tabs");
		assertNoShellErrors(r);
		var p = ConsoleBrowserFixture.probe(r);
		assertEquals(List.of("Main", "More"), p.get("tabs"), r::toString);
		assertEquals(List.of("false", "true"), p.get("selected"), r::toString);
		assertEquals(1, ((Number)p.get("visiblePanes")).intValue(), r::toString);
	}

	// ---- Table-model detail: header actions, ribbon collapse-all, redraw re-open, polling pause counters ----

	private static final String W = "window.__w = function (fn) { return new Promise(function (res) { var n = 0; (function t() { if (fn() || n++ > 400) res(); else setTimeout(t, 50); })(); }); };"
		+ "window.__res = function (re) { return performance.getEntriesByType('resource').filter(function (e) { return re.test(new URL(e.name).pathname); }).length; };"
		+ "window.__tabs = function (id) { return Array.from(document.querySelectorAll('#' + id + '-body [role=tab]')); };";

	private static Map<String,Object> pollCard(String id, boolean pauseWhileEditing) {
		var orders = TableSpec.create(id + "-orders").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/" + id + "orders")
			.columns(Column.create("id").label("Id"));
		var detail = RowDetail.create("/api/" + id + "/{id}/detail").title("Row {id}")
			.tab(DetailTab.fields("Info", "name").id("info"))
			.tab(DetailTab.table("Orders", orders).id("orders").scopeParam("rid").refreshOnPoll(true))
			.action(RowAction.create("del").label("Delete").icon("trash").confirm("Really delete?")
				.endpoint("/api/" + id + "/{id}").method(RowAction.Method.DELETE))
			.action(RowAction.create("note").label("Note").present(RowAction.Present.DIALOG).confirm("Add a note?"));
		var t = TableSpec.create(id).dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/" + id)
			.selection(SelectionDef.create("id")).pollIntervalMs(5000).pausePollingWhileEditing(pauseWhileEditing)
			.columns(Column.create("id").label("Id"), Column.create("name").label("Name"))
			.ribbon(RibbonItem.collapseAll())
			.detail(detail);
		var card = new LinkedHashMap<String,Object>();
		card.put("id", id);
		card.put("type", "datatables");
		card.put("title", id);
		card.put("table", t.toCardBody());
		return card;
	}

	private static String twoTablePage() throws Exception {
		var page = new LinkedHashMap<String,Object>();
		page.put("contractVersion", "1");
		page.put("modelVersion", "1");
		page.put("title", "Two");
		page.put("cards", List.of(pollCard("pa", true), pollCard("pb", false)));
		return contractPage(Json.of(page), "", "", viewsPack());
	}

	private static Map<String,Map<String,Object>> tableReport;

	private static void tables() throws Exception {
		var f = ConsoleBrowserFixture.create("row-detail-model");
		for (var id : List.of("pa", "pb")) {
			f.asset("/api/" + id, "[{\"id\":\"r1\",\"name\":\"Rex\"}]", "application/json")
			 .asset("/api/" + id + "orders", "[{\"id\":\"o1\"}]", "application/json")
			 .asset("/api/" + id + "/r1/detail", "{\"contractVersion\":\"1\",\"fields\":{\"name\":\"Rex\"}}", "application/json");
		}
		var open = "document.querySelectorAll('.juneau-view-detail-toggle').forEach(function (b) { b.click(); });";
		f.page("two", ORIGIN + "/two", twoTablePage(), "dump", "#pa-body")
			.waitFor("#pa-body tbody td.sorting_1").waitFor("#pb-body tbody td.sorting_1")
			.eval(W)
			.eval("var p = window.__probe = window.__probe || {}; (async function () { try {"
				+ open
				+ "await __w(function () { return __tabs('pa').length && __tabs('pb').length; });"
				+ "__tabs('pa')[1].click(); __tabs('pb')[1].click();"
				+ "await __w(function () { return __res(/paorders$/) && __res(/pborders$/); });"
				+ "p.a0 = __res(/paorders$/); p.b0 = __res(/pborders$/);"
				+ "__tabs('pa')[0].click();"
				+ "await new Promise(function (r) { setTimeout(r, 6500); });"
				+ "await __w(function () { return __tabs('pb').length; });"
				+ "p.a1 = __res(/paorders$/); p.b1 = __res(/pborders$/);"
				// paused table: leave and re-enter the tab
				+ "__tabs('pa')[1].click();"
				+ "await new Promise(function (r) { setTimeout(r, 800); });"
				+ "p.a2 = __res(/paorders$/);"
				+ "p.openRows = document.querySelectorAll('.juneau-view-detail-panel').length;"
				// header actions
				+ "document.querySelector('#pb-body [data-juneau-detail-header-action=del]').click();"
				+ "await __w(function () { return document.querySelector('.juneau-view-dialog-backdrop'); });"
				+ "p.delDialog = !!document.querySelector('.juneau-view-dialog-backdrop');"
				+ "p.delText = (document.querySelector('.juneau-view-dialog') || {}).textContent;"
				+ "p.delReqs = __res(/\\/api\\/pb\\/r1$/);"
				+ "document.querySelector('.juneau-view-dialog-cancel').click();"
				+ "await __w(function () { return !document.querySelector('.juneau-view-dialog-backdrop'); });"
				+ "document.querySelector('#pb-body [data-juneau-detail-header-action=note]').click();"
				+ "await __w(function () { return document.querySelector('.juneau-view-dialog-backdrop'); });"
				+ "p.noteText = (document.querySelector('.juneau-view-dialog') || {}).textContent;"
				+ "document.querySelector('.juneau-view-dialog-cancel').click();"
				+ "await __w(function () { return !document.querySelector('.juneau-view-dialog-backdrop'); });"
				// redraw re-open: forced reload keeps the row open and the Orders subtab selected
				+ "var tb = JuneauViews.model.page(); var all = []; (function w(h) { if (h.kind === 'table') all.push(h); h.children.forEach(w); })(tb); p.tableIds = all.map(function (h) { return h.path; }); var f = function () { return all[1]; };"
				+ "var d0 = __res(/\\/api\\/pb$/); f().reload(); await __w(function () { return __res(/\\/api\\/pb$/) > d0; });"
				+ "await __w(function () { return __tabs('pb').length; }); await new Promise(function (r) { setTimeout(r, 400); });"
				+ "p.reopenPanels = document.querySelectorAll('#pb-body .juneau-view-detail-panel').length;"
				+ "p.reopenSel = __tabs('pb').map(function (t) { return t.getAttribute('aria-selected'); });"
				// detail refresh: the bus sees the row close, then open again
				+ "var evs = []; var off = JuneauViews.bus.subscribe('detail:pb', function (v) { evs.push(v.expanded); }, { retained: false });"
				+ "var rd = []; (function w(h) { if (h.kind === 'rowdetail') rd.push(h); h.children.forEach(w); })(JuneauViews.model.page());"
				+ "p.rowDetailCount = rd.length; rd[rd.length - 1].refresh();"
				+ "await new Promise(function (r) { setTimeout(r, 500); }); off(); p.refreshEvents = evs;"
				// ribbon collapse-all
				+ "var ids = Array.from(document.querySelectorAll('[data-juneau-ribbon-id]')).map(function (e) { return e.getAttribute('data-juneau-ribbon-id'); }); p.ribbonIds = ids;"
				+ "document.querySelectorAll('[data-juneau-ribbon-id=collapseAll]')[1].click();"
				+ "await new Promise(function (r) { setTimeout(r, 300); });"
				+ "p.afterCollapse = document.querySelectorAll('#pb-body .juneau-view-detail-panel').length;"
				+ "p.done = true; } catch (e) { p.err = String(e && e.stack || e); } })();")
			.waitFor("body");
		tableReport = f.run();
	}

	private static Map<String,Object> tprobe() {
		var r = tableReport.get("two");
		assertEquals(List.of(), r.get("pageErrors"), r::toString);
		var p = ConsoleBrowserFixture.probe(r);
		assertEquals(Boolean.TRUE, p.get("done"), () -> String.valueOf(p.get("err")) + " " + p);
		return p;
	}

	private static long num(Map<String,Object> p, String k) {
		return ((Number)p.get(k)).longValue();
	}

	@Test void b01_unpausedTable_refreshOnPollTabRefetchesAfterTick() {
		var p = tprobe();
		assertTrue(num(p, "b1") > num(p, "b0"), p::toString);
	}

	@Test void b02_pausedTable_skipsTick_thenRefetchesOnReactivation() {
		var p = tprobe();
		assertEquals(num(p, "a0"), num(p, "a1"), p::toString);
		assertTrue(num(p, "a2") > num(p, "a1"), p::toString);
	}

	@Test void b03_headerDeleteConfirm_opensDialog_withoutRequest() {
		var p = tprobe();
		assertEquals(Boolean.TRUE, p.get("delDialog"), p::toString);
		assertTrue(String.valueOf(p.get("delText")).contains("Really delete?"), p::toString);
		assertEquals(0L, num(p, "delReqs"), p::toString);
	}

	@Test void b04_headerDialogAction_opensDialog() {
		assertTrue(String.valueOf(tprobe().get("noteText")).contains("Add a note?"), tprobe()::toString);
	}

	@Test void b05_redraw_reopensRow_keepingActiveSubtab() {
		var p = tprobe();
		assertEquals(1L, num(p, "reopenPanels"), p::toString);
		assertEquals(List.of("false", "true"), p.get("reopenSel"), p::toString);
	}

	@Test void b06_ribbonCollapseAll_closesOpenRows() {
		var p = tprobe();
		assertTrue(((List<?>)p.get("ribbonIds")).contains("collapseAll"), p::toString);
		assertEquals(0L, num(p, "afterCollapse"), p::toString);
	}

	@Test void b07_detailRefresh_publishesClosedBeforeOpened() {
		var p = tprobe();
		assertEquals(List.of(false, true), p.get("refreshEvents"), p::toString);
	}
}
