// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.console.views;

import static org.apache.juneau.console.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Page Tabs that do not fit the viewport scroll horizontally, show an edge fade on the side that has more tabs, take
 * keyboard focus, and start with the active tab in view.  A strip that fits shows no fade.
 */
@SuppressWarnings({
	"unchecked" // The report is parsed into raw Maps and Lists.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class NavOverflow_BrowserTest extends TestBase {

	private static Map<String,Map<String,Object>> report;

	private static String contract(String active) {
		var nav = new StringJoiner(",");
		for (var n : List.of("Projects", "Surfaces", "Daemon", "Runs", "Reviews", "Settings"))
			nav.add("{\"id\":\"" + n.toLowerCase() + "\",\"label\":\"" + n + "\",\"href\":\"/" + n.toLowerCase() + "\"}");
		return "{\"contractVersion\":\"1\",\"title\":\"Nav\",\"nav\":[" + nav + "],\"activeNav\":[\"" + active + "\"],\"cards\":[]}";
	}

	@BeforeAll
	static void probe() throws Exception {
		var f = ConsoleBrowserFixture.create("nav-overflow");
		f.page("w400-first", ORIGIN + "/projects", contractPage(contract("projects"), "")).viewport(400, 800);
		f.page("w400-last", ORIGIN + "/settings", contractPage(contract("settings"), "")).viewport(400, 800);
		f.page("w600-last", ORIGIN + "/settings", contractPage(contract("settings"), "")).viewport(600, 800);
		f.page("w1280", ORIGIN + "/projects", contractPage(contract("projects"), ""));
		report = f.run();
	}

	private static Map<String,Object> row(String c) {
		assertNoShellErrors(report.get(c));
		return ((List<Map<String,Object>>)report.get(c).get("navOverflow")).get(0);
	}

	@Test void a01_narrowFirstActive_fadesEndOnly() {
		var r = row("w400-first");
		assertTrue(((Number)r.get("scrollWidth")).intValue() > ((Number)r.get("clientWidth")).intValue(), r::toString);
		assertNotEquals("true", r.get("start"), r::toString);
		assertEquals("true", r.get("end"), r::toString);
		assertNotEquals("none", r.get("mask"), r::toString);
		assertEquals(true, r.get("activeVisible"), r::toString);
	}

	@Test void a02_narrowLastActive_scrollsActiveIntoViewAndFadesStart() {
		var r = row("w400-last");
		assertTrue(((Number)r.get("scrollLeft")).intValue() > 0, r::toString);
		assertEquals("true", r.get("start"), r::toString);
		assertNotEquals("true", r.get("end"), r::toString);
		assertEquals(true, r.get("activeVisible"), r::toString);
	}

	@Test void a03_w600_lastTabNotClipped() {
		var r = row("w600-last");
		assertEquals(true, r.get("activeVisible"), r::toString);
	}

	@Test void a04_keyboardReachable() {
		assertEquals("0", row("w400-first").get("tabindex"));
	}

	@Test void a05_fitsAtDesktop_noFade() {
		var r = row("w1280");
		assertNotEquals("true", r.get("start"), r::toString);
		assertNotEquals("true", r.get("end"), r::toString);
		assertEquals("none", r.get("mask"), r::toString);
	}
}
