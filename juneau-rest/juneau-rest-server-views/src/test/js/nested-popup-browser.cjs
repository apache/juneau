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
 * nested-popup-browser.cjs - real-browser prober for the shared popup layer stack (TODO-445h, h3/h4/h5).
 *
 * Never runs in a default build.  Driven by NestedPopup_BrowserTest under `mvn -Pjs-tests`; see that class's javadoc
 * and the profile comment in this module's pom.xml.
 *
 *   Usage:  node nested-popup-browser.cjs <page.html>
 *
 * Loads <page.html> (the real served juneau-views.js) in headless Chromium, then inside the page measures - as a user
 * would experience them - a dialog opening as a portalled focus-trapping layer, Escape unwinding exactly one layer, an
 * outside click dismissing only a light-dismiss popover (never the modal beneath), a row-action menu portalled to
 * body as position:fixed (so it is not clipped by an overflow ancestor), the dialog-kind depth cap of two, and the
 * timestamp popup NOT being a registered layer.  Prints ONE JSON object to stdout; every assertion is in Java.
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
	function makeRow(rowId) {
		const table = document.createElement('table');
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		if (rowId != null) tr.setAttribute('data-juneau-row-id', rowId);
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, tr: tr, td: td };
	}
	const action = { id: 'ack', label: 'Acknowledge', present: 'dialog' };
	const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate', confirm: 'Escalate?' }] } };

	// ---- Block A: a dialog opens as a portalled, focus-trapping, z-stamped layer on the body ----
	{
		const dom = makeRow('INC-1');
		const ui = init.showActionDialog({ title: 'One' }, action, dom.table, dom.tr, ctx);
		await tick();
		out.dialogLayer = {
			onBody: ui.backdrop.parentElement === document.body,
			positionFixed: ui.backdrop.style.position === 'fixed',
			hasZIndex: !!ui.backdrop.style.zIndex,
			dataLayer0: ui.backdrop.getAttribute('data-juneau-layer') === '0',
			focusTrapped: ui.backdrop.contains(document.activeElement)
		};
		drain();
	}

	// ---- Block B: Escape unwinds exactly ONE layer (inner pops, outer stays) ----
	{
		const dom = makeRow('INC-2');
		const outer = init.showActionDialog({ title: 'Outer' }, action, dom.table, dom.tr, ctx);
		const inner = init.showActionDialog({ title: 'Inner' }, action, dom.table, dom.tr, ctx);
		await tick();
		const before = init.dialogLayerCount();
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
		out.escape = {
			before: before,
			after: init.dialogLayerCount(),
			innerDetached: !document.body.contains(inner.backdrop),
			outerStays: document.body.contains(outer.backdrop)
		};
		drain();
	}

	// ---- Block C: an outside click dismisses only a light-dismiss popover, never the modal beneath ----
	{
		const dom = makeRow('INC-3');
		init.showActionDialog({ title: 'Modal' }, action, dom.table, dom.tr, ctx);
		const pop = document.createElement('div');
		pop.className = 'juneau-view-cell-popover';
		pop.textContent = 'popover';
		init.pushLayer(pop, { kind: 'popover', portal: true, lightDismiss: true, trapFocus: false });
		await tick();
		const outside = document.createElement('div');
		document.body.appendChild(outside);
		outside.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }));
		out.outsideClick = {
			popoverDismissed: !document.body.contains(pop),
			modalSurvives: init.dialogLayerCount() === 1,
			topIsDialogAfter: init.topLayer() && init.topLayer().kind === 'dialog'
		};
		drain();
	}

	// ---- Block D: a row-action menu opens portalled to body as position:fixed (unclipped) and closes on outside click ----
	{
		const dom = makeRow('INC-4');
		const trigger = document.createElement('button');
		trigger.className = 'juneau-view-action-trigger';
		dom.td.appendChild(trigger);
		init.initRowActions(dom.table, ctx.viewDef, {});
		trigger.click();
		await tick();
		const menu = document.querySelector('.juneau-view-action-menu');
		out.menu = {
			opened: !!menu,
			onBody: menu ? menu.parentElement === document.body : false,
			positionFixed: menu ? menu.style.position === 'fixed' : false
		};
		const outside = document.createElement('div');
		document.body.appendChild(outside);
		outside.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }));
		out.menu.closedOnOutsideClick = !document.querySelector('.juneau-view-action-menu');
		drain();
	}

	// ---- Block E: the dialog-kind depth cap is 2 - a third dialog is refused inside the current top dialog ----
	{
		const dom = makeRow('INC-5');
		init.showActionDialog({ title: 'One' }, action, dom.table, dom.tr, ctx);
		init.showActionDialog({ title: 'Two' }, action, dom.table, dom.tr, ctx);
		await tick();
		const atCap = init.dialogLayerCount();
		init.openActionDialog({ id: 'esc', label: 'Escalate', confirm: 'Escalate?', present: 'dialog' }, dom.table, dom.tr, ctx);
		await tick();
		const top = init.topLayer();
		out.depthCap = {
			max: init.MAX_DIALOG_DEPTH,
			atCap: atCap,
			afterThirdPush: init.dialogLayerCount(),
			refusalInTopDialog: !!(top && top.el.querySelector('.juneau-view-dialog-depth-refusal'))
		};
		drain();
	}

	// ---- Block F: the timestamp popup is a plain show/hide element - NOT a registered layer (SF-3) ----
	{
		const ts = document.createElement('div');
		ts.id = 'juneau-ts-popup';
		ts.style.display = 'block';
		document.body.appendChild(ts);
		out.timestamp = {
			notOnStack: init.topLayer() === null,
			dialogCountZero: init.dialogLayerCount() === 0
		};
		drain();
	}

	// ---- Block G: a menu opened from a LAST-COLUMN trigger inside a scrolled-right .dt-layout-cell is UNCLIPPED ----
	// The concrete clip case: the trigger's cell lives inside a real .dt-layout-cell overflow box
	// (overflow:auto, narrow) whose table is far wider, scrolled fully right.  A menu appendChild'd into that cell
	// would be clipped by the box; portalling it to the body as position:fixed makes it a sibling lineage of the box,
	// so it is measurably outside the box and horizontally within the viewport.
	{
		const box = document.createElement('div');
		box.className = 'dt-layout-cell';
		box.style.overflow = 'auto';
		box.style.width = '160px';
		box.style.height = '120px';
		document.body.appendChild(box);
		const table = document.createElement('table');
		table.style.width = '1200px';       // far wider than the 160px box -> horizontal scroll
		table.style.tableLayout = 'fixed';
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		tr.setAttribute('data-juneau-row-id', 'INC-G');
		const filler = document.createElement('td');   // pushes the actions cell to the far right
		filler.style.width = '1080px';
		filler.textContent = 'wide';
		const actions = document.createElement('td');
		actions.className = 'juneau-view-actions-cell';
		actions.style.width = '120px';
		const trigger = document.createElement('button');
		trigger.className = 'juneau-view-action-trigger';
		trigger.textContent = 'Actions';
		actions.appendChild(trigger);
		tr.appendChild(filler); tr.appendChild(actions);
		tbody.appendChild(tr); table.appendChild(tbody); box.appendChild(table);
		box.scrollLeft = box.scrollWidth;   // scroll fully right so the last-column trigger is at the box's edge
		init.initRowActions(table, ctx.viewDef, {});
		trigger.click();
		await tick();
		const menu = document.querySelector('.juneau-view-action-menu');
		const vw = window.innerWidth, vh = window.innerHeight;
		const rect = menu ? menu.getBoundingClientRect() : null;
		out.scrolledMenu = {
			opened: !!menu,
			onBody: menu ? menu.parentElement === document.body : false,
			escapedScrollBox: menu ? !box.contains(menu) : false,   // NOT a descendant of the overflow clip box
			positionFixed: menu ? menu.style.position === 'fixed' : false,
			// Horizontally within the viewport (the .dt-layout-cell overflow-x axis is the clip axis this proves).
			withinViewportX: rect ? (rect.left >= -1 && rect.right <= vw + 1) : false,
			withinViewportY: rect ? (rect.top >= -1 && rect.bottom <= vh + 1) : false
		};
		drain();
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node nested-popup-browser.cjs <page.html>\n'); process.exit(2); }
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
