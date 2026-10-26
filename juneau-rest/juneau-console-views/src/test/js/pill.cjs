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

/*
 * pill.cjs - always-on Node harness for the action-bound pill dispatch wired on initRowActions (pill-445k).  Proves
 * an action pill submits its RowAction through the SAME fail-closed handler the row-action menu uses, on a grid with
 * rowActions and NO row-detail template; that Enter/Space activate it (Space preventDefault-ed); that a present=dialog
 * action opens the modal instead of submitting; that a disabled / in-flight pill is inert; and that setActionRefEnabled
 * reflects the disabled state via aria on the (non-native) span button.  Every assertion lives in the Java test.
 *
 *   Usage:  node pill.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node pill.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: !!(I && typeof I.initRowActions === 'function' && typeof I.activatePillAction === 'function')
};
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const ACK = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST' };
const ESC = { id: 'esc', label: 'Escalate', present: 'dialog', confirm: 'Escalate this row?' };

// A recording fetch stub: every install returns a 2xx (415 does not act on the body here).
const fetchCalls = [];
env.setFetch(function (url, opts) { fetchCalls.push({ url: url, opts: opts }); return Promise.resolve(jsonResponse({}, { status: 200 })); });

/**
 * Builds a fresh view fixture: a <table data-juneau-csrf> with one body row whose single <td> holds an
 * action-bound pill span mirroring the renderer's output (data-juneau-pill + role=button + data-juneau-action).
 * initRowActions is the ONLY handler wired (no initDetailsExpander / no row-detail template) unless withDetails.
 */
function fixture(actionId, viewDef, withDetails) {
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-123';
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	const td = env.el('td');
	const pill = env.el('span');
	pill.className = 'jc-pill tag state open';
	pill.dataset.juneauPill = '';
	if (actionId != null) {
		pill.setAttribute('role', 'button');
		pill.setAttribute('tabindex', '0');
		pill.dataset.juneauAction = actionId;
	}
	td.appendChild(pill);
	tr.appendChild(td);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(table);
	const ctx = { viewDef: viewDef };
	I.initRowActions(table, viewDef, ctx);
	if (withDetails) I.initDetailsExpander(table, ctx, viewDef);
	return { table: table, tr: tr, td: td, pill: pill, ctx: ctx };
}

function clickPill(f) {
	f.table.dispatch('click', { target: f.pill });
}
function keyPill(f, key) {
	let prevented = false;
	f.table.dispatch('keydown', { target: f.pill, key: key, preventDefault: function () { prevented = true; } });
	return prevented;
}

// --- Case 1: submit path on a grid with rowActions and NO row-detail template ---------------------------------
let before = fetchCalls.length;
const c1 = fixture('ack', { rowActions: [ACK] }, false);
clickPill(c1);
out.click_fetchIssued = fetchCalls.length > before;
if (out.click_fetchIssued) {
	const call = fetchCalls.at(-1);
	out.click_url = call.url;
	out.click_method = call.opts.method;
}

// --- Case 2: keyboard parity - Enter and Space both activate; Space is preventDefault-ed ----------------------
// A fresh fixture per key: a submit marks the row in-flight, which would (correctly) block a second activation.
before = fetchCalls.length;
out.enter_prevented = keyPill(fixture('ack', { rowActions: [ACK] }, false), 'Enter');
out.enter_fetchIssued = fetchCalls.length > before;
before = fetchCalls.length;
out.space_prevented = keyPill(fixture('ack', { rowActions: [ACK] }, false), ' ');
out.space_fetchIssued = fetchCalls.length > before;
before = fetchCalls.length;
out.otherKey_prevented = keyPill(fixture('ack', { rowActions: [ACK] }, false), 'a');
out.otherKey_fetchIssued = fetchCalls.length > before;

// --- Case 3: present=dialog opens the modal (confirm/dialog branch) instead of submitting ----------------------
before = fetchCalls.length;
const c3 = fixture('esc', { rowActions: [ESC] }, false);
clickPill(c3);
out.dialog_fetchIssued = fetchCalls.length > before;   // confirm-only dialog is a LOCAL open, no submit fetch
out.dialog_layerOpen = I.dialogLayerCount() > 0;
I.closeActionDialog(c3.ctx);

// --- Case 4: a disabled pill (aria-disabled) is inert ---------------------------------------------------------
before = fetchCalls.length;
const c4 = fixture('ack', { rowActions: [ACK] }, false);
c4.pill.setAttribute('aria-disabled', 'true');
clickPill(c4);
out.disabled_fetchIssued = fetchCalls.length > before;

// --- Case 5: an in-flight row is inert (no double submit) -----------------------------------------------------
before = fetchCalls.length;
const c5 = fixture('ack', { rowActions: [ACK] }, false);
c5.tr.dataset.juneauInflight = '1';
clickPill(c5);
out.inflight_fetchIssued = fetchCalls.length > before;

// --- Case 6: an unknown action id (not in rowActions) is inert (fail-closed) ----------------------------------
before = fetchCalls.length;
const c6 = fixture('missing', { rowActions: [ACK] }, false);
clickPill(c6);
out.unknownAction_fetchIssued = fetchCalls.length > before;

// --- Case 7: a display-only pill (no data-juneau-action) never dispatches -------------------------------------
before = fetchCalls.length;
const c7 = fixture(null, { rowActions: [ACK] }, false);
clickPill(c7);
out.displayOnly_fetchIssued = fetchCalls.length > before;

// --- Case 8: setActionRefEnabled reflects the disabled state on the span button via aria ----------------------
const c8 = fixture('ack', { rowActions: [ACK] }, false);
I.setActionRefEnabled(c8.td, false);
out.setDisabled_aria = c8.pill.getAttribute('aria-disabled') === 'true';
before = fetchCalls.length;
clickPill(c8);
out.setDisabled_fetchIssued = fetchCalls.length > before;
I.setActionRefEnabled(c8.td, true);
out.setEnabled_ariaCleared = c8.pill.getAttribute('aria-disabled') == null;
before = fetchCalls.length;
clickPill(c8);
out.setEnabled_fetchIssued = fetchCalls.length > before;

// --- Case 9: NOT details-gated - a pill still dispatches even when initDetailsExpander is also wired ----------
before = fetchCalls.length;
const c9 = fixture('ack', { rowActions: [ACK] }, true);
clickPill(c9);
out.withDetails_fetchIssued = fetchCalls.length > before;

// --- Case 10: selection stays checkbox-only - clicking or keying a pill never toggles a row -------------------
// Selection is the SelectionDef/BulkMutateDef checkbox protocol.  A pill is not a selection affordance, so with
// initSelection ALSO wired on the same table, activating a pill must leave selectionState untouched (and a
// display-only pill must be just as inert).  Guards against re-growing the dropped select-pill path.
(function selectionIntegrity() {
	const selectionState = { selected: new Set(), rowIdField: 'id' };
	const f = fixture('ack', { rowActions: [ACK] }, false);
	f.ctx.selectionState = selectionState;
	I.initSelection(f.table, f.ctx);
	f.tr.dataset.juneauRowId = 'r1';

	clickPill(f);
	out.selection_afterPillClick = selectionState.selected.size;
	keyPill(f, 'Enter');
	keyPill(f, ' ');
	out.selection_afterPillKeys = selectionState.selected.size;

	const d = fixture(null, { rowActions: [ACK] }, false);
	d.ctx.selectionState = selectionState;
	I.initSelection(d.table, d.ctx);
	d.tr.dataset.juneauRowId = 'r2';
	clickPill(d);
	out.selection_afterDisplayOnlyPillClick = selectionState.selected.size;

	// Positive control: the checkbox protocol still works on the very same table, so the zeroes above are the pill
	// being inert rather than initSelection being unwired.
	const cb = env.el('input');
	cb.className = 'juneau-view-select-checkbox';
	cb.checked = true;
	f.td.appendChild(cb);
	f.table.dispatch('change', { target: cb });
	out.selection_afterCheckboxChange = selectionState.selected.size;
	out.selection_checkboxSelectedR1 = selectionState.selected.has('r1');
})();

// ================================================================================================================
// RowAction.enabledWhen - disable-with-reason gate, evaluated client-side by the shared ActionRef-rule evaluator
// (firstFailingActionRule/actionRuleMatches, NOT evaluateRowClassRules), across all three activation surfaces:
// the draw-time pill pass, the row-action menu, and the dialog-form action button (openFormActionDialog).
// ================================================================================================================

const GATED = {
	id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST',
	enabledWhen: [{ field: 'status', op: 'eq', value: 'open', reason: 'Only open items can be acknowledged.' }]
};
const DIALOG_GATED = {
	id: 'esc', label: 'Escalate', present: 'dialog', confirm: 'Escalate this row?',
	enabledWhen: [{ field: 'status', op: 'eq', value: 'open', reason: 'Only open items can be acknowledged.' }]
};

function dataTableOf(rowData) {
	return { row: function () { return { data: function () { return rowData; } }; } };
}

// --- draw-time visual pass (applyRowActionPillGates, the createdRow-time hook) ---------------------------------
const dg1 = fixture('ack', { rowActions: [GATED] }, false);
I.applyRowActionPillGates(dg1.tr, { status: 'closed' }, { rowActions: [GATED] });
out.drawGate_failingAriaDisabled = dg1.pill.getAttribute('aria-disabled') === 'true';
out.drawGate_failingTitle = dg1.pill.getAttribute('title');
out.drawGate_failingNeverHidden = dg1.pill.hidden !== true;
const dg1DescId = dg1.pill.getAttribute('aria-describedby');
const dg1Desc = dg1.td.querySelector('[data-juneau-row-action-desc]');
out.drawGate_failingDescNodeExists = !!dg1Desc;
out.drawGate_failingDescNodeIdMatches = !!dg1Desc && dg1DescId === dg1Desc.getAttribute('id');
out.drawGate_failingDescText = dg1Desc ? dg1Desc.textContent : null;

const dg2 = fixture('ack', { rowActions: [GATED] }, false);
I.applyRowActionPillGates(dg2.tr, { status: 'open' }, { rowActions: [GATED] });
out.drawGate_passingAriaDisabledAbsent = dg2.pill.getAttribute('aria-disabled') == null;
out.drawGate_passingNoTitle = dg2.pill.getAttribute('title') == null;

// --- first-declared-failing-rule wins, in both declared orders --------------------------------------------------
const R_STATUS = { field: 'status', op: 'eq', value: 'open', reason: 'REASON-STATUS' };
const R_OWNER = { field: 'owner', op: 'present', reason: 'REASON-OWNER' };
const bothFail = { status: 'closed', owner: '' };
const forwardAction = { id: 'ack', endpoint: '/x/ack', method: 'POST', enabledWhen: [R_STATUS, R_OWNER] };
const reversedAction = { id: 'ack', endpoint: '/x/ack', method: 'POST', enabledWhen: [R_OWNER, R_STATUS] };

const dg3 = fixture('ack', { rowActions: [forwardAction] }, false);
I.applyRowActionPillGates(dg3.tr, bothFail, { rowActions: [forwardAction] });
out.firstFailing_forward = dg3.pill.getAttribute('title');

const dg4 = fixture('ack', { rowActions: [reversedAction] }, false);
I.applyRowActionPillGates(dg4.tr, bothFail, { rowActions: [reversedAction] });
out.firstFailing_reversed = dg4.pill.getAttribute('title');

// --- fail-closed: the rule's field is absent from rowData, or rowData itself could not be resolved -------------
const dg5 = fixture('ack', { rowActions: [GATED] }, false);
I.applyRowActionPillGates(dg5.tr, { unrelated: 'x' }, { rowActions: [GATED] });
out.failClosed_missingFieldDisabled = dg5.pill.getAttribute('aria-disabled') === 'true';

const dg6 = fixture('ack', { rowActions: [GATED] }, false);
I.applyRowActionPillGates(dg6.tr, null, { rowActions: [GATED] });
out.failClosed_nullRowDataDisabled = dg6.pill.getAttribute('aria-disabled') === 'true';

// --- activatePillAction re-checks fresh at click time, independent of the draw-time pass (defense in depth) ----
const rc1 = fixture('ack', { rowActions: [GATED] }, false);
rc1.ctx.dataTable = dataTableOf({ status: 'closed' });
before = fetchCalls.length;
clickPill(rc1);
out.reCheck_failingNeverFires = fetchCalls.length === before;

const rc2 = fixture('ack', { rowActions: [GATED] }, false);
rc2.ctx.dataTable = dataTableOf({ status: 'open' });
before = fetchCalls.length;
clickPill(rc2);
out.reCheck_passingStillFires = fetchCalls.length > before;

// --- buildRowActionMenu: a gated+failing item is disabled+reasoned but STILL PRESENT (never removed); its click
// never fires.  A gated+passing item renders enabled and its click fires normally.
const menuTr1 = env.el('tr');
const menuCtx1 = { viewDef: { rowActions: [GATED] }, dataTable: dataTableOf({ status: 'closed' }) };
const menu1 = I.buildRowActionMenu(menuCtx1.viewDef, env.el('table'), menuTr1, menuCtx1);
const menuItem1 = menu1.querySelector('.juneau-view-action-item');
out.menu_failingItemDisabled = menuItem1.disabled === true;
out.menu_failingItemTitle = menuItem1.getAttribute('title');
out.menu_failingItemStillPresent = menu1.querySelectorAll('.juneau-view-action-item').length === 1;
out.menu_failingItemNeverHidden = menuItem1.hidden !== true;
const menuDescId1 = menuItem1.getAttribute('aria-describedby');
const menuDesc1 = menu1.querySelector('[data-juneau-row-action-desc]');
out.menu_failingDescNodeExists = !!menuDesc1;
out.menu_failingDescNodeIdMatches = !!menuDesc1 && menuDescId1 === menuDesc1.getAttribute('id');
before = fetchCalls.length;
menuItem1.dispatch('click', {});
out.menu_failingClickNeverFires = fetchCalls.length === before;

const menuTr2 = env.el('tr');
const menuTable2 = env.el('table');
menuTable2.dataset.juneauCsrf = 'tok-123';   // submitRowAction refuses without a CSRF token; irrelevant to the gate itself
const menuCtx2 = { viewDef: { rowActions: [GATED] }, dataTable: dataTableOf({ status: 'open' }) };
const menu2 = I.buildRowActionMenu(menuCtx2.viewDef, menuTable2, menuTr2, menuCtx2);
const menuItem2 = menu2.querySelector('.juneau-view-action-item');
out.menu_passingItemEnabled = menuItem2.disabled === false;
before = fetchCalls.length;
menuItem2.dispatch('click', {});
out.menu_passingClickFires = fetchCalls.length > before;

// --- pin: the menu path resolves row data via ctx.dataTable.row(tr).data(), and fails closed when that lookup
// yields nothing (no dataTable wired at all).
const pinCalls = [];
const menuTr3 = env.el('tr');
const menuCtx3 = {
	viewDef: { rowActions: [GATED] },
	dataTable: { row: function (tr) { pinCalls.push(tr); return { data: function () { return { status: 'open' }; } }; } }
};
I.buildRowActionMenu(menuCtx3.viewDef, env.el('table'), menuTr3, menuCtx3);
out.menu_dataTableRowCalledWithSameTr = pinCalls.length === 1 && pinCalls[0] === menuTr3;

const menuTr4 = env.el('tr');
const menuCtx4 = { viewDef: { rowActions: [GATED] } };   // no dataTable wired at all
const menu4 = I.buildRowActionMenu(menuCtx4.viewDef, env.el('table'), menuTr4, menuCtx4);
out.menu_noDataTableFailsClosed = menu4.querySelector('.juneau-view-action-item').disabled === true;

// --- openFormActionDialog: gated+failing paints a visible refusal in the current top dialog and opens no new
// dialog layer; gated+passing opens the confirm dialog exactly as an ungated action would.
const preExistingDialogDepth = I.dialogLayerCount();
const parentDialogEl = env.el('div');
parentDialogEl.className = 'juneau-view-dialog';
I.pushLayer(parentDialogEl, { kind: 'dialog', portal: false });

const dlgTr1 = env.el('tr');
const dlgCtx1 = { viewDef: { rowActions: [DIALOG_GATED] }, dataTable: dataTableOf({ status: 'closed' }) };
I.openFormActionDialog('esc', env.el('table'), dlgTr1, dlgCtx1);
out.dialogGate_failingNoNewLayer = I.dialogLayerCount() === preExistingDialogDepth + 1;
const refusal = parentDialogEl.querySelector('.juneau-view-dialog-action-refusal');
out.dialogGate_failingRefusalText = refusal ? refusal.textContent : null;

const dlgTr2 = env.el('tr');
const dlgCtx2 = { viewDef: { rowActions: [DIALOG_GATED] }, dataTable: dataTableOf({ status: 'open' }) };
I.openFormActionDialog('esc', env.el('table'), dlgTr2, dlgCtx2);
out.dialogGate_passingOpensDialog = I.dialogLayerCount() === preExistingDialogDepth + 2;
I.closeActionDialog(dlgCtx2);
I.popLayer(parentDialogEl);
out.dialogGate_cleanedUp = I.dialogLayerCount() === preExistingDialogDepth;

process.stdout.write(JSON.stringify(out));
