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
 * card-actions.cjs - Node harness for a card's declared action catalog: renders + views + chrome + cards loaded into
 * ONE sandbox, so a card MENU trigger opens on the REAL shared views layer stack rather than a cards-local mock.
 *
 * Covers: juneau-cards.js as the enhancement owner (chrome's own DOMContentLoaded scan never sees a card), MENU
 * forwarding to the shared stack, SAFE host-event dispatch, a static (non-refreshable) card with actions still being
 * enhanced, a grid-less single card being enhanced, and the hidden-card split - a hosted view table still inits at
 * DOMContentLoaded while a refreshable sibling's poll timers stay suspended.
 *
 *   Usage:  node card-actions.cjs <juneau-renders.js> <juneau-views.js> <juneau-chrome.js> <juneau-cards.js>
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
const cardsJsPath = process.argv[5];
if (!rendersJsPath || !viewsJsPath || !chromeJsPath || !cardsJsPath) {
	console.error('usage: node card-actions.cjs <juneau-renders.js> <juneau-views.js> <juneau-chrome.js> <juneau-cards.js>');
	process.exit(2);
}

// The hosted-table init below hands off to a DataTable stub that throws (there is no real DataTables here); initTable
// surfaces that as a rejected promise nobody awaits, which would otherwise abort node before the report is printed.
process.on('unhandledRejection', function () {});

const env = makeEnv();
env.window.CustomEvent = function (type, init) {
	this.type = type;
	this.detail = init?.detail;
	this.bubbles = !!init?.bubbles;
	this.defaultPrevented = false;
	this.preventDefault = function () { this.defaultPrevented = true; };
};
env.window.MutationObserver = function (cb) {
	env.window.__observerCallback = cb;
	this.observe = function () {};
	this.disconnect = function () {};
};

// Controllable timers: a card poll must never schedule real work that keeps node alive.
const timers = {};
let timerSeq = 1;
function fakeSetTimeout(fn) { const id = timerSeq++; timers[id] = fn; return id; }
function fakeSetInterval(fn) { const id = timerSeq++; timers[id] = fn; return id; }
function fakeClear(id) { delete timers[id]; }
env.window.setTimeout = fakeSetTimeout;
env.window.setInterval = fakeSetInterval;
env.window.clearTimeout = fakeClear;
env.window.clearInterval = fakeClear;
env.window.fetch = function () { return Promise.resolve({ json: function () { return Promise.resolve({ contractVersion: '1', fields: {} }); } }); };

const sandbox = {
	window: env.window, document: env.document, console: console,
	setTimeout: fakeSetTimeout, setInterval: fakeSetInterval, clearTimeout: fakeClear, clearInterval: fakeClear,
	Promise: Promise, Math: Math, Date: Date, Object: Object, Set: Set, JSON: JSON,
	fetch: function () { return env.window.fetch.apply(null, arguments); }
};

// Load order mirrors the served bundle order: views publishes the shared stack, chrome consumes it, cards consumes
// chrome.  Nothing here calls chrome's own initAll - that is the point of the ownership assertions below.
// NOSONAR javascript:S1523 -- loading the production juneau-*.js sources into a VM sandbox is this harness's
// intended mechanism for exercising them together; inputs are fixed local file paths supplied by the test.
vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
// NOSONAR javascript:S1523 -- same fixed-local-file harness mechanism as above, for juneau-views.js.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
// NOSONAR javascript:S1523 -- same fixed-local-file harness mechanism as above, for juneau-chrome.js.
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });
// NOSONAR javascript:S1523 -- same fixed-local-file harness mechanism as above, for juneau-cards.js.
vm.runInNewContext(fs.readFileSync(path.resolve(cardsJsPath), 'utf8'), sandbox, { filename: 'juneau-cards.js' });

const V = env.window.JuneauViews?.init;
const C = env.window.JuneauChrome?.init;
const K = env.window.JuneauCards?.init;
const out = {
	hasViews: !!(V && typeof V.pushLayer === 'function' && typeof V.topLayer === 'function'),
	hasChrome: !!(C && typeof C.wireMenus === 'function'),
	hasCards: !!(K && typeof K.initAll === 'function')
};
if (!out.hasViews || !out.hasChrome || !out.hasCards) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

// The cards runtime must own NO layer stack of its own: it may only be a client of the views one.
out.cards_definesNoLayerStack = K.pushLayer === undefined && K.popLayer === undefined
	&& K.topLayer === undefined;

/** The shim element lacks classList (chrome's menuForTrigger reads it); add a live view over className. */
function mkEl(tag, className, attrs) {
	const n = env.el(tag);
	if (className) n.setAttribute('class', className);
	if (attrs) for (const k in attrs) if (Object.hasOwn(attrs, k)) n.setAttribute(k, attrs[k]);
	Object.defineProperty(n, 'classList', {
		configurable: true,
		get: function () { return this.className ? this.className.split(/\s+/) : []; }
	});
	// The shim node has dispatch() but not the dispatchEvent() the SAFE host-event path calls; fire local listeners
	// (the shim has no bubbling, so the SAFE assertions below listen on the action element itself).
	n.dispatchEvent = function (ev) { this.dispatch(ev.type, ev); return true; };
	return n;
}

function clickEv() { return { type: 'click', preventDefault: function () { this.defaultPrevented = true; } }; }
function drain() { while (V.topLayer()) V.popLayer(); }

/**
 * Builds one server-shaped card `<article>`: header (title, optional refresh trigger), banner, body.  opts:
 * {id, scope, refresh, poll, menu, safe, link} - `scope` is the id qualifier the server minted the menu list with.
 */
function buildCard(opts) {
	const card = mkEl('article', 'juneau-view-card', { 'data-juneau-card': '1', 'data-juneau-card-id': opts.id });
	card.setAttribute('aria-labelledby', opts.id + '-title');
	if (opts.refresh) {
		card.dataset.juneauCardContract = '1';
		card.dataset.juneauCardRefresh = opts.refresh;
	}
	if (opts.poll) card.dataset.juneauCardPollMs = String(opts.poll);

	const header = mkEl('header', 'juneau-view-card-header');
	const title = mkEl('span', 'juneau-view-card-title', { 'id': opts.id + '-title' });
	title.textContent = 'Orders';
	header.appendChild(title);
	const actions = mkEl('div', 'juneau-view-card-actions');
	header.appendChild(actions);
	card.appendChild(header);

	const parts = { header: header, actions: actions };
	if (opts.refresh) {
		const btn = mkEl('button', 'jc-icon-btn', { 'data-juneau-card-refresh-trigger': '1' });
		actions.appendChild(btn);
		parts.refreshBtn = btn;
	}
	if (opts.link) {
		parts.link = mkEl('a', 'jc-icon-btn juneau-view-card-action', {
			'href': '/reports/1', 'data-juneau-header-action': 'open', 'data-juneau-behavior': 'link',
			'data-juneau-icon': 'external', 'aria-label': 'Open', 'title': 'Open'
		});
		parts.link.appendChild(mkEl('span', 'jc-icon', { 'aria-hidden': 'true' }));
		actions.appendChild(parts.link);
	}
	if (opts.safe) {
		parts.safe = mkEl('button', 'jc-icon-btn juneau-view-card-action', {
			'data-juneau-header-action': 'pin', 'data-juneau-behavior': 'safe', 'data-juneau-safe': 'card-pin',
			'aria-label': 'Pin', 'title': 'Pin'
		});
		actions.appendChild(parts.safe);
	}
	if (opts.menu) {
		const menuId = 'juneau-menu:' + opts.scope + ':more';
		parts.trigger = mkEl('button', 'jc-icon-btn juneau-view-card-action', {
			'data-juneau-header-action': 'more', 'data-juneau-behavior': 'menu', 'data-juneau-icon': 'overflow',
			'aria-haspopup': 'menu', 'aria-expanded': 'false', 'aria-controls': menuId,
			'aria-label': 'More actions', 'title': 'More actions'
		});
		parts.trigger.appendChild(mkEl('span', 'jc-icon', { 'aria-hidden': 'true' }));
		actions.appendChild(parts.trigger);
		parts.menu = mkEl('div', 'jc-menu', { 'id': menuId, 'role': 'menu' });
		parts.menu.appendChild(mkEl('button', 'jc-menu-item', { 'role': 'menuitem', 'data-juneau-safe': 'card-pin' }));
		actions.appendChild(parts.menu);
		parts.menuId = menuId;
	}

	const banner = mkEl('div', 'juneau-view-card-banner', { 'data-juneau-card-banner': '1', 'role': 'alert' });
	banner.hidden = true;
	card.appendChild(banner);
	const body = mkEl('div', 'juneau-view-card-body', { 'data-juneau-card-body': '1' });
	card.appendChild(body);
	parts.banner = banner;
	parts.body = body;
	card._parts = parts;
	return card;
}

/** Appends a hosted view table (a ViewCardBody's emitted shell) plus its card-scoped sidecar to a card body. */
function hostView(card, viewId, mintedId, dataUrl) {
	const sidecar = mkEl('script', '', { 'id': 'juneau-view:' + mintedId, 'type': 'application/json' });
	sidecar.textContent = JSON.stringify({
		contractVersion: env.window.JuneauViews.CONTRACT_VERSION, id: viewId, dataMode: 'CLIENT', dataUrl: dataUrl,
		columns: [{ data: 'ref', title: 'Ref' }]
	});
	card._parts.body.appendChild(sidecar);
	const table = mkEl('table', 'juneau-view-table', { 'id': mintedId, 'data-juneau-view': viewId });
	card._parts.body.appendChild(table);
	return table;
}

// ==================================================================================================================
// A) One grid, two cards: the cards runtime enhances the action catalog, MENU rides the SHARED stack
// ==================================================================================================================

const grid = mkEl('section', 'juneau-view-card-grid', { 'data-juneau-card-grid': '1', 'id': 'g1' });
env.body.appendChild(grid);
const cardA = buildCard({ id: 'c1', scope: 'g1:c1', menu: true, safe: true, link: true });
grid.appendChild(cardA);
const cardB = buildCard({ id: 'c2', scope: 'g1:c2', menu: true, refresh: '/data/summary', poll: 10000 });
grid.appendChild(cardB);

// Chrome's OWN DOMContentLoaded scan (headers + bar slots) must not reach a card: it is the wrong owner.
const chromeScan = C.initAll();
out.chrome_scanFoundNoCards = chromeScan.headers.length === 0 && chromeScan.bars.length === 0;
out.chrome_menuNotWiredByChromeScan = cardA._parts.trigger.dataset.juneauMenuWired !== '1';

// The cards runtime IS the owner.
K.initAll();
out.cards_wiredMenuTrigger = cardA._parts.trigger.dataset.juneauMenuWired === '1';
out.cards_hydratedIcon = cardA._parts.trigger.querySelector('.jc-icon') != null;

// MENU forwards to the shared views stack: portalled to body, position:fixed, kind "menu", no dialog-cap inflation.
cardA._parts.trigger.dispatch('click', clickEv());
out.menu_topIsMenu = V.topLayer()?.kind === 'menu';
out.menu_portalledToBody = cardA._parts.menu.parentNode === env.body;
out.menu_escapedCard = !cardA.contains(cardA._parts.menu);
out.menu_positionFixed = cardA._parts.menu.style.position === 'fixed';
out.menu_ariaExpanded = cardA._parts.trigger.getAttribute('aria-expanded');
out.menu_notADialog = V.dialogLayerCount() === 0;

// Escape unwinds through that same stack (the shared owner resets ARIA + hides).
env.dispatchDocument('keydown', { key: 'Escape' });
out.menu_escClosed = V.topLayer() === null;
out.menu_escAriaReset = cardA._parts.trigger.getAttribute('aria-expanded');
drain();

// Two cards declaring the same action id keep separate menus: each trigger controls its own card-scoped list id.
out.menu_scopedIds = cardA._parts.menuId !== cardB._parts.menuId;
out.menu_scopeA = cardA._parts.menuId;
out.menu_scopeB = cardB._parts.menuId;
cardB._parts.trigger.dispatch('click', clickEv());
out.menu_secondCardOpensItsOwnList = V.topLayer()?.el === cardB._parts.menu;
drain();

// SAFE card action dispatches the shared host CustomEvent, carrying its own action id and the card as its root.
const safeSeen = [];
cardA._parts.safe.addEventListener(C.SAFE_EVENT, function (ev) { safeSeen.push(ev); });
cardA._parts.safe.dispatch('click', clickEv());
out.safe_eventCount = safeSeen.length;
out.safe_eventName = C.SAFE_EVENT;
out.safe_token = safeSeen.length ? safeSeen[0].detail.token : null;
out.safe_actionId = safeSeen.length ? safeSeen[0].detail.actionId : null;
out.safe_rootIsTheCard = safeSeen.length ? safeSeen[0].detail.root === cardA : false;
out.safe_bubbles = safeSeen.length ? safeSeen[0].bubbles === true : false;

// ==================================================================================================================
// B) A STATIC card (no refresh wire) with actions is still enhanced, and so is a grid-less single card
// ==================================================================================================================

const staticCard = buildCard({ id: 'c3', scope: 'g1:c3', menu: true });
grid.appendChild(staticCard);
const loneCard = buildCard({ id: 'solo', scope: 'solo', menu: true });   // the per-card emit path mints no grid
env.body.appendChild(loneCard);
K.initAll();
out.static_cardEnhanced = staticCard._parts.trigger.dataset.juneauMenuWired === '1';
out.gridless_cardEnhanced = loneCard._parts.trigger.dataset.juneauMenuWired === '1';
loneCard._parts.trigger.dispatch('click', clickEv());
out.gridless_menuOpensOnSharedStack = V.topLayer()?.kind === 'menu';
drain();

// A card built by a different runtime is refused whole: the handshake failure withholds action wiring too.
const staleCard = buildCard({ id: 'c9', scope: 'g1:c9', menu: true, refresh: '/data/summary' });
staleCard.dataset.juneauCardContract = '2';
grid.appendChild(staleCard);
K.initAll();
out.stale_notEnhanced = staleCard._parts.trigger.dataset.juneauMenuWired !== '1';
out.stale_bannerShown = staleCard._parts.banner.hidden === false;

// ==================================================================================================================
// C) Hidden card: a hosted view table STILL inits at DOMContentLoaded, while a refreshable sibling's poll suspends
// ==================================================================================================================

// A DataTables stub whose construction throws: init runs to the hand-off, so the ctx it stamped is readable while no
// real table is built (the nested-table harness uses the same trick).
env.window.jQuery = function () { return { DataTable: function () { throw new Error('stub: no real DataTables'); } }; };
env.window.jQuery.fn = { DataTable: function () {}, dataTable: { isDataTable: function () { return false; } } };

const panel = mkEl('div', 'jc-panel');   // an INACTIVE pages tab panel: CSS display:none above the grid
env.body.appendChild(panel);
const hiddenGrid = mkEl('section', 'juneau-view-card-grid', { 'data-juneau-card-grid': '1', 'id': 'g2' });
panel.appendChild(hiddenGrid);
const hostCard = buildCard({ id: 'h1', scope: 'g2:h1' });
hiddenGrid.appendChild(hostCard);
const hostedTable = hostView(hostCard, 'orders', 'g2:h1:orders', '/data/hidden');
const pollingSibling = buildCard({ id: 'h2', scope: 'g2:h2', refresh: '/data/summary', poll: 10000 });
hiddenGrid.appendChild(pollingSibling);

out.hidden_cardIsHidden = K.isElementHidden(hostCard) === true;
const hiddenCtl = K.initCard(pollingSibling);
out.hidden_pollSuspended = hiddenCtl?.running === false;

// NOSONAR javascript:S2486 -- the stub DataTable constructor is designed to throw at hand-off (there is no real
// DataTables here); swallowing it is intentional so the ctx it already stamped can be inspected below.
try { V.initAll(); } catch { /* stubbed DataTable construction throws at hand-off; ctx stamped before the throw */ }
out.hidden_hostedTableInited = hostedTable.__juneauCtx != null;
out.hidden_hostedTableReadItsOwnSidecar = hostedTable.__juneauCtx
	? hostedTable.__juneauCtx.viewDef.dataUrl : null;
out.hidden_markerStaysAuthorId = hostedTable.dataset.juneauView;

process.stdout.write(JSON.stringify(out));
