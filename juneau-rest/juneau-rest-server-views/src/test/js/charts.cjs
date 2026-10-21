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
 * charts.cjs - Node harness for the juneau-charts.js card lifecycle against the real bus and shell, with the
 * recording fake echarts from juneau-charts-harness.cjs: chrome and accessibility, src and inline data, refresh and
 * stale, the refreshMs clamps, late fn registration (cycle and source cards), superseded draws, resize observers, the queue and alias, clicks, escaping, destroy, setOption
 * failures, the zoom:/legend: state topics, cmd ops, the theme bridge, a source card and the card API.  Every
 * assertion lives in ConsoleCharts_Test.
 *
 *   Usage:  node charts.cjs <juneau-bus.js> <juneau-data.js> <juneau-console.js> <juneau-charts.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));
const { fakeEcharts } = require(path.join(__dirname, 'juneau-charts-harness.cjs'));

const [busPath, dataPath, shellPath, chartsPath] = process.argv.slice(2);
if (!busPath || !dataPath || !shellPath || !chartsPath) {
	console.error('usage: node charts.cjs <juneau-bus.js> <juneau-data.js> <juneau-console.js> <juneau-charts.js>');
	process.exit(2);
}
const busSrc = fs.readFileSync(path.resolve(busPath), 'utf8');
const dataSrc = fs.readFileSync(path.resolve(dataPath), 'utf8');
const shellSrc = fs.readFileSync(path.resolve(shellPath), 'utf8');
const chartsSrc = fs.readFileSync(path.resolve(chartsPath), 'utf8');

const flush = async () => { for (let i = 0; i < 12; i++) await new Promise(res => setImmediate(res)); };
const wait = ms => new Promise(res => setTimeout(res, ms));

// The element surface the shell needs beyond views-dom-shim (the same patch console-shell-bus.cjs applies), plus a
// click() that records the anchor an export downloads through.
function patchElement(env, node, downloads) {
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
	node.click = function () {
		if (this.tagName === 'A') downloads.push({ href: this.getAttribute('href'), download: this.getAttribute('download') });
		this.dispatch('click', {});
	};
	return node;
}

/**
 * One page: the contract in a #juneau-page island, then DOMContentLoaded.
 * opts: {fetch(url), chartsFirst, noEcharts, theme, links, lang, before(window), queue}
 * links: stylesheet hrefs linked ahead of the theme stylesheet.
 */
async function page(contract, opts) {
	opts = opts || {};
	const navigated = [], opened = [], downloads = [];
	const env = makeConsoleEnv({ location: { assign: u => navigated.push(String(u)) } });
	const doc = env.document;
	const create = doc.createElement;
	doc.createElement = tag => patchElement(env, create(tag), downloads);
	patchElement(env, doc.body, downloads);
	if (opts.lang) doc.documentElement.setAttribute('lang', opts.lang);
	for (const href of (opts.links || []).concat(['/console/juneau-theme-' + (opts.theme || 'gray') + '.css?v=1'])) {
		const link = doc.createElement('link');
		link.setAttribute('rel', 'stylesheet');
		link.setAttribute('href', href);
		link.rel = 'stylesheet';
		link.href = href;
		doc.body.appendChild(link);
	}
	const island = doc.createElement('script');
	island.setAttribute('type', 'application/json');
	island.id = 'juneau-page';
	island.textContent = JSON.stringify({ contractVersion: '1', title: 'T', nav: [], activeNav: [], ...contract });
	doc.body.appendChild(island);
	doc.readyState = 'loading';
	const errors = [], warns = [], intervals = [], fetches = [], ros = [], rafs = [];
	const fake = fakeEcharts();
	// Recorders for the resize path: one observer per drawn plot, and a frame request that only runs when a test runs it.
	class FakeResizeObserver {
		constructor(cb) { this.cb = cb; this.live = true; this.target = null; ros.push(this); }
		observe(t) { this.target = t; }
		disconnect() { this.live = false; }
	}
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: m => warns.push(String(m)), debug: () => {} },
		URLSearchParams: env.window.URLSearchParams, URL, CustomEvent: env.window.CustomEvent, AbortController,
		localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
		fetch: (url, init) => {
			fetches.push({ url: String(url), accept: init?.headers?.Accept });
			return (opts.fetch || (() => Promise.reject(new TypeError('no fetch'))))(String(url), init);
		},
		setTimeout: opts.setTimeout || setTimeout, clearTimeout,
		ResizeObserver: FakeResizeObserver,
		requestAnimationFrame: fn => { rafs.push({ fn, cancelled: false }); return rafs.length; },
		cancelAnimationFrame: id => { if (rafs[id - 1]) rafs[id - 1].cancelled = true; },
		setInterval: (fn, ms) => { intervals.push({ fn, ms, live: true }); return intervals.length; },
		clearInterval: id => { if (id > 0 && intervals[id - 1]) intervals[id - 1].live = false; }
	};
	env.window.window = env.window;
	env.window.document = doc;
	env.window.localStorage = sandbox.localStorage;
	env.window.open = (u, t, f) => { opened.push([String(u), t, f]); return null; };
	env.window.getComputedStyle = () => ({ getPropertyValue: k => (opts.tokens?.[k]) || '' });
	doc.defaultView = env.window;
	if (!opts.noEcharts) env.window.echarts = fake.echarts;
	if (opts.queue) env.window.JuneauConsoleCharts = opts.queue.slice();
	if (opts.before) opts.before(env.window);
	vm.createContext(sandbox);
	vm.runInContext(busSrc, sandbox, { filename: 'juneau-bus.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	vm.runInContext(dataSrc, sandbox, { filename: 'juneau-data.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	if (opts.chartsFirst) vm.runInContext(chartsSrc, sandbox, { filename: 'juneau-charts.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	vm.runInContext(shellSrc, sandbox, { filename: 'juneau-console.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	if (!opts.chartsFirst) vm.runInContext(chartsSrc, sandbox, { filename: 'juneau-charts.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	const w = env.window;
	if (opts.afterLoad) opts.afterLoad(w);
	await flush();
	env.fireDOMContentLoaded();
	await flush();
	return { env, doc, w, errors, warns, intervals, fetches, fake, navigated, opened, downloads, ros, rafs, bus: w.JuneauViews.bus };
}

const ok = body => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body) });
const bad = (status, body) => Promise.resolve({ ok: false, status, json: () => Promise.resolve(body) });
// The shim's querySelector understands ONE simple selector (tag, .class, [attr]); every query is scoped to a card
// host found by id.  Never write a descendant or compound selector here.
const host = (r, id) => r.doc.getElementById(id);
const all = (r, id, sel) => { const h = host(r, id); return h ? Array.from(h.querySelectorAll(sel)) : []; };
const one = (r, id, sel) => all(r, id, sel)[0] || null;
const text = (r, id, sel) => { const n = one(r, id, sel); return n ? n.textContent : null; };
const tick = r => r.intervals.filter(x => x.live).forEach(x => x.fn());
const inst = (r, id) => r.fake.instances.findLast(i => i.el && host(r, id)?.contains(i.el) && !i.disposed) || null;
const lastOption = i => i ? JSON.parse(JSON.stringify(i.options.at(-1))) : null; // NOSONAR javascript:S7784 - JSON round-trip deliberately normalizes to plain JSON (options may carry functions)
// A column of the bound dataset (typed arrays do not survive JSON).
const col = (i, name) => Array.from(i.options.at(-1).dataset[0].source[name]);
const seriesIds = o => (o.series || []).map(s => s.id || null);
const actions = (r, from) => r.fake.calls.slice(from || 0).filter(c => c[0] === 'dispatchAction').map(c => c[1]);
const cardError = (r, id) => text(r, id, '.jc-card-error');
const key = (r, id, k) => one(r, id, '.jc-chart-plot').dispatch('keydown', { key: k, preventDefault() {} });

// Opens a card's "Show data" table (the table body is built on open, not on every draw).
const openTable = (r, id) => one(r, id, '.jc-chart-toggle').dispatch('click', {});
const ROWS = [{ day: 'Mon', placed: 3, delivered: 1 }, { day: 'Tue', placed: 5, delivered: 2 }, { day: 'Wed', placed: 4, delivered: 4 }];
const LINE = {
	id: 'c1', type: 'chart', title: 'Orders', kind: 'line', x: { field: 'day', label: 'Day' },
	series: [{ field: 'placed', label: 'Placed' }, { field: 'delivered', label: 'Delivered', tone: 'ok' }], data: ROWS
};
const card = o => ({ ...LINE, ...o });
const out = {};

// A source card on topic data:x and a page that publishes DataRegistry frames (rows given) through the real bus.
const DIMS = [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }];
const srcCard = o => ({ id: 'src', type: 'chart', title: 'S', source: 'data:x', subscribes: [{ topic: 'data:x', as: 'data' }],
	helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] }, ...o });
async function srcPage(cards, opts) {
	opts = opts || {};
	const r = await page({ topics: [{ topic: 'data:x', retain: true, publisher: 'script' }], cards: cards },
		{ ...opts, afterLoad: w => { w.JuneauViews.bus.declare('data:x', { retain: true, by: 'test' }); if (opts.afterLoad) opts.afterLoad(w); } });
	const own = r.bus.owner('test'), data = r.w.JuneauViews.data;
	r.publish = async (rows, status, more) => {
		const v = data.put('x', { dims: DIMS, rowCount: rows.length, truncated: false, columns: {
			day: { codes: Int32Array.from(rows, (_, i) => i), dict: rows.map(x => x.day) }, placed: Float64Array.from(rows.map(x => x.placed)) } });
		own.publish('data:x', { schemaVersion: 1, source: 'x', version: v, status: status || 'ready', progress: { loaded: rows.length },
			rowCount: rows.length, truncated: false, dims: DIMS, ...more });
		await flush();
		return v;
	};
	r.publishError = async message => {
		own.publish('data:x', { schemaVersion: 1, source: 'x', version: 0, status: 'error', progress: { loaded: 0 }, rowCount: 0, truncated: false,
			dims: [], error: { message } });
		await flush();
	};
	return r;
}
const rowsOf = n => Array.from({ length: n }, (_, i) => ({ day: 'd' + i, placed: i + 1 }));
const heldJson = body => ({ ok: true, status: 200, json: () => Promise.resolve(body) });

async function section(name, fn) {
	try {
		out[name] = await fn();
	} catch (e) {
		out[name] = { crash: String(e?.stack || e) };
	}
}

(async () => {
	// a01. Line, inline data: the chrome, the accessible name, init arguments and the drawn option.
	await section('line', async () => {
		const r = await page({ cards: [LINE] });
		const i = inst(r, 'c1'), o = lastOption(i), plot = one(r, 'c1', '.jc-chart-plot');
		return {
			figure: all(r, 'c1', 'figure').length, captionId: one(r, 'c1', '.jc-chart-caption').id,
			caption: text(r, 'c1', '.jc-chart-caption'), role: plot.getAttribute('role'),
			labelledby: plot.getAttribute('aria-labelledby'), tabindex: plot.getAttribute('tabindex'),
			canvasHeight: one(r, 'c1', '.jc-chart-canvas').style.height,
			init: [i.theme, i.opts], seriesIds: seriesIds(o), seriesNames: o.series.map(s => s.name),
			datasetIds: (o.dataset || []).map(d => d.id), aria: o.aria, okColor: o.series[1].itemStyle?.color,
			setOptionOpts: r.fake.calls.filter(c => c[0] === 'setOption').map(c => c[2] || null),
			bannerHidden: one(r, 'c1', '.jc-chart-banner').hidden, errors: r.errors
		};
	});

	// a02. The "Show data" table and the keyboard path.
	await section('table', async () => {
		const r = await page({ cards: [LINE] });
		const t = one(r, 'c1', '.jc-chart-toggle'), box = one(r, 'c1', '.jc-chart-table-box');
		const before = { expanded: t.getAttribute('aria-expanded'), text: t.textContent, hidden: box.hidden, cells: all(r, 'c1', 'td').length };
		const wiring = { controls: t.getAttribute('aria-controls'), boxId: box.id, tableName: one(r, 'c1', 'table').getAttribute('aria-labelledby'),
			captionId: one(r, 'c1', '.jc-chart-caption').id };
		t.dispatch('click', {});
		const after = { expanded: t.getAttribute('aria-expanded'), text: t.textContent, hidden: box.hidden };
		const head = all(r, 'c1', 'th').map(n => n.textContent), cells = all(r, 'c1', 'td').map(n => n.textContent);
		const from = r.fake.calls.length;
		key(r, 'c1', 'ArrowRight');
		key(r, 'c1', 'ArrowRight');
		key(r, 'c1', 'ArrowLeft');
		return { before, after, wiring, head, cells, rows: all(r, 'c1', 'tr').length, noteHidden: one(r, 'c1', '.jc-chart-table-note').hidden,
			keys: actions(r, from) };
	});

	// a03. src rows through the per-draw fetch; a stacked horizontal bar.
	await section('bar', async () => {
		const r = await page({ cards: [{ id: 'b1', type: 'chart', title: 'Bars', src: '/api/rows',
			helper: { kind: 'bar', x: 'day', series: [{ field: 'placed' }, { field: 'delivered' }], stacked: true, orientation: 'horizontal' } }] },
			{ fetch: url => url === '/api/rows' ? ok({ rows: ROWS }) : bad(404, {}) });
		const o = lastOption(inst(r, 'b1'));
		return { fetches: r.fetches, stack: o.series.map(s => s.stack || null), yAxis: o.yAxis?.type,
			caption: text(r, 'b1', '.jc-chart-caption'), errors: r.errors };
	});

	// a04. A donut: the centre graphic and the caption without a series count.
	await section('donut', async () => {
		const r = await page({ cards: [{ id: 'd1', type: 'chart', title: 'Mix',
			helper: { kind: 'donut', label: 's', value: 'n', center: 'total' }, data: [{ s: 'a', n: 50 }, { s: 'b', n: 47 }, { s: 'c', n: 3 }] }] });
		const o = lastOption(inst(r, 'd1'));
		const g = (o.graphic || []).find(x => x.id === 'juneau-center');
		return { caption: text(r, 'd1', '.jc-chart-caption'), center: g ? g.children[0].style.text : null, errors: r.errors };
	});

	// a04b. A pie or donut leaves room for its outside labels: a gap between legend entries, the legend lifted off the edge and
	// the ring shifted away from the legend, with the centre text following the ring.
	await section('pieLegend', async () => {
		const shape = async (kind, legend) => {
			const r = await page({ cards: [{ id: 'p1', type: 'chart', title: 'Mix',
				helper: { kind: kind, label: 's', value: 'n', center: 'total', ...(legend ? { legend: legend } : {}) },
				data: [{ s: 'a', n: 50 }, { s: 'b', n: 47 }] }] });
			const o = lastOption(inst(r, 'p1'));
			const g = (o.graphic || []).find(x => x.id === 'juneau-center');
			return { legend: o.legend, center: o.series[0].center, radius: o.series[0].radius, graphic: g ? [g.left, g.top] : null,
				group: g ? { type: g.type, bounding: g.bounding, silent: g.silent, text: g.children.map(x => [x.type, x.style.textAlign, x.style.textVerticalAlign, x.left, x.top]) } : null };
		};
		return { donut: await shape('donut'), pie: await shape('pie'), right: await shape('donut', 'right'), none: await shape('pie', 'none') };
	});

	// a05. A missing series field and a failed fetch: painted on the card and logged once each.
	await section('errors', async () => {
		const r = await page({ cards: [card({ id: 'm', series: [{ field: 'missing' }] }), card({ id: 'f', data: undefined, src: '/api/f' })] },
			{ fetch: () => Promise.reject(new TypeError('boom')) });
		return { missing: cardError(r, 'm'), fetch: cardError(r, 'f'), errors: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a06. refreshMs polls; a failed tick after a good draw keeps the picture, marks the card stale and keeps polling.
	await section('refresh', async () => {
		let n = 0, fail = false;
		const r = await page({ cards: [card({ id: 'p', data: undefined, src: '/api/p', refreshMs: 5000 })] },
			{ fetch: () => fail ? Promise.reject(new TypeError('down')) : ok([{ day: 'Mon', placed: ++n, delivered: 0 }]) });
		const first = col(inst(r, 'p'), 'placed');
		tick(r);
		await flush();
		const second = col(inst(r, 'p'), 'placed');
		fail = true;
		tick(r);
		await flush();
		const stale = { cls: host(r, 'p').querySelector('.jc-chart--stale') !== null, title: one(r, 'p', '.jc-chart--stale') && one(r, 'p', '.jc-chart--stale').getAttribute('title') }; // NOSONAR javascript:S6582 - null (not undefined) reaches the serialized assertion
		fail = false;
		tick(r);
		await flush();
		return { ms: r.intervals.map(x => x.ms), live: r.intervals.filter(x => x.live).length, first, second, stale,
			recovered: one(r, 'p', '.jc-chart--stale') === null, figure: all(r, 'p', 'figure').length,
			instances: r.fake.instances.length, log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a07. refreshMs below the minimum is clamped with E-JS-75.
	await section('clamp', async () => {
		const r = await page({ cards: [card({ id: 'r2', data: undefined, src: '/api/r', refreshMs: 1000 })] }, { fetch: () => ok(ROWS) });
		return { ms: r.intervals.map(x => x.ms), log: r.errors.filter(e => e.includes('E-JS-75')) };
	});

	// a08. A name unknown at mount waits: E-JS-90 on the card while unregistered, then registering it heals the card, for
	// a cycle card AND a source card (whose start() has nothing to await).  A card destroyed while waiting never draws.
	await section('lateRegistration', async () => {
		const fnOpt = fn => ({ tooltip: { valueFormatter: { $fn: fn } } });
		const mk = (id, fn) => card({ id, option: fnOpt(fn) });
		const r = await srcPage([mk('w1', 'app.money'), mk('w2', 'app.never'), mk('w3', 'app.money'), srcCard({ id: 'sw', option: fnOpt('app.money') })]);
		await r.publish(ROWS.map(x => ({ day: x.day, placed: x.placed })));
		const state = id => ({ error: cardError(r, id), drawn: inst(r, id) !== null });
		const before = { w1: state('w1'), sw: state('sw') };
		r.w.JuneauConsole.destroyCard('w3');
		r.w.JuneauConsoleCharts.push(['fn', 'app.money', () => v => '$' + v]);
		await flush();
		const fnOf = id => { const i = inst(r, id); return i ? typeof i.options.at(-1).tooltip.valueFormatter : null; };
		return { before, after: { w1: state('w1'), sw: state('sw') }, fns: { w1: fnOf('w1'), sw: fnOf('sw') }, w2: state('w2'), w3: state('w3'),
			log: r.errors.filter(e => e.includes('E-JS-90')) };
	});

	// a09. The queue drains in order, rejects bad entries, and window.JuneauCharts is aliased as JuneauConsole.charts.
	await section('registry', async () => {
		const r = await page({ cards: [] }, {
			queue: [['fn', 'app.one', () => () => 1], ['fn', 'bare', () => () => 1], ['nope', 'app.x', () => 1], ['decorator', 'app.dec', o => o]]
		});
		return { alias: r.w.JuneauConsole.charts === r.w.JuneauCharts, fns: r.w.JuneauCharts.fn.names().filter(n => n.includes('.')),
			decorators: r.w.JuneauCharts.decorator.names().filter(n => n.includes('.')), log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a10. juneau-charts.js loaded before the shell still registers the card type.
	await section('chartsFirst', async () => {
		const r = await page({ cards: [LINE] }, { chartsFirst: true });
		return { drawn: inst(r, 'c1') !== null, alias: r.w.JuneauConsole.charts === r.w.JuneauCharts, errors: r.errors };
	});

	// a11. Clicks: the on.click topic gets the sanitized payload; onClick.href navigates (modifier: new tab);
	// a disallowed URL is E-JS-79.
	await section('clicks', async () => {
		const got = [];
		const r = await page({
			topics: [{ topic: 'app.picked', retain: false }],
			cards: [card({ id: 'k', on: { click: 'app.picked' }, onClick: { href: '/orders?day={day}' }, publishes: [{ topic: 'app.picked', retain: false }] }),
				card({ id: 'x', onClick: { href: 'java\tscript:alert({day})' } })]
		});
		r.bus.subscribe('app.picked', p => got.push(structuredClone(p)));
		const p = { componentType: 'series', seriesId: 'placed', seriesName: 'Placed', dataIndex: 1, name: 'Tue', value: 5,
			data: { meta: { big: 'x'.repeat(5000) } }, event: { event: {} } };
		inst(r, 'k').fire('click', p);
		inst(r, 'k').fire('click', { ...p, dataIndex: 2, data: { meta: { id: 7 } }, event: { event: { ctrlKey: true } } });
		inst(r, 'x').fire('click', p);
		return { got, navigated: r.navigated, opened: r.opened, log: r.errors.filter(e => e.includes('E-JS-79')) };
	});

	// a12. Text is never markup; zero rows show emptyText.
	await section('escaping', async () => {
		const r = await page({ cards: [card({ id: 'e', a11y: { summary: '<b>bold</b>' }, data: [{ day: '<i>Mon</i>', placed: 1, delivered: 2 }] }),
			card({ id: 'z', data: [], emptyText: 'Nothing yet.' })] });
		openTable(r, 'e');
		return { caption: text(r, 'e', '.jc-chart-caption'), cells: all(r, 'e', 'td').map(n => n.textContent), bold: all(r, 'e', 'b').length,
			empty: text(r, 'z', '.jc-chart-empty'), zInst: inst(r, 'z') === null };
	});

	// a13. Destroying a card stops polling and disposes the instance.
	await section('destroy', async () => {
		const r = await page({ cards: [card({ id: 'g', data: undefined, src: '/api/g', refreshMs: 5000 })] }, { fetch: () => ok(ROWS) });
		const i = inst(r, 'g');
		r.w.JuneauConsole.destroyCard('g');
		await flush();
		return { disposed: i.disposed, live: r.intervals.filter(x => x.live).length };
	});

	// a14. setOption throwing on the first draw is E-JS-80 once.
	await section('setOptionThrows', async () => {
		const r = await page({ cards: [card({ id: 'b' })] }, {
			afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); i.setOption = () => { throw new Error('kaput'); }; return i; }; }
		});
		return { error: cardError(r, 'b'), log: r.errors.filter(e => e.includes('E-JS-80')) };
	});

	// a15. zoom:<id> and legend:<id>: restored from the dashboard and folded in before the first draw, published
	// after it, then republished on datazoom (debounced) and legendselectchanged.
	await section('state', async () => {
		const seen = [];
		const pubs = [{ topic: 'zoom:s', retain: true }, { topic: 'legend:s', retain: true }];
		const r = await page({ cards: [card({ id: 's', publishes: pubs, option: { dataZoom: [{ type: 'inside' }], legend: {} } })] }, {
			before: w => { w.JuneauViews = w.JuneauViews || {}; w.JuneauViews.dashboard = { initial: t => t === 'zoom:s' ? { start: 10, end: 60 } : { selected: { Placed: false } } }; }
		});
		const first = lastOption(inst(r, 's'));
		const restored = { zoom: structuredClone(r.bus.get('zoom:s') || null), legend: structuredClone(r.bus.get('legend:s') || null) };
		r.bus.subscribe('zoom:s', v => seen.push(structuredClone(v)), { retained: false });
		const i = inst(r, 's');
		// The published state is read when the timer fires: it varies per read, so a second timer (no coalescing) or a zero delay shows.
		let reads = 0;
		i.getOption = () => ({ dataZoom: [{ start: 20 + ++reads, end: 80, startValue: 1, endValue: 2 }] });
		i.fire('datazoom', {});
		i.fire('datazoom', {});
		await wait(60);
		const early = seen.length;
		await wait(200);
		i.fire('legendselectchanged', { selected: { Placed: true, Delivered: false } });
		return { folded: { zoom: first.dataZoom[0], legend: first.legend.selected }, restored, early, reads, seen,
			legend: structuredClone(r.bus.get('legend:s')), errors: r.errors };
	});

	// a15c. A legend selection over the 4 KiB state cap publishes only the deselected names; when even that is over the cap the last good
	// published value stays (nothing is published, and never an empty map).
	await section('legendOverCap', async () => {
		const pubs = [{ topic: 'legend:big', retain: true }];
		const r = await page({ cards: [card({ id: 'big', publishes: pubs, option: { legend: {} } })] });
		const i = inst(r, 'big');
		const seen = [];
		r.bus.subscribe('legend:big', v => seen.push(structuredClone(v)), { retained: false });
		const all = (n, off) => { const m = {}; for (let k = 0; k < n; k++) { m['series-name-' + k] = off.indexOf(k) < 0; } return m; };
		i.fire('legendselectchanged', { selected: { A: false, B: true } });
		i.fire('legendselectchanged', { selected: all(400, [3, 7]) });
		i.fire('legendselectchanged', { selected: all(400, Array.from({ length: 400 }, (_, k) => k)) });
		return { seen, last: structuredClone(r.bus.get('legend:big')), errors: r.errors };
	});

	// a15d. A restored legend map over the 4 KiB state cap is folded and re-published as the compact deselected-only map; one too big even
	// compact is neither folded nor re-published.
	await section('legendRestoreOverCap', async () => {
		const mk = (n, off) => { const m = {}; for (let k = 0; k < n; k++) { m['series-name-' + k] = off.indexOf(k) < 0; } return m; };
		const run = async (sel) => {
			const r = await page({ cards: [card({ id: 'rs', publishes: [{ topic: 'legend:rs', retain: true }], option: { legend: {} } })] }, {
				before: w => { w.JuneauViews = w.JuneauViews || {}; w.JuneauViews.dashboard = { initial: () => ({ selected: sel }) }; }
			});
			const o = lastOption(inst(r, 'rs'));
			return { folded: o.legend?.selected || null, published: structuredClone(r.bus.get('legend:rs') || null), errors: r.errors };
		};
		return { compact: await run(mk(400, [3, 7])), tooBig: await run(mk(400, Array.from({ length: 400 }, (_, k) => k))) };
	});

	// a15e. A configured legendselectchanged event topic carries the compact deselected-only map when the selection is over the 4 KiB
	// cap, and null (never a misleading empty map) when even that does not fit.
	await section('legendEventOverCap', async () => {
		const r = await page({ cards: [card({ id: 'ev', on: { legendselectchanged: 'app.legend' }, publishes: [{ topic: 'app.legend', retain: false }], option: { legend: {} } })] });
		const i = inst(r, 'ev');
		const seen = [];
		r.bus.subscribe('app.legend', v => seen.push(structuredClone(v).selected), { retained: false });
		const mk = (n, off) => { const m = {}; for (let k = 0; k < n; k++) { m['series-name-' + k] = off.indexOf(k) < 0; } return m; };
		i.fire('legendselectchanged', { selected: { A: false, B: true } });
		i.fire('legendselectchanged', { selected: mk(400, [3, 7]) });
		i.fire('legendselectchanged', { selected: mk(400, Array.from({ length: 400 }, (_, k) => k)) });
		return { seen, errors: r.errors };
	});

	// a15b. A destroyed card with a datazoom timer pending publishes nothing, then or later.
	await section('zoomDestroy', async () => {
		const r = await page({ cards: [card({ id: 'zd', publishes: [{ topic: 'zoom:zd', retain: true }], option: { dataZoom: [{ type: 'inside' }] } })] });
		const i = inst(r, 'zd'), seen = [];
		i.getOption = () => ({ dataZoom: [{ start: 1, end: 2 }] });
		r.bus.subscribe('zoom:zd', v => seen.push(v), { retained: false });
		i.fire('datazoom', {});
		r.w.JuneauConsole.destroyCard('zd');
		await wait(250);
		i.fire('datazoom', {});
		i.fire('click', { componentType: 'series', dataIndex: 0 });
		await wait(250);
		return { seen: seen.length, disposed: i.disposed, errors: r.errors };
	});

	// a16. cmd:<id> ops: highlight, group fan-out, bad options and export.
	await section('ops', async () => {
		const r = await page({ cards: [card({ id: 'o1', group: 'g' }), card({ id: 'o2', group: 'g' }), card({ id: 'o3' })] });
		const from = r.fake.calls.length;
		r.bus.publish('cmd:o1', { schemaVersion: 1, op: 'highlight', options: { seriesId: 'placed', dataIndex: 0 } });
		r.bus.publish('cmd:o1', { schemaVersion: 1, op: 'zoomTo', target: 'group', options: { start: 0, end: 50 } });
		r.bus.publish('cmd:o3', { schemaVersion: 1, op: 'downplay', target: 'group' });
		r.bus.publish('cmd:o3', { schemaVersion: 1, op: 'showTip', options: 'x' });
		r.bus.publish('cmd:o3', { schemaVersion: 1, op: 'export', options: { type: 'svg' } });
		r.bus.publish('cmd:o3', { schemaVersion: 1, op: 'export', options: { type: 'png', pixelRatio: 2 } });
		await flush();
		return { actions: actions(r, from), connect: r.fake.calls.filter(c => c[0] === 'connect').map(c => c[1]),
			groups: r.fake.instances.map(i => i.group), downloads: r.downloads, log: r.errors.filter(e => e.includes('E-JS-103')) };
	});

	// a17. The theme bridge: the page's theme stylesheet names the ECharts theme, tokens fill it, lang zh picks the
	// ZH locale, renderer svg is passed through, and juneau:theme-changed re-themes every token-themed chart in place.
	await section('theme', async () => {
		const tokens = { '--jc-chart-1': '#123456', '--jc-chart-text': '#0a0b0c', '--jc-chart-axis': '#a1a2a3', '--jc-chart-band': 'rgba(1, 2, 3, 0.1)' };
		const r = await page({ cards: [card({ id: 't1' }), card({ id: 't2', renderer: 'svg', theme: 'custom' })] },
			{ theme: 'light-red', lang: 'zh-CN', tokens });
		const reg = r.fake.calls.filter(c => c[0] === 'registerTheme');
		const before = r.fake.instances.length, i1 = inst(r, 't1'), i2 = inst(r, 't2');
		const opts1 = i1.optionOpts.length, opts2 = i2.optionOpts.length;
		tokens['--jc-chart-1'] = '#654321';
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		const regAfter = r.fake.calls.filter(c => c[0] === 'registerTheme'), t = reg[0]?.[2];
		return { registered: reg.map(c => c[1]), color0: t?.color[0],
			registeredAfter: regAfter.map(c => c[1]), color0After: regAfter.length > 1 ? regAfter.at(-1)[2].color[0] : null,
			inits: r.fake.calls.filter(c => c[0] === 'init').map(c => [c[1], c[2]]),
			reinit: r.fake.instances.length - before, disposed: r.fake.instances.filter(i => i.disposed).length,
			setThemeCalls: { t1: i1.themes.length, t2: i2.themes.length }, setThemeColor0: i1.themes.length ? i1.themes[0].color[0] : null,
			setOptionAfter: { t1: i1.optionOpts.length - opts1, t2: i2.optionOpts.length - opts2 },
			replaceMerge: i1.optionOpts.length > opts1 ? i1.optionOpts.at(-1) : null,
			sameInstance: inst(r, 't1') === i1 && inst(r, 't2') === i2,
			toolbox: t?.toolbox, dataZoom: t?.dataZoom, brush: t?.brush,
			tooltipRenderMode: t && t.tooltip.renderMode === undefined, errors: r.errors }; // NOSONAR javascript:S6582 - null vs true differs when t is null
	});

	// A theme change while a refresh is in flight waits for that draw to end, then re-themes the same instance.
	await section('themePending', async () => {
		let n = 0, release = null;
		const r = await page({ cards: [card({ id: 'tp', data: undefined, src: '/api/tp', refreshMs: 5000 })] }, {
			fetch: () => {
				const k = ++n;
				if (k === 2) return new Promise(res => { release = () => res(heldJson([{ day: 'Mon', placed: 2, delivered: 0 }])); });
				return ok([{ day: 'Mon', placed: k, delivered: 0 }]);
			}
		});
		const i = inst(r, 'tp'), first = col(i, 'placed');
		tick(r);
		await flush();
		const from = r.fake.calls.length;
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		const during = i.themes.length;
		release();
		await flush();
		return { first, during, after: i.themes.length, final: col(i, 'placed'), fetches: n,
			order: r.fake.calls.slice(from).map(c => c[0]).filter(c => c === 'setOption' || c === 'setTheme' || c === 'dispose' || c === 'init'),
			instances: r.fake.instances.length, live: r.fake.instances.filter(x => !x.disposed).length,
			stale: one(r, 'tp', '.jc-chart--stale') !== null, errors: r.errors };
	});

	// ECharts without setTheme (an app pinning ECharts 5): a token-themed chart is disposed and redrawn instead.
	await section('themeEc5', async () => {
		const r = await page({ cards: [card({ id: 'e1' }), card({ id: 'e2', theme: 'custom' })] }, {
			afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); delete i.setTheme; return i; }; }
		});
		const before = r.fake.instances.length, e2 = inst(r, 'e2');
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		const fresh = r.fake.instances.slice(before);
		return { disposed: r.fake.instances.filter(i => i.disposed).length, reinit: fresh.length, reinitTheme: fresh.map(i => i.theme),
			customKept: inst(r, 'e2') === e2, drawn: inst(r, 'e1') ? col(inst(r, 'e1'), 'placed') : null, errors: r.errors };
	});

	// The dark scheme stylesheet is linked ahead of the palette stylesheet; the theme is still named after the palette.
	await section('themeNameSkipsDark', async () => {
		const r = await page({ cards: [card({ id: 'nd' })] }, { theme: 'light-red', links: ['/console/juneau-theme-dark.css?v=1'] });
		// The dark layer alone names no palette: the default theme name applies.
		const only = await page({ cards: [card({ id: 'nd' })] }, { theme: 'dark' });
		return { registered: r.fake.calls.filter(c => c[0] === 'registerTheme').map(c => c[1]), init: inst(r, 'nd').theme,
			darkOnly: only.fake.calls.filter(c => c[0] === 'registerTheme').map(c => c[1]) };
	});

	// A semantic tone reads the text role (legible on the scheme's surface) and falls back to the fill when no text role is declared.
	await section('tonesText', async () => {
		const tone = { series: [{ field: 'placed', label: 'Placed', tone: 'danger' }, { field: 'delivered', label: 'Delivered', tone: 'ok' }] };
		const color = (r, i) => lastOption(inst(r, 'tt')).series[i].itemStyle.color;
		const withText = await page({ cards: [card({ id: 'tt', ...tone })] }, { tokens: {
			'--jc-danger-text': '#f07a75', '--jc-danger': '#c23934', '--jc-success-text': '#5cc275', '--jc-success': '#2e8540' } });
		const fillOnly = await page({ cards: [card({ id: 'tt', ...tone })] }, { tokens: { '--jc-danger': '#aa1111', '--jc-success': '#11aa11' } });
		return { withText: color(withText, 0), fillOnly: color(fillOnly, 0), okWithText: color(withText, 1), okFillOnly: color(fillOnly, 1) };
	});

	// An error handle ends the draw in flight: a re-theme owed to that draw runs on the kept picture, whether the draw was the
	// source card's own (1) or the re-theme draw itself (2).
	await section('themeErrorHandle', async () => {
		const run = async themeFirst => {
			let k = 0;
			const r = await srcPage([srcCard({ id: 'eh', decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })], {
				fetch: () => ++k === 2 ? new Promise(() => {}) : ok([{ day: 'd0', label: 'm' }])
			});
			await r.publish(rowsOf(1));
			const i = inst(r, 'eh');
			if (!themeFirst) await r.publish(rowsOf(2));
			r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
			await flush();
			const held = i.themes.length;
			await r.publishError('upstream down');
			await flush();
			return { held, themes: i.themes.length, sameInstance: inst(r, 'eh') === i, placed: col(i, 'placed'),
				stale: one(r, 'eh', '.jc-chart--stale') !== null, error: cardError(r, 'eh'), fetches: k };
		};
		return { sourceDraw: await run(false), rethemeDraw: await run(true) };
	});

	// ECharts without setTheme: a second theme change while the redraw for the first is in flight is not lost; the chart ends on
	// the latest tokens.
	await section('themeEc5Twice', async () => {
		let k = 0, release = null;
		const tokens = { '--jc-success': '#00000a' };
		const r = await page({ cards: [card({ id: 'e5', decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })] }, {
			tokens,
			afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); delete i.setTheme; return i; }; },
			fetch: () => ++k === 2 ? new Promise(res => { release = () => res(heldJson([{ day: 'Mon', label: 'm' }])); }) : ok([{ day: 'Mon', label: 'm' }])
		});
		tokens['--jc-success'] = '#0000b0';
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		tokens['--jc-success'] = '#000c00';
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		release();
		await flush();
		const i = inst(r, 'e5');
		return { okColor: i ? lastOption(i).series[1].itemStyle.color : null, instances: r.fake.instances.length,
			live: r.fake.instances.filter(x => !x.disposed).length, fetches: k, errors: r.errors };
	});

	// A re-theme draw that fails keeps the picture and marks it stale, as a failed refresh does; when the theme moved during it, the
	// newer theme is still applied.
	await section('themeFailure', async () => {
		const run = async moved => {
			let k = 0, release = null;
			const tokens = { '--jc-success': '#00000a' };
			const r = await page({ cards: [card({ id: 'tf', decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })] }, {
				tokens,
				fetch: () => {
					if (++k !== 2) return ok([{ day: 'Mon', label: 'm' }]);
					return moved ? new Promise(res => { release = () => res({ ok: false, status: 500, json: () => Promise.resolve({}) }); }) : bad(500, {}); // NOSONAR javascript:S2004 - test nesting mirrors describe/it structure
				}
			});
			const i = inst(r, 'tf');
			r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
			await flush();
			if (moved) {
				tokens['--jc-success'] = '#000c00';
				r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
				await flush();
				release();
				await flush();
			}
			return { themes: i.themes.length, sameInstance: inst(r, 'tf') === i, okColor: lastOption(i).series[1].itemStyle.color,
				stale: one(r, 'tf', '.jc-chart--stale') !== null, error: cardError(r, 'tf'), fetches: k,
				logged: r.errors.filter(e => e.includes('juneau-charts')).length };
		};
		return { once: await run(false), moved: await run(true) };
	});

	// A card destroyed while its re-theme waits for a draw never re-themes or draws again.
	await section('themeDestroyPending', async () => {
		let n = 0, release = null;
		const r = await page({ cards: [card({ id: 'td', data: undefined, src: '/api/td', refreshMs: 5000 })] }, {
			fetch: () => ++n === 2 ? new Promise(res => { release = () => res(heldJson([{ day: 'Mon', placed: 2, delivered: 0 }])); })
				: ok([{ day: 'Mon', placed: n, delivered: 0 }])
		});
		const i = inst(r, 'td');
		tick(r);
		await flush();
		r.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		r.w.JuneauConsole.destroyCard('td');
		const from = r.fake.calls.length;
		release();
		await flush();
		return { after: r.fake.calls.slice(from).map(c => c[0]).filter(c => c === 'setOption' || c === 'setTheme' || c === 'init'),
			themes: i.themes.length, fetches: n, errors: r.errors };
	});

	// A transparent chart background exports on the surface colour, so the image keeps the page's contrast.
	await section('exportSurface', async () => {
		const r = await page({ cards: [card({ id: 'xs' })] }, { tokens: { '--jc-chart-bg': 'transparent', '--jc-surface': '#24272e' } });
		await r.w.JuneauConsole.cardApi('xs').export('png');
		const solid = await page({ cards: [card({ id: 'xs' })] }, { tokens: { '--jc-chart-bg': '#010203', '--jc-surface': '#24272e' } });
		await solid.w.JuneauConsole.cardApi('xs').export('png');
		const bg = x => x.fake.calls.filter(c => c[0] === 'getDataURL').map(c => c[1].backgroundColor);
		return { transparent: bg(r), solid: bg(solid) };
	});

	// a18. A source card: the data role draws the DataRegistry frame its handle names, shows load progress, goes
	// stale, rejects a malformed handle (E-JS-95) and redraws from the current handle on refresh.
	await section('source', async () => {
		const handle = (v, status, extra) => ({ schemaVersion: 1, source: 'x', version: v, status, progress: { loaded: 0 },
			rowCount: 0, truncated: false, dims: [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }], ...extra });
		const r = await page({
			topics: [{ topic: 'data:x', retain: true, publisher: 'script' }],
			cards: [{ id: 'src', type: 'chart', title: 'S', source: 'data:x', subscribes: [{ topic: 'data:x', as: 'data' }],
				helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] } }]
		}, { afterLoad: w => w.JuneauViews.bus.declare('data:x', { retain: true, by: 'test' }) });
		const own = r.bus.owner('test'), data = r.w.JuneauViews.data;
		const frame = rows => ({ dims: handle(0).dims, rowCount: rows.length, truncated: false,
			columns: { day: { codes: Int32Array.from(rows, (_, i) => i), dict: rows.map(x => x.day) }, placed: Float64Array.from(rows.map(x => x.placed)) } });
		own.publish('data:x', handle(0, 'loading', { progress: { loaded: 50, total: 200 } }));
		await flush();
		const loading = text(r, 'src', '.jc-card-loading');
		const v1 = data.put('x', frame(ROWS));
		own.publish('data:x', handle(v1, 'ready', { rowCount: 3 }));
		await flush();
		const drawn = { points: col(inst(r, 'src'), 'day').length, overlay: one(r, 'src', '.jc-chart-overlay').hidden };
		own.publish('data:x', handle(v1, 'loading', { rowCount: 3, progress: { loaded: 50, total: 200 } }));
		await flush();
		const progress = text(r, 'src', '.jc-chart-overlay');
		const v2 = data.put('x', frame(ROWS.slice(0, 2)));
		own.publish('data:x', handle(v2, 'stale', { rowCount: 2 }));
		await flush();
		const stale = { points: col(inst(r, 'src'), 'day').length, stale: one(r, 'src', '.jc-chart--stale') !== null };
		const setOptions = r.fake.calls.filter(c => c[0] === 'setOption').length;
		r.bus.publish('cmd:src', { schemaVersion: 1, op: 'refresh' });
		await flush();
		const refreshDrew = r.fake.calls.filter(c => c[0] === 'setOption').length - setOptions;
		const refreshOpts = r.fake.calls.findLast(c => c[0] === 'setOption')[2];
		own.publish('data:x', { schemaVersion: 1, nope: true });
		await flush();
		return { loading, drawn, progress, stale, refreshDrew, refreshOpts, points: col(inst(r, 'src'), 'day').length,
			log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a19. The card API, the truncated banner, the table cap and a11y.table false.
	await section('api', async () => {
		const many = Array.from({ length: 7 }, (_, i) => ({ day: 'd' + i, placed: i, delivered: i }));
		const r = await page({ cards: [card({ id: 'a', data: many, a11y: { tableMax: 5 }, option: { tooltip: { valueFormatter: { $fn: 'percent' } } } }),
			card({ id: 'n', a11y: { table: false } })] });
		openTable(r, 'a');
		const api = r.w.JuneauConsole.cardApi('a');
		const url = api && await api.export('png', { pixelRatio: 2 });
		let svg = null;
		try { await api.export('svg'); } catch (e) { svg = e.message; }
		const marked = api?.getOption();
		return { note: text(r, 'a', '.jc-chart-table-note'), rows: all(r, 'a', 'tr').length, url, svg,
			marker: marked?.tooltip.valueFormatter, chart: api?.chart() === inst(r, 'a'),
			noToggle: one(r, 'n', '.jc-chart-toggle') === null, noTable: one(r, 'n', '.jc-chart-table') === null };
	});

	// a20. Fetch isolation: a card src plus two decorator srcs all complete, none aborting another; destroying
	// the card mid-draw aborts all three.
	await section('isolation', async () => {
		const MARKS = [{ at: 'Tue', label: 'Deploy' }];
		const decorated = id => ({ id, type: 'chart', title: 'I', src: '/api/rows', helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] },
			decorators: [{ use: 'markers', src: '/api/m1', at: 'at', label: 'label' }, { use: 'markers', src: '/api/m2', at: 'at', label: 'label' }] });
		const signals = [];
		const r1 = await page({ cards: [decorated('i1')] }, {
			fetch: (url, init) => { signals.push(init.signal); return ok(url === '/api/rows' ? { rows: ROWS } : MARKS); }
		});
		const o = lastOption(inst(r1, 'i1'));
		const done = { urls: r1.fetches.map(f => f.url).sort((a, b) => (a < b ? -1 : Number(a > b))), aborted: signals.filter(x => x?.aborted).length,
			carriers: seriesIds(o).filter(x => x?.startsWith('juneau-markers-')), errors: r1.errors };
		const pending = [];
		const r2 = await page({ cards: [decorated('i2')] }, {
			fetch: (url, init) => {
				pending.push(init.signal);
				if (url === '/api/rows') return ok({ rows: ROWS });
				return new Promise((res, rej) => init.signal.addEventListener('abort', () => rej(Object.assign(new Error('aborted'), { name: 'AbortError' }))));
			}
		});
		r2.w.JuneauConsole.destroyCard('i2');
		await flush();
		// The decorators fetch after the frame exists, so a card src still in flight is the only fetch to abort.
		const held = [];
		const r3 = await page({ cards: [decorated('i3')] }, {
			fetch: (url, init) => { held.push(init.signal); return new Promise((res, rej) => init.signal.addEventListener('abort', () => rej(Object.assign(new Error('aborted'), { name: 'AbortError' })))); }
		});
		r3.w.JuneauConsole.destroyCard('i3');
		await flush();
		return { done, midDraw: { fetched: pending.length, aborted: pending.filter(x => x.aborted).length, errors: r2.errors },
			srcHeld: { fetched: r3.fetches.map(f => f.url), aborted: held.filter(x => x.aborted).length, errors: r3.errors } };
	});

	// a21. Large data: a 1M-row frame is bound by reference, and the $fn walk never reads a column.
	await section('large', async () => {
		const N = 1000000;
		const placed = new Float64Array(N);
		let reads = 0;
		const spy = new Proxy(placed, { get: (t, k) => { if (typeof k === 'string' && /^\d+$/.test(k)) { reads++; } const v = t[k]; return typeof v === 'function' ? v.bind(t) : v; } });
		const r = await page({
			topics: [{ topic: 'data:big', retain: true, publisher: 'script' }],
			cards: [{ id: 'L', type: 'chart', title: 'L', source: 'data:big', subscribes: [{ topic: 'data:big', as: 'data' }], a11y: { table: false },
				helper: { kind: 'line', x: 'n', series: [{ field: 'placed' }] }, option: { tooltip: { valueFormatter: { $fn: 'compact' } } } }]
		}, { afterLoad: w => w.JuneauViews.bus.declare('data:big', { retain: true, by: 'test' }) });
		const dims = [{ name: 'n', type: 'number' }, { name: 'placed', type: 'number' }];
		const n = new Float64Array(N);
		const v = r.w.JuneauViews.data.put('big', { dims, rowCount: N, truncated: false, columns: { n, placed: spy } });
		const before = reads;
		r.bus.owner('test').publish('data:big', { schemaVersion: 1, source: 'big', version: v, status: 'ready', progress: { loaded: N },
			rowCount: N, truncated: false, dims });
		await flush();
		const i = inst(r, 'L'), last = i?.options.at(-1);
		return { sameArray: last?.dataset[0].source.placed === spy, sameX: last?.dataset[0].source.n === n,
			walked: reads - before, fn: !!last && typeof last.tooltip.valueFormatter === 'function',
			sampling: last ? last.series[0].sampling : null, errors: r.errors };
	});

	// a22. Row inference: a column's type is sniffed from its first non-null value within the first INFER_ROWS
	// rows (bool, number, string); a key that first appears after row INFER_ROWS is not a column; a helper field that no row
	// carries is E-JS-63 for inline data and for a src response alike.
	await section('infer', async () => {
		const rows = [{ day: 'Mon', placed: 1, flag: null }, { day: 'Tue', placed: 2, flag: true }, { day: 'Wed', placed: 3, flag: false }];
		const r = await page({ cards: [card({ id: 'n1', data: rows, series: [{ field: 'placed' }] })] });
		const big = Array.from({ length: 101 }, (_, i) => i < 100 ? { day: 'd' + i, placed: i } : { day: 'd' + i, placed: i, late: 'x' });
		const r2 = await page({ cards: [card({ id: 'n2', data: big, series: [{ field: 'placed' }] })] });
		const r3 = await page({ cards: [card({ id: 'n3', data: undefined, src: '/api/n3', series: [{ field: 'absent' }] })] }, { fetch: () => ok(ROWS) });
		openTable(r, 'n1');
		openTable(r2, 'n2');
		const o1 = inst(r, 'n1').options[0].dataset[0].source;
		return { flagCol: o1.flag && Array.from(o1.flag), head: all(r, 'n1', 'th').map(n => n.textContent), cells: all(r, 'n1', 'td').map(n => n.textContent),
			bigHead: all(r2, 'n2', 'th').map(n => n.textContent), bigDrawn: inst(r2, 'n2') !== null,
			srcMissing: cardError(r3, 'n3'), errors: r.errors.concat(r2.errors) };
	});

	// a23. A renderer that throws on a polled redraw is E-JS-80 on the card (stale, last picture kept) and polling goes on.
	await section('rendererThrow', async () => {
		let n = 0, boom = false;
		const r = await page({ cards: [card({ id: 'q', data: undefined, src: '/api/q', refreshMs: 5000 })] }, {
			fetch: () => ok([{ day: 'Mon', placed: ++n, delivered: 0 }]),
			afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); const so = i.setOption; i.setOption = (...b) => { if (boom) { throw new Error('kaput'); } return so.apply(i, b); }; return i; }; }
		});
		const first = col(inst(r, 'q'), 'placed');
		boom = true;
		tick(r);
		await flush();
		const during = { stale: one(r, 'q', '.jc-chart--stale') !== null, live: r.intervals.filter(x => x.live).length };
		boom = false;
		tick(r);
		await flush();
		return { first, during, after: col(inst(r, 'q'), 'placed'), recovered: one(r, 'q', '.jc-chart--stale') === null,
			log: r.errors.filter(e => e.includes('E-JS-80')) };
	});

	// a24. Decorator carrier series (ids juneau-*) are not data series: not counted in the caption or the accessible
	// summary, whether or not they carry data; a click on one has no row.
	await section('carriers', async () => {
		const withDec = id => card({ id, decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }, { use: 'bands', values: [], start: 'start', end: 'end' }] });
		const r = await page({ cards: [withDec('k1'), card({ id: 'k2' })] }, { fetch: () => ok([{ day: 'Tue', label: 'Deploy' }]) });
		const o = lastOption(inst(r, 'k1'));
		return { withCaption: text(r, 'k1', '.jc-chart-caption'), plainCaption: text(r, 'k2', '.jc-chart-caption'),
			ids: seriesIds(o), ariaEnabled: !!(o.aria?.enabled), errors: r.errors };
	});

	// a25. Every URL the lifecycle fetches or links must be a same-origin path: a card src, a map url, a decorator
	// src and an exported data URL that is not an image.
	await section('sameOrigin', async () => {
		const bad = ['https://evil.example/x', '//evil.example/x', 'javascript:alert(1)', '/a/../b', String.raw`/a\b`];
		const cards = bad.map((u, i) => card({ id: 'u' + i, data: undefined, src: u }));
		cards.push(card({ id: 'um', maps: { m: 'https://evil.example/m.json' } }),
			card({ id: 'ud', decorators: [{ use: 'markers', src: '//evil.example/m', at: 'day', label: 'label' }] }), card({ id: 'ux', renderer: 'svg' }));
		const r = await page({ cards }, { fetch: () => ok(ROWS), afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); i.getDataURL = () => 'javascript:alert(1)'; return i; }; } });
		r.bus.publish('cmd:ux', { schemaVersion: 1, op: 'export', options: { type: 'svg' } });
		await flush();
		return { fetches: r.fetches.map(f => f.url), errors: bad.map((u, i) => cardError(r, 'u' + i)), map: cardError(r, 'um'), dec: cardError(r, 'ud'),
			downloads: r.downloads, log: r.errors.filter(e => e.includes('E-JS-103')) };
	});

	// a26. A throw while the draw context is built is a rejection of that draw: painted on the card once, never an uncaught throw.
	await section('ctxThrow', async () => {
		const r = await page({ cards: [card({ id: 'x1' })] }, {
			afterLoad: w => { Object.defineProperty(w.JuneauViews, 'data', { configurable: true, get() { throw new Error('ctx-boom'); } }); }
		});
		return { error: cardError(r, 'x1'), drawn: inst(r, 'x1') !== null, log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a27. The published click payload is JSON-safe: meta with a Date, a function, NaN, undefined, a cycle and a __proto__ key.
	await section('meta', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.picked', retain: false }], cards: [card({ id: 'mm', on: { click: 'app.picked' }, publishes: [{ topic: 'app.picked', retain: false }] })] });
		r.bus.subscribe('app.picked', p => got.push(p));
		const cyc = { a: 1 };
		cyc.self = cyc;
		const meta = JSON.parse('{"__proto__":{"polluted":1},"ok":1}');
		Object.assign(meta, { d: new Date(0), f() {}, n: Number.NaN, u: undefined, nest: { list: [1, undefined, () => 1] }, cyc });
		inst(r, 'mm').fire('click', { componentType: 'series', seriesId: 'placed', dataIndex: 0, name: 'Mon', value: [1, Number.NaN, new Date(0)], data: { meta }, event: { event: {} } });
		const p = got[0];
		let json = null, err = null;
		try { json = JSON.parse(JSON.stringify(p)); } catch (e) { err = String(e); } // NOSONAR javascript:S7784 - the test asserts the payload survives a JSON round-trip
		return { published: got.length, err, meta: json?.meta, value: json?.value, sameShape: p ? JSON.stringify(p) === JSON.stringify(structuredClone(p)) : null,
			polluted: ({}).polluted === undefined };
	});


	// a28. A source card whose first handle is status "error": painted and logged once; the shell's role path adds no E-JS-8.
	await section('sourceError', async () => {
		const r = await page({
			topics: [{ topic: 'data:y', retain: true, publisher: 'script' }],
			cards: [{ id: 'se', type: 'chart', title: 'S', source: 'data:y', subscribes: [{ topic: 'data:y', as: 'data' }],
				helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] } }]
		}, { afterLoad: w => w.JuneauViews.bus.declare('data:y', { retain: true, by: 'test' }) });
		r.bus.owner('test').publish('data:y', { schemaVersion: 1, source: 'y', version: 0, status: 'error', progress: { loaded: 0 }, rowCount: 0,
			truncated: false, dims: [], error: { message: 'upstream down' } });
		await flush();
		return { error: cardError(r, 'se'), drawn: inst(r, 'se') !== null, errors: r.errors };
	});

	// a29. The same context throw on the source-card path (onHandle): one card error, no second E-JS-8 from the shell's role settle.
	await section('ctxThrowSource', async () => {
		const r = await page({
			topics: [{ topic: 'data:z', retain: true, publisher: 'script' }],
			cards: [{ id: 'sz', type: 'chart', title: 'S', source: 'data:z', subscribes: [{ topic: 'data:z', as: 'data' }],
				helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] } }]
		}, { afterLoad: w => {
			w.JuneauViews.bus.declare('data:z', { retain: true, by: 'test' });
			Object.defineProperty(w, 'JuneauCharts', { configurable: true, get() { throw new Error('reg-boom'); } });
		} });
		const dims = [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }];
		const v = r.w.JuneauViews.data.put('z', { dims, rowCount: 1, truncated: false,
			columns: { day: { codes: Int32Array.from([0]), dict: ['Mon'] }, placed: Float64Array.from([1]) } });
		r.bus.owner('test').publish('data:z', { schemaVersion: 1, source: 'z', version: v, status: 'ready', progress: { loaded: 1 }, rowCount: 1, truncated: false, dims });
		await flush();
		return { error: cardError(r, 'sz'), drawn: inst(r, 'sz') !== null, errors: r.errors };
	});


	// a30. a source card whose handle is empty (or errored) keeps its "No data." (or error) text on cmd refresh, and a refresh
	// re-seeds a card that has no chart instance from the CURRENT handle instead of sitting on "Loading...".
	await section('sourceRefresh', async () => {
		const r = await srcPage([srcCard()]);
		await r.publish([]);
		const empty = { empty: text(r, 'src', '.jc-chart-empty'), loading: text(r, 'src', '.jc-card-loading') };
		r.bus.publish('cmd:src', { schemaVersion: 1, op: 'refresh' });
		await flush();
		const emptyAfter = { empty: text(r, 'src', '.jc-chart-empty'), loading: text(r, 'src', '.jc-card-loading'), drawn: inst(r, 'src') !== null };
		await r.publish(rowsOf(2));
		await r.publishError('upstream down');
		const err = { error: cardError(r, 'src'), drawn: inst(r, 'src') !== null, stale: one(r, 'src', '.jc-chart--stale') !== null };
		r.bus.publish('cmd:src', { schemaVersion: 1, op: 'refresh' });
		await flush();
		const errAfter = { error: cardError(r, 'src'), loading: text(r, 'src', '.jc-card-loading'), stale: one(r, 'src', '.jc-chart--stale') !== null };
		const r2 = await srcPage([srcCard()]);
		await r2.publishError('first handle is an error');
		r2.bus.publish('cmd:src', { schemaVersion: 1, op: 'refresh' });
		await flush();
		return { empty, emptyAfter, err, errAfter, firstError: { error: cardError(r2, 'src'), loading: text(r2, 'src', '.jc-card-loading') },
			log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a31. the resize observer follows the plot.  One live observer per drawn card, re-pointed after empty -> data, disconnected on
	// empty and destroy; a frame request pending at either moment is cancelled; a run frame resizes the chart and clears the request.
	await section('resize', async () => {
		const r = await srcPage([srcCard()]);
		await r.publish(rowsOf(3));
		const plot1 = one(r, 'src', '.jc-chart-plot');
		const state = () => r.ros.map(x => ({ live: x.live, onPlot: x.target === one(r, 'src', '.jc-chart-plot') && x.target !== null }));
		const drawn = { ros: state(), firstOnPlot1: r.ros[0].target === plot1 };
		r.ros.find(x => x.live).cb();
		const pendingBeforeEmpty = r.rafs.filter(x => !x.cancelled).length;
		await r.publish([]);
		const empty = { ros: r.ros.map(x => x.live), rafsCancelled: r.rafs.map(x => x.cancelled), pendingBefore: pendingBeforeEmpty };
		await r.publish(rowsOf(2));
		const redrawn = state();
		const live = r.ros.find(x => x.live);
		live.cb();
		const resizesBefore = r.fake.calls.filter(c => c[0] === 'resize').length;
		r.rafs.at(-1).fn();
		const afterFrame = { resizes: r.fake.calls.filter(c => c[0] === 'resize').length - resizesBefore };
		live.cb();
		const rafCount = r.rafs.length;
		r.w.JuneauConsole.destroyCard('src');
		await flush();
		return { drawn, empty, redrawn, afterFrame, afterDestroy: { ros: r.ros.map(x => x.live), lastRafCancelled: r.rafs[rafCount - 1].cancelled, rafs: rafCount } };
	});

	// a32. a time value outside the Date range, or Infinity, is shown as its own text; it never fails the draw (nor reads as a fetch failure).
	await section('badTime', async () => {
		const timeCard = (id, data) => card({ id, x: undefined, kind: undefined, series: undefined,
			helper: { kind: 'line', x: { field: 'day', type: 'time' }, series: [{ field: 'placed' }] }, data });
		const r = await page({ cards: [timeCard('t1', [{ day: 1e20, placed: 1 }, { day: 5, placed: 2 }, { day: 86400000, placed: 3 }]),
			timeCard('t2', [{ day: Infinity, placed: 1 }, { day: 86400000, placed: 2 }, { day: 172800000, placed: 3 }])] });
		openTable(r, 't1');
		openTable(r, 't2');
		return { t1: { error: cardError(r, 't1'), drawn: inst(r, 't1') !== null, caption: text(r, 't1', '.jc-chart-caption'), cells: all(r, 't1', 'td').map(n => n.textContent) },
			t2: { error: cardError(r, 't2'), drawn: inst(r, 't2') !== null, caption: text(r, 't2', '.jc-chart-caption'), cells: all(r, 't2', 'td').map(n => n.textContent) },
			errors: r.errors, source: await (async () => {
				const rs = await srcPage([srcCard({ id: 'ts', helper: { kind: 'line', x: { field: 'day', type: 'time' }, series: [{ field: 'placed' }] } })]);
				const dims = [{ name: 'day', type: 'time' }, { name: 'placed', type: 'number' }];
				const v = rs.w.JuneauViews.data.put('x', { dims, rowCount: 3, truncated: false,
					columns: { day: Float64Array.from([Infinity, 86400000, 1e20]), placed: Float64Array.from([1, 2, 3]) } });
				rs.bus.owner('test').publish('data:x', { schemaVersion: 1, source: 'x', version: v, status: 'ready', progress: { loaded: 3 }, rowCount: 3, truncated: false, dims });
				await flush();
				openTable(rs, 'ts');
				return { error: cardError(rs, 'ts'), drawn: inst(rs, 'ts') !== null, caption: text(rs, 'ts', '.jc-chart-caption'),
					cells: all(rs, 'ts', 'td').map(n => n.textContent), errors: rs.errors };
			})() };
	});

	// a33. On the source path: a redraw that fails keeps the last picture, marks it stale and logs; a later good handle recovers.
	await section('sourceRedrawThrow', async () => {
		let boom = false;
		const r = await srcPage([srcCard()], { afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); const so = i.setOption; i.setOption = (...b) => { if (boom) { throw new Error('kaput'); } return so.apply(i, b); }; return i; }; } });
		await r.publish(rowsOf(1));
		const first = col(inst(r, 'src'), 'placed');
		boom = true;
		await r.publish(rowsOf(2));
		const during = { figure: all(r, 'src', 'figure').length, stale: one(r, 'src', '.jc-chart--stale') !== null, error: cardError(r, 'src'), points: col(inst(r, 'src'), 'placed') };
		boom = false;
		await r.publish(rowsOf(3));
		return { first, during, after: col(inst(r, 'src'), 'placed'), recovered: one(r, 'src', '.jc-chart--stale') === null, figure: all(r, 'src', 'figure').length,
			log: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a34. only a RETAINED published zoom:/legend: topic is a framework state topic: no datazoom/legend wiring, no restore from the
	// dashboard, and no implicit claim (so the shell paints no re-declaration error on the card).
	await section('retainFalse', async () => {
		const restores = [];
		const r = await page({ cards: [card({ id: 'nr', publishes: [{ topic: 'zoom:nr', retain: false }, { topic: 'legend:nr', retain: false }],
				option: { dataZoom: [{ type: 'inside' }], legend: {} } }),
			card({ id: 'yr', publishes: [{ topic: 'zoom:yr', retain: true }], option: { dataZoom: [{ type: 'inside' }] } })] }, {
			before: w => { w.JuneauViews = w.JuneauViews || {}; w.JuneauViews.dashboard = { initial: t => { restores.push(t); return { start: 5, end: 50, selected: { Placed: false } }; } }; } });
		const seen = [], legends = [];
		r.bus.subscribe('zoom:nr', v => seen.push(v), { retained: false });
		r.bus.subscribe('legend:nr', v => legends.push(v), { retained: false });
		const i = inst(r, 'nr');
		i.getOption = () => ({ dataZoom: [{ start: 20, end: 80 }] });
		i.fire('datazoom', {});
		i.fire('legendselectchanged', { selected: { Placed: true } });
		await wait(250);
		const first = lastOption(i);
		return { restores, seen: seen.length, legends: legends.length,
			foldedZoom: first.dataZoom[0], foldedLegend: first.legend?.selected || null,
			control: { drawn: inst(r, 'yr') !== null, error: cardError(r, 'yr') }, errors: r.errors.filter(e => e.includes('juneau-charts')) };
	});

	// a35. Superseded draws: a result that lands after a newer draw is dropped, and the older draw's fetches are aborted.
	// The held fetches ignore the abort and resolve late, so only the seq / stale checks keep an old result off the screen.  (The shell
	// serializes refreshes, so a cycle card's re-theme draw is superseded by a refresh, and a source card by a newer or an error handle.)
	await section('superseded', async () => {
		// 1. the decorator fetch of a re-theme draw is held; a refresh supersedes it; the late decorator result must not paint, and the
		// re-theme the superseded draw owed runs after the refresh.
		let n = 0, decs = [], release = null;
		const r1 = await page({ cards: [card({ id: 's1', data: undefined, src: '/api/rows', refreshMs: 5000,
			decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })] }, {
			fetch: (url, init) => {
				if (url === '/api/rows') return ok([{ day: 'Mon', placed: ++n, delivered: 0 }]);
				decs.push(init.signal);
				if (decs.length === 2) return new Promise(res => { release = () => res(heldJson([{ day: 'Mon', label: 'old' }])); });
				return ok([{ day: 'Mon', label: 'ok' }]);
			}
		});
		const i1 = inst(r1, 's1');
		r1.doc.dispatchEvent({ type: 'juneau:theme-changed' });
		await flush();
		const held = { placed: col(i1, 'placed'), aborted: decs[1].aborted, themes: i1.themes.length };
		tick(r1);
		await flush();
		const painted = { placed: col(i1, 'placed'), olderAborted: decs[1].aborted, themes: i1.themes.length,
			setOptions: r1.fake.calls.filter(c => c[0] === 'setOption').length };
		release();
		await flush();
		const late = { placed: col(i1, 'placed'), setOptions: r1.fake.calls.filter(c => c[0] === 'setOption').length,
			sameInstance: inst(r1, 's1') === i1, stale: one(r1, 's1', '.jc-chart--stale') !== null, error: cardError(r1, 's1'), errors: r1.errors };
		// 3. a source card: a decorator fetch of an older handle is held and REJECTS late, after a newer handle painted.
		let sk = 0, srcRelease = null, srcSignals = [];
		const r3 = await srcPage([srcCard({ id: 's3', decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })], {
			fetch: (url, init) => {
				srcSignals.push(init.signal);
				if (++sk === 2) return new Promise(res => { srcRelease = () => res({ ok: false, status: 500, json: () => Promise.resolve({}) }); }); // NOSONAR javascript:S2004 - test nesting mirrors describe/it structure
				return ok([{ day: 'd0', label: 'm' }]);
			}
		});
		await r3.publish(rowsOf(1));
		await r3.publish(rowsOf(2));
		await r3.publish(rowsOf(3));
		const srcBefore = { placed: col(inst(r3, 's3'), 'placed'), olderAborted: srcSignals[1].aborted };
		srcRelease();
		await flush();
		const srcLate = { placed: col(inst(r3, 's3'), 'placed'), stale: one(r3, 's3', '.jc-chart--stale') !== null, error: cardError(r3, 's3'),
			log: r3.errors.filter(e => e.includes('juneau-charts')) };
		// 4. an error handle supersedes a draw in flight (and aborts its fetches); the late result neither repaints nor clears the stale mark.
		let ek = 0, errRelease = null, errSignals = [];
		const r4 = await srcPage([srcCard({ id: 's4', decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })], {
			fetch: (url, init) => {
				errSignals.push(init.signal);
				if (++ek === 2) return new Promise(res => { errRelease = () => res(heldJson([{ day: 'd0', label: 'm' }])); });
				return ok([{ day: 'd0', label: 'm' }]);
			}
		});
		await r4.publish(rowsOf(1));
		await r4.publish(rowsOf(2));
		await r4.publishError('upstream down');
		const errAborted = errSignals[1].aborted;
		errRelease();
		await flush();
		const errLate = { placed: col(inst(r4, 's4'), 'placed'), stale: one(r4, 's4', '.jc-chart--stale') !== null, aborted: errAborted };
		return { cycle: { held, painted, late }, source: { before: srcBefore, late: srcLate }, errorHandle: errLate };
	});

	// a36. jsonCopy is bounded by nodes (scalars included), not only by depth: a meta that references itself under six keys, and a
	// million-element value array, both come back small and fast.
	await section('metaBudget', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.picked', retain: false }], cards: [card({ id: 'mb', on: { click: 'app.picked' }, publishes: [{ topic: 'app.picked', retain: false }] })] });
		r.bus.subscribe('app.picked', p => got.push(p));
		const meta = {};
		for (const k of ['a', 'b', 'c', 'd', 'e', 'f']) meta[k] = meta;
		const t0 = Date.now();
		inst(r, 'mb').fire('click', { componentType: 'series', seriesId: 'placed', dataIndex: 0, name: 'Mon', data: { meta }, value: Array.from({ length: 1000000 }, (_, i) => i), event: { event: {} } });
		const ms = Date.now() - t0;
		const p = got[0];
		return { published: got.length, fast: ms < 1500, metaNull: p.meta === null, valueLen: Array.isArray(p.value) ? p.value.length : null,
			valueBounded: Array.isArray(p.value) && p.value.length <= 5000 && p.value.length > 4000 };
	});

	// a37. Clicks on a decorator carrier series have no row; Enter without a prior arrow walk publishes nothing; the href lookup has no prototype.
	await section('carrierClick', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.picked', retain: false }], cards: [
			card({ id: 'cc', on: { click: 'app.picked' }, publishes: [{ topic: 'app.picked', retain: false }], onClick: { href: '/o?c={constructor}&d={day}' },
				decorators: [{ use: 'markers', src: '/api/m', at: 'day', label: 'label' }] })] }, { fetch: () => ok([{ day: 'Tue', label: 'Deploy' }]) });
		r.bus.subscribe('app.picked', p => got.push(structuredClone(p)));
		const i = inst(r, 'cc');
		i.fire('click', { componentType: 'series', seriesId: 'juneau-markers-0', dataIndex: 0, name: 'Tue', event: { event: {} } });
		const afterCarrier = { rows: got.map(g => g.row), seriesIds: got.map(g => g.seriesId), navigated: r.navigated.slice() };
		key(r, 'cc', 'Enter');
		const afterEnter = { published: got.length, navigated: r.navigated.length };
		key(r, 'cc', 'ArrowRight');
		key(r, 'cc', 'Enter');
		return { afterCarrier, afterEnter, afterWalk: { published: got.length, row: got.at(-1)?.row, dataIndex: got.at(-1)?.dataIndex,
			navigated: r.navigated } };
	});

	// a38. the export pixel ratio is clamped to 1..4 and a throwing getDataURL rejects (never throws out of the card API).
	await section('exportClamp', async () => {
		const r = await page({ cards: [card({ id: 'ex' }), card({ id: 'ey', renderer: 'svg' })] }, {
			afterLoad: w => { const init = w.echarts.init; w.echarts.init = (...a) => { const i = init(...a); const g = i.getDataURL; i.getDataURL = o => { if (i.throwNext) { throw new Error('canvas-boom'); } return g(o); }; return i; }; }
		});
		for (const pr of [1e6, 0.1, 2.5, 3])
			r.bus.publish('cmd:ex', { schemaVersion: 1, op: 'export', options: { type: 'png', pixelRatio: pr } });
		r.bus.publish('cmd:ex', { schemaVersion: 1, op: 'export', options: { type: 'png', pixelRatio: -5 } });
		await flush();
		const ratios = r.fake.calls.filter(c => c[0] === 'getDataURL').map(c => c[1].pixelRatio);
		inst(r, 'ex').throwNext = true;
		const api = r.w.JuneauConsole.cardApi('ex');
		let threw = null, p = null, rejected = null;
		try { p = api.export('png'); } catch (e) { threw = e.message; }
		try { await p; } catch (e) { rejected = e.message; }
		r.bus.publish('cmd:ex', { schemaVersion: 1, op: 'export', options: { type: 'png' } });
		await flush();
		return { ratios, threw, isPromise: !!p && typeof p.then === 'function', rejected, log: r.errors.filter(e => e.includes('E-JS-103')) };
	});

	// a39. a refreshMs above the timer maximum is the maximum (no log); the data table is built on open and at redraw while open;
	// fixed toggle label with aria-expanded, Home/End/Escape/blur on the keyboard walk.
	await section('refreshMax', async () => {
		const r = await page({ cards: [card({ id: 'big', data: undefined, src: '/api/b', refreshMs: 1e12 })] }, { fetch: () => ok(ROWS) });
		return { ms: r.intervals.map(x => x.ms), log: r.errors };
	});

	await section('lazyTable', async () => {
		let n = 0;
		const r = await page({ cards: [card({ id: 'lt', data: undefined, src: '/api/lt', refreshMs: 5000 })] }, { fetch: () => ok(Array.from({ length: ++n + 1 }, (_, i) => ({ day: 'd' + i, placed: n, delivered: 0 }))) });
		const cells = () => all(r, 'lt', 'td').length;
		const closed = { drawn: cells() };
		tick(r);
		await flush();
		const closedAfterRefresh = cells();
		openTable(r, 'lt');
		const opened = cells();
		tick(r);
		await flush();
		const openedAfterRefresh = cells();
		openTable(r, 'lt');
		tick(r);
		await flush();
		const closedAgain = { expanded: one(r, 'lt', '.jc-chart-toggle').getAttribute('aria-expanded'), text: one(r, 'lt', '.jc-chart-toggle').textContent, hidden: one(r, 'lt', '.jc-chart-table-box').hidden };
		openTable(r, 'lt');
		return { closed, closedAfterRefresh, opened, openedAfterRefresh, closedAgain, reopened: cells() };
	});

	await section('keys', async () => {
		const r = await page({ cards: [LINE] });
		const run = ks => { const from = r.fake.calls.length; for (const k of ks) { key(r, 'c1', k); } return actions(r, from).map(a => a.type + ':' + (a.dataIndex === undefined ? '' : a.dataIndex)); };
		const walk = run(['ArrowRight', 'ArrowRight']);
		const end = run(['End']);
		const home = run(['Home']);
		const escape = run(['Escape']);
		const escapeAgain = run(['Escape']);
		const afterEscape = run(['ArrowLeft']);
		const from = r.fake.calls.length;
		one(r, 'c1', '.jc-chart-plot').dispatch('blur', {});
		const blur = actions(r, from).map(a => a.type + ':' + (a.dataIndex === undefined ? '' : a.dataIndex));
		const blurAgain = (() => { const f = r.fake.calls.length; one(r, 'c1', '.jc-chart-plot').dispatch('blur', {}); return actions(r, f).length; })();
		return { walk, end, home, escape, escapeAgain, afterEscape, blur, blurAgain };
	});

	// a43. A params-driven re-render destroys the card without aborting ctx.signal: the old in-flight fetch must not draw a second chart.
	await section('paramRerender', async () => {
		const pend = [];
		const r = await page({
			topics: [{ topic: 'app.k', retain: true, publisher: 'script' }],
			cards: [card({ id: 'pc', data: undefined, src: '/api/g?k={k}', subscribes: [{ topic: 'app.k', as: 'params', map: { k: 'k' } }] })]
		}, { afterLoad: w => w.JuneauViews.bus.declare('app.k', { retain: true, by: 'test' }), fetch: () => new Promise(res => pend.push(res)) });
		const own = r.bus.owner('test');
		own.publish('app.k', { k: 1 }); await flush(); await wait(20);
		own.publish('app.k', { k: 2 }); await flush(); await wait(20);
		const mid = { fetches: r.fetches.length, pending: pend.length, created: r.fake.instances.length };
		pend.forEach(res => res(ok(ROWS))); await flush(); await wait(30);
		return { mid, created: r.fake.instances.length, live: r.fake.instances.filter(i => !i.disposed).length, errors: r.errors, err: cardError(r, 'pc') };
	});

	// a44. The chart type's implicit() claim is pinned on the glue itself (the Java parity test only covers ChartCardType): only retained publishes.
	await section('implicitPin', async () => {
		let handler = null;
		await page({ cards: [LINE] }, { before: w => {
			// The console replaces JuneauConsoleCards with its own register-on-push object: wrap it to see the chart handler the glue registers.
			let v;
			Object.defineProperty(w, 'JuneauConsoleCards', { configurable: true, get: () => v, set: x => {
				v = x && !Array.isArray(x) && typeof x.push === 'function' ? { push: e => { if (e?.[0] === 'chart') { handler = e[1]; } return x.push(e); } } : x; } });
		} });
		const impl = handler.implicit;
		const pub = (zoom, legend) => ({ id: 'q', publishes: [].concat(zoom === undefined ? [] : [{ topic: 'zoom:q', retain: zoom }], legend === undefined ? [] : [{ topic: 'legend:q', retain: legend }]) });
		return { none: impl(pub()), zoom: impl(pub(true)), both: impl(pub(true, true)), mixed: impl(pub(false, true)), off: impl(pub(false, false)) };
	});

	// a45. The glue's live set (TEST-ONLY __liveCount) empties when the shell destroys every card, and a params re-render keeps exactly one.
	await section('liveCount', async () => {
		const r = await page({ cards: [card({ id: 'l1' }), card({ id: 'l2' }), card({ id: 'l3' })] });
		const count = () => r.w.JuneauCharts.__liveCount();
		const before = count();
		for (const id of ['l1', 'l2', 'l3']) r.w.JuneauConsole.destroyCard(id);
		await flush();
		const after = count();
		const r2 = await page({
			topics: [{ topic: 'app.k', retain: true, publisher: 'script' }],
			cards: [card({ id: 'lp', data: undefined, src: '/api/g?k={k}', subscribes: [{ topic: 'app.k', as: 'params', map: { k: 'k' } }] })]
		}, { afterLoad: w => w.JuneauViews.bus.declare('app.k', { retain: true, by: 'test' }), fetch: () => new Promise(() => {}) });
		const own = r2.bus.owner('test');
		own.publish('app.k', { k: 1 }); await flush(); await wait(20);
		own.publish('app.k', { k: 2 }); await flush(); await wait(20);
		const rerender = r2.w.JuneauCharts.__liveCount();
		r2.w.JuneauConsole.destroyCard('lp');
		await flush();
		return { before, after, rerender, rerenderAfter: r2.w.JuneauCharts.__liveCount() };
	});

	// a46. A card src that never settles is aborted after the bounded wait: the card ends in an error state (not a stuck loading
	// state) and the next refresh runs.  The test shortens the 10 s bound through the sandbox timer.
	await section('srcTimeout', async () => {
		let hang = true, n = 0;
		const signals = [];
		const quick = (fn, ms) => setTimeout(fn, ms >= 10000 ? 20 : ms);
		const r = await page({ cards: [card({ id: 'to', data: undefined, src: '/api/to', refreshMs: 5000 })] }, {
			setTimeout: quick,
			fetch: (url, init) => { signals.push(init.signal); n++; return hang ? new Promise(() => {}) : ok([{ day: 'Mon', placed: 7, delivered: 0 }]); }
		});
		await wait(120);
		await flush();
		const afterHang = { error: cardError(r, 'to'), aborted: signals[0].aborted, instances: r.fake.instances.length };
		hang = false;
		tick(r);
		await flush();
		const i = inst(r, 'to');
		return { afterHang, fetches: n, recovered: !!i, placed: i ? col(i, 'placed') : null, errorAfter: cardError(r, 'to') };
	});

	// a47. A declared source: the shell boots it before wiring cards, the chart draws its frame, a refresh on the card
	// reloads the source first, and a page whose juneau-data.js has no boot is E-JS-106.
	await section('dataSource', async () => {
		const SRC = { id: 'orders', kind: 'source', url: '/rest/orders', parser: 'csv',
			columns: [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }], subscribes: [],
			concurrency: 1, debounceMs: 0, cache: 'none', mode: 'raw', builder: 'columnar' };
		const CARD = { id: 'oc', type: 'chart', title: 'O', source: 'data:orders',
			subscribes: [{ topic: 'data:orders', as: 'data' }],
			helper: { kind: 'line', x: 'day', series: [{ field: 'placed' }] } };
		const csv = () => Promise.resolve({ ok: true, status: 200, headers: { get: () => null },
			text: () => Promise.resolve('day,placed\nMon,3\nTue,5\nWed,4\n') });
		const r = await page({ dataSources: [SRC], cards: [CARD] }, { fetch: csv });
		await wait(20);
		await flush();
		// col throws while the card has no chart; count that as no points, so the red run fails on points, not a crash.
		const points = (() => { try { return col(inst(r, 'oc'), 'placed').length; } catch (e) { return 0; } })(); // NOSONAR javascript:S2486 - the failure is an expected outcome here and the fallback handles it
		const cmds = [];
		r.bus.subscribe('cmd:orders', p => cmds.push(p), { retained: false });
		r.bus.publish('cmd:oc', { schemaVersion: 1, op: 'refresh', options: { clearCache: true } });
		await wait(20);
		await flush();
		const first = cmds.slice();
		r.bus.publish('cmd:oc', { schemaVersion: 1, op: 'refresh' });
		await wait(20);
		await flush();
		const h = r.bus.get('data:orders');
		const missing = await page({ dataSources: [SRC], cards: [] },
			{ afterLoad: w => { w.JuneauViews = { ...w.JuneauViews, data: undefined }; } });
		return { points, cmds: first, plainCmds: cmds.slice(first.length), fetches: r.fetches.map(f => f.url),
			handle: { status: h.status, version: h.version }, errors: r.errors, missing: missing.errors };
	});

	// a48-a55. Treemap: the tree and its option, the missing-group-field error, refresh, the leaf and group clicks, the keyboard.
	const TM_ROWS = [{ region: 'eu', cluster: 'c1', host: 'h1', health: 'ok', cpu: 3 }, { region: 'eu', cluster: 'c1', host: 'h2', health: 'down', cpu: 9 },
		{ region: 'us', cluster: 'c2', host: 'h3', health: 'warn', cpu: 1 }];
	const TM_HELPER = { kind: 'treemap', groupBy: ['region', 'cluster'], label: 'host',
		color: { field: 'health', legend: [{ value: 'ok', label: 'Healthy', tone: 'ok' }, { value: 'down', label: 'Down', tone: 'danger' }] },
		tileFields: ['host', 'health', 'cpu'] };
	const tm = o => ({ id: 'tm', type: 'chart', title: 'Fleet', helper: TM_HELPER, data: TM_ROWS, ...o });
	const tmTree = (r, id) => lastOption(inst(r, id)).series[0].data;
	const tmLeaves = nodes => nodes.flatMap(n => n.children ? tmLeaves(n.children) : [n]);

	await section('treemapOption', async () => {
		const r = await page({ cards: [tm()] });
		const o = lastOption(inst(r, 'tm')), s0 = o.series[0], top = s0.data[0];
		const leaf = id => tmLeaves(s0.data).find(n => n.id === id);
		const api = r.w.JuneauConsole.cardApi('tm').getOption();
		return {
			type: s0.type, roam: s0.roam, nodeClick: s0.nodeClick, breadcrumb: s0.breadcrumb, upperLabel: s0.upperLabel, bottom: s0.bottom,
			levels: s0.levels.length, level0: s0.levels[0], level1: s0.levels[1], level2: s0.levels[2], roots: s0.data.length, rootId: top.id,
			groupIds: top.children.map(n => n.id), clusterIds: top.children[0].children.map(n => n.id),
			leafIds: tmLeaves(s0.data).map(n => n.id), okColor: leaf('eu/c1/h1').itemStyle, downColor: leaf('eu/c1/h2').itemStyle,
			unmatched: leaf('us/c2/h3').itemStyle === undefined,
			formatter: api.tooltip.formatter, renderMode: o.tooltip.renderMode, keyText: o.graphic[0].style.text, errors: r.errors
		};
	});

	await section('treemapTree', async () => {
		const rows = [{ g: 'a/b', h: 'x', w: 5 }, { g: 'z', h: 'x', w: -2 }, { g: 'a/b', h: 'x', w: 3 }, { g: 'a/b', h: 'y' }];
		const mk = (id, helper) => ({ id: id, type: 'chart', helper: { kind: 'treemap', groupBy: ['g'], label: 'h', ...helper }, data: rows });
		const r = await page({ cards: [mk('w', { size: 'w', tileFields: ['h', 'w'] }), mk('u', {}), tm({ id: 'cpu', helper: { ...TM_HELPER, size: 'cpu' } })] });
		const t = tmTree(r, 'w')[0].children, leaves = tmLeaves(t);
		return {
			groupOrder: t.map(n => n.id), groupNames: t.map(n => n.name), leafIds: leaves.map(n => n.id), weights: leaves.map(n => n.value),
			meta: leaves.map(n => n.meta), unsizedWeights: tmLeaves(tmTree(r, 'u')).map(n => n.value), unsizedMeta: tmLeaves(tmTree(r, 'u')).map(n => n.meta),
			cpuWeights: tmLeaves(tmTree(r, 'cpu')).map(n => n.value), errors: r.errors
		};
	});

	await section('treemapEdge', async () => {
		const allIds = nodes => nodes.flatMap(n => [n.id].concat(n.children ? allIds(n.children) : []));
		const rows = [{ region: null, cluster: 'eu', host: 'a', w: 1 }, { region: 'eu', cluster: 'c1', host: null, w: 2 },
			{ region: 'eu', cluster: 'c1', host: 'b', w: 3 }, { region: 'jc-root', cluster: 'q', host: 'c', w: 4 }];
		const sized = [{ g: 'a', h: 5 }, { g: 'b', h: 7 }];
		const late = [];
		for (let i = 0; i < 105; i++) late.push({ g: 'a', h: 'r' + i });
		late.push({ g: 'a', h: 'z', extra: 'late' });
		const r = await page({ cards: [
			{ id: 'e1', type: 'chart', helper: { kind: 'treemap', groupBy: ['region', 'cluster'], label: 'host', size: 'w' }, data: rows },
			{ id: 'e2', type: 'chart', helper: { kind: 'treemap', groupBy: ['g'], label: 'h', size: 'h' }, data: sized },
			{ id: 'e3', type: 'chart', helper: { kind: 'treemap', groupBy: ['g'], label: 'h', tileFields: ['h', 'extra'] }, data: late }] });
		const top = tmTree(r, 'e1')[0], ids = allIds(tmTree(r, 'e1')), l3 = tmLeaves(tmTree(r, 'e3'));
		return {
			groupIds: top.children.map(n => n.id), groupNames: top.children.map(n => n.name), nested: top.children[0].children.map(n => n.id),
			leafIds: tmLeaves(tmTree(r, 'e1')).map(n => n.id), unique: new Set(ids).size === ids.length, noneEmpty: ids.every(i => i !== ''),
			sizedWeights: tmLeaves(tmTree(r, 'e2')).map(n => n.value), firstExtra: l3[0].meta.extra, lastExtra: l3.at(-1).meta.extra,
			errors: r.errors
		};
	});

	await section('treemapMissingField', async () => {
		const r = await page({ cards: [tm({ id: 'bad', data: [{ region: 'eu', host: 'h1' }] })] });
		return { error: cardError(r, 'bad'), drawn: inst(r, 'bad') !== null, log: r.errors.filter(e => e.includes('E-JS-63')) };
	});

	await section('treemapRefresh', async () => {
		let n = 0;
		const r = await page({ cards: [tm({ id: 'tr', data: undefined, src: '/api/tm', refreshMs: 5000 })] },
			{ fetch: () => ok(n++ === 0 ? TM_ROWS : TM_ROWS.slice(0, 2).concat([{ region: 'ap', cluster: 'c9', host: 'h9', health: 'ok', cpu: 2 }])) });
		const before = tmLeaves(tmTree(r, 'tr')).map(l => l.id);
		tick(r);
		await flush();
		const after = tmLeaves(tmTree(r, 'tr')).map(l => l.id);
		return { before, after, fetches: n, errors: r.errors };
	});

	const TM_LEAF = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', seriesName: 'tiles', dataIndex: 2, name: 'h1', value: 1,
		data: { id: 'eu/c1/h1', meta: { host: 'h1', health: 'ok', cpu: 3 } }, event: { event: {} } };
	const TM_CLICK = { on: { click: 'app.tm' }, publishes: [{ topic: 'app.tm', retain: false }] };

	await section('treemapLeafClick', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.tm', retain: false }], cards: [tm(TM_CLICK), card({ id: 'line', ...TM_CLICK })] });
		r.bus.subscribe('app.tm', p => got.push(structuredClone(p)));
		inst(r, 'tm').fire('click', TM_LEAF);
		inst(r, 'line').fire('click', { componentType: 'series', seriesId: 'placed', dataIndex: 0, name: 'Mon', value: 3, event: { event: {} } });
		return { got, errors: r.errors };
	});

	await section('treemapGroupClick', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.tm', retain: false }], cards: [tm({ onClick: { href: '/x/{host}' }, ...TM_CLICK })] });
		r.bus.subscribe('app.tm', p => got.push(structuredClone(p)));
		const group = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', dataIndex: 1, name: 'eu', value: 12,
			data: { id: 'eu', children: [{ id: 'eu/c1' }] }, event: { event: {} } };
		inst(r, 'tm').fire('click', group);
		const afterGroup = { published: got.length, navigated: r.navigated.slice(), e79: r.errors.filter(e => e.includes('E-JS-79')) };
		inst(r, 'tm').fire('click', TM_LEAF);
		return { afterGroup, afterLeaf: { published: got.length, navigated: r.navigated.slice() }, errors: r.errors };
	});

	await section('treemapMeta', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.tm', retain: false }], cards: [tm(TM_CLICK)] });
		r.bus.subscribe('app.tm', p => got.push(structuredClone(p)));
		inst(r, 'tm').fire('click', { ...TM_LEAF, data: { id: 'eu/c1/h1', meta: { big: 'x'.repeat(5000) } } });
		return { published: got.length, metaNull: got[0].meta === null, rowNull: got[0].row === null };
	});

	await section('treemapKeys', async () => {
		const got = [];
		const r = await page({ topics: [{ topic: 'app.tm', retain: false }], cards: [tm(TM_CLICK)] });
		r.bus.subscribe('app.tm', p => got.push(structuredClone(p)));
		const run = ks => { const from = r.fake.calls.length; for (const k of ks) { key(r, 'tm', k); } return actions(r, from).map(a => a.type + ':' + (a.dataIndex === undefined ? a.targetNodeId + '@' + a.seriesIndex : a.dataIndex)); };
		const walk = run(['ArrowRight', 'ArrowRight']);
		const enter = run(['Enter']);
		const end = run(['End']);
		const home = run(['Home']);
		const escape = run(['Escape']);
		const zoomUnfocused = run(['+']);
		const focus = run(['ArrowRight']);
		const zoomIn = run(['+']);
		const out1 = run(['-']);
		const out2 = run(['-']);
		const outAtTop = run(['-']);
		const reset = run(['0']);
		const afterReset = run(['-']);
		return { walk, enter, end, home, escape, zoomUnfocused, focus, zoomIn, out1, out2, outAtTop, reset, afterReset, published: got, errors: r.errors };
	});

	// a56-a69. Card actions: the views hook is stubbed; the card subscribes to its click topic and runs the action once per click.
	const ACT = { id: 'ack', label: 'Ack', endpoint: '/x/{name}', method: 'POST' };
	const CATALOG = [{ id: 'edit', label: 'Edit', endpoint: '/x/e', method: 'PUT', present: 'dialog' }];
	const ACT_CLICK = { on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }], onClick: { action: ACT }, actions: CATALOG };
	const PT = { componentType: 'series', seriesId: 'placed', seriesName: 'Placed', dataIndex: 0, name: 'Mon', value: 3, event: { event: {} } };
	// A hook stub: records every run and release; each run's promise resolves only when the test says so.
	function stubHook(sync) {
		const h = { runs: [], releases: [], pending: [] };
		h.api = {
			run: (a, o) => { h.runs.push({ a, o }); if (sync) { throw new Error('boom'); } return new Promise((res, rej) => h.pending.push({ res, rej })); },
			release: el => { h.releases.push(el); }
		};
		return h;
	}
	const withHook = h => ({ before: w => { w.JuneauViews = w.JuneauViews || {}; w.JuneauViews.actions = h.api; } });
	// Captures the chart card handler and each card's ctx as the shell registers them, so a test can hand refresh() a changed card.
	function captureHandler() {
		const cap = { ctxs: {}, handler: null };
		cap.before = w => {
			const arr = [];
			const orig = arr.push;
			arr.push = function (e) {
				const h = e[1];
				cap.handler = h;
				return orig.call(this, [e[0], { ...h, render: (c, el, ctx) => { cap.ctxs[c.id] = ctx; return h.render(c, el, ctx); } }]);
			};
			w.JuneauConsoleCards = arr;
		};
		return cap;
	}
	const actPage = async (cards, h, extra) => page({ topics: [{ topic: 'app.act', retain: false }], cards: cards }, Object.assign(withHook(h), extra || {}));

	await section('actClick', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'ac', ...ACT_CLICK })], h);
		inst(r, 'ac').fire('click', { ...PT, data: { meta: { name: 'MetaName', extra: 1 } } });
		await flush();
		const run = h.runs[0];
		return { runs: h.runs.length, action: run?.a.id, host: run?.o.host === host(r, 'ac'), row: run?.o.row,
			catalog: run?.o.catalog === r.w.JuneauConsole, catalogIds: run && (run.o.catalog || []).map(c => c.id), hasRedraw: run && typeof run.o.onRedraw === 'function', errors: r.errors };
	});

	await section('actRedraw', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'ar', ...ACT_CLICK })], h);
		const i = inst(r, 'ar');
		i.fire('click', PT);
		await flush();
		const before = i.options.length;
		h.runs[0].o.onRedraw();
		await flush();
		return { redrew: inst(r, 'ar').options.length > before, releases: h.releases.length, errors: r.errors };
	});

	await section('actEnter', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'ae', ...ACT_CLICK })], h);
		key(r, 'ae', 'ArrowRight');
		key(r, 'ae', 'Enter');
		await flush();
		return { runs: h.runs.length, row: h.runs[0]?.o.row, errors: r.errors };
	});

	await section('actInFlight', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'af', ...ACT_CLICK })], h);
		const i = inst(r, 'af'), el = host(r, 'af');
		i.fire('click', PT);
		await flush();
		const busyOn = { aria: one(r, 'af', '.jc-chart-plot').getAttribute('aria-busy'), cardAria: el.getAttribute('aria-busy'), flag: el.dataset.juneauActionBusy !== undefined, cursor: one(r, 'af', '.jc-chart-plot').style.cursor };
		i.fire('click', PT);
		await flush();
		const whileBusy = h.runs.length;
		h.pending[0].res({ outcome: 'success' });
		await flush(); await flush();
		const busyOff = { aria: one(r, 'af', '.jc-chart-plot').getAttribute('aria-busy'), cardAria: el.getAttribute('aria-busy'), flag: el.dataset.juneauActionBusy !== undefined, cursor: one(r, 'af', '.jc-chart-plot').style.cursor };
		i.fire('click', PT);
		await flush();
		return { busyOn, whileBusy, busyOff, afterSettle: h.runs.length, errors: r.errors };
	});

	await section('actRejectReleases', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'arj', ...ACT_CLICK })], h);
		const i = inst(r, 'arj');
		i.fire('click', PT);
		await flush();
		h.pending[0].rej(new Error('x'));
		await flush(); await flush();
		i.fire('click', PT);
		await flush();
		return { runs: h.runs.length, busy: one(r, 'arj', '.jc-chart-plot').getAttribute('aria-busy'), errors: r.errors };
	});

	await section('actSyncThrow', async () => {
		const h = stubHook(true);
		const r = await actPage([card({ id: 'as', ...ACT_CLICK })], h);
		const el = host(r, 'as');
		inst(r, 'as').fire('click', PT);
		await flush();
		const first = h.runs.length;
		inst(r, 'as').fire('click', PT);
		await flush();
		return { first, second: h.runs.length, busy: one(r, 'as', '.jc-chart-plot').getAttribute('aria-busy'), flag: el.dataset.juneauActionBusy !== undefined, errors: r.errors };
	});

	await section('actNoHook', async () => {
		const r = await page({ topics: [{ topic: 'app.act', retain: false }], cards: [card({ id: 'nh', ...ACT_CLICK }), card({ id: 'nh2', ...ACT_CLICK })] });
		inst(r, 'nh').fire('click', PT);
		inst(r, 'nh').fire('click', PT);
		inst(r, 'nh2').fire('click', PT);
		await flush();
		return { errors: r.errors.filter(e => e.includes('E-JS-108')), busy: one(r, 'nh', '.jc-chart-plot').getAttribute('aria-busy') };
	});

	await section('actDispose', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'ad', ...ACT_CLICK })], h);
		const i = inst(r, 'ad'), el = host(r, 'ad');
		r.w.JuneauConsole.destroyCard('ad');
		await flush();
		r.bus.publish('app.act', { schemaVersion: 1, card: 'other', event: 'click', name: 'z' });
		await flush();
		i.fire('click', PT);
		await flush();
		return { runs: h.runs.length, releases: h.releases.length, releasedHost: h.releases[0] === el, errors: r.errors };
	});

	await section('actNone', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'an', on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }] }),
			card({ id: 'an2', on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }], actions: CATALOG })], h);
		inst(r, 'an').fire('click', PT);
		r.bus.publish('app.act', { schemaVersion: 1, event: 'click', name: 'z' });
		await flush();
		return { runs: h.runs.length, errors: r.errors };
	});

	await section('actCatalogIdentity', async () => {
		const h = stubHook();
		const c = card({ id: 'ci', ...ACT_CLICK });
		const r = await actPage([c], h);
		inst(r, 'ci').fire('click', PT);
		await flush();
		const o = h.runs[0].o;
		return { sameLength: o.catalog.length === 1, id: o.catalog[0].id, method: o.catalog[0].method, errors: r.errors };
	});

	await section('actRerenderRelease', async () => {
		const h = stubHook();
		const r = await page({
			topics: [{ topic: 'app.k', retain: true, publisher: 'script' }, { topic: 'app.act', retain: false }],
			cards: [card({ id: 'rr', data: undefined, src: '/api/g?k={k}', subscribes: [{ topic: 'app.k', as: 'params', map: { k: 'k' } }], ...ACT_CLICK })]
		}, Object.assign(withHook(h), { afterLoad: w => w.JuneauViews.bus.declare('app.k', { retain: true, by: 'test' }), fetch: () => new Promise(() => {}) }));
		const own = r.bus.owner('test');
		own.publish('app.k', { k: 1 }); await flush(); await wait(20);
		const afterFirst = h.releases.length;
		own.publish('app.k', { k: 2 }); await flush(); await wait(20);
		const afterSecond = h.releases.length;
		return { afterFirst, afterSecond, allSameHost: h.releases.every(x => x === host(r, 'rr')), errors: r.errors };
	});

	await section('actInPlaceRefresh', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'ip', ...ACT_CLICK })], h);
		const el = host(r, 'ip');
		inst(r, 'ip').fire('click', PT);
		await flush();
		const banner = r.w.document.createElement('div');
		banner.dataset.testid = 'chart-action-banner';
		el.appendChild(banner);
		h.runs[0].o.onRedraw();
		await flush(); await wait(20);
		return { releases: h.releases.length, bannerKept: el.querySelector('[data-testid="chart-action-banner"]') === banner, errors: r.errors };
	});

	await section('actDestroyedRedraw', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'dr', ...ACT_CLICK })], h);
		inst(r, 'dr').fire('click', PT);
		await flush();
		const onRedraw = h.runs[0].o.onRedraw;
		r.w.JuneauConsole.destroyCard('dr');
		await flush(); await wait(20);
		const before = r.fake.calls.length;
		onRedraw();
		await flush(); await wait(20);
		return { delta: r.fake.calls.length - before, errors: r.errors };
	});

	await section('actTreemap', async () => {
		const h = stubHook();
		const r = await actPage([tm(ACT_CLICK)], h);
		inst(r, 'tm').fire('click', TM_LEAF);
		const group = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', dataIndex: 1, name: 'eu', value: 12,
			data: { id: 'eu', children: [{ id: 'eu/c1' }] }, event: { event: {} } };
		inst(r, 'tm').fire('click', group);
		await flush();
		h.pending.forEach(p => p.res({ outcome: 'success' }));
		await flush(); await flush();
		key(r, 'tm', 'ArrowRight');
		key(r, 'tm', 'Enter');
		await flush();
		return { runs: h.runs.length, row: h.runs[0]?.o.row, enterRow: h.runs[1]?.o.row, errors: r.errors };
	});

	await section('treemapGroupEvents', async () => {
		const h = stubHook();
		const got = [];
		const r = await page({ topics: [{ topic: 'app.tmd', retain: false }, { topic: 'app.act', retain: false }],
			cards: [tm({ on: { click: 'app.act', dblclick: 'app.tmd' }, publishes: [{ topic: 'app.act', retain: false }, { topic: 'app.tmd', retain: false }],
				topicActions: [{ topic: 'app.tmd', action: ACT }] })] }, withHook(h));
		r.bus.subscribe('app.tmd', p => got.push(structuredClone(p)));
		const group = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', dataIndex: 1, name: 'eu', value: 12,
			data: { id: 'eu', children: [{ id: 'eu/c1' }] }, event: { event: {} } };
		inst(r, 'tm').fire('dblclick', group);
		await flush();
		const afterGroup = { published: got.length, runs: h.runs.length };
		inst(r, 'tm').fire('dblclick', TM_LEAF);
		await flush();
		return { afterGroup, afterLeaf: { published: got.length, runs: h.runs.length, row: h.runs[0]?.o.row }, errors: r.errors };
	});

	await section('walkAnnounce', async () => {
		const h = stubHook();
		const r = await actPage([tm(ACT_CLICK), card({ id: 'cart', ...ACT_CLICK }), card({ id: 'plain' })], h);
		const live = id => { const n = one(r, id, '.jc-chart-live'); return n ? n.textContent : null; };
		const plot = id => one(r, id, '.jc-chart-plot');
		const hintOf = id => { const d = plot(id).getAttribute('aria-describedby'); const n = d && r.doc.getElementById(d); return n ? n.textContent : null; };
		const out = { tmRole: plot('tm').getAttribute('role'), tmHint: hintOf('tm'), cartRole: plot('cart').getAttribute('role'),
			plainRole: plot('plain').getAttribute('role'), plainHint: plot('plain').getAttribute('aria-describedby'), plainLive: live('plain') };
		key(r, 'tm', 'ArrowRight');
		out.tmFirst = live('tm');
		key(r, 'tm', 'ArrowRight');
		out.tmSecond = live('tm');
		key(r, 'tm', 'Escape');
		out.tmEscape = live('tm');
		key(r, 'cart', 'ArrowRight');
		out.cartFirst = live('cart');
		key(r, 'cart', 'End');
		out.cartLast = live('cart');
		out.errors = r.errors;
		return out;
	});

	await section('treemapGroupHint', async () => {
		const h = stubHook();
		const r = await actPage([tm(ACT_CLICK), tm({ id: 'tm2' })], h);
		const group = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', dataIndex: 1, name: 'eu', value: 12,
			data: { id: 'eu', children: [{ id: 'eu/c1' }] }, event: { event: {} } };
		const b = id => one(r, id, '.jc-chart-banner');
		inst(r, 'tm').fire('click', group);
		inst(r, 'tm2').fire('click', group);
		return { hint: b('tm').textContent, shown: !b('tm').hidden, plainShown: !b('tm2').hidden, runs: h.runs.length, errors: r.errors };
	});

	await section('refreshRebind', async () => {
		const h = stubHook(), cap = captureHandler();
		const ACT2 = { id: 'ack2', label: 'Ack2', endpoint: '/y/{name}', method: 'POST' };
		const base = { id: 'rb', on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }], actions: CATALOG,
			onClick: { action: ACT }, topicActions: [{ topic: 'app.mark', action: ACT }] };
		const r = await page({ topics: [{ topic: 'app.act', retain: false }, { topic: 'app.mark', retain: false }, { topic: 'app.mark2', retain: false }],
			cards: [card(base)] }, Object.assign(withHook(h), { chartsFirst: true, before: w => { withHook(h).before(w); cap.before(w); } }));
		const refresh = async next => { await cap.handler.refresh(card(next), host(r, 'rb'), cap.ctxs.rb); await flush(); };
		const ends = () => h.runs.map(x => x.a.endpoint);
		inst(r, 'rb').fire('click', PT);
		await flush();
		// Same topics, a new action: the new action runs.
		await refresh({ ...base, onClick: { action: ACT2 }, topicActions: [{ topic: 'app.mark', action: ACT2 }] });
		h.pending[0].res({ outcome: 'success' });
		await flush(); await flush();
		inst(r, 'rb').fire('click', PT);
		await flush();
		h.pending[1].res({ outcome: 'success' });
		await flush(); await flush();
		r.bus.publish('app.mark', { schemaVersion: 1, name: 'M' });
		await flush();
		const afterNewAction = ends();
		// A changed topic: the old topic runs nothing, the new one does.
		h.pending[2].res({ outcome: 'success' });
		await flush(); await flush();
		const before = h.runs.length;
		await refresh({ ...base, onClick: { action: ACT2 }, topicActions: [{ topic: 'app.mark2', action: ACT2 }] });
		r.bus.publish('app.mark', { schemaVersion: 1, name: 'M' });
		await flush();
		const oldTopic = h.runs.length - before;
		r.bus.publish('app.mark2', { schemaVersion: 1, name: 'M' });
		await flush();
		return { afterNewAction, oldTopic, newTopic: h.runs.length - before - oldTopic, errors: r.errors };
	});

	await section('refreshInteractivity', async () => {
		const h = stubHook(), cap = captureHandler();
		const plain = { id: 'ri' };
		const r = await page({ topics: [{ topic: 'app.act', retain: false }], cards: [card(plain)] },
			Object.assign(withHook(h), { chartsFirst: true, before: w => { withHook(h).before(w); cap.before(w); } }));
		const plot = () => one(r, 'ri', '.jc-chart-plot');
		const out = { first: plot().getAttribute('role') };
		await cap.handler.refresh(card({ id: 'ri', ...ACT_CLICK }), host(r, 'ri'), cap.ctxs.ri);
		await flush();
		out.added = { role: plot().getAttribute('role'), live: !!one(r, 'ri', '.jc-chart-live') };
		await cap.handler.refresh(card(plain), host(r, 'ri'), cap.ctxs.ri);
		await flush();
		out.removed = { role: plot().getAttribute('role'), live: !!one(r, 'ri', '.jc-chart-live') };
		out.errors = r.errors;
		return out;
	});

	await section('topicActionsOnly', async () => {
		const h = stubHook();
		const TA = { on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }], topicActions: [{ topic: 'app.act', action: ACT }] };
		const XF = { on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }] };
		const r = await actPage([tm(TA), card({ id: 'cart', ...TA }), tm({ id: 'xf', ...XF })], h);
		const plot = id => one(r, id, '.jc-chart-plot');
		const group = { componentType: 'series', seriesType: 'treemap', seriesId: 'tiles', dataIndex: 1, name: 'eu', value: 12,
			data: { id: 'eu', children: [{ id: 'eu/c1' }] }, event: { event: {} } };
		inst(r, 'tm').fire('click', group);
		inst(r, 'xf').fire('click', group);
		const live = id => { const n = one(r, id, '.jc-chart-live'); return n ? n.textContent : null; };
		key(r, 'cart', 'ArrowRight');
		return { tmRole: plot('tm').getAttribute('role'), cartRole: plot('cart').getAttribute('role'), xfRole: plot('xf').getAttribute('role'),
			cartLive: live('cart'), hint: one(r, 'tm', '.jc-chart-banner').textContent, shown: !one(r, 'tm', '.jc-chart-banner').hidden,
			xfShown: !one(r, 'xf', '.jc-chart-banner').hidden, errors: r.errors };
	});

	await section('walkTime', async () => {
		const h = stubHook();
		const t0 = Date.UTC(2026, 9, 13);
		const r = await actPage([card({ id: 'wt', x: undefined, kind: undefined, series: undefined,
			helper: { kind: 'line', x: { field: 'day', type: 'time' }, series: [{ field: 'placed' }, { field: 'delivered' }] },
			data: [{ day: t0, placed: 3, delivered: null }, { day: t0 + 86400000, placed: 4, delivered: 2 }], ...ACT_CLICK })], h);
		key(r, 'wt', 'ArrowRight');
		const first = one(r, 'wt', '.jc-chart-live').textContent;
		key(r, 'wt', 'ArrowRight');
		return { first, second: one(r, 'wt', '.jc-chart-live').textContent, errors: r.errors };
	});

	await section('actTopicActions', async () => {
		const h = stubHook();
		const r = await page({ topics: [{ topic: 'app.act', retain: false }, { topic: 'app.mark', retain: false }],
			cards: [card({ id: 'ta', on: { click: 'app.act' }, publishes: [{ topic: 'app.act', retain: false }],
				topicActions: [{ topic: 'app.mark', action: ACT }] })] }, withHook(h));
		inst(r, 'ta').fire('click', PT);
		await flush();
		const afterClick = h.runs.length;
		r.bus.publish('app.mark', { schemaVersion: 1, name: 'Marked', meta: { k: 'v' } });
		await flush();
		return { afterClick, runs: h.runs.length, row: h.runs[0]?.o.row, errors: r.errors };
	});

	await section('actDialogHeld', async () => {
		const h = stubHook();
		const r = await actPage([card({ id: 'dh', ...ACT_CLICK })], h);
		const i = inst(r, 'dh');
		i.fire('click', PT);
		await flush();
		i.fire('click', PT);
		i.fire('click', PT);
		await flush();
		const held = h.runs.length;
		h.pending[0].res({ outcome: 'cancelled' });
		await flush(); await flush();
		i.fire('click', PT);
		await flush();
		return { held, after: h.runs.length, errors: r.errors };
	});

	// A decorator-timeout timer for a draw that never settles would hold the process for its full 10 s; exit once the
	// output is flushed.
	process.stdout.write(JSON.stringify(out) + '\n', () => process.exit(0));
})();
