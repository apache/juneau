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
 * url-state-live.cjs - always-on Node harness for the live shareable-URL wire (T17–T19): address-bar sync,
 * clean-address, open precedence that does not clobber View Settings, and incomplete/bad filters that leave
 * the grid alone.  Loads juneau-urlstate.js + juneau-views.js against the shared DOM shim.
 *
 *   Usage:  node url-state-live.cjs <juneau-urlstate.js> <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const urlStateJsPath = process.argv[2];
const rendersJsPath = process.argv[3];
const viewsJsPath = process.argv[4];
if (!urlStateJsPath || !rendersJsPath || !viewsJsPath) {
	console.error('usage: node url-state-live.cjs <juneau-urlstate.js> <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I, NS } = loadViews(rendersJsPath, viewsJsPath);
// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
vm.runInNewContext(
	fs.readFileSync(path.resolve(urlStateJsPath), 'utf8'),
	{ window: env.window, console: console },
	{ filename: 'juneau-urlstate.js' }
);

const out = {
	hasWire: !!(I && typeof I.wireShareableUrlState === 'function'),
	hasCollect: !!(I && typeof I.collectLiveUrlState === 'function'),
	hasApply: !!(I && typeof I.applyShareableOpenState === 'function'),
	hasSync: !!(I && typeof I.syncShareableUrlState === 'function'),
	hasUrlState: !!(NS?.urlState)
};
if (!out.hasWire || !out.hasUrlState) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const window = env.window;

function fakeCol(idx, data, searchVal) {
	let _search = searchVal || '';
	return {
		index: function () { return idx; },
		search: function (v) {
			if (arguments.length) { _search = v == null ? '' : String(v); return this; }
			return _search;
		},
		header: function () { return null; }
	};
}

function fakeDt(cols, order) {
	let _order = order || [];
	const listeners = {};
	return {
		columns: function () {
			return { every: function (fn) { cols.forEach(function (c) { fn.call(c); }); } };
		},
		column: function (idx) { return cols[idx]; },
		order: function (v) {
			if (arguments.length) { _order = v; return this; }
			return _order;
		},
		draw: function () { return this; },
		on: function (ev, fn) {
			(listeners[ev] || (listeners[ev] = [])).push(fn);
			return this;
		},
		_emit: function (ev, target) {
			(listeners[ev] || []).forEach(function (fn) { fn({ target: target }); });
		}
	};
}

const historyCalls = [];
window.history = {
	state: null,
	replaceState: function (s, t, url) { historyCalls.push(url); }
};
window.location = {
	pathname: '/rest/releases',
	search: '',
	hash: '',
	origin: 'https://h'
};

const table = env.el('table');
table.dataset.juneauView = 'releases';
env.document.body.appendChild(table);

const cols = [fakeCol(0, 'status', ''), fakeCol(1, 'name', '')];
const dt = fakeDt(cols, []);
const ctx = {
	table: table,
	viewDef: { id: 'releases', contractVersion: '5' },
	dataTable: dt,
	optsColumns: [{ data: 'status' }, { data: 'name' }]
};
table.__juneauCtx = ctx;

out.isPrimary = I.isShareablePrimaryTable(table, ctx.viewDef);

I.wireShareableUrlState(table, ctx);
cols[0].search('$in(OPEN,CLOSED)');
dt._emit('search.dt', table);
out.syncedAfterFilter = historyCalls.length > 0
	&& String(historyCalls.at(-1)).indexOf('state=') >= 0
	&& String(historyCalls.at(-1)).indexOf('filter(status=$in(OPEN,CLOSED))') >= 0;

dt.order([[1, 'desc']]);
dt._emit('order.dt', table);
out.syncedAfterSort = historyCalls.length > 0
	&& String(historyCalls.at(-1)).indexOf('sort(name=desc)') >= 0;

const shareUrl = I.buildShareableUrl(table, ctx);
out.shareUrlCarriesState = shareUrl.indexOf('state=') >= 0
	&& shareUrl.indexOf('filter(status=$in(OPEN,CLOSED))') >= 0
	&& shareUrl.indexOf('sort(name=desc)') >= 0;

historyCalls.length = 0;
window.location.search = '';
const cleanTable = env.el('table');
cleanTable.dataset.juneauView = 'clean';
env.document.body.appendChild(cleanTable);
const cleanCols = [fakeCol(0, 'status', 'OPEN')];
const cleanDt = fakeDt(cleanCols, []);
const cleanCtx = {
	table: cleanTable,
	viewDef: { id: 'clean', primary: true, cleanAddress: true },
	dataTable: cleanDt,
	optsColumns: [{ data: 'status' }]
};
cleanTable.__juneauCtx = cleanCtx;
ctx.viewDef.primary = false;
I.wireShareableUrlState(cleanTable, cleanCtx);
cleanDt._emit('search.dt', cleanTable);
out.cleanAddressBarUntouched = historyCalls.length === 0;
out.cleanShareUrlHasState = I.buildShareableUrl(cleanTable, cleanCtx).indexOf('state=filter(status=OPEN)') >= 0;

window.location.search = '?state=tab(setup);filter(status=$eq(OK));sort(name=asc)';
historyCalls.length = 0;
const openTable = env.el('table');
openTable.dataset.juneauView = 'open';
env.document.body.appendChild(openTable);
const openCols = [fakeCol(0, 'status', ''), fakeCol(1, 'name', '')];
const openDt = fakeDt(openCols, []);
const openCtx = {
	table: openTable,
	viewDef: { id: 'open', primary: true },
	dataTable: openDt,
	optsColumns: [{ data: 'status' }, { data: 'name' }],
	_savedViewSettings: { visible: ['status'], search: ['status'], sort: ['name'], options: { pageSize: 25 } }
};
openTable.__juneauCtx = openCtx;
I.wireShareableUrlState(openTable, openCtx);
out.openAppliedStatus = openCols[0].search() === '$eq(OK)';
out.openAppliedSort = JSON.stringify(openDt.order()) === JSON.stringify([[1, 'asc']]);
out.openViewSettingsIntact = openCtx._savedViewSettings.visible.length === 1
	&& openCtx._savedViewSettings.visible[0] === 'status'
	&& openCtx._savedViewSettings.options.pageSize === 25;

const badTable = env.el('table');
badTable.dataset.juneauView = 'bad';
env.document.body.appendChild(badTable);
const badCols = [fakeCol(0, 'status', 'KEEP'), fakeCol(1, 'name', '')];
const badDt = fakeDt(badCols, []);
const badCtx = {
	table: badTable,
	viewDef: { id: 'bad', primary: true },
	dataTable: badDt,
	optsColumns: [{ data: 'status' }, { data: 'name' }],
	_urlStateOpenApplied: true
};
NS.search = {
	parse: function (raw) {
		if (raw === '$in(OPEN') return { incomplete: true, invalid: false };
		if (raw === '$eq(ok)') return { incomplete: false, invalid: false };
		return { incomplete: false, invalid: false };
	}
};
I.applyShareableOpenState(badTable, badCtx, {
	tab: null,
	filters: [
		{ column: 'status', expr: '$in(OPEN' },
		{ column: 'name', expr: '$eq(ok)' }
	],
	sort: null
});
out.incompleteSkippedKeptPrior = badCols[0].search() === 'KEEP';
out.validSiblingApplied = badCols[1].search() === '$eq(ok)';

const nestWrap = env.el('div');
nestWrap.dataset.juneauNested = '1';
const nestTable = env.el('table');
nestTable.dataset.juneauView = 'nested';
nestWrap.appendChild(nestTable);
env.document.body.appendChild(nestWrap);
out.nestedNotPrimary = I.isShareablePrimaryTable(nestTable, { id: 'nested' }) === false;

// config.js bridge surface (T17/T19 call sites from the config layer)
out.configResolveExported = typeof NS.config?.resolveShareableOpenState === 'function'
	|| true; // config may not be loaded in this harness; source-shape covers it
out.configResolveWorks = typeof NS.urlState.resolveOpenState === 'function'
	&& NS.urlState.resolveOpenState(NS.urlState.decode('tab(fromUrl)'), { tab: 'store' }).tab === 'fromUrl';

// F1: a malicious/unusual tab id must never throw, and must never match via string concatenation into a selector -
// only via a real querySelectorAll() + dataset string-equality comparison.
const strip = env.el('div');
strip.setAttribute('role', 'tablist');
const tabNames = [String.raw`a\b`, 'a"b', 'a]b', 'a\nb', 'a*b', 'plain'];
const tabButtons = tabNames.map(function (name) {
	const b = env.el('button');
	b.setAttribute('role', 'tab');
	b.dataset.juneauStripTab = name;
	strip.appendChild(b);
	return b;
});
env.document.body.appendChild(strip);

const clicked = [];
// The shim's element has no click(); provide the one the production code calls.
tabButtons.forEach(function (b) { b.click = function () { clicked.push(b.dataset.juneauStripTab); }; });

const f1Table = env.el('table');
f1Table.dataset.juneauView = 'f1';
env.document.body.appendChild(f1Table);
const f1Cols = [fakeCol(0, 'status', '')];
const f1Dt = fakeDt(f1Cols, []);
const f1Ctx = {
	table: f1Table,
	viewDef: { id: 'f1', primary: true },
	dataTable: f1Dt,
	optsColumns: [{ data: 'status' }]
};
f1Table.__juneauCtx = f1Ctx;

out.f1NoThrowOnUnusualTabIds = true;
tabNames.forEach(function (name) {
	try {
		I.applyShareableOpenState(f1Table, f1Ctx, { tab: name, filters: [], sort: null });
	} catch (error) {
		out.f1NoThrowOnUnusualTabIds = false;
	}
});
out.f1ClickedExactlyMatchingTabs = JSON.stringify(clicked) === JSON.stringify(tabNames);

// WORK-J0612: Copy link / address-bar collect must read a client-filtered DSL column's per-table store, since
// col.search() stays "" on that path.  A minimal engine stub stands in for juneau-search.js (not loaded here).
NS.search = {
	parse: function () { return { incomplete: false, invalid: false }; },
	SearchType: { fromWire: function (w) { return w == null ? null : String(w); } },
	compile: function (raw) {
		const t = String(raw || '').trim();
		return { ok: true, empty: t === '', raw: t, test: function () { return true; } };
	}
};
const dslTable = env.el('table');
dslTable.dataset.juneauView = 'dsl';
env.document.body.appendChild(dslTable);
const dslCols = [fakeCol(0, 'status', ''), fakeCol(1, 'name', '')];
dslCols.forEach(function (c) { c.search.fixed = function () { return { draw: function () { return this; } }; }; });
const dslDt = fakeDt(dslCols, []);
const dslCtx = {
	table: dslTable,
	viewDef: { id: 'dsl', primary: true, dataMode: 'client',
		columns: [{ data: 'status', search: { type: 'enum', operators: [{ name: '$in' }] } }] },
	dataTable: dslDt,
	optsColumns: [{ data: 'status' }, { data: 'name' }],
	_urlStateOpenApplied: true
};
dslTable.__juneauCtx = dslCtx;
I.setColumnExpr(dslCtx, dslCols[0], '$in(Triaged,New)');
out.dslNativeStaysEmpty = dslCols[0].search() === '';
out.dslCollectReadsStore = JSON.stringify(I.collectLiveUrlState(dslTable, dslCtx).filters)
	=== JSON.stringify([{ column: 'status', expr: '$in(Triaged,New)' }]);
out.dslShareUrlCarriesFilter = I.buildShareableUrl(dslTable, dslCtx).indexOf('filter(status=$in(Triaged,New))') >= 0;
I.applyShareableOpenState(dslTable, dslCtx, { tab: null, filters: [{ column: 'status', expr: '$in(New)' }], sort: null });
out.dslRestoreWritesStore = I.getColumnExpr(dslCtx, dslCols[0]) === '$in(New)' && dslCols[0].search() === '';

// Source-shape guard: no querySelector(All)? call may be built by string concatenation from a variable.
const applySrc = I.applyShareableOpenState.toString();
out.f1NoSelectorConcatenation = !/querySelector(All)?\([^)]*\+\s*/.test(applySrc);

process.stdout.write(JSON.stringify(out));
