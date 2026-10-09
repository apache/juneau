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
 * bus-bridge.cjs - Node harness for the bridge runtime (spec §11.5, §11.6): JuneauViews.bus.attachSource with
 * hand-driven test-double sources and a fake clock. Reports source validation (E-JS-52), grant enforcement
 * (E-JS-54), the bridge:<id> state machine, backoff and its reset, Retry-After, resync clearing, upstream
 * forwarding (E-JS-55), detach, maxAttempts and capability give-up (E-JS-57). Every assertion lives in
 * ViewsJs_BusBridge_Test.
 *
 *   Usage:  node bus-bridge.cjs <juneau-bus.js>
 */
'use strict';

const path = require('node:path');
const { loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));
const { fakeClock } = require(path.join(__dirname, 'bus-fake-clock.cjs'));

const busJs = process.argv[2];
if (!busJs) {
	console.error('usage: node bus-bridge.cjs <juneau-bus.js>');
	process.exit(2);
}

function fresh() {
	const { NS } = loadScripts([busJs]);
	const bus = NS.bus;
	const clock = fakeClock();
	bus.config.timers = clock.timers;
	const errors = [];
	bus.onError(function (e) {
		errors.push({ code: e.code, message: e.message, banner: !!(e.detail && e.detail.banner), from: e.detail ? e.detail.from : null });
	});
	['app.a', 'app.b'].forEach(function (t) { bus.declare(t, { retain: true }); });
	['app.ev', 'app.up'].forEach(function (t) { bus.declare(t, { retain: false }); });
	return { bus: bus, clock: clock, errors: errors };
}

function watch(bus, id) {
	const seen = [];
	bus.subscribe('bridge:' + id, function (s) {
		seen.push(s ? { state: s.state, attempt: s.attempt, nextRetryMs: s.nextRetryMs, gap: s.gap, error: s.error ? s.error.code : null }
			: { cleared: true });
	});
	return seen;
}

/** A hand-driven source: the harness calls src.ctx.* to play the server. */
function manual(id, extra) {
	const src = Object.assign({
		id: id, downstream: ['app.a', 'app.b', 'app.ev', 'ns.items:*', 'cmd:t1'],
		connects: 0, closes: 0, sent: [], ctx: null,
		connect: function (ctx) {
			src.connects++;
			src.ctx = ctx;
			return { close: function () { src.closes++; }, send: function (f) { src.sent.push(f); } };
		}
	}, extra || {});
	return src;
}

function open(src) { src.ctx.resyncBegin(); src.ctx.resyncEnd(); }
function drop(src, info) { src.ctx.state('closed', Object.assign({ retryable: true }, info || {})); }
function codes(errors) { return errors.map(function (e) { return e.code; }); }
function last(seen) { return seen[seen.length - 1]; }

const out = {};

// --- validation (E-JS-52) ---
(function () {
	const { bus, errors } = fresh();
	bus.attachSource(null);
	const sx = watch(bus, 'x');
	bus.attachSource({ id: 'x', downstream: ['app.a'] });
	bus.attachSource({ id: 'fw', downstream: ['selection:t1'], connect: function () { return { close: function () {} }; } });
	bus.attachSource({ id: 'ov', downstream: ['app.a'], upstream: ['app.a'], connect: function () { return { close: function () {} }; } });
	const d1 = manual('d');
	bus.attachSource(d1);
	bus.attachSource(manual('d'));
	const sn = watch(bus, 'nosend');
	bus.attachSource({ id: 'nosend', downstream: ['app.a'], upstream: ['app.up'], connect: function () { return { close: function () {} }; } });
	out.validation = {
		codes: codes(errors),
		messages: errors.map(function (e) { return e.message; }),
		banners: errors.map(function (e) { return e.banner; }),
		xState: last(sx).state,
		firstDStillConnecting: bus.get('bridge:d').state,
		dConnects: d1.connects,
		noSendState: last(sn).state
	};
})();

// --- grants and meta (E-JS-54) ---
(function () {
	const { bus, errors } = fresh();
	const metas = [];
	bus.subscribe('app.a', function (p, m) { if (p) metas.push({ from: m.from, bridge: m.bridge }); });
	const g = manual('g');
	bus.attachSource(g);
	open(g);
	g.ctx.publish('app.zzz', { n: 1 }, { retained: true });
	g.ctx.publish('app.a', { n: 1 }, { retained: true });
	g.ctx.publish('ns.items:k1', { n: 2 }, { retained: false });
	out.grants = { codes: codes(errors), message: errors.length ? errors[0].message : null, a: bus.get('app.a'), metas: metas,
		zzz: bus.get('app.zzz') === undefined, state: bus.get('bridge:g').state };
})();

// --- state machine and gap ---
(function () {
	const { bus, clock } = fresh();
	const seen = watch(bus, 's');
	const s = manual('s');
	bus.attachSource(s);
	open(s);
	drop(s);
	clock.advance(1000);
	open(s);
	out.lifecycle = { seen: seen, connects: s.connects };
})();

// --- backoff schedule ---
(function () {
	const { bus, clock } = fresh();
	const u = bus.util;
	out.backoffTable = [1, 2, 3, 4, 5, 6, 7, 8].map(function (n) { return u.backoffDelay(n, 0.5); });
	out.backoffBounds = { low: u.backoffDelay(3, 0), high: u.backoffDelay(3, 0.999999) };
	const seen = watch(bus, 'b');
	const b = manual('b');
	bus.attachSource(b);
	const delays = [];
	for (let i = 0; i < 7; i++) {
		drop(b);
		delays.push(last(seen).nextRetryMs);
		clock.advance(last(seen).nextRetryMs);
	}
	out.backoffRuntime = delays;
	clock.random = 0;
	drop(b);
	out.backoffJitterLow = last(seen).nextRetryMs;
})();

// --- attempt reset after 30 s open, and no reset before ---
(function () {
	function run(openMs) {
		const { bus, clock } = fresh();
		const seen = watch(bus, 'r');
		const r = manual('r');
		bus.attachSource(r);
		for (let i = 0; i < 3; i++) { drop(r); clock.advance(last(seen).nextRetryMs); }
		open(r);
		clock.advance(openMs);
		drop(r);
		return { attempt: last(seen).attempt, nextRetryMs: last(seen).nextRetryMs };
	}
	out.reset = { after30s: run(30000), after10s: run(10000) };
})();

// --- Retry-After ---
(function () {
	const { bus } = fresh();
	const seen = watch(bus, 'ra');
	const ra = manual('ra');
	bus.attachSource(ra);
	drop(ra, { retryAfterMs: 10000 });
	const big = last(seen).nextRetryMs;
	const { bus: bus2 } = fresh();
	const seen2 = watch(bus2, 'ra');
	const ra2 = manual('ra');
	bus2.attachSource(ra2);
	drop(ra2, { retryAfterMs: 500 });
	out.retryAfter = { big: big, small: last(seen2).nextRetryMs };
})();

// --- resync clears retained topics the server no longer has ---
(function () {
	const { bus, clock } = fresh();
	let aCalls = 0;
	bus.subscribe('app.a', function () { aCalls++; });
	const s = manual('rs');
	bus.attachSource(s);
	s.ctx.resyncBegin();
	s.ctx.publish('app.a', { v: 1 }, { retained: true });
	s.ctx.publish('app.b', { v: 2 }, { retained: true });
	s.ctx.resyncEnd();
	const callsAfterFirst = aCalls;
	drop(s);
	clock.advance(1000);
	s.ctx.resyncBegin();
	s.ctx.publish('app.a', { v: 1 }, { retained: true });
	s.ctx.resyncEnd();
	out.resync = { a: bus.get('app.a'), bCleared: bus.get('app.b') === undefined, callsAfterFirst: callsAfterFirst,
		callsAfterResync: aCalls, gap: bus.get('bridge:rs').gap };
})();

// --- upstream forwarding (E-JS-55) ---
(function () {
	const { bus, errors } = fresh();
	const u = manual('u', { downstream: ['app.a'], upstream: ['app.up'] });
	bus.attachSource(u);
	bus.publish('app.up', { x: 0 });
	const sentWhileConnecting = u.sent.length;
	open(u);
	bus.publish('app.up', { x: 1 });
	out.upstream = { codes: codes(errors), message: errors.length ? errors[0].message : null, from: errors.length ? errors[0].from : null,
		sentWhileConnecting: sentWhileConnecting, sent: u.sent };
})();

// --- detach ---
(function () {
	const { bus } = fresh();
	const seen = watch(bus, 'dt');
	const s = manual('dt');
	const link = bus.attachSource(s);
	s.ctx.resyncBegin();
	s.ctx.publish('app.a', { v: 1 }, { retained: true });
	s.ctx.resyncEnd();
	link.detach();
	link.detach();
	s.ctx.publish('app.a', { v: 9 }, { retained: true });
	out.detach = { aCleared: bus.get('app.a') === undefined, bridgeCleared: bus.get('bridge:dt') === undefined, closes: s.closes,
		tail: seen.slice(-2) };
})();

// --- maxAttempts (E-JS-57) ---
(function () {
	const { bus, clock, errors } = fresh();
	const seen = watch(bus, 'm');
	const m = manual('m', { maxAttempts: 2 });
	bus.attachSource(m);
	for (let i = 0; i < 3; i++) { drop(m); clock.advance(60000); }
	out.maxAttempts = { state: last(seen).state, connects: m.connects, codes: codes(errors), message: errors.length ? errors[0].message : null,
		banner: errors.length ? errors[0].banner : null };
})();

// --- capability refused three times (E-JS-57), and the counter resetting on open ---
(function () {
	const { bus, clock, errors } = fresh();
	const seen = watch(bus, 'c');
	const c = manual('c');
	bus.attachSource(c);
	const delays = [];
	for (let i = 0; i < 2; i++) { drop(c, { capability: true }); delays.push(last(seen).nextRetryMs); clock.advance(0); }
	drop(c, { capability: true });
	const { bus: bus2, clock: clock2 } = fresh();
	const seen2 = watch(bus2, 'c');
	const c2 = manual('c');
	bus2.attachSource(c2);
	drop(c2, { capability: true }); clock2.advance(0);
	drop(c2, { capability: true }); clock2.advance(0);
	open(c2);
	drop(c2, { capability: true });
	out.capability = { delays: delays, state: last(seen).state, connects: c.connects, codes: codes(errors),
		message: errors.length ? errors[0].message : null, resetState: last(seen2).state };
})();

// --- non-retryable, quiet close, per-message error, stale ctx ---
(function () {
	const { bus, errors } = fresh();
	const seen = watch(bus, 'n');
	const n = manual('n');
	bus.attachSource(n);
	drop(n, { retryable: false, error: { code: 'E-JS-53', message: 'session refused (403): bus:topic-denied' } });
	out.nonRetryable = { state: last(seen).state, error: last(seen).error, errors: errors.slice() };
})();
(function () {
	const { bus, errors } = fresh();
	const seen = watch(bus, 'q');
	const q = manual('q');
	bus.attachSource(q);
	drop(q, { retryable: false, quiet: true, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: replaced" } });
	out.quiet = { state: last(seen).state, errors: errors.length };
})();
(function () {
	const { bus, clock, errors } = fresh();
	const p = manual('p');
	bus.attachSource(p);
	open(p);
	p.ctx.error('E-JS-59', 'server error bus:boom: kaput');
	const staleCtx = p.ctx;
	drop(p);
	clock.advance(1000);
	staleCtx.publish('app.a', { stale: true }, { retained: true });
	out.perMessage = { codes: codes(errors), message: errors[0].message, banner: errors[0].banner,
		staleIgnored: bus.get('app.a') === undefined, state: bus.get('bridge:p').state };
})();

// --- retain-declared topics are tracked even when the source omits {retained: true} ---
(function () {
	const { bus, clock } = fresh();
	const s = manual('rd');
	bus.attachSource(s);
	s.ctx.resyncBegin();
	s.ctx.publish('app.a', { v: 1 });
	s.ctx.publish('app.b', { v: 2 });
	s.ctx.resyncEnd();
	drop(s);
	clock.advance(1000);
	s.ctx.resyncBegin();
	s.ctx.publish('app.a', { v: 1 });
	s.ctx.resyncEnd();
	const bCleared = bus.get('app.b') === undefined;
	const aKept = bus.get('app.a') !== undefined;
	out.declaredRetain = { bCleared: bCleared, aKept: aKept };
	out.declaredRetain.aStillThere = bus.get('app.a') !== undefined;
	// detach clears a value published without the flag
	const d = manual('rd3', { downstream: ['app.zz'] });
	bus.declare('app.zz', { retain: true });
	const link = bus.attachSource(d);
	d.ctx.publish('app.zz', { v: 1 });
	const before = bus.get('app.zz') !== undefined;
	link.detach();
	out.declaredRetain.detach = { before: before, after: bus.get('app.zz') === undefined };
})();

// --- the 30 s open-reset republishes bridge:<id> ---
(function () {
	const { bus, clock } = fresh();
	const seen = watch(bus, 'rp');
	const r = manual('rp');
	bus.attachSource(r);
	drop(r);
	clock.advance(last(seen).nextRetryMs);
	open(r);
	const before = last(seen).attempt;
	clock.advance(30000);
	out.resetRepublish = { before: before, after: last(seen).attempt, state: last(seen).state, stored: bus.get('bridge:rp').attempt };
})();

// --- detach while reconnecting cancels the retry ---
(function () {
	const { bus, clock } = fresh();
	const seen = watch(bus, 'dr');
	const r = manual('dr');
	const link = bus.attachSource(r);
	open(r);
	drop(r);
	const stateBefore = last(seen).state;
	link.detach();
	clock.advance(120000);
	out.detachReconnecting = { stateBefore: stateBefore, connects: r.connects, closes: r.closes, cleared: bus.get('bridge:dr') === undefined };
})();

// --- upstream: the bridge's own downstream is never echoed back ---
(function () {
	const { bus } = fresh();
	const u = manual('ec', { downstream: ['app.a'], upstream: ['app.up'] });
	bus.attachSource(u);
	open(u);
	bus.publish('app.up', { x: 1 }, { from: 'server', bridge: 'ec' });
	const echoed = u.sent.length;
	bus.publish('app.up', { x: 2 }, { from: 'server', bridge: 'other' });
	bus.publish('app.up', { x: 3 });
	out.echo = { echoed: echoed, sent: u.sent.map(function (f) { return f.payload.x; }) };
})();

// --- a cmd: downstream publish ---
(function () {
	const { bus, errors } = fresh();
	const got = [];
	bus.subscribe('cmd:t1', function (p, m) { got.push({ op: p.op, from: m.from, bridge: m.bridge }); });
	const c = manual('cm');
	bus.attachSource(c);
	open(c);
	c.ctx.publish('cmd:t1', { schemaVersion: 1, op: 'refresh' });
	out.cmd = { got: got, errors: codes(errors), retained: bus.get('cmd:t1') !== undefined };
})();

// --- connect throws (E-JS-52) ---
(function () {
	const { bus, errors } = fresh();
	const seen = watch(bus, 'ct');
	bus.attachSource({ id: 'ct', downstream: ['app.a'], connect: function () { throw new Error('boom'); } });
	out.connectThrows = { state: last(seen).state, codes: codes(errors), messages: errors.map(function (e) { return e.message; }),
		banners: errors.map(function (e) { return e.banner; }) };
})();

// --- a failed bridge id can be replaced ---
(function () {
	const { bus, errors } = fresh();
	const seen = watch(bus, 'rf');
	const bad = bus.attachSource({ id: 'rf', downstream: ['app.a'], connect: function () { throw new Error('boom'); } });
	const failedState = last(seen).state;
	const good = manual('rf');
	const link = bus.attachSource(good);
	if (good.ctx) open(good);
	bad.detach();
	out.replaceFailed = { failedState: failedState, state: bus.get('bridge:rf').state, connects: good.connects, closes: good.closes,
		codes: codes(errors) };
	link.detach();
})();

process.stdout.write(JSON.stringify(out));
