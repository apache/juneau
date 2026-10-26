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
 * In-page fakes for console-bus.cjs.  Installs window.__busTest:
 *   fetches    every request the page made: {url, path, method, headers (lower-cased), body, request (parsed body)};
 *              session POSTs and stream GETs are recorded here by the fake fetch, /api/ requests by the driver
 *   streams    one handle per SSE stream GET: {push(text), end()}
 *   renders    recorder card render counts by card id
 *   roleCalls  recorder role calls
 *   clock      a virtual clock for JuneauViews.bus.config.timers; advance(ms) fires due timers in order
 * window.__busTestConfig (written by ConsoleBusBrowserSupport) selects the session-POST answer.  /api/ answers come
 * from the driver (Playwright routing), because DataTables fetches through XMLHttpRequest.
 */
(function () {
	const config = window.__busTestConfig || {};
	const T = window.__busTest = { config, fetches: [], streams: [], renders: {}, roleCalls: [], now: 0, timers: [] };

	let nextTimer = 1;
	T.clock = {
		setTimeout(fn, ms) { const id = nextTimer++; T.timers.push({ id, at: T.now + (ms || 0), fn }); return id; },
		clearTimeout(id) { T.timers = T.timers.filter(t => t.id !== id); },
		now() { return T.now; },
		random() { return 0.5; }          // jitter factor 0.8 + 0.4 * 0.5 = 1.0: backoff delays are exact
	};
	T.advance = function (ms) {
		const until = T.now + ms;
		for (;;) {
			T.timers.sort((a, b) => a.at - b.at);
			const t = T.timers[0];
			if (!t || t.at > until)
				break;
			T.timers.shift();
			T.now = t.at;
			t.fn();
		}
		T.now = until;
	};

	function parseBody(body) {
		if (body == null || body === '')
			return {};
		try {
			return JSON.parse(body);
		} catch (e) {
			const o = {};
			new URLSearchParams(body).forEach((v, k) => { o[k] = v; });
			return o;
		}
	}

	function json(value, status) {
		return new Response(JSON.stringify(value), { status: status || 200, headers: { 'Content-Type': 'application/json' } });
	}

	function session(body) {
		const c = config.session || {};
		if (c.status && c.status !== 200)
			return json(c.body || {}, c.status);
		const req = JSON.parse(body);
		const id = 'a'.repeat(64);
		return json({
			v: 1, sessionId: id, heartbeatMs: 15000, graceMs: 60000,
			downstream: req.downstream.map(t => ({ topic: t, retain: true })),
			upstream: req.upstream || [],
			paths: { sse: '/rest/ops/jobs/juneau-bus/stream/' + id }
		});
	}

	function stream() {
		let ctl;
		const enc = new TextEncoder();
		const body = new ReadableStream({ start(c) { ctl = c; } });
		T.streams.push({ push(text) { ctl.enqueue(enc.encode(text)); }, end() { ctl.close(); } });
		return new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
	}

	const realFetch = window.fetch.bind(window);
	window.fetch = async function (input, init) {
		init = init || {};
		const raw = typeof input === 'string' ? input : input.url;
		const u = new URL(raw, location.href);
		const isSession = u.pathname.endsWith('/juneau-bus/session');
		const isStream = u.pathname.includes('/juneau-bus/stream/');
		if (!isSession && !isStream)
			return realFetch(input, init);
		const method = (init.method || 'GET').toUpperCase();
		const headers = {};
		new Headers(init.headers || {}).forEach((v, k) => { headers[k] = v; });
		const body = typeof init.body === 'string' ? init.body : null;
		T.fetches.push({ url: u.pathname + u.search, path: u.pathname, method, headers, body, request: parseBody(body) });
		return isSession ? session(body) : stream();
	};
})();
