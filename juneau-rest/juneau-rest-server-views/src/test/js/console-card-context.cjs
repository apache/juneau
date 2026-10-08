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
 * (hidden/pauseWhen/destroy), prefs (namespacing, corrupt JSON, storage-throws), onDestroy, announce.
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
	if (opts && opts.csrf) {
		doc.body.setAttribute('data-juneau-csrf', opts.csrf);
		if (opts.csrfHeader) doc.body.setAttribute('data-juneau-csrf-header', opts.csrfHeader);
	}
	const storage = (opts && opts.storage) || makeStorage();
	const errors = [];
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, AbortController,
		localStorage: storage,
		fetch: fetchImpl,
		setTimeout: (fn, ms) => fn(), clearTimeout: () => {},
		setInterval: (fn, ms) => { sandbox.__intervals = sandbox.__intervals || []; sandbox.__intervals.push(fn); return sandbox.__intervals.length; },
		clearInterval: id => { if (sandbox.__intervals && id > 0) sandbox.__intervals[id - 1] = () => {}; }
	};
	env.window.window = env.window;
	env.window.document = doc;
	vm.runInNewContext(shellSrc, sandbox, { filename: 'juneau-console.js' });
	return { env, doc, errors, JC: sandbox.window.JuneauConsole, sandbox, storage };
}

function makeStorage(behavior) {
	const m = new Map();
	return {
		getItem: k => { if (behavior === 'throw') throw new Error('storage unavailable'); return m.has(k) ? m.get(k) : null; },
		setItem: (k, v) => { if (behavior === 'throw') throw new Error('storage unavailable'); m.set(k, String(v)); },
		removeItem: k => { m.delete(k); },
		_raw: m
	};
}

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] };
const C = o => Object.assign({}, base, o);
const out = {};

// 1. fetchJson supersede: a second call before the first settles aborts the first; the first's promise
//    rejects with something ctx.isAbort recognizes; handlers see only the second's result.
{
	const seen = [];
	let ctxRef = null;
	const r = load((url, init) => new Promise((resolve, reject) => {
		seen.push(url);
		if (init && init.signal) init.signal.addEventListener('abort', () => {
			const e = new Error('aborted'); e.name = 'AbortError'; reject(e);
		});
		Promise.resolve().then(() => Promise.resolve()).then(() => resolve({ ok: true, json: () => Promise.resolve({ v: url }) }));
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
	const r = load((url, init) => { calls.push({ url, method: (init && init.method) || 'GET', headers: Object.assign({}, init && init.headers) }); return Promise.resolve({ ok: true, json: () => Promise.resolve({}) }); },
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

// 4. paintLoading/paintError roles.
{
	let ctxRef = null;
	const r = load(() => Promise.resolve({ ok: true, json: () => Promise.resolve({}) }), {});
	r.JC.registerCard('kpi', { render(card, el, ctx) { ctxRef = ctx; ctx.paintLoading(el); } });
	const res = r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi' }] }), { root: r.doc.body, document: r.doc });
	const loadingRole = r.doc.querySelector('#k1 .jc-card-loading') && r.doc.querySelector('#k1 .jc-card-loading').getAttribute('role');
	ctxRef.paintError(res.cards.k1, new Error('boom'));
	out.paint = {
		loadingRole,
		loadingThenError: !!r.doc.querySelector('#k1 .jc-card-error') && !r.doc.querySelector('#k1 .jc-card-loading'),
		errorRole: r.doc.querySelector('#k1 .jc-card-error') && r.doc.querySelector('#k1 .jc-card-error').getAttribute('role'),
		errorText: r.doc.querySelector('#k1 .jc-card-error') && r.doc.querySelector('#k1 .jc-card-error').textContent
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

Promise.all([out.supersede, out.csrf, out.errorPrecedence]).then(([supersede, csrf, errorPrecedence]) => {
	out.supersede = supersede;
	out.csrf = csrf;
	out.errorPrecedence = errorPrecedence;
	process.stdout.write(JSON.stringify(out));
});
