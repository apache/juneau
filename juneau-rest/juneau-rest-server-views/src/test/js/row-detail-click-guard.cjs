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
 * row-detail-click-guard.cjs - always-on Node harness: a click on one of the row's OWN interactive controls (the
 * row-actions "..." trigger, a rendered link, a selection checkbox) must NOT ALSO toggle the row's detail
 * expansion - while a bare click on the row body (including the dedicated chevron cell, which carries no special
 * click handling of its own) still does, per the pinned whole-row-click design (RowDetailsExpander_Wiring_Test's
 * c01 `cursor: pointer` CSS pin and b02's `toggleFn.contains("tr.juneau-view-detail-row")` pin).
 *
 * Previously the row-actions trigger button (built by actionTriggerMarkup) carried no [data-juneau-action]/
 * [data-juneau-safe] attribute for handleDetailActionRefClick/handleDetailSafeCollapseClick (the two existing
 * click-side guards ahead of toggleDetailRow) to recognize, so its click fell through to toggleDetailRow
 * completely unguarded: opening the row-action menu ALSO expanded the row.  A selection checkbox and a rendered
 * link in the row body had the same gap - isInteractiveRowControl closes all three at once.
 *
 *   Usage:  node row-detail-click-guard.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Spies on ctx.dataTable.row(...) - toggleDetailRow's own gate to the actual expand/collapse, called immediately
 * after the guard this test pins - rather than standing up a real DataTables child-row API: whether that gate is
 * even REACHED is exactly what the guard decides, so a call count is the direct, honest signal.  The spy returns
 * `{ length: 0 }`, which short-circuits toggleDetailRow one line later (`if (!row || !row.length) return;`) -
 * this harness only needs to prove the gate was reached, never a real expand.  Paired-control style throughout:
 * every "does not reach the gate" claim below has a sibling proving the SAME fixture shape DOES reach it when the
 * click lands on plain row body instead.
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

	// The dedicated expander-chevron cell: a <span>, not a button/link/input, so the guard must NOT catch it -
	// a click here is an ordinary row-body click, not a click on one of the row's OWN controls.
	const chevronTd = env.el('td');
	chevronTd.className = 'juneau-view-detail-control';
	const glyphs = env.el('span');
	glyphs.className = 'juneau-view-detail-glyphs';
	chevronTd.appendChild(glyphs);
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
	return { table: table, tr: tr, chevron: glyphs, plain: plainTd, trigger: trigger, link: link,
		checkbox: checkbox, calls: calls };
}

/** Clicks `target` through the real table-level delegate (mirrors detail-region-action-isolation.cjs's clickAction). */
function click(fx, target) {
	fx.table.dispatch('click', { target: target });
}

// --- CONTROL: a bare click on plain row body still reaches toggleDetailRow's gate ------------------------------
const fxPlain = buildFixture();
click(fxPlain, fxPlain.plain);
out.control_plainCellClick_reachesGate = fxPlain.calls.length === 1;

// --- CONTROL: the dedicated chevron cell reaches the gate too - it is not a specially-recognized target, just --
// --- an ordinary part of the row body that happens not to be an interactive control -----------------------------
const fxChevron = buildFixture();
click(fxChevron, fxChevron.chevron);
out.chevronClick_reachesGate = fxChevron.calls.length === 1;

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
