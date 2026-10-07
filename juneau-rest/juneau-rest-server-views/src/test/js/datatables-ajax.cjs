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
 * datatables-ajax.cjs - real-browser, real-server prober for the DataTables wire contract: the DataTables library,
 * JuneauDataTables.ajax, and the DataTablesQuery adapter all agreeing on one JSON shape.
 *
 * Never runs in a default build.  Driven by DataTablesAjax_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node datatables-ajax.cjs <baseUrl>
 *
 * <baseUrl> is a REAL http:// origin (a JDK HttpServer the Java test starts), not a file:// fixture - unlike this
 * module's other probers, because the point here is a real round trip: real DataTables library, real
 * juneau-datatables.js, real POST, real adapter.  Prints ONE JSON object to stdout; the Java test does every
 * assertion (including the server-side X-Test-Header capture, which this script never sees).
 *
 * Selectors cover both the DataTables 1.x and 2.x DOM (2.x renamed the wrapper classes and dropped the
 * #<table>_filter / #<table>_next ids), scoped to each table's #<table>_wrapper.
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
	const sleep = ms => new Promise(r => setTimeout(r, ms));
	// Every step polls for its own observable outcome instead of sleeping a fixed time: server-side DataTables 2
	// debounces search input (searchDelay, 400ms by default) and each draw is a real round trip, so a fixed sleep
	// reads the PREVIOUS draw on a slow box.  A step that times out is recorded in out.timeouts (the Java test then
	// fails on the step's own assertion, with the timeout visible in the report).
	out.timeouts = [];
	async function waitFor(step, pred, ms) {
		const started = Date.now();
		while (!pred()) {
			if (Date.now() - started >= ms) { out.timeouts.push(step); return false; }
			await sleep(50);
		}
		return true;
	}

	function infoText(id) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only the probe's own source into the browser context
		const el = document.querySelector('#' + id + '_wrapper .dt-info, #' + id + '_wrapper .dataTables_info');
		return el ? el.textContent : null;
	}
	function firstRowText(id, col) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only the probe's own source into the browser context
		const row = document.querySelector('#' + id + ' tbody tr');
		return row?.children[col] ? row.children[col].textContent : null;
	}
	function searchInput(id) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only the probe's own source into the browser context
		return document.querySelector('#' + id + '_wrapper input[type=search]');
	}
	function nextButton(id) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only the probe's own source into the browser context
		return document.querySelector('#' + id + '_wrapper .dt-paging-button.next, #' + id + '_wrapper .paginate_button.next');
	}
	function nameHeader(id) { // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only the probe's own source into the browser context
		return document.querySelector('#' + id + ' thead th:nth-child(2)');
	}
	function isFiltered(id) {
		return /filtered/i.test(infoText(id) || '');
	}

	// 1) #t1 (new DataTable(...) with JuneauDataTables.ajax) drew page 1.
	out.t1Loaded = { info: infoText('t1'), firstRow: firstRowText('t1', 1) };

	// 2) #t2 (data-juneau-datatable-ajax auto-init) drew the same shape of rows.
	out.t2Loaded = { info: infoText('t2'), firstRow: firstRowText('t2', 1) };

	// 3) Global search on #t1 narrows recordsFiltered (reflected in the info text: "... (filtered from 50 ...)").
	const search = searchInput('t1');
	search.value = 'Row 2';
	search.dispatchEvent(new Event('input', { bubbles: true }));
	await waitFor('search', () => isFiltered('t1'), 10000);
	out.t1Search = { info: infoText('t1') };

	// Clear the search and wait until the UNFILTERED page 1 is back, so step 4's "before" is not the filtered draw.
	search.value = '';
	search.dispatchEvent(new Event('input', { bubbles: true }));
	await waitFor('clearSearch', () => !isFiltered('t1') && firstRowText('t1', 1) === 'Row 01', 10000);

	// 4) Clicking the "name" header reverses the first row (table was initially sorted by name ascending).
	const beforeSort = firstRowText('t1', 1);
	nameHeader('t1').click();
	await waitFor('sort', () => firstRowText('t1', 1) !== beforeSort, 10000);
	out.t1Sort = { before: beforeSort, after: firstRowText('t1', 1) };

	// Restore ascending order before paging, so page 2's first row is deterministic ("Row 11").
	nameHeader('t1').click();
	await waitFor('restoreSort', () => firstRowText('t1', 1) === 'Row 01', 10000);

	// 5) Clicking "Next" shows rows 11-20.
	nextButton('t1').click();
	await waitFor('nextPage', () => firstRowText('t1', 1) !== 'Row 01', 10000);
	out.t1Page2 = { firstRow: firstRowText('t1', 1) };

	// 6) #t3 has a bad column (data: 'nope'); its draws surface the server's error via DataTables' default
	// errMode dialog (window.alert), captured by the harness's page.on('dialog') listener below.  DataTables
	// (dt-error) or the Juneau glue (error.dt) fires its event just before that alert, which is what this step
	// waits on.  Searching the column makes the failure independent of the initial-order draw.
	let t3Errored = false;
	window.jQuery('#t3').on('error.dt dt-error.dt', () => { t3Errored = true; });
	window.JuneauDataTables_t3.table.column(0).search('x').draw();
	await waitFor('t3Error', () => t3Errored, 10000);
	await sleep(100); // let the alert itself reach page.on('dialog')

	return out;
};

(async () => {
	const [baseUrl] = process.argv.slice(2);
	if (!baseUrl) {
		process.stderr.write('usage: node datatables-ajax.cjs <baseUrl>\n');
		process.exit(2);
	}

	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const diag = attachDiagnostics(page);
		const failures = [];
		const dialogs = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		// #t3 (the only table sending X-Test-Header) deliberately draws a bad column, so the server answers 400 and the
		// browser logs one "Failed to load resource" console error per response.  Those are expected; anything else is not.
		let expectedBadColumn400s = 0;
		page.on('response', r => {
			if (r.status() === 400 && r.request().headers()['x-test-header']) expectedBadColumn400s++;
		});
		page.on('dialog', async d => { dialogs.push(d.message()); await d.dismiss(); });

		let report;
		try {
			await page.goto(baseUrl);
			await page.waitForFunction(() => !!(window.jQuery?.fn?.DataTable && window.JuneauDataTables), null, { timeout: 15000 });
			// A real data row (not the single-cell "Loading..."/"No data" td.dt-empty placeholder) in both #t1 and #t2.
			await page.waitForFunction(() => ['t1', 't2'].every(id => {
				const td = document.querySelector('#' + id + ' tbody tr td');
				return td && !td.classList.contains('dt-empty') && !td.classList.contains('dataTables_empty');
			}), null, { timeout: 15000 });
			report = await page.evaluate(PROBE);
		} catch (error) {
			dumpDiagnostics(diag);
			throw error;
		}
		const is400 = f => /Failed to load resource.*\b400\b/.test(f);
		const unexpected = [];
		const expected = [];
		for (const f of failures) (is400(f) && expected.length < expectedBadColumn400s ? expected : unexpected).push(f);
		report.jsFailures = unexpected;
		report.expectedBadColumnFailures = expected;
		report.diagnostics = diag.slice();
		report.dialogs = dialogs.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
