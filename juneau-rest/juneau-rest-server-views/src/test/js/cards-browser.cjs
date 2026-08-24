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
	for (let i = 0; i < node.childNodes.length; i++) {
		const c = node.childNodes[i];
		if (c.nodeType === 1) { if (elMatches(c, sel)) acc.push(c); elWalk(c, sel, acc); }
	}
	return acc;
}
function el(tag) {
	const node = {
		nodeType: 1, tagName: String(tag).toUpperCase(), childNodes: [], attrs: {}, parentNode: null,
		hidden: false, disabled: false, innerHTML: '', _listeners: {}, _text: '',
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		hasAttribute: function (k) { return Object.hasOwn(this.attrs, k); },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		addEventListener: function (type, fn) { (this._listeners[type] = this._listeners[type] || []).push(fn); },
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
	article.setAttribute('data-juneau-card', '1');
	article.setAttribute('data-juneau-card-id', 'c1');
	article.setAttribute('aria-labelledby', 'g1-c1-title');
	if (opts.contract != null) article.setAttribute('data-juneau-card-contract', opts.contract);
	if (opts.refresh != null) article.setAttribute('data-juneau-card-refresh', opts.refresh);
	if (opts.poll != null) article.setAttribute('data-juneau-card-poll-ms', String(opts.poll));

	const header = el('header');
	const title = el('span'); title.setAttribute('id', 'g1-c1-title'); title.textContent = 'Live';
	const status = el('span'); status.setAttribute('data-juneau-card-status', '1'); status.hidden = true;
	const btn = el('button'); btn.setAttribute('data-juneau-card-refresh-trigger', '1');
	header.appendChild(title); header.appendChild(status); header.appendChild(btn);

	const banner = el('div'); banner.setAttribute('data-juneau-card-banner', '1'); banner.hidden = true;
	const body = el('div'); body.setAttribute('data-juneau-card-body', '1');
	const dd = el('dd'); dd.setAttribute('data-juneau-card-field', 'k'); dd.textContent = opts.value == null ? 'OLD' : opts.value;
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
	JuneauViews: { icons: { resolveIcon: function (n) { iconsResolved.push(n); return '<svg data-icon="' + n + '"></svg>'; } } },
	setTimeout: fakeSetTimeout, setInterval: fakeSetInterval, clearTimeout: fakeClear, clearInterval: fakeClear
};
window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'FRESH' } });

const sandbox = {
	window: window, document: document, console: console,
	setTimeout: fakeSetTimeout, setInterval: fakeSetInterval, clearTimeout: fakeClear, clearInterval: fakeClear,
	Math: Math, Date: Date, Promise: Promise, Object: Object
};
vm.runInNewContext(fs.readFileSync(path.resolve(cardsJsPath), 'utf8'), sandbox, { filename: 'juneau-cards.js' });

const I = window.JuneauCards && window.JuneauCards.init;
const out = { hasInit: !!(I && typeof I.initCard === 'function') };

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
	out.d_statusError = cardD._parts.status.getAttribute('data-state') === 'error';

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
	const p2 = ctlF.refresh();       // dropped: inFlight already true
	out.f_secondDropped = p2 === undefined;
	await p1;
	out.f_fetchCount = fetchCalls.length;

	// G) Poll teardown via MutationObserver: hiding the card stops its timers; re-showing restarts them.
	fetchCalls = [];
	window.fetch = makeFetch({ contractVersion: '1', fields: { k: 'P' } });
	observerCallback = null;
	const cardG = buildCard({ contract: '1', refresh: '/data/summary', poll: 10000 });
	const grid = el('section'); grid.setAttribute('data-juneau-card-grid', '1'); grid.appendChild(cardG);
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

	process.stdout.write(JSON.stringify(out));
})();
