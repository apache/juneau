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
 * console-shell.cjs - opt-in Chromium prober for the console shell (juneau-console.js).
 *
 * Never runs in a default build.  Driven by the Console*_BrowserTest classes under `mvn -Pjs-tests`.
 *
 *   Usage:  node console-shell.cjs <cases.json>
 *
 * cases.json is {assets: {path: {type, body}}, cases: [{name, url, pages: {path: {type, body}}, queries, computed}]}.
 * Every request to http://juneau.test/ is answered from a case's pages, then from the shared assets, else 404.
 * Prints one report object per case name.
 */
'use strict';

const fs = require('node:fs');
const { chromium } = require('playwright');

const ORIGIN = /^http:\/\/juneau\.test\//;

// Must equal the Task 0 baseline probe (nav-computed-baseline.json).
const SELECTORS = ['nav.juneau-page-nav', '.juneau-page-nav-sections', '.juneau-page-nav-children',
	'.juneau-page-nav-section[aria-current]', '.juneau-page-nav-section:not([aria-current])',
	'.juneau-page-nav-child[aria-current]', '.juneau-page-nav-child:not([aria-current])'];
const PROPS = ['display', 'flex-direction', 'gap', 'padding-top', 'padding-right', 'padding-bottom', 'padding-left',
	'margin-top', 'margin-bottom', 'border-bottom-width', 'border-bottom-style', 'border-bottom-color',
	'color', 'background-color', 'font-size', 'font-weight', 'line-height', 'text-decoration-line', 'position', 'top'];

const PROBE = function (arg) {
	const out = {};
	const banner = document.querySelector('.jc-console-error');
	out.banner = banner
		? Array.prototype.map.call(banner.querySelectorAll('.jc-console-error-item'),
			function (i) { return { code: i.dataset.juneauError ?? null, text: i.textContent }; })
		: [];
	out.current = Array.prototype.map.call(document.querySelectorAll('nav.juneau-page-nav [aria-current="page"]'),
		function (a) { return a.dataset.juneauNavId || a.getAttribute('href'); });
	const navEl = document.querySelector('nav.juneau-page-nav');
	out.navLabel = navEl ? navEl.getAttribute('aria-label') : null;
	out.childRows = document.querySelectorAll('nav.juneau-page-nav .juneau-page-nav-children').length;
	out.probe = window.__probe === undefined ? null : JSON.parse(JSON.stringify(window.__probe)); // NOSONAR javascript:S7784 -- deliberate JSON round-trip: drops non-serialisable members (functions/undefined) that structuredClone would throw on
	out.mounted = window.__mounted || null;
	out.queries = {};
	for (const k of Object.keys(arg.queries)) {
		const el = document.querySelector(arg.queries[k]);
		out.queries[k] = el ? el.textContent : null;
	}
	if (arg.computed) {
		out.computed = {};
		for (const s of arg.selectors) {
			const el = document.querySelector(s);
			if (!el) {
				out.computed[s] = null;
				continue;
			}
			const cs = getComputedStyle(el);
			out.computed[s] = {};
			for (const p of arg.props) out.computed[s][p] = cs.getPropertyValue(p);
		}
	}
	return out;
};

(async () => {
	const [casesFile] = process.argv.slice(2);
	if (!casesFile) {
		process.stderr.write('usage: node console-shell.cjs <cases.json>\n');
		process.exit(2);
	}
	if (!fs.existsSync(casesFile))
		throw new Error('cases file not found: ' + casesFile);
	const spec = JSON.parse(fs.readFileSync(casesFile, 'utf8'));

	const browser = await chromium.launch();
	const report = {};
	try {
		for (const c of spec.cases) {
			const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });
			await context.addInitScript(() => {
				document.addEventListener('juneau:console-mounted', e => {
					window.__mounted = {
						activeNav: e.detail.activeNav,
						activeNavSource: e.detail.activeNavSource,
						cards: Object.keys(e.detail.cards)
					};
				});
			});
			await context.route(ORIGIN, route => {
				const p = new URL(route.request().url()).pathname;
				const f = (c.pages?.[p]) || spec.assets[p];
				return f
					? route.fulfill({ status: 200, contentType: f.type, body: f.body })
					: route.fulfill({ status: 404, contentType: 'text/plain', body: 'not found: ' + p });
			});
			const page = await context.newPage();
			const consoleErrors = [];
			const pageErrors = [];
			page.on('pageerror', e => pageErrors.push(String(e?.message || e)));
			page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
			await page.goto(c.url);
			await page.evaluate(() => new Promise(requestAnimationFrame));
			const r = await page.evaluate(PROBE, { queries: c.queries || {}, computed: !!c.computed, selectors: SELECTORS, props: PROPS });
			r.consoleErrors = consoleErrors.slice();
			r.pageErrors = pageErrors.slice();
			report[c.name] = r;
			await context.close();
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report, null, 2) + '\n');
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
