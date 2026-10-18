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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Meter and segmented quick-stat tiles painted in a real page: roles and values, track and fill geometry, the dark
 * layer's track colour, sizing that survives a strict {@code style-src} policy, and forced-colors fills.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class QuickStats_BrowserTest extends TestBase {

	private static final String PAGE = """
		{"contractVersion":"1","title":"Stats","cards":[
		 {"id":"qs","type":"datatables","table":{"rows":[{"id":1}],"columns":[{"key":"id","label":"Id"}],
		  "quickStats":{"id":"s","items":[
		   {"id":"t1","label":"Open","value":3},
		   {"id":"m1","type":"meter","label":"Inventory","value":12,"max":40,"tone":"success"},
		   {"id":"m2","type":"meter","label":"Quota","value":52,"max":40,"tone":"error"},
		   {"id":"m3","type":"meter","label":"Load","value":20,"max":40,"tone":"warning"},
		   {"id":"g1","type":"segmented","label":"Orders","segments":[
		    {"label":"Placed","count":3},{"label":"Approved","count":5,"tone":"info"},{"label":"Delivered","count":9,"tone":"success"}]},
		   {"id":"g2","type":"segmented","label":"Jobs","segments":[
		    {"label":"Late","count":2,"tone":"warning"},{"label":"Done","count":3,"tone":"success"}]}
		  ]}}}]}
		""";

	/** Writes the measurements to {@code window.__probe}; console-shell.cjs copies it into the report. */
	private static final String PROBE = """
		(function () {
		  const cs = function (el) { return getComputedStyle(el); };
		  const m1 = document.querySelector('[data-juneau-stat="m1"]');
		  const track = m1.querySelector('.jc-stat-track');
		  const fill = m1.querySelector('.jc-stat-fill');
		  const g1 = document.querySelector('[data-juneau-stat="g1"]');
		  const segs = Array.from(g1.querySelectorAll('.jc-stat-segfill'));
		  const tokenProbe = document.createElement('span');
		  tokenProbe.style.background = 'var(--jc-progress-track)';
		  document.body.appendChild(tokenProbe);
		  const canvasBg = document.createElement('span');
		  canvasBg.style.background = 'Canvas';
		  document.body.appendChild(canvasBg);
		  const m2 = document.querySelector('[data-juneau-stat="m2"]');
		  const warnFill = document.querySelector('[data-juneau-stat="m3"] .jc-stat-fill');
		  const warnSeg = document.querySelector('[data-juneau-stat="g2"] .jc-stat-segfill');
		  const canvasText = document.createElement('span');
		  canvasText.style.color = 'CanvasText';
		  document.body.appendChild(canvasText);
		  const control = document.createElement('span');
		  control.setAttribute('style', 'display:inline-block;width:37px');
		  document.body.appendChild(control);
		  window.__probe = {
		    role: m1.getAttribute('role'), now: m1.getAttribute('aria-valuenow'), text: m1.getAttribute('aria-valuetext'),
		    groupRole: g1.getAttribute('role'),
		    trackBg: cs(track).backgroundColor, tokenBg: cs(tokenProbe).backgroundColor,
		    trackWidth: track.getBoundingClientRect().width,
		    fillRatio: fill.getBoundingClientRect().width / track.getBoundingClientRect().width,
		    fillBg: cs(fill).backgroundColor,
		    segBgs: segs.map(function (s) { return cs(s).backgroundColor; }),
		    segbarBg: cs(g1.querySelector('.jc-stat-segbar')).backgroundColor,
		    segWidths: segs.map(function (s) { return s.getBoundingClientRect().width; }),
		    stripe: cs(m2.querySelector('.jc-stat-fill')).backgroundImage,
		    warnStripe: cs(warnFill).backgroundImage, warnBg: cs(warnFill).backgroundColor,
		    okStripe: cs(fill).backgroundImage,
		    segStripe: cs(warnSeg).backgroundImage, okSegStripe: cs(segs[0]).backgroundImage,
		    overShadow: cs(m2.querySelector('.jc-stat-fill')).boxShadow,
		    errOutline: cs(m2.querySelector('.jc-stat-track')).outlineStyle,
		    okOutline: cs(track).outlineStyle,
		    swatchBgs: Array.from(g1.querySelectorAll('.jc-stat-swatch')).map(function (s) { return cs(s).backgroundColor; }),
		    canvasBg: cs(canvasBg).backgroundColor,
		    adjust: cs(fill).forcedColorAdjust,
		    canvasText: cs(canvasText).color,
		    trackBorder: cs(track).borderTopWidth,
		    controlWidth: control.getBoundingClientRect().width
		  };
		})()
		""";

	private static final String CSP_META = "<meta http-equiv=\"Content-Security-Policy\" content=\"style-src 'self'\">";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void run() throws Exception {
		var views = viewsPack();
		var light = contractPage(PAGE, "", "", views);
		var dark = contractPage(PAGE, "", "", views, "", true);
		var csp = light.replace("<meta charset=\"utf-8\">", "<meta charset=\"utf-8\">" + CSP_META);
		assertNotEquals(light, csp, "the CSP meta tag must be injected");
		var ready = "[data-juneau-stat=\"g1\"]";
		report = ConsoleBrowserFixture.create("quickstats")
			.page("light", ORIGIN + "/light", light).waitFor(ready).colorScheme("light").eval(PROBE)
			.page("dark", ORIGIN + "/dark", dark).waitFor(ready).colorScheme("dark").eval(PROBE)
			.page("csp", ORIGIN + "/csp", csp).waitFor(ready).colorScheme("light").eval(PROBE)
			.page("forced", ORIGIN + "/forced", light).waitFor(ready).colorScheme("light").forcedColors("active").eval(PROBE)
			.run();
	}

	private static Map<String,Object> p(String name) {
		return probe(report.get(name));
	}

	private static double num(Object o) {
		return ((Number)o).doubleValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Object> list(Object o) {
		return (List<Object>)o;
	}

	private static void assertFillRatio(Map<String,Object> p) {
		var tolerance = 1.0 / num(p.get("trackWidth"));
		assertEquals(0.30, num(p.get("fillRatio")), tolerance + 0.005, p::toString);
	}

	private static void assertOrderedWidths(Map<String,Object> p) {
		var w = list(p.get("segWidths"));
		assertEquals(3, w.size(), p::toString);
		assertTrue(num(w.get(0)) < num(w.get(1)) && num(w.get(1)) < num(w.get(2)), () -> "3:5:9 widths: " + w);
	}

	@Test void a01_light_rolesAndValues() {
		var p = p("light");
		assertEquals("meter", p.get("role"));
		assertEquals("12", p.get("now"));
		assertEquals("12 / 40", p.get("text"));
		assertEquals("group", p.get("groupRole"));
	}

	@Test void a02_light_paint() {
		var p = p("light");
		assertEquals(p.get("tokenBg"), p.get("trackBg"), p::toString);
		assertEquals(p.get("tokenBg"), p.get("segbarBg"), p::toString);
		assertFillRatio(p);
		var bgs = list(p.get("segBgs"));
		assertEquals(3, bgs.size());
		for (var bg : bgs) {
			assertNotEquals("rgba(0, 0, 0, 0)", bg, p::toString);
			assertNotEquals(p.get("segbarBg"), bg, p::toString);
		}
		assertEquals(3, new HashSet<>(bgs).size(), () -> "segment colours must be pairwise distinct: " + bgs);
		assertOrderedWidths(p);
		assertTrue(String.valueOf(p.get("stripe")).contains("repeating-linear-gradient"), p::toString);
		assertTrue(String.valueOf(p.get("warnStripe")).contains("repeating-linear-gradient"), p::toString);
		assertTrue(String.valueOf(p.get("segStripe")).contains("repeating-linear-gradient"), p::toString);
		assertEquals("none", p.get("okStripe"), p::toString);
		assertEquals("none", p.get("okSegStripe"), p::toString);
		assertNotEquals("none", p.get("overShadow"), () -> "the over-limit cue must paint: " + p);
		assertEquals("solid", p.get("errOutline"), p::toString);
		assertEquals("none", p.get("okOutline"), p::toString);
		var swatches = list(p.get("swatchBgs"));
		assertEquals(3, swatches.size(), p::toString);
		for (var bg : swatches)
			assertNotEquals("rgba(0, 0, 0, 0)", bg, () -> "swatch must be filled: " + p);
	}

	@Test void a03_dark_paint() {
		var p = p("dark");
		var l = p("light");
		assertEquals(p.get("tokenBg"), p.get("trackBg"), p::toString);
		assertEquals(p.get("tokenBg"), p.get("segbarBg"), p::toString);
		assertNotEquals(l.get("trackBg"), p.get("trackBg"), () -> "the dark layer must be live; light=" + l.get("trackBg") + " dark=" + p.get("trackBg"));
		assertNotEquals("rgba(0, 0, 0, 0)", p.get("fillBg"), p::toString);
		assertNotEquals(p.get("trackBg"), p.get("fillBg"), p::toString);
		for (var bg : list(p.get("segBgs"))) {
			assertNotEquals("rgba(0, 0, 0, 0)", bg, p::toString);
			assertNotEquals(p.get("segbarBg"), bg, p::toString);
		}
	}

	@Test void a04_strictCsp_cssomStillSizes() {
		var p = p("csp");
		assertNotEquals(37.0, num(p.get("controlWidth")), () -> "the style-src policy must be active: " + p);
		assertFillRatio(p);
		assertOrderedWidths(p);
	}

	@Test void a05_forcedColors() {
		var p = p("forced");
		assertEquals(p.get("canvasText"), p.get("fillBg"), p::toString);
		for (var bg : list(p.get("segBgs")))
			assertEquals(p.get("canvasText"), bg, p::toString);
		assertNotEquals("0px", p.get("trackBorder"), p::toString);
		assertEquals(p.get("canvasBg"), p.get("trackBg"), () -> "the track must be on Canvas: " + p);
		assertEquals(p.get("canvasBg"), p.get("segbarBg"), () -> "the segment bar must be on Canvas: " + p);
		for (var bg : list(p.get("swatchBgs")))
			assertEquals(p.get("canvasText"), bg, p::toString);
		assertEquals("none", p.get("adjust"), p::toString);
		assertNotEquals("none", p.get("overShadow"), () -> "the over-limit cue must survive forced colours: " + p);
		// Warning and error keep a stripe in system colours, so they differ from a plain success fill.
		for (var key : List.of("warnStripe", "stripe", "segStripe")) {
			var stripe = String.valueOf(p.get(key));
			assertTrue(stripe.contains("repeating-linear-gradient"), () -> key + ": " + p);
			assertTrue(stripe.contains(String.valueOf(p.get("canvasBg"))), () -> key + " must be drawn in the Canvas system colour: " + p);
		}
		assertEquals("none", p.get("okStripe"), p::toString);
		assertNotEquals(p.get("okStripe"), p.get("warnStripe"), p::toString);
		assertEquals("none", p.get("okSegStripe"), p::toString);
	}

	@Test void a06_noShellErrors() {
		assertNoShellErrors(report.get("light"));
		assertNoShellErrors(report.get("dark"));
		assertNoShellErrors(report.get("csp"));
		assertNoShellErrors(report.get("forced"));
	}
}
