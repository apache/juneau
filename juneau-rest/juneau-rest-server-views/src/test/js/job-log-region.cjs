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
 * job-log-region.cjs - always-on Node harness for the job-log card in a row's detail panel (the "job-log" populator
 * juneau-joblog.js registers, and JuneauJobLog.mount): late registration and E-JS-111, the row key ("keying" then
 * idle, Retry after a refusal), field presets from the parent row, the submit through JuneauViews.jobs, output into
 * the region, the detail grid, and a re-attach after a collapse.  Every assertion lives in ConsoleJobLogRegion_Test.
 *
 *   Usage:  node job-log-region.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js> <juneau-joblog.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const H = require(path.join(__dirname, 'regions-harness.cjs'));
const { makeEnv, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath, jobLogPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath || !helpersJsPath || !jobLogPath) {
	console.error('usage: node job-log-region.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js> <juneau-joblog.js>');
	process.exit(2);
}
const jobLogSrc = fs.readFileSync(jobLogPath, 'utf8');

const STARTED = { jobId: 'j-1', streamUrl: '/s/j-1', cancelUrl: '/c/j-1' };
const LOCATION = { origin: 'http://h.test', href: 'http://h.test/species', pathname: '/species', search: '', hash: '' };
const SUBMIT = {
	endpoint: '/species/{id}/restock', envelope: 'action', action: 'restock-species', keyUrl: '/species/{id}/key',
	submitLabel: 'Restock',
	fields: [{ name: 'count', label: 'How many', type: 'number', default: 1 }]
};
const config = o => Object.assign({ submit: SUBMIT }, o || {});
const submitWith = fields => Object.assign({}, SUBMIT, { fields });
const result = (outcome, extra) => JSON.stringify(Object.assign({ contractVersion: '1', outcome }, extra || {}));

class FakeES {
	constructor(url) { this.url = String(url); this.listeners = {}; this.readyState = 1; FakeES.all.push(this); }
	addEventListener(type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); }
	close() { this.readyState = 2; }
	fire(type, data) { if (this.readyState !== 2) (this.listeners[type] || []).slice().forEach(fn => fn({ type, data })); }
}
FakeES.all = [];

function deferred() {
	let resolve, reject;
	const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
	return { promise, resolve, reject };
}

/** Lets promise chains and zero-delay timers run, a few rounds of each. */
async function settle(clock) {
	for (let i = 0; i < 6; i++) {
		await H.flush(10);
		if (clock) clock.advance(0);
	}
}

/**
 * A view table with row r1 (data `row`), expanded into a detail panel, with the stock runtimes and juneau-joblog.js
 * loaded and a routed fetch.  `opts.routes` overrides a route; a route is `(init) => Promise<Response>`.
 */
function setup(opts) {
	opts = opts || {};
	FakeES.all = [];
	const loaded = H.load(rendersJsPath, viewsJsPath, regionsJsPath, {
		helpersJsPath,
		extraJsPaths: [jobLogPath],
		decorate: (env, sandbox) => {
			env.window.location = LOCATION;
			env.window.EventSource = FakeES;
			sandbox.EventSource = FakeES;
			sandbox.URLSearchParams = URLSearchParams;
			if (opts.decorate) opts.decorate(env, sandbox);
		}
	});
	const { env, NS, R, rec, clock } = loaded;
	const table = env.el('table');
	table.setAttribute('data-juneau-view', 'species');
	table.setAttribute('data-juneau-csrf', 'tok-1');
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	env.body.appendChild(table);
	const rowData = new Map();
	table.__juneauCtx = {
		table, viewDef: { id: 'species', rowActions: [] },
		dataTable: { row: tr => ({ data: () => rowData.get(tr) }), on: () => {} }
	};
	const tr = env.el('tr');
	tr.setAttribute('data-juneau-row-id', 'r1');
	tbody.appendChild(tr);
	rowData.set(tr, opts.row || { id: 'r1', species: 'cat' });
	let child = null, panel = null;
	function expand() {
		child = env.el('tr');
		const td = env.el('td');
		panel = env.el('div');
		panel.className = 'juneau-view-detail-panel';
		panel.setAttribute('data-juneau-row-detail', '');
		panel.setAttribute('data-juneau-row-id', 'r1');
		panel._juneauParentTr = tr;
		tr._juneauDetailPanel = panel;
		td.appendChild(panel);
		child.appendChild(td);
		tbody.appendChild(child);
		return panel;
	}
	/** Collapses: the panel's regions are torn down and its child row leaves the table. */
	function collapse() {
		R.teardownRegionsIn(panel);
		tbody.removeChild(child);
		tr._juneauDetailPanel = null;
	}
	let keys = 0;
	const calls = [];
	const routes = Object.assign({
		'/species/r1/key': () => Promise.resolve(jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: 'k-' + (++keys) })),
		'/species/r1/restock': () => Promise.resolve(jsonResponse(STARTED, { status: 202 })),
		'/c/j-1': () => Promise.resolve(jsonResponse({}))
	}, opts.routes || {});
	env.setFetch((url, init) => {
		const i = init || {};
		calls.push({ url: String(url), method: i.method || 'GET', body: i.body ? JSON.parse(i.body) : null });
		const fn = routes[url];
		return fn ? fn(i) : Promise.reject(new TypeError('no route ' + url));
	});
	/** A job-log region in the (current) panel, populated. */
	async function region(cfg, extra) {
		const el = H.mkRegion(env, Object.assign({ id: 'restock', type: 'row-detail', populate: 'job-log', parent: panel,
			declared: Object.assign({ contractVersion: '1', config: cfg }, (extra && extra.declared) || {}) }));
		R.initRegion(el);
		await settle(clock);
		return el;
	}
	const submits = () => calls.filter(c => c.url === '/species/r1/restock');
	return { env, NS, R, rec, clock, table, tr, rowData, expand, collapse, region, calls, submits, routes, keyCount: () => keys,
		get panel() { return panel; } };
}

const q = (el, sel) => el.querySelector(sel);
function view(el) {
	const root = q(el, '.jc-joblog');
	const btn = q(el, '.jc-joblog-submit');
	const banner = q(el, '.jc-joblog-banner');
	const retry = q(el, '.jc-joblog-retry');
	return {
		phase: root ? root.getAttribute('data-phase') : null,
		submitText: btn && btn.textContent,
		submitDisabled: !!(btn && btn.disabled),
		submitHidden: !!(btn && btn.hidden),
		inputsDisabled: Array.from(el.querySelectorAll('.jc-joblog-input')).filter(n => n.type !== 'hidden').map(n => n.disabled),
		retryShown: !!(retry && !retry.hidden),
		banner: banner && !banner.hidden ? banner.textContent : null,
		lines: Array.from(el.querySelectorAll('.jc-joblog-line')).map(n => n.textContent)
	};
}
async function submit(t, el) {
	q(el, '.jc-joblog-form').dispatch('submit', { preventDefault: () => {} });
	await settle(t.clock);
}
const lastES = () => FakeES.all[FakeES.all.length - 1];

const out = {};
const guard = async (name, fn) => {
	try { out[name] = await fn(); } catch (e) { out[name] = { crash: String(e && e.stack || e) }; }
};

(async () => {
	// 1. Registration: loaded after juneau-regions.js the populator is registered at once; loaded before it, at
	// DOMContentLoaded; an app's own "job-log", registered first, is kept.
	await guard('registration', async () => {
		const t = setup();
		const now = typeof t.R.resolve('job-log') === 'function';
		const late = (keep) => {
			const env = makeEnv();
			env.document.readyState = 'loading';
			env.window.location = LOCATION;
			const sandbox = { window: env.window, document: env.document, console: { error: () => {} }, setTimeout, clearTimeout,
				setInterval: () => 0, clearInterval: () => {}, Promise, URL, AbortController, AbortSignal, DOMException,
				fetch: () => Promise.reject(new Error('none')) };
			vm.runInNewContext(jobLogSrc, sandbox, { filename: 'juneau-joblog.js' });
			const before = !!env.window.JuneauViews;
			for (const p of [rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath])
				vm.runInNewContext(fs.readFileSync(p, 'utf8'), sandbox, { filename: path.basename(p) });
			const own = function appJobLog() {};
			if (keep) env.window.JuneauViews.regions.register('job-log', own);
			env.dispatchDocument('DOMContentLoaded');
			const fn = env.window.JuneauViews.regions.resolve('job-log');
			return { viewsBeforeJobLog: before, registered: typeof fn === 'function', appKept: fn === own };
		};
		return { immediate: now, late: late(false), appFirst: late(true) };
	});

	// 2. E-JS-111: JuneauJobLog.mount without JuneauViews.jobs throws; a page with a job-log region and no
	// juneau-regions.js logs once at window load.
	await guard('missingViews', async () => {
		const env = makeEnv();
		env.document.readyState = 'loading';
		env.window.location = LOCATION;
		const loadFns = [];
		env.window.addEventListener = (type, fn) => { if (type === 'load') loadFns.push(fn); };
		const errors = [];
		const sandbox = { window: env.window, document: env.document, console: { error: m => errors.push(String(m)) }, setTimeout, clearTimeout,
			Promise, URL, AbortController };
		vm.runInNewContext(jobLogSrc, sandbox, { filename: 'juneau-joblog.js' });
		let thrown = null;
		try { env.window.JuneauJobLog.mount(env.el('div'), config()); } catch (e) { thrown = e.code || String(e); }
		const r = env.el('div');
		r.setAttribute('data-juneau-region-populate', 'job-log');
		env.body.appendChild(r);
		env.dispatchDocument('DOMContentLoaded');
		loadFns.forEach(fn => fn());
		return { thrown, logged: errors.filter(e => e.indexOf('E-JS-111') >= 0).length, mountLogged: errors.filter(e => e.indexOf('JuneauViews.jobs') >= 0).length,
			regionLogged: errors.filter(e => e.indexOf('JuneauViews.regions') >= 0).length };
	});

	// 3. Keying: submit stays disabled (inputs enabled) until the row's key arrives; a refused key shows E-JS-109 and
	// Retry, which fetches again.
	await guard('keying', async () => {
		const d = deferred();
		let refuse = true;
		const t = setup({ routes: { '/species/r1/key': () => d.promise } });
		t.expand();
		const el = await t.region(config());
		const keying = view(el);
		d.resolve(jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: 'k-a' }));
		await settle(t.clock);
		const ready = view(el);
		const t2 = setup({ routes: { '/species/r1/key': () => Promise.resolve(refuse
			? jsonResponse({ contractVersion: '1', action: 'other', targetId: 'r1', idempotencyKey: 'k-x' })
			: jsonResponse({ contractVersion: '1', action: 'restock-species', targetId: 'r1', idempotencyKey: 'k-b' })) } });
		t2.expand();
		const el2 = await t2.region(config());
		const refused = view(el2);
		const logged = t2.rec.errorsMatching('E-JS-109').length;
		refuse = false;
		q(el2, '.jc-joblog-retry').dispatch('click', {});
		await settle(t2.clock);
		const retried = view(el2);
		await submit(t2, el2);
		return { keying, ready, refused, logged, retried, retriedKey: t2.submits()[0] && t2.submits()[0].body.idempotencyKey };
	});

	// 4. Submit: the action envelope with the row id as target and the row's key; progress lines; the result; the key
	// is spent (a fresh one is fetched for the next run).
	await guard('submit', async () => {
		const t = setup();
		t.expand();
		const el = await t.region(config());
		q(el, '.jc-joblog-input').value = '3';
		await submit(t, el);
		const running = view(el);
		const es = lastES();
		es.fire('progress', 'half way');
		es.fire('result', result('success', { message: 'restocked' }));
		await settle(t.clock);
		const done = view(el);
		q(el, '.jc-joblog-newrun').dispatch('click', {});
		await settle(t.clock);
		return { body: t.submits()[0].body, streamUrl: es.url, running, done, keysFetched: t.keyCount(), afterNewRun: view(el) };
	});

	// 5. Presets: a hidden source reads an own property of the parent row AT SUBMIT; an inherited name (__proto__,
	// constructor, toString) or an object is refused with E-JS-110 and nothing is sent unless there is a default;
	// null is sent as null.
	await guard('sources', async () => {
		const one = async (field, row, mutate) => {
			const t = setup({ row });
			t.expand();
			const el = await t.region(config({ submit: submitWith([field]) }));
			if (mutate) mutate(t);
			await submit(t, el);
			const s = t.submits()[0];
			return { sent: s ? s.body.fields : null, e110: t.rec.errorsMatching('E-JS-110').length, banner: view(el).banner };
		};
		const row = { id: 'r1', species: 'cat', obj: { a: 1 }, nul: null };
		return {
			own: await one({ name: 'species', type: 'hidden', source: 'row.species' }, row),
			proto: await one({ name: 'p', type: 'hidden', source: 'row.__proto__' }, row),
			protoDefault: await one({ name: 'p', type: 'hidden', source: 'row.__proto__', default: 'd' }, row),
			ctor: await one({ name: 'c', type: 'hidden', source: 'row.constructor' }, row),
			toStr: await one({ name: 's', type: 'hidden', source: 'row.toString' }, row),
			object: await one({ name: 'o', type: 'hidden', source: 'row.obj' }, row),
			objectDefault: await one({ name: 'o', type: 'hidden', source: 'row.obj', default: 7 }, row),
			nul: await one({ name: 'n', type: 'hidden', source: 'row.nul' }, row),
			atSubmit: await one({ name: 'species', type: 'hidden', source: 'row.species' }, row,
				t => t.rowData.set(t.tr, { id: 'r1', species: 'dog' })),
			prefill: await (async () => {
				const t = setup({ row });
				t.expand();
				const el = await t.region(config({ submit: submitWith([{ name: 'species', label: 'Species', type: 'text', source: 'row.species' },
					{ name: 'o', label: 'O', type: 'text', source: 'row.obj' }]) }));
				return { values: Array.from(el.querySelectorAll('.jc-joblog-input')).map(n => n.value), e110: t.rec.errorsMatching('E-JS-110').length };
			})()
		};
	});

	// 6. Output: into a detail-grid slot (textContent, no element created), into the card's own input (.value),
	// replace or append, success only by default; a selector that matches only outside the region logs E-JS-112.
	await guard('output', async () => {
		const detail = { renderer: 'fieldGrid', fields: [{ data: 'available', label: 'Available' }] };
		const one = async (cfg, outcome, extra) => {
			const t = setup();
			t.expand();
			const outside = t.env.el('div');
			outside.className = 'elsewhere';
			t.env.body.appendChild(outside);
			const el = await t.region(cfg);
			const slot = q(el, '[data-juneau-field="available"]');
			if (slot) slot.textContent = 'old';
			const note = Array.from(el.querySelectorAll('.jc-joblog-input')).find(n => n.name === 'note');
			if (note && extra && extra.note != null) note.value = extra.note;
			await submit(t, el);
			lastES().fire('result', result(outcome, { message: 'm1', row: { available: 9, obj: { x: 1 } } }));
			await settle(t.clock);
			return { slot: slot ? slot.textContent : null, slotChildren: slot ? slot.childNodes.length : null, note: note ? note.value : null,
				outside: outside.textContent, e112: t.rec.errorsMatching('E-JS-112').length };
		};
		const withNote = submitWith(SUBMIT.fields.concat([{ name: 'note', label: 'Note', type: 'textarea' }]));
		return {
			slot: await one(config({ detail, output: { target: { field: 'available' }, from: 'row.available' } }), 'success'),
			failureSkipped: await one(config({ detail, output: { target: { field: 'available' }, from: 'row.available' } }), 'failure'),
			anyWritten: await one(config({ detail, output: { target: { field: 'available' }, when: 'any' } }), 'failure'),
			appendSlot: await one(config({ detail, output: { target: { field: 'available' }, mode: 'append' } }), 'success'),
			input: await one(config({ submit: withNote, output: { target: { field: 'note' } } }), 'success', { note: 'typed' }),
			appendInput: await one(config({ submit: withNote, output: { target: { field: 'note' }, mode: 'append' } }), 'success', { note: 'typed' }),
			objectSkipped: await one(config({ detail, output: { target: { field: 'available' }, from: 'row.obj' } }), 'success'),
			outsideOnly: await one(config({ output: { target: { selector: '.elsewhere' } } }), 'success')
		};
	});

	// 7. The detail grid: painted above the form from the declared payload; its slots carry data-juneau-field.
	await guard('detail', async () => {
		const t = setup({ routes: { '/species/r1/detail': () => Promise.resolve(jsonResponse({ contractVersion: '1', fields: { available: 4 } })) } });
		t.expand();
		const el = await t.region(config({ detail: { renderer: 'fieldGrid', fields: [{ data: 'available', label: 'Available' }] } }),
			{ declared: { dataUrl: '/species/{id}/detail' } });
		const d = q(el, '.jc-joblog-detail');
		const slot = q(el, '[data-juneau-field="available"]');
		const root = q(el, '.jc-joblog');
		return { gridInDetail: !!(d && slot && d.contains(slot)), value: slot ? slot.textContent : null,
			beforeForm: !!root && root.childNodes.indexOf(d) < root.childNodes.indexOf(q(el, '.jc-joblog-form')),
			regionState: el.getAttribute('data-juneau-region-state') };
	});

	// 8. Collapse and re-expand while the job runs: the job keeps going; the new view attaches view-only from the
	// start of the log (no submit, no cancel) and shows the outcome; then it gets a key for the next run.
	await guard('reattach', async () => {
		const t = setup();
		t.expand();
		const el = await t.region(config());
		await submit(t, el);
		const jobES = lastES();
		jobES.fire('progress', 'one');
		t.collapse();
		await settle(t.clock);
		const afterCollapse = { jobStreamOpen: jobES.readyState !== 2, active: !!t.NS.jobs.active(t.tr, 'restock-species'), viewGone: !q(el, '.jc-joblog') };
		t.expand();
		const el2 = await t.region(config());
		const viewES = lastES();
		const attached = view(el2);
		viewES.fire('progress', 'one');
		viewES.fire('progress', 'two');
		jobES.fire('result', result('success', { message: 'ok' }));
		viewES.fire('result', result('success', { message: 'ok' }));
		await settle(t.clock);
		const done = view(el2);
		return { afterCollapse, viewStreamUrl: viewES.url, distinctStream: viewES !== jobES, attached, done, submits: t.submits().length,
			keyAfter: t.keyCount() };
	});

	// 9. Collapse and re-expand while the submit is still in flight: the new view waits ("submitting", view-only) and
	// attaches once the job starts.
	await guard('reattachPending', async () => {
		const d = deferred();
		const t = setup({ routes: { '/species/r1/restock': () => d.promise } });
		t.expand();
		const el = await t.region(config());
		await submit(t, el);
		t.collapse();
		t.expand();
		const el2 = await t.region(config());
		const waiting = view(el2);
		d.resolve(jsonResponse(STARTED, { status: 202 }));
		await settle(t.clock);
		const attached = view(el2);
		return { waiting, attached, streams: FakeES.all.map(e => e.url) };
	});

	// 10. A second view of the same row cannot start another job: the runtime refuses it, the view says so.
	await guard('oneJobPerRow', async () => {
		const t = setup();
		t.expand();
		const el = await t.region(config());
		const host = t.env.el('div');
		t.panel.appendChild(host);
		const second = t.env.window.JuneauJobLog.mount(host, config());
		await settle(t.clock);
		await submit(t, el);
		q(host, '.jc-joblog-form').dispatch('submit', { preventDefault: () => {} });
		await settle(t.clock);
		return { second: view(host), submits: t.submits().length, apiKeys: Object.keys(second).sort(), phase: second.phase() };
	});

	// 11. A transport failure keeps the key (the retry sends the same one); an HTTP error spends it.
	await guard('keyLifecycle', async () => {
		let mode = 'fail';
		const t = setup({ routes: { '/species/r1/restock': () => mode === 'fail' ? Promise.reject(new TypeError('offline'))
			: mode === 'http' ? Promise.resolve(jsonResponse('nope', { status: 500 })) : Promise.resolve(jsonResponse(STARTED, { status: 202 })) } });
		t.expand();
		const el = await t.region(config());
		await submit(t, el);
		const failed = view(el);
		mode = 'http';
		await submit(t, el);
		const http = view(el);
		const keys = t.submits().map(s => s.body.idempotencyKey);
		return { failed, http, keys, keysFetched: t.keyCount() };
	});

	// 12. Cancel posts through JuneauViews.jobs (CSRF-stamped); the outcome still arrives on the stream.
	await guard('cancel', async () => {
		const t = setup();
		t.expand();
		const el = await t.region(config());
		await submit(t, el);
		const btn = q(el, '.jc-joblog-submit');
		const running = view(el);
		btn.dispatch('click', { preventDefault: () => {} });
		await settle(t.clock);
		const cancelling = view(el);
		lastES().fire('result', result('cancelled'));
		await settle(t.clock);
		return { running, cancelling, cancelPosted: t.calls.some(c => c.url === '/c/j-1' && c.method === 'POST'), done: view(el) };
	});

	process.stdout.write(JSON.stringify(out));
})().catch(e => {
	console.error(e && e.stack || e);
	process.exit(1);
});
