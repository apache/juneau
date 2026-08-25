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
 * column-config.cjs - real-browser XSS canary for the View-tab chooser.
 *
 * Never runs in a default build.  Driven by ColumnConfig_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node column-config.cjs <page.html>
 *
 * Loads the REAL served juneau-config.js in headless Chromium and asserts that a hostile saved-view
 * name and a hostile column label are painted with textContent (literal text, no script execution).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const XSS_NAME = '<img src=x onerror="window.__juneauChooserXss=1">';
	const XSS_LABEL = '<img src=x onerror="window.__juneauChooserLabelXss=1">';
	const NS = window.JuneauViews;
	const out = { hasConfig: !!NS?.config };
	if (!out.hasConfig) return out;

	window.__juneauChooserXss = 0;
	window.__juneauChooserLabelXss = 0;

	const nameEl = document.createElement('span');
	document.body.appendChild(nameEl);
	NS.config.paintUserText(nameEl, XSS_NAME);
	out.nameText = nameEl.textContent;
	out.nameImgCount = nameEl.querySelectorAll('img').length;

	const labelInp = document.createElement('input');
	document.body.appendChild(labelInp);
	NS.config.paintUserInput(labelInp, XSS_LABEL);
	out.labelValue = labelInp.value;

	const th = document.createElement('th');
	th.innerHTML = 'UNTOUCHED-MARKER';
	const tr = document.createElement('tr');
	tr.appendChild(th);
	const thead = document.createElement('thead');
	thead.appendChild(tr);
	const table = document.createElement('table');
	table.appendChild(thead);
	document.body.appendChild(table);
	NS.config.paintHeaderTitles(table, [{ data: 'A', title: XSS_LABEL }], {});
	out.headerText = th.textContent;
	out.headerImgCount = th.querySelectorAll('img').length;

	const cols = [{ data: 'A', title: XSS_LABEL }];
	NS.config.sanitizeColumnTitlesForDataTables(cols);
	out.sanitizedTitleBlank = cols[0].title === '';

	out.xssNameFired = window.__juneauChooserXss === 1;
	out.xssLabelFired = window.__juneauChooserLabelXss === 1;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node column-config.cjs <page.html>\n');
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
