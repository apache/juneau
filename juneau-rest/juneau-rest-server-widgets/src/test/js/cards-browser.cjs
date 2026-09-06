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
 * cards-browser.cjs - Node harness for the juneau-cards.js DOM binding layer (initCard): contract handshake +
 * banner, client-side endpoint re-check, refresh-button fetch wiring (cache:"no-store"), textContent field fill,
 * aria-busy lifecycle, concurrent-click coalescing, icon-glyph injection, and MutationObserver poll teardown.
 *
 *   Usage:  node cards-browser.cjs <path-to-juneau-cards.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const cardsJsPath = process.argv[2];
if (!cardsJsPath) {
	console.error('usage: node cards-browser.cjs <juneau-cards.js>');
	process.exit(2);
}

// --- minimal DOM ---------------------------------------------------------------------------------------------------

function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	if (sel.charAt(0) === '.') {
		const raw = ' ' + (node.getAttribute('class') || '') + ' ';
		return raw.indexOf(' ' + sel.slice(1) + ' ') >= 0;
	}
	if (sel.charAt(0) === '#') return node.getAttribute('id') === sel.slice(1);
	const m = /^\[([\w:-]+)(?:="([^"]*)")?\]$/.exec(sel);
	if (m) { const v = node.getAttribute(m[1]); return m[2] == null ? v != null : v === m[2]; }
	return false;
}
function elWalk(node, sel, acc) {
	for (const c of node.childNodes) {
		if (c.nodeType === 1) {
			if (elMatches(c, sel)) acc.push(c);
			elWalk(c, sel, acc);
		}
	}
	return acc;
}
function datasetKeyToAttr(key) {
	return 'data-' + key.replace(/[A-Z]/g, function (m) { return '-' + m.toLowerCase(); });
}

/** A minimal live `dataset` facade over `node`'s existing attrs store, so `.dataset.x` reads/writes stay in sync
 * with getAttribute('data-x') the way a real DOM element's dataset does. */
function makeDataset(node) {
	return new Proxy({}, {
		get(_, key) {
			if (typeof key !== 'string') return undefined;
			const v = node.getAttribute(datasetKeyToAttr(key));
			return v == null ? undefined : v;
		},
		set(_, key, val) {
			node.setAttribute(datasetKeyToAttr(key), val);
			return true;
		},
		deleteProperty(_, key) {
			delete node.attrs[datasetKeyToAttr(key)];
			return true;
		},
		has(_, key) {
			return typeof key === 'string' && node.getAttribute(datasetKeyToAttr(key)) != null;
		}
	});
}
function el(tag) {
	const node = {
		nodeType: 1, tagName: String(tag).toUpperCase(), childNodes: [], attrs: {}, parentNode: null,
		hidden: false, disabled: false, innerHTML: '', _listeners: {}, _text: '',
		get dataset() { return makeDataset(this); },
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		hasAttribute: function (k) { return Object.hasOwn(this.attrs, k); },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		addEventListener: function (type, fn) {
			this._listeners[type] = this._listeners[type] || [];
			this._listeners[type].push(fn);
		},
		_fire: function (type, ev) { (this._listeners[type] || []).forEach(function (fn) { fn(ev || {}); }); },
		set textContent(v) { this.childNodes.length = 0; this._text = v == null ? '' : String(v); },
		get textContent() {
			if (this.childNodes.length === 0) return this._text || '';
			return this.childNodes.map(function (c) { return c.textContent; }).join('');
		}
	};
	return node;
}

/** Builds a refreshable card <article> DOM.  opts: {contract, refresh, poll, value}. */
function buildCard(opts) {
	const article = el('article');
	article.dataset.juneauCard = '1';
	article.dataset.juneauCardId = 'c1';
	article.setAttribute('aria-labelledby', 'g1-c1-title');
	if (opts.contract != null) article.dataset.juneauCardContract = opts.contract;
	if (opts.refresh != null) article.dataset.juneauCardRefresh = opts.refresh;
	if (opts.poll != null) article.dataset.juneauCardPollMs = String(opts.poll);

	const header = el('header');
	const title = el('span'); title.setAttribute('id', 'g1-c1-title'); title.textContent = 'Live';
	const status = el('span'); status.dataset.juneauCardStatus = '1'; status.hidden = true;
	const btn = el('button'); btn.dataset.juneauCardRefreshTrigger = '1';
	header.appendChild(title); header.appendChild(status); header.appendChild(btn);

	const banner = el('div'); banner.dataset.juneauCardBanner = '1'; banner.hidden = true;
	const body = el('div'); body.dataset.juneauCardBody = '1';
	const dd = el('dd'); dd.dataset.juneauCardField = 'k'; dd.textContent = opts.value == null ? 'OLD' : opts.value;
	body.appendChild(dd);

	article.appendChild(header); article.appendChild(banner); article.appendChild(body);
	article._parts = { title: title, status: status, btn: btn, banner: banner, body: body, dd: dd };
	return article;
}

// --- fakes ---------------------------------------------------------------------------------------------------------

let fetchCalls = [];
function makeFetch(envelope, reject) {
	return function (url, opts) {
		fetchCalls.push({ url: url, opts: opts });
		if (reject) return Promise.reject(new Error('network'));
		return Promise.resolve({ json: function () { return Promise.resolve(envelope); } });
	};
}

// Controllable timers so start()/stop() never schedule real work that keeps node alive.
let timerSeq = 1;
const timers = {};
function fakeSetTimeout(fn) { const id = timerSeq++; timers[id] = { fn: fn, kind: 't' }; return id; }
function fakeSetInterval(fn) { const id = timerSeq++; timers[id] = { fn: fn, kind: 'i' }; return id; }
function fakeClear(id) { delete timers[id]; }

let observerCallback = null;
function FakeMutationObserver(cb) { observerCallback = cb; this.observe = function () {}; this.disconnect = function () {}; }

// A fake JuneauViews.regions - juneau-cards.js never depends on the real juneau-regions.js (widgets never depends
// on views; see this module's pom.xml), so its region-enrolment call sites are proven against a stand-in that
// records every call, the same division of labour the fake fetch/MutationObserver above already use.  `enrolIn`
// mints a plain handle per `[data-juneau-region]` node under the given scope and marks it, mirroring the real
// runtime's `el._juneauRegion` idempotency mark closely enough for these tests (this file does not re-prove
// juneau-regions.js's own contract - that lives in the views module's Regions_* tests).
const regionCalls = { enrolIn: [], activateRegion: [], teardownRegionsIn: [], registerRuntime: [], ready: [] };
let nextRegionId = 1;
function makeFakeRegions() {
	return {
		enrolIn: function (scopeEl) {
			regionCalls.enrolIn.push(scopeEl);
			return scopeEl.querySelectorAll('[data-juneau-region]').map(function (regionEl) {
				const handle = { el: regionEl, key: regionEl.getAttribute('data-juneau-region'), deferred: false, torn: false, id: nextRegionId++ };
				regionEl._juneauRegion = handle;
				return handle;
			});
		},
		activateRegion: function (region) { regionCalls.activateRegion.push(region); },
		teardownRegionsIn: function (root) { regionCalls.teardownRegionsIn.push(root); },
		registerRuntime: function (token) { regionCalls.registerRuntime.push(token); },
		ready: function (token) { regionCalls.ready.push(token); }
	};
}

const iconsResolved = [];
const document = {
	readyState: 'loading',
	hidden: false,
	addEventListener: function () {},
	querySelectorAll: function () { return []; }
};
const window = {
	document: document,
	console: console,
	MutationObserver: FakeMutationObserver,
	JuneauViews: {
		icons: { resolveIcon: function (n) { iconsResolved.push(n); return '<svg data-icon="' + n + '"></svg>'; } },
		regions: makeFakeRegions()
	},
	setTimeout: fakeSetTimeout, setInterval: fakeSetInterval, clearTimeout: fakeClear, clearInterval: fakeClear
};
window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'FRESH' } });

const sandbox = {
	window: window, document: document, console: console,
	setTimeout: fakeSetTimeout, setInterval: fakeSetInterval, clearTimeout: fakeClear, clearInterval: fakeClear,
	Math: Math, Date: Date, Promise: Promise, Object: Object
};
// NOSONAR javascript:S1523 -- this harness deliberately loads the repo-local production juneau-cards.js
// (path given on the command line by the Java test), not untrusted/user-supplied input.
vm.runInNewContext(fs.readFileSync(path.resolve(cardsJsPath), 'utf8'), sandbox, { filename: 'juneau-cards.js' });

const I = window.JuneauCards?.init;
const out = { hasInit: typeof I?.initCard === 'function' };
// Captured immediately: registerRuntime() runs once at IIFE top level, during the vm.runInNewContext load above,
// not from any test action below.
out.regionsRegisterRuntimeCalledAtLoad = regionCalls.registerRuntime.indexOf('juneau-cards.js') >= 0;

(async function () {
	if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); return; }

	// A) Valid refreshable card: refresh() fetches with the right options, fills the field, toggles aria-busy.
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'FRESH' } });
	const cardA = buildCard({ contract: '1', refresh: '/data/summary' });
	const ctlA = I.initCard(cardA);
	out.a_ctl = !!ctlA;
	out.a_btnAria = cardA._parts.btn.getAttribute('aria-label');
	out.a_iconInjected = cardA._parts.btn.innerHTML.indexOf('data-icon="refresh"') >= 0;
	out.a_iconResolved = iconsResolved.indexOf('refresh') >= 0;
	out.a_statusRole = cardA._parts.status.getAttribute('role');
	out.a_ariaBusyDuring = null;
	await ctlA.refresh();
	out.a_fetchCount = fetchCalls.length;
	out.a_fetchUrl = fetchCalls[0].url;
	out.a_cacheNoStore = fetchCalls[0].opts.cache === 'no-store';
	out.a_credentials = fetchCalls[0].opts.credentials;
	out.a_method = fetchCalls[0].opts.method;
	out.a_fieldFilled = cardA._parts.dd.textContent;
	out.a_ariaBusyCleared = !cardA.hasAttribute('aria-busy');

	// B) Contract mismatch: banner shown, no fetch, refuse to enhance.
	fetchCalls = [];
	const cardB = buildCard({ contract: '2', refresh: '/data/summary' });
	const ctlB = I.initCard(cardB);
	out.b_noCtl = ctlB == null;
	out.b_bannerShown = cardB._parts.banner.hidden === false;
	out.b_bannerText = cardB._parts.banner.textContent.length > 0;
	out.b_noFetch = fetchCalls.length === 0;

	// C) Unsafe endpoint (client re-check): templated path refused with a banner, no fetch.
	fetchCalls = [];
	const cardC = buildCard({ contract: '1', refresh: '/cards/{id}' });
	const ctlC = I.initCard(cardC);
	out.c_noCtl = ctlC == null;
	out.c_bannerShown = cardC._parts.banner.hidden === false;
	out.c_noFetch = fetchCalls.length === 0;

	// D) Envelope contract mismatch on the wire: field NOT overwritten, error state set.
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '9', fields: { k: 'SHOULD_NOT_APPLY' } });
	const cardD = buildCard({ contract: '1', refresh: '/data/summary', value: 'ORIG' });
	const ctlD = I.initCard(cardD);
	await ctlD.refresh();
	out.d_fieldUnchanged = cardD._parts.dd.textContent === 'ORIG';
	out.d_statusError = cardD._parts.status.dataset.state === 'error';

	// E) Static card (no refresh attr): not enhanced, no fetch.
	fetchCalls = [];
	const cardE = buildCard({ contract: null, refresh: null });
	const ctlE = I.initCard(cardE);
	out.e_noCtl = ctlE == null;
	out.e_noFetch = fetchCalls.length === 0;

	// F) Concurrent-click coalescing: a second refresh while one is in flight is dropped (fetch fires once).
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'X' } });
	const cardF = buildCard({ contract: '1', refresh: '/data/summary' });
	const ctlF = I.initCard(cardF);
	const p1 = ctlF.refresh();
	const nBeforeSecond = fetchCalls.length;
	ctlF.refresh();       // coalesced: inFlight already true (returns a resolved thenable, no second fetch)
	out.f_secondDropped = fetchCalls.length === nBeforeSecond;
	await p1;
	out.f_fetchCount = fetchCalls.length;

	// G) Poll teardown via MutationObserver: hiding the card stops its timers; re-showing restarts them.
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'P' } });
	observerCallback = null;
	const cardG = buildCard({ contract: '1', refresh: '/data/summary', poll: 10000 });
	const grid = el('section'); grid.dataset.juneauCardGrid = '1'; grid.appendChild(cardG);
	const ctlG = I.initCard(cardG);
	out.g_startedRunning = ctlG.running === true;
	const obs = I.observeGrid(grid, [ctlG]);
	out.g_observerInstalled = obs != null && observerCallback != null;
	cardG.hidden = true;
	observerCallback();
	out.g_stoppedWhenHidden = ctlG.running === false;
	cardG.hidden = false;
	observerCallback();
	out.g_restartedWhenShown = ctlG.running === true;

	// H) isElementHidden walks ancestors.
	const parent = el('div'); parent.hidden = true;
	const child = el('div'); parent.appendChild(child);
	out.h_hiddenViaAncestor = I.isElementHidden(child) === true;
	const shown = el('div'); const kid = el('div'); shown.appendChild(kid);
	out.h_shown = I.isElementHidden(kid) === false;

	// I) The real pages tab-hide path: a card inside a .jc-panel that LACKS .jc-active is CSS-hidden (juneau-views.css
	//    keeps an inactive panel at display:none); juneau-pages.js toggles .jc-active on that ANCESTOR panel, never on
	//    the card subtree.  isElementHidden must report hidden, the poll must not start there, and the observer must
	//    stop/restart the timers as .jc-active is toggled on the ancestor panel.
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'P' } });
	observerCallback = null;
	const panel = el('div'); panel.setAttribute('class', 'jc-panel');       // inactive tab panel: CSS display:none
	const cardI = buildCard({ contract: '1', refresh: '/data/summary', poll: 10000 });
	const gridI = el('section'); gridI.dataset.juneauCardGrid = '1'; gridI.appendChild(cardI);
	panel.appendChild(gridI);
	out.i_hiddenInInactivePanel = I.isElementHidden(cardI) === true;
	const ctlI = I.initCard(cardI);
	out.i_notStartedInInactivePanel = ctlI.running === false;               // start() bails while the ancestor tab is hidden
	const obsI = I.observeGrid(gridI, [ctlI]);
	out.i_observerInstalled = obsI != null && observerCallback != null;
	panel.setAttribute('class', 'jc-panel jc-active');                      // pages shows this tab
	observerCallback();
	out.i_startedWhenActivated = ctlI.running === true;
	out.i_shownWhenActive = I.isElementHidden(cardI) === false;
	panel.setAttribute('class', 'jc-panel');                               // pages hides this tab again
	observerCallback();
	out.i_stoppedWhenDeactivated = ctlI.running === false;

	// J) Region enrolment (design §9.3 round-5/round-6 fix): a STATIC card (no refresh attr - never reaches
	// initCard) with a region body still gets its region enrolled, and `groupFor` runs unconditionally so the
	// group record exists with `regions` populated even though `controls` stays empty.
	regionCalls.enrolIn = [];
	const cardJ = buildCard({ contract: null, refresh: null });
	const regionElJ = el('div'); regionElJ.setAttribute('data-juneau-region', 'stats');
	cardJ._parts.body.appendChild(regionElJ);
	const groupsJ = [];
	I.enhanceOneCard(cardJ, groupsJ);
	out.j_enrolInCalledWithCard = regionCalls.enrolIn.length === 1 && regionCalls.enrolIn[0] === cardJ;
	out.j_groupHasRegion = groupsJ.length === 1 && groupsJ[0].regions.length === 1
		&& groupsJ[0].regions[0].el === regionElJ;
	out.j_staticCardStillNoControls = groupsJ[0].controls.length === 0;

	// K) Missing region runtime (design §9.3's failure mode): with no `JuneauViews.regions` loaded, a
	// `[data-juneau-region]` node renders LOUD (one console.error naming the missing asset + a visible error
	// state), never silently blank.
	const cardK = buildCard({ contract: null, refresh: null });
	const regionElK = el('div'); regionElK.setAttribute('data-juneau-region', 'missing');
	cardK._parts.body.appendChild(regionElK);
	const errorsK = [];
	const savedRegions = window.JuneauViews.regions;
	const origError = window.console.error;
	window.console.error = function (msg) { errorsK.push(String(msg)); };
	window.JuneauViews.regions = null;
	I.enhanceOneCard(cardK, []);
	window.JuneauViews.regions = savedRegions;
	window.console.error = origError;
	out.k_errorReported = errorsK.some(function (m) { return m.indexOf('juneau-regions.js is not loaded') >= 0; });
	out.k_stateError = regionElK.getAttribute('data-juneau-region-state') === 'error';
	out.k_hasErrorText = regionElK.textContent.indexOf('juneau-regions.js is not loaded') >= 0;

	// L) observeGrid's region loop (design §9.3 rule 5's cards-runtime half): a connected, visible enrolled region
	// gets activateRegion() called on it every tick; a detached one never does.
	regionCalls.activateRegion = [];
	const visibleRegionEl = el('div'); visibleRegionEl.isConnected = true;
	const detachedRegionEl = el('div'); detachedRegionEl.isConnected = false;
	const rVisible = { el: visibleRegionEl, deferred: true, torn: false };
	const rDetached = { el: detachedRegionEl, deferred: true, torn: false };
	const gridL = el('section'); gridL.dataset.juneauCardGrid = '1';
	observerCallback = null;
	const obsL = I.observeGrid(gridL, [], [rVisible, rDetached]);
	out.l_observerInstalledForRegionsOnly = obsL != null && observerCallback != null;
	observerCallback();
	out.l_visibleActivated = regionCalls.activateRegion.indexOf(rVisible) >= 0;
	out.l_detachedNotActivated = regionCalls.activateRegion.indexOf(rDetached) < 0;

	// M) ready() protocol: initAll()'s card walk in this harness sees no cards (document.querySelectorAll is
	// stubbed to return []), but that walk still completes synchronously, so ready() must fire regardless.
	regionCalls.ready = [];
	I.initAll();
	out.m_readyCalledWithToken = regionCalls.ready.indexOf('juneau-cards.js') >= 0;

	process.stdout.write(JSON.stringify(out));
})();
