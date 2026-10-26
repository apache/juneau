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
 * dialog-wizard.html.cjs - real-browser prober for a two-step dialog driven through the DialogHandle: an edit step whose
 * Next label is "Review changes", an onConfirm that returns false and relabels the button, and a review step whose
 * confirm sends exactly one write.
 *
 * Never runs in a default build; it is driven by DialogWizard_BrowserTest, which only runs under `mvn -Pjs-tests`.
 *
 *   Usage:  node dialog-wizard.html.cjs <page.html>
 *
 * This script only OBSERVES and prints ONE JSON object; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const out = { hasOpen: typeof NS?.dialogs?.open === 'function' };
	if (!out.hasOpen) return out;

	const fetchCalls = [];
	window.fetch = function (url, opts) { fetchCalls.push({ url: url, opts: opts }); return Promise.resolve({ ok: true, status: 200 }); };
	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the browser context.
	const flush = function () { return new Promise(function (r) { setTimeout(r, 20); }); }; // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source
	const confirmText = function () { return document.querySelector('.juneau-view-dialog-confirm')?.textContent ?? null; };

	const table = document.createElement('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-1';
	const tr = document.createElement('tr');
	table.appendChild(tr);
	document.body.appendChild(table);
	table.__juneauCtx = { viewDef: { id: 'v', rowActions: [] } };

	const action = { id: 'apply', label: 'Apply', endpoint: '/x/apply', method: 'POST' };
	const modal = { title: 'Edit', steps: [
		{ id: 'edit', title: 'Edit', fields: [{ label: 'Name', value: 'Widget' }] },
		{ id: 'review', title: 'Review', fields: [{ label: 'Name', value: 'Widget' }] }
	] };
	const h = NS.dialogs.open(modal, { table: table, tr: tr }, { action: action });
	h.setConfirmLabel('Review changes');
	h.onConfirm(function () {
		if (h.step() === 'edit') { h.setStep('review'); h.setConfirmLabel('Apply 1 change'); return false; }
		return true;
	});
	out.firstLabel = confirmText();
	out.firstStep = h.step();
	document.querySelector('.juneau-view-dialog-confirm').click();
	await flush();
	out.reviewLabel = confirmText();
	out.reviewStep = h.step();
	out.reviewTitle = document.querySelector('.juneau-view-dialog-title')?.textContent ?? null;
	out.fetchesAtReview = fetchCalls.length;
	out.openAtReview = !!document.querySelector('.juneau-view-dialog');
	document.querySelector('.juneau-view-dialog-confirm').click();
	out.closedResult = await h.closed;
	await flush();
	out.fetchesAtEnd = fetchCalls.length;
	out.method = fetchCalls[0]?.opts.method ?? null;
	out.dialogGone = !document.querySelector('.juneau-view-dialog');
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
