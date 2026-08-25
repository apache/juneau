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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} row-selection + bulk-mutation plumbing
 * ({@code TODO-428}).  Mirrors {@link ViewsJs_RowActions_Test}'s served-script substring style: proves the
 * load-bearing pieces of the two-independent-opt-ins contract are present in the shipped asset, without booting a
 * browser (the behavioral proof lives in the opt-in {@code RowSelectionBulk_BrowserTest} canary).
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_Selection_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) DOM attribute names mirror ViewTable's constants exactly - both halves must agree by construction.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_selectionDomAttrNamesMirrorViewTable() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const SELECT_ATTR = \"" + ViewTable.SELECT_ATTR + "\""), body);
		assertTrue(body.contains("const ROW_ID_ATTR = \"data-juneau-row-id\""), body);
		assertTrue(body.contains("const ROW_ID_FIELD_ATTR = \"" + ViewTable.ROW_ID_FIELD_ATTR + "\""), body);
		assertTrue(body.contains("const SELECT_ALL_ATTR = \"" + ViewTable.SELECT_ALL_ATTR + "\""), body);
		assertTrue(body.contains("const BULK_ATTR = \"" + ViewTable.BULK_ATTR + "\""), body);
		assertTrue(body.contains("const BULK_SIDECAR_ID_PREFIX = \"" + ViewTable.BULK_SIDECAR_ID_PREFIX + "\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) A THIRD, independently-versioned contract for bulk actions - never aliased to VIEW_META or the
	//    action-result contract (R2 guard).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_bulkContractVersionIsItsOwnThirdConstant() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const JUNEAU_BULK_CONTRACT_VERSION = \"" + BulkMutateDef.CONTRACT_VERSION + "\""), body);
		// Three DISTINCT contract-version constants must all be present - none aliased to another.
		assertTrue(body.contains("JUNEAU_VIEW_CONTRACT_VERSION"), body);
		assertTrue(body.contains("JUNEAU_ACTION_RESULT_CONTRACT_VERSION"), body);
		assertTrue(body.contains("NS.BULK_CONTRACT_VERSION = JUNEAU_BULK_CONTRACT_VERSION"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Selection identity is the STABLE ROW ID (MED-11) - rowIdOf/stampRowId never fall back to a DOM index.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_rowIdOf_resolvesFromRowDataOnly_neverAnIndex() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function rowIdOf(");
		assertTrue(fn.contains("rowData[rowIdField]"), fn);
		// No index-based fallback anywhere in this function - the whole point of MED-11.
		assertFalse(fn.contains("index"), fn);
	}

	@Test void c02_stampRowId_writesTheStableIdAttribute_neverAnIndex() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function stampRowId(");
		assertTrue(fn.contains("rowIdOf(rowData, rowIdField)"), fn);
		assertTrue(fn.contains("rowEl.setAttribute(ROW_ID_ATTR"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) The off-screen-id-drop persistence rule (Q2/MED-11) - pure, DOM-free.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_pruneSelection_dropsIdsNotInTheCurrentDraw() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function pruneSelection(");
		assertTrue(fn.contains("present[String(id)] = true"), fn);
		assertTrue(fn.contains("Object.hasOwn(present, String(id))"), fn);
	}

	@Test void d02_initSelection_listenerLifetimeSplit_pruneIsPerInstance() throws Exception {
		var body = viewsJs();
		var nativeFn = functionBody(body, "function initSelection(");
		assertTrue(nativeFn.contains("table.addEventListener(\"change\""), nativeFn);
		assertTrue(nativeFn.contains(".juneau-view-select-checkbox"), nativeFn);
		assertFalse(nativeFn.contains("\"draw.dt\""), nativeFn);

		var pruneFn = functionBody(body, "function bindSelectionPrune(");
		assertTrue(pruneFn.contains("\"draw.dt\""), pruneFn);
		assertTrue(pruneFn.contains("pruneSelection(Array.from(selectionState.selected), ids)"), pruneFn);
		assertTrue(pruneFn.contains("selectionState.selected = new Set(pruned)"), pruneFn);
	}

	@Test void e01_selectAll_isScopedToTheCurrentDrawsRowsOnly() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function initSelection(");
		// The select-all header checkbox only ever iterates rows currently in the tbody - never an off-screen page,
		// and (via ownRowsWithId) never a nested table's rows inside an expanded row-detail panel either.
		assertTrue(fn.contains("ownRowsWithId(table)"), fn);
		assertTrue(fn.contains(".juneau-view-select-all-checkbox"), fn);
		var own = functionBody(body, "function ownRowsWithId(");
		assertTrue(own.contains("tbody tr[\" + ROW_ID_ATTR + \"]"), own);
		var ensure = functionBody(body, "function ensureSelectAllCheckbox(");
		assertTrue(ensure.contains("SELECT_ALL_ATTR"), ensure);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Two INDEPENDENT opt-ins (HIGH-5): hasBulk(...) is only ever reachable from inside the hasSelection(...)
	//    branch of initTable - selection alone can never surface a bulk-mutate control.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_initTable_bulkIsOnlyEverConsultedInsideTheSelectionBranch() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function beginInitTable(");
		var selectionIdx = fn.indexOf("const selectionState = hasSelection(table)");
		var selectionBranchIdx = fn.indexOf("if (selectionState) {");
		var bulkCheckIdx = fn.indexOf("hasBulk(table)");
		assertTrue(selectionIdx >= 0, fn);
		assertTrue(selectionBranchIdx > selectionIdx, fn);
		assertTrue(bulkCheckIdx > selectionBranchIdx, fn);
		var construct = functionBody(body, "function constructTable(");
		assertTrue(construct.contains("buildBulkToolbar(ctx._bulkDef, table, ctx, ctx.selectionState)"), construct);
	}

	@Test void f02_buildTable_prependsASyntheticLeadingSelectionColumn_beforeResolveOrder() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function assembleFullColumnArray(");
		assertTrue(fn.contains("buildSelectionColumnDef(ctx.selectionState)"), fn);
		assertTrue(fn.contains("cols.push(sel)"), fn);
		assertTrue(fn.contains("opts.order = resolveOrder(viewDef, opts.columns)"), fn);
		assertFalse(fn.contains("opts.columns.unshift"), fn);
	}

	@Test void f03_initTable_selectionWiringIsUnconditionalOnBulkHealth() throws Exception {
		// initSelection(...) must run whenever selection was declared, REGARDLESS of whether the bulk sidecar is
		// present/healthy - selection (e.g. for export) must keep working even if bulk mutation is withheld.
		var body = viewsJs();
		var fn = functionBody(body, "function beginInitTable(");
		var initSelectionIdx = fn.indexOf("initSelection(table, ctx)");
		var bulkCheckIdx = fn.indexOf("hasBulk(table)");
		assertTrue(initSelectionIdx >= 0 && initSelectionIdx < bulkCheckIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Per-target bulk execution (HIGH-5) - N independent submitRowAction(...) calls, never one aggregate request.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_executeBulkAction_isAPerTargetLoop_notAnAggregateCall() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function executeBulkAction(");
		assertTrue(fn.contains("ids.forEach(function (id) {"), fn);
		assertTrue(fn.contains("submitRowAction(action, table, tr, ctx, { targetId: id })"), fn);
		// No aggregate transport - a bulk action never opens its own fetch/Promise.all; it only reuses the
		// single-row submit path once per target.
		assertFalse(fn.contains("Promise.all"), fn);
		assertFalse(fn.contains("fetch("), fn);
	}

	@Test void g02_executeBulkAction_skipsIdsThatHaveGoneOffScreen() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function executeBulkAction(");
		assertTrue(fn.contains("if (!tr) return;"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) Bulk toolbar reflects the live selection count and gates on it (nothing to target with zero selected).
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_bulkToolbar_disablesButtonsWhenSelectionIsEmpty() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function buildBulkToolbar(");
		assertTrue(fn.contains("btn.disabled = true"), fn);
		assertTrue(fn.contains("b.disabled = count === 0"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) A missing/contract-mismatched bulk sidecar is withheld, fail-loud, WITHOUT killing selection.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_bulkContractMismatch_isLoggedAndWithheld_selectionSurvives() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function beginInitTable(");
		assertTrue(fn.contains("bulkDef.contractVersion !== JUNEAU_BULK_CONTRACT_VERSION"), fn);
		assertTrue(fn.contains("bulk mutation withheld"), fn);
	}
}
