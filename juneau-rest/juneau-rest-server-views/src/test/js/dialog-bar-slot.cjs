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
 * dialog-bar-slot.cjs - always-on Node harness for the THIRD named bar-slot host: a BarSlot anchored to a dialog's
 * title (ModalDef.barSlot).  Loads juneau-views.js AND juneau-chrome.js into ONE sandbox sharing one fake document,
 * the same shape detail-bar-slot.cjs uses for the row-detail host, because this slice reuses that host's identity
 * minting / enhance-on-insert / teardown machinery verbatim rather than reimplementing it.
 *
 *   Usage:  node dialog-bar-slot.cjs <juneau-views.js> <juneau-chrome.js>
 *
 * Covers: painting the region + widgets + sidecar from JSON (client-side, since a dialog has no server-rendered pass
 * to ride into); the dialog-title anchor attribute; clone-time id minting reused from the row-detail host for two
 * stacked dialogs; enhance-on-insert via JuneauChrome.init.initAll() sharing the wired marker with wireSafeActions;
 * collapse teardown; and the fail-closed no-op for a missing/malformed/empty bar slot.
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
const chromeJsPath = process.argv[3];
if (!viewsJsPath || !chromeJsPath) {
	console.error('usage: node dialog-bar-slot.cjs <juneau-views.js> <juneau-chrome.js>');
	process.exit(2);
}

// ------------------------------------------------------------------------------------------------------------------
// Fake DOM.  Verbatim copy of detail-bar-slot.cjs's self-contained shim (deliberately not shared via a shim module -
// see that file's own header note) - rich enough for createElement/insertBefore/appendChild/querySelectorAll/
// getElementById/dataset/addEventListener-dispatchEvent, which is everything the paths under test touch.
// ------------------------------------------------------------------------------------------------------------------

function matchesClassToken(node, tok) {
	const raw = ' ' + (node.className || node.getAttribute('class') || '') + ' ';
	return raw.indexOf(' ' + tok + ' ') >= 0;
}

function matchesAttrToken(node, name, value) {
	const v = node.getAttribute(name);
	if (v == null) return false;
	return value === undefined || v === value;
}

function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	if (sel.indexOf(',') >= 0)
		return sel.split(',').some(function (part) { return elMatches(node, part.trim()); });
	let rest = sel.trim();
	const tm = /^[a-zA-Z][\w-]*/.exec(rest);
	if (tm) {
		if (node.tagName !== tm[0].toUpperCase()) return false;
		rest = rest.slice(tm[0].length);
		if (!rest) return true;
	}
	while (rest.length) {
		const cm = /^\.([\w-]+)/.exec(rest);
		if (cm) {
			if (!matchesClassToken(node, cm[1])) return false;
			rest = rest.slice(cm[0].length);
			continue;
		}
		const am = /^\[([\w:-]+)(?:=["']?([^\]"']*)["']?)?\]/.exec(rest);
		if (am) {
			if (!matchesAttrToken(node, am[1], am[2])) return false;
			rest = rest.slice(am[0].length);
			continue;
		}
		return false;
	}
	return true;
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

function closestFrom(node, sel) {
	let n = node;
	while (n && n.nodeType === 1) {
		if (elMatches(n, sel)) return n;
		n = n.parentNode;
	}
	return null;
}

function dispatchFrom(node, ev) {
	let n = node;
	while (n) {
		(n._listeners && n._listeners[ev.type] || []).slice().forEach(function (fn) { fn(ev); });
		n = ev.bubbles ? n.parentNode : null;
	}
}

function datasetKeyToAttr(key) {
	return 'data-' + key.replace(/[A-Z]/g, function (m) { return '-' + m.toLowerCase(); });
}

function attrToDatasetKey(attr) {
	return attr.slice(5).replace(/-([a-z])/g, function (_, c) { return c.toUpperCase(); });
}

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
			node.removeAttribute(datasetKeyToAttr(key));
			return true;
		},
		has(_, key) {
			return typeof key === 'string' && node.getAttribute(datasetKeyToAttr(key)) != null;
		},
		ownKeys() {
			return Object.keys(node.attrs)
				.filter(function (k) { return k.indexOf('data-') === 0; })
				.map(attrToDatasetKey);
		},
		getOwnPropertyDescriptor(_, key) {
			const v = node.getAttribute(datasetKeyToAttr(key));
			return v == null ? undefined : { value: v, enumerable: true, configurable: true };
		}
	});
}

const byId = {};

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		parentNode: null,
		className: '',
		hidden: false,
		_listeners: {},
		_text: '',
		get firstChild() { return this.childNodes[0] || null; },
		get nextSibling() {
			if (!this.parentNode) return null;
			const kids = this.parentNode.childNodes;
			const i = kids.indexOf(this);
			return i >= 0 && i + 1 < kids.length ? kids[i + 1] : null;
		},
		get dataset() { return makeDataset(this); },
		getAttribute: function (k) {
			if (k === 'class' && this.className) return this.className;
			return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null;
		},
		setAttribute: function (k, v) {
			this.attrs[k] = v == null ? '' : String(v);
			if (k === 'class') this.className = this.attrs[k];
			if (k === 'id') byId[this.attrs[k]] = this;
		},
		removeAttribute: function (k) {
			if (k === 'id' && this.attrs.id != null) delete byId[this.attrs.id];
			delete this.attrs[k];
			if (k === 'class') this.className = '';
		},
		remove: function () {
			if (this.parentNode) {
				const kids = this.parentNode.childNodes;
				const i = kids.indexOf(this);
				if (i >= 0) kids.splice(i, 1);
				this.parentNode = null;
			}
		},
		_detach: function () {
			this.remove();
		},
		appendChild: function (c) {
			if (c._detach) c._detach();
			this.childNodes.push(c);
			c.parentNode = this;
			return c;
		},
		insertBefore: function (c, ref) {
			if (c._detach) c._detach();
			const i = ref ? this.childNodes.indexOf(ref) : -1;
			if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) {
			c.remove();
			return c;
		},
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		closest: function (sel) { return closestFrom(this, sel); },
		contains: function (other) {
			if (other === this) return true;
			for (const c of this.childNodes) {
				if (c === other) return true;
				if (c.nodeType === 1 && c.contains?.(other)) return true;
			}
			return false;
		},
		addEventListener: function (type, fn) {
			this._listeners[type] = this._listeners[type] || [];
			this._listeners[type].push(fn);
		},
		listenerCount: function (type) { return (this._listeners[type] || []).length; },
		dispatchEvent: function (ev) {
			dispatchFrom(this, ev);
			return true;
		},
		focus: function () { document.activeElement = this; },
		getBoundingClientRect: function () { return { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 }; },
		set textContent(v) { this.childNodes.length = 0; this._text = v == null ? '' : String(v); },
		get textContent() {
			if (this.childNodes.length === 0) return this._text || '';
			return this.childNodes.map(function (c) { return c.textContent; }).join('');
		}
	};
	return node;
}

const body = el('body');

const document = {
	readyState: 'loading',
	activeElement: null,
	body: body,
	addEventListener: function () {},
	createElement: function (tag) { return el(tag); },
	getElementById: function (id) { return Object.hasOwn(byId, id) ? byId[id] : null; },
	querySelectorAll: function (sel) { return body.querySelectorAll(sel); },
	querySelector: function (sel) { return body.querySelector(sel); }
};

function CustomEvent(type, init) {
	this.type = type;
	this.detail = init && init.detail;
	this.bubbles = !!(init && init.bubbles);
	this.defaultPrevented = false;
	this.preventDefault = function () { this.defaultPrevented = true; };
}

const window = {
	document: document,
	console: console,
	jQuery: undefined,
	innerWidth: 1024,
	innerHeight: 768,
	CustomEvent: CustomEvent,
	addEventListener: function () {},
	matchMedia: function () { return { matches: false, addEventListener: function () {} }; },
	getComputedStyle: function () { return { getPropertyValue: function () { return ''; } }; }
};

const sandbox = {
	window: window,
	document: document,
	console: console,
	CustomEvent: CustomEvent,
	setTimeout: function (fn) { if (typeof fn === 'function') { fn(); } return 0; },
	clearTimeout: function () {},
	setInterval: function () { return 0; },
	clearInterval: function () {},
	Promise: Promise
};

// NOSONAR javascript:S1523 -- this harness's entire purpose is to load the production runtime under test (a
// repo-local file path from argv, not attacker-controlled input) into an isolated VM sandbox; that IS the test.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
// NOSONAR javascript:S1523 -- same rationale: loading the production juneau-chrome.js under test into the sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const VNS = window.JuneauViews;
const V = VNS?.init;
const CNS = window.JuneauChrome;
const C = CNS?.init;

const out = {
	hasViews: !!(typeof V?.buildDialogOverlay === 'function'),
	hasChrome: !!(typeof C?.initAll === 'function'),
	hasBuildRegion: !!(typeof V?.buildDialogBarSlotRegion === 'function'),
	hasInsert: !!(typeof V?.insertDialogBarSlot === 'function'),
	hasMint: !!(typeof V?.mintDetailBarSlotIdentity === 'function'),
	hasTeardown: !!(typeof V?.teardownDetailBarSlot === 'function'),
	hasEnhance: !!(typeof V?.enhanceChromeInPanel === 'function')
};
if (!(out.hasViews && out.hasChrome && out.hasBuildRegion && out.hasInsert && out.hasMint && out.hasTeardown
		&& out.hasEnhance)) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const BAR_MARKER = 'data-juneau-bar-slot';
const BAR_ANCHOR = 'data-juneau-bar-slot-anchor';
const BAR_META = 'data-juneau-bar-meta';
const BAR_WIDGET_MARKER = 'data-juneau-bar-widget';
const SIDECAR_PREFIX = 'juneau-bar:';
const AUTHOR_ID = 'dialog-ctx';

// ------------------------------------------------------------------------------------------------------------------
// Fixture builders: a hand-painted equivalent of what buildDialogOverlay assembles (backdrop > dialog > title).
// ------------------------------------------------------------------------------------------------------------------

function barSlotJson(id, count) {
	return {
		contractVersion: '1',
		id: id || AUTHOR_ID,
		widgets: [
			{ id: 'state', text: 'Region us-east' },
			{ id: 'open', label: 'Open', badge: { count: count == null ? 3 : count } }
		]
	};
}

function dialogFixture() {
	const backdrop = el('div');
	backdrop.setAttribute('class', 'juneau-view-dialog-backdrop');
	const dialog = el('div');
	dialog.setAttribute('class', 'juneau-view-dialog');
	dialog.setAttribute('role', 'dialog');
	const title = el('h2');
	title.setAttribute('class', 'juneau-view-dialog-title');
	title.textContent = 'Really delete?';
	dialog.appendChild(title);
	backdrop.appendChild(dialog);
	return { backdrop: backdrop, dialog: dialog, title: title };
}

function regionsIn(root) {
	return root.querySelectorAll('[' + BAR_MARKER + ']');
}

// ------------------------------------------------------------------------------------------------------------------
// 1. Paint from JSON: widgets, anchor attribute, placement as the title's immediate next sibling, id-less sidecar.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	const f = dialogFixture();
	const inserted = V.insertDialogBarSlot(f.dialog, f.title, barSlotJson(), 1);
	out.paint_inserted = inserted;
	out.paint_regionCount = regionsIn(f.dialog).length;

	const region = regionsIn(f.dialog)[0];
	out.paint_regionFollowsTitle = f.title.nextSibling === region;
	out.paint_anchorIsDialogTitle = region.getAttribute(BAR_ANCHOR) === 'dialog-title';
	out.paint_hasBarSlotClass = matchesClassToken(region, 'jc-bar-slot');
	out.paint_hasDialogSlotClass = matchesClassToken(region, 'juneau-view-dialog-bar-slot');

	const textWidget = region.querySelector('.jc-bar-text');
	out.paint_textWidgetPainted = !!textWidget && textWidget.textContent === 'Region us-east';
	out.paint_textWidgetMarker = textWidget?.getAttribute(BAR_WIDGET_MARKER);

	const badgeWidget = region.querySelector('.jc-bar-badge');
	out.paint_badgeLabelPainted = badgeWidget?.querySelector('.jc-bar-label')?.textContent === 'Open';
	const badge = badgeWidget?.querySelector('[data-juneau-badge]');
	out.paint_badgeCountPainted = badge?.textContent === '3';
	out.paint_badgeNamespaced = badge?.getAttribute('data-juneau-badge') === 'bar:open';

	const sidecar = f.dialog.querySelector('[' + BAR_META + ']');
	out.paint_sidecarIsIdLess_beforeMint = false;   // insertDialogBarSlot mints immediately; checked via mint_ below
	out.paint_sidecarPresent = !!sidecar;
	out.paint_sidecarJsonOk = (function () {
		try {
			const j = JSON.parse(sidecar.textContent);
			return j.contractVersion === '1' && j.badges['bar:open'] === 3;
		} catch (e) { return false; }
	})();
})();

// ------------------------------------------------------------------------------------------------------------------
// 2. Clone-time id minting is REUSED verbatim from the row-detail host, namespaced by dialogSeq for two stacked
//    dialogs - each must resolve its OWN sidecar, never the other's.
// ------------------------------------------------------------------------------------------------------------------

const fA = dialogFixture();
const fB = dialogFixture();

(function () {
	body.appendChild(fA.backdrop);
	body.appendChild(fB.backdrop);
	V.insertDialogBarSlot(fA.dialog, fA.title, barSlotJson('dialog-ctx', 3), 1);
	V.insertDialogBarSlot(fB.dialog, fB.title, barSlotJson('dialog-ctx', 7), 2);

	const regionA = regionsIn(fA.dialog)[0];
	const regionB = regionsIn(fB.dialog)[0];
	out.mint_markerA = regionA.getAttribute(BAR_MARKER);
	out.mint_markerB = regionB.getAttribute(BAR_MARKER);
	out.mint_markerA_isDialogSeq1 = out.mint_markerA === 'dialog:1';
	out.mint_markerB_isDialogSeq2 = out.mint_markerB === 'dialog:2';
	out.mint_distinct = out.mint_markerA !== out.mint_markerB;

	const sidecarA = fA.dialog.querySelector('[' + BAR_META + ']');
	const sidecarB = fB.dialog.querySelector('[' + BAR_META + ']');
	out.mint_sidecarIdA = sidecarA.getAttribute('id');
	out.mint_sidecarIdB = sidecarB.getAttribute('id');
	out.mint_sidecarIdA_isPrefixPlusMarker = out.mint_sidecarIdA === SIDECAR_PREFIX + out.mint_markerA;
	out.mint_authorIdUnresolvable = document.getElementById(SIDECAR_PREFIX + AUTHOR_ID) === null;

	const rtA = C.readSidecar(SIDECAR_PREFIX, out.mint_markerA);
	const rtB = C.readSidecar(SIDECAR_PREFIX, out.mint_markerB);
	out.mint_roundTripA = !!(rtA?.badges?.['bar:open'] === 3);
	out.mint_roundTripB = !!(rtB?.badges?.['bar:open'] === 7);
})();

// ------------------------------------------------------------------------------------------------------------------
// 3. Enhance-on-insert: initAll() (reused verbatim via enhanceChromeInPanel) applies the sidecar counts and shares
//    the SAFE_WIRED_ATTR marker with wireSafeActions - a re-scan must not double-bind an existing SAFE action.
// ------------------------------------------------------------------------------------------------------------------

let safeAction = null;
let safeFires = 0;

(function () {
	const header = el('div');
	header.setAttribute('class', 'jc-app-header');
	header.dataset.juneauAppHeader = 'main';
	safeAction = el('button');
	safeAction.dataset.juneauHeaderAction = 'ping';
	safeAction.dataset.juneauBehavior = 'safe';
	safeAction.dataset.juneauSafe = 'ping';
	header.appendChild(safeAction);
	body.appendChild(header);
	header.addEventListener(C.SAFE_EVENT, function () { safeFires++; });

	const badgeA = fA.dialog.querySelector('[data-juneau-badge]');
	out.enh_countBeforeA = badgeA.textContent;   // painted at insert-time from the JSON count itself
	out.enh_seamCalled = V.enhanceChromeInPanel(fA.dialog);
	out.enh_countAfterA = badgeA.textContent;
	out.enh_safeListenersAfterFirst = safeAction.listenerCount('click');
	out.enh_wiredMarker = safeAction.dataset.juneauSafeWired;

	// Idempotent: a second enhance-on-insert call must not re-bind the already-wired SAFE action.
	V.enhanceChromeInPanel(fA.dialog);
	out.enh_safeListenersAfterSecond = safeAction.listenerCount('click');
	out.enh_countStillA = badgeA.textContent;

	safeAction.dispatchEvent(new CustomEvent('click', { bubbles: true }));
	out.enh_safeFires = safeFires;

	// A dialog with NO bar slot must not drag chrome in at all (a confirm-only dialog stays a clean no-op).
	const fPlain = dialogFixture();
	out.enh_seamSkippedWithoutSlot = V.enhanceChromeInPanel(fPlain.dialog);
})();

// ------------------------------------------------------------------------------------------------------------------
// 4. Collapse teardown: reused verbatim from the row-detail host - drops the minted sidecar id without touching the
//    OTHER stacked dialog's own sidecar.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	V.teardownDetailBarSlot(fA.dialog);
	out.tear_sidecarIdRemoved = fA.dialog.querySelector('[' + BAR_META + ']').getAttribute('id') === null;
	out.tear_aUnresolvable = document.getElementById(SIDECAR_PREFIX + 'dialog:1') === null;
	out.tear_bStillResolvable = document.getElementById(SIDECAR_PREFIX + 'dialog:2') !== null;
	out.tear_bStillRoundTrips = !!C.readSidecar(SIDECAR_PREFIX, 'dialog:2');
	out.tear_noThrowOnNull = (function () { V.teardownDetailBarSlot(null); return true; })();
})();

// ------------------------------------------------------------------------------------------------------------------
// 5. Fail-closed no-op: a missing/malformed/empty bar slot never blocks the confirm/cancel dialog from opening, and
//    a contract-version mismatch is refused rather than painted.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	const fNone = dialogFixture();
	out.noop_undeclared = V.insertDialogBarSlot(fNone.dialog, fNone.title, undefined, 3);
	out.noop_undeclaredRegionCount = regionsIn(fNone.dialog).length;

	const fEmpty = dialogFixture();
	out.noop_emptyWidgets = V.insertDialogBarSlot(fEmpty.dialog, fEmpty.title,
		{ contractVersion: '1', id: 'x', widgets: [] }, 4);

	const fBadContract = dialogFixture();
	out.noop_badContractVersion = V.insertDialogBarSlot(fBadContract.dialog, fBadContract.title,
		{ contractVersion: '99', id: 'x', widgets: [{ id: 'a', text: 'A' }] }, 5);

	const fNoId = dialogFixture();
	out.noop_missingId = V.insertDialogBarSlot(fNoId.dialog, fNoId.title,
		{ contractVersion: '1', widgets: [{ id: 'a', text: 'A' }] }, 6);
})();

process.stdout.write(JSON.stringify(out, null, 2) + '\n');
