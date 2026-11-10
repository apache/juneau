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
		env.window.jQuery = { fn: { dataTable: { Buttons: function () { /* no-op */ } } } };
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () { /* no-op */ },
		setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ }
	};
	// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	return { env: env, NS: env.window.JuneauViews };
}

const out = {};
const first = loadRibbon(false);
out.hasBuild = !!(first.NS?.ribbon && typeof first.NS.ribbon.build === 'function');
out.hasNormalizeRibbon = !!(first.NS?.ribbon && typeof first.NS.ribbon.normalizeRibbon === 'function');
if (!out.hasBuild) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/** A minimal RibbonAction-shaped action literal, mirroring the RibbonAction wire shape. */
function action(type, extra) { return { type: type, ...extra }; }

function chromeTip(b) {
	return b ? (b.dataset.jcTip || b.title || '') : '';
}

// ------------------------------------------------------------------------------------------------------------------
// Pure function: normalizeRibbon(actions) - DOM-free, run only when the export exists (pre-fix it does not).
// ------------------------------------------------------------------------------------------------------------------

if (out.hasNormalizeRibbon) {
	const normalizeRibbon = first.NS.ribbon.normalizeRibbon;

	// 1) No refresh action - identity no-op (same array reference back).
	const noRefresh = [action('export', { buttons: ['copy', 'csv'] }), action('collapseAll')];
	out.pure_noRefresh_isSameReference = normalizeRibbon(noRefresh) === noRefresh;

	// 2) Exactly one refresh - moved last, into '__refresh'; the export action itself is untouched.
	const oneRefresh = [action('refresh'), action('export', { buttons: ['copy', 'csv'] })];
	const oneRefreshResult = normalizeRibbon(oneRefresh);
	out.pure_oneRefresh_order = oneRefreshResult.map(function (x) { return x.type; }).join(',');
	out.pure_oneRefresh_lastGroup = oneRefreshResult.at(-1).group;
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

	// 6) resolveButtonIcon('print') resolves via DEFAULT_ICONS to its own "print"
	// key, not the neutral "tune" fallback; and it needs no extra dep (unlike excel/pdf), so it survives
	// resolveExportButtons' feature gate even with jszip/pdfmake both absent, as long as Buttons itself is present.
	out.pure_print_icon = first.NS.ribbon.resolveButtonIcon(null, 'print');
	out.pure_print_resolvedFromAlwaysOnButtons = first.NS.ribbon.resolveExportButtons(
		action('export', { buttons: ['copy', 'print'] }), { buttons: true, jszip: false, pdfmake: false }
	).ok.join(',');

	// 7) resolveButtonIcon('collapse') resolves to the wired "collapse" icon key (no longer purely
	// forward-compatible now that the collapseAll action type dispatches to it).
	out.pure_collapse_icon = first.NS.ribbon.resolveButtonIcon(null, 'collapse');

	// 8) the ninth type is normalizer-neutral: only `refresh` relocates, so a `dialog` action keeps
	// its declared position (here, ahead of the refresh that moves past it) and its group stays unset.
	const withDialog = [action('dialog', { id: 'add-project', title: 'Add project' }), action('refresh')];
	const withDialogResult = normalizeRibbon(withDialog);
	out.pure_dialog_order = withDialogResult.map(function (x) { return x.type; }).join(',');
	out.pure_dialog_groupUnset = withDialogResult[0].group == null;
	out.pure_dialog_actionObjectUnchanged = withDialogResult[0] === withDialog[0];

	// And its glyph is NAMED rather than falling through to the neutral "tune" default, which resolves to the
	// settings gear the column chooser already paints (see the file's own javadoc, and TablePolling e06).
	out.pure_dialog_icon = first.NS.ribbon.resolveButtonIcon(action('dialog'), 'dialog');
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
	return NS.ribbon.build({ ribbon: ribbon }, { hasRowDetail: true, dataTable: {}, redraw: function () { /* no-op */ } });
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
	out.dom_oneRefresh_lastGroupButtonCount = groups.length > 0 ? groups.at(-1).childNodes.length : -1;
	const lastGroupBtn = groups.length > 0 ? groups.at(-1).childNodes[0] : null;
	out.dom_oneRefresh_lastGroupIsRefreshGlyph = lastGroupBtn != null && chromeTip(lastGroupBtn) === 'Refresh';
	out.dom_oneRefresh_lastGroupIsLastChildOfBar = groups.length > 0 && bar.lastElementChild === groups.at(-1);
}

// Case 3 - two refresh actions around an export cluster: both move into ONE trailing group, in relative order.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [
		action('refresh', { title: 'A' }), action('export', { buttons: ['copy'] }), action('refresh', { title: 'B' })
	]);
	const groups = groupsOf(bar);
	out.dom_twoRefresh_groupCount = groups.length;
	const lastGroup = groups.length > 0 ? groups.at(-1) : null;
	out.dom_twoRefresh_lastGroupButtonCount = lastGroup ? lastGroup.childNodes.length : -1;
	out.dom_twoRefresh_lastGroupTitles = lastGroup ? lastGroup.childNodes.map(function (b) { return chromeTip(b); }).join(',') : null;
}

// Case 4 - refresh carries an explicit group shared with a neighbour: opts out completely, stays put (here,
// declared FIRST) rather than relocating to the end.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [
		action('refresh', { group: 'filters' }), action('collapseAll', { group: 'filters' }),
		action('export', { buttons: ['copy'] })
	]);
	const groups = groupsOf(bar);
	out.dom_explicitGroup_groupCount = groups.length;
	const firstGroup = groups.length > 0 ? groups[0] : null;
	out.dom_explicitGroup_firstGroupButtonCount = firstGroup ? firstGroup.childNodes.length : -1;
	out.dom_explicitGroup_firstGroupTitles = firstGroup ? firstGroup.childNodes.map(function (b) { return chromeTip(b); }).join(',') : null;
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
	out.dom_trailingDivider_lastGroupButtonCount = groups.length > 0 ? groups.at(-1).childNodes.length : -1;
}

// Case 6 - a `print` id in an `export` action's always-on
// `buttons` list (no `optional` feature-gating needed, unlike excel/pdf) renders as its OWN button in the export
// cluster, alongside `copy` - it is not silently dropped for lack of an extra dependency.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('export', { buttons: ['copy', 'print'] })]);
	const groups = groupsOf(bar);
	out.dom_print_groupCount = groups.length;
	out.dom_print_buttonCount = groups.length > 0 ? groups[0].childNodes.length : -1;
	out.dom_print_buttonTitles = groups.length > 0
		? groups[0].childNodes.map(function (b) { return chromeTip(b); }).join(',')
		: null;
}

// Case 7 - a `collapseAll` action renders one "Collapse all" button that, on click, calls
// ctx.collapseAllDetailRows() (the juneau-views.js-side wiring) rather than ctx.redraw() or any export path.
{
	const { NS } = loadRibbon(true);
	let collapseAllCalled = 0;
	const bar = NS.ribbon.build({ ribbon: [action('collapseAll')] }, {
		hasRowDetail: true, dataTable: {}, redraw: function () { /* no-op */ }, collapseAllDetailRows: function () { collapseAllCalled++; }
	});
	const groups = groupsOf(bar);
	out.dom_collapseAll_groupCount = groups.length;
	const btn = groups.length > 0 ? groups[0].childNodes[0] : null;
	out.dom_collapseAll_title = btn ? chromeTip(btn) : null;
	if (btn) btn.dispatch('click');
	out.dom_collapseAll_clickInvokedHook = collapseAllCalled;
}

// Case 8 - a `dialog` action renders one icon button that hands its id to the VIEW runtime's
// ribbon-catalog resolver.  This harness loads the ribbon runtime alone, which is the point: the hop is
// optional-chained, so it can be observed with a stand-in and, when the view runtime is absent entirely, the
// button must be inert rather than throwing on click.  (The full seam, with both runtimes loaded, is
// ribbon-dialog.cjs.)
{
	const { env, NS } = loadRibbon(true);
	const calls = [];
	NS.init = { openRibbonDialog: function (id, table, ctx) { calls.push({ id: id, table: table, ctx: ctx }); } };
	const table = env.el('table');
	const bar = NS.ribbon.build({ ribbon: [action('dialog', { id: 'add-project', title: 'Add project' })] },
		{ table: table, dataTable: {}, redraw: function () { /* no-op */ } });
	const groups = groupsOf(bar);
	out.dom_dialog_groupCount = groups.length;
	const btn = groups.length > 0 ? groups[0].childNodes[0] : null;
	out.dom_dialog_buttonCount = groups.length > 0 ? groups[0].childNodes.length : -1;
	out.dom_dialog_title = btn ? chromeTip(btn) : null;
	out.dom_dialog_ariaLabel = btn ? btn.getAttribute('aria-label') : null;
	if (btn) btn.dispatch('click');
	out.dom_dialog_clickHandedOffActionId = calls.length === 1 ? calls[0].id : null;
	out.dom_dialog_clickHandedOffTable = calls.length === 1 && calls[0].table === table;
}

// Case 8b - the same button with NO view runtime present: the optional-chained hop makes the click a no-op
// instead of a TypeError, so a page that somehow loaded only the ribbon runtime degrades quietly.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('dialog', { id: 'add-project', title: 'Add project' })]);
	const groups = groupsOf(bar);
	const btn = groups.length > 0 ? groups[0].childNodes[0] : null;
	out.dom_dialog_noViewRuntime_buttonRendered = btn != null;
	let threw = false;
	try { if (btn) btn.dispatch('click'); } catch (error) { threw = true; } // NOSONAR javascript:S2486 -- the thrown/not-thrown outcome is recorded in a flag and asserted on
	out.dom_dialog_noViewRuntime_clickDidNotThrow = ! threw;
}

// Case 8c - a `dialog` action with no title at all names itself by id, so the button is never nameless.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [action('dialog', { id: 'add-project' })]);
	const groups = groupsOf(bar);
	const btn = groups.length > 0 ? groups[0].childNodes[0] : null;
	out.dom_dialog_untitled_nameFallsBackToId = btn != null && chromeTip(btn) === 'add-project';
}

// ------------------------------------------------------------------------------------------------------------------
// Canonical toolbar group order (search|entity|filter|export|viewing|page|status): tagged groups sort among themselves into
// the slots they occupy; other ids keep position; untagged ribbons are unchanged; an ungrouped refresh joins `page`.
// ------------------------------------------------------------------------------------------------------------------
if (out.hasNormalizeRibbon) {
	const normalizeRibbon = first.NS.ribbon.normalizeRibbon;
	const keyOf = function (x) { return (x.group ?? '-') + ':' + (x.title || x.type); };

	// Shuffled tagged declaration - sorted into canonical order, members of a group keep their relative order.
	const shuffled = [
		action('collapseAll', { group: 'page', title: 'p1' }), action('option', { id: 's', group: 'status', title: 's1' }),
		action('option', { id: 'f', group: 'filter', title: 'f1' }), action('option', { id: 'v', group: 'viewing', title: 'v1' }),
		action('option', { id: 'e', group: 'entity', title: 'e1' }), action('option', { id: 'f2', group: 'filter', title: 'f2' })
	];
	out.order_shuffled = normalizeRibbon(shuffled).map(keyOf).join(' ');

	// A group with another id keeps its declared slot; the canonical groups sort around it.
	const mixed = [
		action('option', { id: 'v', group: 'viewing', title: 'v1' }), action('publish', { topic: 't', group: 'custom', title: 'c1' }),
		action('option', { id: 'f', group: 'filter', title: 'f1' })
	];
	out.order_mixed = normalizeRibbon(mixed).map(keyOf).join(' ');

	// Untagged ribbon (no canonical ids, no ungrouped refresh) - identity no-op.
	const untagged = [action('option', { id: 'a', group: 'zeta' }), action('option', { id: 'b', group: 'alpha' })];
	out.order_untagged_isSameReference = normalizeRibbon(untagged) === untagged;

	// Ungrouped refresh joins a `page` group as its LAST member (not the trailing __refresh cluster).
	const withPage = [
		action('refresh'), action('resetDefaults', { group: 'page', title: 'r1' }),
		action('option', { id: 'f', group: 'filter', title: 'f1' })
	];
	out.order_refreshJoinsPage = normalizeRibbon(withPage).map(keyOf).join(' ');

	// An optionGroup carrying a canonical id sorts like any other tagged item.
	const og = [
		action('collapseAll', { group: 'page', title: 'p1' }),
		action('optionGroup', { id: 'phase', group: 'filter', title: 'og', options: [{ id: 'a' }] })
	];
	out.order_optionGroupTagged = normalizeRibbon(og).map(keyOf).join(' ');

	// The export group sits between filter and viewing and stays its own cluster; search sorts first.
	const withExport = [
		action('collapseAll', { group: 'viewing', title: 'v1' }), action('export', { group: 'export', buttons: ['copy'], title: 'x1' }),
		action('option', { id: 'f', group: 'filter', title: 'f1' }), action('search', { id: 'search', group: 'search', title: 's0' })
	];
	out.order_exportBetween = normalizeRibbon(withExport).map(keyOf).join(' ');
	out.order_oldSearchLast = normalizeRibbon([
		action('option', { id: 'e', group: 'entity', title: 'e1' }), action('option', { id: 'f', group: 'filter', title: 'f1' }),
		action('search', { id: 'search', group: 'search', title: 's0' })
	]).map(keyOf).join(' ');
	out.order_unknownStays = normalizeRibbon([
		action('option', { id: 'z', group: 'zeta', title: 'z1' }), action('option', { id: 'v', group: 'viewing', title: 'v1' })
	]).map(keyOf).join(' ');
	out.order_refreshKeepsPage = normalizeRibbon([
		action('refresh', { id: 'refresh', group: 'page', title: 'r1' }), action('export', { group: 'export', buttons: ['copy'], title: 'x1' })
	]).map(keyOf).join(' ');

	out.pure_resetDefaults_icon = first.NS.ribbon.resolveButtonIcon(action('resetDefaults'), 'resetDefaults');
}

// DOM: a tagged optionGroup keeps its own named role=group wrapper (its tag drives ordering only), sitting right
// after the same-id cluster declared before it.
{
	const { NS } = loadRibbon(true);
	const bar = buildBar(NS, [
		action('option', { id: 'mine', group: 'filter', title: 'Mine' }),
		action('optionGroup', { id: 'phase', group: 'filter', title: 'Phase', options: [{ id: 'a', title: 'A' }, { id: 'b', title: 'B' }] })
	]);
	const groups = groupsOf(bar);
	out.dom_optionGroupTagged_groupCount = groups.length;
	out.dom_optionGroupTagged_titles = groups.map(function (g) { return g.childNodes.map(function (b) { return chromeTip(b); }).join('+'); }).join('|');
	out.dom_optionGroupTagged_ownGroupName = groups.length === 2 ? groups[1].getAttribute('aria-label') : null;
}

// DOM: resetDefaults renders "Reset to defaults" with the restore icon and calls NS.config.resetToDefaultsAndApply.
{
	const { env, NS } = loadRibbon(true);
	const calls = [];
	NS.config = { resetToDefaultsAndApply: function (table, ctx) { calls.push({ table: table, ctx: ctx }); } };
	const table = env.el('table');
	const ctx = { table: table, dataTable: {}, redraw: function () { /* no-op */ } };
	const bar = NS.ribbon.build({ columnConfig: true, ribbon: [action('resetDefaults', { group: 'page' })] }, ctx);
	const groups = groupsOf(bar);
	const btn = groups.length ? groups[0].childNodes[0] : null;
	out.dom_resetDefaults_title = btn ? chromeTip(btn) : null;
	if (btn) btn.dispatch('click');
	out.dom_resetDefaults_calledWithTableAndCtx = calls.length === 1 && calls[0].table === table && calls[0].ctx === ctx;
}

// DOM: resetDefaults with no config runtime loaded is inert on click rather than throwing.
{
	const { NS } = loadRibbon(true);
	const bar = NS.ribbon.build({ columnConfig: true, ribbon: [action('resetDefaults')] }, { dataTable: {}, redraw: function () { /* no-op */ } });
	const groups = groupsOf(bar);
	const btn = groups.length ? groups[0].childNodes[0] : null;
	let threw = false;
	try { if (btn) btn.dispatch('click'); } catch (error) { threw = true; } // NOSONAR javascript:S2486 -- the thrown/not-thrown outcome is recorded in a flag and asserted on
	out.dom_resetDefaults_noConfig_clickDidNotThrow = btn != null && !threw;
}

// DOM: a declared pausePolling renders ONE pause/play button inside the `status` group (never its own ungrouped
// button), honours `target:`, swaps pause <-> play with aria-pressed, and a view that never polls renders none.
{
	const { NS } = loadRibbon(true);
	NS.icons = { resolveIcon: function (name) { return '<svg data-icon="' + name + '"></svg>'; } };
	const published = [];
	const ctx = { dataTable: {}, redraw: function () { /* no-op */ }, bus: { cardId: 'a', publish: function (t, c) { published.push({ topic: t, op: c.op }); }, subscribe: function () { return function () { /* no-op */ }; } } };
	const bar = NS.ribbon.build({ id: 'a', pollIntervalMs: 5000, ribbon: [action('pausePolling', { target: 'b', group: 'page' })] }, ctx);
	const groups = groupsOf(bar);
	out.pp_groupCount = groups.length;
	out.pp_groupId = groups.length ? groups[0].getAttribute('data-juneau-group') : null;
	const btn = groups.length ? groups[0].childNodes[0] : null;
	out.pp_buttonCount = groups.length ? groups[0].childNodes.length : -1;
	out.pp_idleIcon = btn ? /data-icon="([^"]+)"/.exec(btn.innerHTML)?.[1] : null;
	out.pp_idlePressed = btn ? btn.getAttribute('aria-pressed') : null;
	if (btn) btn.dispatch('click');
	out.pp_pausedIcon = btn ? /data-icon="([^"]+)"/.exec(btn.innerHTML)?.[1] : null;
	out.pp_pausedPressed = btn ? btn.getAttribute('aria-pressed') : null;
	if (btn) btn.dispatch('click');
	out.pp_resumedIcon = btn ? /data-icon="([^"]+)"/.exec(btn.innerHTML)?.[1] : null;
	out.pp_resumedPressed = btn ? btn.getAttribute('aria-pressed') : null;
	out.pp_published = published.map(function (x) { return x.topic + ':' + x.op; }).join(',');

	// Own-table pause on a view with no poll interval renders nothing.
	const quiet = NS.ribbon.build({ id: 'a', ribbon: [action('pausePolling')] }, { dataTable: {}, redraw: function () { /* no-op */ } });
	out.pp_nonPollingRendersNothing = quiet == null || groupsOf(quiet).length === 0;

	// normalizeRibbon: forces pausePolling into `status` without mutating the declared item, and keeps it there
	// when it is already declared there.
	const declared = [action('pausePolling', { group: 'page' })];
	const normalized = NS.ribbon.normalizeRibbon(declared);
	out.pp_normalizedGroup = normalized[0].group;
	out.pp_declaredUntouched = declared[0].group === 'page';
	const already = [action('pausePolling', { group: 'status' })];
	out.pp_alreadyStatusStays = NS.ribbon.normalizeRibbon(already)[0].group === 'status';
}

// ------------------------------------------------------------------------------------------------------------------
// Pure functions: ribbonQueryParams + ribbonColumnSearches - the browser JOINS multiple active toggles that target
// the SAME single-string parameter (`search`) into ONE comma-separated clause string (the wire
// carries ONE `search`, never repeated params).  A column-scoped option stays a native
// columns[N][search][value] param; any other custom param stays single-valued (last contribution wins).
// ------------------------------------------------------------------------------------------------------------------
if (first.NS?.ribbon && typeof first.NS.ribbon.ribbonQueryParams === 'function') {
	// The flat form (query params plus one columns[N][search][value] entry per column search), composed from the split API.
	const toQuery = function (viewDef, activeState, optsColumns) {
		const q = first.NS.ribbon.ribbonQueryParams(viewDef, activeState);
		const cs = first.NS.ribbon.ribbonColumnSearches(viewDef, activeState, optsColumns);
		Object.keys(cs).forEach(function (k) { q['columns[' + k + '][search][value]'] = cs[k]; });
		return q;
	};
	const viewDef = {
		columns: [{ data: 'status' }, { data: 'owner' }],
		ribbon: [
			{ type: 'option', id: 'onlyOpen', param: 'search', value: 'status=$eq(OPEN)' },
			{ type: 'option', id: 'mine', param: 'search', value: 'owner=$eq(me)' },
			{ type: 'option', id: 'withCounts', param: 'opt', value: 'counts=true' },
			{ type: 'optionGroup', id: 'density', options: [
				{ id: 'compact', param: 'opt', value: 'density=compact' },
				{ id: 'roomy', param: 'opt', value: 'density=roomy' }
			] },
			{ type: 'option', id: 'colScoped', column: 'status', value: '$eq(OPEN)' }
		]
	};
	// Two active `search`-param toggles + one `opt` option + the selected `opt` group member: `search` joins into ONE
	// string, `opt` is single-valued (last wins); the column-scoped toggle stays a native per-index param.
	const joined = toQuery(viewDef, { onlyOpen: true, mine: true, withCounts: true, density: 'compact', colScoped: true });
	out.pure_joinSearchClauses = joined.search;                            // "status=$eq(OPEN),owner=$eq(me)"
	out.pure_joinOptClauses = joined.opt;                                  // "density=compact" (opt is single-valued: last wins)
	out.pure_joinColumnStillNative = joined['columns[0][search][value]'];  // "$eq(OPEN)" - unaffected by the join

	// One active `search` toggle -> the single clause, with no leading/trailing separator comma.
	out.pure_joinSingleSearchNoComma = toQuery(viewDef, { onlyOpen: true }).search;

	// The JOIN only inserts a TOP-LEVEL comma: a comma INSIDE a clause value's $in(...) belongs to that clause.
	const parenView = { columns: [], ribbon: [
		{ type: 'option', id: 'a', param: 'search', value: 'status=$in(OPEN,CLOSED)' },
		{ type: 'option', id: 'b', param: 'search', value: 'tier=$eq(gold)' }
	] };
	out.pure_joinProtectsInnerCommas = toQuery(parenView, { a: true, b: true }).search;
}

// ------------------------------------------------------------------------------------------------------------------
// Pure functions: ribbonColumnSearches / ribbonQueryParams / mergeColumnSearches.  Column-scoped options become
// per-dtIndex $-expressions (same column twice -> $and); param-scoped options become query params; user and
// ribbon searches on one column merge to $and(user,ribbon).
// ------------------------------------------------------------------------------------------------------------------
if (first.NS?.ribbon && typeof first.NS.ribbon.ribbonColumnSearches === 'function') {
	const R = first.NS.ribbon;
	const vd = {
		id: 'split',
		columns: [{ data: 'id' }, { data: 'status' }, { data: 'phase' }],
		ribbon: [
			{ type: 'option', id: 'dropped', column: 'status', value: '$eq(DROPPED)' },
			{ type: 'option', id: 'early', column: 'phase', value: '$in(Waiting,"Ready to push")' },
			{ type: 'option', id: 'notWaiting', column: 'phase', value: '$ne(Waiting)' },
			{ type: 'option', id: 'mine', param: 'owner', value: 'me' }
		]
	};
	const all = { dropped: true, early: true, notWaiting: true, mine: true };
	out.split_columnSearches = R.ribbonColumnSearches(vd, all, null);
	out.split_queryParams = R.ribbonQueryParams(vd, all);
	out.split_merged = R.mergeColumnSearches({ '1': '$eq(ACTIVE)', '0': '  ' }, { '1': '$eq(DROPPED)', '2': '$ne(Waiting)' });
	out.split_wrapperRemoved = (R.ribbonToQueryParams === undefined);
}

// ------------------------------------------------------------------------------------------------------------------
// Claiming items: search slot, link button, column settings; visibility rules; ids on rendered buttons.
// ------------------------------------------------------------------------------------------------------------------
function flat(bar) {
	const all = [];
	(function walk(n) { n.childNodes.forEach(function (c) { if (c.nodeType === 1) { all.push(c); walk(c); } }); })(bar);
	return all;
}
function ribbonIds(bar) {
	return flat(bar).filter(function (c) { return c.getAttribute('data-juneau-ribbon-id') != null; })
		.map(function (c) { return c.getAttribute('data-juneau-ribbon-id'); }).join(',');
}
function claimEnv() {
	const l = loadRibbon(true);
	const NS = l.NS, env = l.env;
	NS.icons = { resolveIcon: function (name) { return '<svg data-icon="' + name + '"></svg>'; } };
	NS.urlState = { buildShareUrl: function () { return 'u'; } };
	const log = { link: 0, opened: 0, seeded: 0, chooserIcon: null };
	NS.init = {
		buildCopyLinkButton: function (table, ctx) {
			if ((ctx.viewDef && ctx.viewDef.copyLink === false) || !NS.urlState) return null;
			const b = env.el('button');
			b.className = 'juneau-view-ribbon-btn juneau-view-copylink-btn';
			b.addEventListener('click', function () { log.link++; });
			return b;
		}
	};
	NS.config = {
		seedChooserDraft: function () { log.seeded++; },
		openChooser: function () { log.opened++; },
		buildChooserButton: function (table, ctx, icon) {
			if (!ctx.viewDef || !ctx.viewDef.columnConfig) return null;
			log.chooserIcon = icon;
			const b = env.el('button');
			b.className = 'juneau-view-ribbon-btn juneau-config-chooser-btn';
			b.addEventListener('click', function () { NS.config.openChooser(table, ctx); });
			return b;
		},
		resetToDefaultsAndApply: function () { /* no-op */ }
	};
	return { env: env, NS: NS, log: log };
}
{
	const { env, NS } = claimEnv();
	const ctx = { table: env.el('table'), dataTable: {}, redraw: function () { /* no-op */ } };
	const bar = NS.ribbon.build({ ribbon: [action('search', { id: 'search', group: 'search' })] }, ctx);
	const slot = flat(bar).find(function (c) { return c.getAttribute('data-juneau-slot') === 'search'; });
	out.claim_search_slot = !!slot && slot.className === 'juneau-view-ribbon-search-slot';
	out.claim_search_group = slot ? slot.parentNode.getAttribute('data-juneau-group') : null;
	out.claim_search_ribbonId = slot ? slot.getAttribute('data-juneau-ribbon-id') : null;
	out.claim_search_owns = ctx._ribbonOwns.search === true;
}
{
	const { env, NS, log } = claimEnv();
	const ctx = { table: env.el('table'), dataTable: {}, viewDef: {}, redraw: function () { /* no-op */ } };
	const bar = NS.ribbon.build({ ribbon: [action('linkButton', { id: 'link', group: 'export' })] }, ctx);
	const b = flat(bar).find(function (c) { return /juneau-view-copylink-btn/.test(c.className); });
	out.claim_link_rendered = !!b;
	out.claim_link_ribbonId = b ? b.getAttribute('data-juneau-ribbon-id') : null;
	if (b) b.dispatch('click');
	out.claim_link_clicked = log.link;
	out.claim_link_owns = ctx._ribbonOwns.linkButton === true;
	const ctx2 = { table: env.el('table'), dataTable: {}, viewDef: { copyLink: false }, redraw: function () { /* no-op */ } };
	const bar2 = NS.ribbon.build({ ribbon: [action('linkButton', { id: 'link' })] }, ctx2);
	out.claim_link_hidden = !bar2 || !flat(bar2).some(function (c) { return /copylink/.test(c.className); });
	out.claim_link_hidden_noOwn = !ctx2._ribbonOwns.linkButton;
	NS.urlState = undefined;
	const ctx3 = { table: env.el('table'), dataTable: {}, viewDef: {}, redraw: function () { /* no-op */ } };
	const bar3 = NS.ribbon.build({ ribbon: [action('linkButton', { id: 'link' })] }, ctx3);
	out.claim_link_noUrlState = !bar3 || !flat(bar3).some(function (c) { return /copylink/.test(c.className); });
}
{
	const { env, NS, log } = claimEnv();
	const ctx = { table: env.el('table'), dataTable: {}, redraw: function () { /* no-op */ } };
	const vd = { columnConfig: true, ribbon: [action('columnSettings', { id: 'columns', group: 'viewing' })] };
	ctx.viewDef = vd;
	const bar = NS.ribbon.build(vd, ctx);
	const b = flat(bar).find(function (c) { return /juneau-config-chooser-btn/.test(c.className); });
	out.claim_cols_rendered = !!b;
	out.claim_cols_ribbonId = b ? b.getAttribute('data-juneau-ribbon-id') : null;
	out.claim_cols_icon = log.chooserIcon;
	if (b) b.dispatch('click');
	out.claim_cols_opened = log.opened;
	out.claim_cols_owns = ctx._ribbonOwns.columnSettings === true;
	const vd2 = { ribbon: [action('columnSettings', { id: 'columns' })] };
	const bar2 = NS.ribbon.build(vd2, { table: env.el('table'), viewDef: vd2, dataTable: {}, redraw: function () { /* no-op */ } });
	out.claim_cols_hiddenNoConfig = !bar2 || !flat(bar2).some(function (c) { return /chooser/.test(c.className); });
	const vd3 = { columnConfig: true, ribbon: [action('columnSettings', { id: 'columns' })] };
	const saved = NS.config.openChooser;
	NS.config.openChooser = undefined;
	NS.config.buildChooserButton = undefined;
	const bar3 = NS.ribbon.build(vd3, { table: env.el('table'), viewDef: vd3, dataTable: {}, redraw: function () { /* no-op */ } });
	out.claim_cols_hiddenNoRuntime = !bar3 || !flat(bar3).some(function (c) { return /chooser/.test(c.className); });
	NS.config.openChooser = saved;
}
{
	const { env, NS } = claimEnv();
	const mk = function (extra) { return Object.assign({ table: env.el('table'), dataTable: {}, redraw: function () { /* no-op */ } }, extra); };
	const has = function (bar, title) { return !!bar && flat(bar).some(function (c) { return chromeTip(c) === title; }); };
	out.vis_collapse_noDetail = !has(NS.ribbon.build({ ribbon: [action('collapseAll', { id: 'collapse' })] }, mk({})), 'Collapse all');
	out.vis_collapse_withDetail = has(NS.ribbon.build({ ribbon: [action('collapseAll', { id: 'collapse' })] }, mk({ hasRowDetail: true })), 'Collapse all');
	const noCfg = { ribbon: [action('resetDefaults', { id: 'reset' })] };
	out.vis_reset_noConfig = !has(NS.ribbon.build(noCfg, mk({ viewDef: noCfg })), 'Reset to defaults');
	const cfg = { columnConfig: true, ribbon: [action('resetDefaults', { id: 'reset' })] };
	out.vis_reset_withConfig = has(NS.ribbon.build(cfg, mk({ viewDef: cfg })), 'Reset to defaults');
	const bare = { ribbon: [action('collapseAll'), action('resetDefaults')] };
	const bareBar = NS.ribbon.build(bare, mk({ viewDef: bare }));
	out.vis_bare_bothHidden = !bareBar || flat(bareBar).length === 0;
	// Ribbon ids on rendered buttons.
	const full = { columnConfig: true, ribbon: [
		action('search', { id: 'search', group: 'search' }),
		action('export', { id: 'export', buttons: ['copy', 'csv'], group: 'export' }),
		action('linkButton', { id: 'link', group: 'export' }),
		action('collapseAll', { id: 'collapse', group: 'viewing' }),
		action('columnSettings', { id: 'columns', group: 'viewing' }),
		action('resetDefaults', { id: 'reset', group: 'viewing' }),
		action('refresh', { id: 'refresh', group: 'page' })
	] };
	const bar = NS.ribbon.build(full, mk({ viewDef: full, hasRowDetail: true, dataTable: { button: function () { return { trigger: function () { /* no-op */ } }; } } }));
	out.ids_all = ribbonIds(bar);
	// Ownership resets per build.
	const ctx = mk({ viewDef: full });
	NS.ribbon.build(full, ctx);
	const slim = { ribbon: [action('refresh')] };
	NS.ribbon.build(slim, ctx);
	out.owns_reset = Object.keys(ctx._ribbonOwns).length === 0;
}

process.stdout.write(JSON.stringify(out));
