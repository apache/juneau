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
 * probe-group.cjs - always-on Node harness for the status probe group (single-select status chips).
 *
 * Proves the selection contract for a server-painted probe group that JuneauViews enhances in place:
 *
 *   - initial selection comes from markup (aria-checked="true") else the first enabled probe, with no event;
 *   - radiogroup/radio roles, exactly one selected probe, and a roving tabindex (one tab stop per group);
 *   - click and arrow keys move selection (Right/Down forward, Left/Up back - both wrap and skip disabled;
 *     Home/End jump to the first/last enabled), firing onSelect once per real change;
 *   - a no-op re-click of the selected probe, an imperative select(), and init do NOT fire onSelect;
 *   - a status repaint keyed to probe IDENTITY preserves the selection even when status classes change;
 *   - disabled probes are skipped by the roving focus and are not selectable;
 *   - two groups are independent, and an empty group is a no-op.
 *
 *   Usage:  node probe-group.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node probe-group.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: typeof I?.enhanceProbeGroup === 'function'
		&& typeof I?.initProbeGroups === 'function'
		&& typeof I?.probeTargetIndex === 'function'
};
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function idOf(el) { return el ? el.getAttribute('data-juneau-probe') : null; }

function probe(id, status, opts) {
	opts = opts || {};
	const el = env.el('button');
	el.className = 'jc-probe jc-probe-' + status + (opts.disabled ? ' is-disabled' : '');
	el.setAttribute('data-juneau-probe', id);
	if (opts.checked) el.setAttribute('aria-checked', 'true');
	if (opts.disabled) el.setAttribute('aria-disabled', 'true');
	el.textContent = opts.label || id;
	return el;
}

function group(probes) {
	const g = env.el('div');
	g.className = 'jc-probe-group';
	g.setAttribute('data-juneau-probe-group', '');
	probes.forEach(function (p) { g.appendChild(p); });
	env.body.appendChild(g);
	return g;
}

// ------------------------------------------------------------------------------------------------------------------
// 1) Initial paint: role wiring, markup-declared selection, roving tabindex, no event on init.
// ------------------------------------------------------------------------------------------------------------------

const p1 = probe('ok', 'ok');
const p2 = probe('warn', 'warn', { checked: true });
const p3 = probe('fail', 'fail');
const g1 = group([p1, p2, p3]);
let events1 = [];
const ctl1 = I.enhanceProbeGroup(g1, { onSelect: function (id) { events1.push(id); } });

out.init_groupRole = g1.getAttribute('role');
out.init_probeRoles = [p1, p2, p3].map(function (p) { return p.getAttribute('role'); }).join(',');
out.init_ariaChecked = [p1, p2, p3].map(function (p) { return p.getAttribute('aria-checked'); }).join(',');
out.init_tabindexes = [p1, p2, p3].map(function (p) { return p.tabIndex; }).join(',');
out.init_selectedId = idOf(ctl1.getSelected());
out.init_eventCount = events1.length;

// ------------------------------------------------------------------------------------------------------------------
// 2) Click selection: fires once, moves the roving tab stop and focus; a re-click of the selected probe is a no-op.
// ------------------------------------------------------------------------------------------------------------------

events1 = [];
env.setActive(null);
g1.dispatch('click', { target: p3 });
out.click_ariaChecked = [p1, p2, p3].map(function (p) { return p.getAttribute('aria-checked'); }).join(',');
out.click_tabindexes = [p1, p2, p3].map(function (p) { return p.tabIndex; }).join(',');
out.click_eventCount = events1.length;
out.click_eventId = events1.length ? events1[0] : null;
out.click_focus = env.getActive() === p3;

events1 = [];
g1.dispatch('click', { target: p3 });
out.reclick_eventCount = events1.length;
out.reclick_stillSelected = idOf(ctl1.getSelected());

// ------------------------------------------------------------------------------------------------------------------
// 3) A status repaint keyed to IDENTITY preserves selection even when status classes change, and emits nothing.
// ------------------------------------------------------------------------------------------------------------------

events1 = [];
p3.className = 'jc-probe jc-probe-ok';   // app re-ran the probe: fail -> ok
p1.className = 'jc-probe jc-probe-fail';
ctl1.repaint();
out.repaint_selectedId = idOf(ctl1.getSelected());
out.repaint_ariaChecked = [p1, p2, p3].map(function (p) { return p.getAttribute('aria-checked'); }).join(',');
out.repaint_eventCount = events1.length;

// ------------------------------------------------------------------------------------------------------------------
// 4) Keyboard: Right/Down forward, Left/Up back (both wrap + skip disabled), Home/End to first/last enabled.
// ------------------------------------------------------------------------------------------------------------------

const q1 = probe('a', 'ok');
const q2 = probe('b', 'warn', { disabled: true });
const q3 = probe('c', 'fail');
const g2 = group([q1, q2, q3]);
let ev2 = [];
const ctl2 = I.enhanceProbeGroup(g2, { onSelect: function (id) { ev2.push(id); } });

out.kbd_initSelected = idOf(ctl2.getSelected());   // first enabled = 'a'

env.setActive(q1); ev2 = [];
g2.dispatch('keydown', { key: 'ArrowRight', preventDefault: function () {} });
out.kbd_rightSkipsDisabled = idOf(ctl2.getSelected());   // 'c' (skips disabled 'b')
out.kbd_rightEventId = ev2.length ? ev2[0] : null;
out.kbd_rightFocus = env.getActive() === q3;

env.setActive(q3);
g2.dispatch('keydown', { key: 'ArrowRight', preventDefault: function () {} });
out.kbd_rightWrap = idOf(ctl2.getSelected());   // wraps to 'a'

env.setActive(q1);
g2.dispatch('keydown', { key: 'ArrowLeft', preventDefault: function () {} });
out.kbd_leftWrap = idOf(ctl2.getSelected());   // wraps to 'c'

env.setActive(q1);
g2.dispatch('keydown', { key: 'ArrowDown', preventDefault: function () {} });
out.kbd_downSameAsRight = idOf(ctl2.getSelected());   // 'c'

env.setActive(q3);
g2.dispatch('keydown', { key: 'ArrowUp', preventDefault: function () {} });
out.kbd_upSameAsLeft = idOf(ctl2.getSelected());   // 'a'

env.setActive(q3);
g2.dispatch('keydown', { key: 'Home', preventDefault: function () {} });
out.kbd_home = idOf(ctl2.getSelected());   // 'a'

env.setActive(q1);
g2.dispatch('keydown', { key: 'End', preventDefault: function () {} });
out.kbd_end = idOf(ctl2.getSelected());   // 'c'

ev2 = [];
const beforeUnhandled = idOf(ctl2.getSelected());
g2.dispatch('keydown', { key: 'Enter', preventDefault: function () {} });
out.kbd_unhandledEventCount = ev2.length;
out.kbd_unhandledUnchanged = idOf(ctl2.getSelected()) === beforeUnhandled;

// ------------------------------------------------------------------------------------------------------------------
// 5) Disabled probes: skipped by the roving focus and not selectable by click.
// ------------------------------------------------------------------------------------------------------------------

out.disabled_tabindex = q2.tabIndex;   // always -1
ev2 = [];
const beforeDisabledClick = idOf(ctl2.getSelected());
g2.dispatch('click', { target: q2 });
out.disabled_clickEventCount = ev2.length;
out.disabled_clickUnchanged = idOf(ctl2.getSelected()) === beforeDisabledClick;

// ------------------------------------------------------------------------------------------------------------------
// 6) Imperative select()/getSelected(): moves selection with NO event; a disabled id is rejected.
// ------------------------------------------------------------------------------------------------------------------

ev2 = [];
out.imperative_changed = ctl2.select('a') === true;
out.imperative_eventCount = ev2.length;
out.imperative_selected = idOf(ctl2.getSelected());
out.imperative_disabledRejected = ctl2.select('b') === false;
out.imperative_selectedStill = idOf(ctl2.getSelected());
out.imperative_unknownRejected = ctl2.select('nope') === false;

// ------------------------------------------------------------------------------------------------------------------
// 7) Two groups are independent: selecting in one does not clear the other.
// ------------------------------------------------------------------------------------------------------------------

const r1 = probe('x', 'ok'); const r2 = probe('y', 'warn');
const gA = group([r1, r2]);
const s1 = probe('m', 'ok'); const s2 = probe('n', 'warn');
const gB = group([s1, s2]);
I.enhanceProbeGroup(gA);
I.enhanceProbeGroup(gB);
gA.dispatch('click', { target: r2 });
gB.dispatch('click', { target: s2 });
out.groups_aSelected = r2.getAttribute('aria-checked') === 'true' && r1.getAttribute('aria-checked') === 'false';
out.groups_bSelected = s2.getAttribute('aria-checked') === 'true' && s1.getAttribute('aria-checked') === 'false';

// ------------------------------------------------------------------------------------------------------------------
// 8) Degenerate groups: an empty group is a no-op; an all-disabled group has nothing selected and no tab stop.
// ------------------------------------------------------------------------------------------------------------------

const gE = group([]);
const ctlE = I.enhanceProbeGroup(gE);
out.empty_noSelected = ctlE.getSelected() === null;
let emptyThrew = false;
try { gE.dispatch('keydown', { key: 'ArrowRight', preventDefault: function () {} }); }
catch (e) { emptyThrew = true; }
out.empty_keydownNoThrow = !emptyThrew;

const t1 = probe('d1', 'ok', { disabled: true });
const t2 = probe('d2', 'warn', { disabled: true });
const gD = group([t1, t2]);
const ctlD = I.enhanceProbeGroup(gD);
out.allDisabled_noSelected = ctlD.getSelected() === null;
out.allDisabled_tabindexes = [t1, t2].map(function (p) { return p.tabIndex; }).join(',');
out.allDisabled_ariaChecked = [t1, t2].map(function (p) { return p.getAttribute('aria-checked'); }).join(',');

// ------------------------------------------------------------------------------------------------------------------
// 9) Idempotency + initProbeGroups page-wide scan.
// ------------------------------------------------------------------------------------------------------------------

out.idempotent_sameCtl = I.enhanceProbeGroup(g1) === ctl1;

const w1 = probe('w1', 'ok', { checked: true }); const w2 = probe('w2', 'warn');
const gW = group([w1, w2]);
I.initProbeGroups(env.document);
out.initAll_enhanced = !!gW._juneauProbeCtl;
out.initAll_role = gW.getAttribute('role');
out.initAll_selected = w1.getAttribute('aria-checked') === 'true';

// ------------------------------------------------------------------------------------------------------------------
// 10) probeTargetIndex - the pure roving-target contract.
// ------------------------------------------------------------------------------------------------------------------

const en = [true, false, true];   // middle disabled
out.pti_right = I.probeTargetIndex('ArrowRight', 0, en);
out.pti_rightWrap = I.probeTargetIndex('ArrowRight', 2, en);
out.pti_left = I.probeTargetIndex('ArrowLeft', 2, en);
out.pti_leftWrap = I.probeTargetIndex('ArrowLeft', 0, en);
out.pti_down = I.probeTargetIndex('ArrowDown', 0, en);
out.pti_up = I.probeTargetIndex('ArrowUp', 2, en);
out.pti_home = I.probeTargetIndex('Home', 2, en);
out.pti_end = I.probeTargetIndex('End', 0, en);
out.pti_unhandled = I.probeTargetIndex('Enter', 0, en);
out.pti_noneEnabled = I.probeTargetIndex('ArrowRight', 0, [false, false]);
out.pti_empty = I.probeTargetIndex('ArrowRight', 0, []);

process.stdout.write(JSON.stringify(out));
