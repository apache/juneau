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

import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} row-selection + bulk-mutation plumbing.
 * Mirrors {@link ViewsJs_RowActions_Test}'s served-script substring style: proves the
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

	/**
	 * stampRowId writes the stable id attribute (never an index); pruneSelection drops ids not in the current
	 * draw; and the bulk toolbar disables its buttons when the selection is empty.
	 */
	@ParameterizedTest
	@MethodSource("c02_functionBodyContainsTwoSubstringsProvider")
	void c02_functionBodyContainsExpectedSubstrings(String functionSignature, String expected1, String expected2) throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, functionSignature);
		assertTrue(fn.contains(expected1), fn);
		assertTrue(fn.contains(expected2), fn);
	}

	static Stream<Arguments> c02_functionBodyContainsTwoSubstringsProvider() {
		return Stream.of(
			Arguments.of("function stampRowId(", "rowIdOf(rowData, rowIdField)", "rowEl.setAttribute(ROW_ID_ATTR"),
			Arguments.of("function pruneSelection(", "present[String(id)] = true", "Object.hasOwn(present, String(id))"),
			Arguments.of("function buildBulkToolbar(", "goBtn.disabled = true", "el.hidden = count === 0"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) The off-screen-id-drop persistence rule (Q2/MED-11) - pure, DOM-free.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d02_initSelection_listenerLifetimeSplit_pruneIsPerInstance() throws Exception {
		var body = viewsJs();
		var nativeFn = functionBody(body, "function initSelection(");
		assertTrue(nativeFn.contains("table.addEventListener(\"change\""), nativeFn);
		assertTrue(nativeFn.contains(".juneau-view-select-checkbox"), nativeFn);
		assertFalse(nativeFn.contains("\"draw.dt\""), nativeFn);

		var pruneFn = functionBody(body, "function bindSelectionPrune(");
		assertTrue(pruneFn.contains("\"draw.dt\""), pruneFn);
		// Scope-aware: only PAGE scope drops off-screen ids; PERSISTENT (the default) keeps them.
		assertTrue(pruneFn.contains("selectionState.scope === \"page\""), pruneFn);
		assertTrue(pruneFn.contains("pruneSelection(Array.from(selectionState.selected), ids)"), pruneFn);
		assertTrue(pruneFn.contains("selectionState.selected = new Set(pruneSelection("), pruneFn);
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
		var fn = functionBody(body, "function initTableFromDef(");
		var selectionIdx = fn.indexOf("const selectionState = hasSelection(table)");
		var selectionBranchIdx = fn.indexOf("if (selectionState) {");
		var bulkCheckIdx = fn.indexOf("if (hasBulk(table))");
		assertTrue(selectionIdx >= 0, fn);
		assertTrue(selectionBranchIdx > selectionIdx, fn);
		assertTrue(bulkCheckIdx > selectionBranchIdx, fn);
		var wire = functionBody(body, "function wireSelectionAndBulkToolbar(");
		assertTrue(wire.contains("buildBulkToolbar(ctx._bulkDef, table, ctx, ctx.selectionState)"), wire);
	}

	@Test void f02_buildTable_prependsASyntheticLeadingSelectionColumn_beforeResolveOrder() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function assembleFullColumnArray(");
		assertTrue(fn.contains("buildSelectionColumnDef(ctx.selectionState, ctx)"), fn);
		assertTrue(fn.contains("cols.push(sel)"), fn);
		assertTrue(fn.contains("opts.order = resolveOrder(viewDef, opts.columns)"), fn);
		assertFalse(fn.contains("opts.columns.unshift"), fn);
	}

	@Test void f03_initTable_selectionWiringIsUnconditionalOnBulkHealth() throws Exception {
		// initSelection(...) must run whenever selection was declared, REGARDLESS of whether the bulk sidecar is
		// present/healthy - selection (e.g. for export) must keep working even if bulk mutation is withheld.
		var body = viewsJs();
		var fn = functionBody(body, "function initTableFromDef(");
		var initSelectionIdx = fn.indexOf("initSelection(table, ctx)");
		var bulkCheckIdx = fn.indexOf("if (hasBulk(table))");
		assertTrue(initSelectionIdx >= 0 && initSelectionIdx < bulkCheckIdx, fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Per-target bulk execution (HIGH-5) - N independent submitRowAction(...) calls, never one aggregate request.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_runBulkAction_replacesExecuteBulkAction_perRowIsTheDefault() throws Exception {
		var body = viewsJs();
		assertFalse(body.contains("executeBulkAction"), "executeBulkAction must be gone");
		var fn = functionBody(body, "async function runBulkAction(");
		assertTrue(fn.contains("submitPerRowBulk("), fn);
		assertTrue(fn.contains("submitAggregateBulk("), fn);
	}

	@Test void g02_perRow_usesSnapshotsSoOffScreenRowsStillSubmit() throws Exception {
		var fn = functionBody(viewsJs(), "async function submitPerRowBulk(");
		assertTrue(fn.contains("selectionState.snapshots[id]"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) Bulk toolbar reflects the live selection count and gates on it - see c02 above, which covers this
	//    alongside two structurally-identical function-body-substring checks.
	//------------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// i) A missing/contract-mismatched bulk sidecar is withheld, fail-loud, WITHOUT killing selection.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_bulkContractMismatch_isLoggedAndWithheld_selectionSurvives() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function resolveTableBulkDef(");
		assertTrue(fn.contains("bulkDef.contractVersion !== JUNEAU_BULK_CONTRACT_VERSION"), fn);
		assertTrue(fn.contains("bulk mutation withheld"), fn);
	}

	@Test void h01_bulkContractVersion_isBumpedToTwo() throws Exception {
		assertTrue(viewsJs().contains("JUNEAU_BULK_CONTRACT_VERSION = \"2\""));
	}

	@Test void h02_selectScopeAndLabelFieldAttrConstants_exist() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("SELECT_SCOPE_ATTR"));
		assertTrue(body.contains("SELECT_LABEL_FIELD_ATTR"));
		assertTrue(body.contains("SELECTABLE_WHEN_SIDECAR_ID_PREFIX"));
	}

	@Test void h03_bindSelectionPrune_isScopeAware_pageScopePrunesPersistentDoesNot() throws Exception {
		var fn = functionBody(viewsJs(), "function bindSelectionPrune(table, ctx)");
		assertTrue(fn.contains("scope"));
		assertTrue(fn.contains("pruneSelection"));
		assertTrue(fn.contains("\"page\""));
	}

	@Test void h04_refreshSelectAllHeaderState_computesTriState() throws Exception {
		var fn = functionBody(viewsJs(), "function refreshSelectAllHeaderState(table, ctx)");
		assertTrue(fn.contains(".indeterminate"));
		assertTrue(fn.contains(".checked"));
	}

	@Test void h05_buildSelectionColumnDef_rendersDisabledCheckboxWithReason() throws Exception {
		var fn = functionBody(viewsJs(), "function selectionCellMarkup(");
		assertTrue(fn.contains("disabled"));
		assertTrue(fn.contains("aria-describedby"));
		assertTrue(fn.contains("title="));
	}

	@Test void h06_initSelection_selectAllSkipsDisabledRows() throws Exception {
		var fn = functionBody(viewsJs(), "function initSelection(table, ctx)");
		assertTrue(fn.contains(".disabled"));
	}

	@Test void h07_captureRowSnapshot_isExported() throws Exception {
		assertTrue(viewsJs().contains("captureRowSnapshot:"));
	}

	@Test void i01_buildBulkToolbar_singleAction_rendersButtonNotDropdown() throws Exception {
		var fn = functionBody(viewsJs(), "function buildBulkToolbar(");
		assertTrue(fn.contains("selected ("), fn);
		assertTrue(fn.contains("juneau-view-bulk-select"), fn);
	}

	@Test void i02_buildBulkToolbar_rendersClearSelectionLink() throws Exception {
		var fn = functionBody(viewsJs(), "function buildBulkToolbar(");
		assertTrue(fn.contains("Clear selection"), fn);
		assertTrue(fn.contains("selected \\u00b7"), fn);
	}

	@Test void i03_wireRightCluster_placesBulkToolbarAtFarEndOfRightCluster() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function wireToolbarRightClusterBulk("));
		var leftFn = functionBody(body, "function wireToolbarLeftCluster(");
		assertFalse(leftFn.contains("bulkToolbar.el"), leftFn);
	}

	@Test void i04_confirmBulkAction_prefersWindowDialogsConfirm_fallsBackToBuiltIn() throws Exception {
		var fn = functionBody(viewsJs(), "function confirmBulkAction(");
		assertTrue(fn.contains("window.JuneauViews?.dialogs?.confirm"), fn);
		assertTrue(fn.contains("buildFallbackConfirmModal("), fn);
		assertTrue(functionBody(viewsJs(), "function buildFallbackConfirmModal(").contains("pushLayer"));
	}

	@Test void i05_builtInConfirmFallback_capsListAtTwentyWithAndNMore() throws Exception {
		var fn = functionBody(viewsJs(), "function buildFallbackConfirmModal(");
		assertTrue(fn.contains("cap = 20"), fn);
		assertTrue(fn.contains(" more"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// j) Bulk execution: per-row concurrency, summary toast, selection clearing, aggregate failures.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_submitRowActionForId_isExported() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("submitRowActionForId: submitRowActionForId"), body.length() + "");
		assertTrue(body.contains("runBulkAction: runBulkAction"));
	}

	@Test void j02_perRow_concurrencyIsCappedAtFour() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const BULK_PER_ROW_CONCURRENCY = 4;"));
		assertTrue(functionBody(body, "async function submitPerRowBulk(").contains("BULK_PER_ROW_CONCURRENCY"));
	}

	@Test void j03_summaryToast_formatMatchesSpecExactly() throws Exception {
		var fn = functionBody(viewsJs(), "function buildBulkSummaryMessage(");
		assertTrue(fn.contains("\" succeeded\""), fn);
		assertTrue(fn.contains("\" not found\""), fn);
		assertTrue(fn.contains("\" failed (ids: \""), fn);
	}

	@Test void j04_onSuccess_clearsSucceededAndNotFound_keepsFailedSelected() throws Exception {
		var fn = functionBody(viewsJs(), "async function runBulkAction(");
		assertTrue(fn.contains("outcome.succeeded.concat(outcome.notFound)"), fn);
		assertTrue(fn.contains("selectionState.selected.delete("), fn);
		assertFalse(fn.contains("outcome.failed.forEach"), fn);
	}

	@Test void j05_onSuccessNone_skipsReload() throws Exception {
		var fn = functionBody(viewsJs(), "async function runBulkAction(");
		assertTrue(fn.contains("action.onSuccess !== \"none\""), fn);
		assertTrue(fn.contains("reloadTableData("), fn);
	}

	@Test void j06_aggregateTransportFailure_raisesEJS26() throws Exception {
		var fn = functionBody(viewsJs(), "async function submitAggregateBulk(");
		assertTrue(fn.contains("E-JS-26"), fn);
		assertTrue(fn.contains("JSON.stringify({ ids: ids, idempotencyKey: idempotencyKey })"), fn);
	}

	@Test void j07_aggregateNonBulkResultResponse_raisesEJS27() throws Exception {
		var fn = functionBody(viewsJs(), "async function submitAggregateBulk(");
		assertTrue(fn.contains("E-JS-27"), fn);
		assertTrue(fn.contains("response is not a BulkResult (contractVersion '"), fn);
	}
}
