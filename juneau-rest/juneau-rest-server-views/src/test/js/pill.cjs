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

process.stdout.write(JSON.stringify(out));
