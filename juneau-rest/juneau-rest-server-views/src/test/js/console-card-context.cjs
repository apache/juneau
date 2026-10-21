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
 * console-card-context.cjs - dependency-free Node harness for the CardContext members added to juneau-console.js:
 * fetchJson (supersede, abort, CSRF, error-message precedence), isAbort, paintLoading/paintError, every
 * (hidden/pauseWhen/destroy, setMs re-arm), prefs (namespacing, corrupt JSON, storage-throws), onDestroy, announce.
 * Every assertion lives in ConsoleCards_Shell_Test.
 *
 *   Usage:  node console-card-context.cjs <juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const shellPath = process.argv[2];
if (!shellPath) {
	console.error('usage: node console-card-context.cjs <juneau-console.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');

function load(fetchImpl, opts) {
	const env = makeConsoleEnv(opts);
	const doc = env.document;
	doc.readyState = 'loading';
	if (opts?.csrf) {
		doc.body.dataset.juneauCsrf = opts.csrf;
		if (opts.csrfHeader) doc.body.dataset.juneauCsrfHeader = opts.csrfHeader;
	}
	const storage = (opts?.storage) || makeStorage();
	const errors = [];
	const warns = [];
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)) },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, AbortController,
		localStorage: storage,
		fetch: fetchImpl,
		setTimeout: (fn, ms) => fn(), clearTimeout: () => {},
		setInterval: (fn, ms) => {
			sandbox.__intervals = sandbox.__intervals || [];
			sandbox.__periods = sandbox.__periods || [];
			sandbox.__intervals.push(fn);
			sandbox.__periods.push(ms);
			return sandbox.__intervals.length;
		},
		clearInterval: id => {
			if (sandbox.__intervals && id > 0) {
				sandbox.__intervals[id - 1] = () => {};
				sandbox.__periods[id - 1] = null;
			}
		}
	};
	env.window.window = env.window;
	env.window.document = doc;
	vm.runInNewContext(shellSrc, sandbox, { filename: 'juneau-console.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	return { env, doc, errors, warns, JC: sandbox.window.JuneauConsole, sandbox, storage };
}

function makeStorage(behavior) {
	const m = new Map();
	return {
		getItem: k => { if (behavior === 'throw') { throw new Error('storage unavailable'); } return m.has(k) ? m.get(k) : null; },
		setItem: (k, v) => { if (behavior === 'throw') { throw new Error('storage unavailable'); } m.set(k, String(v)); },
		removeItem: k => { m.delete(k); },
		_raw: m
	};
}

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] };
const C = o => ({ ...base, ...o });
const out = {};

// 1. fetchJson supersede: a second call before the first settles aborts the first; the first's promise
//    rejects with something ctx.isAbort recognizes; handlers see only the second's result.
{
	const seen = [];
	let ctxRef = null;
	const r = load((url, init) => new Promise((resolve, reject) => {
		seen.push(url);
		if (init?.signal) init.signal.addEventListener('abort', () => {
			const e = new Error('aborted'); e.name = 'AbortError'; reject(e);
		});
		Promise.resolve().then(() => undefined).then(() => resolve({ ok: true, json: () => Promise.resolve({ v: url }) }));
	}), {});
	r.JC.registerCard('kpi', {
		render(card, el, ctx) { ctxRef = ctx; return Promise.resolve(); }
	});
	r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	const p1 = ctxRef.fetchJson('/a').catch(e => ({ aborted: ctxRef.isAbort(e) }));
	const p2 = ctxRef.fetchJson('/b').then(d => ({ v: d.v }));
	out.supersede = Promise.all([p1, p2]).then(([r1, r2]) => ({ seen, r1, r2 }));
}

// 2. CSRF header on non-GET, taken from the body attributes; absent on GET.
{
	const calls = [];
	let ctxRef = null;
	const r = load((url, init) => { calls.push({ url, method: (init?.method) || 'GET', headers: { ...init?.headers } }); return Promise.resolve({ ok: true, json: () => Promise.resolve({}) }); },
		{ csrf: 'tok-123', csrfHeader: 'X-My-Csrf' });
	r.JC.registerCard('kpi', { render(card, el, ctx) { ctxRef = ctx; } });
	r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	// Sequential: a second concurrent call would supersede (abort) the first.
	out.csrf = ctxRef.fetchJson('/get')
		.then(() => ctxRef.fetchJson('/post', { method: 'POST', body: '{}' }))
		.then(() => ({ calls }));
}

// 3. Error precedence: body.message wins; else 'HTTP <status>'; a network-level rejection gives 'Request failed'.
{
	let ctxRef = null;
	let i = 0;
	const r = load(url => {
		i++;
		if (i === 1) return Promise.resolve({ ok: false, status: 400, json: () => Promise.resolve({ message: 'bad input' }) });
		if (i === 2) return Promise.resolve({ ok: false, status: 500, json: () => Promise.reject(new Error('not json')) });
		return Promise.reject(new TypeError('network down'));
	}, {});
	r.JC.registerCard('kpi', { render(card, el, ctx) { ctxRef = ctx; } });
	r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	const settle = p => p.then(v => v, e => e.message);
	out.errorPrecedence = settle(ctxRef.fetchJson('/a'))
		.then(a => settle(ctxRef.fetchJson('/b')).then(b => settle(ctxRef.fetchJson('/c')).then(c => [a, b, c])));
}

// 3b. A body read that fails after the headers: only a parse failure on an ok response resolves null; an abort
//     rejects with something ctx.isAbort recognizes; a transport failure (what a navigation mid-body gives) rejects
//     with 'Request failed' on an ok response and keeps 'HTTP <status>' on an error response.
{
	let ctxRef = null;
	const bodies = {
		'/ok-abort': [200, 'AbortError'], '/ok-transport': [200, 'TypeError'], '/ok-parse': [200, 'SyntaxError'],
		'/err-abort': [503, 'AbortError'], '/err-transport': [503, 'TypeError'], '/err-parse': [503, 'SyntaxError']
	};
	const r = load(url => {
		const [status, name] = bodies[url];
		const e = Object.assign(new Error('body read failed'), { name });
		return Promise.resolve({ ok: status === 200, status, json: () => Promise.reject(e) });
	}, {});
	r.JC.registerCard('kpi', { render(card, el, ctx) { ctxRef = ctx; } });
	r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	const settle = p => p.then(v => ({ value: v }), e => ({ error: e.message, abort: ctxRef.isAbort(e) }));
	// Sequential: a second concurrent call would supersede (abort) the first.
	out.bodyFailures = Object.keys(bodies).reduce(
		(p, url) => p.then(acc => settle(ctxRef.fetchJson(url)).then(v => Object.assign(acc, { [url]: v }))),
		Promise.resolve({}));
}

// 4. paintLoading/paintError roles.
{
	let ctxRef = null;
	const r = load(() => Promise.resolve({ ok: true, json: () => Promise.resolve({}) }), {});
	r.JC.registerCard('kpi', { render(card, el, ctx) { ctxRef = ctx; ctx.paintLoading(el); } });
	const res = r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	const loadingRole = r.doc.querySelector('#k1 .jc-card-loading') && r.doc.querySelector('#k1 .jc-card-loading').getAttribute('role'); // NOSONAR javascript:S6582 - null (not undefined) reaches the serialized assertion
	ctxRef.paintError(res.cards.k1, new Error('boom'));
	out.paint = {
		loadingRole,
		loadingThenError: !!r.doc.querySelector('#k1 .jc-card-error') && !r.doc.querySelector('#k1 .jc-card-loading'),
		errorRole: r.doc.querySelector('#k1 .jc-card-error') && r.doc.querySelector('#k1 .jc-card-error').getAttribute('role'), // NOSONAR javascript:S6582 - null (not undefined) reaches the serialized assertion
		errorText: r.doc.querySelector('#k1 .jc-card-error') && r.doc.querySelector('#k1 .jc-card-error').textContent // NOSONAR javascript:S6582 - null (not undefined) reaches the serialized assertion
	};
}

// 5. every: pauses while document.hidden, pauses while pauseWhen() is true, stops on destroy.
{
	let tick = 0;
	const r = load(() => Promise.resolve({ ok: true, json: () => Promise.resolve({}) }), {});
	r.JC.registerCard('poll', {
		render(card, el, ctx) {
			ctx.every(1000, () => { tick++; }, { pauseWhen: () => r.doc.__paused === true });
		}
	});
	r.JC.mount(C({ cards: [{ id: 'p1', type: 'poll' }] }), { root: r.doc.body, document: r.doc });
	const runInterval = () => { for (const f of r.sandbox.__intervals || []) f(); };
	runInterval();
	const afterOne = tick;
	r.doc.hidden = true;
	runInterval();
	const whileHidden = tick;
	r.doc.hidden = false;
	r.doc.__paused = true;
	runInterval();
	const whilePaused = tick;
	r.doc.__paused = false;
	r.JC.destroyCard('p1');
	runInterval();
	out.every = { afterOne, whileHidden, whilePaused, afterDestroy: tick };
}

// 5b. every handle setMs: re-arms at the new period; paused/hidden still skip; a destroyed card ignores it.
{
	const activePeriods = r => (r.sandbox.__periods || []).filter(p => p != null);
	const runInterval = r => { for (const f of r.sandbox.__intervals || []) f(); };
	const make = (id, onTick) => {
		let handle = null;
		const r = load(() => Promise.resolve({ ok: true, json: () => Promise.resolve({}) }), {});
		r.JC.registerCard('poll2', {
			render(card, el, ctx) {
				handle = ctx.every(1000, onTick, { pauseWhen: () => r.doc.__paused === true });
			}
		});
		r.JC.mount(C({ cards: [{ id, type: 'poll2' }] }), { root: r.doc.body, document: r.doc });
		return { r, handle };
	};
	// (a) shorter period re-arms the timer and the old one stops firing.
	let ticksA = 0;
	const a = make('s1', () => { ticksA++; });
	const before = activePeriods(a.r);
	a.handle.setMs(250);
	const afterSet = activePeriods(a.r);
	runInterval(a.r);
	// (b) setMs while hidden or paused re-arms but nothing fires until resumed.
	let ticksB = 0;
	const b = make('s2', () => { ticksB++; });
	b.r.doc.hidden = true;
	b.handle.setMs(250);
	runInterval(b.r);
	const whileHidden = ticksB;
	b.r.doc.hidden = false;
	b.r.doc.__paused = true;
	b.handle.setMs(125);
	runInterval(b.r);
	const whilePaused = ticksB;
	b.r.doc.__paused = false;
	runInterval(b.r);
	const afterResume = ticksB;
	const periodsB = activePeriods(b.r);
	// (c) setMs after destroy, and after stop, arms nothing.
	let ticksC = 0;
	const c = make('s3', () => { ticksC++; });
	c.r.JC.destroyCard('s3');
	const countBefore = (c.r.sandbox.__periods || []).length;
	c.handle.setMs(250);
	const armedAfterDestroy = (c.r.sandbox.__periods || []).length - countBefore;
	runInterval(c.r);
	let ticksD = 0;
	const d = make('s4', () => { ticksD++; });
	d.handle.stop();
	const countBeforeStop = (d.r.sandbox.__periods || []).length;
	d.handle.setMs(250);
	const armedAfterStop = (d.r.sandbox.__periods || []).length - countBeforeStop;
	// (d) invalid periods: setMs keeps the existing timer; every refuses to arm and returns an inert handle.
	let ticksE = 0;
	const e = make('s5', () => { ticksE++; });
	const periodsBeforeBad = activePeriods(e.r);
	const countBeforeBad = (e.r.sandbox.__periods || []).length;
	for (const bad of [Number.NaN, 0, -5, undefined, Infinity, '250']) e.handle.setMs(bad);
	const badSetMs = { armed: (e.r.sandbox.__periods || []).length - countBeforeBad, active: activePeriods(e.r), before: periodsBeforeBad };
	runInterval(e.r);
	const everyBad = [];
	{
		const r = load(() => Promise.resolve({ ok: true }), {});
		let ticksF = 0;
		let hs = [];
		r.JC.registerCard('poll3', {
			render(card, el, ctx) {
				for (const bad of [Number.NaN, 0, -1, undefined, '1000']) hs.push(ctx.every(bad, () => { ticksF++; }));
			}
		});
		r.JC.mount(C({ cards: [{ id: 's6', type: 'poll3' }] }), { root: r.doc.body, document: r.doc });
		let threw = null;
		try { for (const h of hs) { h.setMs(100); h.stop(); } } catch (error_) { threw = String(error_); }
		runInterval(r);
		everyBad.push({ armed: (r.sandbox.__periods || []).length, ticks: ticksF, threw, warned: r.warns.length });
	}
	out.everySetMs = {
		badSetMs, ticksAfterBadSetMs: ticksE, everyBad: everyBad[0],
		before, afterSet, ticksAfterRun: ticksA,
		whileHidden, whilePaused, afterResume, periodsB,
		armedAfterDestroy, ticksAfterDestroy: ticksC,
		armedAfterStop, activeAfterStop: activePeriods(d.r), ticksAfterStop: ticksD
	};
}

// 6. prefs: namespaced key, corrupt JSON falls back to default, a throwing storage falls back without throwing.
{
	let ctxRef = null;
	const r = load(() => Promise.resolve({ ok: true }), {});
	r.JC.registerCard('prefcard', { render(card, el, ctx) { ctxRef = ctx; } });
	r.JC.mount(C({ cards: [{ id: 'pc1', type: 'prefcard' }] }), { root: r.doc.body, document: r.doc });
	const prefs = ctxRef.prefs();
	prefs.set('color', 'blue');
	const key = Array.from(r.storage._raw.keys())[0];
	const nsPrefs = ctxRef.prefs('cols');
	nsPrefs.set('w', 3);
	const nsKey = Array.from(r.storage._raw.keys())[1];
	const corruptStorage = makeStorage();
	corruptStorage._raw.set('juneau-card:pc2:x', '{not json');
	let ctxRef2 = null;
	const r2 = load(() => Promise.resolve({ ok: true }), { storage: corruptStorage });
	r2.JC.registerCard('prefcard', { render(card, el, ctx) { ctxRef2 = ctx; } });
	r2.JC.mount(C({ cards: [{ id: 'pc2', type: 'prefcard' }] }), { root: r2.doc.body, document: r2.doc });
	const corruptValue = ctxRef2.prefs().get('x', 'dflt');
	const throwingStorage = makeStorage('throw');
	let ctxRef3 = null;
	const r3 = load(() => Promise.resolve({ ok: true }), { storage: throwingStorage });
	r3.JC.registerCard('prefcard', { render(card, el, ctx) { ctxRef3 = ctx; } });
	r3.JC.mount(C({ cards: [{ id: 'pc3', type: 'prefcard' }] }), { root: r3.doc.body, document: r3.doc });
	let threw3 = null;
	let val3 = null;
	try { val3 = ctxRef3.prefs().get('x', 'fallback'); ctxRef3.prefs().set('x', 1); ctxRef3.prefs().remove('x'); } catch (e) { threw3 = e.message; }
	out.prefs = { key, nsKey, storedValue: prefs.get('color', null), corruptValue, throwingThrew: threw3, throwingValue: val3 };
}

// 7. onDestroy callbacks run on destroyCard.
{
	const ran = [];
	const r = load(() => Promise.resolve({ ok: true }), {});
	r.JC.registerCard('ondcard', { render(card, el, ctx) { ctx.onDestroy(() => ran.push('a')); ctx.onDestroy(() => ran.push('b')); } });
	r.JC.mount(C({ cards: [{ id: 'd1', type: 'ondcard' }] }), { root: r.doc.body, document: r.doc });
	r.JC.destroyCard('d1');
	out.onDestroy = { ran };
}

Promise.all([out.supersede, out.csrf, out.errorPrecedence, out.bodyFailures]).then(([supersede, csrf, errorPrecedence, bodyFailures]) => {
	out.supersede = supersede;
	out.csrf = csrf;
	out.errorPrecedence = errorPrecedence;
	out.bodyFailures = bodyFailures;
	process.stdout.write(JSON.stringify(out));
});
