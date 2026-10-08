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
 * row-actions-run.cjs - always-on Node harness for JuneauViews.rowActions.run on a confirm-only action that names a
 * confirm renderer: the renderer's answer gates the write (true sends exactly one request, false sends none), a
 * {field} token in the confirm text is filled from the row (and an empty one refuses before anything shows), tone and
 * confirmLabel reach the renderer, plus the unknown-action (E-JS-70) and outside-a-row (E-JS-74) rejections.  The
 * shim's closest() has no descendant combinators, so the harness points the row lookup at its own <tr>.  Every
 * assertion lives in the Java test.
 *
 *   Usage: node row-actions-run.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node row-actions-run.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, NS } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
const calls = [];
env.setFetch(function (url, opts) { calls.push({ url: url, method: opts.method, body: JSON.parse(opts.body) }); return Promise.resolve(jsonResponse({}, { status: 200 })); });

const DELETE = { id: 'delete', label: 'Delete', endpoint: '/x/{id}', method: 'DELETE', present: 'dialog',
	confirm: 'Delete {name}?', confirmLabel: 'Delete it', tone: 'danger', confirmRenderer: 'verdict' };

function fixture(rowData) {
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-1';
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	tr.dataset.juneauRowId = String(rowData.id);
	const btn = env.el('button');
	tr.appendChild(btn);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(table);
	table.__juneauCtx = { viewDef: { id: 'v', rowActions: [DELETE] }, dataTable: { row: function () { return { data: function () { return rowData; } }; } } };
	const closest = btn.closest;
	btn.closest = function (sel) { return sel === 'tbody tr' ? tr : closest.call(this, sel); };
	return btn;
}

const tick = function () { return new Promise(function (r) { setTimeout(r, 5); }); };
let verdict = true;
const asked = [];
NS.rowActions.registerConfirmRenderer('verdict', function (modal) { asked.push({ title: modal.title, confirmLabel: modal.confirmLabel, tone: modal.tone }); return Promise.resolve(verdict); });

(async function () {
	const btn = fixture({ id: 7, name: 'Widget' });

	verdict = true;
	await NS.rowActions.run(btn, 'delete');
	await tick();
	out.confirmed = { fetches: calls.length, url: calls[0]?.url, method: calls[0]?.method, action: calls[0]?.body.action, targetId: calls[0]?.body.targetId };

	verdict = false;
	await NS.rowActions.run(btn, 'delete');
	await tick();
	out.cancelled = { fetches: calls.length };
	out.asked = asked;

	const blank = fixture({ id: 8, name: '' });
	const before = asked.length;
	await NS.rowActions.run(blank, 'delete');
	await tick();
	out.emptyToken = { asked: asked.length - before, fetches: calls.length };

	out.unknownAction = await NS.rowActions.run(btn, 'nope').then(function () { return 'resolved'; }, function (e) { return e.message; });
	out.outsideRow = await NS.rowActions.run(env.el('div'), 'delete').then(function () { return 'resolved'; }, function (e) { return e.message; });

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
