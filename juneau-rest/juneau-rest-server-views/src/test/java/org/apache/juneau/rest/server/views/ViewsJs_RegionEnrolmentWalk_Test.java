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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * WORK-J0522d: source-shape pins for {@code juneau-views.js}'s ONE real enrolment call site (caller #3 of 3,
 * {@code expandDetailRow}) and all FIVE region removal paths (design §9.3's teardown table, including the B3
 * {@code teardownTable} reinit row) - the {@code juneau-views.js} slice of the design's round-4 region-enrolment
 * fix.
 *
 * <p>Mirrors {@link ViewsJs_Reinit_Test}'s own balanced-brace {@code functionBody} extractor and its
 * source-shape-pin style for {@code teardownTable} et al., rather than driving a full DataTables+row-detail
 * click simulation through a from-scratch fake (that harness - see {@code row-detail.cjs} - is 1300+ lines for
 * ONE subject): a source-shape pin on the exact call, its argument, and its ordering relative to the
 * already-tested neighboring call it was added beside is what actually catches a future edit deleting or
 * misordering one of these five calls, and is far cheaper to keep correct than a sixth full DOM harness.
 *
 * <p>The real BEHAVIORAL proof that {@code enrolIn}/{@code teardownRegionsIn} do what they claim against a live
 * region lives in the {@code Regions_*} Node-harness tests (loading the real {@code juneau-regions.js}); this
 * class only proves that {@code juneau-views.js} calls them, with the right argument, in the right place.
 */
class ViewsJs_RegionEnrolmentWalk_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String regionsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.REGIONS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.REGIONS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Balanced-brace extractor (nested functions/object literals would fool a plain {@code \n\t}} scan). */
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
	// a) The REGION_MARKER copy - a JS-to-JS pin against juneau-regions.js's own REGION_ATTR, since RegionDef has
	// no emitter yet (that's out of this narrowed slice's scope) to pin a Java constant against.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_regionMarkerConstantMatchesRegionsJsOwnAttr() throws Exception {
		var views = viewsJs();
		var regions = regionsJs();
		assertTrue(views.contains("const REGION_MARKER = \"data-juneau-region\";"), views);
		assertTrue(regions.contains("const REGION_ATTR = \"data-juneau-region\";"), regions);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Enrolment call site 3/3: expandDetailRow, right after the existing enhance-on-insert call.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_expandDetailRow_enrolsRegionsRightAfterEnhanceChromeInPanel() throws Exception {
		var fn = functionBody(viewsJs(), "function expandDetailRow(");
		var chromeIdx = fn.indexOf("enhanceChromeInPanel(panel)");
		var enrolIdx = fn.indexOf("NS.regions.enrolIn(panel)");
		assertTrue(chromeIdx >= 0, fn);
		assertTrue(enrolIdx > chromeIdx, "enrolIn(panel) must run AFTER the panel is enhanced-on-insert:\n" + fn);
		assertTrue(fn.contains("if (NS.regions) NS.regions.enrolIn(panel);"), fn);
		assertTrue(fn.contains("reportRegionsWithoutRuntime(panel.querySelectorAll(\"[\" + REGION_MARKER + \"]\"))"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Removal path 1/5: handleDetailSafeCollapseClick's collapse branch.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_handleDetailSafeCollapseClick_tearsDownRegionsBeforeNestedTablesAndHide() throws Exception {
		var fn = functionBody(viewsJs(), "function handleDetailSafeCollapseClick(");
		var teardownRegionsIdx = fn.indexOf("NS.regions?.teardownRegionsIn(panel)");
		var teardownNestedIdx = fn.indexOf("teardownNestedTables(panel)");
		var hideIdx = fn.indexOf("row.child.hide()");
		assertTrue(teardownRegionsIdx >= 0, fn);
		assertTrue(teardownRegionsIdx < teardownNestedIdx, fn);
		assertTrue(teardownNestedIdx < hideIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Removal path 2/5: toggleDetailRow's collapse branch.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_toggleDetailRow_tearsDownRegionsBeforeNestedTablesAndHide() throws Exception {
		var fn = functionBody(viewsJs(), "function toggleDetailRow(");
		var teardownRegionsIdx = fn.indexOf("NS.regions?.teardownRegionsIn(tr._juneauDetailPanel)");
		var teardownNestedIdx = fn.indexOf("teardownNestedTables(tr._juneauDetailPanel)");
		var hideIdx = fn.indexOf("row.child.hide()");
		assertTrue(teardownRegionsIdx >= 0, fn);
		assertTrue(teardownRegionsIdx < teardownNestedIdx, fn);
		assertTrue(teardownNestedIdx < hideIdx, fn);
		// Guarded the same way the nested-table teardown already is: only when a panel actually exists on tr.
		var guardIdx = fn.indexOf("if (tr._juneauDetailPanel) {");
		assertTrue(guardIdx >= 0 && guardIdx < teardownRegionsIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Removal path 3/5: collapseAllDetailRows, per open row.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_collapseAllDetailRows_tearsDownRegionsPerOpenRowBeforeHide() throws Exception {
		var fn = functionBody(viewsJs(), "function collapseAllDetailRows(");
		var teardownRegionsIdx = fn.indexOf("NS.regions?.teardownRegionsIn(tr._juneauDetailPanel)");
		var teardownNestedIdx = fn.indexOf("teardownNestedTables(tr._juneauDetailPanel)");
		var hideIdx = fn.indexOf("row.child.hide()");
		assertTrue(teardownRegionsIdx >= 0, fn);
		assertTrue(teardownRegionsIdx < teardownNestedIdx, fn);
		assertTrue(teardownNestedIdx < hideIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Removal path 4/5: bindDetailInflightDrawGuards's preDraw.dt hook, guarded by _shouldCancelPollDraw.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_preDrawGuard_tearsDownRegionsOnlyWhenNotCancelling_beforeNestedTeardown() throws Exception {
		var fn = functionBody(viewsJs(), "function bindDetailInflightDrawGuards(");
		var cancelGuardIdx = fn.indexOf("if (ctx._shouldCancelPollDraw?.()) return;");
		var teardownRegionsIdx = fn.indexOf("NS.regions?.teardownRegionsIn(table)");
		var teardownNestedIdx = fn.indexOf("teardownNestedTables(table)");
		assertTrue(cancelGuardIdx >= 0, fn);
		assertTrue(teardownRegionsIdx > cancelGuardIdx,
			"region teardown must be AFTER the cancel guard (never run on a redraw this handler cancels):\n" + fn);
		assertTrue(teardownRegionsIdx < teardownNestedIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Removal path 5/5 (B3): teardownTable's reinit/destroy path - first AND unguarded.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_teardownTable_tearsDownRegionsFirstAndUnguarded() throws Exception {
		var fn = functionBody(viewsJs(), "function teardownTable(");
		var openBrace = fn.indexOf('{');
		var teardownRegionsIdx = fn.indexOf("NS.regions?.teardownRegionsIn(table)");
		var pollTimersIdx = fn.indexOf("_pollTimers");
		var destroyIdx = fn.indexOf("ctx.dataTable.destroy()");
		assertTrue(teardownRegionsIdx >= 0, fn);
		// "First": strip `//` line comments from what precedes it - nothing but comment lines (and whitespace)
		// may remain, i.e. no intervening statement/conditional of any kind.
		var between = fn.substring(openBrace + 1, teardownRegionsIdx);
		var codeOnly = between.replaceAll("//[^\n]*", "").trim();
		assertTrue(codeOnly.isEmpty(), () -> "expected ONLY leading comments before the region teardown, got:\n" + between);
		assertTrue(teardownRegionsIdx < pollTimersIdx, fn);
		assertTrue(pollTimersIdx < destroyIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) Missing-runtime failure mode: loud, not blank.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_missingRuntimeReportsLoudNotBlank() throws Exception {
		var fn = functionBody(viewsJs(), "function reportRegionsWithoutRuntime(");
		assertTrue(fn.contains("juneau-regions.js is not loaded"), fn);
		assertTrue(fn.contains("data-juneau-region-state"), fn);
		assertTrue(fn.contains("renderAsyncStatus(el, \"error\""), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) This file is NOT a barrier runtime: no registerRuntime()/ready() call anywhere in it.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_viewsJsNeverRegistersOrDeclaresReady() throws Exception {
		// juneau-views.js is not a boot-time region runtime (the detail expander enrols on demand, not via a
		// document-wide walk) - it must never call the barrier's registerRuntime()/ready() itself, only
		// enrolIn/teardownRegionsIn.  Checked as actual CALLS (regions.xxx(), regions?.xxx(...)) rather than a
		// bare substring match, since this file's own doc comments above legitimately name both by prose.
		var views = viewsJs();
		assertFalse(views.contains("regions.registerRuntime(") || views.contains("regions?.registerRuntime("), views);
		assertFalse(views.contains("regions.ready(") || views.contains("regions?.ready("), views);
	}
}
