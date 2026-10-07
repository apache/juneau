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
 * Petstore browser harness: node petstore-browser.cjs <cases.json>
 *
 * Input:  {baseUrl, snapshots, cases:[{name, path, actions?, queries?, fetches?, capture?, allowFailedLoads?}]}
 * Output (stdout): {cases:{<name>:{status, jsFailures, deprecations, queries, fetches, requests, timeouts}}}
 *
 * Each action is one of:
 *   {click: selector} {fill: [selector, text]} {press: [selector, key]}
 *   {waitFor: jsExpression, timeoutMs?}  (records the expression in timeouts if it never becomes truthy)
 *   {waitForCaptured: n, timeoutMs?}  (waits until n requests matching the case's capture were recorded)
 *   {evaluate: jsExpression}  {screenshot: fileName}
 *
 * allowFailedLoads drops Chromium's own "Failed to load resource" console errors, which a deliberate 401/404
 * fetch produces; the case then asserts the status itself.  Script errors are still recorded.
 */
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

(async () => {
	const input = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
	const browser = await chromium.launch();
	const out = {};
	try {
		for (const c of input.cases) {
			out[c.name] = await runCase(browser, input, c);
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify({ cases: out }));
})().catch(e => {
	process.stderr.write(String(e?.stack || e));
	process.exit(1);
});

async function runCase(browser, input, c) {
	const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
	const page = await context.newPage();
	const r = { status: 0, jsFailures: [], deprecations: [], queries: {}, fetches: {}, requests: [], timeouts: [] };
	page.on('pageerror', e => r.jsFailures.push(String(e)));
	page.on('console', m => {
		const t = m.text();
		if (m.type() === 'error') {
			if (!(c.allowFailedLoads && /Failed to load resource/.test(t))) r.jsFailures.push(t);
		}
		else if (m.type() === 'warning' && /is deprecated/.test(t)) r.deprecations.push(t);
	});
	if (c.capture) {
		page.on('request', q => {
			if (q.url().includes(c.capture)) r.requests.push({ url: q.url(), method: q.method(), postData: q.postData() });
		});
	}
	try {
		const resp = await page.goto(input.baseUrl + c.path, { waitUntil: 'networkidle' });
		r.status = resp ? resp.status() : 0;
		for (const a of c.actions || []) await act(page, input, c, a, r);
		for (const [k, expr] of Object.entries(c.queries || {})) {
			r.queries[k] = await page.evaluate(expr);
		}
		for (const [k, url] of Object.entries(c.fetches || {})) {
			r.fetches[k] = await page.evaluate(async u => {
				const x = await fetch(u, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
				return { status: x.status, body: await x.text() };
			}, url);
		}
	} catch (e) {
		r.jsFailures.push('harness: ' + String(e?.message || e));
	} finally {
		await context.close();
	}
	return r;
}

async function act(page, input, c, a, r) {
	if (a.click) return page.click(a.click);
	if (a.fill) return page.fill(a.fill[0], a.fill[1]);
	if (a.press) return page.press(a.press[0], a.press[1]);
	if (a.evaluate) return page.evaluate(a.evaluate);
	if (a.screenshot) return page.screenshot({ path: path.join(input.snapshots, a.screenshot), fullPage: true });
	if (a.waitFor) {
		try {
			await page.waitForFunction(a.waitFor, null, { timeout: a.timeoutMs || 10000 });
		} catch { // NOSONAR javascript:S2486 -- a timeout is the expected failure here; it is recorded in r.timeouts
			r.timeouts.push(a.waitFor);
		}
		return;
	}
	if (a.waitForCaptured) {
		const deadline = Date.now() + (a.timeoutMs || 10000);
		while (r.requests.length < a.waitForCaptured && Date.now() < deadline) await page.waitForTimeout(100);
		if (r.requests.length < a.waitForCaptured) r.timeouts.push('captured ' + a.waitForCaptured + ' of ' + c.capture);
		return;
	}
	throw new Error('Unknown action ' + JSON.stringify(a));
}
