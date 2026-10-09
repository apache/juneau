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
 * ribbon-bus.cjs - Node harness for the ribbon-as-messages half of the message bus addendum (spec 5.5).
 *
 *   Usage:  node ribbon-bus.cjs <juneau-bus.js> <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>
 *
 * Builds real ribbons (NS.ribbon.build) over hand-built table ctxs with a fake DataTables API, stands in for the
 * console shell by routing cmd:<id> to NS.tableBus.applyCmd, clicks the buttons, and prints ONE JSON report.  Every
 * assertion lives in ViewsJs_RibbonBus_Test / ViewsJs_RibbonDefaultFilter_Test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require('./views-dom-shim.cjs');

const scripts = process.argv.slice(2);
if (scripts.length !== 4) {
	console.error('usage: node ribbon-bus.cjs <juneau-bus.js> <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>');
	process.exit(2);
}

const errors = [];
const con = Object.assign(Object.create(console), {
	error: function (...a) { errors.push(a.join(' ')); },
	warn: function (...a) { errors.push('WARN ' + a.join(' ')); }
});
const env = makeEnv();
env.window.console = con;
const sandbox = {
	window: env.window, document: env.document, console: con,
	setTimeout: setTimeout, clearTimeout: clearTimeout,
	setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ },
	queueMicrotask: queueMicrotask, Promise: Promise,
	fetch: function () { return Promise.reject(new Error('no network in ribbon-bus.cjs')); }
};
scripts.forEach(function (p) {
	// NOSONAR javascript:S1523 -- loading the production sources into a VM sandbox is this harness's mechanism.
	vm.runInNewContext(fs.readFileSync(path.resolve(p), 'utf8'), sandbox, { filename: path.basename(p) }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled scripts
});
const NS = env.window.JuneauViews;
const TB = NS.tableBus;
const store = env.window.localStorage;
const tick = () => new Promise(r => setImmediate(r));
const J = v => JSON.stringify(v);
const last = a => a[a.length - 1];

// The live DataTables column array: a leading selection column, so every catalog index is shifted by one.
const OPTS_COLUMNS = [
	{ data: null, className: 'juneau-view-select-cell' },
	{ data: 'id' }, { data: 'owner' }, { data: 'state' }, { data: 'isNew' }, { data: 'region' }
];
const AGE = { type: 'optionGroup', id: 'age', persist: true, default: 'new', options: [
	{ id: 'new', title: 'New', column: 'isNew', value: '^yes$' },
	{ id: 'all', title: 'All' }
] };

function viewDef(id, mode, extra) {
	return {
		id: id, dataMode: mode, dataUrl: '/api/' + id, pollIntervalMs: 30000,
		columns: [{ data: 'id' }, { data: 'owner' }, { data: 'state' }, { data: 'isNew' }, { data: 'region' }],
		ribbon: [
			{ type: 'refresh' },
			{ type: 'option', id: 'openOnly', title: 'Open only', column: 'state', value: 'open' },
			AGE,
			{ type: 'pausePolling' },
			{ type: 'collapseAll' }
		].concat(extra || [])
	};
}

// A fake DataTables API (the shape table-bus.cjs uses): records every column-search/draw/reload call.
function fakeDt() {
	const handlers = {};
	const colSearch = {};
	const cols = {};
	const log = { columnSearch: [], draws: [], reloads: [] };
	let global = '';
	const dt = {
		log: log,
		on: function (evt, fn) { (handlers[evt] = handlers[evt] || []).push(fn); return dt; },
		fire: function (evt, e) { (handlers[evt] || []).forEach(function (fn) { fn(e); }); },
		search: function (v) { if (arguments.length === 0) return global; global = v; return dt; },
		column: function (i) {
			if (cols[i]) return cols[i];
			const col = { index: function () { return i; }, draw: function () { return dt; } };
			col.search = function (v, regex, smart) {
				if (arguments.length === 0) return colSearch[i] || '';
				colSearch[i] = v;
				log.columnSearch.push({ index: i, value: v, regex: !!regex, smart: smart !== false });
				return col;
			};
			col.search.fixed = function () { return col; };
			cols[i] = col;
			return col;
		},
		rows: function () { return { data: function () { return { toArray: function () { return []; } }; } }; },
		row: function () { return { data: function () { return null; } }; },
		table: function () { return { node: function () { return null; } }; },
		page: { info: function () { return { page: 0, length: 25, pages: 1, recordsDisplay: 0 }; } },
		draw: function (paging) { log.draws.push(paging === undefined ? null : paging); return dt; },
		ajax: { reload: function (cb, reset) { log.reloads.push(reset === undefined ? null : reset); } }
	};
	return dt;
}

/** A view table in a card host.  `card` null = a plain page.  activeState comes from the REAL loadPersistedState. */
function makeTable(def, card) {
	const host = env.el('div');
	const table = env.el('table');
	table.setAttribute('data-juneau-view', def.id);
	host.appendChild(table);
	env.body.appendChild(host);
	if (card) {
		const own = NS.bus.owner(card.id);
		TB.implicitTopics(card).forEach(function (t) { own.claim(t); });
		host.__juneauBus = TB.cardBus(card, own);
	}
	const dt = fakeDt();
	const ctx = {
		table: table, viewDef: def, dataTable: dt, activeState: NS.ribbon.loadPersistedState(def),
		clientFiltered: def.dataMode !== 'server',
		selectionState: null, optsColumns: OPTS_COLUMNS, collapses: 0, rowFilterRefreshes: 0
	};
	if (ctx.clientFiltered) ctx.refreshRibbonRowFilter = function () { ctx.rowFilterRefreshes++; };
	ctx.redraw = function () {
		if (ctx.refreshRibbonRowFilter) ctx.refreshRibbonRowFilter();
		dt.ajax.reload();
	};
	ctx.collapseAllDetailRows = function () { ctx.collapses++; };
	ctx.bus = TB.findBus(table);
	table.__juneauCtx = ctx;
	return { table: table, ctx: ctx, dt: dt };
}

/** The console shell, reduced to what this harness needs: cmd:<id> -> the type's op, logged. */
const cmds = [];
function routeCmds(id, t) {
	NS.bus.subscribe('cmd:' + id, function (cmd) {
		if (!cmd) return;
		cmds.push(cmd);
		TB.applyCmd(t.table, t.ctx, cmd);
	});
}

function buttons(bar) { return bar ? Array.from(bar.querySelectorAll('.juneau-view-ribbon-btn')) : []; }
function btn(bar, label) { return buttons(bar).find(function (b) { return b.getAttribute('aria-label') === label; }) || null; }
function pressed(bar, label) { const b = btn(bar, label); return b ? b.getAttribute('aria-pressed') : 'missing'; }
async function click(bar, label) { const b = btn(bar, label); if (b) b.dispatch('click', {}); await tick(); return !!b; }

(async function () {
	const out = { hasTableBus: !!TB, hasPublishState: typeof TB?.publishState === 'function' };
	if (!TB) { process.stdout.write(J(out)); return; }

	// ---- A) an optionGroup's default is the explicit `default` only: no `default`, no auto-selection ----
	const NOPE = { type: 'optionGroup', id: 'age', persist: true, options: AGE.options };
	const d = (id, g) => ({ id: id, ribbon: [g] });
	store.setItem('juneau.view.d2.ribbon.age', 'all');
	out.defaultNothingStored = J(NS.ribbon.loadPersistedState(d('d1', AGE)));
	out.defaultStoredMember = J(NS.ribbon.loadPersistedState(d('d2', AGE)));
	out.noDefaultNothingStored = J(NS.ribbon.loadPersistedState(d('d3', NOPE)));

	// ---- B) the main console table: default state reaches the ribbon and the first filter: payload ----
	NS.bus.declare('app.region-picked', { retain: true, publisher: 'ribbon' });
	const card = { id: 'changes', type: 'datatables' };
	const main = makeTable(viewDef('changes', 'client', [
		{ type: 'publish', title: 'East', topic: 'app.region-picked', payload: { region: 'east' } }
	]), card);
	routeCmds('changes', main);
	const filters = [];
	NS.bus.subscribe('filter:changes', function (f) { if (f) filters.push(f); });
	const bar = NS.ribbon.build(main.ctx.viewDef, main.ctx);
	TB.bindTable(main.table, main.ctx);
	await tick();
	out.initial = {
		activeState: J(main.ctx.activeState),
		open: pressed(bar, 'Open only'), new: pressed(bar, 'New'), all: pressed(bar, 'All'),
		filterAge: last(filters)?.options.age
	};

	// ---- C) option / optionGroup / refresh / pause / collapse, all as cmd:changes ----
	const refreshes = main.ctx.rowFilterRefreshes, draws = main.dt.log.draws.length;
	await click(bar, 'Open only');
	out.open = {
		cmd: J(last(cmds)), rowFilterRefreshes: main.ctx.rowFilterRefreshes - refreshes,
		draws: main.dt.log.draws.length - draws, reloads: main.dt.log.reloads.length, pressed: pressed(bar, 'Open only'),
		filterOpen: last(filters)?.options.openOnly
	};

	await click(bar, 'All');
	out.all = {
		cmd: J(last(cmds)), new: pressed(bar, 'New'), all: pressed(bar, 'All'),
		stored: store.getItem(NS.ribbon.ribbonStorageKey('changes', 'age'))
	};

	main.dt.log.reloads.length = 0;
	await click(bar, 'Refresh');
	out.refresh = { cmd: J(last(cmds)), reloads: J(main.dt.log.reloads) };

	await click(bar, 'Pause auto-refresh');
	out.pause = { cmd: last(cmds)?.op, paused: main.ctx._pollPaused, pressed: pressed(bar, 'Pause auto-refresh') };
	await click(bar, 'Pause auto-refresh');
	out.resume = { cmd: last(cmds)?.op, paused: main.ctx._pollPaused, pressed: pressed(bar, 'Pause auto-refresh') };

	await click(bar, 'Collapse all');
	out.collapse = { cmd: last(cmds)?.op, collapses: main.ctx.collapses };

	// ---- D) the `publish` item, then a stand-in for the shell's {as:'filter', map:{'columns.region':'region'}} ----
	const picked = [];
	NS.bus.subscribe('app.region-picked', function (p) {
		if (!p) return;
		picked.push(p);
		TB.filterRole(main.table, main.ctx, { 'columns.region': p.region });
	});
	await click(bar, 'East');
	out.publish = { payload: J(last(picked)), filterRegion: last(filters)?.columns.region || null };

	// ---- E) a second ribbon on another card, targeting `changes`: both stay in sync ----
	const side = { id: 'side', type: 'datatables' };
	const sideCtx = {
		table: null, viewDef: { id: 'side', ribbon: [] }, activeState: {},
		bus: TB.cardBus(side, NS.bus.owner('side')), redraw: function () { /* no-op */ }
	};
	sideCtx.viewDef.ribbon = [
		{ type: 'option', id: 'openOnly', title: 'Open (side)', target: 'changes' },
		{ type: 'refresh', title: 'Refresh changes', target: 'changes' }
	];
	const sideBar = NS.ribbon.build(sideCtx.viewDef, sideCtx);
	await tick();
	out.sideInitial = pressed(sideBar, 'Open (side)');
	await click(sideBar, 'Open (side)');
	out.side = {
		cmd: J(last(cmds)), mainOpenOnly: main.ctx.activeState.openOnly,
		mainPressed: pressed(bar, 'Open only'), sidePressed: pressed(sideBar, 'Open (side)'),
		sideActiveState: J(sideCtx.activeState)
	};
	main.dt.log.reloads.length = 0;
	await click(sideBar, 'Refresh changes');
	out.sideRefresh = J(main.dt.log.reloads);

	// ---- F) a rebuild disposes the previous ribbon's filter: listeners ----
	const bar2 = NS.ribbon.build(main.ctx.viewDef, main.ctx);
	NS.bus.publish('cmd:changes', { schemaVersion: 1, op: 'set-filter', options: { openOnly: true } });
	await tick();
	out.rebuild = { oldBar: pressed(bar, 'Open only'), newBar: pressed(bar2, 'Open only') };

	// ---- G) publishState re-publishes filter: and redraw: for a live table whose options changed without a rebuild ----
	const redraws = [];
	NS.bus.subscribe('redraw:changes', function (r) { if (r) redraws.push(r); });
	let localFilterCalls = 0;
	TB.onFilter(main.ctx, function () { localFilterCalls++; });
	await tick();
	const fBefore = filters.length, rBefore = redraws.length;
	main.ctx.activeState.openOnly = !main.ctx.activeState.openOnly;   // what a restore does to the live grid
	if (out.hasPublishState) TB.publishState(main.table);
	await tick();
	out.publishState = {
		filters: filters.length - fBefore, redraws: redraws.length - rBefore,
		openOnly: last(filters)?.options.openOnly, pressed: pressed(bar2, 'Open only')
	};
	// ... and re-emits to local listeners even when nothing changed (the dedupe is reset).
	const lBefore = localFilterCalls;
	if (out.hasPublishState) TB.publishState(main.table);
	out.publishStateUnchanged = { localFilterCalls: localFilterCalls - lBefore };

	// ---- H) a plain page: own target runs the op directly; a cross-card target and `publish` fail loudly ----
	const errsBefore = errors.length;
	const plain = makeTable(viewDef('plain', 'client', [
		{ type: 'option', id: 'remote', title: 'Remote', target: 'other' },
		{ type: 'publish', title: 'Nowhere', topic: 'app.region-picked' }
	]), null);
	const plainBar = NS.ribbon.build(plain.ctx.viewDef, plain.ctx);
	TB.bindTable(plain.table, plain.ctx);
	await click(plainBar, 'Open only');
	await click(plainBar, 'Remote');
	out.plain = {
		bus: plain.ctx.bus, rowFilterRefreshes: plain.ctx.rowFilterRefreshes, activeState: J(plain.ctx.activeState),
		pressed: pressed(plainBar, 'Open only'), publishRendered: !!btn(plainBar, 'Nowhere'),
		errors: errors.slice(errsBefore)
	};

	// ---- I) a page that loaded juneau-ribbon.js alone has no tableBus: the ribbon applies its own command ----
	const solo = { redraws: 0, activeState: {}, bus: null, table: null, redraw: function () { solo.redraws++; } };
	const soloDef = { id: 'solo', ribbon: [{ type: 'option', id: 'mine', title: 'Mine', persist: true }, { type: 'refresh' }] };
	const savedTB = NS.tableBus;
	NS.tableBus = undefined;
	const soloBar = NS.ribbon.build(soloDef, solo);
	await click(soloBar, 'Mine');
	await click(soloBar, 'Refresh');
	NS.tableBus = savedTB;
	out.solo = {
		pressed: pressed(soloBar, 'Mine'), activeState: J(solo.activeState), redraws: solo.redraws,
		stored: store.getItem(NS.ribbon.ribbonStorageKey('solo', 'mine'))
	};

	// ---- J) D-C5-5 through the new path: SERVER mode, column-scoped option, the real request derivation ----
	let ajaxCfg = null;
	env.window.JuneauDataTables = { ajax: function (url, cfg) { ajaxCfg = cfg; return cfg; } };
	const srvCard = { id: 'srv', type: 'datatables' };
	const srv = makeTable(viewDef('srv', 'server'), srvCard);
	routeCmds('srv', srv);
	NS.init.buildOptions(srv.ctx.viewDef, { ribbonActiveState: function () { return srv.ctx.activeState; } });
	const request = function () {
		const d0 = ajaxCfg.data({ columns: [] });
		return J(Object.keys(d0.columns).filter(function (i) { return d0.columns[i]?.search?.value; })
			.map(function (i) { return [Number(i), d0.columns[i].search.value]; }));
	};
	const srvBar = NS.ribbon.build(srv.ctx.viewDef, srv.ctx);
	TB.bindTable(srv.table, srv.ctx);
	await tick();
	out.srvFirstRequest = request();
	srv.dt.log.reloads.length = 0;
	await click(srvBar, 'Open only');
	out.srv = {
		cmd: J(last(cmds)), reloads: srv.dt.log.reloads.length, columnSearches: J(srv.dt.log.columnSearch),
		request: request()
	};

	// ---- K) a deselected optionGroup persists "" and a reload keeps it deselected, even over a `default` ----
	const kKey = NS.ribbon.ribbonStorageKey('srv', 'age');
	NS.bus.publish('cmd:srv', { schemaVersion: 1, op: 'set-filter', options: { age: 'all' } });
	await tick();
	const kSelected = store.getItem(kKey);
	NS.bus.publish('cmd:srv', { schemaVersion: 1, op: 'set-filter', options: { age: null } });
	await tick();
	out.deselect = {
		selected: kSelected, stored: store.getItem(kKey), live: J(srv.ctx.activeState.age ?? null),
		reloaded: J(NS.ribbon.loadPersistedState(srv.ctx.viewDef).age === undefined ? 'unset' : NS.ribbon.loadPersistedState(srv.ctx.viewDef).age),
		reloadedHasKey: 'age' in NS.ribbon.loadPersistedState(srv.ctx.viewDef)
	};
	store.setItem('juneau.view.k2.ribbon.age', '');
	const kState = NS.ribbon.loadPersistedState(d('k2', AGE));
	out.storedEmpty = { hasKey: 'age' in kState, value: J(kState.age ?? null) };

	// ---- L) a stored value that names no group member is not a persisted choice: it falls through to `default` ----
	store.setItem('juneau.view.l1.ribbon.age', 'null');
	out.storedNullString = J(NS.ribbon.loadPersistedState(d('l1', AGE)));
	store.setItem('juneau.view.l2.ribbon.age', 'gone');
	out.storedStaleMember = J(NS.ribbon.loadPersistedState(d('l2', AGE)));
	store.setItem('juneau.view.l3.ribbon.age', 'null');
	out.storedNullNoDefault = J(NS.ribbon.loadPersistedState(d('l3', NOPE)));

	out.errors = errors;
	process.stdout.write(J(out));
})().catch(function (e) {
	process.stdout.write(J({ crash: String(e && e.stack || e) }));
});
