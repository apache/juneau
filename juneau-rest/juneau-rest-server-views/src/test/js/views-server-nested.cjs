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
 * views-server-nested.cjs - real-browser, real-server prober for design §3.1/D8: the REAL juneau-views.js runtime
 * boots a server-mode root table (same VIEW_META/sidecar shape the <@card type="datatables"> macro's
 * CardEnvelope.liftTable emits) with a nested table in its row detail, against a real JDK HttpServer backed by
 * DataTablesQuery.run - proving beforeSend's URL mutation survives a real browser's jQuery/DataTables transport,
 * not just the Node-harness DOM-shim mocks ViewsJs_NestedTable_Test exercises.
 *
 * Never runs in a default build.  Driven by ViewsServerNested_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node views-server-nested.cjs <baseUrl>
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
	function rootRows() {
		return document.querySelectorAll('table[data-juneau-view="root"] > tbody > tr[data-juneau-row-id]');
	}
	function nestedRows() {
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
	function nestedHasExpander() {
		return document.querySelector('table[data-juneau-view="events"] td.juneau-view-detail-control, '
			+ 'table[data-juneau-view="events"] th.juneau-view-detail-th') != null;
	}
	function sleep(ms) {
		return new Promise(r => setTimeout(r, ms));
	}
	async function waitFor(pred, ms) {
		const started = Date.now();
		while (!pred() && Date.now() - started < ms)
			await sleep(50);
	}

	out.root = { firstRow: rootName(0), secondRow: rootName(1) };

	// Expand row 1 -> its row-detail panel's nested table boots and draws, scoped to parentId=1.
	detailControl(0)?.click();
	await waitFor(() => nestedDataRow() != null, 3000);
	out.expand1 = { nestedRow: nestedText(), nestedHasExpander: nestedHasExpander() };

	// Collapse row 1 (tears down its nested table), then expand row 2 -> a FRESH nested table, scoped to
	// parentId=2 - re-derived per request/per expand, not a frozen URL from row 1's init.
	detailControl(0)?.click();
	await waitFor(() => nestedRows().length === 0, 3000);
	detailControl(1)?.click();
	await waitFor(() => nestedDataRow() != null, 3000);
	out.expand2 = { nestedRow: nestedText(), nestedHasExpander: nestedHasExpander() };

	return out;
};

(async () => {
	const [baseUrl] = process.argv.slice(2);
	if (!baseUrl) {
		process.stderr.write('usage: node views-server-nested.cjs <baseUrl>\n');
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
