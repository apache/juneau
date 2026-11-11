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
 * config-chooser-a11y.cjs - always-on Node harness for the View Settings dialog's tab ARIA wiring and
 * roving-tabindex keyboard nav. Unlike config-chooser.cjs's minimal no-op el() shim, this loads
 * juneau-views.js (for NS.init.detailTabTargetIndex) and juneau-config.js into views-dom-shim.cjs's real DOM —
 * real querySelector/dispatch/getAttribute/focus — so ARIA attributes and keydown-driven focus moves can be
 * asserted directly.
 *
 *   Usage:  node config-chooser-a11y.cjs <path-to-juneau-views.js> <path-to-juneau-config.js>
 */
'use strict';

const { loadScripts } = require('./views-dom-shim.cjs');

const viewsJsPath = process.argv[2];
const configJsPath = process.argv[3];
if (!viewsJsPath || !configJsPath) {
	console.error('usage: node config-chooser-a11y.cjs <juneau-views.js> <juneau-config.js>');
	process.exit(2);
}

const { env, NS } = loadScripts([viewsJsPath, configJsPath]);
const out = {};

// openChooser calls NS.persistence.list(table) unconditionally at the end (view-select population) — stub it so
// that call resolves instead of throwing; persistence itself is out of scope for this a11y-only harness.
NS.persistence = { list: function () { return Promise.resolve({ views: [] }); } };

const catalog = [
	{ data: 'name', title: 'Name', search: { type: 'text' }, orderable: true },
	{ data: 'status', title: 'Status', search: { type: 'id' }, orderable: true }
];

function openDialog() {
	const table = env.document.createElement('table');
	const ctx = { viewDef: { columns: catalog, defaultOrder: [{ data: 'name', dir: 'asc' }, { data: 'status', dir: 'asc' }] } };
	NS.config.openChooser(table, ctx);
	return { table: table, ctx: ctx };
}

// id-prefix uniqueness: a dialog's tab ids must never collide with a PREVIOUSLY opened dialog's tab ids (as
// if two tables on one page each got their Columns button clicked in turn).
const first = openDialog();
const firstIds = env.document.querySelectorAll('[role="tab"]').map(function (b) { return b.id; });
NS.config.closeChooserDialog(first.ctx);

const second = openDialog();
const secondIds = env.document.querySelectorAll('[role="tab"]').map(function (b) { return b.id; });
NS.config.closeChooserDialog(second.ctx);

out.firstIdsUnique = new Set(firstIds).size === firstIds.length;
out.secondIdsUnique = new Set(secondIds).size === secondIds.length;
out.idsNonEmpty = firstIds.concat(secondIds).every(function (id) { return typeof id === 'string' && id.length > 0; });
out.noOverlapAcrossDialogs = firstIds.every(function (id) { return secondIds.indexOf(id) < 0; });

// ARIA wiring on a fresh dialog: role=tablist, one role=tab per CONFIG_TABS entry (in order), each
// aria-controls a role=tabpanel with a matching aria-labelledby, and the View tab (the initial selection) is the
// only one selected/focusable.
const opened = openDialog();
out.hasTablist = !!env.document.querySelector('[role="tablist"]');
const tabs = env.document.querySelectorAll('[role="tab"]');
out.tabOrder = tabs.map(function (b) { return b.dataset.tab; });
out.ariaSelected = tabs.map(function (b) { return b.getAttribute('aria-selected'); });
out.tabIndexes = tabs.map(function (b) { return b.tabIndex; });
out.panelsMatchControls = tabs.every(function (b) {
	const panelId = b.getAttribute('aria-controls');
	const panel = env.document.getElementById(panelId);
	return panel?.getAttribute('role') === 'tabpanel' && panel.getAttribute('aria-labelledby') === b.id;
});

// roving tabindex keyboard nav (WAI-ARIA APG tabs pattern): ArrowRight from View moves to Search; ArrowLeft
// from Search goes back to View; ArrowLeft from View (the FIRST tab) wraps to Options (the LAST tab); Home/End
// jump straight to the first/last tab.  The keydown listener lives on the tablist container (matching the
// existing ribbon-strip/probe-group harnesses' own convention — see helpers.cjs's stripOf(wrapper).dispatch
// pattern), so the harness dispatches there too, passing the focused button as `target` since this DOM shim
// never bubbles events up from the button that would really have received the keydown.
const tablist = env.document.querySelector('[role="tablist"]');
function pressKey(key, focusedBtn) {
	focusedBtn.focus();
	tablist.dispatch('keydown', { key: key, target: focusedBtn, preventDefault: function () { /* no-op */ } });
}

pressKey('ArrowRight', opened.ctx._configTabButtons.view);
out.afterArrowRightActiveTab = opened.ctx._configActiveTab;
out.afterArrowRightFocused = env.getActive() === opened.ctx._configTabButtons.search;

pressKey('ArrowLeft', opened.ctx._configTabButtons.search);
out.afterArrowLeftBackToViewTab = opened.ctx._configActiveTab;

pressKey('ArrowLeft', opened.ctx._configTabButtons.view);
out.wrapArrowLeftFromViewActiveTab = opened.ctx._configActiveTab;
out.wrapArrowLeftFocusedOptions = env.getActive() === opened.ctx._configTabButtons.options;

pressKey('Home', opened.ctx._configTabButtons.options);
out.homeActiveTab = opened.ctx._configActiveTab;

pressKey('End', opened.ctx._configTabButtons.view);
out.endActiveTab = opened.ctx._configActiveTab;

// An unrelated key (the browser's own Tab-key focus order) must not be intercepted.
const beforeTabKeyActiveTab = opened.ctx._configActiveTab;
pressKey('Tab', opened.ctx._configTabButtons.view);
out.plainTabKeyIgnored = opened.ctx._configActiveTab === beforeTabKeyActiveTab;

// Close the dialog above so the dialogs below are the only ones in the document.
NS.config.closeChooserDialog(opened.ctx);

// columnConfig.tabs restricts the dialog to a subset of tabs, in CONFIG_TABS' fixed order, and the first
// VISIBLE tab (not always "view") becomes the initial selection.
function openRestrictedDialog(tabs) {
	const table = env.document.createElement('table');
	const ctx = { viewDef: { columns: catalog, columnConfig: { tabs: tabs } } };
	NS.config.openChooser(table, ctx);
	return { table: table, ctx: ctx };
}
function pressKeyOn(tablistEl, key, focusedBtn) {
	focusedBtn.focus();
	tablistEl.dispatch('keydown', { key: key, target: focusedBtn, preventDefault: function () { /* no-op */ } });
}

const restricted = openRestrictedDialog(['search', 'sort']);
out.restrictedTabOrder = env.document.querySelectorAll('[role="tab"]').map(function (b) { return b.dataset.tab; });
out.restrictedInitialActiveTab = restricted.ctx._configActiveTab;
out.restrictedPanelCount = env.document.querySelectorAll('[role="tabpanel"]').length;
// The handler navigates from the ACTIVE tab, so make the last visible tab active before pressing ArrowRight.
NS.config.selectConfigTab(restricted.ctx, 'sort');
pressKeyOn(env.document.querySelector('[role="tablist"]'), 'ArrowRight', restricted.ctx._configTabButtons.sort);
out.restrictedWrapArrowRightFromLastTab = restricted.ctx._configActiveTab;
NS.config.closeChooserDialog(restricted.ctx);

// columnConfig: true (unrestricted) still gets all four, defaulting to "view", exactly as before.
const unrestricted = openDialog();
out.unrestrictedTabOrder = env.document.querySelectorAll('[role="tab"]').map(function (b) { return b.dataset.tab; });
out.unrestrictedInitialActiveTab = unrestricted.ctx._configActiveTab;
NS.config.closeChooserDialog(unrestricted.ctx);

// The Sort tab is an ordered, directional priority list: reordering and direction controls work, and an
// unchecked row drops out of draft.sort while an unsorted row's checkbox appends it back at the tail.
const sortDialog = openDialog();
NS.config.selectConfigTab(sortDialog.ctx, 'sort');
// (views-dom-shim.cjs supports only one simple selector per query - no descendant combinators or multi-class
// matching - so rows are found by class and narrowed by data-col in JS, then queried for their own controls.)
const sortRowEls = function (unsorted) {
	return env.document.querySelectorAll('.juneau-config-sort-row').filter(function (row) {
		return ((' ' + row.className + ' ').indexOf(' juneau-config-sort-row-unsorted ') >= 0) === unsorted;
	});
};
const sortRows = function () { return sortRowEls(false).map(function (row) { return row.dataset.col; }); };
const sortRowFor = function (unsorted, col) {
	return sortRowEls(unsorted).find(function (row) { return row.dataset.col === col; });
};
out.sortTabInitialOrder = sortRows();

const nameDirSelect = sortRowFor(false, 'name').querySelector('.juneau-config-sort-dir');
nameDirSelect.value = 'desc';
nameDirSelect.dispatch('change');
out.sortTabDirAfterChange = sortDialog.ctx._configDraft.sort.find(function (e) { return e.column === 'name'; }).dir;

const nameToggle = sortRowFor(false, 'name').querySelector('.juneau-config-sort-toggle');
nameToggle.checked = false;
nameToggle.dispatch('change');
out.sortTabAfterUncheckOrder = sortRows();
out.sortTabAfterUncheckDraft = sortDialog.ctx._configDraft.sort.map(function (e) { return e.column; });

const nameUnsortedToggle = sortRowFor(true, 'name').querySelector('.juneau-config-sort-toggle');
nameUnsortedToggle.checked = true;
nameUnsortedToggle.dispatch('change');
out.sortTabAfterRecheckDraft = sortDialog.ctx._configDraft.sort.map(function (e) { return e.column; });
NS.config.closeChooserDialog(sortDialog.ctx);

// Gear: the chooser header carries a title-icon figure; a plain dialog (no titleIcon) has none.
NS.icons = { resolveIcon: function (name) { return '<svg data-icon="' + name + '"></svg>'; } };
const gearDialog = openDialog();
out.gearPresent = env.document.querySelectorAll('.juneau-view-dialog-title-icon').length === 1;
NS.config.closeChooserDialog(gearDialog.ctx);
const plain = NS.init.buildDialogOverlay({ title: 'Plain' }, null, env.document.createElement('table'), null, {}, 'plain-1');
out.plainNoTitleIcon = plain.dialog.querySelectorAll('.juneau-view-dialog-title-icon').length === 0;
const actionDlg = NS.init.buildDialogOverlay({ title: 'Act', titleIcon: 'settings' }, { type: 'dialog' }, env.document.createElement('table'), null, {}, 'plain-2');
out.actionFigureIsNotTitleIcon = actionDlg.dialog.querySelectorAll('.juneau-view-dialog-title-icon').length === 0;

// Left pane: Find box, hidden title, grips, draggable Available rows.
const dragCatalog = [
	{ data: 'name', title: 'Name', orderable: true },
	{ data: 'status', title: 'Status', orderable: true },
	{ data: 'notes', title: 'Notes', defaultVisible: false },
	{ data: 'owner', title: 'Owner', defaultVisible: false }
];
function openDragDialog() {
	const table = env.document.createElement('table');
	const ctx = { viewDef: { columns: dragCatalog, defaultOrder: [{ data: 'name', dir: 'asc' }] } };
	NS.config.openChooser(table, ctx);
	return { table: table, ctx: ctx };
}
const leftDlg = openDragDialog();
out.findPlaceholder = env.document.querySelector('.juneau-config-find').placeholder;
const availTitle = env.document.querySelector('.juneau-config-pane-title');
out.paneTitleHidden = availTitle.className.indexOf('juneau-config-sr-only') >= 0;
out.listboxStillLabelled = env.document.querySelector('.juneau-config-avail-list').getAttribute('aria-labelledby') === availTitle.id;
const availRows = env.document.querySelectorAll('.juneau-config-avail');
out.availKeys = availRows.map(function (r) { return r.dataset.col; });
out.availDraggable = availRows.every(function (r) { return r.getAttribute('draggable') === 'true'; });
out.availHaveGrip = availRows.every(function (r) { return r.querySelectorAll('.juneau-config-grip').length === 1; });
const gridHandles = env.document.querySelectorAll('.juneau-config-col-handle');
out.gridHandleHasGrip = gridHandles.length === 2 && gridHandles.every(function (h) { return h.querySelector('.juneau-config-grip') != null; });
NS.config.closeChooserDialog(leftDlg.ctx);

// Drag from Available into the grid.
function dAvail(key) { return env.document.querySelectorAll('.juneau-config-avail').find(function (r) { return r.dataset.col === key; }); }
function dSel(key) { return env.document.querySelectorAll('.juneau-config-col-row').find(function (r) { return r.dataset.col === key; }); }
function dKeys() { return env.document.querySelectorAll('.juneau-config-col-row').map(function (r) { return r.dataset.col; }); }
function dnd() { return { effectAllowed: '', dropEffect: '', setData: function () { /* no-op */ } }; }
const noop = function () { /* no-op */ };
function dropOn(el, y) { el.getBoundingClientRect = function () { return { top: 0, height: 10 }; }; el.dispatch('drop', { clientY: y, preventDefault: noop, stopPropagation: noop }); }

const d1 = openDragDialog();
out.dragInitialSelected = dKeys();
dAvail('notes').dispatch('dragstart', { dataTransfer: dnd() });
dropOn(dSel('status'), 2);
out.dropBefore = dKeys();
out.dropBeforeDirty = d1.ctx._configDirty === true;
out.rowDropRefocuses = env.getActive() === dSel('notes').querySelector('.juneau-config-col-handle');
out.dropBeforeAvail = env.document.querySelectorAll('.juneau-config-avail').map(function (r) { return r.dataset.col; });
NS.config.closeChooserDialog(d1.ctx);

const d2 = openDragDialog();
dAvail('owner').dispatch('dragstart', { dataTransfer: dnd() });
dropOn(dSel('name'), 8);
out.dropAfter = dKeys();
NS.config.closeChooserDialog(d2.ctx);

const d3 = openDragDialog();
dAvail('notes').dispatch('dragstart', { dataTransfer: dnd() });
env.document.querySelector('.juneau-config-col-list').dispatch('drop', { preventDefault: noop, stopPropagation: noop });
out.dropAppend = dKeys();
out.listDropRefocuses = env.getActive() === dSel('notes').querySelector('.juneau-config-col-handle');
NS.config.closeChooserDialog(d3.ctx);

const d4 = openDragDialog();
dropOn(dSel('status'), 2);
env.document.querySelector('.juneau-config-col-list').dispatch('drop', { preventDefault: noop, stopPropagation: noop });
out.dropNoDragNoop = dKeys();
out.dropNoDragClean = d4.ctx._configDirty !== true;
NS.config.closeChooserDialog(d4.ctx);

const d5 = openDragDialog();
dAvail('owner').dispatch('dblclick');
out.dblclickAdds = dKeys();
NS.config.closeChooserDialog(d5.ctx);

// A selected-row reorder drag still works.
const d6 = openDragDialog();
dSel('name').dispatch('dragstart', { dataTransfer: dnd() });
dropOn(dSel('status'), 8);
out.reorderStillWorks = dKeys();
NS.config.closeChooserDialog(d6.ctx);

// Detail strip.
const detailCatalog = [
	{ data: 'name', title: 'Name', orderable: true, search: { type: 'text' }, description: 'The display name.' },
	{ data: 'danger', title: '<img src=x onerror=alert(1)>', orderable: false, defaultVisible: false, formats: ['date', 'ts-zulu'], description: '<b>x</b>' },
	{ data: 'id', title: 'Id', pinned: true, defaultVisible: false }
];
const detTable = env.document.createElement('table');
const detCtx = { viewDef: { columns: detailCatalog, defaultOrder: [{ data: 'name', dir: 'asc' }] } };
NS.config.openChooser(detTable, detCtx);
const strip = env.document.querySelector('.juneau-config-detail');
out.detailRole = strip.getAttribute('role');
out.detailLive = strip.getAttribute('aria-live');
out.detailDefault = strip.textContent;
dSel('name').dispatch('click');
out.detailSelected = strip.textContent;
dAvail('danger').dispatch('click');
out.detailAvail = strip.textContent;
out.detailAvailKey = detCtx._configDetailKey;
out.detailHostileIsText = strip.querySelectorAll('img').length === 0;
dSel('id').dispatch('click');
out.detailPinned = strip.textContent;
NS.config.closeChooserDialog(detCtx);
out.detailKeyCleared = detCtx._configDetailKey == null;
out.detailTextHelperNoCol = NS.config.columnDetailText(null);
out.detailSearchableFlag = NS.config.columnDetailText({ data: 'q', searchable: true }).indexOf('not searchable') < 0;
const rep = env.document.createElement('table');
const repCtx = { viewDef: { columns: [{ data: 'a', title: 'A' }, { data: 'b', title: 'B', defaultVisible: false, description: 'old' }], defaultOrder: [{ data: 'a', dir: 'asc' }] } };
NS.config.openChooser(rep, repCtx);
dAvail('b').dispatch('click');
repCtx.viewDef.columns[1].description = 'fresh';
dAvail('b').dispatch('dblclick');
out.detailRepaintedAfterAdd = env.document.querySelector('.juneau-config-detail').textContent.indexOf('fresh') >= 0;
NS.config.closeChooserDialog(repCtx);

// Reset button.
const resetDlg = openDragDialog();
const resetEl = env.document.querySelector('.juneau-config-reset');
out.resetNotLink = resetEl.className.indexOf('jc-btn-link') < 0 && resetEl.className.indexOf('jc-btn') >= 0;
resetDlg.ctx._configDraft.visible = ['name'];
resetEl.dispatch('click');
out.resetDraftsDefaults = resetDlg.ctx._configDraft.visible.join('/');
NS.config.closeChooserDialog(resetDlg.ctx);

/** Save and Save as sit in the row itself, never inside the "..." menu. */
function menu0Free(bar) {
	const menu = bar.querySelector('.juneau-config-saved-menu');
	return menu.querySelectorAll('.juneau-config-save').length === 0 && menu.querySelectorAll('.juneau-config-saveas').length === 0;
}

(async function () {
	// Compact saved-views row.
	NS.persistence = { list: function () { return Promise.resolve({ views: [] }); } };
	const compactDlg = openDragDialog();
	await Promise.resolve(); await Promise.resolve();
	const bar = env.document.querySelector('.juneau-config-saved-bar');
	out.rowHoldsSaveAndSaveAs = bar.querySelectorAll('.juneau-config-save').length === 1
		&& bar.querySelectorAll('.juneau-config-saveas').length === 1
		&& menu0Free(bar);
	const moreBtn = env.document.querySelector('.juneau-config-saved-more');
	const menu = env.document.querySelector('.juneau-config-saved-menu');
	out.menuClosedInitially = menu.dataset.open !== 'true';
	moreBtn.dispatch('click');
	out.menuOpenAfterMore = menu.dataset.open === 'true' && moreBtn.getAttribute('aria-expanded') === 'true';
	out.menuHoldsActions = menu.querySelectorAll('.juneau-config-rename').length === 1
		&& menu.querySelectorAll('.juneau-config-delete').length === 1;
	out.menuNoHaspopup = moreBtn.getAttribute('aria-haspopup') == null;
	env.dispatchDocument('click', { target: env.document.body });
	out.menuClosesOnOutsideClick = menu.dataset.open !== 'true' && moreBtn.getAttribute('aria-expanded') === 'false';
	moreBtn.dispatch('click');
	let escStopped = false;
	bar.dispatch('keydown', { key: 'Escape', stopPropagation: function () { escStopped = true; } });
	out.menuClosesOnEscape = menu.dataset.open !== 'true' && escStopped;
	out.escapeRefocusesMore = env.getActive() === moreBtn;
	env.setActive(null);
	moreBtn.dispatch('click');
	const renameEl = menu.querySelector('.juneau-config-rename');
	menu.dispatch('click', { target: renameEl });
	out.pickClosesMenu = menu.dataset.open !== 'true';
	NS.config.closeChooserDialog(compactDlg.ctx);

	NS.persistence = { list: function () { return Promise.resolve({ views: [{ name: 'Mine' }] }); } };
	const fullDlg = openDragDialog();
	await Promise.resolve(); await Promise.resolve();
	NS.config.closeChooserDialog(fullDlg.ctx);
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e); process.exit(1); });

