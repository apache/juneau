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
 * urlstate-builtin-config.cjs - always-on Node harness proving that the window and ids built-ins are configured from
 * a view's own urlState entry.  Nothing is registered by hand: prepareUrlStateDirectives is the only thing that
 * registers window and ids, so a regression that hands the factories {} or undefined changes the live request
 * parameter names and the ids cap.  The registry is page-global, hence this scenario owns a fresh sandbox.  Every
 * assertion lives in the Java test.
 *
 *   Usage: URLSTATE_VIEW_JSON='{"ids":{...},"window":{...}}' node urlstate-builtin-config.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [rendersJsPath, viewsJsPath, urlStateJsPath] = process.argv.slice(2);
if (!rendersJsPath || !viewsJsPath || !urlStateJsPath || !process.env.URLSTATE_VIEW_JSON) {
	console.error('usage: URLSTATE_VIEW_JSON=<json> node urlstate-builtin-config.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>');
	process.exit(2);
}

const { env, I, NS } = loadViews(rendersJsPath, viewsJsPath);
// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
vm.runInNewContext( // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	fs.readFileSync(path.resolve(urlStateJsPath), 'utf8'),
	{ window: env.window, console: { error: function () {}, warn: function () {}, log: function () {} } },
	{ filename: 'juneau-urlstate.js' }
);

const U = NS.urlState;
const out = {};

const window = env.window;
window.history = { state: null, replaceState: function () {} };
window.location = {
	pathname: '/rest/x', hash: '', origin: 'https://h',
	search: '?state=window(start=2026-10-01T00:00:00Z;end=2026-10-02T00:00:00Z);ids(a,b,c)'
};
window.addEventListener = function () {};

const table = env.el('table');
table.dataset.juneauView = 'x';
env.document.body.appendChild(table);
const dt = {
	columns: function () { return { every: function () {} }; },
	order: function () { return []; },
	draw: function () { return this; },
	on: function () { return this; },
	ajax: { reload: function () {} }
};
const ctx = { table: table, viewDef: { id: 'x', urlState: JSON.parse(process.env.URLSTATE_VIEW_JSON) }, dataTable: dt, optsColumns: [] };
table.__juneauCtx = ctx;

out.registeredBefore = [U.hasDirective('window'), U.hasDirective('ids')].join('|');
I.prepareUrlStateDirectives(table, ctx.viewDef, ctx);
out.registeredAfter = [U.hasDirective('window'), U.hasDirective('ids')].join('|');

// The request carries the configured parameter names, and the ids cap is the configured max.
out.requestParams = JSON.stringify(I.urlStateRequestParams(ctx));
out.seededIds = JSON.stringify(ctx.urlExt.ids?.ids);
out.idsTruncated = ctx.urlExt.ids?.truncated === true;
out.idsCapMessage = U.directiveCodec('ids').statusMessage(ctx.urlExt.ids);
out.windowStart = ctx.urlExt.window?.start;
out.windowEnd = ctx.urlExt.window?.end;

process.stdout.write(JSON.stringify(out));
