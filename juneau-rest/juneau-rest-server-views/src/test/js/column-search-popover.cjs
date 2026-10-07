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
 * column-search-popover.cjs - always-on Node harness for the per-column search popover commit semantics
 * (design §4.5 help text + §5 evaluation model).  Loads juneau-search.js into the SAME sandbox as views so the
 * popover reads the live parse/operator engine, then drives openColumnSearchPopover(...) through each commit path
 * and reports what reached the grid.  Every assertion lives in the Java test.
 *
 *   Usage:  node column-search-popover.cjs <juneau-renders.js> <juneau-views.js> <juneau-search.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const searchJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !searchJsPath) {
	console.error('usage: node column-search-popover.cjs <juneau-renders.js> <juneau-views.js> <juneau-search.js>');
	process.exit(2);
}

const { env, NS, I } = loadViews(rendersJsPath, viewsJsPath, undefined, searchJsPath);
const out = {
	hasOpenColumnSearchPopover: !!(I && typeof I.openColumnSearchPopover === 'function'),
	hasSearchEngine: !!(NS?.search && typeof NS.search.parse === 'function')
};
if (!out.hasOpenColumnSearchPopover || !out.hasSearchEngine) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

// A text column's effective operator set, mirroring the VIEW_META `search.operators` block CardEnvelope emits.
const OPERATORS = [
	{ name: '$eq', help: 'Exact match.', minArgs: 1, maxArgs: 1, combinator: false, custom: false },
	{ name: '$in', help: 'Any of the listed values.', minArgs: 1, maxArgs: -1, combinator: false, custom: false },
	{ name: '$contains', help: 'Case-insensitive substring.', minArgs: 1, maxArgs: 1, combinator: false, custom: false }
];

/** A DT2-shaped header cell carrying only the title span the popover reads for its heading. */
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

/** A fake DataTables column API tracking the last applied search + how many times draw() fired. */
function fakeCol(idx, header) {
	let applied = '';
	let draws = 0;
	const api = { draw: function () { draws++; return api; } };
	return {
		index: function () { return idx; },
		header: function () { return header; },
		search: function (v) { // NOSONAR javascript:S3800 -- mirrors DataTables' column.search() getter/setter overload: returns the value or the chainable api
			if (arguments.length === 0) return applied;
			applied = v == null ? '' : String(v);
			return api;
		},
		appliedValue: function () { return applied; },
		drawCount: function () { return draws; }
	};
}

function makeCtx(dataMode) {
	return {
		viewDef: {
			dataMode: dataMode,
			columns: [ { data: 'status', search: {
				type: 'text', operators: OPERATORS,
				bareHelp: 'Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match.'
			} } ]
		},
		optsColumns: [ { data: 'status', title: 'Status' } ]
	};
}

function makeIcon() {
	const icon = env.el('span');
	icon.className = 'juneau-view-col-search-icon';
	return icon;
}

/** Sets an input's value and fires the `input` event the popover listens on. */
function type(input, value) {
	input.value = value;
	input.dispatch('input', {});
}

/** Fires the Enter keydown the popover treats as Apply/commit. */
function pressEnter(input) {
	input.dispatch('keydown', { key: 'Enter', preventDefault: function () { /* no-op */ }, stopPropagation: function () { /* no-op */ } });
}

function popEl(ctx) { return ctx._colSearchPopover; }
function popInput(ctx) { return popEl(ctx).querySelector('.juneau-view-col-search-popover-input'); }
function statusText(ctx) {
	const s = popEl(ctx).querySelector('.juneau-view-col-search-popover-status');
	return s ? (s.textContent || '') : '';
}
function popHasClass(ctx, cls) { return (popEl(ctx).className || '').indexOf(cls) >= 0; }

// --- Scenario: operator help rendered from VIEW_META ----------------------------------------------------------
(function help() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	// The shim has no descendant selectors, so read the code/span children off each help row directly.
	const rows = popEl(ctx).querySelectorAll('.juneau-view-col-search-popover-help-op');
	out.helpOps = rows.map(function (r) { const c = r.querySelector('code'); return c ? c.textContent : null; });
	out.helpTexts = rows.map(function (r) { const s = r.querySelector('span'); return s ? s.textContent : null; });
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: the per-type bare-value help line renders before the operator help list -----------------------
(function bareHelp() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	const el = popEl(ctx);
	const bare = el.querySelector('.juneau-view-col-search-popover-barehelp');
	out.bareHelpText = bare ? bare.textContent : null;
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: a column with no search metadata at all renders no bare-help line (silent no-op) ---------------
(function bareHelpAbsent() {
	const ctx = makeCtx('client');
	ctx.viewDef.columns[0].search = null;
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	out.bareHelpAbsentIsNull = popEl(ctx).querySelector('.juneau-view-col-search-popover-barehelp') == null;
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: an incomplete "$" draft must NOT blank/refilter the grid ---------------------------------------
(function incompleteDollar() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	type(popInput(ctx), '$eq(');
	out.incompleteDrawCount = col.drawCount();          // expect 0 - grid untouched
	out.incompleteStatusIncomplete = popHasClass(ctx, 'is-incomplete');
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: an operator not on the effective set is a reject, not a silent filter --------------------------
(function rejectOperator() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	type(popInput(ctx), '$bogus(x)');
	out.rejectDrawCount = col.drawCount();               // expect 0 - not applied
	out.rejectInvalidClass = popHasClass(ctx, 'is-invalid');
	out.rejectStatusText = statusText(ctx);
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: a bare quick-filter previews LIVE on a client table --------------------------------------------
(function bareClient() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	type(popInput(ctx), 'abc');
	out.bareClientInputApplied = col.appliedValue();     // expect 'abc' - live
	out.bareClientInputDraws = col.drawCount();          // expect >= 1
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: a bare quick-filter is DEFERRED on a server table (applied only on Enter) ----------------------
(function bareServer() {
	const ctx = makeCtx('server');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	const input = popInput(ctx);
	type(input, 'abc');
	out.bareServerInputApplied = col.appliedValue();     // expect '' - deferred
	pressEnter(input);
	out.bareServerCommitApplied = col.appliedValue();    // expect 'abc' - committed on Enter
	// Enter commits + closes; nothing left to close.
})();

// --- Scenario: a "$" expression is deferred on a client table too, then commits complete+valid on Enter -------
(function dollarClient() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	const input = popInput(ctx);
	type(input, '$eq(OPEN)');
	out.dollarInputApplied = col.appliedValue();         // expect '' - deferred even on client
	pressEnter(input);
	out.dollarCommitApplied = col.appliedValue();        // expect '$eq(OPEN)'
})();

// --- Scenario: announce() creates a live region outside the wrapper and re-announces repeats ------------------
(function announcerBasics() {
	const wrapper = env.el('div');
	wrapper.className = 'dt-container';
	const table = env.el('table');
	wrapper.appendChild(table);
	env.document.body.appendChild(wrapper);

	I.announce(table, 'First message.');
	const el = wrapper.nextSibling;
	out.announcerHasLiveRegion = !!(el?.getAttribute?.('role') === 'status'
		&& el.getAttribute('aria-live') === 'polite' && el.getAttribute('aria-atomic') === 'true');
	out.announcerOutsideWrapper = !!(el && wrapper.parentNode && el.parentNode === wrapper.parentNode);
	// The Node DOM shim's setTimeout fires synchronously (see views-dom-shim.cjs), so the message is readable
	// immediately after announce() returns.
	out.announcerFirstTextMatches = el?.textContent === 'First message.';
	I.announce(table, 'First message.'); // same message twice must still announce both times
	out.announcerSecondTextMatchesAfterRepeat = el?.textContent === 'First message.';
	out.announcerReused = table.__juneauAnnouncerEl === el;
})();

/** A table inside a DataTables-style wrapper attached to the document, so announce() has a parent to sit beside. */
function wrappedTable() {
	const wrapper = env.el('div');
	wrapper.className = 'dt-container';
	const table = env.el('table');
	wrapper.appendChild(table);
	env.document.body.appendChild(wrapper);
	return { wrapper: wrapper, table: table };
}

// --- Scenario: Esc reverts a live preview back to the value the popover opened with, and announces it ---------
(function revert() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	type(popInput(ctx), 'abc');
	out.revertPreApplied = col.appliedValue();           // 'abc' live
	env.dispatchDocument('keydown', { key: 'Escape' });
	out.revertPostApplied = col.appliedValue();          // expect '' - reverted
	const announcer = w.wrapper.nextSibling;
	out.revertAnnounced = announcer?.textContent === "Search for 'Status' not applied.";
})();

// --- Scenario: a never-previewed "$" draft (incomplete, never reached the grid) still announces on dismiss -----
(function incompleteDollarDismissedAnnounces() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	type(popInput(ctx), '$eq(');
	out.incompleteDismissDrawCount = col.drawCount();     // expect 0 - never previewed
	env.dispatchDocument('keydown', { key: 'Escape' });
	const announcer = w.wrapper.nextSibling;
	out.incompleteDismissAnnounced = announcer?.textContent === "Search for 'Status' not applied.";
})();

// --- Scenario: a committed (Enter) value never announces - only a dismiss-without-commit does ------------------
(function commitDoesNotAnnounce() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	const input = popInput(ctx);
	type(input, 'abc');
	pressEnter(input); // commits + closes; onDismiss still fires, but with committed === true
	const announcer = w.wrapper.nextSibling;
	out.commitAnnouncerAbsentOrEmpty = !announcer || announcer.textContent === '';
})();

// --- Scenario: typing away then restoring the ORIGINAL value before dismissing never announces ------------------
(function typedThenRestoredDoesNotAnnounce() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	col.search('kept'); // the popover opens against this pre-existing applied value
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	const input = popInput(ctx);
	type(input, 'abc');
	type(input, 'kept'); // restore the exact original value before dismissing
	env.dispatchDocument('keydown', { key: 'Escape' });
	const announcer = w.wrapper.nextSibling;
	out.restoredAnnouncerAbsentOrEmpty = !announcer || announcer.textContent === '';
})();

// --- Scenario: two separate reverts on the SAME table both announce (clear-then-set re-fires identical text) ---
(function twoRevertsBothAnnounce() {
	const w = wrappedTable();
	const table = w.table;

	const ctx1 = makeCtx('client');
	const col1 = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col1, ctx1, table);
	type(popInput(ctx1), 'abc');
	env.dispatchDocument('keydown', { key: 'Escape' });
	const firstAnnouncer = table.__juneauAnnouncerEl;
	out.firstRevertAnnounced = firstAnnouncer?.textContent === "Search for 'Status' not applied.";

	const ctx2 = makeCtx('client');
	const col2 = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col2, ctx2, table);
	type(popInput(ctx2), 'xyz');
	env.dispatchDocument('keydown', { key: 'Escape' });
	const secondAnnouncer = table.__juneauAnnouncerEl;
	out.secondRevertAnnounced = secondAnnouncer?.textContent === "Search for 'Status' not applied.";
	out.sameAnnouncerReusedAcrossReverts = firstAnnouncer === secondAnnouncer;
})();

// --- Scenario (WORK-J0612): a client-filtered column whose DataTables API has search.fixed routes every write
// through the per-table store + one "juneau-dsl" predicate; native col.search() never sees the expression ----------
(function dslStore() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	const fixed = {};
	let fixedDraws = 0;
	const fixedApi = { draw: function () { fixedDraws++; return fixedApi; } };
	col.search.fixed = function (name, fn) {
		if (fn == null) delete fixed[name];
		else fixed[name] = fn;
		return fixedApi;
	};
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	type(popInput(ctx), 'abc');
	out.dslLiveNative = col.appliedValue();              // '' - the bare preview went to the predicate
	out.dslLiveFixedNames = Object.keys(fixed);          // ['juneau-dsl']
	out.dslLiveStore = I.getColumnExpr(ctx, col);        // 'abc'
	out.dslLiveDraws = fixedDraws;                       // >= 1
	pressEnter(popInput(ctx));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	out.dslReopenValue = popInput(ctx).value;            // 'abc' - the popover opens on the store's value
	type(popInput(ctx), '$eq(OPEN)');
	pressEnter(popInput(ctx));
	out.dslCommitStore = I.getColumnExpr(ctx, col);
	const fn = fixed['juneau-dsl'];
	out.dslPredicateExact = !!fn && fn('OPEN', { status: 'OPEN' }) === true;
	out.dslPredicateCaseSensitive = !!fn && fn('open', { status: 'open' }) === false;
	out.dslCommitNative = col.appliedValue();
})();

process.stdout.write(JSON.stringify(out));
