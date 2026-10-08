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
 * console-cards.cjs - dependency-free Node harness for the open card registry added to juneau-console.js:
 * registerCard shape/duplicate checks, the deferred window.JuneauConsoleCards queue, pending-card
 * rendering, refreshCard/destroyCard/cardApi/cardTypes, and the juneau:card-mounted/card-failed events.
 * Every assertion lives in ConsoleCards_Shell_Test.
 *
 *   Usage:  node console-cards.cjs <juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const shellPath = process.argv[2];
if (!shellPath) {
	console.error('usage: node console-cards.cjs <juneau-console.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');

function load(opts) {
	const env = makeConsoleEnv(opts);
	const doc = env.document;
	const errors = [];
	const mountedEvents = [];
	const cardEvents = [];
	doc.readyState = 'loading';
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, AbortController,
		localStorage: (opts && opts.localStorage) || makeFakeStorage(),
		fetch: (opts && opts.fetch) || (() => Promise.reject(new Error('no fetch in harness'))),
		setTimeout: (fn, ms) => fn(), clearTimeout: () => {},
		setInterval: (opts && opts.setInterval) || (() => 0), clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	if (opts && opts.queueBefore) env.window.JuneauConsoleCards = opts.queueBefore.slice();
	doc.addEventListener('juneau:console-mounted', ev => { mountedEvents.push(ev.detail); });
	doc.addEventListener('juneau:card-mounted', ev => { cardEvents.push({ kind: 'mounted', detail: ev.detail }); });
	doc.addEventListener('juneau:card-failed', ev => { cardEvents.push({ kind: 'failed', detail: ev.detail }); });
	let threw = null;
	try {
		vm.runInNewContext(shellSrc, sandbox, { filename: 'juneau-console.js' });
	} catch (e) {
		threw = { name: e.name, code: e.code, message: e.message };
	}
	return { env, doc, errors, threw, mountedEvents, cardEvents, JC: sandbox.window.JuneauConsole, sandbox };
}

function makeFakeStorage() {
	const m = new Map();
	return {
		getItem: k => (m.has(k) ? m.get(k) : null),
		setItem: (k, v) => { m.set(k, String(v)); },
		removeItem: k => { m.delete(k); }
	};
}

function txt(doc, sel) {
	const e = doc.querySelector(sel);
	return e ? e.textContent : null;
}

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] };
const C = o => Object.assign({}, base, o);

const out = {};
// Card events settle on a promise, so these fields are filled in once the microtask queue has drained.
const late = [];

// 1. Queue drained before mount: entries pushed before load register and render in card order.
{
	const r = load({
		queueBefore: [
			['alpha', (card, el) => { el.textContent = 'alpha:' + card.id; }],
			['beta', { render(card, el) { el.textContent = 'beta:' + card.id; } }]
		]
	});
	const res = r.JC.mount(C({ cards: [{ id: 'b1', type: 'beta' }, { id: 'a1', type: 'alpha' }] }), { root: r.doc.body, document: r.doc });
	out.queueBefore = {
		threw: r.threw, errors: r.errors,
		a1: res.cards.a1 && res.cards.a1.textContent, b1: res.cards.b1 && res.cards.b1.textContent,
		types: r.JC.cardTypes()
	};
}

// 2. Queue drained after load: a push that arrives after the shell has already run registers immediately.
{
	const r = load({});
	r.sandbox.window.JuneauConsoleCards.push(['gamma', (card, el) => { el.textContent = 'gamma:' + card.id; }]);
	const res = r.JC.mount(C({ cards: [{ id: 'g1', type: 'gamma' }] }), { root: r.doc.body, document: r.doc });
	out.queueAfter = { threw: r.threw, errors: r.errors, g1: res.cards.g1 && res.cards.g1.textContent, types: r.JC.cardTypes() };
}

// 3. Pending card: unregistered type at mount time gets data-juneau-card-pending + loading paint, then renders
//    once registered, in queue order; a pending card of a different still-unknown type stays pending.
{
	const r = load({});
	const res = r.JC.mount(C({ cards: [
		{ id: 'p1', type: 'delta', title: 'P1' }, { id: 'p2', type: 'delta' }, { id: 'p3', type: 'epsilon' }
	] }), { root: r.doc.body, document: r.doc });
	const beforeReg = {
		p1Pending: res.cards.p1 && res.cards.p1.hasAttribute('data-juneau-card-pending'),
		p1Loading: !!res.cards.p1 && !!res.cards.p1.querySelector('.jc-card-loading'),
		p1Title: !!res.cards.p1 && !!res.cards.p1.querySelector('.jc-card-title'),
		p3Pending: res.cards.p3 && res.cards.p3.hasAttribute('data-juneau-card-pending')
	};
	const order = [];
	r.JC.registerCard('delta', (card, el) => { order.push(card.id); el.appendChild(r.doc.createTextNode('delta:' + card.id)); });
	out.pending = {
		threw: r.threw, errors: r.errors, beforeReg, order,
		p1Rendered: res.cards.p1.textContent,
		p1LoadingGone: !res.cards.p1.querySelector('.jc-card-loading'),
		p1StillPending: res.cards.p1 && res.cards.p1.hasAttribute('data-juneau-card-pending'),
		p3StillPending: res.cards.p3 && res.cards.p3.hasAttribute('data-juneau-card-pending'),
		mountedEventKinds: null
	};
	late.push(() => { out.pending.mountedEventKinds = r.cardEvents.map(e => e.kind); });
}

// 4. DOMContentLoaded with a card still pending: non-datatables gives E-JS-4, datatables gives E-JS-10 (per id).
{
	const r = load({});
	r.JC.mount(C({ cards: [{ id: 'z1', type: 'zeta' }, { id: 't1', type: 'datatables' }, { id: 't2', type: 'datatables' }] }),
		{ root: r.doc.body, document: r.doc });
	r.env.fireDOMContentLoaded();
	out.domContentLoadedPending = {
		errors: r.errors, banner: txt(r.doc, '.jc-console-error'),
		codes: Array.from(r.doc.querySelectorAll('.jc-console-error-item')).map(e => e.getAttribute('data-juneau-error')),
		failedEvents: r.cardEvents.filter(e => e.kind === 'failed').map(e => e.detail.id)
	};
}

// 5. E-JS-20, E-JS-21, E-JS-23 verbatim; function vs object handler forms both work.
{
	const r = load({});
	let e20 = null, e21a = null, e21b = null, e23 = null;
	try { r.JC.registerCard('Bad_Type', () => {}); } catch (e) { e20 = { name: e.name, code: e.code, message: e.message }; }
	try { r.JC.registerCard('ok1', 'not-a-fn'); } catch (e) { e21a = { name: e.name, code: e.code, message: e.message }; }
	try { r.JC.registerCard('ok2', {}); } catch (e) { e21b = { name: e.name, code: e.code, message: e.message }; }
	const r2 = load({ queueBefore: [['oops', 'nope', 'extra']] });
	e23 = { threw: r2.threw, errors: r2.errors };
	r.JC.registerCard('obj', { render(card, el) { el.textContent = 'obj:' + card.id; } });
	const res = r.JC.mount(C({ cards: [{ id: 'o1', type: 'obj' }] }), { root: r.doc.body, document: r.doc });
	out.registerErrors = { e20, e21a, e21b, e23, objFormText: res.cards.o1 && res.cards.o1.textContent };
}

// 6. refreshCard coalescing: a refresh requested while one is in flight collapses into one follow-up.
{
	const r = load({});
	let calls = 0;
	const pending = [];
	r.JC.registerCard('slow', {
		render(card, el) { el.textContent = 'r' + (++calls); },
		refresh(card, el) {
			return new Promise(resolve => { pending.push(() => { el.textContent = 'r' + (++calls); resolve(); }); });
		}
	});
	const res6 = r.JC.mount(C({ cards: [{ id: 's1', type: 'slow' }] }), { root: r.doc.body, document: r.doc });
	const p1 = r.JC.refreshCard('s1');
	const p2 = r.JC.refreshCard('s1');
	const p3 = r.JC.refreshCard('s1');
	const callsAtQueueTime = calls;
	pending.shift()();
	// Let the first refresh settle (a few microtasks), then release the single coalesced follow-up.
	setImmediate(() => { if (pending.length) pending.shift()(); });
	out.refreshCoalesce = Promise.all([p1, p2, p3]).then(() => ({
		callsAtQueueTime, callsAfter: calls, text: res6.cards.s1.textContent
	}));
}

// 7. destroyCard runs once; a later refreshCard throws E-JS-22; cardApi returns what the handler set.
{
	const r = load({});
	let destroyCount = 0;
	r.JC.registerCard('withapi', {
		render(card, el, ctx) { ctx.setApi({ ping: () => 'pong:' + card.id }); },
		destroy() { destroyCount++; }
	});
	r.JC.mount(C({ cards: [{ id: 'w1', type: 'withapi' }] }), { root: r.doc.body, document: r.doc });
	const apiBefore = r.JC.cardApi('w1');
	r.JC.destroyCard('w1');
	let e22c = null;
	try { r.JC.destroyCard('w1'); } catch (e) { e22c = { name: e.name, code: e.code, message: e.message }; }
	let e22 = null;
	try { r.JC.refreshCard('w1'); } catch (e) { e22 = { name: e.name, code: e.code, message: e.message }; }
	let e22b = null;
	try { r.JC.cardApi('nope-never-mounted'); } catch (e) { e22b = { name: e.name, code: e.code, message: e.message }; }
	out.destroyOnce = { apiPing: apiBefore && apiBefore.ping(), destroyCount, e22, e22b, e22c, errors: r.errors, banner: txt(r.doc, '.jc-console-error') };
}

// 8. cardTypes() is sorted; events carry {id, type, el}/{id, type, error}.
{
	const r = load({});
	r.JC.registerCard('zed', (card, el) => { el.textContent = 'zed'; });
	r.JC.registerCard('aah', (card, el) => { el.textContent = 'aah'; });
	r.JC.registerCard('mid', (card, el) => { throw new Error('kaput'); });
	r.JC.mount(C({ cards: [{ id: 'z', type: 'zed' }, { id: 'a', type: 'aah' }, { id: 'm', type: 'mid' }] }),
		{ root: r.doc.body, document: r.doc });
	out.typesAndEvents = { types: r.JC.cardTypes(), events: null };
	late.push(() => {
		out.typesAndEvents.events = r.cardEvents
			.map(e => ({ kind: e.kind, id: e.detail.id, type: e.detail.type, hasEl: !!e.detail.el, hasError: e.kind === 'failed' ? typeof e.detail.error === 'string' : undefined }))
			.sort((x, y) => x.id.localeCompare(y.id));
	});
}

// 9. A handler error marked cardReported (the handler painted and logged it itself) still fires juneau:card-failed,
// but adds no E-JS-8 to the page banner (used by the datatables handler for its card-level failures).
{
	const r = load({});
	r.JC.registerCard('quiet', () => { const e = new Error('painted in the card'); e.cardReported = true; throw e; });
	r.JC.mount(C({ cards: [{ id: 'q', type: 'quiet' }] }), { root: r.doc.body, document: r.doc });
	out.cardReported = { errors: r.errors, banner: txt(r.doc, '.jc-console-error'), events: null };
	late.push(() => { out.cardReported.events = r.cardEvents.map(e => e.kind); });
}

Promise.resolve(out.refreshCoalesce).then(resolved => {
	out.refreshCoalesce = resolved;
	setImmediate(() => {
		for (const f of late) f();
		process.stdout.write(JSON.stringify(out));
	});
});
