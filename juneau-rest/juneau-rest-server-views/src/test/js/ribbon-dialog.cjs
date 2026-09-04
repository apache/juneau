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
 * ribbon-dialog.cjs - always-on Node harness for the ROW-LESS (ribbon-hosted) dialog seam (WORK-J0512 §2a): a
 * `RibbonAction.dialog(...)` opened from a view's ribbon, with NO `<tr>` behind it.
 *
 * Loads BOTH runtimes into ONE window, because the seam spans them: juneau-ribbon.js renders the trigger and hops
 * to juneau-views.js's ribbon-catalog resolver (NS.init.openRibbonDialog), which reuses the same openActionDialog
 * -> showActionDialog -> submitActionDialog -> submitRowAction -> settleActionResponse path a row dialog uses.
 *
 * Covers, each as its own case:
 *
 *   1. A ribbon `dialog` click opens the dialog with no row, and its title comes from the widened name read
 *      (label, then `title`, then id) rather than falling through to the raw id.
 *   2. A confirmed submit settles into the RIBBON-ANCHORED host, not a row's actions cell (there isn't one).
 *   3. The OPEN path's failures land in the same host: a non-2xx form GET (transport refusal) and an unparseable
 *      body (a named refusal) - the two calls that made a ribbon click die on open-failure, before the submit
 *      path was ever reached.
 *   4. A pre-flight submit refusal (no CSRF token on the table) lands there too, naming the action.
 *   5. Catalog isolation: a ribbon dialog action resolves from `viewDef.ribbon` and is invisible to the row-action
 *      catalog - it appears in no row's action menu.
 *   6. An unresolvable ribbon action id is a visible refusal, never a silent no-op.
 *   7. A row-less `onSuccess=mergeRow` redraws instead of merging (there is no row to merge into, and
 *      mergeRowFromResult dereferences `tr`).
 *
 * Usage:  node ribbon-dialog.cjs <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const ribbonJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !ribbonJsPath) {
	console.error('usage: node ribbon-dialog.cjs <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>');
	process.exit(2);
}
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

/**
 * One window holding both runtimes: juneau-views.js publishes NS.init, then juneau-ribbon.js publishes NS.ribbon
 * onto the SAME window.JuneauViews namespace - which is exactly how a real page loads them, and what lets the
 * ribbon's `dialog` branch reach the view runtime's resolver at click time.
 */
function loadBoth() {
	const env = makeEnv();
	const loaded = loadViews(rendersJsPath, viewsJsPath, env);
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () {},
		setInterval: function () { return 0; }, clearInterval: function () {}
	};
	// NOSONAR javascript:S1523 -- loading the production juneau-ribbon.js source into a VM sandbox is this
	// harness's intended mechanism; the path is a fixed local file supplied by the test.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' });
	return { env: env, NS: env.window.JuneauViews, I: loaded.I };
}

const out = {};
{
	const probe = loadBoth();
	out.hasSeam = !!(probe.I && typeof probe.I.openRibbonDialog === 'function'
		&& typeof probe.I.ribbonDialogActionIsOpenable === 'function'
		&& typeof probe.NS?.ribbon?.build === 'function');
}
if (!out.hasSeam) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 6) { return r(); } setTimeout(tick, 0); })(); });
}

/**
 * A view whose ribbon carries `dialogAction` (and nothing in `rowActions` unless asked), wired the way
 * constructTable wires a real one: the ribbon bar built by NS.ribbon.build lands in a `.juneau-view-toolbar-row`
 * inside the DataTables wrapper, with the table beside it - so the ribbon-anchored banner host has a real
 * toolbar row to anchor under.
 */
function buildFixture(env, NS, dialogAction, opts) {
	const o = opts || {};
	const wrapper = env.el('div');
	wrapper.className = 'dt-container';
	env.body.appendChild(wrapper);

	const table = env.el('table');
	if (o.csrf !== false) table.setAttribute('data-juneau-csrf', 'tok-1');

	const viewDef = { id: 'v1', ribbon: [dialogAction], rowActions: o.rowActions || [] };
	let redraws = 0;
	const ctx = {
		table: table, viewDef: viewDef, dataTable: {}, activeState: {},
		redraw: function () { redraws++; },
		redrawCount: function () { return redraws; }
	};

	const bar = NS.ribbon.build(viewDef, ctx);
	const toolbarRow = env.el('div');
	toolbarRow.className = 'juneau-view-toolbar-row';
	const right = env.el('div');
	right.className = 'juneau-view-toolbar-right';
	if (bar) right.appendChild(bar);
	toolbarRow.appendChild(right);
	wrapper.appendChild(toolbarRow);
	wrapper.appendChild(table);

	return { wrapper: wrapper, table: table, ctx: ctx, viewDef: viewDef, bar: bar };
}

/** The one ribbon button the `dialog` branch rendered (icon-only: its name rides `title`/`aria-label`). */
function dialogButton(fx) {
	return fx.bar ? fx.bar.querySelector('.juneau-view-ribbon-btn') : null;
}

function ribbonRefusalText(env) {
	const el = env.body.querySelector('.juneau-view-ribbon-action-refusal');
	return el ? el.textContent : null;
}

function ribbonOutcome(env) {
	return env.body.querySelector('.juneau-view-ribbon-action-outcome');
}

/** Any ROW banner anywhere - must stay absent for a row-less dialog, whose messages belong in the ribbon host. */
function anyRowBanner(env) {
	return env.body.querySelector('.juneau-view-action-outcome') != null
		|| env.body.querySelector('.juneau-view-action-refusal') != null;
}

(async function main() {

	// --- 1/2) Open with no row, then confirm: both ends of the seam, settling into the ribbon-anchored host -----
	await (async function () {
		const { env, NS, I } = loadBoth();
		const action = { type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects' };
		const fx = buildFixture(env, NS, action);

		out.open_ribbonRenderedOneButton = fx.bar != null && fx.bar.querySelectorAll('.juneau-view-ribbon-btn').length === 1;
		const btn = dialogButton(fx);
		out.open_buttonNameIsTitleNotId = btn != null && btn.title === 'Add project'
			&& btn.getAttribute('aria-label') === 'Add project';

		const postBodies = [];
		env.setFetch(function (url, o2) {
			postBodies.push(o2 && o2.body);
			return Promise.resolve(jsonResponse({ outcome: 'success', message: 'created' }));
		});

		// The click goes through the ribbon runtime's `dialog` branch -> NS.init.openRibbonDialog -> the shared seam.
		if (btn) btn.dispatch('click');
		await flush();

		out.open_dialogOpenedWithNoRow = I.dialogLayerCount() === 1;
		out.open_noFetchBeforeConfirm = postBodies.length === 0;
		const title = env.body.querySelector('.juneau-view-dialog-title');
		// The widened name read: a RibbonAction has `title` and no `label`, so pre-widening this said 'add-project'.
		out.open_dialogTitleFromWidenedNameRead = title != null && title.textContent === 'Add project';

		const confirmBtn = env.body.querySelector('.juneau-view-dialog-confirm');
		if (confirmBtn) confirmBtn.dispatch('click');
		await flush();

		out.submit_firedOnConfirm = postBodies.length === 1;
		const body = postBodies.length === 1 ? JSON.parse(postBodies[0]) : {};
		out.submit_bodyCarriesAction = body.action === 'add-project';
		// No row, no self-targeted key: nothing to send as a target, and nothing invented.
		out.submit_bodyHasNoTargetId = !Object.hasOwn(body, 'targetId');
		const outcome = ribbonOutcome(env);
		out.submit_outcomeLandedInRibbonHost = outcome != null;
		out.submit_outcomeState = outcome ? outcome.dataset.state : null;
		out.submit_outcomeRole = outcome ? outcome.getAttribute('role') : null;
		out.submit_outcomeTestid = outcome ? outcome.dataset.testid : null;
		out.submit_hostAnchoredAfterToolbarRow = (function () {
			const host = env.body.querySelector('.juneau-view-ribbon-banner-host');
			if (!host || host.parentNode !== fx.wrapper) return false;
			const kids = fx.wrapper.childNodes;
			return kids.indexOf(host) === kids.findIndex(function (c) { return c.className === 'juneau-view-toolbar-row'; }) + 1;
		})();
		out.submit_noRowBannerAnywhere = !anyRowBanner(env);
	})();

	// --- 3a) OPEN-path transport refusal (non-2xx form GET) with tr == null ------------------------------------
	await (async function () {
		const { env, NS, I } = loadBoth();
		const action = {
			type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects',
			form: '/projects/new-form'
		};
		const fx = buildFixture(env, NS, action);
		env.setFetch(function () { return Promise.resolve(jsonResponse({ reason: 'nope' }, { status: 403 })); });

		const btn = dialogButton(fx);
		if (btn) btn.dispatch('click');
		await flush();

		out.openFail_noDialogOpened = I.dialogLayerCount() === 0;
		const outcome = ribbonOutcome(env);
		out.openFail_transportRefusalInRibbonHost = outcome != null;
		out.openFail_transportRefusalState = outcome ? outcome.dataset.state : null;
		out.openFail_transportRefusalRole = outcome ? outcome.getAttribute('role') : null;
		out.openFail_noRowBannerAnywhere = !anyRowBanner(env);
	})();

	// --- 3b) OPEN-path named refusal (unparseable form-GET body) with tr == null -------------------------------
	await (async function () {
		const { env, NS } = loadBoth();
		const action = {
			type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects',
			form: '/projects/new-form'
		};
		const fx = buildFixture(env, NS, action);
		env.setFetch(function () { return Promise.resolve(jsonResponse('not json at all')); });

		const btn = dialogButton(fx);
		if (btn) btn.dispatch('click');
		await flush();

		const text = ribbonRefusalText(env);
		out.openParse_refusalInRibbonHost = text != null;
		// The widened name read again: the refusal names the action, not its raw id.
		out.openParse_refusalNamesActionByTitle = text != null && text.indexOf("'Add project'") >= 0;
		out.openParse_noRowBannerAnywhere = !anyRowBanner(env);
	})();

	// --- 4) SUBMIT-path pre-flight refusal (no CSRF token on the table) ---------------------------------------
	await (async function () {
		const { env, NS } = loadBoth();
		const action = { type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects' };
		const fx = buildFixture(env, NS, action, { csrf: false });
		let fetches = 0;
		env.setFetch(function () { fetches++; return Promise.resolve(jsonResponse({ outcome: 'success' })); });

		const btn = dialogButton(fx);
		if (btn) btn.dispatch('click');
		await flush();
		const confirmBtn = env.body.querySelector('.juneau-view-dialog-confirm');
		if (confirmBtn) confirmBtn.dispatch('click');
		await flush();

		out.submitRefusal_nothingSent = fetches === 0;
		const text = ribbonRefusalText(env);
		out.submitRefusal_inRibbonHost = text != null;
		out.submitRefusal_namesActionAndReason = text != null && text.indexOf("'Add project'") >= 0
			&& text.indexOf('not sent') >= 0;
		out.submitRefusal_noRowBannerAnywhere = !anyRowBanner(env);
	})();

	// --- 5) Catalog isolation: the ribbon catalog is not the row-action catalog -------------------------------
	(function () {
		const { env, NS, I } = loadBoth();
		const action = { type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects' };
		const rowAction = { id: 'ack', label: 'Ack', method: 'POST', endpoint: '/rows/{id}/ack' };
		const fx = buildFixture(env, NS, action, { rowActions: [rowAction] });

		out.catalog_ribbonResolverFindsIt = I.ribbonDialogActionIsOpenable(fx.ctx, 'add-project') === true;
		out.catalog_rowResolverDoesNot = I.dialogActionIsOpenable(fx.ctx, 'add-project') === false;
		out.catalog_findRibbonDialogActionIgnoresNonDialogTypes =
			I.findRibbonDialogAction({ viewDef: { ribbon: [{ type: 'refresh', id: 'add-project' }] } }, 'add-project') == null;

		const tr = env.el('tr');
		const menu = I.buildRowActionMenu(fx.viewDef, fx.table, tr, fx.ctx);
		const ids = menu.querySelectorAll('.juneau-view-action-item').map(function (b) { return b.dataset.actionId; });
		out.catalog_rowMenuIds = ids.join(',');
		out.catalog_rowMenuExcludesRibbonDialog = ids.indexOf('add-project') < 0;
	})();

	// --- 6) An unresolvable ribbon action id is a VISIBLE refusal, not a silent no-op -------------------------
	(function () {
		const { env, NS, I } = loadBoth();
		const action = { type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects' };
		const fx = buildFixture(env, NS, action);
		I.openRibbonDialog('nope-not-here', fx.table, fx.ctx);
		const text = ribbonRefusalText(env);
		out.unknownId_refusalInRibbonHost = text != null;
		out.unknownId_refusalNamesTheId = text != null && text.indexOf("'nope-not-here'") >= 0
			&& text.indexOf('not available') >= 0;
		out.unknownId_noDialogOpened = I.dialogLayerCount() === 0;
	})();

	// --- 7) A row-less onSuccess=mergeRow redraws (mergeRowFromResult is never handed a null tr) --------------
	await (async function () {
		const { env, NS } = loadBoth();
		const action = {
			type: 'dialog', id: 'add-project', title: 'Add project', method: 'POST', endpoint: '/projects',
			onSuccess: 'mergeRow'
		};
		const fx = buildFixture(env, NS, action);
		env.setFetch(function () {
			return Promise.resolve(jsonResponse({ outcome: 'success', row: { id: 'p-1' } }));
		});

		const btn = dialogButton(fx);
		if (btn) btn.dispatch('click');
		await flush();
		const confirmBtn = env.body.querySelector('.juneau-view-dialog-confirm');
		if (confirmBtn) confirmBtn.dispatch('click');
		await flush();

		out.mergeRowless_redrawCalledOnce = fx.ctx.redrawCount() === 1;
		out.mergeRowless_outcomeStillRendered = (ribbonOutcome(env) || {}).dataset?.state === 'success';
	})();

	process.stdout.write(JSON.stringify(out));
})();
