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
 * confirm-without-dialog.cjs - always-on Node harness: a row action that declares `confirm` (or `confirmRenderer`)
 * but no `present: "dialog"` must still prompt before it submits, from every surface that dispatches a row action -
 * the row-action menu, a cell pill, a detail-panel ActionBar button and JuneauViews.rowActions.run.  Cancel sends
 * nothing; Confirm sends exactly one request.  An action with no confirm, or with `confirm: false`, still submits
 * directly.  Every assertion lives in the Java test (ViewsJs_ConfirmWithoutDialog_Test).
 *
 *   Usage: node confirm-without-dialog.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node confirm-without-dialog.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, NS, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
const calls = [];
env.setFetch(function (url, opts) {
	calls.push({ url: url, method: opts.method, body: JSON.parse(opts.body) });
	return Promise.resolve(jsonResponse({ outcome: 'success' }));
});

const ROW = { id: 7, name: 'Rex' };
const tick = function () { return new Promise(function (r) { setTimeout(r, 5); }); };
function drain() { while (I.topLayer()) I.popLayer(); }

function deleteAction(extra) {
	return Object.assign({ id: 'delete', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE',
		confirm: "Delete '{name}'?", confirmLabel: 'Delete', tone: 'danger' }, extra || {});
}

/** One view table with one row, plus the bits each surface needs to find the row and the action. */
function fixture(action) {
	const host = env.el('div');
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-1';
	table.setAttribute('data-juneau-row-id-field', 'id');
	const tpl = env.el('template');   // a sibling row-detail template, so initDetailsExpander wires the detail delegate
	tpl.dataset.juneauRowDetail = '1';
	host.appendChild(table);
	host.appendChild(tpl);
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	tr.dataset.juneauRowId = String(ROW.id);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(host);
	const viewDef = { id: 'v', rowActions: [action] };
	const ctx = { viewDef: viewDef, dataTable: { row: function () { return { data: function () { return ROW; } }; } } };
	table.__juneauCtx = ctx;
	return { table: table, tr: tr, viewDef: viewDef, ctx: ctx };
}

/** The open dialog's title text, or null when none is open. */
function openTitle() {
	if (I.dialogLayerCount() !== 1) return null;
	const t = env.body.querySelector('.juneau-view-dialog-title');
	return t ? t.textContent : null;
}

function clickIn(selector) {
	const b = env.body.querySelector(selector);
	if (b) b.dispatch('click', {});
	return b != null;
}

/** The four surfaces, each a function that activates `action` on a fresh fixture. */
const SURFACES = {
	menu: function (fx) {
		const menu = I.buildRowActionMenu(fx.viewDef, fx.table, fx.tr, fx.ctx);
		fx.tr.appendChild(menu);
		menu.querySelector('.juneau-view-action-item').dispatch('click', {});
	},
	pill: function (fx) {
		const pill = env.el('span');
		pill.dataset.juneauPill = '1';
		pill.dataset.juneauAction = fx.viewDef.rowActions[0].id;
		fx.tr.appendChild(pill);
		I.activatePillAction(pill, fx.table, fx.viewDef, fx.ctx);
	},
	detail: function (fx) {
		const panel = env.el('div');
		panel.className = 'juneau-view-detail-panel';
		panel._juneauParentTr = fx.tr;
		const btn = env.el('button');
		btn.dataset.juneauAction = fx.viewDef.rowActions[0].id;
		panel.appendChild(btn);
		I.initDetailsExpander(fx.table, fx.ctx, fx.viewDef);
		fx.table.dispatch('click', { target: btn, preventDefault: function () { /* no-op */ }, stopPropagation: function () { /* no-op */ } });
	},
	run: function (fx) {
		const btn = env.el('button');
		fx.tr.appendChild(btn);
		const closest = btn.closest;
		btn.closest = function (sel) { return sel === 'tbody tr' ? fx.tr : closest.call(this, sel); };
		return NS.rowActions.run(btn, fx.viewDef.rowActions[0].id);
	}
};

(async function () {
	for (const name of Object.keys(SURFACES)) {
		// Cancel: the prompt appears, nothing is sent before or after.
		calls.length = 0;
		await SURFACES[name](fixture(deleteAction()));
		await tick();
		const cancel = { fetchesAtClick: calls.length, title: openTitle() };
		cancel.cancelled = clickIn('.juneau-view-dialog-cancel');
		await tick();
		cancel.fetchesAfterCancel = calls.length;
		cancel.closed = I.dialogLayerCount() === 0;
		drain();

		// Confirm: the prompt appears, and confirming sends exactly one DELETE for this row.
		calls.length = 0;
		await SURFACES[name](fixture(deleteAction()));
		await tick();
		const confirm = { fetchesAtClick: calls.length, title: openTitle() };
		confirm.confirmed = clickIn('.juneau-view-dialog-confirm');
		await tick();
		confirm.fetches = calls.length;
		confirm.url = calls[0]?.url;
		confirm.method = calls[0]?.method;
		confirm.action = calls[0]?.body.action;
		drain();

		// Controls: no confirm, and confirm:false (noConfirm), keep the direct submit.
		calls.length = 0;
		await SURFACES[name](fixture(deleteAction({ confirm: undefined })));
		await tick();
		const plain = { fetches: calls.length, dialogs: I.dialogLayerCount() };
		drain();
		calls.length = 0;
		await SURFACES[name](fixture(deleteAction({ confirm: false })));
		await tick();
		const noConfirm = { fetches: calls.length, dialogs: I.dialogLayerCount() };
		drain();

		out[name] = { cancel: cancel, confirm: confirm, plain: plain, noConfirm: noConfirm };
	}

	// A named confirmRenderer with no confirm text and no present: the renderer is asked (titled with the action's
	// name), and its answer gates the write.
	let verdict = false;
	const asked = [];
	NS.rowActions.registerConfirmRenderer('verdict', function (modal) { asked.push(modal.title); return Promise.resolve(verdict); });
	calls.length = 0;
	await SURFACES.menu(fixture(deleteAction({ confirm: undefined, confirmRenderer: 'verdict' })));
	await tick();
	const rendererNo = { asked: asked.length, fetches: calls.length };
	verdict = true;
	await SURFACES.menu(fixture(deleteAction({ confirm: undefined, confirmRenderer: 'verdict' })));
	await tick();
	out.renderer = { no: rendererNo, yes: { asked: asked.length, fetches: calls.length }, titles: asked };
	drain();

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
