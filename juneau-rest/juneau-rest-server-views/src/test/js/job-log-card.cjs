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
 * job-log-card.cjs - dependency-free Node harness for juneau-joblog.js: the form, submit, the progress stream, every
 * outcome banner, cancel, reload re-attach, not-found, interruption, no EventSource, new run, clearOnSubmit,
 * refresh and destroy.  Every assertion lives in ConsoleJobLog_Test.
 *
 *   Usage:  node job-log-card.cjs <juneau-console.js> <juneau-joblog.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const [shellPath, jobLogPath] = process.argv.slice(2);
if (!shellPath || !jobLogPath) {
	console.error('usage: node job-log-card.cjs <juneau-console.js> <juneau-joblog.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');
const jobLogSrc = fs.readFileSync(jobLogPath, 'utf8');

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] };
const KEY = 'juneau-card:rebuild:job';
const REF = { jobId: 'j1', streamUrl: '/jobs/j1/stream', cancelUrl: '/jobs/j1/cancel' };
const CARD = {
	id: 'rebuild', type: 'job-log', title: 'Rebuild index',
	submit: {
		endpoint: '/jobs/rebuild',
		fields: [
			{ name: 'reason', label: 'Reason', type: 'text', required: true },
			{ name: 'shards', label: 'Shards', type: 'number', default: 4 },
			{ name: 'notes', label: 'Notes', type: 'textarea', placeholder: 'Optional' }
		],
		submitLabel: 'Rebuild'
	},
	height: 320, urlParam: 'job'
};
const card = o => Object.assign({}, CARD, o);
const result = (outcome, message) => JSON.stringify(Object.assign({ outcome, contractVersion: '1' }, message ? { message } : {}));

class FakeEventSource {
	constructor(url) {
		this.url = String(url);
		this.readyState = 1;
		this.listeners = {};
		FakeEventSource.instances.push(this);
	}
	addEventListener(type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); }
	close() { this.readyState = 2; }
	// id (optional) is the SSE id the server sent with the event; it is set as the event's lastEventId, as a browser does.
	emit(type, data, id) { if (this.readyState !== 2) this.dispatch(type, data, id); }
	// Delivers an event even after close(): simulates the browser race where a queued event fires after the runtime closed the stream.
	forceEmit(type, data, id) { this.dispatch(type, data, id); }
	dispatch(type, data, id) {
		const ev = id === undefined ? { type, data } : { type, data, lastEventId: String(id) };
		(this.listeners[type] || []).slice().forEach(fn => fn(ev));
	}
	fail(readyState) { this.readyState = readyState; (this.listeners.error || []).slice().forEach(fn => fn({ type: 'error' })); }
}

function load(opts) {
	opts = opts || {};
	FakeEventSource.instances = [];
	const env = makeConsoleEnv({ location: { search: opts.search || '' } });
	const doc = env.document;
	doc.readyState = 'loading';
	doc.body.setAttribute('data-juneau-csrf', 'tok');
	const w = env.window;
	const replaced = [];
	w.history = {
		state: null,
		replaceState: (s, t, u) => {
			replaced.push(String(u));
			const q = String(u).indexOf('?');
			w.location.search = q < 0 ? '' : String(u).substring(q);
		}
	};
	let uuid = 0;
	w.crypto = { randomUUID: () => 'uuid-' + (++uuid) };
	if (!opts.noEventSource) w.EventSource = FakeEventSource;
	const storage = new Map(Object.entries(opts.storage || {}));
	const errors = [], fetches = [];
	const sandbox = {
		window: w, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams: w.URLSearchParams || URLSearchParams, URL, CustomEvent: w.CustomEvent,
		AbortController,
		localStorage: {
			getItem: k => storage.has(k) ? storage.get(k) : null,
			setItem: (k, v) => storage.set(k, String(v)),
			removeItem: k => storage.delete(k)
		},
		fetch: (url, init) => {
			const headers = (init && init.headers) || {};
			fetches.push({
				url: String(url),
				method: init && init.method,
				contentType: headers['Content-Type'] || null,
				csrf: Object.values(headers).indexOf('tok') >= 0,
				body: init && init.body ? JSON.parse(init.body) : null
			});
			return (opts.fetch || (() => ok(REF)))(String(url), init);
		},
		setTimeout: opts.timers ? (fn => { opts.timers.push(fn); return opts.timers.length; }) : (fn => { fn(); return 0; }),
		clearTimeout: opts.timers ? (id => { opts.timers[id - 1] = null; }) : () => {},
		setInterval: () => 0, clearInterval: () => {}
	};
	w.window = w;
	w.document = doc;
	const ctx = vm.createContext(sandbox);
	vm.runInContext(shellSrc, ctx, { filename: 'juneau-console.js' });
	vm.runInContext(jobLogSrc, ctx, { filename: 'juneau-joblog.js' });
	const r = { env, doc, w, errors, fetches, replaced, storage, es: FakeEventSource.instances };
	r.mount = cards => w.JuneauConsole.mount(Object.assign({}, base, { cards }), { root: doc.body, document: doc });
	return r;
}

const ok = body => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body) });
const bad = (status, body) => Promise.resolve({ ok: false, status, json: () => Promise.resolve(body) });
const host = (r, id) => r.doc.getElementById(id || 'rebuild');
const all = (r, sel, id) => { const h = host(r, id); return h ? Array.from(h.querySelectorAll(sel)) : []; };
const one = (r, sel, id) => all(r, sel, id)[0] || null;
const text = (r, sel, id) => { const n = one(r, sel, id); return n ? n.textContent : null; };
const flush = async () => { for (let i = 0; i < 8; i++) await new Promise(res => setImmediate(res)); };

function fill(r, values) {
	for (const n of all(r, '.jc-joblog-input'))
		if (Object.hasOwn(values, n.name)) n.value = values[n.name];
}
async function submit(r, values) {
	fill(r, values || { reason: 'nightly' });
	one(r, '.jc-joblog-form').dispatch('submit', { preventDefault: () => {} });
	await flush();
}
async function clickSubmitButton(r) {
	one(r, '.jc-joblog-submit').dispatch('click', { preventDefault: () => {} });
}
const lines = r => all(r, '.jc-joblog-line').map(n => n.textContent);
function state(r) {
	const b = one(r, '.jc-joblog-banner');
	const s = one(r, '.jc-joblog-submit');
	const nr = one(r, '.jc-joblog-newrun');
	return {
		phase: one(r, '.jc-joblog') && one(r, '.jc-joblog').getAttribute('data-phase'),
		banner: b && !b.hidden ? b.textContent : null,
		tone: b && !b.hidden ? ['ok', 'error', 'warning'].find(t => b.classList.contains('jc-joblog-' + t)) || null : null,
		role: b && !b.hidden ? b.getAttribute('role') : null,
		submitText: s && s.textContent,
		submitHidden: !!(s && s.hidden),
		submitDisabled: !!(s && s.disabled),
		newRunHidden: !!(nr && nr.hidden),
		inputsDisabled: all(r, '.jc-joblog-input').map(n => n.disabled)
	};
}
const stored = r => r.storage.has(KEY) ? JSON.parse(r.storage.get(KEY)) : null;
// The shell's screen-reader announce region (ctx.announce); null when nothing was announced.
const announced = r => { const n = r.doc.body.querySelector('.jc-sr-announce'); return n ? n.textContent : null; };
// Which job-log control has focus: 'submit', 'newRun', 'output', 'input' or null.
function focused(r) {
	const a = r.env.getActive();
	if (!a) return null;
	for (const [k, sel] of [['submit', '.jc-joblog-submit'], ['newRun', '.jc-joblog-newrun'], ['output', '.jc-joblog-output'], ['input', '.jc-joblog-input']])
		if (all(r, sel).indexOf(a) >= 0) return k;
	return 'other';
}
const out = {};

(async () => {
	// 1. The form.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		const o = one(r, '.jc-joblog-output');
		out.form = {
			title: text(r, '.jc-card-title'),
			labels: all(r, '.jc-joblog-label').map(n => n.textContent),
			names: all(r, '.jc-joblog-input').map(n => n.name),
			types: all(r, '.jc-joblog-input').map(n => n.tagName === 'TEXTAREA' ? 'textarea' : n.type),
			required: all(r, '.jc-joblog-input').map(n => n.required),
			values: all(r, '.jc-joblog-input').map(n => n.value),
			placeholder: one(r, 'textarea').getAttribute('placeholder'),
			outputRole: o.getAttribute('role'),
			ariaLive: o.getAttribute('aria-live'),
			tabindex: o.getAttribute('tabindex'),
			height: o.style.height,
			api: Object.keys(r.w.JuneauConsole.cardApi('rebuild') || {}).sort(),
			version: r.w.JuneauJobLog && r.w.JuneauJobLog.version,
			state: state(r),
			errors: r.errors
		};
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.form = c; }

	// 2. Defaults: no fields, "Run", 240px.
	try {
		const r = load();
		r.mount([{ id: 'rebuild', type: 'job-log', submit: { endpoint: '/a' } }]);
		await flush();
		out.defaults = { submitText: text(r, '.jc-joblog-submit'), height: one(r, '.jc-joblog-output').style.height, inputs: all(r, '.jc-joblog-input').length };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.defaults = c; }

	// 3. Submit, stream, success.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		fill(r, { reason: 'nightly' });
		one(r, '.jc-joblog-form').dispatch('submit', { preventDefault: () => {} });
		const submitting = state(r);
		await flush();
		const running = state(r);
		const es = r.es[0];
		es.emit('progress', 'step 1 <b>bold</b>');
		es.emit('progress', 'step 2');
		es.emit('result', result('success', 'Rebuilt 4 shards.'));
		out.success = {
			submitting, running,
			fetches: r.fetches,
			streams: r.es.map(e => e.url),
			replaced: r.replaced,
			stored: stored(r),
			lines: lines(r),
			boldTags: all(r, 'b').length,
			closed: es.readyState,
			state: state(r),
			announced: announced(r),
			errors: r.errors
		};
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.success = c; }

	// 4. Every non-success outcome uses the error tone; the message is appended.
	try {
		const outcomes = {}, announcedOutcomes = {};
		for (const [o, m] of [['failure', 'disk full'], ['refusal', null], ['unknown', null], ['cancelled', null], ['cancelled-after-effect', null], ['weird', null]]) {
			const r = load();
			r.mount([CARD]);
			await flush();
			await submit(r);
			r.es[0].emit('result', result(o, m));
			const s = state(r);
			outcomes[o] = { banner: s.banner, tone: s.tone, role: s.role };
			announcedOutcomes[o] = announced(r);
		}
		out.outcomes = outcomes;
		out.outcomesAnnounced = announcedOutcomes;
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.outcomes = c; }

	// 5. A submit response that is not an AsyncJobRef: E-JS-77 inline, form kept.
	try {
		const r = load({ fetch: () => ok({ nope: 1 }) });
		r.mount([CARD]);
		await flush();
		await submit(r);
		out.badRef = { state: state(r), streams: r.es.length, stored: stored(r), form: !!one(r, '.jc-joblog-form'), errors: r.errors };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.badRef = c; }

	// 6. A submit rejected by the server (e.g. a job limit): the response message, inline.
	try {
		const r = load({ fetch: () => bad(429, { message: 'Too many jobs are running; try again later.' }) });
		r.mount([CARD]);
		await flush();
		await submit(r);
		out.submitError = { state: state(r), streams: r.es.length };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.submitError = c; }

	// 7. An unparsable result event: E-JS-78, stream closed.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		r.es[0].emit('result', 'not json');
		out.badEvent = { state: state(r), closed: r.es[0].readyState, errors: r.errors };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.badEvent = c; }

	// 8. Cancel: POST cancelUrl, settle from the response, ignore the late stream result.
	try {
		const r = load({ fetch: url => url === REF.cancelUrl ? ok(JSON.parse(result('cancelled'))) : ok(REF) });
		r.mount([CARD]);
		await flush();
		await submit(r);
		await clickSubmitButton(r);
		const cancelling = state(r);
		await flush();
		r.es[0].forceEmit('result', result('success', 'too late'));
		out.cancel = { cancelling, fetches: r.fetches.map(f => ({ url: f.url, method: f.method, csrf: f.csrf, contentType: f.contentType, body: f.url === REF.cancelUrl ? f.body : undefined })), state: state(r), closed: r.es[0].readyState };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.cancel = c; }

	// 9. Reload re-attach: the URL param matches the stored job.
	try {
		const r = load({ search: '?job=j1', storage: { [KEY]: JSON.stringify(REF) } });
		r.mount([CARD]);
		await flush();
		r.es[0].emit('progress', 'replayed');
		out.reattach = { streams: r.es.map(e => e.url), fetches: r.fetches.length, state: state(r), lines: lines(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.reattach = c; }

	// 10. Re-attach without urlParam: the stored job alone is enough.
	try {
		const r = load({ storage: { [KEY]: JSON.stringify(REF) } });
		r.mount([card({ urlParam: undefined })]);
		await flush();
		out.reattachNoParam = { streams: r.es.map(e => e.url), phase: state(r).phase };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.reattachNoParam = c; }

	// 11. Mismatch: a URL job id with no matching stored job falls back to the form, no error, storage untouched.
	try {
		const r = load({ search: '?job=other', storage: { [KEY]: JSON.stringify(REF) } });
		r.mount([CARD]);
		await flush();
		out.mismatch = { streams: r.es.length, state: state(r), stored: stored(r), errors: r.errors };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.mismatch = c; }

	// 11b. A URL job id with nothing stored (a shared link, another browser): the form, no stream, no error.
	try {
		const r = load({ search: '?job=j1' });
		r.mount([CARD]);
		await flush();
		out.urlNoStored = { streams: r.es.length, fetches: r.fetches.length, state: state(r), stored: stored(r), errors: r.errors };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.urlNoStored = c; }

	// 12. Not found: the stream closes before any event.
	try {
		const r = load({ search: '?job=j1&tab=a', storage: { [KEY]: JSON.stringify(REF) }, fetch: () => bad(404, {}), timers: [] });
		r.mount([CARD]);
		await flush();
		r.es[0].fail(2);
		await flush();
		out.notFound = {
			lines: lines(r),
			notice: all(r, '.jc-joblog-notice').length,
			stored: stored(r),
			replaced: r.replaced,
			state: state(r)
		};
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.notFound = c; }

	// 12b. A refused attach that is not a 404 (429, 5xx, a network failure) is a failed attach, not "Job not found": the stored
	// job and the URL param stay so a reload can try again.
	try {
		const attach = async fetchImpl => {
			const r = load({ search: '?job=j1&tab=a', storage: { [KEY]: JSON.stringify(REF) }, fetch: fetchImpl, timers: [] });
			r.mount([CARD]);
			await flush();
			r.es[0].fail(2);
			await flush();
			return { lines: lines(r), stored: stored(r), replaced: r.replaced, state: state(r), probes: r.fetches.map(f => f.url),
				live: one(r, '.jc-joblog-output').getAttribute('aria-live'), announced: announced(r) };
		};
		out.attachFailed = { busy: await attach(() => bad(429, {})), server: await attach(() => bad(503, {})),
			network: await attach(() => Promise.reject(new TypeError('down'))) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.attachFailed = c; }

	// 13. Interrupted: a reconnecting error is ignored; a close after events is a warning, prefs kept.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		r.es[0].emit('progress', 'step 1');
		r.es[0].fail(0);
		const reconnecting = state(r).phase;
		r.es[0].fail(2);
		out.interrupted = { reconnecting, state: state(r), stored: stored(r), announced: announced(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.interrupted = c; }

	// 14. No EventSource in this browser.
	try {
		const r = load({ noEventSource: true });
		r.mount([CARD]);
		await flush();
		await submit(r);
		out.noSse = { state: state(r), stored: stored(r), fetches: r.fetches.length, announced: announced(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.noSse = c; }

	// 15. New run, and clearOnSubmit (default true) vs false.
	try {
		const runTwice = async c => {
			const r = load();
			r.mount([c]);
			await flush();
			await submit(r);
			r.es[0].emit('progress', 'first');
			r.es[0].emit('result', result('success'));
			one(r, '.jc-joblog-newrun').dispatch('click', {});
			const afterNewRun = { state: state(r), stored: stored(r), replaced: r.replaced.slice(), lines: lines(r) };
			await submit(r, { reason: 'again' });
			r.es[1].emit('progress', 'second');
			return { afterNewRun, lines: lines(r), body: r.fetches[1].body, uuids: r.fetches.map(f => f.body.idempotencyKey) };
		};
		out.newRun = await runTwice(CARD);
		out.keepLog = await runTwice(card({ clearOnSubmit: false }));
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.newRun = c; out.keepLog = c; }

	// 16. refresh is a no-op (a running stream survives it); destroy closes the stream.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		r.es[0].emit('progress', 'step 1');
		await r.w.JuneauConsole.refreshCard('rebuild');
		await flush();
		const afterRefresh = { streams: r.es.length, open: r.es[0].readyState, lines: lines(r), phase: state(r).phase };
		r.w.JuneauConsole.destroyCard('rebuild');
		out.lifecycle = { afterRefresh, closedOnDestroy: r.es[0].readyState };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.lifecycle = c; }

	// 17. A job with no cancelUrl: the running button reads Cancel but is disabled, and clicking it sends nothing.
	try {
		const r = load({ fetch: () => ok({ jobId: 'j1', streamUrl: '/jobs/j1/stream' }) });
		r.mount([CARD]);
		await flush();
		await submit(r);
		const running = state(r);
		await clickSubmitButton(r);
		await flush();
		out.noCancelUrl = { running, fetches: r.fetches.length, state: state(r), open: r.es[0].readyState };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.noCancelUrl = c; }

	// 18. A failed cancel: the error is shown, the job keeps running and can still finish on the stream.
	try {
		const r = load({ fetch: url => url === REF.cancelUrl ? bad(500, { message: 'cancel failed' }) : ok(REF) });
		r.mount([CARD]);
		await flush();
		await submit(r);
		one(r, '.jc-joblog-submit').focus();
		await clickSubmitButton(r);
		await flush();
		const afterFailedCancel = Object.assign(state(r), { open: r.es[0].readyState, focus: focused(r) });
		r.es[0].emit('result', result('success'));
		out.cancelFailed = { afterFailedCancel, fetches: r.fetches.map(f => f.url), state: state(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.cancelFailed = c; }

	// 19. Required fields: the shim's form has no constraint validation (nor does a scripted submit), so the runtime
	// checks them: nothing is sent, the field is marked and focused, and a filled-in resubmit goes through.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r, { reason: '   ' });
		const reason = one(r, '.jc-joblog-input');
		const blocked = { fetches: r.fetches.length, state: state(r), invalid: reason.getAttribute('aria-invalid'),
			focused: r.env.getActive() === reason };
		await submit(r, { reason: 'nightly' });
		out.required = { blocked, fetches: r.fetches.length, invalidAfter: reason.getAttribute('aria-invalid'), phase: state(r).phase };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.required = c; }

	// 20. Reconnect: every progress event carries an SSE id and the browser's own reconnect sends Last-Event-ID, so the
	// server resumes after the last event shown; the runtime just shows what arrives, and the stream stays open while
	// the browser retries.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		const es = r.es[0];
		es.emit('progress', 'p1', 1);
		es.emit('progress', 'p2', 2);
		es.fail(0);
		const dropped = { phase: state(r).phase, readyState: es.readyState, banner: state(r).banner };
		es.readyState = 1;
		es.emit('open');
		es.emit('progress', 'p3', 3);
		const afterFirst = lines(r);
		// A second drop; the server resumes after id 3.
		es.fail(0);
		es.readyState = 1;
		es.emit('open');
		es.emit('progress', 'p4', 4);
		es.emit('result', result('success'));
		out.reconnect = { dropped, afterFirst, lines: lines(r), streams: r.es.length, state: state(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.reconnect = c; }

	// 21. Same-origin: a submit response or stored job whose streamUrl/cancelUrl points at another origin is rejected
	// before anything is opened or sent (the cancel POST would carry the CSRF token); an absolute same-origin URL is fine.
	try {
		const crossRef = async ref => {
			const r = load({ fetch: () => ok(ref) });
			r.mount([CARD]);
			await flush();
			await submit(r);
			await clickSubmitButton(r);
			await flush();
			return { state: state(r), streams: r.es.length, fetches: r.fetches.map(f => f.url), stored: stored(r), errors: r.errors.length };
		};
		const cancel = await crossRef({ jobId: 'j1', streamUrl: '/jobs/j1/stream', cancelUrl: 'https://evil.test/jobs/j1/cancel' });
		const stream = await crossRef({ jobId: 'j1', streamUrl: '//evil.test/jobs/j1/stream', cancelUrl: '/jobs/j1/cancel' });
		const p = load({ search: '?job=j1', storage: { [KEY]: JSON.stringify({ jobId: 'j1', streamUrl: '/jobs/j1/stream', cancelUrl: 'https://evil.test/c' }) } });
		p.mount([CARD]);
		await flush();
		await clickSubmitButton(p);
		await flush();
		const poisoned = { phase: state(p).phase, streams: p.es.length, fetches: p.fetches.length, errors: p.errors,
			stored: stored(p), search: p.w.location.search };
		const abs = load({ fetch: url => url.endsWith('/cancel') ? ok(JSON.parse(result('cancelled'))) :
			ok({ jobId: 'j1', streamUrl: 'http://juneau.test/jobs/j1/stream', cancelUrl: 'http://juneau.test/jobs/j1/cancel' }) }); // NOSONAR javascript:S5332 -- the shim's fake in-memory origin
		abs.mount([CARD]);
		await flush();
		await submit(abs);
		await clickSubmitButton(abs);
		await flush();
		const absolute = { streams: abs.es.map(e => e.url), fetches: abs.fetches.map(f => ({ url: f.url, csrf: f.csrf })), phase: state(abs).phase };
		out.crossOrigin = { cancel, stream, poisoned, absolute };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.crossOrigin = c; }

	// 22. Focus: a focused button that is disabled (submitting, cancelling) hands focus to the log pane; the Run/Cancel
	// button and "New run" hand focus to each other as they swap at done and idle.
	try {
		const r = load({ fetch: url => url === REF.cancelUrl ? ok(JSON.parse(result('cancelled'))) : ok(REF) });
		r.mount([CARD]);
		await flush();
		fill(r, { reason: 'nightly' });
		one(r, '.jc-joblog-submit').focus();
		one(r, '.jc-joblog-form').dispatch('submit', { preventDefault: () => {} });
		const whileSubmitting = focused(r);
		await flush();
		one(r, '.jc-joblog-submit').focus();
		r.es[0].emit('result', result('success'));
		const afterDone = focused(r);
		one(r, '.jc-joblog-newrun').dispatch('click', {});
		const afterNewRun = focused(r);
		await submit(r);
		one(r, '.jc-joblog-submit').focus();
		await clickSubmitButton(r);
		const whileCancelling = focused(r);
		await flush();
		out.focus = { whileSubmitting, afterDone, afterNewRun, whileCancelling, phase: state(r).phase };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.focus = c; }

	// 23. Endpoint origin: the submit POST carries the CSRF token, so an endpoint that resolves to another origin is
	// refused in the browser too (Java checks it at render): an inline error, no fetch, the form stays idle.  Backslash
	// and leading-space forms are refused as well, since a browser reads them as protocol-relative.
	try {
		const tryEndpoint = async endpoint => {
			const r = load();
			r.mount([card({ submit: Object.assign({}, CARD.submit, { endpoint }) })]);
			await flush();
			await submit(r);
			return { fetches: r.fetches.map(f => f.url), streams: r.es.length, state: state(r), errors: r.errors };
		};
		out.crossEndpoint = {};
		for (const [k, ep] of [['backslash', '\\evil.test/x'], ['protocolRelative', '//evil.test'], ['leadingSpace', ' //evil.test'],
				['absolute', 'https://evil.test/jobs/rebuild'], ['sameOrigin', '/jobs/rebuild'],
				['sameOriginQuery', '/jobs/x?a=b'], ['relative', 'jobs/x']])
			out.crossEndpoint[k] = await tryEndpoint(ep);
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.crossEndpoint = c; }

	// 24. Banner order: role is set before the text, and the text is cleared first, so an identical repeated error
	// is a fresh change to the alert region and is announced again.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r, { reason: '' });
		const b = one(r, '.jc-joblog-banner');
		const events = [];
		const tc = Object.getOwnPropertyDescriptor(b, 'textContent') || Object.getOwnPropertyDescriptor(Object.getPrototypeOf(b), 'textContent');
		Object.defineProperty(b, 'textContent', { configurable: true, get() { return tc.get.call(this); },
			set(v) { events.push('text:' + v); tc.set.call(this, v); } });
		let hidden = b.hidden;
		Object.defineProperty(b, 'hidden', { configurable: true, get() { return hidden; }, set(v) { events.push('hidden:' + v); hidden = v; } });
		const sa = b.setAttribute.bind(b), ra = b.removeAttribute.bind(b);
		b.setAttribute = (n, v) => { if (n === 'role') events.push('role:' + v); sa(n, v); };
		b.removeAttribute = n => { if (n === 'role') events.push('role:-'); ra(n); };
		await submit(r, { reason: '' });
		out.bannerOrder = { events, state: state(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.bannerOrder = c; }

	// 25. An id-less stream (an older server): every line is shown, across a drop too.
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		const es = r.es[0];
		es.emit('progress', 'p1');
		es.emit('progress', 'p2');
		es.fail(0);
		es.readyState = 1;
		es.emit('progress', 'p3');
		es.emit('result', result('success'));
		out.idless = { lines: lines(r), streams: r.es.length, state: state(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.idless = c; }

	// 26. A server that re-sends an event after a drop: the repeat is shown, never hidden (fail-loud).
	try {
		const r = load();
		r.mount([CARD]);
		await flush();
		await submit(r);
		const es = r.es[0];
		es.emit('progress', 'p1', 1);
		es.emit('progress', 'p2', 2);
		es.fail(0);
		es.readyState = 1;
		es.emit('progress', 'p2', 2);
		es.emit('progress', 'p3', 3);
		es.emit('result', result('success'));
		out.repeatedId = { lines: lines(r), streams: r.es.length, state: state(r) };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.repeatedId = c; }

	// 27. A reattach replays the buffer in a burst: the log pane is silenced (aria-live off) while the lines arrive and one summary
	// is announced once the burst goes quiet, then the pane is polite again for live lines.  A fresh submit is not silenced.
	try {
		const timers = [];
		const r = load({ search: '?job=j1', storage: { [KEY]: JSON.stringify(REF) }, timers });
		r.mount([CARD]);
		await flush();
		const live = () => one(r, '.jc-joblog-output').getAttribute('aria-live');
		const before = live();
		for (const t of ['a', 'b', 'c']) r.es[0].emit('progress', t);
		const during = { live: live(), announced: announced(r), lines: lines(r) };
		for (const fn of timers.splice(0)) if (fn) fn();
		const after = { live: live(), announced: announced(r) };
		r.es[0].emit('progress', 'd');
		const fresh = load({ timers: [] });
		fresh.mount([CARD]);
		await flush();
		await submit(fresh);
		out.replayQuiet = { before, during, after, lines: lines(r), freshLive: one(fresh, '.jc-joblog-output').getAttribute('aria-live') };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.replayQuiet = c; }

	// 28. A replay also ends when the job finishes during it (polite again, no summary), when the stream is refused (polite again), and at
	// the cap even if lines keep arriving; the attach probe is aborted when the card is torn down, and a probe that never answers reads
	// as a failed attach.
	try {
		const live = r => one(r, '.jc-joblog-output').getAttribute('aria-live');
		const reattach = async (extra) => {
			const timers = [];
			const r = load(Object.assign({ search: '?job=j1', storage: { [KEY]: JSON.stringify(REF) }, timers }, extra || {}));
			r.mount([CARD]);
			await flush();
			return { r, timers };
		};
		const done = await reattach();
		done.r.es[0].emit('progress', 'a');
		done.r.es[0].emit('result', result('success'));
		const resultDuring = { live: live(done.r), announced: announced(done.r), state: state(done.r) };
		const cap = await reattach();
		for (const t of ['a', 'b']) cap.r.es[0].emit('progress', t);
		const beforeCap = live(cap.r);
		cap.timers[0]();   // the cap timer is armed first; the quiet timer (still pending) is never fired
		cap.r.es[0].emit('progress', 'c');
		const atCap = { before: beforeCap, live: live(cap.r), announced: announced(cap.r) };
		let aborted = 0;
		const hang = await reattach({ fetch: (u, init) => new Promise((_, rej) => { if (init && init.signal) init.signal.addEventListener('abort', () => { aborted++; rej(new Error('aborted')); }); }) });
		hang.r.es[0].fail(2);
		await flush();
		const probing = { phase: state(hang.r).phase, live: live(hang.r) };
		for (const fn of hang.timers.splice(0)) if (fn) fn();   // the probe timeout
		await flush();
		const timedOut = { state: state(hang.r), aborted, stored: stored(hang.r) };
		const torn = await reattach({ fetch: (u, init) => new Promise((_, rej) => { if (init && init.signal) init.signal.addEventListener('abort', () => { aborted++; rej(new Error('aborted')); }); }) });
		torn.r.es[0].fail(2);
		await flush();
		const before = aborted;
		torn.r.w.JuneauConsole.destroyCard('rebuild');
		await flush();
		out.replayEnds = { resultDuring, atCap, probing, timedOut, abortedOnTeardown: aborted - before };
	} catch (e) { const c = { crash: String(e && e.stack || e) }; out.replayEnds = c; }

	process.stdout.write(JSON.stringify(out));
})().catch(e => {
	console.error(e && e.stack || e);
	process.exit(1);
});
