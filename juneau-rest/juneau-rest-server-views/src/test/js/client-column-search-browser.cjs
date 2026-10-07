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
 * client-column-search-browser.cjs - real-browser prober for client-mode column-search DSL evaluation (WORK-J0612)
 * against REAL DataTables 2.1.8: a client-side table (inline data, no server) whose `status` column carries search
 * metadata is filtered through JuneauViews.init.setColumnExpr(...) -> column().search.fixed("juneau-dsl", fn).
 * Proves that $in(Triaged,New) shows exactly the two matching rows, that DataTables' own global search box still
 * narrows alongside it, that native col.search() is never written, and that Copy link carries the expression.
 *
 * Never runs in a default build.  Driven by ClientColumnSearch_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node client-column-search-browser.cjs <page.html>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const sleep = ms => new Promise(r => setTimeout(r, ms));
	const out = {};
	const NS = window.JuneauViews;
	const table = document.getElementById('t');
	const dt = window.__clientDt;
	const ctx = {
		table: table,
		viewDef: {
			id: 'clientsearch', primary: true, dataMode: 'client',
			columns: [
				{ data: 'id' },
				{ data: 'status', search: { type: 'enum', operators: [{ name: '$eq' }, { name: '$in' }] } },
				{ data: 'name' }
			]
		},
		optsColumns: [{ data: 'id' }, { data: 'status' }, { data: 'name' }],
		dataTable: dt,
		clientFiltered: true
	};
	table.__juneauCtx = ctx;

	function bodyRows() {
		return Array.from(table.querySelectorAll('tbody tr'))
			.filter(tr => !tr.querySelector('td.dt-empty'))
			.map(tr => tr.children[0].textContent);
	}

	out.hasSearchFixed = typeof dt.column(1).search.fixed === 'function';
	out.before = bodyRows();

	const r = NS.init.setColumnExpr(ctx, dt.column(1), '$in(Triaged,New)');
	out.setOk = r.ok;
	if (r.api && typeof r.api.draw === 'function') r.api.draw();
	out.filtered = bodyRows();
	out.nativeSearch = dt.column(1).search();
	out.storedExpr = NS.init.getColumnExpr(ctx, dt.column(1));

	// The global search box still works alongside the column predicate.
	const box = document.querySelector('#t_wrapper input[type=search], .dt-container input[type=search]');
	out.hasGlobalBox = !!box;
	if (box) {
		box.value = 'beta';
		box.dispatchEvent(new Event('input', { bubbles: true }));
		await sleep(600); // past any search debounce
		out.globalAndColumn = bodyRows();
		box.value = '';
		box.dispatchEvent(new Event('input', { bubbles: true }));
		await sleep(600);
		out.globalCleared = bodyRows();
	}

	// An invalid expression installs nothing; the previous predicate stays.
	const bad = NS.init.setColumnExpr(ctx, dt.column(1), '$gt(1)');
	out.badOk = bad.ok;
	out.badCode = bad.error ? bad.error.code : null;
	dt.draw();
	out.afterBad = bodyRows();

	// Copy link carries the store's expression.
	out.shareUrl = NS.init.buildShareableUrl(table, ctx);
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node client-column-search-browser.cjs <page.html>\n'); process.exit(2); }
	if (!fs.existsSync(fixture)) throw new Error('fixture not found: ' + fixture);
	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.waitForFunction(() => !!(window.__clientDt && window.JuneauViews?.search), null, { timeout: 15000 });
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => { process.stderr.write(String(error?.stack || error) + '\n'); process.exit(1); });
