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
 * bus-core.cjs - always-on Node harness for the juneau-bus.js core (message bus addendum, spec sections 3, 6.3,
 * 7).  Loads juneau-bus.js standalone into the DOM shim (it needs nothing else on window.JuneauViews) and reports
 * topic syntax, delivery order and meta, retained replay, distinct-until-changed, clear, JSON-only payloads, deep
 * freeze, schemaVersion, the drain cap and its cycle text, subscriber isolation, the owner rule, declare conflicts,
 * self-echo, the history ring, the dev trace and its enabling flags, error sinks, owner dispose, the pure helpers
 * and topics().  Every assertion lives in ViewsJs_BusCore_Test.
 *
 *   Usage:  node bus-core.cjs <juneau-bus.js>
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const busJsPath = process.argv[2];
if (!busJsPath) {
	console.error('usage: node bus-core.cjs <juneau-bus.js>');
	process.exit(2);
}

// The bus writes to console.error (every reported error), console.debug (the trace) and console.warn (a disposed
// owner).  stdout must carry only the JSON report, so all three are captured, and the lines become report fields.
const logs = { error: [], debug: [], warn: [] };
Object.keys(logs).forEach(function (k) {
	console[k] = function () { logs[k].push(Array.prototype.map.call(arguments, String).join(' ')); };
});

/** A fresh page: a new shim env, the bus loaded into it, a deterministic clock from 1000, and a recording sink. */
function fresh(prepareEnv) {
	const env = makeEnv();
	if (prepareEnv) prepareEnv(env);
	const bus = loadScripts([busJsPath], env).NS.bus;
	let t = 1000;
	bus.config.timers.now = function () { return t++; };
	const errors = [];
	bus.onError(function (e) {
		errors.push({ code: e.code, message: e.message, from: e.detail.from || null,
			paintOn: e.detail.paintOn === undefined ? 'n/a' : e.detail.paintOn });
	});
	return { env: env, bus: bus, errors: errors };
}

/** Runs fn and returns null when it does not throw, else {code, name, message}. */
function thrown(fn) {
	try { fn(); return null; }
	catch (e) { return { code: e.code || null, name: e.name, message: e.message }; }
}

const J = JSON.stringify;
const out = {};

// --- t01: constants, API surface, BusError, and only `bus` added to the namespace ---------------------------------
{
	const { env, bus } = fresh();
	const e = new bus.BusError('E-JS-40', 'm', { topic: 't' });
	out.t01 = {
		contractVersion: bus.CONTRACT_VERSION,
		families: bus.FRAMEWORK_FAMILIES.slice(),
		familiesFrozen: Object.isFrozen(bus.FRAMEWORK_FAMILIES),
		drainCap: bus.DRAIN_CAP, historyCap: bus.HISTORY_CAP, cycleTraceCap: bus.CYCLE_TRACE_CAP,
		senderKey: bus.FRAMEWORK_SENDER_KEY,
		missingFunctions: ['publish', 'subscribe', 'get', 'declare', 'clear', 'topics', 'trace', 'history', 'owner',
			'claim', 'onError'].filter(function (k) { return typeof bus[k] !== 'function'; }),
		util: Object.keys(bus.util).sort(),
		timers: Object.keys(bus.config.timers).sort(),
		namespaceKeys: Object.keys(env.window.JuneauViews),
		busError: { name: e.name, code: e.code, message: e.message, topic: e.detail.topic, hasStack: typeof e.stack === 'string' },
		reloadKeepsInstance: loadScripts([busJsPath], env).NS.bus === bus
	};
}

// --- t02: topic syntax (spec 3.1) -----------------------------------------------------------------------------------
{
	const { bus } = fresh();
	const cases = ['selection:changes', 'cmd:tasks', 'bridge:live', 'job:j-1.2_3', 'ssc.focus', 'ssc.focus:triage',
		'app.region-picked', 'focus', 'selection', 'ssc.focus.x', 'Ssc.focus', 'ssc.focus:', 'ssc.focus:a b', 'ssc.:x',
		'selection:*', ''];
	out.t02 = {
		results: cases.map(function (t) {
			const err = thrown(function () { bus.util.parseTopic(t); });
			return t + '=' + (err ? err.code : 'ok');
		}),
		parsedCustom: J(bus.util.parseTopic('ssc.focus:triage')),
		parsedEvent: J(bus.util.parseTopic('redraw:runs')),
		parsedCommand: bus.util.parseTopic('cmd:runs').kind,
		pattern: J(bus.util.parseTopic('selection:*', { pattern: true })),
		focusMessage: thrown(function () { bus.publish('focus', {}); }).message,
		subscribeWildcard: thrown(function () { bus.subscribe('ssc.focus:*', function () {}); }).code,
		publishWildcard: thrown(function () { bus.publish('job:*', { schemaVersion: 1 }); }).code,
		declareBad: thrown(function () { bus.declare('focus', { retain: true }); }).code,
		getBad: thrown(function () { bus.get('Bad'); }).code
	};
}

// --- t03: publish/subscribe, idempotent unsubscribe, meta, `from` and `bridge` -------------------------------------
{
	const { bus } = fresh();
	const got = [];
	const off = bus.subscribe('app.note', function (p, m) { got.push(J({ p: p, m: m, frozen: Object.isFrozen(m) })); });
	const r1 = bus.publish('app.note', { n: 1 });
	off();
	off();
	const r2 = bus.publish('app.note', { n: 2 });
	const metas = [];
	bus.subscribe('app.note', function (p, m) { metas.push(m.from + '|' + (m.bridge || '-') + '|' + m.seq); });
	bus.owner('changes').publish('app.note', { n: 3 });
	bus.publish('app.note', { n: 4 }, { from: 'tasks' });
	bus.publish('app.note', { n: 5 }, { from: 'server', bridge: 'live' });
	bus.publish('app.note', { n: 6 }, { bridge: 'live' });
	out.t03 = { r1: r1, r2: r2, got: got, metas: metas };
}

// --- t04: retained replay on subscribe, {retained:false}, events are never replayed ---------------------------------
{
	const { bus } = fresh();
	const o = bus.owner('changes');
	o.publish('selection:changes', { schemaVersion: 1, ids: ['c-17'], count: 1 });
	const late = [];
	bus.subscribe('selection:changes', function (p, m) { late.push(p.ids[0] + '|' + m.retained + '|' + m.seq + '|' + m.from); });
	const lateSync = late.length;
	const optOut = [];
	bus.subscribe('selection:changes', function (p) { optOut.push(p); }, { retained: false });
	const v = bus.get('selection:changes');
	o.publish('redraw:changes', { schemaVersion: 1, rowCount: 3 });
	const lateEvent = [];
	bus.subscribe('redraw:changes', function (p) { lateEvent.push(p); });
	o.publish('selection:changes', { schemaVersion: 1, ids: ['c-18'], count: 1 });
	out.t04 = {
		lateSync: lateSync, late: late, optOut: optOut.length, getIds: v.ids.slice(), getFrozen: Object.isFrozen(v),
		lateEvent: lateEvent.length, eventNotRetained: bus.get('redraw:changes') === undefined
	};
}

// --- t05: distinct-until-changed (canonical JSON, key order ignored) on retained topics only -------------------------
{
	const { bus } = fresh();
	bus.declare('app.sel', { retain: true });
	bus.declare('app.ev', { retain: false });
	const seen = [];
	bus.subscribe('app.sel', function (p) { seen.push(p.a); });
	const returns = [
		bus.publish('app.sel', { a: 1, b: [1, 2] }),
		bus.publish('app.sel', { b: [1, 2], a: 1 }),
		bus.publish('app.sel', { a: 2, b: [1, 2] }),
		bus.publish('app.ev', { a: 1 }),
		bus.publish('app.ev', { a: 1 })
	];
	out.t05 = { returns: returns, seen: seen, suppressed: bus.history().map(function (h) { return h.suppressed; }) };
}

// --- t06: clear ------------------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	bus.declare('app.sel', { retain: true });
	bus.publish('app.sel', { a: 1 });
	const seen = [];
	bus.subscribe('app.sel', function (p, m) { seen.push(J(p) + '|' + !!m.cleared + '|' + m.retained); });
	const r1 = bus.clear('app.sel');
	const r2 = bus.clear('app.sel');
	const late = [];
	bus.subscribe('app.sel', function (p) { late.push(p); });
	out.t06 = { r1: r1, r2: r2, seen: seen, getAfter: bus.get('app.sel') === undefined, late: late.length,
		republish: bus.publish('app.sel', { a: 1 }) };
}

// --- t07: JSON-only payloads (E-JS-42) ---------------------------------------------------------------------------------
{
	const { env, bus } = fresh();
	const cyc = { a: 1 };
	cyc.self = cyc;
	const cases = {
		fn: { f: function () {} }, undef: { u: undefined }, dom: { el: env.el('div') }, cycle: cyc,
		nan: { n: NaN }, inf: [Infinity], symbol: { s: Symbol('x') }, top: undefined
	};
	let delivered = 0;
	bus.subscribe('app.x', function () { delivered++; });
	const results = {};
	Object.keys(cases).forEach(function (k) { results[k] = thrown(function () { bus.publish('app.x', cases[k]); }); });
	const rejectedDeliveries = delivered;
	const shared = { x: 1 };
	out.t07 = { results: results, rejectedDeliveries: rejectedDeliveries,
		sharedNotCycle: bus.publish('app.x', { a: shared, b: shared }), nullOk: bus.publish('app.x', null) };
}

// --- t08: deep freeze of a private copy, one instance shared by every subscriber -------------------------------------
{
	const { bus } = fresh();
	const original = { a: { b: [1, { c: 2 }] } };
	const recs = [];
	bus.subscribe('app.f', function (p) { recs.push(p); });
	bus.subscribe('app.f', function (p) { recs.push(p); });
	bus.publish('app.f', original);
	const rec = recs[0];
	const mutation = thrown(function () { rec.a.b[1].c = 3; });
	out.t08 = {
		deepFrozen: [rec, rec.a, rec.a.b, rec.a.b[1]].every(function (o) { return Object.isFrozen(o); }),
		copied: rec !== original, originalFrozen: Object.isFrozen(original),
		mutationName: mutation && mutation.name, value: rec.a.b[1].c, sameInstanceForAll: recs[0] === recs[1]
	};
}

// --- t09: framework payloads need schemaVersion 1 (E-JS-43); custom topics are unchecked ------------------------------
{
	const { bus } = fresh();
	const msg = function (p) {
		const e = thrown(function () { bus.publish('cmd:tasks', p); });
		return e ? e.code + ' ' + e.message : 'ok';
	};
	out.t09 = {
		missing: msg({ op: 'reload' }), wrong: msg({ schemaVersion: 2, op: 'reload' }), text: msg({ schemaVersion: '1' }),
		nullPayload: msg(null), ok: msg({ schemaVersion: 1, op: 'reload' }),
		customUnchecked: bus.publish('app.x', { op: 'reload' })
	};
}

// --- t10: queued, non-reentrant, breadth-first delivery ------------------------------------------------------------------
{
	const { bus } = fresh();
	const log = [];
	bus.subscribe('app.one', function () { log.push('A:one'); bus.publish('app.two', {}); log.push('A:after-publish'); });
	bus.subscribe('app.one', function () { log.push('B:one'); });
	bus.subscribe('app.two', function () { log.push('C:two'); });
	bus.publish('app.one', {});
	out.t10 = { log: log };
}

// --- t11: drain cap (E-JS-44) with the cycle text ------------------------------------------------------------------------
{
	const { bus, errors } = fresh();
	logs.error.length = 0;
	const a = bus.owner('a');
	const b = bus.owner('b');
	let deliveries = 0;
	let n = 0;
	const returns = [];
	a.subscribe('app.pong', function () { deliveries++; returns.push(a.publish('app.ping', { n: ++n })); });
	b.subscribe('app.ping', function () { deliveries++; returns.push(b.publish('app.pong', { n: ++n })); });
	const first = a.publish('app.ping', { n: 0 });
	const cap = errors.filter(function (e) { return e.code === 'E-JS-44'; });
	out.t11 = {
		first: first, deliveries: deliveries,
		trueReturns: returns.filter(function (r) { return r === true; }).length,
		falseReturns: returns.filter(function (r) { return r === false; }).length,
		lastReturn: returns[returns.length - 1],
		capErrors: cap.length, message: cap.length ? cap[0].message : null,
		from: cap.length ? cap[0].from : null, paintOn: cap.length ? cap[0].paintOn : null,
		dropped: bus.history().filter(function (h) { return h.dropped; }).length,
		consoleLogged: logs.error.filter(function (l) { return l.indexOf('E-JS-44') >= 0; }).length,
		nextDrainWorks: bus.publish('app.other', {})
	};
}

// --- t12: a throwing subscriber is isolated (E-JS-45) --------------------------------------------------------------------
{
	const { bus, errors } = fresh();
	bus.subscribe('app.x', function () { throw new Error('boom'); });
	bus.owner('w').subscribe('app.x', function () { throw new Error('bang'); });
	let reached = false;
	bus.subscribe('app.x', function () { reached = true; });
	const r = bus.publish('app.x', {});
	const e45 = errors.filter(function (e) { return e.code === 'E-JS-45'; });
	out.t12 = { returned: r, reached: reached,
		messages: e45.map(function (e) { return e.message; }), paintOn: e45.map(function (e) { return e.paintOn; }) };
}

// --- t13: the single-owner rule for framework state/event topics (E-JS-49) ----------------------------------------------
{
	const { bus, errors } = fresh();
	const tbl = bus.owner('tbl');
	const other = bus.owner('other');
	const returns = {
		ownerPublish: tbl.publish('selection:tbl', { schemaVersion: 1, ids: [] }),
		otherPublish: other.publish('selection:tbl', { schemaVersion: 1, ids: ['x'] }),
		globalPublish: bus.publish('selection:tbl', { schemaVersion: 1, ids: ['y'] }),
		otherClear: other.clear('selection:tbl'),
		globalClear: bus.clear('selection:tbl'),
		otherCmd: other.publish('cmd:tbl', { schemaVersion: 1, op: 'reload' }),
		globalCmd: bus.publish('cmd:tbl', { schemaVersion: 1, op: 'reload' }),
		claimFree: other.claim('filter:other'),
		claimedByOther: tbl.publish('filter:other', { schemaVersion: 1, options: {} }),
		unclaimedGlobal: bus.publish('selection:zz', { schemaVersion: 1, ids: [] })
	};
	out.t13 = {
		returns: returns,
		stillOwnersValue: bus.get('selection:tbl').ids.length === 0,
		claimConflict: thrown(function () { other.claim('selection:tbl'); }),
		claimCommand: thrown(function () { other.claim('cmd:tbl'); }),
		messages: errors.filter(function (e) { return e.code === 'E-JS-49'; }).map(function (e) { return e.message + '|' + e.paintOn; })
	};
}

// --- t14: declare (E-JS-50), declaredBy, family patterns ---------------------------------------------------------------
{
	const { bus } = fresh();
	bus.declare('app.x', { retain: true });
	bus.declare('app.x', { retain: true, by: 'ssc-script' });
	const conflict = thrown(function () { bus.declare('app.x', { retain: false }); });
	const framework = thrown(function () { bus.declare('selection:*', { retain: false }); });
	const frameworkOk = thrown(function () { bus.declare('cmd:*', { retain: false }); });
	bus.declare('ssc.focus:*', { retain: true });
	const patternConflict = thrown(function () { bus.declare('ssc.focus:triage', { retain: false }); });
	bus.publish('ssc.focus:triage', { v: 1 });
	const late = [];
	bus.subscribe('ssc.focus:triage', function (p) { late.push(p.v); });
	const entry = bus.topics().filter(function (t) { return t.topic === 'app.x'; })[0];
	out.t14 = {
		conflict: conflict, framework: framework, frameworkOk: frameworkOk, patternConflict: patternConflict,
		patternRetained: late, badOpts: thrown(function () { bus.declare('app.y', {}); }).name,
		declaredBy: entry.declaredBy.slice(), retain: entry.retain
	};
}

// --- t15: an undeclared custom topic is delivered but never retained ----------------------------------------------------
{
	const { bus } = fresh();
	const live = [];
	bus.subscribe('app.u', function (p) { live.push(p.a); });
	bus.publish('app.u', { a: 1 });
	const late = [];
	bus.subscribe('app.u', function (p) { late.push(p); });
	out.t15 = { live: live, late: late.length,
		entry: J(bus.topics().filter(function (t) { return t.topic === 'app.u'; })[0]) };
}

// --- t16: self-echo suppression for owner-bound subscriptions -----------------------------------------------------------
{
	const { bus } = fresh();
	const c = bus.owner('c');
	const own = [];
	const echo = [];
	const global = [];
	c.subscribe('app.e', function (p) { own.push(p.n); });
	c.subscribe('app.e', function (p) { echo.push(p.n); }, { echo: true });
	bus.subscribe('app.e', function (p) { global.push(p.n); });
	c.publish('app.e', { n: 1 });
	bus.owner('d').publish('app.e', { n: 2 });
	out.t16 = { own: own, echo: echo, global: global };
}

// --- t17: history() ring ----------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	for (let i = 0; i < 300; i++) bus.publish('app.h', { i: i });
	const h = bus.history();
	const head = { length: h.length, firstSeq: h[0].seq, lastSeq: h[h.length - 1].seq, keys: Object.keys(h[0]).sort(),
		frozen: Object.isFrozen(h[0]), size: h[h.length - 1].size, copy: bus.history() !== h };
	bus.declare('app.r', { retain: true });
	bus.publish('app.r', { a: 1 });
	bus.publish('app.r', { a: 1 });
	const last = bus.history().slice(-1)[0];
	out.t17 = Object.assign(head, { lastSuppressed: last.suppressed, lastSeqAfter: last.seq });
}

// --- t18: the dev trace -----------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	logs.debug.length = 0;
	const prev = bus.trace(true);
	bus.declare('app.r', { retain: true });
	const tasks = bus.owner('tasks');
	bus.subscribe('app.r', function () { tasks.publish('app.nested', { op: 'reload' }); });
	bus.publish('app.r', { rows: [{ a: 1 }, { a: 2 }, { a: 3 }], count: 3 });
	bus.publish('app.r', { rows: [{ a: 1 }, { a: 2 }, { a: 3 }], count: 3 });
	bus.publish('app.big', { s: 'x'.repeat(500) });
	const now = bus.trace(false);
	bus.publish('app.after', {});
	out.t18 = { prev: prev, now: now, query: bus.trace(), lines: logs.debug.slice() };
}

// --- t19: the trace's enabling flags ------------------------------------------------------------------------------------
{
	out.t19 = {
		viaStorage: fresh(function (env) { env.window.localStorage.setItem('juneau-bus-trace', '1'); }).bus.trace(),
		viaUrl: fresh(function (env) { env.window.location = { search: '?x=1&juneau-bus-trace' }; }).bus.trace(),
		otherValue: fresh(function (env) { env.window.localStorage.setItem('juneau-bus-trace', '0'); }).bus.trace(),
		off: fresh().bus.trace(),
		blockedStorage: fresh(function (env) {
			env.window.localStorage = { getItem: function () { throw new Error('blocked'); } };
		}).bus.trace()
	};
}

// --- t20: error sinks ------------------------------------------------------------------------------------------------
{
	const env = makeEnv();
	const bus = loadScripts([busJsPath], env).NS.bus;
	logs.error.length = 0;
	const got = [];
	const off = bus.onError(function (e) { got.push(e.code + '|' + e.name + '|' + e.detail.topic); });
	bus.subscribe('app.x', function () { throw new Error('boom'); });
	bus.publish('app.x', {});
	off();
	off();
	bus.publish('app.x', {});
	bus.onError(function () { throw new Error('sink broke'); });
	const sinkThrow = thrown(function () { bus.publish('app.x', {}); });
	out.t20 = {
		got: got, sinkThrow: sinkThrow,
		consoleErrors: logs.error.filter(function (l) { return l.indexOf('[juneau-bus] E-JS-45: ') === 0; }).length,
		sinkLogged: logs.error.some(function (l) { return l.indexOf('error sink threw: sink broke') >= 0; })
	};
}

// --- t21: owner dispose ----------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	logs.warn.length = 0;
	const card = bus.owner('changes');
	const heard = [];
	card.subscribe('app.n', function () { heard.push(1); });
	card.publish('selection:changes', { schemaVersion: 1, ids: ['a'] });
	card.claim('filter:changes');
	const watcher = [];
	bus.subscribe('selection:changes', function (p, m) { watcher.push(J(p) + '|' + !!m.cleared); });
	card.dispose();
	card.dispose();
	bus.publish('app.n', {});
	const late = [];
	bus.subscribe('selection:changes', function (p) { late.push(p); });
	out.t21 = {
		heard: heard.length, watcher: watcher, late: late.length,
		reclaim: bus.owner('other').claim('selection:changes'),
		filterReleased: bus.owner('other').claim('filter:changes'),
		afterDispose: card.publish('app.n', {}),
		warned: logs.warn.some(function (l) { return l.indexOf("owner 'changes' is disposed; publish ignored") >= 0; })
	};
}

// --- t23: pure helpers -----------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	const u = bus.util;
	const v = { ids: ['c-17'], a: { b: [{ c: 5 }] } };
	out.t23 = {
		path: [u.resolvePath(v, 'ids.0'), u.resolvePath(v, 'a.b.0.c'), u.resolvePath(v, 'a.x') === undefined,
			u.resolvePath(v, 'ids.x') === undefined, u.resolvePath(v, '') === undefined, u.resolvePath(v, 'a..b') === undefined],
		canonical: u.canonicalJson({ b: 1, a: [{ d: 1, c: 2 }] }),
		matches: [u.topicMatches('job:*', 'job:j1'), u.topicMatches('job:*', 'job'), u.topicMatches('job:*', 'jobs:x'),
			u.topicMatches('app.x', 'app.x'), u.topicMatches('app.x:*', 'app.x:k')]
	};
}

// --- t24: topics() --------------------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	bus.owner('changes').publish('selection:changes', { schemaVersion: 1, ids: [] });
	bus.subscribe('selection:changes', function () {});
	bus.declare('app.x', { retain: false });
	bus.publish('ssc.u', {});
	bus.declare('job:*', { retain: true, by: 'bridge:live' });
	out.t24 = { topics: bus.topics().map(function (t) { return J(t); }), frozen: Object.isFrozen(bus.topics()[0]) };
}

// --- t25: a subscribe during a drain gets the newest retained value once, not a stale queued copy ---------------------
{
	const { bus } = fresh();
	const o = bus.owner('t');
	const seen = [];
	bus.subscribe('app.go', function () {
		o.publish('selection:t', { schemaVersion: 1, v: 1 });
		o.publish('selection:t', { schemaVersion: 1, v: 2 });
	});
	bus.subscribe('app.go', function () {
		bus.subscribe('selection:t', function (p, m) { seen.push(p.v + '|' + m.retained + '|' + m.seq); });
	});
	bus.publish('app.go', {});
	const afterDrain = seen.slice();
	o.publish('selection:t', { schemaVersion: 1, v: 3 });
	out.t25 = { afterDrain: afterDrain, all: seen };
}

// --- t26: a drain overflow rolls back the retained values of the messages it purges -----------------------------------
{
	const { bus, errors } = fresh();
	const o = bus.owner('t');
	const r = bus.owner('r');
	const fan = function () { for (let i = 0; i < 70; i++) r.publish('app.fan', { i: i }); };
	bus.subscribe('app.fresh', function () { o.publish('selection:t', { schemaVersion: 1, v: 7 }); fan(); });
	bus.publish('app.fresh', {});
	const freshGet = bus.get('selection:t');
	const freshHist = bus.history().filter(function (h) { return h.topic === 'selection:t'; }).map(function (h) { return h.dropped; });
	const republish = o.publish('selection:t', { schemaVersion: 1, v: 7 });
	o.publish('selection:t', { schemaVersion: 1, v: 5 });
	bus.subscribe('app.prior', function () { o.publish('selection:t', { schemaVersion: 1, v: 9 }); fan(); });
	bus.publish('app.prior', {});
	const priorGet = bus.get('selection:t');
	const late = [];
	bus.subscribe('selection:t', function (p) { late.push(p.v); });
	out.t26 = {
		freshGet: freshGet === undefined ? 'undefined' : freshGet.v, freshHist: freshHist, republish: republish,
		priorGet: priorGet.v, late: late,
		capErrors: errors.filter(function (e) { return e.code === 'E-JS-44'; }).length
	};
}

// --- t27: a clear is never dropped, even while the drain is overflowed ------------------------------------------------
{
	const { bus } = fresh();
	const o = bus.owner('t');
	const r = bus.owner('r');
	o.publish('selection:t', { schemaVersion: 1, v: 1 });
	bus.subscribe('app.go', function () {
		for (let i = 0; i < 70; i++) r.publish('app.fan', { i: i });
		o.dispose();
	});
	bus.publish('app.go', {});
	const late = [];
	bus.subscribe('selection:t', function (p) { late.push(p); });
	out.t27 = { get: bus.get('selection:t') === undefined, late: late.length, reclaim: bus.owner('x').claim('selection:t') };
}

// --- t28: unsubscribing inside a handler mid-drain ---------------------------------------------------------------------
{
	const { bus } = fresh();
	const log = [];
	let offB;
	let offSelf;
	bus.subscribe('app.u', function () { log.push('A'); offB(); });
	offB = bus.subscribe('app.u', function () { log.push('B'); });
	offSelf = bus.subscribe('app.u', function () { log.push('C'); offSelf(); });
	bus.publish('app.u', {});
	bus.publish('app.u', {});
	out.t28 = { log: log };
}

// --- t29: get() during a drain already returns the newest queued value ---------------------------------------------------
{
	const { bus } = fresh();
	const o = bus.owner('t');
	let during = null;
	bus.subscribe('app.go', function () {
		o.publish('selection:t', { schemaVersion: 1, v: 2 });
		during = bus.get('selection:t').v;
	});
	bus.publish('app.go', {});
	out.t29 = { during: during };
}

// --- t30: a non-owner cannot publish an owner-only event topic (E-JS-49) -------------------------------------------------
{
	const { bus, errors } = fresh();
	const returns = {
		owner: bus.owner('tbl').publish('redraw:tbl', { schemaVersion: 1, rowCount: 1 }),
		other: bus.owner('other').publish('redraw:tbl', { schemaVersion: 1, rowCount: 2 }),
		global: bus.publish('redraw:tbl', { schemaVersion: 1, rowCount: 3 })
	};
	out.t30 = { returns: returns, errors: errors.filter(function (e) { return e.code === 'E-JS-49'; })
		.map(function (e) { return e.message + '|' + e.paintOn; }) };
}

// --- t31: the trace marks a clear ----------------------------------------------------------------------------------------
{
	const { bus } = fresh();
	logs.debug.length = 0;
	bus.trace(true);
	const o = bus.owner('c');
	o.publish('selection:c', { schemaVersion: 1, ids: ['a'] });
	o.clear('selection:c');
	out.t31 = { lines: logs.debug.slice() };
}

// --- t32: overflow rollback around clears (publish after a clear; the clear as the overflowing message) ---------------
{
	const { bus } = fresh();
	const o = bus.owner('t');
	const r = bus.owner('r');
	const fan = function (n) { for (let i = 0; i < n; i++) r.publish('app.fan', { i: i }); };
	o.publish('selection:t', { schemaVersion: 1, v: 0 });
	bus.subscribe('app.a', function () {
		o.publish('selection:t', { schemaVersion: 1, v: 1 });
		o.clear('selection:t');
		o.publish('selection:t', { schemaVersion: 1, v: 2 });
		fan(70);
	});
	bus.publish('app.a', {});
	const aGet = bus.get('selection:t') === undefined;
	const aRepublish = o.publish('selection:t', { schemaVersion: 1, v: 2 });
	o.publish('selection:t', { schemaVersion: 1, v: 0 });
	bus.subscribe('app.b', function () {
		fan(62);
		o.publish('selection:t', { schemaVersion: 1, v: 1 });
		o.clear('selection:t');
	});
	bus.publish('app.b', {});
	out.t32 = { aGet: aGet, aRepublish: aRepublish, bGet: bus.get('selection:t') === undefined,
		bRepublish: o.publish('selection:t', { schemaVersion: 1, v: 1 }) };
}

process.stdout.write(JSON.stringify(out));
