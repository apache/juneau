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
 * charts.cjs - dependency-free Node harness for juneau-charts.js: the line/bar/donut renderers, src and inline data,
 * refresh and stale, the refreshMs clamp, unknown kinds and late registration, the JuneauConsoleCharts queue, clicks,
 * keyboard, escaping, destroy, repaint focus and a renderer whose update throws.  Every assertion lives in ConsoleCharts_Test.
 *
 *   Usage:  node charts.cjs <juneau-console.js> <juneau-charts.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const [shellPath, chartsPath] = process.argv.slice(2);
if (!shellPath || !chartsPath) {
	console.error('usage: node charts.cjs <juneau-console.js> <juneau-charts.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');
const chartsSrc = fs.readFileSync(chartsPath, 'utf8');

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] };
const C = o => Object.assign({}, base, o);

function load(opts) {
	opts = opts || {};
	const navigated = [];
	const env = makeConsoleEnv({ location: { assign: u => navigated.push(String(u)) } });
	const doc = env.document;
	doc.readyState = 'loading';
	const clicks = [], failed = [];
	const create = doc.createElement;
	doc.createElement = function (tag) {
		const n = create(tag);
		n.dispatchEvent = function (ev) {
			if (ev && ev.type === 'juneau:chart-click') clicks.push(JSON.parse(JSON.stringify(ev.detail)));
			if (ev && ev.type === 'juneau:card-failed') failed.push(ev.detail.id);
			return true;
		};
		return n;
	};
	doc.createElementNS = function (ns, tag) { const n = doc.createElement(tag); n.namespaceURI = ns; return n; };
	const errors = [], intervals = [], fetches = [];
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		AbortController,
		localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
		fetch: (url, init) => {
			fetches.push({ url: String(url), accept: init && init.headers && init.headers.Accept });
			return (opts.fetch || (() => Promise.reject(new TypeError('no fetch'))))(url, init);
		},
		setTimeout: fn => { fn(); return 0; }, clearTimeout: () => {},
		setInterval: (fn, ms) => { intervals.push({ fn, ms, live: true }); return intervals.length; },
		clearInterval: id => { if (id > 0 && intervals[id - 1]) intervals[id - 1].live = false; }
	};
	env.window.window = env.window;
	env.window.document = doc;
	const ctx = vm.createContext(sandbox);
	if (opts.chartsFirst) vm.runInContext(chartsSrc, ctx, { filename: 'juneau-charts.js' });
	vm.runInContext(shellSrc, ctx, { filename: 'juneau-console.js' });
	if (opts.before) opts.before(sandbox.window);
	if (!opts.chartsFirst) vm.runInContext(chartsSrc, ctx, { filename: 'juneau-charts.js' });
	const w = sandbox.window;
	return {
		env, doc, errors, intervals, fetches, clicks, failed, navigated, w,
		mount: cards => w.JuneauConsole.mount(C({ cards }), { root: doc.body, document: doc })
	};
}

const ok = body => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body) });
const bad = (status, body) => Promise.resolve({ ok: false, status, json: () => Promise.resolve(body) });
// The shim's querySelector understands ONE simple selector (tag, .class, [attr]); it has no #id and no descendant
// combinator (views-dom-shim.cjs parseSimple).  So every query is scoped to a card host found by id.
// CAUTION: never write '#id .cls' or any descendant/compound selector here - the shim silently matches the wrong
// node or nothing.  Always go through host()/all()/one(); a bare tag such as one(r, id, 'svg') is safe only while the
// card has exactly one element of that tag.
const host = (r, id) => r.doc.getElementById(id);
const all = (r, id, sel) => { const h = host(r, id); return h ? Array.from(h.querySelectorAll(sel)) : []; };
const one = (r, id, sel) => all(r, id, sel)[0] || null;
const text = (r, id, sel) => { const n = one(r, id, sel); return n ? n.textContent : null; };
const tableRows = (r, id) => { const t = one(r, id, '.jc-chart-table'); return t ? t.querySelectorAll('tr').length : 0; };
const attrs = (r, id, sel, a) => all(r, id, sel).map(n => n.getAttribute(a));
const tick = r => r.intervals.filter(x => x.live).forEach(x => x.fn());
const flush = async () => { for (let i = 0; i < 8; i++) await new Promise(res => setImmediate(res)); };

const ROWS = [{ day: 'Mon', placed: 3, delivered: 1 }, { day: 'Tue', placed: 5, delivered: 2 }, { day: 'Wed', placed: 4, delivered: 4 }];
const LINE = {
	id: 'c1', type: 'chart', title: 'Orders', kind: 'line', x: { field: 'day', label: 'Day' },
	series: [{ field: 'placed', label: 'Placed' }, { field: 'delivered', label: 'Delivered', tone: 'ok' }], data: ROWS
};
const card = o => Object.assign({}, LINE, o);
const out = {};

(async () => {
	// 1. Line, inline data: structure, accessibility, legend, table toggle.  Without onClick the svg is an image and
	// the hit rects are not focusable; with onClick (c1k) the svg is a group of focusable buttons with roving tabindex.
	try {
		const r = load();
		r.mount([LINE, card({ id: 'c1k', onClick: { href: '/o?d={day}' } })]);
		await flush();
		const svg = one(r, 'c1', 'svg');
		const cap = one(r, 'c1', '.jc-chart-caption');
		const t = one(r, 'c1', '.jc-chart-toggle');
		const before = { expanded: t.getAttribute('aria-expanded'), text: t.textContent, hidden: one(r, 'c1', '.jc-chart-table-box').hidden };
		t.dispatch('click', {});
		const hits = all(r, 'c1', '.jc-chart-hit');
		const khits = all(r, 'c1k', '.jc-chart-hit');
		khits[0].dispatch('keydown', { key: 'ArrowRight', preventDefault: () => {} });
		out.line = {
			title: text(r, 'c1', '.jc-card-title'),
			lines: all(r, 'c1', '.jc-chart-line').length,
			marks: all(r, 'c1', '.jc-chart-mark').length,
			hits: hits.length,
			role: svg && svg.getAttribute('role'),
			aria: svg && svg.getAttribute('aria-label'),
			labelledby: svg && svg.getAttribute('aria-labelledby'),
			captionId: cap && cap.id,
			caption: text(r, 'c1', '.jc-chart-caption'),
			hitTabindex: hits.map(h => h.getAttribute('tabindex')),
			hitRoles: hits.map(h => h.getAttribute('role')),
			clickRole: one(r, 'c1k', 'svg').getAttribute('role'),
			clickHitRoles: khits.map(h => h.getAttribute('role')),
			legend: all(r, 'c1', '.jc-chart-legend-label').map(n => n.textContent),
			tableRows: tableRows(r, 'c1'),
			firstStroke: all(r, 'c1', '.jc-chart-line')[0].style.stroke,
			okStroke: all(r, 'c1', '.jc-chart-line')[1].style.stroke,
			toggleBefore: before,
			toggleAfter: { expanded: t.getAttribute('aria-expanded'), text: t.textContent, hidden: one(r, 'c1', '.jc-chart-table-box').hidden },
			tabindex: khits.map(h => h.getAttribute('tabindex')),
			focused: r.env.getActive() === khits[1],
			errors: r.errors
		};
	} catch (e) { out.line = { crash: String(e && e.stack || e) }; }

	// 2. Bar from src returning {rows}, stacked; and horizontal grouped.
	try {
		const r = load({ fetch: () => ok({ rows: ROWS }) });
		r.mount([card({ id: 'b1', kind: 'bar', data: undefined, src: '/stats', stacked: true }),
			card({ id: 'b2', kind: 'bar', data: ROWS, orientation: 'horizontal' })]);
		await flush();
		out.bar = {
			stackedBars: all(r, 'b1', '.jc-chart-bar').length,
			horizontalBars: all(r, 'b2', '.jc-chart-bar').length,
			fetches: r.fetches,
			errors: r.errors
		};
	} catch (e) { out.bar = { crash: String(e && e.stack || e) }; }

	// 3. Donut: folding two or more sub-2% slices into Other, the center total; a single small slice is not folded.
	try {
		const r = load();
		r.mount([
			{ id: 'd1', type: 'chart', kind: 'donut', label: 's', value: 'n', center: 'total',
				data: [{ s: 'a', n: 50 }, { s: 'b', n: 47 }, { s: 'c', n: 1 }, { s: 'd', n: 1 }, { s: 'e', n: 1 }] },
			{ id: 'd2', type: 'chart', kind: 'donut', label: 's', value: 'n', center: 'Mix', data: [{ s: 'a', n: 99 }, { s: 'b', n: 1 }] },
			{ id: 'd3', type: 'chart', kind: 'donut', label: 's', value: 'n', onClick: { href: '/o?s={s}' },
				data: [{ s: 'a', n: 50 }, { s: 'b', n: 48 }, { s: 'c', n: 1 }, { s: 'd', n: 1 }] }
		]);
		await flush();
		const d3 = all(r, 'd3', '.jc-chart-slice');
		d3[2].dispatch('click', {});
		d3[2].dispatch('keydown', { key: 'Enter', preventDefault: () => {} });
		const otherNav = r.navigated.slice(), otherClicks = r.clicks.length;
		d3[0].dispatch('click', {});
		out.donut = {
			plainRole: one(r, 'd1', 'svg').getAttribute('role'),
			plainTabindex: attrs(r, 'd1', '.jc-chart-slice', 'tabindex'),
			plainRoles: attrs(r, 'd1', '.jc-chart-slice', 'role'),
			clickRole: one(r, 'd3', 'svg').getAttribute('role'),
			clickLegend: all(r, 'd3', '.jc-chart-legend-label').map(n => n.textContent),
			clickTabindex: d3.map(n => n.getAttribute('tabindex')),
			clickRoles: d3.map(n => n.getAttribute('role')),
			otherNav, otherClicks, navigated: r.navigated,
			slices: all(r, 'd1', '.jc-chart-slice').length,
			legend: all(r, 'd1', '.jc-chart-legend-label').map(n => n.textContent),
			center: text(r, 'd1', '.jc-chart-center'),
			noFoldSlices: all(r, 'd2', '.jc-chart-slice').length,
			literalCenter: text(r, 'd2', '.jc-chart-center'),
			caption: text(r, 'd1', '.jc-chart-caption'),
			errors: r.errors
		};
	} catch (e) { out.donut = { crash: String(e && e.stack || e) }; }

	// 4. Card-level errors: a missing series field (E-JS-63) and a failed fetch (E-JS-62); neither repeats as E-JS-8.
	try {
		const r = load({ fetch: () => bad(500, { message: 'boom' }) });
		r.mount([card({ id: 'm', series: [{ field: 'missing' }] }), card({ id: 'f', data: undefined, src: '/x' })]);
		await flush();
		out.errors = { missing: text(r, 'm', '.jc-card-error'), fetch: text(r, 'f', '.jc-card-error'), log: r.errors };
	} catch (e) { out.errors = { crash: String(e && e.stack || e) }; }

	// 5. Refresh: polls at refreshMs, redraws with new rows, marks stale on failure and keeps the last chart.
	try {
		let i = 0;
		const r = load({ fetch: () => { i++; return i === 1 ? ok(ROWS) : i === 2 ? ok(ROWS.concat([{ day: 'Thu', placed: 1, delivered: 1 }])) : bad(503, { message: 'down' }); } });
		r.mount([card({ id: 'p', data: undefined, src: '/p', refreshMs: 5000 })]);
		await flush();
		const ms = r.intervals.filter(x => x.live).map(x => x.ms);
		r.intervals.filter(x => x.live).forEach(x => x.fn());
		await flush();
		const grown = tableRows(r, 'p');
		const grownCaption = text(r, 'p', '.jc-chart-caption');
		r.intervals.filter(x => x.live).forEach(x => x.fn());
		await flush();
		const body = one(r, 'p', '.jc-chart-body');
		out.refresh = { ms, grown, grownCaption, stale: body.classList.contains('jc-chart--stale'), staleTitle: body.getAttribute('title'),
			stillDrawn: !!one(r, 'p', 'svg'), log: r.errors };
	} catch (e) { out.refresh = { crash: String(e && e.stack || e) }; }

	// 6. refreshMs below the floor is clamped and logged (E-JS-75).
	try {
		const r = load({ fetch: () => ok(ROWS) });
		r.mount([card({ id: 'r2', data: undefined, src: '/p', refreshMs: 1000 })]);
		await flush();
		out.clamp = { ms: r.intervals.filter(x => x.live).map(x => x.ms), log: r.errors };
	} catch (e) { out.clamp = { crash: String(e && e.stack || e) }; }

	// 7. Unknown kind: loading until DOMContentLoaded, then E-JS-60; a later register redraws it.
	try {
		const r = load();
		r.mount([{ id: 'u', type: 'chart', kind: 'gauge', data: [{ v: 1 }, { v: 2 }] }]);
		await flush();
		const loading = !!one(r, 'u', '.jc-card-loading');
		r.env.fireDOMContentLoaded();
		await flush();
		const e60 = text(r, 'u', '.jc-card-error');
		r.w.JuneauCharts.register('gauge', (plot, rows, cardDef, c) => {
			const s = c.svg(plot);
			c.node(s, 'text', 'gauge-v').textContent = String(rows.length);
		});
		await flush();
		out.unknown = { loading, e60, redrawn: text(r, 'u', '.gauge-v'), errorGone: !one(r, 'u', '.jc-card-error') };
	} catch (e) { out.unknown = { crash: String(e && e.stack || e) }; }

	// 8. Registry: duplicates (E-JS-64), bad kinds (TypeError), the pre-load queue, the post-load push, kinds(), alias.
	try {
		const r = load({ before: w => { w.JuneauConsoleCharts = [['spark', (plot, rows, cd, c) => { c.svg(plot); }]]; } });
		let dup = null, badKind = null;
		try { r.w.JuneauCharts.register('line', () => {}); } catch (e) { dup = e.message; }
		try { r.w.JuneauCharts.register('Bad Kind', () => {}); } catch (e) { badKind = e.name; }
		r.w.JuneauConsoleCharts.push(['pie2', (plot, rows, cd, c) => { c.svg(plot); }]);
		out.registry = { dup, badKind, kinds: r.w.JuneauCharts.kinds(), alias: r.w.JuneauConsole.charts === r.w.JuneauCharts,
			version: r.w.JuneauCharts.version, log: r.errors };
	} catch (e) { out.registry = { crash: String(e && e.stack || e) }; }

	// 9. Charts script loaded before the shell: the card queue still registers it; the alias appears at first render.
	try {
		const r = load({ chartsFirst: true });
		r.mount([LINE]);
		await flush();
		out.chartsFirst = { lines: all(r, 'c1', '.jc-chart-line').length, alias: r.w.JuneauConsole.charts === r.w.JuneauCharts };
	} catch (e) { out.chartsFirst = { crash: String(e && e.stack || e) }; }

	// 10. Clicks: juneau:chart-click detail, Enter on a hit, href templating with encoding, and the same-origin rule
	// (E-JS-79): a scheme, an absolute cross-origin URL and a protocol-relative URL are all refused.  These hrefs would
	// fail ChartCardType/ChartDef in Java; the harness mounts the contract directly to prove the JS re-check.
	try {
		const r = load();
		r.mount([
			card({ id: 'k', onClick: { href: '/orders?day={day}&q={missing}' }, data: [{ day: 'a b&c', placed: 1, delivered: 1 }, { day: 'Tue', placed: 2, delivered: 2 }] }),
			card({ id: 'x', onClick: { href: 'java\tscript:alert({day})' } }),
			card({ id: 'x2', onClick: { href: 'https://evil.test/x?d={day}' } }),
			card({ id: 'x3', onClick: { href: '//evil.test/{day}' } })
		]);
		await flush();
		const hits = all(r, 'k', '.jc-chart-hit');
		hits[0].dispatch('click', {});
		hits[1].dispatch('keydown', { key: 'Enter', preventDefault: () => {} });
		for (const id of ['x', 'x2', 'x3']) all(r, id, '.jc-chart-hit')[0].dispatch('click', {});
		out.click = { clicks: r.clicks, navigated: r.navigated, log: r.errors };
	} catch (e) { out.click = { crash: String(e && e.stack || e) }; }

	// 11. Escaping, empty text and the default.
	try {
		const r = load();
		r.mount([
			{ id: 'e', type: 'chart', kind: 'donut', label: 's', value: 'n', data: [{ s: '<b>x</b>', n: 1 }] },
			card({ id: 'n1', data: [], emptyText: 'Nothing yet.' }),
			card({ id: 'n2', data: [] })
		]);
		await flush();
		out.escape = { legend: text(r, 'e', '.jc-chart-legend-label'), boldTags: all(r, 'e', 'b').length,
			empty: text(r, 'n1', '.jc-chart-empty'), emptyDefault: text(r, 'n2', '.jc-chart-empty') };
	} catch (e) { out.escape = { crash: String(e && e.stack || e) }; }

	// 12. destroyCard stops polling.
	try {
		const r = load({ fetch: () => ok(ROWS) });
		r.mount([card({ id: 'z', data: undefined, src: '/p', refreshMs: 5000 })]);
		await flush();
		const live = r.intervals.filter(x => x.live).length;
		r.w.JuneauConsole.destroyCard('z');
		out.destroy = { liveBefore: live, liveAfter: r.intervals.filter(x => x.live).length };
	} catch (e) { out.destroy = { crash: String(e && e.stack || e) }; }

	// 13. A kind whose renderer throws: the card shows E-JS-80, it is logged once, and the page banner has no E-JS-8.
	try {
		const r = load();
		r.w.JuneauCharts.register('boom', () => { throw new Error('kaput'); });
		r.mount([{ id: 'b', type: 'chart', kind: 'boom', data: [{ v: 1 }] }]);
		await flush();
		out.boom = { error: text(r, 'b', '.jc-card-error'), log: r.errors };
	} catch (e) { out.boom = { crash: String(e && e.stack || e) }; }

	// 14. Repaint keeps keyboard focus: a focused hit is focused again at the same index after a refresh redraw.
	try {
		let i = 0;
		const r = load({ fetch: () => { i++; return ok(i === 1 ? ROWS : ROWS.concat([{ day: 'Thu', placed: 1, delivered: 1 }])); } });
		r.mount([card({ id: 'q', data: undefined, src: '/q', refreshMs: 5000, onClick: { href: '/o?d={day}' } })]);
		await flush();
		all(r, 'q', '.jc-chart-hit')[0].dispatch('keydown', { key: 'ArrowRight', preventDefault: () => {} });
		tick(r);
		await flush();
		const hits = all(r, 'q', '.jc-chart-hit');
		out.repaint = { hits: hits.length, tabindex: hits.map(h => h.getAttribute('tabindex')), focusedIndex: hits.indexOf(r.env.getActive()),
			caption: text(r, 'q', '.jc-chart-caption'), log: r.errors };
	} catch (e) { out.repaint = { crash: String(e && e.stack || e) }; }

	// 15. A renderer whose update() throws on refresh: E-JS-80 once, the poll stops, one card-failed, no E-JS-8.
	try {
		const r = load({ fetch: () => ok(ROWS) });
		r.w.JuneauCharts.register('flaky', (plot, rows, cd, c) => {
			c.svg(plot);
			return { update: () => { throw new Error('later'); } };
		});
		r.mount([{ id: 'fl', type: 'chart', kind: 'flaky', src: '/fl', refreshMs: 5000 }]);
		await flush();
		const liveBefore = r.intervals.filter(x => x.live).length;
		tick(r);
		await flush();
		tick(r);
		await flush();
		out.flaky = { liveBefore, liveAfter: r.intervals.filter(x => x.live).length, error: text(r, 'fl', '.jc-card-error'),
			stale: one(r, 'fl', '.jc-chart-body').classList.contains('jc-chart--stale'), failed: r.failed, log: r.errors };
	} catch (e) { out.flaky = { crash: String(e && e.stack || e) }; }

	// 16. A refresh whose new rows fail the draw (E-JS-63) fails the card like a first render: card-failed, no E-JS-8.
	try {
		let i = 0;
		const r = load({ fetch: () => { i++; return ok(i === 1 ? ROWS : [{ day: 'Mon' }]); } });
		r.mount([card({ id: 'dr', data: undefined, src: '/dr', refreshMs: 5000, series: [{ field: 'placed' }] })]);
		await flush();
		tick(r);
		await flush();
		out.redrawFail = { error: text(r, 'dr', '.jc-card-error'), failed: r.failed, liveAfter: r.intervals.filter(x => x.live).length,
			stale: one(r, 'dr', '.jc-chart-body').classList.contains('jc-chart--stale'), log: r.errors };
	} catch (e) { out.redrawFail = { crash: String(e && e.stack || e) }; }

	process.stdout.write(JSON.stringify(out));
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
