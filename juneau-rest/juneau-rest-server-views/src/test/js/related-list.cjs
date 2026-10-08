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
 * related-list.cjs - real-browser, real-server prober for a nested related list: the REAL juneau-views.js runtime
 * boots a server-mode root table with a nested table in its row detail, then pages, sorts and runs a row action
 * inside the nested table.  Assertions on what the server received live in the Java test.
 *
 * Never runs in a default build.  Driven by RelatedList_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node related-list.cjs <baseUrl>
 */
'use strict';

const { chromium } = require('playwright');

/*
 * Records everything needed to diagnose a run that never draws a row - every console message, every page error,
 * every failed request, and every POST (body + response status/body) - so a failure is self-explaining when the
 * prober's stderr is the only artifact.  Printed to stderr only when the run fails.
 */
function attachDiagnostics(page) {
	const log = [];
	page.on('console', m => log.push('console.' + m.type() + ': ' + m.text()));
	page.on('pageerror', e => log.push('pageerror: ' + String(e)));
	page.on('requestfailed', r => log.push('requestfailed: ' + r.method() + ' ' + r.url() + ' - ' + (r.failure()?.errorText || '?')));
	page.on('response', async r => {
		const req = r.request();
		if (req.method() === 'GET' && r.ok()) return;
		let body = '';
		try { body = (await r.text()).slice(0, 2000); } catch (error) { body = '<unreadable: ' + error + '>'; }
		log.push('response: ' + req.method() + ' ' + r.url() + ' -> ' + r.status()
			+ '\n  request body: ' + String(req.postData() || '').slice(0, 2000)
			+ '\n  response body: ' + body);
	});
	return log;
}

function dumpDiagnostics(diag) {
	process.stderr.write('--- prober diagnostics (' + diag.length + ' entries) ---\n' + diag.join('\n') + '\n--- end diagnostics ---\n');
}

const PROBE = async function () {
	const out = {};

	// Direct-child selectors: a nested table (and the DataTables child row hosting it) also lives inside the root
	// table's <tbody>, so a descendant selector would count nested rows as root rows.
	function rootRows() { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		return document.querySelectorAll('table[data-juneau-view="root"] > tbody > tr[data-juneau-row-id]');
	}
	function nestedRows() { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		return document.querySelectorAll('table[data-juneau-view="events"] > tbody > tr');
	}
	// The root table has a row-detail template, so column 0 is the expander (td.juneau-view-detail-control):
	// id is children[1], name is children[2].
	function rootName(row) {
		const tr = rootRows()[row];
		return tr?.children[2] ? tr.children[2].textContent : null;
	}
	function detailControl(row) {
		const tr = rootRows()[row];
		return tr ? tr.querySelector('td.juneau-view-detail-control') : null;
	}
	// A nested table first draws a "Loading..."/"No data" placeholder row (td.dt-empty); wait for a real data row.
	function nestedDataRow() {
		for (const tr of nestedRows()) {
			const td = tr.children[0];
			if (td && !td.classList.contains('dt-empty') && !td.classList.contains('dataTables_empty')) return tr;
		}
		return null;
	}
	// The nested view declares no row-detail template, so its column 0 is its own "what" data cell - never an
	// expander inherited from the parent's template (the findRowDetailTemplate pre-wrap regression).
	function nestedText() {
		const tr = nestedDataRow();
		return tr ? tr.children[0].textContent : null;
	}
	function nestedHasExpander() { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		return document.querySelector('table[data-juneau-view="events"] td.juneau-view-detail-control, '
			+ 'table[data-juneau-view="events"] th.juneau-view-detail-th') != null;
	}
	function sleep(ms) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser
		return new Promise(r => setTimeout(r, ms));
	}
	async function waitFor(pred, ms) {
		const started = Date.now();
		while (!pred() && Date.now() - started < ms)
			await sleep(50);
	}

	const nestedTable = () => document.querySelector('table[data-juneau-view="events"]'); // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source into the browser

	// Expand row 1 -> the nested list boots and draws its first page.
	detailControl(0)?.click();
	await waitFor(() => nestedDataRow() != null, 5000);
	const first = nestedText();

	// Paging: advance the nested DataTable one page; the server answers with the next slice.
	window.jQuery(nestedTable()).DataTable().page('next').draw('page');
	await waitFor(() => nestedText() !== first, 5000);
	out.paging = { firstPageFirstRow: first, secondPageFirstRow: nestedText() };

	// Sorting: the column starts ascending, so one click on the header's sort control orders it descending.
	window.jQuery(nestedTable()).DataTable().page('first').draw('page');
	await waitFor(() => nestedText() === first, 5000);
	nestedTable().querySelector('thead th span.dt-column-order').click();   // the header's sort control; a click elsewhere in the th is deliberately swallowed
	await waitFor(() => nestedText() !== first, 5000);
	out.sorting = { firstRowDescending: nestedText() };

	// Row action: open the first nested row's menu and run the action.
	const trigger = nestedTable().querySelector('.juneau-view-action-trigger');
	trigger?.click();
	await waitFor(() => document.querySelector('.juneau-view-action-menu') != null, 3000);
	out.action = { menuOpened: document.querySelector('.juneau-view-action-menu') != null };
	document.querySelector('.juneau-view-action-item')?.click();
	await sleep(1000);
	out.action.notice = Array.from(nestedTable().querySelectorAll('[role="alert"], [role="status"], .juneau-view-action-notice'))
		.map(n => n.textContent).join('|');

	return out;
};

(async () => {
	const [baseUrl] = process.argv.slice(2);
	if (!baseUrl) {
		process.stderr.write('usage: node related-list.cjs <baseUrl>\n');
		process.exit(2);
	}

	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const diag = attachDiagnostics(page);
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });

		let report;
		try {
			await page.goto(baseUrl);
			await page.waitForFunction(() => !!(window.jQuery?.fn?.DataTable && window.JuneauDataTables && window.JuneauViews), null, { timeout: 15000 });
			await page.waitForSelector('table[data-juneau-view="root"] > tbody > tr[data-juneau-row-id]', { timeout: 15000 });
			report = await page.evaluate(PROBE);
		} catch (error) {
			dumpDiagnostics(diag);
			throw error;
		}
		report.jsFailures = failures.slice();
		report.diagnostics = diag.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
