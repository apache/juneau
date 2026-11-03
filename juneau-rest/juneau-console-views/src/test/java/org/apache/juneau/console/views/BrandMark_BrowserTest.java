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
 * The masthead brand mark in a real browser: a text-only brand is inset from the viewport's left edge like a logo
 * brand, and a leading {@code ::before} mark on the title (an app's rotated-square leaf) paints fully inside the
 * title's overflow box - for each stock palette in both schemes.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class BrandMark_BrowserTest extends TestBase {

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");

	/** The app-side leaf: a 0.85em square rotated 45deg, as Foundry draws it (its box is smaller than its painted corner). */
	private static final String BODY = """
		<style>#txt .jc-brand-title::before { content: ""; display: inline-block; width: 0.85em; height: 0.85em;
			background-color: var(--jc-accent); border-radius: 0 70% 0 70%; transform: rotate(45deg); margin-right: 0.5rem; }</style>
		<div class="jc-header" id="txt"><div class="jc-brand" id="b1"><span class="jc-brand-title" id="t1">Text only</span></div></div>
		<div class="jc-header" id="logo"><a class="jc-brand" id="b2" href="/"><div class="jc-logo" id="l2" role="img" aria-label="Logo"></div><span class="jc-brand-title" id="t2">With logo</span></a></div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			function box(id) {
				var el = document.getElementById(id), cs = getComputedStyle(el), r = el.getBoundingClientRect();
				return { left: r.left, paddingLeft: parseFloat(cs.paddingLeft), borderLeft: parseFloat(cs.borderLeftWidth),
					overflowX: cs.overflowX, fontSize: parseFloat(cs.fontSize) };
			}
			window.__probe = { toJSON: function () {
				var q = getComputedStyle(document.getElementById('t1'), '::before');
				return { t1: box('t1'), t2: box('t2'), l2: box('l2'), leafWidth: parseFloat(q.width) };
			} };
		})();
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	private static String caseName(String palette, String scheme) {
		return palette + "-" + scheme;
	}

	private static String page(String palette, String scheme) {
		var chrome = "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n";
		var themes = "<link rel=\"stylesheet\" href=\"" + DARK_CSS + "\">\n<link rel=\"stylesheet\" href=\"/juneau-console/juneau-theme-" + palette + ".css\">\n";
		var attrs = "data-jc-scheme=\"" + scheme + "\" data-jc-scheme-pin=\"" + scheme + "\"";
		return "<!DOCTYPE html>\n<html lang=\"en\" " + attrs + "><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n"
			+ chrome + themes + "</head><body style=\"margin:0\">\n" + BODY + PROBE + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("brand-mark");
		for (var p : PALETTES)
			f.asset("/juneau-console/juneau-theme-" + p + ".css", resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");
		for (var p : PALETTES)
			for (var s : SCHEMES)
				f.page(caseName(p, s), ORIGIN + "/" + caseName(p, s), page(p, s));
		report = f.run();
	}

	private static double num(Map<String,Object> m, String k) {
		return ((Number)m.get(k)).doubleValue();
	}

	@Test void a01_textOnlyBrandIsInsetFromTheViewportEdge() {
		for (var p : PALETTES)
			for (var s : SCHEMES) {
				var name = caseName(p, s);
				var r = report.get(name);
				assertNotNull(r, () -> "no report for " + name);
				assertEquals(List.of(), r.get("pageErrors"), () -> name + ": page errors");
				var t1 = (Map<String,Object>)probe(r).get("t1");
				assertTrue(num(t1, "left") >= 16, () -> name + ": text-only brand title starts at " + num(t1, "left") + "px, expected the logo's 16px inset");
			}
	}

	@Test void a02_leadingBeforeMarkPaintsInsideTheTitleBox() {
		for (var p : PALETTES)
			for (var s : SCHEMES) {
				var name = caseName(p, s);
				var pr = probe(report.get(name));
				var t1 = (Map<String,Object>)pr.get("t1");
				// A square rotated 45deg overhangs its layout box by (sqrt(2) - 1) / 2 of its side on each side.
				var overhang = (Math.sqrt(2) - 1) / 2 * num(pr, "leafWidth");
				assertTrue(num(t1, "paddingLeft") + num(t1, "borderLeft") >= overhang, () -> name + ": title start padding " + num(t1, "paddingLeft")
					+ "px is less than the mark's " + overhang + "px overhang, so overflow:hidden clips it");
				assertEquals("hidden", t1.get("overflowX"), () -> name + ": the title still clips its ellipsis");
			}
	}

	@Test void a03_logoBrandIsUnchanged() {
		for (var p : PALETTES)
			for (var s : SCHEMES) {
				var name = caseName(p, s);
				var pr = probe(report.get(name));
				assertEquals(16.0, num((Map<String,Object>)pr.get("l2"), "left"), () -> name + ": logo inset");
				assertEquals(0.0, num((Map<String,Object>)pr.get("t2"), "paddingLeft"), () -> name + ": a title after a logo takes no extra start padding");
			}
	}
}
