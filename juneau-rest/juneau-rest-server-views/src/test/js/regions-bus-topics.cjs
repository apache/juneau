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
 * regions-bus-topics.cjs - always-on Node harness for a region's view of the PAGE bus (juneau-bus.js):
 * ctx.publish / ctx.subscribe with the region key as owner, self-echo, the "message" re-populate and its
 * messageSignal (decision (i) of the C2 bus plan, Task 2), the per-invocation sweep, teardown disposing the owner,
 * the separation from the region bus (ctx.emit / ctx.on), and E-JS-46 when juneau-bus.js is missing.  Every
 * assertion lives in Regions_BusTopics_Test.
 *
 *   Usage:  node regions-bus-topics.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-bus.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath, busJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath || !busJsPath) {
	console.error('usage: node regions-bus-topics.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>'
		+ ' <juneau-bus.js>');
	process.exit(2);
}

const out = {};

/** A fresh page with juneau-bus.js loaded first. */
function withBus() {
	const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { busJsPath: busJsPath });
	h.B = h.NS.bus;
	return h;
}

function mk(h, id, populate, opts) {
	const el = H.mkRegion(h.env, Object.assign({ id: id, type: 'card-body', populate: populate }, opts || {}));
	h.R.initRegion(el);
	return el;
}

function topicEntry(B, topic) {
	return B.topics().find(function (t) { return t.topic === topic; }) || null;
}

/** Runs fn and returns null when it does not throw, else "CODE: message" (or just the message). */
function thrown(fn) {
	try {
		fn();
		return null;
	} catch (e) {
		return (e && e.code ? e.code + ': ' : '') + String(e && e.message);
	}
}

(async function () {

	// --- t01: ctx.publish - meta.from is the region KEY; a page subscriber and get() both see it ------------------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.picked', { retain: true });
		const got = [];
		B.subscribe('app.picked', function (p, meta) { got.push({ p: p, from: meta.from }); });
		let key = null;
		let returned = null;
		R.register('picker', function (ctx) {
			key = ctx.key;
			returned = ctx.publish('app.picked', { region: 'east' });
		});
		mk(h, 'picker', 'picker', { host: 'map' });
		out.t01_key = key;
		out.t01_returned = returned;
		out.t01_got = got;
		out.t01_retained = B.get('app.picked') || null;
		out.t01_noErrors = h.rec.errors.length === 0;
	}

	// --- t02: self-echo - a region does not hear its own publish unless it subscribed with {echo:true} ------------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.zoom', { retain: false });
		const heard = { a: [], b: [], c: [] };
		const ctxs = {};
		R.register('zoomer', function (ctx) {
			ctxs[ctx.id] = ctx;
			ctx.subscribe('app.zoom', function (p) { heard[ctx.id].push(p.level); });
		});
		R.register('echoer', function (ctx) {
			ctxs[ctx.id] = ctx;
			ctx.subscribe('app.zoom', function (p) { heard[ctx.id].push(p.level); }, { echo: true });
		});
		mk(h, 'a', 'zoomer');
		mk(h, 'b', 'zoomer');
		mk(h, 'c', 'echoer');
		ctxs.a.publish('app.zoom', { level: 1 });
		ctxs.c.publish('app.zoom', { level: 2 });
		out.t02_a = heard.a.join(',');
		out.t02_b = heard.b.join(',');
		out.t02_c = heard.c.join(',');
	}

	// --- t03: decision (i) - a ctx.refresh() from a ctx.subscribe handler is a "message" re-populate with a live
	//     messageSignal, and the NEXT delivery supersedes it.  This is the {retained:false} idiom. --------------------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.probe', { retain: true });
		const runs = [];
		R.register('detail', function (ctx) {
			const probe = B.get('app.probe');
			runs.push({
				reason: ctx.reason,
				signal: ctx.messageSignal,
				abortedAtRun: ctx.messageSignal ? ctx.messageSignal.aborted : null,
				probe: probe ? probe.n : null
			});
			ctx.subscribe('app.probe', function () { ctx.refresh(); }, { retained: false });
		});
		mk(h, 'detail', 'detail');
		B.publish('app.probe', { n: 1 });
		B.publish('app.probe', { n: 2 });
		out.t03_reasons = runs.map(function (r) { return r.reason; });
		out.t03_initialSignalNull = runs[0].signal === null;
		out.t03_liveAtRun = runs.slice(1).map(function (r) { return r.abortedAtRun; });
		out.t03_probeSeen = runs.map(function (r) { return r.probe === null ? '-' : String(r.probe); }).join(',');
		out.t03_firstAbortReason = runs[1].signal.aborted ? String(runs[1].signal.reason?.message) : 'not aborted';
		out.t03_latestLive = runs[2].signal.aborted === false;
		out.t03_quiet = h.rec.errors.length === 0 && h.rec.warns.length === 0;
	}

	// --- t04: the depth unwinds - after a delivery, and after a handler that threw (E-JS-45, isolated) -----------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.boom', { retain: false });
		const reasons = [];
		let ctxRef = null;
		R.register('boomer', function (ctx) {
			ctxRef = ctx;
			reasons.push(ctx.reason);
			ctx.subscribe('app.boom', function () { throw new Error('kaboom'); });
		});
		mk(h, 'boomer', 'boomer');
		B.publish('app.boom', {});
		ctxRef.refresh();
		out.t04_reasons = reasons;
		out.t04_e45 = h.rec.errorsMatching('E-JS-45').length;
		out.t04_e45NamesOwner = h.rec.errorsMatching("owner 'boomer'").length === 1;
	}

	// --- t05: subscriptions are per invocation - two re-populates leave ONE live subscription --------------------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.tick', { retain: false });
		let count = 0;
		let ctxRef = null;
		R.register('ticker', function (ctx) {
			ctxRef = ctx;
			ctx.subscribe('app.tick', function () { count++; });
		});
		mk(h, 'ticker', 'ticker');
		ctxRef.refresh();
		ctxRef.refresh();
		B.publish('app.tick', {});
		out.t05_deliveries = count;
		out.t05_subscribers = topicEntry(B, 'app.tick').subscribers;
	}

	// --- t06: teardown disposes the owner - subscriptions gone, claimed state cleared, late calls ignored --------
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.ping', { retain: false });
		let heard = 0;
		let ctxRef = null;
		R.register('grid', function (ctx) {
			ctxRef = ctx;
			ctx.subscribe('app.ping', function () { heard++; });
			ctx.publish('selection:grid1', { schemaVersion: 1, ids: ['r1'], count: 1 });
		});
		const el = mk(h, 'grid1', 'grid');
		out.t06_ownerBefore = topicEntry(B, 'selection:grid1').owner;
		out.t06_idsBefore = B.get('selection:grid1').ids.join(',');
		R.teardownRegionsIn(el);
		B.publish('app.ping', {});
		out.t06_heardNothing = heard === 0;
		out.t06_valueGone = B.get('selection:grid1') === undefined;
		out.t06_ownerAfter = topicEntry(B, 'selection:grid1').owner;
		out.t06_lateReturned = ctxRef.publish('app.ping', {});
		out.t06_lateWarned = h.rec.warnsMatching("owner 'grid1' is disposed; publish ignored").length === 1;
		out.t06_lateSubscribeIsNoop = typeof ctxRef.subscribe('app.ping', function () { heard++; }) === 'function';
		B.publish('app.ping', {});
		out.t06_stillNothing = heard === 0;
	}

	// --- t07: the two buses are separate - ctx.emit never reaches the page bus, ctx.publish never reaches ctx.on --
	{
		const h = withBus();
		const { R, B } = h;
		B.declare('app.note', { retain: false });
		const onGot = [];
		let a = null;
		R.register('emitter', function (ctx) { a = ctx; });
		R.register('listener', function (ctx) { ctx.on(function (msg) { onGot.push(msg.kind); }); });
		mk(h, 'emitter', 'emitter');
		mk(h, 'listener', 'listener');
		const before = B.history().length;
		a.emit({ kind: 'legacy' });
		a.publish('app.note', { n: 1 });
		out.t07_onGot = onGot;
		out.t07_historyTopics = B.history().slice(before).map(function (e) { return e.topic; });
	}

	// --- t08: E-JS-46 - no juneau-bus.js.  A region that never wires topics is unaffected; one that does fails loud
	{
		const h = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const { R } = h;
		const caught = {};
		R.register('plain', function () { /* never touches the page bus */ });
		R.register('wired', function (ctx) {
			caught.publish = thrown(function () { ctx.publish('app.x', {}); });
			caught.subscribe = thrown(function () { ctx.subscribe('app.x', function () {}); });
		});
		mk(h, 'plain', 'plain');
		out.t08_plainQuiet = h.rec.errorsMatching('E-JS-46').length === 0;
		mk(h, 'wired', 'wired');
		out.t08_publish = caught.publish;
		out.t08_subscribe = caught.subscribe;
		out.t08_logged = h.rec.errorsMatching('E-JS-46').length;
		out.t08_namesRegion = h.rec.errorsMatching("region 'wired' called ctx.publish").length === 1;
		out.t08_busAbsent = h.NS.bus === undefined;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e?.stack ? e.stack : e));
	process.exit(1);
});
