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
 * chrome-menu.cjs - always-on Node INTEGRATION harness for the chrome/views menu-wiring contract: juneau-renders.js
 * + juneau-views.js (the ONE 445h layer stack) + juneau-chrome.js loaded into a SINGLE sandbox, so a real
 * Behavior.MENU trigger opens its .jc-menu list on the shared stack - not on a chrome-local mock.  Proves the list
 * is portalled to document.body (position:fixed, escaping any overflow-clip ancestor) as a kind:"menu"
 * light-dismiss layer, that Escape /
 * outside-click dismissal runs through that ONE stack, and - the stacking case - that a chrome menu opened OVER a
 * views dialog pops off cleanly on Escape without disturbing the dialog beneath it (a menu never inflates the
 * dialog-kind depth cap).
 *
 *   Usage:  node chrome-menu.cjs <juneau-renders.js> <juneau-views.js> <juneau-chrome.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const chromeJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !chromeJsPath) {
	console.error('usage: node chrome-menu.cjs <juneau-renders.js> <juneau-views.js> <juneau-chrome.js>');
	process.exit(2);
}

const env = makeEnv();
// CustomEvent so chrome's SAFE dispatch path does not bail (these stack assertions do not exercise the host event
// itself - header.cjs covers SAFE dispatch - but the constructor must exist for wiring to run cleanly).
env.window.CustomEvent = function (type, init) {
	this.type = type;
	this.detail = init?.detail;
	this.bubbles = !!init?.bubbles;
	this.defaultPrevented = false;
	this.preventDefault = function () { this.defaultPrevented = true; };
};

const sandbox = {
	window: env.window, document: env.document, console: console,
	setTimeout: function (fn) {
		if (typeof fn === 'function') { fn(); }
		return 0;
	},
	clearTimeout: function () {}, setInterval: function () { return 0; }, clearInterval: function () {},
	Promise: Promise,
	fetch: function (...args) { return env.callFetch(...args); }
};

// ONE sandbox: views.js publishes window.JuneauViews.init (the shared stack); chrome.js then CALLS it (never defines it).
// NOSONAR javascript:S1523 -- loading the production juneau-*.js sources into a VM sandbox is this harness's
// intended mechanism for exercising them together; inputs are fixed local file paths supplied by the test.
vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
// NOSONAR javascript:S1523 -- same fixed-local-file harness mechanism as above, for juneau-views.js.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
// NOSONAR javascript:S1523 -- same fixed-local-file harness mechanism as above, for juneau-chrome.js.
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const V = env.window.JuneauViews?.init;
const C = env.window.JuneauChrome?.init;
const out = {
	hasViews: !!(V && typeof V.pushLayer === 'function' && typeof V.topLayer === 'function'),
	hasChrome: !!(C && typeof C.wireMenus === 'function')
};
if (!out.hasViews || !out.hasChrome) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

// The shared shim element lacks classList (chrome's menuForTrigger reads it); add a live getter over className.
function mkEl(tag, className, attrs) {
	const n = env.el(tag);
	if (className) n.setAttribute('class', className);
	if (attrs) for (const k in attrs) if (Object.hasOwn(attrs, k)) n.setAttribute(k, attrs[k]);
	Object.defineProperty(n, 'classList', {
		configurable: true,
		get: function () { return this.className ? this.className.split(/\s+/) : []; }
	});
	return n;
}

function clickEv() { return { type: 'click', preventDefault: function () { this.defaultPrevented = true; } }; }
function drain() { while (V.topLayer()) V.popLayer(); }

// Build the server-shaped app-header: <header data-juneau-app-header> > .jc-header-actions > (trigger + .jc-menu list).
// The MENU trigger's aria-controls points at the list id, exactly as AppHeaderTable emits it.
const header = mkEl('header', 'jc-header', { 'data-juneau-app-header': 'app' });
env.body.appendChild(header);
const actions = mkEl('div', 'jc-header-actions');
header.appendChild(actions);
const trigger = mkEl('button', 'jc-icon-btn', {
	'data-juneau-header-action': 'more', 'data-juneau-behavior': 'menu',
	'aria-haspopup': 'menu', 'aria-expanded': 'false', 'aria-controls': 'juneau-menu:app:more'
});
actions.appendChild(trigger);
const menu = mkEl('div', 'jc-menu', { 'id': 'juneau-menu:app:more', 'role': 'menu' });
menu.appendChild(mkEl('a', 'jc-menu-item', { 'href': '/a', 'role': 'menuitem' }));
menu.appendChild(mkEl('button', 'jc-menu-item', { 'role': 'menuitem', 'data-juneau-safe': 'do-it' }));
actions.appendChild(menu);

C.wireMenus(header);

// --- open: the list is pushed onto the shared stack, portalled to body, position:fixed, as a kind:"menu" layer ------
trigger.dispatch('click', clickEv());
out.open_topIsMenu = V.topLayer()?.kind === 'menu';
out.open_menuOnBody = menu.parentNode === env.body;
out.open_escapedHeader = ! header.contains(menu);            // no longer clipped by any header/actions ancestor
out.open_positionFixed = menu.style.position === 'fixed';
out.open_displayShown = menu.style.display === 'block';
out.open_zIndexSet = !! menu.style.zIndex;
out.open_notADialog = V.dialogLayerCount() === 0;            // a menu must not inflate the dialog-kind depth cap
out.open_ariaExpanded = trigger.getAttribute('aria-expanded');

// --- Escape pops the menu THROUGH the shared stack; onDismiss (shared teardown) resets ARIA + hides (kept for reuse) -
env.dispatchDocument('keydown', { key: 'Escape' });
out.esc_closed = V.topLayer() === null;
out.esc_ariaReset = trigger.getAttribute('aria-expanded');   // 'false'
out.esc_menuHidden = menu.style.display === 'none';
out.esc_menuKeptInBody = menu.parentNode === env.body;       // detachOnPop:false -> node persists, reused on reopen

// --- reopen, then an OUTSIDE pointerdown light-dismisses the top menu layer via the shared stack -------------------
trigger.dispatch('click', clickEv());
out.reopen_topIsMenu = V.topLayer()?.kind === 'menu';
const outside = mkEl('div', ''); env.body.appendChild(outside);
env.dispatchDocument('pointerdown', { target: outside });
out.light_closed = V.topLayer() === null && trigger.getAttribute('aria-expanded') === 'false';

// --- stacking: a chrome menu opened OVER a views dialog pops off on Escape WITHOUT disturbing the dialog beneath ----
drain();
const table = mkEl('table', ''); const tr = mkEl('tr', '');
V.showActionDialog({ title: 'Details' }, { id: 'd', label: 'Details' }, table, tr, {});
out.stack_dialogFirst = V.dialogLayerCount() === 1 && V.topLayer().kind === 'dialog';
trigger.dispatch('click', clickEv());
out.stack_menuOverDialog = V.topLayer()?.kind === 'menu';
out.stack_dialogCountUnchanged = V.dialogLayerCount() === 1;   // the menu stacks ABOVE without touching the dialog cap
env.dispatchDocument('keydown', { key: 'Escape' });            // Escape pops ONLY the top (the menu)
out.stack_menuPoppedDialogRemains = V.topLayer()?.kind === 'dialog';
out.stack_dialogSurvives = V.dialogLayerCount() === 1;
out.stack_ariaReset = trigger.getAttribute('aria-expanded');   // 'false'
drain();

process.stdout.write(JSON.stringify(out));
