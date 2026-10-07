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
 * Bar slots hydrate through {@code JuneauConsole.chrome}: once after mount (a document scan), and again when
 * {@code juneau-views.js} inserts a row-detail panel and calls {@code enhanceChromeInPanel}. Without the shell the
 * slot stays as served and views logs a console.error.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleBarSlot_BrowserTest extends TestBase {

	private static final String SEG = """
		{"contractVersion":"1","title":"Bar","cards":[{"id":"jc-seg-1","type":"html","template":"jc-seg-1","bare":true}]}
		""";

	private static final String SEG_TEMPLATE = """
		<template data-card="jc-seg-1"><div data-juneau-bar-slot="b1"><span data-juneau-badge="pending">0</span></div>
		<script type="application/json" id="juneau-bar:b1">{"contractVersion":"1","badges":{"pending":7}}</script></template>
		""";

	private static final String INSERT_PANEL = """
		<script src="/views/juneau-views.js"></script>
		<script>
		window.addEventListener('load', function () {
			var panel = document.createElement('div');
			panel.className = 'juneau-view-detail-panel';
			panel.innerHTML = '<div data-juneau-bar-slot="b2"><span data-juneau-badge="n" data-juneau-badge-max="9">0</span></div>'
				+ '<script type="application/json" id="juneau-bar:b2">{"contractVersion":"1","badges":{"n":12}}<\\/script>';
			document.body.appendChild(panel);
			window.__probe = { enhanced: window.JuneauViews.init.enhanceChromeInPanel(panel) };
		});
		</script>
		""";

	private static final String BADGE_B2 = "[data-juneau-bar-slot=\"b2\"] [data-juneau-badge=\"n\"]";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var viewsOnly = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>\n"
			+ INSERT_PANEL + "\n</body></html>";
		report = ConsoleBrowserFixture.create("bar-slot")
			.page("scan", ORIGIN + "/scan", contractPage(SEG, SEG_TEMPLATE),
				"badge", "[data-juneau-bar-slot=\"b1\"] [data-juneau-badge=\"pending\"]")
			.page("insert", ORIGIN + "/insert", contractPage("{\"contractVersion\":\"1\",\"title\":\"Bar\",\"cards\":[]}", "", "", INSERT_PANEL),
				"badge", BADGE_B2)
			.page("no-shell", ORIGIN + "/no-shell", viewsOnly, "badge", BADGE_B2)
			.run();
	}

	@Test void a01_mountScansTheDocument() {
		var r = report.get("scan");
		assertNoShellErrors(r);
		assertEquals("7", queries(r).get("badge"));
	}

	@Test void a02_insertedPanel_hydratesThroughEnhanceChromeInPanel() {
		var r = report.get("insert");
		assertNoShellErrors(r);
		assertEquals(Boolean.TRUE, ConsoleBrowserFixture.probe(r).get("enhanced"));
		assertEquals("9+", queries(r).get("badge"), "count 12 is clamped by data-juneau-badge-max=9");
	}

	@Test void a03_withoutTheShell_slotStaysAndViewsLogs() {
		var r = report.get("no-shell");
		assertEquals(Boolean.FALSE, ConsoleBrowserFixture.probe(r).get("enhanced"));
		assertEquals("0", queries(r).get("badge"));
		assertTrue(((List<?>)r.get("consoleErrors")).contains("[juneau-views] bar slot not hydrated: juneau-console.js is not loaded"),
			() -> r.get("consoleErrors").toString());
	}
}
