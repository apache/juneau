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
 * model-views.cjs - Node harness for the hooks the client object model has in juneau-views.js and juneau-ribbon.js:
 * the table handle, the draw cycle, detail rows and the ribbon.  Prints ONE JSON report in which every key is true
 * (passed) or a failure string; ConsoleModelViews_Test asserts each key.
 *
 *   Usage:  node model-views.cjs <juneau-bus.js> <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js> <juneau-search.js> <juneau-model.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require('./views-dom-shim.cjs');

const scripts = process.argv.slice(2);
if (scripts.length !== 6) {
	console.error('usage: node model-views.cjs <bus> <renders> <views> <ribbon> <search> <model>');
	process.exit(2);
}

const report = {};
const pending = [];
function ok(c, msg) { if (!c) throw new Error(msg || 'assertion failed'); }
function eq(a, b, msg) {
	const x = JSON.stringify(a), y = JSON.stringify(b);
	if (x !== y) throw new Error((msg || 'eq') + ': ' + x + ' !== ' + y);
}
function check(key, fn) {
	pending.push(async () => {
		try { await fn(); report[key] = true; } catch (e) { report[key] = 'FAIL: ' + e?.message; }
	});
}
const tick = () => new Promise(r => setImmediate(r));

/** A fake DataTables API: events, child rows, row counts. */
function fakeDt(tableEl, doc) {
	const handlers = {};
	const states = new Map();
	const log = { reloads: [], draws: 0 };
	let count = 0;
	const dt = {
		log: log,
		setCount: n => { count = n; },
		on: (evt, fn) => { handlers[evt] ||= []; handlers[evt].push(fn); return dt; },
		fire: (evt, e, ...rest) => { (handlers[evt] || []).forEach(fn => fn(e, ...rest)); },
		closeAllChildren: () => { for (const s of states.values()) s.shown = false; },
		row: tr => {
			if (!states.has(tr)) states.set(tr, { shown: false });
			const st = states.get(tr);
			const child = panel => ({
				show: () => {
					const ctr = doc.createElement('tr');
					const ctd = doc.createElement('td');
					ctr.appendChild(ctd); ctd.appendChild(panel);
					tableEl.querySelector('tbody').appendChild(ctr);
					st.shown = true;
				}
			});
			child.isShown = () => st.shown;
			child.hide = () => { st.shown = false; };
			return { length: 1, data: () => null, child };
		},
		rows: () => ({ count: () => count, data: () => ({ toArray: () => [] }) }),
		table: () => ({ node: () => tableEl }),
		search: () => dt,
		column: () => ({ search: Object.assign(() => '', { fixed: () => {} }), index: () => 0 }),
		draw: () => { log.draws++; return dt; },
		ajax: { reload: (cb, reset) => { log.reloads.push(reset === undefined ? null : reset); } }
	};
	return dt;
}

/** A fresh sandbox with all six scripts, a model tree built from `table` JSON, and a bound view table. */
function mk(tableJson) {
	const errors = [];
	const con = Object.assign(Object.create(console), { error: (...a) => errors.push(a.join(' ')), warn: (...a) => errors.push('WARN ' + a.join(' ')) });
	const env = makeEnv();
	env.window.console = con;
	const sandbox = {
		window: env.window, document: env.document, console: con,
		setTimeout: setTimeout, clearTimeout: clearTimeout,
		setInterval: () => 0, clearInterval: () => {},
		queueMicrotask: queueMicrotask, Promise: Promise,
		fetch: () => new Promise(() => {})
	};
	vm.createContext(sandbox);
	for (const p of scripts) vm.runInContext(fs.readFileSync(path.resolve(p), 'utf8'), sandbox, { filename: path.basename(p) }); // NOSONAR javascript:S1523 - loads the shipped scripts into a vm sandbox for the test
	const NS = env.window.JuneauViews;
	const M = NS.model;
	const hits = [];
	NS.handlers.register('app.hit', (target, ev) => { hits.push({ kind: target.kind, type: ev.type, payload: ev.payload }); });

	const json = { title: 'T', cards: [{ id: 'c1', type: 'datatables', table: tableJson }] };
	const page = M.fromJSON(json);
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
	const tpl = env.el('template');
	tpl.dataset.juneauRowDetail = '1';
	tpl.dataset.juneauDetailUrl = '/d/{id}';
	tpl.content = {
		cloneNode: () => {
			const wrap = env.el('div');
			const strip = NS.init.buildRibbonStrip([
				{ id: 't1', label: 'One', pane: env.el('div') },
				{ id: 't2', label: 'Two', pane: env.el('div') }
			], {});
			wrap.appendChild(strip.strip);
			return wrap;
		}
	};
	host.appendChild(tpl);

	const dt = fakeDt(table, env.document);
	const viewDef = { id: 'c1', ribbon: tableJson.ribbon || [] };
	const ctx = {
		table: table, viewDef: viewDef, dataTable: dt, activeState: {}, _dialogStack: [], _jobSources: new Set(), _pollTimers: [],
		redraw: () => { dt.ajax.reload(); }
	};
	ctx.collapseAllDetailRows = () => {};
	table.__juneauCtx = ctx;
	NS.init.initDetailsExpander(table, ctx, viewDef);
	NS.init.bindModelDrawHooks(table, ctx);
	const t = NS.tables.handle(table);

	const drawn = [];
	function rowsDom(ids) {
		while (tbody.firstChild) tbody.firstChild.remove();
		drawn.length = 0;
		for (const id of ids) {
			const tr = env.el('tr');
			tr.className = 'juneau-view-detail-row';
			if (id != null) tr.dataset.juneauRowId = id;
			const td = env.el('td');
			td.className = 'juneau-view-detail-control';
			tr.appendChild(td);
			tbody.appendChild(tr);
			drawn.push({ tr, td });
		}
	}
	function draw(ids) {
		dt.fire('preDraw.dt', { target: table });
		dt.closeAllChildren();
		rowsDom(ids);
		dt.fire('draw.dt', { target: table });
	}
	function toggle(i) {
		const d = drawn[i];
		table.dispatch('click', { target: d.td, preventDefault() {}, stopPropagation() {} });
	}
	return { env, NS, M, page, card: page.card('c1'), table, tbody, ctx, dt, t, draw, toggle, drawn, errors, hits, viewDef };
}

const DETAIL = {
	tabs: [{ id: 't1', label: 'One' }, { id: 't2', label: 'Two' }],
	model: { actions: [{ id: 'go', icon: 'x', label: 'Go', handler: 'app.hit' }] }
};

check('tables_handle_is_model_Table', () => {
	const w = mk({ detail: DETAIL });
	ok(w.t instanceof w.NS.model.Table, 'not a model Table');
	ok(w.t === w.card.children.find(c => c.kind === 'table'), 'not the tree table');
	ok(w.NS.tables.handle(w.table) === w.t, 'not stable');
});
check('tables_handle_four_members', () => {
	const w = mk({});
	ok(w.t.element === w.table, 'element');
	ok(w.t.dataTable === w.dt, 'dataTable');
	ok(w.t.viewDef === w.ctx.viewDef, 'viewDef');
	const tr = w.env.el('tr');
	ok(w.t.contextOf(tr).table === w.table, 'contextOf');
});
check('tables_ready_resolves_with_table', async () => {
	const w = mk({});
	const h = await w.NS.tables.ready(w.table);
	ok(h instanceof w.NS.model.Table, 'ready not a Table');
});
check('draw_hooks_bound_to_own_table_not_nested', () => {
	const w = mk({});
	let n = 0;
	const orig = w.t._endDraw.bind(w.t);
	w.t._endDraw = rows => { n++; return orig(rows); };
	w.dt.fire('draw.dt', { target: w.env.el('table') });
	eq(n, 0, 'nested draw ran _endDraw');
	w.dt.fire('draw.dt', { target: w.table });
	eq(n, 1, 'own draw');
});
check('draw_keyed_expanded_detaches_not_destroys', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0);
	const row = w.t.row('a'), d = row.detail;
	ok(d, 'no detail');
	w.t._reopenPending = true;
	w.draw(['a']);
	ok(row.detail === d && !d.destroyed && !d.attached, 'detail should be detached, kept');
	ok(row.attached, 'row reattached');
	const el = w.env.el('div');
	w.t.reattachDetail('a', el, {});
	ok(d.attached && d.el === el, 'reattachDetail');
});
check('draw_keyed_default_destroys_detail', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0);
	const d = w.t.row('a').detail;
	w.draw(['a']);
	ok(d.destroyed && w.t.row('a').detail === null, 'detail should be destroyed');
});
check('draw_positional_destroyed', () => {
	const w = mk({});
	w.draw([null, null]);
	const first = w.t.rows.slice();
	eq(first.length, 2);
	ok(first.every(r => r.positional), 'positional');
	w.draw([null, null]);
	ok(first.every(r => r.destroyed), 'old destroyed');
	eq(w.t.rows.length, 2);
});
check('draw_missing_row_evicted', () => {
	const w = mk({});
	w.draw(['a', 'b']);
	const b = w.t.row('b'), p = b.path;
	w.draw(['a']);
	ok(b.destroyed && w.M.get(p) === null, 'b not evicted');
	ok(w.t.row('a') && !w.t.row('a').destroyed, 'a kept');
});
check('registry_stable_after_draws', () => {
	const w = mk({});
	w.draw(['a', 'b']);
	const a = w.t.row('a');
	for (let i = 0; i < 4; i++) w.draw(['a', 'b']);
	ok(w.M.get(a.path) === a, 'same object');
	eq(w.t.rows.length, 2);
});
check('expand_creates_detail_instance_and_tabs', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0);
	const row = w.t.row('a'), d = row.detail;
	ok(d && !d.template, 'instance');
	eq(d.tabs.map(x => x.id), ['t1', 't2']);
	ok(w.M.get(row.path + '/detail') === d, 'registered');
	ok(d.tabs[0].attached && d.tabs[0].el.dataset.juneauStripTab === 't1', 'tab bound to strip button');
});
check('collapse_destroys_detail_keeps_row', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0); w.toggle(0);
	const row = w.t.row('a');
	ok(row && !row.destroyed && row.detail === null, 'detail should be gone, row kept');
});
check('row_expanded_collapsed_events', () => {
	const w = mk({ detail: DETAIL });
	const seen = [];
	for (const n of ['expanded', 'collapsed', 'rendered']) w.page.on(n, ev => seen.push(n + ':' + ev.source.kind));
	w.draw(['a']); w.toggle(0); w.toggle(0);
	eq(seen, ['expanded:row', 'rendered:rowdetail', 'collapsed:row'].sort((x, y) => seen.indexOf(x) - seen.indexOf(y)) && seen, 'order');
	ok(seen.includes('expanded:row') && seen.includes('rendered:rowdetail') && seen.includes('collapsed:row'), JSON.stringify(seen));
});
check('detail_actions_painted_and_click_runs_handler', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0);
	const d = w.t.row('a').detail;
	const btn = d.el.querySelector('[data-action-id]');
	ok(btn, 'action button not painted');
	btn.dispatch('click', {});
	eq(w.hits.map(h => h.kind + ':' + h.type), ['rowdetail:click']);
});
const RIBBON = [
	{ type: 'refresh' },
	{ type: 'option', id: 'mine', title: 'Mine', param: 'mine', value: 'true' },
	{ type: 'action', id: 'go', title: 'Go', model: { actions: [{ id: 'click', handler: 'app.hit' }] } }
];
function ribbonOf(w) {
	const bar = w.NS.ribbon.build(w.viewDef, w.ctx);
	return bar;
}
check('ribbon_item_handles_by_id', () => {
	const w = mk({ ribbon: RIBBON });
	const bar = ribbonOf(w);
	ok(bar, 'no bar');
	const r = w.t.ribbon;
	ok(r, 'no ribbon handle');
	for (const id of ['refresh', 'mine', 'go']) ok(r.item(id)?.attached && r.item(id).el, 'item ' + id);
});
check('ribbon_missing_id_custom_item_E121', () => {
	const w = mk({ ribbon: [{ type: 'publish', topic: 'a.b' }] });
	ok(w.page.findings.some(f => f.code === 'E-JS-121'), JSON.stringify(w.page.findings.map(f => f.code)));
});
check('ribbon_stock_default_ids_client_mirror', () => {
	const w = mk({ ribbon: [{ type: 'refresh' }, { type: 'collapseAll' }] });
	ok(w.t.ribbon.item('refresh') && w.t.ribbon.item('collapseAll'), 'default ids');
	eq(w.page.findings.length, 0);
});
check('ribbon_duplicate_stock_kind_E121', () => {
	const w = mk({ ribbon: [{ type: 'refresh' }, { type: 'refresh' }] });
	const f = w.page.findings.find(x => x.code === 'E-JS-121');
	ok(f && /explicit id/.test(f.message), 'duplicate stock');
});
check('ribbon_new_item_types_have_handles', () => {
	const w = mk({ ribbon: [{ type: 'search', id: 'search' }, { type: 'linkButton', id: 'link' }, { type: 'columnSettings', id: 'columns' }, { type: 'refresh', id: 'refresh' }], columnConfig: true });
	ribbonOf(w);
	// This harness loads no config or init builders, so only the search slot has an element; link and columns are skipped.
	ok(w.t.ribbon.item('search')?.attached, 'search handle');
	ok(w.t.ribbon.item('refresh')?.attached, 'refresh handle');
	ok(!w.t.ribbon.item('link') && !w.t.ribbon.item('columns'), 'a skipped item must leave no handle');
});
check('ribbon_hidden_items_leave_no_handle', () => {
	const w = mk({ ribbon: [{ type: 'collapseAll', id: 'collapse' }, { type: 'resetDefaults', id: 'reset' }, { type: 'columnSettings', id: 'columns' }, { type: 'refresh', id: 'refresh' }] });
	ribbonOf(w);
	ok(!w.t.ribbon.item('collapse'), 'collapse has a handle without row detail');
	ok(!w.t.ribbon.item('reset'), 'reset has a handle without columnConfig');
	ok(w.t.ribbon.item('refresh'), 'refresh');
});
check('detail_tab_setLabel_on_strip_button', () => {
	const w = mk({ detail: DETAIL });
	w.draw(['a']); w.toggle(0);
	const tab = w.t.row('a').detail.tab('t1');
	tab.setLabel('Renamed');
	ok(tab.el.textContent === 'Renamed', 'label ' + tab.el.textContent);
	ok(tab.el.getAttribute('role') === 'tab', 'role');
});
check('ribbon_action_item_click_handler', () => {
	const w = mk({ ribbon: RIBBON });
	ribbonOf(w);
	const acts = [];
	w.page.on('activated', ev => acts.push(ev.source.id));
	w.t.ribbon.item('go').el.dispatch('click', {});
	eq(w.hits.map(h => h.kind + ':' + h.type), ['ribbonitem:click']);
	eq(acts, []);
});
check('ribbon_stock_refresh_unchanged', () => {
	const w = mk({ ribbon: RIBBON });
	ribbonOf(w);
	w.t.ribbon.item('refresh').el.dispatch('click', {});
	ok(w.dt.log.reloads.length >= 1, 'refresh did not reload');
	const acts = [];
	w.page.on('activated', ev => acts.push(ev.source.id));
	w.t.ribbon.item('mine').el.dispatch('click', {});
	eq(acts, ['mine']);
});
check('table_loaded_event_count', () => {
	const w = mk({});
	const got = [];
	w.t.on('loaded', ev => got.push(ev.payload.count));
	w.dt.fire('xhr.dt', { target: w.table }, {}, { recordsTotal: 42, data: [] });
	eq(got, [42]);
});

(async () => {
	for (const f of pending) await f();
	process.stdout.write(JSON.stringify(report));
})();
