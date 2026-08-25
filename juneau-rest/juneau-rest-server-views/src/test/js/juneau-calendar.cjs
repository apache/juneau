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
 * juneau-calendar.cjs - always-on Node harness for the juneau-calendar.js runtime:
 * the DOM-free PURE LOGIC LAYER (grid math, civil-date bucketing, contract/echo checks, sanitize, color, url
 * safety, cap) AND the DOM BINDING LAYER (contract handshake fail-loud, textContent-only fill, echo-check,
 * coalesce+abort, single-attempt fetch error UI).
 *
 *   Usage:  node juneau-calendar.cjs <path-to-juneau-calendar.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const calendarJsPath = process.argv[2];
if (!calendarJsPath) {
	console.error('usage: node juneau-calendar.cjs <juneau-calendar.js>');
	process.exit(2);
}

// ------------------------------------------------------------------------------------------------------------------
// Minimal DOM mock (mirrors row-detail.cjs): elements, text nodes, a subset selector matcher and <template>.content.
// ------------------------------------------------------------------------------------------------------------------

function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	// tag[attr] / tag[attr="v"]
	let m = /^([a-zA-Z][\w-]*)?(?:\[([\w:-]+)(?:="([^"]*)")?\])?$/.exec(sel);
	if (m && (m[1] || m[2])) {
		if (m[1] && node.tagName !== m[1].toUpperCase()) return false;
		if (m[2]) {
			const v = node.getAttribute(m[2]);
			if (m[3] == null ? v == null : v !== m[3]) return false;
		}
		return true;
	}
	if (sel.charAt(0) === '.') {
		const cls = sel.slice(1);
		const raw = ' ' + (node.getAttribute('class') || '') + ' ';
		return raw.indexOf(' ' + cls + ' ') >= 0;
	}
	return false;
}

function elWalk(node, sel, acc) {
	for (let i = 0; i < node.childNodes.length; i++) {
		const c = node.childNodes[i];
		if (c.nodeType === 1) {
			if (elMatches(c, sel)) acc.push(c);
			elWalk(c, sel, acc);
		}
	}
	return acc;
}

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		parentNode: null,
		_listeners: {},
		__juneauCalendarInit: false,
		get firstElementChild() {
			for (let i = 0; i < this.childNodes.length; i++)
				if (this.childNodes[i].nodeType === 1) return this.childNodes[i];
			return null;
		},
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		hasAttribute: function (k) { return Object.hasOwn(this.attrs, k); },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		insertBefore: function (c, ref) {
			const i = ref ? this.childNodes.indexOf(ref) : -1;
			if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) {
			const i = this.childNodes.indexOf(c);
			if (i >= 0) this.childNodes.splice(i, 1);
			return c;
		},
		cloneNode: function () {
			const copy = el(this.tagName);
			for (const k in this.attrs) copy.attrs[k] = this.attrs[k];
			copy._text = this._text;
			for (let i = 0; i < this.childNodes.length; i++) {
				const c = this.childNodes[i];
				copy.appendChild(c.nodeType === 1 ? c.cloneNode(true) : textNode(c.nodeValue));
			}
			return copy;
		},
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		contains: function (other) {
			if (other === this) return true;
			for (let i = 0; i < this.childNodes.length; i++) {
				const c = this.childNodes[i];
				if (c === other) return true;
				if (c.nodeType === 1 && c.contains(other)) return true;
			}
			return false;
		},
		addEventListener: function (type, fn) { (this._listeners[type] = this._listeners[type] || []).push(fn); },
		focus: function () { document.activeElement = this; },
		style: { _p: {}, setProperty: function (k, v) { this._p[k] = v; }, getPropertyValue: function (k) { return this._p[k]; } },
		_fire: function (type, ev) {
			ev = ev || {};
			if (ev.currentTarget == null) ev.currentTarget = this;
			(this._listeners[type] || []).forEach(function (fn) { fn(ev); });
		},
		set textContent(v) { this.childNodes.length = 0; this._text = v == null ? '' : String(v); },
		get textContent() {
			if (this.childNodes.length === 0) return this._text || '';
			return this.childNodes.map(function (c) { return c.textContent; }).join('');
		}
	};
	node._text = '';
	// A <template> exposes .content (a document fragment); our fragment is just an element holder.
	if (node.tagName === 'TEMPLATE') node.content = el('#fragment');
	return node;
}

function textNode(value) {
	return { nodeType: 3, nodeValue: value == null ? '' : String(value), childNodes: [],
		get textContent() { return this.nodeValue; } };
}

const VOID_TAGS = { br: 1, hr: 1, img: 1, input: 1, meta: 1, link: 1, base: 1 };

function parseAttrs(raw, node) {
	if (!raw) return;
	const re = /([:@\w-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g;
	let m;
	while ((m = re.exec(raw)))
		node.setAttribute(m[1], m[2] != null ? m[2] : (m[3] != null ? m[3] : (m[4] != null ? m[4] : '')));
}

// Parse test HTML into a tree.  A <template>'s children are attached to its .content fragment (as browsers do).
function parseTestHtml(html) {
	const root = el('div');
	const stack = [root];
	const re = /<\/?([a-zA-Z][a-zA-Z0-9]*)\b([^>]*?)(\/?)>|([^<]+)/g;
	let m;
	while ((m = re.exec(html))) {
		if (m[4] != null) {
			const t = m[4];
			if (t.trim().length) stack[stack.length - 1].appendChild(textNode(t));
			continue;
		}
		const name = m[1];
		const closing = html.charAt(m.index + 1) === '/';
		if (closing) { if (stack.length > 1) stack.pop(); continue; }
		const node = el(name);
		parseAttrs(m[2], node);
		const parent = stack[stack.length - 1];
		// <template> children live in its .content fragment, not as direct childNodes.
		(parent.tagName === 'TEMPLATE' ? parent.content : parent).appendChild(node);
		const selfClosing = VOID_TAGS[name.toLowerCase()] || m[3] === '/';
		if (!selfClosing) stack.push(node);
	}
	return root;
}

const documentListeners = {};
const document = {
	readyState: 'complete',
	activeElement: null,
	addEventListener: function (t, fn) { (documentListeners[t] = documentListeners[t] || []).push(fn); },
	removeEventListener: function (t, fn) {
		const a = documentListeners[t]; if (!a) return;
		const i = a.indexOf(fn); if (i >= 0) a.splice(i, 1);
	},
	_fire: function (t, ev) { (documentListeners[t] || []).slice().forEach(function (fn) { fn(ev || {}); }); },
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	createElement: function (tag) { return el(tag); },
	createTextNode: function (v) { return textNode(v); },
	contains: function (n) { return !!n; },
	body: el('body')
};

// ------------------------------------------------------------------------------------------------------------------
// A minimal stand-in for the ONE shared layer stack juneau-views.js owns (window.JuneauViews.init).  It reproduces
// only the contract juneau-calendar.js depends on: portal to body, Escape pops the top layer, an outside pointerdown
// pops a light-dismiss top layer, detach + onDismiss on pop, and focus restore to returnFocusTo.
// ------------------------------------------------------------------------------------------------------------------

const layerStack = [];
function installViewsLayerStack() {
	window.JuneauViews = { init: {
		pushLayer: function (node, opts) {
			opts = opts || {};
			const rec = { el: node, kind: opts.kind || 'layer', lightDismiss: !!opts.lightDismiss,
				onDismiss: typeof opts.onDismiss === 'function' ? opts.onDismiss : null,
				returnFocusTo: opts.returnFocusTo || document.activeElement };
			if (node.parentNode !== document.body) document.body.appendChild(node);
			layerStack.push(rec);
			node.setAttribute('data-juneau-layer', String(layerStack.length - 1));
			return rec;
		},
		popLayer: function (node) {
			if (!layerStack.length) return;
			let from = layerStack.length - 1;
			if (node) {
				let idx = -1;
				for (let i = layerStack.length - 1; i >= 0; i--) if (layerStack[i].el === node) { idx = i; break; }
				if (idx < 0) return;
				from = idx;
			}
			const removed = layerStack.splice(from);
			const restore = removed.length ? removed[0].returnFocusTo : null;
			for (let i = removed.length - 1; i >= 0; i--) {
				const rec = removed[i];
				if (rec.el && rec.el.parentNode) rec.el.parentNode.removeChild(rec.el);
				if (rec.onDismiss) rec.onDismiss();
			}
			if (restore && typeof restore.focus === 'function') restore.focus();
		},
		topLayer: function () { return layerStack.length ? layerStack[layerStack.length - 1] : null; }
	} };
	// The ONE document keydown the shared stack installs: Escape pops the top layer.
	document.addEventListener('keydown', function (e) {
		if (layerStack.length && e.key === 'Escape') window.JuneauViews.init.popLayer();
	});
}
function uninstallViewsLayerStack() {
	delete window.JuneauViews;
}

// Deferred fetch controller so the harness can settle promises on demand (coalesce/abort/echo tests).
let pendingFetches = [];
let abortCount = 0;
function AbortController() {
	this.signal = { aborted: false, addEventListener: function () {} };
	const self = this;
	this.abort = function () { self.signal.aborted = true; abortCount++; };
}
function makeFetch(responder) {
	return function (url, opts) {
		let resolve, reject;
		const p = new Promise(function (res, rej) { resolve = res; reject = rej; });
		pendingFetches.push({ url: url, opts: opts, resolve: resolve, reject: reject, responder: responder });
		return p;
	};
}
function settleFetches() {
	const q = pendingFetches; pendingFetches = [];
	q.forEach(function (f) {
		if (f.opts && f.opts.signal && f.opts.signal.aborted) {
			const e = new Error('aborted'); e.name = 'AbortError'; f.reject(e); return;
		}
		f.responder(f);
	});
}
function jsonResponse(body, contentType) {
	return { ok: true, status: 200,
		headers: { get: function (k) { return k === 'Content-Type' ? (contentType || 'application/json') : null; } },
		json: function () { return Promise.resolve(body); },
		text: function () { return Promise.resolve(JSON.stringify(body)); } };
}

const warnings = [];
const errors = [];
const shimConsole = {
	log: function () {},
	warn: function () { warnings.push(Array.prototype.join.call(arguments, ' ')); },
	error: function () { errors.push(Array.prototype.join.call(arguments, ' ')); }
};

const window = { document: document, console: shimConsole, AbortController: AbortController };
const sandbox = {
	window: window, document: document, console: shimConsole, AbortController: AbortController,
	fetch: undefined, Promise: Promise, setTimeout: setTimeout
};
vm.runInNewContext(fs.readFileSync(path.resolve(calendarJsPath), 'utf8'), sandbox, { filename: 'juneau-calendar.js' });

const NS = window.JuneauCalendar;
const out = { hasNs: !!NS, hasPure: !!(NS && NS.pure) };
if (!out.hasPure) { process.stdout.write(JSON.stringify(out)); process.exit(0); }
const P = NS.pure;

// ------------------------------------------------------------------------------------------------------------------
// PURE LAYER
// ------------------------------------------------------------------------------------------------------------------

out.contractVersion = NS.CONTRACT_VERSION;
out.minPoll = NS.MIN_POLL_INTERVAL_MS;

out.dim_feb2024 = P.daysInMonth(2024, 2);   // leap -> 29
out.dim_feb2026 = P.daysInMonth(2026, 2);   // non-leap -> 28
out.dim_apr = P.daysInMonth(2026, 4);       // 30
out.dim_jan = P.daysInMonth(2026, 1);       // 31

// Aug 1 2026 is a Saturday (dow=6).
out.dow_aug1 = P.dayOfWeek(2026, 8, 1);
out.off_sunday = P.firstWeekdayOffset(2026, 8, 'sunday');   // Sat=6
out.off_monday = P.firstWeekdayOffset(2026, 8, 'monday');   // Sat -> 5

const cells = P.buildMonthCells(2026, 8, 'sunday');
out.cells_count = cells.length;                              // always 42
out.cells_firstInMonth = cells.filter(function (c) { return c.inMonth; })[0].key;
out.cells_leadingAdjacent = cells[0].inMonth === false && cells[5].inMonth === false;
out.cells_firstOfMonthAt6 = cells[6].key === '2026-08-01' && cells[6].inMonth === true;
out.cells_inMonthCount = cells.filter(function (c) { return c.inMonth; }).length;   // 31

const cellsMon = P.buildMonthCells(2026, 8, 'monday');
out.cellsMon_count = cellsMon.length;
out.cellsMon_firstOfMonthAt5 = cellsMon[5].key === '2026-08-01';

out.civil_dateOnly = P.civilKey('2026-08-14');
out.civil_dateTime = P.civilKey('2026-08-14T23:30:00Z');    // leading date only, no Date.parse shift
out.civil_bad = P.civilKey('not-a-date');
out.civil_short = P.civilKey('2026-08');
out.civil_badSep = P.civilKey('2026-08-14X10:00');          // char at 10 not 'T' -> null

out.contract_okStr = P.contractOk('2');
out.contract_badNum = P.contractOk(2);                       // numeric 2 must fail strict ===
out.contract_bad1 = P.contractOk('1');                       // the superseded v1 string must fail too

out.echo_ok = P.echoOk({ year: 2026, month: 8 }, 2026, 8);
out.echo_badMonth = P.echoOk({ year: 2026, month: 9 }, 2026, 8);
out.echo_null = P.echoOk(null, 2026, 8);

const cleaned = P.sanitizeEvents([
	{ id: 'a', title: 'A', start: '2026-08-01' },
	{ id: 'a', title: 'dup', start: '2026-08-02' },
	{ id: 'b', title: 'B', start: '2026-08-03' },
	{ title: 'no id', start: '2026-08-04' },
	{ id: 'c', start: '2026-08-05' }
]);
out.sanitize_ids = cleaned.map(function (e) { return e.id; }).join(',');   // a,b
out.sanitize_warned = warnings.length >= 3;

// ---- `end` split inclusivity, spanning, and the CLOSED malformed set (mirrors CalendarEvent) --------------------

out.last_allDayInclusive = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-04' });
out.last_timedExclusive = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02T09:00', end: '2026-03-02T10:00' });
out.last_timedMidnightCrossing = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02T15:00', end: '2026-03-03T09:00' });
out.last_timedEndsAtMidnight = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02T15:00', end: '2026-03-03T00:00' });
out.last_allDaySameDay = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-02' });
out.last_omittedEnd = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02' });
out.last_offsetIgnored = P.lastDayKey({ id: 'x', title: 'x', start: '2026-03-02T09:00:00Z', end: '2026-03-02T10:00:00Z' });

out.span_allDayThreeDay = P.spanning({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-04' });
out.span_timedHour = P.spanning({ id: 'x', title: 'x', start: '2026-03-02T09:00', end: '2026-03-02T10:00' });
out.span_timedMidnight = P.spanning({ id: 'x', title: 'x', start: '2026-03-02T15:00', end: '2026-03-03T09:00' });
out.span_omittedEnd = P.spanning({ id: 'x', title: 'x', start: '2026-03-02T15:00' });

out.mal_ok = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02' });
out.mal_noId = !!P.malformedReason({ title: 'x', start: '2026-03-02' });
out.mal_noTitle = !!P.malformedReason({ id: 'x', start: '2026-03-02' });
out.mal_noStart = !!P.malformedReason({ id: 'x', title: 'x' });
out.mal_badStart = !!P.malformedReason({ id: 'x', title: 'x', start: 'nope' });
out.mal_badEnd = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02', end: 'nope' });
out.mal_allDayTrueWithTimedEnd = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02', allDay: true, end: '2026-03-02T10:00' });
out.mal_allDayFalseWithDateEnd = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02T09:00', allDay: false, end: '2026-03-04' });
out.mal_mixedShapesNullAllDay = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-04T10:00' });
out.mal_timedZeroDuration = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02T09:00', end: '2026-03-02T09:00' });
out.mal_endBeforeStart = !!P.malformedReason({ id: 'x', title: 'x', start: '2026-03-04', end: '2026-03-02' });
// NOT malformed: omitted end, a null allDay with matching shapes, and a trailing offset/Z.
out.mal_omittedEndOk = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02' }) === null;
out.mal_nullAllDayDateOnlyOk = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-04' }) === null;
out.mal_nullAllDayTimedOk = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02T09:00', end: '2026-03-02T10:00' }) === null;
out.mal_offsetOk = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02T09:00:00Z', end: '2026-03-02T10:00:00Z' }) === null;
out.mal_allDaySameDayOk = P.malformedReason({ id: 'x', title: 'x', start: '2026-03-02', end: '2026-03-02' }) === null;

// One malformed event is DROPPED and the rest of the payload still paints (GET is identical to POST).
const mixedPayload = P.sanitizeEvents([
	{ id: 'good1', title: 'Good', start: '2026-03-02' },
	{ id: 'bad', title: 'Bad', start: '2026-03-02', allDay: true, end: '2026-03-02T10:00' },
	{ id: 'good2', title: 'Also good', start: '2026-03-03T09:00', end: '2026-03-03T10:00' }
]);
out.dropMalformed_ids = mixedPayload.map(function (e) { return e.id; }).join(',');

out.timeLabel_timed = P.startTimeLabel({ id: 'x', title: 'x', start: '2026-03-02T09:05' });
out.timeLabel_allDay = P.startTimeLabel({ id: 'x', title: 'x', start: '2026-03-02' });
out.timeLabel_offset = P.startTimeLabel({ id: 'x', title: 'x', start: '2026-03-02T14:30:00Z' });

const catMap = { team: 'blue', review: 'green' };
out.color_known = P.colorToken('team', catMap);
out.color_unknown = P.colorToken('ghost', catMap);           // neutral + warn
out.color_unknownWarned = warnings.some(function (w) { return w.indexOf('ghost') >= 0; });
out.color_none = P.colorToken(null, catMap);

out.url_path = P.isSafeDocumentUrl('/events/123');
out.url_rel = P.isSafeDocumentUrl('events/123?x=1');
out.url_abs = P.isSafeDocumentUrl('https://evil/x');
out.url_protoRel = P.isSafeDocumentUrl('//evil/x');
out.url_scheme = P.isSafeDocumentUrl('javascript:alert(1)');
out.url_dotdot = P.isSafeDocumentUrl('/a/../b');

out.sub = P.substituteEndpoint('/events/{year}/{month}', 2026, 8);

const dayEvents = P.eventsForDay([
	{ id: '2', title: 'later', start: '2026-08-14T10:00:00Z' },
	{ id: '1', title: 'early', start: '2026-08-14T08:00:00Z' },
	{ id: '3', title: 'other', start: '2026-08-15' }
], '2026-08-14');
out.eventsForDay_ids = dayEvents.map(function (e) { return e.id; }).join(',');   // 1,2 sorted

// Chip ORDER: all-day chips precede timed chips, and timed chips ascend.
const ordered = P.eventsForDay([
	{ id: 't2', title: 'noon', start: '2026-08-14T12:00' },
	{ id: 't1', title: 'morning', start: '2026-08-14T08:00' },
	{ id: 'a1', title: 'all day', start: '2026-08-14' },
	{ id: 'a2', title: 'also all day', start: '2026-08-14', allDay: true }
], '2026-08-14');
out.chipOrder_ids = ordered.map(function (e) { return e.id; }).join(',');   // a1,a2,t1,t2

// A spanning event is NOT a chip in its start cell - it is a bar.
out.chipOrder_spanExcluded = P.eventsForDay([
	{ id: 'sp', title: 'span', start: '2026-08-14', end: '2026-08-16' },
	{ id: 'ch', title: 'chip', start: '2026-08-14' }
], '2026-08-14').map(function (e) { return e.id; }).join(',');

const capped = P.applyCap([1, 2, 3, 4, 5], 3);
out.cap_shown = capped.shown.length;
out.cap_overflow = capped.overflow;

out.laneBudget_default = P.laneBudgetFor(3);
out.laneBudget_capped = P.laneBudgetFor(50);     // hard internal cap of 8

// ---- Spanning-bar segmentation --------------------------------------------------------------------------------
// August 2026, Sunday-start: Aug 1 is a Saturday, so week row 0 is Jul 26..Aug 1 and Aug 2 opens row 1.
// A span Saturday Aug 8 -> Tuesday Aug 11 must cut at the week boundary into TWO pieces.
const weekCross = P.buildSegments([{ id: 'wc', title: 'Week crosser', start: '2026-08-08', end: '2026-08-11' }],
	2026, 8, 'sunday', 3);
out.seg_weekCross_count = weekCross.length;
out.seg_weekCross_shape = weekCross.map(function (s) {
	return s.week + ':' + s.startColumn + '-' + s.endColumn + ':' + (s.continuesLeft ? 'L' : '-')
		+ (s.continuesRight ? 'R' : '-');
}).join(' ');
out.seg_weekCross_sameEvent = weekCross.every(function (s) { return s.id === 'wc'; });

// A span longer than the rendered month clips to the month's first/last visible day, flagged on BOTH ends.
const overMonth = P.buildSegments([{ id: 'om', title: 'Over month', start: '2026-07-20', end: '2026-09-10' }],
	2026, 8, 'sunday', 3);
out.seg_clip_firstKey = P.dateKey(2026, 8, 1);
out.seg_clip_leftmost = overMonth.length ? (overMonth[0].continuesLeft === true) : false;
out.seg_clip_rightmost = overMonth.length ? (overMonth[overMonth.length - 1].continuesRight === true) : false;
out.seg_clip_allInMonth = overMonth.every(function (s) { return s.startColumn >= 0 && s.endColumn <= 6; });
// Aug 1 (Sat, row 0 col 6) through Aug 31 (Mon, row 5 col 1) = 6 week-row pieces.
out.seg_clip_count = overMonth.length;

// Lane assignment: two overlapping spans in one week row take lanes 0 and 1; a non-overlapping one reuses lane 0.
const laned = P.buildSegments([
	{ id: 'b', title: 'B', start: '2026-08-03', end: '2026-08-05' },
	{ id: 'a', title: 'A', start: '2026-08-02', end: '2026-08-04' },
	{ id: 'c', title: 'C', start: '2026-08-06', end: '2026-08-07' }
], 2026, 8, 'sunday', 3);
out.seg_lanes = laned.map(function (s) { return s.id + '@' + s.lane; }).join(',');

// STABILITY: laying out the very same month again must produce the identical lane assignment (no reshuffle).
function laneSignature(segs) {
	return segs.map(function (s) { return s.id + ':' + s.week + ':' + s.startColumn + ':' + s.lane; }).sort().join('|');
}
const stableInput = [
	{ id: 'z', title: 'Z', start: '2026-08-04', end: '2026-08-06' },
	{ id: 'y', title: 'Y', start: '2026-08-02', end: '2026-08-05' },
	{ id: 'x', title: 'X', start: '2026-08-03', end: '2026-08-08' }
];
out.seg_stable = laneSignature(P.buildSegments(stableInput, 2026, 8, 'sunday', 3))
	=== laneSignature(P.buildSegments(stableInput.slice().reverse(), 2026, 8, 'sunday', 3));

// Lanes beyond the budget overflow into the crossed days' "+N more" instead of growing the row.
const overBudget = P.buildSegments([
	{ id: 's1', title: 'S1', start: '2026-08-03', end: '2026-08-05' },
	{ id: 's2', title: 'S2', start: '2026-08-03', end: '2026-08-05' },
	{ id: 's3', title: 'S3', start: '2026-08-03', end: '2026-08-05' }
], 2026, 8, 'sunday', 2);
out.seg_budget_seated = P.segmentsForWeek(overBudget, 1).length;
out.seg_budget_laneCount = P.laneCount(overBudget, 1);
out.seg_budget_overflowAtMon = P.overflowBarsAt(overBudget, 1, 1).map(function (e) { return e.id; }).join(',');

out.coalesce = P.coalesceKey('cal1', 2026, 8, 4);

// ------------------------------------------------------------------------------------------------------------------
// DOM BINDING LAYER
// ------------------------------------------------------------------------------------------------------------------

out.hasInitInstance = typeof NS.initInstance === 'function';
out.hasFillEventNode = typeof NS.fillEventNode === 'function';

// Event fill uses textContent only - a title of "<img onerror>" is literal text, never parsed.
const xss = '<img src=x onerror="window.__x=1">';
const node = NS.fillEventNode(document, { id: 'e', title: xss, categoryId: 'team' }, catMap);
out.fill_tag = node.tagName;                                 // SPAN (no href)
out.fill_text = node.textContent;
out.fill_noChildEls = node.childNodes.filter(function (c) { return c.nodeType === 1; }).length;  // 0
out.fill_class = node.getAttribute('class');
const linked = NS.fillEventNode(document, { id: 'e', title: 'x', href: '/events/1', categoryId: 'team' }, catMap);
out.fill_linkedTag = linked.tagName;                         // A
out.fill_linkedHref = linked.getAttribute('href');
const unsafeLink = NS.fillEventNode(document, { id: 'e', title: 'x', href: 'https://evil/x' }, catMap);
out.fill_unsafeTag = unsafeLink.tagName;                     // SPAN - unsafe href not linked
out.fill_unsafeNoHref = unsafeLink.getAttribute('href') == null;

// A serving-shape calendar fixture (mirrors CalendarTable.of).  seededHtml carries a seed sidecar.
function buildFixture(opts) {
	opts = opts || {};
	const contract = opts.contract == null ? '2' : opts.contract;
	const endpoint = 'endpoint' in opts ? opts.endpoint : '/events/{year}/{month}';
	const seed = 'seed' in opts ? opts.seed : { contractVersion: '2', year: 2026, month: 8,
		events: [{ id: 'e1', title: 'Team offsite', start: '2026-08-14', categoryId: 'team', href: '/events/123' }] };
	let html = '<div data-juneau-calendar="cal1"'
		+ ' data-juneau-calendar-contract="' + contract + '"'
		+ ' data-juneau-calendar-today="2026-08-15"'
		+ (endpoint == null ? '' : ' data-juneau-calendar-endpoint="' + endpoint + '"')
		+ ' data-juneau-calendar-view="month"'
		+ ' data-juneau-calendar-weekstart="sunday"'
		+ ' data-juneau-calendar-lanebudget="3"'
		+ ' data-juneau-calendar-maxperday="' + (opts.maxPerDay == null ? 3 : opts.maxPerDay) + '" class="jc-cal">';
	html += '<div class="jc-cal-header"><div data-juneau-calendar-nav="1">'
		+ '<button data-juneau-calendar-prev="1">p</button>'
		+ '<button data-juneau-calendar-next="1">n</button>'
		+ '<button data-juneau-calendar-today-btn="1">t</button></div>'
		+ '<div data-juneau-calendar-title="1">August 2026</div></div>';
	html += '<div data-juneau-calendar-grid="1" class="jc-cal-grid">'
		+ '<div role="row" class="jc-cal-weekdays"><span data-juneau-calendar-cat="hdr" role="columnheader" class="jc-cal-weekday">Sun</span></div>'
		+ '</div>';
	html += '<ul data-juneau-calendar-legend="1" class="jc-cal-legend">'
		+ '<li data-juneau-calendar-cat="team" class="jc-cal-legend-item jc-cal-cat--blue">'
		+ '<button type="button" data-juneau-calendar-legend-toggle="1" data-juneau-calendar-cat="team"'
		+ ' aria-pressed="true" class="jc-cal-legend-toggle"><span class="jc-cal-legend-label">Team</span></button></li>'
		+ '<li data-juneau-calendar-cat="review" class="jc-cal-legend-item jc-cal-cat--green">'
		+ '<button type="button" data-juneau-calendar-legend-toggle="1" data-juneau-calendar-cat="review"'
		+ ' aria-pressed="true" class="jc-cal-legend-toggle"><span class="jc-cal-legend-label">Review</span></button></li>'
		+ '</ul>';
	html += '<template data-juneau-calendar-day="1"><div role="gridcell" class="jc-cal-day">'
		+ '<span class="jc-cal-day-num"></span><div class="jc-cal-day-lanes"></div>'
		+ '<div class="jc-cal-day-events"></div></div></template>';
	html += '<template data-juneau-calendar-event="1"><span class="jc-cal-event"></span></template>';
	html += '<template data-juneau-calendar-bar="1"><span class="jc-cal-bar"></span></template>';
	if (seed)
		html += '<script data-juneau-calendar-seed="1">' + JSON.stringify(seed) + '</script>';
	html += '</div>';
	return parseTestHtml(html).firstElementChild;
}

function weekCount(root) { return root.querySelector('[data-juneau-calendar-grid]').querySelectorAll('.jc-cal-week').length; }
function paintedTitles(root) {
	return root.querySelectorAll('.jc-cal-event').map(function (n) { return n.textContent; });
}
function paintedBarTitles(root) {
	return root.querySelectorAll('.jc-cal-bar').map(function (n) { return n.textContent; });
}
function legendToggle(root, cat) {
	return root.querySelectorAll('[data-juneau-calendar-legend-toggle]')
		.filter(function (n) { return n.getAttribute('data-juneau-calendar-cat') === cat; })[0];
}
// A month whose seed carries a team chip, a team spanning bar, and a review chip - enough to see the filter work.
function filterSeed() {
	return { contractVersion: '2', year: 2026, month: 8, events: [
		{ id: 'tc', title: 'TeamChip', start: '2026-08-14', categoryId: 'team' },
		{ id: 'tb', title: 'TeamBar', start: '2026-08-10', end: '2026-08-12', categoryId: 'team' },
		{ id: 'rc', title: 'ReviewChip', start: '2026-08-14', categoryId: 'review' }
	] };
}

// readCategoryMap must skip the columnheader and read jc-cal-cat--* off legend items.
const fixMap = NS.readCategoryMap(buildFixture());
out.map_team = fixMap.team;
out.map_review = fixMap.review;
out.map_noHeader = !Object.hasOwn(fixMap, 'hdr');

// Seed init: initial month painted from seed sidecar, no fetch issued (echoOk seed).
{
	sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('should not fetch initial seed month'); });
	pendingFetches = [];
	const root = buildFixture();
	NS.initInstance(root);
	out.seed_weeks = weekCount(root);                        // 6
	out.seed_painted = paintedTitles(root).indexOf('Team offsite') >= 0;
	out.seed_noFetch = pendingFetches.length === 0;
	out.seed_todayCell = root.querySelectorAll('.jc-cal-day--today').length === 1;
}

// Timed chips carry a leading HH:mm label; spanning seed events paint as bars, not chips.
{
	sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('should not fetch initial seed month'); });
	pendingFetches = [];
	const root = buildFixture({ seed: { contractVersion: '2', year: 2026, month: 8, events: [
		{ id: 'sp', title: 'Sprint', start: '2026-08-10', end: '2026-08-12', categoryId: 'team' },
		{ id: 'tm', title: 'Standup', start: '2026-08-14T09:30', end: '2026-08-14T10:00', categoryId: 'team' }
	] } });
	NS.initInstance(root);
	out.timed_barTitles = paintedBarTitles(root).join(',');
	out.timed_chipTitles = paintedTitles(root).join(',');
	const timedChip = root.querySelectorAll('.jc-cal-event')[0];
	out.timed_chipClass = timedChip ? timedChip.getAttribute('class') : null;
	const timeLabel = root.querySelector('.jc-cal-event-time');
	out.timed_label = timeLabel ? timeLabel.textContent : null;
	const bar = root.querySelector('.jc-cal-bar');
	out.timed_barEventId = bar ? bar.getAttribute('data-juneau-calendar-event-id') : null;
	out.timed_barSpan = bar && bar.style ? bar.style.getPropertyValue('--jc-cal-span') : null;
}

// Contract handshake fail-loud: contract !== "2" -> visible error, no fetch, no grid paint.
{
	warnings.length = 0; errors.length = 0;
	sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('should not fetch on contract mismatch'); });
	pendingFetches = [];
	const root = buildFixture({ contract: '1' });
	NS.initInstance(root);
	out.badContract_error = !!root.querySelector('.jc-cal-error');
	out.badContract_noFetch = pendingFetches.length === 0;
	out.badContract_notInit = root.__juneauCalendarInit !== true;
	out.badContract_loud = errors.length >= 1;
}

// Numeric 1 in a *live body* is refused (contract mismatch) after a fetch on navigation.
{
	sandbox.fetch = window.fetch = makeFetch(function (f) {
		f.resolve(jsonResponse({ contractVersion: 2, year: 2026, month: 9, events: [] }));   // numeric 2
	});
	pendingFetches = [];
	const root = buildFixture({ seed: null });               // no seed -> next nav fetches
	NS.initInstance(root);
	root.querySelector('[data-juneau-calendar-next]')._fire('click');
	settleFetches();
	return drainMicrotasks().then(function () {
		out.liveNumeric_error = !!root.querySelector('.jc-cal-error');
		return afterLiveNumeric(root);
	});
}

function afterLiveNumeric() {
	// Echo-check: a 200 body for the WRONG month is dropped (no paint of its events).
	return new Promise(function (resolve) {
		sandbox.fetch = window.fetch = makeFetch(function (f) {
			f.resolve(jsonResponse({ contractVersion: '2', year: 2099, month: 1,
				events: [{ id: 'z', title: 'WrongMonth', start: '2099-01-01' }] }));
		});
		pendingFetches = [];
		const root = buildFixture({ seed: null });
		NS.initInstance(root);
		root.querySelector('[data-juneau-calendar-next]')._fire('click');
		settleFetches();
		drainMicrotasks().then(function () {
			out.echo_dropped = paintedTitles(root).indexOf('WrongMonth') < 0;
			resolve(afterEcho());
		});
	});
}

function afterEcho() {
	// Coalesce + abort: with a seeded initial month (no initial fetch), navigate A(Sep) then B(Oct).  A's slow
	// payload arrives LAST and must be dropped (generation mismatch), and A's request was aborted.
	return new Promise(function (resolve) {
		abortCount = 0;
		const responders = [];
		sandbox.fetch = window.fetch = function (url, opts) {
			let resolveP;
			const p = new Promise(function (res) { resolveP = res; });
			responders.push({ url: url, opts: opts, resolve: resolveP });
			return p;
		};
		const root = buildFixture();                                        // seeded -> initial Aug paints, no fetch
		NS.initInstance(root);
		root.querySelector('[data-juneau-calendar-next]')._fire('click');   // -> Sep, fetch #0
		root.querySelector('[data-juneau-calendar-next]')._fire('click');   // -> Oct, fetch #1 (aborts #0)
		out.coalesce_aborted = abortCount >= 1;                             // the superseded Sep fetch was aborted
		// Resolve the SECOND (current, Oct) first, then the stale first (Sep).
		responders[1].resolve(jsonResponse({ contractVersion: '2', year: 2026, month: 10,
			events: [{ id: 'o', title: 'OctEvent', start: '2026-10-05' }] }));
		responders[0].resolve(jsonResponse({ contractVersion: '2', year: 2026, month: 9,
			events: [{ id: 's', title: 'SepStale', start: '2026-09-05' }] }));
		drainMicrotasks().then(function () {
			const titles = paintedTitles(root);
			out.coalesce_octPainted = titles.indexOf('OctEvent') >= 0;
			out.coalesce_staleDropped = titles.indexOf('SepStale') < 0;
			resolve(afterCoalesce());
		});
	});
}

function afterCoalesce() {
	// Fetch error (500) -> single-attempt visible error + empty month.
	return new Promise(function (resolve) {
		let calls = 0;
		sandbox.fetch = window.fetch = makeFetch(function (f) { calls++; f.resolve({ ok: false, status: 500,
			headers: { get: function () { return null; } }, json: function () { return Promise.resolve({}); } }); });
		pendingFetches = [];
		const root = buildFixture({ seed: null });
		NS.initInstance(root);
		root.querySelector('[data-juneau-calendar-next]')._fire('click');
		settleFetches();
		drainMicrotasks().then(function () {
			out.err_visible = !!root.querySelector('.jc-cal-error');
			out.err_singleAttempt = calls === 1;
			out.err_emptyMonth = paintedTitles(root).length === 0;
			resolve(afterError());
		});
	});
}

function afterError() {
	// Invalid-JSON (HTML content-type) -> error path too.
	return new Promise(function (resolve) {
		sandbox.fetch = window.fetch = makeFetch(function (f) {
			f.resolve({ ok: true, status: 200, headers: { get: function () { return 'text/html'; } },
				json: function () { return Promise.reject(new Error('not json')); } });
		});
		pendingFetches = [];
		const root = buildFixture({ seed: null });
		NS.initInstance(root);
		root.querySelector('[data-juneau-calendar-next]')._fire('click');
		settleFetches();
		drainMicrotasks().then(function () {
			out.html_error = !!root.querySelector('.jc-cal-error');
			resolve(afterHtmlError());
		});
	});
}

/*
 * Legend toggle-filter: pressing a category's toggle hides that category's CHIPS and its SPANNING SEGMENTS with no
 * refetch; pressing again reveals them; a landed month navigation RESETS the filter; a FAILED navigation PRESERVES it.
 */
function afterHtmlError() {
	return new Promise(function (resolve) {
		pendingFetches = [];
		sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('filtering must never refetch'); });
		const root = buildFixture({ seed: filterSeed() });
		NS.initInstance(root);
		out.filter_initialChips = paintedTitles(root).join(',');
		out.filter_initialBars = paintedBarTitles(root).join(',');

		const team = legendToggle(root, 'team');
		out.filter_hasToggles = !!team && team.getAttribute('aria-pressed') === 'true';
		team._fire('click');
		out.filter_hiddenChips = paintedTitles(root).join(',');
		out.filter_hiddenBars = paintedBarTitles(root).join(',');
		out.filter_pressedFalse = team.getAttribute('aria-pressed') === 'false';
		out.filter_noRefetch = pendingFetches.length === 0;

		team._fire('click');
		out.filter_revealedChips = paintedTitles(root).join(',');
		out.filter_revealedBars = paintedBarTitles(root).join(',');
		out.filter_pressedTrueAgain = team.getAttribute('aria-pressed') === 'true';
		resolve(afterFilter());
	});
}

function afterFilter() {
	// A LANDED month navigation resets the filter (state is per-instance and per-month).
	return new Promise(function (resolve) {
		pendingFetches = [];
		sandbox.fetch = window.fetch = makeFetch(function (f) {
			f.resolve(jsonResponse({ contractVersion: '2', year: 2026, month: 9,
				events: [{ id: 'n', title: 'SepTeam', start: '2026-09-04', categoryId: 'team' }] }));
		});
		const root = buildFixture({ seed: filterSeed() });
		NS.initInstance(root);
		legendToggle(root, 'team')._fire('click');
		root.querySelector('[data-juneau-calendar-next]')._fire('click');
		settleFetches();
		drainMicrotasks().then(function () {
			out.navReset_painted = paintedTitles(root).join(',');       // the team category is visible again
			out.navReset_pressed = legendToggle(root, 'team').getAttribute('aria-pressed');
			resolve(afterNavReset());
		});
	});
}

function afterNavReset() {
	// A FAILED navigation preserves the filter: the toggle stays un-pressed and the category stays hidden.
	return new Promise(function (resolve) {
		pendingFetches = [];
		sandbox.fetch = window.fetch = makeFetch(function (f) {
			f.resolve({ ok: false, status: 500, headers: { get: function () { return null; } },
				json: function () { return Promise.resolve({}); } });
		});
		const root = buildFixture({ seed: filterSeed() });
		NS.initInstance(root);
		legendToggle(root, 'team')._fire('click');
		root.querySelector('[data-juneau-calendar-next]')._fire('click');
		settleFetches();
		drainMicrotasks().then(function () {
			out.navFail_error = !!root.querySelector('.jc-cal-error');
			out.navFail_pressed = legendToggle(root, 'team').getAttribute('aria-pressed');
			resolve(afterNavFail());
		});
	});
}

/*
 * "+N more" rides the ONE shared layer stack: with juneau-views.js loaded it registers a light-dismiss "popover"
 * layer that Escape pops with focus restored to the trigger; with the stack ABSENT it FAILS LOUD instead of standing
 * up a second stack of its own.
 */
function afterNavFail() {
	return new Promise(function (resolve) {
		const busySeed = { contractVersion: '2', year: 2026, month: 8, events: [
			{ id: 'm1', title: 'M1', start: '2026-08-14', categoryId: 'team' },
			{ id: 'm2', title: 'M2', start: '2026-08-14', categoryId: 'team' },
			{ id: 'm3', title: 'M3', start: '2026-08-14', categoryId: 'team' },
			{ id: 'm4', title: 'M4', start: '2026-08-14', categoryId: 'team' }
		] };

		// (a) No shared stack -> loud console.error + visible inline error, and NO popover is opened.
		uninstallViewsLayerStack();
		errors.length = 0;
		pendingFetches = [];
		sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('no fetch expected'); });
		const bare = buildFixture({ seed: busySeed });
		NS.initInstance(bare);
		const bareMore = bare.querySelector('.jc-cal-more');
		out.pop_moreLabel = bareMore ? bareMore.textContent : null;      // "+1 more" - only what it hides
		if (bareMore) bareMore._fire('click');
		out.pop_noStackLoud = errors.some(function (e) { return e.indexOf('pushLayer') >= 0; });
		out.pop_noStackNoPopover = !bare.querySelector('.jc-cal-popover') && !document.body.querySelector('.jc-cal-popover');
		out.pop_noStackVisibleError = !!bare.querySelector('.jc-cal-error');

		// (b) With the shared stack: the popover is portalled onto it, Escape pops it and focus returns to the trigger.
		installViewsLayerStack();
		const root = buildFixture({ seed: busySeed });
		NS.initInstance(root);
		const more = root.querySelector('.jc-cal-more');
		document.activeElement = more;
		more._fire('click');
		const top = window.JuneauViews.init.topLayer();
		out.pop_registered = !!top && top.kind === 'popover';
		out.pop_lightDismiss = !!top && top.lightDismiss === true;
		out.pop_expanded = more.getAttribute('aria-expanded');
		out.pop_zOrdered = !!top && top.el.getAttribute('data-juneau-layer') === '0';
		out.pop_listsHidden = document.body.querySelectorAll('.jc-cal-popover')
			.map(function (p) { return p.querySelectorAll('.jc-cal-event').length; }).join(',');
		document.activeElement = null;
		document._fire('keydown', { key: 'Escape' });
		out.pop_escapePopped = window.JuneauViews.init.topLayer() === null;
		out.pop_detached = document.body.querySelectorAll('.jc-cal-popover').length === 0;
		out.pop_focusRestored = document.activeElement === more;
		out.pop_collapsed = more.getAttribute('aria-expanded');
		resolve(finish());
	});
}

function finish() {
	process.stdout.write(JSON.stringify(out));
}

// Let queued microtasks (promise .then chains) run to completion.
function drainMicrotasks() {
	return new Promise(function (resolve) { setTimeout(resolve, 0); });
}
