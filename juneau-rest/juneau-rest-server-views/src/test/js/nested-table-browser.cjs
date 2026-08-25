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
 * nested-table-browser.cjs - real-browser prober for a depth-2 nested table's row actions and selection.
 *
 * Never runs in a default build.  Driven by NestedTable_BrowserTest under `mvn -Pjs-tests`; see that class's javadoc
 * and the profile comment in this module's pom.xml.
 *
 *   Usage:  node nested-table-browser.cjs <page.html>
 *
 * The fixture body is the REAL server-emitted markup (ViewTable.of(request, viewDef)), so the shell this prober
 * measures is the shell a user is served - not a restatement of it.  Inside headless Chromium it clones the parent's
 * row-detail <template> into a real detail panel (as the runtime's expand path does), mints per-row identity, and then
 * measures - as a user would experience them - a nested row-action menu opening through the shared layer stack
 * unclipped by a scrolled detail panel, a nested selection round-trip that does not touch the enclosing table's
 * selection, and the absence of any nested column chooser or nested bulk toolbar.  Prints ONE JSON object to stdout;
 * every assertion is in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS && NS.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));
	function drain() { while (init.topLayer()) init.popLayer(); }

	const parentTable = document.querySelector('table[data-juneau-view="alerts"]');
	out.parentTableFound = !!parentTable;
	if (!parentTable) return out;

	// ---- Block A: the served shell - a nested wrapper with a token, selection, and NO parent-only chrome ----
	{
		const tpl = init.findRowDetailTemplate(parentTable);
		const wrap = tpl && tpl.content.querySelector('[data-juneau-nested]');
		const nested = wrap && wrap.querySelector('table[data-juneau-view]');
		out.served = {
			templateFound: !!tpl,
			wrapFound: !!wrap,
			nestedContract: wrap ? wrap.getAttribute('data-juneau-nested-contract') : null,
			nestedViewId: nested ? nested.getAttribute('data-juneau-view') : null,
			// The enclosing response's token, painted onto the nested table by the request-aware emit.
			token: nested ? nested.getAttribute('data-juneau-csrf') : null,
			selectStamped: nested ? nested.getAttribute('data-juneau-select') === '1' : false,
			rowIdField: nested ? nested.getAttribute('data-juneau-row-id-field') : null,
			// Parent-only affordances: the nested shell carries neither a chooser host nor a bulk sidecar.
			nestedChooserHosts: wrap ? wrap.querySelectorAll('.juneau-view-nested-config').length : -1,
			nestedBulkSidecar: !!document.getElementById('juneau-view-bulk:events'),
			parentBulkSidecar: !!document.getElementById('juneau-view-bulk:alerts')
		};
	}

	/** The parent table's tbody, created on first use (this fixture ships no rows; DataTables would draw them). */
	function parentBody() {
		let tbody = parentTable.querySelector('tbody');
		if (!tbody) {
			tbody = document.createElement('tbody');
			parentTable.appendChild(tbody);
		}
		return tbody;
	}

	/**
	 * Expands one parent row the way the runtime does - a child row beneath it holding the cloned row-detail
	 * template - with the panel inside a narrow, scrolled overflow box (the clip geometry a user hits when a detail
	 * panel scrolls).  Returns the live nested table with the two body rows DataTables would have drawn.
	 */
	function openPanel(parentRowId) {
		const tbody = parentBody();
		const dataRow = document.createElement('tr');
		dataRow.setAttribute('data-juneau-row-id', parentRowId);
		const dataCell = document.createElement('td');
		dataCell.textContent = parentRowId;
		dataRow.appendChild(dataCell);
		tbody.appendChild(dataRow);

		const childRow = document.createElement('tr');
		const childCell = document.createElement('td');
		childRow.appendChild(childCell);
		tbody.appendChild(childRow);

		const box = document.createElement('div');
		box.className = 'juneau-view-detail-panel';
		box.style.overflow = 'auto';
		box.style.width = '200px';
		box.style.height = '90px';
		childCell.appendChild(box);

		const panel = document.createElement('div');
		panel.style.width = '1200px';
		box.appendChild(panel);
		panel.appendChild(init.findRowDetailTemplate(parentTable).content.cloneNode(true));
		init.mintNestedIdentity(panel, parentRowId, 2);

		const wrap = panel.querySelector('[data-juneau-nested]');
		const table = wrap.querySelector('table[data-juneau-view]');
		const nestedBody = document.createElement('tbody');
		['E-1', 'E-2'].forEach(function (id) {
			const tr = document.createElement('tr');
			tr.setAttribute('data-juneau-row-id', id);
			const sel = document.createElement('td');
			const cb = document.createElement('input');
			cb.type = 'checkbox';
			cb.className = 'juneau-view-select-checkbox';
			sel.appendChild(cb);
			const when = document.createElement('td');
			when.textContent = id;
			when.style.width = '900px';   // pushes the actions cell to the far right of the scrolled box
			const actions = document.createElement('td');
			actions.className = 'juneau-view-actions-cell';
			const trigger = document.createElement('button');
			trigger.className = 'juneau-view-action-trigger';
			trigger.textContent = 'Actions';
			actions.appendChild(trigger);
			tr.appendChild(sel); tr.appendChild(when); tr.appendChild(actions);
			nestedBody.appendChild(tr);
		});
		table.appendChild(nestedBody);
		box.scrollLeft = box.scrollWidth;
		return {
			box: box, panel: panel, wrap: wrap, table: table,
			remove: function () { dataRow.remove(); childRow.remove(); }
		};
	}

	// The nested view definition the page shipped in its own id-less sidecar.
	function nestedDef(wrap) {
		return JSON.parse(init.findNestedSidecar(wrap, wrap.querySelector('table[data-juneau-view]')
			.getAttribute('data-juneau-view')).textContent);
	}

	// ---- Block B: a nested row-action menu opens through the shared layer stack, unclipped by the scrolled panel ----
	{
		const dom = openPanel('A-1');
		const def = nestedDef(dom.wrap);
		const ctx = { table: dom.table, viewDef: def, selectionState: null };
		init.initRowActions(dom.table, def, ctx);
		dom.table.querySelector('.juneau-view-action-trigger').click();
		await tick();
		const menu = document.querySelector('.juneau-view-action-menu');
		const rect = menu ? menu.getBoundingClientRect() : null;
		out.menu = {
			shippedRowActions: Array.isArray(def.rowActions) ? def.rowActions.length : -1,
			opened: !!menu,
			// The shipped shared layer stack owns it - this prober never builds a portal of its own.
			onLayerStack: !!init.topLayer(),
			portalledToBody: menu ? menu.parentElement === document.body : false,
			positionFixed: menu ? menu.style.position === 'fixed' : false,
			escapedScrollBox: menu ? !dom.box.contains(menu) : false,
			withinViewportX: rect ? (rect.left >= -1 && rect.right <= window.innerWidth + 1) : false,
			withinViewportY: rect ? (rect.top >= -1 && rect.bottom <= window.innerHeight + 1) : false
		};
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
		await tick();
		out.menu.closedOnEscape = !document.querySelector('.juneau-view-action-menu');
		drain();
		dom.remove();
	}

	// ---- Block C: a nested selection round-trip that never reaches the enclosing table's selection ----
	{
		const dom = openPanel('A-2');
		const def = nestedDef(dom.wrap);
		const nestedCtx = {
			table: dom.table,
			viewDef: def,
			selectionState: { selected: new Set(), rowIdField: dom.table.getAttribute('data-juneau-row-id-field') }
		};
		init.initSelection(dom.table, nestedCtx);
		// The enclosing table's own selection, bound the same way - it must stay empty throughout.
		const parentCtx = { table: parentTable, viewDef: {}, selectionState: { selected: new Set(), rowIdField: 'id' } };
		init.initSelection(parentTable, parentCtx);

		const rows = dom.table.querySelectorAll('tbody tr[data-juneau-row-id]');
		const first = rows[0].querySelector('.juneau-view-select-checkbox');
		first.checked = true;
		first.dispatchEvent(new Event('change', { bubbles: true }));
		const afterCheck = Array.from(nestedCtx.selectionState.selected);

		first.checked = false;
		first.dispatchEvent(new Event('change', { bubbles: true }));
		const afterUncheck = nestedCtx.selectionState.selected.size;

		out.selection = {
			hasSelection: init.hasSelection(dom.table),
			rowIdField: nestedCtx.selectionState.rowIdField,
			afterCheck: afterCheck,
			afterUncheck: afterUncheck,
			parentUntouched: parentCtx.selectionState.selected.size === 0,
			// Its own rows, not the enclosing table's, and not vice versa.
			nestedOwnRows: init.ownRowsWithId(dom.table).length,
			parentOwnRows: init.ownRowsWithId(parentTable).length,
			// What an unguarded descendant query would have swept up: the parent's row PLUS the nested table's rows.
			bareDescendantRows: parentTable.querySelectorAll('tbody tr[data-juneau-row-id]').length,
			parentOwnRowsExcludeNested: init.ownRowsWithId(parentTable).every(function (tr) {
				return !dom.table.contains(tr);
			})
		};

		// No nested column chooser and no nested bulk toolbar in the open panel.
		out.parentOnlyChrome = {
			chooserHosts: dom.panel.querySelectorAll('.juneau-view-config-chooser, .juneau-view-nested-config').length,
			chooserTriggers: dom.panel.querySelectorAll('.juneau-view-config-trigger').length,
			bulkToolbars: dom.panel.querySelectorAll('.juneau-view-bulk-toolbar').length,
			// readBulkDef resolves off the table id: the nested view has no sidecar of its own, the enclosing one does.
			bulkDefOnNested: init.readBulkDef('events') === null,
			bulkDefOnParent: init.readBulkDef('alerts') !== null,
			selectAllCheckboxes: dom.panel.querySelectorAll('.juneau-view-select-all-checkbox').length
		};
		drain();
		dom.remove();
	}

	// ---- Block D: two rows open at once carry distinct minted DOM identity (a bare id lookup cannot cross-wire) ----
	{
		const a = openPanel('A-3');
		const b = openPanel('A-4');
		out.identity = {
			idA: a.table.getAttribute('id'),
			idB: b.table.getAttribute('id'),
			unique: a.table.getAttribute('id') !== b.table.getAttribute('id'),
			// Neither clone answers a page-level lookup: the id that resolves belongs to a root table, not a clone.
			barePageSidecarIsAClone: !!document.getElementById('juneau-view:events')
				&& !!document.getElementById('juneau-view:events').closest('[data-juneau-nested]'),
			authorIdKept: a.table.getAttribute('data-juneau-view') === b.table.getAttribute('data-juneau-view')
		};
		a.remove();
		b.remove();
	}

	// ---- Block E: a ROOT table sharing the nested view's author id does not cross-wire with the nested clone ----
	{
		const pageSibling = Array.prototype.filter.call(
			document.querySelectorAll('table[data-juneau-view="events"]'),
			function (t) { return !t.closest('[data-juneau-nested]'); })[0];
		const dom = openPanel('A-5');
		const pageRow = document.createElement('tr');
		pageRow.setAttribute('data-juneau-row-id', 'E-1');   // the SAME row id the nested table uses
		pageRow.appendChild(document.createElement('td'));
		const pageBody = document.createElement('tbody');
		pageBody.appendChild(pageRow);
		pageSibling.appendChild(pageBody);

		out.pageSibling = {
			found: !!pageSibling,
			// The page sibling keeps the page-level sidecar id; the nested clone was minted away from it.
			pageSidecarIsTheRootOne: document.getElementById('juneau-view:events') !== null
				&& !document.getElementById('juneau-view:events').closest('[data-juneau-nested]'),
			// A panel-scoped lookup still finds the nested clone's own sidecar, not the page sibling's.
			nestedSidecarIsItsOwn: dom.wrap.contains(init.findNestedSidecar(dom.wrap, 'events')),
			nestedTableIsNotThePageSibling: dom.table !== pageSibling,
			// Row identity: same author id, same row id, different owning tables.
			rowOwnerIsNested: init.ownRowsWithId(dom.table).every(function (tr) { return !pageSibling.contains(tr); }),
			pageSiblingOwnRows: init.ownRowsWithId(pageSibling).length,
			nestedRowsExcludedFromPageSibling: init.ownRowsWithId(pageSibling).every(function (tr) {
				return !dom.table.contains(tr);
			})
		};
		pageBody.remove();
		dom.remove();
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node nested-table-browser.cjs <page.html>\n'); process.exit(2); }
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
