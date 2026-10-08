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
 * confirm-renderer.html.cjs - real-browser prober for the confirm pipeline: a registered renderer paints its own
 * modal and its real Confirm/Cancel buttons decide whether the row action's write is sent, and a danger confirm in the
 * built-in modal focuses Cancel first.
 *
 * Never runs in a default build; it is driven by ConfirmRenderer_BrowserTest, which only runs under `mvn -Pjs-tests`.
 *
 *   Usage:  node confirm-renderer.html.cjs <page.html>
 *
 * This script only OBSERVES and prints ONE JSON object; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const out = { hasRun: typeof NS?.rowActions?.run === 'function' };
	if (!out.hasRun) return out;

	const fetchCalls = [];
	window.fetch = function (url, opts) { fetchCalls.push({ url: url, opts: opts }); return Promise.resolve({ ok: true, status: 200 }); };

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the browser context.
	const flush = function () { return new Promise(function (r) { setTimeout(r, 20); }); }; // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source

	// A renderer that paints its own styled modal; its real buttons settle the confirm.
	NS.rowActions.registerConfirmRenderer('styled', function (modal) {
		return new Promise(function (resolve) {
			const box = document.createElement('div');
			box.id = 'styled-modal';
			box.style.cssText = 'position:fixed;top:20px;left:20px;width:240px;height:80px;background:#eee;z-index:5000';
			box.textContent = modal.title;
			const ok = document.createElement('button');
			ok.id = 'styled-ok';
			ok.textContent = modal.confirmLabel;
			const cancel = document.createElement('button');
			cancel.id = 'styled-cancel';
			cancel.textContent = modal.cancelLabel;
			function done(v) { box.remove(); resolve(v); }
			ok.addEventListener('click', function () { done(true); });
			cancel.addEventListener('click', function () { done(false); });
			box.appendChild(ok);
			box.appendChild(cancel);
			document.body.appendChild(box);
		});
	});

	const action = { id: 'delete', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE', present: 'dialog',
		confirm: 'Delete {name}?', confirmLabel: 'Delete it', tone: 'danger', confirmRenderer: 'styled' };
	const table = document.createElement('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-1';
	const tbody = document.createElement('tbody');
	const tr = document.createElement('tr');
	tr.dataset.juneauRowId = '7';
	const td = document.createElement('td');
	const btn = document.createElement('button');
	btn.textContent = 'go';
	td.appendChild(btn);
	tr.appendChild(td);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	document.body.appendChild(table);
	table.__juneauCtx = { viewDef: { id: 'v', rowActions: [action] }, dataTable: { row: function () { return { data: function () { return { id: 7, name: 'Widget' }; } }; } } };

	NS.rowActions.run(btn, 'delete');
	await flush();
	const modal = document.getElementById('styled-modal');
	out.styledText = modal?.firstChild?.textContent ?? null;
	out.styledOkLabel = document.getElementById('styled-ok')?.textContent ?? null;
	const r = modal ? modal.getBoundingClientRect() : null;
	out.styledVisible = !!r && r.width > 0 && r.height > 0;
	out.fetchesBeforeClick = fetchCalls.length;
	document.getElementById('styled-ok').click();
	await flush();
	out.confirmFetches = fetchCalls.length;
	out.confirmMethod = fetchCalls[0]?.opts.method ?? null;
	out.confirmUrl = fetchCalls[0]?.url ?? null;

	NS.rowActions.run(btn, 'delete');
	await flush();
	document.getElementById('styled-cancel').click();
	await flush();
	out.fetchesAfterCancel = fetchCalls.length;

	// The built-in modal: a danger confirm focuses Cancel first.
	const pending = NS.dialogs.confirm({ title: 'Really?', tone: 'danger', confirmLabel: 'Do it' });
	await flush();
	out.dangerCancelFocused = document.activeElement?.classList.contains('juneau-view-dialog-cancel') === true;
	document.querySelector('.juneau-view-dialog-confirm').click();
	out.builtInResolved = await pending;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node confirm-renderer.html.cjs <page.html>\n');
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
