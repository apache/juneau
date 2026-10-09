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
package org.apache.juneau.rest.server.terminal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The terminal region in a real Chromium: the real xterm.js WebJar and the real {@code juneau-terminal.js}, driven by
 * {@code terminal-browser.cjs}.
 *
 * <p>
 * The prober answers every request itself through Playwright's request routing on a made-up origin, so nothing
 * listens on a port.  The endpoints' headers are covered by {@link TerminalMixin_Test}; the end-to-end path is
 * {@link TerminalSmoke_BrowserTest}.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named=Terminal_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with -Pjs-tests")
class Terminal_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	@BeforeAll static void setUp() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harnessDir"), "terminal-browser.cjs");
		var out = TerminalHarness.run(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString()),
			TerminalHarness.BROWSER_ASSETS,
			Map.of("NODE_PATH", dir.resolve("node_modules").toString(), "PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers")),
			300);
		report = Json.to(out, Map.class);
		assertNull(report.get("harnessError"), () -> "prober failed: " + report.get("harnessError"));
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Asserts that case {@code name} passed with no page or console errors. */
	private static void ok(String name) {
		var cases = (Map<?,?>)report.get("cases");
		assertEquals(true, cases.get(name), () -> name + " -> " + cases.get(name));
		assertEquals(List.of(), ((Map<?,?>)report.get("pageErrors")).get(name), () -> name + " page errors");
		assertEquals(List.of(), ((Map<?,?>)report.get("consoleErrors")).get(name), () -> name + " console errors");
	}

	@Test void a01_umdBundleLoadsFromAClassicScript() { ok("b01_umdBundleLoadsFromAClassicScript"); }
	@Test void a02_sgrColoursReachTheCellsAndTheBadgeSaysDone() { ok("b02_sgrColoursReachTheCellsAndTheBadgeSaysDone"); }
	@Test void a03_carriageReturnRedrawsInPlace() { ok("b03_carriageReturnRedrawsInPlace"); }
	@Test void a04_altScreenBoxIsDrawn() { ok("b04_altScreenBoxIsDrawn"); }
	@Test void a05_scrollbackAndSearch() { ok("b05_scrollbackAndSearch"); }
	@Test void a06_hashScrollsToTheStepOffset() { ok("b06_hashScrollsToTheStepOffset"); }
	@Test void a07_fontScalesWithThePanelWhileColsAndRowsStay() { ok("b07_fontScalesWithThePanelWhileColsAndRowsStay"); }
	@Test void a08_osc52LeavesTheClipboardAlone() { ok("b08_osc52LeavesTheClipboardAlone"); }
	@Test void a09_onlyHttpLinksOpen() { ok("b09_onlyHttpLinksOpen"); }
	@Test void a10_rawLinkOnlyForHttpUrls() { ok("b10_rawLinkOnlyForHttpUrls"); }

	@Test void z01_everyCaseHasAMethod() {
		var cases = (Map<?,?>)report.get("cases");
		assertEquals(10, cases.size(), () -> "case count changed; add a method per new case: " + cases.keySet());
	}
}
