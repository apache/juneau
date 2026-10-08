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
 * run-view-browser.cjs - real-browser prober for the run-view region.  One Playwright page per case, each against
 * its own run log on the RunView_BrowserTest server.  Writes one JSON report to stdout:
 *   { cases: {name: {...}}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}, diagnostics: [...] }
 *
 * Never runs in a default build.  Driven by RunView_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node run-view-browser.cjs <baseUrl>
 */
'use strict';

const { chromium } = require('playwright');

const THEMES = ['gray', 'light-brown', 'light-red', 'open', 'red'];

const [baseUrl] = process.argv.slice(2);
if (!baseUrl) {
	process.stderr.write('usage: node run-view-browser.cjs <baseUrl>\n');
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

async function open(page, c, opts) {
	const o = opts || {};
	await page.goto(baseUrl + 'page/' + c + '?theme=' + (o.theme || 'gray'));
	await page.waitForFunction(() => !!(window.JuneauViews && window.JuneauViews.runView), null, { timeout: 15000 });
}

async function waitText(page, selector, text, timeout) {
	await page.waitForFunction(a => {
		const e = document.querySelector(a[0]);
		return !!e && e.textContent.indexOf(a[1]) >= 0;
	}, [selector, text], { timeout: timeout || 10000 });
}

const HEAD = '.juneau-rv-headline';

/* The headline text without its leading status glyph. */
function headline(page) {
	return page.$eval(HEAD, e => e.textContent.trim().replace(/^\S+\s+/, ''));
}

/* The contrast of the headline, the step glyphs and the counts line against their first opaque ancestor background. */
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
	function ratio(el) {
		const fg = rgb(getComputedStyle(el).color);
		let op = fg.a;
		for (let e = el; e; e = e.parentElement) op *= Number(getComputedStyle(e).opacity);
		const bg = background(el);
		const mix = { r: fg.r * op + bg.r * (1 - op), g: fg.g * op + bg.g * (1 - op), b: fg.b * op + bg.b * (1 - op) };
		const l1 = lum(mix);
		const l2 = lum(bg);
		return Math.round(((Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)) * 100) / 100;
	}
	const out = { headline: ratio(document.querySelector('.juneau-rv-headline')), counts: ratio(document.querySelector('.juneau-rv-counts')) };
	Array.from(document.querySelectorAll('.juneau-rv-glyph')).forEach(function (g) {
		const k = Array.from(g.classList).filter(c => c.indexOf('juneau-rv-state-') === 0)[0] || 'glyph';
		out[k] = ratio(g);
	});
	return out;
};

const CASES = [];
function kase(name, fn) { CASES.push([name, fn]); }

kase('a01', async page => {
	await open(page, 'a01');
	await waitText(page, HEAD, 'Running');
	const running = await headline(page);
	await ctl(page, 'a01', 'script');
	await waitText(page, HEAD, 'Failed at');
	return {
		running: running,
		final: await headline(page),
		status: await page.getAttribute('.juneau-rv', 'data-juneau-rv-status'),
		counts: (await page.textContent('.juneau-rv-counts')).trim(),
		failures: await page.locator('button.juneau-rv-failure').count()
	};
});

kase('a02', async page => {
	await open(page, 'a02');
	await page.waitForSelector('.juneau-co-block');
	const block = page.locator('.juneau-co-block-empty, .juneau-co-fill-error').first();
	await block.hover();
	await page.waitForFunction(() => { const t = document.querySelector('.juneau-co-tooltip'); return !!t && !t.hidden && t.textContent.length > 0; }, null, { timeout: 5000 });
	const text = await page.textContent('.juneau-co-tooltip');
	const role = await page.getAttribute('.juneau-co-tooltip', 'role');
	await block.focus();
	await page.keyboard.press('Escape');
	await sleep(100);
	return { tipText: text, role: role, hiddenAfterEscape: await page.evaluate(() => document.querySelector('.juneau-co-tooltip').hidden) };
});

kase('a03', async page => {
	await open(page, 'a03');
	await page.waitForSelector('a.juneau-co-block[href]');
	await page.locator('a.juneau-co-block[href]').first().click();
	await sleep(200);
	return { hash: await page.evaluate(() => location.hash) };
});

kase('a04', async page => {
	await open(page, 'a04');
	await waitText(page, HEAD, 'Failed at');
	// Each view reads the run on its own, so the second can still be catching up when the first settles; wait for the
	// two to agree instead of comparing at the moment the first one does.
	await page.waitForFunction(() => {
		const f = document.querySelector('.juneau-rv:not(.juneau-rv-compact) .juneau-rv-headline');
		const c = document.querySelector('.juneau-rv.juneau-rv-compact .juneau-rv-headline');
		return !!f && !!c && f.textContent.indexOf('Failed at') >= 0 && f.textContent === c.textContent;
	}, null, { timeout: 10000 }).catch(() => {});
	return page.evaluate(() => {
		const full = document.querySelector('.juneau-rv:not(.juneau-rv-compact)');
		const compact = document.querySelector('.juneau-rv.juneau-rv-compact');
		return {
			bothMounted: !!full && !!compact,
			sameHeadline: full.querySelector('.juneau-rv-headline').textContent === compact.querySelector('.juneau-rv-headline').textContent,
			fullFailures: full.querySelectorAll('button.juneau-rv-failure').length,
			compactFailures: compact.querySelectorAll('button.juneau-rv-failure').length
		};
	});
});

kase('a05', async page => {
	await open(page, 'a05');
	const requests = [];
	page.on('request', r => { if (r.url().indexOf('/events') >= 0) requests.push(r.url()); });
	await page.evaluate(() => {
		const rv = window.JuneauViews.runView.of('a05');
		rv.append([{ ev: 'step', id: 's1', title: 'Pushed' }, { ev: 'end', id: 's1', status: 'fail' }, { ev: 'done', status: 'fail' }]);
	});
	await waitText(page, HEAD, 'Failed at Pushed');
	await sleep(1500);
	return { headline: await headline(page), eventsRequests: requests.length };
});

kase('a06', async page => {
	await open(page, 'a06');
	await waitText(page, HEAD, 'Running');
	await ctl(page, 'a06', 'reset');
	await ctl(page, 'a06', 'step', 'id=fresh&title=Fresh');
	await waitText(page, '.juneau-rv-steps', 'Fresh', 15000);
	return {
		hasOldStep: (await page.textContent('.juneau-rv-steps')).indexOf('Before reset') >= 0,
		banner: await page.locator('[role=alert]').count()
	};
});

kase('a07', async page => {
	const themes = {};
	for (const theme of THEMES) {
		await page.goto(baseUrl + 'page/a07?theme=' + theme);
		await page.waitForFunction(() => !!(window.JuneauViews && window.JuneauViews.runView), null, { timeout: 15000 });
		await waitText(page, HEAD, 'Failed at');
		themes[theme] = await page.evaluate(CONTRAST);
	}
	return { themes: themes };
});

kase('a08', async page => {
	await open(page, 'a08');
	await waitText(page, '.juneau-rv-counts', '12000', 30000);
	const blocks = await page.locator('.juneau-co-block').count();
	return { counts: (await page.textContent('.juneau-rv-counts')).trim(), blocks: blocks };
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
		for (const [name, fn] of CASES) {
			const page = await browser.newPage({ viewport: { width: 1200, height: 900 } });
			const pageErrors = [];
			const consoleErrors = [];
			page.on('pageerror', e => pageErrors.push(String(e)));
			page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
			attachDiagnostics(page, report.diagnostics);
			report.pageErrors[name] = pageErrors;
			report.consoleErrors[name] = consoleErrors;
			try {
				report.cases[name] = await fn(page);
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
