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
 * bulk-selection.cjs - always-on Node harness for persistent row selection (scope, tri-state select-all header,
 * selectableWhen gating, per-id row snapshots) on the dependency-free views-dom-shim.cjs.  The asserted
 * behaviours are the exports on JuneauViews.init.  Run by ViewsJs_BulkSelection_Test.
 *
 *   Usage:  node bulk-selection.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const assert = require('node:assert/strict');
const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node bulk-selection.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

function load() {
	const loaded = loadViews(rendersJsPath, viewsJsPath);
	return { env: loaded.env, doc: loaded.env.document, NS: loaded.NS.init };
}

function freshTable(doc) {
	const table = doc.createElement('table');
	table.setAttribute('data-juneau-select', '1');
	table.setAttribute('data-juneau-row-id-field', 'id');
	table.setAttribute('data-juneau-select-all', '1');
	// The shim's selector engine matches one compound (no descendant combinators), so ownRowsWithId's
	// "tbody tr[...]" would resolve to a tbody.  Fold the combinator away for this table only.
	const qsa = table.querySelectorAll;
	table.querySelectorAll = sel => qsa.call(table, sel.replace(/^tbody /, ''));
	doc.body.appendChild(table);
	return table;
}

function addRow(doc, table, id) {
	const tr = doc.createElement('tr');
	tr.setAttribute('data-juneau-row-id', id);
	const cb = doc.createElement('input');
	cb.type = 'checkbox';
	cb.className = 'juneau-view-select-checkbox';
	tr.appendChild(cb);
	let tbody = table.querySelector('tbody');
	if (!tbody) { tbody = doc.createElement('tbody'); table.appendChild(tbody); }
	tbody.appendChild(tr);
	return tr;
}

function addHeaderCb(doc, table) {
	const cb = doc.createElement('input');
	cb.type = 'checkbox';
	cb.className = 'juneau-view-select-all-checkbox';
	table.appendChild(cb);
	return cb;
}

function stateOf(scope, ids) {
	return { selected: new Set(ids), rowIdField: 'id', scope, snapshots: {}, selectableWhen: null };
}

/** A fake DataTables handle recording the draw.dt handler; `rowData` maps tr -> data for dt.row(tr).data(). */
function fakeDt(rowData) {
	const dt = { handlers: {}, on: (evt, fn) => { dt.handlers[evt] = fn; } };
	dt.row = tr => ({ data: () => rowData.get(tr) || null });
	return dt;
}

const cases = [];
const t = (name, fn) => cases.push({ name, fn });

t('persistentScope_keepsOffScreenSelectionAcrossDraws', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	addRow(doc, table, 'r1');
	const ctx = { table, dataTable: fakeDt(new Map()), selectionState: stateOf('persistent', ['r1', 'r9']) };
	NS.bindSelectionPrune(table, ctx);
	ctx.dataTable.handlers['draw.dt']({ target: table });
	assert.deepEqual(Array.from(ctx.selectionState.selected).sort(), ['r1', 'r9']);
});

t('pageScope_prunesIdsNotInCurrentDraw', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	addRow(doc, table, 'r1');
	const ctx = { table, dataTable: fakeDt(new Map()), selectionState: stateOf('page', ['r1', 'r9']) };
	NS.bindSelectionPrune(table, ctx);
	ctx.dataTable.handlers['draw.dt']({ target: table });
	assert.deepEqual(Array.from(ctx.selectionState.selected), ['r1']);
});

t('drawRefreshesSnapshotOfVisibleSelectedRow', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	const tr = addRow(doc, table, 'r1');
	addRow(doc, table, 'r2');
	const data = new Map([[tr, { id: 'r1', state: 'RUNNING' }]]);
	const ctx = { table, dataTable: fakeDt(data), selectionState: stateOf('persistent', ['r1']) };
	NS.bindSelectionPrune(table, ctx);
	ctx.dataTable.handlers['draw.dt']({ target: table });
	assert.deepEqual(ctx.selectionState.snapshots.r1, { id: 'r1', state: 'RUNNING' });
	assert.equal(ctx.selectionState.snapshots.r2, undefined);
});

t('triStateHeader_indeterminateWhenSomeSelected', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	addRow(doc, table, 'r1');
	addRow(doc, table, 'r2');
	const allCb = addHeaderCb(doc, table);
	const ctx = { table, dataTable: fakeDt(new Map()), selectionState: stateOf('persistent', ['r1']) };
	NS.refreshSelectAllHeaderState(table, ctx);
	assert.equal(allCb.indeterminate, true);
	assert.equal(allCb.checked, false);
	ctx.selectionState.selected.add('r2');
	NS.refreshSelectAllHeaderState(table, ctx);
	assert.equal(allCb.indeterminate, false);
	assert.equal(allCb.checked, true);
	ctx.selectionState.selected.clear();
	NS.refreshSelectAllHeaderState(table, ctx);
	assert.equal(allCb.indeterminate, false);
	assert.equal(allCb.checked, false);
});

t('triStateHeader_ignoresRowsFailingSelectableWhen', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	const tr1 = addRow(doc, table, 'r1');
	const tr2 = addRow(doc, table, 'r2');
	const allCb = addHeaderCb(doc, table);
	const data = new Map([[tr1, { id: 'r1', state: 'PENDING' }], [tr2, { id: 'r2', state: 'MERGED' }]]);
	const state = stateOf('persistent', ['r1']);
	state.selectableWhen = [{ field: 'state', op: 'eq', value: 'PENDING', reason: 'Only pending.' }];
	NS.refreshSelectAllHeaderState(table, { table, dataTable: fakeDt(data), selectionState: state });
	assert.equal(allCb.checked, true, 'the one selectable row is selected');
	assert.equal(allCb.indeterminate, false);
});

t('selectionCellMarkup_disabledCarriesReasonOnBothChannels', () => {
	const { NS } = load();
	const markup = NS.selectionCellMarkup(false, 'r1', 'Only pending changes can be aborted.');
	assert.ok(markup.includes(' disabled '), markup);
	assert.ok(markup.includes('title="Only pending changes can be aborted."'), markup);
	assert.ok(markup.includes('aria-describedby="juneau-view-select-reason-r1"'), markup);
	assert.ok(markup.includes('juneau-view-select-reason-sr-only'), markup);
	assert.ok(!NS.selectionCellMarkup(true, 'r1', null).includes('disabled'));
	assert.ok(NS.selectionCellMarkup(true, 'r1', null).includes(' checked'));
});

t('selectionColumnDef_rendersDisabledForFailingRule', () => {
	const { NS } = load();
	const state = stateOf('persistent', []);
	state.selectableWhen = [{ field: 'state', op: 'eq', value: 'PENDING', reason: 'Only pending.' }];
	const def = NS.buildSelectionColumnDef(state, {});
	assert.ok(def.render(null, 'display', { id: 'r1', state: 'MERGED' }).includes('disabled'));
	assert.ok(!def.render(null, 'display', { id: 'r2', state: 'PENDING' }).includes('disabled'));
});

t('captureRowSnapshot_storesByStringId_ignoresMissing', () => {
	const { NS } = load();
	const state = stateOf('persistent', []);
	NS.captureRowSnapshot(state, 7, { id: 7 });
	NS.captureRowSnapshot(state, null, { id: 1 });
	NS.captureRowSnapshot(state, 'x', null);
	assert.deepEqual(Object.keys(state.snapshots), ['7']);
});

t('readSelectableWhen_parsesSidecar_nullOnMissingOrMalformed', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	assert.equal(NS.readSelectableWhen('tbl1', table), null);
	const sidecar = doc.createElement('script');
	sidecar.id = 'juneau-view-selectable-when:tbl1';
	sidecar.textContent = '[{"field":"state","op":"eq","value":"PENDING","reason":"r"}]';
	doc.body.appendChild(sidecar);
	assert.equal(NS.readSelectableWhen('tbl1', table).length, 1);
	sidecar.textContent = '{nope';
	assert.equal(NS.readSelectableWhen('tbl1', table), null);
});

t('initSelection_selectAllSkipsDisabledRows_andCapturesSnapshots', () => {
	const { doc, env, NS } = load();
	const table = freshTable(doc);
	const tr1 = addRow(doc, table, 'r1');
	const tr2 = addRow(doc, table, 'r2');
	tr2.querySelector('.juneau-view-select-checkbox').disabled = true;
	const allCb = addHeaderCb(doc, table);
	const data = new Map([[tr1, { id: 'r1' }], [tr2, { id: 'r2' }]]);
	const ctx = { table, dataTable: fakeDt(data), selectionState: stateOf('persistent', []) };
	NS.initSelection(table, ctx);
	allCb.checked = true;
	table.dispatch('change', { target: allCb });
	assert.deepEqual(Array.from(ctx.selectionState.selected), ['r1']);
	assert.deepEqual(ctx.selectionState.snapshots.r1, { id: 'r1' });
	assert.equal(allCb.checked, true, 'header reflects the one selectable row');
	void env;
});

function freshCtx() {
	return { selectionState: stateOf('persistent', []) };
}

t('toolbar_singleAction_rendersButtonLabelWithCount_hiddenWhenEmpty', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	const ctx = freshCtx();
	const toolbar = NS.buildBulkToolbar({ actions: [{ id: 'abort', label: 'Abort' }] }, table, ctx, ctx.selectionState);
	assert.equal(toolbar.el.hidden, true);
	toolbar.refresh(3);
	assert.equal(toolbar.el.hidden, false);
	assert.ok(toolbar.el.querySelector('.juneau-view-bulk-action-btn').textContent.includes('Abort selected (3)'));
	assert.equal(toolbar.el.querySelector('.juneau-view-bulk-count').textContent, '3 selected \u00b7');
	assert.equal(toolbar.el.querySelector('.juneau-view-bulk-clear').textContent, 'Clear selection');
	assert.equal(toolbar.el.querySelector('.juneau-view-bulk-select'), null);
});

t('toolbar_multipleActions_rendersDropdownAndGoDisabledUntilChosen', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	const ctx = freshCtx();
	const toolbar = NS.buildBulkToolbar({ actions: [{ id: 'abort', label: 'Abort' }, { id: 'retry', label: 'Retry' }] }, table, ctx, ctx.selectionState);
	const select = toolbar.el.querySelector('.juneau-view-bulk-select');
	const go = toolbar.el.querySelector('.juneau-view-bulk-go');
	assert.ok(select);
	assert.equal(select.getAttribute('aria-label'), 'Bulk action');
	assert.equal(go.disabled, true);
	select.value = 'retry';
	select.dispatch('change', {});
	assert.equal(go.disabled, false);
});

t('toolbar_clearSelection_emptiesSelectionAndUnchecksBoxes', () => {
	const { doc, NS } = load();
	const table = freshTable(doc);
	const tr = addRow(doc, table, 'r1');
	const cb = tr.querySelector('.juneau-view-select-checkbox');
	cb.checked = true;
	const allCb = addHeaderCb(doc, table);
	allCb.checked = true;
	const ctx = { table, dataTable: fakeDt(new Map()), selectionState: stateOf('persistent', ['r1', 'r9']) };
	const toolbar = NS.buildBulkToolbar({ actions: [{ id: 'abort', label: 'Abort' }] }, table, ctx, ctx.selectionState);
	toolbar.refresh(2);
	toolbar.el.querySelector('.juneau-view-bulk-clear').dispatch('click', {});
	assert.equal(ctx.selectionState.selected.size, 0);
	assert.equal(cb.checked, false);
	assert.equal(allCb.checked, false);
	assert.equal(toolbar.el.hidden, true);
});

t('confirmBulkAction_usesWindowDialogsConfirmWhenPresent', () => {
	const { doc, env, NS } = load();
	let captured = null;
	env.window.JuneauViews.dialogs = { confirm: opts => { captured = opts; } };
	const state = stateOf('persistent', ['r1']);
	state.snapshots.r1 = { id: 'r1', name: 'Alpha' };
	state.labelField = 'name';
	NS.confirmBulkAction({ id: 'abort', label: 'Abort', confirm: 'This cannot be undone.', confirmTitle: 'Abort {count} changes' },
		freshTable(doc), {}, state);
	assert.equal(captured.title, 'Abort 1 changes');
	assert.equal(captured.body, 'This cannot be undone.');
	assert.equal(captured.confirmLabel, 'Abort');
	assert.equal(captured.rows.length, 1);
	assert.equal(captured.rows[0].name, 'Alpha');
});

t('confirmBulkAction_fallsBackToBuiltInModal_listsByLabelField_capsAtTwenty', () => {
	const { doc, NS } = load();
	const ids = [];
	const state = stateOf('persistent', []);
	state.labelField = 'name';
	for (let i = 1; i <= 25; i++) { ids.push('r' + i); state.snapshots['r' + i] = { id: 'r' + i, name: 'Name' + i }; }
	state.selected = new Set(ids);
	NS.confirmBulkAction({ id: 'abort', label: 'Abort', confirmTitle: 'Abort {count} changes' }, freshTable(doc), {}, state);
	const modal = doc.querySelector('.juneau-view-confirm-modal');
	assert.ok(modal, 'fallback modal is on the layer stack');
	const items = modal.querySelectorAll('li');
	assert.equal(items.length, 21);
	assert.equal(items[0].textContent, 'Name1');
	assert.equal(items[20].textContent, '\u2026and 5 more');
	assert.ok(modal.textContent.includes('Abort 25 changes'));
});

t('confirmBulkAction_fallbackConfirmRunsTheAction_cancelDoesNot', () => {
	const { doc, NS } = load();
	const state = stateOf('persistent', ['r1']);
	const table = freshTable(doc);
	// No runBulkAction yet (a later task): the confirmed action falls through to the per-row executor, which finds
	// no on-screen row for r1 and does nothing - what is asserted here is the modal lifecycle.
	NS.confirmBulkAction({ id: 'abort', label: 'Abort' }, table, {}, state);
	let modal = doc.querySelector('.juneau-view-confirm-modal');
	modal.querySelector('.juneau-view-dialog-cancel').dispatch('click', {});
	assert.equal(doc.querySelector('.juneau-view-confirm-modal'), null);
	NS.confirmBulkAction({ id: 'abort', label: 'Abort' }, table, {}, state);
	modal = doc.querySelector('.juneau-view-confirm-modal');
	modal.querySelector('.juneau-view-dialog-confirm').dispatch('click', {});
	assert.equal(doc.querySelector('.juneau-view-confirm-modal'), null);
});

t('confirmBulkAction_confirmFalse_skipsDialogEntirely', () => {
	const { doc, env, NS } = load();
	let dialogCalled = false;
	env.window.JuneauViews.dialogs = { confirm: () => { dialogCalled = true; } };
	NS.confirmBulkAction({ id: 'retry', label: 'Retry', confirm: false }, freshTable(doc), {}, stateOf('persistent', ['r1']));
	assert.equal(dialogCalled, false);
	assert.equal(doc.querySelector('.juneau-view-confirm-modal'), null);
});

t('wireToolbarRightClusterBulk_appendsToRightClusterOnly', () => {
	const { doc, NS } = load();
	const row = doc.createElement('div');
	const left = doc.createElement('div'); left.className = 'juneau-view-toolbar-left';
	const right = doc.createElement('div'); right.className = 'juneau-view-toolbar-right';
	row.appendChild(left); row.appendChild(right);
	const bulkEl = doc.createElement('div');
	NS.wireToolbarRightClusterBulk(row, { bulkToolbar: { el: bulkEl } });
	assert.equal(bulkEl.parentNode, right);
	NS.wireToolbarRightClusterBulk(null, { bulkToolbar: { el: bulkEl } });
	NS.wireToolbarRightClusterBulk(row, {});
});

// ---- Task 10: bulk execution -------------------------------------------------------------------------------------

const { jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

/** A table with `ids` rows (each checked + selected), a CSRF token, and a ctx carrying a recording bulkToolbar. */
function bulkFixture(ids, scope) {
	const L = load();
	const table = freshTable(L.doc);
	table.setAttribute('data-juneau-csrf', 'tok');
	ids.forEach(id => { addRow(L.doc, table, id).querySelector('.juneau-view-select-checkbox').checked = true; });
	const state = stateOf(scope || 'persistent', ids);
	ids.forEach(id => { state.snapshots[id] = { id }; });
	const toolbarCounts = [];
	const ctx = { table, selectionState: state, bulkToolbar: { refresh: n => toolbarCounts.push(n) } };
	return Object.assign(L, { table, state, ctx, toolbarCounts });
}

const perRowAction = { id: 'abort', label: 'Abort', method: 'POST', endpoint: '/api/changes/{id}/abort' };
const toastsOf = doc => Array.from(doc.querySelectorAll('.juneau-view-bulk-toast'));

t('runBulkAction_perRow_allSucceed_clearsSelectionAndShowsStatusToast', async () => {
	const f = bulkFixture(['a', 'b', 'c']);
	const calls = [];
	f.env.setFetch((url, init) => { calls.push({ url, init }); return Promise.resolve(jsonResponse('', { status: 200 })); });
	await f.NS.runBulkAction(perRowAction, f.table, f.ctx, f.state, ['a', 'b', 'c']);
	assert.equal(calls.length, 3);
	assert.deepEqual(calls.map(c => c.url).sort(), ['/api/changes/a/abort', '/api/changes/b/abort', '/api/changes/c/abort']);
	assert.equal(JSON.parse(calls[0].init.body).targetId !== undefined, true);
	assert.equal(f.state.selected.size, 0);
	assert.deepEqual(f.toolbarCounts[f.toolbarCounts.length - 1], 0);
	const toasts = toastsOf(f.doc);
	assert.equal(toasts.length, 0, 'status toast auto-dismisses (immediate-fire timer in the shim)');
});

t('runBulkAction_perRow_failedStaysSelected_notFoundCleared_alertToastSticky', async () => {
	const f = bulkFixture(['a', 'b', 'c']);
	f.env.setFetch(url => Promise.resolve(
		url.includes('/b/') ? jsonResponse('', { status: 500 }) : url.includes('/c/') ? jsonResponse('', { status: 404 }) : jsonResponse('', { status: 200 })));
	await f.NS.runBulkAction(perRowAction, f.table, f.ctx, f.state, ['a', 'b', 'c']);
	assert.deepEqual(Array.from(f.state.selected), ['b']);
	assert.equal(f.state.snapshots.b !== undefined, true);
	assert.equal(f.state.snapshots.a, undefined);
	const toast = toastsOf(f.doc)[0];
	assert.equal(toast.getAttribute('role'), 'alert');
	assert.equal(toast.textContent.includes('Abort: 1 succeeded, 1 not found, 1 failed (ids: b)'), true, toast.textContent);
	assert.equal(f.doc.querySelector('.juneau-view-announcer').textContent.includes('Abort: 1 succeeded'), true);
});

t('runBulkAction_perRow_concurrencyNeverExceedsFour', async () => {
	const ids = Array.from({ length: 10 }, (_, i) => 'r' + i);
	const f = bulkFixture(ids);
	let live = 0, peak = 0;
	f.env.setFetch(() => {
		live++; peak = Math.max(peak, live);
		return Promise.resolve().then(() => Promise.resolve()).then(() => { live--; return jsonResponse('', { status: 200 }); });
	});
	await f.NS.runBulkAction(perRowAction, f.table, f.ctx, f.state, ids);
	assert.equal(peak, 4);
	assert.equal(f.state.selected.size, 0);
});

t('runBulkAction_perRow_offScreenRowUsesSnapshotForEndpointTokens', async () => {
	const f = bulkFixture(['a']);
	f.state.selected.add('zz');
	f.state.snapshots.zz = { id: 'zz' };
	const urls = [];
	f.env.setFetch(url => { urls.push(url); return Promise.resolve(jsonResponse('', { status: 200 })); });
	await f.NS.runBulkAction(perRowAction, f.table, f.ctx, f.state, ['a', 'zz']);
	assert.deepEqual(urls.sort(), ['/api/changes/a/abort', '/api/changes/zz/abort']);
});

t('runBulkAction_perRow_missingCsrfToken_failsClosedWithoutFetch', async () => {
	const f = bulkFixture(['a']);
	f.table.removeAttribute('data-juneau-csrf');
	let called = 0;
	f.env.setFetch(() => { called++; return Promise.resolve(jsonResponse('', { status: 200 })); });
	await f.NS.runBulkAction(perRowAction, f.table, f.ctx, f.state, ['a']);
	assert.equal(called, 0);
	assert.deepEqual(Array.from(f.state.selected), ['a']);
});

t('runBulkAction_aggregate_postsIdsAndIdempotencyKey_appliesBulkResult', async () => {
	const f = bulkFixture(['a', 'b', 'c']);
	const agg = { id: 'abort', label: 'Abort', method: 'POST', endpoint: '/api/changes/abort', bulkMode: 'aggregate' };
	let sent;
	f.env.setFetch((url, init) => {
		sent = { url, body: JSON.parse(init.body) };
		return Promise.resolve(jsonResponse({ contractVersion: '1', succeeded: ['a'], notFound: ['b'], failed: [{ id: 'c', message: 'merged' }] }));
	});
	await f.NS.runBulkAction(agg, f.table, f.ctx, f.state, ['a', 'b', 'c']);
	assert.equal(sent.url, '/api/changes/abort');
	assert.deepEqual(sent.body.ids, ['a', 'b', 'c']);
	assert.equal(typeof sent.body.idempotencyKey, 'string');
	assert.equal(Object.keys(sent.body).sort().join(','), 'idempotencyKey,ids');
	assert.deepEqual(Array.from(f.state.selected), ['c']);
	assert.equal(toastsOf(f.doc)[0].textContent.includes('Abort: 1 succeeded, 1 not found, 1 failed (ids: c)'), true);
});

t('runBulkAction_aggregate_transportFailure_E_JS_26_keepsSelection', async () => {
	const f = bulkFixture(['a']);
	const agg = { id: 'abort', label: 'Abort', method: 'POST', endpoint: '/api/changes/abort', bulkMode: 'aggregate' };
	f.env.setFetch(() => Promise.resolve(jsonResponse('', { status: 503 })));
	await f.NS.runBulkAction(agg, f.table, f.ctx, f.state, ['a']);
	assert.deepEqual(Array.from(f.state.selected), ['a']);
	const toast = toastsOf(f.doc)[0];
	assert.equal(toast.getAttribute('role'), 'alert');
	assert.equal(toast.textContent.includes("bulk action 'abort' on table"), true, toast.textContent);
});

t('runBulkAction_aggregate_nonBulkResult_E_JS_27_keepsSelection', async () => {
	const f = bulkFixture(['a']);
	const agg = { id: 'abort', label: 'Abort', method: 'POST', endpoint: '/api/changes/abort', bulkMode: 'aggregate' };
	f.env.setFetch(() => Promise.resolve(jsonResponse({ contractVersion: '9', ok: true })));
	await f.NS.runBulkAction(agg, f.table, f.ctx, f.state, ['a']);
	assert.deepEqual(Array.from(f.state.selected), ['a']);
	assert.equal(toastsOf(f.doc)[0].textContent.includes("response is not a BulkResult (contractVersion '9')"), true);
});

t('buildBulkSummaryMessage_omitsZeroOptionalClauses', () => {
	const { NS } = load();
	assert.equal(NS.buildBulkSummaryMessage('Abort', 3, 0, []), 'Abort: 3 succeeded');
	assert.equal(NS.buildBulkSummaryMessage('Abort', 1, 2, ['x', 'y']), 'Abort: 1 succeeded, 2 not found, 2 failed (ids: x, y)');
});

(async () => {
	let failed = 0;
	for (const c of cases) {
		try {
			await c.fn();
			console.log('ok   ' + c.name);
		} catch (e) {
			failed++;
			console.error('FAIL ' + c.name + '\n' + (e && e.stack || e));
		}
	}
	if (failed) process.exit(1);
	console.log('bulk-selection.cjs: ' + cases.length + ' cases passed');
})();
