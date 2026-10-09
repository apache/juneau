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
 * console-output-browser.cjs - real-browser prober for the console-output region.  One Playwright page per case,
 * each against its own log on the ConsoleOutput_BrowserTest server.  Writes one JSON report to stdout:
 *   { cases: {name: {...}}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}, diagnostics: [...] }
 *
 * Never runs in a default build.  Driven by ConsoleOutput_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node console-output-browser.cjs <baseUrl>
 */
'use strict';

const { chromium } = require('playwright');

const THEMES = ['gray', 'light-brown', 'light-red', 'open', 'red'];
const GONE_TEXT = 'the log was replaced or truncated (410 Gone); reload to view it from the start';

const [baseUrl] = process.argv.slice(2);
if (!baseUrl) {
	process.stderr.write('usage: node console-output-browser.cjs <baseUrl>\n');
	process.exit(2);
}

function sleep(ms) {
	return new Promise(r => setTimeout(r, ms));
}

async function ctl(page, log, action, query) {
	const r = await page.request.get(baseUrl + 'ctl/' + log + '/' + action + (query ? '?' + query : ''));
	if (!r.ok())
		throw new Error('ctl ' + log + '/' + action + ' -> ' + r.status() + ': ' + (await r.text()));
	return r.text();
}

async function count(page, log) {
	const m = JSON.parse(await ctl(page, '_', 'count'));
	return m[log] || 0;
}

async function open(page, c, opts) {
	const o = opts || {};
	await page.goto(baseUrl + 'page/' + c + '?theme=' + (o.theme || 'gray') + (o.hash || ''));
	await page.waitForFunction(() => !!(window.JuneauViews && window.JuneauViews.consoleOutput), null, { timeout: 15000 });
}

async function waitLines(page, min, timeout) {
	await page.waitForFunction(m => document.querySelectorAll('.juneau-co-line').length >= m, min, { timeout: timeout || 10000 });
}

async function waitText(page, selector, text, timeout) {
	await page.waitForFunction(a => {
		const e = document.querySelector(a[0]);
		return !!e && e.textContent.indexOf(a[1]) >= 0;
	}, [selector, text], { timeout: timeout || 10000 });
}

/* Pane snapshot; serialized into the page by page.evaluate, so it must be self-contained. */
const PANE = function () {
	const p = document.querySelector('.juneau-co-pane');
	const ns = Array.from(p.querySelectorAll('.juneau-co-line')).map(r => Number(r.getAttribute('data-n')));
	let ascending = true;
	for (let i = 1; i < ns.length; i++)
		if (ns[i] !== ns[i - 1] + 1) ascending = false;
	const jump = document.querySelector('.juneau-co-jump');
	return {
		first: ns[0], last: ns[ns.length - 1], count: ns.length, ascending: ascending,
		atBottom: p.scrollTop + p.clientHeight >= p.scrollHeight - 2,
		jumpHidden: jump.hidden, jumpText: jump.textContent,
		state: document.querySelector('.juneau-co-state').textContent,
		elapsed: document.querySelector('.juneau-co-elapsed').textContent
	};
};

/* a09: the contrast of every sample:<key> text node against its first opaque ancestor background, opacity applied. */
const CONTRAST = function () {
	function rgb(s) {
		const m = /rgba?\(([^)]+)\)/.exec(s);
		if (!m) return null;
		const p = m[1].split(/[\s,/]+/).filter(Boolean).map(Number);
		return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
	}
	function lum(c) {
		const f = function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
		return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b);
	}
	function background(el) {
		for (let e = el; e; e = e.parentElement) {
			const c = rgb(getComputedStyle(e).backgroundColor);
			if (c && c.a > 0) return c;
		}
		return { r: 255, g: 255, b: 255, a: 1 };
	}
	const out = {};
	const walker = document.createTreeWalker(document.querySelector('.juneau-co-pane'), NodeFilter.SHOW_TEXT);
	for (let n = walker.nextNode(); n; n = walker.nextNode()) {
		const m = /sample:([a-z0-9-]+)/.exec(n.data);
		if (!m) continue;
		const el = n.parentElement;
		const fg = rgb(getComputedStyle(el).color);
		let op = fg.a;
		for (let e = el; e; e = e.parentElement) op *= Number(getComputedStyle(e).opacity);
		const bg = background(el);
		const mix = { r: fg.r * op + bg.r * (1 - op), g: fg.g * op + bg.g * (1 - op), b: fg.b * op + bg.b * (1 - op) };
		const l1 = lum(mix);
		const l2 = lum(bg);
		out[m[1]] = Math.round(((Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)) * 100) / 100;
	}
	return out;
};

const CASES = [];
function kase(name, fn) { CASES.push([name, fn]); }

kase('a01', async page => {
	await open(page, 'a01');
	await waitLines(page, 30);
	const s1 = await page.evaluate(PANE);
	await sleep(2200);
	const s2 = await page.evaluate(PANE);
	return {
		first: s1.first, last: s1.last, count: s1.count, ascending: s1.ascending, stuck: s1.atBottom,
		ticking: s1.elapsed !== s2.elapsed && /^Elapsed \d\d:\d\d:\d\d$/.test(s2.elapsed)
	};
});

kase('a02', async page => {
	await open(page, 'a02');
	await waitLines(page, 40);
	await page.evaluate(() => { document.querySelector('.juneau-co-pane').scrollTop = 0; });
	await sleep(300);
	await ctl(page, 'a02', 'append', 'n=5');
	await page.waitForFunction(() => {
		const j = document.querySelector('.juneau-co-jump');
		return !j.hidden && j.textContent === 'Jump to latest (5 new)';
	}, null, { timeout: 10000 });
	const before = await page.evaluate(PANE);
	await page.click('.juneau-co-jump');
	await sleep(300);
	const after = await page.evaluate(PANE);
	await ctl(page, 'a02', 'append', 'n=2');
	await waitLines(page, 47);
	await sleep(200);
	const again = await page.evaluate(PANE);
	return {
		unstuck: !before.atBottom, jumpText: before.jumpText,
		afterJumpAtBottom: after.atBottom, afterJumpHidden: after.jumpHidden, reStuck: again.atBottom
	};
});

kase('a03', async page => {
	await open(page, 'a03');
	await waitLines(page, 5);
	await ctl(page, 'a03', 'complete');
	await waitText(page, '.juneau-co-state', 'SUCCEEDED');
	await sleep(500);
	const s = await page.evaluate(PANE);
	const c1 = await count(page, 'a03');
	await sleep(3500);
	const c2 = await count(page, 'a03');
	return { state: s.state.trim(), durationFormat: /^Duration \d\d:\d\d:\d\d$/.test(s.elapsed), requestsAfterTerminal: c2 - c1 };
});

kase('a04', async page => {
	await open(page, 'a04', { hash: '#L12' });
	await waitLines(page, 5);
	await ctl(page, 'a04', 'append', 'n=10');
	await page.waitForFunction(() => !!document.getElementById('L12'), null, { timeout: 10000 });
	await sleep(500);
	return page.evaluate(() => {
		const el = document.getElementById('L12');
		const p = document.querySelector('.juneau-co-pane').getBoundingClientRect();
		const r = el.getBoundingClientRect();
		return { found: true, target: el.classList.contains('juneau-co-target'), visible: r.top >= p.top - 1 && r.bottom <= p.bottom + 1 };
	});
});

kase('a05', async page => {
	await open(page, 'a05');
	await waitLines(page, 2);
	await page.focus('.juneau-co-pane');
	await page.keyboard.press('Tab');
	await sleep(100);
	const first = await page.evaluate(() => {
		const a = document.activeElement;
		const tip = document.querySelector('.juneau-co-tooltip');
		return {
			firstIsBlock: a.classList.contains('juneau-co-block'), tipShown: !tip.hidden, tipText: tip.textContent,
			describedBy: a.getAttribute('aria-describedby') === tip.id
		};
	});
	await page.keyboard.press('Escape');
	await sleep(100);
	const esc = await page.evaluate(() => ({
		tipHiddenAfterEscape: document.querySelector('.juneau-co-tooltip').hidden,
		describedByAfterEscape: document.activeElement.hasAttribute('aria-describedby')
	}));
	await page.keyboard.press('Tab');
	await sleep(100);
	const second = await page.evaluate(() => {
		const a = document.activeElement;
		return { secondIsLink: a.tagName === 'A' && a.classList.contains('juneau-co-block'), secondHref: a.getAttribute('href') };
	});
	return Object.assign({}, first, esc, second);
});

kase('a06', async page => {
	await open(page, 'a06');
	await page.waitForFunction(() => !!(window.jQuery?.fn?.DataTable && window.JuneauDataTables), null, { timeout: 15000 });
	await page.waitForSelector('table[data-juneau-view="runs"] > tbody > tr[data-juneau-row-id]', { timeout: 15000 });
	const control = 'table[data-juneau-view="runs"] > tbody > tr[data-juneau-row-id] td.juneau-view-detail-control';
	await page.click(control);
	await page.waitForFunction(() => document.querySelectorAll('.juneau-view-detail-panel .juneau-co-line').length >= 5, null, { timeout: 15000 });
	const shape = await page.evaluate(() => ({
		compact: !!document.querySelector('.juneau-view-detail-panel .juneau-co.juneau-co-compact'),
		firstText: document.querySelector('.juneau-view-detail-panel .juneau-co-line .juneau-co-text').textContent
	}));
	const c0 = await count(page, '1');
	await sleep(2500);
	const c1 = await count(page, '1');
	await page.click(control);
	await page.waitForFunction(() => !document.querySelector('.juneau-co'), null, { timeout: 10000 });
	await sleep(300);
	const c2 = await count(page, '1');
	await sleep(3000);
	const c3 = await count(page, '1');
	return { compact: shape.compact, firstText: shape.firstText, requestsWhileOpen: c1 - c0, requestsAfterCollapse: c3 - c2 };
});

kase('a07', async page => {
	await open(page, 'a07');
	await waitLines(page, 10);
	const start = await page.evaluate(() => ({
		dimAtStart: document.querySelector('.juneau-co').classList.contains('juneau-co-markers-dim'),
		toggleVisible: !document.querySelector('.juneau-co-markers').hidden
	}));
	await page.click('.juneau-co-markers');
	await sleep(100);
	const hidden = await page.evaluate(() => {
		const row = document.querySelector('.juneau-co-line[data-n="3"]');
		return {
			hidden: document.querySelector('.juneau-co').classList.contains('juneau-co-markers-hide'),
			markerDisplay: getComputedStyle(row).display, markerInDom: !!row
		};
	});
	await page.reload();
	await page.waitForFunction(() => !!(window.JuneauViews && window.JuneauViews.consoleOutput), null, { timeout: 15000 });
	await waitLines(page, 10);
	const reloaded = await page.evaluate(() => document.querySelector('.juneau-co').classList.contains('juneau-co-markers-hide'));
	return Object.assign({}, start, hidden, { hiddenAfterReload: reloaded });
});

kase('a08', async page => {
	await page.setViewportSize({ width: 1200, height: 1100 });
	await open(page, 'a08');
	await waitLines(page, 60);
	const m = await page.evaluate(() => {
		const p = document.querySelector('.juneau-co-pane');
		const cs = getComputedStyle(p);
		const lh = parseFloat(cs.lineHeight);
		const content = p.clientHeight - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom);
		return { rows: Math.round((content / lh) * 10) / 10, height: p.getBoundingClientRect().height };
	});
	const box = await page.locator('.juneau-co-pane').boundingBox();
	await page.mouse.move(box.x + box.width - 3, box.y + box.height - 3);
	await page.mouse.down();
	await page.mouse.move(box.x + box.width - 3, box.y + box.height + 120, { steps: 10 });
	await page.mouse.up();
	await sleep(100);
	const h2 = await page.evaluate(() => document.querySelector('.juneau-co-pane').getBoundingClientRect().height);
	return { rows: m.rows, grew: Math.round(h2 - m.height) };
});

kase('a09', async (page, record) => {
	const themes = {};
	for (const t of THEMES) {
		const p = await record('a09-' + t);
		await open(p, 'a09', { theme: t });
		await waitLines(p, 14);
		await sleep(200);
		themes[t] = await p.evaluate(CONTRAST);
		await p.close();
	}
	return { themes: themes };
});

kase('a10', async page => {
	await open(page, 'a10');
	await page.waitForSelector('.jc-console-error [data-juneau-error]', { timeout: 10000 });
	return page.evaluate(() => {
		const item = document.querySelector('.jc-console-error [data-juneau-error]');
		return {
			code: item.getAttribute('data-juneau-error'),
			role: document.querySelector('.jc-console-error').getAttribute('role'),
			text: item.textContent,
			regionState: document.querySelector('[data-juneau-region="a10"]').getAttribute('data-juneau-region-state')
		};
	});
});

kase('a12', async page => {
	await open(page, 'a12');
	await waitLines(page, 5000, 20000);
	const initial = await page.evaluate(PANE);
	const firsts = [initial.first];
	let maxShift = 0;
	await page.evaluate(() => { document.querySelector('.juneau-co-pane').scrollTop = 0; });
	await sleep(200);
	for (let i = 0; i < 10 && await page.$('button.juneau-co-earlier'); i++) {
		const anchor = firsts[firsts.length - 1];
		const top = n => {
			const p = document.querySelector('.juneau-co-pane');
			const r = p.querySelector('.juneau-co-line[data-n="' + n + '"]');
			return r.getBoundingClientRect().top - p.getBoundingClientRect().top;
		};
		const before = await page.evaluate(top, anchor);
		await page.click('button.juneau-co-earlier');
		await page.waitForFunction(a => {
			const r = document.querySelector('.juneau-co-pane .juneau-co-line');
			return r && Number(r.getAttribute('data-n')) < a;
		}, anchor, { timeout: 10000 });
		await sleep(200);
		const after = await page.evaluate(top, anchor);
		maxShift = Math.max(maxShift, Math.abs(after - before));
		firsts.push((await page.evaluate(PANE)).first);
	}
	const controlGone = await page.evaluate(() => !document.querySelector('.juneau-co-control'));
	return { initialCount: initial.count, firsts: firsts, maxShift: Math.round(maxShift), controlGone: controlGone };
});

kase('a13', async page => {
	await open(page, 'a13', { hash: '#L100' });
	await page.waitForFunction(() => !!document.getElementById('L100'), null, { timeout: 30000 });
	await sleep(300);
	return page.evaluate(() => {
		const el = document.getElementById('L100');
		const first = Number(document.querySelector('.juneau-co-pane .juneau-co-line').getAttribute('data-n'));
		return { found: true, target: el.classList.contains('juneau-co-target'), first: first };
	});
});

kase('a14', async page => {
	await open(page, 'a14');
	await waitLines(page, 1);
	return page.evaluate(() => {
		const line = document.querySelector('.juneau-co-line');
		const text = line.querySelector('.juneau-co-text').textContent;
		const spans = Array.from(line.querySelectorAll('.juneau-co-text span'));
		const red = spans.find(s => s.textContent === 'red');
		const green = spans.find(s => s.textContent === 'green');
		return {
			text: text, hasEsc: text.indexOf('\u001b') >= 0, hasReplacement: text.indexOf('\ufffd') >= 0,
			links: line.querySelectorAll('a[href]:not(.juneau-co-gutter)').length,
			redIsError: !!red && red.classList.contains('juneau-co-s-error'),
			greenIsSuccessBold: !!green && green.classList.contains('juneau-co-s-success') && green.classList.contains('juneau-co-b')
		};
	});
});

kase('a15', async page => {
	await open(page, 'a15');
	await waitLines(page, 10);
	const initial = await page.evaluate(() => document.querySelectorAll('.juneau-co-line').length);
	await ctl(page, 'a15', 'fileappend', 'n=3');
	await waitLines(page, 13);
	const afterAppend = await page.evaluate(() => document.querySelectorAll('.juneau-co-line').length);
	await ctl(page, 'a15', 'truncate');
	await page.waitForSelector('.jc-console-error [data-juneau-error]', { timeout: 10000 });
	const banner = await page.evaluate(() => {
		const item = document.querySelector('.jc-console-error [data-juneau-error]');
		return { code: item.getAttribute('data-juneau-error'), text: item.textContent };
	});
	return { initial: initial, afterAppend: afterAppend, code: banner.code, text: banner.text, gone: banner.text.indexOf(GONE_TEXT) >= 0 };
});

kase('a16', async page => {
	await open(page, 'a16');
	await waitLines(page, 30);
	await ctl(page, 'a16', 'image');
	await ctl(page, 'a16', 'append', 'n=1');
	await waitLines(page, 32);
	await page.waitForFunction(() => {
		const i = document.querySelector('img.juneau-co-image');
		return !!i && i.complete && i.naturalWidth > 0;
	}, null, { timeout: 10000 });
	await sleep(300);
	return page.evaluate(() => {
		const p = document.querySelector('.juneau-co-pane');
		const i = document.querySelector('img.juneau-co-image');
		return { loaded: true, atBottom: p.scrollTop + p.clientHeight >= p.scrollHeight - 2, imgHeight: i.getBoundingClientRect().height };
	});
});

kase('a17', async page => {
	await open(page, 'a17');
	await waitText(page, '.juneau-co-state', 'PENDING');
	await ctl(page, 'a17', 'start');
	await waitText(page, '.juneau-co-state', 'RUNNING');
	await sleep(1200);
	const s = await page.evaluate(() => ({
		lines: document.querySelectorAll('.juneau-co-line').length,
		elapsed: document.querySelector('.juneau-co-elapsed').textContent
	}));
	return { pending: true, running: true, lines: s.lines, elapsedFormat: /^Elapsed \d\d:\d\d:\d\d$/.test(s.elapsed) };
});

kase('a18', async page => {
	await open(page, 'a18');
	await waitLines(page, 10);
	const before = await page.evaluate(() => {
		const t = document.querySelector('.juneau-co-line[data-n="3"] .juneau-co-text');
		const range = document.createRange();
		range.selectNodeContents(t);
		const sel = window.getSelection();
		sel.removeAllRanges();
		sel.addRange(range);
		return sel.toString();
	});
	await ctl(page, 'a18', 'append', 'n=2');
	await waitLines(page, 12);
	await sleep(200);
	const after = await page.evaluate(() => window.getSelection().toString());
	return { before: before, after: after, grew: true };
});

kase('a19', async page => {
	const row = '.juneau-co-line[data-n="3"]';
	await open(page, 'a19');
	await waitLines(page, 2);
	await ctl(page, 'a19', 'open');
	await waitText(page, row, 'a19 filling');
	const node = await page.evaluateHandle(sel => document.querySelector(sel), row);
	const openClass = await page.evaluate(sel => document.querySelector(sel).classList.contains('juneau-co-open'), row);
	for (let i = 0; i < 3; i++)
		await ctl(page, 'a19', 'dot');
	await waitText(page, row, 'a19 filling...');
	await ctl(page, 'a19', 'closeline');
	await waitText(page, row, 'a19 filling... done');
	await page.waitForFunction(sel => !document.querySelector(sel).classList.contains('juneau-co-open'), row, { timeout: 10000 });
	const s = await page.evaluate(a => ({
		same: document.querySelector(a[0]) === a[1],
		rows: document.querySelectorAll('.juneau-co-line').length,
		dupes: document.querySelectorAll(a[0]).length
	}), [row, node]);
	return { openClass: openClass, sameNode: s.same, rows: s.rows, dupes: s.dupes };
});

kase('a20', async page => {
	const text = '.juneau-co-line[data-n="1"] .juneau-co-text';
	await open(page, 'a20');
	await ctl(page, 'a20', 'open', 'text=' + encodeURIComponent('Performing task x: '));
	await waitLines(page, 1);
	await ctl(page, 'a20', 'settail', 'text=' + encodeURIComponent('1 of 2 complete'));
	await waitText(page, text, 'Performing task x: 1 of 2 complete');
	await ctl(page, 'a20', 'settail', 'text=' + encodeURIComponent('2 of 2 complete'));
	await waitText(page, text, 'Performing task x: 2 of 2 complete');
	await ctl(page, 'a20', 'closeline', 'text=');
	await page.waitForFunction(() => !document.querySelector('.juneau-co-line[data-n="1"]').classList.contains('juneau-co-open'), null, { timeout: 10000 });
	const s = await page.evaluate(sel => ({
		text: document.querySelector(sel).textContent,
		rows: document.querySelectorAll('.juneau-co-line').length
	}), text);
	return { text: s.text, rows: s.rows };
});

function attachDiagnostics(page, log) {
	page.on('console', m => log.push('console.' + m.type() + ': ' + m.text()));
	page.on('pageerror', e => log.push('pageerror: ' + String(e)));
	page.on('requestfailed', r => log.push('requestfailed: ' + r.method() + ' ' + r.url() + ' - ' + (r.failure()?.errorText || '?')));
	page.on('response', r => {
		if (!r.ok()) log.push('response: ' + r.request().method() + ' ' + r.url() + ' -> ' + r.status());
	});
}

(async () => {
	const browser = await chromium.launch();
	const report = { cases: {}, pageErrors: {}, consoleErrors: {}, diagnostics: [] };
	try {
		const record = async name => {
			const page = await browser.newPage({ viewport: { width: 1200, height: 900 } });
			const pageErrors = [];
			const consoleErrors = [];
			page.on('pageerror', e => pageErrors.push(String(e)));
			page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
			attachDiagnostics(page, report.diagnostics);
			report.pageErrors[name] = pageErrors;
			report.consoleErrors[name] = consoleErrors;
			return page;
		};
		for (const [name, fn] of CASES) {
			const page = await record(name);
			try {
				report.cases[name] = await fn(page, record);
			} catch (error) {
				report.cases[name] = { harnessError: String(error?.stack || error) };
			}
			await page.close();
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report, null, 2) + '\n');
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
