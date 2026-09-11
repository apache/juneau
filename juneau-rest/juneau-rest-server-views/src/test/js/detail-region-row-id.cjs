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
 * detail-region-row-id.cjs - always-on Node harness for the row-id containment a row-detail region depends on.
 *
 *   Usage:  node detail-region-row-id.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 *
 * THE GAP THIS PINS.  DataTables inserts a child row as a SIBLING `<tr>` of the row it belongs to, never as a
 * descendant of it.  So a detail panel - and every region container cloned into it - sits OUTSIDE the `<tr>` that
 * carries `data-juneau-row-id`.  `juneau-regions.js`'s `readIds` resolves every enclosing identity with
 * `el.closest("[data-juneau-row-id]")`, which means a panel region would read `rowId: null` and
 * `resolveDeclaredUrl` would then refuse the `{id}` substitution outright (or, for an author's own populate doing
 * its own interpolation, build a literal `/.../null/...` path and take a 400 from the server's path-param
 * resolver).  `expandDetailRow` closes that by re-stamping the row's id onto the panel itself.
 *
 * WHY THIS IS A BEHAVIORAL HARNESS AND NOT A SOURCE-SHAPE PIN.  The claim is not "a setAttribute call exists" - it
 * is "a region inside the panel resolves the RIGHT id and fetches the RIGHT url".  That spans two runtimes
 * (`expandDetailRow` in juneau-views.js stamps; `readIds`/`resolveDeclaredUrl` in juneau-regions.js reads), so only
 * running both against one DOM proves they agree.
 *
 * CONTROLS.  Two, because the positive assertion alone would pass for the wrong reasons:
 *   1. `gap_*` proves the containment gap is REAL - the panel is genuinely not a descendant of its row - so the
 *      stamp is load-bearing rather than a redundant restatement of something `closest` would have found anyway.
 *   2. `unstamped_*` re-runs the SAME enrolment against a panel with the stamp removed and shows it degrades
 *      exactly as described.  Without it, a harness whose region never fetched at all would look identical to a
 *      harness where the fix works.
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const regionsJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node detail-region-row-id.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

/**
 * Adds the `classList` surface the shared shim does not model, to the two nodes `expandDetailRow` decorates on its
 * way past: the row `<tr>` (`juneau-view-detail-open`) and the host `<td>` (the cell-wrap opt-out).  Kept local to
 * this harness rather than pushed into views-dom-shim.cjs so no other harness's behavior can shift underneath it.
 */
function withClassList(node) {
	node.classList = {
		add: function (c) {
			if ((' ' + (node.className || '') + ' ').indexOf(' ' + c + ' ') < 0)
				node.className = ((node.className || '') + ' ' + c).trim();
		},
		remove: function (c) {
			node.className = String(node.className || '').split(/\s+/)
				.filter(function (x) { return x && x !== c; }).join(' ');
		},
		contains: function (c) { return (' ' + (node.className || '') + ' ').indexOf(' ' + c + ' ') >= 0; }
	};
	return node;
}

/** The row id under test, and the two urls the panel and its region respectively own. */
const ROW_ID = 's-77';
const DETAIL_URL = '/scripts/{id}';
const REGION_DATA_URL = '/scripts/{id}/source';

/**
 * Builds one view table with a row-detail template whose clone carries a single region container, then expands the
 * row through the REAL production path: `initDetailsExpander`'s delegated click -> `toggleDetailRow` ->
 * `expandDetailRow` -> `NS.regions.enrolIn(panel)`.
 *
 * @param opts.stripStamp Remove the panel's row-id stamp after expansion and re-enrol a fresh region, modelling the
 *                        pre-fix world (control 2).
 */
function buildAndExpand(env, NS, I, opts) {
	opts = opts || {};

	// A row-detail <template> must be a SIBLING of `table` or findRowDetailTemplate returns null.
	const host = env.el('div');
	const table = env.el('table');
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	env.body.appendChild(host);

	const tpl = env.el('template');
	tpl.setAttribute('data-juneau-row-detail', '1');
	tpl.setAttribute('data-juneau-detail-url', DETAIL_URL);
	host.appendChild(tpl);

	// The shim models no DocumentFragment, so the template's clone is a wrapper <div> holding the region rather
	// than a bare fragment.  Immaterial to what is under test: both `closest` (an ancestor walk) and
	// `querySelectorAll` (a descendant walk) see the identical chain either way - the wrapper only adds one link
	// BELOW the panel, and the stamp being probed sits ABOVE it.
	tpl.content = {
		cloneNode: function () {
			const wrap = env.el('div');
			const region = env.el('div');
			region.setAttribute('data-juneau-region', 'source');
			region.setAttribute('data-juneau-region-type', 'row-detail');
			region.setAttribute('data-juneau-region-declared', JSON.stringify({
				contractVersion: '1',
				dataUrl: REGION_DATA_URL
			}));
			wrap.appendChild(region);
			return wrap;
		}
	};

	const tr = withClassList(env.el('tr'));
	tr.className = 'juneau-view-detail-row';
	tr.setAttribute('data-juneau-row-id', ROW_ID);
	// Dedicated first-column chevron cell — expand/collapse is chevron-only.
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);

	// A DataTables row stub whose `child(panel).show()` inserts the panel the way the real one does: in a NEW
	// sibling <tr>, never inside `tr`.  That sibling insertion IS the condition under test, so the stub reproduces
	// it faithfully rather than taking the shortcut of appending into the row.
	const state = { shown: false, panel: null };
	const child = function (panel) {
		state.panel = panel;
		return {
			show: function () {
				const childTr = env.el('tr');
				const childTd = withClassList(env.el('td'));
				childTr.appendChild(childTd);
				childTd.appendChild(panel);
				tbody.appendChild(childTr);
				state.shown = true;
			}
		};
	};
	child.isShown = function () { return state.shown; };

	const ctx = { dataTable: { row: function () { return { length: 1, child: child }; } } };
	I.initDetailsExpander(table, ctx, { id: 'v1' });

	// Click the chevron cell — the production expand gesture after chevron-only expand.
	table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} });

	const panel = state.panel;
	if (panel && opts.stripStamp) {
		panel.removeAttribute('data-juneau-row-id');
		// Re-enrol a SECOND, freshly-added region so enrolIn's per-node idempotence mark does not make this a no-op.
		const region2 = env.el('div');
		region2.setAttribute('data-juneau-region', 'source2');
		region2.setAttribute('data-juneau-region-type', 'row-detail');
		region2.setAttribute('data-juneau-region-declared', JSON.stringify({
			contractVersion: '1',
			dataUrl: REGION_DATA_URL
		}));
		panel.appendChild(region2);
		NS.regions.enrolIn(panel);
	}
	return { table: table, tr: tr, panel: panel };
}

/** Runs one scenario against a FRESH runtime load, returning the fetch log plus the fixture. */
async function run(opts) {
	const { env, NS, I } = load(rendersJsPath, viewsJsPath, regionsJsPath);
	const fetches = [];
	env.setFetch(function (url) {
		fetches.push(String(url));
		// A shape the detail contract accepts, so the panel settles "ok" rather than erroring on the way past.
		return Promise.resolve({
			ok: true, status: 200,
			headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: {} })); }
		});
	});
	const fx = buildAndExpand(env, NS, I, opts);
	await flush(12);
	return { env: env, NS: NS, fx: fx, fetches: fetches };
}

(async function main() {
	// --- THE CLAIM: the panel carries the row's id, and its region resolves and substitutes it ------------------
	const ok = await run({});
	out.hasFixture = !!ok.fx.panel;
	if (!out.hasFixture) { process.stdout.write(JSON.stringify(out)); return; }

	out.panelCarriesRowId = ok.fx.panel.getAttribute('data-juneau-row-id') === ROW_ID;

	// The property every consumer actually depends on, asserted the way they resolve it.
	const region = ok.fx.panel.querySelector('[data-juneau-region]');
	out.regionResolvesRowIdByClosest =
		region.closest('[data-juneau-row-id]')?.getAttribute('data-juneau-row-id') === ROW_ID;

	// End to end: the region's declared `{id}` url was substituted with the REAL id.
	out.regionFetchedSubstitutedUrl = ok.fetches.indexOf('/scripts/' + ROW_ID + '/source') >= 0;
	out.regionNeverFetchedNull = !ok.fetches.some(function (u) { return u.indexOf('/null/') >= 0; });
	out.regionNeverFetchedLiteralTemplate = !ok.fetches.some(function (u) { return u.indexOf('{id}') >= 0; });

	// The panel's own detail GET is substituted too - proof the fixture drove the real expand path, not a stub.
	out.panelFetchedItsOwnDetailUrl = ok.fetches.indexOf('/scripts/' + ROW_ID) >= 0;

	// --- CONTROL 1: the containment gap is real, so the stamp is load-bearing ------------------------------------
	out.gap_panelIsNotInsideTheRowTr = ok.fx.tr.contains(ok.fx.panel) === false;
	out.gap_panelIsASiblingSubtree = ok.fx.panel.closest('tr') !== ok.fx.tr;

	// --- CONTROL 2: with the stamp removed, the SAME enrolment degrades ------------------------------------------
	const bad = await run({ stripStamp: true });
	const region2 = bad.fx.panel.querySelector('[data-juneau-region="source2"]');
	out.unstamped_resolvesNoRowId = region2.closest('[data-juneau-row-id]') === null;
	// `resolveDeclaredUrl` refuses a `{id}` url it cannot substitute, so the un-stamped region issues NO second
	// fetch for the substituted path - the observable difference the stamp makes.
	out.unstamped_neverFetchedSubstitutedUrlTwice =
		bad.fetches.filter(function (u) { return u === '/scripts/' + ROW_ID + '/source'; }).length === 1;

	process.stdout.write(JSON.stringify(out));
})();
