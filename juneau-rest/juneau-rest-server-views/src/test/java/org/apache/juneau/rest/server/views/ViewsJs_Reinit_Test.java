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
 * Always-on coverage for the destroy/reinit transaction and async {@code initTable} handshake: source-shape
 * pins on {@code juneau-views.js}/{@code juneau-config.js}/{@code juneau-pages.js}/{@code juneau-ribbon.js},
 * plus a Node behavioral check that {@code resolveOrder} uses the live {@code dtIndex} (selection + hidden
 * column → C is 3).
 *
 * <p>Chooser-dialog browser canaries are out of scope here (they land with the View-tab UI).
 */
class ViewsJs_Reinit_Test extends TestBase {

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		return resource(ViewsMixin.VIEWS_JS_RESOURCE);
	}

	private static String configJs() throws IOException {
		return resource(ViewsJs_ConfigPersistence_Test.CONFIG_JS_RESOURCE);
	}

	private static String pagesJs() throws IOException {
		return resource(ViewsMixin.PAGES_JS_RESOURCE);
	}

	private static String ribbonJs() throws IOException {
		return resource(ViewsMixin.RIBBON_JS_RESOURCE);
	}

	/** Balanced-brace extractor (nested functions / object literals would fool a plain {@code \\n\\t}} scan). */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var i = body.indexOf('{', start);
		assertTrue(i >= 0, () -> "'" + signature + "' has no opening brace:\n" + body);
		var depth = 0;
		var j = i;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) { if (body.charAt(j) == '\\') j++; j++; }
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '/') {
				while (j < body.length() && body.charAt(j) != '\n') j++;
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '*') {
				j = body.indexOf("*/", j) + 1;
			}
		}
		return body.substring(start, j);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Async initTable handshake
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_initTable_alwaysReturnsAThenable_andCoalescesInFlight() throws Exception {
		var fn = functionBody(viewsJs(), "function initTable(");
		assertTrue(fn.contains("dataset.juneauInitPending"), fn);
		assertTrue(fn.contains("table.__juneauInitPromise"), fn);
		assertTrue(fn.contains("if (table.__juneauInitPromise) return table.__juneauInitPromise"), fn);
		assertTrue(fn.contains("return Promise.resolve()"), fn);
		assertTrue(fn.contains("return p"), fn);
		assertTrue(fn.contains("delete table.dataset.juneauInitPending"), fn);
	}

	@Test void a02_beginInitTable_awaitsResolveActiveViewWhenColumnConfigPresent() throws Exception {
		var fn = functionBody(viewsJs(), "function beginInitTable(");
		assertTrue(fn.contains("viewDef.columnConfig"), fn);
		assertTrue(fn.contains("NS.config.resolveActiveView"), fn);
		assertTrue(fn.contains("buildTable(table, viewDef, effective, ctx)"), fn);
	}

	@Test void a03_pagesJs_activatePanelViews_awaitsInitTableThenAdjustsIffDataTable() throws Exception {
		var fn = functionBody(pagesJs(), "function activatePanelViews(");
		assertTrue(fn.contains("Promise.resolve(NS.init.initTable(t))"), fn);
		assertTrue(fn.contains("isDataTable(t)"), fn);
		assertTrue(fn.contains("columns.adjust()"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Teardown protocol + mutex + selection survival
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_buildTable_refusesWhileInFlightOrJob_noQueue() throws Exception {
		var fn = functionBody(viewsJs(), "function buildTable(");
		assertTrue(fn.contains("hasInFlightRow(table)"), fn);
		assertTrue(fn.contains("hasJobRow(table)"), fn);
		assertTrue(fn.contains("Finish the in-progress action first."), fn);
		assertFalse(fn.contains("run-when"), fn);
	}

	@Test void b02_teardown_clearsPollTimers_closesEveryJobSource_thenDestroys() throws Exception {
		var fn = functionBody(viewsJs(), "function teardownTable(");
		var clearIdx = fn.indexOf("clearInterval");
		var jobIdx = fn.indexOf("_jobSources");
		var destroyIdx = fn.indexOf("ctx.dataTable.destroy()");
		assertTrue(clearIdx >= 0, fn);
		assertTrue(jobIdx > clearIdx, fn);
		assertTrue(destroyIdx > jobIdx, fn);
		assertTrue(fn.contains("closeActionDialog(ctx)"), fn);
		assertTrue(fn.contains("closeRowActionMenus(table)"), fn);
		assertTrue(fn.contains("stripGeneratedDom(table)"), fn);
	}

	@Test void b03_reinitMutex_latestWins_neverConcurrentTeardown() throws Exception {
		var fn = functionBody(viewsJs(), "function buildTable(");
		assertTrue(fn.contains("ctx._reinitRunning"), fn);
		assertTrue(fn.contains("ctx._reinitPending"), fn);
		assertTrue(fn.contains("coalesced: true"), fn);
	}

	@Test void b04_selectionStateLivesOnCtx_notRebuilt() throws Exception {
		var fn = functionBody(viewsJs(), "function beginInitTable(");
		assertTrue(fn.contains("selected: new Set()"), fn);
		assertTrue(fn.contains("selectionState: selectionState"), fn);
		var assemble = functionBody(viewsJs(), "function assembleFullColumnArray(");
		assertTrue(assemble.contains("buildSelectionColumnDef(ctx.selectionState)"), assemble);
	}

	@Test void b05_jobSourcesTrackedAsSet_notLastSlot() throws Exception {
		var views = viewsJs();
		assertTrue(views.contains("ctx._jobSources = new Set()"), views);
		assertTrue(views.contains("ctx._jobSources.add(es)"), views);
		assertFalse(views.contains("ctx._jobSource = es"), views);
	}

	@Test void b06_restoreHeaderShell_recreatesSelectionThThenActions() throws Exception {
		var fn = functionBody(viewsJs(), "function restoreHeaderShell(");
		assertTrue(fn.contains("juneau-view-select-th"), fn);
		assertTrue(fn.contains("juneau-view-detail-th"), fn);
		assertTrue(fn.contains("appendActionsHeaderCell(table)"), fn);
		var strip = functionBody(viewsJs(), "function stripGeneratedDom(");
		assertTrue(strip.contains("juneau-view-toolbar-row"), strip);
		assertTrue(strip.contains("juneau-view-columnsearch-row"), strip);
		assertTrue(strip.contains("juneau-view-actions-th"), strip);
		assertTrue(strip.contains("juneau-view-select-th"), strip);
		assertTrue(strip.contains("juneau-view-detail-th"), strip);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) dtIndex rebind + export :visible + programmatic Apply
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_assembleFullColumnArray_computesColumnsFirstThenOrderViaDtIndex() throws Exception {
		var fn = functionBody(viewsJs(), "function assembleFullColumnArray(");
		assertTrue(fn.contains("cols.push(sel)"), fn);
		assertTrue(fn.contains("_juneau: \"actions\""), fn);
		assertTrue(fn.contains("opts.order = resolveOrder(viewDef, opts.columns)"), fn);
		assertFalse(fn.contains("unshift"), fn);
	}

	@Test void c02_buildColumnSearchRow_emitsOneThPerOptsColumn_hidesHidden() throws Exception {
		var fn = functionBody(viewsJs(), "function buildColumnSearchRow(");
		assertTrue(fn.contains("(optsColumns || []).forEach"), fn);
		assertTrue(fn.contains("col?.visible === false"), fn);
		assertTrue(fn.contains("th.style.display = \"none\""), fn);
		assertTrue(fn.contains("dt.column(idx).search"), fn);
	}

	@Test void c03_ribbon_exportOptionsVisible_onButtonsConstructor() throws Exception {
		var fn = functionBody(ribbonJs(), "function buildRibbon(");
		assertTrue(fn.contains("exportOptions: { columns: \":visible\" }"), fn);
		assertTrue(fn.contains("new $.fn.dataTable.Buttons(ctx.dataTable"), fn);
	}

	@Test void c04_ribbonToQueryParams_usesLiveOptsColumns() throws Exception {
		var fn = functionBody(ribbonJs(), "function ribbonToQueryParams(");
		assertTrue(fn.contains("optsColumns"), fn);
		assertTrue(fn.contains("optionParam(viewDef, a, optsColumns)"), fn);
	}

	@Test void c05_config_exportsApplyViewAndResolveActiveView() throws Exception {
		var body = configJs();
		assertTrue(body.contains("function applyView("), body);
		assertTrue(body.contains("function resolveActiveView("), body);
		assertTrue(body.contains("NS.config.applyView = applyView"), body);
		assertTrue(body.contains("NS.config.resolveActiveView = resolveActiveView"), body);
		assertTrue(body.contains("NS.init.buildTable"), body);
	}

	@Test void c06_listenersBoundOnceInBeginInit_notInBuildTable() throws Exception {
		var begin = functionBody(viewsJs(), "function beginInitTable(");
		assertTrue(begin.contains("initTableWidgets(table, ctx, viewDef)"), begin);
		assertTrue(begin.contains("initSelection(table, ctx)"), begin);
		var widgets = functionBody(viewsJs(), "function initTableWidgets(");
		assertTrue(widgets.contains("initDetailsExpander(table, ctx, viewDef)"), widgets);
		assertTrue(widgets.contains("initRowActions(table, viewDef, ctx)"), widgets);
		var construct = functionBody(viewsJs(), "function constructTable(");
		assertFalse(construct.contains("initDetailsExpander("), construct);
		assertFalse(construct.contains("initRowActions("), construct);
		assertFalse(construct.contains("initSelection("), construct);
		assertTrue(construct.contains("wireSelectionAndBulkToolbar("), construct);
		assertTrue(construct.contains("wireTablePolling("), construct);
		assertTrue(functionBody(viewsJs(), "function wireSelectionAndBulkToolbar(").contains("bindSelectionPrune("), construct);
		assertTrue(functionBody(viewsJs(), "function wireTablePolling(").contains("initPolling("), construct);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Behavioral — resolveOrder via live dtIndex (skipped when node is absent)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var configFile = Files.createTempFile("juneau-config-", ".js");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		try {
			Files.writeString(configFile, configJs(), UTF_8);
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			report = Json.to(runNode(harness, configFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(configFile);
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
			var p = Path.of(basedir, "src/test/js/config-reinit.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/config-reinit.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/config-reinit.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path configJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("config-reinit-stdout-", ".json");
		var stderr = Files.createTempFile("config-reinit-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of(
					"node", harness.toString(), configJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("config-reinit.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("config-reinit.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or config-reinit.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void d01_resolveOrder_usesDtIndex_Cis3_not2() {
		var r = report();
		assertEquals(true, r.get("hasDtIndex"));
		assertEquals(true, r.get("hasBuildTable"));
		assertEquals(true, r.get("hasApplyView"));
		assertEquals(3, ((Number)r.get("dtIndexC")).intValue());
		assertEquals(3, ((Number)r.get("orderIndex")).intValue());
		assertEquals("asc", r.get("orderDir"));
	}

	@Test void d02_hiddenOrderedColumn_fallsBackToFirstVisibleOrderable() {
		var r = report();
		assertEquals("A", r.get("hiddenFallbackData"));
		assertEquals(1, ((Number)r.get("hiddenFallbackIndex")).intValue());
	}

	@Test void d03_applyView_withoutCtx_returnsNotInitialized() {
		var r = report();
		var v = (Map<?,?>)r.get("applyViewNotInit");
		assertEquals(false, v.get("ok"));
		assertEquals("not-initialized", v.get("reason"));
	}
}
