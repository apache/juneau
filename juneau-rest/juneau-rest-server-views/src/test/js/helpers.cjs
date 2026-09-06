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
 * helpers.cjs - always-on Node behavioral harness for juneau-helpers.js (WORK-J0522b, design §16.4 tests
 * 28/28a/28b/29/30/31).  Every assertion lives in the paired Java test; this script only computes facts.
 *
 *   Usage:  node helpers.cjs <juneau-renders.js> <juneau-views.js> <juneau-helpers.js>
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'helpers-harness.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const helpersJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !helpersJsPath) {
	console.error('usage: node helpers.cjs <juneau-renders.js> <juneau-views.js> <juneau-helpers.js>');
	process.exit(2);
}

const { env, H } = load(rendersJsPath, viewsJsPath, helpersJsPath);
const out = { hasHelpers: !!H };

function textOf(el) { return el.textContent; }
function throws(fn) { try { fn(); return null; } catch (e) { return e.message || String(e); } }

(async function () {

// ================================================================================================
// fieldGrid - test 28a / 28b
// ================================================================================================
(function fieldGridTests() {
	// (i) default + catalog joined against values-only payload -> labeled TEXT grid
	const catalog1 = [{ data: 'name', label: 'Name' }, { data: 'owner', label: 'Owner' }];
	const grid1 = H.fieldGrid(catalog1, { values: { name: 'alerts-primary', owner: 'Platform' } });
	const fields1 = grid1.querySelectorAll('.juneau-view-detail-field');
	out.fieldGrid_fieldCount = fields1.length;
	out.fieldGrid_titles = fields1.map(f => textOf(f.querySelector('.juneau-view-detail-field-title')));
	out.fieldGrid_values = fields1.map(f => textOf(f.querySelector('[data-juneau-field]')));

	// (ii) a catalog entry whose data key is ABSENT from values renders EMPTY, never "undefined"
	const grid2 = H.fieldGrid([{ data: 'missing', label: 'Missing' }], { values: { other: 'x' } });
	out.fieldGrid_missingKeyRendersEmpty = textOf(grid2.querySelector('[data-juneau-field]')) === '';

	// (iii) a values key with NO catalog entry is dropped (no crash, and it is simply absent from output)
	const grid3 = H.fieldGrid([{ data: 'a', label: 'A' }], { values: { a: '1', extra: '2' } });
	out.fieldGrid_extraValuesKeyDropped = grid3.querySelectorAll('[data-juneau-field]').length === 1;

	// format dispatch: markdown / sanitizedHtml route through the SAME copiers juneau-views.js exports
	const mdGrid = H.fieldGrid([{ data: 'body', label: 'Body', format: 'markdown' }], { values: { body: 'hi' } });
	out.fieldGrid_markdown_text = textOf(mdGrid.querySelector('[data-juneau-field]'));

	// render dispatch through the EXISTING renderer registry, with a throwing renderer falling back to textContent
	env.window.JuneauViews.registerRenderer('helpersTestThrows', { display: function () { throw new Error('boom'); } });
	const renderGrid = H.fieldGrid([{ data: 'x', label: 'X', render: 'helpersTestThrows' }], { values: { x: '7' } });
	out.fieldGrid_throwingRendererFallsBackToText = textOf(renderGrid.querySelector('[data-juneau-field]')) === '7';

	// opts.renderers: a per-call scoped override.  This sandbox has no DOMParser, so fillRenderSlot's OWN
	// documented fail-closed behavior (missing parser -> render the raw value, never the renderer's HTML)
	// means the override's and the global renderer's PAINTED text are indistinguishable here - that half
	// of the behavior is exercised for real by the "*-browser.cjs" harnesses that carry a real DOMParser.
	// What THIS sandbox can assert directly, and what test 29's purity scan actually cares about, is the
	// swap-and-restore itself: the temporary `NS.registerRenderer` swap inside fieldGrid leaves the GLOBAL
	// registry holding the EXACT SAME renderer object it held before the call - no page-global state
	// survives past a single fill, checked by identity rather than by repainting.
	const NSref = env.window.JuneauViews;
	NSref.registerRenderer('scopedOne', { display: function (v) { return 'GLOBAL:' + v; } });
	const priorRenderer = NSref.resolveRenderer('scopedOne');
	H.fieldGrid([{ data: 'y', label: 'Y', render: 'scopedOne' }], {
		values: { y: '9' },
		renderers: { scopedOne: { display: function (v) { return 'OVERRIDE:' + v; } } }
	});
	out.fieldGrid_scopedRendererOverrideRestoredByIdentity = NSref.resolveRenderer('scopedOne') === priorRenderer;

	// href wraps the plain-text value node in a safe <a>; an unsafe href does NOT wrap
	const hrefGrid = H.fieldGrid([{ data: 'id', label: 'Id', href: '/widgets/{id}' }], { values: { id: '42' } });
	const link = hrefGrid.querySelector('a');
	out.fieldGrid_hrefWrapsSafeUrl = !!link && link.href === '/widgets/42';
	const unsafeHrefGrid = H.fieldGrid([{ data: 'id', label: 'Id', href: 'javascript:alert(1)//{id}' }], { values: { id: '1' } });
	out.fieldGrid_unsafeHrefNotWrapped = !unsafeHrefGrid.querySelector('a');

	// actions: no onAction -> rendered disabled, never live-and-inert
	// NOTE: this shim's querySelector only understands ONE simple selector token (tag/class/attr) - no
	// descendant combinator - so every multi-part selector below is a two-step "container, then tag" query
	// rather than a single space-separated string (a space-separated string silently degrades to matching
	// just its FIRST token, which is how an earlier draft of this file mis-tested three of these).
	const actionGrid = H.fieldGrid([{ data: 'z', label: 'Z', actions: [{ id: 'unlink', label: 'Unlink' }] }], { values: { z: '1' } });
	const actionBtn = actionGrid.querySelector('.juneau-view-helper-field-actions').querySelector('button');
	out.fieldGrid_actionDisabledWithoutHandler = !!actionBtn && actionBtn.disabled === true;
	let actionFired = null;
	const actionGrid2 = H.fieldGrid([{ data: 'z', label: 'Z', actions: [{ id: 'unlink', label: 'Unlink' }] }], {
		values: { z: '1' }, onAction: function (id, data) { actionFired = { id: id, data: data }; }
	});
	const actionBtn2 = actionGrid2.querySelector('.juneau-view-helper-field-actions').querySelector('button');
	actionBtn2.dispatch('click', { target: actionBtn2 });
	out.fieldGrid_actionFiresOnAction = actionFired && actionFired.id === 'unlink' && actionFired.data === 'z';

	// columns -> a CSS CUSTOM PROPERTY, not an inline grid-template-columns
	const colGrid = H.fieldGrid([{ data: 'a', label: 'A' }], { values: { a: '1' }, columns: 3 });
	out.fieldGrid_columnsIsCustomProperty = colGrid.style['--juneau-view-detail-columns'] === '3'
		|| (typeof colGrid.style.getPropertyValue === 'function' && colGrid.style.getPropertyValue('--juneau-view-detail-columns') === '3');

	// loud argument errors
	out.fieldGrid_nonArrayThrows = !!throws(function () { H.fieldGrid('nope', {}); });
	out.fieldGrid_missingDataKeyThrows = !!throws(function () { H.fieldGrid([{ label: 'no data key' }], {}); });
})();

// ================================================================================================
// kvTable - the two accepted argument forms, plus a loud error for anything else
// ================================================================================================
(function kvTableTests() {
	const t1 = H.kvTable([['latency', 12], ['ok', true]]);
	const rows1 = t1.querySelectorAll('tr');
	out.kvTable_arrayForm = rows1.map(r => [textOf(r.querySelector('th')), textOf(r.querySelector('td'))]);

	const t2 = H.kvTable({ latency: 12, ok: true });
	const rows2 = t2.querySelectorAll('tr');
	out.kvTable_mapForm = rows2.map(r => [textOf(r.querySelector('th')), textOf(r.querySelector('td'))]);

	out.kvTable_sameOutputBothForms = JSON.stringify(out.kvTable_arrayForm) === JSON.stringify(out.kvTable_mapForm);

	const t3 = H.kvTable([{ key: 'a', value: 1 }]);
	out.kvTable_objectTupleForm = textOf(t3.querySelector('th')) === 'a' && textOf(t3.querySelector('td')) === '1';

	out.kvTable_bareScalarArrayThrows = !!throws(function () { H.kvTable([1, 2, 3]); });
	out.kvTable_nullThrows = !!throws(function () { H.kvTable(null); });
})();

// ================================================================================================
// button / buttonRow / text / pill / icon
// ================================================================================================
(function leafTests() {
	let clicked = 0;
	const b1 = H.button({ label: 'Go', onClick: function () { clicked++; } });
	out.button_enabledWithOnClick = b1.disabled === false;
	b1.dispatch('click', {});
	out.button_clickFires = clicked === 1;

	const b2 = H.button({ label: 'Go' });
	out.button_disabledWithoutOnClick = b2.disabled === true;

	const row = H.buttonRow([{ label: 'A' }, { label: 'B', onClick: function () {} }]);
	out.buttonRow_count = row.querySelectorAll('button').length;

	out.text_isTextNode = H.text('hi').nodeType === 3;
	out.text_nullIsEmpty = H.text(null).textContent === '';

	const p = H.pill('Open', 'ok');
	out.pill_label = textOf(p.querySelector('.juneau-view-helper-pill-label'));
	out.pill_toneClassApplied = !!p.querySelector('.juneau-view-helper-pill-dot--ok');

	const unknownIcon = H.icon('this-icon-does-not-exist');
	out.icon_unknownIsHidden = unknownIcon.hidden === true;
})();

// ================================================================================================
// recordTable - static/read-only, catalog vocabulary, empty state
// ================================================================================================
(function recordTableTests() {
	const catalog = [{ data: 'host', label: 'Host' }, { data: 'severity', label: 'Severity' }];
	const rows = [{ host: 'h1', severity: 'warn' }, { host: 'h2', severity: 'ok' }];
	const rt = H.recordTable(catalog, rows);
	const rtBodyRows = rt.querySelector('tbody').querySelectorAll('tr');
	out.recordTable_headerLabels = rt.querySelector('thead').querySelectorAll('th').map(textOf);
	out.recordTable_rowCount = rtBodyRows.length;
	out.recordTable_firstRowCells = rtBodyRows[0].querySelectorAll('td').map(textOf);

	const emptyRt = H.recordTable(catalog, []);
	out.recordTable_emptyIsNotATable = emptyRt.tagName !== 'TABLE';
})();

// ================================================================================================
// dataPane - §8.3's algorithm, all seven rows
// ================================================================================================
await (async function dataPaneTests() {
	// loading node painted first
	let resolveLoad;
	const pending = new Promise(function (res) { resolveLoad = res; });
	const pane1 = env.el('div');
	const populate1 = H.dataPane({ load: function () { return pending; }, render: function (data) { const s = env.el('span'); s.textContent = 'DATA:' + data.v; return s; } });
	const p1 = populate1(pane1, { tabId: 't1', signal: null });
	out.dataPane_loadingPaintedFirst = pane1.querySelectorAll('[role="status"]').length === 1;
	resolveLoad({ v: 'hello' });
	await p1;
	out.dataPane_renderAfterLoad = textOf(pane1.querySelector('span')) === 'DATA:hello';

	// AbortError -> silent: whatever was painted before the abort (the loading status) is left exactly as
	// it was; no error node, no empty node, no second status node
	const pane2 = env.el('div');
	const abortErr = new Error('aborted');
	abortErr.name = 'AbortError';
	const populate2 = H.dataPane({ load: function () { return Promise.reject(abortErr); }, render: function () { return env.el('span'); } });
	await populate2(pane2, {});
	out.dataPane_abortErrorLeavesLoadingStatusUntouched = pane2.querySelectorAll('[role="status"]').length === 1
		&& pane2.querySelectorAll('.juneau-view-helper-empty').length === 0;

	// any other rejection -> the error node, NEVER rethrown
	const pane3 = env.el('div');
	const populate3 = H.dataPane({ load: function () { return Promise.reject(new Error('kaboom')); }, render: function () { return env.el('span'); } });
	let threw3 = false;
	try { await populate3(pane3, {}); } catch (e) { threw3 = true; }
	out.dataPane_otherRejectionNeverRethrows = threw3 === false;
	out.dataPane_otherRejectionPaintsErrorStatus = pane3.querySelectorAll('[role="status"]').length === 1;

	// a THROW inside load (synchronous) is also contained
	const pane3b = env.el('div');
	const populate3b = H.dataPane({ load: function () { throw new Error('sync throw'); }, render: function () { return env.el('span'); } });
	let threw3b = false;
	try { await populate3b(pane3b, {}); } catch (e) { threw3b = true; }
	out.dataPane_syncThrowNeverRethrows = threw3b === false;

	// empty result (array of length 0) -> the empty node, custom empty() honored
	const pane4 = env.el('div');
	const populate4 = H.dataPane({ load: function () { return Promise.resolve([]); }, render: function () { return env.el('span'); }, empty: function () { const p = env.el('p'); p.className = 'custom-empty'; return p; } });
	await populate4(pane4, {});
	out.dataPane_emptyArrayPaintsCustomEmpty = pane4.querySelectorAll('.custom-empty').length === 1;

	// empty result (object with zero own enumerable keys) -> empty, default empty node when none supplied
	const pane5 = env.el('div');
	const populate5 = H.dataPane({ load: function () { return Promise.resolve({}); }, render: function () { return env.el('span'); } });
	await populate5(pane5, {});
	out.dataPane_emptyObjectPaintsDefaultEmpty = pane5.querySelectorAll('.juneau-view-helper-empty').length === 1;

	// a non-empty values map with zero-length keys is NOT the same code path as an object -- sanity: a
	// non-empty map renders, not empty
	const pane6 = env.el('div');
	const populate6 = H.dataPane({ load: function () { return Promise.resolve({ a: 1 }); }, render: function (d) { const s = env.el('span'); s.textContent = 'ok:' + d.a; return s; } });
	await populate6(pane6, {});
	out.dataPane_nonEmptyMapRenders = textOf(pane6.querySelector('span')) === 'ok:1';

	// "empty kind" rejection -> empty branch, not error
	const pane7 = env.el('div');
	const emptyKindErr = new Error('404');
	emptyKindErr.kind = 'empty';
	const populate7 = H.dataPane({ load: function () { return Promise.reject(emptyKindErr); }, render: function () { return env.el('span'); } });
	await populate7(pane7, {});
	out.dataPane_emptyKindRejectionPaintsEmpty = pane7.querySelectorAll('.juneau-view-helper-empty').length === 1;

	// invalid spec -> loud argument errors, not a silent broken helper
	out.dataPane_missingLoadThrows = !!throws(function () { H.dataPane({ render: function () {} }); });
	out.dataPane_missingRenderThrows = !!throws(function () { H.dataPane({ load: function () {} }); });
})();

// ================================================================================================
// tabStrip - the widened contract (test 30, i-xiii)
// ================================================================================================
// tabStrip's click/keydown handling is DELEGATED on the strip (role=tablist) element, exactly like the
// existing buildRibbonStrip it widens - a real browser click on a tab button BUBBLES to that listener
// with `target` set to the button, so a harness dispatch has to reproduce that (dispatch directly ON the
// button fires nothing: no listener is ever attached to an individual tab button).
function stripOf(wrapper) { return wrapper.querySelector('[role="tablist"]'); }
function tabBtn(wrapper, id) { return wrapper.querySelector('[data-juneau-strip-tab="' + id + '"]'); }
function clickTab(wrapper, id) {
	const btn = tabBtn(wrapper, id);
	stripOf(wrapper).dispatch('click', { target: btn });
}
function keyOnTab(wrapper, focusedBtn, key) {
	stripOf(wrapper).dispatch('keydown', { key: key, target: focusedBtn, preventDefault: function () {} });
}
function selectedTabId(wrapper) {
	const sel = wrapper.querySelectorAll('[role="tab"]').filter(b => b.getAttribute('aria-selected') === 'true')[0];
	return sel ? sel.dataset.juneauStripTab : null;
}

await (async function tabStripTests() {
	// (viii) the one-argument eager form still works unchanged
	const eagerPane = env.el('div'); eagerPane.textContent = 'eager content';
	const eagerWrap = H.tabStrip([{ id: 'a', label: 'A', pane: eagerPane }]);
	out.tabStrip_eagerFormHasOneTab = eagerWrap.querySelectorAll('[role="tab"]').length === 1;
	out.tabStrip_eagerFormPaneVisible = eagerWrap.querySelector('[role="tabpanel"]').hidden === false;

	// (i)+(ii) a lazy tab's populate does not run until first activation, and runs AT MOST ONCE
	let lazyRuns = 0;
	const wrap1 = H.tabStrip([
		{ id: 'a', label: 'A', pane: env.el('div') },
		{ id: 'b', label: 'B', populate: function (pane) { lazyRuns++; pane.textContent = 'filled:' + lazyRuns; } }
	]);
	out.tabStrip_lazyDoesNotRunBeforeActivation = lazyRuns === 0;
	clickTab(wrap1, 'b');
	out.tabStrip_lazyRunsOnActivation = lazyRuns === 1;
	clickTab(wrap1, 'a');
	clickTab(wrap1, 'b');
	out.tabStrip_fillOnce_neverReruns = lazyRuns === 1;

	// (iii) lazy:false populates at build time
	let eagerAt0Ran = false;
	H.tabStrip([
		{ id: 'a', label: 'A', pane: env.el('div') },
		{ id: 'b', label: 'B', lazy: false, populate: function () { eagerAt0Ran = true; } }
	]);
	out.tabStrip_lazyFalsePopulatesAtBuildTime = eagerAt0Ran === true;

	// (iv) a throwing tab shows that pane's error state; every sibling stays navigable
	const wrapThrow = H.tabStrip([
		{ id: 'a', label: 'A', populate: function () { throw new Error('bad tab'); }, lazy: false },
		{ id: 'b', label: 'B', pane: env.el('div') }
	]);
	const throwPanelStatuses = wrapThrow.querySelectorAll('[role="tabpanel"]').flatMap(p => p.querySelectorAll('[role="status"]'));
	out.tabStrip_throwingTabPaintsError = throwPanelStatuses.length === 1;
	clickTab(wrapThrow, 'b');
	out.tabStrip_siblingStillNavigableAfterThrow = selectedTabId(wrapThrow) === 'b';

	// a REJECTING (async) tab is also contained
	const wrapReject = H.tabStrip([
		{ id: 'a', label: 'A', pane: env.el('div') },
		{ id: 'b', label: 'B', populate: function () { return Promise.reject(new Error('async bad')); } }
	]);
	clickTab(wrapReject, 'b');
	await flush();
	out.tabStrip_rejectingTabPaintsErrorStatus = wrapReject.querySelectorAll('[role="status"]').length === 1;

	// (v) aborting opts.signal prevents any not-yet-run populate from running
	const ac = new AbortController();
	let abortedTabRan = false;
	const wrapAbort = H.tabStrip([
		{ id: 'a', label: 'A', pane: env.el('div') },
		{ id: 'b', label: 'B', populate: function () { abortedTabRan = true; } }
	], { signal: ac.signal });
	ac.abort();
	clickTab(wrapAbort, 'b');
	out.tabStrip_abortPreventsFuturePopulate = abortedTabRan === false;

	// (vi) loud argument error: both pane AND populate, or neither
	out.tabStrip_bothPaneAndPopulateThrows = !!throws(function () { H.tabStrip([{ id: 'a', label: 'A', pane: env.el('div'), populate: function () {} }]); });
	out.tabStrip_neitherPaneNorPopulateThrows = !!throws(function () { H.tabStrip([{ id: 'a', label: 'A' }]); });
	out.tabStrip_emptyArrayThrows = !!throws(function () { H.tabStrip([]); });
	out.tabStrip_duplicateIdThrows = !!throws(function () { H.tabStrip([{ id: 'a', label: 'A', pane: env.el('div') }, { id: 'a', label: 'A2', pane: env.el('div') }]); });

	// (vii) onActivate fires on EVERY activation including re-shows; populate does not re-run
	let activateCount = 0, populateCount = 0;
	const wrap7 = H.tabStrip([
		{ id: 'a', label: 'A', pane: env.el('div') },
		{ id: 'b', label: 'B', populate: function () { populateCount++; } }
	], { onActivate: function () { activateCount++; } });
	clickTab(wrap7, 'b');
	clickTab(wrap7, 'a');
	clickTab(wrap7, 'b');
	out.tabStrip_onActivateFiresOnEveryActivation = activateCount === 3;
	out.tabStrip_populateRunsOnlyOnce = populateCount === 1;

	// (ix)-(xiii): a TEN-tab strip, keyboard nav, aria-selected exactly one, hidden panes, disabled skip
	const tenTabs = [];
	for (let i = 0; i < 10; i++) tenTabs.push({ id: 't' + i, label: 'Tab ' + i, pane: env.el('div'), disabled: i === 3 });
	const wrap10 = H.tabStrip(tenTabs);
	const btns = wrap10.querySelectorAll('[role="tab"]');
	out.tabStrip_ten_ariaSelectedExactlyOne = btns.filter(b => b.getAttribute('aria-selected') === 'true').length === 1;
	out.tabStrip_ten_rovingTabindexExactlyOneZero = btns.filter(b => b.tabIndex === 0).length === 1;
	const panes10 = wrap10.querySelectorAll('[role="tabpanel"]');
	out.tabStrip_ten_hiddenPanesExceptOne = panes10.filter(p => !p.hidden).length === 1;
	out.tabStrip_ten_disabledNeverActivatable = btns[3].getAttribute('aria-selected') === 'false';

	// Home / End / Left / Right, against detailTabTargetIndex's live contract, skipping the disabled entry
	// focus tab 0 (already selected), press End -> should land on tab 9 (last, not disabled)
	keyOnTab(wrap10, btns[0], 'End');
	out.tabStrip_end_selectsLast = selectedTabId(wrap10) === 't9';
	// from tab 9, Home -> tab 0
	keyOnTab(wrap10, btns[9], 'Home');
	out.tabStrip_home_selectsFirst = selectedTabId(wrap10) === 't0';
	// from tab 2 - activate it first, then ArrowRight -> tab 3 is DISABLED, must skip to tab 4
	clickTab(wrap10, 't2');
	keyOnTab(wrap10, btns[2], 'ArrowRight');
	out.tabStrip_arrowRight_skipsDisabled = selectedTabId(wrap10) === 't4';
	// unhandled key changes nothing
	const beforeKey = selectedTabId(wrap10);
	keyOnTab(wrap10, btns.filter(b => b.getAttribute('aria-selected') === 'true')[0], 'x');
	out.tabStrip_unhandledKeyChangesNothing = selectedTabId(wrap10) === beforeKey;

	// two independent instances: fill-once state does not leak between them (purity, positive half of test 29)
	let runsX = 0, runsY = 0;
	const wrapX = H.tabStrip([{ id: 'a', label: 'A', pane: env.el('div') }, { id: 'b', label: 'B', populate: function () { runsX++; } }]);
	const wrapY = H.tabStrip([{ id: 'a', label: 'A', pane: env.el('div') }, { id: 'b', label: 'B', populate: function () { runsY++; } }]);
	clickTab(wrapX, 'b');
	clickTab(wrapX, 'b');
	out.tabStrip_independentInstances_xRanOnce_yNeverRan = runsX === 1 && runsY === 0;
})();

// ================================================================================================
// dateRange / dropdown / filterBuilder
// ================================================================================================
(function controlsTests() {
	let lastRange = null;
	const dr = H.dateRange({ from: '2026-01-01', to: '2026-01-31' }, function (r) { lastRange = r; });
	const inputs = dr.querySelectorAll('input');
	out.dateRange_twoInputs = inputs.length === 2;
	inputs[1].value = '2026-02-01';
	inputs[1].dispatch('change', {});
	out.dateRange_onChangeFires = !!lastRange && lastRange.to === '2026-02-01';
	inputs[0].value = '2026-03-01'; // from AFTER to -> validation status, no onChange
	lastRange = null;
	inputs[0].dispatch('change', {});
	out.dateRange_fromAfterTo_blocksOnChange = lastRange === null;

	let lastValue = null;
	const dd = H.dropdown({ label: 'Region', options: [{ value: 'us', label: 'US' }, { value: 'eu', label: 'EU' }] }, function (v) { lastValue = v; });
	const select = dd.querySelector('select');
	out.dropdown_optionCount = select.querySelectorAll('option').length;
	select.value = 'eu';
	select.dispatch('change', {});
	out.dropdown_onChangeFires = lastValue === 'eu';

	let lastPredicates = null;
	const fb = H.filterBuilder({ fields: [{ data: 'status', label: 'Status' }] }, function (p) { lastPredicates = p; });
	// The shim's <select> does not auto-default `.value` to the first <option> the way a real browser
	// does, so pin field/op explicitly rather than lean on that browser behavior.
	const selects = fb.querySelectorAll('select');
	selects[0].value = 'status';
	selects[1].value = 'eq';
	const addBtn = fb.querySelectorAll('button')[0];
	const valueInput = fb.querySelector('input');
	valueInput.value = 'open';
	addBtn.dispatch('click', {});
	out.filterBuilder_addFiresOnChangeWithOnePredicate = Array.isArray(lastPredicates) && lastPredicates.length === 1 && lastPredicates[0].field === 'status' && lastPredicates[0].value === 'open';
	out.filterBuilder_chipRendered = fb.querySelectorAll('.juneau-view-helper-filterbuilder-chip').length === 1;
	const removeBtn = fb.querySelector('.juneau-view-helper-filterbuilder-chip').querySelector('button');
	removeBtn.dispatch('click', {});
	out.filterBuilder_removeFiresOnChangeWithZeroPredicates = Array.isArray(lastPredicates) && lastPredicates.length === 0;

	out.dropdown_missingOptionsThrows = !!throws(function () { H.dropdown({}); });
	out.filterBuilder_missingFieldsThrows = !!throws(function () { H.filterBuilder({}); });
})();

process.stdout.write(JSON.stringify(out));
})();
