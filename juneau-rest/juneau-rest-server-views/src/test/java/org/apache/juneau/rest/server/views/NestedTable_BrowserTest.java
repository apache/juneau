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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The depth-2 nested-table half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} against the REAL server-emitted shell in a real headless browser, and asserts a nested
 * table behaves as a user would experience it &mdash; its row-action menu opens through the shared popup layer stack
 * unclipped by a scrolled detail panel, its selection round-trips without reaching the enclosing table's selection,
 * and it is served with no column chooser and no bulk toolbar of its own.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape and Node coverage):</h5>
 * <p>
 * {@link ViewsJs_NestedTable_Test} proves the shipped script contains the depth-2 init path and the Node harness
 * proves it wires a synthetic DOM; neither can prove the menu actually escapes a real overflow-clipping detail panel,
 * because clipping only exists where layout does.  The fixture body is
 * {@link ViewTable#of(jakarta.servlet.http.HttpServletRequest,ViewDef) ViewTable.of(request, viewDef)} output, so the
 * shell under test is the shell that ships.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code nested-table-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed to add this canary.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link NestedPopup_BrowserTest} &mdash; the shared layer-stack canary this class consumes.
 * </ul>
 */
@EnabledIfSystemProperty(named=NestedTable_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class NestedTable_BrowserTest extends TestBase {

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

	/** A depth-2 nested view: its own columns, its own row action, and its own selection. */
	private static NestedTableDef nested() {
		return NestedTableDef.create(ViewDef.create("events")
				.dataMode(DataMode.CLIENT)
				.dataUrl("/data/events")
				.columns(Column.of("when").title("When"))
				.rowActions(RowAction.create("ack").label("Acknowledge").endpoint("/data/events/{id}/ack")
					.method(RowAction.Method.POST))
				.build())
			.parentScopeParam("alertId")
			.selection(SelectionDef.create("id"));
	}

	/** The enclosing view, whose detail section hosts the nested view. */
	private static ViewDef view() {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("related", "Related events")
					.fields(DetailField.of("owner").title("Owner"))
					.table(nested())))
			.build();
	}

	/**
	 * A ROOT table on the same page sharing the nested view's author id.  {@code data-juneau-view} is the author's
	 * {@code ViewDef.id}, so this collision is legal and must not cross-wire: the page sibling keeps the minted
	 * page-level sidecar id, and the nested clone answers only a panel-scoped lookup.
	 */
	private static ViewDef pageSibling() {
		return ViewDef.create("events")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/events")
			.columns(Column.of("when").title("When"))
			.build();
	}

	/** Bulk mutation on the ENCLOSING table only - the fact the nested shell must not mirror. */
	private static BulkMutateDef parentBulk() {
		return BulkMutateDef.create(WritePermit.forCapability("alerts:close"), SelectionDef.create("id"))
			.actions(RowAction.create("close").label("Close").endpoint("/data/alerts/bulk-close")
				.method(RowAction.Method.POST));
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("nested-table-browser.cjs");

		// The fixture restates nothing under test: the body is the real emitted shell and the script is the real
		// served runtime.  No jQuery/DataTables is needed - the prober drives the runtime's exposed DOM helpers and
		// supplies the body rows DataTables would have drawn.
		var page = Html.of(ViewTable.of(tokenRequest(), view(), List.of(), parentBulk()))
			+ "\n" + Html.of(ViewTable.of(tokenRequest(), pageSibling()));
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n" + page + "\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("nested-table.html");
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
		var stdout = dir.resolve("nested-table-stdout.json");
		var stderr = dir.resolve("nested-table-stderr.txt");
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

	@SuppressWarnings("unchecked")
	private static Map<String,Object> block(String key) {
		var v = (Map<String,Object>) report.get(key);
		assertNotNull(v, () -> "prober did not report '" + key + "': " + report);
		return v;
	}

	private static int intOf(Map<String,Object> m, String key) {
		var v = m.get(key);
		assertInstanceOf(Number.class, v, () -> key + " missing from " + m);
		return ((Number)v).intValue();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The runtime loaded against the real emitted shell
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedAgainstTheRealEmittedShell() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not load: " + report);
		assertEquals(Boolean.TRUE, report.get("parentTableFound"), report::toString);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void a02_servedNestedShell_carriesTheEnclosingResponsesTokenAndItsOwnSelection() {
		var s = block("served");
		assertEquals(Boolean.TRUE, s.get("templateFound"), s::toString);
		assertEquals(Boolean.TRUE, s.get("wrapFound"), s::toString);
		assertEquals(NestedTableDef.CONTRACT_VERSION, s.get("nestedContract"), s::toString);
		assertEquals("events", s.get("nestedViewId"), s::toString);
		assertEquals(TOKEN, s.get("token"), () -> "the nested table must ride the enclosing response's token: " + s);
		assertEquals(Boolean.TRUE, s.get("selectStamped"), s::toString);
		assertEquals("id", s.get("rowIdField"), s::toString);
	}

	@Test void a03_servedNestedShell_hasNoChooserHostAndNoBulkSidecarOfItsOwn() {
		var s = block("served");
		assertEquals(0, intOf(s, "nestedChooserHosts"), () -> "the column chooser is parent-table only: " + s);
		assertEquals(Boolean.FALSE, s.get("nestedBulkSidecar"), () -> "bulk mutation is parent-table only: " + s);
		assertEquals(Boolean.TRUE, s.get("parentBulkSidecar"), () -> "the enclosing table keeps its bulk sidecar: " + s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) A nested row-action menu opens through the shared layer stack, unclipped by a scrolled detail panel
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_nestedRowActions_surviveIntoTheRuntimeAndOpenAMenu() {
		var m = block("menu");
		assertEquals(1, intOf(m, "shippedRowActions"), () -> "the depth-2 clamp withheld the row actions: " + m);
		assertEquals(Boolean.TRUE, m.get("opened"), m::toString);
		assertEquals(Boolean.TRUE, m.get("onLayerStack"), () -> "the menu must register on the shared layer stack: " + m);
	}

	@Test void b02_nestedMenu_isPortalledFixedAndClipFreeInsideAScrolledDetailPanel() {
		var m = block("menu");
		assertEquals(Boolean.TRUE, m.get("portalledToBody"), m::toString);
		assertEquals(Boolean.TRUE, m.get("positionFixed"), m::toString);
		assertEquals(Boolean.TRUE, m.get("escapedScrollBox"), () -> "the menu is still inside the clipping panel: " + m);
		assertEquals(Boolean.TRUE, m.get("withinViewportX"), m::toString);
		assertEquals(Boolean.TRUE, m.get("withinViewportY"), m::toString);
	}

	@Test void b03_nestedMenu_unwindsThroughTheSharedStack() {
		assertEquals(Boolean.TRUE, block("menu").get("closedOnEscape"), report::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) A nested selection round-trip, isolated from the enclosing table's selection
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_nestedSelection_roundTrips() {
		var s = block("selection");
		assertEquals(Boolean.TRUE, s.get("hasSelection"), s::toString);
		assertEquals("id", s.get("rowIdField"), s::toString);
		assertEquals(List.of("E-1"), s.get("afterCheck"), s::toString);
		assertEquals(0, intOf(s, "afterUncheck"), s::toString);
	}

	@Test void c02_nestedSelection_neverReachesTheEnclosingTablesSelection() {
		var s = block("selection");
		assertEquals(Boolean.TRUE, s.get("parentUntouched"), () -> "a nested check leaked into the parent: " + s);
		assertEquals(2, intOf(s, "nestedOwnRows"), s::toString);
		assertEquals(1, intOf(s, "parentOwnRows"), s::toString);
		// The guard is load-bearing: an unguarded descendant query sweeps the nested table's rows in as well.
		assertEquals(3, intOf(s, "bareDescendantRows"), s::toString);
		assertEquals(Boolean.TRUE, s.get("parentOwnRowsExcludeNested"), s::toString);
	}

	@Test void c03_openPanel_showsNoNestedChooserAndNoNestedBulkToolbar() {
		var c = block("parentOnlyChrome");
		assertEquals(0, intOf(c, "chooserHosts"), c::toString);
		assertEquals(0, intOf(c, "chooserTriggers"), c::toString);
		assertEquals(0, intOf(c, "bulkToolbars"), c::toString);
		assertEquals(0, intOf(c, "selectAllCheckboxes"),
			() -> "select-all is bulk chrome; a nested table selects rows without it: " + c);
		assertEquals(Boolean.TRUE, c.get("bulkDefOnNested"), c::toString);
		assertEquals(Boolean.TRUE, c.get("bulkDefOnParent"), c::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Two rows open at once cannot cross-wire through a bare page-level id
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_simultaneousExpands_mintDistinctDomIdentity() {
		var i = block("identity");
		assertEquals("events:A-3:2", i.get("idA"), i::toString);
		assertEquals("events:A-4:2", i.get("idB"), i::toString);
		assertEquals(Boolean.TRUE, i.get("unique"), i::toString);
		assertEquals(Boolean.FALSE, i.get("barePageSidecarIsAClone"),
			() -> "a nested sidecar must never answer a page-level juneau-view:<id> lookup: " + i);
		assertEquals(Boolean.TRUE, i.get("authorIdKept"), i::toString);
	}

	@Test void d02_aRootTableSharingTheAuthorId_doesNotCrossWire() {
		var p = block("pageSibling");
		assertEquals(Boolean.TRUE, p.get("found"), p::toString);
		assertEquals(Boolean.TRUE, p.get("pageSidecarIsTheRootOne"),
			() -> "the nested clone shadowed the page-level sidecar id: " + p);
		assertEquals(Boolean.TRUE, p.get("nestedSidecarIsItsOwn"),
			() -> "a panel-scoped lookup resolved to the page sibling's sidecar: " + p);
		assertEquals(Boolean.TRUE, p.get("nestedTableIsNotThePageSibling"), p::toString);
		assertEquals(Boolean.TRUE, p.get("rowOwnerIsNested"), p::toString);
		assertEquals(1, intOf(p, "pageSiblingOwnRows"), p::toString);
		assertEquals(Boolean.TRUE, p.get("nestedRowsExcludedFromPageSibling"), p::toString);
	}
}
