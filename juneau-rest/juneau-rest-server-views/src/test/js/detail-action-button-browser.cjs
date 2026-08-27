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
 * detail-action-button-browser.cjs - opt-in Chromium layout canary for the Detail View action button treatment.
 *
 * Never runs in a default build.  Driven by RowDetail_ActionButton_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node detail-action-button-browser.cjs <page.html>
 *
 * What this measures is unreachable from a fake DOM: computed colour resolution through the CSS cascade, and
 * the difference between an enabled and a disabled solid-fill button.  The fixture's buttons are emitted
 * `disabled` exactly as the server emits them (real ActionRef markup, never faked), so the disabled reading
 * comes first and the enabled reading is taken only after the harness clears the attribute itself.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const READ_DISABLED = function () {
	const primary = document.querySelector('[data-juneau-action="ack"]');
	const secondary = document.querySelector('[data-juneau-action="esc"]');
	return {
		primaryDisabledBg: window.getComputedStyle(primary).backgroundColor,
		secondaryDisabledOpacity: window.getComputedStyle(secondary).opacity
	};
};

const ENABLE_AND_READ = function () {
	const primary = document.querySelector('[data-juneau-action="ack"]');
	const secondary = document.querySelector('[data-juneau-action="esc"]');
	const collapse = document.querySelector('[data-juneau-safe="collapse"]');
	primary.disabled = false;
	secondary.disabled = false;
	const primaryCs = window.getComputedStyle(primary);
	const secondaryCs = window.getComputedStyle(secondary);
	return {
		primaryBg: primaryCs.backgroundColor,
		primaryColor: primaryCs.color,
		secondaryBg: secondaryCs.backgroundColor,
		secondaryColor: secondaryCs.color,
		secondaryOpacity: secondaryCs.opacity,
		collapseHasPrimaryClass: collapse.classList.contains('juneau-view-detail-action-primary'),
		anyDataJuneauEmphasisAttr: document.querySelectorAll('[data-juneau-emphasis]').length > 0
	};
};

async function settle(page) {
	await page.evaluate(() => new Promise(requestAnimationFrame));
	await page.evaluate(() => new Promise(requestAnimationFrame));
}

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node detail-action-button-browser.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage({ viewport: { width: 800, height: 600 } });
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await settle(page);

		const report = {};
		Object.assign(report, await page.evaluate(READ_DISABLED));
		Object.assign(report, await page.evaluate(ENABLE_AND_READ));

		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
