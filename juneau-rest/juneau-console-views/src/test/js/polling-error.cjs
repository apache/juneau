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
 * polling-error.cjs - real-browser, real-server prober for the status dot on a FAILING poll: the REAL juneau-views.js
 * and juneau-datatables.js over the real DataTables library, against a server that can be told to answer the next
 * requests with a 500 JSON body, a 500 HTML body, or to drop the connection.
 *
 * Never runs in a default build.  Driven by PollingError_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node polling-error.cjs <baseUrl>
 *
 * For each data mode (server, client) and each failure kind it reloads the table through DataTables' own
 * ajax.reload(), waits for the dot to leave "refreshing", records its state, then switches the server back to good
 * answers and records whether the dot recovers to "idle".
 */
'use strict';

const { chromium } = require('playwright');

// page.evaluate passes ONE argument, so the wanted state and the cap travel together as a [want, ms] pair.
const PROBE_STATE = async function ([want, ms]) {
	const dot = () => document.querySelector('.juneau-view-staleness');
	const started = Date.now();
	while (Date.now() - started < ms) {
		if (dot()?.dataset.state === want) break;
		await new Promise(r => setTimeout(r, 50));
	}
	return { state: dot()?.dataset.state ?? null, tip: dot()?.dataset.jcTip ?? null };
};

(async () => {
	const [baseUrl] = process.argv.slice(2);
	if (!baseUrl) {
		process.stderr.write('usage: node polling-error.cjs <baseUrl>\n');
		process.exit(2);
	}
	const browser = await chromium.launch();
	const out = { cases: {}, jsFailures: [] };
	try {
		for (const mode of ['server', 'client']) {
			for (const fail of ['json500', 'html500', 'abort']) {
				const page = await browser.newPage();
				page.on('dialog', d => d.dismiss());
				page.on('pageerror', e => out.jsFailures.push(mode + '/' + fail + ': ' + String(e)));
				await page.request.get(baseUrl + 'mode?m=ok');
				await page.goto(baseUrl + mode);
				await page.waitForFunction(() => !!document.querySelector('.juneau-view-staleness')
					&& document.querySelectorAll('table[data-juneau-view="root"] > tbody > tr[data-juneau-row-id]').length > 0, null, { timeout: 15000 });
				const initial = await page.evaluate(PROBE_STATE, ['idle', 5000]);

				await page.request.get(baseUrl + 'mode?m=' + fail);
				await page.evaluate(() => window.jQuery('table[data-juneau-view="root"]').DataTable().ajax.reload(null, false));
				const failed = await page.evaluate(PROBE_STATE, ['error', 6000]);

				await page.request.get(baseUrl + 'mode?m=ok');
				await page.evaluate(() => window.jQuery('table[data-juneau-view="root"]').DataTable().ajax.reload(null, false));
				const recovered = await page.evaluate(PROBE_STATE, ['idle', 6000]);

				out.cases[mode + '-' + fail] = { initial: initial.state, failed: failed.state, failedTip: failed.tip, recovered: recovered.state };
				await page.close();
			}
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(out, null, 2) + '\n');
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
