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
 * table-overflow.cjs - always-on Node harness for the table-overflow-discipline JS helpers
 * (ensureTableScroll / unwrapTableScroll - the DT1 "Approach B" wrap).  The jsdom-style shim has NO DataTables
 * and cannot measure scrollWidth, so this harness pins ONLY the DOM-structure invariants of the wrap helper
 * (INV-1, INV-2, INV-5, N5) and the DT2 skip-guard (N-P5-B1).  Overflow-detected tabindex (L12 A) and the
 * table-vs-page scroll contract are pinned in the opt-in Chromium suite.
 *
 *   Usage:  node table-overflow.cjs <path-to-juneau-renders.js> <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadViews } = require(path.resolve(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node table-overflow.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const report = {};

const env = makeEnv();
const { NS } = loadViews(rendersJsPath, viewsJsPath, env);
const doc = env.document;

report.hasEnsureTableScroll = typeof NS?.init?.ensureTableScroll === 'function';
report.hasUnwrapTableScroll = typeof NS?.init?.unwrapTableScroll === 'function';

const SCROLL = 'juneau-view-table-scroll';

/** Builds a fake ctx whose dataTable.columns.adjust() bumps a counter, so the harness can prove S5. */
function fakeCtx() {
	const ctx = { _adjusts: 0 };
	ctx.dataTable = { columns: function () { return ctx.dataTable._colsApi; } };
	ctx.dataTable.columns.adjust = function () { ctx._adjusts++; };
	ctx.dataTable._colsApi = { adjust: function () { ctx._adjusts++; } };
	return ctx;
}

function newEl(tag, cls) {
	const el = doc.createElement(tag);
	if (cls) el.className = cls;
	return el;
}

// ---------------------------------------------------------------------------------------------------------------
// Fixture 1 - DT1 shape: .dataTables_wrapper holding [toolbar-row (with paging-pill menu), <table>].
// ---------------------------------------------------------------------------------------------------------------
function dt1Fixture() {
	const wrapper = newEl('div', 'dataTables_wrapper');
	const toolbar = newEl('div', 'juneau-view-toolbar-row');
	const menu = newEl('div', 'juneau-view-pagingpill-menu');
	toolbar.appendChild(menu);
	const table = newEl('table');
	table.dataset.juneauView = 't1';
	wrapper.appendChild(toolbar);
	wrapper.appendChild(table);
	doc.body.appendChild(wrapper);
	return { wrapper: wrapper, toolbar: toolbar, menu: menu, table: table };
}

// T-JS-1 - wrap structure / INV-1 + S5 columns.adjust.
(function tjs1() {
	const f = dt1Fixture();
	const ctx = fakeCtx();
	NS.init.ensureTableScroll(f.table, ctx);
	const box = f.table.parentNode;
	report.t1_boxIsScrollClass = box?.className === SCROLL;
	report.t1_boxParentIsWrapper = box?.parentNode === f.wrapper;
	report.t1_tableIsSoleElementChild = box?.childNodes.filter(function (n) { return n.nodeType === 1; }).length === 1;
	// The toolbar row is a SIBLING lineage - never an ancestor of the table, and never inside the box.
	report.t1_toolbarNotAncestorOfTable = f.table.closest('.juneau-view-toolbar-row') === null;
	report.t1_boxDoesNotContainToolbar = !box.contains(f.toolbar);
	report.t1_columnsAdjusted = ctx._adjusts === 1;
	f.wrapper.remove();
})();

// T-JS-3 - paging-pill menu is NOT a descendant of the scroll box / INV-2.
(function tjs3() {
	const f = dt1Fixture();
	NS.init.ensureTableScroll(f.table, fakeCtx());
	const box = f.table.parentNode;
	report.t3_pillMenuOutsideBox = !box.contains(f.menu);
	f.wrapper.remove();
})();

// T-JS-2 - idempotency across teardown / INV-5: wrap -> unwrap -> re-wrap => exactly one box, toolbar not inside.
(function tjs2() {
	const f = dt1Fixture();
	NS.init.ensureTableScroll(f.table, fakeCtx());
	NS.init.unwrapTableScroll(f.table);
	report.t2_unwrapRestoresTableToWrapper = f.table.parentNode === f.wrapper;
	report.t2_noBoxAfterUnwrap = f.wrapper.querySelectorAll('.' + SCROLL).length === 0;
	// Re-wrap (simulates a destroy()+reconstruct construct pass).
	NS.init.ensureTableScroll(f.table, fakeCtx());
	report.t2_exactlyOneBoxAfterRewrap = f.wrapper.querySelectorAll('.' + SCROLL).length === 1;
	const box = f.table.parentNode;
	report.t2_toolbarNotDescendantOfBox = !box.contains(f.toolbar);
	report.t2_tableNotDuplicated = f.wrapper.querySelectorAll('table').length === 1;
	f.wrapper.remove();
})();

// T-JS-5 - nested tables / N5: a table already inside a .juneau-view-table-scroll must NOT get a second box.
(function tjs5() {
	const outerBox = newEl('div', SCROLL);
	const table = newEl('table');
	table.dataset.juneauView = 'inner';
	outerBox.appendChild(table);
	doc.body.appendChild(outerBox);
	NS.init.ensureTableScroll(table, fakeCtx());
	report.t5_noSecondBox = table.parentNode === outerBox
		&& outerBox.querySelectorAll('.' + SCROLL).length === 0;
	outerBox.remove();
})();

// N-P5-B1 - DT2 skip-guard: a DT2 table (parent is .dt-layout-cell) must NOT be wrapped (Approach D CSS owns it).
(function dt2Skip() {
	const container = newEl('div', 'dt-container');
	const row = newEl('div', 'dt-layout-row dt-layout-table');
	const cell = newEl('div', 'dt-layout-cell');
	const table = newEl('table');
	table.dataset.juneauView = 'dt2';
	cell.appendChild(table);
	row.appendChild(cell);
	container.appendChild(row);
	doc.body.appendChild(container);
	NS.init.ensureTableScroll(table, fakeCtx());
	report.dt2_notWrapped = table.parentNode === cell;
	report.dt2_noScrollBoxCreated = container.querySelectorAll('.' + SCROLL).length === 0;
	container.remove();
})();

// L12 A - the scroll region is keyboard-reachable ONLY while it actually overflows.  The shim cannot lay out, but
// applyScrollRegionA11y reads scrollWidth/clientWidth off the region, so the harness can set them directly and drive
// both sides of the fork.  Also proves a DT1-shaped DOM RESOLVES a scroll region at all (there is no DT1 refusal).
(function l12a() {
	const f = dt1Fixture();
	NS.init.ensureTableScroll(f.table, fakeCtx());
	const box = f.table.parentNode;

	// A DT1-shaped DOM resolves through the existing wrap path - the DT1 generation is still fully supported.
	report.l12_dt1RegionResolves = NS.init.scrollRegionFor(f.table) === box;

	// Overflowing: tabindex + a generic label appear.
	box.scrollWidth = 800;
	box.clientWidth = 200;
	NS.init.applyScrollRegionA11y(f.table, fakeCtx());
	report.l12_overflowingHasTabindex = box.getAttribute('tabindex') === '0';
	report.l12_overflowingHasLabel = box.getAttribute('aria-label') === NS.init.TABLE_SCROLL_LABEL;

	// Not overflowing: BOTH are removed again (an unconditional tab stop is a false "scrollable" announcement).
	box.scrollWidth = 200;
	box.clientWidth = 200;
	NS.init.applyScrollRegionA11y(f.table, fakeCtx());
	report.l12_notOverflowingNoTabindex = box.getAttribute('tabindex') == null;
	report.l12_notOverflowingNoLabel = box.getAttribute('aria-label') == null;

	f.wrapper.remove();
})();

// The clip/ellipsis opt-out has to REACH the cell to mean anything: a column bound to a named emitter must carry
// `juneau-cell-wrap` on its DataTables column className, which is what puts the class on the rendered <td>.
(function cellWrapOptOut() {
	const deps = {
		parseRenderId: NS.parseRenderId,
		resolveRenderer: NS.resolveRenderer,
		warn: function () { /* unknown-id path not under test here */ }
	};
	const classOf = function (render, extra) {
		const col = { data: 'c', render: render };
		if (extra) for (const k in extra) col[k] = extra[k];
		return String(NS.init.buildColumnDef(col, deps).className || '');
	};
	report.wrap_pill = classOf('pill').indexOf('juneau-cell-wrap') >= 0;
	report.wrap_progress = classOf('progress').indexOf('juneau-cell-wrap') >= 0;
	report.wrap_tag = classOf('tag').indexOf('juneau-cell-wrap') >= 0;
	report.wrap_linked = classOf('linked').indexOf('juneau-cell-wrap') >= 0;
	// A plain prose column stays on the clip default - it must NOT opt out.
	report.wrap_truncateStaysClipped = classOf('truncate').indexOf('juneau-cell-wrap') < 0;
	report.wrap_dateStaysClipped = classOf('date').indexOf('juneau-cell-wrap') < 0;
	// An author className is preserved, not replaced, when the renderer contributes one.
	report.wrap_authorClassPreserved = classOf('pill', { className: 'author-col' }) === 'author-col pill-cell juneau-cell-wrap';
})();

process.stdout.write(JSON.stringify(report));
