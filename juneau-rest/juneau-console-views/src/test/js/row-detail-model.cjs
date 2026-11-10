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
 * row-detail-model.cjs - always-on Node harness for the object-model handles of a tabbed row detail: one DetailTab
 * handle per tab, strip activation through the handle, the loaded event with its count and handler actions.  Every
 * assertion lives in the paired Java test.
 *
 *   Usage:  node row-detail-model.cjs <renders> <views> <regions> <helpers> <bus> <model>
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const [rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath, busJsPath, modelJsPath] = process.argv.slice(2);
if (!modelJsPath) {
	console.error('usage: node row-detail-model.cjs <renders> <views> <regions> <helpers> <bus> <model>');
	process.exit(2);
}

const out = {};

const DETAIL = {
	mode: 'bean', endpoint: '/rd/pet/{id}', title: 'Pet {id}',
	tabs: [
		{ id: 'a', kind: 'fields', title: 'Alpha', fields: ['name'], model: { on: { loaded: 'app.loaded' } } },
		{ id: 'b', kind: 'fields', title: 'Beta', fields: ['name'], model: { on: { activated: 'app.activated' } } }
	],
	actions: [{ type: 'handler', id: 'go', icon: 'x', label: 'Go', handler: 'app.go' }],
	model: { on: { rendered: 'app.rendered' } }
};

async function scenario(detail, opts) {
	opts = opts || {};
	const { env, NS, I } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath, busJsPath: busJsPath, extraJsPaths: [modelJsPath] });
	const M = NS.model;
	const calls = [];
	NS.handlers.register('app.loaded', function (t, ev) { calls.push('loaded:' + t.kind + ':' + t.id + ':' + JSON.stringify(ev.payload)); });
	NS.handlers.register('app.activated', function (t, ev) { calls.push('activated:' + t.id); });
	NS.handlers.register('app.rendered', function (t, ev) { calls.push('rendered:' + t.kind); });
	NS.handlers.register('app.go', function (t, ev) { calls.push('go:' + t.kind + ':' + ev.type); if (opts.throwOnGo) throw new Error('boom'); });
	env.setFetch(function () {
		return Promise.resolve({ ok: true, status: 200, headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: {}, counts: { a: 4 } })); } });
	});

	const page = M.fromJSON({ title: 'T', cards: [{ id: 'c1', type: 'datatables', table: { detail: detail } }] });
	const root = env.el('div');
	env.body.appendChild(root);
	page._attach(root);
	const host = env.el('div');
	root.appendChild(host);
	page.card('c1')._attach(host);
	M.resolveHandlers();

	const table = env.el('table');
	table.dataset.juneauView = 'c1';
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	const tpl = I.buildDetailTemplate(detail, table);
	tpl.content = {
		cloneNode: function () {
			const wrap = env.el('div');
			I.buildDetailTemplate(detail, table).content.childNodes.slice().forEach(function (k) { wrap.appendChild(k); });
			return wrap;
		}
	};
	host.appendChild(tpl);

	const rowData = { id: 's-7', name: 'Rex' };
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = 's-7';
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);
	const state = { shown: false, panel: null };
	const child = function (panel) {
		state.panel = panel;
		return { show: function () {
			const ctr = env.el('tr'); const ctd = env.el('td');
			ctr.appendChild(ctd); ctd.appendChild(panel); tbody.appendChild(ctr); state.shown = true;
		}, hide: function () { state.shown = false; } };
	};
	child.isShown = function () { return state.shown; };
	child.hide = function () { state.shown = false; };
	const ctx = {
		table: table, viewDef: { id: 'c1', columns: [{ data: 'name', title: 'Name' }] },
		dataTable: { row: function () { return { length: 1, child: child, data: function () { return rowData; } }; } }
	};
	table.__juneauCtx = ctx;
	I.initDetailsExpander(table, ctx, ctx.viewDef);
	table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);
	const mt = NS.tables.handle(table);
	return { env, NS, I, M, calls, panel: state.panel, mt, ctx, table,
		detail: function () { const r = mt.row('s-7'); return r && r.detail; } };
}

function clickTab(panel, id) {
	const list = panel.querySelector('[role="tablist"]');
	const btn = panel.querySelectorAll('[role="tab"]').filter(function (b) { return b.dataset.juneauStripTab === id; })[0];
	list.dispatch('click', { target: btn, preventDefault: function () {}, stopPropagation: function () {} });
}

(async function main() {
	const s = await scenario(DETAIL);
	const d = s.detail();
	out.detail_exists = !!d;
	out.tab_handles = d ? d.tabs.map(function (t) { return t.id; }).join('|') : null;
	out.tab_lookup = !!(d && d.tab('b') && d.tab('b').id === 'b');
	out.loaded_once_with_count = s.calls.filter(function (c) { return c.indexOf('loaded:') === 0; }).join('|');
	out.rendered_fired = s.calls.indexOf('rendered:rowdetail') >= 0;

	// strip activation emits activated on the tab handle
	clickTab(s.panel, 'b');
	await flush(12);
	out.activated_emitted = s.calls.indexOf('activated:b') >= 0;

	// handle.activate() drives the strip
	d.tab('a').activate();
	await flush(6);
	const sel = s.panel.querySelectorAll('[role="tab"]').filter(function (b) { return b.getAttribute('aria-selected') === 'true'; });
	out.handle_activate_drives_strip = sel.length === 1 && sel[0].dataset.juneauStripTab === 'a';

	// handler action painted into the model actions box and invokes the registered handler
	const box = s.panel.querySelector('.juneau-model-actions');
	const goBtn = box && box.querySelector('[data-action-id="go"]');
	out.action_painted = !!goBtn;
	if (goBtn) goBtn.dispatch('click', {});
	out.action_invoked = s.calls.filter(function (c) { return c.indexOf('go:') === 0; }).join('|');
	out.no_duplicate_buttons = (box ? box.querySelectorAll('[data-action-id="go"]').length : -1) === 1;

	// a throwing handler is contained
	const t = await scenario(DETAIL, { throwOnGo: true });
	const tb = t.panel.querySelector('.juneau-model-actions');
	let threw = false;
	try { tb.querySelector('[data-action-id="go"]').dispatch('click', {}); } catch (e) { threw = true; }
	clickTab(t.panel, 'b');
	await flush(6);
	out.throwing_handler_contained = !threw && t.calls.indexOf('activated:b') >= 0;

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e); process.exit(1); });
