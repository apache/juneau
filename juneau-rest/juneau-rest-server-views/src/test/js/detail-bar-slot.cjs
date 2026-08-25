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
	// Tag-name prefix only; the remainder is sliced off by length rather than captured by a second `(.*)$` group,
	// which avoided a super-linear-backtracking shape (the `[\w-]*` and `.*` character classes fully overlap).
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

/** Walks from `node` up through parentNode, matching `sel` at each step. Takes the start node as a parameter
 * (rather than aliasing `this` to a local) so the caller passes `this` in as an argument instead. */
function closestFrom(node, sel) {
	let n = node;
	while (n && n.nodeType === 1) {
		if (elMatches(n, sel)) return n;
		n = n.parentNode;
	}
	return null;
}

/** Dispatches `ev` on `node`, then bubbles to parentNode when ev.bubbles is set. Takes the start node as a
 * parameter for the same reason as closestFrom above. */
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
	setTimeout: function (fn) { if (typeof fn === 'function') { fn(); } return 0; },
	clearTimeout: function () {},
	setInterval: function () { intervalCalls++; return 0; },
	clearInterval: function () {},
	Promise: Promise,
	fetch: function (...args) { return window.fetch(...args); }
};

// NOSONAR javascript:S1523 -- this harness's entire purpose is to load the production runtime under test (a
// repo-local file path from argv, not attacker-controlled input) into an isolated VM sandbox; that IS the test.
if (rendersJsPath)
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
// NOSONAR javascript:S1523 -- same rationale: loading the production juneau-views.js under test into the sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
// NOSONAR javascript:S1523 -- same rationale: loading the production juneau-chrome.js under test into the sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(chromeJsPath), 'utf8'), sandbox, { filename: 'juneau-chrome.js' });

const VNS = window.JuneauViews;
const V = VNS?.init;
const CNS = window.JuneauChrome;
const C = CNS?.init;

const out = {
	hasViews: !!(typeof V?.buildDetailStrip === 'function'),
	hasChrome: !!(typeof C?.initAll === 'function'),
	hasRelocate: !!(typeof V?.relocateDetailBarSlot === 'function'),
	hasMint: !!(typeof V?.mintDetailBarSlotIdentity === 'function'),
	hasTeardown: !!(typeof V?.teardownDetailBarSlot === 'function'),
	hasEnhance: !!(typeof V?.enhanceChromeInPanel === 'function')
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
	const region = el('div');
	region.setAttribute('class', 'jc-bar-slot juneau-view-detail-bar-slot');
	region.setAttribute(BAR_MARKER, AUTHOR_ID);
	region.setAttribute(BAR_ANCHOR, anchor);
	region.dataset.juneauRefresh = '/chrome/bar-counts';
	const widget = el('span');
	widget.setAttribute('class', 'jc-bar-badge');
	widget.dataset.juneauBarWidget = 'open';
	const badge = el('span');
	badge.setAttribute('class', 'jc-badge');
	badge.dataset.juneauBadge = 'bar:open';
	badge.textContent = '0';
	widget.appendChild(badge);
	region.appendChild(widget);
	return region;
}

function barSidecar(count) {
	const s = el('script');
	s.setAttribute('type', 'application/json');
	s.setAttribute(BAR_META, AUTHOR_ID);
	s.textContent = JSON.stringify({ contractVersion: '1', badges: { 'bar:open': count } });
	return s;
}

function detailSection(id, title) {
	const sec = el('section');
	sec.setAttribute('class', 'juneau-view-detail-section');
	sec.dataset.juneauDetailSection = id;
	const h2 = el('h2');
	h2.setAttribute('class', 'juneau-view-detail-section-title');
	h2.textContent = title;
	sec.appendChild(h2);
	return sec;
}

function fieldsGrid() {
	const d = el('div');
	d.setAttribute('class', 'juneau-view-detail-fields');
	return d;
}

/** A 2-section panel: the region is the panel's LAST direct child, exactly as the ribbon-anchored emit paints it. */
function twoSectionPanel(withHeader, count) {
	const panel = el('div');
	panel.setAttribute('class', 'juneau-view-detail-panel');
	if (withHeader) {
		const hdr = el('div');
		hdr.setAttribute('class', 'juneau-view-detail-header');
		hdr.dataset.juneauDetailHeader = '1';
		panel.appendChild(hdr);
	}
	['overview', 'detail'].forEach(function (id, i) {
		const sec = detailSection(id, i === 0 ? 'Overview' : 'Detail');
		sec.appendChild(fieldsGrid());
		panel.appendChild(sec);
	});
	panel.appendChild(barRegion('ribbon'));
	panel.appendChild(barSidecar(count == null ? 3 : count));
	return panel;
}

/** A 1-section panel: no ribbon exists, so the region sits INSIDE the lone section right after its title. */
function oneSectionPanel() {
	const panel = el('div');
	panel.setAttribute('class', 'juneau-view-detail-panel');
	const sec = detailSection('overview', 'Overview');
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
	const panel = twoSectionPanel(true);
	const regionBefore = regionsIn(panel)[0];
	out.two_regionStartsLast = indexOfChild(panel, regionBefore) === panel.childNodes.length - 2;

	const strip = V.buildDetailStrip(panel, null);
	out.two_stripBuilt = !!strip;
	out.two_stripMode = strip?.dataset.juneauStripMode;

	const region = regionsIn(panel)[0];
	out.two_regionCount = regionsIn(panel).length;
	out.two_regionTrailsStrip = strip ? strip.nextSibling === region : false;
	out.two_stripIndex = strip ? indexOfChild(panel, strip) : -1;
	out.two_regionIndex = indexOfChild(panel, region);
	out.two_sameNode = region === regionBefore;              // moved, never re-created (no orphan, no duplicate)
	out.two_regionStillInPanel = panel.contains(region);
	out.two_stripTrailedMarker = strip?.dataset.juneauStripTrailed;

	// Idempotent: a second relocate against the same strip must not move or duplicate anything.
	const moved = V.relocateDetailBarSlot(panel, strip);
	out.two_secondRelocateMoved = moved;
	out.two_regionCountAfterSecond = regionsIn(panel).length;
	out.two_regionIndexAfterSecond = indexOfChild(panel, region);
	out.two_regionTrailsStripAfterSecond = strip.nextSibling === region;

	// Idempotent across a re-render: a second panel (a fresh clone) relocates its OWN region and leaves the first alone.
	const panel2 = twoSectionPanel(true);
	const strip2 = V.buildDetailStrip(panel2, null);
	out.two_rerenderRegionCount = regionsIn(panel2).length;
	out.two_rerenderTrailsStrip = strip2.nextSibling === regionsIn(panel2)[0];
	out.two_firstPanelRegionCount = regionsIn(panel).length;
	out.two_firstPanelStillTrails = strip.nextSibling === region;
})();

// A header-less 2-section panel: the strip is prepended, and the region must still end up trailing it.
(function () {
	const panel = twoSectionPanel(false);
	const strip = V.buildDetailStrip(panel, null);
	out.twoNoHeader_stripIsFirst = indexOfChild(panel, strip) === 0;
	out.twoNoHeader_regionTrailsStrip = strip.nextSibling === regionsIn(panel)[0];
})();

// A 2-section panel with NO bar slot at all: relocate is a clean no-op, and nothing is synthesized.
(function () {
	const panel = twoSectionPanel(true);
	regionsIn(panel)[0].remove();
	const strip = V.buildDetailStrip(panel, null);
	out.twoNoSlot_stripBuilt = !!strip;
	out.twoNoSlot_regionCount = regionsIn(panel).length;
	out.twoNoSlot_relocateMoved = V.relocateDetailBarSlot(panel, strip);
	out.twoNoSlot_stripUnmarked = strip.dataset.juneauStripTrailed === undefined;
})();

// ------------------------------------------------------------------------------------------------------------------
// 2. One section: no ribbon is synthesized and the region stays at its section-title anchor.
// ------------------------------------------------------------------------------------------------------------------

(function () {
	const panel = oneSectionPanel();
	const sec = panel.querySelector('[data-juneau-detail-section]');
	const region = regionsIn(panel)[0];
	const strip = V.buildDetailStrip(panel, null);

	out.one_stripIsNull = strip === null;
	out.one_noRibbonSynthesized = panel.querySelector('[data-juneau-strip-mode]') === null;
	out.one_noTablist = panel.querySelector('.juneau-view-detail-tabs') === null;
	out.one_regionCount = regionsIn(panel).length;
	out.one_regionAnchor = region.getAttribute(BAR_ANCHOR);
	out.one_regionParentIsSection = region.parentNode === sec;
	// The 1-section contract: immediate next sibling of h2.juneau-view-detail-section-title.
	const title = sec.querySelector('.juneau-view-detail-section-title');
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

	const regionA = regionsIn(panelA)[0];
	const regionB = regionsIn(panelB)[0];
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
	const rtA = C.readSidecar(SIDECAR_PREFIX, out.mint_markerA);
	const rtB = C.readSidecar(SIDECAR_PREFIX, out.mint_markerB);
	out.mint_roundTripA = !!(rtA?.badges?.['bar:open'] === 3);
	out.mint_roundTripB = !!(rtB?.badges?.['bar:open'] === 7);
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

	const badgeA = panelA.querySelector('[data-juneau-badge]');
	const badgeB = panelB.querySelector('[data-juneau-badge]');
	out.enh_countBeforeA = badgeA.textContent;

	const first = C.initAll();
	out.enh_barsFound = first.bars.length;
	out.enh_headersFound = first.headers.length;
	out.enh_countAfterA = badgeA.textContent;
	out.enh_countAfterB = badgeB.textContent;
	out.enh_safeListenersAfterFirst = safeAction.listenerCount('click');
	out.enh_wiredMarker = safeAction.dataset.juneauSafeWired;

	// Idempotent: a second (expand-time) initAll re-finds the same clones and must not re-bind the SAFE action.
	const second = C.initAll();
	out.enh_barsFoundSecond = second.bars.length;
	out.enh_safeListenersAfterSecond = safeAction.listenerCount('click');
	out.enh_countStillA = badgeA.textContent;

	safeAction.dispatchEvent(new CustomEvent('click', { bubbles: true }));
	out.enh_safeFires = safeFires;

	// And the views-side seam actually calls it: enhanceChromeInPanel on a panel holding a slot returns true.
	const panelC = twoSectionPanel(true, 5);
	body.appendChild(panelC);
	V.buildDetailStrip(panelC, null);
	V.mintDetailBarSlotIdentity(panelC, PARENT_ID, 'c3');
	const badgeC = panelC.querySelector('[data-juneau-badge]');
	out.enh_seamBefore = badgeC.textContent;
	out.enh_seamCalled = V.enhanceChromeInPanel(panelC);
	out.enh_seamAfter = badgeC.textContent;
	out.enh_seamSafeListeners = safeAction.listenerCount('click');
	// A panel with no bar slot must not drag chrome in at all.
	const plain = twoSectionPanel(true);
	regionsIn(plain)[0].remove();
	out.enh_seamSkippedWithoutSlot = V.enhanceChromeInPanel(plain);
})();

// ------------------------------------------------------------------------------------------------------------------
// 5. Demand refresh fetches exactly once; no interval timer is ever created.
// ------------------------------------------------------------------------------------------------------------------

(async function () {
	const regionA = regionsIn(panelA)[0];
	nextEnvelope = { contractVersion: '1', badges: { 'bar:open': 11 } };
	fetchCalls = 0;
	out.refresh_result = await C.refresh(regionA);
	out.refresh_fetchCalls = fetchCalls;
	out.refresh_url = window._lastFetch?.url;
	out.refresh_countAfter = panelA.querySelector('[data-juneau-badge]').textContent;
	out.refresh_otherRowUntouched = panelB.querySelector('[data-juneau-badge]').textContent;

	// ---- Teardown on collapse: the minted sidecar id is dropped, the sibling row keeps its own ----
	V.teardownDetailBarSlot(panelA);
	out.tear_sidecarIdRemoved = panelA.querySelector('[' + BAR_META + ']').getAttribute('id') === null;
	out.tear_aUnresolvable = document.getElementById(SIDECAR_PREFIX + PARENT_ID + ':a1') === null;
	out.tear_bStillResolvable = document.getElementById(SIDECAR_PREFIX + PARENT_ID + ':b2') !== null;
	out.tear_bStillRoundTrips = !!C.readSidecar(SIDECAR_PREFIX, PARENT_ID + ':b2');
	panelA.remove();
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
