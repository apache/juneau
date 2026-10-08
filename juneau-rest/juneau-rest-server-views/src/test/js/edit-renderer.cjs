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
 * edit-renderer.cjs - always-on Node harness for the `edit` renderer and the inline cell editor it drives: the
 * renderer's markup, the pencil gates (visibleWhen hides, enabledWhen disables with the reason), Enter / blur commit,
 * Escape and an unchanged value cancelling without a request, a failed save keeping the editor open with the error
 * inline, a nested table's pencil not opening the parent's editor, and the commit's wire body.  Every assertion lives
 * in the Java test.
 *
 *   Usage: node edit-renderer.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node edit-renderer.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I, NS } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
const calls = [];
let respond = function () { return jsonResponse({ outcome: 'success' }, { status: 200 }); };
env.setFetch(function (url, opts) { calls.push({ url: url, method: opts.method, body: JSON.parse(opts.body) }); return Promise.resolve(respond()); });
const tick = function () { return new Promise(function (r) { setTimeout(r, 5); }); };

// ---- renderer markup ----------------------------------------------------------------------------------------------
const edit = NS.resolveRenderer('edit');
out.registered = !!edit;
const html = String(edit.display('a<b', {}, { field: 'assign', column: 'owner', input: 'select', options: 'x,y' }));
out.markup = {
	escaped: html.indexOf('a&lt;b') >= 0 && html.indexOf('a<b') < 0,
	action: html.indexOf('data-juneau-edit="assign"') >= 0,
	column: html.indexOf('data-juneau-edit-col="owner"') >= 0,
	input: html.indexOf('data-juneau-edit-input="select"') >= 0,
	options: html.indexOf('data-juneau-edit-options="x,y"') >= 0
};
out.noActionPlain = String(edit.display('v', {}, { column: 'c' })).indexOf('<button') < 0;
out.hostileActionEscaped = String(edit.display('v', {}, { action: '"><img>', column: 'c' })).indexOf('"><img>') < 0;
out.wrapClass = edit['class']({}, {}, {}).indexOf('juneau-cell-wrap') >= 0;

// ---- fixture ------------------------------------------------------------------------------------------------------
const ASSIGN = { id: 'assign', label: 'Assign', endpoint: '/x/{id}', method: 'POST', onSuccess: 'mergeRow' };

function fixture(rowData, action, opts) {
	opts = opts || {};
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-1';
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	tr.dataset.juneauRowId = String(rowData.id);
	const td = env.el('td');
	const cell = env.el('span');
	cell.className = 'juneau-edit-cell';
	const value = env.el('span');
	value.className = 'juneau-edit-value';
	value.textContent = String(rowData.owner);
	const pencil = env.el('button');
	pencil.className = 'juneau-edit-pencil';
	pencil.setAttribute('data-juneau-edit', 'assign');
	pencil.setAttribute('data-juneau-edit-col', 'owner');
	pencil.setAttribute('data-juneau-edit-input', opts.input || 'text');
	if (opts.options) pencil.setAttribute('data-juneau-edit-options', opts.options);
	cell.appendChild(value);
	cell.appendChild(pencil);
	td.appendChild(cell);
	tr.appendChild(td);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(table);
	const reloads = [];
	const viewDef = { id: 'v', rowActions: action ? [action] : [] };
	const ctx = { viewDef: viewDef, dataTable: {
		row: function () { return { data: function () { return rowData; } }; },
		ajax: { reload: function () { reloads.push(1); } }
	}, redraw: function () { reloads.push('redraw'); } };
	table.__juneauCtx = ctx;
	const origClosest = pencil.closest;
	pencil.closest = function (sel) { return sel === 'td' ? td : origClosest.call(this, sel); };
	I.initInlineEdit(table, ctx, viewDef);
	return { table: table, tr: tr, td: td, pencil: pencil, cell: cell, value: value, reloads: reloads, viewDef: viewDef };
}
const editorOf = function (f) { return f.td.childNodes.length === 1 && f.td.childNodes[0].className === 'juneau-edit-editor' ? f.td.childNodes[0] : null; };
const inputOf = function (f) { const e = editorOf(f); return e ? e.childNodes[0] : null; };
const click = function (f) { f.table.dispatch('click', { target: f.pencil, preventDefault: function () {}, stopPropagation: function () {} }); };
const key = function (input, k) { input.dispatch('keydown', { key: k, preventDefault: function () {}, stopPropagation: function () {} }); };

(async function () {
	// gates
	const gated = fixture({ id: 1, owner: 'a' }, null);
	I.applyInlineEditGates?.(gated.tr, { id: 1 }, gated.viewDef);
	out.gateExposed = typeof I.applyInlineEditGates === 'function';
	if (out.gateExposed) {
		out.unknownActionHides = gated.pencil.hidden === true;
		const hid = fixture({ id: 2, owner: 'a', locked: true }, Object.assign({}, ASSIGN, { visibleWhen: [{ field: 'locked', op: 'eq', value: false }] }));
		I.applyInlineEditGates(hid.tr, { id: 2, locked: true }, hid.viewDef);
		out.visibleWhenHides = hid.pencil.hidden === true;
		const dis = fixture({ id: 3, owner: 'a', closed: true }, Object.assign({}, ASSIGN, { enabledWhen: [{ field: 'closed', op: 'eq', value: false, reason: 'Closed rows cannot be reassigned' }] }));
		I.applyInlineEditGates(dis.tr, { id: 3, closed: true }, dis.viewDef);
		out.enabledWhenDisables = { disabled: dis.pencil.disabled === true || dis.pencil.getAttribute('aria-disabled') === 'true', title: dis.pencil.getAttribute('title') };
		click(dis);
		out.disabledNoEditor = editorOf(dis) === null;
	}

	// open, prefill, Enter commits
	let f = fixture({ id: 7, owner: 'old' }, ASSIGN);
	click(f);
	out.opened = editorOf(f) !== null;
	out.prefill = inputOf(f)?.value;
	inputOf(f).value = 'new';
	calls.length = 0;
	key(inputOf(f), 'Enter');
	await tick();
	out.commit = { fetches: calls.length, url: calls[0]?.url, action: calls[0]?.body.action, targetId: calls[0]?.body.targetId, fields: calls[0]?.body.fields };
	out.closedAfterSuccess = editorOf(f) === null;
	out.shownValue = f.value.textContent;
	out.reloadedAfterSuccess = f.reloads.length;

	// Escape: no request
	f = fixture({ id: 8, owner: 'old' }, ASSIGN);
	click(f);
	inputOf(f).value = 'zzz';
	calls.length = 0;
	key(inputOf(f), 'Escape');
	await tick();
	out.escape = { fetches: calls.length, closed: editorOf(f) === null, restored: f.value.textContent, pencilBack: f.td.childNodes[0] === f.cell };

	// unchanged value: no request
	f = fixture({ id: 9, owner: 'same' }, ASSIGN);
	click(f);
	calls.length = 0;
	key(inputOf(f), 'Enter');
	await tick();
	out.unchanged = { fetches: calls.length, closed: editorOf(f) === null };

	// blur commits
	f = fixture({ id: 10, owner: 'old' }, ASSIGN);
	click(f);
	inputOf(f).value = 'blurred';
	calls.length = 0;
	inputOf(f).dispatch('blur', {});
	await tick();
	out.blurCommit = { fetches: calls.length, fields: calls[0]?.body.fields };

	// failure: editor stays, error inline, no blur re-submit
	respond = function () { return jsonResponse({ outcome: 'failure', message: 'Not allowed' }, { status: 200 }); };
	f = fixture({ id: 11, owner: 'old' }, ASSIGN);
	click(f);
	inputOf(f).value = 'bad';
	calls.length = 0;
	key(inputOf(f), 'Enter');
	await tick();
	const err = editorOf(f)?.childNodes[1];
	out.failure = { stillOpen: editorOf(f) !== null, error: err?.textContent, errorVisible: err?.hidden === false, role: err?.getAttribute('role'), enabled: inputOf(f)?.disabled === false, reloads: f.reloads.length };
	inputOf(f).dispatch('blur', {});
	await tick();
	out.failureBlurNoResubmit = calls.length;

	// refusal (HTTP error) also surfaces inline
	respond = function () { return jsonResponse({}, { status: 403 }); };
	f = fixture({ id: 12, owner: 'old' }, ASSIGN);
	click(f);
	inputOf(f).value = 'x';
	key(inputOf(f), 'Enter');
	await tick();
	out.refusal = { stillOpen: editorOf(f) !== null, hasMessage: (editorOf(f)?.childNodes[1]?.textContent || '').length > 0 };

	// select input
	respond = function () { return jsonResponse({ outcome: 'success' }, { status: 200 }); };
	f = fixture({ id: 13, owner: 'y' }, ASSIGN, { input: 'select', options: 'x,y,z' });
	click(f);
	const sel = inputOf(f);
	out.select = { tag: sel.tagName, options: sel.childNodes.map(function (o) { return o.value; }), value: sel.value };

	// a nested table's pencil must not open the parent's editor
	f = fixture({ id: 14, owner: 'old' }, ASSIGN);
	const nested = env.el('table');
	nested.dataset.juneauView = 'inner';
	nested.appendChild(f.pencil);
	f.table.dispatch('click', { target: f.pencil, preventDefault: function () {}, stopPropagation: function () {} });
	out.nestedIgnored = editorOf(f) === null;

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
