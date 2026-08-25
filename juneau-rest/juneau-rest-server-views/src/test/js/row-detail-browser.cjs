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
 * row-detail-browser.cjs - opt-in Chromium XSS canary for row-detail slot fill (row-detail-445a).
 *
 * Never runs in a default build.  Driven by RowDetail_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node row-detail-browser.cjs <page.html>
 *
 * Loads the REAL served juneau-views.js in headless Chromium and asserts that a hostile expand-JSON
 * field is painted with textContent (literal text, no script execution).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const XSS = '<img src=x onerror="window.__juneauDetailXss=1">';
	const NS = window.JuneauViews;
	const I = NS?.init;
	const out = { hasInit: typeof I?.fillDetailSlots === 'function' };
	if (!out.hasInit) return out;

	window.__juneauDetailXss = 0;

	const slot = document.createElement('div');
	slot.dataset.juneauField = 'title';
	document.body.appendChild(slot);
	const root = document.createElement('div');
	root.appendChild(slot);
	I.fillDetailSlots(root, { title: XSS });
	out.titleText = slot.textContent;
	out.titleImgCount = slot.querySelectorAll('img').length;
	out.xssFired = window.__juneauDetailXss === 1;

	const hostile = '../etc/passwd?x=http://evil';
	out.sub = I.substituteDetailUrl('/data/alerts/{id}', hostile);
	out.subEncoded = out.sub === '/data/alerts/' + encodeURIComponent(hostile);
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node row-detail-browser.cjs <page.html>\n');
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
