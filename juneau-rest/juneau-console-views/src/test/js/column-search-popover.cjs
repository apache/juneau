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
 * column-search-popover.cjs - always-on Node harness for the per-column search popover commit semantics. Loads
 * juneau-search.js into the SAME sandbox as views so the
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
	const rows = popEl(ctx).querySelector('.juneau-view-col-search-popover-help').querySelectorAll('.juneau-view-col-search-popover-help-op');
	out.helpOps = rows.map(function (r) { const c = r.querySelector('code'); return c ? c.textContent : null; });
	out.helpTexts = rows.map(function (r) { const s = r.querySelector('span'); return s ? s.textContent : null; });
	I.closeColumnSearchPopover(ctx);
})();

// --- Scenario: built-in operators arrive WITHOUT help; the popover falls back to juneau-search.js -------------
// Column.searchMeta omits help for built-ins, so the popover supplies it.  A custom operator's own help still wins.
(function helpFallback() {
	const ctx = makeCtx('client');
	ctx.viewDef.columns[0].search.operators = [
		{ name: '$eq', minArgs: 1, maxArgs: 1, combinator: false, custom: false },
		{ name: '$contains', minArgs: 1, maxArgs: 1, combinator: false, custom: false },
		{ name: '$near', help: 'Within N units. Example: $near(5)', minArgs: 1, maxArgs: 1, combinator: false, custom: false }
	];
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	const rows = popEl(ctx).querySelector('.juneau-view-col-search-popover-help').querySelectorAll('.juneau-view-col-search-popover-help-op');
	out.fallbackOps = rows.map(function (r) { const c = r.querySelector('code'); return c ? c.textContent : null; });
	out.fallbackTexts = rows.map(function (r) { const s = r.querySelector('span'); return s ? s.textContent : null; });
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
	out.bareHelpFallbackShown = popEl(ctx).querySelector('.juneau-view-col-search-popover-barehelp') != null;
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

// --- Scenario: a bare quick-filter does not preview on a client table; Enter commits it --------------------------------------------
(function bareClient() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	const input = popInput(ctx);
	type(input, 'abc');
	out.bareClientInputApplied = col.appliedValue();     // expect '' - typing never filters
	out.bareClientInputDraws = col.drawCount();          // expect 0
	pressEnter(input);
	out.bareClientCommitApplied = col.appliedValue();    // expect 'abc' - committed on Enter
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

// --- Scenario: Esc leaves the filter as it was and announces it ---------
(function revert() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	type(popInput(ctx), 'abc');
	out.revertPreApplied = col.appliedValue();           // '' - typing does not filter
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

// --- Scenario: a client-filtered column whose DataTables API has search.fixed routes every write
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
	out.dslLiveNative = col.appliedValue();              // '' - typing does not apply
	out.dslLiveStore = I.getColumnExpr(ctx, col);        // '' - nothing committed yet
	out.dslLiveDraws = fixedDraws;                       // 0
	pressEnter(popInput(ctx));
	out.dslLiveFixedNames = Object.keys(fixed);          // ['juneau-dsl'] once committed
	out.dslEnterStore = I.getColumnExpr(ctx, col);       // 'abc'
	out.dslEnterDraws = fixedDraws;                      // 1
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

// ===== footer, type line, instructions, examples, "?" =========================================================
function ctxWith(type, operatorNames, extra) {
	const ctx = makeCtx('client');
	ctx.viewDef.columns[0].search = {
		type: type,
		operators: operatorNames.map(function (n) { return { name: n, help: 'Help for ' + n + '.', minArgs: 1, maxArgs: 1, combinator: false, custom: false }; }),
		bareHelp: 'Bare help.'
	};
	if (extra) Object.assign(ctx.viewDef, extra);
	return ctx;
}
function suffix(node) {
	const m = /juneau-view-col-search-popover(-[a-z]+)?/.exec(node.className || '');
	return node.tagName === 'INPUT' ? 'input' : (m && m[1] ? m[1].slice(1) : node.tagName);
}
function focusables(ctx) { return popEl(ctx).querySelectorAll('input,button,a'); }
function clickOn(ctx, cls) {
	popEl(ctx).querySelector('.juneau-view-col-search-popover-' + cls).dispatch('click', { preventDefault: function () { /* no-op */ }, stopPropagation: function () { /* no-op */ } });
}

(function footer() {
	const ctx = ctxWith('text', ['$eq', '$or']);
	I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
	const buttons = popEl(ctx).querySelectorAll('button');
	out.footerOrder = buttons.map(suffix);
	out.footerApplyClass = buttons[0].className;
	out.footerCancelHasJcBtn = (buttons[1].className || '').indexOf('jc-btn') >= 0;
	out.footerClearHasJcBtn = (buttons[2].className || '').indexOf('jc-btn') >= 0;
	out.footerMoreAria = popEl(ctx).querySelector('.juneau-view-col-search-popover-more').getAttribute('aria-expanded');
	out.footerInFooter = popEl(ctx).querySelector('.juneau-view-col-search-popover-footer').querySelectorAll('button').length === 4;
	out.tabOrder = focusables(ctx).map(suffix);
	I.closeColumnSearchPopover(ctx);
})();

(function typeLine() {
	const ctx = ctxWith('text', ['$eq']);
	I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
	out.typeLineText = popEl(ctx).querySelector('.juneau-view-col-search-popover-type').textContent;
	I.closeColumnSearchPopover(ctx);
	const ctx2 = makeCtx('client');
	ctx2.viewDef.columns[0].search = null;
	I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx2, env.el('table'));
	out.typeLineFallback = popEl(ctx2).querySelector('.juneau-view-col-search-popover-type') != null;
	I.closeColumnSearchPopover(ctx2);
})();

(function instructions() {
	function examplesFor(type, ops) {
		const ctx = ctxWith(type, ops);
		I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
		const ex = popEl(ctx).querySelector('.juneau-view-col-search-popover-examples');
		const res = ex ? ex.childNodes.map(function (c) { return c.textContent; }) : null;
		I.closeColumnSearchPopover(ctx);
		return res;
	}
	const ctx = ctxWith('text', ['$eq', '$or']);
	I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
	const ins = popEl(ctx).querySelector('.juneau-view-col-search-popover-instructions');
	out.instructionsHasBare = ins.querySelector('.juneau-view-col-search-popover-barehelp').textContent === 'Bare help.';
	out.instructionsOps = ins.querySelector('.juneau-view-col-search-popover-help').querySelectorAll('.juneau-view-col-search-popover-help-op')
		.map(function (r) { return r.querySelector('code').textContent; });
	I.closeColumnSearchPopover(ctx);
	out.examplesText = examplesFor('text', ['$eq', '$or']);
	out.examplesNumeric = examplesFor('numeric', ['$between', '$and', '$gt', '$lt']);
	out.examplesFilteredByOperators = examplesFor('numeric', ['$gt']);                 // no example fully supported
	out.examplesBareAlwaysShown = examplesFor('text', ['$eq']);                        // 'err' only
	out.examplesUncatalogued = examplesFor('bogus', ['$eq']);
})();

(function noLivePreviewAndCommit() {
	const ctx = makeCtx('client');
	const icon = makeIcon();
	env.document.body.appendChild(icon);
	const col = fakeCol(0, headerCell('Status'));
	const w = wrappedTable();
	I.openColumnSearchPopover(icon, col, ctx, w.table);
	type(popInput(ctx), 'abc');
	out.noLivePreviewApplied = col.appliedValue();
	out.noLivePreviewDraws = col.drawCount();
	clickOn(ctx, 'apply');
	out.applyApplied = col.appliedValue();
	out.applyClosed = ctx._colSearchPopover == null;
	out.applyFocusReturned = env.getActive() === icon;
	out.applyNoAnnounce = !w.wrapper.nextSibling || w.wrapper.nextSibling.textContent === '';
})();

(function cancelAndEsc() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	col.search('kept');
	const w = wrappedTable();
	I.openColumnSearchPopover(makeIcon(), col, ctx, w.table);
	type(popInput(ctx), 'abc');
	clickOn(ctx, 'cancel');
	out.cancelApplied = col.appliedValue();
	out.cancelClosed = ctx._colSearchPopover == null;
	out.cancelAnnounced = w.wrapper.nextSibling?.textContent === "Search for 'Status' not applied.";
	const ctx2 = makeCtx('client');
	const col2 = fakeCol(0, headerCell('Status'));
	col2.search('kept');
	I.openColumnSearchPopover(makeIcon(), col2, ctx2, wrappedTable().table);
	type(popInput(ctx2), 'abc');
	env.dispatchDocument('keydown', { key: 'Escape' });
	out.escApplied = col2.appliedValue();
	out.escClosed = ctx2._colSearchPopover == null;
})();

(function clearRemoves() {
	const ctx = makeCtx('client');
	const icon = makeIcon();
	icon.classList.add('is-active');
	const col = fakeCol(0, headerCell('Status'));
	col.search('x');
	I.openColumnSearchPopover(icon, col, ctx, env.el('table'));
	clickOn(ctx, 'clear');
	out.clearApplied = col.appliedValue();
	out.clearClosed = ctx._colSearchPopover == null;
	out.clearIconInactive = (icon.className || '').indexOf('is-active') < 0;
})();

(function incompleteStatusNoApply() {
	const ctx = makeCtx('client');
	const col = fakeCol(0, headerCell('Status'));
	I.openColumnSearchPopover(makeIcon(), col, ctx, env.el('table'));
	type(popInput(ctx), '$eq(');
	out.statusIncompleteWhileTyping = popHasClass(ctx, 'is-incomplete');
	out.statusIncompleteApplied = col.appliedValue();
	clickOn(ctx, 'apply');
	out.statusIncompleteStillOpen = ctx._colSearchPopover != null;
	out.statusIncompleteApplyBlocked = col.appliedValue() === '';
	I.closeColumnSearchPopover(ctx);
})();

(function moreToggles() {
	const ctx = ctxWith('text', ['$eq', '$in']);
	I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
	const list = popEl(ctx).querySelector('.juneau-view-col-search-popover-more-list');
	out.moreTag = popEl(ctx).querySelector('.juneau-view-col-search-popover-more').tagName;
	out.moreInitiallyHidden = list.hidden === true;
	clickOn(ctx, 'more');
	out.moreExpandedAria = popEl(ctx).querySelector('.juneau-view-col-search-popover-more').getAttribute('aria-expanded');
	out.moreExpandedVisible = list.hidden === false;
	out.moreRows = list.querySelectorAll('.juneau-view-col-search-popover-help-op').map(function (r) { return r.querySelector('code').textContent; });
	clickOn(ctx, 'more');
	out.moreCollapsedAria = popEl(ctx).querySelector('.juneau-view-col-search-popover-more').getAttribute('aria-expanded');
	out.moreCollapsedHidden = list.hidden === true;
	I.closeColumnSearchPopover(ctx);
})();

(function moreLinks() {
	function open(url) {
		const ctx = ctxWith('text', ['$eq'], { columnSearchHelpUrl: url });
		I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
		const more = popEl(ctx).querySelector('.juneau-view-col-search-popover-more');
		const res = { tag: more.tagName, href: more.getAttribute('href'), target: more.getAttribute('target'), rel: more.getAttribute('rel') };
		I.closeColumnSearchPopover(ctx);
		return res;
	}
	out.moreLinkAbsolute = open('https://example.com/search');
	out.moreLinkRootRelative = open('/docs/search');
	out.moreLinkUnsafe = open('javascript:alert(1)');
	out.moreLinkProtocolRelative = open('//evil.example/x');
	out.moreLinkNone = open(undefined);
})();

(function viewKeys() {
	out.viewKeysHasHelpUrl = (NS.card.liftCatalog('t', { dataUrl: '/d', columns: [{ key: 'a', label: 'A' }], columnSearchHelpUrl: '/docs/search' }).view || {}).columnSearchHelpUrl;
})();

// --- Scenario: a column with NO search block still shows type, plain-value help, operators and examples ---------
(function fallbackHelp() {
	function open(render) {
		const ctx = makeCtx('client');
		ctx.viewDef.columns[0].search = null;
		if (render) ctx.viewDef.columns[0].render = render;
		I.openColumnSearchPopover(makeIcon(), fakeCol(0, headerCell('Status')), ctx, env.el('table'));
		const el = popEl(ctx);
		const res = {
			type: el.querySelector('.juneau-view-col-search-popover-type')?.textContent ?? null,
			bare: !!el.querySelector('.juneau-view-col-search-popover-barehelp'),
			ops: el.querySelector('.juneau-view-col-search-popover-help').querySelectorAll('.juneau-view-col-search-popover-help-op').map(function (r) { return r.querySelector('code').textContent; }),
			examples: !!el.querySelector('.juneau-view-col-search-popover-examples')
		};
		I.closeColumnSearchPopover(ctx);
		return res;
	}
	out.fallbackText = open(null);
	out.fallbackTimestamp = open('datetime');
	out.fallbackNumeric = open('decimal:2');
	out.fallbackBoolean = open('bool');
})();

process.stdout.write(JSON.stringify(out));
