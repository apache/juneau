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
 * End to end: the page TerminalSmoke_BrowserTest serves, over the real endpoints and a real juneau_run --pty log.
 *
 *   node terminal-smoke.cjs <baseUrl>
 *
 * Prints one JSON report to stdout:
 *   {cases: {name: true | "failure"}, pageErrors: {name: [...]}, consoleErrors: {name: [...]}}
 */

const { chromium } = require('playwright');

const BASE = process.argv[2];
const ID = 'smoke';

function expect(cond, msg) {
	if (!cond)
		throw new Error(msg);
}

async function open(page) {
	await page.goto(BASE + 'page');
	await page.waitForFunction(function () {
		const b = document.querySelector('.juneau-term-badge');
		return b && b.textContent !== 'Connecting' && b.textContent !== 'Running';
	}, null, { timeout: 30000 });
}

const CASES = [
	['s01_badgeShowsTheExitCodeProgressAndColour', async function (page) {
		await open(page);
		const r = await page.evaluate(function () {
			const b = document.querySelector('.juneau-term-badge');
			const red = Array.from(document.querySelectorAll('.xterm-rows span')).find(function (s) { return s.textContent === 'red tail'; });
			const rows = Array.from(document.querySelectorAll('.xterm-rows > div')).map(function (d) { return d.textContent.replace(/\u00a0/g, ' ').replace(/\s+$/, ''); });
			return { badge: b.textContent, kind: b.className, red: red ? red.className : null, progress: rows.indexOf('progress 100%') >= 0 };
		});
		expect(r.badge === 'exit 3' && /juneau-term-fail/.test(r.kind), 'badge: ' + r.badge + ' ' + r.kind);
		expect(/\bxterm-fg-1\b/.test(r.red || ''), 'the last line is red and on screen while following: ' + r.red);
		expect(r.progress, 'the carriage return redrew the progress line in place');
	}],
	['s02_stepHashScrollsToItsBuildingLine', async function (page) {
		await open(page);
		const step = await page.evaluate(async function (id) {
			const res = await fetch('/juneau-run-view/' + id + '/events');
			const body = await res.json();
			return body.events.find(function (e) { return e.ev === 'step' && e.title === 'Demo Module A'; }) || null;
		}, ID);
		expect(step && step.rawOffset > 0, 'the events carry the step and its offset: ' + JSON.stringify(step));
		await page.evaluate(function (h) { window.location.hash = h; }, '#' + ID + '-O' + step.rawOffset);
		await page.waitForFunction(function () {
			const r = document.querySelector('.xterm-rows > div');
			return r && r.textContent.indexOf('Building Demo Module A') >= 0;
		}, null, { timeout: 5000 });
		expect(!(await page.isChecked('.juneau-term-follow input')), 'a step jump turns follow off');
	}],
	['s03_rawIsTheWholeLogAsAnAttachment', async function (page) {
		await open(page);
		const r = await page.evaluate(async function () {
			const href = document.querySelector('.juneau-term-raw').getAttribute('href');
			const raw = await fetch(href);
			const bytes = await fetch(href.replace(/\/raw$/, '/bytes'));
			return {
				href: href, status: raw.status, disposition: raw.headers.get('Content-Disposition'),
				length: (await raw.arrayBuffer()).byteLength, end: bytes.headers.get('Term-End'), done: bytes.headers.get('Term-Done')
			};
		});
		expect(r.href === new URL('/juneau-terminal/smoke/raw', BASE).href && r.status === 200, 'raw link: ' + JSON.stringify(r));
		expect(/^attachment;/.test(r.disposition || ''), 'an attachment: ' + r.disposition);
		expect(String(r.length) === r.end && r.done === 'true', 'the whole finished log: ' + JSON.stringify(r));
	}]
];

(async function main() {
	const report = { cases: {}, pageErrors: {}, consoleErrors: {} };
	const browser = await chromium.launch();
	try {
		for (const [name, fn] of CASES) {
			const ctx = await browser.newContext({ viewport: { width: 1200, height: 900 } });
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
