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
 * The standard button kinds in a real browser (spec section 2): every kind's computed colours at rest and on hover,
 * selected = hover for ribbon toggles and in-dialog section tabs, page tabs and detail strips unchanged, the
 * bare-button default and its limits, for each stock palette in both schemes and in both link orders of
 * {@code chrome.css} and {@code juneau-views.css} (cascade Rule 3).
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class ConsoleButtons_BrowserTest extends TestBase {

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");
	/** {@code cv}: chrome.css then juneau-views.css (every real page); {@code vc}: the reverse. */
	private static final List<String> ORDERS = List.of("cv", "vc");

	private static final String[] TOKENS = { "--jc-control-bg", "--jc-text", "--jc-border", "--jc-hover-bg", "--jc-control-border", "--jc-accent",
		"--jc-accent-wash", "--jc-btn-primary-bg", "--jc-btn-primary-bg-hover", "--jc-on-btn-primary", "--jc-danger",
		"--jc-danger-strong", "--jc-danger-text", "--jc-danger-wash", "--jc-accent-selected", "--jc-on-accent", "--jc-link" };

	/** Elements probed at rest, and the elements hovered (each recorded under {@code hover:<id>}). */
	private static final List<String> HOVERED = List.of("sec", "pri", "des", "dan", "lnk", "tib", "dis", "bare", "rb", "ds2", "dc", "dd");

	private static final String BODY = """
		<div id="park" style="height:40px"></div>
		<main class="jc-main"><div class="jc-card">
		<button type="button" class="jc-btn" id="sec">Secondary</button>
		<button type="button" class="jc-btn jc-btn-secondary" id="sec2">Secondary 2</button>
		<button type="button" class="jc-btn jc-btn-primary" id="pri">Primary</button>
		<button type="button" class="jc-btn jc-btn-destructive" id="des">Delete</button>
		<button type="button" class="jc-btn jc-btn-danger" id="dan">Drop</button>
		<button type="button" class="jc-btn jc-btn-link" id="lnk">Reset</button>
		<button type="button" class="jc-btn" id="dis" disabled title="Not now">Disabled</button>
		<span class="jc-btn-group"><button type="button" class="jc-btn" id="g1">One</button><button type="button" class="jc-btn" id="g2" aria-pressed="true">Two</button></span>
		<button type="button" class="jc-tbl-icon-btn" id="tib" aria-label="Filter">F</button>
		<button type="button" class="jc-tbl-icon-btn" id="tibp" aria-pressed="true" aria-label="Filter on">F</button>
		<button type="button" class="jc-icon-btn" id="ico" aria-label="Help">?</button>
		<button type="button" id="bare">Bare</button>
		<button type="button" id="bareapp" data-app>App</button>
		<div class="juneau-view-ribbon"><button type="button" class="juneau-view-ribbon-btn" id="rb">R</button><button type="button" class="juneau-view-ribbon-btn" id="rbp" aria-pressed="true">P</button></div>
		<div class="juneau-view-ribbon-group juneau-view-dialog-sections" data-juneau-strip-mode="tab" role="tablist">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="ds1" aria-selected="true">Basics</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="ds2" aria-selected="false">More</button></div>
		<div class="juneau-view-ribbon-group" data-juneau-strip-mode="tab" role="tablist">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="st1" aria-selected="true">Detail</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="st2" aria-selected="false">Other</button></div>
		<nav><a class="jc-tab jc-tab-active" id="tab" href="#">Tab</a><a class="jc-subtab jc-subtab-active" id="sub" href="#">Sub</a></nav>
		<div class="juneau-view-dialog-actions"><button type="button" class="juneau-view-dialog-cancel" id="dc">Cancel</button><button type="button" class="juneau-view-dialog-close" id="dx">Close</button><button type="button" class="juneau-view-dialog-confirm" id="dk">Save</button><button type="button" class="juneau-view-dialog-confirm juneau-view-dialog-confirm-danger" id="dd">Delete</button></div>
		</div></main>
		<div><button type="button" id="outside">Outside</button></div>
		<div id="tokprobe" style="display:none"></div>
		""";

	/** Records computed styles; {@code __rec(key, id)} stores one snapshot, the probe reports them all. */
	private static final String PROBE = """
		<script>
		(function () {
			var PROPS = ['background-color', 'color', 'border-top-color', 'border-left-color', 'border-top-width', 'opacity',
				'cursor', 'pointer-events', 'z-index', 'margin-left', 'width', 'height', 'border-top-left-radius',
				'text-decoration-line', 'filter'];
			var rec = {};
			function snap(id) {
				var cs = getComputedStyle(document.getElementById(id));
				var o = {};
				PROPS.forEach(function (p) { o[p] = cs.getPropertyValue(p); });
				return o;
			}
			window.__rec = function (key, id) { rec[key] = snap(id); };
			window.__probe = { toJSON: function () {
				var tok = {};
				var t = document.getElementById('tokprobe');
				%s.forEach(function (n) { t.style.color = 'var(' + n + ')'; tok[n] = getComputedStyle(t).color; });
				var rest = {};
				['sec','sec2','pri','des','dan','lnk','dis','g1','g2','tib','tibp','ico','bare','bareapp','rb','rbp','ds1','ds2','st1','st2','tab','sub','dc','dx','dk','dd','outside']
					.forEach(function (id) { rest[id] = snap(id); });
				return { tok: tok, rest: rest, hover: rec };
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
			+ "<style>button[data-app] { background-color: rgb(255, 0, 0); }</style>\n"
			+ "</head><body>\n" + BODY + PROBE.formatted(tokens) + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("buttons");
		for (var p : PALETTES)
			f.asset("/juneau-console/juneau-theme-" + p + ".css", resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");
		for (var p : PALETTES)
			for (var s : SCHEMES)
				for (var o : ORDERS) {
					f.page(caseName(p, s, o), ORIGIN + "/" + caseName(p, s, o), page(p, s, o));
					for (var id : HOVERED)
						f.hover("#" + id).eval("window.__rec('" + id + "', '" + id + "')");
					// Park the pointer off every button so the rest snapshot is not hovered.
					f.hover("#park");
				}
		report = f.run();
	}

	private record Probe(String name, Map<String,String> tok, Map<String,Map<String,String>> rest, Map<String,Map<String,String>> hover) {
		String tok(String n) { return tok.get(n); }
		String rest(String id, String prop) { return rest.get(id).get(prop); }
		String hover(String id, String prop) { return hover.get(id).get(prop); }
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
					var pr = probe(r);
					out.add(new Probe(name, (Map<String,String>)pr.get("tok"), (Map<String,Map<String,String>>)pr.get("rest"),
						(Map<String,Map<String,String>>)pr.get("hover")));
				}
		return out;
	}

	private static final String TRANSPARENT = "rgba(0, 0, 0, 0)";

	private static void eq(Probe p, String expected, String actual, String what) {
		assertEquals(expected, actual, () -> p.name() + ": " + what);
	}

	@Test void a01_secondaryIsTheDefaultFace() {
		for (var p : probes()) {
			for (var id : List.of("sec", "sec2", "dc", "dx")) {
				eq(p, p.tok("--jc-control-bg"), p.rest(id, "background-color"), id + " rest bg");
				eq(p, p.tok("--jc-text"), p.rest(id, "color"), id + " rest ink");
				eq(p, p.tok("--jc-control-border"), p.rest(id, "border-top-color"), id + " rest edge");
				eq(p, "1px", p.rest(id, "border-top-width"), id + " edge width");
			}
			for (var id : List.of("sec", "dc")) {
				eq(p, p.tok("--jc-hover-bg"), p.hover(id, "background-color"), id + " hover bg");
				eq(p, p.tok("--jc-accent"), p.hover(id, "border-top-color"), id + " hover edge");
			}
		}
	}

	@Test void a02_primaryAndDialogConfirm() {
		for (var p : probes()) {
			for (var id : List.of("pri", "dk")) {
				eq(p, p.tok("--jc-btn-primary-bg"), p.rest(id, "background-color"), id + " rest bg");
				eq(p, p.tok("--jc-on-btn-primary"), p.rest(id, "color"), id + " ink");
			}
			eq(p, p.tok("--jc-btn-primary-bg-hover"), p.hover("pri", "background-color"), "pri hover bg");
		}
	}

	@Test void a03_destructiveAndDangerConfirm() {
		for (var p : probes()) {
			for (var id : List.of("des", "dd")) {
				eq(p, p.tok("--jc-danger"), p.rest(id, "background-color"), id + " rest bg");
				eq(p, p.tok("--jc-on-btn-primary"), p.rest(id, "color"), id + " ink");
				eq(p, p.tok("--jc-danger-strong"), p.hover(id, "background-color"), id + " hover bg");
				eq(p, "none", p.hover(id, "filter"), id + " hover has no filter");
			}
		}
	}

	@Test void a04_dangerOutlineUnchanged() {
		for (var p : probes()) {
			eq(p, p.tok("--jc-control-bg"), p.rest("dan", "background-color"), "dan rest bg");
			eq(p, p.tok("--jc-danger-text"), p.rest("dan", "color"), "dan ink");
			eq(p, p.tok("--jc-danger-wash"), p.hover("dan", "background-color"), "dan hover wash");
		}
	}

	@Test void a05_link() {
		for (var p : probes()) {
			eq(p, TRANSPARENT, p.rest("lnk", "background-color"), "link bg");
			eq(p, TRANSPARENT, p.rest("lnk", "border-top-color"), "link edge");
			eq(p, p.tok("--jc-link"), p.rest("lnk", "color"), "link ink");
			eq(p, "none", p.rest("lnk", "text-decoration-line"), "link rest underline");
			eq(p, "underline", p.hover("lnk", "text-decoration-line"), "link hover underline");
		}
	}

	@Test void a06_toggleGroup() {
		for (var p : probes()) {
			eq(p, "-1px", p.rest("g2", "margin-left"), "joined member");
			eq(p, "1", p.rest("g2", "z-index"), "selected member on top");
			eq(p, p.tok("--jc-hover-bg"), p.rest("g2", "background-color"), "selected member bg = hover");
			eq(p, p.tok("--jc-accent"), p.rest("g2", "border-top-color"), "selected member edge = hover");
		}
	}

	@Test void a07_tableIconButton() {
		for (var p : probes()) {
			eq(p, "30px", p.rest("tib", "width"), "width");
			eq(p, "30px", p.rest("tib", "height"), "height");
			eq(p, p.tok("--jc-control-bg"), p.rest("tib", "background-color"), "rest bg");
			for (var prop : List.of("background-color", "border-top-color"))
				eq(p, p.hover("tib", prop), p.rest("tibp", prop), "pressed = hover: " + prop);
		}
	}

	@Test void a08_disabled() {
		for (var p : probes()) {
			eq(p, "0.5", p.rest("dis", "opacity"), "dim");
			eq(p, "default", p.rest("dis", "cursor"), "cursor");
			eq(p, "auto", p.rest("dis", "pointer-events"), "keeps pointer events (title tooltip)");
			eq(p, p.rest("dis", "background-color"), p.hover("dis", "background-color"), "no hover face");
			eq(p, p.rest("dis", "border-top-color"), p.hover("dis", "border-top-color"), "no hover edge");
		}
	}

	@Test void a09_bareButton() {
		for (var p : probes()) {
			eq(p, p.tok("--jc-control-bg"), p.rest("bare", "background-color"), "bare rest bg");
			eq(p, p.tok("--jc-control-border"), p.rest("bare", "border-top-color"), "bare rest edge");
			eq(p, p.tok("--jc-hover-bg"), p.hover("bare", "background-color"), "bare hover bg");
			eq(p, "rgb(255, 0, 0)", p.rest("bareapp", "background-color"), "an app rule wins");
			assertNotEquals(p.tok("--jc-border"), p.rest("outside", "border-top-color"), () -> p.name() + ": outside the roots is untouched");
		}
	}

	@Test void a10_ribbonPressedLooksLikeHover() {
		for (var p : probes()) {
			for (var prop : List.of("background-color", "color", "border-top-color", "border-left-color"))
				eq(p, p.hover("rb", prop), p.rest("rbp", prop), "ribbon pressed = hover: " + prop);
			assertNotEquals(p.rest("rb", "background-color"), p.rest("rbp", "background-color"), () -> p.name() + ": pressed differs from rest");
			eq(p, p.tok("--jc-accent-wash"), p.rest("rbp", "background-color"), "pressed wash");
			eq(p, p.tok("--jc-link"), p.rest("rbp", "color"), "pressed ink");
		}
	}

	@Test void a11_dialogSectionSelectedLooksLikeHover() {
		for (var p : probes())
			for (var prop : List.of("background-color", "color", "border-top-color", "border-left-color"))
				eq(p, p.hover("ds2", prop), p.rest("ds1", prop), "dialog section selected = hover: " + prop);
	}

	@Test void a12_tabsAndDetailStripUnchanged() {
		for (var p : probes()) {
			for (var id : List.of("st1", "tab", "sub")) {
				eq(p, p.tok("--jc-accent-selected"), p.rest(id, "background-color"), id + " solid selected fill");
				eq(p, p.tok("--jc-on-accent"), p.rest(id, "color"), id + " selected ink");
			}
		}
	}

	@Test void a13_iconButtonStaysRound() {
		for (var p : probes())
			eq(p, "50%", p.rest("ico", "border-top-left-radius"), ".jc-icon-btn radius");
	}
}
