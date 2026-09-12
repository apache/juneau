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
 * slot-detail-chevron-browser.cjs - real-browser prober for DETAIL_SLOT template.content + chevron-only expand.
 *
 * Never runs in a default build.  Driven by SlotDetailChevron_BrowserTest under `mvn -Pjs-tests`; see that class's
 * javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node slot-detail-chevron-browser.cjs <page.html>
 *
 * Chromium is the subject: buildDetailTemplate must paint into tpl.content (the fragment expandDetailRow clones).
 * template.appendChild leaves that fragment empty in Chromium, which expands to a ~26px blank gap.  The fixture
 * is a ViewSlot envelope mounted through JuneauViews.regions.mount({ table }), so the template under test is the
 * production slot path.  No jQuery/DataTables is provisioned for this profile: after mount, the prober synthesizes
 * one body row with the production chevron markup and a DataTables child-row stand-in, matching TableClipFree.
 * Prints ONE JSON object to stdout; every assertion is in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS?.init;
	const R = NS?.regions;
	const out = {
		hasInit: typeof init?.initTableFromDef === 'function',
		hasMount: typeof R?.mount === 'function',
		hasRenders: typeof NS?.resolveRenderer === 'function'
	};
	if (!out.hasInit || !out.hasMount) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));

	const envelopeEl = document.getElementById('slot-envelope');
	const slot = document.getElementById('gacks');
	out.hasEnvelope = envelopeEl != null && slot != null;
	if (!out.hasEnvelope) return out;
	const envelope = JSON.parse(envelopeEl.textContent);
	await Promise.resolve(R.mount({ gacks: { table: envelope } }));

	const tpl = slot.querySelector('template[data-juneau-row-detail]');
	const dest = tpl && tpl.content ? tpl.content : null;
	out.hasTemplate = tpl != null;
	out.lightDomEmpty = tpl != null && tpl.childNodes.length === 0;
	out.contentChildCount = dest ? dest.childNodes.length : 0;
	out.contentHasHeader = dest != null && dest.querySelector('.juneau-view-detail-header') != null;
	out.contentHasRegion = dest != null && dest.querySelector('[data-juneau-region]') != null;

	const table = slot.querySelector('table[data-juneau-view]');
	out.hasTable = table != null;
	if (!table || typeof init.initDetailsExpander !== 'function' || typeof init.detailsControlCellMarkup !== 'function')
		return out;

	const tbody = document.createElement('tbody');
	const tr = document.createElement('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = 'INC-1';
	const chevronTd = document.createElement('td');
	chevronTd.className = 'juneau-view-detail-control';
	chevronTd.innerHTML = init.detailsControlCellMarkup();
	const nameTd = document.createElement('td');
	nameTd.textContent = 'gack body cell';
	tr.appendChild(chevronTd);
	tr.appendChild(nameTd);
	tbody.appendChild(tr);
	table.appendChild(tbody);

	const toggle = chevronTd.querySelector('.juneau-view-detail-toggle');
	out.hasToggle = toggle != null;

	let shown = false, childTr = null;
	const rowApi = { length: 1, data: function () { return { id: 'INC-1' }; } };
	rowApi.child = function (panel) {
		return { show: function () {
			childTr = document.createElement('tr');
			const cell = document.createElement('td');
			cell.colSpan = 2;
			cell.appendChild(panel);
			childTr.appendChild(cell);
			tr.parentNode.insertBefore(childTr, tr.nextSibling);
			shown = true;
		} };
	};
	rowApi.child.isShown = function () { return shown; };
	rowApi.child.hide = function () { childTr?.remove(); shown = false; };

	const ctx = { dataTable: { row: function () { return rowApi; } } };
	init.initDetailsExpander(table, ctx, envelope.view);

	window.fetch = function () {
		return Promise.resolve({
			ok: true,
			status: 200,
			text: function () {
				return Promise.resolve(JSON.stringify({
					contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION,
					fields: { status: 'OPEN' }
				}));
			}
		});
	};

	nameTd.dispatchEvent(new MouseEvent('click', { bubbles: true }));
	await tick();
	out.bodyClickExpanded = document.querySelector('.juneau-view-detail-panel') != null;

	if (toggle)
		toggle.dispatchEvent(new MouseEvent('click', { bubbles: true }));
	for (let i = 0; i < 30; i++) {
		const p = document.querySelector('.juneau-view-detail-panel');
		if (p && p.dataset.juneauDetailState === 'ok') break;
		await tick();
	}

	const panel = document.querySelector('.juneau-view-detail-panel');
	const header = panel && panel.querySelector('.juneau-view-detail-header');
	out.expanded = !!panel;
	out.clonedHasHeader = !!header;
	out.headerHeight = header ? Math.round(header.getBoundingClientRect().height) : 0;
	out.panelHeight = panel ? Math.round(panel.getBoundingClientRect().height) : 0;
	out.detailState = panel ? (panel.dataset.juneauDetailState || '') : '';
	out.headerText = header ? String(header.textContent || '').trim() : '';
	out.visibleText = panel ? String(panel.innerText || '').trim() : '';
	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node slot-detail-chevron-browser.cjs <page.html>\n'); process.exit(2); }
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
