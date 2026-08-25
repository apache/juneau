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
 * row-actions.cjs - real-browser prober for the juneau-views.js row-action + fail-closed CSRF contract (row-actions-415).
 *
 * Never runs in a default build.  It is driven by RowActionCsrf_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node row-actions.cjs <page.html>
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL served juneau-views.js - in
 * headless Chromium, then, entirely inside the page, exercises the runtime's row-action pipeline and its
 * fail-closed CSRF submit via the real DOM and a stubbed window.fetch.  Prints ONE JSON object to stdout.
 *
 * DIVISION OF LABOUR (mirrors panel-visibility.cjs): this script only OBSERVES; every assertion lives in the Java
 * test.  It reports facts (was fetch issued, with what method/headers/body; did a visible refusal banner appear)
 * and lets JUnit decide pass/fail, so the expectations live next to the runtime they constrain.
 *
 * WHY A REAL BROWSER: the fail-closed refusal is a user-visible outcome (a rendered banner and a NOT-sent request);
 * measuring "a request was suppressed and a banner became visible" is exactly what a browser, and not a substring
 * assertion, can show.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/*
 * Runs inside the page.  window.JuneauViews.init is populated by the real juneau-views.js the fixture loaded.
 * Drives the pure request builder AND the DOM menu/submit path against a stubbed fetch, returning a plain report.
 */
const PROBE = function () {
	const NS = window.JuneauViews;
	const init = NS?.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	out.contractVersion = NS.CONTRACT_VERSION;
	out.defaultCsrfHeader = init.DEFAULT_CSRF_HEADER;

	// Pure request builder: whitespace, absent, safe-method all refuse; a valid token yields a JSON request.
	const action = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST' };
	out.reqBlankWhitespace = init.buildActionRequest(action, '   ', null);
	out.reqAbsent = init.buildActionRequest(action, null, null);
	out.reqSafeMethod = init.buildActionRequest({ id: 'g', endpoint: '/x', method: 'GET' }, 'tok', null);
	out.reqValid = init.buildActionRequest(action, 'tok-123', null);

	// Behavioural DOM path: build a table+row carrying a token attribute, open the menu, click the item, and
	// record whether fetch was issued and whether a visible refusal banner rendered.
	const fetchCalls = [];
	const realFetch = window.fetch;
	window.fetch = function (url, opts) {
		fetchCalls.push({ url: url, opts: opts });
		return Promise.resolve({ ok: true });   // 415 does not act on the body; a resolved ok is enough here
	};

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function makeTable(tokenValue) {
		const table = document.createElement('table');
		table.dataset.juneauView = 'v';
		if (tokenValue != null) table.dataset.juneauCsrf = tokenValue;
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, tr: tr, td: td };
	}

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function rendered(el) {
		if (!el) return false;
		const r = el.getBoundingClientRect();
		return r.width > 0 && r.height > 0;
	}

	// Case 1: BLANK (whitespace) token -> visible refusal, NO fetch.
	const before1 = fetchCalls.length;
	const dom1 = makeTable('   ');
	const menu1 = init.buildRowActionMenu({ rowActions: [action] }, dom1.table, dom1.tr, {});
	dom1.td.appendChild(menu1);
	menu1.querySelector('.juneau-view-action-item').click();
	out.blankTokenFetchIssued = fetchCalls.length > before1;
	const banner1 = dom1.td.querySelector('.juneau-view-action-refusal');
	out.blankTokenRefusalVisible = rendered(banner1);
	out.blankTokenRefusalText = banner1?.textContent ?? null;

	// Case 2: VALID token -> fetch issued with POST + JSON content type + the CSRF header, NO refusal banner.
	const before2 = fetchCalls.length;
	const dom2 = makeTable('tok-123');
	const menu2 = init.buildRowActionMenu({ rowActions: [action] }, dom2.table, dom2.tr, {});
	dom2.td.appendChild(menu2);
	menu2.querySelector('.juneau-view-action-item').click();
	const call = fetchCalls.at(-1);
	out.validTokenFetchIssued = fetchCalls.length > before2;
	if (out.validTokenFetchIssued) {
		out.validTokenUrl = call.url;
		out.validTokenMethod = call.opts.method;
		out.validTokenContentType = call.opts.headers['Content-Type'];
		out.validTokenCsrfHeader = call.opts.headers[init.DEFAULT_CSRF_HEADER];
		out.validTokenBody = call.opts.body;
	}
	out.validTokenRefusalVisible = rendered(dom2.td.querySelector('.juneau-view-action-refusal'));

	window.fetch = realFetch;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node row-actions.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
