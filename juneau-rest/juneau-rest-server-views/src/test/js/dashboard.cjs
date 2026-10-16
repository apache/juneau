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
 * dashboard.cjs - dependency-free Node harness for juneau-dashboard.js: the section and decoder registries, the
 * JuneauConsoleDashboard queue, the share-link codec, payload validation, restore order, quick ranges, the status
 * text and a seeded share-link fuzz.  The file runs with no document, so only the pure functions are reached (through
 * JuneauDashboard._internals).  Every assertion lives in ConsoleDashboard_Test.
 *
 *   Usage:  node dashboard.cjs <juneau-dashboard.js>
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const [dashPath] = process.argv.slice(2);
if (!dashPath) {
	console.error('usage: node dashboard.cjs <juneau-dashboard.js>');
	process.exit(2);
}
const src = fs.readFileSync(dashPath, 'utf8');

function load(opts) {
	opts = opts || {};
	const errors = [], warnings = [], timers = [];
	const ctx = {
		console: { error: m => errors.push(String(m)), warn: m => warnings.push(String(m)), log: () => {} },
		TextEncoder, TextDecoder, btoa, atob,
		// Timers never fire on their own; a later flush(r) runs the live ones.
		setTimeout: (fn, ms) => { timers.push({ fn, ms, live: true }); return timers.length; },
		clearTimeout: id => { if (id > 0 && timers[id - 1]) timers[id - 1].live = false; }
	};
	ctx.window = ctx;
	if (opts.queue) ctx.JuneauConsoleDashboard = opts.queue;
	vm.createContext(ctx);
	vm.runInContext(src, ctx, { filename: 'juneau-dashboard.js' });
	return { w: ctx, errors, warnings, timers, I: ctx.JuneauDashboard._internals };
}

function caught(fn) {
	try { fn(); return null; } catch (e) { return String(e && e.message || e); }
}

// A minimal DOM for the rail-section tests: elements with children, attributes, listeners and fire(type, event).
function fakeDoc() {
	const doc = {
		createElement(tag) {
			return {
				tagName: String(tag).toUpperCase(), children: [], attrs: {}, listeners: {}, ownerDocument: doc,
				textContent: '', className: '', value: '', checked: false, type: '', placeholder: '',
				appendChild(c) { this.children.push(c); c.parentNode = this; return c; },
				removeChild(c) { this.children.splice(this.children.indexOf(c), 1); return c; },
				get firstChild() { return this.children[0] || null; },
				setAttribute(k, v) { this.attrs[k] = String(v); },
				getAttribute(k) { return k in this.attrs ? this.attrs[k] : null; },
				removeAttribute(k) { delete this.attrs[k]; },
				addEventListener(t, fn) { (this.listeners[t] = this.listeners[t] || []).push(fn); },
				fire(t, ev) { for (const fn of (this.listeners[t] || []).slice()) fn(Object.assign({ preventDefault() {} }, ev)); },
				focus() { doc.activeElement = this; },
				select() { this.selected = true; }
			};
		},
		listeners: {},
		addEventListener(t, fn) { (this.listeners[t] = this.listeners[t] || []).push(fn); },
		fire(t, ev) { for (const fn of (this.listeners[t] || []).slice()) fn(ev); },
		createTextNode(t) { return { nodeType: 3, textContent: t }; }
	};
	return doc;
}

function findAll(n, pred, acc) {
	acc = acc || [];
	for (const c of n.children || []) {
		if (pred(c)) acc.push(c);
		findAll(c, pred, acc);
	}
	return acc;
}

// Runs the live timers once (they were queued by setTimeout and never fire on their own).
function flush(r) {
	for (const t of r.timers.slice()) if (t.live) { t.live = false; t.fn(); }
}

const copy = v => JSON.parse(JSON.stringify(v));

function ctxFor(s, init, pubs, subs) {
	return {
		id: s.id, topic: s.topic, viewId: 'd', args: {}, initial: init,
		publish: p => { pubs.push(copy(p)); return true; },
		subscribe: (t, fn) => { subs[t] = fn; return () => {}; }
	};
}

function fakeBus() {
	const published = [];
	return {
		published,
		owner: id => ({ id, publish: (t, p) => { published.push({ from: id, topic: t, payload: copy(p) }); return true; }, subscribe: () => () => {} })
	};
}

// The page-level bus: anonymous subscribe (the last subscriber per topic is kept) and publish.
function pageBus() {
	const subs = {}, published = [];
	return {
		subs, published,
		subscribe: (t, fn) => { subs[t] = fn; return () => {}; },
		publish: (t, p) => { published.push({ topic: t, payload: copy(p) }); return true; }
	};
}

// Lets the vm's promise callbacks run.
const tick = () => new Promise(res => setImmediate(res));

(async () => {
	const out = {};

	// 1. Registries and the pre-load queue.
	try {
		const r = load({ queue: [
			['section', 'app.queued', { mount() {} }],
			['decoder', 'bare', () => null],
			['nope', 'app.x', () => null],
			'junk'
		] });
		const reg = r.w.JuneauDashboard;
		const queueErrors = r.errors.slice();
		out.registry = {
			queued: r.I.names('section'),
			queueErrors,
			bare: caught(() => reg.section.register('note', { mount() {} })),
			invalid: caught(() => reg.section.register('App.note', { mount() {} })),
			nonString: caught(() => reg.decoder.register(7, () => null)),
			noMount: caught(() => reg.section.register('app.empty', {})),
			notFn: caught(() => reg.decoder.register('app.dec', {})),
			ok: caught(() => reg.decoder.register('app.dec', () => null)),
			dup: caught(() => reg.decoder.register('app.dec', () => null)),
			pushed: caught(() => r.w.JuneauConsoleDashboard.push(['section', 'app.pushed', { mount() {} }])),
			pushJunk: caught(() => r.w.JuneauConsoleDashboard.push(['section', 'app.y'])),
			sections: r.I.names('section'),
			decoders: r.I.names('decoder'),
			frozen: Object.isFrozen(reg) && Object.isFrozen(reg.section) && Object.isFrozen(r.w.JuneauViews.dashboard),
			version: reg.version,
			nameRe: r.I.EXTENSION_NAME_RE.source,
			clamp: [r.I.clampWidth(100, 280), r.I.clampWidth(999, 280), r.I.clampWidth(300.4, 280), r.I.clampWidth('x', 280), r.I.clampWidth(null, 250)]
		};
	} catch (e) { out.registry = { crash: String(e && e.stack || e) }; }

	// 2. The share-link codec and payload validation.
	try {
		const I = load().I;
		const P = { ns: 'app.dash', version: 2, topics: ['filter:range', 'filter:q', 'zoom:daily', 'legend:daily'], shareLink: true, decoders: [] };
		const range = { schemaVersion: 1, viewId: 'd', options: { range: '30d' }, start: '2026-09-11T00:00:00.000Z', end: '2026-10-11T00:00:00.000Z' };
		const zoom = { schemaVersion: 1, start: 10, end: 90, startValue: null, endValue: null };
		const enc = I.encodeLink(P, { 'filter:range': range, 'zoom:daily': zoom, 'filter:other': range });
		const dec = I.decodeShareLink(enc, P);
		const raw = s => '#s=' + Buffer.from(s, 'utf8').toString('base64url');
		const err = h => I.decodeShareLink(h, P).error;
		out.codec = {
			prefix: enc.slice(0, 3),
			noPadding: !/[=+/]/.test(enc.slice(3)),
			roundTrip: JSON.stringify(dec.t),
			nullProto: Object.getPrototypeOf(dec.t) === null && Object.getPrototypeOf(dec.t['filter:range']) === null,
			unicode: I.decodeShareLink(I.encodeLink(P, { 'filter:q': { schemaVersion: 1, viewId: 'd', search: 'café ✓' } }), P).t['filter:q'].search,
			errors: [
				err('#s=' + 'A'.repeat(8200)),
				err('#s=ab$c'),
				err('#s=' + Buffer.from([0xff, 0xfe]).toString('base64url')),
				err(raw('{nope')),
				err(raw('{"v":2,"ns":"app.dash","t":{"filter:q":{"schemaVersion":1,"viewId":"d","x":{"__proto__":1}}}}')),
				err(raw('{"v":2,"ns":"app.dash","t":{"a":' + '['.repeat(70) + ']'.repeat(70) + '}}')),
				err(raw('[1,2]')),
				err(raw('{"v":2,"ns":"other.dash","t":{}}')),
				err(raw('{"v":1,"ns":"app.dash","t":{}}')),
				err(raw('{"v":2,"ns":{"a":1},"t":{}}'))
			]
		};
		const cp = (t, v) => { const x = I.checkPayload(t, v); return x === null ? null : JSON.stringify(x); };
		out.validate = {
			filterOk: cp('filter:q', { schemaVersion: 1, viewId: 'd', search: 'x', extra: 1 }),
			noViewId: cp('filter:q', { schemaVersion: 1, search: 'x' }),
			badVersion: cp('filter:q', { schemaVersion: 2, viewId: 'd' }),
			badSearch: cp('filter:q', { schemaVersion: 1, viewId: 'd', search: 3 }),
			badColumns: cp('filter:q', { schemaVersion: 1, viewId: 'd', columns: [] }),
			oversize: cp('filter:q', { schemaVersion: 1, viewId: 'd', search: 'x'.repeat(5000) }),
			zoomOk: cp('zoom:daily', { schemaVersion: 1, start: 0, end: 100, startValue: 'Mon', endValue: 1700000000000 }),
			zoomBad: cp('zoom:daily', { schemaVersion: 1, start: 120 }),
			legendOk: cp('legend:daily', { schemaVersion: 1, selected: { a: true, b: false } }),
			legendBad: cp('legend:daily', { schemaVersion: 1, selected: { a: 'yes' } }),
			otherFamily: cp('cmd:daily', { schemaVersion: 1 }),
			array: cp('filter:q', [1]),
			noColon: cp('filterq', { schemaVersion: 1, viewId: 'd' })
		};
	} catch (e) { out.codec = { crash: String(e && e.stack || e) }; }

	// 3. Restore order.
	try {
		const I = load().I;
		const P = { ns: 'app.dash', version: 2, topics: ['filter:q'], shareLink: true, decoders: [] };
		const saved = { v: 2, topics: { 'filter:q': { schemaVersion: 1, viewId: 'd', search: 'saved' } } };
		const linkHash = I.encodeLink(P, { 'filter:q': { schemaVersion: 1, viewId: 'd', search: 'link' } });
		const decs = new Map([
			['app.throws', () => { throw new Error('boom'); }],
			['app.empty', () => ({})],
			['app.array', () => [1]],
			['app.wins', () => ({ 'filter:q': { schemaVersion: 1, viewId: 'd', search: 'decoder' }, 'filter:unknown': { schemaVersion: 1, viewId: 'd' } })],
			['app.proto', () => JSON.parse('{"filter:q":{"schemaVersion":1,"viewId":"d","__proto__":{}}}')]
		]);
		const run = (persist, hash, savedValue) => {
			const warns = [];
			const res = I.resolveInitial({ persist, hash, saved: savedValue, decoders: decs, location: {}, warn: m => warns.push(m) });
			return { source: res.source, topics: res.topics ? JSON.stringify(res.topics) : null, deleteSaved: res.deleteSaved, stripFragment: res.stripFragment, warns };
		};
		const withDecoders = list => Object.assign({}, P, { decoders: list });
		out.restore = {
			decoderWins: run(withDecoders(['app.missing', 'app.throws', 'app.empty', 'app.array', 'app.wins']), linkHash, saved),
			protoDecoder: run(withDecoders(['app.proto']), linkHash, saved),
			linkWins: run(P, linkHash, saved),
			linkOff: run(Object.assign({}, P, { shareLink: false }), linkHash, saved),
			badLink: run(P, '#s=!!', saved),
			savedOnly: run(P, '', saved),
			oldVersion: run(P, '', { v: 1, topics: {} }),
			badSaved: run(P, '', 'junk'),
			none: run(P, '#other', null)
		};
	} catch (e) { out.restore = { crash: String(e && e.stack || e) }; }

	// 3b. A decoder whose result has a throwing getter is "not plain JSON"; the restore goes on to the next source.
	try {
		const I = load().I;
		const P = { ns: 'app.dash', version: 2, topics: ['filter:q'], shareLink: false, decoders: ['app.getter'] };
		const bad = {};
		Object.defineProperty(bad, 'filter:q', { enumerable: true, get() { throw new Error('getter'); } });
		const warns = [];
		const res = I.resolveInitial({ persist: P, hash: '', saved: { v: 2, topics: { 'filter:q': { schemaVersion: 1, viewId: 'd', search: 's' } } },
			decoders: new Map([['app.getter', () => bad]]), location: {}, warn: m => warns.push(m) });
		out.restoreGetter = { source: res.source, topics: res.topics ? JSON.stringify(res.topics) : null, warns };
	} catch (e) { out.restoreGetter = { crash: String(e && e.stack || e) }; }

	// 4. Restore runs once; a decoder registered after it is E-JS-97.
	try {
		const r = load();
		const reg = r.w.JuneauDashboard;
		out.timing = {
			before: caught(() => reg.decoder.register('app.early', () => null)),
			initial: r.w.JuneauViews.dashboard.initial('filter:x'),
			late: caught(() => reg.decoder.register('app.late', () => null)),
			lateSection: caught(() => reg.section.register('app.sec', { mount() {} }))
		};
	} catch (e) { out.timing = { crash: String(e && e.stack || e) }; }

	// 5. Range: presets, aria-pressed, the custom inputs and a restored custom range.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const range = { id: 'range', kind: 'range', topic: 'filter:range', presets: ['7d', '30d', 'ytd'], custom: true, defaultPreset: '30d' };
		const pubs = [];
		const el = doc.createElement('fieldset');
		I.BUILTIN.range.mount(el, range, ctxFor(range, null, pubs, {}));
		const buttons = findAll(el, n => n.tagName === 'BUTTON');
		const inputs = findAll(el, n => n.tagName === 'INPUT');
		const pressed = () => buttons.slice(0, 3).map(b => b.getAttribute('aria-pressed'));
		const span = p => Date.parse(p.end) - Date.parse(p.start);
		const pressedInitial = pressed();
		buttons[0].fire('click');
		inputs[0].value = '2026-10-01T00:00';
		inputs[1].value = '2026-09-01T00:00';
		buttons[3].fire('click');                       // Apply with start >= end: nothing is published
		const invalid = [inputs[0].getAttribute('aria-invalid'), inputs[1].getAttribute('aria-invalid')];
		const countAfterInvalid = pubs.length;
		inputs[1].value = '2026-10-08T12:30';
		buttons[3].fire('click');
		const restoredPubs = [];
		const el2 = doc.createElement('fieldset');
		I.BUILTIN.range.mount(el2, range, ctxFor(range, { schemaVersion: 1, viewId: 'd', options: { range: 'custom' },
			start: '2026-01-02T03:04:00.000Z', end: '2026-01-05T00:00:00.000Z' }, restoredPubs, {}));
		const in2 = findAll(el2, n => n.tagName === 'INPUT');
		const loosePubs = [];
		I.BUILTIN.range.mount(doc.createElement('fieldset'), range, ctxFor(range, { schemaVersion: 1, viewId: 'd', options: { range: 'custom' },
			start: '2026', end: '2026-02' }, loosePubs, {}));
		const v = p => I.BUILTIN.range.validate(p, range);
		out.range = {
			labels: buttons.slice(0, 3).map(b => b.textContent), inputTypes: inputs.map(i => i.type), pressedInitial,
			firstRange: pubs[0].options.range, firstDays: span(pubs[0]) / 86400000, firstFloored: Date.parse(pubs[0].end) % 60000 === 0,
			firstKeys: Object.keys(pubs[0]),
			clicked: pubs[1].options.range, clickedHours: span(pubs[1]) / 3600000,
			invalid, countAfterInvalid, custom: pubs[pubs.length - 1], pressedAfterCustom: pressed(),
			restored: restoredPubs[0], loose: loosePubs[0], restoredInputs: [in2[0].value, in2[1].value],
			validate: [
				v({ schemaVersion: 1, viewId: 'd', options: { range: '30d' } }),
				v({ schemaVersion: 1, viewId: 'd', options: { range: '90d' } }),
				v({ schemaVersion: 1, viewId: 'd', options: { range: 'custom' }, start: '2026-01-02T00:00:00Z', end: '2026-01-01T00:00:00Z' }),
				I.BUILTIN.range.validate({ schemaVersion: 1, viewId: 'd', options: { range: 'custom' }, start: '2026-01-01T00:00:00Z', end: '2026-01-02T00:00:00Z' },
					Object.assign({}, range, { custom: false }))
			],
			quick: [I.quickRange('7d', Date.UTC(2026, 9, 11, 13, 40, 59, 999)), I.quickRange('12h', Date.UTC(2026, 9, 11, 13, 40, 30)),
				I.quickRange('ytd', Date.UTC(2026, 9, 11, 13, 40)), I.quickRange('7w', 0)],
			parse: [I.parseUtcLocal('2026-10-01T00:00'), I.parseUtcLocal('2026-10-01T00:00:30'), I.parseUtcLocal('2026-10-01'), I.parseUtcLocal('nope')]
		};
	} catch (e) { out.range = { crash: String(e && e.stack || e) }; }

	// 6. Search: a restored value, the 250 ms pause and Enter.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const search = { id: 'q', kind: 'search', topic: 'filter:q', placeholder: 'Find' };
		const pubs = [];
		const el = doc.createElement('fieldset');
		I.BUILTIN.search.mount(el, search, ctxFor(search, { schemaVersion: 1, viewId: 'd', search: 'abc' }, pubs, {}));
		const input = findAll(el, n => n.tagName === 'INPUT')[0];
		const initialValue = input.value;
		input.value = 'abcd';
		input.fire('input');
		const beforePause = pubs.length;
		const pause = r.timers.filter(t => t.live).map(t => t.ms);
		flush(r);
		input.value = 'abcde';
		input.fire('input');
		const liveBeforeEnter = r.timers.filter(t => t.live).length;
		input.fire('keydown', { key: 'Enter' });
		const liveAfterEnter = r.timers.filter(t => t.live).length;
		flush(r);                                       // the cancelled pause must not publish again
		out.search = { initialValue, type: input.type, placeholder: input.placeholder, label: input.getAttribute('aria-label'),
			pubs: pubs.map(p => p.search), beforePause, pause, liveBeforeEnter, liveAfterEnter };
	} catch (e) { out.search = { crash: String(e && e.stack || e) }; }

	// 7. Choice: values from the frame's dict, counts, the cap, kept (greyed) selections, all-selected omits columns.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const choice = { id: 'reasons', kind: 'choice', topic: 'filter:reasons', source: 'orders', dim: 'reason', max: 3 };
		const frames = { orders: { source: 'orders', version: 1, rowCount: 10, dims: [],
			columns: { reason: { codes: Int32Array.from([0, 0, 0, 0, 1, 1, 1, 2, 2, 3]), dict: ['late', 'lost', 'damaged', 'wrong-item'] } } } };
		r.w.JuneauViews.data = { get: h => frames[h.source] && frames[h.source].version === h.version ? frames[h.source] : null };
		const ready = version => ({ schemaVersion: 1, source: 'orders', version, status: 'ready', dims: [] });
		const rows = el => findAll(el, n => n.tagName === 'LABEL').map(l => ({
			value: l.children.filter(c => c.nodeType === 3).map(c => c.textContent).join(''),
			count: (l.children.find(c => c.className === 'jc-dash-count') || {}).textContent || null,
			checked: l.children[0].checked, missing: l.getAttribute('data-missing') !== null
		}));
		const buttonText = el => findAll(el, n => n.tagName === 'BUTTON').map(b => b.textContent);

		const p1 = [], s1 = {};
		const e1 = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(e1, choice, ctxFor(choice, { schemaVersion: 1, viewId: 'd', columns: { reason: ['late', 'gone'] } }, p1, s1));
		const restoredFirst = p1[0];
		s1['data:orders'](ready(1));
		const shown = rows(e1), more = buttonText(e1);
		findAll(e1, n => n.tagName === 'BUTTON')[0].fire('click');
		const all = rows(e1).length, less = buttonText(e1);

		const p2 = [], s2 = {};
		const e2 = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(e2, choice, ctxFor(choice, null, p2, s2));
		s2['data:orders'](ready(1));
		let box = findAll(e2, n => n.tagName === 'INPUT')[0];
		box.checked = false;
		box.fire('change');
		const unchecked = p2[p2.length - 1];
		box = findAll(e2, n => n.tagName === 'INPUT')[0];
		box.checked = true;
		box.fire('change');
		const rechecked = p2[p2.length - 1];
		const countBefore = p2.length;
		s2['data:orders'](Object.assign(ready(1), { status: 'loading' }));
		s2['data:orders'](ready(1));                    // the same version again: ignored
		const ignored = p2.length === countBefore && rows(e2).length === 3;
		// Untick late, and the next frame no longer has it; it stays as a greyed, unticked row and can be ticked again.
		box = findAll(e2, n => n.tagName === 'INPUT')[0];
		box.checked = false;
		box.fire('change');
		frames.orders = Object.assign({}, frames.orders, { version: 2, rowCount: 6,
			columns: { reason: { codes: Int32Array.from([0, 0, 0, 1, 1, 2]), dict: ['lost', 'damaged', 'wrong-item'] } } });
		s2['data:orders'](ready(2));
		const gone = rows(e2);
		findAll(e2, n => n.tagName === 'INPUT')[3].checked = true;
		findAll(e2, n => n.tagName === 'INPUT')[3].fire('change');
		const retick = p2[p2.length - 1];
		frames.orders = Object.assign({}, frames.orders, { version: 3, columns: { reason: Float64Array.from([1]) } });
		s2['data:orders'](ready(3));
		const notDict = findAll(e2, n => n.tagName === 'P').map(n => n.textContent);
		out.choice = { restoredFirst, shown, more, all, less, first: p2[0], unchecked, rechecked, ignored, gone, retick, notDict,
			validate: [
				I.BUILTIN.choice.validate({ schemaVersion: 1, viewId: 'd', columns: { reason: ['a'] } }, choice),
				I.BUILTIN.choice.validate({ schemaVersion: 1, viewId: 'd', columns: { other: ['a'] } }, choice),
				I.BUILTIN.choice.validate({ schemaVersion: 1, viewId: 'd', columns: { reason: [1] } }, choice),
				I.BUILTIN.choice.validate({ schemaVersion: 1, viewId: 'd' }, choice)
			] };
	} catch (e) { out.choice = { crash: String(e && e.stack || e) }; }

	// 7c. Choice: unticked values survive a reload, Select all, and the cap on departed values.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const choice = { id: 'reasons', kind: 'choice', topic: 'filter:reasons', source: 'orders', dim: 'reason', max: 2 };
		const frames = { orders: { source: 'orders', version: 1, rowCount: 4, dims: [],
			columns: { reason: { codes: Int32Array.from([0, 0, 1, 2]), dict: ['lost', 'wrong-item', 'damaged'] } } } };
		r.w.JuneauViews.data = { get: h => frames[h.source] && frames[h.source].version === h.version ? frames[h.source] : null };
		const ready = version => ({ schemaVersion: 1, source: 'orders', version, status: 'ready', dims: [] });
		const rows = el => findAll(el, n => n.tagName === 'LABEL').map(l => ({
			value: l.children.filter(c => c.nodeType === 3).map(c => c.textContent).join(''),
			checked: l.children[0].checked, missing: l.getAttribute('data-missing') !== null
		}));
		const buttonText = el => findAll(el, n => n.tagName === 'BUTTON').map(b => b.textContent);

		// A reload: the filter and the unticked values come back; the frame (already filtered) holds only the ticked ones.
		const pubs = [], subs = {};
		const el = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(el, Object.assign({}, choice, { max: 5 }), ctxFor(choice, { schemaVersion: 1, viewId: 'd',
			columns: { reason: ['lost', 'wrong-item'] }, options: { deselected: ['late', 'damaged'] } }, pubs, subs));
		const first = pubs[0];
		const full = frames.orders;
		frames.orders = Object.assign({}, full, { rowCount: 3, columns: { reason: { codes: Int32Array.from([0, 0, 1]), dict: ['lost', 'wrong-item'] } } });
		subs['data:orders'](ready(1));
		frames.orders = full;
		const restored = rows(el), restoredButtons = buttonText(el);
		const inputs = () => findAll(el, n => n.tagName === 'INPUT');
		const byValue = v => inputs()[rows(el).map(x => x.value).indexOf(v)];
		let b = byValue('damaged'); b.checked = true; b.fire('change');
		const afterDamaged = pubs[pubs.length - 1];
		b = byValue('late'); b.checked = true; b.fire('change');
		const afterLate = pubs[pubs.length - 1], afterLateButtons = buttonText(el);

		// Select all clears the filter without listing columns.
		const pubs2 = [], subs2 = {};
		const el2 = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(el2, choice, ctxFor(choice, { schemaVersion: 1, viewId: 'd', columns: { reason: ['lost'] },
			options: { deselected: ['damaged'] } }, pubs2, subs2));
		subs2['data:orders'](ready(1));
		const before2 = buttonText(el2);
		findAll(el2, n => n.tagName === 'BUTTON').filter(x => x.textContent === 'Select all')[0].fire('click');
		const after2 = { published: pubs2[pubs2.length - 1], buttons: buttonText(el2), rows: rows(el2) };

		// Departed unticked values are capped at max (Show all lifts the cap); ticked ones are always listed.
		const gone = [];
		for (let i = 0; i < 6; i++) gone.push('g' + i);
		const pubs3 = [], subs3 = {};
		const el3 = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(el3, choice, ctxFor(choice, { schemaVersion: 1, viewId: 'd', columns: { reason: ['lost', 'k1', 'k2', 'k3'] },
			options: { deselected: gone } }, pubs3, subs3));
		subs3['data:orders'](ready(1));
		const capped = rows(el3).map(x => x.value), cappedButtons = buttonText(el3);
		findAll(el3, n => n.tagName === 'BUTTON')[0].fire('click');
		const lifted = rows(el3).map(x => x.value);
		// A filter payload stays within the 4 KiB cap: the deselected list shrinks from its end, then goes.
		const bytes = p => Buffer.byteLength(JSON.stringify(p));
		const pad = (c, i) => c + String(i).padStart(37, '0');
		const capOf = (nSel, nGone) => {
			const pubsC = [];
			const sel = [], off = [];
			for (let i = 0; i < nSel; i++) sel.push(pad('v', i));
			for (let i = 0; i < nGone; i++) off.push(pad('z', i));
			I.BUILTIN.choice.mount(doc.createElement('fieldset'), Object.assign({}, choice, { max: 500 }),
				ctxFor(choice, { schemaVersion: 1, viewId: 'd', columns: { reason: sel }, options: { deselected: off } }, pubsC, {}));
			const p = pubsC[0];
			return { bytes: bytes(p), columns: p.columns.reason.length, gone: p.options ? p.options.deselected.length : null,
				prefix: p.options ? p.options.deselected.every((x, i) => x === off[i]) : null };
		};
		const capShrunk = capOf(10, 150), capDropped = capOf(98, 1), capSmall = capOf(10, 5);
		// With max 5, four frame values and two departed unticked ones all fit: no Show all button.
		const fit = { id: 'reasons', kind: 'choice', topic: 'filter:reasons', source: 'orders', dim: 'reason', max: 5 };
		const frames4 = { source: 'orders', version: 1, rowCount: 4, dims: [],
			columns: { reason: { codes: Int32Array.from([0, 1, 2, 3]), dict: ['a', 'b', 'c', 'd'] } } };
		frames.orders = frames4;
		const subs4 = {};
		const el4 = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(el4, fit, ctxFor(fit, { schemaVersion: 1, viewId: 'd', columns: { reason: ['a', 'b'] },
			options: { deselected: ['x', 'y'] } }, [], subs4));
		subs4['data:orders'](ready(1));
		const noShowAll = { rows: rows(el4).length, buttons: buttonText(el4) };
		const v = p => I.BUILTIN.choice.validate(p, choice);
		out.choice2 = { capShrunk, capDropped, capSmall, noShowAll, first, restored, restoredButtons, afterDamaged, afterLate, afterLateButtons, before2, after2, capped, cappedButtons, lifted,
			validate: [
				v({ schemaVersion: 1, viewId: 'd', columns: { reason: ['a'] }, options: { deselected: ['b'] } }),
				v({ schemaVersion: 1, viewId: 'd', options: { deselected: [1] } }),
				v({ schemaVersion: 1, viewId: 'd', options: { deselected: 'b' } }),
				v({ schemaVersion: 1, viewId: 'd', options: { other: 1 } }),
				v({ schemaVersion: 1, viewId: 'd', options: [] })
			] };
	} catch (e) { out.choice2 = { crash: String(e && e.stack || e) }; }

	// 7b. Rail accessibility: focus follows a rebuilt choice control, the Apply reason, the missing-row cue, impossible dates.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const focused = [];
		const mk = doc.createElement;
		doc.createElement = tag => { const n = mk(tag); const f = n.focus; n.focus = function () { focused.push(this); f.call(this); }; return n; };
		const choice = { id: 'reasons', kind: 'choice', topic: 'filter:reasons', source: 'orders', dim: 'reason', max: 2 };
		const frames = { orders: { source: 'orders', version: 1, rowCount: 6, dims: [],
			columns: { reason: { codes: Int32Array.from([0, 0, 0, 1, 1, 2]), dict: ['late', 'lost', 'damaged'] } } } };
		r.w.JuneauViews.data = { get: h => frames[h.source] && frames[h.source].version === h.version ? frames[h.source] : null };
		const subs = {};
		const el = doc.createElement('fieldset');
		I.BUILTIN.choice.mount(el, choice, ctxFor(choice, { schemaVersion: 1, viewId: 'd', columns: { reason: ['late', 'gone'] } }, [], subs));
		subs['data:orders']({ schemaVersion: 1, source: 'orders', version: 1, status: 'ready', dims: [] });
		const labelOf = n => n.parentNode.children.filter(c => c.nodeType === 3).map(c => c.textContent).join('');
		const inputs = () => findAll(el, n => n.tagName === 'INPUT');
		focused.length = 0;
		const first = inputs()[0];
		first.focus(); focused.length = 0;
		first.checked = false;
		first.fire('change');
		const afterToggle = { count: focused.length, same: focused[0] === inputs()[0], value: focused[0] ? labelOf(focused[0]) : null, active: doc.activeElement === inputs()[0], rebuilt: first !== inputs()[0] };
		findAll(el, n => n.tagName === 'BUTTON')[0].focus(); focused.length = 0;
		findAll(el, n => n.tagName === 'BUTTON')[0].fire('click');
		const afterMore = { count: focused.length, text: focused[0] ? focused[0].textContent : null, active: doc.activeElement === findAll(el, n => n.tagName === 'BUTTON')[0] };
		// A new ready version rebuilds the boxes; focus follows to the new box of the same value.
		const boxBefore = inputs()[1];
		boxBefore.focus(); focused.length = 0;
		frames.orders = { source: 'orders', version: 2, rowCount: 6, dims: [], columns: frames.orders.columns };
		subs['data:orders']({ schemaVersion: 1, source: 'orders', version: 2, status: 'ready', dims: [] });
		const afterVersion = { count: focused.length, same: focused[0] === inputs()[1], rebuilt: boxBefore !== inputs()[1], active: doc.activeElement === inputs()[1], value: focused[0] ? labelOf(focused[0]) : null };
		const missing = findAll(el, n => n.tagName === 'LABEL').filter(l => l.getAttribute('data-missing') !== null)
			.map(l => l.children.filter(c => c.nodeType === 3).map(c => c.textContent).join('') + '|' + l.children.filter(c => c.nodeType !== 3).slice(1).map(c => c.textContent).join('|'));

		// Select all removes its own button: focus falls to the first box.
		findAll(el, n => n.tagName === 'BUTTON').filter(x => x.textContent === 'Select all')[0].focus(); focused.length = 0;
		findAll(el, n => n.tagName === 'BUTTON').filter(x => x.textContent === 'Select all')[0].fire('click');
		const afterSelectAll = { count: focused.length, first: focused[0] === inputs()[0], active: doc.activeElement === inputs()[0],
			buttons: findAll(el, n => n.tagName === 'BUTTON').map(x => x.textContent) };

		const range = { id: 'range', kind: 'range', topic: 'filter:range', presets: ['7d'], custom: true, defaultPreset: '7d' };
		const re = doc.createElement('fieldset');
		I.BUILTIN.range.mount(re, range, ctxFor(range, null, [], {}));
		const ri = findAll(re, n => n.tagName === 'INPUT');
		const apply = findAll(re, n => n.tagName === 'BUTTON')[1];
		const err = findAll(re, n => n.tagName === 'P')[0];
		const state = () => ({ text: err.textContent, role: err.getAttribute('role'), described: ri.map(i => i.getAttribute('aria-describedby') === err.getAttribute('id') && !!err.getAttribute('id')) });
		ri[0].value = ''; ri[1].value = '2026-10-01T00:00';
		apply.fire('click');
		const unparsable = state();
		ri[0].value = '2026-10-02T00:00';
		apply.fire('click');
		const order = state();
		ri[0].value = '2026-09-30T00:00';
		apply.fire('click');
		const ok = state();
		// A stale error goes away on an edit and on a preset choice.
		ri[0].value = '2026-10-02T00:00'; apply.fire('click');
		const shown = state().text;
		ri[0].fire('input');
		const afterEdit = state();
		apply.fire('click');
		findAll(re, n => n.tagName === 'BUTTON')[0].fire('click');
		const afterPreset = state();
		out.railA11y = { afterToggle, afterMore, afterVersion, afterSelectAll, missing, unparsable, order, ok, shown, afterEdit, afterPreset,
			parse: ['2026-02-30T00:00', '2026-10-01T24:00', '2026-13-01T00:00', '2026-02-28T23:59', '2028-02-29T00:00', '2026-02-29T00:00', '2026-10-01T00:00:61', '2026-10-01T00:00:59.123']
				.map(v => I.parseUtcLocal(v)) };
	} catch (e) { out.railA11y = { crash: String(e && e.stack || e) }; }

	// 8. Custom sections: E-JS-105 for an unknown name, the frozen ctx, and publishing through the section's owner.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const bus = fakeBus();
		r.w.JuneauViews.bus = bus;
		let seen = null;
		r.w.JuneauDashboard.section.register('app.note', { mount(el, spec, ctx) {
			seen = { id: ctx.id, topic: ctx.topic, viewId: ctx.viewId, args: ctx.args, initial: ctx.initial, frozen: Object.isFrozen(ctx) };
			ctx.publish({ schemaVersion: 1, viewId: ctx.viewId, search: 'n' });
		} });
		const ue = doc.createElement('fieldset');
		const unknown = I.mountSection(ue, { id: 'x', kind: 'custom', topic: 'filter:x', name: 'app.nope', args: {} }, 'd');
		const ke = doc.createElement('fieldset');
		I.mountSection(ke, { id: 'n', kind: 'custom', topic: 'filter:n', name: 'app.note', args: { a: 1 } }, 'd');
		out.custom = { unknown, box: ue.children[0].textContent, role: ue.children[0].getAttribute('role'), cls: ue.children[0].className,
			log: r.errors.slice(), seen, published: bus.published };
	} catch (e) { out.custom = { crash: String(e && e.stack || e) }; }

	// 9. Status text, compaction, "ago" and the 5 s announcer.
	try {
		const r = load(), I = r.I;
		const now = Date.parse('2026-10-11T12:00:00.000Z');
		const handles = [
			{ status: 'loading', progress: { loaded: 412118, total: 654000 } },
			{ status: 'loading', progress: { loaded: 412118 } },
			{ status: 'ready', rowCount: 412118, loadedAt: '2026-10-11T11:58:00.000Z' },
			{ status: 'stale', rowCount: 412118, loadedAt: '2026-10-11T11:58:00.000Z' },
			{ status: 'ready', rowCount: 2000000, truncated: true, truncatedAt: 2000000, loadedAt: '2026-10-11T11:59:30.000Z' },
			{ status: 'ready', rowCount: 12, mode: 'aggregate', cache: 'memory-fallback', loadedAt: '2026-10-11T09:00:00.000Z' },
			{ status: 'error', error: { code: 'E-JS-62', message: 'HTTP 500' } },
			null,
			{ status: 'ready', rowCount: 5 }
		];
		let t = 0;
		const el = { textContent: '' };
		const announce = I.announcer(el, () => t);
		announce('a');
		const a1 = el.textContent;
		t = 1000; announce('b');
		t = 2000; announce('c');
		const a2 = el.textContent;
		const wait1 = r.timers.filter(x => x.live).map(x => x.ms);
		t = 5000; flush(r);
		const a3 = el.textContent;
		t = 5100; announce('d');
		const wait2 = r.timers.filter(x => x.live).map(x => x.ms);
		out.status = {
			texts: handles.map(h => I.statusText(h, now)),
			compact: [999, 1000, 1500, 412118, 2000000, 1500000].map(I.compact),
			ago: [-5, 0, 59999, 60000, 3599999, 3600000, 86400000].map(I.ago),
			announcer: { a1, a2, wait1, a3, wait2 }
		};
	} catch (e) { out.status = { crash: String(e && e.stack || e) }; }

	// 10. The header: status lines, the announcer, Refresh and its menu, save, and Copy link with its fallback.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const bus = pageBus();
		r.w.JuneauViews.bus = bus;
		r.w.location = { origin: 'https://h', pathname: '/p', search: '?state=x', hash: '' };
		let clipped = null, clipOk = true;
		r.w.navigator = { clipboard: { writeText: u => { clipped = u; return clipOk ? Promise.resolve() : Promise.reject(new Error('denied')); } } };
		const parts = { header: doc.createElement('header'), status: doc.createElement('div'), live: doc.createElement('span') };
		const sets = [];
		const d = { id: 'd', status: ['data:orders', 'data:totals'], persist: { ns: 'a', version: 1, topics: ['filter:q'], shareLink: true } };
		const current = I.wireSave(d, { set: (k, v) => sets.push([k, copy(v)]) });
		I.wireHeader(doc, parts, d, current);
		const lines = () => parts.status.children.map(s => [s.textContent, s.getAttribute('data-tone')]);
		const initialLines = lines();
		const firstPauses = r.timers.filter(x => x.live).map(x => x.ms);
		const loadedAt = new Date(Date.now() - 120000).toISOString();
		bus.subs['data:orders']({ schemaVersion: 1, source: 'orders', version: 1, status: 'ready', rowCount: 412118, loadedAt, dims: [] }, {});
		const live1 = parts.live.textContent;
		bus.subs['data:totals']({ schemaVersion: 1, source: 'totals', version: 1, status: 'error', error: { code: 'E-JS-62', message: 'HTTP 500' } }, {});
		const live2 = parts.live.textContent;
		const waits = r.timers.filter(x => x.live && x.ms !== 30000).map(x => x.ms);
		flush(r);
		const live3 = parts.live.textContent;
		const afterLines = lines();
		const buttons = findAll(parts.header, n => n.tagName === 'BUTTON');
		const menu = findAll(parts.header, n => n.className === 'jc-dash-menu')[0];
		buttons[0].fire('click');                       // Refresh
		buttons[1].fire('click');                       // the menu toggle
		const opened = [buttons[1].getAttribute('aria-expanded'), menu.hidden, doc.activeElement === buttons[2]];
		buttons[2].fire('click');                       // Clear cache
		const closed = [buttons[1].getAttribute('aria-expanded'), menu.hidden];
		buttons[1].fire('click');
		menu.fire('keydown', { key: 'Escape' });
		const escaped = [buttons[1].getAttribute('aria-expanded'), menu.hidden, doc.activeElement === buttons[1]];
		bus.subs['filter:q']({ schemaVersion: 1, viewId: 'd', search: 'x' }, {});
		bus.subs['filter:q']({ schemaVersion: 1, viewId: 'd', search: 'xy' }, {});
		const savePending = r.timers.filter(x => x.live && x.ms === 500).length;
		flush(r);
		buttons[3].fire('click');                       // Copy link
		await tick();
		const copiedText = buttons[3].textContent;
		const url = clipped;
		bus.subs['filter:q'](null, { cleared: true });
		flush(r);
		clipOk = false;
		buttons[3].fire('click');
		await tick();
		const field = findAll(parts.header, n => n.className === 'jc-dash-copy-field')[0];
		const input = field.children[0];

		// A fragment over 8 KiB is refused: three payloads of about 3 KiB each.
		const r2 = load(), doc2 = fakeDoc(), bus2 = pageBus();
		r2.w.JuneauViews.bus = bus2;
		r2.w.location = r.w.location;
		let clipped2 = null;
		r2.w.navigator = { clipboard: { writeText: u => { clipped2 = u; return Promise.resolve(); } } };
		const d2 = { id: 'd', persist: { ns: 'a', version: 1, topics: ['filter:a', 'filter:b', 'filter:c'], shareLink: true } };
		const cur2 = r2.I.wireSave(d2, { set() {} });
		for (const k of ['a', 'b', 'c']) bus2.subs['filter:' + k]({ schemaVersion: 1, viewId: 'd', search: k.repeat(3000) }, {});
		const parts2 = { header: doc2.createElement('header'), status: doc2.createElement('div'), live: doc2.createElement('span') };
		r2.I.wireHeader(doc2, parts2, d2, cur2);
		const b2 = findAll(parts2.header, n => n.tagName === 'BUTTON');
		b2[0].fire('click');
		await tick();

		out.header = {
			initialLines, firstPauses, live1, live2, waitOk: waits.length === 1 && waits[0] > 4000 && waits[0] <= 5000, live3, afterLines,
			published: bus.published, opened, closed, escaped, savePending, sets, copiedText,
			urlStart: url.startsWith('https://h/p?state=x#s='), decoded: I.decodeShareLink(url.slice(url.indexOf('#')), d.persist),
			field: { readOnly: input.readOnly, sameUrl: input.value === clipped, label: input.getAttribute('aria-label'),
				selected: input.selected === true, focused: doc.activeElement === input, hint: field.children[1].textContent },
			tooLong: { buttons: b2.map(b => b.textContent), warns: r2.warnings.slice(), statusLines: parts2.status.children.length,
				clipped: clipped2, header: parts2.header.children.map(c => c.className) }
		};
	} catch (e) { out.header = { crash: String(e && e.stack || e) }; }

	// 12. Header details: flush on pagehide, announced and de-stacked Copy flashes, and the Refresh menu closing on outside interaction.
	try {
		const r = load(), I = r.I, doc = fakeDoc();
		const bus = pageBus();
		r.w.JuneauViews.bus = bus;
		r.w.location = { origin: 'https://h', pathname: '/p', search: '', hash: '' };
		const winL = {};
		r.w.addEventListener = (t, fn) => { (winL[t] = winL[t] || []).push(fn); };
		doc.visibilityState = 'visible';
		r.w.document = doc;
		r.w.navigator = { clipboard: { writeText: () => Promise.resolve() } };
		const parts = { header: doc.createElement('header'), status: doc.createElement('div'), live: doc.createElement('span') };
		const sets = [];
		const d = { id: 'd', status: ['data:orders'], persist: { ns: 'a', version: 1, topics: ['filter:q'], shareLink: true } };
		const current = I.wireSave(d, { set: (k, v) => sets.push([k, copy(v)]) });
		I.wireHeader(doc, parts, d, current);
		const live500 = () => r.timers.filter(x => x.live && x.ms === 500).length;
		bus.subs['filter:q']({ schemaVersion: 1, viewId: 'd', search: 'x' }, {});
		const pendingBefore = live500();
		for (const fn of winL.pagehide || []) fn({});
		const flushed = { pendingBefore, pendingAfter: live500(), sets: sets.length };
		for (const fn of winL.pagehide || []) fn({});
		flushed.afterIdle = sets.length;
		bus.subs['filter:q']({ schemaVersion: 1, viewId: 'd', search: 'y' }, {});
		doc.visibilityState = 'visible'; doc.fire('visibilitychange', {});
		const visibleSets = sets.length;
		doc.visibilityState = 'hidden'; doc.fire('visibilitychange', {});
		doc.fire('visibilitychange', {});
		flushed.visibility = { whileVisible: visibleSets, afterHidden: sets.length };

		const buttons = findAll(parts.header, n => n.tagName === 'BUTTON');
		const copyBtn = buttons[3];
		const flushLive = () => { for (const t of r.timers) if (t.live && t.ms === 0) { t.live = false; t.fn(); } };
		copyBtn.fire('click'); await tick();
		flushLive();
		const live1 = parts.live.textContent;
		copyBtn.fire('click'); await tick();
		const liveCleared = parts.live.textContent;   // cleared first, so the same text announces again
		flushLive();
		const live2 = parts.live.textContent;
		const reverts = r.timers.filter(x => x.live && x.ms === 2000).length;
		flush(r);
		const copy2 = { live1, liveCleared, live2, reverts, text: copyBtn.textContent };

		const menu = findAll(parts.header, n => n.className === 'jc-dash-menu')[0];
		const toggle = buttons[1], wrap = menu.parentNode;
		const state = () => [toggle.getAttribute('aria-expanded'), menu.hidden];
		const out2 = {};
		toggle.fire('click'); out2.open = state();
		doc.fire('pointerdown', { target: buttons[2] }); out2.insidePress = state();
		wrap.fire('focusout', { relatedTarget: buttons[2] }); out2.insideFocus = state();
		doc.fire('pointerdown', { target: copyBtn }); out2.outsidePress = state();
		toggle.fire('click');
		wrap.fire('focusout', { relatedTarget: copyBtn }); out2.focusAway = state();
		toggle.fire('click'); out2.reopened = state();
		wrap.fire('focusout', { relatedTarget: null }); out2.idleFocusout = state();
		out.headerA11y = { flushed, copy: copy2, menu: out2 };
	} catch (e) { out.headerA11y = { crash: String(e && e.stack || e) }; }

	// 11. encodeLink leaves out a topic whose value cannot be serialized.
	try {
		const r = load(), I = r.I;
		const P = { ns: 'a', version: 1, topics: ['filter:q', 'filter:cyc', 'filter:big'], shareLink: true };
		const cyc = { schemaVersion: 1, viewId: 'd' };
		cyc.self = cyc;
		const big = { schemaVersion: 1, viewId: 'd', search: 1n };
		const q = { schemaVersion: 1, viewId: 'd', search: 'ok' };
		const frag = I.encodeLink(P, { 'filter:q': q, 'filter:cyc': cyc, 'filter:big': big });
		out.encodeGuard = { threw: false, t: I.decodeShareLink(frag, P).t };
	} catch (e) { out.encodeGuard = { crash: String(e && e.stack || e) }; }

	// ---- harness sections ----

	process.stdout.write(JSON.stringify(out));
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
