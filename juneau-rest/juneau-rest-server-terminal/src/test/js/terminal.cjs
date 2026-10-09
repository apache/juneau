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
		navigator: { userAgent: 'node', platform: 'MacIntel' } };
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
