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
 * The standard dialog frame in a real browser (spec section 1): header and footer dividers, the right-aligned footer
 * and the wizard footer, the close-&times; hover, pill and folder inner navigation, the wizard step rail, and the
 * secondary face on Back and in-form action buttons - for each stock palette in both schemes and in both link orders
 * of {@code chrome.css} and {@code juneau-views.css} (cascade Rule 3).
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class DialogFrame_BrowserTest extends TestBase {

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");
	/** {@code cv}: chrome.css then juneau-views.css (every real page); {@code vc}: the reverse. */
	private static final List<String> ORDERS = List.of("cv", "vc");

	private static final String[] TOKENS = { "--jc-control-bg", "--jc-text", "--jc-border", "--jc-hover-bg", "--jc-link", "--jc-control-border", "--jc-accent",
		"--jc-accent-wash", "--jc-btn-primary-bg", "--jc-on-btn-primary", "--jc-danger-text", "--jc-text-muted" };

	/** Elements hovered (each recorded under {@code hover:<id>}). */
	private static final List<String> HOVERED = List.of("dx", "p2", "f2", "wb", "fa");

	private static final String BODY = """
		<div id="park" style="height:40px"></div>
		<div class="juneau-view-dialog" id="dlg" data-nav="pill" style="width:40em">
		<div class="juneau-view-dialog-header" id="hd"><h2 class="juneau-view-dialog-title">Plain</h2><button type="button" class="juneau-view-dialog-dismiss" id="dx" aria-label="Close">&times;</button></div>
		<div class="juneau-view-dialog-form">
		<div class="juneau-view-ribbon-group juneau-view-dialog-sections" data-juneau-strip-mode="tab" role="tablist" id="ps">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="p1" aria-selected="true">Basics</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="p2" aria-selected="false">More</button></div>
		<button type="button" class="juneau-view-dialog-form-action" id="fa">Add item</button>
		<div class="juneau-view-dialog-form-row"><label for="cb">Plain</label><input type="checkbox" id="cb"></div>
		<div class="juneau-view-dialog-form-row"><label for="tg">Stock</label><input type="checkbox" role="switch" class="juneau-view-toggle" id="tg"></div>
		<div class="juneau-view-dialog-form-row"><label for="atg">App</label><input type="checkbox" role="switch" class="juneau-view-toggle app-toggle" id="atg"></div>
		</div>
		<div class="juneau-view-dialog-actions" id="ft"><button type="button" class="juneau-view-dialog-cancel" id="fc">Cancel</button><button type="button" class="juneau-view-dialog-confirm" id="fk">Save</button></div>
		</div>
		<div class="juneau-view-dialog" id="fdlg" data-nav="folder" style="width:40em">
		<div class="juneau-view-dialog-form">
		<div class="juneau-view-ribbon-group juneau-view-dialog-sections" data-juneau-strip-mode="tab" role="tablist" id="fs">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="f1" aria-selected="true">Basics</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="f2" aria-selected="false">More</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="f3" aria-selected="false">Rest</button></div>
		</div>
		</div>
		<div class="juneau-view-dialog" id="wdlg" data-nav="wizard" style="width:40em">
		<ol class="juneau-view-dialog-steps" id="rail">
		<li class="juneau-view-dialog-step" data-state="done" id="s1"><span class="juneau-view-dialog-step-mark" id="m1">&#10003;</span><span class="juneau-view-dialog-step-label">One</span></li>
		<li class="juneau-view-dialog-step" data-state="active" id="s2" aria-current="step"><span class="juneau-view-dialog-step-mark" id="m2">2</span><span class="juneau-view-dialog-step-label">Two</span></li>
		<li class="juneau-view-dialog-step" data-state="todo" id="s3"><span class="juneau-view-dialog-step-mark" id="m3">3</span><span class="juneau-view-dialog-step-label">Three</span></li></ol>
		<div class="juneau-view-dialog-actions" id="wft"><button type="button" class="juneau-view-dialog-cancel" id="wc">Cancel</button><span class="juneau-view-dialog-actions-spacer" id="wsp"></span><button type="button" class="juneau-view-dialog-back" id="wb">Back</button><button type="button" class="juneau-view-dialog-confirm" id="wn">Next</button></div>
		</div>
		<div id="tokprobe" style="display:none"></div>
		""";

	/** Records computed styles; {@code __rec(key, id)} stores one snapshot, the probe reports them all. */
	private static final String PROBE = """
		<script>
		(function () {
			var PROPS = ['background-color', 'color', 'border-top-color', 'border-bottom-color', 'border-top-width',
				'border-bottom-width', 'border-top-left-radius', 'border-bottom-left-radius', 'font-weight', 'height',
				'width', 'justify-self', 'justify-content', 'column-gap', 'padding-top', 'padding-left', 'margin-bottom', 'flex-grow'];
			var rec = {};
			function snap(id) {
				var el = document.getElementById(id);
				var cs = getComputedStyle(el);
				var o = {};
				PROPS.forEach(function (p) { o[p] = cs.getPropertyValue(p); });
				var r = el.getBoundingClientRect();
				o.left = r.left; o.right = r.right;
				return o;
			}
			function after(id, prop) { return getComputedStyle(document.getElementById(id), '::after').getPropertyValue(prop); }
			window.__rec = function (key, id) { rec[key] = snap(id); };
			window.__probe = { toJSON: function () {
				var tok = {};
				var t = document.getElementById('tokprobe');
				%s.forEach(function (n) { t.style.color = 'var(' + n + ')'; tok[n] = getComputedStyle(t).color; });
				var rest = {};
				['hd','dx','ps','p1','p2','fa','cb','tg','atg','ft','fc','fk','fs','f1','f2','f3','rail','m1','m2','m3','wft','wc','wsp','wb','wn']
					.forEach(function (id) { rest[id] = snap(id); });
				var bars = { s1: after('s1', 'border-top-color'), s2: after('s2', 'border-top-color'), s3: after('s3', 'content') };
				return { tok: tok, rest: rest, hover: rec, bars: bars };
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
			// An app's own toggle size, at single-class specificity and loaded last (as SSC's console.css does).
			+ "<style>.app-toggle { width: 50px; }</style>\n"
			+ "</head><body>\n" + BODY + PROBE.formatted(tokens) + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("dialog-frame");
		for (var p : PALETTES)
			f.asset("/juneau-console/juneau-theme-" + p + ".css", resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");
		for (var p : PALETTES)
			for (var s : SCHEMES)
				for (var o : ORDERS) {
					f.page(caseName(p, s, o), ORIGIN + "/" + caseName(p, s, o), page(p, s, o));
					for (var id : HOVERED)
						f.hover("#" + id).eval("window.__rec('" + id + "', '" + id + "')");
					// Park the pointer off every control so the rest snapshot is not hovered.
					f.hover("#park");
				}
		report = f.run();
	}

	private record Probe(String name, Map<String,String> tok, Map<String,Map<String,Object>> rest, Map<String,Map<String,Object>> hover, Map<String,String> bars) {
		String tok(String n) { return tok.get(n); }
		String rest(String id, String prop) { return String.valueOf(rest.get(id).get(prop)); }
		double x(String id, String edge) { return ((Number)rest.get(id).get(edge)).doubleValue(); }
		String hover(String id, String prop) { return String.valueOf(hover.get(id).get(prop)); }
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
					out.add(new Probe(name, (Map<String,String>)pr.get("tok"), (Map<String,Map<String,Object>>)pr.get("rest"),
						(Map<String,Map<String,Object>>)pr.get("hover"), (Map<String,String>)pr.get("bars")));
				}
		return out;
	}

	private static void eq(Probe p, Object expected, Object actual, String what) {
		assertEquals(expected, actual, () -> p.name() + ": " + what);
	}

	@Test void a01_headerAndFooterDividers() {
		for (var p : probes()) {
			eq(p, "1px", p.rest("hd", "border-bottom-width"), "header divider width");
			eq(p, p.tok("--jc-border"), p.rest("hd", "border-bottom-color"), "header divider colour");
			eq(p, "8px", p.rest("hd", "padding-top"), "header padding-y");
			eq(p, "10px", p.rest("hd", "padding-left"), "header padding-x");
			eq(p, "1px", p.rest("ft", "border-top-width"), "footer divider width");
			eq(p, p.tok("--jc-border"), p.rest("ft", "border-top-color"), "footer divider colour");
			eq(p, "8px", p.rest("ft", "padding-top"), "footer padding-y");
			eq(p, "10px", p.rest("ft", "padding-left"), "footer padding-x");
		}
	}

	@Test void a02_closeHoverIsDanger() {
		for (var p : probes())
			eq(p, p.tok("--jc-danger-text"), p.hover("dx", "color"), "close hover ink");
	}

	@Test void a03_footerRightAlignedPrimaryLast() {
		for (var p : probes()) {
			eq(p, "flex-end", p.rest("ft", "justify-content"), "footer alignment");
			eq(p, "8px", p.rest("ft", "column-gap"), "footer gap");
			assertTrue(p.x("fc", "right") < p.x("fk", "left"), () -> p.name() + ": primary after secondary");
			assertTrue(p.x("ft", "right") - p.x("fk", "right") <= 11, () -> p.name() + ": primary at the right edge");
		}
	}

	@Test void a04_wizardFooterCancelLeftSpacerBackNext() {
		for (var p : probes()) {
			eq(p, "1", p.rest("wsp", "flex-grow"), "spacer grows");
			assertTrue(p.x("wc", "left") - p.x("wft", "left") <= 11, () -> p.name() + ": Cancel at the left edge");
			assertTrue(p.x("wc", "right") < p.x("wsp", "left") + 1 && p.x("wsp", "right") < p.x("wb", "left") + 1
				&& p.x("wb", "right") < p.x("wn", "left"), () -> p.name() + ": Cancel, spacer, Back, Next");
			assertTrue(p.x("wsp", "right") - p.x("wsp", "left") > 50, () -> p.name() + ": spacer takes the slack");
			eq(p, p.tok("--jc-btn-primary-bg"), p.rest("wn", "background-color"), "Next is primary");
		}
	}

	@Test void a05_backAndFormActionAreSecondary() {
		for (var p : probes()) {
			for (var id : List.of("wb", "fa")) {
				eq(p, p.tok("--jc-control-bg"), p.rest(id, "background-color"), id + " rest bg");
				eq(p, p.tok("--jc-text"), p.rest(id, "color"), id + " rest ink");
				eq(p, p.tok("--jc-control-border"), p.rest(id, "border-top-color"), id + " rest edge");
				eq(p, p.tok("--jc-hover-bg"), p.hover(id, "background-color"), id + " hover bg");
				eq(p, p.tok("--jc-accent"), p.hover(id, "border-top-color"), id + " hover edge");
				eq(p, p.rest("fc", "height"), p.rest(id, "height"), id + " height = Cancel");
				eq(p, p.rest("fc", "padding-left"), p.rest(id, "padding-left"), id + " padding = Cancel");
			}
		}
	}

	@Test void a06_pillSelectedLooksLikeHover() {
		for (var p : probes()) {
			for (var prop : List.of("background-color", "color", "border-top-color"))
				eq(p, p.hover("p2", prop), p.rest("p1", prop), "pill selected = hover: " + prop);
			eq(p, "0px", p.rest("ps", "border-bottom-width"), "pill strip has no rule");
		}
	}

	@Test void a07_folderTabs() {
		for (var p : probes()) {
			eq(p, "1px", p.rest("fs", "border-bottom-width"), "folder strip rule width");
			eq(p, p.tok("--jc-border"), p.rest("fs", "border-bottom-color"), "folder strip rule colour");
			for (var id : List.of("f1", "f2", "f3")) {
				eq(p, "0px", p.rest(id, "border-bottom-width"), id + " open at the bottom");
				eq(p, "-1px", p.rest(id, "margin-bottom"), id + " stands on the rule");
				eq(p, "0px", p.rest(id, "border-bottom-left-radius"), id + " square bottom");
				eq(p, "4px", p.rest(id, "border-top-left-radius"), id + " rounded top");
				eq(p, "6px", p.rest(id, "padding-top"), id + " padding-y");
				eq(p, "14px", p.rest(id, "padding-left"), id + " padding-x");
			}
			eq(p, p.tok("--jc-border"), p.rest("f3", "border-top-color"), "rest tab edge");
			for (var prop : List.of("background-color", "color", "border-top-color"))
				eq(p, p.hover("f2", prop), p.rest("f1", prop), "folder selected = hover: " + prop);
			eq(p, p.tok("--jc-accent-wash"), p.rest("f1", "background-color"), "selected wash");
			eq(p, p.tok("--jc-link"), p.rest("f1", "color"), "selected ink");
			eq(p, p.tok("--jc-accent"), p.rest("f1", "border-top-color"), "selected accent edge");
			eq(p, "600", p.rest("f1", "font-weight"), "selected is bold");
			eq(p, "400", p.hover("f2", "font-weight"), "hover is not bold");
		}
	}

	@Test void a08_wizardRail() {
		for (var p : probes()) {
			for (var id : List.of("m1", "m2", "m3")) {
				eq(p, "20px", p.rest(id, "width"), id + " width");
				eq(p, "20px", p.rest(id, "height"), id + " height");
			}
			eq(p, p.tok("--jc-btn-primary-bg"), p.rest("m2", "background-color"), "active mark fill");
			eq(p, p.tok("--jc-on-btn-primary"), p.rest("m2", "color"), "active mark ink");
			eq(p, p.tok("--jc-accent"), p.rest("m1", "color"), "done mark ink");
			eq(p, p.tok("--jc-accent"), p.rest("m1", "border-top-color"), "done mark edge");
			eq(p, p.tok("--jc-text-muted"), p.rest("m3", "color"), "to-do mark ink");
			eq(p, p.tok("--jc-border"), p.rest("m3", "border-top-color"), "to-do mark edge");
			eq(p, p.tok("--jc-accent"), p.bars().get("s1"), "bar after a done step");
			eq(p, p.tok("--jc-border"), p.bars().get("s2"), "bar after the active step");
			eq(p, "none", p.bars().get("s3"), "no bar after the last step");
		}
	}

	@Test void a09_dialogCheckboxRuleLeavesTogglesAlone() {
		for (var p : probes()) {
			eq(p, "50px", p.rest("atg", "width"), "an app's toggle keeps its own width");
			eq(p, "start", p.rest("atg", "justify-self"), "app toggle stays inline");
			assertTrue(p.x("tg", "right") - p.x("tg", "left") > 20, () -> p.name() + ": stock toggle has its track width");
			eq(p, "start", p.rest("cb", "justify-self"), "plain checkbox inline");
			assertTrue(p.x("cb", "right") - p.x("cb", "left") < 30, () -> p.name() + ": plain checkbox is auto width, not full-row");
		}
	}
}
