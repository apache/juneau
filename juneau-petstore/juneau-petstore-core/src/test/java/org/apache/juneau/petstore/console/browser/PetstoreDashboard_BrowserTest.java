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
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The dashboard rail, splitter, grid, status header, Refresh, Copy link and saved state on
 * {@code /console/dev/dashboard}.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreDashboard_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String PAGE = "/console/dev/dashboard";
	private static final String STATUS = "(document.querySelector('.jc-dash-status-line') || {textContent: ''}).textContent";
	private static final String READY = "/^\\d[\\d,]* rows/.test(" + STATUS + ")";
	private static final String TONE = "document.querySelector('.jc-dash-status-line').getAttribute('data-tone')";
	private static final String PRESETS = "Array.from(document.querySelectorAll('.jc-dash-presets button'))"
		+ ".map(b => b.textContent + ':' + b.getAttribute('aria-pressed')).join('|')";
	private static final String CHOICES = "Array.from(document.querySelectorAll('.jc-dash-choice label'))"
		+ ".map(l => l.childNodes[1].textContent + ':' + l.querySelector('input').checked + (l.hasAttribute('data-missing') ? ':missing' : ''))"
		+ ".sort().join('|')";
	private static final String SEARCH = "document.querySelector('.jc-dash-rail input[type=search]')";
	private static final String CHART = "JuneauConsole.cardApi('daily').chart()";
	private static final String SAVED = "JSON.parse(localStorage.getItem('juneau.pagestate.page.petstore.dashboard.state'))";
	private static final String COPY_FIELD = "document.querySelector('.jc-dash-copy-field input')";

	private static Map<String,Map<String,Object>> report;

	// Map.of stops at ten pairs; the cases and their query maps need more.
	private static Map<String,Object> m(Object...kv) {
		var r = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kv.length; i += 2)
			r.put((String) kv[i], kv[i + 1]);
		return r;
	}

	private static Map<String,Object> pause(int ms) {
		return m("evaluate", "new Promise(r => setTimeout(r, " + ms + "))");
	}

	private static Map<String,Object> waitFor(String expr) {
		return m("waitFor", expr, "timeoutMs", 15000);
	}

	private static String clipboard(String writeText) {
		return "Object.defineProperty(navigator, 'clipboard', {configurable: true, value: {writeText: " + writeText + "}});";
	}

	private static String top(String selector) {
		return "document.querySelector('" + selector + "').getBoundingClientRect().top";
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("dashboard", base, List.of(
			m("name", "rail", "path", PAGE,
				"actions", List.of(waitFor(READY)),
				"queries", m(
					"legends", "Array.from(document.querySelectorAll('.jc-dash-rail > .jc-dash-section'))"
						+ ".map(f => (f.querySelector(':scope > legend') || {textContent: '-'}).textContent).join('|')",
					"presets", PRESETS,
					"utcInputs", "document.querySelectorAll('.jc-dash-rail input[type=datetime-local]').length",
					"placeholder", SEARCH + ".placeholder",
					"choices", CHOICES,
					"cardSection", "!!document.querySelector('.jc-dash-rail #share .jc-chart-canvas')",
					"custom", "!!document.querySelector('.jc-dash-rail input[data-late]')",
					"errorBoxes", "document.querySelectorAll('.jc-dash .jc-card-error').length")),
			m("name", "late", "path", PAGE,
				"actions", List.of(waitFor(READY), m("click", ".jc-dash-rail input[data-late]"),
					waitFor("document.querySelectorAll('.jc-dash-choice label').length === 1")),
				"queries", m("choices", CHOICES)),
			m("name", "splitter", "path", PAGE,
				"actions", List.of(waitFor(READY), m("drag", List.of(".jc-dash-splitter", 64)),
					m("press", List.of(".jc-dash-splitter", "ArrowRight")), m("reload", true), waitFor(READY)),
				"queries", m(
					"valueNow", "document.querySelector('.jc-dash-splitter').getAttribute('aria-valuenow')",
					"railPx", "Math.round(document.querySelector('.jc-dash-rail').getBoundingClientRect().width)",
					"stored", "JuneauViews.pageState.page('petstore.dashboard').get('railWidth')")),
			m("name", "wide", "path", PAGE, "viewport", List.of(1440, 900),
				"actions", List.of(waitFor(READY)),
				"queries", m(
					"cols", "getComputedStyle(document.querySelector('.jc-dash-grid')).gridTemplateColumns.split(' ').length",
					"railShown", "getComputedStyle(document.querySelector('.jc-dash-rail')).display !== 'none'",
					"toggleShown", "getComputedStyle(document.querySelector('.jc-dash-filters')).display !== 'none'",
					"spanFull", "Math.abs(document.querySelector('.jc-dash-panel[data-span=\"2\"]').getBoundingClientRect().width"
						+ " - document.querySelector('.jc-dash-grid').getBoundingClientRect().width) < 2",
					"pairRow", "(p => p[0].getBoundingClientRect().top === p[1].getBoundingClientRect().top"
						+ " && p[0].getBoundingClientRect().left < p[1].getBoundingClientRect().left)"
						+ "(document.querySelectorAll('.jc-dash-panel[data-span=\"1\"]'))",
					"dashPx", "Math.round(document.querySelector('.jc-dash').getBoundingClientRect().width)")),
			m("name", "narrow", "path", PAGE, "viewport", List.of(800, 900),
				"actions", List.of(waitFor(READY),
					m("evaluate", "window.__before = getComputedStyle(document.querySelector('.jc-dash-rail')).display"),
					m("click", ".jc-dash-filters")),
				"queries", m(
					"before", "window.__before",
					"railShown", "getComputedStyle(document.querySelector('.jc-dash-rail')).display !== 'none'",
					"expanded", "document.querySelector('.jc-dash-filters').getAttribute('aria-expanded')",
					"cols", "getComputedStyle(document.querySelector('.jc-dash-grid')).gridTemplateColumns.split(' ').length",
					"sameWidth", "(p => Math.abs(p[0].getBoundingClientRect().width - p[1].getBoundingClientRect().width) < 2)"
						+ "(document.querySelectorAll('.jc-dash-panel'))")),
			m("name", "narrow-right", "path", PAGE + "?variant=right", "viewport", List.of(800, 900),
				"actions", List.of(waitFor(READY),
					m("evaluate", "window.__filtersTop = " + top(".jc-dash-filters") + "; window.__gridTop = " + top(".jc-dash-grid")),
					m("click", ".jc-dash-filters")),
				"queries", m(
					"side", "document.querySelector('.jc-dash-body').getAttribute('data-rail')",
					"filtersTop", "window.__filtersTop",
					"gridTop", "window.__gridTop",
					"railTop", top(".jc-dash-rail"),
					"gridTopOpen", top(".jc-dash-grid"),
					"railShown", "getComputedStyle(document.querySelector('.jc-dash-rail')).display !== 'none'",
					"splitterShown", "getComputedStyle(document.querySelector('.jc-dash-splitter')).display !== 'none'")),
			m("name", "slow", "path", PAGE + "?variant=slow", "waitUntil", "domcontentloaded",
				"actions", List.of(waitFor("/^Loading/.test(" + STATUS + ")"), m("evaluate", "window.__loading = " + STATUS), waitFor(READY)),
				"queries", m("loading", "window.__loading", "ready", STATUS, "tone", TONE)),
			m("name", "truncated", "path", PAGE + "?variant=truncated",
				"actions", List.of(waitFor(READY)),
				"queries", m("text", STATUS, "tone", TONE)),
			m("name", "error", "path", PAGE + "?variant=error", "allowFailedLoads", true,
				"actions", List.of(waitFor("/^Load failed: /.test(" + STATUS + ")")),
				"queries", m("text", STATUS, "tone", TONE)),
			m("name", "refresh", "path", PAGE,
				"actions", List.of(waitFor(READY),
					m("evaluate", "window.__ready = 0; window.__cmds = [];"
						+ " JuneauViews.bus.subscribe('data:orders', h => { if (h && h.status === 'ready') window.__ready++; });"
						+ " JuneauViews.bus.subscribe('cmd:orders', p => window.__cmds.push(p.op + ':' + !!(p.options && p.options.clearCache)));"),
					pause(500),
					m("evaluate", "window.__r0 = window.__ready;"
						+ " fetch('/console/dev/dashboard/requests', {headers: {Accept: 'application/json'}}).then(x => x.json()).then(j => { window.__n0 = j.series; })"),
					m("click", ".jc-dash-refresh > button:first-child"),
					waitFor("window.__ready > window.__r0"),
					m("evaluate", "window.__r1 = window.__ready"),
					m("click", ".jc-dash-refresh [aria-label=\"Refresh options\"]"),
					m("evaluate", "(() => { const m = document.querySelector('.jc-dash-menu').getBoundingClientRect(),"
						+ " b = document.querySelector('.jc-dash-refresh [aria-label=\"Refresh options\"]').getBoundingClientRect();"
						+ " sessionStorage.setItem('menuRect', [m.top, b.bottom, m.right, b.right].join('|')); })()"),
					m("click", ".jc-dash-menu [role=menuitem]"),
					waitFor("window.__ready > window.__r1")),
				"queries", m(
					"cmds", "window.__cmds.join('|')",
					"menuRect", "sessionStorage.getItem('menuRect')",
					"served", "fetch('/console/dev/dashboard/requests', {headers: {Accept: 'application/json'}}).then(x => x.json()).then(j => j.series - window.__n0)")),
			m("name", "copy", "path", PAGE,
				"initScript", clipboard("t => { window.__copied = t; return Promise.resolve(); }"),
				"actions", List.of(waitFor(READY),
					m("click", ".jc-dash-presets button:nth-child(1)"),
					m("fill", List.of(".jc-dash-rail input[type=search]", "o")),
					pause(600), waitFor(READY),
					m("click", ".jc-dash-actions button:has-text(\"Copy link\")"),
					waitFor("window.__copied"),
					m("evaluate", "sessionStorage.setItem('copied', window.__copied);"
						+ " sessionStorage.setItem('labels', Array.from(document.querySelectorAll('.jc-dash-actions button')).map(b => b.textContent).join('|'))"),
					pause(700),
					m("evaluate", "localStorage.clear()"),
					// A hash-only change would not reload the page, so the link is opened with an extra query parameter.
					// URL.searchParams keeps any ?state= / ?variant= / ?v= query and the #s= fragment intact.
					m("follow", "(u => { u.searchParams.set('shared', '1'); return u.href; })(new URL(sessionStorage.getItem('copied')))"),
					waitFor(READY)),
				"queries", m(
					"copied", "sessionStorage.getItem('copied')",
					"labels", "sessionStorage.getItem('labels')",
					"presets", PRESETS,
					"search", SEARCH + ".value",
					"hash", "location.hash")),
			m("name", "copy-fallback", "path", PAGE,
				"initScript", clipboard("() => Promise.reject(new Error('denied'))"),
				"actions", List.of(waitFor(READY),
					m("click", ".jc-dash-actions button:has-text(\"Copy link\")"),
					waitFor(COPY_FIELD)),
				"queries", m(
					"value", COPY_FIELD + ".value",
					"focused", "document.activeElement === " + COPY_FIELD,
					"selected", "(i => i.selectionStart === 0 && i.selectionEnd === i.value.length)(" + COPY_FIELD + ")",
					"hint", "document.querySelector('.jc-dash-copy-field span').textContent")),
			m("name", "roundtrip", "path", PAGE,
				"actions", List.of(waitFor(READY),
					m("click", ".jc-dash-choice label:has-text(\"late\") input"),
					pause(600), waitFor(READY),
					m("evaluate", "sessionStorage.setItem('mid', " + CHOICES + ")"),
					m("click", ".jc-dash-presets button:nth-child(1)"),
					m("fill", List.of(".jc-dash-rail input[type=search]", "t")),
					pause(600), waitFor(READY),
					m("evaluate", CHART + ".dispatchAction({type: 'dataZoom', start: 20, end: 80})"),
					pause(1500),
					m("reload", true), waitFor(READY), pause(500)),
				"queries", m(
					"presets", PRESETS,
					"search", SEARCH + ".value",
					"choices", CHOICES,
					"mid", "sessionStorage.getItem('mid')",
					"zoom", "(o => o.dataZoom[0].start + '-' + o.dataZoom[0].end)(" + CHART + ".getOption())",
					"savedV", SAVED + ".v")),
			m("name", "version", "path", PAGE,
				"actions", List.of(waitFor(READY),
					m("click", ".jc-dash-presets button:nth-child(1)"),
					pause(800),
					m("goto", PAGE + "?v=2"), waitFor(READY), pause(800)),
				"queries", m(
					"presets", PRESETS,
					"savedV", SAVED + ".v",
					"savedRange", SAVED + ".topics['filter:range'].options.range"))));
	}

	@Test void a01_railRendersEverySectionKind() {
		var c = assertClean(report, "rail");
		assertString("Time range|Search|Reasons|-|-", query(c, "legends"));
		assertString("7d:false|30d:true|YTD:false", query(c, "presets"));
		assertString("2", String.valueOf(query(c, "utcInputs")));
		assertString("Reason contains", query(c, "placeholder"));
		assertString("damaged:true|late:true|lost:true|wrong-item:true", query(c, "choices"));
		assertEquals(true, query(c, "cardSection"));
		assertEquals(true, query(c, "custom"));
		assertString("0", String.valueOf(query(c, "errorBoxes")));
		// The custom section reaches the source: lateOnly leaves one reason in the frame.
		assertString("late:true", query(assertClean(report, "late"), "choices"));
	}

	@Test void a02_splitterDragAndKeyboardSurviveAReload() {
		var c = assertClean(report, "splitter");
		// 280 default, +64 by drag, +16 by ArrowRight (the splitter keyboard step).
		assertString("360", query(c, "valueNow"));
		assertString("360", String.valueOf(query(c, "railPx")));
		assertString("360", String.valueOf(query(c, "stored")));
	}

	@Test void a03_gridIsTwoColumnsWideAndOneNarrow() {
		var w = assertClean(report, "wide");
		assertEquals("2", String.valueOf(query(w, "cols")), () -> "dashboard width " + query(w, "dashPx"));
		assertEquals(true, query(w, "railShown"));
		assertEquals(false, query(w, "toggleShown"));
		var n = assertClean(report, "narrow");
		assertString("1", String.valueOf(query(n, "cols")));
		assertString("none", query(n, "before"));
		assertEquals(true, query(n, "railShown"));
		assertString("true", query(n, "expanded"));
	}

	@Test void a04_spans() {
		assertEquals(true, query(assertClean(report, "wide"), "spanFull"));
		assertEquals(true, query(assertClean(report, "wide"), "pairRow"));
		assertEquals(true, query(assertClean(report, "narrow"), "sameWidth"));   // spans collapse to one column
	}

	@Test void a04b_rightRailPrecedesTheGridWhenNarrow() {
		var c = assertClean(report, "narrow-right");
		assertString("right", query(c, "side"));
		// The toggle comes first even though the rail sits on the right on a wide page.
		assertTrue(num(c, "filtersTop") < num(c, "gridTop"), () -> "filters " + query(c, "filtersTop") + " grid " + query(c, "gridTop"));
		assertEquals(true, query(c, "railShown"));
		// Opening the rail pushes the grid down, so the rail is compared with the grid's position after the click.
		assertTrue(num(c, "railTop") < num(c, "gridTopOpen"), () -> "rail " + query(c, "railTop") + " grid " + query(c, "gridTopOpen"));
		assertTrue(num(c, "filtersTop") < num(c, "railTop"), () -> "filters " + query(c, "filtersTop") + " rail " + query(c, "railTop"));
		assertEquals(false, query(c, "splitterShown"));
	}

	private static double num(Map<String,Object> c, String key) {
		return ((Number) query(c, key)).doubleValue();
	}

	@Test void a05_statusLoadingThenReady() {
		var c = assertClean(report, "slow");
		assertTrue(((String) query(c, "loading")).startsWith("Loading"), () -> "loading: " + query(c, "loading"));
		assertTrue(((String) query(c, "ready")).matches("3[01] rows · loaded just now"), () -> "ready: " + query(c, "ready"));
		assertString("normal", query(c, "tone"));
	}

	@Test void a06_statusTruncatedAndError() {
		var t = assertClean(report, "truncated");
		assertString("20 rows · loaded just now · truncated at 20", query(t, "text"));
		assertString("warning", query(t, "tone"));
		// The failed load is logged by the data source (one console.error per failed generation), so this
		// case is not "clean". Every logged failure must be that line; how many generations run before the sections settle
		// is the source's debounce, not this test's.
		var e = report.get("error");
		assertString("200", String.valueOf(e.get("status")));
		assertEquals(List.of(), e.get("timeouts"));
		assertTrue(((String) query(e, "text")).startsWith("Load failed: "), () -> "error: " + query(e, "text"));
		assertString("error", query(e, "tone"));
		var failures = (List<?>) e.get("jsFailures");
		assertFalse(failures.isEmpty(), "the source logged nothing for a failed load");
		for (var f : failures)
			assertTrue(String.valueOf(f).startsWith("[juneau-data] E-JS-62: data source 'orders' fetch failed: ")
				|| String.valueOf(f).startsWith("[juneau-charts] E-JS-62: chart card 'daily' fetch failed: "), () -> "unexpected failure: " + f);
	}

	@Test void a07_refreshAndClearCacheReachTheSource() {
		var c = assertClean(report, "refresh");
		assertString("refresh:false|refresh:true", query(c, "cmds"));
		assertString("2", String.valueOf(query(c, "served")));
	}

	@Test void a08_copyLinkRestoresTheView() {
		var c = assertClean(report, "copy");
		assertTrue(((String) query(c, "copied")).matches("http://[^/]+/console/dev/dashboard#s=[A-Za-z0-9_-]+"), () -> "copied: " + query(c, "copied"));
		assertContains("Copied", query(c, "labels"));
		assertString("7d:true|30d:false|YTD:false", query(c, "presets"));
		assertString("o", query(c, "search"));
		assertString("", query(c, "hash"));   // the fragment is removed once read
	}

	@Test void a09_copyLinkFallbackField() {
		var c = assertClean(report, "copy-fallback");
		assertTrue(((String) query(c, "value")).contains("/console/dev/dashboard#s="), () -> "value: " + query(c, "value"));
		assertEquals(true, query(c, "focused"));
		assertEquals(true, query(c, "selected"));
		assertString("Press Ctrl+C / Cmd+C", query(c, "hint"));
	}

	@Test void a10_stateRoundTripsThroughAReload() {
		var c = assertClean(report, "roundtrip");
		assertString("7d:true|30d:false|YTD:false", query(c, "presets"));
		assertString("t", query(c, "search"));
		// late was unticked while all four were present, so it leaves the frame and stays as a greyed, unticked row.
		assertString("damaged:true|late:false:missing|lost:true|wrong-item:true", query(c, "mid"));
		// After the reload the unticked value is restored from the saved state, so late is still listed, unticked. The
		// search then drops damaged from the frame, so it stays as a ticked, greyed row.
		assertString("damaged:true:missing|late:false:missing|lost:true|wrong-item:true", query(c, "choices"));
		assertString("20-80", query(c, "zoom"));
		assertString("1", String.valueOf(query(c, "savedV")));
	}

	@Test void a11_versionBumpRestoresDefaultsAndReplacesTheOldState() {
		var c = assertClean(report, "version");
		assertString("7d:false|30d:true|YTD:false", query(c, "presets"));
		assertString("2", String.valueOf(query(c, "savedV")));
		assertString("30d", query(c, "savedRange"));
	}

	@Test void a12_clearCacheMenuOpensUnderItsButton() {
		var c = assertClean(report, "refresh");
		var r = String.valueOf(query(c, "menuRect")).split("\\|");
		assertEquals(4, r.length, () -> "menuRect: " + query(c, "menuRect"));
		var menuTop = Double.parseDouble(r[0]);
		var buttonBottom = Double.parseDouble(r[1]);
		var menuRight = Double.parseDouble(r[2]);
		var buttonRight = Double.parseDouble(r[3]);
		assertTrue(menuTop >= buttonBottom - 1, () -> "menu top " + menuTop + " is above the button bottom " + buttonBottom);
		assertTrue(Math.abs(menuRight - buttonRight) <= 4, () -> "menu right " + menuRight + " is far from the button right " + buttonRight);
	}
}
