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
 * job-host.cjs - always-on Node harness for async jobs started through JuneauViews.actions.run (a chart card has no
 * row, so the job paints into the card's banner host instead of a table row) and for cancelJob's contract.  A fake
 * EventSource drives progress, result and error; every assertion lives in the Java test.
 *
 *   Usage: node job-host.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node job-host.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const out = {};
const ACTION = { id: 'run', label: 'Run job', endpoint: '/x/run', method: 'POST' };
const ROW = { id: 'r1' };

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 10) { return r(); } setTimeout(tick, 0); })(); });
}

function setup(o) {
	const opts = o || {};
	const env = makeEnv();
	const sources = [];
	if (!opts.noEventSource) {
		const FakeES = function (url) {
			if (opts.throwOnOpen) throw new Error('blocked');
			this.url = url; this.listeners = {}; this.closed = false; sources.push(this);
		};
		FakeES.prototype.addEventListener = function (type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); };
		FakeES.prototype.close = function () { this.closed = true; };
		FakeES.prototype.fire = function (type, data) { (this.listeners[type] || []).forEach(function (fn) { fn({ data: data }); }); };
		env.EventSource = FakeES;
	}
	const { NS, I } = loadViews(rendersJsPath, viewsJsPath, env);
	const wrapper = env.el('div');
	wrapper.dataset.juneauCsrf = 'tok-1';
	const host = env.el('div');
	wrapper.appendChild(host);
	env.body.appendChild(wrapper);
	const calls = [];
	const started = opts.started || { jobId: 'j-1', streamUrl: '/s/j-1', cancelUrl: '/c/j-1' };
	env.setFetch(function (url, init) {
		const method = (init && init.method) || 'GET';
		calls.push({ url: String(url), method: method, headers: (init && init.headers) || {} });
		if (url === '/x/run') return Promise.resolve(jsonResponse(started));
		return opts.cancelResp ? opts.cancelResp() : Promise.resolve(jsonResponse({}));
	});
	return { env: env, NS: NS, I: I, host: host, sources: sources, calls: calls, wrapper: wrapper };
}

const q = (s, sel) => s.env.body.querySelector(sel);
const text = (s, sel) => { const e = q(s, sel); return e ? e.textContent : null; };
const bannerText = (s) => text(s, '[data-testid="chart-action-banner"]');

(async function main() {

	// --- r01: the run stays pending while the job streams, shows progress, and resolves with the job's result ---
	{
		const s = setup();
		let done = null;
		const p = s.NS.actions.run(ACTION, { host: s.host, row: ROW }).then((r) => { done = r; });
		await flush();
		out.r01_pending = done === null;
		out.r01_sourceOpened = s.sources.length === 1 && s.sources[0].url === '/s/j-1';
		out.r01_progressShown = q(s, '[data-testid="job-progress"]') != null;
		out.r01_cancelButton = q(s, '.juneau-view-job-cancel') != null;
		out.r01_cancelLabel = q(s, '.juneau-view-job-cancel') && q(s, '.juneau-view-job-cancel').getAttribute('aria-label');
		out.r01_runningMarker = s.host.getAttribute('data-juneau-job') || (s.host.dataset && s.host.dataset.juneauJob) || null;
		s.sources[0].fire('progress', '40%');
		out.r01_progressText = text(s, '.juneau-view-job-progress-msg');
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'all done' }));
		await flush(); await p;
		out.r01_outcome = done && done.outcome;
		out.r01_message = done && done.message;
		out.r01_progressGone = q(s, '[data-testid="job-progress"]') === null;
		out.r01_closed = s.sources[0].closed;
		out.r01_painted = (bannerText(s) || '').indexOf('all done') >= 0;
	}

	// --- r02: a stream error resolves unknown ---------------------------------------------------------------------
	{
		const s = setup();
		const p = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		s.sources[0].fire('error');
		const r = await p;
		out.r02_outcome = r.outcome;
		out.r02_message = r.message;
	}

	// --- r03: no EventSource at all resolves unknown with the browser notice ---------------------------------------
	{
		const s = setup({ noEventSource: true });
		const r = await s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		out.r03_outcome = r.outcome;
		out.r03_message = r.message;
	}

	// --- r04: an off-site stream URL opens no stream and says why --------------------------------------------------
	{
		const s = setup({ started: { jobId: 'j-2', streamUrl: 'https://evil.example/s', cancelUrl: '/c/j-2' } });
		const r = await s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		out.r04_opened = s.sources.length;
		out.r04_outcome = r.outcome;
		out.r04_message = r.message;
	}

	// --- r05: an EventSource constructor that throws resolves unknown ----------------------------------------------
	{
		const s = setup({ throwOnOpen: true });
		const r = await s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		out.r05_outcome = r.outcome;
		out.r05_message = r.message;
	}

	// --- r06: an off-site cancel URL shows no cancel button --------------------------------------------------------
	{
		const s = setup({ started: { jobId: 'j-3', streamUrl: '/s/j-3', cancelUrl: 'https://evil.example/c' } });
		const p = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		out.r06_progress = q(s, '[data-testid="job-progress"]') != null;
		out.r06_noCancel = q(s, '.juneau-view-job-cancel') === null;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
		await p;
	}

	// --- r07: the cancel button posts once to the cancel URL with the CSRF token, and is disabled after ------------
	{
		const s = setup();
		const p = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		const btn = q(s, '.juneau-view-job-cancel');
		btn.dispatch('click');
		btn.dispatch('click');
		await flush();
		const cancels = s.calls.filter((c) => c.url === '/c/j-1');
		out.r07_cancelPosts = cancels.length;
		out.r07_csrf = JSON.stringify(cancels[0] && cancels[0].headers).indexOf('tok-1') >= 0;
		out.r07_disabled = btn.disabled === true;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled', message: 'stopped' }));
		const r = await p;
		out.r07_outcome = r.outcome;
	}

	// --- r08: a failed cancel re-enables the button and does not become the run's outcome ---------------------------
	{
		const s = setup({ cancelResp: () => Promise.resolve(jsonResponse({}, { status: 500 })) });
		const p = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		const btn = q(s, '.juneau-view-job-cancel');
		btn.dispatch('click');
		await flush();
		out.r08_reEnabled = btn.disabled === false;
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'finished anyway' }));
		const r = await p;
		out.r08_outcome = r.outcome;
	}

	// --- r09: release closes the stream and settles the pending run as unknown --------------------------------------
	{
		const s = setup();
		let done = null;
		s.NS.actions.run(ACTION, { host: s.host, row: ROW }).then((r) => { done = r; });
		await flush();
		s.NS.actions.release(s.host);
		await flush();
		out.r09_closed = s.sources[0].closed;
		out.r09_outcome = done && done.outcome;
		out.r09_bannerGone = q(s, '[data-testid="chart-action-banner"]') === null;
		// a late event that still reaches a released run paints nothing: no banner host is rebuilt on the card's parent
		s.sources[0].fire('progress', '90%');
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success', message: 'late-text' }));
		await flush();
		out.r09_noBannerHostRebuilt = q(s, '[data-testid="ribbon-banner-host"]') === null && q(s, '[data-testid="chart-action-banner"]') === null;
		out.r09_noLateText = s.env.body.textContent.indexOf('late-text') < 0 && q(s, '[data-testid="job-progress"]') === null;
	}

	// --- r10: a second run after the job settles starts a fresh stream ---------------------------------------------
	{
		const s = setup();
		const p1 = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		s.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
		await p1;
		const p2 = s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		await flush();
		out.r10_secondStream = s.sources.length === 2;
		s.sources[1].fire('result', JSON.stringify({ outcome: 'failure', message: 'bad' }));
		const r2 = await p2;
		out.r10_outcome = r2.outcome;
	}

	// --- r04b: a stream URL that parses same-origin but carries characters trimming would change opens no stream -----
	{
		const s = setup({ started: { jobId: 'j-4', streamUrl: '/s/j-4 ', cancelUrl: '/c/j-4' } });
		const r = await s.NS.actions.run(ACTION, { host: s.host, row: ROW });
		out.r04b_opened = s.sources.length;
		out.r04b_outcome = r.outcome;
	}

	// --- r11: two jobs on one host keep one progress element each, updated independently ------------------------------
	{
		const s = setup();
		const row = s.env.el('div');
		const host = { container: function () { return row; } };
		const a = { jobId: 'a', cancelUrl: '/c/a' };
		const b = { jobId: 'b', cancelUrl: '/c/b' };
		s.I.renderJobProgress(host, a, ACTION, s.wrapper, 'one');
		s.I.renderJobProgress(host, b, ACTION, s.wrapper, 'two');
		s.I.renderJobProgress(host, a, ACTION, s.wrapper, 'one-again');
		const els = row.querySelectorAll('.juneau-view-job-progress');
		out.r11_count = els.length;
		out.r11_texts = Array.prototype.map.call(els, function (e) { return e.querySelector('.juneau-view-job-progress-msg').textContent; }).join(',');
	}

	// --- r12: job URL shapes that must never be streamed or cancelled -------------------------------------------------
	{
		const bad = { backslash: '/\\evil', lead: ' /s/x', c0: '/s/\u0001x', newline: '/s/x\n', proto: '//evil.example/s', scheme: 'javascript:alert(1)' };
		for (const k of Object.keys(bad)) {
			const s = setup({ started: { jobId: 'j-5', streamUrl: bad[k], cancelUrl: '/c/j-5' } });
			const r = await s.NS.actions.run(ACTION, { host: s.host, row: ROW });
			out['r12_stream_' + k] = { opened: s.sources.length, outcome: r.outcome };
			const t = setup({ started: { jobId: 'j-6', streamUrl: '/s/j-6', cancelUrl: bad[k] } });
			const p = t.NS.actions.run(ACTION, { host: t.host, row: ROW });
			await flush();
			out['r12_cancel_' + k] = { noButton: q(t, '.juneau-view-job-cancel') === null, streamed: t.sources.length };
			t.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
			await p;
			out['r12_cancelJob_' + k] = (await t.I.cancelJob({ cancelUrl: bad[k] }, t.wrapper, t.env.el('tr'), ACTION)) === false;
			out['r12_noCancelPost_' + k] = t.calls.filter((c) => c.method === 'POST' && c.url !== '/x/run').length === 0;
		}
		const u = setup({ started: { jobId: 'j-7', streamUrl: '/s/j-7', cancelUrl: '/c/j-7' } });
		const up = u.NS.actions.run(ACTION, { host: u.host, row: ROW });
		await flush();
		u.sources[0].fire('result', JSON.stringify({ outcome: 'success' }));
		await up;
		await u.I.cancelJob({ cancelUrl: '//evil.example/c' }, u.wrapper, u.host, ACTION);
		out.r12_untrustedCancelText = u.host.textContent || '';
		const m = setup();
		out.r12_missingCancel = await m.I.cancelJob({}, m.wrapper, m.env.el('tr'), ACTION);
	}

	// --- m01..m09: actionOutcomeMessage never ends in a doubled period --------------------------------------------
	{
		const s = setup();
		const msg = (cls) => s.I.actionOutcomeMessage(cls);
		out.m01_refusalDot = msg({ outcome: 'refusal', refusalCode: 'CSRF_TOKEN_MISMATCH', transport: true,
			message: 'The token is not valid for this request.' });
		out.m02_successReplayDot = msg({ outcome: 'success', replay: true, message: 'Saved.' });
		out.m03_failureDots = msg({ outcome: 'failure', message: 'Gone...' });
		out.m04_noDot = msg({ outcome: 'cancelled', message: 'stopped' });
		out.m05_bangKept = msg({ outcome: 'failure', message: 'Boom!' });
		out.m06_questionKept = msg({ outcome: 'unknown', message: 'Did it work?' });
		out.m07_noMessage = msg({ outcome: 'success' });
		out.m08_afterEffectDot = msg({ outcome: 'cancelled-after-effect', message: 'Half done.' });
		out.m09_dotsOnly = msg({ outcome: 'failure', message: '...' });
	}

	// --- t01..t04: cancelJob's contract --------------------------------------------------------------------------
	{
		const s = setup();
		const table = s.wrapper;
		const started = { jobId: 'j-t', cancelUrl: '/c/j-t' };
		const tr = s.env.el('tr');
		out.t01_returnsPromise = typeof s.I.cancelJob(started, table, tr, ACTION).then === 'function';
		out.t02_okTrue = (await s.I.cancelJob(started, table, tr, ACTION)) === true;
		out.t03_untrustedFalse = (await s.I.cancelJob({ cancelUrl: 'https://evil.example/c' }, table, tr, ACTION)) === false;
		const bad = setup({ cancelResp: () => Promise.resolve(jsonResponse({}, { status: 500 })) });
		out.t04_nonOkFalse = (await bad.I.cancelJob(started, bad.wrapper, bad.env.el('tr'), ACTION)) === false;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
