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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The folder-tab look in Chromium, light and dark: the selected tab's fill, accent top bar and accent edge, the
 * white unselected tabs, labels that share one top, the padded panel, and the shorter row (including the compact
 * chart tabs).
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class FolderTabs_BrowserTest extends org.apache.juneau.TestBase {

	private static final List<String> SCHEMES = List.of("light", "dark");

	private static final String BODY = """
		<div class="juneau-view-helper-tabstrip-wrapper" data-juneau-strip-panel id="w">
		<div class="juneau-view-ribbon-group" data-juneau-strip-mode="tab" role="tablist" id="g">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="t1" aria-selected="true">Alpha</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="t2" aria-selected="false">Beta</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="t3" aria-selected="false">Gamma</button></div>
		<div class="juneau-view-helper-tabstrip-panes" id="pn">Pane</div></div>
		<div class="juneau-view-helper-tabstrip-wrapper" data-juneau-strip-panel data-juneau-strip-compact id="cw">
		<div class="juneau-view-ribbon-group" data-juneau-strip-mode="tab" role="tablist">
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="c1" aria-selected="true">Chart</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="c2" aria-selected="false">Data</button></div>
		<div class="juneau-view-helper-tabstrip-panes">Pane</div></div>
		<div id="tokprobe" style="display:none"></div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			var PROPS = ['background-color', 'color', 'border-top-color', 'border-top-width', 'border-bottom-width', 'border-bottom-color',
				'border-left-color', 'font-weight', 'padding-top', 'padding-left', 'box-shadow'];
			function snap(id) {
				var el = document.getElementById(id), cs = getComputedStyle(el), o = {};
				PROPS.forEach(function (p) { o[p] = cs.getPropertyValue(p); });
				var r = el.getBoundingClientRect(), rg = document.createRange();
				rg.selectNodeContents(el);
				o.height = r.height; o.top = r.top; o.labelTop = rg.getBoundingClientRect().top;
				return o;
			}
			function tok(expr, prop) {
				var t = document.getElementById('tokprobe'); t.style[prop] = expr;
				return getComputedStyle(t)[prop];
			}
			window.__probe = { toJSON: function () {
				var sel = document.getElementById('t1').getBoundingClientRect(), pn = document.getElementById('pn').getBoundingClientRect();
				var hit = document.elementFromPoint(sel.left + sel.width / 2, pn.top + 0.5);
				return {
					cover: { selBottom: sel.bottom, paneTop: pn.top, hit: hit ? hit.id : null },
					tabs: { t1: snap('t1'), t2: snap('t2'), t3: snap('t3'), c1: snap('c1'), c2: snap('c2') },
					pane: snap('pn'),
					tok: {
						accent: tok('var(--jc-accent)', 'color'), border: tok('var(--jc-border)', 'color'), card: tok('var(--jc-card-bg)', 'color'),
						wash: tok('var(--jc-tab-selected-bg)', 'backgroundColor'), link: tok('var(--jc-link)', 'color')
					}
				};
			} };
		})();
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	private static String page(String scheme) {
		var attrs = "data-jc-scheme=\"" + scheme + "\" data-jc-scheme-pin=\"" + scheme + "\"";
		return "<!DOCTYPE html>\n<html lang=\"en\" " + attrs + "><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n<link rel=\"stylesheet\" href=\"" + DARK_CSS + "\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + THEME_CSS + "\">\n<link rel=\"stylesheet\" href=\"" + VIEWS_CSS + "\">\n"
			+ "</head><body>\n" + BODY + PROBE + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("folder-tabs");
		for (var s : SCHEMES)
			f.page(s, ORIGIN + "/" + s, page(s));
		report = f.run();
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> probe(String scheme) {
		var r = report.get(scheme);
		assertEquals(List.of(), r.get("pageErrors"), () -> scheme + ": page errors");
		return ConsoleBrowserFixture.probe(r);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> tab(Map<String,Object> p, String id) {
		return (Map<String,Object>)((Map<String,Object>)p.get("tabs")).get(id);
	}

	@SuppressWarnings("unchecked")
	private static String tok(Map<String,Object> p, String n) {
		return String.valueOf(((Map<String,Object>)p.get("tok")).get(n));
	}

	private static double num(Map<String,Object> m, String k) {
		return ((Number)m.get(k)).doubleValue();
	}

	@Test void a01_selectedTabIsWashFilledWithAccentBarAndEdge() {
		for (var s : SCHEMES) {
			var p = probe(s);
			var t = tab(p, "t1");
			assertEquals(tok(p, "wash"), t.get("background-color"), s + ": selected fill");
			assertEquals("600", t.get("font-weight"), s);
			assertEquals("3px", t.get("border-top-width"), s);
			assertEquals(tok(p, "accent"), t.get("border-top-color"), s);
			assertEquals(tok(p, "accent"), t.get("border-left-color"), s);
			assertEquals("rgba(0, 0, 0, 0)", t.get("border-bottom-color"), s + ": selected tab has no visible bottom edge");
			assertEquals("none", t.get("box-shadow"), s + ": the bar is the top border, not an inset shadow");
		}
	}

	@Test void a02_unselectedTabsAreWhiteWithLightEdgeAndNeutralTopBorder() {
		for (var s : SCHEMES) {
			var p = probe(s);
			for (var id : List.of("t2", "t3")) {
				var t = tab(p, id);
				assertEquals(tok(p, "card"), t.get("background-color"), s + ": " + id + " fill");
				assertEquals(tok(p, "border"), t.get("border-left-color"), s + ": " + id + " edge");
				assertEquals("3px", t.get("border-top-width"), s + ": " + id + " reserves the bar");
				assertEquals(tok(p, "border"), t.get("border-top-color"), s + ": " + id + " has a neutral top border");
			}
		}
	}

	@Test void a06_selectedTabCoversThePanelTopBorder() {
		for (var s : SCHEMES) {
			@SuppressWarnings("unchecked") var c = (Map<String,Object>)probe(s).get("cover");
			assertTrue(num(c, "selBottom") >= num(c, "paneTop") + 1 - 0.01, s + ": selected tab overlaps the pane's 1px top border " + c);
			assertEquals("t1", c.get("hit"), s + ": the selected tab, not the pane border, paints at the pane's top edge");
		}
	}

	@Test void a03_labelsShareOneTopAndUnselectedHeightsMatch() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertEquals(num(tab(p, "t1"), "labelTop"), num(tab(p, "t2"), "labelTop"), 0.5, s + ": selected vs unselected label");
			assertEquals(num(tab(p, "t2"), "labelTop"), num(tab(p, "t3"), "labelTop"), 0.5, s);
			assertEquals(num(tab(p, "t2"), "height"), num(tab(p, "t3"), "height"), 0.5, s);
		}
	}

	@Test void a04_panelIsCardFilledAccentBorderedAndPadded() {
		for (var s : SCHEMES) {
			var p = probe(s);
			@SuppressWarnings("unchecked") var pane = (Map<String,Object>)p.get("pane");
			assertEquals(tok(p, "card"), pane.get("background-color"), s);
			assertEquals(tok(p, "accent"), pane.get("border-top-color"), s);
			assertEquals("12px", pane.get("padding-top"), s);
			assertEquals("12px", pane.get("padding-left"), s);
		}
	}

	@Test void a05_rowIsShorter() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertEquals("4px", tab(p, "t2").get("padding-top"), s + ": default tab padding");
			assertEquals("2px", tab(p, "c2").get("padding-top"), s + ": compact chart tab padding");
		}
	}
}
