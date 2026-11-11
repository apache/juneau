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
 * Table chrome in Chromium: slimmer body and header rows (nested tables included), a paging pill whose text is the
 * lighter neutral, and a selection line that sits in its own row above the toolbar so the toolbar never shifts sideways.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class TableChrome_BrowserTest extends org.apache.juneau.TestBase {

	private static final List<String> SCHEMES = List.of("light", "dark");

	private static final String BODY = """
		<div id="host" class="dt-container">
		<div id="bulk" class="juneau-view-bulk-toolbar" hidden><span class="juneau-view-bulk-count">3 selected</span></div>
		<div class="juneau-view-toolbar-row">
		<div class="juneau-view-toolbar-left"><div class="juneau-view-pagingpill">
		<button type="button" class="juneau-view-pagingpill-btn" id="pgn">N</button>
		<span class="juneau-view-pagingpill-menuwrap"><button type="button" class="juneau-view-pagingpill-menubtn" id="pgm">1-25 of 300</button></span>
		</div></div>
		<div class="juneau-view-toolbar-right"><div class="juneau-view-ribbon"><span class="juneau-view-ribbon-group">
		<button type="button" class="juneau-view-ribbon-btn" id="rb">X</button></span></div></div>
		</div>
		<table data-juneau-view="a" class="dataTable" id="ta"><thead><tr><th id="ah">Name</th></tr></thead><tbody><tr><td id="ac">alpha</td></tr></tbody></table>
		<table data-juneau-view="b" class="dataTable juneau-view-density-comfortable" id="tb"><thead><tr><th>Name</th></tr></thead><tbody><tr><td id="bc">alpha</td></tr></tbody></table>
		<table class="juneau-view-table dataTable" id="tn"><thead><tr><th>Name</th></tr></thead><tbody><tr><td id="nc">alpha</td></tr></tbody></table>
		</div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			function h(id) { return document.getElementById(id).getBoundingClientRect().height; }
			function left(id) { return document.getElementById(id).getBoundingClientRect().left; }
			window.__probe = { toJSON: function () {
				var bulk = document.getElementById('bulk');
				var out = { body: h('ac'), head: h('ah'), comfy: h('bc'), nested: h('nc'),
					pgColor: getComputedStyle(document.getElementById('pgm')).color,
					mutedColor: (function () { var t = document.createElement('i'); t.style.color = 'var(--jc-text-muted)'; document.body.appendChild(t); return getComputedStyle(t).color; })(),
					bulkBeforeToolbar: !!(bulk.compareDocumentPosition(document.querySelector('.juneau-view-toolbar-row')) & Node.DOCUMENT_POSITION_FOLLOWING),
					bulkHiddenDisplay: getComputedStyle(bulk).display,
					leftEmpty: [left('pgn'), left('rb')] };
				bulk.hidden = false;
				out.bulkShownDisplay = getComputedStyle(bulk).display;
				out.leftSelected = [left('pgn'), left('rb')];
				var b = bulk.getBoundingClientRect(), t = document.querySelector('.juneau-view-toolbar-row').getBoundingClientRect();
				out.bulkAbove = b.bottom <= t.top + 0.5;
				return out;
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
		var f = ConsoleBrowserFixture.create("table-chrome");
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

	@Test void a01_bodyRowsAreSlimmerThanTheOldCeiling() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertTrue(num(p, "body") <= 24, s + ": body row " + p.get("body"));
			assertTrue(num(p, "nested") <= 24, s + ": nested row " + p.get("nested"));
		}
	}

	@Test void a02_headerRowsAreSlimmerThanTheOldCeiling() {
		for (var s : SCHEMES)
			assertTrue(num(probe(s), "head") <= 24, s + ": head row " + probe(s).get("head"));
	}

	@Test void a03_comfortableDensityFollowsTheNewBase() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertTrue(num(p, "comfy") > num(p, "body") + 3, s + ": comfortable " + p.get("comfy") + " vs " + p.get("body"));
			assertTrue(num(p, "comfy") <= 31, s + ": comfortable " + p.get("comfy"));
		}
	}

	@Test void b01_pagingTextIsTheLighterNeutral() {
		for (var s : SCHEMES)
			assertEquals(probe(s).get("mutedColor"), probe(s).get("pgColor"), s + ": paging text colour");
	}

	@Test void c01_selectionLineSitsAboveTheToolbarAndNeverShiftsIt() {
		for (var s : SCHEMES) {
			var p = probe(s);
			assertEquals(Boolean.TRUE, p.get("bulkBeforeToolbar"), s);
			assertEquals("none", p.get("bulkHiddenDisplay"), s + ": hidden when empty");
			assertEquals(p.get("leftEmpty"), p.get("leftSelected"), s + ": toolbar controls moved sideways");
			assertEquals(Boolean.TRUE, p.get("bulkAbove"), s + ": selection line is not above the toolbar row");
		}
	}
}
