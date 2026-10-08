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
 * hooks-api.cjs - always-on Node harness for the sanctioned hook namespaces
 * (JuneauViews.rowActions/dialogs/probeSelection/tables/status/csrf/details/html): proves each namespace exists,
 * is frozen, exposes the right member shapes, and that the pure helpers behave.  DOM/browser-free.
 *
 *   Usage:  node hooks-api.cjs <path-to-juneau-views.js> [path-to-juneau-renders.js]
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node hooks-api.cjs <juneau-views.js> [juneau-renders.js]');
	process.exit(2);
}
const rendersJsPath = process.argv[3];

const document = {
	readyState: 'loading',
	addEventListener: function () { /* no-op */ },
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () { return {}; },
	body: { appendChild: function () { /* no-op */ }, querySelectorAll: function () { return []; } }
};
const window = { document: document, console: console };
const sandbox = { window: window, document: document, console: console };
sandbox.globalThis = sandbox;

// NOSONAR javascript:S1523 -- the harness's purpose is to load the production runtime under test (a repo-local path
// from argv) into an isolated VM sandbox.
if (rendersJsPath)
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const out = {};

function shape(ns, members) {
	if (!ns) return { exists: false };
	const r = { exists: true, frozen: Object.isFrozen(ns), members: {} };
	for (const m of members) r.members[m] = typeof ns[m];
	return r;
}

out.hasNamespace = !!NS;
out.rowActions = shape(NS?.rowActions, ['contextOf', 'find', 'isDialog', 'run', 'open', 'submit']);
out.dialogs = shape(NS?.dialogs, ['open']);
out.probeSelection = shape(NS?.probeSelection, ['enhance', 'initAll']);
out.tables = shape(NS?.tables, ['columnIndex', 'ready', 'handle', 'reload']);
out.status = shape(NS?.status, ['render']);
out.csrf = shape(NS?.csrf, ['token', 'headerName', 'headers']);
out.details = shape(NS?.details, ['fillSlots']);
out.html = shape(NS?.html, ['esc', 'escAttr']);
out.htmlEscOutput = typeof NS?.html?.esc === 'function' ? NS.html.esc('<a>&"\'') : null;

// csrf.headers against a fake table element (no owning view table, so the element itself is read).
function fakeTable(attrs, dataset) {
	return { getAttribute: function (k) { return Object.hasOwn(attrs, k) ? attrs[k] : null; }, dataset: dataset || {} };
}
out.csrfHeadersWithToken = NS.csrf.headers(fakeTable({ 'data-juneau-csrf': 'tok' }));
out.csrfHeadersCustomName = NS.csrf.headers(fakeTable({ 'data-juneau-csrf': 'tok' }, { juneauCsrfHeader: 'X-Mine' }));
out.csrfHeadersBlank = NS.csrf.headers(fakeTable({ 'data-juneau-csrf': '   ' }));
out.csrfHeadersNoEl = NS.csrf.headers(null);
out.csrfToken = NS.csrf.token(fakeTable({ 'data-juneau-csrf': 'tok' }));

// tables.handle / reload against a fake table carrying a __juneauCtx.
let reloadArgs = null;
const dt = { row: function () { return { data: function () { return { id: 7 }; } }; }, ajax: { reload: function (a, b) { reloadArgs = [a, b]; } } };
const tbl = { __juneauCtx: { dataTable: dt, viewDef: { id: 'v' } } };
const h = NS.tables.handle(tbl);
out.handleViewId = h?.viewDef?.id;
out.handleRow = h?.contextOf({}).row;
out.handleNull = NS.tables.handle({}) === null;
NS.tables.reload(tbl);
out.reloadDefault = reloadArgs;
NS.tables.reload(tbl, { resetPaging: true });
out.reloadReset = reloadArgs;
out.contextOfNull = NS.rowActions.contextOf(null) === null;

process.stdout.write(JSON.stringify(out));
