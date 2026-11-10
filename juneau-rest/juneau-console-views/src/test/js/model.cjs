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
 * model.cjs - dependency-free Node harness for juneau-model.js (the client object model: handles, path ids,
 * registry, lifecycle, kind classes, triggers, handler registry).  Prints ONE JSON report in which every key is
 * true (passed) or a failure string; ConsoleModel_Test asserts each key.
 *
 *   Usage:  node model.cjs <juneau-bus.js> [<juneau-console.js>] [<juneau-ribbon.js>] <juneau-model.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const files = {};
for (const p of process.argv.slice(2)) {
	const b = path.basename(p);
	const k = ['juneau-bus', 'juneau-console', 'juneau-ribbon', 'juneau-model-inline', 'juneau-model'].find(n => b.startsWith(n + '-') || b.startsWith(n + '.'));
	if (k) files[k] = fs.readFileSync(p, 'utf8');
}
if (!files['juneau-model']) {
	console.error('usage: node model.cjs <juneau-bus.js> [<juneau-console.js>] [<juneau-ribbon.js>] <juneau-model.js>');
	process.exit(2);
}

/** A fresh sandbox with the requested scripts loaded in dependency order. */
function load(opts) {
	opts = opts || {};
	const env = makeConsoleEnv(opts);
	const doc = env.document;
	const warns = [], errors = [];
	doc.baseURI = 'http://juneau.test/';
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)), debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent, URL, Promise, JSON,
		AbortController, setTimeout: fn => { fn(); return 0; }, clearTimeout: () => {},
		setInterval: () => 0, clearInterval: () => {}, localStorage: env.window.localStorage,
		fetch: () => Promise.reject(new Error('no fetch in harness'))
	};
	env.window.window = env.window;
	vm.createContext(sandbox);
	const order = ['juneau-bus'];
	if (opts.shell) order.push('juneau-console');
	if (opts.ribbon !== false && files['juneau-ribbon'] !== undefined) order.push('juneau-ribbon');   // the model reads the stock default ids from it
	order.push('juneau-model');
	if (files['juneau-model-inline'] !== undefined && opts.inline === true) order.push('juneau-model-inline');
	for (const n of order) {
		if (n === 'juneau-model' && typeof opts.beforeModel === 'function') opts.beforeModel(env.window);
		if (files[n] === undefined) throw new Error('script not supplied: ' + n);
		vm.runInContext(files[n], sandbox, { filename: n + '.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	}
	const NS = env.window.JuneauViews;
	return { env, doc, warns, errors, NS, M: NS.model, bus: NS.bus, JC: env.window.JuneauConsole, window: env.window };
}

const report = {};
const pending = [];
function ok(c, msg) { if (!c) throw new Error(msg || 'assertion failed'); }
function eq(a, b, msg) {
	const x = JSON.stringify(a), y = JSON.stringify(b);
	if (x !== y) throw new Error((msg || 'eq') + ': ' + x + ' !== ' + y);
}
function throws(fn, name) {
	try { fn(); } catch (e) {
		if (name && e.name !== name && e.code !== name) throw new Error('expected ' + name + ' but got ' + (e.code || e.name) + ': ' + e.message);
		return e;
	}
	throw new Error('expected a throw' + (name ? ' of ' + name : ''));
}
/** Registers one report key; fn may be async.  Run in registration order. */
function check(key, fn) {
	pending.push(async () => {
		try { await fn(); report[key] = true; } catch (e) { report[key] = 'FAIL: ' + e?.message; }
	});
}

/** A small tree builder over the base Handle. */
function world(opts) {
	const s = load(opts);
	const all = [];
	const mk = (kind, id, o) => {
		const h = new s.M.Handle(kind, { title: kind + ':' + id }, { id, ...o });
		all.push(h);
		return h;
	};
	const live = () => all.filter(h => h.path != null && s.M.get(h.path) === h).length;
	const el = () => s.doc.createElement('div');
	return Object.assign(s, { mk, all, live, el });
}
function fullTree() {
	const w = world();
	const page = w.mk('page', 'instances'), card = w.mk('card', 'list');
	const table = w.mk('table', 'table', { singleton: true });
	const row = w.mk('row', '651'), detail = w.mk('rowdetail', 'detail', { singleton: true }), tab = w.mk('detailtab', 'orders');
	page._add(card); card._add(table); table._add(row); row._add(detail); detail._add(tab);
	return Object.assign(w, { page, card, table, row, detail, tab });
}
function attachAll(t) {
	for (const h of [t.page, t.card, t.table, t.row, t.detail, t.tab]) h._attach(t.el());
}

//-----------------------------------------------------------------------------------------------------------------
// Core: path ids, registry, lifecycle, navigation, base surface
//-----------------------------------------------------------------------------------------------------------------

check('path_segment_encoding', () => {
	const w = world();
	const p = w.mk('page', 'p'), c = w.mk('card', "a:b'()!*é-._~");
	p._add(c);
	eq(c.path, 'page/p/card:a%3Ab%27%28%29%21%2A%E9-._%7E');
	const d = w.mk('card', 'Ab9._-');
	p._add(d);
	eq(d.path, 'page/p/card:Ab9._-');
});
check('path_page_card_table_row_detail_tab', () => {
	const t = fullTree();
	eq(t.tab.path, 'page/instances/card:list/table/row:651/detail/tab:orders');
	eq(t.page.path, 'page/instances');
});
check('path_positional_row', () => {
	const t = fullTree();
	const r = t.mk('row', '3', { positional: true });
	t.table._add(r);
	eq(r.path, 'page/instances/card:list/table/row:#3');
	ok(r._positional === true);
});

check('get_returns_same_object', () => {
	const t = fullTree();
	attachAll(t);
	ok(t.M.get(t.row.path) === t.row);
	const again = t.M.get(t.row.path);
	ok(t.M.get(t.row.path) === again);
});
check('get_unknown_null', () => { eq(world().M.get('page/nope'), null); });
check('get_never_creates', () => {
	const t = fullTree();
	const before = t.live();
	eq(t.M.get('page/instances/card:list/table/row:999'), null);
	eq(t.M.get('page/instances/card:list/table/row:999'), null);
	eq(t.live(), before);
	eq(t.table.children.length, 1);
});

check('detach_keyed_row_reopens_same_object', () => {
	const t = fullTree();
	attachAll(t);
	const path = t.row.path;
	t.row._detach();
	ok(t.M.get(path) === t.row, 'detached handle stays reachable');
	eq(t.row.attached, false);
	eq(t.row.el, null);
	eq(t.detail.attached, false, 'subtree is detached with it');
	const el2 = t.el();
	t.row._attach(el2, { id: 651 });
	ok(t.M.get(path) === t.row);
	ok(t.row.el === el2);
	eq(t.row.data, { id: 651 });
	eq(t.row.attached, true);
});
check('detach_methods_noop_warn_once', () => {
	const t = fullTree();
	attachAll(t);
	t.row._detach();
	t.warns.length = 0;
	t.row.setLabel('x');
	t.row.show();
	t.row.setBadge(3, 'info');
	eq(t.warns.length, 1);
	ok(t.warns[0].includes('E-JS-117'));
	ok(t.warns[0].includes(t.row.path));
});
check('reattach_clears_warned_flag', () => {
	const t = fullTree();
	attachAll(t);
	t.row._detach();
	t.warns.length = 0;
	t.row.hide();
	t.row._attach(t.el());
	t.row._detach();
	t.row.hide();
	eq(t.warns.length, 2);
});
check('positional_rows_destroyed_on_draw', () => {
	const t = fullTree();
	const r = t.mk('row', '0', { positional: true }), d = t.mk('rowdetail', 'detail', { singleton: true });
	t.table._add(r); r._add(d);
	r._attach(t.el()); d._attach(t.el());
	const path = r.path;
	for (const c of t.table.children) if (c._positional) c._destroy();
	eq(r.destroyed, true);
	eq(d.destroyed, true);
	eq(t.M.get(path), null);
	ok(!t.table.children.includes(r));
});
check('collapse_destroys_detail_and_tabs_not_row', () => {
	const t = fullTree();
	attachAll(t);
	const dp = t.detail.path, tp = t.tab.path;
	t.detail._destroy();
	eq(t.detail.destroyed, true);
	eq(t.tab.destroyed, true);
	eq(t.M.get(dp), null);
	eq(t.M.get(tp), null);
	eq(t.row.destroyed, false);
	ok(t.M.get(t.row.path) === t.row);
	eq(t.row.children.length, 0);
});
check('missing_keyed_row_evicted_after_draw', () => {
	const t = fullTree();
	attachAll(t);
	const p = t.row.path;
	t.row._detach();
	t.row._destroy();
	eq(t.M.get(p), null);
	eq(t.row.destroyed, true);
	eq(t.table.children.length, 0);
});
check('destroy_disposes_bus_owner', () => {
	const t = fullTree();
	attachAll(t);
	const ow = t.tab._busOwner();
	ok(ow && typeof ow.publish === 'function');
	t.warns.length = 0;
	t.tab._destroy();
	ow.publish('selection:x', { schemaVersion: 1, ids: [], count: 0 });
	ok(t.warns.some(m => m.includes('disposed')), 'owner is disposed: ' + JSON.stringify(t.warns));
});
check('registry_size_stable_after_polls', () => {
	const t = fullTree();
	attachAll(t);
	const keyed = [];
	for (let i = 0; i < 5; i++) {
		const r = t.mk('row', 'k' + i); t.table._add(r); r._attach(t.el());
		const d = t.mk('rowdetail', 'detail', { singleton: true }); r._add(d); d._attach(t.el());
		keyed.push([r, d]);
	}
	const baseline = t.live();
	for (let draw = 0; draw < 50; draw++) {
		// beginDraw: keyed rows detach (their detail stays reachable), positional rows die
		const pos = t.mk('row', String(draw), { positional: true }); t.table._add(pos); pos._attach(t.el());
		for (const c of t.table.children) c._positional ? c._destroy() : c._detach();
		// endDraw: keyed rows come back with new elements
		for (const [r, d] of keyed) { r._attach(t.el()); d._attach(t.el()); }
	}
	eq(t.live(), baseline);
	ok(t.all.filter(h => h.destroyed).every(h => h.path == null || t.M.get(h.path) !== h), 'destroyed handles are not reachable');
});
check('page_unmount_destroys_subtree', () => {
	const t = fullTree();
	attachAll(t);
	const paths = [t.page, t.card, t.table, t.row, t.detail, t.tab].map(h => h.path);
	t.page._destroy();
	for (const h of [t.page, t.card, t.table, t.row, t.detail, t.tab]) eq(h.destroyed, true, h.kind);
	for (const p of paths) eq(t.M.get(p), null, p);
	eq(t.M.page(), null);
});

check('parent_children', () => {
	const t = fullTree();
	ok(t.row.parent === t.table);
	ok(t.table.children[0] === t.row);
	eq(t.page.parent, null);
});
check('children_is_copy', () => {
	const t = fullTree();
	const kids = t.table.children;
	kids.length = 0;
	eq(t.table.children.length, 1);
});
check('closest_self_and_ancestor', () => {
	const t = fullTree();
	ok(t.row.closest('row') === t.row);
	ok(t.tab.closest('card') === t.card);
	ok(t.tab.closest('page') === t.page);
	eq(t.row.closest('chart'), null);
});
check('find_relative_path', () => {
	const t = fullTree();
	ok(t.card.find('table/row:651/detail') === t.detail);
	ok(t.row.find('detail/tab:orders') === t.tab);
	eq(t.card.find('table/row:999/detail'), null);
});
check('find_kind_id_depth_first', () => {
	const t = fullTree();
	ok(t.page.find('tab:orders') === t.tab);
	ok(t.page.find('row:651') === t.row);
	ok(t.page.find('table') === t.table);
	eq(t.page.find('tab:nope'), null);
});
check('page_accessor', () => {
	const t = fullTree();
	eq(t.M.page(), null);
	t.page._attach(t.el());
	ok(t.M.page() === t.page);
	ok(t.tab.closest('page') === t.page);
});

check('spec_deep_frozen', () => {
	const w = world();
	const h = new w.M.Handle('card', { a: { b: [1, { c: 2 }] } }, { id: 'x' });
	ok(Object.isFrozen(h.spec) && Object.isFrozen(h.spec.a) && Object.isFrozen(h.spec.a.b) && Object.isFrozen(h.spec.a.b[1]));
});
check('missing_member_is_typeerror', () => {
	const w = world();
	const h = w.mk('card', 'x');
	throws(() => h.rowCount(), 'TypeError');
	ok(!('rowCount' in h));
});
check('helpers_is_frozen_empty_object', () => {
	const w = world();
	const h = w.mk('card', 'x');
	ok(Object.isFrozen(h.helpers));
	eq(Object.keys(h.helpers).length, 0);
});

//-----------------------------------------------------------------------------------------------------------------
// Handler registry and built-ins
//-----------------------------------------------------------------------------------------------------------------

const H = w => w.NS.handlers;
check('register_ok_get_names_sorted', () => {
	const w = world();
	H(w).register('zeta.go', () => 1);
	H(w).register('alpha.run', () => 2);
	ok(typeof H(w).get('alpha.run') === 'function');
	const names = H(w).names();
	ok(names.indexOf('alpha.run') < names.indexOf('zeta.go'));
	eq(names, names.slice().sort());
});
check('register_badName_E114_fatal', () => {
	const w = world();
	for (const bad of ['foo', 'Foo.bar', 'foo.Bar', 'foo..bar', 'foo.bar baz', '', null, 5])
		throws(() => H(w).register(bad, () => {}), 'E-JS-114');
	const e = throws(() => H(w).register('foo', () => {}));
	ok(e.code === 'E-JS-114' && typeof e.message === 'string');
});
check('register_badName_with_shell_is_JuneauConsoleError', () => {
	const w = load({ shell: true });
	const e = throws(() => w.NS.handlers.register('Bad', () => {}), 'E-JS-114');
	ok(e instanceof w.JC.JuneauConsoleError, 'shell error type');
});
check('register_duplicate_E115', () => {
	const w = world();
	H(w).register('dup.one', () => {});
	throws(() => H(w).register('dup.one', () => {}), 'E-JS-115');
});
check('register_nonFunction_E122', () => {
	const w = world();
	throws(() => H(w).register('app.nofn', 'x'), 'E-JS-122');
	throws(() => H(w).register('app.nofn', null), 'E-JS-122');
});
check('register_juneauNs_E123', () => {
	const w = world();
	throws(() => H(w).register('juneau.mine', () => {}), 'E-JS-123');
});
check('get_unknown_undefined', () => { ok(H(world()).get('no.such') === undefined); });
check('handlers_namespace_frozen', () => { ok(Object.isFrozen(H(world()))); });
check('builtins_listed', () => {
	const names = H(world()).names();
	for (const n of ['juneau.refresh', 'juneau.refreshClosest', 'juneau.expand', 'juneau.collapse', 'juneau.activate', 'juneau.publish', 'juneau.countLabel', 'juneau.countBadge'])
		ok(names.includes(n), n);
});
check('builtin_refresh_calls_target_refresh', () => {
	const w = world();
	let n = 0;
	H(w).get('juneau.refresh')({ refresh() { n++; } }, { payload: {} });
	eq(n, 1);
});
check('builtin_refreshClosest_kind_payload', () => {
	const w = world();
	const calls = [];
	const target = { closest(k) { calls.push(k); return k === 'nope' ? null : { refresh() { calls.push('r'); } }; } };
	H(w).get('juneau.refreshClosest')(target, { payload: {} });
	H(w).get('juneau.refreshClosest')(target, { payload: { kind: 'table' } });
	H(w).get('juneau.refreshClosest')(target, { payload: { kind: 'nope' } });
	H(w).get('juneau.refreshClosest')(target, undefined);
	eq(calls, ['card', 'r', 'table', 'r', 'nope', 'card', 'r']);
});
check('builtin_expand_collapse_row', () => {
	const w = world();
	const calls = [];
	const row = { expand() { calls.push('e'); }, collapse() { calls.push('c'); } };
	H(w).get('juneau.expand')(row, { payload: {} });
	H(w).get('juneau.collapse')(row, { payload: {} });
	eq(calls, ['e', 'c']);
});
check('builtin_activate', () => {
	const w = world();
	let n = 0;
	H(w).get('juneau.activate')({ activate() { n++; } }, { payload: {} });
	eq(n, 1);
});
check('builtin_publish_topic_payload', () => {
	const w = world();
	const got = [];
	H(w).get('juneau.publish')({ publish: (t, p) => got.push([t, p]) }, { payload: { topic: 'filter:x', payload: { a: 1 } } });
	eq(got, [['filter:x', { a: 1 }]]);
});
const labelTarget = (rowCount, spec) => {
	const calls = [];
	return { calls, spec: spec || { title: 'Orders' }, table: rowCount === undefined ? null : { rowCount: () => rowCount },
		setLabel: t => calls.push(['label', t]), setBadge: (n, tone) => calls.push(['badge', n, tone]) };
};
check('countLabel_uses_payload_count_over_rowCount', () => {
	const w = world();
	const t = labelTarget(9);
	H(w).get('juneau.countLabel')(t, { payload: { count: 4 } });
	eq(t.calls, [['label', 'Orders (4)']]);
});
check('countLabel_undefined_noop', () => {
	const w = world();
	const t = labelTarget(undefined);
	H(w).get('juneau.countLabel')(t, { payload: {} });
	eq(t.calls, []);
});
check('countLabel_writes_serverLabel_plus_count', () => {
	const w = world();
	const t = labelTarget(3, { label: 'Items' });
	H(w).get('juneau.countLabel')(t, { payload: {} });
	eq(t.calls, [['label', 'Items (3)']]);
	const z = labelTarget(0);
	H(w).get('juneau.countLabel')(z, { payload: { count: 0 } });
	eq(z.calls, [['label', 'Orders (0)']]);
});
check('countBadge_info_tone', () => {
	const w = world();
	const t = labelTarget(5);
	H(w).get('juneau.countBadge')(t, { payload: {} });
	eq(t.calls, [['badge', 5, 'info']]);
	const n = labelTarget(undefined);
	H(w).get('juneau.countBadge')(n, { payload: {} });
	eq(n.calls, []);
});

//-----------------------------------------------------------------------------------------------------------------
// Kind classes
//-----------------------------------------------------------------------------------------------------------------

const KINDS = [['Page', 'page'], ['PageTab', 'pagetab'], ['Card', 'card'], ['Table', 'table'], ['Row', 'row'],
	['RowDetail', 'rowdetail'], ['DetailTab', 'detailtab'], ['Ribbon', 'ribbon'], ['RibbonItem', 'ribbonitem'],
	['Switcher', 'switcher'], ['SwitcherView', 'view'], ['Chart', 'chart']];
const specFor = { RibbonItem: { id: 'x', type: 'action' }, Row: { key: 'k' }, Page: { title: 'T', nav: [], activeNav: [] } };
for (const [cls, kind] of KINDS) {
	check('kind_' + cls + '_exists_and_instanceof_Handle', () => {
		const w = load({ shell: true });
		const K = w.M[cls];
		ok(typeof K === 'function', cls + ' missing');
		const h = new K({ id: 'a1', ...specFor[cls] });
		// Card extends the shell base, so it is not a Handle subclass: it carries every Handle member instead.
		ok(cls === 'Card' ? typeof h.closest === 'function' : h instanceof w.M.Handle);
		eq(h.kind, kind);
	});
}
const sh = () => load({ shell: true });
check('Table_has_four_legacy_members', () => {
	const w = sh();
	const t = new w.M.Table({});
	const impl = { element: { n: 1 }, dataTable: { n: 2 }, viewDef: { n: 3 }, contextOf: tr => ({ tr }) };
	t._impl = impl;
	ok(t.element === impl.element && t.dataTable === impl.dataTable && t.viewDef === impl.viewDef);
	eq(t.contextOf('r'), { tr: 'r' });
	ok(t instanceof w.M.Table);
});
check('Table_methods_delegate_to_impl', () => {
	const w = sh();
	const t = new w.M.Table({});
	const calls = [];
	t._impl = { reload: o => calls.push(['reload', o]), selection: () => ['a'], select: ids => calls.push(['select', ids]),
		clearSelection: () => calls.push(['clear']), setFilter: p => calls.push(['filter', p]), pausePolling: () => calls.push(['pause']),
		resumePolling: () => calls.push(['resume']), collapseAll: () => calls.push(['collapseAll']) };
	t._attach(w.doc.createElement('div'));
	t.reload({ resetPaging: true }); t.select(['1']); t.clearSelection(); t.setFilter({ q: 1 }); t.pausePolling(); t.resumePolling(); t.collapseAll();
	eq(t.selection(), ['a']);
	eq(calls.map(c => c[0]), ['reload', 'select', 'clear', 'filter', 'pause', 'resume', 'collapseAll']);
	eq(calls[0][1], { resetPaging: true });
});
check('Table_rows_rowCount_row_lookup', () => {
	const w = sh();
	const t = new w.M.Table({});
	const r1 = new w.M.Row({}, { key: '651' }), r2 = new w.M.Row({}, { key: '652' });
	t._add(r1); t._add(r2);
	eq(t.rows.length, 2);
	eq(t.rowCount(), 2);
	ok(t.row('652') === r2);
	eq(t.row('nope'), null);
});
check('Table_refresh_is_reload', () => {
	const w = sh();
	const t = new w.M.Table({});
	let got;
	t._impl = { reload: o => { got = o; } };
	t._attach(w.doc.createElement('div'));
	t.refresh();
	eq(got, {});
});
check('Row_positional_flag', () => {
	const w = sh();
	const r = new w.M.Row({}, { key: '3', positional: true });
	eq(r.positional, true);
	eq(r.key, '3');
	const k = new w.M.Row({}, { key: '651' });
	eq(k.positional, false);
});
check('Row_expand_collapse_events', () => {
	const w = sh();
	const row = new w.M.Row({}, { key: '651' });
	const calls = [];
	row._hooks = {
		expand: r => { calls.push('expand'); r._add(new w.M.RowDetail({})); },
		collapse: r => { calls.push('collapse'); r.detail._destroy(); }
	};
	row._attach(w.doc.createElement('div'));
	eq(row.expanded, false);
	row.expand();
	eq(row.expanded, true);
	ok(row.detail instanceof w.M.RowDetail);
	row.expand();
	row.collapse();
	eq(row.expanded, false);
	eq(row.detail, null);
	eq(row.destroyed, false);
	eq(calls, ['expand', 'collapse']);
});
check('RowDetail_tab_lookup', () => {
	const w = sh();
	const d = new w.M.RowDetail({});
	const a = d.addTab({ id: 'orders' }), b = d.addTab({ id: 'notes' });
	ok(a instanceof w.M.DetailTab);
	eq(d.tabs.length, 2);
	ok(d.tab('notes') === b);
	eq(d.tab('zzz'), null);
	ok(d.activeTab === a);
	d.removeTab('orders');
	eq(a.destroyed, true);
	eq(d.tabs.length, 1);
	eq(d.template, false);
});
check('Ribbon_item_lookup_by_id', () => {
	const w = sh();
	const r = new w.M.Ribbon({});
	const a = r.addItem({ id: 'go', type: 'action' }), b = r.addItem({ type: 'refresh' });
	eq(r.items.length, 2);
	ok(r.item('go') === a);
	eq(b.id, 'refresh');
	ok(r.item('refresh') === b);
	r.removeItem('go');
	eq(a.destroyed, true);
	eq(r.item('go'), null);
});
check('RibbonItem_requires_id_E121', () => {
	const w = sh();
	const e = throws(() => new w.M.RibbonItem({ type: 'action' }), 'E-JS-121');
	ok(e instanceof w.JC.JuneauConsoleError);
	throws(() => new w.M.RibbonItem({ type: 'publish' }), 'E-JS-121');
	eq(new w.M.RibbonItem({ type: 'refresh' }).id, 'refresh');
	eq(new w.M.RibbonItem({ type: 'action', id: 'z' }).id, 'z');
	throws(() => new w.M.RibbonItem({ type: 'divider' }), 'TypeError');
});
check('Ribbon_second_stock_item_needs_explicit_id_E121', () => {
	const w = sh();
	const r = new w.M.Ribbon({});
	r.addItem({ type: 'refresh' });
	throws(() => r.addItem({ type: 'refresh' }), 'E-JS-121');
	eq(r.addItem({ type: 'refresh', id: 'refresh2' }).id, 'refresh2');
	eq(r.items.length, 2);
});
check('Switcher_Chart_skeletons', () => {
	const w = sh();
	const s = new w.M.Switcher({}), v = new w.M.SwitcherView({ id: 'v1' }), c = new w.M.Chart({});
	eq(s.path, null);
	s._add(v);
	ok(s.view('v1') === v);
	eq(s.views.length, 1);
	const calls = [];
	c._impl = { export: (t, o) => { calls.push(['export', t, o]); return 'blob'; }, getOption: () => ({ a: 1 }), chart: () => 'inst' };
	eq(c.export('png', { w: 1 }), 'blob');
	eq(c.getOption(), { a: 1 });
	eq(c.chart(), 'inst');
	eq(calls[0], ['export', 'png', { w: 1 }]);
});
check('absent_member_typeerror', () => {
	const w = sh();
	throws(() => new w.M.Row({}, { key: 'a' }).rowCount(), 'TypeError');
	throws(() => new w.M.Ribbon({}).expand(), 'TypeError');
	throws(() => new w.M.Chart({}).select([]), 'TypeError');
	throws(() => new w.M.DetailTab({ id: 'a' }).items(), 'TypeError');
});
check('Card_collisions_resolved', () => {
	const w = sh();
	const c = new w.M.Card({ id: 'k1' });
	ok(w.M.Card.prototype.refresh === w.JC.Card.prototype.refresh);
	ok(w.M.Card.prototype.publish === w.JC.Card.prototype.publish);
	ok(w.M.Card.prototype.subscribe === w.JC.Card.prototype.subscribe);
	ok(w.M.Card.prototype.refresh !== w.M.Handle.prototype.refresh);
	ok(c instanceof w.JC.Card);
	for (const m of ['closest', 'find', 'setLabel', 'setBadge', 'show', 'hide', 'activate'])
		ok(typeof c[m] === 'function', m);
	for (const a of ['kind', 'path', 'parent', 'children', 'attached', 'destroyed', 'helpers', 'data'])
		ok(Object.getOwnPropertyDescriptor(w.M.Card.prototype, a), a);
	eq(c.kind, 'card');
	eq(c.id, 'k1');
});
check('Card_shell_assigns_el_and_spec', () => {
	const w = sh();
	const c = new w.M.Card();
	const el = w.doc.createElement('div');
	c.el = el;
	c.spec = Object.freeze({ id: 'k2', type: 'x' });
	ok(c.el === el);
	eq(c.spec.id, 'k2');
	eq(c.id, 'k2');
});
check('Card_registers_and_renders_via_shell', () => {
	const w = sh();
	let seen = null;
	w.JC.registerCard('probe', class extends w.M.Card { render() { seen = this; this.el.appendChild(w.doc.createTextNode('ok')); } }); // NOSONAR javascript:S7740 - the test needs the instance the shell constructs
	w.JC.mount({ contractVersion: '1', title: 't', nav: [], activeNav: [], cards: [{ id: 'p1', type: 'probe' }] }, { root: w.doc.body, document: w.doc });
	ok(seen instanceof w.M.Card);
	eq(seen.id, 'p1');
	eq(seen.kind, 'card');
	eq(w.errors.length, 0);
});
check('HtmlCard_instanceof_model_Card', () => {
	const w = sh();
	ok(typeof w.JC.rebindBuiltins === 'function');
	const map = w.JC.rebindBuiltins(w.M.Card);
	ok(new map.html() instanceof w.M.Card);
	ok(new map['terminal-view']() instanceof w.M.Card);
	ok(typeof map.html.prototype.render === 'function');
	ok(w.JC.cardTypes().includes('html'));
});
// A built-in card script registers its lateBase class whenever it loads; the model may load before or after it.
const LATE_TYPES = ['datatables', 'chart', 'job-log', 'terminal'];
const queueLate = win => { for (const t of LATE_TYPES) win.JuneauConsoleCards.push([t, class extends (win.JuneauViews?.model?.Card || win.JuneauConsole?.Card || class {}) { static lateBase = true; render() { /* no-op */ } }]); }; // NOSONAR javascript:S2094 - an inert fallback base
const lateCheck = async (w) => {
	mountPage(w, PAGE({ cards: LATE_TYPES.map((t, i) => ({ id: 'l' + i, type: t })) }));
	await tick();
	return LATE_TYPES.map((t, i) => w.JC.cardApi('l' + i) instanceof w.M.Card && w.M.get('page/pg/card:l' + i) === w.JC.cardApi('l' + i));
};
check('lateBase_builtins_registered_before_model_are_model_cards', async () => {
	const w = load({ shell: true, ribbon: true, beforeModel: win => queueLate(win) });
	eq(await lateCheck(w), [true, true, true, true]);
});
check('lateBase_builtins_registered_after_model_are_model_cards', async () => {
	const w = load({ shell: true, ribbon: true });
	queueLate(w.window);
	eq(await lateCheck(w), [true, true, true, true]);
});
check('DetailTab_setLabel_updates_strip_button', () => {
	const w = sh();
	const btn = w.doc.createElement('button');
	btn.appendChild(w.doc.createTextNode('Orders'));
	const chip = w.doc.createElement('span');
	chip.className = 'juneau-view-detail-tab-count';
	chip.textContent = '3';
	btn.appendChild(chip);
	const t = new w.M.DetailTab({ id: 'orders' });
	t._attach(btn);
	t.setLabel('Orders (3)');
	eq(btn.childNodes[0].nodeValue, 'Orders (3)');
	ok(btn.childNodes[1] === chip);
	eq(chip.textContent, '3');
	const bare = w.doc.createElement('button');
	const t2 = new w.M.DetailTab({ id: 'b' });
	t2._attach(bare);
	t2.setLabel('Fresh');
	eq(bare.textContent, 'Fresh');
});
check('DetailTab_setBadge_chip_and_tones', () => {
	const w = sh();
	const btn = w.doc.createElement('button');
	btn.appendChild(w.doc.createTextNode('Orders'));
	const t = new w.M.DetailTab({ id: 'orders' });
	t._attach(btn);
	t.setBadge(5, 'warning');
	const chip = btn.querySelector('.juneau-view-detail-tab-count');
	ok(chip, 'chip created');
	eq(chip.textContent, '5');
	eq(chip.dataset.tone, 'warning');
	t.setBadge(6);
	eq(btn.querySelectorAll('.juneau-view-detail-tab-count').length, 1);
	eq(chip.textContent, '6');
	t.setBadge(null);
	eq(btn.querySelectorAll('.juneau-view-detail-tab-count').length, 0);
	for (const tone of ['info', 'success', 'warning', 'danger', 'neutral']) t.setBadge(1, tone);
	throws(() => t.setBadge(1, 'purple'), 'TypeError');
});
check('transient_setLabel_restored_on_rerender', () => {
	const w = sh();
	const mkBtn = () => { const b = w.doc.createElement('button'); b.appendChild(w.doc.createTextNode('Orders')); return b; };
	const t = new w.M.DetailTab({ id: 'orders' });
	const b1 = mkBtn();
	t._attach(b1);
	t.setLabel('Orders (9)');
	eq(b1.textContent, 'Orders (9)');
	t._detach();
	const b2 = mkBtn();
	t._attach(b2);
	eq(b2.textContent, 'Orders');
});
check('PageTab_label_badge_activate', () => {
	const w = sh();
	const a = w.doc.createElement('a');
	a.appendChild(w.doc.createTextNode('Home'));
	const t = new w.M.PageTab({ id: 'home' });
	let nav = null;
	t._hooks = { navigate: h => { nav = h.id; } };
	t._attach(a);
	t.setLabel('Start');
	eq(a.childNodes[0].nodeValue, 'Start');
	t.setBadge(2, 'info');
	ok(a.querySelector('.juneau-view-detail-tab-count'));
	t.activate();
	eq(nav, 'home');
});
check('RibbonItem_label_enable_disable_activate', () => {
	const w = sh();
	const b = w.doc.createElement('button');
	const it = new w.M.RibbonItem({ id: 'go', type: 'action' });
	let act = 0;
	it._hooks = { activate: () => { act++; } };
	it._attach(b);
	it.setLabel('Run');
	eq(b.getAttribute('aria-label'), 'Run');
	it.disable();
	eq(b.disabled, true);
	it.enable();
	eq(b.disabled, false);
	it.activate();
	eq(act, 1);
});
check('Card_title_label_and_badge', () => {
	const w = sh();
	const host = w.doc.createElement('section');
	const title = w.doc.createElement('h2');
	title.className = 'jc-card-title';
	title.appendChild(w.doc.createTextNode('Orders'));
	host.appendChild(title);
	const c = new w.M.Card({ id: 'k' });
	c._attach(host);
	c.setLabel('Orders!');
	eq(title.childNodes[0].nodeValue, 'Orders!');
	c.setBadge(4, 'danger');
	eq(host.querySelector('.juneau-view-detail-tab-count').textContent, '4');
});

//-----------------------------------------------------------------------------------------------------------------
// Triggers: on / subscribe / emit, declarative action / bind / listen, event object, error containment
//-----------------------------------------------------------------------------------------------------------------

const tick = () => new Promise(r => setImmediate(r));
/** A full attached tree built from the kind classes, with a ribbon-capable sandbox. */
function T7(opts) {
	const w = world({ ribbon: true, ...opts });
	const M = w.M;
	const page = new M.Page({ id: 'instances', title: 'T', nav: [], activeNav: [] }), card = new M.Card({ id: 'list' }), table = new M.Table({});
	const row = new M.Row({}, { key: '651' }), detail = new M.RowDetail({}), tab = new M.DetailTab({ id: 'orders' });
	page._add(card); card._add(table); table._add(row); row._add(detail); detail._add(tab);
	const t = Object.assign(w, { page, card, table, row, detail, tab });
	t.attachAll = () => { for (const h of [page, card, table, row, detail, tab]) h._attach(w.doc.createElement('div')); };
	t.reg = (name, fn) => w.NS.handlers.register(name, fn);
	return t;
}
const topicCount = (w, topic) => { const e = w.bus.topics().find(x => x.topic === topic); return e ? e.subscribers : 0; };

check('on_returns_off', () => {
	const t = T7(); t.attachAll();
	const got = [];
	const off = t.tab.on('x-evt', ev => got.push(ev.type));
	t.tab.emit('x-evt');
	off();
	t.tab.emit('x-evt');
	eq(got, ['x-evt']);
	eq(typeof off, 'function');
});
check('on_string_typeerror', () => { const t = T7(); throws(() => t.tab.on('loaded', 'a.b'), 'TypeError'); });
check('bind_function_typeerror', () => { const t = T7(); throws(() => t.tab.bind('loaded', () => {}), 'TypeError'); });
check('subscribe_returns_off', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	const got = [];
	const off = t.tab.subscribe('app.t', p => got.push(p.n));
	t.bus.publish('app.t', { n: 1 });
	off();
	t.bus.publish('app.t', { n: 2 });
	eq(got, [1]);
	eq(topicCount(t, 'app.t'), 0);
});
check('subscribe_string_typeerror', () => { const t = T7(); throws(() => t.tab.subscribe('app.t', 'a.b'), 'TypeError'); });
check('bind_returns_this', () => { const t = T7(); t.reg('ord.h', () => {}); ok(t.tab.bind('loaded', 'ord.h') === t.tab); });
check('listen_returns_this', () => { const t = T7(); t.reg('ord.h', () => {}); ok(t.tab.listen('app.t', 'ord.h') === t.tab); });
check('action_returns_this', () => { const t = T7(); t.reg('ord.h', () => {}); ok(t.tab.action('go', 'refresh', 'ord.h') === t.tab); });
check('action_duplicate_id_E121', () => {
	const t = T7(); t.reg('ord.h', () => {});
	t.tab.action('go', 'refresh', 'ord.h');
	throws(() => t.tab.action('go', 'refresh', 'ord.h'), 'E-JS-121');
});
check('action_click_payload', () => {
	const t = T7(); t.attachAll();
	const got = [];
	t.reg('ord.click', (target, ev) => got.push([target.id, ev.type, ev.payload, ev.source.id]));
	t.tab.action('go', 'refresh', 'ord.click', { x: 1 });
	t.tab.action({ id: 'other', icon: 'tune', label: 'Other', handler: 'ord.click' });
	const bar = t.tab.el.querySelector('.juneau-model-actions');
	ok(bar, 'actions container painted');
	eq(bar.childNodes.length, 2);
	const b = bar.childNodes[0];
	eq(b.dataset.actionId, 'go');
	eq(b.getAttribute('type'), 'button');
	b.dispatch('click', {});
	bar.childNodes[1].dispatch('click', {});
	eq(got, [['orders', 'click', { actionId: 'go', x: 1 }, 'orders'], ['orders', 'click', { actionId: 'other' }, 'orders']]);
	eq(bar.childNodes[1].getAttribute('aria-label'), 'Other');
});
check('event_frozen_except_stopPropagation', () => {
	const t = T7(); t.attachAll();
	let ev0;
	t.tab.on('e', ev => { ev0 = ev; });
	t.tab.emit('e', { a: 1 });
	ok(Object.isFrozen(ev0));
	eq(typeof ev0.stopPropagation, 'function');
	throws(() => { ev0.type = 'z'; }, 'TypeError');
	eq(Object.keys(ev0).sort((a, b) => (a < b ? -1 : Number(a > b))), ['meta', 'payload', 'source', 'stopPropagation', 'target', 'type']);
});
check('event_target_is_declaring_handle', () => {
	const t = T7(); t.attachAll();
	const seen = [];
	for (const h of [t.tab, t.row, t.card, t.page]) h.on('e', ev => seen.push([ev.target.kind, ev.source.kind]));
	t.tab.emit('e');
	eq(seen, [['detailtab', 'detailtab'], ['row', 'detailtab'], ['card', 'detailtab'], ['page', 'detailtab']]);
});
check('event_source_is_emitter', () => {
	const t = T7(); t.attachAll();
	let src;
	t.page.on('e', ev => { src = ev.source; });
	t.row.emit('e');
	ok(src === t.row);
});
check('event_target_for_row_action_is_row_not_card', () => {
	const t = T7(); t.attachAll();
	const got = [];
	t.reg('ord.act', (target, ev) => got.push([target.kind, ev.target.kind]));
	t.row.action('go', 'refresh', 'ord.act');
	t.row.el.querySelector('.juneau-model-actions').childNodes[0].dispatch('click', {});
	eq(got, [['row', 'row']]);
});
check('action_handler_target_is_declaring_handle_event_source_is_clicked_row', () => {
	const t = T7(); t.attachAll();
	const got = [];
	t.reg('ord.act', (target, ev) => got.push([target.kind, ev.source.kind, ev.source.id]));
	t.row.action('go', 'refresh', 'ord.act');
	t.row.el.querySelector('.juneau-model-actions').childNodes[0].dispatch('click', {});
	eq(got, [['row', 'row', '651']]);
});
check('emit_bubbles_to_page', () => {
	const t = T7(); t.attachAll();
	let n = 0;
	t.page.on('e', () => { n++; });
	t.tab.emit('e');
	eq(n, 1);
});
check('emit_app_listeners_before_model_on', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.reg('ord.decl', () => log.push('declared'));
	t.tab.bind('loaded', 'ord.decl');
	t.tab.on('loaded', () => log.push('app'));
	t.tab.emit('loaded');
	eq(log, ['app', 'declared']);
});
check('emit_stopPropagation', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.reg('ord.decl', () => log.push('declared'));
	t.tab.bind('loaded', 'ord.decl');
	t.tab.on('loaded', ev => { log.push('tab'); ev.stopPropagation(); });
	t.card.on('loaded', () => log.push('card'));
	t.tab.emit('loaded');
	eq(log, ['tab', 'declared']);
});
check('emit_custom_name', () => {
	const t = T7(); t.attachAll();
	let got;
	t.page.on('orders-counted', ev => { got = [ev.type, ev.payload]; });
	t.tab.emit('orders-counted', { count: 4 });
	eq(got, ['orders-counted', { count: 4 }]);
});
check('bind_undocumented_event_E121', () => {
	const t = T7(); t.reg('ord.h', () => {});
	throws(() => t.row.bind('loaded', 'ord.h'), 'E-JS-121');
	t.row.bind('expanded', 'ord.h');
});
check('listen_template_expansion', () => {
	const t = T7(); t.attachAll();
	for (const x of ['app.r:{rowKey}', 'app.t:{tabId}', 'app.c:{cardId}']) t.bus.declare(x.replace(/\{.*/, '*'), { retain: false });
	const got = [];
	t.reg('ord.l', (target, ev) => got.push([target.kind, ev.type, ev.payload.n]));
	t.tab.listen('app.r:{rowKey}', 'ord.l');
	t.tab.listen('app.t:{tabId}', 'ord.l');
	t.tab.listen('app.c:{cardId}', 'ord.l');
	t.bus.publish('app.r:651', { n: 1 });
	t.bus.publish('app.t:orders', { n: 2 });
	t.bus.publish('app.c:list', { n: 3 });
	t.bus.publish('app.r:999', { n: 4 });
	eq(got, [['detailtab', 'topic', 1], ['detailtab', 'topic', 2], ['detailtab', 'topic', 3]]);
	throws(() => t.tab.listen('app.r:{bogus}', 'ord.l'), 'E-JS-121');
});
check('listen_subscription_disposed_on_detach', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	let n = 0;
	t.reg('ord.l', () => { n++; });
	t.tab.listen('app.t', 'ord.l');
	t.bus.publish('app.t', { a: 1 });
	eq(n, 1);
	eq(topicCount(t, 'app.t'), 1);
	t.tab._detach();
	eq(topicCount(t, 'app.t'), 0);
	t.bus.publish('app.t', { a: 2 });
	eq(n, 1);
});
check('listen_resubscribed_on_reattach_once', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	let n = 0;
	t.reg('ord.l', () => { n++; });
	t.tab.listen('app.t', 'ord.l');
	t.tab._detach();
	t.tab._attach(t.doc.createElement('div'));
	t.tab._attach(t.doc.createElement('div'));
	t.bus.publish('app.t', { a: 1 });
	eq(n, 1);
	eq(topicCount(t, 'app.t'), 1);
});
check('app_subscribe_suspended_on_detach_not_doubled', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	let n = 0;
	t.tab.subscribe('app.t', () => { n++; });
	t.bus.publish('app.t', { a: 1 });
	eq(n, 1);
	t.tab._detach();
	eq(topicCount(t, 'app.t'), 0);
	t.bus.publish('app.t', { a: 2 });
	eq(n, 1);
	t.tab._attach(t.doc.createElement('div'));
	t.tab._attach(t.doc.createElement('div'));
	t.bus.publish('app.t', { a: 3 });
	eq(n, 2);
	eq(topicCount(t, 'app.t'), 1);
});
check('no_leaked_subscribers_after_destroy', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	t.reg('ord.l', () => {});
	t.tab.subscribe('app.t', () => {});
	t.tab.listen('app.t', 'ord.l');
	eq(topicCount(t, 'app.t'), 2);
	t.page._destroy();
	eq(topicCount(t, 'app.t'), 0);
});
check('destroy_drops_app_listeners', () => {
	const t = T7(); t.attachAll();
	let n = 0;
	t.tab.on('e', () => { n++; });
	t.tab._destroy();
	t.tab.emit('e');
	eq(n, 0);
});
check('publish_through_handle_owner', () => {
	const t = T7(); t.attachAll();
	t.bus.declare('app.t', { retain: false });
	const got = [];
	t.bus.subscribe('app.t', (p, m) => got.push([p.n, m.from]));
	t.tab.publish('app.t', { n: 7 });
	eq(got, [[7, t.tab.path]]);
});
check('handler_throw_painted_E118_target_only', () => {
	const t = T7(); t.attachAll();
	t.reg('bad.throw', () => { throw new Error('boom'); });
	t.card.bind('loaded', 'bad.throw');
	t.card._fire('loaded', {});
	const slot = t.card.el.querySelector('.jc-card-bus-error');
	ok(slot, 'painted on the target');
	eq(slot.dataset.juneauError, 'E-JS-118');
	ok(slot.textContent.includes('boom') && slot.textContent.includes('bad.throw'));
	eq(t.page.el.querySelector('.jc-card-bus-error'), null);
	eq(t.table.el.querySelector('.jc-card-bus-error'), null);
	ok(t.errors.some(m => m.includes('boom')));
});
check('handler_rejects_painted_E118', async () => {
	const t = T7(); t.attachAll();
	t.reg('bad.reject', () => Promise.reject(new Error('nope')));
	t.card.bind('loaded', 'bad.reject');
	t.card._fire('loaded', {});
	await tick(); await tick();
	const slot = t.card.el.querySelector('.jc-card-bus-error');
	ok(slot?.textContent.includes('nope'));
});
check('other_handlers_still_run_after_throw', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.reg('bad.throw', () => { throw new Error('boom'); });
	t.reg('ord.ok', () => log.push('declared'));
	t.card.bind('loaded', 'ord.ok');
	t.card.on('loaded', () => { throw new Error('app boom'); });
	t.card.on('loaded', () => log.push('app2'));
	t.card._fire('loaded', {});
	t.table.bind('loaded', 'bad.throw');
	t.table._fire('loaded', {});
	eq(log, ['app2', 'declared', 'app2', 'declared']);   // the table event bubbles to the card
});
check('detached_target_logs_only', () => {
	const t = T7(); t.attachAll();
	t.reg('bad.throw', () => { throw new Error('boom'); });
	t.card.bind('loaded', 'bad.throw');
	t.card._detach();
	t.card._fire('loaded', {});
	ok(t.errors.some(m => m.includes('boom')));
});
check('lifecycle_events_per_kind', () => {
	const t = T7(); t.attachAll();
	const EV = { page: ['loaded'], card: ['loaded', 'rendered'], table: ['loaded', 'rendered'], row: ['rendered', 'expanded', 'collapsed'],
		rowdetail: ['loaded', 'rendered'], detailtab: ['loaded', 'rendered', 'activated'], ribbonitem: ['activated'], pagetab: ['activated'],
		view: ['activated'], chart: ['loaded', 'rendered'] };
	eq(t.M.EVENTS, EV);
	const hits = [];
	const order = [['page', 'page'], ['card', 'card'], ['table', 'table'], ['row', 'row'], ['rowdetail', 'detail'], ['detailtab', 'tab']];
	for (const [kind, key] of order) for (const e of EV[kind]) {
		const u = T7(); u.attachAll();
		u.reg('ord.hit', (target, ev) => hits.push(target.kind + ':' + ev.type));
		u[key].bind(e, 'ord.hit'); u[key]._fire(e, {});
	}
	eq(hits, ['page:loaded', 'card:loaded', 'card:rendered', 'table:loaded', 'table:rendered', 'row:rendered', 'row:expanded', 'row:collapsed',
		'rowdetail:loaded', 'rowdetail:rendered', 'detailtab:loaded', 'detailtab:rendered', 'detailtab:activated']);
	for (const kind of ['ribbonitem', 'pagetab', 'view', 'chart']) {
		const h = new t.M.Handle(kind, {}, { id: 'x' });
		for (const e of EV[kind]) h.bind(e, 'ord.hit');
	}
});

//-----------------------------------------------------------------------------------------------------------------
// Handler resolution (the shell calls resolveHandlers after page scripts load)
//-----------------------------------------------------------------------------------------------------------------

check('resolve_unknown_E116_painted_inline_others_still_work', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.reg('ord.ok', () => log.push('ok'));
	t.card.bind('loaded', 'ord.missing');
	t.table.bind('loaded', 'ord.ok');
	t.M.resolveHandlers(t.doc, {});
	const slot = t.card.el.querySelector('.jc-card-bus-error');
	ok(slot, 'painted on the owner');
	eq(slot.dataset.juneauError, 'E-JS-116');
	ok(slot.textContent.includes('ord.missing'));
	eq(t.table.el.querySelector('.jc-card-bus-error'), null);
	t.table._fire('loaded', {});
	eq(log, ['ok']);
});
check('imperative_unknown_handler_throws_E116_at_call', () => {
	const t = T7();
	t.M.resolveHandlers(t.doc, {});
	throws(() => t.tab.bind('loaded', 'ord.nope'), 'E-JS-116');
	throws(() => t.tab.action('go', 'refresh', 'ord.nope'), 'E-JS-116');
	throws(() => t.tab.bind('loaded', 'Bad'), 'E-JS-114');
	throws(() => t.tab.bind('loaded', 'juneau.nothing'), 'E-JS-123');
});
check('resolve_after_scripts', () => {
	const t = T7(); t.attachAll();
	t.card.bind('loaded', 'ord.late-script');
	t.reg('ord.late-script', () => {});
	t.M.resolveHandlers(t.doc, {});
	eq(t.card.el.querySelector('.jc-card-bus-error'), null);
});
check('late_registration_still_E116', () => {
	const t = T7(); t.attachAll();
	let n = 0;
	t.card.bind('loaded', 'ord.late');
	t.M.resolveHandlers(t.doc, {});
	t.reg('ord.late', () => { n++; });
	t.card._fire('loaded', {});
	eq(n, 0);
	ok(t.card.el.querySelector('.jc-card-bus-error'));
});
check('replay_first_paint_events_once_in_order', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.card.bind('rendered', 'ord.r');
	t.table.bind('rendered', 'ord.r');
	t.card._fire('rendered', {});
	t.table._fire('rendered', {});
	eq(log, []);
	t.reg('ord.r', target => log.push(target.kind));
	t.M.resolveHandlers(t.doc, {});
	eq(log, ['card', 'table', 'card']);   // the table event bubbled to the card
	t.M.resolveHandlers(t.doc, {});
	eq(log, ['card', 'table', 'card']);
	t.card._fire('rendered', {});
	eq(log, ['card', 'table', 'card', 'card']);
});
check('handler_error_reuses_one_slot_per_code', () => {
	const t = T7(); t.attachAll();
	t.reg('bad.throw', () => { throw new Error('boom'); });
	t.card.bind('loaded', 'bad.throw');
	for (let i = 0; i < 3; i++) t.card._fire('loaded', {});
	eq(t.card.el.querySelectorAll('.jc-card-bus-error').length, 1);
	eq(t.errors.filter(m => m.includes('boom')).length, 3);
});
check('replay_skips_destroyed_handles', () => {
	const t = T7(); t.attachAll();
	const log = [];
	t.card.bind('rendered', 'ord.d');
	t.table.bind('rendered', 'ord.d');
	t.card._fire('rendered', {});
	t.table._fire('rendered', {});
	t.table._destroy();
	t.reg('ord.d', target => log.push(target.kind));
	t.M.resolveHandlers(t.doc, {});
	ok(!log.includes('table'), 'destroyed handle not replayed');
	ok(log.includes('card'));
});
check('inline_without_opt_in_E119', () => {
	const t = T7();
	throws(() => t.tab.bind('loaded', { inline: 'return 1' }), 'E-JS-119');
});
check('inline_without_file_E119', () => {
	const t = T7();
	const p = new t.M.Page({ id: 'p2', title: 'T', nav: [], activeNav: [], allowInlineHandlers: true }), c = new t.M.Card({ id: 'c2' });
	p._add(c);
	throws(() => c.bind('loaded', { inline: 'return 1' }), 'E-JS-119');
});
check('inline_with_compiler_ok', () => {
	const t = T7();
	const p = new t.M.Page({ id: 'p2', title: 'T', nav: [], activeNav: [], allowInlineHandlers: true }), c = new t.M.Card({ id: 'c2' });
	p._add(c);
	const compiled = [];
	t.M.inline.compile = body => { compiled.push(body); return () => compiled.push('ran'); };
	c.bind('loaded', { inline: 'return 1' });
	c._attach(t.doc.createElement('div'));
	c._fire('loaded', {});
	eq(compiled, ['return 1', 'ran']);
});


//-----------------------------------------------------------------------------------------------------------------
// Building the tree from JSON and mounting it
//-----------------------------------------------------------------------------------------------------------------

const PAGE = o => ({ contractVersion: '1', modelVersion: '1', id: 'pg', title: 'T', nav: [{ id: 'home', label: 'Home' }], activeNav: ['home'], cards: [], ...o });
const mountPage = (w, contract, extra) => w.JC.mount(contract, { root: w.doc.body, document: w.doc, ...extra });
const regProbe = (w, log) => {
	w.JC.registerCard('probe', class extends w.M.Card { render() { if (log) { log.push('render:' + this.id); } this.el.appendChild(w.doc.createTextNode('p')); } });
};
check('fromJSON_builds_page_cards_nav', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ nav: [{ id: 'a', label: 'A', children: [{ id: 'a1', label: 'A1' }] }, { id: 'b', label: 'B' }], cards: [{ id: 'c1', type: 'html' }, { id: 'c2', type: 'html' }] }));
	ok(page instanceof w.M.Page);
	eq(page.id, 'pg');
	eq(page.tabs.map(t => t.id), ['a', 'b']);
	eq(page.tabs[0].children.map(t => t.id), ['a1']);
	eq(page.cards.map(c => c.id), ['c1', 'c2']);
	ok(page.cards[0] instanceof w.M.Card);
	eq(page.cards[0].path, 'page/pg/card:c1');
});
check('fromJSON_card_ids_required_E121', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ cards: [{ type: 'html' }, { id: 'ok', type: 'html' }] }));
	eq(page.cards.map(c => c.id), ['ok']);
	eq(page.findings.map(f => f.code), ['E-JS-121']);
});
check('fromJSON_ribbon_item_ids_required_E121', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ cards: [{ id: 'c1', type: 'datatables', table: { ribbon: [{ type: 'divider' }, { type: 'action' }, { type: 'refresh' }, { type: 'action', id: 'go' }] } }] }));
	const rb = page.card('c1').find('ribbon');
	eq(rb.items.map(i => i.id), ['refresh', 'go']);
	eq(page.findings.map(f => f.code), ['E-JS-121']);
});
check('fromJSON_divider_no_handle', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ cards: [{ id: 'c1', type: 'datatables', table: { ribbon: [{ type: 'divider' }, { type: 'refresh' }] } }] }));
	eq(page.card('c1').find('ribbon').items.length, 1);
	eq(page.findings.length, 0);
});
check('fromJSON_model_blocks_become_declared', () => {
	const w = load({ shell: true, ribbon: true });
	w.NS.handlers.register('app.h1', () => {});
	const page = w.M.fromJSON(PAGE({
		model: { on: { loaded: 'app.h1' } },
		cards: [{ id: 'c1', type: 'html', model: { actions: [{ type: 'handler', id: 'go', icon: 'x', handler: 'app.h1' }], subscribes: [{ topic: 'a.b', handler: 'app.h1' }] } }]
	}));
	eq(page._declared.on.loaded.length, 1);
	const c = page.card('c1');
	eq(c._declared.actions.map(a => a.id), ['go']);
	eq(c._declared.subscribes.length, 1);
	eq(page.findings.length, 0);
});
check('fromJSON_bad_handler_is_finding_not_throw', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ cards: [{ id: 'c1', type: 'html', model: { on: { loaded: 'BadName', nope: 'a.b' } } }] }));
	eq(page.findings.map(f => f.code), ['E-JS-121', 'E-JS-121']);
});
check('fromJSON_detail_template_and_tabs', () => {
	const w = load({ shell: true, ribbon: true });
	const page = w.M.fromJSON(PAGE({ cards: [{ id: 'c1', type: 'datatables', table: { detail: { tabs: [{ id: 'orders' }, { id: 'items' }] } } }] }));
	const d = page.card('c1').find('table').detail;
	ok(d.template);
	eq(d.tabs.map(t => t.id), ['orders', 'items']);
});
check('mount_creates_page_handle_with_paths', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe' }] }));
	const page = w.M.page();
	ok(page?.attached);
	ok(w.M.get('page/pg') === page);
	ok(w.M.get('page/pg/card:p1').attached);
	eq(page.tabs[0].path, 'page/pg/pagetab:home');
});
check('mount_card_handles_are_instances_returned_by_cardApi', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe' }] }));
	ok(w.JC.cardApi('p1') === w.M.get('page/pg/card:p1'));
	ok(w.JC.cardApi('p1') instanceof w.M.Card);
});
check('mount_html_builtin_card_is_handle', () => {
	const w = load({ shell: true, ribbon: true });
	mountPage(w, PAGE({ cards: [{ id: 'h1', type: 'html', body: 'x' }] }));
	ok(w.M.get('page/pg/card:h1') !== null);
});
check('mount_replays_rendered_after_resolution', async () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	const log = [];
	w.NS.handlers.register('app.rend', (t, ev) => log.push(ev.type + ':' + t.id));
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe', model: { on: { rendered: 'app.rend' } } }] }));
	await tick();
	eq(log, ['rendered:p1']);
});
check('unmount_destroys_tree', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe' }] }));
	const page = w.M.page();
	page.unmount();
	ok(page.destroyed);
	eq(w.M.get('page/pg'), null);
	eq(w.M.get('page/pg/card:p1'), null);
	eq(w.M.page(), null);
});
check('second_mount_replaces_page', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe' }] }));
	const first = w.M.page();
	const root2 = w.doc.createElement('div');
	w.doc.body.appendChild(root2);
	w.JC.mount(PAGE({ id: 'pg2', cards: [{ id: 'q1', type: 'probe' }] }), { root: root2, document: w.doc });
	ok(first.destroyed);
	eq(w.M.page().id, 'pg2');
	eq(w.M.get('page/pg'), null);
	ok(w.M.get('page/pg2/card:q1'));
});
check('premade_handles_win_over_json_instances', () => {
	const w = load({ shell: true, ribbon: true });
	let constructed = 0;
	w.JC.registerCard('probe', class extends w.M.Card { constructor(s, o) { super(s, o); constructed++; } render() { /* no-op */ } });
	const pre = new (w.JC.cardClass('probe'))({ id: 'p1' });
	const before = constructed;
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe' }] }), { instances: { p1: pre } });
	eq(constructed, before);
	ok(w.JC.cardApi('p1') === pre);
	ok(w.M.get('page/pg/card:p1') === pre);
	ok(pre.attached);
});
check('late_registered_shell_card_subclass_is_not_adopted', async () => {
	const w = load({ shell: true, ribbon: true });
	let rendered = false;
	w.doc.readyState = 'loading';
	mountPage(w, PAGE({ cards: [{ id: 'l1', type: 'later' }] }));
	let threw = null;
	try { w.JC.registerCard('later', class extends w.JC.Card { render() { rendered = true; } }); } catch (e) { threw = String(e); }
	w.env.fireDOMContentLoaded();
	for (let i = 0; i < 5; i++) await tick();
	eq(threw, null);
	ok(rendered);
});
check('page_loaded_event_after_all_cards_settle', async () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	const log = [];
	w.NS.handlers.register('app.pl', (t, ev) => log.push(t.kind + ':' + ev.type));
	mountPage(w, PAGE({ model: { on: { loaded: 'app.pl' } }, cards: [{ id: 'p1', type: 'probe' }, { id: 'p2', type: 'probe' }] }));
	await tick();
	eq(log, ['page:loaded']);
});
check('mount_unregistered_name_painted_E116_page_renders', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe', model: { on: { rendered: 'app.missing' } } }, { id: 'p2', type: 'probe' }] }));
	ok(w.doc.getElementById('p1').querySelector('[data-juneau-error="E-JS-116"]'));
	ok(w.doc.getElementById('p2'));
});
check('mount_json_findings_painted_inline', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	mountPage(w, PAGE({ cards: [{ id: 'p1', type: 'probe', model: { on: { rendered: 'BadName' } } }, { id: 'p2', type: 'probe' }] }));
	ok(w.doc.getElementById('p1').querySelector('[data-juneau-error="E-JS-121"]'));
	ok(w.doc.getElementById('p2').textContent.includes('p'));
});

//-----------------------------------------------------------------------------------------------------------------
// Client validation: the shared corpus, the schema port, the pack check
//-----------------------------------------------------------------------------------------------------------------

const CORPUS = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../resources/model-validation-corpus.json'), 'utf8')).cases;
const SCHEMA = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../../../juneau-console/src/main/resources/org/apache/juneau/console/juneau-page.schema.json'), 'utf8'));
const SCHEMA_ROOT_KEYS = ['contractVersion', 'title', 'nav', 'activeNav', 'cards'];
const prep = (w, c) => {
	w.NS.handlers.register('app.hello', () => {});
	regProbe(w);
};
const codesOf = list => list.map(f => f.code);
const stripForSchema = c => { const x = structuredClone(c); delete x.id; delete x.modelVersion; for (const n of x.nav || []) { if (!n.children) { n.href = '/' + n.id; } } return x; };

// One mirror per corpus case name (a Java check without its JS twin has no entry here and fails corpus_every_case_has_mirror).
const MIRRORS = {};
const jsonMirror = c => () => {
	const w = load({ shell: true, ribbon: true });
	prep(w, c);
	if (c.schema) {
		const base = w.M.validate(stripForSchema(PAGE({})), { schema: SCHEMA });
		eq(codesOf(base), [], 'a valid page has no findings');
		const got = w.M.validate(stripForSchema(c.contract), { schema: SCHEMA });
		ok(got.some(f => f.code === c.jsonCode && f.message.startsWith('schema:')), 'schema finding missing: ' + JSON.stringify(got));
		return;
	}
	if (c.fatal) {
		throws(() => mountPage(w, c.contract), c.jsonCode);
		ok(codesOf(w.M.validate(c.contract)).includes(c.jsonCode), 'validate lacks ' + c.jsonCode);
		return;
	}
	const found = codesOf(w.M.validate(c.contract));
	ok(found.includes(c.jsonCode), 'validate lacks ' + c.jsonCode + ': ' + JSON.stringify(found));
	if (c.base) ok(!codesOf(w.M.validate(c.base)).includes(c.jsonCode), 'the base page already reports ' + c.jsonCode);
	mountPage(w, c.contract);                              // the page still renders; findings are recorded, not thrown
	ok(codesOf(w.M.page().findings).includes(c.jsonCode), 'mount lacks finding ' + c.jsonCode);
	ok(w.doc.getElementById('c1') || w.doc.getElementById('t1') || c.contract.nav.length, 'page did not render');
};
const imperativeMirror = c => () => {
	const w = load({ shell: true, ribbon: true });
	prep(w, c);
	const vars = {};
	const page = c.base ? w.M.fromJSON(c.base) : null;
	const target = sel => {
		if (sel[0] === '@') return vars[sel.slice(1)];
		if (sel === 'page/pg') return page;
		return page.find(sel.replace(/^page\/pg\//, ''));
	};
	const run = () => {
		for (const step of c.build) {
			const args = step.args.map(a => a);
			if (step.ctor) vars[step.as] = new w.M[step.ctor](...args);
			else { const t = target(step.on); ok(t, 'no handle at ' + step.on); t[step.call](...args); }
		}
	};
	throws(run, c.imperativeCode);
};
for (const c of CORPUS) {
	MIRRORS[c.name] = c;
	if (c.jsonCode) check(c.name + '_json', jsonMirror(c));
	if (c.imperativeCode) check(c.name + '_imperative', imperativeMirror(c));
}
check('corpus_every_case_has_mirror', () => {
	for (const c of CORPUS) {
		ok(MIRRORS[c.name], 'no mirror for ' + c.name);
		ok(c.jsonCode || c.imperativeCode, c.name + ' has neither jsonCode nor imperativeCode');
		if (c.imperativeCode) ok(Array.isArray(c.build) && c.build.length, c.name + ' needs a build array');
	}
	ok(CORPUS.length >= 25);
});
check('validate_never_throws', () => {
	const w = load({ shell: true, ribbon: true });
	for (const v of [null, undefined, 5, 'x', [], {}, { cards: 5 }, { contractVersion: '1', cards: [null, 3, { id: 1 }] }])
		ok(Array.isArray(w.M.validate(v)));
});
check('validate_valid_page_has_no_findings', () => {
	const w = load({ shell: true, ribbon: true });
	eq(w.M.validate(PAGE({ cards: [{ id: 'c1', type: 'probe' }] })), []);
});
check('validate_findings_shape', () => {
	const w = load({ shell: true, ribbon: true });
	const f = w.M.validate(PAGE({ cards: [{ id: 'c1', type: 'probe', model: { on: { rendered: 'BadName' } } }] }));
	ok(f.length === 1 && f[0].code === 'E-JS-121' && typeof f[0].path === 'string' && typeof f[0].message === 'string', JSON.stringify(f));
});
check('schema_port_keywords_equal_java_set', () => {
	const w = load({ shell: true, ribbon: true });
	const java = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../resources/schema-subset-keywords.json'), 'utf8'));
	eq(w.M.SCHEMA_KEYWORDS.slice().sort(), java.slice().sort());
});
check('schema_port_full_schema_accepts_valid_page', () => {
	const w = load({ shell: true, ribbon: true });
	eq(w.M.validate(stripForSchema(PAGE({ cards: [{ id: 'h1', type: 'html', template: 't' }] })), { schema: SCHEMA }), []);
});
check('schema_pass_opt_in_on_json_mount', () => {
	const w = load({ shell: true, ribbon: true });
	regProbe(w);
	const bad = PAGE({ bogusKey: 1, cards: [{ id: 'p1', type: 'probe' }] });
	w.M.schema = SCHEMA;
	mountPage(w, bad);
	ok(!codesOf(w.M.page().findings).length, 'the schema pass must be off by default');
	const w2 = load({ shell: true, ribbon: true });
	regProbe(w2);
	w2.M.schema = SCHEMA;
	w2.M.validateOnMount = true;
	mountPage(w2, bad);
	ok(w2.M.page().findings.some(f => /schema:/.test(f.message)), 'schema pass did not run');
	ok(w2.doc.getElementById('p1'), 'page must still render');
});
check('pack_missing_reported', () => {
	const w = load({ shell: true, ribbon: true });
	w.JC.registerCard('needy', class extends w.M.Card { static requiredPacks(spec) { return ['nopack']; } render() { /* no-op */ } });
	const f = w.M.validate(PAGE({ cards: [{ id: 'n1', type: 'needy' }] }));
	ok(f.some(x => x.code === 'E-JS-121' && /nopack/.test(x.message)), JSON.stringify(f));
});

//-----------------------------------------------------------------------------------------------------------------
// Imperative construction and toJSON
//-----------------------------------------------------------------------------------------------------------------

const ISPEC = o => ({ id: 'pets', title: 'Pets', nav: [], activeNav: [], modelVersion: '1', ...o });
const petsTable = () => new (load({ shell: true }).M.Table)({});
const iw = () => { const w = load({ shell: true, ribbon: true }); w.NS.handlers.register('app.hello', () => {}); regProbe(w); return w; };
const TBL = { id: 'pets', dataUrl: '/api/pets', selection: { rowIdField: 'id' }, columns: [{ key: 'id' }, { key: 'name' }] };
check('ctor_Page_requires_title_nav_activeNav_E121', () => {
	const w = iw();
	for (const k of ['title', 'nav', 'activeNav']) {
		const spec = ISPEC(); delete spec[k];
		throws(() => new w.M.Page(spec), 'E-JS-121');
	}
	ok(new w.M.Page(ISPEC()) instanceof w.M.Page);
});
check('ctor_Page_contractVersion_default', () => {
	const w = iw();
	eq(new w.M.Page(ISPEC()).toJSON().contractVersion, '1');
	throws(() => new w.M.Page(ISPEC({ contractVersion: '9' })), 'E-JS-2');
});
check('ctor_Page_modelVersion_bad_E120_thrown', () => {
	const w = iw();
	throws(() => new w.M.Page(ISPEC({ modelVersion: '7' })), 'E-JS-120');
});
check('page_add_card_wraps_bare_Table', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC()), t = new w.M.Table(TBL);
	page.add(t);
	const card = page.card('pets');
	ok(card?.spec.type === 'datatables', 'wrapped card');
	ok(t.parent === card && t.path === 'page/pets/card:pets/table', t.path);
	const j = page.toJSON().cards[0];
	eq(j.id, 'pets'); eq(j.type, 'datatables'); eq(j.table.dataUrl, '/api/pets');
});
check('page_addCard_unknown_type_E4', () => {
	const w = iw();
	throws(() => new w.M.Page(ISPEC()).addCard('nope', { id: 'x' }), 'E-JS-4');
});
check('add_already_parented_E121', () => {
	const w = iw();
	const a = new w.M.Page(ISPEC()), b = new w.M.Page(ISPEC({ id: 'b' }));
	const c = a.addCard('probe', { id: 'c1' });
	throws(() => b.add(c), 'E-JS-121');
});
check('add_sibling_collision_E121', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC());
	page.addCard('probe', { id: 'c1' });
	throws(() => page.addCard('probe', { id: 'c1' }), 'E-JS-121');
	throws(() => page.addCard('probe', {}), 'E-JS-121');
});
check('path_deferred_until_root', () => {
	const w = iw();
	const c = new w.M.Card({ id: 'c1' }), t = new w.M.Table(TBL);
	c._add(t);
	eq(t.path, null);
	const page = new w.M.Page(ISPEC());
	page.add(c);
	eq(t.path, 'page/pets/card:c1/table');
});
check('path_assigned_at_add', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC({ nav: [{ id: 'home', label: 'Home', href: '/h' }] }));
	eq(page.tabs[0].path, 'page/pets/pagetab:home');
	eq(page.addCard('probe', { id: 'c1' }).path, 'page/pets/card:c1');
});
const withDetail = w => {
	const page = new w.M.Page(ISPEC()), t = new w.M.Table(TBL);
	page.add(t);
	return { page, t, d: t.setDetail({ endpoint: '/api/pets/{id}/detail', title: 'Pet' }) };
};
check('table_setDetail_template_path', () => {
	const w = iw();
	const { t, d } = withDetail(w);
	ok(d.template && t.detail === d);
	eq(d.path, 'page/pets/card:pets/table/detail');
});
check('detail_addTab_template', () => {
	const w = iw();
	const { d } = withDetail(w);
	const tab = d.addTab({ id: 'orders', title: 'Orders' }).bind('loaded', 'app.hello');
	eq(tab.path, 'page/pets/card:pets/table/detail/tab:orders');
	ok(d.tab('orders') === tab);
	throws(() => d.addTab({ id: 'orders', title: 'x' }), 'E-JS-121');
	throws(() => d.addTab({ title: 'x' }), 'E-JS-121');
});
check('template_change_applies_to_future_instances', () => {
	const w = iw();
	const { t, d } = withDetail(w);
	d.addTab({ id: 'orders', title: 'Orders' });
	d.bind('loaded', 'app.hello');
	const row = t._newRow('651', false);
	row._attach({ getAttribute: () => '651' });
	const tr = row._el;
	t._rowExpanded(tr, { querySelectorAll: () => [] });
	const inst = row.detail;
	ok(inst && !inst.template && inst.tabs.map(x => x.id).join() === 'orders');
	eq(inst._declared.on.loaded.length, 1);
});
check('instance_addTab_transient', () => {
	const w = iw();
	const { t, d } = withDetail(w);
	d.addTab({ id: 'orders', title: 'Orders' });
	const row = t._newRow('651', false);
	row._attach({ getAttribute: () => '651' });
	t._rowExpanded(row._el, { querySelectorAll: () => [] });
	row.detail.addTab({ id: 'extra', title: 'Extra' });
	eq(row.detail.tabs.map(x => x.id), ['orders', 'extra']);
	eq(d.tabs.map(x => x.id), ['orders']);
	t._rowCollapsed(row._el);
	t._rowExpanded(row._el, { querySelectorAll: () => [] });
	eq(row.detail.tabs.map(x => x.id), ['orders']);
});
check('ribbon_addItem_requires_id', () => {
	const w = iw();
	const { t } = withDetail(w);
	throws(() => t.ribbon.addItem({ type: 'action' }), 'E-JS-121');
	const it = t.ribbon.addItem({ type: 'action', id: 'reload', icon: 'refresh-cw' });
	eq(it.path, 'page/pets/card:pets/table/ribbon/item:reload');
	ok(t.ribbon.addItem({ type: 'refresh' }).id === 'refresh');
	throws(() => t.ribbon.addItem({ type: 'action', id: 'reload' }), 'E-JS-121');
});
check('removeTab_removeItem_remove_destroy', () => {
	const w = iw();
	const { page, t, d } = withDetail(w);
	const tab = d.addTab({ id: 'orders', title: 'O' });
	const it = t.ribbon.addItem({ type: 'action', id: 'go' });
	d.removeTab('orders'); t.ribbon.removeItem('go');
	ok(tab.destroyed && it.destroyed && d.tabs.length === 0 && t.ribbon.items.length === 0);
	const c = page.addCard('probe', { id: 'c9' });
	c.remove();
	ok(c.destroyed && page.card('c9') === null);
});
check('toJSON_page_full_contract_validates_against_schema', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC({ nav: [{ id: 'home', label: 'Home', href: '/h' }], activeNav: ['home'] }));
	const t = new w.M.Table(TBL);
	page.add(t);
	t.ribbon.addItem({ type: 'refresh' });
	t.setDetail({ endpoint: '/api/pets/{id}/detail', title: 'Pet' }).addTab({ id: 'orders', title: 'Orders' });
	const j = page.toJSON();
	const f = w.M.validate(stripForSchema(j), { schema: SCHEMA });
	eq(f.filter(x => x.message.startsWith('schema:')), []);
});
check('toJSON_includes_optional_root_keys', () => {
	const w = iw();
	const j = new w.M.Page(ISPEC({ theme: 'dark', footer: { text: 'x' }, allowInlineHandlers: true, scripts: ['/a.js'] })).toJSON();
	eq([j.theme, j.footer.text, j.allowInlineHandlers, j.scripts[0]], ['dark', 'x', true, '/a.js']);
	eq(j.cards, []);
});
check('toJSON_includes_model_blocks', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC());
	const c = page.addCard('probe', { id: 'c1' });
	c.action('go', 'refresh-cw', 'juneau.refresh').bind('rendered', 'app.hello').listen('app.x:{cardId}', 'app.hello');
	eq(page.toJSON().cards[0].model, {
		actions: [{ id: 'go', icon: 'refresh-cw', handler: 'juneau.refresh' }],
		on: { rendered: 'app.hello' }, subscribes: [{ topic: 'app.x:{cardId}', handler: 'app.hello' }]
	});
});
check('h_action_bind_listen_write_under_model', () => {
	const w = iw();
	const { t, d } = withDetail(w);
	t.action('reload', 'refresh-cw', 'juneau.refresh');
	d.action('refresh', 'refresh-cw', 'juneau.refresh');
	eq(t.toJSON().model.actions[0].id, 'reload');
	const dj = d.toJSON();
	eq(dj.actions, [{ type: 'handler', id: 'refresh', icon: 'refresh-cw', handler: 'juneau.refresh' }]);
	ok(!dj.model);
});
check('fromJSON_toJSON_roundtrip_equal', () => {
	const w = iw();
	const src = PAGE({ nav: [{ id: 'home', label: 'Home', href: '/h' }], activeNav: ['home'], cards: [
		{ id: 'c1', type: 'probe', model: { on: { rendered: 'app.hello' } } },
		{ id: 't1', type: 'datatables', table: { dataUrl: '/api/a', columns: [{ key: 'id' }], model: { actions: [{ id: 'go', handler: 'juneau.refresh' }] },
			ribbon: [{ type: 'refresh' }, { type: 'divider' }, { type: 'option', id: 'mine', title: 'Mine' }],
			detail: { endpoint: '/d/{id}', tabs: [{ id: 'o', title: 'O', model: { on: { loaded: 'app.hello' } } }], actions: [{ type: 'handler', id: 'rf', handler: 'juneau.refresh' }] } } }
	] });
	const canon = v => {
		if (Array.isArray(v)) return v.map(canon);
		if (v && typeof v === 'object') return Object.fromEntries(Object.keys(v).sort((a, b) => (a < b ? -1 : Number(a > b))).map(k => [k, canon(v[k])]));
		return v;
	};
	eq(canon(w.M.fromJSON(src).toJSON()), canon(src));
});
check('build_equals_fromJSON_mount', () => {
	const w = iw();
	const src = PAGE({ cards: [{ id: 'c1', type: 'probe' }] });
	w.M.build(src, { root: w.doc.body, document: w.doc });
	const a = w.doc.getElementById('c1') ? 1 : 0;
	const w2 = iw();
	w2.M.fromJSON(src).mount({ root: w2.doc.body, document: w2.doc });
	eq(w2.doc.getElementById('c1') ? 1 : 0, a);
	ok(a === 1);
	ok(w.M.page().card('c1').attached && w2.M.page().card('c1').attached);
});
check('identity_kept_through_mount', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC({ id: 'pg' }));
	const c = page.addCard('probe', { id: 'c1' });
	const t = new w.M.Table(TBL);
	page.add(t);
	page.mount({ root: w.doc.body, document: w.doc });
	ok(w.M.get('page/pg') === page, 'page');
	ok(w.M.get('page/pg/card:c1') === c && c.attached, 'card');
	ok(w.JC.cardApi('c1') === c, 'cardApi');
	ok(w.M.get('page/pg/card:pets') !== null, 'table card ' + JSON.stringify(w.errors));
});
check('page_mount_calls_shell_mount_with_toJSON_and_instances', () => {
	const w = iw();
	const page = new w.M.Page(ISPEC({ id: 'pg' }));
	const c = page.addCard('probe', { id: 'c1' });
	let seen = null;
	const real = w.JC.mount;
	w.JC.mount = (contract, opts) => { seen = { contract, opts }; return real(contract, opts); };
	page.mount({ root: w.doc.body, document: w.doc });
	eq(seen.contract, page.toJSON());
	ok(seen.opts.instances.c1 === c);
});
check('imperative_bad_handler_throws_E114_E123_E119_at_call', () => {
	const w = iw();
	const c = new w.M.Page(ISPEC()).addCard('probe', { id: 'c1' });
	throws(() => c.bind('rendered', 'BadName'), 'E-JS-114');
	throws(() => c.bind('rendered', 'juneau.bogus'), 'E-JS-123');
	throws(() => c.bind('rendered', { inline: 'return 1;' }), 'E-JS-119');
	eq(w.errors.filter(e => /E-JS-1(14|23|19)/.test(e)).length, 0, 'thrown, not painted');
});
check('prebuilt_handles_win_over_shell_instances', () => {
	const w = iw();
	let built = 0;
	w.JC.registerCard('counted', class extends w.M.Card { constructor(s, o) { super(s, o); built++; } render() { /* no-op */ } });
	const page = new w.M.Page(ISPEC({ id: 'pg' }));
	const c = page.addCard('counted', { id: 'k1' });
	const before = built;
	page.mount({ root: w.doc.body, document: w.doc });
	eq(built, before);
	ok(w.JC.cardApi('k1') === c);
});

check('inline_compiles_body_to_fn_target_event', () => {
	const w = load({ shell: true, ribbon: true, inline: true });
	ok(typeof w.M.inline.compile === 'function', 'compiler installed');
	const fn = w.M.inline.compile('target.seen = event.x;');
	const t = {};
	fn(t, { x: 7 });
	eq(t.seen, 7);
});
check('inline_compile_error_E119', () => {
	const w = load({ shell: true, ribbon: true, inline: true });
	const p = new w.M.Page(ISPEC({ allowInlineHandlers: true })), c = new w.M.Card({ id: 'c2' });
	p._add(c);
	const e = throws(() => c.bind('loaded', { inline: 'if (' }), 'E-JS-119');
	ok(/inline handler on/.test(e.message), e.message);
});
check('inline_csp_evalerror_E119_mentions_unsafe_eval', () => {
	const w = load({ shell: true, ribbon: true });
	const p = new w.M.Page(ISPEC({ allowInlineHandlers: true })), c = new w.M.Card({ id: 'c2' });
	p._add(c);
	w.M.inline.compile = () => { throw new EvalError('blocked'); };
	const e = throws(() => c.bind('loaded', { inline: 'return 1' }), 'E-JS-119');
	ok(/unsafe-eval/.test(e.message), e.message);
});
check('inline_opt_in_required', () => {
	const w = load({ shell: true, ribbon: true });
	const p = new w.M.Page(ISPEC({})), c = new w.M.Card({ id: 'c2' });
	p._add(c);
	throws(() => c.bind('loaded', { inline: 'return 1' }), 'E-JS-119');
});

(async () => {
	for (const f of pending) await f();
	process.stdout.write(JSON.stringify(report));
})();
