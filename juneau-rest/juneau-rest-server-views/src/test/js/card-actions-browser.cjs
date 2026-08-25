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
 * card-actions-browser.cjs - real-browser prober for a card's action menu and a card-hosted view table.
 *
 * Never runs in a default build.  Driven by CardActions_BrowserTest under `mvn -Pjs-tests`; see that class's javadoc
 * and the profile comment in this module's pom.xml.
 *
 *   Usage:  node card-actions-browser.cjs <page.html>
 *
 * The fixture body is the REAL server-emitted markup and the scripts are the REAL served runtimes, which bootstrap
 * themselves on DOMContentLoaded - so what this prober measures is what a user is served.  Inside headless Chromium
 * it puts the card grid in a narrow, scrolled overflow box (the clip geometry a dashboard actually hits) and measures
 * a card action menu escaping that box through the shipped shared layer stack, then measures that a hosted table
 * changing page length or overflowing horizontally never resizes the card or the grid around it.
 *
 * No jQuery/DataTables is provisioned for this profile, so the row sets a page change would draw are synthesized and
 * the runtime's own exposed overflow helper is driven directly - the same technique the other canaries in this
 * module use.  Prints ONE JSON object to stdout; every assertion is in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const views = window.JuneauViews && window.JuneauViews.init;
	const cards = window.JuneauCards && window.JuneauCards.init;
	const chrome = window.JuneauChrome && window.JuneauChrome.init;
	const out = { hasViews: !!views, hasCards: !!cards, hasChrome: !!chrome };
	if (!views || !cards || !chrome) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));
	function drain() { while (views.topLayer()) views.popLayer(); }

	const grid = document.querySelector('[data-juneau-card-grid]');
	const hostCard = document.querySelector('[data-juneau-card-id="c1"]');
	const fieldCard = document.querySelector('[data-juneau-card-id="c2"]');
	out.gridFound = !!grid;
	out.cardsFound = !!hostCard && !!fieldCard;
	if (!grid || !hostCard || !fieldCard) return out;

	// ---- Block A: the served shell - card-scoped action/menu identity and a hosted table shell ----
	{
		const hostedTable = hostCard.querySelector('table[data-juneau-view]');
		const trigger = hostCard.querySelector('[data-juneau-behavior="menu"]');
		const otherTrigger = fieldCard.querySelector('[data-juneau-behavior="menu"]');
		out.served = {
			actionsInHostCard: hostCard.querySelectorAll('[data-juneau-header-action]').length,
			behaviors: Array.prototype.map.call(hostCard.querySelectorAll('[data-juneau-header-action]'),
				function (a) { return a.getAttribute('data-juneau-behavior'); }),
			menuIdHostCard: trigger ? trigger.getAttribute('aria-controls') : null,
			menuIdFieldCard: otherTrigger ? otherTrigger.getAttribute('aria-controls') : null,
			// The hosted table's element id is grid+card qualified; its marker stays the AUTHOR's view id.
			hostedTableId: hostedTable ? hostedTable.getAttribute('id') : null,
			hostedTableMarker: hostedTable ? hostedTable.getAttribute('data-juneau-view') : null,
			hostedSidecarInsideCard: !!hostCard.querySelector('[id="juneau-view:g1:c1:orders"]'),
			// The hosted body brings the table's own data path: no card refresh envelope on that card.
			hostCardRefreshWire: hostCard.getAttribute('data-juneau-card-refresh'),
			// The runtimes wired themselves at DOMContentLoaded - the cards runtime is the owner for a card.
			menuWiredAtBootstrap: trigger ? trigger.getAttribute('data-juneau-menu-wired') : null,
			// The icon host the registry hydrates into is present (the sprite itself needs an http origin, so icon
			// hydration is covered by the always-on Node layer rather than here).
			iconHostPresent: trigger ? trigger.querySelector('.jc-icon') != null : false
		};
	}

	/** Moves the grid into a narrow, scrolled overflow box - the clip geometry a real dashboard hits. */
	function clipGrid() {
		const box = document.createElement('div');
		box.style.overflow = 'auto';
		box.style.width = '240px';
		box.style.height = '120px';
		grid.parentNode.insertBefore(box, grid);
		box.appendChild(grid);
		grid.style.width = '1200px';
		box.scrollLeft = box.scrollWidth;
		box.scrollTop = box.scrollHeight;
		return box;
	}

	const box = clipGrid();

	// ---- Block B: a card action menu escapes the scrolled grid through the SHIPPED shared layer stack ----
	{
		const trigger = hostCard.querySelector('[data-juneau-behavior="menu"]');
		trigger.click();
		await tick();
		const menu = document.getElementById(trigger.getAttribute('aria-controls'));
		const rect = menu ? menu.getBoundingClientRect() : null;
		out.menu = {
			opened: !!menu,
			onLayerStack: !!views.topLayer(),
			layerKind: views.topLayer() ? views.topLayer().kind : null,
			// The 445h portal contract, consumed - this prober builds no portal of its own.
			portalledToBody: menu ? menu.parentElement === document.body : false,
			positionFixed: menu ? menu.style.position === 'fixed' : false,
			escapedScrollBox: menu ? !box.contains(menu) : false,
			escapedCard: menu ? !hostCard.contains(menu) : false,
			displayed: menu ? menu.style.display === 'block' : false,
			withinViewportX: rect ? (rect.left >= -1 && rect.right <= window.innerWidth + 1) : false,
			withinViewportY: rect ? (rect.top >= -1 && rect.bottom <= window.innerHeight + 1) : false,
			ariaExpanded: trigger.getAttribute('aria-expanded'),
			dialogCount: views.dialogLayerCount()
		};
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
		await tick();
		out.menu.closedOnEscape = !views.topLayer();
		out.menu.ariaResetOnEscape = trigger.getAttribute('aria-expanded');
		drain();
	}

	// ---- Block C: two cards, same authored action id, two distinct menus - opening one never opens the other ----
	{
		const t1 = hostCard.querySelector('[data-juneau-behavior="menu"]');
		const t2 = fieldCard.querySelector('[data-juneau-behavior="menu"]');
		t2.click();
		await tick();
		const opened = views.topLayer() ? views.topLayer().el : null;
		out.perCardMenus = {
			distinctListIds: t1.getAttribute('aria-controls') !== t2.getAttribute('aria-controls'),
			openedTheSecondCardsList: opened === document.getElementById(t2.getAttribute('aria-controls')),
			firstCardStillCollapsed: t1.getAttribute('aria-expanded') === 'false'
		};
		drain();
	}

	/** Appends `n` synthetic body rows to the hosted table - the row set a page of that length would draw. */
	function drawRows(table, n, cellWidth) {
		const old = table.querySelector('tbody');
		if (old) old.remove();
		const tbody = document.createElement('tbody');
		for (let i = 0; i < n; i++) {
			const tr = document.createElement('tr');
			['R-' + i, '100'].forEach(function (text, c) {
				const td = document.createElement('td');
				td.textContent = text;
				if (c === 0 && cellWidth) td.style.minWidth = cellWidth;
				tr.appendChild(td);
			});
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		return tbody;
	}

	// ---- Block D: paging + horizontal overflow inside the card never resize the card or the grid ----
	{
		const table = hostCard.querySelector('table[data-juneau-view]');
		const gridBefore = grid.getBoundingClientRect();
		const cardBefore = hostCard.getBoundingClientRect();

		drawRows(table, 10);                         // "page 1" at a 10-row page length
		await tick();
		const gridTenRows = grid.getBoundingClientRect();
		const cardTenRows = hostCard.getBoundingClientRect();

		drawRows(table, 3);                          // a page-length change to 3 rows
		await tick();
		const gridThreeRows = grid.getBoundingClientRect();

		// A sort click on the hosted header must not move the card either (the shell DataTables binds its sort to).
		const th = table.querySelector('thead th');
		if (th) { th.setAttribute('aria-sort', 'ascending'); th.click(); }
		await tick();
		const gridAfterSort = grid.getBoundingClientRect();

		// A very wide cell: the hosted table's own overflow discipline absorbs it instead of widening the grid.
		drawRows(table, 3, '3000px');
		views.ensureTableScroll(table);
		await tick();
		const scrollWrap = table.parentElement;
		const gridAfterWide = grid.getBoundingClientRect();

		out.layout = {
			gridWidthStableOnPage: Math.abs(gridTenRows.width - gridBefore.width) < 1
				&& Math.abs(gridThreeRows.width - gridBefore.width) < 1,
			gridWidthStableOnSort: Math.abs(gridAfterSort.width - gridBefore.width) < 1,
			gridWidthStableOnWideRow: Math.abs(gridAfterWide.width - gridBefore.width) < 1,
			cardWidthStableOnPage: Math.abs(cardTenRows.width - cardBefore.width) < 1,
			// The rows really did render (a no-op draw would make the stability claims vacuous).
			rowsDrawn: table.querySelectorAll('tbody tr').length,
			grewTallerWithRows: gridTenRows.height > gridBefore.height,
			shrankBackOnSmallerPage: gridThreeRows.height < gridTenRows.height,
			// The overflow is owned by the table's own scroll region, inside the card body.
			scrollWrapIsTheRuntimes: !!scrollWrap && scrollWrap.className.indexOf('juneau-view-table-scroll') >= 0,
			scrollWrapInsideCardBody: !!scrollWrap
				&& !!scrollWrap.closest('[data-juneau-card-body]'),
			tableOverflowsItsScrollRegion: !!scrollWrap && scrollWrap.scrollWidth > scrollWrap.clientWidth
		};
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node card-actions-browser.cjs <page.html>\n'); process.exit(2); }
	if (!fs.existsSync(fixture)) throw new Error('fixture not found: ' + fixture);
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
})().catch(e => { process.stderr.write(String((e && e.stack) || e) + '\n'); process.exit(1); });
