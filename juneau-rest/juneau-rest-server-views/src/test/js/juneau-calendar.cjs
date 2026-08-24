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
	body: { appendChild: function () {}, querySelectorAll: function () { return []; } }
};

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

out.contract_okStr = P.contractOk('1');
out.contract_badNum = P.contractOk(1);                       // numeric 1 must fail strict ===
out.contract_bad2 = P.contractOk('2');

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

const capped = P.applyCap([1, 2, 3, 4, 5], 3);
out.cap_shown = capped.shown.length;
out.cap_overflow = capped.overflow;

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
	const contract = opts.contract == null ? '1' : opts.contract;
	const endpoint = 'endpoint' in opts ? opts.endpoint : '/events/{year}/{month}';
	const seed = 'seed' in opts ? opts.seed : { contractVersion: '1', year: 2026, month: 8,
		events: [{ id: 'e1', title: 'Team offsite', start: '2026-08-14', categoryId: 'team', href: '/events/123' }] };
	let html = '<div data-juneau-calendar="cal1"'
		+ ' data-juneau-calendar-contract="' + contract + '"'
		+ ' data-juneau-calendar-today="2026-08-15"'
		+ (endpoint == null ? '' : ' data-juneau-calendar-endpoint="' + endpoint + '"')
		+ ' data-juneau-calendar-view="month"'
		+ ' data-juneau-calendar-weekstart="sunday"'
		+ ' data-juneau-calendar-maxperday="3" class="jc-cal">';
	html += '<div class="jc-cal-header"><div data-juneau-calendar-nav="1">'
		+ '<button data-juneau-calendar-prev="1">p</button>'
		+ '<button data-juneau-calendar-next="1">n</button>'
		+ '<button data-juneau-calendar-today-btn="1">t</button></div>'
		+ '<div data-juneau-calendar-title="1">August 2026</div></div>';
	html += '<div data-juneau-calendar-grid="1" class="jc-cal-grid">'
		+ '<div role="row" class="jc-cal-weekdays"><span data-juneau-calendar-cat="hdr" role="columnheader" class="jc-cal-weekday">Sun</span></div>'
		+ '</div>';
	html += '<ul data-juneau-calendar-legend="1" class="jc-cal-legend">'
		+ '<li data-juneau-calendar-cat="team" class="jc-cal-legend-item jc-cal-cat--blue"><span class="jc-cal-legend-label">Team</span></li>'
		+ '<li data-juneau-calendar-cat="review" class="jc-cal-legend-item jc-cal-cat--green"><span class="jc-cal-legend-label">Review</span></li>'
		+ '</ul>';
	html += '<template data-juneau-calendar-day="1"><div role="gridcell" class="jc-cal-day">'
		+ '<span class="jc-cal-day-num"></span><div class="jc-cal-day-events"></div></div></template>';
	html += '<template data-juneau-calendar-event="1"><span class="jc-cal-event"></span></template>';
	if (seed)
		html += '<script data-juneau-calendar-seed="1">' + JSON.stringify(seed) + '</script>';
	html += '</div>';
	return parseTestHtml(html).firstElementChild;
}

function weekCount(root) { return root.querySelector('[data-juneau-calendar-grid]').querySelectorAll('.jc-cal-week').length; }
function paintedTitles(root) {
	return root.querySelectorAll('.jc-cal-event').map(function (n) { return n.textContent; });
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

// Contract handshake fail-loud: contract !== "1" -> visible error, no fetch, no grid paint.
{
	warnings.length = 0; errors.length = 0;
	sandbox.fetch = window.fetch = makeFetch(function () { throw new Error('should not fetch on contract mismatch'); });
	pendingFetches = [];
	const root = buildFixture({ contract: '2' });
	NS.initInstance(root);
	out.badContract_error = !!root.querySelector('.jc-cal-error');
	out.badContract_noFetch = pendingFetches.length === 0;
	out.badContract_notInit = root.__juneauCalendarInit !== true;
	out.badContract_loud = errors.length >= 1;
}

// Numeric 1 in a *live body* is refused (contract mismatch) after a fetch on navigation.
{
	sandbox.fetch = window.fetch = makeFetch(function (f) {
		f.resolve(jsonResponse({ contractVersion: 1, year: 2026, month: 9, events: [] }));   // numeric 1
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
			f.resolve(jsonResponse({ contractVersion: '1', year: 2099, month: 1,
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
		responders[1].resolve(jsonResponse({ contractVersion: '1', year: 2026, month: 10,
			events: [{ id: 'o', title: 'OctEvent', start: '2026-10-05' }] }));
		responders[0].resolve(jsonResponse({ contractVersion: '1', year: 2026, month: 9,
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
			resolve(finish());
		});
	});
}

function finish() {
	process.stdout.write(JSON.stringify(out));
}

// Let queued microtasks (promise .then chains) run to completion.
function drainMicrotasks() {
	return new Promise(function (resolve) { setTimeout(resolve, 0); });
}
