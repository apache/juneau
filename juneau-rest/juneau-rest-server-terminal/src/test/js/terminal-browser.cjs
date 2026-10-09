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
 *   node terminal-browser.cjs <xterm.js> <xterm.css> <juneau-terminal.js> <juneau-terminal.css>
 *
 * Every request goes to the made-up origin http://term.test and is answered by page.route, so nothing listens on a
 * port.  The bytes and events endpoints are scripted here; their real headers are covered by TerminalMixin_Test and
 * by the end-to-end smoke test.  Prints one JSON report to stdout:
 *   {cases: {name: true | "failure"}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}}
 */

const fs = require('fs');
const { chromium } = require('playwright');

const ORIGIN = 'http://term.test';
const [XTERM_JS, XTERM_CSS, TERM_JS, TERM_CSS] = process.argv.slice(2).map(function (f) { return fs.readFileSync(f); });

const PAGE = '<!DOCTYPE html><html><head><meta charset="utf-8">'
	+ '<link rel="stylesheet" href="/a/xterm.css"><link rel="stylesheet" href="/a/juneau-terminal.css">'
	+ '<script src="/a/xterm.js"></script><script src="/a/juneau-terminal.js"></script>'
	+ '</head><body><div id="host" style="width:1000px"></div></body></html>';

const ASSETS = {
	'/page': ['text/html', Buffer.from(PAGE)],
	'/a/xterm.js': ['text/javascript', XTERM_JS],
	'/a/xterm.css': ['text/css', XTERM_CSS],
	'/a/juneau-terminal.js': ['text/javascript', TERM_JS],
	'/a/juneau-terminal.css': ['text/css', TERM_CSS]
};

/** {log, done, cols, rows, events} -> a page.route handler for the page, its assets and the two endpoints. */
function server(fx) {
	const f = Object.assign({ done: true, cols: 80, rows: 24, events: null }, fx);
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
	}, { opts: Object.assign({ id: 't', bytesUrl: '/t/bytes' }, fx.events ? { eventsUrl: '/t/events' } : {}, opts || {}), hash: fx.hash || null });
	await page.waitForFunction(function () {
		const b = document.querySelector('.juneau-term-badge');
		return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
	});
}

function rows(page) {
	return page.evaluate(function () {
		return Array.from(document.querySelectorAll('.xterm-rows > div')).map(function (d) {
			return d.textContent.replace(/\u00a0/g, ' ').replace(/\s+$/, '');
		});
	});
}

async function waitRow0(page, text) {
	await page.waitForFunction(function (t) {
		const r = document.querySelector('.xterm-rows > div');
		return r && r.textContent.replace(/\u00a0/g, ' ').trim() === t;
	}, text, { timeout: 5000 });
}

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
	['b07_fontScalesWithThePanelWhileColsAndRowsStay', async function (page) {
		await open(page, { log: Buffer.from('x'.repeat(80) + '\r\n'), cols: 80, rows: 24 });
		const measure = function () {
			return page.evaluate(function () {
				const xs = document.querySelector('.xterm-screen').getBoundingClientRect();
				const rowsEl = document.querySelector('.xterm-rows');
				return {
					host: document.getElementById('host').getBoundingClientRect().width, screen: xs.width,
					font: parseFloat(getComputedStyle(rowsEl).fontSize), rows: rowsEl.children.length,
					first: rowsEl.children[0].textContent.length
				};
			});
		};
		const wide = await measure();
		await page.evaluate(function () { document.getElementById('host').style.width = '500px'; });
		await page.waitForFunction(function (f) { return parseFloat(getComputedStyle(document.querySelector('.xterm-rows')).fontSize) < f; }, wide.font, { timeout: 5000 });
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
		const link = function (href, text) { return '\u001b]8;;' + href + '\u0007' + text + '\u001b]8;;\u0007'; };
		await open(page, { log: Buffer.from(link('javascript:alert(1)', 'BAD') + ' ' + link('https://example.org/ok', 'GOOD') + '\r\n'), cols: 80, rows: 24 });
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
			page.on('pageerror', function (e) { pageErrors.push(String(e && e.stack || e)); });
			page.on('console', function (m) { if (m.type() === 'error') consoleErrors.push(m.text()); });
			try {
				await fn(page);
				report.cases[name] = true;
			} catch (e) {
				report.cases[name] = String(e && e.stack || e);
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
	process.stdout.write(JSON.stringify({ harnessError: String(e && e.stack || e) }));
});
