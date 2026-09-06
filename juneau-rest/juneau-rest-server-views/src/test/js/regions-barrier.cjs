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
 * regions-barrier.cjs - always-on Node harness for the page-scoped initial-broadcast barrier and its bounded replay
 * window: the arming protocol and its loud backstop, the three non-settlement modes, the 2000ms deadline, the
 * released-region replay window with its membership-snapshot and last-window-close rules, the two INDEPENDENT caps,
 * and enrolled-but-hidden not being a barrier member.
 *
 * The barrier's membership set is produced by the chrome enrolment walk, which is a later edit; everything here runs
 * against a SYNTHETIC enrolment set built from initRegion/enrolIn plus registerRuntime/ready directly, which is the
 * layer the barrier's own discipline lives at.  The host-level arms - a real card grid, a real tabbed page shell -
 * re-run against the three real hosts once those call sites exist.
 *
 * Several assertions are against the CLOCK rather than against delivery order, deliberately: for a hidden enrolled
 * card, delivery is correct under both the right and the wrong reading and only the latency differs, so an
 * order-based assertion passes either way.
 *
 *   Usage:  node regions-barrier.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node regions-barrier.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

/** Loads a page and returns the harness plus a shared receive log stamped with the clock reading at delivery. */
function scene() {
	const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
	h.log = {};
	h.note = function (id, msg) {
		(h.log[id] = h.log[id] || []).push({ kind: msg.kind, at: h.clock.now() });
	};
	h.kinds = function (id) { return (h.log[id] || []).map(function (r) { return r.kind; }); };
	h.at = function (id) { return (h.log[id] || []).map(function (r) { return r.at; }); };
	return h;
}

/** A subscriber region: subscribes synchronously during its own initial populate, then settles as told. */
function subscriber(h, id, opts) {
	opts = opts || {};
	h.R.register('sub-' + id, function (ctx, container) {
		if (opts.captureCtx) opts.captureCtx(ctx, container);
		if (opts.subscribeNow !== false) ctx.on(function (msg) { h.note(id, msg); });
		if (opts.returns) return opts.returns(ctx, container);
		return undefined;
	});
	const el = H.mkRegion(h.env, {
		id: id,
		type: opts.type || 'card-body',
		host: opts.host,
		populate: 'sub-' + id,
		parent: opts.parent,
		hiddenPanel: opts.hiddenPanel
	});
	if (opts.enrol !== false) h.R.initRegion(el);
	return el;
}

/** A control region that emits during its own initial populate - the case the whole barrier exists for. */
function control(h, id, msg, opts) {
	opts = opts || {};
	h.R.register('ctl-' + id, function (ctx) {
		ctx.emit(msg);
		if (opts.returns) return opts.returns(ctx);
		return undefined;
	});
	const el = H.mkRegion(h.env, { id: id, type: 'card-body', host: opts.host, populate: 'ctl-' + id, parent: opts.parent });
	h.R.initRegion(el);
	return el;
}

/** An emitter region enrolled AFTER the barrier lifted, so its emit is an ordinary top-of-turn broadcast. */
function emitOnce(h, id, msg) {
	h.R.register('em-' + id, function (ctx) { ctx.emit(msg); });
	const el = H.mkRegion(h.env, { id: id, type: 'card-body', populate: 'em-' + id });
	h.R.initRegion(el);
	return el;
}

(async function () {

	// =================================================================================================================
	// Test 49(a) - a control region that emits during its own reason:"initial" populate and initializes FIRST still
	// reaches three chart regions that subscribe during their own LATER initial populates - including one whose
	// populate returns a Promise that settles after the control's, so the barrier is proven to wait on async
	// populates and not merely on synchronous return.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const held = H.deferred();
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'chart1');
		subscriber(h, 'chart2');
		subscriber(h, 'chart3', { returns: function () { return held.promise; } });
		h.R.ready('test');
		out.t49a_beforeAsyncSettle = [h.kinds('chart1'), h.kinds('chart2'), h.kinds('chart3')];
		held.resolve();
		await H.flush();
		out.t49a_afterAsyncSettle = [h.kinds('chart1'), h.kinds('chart2'), h.kinds('chart3')];
		out.t49a_drainedAtZeroNotDeadline = h.at('chart1').concat(h.at('chart3'));
		out.t49a_noDeadlineWarning = h.rec.warnsMatching('deadline').length === 0;
		out.t49a_controlDidNotReceiveItsOwn = h.kinds('ctl').length === 0;

		// Test 49(b) - a region activated LATER receives NOTHING retroactively: the design declines replay-on-subscribe
		// for a region that was never a barrier member.
		const late = subscriber(h, 'late', { hiddenPanel: true });
		H.activatePanel(late);
		h.R.activateRegion(late._juneauRegion);
		out.t49b_lateActivatedGetsNothing = h.kinds('late');
	}

	// =================================================================================================================
	// Test 49a - a SYNCHRONOUSLY-THROWING initial populate releases the barrier (non-settlement mode 1).
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const held = H.deferred();
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'r1');
		h.R.register('thrower', function () { throw new Error('sync boom'); });
		const bad = H.mkRegion(h.env, { id: 'bad', type: 'card-body', populate: 'thrower' });
		h.R.initRegion(bad);
		subscriber(h, 'r3', { returns: function () { return held.promise; } });
		h.R.ready('test');
		out.t49a1_heldUntilThirdSettles = [h.kinds('r1'), h.kinds('r3')];
		held.resolve();
		await H.flush();
		out.t49a1_drainedOnThirdSettle = [h.kinds('r1'), h.kinds('r3')];
		out.t49a1_drainedAtZero = h.at('r1');
		out.t49a1_throwerErrored = bad.getAttribute('data-juneau-region-state');
		out.t49a1_noDeadlineNeeded = h.rec.warnsMatching('deadline').length === 0;
	}

	// =================================================================================================================
	// Test 49b - a REJECTED initial populate releases the barrier (mode 2).  Worth separating from 49a because the
	// natural implementation - a try around a synchronous call - catches the throw and NOT the rejection, and the
	// symptom of missing the second is a page-wide freeze with a green-looking console.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const rejected = H.deferred();
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'r1');
		h.R.register('rejector', function () { return rejected.promise; });
		const bad = H.mkRegion(h.env, { id: 'bad', type: 'card-body', populate: 'rejector' });
		h.R.initRegion(bad);
		h.R.ready('test');
		out.t49b_heldWhileRejectionPending = h.kinds('r1');
		rejected.reject(new Error('async boom'));
		await H.flush();
		out.t49b_drainedOnRejection = h.kinds('r1');
		out.t49b_drainedAtZero = h.at('r1');
		out.t49b_rejectorErrored = bad.getAttribute('data-juneau-region-state');
		out.t49b_noDeadlineNeeded = h.rec.warnsMatching('deadline').length === 0;
	}

	// =================================================================================================================
	// Test 49c - a NEVER-SETTLING initial populate releases the barrier AT THE DEADLINE (mode 3).  This is the test
	// for the failure rated worse than the bug it replaced: a barrier that buffers every emit while any region is in
	// its initial populate freezes all subsequent user-driven traffic for the life of the page, not just the initial
	// broadcast.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'healthy');
		let hungContainer = null;
		let hungSignal = null;
		const hung = subscriber(h, 'hung', {
			subscribeNow: false,
			captureCtx: function (ctx, container) {
				hungSignal = ctx.signal;
				container.appendChild(h.env.el('i'));
				hungContainer = container;
			},
			returns: function () { return new Promise(function () { /* never settles */ }); }
		});
		h.R.ready('test');
		out.t49c_heldBeforeDeadline = h.kinds('healthy');
		h.clock.advance(1999);
		out.t49c_stillHeldAt1999 = h.kinds('healthy');
		h.clock.advance(1);
		out.t49c_drainedAtDeadline = h.kinds('healthy');
		out.t49c_deliveredAt = h.at('healthy');
		out.t49c_warningNamesRegion = h.rec.warnsMatching('hung').length === 1;
		out.t49c_oneWarningForThePage = h.rec.warnsMatching('initial-broadcast barrier reached').length === 1;
		// The hung populate is NOT aborted and its container is untouched: the barrier is an ordering device, not a
		// lifecycle one.
		out.t49c_hungNotAborted = hungSignal.aborted === false;
		out.t49c_hungContainerUntouched = hungContainer.querySelectorAll('i').length === 1;
		out.t49c_hungStillLoading = hung.getAttribute('data-juneau-region-state');
		// ...and a later user-driven emit is delivered normally rather than frozen for the life of the page.
		h.clock.advance(5000);
		emitOnce(h, 'later', { kind: 'M2' });
		out.t49c_laterTrafficFlows = h.kinds('healthy');
	}

	// =================================================================================================================
	// Test 49d - the deadline is a CLAMPED CONSTANT, not settable from a descriptor, a bean or a global.  An
	// author-settable barrier deadline is a way to configure a page-wide freeze.
	// =================================================================================================================
	{
		const h = scene();
		out.t49d_value = h.R.BARRIER_DEADLINE_MS;
		// Not settable through the published namespace...
		try { h.R.BARRIER_DEADLINE_MS = 5; } catch (e) { /* strict-mode refusal is also an acceptable answer */ }
		// ...nor through a global the runtime might have consulted...
		h.env.window.JUNEAU_BARRIER_DEADLINE_MS = 5;
		h.env.window.juneauBarrierDeadlineMs = 5;
		// ...nor through an attribute on the region or on its host.
		h.R.registerRuntime('test');
		const el = subscriber(h, 'slow', { enrol: false, returns: function () { return new Promise(function () {}); } });
		el.setAttribute('data-juneau-region-barrier-ms', '5');
		el.setAttribute('data-juneau-barrier-deadline', '5');
		h.env.body.setAttribute('data-juneau-barrier-deadline', '5');
		h.R.initRegion(el);
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'healthy');
		h.R.ready('test');
		h.clock.advance(5);
		out.t49d_notReleasedAtFive = h.kinds('healthy');
		h.clock.advance(1995);
		out.t49d_releasedAtTwoThousand = h.kinds('healthy');
		out.t49d_deliveredAt = h.at('healthy');
	}

	// =================================================================================================================
	// Test 49e - THE TWO-HOST CASE.  A page with both a card grid and a tabbed page shell, and the barrier must hold
	// across two independently-booting runtimes that have no reference to each other.  Under a host-scoped barrier
	// sub-case (i) fails, and it fails intermittently, which is the worst way for it to fail.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('cards');
		h.R.registerRuntime('pages');

		// The grid boots first and its control card emits during its initial populate.
		const grid = h.env.el('div');
		grid.setAttribute('data-juneau-card-grid', 'glance');
		h.env.body.appendChild(grid);
		control(h, 'ctl', { kind: 'M1' }, { parent: grid, host: 'glance' });
		subscriber(h, 'gridChart', { parent: grid, host: 'glance' });
		h.R.ready('cards');
		out.t49e_notDrainedAfterFirstRuntime = h.kinds('gridChart');

		// The page shell boots second.  Its INITIAL panel's regions enrol before the barrier lifts...
		const initialPanel = h.env.el('div');
		initialPanel.className = 'jc-panel jc-active';
		h.env.body.appendChild(initialPanel);
		subscriber(h, 'panelChart', { parent: initialPanel, type: 'tab-body', host: 'repo-a' });
		// ...and a NON-INITIAL panel carries both a naked tab-body region and a CARD, both enrolled while hidden.
		const lazyTab = subscriber(h, 'lazyTab', { hiddenPanel: true, type: 'tab-body', host: 'repo-b' });
		const lazyCard = subscriber(h, 'lazyCard', { hiddenPanel: true, type: 'card-body', host: 'repo-b' });
		h.R.ready('pages');

		// (i) The barrier held across both runtimes, and the initial panel's regions received the broadcast.
		out.t49e_i_gridChart = h.kinds('gridChart');
		out.t49e_i_panelChart = h.kinds('panelChart');
		out.t49e_i_drainedAtZero = h.at('gridChart').concat(h.at('panelChart'));

		// (ii)/(iii) The lazily activated tab's chart gets NOTHING, and so does the CARD in a non-initial panel -
		// so this is a property of LATE ACTIVATION rather than of which runtime enrolled the region.
		h.clock.advance(10000);
		for (const el of [lazyTab, lazyCard]) {
			H.activatePanel(el);
			h.R.activateRegion(el._juneauRegion);
		}
		out.t49e_ii_lazyTab = h.kinds('lazyTab');
		out.t49e_iii_lazyCard = h.kinds('lazyCard');
	}

	// =================================================================================================================
	// Test 49f - THE ARMING PROTOCOL.  (i) an empty outstanding set is held until every registered runtime has
	// reported; (ii) a page with NO regions lifts immediately and adds no listener; (iii) a registered runtime that
	// never reports has the barrier lift at DOMContentLoaded plus one macrotask, with ONE error naming it.
	// =================================================================================================================
	{
		// (i) Two registered runtimes, the second reporting late.  Every region has already settled, so the
		// outstanding set is empty the whole time - only the ready protocol is holding the barrier.
		const h = scene();
		h.R.registerRuntime('cards');
		h.R.registerRuntime('pages');
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'chart');
		h.R.ready('cards');
		out.t49f_i_heldOnEmptyOutstanding = h.kinds('chart');
		h.R.ready('pages');
		out.t49f_i_liftedOnSecondReport = h.kinds('chart');
		out.t49f_i_liftedAtZero = h.at('chart');
	}
	{
		// (ii) A page with NO regions: nothing arms, no timer is scheduled, no document listener is added, and an
		// emit is delivered immediately rather than buffered.  "A page with no regions pays nothing."
		const h = scene();
		out.t49f_ii_noTimers = h.clock.pending() === 0;
		out.t49f_ii_noDocumentListeners = Object.keys(h.env.document).indexOf('DOMContentLoaded') < 0;
		// Enrolling a region is the ONLY thing that arms it - loading the asset does not.
		out.t49f_ii_assetLoadedButUnarmed = typeof h.R.initRegion === 'function' && h.clock.pending() === 0;
	}
	{
		// (iii) The rule-4a backstop: a registered runtime that NEVER reports.  The barrier lifts anyway, one
		// macrotask after DOMContentLoaded, with exactly one error naming the forgetful runtime - so it degrades
		// LOUDLY rather than silently, which is the inverse of the failure mode the clock-only design was rejected
		// for.
		const h = scene();
		h.R.registerRuntime('forgetful');
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'chart');
		out.t49f_iii_heldBeforeMacrotask = h.kinds('chart');
		h.clock.advance(0);
		out.t49f_iii_liftedAtMacrotask = h.kinds('chart');
		out.t49f_iii_liftedAtZeroNotDeadline = h.at('chart');
		out.t49f_iii_oneErrorNamingRuntime = h.rec.errorsMatching('forgetful').length === 1;
		out.t49f_iii_errorMentionsReady = h.rec.errorsMatching('JuneauViews.regions.ready').length === 1;
	}

	// =================================================================================================================
	// Test 49g - THE RELEASED-REGION REPLAY WINDOW DELIVERS.  The healthy regions receive the broadcast at the
	// deadline (no page-wide wait); the slow region's LATER ctx.on receives the same messages, in the same order,
	// exactly once; and - the assertion that distinguishes a replay window from a retained cache - a subsequent live
	// emit is delivered AFTER the replayed ones and is not duplicated.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const slowFetch = H.deferred();
		control(h, 'ctl', { kind: 'M1' });
		subscriber(h, 'healthy');
		// The §8.5 row-1 mix-wrapper shape, with the declarative delegation stubbed out (that half is a later
		// child): await the declared fetch, THEN subscribe.
		subscriber(h, 'slow', {
			subscribeNow: false,
			returns: function (ctx) {
				return slowFetch.promise.then(function () { ctx.on(function (msg) { h.note('slow', msg); }); });
			}
		});
		h.R.ready('test');
		out.t49g_heldBeforeDeadline = h.kinds('healthy');
		h.clock.advance(2000);
		out.t49g_healthyGotItAtDeadline = h.kinds('healthy');
		out.t49g_healthyAt = h.at('healthy');
		out.t49g_slowNothingYet = h.kinds('slow');
		// The slow region settles late, subscribes, and the replay hands it what it missed.
		slowFetch.resolve();
		await H.flush();
		out.t49g_slowReplayed = h.kinds('slow');
		out.t49g_slowReplayedOnce = h.kinds('slow').filter(function (k) { return k === 'M1'; }).length === 1;
		// A subsequent LIVE emit lands after the replayed ones and is not duplicated.
		emitOnce(h, 'later', { kind: 'M2' });
		out.t49g_slowAfterLiveEmit = h.kinds('slow');
		out.t49g_healthyAfterLiveEmit = h.kinds('healthy');
	}

	// =================================================================================================================
	// Test 49h - THE REPLAY WINDOW CLOSES, AND IS NOT A RETAINED CACHE.  Four negatives, because every one of them is
	// a way the replay could grow into the retained-cache option that was rejected.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const slowFetch = H.deferred();
		const reads = [];
		const trapped = new Proxy({ kind: 'M1' }, {
			get: function (t, k) { reads.push(String(k)); return t[k]; },
			has: function (t, k) { reads.push('has:' + String(k)); return k in t; },
			ownKeys: function (t) { reads.push('ownKeys'); return Reflect.ownKeys(t); }
		});
		h.R.register('ctl', function (ctx) { ctx.emit(trapped); });
		h.R.initRegion(H.mkRegion(h.env, { id: 'ctl', type: 'card-body', populate: 'ctl' }));
		let slowCtx = null;
		subscriber(h, 'slow', {
			subscribeNow: false,
			captureCtx: function (ctx) { slowCtx = ctx; },
			returns: function () { return slowFetch.promise; }
		});
		h.R.ready('test');
		h.clock.advance(2000);

		// (i) A region that ENROLLED after the barrier lifted receives NOTHING, even while a released region's
		// window is still open.
		subscriber(h, 'lateEnrolled');
		out.t49h_i_lateEnrolledGetsNothing = h.kinds('lateEnrolled');

		// (iv) The replay reads NO payload property - what keeps payload opacity true on the replay path, and
		// precisely the objection the retained-cache option failed.  The handler captures the REFERENCE and reads
		// nothing, so the only reads that could be recorded are the framework's own.
		const got = [];
		reads.length = 0;
		slowCtx.on(function (msg) { got.push(msg); });
		out.t49h_iv_replayReadsNothing = reads.length === 0;
		out.t49h_iv_replayedIdentityPreserved = got.length === 1 && got[0] === trapped;
		for (const m of got) h.note('slow', m);
		out.t49h_iv_replayed = h.kinds('slow');

		// (ii) A released region that subscribes AFTER ITS OWN POPULATE SETTLES receives nothing: the window closed
		// with the settle.
		slowFetch.resolve();
		await H.flush();
		slowCtx.on(function (msg) { h.note('slowAfterSettle', msg); });
		out.t49h_ii_afterOwnSettleGetsNothing = h.kinds('slowAfterSettle');

		// (iii) The buffer is DISCARDED once the last released window closes, asserted by a ctx.on from a fresh
		// region afterwards receiving nothing.
		subscriber(h, 'fresh');
		out.t49h_iii_freshRegionGetsNothing = h.kinds('fresh');
	}

	// =================================================================================================================
	// Test 49i - REPLAY IS A FAN-OUT, AND A STILL-OPEN SECOND WINDOW RECEIVES WHAT THE REPLAY EMITS.  Run as two
	// arms of the SAME page so the expected order is DERIVED from the ordinary delivery rules rather than asserted as
	// a literal - which is what keeps the second delivery path from drifting out of agreement with the first.
	// =================================================================================================================
	{
		const arms = {};
		for (const arm of ['released', 'control']) {
			const h = scene();
			h.R.registerRuntime('test');
			const bFetch = H.deferred();
			const cFetch = H.deferred();
			const trace = [];
			control(h, 'ctl', { kind: 'M1' });
			// A is healthy and subscribed throughout.
			subscriber(h, 'A');
			// B and C are mix wrappers: await the declared fetch, THEN subscribe.  B re-emits on M1.
			subscriber(h, 'B', {
				subscribeNow: false,
				returns: function (ctx) {
					return bFetch.promise.then(function () {
						ctx.on(function (msg) {
							h.note('B', msg);
							if (msg.kind === 'M1') {
								ctx.emit({ kind: 'M2' });
								// HALF ONE, asserted OBSERVABLY: at the instant this handler returns from emit, A has
								// not yet received M2.  It receives it only after this drain completes.
								trace.push('A-had-M2-at-emit-return:' + (h.kinds('A').indexOf('M2') >= 0));
							}
						});
					});
				}
			});
			subscriber(h, 'C', {
				subscribeNow: false,
				returns: function (ctx) {
					return cFetch.promise.then(function () {
						ctx.on(function (msg) { h.note('C', msg); });
					});
				}
			});
			h.R.ready('test');

			if (arm === 'control') {
				// The CONTROL arm: both settle BEFORE the deadline, so no window ever opens and the ordinary
				// delivery rules govern unmodified.
				bFetch.resolve();
				await H.flush();
				cFetch.resolve();
				await H.flush();
				h.clock.advance(3000);
			} else {
				// The RELEASED arm: both are held past the deadline, B is released and replayed first, and C's fetch
				// is still in flight - its window still open - when B's replayed handler emits M2.
				h.clock.advance(2000);
				bFetch.resolve();
				await H.flush();
				cFetch.resolve();
				await H.flush();
			}
			arms[arm] = {
				A: h.kinds('A'),
				B: h.kinds('B'),
				C: h.kinds('C'),
				trace: trace,
				cycleErrors: h.rec.errorsMatching('emit cycle detected').length
			};

			if (arm === 'released') {
				// (iv) The bounds hold under appending: a late-ENROLLED region and a region enrolled at boot while
				// HIDDEN both still receive nothing, and the buffer is discarded once the last window closes.
				const lateEnrolled = subscriber(h, 'lateEnrolled');
				const hiddenAtBoot = subscriber(h, 'hiddenAtBoot', { hiddenPanel: true });
				H.activatePanel(hiddenAtBoot);
				h.R.activateRegion(hiddenAtBoot._juneauRegion);
				arms[arm].lateEnrolled = h.kinds('lateEnrolled');
				arms[arm].hiddenAtBoot = h.kinds('hiddenAtBoot');
			}
		}
		out.t49i_i_emitWasQueuedNotLive = arms.released.trace;
		out.t49i_ii_cReceivedBoth = arms.released.C;
		out.t49i_iii_identicalSequences =
			JSON.stringify([arms.released.A, arms.released.B, arms.released.C])
			=== JSON.stringify([arms.control.A, arms.control.B, arms.control.C]);
		out.t49i_iii_released = { A: arms.released.A, B: arms.released.B, C: arms.released.C };
		out.t49i_iii_control = { A: arms.control.A, B: arms.control.B, C: arms.control.C };
		out.t49i_iv_lateEnrolled = arms.released.lateEnrolled;
		out.t49i_iv_hiddenAtBoot = arms.released.hiddenAtBoot;
		out.t49i_noCycleError = arms.released.cycleErrors === 0 && arms.control.cycleErrors === 0;
	}

	// =================================================================================================================
	// Test 49j - ENROLLED-BUT-HIDDEN IS NOT A BARRIER MEMBER.  Enrolment and barrier membership are different axes,
	// and the first assertion is the one with teeth: the drain must NOT be delayed by the hidden card, asserted
	// against the CLOCK, because delivery is correct under both readings and only the latency differs.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('cards');
		const grid = h.env.el('div');
		grid.setAttribute('data-juneau-card-grid', 'glance');
		h.env.body.appendChild(grid);
		control(h, 'ctl', { kind: 'M1' }, { parent: grid });
		subscriber(h, 'visible', { parent: grid });
		const hidden = subscriber(h, 'hiddenCard', { hiddenPanel: true, parent: grid });
		h.R.ready('cards');

		// (i) THE DRAIN IS NOT DELAYED BY THE HIDDEN CARD: the clock has advanced 0ms, not 2000.
		out.t49j_i_deliveredAt = h.at('visible');
		out.t49j_i_visibleGotIt = h.kinds('visible');
		out.t49j_i_clockAtDelivery = h.clock.now();
		out.t49j_i_noDeadlineWarning = h.rec.warnsMatching('deadline').length === 0;

		// (ii) The hidden card's region IS enrolled: initRegion was called for it, it has a handle, and its teardown
		// is reachable.
		out.t49j_ii_enrolled = hidden._juneauRegion !== undefined && hidden._juneauRegion !== null;
		out.t49j_ii_hasKey = hidden._juneauRegion.key;
		out.t49j_ii_teardownReachable = typeof h.R.teardownRegion === 'function';

		// (iii) It has had NO populate at all while hidden - which is WHY it is not in the outstanding set, rather
		// than by a special case.
		out.t49j_iii_state = hidden.getAttribute('data-juneau-region-state');
		out.t49j_iii_noContent = hidden.childNodes.length === 0;

		// (iv) Activating its panel later produces exactly one populate, reason:"activate", and it receives NOTHING
		// retroactively - not M1, and no replay even if a released window is open at that moment.
		const reasons = [];
		h.R.register('sub-hiddenCard', function (ctx) {
			reasons.push(ctx.reason);
			ctx.on(function (msg) { h.note('hiddenCard', msg); });
		});
		H.activatePanel(hidden);
		h.R.activateRegion(hidden._juneauRegion);
		out.t49j_iv_reasons = reasons.slice();
		out.t49j_iv_receivedNothing = h.kinds('hiddenCard');
		h.R.activateRegion(hidden._juneauRegion);
		out.t49j_iv_activateOnce = reasons.length === 1;
	}

	// =================================================================================================================
	// Test 49k - THE REPLAY BUFFER'S OVERFLOW IS NOT A CYCLE ERROR.  257 legitimate, non-cyclic, user-driven
	// broadcasts, each from a DIFFERENT emitter and each at the top of its own turn, so no fan-out is ever nested.
	// =================================================================================================================
	{
		const h = scene();
		h.R.registerRuntime('test');
		const cFetch = H.deferred();
		const dFetch = H.deferred();
		let cCtx = null;
		subscriber(h, 'C', {
			subscribeNow: false,
			captureCtx: function (ctx) { cCtx = ctx; },
			returns: function (ctx) {
				return cFetch.promise.then(function () { ctx.on(function (m) { h.note('C', m); }); });
			}
		});
		subscriber(h, 'D', {
			subscribeNow: false,
			returns: function (ctx) {
				return dFetch.promise.then(function () { ctx.on(function (m) { h.note('D', m); }); });
			}
		});
		h.R.ready('test');
		h.clock.advance(2000);
		out.t49k_bothReleased = h.rec.warnsMatching('initial-broadcast barrier reached').length === 1;

		const cap = h.R.REPLAY_BUFFER_CAP;
		for (let i = 1; i <= cap + 1; i++) {
			emitOnce(h, 'e' + i, { kind: 'm' + i });
			if (i === 200) {
				// (ii) D settles and subscribes at append 200: it replays exactly the first 200, in order, exactly
				// once, and its window then closes - so D never reaches append 257 while C stays in flight through
				// it.  Two regions, two outcomes, both reachable.
				dFetch.resolve();
				await H.flush();
				out.t49k_ii_dReplayCount = h.kinds('D').length;
				out.t49k_ii_dReplayFirst = h.kinds('D')[0];
				out.t49k_ii_dReplayLast = h.kinds('D')[h.kinds('D').length - 1];
				out.t49k_ii_dInOrder = h.kinds('D').every(function (k, idx) { return k === 'm' + (idx + 1); });
			}
		}

		// (i) NO CYCLE ERROR IS LOGGED, at any point.  Under a single shared cap, append 65 raises one - a false
		// diagnostic for traffic that contains no cycle, and worse than silence because it names a defect that does
		// not exist.
		out.t49k_i_noCycleError = h.rec.errorsMatching('emit cycle detected').length === 0;

		// (iii) AT APPEND 257 THE BUFFER IS ABANDONED, LOUDLY.  C receives NO replay at all rather than a truncated
		// one - a run of messages with a hole is the outcome rated worse than none, because the region cannot detect
		// it - and exactly one error names C's key, the cap, and the exactly-once failure.
		const overflow = h.rec.errorsMatching('replay buffer exceeded its cap');
		out.t49k_iii_exactlyOneOverflowError = overflow.length === 1;
		out.t49k_iii_namesKey = overflow.length === 1 && overflow[0].indexOf(cCtx.key) >= 0;
		out.t49k_iii_namesCap = overflow.length === 1 && overflow[0].indexOf(String(cap)) >= 0;
		out.t49k_iii_namesExactlyOnce = overflow.length === 1 && overflow[0].indexOf('exactly-once') >= 0;
		out.t49k_iii_saysNotACycle = overflow.length === 1 && overflow[0].indexOf('not an emit cycle') >= 0;
		out.t49k_iii_namesOnlyTheOpenWindow = overflow.length === 1
			&& overflow[0].indexOf('NOT met for: ' + cCtx.key + '.') >= 0;
		cFetch.resolve();
		await H.flush();
		out.t49k_iii_cReceivedNoReplay = h.kinds('C');
		out.t49k_iii_noPartialReplay = h.kinds('C').length === 0;

		// (iv) The two caps are INDEPENDENT, asserted in both directions.  This 257-message page never raised the
		// cycle error even though its buffer overflowed...
		out.t49k_iv_bufferOverflowedWithoutCycleError = out.t49k_i_noCycleError === true
			&& out.t49k_iii_exactlyOneOverflowError === true;
		out.t49k_iv_capsDiffer = h.R.BUS_QUEUE_DEPTH_CAP !== h.R.REPLAY_BUFFER_CAP;
	}
	{
		// ...and a page that reaches the QUEUE depth cap by genuine nesting still raises the cycle error even though
		// its buffer is short.  Under one shared constant one of these two must fail.
		const h = scene();
		h.R.register('pong', function (ctx) {
			ctx.on(function () { ctx.emit({ kind: 'pong' }, { to: ctx.id === 'p1' ? 'p2' : 'p1' }); });
		});
		let starter = null;
		h.R.register('starter', function (ctx) { starter = ctx; });
		for (const id of ['p1', 'p2']) h.R.initRegion(H.mkRegion(h.env, { id: id, type: 'card-body', populate: 'pong' }));
		h.R.initRegion(H.mkRegion(h.env, { id: 's', type: 'card-body', populate: 'starter' }));
		starter.emit({ kind: 'go' }, { to: 'p1' });
		out.t49k_iv_nestingRaisesCycleError = h.rec.errorsMatching('emit cycle detected').length === 1;
		out.t49k_iv_nestingRaisedNoOverflowError = h.rec.errorsMatching('replay buffer exceeded').length === 0;
	}

	// =================================================================================================================
	// Test 49l - THE LAST-WINDOW CLOSE, WITH A GENUINELY SYNCHRONOUS SETTLE.  A thenable with a synchronous `then`,
	// so C's close condition becomes true PARTWAY THROUGH a live fan-out.  Resolving an ordinary promise from inside a
	// subscriber cannot reach this: resolving queues a microtask while the fan-out is synchronous, so the continuation
	// runs after the drain and the assertion passes for the wrong reason.
	// =================================================================================================================
	{
		const arms = {};
		for (const bFirst of [true, false]) {
			const h = scene();
			h.R.registerRuntime('test');
			const bFetch = H.deferred();
			const cSync = H.syncThenable();
			let cCtx = null;

			// B subscribes synchronously during its own populate and settles BEFORE the live emit, so B is a live
			// subscriber whose OWN window has already closed - leaving C as the LAST open window.  Its handler is
			// what makes C's close condition become true partway through the fan-out.
			const mkB = function () {
				subscriber(h, 'B', {
					subscribeNow: false,
					captureCtx: function (ctx) {
						ctx.on(function (msg) {
							h.note('B', msg);
							if (msg.kind !== 'M3') return;
							// C's settle path, collapsed onto a synchronous settle: subscribe, then resolve, both
							// inside this handler's own turn.
							cCtx.on(function (m) { h.note('C', m); });
							cSync.settle();
						});
					},
					returns: function () { return bFetch.promise; }
				});
			};
			// D exists only to make subscription order controllable: the arms differ in whether B subscribes before
			// or after it, and C's replayed sequence must be identical either way.
			if (bFirst) { mkB(); subscriber(h, 'D'); } else { subscriber(h, 'D'); mkB(); }
			subscriber(h, 'C', {
				subscribeNow: false,
				captureCtx: function (ctx) { cCtx = ctx; },
				returns: function () { return cSync.thenable; }
			});
			h.R.ready('test');
			h.clock.advance(2000);
			bFetch.resolve();
			await H.flush();

			emitOnce(h, 'em3', { kind: 'M3' });
			const armed = { C: h.kinds('C'), B: h.kinds('B'), D: h.kinds('D') };

			// (iii) A second live emit AFTER the drain is not appended for C, and (iv) the buffer was discarded at
			// that same boundary - both observed through a fresh region's subscribe receiving nothing.
			emitOnce(h, 'em4', { kind: 'M4' });
			subscriber(h, 'fresh');
			armed.freshAfterClose = h.kinds('fresh');
			armed.cAfterM4 = h.kinds('C');
			arms[bFirst ? 'bFirst' : 'bSecond'] = armed;
		}
		// (i) M3 IS in the buffer C replays: membership was snapshotted at the fan-out's start, so a window that
		// closes mid-fan-out still counted as a member for that fan-out.  Had the close taken effect immediately, C
		// would have replayed nothing.
		out.t49l_i_cReplayedM3 = arms.bFirst.C;
		out.t49l_i_exactlyOnce = arms.bFirst.C.filter(function (k) { return k === 'M3'; }).length === 1;
		// (ii) Order-independent: the buffer's contents depend on message order and member-open-time, never on the
		// order in which subscribers happen to run.
		out.t49l_ii_orderIndependent = JSON.stringify(arms.bFirst.C) === JSON.stringify(arms.bSecond.C);
		out.t49l_ii_arms = arms;
		// (iii) C is subscribed by then, so it receives M4 LIVE - what it does not get is a second replay.
		out.t49l_iii_cGotM4Live = arms.bFirst.cAfterM4;
		out.t49l_iv_bufferDiscarded = arms.bFirst.freshAfterClose.length === 0
			&& arms.bSecond.freshAfterClose.length === 0;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
