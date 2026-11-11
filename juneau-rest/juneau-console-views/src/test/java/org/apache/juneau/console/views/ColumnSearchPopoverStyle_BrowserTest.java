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
 * The column-search popover frame in Chromium: a thin bordered, rounded, shadowed card; a filled Apply with plain
 * Cancel and Clear at the input's height; and the help mark at the bottom right of the footer.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class ColumnSearchPopoverStyle_BrowserTest extends org.apache.juneau.TestBase {

	private static final List<String> SCHEMES = List.of("light", "dark");

	private static final String BODY = """
		<div class="juneau-view-col-search-popover" id="pop" style="position:absolute;left:40px;top:40px">
		<div class="juneau-view-col-search-popover-title" id="ttl">Status</div>
		<input type="text" class="juneau-view-col-search-popover-input" id="inp" value="x">
		<div class="juneau-view-col-search-popover-footer" id="ftr">
		<button type="button" class="juneau-view-col-search-popover-apply jc-btn jc-btn-primary" id="apply">Apply</button>
		<button type="button" class="juneau-view-col-search-popover-cancel jc-btn" id="cancel">Cancel</button>
		<button type="button" class="juneau-view-col-search-popover-clear jc-btn" id="clear">Clear</button>
		<button type="button" class="juneau-view-col-search-popover-more" id="more">?</button>
		</div></div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			function cs(id) { return getComputedStyle(document.getElementById(id)); }
			function r(id) { return document.getElementById(id).getBoundingClientRect(); }
			window.__probe = { toJSON: function () {
				var p = cs('pop');
				var pr = r('pop'), mr = r('more'), fr = r('ftr');
				return { border: p.borderTopWidth, borderStyle: p.borderTopStyle, radius: parseFloat(p.borderTopLeftRadius), shadow: p.boxShadow,
					applyBg: cs('apply').backgroundColor, cancelBg: cs('cancel').backgroundColor, clearBg: cs('clear').backgroundColor,
					inputH: r('inp').height, applyH: r('apply').height, cancelH: r('cancel').height, clearH: r('clear').height,
					moreRight: pr.right - mr.right, moreBottom: fr.bottom - mr.bottom, moreIsRight: mr.left > r('clear').right,
					titleColor: cs('ttl').color, accent: (function () { var t = document.createElement('i'); t.style.color = 'var(--jc-accent)'; document.body.appendChild(t); return getComputedStyle(t).color; })() };
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
		var f = ConsoleBrowserFixture.create("column-search-popover-style");
		for (var s : SCHEMES)
			f.page(s, ORIGIN + "/" + s, page(s));
		report = f.run();
	}

	private static Map<String,Object> probe(String scheme) {
		var r = report.get(scheme);
		assertEquals(List.of(), r.get("pageErrors"), () -> scheme + ": page errors");
		return ConsoleBrowserFixture.probe(r);
	}

	private static double num(Map<String,Object> p, String k) {
		return ((Number)p.get(k)).doubleValue();
	}

	@Test void a01_frameIsAThinRoundedShadowedCard() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertEquals("1px", p.get("border"), s);
			assertEquals("solid", p.get("borderStyle"), s);
			assertTrue(num(p, "radius") >= 4, s + ": radius " + p.get("radius"));
			assertNotEquals("none", p.get("shadow"), s + ": shadow");
		}
	}

	@Test void b01_applyIsFilledAndTheOthersAreNot() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertNotEquals(p.get("cancelBg"), p.get("applyBg"), s + ": apply vs cancel");
			assertEquals(p.get("cancelBg"), p.get("clearBg"), s + ": cancel vs clear");
		}
	}

	@Test void b02_buttonsMatchTheInputHeight() {
		for (var s : SCHEMES) {
			var p = probe(s);
			for (var k : List.of("applyH", "cancelH", "clearH"))
				assertEquals(num(p, "inputH"), num(p, k), 1.0, s + ": " + k);
		}
	}

	@Test void c01_helpMarkSitsAtTheBottomRight() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertEquals(Boolean.TRUE, p.get("moreIsRight"), s);
			assertTrue(num(p, "moreRight") <= 16, s + ": gap to the right edge " + p.get("moreRight"));
			assertTrue(num(p, "moreBottom") <= 4, s + ": gap to the footer bottom " + p.get("moreBottom"));
		}
	}

	@Test void d01_titleUsesBodyText() {
		for (var s : SCHEMES)
			assertNotEquals(probe(s).get("accent"), probe(s).get("titleColor"), s);
	}
}
