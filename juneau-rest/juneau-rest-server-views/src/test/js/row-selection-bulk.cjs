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
 * row-selection-bulk.cjs - real-browser prober for the juneau-views.js row-selection + bulk-mutation contract.
 *
 * Never runs in a default build.  It is driven by RowSelectionBulk_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node row-selection-bulk.cjs <page.html>
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL served juneau-views.js - in
 * headless Chromium, then, entirely inside the page, drives the exposed selection/bulk helpers directly (no
 * jQuery/DataTables is bundled by this module, so - mirroring row-actions.cjs and modal-result.cjs - this prober
 * never boots a real DataTable; it exercises initSelection/runBulkAction/etc. against a fabricated DOM +
 * fetch, which is exactly the same binding surface initTable itself calls into).  Prints ONE JSON object to
 * stdout.
 *
 * DIVISION OF LABOUR (mirrors row-actions.cjs / modal-result.cjs): this script only OBSERVES; every assertion
 * lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/* Runs inside the page.  Async: the bulk-settle path reads response bodies via promises. */
const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS?.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	out.bulkContractVersion = NS.BULK_CONTRACT_VERSION;

	const tick = () => new Promise(r => setTimeout(r, 0));

	// Builds a table with one row per id in `rowIds`, each carrying the stable ROW_ID_ATTR (never a DOM index),
	// a selection-cell checkbox, and an actions-cell + trigger (for the bulk-settle path to render into).
	function makeTable(rowIds, opts) {
		opts = opts || {};
		const table = document.createElement('table');
		table.dataset.juneauView = 'v';
		if (opts.select) table.setAttribute(init.SELECT_ATTR, '1');
		if (opts.select && opts.selectAll !== false) table.setAttribute(init.SELECT_ALL_ATTR, '1');
		if (opts.rowIdField) table.setAttribute(init.ROW_ID_FIELD_ATTR, opts.rowIdField);
		if (opts.bulk) table.setAttribute(init.BULK_ATTR, '1');
		if (opts.csrf) table.dataset.juneauCsrf = opts.csrf;

		const thead = document.createElement('thead');
		const headRow = document.createElement('tr');
		const selectTh = document.createElement('th');
		selectTh.className = 'juneau-view-select-th';
		headRow.appendChild(selectTh);
		thead.appendChild(headRow);
		table.appendChild(thead);

		const tbody = document.createElement('tbody');
		const trs = {};
		rowIds.forEach(function (id) {
			const tr = document.createElement('tr');
			tr.setAttribute(init.ROW_ID_ATTR, id);
			const selectTd = document.createElement('td');
			selectTd.innerHTML = opts.disabledIds?.includes(id) ? init.selectionCellMarkup(false, id, 'Not eligible') : init.selectionCellMarkup(false);
			tr.appendChild(selectTd);
			const actionsTd = document.createElement('td');
			actionsTd.className = 'juneau-view-actions-cell';
			const trigger = document.createElement('button');
			trigger.className = 'juneau-view-action-trigger';
			actionsTd.appendChild(trigger);
			tr.appendChild(actionsTd);
			tbody.appendChild(tr);
			trs[id] = tr;
		});
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, trs: trs, selectTh: selectTh };
	}

	// A minimal fake DataTables instance exposing only `.on(event, cb)` - just enough for initSelection's
	// draw.dt-driven off-screen-id-drop wiring; `.fire` lets the probe simulate a poll/sort/page tick.
	// NOSONAR javascript:S7721 -- stays nested inside PROBE: page.evaluate() serializes this function
	// source across the Playwright process boundary with no access to outer Node-module scope, so it
	// cannot be hoisted to module level without breaking in-browser execution.
	function fakeDt() { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		const handlers = {};
		return {
			on: function (evt, cb) {
				handlers[evt] = handlers[evt] || [];
				handlers[evt].push(cb);
			},
			fire: function (evt) { (handlers[evt] || []).forEach(function (cb) { cb(); }); }
		};
	}

	// NOSONAR javascript:S7721 -- stays nested inside PROBE for the same cross-process-boundary reason
	// as fakeDt() above.
	function dispatchChange(el) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		el.dispatchEvent(new Event('change', { bubbles: true }));
	}

	// ---- a) per-row selection + select-all (MED-11: identity is the stable id, never a DOM index) ----
	{
		const dom = makeTable(['1', '2', '3'], { select: true, rowIdField: 'id' });
		const selectionState = { selected: new Set(), rowIdField: 'id' };
		const ctx = { selectionState: selectionState, dataTable: fakeDt(), bulkToolbar: null };
		init.ensureSelectAllCheckbox(dom.table);
		init.bindSelectionPrune(dom.table, ctx);
		init.initSelection(dom.table, ctx);

		function check(id, checked) {
			const cb = dom.trs[id].querySelector('.juneau-view-select-checkbox');
			cb.checked = checked;
			dispatchChange(cb);
		}

		check('1', true);
		check('2', true);
		out.afterTwoChecked = Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));

		const allCb = dom.selectTh.querySelector('.juneau-view-select-all-checkbox');
		out.hasSelectAllCheckbox = !!allCb;
		if (allCb) {
			allCb.checked = true;
			dispatchChange(allCb);
			out.afterSelectAll = Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));

			allCb.checked = false;
			dispatchChange(allCb);
			out.afterDeselectAll = Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));
		}
	}

	// ---- b) Scope: a draw that removes a row prunes it only under scope "page"; "persistent" (default) keeps it ----
	function offScreenDraw(scope) {
		const dom = makeTable(['1', '2', '3'], { select: true, rowIdField: 'id' });
		const selectionState = { selected: new Set(['1', '2', '3']), rowIdField: 'id', scope: scope, snapshots: {}, selectableWhen: null };
		const dt = fakeDt();
		const ctx = { selectionState: selectionState, dataTable: dt, bulkToolbar: null };
		init.ensureSelectAllCheckbox(dom.table);
		init.bindSelectionPrune(dom.table, ctx);
		init.initSelection(dom.table, ctx);
		dom.trs['3'].remove();   // row '3' left the current draw
		dt.fire('draw.dt');
		return Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));
	}
	out.persistentSelectedAfterOffScreenDraw = offScreenDraw('persistent');
	out.pageScopeSelectedAfterOffScreenDraw = offScreenDraw('page');

	// ---- b2) tri-state select-all header + a selectableWhen-disabled row ----
	{
		const dom = makeTable(['1', '2', '3'], { select: true, rowIdField: 'id', disabledIds: ['3'] });
		const selectionState = { selected: new Set(), rowIdField: 'id', scope: 'persistent', snapshots: {}, selectableWhen: null };
		const ctx = { selectionState: selectionState, dataTable: fakeDt(), bulkToolbar: null };
		init.ensureSelectAllCheckbox(dom.table);
		init.bindSelectionPrune(dom.table, ctx);
		init.initSelection(dom.table, ctx);
		const allCb = dom.selectTh.querySelector('.juneau-view-select-all-checkbox');
		const cbOf = id => dom.trs[id].querySelector('.juneau-view-select-checkbox');
		const sel = (id, checked) => { cbOf(id).checked = checked; dispatchChange(cbOf(id)); };

		out.headerCheckedInitially = allCb.checked;
		out.headerIndeterminateInitially = allCb.indeterminate;
		sel('1', true);
		out.headerCheckedAfterSomeSelected = allCb.checked;
		out.headerIndeterminateAfterSomeSelected = allCb.indeterminate;
		sel('2', true);   // row '3' is disabled, so '1' and '2' are every SELECTABLE row
		out.headerCheckedAfterAllSelected = allCb.checked;
		out.headerIndeterminateAfterAllSelected = allCb.indeterminate;

		sel('1', false); sel('2', false);
		allCb.checked = true;
		dispatchChange(allCb);
		out.afterSelectAllWithOneDisabledRow = Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));

		const disabledCb = cbOf('3');
		out.disabledCheckboxHasTitle = disabledCb.disabled && !!disabledCb.getAttribute('title');
		out.disabledCheckboxHasAriaDescribedBy = !!disabledCb.getAttribute('aria-describedby');
	}

	// ---- c) two INDEPENDENT opt-ins (HIGH-5): a selection-only (export) table never carries the bulk marker ----
	{
		const selectOnly = makeTable(['1'], { select: true, rowIdField: 'id', bulk: false });
		const withBulk = makeTable(['1'], { select: true, rowIdField: 'id', bulk: true });
		out.selectOnlyHasSelection = init.hasSelection(selectOnly.table);
		out.selectOnlyHasBulk = init.hasBulk(selectOnly.table);
		out.withBulkHasSelection = init.hasSelection(withBulk.table);
		out.withBulkHasBulk = init.hasBulk(withBulk.table);
	}

	// ---- shared helpers for the toolbar / confirm / run scenarios ----
	const flush = async function () { for (let i = 0; i < 8; i++) await tick(); };
	const clearOverlays = function () {
		document.querySelectorAll('.juneau-view-bulk-toast, .juneau-view-confirm-modal, .juneau-view-announcer')
			.forEach(function (n) { n.remove(); });
	};
	const bulkAction = function (extra) {
		return Object.assign({ id: 'abort', label: 'Abort', method: 'POST', endpoint: '/x/{id}/abort' }, extra);
	};
	// A selected-rows fixture: `n` rows ids '1'..'n', all checked + selected, each with a snapshot (so {id} resolves
	// and the confirm dialog can label it).
	function bulkFixture(n, selectionOpts) {
		const ids = Array.from({ length: n }, function (_, i) { return String(i + 1); });
		const dom = makeTable(ids, { select: true, rowIdField: 'id', bulk: true, csrf: 'tok-xyz' });
		const selectionState = Object.assign({ selected: new Set(ids), rowIdField: 'id', scope: 'persistent',
			labelField: 'name', snapshots: {}, selectableWhen: null }, selectionOpts);
		ids.forEach(function (id) {
			selectionState.snapshots[id] = { id: id, name: 'Change ' + id };
			dom.trs[id].querySelector('.juneau-view-select-checkbox').checked = true;
		});
		let reloads = 0;
		const dt = { ajax: { reload: function () { reloads++; } } };
		const ctx = { selectionState: selectionState, dataTable: dt, bulkToolbar: null };
		return { dom: dom, ids: ids, selectionState: selectionState, ctx: ctx, reloads: function () { return reloads; } };
	}
	// Installs a fetch stub for the duration of `fn`; `handler(url, body, opts)` returns {status, body} or a Promise.
	async function withFetch(handler, fn) {
		const realFetch = window.fetch;
		const calls = [];
		window.fetch = function (url, opts) {
			const body = opts?.body ? JSON.parse(opts.body) : null;
			calls.push({ url: url, body: body, headers: opts?.headers || {} });
			return Promise.resolve(handler(url, body, opts)).then(function (r) {
				return {
					ok: r.status >= 200 && r.status < 300, status: r.status,
					headers: { get: function () { return null; } },
					text: function () { return Promise.resolve(typeof r.body === 'string' ? r.body : JSON.stringify(r.body ?? '')); }
				};
			});
		};
		try { await fn(calls); } finally { window.fetch = realFetch; }
		return calls;
	}
	const toastOf = function () { return document.querySelector('.juneau-view-bulk-toast'); };
	const announcerText = function () { return document.querySelector('.juneau-view-announcer')?.textContent ?? null; };
	const sortedIds = function (set) { return Array.from(set).sort((a, b) => a.localeCompare(b)); };

	// ---- c2) toolbar: single action = button, several = dropdown + Go; lives in the RIGHT cluster; hidden at zero ----
	{
		const dom = makeTable(['1', '2'], { select: true, rowIdField: 'id', bulk: true });
		const selectionState = { selected: new Set(), rowIdField: 'id', scope: 'persistent', snapshots: {}, selectableWhen: null };
		const one = init.buildBulkToolbar({ contractVersion: NS.BULK_CONTRACT_VERSION, actions: [bulkAction()] }, dom.table, {}, selectionState);
		const many = init.buildBulkToolbar({ contractVersion: NS.BULK_CONTRACT_VERSION,
			actions: [bulkAction(), bulkAction({ id: 'retry', label: 'Retry' })] }, dom.table, {}, selectionState);
		const row = document.createElement('div');
		row.innerHTML = '<div class="juneau-view-toolbar-left"></div><div class="juneau-view-toolbar-right"></div>';
		init.wireToolbarRightClusterBulk(row, { bulkToolbar: one });
		document.body.appendChild(row);
		document.body.appendChild(many.el);

		out.toolbar = { hiddenInitially: one.el.hidden };
		one.refresh(2);
		many.refresh(2);
		out.toolbar.hiddenWithSelection = one.el.hidden;
		out.toolbar.countTextWithSelection = one.el.querySelector('.juneau-view-bulk-count').textContent;
		out.singleActionIsButton = !!one.el.querySelector('button.juneau-view-bulk-action-btn');
		out.singleActionHasDropdown = !!one.el.querySelector('select');
		out.singleActionButtonTextWithTwoSelected = one.el.querySelector('.juneau-view-bulk-action-btn').textContent;
		out.toolbarIsInRightCluster = !!one.el.closest('.juneau-view-toolbar-right') && !one.el.closest('.juneau-view-toolbar-left');

		out.multiActionHasDropdown = !!many.el.querySelector('select.juneau-view-bulk-select');
		const go = many.el.querySelector('.juneau-view-bulk-go');
		const choose = many.el.querySelector('select');
		out.goDisabledBeforeChoice = go.disabled;
		choose.value = 'retry';
		choose.dispatchEvent(new Event('change', { bubbles: true }));
		out.goDisabledAfterChoice = go.disabled;

		one.el.querySelector('.juneau-view-bulk-clear').click();
		out.toolbar.hiddenAfterCleared = one.el.hidden;
		out.afterClearSelectionLinkClicked = Array.from(selectionState.selected);
		one.el.remove(); many.el.remove(); row.remove();
	}

	// ---- d) the mandatory confirm dialog (built-in fallback: this page defines no JuneauViews.dialogs.confirm) ----
	{
		clearOverlays();
		const f = bulkFixture(25);
		const action = bulkAction();
		const tb = init.buildBulkToolbar({ contractVersion: NS.BULK_CONTRACT_VERSION, actions: [action] }, f.dom.table, f.ctx, f.selectionState);
		f.ctx.bulkToolbar = tb;
		document.body.appendChild(tb.el);
		tb.refresh(f.ids.length);
		const calls = await withFetch(function () { return { status: 200, body: '' }; }, async function (c) {
			tb.el.querySelector('.juneau-view-bulk-action-btn').click();
			await flush();
			const modal = document.querySelector('.juneau-view-confirm-modal');
			out.confirmModalShown = !!modal;
			out.confirmModalListText = modal ? modal.textContent : null;
			out.fetchCountBeforeConfirm = c.length;
			document.querySelector('.juneau-view-dialog-cancel')?.click();
			await flush();
			out.fetchCountAfterCancel = c.length;
			out.modalGoneAfterCancel = !document.querySelector('.juneau-view-confirm-modal');
		});
		out.selectionKeptAfterCancel = f.selectionState.selected.size;

		clearOverlays();
		const g = bulkFixture(2);
		await withFetch(function () { return { status: 200, body: '' }; }, async function () {
			init.confirmBulkAction(bulkAction({ confirm: false }), g.dom.table, g.ctx, g.selectionState);
			out.noConfirmActionShowedModal = !!document.querySelector('.juneau-view-confirm-modal');
			await flush();
		});
		tb.el.remove();
	}

	// ---- e) PER_ROW (default bulkMode): N independent writes, one summary; 4-wide concurrency; onSuccess none ----
	{
		clearOverlays();
		const f = bulkFixture(3);
		const calls = await withFetch(function (url, body) {
			return { status: body.targetId === '2' ? 500 : body.targetId === '3' ? 404 : 200, body: '' };
		}, async function () {
			await init.runBulkAction(bulkAction(), f.dom.table, f.ctx, f.selectionState, f.ids);
			await flush();
		});
		out.perRowBulk = {
			fetchCount: calls.length,
			targetIds: calls.map(function (c) { return c.body.targetId; }).sort((a, b) => a.localeCompare(b)),
			actionIds: calls.map(function (c) { return c.body.action; }),
			urls: calls.map(function (c) { return c.url; }).sort((a, b) => a.localeCompare(b)),
			toastText: toastOf()?.querySelector('span')?.textContent ?? null,
			toastRole: toastOf()?.getAttribute('role') ?? null,
			announceText: announcerText(),
			selectionAfterApply: sortedIds(f.selectionState.selected),
			uncheckedIds: f.ids.filter(function (id) { return !f.dom.trs[id].querySelector('.juneau-view-select-checkbox').checked; }),
			reloadCalled: f.reloads() > 0
		};

		clearOverlays();
		const ok = bulkFixture(3);
		const delays = [];
		const realST = window.setTimeout;
		window.setTimeout = function (fn, ms) { delays.push(ms); return realST(fn, ms); };
		try {
			await withFetch(function () { return { status: 200, body: '' }; }, async function () {
				await init.runBulkAction(bulkAction(), ok.dom.table, ok.ctx, ok.selectionState, ok.ids);
			});
		} finally { window.setTimeout = realST; }
		out.perRowBulkOk = { toastRole: toastOf()?.getAttribute('role') ?? null, toastText: toastOf()?.querySelector('span')?.textContent ?? null,
			toastAutoDismissMs: delays.includes(6000) ? 6000 : null };

		clearOverlays();
		const wide = bulkFixture(10);
		let live = 0, peak = 0;
		await withFetch(function () {
			live++; peak = Math.max(peak, live);
			return new Promise(function (r) { setTimeout(function () { live--; r({ status: 200, body: '' }); }, 5); });
		}, async function () { await init.runBulkAction(bulkAction(), wide.dom.table, wide.ctx, wide.selectionState, wide.ids); });
		out.perRowConcurrency = { maxConcurrentInFlight: peak, selectionAfter: wide.selectionState.selected.size };

		clearOverlays();
		const none = bulkFixture(2);
		await withFetch(function () { return { status: 200, body: '' }; },
			async function () { await init.runBulkAction(bulkAction({ onSuccess: 'none' }), none.dom.table, none.ctx, none.selectionState, none.ids); });
		out.perRowBulkOnSuccessNone = { reloadCalled: none.reloads() > 0 };
	}

	// ---- g) AGGREGATE: one CSRF-protected POST of {ids, idempotencyKey}; the BulkResult drives the outcome ----
	{
		const errors = [];
		const realErr = console.error;
		console.error = function (m) { errors.push(String(m)); };
		const agg = bulkAction({ bulkMode: 'aggregate', endpoint: '/x/abort' });
		try {
			clearOverlays();
			const f = bulkFixture(3);
			const calls = await withFetch(function () {
				return { status: 200, body: { contractVersion: '1', succeeded: ['1', '2'], notFound: ['9'], failed: [{ id: '3', message: 'nope' }] } };
			}, async function () { await init.runBulkAction(agg, f.dom.table, f.ctx, f.selectionState, f.ids); await flush(); });
			out.aggregateBulk = {
				fetchCount: calls.length,
				requestBody_ids: calls[0]?.body?.ids,
				requestBody_idempotencyKey: calls[0]?.body?.idempotencyKey ?? null,
				requestBodyKeys: calls[0] ? Object.keys(calls[0].body).sort((a, b) => a.localeCompare(b)) : null,
				requestHadCsrfHeader: Object.values(calls[0]?.headers || {}).includes('tok-xyz'),
				resultSucceeded: ['1', '2'], resultNotFound: ['9'], resultFailedIds: ['3'],
				toastText: toastOf()?.querySelector('span')?.textContent ?? null,
				announceText: announcerText(),
				selectionAfterApply: sortedIds(f.selectionState.selected)
			};

			clearOverlays();
			const t = bulkFixture(2);
			await withFetch(function () { return Promise.reject(new Error('connection reset')); },
				async function () { await init.runBulkAction(agg, t.dom.table, t.ctx, t.selectionState, t.ids); await flush(); });
			out.aggregateTransportFailure = {
				banner: Array.from(document.querySelectorAll('.juneau-view-error')).map(function (n) { return n.textContent; }),
				toastRole: toastOf()?.getAttribute('role') ?? null,
				toastIsSticky: !!toastOf()?.querySelector('.juneau-view-bulk-toast-close'),
				consoleErrorText: errors[errors.length - 1] ?? null,
				selectionAfter: sortedIds(t.selectionState.selected)
			};

			clearOverlays();
			errors.length = 0;
			const b = bulkFixture(2);
			await withFetch(function () { return { status: 200, body: { ok: true } }; },
				async function () { await init.runBulkAction(agg, b.dom.table, b.ctx, b.selectionState, b.ids); await flush(); });
			out.aggregateBadResponseShape = { consoleErrorText: errors[errors.length - 1] ?? null, selectionAfter: sortedIds(b.selectionState.selected) };
		} finally { console.error = realErr; }
	}

	// ---- f) the bulk sidecar is read + contract-checked at runtime (R2: independent of VIEW_META) ----
	{
		const id = 'v-sidecar';
		const sidecar = document.createElement('script');
		sidecar.type = 'application/json';
		sidecar.id = init.BULK_SIDECAR_ID_PREFIX + id;
		sidecar.textContent = JSON.stringify({ contractVersion: NS.BULK_CONTRACT_VERSION, actions: [{ id: 'a' }] });
		document.body.appendChild(sidecar);
		const read = init.readBulkDef(id);
		out.bulkSidecar = {
			contractVersion: read ? read.contractVersion : null,
			actionCount: read?.actions ? read.actions.length : -1,
			missingSidecarReturnsNull: init.readBulkDef('does-not-exist') === null
		};
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node row-selection-bulk.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
