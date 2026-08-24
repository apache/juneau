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
 * popup-layer.cjs - always-on Node harness for the shared popupLayerStack (TODO-445h): push/pop ordering,
 * top-layer-only Escape / outside-click, per-dialog backdrop pop (no sibling removal), focus trap + restore,
 * per-depth inline z-index, the dialog-kind depth cap (2), and the registry-regression case (dialog + popover =
 * two entries, one dialog).
 *
 *   Usage:  node popup-layer.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node popup-layer.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(I && typeof I.pushLayer === 'function' && typeof I.showActionDialog === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function drain() { while (I.topLayer()) I.popLayer(); }
const table = env.el('table');
const tr = env.el('tr');

// --- push/pop ordering + per-depth inline z-index --------------------------------------------------------------
(function () {
	const a = env.el('div'), b = env.el('div');
	I.pushLayer(a, { kind: 'popover', portal: true, lightDismiss: true });
	I.pushLayer(b, { kind: 'popover', portal: true, lightDismiss: true });
	out.push_topIsB = I.topLayer().el === b;
	out.push_portalledToBody = a.parentNode === env.body && a.style.position === 'fixed';
	out.z_increasesPerDepth = parseInt(b.style.zIndex, 10) > parseInt(a.style.zIndex, 10);
	out.z_dataLayerIndex = a.getAttribute('data-juneau-layer') === '0' && b.getAttribute('data-juneau-layer') === '1';
	I.popLayer();
	out.pop_topIsA = I.topLayer().el === a;
	I.popLayer();
	out.pop_empty = I.topLayer() === null;
	drain();
})();

// --- top-layer-only Escape: inner pops, outer stays; Escape preventDefaults ------------------------------------
(function () {
	const ctx = {};
	const outer = I.showActionDialog({ title: 'Outer' }, { id: 'a', label: 'A' }, table, tr, ctx);
	out.dc_afterOuter = I.dialogLayerCount();
	const inner = I.showActionDialog({ title: 'Inner' }, { id: 'b', label: 'B' }, table, tr, ctx);
	out.dc_afterInner = I.dialogLayerCount();
	const ev = env.dispatchDocument('keydown', { key: 'Escape' });
	out.esc_preventDefault = ev.defaultPrevented === true;
	out.esc_popsOne = I.dialogLayerCount() === 1;
	out.esc_innerDetached = inner.backdrop.parentNode == null;
	out.esc_outerStays = outer.backdrop.parentNode === env.body;
	drain();
})();

// --- per-dialog pop removes only THAT dialog's backdrop, not a sibling's ---------------------------------------
(function () {
	const ctx = {};
	const outer = I.showActionDialog({ title: 'Outer' }, { id: 'a' }, table, tr, ctx);
	const inner = I.showActionDialog({ title: 'Inner' }, { id: 'b' }, table, tr, ctx);
	I.popLayer(inner.backdrop);
	out.sibling_outerBackdropSurvives = env.body.contains(outer.backdrop) && !env.body.contains(inner.backdrop);
	drain();
})();

// --- outside-click dismisses only a light-dismiss top layer (a modal is not light-dismiss) ---------------------
(function () {
	const ctx = {};
	I.showActionDialog({ title: 'M' }, { id: 'm' }, table, tr, ctx);
	const outside = env.el('div'); env.body.appendChild(outside);
	env.dispatchDocument('pointerdown', { target: outside });
	out.modal_notLightDismissed = I.dialogLayerCount() === 1;
	const pop = env.el('div');
	I.pushLayer(pop, { kind: 'popover', portal: true, lightDismiss: true });
	env.dispatchDocument('pointerdown', { target: outside });
	out.popover_lightDismissed = I.topLayer() != null && I.topLayer().kind === 'dialog';
	drain();
})();

// --- focus trap into the dialog + focus restore to the invoking trigger on pop ---------------------------------
(function () {
	const trigger = env.el('button'); env.body.appendChild(trigger); trigger.focus();
	const ctx = {};
	const ui = I.showActionDialog({ title: 'F' }, { id: 'f' }, table, tr, ctx);
	out.trap_focusMovedIntoDialog = ui.backdrop.contains(env.getActive());
	I.popLayer(ui.backdrop);
	out.focus_restoredToTrigger = env.getActive() === trigger;
	drain();
})();

// --- registry regression: a dialog + a separately-triggered popover are TWO entries but ONE dialog ------------
(function () {
	const ctx = {};
	I.showActionDialog({ title: 'D' }, { id: 'd' }, table, tr, ctx);
	const pop = env.el('div');
	I.pushLayer(pop, { kind: 'popover', portal: true, lightDismiss: true });
	out.reg_dialogCountStill1 = I.dialogLayerCount() === 1;   // popover does NOT consume the dialog-kind cap
	out.reg_topIsPopover = I.topLayer().kind === 'popover';
	I.popLayer();   // pop the popover
	out.reg_dialogRemains = I.topLayer() != null && I.topLayer().kind === 'dialog';
	drain();
})();

// --- type=action opens a SECOND dialog without closing the first; third dialog is refused inside the top ------
(function () {
	const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate' }] } };
	const outer = I.showActionDialog({ title: 'Ack' }, { id: 'ack', label: 'Ack' }, table, tr, ctx);
	out.nested_before = I.dialogLayerCount();
	I.openFormActionDialog('esc', table, tr, ctx);   // esc is confirm-only (no form) -> local showActionDialog
	out.nested_after = I.dialogLayerCount();
	out.nested_outerStillOpen = env.body.contains(outer.backdrop);
	out.cap_max = I.MAX_DIALOG_DEPTH;
	// A THIRD dialog push must be refused - dialog-kind count stays 2, refusal painted into the current top dialog.
	I.openFormActionDialog('esc', table, tr, ctx);
	out.cap_staysAt2 = I.dialogLayerCount() === 2;
	const top = I.topLayer();
	out.cap_refusalInTopDialog = top != null && top.el.querySelector('.juneau-view-dialog-depth-refusal') != null;
	// A missing / non-dialog actionId is a visible refusal, not a throw.
	I.openFormActionDialog('does-not-exist', table, tr, ctx);
	out.missing_actionRefusal = top != null && top.el.querySelector('.juneau-view-dialog-action-refusal') != null;
	drain();
})();

process.stdout.write(JSON.stringify(out));
