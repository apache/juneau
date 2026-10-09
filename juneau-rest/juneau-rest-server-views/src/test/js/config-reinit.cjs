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
 * config-reinit.cjs - always-on Node harness for the destroy/reinit index rebind (slice 5):
 * resolveOrder against the live opts.columns array via dtIndex, plus applyView / resolveActiveView
 * export presence.
 *
 * No Playwright / Chromium / DataTables — loads the real juneau-config.js then juneau-views.js IIFEs
 * against a minimal fake window/document.  Driven by ViewsJs_Reinit_Test.
 *
 *   Usage:  node config-reinit.cjs <path-to-juneau-config.js> <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test (this script only OBSERVES).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const configJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!configJsPath || !viewsJsPath) {
	console.error('usage: node config-reinit.cjs <juneau-config.js> <juneau-views.js>');
	process.exit(2);
}

const document = {
	readyState: 'loading',
	addEventListener: function () { /* no-op */ },
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () {
		return {
			setAttribute: function () { /* no-op */ },
			appendChild: function () { /* no-op */ },
			querySelector: function () { return null; },
			querySelectorAll: function () { return []; }
		};
	},
	body: { appendChild: function () { /* no-op */ }, querySelectorAll: function () { return []; } }
};

const window = { document: document, console: console, jQuery: undefined };
const sandbox = { window: window, document: document, console: console };
// NOSONAR javascript:S1523 -- this is the test harness deliberately loading the real
// juneau-config.js under test into an isolated vm sandbox; there is no untrusted input.
vm.runInNewContext(fs.readFileSync(path.resolve(configJsPath), 'utf8'), sandbox, { filename: 'juneau-config.js' }); // NOSONAR javascript:S1523 -- harness evaluates the module's own bundled script, a fixed local file
// NOSONAR javascript:S1523 -- same rationale: deliberately loading the real juneau-views.js under test
// into an isolated vm sandbox; there is no untrusted input.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' }); // NOSONAR javascript:S1523 -- harness evaluates the module's own bundled script, a fixed local file

const NS = window.JuneauViews;
const out = {
	hasConfig: !!NS?.config,
	hasInit: !!NS?.init,
	hasDtIndex: typeof NS?.config?.dtIndex === 'function',
	hasApplyView: typeof NS?.config?.applyView === 'function',
	hasResolveActiveView: typeof NS?.config?.resolveActiveView === 'function',
	hasBuildTable: typeof NS?.init?.buildTable === 'function',
	hasResolveOrder: typeof NS?.init?.resolveOrder === 'function'
};

if (!out.hasConfig || !out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const C = NS.config;
const I = NS.init;

const catalog = [
	{ data: 'A', title: 'Col A', orderable: true },
	{ data: 'B', title: 'Col B', orderable: true, visible: false },
	{ data: 'C', title: 'Col C', orderable: true }
];
const effective = C.computeEffectiveColumns(catalog, {
	schemaVersion: 2, visible: ['A', 'C'], order: ['A', 'B', 'C'], labels: {}, formats: {}
});
const optsColumns = C.buildOptsColumnSpace(effective, { hasSelection: true, hasActions: true });
optsColumns.forEach(function (c) {
	if (c.data === 'B') c.visible = false;
	if (c.data && c.data !== 'B') { c.visible = true; c.orderable = true; }
});

const viewDef = { columns: catalog, defaultOrder: [{ data: 'C', dir: 'asc' }] };
const order = I.resolveOrder(viewDef, optsColumns);
out.order = order;
out.orderIndex = order?.[0] ? order[0][0] : null;
out.orderDir = order?.[0] ? order[0][1] : null;
out.dtIndexC = C.dtIndex('C', optsColumns);

const hiddenOrder = I.resolveOrder(
	{ columns: catalog, defaultOrder: [{ data: 'B', dir: 'desc' }] },
	optsColumns
);
out.hiddenOrderIndex = hiddenOrder?.[0] ? hiddenOrder[0][0] : null;
out.hiddenOrderData = hiddenOrder?.[0]
	? optsColumns[hiddenOrder[0][0]]?.data
	: null;
out.hiddenOrderDir = hiddenOrder?.[0] ? hiddenOrder[0][1] : null;

// A column the catalog hides (defaultVisible:false) is honored the same way: its effective opts column is visible:false.
const catalogHiddenColumns = C.buildOptsColumnSpace(
	C.computeEffectiveColumns([{ data: 'A' }, { data: 'Created', defaultVisible: false }], { hasSelection: false }),
	{ hasSelection: false, hasActions: false }
);
catalogHiddenColumns.forEach(function (c) { if (c.data === 'Created') c.visible = false; else if (c.data) { c.visible = true; c.orderable = true; } });
const catalogHiddenOrder = I.resolveOrder({ columns: [], defaultOrder: [{ data: 'Created', dir: 'desc' }] }, catalogHiddenColumns);
out.catalogHiddenOrderData = catalogHiddenOrder?.[0] ? catalogHiddenColumns[catalogHiddenOrder[0][0]]?.data : null;
out.catalogHiddenOrderDir = catalogHiddenOrder?.[0] ? catalogHiddenOrder[0][1] : null;
out.catalogHiddenOrderLength = catalogHiddenOrder.length;

// An unknown field is the only case that falls back: first visible orderable column, ascending.
const unknownOrder = I.resolveOrder({ columns: catalog, defaultOrder: [{ data: 'nope', dir: 'desc' }] }, optsColumns);
out.unknownFallbackData = unknownOrder?.[0] ? optsColumns[unknownOrder[0][0]]?.data : null;
out.unknownFallbackDir = unknownOrder?.[0] ? unknownOrder[0][1] : null;

out.applyViewNotInit = C.applyView({}, null);

// Gap 1: applySearchMembershipToColumns never upgrades an intrinsically incapable column, downgrades an
// absent-from-membership capable one, and passes everything through unchanged when membership is null.
const searchCatalogEffective = C.computeEffectiveColumns(
	[
		{ data: 'A', searchable: true },
		{ data: 'B', searchable: true },
		{ data: 'C', searchable: false }
	],
	null
);
const downgraded = C.applySearchMembershipToColumns(searchCatalogEffective, ['A']);
out.searchMembershipDowngradesB = downgraded.find(function (c) { return c.data === 'B'; }).searchable;
out.searchMembershipKeepsA = downgraded.find(function (c) { return c.data === 'A'; }).searchable;
out.searchMembershipNeverUpgradesC = downgraded.find(function (c) { return c.data === 'C'; }).searchable;
out.searchMembershipNullIsNoop = C.applySearchMembershipToColumns(searchCatalogEffective, null) === searchCatalogEffective;

// Gap 1: defaultOrderFromSort is a pure {column,dir}[] -> {data,dir}[] mapping, empty/absent-safe.
out.defaultOrderFromSort = I.defaultOrderFromSort([{ column: 'C', dir: 'desc' }, { column: 'A', dir: 'asc' }]);
out.defaultOrderFromSortEmpty = I.defaultOrderFromSort(null);

// Gap 1: resolveLastAppliedViewSettings memoizes - the second call on the same ctx returns the SAME cached result
// object (so storage is never read twice and the Q1 reset notice cannot be lost to a second read).
const memoCtx = { table: {}, viewDef: { columns: [{ data: 'A' }] } };
const firstResolve = C.resolveLastAppliedViewSettings(memoCtx.table, memoCtx);
const secondResolve = C.resolveLastAppliedViewSettings(memoCtx.table, memoCtx);
out.resolveLastAppliedIsMemoized = firstResolve === secondResolve && memoCtx._lastAppliedViewSettings === firstResolve;
out.resolveLastAppliedNoBlobIsNull = memoCtx._lastAppliedViewSettings.draft;

// Gap 1: applyView's new overrides param reaches NS.init.buildTable - spy it (applyView calls it via NS.init).
let captured = null;
const realBuildTable = I.buildTable;
I.buildTable = function (table, viewDef, effective, ctx) { captured = { viewDef: viewDef, effective: effective }; return { ok: true }; };
const overrideTable = {};
overrideTable.__juneauCtx = { viewDef: { columns: [{ data: 'A', searchable: true }, { data: 'B', searchable: true }] } };
C.applyView(overrideTable, { schemaVersion: 2, visible: ['A', 'B'], order: ['A', 'B'], labels: {}, formats: {} }, {
	defaultOrder: [{ data: 'B', dir: 'desc' }],
	searchMembership: ['A']
});
I.buildTable = realBuildTable;
out.applyViewOverridesDefaultOrder = captured?.viewDef?.defaultOrder;
out.applyViewOverridesSearchableB = captured?.effective?.find(function (c) { return c.data === 'B'; })?.searchable;

// Gap 7: defaultOptions/normalizeOptions carry autoRefreshMs, defaulting Off and dropping an out-of-range value.
out.defaultOptionsAutoRefreshOff = C.defaultOptions().autoRefreshMs;
out.normalizeOptionsDropsBadAutoRefresh = C.normalizeOptions({ autoRefreshMs: 12345 }).autoRefreshMs;
out.normalizeOptionsKeepsGoodAutoRefresh = C.normalizeOptions({ autoRefreshMs: 60000 }).autoRefreshMs;

process.stdout.write(JSON.stringify(out));
