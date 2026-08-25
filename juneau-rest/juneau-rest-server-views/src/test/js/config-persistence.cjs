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
 * config-persistence.cjs - real-browser prober for the juneau-config.js client-side persistence SPI: the
 * localStorage provider (real Web Storage, not a Node shim) and the server-persisted provider's
 * transport envelope (a stubbed window.fetch).
 *
 * Never runs in a default build.  It is driven by ConfigPersistence_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node config-persistence.cjs <page.html>
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL served juneau-views.js +
 * juneau-config.js - in headless Chromium, then, entirely inside the page, exercises the async persistence
 * facade (NS.persistence) against REAL window.localStorage (Node has no Web Storage API, so this is the one
 * place localStorage-provider behavior can be proven end-to-end) and against a stubbed fetch for the
 * server-persisted provider.  Prints ONE JSON object to stdout.
 *
 * DIVISION OF LABOUR (mirrors row-actions.cjs): this script only OBSERVES; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/*
 * Runs inside the page.  window.JuneauViews.persistence/config are populated by the real juneau-config.js the
 * fixture loaded.  Returns a plain, JSON-serializable report; every step is awaited in order so later steps can
 * rely on earlier ones' effects.
 */
const PROBE = async function () {
	const NS = window.JuneauViews;
	const out = { hasConfig: !!(NS?.config && NS.persistence) };
	if (!out.hasConfig) return out;

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page and
	// break every caller below.
	function makeTable(pageId, viewId, savedViewsBase) {
		const page = document.createElement('div');
		page.dataset.juneauPage = pageId;
		if (savedViewsBase != null) page.dataset.juneauSavedViews = savedViewsBase;
		const table = document.createElement('table');
		table.dataset.juneauView = viewId;
		page.appendChild(table);
		document.body.appendChild(page);
		return table;
	}

	// ---- a) localStorage provider round trip (real Web Storage) ----
	NS.setPersistenceProvider(NS.persistenceProviders.localStorage());
	const tableA = makeTable('reportsA', 'orders');

	out.a_listEmpty = await NS.persistence.list(tableA);
	await NS.persistence.save(tableA, 'My View', { schemaVersion: 1, columns: ['x'] });
	out.a_listAfterSave = await NS.persistence.list(tableA);
	out.a_loaded = await NS.persistence.load(tableA, 'My View');
	await NS.persistence.setActive(tableA, 'My View');
	out.a_activeAfterSetActive = await NS.persistence.getActive(tableA);
	await NS.persistence.delete(tableA, 'My View');
	out.a_activeAfterDelete = await NS.persistence.getActive(tableA);   // dangling -> Default
	await NS.persistence.saveAndActivate(tableA, 'Second View', { schemaVersion: 1, columns: ['y'] });
	out.a_activeAfterSaveAndActivate = await NS.persistence.getActive(tableA);

	// Two tables sharing a view id under different pages must not collide.
	const tableB = makeTable('reportsB', 'orders');
	out.a_otherPageScopeIsIndependent = await NS.persistence.list(tableB);

	// A reserved/blank name rejects with a typed 'malformed' error (never a thrown raw Error, never a silent no-op).
	try {
		await NS.persistence.save(tableA, 'Default', { schemaVersion: 1 });
		out.a_reservedNameRejection = { threw: false };
	} catch (e) {
		out.a_reservedNameRejection = { threw: true, code: e.code };
	}

	// ---- b) localStorage per-scope quota (MAX_VIEWS_PER_SCOPE = 50) ----
	const tableC = makeTable('quotaPage', 'quotaView');
	for (let i = 0; i < NS.config.LOCALSTORAGE_MAX_VIEWS_PER_SCOPE; i++)
		await NS.persistence.save(tableC, 'v' + i, { schemaVersion: 1 });
	try {
		await NS.persistence.save(tableC, 'oneTooMany', { schemaVersion: 1 });
		out.b_overQuota = { threw: false };
	} catch (e) {
		out.b_overQuota = { threw: true, code: e.code };
	}

	// ---- c) cross-tab storage-event reconcile (localStorage provider only) ----
	const lsProvider = NS.persistenceProviders.localStorage();
	const tableD = makeTable('watchPage', 'watchView');
	const seen = [];
	const unwatch = lsProvider.watchExternalChanges(tableD, function (change) { seen.push(change.key); });
	// A real cross-tab write never fires 'storage' in the SAME document that wrote it, so this dispatches the
	// native event by hand to simulate what another tab's write would deliver to this one.
	window.dispatchEvent(new StorageEvent('storage', { key: 'juneau.view.' + NS.config.scopeKey('watchPage', 'watchView') + '.columns.views.someKey', oldValue: null, newValue: '{}' }));
	window.dispatchEvent(new StorageEvent('storage', { key: 'juneau.view.SOME-OTHER-SCOPE.columns.views.someKey', oldValue: null, newValue: '{}' }));
	unwatch();
	window.dispatchEvent(new StorageEvent('storage', { key: 'juneau.view.' + NS.config.scopeKey('watchPage', 'watchView') + '.columns.views.afterUnwatch', oldValue: null, newValue: '{}' }));
	out.c_storageEventsSeen = seen;

	// ---- d) server-persisted provider transport envelope (stubbed fetch) ----
	const calls = [];
	const realFetch = window.fetch;
	window.fetch = function (url, init) {
		calls.push({ url: url, init: { method: init.method, headers: init.headers, body: init.body, credentials: init.credentials } });
		if (init.method === 'GET')
			return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve(JSON.stringify({ active: null, views: [] })) });
		return Promise.resolve({ ok: true, status: 200, text: () => Promise.resolve('') });
	};

	NS.setPersistenceProvider(NS.persistenceProviders.server());
	const tableE = makeTable('serverPage', 'serverView', '/ctx/juneau-saved-views');
	// writeRequest reads the token straight off the table (mirrors juneau-views.js's own resolveCsrfToken) - a
	// real host stamps this from its own CSRF-issuance story, out of scope for this prober.
	tableE.dataset.juneauCsrf = 'tok-123';

	calls.length = 0;
	await NS.persistence.list(tableE);
	out.d_listCall = calls[0];

	calls.length = 0;
	await NS.persistence.save(tableE, 'My View', { schemaVersion: 1 });
	out.d_saveCall = calls[0];

	calls.length = 0;
	await NS.persistence.saveAndActivate(tableE, 'My View', { schemaVersion: 1 });
	out.d_saveAndActivateCall = calls[0];

	calls.length = 0;
	await NS.persistence.setActive(tableE, null);
	out.d_clearActiveCall = calls[0];

	calls.length = 0;
	await NS.persistence.delete(tableE, 'My View');
	out.d_deleteCall = calls[0];

	// Fail-closed: no [data-juneau-saved-views] shell -> 'unavailable', zero fetch calls.
	calls.length = 0;
	const tableF = makeTable('noShellPage', 'noShellView', null);
	try {
		await NS.persistence.list(tableF);
		out.d_noShell = { threw: false, calls: calls.length };
	} catch (e) {
		out.d_noShell = { threw: true, code: e.code, calls: calls.length };
	}

	window.fetch = realFetch;
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node config-persistence.cjs <page.html>\n');
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
