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
 * detail-action-dialog-routing.cjs - always-on Node harness for a regression: a detail-header/detail-panel
 * ActionBar ActionRef click (`handleDetailActionRefClick`) must honor a `.present("dialog")` action the SAME way
 * the row-action menu / cell-pill paths already do (`isDialogAction(action) ? openActionDialog(...) :
 * submitRowAction(...)`, see `buildRowActionMenu` / `activatePillAction`).  Before the fix, the detail path called
 * `submitRowAction(action, table, parentTr, ctx)` directly, unconditionally - no dialog, no `extra`, so no
 * `targetId` and no idempotency key ever reached `buildActionRequest`.
 *
 *   Usage:  node detail-action-dialog-routing.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node detail-action-dialog-routing.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: !!(I && typeof I.initDetailsExpander === 'function' && typeof I.isDialogAction === 'function'
		&& typeof I.openActionDialog === 'function' && typeof I.submitRowAction === 'function')
};
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function drain() { while (I.topLayer()) I.popLayer(); }
function flush() { return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 6) { return r(); } setTimeout(tick, 0); })(); }); }

/**
 * Builds ONE view table with an already-expanded detail panel (mirroring what `expandDetailRow` leaves behind:
 * `panel._juneauParentTr` pointing at the expanded row) hosting a single ActionBar ActionRef button for `action`,
 * and wires the real click-delegation listener via `initDetailsExpander` - exactly the production entry point
 * `handleDetailActionRefClick` is reached through.
 */
function buildFixture(action) {
	const table = env.el('table');
	table.setAttribute('data-juneau-csrf', 'tok-1');
	// A row-detail <template> must be a SIBLING of `table` for findRowDetailTemplate to recognize this as a
	// detail-bearing table at all (see initDetailsExpander's early-return guard).
	const host = env.el('div');
	const tpl = env.el('template');
	tpl.setAttribute('data-juneau-row-detail', '1');
	host.appendChild(table);
	host.appendChild(tpl);

	const parentTr = env.el('tr');
	parentTr.dataset.juneauRowId = 'row-42';

	const panel = env.el('div');
	panel.className = 'juneau-view-detail-panel';
	panel._juneauParentTr = parentTr;

	const actionBtn = env.el('button');
	actionBtn.setAttribute('data-juneau-action', action.id);
	panel.appendChild(actionBtn);

	const viewDef = { rowActions: [action] };
	const ctx = { dataTable: {} };
	I.initDetailsExpander(table, ctx, viewDef);
	return { table: table, ctx: ctx, viewDef: viewDef, panel: panel, actionBtn: actionBtn, parentTr: parentTr };
}

function clickAction(fx) {
	fx.table.dispatch('click', { target: fx.actionBtn, preventDefault: function () {}, stopPropagation: function () {} });
}

(async function main() {

	// --- A dialog-declared action clicked from the detail header MUST open the dialog seam, not submit direct ---
	(function () {
		const action = {
			id: 'esc', present: 'dialog', label: 'Escalate', method: 'POST', endpoint: '/rows/{id}/esc',
			confirm: 'Escalate this row?'
		};
		const fx = buildFixture(action);
		const fetchCalls = [];
		env.setFetch(function (url, opts) {
			fetchCalls.push({ url: url, opts: opts });
			return Promise.reject(new Error('must not fetch before the dialog is confirmed'));
		});

		clickAction(fx);

		// The bug: a direct submitRowAction(...) call fetches synchronously, before any dialog exists.
		out.dialogAction_fetchCallsBeforeConfirm = fetchCalls.length;
		out.dialogAction_dialogOpened = I.dialogLayerCount() === 1;
		const backdrop = env.body.querySelector('.juneau-view-dialog-backdrop');
		out.dialogAction_backdropPortalledToBody = backdrop != null && backdrop.parentNode === env.body;
		out.dialogAction_ctxTracksDialog = fx.ctx._actionDialog != null;
		const title = env.body.querySelector('.juneau-view-dialog-title');
		out.dialogAction_titleIsConfirmText = title != null && title.textContent === 'Escalate this row?';

		// Confirming now submits through submitActionDialog (untouched) - and that submit carries the row's
		// targetId, which only happens when the seam (submitActionDialog) was actually reached.
		fetchCalls.length = 0;
		env.setFetch(function (url, opts) {
			fetchCalls.push({ url: url, opts: opts });
			return Promise.resolve(jsonResponse({ outcome: 'success' }));
		});
		const confirmBtn = env.body.querySelector('.juneau-view-dialog-confirm');
		out.dialogAction_confirmBtnPresent = confirmBtn != null;
		if (confirmBtn) confirmBtn.dispatch('click', {});

		out.dialogAction_submitFiredOnConfirm = fetchCalls.length === 1;
		const body = fetchCalls.length === 1 ? JSON.parse(fetchCalls[0].opts.body) : {};
		out.dialogAction_submitCarriesAction = body.action === 'esc';
		out.dialogAction_submitCarriesTargetId = body.targetId === 'row-42';
		out.dialogAction_dialogClosedAfterConfirm = I.dialogLayerCount() === 0;
		drain();
	})();

	// --- Control: a NON-dialog detail action must keep going straight through submitRowAction, unchanged --------
	(function () {
		const action = { id: 'ack', label: 'Ack', method: 'POST', endpoint: '/rows/{id}/ack' };
		const fx = buildFixture(action);
		const fetchCalls = [];
		env.setFetch(function (url, opts) {
			fetchCalls.push({ url: url, opts: opts });
			return Promise.resolve(jsonResponse({ outcome: 'success' }));
		});

		clickAction(fx);

		out.nonDialogAction_fetchCallsAtClick = fetchCalls.length;
		out.nonDialogAction_noDialogOpened = I.dialogLayerCount() === 0;
		const body = fetchCalls.length === 1 ? JSON.parse(fetchCalls[0].opts.body) : {};
		out.nonDialogAction_bodyIsBareAction = fetchCalls.length === 1 && Object.keys(body).length === 1 && body.action === 'ack';
		drain();
	})();

	// --- A form-bearing dialog action: the read-only confirmation GET runs BEFORE any write, and the server- -----
	// --- minted idempotencyKey it returns rides the eventual submit alongside targetId (HIGH-8) ------------------
	await (async function () {
		const action = {
			id: 'esc2', present: 'dialog', label: 'Escalate', method: 'POST', endpoint: '/rows/{id}/esc2',
			form: '/data/x/esc2-form'
		};
		const fx = buildFixture(action);
		let getCalls = 0;
		const postBodies = [];
		env.setFetch(function (url, opts) {
			if ((opts && opts.method) === 'POST') {
				postBodies.push(opts.body);
				return Promise.resolve(jsonResponse({ outcome: 'success' }));
			}
			getCalls++;
			return Promise.resolve(jsonResponse({ title: 'Escalate?', idempotencyKey: 'idem-777' }));
		});

		clickAction(fx);
		await flush();

		out.formDialogAction_getFetchedForConfirmation = getCalls === 1;
		out.formDialogAction_noPostBeforeConfirm = postBodies.length === 0;
		out.formDialogAction_dialogOpened = I.dialogLayerCount() === 1;

		const confirmBtn = env.body.querySelector('.juneau-view-dialog-confirm');
		if (confirmBtn) confirmBtn.dispatch('click', {});

		out.formDialogAction_postFiredOnConfirm = postBodies.length === 1;
		const body = postBodies.length === 1 ? JSON.parse(postBodies[0]) : {};
		out.formDialogAction_targetIdAttached = body.targetId === 'row-42';
		out.formDialogAction_idempotencyKeyAttached = body.idempotencyKey === 'idem-777';
		drain();
	})();

	process.stdout.write(JSON.stringify(out));
})();
