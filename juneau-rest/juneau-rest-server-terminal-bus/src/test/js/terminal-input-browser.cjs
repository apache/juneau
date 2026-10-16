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
 * End to end for terminal input: the page TerminalInput_BrowserTest serves, over the real bus WebSocket and TerminalInput.
 *
 *   node terminal-input-browser.cjs <baseUrl>
 *
 * Prints one JSON report to stdout:
 *   {cases: {name: true | "failure"}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}}
 */

const { chromium } = require('playwright');

const BASE = process.argv[2];

function expect(cond, msg) {
	if (!cond)
		throw new Error(msg);
}

/** Opens the page for one job; typist says whether the bus session may type, input whether the card asks for input. */
async function open(page, id, typist, input) {
	await page.goto(BASE + 'page?id=' + id + '&typist=' + (typist ? 1 : 0) + '&input=' + (input ? 1 : 0));
	await page.waitForSelector('.xterm-rows');
}

function rows(page) {
	return page.evaluate(function () {
		return Array.from(document.querySelectorAll('.xterm-rows > div')).map(function (d) {
			return d.textContent.replace(/\u00a0/g, ' ').replace(/\s+$/, '');
		});
	});
}

async function waitRow(page, text, timeout) {
	await page.waitForFunction(function (t) {
		return Array.from(document.querySelectorAll('.xterm-rows > div')).some(function (d) { return d.textContent.replace(/\u00a0/g, ' ').indexOf(t) >= 0; });
	}, text, { timeout: timeout || 15000 });
}

async function arm(page) {
	await page.waitForFunction(function () {
		const b = document.querySelector('.juneau-term-input');
		return b && !b.disabled;
	}, null, { timeout: 15000 });
	// Wait for the bridge to open, then press the toggle once.
	await page.waitForFunction(function () {
		const s = window.JuneauViews.bus.get('bridge:term');
		return s && s.state === 'open';
	}, null, { timeout: 15000 });
	await page.click('.juneau-term-input');
	await page.waitForFunction(function () {
		const c = document.querySelector('.juneau-term-chip--input');
		return c && !c.hidden && c.textContent === 'Input live';
	}, null, { timeout: 15000 });
}

const CASES = [
	['i01_typedAnswerReachesTheJobAndItsOutputShows', async function (page) {
		await open(page, 't-prompt', true, true);
		await waitRow(page, 'Continue? [y/N]');
		await arm(page);
		await page.keyboard.type('y');
		await page.keyboard.press('Enter');
		await waitRow(page, 'got y');
		await page.waitForFunction(function () {
			const b = document.querySelector('.juneau-term-badge');
			return b && b.textContent === 'Done' && /juneau-term-ok/.test(b.className);
		}, null, { timeout: 15000 });
	}],
	['i02_ctrlCReachesTheJobAndTheBadgeFails', async function (page) {
		await open(page, 't-sleep', true, true);
		await arm(page);
		await page.keyboard.press('Control+C');
		await page.waitForFunction(function () {
			const b = document.querySelector('.juneau-term-badge');
			return b && /^exit /.test(b.textContent) && /juneau-term-fail/.test(b.className);
		}, null, { timeout: 5000 });
	}],
	['i03_noToggleWhenTheCardHasNoInput', async function (page) {
		await open(page, 't-idle', true, false);
		await waitRow(page, 'idle');
		const n = await page.evaluate(function () {
			return { toggle: document.querySelectorAll('.juneau-term-input').length, chip: document.querySelectorAll('.juneau-term-chip--input').length };
		});
		expect(n.toggle === 0 && n.chip === 0, 'no input controls: ' + JSON.stringify(n));
	}],
	['i04_deniedKeepsTheTerminalUpdating', async function (page) {
		await open(page, 't-tick', false, true);
		await page.waitForFunction(function () {
			return Array.from(document.querySelectorAll('.xterm-rows > div')).some(function (d) { return /^tick \d+/.test(d.textContent.replace(/\u00a0/g, ' ')); });
		}, null, { timeout: 15000 });
		await page.waitForFunction(function () { return window.JuneauViews.bus.get('bridge:term') && window.JuneauViews.bus.get('bridge:term').state === 'open'; }, null, { timeout: 15000 });
		await page.click('.juneau-term-input');
		await page.waitForFunction(function () {
			const c = document.querySelector('.juneau-term-chip--input');
			const b = document.querySelector('.juneau-term-input');
			return c && !c.hidden && c.textContent === 'Input not permitted' && b.disabled && b.textContent === 'Input not permitted';
		}, null, { timeout: 15000 });
		const last = function (r) {
			return Math.max.apply(null, [0].concat(r.map(function (l) { const m = /^tick (\d+)/.exec(l); return m ? Number(m[1]) : 0; })));
		};
		const before = last(await rows(page));
		await page.waitForFunction(function (n) {
			return Array.from(document.querySelectorAll('.xterm-rows > div')).some(function (d) {
				const m = /^tick (\d+)/.exec(d.textContent.replace(/\u00a0/g, ' '));
				return m && Number(m[1]) > n + 1;
			});
		}, before, { timeout: 10000 });
	}]
];

(async function main() {
	const report = { cases: {}, pageErrors: {}, consoleErrors: {} };
	const browser = await chromium.launch();
	try {
		for (const [name, fn] of CASES) {
			const ctx = await browser.newContext({ viewport: { width: 1200, height: 900 } });
			const page = await ctx.newPage();
			const pageErrors = [], consoleErrors = [], consoleAll = [];
			page.on('pageerror', function (e) { pageErrors.push(String(e && e.stack || e)); });
			page.on('console', function (m) { consoleAll.push(m.type() + ': ' + m.text()); if (m.type() === 'error') consoleErrors.push(m.text()); });
			try {
				await fn(page);
				report.cases[name] = true;
			} catch (e) {
				let diag = '';
				try {
					diag = await page.evaluate(function () {
						const b = window.JuneauViews.bus;
						return JSON.stringify({ bridge: b.get('bridge:term'), history: b.history().slice(-3), rows: Array.from(document.querySelectorAll('.xterm-rows > div')).map(function (d) { return d.textContent; }).filter(Boolean).slice(-6) });
					});
				} catch (x) { /* the page may be gone; the failure above is the useful part */ }
				report.cases[name] = String(e && e.stack || e) + '\nbus: ' + diag + '\nconsole: ' + consoleAll.join(' | ');
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
