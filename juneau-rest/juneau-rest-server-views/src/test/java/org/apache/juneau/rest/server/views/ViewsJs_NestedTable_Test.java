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
 * Always-on coverage for the nested read-only table JS helpers (a table inside a row-detail section, scoped to its
 * parent row).  Source-shape always runs; the behavioral Node harness ({@code nested-table.cjs}) runs when
 * {@code node} is on {@code PATH} (skipped otherwise — no {@code -Pjs-tests} required).
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
			"adjustNestedColumns: adjustNestedColumns",
			"activateNestedTablesInPane: activateNestedTablesInPane",
			"initNestedTablesInVisiblePanes: initNestedTablesInVisiblePanes",
			"teardownNestedTables: teardownNestedTables"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		assertTrue(body.contains("const JUNEAU_NESTED_CONTRACT_VERSION = \"1\""),
			"nested contract version const must be declared as \"1\"");
		assertTrue(body.contains("NS.NESTED_CONTRACT_VERSION = JUNEAU_NESTED_CONTRACT_VERSION"),
			"nested contract version must be published on the JuneauViews namespace");
	}

	@Test void a02_parentDtHandlersGuarded() throws Exception {
		var body = viewsJs();
		// Every parent .dt handler must ignore a nested table's event bubbling up (e.target === parent-table guard).
		assertTrue(body.contains("ctx.dataTable.on(\"draw.dt\", function (e) { if (e && e.target !== ctx.table) return; refreshPillState(); })"),
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
		assertEquals("1", r.get("nestedContractVersion"));
		assertEquals(true, r.get("hasApplyNestedScope"));
		assertEquals(true, r.get("hasFindNestedSidecar"));
		assertEquals(true, r.get("hasActivate"));
		assertEquals(true, r.get("hasInitVisible"));
		assertEquals(true, r.get("hasTeardown"));
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
		assertEquals("alertId", r.get("pnt_ok_scopeParam"));
		assertEquals("a1", r.get("pnt_ok_scopeReadsAttr"));
		assertEquals(true, r.get("pnt_ok_clampRowActions"));
		assertEquals(true, r.get("pnt_ok_clampColumnConfig"));
		assertEquals(true, r.get("pnt_ok_clampPoll"));
		assertEquals(true, r.get("pnt_ok_clampDetails"));
		assertEquals(true, r.get("pnt_ok_idempotent"));
	}

	@Test void b07_teardownNestedTables_destroysAndClears() {
		var r = report();
		assertEquals(true, r.get("td_destroyed1"));
		assertEquals(true, r.get("td_destroyed2"));
		assertEquals(true, r.get("td_markerCleared1"));
		assertEquals(true, r.get("td_ctxNulled1"));
		assertEquals(true, r.get("td_plainUntouched"));
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
