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
 * regions-primitive.cjs - always-on Node harness for the populate(ctx, container) primitive: the ctx golden, the
 * clear-before-every-call and coalesce-on-in-flight idempotency rules, contained throws, the four return-value
 * shapes, the fixed teardown order, cancellation generations including signal.fork()'s attachment mechanics, the
 * AbortController baseline refusal, and reason-tracks-the-causal-event.
 *
 *   Usage:  node regions-primitive.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node regions-primitive.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

function stateOf(el) {
	return el.getAttribute('data-juneau-region-state');
}

(async function () {

	// =================================================================================================================
	// Test 1 - the container is EMPTY and IN-DOCUMENT at the first call, for each of the three region types.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const seen = {};
		R.register('probe', function (ctx, container) {
			seen[ctx.type] = {
				childCount: container.childNodes.length,
				connected: env.document.contains(container),
				sameNode: container === ctx0[ctx.type],
				state: stateOf(container)
			};
			container.appendChild(env.el('span'));
		});
		const ctx0 = {};
		for (const type of ['row-detail', 'card-body', 'tab-body']) {
			const el = H.mkRegion(env, { id: 'r-' + type, type: type, populate: 'probe' });
			ctx0[type] = el;
			R.initRegion(el);
		}
		out.t1_types = Object.keys(seen).sort();
		out.t1_allEmpty = Object.values(seen).every(function (s) { return s.childCount === 0; });
		out.t1_allConnected = Object.values(seen).every(function (s) { return s.connected === true; });
		out.t1_allSameNode = Object.values(seen).every(function (s) { return s.sameNode === true; });
		// The state was already `loading` when the populator ran, and NOTHING had been painted into the container to
		// say so - the state rides an attribute precisely so the container can stay empty.
		out.t1_loadingViaAttrNotDom = Object.values(seen).every(function (s) { return s.state === 'loading'; });
	}

	// =================================================================================================================
	// Test 2 - the ctx field set is an EXACT golden, and fork()'s attachment does not show up in it.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let ctx = null;
		R.register('capture', function (c) { ctx = c; });
		const grid = env.el('div');
		grid.setAttribute('data-juneau-card-grid', 'glance');
		const card = env.el('div');
		card.setAttribute('data-juneau-card', 'posture');
		grid.appendChild(card);
		env.body.appendChild(grid);
		const el = H.mkRegion(env, { id: 'diagnose', type: 'row-detail', host: 'gacks/row-42', populate: 'capture', parent: card });
		R.initRegion(el);

		out.t2_keys = Object.keys(ctx).sort();
		out.t2_declaredKeys = Object.keys(ctx.declared).sort();
		out.t2_idsKeys = Object.keys(ctx.ids).sort();
		out.t2_types = {};
		for (const k of Object.keys(ctx).sort()) out.t2_types[k] = typeof ctx[k];
		out.t2_contractVersion = ctx.contractVersion;
		out.t2_type = ctx.type;
		out.t2_id = ctx.id;
		out.t2_key = ctx.key;
		out.t2_host = ctx.host;
		out.t2_reason = ctx.reason;
		out.t2_generation = ctx.generation;
		out.t2_messageSignalNullOnInitial = ctx.messageSignal === null;
		out.t2_dataNull = ctx.data === null;
		out.t2_selectionNull = ctx.selection === null;
		out.t2_paramsIsObject = ctx.params !== null && typeof ctx.params === 'object';
		// ids is a flat bag: it carries what the framework already knew from the ancestry, and null elsewhere.
		out.t2_idsGrid = ctx.ids.gridId;
		out.t2_idsCard = ctx.ids.cardId;
		out.t2_idsView = ctx.ids.viewId;
		// SD-3: sectionId is on the shape and is null on a row-detail region.  A member whose value is null for one
		// region type is not a member that left.
		out.t2_idsSectionNullOnRowDetail = ctx.ids.sectionId === null;
		// declared.fields is a member of the contract here even though nothing populates it yet.
		out.t2_declaredFieldsNull = ctx.declared.fields === null;
		out.t2_declaredTitleFieldsNull = ctx.declared.titleFields === null;
	}

	// =================================================================================================================
	// Test 8a - fork() attachment mechanics: six assertions, because `fork` is a non-standard method on a standard
	// type and every one of these is a way to get it subtly wrong.
	// =================================================================================================================
	{
		const suffix = '';
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let ctx = null;
		R.register('capture', function (c) { ctx = c; });
		const el = H.mkRegion(env, { id: 'forker', type: 'card-body', populate: 'capture' });
		R.initRegion(el);

		// (i) It is a GENUINE platform signal, not a look-alike: instanceof holds, its own members behave normally,
		// and the real global fetch accepts it with no adapter.
		out['t8a_isRealSignal' + suffix] = ctx.signal instanceof AbortSignal;
		out['t8a_signalMembers' + suffix] = ctx.signal.aborted === false
			&& typeof ctx.signal.addEventListener === 'function'
			&& typeof ctx.signal.throwIfAborted === 'function';
		// Handed to the platform's OWN fetch, pre-aborted, so the assertion is "fetch consumed it as a signal" with
		// no socket opened: a look-alike rejects with TypeError, a genuine signal with AbortError.
		out['t8a_acceptedByRealFetch' + suffix] = await (async function () {
			const probe = ctx.signal.fork();
			ctx.signal.fork();   // supersedes `probe`, so it is aborted without tearing the region down
			try { await fetch('http://juneau.invalid/none', { signal: probe }); return 'RESOLVED'; }
			catch (e) { return e.name === 'AbortError'; }
		})();
		out['t8a_forkTypeof' + suffix] = typeof ctx.signal.fork;

		// (ii) `fork` is an OWN, non-enumerable, non-writable property of that instance.
		const d = Object.getOwnPropertyDescriptor(ctx.signal, 'fork');
		out['t8a_own' + suffix] = !!d;
		out['t8a_enumerable' + suffix] = d ? d.enumerable : null;
		out['t8a_writable' + suffix] = d ? d.writable : null;
		out['t8a_notInOwnKeys' + suffix] = Object.keys(ctx.signal).indexOf('fork') < 0;

		// (iii) AbortSignal.prototype.fork is undefined IN A PAGE THAT HAS LOADED THE REGION RUNTIME.  The lazy way
		// to implement fork() is exactly the prototype patch, which is why this is worth a test.
		out['t8a_notOnPrototype' + suffix] = AbortSignal.prototype.fork === undefined
			&& Object.getOwnPropertyDescriptor(AbortSignal.prototype, 'fork') === undefined;

		// (iv) A forked child is a plain AbortSignal with NO fork of its own - forking is a capability of the
		// invocation signal, not a chainable operation, so the tree stays exactly two levels deep.
		const g0 = ctx.generation;
		const sig1 = ctx.signal.fork();
		out['t8a_childIsSignal' + suffix] = sig1 instanceof AbortSignal;
		out['t8a_childHasNoFork' + suffix] = sig1.fork === undefined;

		// (vi) Forking again aborts the first child and increments ctx.generation.
		const sig2 = ctx.signal.fork();
		out['t8a_firstChildAborted' + suffix] = sig1.aborted === true;
		out['t8a_secondChildLive' + suffix] = sig2.aborted === false;
		out['t8a_generationIncremented' + suffix] = ctx.generation === g0 + 2;

		// (v) Aborting the parent aborts every live child.
		R.teardownRegion(el._juneauRegion);
		out['t8a_parentAbortKillsChild' + suffix] = sig2.aborted === true;
	}

	// =================================================================================================================
	// Test 3 - idempotent re-populate: two calls yield ONE copy of the content, not two.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let calls = 0;
		let ctx = null;
		R.register('once', function (c, container) {
			calls++;
			ctx = c;
			container.appendChild(env.el('span'));
		});
		const el = H.mkRegion(env, { id: 'idem', type: 'card-body', populate: 'once' });
		const handle = R.initRegion(el);
		// A second enrolment walk over the same node enrols NOTHING new and returns the same handle.
		const handle2 = R.initRegion(el);
		out.t3_sameHandle = handle === handle2;
		out.t3_callsAfterSecondInit = calls;
		// A re-populate clears first, so the content is one copy and not two.
		ctx.refresh();
		await H.flush();
		out.t3_callsAfterRefresh = calls;
		out.t3_spanCount = el.querySelectorAll('span').length;
	}

	// =================================================================================================================
	// Test 4 - a throwing populate is CONTAINED: the region errors, its siblings stay healthy, the page is alive.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		R.register('boom', function () { throw new Error('populate exploded'); });
		R.register('fine', function (ctx, container) { container.appendChild(env.el('b')); });
		const bad = H.mkRegion(env, { id: 'bad', type: 'card-body', populate: 'boom' });
		const good = H.mkRegion(env, { id: 'good', type: 'card-body', populate: 'fine' });
		R.initRegion(bad);
		R.initRegion(good);
		out.t4_badState = stateOf(bad);
		out.t4_goodState = stateOf(good);
		out.t4_goodHasContent = good.querySelectorAll('b').length === 1;
		out.t4_errorNamesRegion = rec.errorsMatching("region 'bad'").length === 1;
		out.t4_errorVisibleInContainer = (bad.textContent || '').indexOf('populate exploded') >= 0;

		// An unknown populator name fails VISIBLY too - a region has no raw value to fall back to, so a blank
		// container with no error would read as "no data" rather than "broken page".
		const unknown = H.mkRegion(env, { id: 'nope', type: 'card-body', populate: 'no-such-populator' });
		R.initRegion(unknown);
		out.t4_unknownState = stateOf(unknown);
		out.t4_unknownErrorNamesName = rec.errorsMatching("no populator is registered under the name 'no-such-populator'").length === 1;

		// The reserved default cannot be overridden, and the refusal mutates nothing.
		const before = R.resolve('default');
		const refused = R.register('default', function () {});
		out.t4_reservedRefused = refused === null && R.resolve('default') === before;
		out.t4_reservedErrorLogged = rec.errorsMatching('reserved populator name').length === 1;
	}

	// =================================================================================================================
	// Test 5 - a Promise-returning populate goes loading -> ok only AFTER the settle.
	// Test 6a - Promise<() => void>: the cleanup resolved from the promise is invoked exactly once at teardown.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const d = H.deferred();
		let cleanups = 0;
		R.register('slow', function (ctx, container) {
			container.appendChild(env.el('span'));
			return d.promise.then(function () { return function () { cleanups++; }; });
		});
		const el = H.mkRegion(env, { id: 'slow', type: 'tab-body', populate: 'slow' });
		R.initRegion(el);
		out.t5_stateWhileInFlight = stateOf(el);
		d.resolve();
		await H.flush();
		out.t5_stateAfterSettle = stateOf(el);
		out.t6a_cleanupsBeforeTeardown = cleanups;
		R.teardownRegion(el._juneauRegion);
		out.t6a_cleanupsAfterTeardown = cleanups;
		R.teardownRegion(el._juneauRegion);
		out.t6a_cleanupsAfterSecondTeardown = cleanups;

		// A REJECTED promise is a contained throw and registers NO cleanup: a populate that failed before it finished
		// allocating cannot be trusted to know what to release.
		let rejectedCleanups = 0;
		const d2 = H.deferred();
		R.register('rejects', function () {
			return d2.promise.then(function () { return function () { rejectedCleanups++; }; });
		});
		const el2 = H.mkRegion(env, { id: 'rej', type: 'card-body', populate: 'rejects' });
		R.initRegion(el2);
		d2.reject(new Error('fetch died'));
		await H.flush();
		out.t6a_rejectedState = stateOf(el2);
		R.teardownRegion(el2._juneauRegion);
		out.t6a_rejectedRegistersNoCleanup = rejectedCleanups === 0;
	}

	// =================================================================================================================
	// Test 6 + Test 7 - the cleanup function runs exactly once BEFORE the container is cleared, and the five teardown
	// steps run in their fixed order.  Written as an ORDER-RECORDING test, because a plausible refactor reorders it
	// silently and every individual step would still be observably "done".
	// =================================================================================================================
	{
		const { env, R, clock } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const order = [];
		let pollFired = false;
		let pollTimerNullAtAbort = null;
		let childrenAtCleanup = null;
		let stillSubscribedAtCleanup = null;
		let cleanups = 0;
		let received = [];

		R.register('recorder', function (ctx, container) {
			container.appendChild(env.el('span'));
			ctx.on(function (msg) { received.push(msg); });
			ctx.signal.addEventListener('abort', function () {
				order.push('abort');
				pollTimerNullAtAbort = el._juneauRegion.pollTimer === null;
			});
			return function () {
				order.push('cleanup');
				cleanups++;
				childrenAtCleanup = container.childNodes.length;
				// Step 3 (unsubscribe) has NOT run yet, so a sibling's emit still reaches this region's handler.
				const before = received.length;
				sibling.emit({ kind: 'during-cleanup' });
				stillSubscribedAtCleanup = received.length === before + 1;
			};
		});
		let sibling = null;
		R.register('sibling', function (ctx) { sibling = ctx; });
		const sibEl = H.mkRegion(env, { id: 'sib', type: 'card-body', populate: 'sibling' });
		R.initRegion(sibEl);
		const el = H.mkRegion(env, { id: 'rec', type: 'card-body', populate: 'recorder' });
		R.initRegion(el);

		// The declared poll timer is J0522c's to arm; the framework's own record of it is what step 0 clears, so the
		// test arms it on the framework's handle to assert the ordering step 0 exists for.
		el._juneauRegion.pollTimer = clock.set(function () { pollFired = true; }, 50);

		R.teardownRegion(el._juneauRegion);
		out.t7_order = order;
		out.t7_pollTimerClearedBeforeAbort = pollTimerNullAtAbort;
		out.t7_pollTimerCleared = el._juneauRegion === undefined;
		clock.advance(1000);
		out.t7_pollNeverFired = pollFired === false;
		out.t6_cleanupsExactlyOnce = cleanups === 1;
		out.t6_containerNotYetClearedAtCleanup = childrenAtCleanup > 0;
		out.t7_stillSubscribedAtCleanup = stillSubscribedAtCleanup;
		out.t7_containerClearedAfter = el.childNodes.length === 0;
		out.t7_markRemoved = stateOf(el) === null && el._juneauRegion === undefined;
		// Step 3 did run: after teardown the handler is unreachable from the bus even though the test's closure still
		// holds it.  This is the leak assertion.
		const beforeCount = received.length;
		sibling.emit({ kind: 'after-teardown' });
		out.t7_unsubscribedAfterTeardown = received.length === beforeCount;
	}

	// =================================================================================================================
	// Test 8 - ctx.signal aborts an in-flight fetch on collapse, and TRANSITIVELY aborts every fork() child and any
	// live messageSignal.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let ctx = null;
		let topLevelSignal = null;
		let forkSignal = null;
		R.register('fetcher', function (c) {
			ctx = c;
			topLevelSignal = c.signal;
			forkSignal = c.signal.fork();
		});
		const el = H.mkRegion(env, { id: 'f', type: 'card-body', populate: 'fetcher' });
		R.initRegion(el);
		out.t8_liveBeforeTeardown = topLevelSignal.aborted === false && forkSignal.aborted === false;
		R.teardownRegion(el._juneauRegion);
		out.t8_topLevelAborted = topLevelSignal.aborted === true;
		out.t8_forkChildAborted = forkSignal.aborted === true;
		out.t8_abortReasonNamesTeardown = String(topLevelSignal.reason).indexOf('torn-down') >= 0;
	}

	// =================================================================================================================
	// Test 9a - `reason` tracks the CAUSAL EVENT, not the API that was invoked.  ctx.refresh() from inside a bus
	// handler is "message"; the identical call from a timer is "refresh".
	// =================================================================================================================
	{
		const { env, R, clock } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const reasons = [];
		let ctx = null;
		let messageSignals = [];
		R.register('reasoner', function (c) {
			ctx = c;
			reasons.push(c.reason);
			messageSignals.push(c.messageSignal);
			c.on(function () { c.refresh(); });
		});
		let driver = null;
		R.register('driver', function (c) { driver = c; });
		R.initRegion(H.mkRegion(env, { id: 'drv', type: 'card-body', populate: 'driver' }));
		const el = H.mkRegion(env, { id: 'reasoner', type: 'card-body', populate: 'reasoner' });
		R.initRegion(el);
		clock.advance(0);
		await H.flush();
		out.t9a_initial = reasons[0];

		// Synchronously inside a fan-out -> "message".
		driver.emit({ kind: 'x' });
		await H.flush();
		out.t9a_fromHandler = reasons[1];
		out.t9a_messageSignalNonNullOnMessage = messageSignals[1] !== null
			&& messageSignals[1] instanceof AbortSignal;

		// The identical call from a macrotask -> "refresh".
		clock.set(function () { ctx.refresh(); }, 5);
		clock.advance(10);
		await H.flush();
		out.t9a_fromTimer = reasons[2];
		out.t9a_messageSignalNullOnRefresh = messageSignals[2] === null;

		// A second message aborts the previous delivery's messageSignal, so a region that re-populates per message
		// gets ordering for free.
		const priorMessageSignal = messageSignals[1];
		driver.emit({ kind: 'y' });
		await H.flush();
		out.t9a_priorMessageSignalAborted = priorMessageSignal.aborted === true;

		// A populate that calls refresh() unconditionally spins exactly once, not forever.
		let spins = 0;
		R.register('spinner', function (c) { spins++; c.refresh(); });
		const sp = H.mkRegion(env, { id: 'spin', type: 'card-body', populate: 'spinner' });
		R.initRegion(sp);
		await H.flush();
		out.t9a_selfRefreshSpinsOnce = spins;
	}

	// =================================================================================================================
	// A region in flight is NOT re-entered: a second trigger aborts the current invocation, waits for the settle, and
	// then runs once.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const d = H.deferred();
		const reasons = [];
		let first = true;
		let ctx = null;
		let firstSignal = null;
		R.register('coalescer', function (c) {
			ctx = c;
			reasons.push(c.reason);
			if (!first) return;
			first = false;
			firstSignal = c.signal;
			return d.promise;
		});
		const el = H.mkRegion(env, { id: 'co', type: 'card-body', populate: 'coalescer' });
		R.initRegion(el);
		ctx.refresh();
		ctx.refresh();
		ctx.refresh();
		out.t_coalesce_callsWhileInFlight = reasons.length;
		out.t_coalesce_inFlightSignalAborted = firstSignal.aborted === true;
		d.resolve();
		await H.flush();
		out.t_coalesce_callsAfterSettle = reasons.length;
		out.t_coalesce_reasons = reasons.slice();
	}

	// =================================================================================================================
	// enrolIn walks a scope synchronously; a hidden region is ENROLLED but not populated until it is activated, and
	// its first populate is "activate" and never "initial".  teardownRegionsIn sweeps a subtree idempotently.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const reasons = {};
		R.register('noter', function (ctx) { (reasons[ctx.id] = reasons[ctx.id] || []).push(ctx.reason); });
		const scope = env.el('div');
		env.body.appendChild(scope);
		const visible = H.mkRegion(env, { id: 'vis', type: 'card-body', populate: 'noter', parent: scope });
		const hidden = H.mkRegion(env, { id: 'hid', type: 'card-body', populate: 'noter', parent: scope, hiddenPanel: true });
		const handles = R.enrolIn(scope);
		out.t_enrol_count = handles.length;
		out.t_enrol_visiblePopulated = (reasons.vis || []).slice();
		out.t_enrol_hiddenNotPopulated = (reasons.hid || []).length === 0;
		out.t_enrol_hiddenIsEnrolled = hidden._juneauRegion !== undefined && hidden._juneauRegion !== null;
		out.t_enrol_hiddenState = stateOf(hidden);
		H.activatePanel(hidden);
		R.activateRegion(hidden._juneauRegion);
		out.t_enrol_hiddenReasonOnActivate = (reasons.hid || []).slice();
		// A second activation produces no second populate.
		R.activateRegion(hidden._juneauRegion);
		out.t_enrol_activateOnce = (reasons.hid || []).length === 1;

		R.teardownRegionsIn(scope);
		out.t_teardown_swept = visible._juneauRegion === undefined && hidden._juneauRegion === undefined;
		R.teardownRegionsIn(scope);
		out.t_teardown_idempotent = rec.errors.length === 0;

		// A marked node found DISCONNECTED is a loud named error rather than a silent no-op.
		const orphan = H.mkRegion(env, { id: 'orphan', type: 'card-body', populate: 'noter' });
		R.initRegion(orphan);
		orphan.remove();
		R.initRegion(orphan);
		out.t_orphan_errorLogged = rec.errorsMatching('removed without a').length === 1;
	}

	// =================================================================================================================
	// Test 50b (runtime half) - ctx.write from a NON-TABLE region carries the CSRF header, refuses a cross-origin URL,
	// refuses a safe method, issues zero network traffic on a refusal, aborts a prior in-flight write from the same
	// region, and does not touch the region's loading/ok/error state in any of those cases.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let ctx = null;
		R.register('writer', function (c) { ctx = c; });
		const host = env.el('div');   // a card grid, not a table: there is no table[data-juneau-csrf] to read from.
		host.setAttribute('data-juneau-csrf', 'tok-123');
		env.body.appendChild(host);
		const el = H.mkRegion(env, { id: 'w', type: 'card-body', populate: 'writer', parent: host });
		R.initRegion(el);

		const calls = [];
		const pending = [];
		env.setFetch(function (url, init) {
			calls.push({ url: url, init: init });
			const d = H.deferred();
			pending.push(d);
			return d.promise;
		});

		const stateBefore = stateOf(el);
		const p1 = ctx.write('/rest/x/act', { a: 1 }, { idempotencyKey: 'k-1' });
		out.t50b_headerStamped = calls[0].init.headers['X-Csrf-Token'] === 'tok-123';
		out.t50b_contentType = calls[0].init.headers['Content-Type'] === 'application/json';
		out.t50b_method = calls[0].init.method;
		out.t50b_idempotencyMerged = JSON.parse(calls[0].init.body).idempotencyKey === 'k-1';
		out.t50b_notBareCtxSignal = calls[0].init.signal !== ctx.signal;
		const firstSignal = calls[0].init.signal;

		// A second write from the same region aborts the first rather than racing it.
		const p2 = ctx.write('/rest/x/act', { a: 2 });
		out.t50b_priorWriteAborted = firstSignal.aborted === true;
		out.t50b_secondWriteLive = calls[1].init.signal.aborted === false;
		pending[0].resolve(H.jsonResponse({ ok: 1 }));
		pending[1].resolve(H.jsonResponse({ ok: 1 }, { status: 500 }));
		const r2 = await p2;
		out.t50b_nonTwoHundredResolvesNotRejects = r2.ok === false && r2.status === 500;
		await p1.catch(function () {});

		// Three refusals, each with a marker, a visible message and ZERO network traffic.
		const callsBefore = calls.length;
		const refusals = {};
		for (const [name, args] of Object.entries({
			safeMethod: ['/rest/x/act', {}, { method: 'GET' }],
			crossOrigin: ['https://evil.example/act', {}, {}],
			dotDot: ['/rest/../etc', {}, {}]
		})) {
			try { await ctx.write(...args); refusals[name] = 'RESOLVED'; }
			catch (e) { refusals[name] = e.reason; }
		}
		out.t50b_refusalReasons = refusals;
		out.t50b_refusalsIssuedNoTraffic = calls.length === callsBefore;
		out.t50b_refusalVisible = el.querySelectorAll('[data-juneau-region-write-error]').length === 3;
		out.t50b_stateUntouched = stateOf(el) === stateBefore && stateOf(el) === 'ok';

		// No token on the host -> missing-token refusal, never a silent unauthenticated send.
		const el2 = H.mkRegion(env, { id: 'w2', type: 'card-body', populate: 'writer' });
		R.initRegion(el2);
		const ctx2 = ctx;
		let missing = null;
		try { await ctx2.write('/rest/x/act', {}); } catch (e) { missing = e.reason; }
		out.t50b_missingTokenReason = missing;
	}

	// =================================================================================================================
	// Test 8b - THE AbortController BASELINE FAILS LOUD.  A page carrying three regions in an environment with no
	// AbortController populates NOTHING, shows the error state on each, logs EXACTLY ONE console error naming the
	// missing platform feature, and hands no region a ctx at all - so there is no ctx whose signal is undefined or a
	// polyfilled look-alike.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { noAbortController: true });
		let populates = 0;
		const seenSignals = [];
		R.register('never', function (ctx) { populates++; seenSignals.push(ctx && ctx.signal); });
		const els = ['a', 'b', 'c'].map(function (id) {
			return H.mkRegion(env, { id: id, type: 'card-body', populate: 'never' });
		});
		const handles = els.map(function (el) { return R.initRegion(el); });
		out.t8b_populates = populates;
		out.t8b_noHandles = handles.every(function (h) { return h === null; });
		out.t8b_allErrored = els.map(stateOf);
		out.t8b_exactlyOneError = rec.errorsMatching('requires AbortController').length === 1;
		out.t8b_totalErrors = rec.errors.length;
		out.t8b_noCtxHandedOut = seenSignals.length === 0;
		out.t8b_messageVisible = els.every(function (el) {
			return (el.textContent || '').indexOf('AbortController') >= 0;
		});
	}

	// =================================================================================================================
	// The exported surface: register / resolve / initRegion / ready are published on JuneauViews.regions, and the
	// contract version is the region envelope's own and not another envelope's.
	// =================================================================================================================
	{
		const { NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		out.tx_exports = ['register', 'resolve', 'initRegion', 'ready'].map(function (n) { return typeof R[n]; });
		out.tx_contractVersion = R.CONTRACT_VERSION;
		out.tx_contractVersionIndependent = R.CONTRACT_VERSION !== NS.CONTRACT_VERSION
			|| NS.CONTRACT_VERSION === undefined;
		out.tx_regionTypes = R.REGION_TYPES.slice();
		out.tx_reserved = R.RESERVED_POPULATOR_NAMES.slice();
		out.tx_caps = { queue: R.BUS_QUEUE_DEPTH_CAP, replay: R.REPLAY_BUFFER_CAP, deadline: R.BARRIER_DEADLINE_MS };
		out.tx_publishedOnSharedNamespace = NS.regions === R && typeof NS.init === 'object';
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
