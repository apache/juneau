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
'use strict';

/*
 * The terminal region in a real Chromium, through Playwright.
 *
 *   node terminal-browser.cjs <xterm.js> <xterm.css> <juneau-terminal.js> <juneau-terminal.css> <juneau-views.css> <juneau-theme-dark.css>
 *
 * Every request goes to the made-up origin http://term.test and is answered by page.route, so nothing listens on a
 * port.  The bytes and events endpoints are scripted here; their real headers are covered by TerminalMixin_Test and
 * by the end-to-end smoke test.  Prints one JSON report to stdout:
 *   {cases: {name: true | "failure"}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}}
 */

const fs = require('node:fs');
const { chromium } = require('playwright');

const ORIGIN = 'http://term.test';
const [XTERM_JS, XTERM_CSS, TERM_JS, TERM_CSS, VIEWS_CSS, DARK_CSS] = process.argv.slice(2).map(function (f) { return fs.readFileSync(f); });

const PAGE = '<!DOCTYPE html><html><head><meta charset="utf-8">'
	+ '<link rel="stylesheet" href="/a/xterm.css"><link rel="stylesheet" href="/a/juneau-views.css"><link rel="stylesheet" href="/a/juneau-theme-dark.css"><link rel="stylesheet" href="/a/juneau-terminal.css">'
	+ '<script src="/a/xterm.js"></script><script src="/a/juneau-terminal.js"></script>'
	+ '</head><body><div id="host" style="width:1000px"></div></body></html>';

const ASSETS = {
	'/page': ['text/html', Buffer.from(PAGE)],
	'/a/xterm.js': ['text/javascript', XTERM_JS],
	'/a/xterm.css': ['text/css', XTERM_CSS],
	'/a/juneau-terminal.js': ['text/javascript', TERM_JS],
	'/a/juneau-terminal.css': ['text/css', TERM_CSS],
	'/a/juneau-views.css': ['text/css', VIEWS_CSS],
	'/a/juneau-theme-dark.css': ['text/css', DARK_CSS]
};

/** An OSC 8 hyperlink escape sequence around the text. */
function osc8Link(href, text) { return '\u001b]8;;' + href + '\u0007' + text + '\u001b]8;;\u0007'; }

/** Runs in the page: the contrast of the four tone-coloured elements, creating a probe element for each as needed. */
function toneProbe() {
	const lum = function (c) { // NOSONAR javascript:S7721 - runs inside the browser page; it must stay in the serialized function
		const m = c.match(/[\d.]+/g).map(Number).slice(0, 3).map(function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
		return 0.2126 * m[0] + 0.7152 * m[1] + 0.0722 * m[2];
	};
	return ['juneau-term-badge', 'juneau-term-ok', 'juneau-term-chip', 'juneau-term-banner'].map(function (cls) {
		let el = document.querySelector('.' + cls + '.probe');
		if (!el) {
			el = document.createElement('span');
			el.className = cls + ' probe';
			el.textContent = 'x';
			document.body.appendChild(el);
		}
		const cs = getComputedStyle(el);
		const a = lum(cs.backgroundColor), b = lum(cs.color);
		return { cls: cls, bg: cs.backgroundColor, color: cs.color, ratio: (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05) };
	});
}

/** {log, done, cols, rows, events} -> a page.route handler for the page, its assets and the two endpoints. */
function server(fx) {
	const f = { done: true, cols: 80, rows: 24, events: null, ...fx };
	return function (route) {
		const u = new URL(route.request().url());
		const asset = ASSETS[u.pathname];
		if (asset)
			return route.fulfill({ status: 200, contentType: asset[0], body: asset[1] });
		if (u.pathname === '/t/bytes') {
			const from = Number(u.searchParams.get('from') || '0');
			const max = u.searchParams.has('max') ? Number(u.searchParams.get('max')) : 1 << 18;
			if (from > f.log.length)
				return route.fulfill({ status: 416, headers: { 'Term-Next': '0', 'Term-End': String(f.log.length), 'Term-Cols': String(f.cols), 'Term-Rows': String(f.rows) } });
			const body = f.log.subarray(from, from + max);
			const next = from + body.length;
			return route.fulfill({ status: 200, contentType: 'application/octet-stream', body: body, headers: {
				'Term-Next': String(next), 'Term-End': String(f.log.length), 'Term-Cols': String(f.cols), 'Term-Rows': String(f.rows),
				'Term-Done': String(f.done && next === f.log.length), 'Cache-Control': 'no-store'
			} });
		}
		if (u.pathname === '/t/events' && f.events)
			return route.fulfill({ status: 200, contentType: 'application/json',
				body: JSON.stringify({ contractVersion: '1', events: f.events, next: String(f.events.length), more: false, terminal: true }) });
		return route.fulfill({ status: 404, body: 'not found' });
	};
}

function lines(n) {
	let s = '';
	for (let i = 0; i < n; i++)
		s += 'line ' + String(i).padStart(3, '0') + '\r\n';
	return Buffer.from(s);
}

function expect(cond, msg) {
	if (!cond)
		throw new Error(msg);
}

/** Opens the page, mounts the region over the fixture and waits for the badge to say Done. */
async function open(page, fx, opts) {
	await page.route(ORIGIN + '/**', server(fx));
	await page.goto(ORIGIN + '/page');
	await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
	await page.evaluate(function (o) {
		if (o.hash)
			window.location.hash = o.hash;
		window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), o.opts);
	}, { opts: { id: 't', bytesUrl: '/t/bytes', ...(fx.events ? { eventsUrl: '/t/events' } : {}), ...opts }, hash: fx.hash || null });
	await page.waitForFunction(function () {
		const b = document.querySelector('.juneau-term-badge');
		return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
	});
}

function rows(page) {
	return page.evaluate(function () {
		return Array.from(document.querySelectorAll('.xterm-rows > div')).map(function (d) {
			return d.textContent.replaceAll('\u00a0', ' ').trimEnd();
		});
	});
}

async function waitRow0(page, text) {
	await page.waitForFunction(function (t) {
		const r = document.querySelector('.xterm-rows > div');
		return r && r.textContent.replaceAll('\u00a0', ' ').trim() === t;
	}, text, { timeout: 5000 });
}

/** Wraps window.Terminal so the case can read each xterm's options; returns once the page and its scripts are up. */
async function openRunning(page, fx) {
	await page.route(ORIGIN + '/**', server({ done: false, ...fx }));
	await page.goto(ORIGIN + '/page');
	await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
	await page.evaluate(function () {
		const T = window.Terminal;
		window.__terms = [];
		window.Terminal = function (o) { const x = new T(o); window.__terms.push(x); return x; };
		const h2 = document.createElement('div');
		h2.id = 'host2';
		h2.style.width = '1000px';
		document.body.appendChild(h2);
	});
}

/** Waits until the region in host {@code id} has its xterm and is polling. */
function waitRunning(page, id) {
	return page.waitForFunction(function (i) {
		const b = document.querySelector('#' + i + ' .juneau-term-badge');
		return b?.textContent === 'Running' && document.querySelector('#' + i + ' .xterm');
	}, id, { timeout: 5000 });
}

/**
 * A running card that subscribes to nudges, over a log the case can grow.  The bridge-state callback the card registers
 * is kept in window.__bridge; every bytes request URL is recorded in n.urls.
 */
async function nudgeCard(page, o) {
	const opts = { topic: 'term.nudge:t1', ...o };
	const n = { f: { log: Buffer.from('hello\r\n'), done: false }, urls: [] };
	await page.route(ORIGIN + '/**', function (route) {
		const u = new URL(route.request().url());
		if (u.pathname === '/t/bytes' && !u.searchParams.has('max'))
			n.urls.push(u.search);
		return server(n.f)(route);
	});
	await page.goto(ORIGIN + '/page');
	await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
	await page.evaluate(function (o) {
		window.__subs = [];
		window.__off = 0;
		window.__bridge = null;
		window.__delays = [];
		const realSetTimeout = window.setTimeout;
		window.setTimeout = function (fn, ms) {
			window.__delays.push(ms);
			return realSetTimeout.apply(window, arguments);
		};
		window.JuneauConsole = { contract: function () {
			return { bridges: [{ id: 'nb', transport: 'sse', session: '/s', downstream: [o.topic] }] };
		} };
		const ctx = {
			publish: function () {},
			subscribe: function (topic, fn) { window.__subs.push(topic); window.__bridge = fn; return function () { window.__off++; }; }
		};
		const h = window.JuneauConsoleCards.find(function (e) { return e[0] === 'terminal'; })[1];
		window.__h = h;
		const card = { id: 'card1', type: 'terminal', terminal: { bytesUrl: '/t/bytes', refreshMs: 3000 } };
		if (o.subscribe)
			card.subscribes = [{ topic: o.topic, as: 'nudge' }];
		h.render(card, document.getElementById('host'), ctx);
	}, opts);
	await waitRunning(page, 'host');
	return n;
}

/** Waits up to {@code ms} for a bytes request beyond the first {@code count}; returns the elapsed time, or -1. */
async function waitRequest(n, count, ms) {
	const t0 = Date.now();
	while (Date.now() - t0 < ms) {
		if (n.urls.length > count)
			return Date.now() - t0;
		await new Promise(function (r) { setTimeout(r, 25); });
	}
	return -1;
}

const sleep = function (ms) { return new Promise(function (r) { setTimeout(r, ms); }); };

const CASES = [
	['b01_umdBundleLoadsFromAClassicScript', async function (page) {
		await open(page, { log: Buffer.from('hello\r\n') });
		const r = await page.evaluate(function () {
			return {
				terminal: typeof window.Terminal,
				queued: (window.JuneauConsoleCards || []).some(function (e) { return e[0] === 'terminal'; }),
				region: document.querySelectorAll('.juneau-term[role=region] .juneau-term-screen .xterm').length
			};
		});
		expect(r.terminal === 'function', 'window.Terminal: ' + r.terminal);
		expect(r.queued, 'the terminal card handler is queued');
		expect(r.region === 1, 'one xterm inside the region: ' + r.region);
		expect((await rows(page))[0] === 'hello', 'first row');
	}],
	['b02_sgrColoursReachTheCellsAndTheBadgeSaysDone', async function (page) {
		await open(page, { log: Buffer.from('\u001b[31mred\u001b[0m plain \u001b[1;32mbold green\u001b[0m\r\n') });
		const r = await page.evaluate(function () {
			const spans = Array.from(document.querySelectorAll('.xterm-rows > div:first-child span'));
			const cls = function (t) { const s = spans.find(function (x) { return x.textContent === t; }); return s ? s.className : null; };
			const b = document.querySelector('.juneau-term-badge');
			return { red: cls('red'), green: cls('bold green'), badge: b.textContent, kind: b.className };
		});
		expect(/\bxterm-fg-1\b/.test(r.red || ''), 'red is palette 1: ' + r.red);
		expect(/\bxterm-fg-10\b/.test(r.green || '') && /\bxterm-bold\b/.test(r.green || ''), 'bold green is bright green (palette 10, xterm draws bold in bright colours): ' + r.green);
		expect(r.badge === 'Done' && /juneau-term-ok/.test(r.kind), 'badge: ' + r.badge + ' ' + r.kind);
	}],
	['b03_carriageReturnRedrawsInPlace', async function (page) {
		await open(page, { log: Buffer.from('10%\r50%\r100%\r\nnext\r\n') });
		const r = await rows(page);
		expect(r[0] === '100%' && r[1] === 'next', 'rows: ' + JSON.stringify(r.slice(0, 2)));
	}],
	['b04_altScreenBoxIsDrawn', async function (page) {
		await open(page, { log: Buffer.from('before\r\n\u001b[?1049h\u001b[H┌──┐\r\n│ok│\r\n└──┘') });
		const r = await rows(page);
		expect(r[0] === '┌──┐' && r[1] === '│ok│' && r[2] === '└──┘', 'box: ' + JSON.stringify(r.slice(0, 3)));
		expect(!r.includes('before'), 'the normal screen is hidden while the alternate one is up');
	}],
	['b05_scrollbackAndSearch', async function (page) {
		await open(page, { log: lines(300) });
		let r = await rows(page);
		expect(r.includes('line 299') && !r.includes('line 007'), 'follows to the bottom: ' + r[0]);
		await page.fill('.juneau-term-search', 'line 007');
		await page.press('.juneau-term-search', 'Enter');
		await page.waitForFunction(function () {
			return Array.from(document.querySelectorAll('.xterm-rows > div')).some(function (d) { return d.textContent.indexOf('line 007') >= 0; });
		}, null, { timeout: 5000 });
		const st = await page.evaluate(function () {
			return { follow: document.querySelector('.juneau-term-follow input').checked, invalid: document.querySelector('.juneau-term-search').getAttribute('aria-invalid') };
		});
		expect(!st.follow && st.invalid === 'false', 'search turns follow off: ' + JSON.stringify(st));
		await page.fill('.juneau-term-search', 'no such text');
		await page.press('.juneau-term-search', 'Enter');
		expect(await page.getAttribute('.juneau-term-search', 'aria-invalid') === 'true', 'a miss marks the box invalid');
	}],
	['b06_hashScrollsToTheStepOffset', async function (page) {
		const events = [
			{ ev: 'step', id: 'a', title: 'A', rawOffset: 500 },
			{ ev: 'step', id: 'b', title: 'B', rawOffset: 1500 },
			{ ev: 'done', status: 'ok' }
		];
		await open(page, { log: lines(300), events: events, hash: '#t-O500' });
		await waitRow0(page, 'line 050');
		await page.evaluate(function () { window.location.hash = '#t-O1500'; });
		await waitRow0(page, 'line 150');
		await page.evaluate(function () { window.location.hash = '#other-O500'; });
		await page.waitForTimeout(100);
		expect((await rows(page))[0] === 'line 150', 'a hash for another card is ignored');
	}],
	['b11_handleScrollsWithoutAHashChange', async function (page) {
		const events = [
			{ ev: 'step', id: 'a', title: 'A', rawOffset: 500 },
			{ ev: 'step', id: 'b', title: 'B', rawOffset: 1500 },
			{ ev: 'done', status: 'ok' }
		];
		await page.route(ORIGIN + '/**', server({ log: lines(300), events: events }));
		await page.goto(ORIGIN + '/page');
		await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
		expect(await page.evaluate(function () { return window.JuneauTerminal.handle('t'); }) === null, 'no handle before the card is mounted');
		await page.evaluate(function () {
			window.__len = history.length;
			window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), { id: 't', bytesUrl: '/t/bytes', eventsUrl: '/t/events' });
		});
		await page.waitForFunction(function () {
			const b = document.querySelector('.juneau-term-badge');
			return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
		});
		await page.evaluate(function () { window.JuneauTerminal.handle('t').scrollToOffset(1500); });
		await waitRow0(page, 'line 150');
		const r = await page.evaluate(function () { return { hash: location.hash, len: history.length - window.__len }; });
		expect(r.hash === '' && r.len === 0, 'no hash change and no history entry: ' + JSON.stringify(r));
		await page.evaluate(function () { window.__cleanup(); });
		expect(await page.evaluate(function () { return window.JuneauTerminal.handle('t'); }) === null, 'the handle goes away with the card');
	}],
	['b21_terminalOptionsReachTheXterm', async function (page) {
		await page.route(ORIGIN + '/**', server({ log: Buffer.from('aa\nbb\n'), cols: 80, rows: 24 }));
		await page.goto(ORIGIN + '/page');
		await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
		await page.evaluate(function () {
			const T = window.Terminal;
			window.__terms = [];
			window.Terminal = function (o) { const x = new T(o); window.__terms.push(x); return x; };
			window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), {
				id: 't', bytesUrl: '/t/bytes', terminal: { convertEol: true, cols: 3, disableStdin: false }
			});
		});
		await page.waitForFunction(function () {
			const b = document.querySelector('.juneau-term-badge');
			return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
		});
		const o = await page.evaluate(function () { const t = window.__terms[0]; return { eol: t.options.convertEol, cols: t.cols, stdin: t.options.disableStdin }; });
		expect(o.eol === true && o.cols === 80 && o.stdin === true, 'options: ' + JSON.stringify(o));
		const r = await rows(page);
		expect(r[0] === 'aa' && r[1] === 'bb', 'bare LF lines start at column 0: ' + JSON.stringify(r.slice(0, 3)));
	}],
	['b22_absentTerminalKeepsTheDefaultXtermOptions', async function (page) {
		await page.route(ORIGIN + '/**', server({ log: Buffer.from('aa\n'), cols: 80, rows: 24 }));
		await page.goto(ORIGIN + '/page');
		await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
		await page.evaluate(function () {
			const T = window.Terminal;
			window.__terms = [];
			window.Terminal = function (o) { const x = new T(o); window.__terms.push(x); return x; };
			window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), { id: 't', bytesUrl: '/t/bytes' });
		});
		await page.waitForFunction(function () {
			const b = document.querySelector('.juneau-term-badge');
			return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
		});
		const o = await page.evaluate(function () {
			const x = window.__terms[0].options;
			return { sb: x.scrollback, eol: x.convertEol, blink: x.cursorBlink, stdin: x.disableStdin };
		});
		expect(o.sb === 100000 && o.eol === false && o.blink === false && o.stdin === true, 'options: ' + JSON.stringify(o));
	}],
	['b07_fontScalesWithThePanelWhileColsAndRowsStay', async function (page) {
		await open(page, { log: Buffer.from('x'.repeat(80) + '\r\n'), cols: 80, rows: 24 });
		const measure = function () {
			return page.evaluate(function () {
				const xs = document.querySelector('.xterm-screen').getBoundingClientRect();
				const rowsEl = document.querySelector('.xterm-rows');
				return {
					host: document.getElementById('host').getBoundingClientRect().width, screen: xs.width,
					font: Number.parseFloat(getComputedStyle(rowsEl).fontSize), rows: rowsEl.children.length,
					first: rowsEl.children[0].textContent.length
				};
			});
		};
		const wide = await measure();
		await page.evaluate(function () { document.getElementById('host').style.width = '500px'; });
		await page.waitForFunction(function (f) { return Number.parseFloat(getComputedStyle(document.querySelector('.xterm-rows')).fontSize) < f; }, wide.font, { timeout: 5000 });
		const narrow = await measure();
		expect(wide.rows === 24 && narrow.rows === 24, 'rows stay 24: ' + wide.rows + ' ' + narrow.rows);
		expect(narrow.first === 80, 'the 80-column row is not rewrapped: ' + narrow.first);
		expect(wide.screen <= wide.host && narrow.screen <= narrow.host, 'the screen fits: ' + JSON.stringify([wide, narrow]));
		expect(narrow.font >= 8 && narrow.font < wide.font, 'the font shrinks: ' + wide.font + ' -> ' + narrow.font);
	}],
	['b08_osc52LeavesTheClipboardAlone', async function (page) {
		await open(page, { log: Buffer.from('\u001b]52;c;aGVsbG8=\u0007after\r\n') });
		const r = await page.evaluate(function () { return window.__clipboardWrites; });
		expect(Array.isArray(r) && r.length === 0, 'no clipboard writes: ' + JSON.stringify(r));
		expect((await rows(page))[0] === 'after', 'the sequence itself prints nothing');
	}],
	['b09_onlyHttpLinksOpen', async function (page) {
		await open(page, { log: Buffer.from(osc8Link('javascript:alert(1)', 'BAD') + ' ' + osc8Link('https://example.org/ok', 'GOOD') + '\r\n'), cols: 80, rows: 24 });
		const box = await page.evaluate(function () {
			const r = document.querySelector('.xterm-screen').getBoundingClientRect();
			return { x: r.left, y: r.top, w: r.width / 80, h: r.height / 24 };
		});
		const click = async function (col) {
			const x = box.x + (col + 0.5) * box.w, y = box.y + 0.5 * box.h;
			await page.mouse.move(x, y);
			await page.waitForTimeout(100);
			await page.mouse.click(x, y);
			await page.waitForTimeout(100);
		};
		await click(1);
		await click(5);
		const opened = await page.evaluate(function () { return window.__opened; });
		expect(JSON.stringify(opened) === JSON.stringify([['https://example.org/ok', '_blank', 'noopener,noreferrer']]), 'opened: ' + JSON.stringify(opened));
	}],
	['b10_rawLinkOnlyForHttpUrls', async function (page) {
		await open(page, { log: Buffer.from('hello\r\n') });
		const r = await page.evaluate(function () {
			const a = document.querySelectorAll('.juneau-term-raw');
			const host = document.createElement('div');
			host.id = 'host2';
			document.body.appendChild(host);
			window.JuneauTerminal.mount(host, { id: 'u', bytesUrl: 'data:,hi/bytes' });
			return { count: a.length, href: a.length ? a[0].getAttribute('href') : null, dataRaw: host.querySelectorAll('.juneau-term-raw').length, dataRegion: host.querySelectorAll('.juneau-term').length };
		});
		expect(r.count === 1 && r.href === ORIGIN + '/t/raw', 'an http(s) bytes URL gets a Raw link: ' + JSON.stringify(r));
		expect(r.dataRegion === 1 && r.dataRaw === 0, 'a non-http(s) bytes URL gets no Raw link: ' + JSON.stringify(r));
	}],
	['b12_mountWithoutInputHasNoToggle', async function (page) {
		await openRunning(page, { log: Buffer.from('hello\r\n') });
		await page.evaluate(function () {
			window.JuneauTerminal.mount(document.getElementById('host'), { id: 'a', bytesUrl: '/t/bytes' }, { publish: function () {}, onConnected: function () {} });
			window.JuneauTerminal.mount(document.getElementById('host2'), { id: 'b', bytesUrl: '/t/bytes', input: true, inputId: 't1' });
		});
		await waitRunning(page, 'host');
		await waitRunning(page, 'host2');
		const r = await page.evaluate(function () {
			return {
				toggles: document.querySelectorAll('.juneau-term-input').length,
				chips: document.querySelectorAll('.juneau-term-chip--input').length,
				stdin: window.__terms.map(function (t) { return t.options.disableStdin; })
			};
		});
		expect(r.toggles === 0 && r.chips === 0, 'no input chrome without input: true and a link: ' + JSON.stringify(r));
		expect(JSON.stringify(r.stdin) === '[true,true]', 'disableStdin stays set: ' + JSON.stringify(r.stdin));
	}],
	['b13_inputCardArmsTheTerminal', async function (page) {
		await openRunning(page, { log: Buffer.from('Continue? [y/N] ') });
		await page.evaluate(function () {
			window.__sent = [];
			const link = { publish: function (p) { window.__sent.push(p); }, onConnected: function (fn) { fn(true); } };
			window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), { id: 'card1', bytesUrl: '/t/bytes', input: true, inputId: 't1' }, link);
		});
		await waitRunning(page, 'host');
		const before = await page.evaluate(function () {
			const bar = document.querySelector('#host .juneau-term-bar');
			const kids = Array.from(bar.children);
			const at = function (sel) { return kids.indexOf(bar.querySelector(sel)); };
			const toggle = bar.querySelector('.juneau-term-input');
			const chip = bar.querySelector('.juneau-term-chip--input');
			return {
				order: [at('.juneau-term-input'), at('.juneau-term-chip--input'), at('.juneau-term-badge')],
				tag: toggle?.tagName, type: toggle?.type, label: toggle?.textContent,
				pressed: toggle?.getAttribute('aria-pressed'), chipRole: chip?.getAttribute('role'), chipHidden: chip?.hidden,
				stdin: window.__terms[0].options.disableStdin
			};
		});
		expect(before.order[0] >= 0 && before.order[0] < before.order[1] && before.order[1] < before.order[2], 'toggle, chip, then badge: ' + JSON.stringify(before.order));
		expect(before.tag === 'BUTTON' && before.type === 'button' && before.label === 'Enable input' && before.pressed === 'false', 'a real toggle button: ' + JSON.stringify(before));
		expect(before.chipRole === 'status' && before.chipHidden === true, 'its own status chip, hidden while idle: ' + JSON.stringify(before));
		expect(before.stdin === true, 'stdin disabled before arming');
		await page.click('#host .juneau-term-input');
		expect(JSON.stringify(await page.evaluate(function () { return window.__sent; })) === JSON.stringify([{ id: 't1', data: '' }]), 'the press probes');
		expect(await page.evaluate(function () { return window.__terms[0].options.disableStdin; }) === true, 'a probe alone does not arm');
		await page.evaluate(function () {
			const h = window.JuneauConsoleCards.find(function (e) { return e[0] === 'terminal'; })[1];
			h.roles['input-status']({ id: 't1', state: 'live' }, {}, { id: 'card1' });
		});
		const armed = await page.evaluate(function () {
			const chip = document.querySelector('#host .juneau-term-chip--input');
			return {
				stdin: window.__terms[0].options.disableStdin, pressed: document.querySelector('#host .juneau-term-input').getAttribute('aria-pressed'),
				chip: chip.textContent, chipState: chip.dataset.state, chipHidden: chip.hidden,
				badge: document.querySelector('#host .juneau-term-badge').textContent
			};
		});
		expect(armed.stdin === false && armed.pressed === 'true', 'live arms: ' + JSON.stringify(armed));
		expect(armed.chip === 'Input live' && armed.chipState === 'live' && armed.chipHidden === false, 'chip: ' + JSON.stringify(armed));
		expect(armed.badge === 'Running', 'the run-status badge is not written by input state: ' + armed.badge);
		await page.keyboard.type('y');
		await page.waitForFunction(function () { return window.__sent.some(function (p) { return p.id === 't1' && p.data === 'y'; }); }, null, { timeout: 5000 });
		await page.click('#host .juneau-term-input');
		const off = await page.evaluate(function () {
			return { stdin: window.__terms[0].options.disableStdin, pressed: document.querySelector('#host .juneau-term-input').getAttribute('aria-pressed') };
		});
		expect(off.stdin === true && off.pressed === 'false', 'the toggle disarms: ' + JSON.stringify(off));
		await page.evaluate(function () { window.__cleanup(); });
	}],
	['b14_inputStatusRoleReachesItsCard', async function (page) {
		await openRunning(page, { log: Buffer.from('hello\r\n') });
		await page.evaluate(function () {
			window.__pub = [];
			window.__subs = [];
			window.JuneauConsole = { contract: function () {
				return { bridges: [{ id: 'term', transport: 'websocket', session: '/s', downstream: ['term.input-status'], upstream: ['term.input'] }] };
			} };
			const ctx = {
				publish: function (topic, p) { window.__pub.push([topic, p]); },
				subscribe: function (topic, fn) { window.__subs.push(topic); fn({ state: 'open' }); return function () {}; }
			};
			const h = window.JuneauConsoleCards.find(function (e) { return e[0] === 'terminal'; })[1];
			window.__h = h;
			h.render({ id: 'card1', type: 'terminal', terminal: { bytesUrl: '/t/bytes', input: true, inputId: 't1' } }, document.getElementById('host'), ctx);
			h.render({ id: 'card2', type: 'terminal', terminal: { bytesUrl: '/t/bytes', input: true, inputId: 't1' } }, document.getElementById('host2'), ctx);
		});
		await waitRunning(page, 'host');
		await waitRunning(page, 'host2');
		await page.click('#host .juneau-term-input');
		await page.click('#host2 .juneau-term-input');
		await page.evaluate(function () {
			window.__h.roles['input-status']({ id: 't1', state: 'live' }, {}, { id: 'card1' });
			window.__h.roles['input-status']({ id: 't1', state: 'live' }, {}, { id: 'nope' });
		});
		const r = await page.evaluate(function () {
			const p = function (h) { // NOSONAR javascript:S7721 - runs inside the browser page; it must stay in the serialized function
 return document.querySelector('#' + h + ' .juneau-term-input').getAttribute('aria-pressed'); };
			return { one: p('host'), two: p('host2'), pub: window.__pub, subs: window.__subs };
		});
		expect(r.one === 'true' && r.two === 'false', 'only card1 armed: ' + JSON.stringify(r));
		expect(JSON.stringify(r.pub) === JSON.stringify([['term.input', { id: 't1', data: '' }], ['term.input', { id: 't1', data: '' }]]), 'probes go out on term.input: ' + JSON.stringify(r.pub));
		expect(JSON.stringify(r.subs) === JSON.stringify(['bridge:term', 'bridge:term']), 'each card watches the bridge state: ' + JSON.stringify(r.subs));
		await page.evaluate(function () {
			window.__h.destroy({ id: 'card1' }, document.getElementById('host'));
			window.__h.roles['input-status']({ id: 't1', state: 'closed' }, {}, { id: 'card1' });
		});
		expect(await page.evaluate(function () { return document.querySelectorAll('#host .juneau-term').length; }) === 0, 'card1 is gone, and a late answer for it is a no-op');
	}],
	['b16_nudgeRoleFetchesAtOnce', async function (page) {
		const n = await nudgeCard(page, { subscribe: true });
		const before = n.urls.length;
		await page.evaluate(function () {
			window.__bridge({ state: 'open' });
			window.__h.roles.nudge({ end: 999 }, {}, { id: 'nope' });
		});
		await sleep(300);
		expect(n.urls.length === before, 'a card that is not mounted ignores the nudge: ' + n.urls.slice(before).join(' '));
		await page.evaluate(function () { window.__h.roles.nudge({ end: 999 }, {}, { id: 'card1' }); });
		const took = await waitRequest(n, before, 1000);
		expect(took >= 0, 'a bytes request follows the nudge well before the 3000 ms poll: ' + n.urls.slice(before).join(' '));
		expect(n.urls[before] === '?from=7', 'it resumes from next: ' + n.urls[before]);
		expect(JSON.stringify(await page.evaluate(function () { return window.__subs; })) === JSON.stringify(['bridge:nb']), 'the card watches the bridge that carries the topic');
		await page.evaluate(function () { window.__h.destroy({ id: 'card1' }, document.getElementById('host')); });
	}],
	['b17_cardWithoutNudgeSubscriptionPollsNormally', async function (page) {
		const n = await nudgeCard(page, { subscribe: false });
		const before = n.urls.length;
		await page.evaluate(function () { window.__h.roles.nudge({ end: 999 }, {}, { id: 'card1' }); });
		await sleep(300);
		expect(n.urls.length === before, 'a nudge for a card that does not subscribe is a no-op: ' + n.urls.slice(before).join(' '));
		const r = await page.evaluate(function () {
			return { subs: window.__subs, badge: document.querySelector('#host .juneau-term-badge').textContent };
		});
		expect(r.subs.length === 0, 'no bridge subscription: ' + JSON.stringify(r.subs));
		expect(r.badge === 'Running', 'still polling: ' + r.badge);
		await page.evaluate(function () { window.__h.destroy({ id: 'card1' }, document.getElementById('host')); });
	}],
	['b18_destroyUnregistersTheNudgeLink', async function (page) {
		const n = await nudgeCard(page, { subscribe: true });
		await page.evaluate(function () { window.__bridge({ state: 'open' }); });
		await page.evaluate(function () { window.__h.destroy({ id: 'card1' }, document.getElementById('host')); });
		const before = n.urls.length;
		await page.evaluate(function () { window.__h.roles.nudge({ end: 999 }, {}, { id: 'card1' }); });
		await sleep(300);
		expect(n.urls.length === before, 'a nudge after destroy is a no-op: ' + n.urls.slice(before).join(' '));
		expect(await page.evaluate(function () { return window.__off; }) === 1, 'the bridge-state subscription was released');
	}],
	['b19_nudgeFetchesAndRestores', async function (page) {
		const n = await nudgeCard(page, { subscribe: true });
		const first = n.urls.length;
		expect(first >= 1 && n.urls[0] === '?from=0', 'the first bytes request: ' + n.urls.join(' '));
		expect(await page.evaluate(function () { return window.__delays[window.__delays.length - 1]; }) === 3000, 'the normal poll is scheduled');
		await page.evaluate(function () { window.__bridge({ state: 'open' }); window.__mark = window.__delays.length; });
		n.f.log = Buffer.concat([n.f.log, Buffer.from('more\r\n')]);
		await page.evaluate(function () { window.__h.roles.nudge({ end: 13 }, {}, { id: 'card1' }); });
		expect(await waitRequest(n, first, 1200) >= 0, 'only the nudge can explain a fetch inside the 3000 ms poll: ' + n.urls.slice(first).join(' '));
		expect(n.urls[first] === '?from=7', 'resumes from the previous next: ' + n.urls[first]);
		await page.waitForFunction(function () { return window.__delays.slice(window.__mark).indexOf(5000) >= 0; }, null, { timeout: 3000 });
		await page.evaluate(function () { window.__mark = window.__delays.length; window.__bridge({ state: 'closed' }); });
		await page.waitForFunction(function () { return window.__delays.slice(window.__mark).indexOf(3000) >= 0; }, null, { timeout: 3000 });
		await page.evaluate(function () { window.__h.destroy({ id: 'card1' }, document.getElementById('host')); });
	}],
	['b20_wildcardNudgeTopicNeverLinks', async function (page) {
		const n = await nudgeCard(page, { subscribe: true, topic: 'term.nudge:*' });
		const before = n.urls.length;
		await page.evaluate(function () { window.__h.roles.nudge({ end: 999 }, {}, { id: 'card1' }); });
		await sleep(300);
		expect(n.urls.length === before, 'no nudge routing for a wildcard topic: ' + n.urls.slice(before).join(' '));
		expect(await page.evaluate(function () { return window.__subs.length; }) === 0, 'no bridge subscription');
		await page.evaluate(function () { window.__h.destroy({ id: 'card1' }, document.getElementById('host')); });
	}],
	['b23_directMountNudgeFetchesAtOnce', async function (page) {
		const n = { f: { log: Buffer.from('hello\r\n'), done: false }, urls: [] };
		await page.route(ORIGIN + '/**', function (route) {
			const u = new URL(route.request().url());
			if (u.pathname === '/t/bytes' && !u.searchParams.has('max'))
				n.urls.push(u.search);
			return server(n.f)(route);
		});
		await page.goto(ORIGIN + '/page');
		await page.waitForFunction(function () { return typeof window.Terminal === 'function' && !!window.JuneauTerminal; });
		// No card, no nudge link: the page mounts directly and nudges through what mount gives back.
		await page.evaluate(function () {
			window.__cleanup = window.JuneauTerminal.mount(document.getElementById('host'), { id: 'lane_x', bytesUrl: '/t/bytes', refreshMs: 3000 });
		});
		await waitRunning(page, 'host');
		expect(await page.evaluate(function () { return typeof window.JuneauTerminal.handle('lane_x').nudge; }) === 'function', 'the handle has nudge');
		expect(await page.evaluate(function () { return window.JuneauTerminal.handle('lane_x').nudge === window.__cleanup.nudge; }), 'the cleanup carries the same nudge, for a mount without an id');
		const first = n.urls.length;
		await page.evaluate(function () { window.JuneauTerminal.handle('lane_x').nudge({ end: 7 }); });
		await sleep(300);
		expect(n.urls.length === first, 'a nudge at the current offset is ignored: ' + n.urls.slice(first).join(' '));
		n.f.log = Buffer.concat([n.f.log, Buffer.from('more\r\n')]);
		await page.evaluate(function () { window.JuneauTerminal.handle('lane_x').nudge({ end: 13 }); });
		expect(await waitRequest(n, first, 1200) >= 0, 'only the nudge can explain a fetch inside the 3000 ms poll: ' + n.urls.slice(first).join(' '));
		expect(n.urls[first] === '?from=7', 'resumes from the previous next: ' + n.urls[first]);
		const after = n.urls.length;
		await page.evaluate(function () { window.__cleanup.nudge({ end: 999 }); });
		expect(await waitRequest(n, after, 1200) >= 0, 'the nudge on the returned cleanup works too');
		await page.evaluate(function () { window.__cleanup(); window.__cleanup.nudge({ end: 9999 }); window.__cleanup.nudge(null); });
		const done = n.urls.length;
		await sleep(300);
		expect(n.urls.length === done, 'a nudge after cleanup is a no-op: ' + n.urls.slice(done).join(' '));
	}],
	// HTT: the badge, ok badge, chip and banner must stay legible once the views CSS defines --jc-tone-* (dark inks).
	['b15_toneBackgroundsStayLegibleWithViewsCss', async function (page) {
		await page.route(ORIGIN + '/**', server({ log: Buffer.from('x') }));
		await page.goto(ORIGIN + '/page');
		const light = await page.evaluate(toneProbe);
		light.forEach(function (r) { expect(r.ratio >= 4.5, 'legible with views CSS only: ' + JSON.stringify(r)); });
		// A theme (e.g. dark) that overrides the tag palette must reach all four.
		await page.addStyleTag({ content: ':root{--jc-tag-blue-bg:rgb(1,2,3);--jc-tag-blue-text:rgb(250,250,250);--jc-tag-green-bg:rgb(2,3,4);--jc-tag-green-text:rgb(250,250,250);'
			+ '--jc-tag-amber-bg:rgb(3,4,5);--jc-tag-amber-text:rgb(250,250,250)}' });
		const dark = await page.evaluate(toneProbe);
		const want = ['rgb(1, 2, 3)', 'rgb(2, 3, 4)', 'rgb(3, 4, 5)', 'rgb(3, 4, 5)'];
		dark.forEach(function (r, i) { expect(r.bg === want[i] && r.color === 'rgb(250, 250, 250)', 'follows the tag palette: ' + JSON.stringify(r)); });
		// The neutral "Input live" chip must not pick up the chip's amber text.
		const live = await page.evaluate(function () {
			const par = document.createElement('div');
			par.style.color = 'rgb(10, 20, 30)';
			par.innerHTML = '<span class="juneau-term-chip juneau-term-chip--input" data-state="live">Input live</span>';
			document.body.appendChild(par);
			return { chip: getComputedStyle(par.firstChild).color, parent: getComputedStyle(par).color };
		});
		expect(live.chip === live.parent, 'live chip inherits its parent colour: ' + JSON.stringify(live));
	}],
	// The dark scheme re-themes the tone backgrounds and the screen; b15 above covers the light scheme.
	['b15d_toneBackgroundsLegibleUnderDark', async function (page) {
		await page.route(ORIGIN + '/**', server({ log: Buffer.from('x') }));
		await page.goto(ORIGIN + '/page');
		await page.evaluate(function () { document.documentElement.dataset.jcScheme = 'dark'; });
		const r = await page.evaluate(function () {
			const lum = function (c) { // NOSONAR javascript:S7721 - runs inside the browser page; it must stay in the serialized function
				const m = c.match(/[\d.]+/g).map(Number).slice(0, 3).map(function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
				return 0.2126 * m[0] + 0.7152 * m[1] + 0.0722 * m[2];
			};
			const out = ['juneau-term-badge', 'juneau-term-ok', 'juneau-term-chip', 'juneau-term-banner', 'juneau-term-fail'].map(function (cls) {
				const el = document.createElement('span');
				el.className = cls;
				el.textContent = 'x';
				document.body.appendChild(el);
				const cs = getComputedStyle(el);
				const a = lum(cs.backgroundColor), b = lum(cs.color);
				return { cls: cls, bg: cs.backgroundColor, color: cs.color, ratio: (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05) };
			});
			const screen = document.createElement('div');
			screen.className = 'juneau-term-screen';
			document.body.appendChild(screen);
			return { tones: out, screen: getComputedStyle(screen).backgroundColor };
		});
		r.tones.forEach(function (t) { expect(t.ratio >= 4.5, 'legible under dark: ' + JSON.stringify(t)); });
		expect(r.screen === 'rgb(13, 15, 18)', 'screen follows --jc-term-bg: ' + r.screen);
	}]
];

(async function main() {
	const report = { cases: {}, pageErrors: {}, consoleErrors: {} };
	const browser = await chromium.launch();
	try {
		for (const [name, fn] of CASES) {
			const ctx = await browser.newContext({ viewport: { width: 1200, height: 900 } });
			await ctx.addInitScript(function () {
				window.__clipboardWrites = [];
				window.__opened = [];
				window.open = function (u, t, f) { window.__opened.push([u, t, f]); return null; };
				Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: function (s) { window.__clipboardWrites.push(s); return Promise.resolve(); } } });
			});
			const page = await ctx.newPage();
			const pageErrors = [], consoleErrors = [];
			page.on('pageerror', function (e) { pageErrors.push(String(e?.stack || e)); });
			page.on('console', function (m) { if (m.type() === 'error') consoleErrors.push(m.text()); });
			try {
				await fn(page);
				report.cases[name] = true;
			} catch (e) {
				report.cases[name] = String(e?.stack || e);
			}
			report.pageErrors[name] = pageErrors;
			report.consoleErrors[name] = consoleErrors;
			await ctx.close();
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report));
})().catch(function (e) {
	process.stdout.write(JSON.stringify({ harnessError: String(e?.stack || e) }));
});
