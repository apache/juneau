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
 * progress-browser.cjs - opt-in Chromium canary for the progress renderer.
 *
 * Never runs in a default build.  Driven by Progress_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node progress-browser.cjs <page.html>
 *
 * Loads the REAL served juneau-renders.js, calls display(), injects the HTML string into a fixture
 * DOM (the DataTables innerHTML-class sink).  An <img onerror> cell value must not execute.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const XSS = '<img src=x onerror="window.__juneauProgressXss=1">';
	const NS = window.JuneauViews;
	const r = typeof NS?.resolveRenderer === 'function' ? NS.resolveRenderer('progress') : null;
	const out = { hasInit: !!(r && typeof r.display === 'function') };
	if (!out.hasInit) return out;

	window.__juneauProgressXss = 0;
	const host = document.createElement('div');
	document.body.appendChild(host);
	host.innerHTML = r.display(XSS, {}, { max: '100' });
	out.xssFired = window.__juneauProgressXss === 1;
	out.imgCount = host.querySelectorAll('img').length;
	out.hostileEmpty = !host.querySelector('.jc-progress-bar');

	host.innerHTML = r.display(50, {}, { max: '100' });
	out.ok = !!host.querySelector('.is-ok');
	host.innerHTML = r.display(80, {}, { max: '100', warn: '80' });
	out.warn = !!host.querySelector('.is-warn');
	host.innerHTML = r.display(130, {}, { max: '100' });
	out.exceeds = !!host.querySelector('.is-exceeds');
	const bar = host.querySelector('.jc-progress-bar');
	out.overWidth = bar ? bar.getAttribute('style') : '';
	out.overLabel = host.textContent.indexOf('130%') >= 0;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node progress-browser.cjs <page.html>\n');
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
