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
 * badges.html.cjs - real-browser prober for the count badge: label and visibility in the header, the hover popover,
 * click navigation (to a same-page hash, since location.assign is not stubbable in a real browser), and removal on 403.
 *
 * Never runs in a default build; it is driven by PendingBadge_BrowserTest, which only runs under `mvn -Pjs-tests`.
 *
 *   Usage:  node badges.html.cjs <page.html>
 *
 * This script only OBSERVES and prints ONE JSON object; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauConsoleBadges;
	const out = { hasMount: typeof NS?.mount === 'function' };
	if (!out.hasMount) return out;
	const bodies = {
		'/badges/pending': { status: 200, body: { total: 3, mine: 1, items: [{ id: '1', label: 'Rule one' }, { id: '2', label: 'Rule two' }] } },
		'/badges/denied': { status: 403, body: {} }
	};
	window.fetch = function (url) {
		const r = bodies[String(url).split('?')[0]] || { status: 404, body: {} };
		return Promise.resolve({ status: r.status, json: function () { return Promise.resolve(r.body); } });
	};
	NS.mount([
		{ id: 'pending', src: '/badges/pending', href: '#changes', refreshMs: 60000 },
		{ id: 'denied', src: '/badges/denied' }
	]);
	await new Promise(function (r) { setTimeout(r, 100); });
	const b = document.querySelector('[data-juneau-badge-id=pending]');
	if (b) {
		const r = b.getBoundingClientRect();
		out.label = b.textContent;
		out.visible = r.width > 0 && r.height > 0;
		out.hostClass = b.parentNode.className;
		b.dispatchEvent(new MouseEvent('mouseenter'));
		const pop = document.querySelector('.jc-count-badge-popover');
		out.popoverItems = pop ? pop.querySelectorAll('.jc-count-badge-popover-item').length : -1;
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
		out.popoverClosedOnEscape = document.querySelector('.jc-count-badge-popover') === null;
		b.click();
		out.hash = location.hash;
	}
	out.deniedRemoved = document.querySelector('[data-juneau-badge-id=denied]') === null;
	NS.unmountAll();
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node badges.html.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		await page.goto('file://' + path.resolve(fixture));
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
