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
 * console-shell-bus.cjs - Node harness for juneau-console.js's page-bus integration (message bus addendum,
 * spec 4.1, 4.5, 5.2-5.4, 6.3): card:<id> lifecycle, implicit claims, cmd:<id> ops, declared roles, params,
 * ctx.publish / ctx.subscribe, the error painter and E-JS-46.  Loads the real juneau-bus.js and juneau-console.js
 * into one fresh vm sandbox PER CASE and prints ONE JSON report; every assertion lives in ConsoleBus_Shell_Test.
 *
 *   Usage:  node console-shell-bus.cjs <juneau-bus.js> <juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const [busPath, shellPath] = process.argv.slice(2);
if (!busPath || !shellPath) {
	console.error('usage: node console-shell-bus.cjs <juneau-bus.js> <juneau-console.js>');
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
				for (const c of this.childNodes.slice()) this.removeChild(c);
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
	island.textContent = JSON.stringify(Object.assign({ contractVersion: '1', title: 'T', nav: [], activeNav: [] }, contract));
	doc.body.appendChild(island);
	doc.readyState = 'loading';
	const fetches = [];
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)), debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, Number, AbortController,
		encodeURIComponent, Date,
		localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
		fetch: url => { fetches.push(url); return Promise.reject(new Error('no fetch in harness')); },
		setTimeout: fn => fn(), clearTimeout: () => {}, setInterval: () => 0, clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	env.window.localStorage = sandbox.localStorage;
	if (opts.queue) env.window.JuneauConsoleCards = opts.queue.slice();
	doc.addEventListener('juneau:card-mounted', ev => cardEvents.push('mounted:' + ev.detail.id));
	doc.addEventListener('juneau:card-failed', ev => cardEvents.push('failed:' + ev.detail.id));
	vm.createContext(sandbox);
	const r = { env, doc, errors, warns, cardEvents, fetches, sandbox, threw: null, readyThrew: null };
	try {
		const loadBus = () => { if (!opts.noBus) vm.runInContext(busSrc, sandbox, { filename: 'juneau-bus.js' }); };
		if (!opts.shellFirst) loadBus();
		if (opts.beforeShell) opts.beforeShell(sandbox, r);
		vm.runInContext(shellSrc, sandbox, { filename: 'juneau-console.js' });
		if (opts.shellFirst) loadBus();
	} catch (e) {
		r.threw = { code: e.code, message: e.message };
	}
	r.JC = env.window.JuneauConsole;
	r.bus = env.window.JuneauViews && env.window.JuneauViews.bus;
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

/** `sel` inside card `id` (the shim's selectors have no descendant combinator). */
function q(r, id, sel) { const c = r.doc.getElementById(id); return c ? c.querySelector(sel) : null; }
function text(r, id, sel) { const n = q(r, id, sel); return n ? n.textContent : null; }
function codes(r, id) { return r.doc.getElementById(id).querySelectorAll('.jc-card-bus-error').map(n => n.getAttribute('data-juneau-error')); }
function banner(r) { return r.doc.querySelectorAll('.jc-console-error-item').map(n => n.getAttribute('data-juneau-error')); }
function get(r, topic) { const v = r.bus.get(topic); return v === undefined ? null : JSON.parse(JSON.stringify(v)); }
function owner(r, topic) { const e = r.bus.topics().find(t => t.topic === topic); return e ? e.owner : null; }

/** A card type that records every call; `extra` adds roles / ops / implicit / publishes. */
function recorder(log, extra) {
	return Object.assign({
		render(card, el, ctx) {
			log.push('render:' + (card.src || card.id));
			const p = ctx.document.createElement('p');
			p.className = 'body';
			p.textContent = 'body:' + card.id;
			el.appendChild(p);
		},
		refresh(card) { log.push('refresh:' + card.id); },
		destroy(card) { log.push('destroy:' + card.id); }
	}, extra || {});
}

const out = {};

async function main() {
	// s1. No bus on a page that wires topics: E-JS-46 is FATAL, painted on the banner, at DOMContentLoaded.
	{
		const r = await page({ cards: [{ id: 'a', type: 'rec', subscribes: [{ topic: 'app.x', as: 'refresh' }] }] },
			{ noBus: true, queue: [['rec', recorder([])]] });
		out.s1 = { threw: r.threw, readyThrew: r.readyThrew, banner: banner(r) };
	}
	// s1b. No bus and no wiring: the page mounts; a later ctx.publish is E-JS-46.
	{
		let ctx = null;
		const r = await page({ cards: [{ id: 'k', type: 'probe' }] },
			{ noBus: true, queue: [['probe', { render(card, el, c) { ctx = c; } }]] });
		let late = null;
		try { ctx.publish('app.x', { a: 1 }); } catch (e) { late = e.code; }
		out.s1b = { threw: r.threw, readyThrew: r.readyThrew, late: late };
	}
	// s2. card:<id> lifecycle and ownership; destroy publishes destroyed and then clears the topic.
	{
		const log = [];
		const r = await page({ cards: [{ id: 'k', type: 'rec' }, { id: 'bad', type: 'boom' }] },
			{ queue: [['rec', recorder(log)], ['boom', { render() { throw new Error('kaput'); } }]] });
		const seen = [];
		r.bus.subscribe('card:k', p => seen.push(p === null ? 'cleared' : p.state), { retained: false });
		const mounted = get(r, 'card:k'), failed = get(r, 'card:bad'), ownerK = owner(r, 'card:k');
		r.JC.destroyCard('k');
		out.s2 = { mounted: mounted, failed: failed, owner: ownerK, afterDestroy: seen, retainedAfter: get(r, 'card:k'), log: log };
	}
	// s3. Implicit topics are claimed for the card: a page publish to one is refused (E-JS-49, console-only).
	{
		const r = await page({ cards: [{ id: 't', type: 'grid' }] },
			{ queue: [['grid', recorder([], { implicit: card => ['selection:' + card.id, 'redraw:' + card.id, 'app.grid-picked'] })]] });
		const before = r.errors.length;
		const accepted = r.bus.publish('selection:t', { schemaVersion: 1, ids: ['x'], count: 1 });
		out.s3 = { selectionOwner: owner(r, 'selection:t'), redrawOwner: owner(r, 'redraw:t'), accepted: accepted,
			refused: r.errors.slice(before).some(e => e.indexOf('E-JS-49') >= 0), banner: banner(r) };
	}
	// s4. cmd:<id>: refresh for every type, a handler op, and an unknown op (E-JS-48, inline).
	{
		const log = [];
		const r = await page({ cards: [{ id: 'g', type: 'grid' }] },
			{ queue: [['grid', recorder(log, { ops: { reload(cmd, card) { log.push('op:reload:' + card.id + ':' + cmd.hard); } } })]] });
		r.bus.publish('cmd:g', { schemaVersion: 1, op: 'refresh' });
		await tick();
		r.bus.publish('cmd:g', { schemaVersion: 1, op: 'reload', hard: true });
		r.bus.publish('cmd:g', { schemaVersion: 1, op: 'explode' });
		await tick();
		out.s4 = { log: log, inline: codes(r, 'g'), inlineText: text(r, 'g', '.jc-card-bus-error'), banner: banner(r) };
	}
	// s5. A custom role with map, whenEmpty clear vs keep, and refresh with {retained:false}.
	{
		const calls = [];
		const role = name => (payload, meta, card) => calls.push(name + ':' + card.id + ':' + JSON.stringify(payload));
		const r = await page({
			topics: [{ topic: 'app.level', retain: true, publisher: 'script' }],
			cards: [
				{ id: 'm1', type: 'meter', subscribes: [{ topic: 'app.level', as: 'highlight', map: { hot: 'level' } }] },
				{ id: 'm2', type: 'meter', subscribes: [{ topic: 'app.level', as: 'highlight', map: { hot: 'level' }, whenEmpty: 'keep' }] },
				{ id: 'm3', type: 'meter', subscribes: [{ topic: 'app.level', as: 'refresh' }] }]
		}, {
			queue: [['meter', recorder(calls, { roles: { highlight: role('hl') } })]],
			beforeReady: r => { r.bus.declare('app.level', { retain: true }); r.bus.publish('app.level', { level: 3 }); }
		});
		const atMount = calls.slice();
		r.bus.publish('app.level', { level: 7 });
		r.bus.publish('app.level', { other: 1 });
		r.bus.clear('app.level');
		await tick();
		out.s5 = { atMount: atMount, after: calls.slice(atMount.length), banner: banner(r) };
	}
	// s6. params: awaiting text, encoded URL, no re-render for the same URL, re-render on change, clear, E-JS-51.
	{
		const log = [];
		const r = await page({
			cards: [{ id: 'src', type: 'grid' },
				{ id: 'tasks', type: 'grid', title: 'Tasks', src: '/api/changes/{changeId}/tasks',
					subscribes: [{ topic: 'selection:src', as: 'params', map: { changeId: 'ids.0' }, emptyText: 'Select a change.' }] },
				{ id: 'plain', type: 'grid', src: '/api/{x}', subscribes: [{ topic: 'selection:src', as: 'params', map: { x: 'ids' } }] }]
		}, { queue: [['grid', recorder(log, { implicit: card => ['selection:' + card.id] })]] });
		const awaiting = { text: text(r, 'tasks', '.jc-card-empty'), role: q(r, 'tasks', '.jc-card-empty').getAttribute('role'),
			defaultText: text(r, 'plain', '.jc-card-empty'), card: get(r, 'card:tasks').state, title: text(r, 'tasks', '.jc-card-title') };
		// The table card 'src' owns selection:src; this stands in for its selection change.
		const src = r.bus.owner('src');
		const pub = ids => src.publish('selection:src', { schemaVersion: 1, ids: ids, count: ids.length });
		const before = log.length;
		pub(['c 17']);
		await tick();
		const first = { log: log.slice(before), body: text(r, 'tasks', '.body'), empty: q(r, 'tasks', '.jc-card-empty') === null,
			card: get(r, 'card:tasks').state };
		const mark = log.length;
		pub(['c 17', 'c 99']);
		r.bus.publish('cmd:tasks', { schemaVersion: 1, op: 'refresh' });
		await tick();
		const same = log.slice(mark);
		const mark2 = log.length;
		pub(['c 18']);
		await tick();
		const changed = log.slice(mark2);
		const mark3 = log.length;
		src.clear('selection:src');
		await tick();
		out.s6 = { awaiting: awaiting, first: first, same: same, changed: changed, cleared: log.slice(mark3),
			clearedText: text(r, 'tasks', '.jc-card-empty'), cardAfterClear: get(r, 'card:tasks').state, plainInline: codes(r, 'plain') };
	}
	// s7. ctx.publish / ctx.subscribe before DOMContentLoaded are buffered and run, as the card's owner, at wiring.
	{
		const heard = [];
		const r = await page({ topics: [{ topic: 'app.ping', retain: true, publisher: 'script' }], cards: [{ id: 'p', type: 'pinger' }] }, {
			queue: [['pinger', { render(card, el, ctx) {
				ctx.subscribe('app.pong', v => heard.push(v));
				ctx.publish('app.ping', { n: 1 });
			} }]],
			beforeReady: r => { r.bus.declare('app.ping', { retain: true }); r.bus.declare('app.pong', { retain: true }); }
		});
		r.bus.publish('app.pong', { n: 2 });
		const ping = r.bus.history().filter(h => h.topic === 'app.ping').map(h => h.from);
		out.s7 = { ping: ping, value: get(r, 'app.ping'), heard: JSON.parse(JSON.stringify(heard)), banner: banner(r) };
	}
	// s8. The painter: banner codes, inline codes, console-only codes; validate's problems painted too.
	{
		let sink = null;
		const r = await page({
			topics: [{ topic: 'app.feed:*', retain: false, publisher: 'server' }],
			cards: [{ id: 'h', type: 'viewer', subscribes: [{ topic: 'card:v', as: 'sparkle' }] }, { id: 'v', type: 'viewer' }]
		}, {
			queue: [['viewer', { render() {} }]],
			beforeShell: sb => {
				const bus = sb.window.JuneauViews.bus;
				const orig = bus.onError;
				bus.onError = fn => { sink = fn; return orig(fn); };
			}
		});
		const B = r.bus.BusError;
		sink(new B('E-JS-53', 'bridge down', { paintOn: 'v' }));
		sink(new B('E-JS-58', 'custom banner', { banner: true }));
		sink(new B('E-JS-44', 'cycle', { paintOn: 'v' }));
		sink(new B('E-JS-45', 'subscriber threw', { paintOn: 'v' }));
		sink(new B('E-JS-49', 'not yours', { paintOn: 'nobody' }));
		out.s8 = { banner: banner(r), inlineV: codes(r, 'v'), inlineH: codes(r, 'h'),
			exported: r.JC.BUS_BANNER_CODES ? r.JC.BUS_BANNER_CODES.slice() : null,
			frozen: Object.isFrozen(r.JC.BUS_BANNER_CODES) };
	}
	// s9. A publisher=script topic no script declared by DOMContentLoaded: E-JS-41 on each subscriber.
	{
		const r = await page({
			topics: [{ topic: 'app.region-picked', retain: true, publisher: 'script' }],
			cards: [{ id: 'w', type: 'viewer', subscribes: [{ topic: 'app.region-picked', as: 'refresh' }] }]
		}, { queue: [['viewer', { render() {} }]] });
		out.s9 = { inline: codes(r, 'w'), text: text(r, 'w', '.jc-card-bus-error'), banner: banner(r) };
	}
	// s10. The real <@console> order: the shell auto-mounts and a card renders BEFORE juneau-bus.js has loaded.  The
	// shell must not read the bus at load or inside mount(); ctx calls are buffered and run once DOMContentLoaded finds it.
	{
		const heard = [];
		const r = await page({ topics: [{ topic: 'app.ping', retain: true, publisher: 'script' }], cards: [{ id: 'p', type: 'pinger' }] }, {
			shellFirst: true,
			queue: [['pinger', { render(card, el, c) {
				c.subscribe('app.pong', v => heard.push(v));
				c.publish('app.ping', { n: 1 });
			} }]],
			beforeReady: r => { r.bus.declare('app.ping', { retain: true }); r.bus.declare('app.pong', { retain: true }); }
		});
		r.bus.publish('app.pong', { n: 2 });
		out.s10 = { threw: r.threw, readyThrew: r.readyThrew, busLoaded: !!r.bus, ping: r.bus.history().filter(h => h.topic === 'app.ping').map(h => h.from),
			value: get(r, 'app.ping'), heard: JSON.parse(JSON.stringify(heard)), card: get(r, 'card:p').state, banner: banner(r), errors: r.errors };
	}
	// s11. Destroyed before DOMContentLoaded: the buffered ctx.publish / ctx.subscribe of the dead card never run.
	{
		const heard = [];
		const r = await page({ topics: [{ topic: 'app.ping', retain: true, publisher: 'script' }], cards: [{ id: 'p', type: 'pinger' }] }, {
			queue: [['pinger', { render(card, el, c) {
				c.subscribe('app.pong', v => heard.push(v));
				c.publish('app.ping', { n: 1 });
			} }]],
			beforeReady: r => { r.JC.destroyCard('p'); r.bus.declare('app.ping', { retain: true }); r.bus.declare('app.pong', { retain: true }); }
		});
		r.bus.publish('app.pong', { n: 2 });
		out.s11 = { ping: r.bus.history().filter(h => h.topic === 'app.ping').length, heard: heard.length, banner: banner(r) };
	}
	// s12. After destroyCard nothing of the card is live: cmd:<id>, a role topic and a ctx.subscribe topic all go quiet.
	{
		const log = [];
		const r = await page({
			topics: [{ topic: 'app.level', retain: true, publisher: 'script' }, { topic: 'app.pong', retain: true, publisher: 'script' }],
			cards: [{ id: 'g', type: 'meter', subscribes: [{ topic: 'app.level', as: 'highlight' }] }]
		}, {
			queue: [['meter', recorder(log, {
				roles: { highlight() { log.push('role'); } },
				ops: { reload() { log.push('op'); } }
			})]],
			beforeReady: r => { r.bus.declare('app.level', { retain: true }); r.bus.declare('app.pong', { retain: true }); }
		});
		r.JC.destroyCard('g');
		const mark = log.length;
		try { r.bus.publish('cmd:g', { schemaVersion: 1, op: 'reload' }); } catch (e) { /* refused is fine too */ }
		r.bus.publish('app.level', { level: 9 });
		await tick();
		out.s12 = { after: log.slice(mark) };
		let ctxHeard = 0;
		const r2 = await page({ topics: [{ topic: 'app.pong', retain: true, publisher: 'script' }], cards: [{ id: 'g', type: 'sub' }] }, {
			queue: [['sub', { render(card, el, c) { c.subscribe('app.pong', () => { ctxHeard++; }); } }]],
			beforeReady: r => { r.bus.declare('app.pong', { retain: true }); }
		});
		r2.JC.destroyCard('g');
		r2.bus.publish('app.pong', { n: 1 });
		out.s12.ctxHeard = ctxHeard;
	}
	// s13. A card destroyed before DOMContentLoaded whose async render continues after the bus started: no leak.
	{
		const heard = [];
		let gate, resume = new Promise(res => { gate = res; });
		const r = await page({ topics: [{ topic: 'app.ping', retain: true, publisher: 'script' }], cards: [{ id: 'p', type: 'late' }] }, {
			queue: [['late', { async render(card, el, c) {
				await resume;
				c.subscribe('app.pong', v => heard.push(v));
				c.publish('app.ping', { n: 1 });
			} }]],
			beforeReady: r => { r.JC.destroyCard('p'); r.bus.declare('app.ping', { retain: true }); r.bus.declare('app.pong', { retain: true }); }
		});
		gate();
		await tick();
		r.bus.publish('app.pong', { n: 2 });
		out.s13 = { ping: r.bus.history().filter(h => h.topic === 'app.ping').length, heard: heard.length,
			pongSubs: r.bus.topics().filter(t => t.topic === 'app.pong').map(t => t.subscribers) };
	}
	// s14. A throwing handler.implicit must not abort the page wiring: the other cards still wire.
	{
		const log = [];
		const r = await page({ cards: [{ id: 'bad', type: 'boom' }, { id: 'ok', type: 'grid' }] }, {
			queue: [['boom', recorder(log, { implicit: () => { throw new Error('implicit kaput'); } })],
				['grid', recorder(log, { implicit: card => ['selection:' + card.id] })]]
		});
		out.s14 = { readyThrew: r.readyThrew, okCard: (get(r, 'card:ok') || {}).state, badCard: (get(r, 'card:bad') || {}).state,
			selectionOwner: owner(r, 'selection:ok'), badInline: codes(r, 'bad'), errors: r.errors.filter(e => e.indexOf('implicit kaput') >= 0).length };
	}
	// s15. as:"toString" is not a role: Object.prototype members are not handler roles.
	{
		const calls = [];
		const r = await page({
			topics: [{ topic: 'app.x', retain: true, publisher: 'script' }],
			cards: [{ id: 'c', type: 'plain', subscribes: [{ topic: 'app.x', as: 'toString' }] }]
		}, { queue: [['plain', recorder(calls)]], beforeReady: r => { r.bus.declare('app.x', { retain: true }); } });
		out.s15 = { subscribers: r.bus.topics().filter(t => t.topic === 'app.x').map(t => t.subscribers) };
	}
	// s16. A throwing bus.publish for the phase must not swallow the card's DOM event.
	{
		const log = [];
		const r = await page({ cards: [{ id: 'k', type: 'rec' }] }, {
			queue: [['rec', recorder(log)]],
			beforeReady: r => {
				const real = r.bus.owner;
				r.bus.owner = id => { const o = real(id); return Object.assign({}, o, { publish: (t, p) => { if (t === 'card:' + id && p.state === 'mounted') throw new Error('publish kaput'); return o.publish(t, p); } }); };
			}
		});
		const before = r.cardEvents.length;
		let threw = null;
		try { await r.JC.refreshCard('k'); } catch (e) { threw = e.message; }
		out.s16 = { events: r.cardEvents.slice(before), threw: threw };
	}
	// s17. Params cards: a render that settles after the params were cleared is stale, and refresh on a card still
	// awaiting its params is a no-op.
	{
		const log = [];
		let gate, hold = new Promise(res => { gate = res; });
		const r = await page({
			cards: [{ id: 'src', type: 'grid' },
				{ id: 'tasks', type: 'late', src: '/api/{x}', subscribes: [{ topic: 'selection:src', as: 'params', map: { x: 'ids.0' } }] }]
		}, { queue: [['grid', recorder([], { implicit: card => ['selection:' + card.id] })],
			['late', recorder(log, { render(card) { log.push('render:' + card.src); return hold; } })]] });
		const src = r.bus.owner('src');
		src.publish('selection:src', { schemaVersion: 1, ids: ['a'], count: 1 });
		src.clear('selection:src');
		await tick();
		const phaseAwaiting = get(r, 'card:tasks').state;
		const evBefore = r.cardEvents.length;
		gate();
		await tick();
		const staleEvents = r.cardEvents.slice(evBefore).filter(e => e === 'mounted:tasks').length;
		const phaseAfter = get(r, 'card:tasks').state;
		const mark = log.length;
		r.bus.publish('cmd:tasks', { schemaVersion: 1, op: 'refresh' });
		let threw = null;
		try { await r.JC.refreshCard('tasks'); } catch (e) { threw = e.code; }
		out.s17 = { phaseAwaiting: phaseAwaiting, staleEvents: staleEvents, phaseAfter: phaseAfter, refreshLog: log.slice(mark), threw: threw };
	}
}

main().then(
	() => process.stdout.write(JSON.stringify(out)),
	e => { process.stdout.write(JSON.stringify({ harnessError: String(e && e.stack || e), partial: out })); });
