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
 * table-polling.cjs - always-on Node harness for juneau-views.js's poll suspension: isPollSuspended's two
 * sources (the manual pausePolling flag and the opt-in editor-open rule), what the poll tick actually does when
 * suspended, and what the staleness pill actually says.
 *
 * Why this exists alongside TablePolling_Wiring_Test's source-substring canaries: those pin the SHAPE of the
 * code and would keep passing if the shape were right and the behavior wrong.  Two of the questions this
 * feature turns on cannot be answered by reading the source at all -
 *
 *   1. does a NON-opted-in view (every existing consumer of this shared toolkit) really still poll, even with
 *      an open detail panel and an open dialog sitting right there; and
 *   2. does the age label really keep ADVANCING while paused, rather than freezing - a frozen clock being
 *      precisely how a BROKEN poll looks, which is the confusion initPolling exists to prevent.
 *
 * Both are answered here by running the real functions against a controllable clock and interval registry.
 *
 *   Usage:  node table-polling.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require('./views-dom-shim.cjs');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node table-polling.cjs <juneau-views.js>');
	process.exit(2);
}

// ----------------------------------------------------------------------------------------------------------------
// Load juneau-views.js with a clock and an interval registry this harness owns.
//
// The shared shim's loadViews(...) stubs setInterval to a no-op returning 0 and leaves Date alone, which is fine
// for the harnesses that only poke at DOM helpers - but this feature IS a pair of timers reading a clock, so
// neither can be a stub here.  Everything else mirrors loadViews.
// ----------------------------------------------------------------------------------------------------------------

const env = makeEnv();

let now = 1_700_000_000_000;
const intervals = [];
let nextIntervalId = 1;

const RealDate = Date;
function FakeDate(...args) { return new RealDate(...args); }
FakeDate.now = function () { return now; };
FakeDate.parse = RealDate.parse;
FakeDate.UTC = RealDate.UTC;
FakeDate.prototype = RealDate.prototype;

const sandbox = {
	window: env.window,
	document: env.document,
	console: console,
	setTimeout: function (fn) { if (typeof fn === 'function') fn(); return 0; },
	clearTimeout: function () {},
	setInterval: function (fn, ms) {
		const id = nextIntervalId++;
		intervals.push({ id: id, fn: fn, ms: ms });
		return id;
	},
	clearInterval: function (id) {
		const i = intervals.findIndex(function (t) { return t.id === id; });
		if (i >= 0) intervals.splice(i, 1);
	},
	Promise: Promise,
	Date: FakeDate,
	fetch: function (...args) { return env.callFetch(...args); }
};

// NOSONAR javascript:S1523 -- loading the production juneau-views.js source into a VM sandbox is this harness's
// intended mechanism for exercising it under the DOM shim; the path is a fixed local file supplied by the test.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = env.window.JuneauViews.init;

/** Advances the fake clock and fires every registered interval whose period has elapsed at least once. */
function tick(ms) {
	now += ms;
	intervals.slice().forEach(function (t) {
		if (ms >= t.ms) t.fn();
	});
}

/** Fires only the interval registered with the given period (the poll timer vs. the 1s render timer). */
function fireIntervalsWithPeriod(ms) {
	intervals.slice().forEach(function (t) {
		if (t.ms === ms) t.fn();
	});
}

// ----------------------------------------------------------------------------------------------------------------
// Fixtures.
// ----------------------------------------------------------------------------------------------------------------

/**
 * A table with one body row, plus a fake DataTables api recording every ajax.reload and capturing the draw.dt /
 * error.dt handlers initPolling binds so the harness can fire them.
 */
function makeTable() {
	const table = env.el('table');
	table.className = 'juneau-view-table';
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(table);

	const handlers = {};
	const dt = {
		reloads: 0,
		on: function (ev, fn) { handlers[ev] = (handlers[ev] || []).concat(fn); },
		ajax: { reload: function () { dt.reloads++; } }
	};
	dt.fire = function (ev, target) {
		(handlers[ev] || []).forEach(function (fn) { fn({ target: target === undefined ? table : target }); });
	};
	/**
	 * Lands a response the way DataTables does: preDraw.dt first, and if ANY handler returns false the draw is
	 * cancelled - no draw.dt, no child rows discarded.  Returns whether the table actually redrew.
	 */
	dt.land = function () {
		const cancelled = (handlers['preDraw.dt'] || [])
			.map(function (fn) { return fn({ target: table }); })
			.some(function (r) { return r === false; });
		if (cancelled) return false;
		dt.fire('draw.dt');
		return true;
	};
	return { table: table, tbody: tbody, tr: tr, dt: dt };
}

/** Opens a detail panel the way expandDetailRow does: a child <tr> in the tbody hosting the panel div. */
function openDetailPanel(f) {
	const childTr = env.el('tr');
	const td = env.el('td');
	const panel = env.el('div');
	panel.className = 'juneau-view-detail-panel';
	td.appendChild(panel);
	childTr.appendChild(td);
	f.tbody.appendChild(childTr);
	// The marker class the FIRST implementation of this feature keyed off, and which nothing clears on draw.dt.
	f.tr.className = f.tr.className + ' juneau-view-detail-open';
	return { childTr: childTr, panel: panel };
}

/** What a redraw does to an open detail: DataTables discards the child row.  The marker class is NOT cleared. */
function redrawDiscardsChildRow(open) {
	open.childTr.remove();
}

function startPolling(f, viewDef, ctx) {
	const indicator = NS.buildStalenessIndicator();
	env.body.appendChild(indicator);
	NS.initPolling(f.table, f.dt, viewDef, indicator, ctx);
	return indicator;
}

const out = {};

// ----------------------------------------------------------------------------------------------------------------
// A. The non-opted-in consumer (IRS, the support console, every existing Juneau views user).
//
// The whole multi-consumer safety claim in one section: with NEITHER affordance declared, nothing suspends -
// not an open detail panel, not an open dialog, not both at once - and the pill keeps saying exactly what it
// always said.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000 };            // no pausePollingWhileEditing
	const ctx = { dataTable: f.dt };                      // no _pollPaused
	const indicator = startPolling(f, viewDef, ctx);

	out.default_initialLabel = indicator.textContent;
	out.default_initialState = indicator.dataset.state;

	const open = openDetailPanel(f);
	const backdrop = env.el('div');
	env.body.appendChild(backdrop);
	ctx._dialogStack = [backdrop];

	out.default_notSuspendedWithPanelAndDialogOpen = NS.isPollSuspended(f.table, ctx, viewDef) === false;

	const before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.default_pollStillFetchesWithEditorsOpen = f.dt.reloads === before + 1;

	tick(7000);
	out.default_labelAfter7s = indicator.textContent;
	out.default_stateAfter7s = indicator.dataset.state;
	out.default_neverSaysPaused = indicator.textContent.indexOf('Paused') < 0;

	redrawDiscardsChildRow(open);
	ctx._dialogStack = [];
}

// ----------------------------------------------------------------------------------------------------------------
// B. The opted-in view: an open editor suspends the fetch, closing it resumes, and the pill says so.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	const indicator = startPolling(f, viewDef, ctx);

	out.optedIn_notSuspendedWhileIdle = NS.isPollSuspended(f.table, ctx, viewDef) === false;
	let before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.optedIn_pollsWhileIdle = f.dt.reloads === before + 1;

	const open = openDetailPanel(f);
	out.optedIn_suspendedWithPanelOpen = NS.isPollSuspended(f.table, ctx, viewDef) === true;
	before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.optedIn_pollSkippedWithPanelOpen = f.dt.reloads === before;

	// The pill, 42s into a hold: "Paused", a distinct state, and an age that has ADVANCED rather than frozen.
	tick(42000);
	out.paused_label = indicator.textContent;
	out.paused_state = indicator.dataset.state;
	const firstPausedLabel = indicator.textContent;
	tick(10000);
	out.paused_ageAdvancesWhileHeld = indicator.textContent !== firstPausedLabel;
	out.paused_labelLater = indicator.textContent;

	// Closing the editor resumes on the next tick, with no manual refresh.
	redrawDiscardsChildRow(open);
	out.optedIn_resumedAfterPanelClosed = NS.isPollSuspended(f.table, ctx, viewDef) === false;
	before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.optedIn_pollResumesAfterClose = f.dt.reloads === before + 1;

	f.dt.fire('draw.dt');
	out.resumed_stateAfterDraw = indicator.dataset.state;
	out.resumed_labelAfterDraw = indicator.textContent;
}

// ----------------------------------------------------------------------------------------------------------------
// C. The stale-marker hazard, pinned behaviorally.
//
// Nothing clears `.juneau-view-detail-open` on draw.dt, and DataTables reuses row nodes in client-side mode, so
// after a redraw that is not a collapse click the class outlives the panel.  Keying suspension off the class
// would suspend this view FOREVER.  Keying it off the panel must not.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	const indicator = startPolling(f, viewDef, ctx);

	const open = openDetailPanel(f);
	redrawDiscardsChildRow(open);

	out.stale_markerClassSurvivedTheRedraw = f.table.querySelector('tr.juneau-view-detail-open') !== null;
	out.stale_panelIsGone = f.table.querySelector('.juneau-view-detail-panel') === null;
	out.stale_notSuspendedByTheOrphanedMarker = NS.isPollSuspended(f.table, ctx, viewDef) === false;

	const before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.stale_pollStillRunsAfterOrphanedMarker = f.dt.reloads === before + 1;
	tick(3000);
	out.stale_pillIsNotStuckOnPaused = indicator.textContent.indexOf('Paused') < 0;
}

// ----------------------------------------------------------------------------------------------------------------
// D. The dialog source, including the leaked-entry guard.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	startPolling(f, viewDef, ctx);

	const backdrop = env.el('div');
	env.body.appendChild(backdrop);
	ctx._dialogStack = [backdrop];
	out.dialog_suspendsWhileAttached = NS.isPollSuspended(f.table, ctx, viewDef) === true;

	// popLayer detaches the backdrop (detachOnPop) before running onDismiss.  If an onDismiss threw, the splice
	// would never happen and the entry would leak - which must NOT hold polling forever.
	backdrop.remove();
	out.dialog_leakedDetachedEntryDoesNotSuspend = NS.isPollSuspended(f.table, ctx, viewDef) === false;

	ctx._dialogStack = [];
	out.dialog_emptyStackDoesNotSuspend = NS.isPollSuspended(f.table, ctx, viewDef) === false;
}

// ----------------------------------------------------------------------------------------------------------------
// E. The manual toggle: independent of the opt-in flag, and it wins over an idle table.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000 };            // deliberately NOT opted into the editor rule
	const ctx = { dataTable: f.dt };
	const indicator = startPolling(f, viewDef, ctx);

	out.manual_notSuspendedBeforePress = NS.isPollSuspended(f.table, ctx, viewDef) === false;

	ctx._pollPaused = true;
	out.manual_suspendsWithoutTheOptInFlag = NS.isPollSuspended(f.table, ctx, viewDef) === true;

	const before = f.dt.reloads;
	fireIntervalsWithPeriod(5000);
	out.manual_pollSkipped = f.dt.reloads === before;

	// The late-bound repaint hook is what makes the pill flip on the click rather than up to a second later.
	out.manual_repaintHookInstalled = typeof ctx._onPollPausedChange === 'function';
	ctx._onPollPausedChange();
	out.manual_labelAfterPress = indicator.textContent;
	out.manual_stateAfterPress = indicator.dataset.state;

	// An explicit user refresh still works while paused (ctx.redraw / paging / sorting are not gated) and the
	// pill must stay honest about BOTH facts: freshly drawn, still held.
	f.dt.fire('draw.dt');
	out.manual_labelAfterExplicitRedraw = indicator.textContent;
	out.manual_stateAfterExplicitRedraw = indicator.dataset.state;

	ctx._pollPaused = false;
	ctx._onPollPausedChange();
	out.manual_stateAfterResume = indicator.dataset.state;
	out.manual_labelAfterResume = indicator.textContent;
}

// ----------------------------------------------------------------------------------------------------------------
// F. Error precedence: a pause the operator chose must not paper over a failure that happened to them.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	const indicator = startPolling(f, viewDef, ctx);

	f.dt.fire('error.dt');
	out.error_stateBeforePause = indicator.dataset.state;
	out.error_labelBeforePause = indicator.textContent;

	ctx._pollPaused = true;
	tick(5000);
	out.errorAndPaused_state = indicator.dataset.state;
	out.errorAndPaused_label = indicator.textContent;

	// ...and the error state is still reachable/recoverable from a hold: a successful draw clears it.
	f.dt.fire('draw.dt');
	out.errorThenDraw_state = indicator.dataset.state;
	out.errorThenDraw_label = indicator.textContent;
}

// ----------------------------------------------------------------------------------------------------------------
// G. Suspension stops the FETCH, never the render tick - the age has to keep moving while held.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt, _pollPaused: true };
	const indicator = startPolling(f, viewDef, ctx);

	const seen = [];
	for (let i = 0; i < 4; i++) {
		tick(1000);
		seen.push(indicator.textContent);
	}
	out.render_tickKeepsRunningWhilePaused = new Set(seen).size === seen.length;
	out.render_labelsWhilePaused = seen;
	out.render_pollNeverFetchedWhilePaused = f.dt.reloads === 0;
}

// ----------------------------------------------------------------------------------------------------------------
// H. The in-flight race: a reload that LEFT BEFORE the editor opened must not paint over it.
//
// The tick-time guards cannot help here - the request is already gone by the time the row is expanded.  Without
// the preDraw cancel, this is the feature failing on its own happy path: expand during the last round trip and
// the panel vanishes about as often as it survives.
// ----------------------------------------------------------------------------------------------------------------
{
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	const indicator = startPolling(f, viewDef, ctx);

	fireIntervalsWithPeriod(5000);                       // poll leaves...
	out.inflight_pollWentOut = f.dt.reloads === 1;
	const open = openDetailPanel(f);                     // ...operator expands mid-flight...
	tick(2000);
	out.inflight_drawWasCancelled = f.dt.land() === false;   // ...and the response is discarded.
	out.inflight_panelSurvived = f.table.querySelector('.juneau-view-detail-panel') !== null;

	// A cancelled draw fires neither draw.dt nor error.dt, so the clock must keep climbing and the pill must NOT
	// claim a refresh failed - nothing failed, it was thrown away on purpose.
	out.inflight_stateAfterCancel = indicator.dataset.state;
	out.inflight_labelAfterCancel = indicator.textContent;

	// Closing the editor lets the next tick through normally.
	redrawDiscardsChildRow(open);
	fireIntervalsWithPeriod(5000);
	out.inflight_drawLandsOnceEditorClosed = f.dt.land() === true;
	out.inflight_stateAfterResume = indicator.dataset.state;
}

// ----------------------------------------------------------------------------------------------------------------
// I. What the cancel must NOT eat.
// ----------------------------------------------------------------------------------------------------------------
{
	// A draw the OPERATOR asked for, while a panel is open, still paints - ctx.redraw clears the timer marker.
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt };
	startPolling(f, viewDef, ctx);

	fireIntervalsWithPeriod(5000);
	openDetailPanel(f);
	ctx._pollDrawPending = false;                        // what ctx.redraw does before an explicit reload
	out.explicit_userRefreshStillPaintsWithPanelOpen = f.dt.land() === true;
}
{
	// A view that opted into NOTHING keeps every draw, panel open or not.  This is the multi-consumer half of the
	// race fix: IRS and the console must not start silently losing redraws.
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000 };
	const ctx = { dataTable: f.dt };
	startPolling(f, viewDef, ctx);

	fireIntervalsWithPeriod(5000);
	openDetailPanel(f);
	out.default_inflightDrawStillPaints = f.dt.land() === true;
}
{
	// Manual pause alone must not cancel a draw: refresh-while-paused is the point of the manual toggle.
	const f = makeTable();
	const viewDef = { pollIntervalMs: 5000, pausePollingWhileEditing: true };
	const ctx = { dataTable: f.dt, _pollDrawPending: true, _pollPaused: true };
	startPolling(f, viewDef, ctx);

	out.manualPause_drawNotCancelledWithoutAnOpenPanel = f.dt.land() === true;
}

process.stdout.write(JSON.stringify(out, null, 1));
