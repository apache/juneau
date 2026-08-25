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
	for (const k of kids) { fn(k); walk(k, fn); }
}

function matches(el, sel) {
	if (el.nodeType !== 1) return false;
	let i = 0;
	const tagM = /^[a-zA-Z][a-zA-Z0-9-]*/.exec(sel);
	if (tagM) { if ((el.tag || '').toLowerCase() !== tagM[0].toLowerCase()) return false; i = tagM[0].length; }
	const rest = sel.slice(i);
	// NOSONAR javascript:S5843 -- this regex intentionally recognizes .class / #id / [attr] selector tokens in one
	// pass for the fake-DOM selector matcher below; splitting the 3-way alternation would change which characters
	// are permitted inside each token with no behavior-preserving equivalent, so it is left as-is.
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
		attrs: { ...(attrs || {}) },
		children: kids || [],
		_text: '',
		_html: null,
		hidden: false,
		listeners: {},
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		get classList() { const c = this.getAttribute('class'); return c ? c.split(/\s+/) : []; },
		addEventListener: function (type, fn) {
			this.listeners[type] = this.listeners[type] || [];
			this.listeners[type].push(fn);
		},
		dispatchEvent: function (ev) {
			const ls = this.listeners[ev.type] || [];
			for (const l of ls) l(ev);
			return true;
		},
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

const documentAll = [];
const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	getElementById: function (id) {
		for (const d of documentAll) {
			if (d.getAttribute?.('id') === id) return d;
		}
		return null;
	}
};

const window = {
	document: document,
	console: console,
	CustomEvent: function (type, init) {
		this.type = type;
		this.detail = init?.detail;
		this.bubbles = !!(init?.bubbles);
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
// NOSONAR javascript:S1523 -- this harness's entire purpose is to load the production runtime under test (a
// repo-local file path from argv, not attacker-controlled input) into an isolated VM sandbox; that IS the test.
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const NS = window.JuneauChrome;
const I = NS?.init;
const out = { hasInit: !!(typeof I?.applyCounts === 'function') };

function runPureChecks() {
	out.headerContract = I.JUNEAU_HEADER_CONTRACT_VERSION;
	out.barContract = I.JUNEAU_BAR_CONTRACT_VERSION;
	out.nsHeaderContract = NS.HEADER_CONTRACT_VERSION;
	out.nsBarContract = NS.BAR_CONTRACT_VERSION;

	// ---- Pure: endpoint safety (same-origin AND non-templated) ----
	out.ep_pathOk = I.isSafeChromeEndpoint('/chrome/counts');
	out.ep_relativeOk = I.isSafeChromeEndpoint('chrome/counts');
	out.ep_templated = I.isSafeChromeEndpoint('/x/{id}');
	out.ep_absolute = I.isSafeChromeEndpoint('https://evil/x');
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
}

function runIconHydrationChecks() {
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
}

function runApplyCountsChecks() {
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
}

function runSafeWiringChecks() {
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
	out.safe_dispatched = captured?.detail.token === 'refresh-counts' && captured.detail.actionId === 'reload';
	out.safe_bubbles = captured?.bubbles === true;
	out.safe_prevented = clickEv.defaultPrevented === true;

	const badTokenAction = makeEl('button', {
		'data-juneau-header-action': 'x', 'data-juneau-behavior': 'safe', 'data-juneau-safe': 'Bad Token'
	});
	I.wireSafeActions(makeEl('header', {}, [badTokenAction]));
	out.safe_malformedNotWired = !badTokenAction.listeners?.['click'];
}

function runAvatarFallbackChecks() {
	// ---- DOM: avatar image fallback to hidden initials ----
	const img = makeEl('img', { 'class': 'jc-avatar-img' });
	const initials = makeEl('span', { 'class': 'jc-avatar-initials' });
	initials.hidden = true;
	const avatar = makeEl('span', { 'data-juneau-avatar': '1' }, [img, initials]);
	I.wireAvatarFallback(makeEl('header', {}, [avatar]));
	img.dispatchEvent({ type: 'error' });
	out.avatar_imgHidden = img.hidden === true;
	out.avatar_initialsShown = initials.hidden === false;
}

function runMenuChecks() {
	// ---- DOM: a MENU trigger opens/closes its .jc-menu list on the SHARED views stack (chrome defines NO stack) ----
	// The mock stands in for window.JuneauViews.init (445h): it records push/pop and runs onDismiss on pop, exactly as
	// the real stack does - so aria-expanded round-trips through the shared teardown, not a chrome-local closer.
	let menuPushCall = null, menuPopCall = null, lastMenuOpts = null;
	window.JuneauViews = window.JuneauViews || {};
	window.JuneauViews.init = {
		pushLayer: function (el, opts) { menuPushCall = { el: el, opts: opts }; lastMenuOpts = opts; return { el: el }; },
		popLayer: function (el) {
			menuPopCall = { el: el };
			if (lastMenuOpts && typeof lastMenuOpts.onDismiss === 'function') lastMenuOpts.onDismiss();
		}
	};
	const miLink = makeEl('a', { 'class': 'jc-menu-item', 'href': '/a', 'role': 'menuitem' });
	const miSafe = makeEl('button', { 'class': 'jc-menu-item', 'role': 'menuitem', 'data-juneau-safe': 'do-it' });
	const menuList = makeEl('div', { 'class': 'jc-menu', 'id': 'juneau-menu:app:more', 'role': 'menu' }, [miLink, miSafe]);
	documentAll.push(menuList);
	const menuTrigger = makeEl('button', {
		'data-juneau-header-action': 'more', 'data-juneau-behavior': 'menu', 'aria-haspopup': 'menu',
		'aria-expanded': 'false', 'aria-controls': 'juneau-menu:app:more'
	});
	const menuHeader = makeEl('header', { 'data-juneau-app-header': 'app' }, [menuTrigger]);
	I.wireMenus(menuHeader);
	menuTrigger.dispatchEvent({ type: 'click', preventDefault: function () { this.defaultPrevented = true; } });
	out.menu_pushedList = menuPushCall !== null && menuPushCall.el === menuList;
	out.menu_kind = menuPushCall?.opts ? menuPushCall.opts.kind : null;
	out.menu_portal = menuPushCall?.opts?.portal === true;
	out.menu_lightDismiss = menuPushCall?.opts?.lightDismiss === true;
	out.menu_returnFocusToTrigger = menuPushCall?.opts?.returnFocusTo === menuTrigger;
	out.menu_ariaExpandedOnOpen = menuTrigger.getAttribute('aria-expanded');
	// A SAFE menu item dispatches the host event FROM THE TRIGGER (so it bubbles through the header), then closes.
	let menuSafeCaptured = null;
	menuTrigger.addEventListener(I.SAFE_EVENT, function (ev) { menuSafeCaptured = ev; });
	miSafe.dispatchEvent({ type: 'click', preventDefault: function () { this.defaultPrevented = true; } });
	out.menu_safeDispatchedFromTrigger = menuSafeCaptured?.detail.token === 'do-it'
		&& menuSafeCaptured.detail.actionId === 'more';
	out.menu_closedOnSafe = menuPopCall !== null && menuPopCall.el === menuList;
	out.menu_ariaExpandedAfterClose = menuTrigger.getAttribute('aria-expanded');
}

function runInitHeaderChecks() {
	// ---- DOM: initHeader handshake (mismatch -> null, no apply; ok -> counts applied from sidecar) ----
	const badBadge = makeEl('span', { 'data-juneau-badge': 'header:reload' });
	badBadge.textContent = 'keep';
	const badHeader = makeEl('header', { 'data-juneau-app-header': 'bad' }, [badBadge]);
	documentAll.push(makeEl('script', { 'id': 'juneau-header:bad' }));
	documentAll.at(-1).textContent = '{"contractVersion":"2","badges":{"header:reload":50}}';
	out.init_mismatchNull = I.initHeader(badHeader) === null;
	out.init_mismatchNoApply = badBadge.textContent === 'keep';

	const okBadge = makeEl('span', { 'data-juneau-badge': 'header:reload' });
	okBadge.textContent = 'old';
	const okHeader = makeEl('header', { 'data-juneau-app-header': 'app' }, [okBadge]);
	const okSidecar = makeEl('script', { 'id': 'juneau-header:app' });
	okSidecar.textContent = '{"contractVersion":"1","badges":{"header:reload":50}}';
	documentAll.push(okSidecar);
	const ctl = I.initHeader(okHeader);
	out.init_okCtl = !!(typeof ctl?.refresh === 'function');
	out.init_okApplied = okBadge.textContent;   // "50"
}

async function runDemandRefreshChecks() {
	// ---- DOM: demand-refresh handshake (async) ----
	const rBadge = makeEl('span', { 'data-juneau-badge': 'header:reload', 'data-juneau-badge-max': '99' });
	rBadge.textContent = 'old';
	const rHeader = makeEl('header', { 'data-juneau-app-header': 'app', 'data-juneau-refresh': '/chrome/counts' }, [rBadge]);

	fetchCalls = 0;
	nextEnvelope = { contractVersion: '1', badges: { 'header:reload': 120 } };
	out.refresh_okReturn = await I.refresh(rHeader);
	out.refresh_okApplied = rBadge.textContent;   // clamped "99+"
	out.refresh_okFetched = fetchCalls;
	out.refresh_fetchOpts = window._lastFetch?.opts
		? (window._lastFetch.opts.credentials + '|' + window._lastFetch.opts.cache + '|' + window._lastFetch.opts.method) : '';

	rBadge.textContent = 'old';
	nextEnvelope = { contractVersion: '2', badges: { 'header:reload': 120 } };
	out.refresh_mismatchReturn = await I.refresh(rHeader);
	out.refresh_mismatchNoApply = rBadge.textContent === 'old';

	fetchCalls = 0;
	const unsafeHeader = makeEl('header', { 'data-juneau-app-header': 'app', 'data-juneau-refresh': 'https://evil/x' }, []);
	out.refresh_unsafeReturn = await I.refresh(unsafeHeader);
	out.refresh_unsafeNoFetch = fetchCalls === 0;
}

(async function () {
	if (!out.hasInit) {
		process.stdout.write(JSON.stringify(out));
		process.exit(0);
	}

	runPureChecks();
	runIconHydrationChecks();
	runApplyCountsChecks();
	runSafeWiringChecks();
	runAvatarFallbackChecks();
	runMenuChecks();
	runInitHeaderChecks();
	await runDemandRefreshChecks();

	process.stdout.write(JSON.stringify(out));
})();
