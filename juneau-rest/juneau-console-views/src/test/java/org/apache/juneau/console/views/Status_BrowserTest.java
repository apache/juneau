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
 * The status dot in a real browser (spec section 6): its colour per state for each stock palette in both schemes, the
 * dashed ring that marks paused, and that the dot shows no text.
 *
 * <p>
 * The markup is the DOM {@code wireTablePolling} (juneau-views.js) builds in the {@code status} group, written out
 * statically with one dot per state.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class Status_BrowserTest extends TestBase {

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");
	private static final List<String> STATES = List.of("idle", "refreshing", "error", "paused");

	private static final String BODY = """
		<main class="jc-main"><div class="jc-card"><div class="juneau-view-toolbar-right">
		<span class="juneau-view-ribbon-group" data-juneau-group="status">
		<span class="juneau-view-staleness" id="idle" role="img" data-state="idle" data-jc-tip="Updated just now"></span>
		<span class="juneau-view-staleness" id="refreshing" role="img" data-state="refreshing" data-jc-tip="Refreshing"></span>
		<span class="juneau-view-staleness" id="error" role="img" data-state="error" data-jc-tip="Refresh failed"></span>
		<span class="juneau-view-staleness" id="paused" role="img" data-state="paused" data-jc-tip="Paused"></span>
		</span></div></div></main>
		<div id="tokprobe" style="display:none"></div>
		""";

	private static final String PROBE = """
		<script>
		(function () {
			window.__probe = { toJSON: function () {
				var t = document.getElementById('tokprobe');
				var tok = {};
				['--jc-text-muted', '--jc-success', '--jc-danger-text'].forEach(function (n) {
					t.style.color = 'var(' + n + ')'; tok[n] = getComputedStyle(t).color;
				});
				var dots = {};
				['idle', 'refreshing', 'error', 'paused'].forEach(function (id) {
					var el = document.getElementById(id), cs = getComputedStyle(el), r = el.getBoundingClientRect();
					dots[id] = { color: cs.color, bg: cs.backgroundColor, borderStyle: cs.borderTopStyle,
						opacity: cs.opacity, text: el.textContent, width: r.width, height: r.height };
				});
				return { tok: tok, dots: dots };
			} };
		})();
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	private static String caseName(String palette, String scheme) {
		return palette + "-" + scheme;
	}

	private static String page(String palette, String scheme) {
		var attrs = "data-jc-scheme=\"" + scheme + "\" data-jc-scheme-pin=\"" + scheme + "\"";
		return "<!DOCTYPE html>\n<html lang=\"en\" " + attrs + "><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n<link rel=\"stylesheet\" href=\"" + DARK_CSS + "\">\n"
			+ "<link rel=\"stylesheet\" href=\"/juneau-console/juneau-theme-" + palette + ".css\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + VIEWS_CSS + "\">\n"
			+ "</head><body>\n" + BODY + PROBE + "</body></html>\n";
	}

	@BeforeAll
	static void run() throws Exception {
		var f = ConsoleBrowserFixture.create("status");
		for (var p : PALETTES)
			f.asset("/juneau-console/juneau-theme-" + p + ".css", resource("/org/apache/juneau/console/juneau-theme-" + p + ".css"), "text/css");
		for (var p : PALETTES)
			for (var s : SCHEMES)
				f.page(caseName(p, s), ORIGIN + "/" + caseName(p, s), page(p, s));
		report = f.run();
	}

	private static void eachCase(java.util.function.BiConsumer<String,Map<String,Object>> check) {
		for (var p : PALETTES)
			for (var s : SCHEMES) {
				var name = caseName(p, s);
				var r = report.get(name);
				assertNotNull(r, () -> "no report for " + name);
				assertEquals(List.of(), r.get("pageErrors"), () -> name + ": page errors");
				check.accept(name, probe(r));
			}
	}

	private static String dot(Map<String,Object> pr, String state, String prop) {
		return String.valueOf(((Map<String,Map<String,Object>>)pr.get("dots")).get(state).get(prop));
	}

	private static String tok(Map<String,Object> pr, String n) {
		return ((Map<String,String>)pr.get("tok")).get(n);
	}

	@Test void a01_dotColourPerState() {
		eachCase((name, pr) -> {
			assertEquals(tok(pr, "--jc-text-muted"), dot(pr, "idle", "color"), name + ": idle is muted");
			assertEquals(tok(pr, "--jc-success"), dot(pr, "refreshing", "color"), name + ": refreshing is success");
			assertEquals(tok(pr, "--jc-danger-text"), dot(pr, "error", "color"), name + ": error is danger-text");
			assertEquals(tok(pr, "--jc-text-muted"), dot(pr, "paused", "color"), name + ": paused is muted");
			assertEquals(tok(pr, "--jc-success"), dot(pr, "refreshing", "bg"), name + ": refreshing fills with its colour");
		});
	}

	@Test void a02_statesAreDistinguishable() {
		eachCase((name, pr) -> {
			assertNotEquals(dot(pr, "idle", "color"), dot(pr, "refreshing", "color"), name);
			assertNotEquals(dot(pr, "idle", "color"), dot(pr, "error", "color"), name);
			assertNotEquals(dot(pr, "refreshing", "color"), dot(pr, "error", "color"), name);
		});
	}

	@Test void a03_pausedIsADashedRingNotAFilledDot() {
		eachCase((name, pr) -> {
			assertEquals("dashed", dot(pr, "paused", "borderStyle"), name);
			assertEquals("rgba(0, 0, 0, 0)", dot(pr, "paused", "bg"), name + ": paused is hollow");
			assertEquals("0.7", dot(pr, "paused", "opacity"), name);
			for (var s : List.of("idle", "refreshing", "error"))
				assertEquals("none", dot(pr, s, "borderStyle"), name + ": " + s + " has no ring");
		});
	}

	@Test void a04_dotIsSmallAndShowsNoText() {
		eachCase((name, pr) -> {
			for (var s : STATES) {
				assertEquals("", dot(pr, s, "text"), name + ": " + s + " has no visible text");
				assertTrue(Double.parseDouble(dot(pr, s, "width")) < 20, name + ": " + s + " width " + dot(pr, s, "width"));
				assertEquals(dot(pr, s, "width"), dot(pr, s, "height"), name + ": " + s + " is round");
			}
		});
	}
}
