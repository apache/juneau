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
 * view-settings-a11y-browser.cjs - real-browser prober for the five end-to-end View Settings scenarios (design
 * §9, F1+F2+F3+Q1 combined): (1) a malformed `?state=tab(a%5C)` tab id alongside a real `filter(status=$eq(OK))`
 * clause: no console/page error, the real location.search decoder handles both, exactly one tab is clicked by
 * dataset string-equality, and the filter expression reaches the (fake) DataTables column; (2) the View Settings
 * dialog driven keyboard-only - focus the gear, Enter opens it, Tab lands on the roving-tabindex View tab, then
 * Right x3 / End / Home move both focus and the visible panel; (3) a column-search popover typed into then
 * dismissed with Escape, proving the per-table announcer text and that focus returns to the search icon; (4) a
 * version-mismatched (CURRENT_SCHEMA_VERSION + 1) View Settings blob surviving a REAL page reload, showing the reset notice exactly once
 * and never again on a second reload; (5) the Copy-link toolbar button: a real click copies the built URL (via a
 * stubbed clipboard) and announces "Link copied." on the per-table live region.
 *
 * Never runs in a default build.  Driven by ViewSettingsA11y_BrowserTest under `mvn -Pjs-tests`; see that class's
 * javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node view-settings-a11y-browser.cjs <page.html>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const GEAR = '.juneau-config-chooser-btn';

/** Reads which tab button is focused and which tabpanel is visible, by className suffix.  Self-contained: no
 *  outer references, since page.evaluate() only serializes this function's own source text. */
const DIALOG_STATE = () => {
	const tabs = ['view', 'search', 'sort', 'options'];
	const active = document.activeElement;
	const panels = document.querySelectorAll('[role="tabpanel"]');
	let visiblePanel = null;
	for (const panel of panels) {
		if (panel.hidden) continue;
		for (const tab of tabs) {
			if (panel.classList.contains('juneau-config-body-' + tab)) { visiblePanel = tab; break; }
		}
	}
	return {
		activeTab: active?.dataset ? (active.dataset.tab || null) : null,
		visiblePanel: visiblePanel
	};
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node view-settings-a11y-browser.cjs <page.html>\n'); process.exit(2); }
	if (!fs.existsSync(fixture)) throw new Error('fixture not found: ' + fixture);
	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });

		// ---- Scenario 1 (F1): ?state=tab(a%5C);filter(status=$eq(OK)) decodes and applies with no throw ---------
		await page.goto(url + '?state=tab(a%5C);filter(status=$eq(OK))');
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const f1 = await page.evaluate(() => {
			const NS = window.JuneauViews;
			const strip = document.createElement('div');
			strip.setAttribute('role', 'tablist');
			document.body.appendChild(strip);
			const clicked = [];
			function makeTab(name) {
				const b = document.createElement('button');
				b.setAttribute('role', 'tab');
				b.dataset.juneauStripTab = name;
				b.addEventListener('click', function () { clicked.push(name); });
				strip.appendChild(b);
				return b;
			}
			makeTab('a\\'); // NOSONAR javascript:S7780 -- String.raw cannot end in a backslash
			makeTab('plain');

			const table = document.createElement('table');
			table.dataset.juneauView = 'f1browser';
			document.body.appendChild(table);

			let appliedExpr = null;
			const col = {
				index: function () { return 0; },
				header: function () { return null; },
				search: function (v) {
					if (arguments.length) { appliedExpr = v; return this; }
					return appliedExpr || '';
				},
				draw: function () { return this; }
			};
			const dt = {
				columns: function () { return { every: function (fn) { fn.call(col); } }; },
				column: function () { return col; },
				order: function () { return dt; },
				draw: function () { return dt; },
				on: function () { return dt; }
			};
			const ctx = {
				table: table,
				viewDef: { id: 'f1browser', primary: true },
				dataTable: dt,
				optsColumns: [{ data: 'status' }]
			};

			const state = NS.urlState.readFromSearch(window.location.search);
			NS.init.applyShareableOpenState(table, ctx, state);

			return {
				decodedTab: state ? state.tab : null,
				decodedFilterColumn: state?.filters?.[0] ? state.filters[0].column : null,
				decodedFilterExpr: state?.filters?.[0] ? state.filters[0].expr : null,
				clicked: clicked,
				appliedExpr: appliedExpr
			};
		});

		// ---- Scenario 2 (F3): keyboard-only dialog nav - gear, Enter, Tab, Right x3, End, Home ------------------
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		await page.evaluate(() => {
			const NS = window.JuneauViews;
			NS.persistence = { list: function () { return Promise.resolve({ views: [] }); } };
			const host = document.createElement('div');
			document.body.appendChild(host);
			const table = document.createElement('table');
			table.dataset.juneauView = 'f2browser';
			host.appendChild(table);
			const catalog = [
				{ data: 'name', title: 'Name', search: { type: 'text' }, orderable: true },
				{ data: 'status', title: 'Status', search: { type: 'id' }, orderable: true }
			];
			const ctx = { viewDef: { columns: catalog, columnConfig: {} } };
			NS.config.mountChooser(table, ctx);
		});
		await page.focus(GEAR);
		await page.keyboard.press('Enter');
		await page.keyboard.press('Tab');
		const afterTab = await page.evaluate(DIALOG_STATE);
		await page.keyboard.press('ArrowRight');
		const afterRight1 = await page.evaluate(DIALOG_STATE);
		await page.keyboard.press('ArrowRight');
		const afterRight2 = await page.evaluate(DIALOG_STATE);
		await page.keyboard.press('ArrowRight');
		const afterRight3 = await page.evaluate(DIALOG_STATE);
		await page.keyboard.press('End');
		const afterEnd = await page.evaluate(DIALOG_STATE);
		await page.keyboard.press('Home');
		const afterHome = await page.evaluate(DIALOG_STATE);
		const f2 = { afterTab, afterRight1, afterRight2, afterRight3, afterEnd, afterHome };

		// ---- Scenario 3 (F2): type into the column-search popover, Escape reverts and announces -----------------
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		await page.evaluate(() => {
			const NS = window.JuneauViews;
			const wrapper = document.createElement('div');
			wrapper.className = 'dt-container';
			document.body.appendChild(wrapper);
			const table = document.createElement('table');
			wrapper.appendChild(table);

			const th = document.createElement('th');
			const headRow = document.createElement('tr');
			headRow.appendChild(th);
			const thead = document.createElement('thead');
			thead.appendChild(headRow);
			table.appendChild(thead);
			const flex = document.createElement('div');
			flex.className = 'dt-column-header';
			const titleSpan = document.createElement('span');
			titleSpan.className = 'dt-column-title';
			titleSpan.textContent = 'Status';
			flex.appendChild(titleSpan);
			th.appendChild(flex);

			const icon = document.createElement('span');
			icon.setAttribute('role', 'button');
			icon.setAttribute('tabindex', '0');
			th.appendChild(icon);

			let _search = '';
			const col = {
				index: function () { return 0; },
				header: function () { return th; },
				search: function (v) {
					if (arguments.length) { _search = v == null ? '' : String(v); return this; }
					return _search;
				},
				draw: function () { return this; }
			};
			NS.init.openColumnSearchPopover(icon, col, {}, table);
			window.__f3Icon = icon;
			window.__f3Table = table;
		});
		await page.keyboard.type('abc');
		await page.keyboard.press('Escape');
		await page.evaluate(() => new Promise(function (r) { setTimeout(r, 50); }));
		const f3 = await page.evaluate(() => {
			const icon = window.__f3Icon;
			const table = window.__f3Table;
			const announcer = table.__juneauAnnouncerEl;
			return {
				popoverRemoved: !document.querySelector('.juneau-view-col-search-popover'),
				focusReturnedToIcon: document.activeElement === icon,
				announcerText: announcer ? announcer.textContent : null
			};
		});

		// ---- Scenario 4 (Q1): a version-mismatched blob survives a reload, notices once, never twice ---------
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		await page.evaluate(() => {
			window.JuneauViews.pageState.table('f4browser').set('viewSettings', { schemaVersion: window.JuneauViews.config.CURRENT_SCHEMA_VERSION + 1, visible: ['name'] });
		});
		await page.reload();
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const openOnce = async () => page.evaluate(() => {
			const NS = window.JuneauViews;
			NS.persistence = { list: function () { return Promise.resolve({ views: [] }); } };
			const table = document.createElement('table');
			table.dataset.juneauView = 'f4browser';
			document.body.appendChild(table);
			const catalog = [
				{ data: 'name', title: 'Name', search: { type: 'text' }, orderable: true },
				{ data: 'status', title: 'Status', search: { type: 'id' }, orderable: true }
			];
			const ctx = { viewDef: { columns: catalog } };
			NS.config.openChooser(table, ctx);
			const status = document.querySelector('.juneau-config-status');
			return {
				noticeShown: status?.hidden === false,
				noticeText: status ? status.textContent : null
			};
		});
		const firstOpen = await openOnce();
		await page.reload();
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const secondOpen = await openOnce();
		const f4 = { firstOpen, secondOpen };

		// ---- Scenario 5 (Copy-link): a real click copies a real URL and announces it, with a stubbed clipboard ---
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		await page.evaluate(() => {
			const NS = window.JuneauViews;
			window.__f5CopiedUrl = null;
			NS.urlState.buildShareUrl = function (loc) { return loc.href + '#shared'; };
			NS.urlState.copy = function (nav, u) { window.__f5CopiedUrl = u; return Promise.resolve(true); };
			const wrapper = document.createElement('div');
			document.body.appendChild(wrapper);
			const table = document.createElement('table');
			table.dataset.juneauView = 'f5browser';
			wrapper.appendChild(table);
			const ctx = { viewDef: {} };
			NS.init.mountCopyLinkButton(table, ctx);
			window.__f5Table = table;
		});
		await page.click('.juneau-view-copylink-btn');
		await page.evaluate(() => new Promise(function (r) { setTimeout(r, 50); }));
		const f5 = await page.evaluate(() => {
			const table = window.__f5Table;
			const announcer = table.__juneauAnnouncerEl;
			return {
				copiedUrl: window.__f5CopiedUrl,
				copiedHasShared: typeof window.__f5CopiedUrl === 'string' && window.__f5CopiedUrl.endsWith('#shared'),
				announcerText: announcer ? announcer.textContent : null
			};
		});

		const report = { f1, f2, f3, f4, f5, jsFailures: failures.slice() };
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => { process.stderr.write(String(error?.stack || error) + '\n'); process.exit(1); });
