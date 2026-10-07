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
 * client-column-search.cjs - always-on Node harness for client-mode column-search DSL evaluation (WORK-J0612).
 * Loads juneau-search.js into the SAME sandbox as views, then drives the per-table expression store
 * (getColumnExpr/setColumnExpr), the popover, the shareable-URL collect/restore pair and teardown against fake
 * DataTables columns that implement `search()` AND `search.fixed(name, fn)`.  A fake filter pass runs every
 * installed fixed predicate (plus any native search, as a case-insensitive substring) over fixture rows and reports
 * the surviving row indices.  Every assertion lives in the Java test (ViewsJs_ClientColumnSearch_Test).
 *
 *   Usage:  node client-column-search.cjs <juneau-renders.js> <juneau-views.js> <juneau-search.js> [<dates.json>]
 *
 * The optional dates.json is a Java-serialized row list (S6: a bean with Date / Instant / LocalDate properties
 * serialized by the same JSON serializer a client-mode dataUrl uses), filtered with $between on each column.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const searchJsPath = process.argv[4];
const datesJsonPath = process.argv[5];
if (!rendersJsPath || !viewsJsPath || !searchJsPath) {
	console.error('usage: node client-column-search.cjs <juneau-renders.js> <juneau-views.js> <juneau-search.js> [<dates.json>]');
	process.exit(2);
}

// Capture views' warn() calls (custom-operator warnings) instead of letting them reach stderr.
const warnings = [];
const realWarn = console.warn;
console.warn = function (msg) { warnings.push(String(msg)); };

const { env, NS, I } = loadViews(rendersJsPath, viewsJsPath, undefined, searchJsPath);
const out = {
	hasGetColumnExpr: !!(I && typeof I.getColumnExpr === 'function'),
	hasSetColumnExpr: !!(I && typeof I.setColumnExpr === 'function'),
	hasCompile: !!(NS?.search && typeof NS.search.compile === 'function'),
	hasRegisterCustom: !!(NS?.search && typeof NS.search.registerCustom === 'function')
};
if (!out.hasGetColumnExpr || !out.hasSetColumnExpr || !out.hasCompile) {
	console.warn = realWarn;
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

// ---------------------------------------------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------------------------------------------

function op(name, extra) {
	return Object.assign({ name: name, help: name + ' help.', minArgs: 1, maxArgs: 1, combinator: false, custom: false }, extra || {});
}

const ENUM_OPS = [op('$eq'), op('$ne'), op('$in', { maxArgs: -1 }), op('$not', { combinator: true })];
const NUM_OPS = [op('$eq'), op('$gt'), op('$between', { minArgs: 2, maxArgs: 2 })];
const TEXT_OPS = [op('$eq'), op('$eqic'), op('$contains'), op('$not', { combinator: true }),
	op('$startsCI', { custom: true })];
const REGEX_OPS = [op('$eq'), op('$regex', { maxArgs: 2 })];

const ROWS = [
	{ id: 'r0', status: 'Triaged', priority: 1, name: 'Alpha', tags: ['a', 'b'], note: 'x' },
	{ id: 'r1', status: 'New', priority: 3, name: 'alpha', tags: ['c'], note: 'y' },
	{ id: 'r2', status: 'Closed', priority: 5, name: 'Beta', tags: [], note: 'z' },
	{ id: 'r3', priority: 2, name: 'gamma', tags: 'b', note: 'x' } // status missing entirely
];

const COLUMNS = [
	{ data: 'status', search: { type: 'enum', operators: ENUM_OPS } },
	{ data: 'priority', search: { type: 'numeric', operators: NUM_OPS } },
	{ data: 'name', search: { type: 'text', operators: TEXT_OPS } },
	{ data: 'tags', search: { type: 'text', operators: TEXT_OPS } },
	{ data: 'note' }, // no search metadata: stays native
	{ data: 'code', search: { type: 'text', operators: REGEX_OPS } }
];

/** A DT2-shaped header cell carrying the title span the popover reads, plus a search icon. */
function headerCell(title) {
	const th = env.el('th');
	const flex = env.el('div');
	flex.className = 'dt-column-header';
	const t = env.el('span');
	t.className = 'dt-column-title';
	t.textContent = title;
	flex.appendChild(t);
	th.appendChild(flex);
	return th;
}

/**
 * A fake DataTables 2 column API: native `search()` plus `search.fixed(name, fn)` (fn null removes it), the same
 * contract DataTables 2.1.8 exposes.  `withFixed:false` simulates a DataTables build without search.fixed (DT1).
 */
function fakeCol(idx, opts) {
	opts = opts || {};
	let applied = '';
	let draws = 0;
	const fixed = {};
	const header = headerCell(opts.title || ('Col' + idx));
	const api = { draw: function () { draws++; return api; } };
	const search = function (v) {
		if (arguments.length === 0) return applied;
		applied = v == null ? '' : String(v);
		return api;
	};
	if (opts.withFixed !== false) {
		search.fixed = function (name, fn) {
			if (arguments.length === 1) return fixed[name];
			if (fn == null) delete fixed[name];
			else fixed[name] = fn;
			return api;
		};
	}
	return {
		index: function () { return idx; },
		header: function () { return header; },
		search: search,
		fixedNames: function () { return Object.keys(fixed); },
		fixedFns: function () { return Object.keys(fixed).map(function (k) { return fixed[k]; }); },
		appliedValue: function () { return applied; },
		drawCount: function () { return draws; }
	};
}

/** A fake DataTables instance over fake columns: columns().every, column(i), order, draw, plus a filter pass. */
function fakeDt(cols) {
	let order = [];
	let draws = 0;
	return {
		columns: function () { return { every: function (fn) { cols.forEach(function (c) { fn.call(c); }); } }; },
		column: function (i) { return cols[i]; },
		order: function (v) { if (arguments.length) { order = v; return this; } return order; },
		draw: function () { draws++; return this; },
		drawCount: function () { return draws; },
		destroy: function () { /* no-op */ },
		on: function () { return this; }
	};
}

/**
 * Makes a fresh table context.  `mode` is 'client' (dataMode:'client'), 'server', or 'inline' (dataMode:'server'
 * but inline rows - constructTable records clientFiltered:true, which this mirrors).
 */
function makeTable(mode, colOpts) {
	const optsColumns = COLUMNS.map(function (c) { return { data: c.data, title: c.data }; });
	const cols = COLUMNS.map(function (c, i) { return fakeCol(i, Object.assign({ title: c.data }, (colOpts || {})[c.data])); });
	const ctx = {
		viewDef: { id: 't-' + mode, dataMode: mode === 'client' ? 'client' : 'server', columns: COLUMNS },
		optsColumns: optsColumns,
		dataTable: fakeDt(cols)
	};
	if (mode === 'inline') ctx.clientFiltered = true;
	if (mode === 'server') ctx.clientFiltered = false;
	const table = env.el('table');
	env.document.body.appendChild(table);
	table.__juneauCtx = ctx;
	ctx.table = table;
	return { ctx: ctx, cols: cols, table: table };
}

/** One filter pass over `rows`: every column's native search (substring) AND every installed fixed predicate. */
function visible(t, rows) {
	rows = rows || ROWS;
	const res = [];
	rows.forEach(function (row, ri) {
		const ok = t.cols.every(function (c, ci) {
			const data = t.ctx.optsColumns[ci].data;
			const cell = row[data];
			const native = c.appliedValue();
			if (native !== '' && String(cell == null ? '' : cell).toLowerCase().indexOf(native.toLowerCase()) < 0) return false;
			// DataTables passes the column's search-data string as cellData; the predicate must read the RAW row value.
			return c.fixedFns().every(function (fn) { return fn(cell == null ? '' : String(cell), row, ri) === true; });
		});
		if (ok) res.push(ri);
	});
	return res;
}

function colByName(t, name) {
	return t.cols[t.ctx.optsColumns.findIndex(function (d) { return d.data === name; })];
}

function setExpr(t, name, expr) {
	return I.setColumnExpr(t.ctx, colByName(t, name), expr);
}

function makeIcon() {
	const icon = env.el('span');
	icon.className = 'juneau-view-col-search-icon';
	return icon;
}
function type(input, value) { input.value = value; input.dispatch('input', {}); }
function pressEnter(input) {
	input.dispatch('keydown', { key: 'Enter', preventDefault: function () { /* no-op */ }, stopPropagation: function () { /* no-op */ } });
}
function popEl(ctx) { return ctx._colSearchPopover; }
function popInput(ctx) { return popEl(ctx).querySelector('.juneau-view-col-search-popover-input'); }
function popStatus(ctx) {
	const s = popEl(ctx).querySelector('.juneau-view-col-search-popover-status');
	return s ? (s.textContent || '') : '';
}

// ---------------------------------------------------------------------------------------------------------------
// Scenarios
// ---------------------------------------------------------------------------------------------------------------

// --- S1: client-filtered detection (inline rows force client filtering whatever the dataMode) -----------------
out.clientFilteredClient = I.isClientFiltered({ viewDef: { dataMode: 'client' } });
out.clientFilteredServer = I.isClientFiltered({ viewDef: { dataMode: 'server' } });
out.clientFilteredInline = I.isClientFiltered({ viewDef: { dataMode: 'server' }, clientFiltered: true });

// --- $in on an enum: the DSL predicate, never native substring ----------------------------------------------------
(function enumIn() {
	const t = makeTable('client');
	const r = setExpr(t, 'status', '$in(Triaged,New)');
	const col = colByName(t, 'status');
	out.enumInOk = r.ok;
	out.enumInRows = visible(t);
	out.enumInNativeSearch = col.appliedValue();       // '' - native search never sees the expression
	out.enumInFixedNames = col.fixedNames();             // ['juneau-dsl']
	out.enumInStoredExpr = I.getColumnExpr(t.ctx, col);  // the store, not col.search()
})();

// --- $between on a numeric column; $eqic vs $eq; $not; bare enum (D1) ---------------------------------------------
(function operators() {
	let t = makeTable('client');
	setExpr(t, 'priority', '$between(2,4)');
	out.numericBetweenRows = visible(t);
	t = makeTable('client');
	setExpr(t, 'name', '$eq(alpha)');
	out.textEqRows = visible(t);
	t = makeTable('client');
	setExpr(t, 'name', '$eqic(alpha)');
	out.textEqicRows = visible(t);
	t = makeTable('client');
	setExpr(t, 'status', '$not($eq(New))');
	out.enumNotRows = visible(t);
	t = makeTable('client');
	setExpr(t, 'status', 'Tri');
	out.enumBareTriRows = visible(t);                    // [] - D1: a bare enum value is whole-value, not substring
	t = makeTable('client');
	setExpr(t, 'status', 'Tri*');
	out.enumBareTriStarRows = visible(t);
	t = makeTable('client');
	setExpr(t, 'name', 'alp');
	out.textBareRows = visible(t);                       // bare text stays case-insensitive substring
})();

// --- Missing cell (property absent from the row object) and an array cell (D6: String(value)) --------------------
(function cellShapes() {
	let t = makeTable('client');
	setExpr(t, 'status', '$ne(New)');
	out.missingCellNeRows = visible(t);                  // includes r3 (missing) - server parity (corpus v01)
	t = makeTable('client');
	setExpr(t, 'tags', '$eq("a,b")');
	out.arrayCellEqJoinedRows = visible(t);              // ['a','b'] is matched as String(value) == 'a,b'
	t = makeTable('client');
	setExpr(t, 'tags', '$contains(b)');
	out.arrayCellContainsRows = visible(t);
})();

// --- Strict validation: a bad expression installs nothing and keeps the previous filter (D3) ---------------------
(function strict() {
	const t = makeTable('client');
	setExpr(t, 'priority', '$gt(2)');
	const before = visible(t);
	const r = setExpr(t, 'priority', '$gt(abc)');
	out.strictBadOk = r.ok;
	out.strictBadCode = r.error ? r.error.code : null;
	out.strictKeptRows = JSON.stringify(visible(t)) === JSON.stringify(before);
	out.strictKeptExpr = I.getColumnExpr(t.ctx, colByName(t, 'priority'));
	out.strictUnknownOpCode = (setExpr(t, 'priority', '$nope(1)').error || {}).code;
	out.strictOutOfTypeCode = (setExpr(t, 'status', '$gt(1)').error || {}).code;
	// Blank clears: the fixed predicate is removed and the store entry dropped.
	const cleared = setExpr(t, 'priority', '');
	out.strictClearOk = cleared.ok;
	out.strictClearFixedNames = colByName(t, 'priority').fixedNames();
	out.strictClearExpr = I.getColumnExpr(t.ctx, colByName(t, 'priority'));
})();

// --- Popover on a client DSL column: the strict server message, nothing applied ----------------------------------
(function popover() {
	const t = makeTable('client');
	const col = colByName(t, 'priority');
	I.openColumnSearchPopover(makeIcon(), col, t.ctx, t.table);
	const input = popInput(t.ctx);
	type(input, '$gt(abc)');
	pressEnter(input);
	out.popoverStillOpen = !!popEl(t.ctx);
	out.popoverInvalid = (popEl(t.ctx).className || '').indexOf('is-invalid') >= 0;
	out.popoverStatus = popStatus(t.ctx);
	out.popoverFixedNames = col.fixedNames();            // [] - nothing installed
	type(input, '$between(2,4)');
	pressEnter(input);
	out.popoverCommitClosed = !popEl(t.ctx);
	out.popoverCommitRows = visible(t);
	out.popoverCommitDraws = col.drawCount();
	// Bare value previews live on a client table through the DSL predicate.
	const t2 = makeTable('client');
	const col2 = colByName(t2, 'status');
	I.openColumnSearchPopover(makeIcon(), col2, t2.ctx, t2.table);
	type(popInput(t2.ctx), 'New');
	out.popoverLiveRows = visible(t2);
	out.popoverLiveNative = col2.appliedValue();
	env.dispatchDocument('keydown', { key: 'Escape' });  // Esc reverts the live preview
	out.popoverRevertRows = visible(t2);
	out.popoverRevertExpr = I.getColumnExpr(t2.ctx, col2);
	// A timestamp DSL column carries the UTC note; a text one does not.
	out.popoverTextHasTzHelp = false;
	const t3 = makeTable('client');
	I.openColumnSearchPopover(makeIcon(), colByName(t3, 'name'), t3.ctx, t3.table);
	out.popoverTextHasTzHelp = !!popEl(t3.ctx).querySelector('.juneau-view-col-search-popover-tzhelp');
	// Unregistered custom operators are hidden from the help list on a client-filtered table (D4).
	out.popoverHelpOpsBeforeRegister = popEl(t3.ctx).querySelectorAll('.juneau-view-col-search-popover-help-op')
		.map(function (r) { const c = r.querySelector('code'); return c ? c.textContent : null; });
	I.closeColumnSearchPopover(t3.ctx);
})();

// --- Custom operators: rejected (warned once) until registered, then evaluated (D4) ------------------------------
(function customs() {
	const t = makeTable('client');
	const before = warnings.length;
	const r1 = setExpr(t, 'name', '$startsCI(AL)');
	const r2 = setExpr(t, 'name', '$startsCI(GA)');
	out.customUnregisteredOk = r1.ok;
	out.customUnregisteredCode = r1.error ? r1.error.code : null;
	out.customWarnCount = warnings.length - before;      // 1 - one warning per name, not per attempt
	out.customSecondAttemptOk = r2.ok;
	NS.search.registerCustom('$startsCI', function (cell, args) {
		return String(cell == null ? '' : cell).toLowerCase().startsWith(String(args[0]).toLowerCase());
	});
	const r3 = setExpr(t, 'name', '$startsCI(AL)');
	out.customRegisteredOk = r3.ok;
	out.customRegisteredRows = visible(t);
	const t2 = makeTable('client');
	I.openColumnSearchPopover(makeIcon(), colByName(t2, 'name'), t2.ctx, t2.table);
	out.popoverHelpOpsAfterRegister = popEl(t2.ctx).querySelectorAll('.juneau-view-col-search-popover-help-op')
		.map(function (r) { const c = r.querySelector('code'); return c ? c.textContent : null; });
	I.closeColumnSearchPopover(t2.ctx);
	let threw = null;
	try { NS.search.registerCustom('$eq', function () { return true; }); } catch (e) { threw = e.name; }
	out.customRegisterBuiltinThrows = threw;
})();

// --- Copy-link / address bar: collectLiveUrlState reads the store -------------------------------------------------
(function collect() {
	const t = makeTable('client');
	setExpr(t, 'status', '$in(Triaged,New)');
	colByName(t, 'note').search('x');                    // a native column alongside
	const s = I.collectLiveUrlState(t.table, t.ctx);
	out.collectFilters = s.filters.map(function (f) { return f.column + '=' + f.expr; });
})();

// --- ?state= restore: valid filters install, bad ones are skipped (D3 / S8) ---------------------------------------
(function restore() {
	const t = makeTable('client');
	setExpr(t, 'priority', '$gt(1)');                   // prior value a bad link must not clobber
	const longPattern = 'a'.repeat(NS.search.MAX_REGEX_LENGTH + 1);
	I.applyShareableOpenState(t.table, t.ctx, {
		tab: null,
		filters: [
			{ column: 'status', expr: '$in(Triaged,New)' },
			{ column: 'priority', expr: '$gt(abc)' },             // BAD_VALUE - skipped, prior kept
			{ column: 'name', expr: '$regex(al.*)' },             // $regex not offered by this column - skipped
			{ column: 'code', expr: '$regex(' + longPattern + ')' } // over the server's pattern cap - skipped
		],
		sort: null
	});
	out.restoreStatusExpr = I.getColumnExpr(t.ctx, colByName(t, 'status'));
	out.restorePriorityExpr = I.getColumnExpr(t.ctx, colByName(t, 'priority'));
	out.restoreNameExpr = I.getColumnExpr(t.ctx, colByName(t, 'name'));
	out.restoreCodeExpr = I.getColumnExpr(t.ctx, colByName(t, 'code'));
	out.restoreRows = visible(t);
	// A short $regex on a column that offers it IS restored.
	I.applyShareableOpenState(t.table, t.ctx, { tab: null, filters: [{ column: 'code', expr: '$regex(c.*)' }], sort: null });
	out.restoreCodeShortExpr = I.getColumnExpr(t.ctx, colByName(t, 'code'));
})();

// --- Teardown (View Settings rebuild) clears the store, matching today's native drop (D7) -------------------------
(function teardown() {
	const t = makeTable('client');
	setExpr(t, 'status', '$in(Triaged,New)');
	out.teardownBefore = I.getColumnExpr(t.ctx, colByName(t, 'status'));
	I.teardownTable(t.table, t.ctx);
	t.ctx.dataTable = fakeDt(t.cols);                    // a rebuilt grid
	out.teardownAfter = I.getColumnExpr(t.ctx, colByName(t, 'status'));
})();

// --- Server mode, inline rows, no-meta columns and DT1 -------------------------------------------------------------
(function routing() {
	let t = makeTable('server');
	const r = setExpr(t, 'status', '$in(Triaged,New)');
	out.serverOk = r.ok;
	out.serverNative = colByName(t, 'status').appliedValue(); // the raw expression goes to the server as before
	out.serverFixedNames = colByName(t, 'status').fixedNames();
	t = makeTable('inline');
	setExpr(t, 'status', '$in(Triaged,New)');
	out.inlineRows = visible(t);
	out.inlineNative = colByName(t, 'status').appliedValue();
	t = makeTable('client');
	setExpr(t, 'note', 'x');
	out.noMetaNative = colByName(t, 'note').appliedValue();
	out.noMetaFixedNames = colByName(t, 'note').fixedNames();
	out.noMetaRows = visible(t);
	t = makeTable('client', { status: { withFixed: false } });
	setExpr(t, 'status', 'New');
	out.dt1Native = colByName(t, 'status').appliedValue();
})();

// --- S6: Java-serialized Date / Instant / LocalDate rows, filtered with $between ---------------------------------
if (datesJsonPath) {
	const rows = JSON.parse(fs.readFileSync(datesJsonPath, 'utf8'));
	const DATE_COLS = ['date', 'instant', 'localDate'];
	const TS_OPS = [op('$eq'), op('$between', { minArgs: 2, maxArgs: 2 })];
	out.dateSample = rows.length ? DATE_COLS.map(function (c) { return rows[0][c]; }) : [];
	out.dateBetween = {};
	DATE_COLS.forEach(function (c) {
		const cols = [fakeCol(0, { title: c })];
		const ctx = {
			viewDef: { dataMode: 'client', columns: [{ data: c, search: { type: 'timestamp', operators: TS_OPS } }] },
			optsColumns: [{ data: c }],
			dataTable: fakeDt(cols)
		};
		const t = { ctx: ctx, cols: cols };
		const r = I.setColumnExpr(ctx, cols[0], '$between(2026-02-01,2026-03-31)');
		out.dateBetween[c] = r.ok ? visible(t, rows) : ('error:' + (r.error && r.error.code));
	});
	// Popover UTC note on a timestamp column.
	const cols = [fakeCol(0, { title: 'date' })];
	const ctx = {
		viewDef: { dataMode: 'client', columns: [{ data: 'date', search: { type: 'timestamp', operators: TS_OPS } }] },
		optsColumns: [{ data: 'date' }],
		dataTable: fakeDt(cols)
	};
	const table = env.el('table');
	I.openColumnSearchPopover(makeIcon(), cols[0], ctx, table);
	out.popoverTimestampHasTzHelp = !!popEl(ctx).querySelector('.juneau-view-col-search-popover-tzhelp');
	I.closeColumnSearchPopover(ctx);
}

console.warn = realWarn;
out.warnings = warnings;
process.stdout.write(JSON.stringify(out));
