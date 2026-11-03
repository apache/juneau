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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The standard table toolbar in a real browser (spec section 4): positional ribbon edges, accent hover, selected =
 * hover, the toolbar Search border, whole-group wrapping and paging on the left, for each stock palette in both
 * schemes and in both link orders of {@code chrome.css} and {@code juneau-views.css} (cascade Rule 3).
 *
 * <p>
 * The markup is the DOM {@code buildToolbarRow} (juneau-views.js) and {@code buildRibbon} (juneau-ribbon.js) emit for a
 * canonically tagged ribbon, written out statically so the layout is measured without DataTables.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class Toolbar_BrowserTest extends TestBase {

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");
	/** {@code cv}: chrome.css then juneau-views.css (every real page); {@code vc}: the reverse. */
	private static final List<String> ORDERS = List.of("cv", "vc");
	private static final List<Integer> WIDTHS = List.of(1200, 900, 600, 400);

	private static final String[] TOKENS = { "--jc-accent", "--jc-link", "--jc-accent-wash", "--jc-control-border", "--jc-control-bg",
		"--jc-chrome-control-focus-border" };

	/** Elements hovered, each recorded under {@code hover:<id>}. */
	private static final List<String> HOVERED = List.of("vf", "vm", "vl", "pgn");

	private static final String BTN = "<button type=\"button\" class=\"juneau-view-ribbon-btn\"";

	private static final String BODY = """
		<div id="park" style="height:40px"></div>
		<main class="jc-main"><div class="jc-card"><div id="host" class="dt-container">
		<div class="juneau-view-toolbar-row">
		<div class="juneau-view-toolbar-left"><div class="juneau-view-pagingpill" data-testid="paging">
		<button type="button" class="juneau-view-pagingpill-btn" id="pgf" disabled>F</button>
		<button type="button" class="juneau-view-pagingpill-btn" id="pgp" disabled>P</button>
		<span class="juneau-view-pagingpill-menuwrap"><button type="button" class="juneau-view-pagingpill-menubtn">1-25 of 300</button></span>
		<button type="button" class="juneau-view-pagingpill-btn" id="pgn">N</button>
		<button type="button" class="juneau-view-pagingpill-btn" id="pgl">L</button>
		</div></div>
		<div class="juneau-view-toolbar-right"><div class="juneau-view-ribbon" data-testid="ribbon">
		<span class="juneau-view-ribbon-group" data-juneau-group="entity">%1$s id="e1">New</button>%1$s id="e2">Edit</button></span>
		<span class="juneau-view-ribbon-group" data-juneau-group="filter">%1$s id="f1">A</button>%1$s id="f2">B</button>%1$s id="f3">C</button></span>
		<div class="dt-search"><input type="search" id="q" placeholder="Search"></div>
		<span class="juneau-view-ribbon-group" data-juneau-group="viewing">%1$s id="vf">X</button>%1$s id="vm">X</button>%1$s id="vp" aria-pressed="true">X</button>%1$s id="vl">X</button></span>
		<span class="juneau-view-ribbon-group" data-juneau-group="page">%1$s id="p1">R</button>%1$s id="p2">D</button></span>
		<span class="juneau-view-ribbon-group" data-juneau-group="status">%1$s id="s1">S</button></span>
		</div></div>
		</div>
		</div></div></main>
		<div id="tokprobe" style="display:none"></div>
		""".formatted(BTN);

	/** Records computed styles and the toolbar layout at each width; the probe reports them all. */
	private static final String PROBE = """
		<script>
		(function () {
			var PROPS = ['background-color', 'color', 'border-top-color', 'border-bottom-color', 'border-left-color',
				'border-right-color', 'border-top-width', 'border-left-width', 'border-right-width', 'width', 'height'];
			var rec = {};
			function snap(id) {
				var cs = getComputedStyle(document.getElementById(id));
				var o = {};
				PROPS.forEach(function (p) { o[p] = cs.getPropertyValue(p); });
				return o;
			}
			function layout(w) {
				document.getElementById('host').style.width = w + 'px';
				var pill = document.querySelector('.juneau-view-pagingpill').getBoundingClientRect();
				var groups = Array.from(document.querySelectorAll('.juneau-view-toolbar-right .juneau-view-ribbon-group')).map(function (g) {
					var tops = Array.from(g.children).map(function (b) { return Math.round(b.getBoundingClientRect().top); });
					var r = g.getBoundingClientRect();
					return { id: g.getAttribute('data-juneau-group'), rows: new Set(tops).size, left: r.left, right: r.right, top: r.top };
				});
				var row = document.querySelector('.juneau-view-toolbar-row').getBoundingClientRect();
				return { pill: { left: pill.left, right: pill.right, top: pill.top, bottom: pill.bottom }, groups: groups,
					rowRight: row.right, rowScroll: document.querySelector('.juneau-view-toolbar-row').scrollWidth - row.width };
			}
			window.__rec = function (key, id) { rec[key] = snap(id); };
			window.__probe = { toJSON: function () {
				var tok = {};
				var t = document.getElementById('tokprobe');
				%s.forEach(function (n) { t.style.color = 'var(' + n + ')'; tok[n] = getComputedStyle(t).color; });
				var rest = {};
				['vf','vm','vp','vl','pgn','q'].forEach(function (id) { rest[id] = snap(id); });
				var q = document.getElementById('q');
				q.focus();
				var focus = snap('q');
				q.blur();
				var lay = {};
				%s.forEach(function (w) { lay[w] = layout(w); });
				return { tok: tok, rest: rest, hover: rec, focus: focus, layout: lay };
			} };
		})();
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	private static String caseName(String palette, String scheme, String order) {
		return palette + "-" + scheme + "-" + order;
	}

	private static String page(String palette, String scheme, String order) {
		var chrome = "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n";
		var views = "<link rel=\"stylesheet\" href=\"" + VIEWS_CSS + "\">\n";
		var themes = "<link rel=\"stylesheet\" href=\"" + DARK_CSS + "\">\n<link rel=\"stylesheet\" href=\"/juneau-console/juneau-theme-" + palette + ".css\">\n";
		var attrs = "data-jc-scheme=\"" + scheme + "\" data-jc-scheme-pin=\"" + scheme + "\"";
		var tokens = new StringBuilder("[");
		for (var t : TOKENS)
			tokens.append(tokens.length() == 1 ? "" : ",").append('\'').append(t).append('\'');
		tokens.append(']');
		return "<!DOCTYPE html>\n<html lang=\"en\" " + attrs + "><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n"
			+ (order.equals("cv") ? chrome + themes + views : views + chrome + themes)
			+ "</head><body>\n" + BODY + PROBE.formatted(tokens, WIDTHS) + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("toolbar");
		for (var p : PALETTES)
			f.asset("/juneau-console/juneau-theme-" + p + ".css", resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");
		for (var p : PALETTES)
			for (var s : SCHEMES)
				for (var o : ORDERS) {
					f.page(caseName(p, s, o), ORIGIN + "/" + caseName(p, s, o), page(p, s, o));
					for (var id : HOVERED)
						f.hover("#" + id).eval("window.__rec('" + id + "', '" + id + "')");
					f.hover("#park");
				}
		report = f.run();
	}

	private record Probe(String name, Map<String,Object> pr) {
		String tok(String n) { return ((Map<String,String>)pr.get("tok")).get(n); }
		String rest(String id, String prop) { return ((Map<String,Map<String,String>>)pr.get("rest")).get(id).get(prop); }
		String hover(String id, String prop) { return ((Map<String,Map<String,String>>)pr.get("hover")).get(id).get(prop); }
		String focus(String prop) { return ((Map<String,String>)pr.get("focus")).get(prop); }
		Map<String,Object> layout(int w) { return ((Map<String,Map<String,Object>>)pr.get("layout")).get(String.valueOf(w)); }
	}

	private static List<Probe> probes() {
		var out = new ArrayList<Probe>();
		for (var p : PALETTES)
			for (var s : SCHEMES)
				for (var o : ORDERS) {
					var name = caseName(p, s, o);
					var r = report.get(name);
					assertNotNull(r, () -> "no report for " + name);
					assertEquals(List.of(), r.get("pageErrors"), () -> name + ": page errors");
					out.add(new Probe(name, probe(r)));
				}
		return out;
	}

	private static void eq(Probe p, String expected, String actual, String what) {
		assertEquals(expected, actual, () -> p.name() + ": " + what);
	}

	@Test void a01_noVerticalBorderBetweenAdjacentButtons() {
		for (var p : probes()) {
			eq(p, "1px", p.rest("vf", "border-left-width"), "first keeps its outer left edge");
			eq(p, "0px", p.rest("vf", "border-right-width"), "first has no inner right edge");
			for (var id : List.of("vm", "vp")) {
				eq(p, "0px", p.rest(id, "border-left-width"), id + " inner left edge");
				eq(p, "0px", p.rest(id, "border-right-width"), id + " inner right edge");
			}
			eq(p, "0px", p.rest("vl", "border-left-width"), "last has no inner left edge");
			eq(p, "1px", p.rest("vl", "border-right-width"), "last keeps its outer right edge");
			eq(p, p.tok("--jc-control-border"), p.rest("vm", "border-top-color"), "rest edge");
		}
	}

	@Test void a02_hoverPaintsTheButtonsOwnEdgesAccent() {
		for (var p : probes()) {
			for (var id : List.of("vf", "vm", "vl")) {
				eq(p, p.tok("--jc-accent"), p.hover(id, "border-top-color"), id + " hover top");
				eq(p, p.tok("--jc-accent"), p.hover(id, "border-bottom-color"), id + " hover bottom");
				eq(p, p.tok("--jc-accent-wash"), p.hover(id, "background-color"), id + " hover wash");
				eq(p, p.tok("--jc-link"), p.hover(id, "color"), id + " hover ink");
			}
			eq(p, "0px", p.hover("vm", "border-left-width"), "inner hover gains no left edge");
			eq(p, "0px", p.hover("vm", "border-right-width"), "inner hover gains no right edge");
			eq(p, p.tok("--jc-accent"), p.hover("vf", "border-left-color"), "first hover outer left");
			eq(p, p.tok("--jc-accent"), p.hover("vl", "border-right-color"), "last hover outer right");
		}
	}

	@Test void a03_selectedEqualsHover() {
		for (var p : probes())
			for (var prop : List.of("background-color", "color", "border-top-color", "border-bottom-color", "border-top-width",
					"border-left-width", "border-right-width", "width", "height"))
				eq(p, p.hover("vm", prop), p.rest("vp", prop), "pressed = hovered twin: " + prop);
	}

	/** Inside {@code div.dt-container} (every real table): the views paging rules must not out-specify the hover edges. */
	@Test void a04_pagingHoverPaintsTopAndBottomAccent() {
		for (var p : probes()) {
			eq(p, p.tok("--jc-control-border"), p.rest("pgn", "border-top-color"), "paging rest top");
			eq(p, p.tok("--jc-accent"), p.hover("pgn", "border-top-color"), "paging hover top");
			eq(p, p.tok("--jc-accent"), p.hover("pgn", "border-bottom-color"), "paging hover bottom");
		}
	}

	@Test void a05_searchBorderAndFocus() {
		for (var p : probes()) {
			eq(p, p.tok("--jc-control-border"), p.rest("q", "border-top-color"), "search rest edge");
			eq(p, "1px", p.rest("q", "border-top-width"), "search edge width");
			eq(p, p.tok("--jc-control-bg"), p.rest("q", "background-color"), "search face");
			// The accent in light, --jc-focus in dark.
			eq(p, p.tok("--jc-chrome-control-focus-border"), p.focus("border-top-color"), "search focus edge");
		}
	}

	@Test void a06_groupsNeverSplitAndPagingIsLeft() {
		for (var p : probes())
			for (var w : WIDTHS) {
				var lay = p.layout(w);
				var pill = (Map<String,Number>)lay.get("pill");
				var at = p.name() + " @" + w + "px";
				for (var g : (List<Map<String,Object>>)lay.get("groups")) {
					assertEquals(1, ((Number)g.get("rows")).intValue(), () -> at + ": group " + g.get("id") + " split across rows");
					var top = ((Number)g.get("top")).doubleValue();
					var left = ((Number)g.get("left")).doubleValue();
					// Same line as the pill: strictly to its right.  A later line: never left of the pill's start.
					if (top < pill.get("bottom").doubleValue())
						assertTrue(left >= pill.get("right").doubleValue(), () -> at + ": group " + g.get("id") + " left of paging");
					else
						assertTrue(left >= pill.get("left").doubleValue(), () -> at + ": group " + g.get("id") + " left of paging");
				}
				assertTrue(((Number)lay.get("rowScroll")).doubleValue() <= 1, () -> at + ": toolbar overflows its row");
			}
	}
}
