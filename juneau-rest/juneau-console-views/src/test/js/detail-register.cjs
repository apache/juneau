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
 * detail-register.cjs - always-on Node harness for the client-rendered tabs: the detail renderer registry, the frozen data object and helpers whitelist, same-origin fetch and error handling.  Every
 * assertion lives in the paired Java test.
 *
 *   Usage:  node detail-register.cjs <renders> <views> <regions> <helpers> <bus> <model>
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const [rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath, busJsPath, modelJsPath] = process.argv.slice(2);
if (!modelJsPath) {
	console.error('usage: node detail-register.cjs <renders> <views> <regions> <helpers> <bus> <model>');
	process.exit(2);
}

const out = {};

async function scenario(detail, opts) {
	opts = opts || {};
	const loaded = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath, busJsPath: busJsPath, extraJsPaths: [modelJsPath] });
	const { env, NS, I } = loaded;
	const M = NS.model;
	const calls = [];
	const recFetch = [];
	if (opts.setup) opts.setup(NS, calls, env);
	NS.handlers.register('app.loaded', function (t, ev) { calls.push('loaded:' + t.kind + ':' + t.id + ':' + JSON.stringify(ev.payload)); });
	NS.handlers.register('app.activated', function (t, ev) { calls.push('activated:' + t.id); });
	NS.handlers.register('app.rendered', function (t, ev) { calls.push('rendered:' + t.kind); });
	NS.handlers.register('app.go', function (t, ev) { calls.push('go:' + t.kind + ':' + ev.type); if (opts.throwOnGo) throw new Error('boom'); });
	env.setFetch(function (url, init) {
		recFetch.push({ url: String(url), init: init });
		if (opts.fetchImpl) return opts.fetchImpl(String(url), init);
		return Promise.resolve({ ok: true, status: 200, headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: {} })); } });
	});

	const page = M.fromJSON({ title: 'T', cards: [{ id: 'c1', type: 'datatables', table: { detail: detail } }] });
	const root = env.el('div');
	env.body.appendChild(root);
	page._attach(root);
	const host = env.el('div');
	root.appendChild(host);
	page.card('c1')._attach(host);
	M.resolveHandlers();

	const table = env.el('table');
	table.dataset.juneauView = 'c1';
	table.dataset.juneauCsrf = 'tok-1';
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	const tpl = I.buildDetailTemplate(detail, table);
	tpl.content = {
		cloneNode: function () {
			const wrap = env.el('div');
			I.buildDetailTemplate(detail, table).content.childNodes.slice().forEach(function (k) { wrap.appendChild(k); });
			return wrap;
		}
	};
	host.appendChild(tpl);

	const rowData = { id: 's-7', name: 'Rex', hidden: 'secret' };
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = 's-7';
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);
	const state = { shown: false, panel: null };
	const child = function (panel) {
		state.panel = panel;
		return { show: function () {
			const ctr = env.el('tr'); const ctd = env.el('td');
			ctr.appendChild(ctd); ctd.appendChild(panel); tbody.appendChild(ctr); state.shown = true;
		}, hide: function () { state.shown = false; } };
	};
	child.isShown = function () { return state.shown; };
	child.hide = function () { state.shown = false; };
	const ctx = {
		table: table, viewDef: { id: 'c1', columns: [{ data: 'name', title: 'Name' }] },
		dataTable: { row: function () { return { length: 1, child: child, data: function () { return rowData; } }; } }
	};
	table.__juneauCtx = ctx;
	I.initDetailsExpander(table, ctx, ctx.viewDef);
	table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);
	const mt = NS.tables.handle(table);
	return { env, NS, I, M, calls, recFetch, rec: loaded.rec, panel: state.panel, mt, ctx, table,
		detail: function () { const r = mt.row('s-7'); return r?.detail; } };
}


const CLIENT = function (extra) {
	return {
		mode: 'inline', title: 'Pet {id}',
		tabs: [{ id: 'c', kind: 'client', title: 'Custom', renderer: 'pet-labels', emptyText: 'Nothing here', ...extra }]
	};
};

const resp = function (status, body) {
	return Promise.resolve({ ok: status >= 200 && status < 300, status: status, headers: { get: function () { return null; } },
		text: function () { return Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)); } });
};

function write(env, pane, text) { const e = env.el('span'); e.textContent = text; pane.appendChild(e); }

function statusText(pane) { return pane.querySelectorAll('[role="status"]').map(function (n) { return n.textContent; }).join('|'); }

(async function main() {
	// --- registered renderer: called once with the tab handle and a frozen data object
	let seen = null, count = 0;
	const a = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('pet-labels', function (tab, data) {
			count++;
			seen = { tabId: tab.id, kind: tab.kind, keys: Object.keys(data).sort((a, b) => (a < b ? -1 : Number(a > b))).join(','), frozen: Object.isFrozen(data),
				rowId: data.rowId, name: data.row.name, hidden: data.row.hidden, refresh: data.refresh, hasPane: !!data.pane, hasSignal: !!data.signal,
				helperKeys: Object.keys(tab.helpers).sort((a, b) => (a < b ? -1 : Number(a > b))).join(','), helpersFrozen: Object.isFrozen(tab.helpers),
				fieldGrid: typeof tab.helpers.fieldGrid, status: typeof tab.helpers.renderAsyncStatus };
			try { tab.helpers.extra = 1; } catch (e) { /* strict mode */ } // NOSONAR javascript:S2486 - strict mode rejects the write, which is the point
			try { tab.helpers = {}; } catch (e) { /* strict mode */ } // NOSONAR javascript:S2486 - strict mode rejects the write, which is the point
			seen.helpersStill = Object.keys(tab.helpers).sort((a, b) => (a < b ? -1 : Number(a > b))).join(',');
			write(env, data.pane, 'rendered');
		});
	}});
	out.reg_calledOnce = count === 1;
	out.reg_tab = seen && seen.tabId + ':' + seen.kind;
	out.reg_dataKeys = seen?.keys;
	out.reg_dataFrozen = seen?.frozen;
	out.reg_row = seen && seen.rowId + ':' + seen.name + ':' + seen.hidden;
	out.reg_refresh = seen?.refresh;
	out.reg_paneSignal = seen?.hasPane && seen.hasSignal;
	out.reg_helpers = seen && seen.helperKeys + ':' + seen.helpersFrozen + ':' + seen.fieldGrid + ':' + seen.status + ':' + seen.helpersStill;
	out.reg_painted = a.panel.querySelectorAll('[role="tabpanel"]').map(function (p) { return p.textContent; }).join('|').indexOf('rendered') >= 0;

	// --- blank name / non-function refused; last write wins
	const b = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('', function () {});
		NS.detail.register('   ', function () {});
		NS.detail.register('pet-labels', 'not a function');
		NS.detail.register('pet-labels', function (t, d) { write(env, d.pane, 'first'); });
		NS.detail.register('pet-labels', function (t, d) { write(env, d.pane, 'second'); });
	}});
	out.refuse_errors = b.rec.errors.length;
	out.lastWins = b.panel.querySelectorAll('[role="tabpanel"]').map(function (p) { return p.textContent; }).join('|').indexOf('second') >= 0;

	// --- unregistered
	const u = await scenario(CLIENT({ renderer: 'nope' }), {});
	out.unregistered = statusText(u.panel);

	// --- data.fetch
	let fetchResults = {};
	const f = await scenario(CLIENT(), {
		setup: function (NS, calls, env) {
			NS.detail.register('pet-labels', function (tab, data) {
				const run = function (k, url) { return data.fetch(url).then(function (v) { fetchResults[k] = 'ok:' + JSON.stringify(v); }, function (e) { fetchResults[k] = 'err:' + e.message; }); };
				return Promise.all([
					run('good', '/api/pet/{id}/labels'),
					run('cross', 'http://evil.example/x'),
					run('proto', '//evil.example/x'),
					run('bad', '/api/bad'),
					run('contract', '/api/contract')
				]);
			});
		},
		fetchImpl: function (url) {
			if (url.indexOf('/api/bad') === 0) return resp(500, {});
			if (url.indexOf('/api/contract') === 0) return resp(200, { contractVersion: '9' });
			return resp(200, { contractVersion: '1', labels: ['a'] });
		}
	});
	await flush(20);
	out.fetch_good = fetchResults.good;
	out.fetch_cross = (fetchResults.cross || '').indexOf('err:') === 0;
	out.fetch_proto = (fetchResults.proto || '').indexOf('err:') === 0;
	out.fetch_bad = (fetchResults.bad || '').indexOf('err:') === 0;
	out.fetch_contract = (fetchResults.contract || '').indexOf('err:') === 0;
	const good = f.recFetch.find(function (r) { return r.url.indexOf('/api/pet/') === 0; });
	out.fetch_url = good?.url;
	out.fetch_accept = good?.init?.headers?.Accept;
	out.fetch_csrf = !!(good?.init && Object.values(good.init.headers || {}).indexOf('tok-1') >= 0);
	out.fetch_noEvilRequest = f.recFetch.every(function (r) { return r.url.indexOf('evil') < 0; });

	// --- throw -> error with Retry; Retry re-runs
	let runs = 0;
	const t = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('pet-labels', function (tab, data) { runs++; if (runs === 1) { throw new Error('kaput'); } write(env, data.pane, 'recovered'); });
	}});
	out.throw_status = statusText(t.panel);
	const retry = t.panel.querySelector('.juneau-view-detail-retry');
	out.throw_hasRetry = !!retry;
	if (retry) retry.dispatch('click', { preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);
	out.retry_reran = runs === 2 && t.panel.querySelectorAll('[role="tabpanel"]').map(function (p) { return p.textContent; }).join('|').indexOf('recovered') >= 0;

	// --- data.fail(err)
	const fl = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('pet-labels', function (tab, data) { data.fail(new Error('nope')); });
	}});
	out.fail_status = statusText(fl.panel);
	out.fail_retry = !!fl.panel.querySelector('.juneau-view-detail-retry');

	// --- promise: loading until resolved; empty pane shows emptyText
	let release = null;
	const p = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('pet-labels', function () { return new Promise(function (r) { release = r; }); });
	}});
	out.promise_loading = statusText(p.panel);
	release();
	await flush(12);
	out.promise_empty = statusText(p.panel);

	// --- a rejected promise is an error
	const rj = await scenario(CLIENT(), { setup: function (NS, calls, env) {
		NS.detail.register('pet-labels', function () { return Promise.reject(new Error('async boom')); });
	}});
	out.reject_status = statusText(rj.panel);

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e); process.exit(1); });
