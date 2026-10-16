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
 * actions-run.cjs - always-on Node harness for JuneauViews.actions.run, the hook a chart card uses to run a
 * RowAction for a clicked point.  Every case loads a fresh window, builds a card host (optionally inside a CSRF
 * ancestor), runs an action and records what was sent, what was painted and what the returned promise resolved with.
 * Every assertion lives in the Java test; this prints ONE JSON object.
 *
 *   Usage: node actions-run.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node actions-run.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const out = {};
const FORM_URL = '/x/form';
const CHILD_FORM = '/x/child-form';

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 10) { return r(); } setTimeout(tick, 0); })(); });
}

/** A fresh window, a card host (inside a CSRF-carrying ancestor unless `csrf === false`), and a recording fetch. */
function setup(o) {
	const opts = o || {};
	const env = makeEnv();
	const sources = [];
	if (opts.es) {
		const FakeES = function (url) { this.url = url; this.l = {}; this.closed = false; sources.push(this); };
		FakeES.prototype.addEventListener = function (t, f) { (this.l[t] = this.l[t] || []).push(f); };
		FakeES.prototype.close = function () { this.closed = true; };
		FakeES.prototype.fire = function (t, d) { (this.l[t] || []).forEach(function (f) { f({ data: d }); }); };
		env.EventSource = FakeES;
	}
	const { NS, I } = loadViews(rendersJsPath, viewsJsPath, env);
	const wrapper = env.el('div');
	if (opts.csrf !== false) wrapper.dataset.juneauCsrf = opts.csrf || 'tok-1';
	const host = env.el('div');
	wrapper.appendChild(host);
	env.body.appendChild(wrapper);
	const calls = [];
	const respond = opts.respond || function () { return Promise.resolve(jsonResponse({ outcome: 'success', message: 'done' })); };
	env.setFetch(function (url, init) {
		const method = (init && init.method) || 'GET';
		calls.push({ url: String(url), method: method, headers: (init && init.headers) || {}, body: init && init.body ? JSON.parse(init.body) : null });
		return respond(String(url), method, init);
	});
	return { env: env, NS: NS, I: I, host: host, wrapper: wrapper, calls: calls, sources: sources };
}

const writes = (s) => s.calls.filter((c) => c.method !== 'GET');
const q = (s, sel) => s.env.body.querySelector(sel);
const banner = (s) => q(s, '[data-testid="chart-action-banner"]');
const bannerText = (s) => { const b = banner(s); return b ? b.textContent : null; };
const alertEl = (s) => { const b = banner(s); return b ? b.querySelector('[role="alert"]') : null; };

const POST = { id: 'ack', label: 'Acknowledge', endpoint: '/x/{id}/ack', method: 'POST' };
const ROW = { id: 'INC-7', region: 'east' };

function dialogPayload(extra) {
	return Object.assign({ contractVersion: '2', title: 'Step', form: { contractVersion: '2', fields: [{ name: 'note', type: 'text', label: 'Note' }] } }, extra || {});
}
const confirmBtn = (s) => q(s, '[data-testid="dialog-confirm"]') || q(s, '.juneau-view-dialog-confirm');

(async function main() {

	// --- a01: a direct POST substitutes the row, sends the ancestor's CSRF token, and resolves with the outcome ---
	{
		const s = setup();
		const r = await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.a01_calls = writes(s).length;
		out.a01_url = writes(s)[0] && writes(s)[0].url;
		out.a01_method = writes(s)[0] && writes(s)[0].method;
		out.a01_csrf = JSON.stringify(writes(s)[0] && writes(s)[0].headers).indexOf('tok-1') >= 0;
		out.a01_outcome = r.outcome;
		out.a01_message = r.message;
	}

	// --- a02: no CSRF ancestor falls back to the body; a missing token everywhere fails closed -------------------
	{
		const s = setup({ csrf: false });
		s.env.body.dataset.juneauCsrf = 'body-tok';
		await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.a02_bodyFallbackSent = writes(s).length === 1 && JSON.stringify(writes(s)[0].headers).indexOf('body-tok') >= 0;
		const t = setup({ csrf: false });
		const r = await t.NS.actions.run(POST, { host: t.host, row: ROW });
		out.a02_missingSent = writes(t).length;
		out.a02_missingOutcome = r.outcome;
	}

	// --- a03: refusals send nothing and paint a role=alert in the card banner -----------------------------------
	{
		const cases = {
			emptySub: [{ id: 'x', label: 'X', endpoint: '/x/{id}', method: 'POST' }, { id: '' }],
			safeMethod: [{ id: 'x', label: 'X', endpoint: '/x', method: 'GET' }, ROW],
			noEndpoint: [{ id: 'x', label: 'X', method: 'POST' }, ROW]
		};
		for (const k of Object.keys(cases)) {
			const s = setup();
			const r = await s.NS.actions.run(cases[k][0], { host: s.host, row: cases[k][1] });
			out['a03_' + k] = { sent: writes(s).length, outcome: r.outcome, alert: alertEl(s) != null || (bannerText(s) || '').length > 0 };
		}
	}

	// --- a04: transport refusals ---------------------------------------------------------------------------------
	for (const status of [403, 415, 421]) {
		const s = setup({ respond: () => Promise.resolve(jsonResponse({}, { status: status })) });
		const r = await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out['a04_' + status] = { outcome: r.outcome, text: bannerText(s) };
	}

	// --- a05: ActionResult failure and error ---------------------------------------------------------------------
	{
		const s = setup({ respond: () => Promise.resolve(jsonResponse({ outcome: 'failure', message: 'nope' })) });
		const r = await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.a05_failure = { outcome: r.outcome, message: r.message, text: bannerText(s) };
	}

	// --- a06: onSuccess=redraw calls onRedraw; no onSuccess does not ---------------------------------------------
	{
		let n = 0;
		const s = setup();
		await s.NS.actions.run(Object.assign({ onSuccess: 'redraw' }, POST), { host: s.host, row: ROW, onRedraw: () => { n++; } });
		out.a06_redraws = n;
		let m = 0;
		const t = setup();
		await t.NS.actions.run(POST, { host: t.host, row: ROW, onRedraw: () => { m++; } });
		out.a06_plainRedraws = m;
	}

	// --- a07: the banner is rebuilt when the host's contents are wiped -------------------------------------------
	{
		const s = setup();
		await s.NS.actions.run(POST, { host: s.host, row: ROW });
		const first = banner(s);
		s.host.removeChild(first);
		await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.a07_rebuilt = banner(s) != null && banner(s) !== first && banner(s).parentNode === s.host;
		out.a07_oneBanner = s.host.querySelectorAll('[data-testid="chart-action-banner"]').length;
	}

	// --- a08: rowDataForTr serves the override with no row element, and still reads a real row -------------------
	{
		const s = setup();
		out.a08_override = JSON.stringify(s.I.rowDataForTr({ _rowOverride: { a: 1 } }, null));
		out.a08_noRow = s.I.rowDataForTr({}, null) == null;
	}

	// --- a09: extra body fields travel with the request ----------------------------------------------------------
	{
		const s = setup();
		await s.NS.actions.run(POST, { host: s.host, row: ROW, extra: { idempotencyKey: 'k-1' } });
		out.a09_extra = writes(s)[0] && writes(s)[0].body && writes(s)[0].body.idempotencyKey;
	}

	// --- b: rule gates -------------------------------------------------------------------------------------------
	{
		const vis = Object.assign({ visibleWhen: [{ field: 'region', op: 'eq', value: 'west' }] }, POST);
		const s = setup();
		const r = await s.NS.actions.run(vis, { host: s.host, row: ROW });
		out.b01 = { sent: writes(s).length, outcome: r.outcome, text: bannerText(s) };
		const en = Object.assign({ enabledWhen: [{ field: 'region', op: 'eq', value: 'west', reason: 'West only' }] }, POST);
		const t = setup();
		const r2 = await t.NS.actions.run(en, { host: t.host, row: ROW });
		out.b02 = { sent: writes(t).length, outcome: r2.outcome, text: bannerText(t) };
		const en2 = Object.assign({ enabledWhen: [{ field: 'region', op: 'eq', value: 'east', reason: 'East only' }] }, POST);
		const u = setup();
		const r3 = await u.NS.actions.run(en2, { host: u.host, row: ROW });
		out.b03 = { sent: writes(u).length, outcome: r3.outcome };
		// A single rule object (not an array) is still a gate: it must fail closed, never read as "no gate".
		const visObj = Object.assign({ visibleWhen: { field: 'region', op: 'eq', value: 'west' } }, POST);
		const v = setup();
		const r4 = await v.NS.actions.run(visObj, { host: v.host, row: ROW });
		out.b04 = { sent: writes(v).length, outcome: r4.outcome, text: bannerText(v) };
		const enObj = Object.assign({ enabledWhen: { field: 'region', op: 'eq', value: 'west', reason: 'West only' } }, POST);
		const w = setup();
		const r5 = await w.NS.actions.run(enObj, { host: w.host, row: ROW });
		out.b05 = { sent: writes(w).length, outcome: r5.outcome, text: bannerText(w) };
	}

	// --- c: dialog and confirm flows -----------------------------------------------------------------------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const respond = (url, method) => {
			if (method === 'GET' && url === FORM_URL) return Promise.resolve(jsonResponse(dialogPayload()));
			return Promise.resolve(jsonResponse({ outcome: 'success', message: 'saved' }));
		};
		// c01: the promise stays pending while the dialog is open, then resolves after the submit
		{
			const s = setup({ respond: respond });
			let done = null;
			const p = s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => { done = r; });
			await flush();
			out.c01_pendingWhileOpen = done === null && s.I.dialogLayerCount() === 1;
			const b = confirmBtn(s);
			out.c01_hasConfirm = b != null;
			if (b) b.dispatch('click');
			await flush(); await p;
			out.c01_outcome = done && done.outcome;
			out.c01_closed = s.I.dialogLayerCount() === 0;
		}
		// c02: dismissing the dialog resolves cancelled and sends nothing
		{
			const s = setup({ respond: respond });
			let done = null;
			const p = s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => { done = r; });
			await flush();
			s.env.dispatchDocument('keydown', { key: 'Escape' });
			await flush(); await p;
			out.c02_outcome = done && done.outcome;
			out.c02_writes = writes(s).length;
		}
		// c03: a second run while one is open is refused and leaves the first intact
		{
			const s = setup({ respond: respond });
			let done = null;
			const p = s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => { done = r; });
			await flush();
			const second = await s.NS.actions.run(POST, { host: s.host, row: ROW });
			out.c03_secondOutcome = second.outcome;
			out.c03_firstStillOpen = done === null && s.I.dialogLayerCount() === 1;
			s.env.dispatchDocument('keydown', { key: 'Escape' });
			await flush(); await p;
		}
		// c03b: the refused second run must not swap the live run's row: the first dialog still sends the first row's id
		{
			const TOK = { id: 'edit', label: 'Edit', endpoint: '/x/{id}/edit', method: 'POST', present: 'dialog', form: FORM_URL };
			const s = setup({ respond: respond });
			const p = s.NS.actions.run(TOK, { host: s.host, row: { id: 'FIRST' } });
			await flush();
			const second = await s.NS.actions.run(POST, { host: s.host, row: { id: 'SECOND' } });
			out.c03b_secondOutcome = second.outcome;
			const b = confirmBtn(s);
			if (b) b.dispatch('click');
			await flush(); await p;
			out.c03b_url = writes(s)[0] && writes(s)[0].url;
		}
		// c04: a declined confirmRenderer resolves cancelled, sends nothing
		{
			const s = setup();
			s.NS.rowActions.registerConfirmRenderer('no', () => Promise.resolve(false));
			const act = { id: 'del', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE', present: 'dialog', confirm: 'Delete {id}?', confirmRenderer: 'no' };
			const r = await s.NS.actions.run(act, { host: s.host, row: ROW });
			out.c04_outcome = r.outcome;
			out.c04_writes = writes(s).length;
		}
		// c04b: an accepted confirmRenderer sends exactly one request
		{
			const s = setup();
			s.NS.rowActions.registerConfirmRenderer('yes', () => Promise.resolve(true));
			const act = { id: 'del', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE', present: 'dialog', confirm: 'Delete {id}?', confirmRenderer: 'yes' };
			const r = await s.NS.actions.run(act, { host: s.host, row: ROW });
			out.c04b_outcome = r.outcome;
			out.c04b_writes = writes(s).length;
		}
		// c06: a form GET that fails resolves with a refusal
		{
			const s = setup({ respond: () => Promise.resolve(jsonResponse({}, { status: 500 })) });
			const r = await s.NS.actions.run(DLG, { host: s.host, row: ROW });
			out.c06_outcome = r.outcome;
		}
	}

	// --- e: release ---------------------------------------------------------------------------------------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const s = setup({ respond: (url, method) => method === 'GET' ? Promise.resolve(jsonResponse(dialogPayload())) : Promise.resolve(jsonResponse({ outcome: 'success' })) });
		let done = null;
		s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => { done = r; });
		await flush();
		out.e01_openBefore = s.I.dialogLayerCount();
		s.NS.actions.release(s.host);
		await flush();
		out.e01_openAfter = s.I.dialogLayerCount();
		out.e01_outcome = done && done.outcome;
		out.e01_bannerGone = banner(s) === null;
		// releasing a host that never ran is a no-op
		const t = setup();
		let threw = false;
		try { t.NS.actions.release(t.host); t.NS.actions.release(null); } catch (e) { threw = true; }
		out.e02_noop = !threw;
		// the hook is frozen
		out.e03_frozen = Object.isFrozen(t.NS.actions);
		// bad arguments resolve a refusal rather than throwing
		const bad = await t.NS.actions.run(null, { host: t.host });
		const bad2 = await t.NS.actions.run(POST, {});
		out.e04_bad = bad.outcome + ',' + bad2.outcome;
	}

	// --- f: banner is cleared at the start of the next run ------------------------------------------------------
	{
		let first = true;
		const s = setup({ respond: () => Promise.resolve(jsonResponse(first ? { outcome: 'failure', message: 'first-fail' } : { outcome: 'success', message: 'second-ok' })) });
		await s.NS.actions.run(POST, { host: s.host, row: ROW });
		first = false;
		await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.f01_noStaleText = (bannerText(s) || '').indexOf('first-fail') < 0;
		out.f01_hasNew = (bannerText(s) || '').indexOf('second-ok') >= 0;
	}

	// --- g: confirm-only prompt, terminal outcomes, catalog use, once-only warnings, release isolation -----------
	{
		// g01: a plain confirm text (no renderer, no dialog) opens a prompt first; nothing is sent until it is confirmed
		const s = setup();
		const act = { id: 'del', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE', confirm: 'Delete {id}?' };
		let done = null;
		const p = s.NS.actions.run(act, { host: s.host, row: ROW }).then((r) => { done = r; });
		await flush();
		out.g01_writesBefore = writes(s).length;
		out.g01_promptOpen = s.I.dialogLayerCount() === 1;
		out.g01_pending = done === null;
		const b = confirmBtn(s);
		if (b) b.dispatch('click');
		await flush(); await p;
		out.g01_writesAfter = writes(s).length;
		out.g01_outcome = done && done.outcome;
	}
	{
		// g02: a bare 2xx (no typed body) resolves success
		const s = setup({ respond: () => Promise.resolve(jsonResponse('')) });
		const r = await s.NS.actions.run(POST, { host: s.host, row: ROW });
		out.g02_outcome = r.outcome;
		// g03: a rejected fetch resolves unknown, not cancelled
		const t = setup({ respond: () => Promise.reject(new Error('net')) });
		const r2 = await t.NS.actions.run(POST, { host: t.host, row: ROW });
		out.g03_outcome = r2.outcome;
		// g03b: from inside a dialog the lost response is a held terminal notice; closing it resolves unknown
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const u = setup({ respond: (url, method) => method === 'GET' ? Promise.resolve(jsonResponse(dialogPayload({ keepOpenOnSubmit: true }))) : Promise.reject(new Error('net')) });
		let done = null;
		const p = u.NS.actions.run(DLG, { host: u.host, row: ROW }).then((x) => { done = x; });
		await flush();
		const b = confirmBtn(u);
		if (b) b.dispatch('click');
		await flush();
		out.g03b_pendingWhileHeld = done === null;
		u.env.dispatchDocument('keydown', { key: 'Escape' });
		await flush();
		out.g03b_outcome = done && done.outcome;
	}
	{
		// g04: a catalog entry is usable from inside the dialog the action opened; without it the button is disabled
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const OTHER = { id: 'other', label: 'Other', endpoint: '/x/other', method: 'POST', present: 'dialog', form: FORM_URL };
		const respond = (url, method) => method === 'GET'
			? Promise.resolve(jsonResponse(dialogPayload({ form: { contractVersion: '2', fields: [{ name: 'go', type: 'action', actionId: 'other', label: 'Go' }] } })))
			: Promise.resolve(jsonResponse({ outcome: 'success' }));
		const s = setup({ respond: respond });
		s.NS.actions.run(DLG, { host: s.host, row: ROW, catalog: [OTHER] });
		await flush();
		const btn = q(s, '.juneau-view-dialog-form-action');
		out.g04_buttonFound = btn != null;
		out.g04_enabled = btn != null && btn.disabled !== true && !(btn.dataset && btn.dataset.juneauActionMissing);
		const t = setup({ respond: respond });
		t.NS.actions.run(DLG, { host: t.host, row: ROW });
		await flush();
		const b2 = q(t, '.juneau-view-dialog-form-action');
		out.g04_missingDisabled = b2 != null && (b2.disabled === true || !!(b2.dataset && b2.dataset.juneauActionMissing));
	}
	{
		// g05: a duplicate catalog id warns once per host and the first entry is the one used
		const warns = [];
		const orig = console.warn;
		console.warn = function (m) { warns.push(String(m)); };
		try {
			const s = setup();
			const cat = [{ id: 'dup', label: 'First', endpoint: '/x/a', method: 'POST' }, { id: 'dup', label: 'Second', endpoint: '/x/b', method: 'POST' }];
			await s.NS.actions.run(POST, { host: s.host, row: ROW, catalog: cat });
			await s.NS.actions.run(POST, { host: s.host, row: ROW, catalog: cat });
			out.g05_warnCount = warns.filter((w) => w.indexOf('more than once') >= 0).length;
		} finally { console.warn = orig; }
	}
	{
		// g06: releasing an idle host leaves another host's open dialog alone
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const s = setup({ respond: (url, method) => method === 'GET' ? Promise.resolve(jsonResponse(dialogPayload())) : Promise.resolve(jsonResponse({ outcome: 'success' })) });
		const other = s.env.el('div');
		s.wrapper.appendChild(other);
		await s.NS.actions.run(POST, { host: other, row: ROW });
		s.NS.actions.run(DLG, { host: s.host, row: ROW });
		await flush();
		const foreign = s.env.el('div');   // a table's dialog layer, which this host never owned
		foreign.className = 'juneau-view-dialog-backdrop';
		foreign.dataset.foreign = '1';
		s.env.body.appendChild(foreign);
		out.g06_openBefore = q(s, '.juneau-view-dialog-backdrop') != null;
		s.NS.actions.release(other);
		await flush();
		out.g06_openAfter = q(s, '.juneau-view-dialog-backdrop') != null;
		out.g06_foreignKept = q(s, '[data-foreign="1"]') != null;
	}

	// --- g07: with a duplicated catalog id, the button a dialog paints opens the FIRST entry --------------------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const A = { id: 'dup', label: 'A', endpoint: '/x/a', method: 'POST', present: 'dialog', form: '/x/a-form' };
		const B = { id: 'dup', label: 'B', endpoint: '/x/b', method: 'POST', present: 'dialog', form: '/x/b-form' };
		const respond = (url, method) => {
			if (method === 'GET' && url === FORM_URL) return Promise.resolve(jsonResponse(dialogPayload({ form: { contractVersion: '2', fields: [{ name: 'go', type: 'action', actionId: 'dup', label: 'Go' }] } })));
			if (method === 'GET') return Promise.resolve(jsonResponse(dialogPayload()));
			return Promise.resolve(jsonResponse({ outcome: 'success' }));
		};
		const s = setup({ respond: respond });
		const w = console.warn; console.warn = function () { /* expected duplicate warning */ };
		try {
			s.NS.actions.run(DLG, { host: s.host, row: ROW, catalog: [A, B] });
			await flush();
			const btn = q(s, '.juneau-view-dialog-form-action');
			if (btn) btn.dispatch('click');
			await flush();
		} finally { console.warn = w; }
		out.g07_aForms = s.calls.filter((c) => c.url === '/x/a-form').length;
		out.g07_bForms = s.calls.filter((c) => c.url === '/x/b-form').length;
	}

	// --- h: a response that lands after release() paints, opens and streams nothing -------------------------------
	{
		const deferred = () => { let res; const pr = new Promise((r) => { res = r; }); return { pr: pr, res: res }; };
		const leaked = (s) => s.env.body.querySelector('[data-testid="ribbon-banner-host"]') != null
			|| s.env.body.querySelector('[data-testid="chart-action-banner"]') != null;
		// h01: a job pointer arriving after release opens no EventSource
		{
			const d = deferred();
			const s = setup({ es: true, respond: () => d.pr });
			s.NS.actions.run(POST, { host: s.host, row: ROW });
			await flush();
			s.NS.actions.release(s.host);
			d.res(jsonResponse({ jobId: 'j-9', streamUrl: '/s/j-9', cancelUrl: '/c/j-9' }, { status: 202 }));
			await flush();
			out.h01_sources = s.sources.length;
			out.h01_leaked = leaked(s);
			out.h01_progress = s.env.body.querySelector('.juneau-view-job-progress') != null;
		}
		// h02: a typed success with onSuccess=redraw after release calls no redraw and paints no banner
		{
			const d = deferred();
			let redraws = 0;
			const s = setup({ respond: () => d.pr });
			s.NS.actions.run(Object.assign({ onSuccess: 'redraw' }, POST), { host: s.host, row: ROW, onRedraw: () => { redraws++; } });
			await flush();
			s.NS.actions.release(s.host);
			d.res(jsonResponse({ outcome: 'success', message: 'late' }));
			await flush();
			out.h02_redraws = redraws;
			out.h02_leaked = leaked(s);
			out.h02_text = s.env.body.textContent.indexOf('late') >= 0;
		}
		// h03: a form GET that lands after release opens no dialog
		{
			const d = deferred();
			const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
			const s = setup({ respond: () => d.pr });
			s.NS.actions.run(DLG, { host: s.host, row: ROW });
			await flush();
			s.NS.actions.release(s.host);
			d.res(jsonResponse(dialogPayload()));
			await flush();
			out.h03_layers = s.I.dialogLayerCount();
			out.h03_leaked = leaked(s);
		}
	}

	// --- i: a resultForm URL from the response body is trusted like the job URLs ------------------------------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const shapes = { evil: 'https://evil.example/r', backslash: '/\\evil', lead: ' /x/receipt', trail: '/x/receipt ', proto: '//evil.example/r' };
		for (const k of Object.keys(shapes)) {
			const s = setup({ respond: (url, method) => {
				if (method === 'GET' && url === FORM_URL) return Promise.resolve(jsonResponse(dialogPayload({ keepOpenOnSubmit: true })));
				if (method === 'GET') return Promise.resolve(jsonResponse({ title: 'Receipt' }));
				return Promise.resolve(jsonResponse({ outcome: 'success', resultForm: shapes[k] }));
			} });
			s.NS.actions.run(DLG, { host: s.host, row: ROW });
			await flush();
			const b = confirmBtn(s);
			if (b) b.dispatch('click');
			await flush();
			out['i_' + k + '_gets'] = s.calls.filter((c) => c.method === 'GET' && c.url !== FORM_URL).length;
			out['i_' + k + '_notice'] = s.env.body.textContent.indexOf('its result could not be loaded') >= 0;
		}
		// control: a trusted resultForm is fetched, and the run settles success only when the receipt is closed
		const s = setup({ respond: (url, method) => {
			if (method === 'GET' && url === FORM_URL) return Promise.resolve(jsonResponse(dialogPayload({ keepOpenOnSubmit: true })));
			if (method === 'GET') return Promise.resolve(jsonResponse({ title: 'Receipt' }));
			return Promise.resolve(jsonResponse({ outcome: 'success', resultForm: '/x/receipt' }));
		} });
		let done = null;
		s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => { done = r; });
		await flush();
		const b = confirmBtn(s);
		if (b) b.dispatch('click');
		await flush();
		out.i_control_gets = s.calls.filter((c) => c.method === 'GET' && c.url === '/x/receipt').length;
		out.i_control_pendingWhileReceiptOpen = done === null && s.I.dialogLayerCount() === 1;
		s.env.dispatchDocument('keydown', { key: 'Escape' });
		await flush();
		out.i_control_outcome = done && done.outcome;
	}

	// --- j: outcomes on one host do not leak into each other; a throw while handling a 2xx still settles ------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const s = setup({ respond: (url, method) => method === 'GET' ? Promise.resolve(jsonResponse(dialogPayload())) : Promise.resolve(jsonResponse({ outcome: 'success' })) });
		const seq = [];
		const p1 = s.NS.actions.run(DLG, { host: s.host, row: ROW }).then((r) => seq.push(r.outcome));
		await flush();
		s.env.dispatchDocument('keydown', { key: 'Escape' });
		await flush(); await p1;
		seq.push((await s.NS.actions.run({ id: 'x', label: 'X', endpoint: '/x', method: 'GET' }, { host: s.host, row: ROW })).outcome);
		seq.push((await s.NS.actions.run(POST, { host: s.host, row: ROW })).outcome);
		out.j01_sequence = seq.join(',');
		// j02: the 2xx body cannot be read -> the run still settles (unknown), it does not hang
		const t = setup({ respond: () => Promise.resolve({ ok: true, status: 200, headers: { get: () => null }, text: () => Promise.reject(new Error('boom')) }) });
		const r = await t.NS.actions.run(POST, { host: t.host, row: ROW });
		out.j02_outcome = r.outcome;
		// j03: a 2xx whose handler THROWS (the redraw callback) still settles the run, and the caller sees an outcome
		const u = setup({ respond: () => Promise.resolve(jsonResponse({ outcome: 'success', message: 'ok' })) });
		const r3 = await u.NS.actions.run(Object.assign({}, POST, { onSuccess: 'redraw' }), { host: u.host, row: ROW, onRedraw: () => { throw new Error('redraw boom'); } });
		out.j03_outcome = r3.outcome;
	}

	// --- k: URL shapes that must be refused before anything is sent --------------------------------------------------
	{
		const cases = {
			dotdot: [{ id: 'x', label: 'X', endpoint: '/x/{id}', method: 'POST' }, { id: '..' }],
			slash: [{ id: 'x', label: 'X', endpoint: '/x/{id}', method: 'POST' }, { id: 'a/b' }],
			residual: [{ id: 'x', label: 'X', endpoint: '/x/{missing}', method: 'POST' }, ROW],
			scheme: [{ id: 'x', label: 'X', endpoint: 'https://evil.example/x', method: 'POST' }, ROW]
		};
		for (const k of Object.keys(cases)) {
			const s = setup();
			const r = await s.NS.actions.run(cases[k][0], { host: s.host, row: cases[k][1] });
			out['k_' + k] = { sent: writes(s).length, outcome: r.outcome, text: bannerText(s), alert: alertEl(s) != null };
		}
	}

	// --- m: dialogs belong to the card that opened them -------------------------------------------------------------
	{
		const DLG = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: FORM_URL };
		const respond = (url, method) => method === 'GET' && url === FORM_URL
			? Promise.resolve(jsonResponse(dialogPayload()))
			: Promise.resolve(jsonResponse({ outcome: 'success', message: 'saved' }));
		const extraHost = (s) => { const h = s.env.el('div'); s.wrapper.appendChild(h); return h; };
		const backdrops = (s) => s.env.body.querySelectorAll('.juneau-view-dialog-backdrop');
		const start = (s, h, tag) => { const o = { r: null }; s.NS.actions.run(DLG, { host: h, row: ROW }).then((r) => { o.r = r; }); return o; };
		const BTN = { name: 'go', type: 'action', actionId: 'other', label: 'Go' };
		const STACK = { id: 'edit', label: 'Edit', endpoint: '/x/edit', method: 'POST', present: 'dialog', form: '/x/stack-form' };
		const OTHER = { id: 'other', label: 'Other', endpoint: '/x/other', method: 'POST', present: 'dialog', form: '/x/other-form' };
		const stackResponder = (deep) => (url, method) => {
			if (method === 'GET' && url === '/x/stack-form') return Promise.resolve(jsonResponse(dialogPayload({ form: { contractVersion: '2', fields: [BTN] } })));
			if (method === 'GET' && url === '/x/other-form') return Promise.resolve(jsonResponse(dialogPayload(deep ? { form: { contractVersion: '2', fields: [BTN] } } : {})));
			if (method === 'GET') return Promise.resolve(jsonResponse(dialogPayload()));
			return Promise.resolve(jsonResponse({ outcome: 'success', message: 'saved' }));
		};
		const startStack = (s, h) => { const o = { r: null }; s.NS.actions.run(STACK, { host: h, row: ROW, catalog: [OTHER] }).then((r) => { o.r = r; }); return o; };
		const pushChild = async (s, backdrop) => { const b = backdrop.querySelector('.juneau-view-dialog-form-action'); if (b) b.dispatch('click'); await flush(); };

		// m01: releasing one card's host leaves another card's open dialog alone
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			const opener = s.env.el('button'); s.env.body.appendChild(opener); s.env.setActive(opener);
			const a = start(s, s.host), b = start(s, hb);
			await flush();
			out.m01_before = s.I.dialogLayerCount();
			s.NS.actions.release(s.host);
			await flush();
			out.m01_after = s.I.dialogLayerCount();
			out.m01_backdrops = backdrops(s).length;
			out.m01_aOutcome = a.r && a.r.outcome;
			out.m01_bPending = b.r === null;
			out.m01_survivorLayer = backdrops(s)[0] && backdrops(s)[0].dataset.juneauLayer;
			out.m01_survivorZ = backdrops(s)[0] && backdrops(s)[0].style.zIndex;
			out.m01_focusInSurvivor = !!backdrops(s)[0] && backdrops(s)[0].contains(s.env.getActive());
		}

		// m02: committing one card's dialog closes only that card's dialog
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			const opener = s.env.el('button'); s.env.body.appendChild(opener); s.env.setActive(opener);
			const a = start(s, s.host), b = start(s, hb);
			await flush();
			const first = backdrops(s)[0];
			const btn = first.querySelector('[data-testid="dialog-confirm"]') || first.querySelector('.juneau-view-dialog-confirm');
			btn.dispatch('click');
			await flush();
			out.m02_after = s.I.dialogLayerCount();
			out.m02_backdrops = backdrops(s).length;
			out.m02_aOutcome = a.r && a.r.outcome;
			out.m02_bPending = b.r === null;
			out.m02_survivorLayer = backdrops(s)[0] && backdrops(s)[0].dataset.juneauLayer;
			out.m02_focusInSurvivor = !!backdrops(s)[0] && backdrops(s)[0].contains(s.env.getActive());
		}

		// m03: one card's open dialog does not consume another card's depth budget
		{
			const s = setup({ respond: respond });
			const hosts = [s.host, extraHost(s), extraHost(s)];
			const runs = hosts.map((h) => start(s, h));
			await flush();
			out.m03_count = s.I.dialogLayerCount();
			out.m03_refused = runs.filter((o) => o.r && o.r.outcome === 'refusal').length;
		}

		// m04: the page-wide ceiling still refuses once enough cards have dialogs open
		{
			const s = setup({ respond: respond });
			const ceiling = s.I.MAX_PAGE_DIALOG_DEPTH;
			const runs = [];
			for (let k = 0; k < ceiling + 1; k++) runs.push(start(s, k === 0 ? s.host : extraHost(s)));
			await flush();
			out.m04_ceiling = ceiling;
			out.m04_count = s.I.dialogLayerCount();
			out.m04_refused = runs.filter((o) => o.r && o.r.outcome === 'refusal').length;
			const refused = runs.find((o) => o.r && o.r.outcome === 'refusal');
			out.m04_message = refused && refused.r.message;
			out.m04_noticeInDialog = Array.prototype.reduce.call(backdrops(s), (n, b) => n + b.querySelectorAll('[data-testid="dialog-depth-refusal"]').length, 0);
			out.m04_noticeInCard = s.env.body.querySelectorAll('[data-testid="dialog-depth-refusal"]').length;
		}

		// m05: a card that holds a dialog above another card's closes without touching it (interleaved stacks)
		{
			const s = setup({ respond: stackResponder(false) });
			const hb = extraHost(s);
			const a0 = startStack(s, s.host), b0 = start(s, hb);
			await flush();
			const bBackdrop = backdrops(s)[1];
			await pushChild(s, backdrops(s)[0]);      // [A0, B0, A1]
			out.m05_before = s.I.dialogLayerCount();
			out.m05_aCount = backdrops(s).length;
			s.NS.actions.release(s.host);
			await flush();
			out.m05_after = s.I.dialogLayerCount();
			out.m05_survivorIsB = backdrops(s).length === 1 && backdrops(s)[0] === bBackdrop;
			out.m05_bPending = b0.r === null;
			out.m05_survivorLayer = bBackdrop.dataset.juneauLayer;
		}

		// m05b: the other interleaving, [A0, A1, B0]
		{
			const s = setup({ respond: stackResponder(false) });
			const hb = extraHost(s);
			const a0 = startStack(s, s.host);
			await flush();
			await pushChild(s, backdrops(s)[0]);      // [A0, A1]
			const b0 = start(s, hb);
			await flush();
			const bBackdrop = backdrops(s)[2];
			out.m05b_before = s.I.dialogLayerCount();
			s.NS.actions.release(s.host);
			await flush();
			out.m05b_after = s.I.dialogLayerCount();
			out.m05b_survivorIsB = backdrops(s).length === 1 && backdrops(s)[0] === bBackdrop;
			out.m05b_survivorLayer = bBackdrop && bBackdrop.dataset.juneauLayer;
		}

		// m06: a transient layer sitting above another card's surviving dialog stays with that dialog
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			const a0 = start(s, s.host), b0 = start(s, hb);
			await flush();
			const pop = s.env.el('div'); s.env.body.appendChild(pop);
			s.I.pushLayer(pop, { kind: 'popover', portal: false });   // [A0, B0, Bpop]
			s.NS.actions.release(s.host);
			await flush();
			out.m06_popoverKept = s.I.topLayer() && s.I.topLayer().el === pop;
			out.m06_dialogs = s.I.dialogLayerCount();
		}

		// m07: Escape closes only the top dialog and leaves the card below it
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			const a0 = start(s, s.host), b0 = start(s, hb);
			await flush();
			const aBackdrop = backdrops(s)[0];
			s.env.dispatchDocument('keydown', { key: 'Escape' });
			await flush();
			out.m07_count = s.I.dialogLayerCount();
			out.m07_remainingIsA = backdrops(s).length === 1 && backdrops(s)[0] === aBackdrop;
			out.m07_bOutcome = b0.r && b0.r.outcome;
			out.m07_aPending = a0.r === null;
		}

		// m08: a ctx that has not opened a dialog yet owns none, whatever else is open on the page
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			start(s, s.host); start(s, hb);
			await flush();
			out.m08_page = s.I.dialogLayerCount();
			out.m08_unseeded = s.I.dialogLayerCount({});
			out.m08_exceeded = s.I.dialogDepthExceeded({});
			out.m08_nullCtx = s.I.dialogDepthExceeded(null);
		}

		// m09: one card still stacks to its own cap while other cards have dialogs open, and no further
		{
			const s = setup({ respond: stackResponder(true) });
			const hb = extraHost(s);
			const b0 = start(s, hb);
			const a0 = startStack(s, s.host);
			await flush();
			await pushChild(s, backdrops(s)[1]);      // A1: allowed, A is at 1 of 2 although the page holds 3
			out.m09_count = s.I.dialogLayerCount();
			await pushChild(s, backdrops(s)[2]);      // A2: refused, A is at its own cap
			out.m09_countAfter = s.I.dialogLayerCount();
			out.m09_noticeInA1 = backdrops(s)[2].querySelectorAll('[data-testid="dialog-depth-refusal"]').length;
			out.m09_noticeInB = backdrops(s)[0].querySelectorAll('[data-testid="dialog-depth-refusal"]').length;
			out.m09_noticeText = (q(s, '[data-testid="dialog-depth-refusal"]') || {}).textContent;
		}

		// m11: a stacked step that commits while another card's modal sits above does not pull focus behind that modal
		{
			const s = setup({ respond: stackResponder(false) });
			const hb = extraHost(s);
			const a0 = startStack(s, s.host), b0 = start(s, hb);
			await flush();
			const bBackdrop = backdrops(s)[1];
			await pushChild(s, backdrops(s)[0]);      // [A0, B0, A1]
			s.env.setActive(null);
			const c = backdrops(s)[2].querySelector('[data-testid="dialog-confirm"]') || backdrops(s)[2].querySelector('.juneau-view-dialog-confirm');
			if (c) c.dispatch('click');
			out.m11_focusInBSync = bBackdrop.contains(s.env.getActive());   // before the commit settles and closes the parent
			await flush();
			out.m11_confirmFound = c != null;
			out.m11_focusInB = bBackdrop.contains(s.env.getActive());
			out.m11_bOpen = bBackdrop.parentNode != null;
		}

		// m10: a held-result or draft-refusal notice lands in the owning card's dialog, not the page's top dialog
		{
			const s = setup({ respond: respond });
			const hb = extraHost(s);
			start(s, s.host); start(s, hb);
			await flush();
			const aBackdrop = backdrops(s)[0], bBackdrop = backdrops(s)[1];
			const ctxA = { _dialogStack: [aBackdrop] };
			s.I.renderHeldResultNotice(ctxA, 'held-A');
			s.I.renderDialogDraftRefusal('x', ctxA);
			out.m10_heldInA = aBackdrop.textContent.indexOf('held-A') >= 0;
			out.m10_heldInB = bBackdrop.textContent.indexOf('held-A') >= 0;
			out.m10_draftInA = aBackdrop.textContent.indexOf("'x'") >= 0;
			out.m10_draftInB = bBackdrop.textContent.indexOf("'x'") >= 0;
		}
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
