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
 * row-detail-click-guard.cjs - always-on Node harness: expand/collapse is chevron-only.
 * A click on the first-column .juneau-view-detail-control (or .juneau-view-detail-toggle) reaches
 * toggleDetailRow's dt.row(tr) gate; a click on plain row body, ID link, row-actions trigger, or
 * selection checkbox must not.
 *
 *   Usage:  node row-detail-click-guard.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Spies on ctx.dataTable.row(...) - toggleDetailRow's own gate to the actual expand/collapse —
 * rather than standing up a real DataTables child-row API.  The spy returns `{ length: 0 }`, which
 * short-circuits toggleDetailRow one line later.  Paired-control style: every "does not reach the
 * gate" claim has a sibling proving the SAME fixture DOES reach it when the click lands on the
 * chevron.
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node row-detail-click-guard.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(I && typeof I.initDetailsExpander === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/**
 * Builds one view table with one detail-expandable row carrying: the dedicated chevron cell (real markup from
 * detailsControlCellMarkup), a plain data cell, the row-actions trigger (real markup from actionTriggerMarkup),
 * a rendered link (as Column.href would paint one), and a selection checkbox (as buildSelectionColumnDef would
 * paint one) - then wires the real click delegation through initDetailsExpander, the production entry point.
 */
function buildFixture() {
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.setAttribute('data-juneau-csrf', 'tok-1');

	// A row-detail <template> must be a SIBLING of `table` or initDetailsExpander early-returns.
	const host = env.el('div');
	const tpl = env.el('template');
	tpl.setAttribute('data-juneau-row-detail', '1');
	host.appendChild(table);
	host.appendChild(tpl);

	const tbody = env.el('tbody');
	table.appendChild(tbody);
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tbody.appendChild(tr);

	// Dedicated expander-chevron cell: a real <button class="juneau-view-detail-toggle"> inside
	// td.juneau-view-detail-control — the only click target that may expand the row.
	const chevronTd = env.el('td');
	chevronTd.className = 'juneau-view-detail-control';
	const toggle = env.el('button');
	toggle.setAttribute('type', 'button');
	toggle.className = 'juneau-view-detail-toggle';
	toggle.setAttribute('aria-label', 'Expand or collapse row');
	toggle.setAttribute('aria-expanded', 'false');
	const glyphs = env.el('span');
	glyphs.className = 'juneau-view-detail-glyphs';
	toggle.appendChild(glyphs);
	chevronTd.appendChild(toggle);
	tr.appendChild(chevronTd);

	const plainTd = env.el('td');
	plainTd.textContent = 'WORK-D0012';
	tr.appendChild(plainTd);

	// The row-actions "..." trigger: a <button>, but with NO data-juneau-action - the exact shape of the
	// reported bug (handleDetailActionRefClick's [data-juneau-action] selector does not see it).
	const actionsTd = env.el('td');
	const trigger = env.el('button');
	trigger.setAttribute('type', 'button');
	trigger.className = 'juneau-view-action-trigger';
	trigger.setAttribute('aria-haspopup', 'menu');
	actionsTd.appendChild(trigger);
	tr.appendChild(actionsTd);

	const linkTd = env.el('td');
	const link = env.el('a');
	link.setAttribute('href', '/rows/1');
	linkTd.appendChild(link);
	tr.appendChild(linkTd);

	const checkboxTd = env.el('td');
	const checkbox = env.el('input');
	checkbox.setAttribute('type', 'checkbox');
	checkbox.className = 'juneau-view-select-checkbox';
	checkboxTd.appendChild(checkbox);
	tr.appendChild(checkboxTd);

	const calls = [];
	const ctx = {
		dataTable: {
			row: function (rowTr) { calls.push(rowTr); return { length: 0 }; }
		}
	};
	const viewDef = { rowActions: [{ id: 'ack', label: 'Ack', endpoint: '/x/ack', method: 'POST' }] };
	I.initDetailsExpander(table, ctx, viewDef);
	return { table: table, tr: tr, chevron: toggle, chevronTd: chevronTd, plain: plainTd, trigger: trigger, link: link,
		checkbox: checkbox, calls: calls };
}

/** Clicks `target` through the real table-level delegate (mirrors detail-region-action-isolation.cjs's clickAction). */
function click(fx, target) {
	fx.table.dispatch('click', { target: target });
}

// --- CONTROL: the dedicated chevron (button or cell) reaches toggleDetailRow's gate -----------------------------
const fxChevron = buildFixture();
click(fxChevron, fxChevron.chevron);
out.chevronClick_reachesGate = fxChevron.calls.length === 1;

const fxChevronTd = buildFixture();
click(fxChevronTd, fxChevronTd.chevronTd);
out.chevronTdClick_reachesGate = fxChevronTd.calls.length === 1;

// --- Plain row body / title cell must NOT expand ----------------------------------------------------------------
const fxPlain = buildFixture();
click(fxPlain, fxPlain.plain);
out.plainCellClick_doesNotReachGate = fxPlain.calls.length === 0;

// --- THE BUG: clicking the row-actions "..." trigger must NOT also toggle the row's detail ---------------------
const fxTrigger = buildFixture();
click(fxTrigger, fxTrigger.trigger);
out.triggerClick_doesNotReachGate = fxTrigger.calls.length === 0;

// --- A rendered link in the row body must not toggle either -----------------------------------------------------
const fxLink = buildFixture();
click(fxLink, fxLink.link);
out.linkClick_doesNotReachGate = fxLink.calls.length === 0;

// --- A selection checkbox in the row body must not toggle either ------------------------------------------------
const fxCheckbox = buildFixture();
click(fxCheckbox, fxCheckbox.checkbox);
out.checkboxClick_doesNotReachGate = fxCheckbox.calls.length === 0;

process.stdout.write(JSON.stringify(out));
