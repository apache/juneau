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
 * ribbon-normalize.cjs - the module's first always-on DOM harness for buildRibbon(...) in juneau-ribbon.js, built
 * to prove the refresh-to-trailing-cluster normalization against real rendered DOM structure rather than source
 * text (the existing wiring canaries in ViewsMixin_Serving_Test only assert substrings that survive this change
 * unchanged).
 *
 * Covers, each as its own case:
 *
 *   1. No refresh action - normalizeRibbon is an identity no-op, and buildRibbon's output is unaffected.
 *   2. Exactly one (ungrouped) refresh - it is removed from its declared position and rendered alone, last, in
 *      its own one-button ".juneau-view-ribbon-group" cluster; the export cluster ahead of it is untouched.
 *   3. Two or more ungrouped refresh actions - ALL of them move, preserving their relative order, into the SAME
 *      trailing cluster (not split across two clusters, not left behind).
 *   4. A refresh action carrying an explicit `group` opts out entirely: it is not moved and does not join the
 *      synthetic `__refresh` cluster - it stays exactly where its neighbours put it.
 *   5. A `divider` left dangling in trailing position by the move (nothing follows it once refresh is gone) is
 *      dropped, not rendered as an empty seam; a NON-trailing divider (still separating two other actions)
 *      survives untouched.
 *
 * Usage:  node ribbon-normalize.cjs <juneau-ribbon.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const ribbonJsPath = process.argv[2];
if (!ribbonJsPath) {
	console.error('usage: node ribbon-normalize.cjs <juneau-ribbon.js>');
	process.exit(2);
}
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

/**
 * Loads a fresh copy of juneau-ribbon.js into its own env/sandbox.  `withExportFeature` installs a minimal
 * jQuery-with-DataTables-Buttons stand-in so the `export` branch of buildRibbon actually resolves and renders
 * its buttons (feature-detection would otherwise degrade export to nothing, per the file's own design).
 */
function loadRibbon(withExportFeature) {
	const env = makeEnv();
	if (withExportFeature)
		env.window.jQuery = { fn: { dataTable: { Buttons: function () {} } } };
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () {},
		setInterval: function () { return 0; }, clearInterval: function () {}
	};
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' });
	return { env: env, NS: env.window.JuneauViews };
}

const out = {};
const first = loadRibbon(false);
out.hasBuild = !!(first.NS && first.NS.ribbon && typeof first.NS.ribbon.build === 'function');
out.hasNormalizeRibbon = !!(first.NS && first.NS.ribbon && typeof first.NS.ribbon.normalizeRibbon === 'function');
if (!out.hasBuild) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/** A minimal RibbonAction-shaped action literal, mirroring the RibbonAction wire shape. */
function action(type, extra) { return Object.assign({ type: type }, extra || {}); }

// ------------------------------------------------------------------------------------------------------------------
// Pure function: normalizeRibbon(actions) - DOM-free, run only when the export exists (pre-fix it does not).
// ------------------------------------------------------------------------------------------------------------------

if (out.hasNormalizeRibbon) {
	const normalizeRibbon = first.NS.ribbon.normalizeRibbon;

	// 1) No refresh action - identity no-op (same array reference back).
	const noRefresh = [action('export', { buttons: ['copy', 'csv'] }), action('columnSearchToggle')];
	out.pure_noRefresh_isSameReference = normalizeRibbon(noRefresh) === noRefresh;

	// 2) Exactly one refresh - moved last, into '__refresh'; the export action itself is untouched.
	const oneRefresh = [action('refresh'), action('export', { buttons: ['copy', 'csv'] })];
	const oneRefreshResult = normalizeRibbon(oneRefresh);
	out.pure_oneRefresh_order = oneRefreshResult.map(function (x) { return x.type; }).join(',');
	out.pure_oneRefresh_lastGroup = oneRefreshResult[oneRefreshResult.length - 1].group;
	out.pure_oneRefresh_exportGroupUnset = oneRefreshResult[0].type === 'export' && oneRefreshResult[0].group == null;

	// 3) Two refresh actions - BOTH move, preserving relative order, into the SAME trailing group.
	const twoRefresh = [
		action('refresh', { title: 'first' }), action('export', { buttons: ['copy'] }), action('refresh', { title: 'second' })
	];
	const twoRefreshResult = normalizeRibbon(twoRefresh);
	out.pure_twoRefresh_order = twoRefreshResult.map(function (x) { return x.type + (x.title ? ':' + x.title : ''); }).join(',');
	out.pure_twoRefresh_bothInRefreshGroup = twoRefreshResult[1].group === '__refresh' && twoRefreshResult[2].group === '__refresh';
	out.pure_twoRefresh_relativeOrderPreserved = twoRefreshResult[1].title === 'first' && twoRefreshResult[2].title === 'second';

	// 4) Explicit group on refresh - opts out entirely: stays in place, group left exactly as declared.
	const explicitGroup = [action('refresh', { group: 'filters' }), action('export', { buttons: ['copy'] })];
	const explicitGroupResult = normalizeRibbon(explicitGroup);
	out.pure_explicitGroup_order = explicitGroupResult.map(function (x) { return x.type; }).join(',');
	out.pure_explicitGroup_groupUnchanged = explicitGroupResult[0].group === 'filters';

	// 5a) Trailing divider dropped: '..., divider(), refresh()' leaves no dangling divider once refresh moves.
	const trailingDivider = [action('export', { buttons: ['copy'] }), action('divider'), action('refresh')];
	const trailingDividerResult = normalizeRibbon(trailingDivider);
	out.pure_trailingDivider_order = trailingDividerResult.map(function (x) { return x.type; }).join(',');
	out.pure_trailingDivider_dropped = trailingDividerResult.every(function (x) { return x.type !== 'divider'; });

	// 5b) A NON-trailing divider (still separating two other actions once refresh is gone) survives untouched.
	const nonTrailingDivider = [action('refresh'), action('divider'), action('export', { buttons: ['copy'] })];
	const nonTrailingDividerResult = normalizeRibbon(nonTrailingDivider);
	out.pure_nonTrailingDivider_order = nonTrailingDividerResult.map(function (x) { return x.type; }).join(',');

	// 6) WORK-J0507 (Foundry WORK-P0063) - resolveButtonIcon('print') resolves via DEFAULT_ICONS to its own "print"
	// key, not the neutral "tune" fallback; and it needs no extra dep (unlike excel/pdf), so it survives
	// resolveExportButtons' feature gate even with jszip/pdfmake both absent, as long as Buttons itself is present.
	out.pure_print_icon = first.NS.ribbon.resolveButtonIcon(null, 'print');
	out.pure_print_resolvedFromAlwaysOnButtons = first.NS.ribbon.resolveExportButtons(
		action('export', { buttons: ['copy', 'print'] }), { buttons: true, jszip: false, pdfmake: false }
	).join(',');

	// 7) WORK-J0507 - resolveButtonIcon('collapse') resolves to the wired "collapse" icon key (no longer purely
	// forward-compatible now that the collapseAll action type dispatches to it).
	out.pure_collapse_icon = first.NS.ribbon.resolveButtonIcon(null, 'collapse');
}

// ------------------------------------------------------------------------------------------------------------------
// DOM behavior: buildRibbon(viewDef, ctx), through the full render path (the real deliverable - the canaries in
// ViewsMixin_Serving_Test only assert source substrings, none of which change shape when this normalizer lands).
// ------------------------------------------------------------------------------------------------------------------

function groupsOf(bar) {
	return bar.childNodes.filter(function (c) { return c.className === 'juneau-view-ribbon-group'; });
}

function dividersOf(bar) {
	return bar.childNodes.filter(function (c) { return c.className === 'juneau-view-ribbon-divider'; });
}

function buildBar(NS, ribbon) {
	return NS.ribbon.build({ ribbon: ribbon }, { dataTable: {}, redraw: function () {} });
}

// Case 1 - no refresh action: unaffected. One export cluster, nothing trailing added.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('export', { buttons: ['copy', 'csv'] })]);
	const groups = groupsOf(bar);
	out.dom_noRefresh_groupCount = groups.length;
	out.dom_noRefresh_onlyGroupButtonCount = groups.length ? groups[0].childNodes.length : -1;
}

// Case 2 - exactly one refresh, declared FIRST (the console's actual today-shape): two groups after the fix,
// exports first, refresh alone and last.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('refresh'), action('export', { buttons: ['copy', 'csv'] })]);
	const groups = groupsOf(bar);
	out.dom_oneRefresh_groupCount = groups.length;
	out.dom_oneRefresh_firstGroupButtonCount = groups.length > 0 ? groups[0].childNodes.length : -1;
	out.dom_oneRefresh_lastGroupButtonCount = groups.length > 0 ? groups[groups.length - 1].childNodes.length : -1;
	const lastGroupBtn = groups.length > 0 ? groups[groups.length - 1].childNodes[0] : null;
	out.dom_oneRefresh_lastGroupIsRefreshGlyph = lastGroupBtn != null && lastGroupBtn.title === 'Refresh';
	out.dom_oneRefresh_lastGroupIsLastChildOfBar = groups.length > 0 && bar.lastElementChild === groups[groups.length - 1];
}

// Case 3 - two refresh actions around an export cluster: both move into ONE trailing group, in relative order.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [
		action('refresh', { title: 'A' }), action('export', { buttons: ['copy'] }), action('refresh', { title: 'B' })
	]);
	const groups = groupsOf(bar);
	out.dom_twoRefresh_groupCount = groups.length;
	const lastGroup = groups.length > 0 ? groups[groups.length - 1] : null;
	out.dom_twoRefresh_lastGroupButtonCount = lastGroup ? lastGroup.childNodes.length : -1;
	out.dom_twoRefresh_lastGroupTitles = lastGroup ? lastGroup.childNodes.map(function (b) { return b.title; }).join(',') : null;
}

// Case 4 - refresh carries an explicit group shared with a neighbour: opts out completely, stays put (here,
// declared FIRST) rather than relocating to the end.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [
		action('refresh', { group: 'filters' }), action('columnSearchToggle', { group: 'filters' }),
		action('export', { buttons: ['copy'] })
	]);
	const groups = groupsOf(bar);
	out.dom_explicitGroup_groupCount = groups.length;
	const firstGroup = groups.length > 0 ? groups[0] : null;
	out.dom_explicitGroup_firstGroupButtonCount = firstGroup ? firstGroup.childNodes.length : -1;
	out.dom_explicitGroup_firstGroupTitles = firstGroup ? firstGroup.childNodes.map(function (b) { return b.title; }).join(',') : null;
	out.dom_explicitGroup_isNotLastChildOfBar = groups.length > 0 && bar.lastElementChild !== groups[0];
}

// Case 5 - a divider immediately before a trailing refresh is dropped (no dangling seam); export cluster and the
// refresh cluster both survive.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('export', { buttons: ['copy'] }), action('divider'), action('refresh')]);
	const groups = groupsOf(bar);
	out.dom_trailingDivider_groupCount = groups.length;
	out.dom_trailingDivider_dividerCount = dividersOf(bar).length;
	out.dom_trailingDivider_lastGroupButtonCount = groups.length > 0 ? groups[groups.length - 1].childNodes.length : -1;
}

// Case 6 (WORK-J0507, Foundry WORK-P0063 toolbar follow-up) - a `print` id in an `export` action's always-on
// `buttons` list (no `optional` feature-gating needed, unlike excel/pdf) renders as its OWN button in the export
// cluster, alongside `copy` - it is not silently dropped for lack of an extra dependency.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('export', { buttons: ['copy', 'print'] })]);
	const groups = groupsOf(bar);
	out.dom_print_groupCount = groups.length;
	out.dom_print_buttonCount = groups.length > 0 ? groups[0].childNodes.length : -1;
	out.dom_print_buttonTitles = groups.length > 0
		? groups[0].childNodes.map(function (b) { return b.title; }).join(',')
		: null;
}

// Case 7 (WORK-J0507) - a `collapseAll` action renders one "Collapse all" button that, on click, calls
// ctx.collapseAllDetailRows() (the juneau-views.js-side wiring) rather than ctx.redraw() or any export path.
{
	const { NS } = loadRibbon(true);
	let collapseAllCalled = 0;
	const bar = NS.ribbon.build({ ribbon: [action('collapseAll')] }, {
		dataTable: {}, redraw: function () {}, collapseAllDetailRows: function () { collapseAllCalled++; }
	});
	const groups = groupsOf(bar);
	out.dom_collapseAll_groupCount = groups.length;
	const btn = groups.length > 0 ? groups[0].childNodes[0] : null;
	out.dom_collapseAll_title = btn ? btn.title : null;
	if (btn) btn.dispatch('click');
	out.dom_collapseAll_clickInvokedHook = collapseAllCalled;
}

process.stdout.write(JSON.stringify(out));
