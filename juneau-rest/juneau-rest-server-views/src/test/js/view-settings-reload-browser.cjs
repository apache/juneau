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
 * view-settings-reload-browser.cjs - real-browser, real-server prober for Gap 1 end to end: a column hidden through
 * the View Settings dialog STAYS hidden after a real page reload, in both data modes.  Real DataTables, real
 * juneau-views.js / juneau-config.js, real localStorage (same http:// origin across the reload), real dialog clicks.
 *
 * Never runs in a default build.  Driven by ViewSettingsReload_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node view-settings-reload-browser.cjs <baseUrl>
 *
 * Visits <baseUrl>/server and <baseUrl>/client.  For each: records the header titles, opens the gear, unchecks the
 * "Status" column's visibility checkbox, clicks Apply, records headers again, reloads, records headers and the first
 * row's cells.  Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const { chromium } = require('playwright');

const HEADERS = () => Array.from(document.querySelectorAll('table[data-juneau-view="t1"] thead tr:first-child th'))
	.map(th => { const t = th.querySelector('.dt-column-title'); return (t || th).textContent.trim(); })
	.filter(x => x !== '');
const FIRST_ROW = () => {
	const tr = document.querySelector('table[data-juneau-view="t1"] tbody tr');
	return tr ? Array.from(tr.children).map(td => td.textContent.trim()) : [];
};

async function settled(page) {
	await page.waitForFunction(() => !!(window.jQuery?.fn?.DataTable && window.JuneauViews),
		null, { timeout: 15000 });
	await page.waitForFunction(() => {
		const td = document.querySelector('table[data-juneau-view="t1"] tbody tr td');
		return !!td && !td.classList.contains('dt-empty') && !td.classList.contains('dataTables_empty');
	}, null, { timeout: 15000 });
}

(async () => {
	const [baseUrl] = process.argv.slice(2);
	if (!baseUrl) { process.stderr.write('usage: node view-settings-reload-browser.cjs <baseUrl>\n'); process.exit(2); }
	const browser = await chromium.launch();
	const diag = [];
	try {
		const failures = [];
		const report = {};
		for (const mode of ['server', 'client']) {
			const context = await browser.newContext();
			const page = await context.newPage();
			page.on('pageerror', e => { failures.push(mode + ': ' + String(e)); diag.push(mode + " pageerror: " + (e?.stack || e)); });
			page.on('console', m => { diag.push(mode + ' console.' + m.type() + ': ' + m.text()); if (m.type() === 'error') failures.push(mode + ': ' + m.text()); });
			page.on('requestfailed', r => diag.push(mode + ' requestfailed: ' + r.url()));

			await page.goto(baseUrl + mode);
			await settled(page);
			const before = await page.evaluate(HEADERS);
			const rowBefore = await page.evaluate(FIRST_ROW);

			await page.click('.juneau-config-chooser-btn');
			await page.waitForSelector('.juneau-config-col-row[data-col="status"] .juneau-config-col-vis', { timeout: 5000 });
			await page.uncheck('.juneau-config-col-row[data-col="status"] .juneau-config-col-vis');
			await page.click('.juneau-config-apply');
			await page.waitForFunction(() => {
				const ths = Array.from(document.querySelectorAll('table[data-juneau-view="t1"] thead tr:first-child th'));
				return ths.length > 0 && !ths.some(th => /Status/.test(th.textContent));
			}, null, { timeout: 10000 }).catch(() => { /* no-op */ });
			await settled(page);
			const afterApply = await page.evaluate(HEADERS);

			await page.reload();
			await settled(page);
			const afterReload = await page.evaluate(HEADERS);
			const rowAfterReload = await page.evaluate(FIRST_ROW);

			// Finding 1: after the reload the dialog opens on the STORED settings (Status unchecked), not catalog defaults.
			const STATUS_VIS = '.juneau-config-col-row[data-col="status"] .juneau-config-col-vis';
			await page.click('.juneau-config-chooser-btn');
			await page.waitForSelector(STATUS_VIS, { timeout: 5000 });
			const reopen = await page.evaluate((sel) => {
				const cb = document.querySelector(sel);
				return { checked: cb.checked, aria: cb.getAttribute('aria-label') };
			}, STATUS_VIS);
			// Finding 2: Apply with the Sort tab visible and nothing touched must not persist "every column" as the sort.
			const storedSort = await page.evaluate(() => {
				for (let i = 0; i < localStorage.length; i++) {
					const k = localStorage.key(i);
					if (/viewSettings/.test(k)) { const b = JSON.parse(localStorage.getItem(k)); return b.sort; }
				}
				return 'no-blob';
			});
			await page.evaluate(() => window.JuneauViews.config.closeChooserDialog(document.querySelector('#t1').__juneauCtx));

			// Finding 1 (notice): a stale-schemaVersion blob is discarded and the one-time notice shows in the dialog.
			await page.evaluate(() => {
				for (let i = 0; i < localStorage.length; i++) {
					const k = localStorage.key(i);
					if (/viewSettings/.test(k)) { const b = JSON.parse(localStorage.getItem(k)); b.schemaVersion = 1; localStorage.setItem(k, JSON.stringify(b)); }
				}
			});
			await page.reload();
			await settled(page);
			await page.click('.juneau-config-chooser-btn');
			await page.waitForSelector(STATUS_VIS, { timeout: 5000 });
			const stale = await page.evaluate((sel) => {
				const st = document.querySelector('.juneau-config-status');
				return { notice: st ? st.textContent.trim() : null, statusChecked: document.querySelector(sel).checked };
			}, STATUS_VIS);
			report[mode] = { before, rowBefore, afterApply, afterReload, rowAfterReload, reopen, storedSort, stale };
			await context.close();
		}
		report.jsFailures = failures;
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} catch (error) {
		process.stderr.write('--- diagnostics ---\n' + diag.join('\n') + '\n');
		throw error;
	} finally {
		await browser.close();
	}
})().catch(error => { process.stderr.write(String(error?.stack || error) + '\n'); process.exit(1); });
