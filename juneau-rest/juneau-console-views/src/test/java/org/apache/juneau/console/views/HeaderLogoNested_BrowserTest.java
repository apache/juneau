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
 * A header logo drawn from a built-in symbol on a nested route: the sprite is fetched from the directory the views
 * toolkit loads from, so a nested route resolves correctly whenever {@code juneau-icons.js} is on the page.  A page
 * without that script has no known sprite location: the logo is not drawn, nothing is requested from the nested
 * directory, and one console warning names the missing script.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class HeaderLogoNested_BrowserTest extends org.apache.juneau.TestBase {

	private static final String SPRITE = "/views/juneau-symbols-lucide.svg";
	private static final String CONTRACT = "{\"contractVersion\":\"1\",\"title\":\"T\",\"header\":{\"title\":\"Pets\",\"logo\":{\"symbol\":\"paw_print\"}},\"cards\":[]}";

	/** Counts console warnings that name the missing script, and reports whether a logo symbol was drawn. */
	private static final String PROBE = """
		<script>
		window.__warned = 0;
		(function () { var w = console.warn; console.warn = function () { if (/juneau-icons\\.js/.test(Array.prototype.join.call(arguments, " "))) window.__warned++; return w.apply(console, arguments); }; })();
		window.__probe = { toJSON: function () { var m = document.querySelector(".jc-logo");
			return { hasLogo: !!document.querySelector(".jc-logo-symbol"), warnings: window.__warned,
				marks: document.querySelectorAll(".jc-logo").length, cls: m ? m.className : null,
				bg: m ? getComputedStyle(m).backgroundImage : null, svg: !!(m && m.querySelector("svg")) }; } };
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void run() throws Exception {
		var sprite = "<svg xmlns=\"http://www.w3.org/2000/svg\"><symbol id=\"juneau-sym-paw_print\" viewBox=\"0 0 24 24\"><circle cx=\"12\" cy=\"12\" r=\"5\"/></symbol></svg>";
		var f = ConsoleBrowserFixture.create("header-logo-nested")
			.asset(SPRITE, sprite, "image/svg+xml")
			.asset("/views/juneau-icons.js", "/* stand-in: the shell only reads this script's src */", "text/javascript");
		// The icons script is named by an absolute URL (what the server renders) and by a relative one.
		f.page("absolute", ORIGIN + "/a/b/c", contractPage(CONTRACT, "", PROBE + "<script src=\"/views/juneau-icons.js?v=1\"></script>", "")).watchRequests();
		f.page("relative", ORIGIN + "/a/b/c", contractPage(CONTRACT, "", PROBE + "<script src=\"../../views/juneau-icons.js\"></script>", "")).watchRequests();
		// The icons script loads after the shell, as it does on a server-rendered page that lists it last.
		f.page("late", ORIGIN + "/a/b/c", contractPage(CONTRACT, "", PROBE, "<script src=\"/views/juneau-icons.js?v=1\"></script>")).watchRequests();
		// No icons script on the page at all.
		f.page("none", ORIGIN + "/a/b/c", contractPage(CONTRACT, "", PROBE, "")).watchRequests();
		// Each logo kind draws only its own image: the default drop, a page-supplied URL, or an inline symbol.
		f.page("kind-default", ORIGIN + "/a/b/c", contractPage(contract("{}"), "", PROBE, ""));
		f.page("kind-image", ORIGIN + "/a/b/c", contractPage(contract("{\"src\":\"/logo-test.png\"}"), "", PROBE, ""));
		f.page("kind-symbol", ORIGIN + "/a/b/c", contractPage(CONTRACT, "", PROBE + "<script src=\"/views/juneau-icons.js?v=1\"></script>", ""));
		report = f.run();
	}

	private static String contract(String logo) {
		return "{\"contractVersion\":\"1\",\"title\":\"T\",\"header\":{\"title\":\"Pets\",\"logo\":" + logo + "},\"cards\":[]}";
	}

	private static List<String> paths(String name) {
		var l = new ArrayList<String>();
		for (var e : requestLog(report.get(name)))
			l.add(String.valueOf(e.get("path")));
		return l;
	}

	@Test void a01_absoluteIconsScriptFetchesTheSprite() {
		assertTrue(paths("absolute").contains(SPRITE), () -> "requests: " + paths("absolute"));
	}

	@Test void a02_relativeIconsScriptFetchesTheSprite() {
		assertTrue(paths("relative").contains(SPRITE), () -> "requests: " + paths("relative"));
	}

	@Test void a03_noIconsScriptDrawsNoLogoAndWarnsOnce() {
		assertFalse(paths("none").contains("/a/b/juneau-symbols-lucide.svg"), () -> "requests: " + paths("none"));
		assertEquals(Boolean.FALSE, probe("none").get("hasLogo"));
		assertEquals(1, ((Number)probe("none").get("warnings")).intValue());
	}

	@Test void a04_iconsScriptDrawsTheLogo() {
		assertEquals(Boolean.TRUE, probe("absolute").get("hasLogo"));
		assertEquals(0, ((Number)probe("absolute").get("warnings")).intValue());
	}

	@Test void a05_iconsScriptAfterTheShellStillDrawsTheLogo() {
		assertTrue(paths("late").contains(SPRITE), () -> "requests: " + paths("late"));
		assertEquals(Boolean.TRUE, probe("late").get("hasLogo"));
		assertEquals(0, ((Number)probe("late").get("warnings")).intValue());
	}

	@Test void b01_symbolLogoPaintsNoBackgroundImage() {
		var p = probe("kind-symbol");
		assertEquals(1, ((Number)p.get("marks")).intValue());
		assertEquals("none", p.get("bg"), "the symbol tile must not sit on the default drop");
		assertEquals(Boolean.TRUE, p.get("svg"));
		assertFalse(String.valueOf(p.get("cls")).contains("jc-logo-default"), () -> String.valueOf(p.get("cls")));
	}

	@Test void b02_defaultLogoShowsTheDrop() {
		var p = probe("kind-default");
		assertTrue(String.valueOf(p.get("cls")).contains("jc-logo-default"), () -> String.valueOf(p.get("cls")));
		assertTrue(String.valueOf(p.get("bg")).contains("data:image/svg+xml"), () -> String.valueOf(p.get("bg")));
	}

	@Test void b03_imageLogoShowsOnlyItsUrl() {
		var p = probe("kind-image");
		assertEquals(1, ((Number)p.get("marks")).intValue());
		assertTrue(String.valueOf(p.get("bg")).contains("/logo-test.png"), () -> String.valueOf(p.get("bg")));
		assertFalse(String.valueOf(p.get("bg")).contains("data:image/svg+xml"), () -> String.valueOf(p.get("bg")));
		assertFalse(String.valueOf(p.get("cls")).contains("jc-logo-default"), () -> String.valueOf(p.get("cls")));
	}

	private static Map<String,Object> probe(String name) {
		var r = report.get(name);
		return ConsoleBrowserFixture.probe(r);
	}
}
