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
 * The client object model in a real browser on pages built from {@code html} cards only: an imperative page and its
 * JSON twin, handler resolution and failure painting, the loud shell failures, an inline handler under a
 * Content-Security-Policy without {@code 'unsafe-eval'}, and lookup by path.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // Casts a parsed JSON value to its known Map/List shape.
})
class ConsoleModel_BrowserTest extends TestBase {

	private static final String HANDLERS = "/app/handlers.js";
	private static final String TEMPLATES = "<template data-card=\"t1\"><p>one</p></template><template data-card=\"t2\"><p>two</p></template>";

	private static final String MAKE_TEMPLATES = """
		<script>
		['t1:one', 't2:two'].forEach(function (s) {
		  var t = document.createElement('template'); t.setAttribute('data-card', s.split(':')[0]);
		  t.appendChild(document.createTextNode(s.split(':')[1])); document.body.appendChild(t);
		});
		</script>
		""";

	private static final String IMPERATIVE = """
		<script>
		(function () {
		  var M = window.JuneauViews.model;
		  var p = window.__probe = window.__probe || {};
		  var page = M.Page ? new M.Page({ title: "T", nav: [{ id: "home", label: "Home" }], activeNav: ["home"], scripts: ["/app/handlers.js"] }) : null;
		  var a = page.addCard("html", { id: "h1", template: "t1", title: "One" });
		  a.action({ id: "go", handler: "app.go" });
		  a.bind("loaded", "app.cardLoaded");
		  var b = page.addCard("html", { id: "h2", template: "t2", title: "Two" });
		  b.listen("app.picked", "app.go");
		  page.on("ping", function (e) { p.log.push("ping:" + e.source.id + ":" + e.payload.n); });
		  window.__page = page;
		  page.mount();
		  p.impJson = JSON.stringify(page.toJSON());
		})();
		</script>
		""";

	private static final String JSON_CARDS = "\"cards\":[{\"id\":\"h1\",\"type\":\"html\",\"template\":\"t1\",\"title\":\"One\",\"model\":{\"actions\":[{\"id\":\"go\",\"handler\":\"app.go\"}],\"on\":{\"loaded\":\"app.cardLoaded\"}}},"
		+ "{\"id\":\"h2\",\"type\":\"html\",\"template\":\"t2\",\"title\":\"Two\",\"model\":{\"subscribes\":[{\"topic\":\"app.picked\",\"handler\":\"app.go\"}]}}]";

	private static String json(String extra, String cards) {
		return "{\"contractVersion\":\"1\",\"modelVersion\":\"1\",\"title\":\"T\",\"nav\":[{\"id\":\"home\",\"label\":\"Home\"}],\"activeNav\":[\"home\"]" + extra + "," + cards + "}";
	}

	private static String pack() throws Exception {
		return viewsPack() + "<script src=\"" + HANDLERS + "\"></script>\n";
	}

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		LIFE_PACK = "<script>\n" + resource(ConsoleChromeMixin.JOBLOG_JS_RESOURCE) + "\n</script>\n<script>\n"
			+ java.nio.file.Files.readString(java.nio.file.Path.of("../juneau-terminal-region/src/main/resources/org/apache/juneau/terminal/juneau-terminal.js")) + "\n</script>\n";
		var handlers = resource("/model-js-only/handlers.js");
		var inline = resource(ViewsMixin.MODEL_INLINE_JS_RESOURCE);
		var f = ConsoleBrowserFixture.create("model-js-only")
			.asset(HANDLERS, handlers, "text/javascript")
			.asset("/app/inline.js", inline, "text/javascript");
		// Imperative page: a script after the shell builds and mounts it (no island), then the steps drive it.
		var impPage = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>\n"
			+ "<script src=\"" + SHELL + "\"></script>\n" + pack() + MAKE_TEMPLATES + IMPERATIVE + "\n</body></html>";
		f.page("imp", ORIGIN + "/imp", impPage, "h1", "#h1", "h2", "#h2")
			.click("#h1 [data-action-id=go]")
			.eval("var p = window.__probe, pg = window.__page; p.afterClick = p.log.slice();"
				+ "pg.card('h1').setLabel('L1'); pg.card('h1').setBadge(3, 'info'); pg.tabs[0].setBadge(2, 'info'); pg.tabs[0].activate();"
				+ "p.tabAttached = pg.tabs[0].attached; p.path = pg.card('h1').path;"
				+ "pg.card('h1').emit('ping', { n: 7 });"
				+ "pg.card('h1').publish('app.picked', {});"
				+ "p.get = window.JuneauViews.model.get(p.path) === pg.card('h1');"
				+ "p.getMissing = window.JuneauViews.model.get('page/page/card:nope');");
		f.page("json", ORIGIN + "/json",
			contractPage(json(",\"scripts\":[\"" + HANDLERS + "\"]", JSON_CARDS), TEMPLATES, "", viewsPack())
				+ "<script>window.__probe = window.__probe || {}; window.addEventListener('load', function () { window.__probe.jsonJson = JSON.stringify(window.JuneauViews.model.page().toJSON()); });</script>",
			"h1", "#h1")
			.click("#h1 [data-action-id=go]")
			.eval("window.__probe.jsonLog = (window.__probe.log || []).slice();");
		f.page("htmlcmd", ORIGIN + "/htmlcmd",
			contractPage(json(",\"scripts\":[\"" + HANDLERS + "\"]", "\"cards\":[{\"id\":\"h1\",\"type\":\"html\",\"template\":\"t1\",\"model\":{\"on\":{\"loaded\":\"app.cardLoaded\"}}}]"), TEMPLATES, "", viewsPack()),
			"h1", "#h1")
			.eval("(async function () { var p = window.__probe = window.__probe || {}; var body = document.querySelector('#h1 p'); p.before = body ? body.textContent : null; if (body) body.textContent = 'changed'; p.mid = document.querySelector('#h1 p') ? document.querySelector('#h1 p').textContent : null;"
				+ " JuneauViews.bus.publish('cmd:h1', { schemaVersion: 1, op: 'refresh' });"
				+ " await new Promise(function (r) { setTimeout(r, 400); }); p.after = document.querySelector('#h1 p') ? document.querySelector('#h1 p').textContent : null;"
				+ " JuneauConsole.destroyCard('h1'); p.destroyedEmpty = document.querySelector('#h1') ? document.querySelector('#h1').children.length : -1;"
				+ " try { JuneauConsole.refreshCard('h1'); p.afterDestroy = 'no error'; } catch (e) { p.afterDestroy = e.code; } p.done = true; })();");
		var lifeCards = "\"cards\":[{\"id\":\"tv\",\"type\":\"terminal-view\",\"src\":\"/tv/none\"},{\"id\":\"jl\",\"type\":\"job-log\",\"submitUrl\":\"/jobs/x\"},{\"id\":\"tm\",\"type\":\"terminal\",\"src\":\"/term/none\"}]";
		f.page("life", ORIGIN + "/life", contractPage(json("", lifeCards), "", "", viewsPack() + LIFE_PACK), "other", "body")
			.eval("(async function () { var p = window.__probe = window.__probe || {}; p.res = {};"
				+ " await new Promise(function (x) { setTimeout(x, 400); }); var b0 = document.querySelector('.jc-console-error'); if (b0) b0.remove();"
				+ " for (var id of ['tv', 'jl', 'tm']) { var r = p.res[id] = {};"
				+ "  try { JuneauViews.bus.publish('cmd:' + id, { schemaVersion: 1, op: 'refresh' }); r.cmd = 'ok'; } catch (e) { r.cmd = String(e.code || e.message); }"
				+ "  await new Promise(function (x) { setTimeout(x, 150); });"
				+ "  var bn = document.querySelector('.jc-console-error'); r.bannerAfterCmd = bn && bn.textContent.indexOf(\"card '\" + id + \"'\") >= 0 ? bn.textContent : ''; if (bn) bn.remove();"
				+ "  try { JuneauConsole.destroyCard(id); r.destroy = 'ok'; } catch (e) { r.destroy = String(e.code || e.message); }"
				+ "  bn = document.querySelector('.jc-console-error'); r.bannerAfterDestroy = bn && bn.textContent.indexOf(\"card '\" + id + \"'\") >= 0 ? bn.textContent : ''; if (bn) bn.remove();"
				+ "  try { JuneauConsole.refreshCard(id); r.after = 'no error'; } catch (e) { r.after = e.code; } }"
				+ " p.done = true; })();");
		f.page("unknown", ORIGIN + "/unknown",
			contractPage(json(",\"scripts\":[\"" + HANDLERS + "\"]", "\"cards\":[{\"id\":\"h1\",\"type\":\"html\",\"template\":\"t1\",\"model\":{\"on\":{\"loaded\":\"app.nope\"}}},{\"id\":\"h2\",\"type\":\"html\",\"template\":\"t2\"}]"), TEMPLATES, "", viewsPack()),
			"err", "#h1 [data-juneau-error]", "other", "#h2");
		f.page("throws", ORIGIN + "/throws",
			contractPage(json(",\"scripts\":[\"" + HANDLERS + "\"]", "\"cards\":[{\"id\":\"h1\",\"type\":\"html\",\"template\":\"t1\",\"model\":{\"actions\":[{\"id\":\"boom\",\"handler\":\"app.boom\"}]}}]"), TEMPLATES, "", viewsPack()),
			"err", "#h1 [data-juneau-error]")
			.click("#h1 [data-action-id=boom]");
		var kpi = "<script>try { JuneauConsole.registerCard('kpi', { render: function () {} }); } catch (e) { window.__probe = { threw: String(e.message) }; }</script>";
		f.page("e21", ORIGIN + "/e21", contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[]}", "", "", kpi));
		var reg = "<script>window.__probe = { r: [] }; ['Bad', 'app.dup', 'app.dup'].forEach(function (n) { try { JuneauViews.handlers.register(n, function () {}); window.__probe.r.push('ok'); } catch (e) { window.__probe.r.push('threw'); } });</script>";
		f.page("e114", ORIGIN + "/e114", contractPage(json("", "\"cards\":[]"), "", "", viewsPack() + reg));
		f.page("e120", ORIGIN + "/e120", contractPage(json("", "\"cards\":[]").replace("\"modelVersion\":\"1\"", "\"modelVersion\":\"9\""), "", "", ""));
		f.page("e124", ORIGIN + "/e124", contractPage(json(",\"scripts\":[\"/app/missing.js\"]", "\"cards\":[]"), "", "", viewsPack()));
		var inlineCards = "\"cards\":[{\"id\":\"h1\",\"type\":\"html\",\"template\":\"t1\",\"model\":{\"on\":{\"loaded\":{\"inline\":\"window.__probe.ran = true\"}}}},{\"id\":\"h2\",\"type\":\"html\",\"template\":\"t2\"}]";
		f.page("csp", ORIGIN + "/csp",
			contractPage(json(",\"allowInlineHandlers\":true", inlineCards), TEMPLATES, "", viewsPack() + "<script src=\"/app/inline.js\"></script>\n"),
			"other", "#h2")
			.pageHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
		report = f.run();
		tableProbe();
	}

	private static final String WAIT = "window.__w = function (fn) { return new Promise(function (res) { var n = 0; (function t() { if (fn() || n++ > 160) res(); else setTimeout(t, 50); })(); }); };"
		+ "window.__tab = function (i) { return Array.from(document.querySelectorAll('#pets-body [role=tab]'))[i]; };"
		+ "window.__res = function (re) { return performance.getEntriesByType('resource').filter(function (e) { return re.test(new URL(e.name).pathname); }).length; };"
		+ "window.__size = function () { var n = 0; (function w(h) { n++; h.children.forEach(w); })(JuneauViews.model.page()); return n; };"
		+ "window.__tbl = function () { var f = function (h) { if (h.kind === 'table') return h; for (var c of h.children) { var r = f(c); if (r) return r; } return null; }; return f(JuneauViews.model.page()); };";

	private static final String DETAIL_RE = "/^\\/api\\/pets\\/p1\\/detail$/", PETS_RE = "/^\\/api\\/pets$/";

	/** The shared steps: counts, label, detail refresh icon, ribbon refresh, forced draws (what a poll tick does), registry size. */
	private static final String STEPS = "var p = window.__probe = window.__probe || {};"
		+ "(async function () { try {"
		+ "  await __w(function () { return __tab(1); });"
		+ "  __tab(1).click();"
		+ "  await __w(function () { return /Orders \\(3\\)/.test(__tab(1).textContent); });"
		+ "  p.label1 = __tab(1).textContent;"
		+ "  p.d0 = __res(" + DETAIL_RE + "); p.t0 = __res(" + PETS_RE + ");"
		+ "  document.querySelector('#pets-body .juneau-model-actions [data-action-id=refresh]').click();"
		+ "  await __w(function () { return __res(" + DETAIL_RE + ") > p.d0; }); p.d1 = __res(" + DETAIL_RE + ");"
		+ "  document.querySelector('[data-juneau-ribbon-id=reload]').click();"
		+ "  await __w(function () { return __res(" + PETS_RE + ") > p.t0; }); p.t1 = __res(" + PETS_RE + ");"
		+ "  await __w(function () { return /Orders \\(3\\)/.test(__tab(1) && __tab(1).textContent); });"
		+ "  p.size0 = __size();"
		+ "  for (var i = 0; i < 12; i++) { var n = __res(" + PETS_RE + "); __tbl().reload(); await __w(function () { return __res(" + PETS_RE + ") > n; }); await new Promise(function (r) { setTimeout(r, 150); }); }"
		+ "  p.size1 = __size();"
		+ "  await __w(function () { return __tab(1); });"
		+ "  p.d2 = __res(" + DETAIL_RE + ");"
		+ "  p.reopenTabs = Array.from(document.querySelectorAll('#pets-body [role=tab]')).map(function (t) { return t.textContent; });"
		+ "  p.reopenSelected = Array.from(document.querySelectorAll('#pets-body [role=tab]')).map(function (t) { return t.getAttribute('aria-selected'); });"
		+ "  p.pageJson = JSON.stringify(JuneauViews.model.page().toJSON());"
		+ "  p.done = true;"
		+ "} catch (e) { p.err = String(e && e.stack || e); } })();";

	private static String detailMeta() {
		return "{\"contractVersion\":\"1\",\"fields\":{\"name\":\"Rex\"},\"counts\":{\"orders\":3}}";
	}

	private static TableSpec petsTable(boolean refreshOnPoll) {
		var orders = TableSpec.create("pet-orders").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/orders")
			.columns(Column.create("id").label("Id"), Column.create("status").label("Status"));
		var tab = DetailTab.table("Orders", orders).id("orders").scopeParam("petId").on("loaded", "juneau.countLabel").refreshOnPoll(refreshOnPoll);
		var detail = RowDetail.create("/api/pets/{id}/detail").title("Pet {id}")
			.tab(DetailTab.fields("Info", "name").id("info"))
			.tab(tab)
			.action("refresh", "refresh-cw", "juneau.refresh");
		return TableSpec.create("pets").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/pets")
			.selection(SelectionDef.create("id"))
			.columns(Column.create("id").label("Id"), Column.create("name").label("Name"))
			.ribbon(RibbonItem.action("reload").icon("refresh-cw").action("click", "refresh-cw", "juneau.refreshClosest", Map.of("kind", "table")))
			.detail(detail);
	}

	private static String petsPage(boolean refreshOnPoll) throws Exception {
		var card = new LinkedHashMap<String,Object>();
		card.put("id", "pets");
		card.put("type", "datatables");
		card.put("title", "Pets");
		card.put("table", petsTable(refreshOnPoll).toCardBody());
		var page = new LinkedHashMap<String,Object>();
		page.put("contractVersion", "1");
		page.put("modelVersion", "1");
		page.put("title", "Pets");
		page.put("cards", List.of(card));
		return contractPage(Json.of(page), "", "", viewsPack());
	}

	private static String jsOnlyPage(boolean refreshOnPoll) throws Exception {
		return jsOnlyPage(refreshOnPoll, "");
	}

	/** The JS-only page with an extra script (the handler variants) loaded before the page is built. */
	private static String jsOnlyPage(boolean refreshOnPoll, String variantScript) throws Exception {
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>\n"
			+ "<script src=\"" + SHELL + "\"></script>\n" + viewsPack()
			+ "<script>window.__refreshOnPoll = " + refreshOnPoll + ";" + variantScript + "</script><script src=\"/app/cards-js-only.js\"></script>\n</body></html>";
	}

	private static Map<String,Map<String,Object>> tableReport;

	private static String LIFE_PACK;

	private static final String HANDLERS_JS = "var H = window.JuneauViews.handlers; ";
	/** The detail-count handler variants: update the detail header, update the row, tell an ancestor. */
	private static final Map<String,String> VARIANTS = Map.of(
		"jsHdr", HANDLERS_JS + "H.register('pets.ordersInHeader', function (tab, e) { tab.closest('rowdetail').setBadge(e.payload.count, 'info'); }); window.__ordersHandler = 'pets.ordersInHeader';",
		"jsRow", HANDLERS_JS + "H.register('pets.ordersOnRow', function (tab, e) { tab.closest('row').setBadge(e.payload.count, 'info'); }); window.__ordersHandler = 'pets.ordersOnRow';",
		"jsEmit", HANDLERS_JS + "H.register('pets.ordersEmit', function (tab, e) { tab.emit('orders-counted', { count: e.payload.count }); }); window.__ordersHandler = 'pets.ordersEmit';"
			+ " window.__onPage = function (page) { page.on('orders-counted', function (ev) { (window.__probe = window.__probe || {}).emitted = { path: ev.source.path, count: ev.payload.count }; }); };");

	private static void tableProbe() throws Exception {
		var many = new String[10];
		Arrays.fill(many, "[{\"id\":\"p1\",\"name\":\"Rex\"}]");
		var detailBodies = new String[40];
		Arrays.fill(detailBodies, detailMeta());
		var f = ConsoleBrowserFixture.create("model-table")
			.asset("/api/pets", "[{\"id\":\"p1\",\"name\":\"Rex\"}]", "application/json")
			.asset("/api/orders", "[{\"id\":\"o1\",\"status\":\"new\"},{\"id\":\"o2\",\"status\":\"new\"},{\"id\":\"o3\",\"status\":\"paid\"}]", "application/json")
			.asset("/app/cards-js-only.js", resource("/model-js-only/cards-js-only.js"), "text/javascript")
			.sequence("/api/pets/p1/detail", "application/json", detailBodies);
		var open = "#pets-body .juneau-view-detail-toggle";
		f.page("tbl", ORIGIN + "/tbl", petsPage(false), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").click(open).eval(WAIT).eval(STEPS)
			.waitFor("body");
		f.page("tblPoll", ORIGIN + "/tblPoll", petsPage(true), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").click(open).eval(WAIT).eval(STEPS);
		f.page("js", ORIGIN + "/js", jsOnlyPage(false), "dump", "#pets-body")
			.waitFor("#pets-body tbody td.sorting_1").click(open).eval(WAIT).eval(STEPS);
		var variantSteps = "var p = window.__probe = window.__probe || {};(async function () { try {"
			+ "  await __w(function () { return __tab(1); }); __tab(1).click();"
			+ "  await __w(function () { return document.querySelectorAll('.juneau-view-detail-tab-count').length > 0 || p.emitted; });"
			+ "  await new Promise(function (r) { setTimeout(r, 300); });"
			+ "  p.chips = Array.from(document.querySelectorAll('.juneau-view-detail-tab-count')).map(function (c) { return { text: c.textContent, inTab: !!c.closest('[role=tab]'), inPanel: !!c.closest('.juneau-view-detail-panel'), inTr: !!c.closest('tr') }; });"
			+ "  p.done = true; } catch (e) { p.err = String(e && e.stack || e); } })();";
		for (var v : VARIANTS.entrySet())
			f.page(v.getKey(), ORIGIN + "/" + v.getKey(), jsOnlyPage(false, v.getValue()), "dump", "#pets-body")
				.waitFor("#pets-body tbody td.sorting_1").click(open).eval(WAIT).eval(variantSteps);
		var types = "<script>\n" + resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE) + "\n</script>\n<script>\n" + resource(ConsoleChromeMixin.JOBLOG_JS_RESOURCE)
			+ "\n</script>\n<script>\n" + java.nio.file.Files.readString(java.nio.file.Path.of("../juneau-terminal-region/src/main/resources/org/apache/juneau/terminal/juneau-terminal.js")) + "\n</script>\n";
		var typeProbe = "<script>window.addEventListener('load', function () { var JC = JuneauViews && window.JuneauConsole, M = JuneauViews.model, out = {};"
			+ " ['html','terminal-view','datatables','chart','job-log','terminal'].forEach(function (t) {"
			+ "  var c = JC.cardClass(t); var r = { registered: !!c, isModelCard: !!c && c.prototype instanceof M.Card, bad: [] };"
			+ "  if (c) ['roles','ops'].forEach(function (k) { var m = c[k] || {}; Object.keys(m).forEach(function (a) { if (typeof c.prototype[m[a]] !== 'function') r.bad.push(k + ':' + a); }); });"
			+ "  out[t] = r; });"
			+ " window.__probe = { types: out }; });</script>";
		f.page("types", ORIGIN + "/types", contractPage(json("", "\"cards\":[]"), "", "", viewsPack() + types + typeProbe));
		tableReport = f.run();
	}

	private static Map<String,Object> tp(String name) {
		var r = tableReport.get(name);
		assertEquals(List.of(), r.get("pageErrors"), r::toString);
		var p = ConsoleBrowserFixture.probe(r);
		assertEquals(Boolean.TRUE, p.get("done"), () -> String.valueOf(p.get("err")));
		return p;
	}

	private static long n(Map<String,Object> p, String k) {
		return ((Number)p.get(k)).longValue();
	}

	private static Map<String,Object> probeOf(String name) {
		return ConsoleBrowserFixture.probe(report.get(name));
	}

	private static List<String> codes(String name) {
		return ((List<Map<String,String>>)report.get(name).get("banner")).stream().map(m -> m.get("code")).toList();
	}

	@Test void jsOnly_html_card_action_click_runs_handler() {
		var r = report.get("imp");
		assertEquals(List.of(), r.get("pageErrors"), r::toString);
		assertEquals(List.of("go:h1"), probeOf("imp").get("afterClick"), r::toString);
	}

	@Test void jsOnly_card_setLabel_and_setBadge() {
		var t = String.valueOf(queries(report.get("imp")).get("h1"));
		assertTrue(t.contains("L1"), t);
		assertTrue(t.contains("3"), t);
	}

	@Test void jsOnly_pagetab_setBadge_and_activate() {
		assertEquals(List.of(), report.get("imp").get("pageErrors"));
		assertEquals(Boolean.TRUE, probeOf("imp").get("tabAttached"), report.get("imp")::toString);
	}

	@Test void jsOnly_emit_bubbles_to_page_listener() {
		var log = (List<String>)probeOf("imp").get("log");
		assertTrue(log.contains("ping:h1:7"), String.valueOf(log));
		assertTrue(log.contains("go:h2"), "the declared listener on h2 ran for app.picked: " + log);
	}

	@Test void jsOnly_json_variant_equals_imperative_toJSON() {
		var imp = (Map<String,Object>)Json.to((String)probeOf("imp").get("impJson"), Map.class);
		var js = (Map<String,Object>)Json.to((String)probeOf("json").get("jsonJson"), Map.class);
		assertEquals(imp, js);
	}

	@Test void jsOnly_scripts_loaded_by_shell_before_handler_resolution() {
		var r = report.get("json");
		assertNoShellErrors(r);
		assertEquals(List.of(), r.get("pageErrors"), r::toString);
		var log = (List<String>)probeOf("json").get("jsonLog");
		assertTrue(log.contains("cardLoaded:h1") || log.contains("go:h1"), String.valueOf(log));
		assertTrue(log.contains("go:h1"), String.valueOf(log));
	}

	@Test void jsOnly_unknown_handler_painted_E116_page_renders() {
		var r = report.get("unknown");
		assertTrue(String.valueOf(queries(r).get("err")).contains("app.nope"), r::toString);
		assertTrue(String.valueOf(queries(r).get("other")).contains("two"), r::toString);
	}

	@Test void jsOnly_handler_throw_painted_E118() {
		var r = report.get("throws");
		var t = String.valueOf(queries(r).get("err"));
		assertTrue(t.contains("kaboom") && t.contains("app.boom"), t);
	}

	@Test void loudFailure_E21_object_form_banner() {
		assertTrue(codes("e21").contains("E-JS-21"), report.get("e21")::toString);
	}

	@Test void loudFailure_E114_duplicate_E115_E120_E124_banners() {
		assertEquals(List.of("threw", "ok", "threw"), probeOf("e114").get("r"), report.get("e114")::toString);
		assertTrue(codes("e120").contains("E-JS-120"), report.get("e120")::toString);
		assertTrue(codes("e124").contains("E-JS-124"), report.get("e124")::toString);
	}

	@Test void inline_csp_page_reports_E119_not_silent() {
		var r = report.get("csp");
		var errors = String.valueOf(r.get("consoleErrors"));
		assertTrue(errors.contains("E-JS-119"), r::toString);
		assertNull(r.get("probe") == null ? null : ((Map<String,Object>)r.get("probe")).get("ran"), r::toString);
		assertTrue(String.valueOf(queries(r).get("other")).contains("two"), r::toString);
	}

	@Test void model_get_by_path_in_page() {
		var p = probeOf("imp");
		assertEquals("page/page/card:h1", p.get("path"));
		assertEquals(Boolean.TRUE, p.get("get"), report.get("imp")::toString);
		assertNull(p.get("getMissing"));
	}

	@Test void jsOnly_ordersCount_sets_tab_label_Orders_3() {
		for (var c : List.of("tbl", "js"))
			assertTrue(String.valueOf(tp(c).get("label1")).contains("Orders (3)"), c + ": " + tableReport.get(c));
	}

	@Test void jsOnly_detailRefreshIcon_click_refetches_detail() {
		for (var c : List.of("tbl", "js")) {
			var p = tp(c);
			assertTrue(n(p, "d1") > n(p, "d0"), c + ": " + p);
		}
	}

	@Test void jsOnly_ribbonRefresh_reloads_table() {
		for (var c : List.of("tbl", "js")) {
			var p = tp(c);
			assertTrue(n(p, "t1") > n(p, "t0"), c + ": " + p);
		}
	}

	/** After forced draws (what a poll tick does) the open row re-opens on the same subtab: the detail is fetched again, the label
	 *  goes back to the server label and the loaded handler re-applies the count. */
	@Test void jsOnly_ordersCount_after_poll_redraw_reopens_with_server_label() {
		var p = tp("tbl");
		assertTrue(n(p, "d2") > n(p, "d1"), "the re-open fetched the detail again: " + p);
		assertEquals(List.of("Info", "Orders (3)"), p.get("reopenTabs"), p::toString);
		assertEquals(List.of("false", "true"), p.get("reopenSelected"), p::toString);
	}

	@Test void jsOnly_count_on_detail_header_and_row_variants() {
		var p = tp("tblPoll");
		assertTrue(String.valueOf(p.get("label1")).contains("Orders (3)"), p::toString);
		assertEquals(List.of("Info", "Orders (3)"), p.get("reopenTabs"), p::toString);
	}

	@Test void registry_size_stable_over_forced_draws() {
		for (var c : List.of("tbl", "js")) {
			var p = tp(c);
			assertEquals(n(p, "size0"), n(p, "size1"), c + ": " + p);
		}
	}

	@Test void jsOnly_header_variant_badges_the_detail_header_not_the_tab() {
		var p = tp("jsHdr");
		var chips = (List<Map<String,Object>>)p.get("chips");
		assertTrue(chips.stream().anyMatch(c -> "3".equals(c.get("text")) && Boolean.TRUE.equals(c.get("inPanel")) && ! Boolean.TRUE.equals(c.get("inTab"))), p::toString);
		assertTrue(chips.stream().noneMatch(c -> Boolean.TRUE.equals(c.get("inTab"))), p::toString);
	}

	@Test void jsOnly_row_variant_badges_the_row() {
		var p = tp("jsRow");
		var chips = (List<Map<String,Object>>)p.get("chips");
		assertTrue(chips.stream().anyMatch(c -> "3".equals(c.get("text")) && Boolean.TRUE.equals(c.get("inTr")) && ! Boolean.TRUE.equals(c.get("inPanel"))), p::toString);
	}

	@Test void jsOnly_emit_variant_reaches_the_page_listener() {
		var p = tp("jsEmit");
		var e = (Map<String,Object>)p.get("emitted");
		assertEquals(3L, ((Number)e.get("count")).longValue(), p::toString);
		assertTrue(String.valueOf(e.get("path")).contains("tab:orders"), p::toString);
	}

	/** The Java-built table page and the JS-only table page describe the same contract. */
	@Test void jsOnly_table_page_contract_equals_java_built_table_page() {
		var java = Json.to((String)tp("tbl").get("pageJson"), Map.class);
		var js = Json.to((String)tp("js").get("pageJson"), Map.class);
		assertEquals(java, js);
	}

	/** Every ported card type is a class extending the model Card, and the methods its static roles and ops name exist. */
	@Test void builtin_card_types_are_model_Card_subclasses_with_real_role_and_op_methods() {
		var types = (Map<String,Map<String,Object>>)ConsoleBrowserFixture.probe(tableReport.get("types")).get("types");
		assertEquals(Set.of("html", "terminal-view", "datatables", "chart", "job-log", "terminal"), types.keySet());
		types.forEach((t, r) -> {
			assertEquals(Boolean.TRUE, r.get("registered"), t + ": " + r);
			assertEquals(Boolean.TRUE, r.get("isModelCard"), t + ": " + r);
			assertEquals(List.of(), r.get("bad"), t + ": " + r);
		});
	}

	/** The html card: a cmd:refresh through the bus re-renders its template; destroyCard retires it and a later refresh raises E-JS-22. */
	@Test void html_card_cmd_refresh_and_destroy() {
		var p = probeOf("htmlcmd");
		assertEquals(Boolean.TRUE, p.get("done"), report.get("htmlcmd")::toString);
		assertEquals("one", p.get("before"), p::toString);
		assertEquals("changed", p.get("mid"), p::toString);
		assertEquals("one", p.get("after"), "cmd:refresh re-rendered the template: " + p);
		assertEquals("E-JS-22", p.get("afterDestroy"), p::toString);
	}

	/** The terminal-view, job-log and terminal cards: a cmd:refresh through the bus and destroyCard work, and a refresh after destroy raises E-JS-22. A card's own cmd or destroy that throws is caught by the shell and painted as a page banner, so the test also requires that destroy raises no banner for the card. (The page has no xterm, so a cmd:refresh on the terminal cards paints a render banner by design: it is not asserted.) */
	@Test void terminal_view_job_log_and_terminal_cards_take_cmd_refresh_and_destroy() {
		var p = probeOf("life");
		assertEquals(Boolean.TRUE, p.get("done"), report.get("life")::toString);
		var res = (Map<String,Map<String,Object>>)p.get("res");
		var bad = new ArrayList<String>();
		for (var id : List.of("tv", "jl", "tm")) {
			var r = res.get(id);
			if (r == null || ! "ok".equals(r.get("cmd")) || ! "ok".equals(r.get("destroy")) || ! "E-JS-22".equals(r.get("after"))
				|| ! "".equals(r.get("bannerAfterDestroy")))
				bad.add(id + "=" + r);
		}
		assertEquals(List.of(), bad, () -> "cards that broke: " + bad + "; probe: " + p);
	}
}
