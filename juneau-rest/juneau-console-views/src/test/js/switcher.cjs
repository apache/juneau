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
 * switcher.cjs - Node harness for the switcher card (juneau-switcher.js): lazy views, selector and display
 * accessibility, state precedence (address, preferences, spec), the sw directive, the handle surface and nested-card
 * behaviour.  Loads the real scripts into a fresh vm sandbox per case and prints ONE JSON report; every assertion
 * lives in Switcher_Test.
 *
 *   Usage:  node switcher.cjs <bus> <renders> <views> <urlstate> <helpers> <console> <model> <switcher>
 */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const files = process.argv.slice(2);
if (files.length !== 8) {
	console.error('usage: node switcher.cjs <bus> <renders> <views> <urlstate> <helpers> <console> <model> <switcher>');
	process.exit(2);
}
const sources = files.map(f => [path.basename(f), fs.readFileSync(path.resolve(f), 'utf8')]);
const switcherSrc = sources[7];
const tick = () => new Promise(r => setImmediate(r));
const codeOf = fn => { try { fn(); return null; } catch (e) { return e.code || String(e); } };

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

/** One page: `contract` in a #juneau-page island, the six view scripts, the shell and the switcher script. */
async function page(contract, opts) {
	opts = opts || {};
	const search = opts.search || '';
	const env = makeConsoleEnv({ location: { search } });
	const doc = env.document;
	const create = doc.createElement;
	doc.createElement = tag => patchElement(env, create(tag));
	patchElement(env, doc.body);
	const errors = [], hist = [], renders = [], refreshes = [], destroys = [];
	const island = doc.createElement('script');
	island.setAttribute('type', 'application/json');
	island.id = 'juneau-page';
	island.textContent = JSON.stringify({ contractVersion: '1', modelVersion: '1', title: 'T', nav: [], activeNav: [], ...contract });
	doc.body.appendChild(island);
	doc.readyState = 'loading';
	const store = new Map(Object.entries(opts.prefs || {}));
	const localStorage = { getItem: k => (store.has(k) ? store.get(k) : null), setItem: (k, v) => { store.set(k, String(v)); }, removeItem: k => { store.delete(k); } };
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {}, debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, URL, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, Number, AbortController, Math,
		encodeURIComponent, Date, localStorage,
		fetch: () => Promise.reject(new Error('no fetch in harness')),
		setTimeout: fn => fn(), clearTimeout: () => {}, setInterval: () => 0, clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	env.window.localStorage = localStorage;
	env.window.history = { state: null, replaceState: (s, t, u) => { hist.push(decodeURIComponent(String(u))); const q = String(u).indexOf('?'); env.window.location.search = q < 0 ? '' : String(u).slice(q); } };
	const leaf = {
		render(spec, el) { renders.push(spec.id); const p = doc.createElement('p'); p.textContent = 'leaf:' + spec.id; el.appendChild(p); },
		refresh(spec) { refreshes.push(spec.id); },
		destroy(spec) { destroys.push(spec.id); }
	};
		env.window.JuneauConsoleCards = [['leaf', toCardClass(leaf)]];
	vm.createContext(sandbox);
	const load = ([name, src]) => vm.runInContext(src, sandbox, { filename: name }); // NOSONAR javascript:S1523 - loads the shipped scripts into a vm sandbox for the test
	const r = { env, doc, errors, hist, renders, refreshes, destroys, store, sandbox, threw: null };
	try {
		for (const s of sources) load(s);
			if (opts.loadTwice) {
			try { load(switcherSrc); } catch (e) { r.secondLoadThrew = e.code || String(e); }
		}
	} catch (e) {
		r.threw = { code: e.code, message: e.message };
	}
	r.JC = env.window.JuneauConsole;
	r.U = env.window.JuneauViews?.urlState;
	await tick();
	try { env.fireDOMContentLoaded(); } catch (e) { r.readyThrew = { code: e.code, message: e.message }; }
	await tick();
	return r;
}

function toCardClass(h) {
	// Derived (so the constructor chain reaches the Card base the shell puts under it) and flagged lateBase.
	const K = class extends (class {}) {}; // NOSONAR javascript:S2094 - an inert base standing in for the Card base
	K.lateBase = true;
	const P = K.prototype;
	P.render = function () { return h.render(this.spec, this.el, this); };
	P.refresh = function () { return h.refresh(this.spec, this.el, this); };
	P.destroy = function () { return h.destroy(this.spec, this.el, this); };
	return K;
}

const leafCard = id => ({ id, type: 'leaf' });
const baseSpec = extra => ({ id: 'sw1', type: 'switcher', title: 'Switch', views: [
	{ id: 'v1', label: 'One', card: leafCard('c1') },
	{ id: 'v2', label: 'Two', sections: [{ id: 's1', label: 'S1', card: leafCard('c2') }, { id: 's2', label: 'S2', card: leafCard('c3') }] },
	{ id: 'v3', label: 'Three', card: leafCard('c4') }
], ...extra });
const withSwitchers = (...specs) => ({ cards: specs });
const api = r => r.JC.cardApi('sw1');
const activeId = (r, id) => { const sw = r.JC.cardApi(id || 'sw1'); return sw.activeView; };
const tabBtn = (r, viewId) => r.doc.getElementById('sw1').querySelector('[data-tab-id="' + viewId + '"]');
const strips = r => r.doc.getElementById('sw1').querySelectorAll('.juneau-view-helper-tabstrip');

const out = {};

async function main() {
	// s1. Only the initial view is built; later views build once, on first selection.
	{
		const r = await page(withSwitchers(baseSpec()));
		const afterMount = r.renders.slice();
		api(r).select('v3');
		const afterV3 = r.renders.slice();
		api(r).select('v1');
		api(r).select('v3');
		out.s1 = { afterMount, afterV3, final: r.renders.slice(), threw: r.threw, readyThrew: r.readyThrew, errors: r.errors };
	}
	// s2. The display is a labelled tab panel; the selector is a pane-less ribbon-mode strip.
	{
		const r = await page(withSwitchers(baseSpec()));
		const disp = r.doc.getElementById('sw1-display');
		const labelled = () => disp.getAttribute('aria-labelledby');
		const first = labelled(), firstBtn = tabBtn(r, 'v1').id;
		api(r).select('v3');
		const second = labelled(), secondBtn = tabBtn(r, 'v3').id;
		const sel = r.doc.getElementById('sw1').querySelector('[data-juneau-strip-mode="ribbon"]');
		out.s2 = { role: disp.getAttribute('role'), tabindex: disp.getAttribute('tabindex'),
			first: first === firstBtn && !!first, second: second === secondBtn && second !== first,
			mode: !!sel, controls: tabBtn(r, 'v1').getAttribute('aria-controls'),
			panes: r.doc.getElementById('sw1').querySelectorAll('[role="tabpanel"]').length };
	}
	// s3. A second strip (tab mode, with panes) exists only for a view with several sections.
	{
		const r = await page(withSwitchers(baseSpec()));
		const one = strips(r).length;
		api(r).select('v2');
		const two = strips(r).length;
		const sub = r.doc.getElementById('sw1').querySelector('[data-juneau-strip-mode="tab"]');
		out.s3 = { one, two, subMode: !!sub, subPanes: r.doc.getElementById('sw1').querySelectorAll('.juneau-view-helper-tabstrip-pane').length };
	}
	// s4. Precedence: address, then preferences, then spec, then first; stale ids fall through.
	{
		const p = { 'juneau-card:sw1:switcher:view': JSON.stringify('v3') };
		const url = await page(withSwitchers(baseSpec()), { search: '?state=sw(sw1:v2)', prefs: p });
		const prefs = await page(withSwitchers(baseSpec()), { prefs: p });
		const initial = await page(withSwitchers(baseSpec({ initialView: 'v2' })));
		const none = await page(withSwitchers(baseSpec()));
		const stale = await page(withSwitchers(baseSpec({ initialView: 'v3' })), { search: '?state=sw(sw1:nope)', prefs: { 'juneau-card:sw1:switcher:view': JSON.stringify('gone') } });
		out.s4 = { url: activeId(url), prefs: activeId(prefs), initial: activeId(initial), none: activeId(none), stale: activeId(stale) };
	}
	// s5. Section restore from preferences and from the address (the address wins).
	{
		const p = { 'juneau-card:sw1:switcher:view': JSON.stringify('v2'), 'juneau-card:sw1:switcher:section.v2': JSON.stringify('s2') };
		const prefs = await page(withSwitchers(baseSpec()), { prefs: p });
		const url = await page(withSwitchers(baseSpec()), { search: '?state=sw(sw1:v2.s1)', prefs: p });
		out.s5 = { prefs: prefs.JC.cardApi('sw1')._currentKey(), url: url.JC.cardApi('sw1')._currentKey() };
	}
	// s6. Serialisation: defaults omitted, two switchers share one directive, ids sorted.
	{
		const rename = (sp, id) => ({ ...sp, id, views: sp.views.map(v => JSON.parse(JSON.stringify(v).replace(/"id":"c(\d)"/g, '"id":"' + id + 'c$1"'))) });
		const a = rename(baseSpec(), 'a'), b = rename(baseSpec({ initialView: 'v1' }), 'b');
		const r = await page(withSwitchers(a, b));
		const base = r.hist.length;
		const none = r.U.currentDirective('sw');
		r.JC.cardApi('a').select('v2');
		const afterA = r.hist.at(-1);
		r.JC.cardApi('b').select('v3');
		if (r.hist.length <= base) { throw new Error("second select wrote no history entry"); }
		const both = r.hist.at(-1);
		r.JC.cardApi('a').select('v1');
		r.JC.cardApi('b').select('v1');
		const cleared = r.hist.at(-1);
		out.s6 = { none: none === undefined, afterA, both, cleared, wrote: r.hist.length > base };
	}
	// s7. Loading the script a second time registers nothing twice.
	{
		const r = await page(withSwitchers(baseSpec()), { loadTwice: true });
		out.s7 = { has: r.U.hasDirective('sw'), dup67: r.errors.some(e => /E-JS-67/.test(e)), secondThrew: r.secondLoadThrew || null };
	}
	// s8. Handle surface: kinds, paths, activation through a handle, imperative addView.
	{
		const r = await page(withSwitchers(baseSpec()));
		const inst = api(r), sw = inst.switcher;
		const v2 = sw.view('v2');
		const fired = [];
		v2.on('activated', ev => fired.push(ev.target.id));
		const histBefore = r.hist.length;
		v2.activate();
		const handles = { kind: sw.kind, views: sw.views.length, v2Path: String(v2.path).endsWith('view:v2'),
			sectionKinds: v2.sections.map(s => s.kind), sectionIds: v2.sections.map(s => s.id), segment: String(v2.sections[0].path).includes('tab:s1') };
		const active = inst.activeView;
		const synced = r.hist.length > histBefore;
		sw.addView({ id: 'v4', label: 'Four', card: leafCard('c9') });
		const added = sw.views.length;
		const dup = (() => { try { sw.addView({ id: 'v4', label: 'x', card: leafCard('c10') }); return null; } catch (e) { return e.message; } })();
		inst.select('v4');
		out.s8 = { handles, active, synced, fired, added, dup, v4Built: r.renders.includes('c9') };
	}
	// s9. refresh() touches the active view's nested cards only and never mounts anything.
	{
		const r = await page(withSwitchers(baseSpec()));
		const before = r.renders.length;
		api(r).refresh();
		out.s9 = { refreshed: r.refreshes.slice(), rendersUnchanged: r.renders.length === before };
	}
	// s10. destroy() releases the nested cards and the sw entry.
	{
		const r = await page(withSwitchers(baseSpec()));
		api(r).select('v3');
		r.JC.destroyCard('sw1');
		out.s10 = { destroyed: r.destroys.slice().sort((a, b) => (a < b ? -1 : Number(a > b))), api: codeOf(() => r.JC.cardApi('c4')), source: !/sw\(/.test(r.hist.at(-1)) };
	}
	// s11. Nested cards keep their api; the view lists them; a nested switcher is rejected.
	{
		const r = await page(withSwitchers(baseSpec()));
		const before = r.JC.cardApi('sw1').switcher.view('v3').cards().length;
		const apiBefore = codeOf(() => r.JC.cardApi('c4'));
		api(r).select('v3');
		const nested = r.JC.cardApi('c4');
		const cards = api(r).switcher.view('v3').cards();
		const bad = await page(withSwitchers(baseSpec({ views: [
			{ id: 'v1', label: 'One', card: leafCard('c1') }, { id: 'v2', label: 'Two', card: { id: 'inner', type: 'switcher', views: [] } }] })));
		out.s11 = { before, apiBefore, same: cards.length === 1 && cards[0] === nested, hostId: nested.el.id, rejected: codeOf(() => bad.JC.cardApi('sw1').select('v2')) };
	}
}

main().then(() => { process.stdout.write(JSON.stringify(out)); }).catch(e => { process.stdout.write(JSON.stringify({ harnessError: String(e?.stack || e), partial: out })); });
