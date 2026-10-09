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
 * bus-bridge-sse.cjs - Node harness for the stock SSE source (spec §11.3 step 2a, §11.6): session POST, fetch +
 * ReadableStream reader, SSE parse, frame decode, watchdog, and every HTTP status the source maps. Uses a fake
 * fetch (requests recorded, responses scripted), hand-fed streams and a fake clock. Every assertion lives in
 * ViewsJs_BusBridgeSse_Test.
 *
 *   Usage:  node bus-bridge-sse.cjs <juneau-bus.js>
 */
'use strict';

const path = require('node:path');
const { loadScripts, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));
const { fakeClock, settle } = require(path.join(__dirname, 'bus-fake-clock.cjs'));

const busJs = process.argv[2];
if (!busJs) {
	console.error('usage: node bus-bridge-sse.cjs <juneau-bus.js>');
	process.exit(2);
}

const SID = 'a'.repeat(64);
const LOCATION = { protocol: 'https:', host: 'console.example.test', origin: 'https://console.example.test' };
const enc = new TextEncoder();

/** A ReadableStream stand-in: the harness pushes text chunks; read() resolves as chunks arrive. */
function stream() {
	const queue = [];
	const waiting = [];
	const s = {
		cancelled: false,
		push: function (text) { deliver({ done: false, value: enc.encode(text) }); },
		end: function () { deliver({ done: true, value: undefined }); },
		fail: function (err) { deliver({ reject: err }); },
		body: {
			cancel: function () { s.cancelled = true; return Promise.resolve(); },
			getReader: function () {
				return {
					read: function () {
						if (queue.length) return settleStep(queue.shift());
						return new Promise(function (res, rej) { waiting.push({ res: res, rej: rej }); });
					},
					cancel: function () { s.cancelled = true; return Promise.resolve(); }
				};
			}
		}
	};
	function settleStep(step) { return step.reject ? Promise.reject(step.reject) : Promise.resolve(step); }
	function deliver(step) {
		if (!waiting.length) return queue.push(step);
		const w = waiting.shift();
		if (step.reject) w.rej(step.reject); else w.res(step);
	}
	return s;
}

function streamResponse(s, status) {
	return { ok: true, status: status || 200, headers: { get: function () { return null; } }, body: s.body };
}

function sessionBody(over) {
	return Object.assign({
		v: 1, sessionId: SID, heartbeatMs: 15000,
		downstream: [{ topic: 'app.a', retain: true }, { topic: 'app.ev', retain: false }],
		upstream: [], paths: { sse: '/rest/ops/juneau-bus/stream/' + SID, websocket: '/rest/ops/juneau-bus/ws/' + SID }
	}, over || {});
}

/**
 * A fresh bus with a scripted fetch. `script` is an array of functions (req) -> response|Promise, consumed in
 * order; requests are recorded as {method, url, headers, body}.
 */
function fresh(script, bodyAttrs) {
	const { NS } = loadScripts([busJs]);
	const bus = NS.bus;
	const clock = fakeClock();
	bus.config.timers = clock.timers;
	bus.declare('app.a', { retain: true });
	bus.declare('app.ev', { retain: false });
	const errors = [];
	bus.onError(function (e) { errors.push({ code: e.code, message: e.message, banner: !!(e.detail && e.detail.banner) }); });
	const requests = [];
	const signals = [];
	const queue = script.slice();
	const fetch = function (url, init) {
		const req = { method: init.method, url: url, headers: init.headers || {}, body: init.body ? JSON.parse(init.body) : null,
			credentials: init.credentials };
		requests.push(req);
		signals.push(init.signal || null);
		const next = queue.shift();
		if (!next) return Promise.reject(new Error('unscripted fetch ' + url));
		return Promise.resolve(next(req));
	};
	const attrs = bodyAttrs === undefined ? { 'data-juneau-csrf': 'tok-123' } : bodyAttrs;
	const document = { body: { getAttribute: function (n) { return Object.hasOwn(attrs, n) ? attrs[n] : null; } } };
	const states = [];
	bus.subscribe('bridge:s', function (p) { if (p) states.push(p.state); });
	function attach(over) {
		return bus.attachSource(bus.sources.sse(Object.assign({
			id: 's', session: '/rest/ops/juneau-bus/session', downstream: ['app.a', 'app.ev'],
			fetch: fetch, location: LOCATION, document: document, TextDecoder: TextDecoder, AbortController: AbortController
		}, over || {})));
	}
	return { bus: bus, clock: clock, errors: errors, requests: requests, signals: signals, states: states, attach: attach };
}

function sessionOk(over) { return function () { return jsonResponse(sessionBody(over)); }; }
function ev(frame) { return 'event: bus\ndata: ' + JSON.stringify(frame) + '\n\n'; }
const RESYNC = [{ v: 1, type: 'resync-begin', seq: 1 }, { v: 1, type: 'pub', topic: 'app.a', payload: { n: 1 }, retained: true, seq: 2 },
	{ v: 1, type: 'resync-end', seq: 3 }];
function codes(errors) { return errors.map(function (e) { return e.code; }); }

async function main() {
	const out = {};

	// --- happy path: POST shape, stream GET, chunked frames, state ---
	{
		const s = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s); }]);
		const got = [];
		h.bus.subscribe('app.ev', function (p, m) { got.push({ p: p, from: m.from, bridge: m.bridge }); });
		h.attach();
		await settle();
		const text = RESYNC.map(ev).join('') + ': keepalive\n\n' + ev({ v: 1, type: 'pub', topic: 'app.ev', payload: { e: 1 }, retained: false, seq: 4 });
		s.push(text.slice(0, 37));
		s.push(text.slice(37, 120));
		await settle();
		const stateMid = h.bus.get('bridge:s').state;
		s.push(text.slice(120));
		await settle();
		out.happy = {
			post: h.requests[0], get: { method: h.requests[1].method, url: h.requests[1].url, accept: h.requests[1].headers.Accept },
			stateMid: stateMid, states: h.states, a: h.bus.get('app.a'), ev: got, errors: codes(h.errors)
		};
	}

	// --- reconnect reuses the session; stream end retries with backoff ---
	{
		const s1 = stream();
		const s2 = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s1); }, function () { return streamResponse(s2); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s1.push(ev(f)); });
		await settle();
		s1.end();
		await settle();
		const st = h.bus.get('bridge:s');
		h.clock.advance(st.nextRetryMs);
		await settle();
		RESYNC.forEach(function (f) { s2.push(ev(f)); });
		await settle();
		out.reconnect = { methods: h.requests.map(function (r) { return r.method; }), afterEnd: { state: st.state, nextRetryMs: st.nextRetryMs,
			error: st.error && st.error.message }, final: h.bus.get('bridge:s').state, gap: h.bus.get('bridge:s').gap };
	}

	// --- stream 404 => capability refusal: re-POST immediately; three in a row => E-JS-57 ---
	{
		const nf = function () { return jsonResponse({ code: 'bus:unknown-session' }, { status: 404 }); };
		const h = fresh([sessionOk(), nf, sessionOk(), nf, sessionOk(), nf]);
		h.attach();
		for (let i = 0; i < 3; i++) { await settle(); h.clock.advance(0); }
		await settle();
		out.capability = { methods: h.requests.map(function (r) { return r.method; }), state: h.bus.get('bridge:s').state, errors: h.errors };
	}

	// --- status table: session POST and stream GET ---
	async function postStatus(status, body, headers) {
		const h = fresh([function () { return jsonResponse(body || { code: 'bus:x' }, { status: status, headers: headers }); }]);
		h.attach();
		await settle();
		const st = h.bus.get('bridge:s');
		return { state: st.state, nextRetryMs: st.nextRetryMs, error: st.error ? st.error.code : null, codes: codes(h.errors),
			message: h.errors.length ? h.errors[0].message : (st.error ? st.error.message : null) };
	}
	async function streamStatus(status, headers) {
		const h = fresh([sessionOk(), function () { return jsonResponse('', { status: status, headers: headers }); }]);
		h.attach();
		await settle();
		const st = h.bus.get('bridge:s');
		return { state: st.state, nextRetryMs: st.nextRetryMs, error: st.error ? st.error.code : null, codes: codes(h.errors) };
	}
	out.postStatus = {
		s403: await postStatus(403, { code: 'bus:topic-denied', message: 'not granted', denied: ['app.ev'] }),
		s400: await postStatus(400, { code: 'bus:bad-request' }),
		s429: await postStatus(429, null, { 'Retry-After': '7' }),
		s503: await postStatus(503),
		s501: await postStatus(501, { code: 'bus:transport-disabled' })
	};
	out.streamStatus = {
		s403: await streamStatus(403),
		s429: await streamStatus(429, { 'Retry-After': '12' }),
		s500: await streamStatus(500),
		noBody: await streamStatus(200)
	};

	// --- session-body validation: malformed id, grant mismatch, retain mismatch ---
	async function badSession(over) {
		const h = fresh([sessionOk(over)]);
		h.attach();
		await settle();
		return { state: h.bus.get('bridge:s').state, codes: codes(h.errors), message: h.errors.length ? h.errors[0].message : null,
			fetches: h.requests.length };
	}
	out.sessionBody = {
		badId: await badSession({ sessionId: 'abc' }),
		grant: await badSession({ downstream: [{ topic: 'app.a', retain: true }] }),
		retain: await badSession({ downstream: [{ topic: 'app.a', retain: false }, { topic: 'app.ev', retain: false }] }),
		crossOriginStream: await badSession({ paths: { sse: 'https://evil.example.test/s' } })
	};

	// --- CSRF: custom header name, blank token (fail closed, no fetch), no attribute (no header) ---
	{
		const h1 = fresh([sessionOk(), function () { return new Promise(function () {}); }],
			{ 'data-juneau-csrf': 'tok-9', 'data-juneau-csrf-header': 'X-App-Csrf' });
		h1.attach();
		await settle();
		const h2 = fresh([], { 'data-juneau-csrf': '   ' });
		h2.attach();
		await settle();
		const h3 = fresh([sessionOk(), function () { return new Promise(function () {}); }], {});
		h3.attach();
		await settle();
		out.csrf = {
			customHeader: h1.requests[0].headers['X-App-Csrf'] || null, defaultAbsent: h1.requests[0].headers['X-Csrf-Token'] === undefined,
			blank: { fetches: h2.requests.length, state: h2.bus.get('bridge:s').state, codes: codes(h2.errors), message: h2.errors.length ? h2.errors[0].message : null },
			none: Object.keys(h3.requests[0].headers).sort()
		};
	}

	// --- session URL must be same-origin (E-JS-52) ---
	{
		const h = fresh([]);
		h.attach({ session: 'https://other.example.test/rest/ops/juneau-bus/session' });
		await settle();
		const h2 = fresh([]);
		h2.attach({ session: '//other.example.test/x' });
		await settle();
		out.crossOrigin = { fetches: h.requests.length + h2.requests.length, codes: codes(h.errors).concat(codes(h2.errors)),
			message: h.errors.length ? h.errors[0].message : null };
	}

	// --- opts.csrf: custom header; a blank token is refused before any request ---
	{
		const h1 = fresh([sessionOk(), function () { return new Promise(function () {}); }], {});
		h1.attach({ csrf: { header: 'X-Own', token: 'tok-own' } });
		await settle();
		const h2 = fresh([], {});
		h2.attach({ csrf: { token: '  ' } });
		await settle();
		out.csrfOpts = { headers: h1.requests[0].headers, blank: { fetches: h2.requests.length, state: h2.bus.get('bridge:s').state,
			codes: codes(h2.errors) } };
	}

	// --- control characters cannot smuggle a cross-origin path past the same-origin check ---
	{
		const urls = ['/\t/evil.example.test/x', '/\n/evil.example.test/x', '/ok\u0000', '/\\evil.example.test/x'];
		let fetches = 0;
		const cs = [];
		for (const u of urls) {
			const h = fresh([]);
			h.attach({ session: u });
			await settle();
			fetches += h.requests.length;
			cs.push(codes(h.errors)[0] || null);
		}
		out.controlChars = { fetches: fetches, codes: cs };
	}

	// --- detach while the stream GET is still pending: the late response is cancelled, the request aborted ---
	{
		const s = stream();
		let release;
		const h = fresh([sessionOk(), function () { return new Promise(function (r) { release = r; }); }]);
		const link = h.attach();
		await settle();
		const signal = h.signals[1];
		const abortedBefore = !!(signal && signal.aborted);
		link.detach();
		const abortedAfter = !!(signal && signal.aborted);
		release(streamResponse(s));
		await settle();
		out.detachPending = { hasSignal: !!signal, abortedBefore: abortedBefore, abortedAfter: abortedAfter, cancelled: s.cancelled };
	}

	// --- an oversized SSE event is dropped (E-JS-58 too-large) and the stream carries on ---
	{
		const s = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		s.push('event: bus\ndata: ' + 'x'.repeat(70000) + '\n\n');
		s.push('event: bus\ndata: ' + 'y'.repeat(40000));
		s.push('y'.repeat(40000));
		s.push('\n\n');
		s.push(ev({ v: 1, type: 'pub', topic: 'app.a', payload: { n: 2 }, retained: true, seq: 9 }));
		await settle();
		out.oversize = { codes: codes(h.errors), messages: h.errors.map(function (e) { return e.message; }),
			state: h.bus.get('bridge:s').state, a: h.bus.get('app.a') };
	}

	// --- a rejected read() ends the stream as a retryable E-JS-56 close ---
	{
		const s = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		await settle();
		s.fail(new Error('network reset'));
		await settle();
		const st = h.bus.get('bridge:s');
		out.readFails = { state: st.state, nextRetryMs: st.nextRetryMs, error: st.error && st.error.message, codes: codes(h.errors) };
	}

	// --- watchdog: 3 x heartbeatMs with no bytes kills the stream ---
	async function watchdogAt(ms) {
		const s = stream();
		const h = fresh([sessionOk({ heartbeatMs: 15000 }), function () { return streamResponse(s); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		await settle();
		h.clock.advance(ms);
		await settle();
		const st = h.bus.get('bridge:s');
		return { state: st.state, cancelled: s.cancelled, error: st.error ? st.error.message : null };
	}
	out.watchdog = { at44999: await watchdogAt(44999), at45000: await watchdogAt(45000) };
	{
		// a comment line (server heartbeat) feeds the watchdog too
		const s = stream();
		const h = fresh([sessionOk({ heartbeatMs: 15000 }), function () { return streamResponse(s); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		await settle();
		h.clock.advance(30000);
		s.push(': hb\n\n');
		await settle();
		h.clock.advance(30000);
		await settle();
		out.watchdog.fedByComment = h.bus.get('bridge:s').state;
	}

	// --- per-message problems keep the stream up: bad frame, error frames, ungranted topic, foreign events ---
	{
		const s = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s); }]);
		h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		s.push('event: bus\ndata: {nope\n\n');
		s.push(ev({ v: 1, type: 'error', code: 'bus:upstream-denied', message: 'not granted', topic: 'app.up' }));
		s.push(ev({ v: 1, type: 'error', code: 'bus:boom', message: 'kaput' }));
		s.push(ev({ v: 1, type: 'error', code: 'bus:slow-consumer', message: 'queue full' }));
		s.push(ev({ v: 1, type: 'pub', topic: 'app.secret', payload: 1, retained: true, seq: 9 }));
		s.push('event: other\ndata: {"v":1,"type":"pub","topic":"app.a","payload":{"n":99},"retained":true,"seq":10}\n\n');
		s.push('data: {"v":1,"type":"pub","topic":"app.a","payload":{"n":98},"retained":true,"seq":11}\n\n');
		await settle();
		out.perMessage = { codes: codes(h.errors), messages: h.errors.map(function (e) { return e.message; }),
			banners: h.errors.map(function (e) { return e.banner; }), state: h.bus.get('bridge:s').state, a: h.bus.get('app.a'),
			secret: h.bus.get('app.secret') === undefined ? null : 'leaked', cancelled: s.cancelled };
	}

	// --- detach cancels the reader and clears retained topics ---
	{
		const s = stream();
		const h = fresh([sessionOk(), function () { return streamResponse(s); }]);
		const link = h.attach();
		await settle();
		RESYNC.forEach(function (f) { s.push(ev(f)); });
		await settle();
		link.detach();
		s.push(ev({ v: 1, type: 'pub', topic: 'app.a', payload: { n: 5 }, retained: true, seq: 4 }));
		await settle();
		out.detach = { cancelled: s.cancelled, a: h.bus.get('app.a') === undefined ? null : h.bus.get('app.a'),
			bridge: h.bus.get('bridge:s') === undefined ? null : 'present' };
	}

	process.stdout.write(JSON.stringify(out));
}

main().catch(function (e) { console.error(e && e.stack || e); process.exit(1); });
