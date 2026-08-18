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
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The module's <b>JavaScript-execution harness</b>: runs {@code juneau-pages.js} in a real headless browser and
 * asserts on what a user would actually see.
 *
 * <h5 class='section'>Why this exists:</h5>
 * <p>
 * Every other test of this runtime asserts on the served script's <i>source shape</i>, which cannot distinguish a
 * working page from a blank one.  Two real defects reached a released module through exactly that gap: a sub-tabbed
 * tab rendered completely blank, and a tab's outer panel eagerly initialized every one of its sub-panels' tables.
 * Both are invisible to a substring assertion and obvious to a browser.
 *
 * <h5 class='section'>What makes the result trustworthy:</h5>
 * <p>
 * The fixture is not a hand-written page.  This class builds it from the <b>real</b> {@link PageTable} emitter
 * output, the <b>real</b> {@code juneau-views.css} and the <b>real</b> {@code juneau-pages.js} resources
 * {@link ViewsMixin} serves, so nothing under test is restated in the fixture.  Visibility is then measured as a
 * non-empty layout box in Chromium &mdash; the only check that accounts for the CSS cascade through <i>ancestors</i>,
 * which is the whole substance of the blank-tab defect (a sub-panel can carry {@code .jc-active} and still be
 * invisible because the panel wrapping it does not).  An emulated DOM is not good enough here: HtmlUnit reports
 * every panel visible for both the fixed and the broken runtime, so a harness built on one would pass against
 * known-broken code.
 * <p>
 * {@code juneau-views.js} is deliberately <b>not</b> loaded.  The lazy-init seam is instead observed by stubbing
 * {@code JuneauViews.init.initTable} and recording which views it is called for, which is what lets the sub-panel
 * scoping be asserted rather than inferred.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does &mdash; a default {@code mvn install} needs no Node, no npm, no browser download and no network.  See
 * that profile's comment in this module's {@code pom.xml} for how to run it, and
 * {@code src/test/js/panel-visibility.cjs} for the prober this drives.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PageTable_SubtabPanelContract_Test} &mdash; the always-on markup/CSS half of the same
 * 		contract, which runs with no Node at all.
 * </ul>
 */
@EnabledIfSystemProperty(named=PagePanelVisibility_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class PagePanelVisibility_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static final String CATALOG = "#admin/catalog";
	private static final String CATALOG_BUNDLES = "#admin/catalog/bundles";
	private static final String RELEASES = "#admin/releases";

	/** One prober report: the rendered state of the page after visiting one hash. */
	private record Step(Map<?,?> raw) {

		private String str(String k) { return (String) raw.get(k); }

		@SuppressWarnings("unchecked")
		private List<Object> list(String k) { return (List<Object>) raw.get(k); }

		String activeTab() { return str("activeTab"); }

		String activeSubtab() { return str("activeSubtab"); }

		int visibleSubtabBars() { return ((Number) raw.get("visibleSubtabBars")).intValue(); }

		List<Object> initedViews() { return list("initedViews"); }

		List<Object> jsFailures() { return list("jsFailures"); }

		List<Object> errorBanners() { return list("errorBanners"); }

		/** Whether the panel scoped to exactly this (tab, subtab) pair occupies a non-empty box; fails if absent. */
		boolean visible(String tab, String subtab) {
			var matches = list("panels").stream()
				.map(x -> (Map<?,?>) x)
				.filter(x -> tab.equals(x.get("tab")) && Objects.equals(subtab, x.get("subtab")))
				.toList();
			assertEquals(1, matches.size(),
				() -> "expected exactly one panel for tab=" + tab + " subtab=" + subtab + " in " + raw);
			return (Boolean) matches.get(0).get("visible");
		}
	}

	private static List<Step> steps;

	public static class Release {
		public String name;
	}

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("/" + id + "/data")
			.columns(Column.of("name").title("Name"))
			.build();
	}

	/** A leaf tab plus a sub-tabbed tab - the pairing that made the blank-tab defect invisible to a leaf-only suite. */
	private static PageDef page() {
		return PageDef.create("admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("packages", "Packages").view(view("packages")),
					Subtab.create("bundles", "Bundles").view(view("bundles"))))
			.build();
	}

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness"));

		// The fixture restates nothing: emitter output, stylesheet and runtime all come from the real artifacts.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n"
			+ Html.of(PageTable.of(page()))
			+ "\n<script>\n"
			+ resource(ViewsMixin.PAGES_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("page.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		var out = run(dir, harness, fixtureFile, CATALOG, CATALOG_BUNDLES, RELEASES);
		List<?> reports = Json.to(out, List.class);
		steps = reports.stream().map(x -> new Step((Map<?,?>) x)).toList();
		assertEquals(3, steps.size(), () -> "expected one report per hash, got:\n" + out);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture, String...hashes) throws Exception {
		// Every attribute name the prober needs is handed over from PageTable's constants, so the prober is not a
		// third spelling of them; juneau-pages.js's own irreducible copy is pinned in
		// PageTable_SubtabPanelContract_Test instead.  The active-state CLASS names are not passed, because the
		// emitter has no constant for them to come from - see that test's c03.
		var attrs = Json.of(Map.of(
			"panelTab", PageTable.PANEL_TAB_ATTR,
			"panelSubtab", PageTable.PANEL_SUBTAB_ATTR,
			"tabId", PageTable.TAB_ID_ATTR,
			"subtabId", PageTable.SUBTAB_ID_ATTR));
		var cmd = new ArrayList<>(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(),
			fixture.toString(), attrs));
		cmd.addAll(List.of(hashes));

		// Redirected to files rather than pipes, both because a chatty failure can fill one pipe's buffer while this
		// side is still draining the other, and because the raw output is then left in target/ to look at.
		var stdout = dir.resolve("prober-stdout.json");
		var stderr = dir.resolve("prober-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		// Both provisioned by the js-tests profile.  node_modules lives under target/, so it is gitignored and
		// RAT-excluded already; the browser path is separately overridable because CI needs it somewhere cacheable
		// that `mvn clean` will not delete.
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

	/** Reads a diagnostic file without letting a second failure mask the first. */
	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	private static Step catalogNoSubtab() { return steps.get(0); }

	private static Step catalogBundles() { return steps.get(1); }

	private static Step releases() { return steps.get(2); }

	//------------------------------------------------------------------------------------------------------------------
	// a: a sub-tabbed tab renders at all  (behavioural replacement for the panelMatches source-shape assertion)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_subtabbedTabRendersItsOuterPanelSubtabBarAndFirstSubpanel() {
		// The blank-tab defect in one assertion: with the tab named but no sub-tab named, the runtime resolves a
		// non-null subtabId, and an exact-match panel rule would leave the outer panel display:none - taking the
		// sub-tab bar and the sub-panel nested inside it down with it, however correctly they were classed.
		var s = catalogNoSubtab();
		assertTrue(s.visible("catalog", null), () -> "outer .jc-panel of the sub-tabbed tab is not rendered: " + s.raw());
		assertEquals(1, s.visibleSubtabBars(), () -> "the sub-tab bar must be reachable: " + s.raw());
		assertTrue(s.visible("catalog", "packages"), () -> "first sub-panel is not rendered: " + s.raw());
		assertEquals("catalog", s.activeTab());
		assertEquals("packages", s.activeSubtab(), "an unnamed sub-tab must fall back to the first");
	}

	@Test void a02_theOtherTabsPanelStaysHidden() {
		// The relaxation that fixes a01 must not leak across tabs.
		var s = catalogNoSubtab();
		assertFalse(s.visible("releases", null), () -> "another tab's panel leaked into view: " + s.raw());
	}

	@Test void a03_onlyTheActiveSubpanelIsRendered() {
		var s = catalogNoSubtab();
		assertFalse(s.visible("catalog", "bundles"), () -> "an inactive sub-panel is rendered: " + s.raw());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b: naming a sub-tab still narrows, and does so over hashchange
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_namingASubtabSwitchesWhichSubpanelRenders() {
		// Reached by assigning location.hash, so this also proves the hashchange listener re-applies the resolution
		// rather than the page only being correct on first load.
		var s = catalogBundles();
		assertTrue(s.visible("catalog", "bundles"), () -> "named sub-panel is not rendered: " + s.raw());
		assertFalse(s.visible("catalog", "packages"), () -> "previous sub-panel stayed visible: " + s.raw());
		assertTrue(s.visible("catalog", null), () -> "outer panel must stay visible for EVERY sub-tab: " + s.raw());
		assertEquals("bundles", s.activeSubtab());
	}

	@Test void b02_switchingToALeafTabHidesTheWholeSubtabbedSubtree() {
		var s = releases();
		assertTrue(s.visible("releases", null), () -> "leaf tab panel is not rendered: " + s.raw());
		assertFalse(s.visible("catalog", null), () -> "the sub-tabbed tab's outer panel stayed visible: " + s.raw());
		assertFalse(s.visible("catalog", "bundles"), () -> "a sub-panel stayed visible: " + s.raw());
		assertEquals(0, s.visibleSubtabBars(), () -> "a hidden tab's sub-tab bar stayed visible: " + s.raw());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c: lazy init is scoped to the panel that OWNS the table
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_aPanelOnlyInitsTheViewTablesItOwns() {
		// A sub-tabbed tab's outer panel encloses every sub-panel's table.  Without ownership scoping it would init
		// all of them the moment the tab opens - defeating lazy init and sizing columns inside hidden sub-panels.
		assertEquals(List.of("packages"), catalogNoSubtab().initedViews(),
			() -> "opening the tab must init only the active sub-panel's view: " + catalogNoSubtab().raw());
	}

	@Test void c02_initIsDeferredUntilASubtabIsActuallyActivated() {
		// initedViews accumulates across the whole browser session, so this shows `bundles` was inited only once its
		// own sub-panel became active - not earlier, when its ancestor panel did.
		assertEquals(List.of("packages", "bundles"), catalogBundles().initedViews(),
			() -> "expected bundles to init on activation, and not before: " + catalogBundles().raw());
	}

	@Test void c03_eachViewIsInitedAtMostOnce() {
		// Re-activation must go down the columns.adjust() path, not init a second time.
		var all = releases().initedViews();
		assertEquals(new HashSet<>(all).size(), all.size(), () -> "a view was inited twice: " + all);
		assertEquals(List.of("packages", "bundles", "releases"), all, () -> releases().raw().toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d: the runtime ran cleanly
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_noScriptErrorsAndNoFailLoudBanner() {
		// A green run above would be meaningless if the runtime had actually thrown, or had refused to init and
		// rendered its contract-version banner instead.
		for (var s : steps) {
			assertEquals(List.of(), s.jsFailures(), () -> "the runtime logged errors: " + s.jsFailures());
			assertEquals(List.of(), s.errorBanners(), () -> "the runtime refused to init: " + s.errorBanners());
		}
	}
}
