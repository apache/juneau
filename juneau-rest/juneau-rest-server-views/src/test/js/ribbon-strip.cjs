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
 * ribbon-strip.cjs - always-on Node harness for the SHARED ribbon-format strip builder and its two callers.
 *
 * Three things are proved here:
 *
 *   1. buildRibbonStrip is a real generic: given {id, label, pane} tuples it wires role=tab / aria-selected /
 *      aria-controls / role=tabpanel / aria-labelledby / roving tabindex, moves selection on
 *      Left/Right/Home/End (wrapping), ignores keys it does not own, and fires onActivate exactly once per
 *      activation - with no knowledge of row details or dialogs.
 *
 *   2. buildDetailStrip, now a thin caller of that builder, still emits DOM that is IDENTICAL to what it emitted
 *      before: the harness serializes the whole panel (element order, attribute insertion order, the properties the
 *      runtime sets rather than attributes) and the Java side pins it against an exact expected string.
 *
 *   3. A SECTIONED dialog form is built by the generic builder, and buildDetailStrip is NOT on that call path (the
 *      export is spied and must stay at zero calls), while a nested dialog opened from inside a sectioned dialog
 *      still refuses at MAX_DIALOG_DEPTH rather than stacking a third overlay.
 *
 *   Usage:  node ribbon-strip.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node ribbon-strip.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: !!(I && typeof I.buildRibbonStrip === 'function' && typeof I.buildDetailStrip === 'function')
};
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/**
 * Canonical serialization of a subtree: tag, attributes in INSERTION order, then the properties the runtime sets
 * directly rather than through setAttribute (class, hidden, tabindex, input type), then text, then children.  A
 * change in the order the builder sets things therefore shows up as a diff, which is what makes this usable as a
 * byte-for-byte pin on the refactor.
 */
function dump(n, depth) {
	if (!n || n.nodeType !== 1) return '';
	const parts = [];
	Object.keys(n.attrs).forEach(function (k) { parts.push(k + '="' + n.attrs[k] + '"'); });
	if (n.className) parts.push('.class="' + n.className + '"');
	if (n.hidden === true) parts.push('.hidden');
	if (n.tabIndex != null) parts.push('.tabindex=' + n.tabIndex);
	if (n._type != null) parts.push('.type="' + n._type + '"');
	let s = new Array(depth + 1).join('  ') + '<' + n.tagName.toLowerCase()
		+ (parts.length ? ' ' + parts.join(' ') : '') + '>';
	if (n.childNodes.length === 0 && n._text) s += n._text;
	s += '\n';
	for (let i = 0; i < n.childNodes.length; i++) s += dump(n.childNodes[i], depth + 1);
	return s;
}

function tabsOf(strip) {
	return strip.childNodes.filter(function (c) { return c.getAttribute && c.getAttribute('role') === 'tab'; });
}

function detailSection(sid, title) {
	const sec = env.el('section');
	sec.setAttribute('data-juneau-detail-section', sid);
	sec.className = 'juneau-view-detail-section';
	const h2 = env.el('h2');
	h2.className = 'juneau-view-detail-section-title';
	h2.textContent = title;
	sec.appendChild(h2);
	return sec;
}

function detailPanel(pairs) {
	const panel = env.el('div');
	panel.className = 'juneau-view-detail-panel';
	pairs.forEach(function (p) { panel.appendChild(detailSection(p[0], p[1])); });
	return panel;
}

// ------------------------------------------------------------------------------------------------------------------
// 2) buildDetailStrip's DOM, pinned.  FIRST buildDetailStrip call in this process, so detailStripSeq is 1 and the
//    minted ids are stable - the pin is on the shape and the ordering, not on a run-dependent counter.
// ------------------------------------------------------------------------------------------------------------------

const pinned = detailPanel([['overview', 'Overview'], ['context', 'Context']]);
I.buildDetailStrip(pinned);
out.detailStrip_dump = dump(pinned, 0);

// ------------------------------------------------------------------------------------------------------------------
// 1) The generic builder, driven directly - no panel, no dialog, no titles to hide.
// ------------------------------------------------------------------------------------------------------------------

const panes = [env.el('div'), env.el('div'), env.el('div')];
let activations = [];
const generic = I.buildRibbonStrip([
	{ id: 'one', label: 'One', pane: panes[0] },
	{ id: 'two', label: 'Two', pane: panes[1] },
	{ id: 'three', label: 'Three', pane: panes[2] }
], {
	className: 'juneau-view-ribbon-group juneau-view-test-strip',
	testId: 'test-strip',
	tabId: function (i) { return 'tt-' + i; },
	paneId: function (i) { return 'pp-' + i; },
	onActivate: function (id, pane) { activations.push({ id: id, pane: pane }); }
});
out.generic_returnsStripTabsActivate = !!(generic && generic.strip && generic.tabs && typeof generic.activate === 'function');
const gStrip = generic.strip;
const gTabs = tabsOf(gStrip);

out.generic_stripRole = gStrip.getAttribute('role');
out.generic_stripMode = gStrip.getAttribute('data-juneau-strip-mode');
out.generic_stripTestId = gStrip.getAttribute('data-testid');
out.generic_stripClass = gStrip.className;
out.generic_tabCount = gTabs.length;
out.generic_labels = gTabs.map(function (b) { return b.textContent; }).join(',');
out.generic_btnClass = gTabs[0].className;
out.generic_tabIds = gTabs.map(function (b) { return b.id; }).join(',');
out.generic_paneIds = panes.map(function (p) { return p.id; }).join(',');
out.generic_ariaSelected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.generic_tabindexes = gTabs.map(function (b) { return b.tabIndex; }).join(',');
out.generic_ariaControls = gTabs.map(function (b) { return b.getAttribute('aria-controls'); }).join(',');
out.generic_paneRoles = panes.map(function (p) { return p.getAttribute('role'); }).join(',');
out.generic_paneLabelledby = panes.map(function (p) { return p.getAttribute('aria-labelledby'); }).join(',');
out.generic_paneTabindexAttr = panes.map(function (p) { return p.getAttribute('tabindex'); }).join(',');
out.generic_paneHidden = panes.map(function (p) { return p.hidden === true; }).join(',');
out.generic_stripDetached = gStrip.parentNode === null;
out.generic_stripHasNoDetailTestId = gStrip.getAttribute('data-testid') !== 'detail-tabs';

// The activation callback fires EXACTLY once per activation, with the activated pane.
activations = [];
env.setActive(null);
gStrip.dispatch('keydown', { key: 'ArrowRight', preventDefault: function () {} });
out.kbd_right_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.kbd_right_focus = env.getActive() === gTabs[1];
out.kbd_right_activationCount = activations.length;
out.kbd_right_activationId = activations.length ? activations[0].id : null;
out.kbd_right_activationPane = activations.length ? activations[0].pane === panes[1] : false;

activations = [];
gStrip.dispatch('keydown', { key: 'End', preventDefault: function () {} });
out.kbd_end_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.kbd_end_activationCount = activations.length;

activations = [];
gStrip.dispatch('keydown', { key: 'ArrowRight', preventDefault: function () {} });   // wraps End -> first
out.kbd_rightWrap_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');

activations = [];
gStrip.dispatch('keydown', { key: 'ArrowLeft', preventDefault: function () {} });    // wraps first -> last
out.kbd_leftWrap_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');

activations = [];
gStrip.dispatch('keydown', { key: 'Home', preventDefault: function () {} });
out.kbd_home_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.kbd_home_activationCount = activations.length;

activations = [];
gStrip.dispatch('keydown', { key: 'Enter', preventDefault: function () {} });
out.kbd_unhandled_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.kbd_unhandled_activationCount = activations.length;

activations = [];
gStrip.dispatch('click', { target: gTabs[2] });
out.click_selected = gTabs.map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
out.click_activationCount = activations.length;
out.click_activationId = activations.length ? activations[0].id : null;
out.click_paneVisible = panes[2].hidden === false;
out.click_othersHidden = panes[0].hidden === true && panes[1].hidden === true;

// Explicit activeIndex, and a pane-less strip (a strip is allowed to be pure navigation).
const shifted = I.buildRibbonStrip(
	[{ id: 'a', label: 'A', pane: env.el('div') }, { id: 'b', label: 'B', pane: env.el('div') }],
	{ activeIndex: 1 });
out.activeIndex_selected = tabsOf(shifted.strip).map(function (b) { return b.getAttribute('aria-selected'); }).join(',');
const paneless = I.buildRibbonStrip([{ id: 'x', label: 'X' }], {});
out.paneless_built = !!(paneless && tabsOf(paneless.strip).length === 1);
out.paneless_tabControlsMintedId = tabsOf(paneless.strip)[0].getAttribute('aria-controls') === 'juneau-strip-pane-0';
out.empty_returnsNull = I.buildRibbonStrip([], {}) === null;
out.null_returnsNull = I.buildRibbonStrip(null, {}) === null;

// ------------------------------------------------------------------------------------------------------------------
// 3) A sectioned dialog form: generic builder, and buildDetailStrip NOT on the call path.
// ------------------------------------------------------------------------------------------------------------------

// Spy the export before painting anything: the dialog path must never route through it.
let detailStripCalls = 0;
const realBuildDetailStrip = I.buildDetailStrip;
I.buildDetailStrip = function () { detailStripCalls++; return realBuildDetailStrip.apply(null, arguments); };

const table = env.el('table');
const tr = env.el('tr');
const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate' }] } };

const sectionedDialog = env.el('div');
I.appendDialogForm(sectionedDialog, {
	sections: [
		{ id: 'basics', label: 'Basics', fields: [
			{ name: 'title', type: 'text', label: 'Title', value: 'T', required: true }
		] },
		{ id: 'advanced', label: 'Advanced', fields: [
			{ name: 'notes', type: 'textarea', label: 'Notes', value: 'N' },
			{ name: 'esc', type: 'action', label: 'Escalate', actionId: 'esc' }
		] }
	]
}, table, tr, ctx, 7);

const wrap = sectionedDialog.querySelector('[data-testid="dialog-form"]');
out.sectioned_wrapPresent = wrap != null;
const dStrip = sectionedDialog.querySelector('[data-testid="dialog-sections"]');
out.sectioned_stripPresent = dStrip != null;
out.sectioned_stripRole = dStrip ? dStrip.getAttribute('role') : null;
out.sectioned_stripMode = dStrip ? dStrip.getAttribute('data-juneau-strip-mode') : null;
out.sectioned_stripClass = dStrip ? dStrip.className : null;
out.sectioned_stripIsFirstChildOfWrap = wrap != null && wrap.firstChild === dStrip;
out.sectioned_noDetailTestId = sectionedDialog.querySelector('[data-testid="detail-tabs"]') === null;
out.sectioned_buildDetailStripNotCalled = detailStripCalls === 0;

const dTabs = dStrip ? tabsOf(dStrip) : [];
out.sectioned_tabCount = dTabs.length;
out.sectioned_labels = dTabs.map(function (b) { return b.textContent; }).join(',');
out.sectioned_tabIds = dTabs.map(function (b) { return b.id; }).join(',');
const dPanes = sectionedDialog.querySelectorAll('[data-juneau-form-section]');
out.sectioned_paneCount = dPanes.length;
out.sectioned_paneIds = dPanes.map(function (p) { return p.getAttribute('data-juneau-form-section'); }).join(',');
out.sectioned_pane0Visible = dPanes.length ? dPanes[0].hidden === false : false;
out.sectioned_pane1Hidden = dPanes.length > 1 ? dPanes[1].hidden === true : false;
out.sectioned_pane0Role = dPanes.length ? dPanes[0].getAttribute('role') : null;

// Field element ids are the flat form's, so nothing about collection or validation changes.
const titleCtl = sectionedDialog.querySelector('[data-juneau-form-field="title"]');
const notesCtl = sectionedDialog.querySelector('[data-juneau-form-field="notes"]');
out.sectioned_fieldIdShape = titleCtl.id === 'juneau-dialog-field-7-title' && notesCtl.id === 'juneau-dialog-field-7-notes';
out.sectioned_errorSiblingsPerSection = dPanes.length > 1
	&& dPanes[0].querySelector('[data-juneau-error-for="title"]') != null
	&& dPanes[1].querySelector('[data-juneau-error-for="notes"]') != null;

// Values survive a trip through another section: nothing is re-created, only hidden.
notesCtl.value = 'typed into a hidden section';
dStrip.dispatch('click', { target: dTabs[1] });
dStrip.dispatch('click', { target: dTabs[0] });
out.sectioned_valuePreservedAcrossSwitch = notesCtl.value === 'typed into a hidden section';
out.sectioned_collectSpansHiddenSections = (function () {
	const c = I.collectDialogFormFields(sectionedDialog);
	return c.title === 'T' && c.notes === 'typed into a hidden section' && !Object.hasOwn(c, 'esc');
})();

// A required control emptied in a HIDDEN section still fails validation, and the confirm-time pass reveals its
// own section rather than focusing something invisible.
dStrip.dispatch('click', { target: dTabs[1] });          // show 'advanced', hiding 'basics'
titleCtl.value = '';
out.sectioned_validateFailsFromHiddenSection = I.validateDialogForm(sectionedDialog, true) === false;
out.sectioned_errorAttachedToOwnSection =
	dPanes[0].querySelector('[data-juneau-error-for="title"]').textContent.length > 0
	&& dPanes[1].querySelector('[data-juneau-error-for="notes"]').textContent === '';
out.sectioned_ownSectionRevealed = dPanes[0].hidden === false && dPanes[1].hidden === true;

// A flat form is untouched by any of this: no strip, no section panes.
const flatDialog = env.el('div');
I.appendDialogForm(flatDialog, { fields: [{ name: 'only', type: 'text', label: 'Only' }] }, table, tr, ctx, 8);
out.flat_noStrip = flatDialog.querySelector('[data-testid="dialog-sections"]') === null;
out.flat_noSectionPanes = flatDialog.querySelectorAll('[data-juneau-form-section]').length === 0;
out.flat_stillPaintsTheRow = flatDialog.querySelector('[data-juneau-form-field="only"]') != null;
out.flat_buildDetailStripStillNotCalled = detailStripCalls === 0;

// ------------------------------------------------------------------------------------------------------------------
// Depth accounting: a sectioned dialog is ONE dialog.  Opening from inside it reaches the cap, and the third
// attempt is the in-dialog refusal, not a third overlay.
// ------------------------------------------------------------------------------------------------------------------

const V = I.JUNEAU_DIALOG_FORM_CONTRACT_VERSION;
const SECTIONED_MODAL = {
	contractVersion: V, title: 'Sectioned',
	form: { contractVersion: V, sections: [
		{ id: 'basics', label: 'Basics', fields: [{ name: 'title', type: 'text', label: 'Title' }] },
		{ id: 'more', label: 'More', fields: [{ name: 'go', type: 'action', label: 'Go', actionId: 'esc' }] }
	] }
};
const depthCtx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate' }] } };
const dtr = env.el('tr');

// One sectioned dialog is ONE dialog-kind layer: the strip is layout, not a layer.
const opened = I.showActionDialog(SECTIONED_MODAL, { id: 'esc', label: 'Escalate' }, table, dtr, depthCtx);
out.depth_capIsTwo = I.MAX_DIALOG_DEPTH === 2;
out.depth_sectionedIsOneLayer = I.dialogLayerCount() === 1;
out.depth_sectionedDialogHasStrip = opened.dialog.querySelector('[data-testid="dialog-sections"]') != null;
out.depth_sectionedDialogTabCount = opened.dialog.querySelectorAll('[role="tab"]').length;

// A second dialog opened from inside it stacks normally and reaches the cap...
I.openFormActionDialog('esc', table, dtr, depthCtx);
out.depth_secondStacks = I.dialogLayerCount() === 2;
out.depth_outerStillOpen = env.body.contains(opened.backdrop);

// ...and the third is the in-dialog refusal, never a third overlay.
I.openFormActionDialog('esc', table, dtr, depthCtx);
out.depth_thirdRefused = I.dialogLayerCount() === 2;
const topEl = I.topLayer() ? I.topLayer().el : null;
const refusal = topEl ? topEl.querySelector('.juneau-view-dialog-depth-refusal') : null;
out.depth_refusalInTopDialog = refusal != null;
out.depth_refusalNamesTheCap = refusal != null && String(refusal.textContent).indexOf('2') >= 0;
out.depth_buildDetailStripNeverCalled = detailStripCalls === 0;
while (I.topLayer()) I.popLayer();

I.buildDetailStrip = realBuildDetailStrip;
process.stdout.write(JSON.stringify(out));
