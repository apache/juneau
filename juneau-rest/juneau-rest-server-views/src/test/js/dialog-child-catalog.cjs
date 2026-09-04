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
 * dialog-child-catalog.cjs - always-on Node harness for the DIALOG-SCOPED child-action catalog (WORK-J0513 Scope
 * B): `ModalDef.childActions` lets a `type="action"` input inside one dialog's form open a STACKED step that is
 * not a row action, and therefore appears in no row's action menu and in no ribbon.
 *
 * The properties worth a DOM harness rather than a source pin, in the order the resolver decides them:
 *
 *   1. Scope. A child action opens from the dialog that declares it, and is invisible to buildRowActionMenu.
 *   2. Precedence. A declared ROW action wins an id collision, gating included - a served payload must never be
 *      able to shadow (and thereby bypass the gating of) a row action.  A collision with a NON-dialog row action
 *      is a fail-closed refusal, not a fall-through to the catalog, because rescuing it there would be the same
 *      shadowing hole through a narrower door.
 *   3. Fail-closed. An id in neither catalog stays today's disabled+marked paint, and clicking it is a visible
 *      refusal - never a throw, never a silent no-op.
 *   4. Stacking. Opening a child pushes a real layer and consumes a real MAX_DIALOG_DEPTH slot, so the cap still
 *      refuses at the same place.
 *   5. Drafts. `carryDrafts` carries the PARENT's typed-but-unsubmitted values on the child's form GET; over the
 *      cap it is a visible refusal and the child does not open, because a silently-truncated prefill is a
 *      silently-WRONG prefill.
 *   6. Hold interaction. A `type=action` click on a dialog whose write is in flight or already settled into a
 *      receipt is a no-op - it must not stack a child onto a committed layer.
 *
 * Usage:  node dialog-child-catalog.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-child-catalog.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const PARENT_FORM = '/x/step1-form';
const CHILD_FORM = '/x/review-form';

function load() {
	const env = makeEnv();
	return loadViews(rendersJsPath, viewsJsPath, env);
}

const out = {};
{
	const probe = load();
	out.hasCatalogSeam = !!(probe.I
		&& typeof probe.I.childActionById === 'function'
		&& typeof probe.I.rowActionIdExists === 'function'
		&& typeof probe.I.withDraftQuery === 'function'
		&& typeof probe.I.openChildActionDialog === 'function'
		&& typeof probe.I.MAX_DRAFT_QUERY_BYTES === 'number');
}
if (!out.hasCatalogSeam) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 8) { return r(); } setTimeout(tick, 0); })(); });
}

const q = (env, sel) => env.body.querySelector(sel);
const dialogRefusalText = (env) => {
	const el = q(env, '.juneau-view-dialog-action-refusal');
	return el ? el.textContent : null;
};

/**
 * The parent dialog's served payload: a form carrying a `type=action` input plus the dialog-scoped catalog the
 * input resolves against.
 */
function parentPayload(opts) {
	const o = opts || {};
	return {
		contractVersion: '2',
		title: 'Step 1',
		form: {
			contractVersion: '2',
			fields: [
				{ name: 'note', type: 'text', label: 'Note', value: 'typed-by-operator' },
				{ name: 'go', type: 'action', label: 'Review', actionId: o.actionId || 'review' }
			]
		},
		childActions: o.childActions === undefined
			? [{ id: 'review', label: 'Review', form: CHILD_FORM, endpoint: '/x/review', method: 'POST' }]
			: o.childActions
	};
}

/** A table + row + ctx with a `present=dialog` row action that serves the parent payload. */
function fixture(env, opts) {
	const o = opts || {};
	const table = env.el('table');
	table.setAttribute('data-juneau-csrf', 'tok-1');
	env.body.appendChild(table);

	const tr = env.el('tr');
	tr.setAttribute('data-juneau-row-id', 'INC-1');
	tr.dataset.juneauRowId = 'INC-1';
	const cell = env.el('td');
	cell.className = 'juneau-view-actions-cell';
	tr.appendChild(cell);
	table.appendChild(tr);

	const parent = { id: 'step1', label: 'Step 1', present: 'dialog', method: 'POST', endpoint: '/x/step1', form: PARENT_FORM };
	const rowActions = [parent].concat(o.extraRowActions || []);
	const ctx = { table: table, viewDef: { id: 'v1', rowActions: rowActions }, dataTable: {}, activeState: {} };
	return { table: table, tr: tr, parent: parent, ctx: ctx };
}

/** Opens the parent dialog through the real fetch path and returns the child-action button inside it. */
async function openParent(opts) {
	const o = opts || {};
	const { env, I } = load();
	const fx = fixture(env, o);
	const gets = [];
	env.setFetch(function (url, init) {
		const method = (init && init.method) || 'GET';
		if (method === 'GET') gets.push(String(url));
		if (method === 'GET' && url === PARENT_FORM) return Promise.resolve(jsonResponse(parentPayload(o)));
		if (method === 'GET' && String(url).indexOf(CHILD_FORM) === 0)
			return Promise.resolve(jsonResponse({
				contractVersion: '2', title: 'Review',
				form: { contractVersion: '2', fields: [{ name: 'note', type: 'text', label: 'Note' }] }
			}));
		return Promise.resolve(jsonResponse({ outcome: 'success' }));
	});
	I.openActionDialog(fx.parent, fx.table, fx.tr, fx.ctx);
	await flush();
	return { env: env, I: I, fx: fx, gets: gets, btn: q(env, '.juneau-view-dialog-form-action') };
}

(async function main() {

	// --- 1) A child action opens a STACKED step, and never appears in a row menu -----------------------------
	{
		const r = await openParent();
		const env = r.env, I = r.I;
		out.open_parentOpened = I.dialogLayerCount() === 1;
		out.open_childButtonPainted = r.btn != null;
		// The resolution that fails closed today: the id is in NEITHER row catalog, so pre-J0513 this button was
		// painted disabled+marked with no way for its author to reach it.
		out.open_childButtonEnabled = r.btn != null && r.btn.disabled === false;
		out.open_childButtonNotMarkedMissing = r.btn != null && r.btn.dataset.juneauActionMissing === undefined;

		if (r.btn) r.btn.dispatch('click');
		await flush();
		out.open_childStackedALayer = I.dialogLayerCount() === 2;
		out.open_childFormWasFetched = r.gets.some(function (u) { return u.indexOf(CHILD_FORM) === 0; });
		out.open_noRefusalPainted = dialogRefusalText(env) === null;

		// Scope: the catalog rides the per-open payload, so no row menu can see it.
		const menu = I.buildRowActionMenu(r.fx.ctx.viewDef, r.fx.table, r.fx.tr, r.fx.ctx);
		const ids = menu.querySelectorAll('.juneau-view-action-item').map(function (b) { return b.dataset.actionId; });
		out.scope_rowMenuIds = ids.join(',');
		out.scope_rowMenuExcludesChild = ids.indexOf('review') < 0;
		// ...and neither can the ribbon resolver.
		out.scope_ribbonResolverExcludesChild = I.ribbonDialogActionIsOpenable(r.fx.ctx, 'review') === false;
		// ...nor the row-action resolver.
		out.scope_rowResolverExcludesChild = I.dialogActionIsOpenable(r.fx.ctx, 'review') === false;
	}

	// --- 2) Precedence: a declared DIALOG row action of the same id wins, gating included --------------------
	{
		const rowTwin = {
			id: 'review', label: 'Row Review', present: 'dialog', method: 'POST', endpoint: '/x/row-review',
			enabledWhen: [{ field: 'status', op: 'eq', value: 'open', reason: 'only open records' }]
		};
		const r = await openParent({ extraRowActions: [rowTwin] });
		const env = r.env;
		// The row-action check runs FIRST and unchanged, so its enabledWhen gate still applies - which is exactly
		// what a served payload must not be able to route around.  This fixture's row carries no `status` at all,
		// which the shared rule evaluator fails CLOSED on.
		out.precedence_rowActionWinsAndStaysGated = r.btn != null && r.btn.disabled === true;
		// Gated, but not silently: the failing rule's own reason is what the control announces.
		out.precedence_gateReasonSurfaced = (function () {
			const el = q(env, '[data-juneau-row-action-desc]');
			return el != null && el.textContent === 'only open records';
		})();
		out.precedence_reasonOnTheControlTitle = r.btn != null && r.btn.getAttribute('title') === 'only open records';
		// NOT the fail-closed "missing" paint: the id resolved, it is the GATE that closed it.
		out.precedence_notMarkedMissing = r.btn != null && r.btn.dataset.juneauActionMissing === undefined;
	}

	// --- 3) Precedence: a collision with a NON-dialog row action is a fail-closed REFUSAL -------------------
	{
		// A gated direct-submit Delete is the dangerous case: falling through to the child catalog here would let
		// a served payload shadow it and bypass its gating through a narrower door.
		const directDelete = { id: 'review', label: 'Delete', method: 'DELETE', endpoint: '/x/rows/{id}' };
		const r = await openParent({ extraRowActions: [directDelete] });
		const env = r.env, I = r.I;
		out.shadow_buttonPaintedDisabled = r.btn != null && r.btn.disabled === true;
		out.shadow_buttonMarkedMissing = r.btn != null && r.btn.dataset.juneauActionMissing === '1';
		out.shadow_ariaDisabled = r.btn != null && r.btn.getAttribute('aria-disabled') === 'true';
		// Defense in depth: the click handler refuses too, rather than relying on the disabled attribute alone.
		I.openFormActionDialog('review', r.fx.table, r.fx.tr, r.fx.ctx,
			[{ id: 'review', label: 'Review', form: CHILD_FORM }]);
		await flush();
		out.shadow_clickIsAVisibleRefusal = dialogRefusalText(env) != null;
		out.shadow_refusalNamesTheId = (dialogRefusalText(env) || '').indexOf("'review'") >= 0;
		out.shadow_noChildOpened = I.dialogLayerCount() === 1;
	}

	// --- 4) Fail-closed: an id in NEITHER catalog stays disabled, and a click refuses visibly ---------------
	{
		const r = await openParent({ actionId: 'nope', childActions: [] });
		const env = r.env, I = r.I;
		out.unknown_buttonDisabled = r.btn != null && r.btn.disabled === true;
		out.unknown_buttonMarkedMissing = r.btn != null && r.btn.dataset.juneauActionMissing === '1';
		I.openFormActionDialog('nope', r.fx.table, r.fx.tr, r.fx.ctx, []);
		await flush();
		out.unknown_clickIsAVisibleRefusal = dialogRefusalText(env) != null;
		out.unknown_noChildOpened = I.dialogLayerCount() === 1;
		// An absent catalog behaves exactly like an empty one (the parameter is optional by design, so every
		// existing positional caller keeps working unchanged).
		out.unknown_absentCatalogSameAsEmpty = I.childActionById(null, 'review') === null
			&& I.childActionById(undefined, 'review') === null
			&& I.childActionById([], 'review') === null;
	}

	// --- 5) Stacking: a child consumes a real depth slot, so the cap still refuses at the same place --------
	{
		const r = await openParent();
		const I = r.I;
		if (r.btn) r.btn.dispatch('click');
		await flush();
		out.depth_childIsARealLayer = I.dialogLayerCount() === 2 && I.MAX_DIALOG_DEPTH === 2;
		// A third is the visible depth refusal, not a third overlay.
		I.openChildActionDialog({ id: 'review', label: 'Review', form: CHILD_FORM }, r.fx.table, r.fx.tr, r.fx.ctx);
		await flush();
		out.depth_thirdIsRefused = I.dialogLayerCount() === 2;
		out.depth_refusalPainted = q(r.env, '[data-testid="dialog-depth-refusal"]') != null;
	}

	// --- 6) carryDrafts: the parent's typed values ride the child's form GET --------------------------------
	{
		const r = await openParent({
			childActions: [{ id: 'review', label: 'Review', form: CHILD_FORM, carryDrafts: true }]
		});
		const I = r.I;
		// Type something the operator has NOT submitted yet - the whole reason this channel exists.
		const note = q(r.env, '[data-juneau-form-field="note"]');
		if (note) note.value = 'edited-in-place';
		if (r.btn) r.btn.dispatch('click');
		await flush();
		const childGet = r.gets.find(function (u) { return u.indexOf(CHILD_FORM) === 0; });
		out.drafts_childGetUrl = childGet || null;
		out.drafts_queryParamPresent = childGet != null && childGet.indexOf('juneauDrafts=') > 0;
		out.drafts_carriedTheEditedValue = (function () {
			if (childGet == null) return false;
			const raw = decodeURIComponent(childGet.slice(childGet.indexOf('juneauDrafts=') + 'juneauDrafts='.length));
			const parsed = JSON.parse(raw);
			return parsed.note === 'edited-in-place';
		})();
		out.drafts_childOpened = I.dialogLayerCount() === 2;
	}

	// --- 7) The draft cap is measured on the ENCODED value, and over it the child does NOT open ------------
	{
		const { env, I } = load();
		out.cap_value = I.MAX_DRAFT_QUERY_BYTES;
		const child = { id: 'review', label: 'Review', form: CHILD_FORM };
		out.cap_smallDraftPasses = I.withDraftQuery(child, { note: 'hi' }) != null;
		// A raw JSON string that fits the cap but whose ENCODED form does not: every one of these characters
		// expands to three bytes, so measuring the raw string would let a ~2KB draft become a ~6KB query and blow
		// a request-line limit AFTER the client had already told the operator it was fine.
		const expands = { note: '\u00e9'.repeat(900) };
		out.cap_rawJsonWouldHaveFit = JSON.stringify(expands).length < I.MAX_DRAFT_QUERY_BYTES;
		out.cap_encodedFormIsRefused = I.withDraftQuery(child, expands) === null;
		out.cap_overCapIsRefused = I.withDraftQuery(child, { note: 'x'.repeat(I.MAX_DRAFT_QUERY_BYTES + 1) }) === null;
		// It is a COPY: the descriptor handed in is never mutated.
		const copy = I.withDraftQuery(child, { note: 'hi' });
		out.cap_returnsACopyAndDoesNotMutate = child.form === CHILD_FORM && copy.form !== CHILD_FORM
			&& copy.id === 'review' && copy.label === 'Review';
		// An existing query string is appended to, not clobbered.
		const withQuery = I.withDraftQuery({ id: 'r', label: 'R', form: '/x/f?a=1' }, { note: 'hi' });
		out.cap_appendsToAnExistingQuery = withQuery.form.indexOf('/x/f?a=1&juneauDrafts=') === 0;

		// Over the cap the child does NOT open, and the refusal is visible.
		const fx = fixture(env);
		const backdrop = env.el('div');
		const dialog = env.el('div');
		dialog.className = 'juneau-view-dialog';
		backdrop.appendChild(dialog);
		env.body.appendChild(backdrop);
		I.pushLayer(backdrop, { kind: 'dialog', trapFocus: false, lightDismiss: false });
		const field = env.el('input');
		field.dataset.juneauFormField = 'note';
		field.value = 'x'.repeat(I.MAX_DRAFT_QUERY_BYTES + 1);
		dialog.appendChild(field);
		I.openChildActionDialog({ id: 'review', label: 'Review', form: CHILD_FORM, carryDrafts: true },
			fx.table, fx.tr, fx.ctx);
		out.capRefusal_noChildOpened = I.dialogLayerCount() === 1;
		out.capRefusal_visible = dialogRefusalText(env) != null;
		out.capRefusal_namesTheCap = (dialogRefusalText(env) || '').indexOf(String(I.MAX_DRAFT_QUERY_BYTES)) >= 0;
	}

	// --- 8) A `type=action` click on a BUSY (in-flight or receipt) dialog is a no-op ----------------------
	{
		const r = await openParent();
		const I = r.I;
		const dialog = q(r.env, '.juneau-view-dialog');
		dialog.dataset.juneauDialogBusy = '1';
		I.openFormActionDialog('review', r.fx.table, r.fx.tr, r.fx.ctx,
			[{ id: 'review', label: 'Review', form: CHILD_FORM }]);
		await flush();
		// Not a refusal either: the operator did not ask for anything new, a stale control fired.
		out.busy_noChildStacked = I.dialogLayerCount() === 1;
		out.busy_noRefusalPainted = dialogRefusalText(r.env) === null;
		delete dialog.dataset.juneauDialogBusy;
		I.openFormActionDialog('review', r.fx.table, r.fx.tr, r.fx.ctx,
			[{ id: 'review', label: 'Review', form: CHILD_FORM }]);
		await flush();
		out.busy_worksAgainOnceCleared = I.dialogLayerCount() === 2;
	}

	// --- 9) A SECTIONED parent form threads the catalog too (the second row-painting path) ---------------
	{
		const { env, I } = load();
		const fx = fixture(env);
		const dialog = env.el('div');
		const catalog = [{ id: 'review', label: 'Review', form: CHILD_FORM }];
		I.appendDialogForm(dialog, {
			sections: [{ id: 's1', label: 'One', fields: [{ name: 'go', type: 'action', label: 'Review', actionId: 'review' }] }]
		}, fx.table, fx.tr, fx.ctx, 7, catalog);
		const btn = dialog.querySelector('.juneau-view-dialog-form-action');
		out.sectioned_buttonPainted = btn != null;
		out.sectioned_buttonEnabledFromCatalog = btn != null && btn.disabled === false
			&& btn.dataset.juneauActionMissing === undefined;
		// Without the catalog the same field stays today's fail-closed paint.
		const dialog2 = env.el('div');
		I.appendDialogForm(dialog2, {
			sections: [{ id: 's1', label: 'One', fields: [{ name: 'go', type: 'action', label: 'Review', actionId: 'review' }] }]
		}, fx.table, fx.tr, fx.ctx, 8);
		const btn2 = dialog2.querySelector('.juneau-view-dialog-form-action');
		out.sectioned_withoutCatalogStaysDisabled = btn2 != null && btn2.disabled === true
			&& btn2.dataset.juneauActionMissing === '1';
	}

	// --- 10) The resolver helpers, in isolation ---------------------------------------------------------
	{
		const { I } = load();
		const catalog = [{ id: 'a', label: 'A' }, { id: 'b', label: 'B' }];
		out.helpers_childActionByIdFindsFirst = I.childActionById(catalog, 'b').label === 'B';
		out.helpers_childActionByIdMissIsNull = I.childActionById(catalog, 'zz') === null;
		const ctx = { viewDef: { rowActions: [{ id: 'del', label: 'Delete', method: 'DELETE' }] } };
		// rowActionIdExists is deliberately BROADER than dialogActionIsOpenable: it sees non-dialog row actions
		// too, which is what makes the shadowing refusal in step 3 possible.
		out.helpers_rowActionIdExistsSeesNonDialog = I.rowActionIdExists(ctx, 'del') === true;
		out.helpers_dialogActionIsOpenableDoesNot = I.dialogActionIsOpenable(ctx, 'del') === false;
		out.helpers_rowActionIdExistsMiss = I.rowActionIdExists(ctx, 'nope') === false;
		out.helpers_rowActionIdExistsNullSafe = I.rowActionIdExists(null, 'x') === false
			&& I.rowActionIdExists({}, 'x') === false;
	}

	process.stdout.write(JSON.stringify(out));
})();
