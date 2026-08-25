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
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The card-chrome half of the module's <b>JavaScript-execution harness</b>: runs the REAL served runtimes against the
 * REAL server-emitted card markup in a real headless browser, and asserts the two things only layout can prove &mdash;
 * that a card's action menu escapes a scrolled card grid, and that a card-hosted view table changing page length or
 * overflowing horizontally never resizes the card or the grid around it.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape and Node coverage):</h5>
 * <p>
 * {@link ViewsJs_CardActions_Test} proves the shipped scripts wire a synthetic DOM and route a menu through the shared
 * layer stack; neither it nor any Java emit test can prove the menu actually escapes a real overflow-clipping grid, or
 * that a hosted table's growth is absorbed by its own scroll region, because clipping and reflow only exist where
 * layout does.  The fixture body is real {@link CardGridTable} output and the scripts and stylesheet are the real
 * served assets, so the shell under test is the shell that ships.
 *
 * <p>
 * The opt-in profile provisions Playwright only, so there is no jQuery/DataTables here: the prober synthesizes the row
 * sets a page change would draw and drives the runtime's own exposed overflow helper, exactly as this module's other
 * canaries do.  What is measured is therefore the layout contract around the hosted table, not DataTables itself.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code card-actions-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed to add this canary.
 */
@EnabledIfSystemProperty(named=CardActions_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class CardActions_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static final String TOKEN = "tok-123";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** The view a card hosts: sortable columns and its own data path (a hosted table brings no card refresh wire). */
	private static ViewDef hosted() {
		return ViewDef.create("orders")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/orders")
			.columns(Column.of("ref").title("Ref"), Column.of("total").title("Total"))
			.build();
	}

	/** The action catalog both cards declare - deliberately the SAME ids, so a collision would show up as one menu. */
	private static HeaderAction[] actions() {
		return new HeaderAction[]{
			HeaderAction.link("open", "external", "Open report", "/reports/1"),
			HeaderAction.safe("pin", "pin", "Pin card", "card-pin"),
			HeaderAction.menu("more", "overflow", "More actions").menu(
				MenuItem.safe("export", "Export", "card-export"),
				MenuItem.divider(),
				MenuItem.link("docs", "Docs", "/docs"))
		};
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("card-actions-browser.cjs");

		// A request-aware grid, assembled by walking cards - the only path that can host a view, and the path that
		// qualifies each hosted table's identity by grid + card.
		var grid = CardGrid.create("g1").cards(
			Card.create("c1", "Orders").body(ViewCardBody.of(hosted())).actions(actions()),
			Card.create("c2", "Summary").body(
				CardFieldList.create().fields(CardField.of("open", "Open", "12"))).actions(actions()));

		var body = Html.of(CardGridTable.ofGrid(tokenRequest(), grid));
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			// The icon registry is deliberately absent: it fetches its sprite relative to the page, which a file://
			// origin forbids.  Icon hydration is covered by the always-on Node layer; this canary is about layout.
			+ "\n</style></head><body>\n" + body + "\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.CHROME_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.CARDS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("card-actions.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	/** A request carrying the boundary-stamped CSRF token (the auto-embed entry point of the token contract). */
	private static MockServletRequest tokenRequest() {
		return MockServletRequest.create().attribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("card-actions-stdout.json");
		var stderr = dir.resolve("card-actions-stderr.txt");
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
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> section(String name) {
		var v = report.get(name);
		assertInstanceOf(Map.class, v, () -> "prober did not report '" + name + "': " + report);
		return (Map<?,?>)v;
	}

	private static long num(Object v) {
		assertInstanceOf(Number.class, v, () -> "expected a number, got: " + v);
		return ((Number)v).longValue();
	}

	@Test void a01_pageLoadedCleanlyWithAllThreeRuntimes() {
		assertEquals(true, report.get("hasViews"));
		assertEquals(true, report.get("hasCards"));
		assertEquals(true, report.get("hasChrome"));
		assertEquals(true, report.get("gridFound"));
		assertEquals(true, report.get("cardsFound"));
		assertEquals(List.of(), report.get("jsFailures"), "the served runtimes must load with no page errors");
	}

	@Test void a02_servedShellCarriesCardScopedActionAndTableIdentity() {
		var s = section("served");
		assertEquals(3L, num(s.get("actionsInHostCard")));
		assertEquals(List.of("link", "safe", "menu"), s.get("behaviors"));
		assertEquals("juneau-menu:g1:c1:more", s.get("menuIdHostCard"));
		assertEquals("juneau-menu:g1:c2:more", s.get("menuIdFieldCard"));
		assertEquals("g1:c1:orders", s.get("hostedTableId"));
		assertEquals("orders", s.get("hostedTableMarker"));   // the marker stays the AUTHOR's view id
		assertEquals(true, s.get("hostedSidecarInsideCard"));
		assertNull(s.get("hostCardRefreshWire"), "a hosted table brings its own data path, not the card envelope");
	}

	@Test void a03_theCardsRuntimeEnhancedTheActionsAtBootstrap() {
		var s = section("served");
		assertEquals("1", s.get("menuWiredAtBootstrap"), "the cards runtime is the enhancement owner for a card");
		assertEquals(true, s.get("iconHostPresent"));
	}

	@Test void a04_actionMenuIsClipFreeInsideAScrolledCardGrid() {
		var m = section("menu");
		assertEquals(true, m.get("opened"));
		assertEquals(true, m.get("onLayerStack"));
		assertEquals("menu", m.get("layerKind"));
		assertEquals(true, m.get("portalledToBody"));         // the shipped portal contract, consumed
		assertEquals(true, m.get("positionFixed"));
		assertEquals(true, m.get("escapedScrollBox"));        // the whole point: no clipping by the scrolled grid
		assertEquals(true, m.get("escapedCard"));
		assertEquals(true, m.get("displayed"));
		assertEquals(true, m.get("withinViewportX"));
		assertEquals(true, m.get("withinViewportY"));
		assertEquals("true", m.get("ariaExpanded"));
		assertEquals(0L, num(m.get("dialogCount")), "a menu must not inflate the dialog-kind depth cap");
	}

	@Test void a05_escapeUnwindsThroughTheSharedStack() {
		var m = section("menu");
		assertEquals(true, m.get("closedOnEscape"));
		assertEquals("false", m.get("ariaResetOnEscape"));
	}

	@Test void a06_twoCardsWithTheSameActionIdOpenTheirOwnMenus() {
		var p = section("perCardMenus");
		assertEquals(true, p.get("distinctListIds"));
		assertEquals(true, p.get("openedTheSecondCardsList"));
		assertEquals(true, p.get("firstCardStillCollapsed"));
	}

	@Test void a07_hostedTablePagesInsideItsCardWithoutResizingTheGrid() {
		var l = section("layout");
		assertEquals(3L, num(l.get("rowsDrawn")), "the smaller page really did redraw");
		assertEquals(true, l.get("grewTallerWithRows"));       // the draws are real, so the width claims are not vacuous
		assertEquals(true, l.get("shrankBackOnSmallerPage"));
		assertEquals(true, l.get("gridWidthStableOnPage"));
		assertEquals(true, l.get("cardWidthStableOnPage"));
		assertEquals(true, l.get("gridWidthStableOnSort"));
	}

	@Test void a08_horizontalOverflowIsAbsorbedByTheTablesOwnScrollRegion() {
		var l = section("layout");
		assertEquals(true, l.get("gridWidthStableOnWideRow"));
		assertEquals(true, l.get("scrollWrapIsTheRuntimes"));
		assertEquals(true, l.get("scrollWrapInsideCardBody"));
		assertEquals(true, l.get("tableOverflowsItsScrollRegion"));
	}
}
