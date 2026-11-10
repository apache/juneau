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
 * console-nested.cjs - Node harness for cards nested inside a card (renderNested) and their bus wiring: nested
 * rejection codes, parent/child lifecycle, topics declared before a nested view is built, cardApi for nested cards.
 * Loads the real juneau-bus.js and juneau-console.js into a fresh vm sandbox per case and prints ONE JSON report; every
 * assertion lives in ConsoleNested_Test.
 *
 *   Usage:  node console-nested.cjs <juneau-bus.js> <juneau-console.js>
 */
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const [busPath, shellPath] = process.argv.slice(2);
if (!busPath || !shellPath) {
	console.error('usage: node console-nested.cjs <juneau-bus.js> <juneau-console.js>');
	process.exit(2);
}
const busSrc = fs.readFileSync(path.resolve(busPath), 'utf8');
const shellSrc = fs.readFileSync(path.resolve(shellPath), 'utf8');

const tick = () => new Promise(r => setImmediate(r));

// The element surface the card shell needs beyond views-dom-shim: hasAttribute, innerHTML = "" and a bubbling
// dispatchEvent that reaches the document's listeners.  Each patch is skipped when the shim already has it.
function patchElement(env, node) {
	if (!node.hasAttribute) node.hasAttribute = function (k) { return Object.hasOwn(this.attrs, k); };
	if (!Object.getOwnPropertyDescriptor(node, 'innerHTML'))
		Object.defineProperty(node, 'innerHTML', {
			get() { return this.childNodes.map(c => c.className || '').join(' '); },
			set(v) {
				if (v !== '') throw new Error('harness: only innerHTML = "" is supported');
				for (const c of this.childNodes.slice()) c.remove();
			}
		});
	if (!node.dispatchEvent) node.dispatchEvent = function (ev) { env.document.dispatchEvent(ev); return true; };
	return node;
}

/**
 * One page: `contract` in a #juneau-page island, optional page-script hooks, then DOMContentLoaded.
 * opts: {noBus, shellFirst, beforeShell(sandbox), queue: [[type, handler]], beforeReady(r), location}
 * shellFirst loads the shell BEFORE the bus, the order a <@console> page really has: the shell auto-mounts at
 * script load, and the bus script runs later in the same document.
 */
async function page(contract, opts) {
	opts = opts || {};
	const env = makeConsoleEnv({ location: opts.location });
	const doc = env.document;
	const create = doc.createElement;
	doc.createElement = tag => patchElement(env, create(tag));
	patchElement(env, doc.body);
	const errors = [], warns = [], cardEvents = [];
	const island = doc.createElement('script');
	island.setAttribute('type', 'application/json');
	island.id = 'juneau-page';
	island.textContent = JSON.stringify({ contractVersion: '1', title: 'T', nav: [], activeNav: [], ...contract });
	doc.body.appendChild(island);
	doc.readyState = 'loading';
	const fetches = [];
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)), debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, URL: URL, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, Number, AbortController,
		encodeURIComponent, Date,
		localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
		fetch: url => { fetches.push(url); return Promise.reject(new Error('no fetch in harness')); },
		setTimeout: fn => fn(), clearTimeout: () => {}, setInterval: () => 0, clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	env.window.localStorage = sandbox.localStorage;
	if (opts.queue) env.window.JuneauConsoleCards = opts.queue.map(([t, h]) => [t, toCardClass(h)]);
	doc.addEventListener('juneau:card-mounted', ev => cardEvents.push('mounted:' + ev.detail.id));
	doc.addEventListener('juneau:card-failed', ev => cardEvents.push('failed:' + ev.detail.id));
	vm.createContext(sandbox);
	const r = { env, doc, errors, warns, cardEvents, fetches, sandbox, threw: null, readyThrew: null };
	try {
		const loadBus = () => { if (!opts.noBus) vm.runInContext(busSrc, sandbox, { filename: 'juneau-bus.js' }); }; // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
		if (!opts.shellFirst) loadBus();
		if (opts.beforeShell) opts.beforeShell(sandbox, r);
		vm.runInContext(shellSrc, sandbox, { filename: 'juneau-console.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
		if (opts.shellFirst) loadBus();
	} catch (e) {
		r.threw = { code: e.code, message: e.message };
	}
	r.JC = env.window.JuneauConsole;
	r.bus = env.window.JuneauViews?.bus;
	await tick();
	if (opts.beforeReady) opts.beforeReady(r);
	try {
		env.fireDOMContentLoaded();
	} catch (e) {
		r.readyThrew = { code: e.code, message: e.message };
	}
	await tick();
	return r;
}

/**
 * Turns a test card description (render/refresh/destroy hooks taking the card spec, roles/ops taking the spec as the
 * last argument, an implicit(spec) function) into a card class; the shell puts its Card base under it at registration.
 */
function toCardClass(h) {
	if (typeof h === 'function') return h;
	const K = class {}; // NOSONAR javascript:S2094 - a bare card type for the fixture
	K.lateBase = true;
	const P = K.prototype;
	if (h.render) P.render = function () { return h.render(this.spec, this.el, this); };
	if (h.refresh) P.refresh = function () { return h.refresh(this.spec, this.el, this); };
	if (h.destroy) P.destroy = function () { return h.destroy(this.spec, this.el, this); };
	K.roles = {};
	for (const k of Object.keys(h.roles || {})) {
		const f = h.roles[k];
		P['role$' + k] = function (payload, meta) { return f.call(this, payload, meta, this.spec); };
		K.roles[k] = 'role$' + k;
	}
	K.ops = {};
	for (const k of Object.keys(h.ops || {})) {
		const f = h.ops[k];
		P['op$' + k] = function (cmd) { return f.call(this, cmd, this.spec); };
		K.ops[k] = 'op$' + k;
	}
	if (h.implicit) K.implicitTopics = spec => h.implicit(spec);
	return K;
}

/** `sel` inside card `id` (the shim's selectors have no descendant combinator). */
function q(r, id, sel) { const c = r.doc.getElementById(id); return c ? c.querySelector(sel) : null; }
function text(r, id, sel) { const n = q(r, id, sel); return n ? n.textContent : null; }
function codes(r, id) { return r.doc.getElementById(id).querySelectorAll('.jc-card-bus-error').map(n => n.dataset.juneauError ?? null); }
function banner(r) { return r.doc.querySelectorAll('.jc-console-error-item').map(n => n.dataset.juneauError ?? null); }
function get(r, topic) { const v = r.bus.get(topic); return v === undefined ? null : structuredClone(v); }
function owner(r, topic) { const e = r.bus.topics().find(t => t.topic === topic); return e ? e.owner : null; }


const out = {};
const codeOf = fn => { try { fn(); return null; } catch (e) { return e.code || String(e); } };

async function main() {
	// A host card whose render nests whatever the test queued; handles are kept for the assertions.
	const nestedCalls = [];
	const hostType = (specs, keep) => ({ render(card, el, inst) {
		for (const spec of specs) {
			try { keep[spec.id] = inst.renderNested(spec, el, {}); }
			catch (e) { nestedCalls.push({ id: spec.id, code: e.code || String(e) }); }
		}
	} });
	const leaf = log => ({ render(card, el, inst) { log.push('render:' + card.id); const p = inst.document.createElement('p'); p.textContent = 'leaf:' + card.id; el.appendChild(p); },
		destroy(card) { log.push('destroy:' + card.id); } });

	// n1. Nested card is a real card: element, cardApi, lifecycle topic, destroyed with the parent, id reusable.
	{
		const log = [], keep = {};
		const r = await page({ cards: [{ id: 'host', type: 'host' }] },
			{ queue: [['host', hostType([{ type: 'leaf', id: 'inner', title: 'T' }], keep)], ['leaf', leaf(log)]] });
		const innerEl = r.doc.getElementById('inner');
		const api = r.JC.cardApi('inner');
		const lifecycle = get(r, 'card:inner');
		const sameInst = api === keep.inner;
		r.JC.destroyCard('host');
		out.n1 = { hasEl: !!innerEl, sameInst, lifecycle, log, afterApi: codeOf(() => r.JC.cardApi('inner')),
			afterTopic: get(r, 'card:inner'), calls: nestedCalls.slice() };
	}
	// n2. Rejections: nested switcher is E-JS-126, an id already live is E-JS-127 (also against a top-level card).
	{
		nestedCalls.length = 0;
		const keep = {};
		const r = await page({ cards: [{ id: 'top', type: 'leaf' }, { id: 'host', type: 'host' }] },
			{ queue: [['host', hostType([{ type: 'switcher', id: 'sw' }, { type: 'leaf', id: 'top' }, { type: 'leaf', id: 'dup' }, { type: 'leaf', id: 'dup' }], keep)], ['leaf', leaf([])]] });
		out.n2 = { calls: nestedCalls.slice(), banner: r.doc.querySelectorAll('.jc-console-error-item').map(n => n.dataset.juneauError ?? null), dupOk: !!keep.dup };
	}
	// n3. The parent's signal aborts the nested card's signal.
	{
		const keep = {}; let sig = null;
		const r = await page({ cards: [{ id: 'host', type: 'host' }] },
			{ queue: [['host', hostType([{ type: 'sig', id: 'inner' }], keep)], ['sig', { render(card, el, inst) { sig = inst.signal; } }]] });
		const before = sig.aborted;
		r.JC.destroyCard('host');
		out.n3 = { before, after: sig.aborted };
	}
	// n4. Bus: nested publishes are declared before the nested card is built, a nested publish reaches a subscriber,
	// a nested subscriber is wired, and destroying the parent releases the topics.
	{
		const keep = {}; let nestedCtx = null; const got = [];
		const contract = { cards: [
			{ id: 'host', type: 'host2' },
			{ id: 'sub', type: 'listen', subscribes: [{ topic: 'inner.click', as: 'record' }] }
		] };
		const hostCls = { render(card, el, inst) {
			keep.inner = inst.renderNested({ type: 'pub', id: 'inner', publishes: [{ topic: 'inner.click', retain: false }] }, el, {});
			keep.listener = inst.renderNested({ type: 'listen', id: 'listener', subscribes: [{ topic: 'inner.click', as: 'record' }] }, el, {});
		} };
		hostCls.static = null;
		const r = await page(contract, { queue: [
			['host2', hostCls],
			['pub', { render(card, el, inst) { nestedCtx = inst; } }],
			['listen', { roles: { record: (payload, meta, spec) => { got.push(spec.id + ':' + JSON.stringify(payload)); } }, render() {} }]
		] });
		const seen = [];
		r.bus.subscribe('inner.click', p => seen.push(p), { retained: false });
		nestedCtx.publish('inner.click', { n: 1 });
		await tick();
		const topics = r.bus.topics().map(t => t.topic);
		const lifecycle = get(r, 'card:inner');
		const listenerLifecycle = get(r, 'card:listener');
		r.JC.destroyCard('host');
		nestedCtx = null;
		out.n4 = { seen, got, declared: topics.includes('inner.click'), lifecycle, listenerLifecycle,
			afterLifecycle: get(r, 'card:inner'), apiAfter: codeOf(() => r.JC.cardApi('inner')) };
	}
	// n5. nestedCards on a registered class: its specs are declared and validated before the nested card is built.
	{
		const Sw = class {}; // NOSONAR javascript:S2094 - a bare card type for the fixture
		Sw.lateBase = true;
		Sw.nestedCards = spec => spec.views.map(v => v.card);
		Sw.prototype.render = function () {};
		const r = await page({ cards: [{ id: 'sw1', type: 'swx', views: [{ id: 'a', card: { id: 'early', type: 'leaf', publishes: [{ topic: 'early.topic', retain: false }] } }] }] },
			{ queue: [['swx', Sw], ['leaf', leaf([])]] });
		out.n5 = { declared: r.bus.topics().some(t => t.topic === 'early.topic'), all: JSON.stringify(r.bus.topics().map(t => t.topic)), errs: r.errors.join('|'), noBuilt: codeOf(() => r.JC.cardApi('early')) };
	}
}

main().then(() => { process.stdout.write(JSON.stringify(out)); }).catch(e => { process.stdout.write(JSON.stringify({ harnessError: String(e?.stack || e) })); });
