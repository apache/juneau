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
 * The header theme selector: it renders only when the page opts in, switches the palette link, persists the pick
 * across a reload, leaves the dark scheme layer in place, and clears the pick through the "Page default" entry.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps and Lists.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
class ThemeSelector_BrowserTest extends TestBase {

	private static final String KEY = "juneau-console:theme";
	private static final String ON = "data-jc-theme-selector data-jc-themes=\"open red gray\"";
	private static final String SIMPLE = """
		{"contractVersion":"1","title":"S","header":{"title":"T"},"cards":[]}
		""";

	private static final String EVENTS = """
		<script>
		window.__events = [];
		document.addEventListener('juneau:theme-changed', function (e) { window.__events.push(e.detail); });
		</script>
		""";

	private static final String PROBE = """
		<script>
		window.__probe = { toJSON: function () {
			var h = document.documentElement, s = document.querySelector('.jc-theme-select select'), l = document.querySelector('link[data-jc-theme-link]'), out = {};
			out.hasControl = !!s;
			out.value = s ? s.value : null;
			out.options = s ? Array.prototype.map.call(s.options, function (o) { return o.value; }) : null;
			out.href = l ? l.getAttribute('href') : null;
			out.scheme = h.getAttribute('data-jc-scheme');
			out.dark = !!document.querySelector('link[href*="juneau-theme-dark.css"]');
			out.events = window.__events.slice();
			try { out.stored = localStorage.getItem('juneau-console:theme'); } catch (x) { out.stored = 'ERR'; }
			return out;
		} };
		</script>
		""";

	/** Picks a value and resolves once the palette sheet has loaded and the change was announced (or after a second). */
	private static final String PICK = "new Promise(function(r){document.addEventListener('juneau:theme-changed',function(){r()},{once:true});setTimeout(r,3500);"
		+ "var s=document.querySelector('.jc-theme-select select');s.value='%s';s.dispatchEvent(new Event('change',{bubbles:true}))})";

	private static Map<String,Map<String,Object>> report;

	private static String page(String attrs) {
		var html = contractPage(SIMPLE, "", EVENTS, PROBE, attrs, true);
		return html.replace("href=\"" + THEME_CSS + "\"", "href=\"" + THEME_CSS + "\"" + (attrs.contains("theme-selector") ? " data-jc-theme-link" : ""));
	}

	@BeforeAll
	static void runPages() throws Exception {
		var fx = ConsoleBrowserFixture.create("theme-selector");
		for (var n : List.of("red", "gray"))
			fx.asset("/juneau-console/juneau-theme-" + n + ".css", "html:root{}", "text/css");
		report = fx
			.page("off", ORIGIN + "/off", page(""))
			.page("on", ORIGIN + "/on", page(ON))
			.page("pick", ORIGIN + "/pick", page(ON)).eval(PICK.formatted("red"))
			.page("stored", ORIGIN + "/stored", page(ON)).storage(Map.of(KEY, "gray"))
			.page("garbage", ORIGIN + "/garbage", page(ON)).storage(Map.of(KEY, "nope"))
			.page("clear", ORIGIN + "/clear", page(ON)).storage(Map.of(KEY, "gray")).eval(PICK.formatted(""))
			.page("darkPick", ORIGIN + "/darkPick", page(ON + " data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\"")).eval(PICK.formatted("red"))
			.run();
	}

	private static Map<String,Object> p(String name) {
		var r = report.get(name);
		assertNoShellErrors(r);
		return probe(r);
	}

	@Test void a01_flagOffMeansNoControl() {
		assertEquals(false, p("off").get("hasControl"));
	}

	@Test void a02_controlListsPageDefaultThenThemes() {
		var pr = p("on");
		assertEquals(true, pr.get("hasControl"));
		assertEquals(List.of("", "open", "red", "gray"), pr.get("options"));
		assertEquals("", pr.get("value"));
		assertEquals(THEME_CSS, pr.get("href"));
	}

	@Test void a03_pickSwitchesPaletteAndStores() {
		var pr = p("pick");
		assertEquals("/juneau-console/juneau-theme-red.css", pr.get("href"));
		assertEquals("red", pr.get("stored"));
		assertEquals("red", pr.get("value"));
		assertEquals(List.of(Map.of("scheme", "light", "theme", "red")), pr.get("events"));
	}

	@Test void a04_storedPickAppliesOnLoad() {
		var pr = p("stored");
		assertEquals("/juneau-console/juneau-theme-gray.css", pr.get("href"));
		assertEquals("gray", pr.get("value"));
	}

	@Test void a05_unknownStoredPickIgnored() {
		var pr = p("garbage");
		assertEquals(THEME_CSS, pr.get("href"));
		assertEquals("", pr.get("value"));
	}

	@Test void a06_pageDefaultClearsThePick() {
		var pr = p("clear");
		assertEquals(THEME_CSS, pr.get("href"));
		assertNull(pr.get("stored"));
		assertEquals("", pr.get("value"));
	}

	@Test void a07_darkOverlayStillApplies() {
		var pr = p("darkPick");
		assertEquals("dark", pr.get("scheme"));
		assertEquals(true, pr.get("dark"));
		assertEquals("/juneau-console/juneau-theme-red.css", pr.get("href"));
	}
}
