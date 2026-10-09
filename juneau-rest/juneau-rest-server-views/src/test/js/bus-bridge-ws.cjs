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
 * bus-bridge-ws.cjs - Node harness for the stock WebSocket source (spec §11.3 step 2b, §11.6) and the shell hook
 * JuneauViews.bus.wiring.attachBridges (§11.5). A FakeWebSocket records construction URLs and sent text and lets
 * the harness deliver messages and close codes. Every assertion lives in ViewsJs_BusBridgeWs_Test.
 *
 *   Usage:  node bus-bridge-ws.cjs <juneau-bus.js>
 */
'use strict';

const path = require('node:path');
const { loadScripts, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));
const { fakeClock, settle } = require(path.join(__dirname, 'bus-fake-clock.cjs'));

const busJs = process.argv[2];
if (!busJs) {
	console.error('usage: node bus-bridge-ws.cjs <juneau-bus.js>');
	process.exit(2);
}

const SID = 'b'.repeat(64);
const HTTPS = { protocol: 'https:', host: 'console.example.test' };
const HTTP = { protocol: 'http:', host: 'localhost:10000' };

function wsFactory() {
	const sockets = [];
	function FakeWebSocket(url) {
		this.url = url;
		this.readyState = 0;
		this.sent = [];
		this.closedWith = null;
		this.onmessage = this.onclose = this.onerror = this.onopen = null;
		sockets.push(this);
	}
	FakeWebSocket.prototype.send = function (t) { this.sent.push(t); };
	FakeWebSocket.prototype.close = function (code, reason) { this.closedWith = { code: code, reason: reason }; this.readyState = 3; };
	FakeWebSocket.prototype.open = function () { this.readyState = 1; if (this.onopen) this.onopen({}); };
	FakeWebSocket.prototype.message = function (frame) {
		if (this.onmessage) this.onmessage({ data: typeof frame === 'string' ? frame : JSON.stringify(frame) });
	};
	FakeWebSocket.prototype.serverClose = function (code, reason) {
		this.readyState = 3;
		if (this.onclose) this.onclose({ code: code, reason: reason || '' });
	};
	return { WebSocket: FakeWebSocket, sockets: sockets };
}

function sessionBody(over) {
	return Object.assign({
		v: 1, sessionId: SID, heartbeatMs: 15000,
		downstream: [{ topic: 'app.a', retain: true }], upstream: ['app.up'],
		paths: { sse: '/rest/ops/juneau-bus/stream/' + SID, websocket: '/rest/ops/juneau-bus/ws/' + SID }
	}, over || {});
}

function fresh(opts) {
	opts = opts || {};
	const { env, NS } = loadScripts([busJs]);
	const bus = NS.bus;
	const clock = fakeClock();
	bus.config.timers = clock.timers;
	bus.declare('app.a', { retain: true });
	bus.declare('app.up', { retain: false });
	const errors = [];
	bus.onError(function (e) { errors.push({ code: e.code, message: e.message, banner: !!(e.detail && e.detail.banner) }); });
	const posts = [];
	const fetch = function (url, init) {
		posts.push({ url: url, body: JSON.parse(init.body) });
		return Promise.resolve(opts.post ? opts.post(posts.length) : jsonResponse(sessionBody(opts.session)));
	};
	const ws = wsFactory();
	const document = { body: { getAttribute: function (n) { return n === 'data-juneau-csrf' ? 'tok' : null; } } };
	const sourceOptions = { fetch: fetch, location: opts.location || HTTPS, document: document,
		WebSocket: opts.noWebSocket ? null : ws.WebSocket };
	function attach(over) {
		return bus.attachSource(bus.sources.websocket(Object.assign({
			id: 'w', session: '/rest/ops/juneau-bus/session', downstream: ['app.a'], upstream: ['app.up']
		}, sourceOptions, over || {})));
	}
	return { env: env, NS: NS, bus: bus, clock: clock, errors: errors, posts: posts, ws: ws, attach: attach, sourceOptions: sourceOptions };
}

const RESYNC = [{ v: 1, type: 'resync-begin', seq: 1 }, { v: 1, type: 'pub', topic: 'app.a', payload: { n: 1 }, retained: true, seq: 2 },
	{ v: 1, type: 'resync-end', seq: 3 }];
function codes(errors) { return errors.map(function (e) { return e.code; }); }

async function openSocket(h) {
	await settle();
	const s = h.ws.sockets[h.ws.sockets.length - 1];
	s.open();
	RESYNC.forEach(function (f) { s.message(f); });
	return s;
}

async function main() {
	const out = {};

	// --- URL scheme and host come from location; upstream publishes become text frames ---
	{
		const h = fresh();
		h.attach();
		const s = await openSocket(h);
		h.bus.publish('app.up', { x: 1 });
		const h2 = fresh({ location: HTTP });
		h2.attach();
		await settle();
		out.happy = { url: s.url, httpUrl: h2.ws.sockets[0].url, post: h.posts[0].body, state: h.bus.get('bridge:w').state,
			a: h.bus.get('app.a'), sent: s.sent, errors: codes(h.errors) };
	}

	// --- send before the socket is open: E-JS-55, nothing sent ---
	{
		const h = fresh();
		h.attach();
		await settle();
		const s = h.ws.sockets[0];
		h.bus.publish('app.up', { x: 1 });
		out.notOpen = { sent: s.sent.length, codes: codes(h.errors), message: h.errors[0].message };
	}

	// --- the close-code table ---
	async function closeWith(code, reason) {
		const h = fresh();
		h.attach();
		const s = await openSocket(h);
		s.serverClose(code, reason);
		const st = h.bus.get('bridge:w');
		h.clock.advance(st.nextRetryMs);
		await settle();
		return { state: st.state, nextRetryMs: st.nextRetryMs, error: st.error ? st.error.code : null, codes: codes(h.errors),
			message: h.errors.length ? h.errors[0].message : (st.error ? st.error.message : null),
			posts: h.posts.length, sockets: h.ws.sockets.length };
	}
	out.close = {
		c1000: await closeWith(1000, 'bye'),
		c1001: await closeWith(1001),
		c1006: await closeWith(1006),
		c1011: await closeWith(1011),
		c1013: await closeWith(1013),
		c4429: await closeWith(4429, 'bus:rate-limited'),
		c4401: await closeWith(4401, 'bus:unknown-session'),
		c4403: await closeWith(4403, 'bus:refused'),
		c4409: await closeWith(4409, 'bus:replaced'),
		c1008: await closeWith(1008),
		c1009: await closeWith(1009, 'frame too large')
	};

	// --- 4401 three times in a row: E-JS-57 ---
	{
		const h = fresh();
		h.attach();
		for (let i = 0; i < 3; i++) {
			await settle();
			h.ws.sockets[h.ws.sockets.length - 1].serverClose(4401, 'bus:unknown-session');
			h.clock.advance(0);
		}
		await settle();
		out.capability = { posts: h.posts.length, state: h.bus.get('bridge:w').state, codes: codes(h.errors), message: h.errors[0].message };
	}

	// --- per-message: bad frame, binary message, error frames; the socket stays open ---
	{
		const h = fresh();
		h.attach();
		const s = await openSocket(h);
		s.message('{"v":2,"type":"ping"}');
		if (s.onmessage) s.onmessage({ data: new Uint8Array([1, 2]) });
		s.message({ v: 1, type: 'error', code: 'bus:rate-limited', message: 'slow down', topic: 'app.up' });
		s.message({ v: 1, type: 'error', code: 'bus:upstream-failed', message: 'handler threw', topic: 'app.up' });
		s.message({ v: 1, type: 'error', code: 'bus:slow-consumer', message: 'queue full' });
		out.perMessage = { codes: codes(h.errors), messages: h.errors.map(function (e) { return e.message; }),
			state: h.bus.get('bridge:w').state, closed: s.closedWith };
	}

	// --- watchdog closes a silent socket ---
	{
		const h = fresh();
		h.attach();
		const s = await openSocket(h);
		h.clock.advance(45000);
		out.watchdog = { state: h.bus.get('bridge:w').state, closedWith: s.closedWith };
	}

	// --- the watchdog is armed at connect and re-armed by every frame ---
	{
		const h = fresh();
		h.attach();
		await settle();
		const silent = h.ws.sockets[0];
		silent.open();
		h.clock.advance(45000);
		const h2 = fresh();
		h2.attach();
		const live = await openSocket(h2);
		h2.clock.advance(30000);
		live.message({ v: 1, type: 'ping' });
		h2.clock.advance(30000);
		out.watchdogFeed = { silent: { state: h.bus.get('bridge:w').state, closedWith: silent.closedWith },
			afterFrame: { state: h2.bus.get('bridge:w').state, closedWith: live.closedWith } };
	}

	// --- no WebSocket in the browser; 501 from the session POST; bad websocket path ---
	{
		const h = fresh({ noWebSocket: true });
		h.attach();
		await settle();
		const h2 = fresh({ post: function () { return jsonResponse({ code: 'bus:transport-disabled' }, { status: 501 }); } });
		h2.attach();
		await settle();
		const h3 = fresh({ session: { paths: { websocket: '//evil.example.test/ws' } } });
		h3.attach();
		await settle();
		out.unavailable = {
			noWs: { state: h.bus.get('bridge:w').state, codes: codes(h.errors), message: h.errors[0].message, posts: h.posts.length },
			s501: { state: h2.bus.get('bridge:w').state, codes: codes(h2.errors), message: h2.errors[0].message },
			badPath: { state: h3.bus.get('bridge:w').state, codes: codes(h3.errors), sockets: h3.ws.sockets.length }
		};
	}

	// --- detach closes the socket with 1000 ---
	{
		const h = fresh();
		const link = h.attach();
		const s = await openSocket(h);
		link.detach();
		out.detach = { closedWith: s.closedWith, a: h.bus.get('app.a') === undefined ? null : 'present' };
	}

	// --- attachBridges: one stock source per contract bridge; unknown transport; pagehide detaches all ---
	{
		const h = fresh();
		const listeners = {};
		h.env.window.addEventListener = function (type, fn) { (listeners[type] = listeners[type] || []).push(fn); };
		const sseFetches = [];
		const sseFetch = function (url, init) {
			sseFetches.push({ url: url, method: init.method });
			if (init.method === 'POST')
				return Promise.resolve(jsonResponse(sessionBody({ downstream: [{ topic: 'app.a', retain: true }], upstream: [] })));
			return new Promise(function () {});
		};
		const contract = { bridges: [
			{ id: 'live', transport: 'sse', session: '/rest/ops/juneau-bus/session', downstream: ['app.a'] },
			{ id: 'cmd', transport: 'websocket', session: '/rest/ops/juneau-bus/session', downstream: ['app.a'], upstream: ['app.up'], maxAttempts: 4 },
			{ id: 'odd', transport: 'carrier-pigeon', session: '/x', downstream: ['app.a'] }
		] };
		// one shared sourceOptions: the SSE source uses fetch only, the WS source uses fetch + WebSocket
		let calls = 0;
		const sharedFetch = function (url, init) {
			calls++;
			const body = JSON.parse(init.body || '{}');
			if (body.transport === 'websocket') return h.sourceOptions.fetch(url, init);
			return sseFetch(url, init);
		};
		const links = h.bus.wiring.attachBridges(contract, { sourceOptions: Object.assign({}, h.sourceOptions, { fetch: sharedFetch }) });
		await settle();
		const before = { live: h.bus.get('bridge:live').transport, cmd: h.bus.get('bridge:cmd').transport,
			odd: h.bus.get('bridge:odd') === undefined ? null : 'present', links: links.length, sockets: h.ws.sockets.length,
			codes: codes(h.errors), message: h.errors[0].message, banner: h.errors[0].banner, sseMethods: sseFetches.map(function (f) { return f.method; }) };
		(listeners.pagehide || []).forEach(function (fn) { fn({}); });
		out.attachBridges = { before: before, pagehideListeners: (listeners.pagehide || []).length,
			afterPagehide: { live: h.bus.get('bridge:live') === undefined, cmd: h.bus.get('bridge:cmd') === undefined,
				socketClosed: h.ws.sockets[0].closedWith }, empty: h.bus.wiring.attachBridges({}).length, fetchCalls: calls };
	}

	// --- pagehide: a bfcache-bound page (persisted) keeps its bridges; a real unload detaches every link ---
	{
		const h = fresh();
		const listeners = {};
		h.env.window.addEventListener = function (type, fn) { (listeners[type] = listeners[type] || []).push(fn); };
		h.bus.wiring.attachBridges({ bridges: [
			{ id: 'w', transport: 'websocket', session: '/rest/ops/juneau-bus/session', downstream: ['app.a'], upstream: ['app.up'] }
		] }, { sourceOptions: h.sourceOptions });
		const s = await openSocket(h);
		(listeners.pagehide || []).forEach(function (fn) { fn({ persisted: true }); });
		const kept = { state: h.bus.get('bridge:w') === undefined ? null : h.bus.get('bridge:w').state, closedWith: s.closedWith };
		(listeners.pagehide || []).forEach(function (fn) { fn({ persisted: false }); });
		out.pagehidePersisted = { kept: kept, after: { gone: h.bus.get('bridge:w') === undefined, closedWith: s.closedWith } };
	}

	// --- attachBridges registers no pagehide listener when nothing was attached ---
	{
		const count = function (contract) {
			const h = fresh();
			const listeners = {};
			h.env.window.addEventListener = function (type, fn) { (listeners[type] = listeners[type] || []).push(fn); };
			const links = h.bus.wiring.attachBridges(contract, { sourceOptions: h.sourceOptions });
			return { links: links.length, listeners: (listeners.pagehide || []).length, codes: codes(h.errors),
				message: h.errors.length ? h.errors[0].message : null, banner: h.errors.length ? h.errors[0].banner : null };
		};
		out.noListener = { empty: count({}), noBridges: count({ bridges: [] }),
			unknownOnly: count({ bridges: [{ id: 'odd', transport: 'carrier-pigeon', session: '/x', downstream: ['app.a'] }] }) };
	}

	// --- detach while the session POST is in flight: no socket is ever constructed ---
	{
		let release = null;
		const h = fresh({ post: function () { return new Promise(function (r) { release = r; }); } });
		const link = h.attach();
		await settle();
		const posted = h.posts.length;
		link.detach();
		release(jsonResponse(sessionBody()));
		await settle();
		out.detachInFlight = { posted: posted, sockets: h.ws.sockets.length, pending: h.clock.pending(),
			bridge: h.bus.get('bridge:w') === undefined ? null : 'present' };
	}

	// --- detach (and pagehide) while reconnecting or open leaves no timer behind ---
	{
		const h = fresh();
		const link = h.attach();
		const s = await openSocket(h);
		const open = h.clock.pending();
		s.serverClose(1006);
		const reconnecting = { state: h.bus.get('bridge:w').state, pending: h.clock.pending() };
		link.detach();
		const h2 = fresh();
		const listeners = {};
		h2.env.window.addEventListener = function (type, fn) { (listeners[type] = listeners[type] || []).push(fn); };
		h2.bus.wiring.attachBridges({ bridges: [
			{ id: 'w', transport: 'websocket', session: '/rest/ops/juneau-bus/session', downstream: ['app.a'], upstream: ['app.up'] }
		] }, { sourceOptions: h2.sourceOptions });
		const s2 = await openSocket(h2);
		s2.serverClose(1006);
		const before2 = h2.clock.pending();
		(listeners.pagehide || []).forEach(function (fn) { fn({ persisted: false }); });
		out.noTimers = { open: open > 0, reconnecting: reconnecting, afterDetach: h.clock.pending(),
			pagehideBefore: before2 > 0, afterPagehide: h2.clock.pending() };
	}

	process.stdout.write(JSON.stringify(out));
}

main().catch(function (e) { console.error(e && e.stack || e); process.exit(1); });
