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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The end-to-end half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} / {@code juneau-urlstate.js} / {@code juneau-search.js} / {@code juneau-pagestate.js} /
 * {@code juneau-config.js} in a real headless browser and proves, together in one page, the four scenarios spec
 * §9's last table row lists for F1 + F2 + F3 + Q1 (plus a fifth, Copy-link): a malformed {@code ?state=} tab id applied alongside a real
 * filter clause; the View Settings dialog driven keyboard-only; a column-search popover's Escape-revert
 * announcement and focus return; and a version-mismatched {@code schemaVersion} blob's one-time reset notice surviving a real
 * page reload; and a real click on the Copy-link toolbar button copying and announcing the built URL.
 *
 * <h5 class='section'>Why this exists (beyond the Node harnesses):</h5>
 * <p>
 * {@code ViewsJs_UrlState_Test}, {@code ViewsJs_ColumnSearchPopover_Test}, {@code ViewsJs_ConfigChooser_Test}, and
 * {@code ViewsJs_ConfigChooserA11y_Test} each prove one fix against a hand-rolled Node DOM shim. None of them can
 * prove a real {@code KeyboardEvent} bubbles to a document-level handler, that {@code Enter} on a focused
 * {@code <button>} triggers its native click, that {@code location.search} is decoded by a real browser's URL
 * handling, or that a stored blob survives a real {@code page.reload()} against real {@code localStorage}.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests}
 * Maven profile does. Run with {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ConfigPersistence_BrowserTest} &mdash; the sibling canary this one borrows its
 * 		fixture-building and prober-running conventions from.
 * </ul>
 */
@EnabledIfSystemProperty(named=ViewSettingsA11y_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // Browser report values are cast to Map<String,Object>
})
class ViewSettingsA11y_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		// The pom's js-tests profile provisions ONE harness property; this canary lives beside it in src/test/js,
		// so it is derived from that property's directory rather than adding a new pom property.
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("view-settings-a11y-browser.cjs");

		// Dependency order per the module's own doc comment: renders -> views -> urlstate -> search -> pagestate ->
		// config.  No CSS is loaded: selectConfigTab toggles the native `hidden` property and the popover sets its
		// own inline `display`, so neither juneau-views.css nor juneau-config.css affects any assertion here.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.URLSTATE_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.SEARCH_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.PAGESTATE_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.CONFIG_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("view-settings-a11y.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("view-settings-a11y-stdout.json");
		var stderr = dir.resolve("view-settings-a11y-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	private static Map<String,Object> obj(String key) { return (Map<String,Object>) report.get(key); }

	private static Map<String,Object> sub(Map<String,Object> parent, String key) { return (Map<String,Object>) parent.get(key); }

	//------------------------------------------------------------------------------------------------------------------
	// Overall: no console/page error anywhere across all scenarios
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_noConsoleOrPageErrorsAcrossAnyScenario() {
		assertEmpty(report::toString, report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scenario 1 (F1): ?state=tab(a%5C);filter(status=$eq(OK)) decodes and applies with no throw
	//------------------------------------------------------------------------------------------------------------------

	@Test void f1_01_theBackslashTabIdDecodesExactlyAndOnlyThatTabIsClicked() {
		// Exactly the tab whose dataset.juneauStripTab equals the decoded id is clicked, never 'plain'.
		// A literal backslash is BCT's escape character, so these two values are compared as plain strings.
		assertEquals("a\\", obj("f1").get("decodedTab"), report::toString);
		assertEquals(List.of("a\\"), obj("f1").get("clicked"), report::toString);
	}

	@Test void f1_02_theFilterClauseDecodesAndReachesTheColumn() {
		assertBean(report::toString, obj("f1"), "decodedFilterColumn,decodedFilterExpr,appliedExpr", "status,$eq(OK),$eq(OK)");
	}

	@Test void f1_03_onAClientDslColumnTheFilterLandsInTheStoreAndPredicate() {
		// WORK-J0612: real juneau-search.js validates the restored $eq(OK); it installs the "juneau-dsl" predicate and
		// the per-table store, never native col.search().
		assertBean(report::toString, obj("f1"), "dslStoredExpr,dslNativeExpr,dslPredicateMatchesOk,dslPredicateRejectsOther",
			"$eq(OK),,true,true");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scenario 2 (F3): keyboard-only dialog nav - gear, Enter, Tab, Right x3, End, Home
	//------------------------------------------------------------------------------------------------------------------

	@Test void f2_01_enterOnTheFocusedGearOpensTheDialogAndTabLandsOnTheViewTab() {
		assertBean(report::toString, sub(obj("f2"), "afterTab"), "activeTab,visiblePanel", "view,view");
	}

	@Test void f2_02_arrowRightAdvancesThroughSearchThenSort() {
		assertBean(report::toString, sub(obj("f2"), "afterRight1"), "activeTab,visiblePanel", "search,search");
		assertBean(report::toString, sub(obj("f2"), "afterRight2"), "activeTab,visiblePanel", "sort,sort");
	}

	@Test void f2_03_arrowRightLandsOnOptionsAndEndStaysThere() {
		assertBean(report::toString, sub(obj("f2"), "afterRight3"), "activeTab,visiblePanel", "options,options");
		assertBean(report::toString, sub(obj("f2"), "afterEnd"), "activeTab,visiblePanel", "options,options");
	}

	@Test void f2_04_homeJumpsBackToTheViewTab() {
		assertBean(report::toString, sub(obj("f2"), "afterHome"), "activeTab,visiblePanel", "view,view");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scenario 3 (F2): type into the column-search popover, Escape reverts and announces
	//------------------------------------------------------------------------------------------------------------------

	@Test void f3_01_escapeRemovesThePopoverAndReturnsFocusToTheSearchIcon() {
		// focusReturnedToIcon: the shared layer stack's returnFocusTo must put focus back on the search icon.
		assertBean(report::toString, obj("f3"), "popoverRemoved,focusReturnedToIcon", "true,true");
	}

	@Test void f3_02_escapeAnnouncesTheRevertWithTheColumnTitle() {
		assertBean(report::toString, obj("f3"), "announcerText", "Search for 'Status' not applied.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scenario 4 (Q1): a version-mismatched schemaVersion blob survives a reload, notices once, never twice
	//------------------------------------------------------------------------------------------------------------------

	@Test void f4_01_firstOpenAfterReloadShowsTheResetNoticeOnce() {
		assertBean(report::toString, sub(obj("f4"), "firstOpen"), "noticeShown,noticeText",
			"true,Saved view settings were reset because the table changed.");
	}

	@Test void f4_02_secondOpenAfterASecondReloadShowsNoNotice() {
		// The blob was deleted on first detection, so a second reload finds nothing stored.
		assertBean(report::toString, sub(obj("f4"), "secondOpen"), "noticeShown", "false");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scenario 5 (Copy-link): a real click copies the built URL (via a stubbed clipboard) and announces it
	//------------------------------------------------------------------------------------------------------------------

	@Test void f5_01_aRealClickCopiesTheBuiltUrlAndAnnouncesSuccess() {
		assertBean(report::toString, obj("f5"), "copiedHasShared,announcerText", "true,Link copied.");
	}
}
