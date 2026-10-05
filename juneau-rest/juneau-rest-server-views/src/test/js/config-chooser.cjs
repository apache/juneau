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
 * config-chooser.cjs - always-on Node harness for the View-tab chooser:
 * last-column-hide refusal, pinned-unhideable, XSS textContent paint, Default-name reserved,
 * DataTables title sanitization.
 *
 * No Playwright / Chromium — loads the real juneau-config.js IIFE against a minimal fake `window`/`document`.
 * Driven by ViewsJs_ConfigChooser_Test (always-on when `node` is on PATH).
 *
 *   Usage:  node config-chooser.cjs <path-to-juneau-config.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const configJsPath = process.argv[2];
if (!configJsPath) {
	console.error('usage: node config-chooser.cjs <juneau-config.js>');
	process.exit(2);
}

function el(tag) {
	return {
		tagName: String(tag || 'div').toUpperCase(),
		children: [],
		textContent: '',
		innerHTML: '',
		value: '',
		hidden: false,
		disabled: false,
		checked: false,
		className: '',
		style: {},
		classList: { toggle: function () { /* no-op */ } },
		setAttribute: function () { /* no-op */ },
		getAttribute: function () { return null; },
		addEventListener: function () { /* no-op */ },
		appendChild: function (c) { this.children.push(c); return c; },
		removeChild: function (c) {
			this.children = this.children.filter(function (x) { return x !== c; });
			return c;
		},
		querySelector: function () { return null; },
		querySelectorAll: function () { return []; }
	};
}

const body = el('body');
body.appendChild = function (c) { this.children.push(c); c.parentNode = this; return c; };

const document = {
	body: body,
	createElement: el,
	querySelector: function () { return null; },
	querySelectorAll: function () { return []; }
};

const window = { document: document, console: console, prompt: function () { return null; }, confirm: function () { return true; } };
// NOSONAR javascript:S1523 -- loading the production juneau-config.js source into a VM sandbox is this harness's
// intended mechanism for exercising it against a minimal fake window/document; the input is a fixed local file
// path supplied by the test, never attacker-controlled data.
vm.runInNewContext(fs.readFileSync(path.resolve(configJsPath), 'utf8'), { window: window, document: document, console: console }, { filename: 'juneau-config.js' });

const NS = window.JuneauViews;
const out = { hasConfig: !!NS?.config };
if (!out.hasConfig) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const C = NS.config;
out.hasPaintUserText = typeof C.paintUserText === 'function';
out.hasCanHide = typeof C.canHideColumn === 'function';
out.hasMountChooser = typeof C.mountChooser === 'function';
out.hasSanitize = typeof C.sanitizeColumnTitlesForDataTables === 'function';

const catalog = [
	{ data: 'A', title: 'Col A', pinned: true },
	{ data: 'B', title: 'Col B', defaultVisible: true },
	{ data: 'C', title: 'Col C', defaultVisible: true, formats: ['date', 'ts-zulu'] }
];

const oneVisible = { visible: ['A'], order: ['A', 'B', 'C'], labels: {}, formats: {} };
out.lastCannotHideA = C.canHideColumn(oneVisible, catalog, 'A') === false;
out.lastCannotHidePinned = C.canHideColumn(oneVisible, catalog, 'A') === false;

const twoVisible = { visible: ['A', 'B'], order: ['A', 'B', 'C'], labels: {}, formats: {} };
out.pinnedCannotHide = C.canHideColumn(twoVisible, catalog, 'A') === false;
out.unpinnedCanHide = C.canHideColumn(twoVisible, catalog, 'B') === true;
out.alreadyHiddenCanShow = C.canHideColumn(twoVisible, catalog, 'C') === true;

const xssEl = el('span');
xssEl.innerHTML = 'UNTOUCHED';
C.paintUserText(xssEl, '<img src=x onerror=alert(1)>');
out.xssTextContent = xssEl.textContent;
out.xssInnerHtmlUntouched = xssEl.innerHTML === 'UNTOUCHED';

const xssInp = el('input');
xssInp.innerHTML = 'UNTOUCHED';
C.paintUserInput(xssInp, '<img src=x onerror=alert(1)>');
out.xssInputValue = xssInp.value;
out.xssInputInnerHtmlUntouched = xssInp.innerHTML === 'UNTOUCHED';

out.defaultReserved = C.isReservedName('Default') === true;
out.defaultReservedCase = C.isReservedName('DEFAULT') === true;
const basic = C.validateNameBasic('Default');
out.saveAsDefaultRefused = basic?.ok === false;

const cols = [
	{ data: 'A', title: '<img src=x onerror=alert(1)>' },
	{ data: 'B', title: 'Safe', _juneau: 'selection' }
];
C.sanitizeColumnTitlesForDataTables(cols);
out.sanitizedDataTitleBlank = cols[0].title === '';
out.sanitizedSelectionUntouched = cols[1].title === 'Safe';

const table = el('table');
const thead = el('thead');
const tr = el('tr');
const thSel = el('th');
const thA = el('th');
thA.innerHTML = 'UNTOUCHED';
const thB = el('th');
tr.children = [thSel, thA, thB];
thead.children = [tr];
table.querySelector = function (sel) { return sel === 'thead tr' ? tr : null; };
const effective = [
	{ data: 'A', title: '<img src=x onerror=alert(1)>' },
	{ data: 'B', title: 'Col B' }
];
C.paintHeaderTitles(table, effective, { selectionState: {} });
out.headerAText = thA.textContent;
out.headerAInnerHtmlUntouched = thA.innerHTML === 'UNTOUCHED';
out.headerBText = thB.textContent;

// A hidden middle column has no header cell, so the labels after it must not shift; the label goes into the
// DataTables title element and leaves the sibling sort control alone.
const hTitleA = el('span');
const hOrderA = el('span');
hOrderA.textContent = 'ORDER';
const hThA = el('th');
hThA.children = [hTitleA, hOrderA];
hThA.querySelector = function (sel) { return sel === '.dt-column-title' ? hTitleA : null; };
const hThC = el('th');
const hTr = el('tr');
hTr.children = [hThA, hThC];
const hTable = el('table');
hTable.querySelector = function (sel) { return sel === 'thead tr' ? hTr : null; };
C.paintHeaderTitles(hTable, [
	{ data: 'A', title: 'Col A' },
	{ data: 'B', title: 'Col B', visible: false },
	{ data: 'C', title: 'Col C' }
], {});
out.hiddenSkipTitleA = hTitleA.textContent;
out.hiddenSkipOrderKept = hOrderA.textContent;
out.hiddenSkipThC = hThC.textContent;

const draft = C.defaultDraftFromCatalog(catalog);
out.defaultDraftOrder = draft.order.slice();
out.defaultDraftVisible = draft.visible.slice();
out.moved = C.moveColumn(draft, 'C', -1);
out.orderAfterMove = draft.order.slice();

// -----------------------------------------------------------------------------------------------------------------
// T12 - four-tab View Settings pure layer + page-state persistence bridge.
// -----------------------------------------------------------------------------------------------------------------
out.configTabs = C.CONFIG_TABS.slice();

const t12Catalog = [
	{ data: 'name', title: 'Name', search: { type: 'text' }, orderable: true },
	{ data: 'status', title: 'Status', search: { type: 'id' }, orderable: true },
	{ data: 'notes', title: 'Notes', orderable: false }   // neither searchable nor sortable
];
out.searchCapable = C.searchCapableColumns(t12Catalog).map(function (c) { return c.data; });
out.sortCapable = C.sortCapableColumns(t12Catalog).map(function (c) { return c.data; });

const t12Draft = C.defaultDraftFromCatalog(t12Catalog);
out.draftSearch = t12Draft.search.slice();
out.draftSort = t12Draft.sort;
out.draftOptions = t12Draft.options;

out.normOptionsClamped = C.normalizeOptions({ pageSize: 999, wrap: 'yes', density: 'weird', extra: 1 });
out.normOptionsOk = C.normalizeOptions({ pageSize: 50, wrap: true, density: 'compact' });
out.intersectDropsUnknown = C.intersectMembership(['name', 'ZZZ', 'status'], ['name', 'status']);
out.intersectAbsentIsAll = C.intersectMembership(undefined, ['name', 'status']);

const vs = C.viewSettingsFromDraft(t12Draft);
out.viewSettingsKeys = Object.keys(vs).sort((a, b) => Number(a > b) - Number(a < b));

// Page-state round-trip through a mock replaceable store (design section 6.2 keying: per-table).
const memStore = {};
NS.pageState = {
	table: function (key) {
		return {
			get: function (n) { const v = memStore[key + '.' + n]; return v == null ? null : JSON.parse(v); },
			set: function (n, value) { memStore[key + '.' + n] = JSON.stringify(value); },
			remove: function (n) { delete memStore[key + '.' + n]; }
		};
	}
};
const tbl = el('table');
tbl.dataset = { juneauView: 'releases' };
C.writeViewSettings(tbl, vs);
out.persistedKeys = Object.keys(memStore);
const readback = C.readViewSettings(tbl);
out.readbackSearch = readback.settings ? readback.settings.search : null;
out.readbackResetFalseOnValidBlob = readback.reset === false;

// Two tables on one page must NOT clobber each other (T13 invariant, exercised here too).
const tbl2 = el('table');
tbl2.dataset = { juneauView: 'users' };
C.writeViewSettings(tbl2, C.viewSettingsFromDraft(C.defaultDraftFromCatalog([{ data: 'id', title: 'ID', orderable: true }])));
out.twoTableKeys = Object.keys(memStore).sort((a, b) => Number(a > b) - Number(a < b));

// A table with no view id (no page-state scope) silently no-ops rather than throwing.
const tblNoId = el('table');
tblNoId.dataset = {};
C.writeViewSettings(tblNoId, vs);
out.noIdReadback = C.readViewSettings(tblNoId).settings;

// Q1: a schemaVersion mismatch discards the WHOLE blob, deletes it, and reports reset:true.
memStore['releases.viewSettings'] = JSON.stringify({ schemaVersion: 3, visible: ['name'] });
const mismatchRead = C.readViewSettings(tbl);
out.mismatchSettingsNull = mismatchRead.settings === null;
out.mismatchResetTrue = mismatchRead.reset === true;
out.mismatchBlobDeleted = !Object.hasOwn(memStore, 'releases.viewSettings');

// Q1: a non-object stored value is treated the same as a mismatch.
memStore['releases.viewSettings'] = JSON.stringify('not-an-object');
const nonObjectRead = C.readViewSettings(tbl);
out.nonObjectSettingsNull = nonObjectRead.settings === null;
out.nonObjectResetTrue = nonObjectRead.reset === true;

// Q1: nothing stored at all is NOT a reset - just absent.
delete memStore['releases.viewSettings'];
const absentRead = C.readViewSettings(tbl);
out.absentSettingsNull = absentRead.settings === null;
out.absentResetFalse = absentRead.reset === false;

// Q1: draftFromViewSettings itself also version-checks - called directly with an unsupported version, it returns
// exactly the catalog defaults (defaultDraftFromCatalog), unchanged.
const v2Draft = C.draftFromViewSettings(t12Catalog, { schemaVersion: 3, visible: ['name'] });
const defaultsDraft = C.defaultDraftFromCatalog(t12Catalog);
out.v2DraftMatchesDefaults = JSON.stringify(v2Draft) === JSON.stringify(defaultsDraft);

// Overlaying a persisted blob drops unknown ids and normalizes Options.
const overlaid = C.draftFromViewSettings(t12Catalog,
	{ schemaVersion: C.CURRENT_SCHEMA_VERSION, visible: ['name', 'status'], order: ['status', 'name'], search: ['name', 'GONE'],
		sort: [{ column: 'status', dir: 'desc' }, { column: 'GONE', dir: 'asc' }],
		options: { pageSize: 100, wrap: true, density: 'compact' } });
out.overlaidSearch = overlaid.search.slice();
out.overlaidSort = overlaid.sort;
out.overlaidOptions = overlaid.options;

// -----------------------------------------------------------------------------------------------------------------
// Gap-g (WORK-J0559 Q7/R7) - per-view tab restriction. columnConfig.tabs restricts which of the four dialog tabs
// are offered, in CONFIG_TABS' fixed order regardless of the input array's order; an absent/boolean columnConfig,
// or an empty/all-unknown tabs array, means "all four tabs".
// -----------------------------------------------------------------------------------------------------------------
out.visibleTabsDefaultTrue = C.visibleConfigTabs({ columnConfig: true });
out.visibleTabsDefaultAbsent = C.visibleConfigTabs({});
out.visibleTabsRestricted = C.visibleConfigTabs({ columnConfig: { tabs: ['search', 'sort'] } });
out.visibleTabsOrderIsFixed = C.visibleConfigTabs({ columnConfig: { tabs: ['options', 'columns'] } });
out.visibleTabsUnknownNamesDropped = C.visibleConfigTabs({ columnConfig: { tabs: ['search', 'bogus'] } });
out.visibleTabsEmptyArrayIsAll = C.visibleConfigTabs({ columnConfig: { tabs: [] } });
out.visibleTabsAllUnknownIsAll = C.visibleConfigTabs({ columnConfig: { tabs: ['bogus', 'also-bogus'] } });

// -----------------------------------------------------------------------------------------------------------------
// Gap 6 - Sort tab is an ordered, directional priority list, not an unordered membership set.
// -----------------------------------------------------------------------------------------------------------------
const t17Catalog = [
	{ data: 'name', title: 'Name', orderable: true },
	{ data: 'status', title: 'Status', orderable: true },
	{ data: 'created', title: 'Created', orderable: true }
];
out.defaultSortOrderIsCatalogOrderAscending = C.defaultSortOrder(t17Catalog);

const sortDraft = { sort: [{ column: 'status', dir: 'desc' }, { column: 'name', dir: 'asc' }] };
out.moveSortEntryUp = (function () {
	const d = { sort: sortDraft.sort.map(function (e) { return { column: e.column, dir: e.dir }; }) };
	const moved = C.moveSortEntry(d, 'name', -1);
	return { moved: moved, order: d.sort.map(function (e) { return e.column; }) };
})();
out.moveSortEntryPastEdgeFails = (function () {
	const d = { sort: sortDraft.sort.map(function (e) { return { column: e.column, dir: e.dir }; }) };
	const moved = C.moveSortEntry(d, 'status', -1);
	return { moved: moved, order: d.sort.map(function (e) { return e.column; }) };
})();
out.intersectSortOrderDropsUnknownAndKeepsDir =
	C.intersectSortOrder([{ column: 'created', dir: 'desc' }, { column: 'GONE', dir: 'asc' }, { column: 'name', dir: 'desc' }],
		['name', 'status', 'created']);
out.intersectSortOrderCoercesBadDirToAsc =
	C.intersectSortOrder([{ column: 'name', dir: 'sideways' }], ['name']);
out.intersectSortOrderDropsDuplicates =
	C.intersectSortOrder([{ column: 'name', dir: 'asc' }, { column: 'name', dir: 'desc' }], ['name']);
out.intersectSortOrderAbsentIsCatalogOrder = C.intersectSortOrder(undefined, ['name', 'status']);

process.stdout.write(JSON.stringify(out));
