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
 * never boots a real DataTable; it exercises initSelection/executeBulkAction/etc. against a fabricated DOM +
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
			selectTd.innerHTML = init.selectionCellMarkup(false);
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
	function fakeDt() {
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
	function dispatchChange(el) {
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

	// ---- b) off-screen-id-drop persistence rule (MED-11/Q2): a poll/sort/page draw prunes ids no longer present ----
	{
		const dom = makeTable(['1', '2', '3'], { select: true, rowIdField: 'id' });
		const selectionState = { selected: new Set(['1', '2', '3']), rowIdField: 'id' };
		const dt = fakeDt();
		const ctx = { selectionState: selectionState, dataTable: dt, bulkToolbar: null };
		init.ensureSelectAllCheckbox(dom.table);
		init.bindSelectionPrune(dom.table, ctx);
		init.initSelection(dom.table, ctx);

		dom.trs['3'].remove();   // row '3' left the current draw
		dt.fire('draw.dt');

		out.selectedAfterOffScreenDraw = Array.from(selectionState.selected).sort((a, b) => a.localeCompare(b));
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

	// ---- d) bulk = N INDEPENDENT per-row writes; per-target typed result; per-row in-flight marker ----
	{
		const dom = makeTable(['1', '2', '3'], { select: true, rowIdField: 'id', bulk: true, csrf: 'tok-xyz' });
		// '9' has no corresponding row - simulates an id that went off-screen between the click and this run;
		// executeBulkAction must silently skip it rather than target a row that no longer exists.
		const selectionState = { selected: new Set(['1', '2', '3', '9']), rowIdField: 'id' };

		const fetchCalls = [];
		const inflightAtFetchTime = {};
		const realFetch = window.fetch;
		window.fetch = function (url, opts) {
			const body = JSON.parse(opts.body);
			fetchCalls.push({ url: url, body: body });
			inflightAtFetchTime[body.targetId] = dom.trs[body.targetId].dataset.juneauInflight !== undefined;
			let respBody;
			if (body.targetId === '1') respBody = { contractVersion: '1', outcome: 'success' };
			else if (body.targetId === '2') respBody = { contractVersion: '1', outcome: 'failure', message: 'nope' };
			else respBody = { contractVersion: '1', outcome: 'refusal', refusalCode: 'write-guard:not-armed' };
			return Promise.resolve({
				ok: true, status: 200,
				headers: { get: function () { return null; } },
				text: function () { return Promise.resolve(JSON.stringify(respBody)); }
			});
		};

		const action = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST' };
		init.executeBulkAction(action, dom.table, {}, selectionState);
		await tick(); await tick(); await tick(); await tick(); await tick();
		window.fetch = realFetch;

		function outcomeOf(id) {
			const cell = dom.trs[id].querySelector('.juneau-view-action-outcome');
			return cell ? { state: cell.dataset.state, text: cell.textContent } : null;
		}

		out.bulk = {
			fetchCount: fetchCalls.length,
			targetIds: fetchCalls.map(function (c) { return c.body.targetId; }).sort((a, b) => a.localeCompare(b)),
			actionIds: fetchCalls.map(function (c) { return c.body.action; }),
			inflightAtFetchTime: inflightAtFetchTime,
			inflightAfter: {
				'1': dom.trs['1'].dataset.juneauInflight !== undefined,
				'2': dom.trs['2'].dataset.juneauInflight !== undefined,
				'3': dom.trs['3'].dataset.juneauInflight !== undefined
			},
			outcome1: outcomeOf('1'),
			outcome2: outcomeOf('2'),
			outcome3: outcomeOf('3')
		};
	}

	// ---- e) the bulk toolbar reflects the live selection count and gates its buttons on it ----
	{
		const dom = makeTable(['1', '2'], { select: true, rowIdField: 'id', bulk: true });
		const selectionState = { selected: new Set(), rowIdField: 'id' };
		const bulkDef = { contractVersion: NS.BULK_CONTRACT_VERSION, actions: [{ id: 'ack', label: 'Acknowledge' }] };
		const toolbar = init.buildBulkToolbar(bulkDef, dom.table, {}, selectionState);
		const btn = toolbar.el.querySelector('.juneau-view-bulk-action-btn');
		out.toolbar = { disabledInitially: btn.disabled };
		toolbar.refresh(2);
		out.toolbar.disabledWithSelection = btn.disabled;
		out.toolbar.countTextWithSelection = toolbar.el.querySelector('.juneau-view-bulk-count').textContent;
		toolbar.refresh(0);
		out.toolbar.disabledAfterCleared = btn.disabled;
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
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
