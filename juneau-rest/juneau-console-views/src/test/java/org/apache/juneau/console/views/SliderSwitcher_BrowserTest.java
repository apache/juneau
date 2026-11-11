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
 * The slider switcher in Chromium, light and dark: no stray border on any button, an oval hover, an even inset for
 * the thumb, and a thumb shadow that does not bleed past the track in the dark scheme.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class SliderSwitcher_BrowserTest extends org.apache.juneau.TestBase {

	private static final List<String> SCHEMES = List.of("light", "dark");

	private static final String BODY = """
		<div class="juneau-switcher-selector" data-juneau-switcher-style="slider">
		<div class="juneau-view-ribbon-group" role="tablist" id="g">
		<span class="juneau-switcher-thumb" id="th" aria-hidden="true" style="width:60px"></span>
		<button type="button" class="juneau-view-ribbon-btn" role="tab" id="b1" aria-selected="true">One</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="b2" aria-selected="false">Two</button><button type="button" class="juneau-view-ribbon-btn" role="tab" id="b3" aria-selected="false">Three</button></div></div>
		<div class="juneau-switcher-selector" data-juneau-switcher-style="slider">
		<div class="juneau-view-ribbon-group" role="tablist"><button type="button" class="juneau-view-ribbon-btn" role="tab" id="o1" aria-selected="true">Only</button></div></div>
		<div id="park" style="height:40px"></div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			var PROPS = ['border-top-width', 'border-right-width', 'border-bottom-width', 'border-left-width', 'border-top-color',
				'border-top-left-radius', 'border-top-right-radius', 'border-bottom-left-radius', 'border-bottom-right-radius', 'background-color', 'box-shadow'];
			function snap(id) {
				var el = document.getElementById(id), cs = getComputedStyle(el), o = {};
				PROPS.forEach(function (p) { o[p] = cs.getPropertyValue(p); });
				var r = el.getBoundingClientRect(); o.top = r.top; o.bottom = r.bottom; o.height = r.height;
				return o;
			}
			var rec = {};
			window.__rec = function (key, id) { rec[key] = snap(id); };
			window.__probe = { toJSON: function () {
				var g = document.getElementById('g').getBoundingClientRect();
				return { rest: { b1: snap('b1'), b2: snap('b2'), b3: snap('b3'), o1: snap('o1'), th: snap('th') },
					hover: rec, group: { top: g.top, bottom: g.bottom } };
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
		var f = ConsoleBrowserFixture.create("slider-switcher");
		for (var s : SCHEMES) {
			f.page(s, ORIGIN + "/" + s, page(s));
			for (var id : List.of("b1", "b3", "o1"))
				f.hover("#" + id).eval("window.__rec('" + id + "', '" + id + "')");
			f.hover("#park");
		}
		report = f.run();
	}

	private static Map<String,Object> probe(String scheme) {
		var r = report.get(scheme);
		assertEquals(List.of(), r.get("pageErrors"), () -> scheme + ": page errors");
		return ConsoleBrowserFixture.probe(r);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> sub(Map<String,Object> p, String a, String b) {
		return (Map<String,Object>)((Map<String,Object>)p.get(a)).get(b);
	}

	@Test void a01_noButtonCarriesABorder() {
		for (var s : SCHEMES) {
			var p = probe(s);
			for (var id : List.of("b1", "b2", "b3", "o1"))
				for (var side : List.of("top", "right", "bottom", "left"))
					assertEquals("0px", sub(p, "rest", id).get("border-" + side + "-width"), s + ": " + id + " " + side);
		}
	}

	@Test void a02_hoverIsAnOvalWithNoBorder() {
		for (var s : SCHEMES) {
			var p = probe(s);
			for (var id : List.of("b1", "b3", "o1")) {
				var h = sub(p, "hover", id);
				assertNotEquals("rgba(0, 0, 0, 0)", h.get("background-color"), s + ": " + id + " hover wash");
				for (var c : List.of("top-left", "top-right", "bottom-left", "bottom-right"))
					assertTrue(Double.parseDouble(String.valueOf(h.get("border-" + c + "-radius")).replace("px", "")) >= 9, s + ": " + id + " " + c + " " + h.get("border-" + c + "-radius"));
				for (var side : List.of("top", "right", "bottom", "left"))
					assertEquals("0px", h.get("border-" + side + "-width"), s + ": " + id + " hover " + side);
			}
		}
	}

	@Test void a03_thumbIsInsetEvenlyTopAndBottom() {
		for (var s : SCHEMES) {
			var p = probe(s);
			@SuppressWarnings("unchecked") var g = (Map<String,Object>)p.get("group");
			var th = sub(p, "rest", "th");
			var top = ((Number)th.get("top")).doubleValue() - ((Number)g.get("top")).doubleValue();
			var bottom = ((Number)g.get("bottom")).doubleValue() - ((Number)th.get("bottom")).doubleValue();
			assertEquals(top, bottom, 0.5, s + ": thumb inset");
			assertTrue(top >= 2.5, s + ": thumb inset " + top);
		}
	}

	@Test void a04_darkThumbShadowDoesNotBleedPastTheTrack() {
		var light = String.valueOf(sub(probe("light"), "rest", "th").get("box-shadow"));
		var dark = String.valueOf(sub(probe("dark"), "rest", "th").get("box-shadow"));
		assertNotEquals("none", light);
		assertEquals("none", dark, "dark thumb shadow");
	}
}
