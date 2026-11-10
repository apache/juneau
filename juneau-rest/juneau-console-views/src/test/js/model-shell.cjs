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
 * model-shell.cjs - dependency-free Node harness for the shell side of the client object model in
 * juneau-console.js: error codes, the modelVersion check, the Card base and class-form registerCard,
 * the page scripts loader and bus-phase ordering.  Prints ONE JSON report; every assertion lives in
 * ConsoleModelShell_Test.
 *
 *   Usage:  node model-shell.cjs <juneau-bus.js> <juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const busPath = process.argv[2];
const shellPath = process.argv[3];
if (!shellPath) {
	console.error('usage: node model-shell.cjs <juneau-bus.js> <juneau-console.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');
const busSrc = busPath ? fs.readFileSync(busPath, 'utf8') : '';
const tick = () => new Promise(r => setImmediate(r));

function makeFakeStorage() {
	const m = new Map();
	return {
		getItem: k => (m.has(k) ? m.get(k) : null),
		setItem: (k, v) => { m.set(k, String(v)); },
		removeItem: k => { m.delete(k); }
	};
}

/** A fresh sandbox with the shell (and, unless opts.noBus, the bus) loaded; returns helpers over it. */
function load(opts) {
	opts = opts || {};
	const env = makeConsoleEnv(opts);
	const doc = env.document;
	const errors = [], warns = [], cardEvents = [], intervals = [];
	doc.readyState = 'loading';
	doc.baseURI = 'http://juneau.test/';
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)), debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent, URL,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, Number, AbortController,
		encodeURIComponent, Date, Symbol,
		localStorage: opts.localStorage || makeFakeStorage(),
		fetch: opts.fetch || (() => Promise.reject(new Error('no fetch in harness'))),
		setTimeout: (fn, ms) => fn(), clearTimeout: () => {},
		setInterval: (fn, ms) => { intervals.push({ fn, ms }); return intervals.length; }, clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	env.window.localStorage = sandbox.localStorage;
	if (opts.queueBefore) env.window.JuneauConsoleCards = opts.queueBefore.slice();
	doc.addEventListener('juneau:card-mounted', ev => { cardEvents.push('mounted:' + ev.detail.id); });
	doc.addEventListener('juneau:card-failed', ev => { cardEvents.push('failed:' + ev.detail.id); });
	if (opts.island) {
		const island = doc.createElement('script');
		island.setAttribute('type', 'application/json');
		island.id = 'juneau-page';
		island.textContent = JSON.stringify(opts.island);
		doc.body.appendChild(island);
	}
	vm.createContext(sandbox);
	let threw = null;
	try {
		if (!opts.noBus) vm.runInContext(busSrc, sandbox, { filename: 'juneau-bus.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
		vm.runInContext(shellSrc, sandbox, { filename: 'juneau-console.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	} catch (e) {
		threw = e.code || String(e);
	}
	const JC = env.window.JuneauConsole;
	return {
		env, doc, errors, warns, cardEvents, intervals, sandbox, JC, threw,
		bus: env.window.JuneauViews && env.window.JuneauViews.bus,
		bannerText: () => { const b = doc.querySelector('.jc-console-error'); return b ? b.textContent : ''; },
		bannerCodes: () => doc.querySelectorAll('.jc-console-error-item').map(e => e.getAttribute('data-juneau-error')),
		scripts: () => doc.querySelectorAll('script[src]'),
		cardEl: id => doc.getElementById(id),
		mount: contract => JC.mount(contract, { root: doc.body, document: doc }),
		ready: () => env.fireDOMContentLoaded()
	};
}

const base = { contractVersion: '1', title: 't', nav: [], activeNav: [], cards: [] };
const C = o => ({ ...base, ...o });
const codeOf = fn => { try { fn(); return null; } catch (e) { return e.code || String(e); } };
const errInfo = fn => { try { fn(); return null; } catch (e) { return { code: e.code, msg: e.message }; } };
const Base = s => s.JC.Card;

const report = {};

(async () => {
	// ---- modelVersion ----
	const mountExpect = contract => {
		const s = load();
		let threw = null;
		try { s.mount(contract); } catch (e) { threw = e.code || String(e); }
		return { threw, banner: s.bannerText() };
	};
	report.modelVersionMissing = mountExpect({ ...base, model: { on: { loaded: 'a.b' } } }).threw;
	report.modelVersionMissingBanner = mountExpect({ ...base, model: { on: { loaded: 'a.b' } } }).banner;
	report.modelVersionBad = mountExpect({ ...base, modelVersion: '2' }).threw;
	report.modelVersionOk = mountExpect({ ...base, modelVersion: '1' }).threw;
	report.noModelNoVersion = mountExpect(base).threw;
	report.modelVersionMissingNested = mountExpect({ ...base, cards: [{ id: 'c1', type: 'html', model: { on: { loaded: 'a.b' } } }] }).threw;
	report.modelVersionMissingSubscribes = mountExpect({ ...base, cards: [{ id: 'c1', type: 'html', subscribes: [{ topic: 'a.b', handler: 'a.b' }] }] }).threw;
	report.noModelNeverReadsViews = (() => {
		const s = load({ noBus: true });
		let reads = 0;
		const views = {};
		Object.defineProperty(views, 'model', { configurable: true, get: () => { reads++; return undefined; } });
		s.env.window.JuneauViews = views;
		let threw = null;
		try { s.mount(base); s.ready(); } catch (e) { threw = e.code || String(e); }
		return { reads, threw };
	})();
	report.modelVersionTableRibbon = mountExpect({ ...base, cards: [{ id: 'c1', type: 'html', table: { ribbon: [{ type: 'refresh', model: { on: {} } }] } }] }).threw;

	// ---- Card base members ----
	const members = (fn, opts) => {
		const s = load(opts);
		let inst = null;
		s.JC.registerCard('probe', class extends Base(s) { render() { inst = this; return fn ? fn(this, s) : undefined; } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return { s, inst };
	};
	report.class_card_renders_without_new_error = (() => {
		const s = load();
		let ok = false;
		s.JC.registerCard('probe', class extends Base(s) { render() { ok = true; } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return { ok, errors: s.errors };
	})();
	report.this_el_spec_set_before_render = (() => {
		const s = load();
		s.JC.registerCard('probe', class extends Base(s) {
			render() { this.seen = { el: this.el === s.cardEl('p1'), specId: this.spec.id, frozen: Object.isFrozen(this.spec), hasSignal: !!this.signal }; }
		});
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return s.JC.cardApi('p1').seen;
	})();
	report.member_document = (() => { const { s, inst } = members(); return inst.document === s.doc; })();
	report.member_template = (() => {
		const s = load();
		let got = null, err = null;
		s.JC.registerCard('probe', class extends Base(s) { render() { try { got = this.template('p1') != null; } catch (e) { err = e.message; } } });
		const t = s.doc.createElement('template');
		t.setAttribute('data-card', 'p1');
		s.doc.body.appendChild(t);
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return { got, err };
	})();
	report.member_fail = (() => { const { inst } = members(); try { inst.fail('x'); return null; } catch (e) { return e.message; } })();
	report.member_onReady = (() => {
		const { s, inst } = members();
		let n = 0;
		inst.onReady(() => { n++; });
		const before = n;
		s.ready();
		return { before, after: n };
	})();
	report.member_fetchJson_aborts_on_destroy = await (async () => {
		let aborted = false;
		const { s, inst } = members(null, { fetch: (u, init) => new Promise((res, rej) => {
			init.signal.addEventListener('abort', () => { aborted = true; rej(Object.assign(new Error('a'), { name: 'AbortError' })); });
		}) });
		const p = inst.fetchJson('/x').then(() => false, e => inst.isAbort(e));
		s.JC.destroyCard('p1');
		return { isAbort: await p, aborted };
	})();
	report.member_isAbort = (() => { const { inst } = members(); return [inst.isAbort({ name: 'AbortError' }), inst.isAbort(new Error('x'))]; })();
	report.member_paintLoading_paintError = (() => {
		const { s, inst } = members();
		inst.paintLoading();
		const loading = !!s.cardEl('p1').querySelector('.jc-card-loading');
		inst.paintError(new Error('boom'));
		const e1 = (s.cardEl('p1').querySelector('.jc-card-error') || {}).textContent;
		const t = s.doc.createElement('div');
		s.cardEl('p1').appendChild(t);
		inst.paintError(t, new Error('two'));
		return { loading, e1, e2: t.textContent };
	})();
	report.member_every_cleared_on_destroy = (() => {
		const { s, inst } = members();
		let n = 0;
		inst.every(1000, () => { n++; });
		const registered = s.intervals.length;
		s.JC.destroyCard('p1');
		s.intervals[0].fn();
		return { registered, afterDestroy: n };
	})();
	report.member_prefs = (() => { const { inst } = members(); const p = inst.prefs('ns'); p.set('k', { a: 1 }); return p.get('k'); })();
	report.member_onDestroy = (() => { const { s, inst } = members(); let n = 0; inst.onDestroy(() => { n++; }); s.JC.destroyCard('p1'); return n; })();
	report.member_announce = (() => { const { s, inst } = members(); inst.announce('hello'); const r = s.doc.querySelector('.jc-sr-announce'); return r ? r.textContent : null; })();
	report.member_signal = (() => { const { s, inst } = members(); const before = inst.signal.aborted; s.JC.destroyCard('p1'); return { before, after: inst.signal.aborted }; })();
	report.member_publish_subscribe_returns_off = await (async () => {
		const heard = [];
		const s = load();
		s.JC.registerCard('probe', class extends Base(s) {
			render() { this.off = this.subscribe('app.t', v => heard.push(v)); }
		});
		s.mount(C({ topics: [{ topic: 'app.t', publisher: 'script' }], cards: [{ id: 'p1', type: 'probe' }] }));
		s.bus.declare('app.t', { retain: false });
		s.ready();
		const inst = s.JC.cardApi('p1');
		inst.publish('app.t', { n: 1 });
		const offType = typeof inst.off;
		inst.off();
		inst.publish('app.t', { n: 2 });
		return { heard, offType };
	})();
	report.member_refresh = await (async () => {
		const s = load();
		let n = 0;
		s.JC.registerCard('probe', class extends Base(s) { render() { n++; } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		await s.JC.cardApi('p1').refresh();
		return n;
	})();

	// ---- registration, roles/ops, cardApi ----
	const cls = s => class extends Base(s) { render() {} };
	report.register_objectForm_E21 = (() => { const s = load(); return codeOf(() => s.JC.registerCard('legacy', { render() {} })); })();
	report.register_objectForm_message = (() => { const s = load(); return (errInfo(() => s.JC.registerCard('legacy', { render() {} })) || {}).msg; })();
	report.register_functionForm_E21 = (() => { const s = load(); return codeOf(() => s.JC.registerCard('legacy', () => {})); })();
	report.register_string_E21 = (() => { const s = load(); return codeOf(() => s.JC.registerCard('legacy', 'nope')); })();
	report.register_missingRoleMethod_E21 = (() => { const s = load(); return errInfo(() => s.JC.registerCard('mt', class extends Base(s) { static roles = { r: 'nope' }; render() {} })); })();
	report.register_missingOpMethod_E21 = (() => { const s = load(); return errInfo(() => s.JC.registerCard('mt', class extends Base(s) { static ops = { 'do-it': 'nope' }; render() {} })); })();
	report.register_class_ok = (() => { const s = load(); s.JC.registerCard('okc', cls(s)); return s.JC.cardTypes().includes('okc'); })();
	report.register_duplicate_E12 = (() => { const s = load(); s.JC.registerCard('okc', cls(s)); return codeOf(() => s.JC.registerCard('okc', cls(s))); })();
	report.queue_accepts_class = (() => { const s = load(); s.sandbox.window.JuneauConsoleCards.push(['qc', cls(s)]); return s.JC.cardTypes().includes('qc'); })();
	report.queue_rejects_object = (() => { const s = load(); return codeOf(() => s.sandbox.window.JuneauConsoleCards.push(['qo', { render() {} }])); })();
	report.legacy_builtin_object_form_now_E21 = (() => {
		const s = load();
		return ['datatables', 'chart', 'terminal', 'job-log'].map(t => codeOf(() => s.JC.registerCard(t, { render() {} })));
	})();
	report.no_setApi_on_ctx = (() => {
		const s = load();
		let seen = 'unset';
		s.JC.registerCard('probe', class extends Base(s) { render() { seen = typeof this.setApi; } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return seen;
	})();
	report.cardApi_returns_instance_for_all_builtins = (() => {
		const s = load();
		const types = ['datatables', 'chart', 'terminal', 'job-log'];
		const made = {}, rendered = [];
		for (const t of types) s.JC.registerCard(t, made[t] = class extends Base(s) { render() { rendered.push(this.spec.id); } });
		s.mount(C({ cards: types.map((t, i) => ({ id: 'b' + i, type: t })) }));
		s.ready();
		return rendered.length === types.length && types.map((t, i) => s.JC.cardApi('b' + i) instanceof made[t]);
	})();

	// role dispatch: payload, meta; shell roles refresh/params unchanged
	report.role_dispatch_payload_meta = await (async () => {
		const got = [];
		const s = load();
		s.JC.registerCard('meter', class extends Base(s) {
			static roles = { highlight: 'hl' };
			render() {}
			hl(payload, meta) { got.push({ payload, hasMeta: meta != null && typeof meta === 'object' }); }
		});
		s.mount(C({ topics: [{ topic: 'app.level', publisher: 'script' }],
			cards: [{ id: 'm1', type: 'meter', subscribes: [{ topic: 'app.level', as: 'highlight', map: { hot: 'level' } }] }] }));
		s.bus.declare('app.level', { retain: true });
		s.ready();
		s.bus.publish('app.level', { level: 7 });
		return { got, banner: s.bannerCodes() };
	})();
	report.shellRoles_refresh_params_unchanged = await (async () => {
		const log = [];
		const s = load();
		s.JC.registerCard('grid', class extends Base(s) {
			render() { log.push('render:' + this.spec.src); }
			refresh() { log.push('refresh'); }
		});
		s.mount(C({ topics: [{ topic: 'app.go', publisher: 'script' }, { topic: 'app.pick', publisher: 'script' }],
			cards: [
				{ id: 'g1', type: 'grid', subscribes: [{ topic: 'app.go', as: 'refresh' }] },
				{ id: 'g2', type: 'grid', src: '/api/{x}', subscribes: [{ topic: 'app.pick', as: 'params', map: { x: 'v' } }] }] }));
		s.bus.declare('app.go', { retain: false });
		s.bus.declare('app.pick', { retain: true });
		s.ready();
		const mark = log.length;
		s.bus.publish('app.go', { n: 1 });
		s.bus.publish('app.pick', { v: 'a' });
		await new Promise(r => setImmediate(r));
		return { log: log.slice(mark), banner: s.bannerCodes() };
	})();
	// cmd ops
	const opPage = async (register) => {
		const s = load();
		const got = [];
		register(s, got);
		s.mount(C({ cards: [{ id: 'o1', type: 'op' }] }));
		s.ready();
		return { s, got };
	};
	report.cmd_op_receives_full_cmd = await (async () => {
		const { s, got } = await opPage((s, got) => {
			s.JC.registerCard('op', class extends Base(s) { static ops = { 'do-it': 'doIt' }; render() {} doIt(cmd) { got.push(cmd); } });
		});
		s.bus.publish('cmd:o1', { schemaVersion: 1, op: 'do-it', ids: [1], resetPaging: true, options: { a: 1 }, target: 'g' });
		return got[0] ? { op: got[0].op, ids: got[0].ids, resetPaging: got[0].resetPaging, options: got[0].options, target: got[0].target } : null;
	})();
	report.cmd_refresh_forwards = await (async () => {
		const { s, got } = await opPage((s, got) => {
			s.JC.registerCard('op', class extends Base(s) { render() { got.push('render'); } refresh() { got.push('refresh'); } });
		});
		s.bus.publish('cmd:o1', { schemaVersion: 1, op: 'refresh' });
		await new Promise(r => setImmediate(r));
		return got;
	})();
	report.cmd_unknown_op_E48 = await (async () => {
		const { s } = await opPage((s) => { s.JC.registerCard('op', class extends Base(s) { render() {} }); });
		s.bus.publish('cmd:o1', { schemaVersion: 1, op: 'explode' });
		const inline = s.cardEl('o1').querySelectorAll('.jc-card-bus-error').map(n => n.getAttribute('data-juneau-error'));
		return inline;
	})();
	report.cardApi_returns_instance = (() => {
		const s = load();
		let inst = null;
		s.JC.registerCard('probe', class extends Base(s) { render() { inst = this; } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		return s.JC.cardApi('p1') === inst && inst instanceof Base(s);
	})();
	report.cardApi_unknown_E22 = (() => { const s = load(); return codeOf(() => s.JC.cardApi('never')); })();
	report.cardApi_destroyed_E22 = (() => {
		const s = load();
		s.JC.registerCard('probe', cls(s));
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		s.JC.destroyCard('p1');
		return codeOf(() => s.JC.cardApi('p1'));
	})();
	report.cardTypeTable_reads_static_maps = (() => {
		const s = load();
		let table = null;
		s.JC.registerCard('tt', class extends Base(s) { static roles = { alpha: 'a', 'beta-x': 'b' }; static ops = { 'op-one': 'o' }; render() {} a() {} b() {} o() {} });
		const orig = s.bus.wiring.validate;
		s.bus.wiring.validate = (contract, t) => { table = t; return orig(contract, t); };
		s.mount(C({ cards: [{ id: 't1', type: 'tt' }] }));
		s.ready();
		return table ? { roles: table.tt.roles, ops: table.tt.ops, builtins: Object.keys(table).sort() } : null;
	})();
	report.implicitTopics_function_form = (() => {
		const s = load();
		let table = null;
		s.JC.registerCard('grid', class extends Base(s) {
			static implicitTopics(spec) { return ['selection:' + spec.id, spec.detail ? 'detail:' + spec.id : 'plain:' + spec.id]; }
			render() {}
		});
		const orig = s.bus.wiring.validate;
		s.bus.wiring.validate = (contract, t) => { table = t; return orig(contract, t); };
		s.mount(C({ cards: [{ id: 'a', type: 'grid' }, { id: 'b', type: 'grid', detail: true }] }));
		s.ready();
		return { isFn: typeof table.grid.implicit === 'function', a: table.grid.implicit({ id: 'a' }), b: table.grid.implicit({ id: 'b', detail: true }) };
	})();
	report.implicitTopics_claimed = (() => {
		const s = load();
		s.JC.registerCard('grid', class extends Base(s) { static implicitTopics(spec) { return ['selection:' + spec.id]; } render() {} });
		s.mount(C({ cards: [{ id: 'a', type: 'grid' }] }));
		s.ready();
		const t = s.bus.topics().find(x => x.topic === 'selection:a');
		return t ? t.owner : null;
	})();
	report.builtin_html_renders = (() => {
		const s = load();
		const t = s.doc.createElement('template');
		t.setAttribute('data-card', 'h1');
		t.appendChild(s.doc.createTextNode('hello html'));
		s.doc.body.appendChild(t);
		s.mount(C({ cards: [{ id: 'h1', type: 'html', template: 'h1' }] }));
		return { text: s.cardEl('h1').textContent, errors: s.errors, instance: s.JC.cardApi('h1') instanceof Base(s) };
	})();
	report.builtin_terminalView_renders = (() => {
		const s = load();
		const calls = [];
		s.env.window.JuneauViews = s.env.window.JuneauViews || {};
		s.env.window.JuneauViews.terminalView = { mount: (body, spec) => { calls.push(body.id); } };
		s.mount(C({ cards: [{ id: 'tv', type: 'terminal-view' }] }));
		s.ready();
		return { calls, marker: s.cardEl('tv').getAttribute('data-juneau-card'), errors: s.errors };
	})();
	report.lifecycle_render_refresh_destroy_order = await (async () => {
		const log = [];
		const s = load();
		s.JC.registerCard('lc', class extends Base(s) {
			render() { log.push('render'); }
			refresh() { log.push('refresh'); }
			destroy() { log.push('destroy'); }
		});
		s.mount(C({ cards: [{ id: 'l1', type: 'lc' }] }));
		await s.JC.refreshCard('l1');
		s.JC.destroyCard('l1');
		return log;
	})();
	report.refresh_default_rerenders = await (async () => {
		const log = [];
		const s = load();
		s.JC.registerCard('lc', class extends Base(s) { render() { log.push('render'); } destroy() { log.push('destroy'); } });
		s.mount(C({ cards: [{ id: 'l1', type: 'lc' }] }));
		await s.JC.refreshCard('l1');
		return { log, errors: s.errors };
	})();
	report.destroy_throw_painted_E8 = (() => {
		const s = load();
		s.JC.registerCard('lc', class extends Base(s) { render() {} destroy() { throw new Error('bad destroy'); } });
		s.mount(C({ cards: [{ id: 'l1', type: 'lc' }] }));
		let threw = null;
		try { s.JC.destroyCard('l1'); } catch (e) { threw = e.code || String(e); }
		return { threw, banner: s.bannerCodes() };
	})();

	// ---- page scripts loader ----
	const tickN = async n => { for (let i = 0; i < n; i++) await new Promise(r => setImmediate(r)); };
	const scriptPage = (contract, opts) => {
		const s = load(opts);
		s.mount(C(contract));
		return s;
	};
	const fire = (s, i, type) => { const sc = s.scripts()[i]; sc.dispatch(type || 'load', {}); };
	report.scripts_load_in_order = await (async () => {
		const s = scriptPage({ scripts: ['/a.js', '/b.js'] });
		s.ready();
		await tickN(3);
		const afterReady = s.scripts().map(x => x.getAttribute('src'));
		fire(s, 0);
		await tickN(3);
		const afterFirst = s.scripts().map(x => x.getAttribute('src'));
		fire(s, 1);
		await tickN(3);
		return { afterReady, afterFirst, banner: s.bannerCodes() };
	})();
	report.scripts_dedupe_existing_tag = await (async () => {
		const s = load();
		const tag = s.doc.createElement('script');
		tag.setAttribute('src', '/a.js');
		s.doc.body.appendChild(tag);
		s.mount(C({ scripts: ['/a.js', '/b.js'] }));
		s.ready();
		await tickN(3);
		return s.scripts().map(x => x.getAttribute('src'));
	})();
	report.scripts_dedupe_resolved_url = await (async () => {
		const s = load();
		const tag = s.doc.createElement('script');
		tag.setAttribute('src', 'http://juneau.test/a.js');
		s.doc.body.appendChild(tag);
		s.mount(C({ scripts: ['/a.js'] }));
		s.ready();
		await tickN(3);
		return s.scripts().map(x => x.getAttribute('src'));
	})();
	report.scripts_crossOrigin_E124 = await (async () => {
		const s = scriptPage({ scripts: ['http://evil.test/x.js'] });
		s.ready();
		await tickN(3);
		return { codes: s.bannerCodes(), appended: s.scripts().length };
	})();
	report.scripts_backslash_E124 = await (async () => {
		const s = scriptPage({ scripts: ['/\\evil.test/x.js'] });
		s.ready();
		await tickN(3);
		return { codes: s.bannerCodes(), appended: s.scripts().length };
	})();
	report.scripts_baseHref_crossOrigin_E124 = await (async () => {
		const s = load();
		s.doc.baseURI = 'http://other.test/';
		s.mount(C({ scripts: ['/x.js'] }));
		s.ready();
		await tickN(3);
		return { codes: s.bannerCodes(), appended: s.scripts().length };
	})();
	report.scripts_malformedExistingSrc_notAMatch = await (async () => {
		const s = load();
		const tag = s.doc.createElement('script');
		tag.setAttribute('src', 'http://[bad');
		s.doc.body.appendChild(tag);
		s.mount(C({ scripts: ['/a.js'] }));
		s.ready();
		await tickN(3);
		return { codes: s.bannerCodes(), srcs: s.scripts().map(x => x.getAttribute('src')) };
	})();
	report.scripts_loadError_E124 = await (async () => {
		const s = scriptPage({ scripts: ['/a.js'] });
		s.ready();
		await tickN(3);
		fire(s, 0, 'error');
		await tickN(3);
		return s.bannerCodes();
	})();
	report.scripts_nonce_copied = await (async () => {
		const s = load();
		const shell = s.doc.createElement('script');
		shell.setAttribute('src', '/console/juneau-console.js');
		shell.setAttribute('nonce', 'abc123');
		s.doc.body.appendChild(shell);
		s.mount(C({ scripts: ['/a.js'] }));
		s.ready();
		await tickN(3);
		const sc = s.scripts().find(x => x.getAttribute('src') === '/a.js');
		return sc ? sc.getAttribute('nonce') : null;
	})();
	report.scripts_none_noop = (() => {
		const s = load();
		const log = [];
		s.JC.registerCard('probe', class extends Base(s) { render() { log.push('render'); } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		s.ready();
		return { scripts: s.scripts().length, log, busStarted: s.bus.topics().some(t => t.topic === 'card:p1') };
	})();
	report.scripts_none_synchronous_bus_phase = (() => {
		const s = load();
		s.JC.registerCard('probe', cls(s));
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }] }));
		s.ready();
		return s.bus.topics().some(t => t.topic === 'card:p1');   // wired with no microtask turn
	})();
	report.scripts_before_wireCards = await (async () => {
		const s = load();
		s.mount(C({ scripts: ['/late.js'], cards: [{ id: 'c1', type: 'late' }] }));
		s.ready();
		await tickN(3);
		s.JC.registerCard('late', class extends Base(s) { render() { this.el.appendChild(s.doc.createTextNode('late ok')); } });
		fire(s, 0);
		await tickN(4);
		return { text: s.cardEl('c1').textContent, banner: s.bannerCodes(), wired: s.bus.topics().some(t => t.topic === 'card:c1') };
	})();
	report.sweep_still_fails_unknown_type_without_scripts = (() => {
		const s = load();
		s.mount(C({ cards: [{ id: 'z1', type: 'zeta' }, { id: 't1', type: 'datatables' }] }));
		s.ready();
		return s.bannerCodes();
	})();
	report.sweep_fails_unknown_type_after_scripts = await (async () => {
		const s = scriptPage({ scripts: ['/a.js'], cards: [{ id: 'z1', type: 'zeta' }] });
		s.ready();
		await tickN(3);
		fire(s, 0);
		await tickN(4);
		return s.bannerCodes();
	})();
	report.scripts_failure_still_runs_sweep_and_bus = await (async () => {
		const s = scriptPage({ scripts: ['/a.js'], cards: [{ id: 'z1', type: 'zeta' }] });
		s.ready();
		await tickN(3);
		fire(s, 0, 'error');
		await tickN(4);
		return { banner: s.bannerCodes(), wired: s.bus.topics().some(t => t.topic === 'card:z1') };
	})();
	report.model_never_loaded_is_painted = (() => {
		const s = load();
		s.JC.registerCard('probe', class extends Base(s) { render() {} });
		s.mount(C({ modelVersion: '1', nav: [{ id: 'h', label: 'H' }], activeNav: ['h'], cards: [{ id: 'm1', type: 'probe', model: { on: { loaded: 'a.b' } } }] }));
		s.ready();
		return s.bannerCodes();
	})();
	report.scripts_only_without_model_is_quiet = (() => {
		const s = load();
		s.JC.registerCard('probe', class extends Base(s) { render() {} });
		s.mount(C({ nav: [{ id: 'h', label: 'H' }], activeNav: ['h'], cards: [{ id: 'p1', type: 'probe' }] }));
		s.ready();
		return s.bannerCodes();
	})();
	report.nav_model_key_is_not_a_model_block = mountExpect({ ...base, nav: [{ id: 'h', label: 'H', model: { on: {} } }], activeNav: ['h'] }).threw;

	report.mount_without_model_js_loaded_still_works = (() => {
		const s = load();
		s.JC.registerCard('probe', class extends Base(s) { render() { this.el.appendChild(s.doc.createTextNode('ok')); } });
		s.mount(C({ cards: [{ id: 'p1', type: 'probe' }], nav: [{ id: 'h', label: 'H' }], activeNav: ['h'] }));
		const api = s.JC.cardApi('p1');
		return { sameType: api instanceof Base(s), text: s.cardEl('p1').textContent, noModel: !(s.env.window.JuneauViews && s.env.window.JuneauViews.model), errors: s.errors };
	})();

	process.stdout.write(JSON.stringify(report));
})();
