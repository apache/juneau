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
 * Input:  {baseUrl, snapshots, deviceScaleFactor?, cases:[{name, path, viewport?, deviceScaleFactor?, waitUntil?,
 *          initScript?, actions?, queries?, fetches?, capture?, allowFailedLoads?}]}
 * Output (stdout): {cases:{<name>:{status, jsFailures, deprecations, queries, fetches, requests, downloads, timeouts}}}
 *
 * Each action is one of:
 *   {click: selector} {fill: [selector, text]} {press: [selector, key]}
 *   {waitFor: jsExpression, timeoutMs?}  (records the expression in timeouts if it never becomes truthy)
 *   {waitForCaptured: n, timeoutMs?}  (waits until n requests matching the case's capture were recorded)
 *   {evaluate: jsExpression}  {screenshot: fileName, selector?}  (the whole page, or only the first element matching
 *     selector; written to the input's snapshots directory)
 *   {reload: true}  (reloads the page in the same context, so IndexedDB and the captured requests carry over; a
 *     reload through evaluate would destroy the execution context mid-call)
 *   {back: true} {forward: true}  (the browser's own back and forward buttons)
 *   {download: selector, as?, timeoutMs?}  (clicks and records the download as downloads[as]: {name, bytes, head}, head being its first 20000 characters)
 *   {goto: path}  {follow: jsExpression}  (follow navigates to the URL the expression returns; both wait like reload)
 *   {drag: [selector, dx]}  (presses the mouse at the element's centre, moves dx pixels right, and releases)
 * reload waits for the case's waitUntil, as the first load does.  back and forward wait only for the navigation to
 * commit: follow them with a waitFor on what the page should then show.
 *
 * waitUntil is the navigation's Playwright wait state, networkidle by default.  A case on a page that polls can pass
 * 'load' and start its actions with a waitFor, so it never depends on the poll leaving a quiet gap.
 *
 * viewport is [width, height] (default [1280, 900]).  deviceScaleFactor (case, else input, else 1) is the device pixel
 * ratio, so 2 gives screenshots at twice the viewport's pixel size.  Screenshots are taken with animations disabled
 * and the caret hidden.  initScript is JS source run at the start of every document the
 * case opens, reloads included.
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
	const vp = Array.isArray(c.viewport) ? c.viewport : [1280, 900];
	const context = await browser.newContext({ viewport: { width: vp[0], height: vp[1] }, acceptDownloads: true,
		deviceScaleFactor: c.deviceScaleFactor || input.deviceScaleFactor || 1 });
	if (c.initScript) await context.addInitScript(c.initScript);
	const page = await context.newPage();
	const r = { status: 0, jsFailures: [], deprecations: [], queries: {}, fetches: {}, requests: [], downloads: {}, timeouts: [] };
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
		const resp = await page.goto(input.baseUrl + c.path, { waitUntil: c.waitUntil || 'networkidle' });
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

async function act(page, input, c, a, r) { // NOSONAR javascript:S3776 - one flat dispatch over the action kinds
	if (a.click) return page.click(a.click);
	if (a.fill) return page.fill(a.fill[0], a.fill[1]);
	if (a.press) return page.press(a.press[0], a.press[1]);
	const until = { waitUntil: c.waitUntil || 'networkidle' };
	if (a.reload) return page.reload(until);
	if (a.goto) return page.goto(input.baseUrl + a.goto, until);
	if (a.follow) return page.goto(String(await page.evaluate(a.follow)), until);
	if (a.drag) {
		const box = await page.locator(a.drag[0]).boundingBox();
		if (!box) throw new Error('drag: ' + a.drag[0] + ' is not visible');
		const x = box.x + box.width / 2, y = box.y + box.height / 2;
		await page.mouse.move(x, y);
		await page.mouse.down();
		await page.mouse.move(x + a.drag[1], y, { steps: 4 });
		return page.mouse.up();
	}
	if (a.back) return page.goBack({ waitUntil: 'commit' });
	if (a.forward) return page.goForward({ waitUntil: 'commit' });
	if (a.evaluate) return page.evaluate(a.evaluate);
	if (a.screenshot) {
		const shot = { path: path.join(input.snapshots, a.screenshot), animations: 'disabled', caret: 'hide' };
		if (a.selector) return page.locator(a.selector).first().screenshot(shot);
		return page.screenshot({ ...shot, fullPage: true });
	}
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
	if (a.download) {
		const [dl] = await Promise.all([page.waitForEvent('download', { timeout: a.timeoutMs || 10000 }), page.click(a.download)]);
		const file = await dl.path();
		r.downloads[a.as || 'download'] = { name: dl.suggestedFilename(), bytes: fs.statSync(file).size,
			head: fs.readFileSync(file, 'utf8').slice(0, 20000) };
		return;
	}
	throw new Error('Unknown action ' + JSON.stringify(a));
}
