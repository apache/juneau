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
 * jobs.cjs - always-on Node harness for JuneauViews.jobs (row-bound jobs run from a row or its detail panel), the
 * shared row resolver behind hookContextOf, and helpers.jobButton.  A view table with stamped rows, a hand-expanded
 * detail panel, a fake EventSource and routed fetch stand in for the browser; every assertion lives in the Java test.
 *
 *   Usage: node jobs.cjs <juneau-renders.js> <juneau-views.js> <juneau-helpers.js>
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadScripts, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [, , rendersJsPath, viewsJsPath, helpersJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !helpersJsPath) {
	console.error('usage: node jobs.cjs <juneau-renders.js> <juneau-views.js> <juneau-helpers.js>');
	process.exit(2);
}

const out = {};
const errors = [];
const realError = console.error;
console.error = function (...a) { errors.push(a.join(' ')); };

const STARTED = { jobId: 'j-1', streamUrl: '/s/j-1', cancelUrl: '/c/j-1' };
const KEY_URL = '/species/{id}/key';
const RESTOCK = '/species/{id}/restock';

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 10) { return r(); } setTimeout(tick, 0); })(); });
}

function never() { return new Promise(function () { /* never settles */ }); }

/** A view table with rows r1 and r2, each carrying an actions cell, and a routed fetch. */
function setup(o) {
	const opts = o || {};
	const env = makeEnv();
	const sources = [];
	const FakeES = function (url) { this.url = url; this.listeners = {}; this.closed = false; this.readyState = 1; sources.push(this); };
	FakeES.prototype.addEventListener = function (type, fn) { this.listeners[type] ||= []; this.listeners[type].push(fn); };
	FakeES.prototype.close = function () { this.closed = true; this.readyState = 2; };
	FakeES.prototype.fire = function (type, data) { (this.listeners[type] || []).forEach(function (fn) { fn({ data: data }); }); };
	env.EventSource = FakeES;
	const { NS } = loadScripts([rendersJsPath, viewsJsPath, helpersJsPath], env);
	const table = env.el('table');
	table.dataset.juneauView = 'species';
	table.setAttribute('id', 'species-t');
	if (!opts.noCsrf) table.dataset.juneauCsrf = 'tok-1';
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	env.body.appendChild(table);
	const rowData = new Map();
	const drawHandlers = [];
	table.__juneauCtx = {
		table: table,
		viewDef: { id: 'species', rowActions: [{ id: 'restock-species', label: 'Restock' }, { id: 'ping', label: 'Ping', endpoint: '/species/{id}/ping', method: 'POST' }] },
		dataTable: {
			row: function (tr) { return { data: function () { return rowData.get(tr); } }; },
			on: function (ev, fn) { if (ev === 'draw.dt') drawHandlers.push(fn); }
		}
	};
	function makeRow(id, data) {
		const tr = env.el('tr');
		if (id != null) tr.dataset.juneauRowId = id;
		const td = env.el('td');
		td.className = 'juneau-view-actions-cell';
		const trig = env.el('button');
		trig.className = 'juneau-view-action-trigger';
		td.appendChild(trig);
		tr.appendChild(td);
		const cell = env.el('td');
		const btn = env.el('button');
		cell.appendChild(btn);
		tr.appendChild(cell);
		tbody.appendChild(tr);
		rowData.set(tr, data || { id: id });
		tr._btn = btn;
		tr._trigger = trig;
		return tr;
	}
	/** Expands `tr` by hand: a child row after it holding a stamped panel back-linked to `tr`. */
	function expand(tr, panelId) {
		const child = env.el('tr');
		const td = env.el('td');
		const panel = env.el('div');
		panel.className = 'juneau-view-detail-panel';
		panel.dataset.juneauRowDetail = '';
		panel.dataset.juneauRowId = panelId == null ? tr.dataset.juneauRowId : panelId;
		panel._juneauParentTr = tr;
		tr._juneauDetailPanel = panel;
		tr.classList.add('juneau-view-detail-open');
		td.appendChild(panel);
		child.appendChild(td);
		tbody.insertBefore(child, tr.nextSibling);
		const btn = env.el('button');
		panel.appendChild(btn);
		child._panel = panel;
		panel._btn = btn;
		return panel;
	}
	/** Redraws: every body row is replaced by a fresh <tr> with the same id, then draw.dt fires. */
	function redraw() {
		const old = Array.prototype.slice.call(tbody.childNodes);
		old.forEach(function (n) { n.remove(); });
		const fresh = {};
		old.forEach(function (n) {
			const id = n.dataset.juneauRowId ?? null;
			if (id != null) fresh[id] = makeRow(id, rowData.get(n));
		});
		drawHandlers.forEach(function (fn) { fn({ target: table }); });
		return fresh;
	}
	const calls = [];
	const routes = { '/species/r1/key': function () { return Promise.resolve(jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: 'k-' + (++keyCount) })); },
		'/species/r1/restock': function () { return Promise.resolve(jsonResponse(STARTED, { status: 202 })); },
		'/c/j-1': function () { return Promise.resolve(jsonResponse({})); }, ...opts.routes || {} };
	let keyCount = 0;
	env.setFetch(function (url, init) {
		const i = init || {};
		calls.push({ url: String(url), method: i.method || 'GET', headers: i.headers || {}, body: i.body, signal: i.signal });
		const fn = routes[url];
		return fn ? fn(i) : Promise.reject(new TypeError('no route ' + url));
	});
	const r1 = makeRow('r1', { id: 'r1', species: 'cat' });
	const r2 = makeRow('r2', { id: 'r2', species: 'dog' });
	return { env: env, NS: NS, I: NS.init, H: NS.helpers, table: table, tbody: tbody, r1: r1, r2: r2, sources: sources,
		calls: calls, expand: expand, redraw: redraw, makeRow: makeRow, rowData: rowData, keyCalls: function () { return keyCount; } };
}

const bodyOf = (c) => { try { return JSON.parse(c.body); } catch (e) { return null; } }; // NOSONAR javascript:S2486 -- an unparseable body reads as null

/** A hooks object recording each call in order. */
function recorder() {
	const order = [];
	const seen = {};
	const hooks = {};
	['onStart', 'onProgress', 'onResult', 'onEnd'].forEach(function (n) {
		hooks[n] = function (x) { order.push(n); seen[n] = x; };
	});
	return { hooks: hooks, order: order, seen: seen };
}

(async function main() { // NOSONAR javascript:S3776 - branching mirrors the behavior being implemented; splitting it would obscure the flow

	// --- j01: action envelope from inside a detail panel: key, submit, history stream, registry, hooks, release -------
	{
		const s = setup();
		const panel = s.expand(s.r1);
		const rec = recorder();
		const h = s.NS.jobs.run({ el: panel._btn, trigger: panel._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species',
			envelope: 'action', fields: { count: 2 }, hooks: rec.hooks });
		out.j01_triggerDisabled = panel._btn.disabled === true;
		out.j01_pendingHoldsPoll = s.I.hasOpenRunningJobPanel(s.table) === true;
		await flush();
		out.j01_keyCall = s.calls[0]?.url === '/species/r1/key' && s.calls[0].method === 'POST';
		out.j01_keyCsrf = s.calls[0]?.headers['X-Csrf-Token'] === 'tok-1';
		out.j01_keyBody = s.calls[0]?.body;
		out.j01_submitUrl = s.calls[1]?.url;
		out.j01_submitBody = s.calls[1] && bodyOf(s.calls[1]);
		out.j01_streamUrl = s.sources[0]?.url;
		out.j01_marker = s.r1.dataset.juneauJob === '1';
		out.j01_activeMatches = s.NS.jobs.active(panel._btn, 'restock-species') === h.job && h.job?.jobId === 'j-1';
		out.j01_activeOtherAction = s.NS.jobs.active(panel._btn, 'other') === null;
		out.j01_activeOtherRow = s.NS.jobs.active(s.r2._btn) === null;
		out.j01_runningHoldsPoll = s.I.hasOpenRunningJobPanel(s.table) === true;
		s.sources[0].fire('progress', 'half way');
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'restocked' }));
		const end = await h.done;
		out.j01_state = end.state;
		out.j01_order = rec.order.join(',');
		out.j01_progressArg = rec.seen.onProgress;
		out.j01_resultMessage = rec.seen.onResult?.message;
		out.j01_endFrozen = Object.isFrozen(end) && rec.seen.onEnd === end;
		out.j01_handleFrozen = Object.isFrozen(h);
		out.j01_markerCleared = s.r1.dataset.juneauJob === undefined;
		out.j01_activeAfter = s.NS.jobs.active(panel._btn) === null;
		out.j01_pollReleased = s.I.hasOpenRunningJobPanel(s.table) === false;
		out.j01_triggerEnabled = panel._btn.disabled === false;
		out.j01_closed = s.sources[0].closed;
	}

	// --- j02: flat envelope with a given key and a synchronous typed result ---------------------------------------------
	{
		const s = setup({ routes: { '/species/r2/sync': function () { return Promise.resolve(jsonResponse({ outcome: 'success', message: 'ok' })); } } });
		const rec = recorder();
		const end = await s.NS.jobs.run({ el: s.r2._btn, endpoint: '/species/{id}/sync', idempotencyKey: 'given-1', fields: { a: 1 }, hooks: rec.hooks }).done;
		out.j02_state = end.state;
		out.j02_calls = s.calls.length;
		out.j02_body = s.calls[0] && bodyOf(s.calls[0]);
		out.j02_order = rec.order.join(',');
		out.j02_noStream = s.sources.length === 0;
	}

	// --- j03: only {id} substitutes; another token refuses before any request -----------------------------------------
	{
		const s = setup();
		const end = await s.NS.jobs.run({ el: s.r1._btn, endpoint: '/species/{species}/restock', idempotencyKey: 'k' }).done;
		out.j03_state = end.state;
		out.j03_reason = end.error?.reason;
		out.j03_calls = s.calls.length;
		const k = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: '/species/{name}/key', action: 'restock-species', envelope: 'action' }).done;
		out.j03_keyReason = k.error?.reason;
		out.j03_keyCalls = s.calls.length;
		out.j03_encoded = s.I.substituteJobUrl('/species/{id}/x', 'a b').url;
		out.j03_rowlessToken = s.I.substituteJobUrl('/species/{id}/x', null).reason;
	}

	// --- j04: no row id: an unstamped row, and a panel whose stamp disagrees with its parent row ----------------------
	{
		const s = setup();
		const bare = s.makeRow(null);
		const a = await s.NS.jobs.run({ el: bare._btn, endpoint: '/x', keyUrl: '/k', action: 'restock-species', envelope: 'action' }).done;
		out.j04_unstamped = a.error?.reason;
		const panel = s.expand(s.r2, 'r9');
		const b = await s.NS.jobs.run({ el: panel._btn, endpoint: '/x', idempotencyKey: 'k' }).done;
		out.j04_mismatch = b.error?.reason;
		out.j04_calls = s.calls.length;
		out.j04_state = b.state;
	}

	// --- j05: one job per row: a second run while the first is pending is refused ---------------------------------------
	{
		const s = setup({ routes: { '/species/r1/key': never } });
		const first = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		const second = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' }).done;
		out.j05_reason = second.error?.reason;
		out.j05_firstPending = first.job === null;
		s.NS.jobs.run({ el: s.r2._btn, endpoint: '/never', idempotencyKey: 'k' });
		await flush();
		out.j05_otherRowNotRefused = s.calls.some(function (c) { return c.url === '/never'; });
	}

	// --- j06: no CSRF token refuses before any request ------------------------------------------------------------------
	{
		const s = setup({ noCsrf: true });
		const end = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' }).done;
		out.j06_reason = end.error?.reason;
		out.j06_calls = s.calls.length;
		// The row is free again: the refusal released its registry entry.
		out.j06_released = s.NS.jobs.active(s.r1._btn) === null;
	}

	// --- j07: fetchKey checks the answer (E-JS-109) ---------------------------------------------------------------------
	{
		const s = setup({ routes: {
			'/species/r2/key': function () { return Promise.resolve(jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: 'x' })); },
			'/wrong/r1': function () { return Promise.resolve(jsonResponse({ contractVersion: '1', action: 'other', targetId: 'r1', idempotencyKey: 'x' })); },
			'/v2/r1': function () { return Promise.resolve(jsonResponse({ contractVersion: '2', action: 'restock-species', targetId: 'r1', idempotencyKey: 'x' })); },
			'/blank/r1': function () { return Promise.resolve(jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: ' ' })); },
			'/500/r1': function () { return Promise.resolve(jsonResponse('nope', { status: 500 })); }
		} });
		out.j07_ok = await s.NS.jobs.fetchKey({ el: s.r1._btn, keyUrl: KEY_URL, action: 'restock-species' });
		const rej = async function (o) { try { await s.NS.jobs.fetchKey(o); return 'resolved'; } catch (e) { return e.message; } };
		errors.length = 0;
		out.j07_otherRow = await rej({ el: s.r2._btn, keyUrl: KEY_URL, action: 'restock-species' });
		out.j07_wrongAction = await rej({ el: s.r1._btn, keyUrl: '/wrong/{id}', action: 'restock-species' });
		out.j07_wrongContract = await rej({ el: s.r1._btn, keyUrl: '/v2/{id}', action: 'restock-species' });
		out.j07_blankKey = await rej({ el: s.r1._btn, keyUrl: '/blank/{id}', action: 'restock-species' });
		out.j07_http = await rej({ el: s.r1._btn, keyUrl: '/500/{id}', action: 'restock-species' });
		out.j07_outsideRow = await rej({ el: s.env.body, keyUrl: KEY_URL, action: 'restock-species' });
		out.j07_logged = errors.filter(function (e) { return e.indexOf('E-JS-109') >= 0; }).length;
		// A run whose key is refused never submits.
		const end = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: '/wrong/{id}', action: 'restock-species', envelope: 'action' }).done;
		out.j07_runReason = end.error?.reason;
		out.j07_runNoSubmit = !s.calls.some(function (c) { return c.url === '/species/r1/restock'; });
	}

	// --- j08: the key survives a transport failure and is discarded once the server answers -----------------------------
	{
		let submits = 0;
		const s = setup({ routes: { '/species/r1/restock': function () {
			submits++;
			if (submits === 1) return Promise.reject(new TypeError('network down'));
			if (submits === 2) return Promise.resolve(jsonResponse('boom', { status: 500 }));
			return Promise.resolve(jsonResponse({ outcome: 'success' }));
		} } });
		const spec = function () { return { el: s.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' }; };
		const a = await s.NS.jobs.run(spec()).done;
		out.j08_firstReason = a.error?.reason;
		const b = await s.NS.jobs.run(spec()).done;
		out.j08_secondReason = b.error?.reason;
		out.j08_keysAfterTwo = s.keyCalls();
		const submitKeys = s.calls.filter(function (c) { return c.url === '/species/r1/restock'; }).map(function (c) { return bodyOf(c).idempotencyKey; });
		const c = await s.NS.jobs.run(spec()).done;
		out.j08_thirdState = c.state;
		out.j08_keysAfterThree = s.keyCalls();
		const all = s.calls.filter(function (x) { return x.url === '/species/r1/restock'; }).map(function (x) { return bodyOf(x).idempotencyKey; });
		out.j08_retryReusedKey = submitKeys.length === 2 && submitKeys[0] === submitKeys[1];
		out.j08_freshKeyAfterAnswer = all.length === 3 && all[2] !== all[1];
	}

	// --- j09: typed refusal (409) is refused; a failure is failed --------------------------------------------------------
	{
		const s = setup({ routes: {
			'/species/r1/restock': function () { return Promise.resolve(jsonResponse({ outcome: 'refusal', code: 'write-guard:idempotency-binding-mismatch', message: 'This key is not valid for this row.' }, { status: 409 })); },
			'/species/r2/fail': function () { return Promise.resolve(jsonResponse({ outcome: 'failure', message: 'no stock' })); }
		} });
		const a = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k' }).done;
		out.j09_refused = a.state;
		out.j09_refusedResult = a.result?.message;
		const b = await s.NS.jobs.run({ el: s.r2._btn, endpoint: '/species/{id}/fail', idempotencyKey: 'k' }).done;
		out.j09_failed = b.state;
	}

	// --- j10: paint none draws no progress or outcome in the row, but still marks and releases it ---------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', paint: 'none' });
		await flush();
		out.j10_noProgress = s.table.querySelector('[data-testid="job-progress"]') === null;
		out.j10_marker = s.r1.dataset.juneauJob === '1';
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'quiet' }));
		const end = await h.done;
		out.j10_state = end.state;
		out.j10_noOutcome = s.table.querySelector('.juneau-view-action-outcome') === null
			&& (s.r1.textContent || '').indexOf('quiet') < 0;
	}

	// --- j11: a throwing hook is logged (E-JS-113) and never interrupts the job ----------------------------------------
	{
		const s = setup();
		errors.length = 0;
		let ended = null;
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k',
			hooks: { onStart: function () { throw new Error('bad hook'); }, onEnd: function (x) { ended = x.state; } } });
		await flush();
		out.j11_logged = errors.some(function (e) { return e.indexOf('E-JS-113') >= 0 && e.indexOf('onStart') >= 0 && e.indexOf('bad hook') >= 0; });
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
		const end = await h.done;
		out.j11_state = end.state;
		out.j11_onEnd = ended;
	}

	// --- j12: a redraw replaces the <tr>; the marker comes back and the outcome lands on the new row --------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k' });
		await flush();
		const fresh = s.redraw();
		out.j12_newRowMarked = fresh.r1.dataset.juneauJob === '1';
		out.j12_otherRowUnmarked = fresh.r2.dataset.juneauJob === undefined;
		out.j12_newTriggerDisabled = fresh.r1._trigger.disabled === true;
		out.j12_active = s.NS.jobs.active(fresh.r1._btn) != null;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
		await h.done;
		out.j12_newRowCleared = fresh.r1.dataset.juneauJob === undefined;
	}

	// --- j13: a collapsed row's job holds no poll; tolerateReconnect ignores a CONNECTING error ------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k' });
		await flush();
		out.j13_collapsedNoHold = s.I.hasOpenRunningJobPanel(s.table) === false;
		s.sources[0].readyState = 0;
		s.sources[0].fire('error');
		out.j13_reconnecting = s.NS.jobs.active(s.r1._btn) != null && !s.sources[0].closed;
		s.sources[0].readyState = 2;
		s.sources[0].fire('error');
		const end = await h.done;
		out.j13_state = end.state;
		out.j13_reason = end.error?.reason;
	}

	// --- j14: cancel() posts the job's cancel URL with the CSRF token ----------------------------------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k' });
		out.j14_cancelBeforeStart = await h.cancel();
		await flush();
		await h.cancel();
		const c = s.calls.find(function (x) { return x.url === '/c/j-1'; });
		out.j14_cancelPosted = !!c && c.method === 'POST' && c.headers['X-Csrf-Token'] === 'tok-1';
	}

	// --- j15: an aborted signal ends the run and frees the row -----------------------------------------------------------
	{
		const s = setup();
		const ac = new AbortController();
		ac.abort();
		const a = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action', signal: ac.signal }).done;
		out.j15_reason = a.error?.reason;
		out.j15_noCalls = s.calls.length === 0;
		const ac2 = new AbortController();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', signal: ac2.signal });
		await flush();
		out.j15_streaming = s.sources.length === 1;
		ac2.abort();
		const b = await h.done;
		out.j15_streamAborted = b.error?.reason;
		out.j15_streamClosed = s.sources[0].closed;
		out.j15_released = s.NS.jobs.active(s.r1._btn) === null;
	}

	// --- j16: helpers.jobButton runs through JuneauViews.jobs, and says so when it is missing (E-JS-111) ------------------
	{
		const s = setup({ routes: { '/species/r1/sync': function () { return Promise.resolve(jsonResponse({ outcome: 'success' })); } } });
		const panel = s.expand(s.r1);
		const btn = s.H.jobButton({ label: 'Restock', endpoint: '/species/{id}/sync', idempotencyKey: 'k' });
		panel.appendChild(btn);
		out.j16_class = (btn.className || '').indexOf('juneau-view-helper-job-btn') >= 0;
		btn.dispatch('click');
		await flush();
		out.j16_posted = s.calls.length === 1 && s.calls[0].url === '/species/r1/sync';
		out.j16_bodyHasNoLabel = s.calls[0] && bodyOf(s.calls[0]).label === undefined;
		const jobs = s.NS.jobs;
		delete s.env.window.JuneauViews.jobs;
		errors.length = 0;
		btn.dispatch('click');
		out.j16_missingLogged = errors.some(function (e) { return e.indexOf('E-JS-111') >= 0; });
		out.j16_missingSentNothing = s.calls.length === 1;
		s.env.window.JuneauViews.jobs = jobs;
	}

	// --- h01: hookContextOf (rowActions.contextOf) resolves a panel element to the PARENT row ---------------------------
	{
		const s = setup();
		const panel = s.expand(s.r1);
		const c = s.NS.rowActions.contextOf(panel._btn);
		out.h01_parentTr = !!c && c.tr === s.r1;
		out.h01_parentRow = c?.row?.species;
		out.h01_table = !!c && c.table === s.table;
		const plain = s.NS.rowActions.contextOf(s.r2._btn);
		out.h01_plainRow = !!plain && plain.tr === s.r2 && plain.row.species === 'dog';
		// A nested view table inside the panel: its own row stays its own.
		const nested = s.env.el('table');
		nested.dataset.juneauView = 'litters';
		nested.setAttribute('id', 'litters-t');
		const nb = s.env.el('tbody');
		const ntr = s.env.el('tr');
		ntr.dataset.juneauRowId = 'n1';
		const ntd = s.env.el('td');
		const nbtn = s.env.el('button');
		ntd.appendChild(nbtn); ntr.appendChild(ntd); nb.appendChild(ntr); nested.appendChild(nb); panel.appendChild(nested);
		nested.__juneauCtx = { table: nested, viewDef: { id: 'litters' }, dataTable: { row: function () { return { data: function () { return { id: 'n1', kind: 'nested' }; } }; } } };
		const nc = s.NS.rowActions.contextOf(nbtn);
		out.h01_nestedTr = !!nc && nc.tr === ntr && nc.table === nested;
		out.h01_nestedRow = nc?.row?.kind;
		const r = s.I.rowOfElement(nbtn);
		out.h01_resolverNested = r.tr === ntr && r.panel === null;
		const rp = s.I.rowOfElement(panel._btn);
		out.h01_resolverPanel = rp.tr === s.r1 && rp.panel === panel;
	}

	// --- j17: pending() resolves to the ref once the job starts, or to null when it ends first; never while running ---
	{
		let release;
		const s = setup({ routes: { '/species/r1/restock': function () { return new Promise(function (res) { release = res; }); } } });
		const panel = s.expand(s.r1);
		out.j17_noneBefore = s.NS.jobs.pending(panel._btn, 'restock-species') === null;
		s.NS.jobs.run({ el: panel._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		const p = s.NS.jobs.pending(s.r1, 'restock-species');
		out.j17_pendingIsPromise = !!p && typeof p.then === 'function';
		out.j17_otherAction = s.NS.jobs.pending(s.r1, 'other') === null;
		await flush();
		release(jsonResponse(STARTED, { status: 202 }));
		const ref = await p;
		out.j17_ref = ref?.jobId;
		out.j17_noneWhileRunning = s.NS.jobs.pending(s.r1, 'restock-species') === null;
		const s2 = setup({ routes: { '/species/r1/restock': function () { return Promise.reject(new TypeError('offline')); } } });
		s2.NS.jobs.run({ el: s2.r1._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		const p2 = s2.NS.jobs.pending(s2.r1, 'restock-species');
		out.j17_endedFirst = (await p2) === null;
	}

	// --- j18: the poll hold (Decision 12) through the real initPolling pill and preDraw cancel, on a view that never
	// opted into pausePollingWhileEditing ------------------------------------------------------------------------------
	{
		let release;
		const s = setup({ routes: { '/species/r1/restock': function () { return new Promise(function (res) { release = res; }); } } });
		const ctx = s.table.__juneauCtx;
		const viewDef = { pollIntervalMs: 5000 };
		const handlers = {};
		const dt = { on: function (ev, fn) { handlers[ev] ||= []; handlers[ev].push(fn); }, ajax: { reload: function () { /* not fired */ } } };
		/** Lands a timer (or user) reload: preDraw.dt first; a false from any handler cancels the draw. */
		function land(timer) {
			ctx._pollDrawPending = timer;
			const cancelled = (handlers['preDraw.dt'] || []).map(function (fn) { return fn({ target: s.table }); }).some(function (r) { return r === false; });
			if (!cancelled) (handlers['draw.dt'] || []).forEach(function (fn) { fn({ target: s.table }); });
			return !cancelled;
		}
		const indicator = s.I.buildStalenessIndicator();
		s.env.body.appendChild(indicator);
		s.I.initPolling(s.table, dt, viewDef, indicator, ctx);
		function collapse(tr) {
			const panel = tr._juneauDetailPanel;
			const child = panel.parentNode.parentNode;
			child.remove();
			tr.classList.remove('juneau-view-detail-open');
			tr._juneauDetailPanel = null;
		}
		out.j18_idleState = indicator.dataset.state;
		// A row-action job with no panel, and a job on a row whose panel is shut, hold nothing.
		s.NS.jobs.run({ el: s.r2._btn, endpoint: '/species/r2/restock', idempotencyKey: 'k-r2' });
		out.j18_unexpandedRowNoHold = s.I.isPollSuspended(s.table, ctx, viewDef) === false && land(true) === true;
		const panel = s.expand(s.r1);
		s.NS.jobs.run({ el: panel._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		out.j18_pendingHolds = s.I.isPollSuspended(s.table, ctx, viewDef) === true;
		out.j18_pendingState = indicator.dataset.state;
		out.j18_pendingTimerDrawCancelled = land(true) === false;
		await flush();
		release(jsonResponse(STARTED, { status: 202 }));
		await flush();
		out.j18_runningHolds = s.I.isPollSuspended(s.table, ctx, viewDef) === true;
		out.j18_runningLabel = indicator.textContent;
		out.j18_runningTimerDrawCancelled = land(true) === false;
		out.j18_userReloadNotHeld = land(false) === true;
		// A second, job-less expanded row does not lift the hold; collapsing the running row's panel does.
		s.expand(s.r2);
		out.j18_otherPanelStillHeld = s.I.isPollSuspended(s.table, ctx, viewDef) === true;
		collapse(s.r2);
		collapse(s.r1);
		out.j18_collapsedReleases = s.I.isPollSuspended(s.table, ctx, viewDef) === false && land(true) === true;
		s.expand(s.r1);
		out.j18_reexpandHolds = s.I.isPollSuspended(s.table, ctx, viewDef) === true && land(true) === false;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'ok' }));
		await flush();
		out.j18_endReleases = s.I.isPollSuspended(s.table, ctx, viewDef) === false;
		out.j18_endState = indicator.dataset.state;
		out.j18_endDraws = land(true) === true;
	}

	// --- j19: a row action's stream on a row whose panel run is still pending adopts nothing of the run's -----------
	{
		let release;
		const s = setup({ routes: { '/species/r1/restock': function () { return new Promise(function (res) { release = res; }); } } });
		const ctx = s.table.__juneauCtx;
		const panel = s.expand(s.r1);
		const h = s.NS.jobs.run({ el: panel._btn, trigger: panel._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		let adopted = 'none';
		s.NS.jobs.pending(s.r1, 'restock-species').then(function (ref) { adopted = ref ? ref.jobId : null; });
		await flush();
		// The row action's job pointer arrives while the panel's submit is in flight.
		s.I.startJobStream({ jobId: 'j-2', streamUrl: '/s/j-2', cancelUrl: '/c/j-2' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, ctx);
		await flush();
		out.j19_notAdopted = adopted === 'none';
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'regroomed' }));
		await flush();
		out.j19_stillPending = s.NS.jobs.pending(s.r1, 'restock-species') != null;
		out.j19_stillHolds = s.I.hasOpenRunningJobPanel(s.table) === true;
		out.j19_triggerStillDisabled = panel._btn.disabled === true;
		release(jsonResponse(STARTED, { status: 202 }));
		await flush();
		out.j19_adopted = adopted;
		out.j19_panelStreams = s.sources.length === 2 && s.sources[1].url === '/s/j-1?lastEventId=0';
		out.j19_active = s.NS.jobs.active(panel._btn, 'restock-species')?.jobId;
		out.j19_marker = s.r1.dataset.juneauJob === '1';
		s.sources[1].fire('result', JSON.stringify({ outcome: 'success', message: 'restocked' }));
		out.j19_state = (await h.done).state;
	}

	// --- j20: a row action's job marks the row and blocks a run, but holds no poll even with the panel open -------------
	{
		const s = setup();
		const ctx = s.table.__juneauCtx;
		s.expand(s.r1);
		s.I.startJobStream({ jobId: 'j-3', streamUrl: '/s/j-3', cancelUrl: '/c/j-3' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, ctx);
		out.j20_marker = s.r1.dataset.juneauJob === '1';
		out.j20_noHold = s.I.hasOpenRunningJobPanel(s.table) === false;
		const refused = await s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k' }).done;
		out.j20_runRefused = refused.error?.reason;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'regroomed' }));
		await flush();
		out.j20_released = s.NS.jobs.active(s.r1._btn) === null && s.r1.dataset.juneauJob === undefined;
	}

	// --- j21: an ordinary row action is refused on a row whose job is pending or running, and runs once it ends --------
	{
		const s = setup();
		const pings = function () { return s.calls.filter(function (c) { return c.url === '/species/r1/ping'; }).length; };
		const banner = function () { return s.r1.querySelector('.juneau-view-action-refusal'); };
		const panel = s.expand(s.r1);
		const h = s.NS.jobs.run({ el: panel._btn, trigger: panel._btn, endpoint: RESTOCK, keyUrl: KEY_URL, action: 'restock-species', envelope: 'action' });
		await s.NS.rowActions.run(s.r1._btn, 'ping');   // the run is still pending (key fetch / submit)
		await flush();
		out.j21_pendingSentNothing = pings() === 0;
		out.j21_pendingBanner = banner()?.textContent.includes('a job is already running on this row');
		out.j21_otherRowUnaffected = (await s.NS.rowActions.run(s.r2._btn, 'ping'), s.calls.some(function (c) { return c.url === '/species/r2/ping'; }));
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'ok' }));
		await h.done;
		await s.NS.rowActions.run(s.r1._btn, 'ping');
		await flush();
		out.j21_afterEndSent = pings() === 1;
		// A row action's own job also blocks a second one.
		s.I.startJobStream({ jobId: 'j-4', streamUrl: '/s/j-4', cancelUrl: '/c/j-4' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, s.table.__juneauCtx);
		await s.NS.rowActions.run(s.r1._btn, 'ping');
		await flush();
		out.j21_rowJobSentNothing = pings() === 1;
	}

	// --- k01: jobs.cancel on a running row: one POST to the start-time URL with the row table's token ------------------
	{
		const s = setup();
		s.table.dataset.juneauCsrfHeader = 'X-Alt';
		const panel = s.expand(s.r1);
		const h = s.NS.jobs.run({ el: panel._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const sent = await s.NS.jobs.cancel(panel._btn, 'restock-species');
		const posts = s.calls.filter(function (c) { return c.url === '/c/j-1'; });
		out.k01_resolved = sent;
		out.k01_posts = posts.length;
		out.k01_method = posts[0]?.method;
		out.k01_header = posts[0]?.headers['X-Alt'];
		out.k01_noDefaultHeader = posts[0] && posts[0].headers['X-Csrf-Token'] === undefined;
		out.k01_body = posts[0]?.body;
		out.k01_stillRegistered = s.NS.jobs.active(panel._btn) != null && s.r1.dataset.juneauJob === '1';
		out.k01_nullActionMeansAny = (await s.NS.jobs.cancel(s.r1)) === true && s.calls.filter(function (c) { return c.url === '/c/j-1'; }).length === 2;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		out.k01_state = (await h.done).state;
		out.k01_afterRelease = (await s.NS.jobs.cancel(s.r1)) === false;
	}

	// --- k02: the cancel URL is a start-time snapshot; rewriting the shared ref redirects nothing ------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		s.NS.jobs.active(s.r1).cancelUrl = '/elsewhere/cancel';
		h.job.cancelUrl = '/elsewhere/cancel';
		out.k02_jobs = await s.NS.jobs.cancel(s.r1);
		out.k02_handle = await h.cancel();
		const urls = s.calls.filter(function (c) { return c.url.indexOf('/c/') === 0 || c.url.indexOf('/elsewhere') === 0; }).map(function (c) { return c.url; });
		out.k02_urls = urls.join(',');
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await h.done;
		// A job whose 202 had no cancelUrl, later given one through the ref, still cancels nothing.
		const t = setup({ routes: { '/species/r1/restock': function () { return Promise.resolve(jsonResponse({ jobId: 'j-9', streamUrl: '/s/j-9' }, { status: 202 })); } } });
		const h2 = t.NS.jobs.run({ el: t.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		t.NS.jobs.active(t.r1).cancelUrl = '/c/j-1';
		const before = t.calls.length;
		out.k02_noUrl = await t.NS.jobs.cancel(t.r1);
		out.k02_noUrlFetches = t.calls.length - before;
		t.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await h2.done;
	}

	// --- k03: element resolution and the no-I/O false cases ---------------------------------------------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const n0 = s.calls.length;
		const fetches = function () { return s.calls.length - n0; };
		out.k03_rowB = await s.NS.jobs.cancel(s.r2._btn);
		out.k03_rowBFetches = fetches();
		out.k03_actionMismatch = await s.NS.jobs.cancel(s.r1, 'other');
		out.k03_nonElement = (await s.NS.jobs.cancel(null)) === false && (await s.NS.jobs.cancel({})) === false;
		out.k03_noRowFetches = fetches();
		const panel = s.expand(s.r1);
		out.k03_panelElement = await s.NS.jobs.cancel(panel._btn, 'restock-species');
		out.k03_panelFetches = fetches();
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await h.done;
		// Pending: the submit is held.
		let release;
		const p = setup({ routes: { '/species/r1/restock': function () { return new Promise(function (res) { release = res; }); } } });
		const hp = p.NS.jobs.run({ el: p.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const pn = p.calls.length;
		out.k03_pending = await p.NS.jobs.cancel(p.r1);
		out.k03_pendingFetches = p.calls.length - pn;
		release(jsonResponse(STARTED, { status: 202 }));
		await flush();
		p.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await hp.done;
		// Blank token / untrusted URL / no cancel URL.
		const b = setup();
		const hb = b.NS.jobs.run({ el: b.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		b.table.dataset.juneauCsrf = '  ';
		const bn = b.calls.length;
		out.k03_blankToken = await b.NS.jobs.cancel(b.r1);
		out.k03_blankFetches = b.calls.length - bn;
		b.table.dataset.juneauCsrf = 'tok-1';
		b.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await hb.done;
		const u = setup({ routes: { '/species/r1/restock': function () { return Promise.resolve(jsonResponse({ jobId: 'j-9', streamUrl: '/s/j-9', cancelUrl: 'https://evil.test/c' }, { status: 202 })); } } });
		const hu = u.NS.jobs.run({ el: u.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const un = u.calls.length;
		out.k03_untrusted = await u.NS.jobs.cancel(u.r1);
		out.k03_untrustedFetches = u.calls.length - un;
		u.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await hu.done;
	}

	// --- k04: a stale entry (released, or replaced by a later job on the row) cancels nothing -----------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const oldEntry = s.I.rowJobEntryFor(s.table, 'r1');
		out.k04_captured = oldEntry?.cancelUrl === '/c/j-1';
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'ok' }));
		await h.done;
		const h2 = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k2', action: 'restock-species', envelope: 'action' });
		await flush();
		const n0 = s.calls.length;
		out.k04_stale = await s.I.cancelRowJob(oldEntry, s.table, s.I.jobHostFor(s.r1, null), { id: 'x' });
		out.k04_staleFetches = s.calls.length - n0;
		out.k04_newJobLive = s.NS.jobs.active(s.r1) != null;
		s.sources[1].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await h2.done;
	}

	// --- k05: failures resolve false and the next call sends again; in-flight requests are shared; success is not cached ----
	{
		let mode = 'fail';
		let gate = null;
		const s = setup({ routes: { '/c/j-1': function () {
			if (mode === 'fail') { return Promise.resolve(jsonResponse({}, { status: 500 })); }
			if (mode === 'net') { return Promise.reject(new TypeError('offline')); }
			if (mode === 'html') { return Promise.resolve(jsonResponse('<html>login</html>', { headers: { 'content-type': 'text/html' } })); }
			return new Promise(function (res) { gate = res; });
		} } });
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		const posts = function () { return s.calls.filter(function (c) { return c.url === '/c/j-1'; }).length; };
		out.k05_nonOk = await s.NS.jobs.cancel(s.r1);
		mode = 'net';
		out.k05_network = await s.NS.jobs.cancel(s.r1);
		out.k05_sentEachTime = posts() === 2;
		mode = 'gate';
		const a = s.NS.jobs.cancel(s.r1);
		const b = s.NS.jobs.cancel(s.r1);
		const c = h.cancel();
		await flush();
		out.k05_inflightPosts = posts() - 2;
		out.k05_clearedWhileInFlight = s.I.rowJobEntryFor(s.table, 'r1').cancelling != null;
		gate(jsonResponse({}));
		const r = await Promise.all([a, b, c]);
		out.k05_sharedResult = r.join(',');
		out.k05_clearedAfter = s.I.rowJobEntryFor(s.table, 'r1').cancelling === null;
		mode = 'html';
		out.k05_secondPostAfterTrue = (await s.NS.jobs.cancel(s.r1)) === true && posts() === 4;
		out.k05_htmlTrueCleared = s.I.rowJobEntryFor(s.table, 'r1').cancelling === null;
		out.k05_thenAgain = (await s.NS.jobs.cancel(s.r1)) === true && posts() === 5;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await h.done;
	}

	// --- k06: a progress button and jobs.cancel share one request; a row-less job keeps the direct path --------------------
	{
		let gate = null;
		const s = setup({ routes: { '/c/j-3': function () { return new Promise(function (res) { gate = res; }); } } });
		const ctx = s.table.__juneauCtx;
		s.I.startJobStream({ jobId: 'j-3', streamUrl: '/s/j-3', cancelUrl: '/c/j-3' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, ctx);
		const btn = s.r1.querySelector('.juneau-view-job-cancel');
		out.k06_button = btn != null;
		btn.dispatch('click', {});
		const viaApi = s.NS.jobs.cancel(s.r1);
		await flush();
		out.k06_posts = s.calls.filter(function (c) { return c.url === '/c/j-3'; }).length;
		gate(jsonResponse({}));
		out.k06_apiResult = await viaApi;
		out.k06_rowActionCancelled = true;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await flush();
		// A row-less job (no row): no registry entry; handle.cancel() posts directly.
		const t = setup({ routes: { '/x': function () { return Promise.resolve(jsonResponse({ jobId: 'j-5', streamUrl: '/s/j-5', cancelUrl: '/c/j-5' }, { status: 202 })); },
			'/c/j-5': function () { return Promise.resolve(jsonResponse({})); } } });
		const holder = t.env.el('div');
		holder.dataset.juneauCsrf = 'tok-1';
		const hb = t.env.el('button');
		holder.appendChild(hb);
		t.env.body.appendChild(holder);
		const rh = t.NS.jobs.run({ el: hb, endpoint: '/x', idempotencyKey: 'k' });
		await flush();
		out.k06_rowlessCancel = await rh.cancel();
		out.k06_rowlessPosts = t.calls.filter(function (c) { return c.url === '/c/j-5'; }).length;
	}

	// --- k07: paint: "none" never paints a failed cancel; "row" and the progress button still do -------------------------------
	{
		const failing = { '/c/j-1': function () { return Promise.resolve(jsonResponse({}, { status: 500 })); } };
		const refusal = function (s) { return s.r1.querySelector('.juneau-view-action-refusal') != null; };
		const run = async function (paint) {
			const s = setup({ routes: failing });
			const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action', paint: paint });
			await flush();
			return { s: s, h: h };
		};
		const a = await run('none');
		out.k07_noneViaApi = (await a.s.NS.jobs.cancel(a.s.r1)) === false && !refusal(a.s);
		out.k07_noneViaHandle = (await a.h.cancel()) === false && !refusal(a.s);
		const b = await run('row');
		out.k07_rowViaApi = (await b.s.NS.jobs.cancel(b.s.r1)) === false && refusal(b.s);
		const c = await run('row');
		out.k07_rowViaHandle = (await c.h.cancel()) === false && refusal(c.s);
		// The row-less direct path honors paint too.
		const s = setup({ routes: { '/c/j-3': function () { return Promise.resolve(jsonResponse({}, { status: 500 })); } } });
		s.I.startJobStream({ jobId: 'j-3', streamUrl: '/s/j-3', cancelUrl: '/c/j-3' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, s.table.__juneauCtx);
		s.r1.querySelector('.juneau-view-job-cancel').dispatch('click', {});
		await flush();
		out.k07_progressButtonStillPaints = refusal(s);
		// Four-argument cancelJob still paints.
		const d = setup({ routes: { '/c/j-3': function () { return Promise.resolve(jsonResponse({}, { status: 500 })); } } });
		await d.I.cancelJob({ cancelUrl: '/c/j-3' }, d.table, d.r1, { id: 'regroom' });
		out.k07_fourArgPaints = refusal(d);
		const e = setup();
		await e.I.cancelJob({ cancelUrl: 'https://evil.test/c' }, e.table, e.r1, { id: 'regroom' });
		out.k07_fourArgRefusalPaints = refusal(e);
		const f = setup();
		await f.I.cancelJob({ cancelUrl: 'https://evil.test/c' }, f.table, f.r1, { id: 'regroom' }, true);
		out.k07_quietSkips = !refusal(f);
	}

	// --- k08: release timing after an accepted cancel ---------------------------------------------------------------------
	{
		const s = setup();
		const h = s.NS.jobs.run({ el: s.r1._btn, endpoint: RESTOCK, idempotencyKey: 'k', action: 'restock-species', envelope: 'action' });
		await flush();
		out.k08_accepted = await s.NS.jobs.cancel(s.r1);
		out.k08_stillActive = s.NS.jobs.active(s.r1) != null && s.r1.dataset.juneauJob === '1';
		await s.NS.rowActions.run(s.r1._btn, 'ping');
		await flush();
		out.k08_pingRefused = s.calls.filter(function (c) { return c.url === '/species/r1/ping'; }).length === 0;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		const end = await h.done;
		out.k08_state = end.state;
		out.k08_released = s.NS.jobs.active(s.r1) === null && s.r1.dataset.juneauJob === undefined;
		await s.NS.rowActions.run(s.r1._btn, 'ping');
		await flush();
		out.k08_pingRuns = s.calls.filter(function (c) { return c.url === '/species/r1/ping'; }).length === 1;
	}

	// --- k09: jobs.cancel cancels a row action's entry through the URL its progress button uses --------------------------------
	{
		const s = setup({ routes: { '/c/j-3': function () { return Promise.resolve(jsonResponse({})); } } });
		s.I.startJobStream({ jobId: 'j-3', streamUrl: '/s/j-3', cancelUrl: '/c/j-3' }, { id: 'regroom', label: 'Regroom' }, s.table, s.r1, s.table.__juneauCtx);
		out.k09_sent = await s.NS.jobs.cancel(s.r1, 'regroom');
		out.k09_url = s.calls.filter(function (c) { return c.url === '/c/j-3'; }).length;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled' }));
		await flush();
	}

	// --- u01: jobHistoryUrl sets lastEventId=0, replacing any existing one ---------------------------------------------
	{
		const s = setup();
		out.u01_plain = s.I.jobHistoryUrl('/s/j-1');
		out.u01_query = s.I.jobHistoryUrl('/s/j-1?a=1&lastEventId=7#f');
		out.u01_stateOf = [
			s.I.jobStateOf({ outcome: 'success' }), s.I.jobStateOf({ outcome: 'failure' }), s.I.jobStateOf({ outcome: 'refusal' }),
			s.I.jobStateOf({ outcome: 'cancelled' }), s.I.jobStateOf(null), s.I.jobStateOf({ outcome: 'success', contractVersion: '9' })
		].join(',');
	}

	console.error = realError;
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error = realError; console.error(e?.stack || e); process.exit(1); });
