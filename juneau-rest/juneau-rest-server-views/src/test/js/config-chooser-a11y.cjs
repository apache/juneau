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
 * roving-tabindex keyboard nav (F3). Unlike config-chooser.cjs's minimal no-op el() shim, this loads
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

// F3 — id-prefix uniqueness: a dialog's tab ids must never collide with a PREVIOUSLY opened dialog's tab ids (as
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

// F3 — ARIA wiring on a fresh dialog: role=tablist, one role=tab per CONFIG_TABS entry (in order), each
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

// F3 — roving tabindex keyboard nav (WAI-ARIA APG tabs pattern): ArrowRight from View moves to Search; ArrowLeft
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

// Close the F3 dialog above so the dialogs below are the only ones in the document.
NS.config.closeChooserDialog(opened.ctx);

// Gap-g - columnConfig.tabs restricts the dialog to a subset of tabs, in CONFIG_TABS' fixed order, and the first
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

// columnConfig: true (unrestricted) still gets all four, defaulting to "view", exactly as before Gap-g.
const unrestricted = openDialog();
out.unrestrictedTabOrder = env.document.querySelectorAll('[role="tab"]').map(function (b) { return b.dataset.tab; });
out.unrestrictedInitialActiveTab = unrestricted.ctx._configActiveTab;
NS.config.closeChooserDialog(unrestricted.ctx);

// Gap 6 - the Sort tab is an ordered, directional priority list: reordering and direction controls work, and an
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

process.stdout.write(JSON.stringify(out));
