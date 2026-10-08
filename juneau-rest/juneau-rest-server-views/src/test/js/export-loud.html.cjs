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
 * export-loud.html.cjs - real-browser prober for the ribbon "export loud" contract: with no JSZip loaded, an `excel` button in the hard
 * `buttons` list renders visibly disabled with a tip naming JSZip, while `copy` stays enabled.
 *
 * Never runs in a default build; it is driven by ExportLoud_BrowserTest, which only runs under `mvn -Pjs-tests`.
 *
 *   Usage:  node export-loud.html.cjs <page.html>
 *
 * This script only OBSERVES and prints ONE JSON object; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const out = { hasBuild: typeof NS?.ribbon?.build === 'function' };
	if (!out.hasBuild) return out;
	// Stand in for the DataTables Buttons extension; JSZip is deliberately NOT defined.
	window.jQuery = { fn: { dataTable: { Buttons: function () {} } } };
	const calls = [];
	const ctx = { dataTable: { button: function (id) { return { trigger: function () { calls.push(id); } }; } }, redraw: function () {} };
	const bar = NS.ribbon.build({ ribbon: [{ type: 'export', buttons: ['copy', 'excel'] }] }, ctx);
	document.body.appendChild(bar);
	const buttons = Array.from(bar.querySelectorAll('button')).map(function (b) {
		const r = b.getBoundingClientRect();
		return { disabled: b.disabled, tip: b.dataset.jcTip || '', label: b.getAttribute('aria-label') || '', visible: r.width > 0 && r.height > 0 };
	});
	out.buttons = buttons;
	const live = Array.from(bar.querySelectorAll('button')).find(function (b) { return !b.disabled; });
	if (live) live.click();
	const dead = Array.from(bar.querySelectorAll('button')).find(function (b) { return b.disabled; });
	if (dead) dead.click();
	out.triggered = calls;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node dialog-wizard.html.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
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
