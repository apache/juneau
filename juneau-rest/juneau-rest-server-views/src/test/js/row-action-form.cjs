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
 * row-action-form.cjs - always-on Node harness for RowAction.form GET URL substitution: openActionDialog
 * substitutes `{property}` tokens and applies the write-path malformed-URL guards before the confirmation GET.
 *
 *   Usage:  node row-action-form.cjs <path-to-juneau-renders.js> <path-to-juneau-views.js>
 *
 * Loads both assets into views-dom-shim.cjs (and a views-only sandbox for the residual-token cases).
 * Stubs ctx.dataTable.row(tr).data() for row-backed cases; tr = null for ribbon.  Captures fetch's first
 * argument.  Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv, loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node row-action-form.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

function flush() {
	return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 8) { return r(); } setTimeout(tick, 0); })(); });
}

function loadViewsOnly(env) {
	env = env || makeEnv();
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function (fn) { if (typeof fn === 'function') { fn(); } return 0; },
		clearTimeout: function () {},
		setInterval: function () { return 0; },
		clearInterval: function () {},
		Promise: Promise,
		fetch: function (...args) { return env.callFetch(...args); }
	};
	// NOSONAR javascript:S1523 -- loading the production juneau-views.js under test into the sandbox.
	vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
	const NS = env.window.JuneauViews;
	return { env: env, NS: NS, I: NS?.init };
}

function rowFixture(env, rowData) {
	const table = env.el('table');
	const tr = env.el('tr');
	const cell = env.el('td');
	cell.className = 'juneau-view-actions-cell';
	tr.appendChild(cell);
	table.appendChild(tr);
	env.body.appendChild(table);
	const ctx = {
		table: table,
		viewDef: { id: 'v1' },
		dataTable: {
			row: function () { return { data: function () { return rowData; } }; }
		}
	};
	return { table: table, tr: tr, ctx: ctx };
}

function ribbonFixture(env) {
	const wrapper = env.el('div');
	wrapper.className = 'dt-container';
	env.body.appendChild(wrapper);
	const toolbar = env.el('div');
	toolbar.className = 'juneau-view-toolbar-row';
	wrapper.appendChild(toolbar);
	const table = env.el('table');
	wrapper.appendChild(table);
	const ctx = { table: table, viewDef: { id: 'v1' }, dataTable: {} };
	return { table: table, ctx: ctx };
}

function reasonOf(text) {
	if (!text) return null;
	if (text.indexOf('could not be completed from this row') >= 0) return 'empty-substitution';
	if (text.indexOf('could not be resolved') >= 0) return 'unresolved-endpoint';
	if (text.indexOf('unsafe path') >= 0) return 'unsafe-endpoint';
	return 'other';
}

function rowRefusal(env) {
	const el = env.body.querySelector('.juneau-view-action-refusal');
	return el ? el.textContent : null;
}

function ribbonRefusal(env) {
	const el = env.body.querySelector('.juneau-view-ribbon-action-refusal');
	return el ? el.textContent : null;
}

async function openForm(opts) {
	const o = opts || {};
	const loaded = o.omitRenders ? loadViewsOnly(makeEnv()) : loadViews(rendersJsPath, viewsJsPath, makeEnv());
	const env = loaded.env, I = loaded.I;
	const fetches = [];
	env.setFetch(function (url, init) {
		fetches.push({ url: url, method: (init && init.method) || 'GET', headers: init && init.headers });
		return Promise.resolve(jsonResponse({ title: 'ok' }));
	});
	const action = { id: 'ack', label: 'Acknowledge', present: 'dialog', method: 'POST',
		endpoint: '/x/ack', form: o.form, confirm: 'Confirm?' };
	let table, tr, ctx;
	if (o.ribbon) {
		const fx = ribbonFixture(env);
		table = fx.table; tr = null; ctx = fx.ctx;
	} else {
		const fx = rowFixture(env, Object.hasOwn(o, 'rowData') ? o.rowData : { id: 'ALRT-2' });
		table = fx.table; tr = fx.tr; ctx = fx.ctx;
	}
	I.openActionDialog(action, table, tr, ctx);
	await flush();
	const refusalText = o.ribbon ? ribbonRefusal(env) : rowRefusal(env);
	return {
		fetched: fetches.length > 0,
		url: fetches.length ? fetches[0].url : null,
		method: fetches.length ? fetches[0].method : null,
		accept: fetches.length ? (fetches[0].headers && fetches[0].headers.Accept) : null,
		credentials: fetches.length ? fetches[0].headers && fetches[0].headers : null,
		fetchCount: fetches.length,
		refusalReason: reasonOf(refusalText),
		dialogCount: typeof I.dialogLayerCount === 'function' ? I.dialogLayerCount() : -1,
		hasResolverExport: typeof I.resolveRowActionFormUrl === 'function'
	};
}

(async function main() {
	const probe = loadViews(rendersJsPath, viewsJsPath, makeEnv());
	const out = { hasInit: !!(probe.I && typeof probe.I.openActionDialog === 'function') };
	if (!out.hasInit) {
		process.stdout.write(JSON.stringify(out));
		return;
	}
	out.hasResolverExport = typeof probe.I.resolveRowActionFormUrl === 'function';

	const b01 = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: 'ALRT-2' } });
	out.b01_url = b01.url;
	out.b01_fetched = b01.fetched;

	const b01b = await openForm({ form: 'servlet:/incidents/{id}/form', rowData: { id: 'a1' } });
	out.b01b_url = b01b.url;

	const b02a = await openForm({ form: '/data/x/ack-form', rowData: { id: 'ALRT-2' } });
	const b02b = await openForm({ form: '/data/x/ack-form', rowData: null });
	out.b02_url_withRow = b02a.url;
	out.b02_url_noRow = b02b.url;

	const b03missing = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: {} });
	const b03null = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: null } });
	const b03absent = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: null });
	const b03ws = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: '   ' } });
	out.b03_missing_fetched = b03missing.fetched;
	out.b03_missing_reason = b03missing.refusalReason;
	out.b03_null_fetched = b03null.fetched;
	out.b03_null_reason = b03null.refusalReason;
	out.b03_absent_fetched = b03absent.fetched;
	out.b03_absent_reason = b03absent.refusalReason;
	out.b03_ws_fetched = b03ws.fetched;
	out.b03_ws_reason = b03ws.refusalReason;

	const b04 = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: 'a/1 b' } });
	out.b04_url = b04.url;

	const b05 = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: 'ALRT-2' }, omitRenders: true });
	out.b05_fetched = b05.fetched;
	out.b05_reason = b05.refusalReason;

	const b06 = await openForm({ form: '/data/x/ack-form', omitRenders: true });
	out.b06_fetched = b06.fetched;
	out.b06_url = b06.url;

	const b07 = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: '..' } });
	out.b07_fetched = b07.fetched;
	out.b07_reason = b07.refusalReason;

	const b08 = await openForm({ form: 'servlet:/incidents/../form', rowData: { id: 'a1' } });
	out.b08_fetched = b08.fetched;
	out.b08_reason = b08.refusalReason;

	const b09 = await openForm({ form: 'servlet:/x/form?target={id}', rowData: { id: 'a&b=c#d' } });
	out.b09_url = b09.url;

	const b10 = await openForm({ form: '/data/alerts/{id}/ack-form', rowData: { id: 'a/../b' } });
	out.b10_url = b10.url;
	out.b10_fetched = b10.fetched;

	const b11 = await openForm({ form: '/x/{id}/status/{status}', rowData: { id: 'a1', status: '' } });
	out.b11_fetched = b11.fetched;
	out.b11_reason = b11.refusalReason;

	const b12 = await openForm({ form: 'servlet:/incidents/{id}/form', ribbon: true });
	out.b12_fetched = b12.fetched;
	out.b12_reason = b12.refusalReason;

	const b13 = await openForm({ form: '/projects/new-form', ribbon: true });
	out.b13_fetched = b13.fetched;
	out.b13_url = b13.url;

	const b14 = await openForm({
		form: 'servlet:/x/{id}/form?juneauDrafts=%7B%22note%22%3A%22hi%22%7D',
		rowData: { id: 'a1' }
	});
	out.b14_url = b14.url;

	const b15 = await openForm({ form: '' });
	out.b15_fetched = b15.fetched;
	out.b15_dialogCount = b15.dialogCount;
	out.b15_refusalReason = b15.refusalReason;

	process.stdout.write(JSON.stringify(out));
})().catch(function (err) {
	console.error(err && err.stack ? err.stack : err);
	process.exit(1);
});
