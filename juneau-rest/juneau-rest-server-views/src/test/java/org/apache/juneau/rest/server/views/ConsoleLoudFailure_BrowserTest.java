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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Every shell failure E-JS-1 to E-JS-14, and the registration-time E-JS-20 to E-JS-23, shows in the {@code .jc-console-error} banner with the exact message, and is
 * logged through console.error with the {@code [juneau-console] } prefix.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleLoudFailure_BrowserTest extends TestBase {

	private static Map<String,Map<String,Object>> report;
	private static Map<String,Map<String,Object>> registerReport;

	private static String c(String rest) {
		return "{\"contractVersion\":\"1\",\"title\":\"F\"" + (rest.isEmpty() ? "" : "," + rest) + "}";
	}

	@BeforeAll
	static void probe() throws Exception {
		var noIsland = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>\n"
			+ "<template data-card=\"x\">x</template>\n<script src=\"" + SHELL + "\"></script>\n</body></html>";
		report = ConsoleBrowserFixture.create("loud-failure")
			.page("e1", ORIGIN + "/e1", noIsland)
			.page("e1-json", ORIGIN + "/e1-json", contractPage("{not json", ""))
			.page("e2", ORIGIN + "/e2", contractPage("{\"contractVersion\":\"2\",\"title\":\"F\"}", ""))
			.page("e2b", ORIGIN + "/e2b", contractPage("{\"version\":\"1\",\"title\":\"F\"}", ""))
			.page("e3", ORIGIN + "/e3", contractPage(c("\"header\":{\"title\":\"T\",\"slots\":{\"banner\":\"header.banner\"}}"), ""))
			.page("e4", ORIGIN + "/e4", contractPage(c("\"cards\":[{\"id\":\"k\",\"type\":\"kpi\"}]"), ""))
			.page("e5", ORIGIN + "/e5", contractPage(c("\"nav\":[{\"id\":\"a\",\"label\":\"A\",\"href\":\"/a\"}],\"activeNav\":[\"a\",\"zz\"]"), ""))
			.page("e6", ORIGIN + "/e6", contractPage(c("\"nav\":[{\"id\":\"a\",\"label\":\"A\",\"href\":\"/a\"},{\"id\":\"a\",\"label\":\"B\",\"href\":\"/b\"}]"), ""))
			.page("e7", ORIGIN + "/e7", contractPage(c(""), "<template data-card=\"x\">1</template><template data-card=\"x\">2</template>"))
			.page("e8", ORIGIN + "/e8", contractPage(c("\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"src\":\"https://evil.test/x\"}]"), ""))
			.page("e9", ORIGIN + "/e9", contractPage(c("\"nav\":[{\"id\":\"a\",\"label\":\"A\",\"href\":\"javascript:alert(1)\"}]"), ""))
			.page("e10", ORIGIN + "/e10", contractPage(c("\"cards\":[{\"id\":\"t\",\"type\":\"datatables\",\"table\":\"/rest/t\"}]"), ""))
			.page("e13", ORIGIN + "/e13", contractPage(c("\"cards\":[{\"id\":\"co\",\"type\":\"console-output\",\"output\":{\"linesUrl\":\"/runs/1/lines\"}}]"), ""))
			.page("e14", ORIGIN + "/e14", contractPage(c("\"cards\":[{\"id\":\"rv\",\"type\":\"run-view\",\"runView\":{\"eventsUrl\":\"/runs/1/events\"}}]"), ""))
			// E-JS-11/E-JS-12 (juneau-console.js lines 39, 51-52, 492, 601, 1023-1024, 1026-1034): the page's own
			// afterShell script calls JuneauConsole.mount/registerCard a second time, after the shell's own
			// auto-mount (lines 1036-1049) already claimed document.body / registered "html" at boot.
			.page("e11", ORIGIN + "/e11", contractPage(c(""), "", "",
				"<script>window.JuneauConsole.mount(" + c("") + ", {root: document.body});</script>"))
			.page("e12", ORIGIN + "/e12", contractPage(c(""), "", "",
				"<script>window.JuneauConsole.registerCard('html', function(){});</script>"))
			// E-JS-24/E-JS-25 are card-level: painted in the card body, logged unprefixed by juneau-views.js, no banner.
			.page("e24", ORIGIN + "/e24", contractPage(c("\"cards\":[{\"id\":\"releases\",\"type\":\"datatables\","
				+ "\"table\":{\"dataUrl\":\"/rest/releases/data\"}}]"), "", viewsPack(), ""),
				"cardError", "#releases-body .jc-card-error")
			.page("e25", ORIGIN + "/e25", contractPage(c("\"cards\":[{\"id\":\"releases\",\"type\":\"datatables\","
				+ "\"src\":\"/rest/releases/data\"}]"), "", viewsPack(), ""),
				"cardError", "#releases-body .jc-card-error")
			.run();
	}

	private static String noIslandPage(String script) {
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>\n"
			+ script + "\n</body></html>";
	}

	// Registration-time failures cannot ride a contract island: an inline script calls the API directly. The caught
	// error goes to window.__probe, which console-shell.cjs copies into the report as "probe".
	private static String caught(String call) {
		return "<script>window.__probe = {}; try { " + call + " } catch (e) { window.__probe.caught = { name: e.name, code: e.code, message: e.message }; }</script>";
	}

	@BeforeAll
	static void probeRegister() throws Exception {
		var shell = "<script src=\"" + SHELL + "\"></script>\n";
		var badType = shell + caught("JuneauConsole.registerCard('Bad_Type', function(){});");
		var badHandler = shell + caught("JuneauConsole.registerCard('ok', 'nope');");
		var notMounted = shell + caught("JuneauConsole.refreshCard('never-mounted');");
		var badQueueEntry = "<script>(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['oops', 'nope', 'extra']);</script>\n" + shell;
		registerReport = ConsoleBrowserFixture.create("loud-failure-register")
			.page("e20", ORIGIN + "/e20", noIslandPage(badType))
			.page("e21", ORIGIN + "/e21", noIslandPage(badHandler))
			.page("e22", ORIGIN + "/e22", noIslandPage(notMounted))
			.page("e23", ORIGIN + "/e23", noIslandPage(badQueueEntry))
			.run();
	}

	@Test void a01_missingIsland() {
		assertFailure(report.get("e1"), "E-JS-1", "missing or unparseable <script id=\"juneau-page\">: 'no #juneau-page island'");
	}

	@Test void a02_unparseableIsland() {
		var r = report.get("e1-json");
		var prefix = "missing or unparseable <script id=\"juneau-page\">: '";
		assertTrue(shellErrors(r).stream().anyMatch(s -> s.startsWith(prefix)), r::toString);
		assertEquals("E-JS-1", ((Map<?,?>)((List<?>)r.get("banner")).get(0)).get("code"));
	}

	@Test void a03_unsupportedVersion() {
		assertFailure(report.get("e2"), "E-JS-2", "unsupported page contract version '2'; this shell supports '1'");
	}

	@Test void a03b_renamedVersionKey() {
		assertFailure(report.get("e2b"), "E-JS-2",
			"page contract key 'version' was renamed to 'contractVersion'; regenerate the page with a current Juneau");
	}

	@Test void a04_missingSlotTemplate() {
		assertFailure(report.get("e3"), "E-JS-3",
			"header.slots.banner references template 'header.banner', but no <template data-slot=\"header.banner\"> exists");
	}

	@Test void a05_unknownCardType() {
		assertFailure(report.get("e4"), "E-JS-4", "card 'k' has unknown type 'kpi'; registered types: 'html, console-output, run-view'");
	}

	@Test void a06_activeNavNotInTree() {
		assertFailure(report.get("e5"), "E-JS-5", "activeNav 'a/zz' is not a path in the nav tree (failed at 'zz')");
	}

	@Test void a07_duplicateNavId() {
		assertFailure(report.get("e6"), "E-JS-6", "duplicate nav id 'a'");
	}

	@Test void a08_duplicateTemplate() {
		assertFailure(report.get("e7"), "E-JS-7", "duplicate <template data-card=\"x\">");
	}

	@Test void a09_handlerThrew() {
		assertFailure(report.get("e8"), "E-JS-8", "card 'h' (type 'html') handler threw: 'src 'https://evil.test/x' is not same-origin'");
	}

	@Test void a10_unsafeHref() {
		assertFailure(report.get("e9"), "E-JS-9", "unsafe href 'javascript:alert(1)' on nav 'A'");
	}

	@Test void a11_datatablesWithoutViews() {
		assertFailure(report.get("e10"), "E-JS-10", "datatables card 't' needs JuneauViews.regions; load the views toolkit");
	}

	@Test void a12_cardLevelFailures_doNotStopTheMount() {
		for (var name : List.of("e8", "e9", "e10", "e13", "e14")) {
			var r = report.get(name);
			assertNotNull(r.get("mounted"), () -> name + " must still finish the mount: " + r);
			assertEmpty(() -> name + " must not throw: " + r, r.get("pageErrors"));
		}
	}

	/**
	 * E-JS-11 is FATAL (juneau-console.js line 39) and is raised by {@code mount(contract, opts)} (line 492) when the
	 * page's own script calls {@code JuneauConsole.mount(...)} a second time against the same root the shell's own
	 * auto-mount (lines 1036-1049) already claimed - here {@code document.body}, since {@code contractPage()} places
	 * the island directly under {@code <body>} and the shell auto-mounts with {@code root: island.parentNode}.
	 */
	@Test void a13_mountCalledTwiceOnSameRoot() {
		assertFailure(report.get("e11"), "E-JS-11", "JuneauConsole.mount called twice on the same root");
	}

	/**
	 * E-JS-12 is FATAL (juneau-console.js line 39) and is raised by {@code registerCard(type, handler)} (line 601)
	 * when a type collides with a handler already in the map - here the built-in {@code "html"} handler the shell
	 * pre-registers at boot (lines 1023-1024), so the page's own {@code registerCard('html', ...)} call collides.
	 */
	@Test void a14_cardTypeAlreadyRegistered() {
		assertFailure(report.get("e12"), "E-JS-12", "card type 'html' is already registered");
	}

	@Test void a15_consoleOutputWithoutViews() {
		assertFailure(report.get("e13"), "E-JS-13", "console-output card 'co' needs JuneauViews.consoleOutput; load the views toolkit");
	}

	@Test void a16_runViewWithoutViews() {
		assertFailure(report.get("e14"), "E-JS-14", "run-view card 'rv' needs JuneauViews.runView; load the views toolkit");
	}

	@Test void a17_badTypeName() {
		assertFailure(registerReport.get("e20"), "E-JS-20", "card type 'Bad_Type' must match /^[a-z][a-z0-9-]{0,31}$/");
	}

	@Test void a18_badHandlerShape() {
		assertFailure(registerReport.get("e21"), "E-JS-21", "card type 'ok' handler must be a function or an object with render(); got 'string'");
	}

	@Test void a19_refreshNotMounted_throwsWithoutBanner() {
		var r = registerReport.get("e22");
		assertEquals(List.of(), r.get("banner"), () -> "E-JS-22 must not show in the page banner: " + r);
		assertEquals(List.of(), shellErrors(r), () -> "E-JS-22 must not console.error: " + r);
		var caught = (Map<?,?>)ConsoleBrowserFixture.probe(r).get("caught");
		assertEquals("E-JS-22", caught.get("code"), r::toString);
		assertEquals("card 'never-mounted' is not mounted", caught.get("message"), r::toString);
	}

	@Test void a20_badQueueEntry() {
		assertFailure(registerReport.get("e23"), "E-JS-23", "JuneauConsoleCards entry '[\"oops\",\"nope\",\"extra\"]' is not a [type, handler] pair");
	}

	private static void assertCardLevelFailure(Map<String,Object> r, String message) {
		assertEquals(List.of(), r.get("banner"), () -> "card-level failure must not reach the page banner: " + r);
		assertEquals(message, queries(r).get("cardError"), () -> "card body does not show the failure: " + r);
		assertTrue(((List<?>)r.get("consoleErrors")).contains(message), () -> "console.error did not log '" + message + "': " + r);
		assertNotNull(r.get("mounted"), () -> "the mount must still finish: " + r);
	}

	@Test void a21_datatablesLiftShapeError_isCardLevel() {
		assertCardLevelFailure(report.get("e24"), "datatables card 'releases': 'columns' must be a non-empty array");
	}

	@Test void a22_datatablesSrcFetchFailed_isCardLevel() {
		assertCardLevelFailure(report.get("e25"), "datatables card 'releases' src '/rest/releases/data' failed: 'HTTP 404'");
	}
}
