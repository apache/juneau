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
 * dialog-receipt.cjs - always-on Node harness for the in-dialog result RECEIPT (WORK-J0513 Scope A): a dialog that
 * opted in with `ModalDef.keepOpenOnSubmit` keeps its layer across the submit, and the runtime paints the write's
 * result into that already-open dialog rather than closing it and painting a row banner.
 *
 * Why a DOM harness and not source pins: the interesting failures here are not "is the right function called", they
 * are "which surface did the outcome land on, and can the operator still submit".  A stuck busy modal, a Confirm
 * left disabled on a retryable refusal, a receipt painted over a still-populated form, an outcome rendered twice
 * (once in the dialog and once behind the backdrop) - all of those are DOM-shaped and invisible to a source pin.
 *
 * The submit graph has NINE mutually-exclusive terminals, so this harness walks them one case per terminal and
 * checks the DISPOSITION each one settles to, plus the two enforcement properties that hold across all of them:
 *
 *   1. T8-receipt   - success + resultForm on a held dialog: swap in place, same layer depth, no submit controls.
 *   2. T8-commit    - success with NO resultForm: whole-stack close, and the row banner still renders.
 *   3. F4           - success + resultForm on an UN-held dialog: success banner unchanged, plus one role=status
 *                     diagnostic.  Never a new layer, never a refusal.
 *   4. F1c          - the receipt payload carries a `form`: refused VISIBLY with its own wording, terminal.
 *   5. F12          - the receipt GET fails (non-2xx / unparseable / REJECTED promise): terminal, Close-only, and
 *                     the busy marker cleared - a committed write must never leave a permanently stuck modal.
 *   6. T1           - a pre-flight client refusal (nothing sent): retryable, Confirm re-enabled.
 *   7. T2           - a network reject (ambiguous): terminal, Confirm NOT re-enabled, no one-click duplicate.
 *   8. T4           - a transport refusal (a gate said no, nothing written): retryable.
 *   9. T5           - a job-started pointer: the dialog closes BEFORE the row-anchored progress affordance paints.
 *  10. Cancel       - the operator dismissed the dialog mid-flight: the outcome falls back to the row banner.
 *  11. Field kinds  - `code` paints a <pre> + copy button; an unrecognized kind falls back to text.
 *
 * Usage:  node dialog-receipt.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-receipt.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const FORM_URL = '/x/form';
const RECEIPT_URL = '/x/receipt';
const ENDPOINT = '/x/act';

function load() {
	const env = makeEnv();
	return loadViews(rendersJsPath, viewsJsPath, env);
}

const out = {};
{
	const probe = load();
	out.hasReceiptSeam = !!(probe.I
		&& typeof probe.I.beginDialogResultHold === 'function'
		&& typeof probe.I.settleDialogResultHold === 'function'
		&& typeof probe.I.paintDialogReceipt === 'function'
		&& typeof probe.I.fetchResultForm === 'function'
		&& typeof probe.I.appendDialogCloseOnlyRow === 'function');
}
if (!out.hasReceiptSeam) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 8) { return r(); } setTimeout(tick, 0); })(); });
}

/**
 * The served confirmation payload: a form-bearing ModalDef, so it must carry the runtime's dialog-form contract
 * version at BOTH levels or openActionDialog refuses before any of this is reachable.
 */
function modalPayload(opts) {
	const o = opts || {};
	const m = {
		contractVersion: '2',
		title: 'Really do it?',
		fields: o.fields || [{ label: 'Id', value: 'INC-1' }],
		form: {
			contractVersion: '2',
			fields: [{ name: 'note', type: 'text', label: 'Note', value: 'typed-by-operator' }]
		}
	};
	if (o.keepOpen) m.keepOpenOnSubmit = true;
	if (o.childActions) m.childActions = o.childActions;
	return m;
}

/** A table + row + ctx wired the way constructTable wires a real one, with a `present=dialog` row action. */
function fixture(env, opts) {
	const o = opts || {};
	const table = env.el('table');
	if (o.csrf !== false) table.setAttribute('data-juneau-csrf', 'tok-1');
	env.body.appendChild(table);

	const tr = env.el('tr');
	tr.setAttribute('data-juneau-row-id', 'INC-1');
	tr.dataset.juneauRowId = 'INC-1';
	const cell = env.el('td');
	cell.className = 'juneau-view-actions-cell';
	tr.appendChild(cell);
	table.appendChild(tr);

	const action = {
		id: 'act', label: 'Act', present: 'dialog', method: 'POST', endpoint: ENDPOINT, form: FORM_URL,
		onSuccess: o.onSuccess || null
	};
	let redraws = 0;
	const ctx = {
		table: table, viewDef: { id: 'v1', rowActions: [action] }, dataTable: {}, activeState: {},
		redraw: function () { redraws++; },
		redrawCount: function () { return redraws; }
	};
	return { table: table, tr: tr, action: action, ctx: ctx };
}

const q = (env, sel) => env.body.querySelector(sel);
const dialogEl = (env) => q(env, '.juneau-view-dialog');
const confirmBtn = (env) => q(env, '.juneau-view-dialog-confirm');
const heldNotice = (env) => q(env, '.juneau-view-dialog-result-notice');
const rowOutcome = (env) => q(env, '.juneau-view-action-outcome');
const rowRefusal = (env) => q(env, '.juneau-view-action-refusal');
const anyRowBanner = (env) => rowOutcome(env) != null || rowRefusal(env) != null;

/**
 * Opens the dialog and presses Confirm, with `respond` answering the submit POST (and, when given, the follow-up
 * receipt GET).  Returns the loaded env / interfaces so each case can inspect the settled DOM.
 */
async function openAndConfirm(opts) {
	const o = opts || {};
	const { env, I } = load();
	const fx = fixture(env, o);
	env.setFetch(function (url, init) {
		const method = (init && init.method) || 'GET';
		if (method === 'GET' && url === FORM_URL)
			return Promise.resolve(jsonResponse(modalPayload(o)));
		if (method === 'GET' && String(url).indexOf(RECEIPT_URL) === 0)
			return o.receipt();
		return o.submit();
	});
	I.openActionDialog(fx.action, fx.table, fx.tr, fx.ctx);
	await flush();
	const opened = I.dialogLayerCount() === 1;
	const btn = confirmBtn(env);
	if (o.beforeConfirm) o.beforeConfirm(env, I, fx);
	if (btn) btn.dispatch('click');
	if (o.afterConfirm) o.afterConfirm(env, I, fx);
	await flush();
	return { env: env, I: I, fx: fx, opened: opened, confirm: btn };
}

const success = (extra) => () => Promise.resolve(jsonResponse(Object.assign({ outcome: 'success' }, extra || {})));

(async function main() {

	// --- 1) T8-receipt: the swap.  Same layer, no submit controls, the submitted form torn down ---------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: success({ resultForm: RECEIPT_URL, message: 'done' }),
			receipt: () => Promise.resolve(jsonResponse({
				title: 'Deleted',
				fields: [{ label: 'Id', value: 'INC-1' }, { label: 'Token', value: 'tok-9', kind: 'code' }]
			}))
		});
		const env = r.env, d = dialogEl(env);
		out.receipt_dialogOpened = r.opened;
		out.receipt_stillOpenAtSameDepth = r.I.dialogLayerCount() === 1;
		out.receipt_marked = d != null && d.dataset.juneauReceipt === '1' && d.dataset.testid === 'dialog-receipt';
		// The ENFORCEMENT, and it is an absence: no confirm control anywhere in the receipt.
		out.receipt_noConfirmButton = confirmBtn(env) === null;
		out.receipt_noInputsAtAll = env.body.querySelectorAll('[data-juneau-form-field]').length === 0;
		out.receipt_submittedFormTornDown = env.body.querySelector('.juneau-view-dialog-form') === null;
		out.receipt_closeButtonPresent = q(env, '[data-testid="dialog-receipt-close"]') != null;
		out.receipt_titleFromPayload = (function () {
			const t = q(env, '.juneau-view-dialog-title');
			return t != null && t.textContent === 'Deleted';
		})();
		out.receipt_fieldsRepainted = (function () {
			const dl = q(env, '.juneau-view-dialog-fields');
			return dl != null && dl.querySelectorAll('dd').length === 2 && dl.querySelectorAll('dt').length === 2;
		})();
		out.receipt_exactlyOneFieldList = env.body.querySelectorAll('.juneau-view-dialog-fields').length === 1;
		out.receipt_codeFieldIsPre = q(env, '.juneau-view-dialog-field-code') != null
			&& q(env, '.juneau-view-dialog-field-code').textContent === 'tok-9';
		out.receipt_codeFieldHasCopyButton = q(env, '[data-testid="dialog-field-copy"]') != null;
		// The row banner is SUPPRESSED: the dialog is on screen, so rendering it too would show one outcome twice.
		out.receipt_noRowBanner = !anyRowBanner(env);
		out.receipt_busyMarkerCleared = d != null && d.dataset.juneauDialogBusy === undefined;
		// Pressing Close takes the whole stack down.
		const closeBtn = q(env, '[data-testid="dialog-receipt-close"]');
		if (closeBtn) closeBtn.dispatch('click');
		out.receipt_closeTearsDownTheStack = r.I.dialogLayerCount() === 0;
	}

	// --- 2) T8-commit: a held success with NO resultForm closes, and the row banner STILL renders -------------
	{
		const r = await openAndConfirm({ keepOpen: true, submit: success({ message: 'done' }) });
		const env = r.env;
		out.commit_dialogClosed = r.I.dialogLayerCount() === 0;
		out.commit_noDialogInDom = dialogEl(env) === null;
		// closeCommitted returns false on purpose: with the dialog gone the row banner is the ONLY signal left.
		out.commit_rowOutcomeRendered = rowOutcome(env) != null;
		out.commit_rowOutcomeState = rowOutcome(env) ? rowOutcome(env).dataset.state : null;
		out.commit_noReceipt = env.body.querySelector('[data-juneau-receipt]') === null;
	}

	// --- 3) F4: an UN-held success carrying a resultForm - diagnose, never open a layer ----------------------
	{
		let receiptGets = 0;
		const r = await openAndConfirm({
			submit: success({ resultForm: RECEIPT_URL, message: 'done' }),
			receipt: () => { receiptGets++; return Promise.resolve(jsonResponse({ title: 'Deleted', fields: [] })); }
		});
		const env = r.env;
		out.f4_dialogClosedAsToday = r.I.dialogLayerCount() === 0;
		out.f4_noFollowUpGet = receiptGets === 0;
		out.f4_successBannerUnchanged = rowOutcome(env) != null && rowOutcome(env).dataset.state === 'success';
		const diag = q(env, '[data-testid="result-form-ignored"]');
		out.f4_diagnosticPresent = diag != null;
		// role=status, not alert: the write SUCCEEDED, and a consumer authoring bug must not look like a failure.
		out.f4_diagnosticIsStatusNotAlert = diag != null && diag.getAttribute('role') === 'status';
		out.f4_noNewLayer = r.I.dialogLayerCount() === 0 && env.body.querySelector('[data-juneau-receipt]') === null;
	}

	// --- 4) F1c: the receipt payload carries a form - the FORBIDDEN shape, refused visibly -------------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: success({ resultForm: RECEIPT_URL }),
			receipt: () => Promise.resolve(jsonResponse({
				title: 'Nope', form: { contractVersion: '2', fields: [{ name: 'again', type: 'text', label: 'Again' }] }
			}))
		});
		const env = r.env;
		out.f1c_stillOpen = r.I.dialogLayerCount() === 1;
		out.f1c_notPaintedAsReceipt = env.body.querySelector('[data-juneau-receipt]') === null;
		out.f1c_noInputsPainted = env.body.querySelectorAll('[data-juneau-form-field]').length === 0;
		out.f1c_noticeText = heldNotice(env) ? heldNotice(env).textContent : null;
		out.f1c_closeOnlyRow = q(env, '[data-testid="dialog-result-close"]') != null;
		out.f1c_noConfirm = confirmBtn(env) === null;
		out.f1c_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
	}

	// --- 5a) F12: the receipt GET returns a non-2xx --------------------------------------------------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: success({ resultForm: RECEIPT_URL }),
			receipt: () => Promise.resolve(jsonResponse({ reason: 'nope' }, { status: 500 }))
		});
		const env = r.env;
		out.f12_stillOpen = r.I.dialogLayerCount() === 1;
		out.f12_noticeText = heldNotice(env) ? heldNotice(env).textContent : null;
		out.f12_closeOnlyRow = q(env, '[data-testid="dialog-result-close"]') != null;
		out.f12_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
		out.f12_submittedFormTornDown = env.body.querySelector('.juneau-view-dialog-form') === null;
	}

	// --- 5b) F12: the receipt GET PROMISE REJECTS - the arm without which a committed write hangs forever ---
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: success({ resultForm: RECEIPT_URL }),
			receipt: () => Promise.reject(new Error('socket died'))
		});
		const env = r.env;
		out.f12reject_stillOpen = r.I.dialogLayerCount() === 1;
		out.f12reject_noticeText = heldNotice(env) ? heldNotice(env).textContent : null;
		out.f12reject_closeOnlyRow = q(env, '[data-testid="dialog-result-close"]') != null;
		out.f12reject_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
		out.f12reject_noConfirmStuckDisabled = confirmBtn(env) === null;
	}

	// --- 5c) F12: the receipt GET returns 2xx that does not parse ------------------------------------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: success({ resultForm: RECEIPT_URL }),
			receipt: () => Promise.resolve(jsonResponse('not json at all'))
		});
		const env = r.env;
		out.f12parse_noticeText = heldNotice(env) ? heldNotice(env).textContent : null;
		out.f12parse_closeOnlyRow = q(env, '[data-testid="dialog-result-close"]') != null;
		out.f12parse_notPaintedAsReceipt = env.body.querySelector('[data-juneau-receipt]') === null;
	}

	// --- 6) T1: a pre-flight client refusal (no CSRF token) is RETRYABLE ----------------------------------
	{
		let posts = 0;
		const r = await openAndConfirm({
			keepOpen: true, csrf: false,
			submit: () => { posts++; return Promise.resolve(jsonResponse({ outcome: 'success' })); }
		});
		const env = r.env;
		out.t1_nothingSent = posts === 0;
		out.t1_stillOpen = r.I.dialogLayerCount() === 1;
		out.t1_noticePresent = heldNotice(env) != null;
		// Retryable: nothing was sent, so the operator can fix the declaration and press Confirm again.
		out.t1_confirmReEnabled = confirmBtn(env) != null && confirmBtn(env).disabled === false;
		out.t1_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
		out.t1_noCloseOnlyRow = q(env, '[data-testid="dialog-result-close"]') === null;
		out.t1_noRowBanner = !anyRowBanner(env);
		out.t1_formStillStanding = env.body.querySelector('.juneau-view-dialog-form') != null;
	}

	// --- 7) T2: a network reject is AMBIGUOUS - terminal, and Confirm must NOT come back ------------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.reject(new Error('connection reset'))
		});
		const env = r.env;
		out.t2_stillOpen = r.I.dialogLayerCount() === 1;
		out.t2_noticeText = heldNotice(env) ? heldNotice(env).textContent : null;
		// The whole point: re-enabling Confirm here would hand the operator a one-click duplicate of a write that
		// may well have landed.
		out.t2_noConfirmButton = confirmBtn(env) === null;
		out.t2_closeOnlyRow = q(env, '[data-testid="dialog-result-close"]') != null;
		out.t2_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
		// Terminal keeps the typed values on screen so the operator can copy them out before Close.
		out.t2_formStillStanding = env.body.querySelector('.juneau-view-dialog-form') != null;
		out.t2_noRowBanner = !anyRowBanner(env);
	}

	// --- 8) T4: a transport refusal is a gate saying no - nothing written, so RETRYABLE -------------------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.resolve(jsonResponse({ reason: 'not-armed' }, { status: 403 }))
		});
		const env = r.env;
		out.t4_stillOpen = r.I.dialogLayerCount() === 1;
		out.t4_noticePresent = heldNotice(env) != null;
		out.t4_confirmReEnabled = confirmBtn(env) != null && confirmBtn(env).disabled === false;
		out.t4_noCloseOnlyRow = q(env, '[data-testid="dialog-result-close"]') === null;
		out.t4_busyCleared = dialogEl(env).dataset.juneauDialogBusy === undefined;
	}

	// --- 9) T8-failure / T8-refusal: the two remaining dispositions of the typed-outcome terminal --------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.resolve(jsonResponse({ outcome: 'failure', message: 'boom' }))
		});
		const env = r.env;
		out.t8fail_stillOpen = r.I.dialogLayerCount() === 1;
		out.t8fail_terminalCloseOnly = q(env, '[data-testid="dialog-result-close"]') != null;
		out.t8fail_noConfirm = confirmBtn(env) === null;
		out.t8fail_noticePresent = heldNotice(env) != null;
	}
	{
		// F11: a named refusal did not write, so Confirm comes back.
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.resolve(jsonResponse({ outcome: 'refusal', refusalCode: 'write-guard:not-armed' }))
		});
		const env = r.env;
		out.t8refuse_confirmReEnabled = confirmBtn(env) != null && confirmBtn(env).disabled === false;
		out.t8refuse_noCloseOnlyRow = q(env, '[data-testid="dialog-result-close"]') === null;
		out.t8refuse_noticePresent = heldNotice(env) != null;
	}

	// --- 10) resultForm is IGNORED on a non-success outcome: no receipt for a write that did not happen ---
	{
		let receiptGets = 0;
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.resolve(jsonResponse({ outcome: 'failure', resultForm: RECEIPT_URL })),
			receipt: () => { receiptGets++; return Promise.resolve(jsonResponse({ title: 'Deleted' })); }
		});
		const env = r.env;
		out.failResultForm_noGetIssued = receiptGets === 0;
		out.failResultForm_noReceiptPainted = env.body.querySelector('[data-juneau-receipt]') === null;
		out.failResultForm_terminalCloseOnly = q(env, '[data-testid="dialog-result-close"]') != null;
		out.failResultForm_noDiagnostic = q(env, '[data-testid="result-form-ignored"]') === null;
	}

	// --- 11) T5: a job pointer closes the dialog BEFORE the row-anchored progress affordance paints -------
	{
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => Promise.resolve(jsonResponse({ streamUrl: '/x/stream' }))
		});
		const env = r.env;
		out.t5_dialogClosed = r.I.dialogLayerCount() === 0;
		out.t5_noDialogInDom = dialogEl(env) === null;
		// Everything startJobStream paints is ROW-anchored, so a surviving backdrop would hide the whole
		// affordance behind a modal that can never settle (a job pointer is not a terminal result and can never
		// carry a resultForm).  This shim has no EventSource, so the path taken is that function's own
		// no-SSE-transport fallback - which is itself a ROW paint, and therefore proves the same ordering: the
		// close already happened, and the row was reachable when the job path ran.
		out.t5_rowAnchoredSignalVisible = rowOutcome(env) != null;
		out.t5_rowSignalIsNotInADialog = rowOutcome(env) != null && rowOutcome(env).closest('.juneau-view-dialog') === null;
	}

	// --- 12) Cancel mid-flight: the layer is gone, so the outcome falls back to the row banner -----------
	{
		let resolveSubmit = null;
		const r = await openAndConfirm({
			keepOpen: true,
			submit: () => new Promise(function (res) { resolveSubmit = res; }),
			afterConfirm: function (env, I) {
				// Cancel stays enabled on purpose (Escape pops the layer regardless, so the design has to tolerate
				// "dialog gone before settle" anyway).
				const cancel = env.body.querySelector('.juneau-view-dialog-cancel');
				if (cancel) cancel.dispatch('click');
			}
		});
		out.cancel_layerGone = r.I.dialogLayerCount() === 0;
		if (resolveSubmit) resolveSubmit(jsonResponse({ outcome: 'refusal', refusalCode: 'app:x' }));
		await flush();
		const env = r.env;
		out.cancel_outcomeFellBackToRow = rowOutcome(env) != null || rowRefusal(env) != null;
		out.cancel_noThrowAndNoDialog = dialogEl(env) === null;
	}

	// --- 13) Field kinds on the confirmation payload: `code` -> <pre>, unknown -> plain text -------------
	{
		const { env, I } = load();
		const dl = I.buildModalFieldList([
			{ label: 'Plain', value: 'p' },
			{ label: 'Coded', value: 'c' , kind: 'code' },
			{ label: 'Bogus', value: 'b', kind: 'html' },
			{ label: 'Explicit', value: 'e', kind: 'text' }
		]);
		env.body.appendChild(dl);
		const dds = dl.querySelectorAll('dd');
		out.kinds_ddCount = dds.length === 4;
		out.kinds_plainIsText = dds[0].querySelector('pre') === null && dds[0].textContent === 'p';
		out.kinds_codeIsPre = dds[1].querySelector('pre') !== null;
		out.kinds_codeHasCopy = dds[1].querySelector('.juneau-view-dialog-field-copy') !== null;
		// An unrecognized token falls BACK to text rather than being trusted.
		out.kinds_unknownFallsBackToText = dds[2].querySelector('pre') === null && dds[2].textContent === 'b';
		out.kinds_explicitTextIsText = dds[3].querySelector('pre') === null && dds[3].textContent === 'e';
		out.kinds_allowlistIsExactlyTwo = I.isModalFieldKind('text') === true && I.isModalFieldKind('code') === true
			&& I.isModalFieldKind('html') === false && I.isModalFieldKind('') === false;
	}

	// --- 14) A hold settles exactly ONCE: the second hand-off on the same submit finds nothing -----------
	{
		const { env, I } = load();
		const fx = fixture(env);
		const dialog = env.el('div');
		dialog.className = 'juneau-view-dialog';
		const btn = env.el('button');
		const backdrop = env.el('div');
		env.body.appendChild(backdrop);
		fx.ctx._dialogStack = [backdrop];
		I.beginDialogResultHold({ backdrop: backdrop, dialog: dialog, confirmBtn: btn }, fx.action, {}, fx.ctx);
		out.once_holdRegistered = fx.ctx._resultHold != null;
		out.once_confirmDisabledAtClickTime = btn.disabled === true;
		out.once_busyMarked = dialog.dataset.juneauDialogBusy === '1';
		out.once_firstSettleHandsOff = I.settleDialogResultHold(fx.ctx, 'terminal') === true;
		out.once_holdCleared = fx.ctx._resultHold == null;
		out.once_secondSettleIsANoOp = I.settleDialogResultHold(fx.ctx, 'terminal') === false;
		// closeCommitted is the ONE disposition that returns false, so the caller's row painter still runs.
		fx.ctx._dialogStack = [backdrop];
		I.beginDialogResultHold({ backdrop: backdrop, dialog: dialog, confirmBtn: btn }, fx.action, {}, fx.ctx);
		out.once_closeCommittedDoesNotSuppressRowPaint = I.settleDialogResultHold(fx.ctx, 'closeCommitted') === false;
		// A settle with no hold at all is also false - the un-held path renders exactly as it does today.
		out.once_noHoldIsFalse = I.settleDialogResultHold(fx.ctx, 'terminal') === false;
		out.once_noCtxIsFalse = I.settleDialogResultHold(null, 'terminal') === false;
	}

	// --- 15) A child-action button is disabled for the duration of the hold, and RESTORED to its prior state
	{
		const { env, I } = load();
		const fx = fixture(env);
		const dialog = env.el('div');
		dialog.className = 'juneau-view-dialog';
		const live = env.el('button');
		live.className = 'juneau-view-dialog-form-action';
		const alreadyGated = env.el('button');
		alreadyGated.className = 'juneau-view-dialog-form-action';
		alreadyGated.disabled = true;
		dialog.appendChild(live);
		dialog.appendChild(alreadyGated);
		const backdrop = env.el('div');
		env.body.appendChild(backdrop);
		fx.ctx._dialogStack = [backdrop];
		I.beginDialogResultHold({ backdrop: backdrop, dialog: dialog, confirmBtn: env.el('button') }, fx.action, {}, fx.ctx);
		out.children_liveOneDisabled = live.disabled === true && live.getAttribute('aria-disabled') === 'true';
		out.children_gatedOneStaysDisabled = alreadyGated.disabled === true;
		I.settleDialogResultHold(fx.ctx, 'retryable');
		out.children_liveOneRestored = live.disabled === false && live.getAttribute('aria-disabled') === null;
		// The restore must never ENABLE a child that was gated before the submit started.
		out.children_gatedOneStillDisabled = alreadyGated.disabled === true;
	}

	process.stdout.write(JSON.stringify(out));
})();
