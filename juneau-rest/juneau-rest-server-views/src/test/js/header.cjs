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
 * header.cjs - always-on Node harness for the juneau-chrome.js PURE helpers + DOM binding: endpoint/token safety,
 * envelope handshake, count clamp, icon hydration (single innerHTML sink), namespaced count apply, SAFE CustomEvent
 * dispatch, avatar image fallback, the thin pushLayer forward, and demand-only refresh with contract handshake.
 *
 *   Usage:  node header.cjs <path-to-juneau-chrome.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const chromeJsPath = process.argv[2];
if (!chromeJsPath) {
	console.error('usage: node header.cjs <juneau-chrome.js>');
	process.exit(2);
}

// ------------------------------------------------------------------------------------------------------------------
// Minimal fake DOM: enough to exercise the querySelector[All] / getAttribute / textContent / innerHTML / event paths
// juneau-chrome.js actually uses.  Supports tag, .class, #id and [attr] / [attr='val'] selectors (concatenated).
// ------------------------------------------------------------------------------------------------------------------

function walk(el, fn) {
	const kids = el.children || [];
	for (let i = 0; i < kids.length; i++) { fn(kids[i]); walk(kids[i], fn); }
}

function matches(el, sel) {
	if (el.nodeType !== 1) return false;
	let i = 0;
	const tagM = /^[a-zA-Z][a-zA-Z0-9-]*/.exec(sel);
	if (tagM) { if ((el.tag || '').toLowerCase() !== tagM[0].toLowerCase()) return false; i = tagM[0].length; }
	const rest = sel.slice(i);
	const re = /\.([a-zA-Z0-9_-]+)|#([a-zA-Z0-9_:-]+)|\[([a-zA-Z0-9_-]+)(?:=(?:'([^']*)'|"([^"]*)"))?\]/g;
	let m;
	while ((m = re.exec(rest))) {
		if (m[1]) { if (el.classList.indexOf(m[1]) < 0) return false; }
		else if (m[2]) { if (el.getAttribute('id') !== m[2]) return false; }
		else {
			const name = m[3];
			const val = m[4] !== undefined ? m[4] : m[5];
			const av = el.getAttribute(name);
			if (av == null) return false;
			if (val !== undefined && av !== val) return false;
		}
	}
	return true;
}

function makeEl(tag, attrs, kids) {
	return {
		nodeType: 1,
		tag: tag,
		attrs: Object.assign({}, attrs || {}),
		children: kids || [],
		_text: '',
		_html: null,
		hidden: false,
		listeners: {},
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		get classList() { const c = this.getAttribute('class'); return c ? c.split(/\s+/) : []; },
		addEventListener: function (type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); },
		dispatchEvent: function (ev) { const ls = this.listeners[ev.type] || []; for (let i = 0; i < ls.length; i++) ls[i](ev); return true; },
		set textContent(v) { this._text = v == null ? '' : String(v); },
		get textContent() { return this._text; },
		set innerHTML(v) { this._html = v; },
		get innerHTML() { return this._html; },
		querySelectorAll: function (sel) { const out = []; walk(this, function (e) { if (matches(e, sel)) out.push(e); }); return out; },
		querySelector: function (sel) { let f = null; walk(this, function (e) { if (!f && matches(e, sel)) f = e; }); return f; }
	};
}

const registry = { refresh: '<svg>refresh</svg>' };

let fetchCalls = 0;
let nextEnvelope = null;
let pushCall = null;

const documentAll = [];
const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	getElementById: function (id) {
		for (let i = 0; i < documentAll.length; i++)
			if (documentAll[i].getAttribute && documentAll[i].getAttribute('id') === id) return documentAll[i];
		return null;
	}
};

const window = {
	document: document,
	console: console,
	CustomEvent: function (type, init) {
		this.type = type;
		this.detail = init && init.detail;
		this.bubbles = !!(init && init.bubbles);
		this.defaultPrevented = false;
		this.preventDefault = function () { this.defaultPrevented = true; };
	},
	fetch: function (url, opts) {
		fetchCalls++;
		window._lastFetch = { url: url, opts: opts };
		return Promise.resolve({ json: function () { return Promise.resolve(nextEnvelope); } });
	}
};

const sandbox = { window: window, document: document, console: console };
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const NS = window.JuneauChrome;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.applyCounts === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

(async function () {

	out.headerContract = I.JUNEAU_HEADER_CONTRACT_VERSION;
	out.barContract = I.JUNEAU_BAR_CONTRACT_VERSION;
	out.nsHeaderContract = NS.HEADER_CONTRACT_VERSION;
	out.nsBarContract = NS.BAR_CONTRACT_VERSION;

	// ---- Pure: endpoint safety (same-origin AND non-templated) ----
	out.ep_pathOk = I.isSafeChromeEndpoint('/chrome/counts');
	out.ep_relativeOk = I.isSafeChromeEndpoint('chrome/counts');
	out.ep_templated = I.isSafeChromeEndpoint('/x/{id}');
	out.ep_absolute = I.isSafeChromeEndpoint('http://evil/x');
	out.ep_protoRel = I.isSafeChromeEndpoint('//evil/x');
	out.ep_scheme = I.isSafeChromeEndpoint('servlet:/x');
	out.ep_js = I.isSafeChromeEndpoint('javascript:alert(1)');
	out.ep_dotdot = I.isSafeChromeEndpoint('/a/../b');
	out.ep_empty = I.isSafeChromeEndpoint('');

	// ---- Pure: SAFE token format ----
	out.tok_ok = I.isSafeToken('refresh-counts');
	out.tok_single = I.isSafeToken('r');
	out.tok_upper = I.isSafeToken('Refresh');
	out.tok_leadingDigit = I.isSafeToken('1x');
	out.tok_space = I.isSafeToken('a b');
	out.tok_tooLong = I.isSafeToken('a' + 'b'.repeat(64));

	// ---- Pure: envelope handshake + clamp + badges extract ----
	out.env_ok = I.envelopeContractOk({ contractVersion: '1', badges: {} }, '1');
	out.env_bad = I.envelopeContractOk({ contractVersion: '2' }, '1');
	out.env_missing = I.envelopeContractOk({ badges: {} }, '1');
	out.env_null = I.envelopeContractOk(null, '1');
	out.clamp_under = I.clampCount(5, 99);
	out.clamp_over = I.clampCount(120, 99);
	out.clamp_noMax = I.clampCount(7, null);
	out.badges_extract = I.envelopeBadges({ badges: { 'header:x': 3 } })['header:x'];
	out.badges_emptyIsObj = JSON.stringify(I.envelopeBadges({}));

	// ---- DOM: icon hydration (single innerHTML sink; registry markup only) ----
	const iconSpan = makeEl('span', { 'class': 'jc-icon' });
	const okAction = makeEl('a', { 'data-juneau-icon': 'refresh', 'data-juneau-header-action': 'd' }, [iconSpan]);
	const unknownSpan = makeEl('span', { 'class': 'jc-icon' });
	const unknownAction = makeEl('button', { 'data-juneau-icon': 'nope', 'data-juneau-header-action': 'e' }, [unknownSpan]);
	const iconRoot = makeEl('header', {}, [okAction, unknownAction]);
	// juneau-chrome.js resolves via window.JuneauViews.icons.resolveIcon:
	window.JuneauViews = { icons: { resolveIcon: function (n) { return Object.hasOwn(registry, n) ? registry[n] : null; } } };
	I.hydrateIcons(iconRoot);
	out.icon_injected = iconSpan.innerHTML;
	out.icon_unknownUntouched = unknownSpan.innerHTML === null;

	// ---- DOM: applyCounts by namespaced id, clamp with data-juneau-badge-max, unknown untouched ----
	const b1 = makeEl('span', { 'data-juneau-badge': 'header:reload', 'data-juneau-badge-max': '99' });
	b1.textContent = 'old';
	const b2 = makeEl('span', { 'data-juneau-badge': 'header:x' });
	b2.textContent = 'old2';
	const b3 = makeEl('span', { 'data-juneau-badge': 'header:z' });
	b3.textContent = 'keep';
	const countRoot = makeEl('header', {}, [b1, b2, b3]);
	I.applyCounts(countRoot, { 'header:reload': 120, 'header:x': 4, 'header:none': 9, 'header:z': 'bad' });
	out.count_clamped = b1.textContent;          // 120 > max 99 -> "99+"
	out.count_plain = b2.textContent;            // "4"
	out.count_unknownUntouched = b3.textContent; // non-number -> skipped -> "keep"

	// ---- DOM: SAFE wiring dispatches a CustomEvent; malformed token is refused (not wired) ----
	const safeAction = makeEl('button', {
		'data-juneau-header-action': 'reload', 'data-juneau-behavior': 'safe', 'data-juneau-safe': 'refresh-counts'
	});
	const safeRoot = makeEl('header', {}, [safeAction]);
	I.wireSafeActions(safeRoot);
	let captured = null;
	safeAction.addEventListener(I.SAFE_EVENT, function (ev) { captured = ev; });
	const clickEv = { type: 'click', defaultPrevented: false, preventDefault: function () { this.defaultPrevented = true; } };
	safeAction.dispatchEvent(clickEv);
	out.safe_dispatched = !!captured && captured.detail.token === 'refresh-counts' && captured.detail.actionId === 'reload';
	out.safe_bubbles = !!captured && captured.bubbles === true;
	out.safe_prevented = clickEv.defaultPrevented === true;

	const badTokenAction = makeEl('button', {
		'data-juneau-header-action': 'x', 'data-juneau-behavior': 'safe', 'data-juneau-safe': 'Bad Token'
	});
	I.wireSafeActions(makeEl('header', {}, [badTokenAction]));
	out.safe_malformedNotWired = !(badTokenAction.listeners && badTokenAction.listeners['click']);

	// ---- DOM: avatar image fallback to hidden initials ----
	const img = makeEl('img', { 'class': 'jc-avatar-img' });
	const initials = makeEl('span', { 'class': 'jc-avatar-initials' });
	initials.hidden = true;
	const avatar = makeEl('span', { 'data-juneau-avatar': '1' }, [img, initials]);
	I.wireAvatarFallback(makeEl('header', {}, [avatar]));
	img.dispatchEvent({ type: 'error' });
	out.avatar_imgHidden = img.hidden === true;
	out.avatar_initialsShown = initials.hidden === false;

	// ---- DOM: pushLayer is a THIN FORWARD to JuneauViews.init.pushLayer, else no-op ----
	const sentinel = { rec: true };
	window.JuneauViews.init = { pushLayer: function (el, opts) { pushCall = { el: el, opts: opts }; return sentinel; } };
	const someEl = makeEl('div', {});
	out.push_forwarded = I.pushLayer(someEl, { kind: 'menu' }) === sentinel && pushCall !== null && pushCall.el === someEl;
	delete window.JuneauViews.init;
	out.push_noop = I.pushLayer(someEl, {}) === null;

	// ---- DOM: initHeader handshake (mismatch -> null, no apply; ok -> counts applied from sidecar) ----
	const badBadge = makeEl('span', { 'data-juneau-badge': 'header:reload' });
	badBadge.textContent = 'keep';
	const badHeader = makeEl('header', { 'data-juneau-app-header': 'bad' }, [badBadge]);
	documentAll.push(makeEl('script', { 'id': 'juneau-header:bad' }));
	documentAll[documentAll.length - 1].textContent = '{"contractVersion":"2","badges":{"header:reload":50}}';
	out.init_mismatchNull = I.initHeader(badHeader) === null;
	out.init_mismatchNoApply = badBadge.textContent === 'keep';

	const okBadge = makeEl('span', { 'data-juneau-badge': 'header:reload' });
	okBadge.textContent = 'old';
	const okHeader = makeEl('header', { 'data-juneau-app-header': 'app' }, [okBadge]);
	const okSidecar = makeEl('script', { 'id': 'juneau-header:app' });
	okSidecar.textContent = '{"contractVersion":"1","badges":{"header:reload":50}}';
	documentAll.push(okSidecar);
	const ctl = I.initHeader(okHeader);
	out.init_okCtl = !!(ctl && typeof ctl.refresh === 'function');
	out.init_okApplied = okBadge.textContent;   // "50"

	// ---- DOM: demand-refresh handshake (async) ----
	const rBadge = makeEl('span', { 'data-juneau-badge': 'header:reload', 'data-juneau-badge-max': '99' });
	rBadge.textContent = 'old';
	const rHeader = makeEl('header', { 'data-juneau-app-header': 'app', 'data-juneau-refresh': '/chrome/counts' }, [rBadge]);

	fetchCalls = 0;
	nextEnvelope = { contractVersion: '1', badges: { 'header:reload': 120 } };
	out.refresh_okReturn = await I.refresh(rHeader);
	out.refresh_okApplied = rBadge.textContent;   // clamped "99+"
	out.refresh_okFetched = fetchCalls;
	out.refresh_fetchOpts = window._lastFetch && window._lastFetch.opts
		? (window._lastFetch.opts.credentials + '|' + window._lastFetch.opts.cache + '|' + window._lastFetch.opts.method) : '';

	rBadge.textContent = 'old';
	nextEnvelope = { contractVersion: '2', badges: { 'header:reload': 120 } };
	out.refresh_mismatchReturn = await I.refresh(rHeader);
	out.refresh_mismatchNoApply = rBadge.textContent === 'old';

	fetchCalls = 0;
	const unsafeHeader = makeEl('header', { 'data-juneau-app-header': 'app', 'data-juneau-refresh': 'http://evil/x' }, []);
	out.refresh_unsafeReturn = await I.refresh(unsafeHeader);
	out.refresh_unsafeNoFetch = fetchCalls === 0;

	process.stdout.write(JSON.stringify(out));
})();
