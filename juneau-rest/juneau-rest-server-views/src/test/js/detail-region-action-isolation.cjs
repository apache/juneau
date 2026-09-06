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
 * detail-region-action-isolation.cjs - always-on Node harness for design test 35a (§13.6): a POPULATE'S OWN
 * BUTTONS DO NOT REACH `submitRowAction`.
 *
 *   Usage:  node detail-region-action-isolation.cjs <juneau-renders.js> <juneau-views.js>
 *
 * `handleDetailActionRefClick` is bound at the TABLE level and matches `[data-juneau-action]` ANYWHERE beneath it.
 * A region container inside a detail panel is beneath it.  So without an explicit exclusion, a populate that
 * paints its own action button - the most ordinary thing a populate does - has that click silently routed into the
 * framework's write path: a POST with no dialog seam, no confirmation, no idempotency key, and a `targetId` taken
 * from whatever row the panel happens to belong to.
 *
 * This is the click-side twin of the emit-side negative test, and it is the test that makes the downstream
 * consumer's hand-rolled capture-phase intercept deletable: that intercept exists ONLY to stop this leak, so it
 * cannot be removed on the strength of a source-shape pin - it needs a real click proving the framework stays out.
 *
 * Structure: every claim is paired with a CONTROL that must behave the OPPOSITE way through the same fixture
 * builder.  A test asserting only "no fetch happened" passes just as well when the harness is broken and nothing
 * would ever fetch, so each `*_noFetch` below has a sibling proving that same builder DOES fetch when the button
 * is not in a region.
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node detail-region-action-isolation.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: !!(I && typeof I.initDetailsExpander === 'function' && typeof I.submitRowAction === 'function')
};
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function drain() { while (I.topLayer()) I.popLayer(); }

/**
 * Builds one view table with an already-expanded detail panel hosting a single `[data-juneau-action]` button, and
 * wires the real click delegation through `initDetailsExpander` - the production entry point.
 *
 * @param opts.action        The rowAction descriptor the button refers to.
 * @param opts.inRegion      Paint the button inside a region container nested in the panel (a populate's button).
 * @param opts.regionOnButton Put `data-juneau-region` on the BUTTON ITSELF (the self-match bypass probe).
 * @param opts.regionAboveTable Wrap the whole table host in a region, with the button NOT in any panel region -
 *                              i.e. a view that is itself region-hosted, whose framework buttons must still work.
 */
function buildFixture(opts) {
	const action = opts.action;
	const table = env.el('table');
	table.setAttribute('data-juneau-csrf', 'tok-1');

	// A row-detail <template> must be a SIBLING of `table` or initDetailsExpander early-returns.
	const host = env.el('div');
	const tpl = env.el('template');
	tpl.setAttribute('data-juneau-row-detail', '1');
	host.appendChild(table);
	host.appendChild(tpl);

	if (opts.regionAboveTable) {
		const outer = env.el('div');
		outer.setAttribute('data-juneau-region', 'outer-host');
		outer.appendChild(host);
		env.body.appendChild(outer);
	}

	const parentTr = env.el('tr');
	parentTr.dataset.juneauRowId = 'row-42';

	const panel = env.el('div');
	panel.className = 'juneau-view-detail-panel';
	panel._juneauParentTr = parentTr;

	// Attach the panel BENEATH the table, the way a real expanded child row does.  This is load-bearing for the
	// `regionAboveTable` case: `isInRegionSubtree` walks the ANCESTOR chain via closest(), so a detached panel
	// would find no region above it and that case would silently degrade into a duplicate of the plain control.
	const childTr = env.el('tr');
	const childTd = env.el('td');
	childTr.appendChild(childTd);
	childTd.appendChild(panel);
	table.appendChild(childTr);

	const actionBtn = env.el('button');
	actionBtn.setAttribute('data-juneau-action', action.id);
	if (opts.regionOnButton)
		actionBtn.setAttribute('data-juneau-region', 'self-region');

	if (opts.inRegion) {
		const region = env.el('div');
		region.setAttribute('data-juneau-region', 'detail-body');
		region.setAttribute('data-juneau-region-type', 'row-detail');
		region.appendChild(actionBtn);
		panel.appendChild(region);
	} else {
		panel.appendChild(actionBtn);
	}

	const viewDef = { rowActions: [action] };
	const ctx = { dataTable: {} };
	I.initDetailsExpander(table, ctx, viewDef);
	return { table: table, ctx: ctx, panel: panel, actionBtn: actionBtn, parentTr: parentTr };
}

/** Clicks the button through the real table-level delegate, recording whether the framework suppressed the event. */
function clickAction(fx) {
	let prevented = false;
	let stopped = false;
	fx.table.dispatch('click', {
		target: fx.actionBtn,
		preventDefault: function () { prevented = true; },
		stopPropagation: function () { stopped = true; }
	});
	return { prevented: prevented, stopped: stopped };
}

/** Runs one scenario, returning the fetches it produced and whether a dialog opened. */
function run(opts) {
	const fx = buildFixture(opts);
	const fetchCalls = [];
	env.setFetch(function (url, o) {
		fetchCalls.push({ url: url, opts: o });
		return Promise.resolve(jsonResponse({ outcome: 'success' }));
	});
	const flags = clickAction(fx);
	const r = {
		fetches: fetchCalls.length,
		dialogs: I.dialogLayerCount(),
		prevented: flags.prevented,
		stopped: flags.stopped
	};
	drain();
	return r;
}

const PLAIN = { id: 'ack', label: 'Ack', method: 'POST', endpoint: '/rows/{id}/ack' };
const DIALOG = { id: 'esc', present: 'dialog', label: 'Escalate', method: 'POST', endpoint: '/rows/{id}/esc',
	confirm: 'Escalate this row?' };

// --- 35a, THE CLAIM: a populate's button inside a detail-panel region reaches nothing --------------------------
const regionPlain = run({ action: PLAIN, inRegion: true });
out.regionButton_fetches = regionPlain.fetches;
out.regionButton_dialogs = regionPlain.dialogs;

// The framework must not SUPPRESS the event either.  Returning "handled" while still calling
// preventDefault/stopPropagation would kill the author's own listener and the element's native behavior - the
// populate owns this button, so the framework declining to ACT must also mean declining to INTERFERE.
//
// These two flags are the observable proxy for that.  The claim they stand in for - "the populate's own click
// listener still fires" - is NOT directly checkable here, because this module's DOM shim dispatches to listeners on
// the target node only and models no bubbling (views-dom-shim.cjs:222-227), so a listener on the button never sees
// a click dispatched at the table regardless of what the framework does.  Asserting the absence of
// preventDefault/stopPropagation is the honest form: in a real DOM those two calls are the ONLY way this delegate
// could interfere with the author's handler, so their absence is exactly the property that matters.
out.regionButton_notPrevented = regionPlain.prevented === false;
out.regionButton_notStopped = regionPlain.stopped === false;

// --- CONTROL for the above: the SAME builder, button NOT in a region, MUST submit ------------------------------
// Without this, every assertion above would pass against a harness that never fetches at all.
const panelPlain = run({ action: PLAIN, inRegion: false });
out.control_panelButton_fetches = panelPlain.fetches;
out.control_panelButton_prevented = panelPlain.prevented === true;

// --- The dialog path is excluded too, not just the direct-submit path -----------------------------------------
const regionDialog = run({ action: DIALOG, inRegion: true });
out.regionDialogButton_fetches = regionDialog.fetches;
out.regionDialogButton_dialogs = regionDialog.dialogs;

// CONTROL: the same dialog action outside a region DOES open the dialog seam.
const panelDialog = run({ action: DIALOG, inRegion: false });
out.control_panelDialogButton_dialogs = panelDialog.dialogs;

// --- The self-match bypass: a button that ITSELF carries data-juneau-region is still author DOM ---------------
// An earlier form of the guard excluded the self-match, so a populate could escape the exclusion just by putting
// the region marker on its own button.  Both `closest` self-inclusion and this probe exist for that reason.
const selfRegion = run({ action: PLAIN, inRegion: false, regionOnButton: true });
out.selfMarkedRegionButton_fetches = selfRegion.fetches;

// --- The scope bound, in the OTHER direction: a region ABOVE the table must not suppress the panel's own -------
// --- framework-emitted buttons.  A whole view can be region-hosted; that must not disable its detail actions. --
const regionHostedView = run({ action: PLAIN, inRegion: false, regionAboveTable: true });
out.regionHostedView_panelButtonStillFetches = regionHostedView.fetches;
out.regionHostedView_panelButtonStillPrevented = regionHostedView.prevented === true;

process.stdout.write(JSON.stringify(out));
