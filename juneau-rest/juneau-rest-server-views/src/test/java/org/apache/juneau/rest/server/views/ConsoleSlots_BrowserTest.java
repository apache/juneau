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
 * Header and footer slots render in place; an inline script inside a template runs exactly once at mount; a JSON
 * island inside a template is readable by a later script and by a DOMContentLoaded listener; and a {@code replace}-
 * kind slot swaps the entire stock element rather than filling it.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleSlots_BrowserTest extends TestBase {

	private static final String SLOTS = """
		{"version":"1","title":"Slots",
		 "header":{"title":"T","slots":{"brand":"header.brand","actions":"header.actions","banner":"header.banner"}},
		 "footer":{"slots":{"content":"footer.content"}},
		 "cards":[{"id":"jc-seg-1","type":"html","template":"jc-seg-1","bare":true}]}
		""";

	private static final String SLOT_TEMPLATES = """
		<template data-slot="header.brand"><span class="probe-brand">B</span></template>
		<template data-slot="header.actions"><button type="button" class="probe-action">A</button></template>
		<template data-slot="header.banner"><div class="probe-banner">Demo</div></template>
		<template data-slot="footer.content"><em class="probe-foot">F</em></template>
		<template data-card="jc-seg-1"><div id="seg">seg</div>
		<script>window.__probe = window.__probe || {}; window.__probe.runs = (window.__probe.runs || 0) + 1;</script>
		<script type="application/json" id="island">{"k":42}</script></template>
		""";

	private static final String AFTER = """
		<script>
		window.__probe = window.__probe || {};
		window.__probe.templatesLeft = document.querySelectorAll('template').length;
		window.__probe.islandLater = JSON.parse(document.getElementById('island').textContent).k;
		document.addEventListener('DOMContentLoaded', function () {
			window.__probe.islandReady = JSON.parse(document.getElementById('island').textContent).k;
		});
		</script>
		""";

	private static final String REPLACE = """
		{"version":"1","title":"Replace","header":{"title":"ignored","slots":{"replace":"header.replace"}},"cards":[]}
		""";

	private static final String CHROME_ON = """
		{"version":"1","title":"Chrome","header":{"title":"T","chrome":true,"slots":{"banner":"header.banner"}},
		 "nav":[{"id":"home","label":"Home","href":"/chrome"}],"activeNav":["home"],"cards":[]}
		""";

	private static final String CHROME_OFF = """
		{"version":"1","title":"NoChrome","header":{"title":"T","slots":{"banner":"header.banner"}},
		 "nav":[{"id":"home","label":"Home","href":"/nochrome"}],"activeNav":["home"],"cards":[]}
		""";

	private static final String BANNER_ONLY = """
		<template data-slot="header.banner"><div class="probe-banner">Demo</div></template>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("slots")
			.page("slots", ORIGIN + "/slots", contractPage(SLOTS, SLOT_TEMPLATES, "", AFTER),
				"brand", "header.jc-header > .jc-brand > .probe-brand",
				"actions", "header.jc-header > .jc-header-actions > .probe-action",
				"banner", "body > .probe-banner",
				"footer", "footer.jc-page-footer > .probe-foot",
				"order", "header.jc-header + .probe-banner + main.jc-main + footer.jc-page-footer",
				"segment", "main.jc-main > #seg")
			.page("chromeOn", ORIGIN + "/chrome", contractPage(CHROME_ON, BANNER_ONLY),
				"order", "body > div.jc-chrome > header.jc-header + .probe-banner + nav.juneau-page-nav",
				"strayBanner", "body > .probe-banner")
			.page("chromeOff", ORIGIN + "/nochrome", contractPage(CHROME_OFF, BANNER_ONLY),
				"order", "body > header.jc-header + .probe-banner + nav.juneau-page-nav")
			.page("replace", ORIGIN + "/replace",
				contractPage(REPLACE, "<template data-slot=\"header.replace\"><header class=\"my-header\">WHOLE</header></template>"),
				"replaced", "body > header.my-header",
				"stock", ".jc-header")
			.run();
	}

	@Test void a01_slotsRenderInPlace() {
		var r = report.get("slots");
		assertNoShellErrors(r);
		var q = queries(r);
		assertEquals("B", q.get("brand"));
		assertEquals("A", q.get("actions"));
		assertEquals("Demo", q.get("banner"));
		assertEquals("F", q.get("footer"));
		assertEquals("F", q.get("order"), "header, banner, main, footer must be siblings in that order");
		assertEquals("seg", q.get("segment"), "a bare html card is inserted without a .jc-card wrapper");
	}

	@Test void a01b_bannerSitsBetweenHeaderAndNav_insideChromeWhenOn() {
		var on = report.get("chromeOn");
		assertNoShellErrors(on);
		assertEquals("Home", queries(on).get("order"),
			"chrome on: .jc-chrome must hold header, banner, nav as consecutive siblings in that order");
		assertNull(queries(on).get("strayBanner"), "chrome on: the banner must not be a direct child of <body>");
		var off = report.get("chromeOff");
		assertNoShellErrors(off);
		assertEquals("Home", queries(off).get("order"), "chrome off: header, banner, nav are body siblings in that order");
	}

	@Test void a02_inlineScriptRunsOnce_andTemplatesAreConsumed() {
		var p = ConsoleBrowserFixture.probe(report.get("slots"));
		assertEquals(1, ((Number)p.get("runs")).intValue());
		assertEquals(0, ((Number)p.get("templatesLeft")).intValue());
	}

	@Test void a03_jsonIslandIsReadableLaterAndOnReady() {
		var p = ConsoleBrowserFixture.probe(report.get("slots"));
		assertEquals(42, ((Number)p.get("islandLater")).intValue());
		assertEquals(42, ((Number)p.get("islandReady")).intValue());
	}

	@Test void a04_replaceSlot_replacesTheWholeHeader() {
		var r = report.get("replace");
		assertNoShellErrors(r);
		assertEquals("WHOLE", queries(r).get("replaced"));
		assertNull(queries(r).get("stock"));
	}
}
