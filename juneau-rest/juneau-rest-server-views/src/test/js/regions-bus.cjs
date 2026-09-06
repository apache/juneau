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
 * regions-bus.cjs - always-on Node harness for the region message bus: broadcast-by-default, opt-in targeted
 * delivery, payload opacity, per-subscriber error isolation, subscription-ordered synchronous delivery with
 * emit-during-delivery queued under an independent depth cap, unsubscribe both returned and swept at teardown, the
 * closed set of framework-emitted message schemas, and SD-2's emit-ownership invariant in both directions.
 *
 *   Usage:  node regions-bus.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node regions-bus.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

/**
 * Builds a page of subscriber regions, each recording what it receives.  Returns the ctx of each by id, plus the
 * shared receive log.  Everything here uses ONLY ctx.emit / ctx.on - a region never sees the bus itself.
 */
function page(h, opts) {
	const { env, R, rec, clock } = h;
	const ctxs = {};
	const received = {};
	const order = [];
	R.register('sub', function (ctx) {
		ctxs[ctx.id] = ctx;
		received[ctx.id] = [];
		ctx.on(function (msg, meta) {
			received[ctx.id].push({ msg: msg, meta: meta });
			order.push(ctx.id);
			if (opts && opts.onMessage) opts.onMessage(ctx, msg, meta);
		});
	});
	R.register('silent', function (ctx) { ctxs[ctx.id] = ctx; received[ctx.id] = []; });
	return { env: env, R: R, rec: rec, clock: clock, ctxs: ctxs, received: received, order: order };
}

function mk(p, id, opts) {
	const el = H.mkRegion(p.env, Object.assign({ id: id, type: 'card-body', populate: 'sub' }, opts || {}));
	p.R.initRegion(el);
	return el;
}

function kinds(list) {
	return list.map(function (r) { return r.msg.kind; });
}

(async function () {

	// =================================================================================================================
	// Test 17 - broadcast reaches ALL subscribers but NOT the emitter.  A self-reflecting control cannot learn its own
	// state from the bus and must keep it in local state.
	// =================================================================================================================
	{
		const p = page(H.load(rendersJsPath, viewsJsPath, regionsJsPath));
		mk(p, 'a'); mk(p, 'b'); mk(p, 'c');
		p.ctxs.a.emit({ kind: 'ping' });
		out.t17_a = kinds(p.received.a);
		out.t17_b = kinds(p.received.b);
		out.t17_c = kinds(p.received.c);
		out.t17_metaFrom = p.received.b[0].meta.from;
		out.t17_metaToNullOnBroadcast = p.received.b[0].meta.to === null;
		// Synchronous: by the time emit() returns, every subscriber has already run.
		out.t17_synchronous = p.received.b.length === 1;
	}

	// =================================================================================================================
	// Test 18 - targeted delivery reaches EXACTLY one, and a short id and a fully-qualified key both resolve.
	// Test 19 - an unknown target is a no-op plus a warning, never a throw.
	// =================================================================================================================
	{
		const p = page(H.load(rendersJsPath, viewsJsPath, regionsJsPath));
		mk(p, 'a', { host: 'grid' }); mk(p, 'b', { host: 'grid' }); mk(p, 'c', { host: 'grid' });
		// Short id, resolved within the emitter's own host.
		p.ctxs.a.emit({ kind: 'short' }, { to: 'b' });
		out.t18_shortB = kinds(p.received.b);
		out.t18_shortC = kinds(p.received.c);
		out.t18_metaTo = p.received.b[0].meta.to;
		// Fully-qualified key.
		p.ctxs.a.emit({ kind: 'qualified' }, { to: 'grid/c' });
		out.t18_qualifiedC = kinds(p.received.c);
		out.t18_qualifiedB = kinds(p.received.b);
		out.t18_keys = [p.ctxs.a.key, p.ctxs.b.key, p.ctxs.c.key];

		let threw = false;
		try { p.ctxs.a.emit({ kind: 'nowhere' }, { to: 'does-not-exist' }); } catch (e) { threw = true; }
		out.t19_noThrow = threw === false;
		out.t19_warned = p.rec.warnsMatching("no region matches emit target 'does-not-exist'").length === 1;
		out.t19_nobodyGotIt = kinds(p.received.b).indexOf('nowhere') < 0 && kinds(p.received.c).indexOf('nowhere') < 0;

		// A region addressing ITSELF is refused with a warning rather than delivered.
		p.ctxs.a.emit({ kind: 'selfie' }, { to: 'a' });
		out.t19_selfAddressWarned = p.rec.warnsMatching('addressed itself').length === 1;
		out.t19_selfAddressNotDelivered = kinds(p.received.a).length === 0;
	}

	// =================================================================================================================
	// Test 20 - PAYLOAD OPACITY: a getter-trapped payload arrives with ZERO recorded property reads.  The framework
	// moves the object reference and never reads a property of it - no normalizing, no kind-based routing, no cloning,
	// no serialization - so payload IDENTITY is shared with every subscriber.
	// =================================================================================================================
	{
		const p = page(H.load(rendersJsPath, viewsJsPath, regionsJsPath));
		mk(p, 'a'); mk(p, 'b'); mk(p, 'c');
		const reads = [];
		const raw = { kind: 'trapped', payload: 1 };
		const trapped = new Proxy(raw, {
			get: function (t, k) { reads.push(String(k)); return t[k]; },
			has: function (t, k) { reads.push('has:' + String(k)); return k in t; },
			ownKeys: function (t) { reads.push('ownKeys'); return Reflect.ownKeys(t); }
		});
		p.ctxs.a.emit(trapped);
		out.t20_reads = reads.slice();
		out.t20_zeroReads = reads.length === 0;
		out.t20_identityShared = p.received.b[0].msg === trapped && p.received.c[0].msg === trapped;

		// ...and a TARGETED emit reads nothing either: routing is done from `opts`, never from the payload.
		reads.length = 0;
		p.ctxs.a.emit(trapped, { to: 'b' });
		out.t20_zeroReadsTargeted = reads.length === 0;
	}

	// =================================================================================================================
	// Test 21 - a THROWING subscriber does not block the rest of the fan-out, and is reported.  A repeatedly throwing
	// subscriber is deliberately NOT auto-unsubscribed: silently dropping it turns a loud bug into a mysterious one.
	// =================================================================================================================
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const p = page(h, {
			onMessage: function (ctx) { if (ctx.id === 'b') throw new Error('subscriber exploded'); }
		});
		mk(p, 'a'); mk(p, 'b'); mk(p, 'c');
		p.ctxs.a.emit({ kind: 'm1' });
		out.t21_cStillGotIt = kinds(p.received.c);
		out.t21_reported = p.rec.errorsMatching("subscriber 'b' threw handling a message from 'a'").length === 1;
		// Not auto-unsubscribed: it throws again on the next message, and c still receives that one too.
		p.ctxs.a.emit({ kind: 'm2' });
		out.t21_stillSubscribed = kinds(p.received.b);
		out.t21_cGotBoth = kinds(p.received.c);
		out.t21_reportedTwice = p.rec.errorsMatching("subscriber 'b' threw").length === 2;
	}

	// =================================================================================================================
	// Test 22 - delivery order is SUBSCRIPTION order.
	// =================================================================================================================
	{
		const p = page(H.load(rendersJsPath, viewsJsPath, regionsJsPath));
		mk(p, 'z'); mk(p, 'm'); mk(p, 'a'); mk(p, 'driver');
		p.ctxs.driver.emit({ kind: 'ordered' });
		out.t22_order = p.order.slice();
	}

	// =================================================================================================================
	// Test 23 - an emit DURING delivery is QUEUED, not nested, asserted by CALL-ORDER RECORDING rather than by
	// absence-of-crash.  A control card that emits its initial filter state during its own paint cannot re-enter a
	// listener mid-paint.
	// =================================================================================================================
	{
		const trace = [];
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const p = page(h, {
			onMessage: function (ctx, msg) {
				trace.push('enter:' + ctx.id + ':' + msg.kind);
				if (ctx.id === 'b' && msg.kind === 'm1') ctx.emit({ kind: 'm2' });
				trace.push('exit:' + ctx.id + ':' + msg.kind);
			}
		});
		mk(p, 'b'); mk(p, 'c'); mk(p, 'driver');
		p.ctxs.driver.emit({ kind: 'm1' });
		out.t23_trace = trace.slice();
		// The nested-delivery signature would be enter:b:m1, enter:c:m2, exit:c:m2, exit:b:m1.  The queued signature
		// is every m1 handler completing before any m2 handler starts.
		out.t23_notNested = trace.indexOf('exit:b:m1') < trace.indexOf('enter:c:m2');
		out.t23_m1FanOutCompletedFirst = trace.indexOf('enter:c:m1') < trace.indexOf('enter:c:m2');
		out.t23_cGot = kinds(p.received.c);
	}

	// =================================================================================================================
	// Test 24 - an emit cycle hits the depth cap and errors loudly, PRINTING THE CYCLE - the shortest repeating
	// (from -> to) suffix - not an arbitrary pair of keys.  Asserted with a THREE-region cycle as well as a two-region
	// one, since the two-region case cannot distinguish "prints the cycle" from "prints the last two hops".
	// =================================================================================================================
	for (const n of [2, 3]) {
		const ring = n === 2 ? { a: 'b', b: 'a' } : { a: 'b', b: 'c', c: 'a' };
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const p = page(h, {
			onMessage: function (ctx) { ctx.emit({ kind: 'loop' }, { to: ring[ctx.id] }); }
		});
		for (const id of Object.keys(ring)) mk(p, id);
		p.ctxs.a.emit({ kind: 'loop' }, { to: ring.a });
		const errs = p.rec.errorsMatching('emit cycle detected');
		out['t24_' + n + '_exactlyOneError'] = errs.length === 1;
		out['t24_' + n + '_terminated'] = true;   // reaching this line at all means the drain terminated
		const cycle = errs.length ? errs[0].slice(errs[0].indexOf('Cycle: ') + 7) : '';
		out['t24_' + n + '_cycle'] = cycle;
		out['t24_' + n + '_hopCount'] = cycle.split('|').length;
		out['t24_' + n + '_namesEveryRegion'] = Object.keys(ring).every(function (id) { return cycle.indexOf(id) >= 0; });
	}

	// =================================================================================================================
	// Test 25 - UNSUBSCRIBE-ON-TEARDOWN, the leak test: a torn-down region receives nothing even though its handler is
	// still reachable from the test's own closure.
	// Test 26 - ctx.on's returned unsubscribe works independently of teardown.
	// =================================================================================================================
	{
		const p = page(H.load(rendersJsPath, viewsJsPath, regionsJsPath));
		const torn = mk(p, 'torn');
		mk(p, 'kept');
		mk(p, 'driver');
		const handler = p.received.torn;   // the test's closure still holds the array the handler pushes into
		p.ctxs.driver.emit({ kind: 'before' });
		out.t25_beforeTeardown = kinds(handler);
		p.R.teardownRegion(torn._juneauRegion);
		p.ctxs.driver.emit({ kind: 'after' });
		out.t25_afterTeardown = kinds(handler);
		out.t25_siblingUnaffected = kinds(p.received.kept);

		// The returned unsubscribe is independent of teardown.
		let got = [];
		let off = null;
		p.R.register('selfmanaged', function (ctx) { off = ctx.on(function (m) { got.push(m.kind); }); });
		const sm = H.mkRegion(p.env, { id: 'sm', type: 'card-body', populate: 'selfmanaged' });
		p.R.initRegion(sm);
		p.ctxs.driver.emit({ kind: 'one' });
		off();
		p.ctxs.driver.emit({ kind: 'two' });
		out.t26_receivedBeforeOff = got.slice();
		out.t26_regionStillLive = sm._juneauRegion !== undefined;
		out.t26_stateStillOk = sm.getAttribute('data-juneau-region-state');
	}

	// =================================================================================================================
	// Teardown step 4 - a message a region QUEUED but that has not been delivered yet is DISCARDED: a region that is
	// going away should not drive the page after it is gone.
	// =================================================================================================================
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let toTearDown = null;
		const p = page(h, {
			onMessage: function (ctx, msg) {
				if (ctx.id === 'b' && msg.kind === 'm1') ctx.emit({ kind: 'from-b' });
				if (ctx.id === 'a' && msg.kind === 'm1') p.R.teardownRegion(toTearDown._juneauRegion);
			}
		});
		toTearDown = mk(p, 'b');   // subscribes FIRST, so it queues before a tears it down in the same fan-out
		mk(p, 'a');
		mk(p, 'c');
		mk(p, 'driver');
		p.ctxs.driver.emit({ kind: 'm1' });
		out.t_step4_cGot = kinds(p.received.c);
		out.t_step4_queuedMessageDropped = kinds(p.received.c).indexOf('from-b') < 0;
	}

	// =================================================================================================================
	// Test 27 / 27a / 27c / 51 - the framework-emitted set is SMALL and CLOSED: selection changed, detail toggled,
	// table redrew, and nothing else.  27a/27c are the CHROME half of SD-2's ownership invariant: a region beside the
	// chrome is driven with NO author emit code at all - only a ctx.on plus a msg.viewId filter.
	// =================================================================================================================
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const R = h.R;
		const seen = [];
		let mine = [];
		R.register('chart', function (ctx) {
			// The entire consumer-side contract for a chrome-driven region: subscribe, filter on viewId.  No emit.
			ctx.on(function (msg) {
				seen.push(msg);
				if (msg.viewId === 'gacks') mine.push(msg.kind);
			});
		});
		R.initRegion(H.mkRegion(h.env, { id: 'chart', type: 'card-body', populate: 'chart' }));

		// 27a - selection changed.  Emitted BY THE FRAMEWORK, so it reaches every subscriber including one whose own
		// chrome caused it: meta.from names the framework rather than a region.
		R.emitFramework(R.selectionChangedMessage({ viewId: 'gacks', ids: ['42'], rows: [{ id: '42' }], added: ['42'], removed: [] }));
		// 27c - detail expand/collapse.
		R.emitFramework(R.detailToggledMessage({ viewId: 'gacks', rowId: '42', expanded: true, generation: 7 }));
		// table redrew.
		R.emitFramework(R.tableRedrewMessage({ viewId: 'gacks', nested: false, rowCount: 25, page: 0 }));

		out.t27_kinds = seen.map(function (m) { return m.kind; });
		out.t27_allNamespaced = seen.every(function (m) { return m.kind.indexOf('juneau:') === 0; });
		out.t27_senderIsFramework = R.FRAMEWORK_SENDER_KEY;
		out.t27a_drivenWithNoAuthorEmit = mine.slice();

		// 51 - a golden per framework message: kind including the prefix, schemaVersion, and the full field set.
		out.t51_selectionKeys = Object.keys(seen[0]).sort();
		out.t51_selection = seen[0];
		out.t51_detailKeys = Object.keys(seen[1]).sort();
		out.t51_detail = seen[1];
		out.t51_redrewKeys = Object.keys(seen[2]).sort();
		out.t51_redrew = seen[2];
		out.t51_schemaVersionsIndependentOfContract = seen.every(function (m) { return m.schemaVersion === 1; })
			&& R.CONTRACT_VERSION === '1';

		// 51 - the DISAMBIGUATION case: two tables on one page, each emitting selection, and a subscriber correctly
		// filtering on msg.viewId.  Without viewId on the payload this is unanswerable, because meta.from names the
		// framework and there is no host for a short target to resolve against.
		mine = [];
		R.emitFramework(R.selectionChangedMessage({ viewId: 'gacks', ids: ['1'] }));
		R.emitFramework(R.selectionChangedMessage({ viewId: 'other', ids: ['2'] }));
		out.t51_filteredByViewId = mine.length === 1;
		out.t51_viewIdsSeen = seen.slice(3).map(function (m) { return m.viewId; });
	}

	// =================================================================================================================
	// Test 27b - CONTENT DOES NOT AUTO-EMIT (SD-2 / R25b).  The negative test, and the one that keeps someone from
	// later adding a "helpful" click delegate over author DOM and quietly breaking the ownership invariant.  A
	// populate paints clickable elements, calls no emit, and clicking them produces ZERO bus traffic.
	// =================================================================================================================
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const p = page(h);
		mk(p, 'watcher');
		const buttons = [];
		h.R.register('painter', function (ctx, container) {
			for (const label of ['one', 'two', 'three']) {
				const b = h.env.el('button');
				b.textContent = label;
				b.setAttribute('data-author-action', label);
				// The author's OWN listener - the framework must not add one of its own beside it.
				b.addEventListener('click', function () { b.setAttribute('data-clicked', '1'); });
				container.appendChild(b);
				buttons.push(b);
			}
		});
		const el = H.mkRegion(h.env, { id: 'content', type: 'card-body', populate: 'painter' });
		h.R.initRegion(el);

		for (const b of buttons) b.dispatch('click');
		out.t27b_authorHandlersRan = buttons.every(function (b) { return b.getAttribute('data-clicked') === '1'; });
		out.t27b_zeroBusTraffic = p.received.watcher.length === 0;
		// The framework bound NO listener of its own inside the region container: every listener on every painted
		// node is the author's single click handler.
		out.t27b_listenerCounts = buttons.map(function (b) { return Object.keys(b._listeners).map(function (k) { return k + ':' + b._listeners[k].length; }).join(','); });
		out.t27b_containerHasNoFrameworkListener = Object.keys(el._listeners).length === 0;
		// ...and the region container itself never gained a delegate on the way in or out.
		el.dispatch('click');
		out.t27b_stillZeroAfterContainerClick = p.received.watcher.length === 0;
	}

	// =================================================================================================================
	// Test 50 - OUT-OF-ORDER SETTLE.  Two rapid messages to an update-in-place subscriber, the first response
	// artificially delayed past the second: the container holds the SECOND message's data and the first fetch's signal
	// was aborted.  Written with deterministic promise ordering, not timers.  The handler uses ctx.signal.fork() and
	// never the bare ctx.signal - the bare signal is aborted by nothing in this mode, so an unbounded number of
	// fetches would share one signal and the last to settle would win the paint.
	// =================================================================================================================
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const R = h.R;
		let container = null;
		const pending = H.abortableFetch(h.env);
		const signals = pending;
		// The page's own fetch, so the harness sees what the populate issued.  A region author writes plain
		// `fetch(url, {signal: sig})`; only the indirection differs here.
		const pageFetch = function (url, init) { return h.env.callFetch(url, init); };
		R.register('inplace', function (ctx, el) {
			container = el;
			ctx.on(function (msg) {
				const sig = ctx.signal.fork();
				pageFetch('/probe/' + msg.probe, { signal: sig })
					.then(function (res) { return res.text(); })
					.then(function (text) { el.textContent = text; })
					.catch(function (e) { if (e.name !== 'AbortError') throw e; });
			});
		});
		let driver = null;
		R.register('driver', function (ctx) { driver = ctx; });
		R.initRegion(H.mkRegion(h.env, { id: 'ip', type: 'card-body', populate: 'inplace' }));
		R.initRegion(H.mkRegion(h.env, { id: 'drv', type: 'card-body', populate: 'driver' }));

		driver.emit({ kind: 'probe-selected', probe: 1 });
		driver.emit({ kind: 'probe-selected', probe: 2 });
		out.t50_twoFetches = pending.length === 2;
		out.t50_firstSignalAborted = signals[0].init.signal.aborted === true;
		out.t50_secondSignalLive = signals[1].init.signal.aborted === false;
		// Settle in the WRONG order: the second response first, then the stale first one - which the abort has
		// already made unsettleable, which is the whole point of forking.
		out.t50_secondSettled = pending.resolveText(1, 'probe-2');
		await H.flush();
		out.t50_staleFirstCannotSettle = pending.resolveText(0, 'probe-1') === false;
		await H.flush();
		out.t50_containerHoldsSecond = container.textContent;
		out.t50_noRegionError = h.rec.errors.length === 0;

		// Three rapid clicks with responses returning in the order 3, 1, 2 - the flagship shape.  The panel holds the
		// LAST REQUESTED probe, the two superseded signals are aborted, and no AbortError ever surfaced as a region
		// error (the calendar-idiom swallow).
		driver.emit({ kind: 'probe-selected', probe: 3 });
		driver.emit({ kind: 'probe-selected', probe: 4 });
		driver.emit({ kind: 'probe-selected', probe: 5 });
		out.t50a_lastSettles = pending.resolveText(4, 'probe-5');
		await H.flush();
		out.t50a_supersededCannotSettle = [pending.resolveText(2, 'probe-3'), pending.resolveText(3, 'probe-4')];
		await H.flush();
		out.t50a_holdsLast = container.textContent;
		out.t50a_earlierSignalsAborted = [2, 3, 4].map(function (i) { return signals[i].init.signal.aborted; });
		out.t50a_noAbortErrorSurfaced = h.rec.errors.length === 0;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
