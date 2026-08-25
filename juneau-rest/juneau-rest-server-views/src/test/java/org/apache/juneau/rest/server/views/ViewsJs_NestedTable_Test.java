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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the nested table JS helpers (a table inside a row-detail section, scoped to its parent row,
 * capped at {@link NestedTableDef#MAX_DEPTH}).  Source-shape always runs; the behavioral Node harness
 * ({@code nested-table.cjs}) runs when {@code node} is on {@code PATH} (skipped otherwise — no {@code -Pjs-tests}
 * required).
 */
class ViewsJs_NestedTable_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"JUNEAU_NESTED_CONTRACT_VERSION: JUNEAU_NESTED_CONTRACT_VERSION",
			"applyNestedScope: applyNestedScope",
			"findNestedSidecar: findNestedSidecar",
			"prepareNestedTable: prepareNestedTable",
			"mintNestedIdentity: mintNestedIdentity",
			"nestedTableDepth: nestedTableDepth",
			"MAX_NESTED_DEPTH: MAX_NESTED_DEPTH",
			"adjustNestedColumns: adjustNestedColumns",
			"activateNestedTablesInPane: activateNestedTablesInPane",
			"initNestedTablesInVisiblePanes: initNestedTablesInVisiblePanes",
			"teardownNestedTables: teardownNestedTables",
			"isOwnTableEvent: isOwnTableEvent",
			"ownRowsWithId: ownRowsWithId"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		assertTrue(body.contains("NS.NESTED_CONTRACT_VERSION = JUNEAU_NESTED_CONTRACT_VERSION"),
			"nested contract version must be published on the JuneauViews namespace");
	}

	/**
	 * Per-widget versioning: the nested shell's version is declared in BOTH tiers and must move in lockstep, while
	 * the view / row-detail / bulk contracts are untouched by a nested-shell revision.
	 */
	@Test void a03_contractVersion_lockstepWithNestedTableDef() throws Exception {
		var body = viewsJs();
		assertEquals("2", NestedTableDef.CONTRACT_VERSION);
		assertTrue(body.contains("const JUNEAU_NESTED_CONTRACT_VERSION = \"" + NestedTableDef.CONTRACT_VERSION + "\""),
			"the juneau-views.js literal must match NestedTableDef.CONTRACT_VERSION");
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertTrue(body.contains("const JUNEAU_VIEW_CONTRACT_VERSION = \"" + ViewDef.CONTRACT_VERSION + "\""), body);
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
		assertTrue(body.contains("const JUNEAU_ROW_DETAIL_CONTRACT_VERSION = \"" + RowDetailDef.CONTRACT_VERSION + "\""),
			body);
	}

	/** The depth cap is one number, declared on both tiers. */
	@Test void a04_maxDepth_lockstepWithNestedTableDef() throws Exception {
		var body = viewsJs();
		assertEquals(2, NestedTableDef.MAX_DEPTH);
		assertTrue(body.contains("const MAX_NESTED_DEPTH = " + NestedTableDef.MAX_DEPTH + ";"),
			"the juneau-views.js depth cap must match NestedTableDef.MAX_DEPTH");
	}

	/**
	 * The clamp is now parent-only: withholding {@code rowActions}/{@code details} here would make the server's
	 * depth-2 widening a no-op, while {@code columnConfig}/{@code pollIntervalMs} must stay withheld.
	 */
	@Test void a05_prepareNestedTable_clampIsParentOnly_andRunsTheParentInitPath() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function prepareNestedTable(");
		assertTrue(fn.contains("viewDef.columnConfig = null;"), fn);
		assertTrue(fn.contains("viewDef.pollIntervalMs = null;"), fn);
		assertFalse(fn.contains("viewDef.rowActions = null;"),
			"a depth-2 nested table keeps its declared row actions");
		assertFalse(fn.contains("viewDef.details = null;"),
			"a depth-2 nested table keeps its declared detail sections");
		assertFalse(body.contains("Read-only is enforced defensively here"),
			"the stale read-only javadoc must be gone from the nested runtime");
		// The same init path a root table runs (see beginInitTable) - minus the bulk branch, which stays on the parent.
		assertTrue(fn.contains("initDetailsExpander(table, ctx, viewDef)"), fn);
		assertTrue(fn.contains("initCellPopover(table, ctx, viewDef)"), fn);
		assertTrue(fn.contains("initRowActions(table, viewDef, ctx)"), fn);
		assertTrue(fn.contains("initSelection(table, ctx)"), fn);
		assertTrue(fn.contains("selectionState: selectionState"), "ctx.selectionState must be live, not null: " + fn);
		assertFalse(fn.contains("readBulkDef"), "bulk mutation is parent-table only: " + fn);
		assertFalse(fn.contains("mountChooser"), "the column chooser is parent-table only: " + fn);
	}

	/** The source text of one top-level {@code function} declaration, up to its closing brace. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found");
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	@Test void a02_parentDtHandlersGuarded() throws Exception {
		var body = viewsJs();
		// Every parent .dt handler must ignore a nested table's event bubbling up (e.target === parent-table guard).
		assertTrue(body.contains("ctx.dataTable.on(\"draw.dt\", function (e) { if (e && e.target !== ctx.table) { return; } refreshPillState(); })"),
			"paging pill draw.dt must be guarded");
		assertTrue(body.contains("ctx.dataTable.on(\"preDraw.dt\", function (e) {"),
			"parent must tear down nested tables on preDraw.dt (while child rows still exist)");
		assertTrue(body.contains("teardownNestedTables(table);"),
			"parent preDraw.dt must call teardownNestedTables");
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			report = Json.to(runNode(harness, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/nested-table.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/nested-table.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/nested-table.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("nested-table-stdout-", ".json");
		var stderr = Files.createTempFile("nested-table-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("nested-table.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("nested-table.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or nested-table.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_helpersPresent_andContractVersion() {
		var r = report();
		assertEquals(NestedTableDef.CONTRACT_VERSION, r.get("nestedContractVersion"));
		assertEquals(NestedTableDef.MAX_DEPTH, ((Number)r.get("maxNestedDepth")).intValue());
		assertEquals(true, r.get("hasApplyNestedScope"));
		assertEquals(true, r.get("hasFindNestedSidecar"));
		assertEquals(true, r.get("hasActivate"));
		assertEquals(true, r.get("hasInitVisible"));
		assertEquals(true, r.get("hasTeardown"));
		assertEquals(true, r.get("hasMint"));
		assertEquals(true, r.get("hasDepth"));
	}

	@Test void b02_applyNestedScope_bothBranches() {
		var r = report();
		assertEquals(true, r.get("scope_server_ribbonKept"));   // pre-existing (ribbon) data fn preserved
		assertEquals(true, r.get("scope_server_paramAdded"));
		assertEquals(true, r.get("scope_client_paramAdded"));
		assertEquals(true, r.get("scope_blank_absent"));        // blank parent id -> no param
		assertEquals("a1", r.get("scope_getter_first"));
		assertEquals("b2", r.get("scope_getter_second"));       // getter read at request time
		assertEquals(true, r.get("scope_customName"));
	}

	@Test void b03_buildOptions_scopesBothAjaxModes() {
		var r = report();
		assertEquals(true, r.get("bo_server_serverSide"));
		assertEquals(true, r.get("bo_server_ribbon"));
		assertEquals(true, r.get("bo_server_scope"));
		assertEquals(true, r.get("bo_client_serverSide"));      // false
		assertEquals(true, r.get("bo_client_dataSrc"));
		assertEquals(true, r.get("bo_client_scope"));
		assertEquals(true, r.get("bo_top_client_noDataFn"));    // no nestedScope -> client keeps no data fn
	}

	@Test void b04_findNestedSidecar_siblingByAuthorId() {
		var r = report();
		assertEquals(true, r.get("find_byId"));
		assertEquals(true, r.get("find_fallbackFirst"));
		assertEquals(true, r.get("find_noneNull"));
	}

	@Test void b05_prepareNestedTable_failClosedHandshakes() {
		var r = report();
		assertEquals(true, r.get("pnt_contractMismatch_banner"));
		assertEquals(true, r.get("pnt_contractMismatch_noInit"));
		assertEquals(true, r.get("pnt_contractMismatch_noCtx"));
		assertEquals(true, r.get("pnt_malformed_banner"));
		assertEquals(true, r.get("pnt_malformed_noInit"));
		assertEquals(true, r.get("pnt_viewContract_banner"));
		assertEquals(true, r.get("pnt_viewContract_noInit"));
		assertEquals(true, r.get("pnt_noSidecar_noInit"));
		assertEquals(true, r.get("pnt_noSidecar_noBanner"));
		assertEquals(true, r.get("pnt_noJq_noInit"));
		assertEquals(true, r.get("pnt_noJq_noCtx"));
	}

	@Test void b06_prepareNestedTable_stampsClampsAndScopes() {
		var r = report();
		assertEquals(true, r.get("pnt_ok_construction_attempted"));
		assertEquals(true, r.get("pnt_ok_initMarked"));
		assertEquals(true, r.get("pnt_ok_parentStamped"));
		assertEquals(true, r.get("pnt_ok_hasCtx"));
		assertEquals(true, r.get("pnt_ok_nested"));
		assertEquals(2, ((Number)r.get("pnt_ok_depth")).intValue());
		assertEquals("alertId", r.get("pnt_ok_scopeParam"));
		assertEquals("a1", r.get("pnt_ok_scopeReadsAttr"));
		assertEquals(true, r.get("pnt_ok_clampColumnConfig"));
		assertEquals(true, r.get("pnt_ok_clampPoll"));
		assertEquals(true, r.get("pnt_ok_idempotent"));
	}

	@Test void b06a_depthTwoIsAFullView_rowActionsDetailsAndLiveSelection() {
		var r = report();
		assertEquals(true, r.get("pnt_ok_keepsRowActions"), "rowActions must survive the depth-2 clamp");
		assertEquals(true, r.get("pnt_ok_keepsDetails"), "details must survive the depth-2 clamp");
		assertEquals(true, r.get("pnt_ok_selectionLive"), "ctx.selectionState must be live, not hardcoded null");
		assertEquals("id", r.get("pnt_ok_selectionRowIdField"));
		assertEquals(true, r.get("pnt_ok_selectionEmpty"));
		assertEquals(true, r.get("pnt_ok_noBulkDef"), "bulk mutation stays on the enclosing table");
		// The parent init path: details expander + cell popover + row actions on click, row actions on keydown,
		// selection on change.
		assertEquals(3, ((Number)r.get("pnt_ok_clickListeners")).intValue());
		assertEquals(1, ((Number)r.get("pnt_ok_keydownListeners")).intValue());
		assertEquals(1, ((Number)r.get("pnt_ok_changeListeners")).intValue());
		assertEquals(true, r.get("pnt_ok_popoverBound"));
		assertEquals(true, r.get("pnt_ok_detailInflight"));
		// A nested table with no selection stamp gets none of it.
		assertEquals(true, r.get("pnt_noSel_selectionNull"));
		assertEquals(true, r.get("pnt_noSel_noChangeListener"));
	}

	@Test void b06b_depthCap_twoInstantiates_threeIsRefused() {
		var r = report();
		assertEquals(true, r.get("depth_loneWrapperIsTwo"));
		assertEquals(true, r.get("depth_nestedInNestedIsThree"));
		assertEquals(true, r.get("depth_threeRefused_banner"));
		assertEquals(true, r.get("depth_threeRefused_noInit"));
		assertEquals(true, r.get("depth_threeRefused_noCtx"));
	}

	@Test void b06c_mintedIdentity_isPerExpandedRow() {
		var r = report();
		assertEquals("events:a1:2", r.get("mint_tableIdA"));
		assertEquals("events:a2:2", r.get("mint_tableIdB"));
		assertEquals(true, r.get("mint_tableIdsUnique"));
		assertEquals("juneau-view:events:a1:2", r.get("mint_sidecarIdA"));
		assertEquals(true, r.get("mint_sidecarIdsUnique"));
		assertEquals(true, r.get("mint_sidecarNotBarePageId"),
			"a nested sidecar must never shadow a page-level juneau-view:<id> lookup");
		assertEquals(true, r.get("mint_authorIdKept"));
	}

	@Test void b06d_twoRowsExpandedSimultaneously_bothInit_andRedrawIsIsolated() {
		var r = report();
		assertEquals(true, r.get("sim_bothInited"));
		assertEquals(true, r.get("sim_distinctCtx"));
		assertEquals(true, r.get("sim_distinctViewDefs"));
		assertEquals("a1", r.get("sim_scope1"));
		assertEquals("a2", r.get("sim_scope2"));
		assertEquals(true, r.get("sim_redrawIsolated"), "paging one nested table must not redraw the other");
	}

	@Test void b06e_ownership_parentNeverClaimsANestedTablesRowsOrEvents() {
		var r = report();
		assertEquals(true, r.get("own_parentRowsExcludeNested"));
		assertEquals(true, r.get("own_nestedRowsAreItsOwn"));
		assertEquals(true, r.get("own_owningTableOfNestedNode"));
		assertEquals(true, r.get("own_parentIgnoresNestedEvent"));
		assertEquals(true, r.get("own_nestedClaimsItsOwnEvent"));
		assertEquals(true, r.get("own_parentClaimsItsOwnEvent"));
		assertEquals(true, r.get("own_parentTemplateNotTheNestedOne"));
		assertEquals(true, r.get("own_nestedTemplateIsItsOwn"));
	}

	@Test void b07_teardownNestedTables_destroysAndClears() {
		var r = report();
		assertEquals(true, r.get("td_destroyed1"));
		assertEquals(true, r.get("td_destroyed2"));
		assertEquals(true, r.get("td_markerCleared1"));
		assertEquals(true, r.get("td_ctxNulled1"));
		assertEquals(true, r.get("td_plainUntouched"));
		assertEquals(true, r.get("td_timersCleared"), "no poll timer may outlive teardown");
		assertEquals(true, r.get("td_streamsClosed"), "no job stream may outlive teardown");
	}

	@Test void b07a_teardownIsDepthFirst() {
		var r = report();
		assertEquals("inner,outer", r.get("td_depthFirstOrder"),
			"a table inside another table's open panel must be destroyed first");
		assertEquals(true, r.get("td_depthFirstBothDestroyed"));
	}

	@Test void b08_paneRouting_visibleInits_hiddenDefers_reshowAdjusts() {
		var r = report();
		assertEquals(true, r.get("route_visibleInvoked"));
		assertEquals(true, r.get("route_hiddenSkipped"));
		assertEquals(true, r.get("route_activateHiddenNow"));
		assertEquals(true, r.get("route_adjustCalled"));
		assertEquals(true, r.get("route_adjustNoBanner"));
	}
}
