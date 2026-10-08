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
 * ribbon-parity-browser.cjs - real-browser wiring check for ribbon parity (the shared ribbon corpus, wiring layer).
 * Real DataTables, real juneau-views.js / juneau-ribbon.js / juneau-search.js, real juneau-datatables.js.  Nothing
 * touches the network: the page and both data endpoints are answered by page.route at http://corpus.test/.
 *
 * Never runs in a default build.  Driven by RibbonParity_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  CORPUS_ROWS=<json array> node ribbon-parity-browser.cjs <fixture.html>
 *
 * Boots one server-mode table (corpus case jrm-dropped-only, persisted ON) and one client-mode table (corpus case
 * foundry-review-phase with a default member) and prints ONE JSON object to stdout describing what the wire and the
 * DOM actually did; every assertion lives in the Java test.
 */
'use strict';

const fs = require('fs');
const { chromium } = require('playwright');

const ROWS = JSON.parse(process.env.CORPUS_ROWS);

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function until(pred, what, timeoutMs) {
	const end = Date.now() + (timeoutMs || 10000);
	while (!pred()) {
		if (Date.now() > end) throw new Error('timed out waiting for ' + what);
		await sleep(50);
	}
}

const statusSearch = body => {
	const c = (body.columns || []).find(x => x.data === 'status');
	return c && c.search ? c.search.value : null;
};

(async function () {
	const fixture = process.argv[2];
	if (!fixture) { process.stderr.write('usage: node ribbon-parity-browser.cjs <fixture.html>\n'); process.exit(2); }
	const browser = await chromium.launch();
	const out = { posts: [], errors: [] };
	try {
		const page = await browser.newPage();
		page.on('console', m => { if (m.type() === 'error') out.errors.push(m.text()); });
		page.on('pageerror', e => out.errors.push(String(e)));

		await page.route('http://corpus.test/**', route => {
			const req = route.request();
			const path = new URL(req.url()).pathname;
			if (path === '/index.html')
				return route.fulfill({ contentType: 'text/html', body: fs.readFileSync(fixture, 'utf8') });
			if (path === '/server-rows') {
				const body = JSON.parse(req.postData() || '{}');
				out.posts.push(body);
				return route.fulfill({ contentType: 'application/json',
					body: JSON.stringify({ draw: body.draw, recordsTotal: 0, recordsFiltered: 0, data: [] }) });
			}
			if (path === '/client-rows')
				return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ROWS) });
			return route.fulfill({ status: 404, body: 'not found: ' + path });
		});

		await page.goto('http://corpus.test/index.html');
		await page.waitForFunction(() => window.__corpusReady === true, null, { timeout: 30000 });
		await page.waitForFunction(() => document.querySelectorAll('#cli tbody tr').length > 0, null, { timeout: 30000 });
		await until(() => out.posts.length > 0, 'the first server-mode request');

		const visible = () => page.evaluate(() =>
			Array.from(document.querySelectorAll('#cli tbody tr')).filter(tr => !tr.querySelector('.dt-empty')).length);

		out.serverFirstStatusSearch = statusSearch(out.posts[0]);
		out.clientInitialRows = await visible();

		await page.locator('[aria-label="All"]').first().click();
		await sleep(300);
		out.clientAllRows = await visible();

		await page.locator('[aria-label="Waiting"]').first().click();
		await sleep(300);
		out.clientWaitingRows = await visible();

		const before = out.posts.length;
		await page.locator('[aria-label="Dropped only"]').first().click();
		await until(() => out.posts.length > before, 'the server-mode request after toggling the option off');
		out.serverAfterToggleStatusSearch = statusSearch(out.posts[out.posts.length - 1]);
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(out));
})().catch(e => { process.stderr.write(String(e && e.stack || e) + '\n'); process.exit(1); });
