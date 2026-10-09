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

/*
 * table-bus.cjs - Node harness for juneau-views.js's NS.tableBus (message bus addendum, spec 4.2-4.5, 5.3, 5.4).
 *
 *   Usage:  node table-bus.cjs <juneau-bus.js> <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js> <juneau-search.js>
 *
 * Loads the five production scripts into one shim sandbox and drives NS.tableBus against a hand-built table ctx and
 * a fake DataTables API (no jQuery/DataTables is bundled by this module).  Prints ONE JSON object to stdout; every
 * assertion lives in ViewsJs_TableBus_Test / ViewsJs_TableBusClientFilter_Test.  Structured values are emitted as
 * JSON strings so the Java side compares them byte-for-byte.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require('./views-dom-shim.cjs');

const scripts = process.argv.slice(2);
if (scripts.length !== 5) {
	console.error('usage: node table-bus.cjs <juneau-bus.js> <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js> <juneau-search.js>');
	process.exit(2);
}

const errors = [];
const con = Object.assign(Object.create(console), {
	error: function (...a) { errors.push(a.join(' ')); },
	warn: function (...a) { errors.push('WARN ' + a.join(' ')); }
});
const env = makeEnv();
env.window.console = con;
// The fetch the sandboxed scripts see; a case swaps `fetchImpl` for the duration of the request it drives.
let fetchImpl = function () { return Promise.reject(new Error('no network in table-bus.cjs')); };
const sandbox = {
	window: env.window, document: env.document, console: con,
	setTimeout: setTimeout, clearTimeout: clearTimeout,
	setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ },
	queueMicrotask: queueMicrotask, Promise: Promise,
	fetch: function (...a) { return fetchImpl(...a); }
};
scripts.forEach(function (p) {
	// NOSONAR javascript:S1523 -- loading the production sources into a VM sandbox is this harness's mechanism.
	vm.runInNewContext(fs.readFileSync(path.resolve(p), 'utf8'), sandbox, { filename: path.basename(p) }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled scripts
});
const NS = env.window.JuneauViews;
const TB = NS.tableBus;
const tick = () => new Promise(r => setImmediate(r));
const J = v => JSON.stringify(v);
const last = a => a[a.length - 1];

// The live DataTables column array: a leading selection column, so every catalog index is shifted by one.
const OPTS_COLUMNS = [
	{ data: null, className: 'juneau-view-select-cell' },
	{ data: 'id' }, { data: 'owner' }, { data: 'state' }, { data: 'isNew' }, { data: 'region' }, { data: 'stage' }
];
const STAGE_INDEX = 6;

function viewDef(mode) {
	return {
		id: 'changes', dataMode: mode, dataUrl: '/api/changes',
		// `stage` carries search metadata: on a client-filtered table its expression goes through the DSL engine
		// (search.fixed), never col.search().
		columns: [{ data: 'id' }, { data: 'owner' }, { data: 'state' }, { data: 'isNew' }, { data: 'region' },
			{ data: 'stage', search: { type: 'enum', operators: [{ name: '$eq' }, { name: '$ne' }] } }],
		ribbon: [
			{ type: 'refresh' },
			{ type: 'option', id: 'mine', title: 'Mine only', param: 'mine', value: 'true' },
			{ type: 'option', id: 'openOnly', title: 'Open only', column: 'state', value: 'open' },
			{ type: 'optionGroup', id: 'age', options: [
				{ id: 'new', title: 'New', column: 'isNew', value: '^yes$' },
				{ id: 'all', title: 'All' }
			] }
		]
	};
}

// A fake DataTables API: records every column-search/draw/reload call; `fire` simulates draw.dt.  Like the real API
// it always has `ajax` (an inline-rows table has an ajax namespace too, just no source); the table is inline when
// its node carries `__juneauRows`, which `table().node()` hands back.  A column has `index()` and `search.fixed`, so
// a client-filtered column with search metadata takes the DSL path; a row has the child-row API the detail panel uses.
function fakeDt(rows, info, tableEl) {
	const handlers = {};
	const colSearch = {};
	const cols = {};
	const fixed = {};
	const rowStates = new Map();
	const log = { columnSearch: [], draws: [], reloads: [], search: [], fixed: [] };
	let global = '';
	const dt = {
		log: log,
		fixed: fixed,
		on: function (evt, fn) { (handlers[evt] = handlers[evt] || []).push(fn); return dt; },
		fire: function (evt, e) { (handlers[evt] || []).forEach(function (fn) { fn(e); }); },
		search: function (v) {
			if (arguments.length === 0) return global;
			global = v; log.search.push(v); return dt;
		},
		column: function (i) {
			if (cols[i]) return cols[i];
			const col = {
				index: function () { return i; },
				draw: function () { log.draws.push('column'); return dt; }
			};
			col.search = function (v, regex, smart) {
				if (arguments.length === 0) return colSearch[i] || '';
				colSearch[i] = v;
				log.columnSearch.push({ index: i, value: v, regex: !!regex, smart: smart !== false });
				return col;
			};
			col.search.fixed = function (name, fn) {
				fixed[i] = fn;
				log.fixed.push({ index: i, name: name, installed: fn != null });
				return col;
			};
			cols[i] = col;
			return col;
		},
		rows: function () { return { data: function () { return { toArray: function () { return rows.slice(); } }; } }; },
		row: function (tr) {
			if (!rowStates.has(tr)) rowStates.set(tr, { shown: false });
			const st = rowStates.get(tr);
			const child = function () { return { show: function () { st.shown = true; } }; };
			child.isShown = function () { return st.shown; };
			child.hide = function () { st.shown = false; };
			return { length: 1, data: function () { return null; }, child: child };
		},
		table: function () { return { node: function () { return tableEl || null; } }; },
		page: { info: function () { return info; } },
		draw: function (paging) { log.draws.push(paging === undefined ? null : paging); return dt; },
		ajax: { reload: function (cb, reset) { log.reloads.push(reset === undefined ? null : reset); } }
	};
	return dt;
}

const ROWS = [
	{ id: 'c-17', owner: 'jb', state: 'open' },
	{ id: 'c-21', owner: 'ak', state: 'closed' },
	{ id: 'c-30', owner: 'jb', state: 'open' }
];

// Builds a view table inside a card host.  `card` null = a plain (non-console) page: no host bus.  The ctx is shaped
// like initTableFromDef's + constructTable's: a CLIENT table has the ribbon row filter hook, a SERVER table does not.
function makeTable(mode, card, cardCtx, inline) {
	const host = env.el('div');
	const table = env.el('table');
	table.setAttribute('data-juneau-view', 'changes');
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	env.body.appendChild(host);
	if (card) host.__juneauBus = TB.cardBus(card, cardCtx);
	if (inline) table.__juneauRows = [];
	const dt = fakeDt(ROWS, { page: 0, length: 25, pages: 1, recordsDisplay: 3 }, table);
	const ctx = {
		table: table, viewDef: viewDef(mode), dataTable: dt, activeState: {},
		clientFiltered: mode !== 'server',
		selectionState: { selected: new Set(), rowIdField: 'id', snapshots: {} },
		optsColumns: OPTS_COLUMNS,
		bulkToolbar: { sizes: [], refresh: function (n) { this.sizes.push(n); } },
		collapses: 0, rowFilterRefreshes: 0
	};
	if (mode !== 'server') ctx.refreshRibbonRowFilter = function () { ctx.rowFilterRefreshes++; };
	ctx.redraw = function () {
		if (ctx.refreshRibbonRowFilter) ctx.refreshRibbonRowFilter();
		if (inline) dt.draw(); else dt.ajax.reload();
	};
	ctx.collapseAllDetailRows = function () { ctx.collapses++; };
	ctx.bus = TB.findBus(table);
	table.__juneauCtx = ctx;
	return { host: host, table: table, tbody: tbody, ctx: ctx, dt: dt };
}

// A card-owned bus, as the shell builds it: owner facade + claimed implicit topics.
function cardOwner(card) {
	const own = NS.bus.owner(card.id);
	TB.implicitTopics(card).forEach(function (t) { own.claim(t); });
	return own;
}

function recorder(cardId) {
	const seen = {};
	['selection', 'filter', 'redraw', 'detail', 'bulk'].forEach(function (f) {
		seen[f] = [];
		NS.bus.subscribe(f + ':' + cardId, function (p) { if (p) seen[f].push(p); });
	});
	return seen;
}

(async function () {
	const out = { hasTableBus: !!TB };
	if (!TB) { process.stdout.write(J(out)); return; }

	// ---- A) pure layer ----
	out.ops = J(TB.OPS);
	out.selPure = J(TB.selectionPayload('v', ['a', 'b'], ['b', 'c'], function (id) { return { id: id }; }, 0));
	const capped = TB.selectionPayload('v', [], ['a', 'b', 'c'], function (id) { return { id: id }; }, 2);
	out.selCap = { ids: capped.ids.length, rows: capped.rows.length, rowsTruncated: capped.rowsTruncated === true };
	out.selDefaultCap = TB.SELECTION_ROWS_CAP;
	out.selEmpty = J(TB.selectionPayload('v', ['a'], [], function (id) { return { id: id }; }, 0));
	out.filterPure = J(TB.filterPayload('v', 'ssc', { state: 'open', owner: 'jb', region: '', x: null }, { mine: true }));
	out.redrawPure = J(TB.redrawPayload('v', { page: 1, length: 25, pages: 17, recordsDisplay: 412 }, false));
	out.detailPure = J(TB.detailPayload('v', 'c-17', true, 3));
	out.bulkPure = J(TB.bulkPayload('v', 'close', ['c-17'], [], [{ id: 'c-21', status: 409 }]));
	out.implicitFull = J(TB.implicitTopics({ id: 'changes', table: { dataUrl: '/x', selection: { mode: 'single' }, detail: {}, bulk: {} } }));
	out.implicitCatalog = J(TB.implicitTopics({ id: 't', table: { dataUrl: '/x' } }));
	out.implicitFlat = J(TB.implicitTopics({ id: 'f', dataUrl: '/x', bulk: {} }));
	out.implicitSrcOnly = J(TB.implicitTopics({ id: 's', src: '/api/s' }));
	out.optionStateEmpty = J(TB.optionState(viewDef('client'), {}));

	// ---- B) CLIENT mode with a bus ----
	const card = { id: 'changes', type: 'datatables', table: { dataUrl: '/x', selection: { mode: 'multi' }, detail: {}, bulk: {} } };
	const own = cardOwner(card);
	const seen = recorder('changes');
	const c = makeTable('client', card, own);
	out.ctxBusCardId = c.ctx.bus ? c.ctx.bus.cardId : null;
	// The bus drops an unchanged retained value on its own, so the changed-only check is counted at a local listener.
	let filterCalls = 0;
	TB.onFilter(c.ctx, function () { filterCalls++; });
	TB.bindTable(c.table, c.ctx);
	await tick();
	out.bindSearches = J(c.dt.log.columnSearch);
	out.bindFilter = J(seen.filter[0] || null);
	out.bindSelection = J(seen.selection[0] || null);
	out.bindRedraws = seen.redraw.length;

	c.dt.log.columnSearch.length = 0;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', options: { age: 'new' } });
	await tick();
	out.clientNew = {
		searches: J(c.dt.log.columnSearch), draws: c.dt.log.draws.length, reloads: c.dt.log.reloads.length,
		rowFilterRefreshes: c.ctx.rowFilterRefreshes,
		activeAge: c.ctx.activeState.age, filter: J(last(seen.filter))
	};

	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', options: { age: 'all' } });
	await tick();
	out.clientAll = { rowFilterRefreshes: c.ctx.rowFilterRefreshes, filterAge: last(seen.filter).options.age };

	c.dt.log.columnSearch.length = 0;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', search: 'ssc', columns: { owner: 'jb' } });
	await tick();
	out.clientColumns = { searches: J(c.dt.log.columnSearch), globalSearch: J(c.dt.log.search), filter: J(last(seen.filter)) };

	// A column an option also filters is an ordinary column: the option's row filter is a separate predicate.
	c.dt.log.columnSearch.length = 0;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', columns: { state: 'x' } });
	await tick();
	out.optionColumn = { searches: J(c.dt.log.columnSearch), columns: J(last(seen.filter).columns) };
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', columns: { state: '' } });

	const errsBefore = errors.length;
	c.dt.log.columnSearch.length = 0;
	const callsMid = filterCalls;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'set-filter', columns: { nope: 'x' }, options: { nope: true, age: 'nope' } });
	await tick();
	out.refused = { searches: J(c.dt.log.columnSearch), errors: errors.slice(errsBefore), ageStill: c.ctx.activeState.age, newFilters: filterCalls - callsMid };

	const callsBefore = filterCalls;
	const redrawsBefore = seen.redraw.length;
	c.dt.fire('draw.dt', { target: c.table });
	c.dt.fire('draw.dt', { target: env.el('table') });   // a nested table's draw bubbling up
	await tick();
	out.draw = {
		newFilters: filterCalls - callsBefore, newRedraws: seen.redraw.length - redrawsBefore,
		redraw: J(last(seen.redraw))
	};

	// ---- C) ops ----
	c.dt.log.reloads.length = 0;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'reload' });
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'reload', resetPaging: true });
	out.reloads = J(c.dt.log.reloads);

	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'select', ids: ['c-21', 'zzz'] });
	await tick();
	out.select = { selected: J(Array.from(c.ctx.selectionState.selected)), payload: J(last(seen.selection)), toolbar: last(c.ctx.bulkToolbar.sizes) };
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'clear-selection' });
	await tick();
	out.clear = { selected: c.ctx.selectionState.selected.size, payload: J(last(seen.selection)) };

	let pauseNotes = 0;
	c.ctx._onPollPausedChange = function () { pauseNotes++; };
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'pause-polling' });
	out.paused = c.ctx._pollPaused;
	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'resume-polling' });
	out.resumed = c.ctx._pollPaused;
	out.pauseNotes = pauseNotes;

	TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'collapse-all' });
	out.collapses = c.ctx.collapses;

	try {
		TB.applyCmd(c.table, c.ctx, { schemaVersion: 1, op: 'explode' });
		out.unknownOp = null;
	} catch (e) {
		out.unknownOp = { code: e.code, message: String(e.message) };
	}

	// ---- D) the selection hook in initSelection's change listener ----
	const tr = env.el('tr');
	tr.setAttribute(NS.init.ROW_ID_ATTR, 'c-30');
	const td = env.el('td');
	const cb = env.el('input');
	cb.type = 'checkbox';
	cb.className = 'juneau-view-select-checkbox';
	td.appendChild(cb);
	tr.appendChild(td);
	c.tbody.appendChild(tr);
	NS.init.initSelection(c.table, c.ctx);
	cb.checked = true;
	c.table.dispatch('change', { target: cb });
	await tick();
	out.checkbox = J(last(seen.selection));

	// ---- E) detail / bulk publishers, gated on the card's implicit topics ----
	TB.publishDetail(c.ctx, 'c-17', true, 3);
	TB.publishBulk(c.ctx, 'close', ['c-17'], [], [{ id: 'c-21', status: 409 }]);
	await tick();
	out.detail = J(last(seen.detail));
	out.bulk = J(last(seen.bulk));

	const bare = { id: 'bare', type: 'datatables', table: { dataUrl: '/x' } };
	const bareSeen = recorder('bare');
	const b = makeTable('client', bare, cardOwner(bare));
	TB.bindTable(b.table, b.ctx);
	TB.publishDetail(b.ctx, 'c-17', true, 1);
	TB.publishBulk(b.ctx, 'close', [], [], []);
	TB.applyCmd(b.table, b.ctx, { schemaVersion: 1, op: 'select', ids: ['c-17'] });
	await tick();
	out.bare = { detail: bareSeen.detail.length, bulk: bareSeen.bulk.length, selection: bareSeen.selection.length, filter: bareSeen.filter.length, redraw: bareSeen.redraw.length };

	// ---- F) the filter role, mapped and cleared ----
	c.dt.log.columnSearch.length = 0;
	TB.filterRole(c.table, c.ctx, { 'columns.region': 'east', 'options.age': 'new', search: 'zz' });
	await tick();
	out.role = { searches: J(c.dt.log.columnSearch), region: last(seen.filter).columns.region, age: last(seen.filter).options.age, search: last(seen.filter).search };
	const roleFilters = seen.filter.length;
	TB.filterRole(c.table, c.ctx, null);
	await tick();
	out.roleCleared = seen.filter.length - roleFilters;

	// ---- G) SERVER mode: options reach the request, never a column search or a row filter ----
	const srv = { id: 'srv', type: 'datatables', table: { dataUrl: '/x' } };
	const srvSeen = recorder('srv');
	const s = makeTable('server', srv, cardOwner(srv));
	TB.bindTable(s.table, s.ctx);
	s.dt.log.columnSearch.length = 0;
	TB.applyCmd(s.table, s.ctx, { schemaVersion: 1, op: 'set-filter', options: { openOnly: true, mine: true } });
	await tick();
	out.server = {
		searches: J(s.dt.log.columnSearch), reloads: s.dt.log.reloads.length, draws: s.dt.log.draws.length,
		params: J(NS.ribbon.ribbonQueryParams(s.ctx.viewDef, s.ctx.activeState)),
		columnSearches: J(NS.ribbon.ribbonColumnSearches(s.ctx.viewDef, s.ctx.activeState, s.ctx.optsColumns)),
		filterOpenOnly: last(srvSeen.filter).options.openOnly
	};

	// ---- H) a plain page (no console card): ops still work, listeners still fire ----
	const p = makeTable('client', null, null);
	out.plainBus = p.ctx.bus;
	const got = [];
	TB.onFilter(p.ctx, function (f) { got.push(f); });
	TB.bindTable(p.table, p.ctx);
	TB.applyCmd(p.table, p.ctx, { schemaVersion: 1, op: 'set-filter', options: { age: 'new' } });
	out.plain = { rowFilterRefreshes: p.ctx.rowFilterRefreshes, lastAge: got.length ? last(got).options.age : null, draws: p.dt.log.draws.length, seen: got.length };

	// ---- I) a nested table never resolves its parent card's bus ----
	const nested = env.el('table');
	nested.setAttribute('data-juneau-view', 'inner');
	const ntd = env.el('td');
	ntd.appendChild(nested);
	c.tbody.appendChild(ntd);
	out.nestedBus = TB.findBus(nested);

	// ---- J) a rebuild (buildTable -> constructTable -> bindTable) re-publishes the unchanged filter ----
	// The bus itself drops an unchanged retained value, so the changed-only check is observed through a local listener.
	let rebuildFilters = 0;
	TB.onFilter(c.ctx, function () { rebuildFilters++; });
	const fresh = fakeDt(ROWS, { page: 0, length: 25, pages: 1, recordsDisplay: 3 }, c.table);
	fresh.search(c.dt.search());
	OPTS_COLUMNS.forEach(function (col, idx) { fresh.column(idx).search(c.dt.column(idx).search()); });
	c.ctx.dataTable = fresh;
	TB.bindTable(c.table, c.ctx);
	await tick();
	out.rebuild = { newFilters: rebuildFilters };

	// ---- K) an inline-rows table still has an ajax namespace, but its node carries __juneauRows: its first draw predates bindTable ----
	const inl = { id: 'inl', type: 'datatables', table: { rows: [] } };
	const inlSeen = recorder('inl');
	const i = makeTable('client', inl, cardOwner(inl), true);
	TB.bindTable(i.table, i.ctx);
	await tick();
	out.inline = { redraws: inlSeen.redraw.length };

	// ---- L) the card handler: registered ops/roles/implicit, the host stamp, the unmounted-op refusal ----
	const reg = (env.window.JuneauConsoleCards || []).filter(function (e) { return e[0] === 'datatables'; });
	out.registrations = reg.length;
	const h = reg.length ? reg[0][1] : {};
	out.handler = {
		ops: J(Object.keys(h.ops || {})), roles: J(Object.keys(h.roles || {})),
		implicitIsTableBus: h.implicit === TB.implicitTopics, filterRoleIsTableBus: (h.roles || {}).filter === TB.cardFilterRole
	};
	const realMount = NS.card.mountDatatables;
	NS.card.mountDatatables = function () { return 'mounted'; };
	const rcard = { id: 'rc', type: 'datatables', table: { dataUrl: '/x' } };
	const rel = env.el('div');
	const rend = h.render(rcard, rel, { publish: function () {}, subscribe: function () {} });
	out.render = { result: rend, stamped: rel.__juneauBus ? rel.__juneauBus.cardId : null };
	h.destroy(rcard, rel);
	out.render.afterDestroy = rel.__juneauBus === undefined ? 'cleared' : 'kept';
	NS.card.mountDatatables = realMount;
	c.host.querySelector('table[data-juneau-view]');
	TB.cardOps().reload({ op: 'reload' }, card, c.host);
	out.cardOpRan = c.dt === c.ctx.dataTable ? 'stale' : J(c.ctx.dataTable.log.reloads);
	try {
		TB.cardOps().reload({ op: 'reload' }, { id: 'x' }, env.el('div'));
		out.unmounted = null;
	} catch (e) {
		out.unmounted = { code: e.code, message: String(e.message) };
	}
	TB.cardFilterRole({ 'options.age': 'all' }, {}, card, c.host);
	out.cardRole = c.ctx.activeState.age;

	// ---- M) client-filtered DSL columns: filter:<id>.columns and set-filter go through get/setColumnExpr ----
	const mcard = { id: 'dsl', type: 'datatables', table: { dataUrl: '/x' } };
	const mSeen = recorder('dsl');
	const m = makeTable('client', mcard, cardOwner(mcard));
	let mFilters = 0;
	TB.onFilter(m.ctx, function () { mFilters++; });
	TB.bindTable(m.table, m.ctx);
	const mErrBase = errors.length;
	m.dt.log.columnSearch.length = 0;
	m.dt.log.draws.length = 0;
	TB.applyCmd(m.table, m.ctx, { schemaVersion: 1, op: 'set-filter', columns: { stage: '$eq(open)' } });
	await tick();
	const stageFn = m.dt.fixed[STAGE_INDEX];
	out.dslSet = {
		columnSearches: J(m.dt.log.columnSearch), fixed: J(m.dt.log.fixed), store: m.ctx._colExprs?.stage ?? null,
		draws: J(m.dt.log.draws), filterColumns: J(last(mSeen.filter).columns),
		matches: stageFn ? J([stageFn('open', { stage: 'open' }), stageFn('closed', { stage: 'closed' })]) : null
	};

	const fixedBefore = m.dt.log.fixed.length;
	const filtersBefore = mFilters;
	TB.applyCmd(m.table, m.ctx, { schemaVersion: 1, op: 'set-filter', columns: { stage: '$bogus(x)' } });
	await tick();
	out.dslBad = {
		errors: errors.slice(mErrBase), store: m.ctx._colExprs?.stage ?? null,
		newFixed: m.dt.log.fixed.length - fixedBefore, newFilters: mFilters - filtersBefore,
		filterColumns: J(last(mSeen.filter).columns)
	};

	// Read direction: a user-typed DSL expression lives in the store, so the filter payload must read it from there.
	m.ctx._colExprs = m.ctx._colExprs || {};
	m.ctx._colExprs.stage = '$eq(closed)';
	TB.emitFilter(m.ctx);
	await tick();
	out.dslRead = { filterColumns: J(last(mSeen.filter).columns), columnSearches: J(m.dt.log.columnSearch) };

	TB.applyCmd(m.table, m.ctx, { schemaVersion: 1, op: 'set-filter', columns: { stage: '' } });
	await tick();
	out.dslClear = {
		fixed: J(last(m.dt.log.fixed)), store: m.ctx._colExprs?.stage ?? null, filterColumns: J(last(mSeen.filter).columns)
	};

	// SERVER mode never takes the DSL path: the expression is a plain column search sent to the server.
	s.dt.log.columnSearch.length = 0;
	TB.applyCmd(s.table, s.ctx, { schemaVersion: 1, op: 'set-filter', columns: { stage: '$eq(open)' } });
	out.dslServer = { searches: J(s.dt.log.columnSearch), fixed: J(s.dt.log.fixed) };
	out.dslErrors = errors.splice(mErrBase);

	// ---- N) the filter role: foreign extras are skipped quietly; a key the source dropped is cleared ----
	const nErrBase = errors.length;
	m.dt.log.columnSearch.length = 0;
	TB.filterRole(m.table, m.ctx, { 'columns.nope': 'x', 'options.nope': true, 'options.ghost': false, 'columns.owner': 'jb' });
	await tick();
	out.roleForeign = { errors: errors.slice(nErrBase), searches: J(m.dt.log.columnSearch) };

	m.dt.column(5).search('west');   // the target's own filter, set by its user
	m.dt.log.columnSearch.length = 0;
	TB.filterRole(m.table, m.ctx, { 'columns.owner': 'jb', 'options.openOnly': true, 'options.mine': true });
	await tick();
	const afterApply = last(mSeen.filter);
	TB.filterRole(m.table, m.ctx, { 'options.mine': true });
	await tick();
	const afterDrop = last(mSeen.filter);
	out.roleClearing = {
		appliedColumns: J(afterApply.columns), appliedOptions: J(afterApply.options),
		columns: J(afterDrop.columns), options: J(afterDrop.options),
		searches: J(m.dt.log.columnSearch)
	};
	out.roleErrors = errors.splice(nErrBase);

	// ---- O) the real hooks: detail expand/collapse, collapse-all, safe collapse, selection prune, bulk toolbar, bulk run ----
	const oErrBase = errors.length;
	const ocard = { id: 'drv', type: 'datatables', table: { dataUrl: '/x', selection: { mode: 'multi' }, detail: {}, bulk: {} } };
	const oOwn = cardOwner(ocard);
	const oTrace = [];   // every bus publish with the number of summary toasts already on the page at that moment
	const toastCount = function () { return env.body.querySelectorAll('.juneau-view-bulk-toast').length; };
	const oCardCtx = {
		publish: function (topic, payload) { oTrace.push({ topic: topic, toasts: toastCount() }); return oOwn.publish(topic, payload); },
		subscribe: function (topic, fn, opts) { return oOwn.subscribe(topic, fn, opts); }
	};
	const oSeen = recorder('drv');
	const o = makeTable('client', ocard, oCardCtx);
	// The test DOM shim has no descendant combinators; translate the two table-scoped selectors the hooks use.
	const realQsa = o.table.querySelectorAll;
	o.table.querySelectorAll = function (sel) {
		const mm = /^tbody (?:> )?tr(.*)$/.exec(sel);
		return realQsa.call(this, mm ? 'tr' + mm[1] : sel);
	};
	o.table.setAttribute('data-juneau-csrf', 'tok');
	const tpl = env.el('template');
	tpl.setAttribute('data-juneau-row-detail', '');
	tpl.dataset.juneauDetailUrl = '/api/changes/{id}/detail';
	tpl.content.cloneNode = function () { return env.el('div'); };   // the shim's fragment has no cloneNode
	o.host.appendChild(tpl);
	const oRow = function (id) {
		const tr = env.el('tr');
		tr.className = 'juneau-view-detail-row';
		tr.setAttribute(NS.init.ROW_ID_ATTR, id);
		const ctl = env.el('td');
		ctl.className = 'juneau-view-detail-control';
		tr.appendChild(ctl);
		const sel = env.el('td');
		const cb = env.el('input');
		cb.type = 'checkbox';
		cb.className = 'juneau-view-select-checkbox';
		sel.appendChild(cb);
		tr.appendChild(sel);
		o.tbody.appendChild(tr);
		return { tr: tr, ctl: ctl, cb: cb };
	};
	const r17 = oRow('c-17');
	const r21 = oRow('c-21');
	oRow('c-30');
	fetchImpl = function () { return new Promise(function () { /* the detail GET never settles; only the publishes matter */ }); };
	NS.init.initDetailsExpander(o.table, o.ctx, o.ctx.viewDef);
	o.ctx.collapseAllDetailRows = function () { NS.init.collapseAllDetailRows(o.table, o.ctx); };
	TB.bindTable(o.table, o.ctx);
	await tick();
	const click = function (target) { o.table.dispatch('click', { target: target, preventDefault: function () {}, stopPropagation: function () {} }); };
	const detailFrom = function (n) { return J(oSeen.detail.slice(n).map(function (d) { return [d.rowId, d.expanded, d.generation]; })); };

	click(r17.ctl);
	await tick();
	const afterExpand = oSeen.detail.length;
	click(r17.ctl);
	await tick();
	out.detailToggle = { expanded: detailFrom(0), count: afterExpand };

	let mark = oSeen.detail.length;
	click(r17.ctl);
	click(r21.ctl);
	TB.applyCmd(o.table, o.ctx, { schemaVersion: 1, op: 'collapse-all' });
	await tick();
	out.detailCollapseAll = detailFrom(mark);

	mark = oSeen.detail.length;
	click(r21.ctl);
	await tick();
	const panel = r21.tr._juneauDetailPanel;
	const safe = env.el('button');
	safe.setAttribute('data-juneau-safe', 'collapse');
	panel.appendChild(safe);
	click(safe);
	await tick();
	out.detailSafeCollapse = detailFrom(mark);

	// bindSelectionPrune: a page-scoped selection drops an id that left the page, and says so.
	o.ctx.selectionState.scope = 'page';
	o.ctx.selectionState.selected = new Set(['c-17', 'c-99']);
	o.ctx._busSelectionIds = ['c-17', 'c-99'];
	NS.init.bindSelectionPrune(o.table, o.ctx);
	mark = oSeen.selection.length;
	o.dt.fire('draw.dt', { target: o.table });
	await tick();
	out.selectionPrune = { published: oSeen.selection.length - mark, last: J(last(oSeen.selection)) };
	o.ctx.selectionState.scope = 'persistent';

	// The bulk toolbar's "Clear selection" link.
	const bulkAction = { id: 'close', label: 'Close', method: 'POST', endpoint: '/api/changes/{id}/close' };
	const tb = NS.init.buildBulkToolbar({ contractVersion: NS.BULK_CONTRACT_VERSION, actions: [bulkAction] }, o.table, o.ctx, o.ctx.selectionState);
	TB.applyCmd(o.table, o.ctx, { schemaVersion: 1, op: 'select', ids: ['c-17', 'c-21'] });
	await tick();
	mark = oSeen.selection.length;
	tb.el.querySelector('.juneau-view-bulk-clear').dispatch('click');
	await tick();
	out.clearLink = { selected: o.ctx.selectionState.selected.size, published: oSeen.selection.length - mark, last: J(last(oSeen.selection)) };

	// runBulkAction, per-row: one success, one 409, one 404.
	const statusOf = function (url) { return url.includes('c-21') ? 409 : (url.includes('c-30') ? 404 : 200); };
	fetchImpl = function (url) {
		const st = statusOf(String(url));
		return Promise.resolve({ status: st, ok: st < 300, text: function () { return Promise.resolve(''); } });
	};
	const pick = function (ids) {
		o.ctx.selectionState.selected = new Set(ids);
		o.ctx.selectionState.snapshots = {};
		ids.forEach(function (id) { o.ctx.selectionState.snapshots[id] = { id: id }; });
	};
	pick(['c-17', 'c-21', 'c-30']);
	mark = oSeen.bulk.length;
	let traceMark = oTrace.length;
	await NS.init.runBulkAction(bulkAction, o.table, o.ctx, o.ctx.selectionState, ['c-17', 'c-21', 'c-30']);
	await tick();
	out.bulkPerRow = {
		published: oSeen.bulk.length - mark, payload: J(last(oSeen.bulk)), selected: J(Array.from(o.ctx.selectionState.selected)),
		bulkPublishedAfterToast: J(oTrace.slice(traceMark).filter(function (t) { return t.topic === 'bulk:drv'; }).map(function (t) { return t.toasts; }))
	};

	// runBulkAction, aggregate: a failure that names no status reports 0.
	Array.from(env.body.querySelectorAll('.juneau-view-bulk-toast')).forEach(function (t) { t.remove(); });
	const agg = { id: 'close', label: 'Close', method: 'POST', endpoint: '/api/changes/close', bulkMode: 'aggregate' };
	fetchImpl = function () {
		return Promise.resolve({ status: 200, ok: true, text: function () {
			return Promise.resolve(J({ contractVersion: '1', succeeded: ['c-17'], notFound: [], failed: [{ id: 'c-21', status: 409 }, { id: 'c-30' }] }));
		} });
	};
	pick(['c-17', 'c-21', 'c-30']);
	mark = oSeen.bulk.length;
	traceMark = oTrace.length;
	await NS.init.runBulkAction(agg, o.table, o.ctx, o.ctx.selectionState, ['c-17', 'c-21', 'c-30']);
	await tick();
	out.bulkAggregate = {
		published: oSeen.bulk.length - mark, payload: J(last(oSeen.bulk)),
		bulkPublishedAfterToast: J(oTrace.slice(traceMark).filter(function (t) { return t.topic === 'bulk:drv'; }).map(function (t) { return t.toasts; }))
	};
	out.driveErrors = errors.splice(oErrBase);

	out.errors = errors;
	process.stdout.write(J(out));
})().catch(function (e) {
	process.stdout.write(J({ crash: String(e && e.stack || e) }));
});
