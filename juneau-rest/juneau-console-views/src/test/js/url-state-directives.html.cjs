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
 * url-state-directives.html.cjs - real-browser prober for the ?state= window/ids directives: a deep link seeds the
 * request params, setDirective rewrites the real address bar and the params, and browser Back restores the prior ids.
 *
 * Never runs in a default build; it is driven by UrlStateDirectives_BrowserTest, which only runs under `mvn -Pjs-tests`.
 *
 *   Usage:  node url-state-directives.html.cjs <page.html>
 *
 * This script only OBSERVES and prints ONE JSON object; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const U = NS?.urlState;
	const out = { hasSet: typeof U?.setDirective === 'function' };
	if (!out.hasSet) return out;
	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the browser context.
	const flush = function () { return new Promise(function (r) { setTimeout(r, 30); }); }; // NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) serializes only PROBE's own source

	const table = document.createElement('table');
	table.dataset.juneauView = 'v';
	document.body.appendChild(table);
	let reloads = 0;
	const dt = { columns: function () { return { every: function () {} }; }, order: function () { return []; }, draw: function () { return this; },
		on: function () { return this; }, ajax: { reload: function () { reloads++; } } };
	const ctx = { table: table, viewDef: { id: 'v', urlState: { window: { params: { start: 'from', end: 'to' } }, ids: { max: 2 } } }, dataTable: dt, optsColumns: [] };
	table.__juneauCtx = ctx;

	history.replaceState(null, '', location.pathname + '?state=' + encodeURIComponent('ids(a,b,c)'));
	NS.init.prepareUrlStateDirectives(table, ctx.viewDef, ctx);
	out.seededParams = NS.init.urlStateRequestParams(ctx);
	NS.init.wireShareableUrlState(table, ctx);

	history.pushState(null, '', location.pathname + '?state=' + encodeURIComponent('ids(x,y)'));
	window.dispatchEvent(new PopStateEvent('popstate'));
	await flush();
	out.afterPushIds = NS.init.urlStateRequestParams(ctx).ids;

	reloads = 0;
	U.setDirective(table, 'window', { start: '2026-10-01T00:00:00Z', end: '2026-10-02T00:00:00Z' });
	await flush();
	out.address = decodeURIComponent(location.search);
	out.paramsAfterSet = NS.init.urlStateRequestParams(ctx);
	out.reloadsAfterSet = reloads;

	history.back();
	await flush();
	out.addressAfterBack = decodeURIComponent(location.search);
	out.idsAfterBack = NS.init.urlStateRequestParams(ctx).ids;
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
