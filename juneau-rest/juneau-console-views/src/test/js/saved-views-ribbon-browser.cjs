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
 * saved-views-ribbon-browser.cjs - real-browser prober for the ribbon's saved-views drop-down: hidden until the table
 * has a saved view, placed immediately before the gear, applies a view on selection, marks "(modified)" when the
 * table differs from the selected view, and absent when the table turns it off.
 *
 * Never runs in a default build.  Driven by SavedViewsRibbon_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node saved-views-ribbon-browser.cjs <fixture.html>
 */
'use strict';

const path = require('node:path');
const { chromium } = require('playwright');

const GEAR = '.juneau-config-chooser-btn';
const DROP = '.juneau-config-ribbon-views';

const MOUNT = (off) => {
	const NS = window.JuneauViews;
	localStorage.clear();
	const host = document.getElementById('host');
	host.textContent = '';
	const table = document.createElement('table');
	table.dataset.juneauView = 'svrbrowser';
	host.appendChild(table);
	const catalog = [
		{ data: 'name', title: 'Name', orderable: true },
		{ data: 'status', title: 'Status', orderable: true },
		{ data: 'owner', title: 'Owner', orderable: true }
	];
	const viewDef = { id: 'svrbrowser', columns: catalog, columnConfig: {} };
	if (off) viewDef.savedViewsInRibbon = false;
	const ctx = { viewDef: viewDef };
	window.__ctx = ctx;
	table.__juneauCtx = ctx;
	NS.init.buildTable = function () { return { ok: true }; };
	NS.config.mountChooser(table, ctx);
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node saved-views-ribbon-browser.cjs <fixture.html>\n'); process.exit(2); }
	const browser = await chromium.launch();
	const report = {};
	try {
		const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
		const page = await context.newPage();
		const errors = [];
		page.on('pageerror', (e) => errors.push(String(e)));
		page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
		await page.goto('file://' + path.resolve(fixture));

		const shown = () => page.evaluate((s) => { const e = document.querySelector(s); return !!e && !e.hidden && e.offsetParent !== null; }, DROP);
		const labels = () => page.evaluate((s) => Array.prototype.map.call(document.querySelector(s).options, (o) => o.textContent), DROP);
		const selectedLabel = () => page.evaluate((s) => { const e = document.querySelector(s); return e.options[e.selectedIndex].textContent; }, DROP);
		const active = () => page.evaluate(async () => (await window.JuneauViews.persistence.list(document.querySelector('table[data-juneau-view="svrbrowser"]'))).active);

		await page.evaluate(MOUNT, false);
		await page.waitForSelector(GEAR);
		report.hiddenWithoutViews = !(await shown());

		// Save one view through the dialog, so the drop-down has Default plus one.
		await page.click(GEAR);
		await page.waitForSelector('.juneau-config-saveas');
		await page.click('.juneau-config-saveas');
		await page.fill('.juneau-config-name-input', 'Alt');
		await page.keyboard.press('Enter');
		await page.waitForFunction(() => document.querySelector('.juneau-config-view-select').value === 'Alt', null, { timeout: 5000 });
		await page.click('.juneau-config-apply');
		await page.waitForFunction(() => !document.querySelector('.juneau-config-dialog'), null, { timeout: 5000 });
		await page.waitForFunction((s) => { const e = document.querySelector(s); return e && !e.hidden; }, DROP, { timeout: 5000 });
		report.shownWithView = await shown();
		report.labels = await labels();
		report.selected = await selectedLabel();
		report.beforeGear = await page.evaluate(([d, g]) => document.querySelector(d).nextElementSibling === document.querySelector(g), [DROP, GEAR]);

		// Choosing Default applies it and remembers it.
		await page.selectOption(DROP, '');
		await page.waitForFunction(async () => (await window.JuneauViews.persistence.list(document.querySelector('table[data-juneau-view="svrbrowser"]'))).active == null, null, { timeout: 5000 });
		report.activeAfterDefault = await active();
		report.selectedAfterDefault = await selectedLabel();

		// A change made in the dialog and applied leaves the table differing from the selected view.
		await page.click(GEAR);
		await page.waitForSelector('.juneau-config-apply');
		await page.evaluate(() => { window.__ctx._configDraft.visible.pop(); window.__ctx._configDirty = true; });
		await page.click('.juneau-config-apply');
		await page.waitForFunction(() => !document.querySelector('.juneau-config-dialog'), null, { timeout: 5000 });
		await page.waitForFunction((s) => /\(modified\)/.test(document.querySelector(s).selectedOptions[0].textContent), DROP, { timeout: 5000 }).catch(() => {});
		report.modifiedLabel = await selectedLabel();

		// A table that turns the flag off has no drop-down.
		await page.evaluate(MOUNT, true);
		await page.waitForSelector(GEAR);
		report.absentWhenOff = (await page.$(DROP)) === null;

		report.errors = errors;
		await context.close();
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report, null, 2) + '\n');
})().catch((e) => { process.stderr.write(String(e && e.stack || e) + '\n'); process.exit(1); });
