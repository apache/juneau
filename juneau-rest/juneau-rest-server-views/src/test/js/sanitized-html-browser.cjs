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
 * sanitized-html-browser.cjs - opt-in Chromium XSS canary for the SANITIZED_HTML detail-field format
 * (DetailField.Format.SANITIZED_HTML), driven by SanitizedHtml_BrowserTest under `mvn -Pjs-tests`.
 *
 * Never runs in a default build.  ViewsJs_RowDetail_Test's b17* battery already proves the copier's
 * behavior against this module's own regex-fixture DOMParser shim (row-detail.cjs) - a hand-rolled test
 * parser, not a browser HTML parser (see the fidelity note at the top of that file's SANITIZED_HTML
 * section). This harness proves the SAME never-executes guarantee against a REAL browser HTML parser and
 * a real DOM, which that shim cannot stand in for.
 *
 *   Usage:  node sanitized-html-browser.cjs <page.html>
 *
 * Loads the REAL served juneau-views.js in headless Chromium and asserts that a hostile expand-JSON
 * SANITIZED_HTML value is copied through the allowlist (no script/handler executes), while benign markup
 * (a <b> and a <table>) survives as real elements.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const I = NS?.init;
	const out = { hasFillSanitizedHtmlSlot: typeof I?.fillSanitizedHtmlSlot === 'function' };
	if (!out.hasFillSanitizedHtmlSlot) return out;

	window.__juneauSanitizedHtmlXss = 0;

	const slot = document.createElement('div');
	slot.dataset.juneauField = 'body';
	slot.setAttribute('data-juneau-field-format', 'sanitizedHtml');
	document.body.appendChild(slot);

	// Hostile half: a real <script> plus an <img onerror> - neither may produce an executable node, and a
	// benign <b> alongside them must still survive (proves the copier, not just an empty result).
	const XSS = '<script>window.__juneauSanitizedHtmlXss=1</script>'
		+ '<img src=x onerror="window.__juneauSanitizedHtmlXss=1">'
		+ '<b>survivor</b>';
	I.fillSanitizedHtmlSlot(slot, XSS);
	out.xssFired = window.__juneauSanitizedHtmlXss === 1;
	out.hasScript = slot.querySelectorAll('script').length > 0;
	out.hasImg = slot.querySelectorAll('img').length > 0;
	out.survivorBold = slot.querySelectorAll('b').length === 1 && slot.textContent.indexOf('survivor') >= 0;

	// Benign half: a <b>/<table> must render as real elements, not escaped text.
	const OK = '<p>Hello <b>world</b></p><table><tr><td>1</td><td>2</td></tr></table>';
	I.fillSanitizedHtmlSlot(slot, OK);
	out.okHasTable = slot.querySelectorAll('table').length === 1;
	out.okHasBold = slot.querySelectorAll('b').length === 1;
	out.okTextHasWorld = slot.textContent.indexOf('world') >= 0;
	out.okTextHasMarkup = slot.textContent.indexOf('<b>') >= 0;

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node sanitized-html-browser.cjs <page.html>\n');
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
