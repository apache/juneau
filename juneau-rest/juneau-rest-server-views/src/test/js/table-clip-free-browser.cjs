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
 * table-clip-free-browser.cjs - real-browser prober for the "popups never clip" contract across a table's scroll
 * boundary.
 *
 * Never runs in a default build.  Driven by TableClipFree_BrowserTest under `mvn -Pjs-tests`; see that class's
 * javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node table-clip-free-browser.cjs <page.html>
 *
 * The matrix is (menu | popover | timestamp) x (DT1 wrap box | DT2 layout cell | nested detail panel).  In each case
 * the anchoring cell is deliberately scrolled half out of its scroll box, and the measurements are MECHANICAL, never
 * visual: the layer is position:fixed, parented to document.body, has no clipping ancestor at all, and SURVIVES a
 * scroll of the region unchanged - same rect, still in the document.  Prints ONE JSON object to stdout; every
 * assertion is in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS?.init;
	const out = { hasInit: !!init, hasRenders: typeof NS?.resolveRenderer === 'function' };
	if (!init) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));
	function drain() { while (init.topLayer()) init.popLayer(); }

	/**
	 * The clip-free criterion, stated as mechanism: walk every ancestor between the layer and the document and
	 * report any that would clip it.  A portalled layer is a child of <body>, so this list must come back EMPTY -
	 * that is a stronger statement than "not inside THAT box", because it also catches a new clipping ancestor.
	 */
	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function clippingAncestors(el) {
		const bad = [];
		let n = el?.parentElement;
		while (n && n !== document.documentElement) {
			const o = getComputedStyle(n);
			if (o.overflow !== 'visible' || o.overflowX !== 'visible' || o.overflowY !== 'visible')
				bad.push((n.tagName || '?').toLowerCase() + '.' + (n.className || ''));
			n = n.parentElement;
		}
		return bad;
	}

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function rectOf(el) {
		const r = el.getBoundingClientRect();
		return { left: Math.round(r.left), top: Math.round(r.top), w: Math.round(r.width), h: Math.round(r.height) };
	}

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function sameRect(a, b) {
		return a.left === b.left && a.top === b.top && a.w === b.w && a.h === b.h;
	}

	/**
	 * Measures one opened layer against its scroll box, then scrolls the box underneath it and measures again.
	 * `retained` is the whole point of the "scrolling retains the layer" half of the contract: the layer neither
	 * moves nor disappears.
	 */
	async function measure(el, box) {
		if (!el) return { opened: false };
		const before = rectOf(el);
		const m = {
			opened: true,
			onBody: el.parentElement === document.body,
			positionFixed: getComputedStyle(el).position === 'fixed',
			escapedScrollBox: !box.contains(el),
			clippingAncestors: clippingAncestors(el),
			hasArea: before.w > 0 && before.h > 0
		};
		// Scroll the region underneath the open layer.
		box.scrollLeft = Math.max(0, box.scrollLeft - 60);
		box.dispatchEvent(new Event('scroll', { bubbles: true }));
		window.dispatchEvent(new Event('scroll', { bubbles: true }));
		await tick();
		m.retainedInDom = document.body.contains(el);
		m.retainedVisible = getComputedStyle(el).display !== 'none';
		m.notRepositioned = sameRect(before, rectOf(el));
		return m;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Fixtures - three scroll boxes, each holding a table far wider than the box, scrolled so the LAST column (which
	// carries the anchor) is half out of view.
	// ---------------------------------------------------------------------------------------------------------------

	/** Builds a wide table whose last cell is the anchor; returns { box, table, tr, td }. */
	function buildScrolled(kind) {
		let box, host = document.createElement('div');
		document.body.appendChild(host);
		const table = document.createElement('table');
		table.style.width = '1400px';
		table.style.tableLayout = 'fixed';
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		tr.dataset.juneauRowId = 'INC-' + kind;
		const filler = document.createElement('td');
		filler.style.width = '1200px';
		filler.textContent = 'wide filler column';
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		td.style.width = '200px';
		tr.appendChild(filler);
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);

		if (kind === 'dt1') {
			// The REAL DT1 path: a .dataTables_wrapper whose <table> the runtime wraps in .juneau-view-table-scroll.
			host.className = 'dataTables_wrapper';
			host.style.width = '240px';
			host.appendChild(table);
			init.ensureTableScroll(table, {});
			box = table.parentElement;
		} else if (kind === 'dt2') {
			// The REAL DT2 path: the flex .dt-layout-cell IS the scroll box (scoped to the table's layout row).
			host.className = 'dt-container';
			host.style.width = '240px';
			const row = document.createElement('div');
			row.className = 'dt-layout-row dt-layout-table';
			const cell = document.createElement('div');
			cell.className = 'dt-layout-cell';
			cell.style.width = '240px';
			cell.appendChild(table);
			row.appendChild(cell);
			host.appendChild(row);
			box = cell;
		} else {
			// A nested table inside a row-detail panel that itself scrolls (445o keeps this wrapper in place).
			host.className = 'juneau-view-detail-panel';
			host.style.width = '240px';
			const nested = document.createElement('div');
			nested.className = 'juneau-view-detail-nested';
			nested.style.width = '240px';
			nested.appendChild(table);
			host.appendChild(nested);
			box = nested;
		}
		box.scrollLeft = box.scrollWidth;   // scroll fully right: the anchor cell is now half out of the box
		return { host: host, box: box, table: table, tr: tr, td: td };
	}

	const viewDef = {
		rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate', confirm: 'Escalate?' }],
		columns: [{ data: 'name', render: { id: 'text', popover: { title: 'More', fields: [{ data: 'name' }] } } }]
	};

	/** Opens the row-action menu from a trigger in the anchor cell. */
	async function openMenu(f) {
		const trigger = document.createElement('button');
		trigger.className = 'juneau-view-action-trigger';
		trigger.textContent = 'Actions';
		f.td.appendChild(trigger);
		init.initRowActions(f.table, viewDef, {});
		trigger.click();
		await tick();
		return document.querySelector('.juneau-view-action-menu');
	}

	/** Opens the shipped cell popover from a trigger in the anchor cell (the real delegated-click path). */
	async function openPopover(f) {
		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'jc-cell-popover-trigger';
		btn.setAttribute('aria-expanded', 'false');
		btn.dataset.juneauPopover = '1';
		btn.dataset.juneauPopoverCol = 'name';
		f.td.appendChild(btn);
		const ctx = { dataTable: { row: function () { return { data: function () { return { name: 'Ada Lovelace' }; } }; } } };
		init.initCellPopover(f.table, ctx, viewDef);
		btn.click();
		await tick();
		const el = document.getElementById('juneau-cell-popover');
		return (el?.style.display !== 'none') ? el : null;
	}

	/** Opens the datetime renderer's timestamp popup by hovering a [data-juneau-ts] host in the anchor cell. */
	async function openTimestamp(f) {
		const host = document.createElement('span');
		host.className = 'juneau-ts';
		host.setAttribute('tabindex', '0');
		host.dataset.juneauTs = '2026-08-25T05:41:00.000Z';
		host.textContent = '2026-08-25T05:41Z';
		f.td.appendChild(host);
		const r = host.getBoundingClientRect();
		host.dispatchEvent(new MouseEvent('mouseover', {
			bubbles: true, clientX: Math.round(r.left) + 2, clientY: Math.round(r.top) + 2
		}));
		await tick();
		const el = document.getElementById('juneau-ts-popup');
		return (el?.style.display !== 'none') ? el : null;
	}

	const openers = { Menu: openMenu, Popover: openPopover, Timestamp: openTimestamp };

	for (const kind of ['dt1', 'dt2', 'nested']) {
		for (const surface of ['Menu', 'Popover', 'Timestamp']) {
			const f = buildScrolled(kind);
			// The box really is a clipping, actually-scrolled region - otherwise the case proves nothing.
			const boxStyle = getComputedStyle(f.box);
			const el = await openers[surface](f);
			const m = await measure(el, f.box);
			m.boxClips = boxStyle.overflowX !== 'visible';
			m.boxActuallyScrolled = f.box.scrollWidth > f.box.clientWidth;
			out[kind + surface] = m;
			drain();
			f.host.remove();
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// The expanded row-detail panel.  DataTables' native child row wraps the panel in a plain <td colspan>, and that
	// cell is a descendant of the .juneau-view-table - so the table's own clip/ellipsis default would cut the whole
	// expanded panel down to one nowrap line unless the runtime opts that cell out.
	// ---------------------------------------------------------------------------------------------------------------
	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	async function checkDetailPanelClip() {
		const table = document.createElement('table');
		table.className = 'juneau-view-table';
		table.dataset.juneauView = 'detail';
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		tr.className = 'juneau-view-detail-row';
		tr.dataset.juneauRowId = 'INC-D';
		const td = document.createElement('td');
		td.textContent = 'row';
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);

		// The row-detail <template> the expander looks for, as a sibling of the table.
		const host = document.createElement('div');
		const tpl = document.createElement('template');
		tpl.dataset.juneauRowDetail = '1';
		tpl.innerHTML = '<div class="juneau-view-detail-section">'
			+ '<div data-juneau-field="title">a long detail body that must be free to wrap onto several lines</div>'
			+ '</div>';
		host.appendChild(table);
		host.appendChild(tpl);
		document.body.appendChild(host);

		// A minimal stand-in for the DataTables row API that reproduces its child-row DOM exactly.
		let shown = false, childTr = null;
		const rowApi = { length: 1, data: function () { return { id: 'INC-D' }; } };
		rowApi.child = function (panel) {
			return { show: function () {
				childTr = document.createElement('tr');
				const cell = document.createElement('td');
				cell.colSpan = 1;
				cell.appendChild(panel);
				childTr.appendChild(cell);
				tr.parentNode.insertBefore(childTr, tr.nextSibling);
				shown = true;
			} };
		};
		rowApi.child.isShown = function () { return shown; };
		rowApi.child.hide = function () { childTr?.remove(); shown = false; };

		const ctx = { dataTable: { row: function () { return rowApi; } } };
		const viewDef = { details: { endpoint: '/detail/{id}' }, columns: [] };
		init.initDetailsExpander(table, ctx, viewDef);
		td.dispatchEvent(new MouseEvent('click', { bubbles: true }));
		await tick();

		const panel = document.querySelector('.juneau-view-detail-panel');
		const cell = panel?.parentElement;
		const s = cell ? getComputedStyle(cell) : null;
		const result = {
			expanded: !!panel,
			hostIsTd: cell?.tagName === 'TD',
			insideViewTable: !!cell?.closest('.juneau-view-table'),
			// The three ways the clip default would maim an expanded panel.
			wraps: (s?.whiteSpace ?? 'nowrap') !== 'nowrap',
			notOverflowHidden: (s?.overflow ?? 'hidden') !== 'hidden',
			noMaxWidthCap: s?.maxWidth === 'none',
			panelHasArea: panel?.getBoundingClientRect().height > 0
		};
		host.remove();
		drain();
		return result;
	}
	out.detailPanelCell = await checkDetailPanelClip();

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node table-clip-free-browser.cjs <page.html>\n'); process.exit(2); }
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
})().catch(e => { process.stderr.write(String(e?.stack || e) + '\n'); process.exit(1); });
