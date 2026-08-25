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
 * detail-bar-slot.cjs - always-on Node harness for the SECOND named bar-slot host: a BarSlot riding the row-detail
 * ribbon.  Loads juneau-views.js AND juneau-chrome.js into ONE sandbox sharing one fake document, because the whole
 * point of this slice is the seam between them: views clones + relocates + mints, chrome enhances.
 *
 *   Usage:  node detail-bar-slot.cjs <juneau-views.js> <juneau-chrome.js> [juneau-renders.js]
 *
 * Covers: the relocate step in the detail caller (2-section) and its absence (1-section); clone-time id minting for
 * two simultaneously-expanded rows; enhance-on-insert via JuneauChrome.init.initAll() including its idempotence and
 * the shared wired marker that stops wireSafeActions double-binding; demand refresh fetching exactly once; collapse
 * teardown; and that NO interval timer is ever created.
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
const chromeJsPath = process.argv[3];
const rendersJsPath = process.argv[4];
if (!viewsJsPath || !chromeJsPath) {
	console.error('usage: node detail-bar-slot.cjs <juneau-views.js> <juneau-chrome.js> [juneau-renders.js]');
	process.exit(2);
}

// ------------------------------------------------------------------------------------------------------------------
// Fake DOM.  Deliberately self-contained (the row-detail.cjs precedent) rather than extending views-dom-shim.cjs,
// which ten other harnesses share.  Rich enough for the paths under test: nextSibling, real re-parenting on
// insert/append, a document-wide getElementById that FORGETS an element whose id attribute is removed, and
// addEventListener/dispatchEvent so a double-bound SAFE click would be visible as two dispatches.
// ------------------------------------------------------------------------------------------------------------------

function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	if (sel.indexOf(',') >= 0)
		return sel.split(',').some(function (part) { return elMatches(node, part.trim()); });
	var rest = sel.trim();
	var tm = /^([a-zA-Z][\w-]*)(.*)$/.exec(rest);
	if (tm) {
		if (node.tagName !== tm[1].toUpperCase()) return false;
		rest = tm[2] || '';
		if (!rest) return true;
	}
	while (rest.length) {
		var cm = /^\.([\w-]+)/.exec(rest);
		if (cm) {
			var raw = ' ' + (node.className || node.getAttribute('class') || '') + ' ';
			if (raw.indexOf(' ' + cm[1] + ' ') < 0) return false;
			rest = rest.slice(cm[0].length);
			continue;
		}
		var am = /^\[([\w:-]+)(?:=["']?([^\]"']*)["']?)?\]/.exec(rest);
		if (am) {
			var v = node.getAttribute(am[1]);
			if (v == null) return false;
			if (am[2] !== undefined && v !== am[2]) return false;
			rest = rest.slice(am[0].length);
			continue;
		}
		return false;
	}
	return true;
}

function elWalk(node, sel, acc) {
	for (var i = 0; i < node.childNodes.length; i++) {
		var c = node.childNodes[i];
		if (c.nodeType === 1) {
			if (elMatches(c, sel)) acc.push(c);
			elWalk(c, sel, acc);
		}
	}
	return acc;
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
			var kids = this.parentNode.childNodes;
			var i = kids.indexOf(this);
			return i >= 0 && i + 1 < kids.length ? kids[i + 1] : null;
		},
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
		_detach: function () {
			if (this.parentNode) this.parentNode.removeChild(this);
		},
		appendChild: function (c) {
			if (c._detach) c._detach();
			this.childNodes.push(c);
			c.parentNode = this;
			return c;
		},
		insertBefore: function (c, ref) {
			if (c._detach) c._detach();
			var i = ref ? this.childNodes.indexOf(ref) : -1;
			if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) {
			var i = this.childNodes.indexOf(c);
			if (i >= 0) { this.childNodes.splice(i, 1); c.parentNode = null; }
			return c;
		},
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { var r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		closest: function (sel) {
			var n = this;
			while (n && n.nodeType === 1) { if (elMatches(n, sel)) return n; n = n.parentNode; }
			return null;
		},
		contains: function (other) {
			if (other === this) return true;
			for (var i = 0; i < this.childNodes.length; i++) {
				var c = this.childNodes[i];
				if (c === other) return true;
				if (c.nodeType === 1 && c.contains && c.contains(other)) return true;
			}
			return false;
		},
		addEventListener: function (type, fn) { (this._listeners[type] = this._listeners[type] || []).push(fn); },
		listenerCount: function (type) { return (this._listeners[type] || []).length; },
		dispatchEvent: function (ev) {
			var n = this;
			while (n) {
				(n._listeners && n._listeners[ev.type] || []).slice().forEach(function (fn) { fn(ev); });
				n = ev.bubbles ? n.parentNode : null;
			}
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
	// "loading" keeps both runtimes from self-initializing at load: this harness drives initAll() explicitly, which is
	// precisely the enhance-on-insert path under test.
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

let fetchCalls = 0;
let nextEnvelope = null;
let intervalCalls = 0;

const window = {
	document: document,
	console: console,
	jQuery: undefined,
	innerWidth: 1024,
	innerHeight: 768,
	CustomEvent: CustomEvent,
	addEventListener: function () {},
	matchMedia: function () { return { matches: false, addEventListener: function () {} }; },
	getComputedStyle: function () { return { getPropertyValue: function () { return ''; } }; },
	fetch: function (url, opts) {
		fetchCalls++;
		window._lastFetch = { url: url, opts: opts };
		return Promise.resolve({ json: function () { return Promise.resolve(nextEnvelope); } });
	}
};

const sandbox = {
	window: window,
	document: document,
	console: console,
	CustomEvent: CustomEvent,
	setTimeout: function (fn) { if (typeof fn === 'function') fn(); return 0; },
	clearTimeout: function () {},
	setInterval: function () { intervalCalls++; return 0; },
	clearInterval: function () {},
	Promise: Promise,
	fetch: function () { return window.fetch.apply(window, arguments); }
};

if (rendersJsPath)
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const VNS = window.JuneauViews;
const V = VNS && VNS.init;
const CNS = window.JuneauChrome;
const C = CNS && CNS.init;

const out = {
	hasViews: !!(V && typeof V.buildDetailStrip === 'function'),
	hasChrome: !!(C && typeof C.initAll === 'function'),
	hasRelocate: !!(V && typeof V.relocateDetailBarSlot === 'function'),
	hasMint: !!(V && typeof V.mintDetailBarSlotIdentity === 'function'),
	hasTeardown: !!(V && typeof V.teardownDetailBarSlot === 'function'),
	hasEnhance: !!(V && typeof V.enhanceChromeInPanel === 'function')
};
if (!(out.hasViews && out.hasChrome && out.hasRelocate && out.hasMint && out.hasTeardown && out.hasEnhance)) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const BAR_MARKER = 'data-juneau-bar-slot';
const BAR_ANCHOR = 'data-juneau-bar-slot-anchor';
const BAR_META = 'data-juneau-bar-meta';
const SIDECAR_PREFIX = 'juneau-bar:';
const AUTHOR_ID = 'detail-ctx';

// ------------------------------------------------------------------------------------------------------------------
// Fixture builders: hand-painted equivalents of what ViewTable.emitDetailTemplate clones into a child row.
// ------------------------------------------------------------------------------------------------------------------

function barRegion(anchor) {
	var region = el('div');
	region.setAttribute('class', 'jc-bar-slot juneau-view-detail-bar-slot');
	region.setAttribute(BAR_MARKER, AUTHOR_ID);
	region.setAttribute(BAR_ANCHOR, anchor);
	region.setAttribute('data-juneau-refresh', '/chrome/bar-counts');
	var widget = el('span');
	widget.setAttribute('class', 'jc-bar-badge');
	widget.setAttribute('data-juneau-bar-widget', 'open');
	var badge = el('span');
	badge.setAttribute('class', 'jc-badge');
	badge.setAttribute('data-juneau-badge', 'bar:open');
	badge.textContent = '0';
	widget.appendChild(badge);
	region.appendChild(widget);
	return region;
}

function barSidecar(count) {
	var s = el('script');
	s.setAttribute('type', 'application/json');
	s.setAttribute(BAR_META, AUTHOR_ID);
	s.textContent = JSON.stringify({ contractVersion: '1', badges: { 'bar:open': count } });
	return s;
}

function detailSection(id, title) {
	var sec = el('section');
	sec.setAttribute('class', 'juneau-view-detail-section');
	sec.setAttribute('data-juneau-detail-section', id);
	var h2 = el('h2');
	h2.setAttribute('class', 'juneau-view-detail-section-title');
	h2.textContent = title;
	sec.appendChild(h2);
	return sec;
}

function fieldsGrid() {
	var d = el('div');
	d.setAttribute('class', 'juneau-view-detail-fields');
	return d;
}

/** A 2-section panel: the region is the panel's LAST direct child, exactly as the ribbon-anchored emit paints it. */
function twoSectionPanel(withHeader, count) {
	var panel = el('div');
	panel.setAttribute('class', 'juneau-view-detail-panel');
	if (withHeader) {
		var hdr = el('div');
		hdr.setAttribute('class', 'juneau-view-detail-header');
		hdr.setAttribute('data-juneau-detail-header', '1');
		panel.appendChild(hdr);
	}
	['overview', 'detail'].forEach(function (id, i) {
		var sec = detailSection(id, i === 0 ? 'Overview' : 'Detail');
		sec.appendChild(fieldsGrid());
		panel.appendChild(sec);
	});
	panel.appendChild(barRegion('ribbon'));
	panel.appendChild(barSidecar(count == null ? 3 : count));
	return panel;
}

/** A 1-section panel: no ribbon exists, so the region sits INSIDE the lone section right after its title. */
function oneSectionPanel() {
	var panel = el('div');
	panel.setAttribute('class', 'juneau-view-detail-panel');
	var sec = detailSection('overview', 'Overview');
	sec.appendChild(barRegion('section-title'));
	sec.appendChild(fieldsGrid());
	panel.appendChild(sec);
	panel.appendChild(barSidecar(3));
	return panel;
}

function indexOfChild(parent, node) {
	return parent.childNodes.indexOf(node);
}

function regionsIn(root) {
	return root.querySelectorAll('[' + BAR_MARKER + ']');
}

// ------------------------------------------------------------------------------------------------------------------
// 1. Two sections: the relocate step moves the region to the ribbon's trailing position, exactly once.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	var panel = twoSectionPanel(true);
	var regionBefore = regionsIn(panel)[0];
	out.two_regionStartsLast = indexOfChild(panel, regionBefore) === panel.childNodes.length - 2;

	var strip = V.buildDetailStrip(panel, null);
	out.two_stripBuilt = !!strip;
	out.two_stripMode = strip && strip.getAttribute('data-juneau-strip-mode');

	var region = regionsIn(panel)[0];
	out.two_regionCount = regionsIn(panel).length;
	out.two_regionTrailsStrip = strip ? strip.nextSibling === region : false;
	out.two_stripIndex = strip ? indexOfChild(panel, strip) : -1;
	out.two_regionIndex = indexOfChild(panel, region);
	out.two_sameNode = region === regionBefore;              // moved, never re-created (no orphan, no duplicate)
	out.two_regionStillInPanel = panel.contains(region);
	out.two_stripTrailedMarker = strip && strip.getAttribute('data-juneau-strip-trailed');

	// Idempotent: a second relocate against the same strip must not move or duplicate anything.
	var moved = V.relocateDetailBarSlot(panel, strip);
	out.two_secondRelocateMoved = moved;
	out.two_regionCountAfterSecond = regionsIn(panel).length;
	out.two_regionIndexAfterSecond = indexOfChild(panel, region);
	out.two_regionTrailsStripAfterSecond = strip.nextSibling === region;

	// Idempotent across a re-render: a second panel (a fresh clone) relocates its OWN region and leaves the first alone.
	var panel2 = twoSectionPanel(true);
	var strip2 = V.buildDetailStrip(panel2, null);
	out.two_rerenderRegionCount = regionsIn(panel2).length;
	out.two_rerenderTrailsStrip = strip2.nextSibling === regionsIn(panel2)[0];
	out.two_firstPanelRegionCount = regionsIn(panel).length;
	out.two_firstPanelStillTrails = strip.nextSibling === region;
})();

// A header-less 2-section panel: the strip is prepended, and the region must still end up trailing it.
(function () {
	var panel = twoSectionPanel(false);
	var strip = V.buildDetailStrip(panel, null);
	out.twoNoHeader_stripIsFirst = indexOfChild(panel, strip) === 0;
	out.twoNoHeader_regionTrailsStrip = strip.nextSibling === regionsIn(panel)[0];
})();

// A 2-section panel with NO bar slot at all: relocate is a clean no-op, and nothing is synthesized.
(function () {
	var panel = twoSectionPanel(true);
	panel.removeChild(regionsIn(panel)[0]);
	var strip = V.buildDetailStrip(panel, null);
	out.twoNoSlot_stripBuilt = !!strip;
	out.twoNoSlot_regionCount = regionsIn(panel).length;
	out.twoNoSlot_relocateMoved = V.relocateDetailBarSlot(panel, strip);
	out.twoNoSlot_stripUnmarked = strip.getAttribute('data-juneau-strip-trailed') === null;
})();

// ------------------------------------------------------------------------------------------------------------------
// 2. One section: no ribbon is synthesized and the region stays at its section-title anchor.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	var panel = oneSectionPanel();
	var sec = panel.querySelector('[data-juneau-detail-section]');
	var region = regionsIn(panel)[0];
	var strip = V.buildDetailStrip(panel, null);

	out.one_stripIsNull = strip === null;
	out.one_noRibbonSynthesized = panel.querySelector('[data-juneau-strip-mode]') === null;
	out.one_noTablist = panel.querySelector('.juneau-view-detail-tabs') === null;
	out.one_regionCount = regionsIn(panel).length;
	out.one_regionAnchor = region.getAttribute(BAR_ANCHOR);
	out.one_regionParentIsSection = region.parentNode === sec;
	// The 1-section contract: immediate next sibling of h2.juneau-view-detail-section-title.
	var title = sec.querySelector('.juneau-view-detail-section-title');
	out.one_regionFollowsTitle = title.nextSibling === region;
	out.one_anchorSelectorResolves =
		panel.querySelector('[' + BAR_ANCHOR + '="section-title"]') === region;

	// Relocating with a null strip must leave the region exactly where the server put it.
	out.one_relocateMoved = V.relocateDetailBarSlot(panel, null);
	out.one_regionStillFollowsTitle = title.nextSibling === region;
})();

// ------------------------------------------------------------------------------------------------------------------
// 3. Clone-time id minting: two rows expanded at once, each with its own getElementById target.
// ------------------------------------------------------------------------------------------------------------------

const PARENT_ID = 'juneau-view-alerts-1';         // the MINTED table.id, not the author ViewDef.id
const panelA = twoSectionPanel(true, 3);
const panelB = twoSectionPanel(true, 7);
let stripA = null;
let stripB = null;

(function () {
	body.appendChild(panelA);
	body.appendChild(panelB);
	stripA = V.buildDetailStrip(panelA, null);
	stripB = V.buildDetailStrip(panelB, null);

	out.mint_suffixA = V.mintDetailBarSlotIdentity(panelA, PARENT_ID, 'a1');
	out.mint_suffixB = V.mintDetailBarSlotIdentity(panelB, PARENT_ID, 'b2');

	var regionA = regionsIn(panelA)[0];
	var regionB = regionsIn(panelB)[0];
	out.mint_markerA = regionA.getAttribute(BAR_MARKER);
	out.mint_markerB = regionB.getAttribute(BAR_MARKER);
	out.mint_sidecarIdA = panelA.querySelector('[' + BAR_META + ']').getAttribute('id');
	out.mint_sidecarIdB = panelB.querySelector('[' + BAR_META + ']').getAttribute('id');

	// The marker is SUFFIX-ONLY (so readSidecar's own prefix is not doubled); the sidecar id carries the prefix.
	out.mint_markerHasNoPrefix = out.mint_markerA.indexOf(SIDECAR_PREFIX) < 0;
	out.mint_sidecarIdHasPrefix = out.mint_sidecarIdA.indexOf(SIDECAR_PREFIX) === 0;
	out.mint_sidecarIdIsPrefixPlusMarker = out.mint_sidecarIdA === SIDECAR_PREFIX + out.mint_markerA;
	out.mint_markerUsesMintedParentId = out.mint_markerA === PARENT_ID + ':a1';
	out.mint_distinct = out.mint_markerA !== out.mint_markerB;

	// Each minted id round-trips through the shipped readSidecar (prefix + marker), and the AUTHOR id no longer does.
	var rtA = C.readSidecar(SIDECAR_PREFIX, out.mint_markerA);
	var rtB = C.readSidecar(SIDECAR_PREFIX, out.mint_markerB);
	out.mint_roundTripA = !!(rtA && rtA.badges && rtA.badges['bar:open'] === 3);
	out.mint_roundTripB = !!(rtB && rtB.badges && rtB.badges['bar:open'] === 7);
	out.mint_authorIdUnresolvable = document.getElementById(SIDECAR_PREFIX + AUTHOR_ID) === null;
	out.mint_authorSidecarUnresolvable = C.readSidecar(SIDECAR_PREFIX, AUTHOR_ID) === null;
})();

// ------------------------------------------------------------------------------------------------------------------
// 4. Enhance-on-insert: initAll() after insertion enhances BOTH cloned slots, idempotently, and does not double-bind
//    the page header's SAFE actions.
// ------------------------------------------------------------------------------------------------------------------

let safeAction = null;
let safeFires = 0;

(function () {
	// A page header with one SAFE action: the control an expand-time initAll() must not re-bind.
	var header = el('div');
	header.setAttribute('class', 'jc-app-header');
	header.setAttribute('data-juneau-app-header', 'main');
	safeAction = el('button');
	safeAction.setAttribute('data-juneau-header-action', 'ping');
	safeAction.setAttribute('data-juneau-behavior', 'safe');
	safeAction.setAttribute('data-juneau-safe', 'ping');
	header.appendChild(safeAction);
	body.appendChild(header);
	header.addEventListener(C.SAFE_EVENT, function () { safeFires++; });

	var badgeA = panelA.querySelector('[data-juneau-badge]');
	var badgeB = panelB.querySelector('[data-juneau-badge]');
	out.enh_countBeforeA = badgeA.textContent;

	var first = C.initAll();
	out.enh_barsFound = first.bars.length;
	out.enh_headersFound = first.headers.length;
	out.enh_countAfterA = badgeA.textContent;
	out.enh_countAfterB = badgeB.textContent;
	out.enh_safeListenersAfterFirst = safeAction.listenerCount('click');
	out.enh_wiredMarker = safeAction.getAttribute('data-juneau-safe-wired');

	// Idempotent: a second (expand-time) initAll re-finds the same clones and must not re-bind the SAFE action.
	var second = C.initAll();
	out.enh_barsFoundSecond = second.bars.length;
	out.enh_safeListenersAfterSecond = safeAction.listenerCount('click');
	out.enh_countStillA = badgeA.textContent;

	safeAction.dispatchEvent(new CustomEvent('click', { bubbles: true }));
	out.enh_safeFires = safeFires;

	// And the views-side seam actually calls it: enhanceChromeInPanel on a panel holding a slot returns true.
	var panelC = twoSectionPanel(true, 5);
	body.appendChild(panelC);
	V.buildDetailStrip(panelC, null);
	V.mintDetailBarSlotIdentity(panelC, PARENT_ID, 'c3');
	var badgeC = panelC.querySelector('[data-juneau-badge]');
	out.enh_seamBefore = badgeC.textContent;
	out.enh_seamCalled = V.enhanceChromeInPanel(panelC);
	out.enh_seamAfter = badgeC.textContent;
	out.enh_seamSafeListeners = safeAction.listenerCount('click');
	// A panel with no bar slot must not drag chrome in at all.
	var plain = twoSectionPanel(true);
	plain.removeChild(regionsIn(plain)[0]);
	out.enh_seamSkippedWithoutSlot = V.enhanceChromeInPanel(plain);
})();

// ------------------------------------------------------------------------------------------------------------------
// 5. Demand refresh fetches exactly once; no interval timer is ever created.
// ------------------------------------------------------------------------------------------------------------------

(async function () {
	var regionA = regionsIn(panelA)[0];
	nextEnvelope = { contractVersion: '1', badges: { 'bar:open': 11 } };
	fetchCalls = 0;
	out.refresh_result = await C.refresh(regionA);
	out.refresh_fetchCalls = fetchCalls;
	out.refresh_url = window._lastFetch && window._lastFetch.url;
	out.refresh_countAfter = panelA.querySelector('[data-juneau-badge]').textContent;
	out.refresh_otherRowUntouched = panelB.querySelector('[data-juneau-badge]').textContent;

	// ---- Teardown on collapse: the minted sidecar id is dropped, the sibling row keeps its own ----
	V.teardownDetailBarSlot(panelA);
	out.tear_sidecarIdRemoved = panelA.querySelector('[' + BAR_META + ']').getAttribute('id') === null;
	out.tear_aUnresolvable = document.getElementById(SIDECAR_PREFIX + PARENT_ID + ':a1') === null;
	out.tear_bStillResolvable = document.getElementById(SIDECAR_PREFIX + PARENT_ID + ':b2') !== null;
	out.tear_bStillRoundTrips = !!C.readSidecar(SIDECAR_PREFIX, PARENT_ID + ':b2');
	body.removeChild(panelA);
	out.tear_bRegionStillPresent = regionsIn(panelB).length;
	out.tear_noThrowOnNull = (function () { V.teardownDetailBarSlot(null); return true; })();

	// ---- No poller anywhere on this host ----
	out.intervalCalls = intervalCalls;

	// ---- Contract handshake regression: an existing expand GET still handshakes ----
	out.rowDetailContract = V.JUNEAU_ROW_DETAIL_CONTRACT_VERSION;
	out.barContract = C.JUNEAU_BAR_CONTRACT_VERSION;
	out.handshakeOk = V.detailContractOk({ contractVersion: '1' }, '1');
	out.handshakeRejectsOther = V.detailContractOk({ contractVersion: '2' }, '1');

	process.stdout.write(JSON.stringify(out, null, 2) + '\n');
})().catch(function (e) {
	process.stderr.write(String((e && e.stack) || e) + '\n');
	process.exit(1);
});
