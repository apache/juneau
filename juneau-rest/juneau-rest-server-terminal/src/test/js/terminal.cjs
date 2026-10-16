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
 * terminal.cjs - the terminal runtime's engine harness: the poll loop, the write queue and the step-line map,
 * against a fake fetch, a fake clock and an xterm stub that records writes and holds their callbacks.  The x-cases
 * run the same engine against the real xterm.js bundle, without a DOM.
 *
 * Every case gets a FRESH load of juneau-terminal.js and passes by returning without throwing; the report maps the
 * case name to true, or to the failure's stack.
 *
 *   Usage:  node terminal.cjs <juneau-terminal.js> <xterm.js>
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const argv = process.argv.slice(2);
if (argv.length < 2) {
	process.stderr.write('usage: terminal.cjs <juneau-terminal.js> <xterm.js>\n');
	process.exit(2);
}
const TERMINAL_JS = fs.readFileSync(argv[0], 'utf8');
const XTERM_JS = fs.readFileSync(argv[1], 'utf8');

const cases = [];
function test(name, fn) { cases.push({ name: name, fn: fn }); }
function expect(cond, detail) {
	if (!cond) throw new Error(typeof detail === 'string' ? detail : JSON.stringify(detail));
}
function expectSame(actual, expected, what) {
	expect(JSON.stringify(actual) === JSON.stringify(expected),
		(what || 'value') + ': expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual));
}

const enc = new TextEncoder();
const dec = new TextDecoder();

/** Runs every pending microtask and promise job. */
function settle() { return new Promise(function (r) { setImmediate(r); }); }

/** Lets the real xterm's write callbacks (setTimeout-driven) run. */
function settleReal() { return new Promise(function (r) { setTimeout(r, 30); }); }

/** A fresh window with juneau-terminal.js loaded; warnings are recorded. */
function load() {
	const warns = [];
	const g = {
		TextEncoder: TextEncoder, AbortController: AbortController, URL: URL, Promise: Promise,
		console: { warn: function () { warns.push(Array.from(arguments).join(' ')); }, error: function () { warns.push(Array.from(arguments).join(' ')); }, log: function () {} }
	};
	g.window = g;
	vm.createContext(g);
	vm.runInContext(TERMINAL_JS, g);
	return { win: g, JT: g.JuneauTerminal, warns: warns };
}

/** The real xterm Terminal constructor, from the UMD bundle in its own context. */
function realTerminal() {
	const g = { console: console, setTimeout: setTimeout, clearTimeout: clearTimeout, queueMicrotask: queueMicrotask,
		navigator: { userAgent: 'node', platform: 'MacIntel', language: 'en-US' }, document: {}, performance: performance };
	g.window = g;
	g.self = g;
	vm.createContext(g);
	vm.runInContext(XTERM_JS, g);
	return g.Terminal;
}

/** Timers that run only when advanced. */
function fakeClock() {
	let now = 0, seq = 0;
	const timers = [];
	return {
		setTimeout: function (fn, ms) { const id = ++seq; timers.push({ id: id, at: now + (ms || 0), fn: fn }); return id; },
		clearTimeout: function (id) { const i = timers.findIndex(function (t) { return t.id === id; }); if (i >= 0) timers.splice(i, 1); },
		pending: function () { return timers.map(function (t) { return t.at - now; }); },
		now: function () { return now; },
		advance: function (ms) {
			const until = now + ms;
			for (;;) {
				timers.sort(function (a, b) { return a.at - b.at || a.id - b.id; });
				if (!timers.length || timers[0].at > until) break;
				const t = timers.shift();
				now = t.at;
				t.fn();
			}
			now = until;
		}
	};
}

/**
 * An xterm stub.  Each written \n moves the cursor down a row.  With hold=true, write callbacks wait for flush();
 * otherwise they run as a microtask.
 */
function stubTerm(cols, rows, hold) {
	const t = {
		cols: cols, rows: rows, writes: [], held: [], resets: 0, scrolledTo: [], bottoms: 0, cursor: 0, hold: !!hold,
		buffer: { normal: { baseY: 0, cursorY: 0 } },
		write: function (bytes, cb) {
			const copy = Uint8Array.from(bytes);
			t.writes.push(copy);
			const apply = function () {
				for (const b of copy) if (b === 0x0a) t.cursor++;
				t.buffer.normal.cursorY = t.cursor;
				cb();
			};
			if (t.hold) t.held.push(apply);
			else queueMicrotask(apply);
		},
		flush: function () { const f = t.held.shift(); if (f) f(); return !!f; },
		reset: function () { t.resets++; t.cursor = 0; t.buffer.normal.cursorY = 0; },
		scrollToLine: function (l) { t.scrolledTo.push(l); },
		scrollToBottom: function () { t.bottoms++; },
		texts: function () { return t.writes.map(function (w) { return dec.decode(w); }); }
	};
	return t;
}

/** A bytes response.  h: {next, end, done, truncated, error, cols, rows}. */
function bytesRes(body, h) {
	const b = enc.encode(body);
	const hdr = {
		'Term-Next': String(h.next), 'Term-End': String(h.end === undefined ? h.next : h.end),
		'Term-Done': String(!!h.done), 'Term-Cols': String(h.cols || 80), 'Term-Rows': String(h.rows || 24),
		'Term-Truncated': String(!!h.truncated)
	};
	if (h.error) hdr['Term-Error'] = h.error;
	return {
		status: 200, ok: true,
		headers: { get: function (k) { return Object.prototype.hasOwnProperty.call(hdr, k) ? hdr[k] : null; } },
		arrayBuffer: function () { return Promise.resolve(b.buffer.slice(b.byteOffset, b.byteOffset + b.length)); }
	};
}

function statusRes(status, h) {
	const r = bytesRes('', h || { next: 0 });
	r.status = status;
	r.ok = status >= 200 && status < 300;
	return r;
}

function pageRes(events, next, o) {
	const p = Object.assign({ contractVersion: '1', events: events, next: next, more: false, terminal: false }, o || {});
	return { status: 200, ok: true, headers: { get: function () { return null; } }, json: function () { return Promise.resolve(p); } };
}

/**
 * An engine over a scripted server.  route(url) returns a response, a response promise, or throws to reject.  Every
 * requested URL is recorded in c.urls.
 */
function start(t, opts, route, termOpts) {
	const clock = fakeClock();
	const c = { urls: [], clock: clock, statuses: [], hidden: false, visibleFns: [], term: null };
	const env = {
		fetch: function (url) {
			c.urls.push(url);
			return Promise.resolve().then(function () { return route(url, c); });
		},
		setTimeout: clock.setTimeout,
		clearTimeout: clock.clearTimeout,
		hidden: function () { return c.hidden; },
		onVisible: function (fn) {
			c.visibleFns.push(fn);
			return function () { const i = c.visibleFns.indexOf(fn); if (i >= 0) c.visibleFns.splice(i, 1); };
		},
		open: function (cols, rows) {
			c.term = termOpts && termOpts.real ? new termOpts.real({ cols: cols, rows: rows, scrollback: 100000, allowProposedApi: false })
				: stubTerm(cols, rows, termOpts && termOpts.hold);
			c.opened = [cols, rows];
			return c.term;
		},
		onStatus: function (st) { c.statuses.push(st); }
	};
	c.engine = t.JT.createEngine(Object.assign({ bytesUrl: '/t/bytes' }, opts || {}), env);
	c.status = function () { return c.engine.status(); };
	c.show = function () { c.hidden = false; c.visibleFns.slice().forEach(function (f) { f(); }); };
	c.engine.start();
	return c;
}

/** A byte log served from a string: from/max honoured, done once `done` is set. */
function logServer(log, o) {
	const srv = Object.assign({ log: log, done: false, max: 1 << 18 }, o || {});
	srv.route = function (url) {
		const m = /from=(\d+)(?:&max=(\d+))?/.exec(url);
		const from = Number(m[1]);
		const max = m[2] === undefined ? srv.max : Number(m[2]);
		if (from > srv.log.length) return statusRes(416, { next: 0, end: srv.log.length });
		const body = srv.log.slice(from, from + max);
		const next = from + body.length;
		return bytesRes(body, { next: next, end: srv.log.length, done: srv.done && next === srv.log.length });
	};
	return srv;
}

// @cases:pure

test('f01_fontFitArithmeticAndClamp', function (t) {
	expect(t.JT.fitFont(14, 700, 980) === 10, 'floor(14*700/980) = 10: ' + t.JT.fitFont(14, 700, 980));
	expect(t.JT.fitFont(14, 1000, 700) === 20, 'floor(20) at the max');
	expect(t.JT.fitFont(14, 3000, 700) === 20, 'clamped to 20');
	expect(t.JT.fitFont(14, 100, 700) === 8, 'clamped to 8');
	expect(t.JT.fitFont(14, 0, 700) === 14 && t.JT.fitFont(14, 700, 0) === 14, 'an unmeasured panel keeps the size');
});

test('f02_parseHashAndFormatSize', function (t) {
	expect(t.JT.parseHash('#build-O3400', 'build') === 3400, 'offset');
	expect(t.JT.parseHash('#build-O0', 'build') === 0, 'zero');
	for (const bad of ['#build-O', '#build-O-1', '#build-O1.5', '#other-O3', '#build-O12345678901234567', '', null])
		expect(t.JT.parseHash(bad, 'build') === null, 'rejects ' + bad);
	expect(t.JT.parseHash('#-O3', '') === null, 'no id, no hash link');
	expectSame([2 * 1024 * 1024, 1536 * 1024, 512 * 1024, 10].map(t.JT.formatSize), ['2 MiB', '1.5 MiB', '512 KiB', '10 B'], 'sizes');
});

test('f04_linkHandlerOpensOnlyHttp', function (t) {
	const opened = [];
	const h = t.JT.linkHandlerFor({ open: function (u, w, f) { opened.push([u, w, f]); } });
	expect(h.allowNonHttpProtocols !== true, 'xterm keeps dropping non-HTTP links');
	for (const bad of ['javascript:alert(1)', 'data:text/html,x', 'file:///etc/passwd', 'not a url', ''])
		h.activate(null, bad);
	h.activate(null, 'https://example.org/ok');
	h.activate(null, 'http://example.org/a b');
	expectSame(opened, [['https://example.org/ok', '_blank', 'noopener,noreferrer'], ['http://example.org/a%20b', '_blank', 'noopener,noreferrer']], 'opened');
});

test('f03_findNextWrapsBothWays', function (t) {
	const rows = ['alpha', 'beta ERR one', 'gamma', 'err two'];
	const buf = { length: rows.length, getLine: function (y) { return { translateToString: function () { return rows[y]; } }; } };
	const a = t.JT.findNext(buf, 'err', null, false);
	expectSame(a, { y: 1, x: 5, length: 3 }, 'first, case-insensitive');
	expectSame(t.JT.findNext(buf, 'err', a, false), { y: 3, x: 0, length: 3 }, 'next');
	expectSame(t.JT.findNext(buf, 'err', { y: 3, x: 0 }, false), { y: 1, x: 5, length: 3 }, 'wraps forward');
	expectSame(t.JT.findNext(buf, 'err', null, true), { y: 3, x: 0, length: 3 }, 'backward starts at the bottom');
	expectSame(t.JT.findNext(buf, 'err', { y: 3, x: 0 }, true), { y: 1, x: 5, length: 3 }, 'previous');
	expect(t.JT.findNext(buf, 'zzz', null, false) === null && t.JT.findNext(buf, '', null, false) === null, 'no match');
});

test('h01_handleIsNullUntilMounted', function (t) {
	expect(typeof t.JT.handle === 'function', 'handle is exported');
	expect(t.JT.handle('t') === null && t.JT.handle(undefined) === null, 'no handle for a card that is not mounted');
});

test('c01_cardHandlerIsQueued', function (t) {
	const q = t.win.JuneauConsoleCards;
	expect(Array.isArray(q) && q.length === 1 && q[0][0] === 'terminal', 'queued as ["terminal", handler]');
	expect(typeof q[0][1].render === 'function' && typeof q[0][1].destroy === 'function', 'render and destroy');
	const pushed = [];
	const g = { TextEncoder: TextEncoder, AbortController: AbortController, URL: URL, console: console };
	g.window = g;
	g.JuneauConsoleCards = { push: function (e) { pushed.push(e[0]); } };
	vm.createContext(g);
	vm.runInContext(TERMINAL_JS, g);
	expectSame(pushed, ['terminal'], 'a shell that has already drained gets push()');
});

// @cases:poll

test('w01_writesAreSerialized', async function (t) {
	const srv = logServer('aaa\nbbb\nccc');
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		if (url.startsWith('/t/events')) return pageRes([{ ev: 'step', id: 's', title: 'S', rawOffset: 4 }], '1');
		return srv.route(url);
	}, { hold: true });
	await settle();
	expectSame(c.opened, [80, 24], 'opened at the source size');
	expectSame(c.term.texts(), ['aaa\n'], 'only the first segment before its callback');
	const urls = c.urls.length;
	await settle();
	expect(c.term.writes.length === 1 && c.urls.length === urls, 'no second write and no fetch while the callback is held');
	c.term.flush();
	await settle();
	expectSame(c.term.texts(), ['aaa\n', 'bbb\nccc'], 'the second segment after the first callback');
	expect(c.urls.length === urls, 'still no fetch before the last callback');
	c.term.flush();
	await settle();
	expect(c.clock.pending().length === 1, 'the next poll is scheduled after the last callback');
});

test('p01_catchUpFetchesAtOnce', async function (t) {
	const srv = logServer('0123456789', { max: 4 });
	const c = start(t, {}, srv.route);
	await settle();
	expectSame(c.urls, ['/t/bytes?from=0&max=0', '/t/bytes?from=0', '/t/bytes?from=4', '/t/bytes?from=8'], 'no clock between catch-up fetches');
	expectSame(c.term.texts(), ['0123', '4567', '89'], 'every chunk written in order');
	expectSame(c.clock.pending(), [1000], 'then refreshMs');
});

test('p02_idleBacksOffAfterThreeEmptyPolls', async function (t) {
	const srv = logServer('abc');
	const c = start(t, { refreshMs: 1500 }, srv.route);
	await settle();
	expectSame(c.clock.pending(), [1500], 'refreshMs after new bytes');
	for (let i = 1; i <= 3; i++) {
		c.clock.advance(c.clock.pending()[0]);
		await settle();
		expectSame(c.clock.pending(), [i < 3 ? 1500 : 5000], 'after empty poll ' + i);
	}
	srv.log += 'd';
	c.clock.advance(5000);
	await settle();
	expectSame(c.clock.pending(), [1500], 'new bytes reset the backoff');
	expect(c.urls[c.urls.length - 1] === '/t/bytes?from=3', 'resumes from next');
});

test('p03_stopsOnDone', async function (t) {
	const srv = logServer('all\n', { done: true });
	const c = start(t, {}, srv.route);
	await settle();
	expect(c.status().badge === 'Done' && c.status().kind === 'ok', 'done badge: ' + JSON.stringify(c.status()));
	expect(c.status().stopped && c.clock.pending().length === 0, 'nothing scheduled');
	const n = c.urls.length;
	c.clock.advance(60000);
	await settle();
	expect(c.urls.length === n, 'no more requests');
});

test('p04_exitBadgeFromEvents', async function (t) {
	const srv = logServer('x\n', { done: true });
	const evs = [{ ev: 'step', id: 't', title: 'T', rawOffset: 0 }, { ev: 'note', level: 'warn', text: 't killed by SIGKILL' },
		{ ev: 'end', id: 't', status: 'fail', ms: 5, exit: 137 }, { ev: 'done', status: 'fail' }];
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes(evs, '4', { terminal: true }) : srv.route(url);
	});
	await settle();
	expect(c.status().badge === 'exit 137 (SIGKILL)' && c.status().kind === 'fail', 'badge: ' + c.status().badge);
	expect(c.urls[0] === '/t/events', 'a finished run polls its events before any bytes: ' + c.urls[0]);
});

test('p05_bytesDoneWaitsForTheEvents', async function (t) {
	const srv = logServer('x\n', { done: true });
	let terminal = false;
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes(terminal ? [{ ev: 'done', status: 'ok' }] : [], terminal ? '2' : '1', { terminal: terminal }) : srv.route(url);
	});
	await settle();
	expect(c.status().badge === 'Running' && c.clock.pending().length === 1, 'still polling the events');
	terminal = true;
	c.clock.advance(1000);
	await settle();
	expect(c.urls[c.urls.length - 1] === '/t/events?after=1', 'after= the last token');
	expect(c.status().badge === 'Done' && c.status().stopped, 'done once the events end');
	expect(c.urls.filter(function (u) { return u.startsWith('/t/bytes?from=2'); }).length === 0, 'no bytes request once bytes are done');
});

test('v01_hiddenTabPausesAndResumes', async function (t) {
	const srv = logServer('ab');
	const c = start(t, {}, srv.route);
	await settle();
	c.hidden = true;
	const n = c.urls.length;
	c.clock.advance(1000);
	await settle();
	expect(c.urls.length === n && c.visibleFns.length === 1, 'a due poll waits while hidden');
	srv.log += 'cd';
	c.show();
	await settle();
	expect(c.urls[n] === '/t/bytes?from=2', 'resumes from next: ' + c.urls[n]);
	expect(c.visibleFns.length === 0, 'the visibility listener is removed');
});

test('p06_eventsThatNeverFinishGiveUp', async function (t) {
	const srv = logServer('x\n', { done: true });
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([], '1') : srv.route(url);
	});
	await settle();
	expect(c.status().badge === 'Running' && !c.status().stopped, 'waiting for the events: ' + c.status().badge);
	const delays = [];
	for (let i = 0; i < 10 && !c.status().stopped; i++) {
		delays.push(c.clock.pending()[0]);
		c.clock.advance(c.clock.pending()[0]);
		await settle();
	}
	expectSame(delays, [1000, 1000, 1000, 5000, 5000], 'idle back-off once the bytes are done');
	expect(c.status().stopped && c.status().badge === 'Done' && c.status().kind === 'ok', 'final badge, no exit code: ' + JSON.stringify(c.status()));
	expect(c.clock.pending().length === 0, 'nothing scheduled');
	expect(c.urls.filter(function (u) { return u.startsWith('/t/events'); }).length === 6, 'six event polls: ' + c.urls.join(' '));
});

test('p07_zeroProgressCatchUpStops', async function (t) {
	const c = start(t, {}, function () { return bytesRes('', { next: 0, end: 5 }); });
	await settle();
	expect(c.urls.length === 2, 'probe and one poll, no spin: ' + c.urls.join(' '));
	expect(c.clock.pending().length === 1, 'then the normal schedule');
});

// @cases:errors

test('r01_retryLadderKeepsTheTerminal', async function (t) {
	const srv = logServer('ok\n');
	let failing = 0;
	const c = start(t, {}, function (url) {
		if (failing > 0) { failing--; throw new TypeError('Failed to fetch'); }
		return srv.route(url);
	});
	await settle();
	failing = 5;
	const delays = [];
	c.clock.advance(1000);
	await settle();
	for (let i = 0; i < 5; i++) {
		delays.push(c.clock.pending()[0]);
		expect(c.status().reconnecting, 'reconnecting chip');
		c.clock.advance(c.clock.pending()[0]);
		await settle();
	}
	expectSame(delays, [1000, 2000, 5000, 10000, 10000], 'the ladder');
	expect(!c.status().reconnecting, 'chip cleared on success');
	expect(c.term.resets === 0, 'never reset');
	expect(c.urls[c.urls.length - 1] === '/t/bytes?from=3', 'resumes from the last good next');
	srv.log += 'more';
	c.clock.advance(c.clock.pending()[0]);
	await settle();
	expectSame(c.term.texts(), ['ok\n', 'more'], 'nothing replayed');
});

test('r02_fiveHundredRetriesAndFourHundredStops', async function (t) {
	let code = 503;
	const srv = logServer('a');
	const c = start(t, {}, function (url) { return url.includes('max=0') ? srv.route(url) : statusRes(code); });
	await settle();
	expect(c.status().reconnecting && c.clock.pending()[0] === 1000, '5xx retries');
	code = 403;
	c.clock.advance(1000);
	await settle();
	expect(c.status().badge === 'HTTP 403' && c.status().kind === 'error' && c.status().stopped, 'other 4xx stops: ' + c.status().badge);
});

test('r03_416ResetsAndReplays', async function (t) {
	const srv = logServer('first\n');
	const c = start(t, {}, srv.route);
	await settle();
	srv.log = 'new\n';
	c.clock.advance(1000);
	await settle();
	expect(c.term.resets === 1, 'xterm reset');
	expectSame(c.urls.slice(-3), ['/t/bytes?from=6', '/t/bytes?from=0&max=0', '/t/bytes?from=0'], '416, probe, replay');
	expectSame(c.term.texts(), ['first\n', 'new\n'], 'replayed from 0');
});

test('r04_notFoundAndGone', async function (t) {
	const nf = start(t, {}, function () { return statusRes(404); });
	await settle();
	expect(nf.status().badge === 'Terminal not found' && nf.status().stopped && nf.term === null, 'not found: ' + nf.status().badge);
	const srv = logServer('kept\n');
	let gone = false;
	// The server's Term-Next on gone is from (5); this one sends 0 to show the client does not rewind on it.
	const g = start(t, {}, function (url) {
		return gone ? bytesRes('', { next: 0, end: 0, done: true, error: 'gone' }) : srv.route(url);
	});
	await settle();
	gone = true;
	g.clock.advance(1000);
	await settle();
	expect(g.status().badge === 'Source unavailable' && g.status().kind === 'error' && g.status().stopped, 'gone: ' + g.status().badge);
	expect(g.term.resets === 0 && g.term.texts().join('') === 'kept\n', 'what was rendered is kept');
	expect(g.status().next === 5, 'next stays at what was read: ' + g.status().next);
	const polls = g.urls.length;
	g.clock.advance(60000);
	await settle();
	expect(g.urls.length === polls, 'no poll after gone: ' + g.urls.slice(polls).join(' '));
});

test('r05_events410RefetchesFromTheStart', async function (t) {
	const srv = logServer('');
	let n = 0;
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		if (!url.startsWith('/t/events')) return srv.route(url);
		n++;
		if (url.includes('after=')) return n === 2 ? statusRes(410) : pageRes([], 'b');
		return pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 0 }], 'a');
	});
	await settle();
	c.clock.advance(1000);
	await settle();
	expectSame(c.urls.filter(function (u) { return u.startsWith('/t/events'); }), ['/t/events', '/t/events?after=a', '/t/events'], '410 drops the token');
});

test('r06_eventsUnavailableTurnsStepsOff', async function (t) {
	const srv = logServer('x');
	const c = start(t, { eventsUrl: '/t/events' }, function (url) { return url.startsWith('/t/events') ? statusRes(404) : srv.route(url); });
	await settle();
	expect(c.term.texts().join('') === 'x', 'bytes still render');
	expect(t.warns.some(function (w) { return w.includes('steps are off'); }), 'warned: ' + JSON.stringify(t.warns));
	c.clock.advance(1000);
	await settle();
	expect(c.urls.filter(function (u) { return u.startsWith('/t/events'); }).length === 1, 'not polled again');
});

test('r07_malformedEventsPageTurnsStepsOff', async function (t) {
	const srv = logServer('x');
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		if (!url.startsWith('/t/events'))
			return srv.route(url);
		const r = pageRes([], '1');
		r.json = function () { return Promise.reject(new SyntaxError('Unexpected token <')); };
		return r;
	});
	await settle();
	expect(c.term.texts().join('') === 'x' && !c.status().reconnecting, 'bytes render, no retry ladder');
	expect(t.warns.some(function (w) { return w.includes('steps are off'); }), 'warned: ' + JSON.stringify(t.warns));
});

// @cases:ring

test('t01_truncationSequenceBytes', async function (t) {
	const c = start(t, {}, function (url) {
		if (url.includes('max=0')) return bytesRes('', { next: 0, end: 20 });
		if (url === '/t/bytes?from=0') return bytesRes('tail\n', { next: 20, end: 20, truncated: true });
		return bytesRes('', { next: 20, end: 20 });
	});
	await settle();
	expectSame(c.term.texts(), ['\u001b[0m\u001b[?1049l\u001b[2m[… 15 bytes dropped …]\u001b[0m\r\n', 'tail\n'], 'reset, alt-screen exit, dim notice, then the bytes');
	expect(c.status().next === 20, 'carries on from next');
});

test('t02_trimmedStartBannerAndLoadAll', async function (t) {
	const srv = logServer('0123456789\nabc\ndefghij\n');
	const c = start(t, { tailBytes: 10, eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 0 }, { ev: 'step', id: 'b', title: 'B', rawOffset: 15 }], '2', { terminal: true }) : srv.route(url);
	});
	await settle();
	expect(c.urls[2] === '/t/bytes?from=13', 'starts at end - tailBytes: ' + c.urls[2]);
	expectSame(c.term.texts(), ['defghij\n'], 'moved forward to just after the next newline');
	expect(c.status().trimmedFrom === 15, 'trimmedFrom: ' + c.status().trimmedFrom);
	expectSame(c.status().banner, { text: 'Showing the last 10 B', slow: null }, 'banner');
	expect(c.engine.lineFor(15) === 0 && c.engine.lineFor(0) === null, 'only the shown step has a line');
	c.engine.loadAll();
	await settle();
	expect(c.term.resets === 1 && c.status().banner === null && c.status().trimmedFrom === null, 'reset, no banner');
	expectSame(c.term.texts().slice(1), ['0123456789\nabc\n', 'defghij\n'], 'replayed from 0, split at the step');
	expect(c.engine.lineFor(0) === 0 && c.engine.lineFor(15) === 2, 'map rebuilt: ' + c.engine.lineFor(15));
});

test('t03_slowLoadAllWarning', async function (t) {
	const big = 21 * 1024 * 1024;
	const c = start(t, {}, function (url) {
		return url.includes('max=0') ? bytesRes('', { next: 0, end: big }) : bytesRes('\nx', { next: big, end: big });
	});
	await settle();
	expectSame(c.status().banner, { text: 'Showing the last 2 MiB', slow: 'The full log is 21 MiB; loading it all may be slow.' }, 'warns above 20 MiB');
});

// @cases:steps

test('s01_splitAtOffsetsRecordsTheLine', async function (t) {
	const srv = logServer('aaaaa\nbbbbb\nccc');
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([0, 6, 12].map(function (o, i) { return { ev: 'step', id: 's' + i, title: 'S', rawOffset: o }; }), '3') : srv.route(url);
	});
	await settle();
	expectSame(c.term.texts(), ['aaaaa\n', 'bbbbb\n', 'ccc'], 'split at 6 and 12, not at the chunk start');
	expectSame([0, 6, 12].map(c.engine.lineFor), [0, 1, 2], 'recorded lines');
});

test('s02_scrollToOffsetPicksTheNearestStepAtOrBelow', async function (t) {
	const srv = logServer('l0\nl1\nl2\nl3\n');
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 3 }, { ev: 'test', step: 'a', fw: 'jest', suite: 's', name: 'n', status: 'fail', rawOffset: 9 }], '2') : srv.route(url);
	});
	await settle();
	c.engine.scrollToOffset(7);
	c.engine.scrollToOffset(10);
	c.engine.scrollToOffset(1);
	expectSame(c.term.scrolledTo, [1, 3, 0], 'step at 3 -> line 1, test at 9 -> line 3, before any step -> checkpoint line 0');
	expect(c.status().follow === false, 'scrolling to a step turns follow off');
	c.engine.scrollToOffset(50);
	expectSame(c.term.scrolledTo, [1, 3, 0], 'an offset past the written bytes waits');
	srv.log += 'later\n'.repeat(8);
	c.clock.advance(1000);
	await settle();
	expect(c.term.scrolledTo.length === 4, 'applied once its bytes are written: ' + JSON.stringify(c.term.scrolledTo));
});

test('s03_lateStepUsesTheNearestCheckpoint', async function (t) {
	const srv = logServer('one\ntwo\n');
	let evs = [{ ev: 'step', id: 'a', title: 'A', rawOffset: 0 }];
	const c = start(t, { eventsUrl: '/t/events' }, function (url) { return url.startsWith('/t/events') ? pageRes(evs, String(evs.length)) : srv.route(url); });
	await settle();
	srv.log += 'three\n';
	srv.done = true;
	c.clock.advance(1000);
	await settle();
	evs = evs.concat([{ ev: 'step', id: 'b', title: 'B', rawOffset: 8 }]);
	c.clock.advance(1000);
	await settle();
	expect(c.urls[c.urls.length - 1] === '/t/events?after=1', 'only the events were polled');
	expect(c.engine.lineFor(8) === 2, 'the checkpoint written at offset 8 is on line 2: ' + c.engine.lineFor(8));
});

test('s04_scrollBeforeTheTrimLoadsAll', async function (t) {
	const srv = logServer('0123456789\nabc\ndefghij\n', { done: true });
	const c = start(t, { tailBytes: 10, eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 11 }], '1', { terminal: true }) : srv.route(url);
	});
	await settle();
	expect(c.status().stopped && c.status().trimmedFrom === 15, 'finished and trimmed');
	c.engine.scrollToOffset(11);
	await settle();
	expect(c.term.resets === 1 && c.status().trimmedFrom === null, 'Load all ran even after the run finished');
	expectSame(c.term.scrolledTo, [1], 'then scrolled to the step');
});

test('s05_followScrollsToTheBottom', async function (t) {
	const srv = logServer('a\n');
	const c = start(t, {}, srv.route);
	await settle();
	expect(c.term.bottoms === 1, 'follow is on by default');
	c.engine.setFollow(false);
	srv.log += 'b\n';
	c.clock.advance(1000);
	await settle();
	expect(c.term.bottoms === 1, 'off: no scroll');
	c.engine.setFollow(true);
	expect(c.term.bottoms === 2, 'turning it on scrolls at once');
});

test('s06_deepLinkBeforeTheFirstPollLoadsAll', async function (t) {
	const srv = logServer('0123456789\nabc\ndefghij\n');
	const c = start(t, { tailBytes: 10, eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 11 }], '1') : srv.route(url);
	});
	c.engine.scrollToOffset(11);
	await settle();
	expect(c.term.resets === 1 && c.status().trimmedFrom === null && c.status().banner === null, 'Load all ran: ' + JSON.stringify(c.status()));
	expectSame(c.term.scrolledTo, [1], 'and the scroll was applied once');
});

// @cases:real

test('x01_realXtermLinesAndCells', async function (t) {
	const T = realTerminal();
	const srv = logServer('\u001b[31mred\u001b[0m\r\n50%\r100%\r\nstep two\r\n', { done: true });
	const c = start(t, { eventsUrl: '/t/events' }, function (url) {
		return url.startsWith('/t/events') ? pageRes([{ ev: 'step', id: 'a', title: 'A', rawOffset: 0 }, { ev: 'step', id: 'b', title: 'B', rawOffset: 25 }], '2', { terminal: true }) : srv.route(url);
	}, { real: T });
	for (let i = 0; i < 20 && !c.status().stopped; i++) await settleReal();
	const b = c.term.buffer.active;
	expect(b.getLine(0).translateToString(true) === 'red', 'line 0');
	expect(b.getLine(0).getCell(0).getFgColor() === 1 && b.getLine(0).getCell(0).isFgPalette(), 'SGR 31 is palette red');
	expect(b.getLine(1).translateToString(true) === '100%', 'the \\r counter is one line');
	expect(c.engine.lineFor(25) === 2, 'the step at offset 25 is on line 2: ' + c.engine.lineFor(25));
	expect(c.term.options.scrollback === 100000, 'scrollback');
	c.term.reset();
	await settleReal();
	expect(c.term.buffer.active.getLine(0).translateToString(true) === '', 'reset() clears the buffer');
});

// @cases:input

/** createInput('t1') over a fake clock; records publishes, views and armed() calls. */
function input(t) {
	const clock = fakeClock();
	const e = { sent: [], views: [], armed: [], clock: clock };
	e.input = t.JT.createInput({ inputId: 't1' }, {
		publish: function (p) { e.sent.push(p); },
		setTimeout: clock.setTimeout, clearTimeout: clock.clearTimeout, now: clock.now,
		onChange: function (v) { e.views.push(v); },
		armed: function (on) { e.armed.push(on); }
	});
	e.view = function () { return e.input.view(); };
	e.data = function () { return e.sent.filter(function (p) { return p.data !== ''; }).map(function (p) { return p.data; }); };
	e.arm = function () { e.input.press(); e.input.onStatus({ id: 't1', state: 'live' }); };
	return e;
}

const ESC_KEY = { type: 'keydown', key: 'Escape' };

test('i01_pressProbes', function (t) {
	const e = input(t);
	expectSame(e.view().toggle, { label: 'Enable input', pressed: false, disabled: false }, 'idle toggle');
	e.input.press();
	expectSame(e.sent, [{ id: 't1', data: '' }], 'a probe with the inputId, not the card id');
	expect(e.view().state === 'probing', 'probing: ' + e.view().state);
	expect(e.armed.length === 0, 'not armed by the press alone');
});

test('i02_liveArms', function (t) {
	const e = input(t);
	e.arm();
	expectSame(e.armed, [true], 'armed(true)');
	const v = e.view();
	expect(v.state === 'armed' && v.chip === 'Input live' && v.chipState === 'live', 'chip: ' + JSON.stringify(v));
	expect(v.toggle.pressed === true && v.toggle.disabled === false, 'pressed: ' + JSON.stringify(v.toggle));
	expect(e.views.length > 0 && e.views[e.views.length - 1].state === 'armed', 'onChange saw the arm');
});

test('i03_deniedDisablesTheToggle', function (t) {
	const e = input(t);
	e.input.press();
	e.input.onStatus({ id: 't1', state: 'denied' });
	expectSame(e.view().toggle, { label: 'Input not permitted', pressed: false, disabled: true }, 'denied while probing');
	expect(e.view().chip === 'Input not permitted' && e.view().chipState === 'warn', 'the chip announces it: ' + JSON.stringify(e.view()));
	e.input.onStatus({ id: 't1', state: 'live' });
	e.input.press();
	expect(e.view().state === 'denied' && e.armed.length === 0 && e.sent.length === 1, 'and it stays that way');
	const a = input(t);
	a.arm();
	a.input.onStatus({ id: 't1', state: 'denied' });
	expectSame(a.armed, [true, false], 'denied while armed disarms');
	expectSame(a.view().toggle, { label: 'Input not permitted', pressed: false, disabled: true }, 'denied while armed');
	expect(a.view().chip === 'Input not permitted' && a.view().chipState === 'warn', 'chip while armed: ' + JSON.stringify(a.view()));
});

test('i04_probeTimeout', function (t) {
	const e = input(t);
	e.input.press();
	e.clock.advance(1999);
	expect(e.view().state === 'probing', 'still probing at 1999 ms');
	e.clock.advance(1);
	expect(e.view().state === 'idle' && e.view().chip === 'Input unavailable', 'unavailable: ' + JSON.stringify(e.view()));
	e.input.onStatus({ id: 't1', state: 'live' });
	expect(e.view().state === 'idle' && e.armed.length === 0, 'a late live for the expired probe is ignored');
	e.input.press();
	expect(e.sent.length === 2 && e.view().state === 'probing' && e.view().chip === null, 'pressing again sends a new probe');
});

test('i05_batching', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('a');
	e.clock.advance(50);
	e.input.onData('b');
	e.clock.advance(49);
	expectSame(e.data(), [], 'nothing before 100 ms');
	e.clock.advance(1);
	expectSame(e.sent.slice(1), [{ id: 't1', data: 'ab' }], 'one frame');
	e.clock.advance(1000);
	expect(e.sent.length === 2, 'and nothing more');
});

test('i06_largePasteSplitsAt4096', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('x'.repeat(5000));
	expectSame(e.data().map(function (d) { return d.length; }), [4096], 'one 4096-byte frame now');
	e.clock.advance(99);
	expect(e.data().length === 1, 'at most one frame per tick');
	e.clock.advance(1);
	expectSame(e.data().map(function (d) { return d.length; }), [4096, 904], 'the rest on the next tick');
});

test('i07_splitIsCodePointSafe', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('a'.repeat(4094) + '\u{1F600}b');
	e.clock.advance(100);
	const d = e.data();
	expect(d.length === 2, 'two frames: ' + d.length);
	expect(d[0] === 'a'.repeat(4094), 'the emoji is not split: first frame ends ' + JSON.stringify(d[0].slice(-3)));
	expect(d[1] === '\u{1F600}b', 'second frame: ' + JSON.stringify(d[1]));
	for (const f of d)
		expect(enc.encode(f).length <= 4096 && f.indexOf('\uFFFD') < 0, 'every frame is whole UTF-8 within 4096 bytes');
});

test('i08_escEscDisarms', function (t) {
	const e = input(t);
	e.arm();
	expect(e.input.onKeyDown(ESC_KEY) === true, 'the first Esc goes through');
	e.input.onData('\u001b');
	e.clock.advance(200);
	expect(e.input.onKeyDown(ESC_KEY) === false, 'the second Esc is swallowed');
	expectSame(e.armed, [true, false], 'disarmed');
	e.clock.advance(1000);
	expectSame(e.data(), ['\u001b'], 'only the first Esc was sent');
	const s = input(t);
	s.arm();
	s.input.onKeyDown(ESC_KEY);
	s.clock.advance(501);
	expect(s.input.onKeyDown(ESC_KEY) === true && s.view().state === 'armed', 'Esc, then Esc after 500 ms, stays armed');
	expect(s.input.onKeyDown({ type: 'keyup', key: 'Escape' }) === true && s.view().state === 'armed', 'keyup does not count');
});

test('i09_busyShowsTheThrottledChip', function (t) {
	const e = input(t);
	e.arm();
	e.clock.advance(5000);
	expect(e.view().state === 'armed' && e.view().chip === 'Input live', 'silence after data frames changes nothing');
	e.input.onStatus({ id: 't1', state: 'busy' });
	expect(e.view().state === 'armed' && e.view().chip === 'Input throttled: some keys not sent' && e.view().chipState === 'warn',
		'throttled: ' + JSON.stringify(e.view()));
	expectSame(e.armed, [true], 'still armed');
	e.input.onData('z');
	e.clock.advance(100);
	expect(e.view().chip === 'Input live', 'until the next flush: ' + e.view().chip);
});

test('i10_disconnectDropsKeys', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('q');
	e.input.connected(false);
	expectSame(e.armed, [true, false], 'disarmed');
	expect(e.view().chip === 'Disconnected: input not sent' && e.view().state === 'idle', 'chip: ' + JSON.stringify(e.view()));
	e.clock.advance(1000);
	e.input.onData('r');
	e.clock.advance(1000);
	expectSame(e.data(), [], 'queued and later keys are dropped, not replayed');
	e.input.connected(true);
	expect(e.view().state === 'idle' && e.armed.length === 2, 'reconnecting does not re-arm');
});

test('i11_runEndedDisablesTheToggle', function (t) {
	const e = input(t);
	e.arm();
	e.input.runEnded();
	expectSame(e.armed, [true, false], 'disarmed');
	expectSame(e.view().toggle, { label: 'Job ended', pressed: false, disabled: true }, 'toggle');
	expect(e.view().chip === 'Job ended' && e.view().chipState === 'warn', 'the chip announces it: ' + JSON.stringify(e.view()));
	e.input.press();
	expect(e.sent.length === 1, 'no probe after the job ended');
});

test('i12_otherIdIgnored', function (t) {
	const e = input(t);
	e.input.press();
	e.input.onStatus({ id: 't2', state: 'live' });
	e.input.onStatus({ state: 'bad' });
	e.input.onStatus({ id: 't2', state: 'denied' });
	e.input.onStatus(null);
	expect(e.view().state === 'probing' && e.armed.length === 0, 'unchanged: ' + JSON.stringify(e.view()));
});

test('i13_closedWhileArmed', function (t) {
	const e = input(t);
	e.arm();
	e.input.onStatus({ id: 't1', state: 'closed' });
	expectSame(e.armed, [true, false], 'disarmed');
	expectSame(e.view().toggle, { label: 'Job ended', pressed: false, disabled: true }, 'toggle');
	expect(e.view().chip === 'Job ended' && e.view().chipState === 'warn', 'the chip announces it: ' + JSON.stringify(e.view()));
});

test('i14_disarmFlushesAtMostOneFrame', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('abc');
	e.input.press();
	expectSame(e.armed, [true, false], 'the toggle disarms');
	e.clock.advance(1000);
	expectSame(e.data(), ['abc'], 'the queued keys go in one frame');
	const b = input(t);
	b.arm();
	b.input.onData('x'.repeat(3000));
	b.clock.advance(100);
	b.input.onData('y'.repeat(5000));
	b.input.press();
	b.clock.advance(1000);
	expectSame(b.data().map(function (d) { return d.length; }), [3000, 4096], 'one frame on disarm, the rest dropped');
});

test('i15_reconnectClearsTheDisconnectedChip', function (t) {
	const e = input(t);
	e.arm();
	e.input.connected(false);
	expect(e.view().chip === 'Disconnected: input not sent', 'disconnected chip');
	const n = e.views.length;
	e.input.connected(true);
	expect(e.view().chip === null && e.view().chipState === null, 'cleared on reconnect: ' + JSON.stringify(e.view()));
	expect(e.views.length === n + 1, 'and announced through onChange');
	const u = input(t);
	u.input.press();
	u.clock.advance(2000);
	u.input.connected(true);
	expect(u.view().chip === 'Input unavailable', 'a reconnect leaves other chips alone: ' + u.view().chip);
});

test('i16_pressWhileDisconnected', function (t) {
	const e = input(t);
	e.input.connected(false);
	expect(e.view().chip === null, 'an idle disconnect shows nothing');
	e.input.press();
	expect(e.sent.length === 0, 'no probe while disconnected');
	expect(e.view().state === 'idle' && e.view().chip === 'Disconnected: input not sent', 'chip: ' + JSON.stringify(e.view()));
});

test('i17_pressWhileProbing', function (t) {
	const e = input(t);
	e.input.press();
	e.input.press();
	expect(e.sent.length === 1 && e.view().state === 'probing', 'no second probe: ' + e.sent.length);
});

test('i18_disposeDuringProbing', function (t) {
	const e = input(t);
	e.input.press();
	e.input.dispose();
	const n = e.views.length;
	e.clock.advance(2000);
	expect(e.views.length === n, 'no onChange after dispose');
	expect(e.view().chip === null, 'no Input unavailable chip: ' + e.view().chip);
	expectSame(e.clock.pending(), [], 'the probe timer was cleared');
});

test('i19_queueAtTheCapIsAccepted', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('x'.repeat(262144));
	e.clock.advance(10000);
	expect(e.data().join('').length === 262144, 'every byte was sent: ' + e.data().join('').length);
	expect(e.view().chip === 'Input live', 'no refusal chip: ' + e.view().chip);
});

test('i20_oneByteOverTheCapIsRefused', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('x'.repeat(262145));
	e.clock.advance(10000);
	expectSame(e.data(), [], 'nothing from the refused chunk is sent');
	expect(e.view().chip === 'Paste too large: not sent' && e.view().chipState === 'warn', 'chip: ' + JSON.stringify(e.view()));
	e.input.onData('k');
	e.clock.advance(100);
	expectSame(e.data(), ['k'], 'typing works afterwards');
	expect(e.view().chip === 'Input live', 'the next sent frame clears the chip: ' + e.view().chip);
});

test('i21_capCountsUtf8BytesNotChars', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('\u20ac'.repeat(87382));    // 87382 chars (< 262144) but 262146 UTF-8 bytes
	e.clock.advance(10000);
	expectSame(e.data(), [], 'over the cap in bytes: refused');
	expect(e.view().chip === 'Paste too large: not sent', 'chip: ' + e.view().chip);
	const ok = input(t);
	ok.arm();
	ok.input.onData('\u20ac'.repeat(87381) + 'x');    // exactly 262144 bytes
	ok.clock.advance(10000);
	expect(ok.data().join('') === '\u20ac'.repeat(87381) + 'x', 'exactly at the cap in bytes: accepted whole');
});

test('i22_queuedKeysStillDrainAfterARefusal', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('a'.repeat(200000));
	e.input.onData('b'.repeat(100000));    // queued 195904 + 100000 > 262144
	expect(e.view().chip === 'Paste too large: not sent', 'chip: ' + e.view().chip);
	e.clock.advance(10000);
	expect(e.data().join('') === 'a'.repeat(200000), 'the queued keys all drain and none of the refused chunk');
});

test('i23_refusalChipSurvivesTheDrain', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('a'.repeat(200000));
	e.input.onData('b'.repeat(100000));    // refused while the queue is draining
	e.clock.advance(100);
	expect(e.view().chip === 'Paste too large: not sent' && e.view().chipState === 'warn', 'chip after a tick: ' + JSON.stringify(e.view()));
	e.clock.advance(1000);
	expect(e.view().chip === 'Paste too large: not sent', 'and while it keeps draining: ' + e.view().chip);
	e.input.onData('k');
	e.clock.advance(100);
	expect(e.view().chip === 'Input live' && e.view().chipState === 'live', 'an accepted chunk restores Input live: ' + JSON.stringify(e.view()));
});

test('i24_throttledChipSurvivesTheDrain', function (t) {
	const e = input(t);
	e.arm();
	e.input.onData('a'.repeat(20000));
	e.input.onStatus({ id: 't1', state: 'busy' });    // the sink dropped a frame while the queue is draining
	e.clock.advance(100);
	expect(e.view().chip === 'Input throttled: some keys not sent' && e.view().chipState === 'warn', 'chip after a tick: ' + JSON.stringify(e.view()));
	e.clock.advance(1000);
	expect(e.view().chip === 'Input throttled: some keys not sent', 'and while it keeps draining: ' + e.view().chip);
	e.input.onData('k');
	e.clock.advance(100);
	expect(e.view().chip === 'Input live' && e.view().chipState === 'live', 'an accepted chunk restores Input live: ' + JSON.stringify(e.view()));
});

test('l01_linkForPicksTheBridge', function (t) {
	const both = { downstream: ['term.input-status'], upstream: ['term.input'] };
	const c = { bridges: [
		Object.assign({ id: 'sse', transport: 'sse' }, both),
		{ id: 'half', transport: 'websocket', downstream: ['other'], upstream: ['term.input'] },
		Object.assign({ id: 'term', transport: 'websocket' }, both),
		Object.assign({ id: 'later', transport: 'websocket' }, both)
	] };
	expect(t.JT.linkFor(c) === 'term', 'the first websocket bridge carrying both topics: ' + t.JT.linkFor(c));
	expect(t.JT.linkFor({ bridges: [c.bridges[0], c.bridges[1]] }) === null, 'none carries both');
	expect(t.JT.linkFor(null) === null && t.JT.linkFor({}) === null && t.JT.linkFor({ bridges: 'x' }) === null, 'no bridges');
});

// @cases:nudge

/** A started engine over a one-chunk log, after its probe and first tick.  The nudge link is not up yet. */
async function nudgeable(t, opts, log) {
	const srv = logServer(log || 'abc');
	const hang = { p: null, skip: 0 };
	const c = start(t, opts, function (url) {
		if (hang.p && hang.skip-- <= 0) {
			const p = hang.p;
			hang.p = null;
			return p;
		}
		return c.fail ? statusRes(503) : srv.route(url);
	});
	c.srv = srv;
	c.hang = hang;
	c.fail = false;
	await settle();
	return c;
}

/** Makes the next request wait (after {@code skip} others); the returned function answers it with the given response. */
function holdNext(c, skip) {
	let answer;
	c.hang.skip = skip || 0;
	c.hang.p = new Promise(function (r) { answer = r; });
	return function (res) { answer(res); };
}

test('n01_nudgeAheadFetchesNow', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const n = c.urls.length;
	const due = c.clock.pending();
	c.engine.nudge({ end: c.status().next + 10 });
	await settle();
	expect(c.urls.length === n + 1 && c.urls[n] === '/t/bytes?from=3', 'one fetch from next without the clock: ' + c.urls.slice(n).join(' '));
	expectSame(due, [1000], 'the poll had not been due');
});

test('n02_nudgeBehindIgnored', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const n = c.urls.length;
	const due = c.clock.pending();
	c.engine.nudge({ end: c.status().next });
	await settle();
	expect(c.urls.length === n, 'no request: ' + c.urls.slice(n).join(' '));
	expectSame(c.clock.pending(), due, 'the pending timer is untouched');
});

test('n03_inFlightOneFollowUp', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const release = holdNext(c);
	c.clock.advance(1000);
	await settle();
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	c.engine.nudge({ end: 200 });
	await settle();
	expect(c.urls.length === n, 'nothing new while a tick is in flight');
	release(bytesRes('', { next: 3, end: 3 }));
	await settle();
	expect(c.urls.length === n + 1, 'exactly one follow-up tick: ' + c.urls.slice(n - 1).join(' '));
});

test('n03b_inFlightFailureOneRetry', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const release = holdNext(c);
	c.clock.advance(1000);
	await settle();
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	release(statusRes(503));
	await settle();
	expect(c.urls.length === n, 'no immediate extra request after the failure: ' + c.urls.slice(n).join(' '));
	expectSame(c.clock.pending(), [1000], 'only the retry timer is pending');
});

test('n03c_followUpOnlyWhenStillBehind', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const release = holdNext(c);
	c.clock.advance(1000);
	await settle();
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	release(bytesRes('x'.repeat(97), { next: 100, end: 100 }));
	await settle();
	expect(c.urls.length === n, 'the tick already reached the nudged end: ' + c.urls.slice(n).join(' '));
});

test('n14_replayInFlightNoDoubleTick', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const releaseOld = holdNext(c);
	c.clock.advance(1000);
	await settle();
	const releaseNew = holdNext(c, 1);
	c.engine.loadAll();
	await settle();
	const n = c.urls.length;
	releaseOld(bytesRes('', { next: 3, end: 3 }));
	await settle();
	c.engine.nudge({ end: 100 });
	await settle();
	expect(c.urls.length === n, 'the replay tick is still fetching, so the nudge waits: ' + c.urls.slice(n).join(' '));
	releaseNew(bytesRes('abc', { next: 100, end: 100 }));
	await settle();
	expect(c.urls.length === n, 'no extra request once the replay tick finished: ' + c.urls.slice(n).join(' '));
});

test('n14b_replayDropsLeftoverNudgeEnd', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const releaseOld = holdNext(c);
	c.clock.advance(1000);
	await settle();
	c.engine.nudge({ end: 100 });
	const releaseNew = holdNext(c, 1);
	c.engine.loadAll();
	await settle();
	releaseOld(bytesRes('', { next: 3, end: 3 }));
	releaseNew(bytesRes('abc', { next: 3, end: 3 }));
	await settle();
	const release = holdNext(c);
	c.clock.advance(c.clock.pending()[0]);
	await settle();
	const n = c.urls.length;
	c.engine.nudge({ end: 5 });
	await settle();
	release(bytesRes('de', { next: 5, end: 5 }));
	await settle();
	expect(c.urls.length === n, 'the old nudged end is gone, so no follow-up tick: ' + c.urls.slice(n).join(' '));
});

test('n15_nudgeDuringRetryKeepsBackoff', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	c.fail = true;
	for (let i = 0; i < 2; i++) {
		c.clock.advance(c.clock.pending()[0]);
		await settle();
	}
	expectSame(c.clock.pending(), [2000], 'the second retry delay');
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	expect(c.urls.length === n, 'no request while backing off: ' + c.urls.slice(n).join(' '));
	expectSame(c.clock.pending(), [2000], 'the retry timer is untouched');
});

test('n16_disconnectDuringRetryKeepsBackoff', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	c.engine.nudge({ end: 13 });
	await settle();
	expect(c.status().nudged === true, 'nudge mode on');
	c.fail = true;
	for (let i = 0; i < 2; i++) {
		c.clock.advance(c.clock.pending()[0]);
		await settle();
	}
	expectSame(c.clock.pending(), [2000], 'the second retry delay');
	c.engine.nudgeConnected(false);
	await settle();
	expectSame(c.clock.pending(), [2000], 'the retry keeps its delay');
	expect(c.status().nudged === false, 'nudge mode off');
});

test('n04_nudgeModeSlowsPoll', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	c.engine.nudge({ end: 13 });
	await settle();
	expectSame(c.clock.pending(), [5000], 'the safety-net poll');
	expect(c.status().nudged === true, 'nudged: ' + JSON.stringify(c.status()));
});

test('n05_disconnectRestores', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	c.engine.nudge({ end: 13 });
	await settle();
	c.engine.nudgeConnected(false);
	await settle();
	expectSame(c.clock.pending(), [1000], 'back to refreshMs');
	expect(c.status().nudged === false, 'nudged cleared');
});

test('n05b_disconnectWhileIdleKeepsIdle', async function (t) {
	const c = await nudgeable(t);
	for (let i = 0; i < 3; i++) {
		c.clock.advance(c.clock.pending()[0]);
		await settle();
	}
	expectSame(c.clock.pending(), [5000], 'idle after three empty polls');
	c.engine.nudgeConnected(true);
	c.engine.nudge({ end: c.status().next });
	await settle();
	expect(c.status().nudged === true, 'nudge mode on');
	c.engine.nudgeConnected(false);
	await settle();
	expectSame(c.clock.pending(), [5000], 'still idle, not refreshMs');
	expect(c.status().nudged === false, 'nudged cleared');
});

test('n06_connectAloneNoSlowdown', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	c.clock.advance(1000);
	await settle();
	expectSame(c.clock.pending(), [1000], 'a link without a nudge keeps the normal pace');
	expect(c.status().nudged === false, 'not nudged');
});

test('n07_doneNudge', async function (t) {
	const c = await nudgeable(t);
	const n = c.urls.length;
	c.engine.nudge({ end: c.status().next, done: true });
	await settle();
	expect(c.urls.length === n + 1, 'a done nudge fetches although nothing is ahead: ' + c.urls.slice(n).join(' '));
});

test('n08_badPayloadIgnored', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	const n = c.urls.length;
	[null, {}, { end: -1 }, { end: '5' }, { end: NaN }, { end: 10.5 }, { end: Infinity }, { end: Number.MAX_SAFE_INTEGER + 2 }].forEach(function (p) {
		c.engine.nudge(p);
	});
	await settle();
	expect(c.urls.length === n, 'no request: ' + c.urls.slice(n).join(' '));
	expect(c.status().nudged === false, 'a bad payload does not switch to nudge mode');
});

test('n09_stoppedIgnores', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	c.engine.stop();
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	expect(c.urls.length === n, 'no request after stop');
	expect(c.status().nudged === false, 'a stopped engine does not switch to nudge mode');
});

test('n10_unprobedIgnored', async function (t) {
	const c = start(t, {}, function () { return statusRes(503); });
	await settle();
	expect(c.status().reconnecting && c.clock.pending().length === 1, 'the probe failed and a retry is waiting');
	c.engine.nudgeConnected(true);
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	expect(c.urls.length === n, 'a nudge before the probe succeeds does nothing: ' + c.urls.slice(n).join(' '));
	expect(c.status().nudged === false, 'and does not switch to nudge mode');
});

test('n11_refreshAboveFiveKept', async function (t) {
	const c = await nudgeable(t, { refreshMs: 8000 });
	c.engine.nudgeConnected(true);
	c.engine.nudge({ end: 13 });
	await settle();
	expectSame(c.clock.pending(), [8000], 'the larger of refreshMs and the nudge interval');
});

test('n12_hiddenTabDefersNudge', async function (t) {
	const c = await nudgeable(t);
	c.engine.nudgeConnected(true);
	await settle();
	c.hidden = true;
	const n = c.urls.length;
	c.engine.nudge({ end: 100 });
	await settle();
	c.engine.nudge({ end: 200 });
	await settle();
	expect(c.urls.length === n, 'no request while hidden');
	expect(c.visibleFns.length === 1, 'one visibility listener live: ' + c.visibleFns.length);
	c.show();
	await settle();
	expect(c.urls.length === n + 1, 'exactly one request on show: ' + c.urls.slice(n).join(' '));
});

test('n13_nudgeLinkFor', function (t) {
	const c = { bridges: [{ id: 'a', downstream: ['x.y'] }, { id: 'b', downstream: ['term.nudge:t1'] }] };
	expect(t.JT.nudgeLinkFor(c, 'term.nudge:t1') === 'b', 'the bridge that carries the topic');
	expect(t.JT.nudgeLinkFor(c, 'term.nudge:t2') === null, 'no match');
	expect(t.JT.nudgeLinkFor(null, 'term.nudge:t1') === null && t.JT.nudgeLinkFor({}, 'term.nudge:t1') === null, 'no contract or bridges');
	expect(t.JT.nudgeLinkFor({ bridges: [{ id: 'a', downstream: 'term.nudge:t1' }, { id: 'b' }, null] }, 'term.nudge:t1') === null, 'downstream not an array');
	expect(t.JT.nudgeLinkFor({ bridges: [{ id: 'w', downstream: ['term.nudge:*'] }] }, 'term.nudge:t1') === null, 'a wildcard downstream does not match');
});

const LINK_HANDLER = { activate: function () {} };

/** The fixed option set mount() builds today, with the given extra terminal options merged through the allowlist. */
function xtermCtorOptions(t, extra) {
	const base = { cols: 80, rows: 24, scrollback: 100000, disableStdin: true, allowProposedApi: false, fontSize: 14, cursorBlink: false, linkHandler: LINK_HANDLER };
	return t.JT.xtermOptions(base, extra);
}

test('o01_convertEolReachesTheTerminal', function (t) {
	expect(xtermCtorOptions(t, { convertEol: true }).convertEol === true, 'convertEol true passed');
	expect(xtermCtorOptions(t, { convertEol: false }).convertEol === false, 'convertEol false passed');
});

test('o02_unknownKeyIsIgnored', function (t) {
	const o = xtermCtorOptions(t, { convertEol: true, theme: { background: 'red' }, bogus: 1 });
	expect(!('theme' in o) && !('bogus' in o), 'unknown keys dropped: ' + Object.keys(o));
});

test('o03_juneauOwnedKeysCannotBeOverridden', function (t) {
	const base = xtermCtorOptions(t, undefined);
	const o = xtermCtorOptions(t, { cols: 1, rows: 1, disableStdin: false, linkHandler: null, allowProposedApi: true, fontSize: 40, cursorBlink: true });
	expectSame(Object.keys(o), Object.keys(base), 'same key set');
	for (const k of Object.keys(base)) expect(o[k] === base[k], k + ' unchanged');
});

test('o04_wrongTypesAreIgnored', function (t) {
	const base = xtermCtorOptions(t, undefined);
	const o = xtermCtorOptions(t, { convertEol: 'true', scrollback: '500', fontFamily: 12 });
	expectSame(o, base, 'nothing merged');
	for (const bad of [-1, 1.5, NaN, Infinity, null, 1000001, 1e21, Number.MAX_SAFE_INTEGER + 1])
		expect(xtermCtorOptions(t, { scrollback: bad }).scrollback === base.scrollback, 'scrollback ' + bad + ' ignored');
	expect(xtermCtorOptions(t, { scrollback: 1000000 }).scrollback === 1000000, 'scrollback at the cap accepted');
	expect(!('fontFamily' in xtermCtorOptions(t, { fontFamily: '' })), 'empty fontFamily ignored');
	expectSame(xtermCtorOptions(t, 'x'), base, 'non-object terminal');
	expectSame(xtermCtorOptions(t, [true]), base, 'array terminal');
});

test('o05_validScrollbackAndFontFamilyMerge', function (t) {
	const o = xtermCtorOptions(t, { scrollback: 0, fontFamily: 'monospace' });
	expect(o.scrollback === 0 && o.fontFamily === 'monospace', JSON.stringify(o));
	expect(xtermCtorOptions(t, { scrollback: 2000 }).scrollback === 2000, 'scrollback 2000');
});

test('o06_absentTerminalCopiesBase', function (t) {
	const o = xtermCtorOptions(t, undefined);
	expectSame(Object.keys(o), ['cols', 'rows', 'scrollback', 'disableStdin', 'allowProposedApi', 'fontSize', 'cursorBlink', 'linkHandler'], 'keys');
	expectSame(xtermCtorOptions(t, {}), o, 'empty object');
});

// @cases:theme

/** A DOM element stub: just enough surface for mount() to build and tear down its region. */
function fakeEl(tag) {
	const e = {
		tagName: tag, children: [], attrs: {}, className: '', textContent: '', hidden: false, parentNode: null, clientWidth: 0,
		appendChild: function (c) { c.parentNode = e; e.children.push(c); return c; },
		append: function () { Array.from(arguments).forEach(function (c) { e.appendChild(c); }); },
		removeChild: function (c) { const i = e.children.indexOf(c); if (i >= 0) e.children.splice(i, 1); c.parentNode = null; },
		setAttribute: function (k, v) { e.attrs[k] = v; },
		getAttribute: function (k) { return Object.prototype.hasOwnProperty.call(e.attrs, k) ? e.attrs[k] : null; },
		removeAttribute: function (k) { delete e.attrs[k]; },
		addEventListener: function () {}, removeEventListener: function () {}, focus: function () {},
		querySelector: function () { return null; },
		getBoundingClientRect: function () { return { width: 0 }; }
	};
	return e;
}

/**
 * Mounts a real terminal region over a fake document whose computed style serves the given tokens.  Returns the
 * constructor options the terminal was created with, the live terminal, the mutable token map, a dispatcher for
 * document events and the mount cleanup.
 */
async function mountThemed(t, tokens, terminalOpts) {
	const clock = fakeClock();
	const listeners = {};
	const srv = logServer('hi\n');
	const created = { ctorOptions: null, term: null };
	const doc = {
		hidden: false, documentElement: fakeEl('html'),
		createElement: fakeEl,
		createTextNode: function (x) { return { text: x }; },
		addEventListener: function (n, f) { (listeners[n] = listeners[n] || []).push(f); },
		removeEventListener: function (n, f) { const l = listeners[n] || []; const i = l.indexOf(f); if (i >= 0) l.splice(i, 1); }
	};
	const win = {
		TextEncoder: TextEncoder, URL: URL, Promise: Promise, Date: Date, console: console,
		location: { href: 'http://x/p', hash: '' },
		setTimeout: clock.setTimeout, clearTimeout: clock.clearTimeout,
		fetch: function (url) { return Promise.resolve().then(function () { return srv.route(url); }); },
		addEventListener: function () {}, removeEventListener: function () {},
		getComputedStyle: function () { return { getPropertyValue: function (n) { return Object.prototype.hasOwnProperty.call(tokens, n) ? tokens[n] : ''; } }; },
		Terminal: function (o) {
			const term = stubTerm(o.cols, o.rows, false);
			term.options = Object.assign({}, o);
			term.open = function () {}; term.dispose = function () { term.disposed = true; };
			term.onData = function () {}; term.attachCustomKeyEventHandler = function () {};
			created.ctorOptions = o; created.term = term;
			return term;
		}
	};
	doc.defaultView = win;
	const host = fakeEl('div');
	host.ownerDocument = doc;
	const cleanup = t.JT.mount(host, { bytesUrl: '/t/bytes', terminal: terminalOpts }, null, null);
	await settle();
	return {
		created: created, tokens: tokens, cleanup: cleanup,
		fire: function (name) { (listeners[name] || []).slice().forEach(function (f) { f({ type: name }); }); },
		count: function (name) { return (listeners[name] || []).length; }
	};
}

test('t01_termThemeFromTokens', async function (t) {
	const m = await mountThemed(t, { '--jc-term-bg': ' #0d0f12', '--jc-term-fg': '#e5e5e5', '--jc-term-cursor': '#e5e5e5', '--jc-term-selection': 'rgba(255,255,255,0.3)' });
	expectSame(m.created.ctorOptions.theme, { background: '#0d0f12', foreground: '#e5e5e5', cursor: '#e5e5e5', cursorAccent: '#0d0f12', selectionBackground: 'rgba(255,255,255,0.3)' }, 'theme from tokens');
	m.cleanup();
});

test('t02_fallbacks', async function (t) {
	const m = await mountThemed(t, {});
	expectSame(m.created.ctorOptions.theme, { background: '#000000', foreground: '#ffffff', cursor: '#ffffff', cursorAccent: '#000000', selectionBackground: 'rgba(255,255,255,0.3)' }, 'xterm default fallbacks');
	m.cleanup();
});

test('t03_cardCannotOverrideTheme', async function (t) {
	const m = await mountThemed(t, { '--jc-term-bg': '#0d0f12' }, { theme: { background: 'red' }, convertEol: true });
	expect(m.created.ctorOptions.theme.background === '#0d0f12', 'card theme ignored: ' + m.created.ctorOptions.theme.background);
	expect(m.created.ctorOptions.convertEol === true, 'allowlisted keys still merge');
	m.cleanup();
});

test('t04_themeChangedUpdates', async function (t) {
	const m = await mountThemed(t, { '--jc-term-bg': '#000000' });
	m.tokens['--jc-term-bg'] = '#0d0f12';
	m.fire('juneau:theme-changed');
	expect(m.created.term.options.theme.background === '#0d0f12', 'background updated: ' + m.created.term.options.theme.background);
	expect(m.created.term.options.theme.cursorAccent === '#0d0f12', 'cursorAccent follows the background');
	m.cleanup();
});

test('t05_listenerRemovedOnCleanup', async function (t) {
	const m = await mountThemed(t, { '--jc-term-bg': '#000000' });
	expect(m.count('juneau:theme-changed') === 1, 'one listener while mounted');
	m.cleanup();
	expect(m.count('juneau:theme-changed') === 0, 'listener removed');
	m.tokens['--jc-term-bg'] = '#123456';
	m.fire('juneau:theme-changed');
	expect(m.created.term.options.theme.background === '#000000', 'a disposed terminal is not touched');
});

// @cases:end

(async function main() {
	const out = {};
	for (const c of cases) {
		try {
			await c.fn(load());
			out[c.name] = true;
		} catch (e) {
			out[c.name] = String((e && e.stack) || e);
		}
	}
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String((e && e.stack) || e));
	process.exit(1);
});
