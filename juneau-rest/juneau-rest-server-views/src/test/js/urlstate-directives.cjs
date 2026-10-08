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
 * urlstate-directives.cjs - always-on Node harness for the ?state= directive registry and its two built-ins
 * (window, ids), plus the live wire: registration from a view's urlState config, request params, setDirective,
 * and popstate re-apply.  Every assertion lives in the Java test.
 *
 *   Usage: node urlstate-directives.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [rendersJsPath, viewsJsPath, urlStateJsPath] = process.argv.slice(2);
if (!rendersJsPath || !viewsJsPath || !urlStateJsPath) {
	console.error('usage: node urlstate-directives.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>');
	process.exit(2);
}

// Captured so a logged E-JS code can be reported; the harness's own stdout stays the single JSON report.
const logged = [];
const sandboxConsole = { error: function (m) { logged.push(String(m)); }, warn: function () {}, log: function () {} };

const { env, I, NS } = loadViews(rendersJsPath, viewsJsPath);
// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
vm.runInNewContext( // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	fs.readFileSync(path.resolve(urlStateJsPath), 'utf8'),
	{ window: env.window, console: sandboxConsole },
	{ filename: 'juneau-urlstate.js' }
);

const U = NS.urlState;
const out = {};
const code = function (re) { const m = logged.find(function (l) { return re.test(l); }); return m ? re.exec(m)[0] : null; };

// ---- registry --------------------------------------------------------------------------------------------------
const noop = { parse: function (b) { return b; }, serialize: function (v) { return v; } };
out.registerReserved = U.registerDirective('tab', noop) === false && code(/E-JS-67/) === 'E-JS-67';
out.registerBadName = U.registerDirective('Bad-Name', noop) === false;
out.registerOk = U.registerDirective('probe', noop) === true;
out.registerDuplicate = U.registerDirective('probe', noop) === false;
out.extRoundTrip = U.decode(U.encode({ tab: 't', filters: [], sort: null, ext: { probe: 'abc' } })).ext.probe;
out.encoded = U.encode({ ext: { probe: 'abc' } });
out.stillDroppedUnregistered = U.decode('nope(x)').ext.nope ?? null;
out.notEmptyBecauseOfExtOnly = U.isEmptyState({ tab: null, filters: [], sort: null, ext: { probe: 'abc' } });
out.emptyWithNoExt = U.isEmptyState({ tab: null, filters: [], sort: null, ext: {} });
out.emptyWithBlankExt = U.isEmptyState({ ext: { probe: '' } });

logged.length = 0;
U.registerDirective('boom', { parse: function () { throw new Error('bad'); }, serialize: String });
const boom = U.decode('tab(a);boom(x)');
out.throwingCodecLogged = code(/E-JS-68/);
out.throwingCodecValue = boom.ext.boom ?? null;
out.throwingCodecKeepsTab = boom.tab;

out.encodeArgSample = U.encodeArg('a;b(c)d,e=f%');

// ---- window ----------------------------------------------------------------------------------------------------
U.registerDirective('window', U.builtins.window({ params: { start: 'from', end: 'to' } }));
const w = U.decode(U.encode({ ext: { window: { start: '2026-10-01T00:00:00Z', end: '2026-10-02T00:00:00Z' } } })).ext.window;
out.windowStart = w.start;
out.windowEnd = w.end;
const wp = U.directiveCodec('window').toParams(w);
out.windowParamFrom = wp.from;
out.windowParamTo = wp.to;
out.windowOpenEnded = JSON.stringify(U.directiveCodec('window').toParams({ start: 'x' }));

// ---- ids -------------------------------------------------------------------------------------------------------
U.registerDirective('ids', U.builtins.ids({ max: 2, param: 'idList' }));
const ids = U.decode('ids( a , b ,a, ,c)').ext.ids;
out.idsNormalized = ids.ids.join(',');
out.idsCappedAtMax = ids.truncated === true && ids.ids.length === 2;
out.idsCapMessage = U.directiveCodec('ids').statusMessage(ids);
out.idsNoMessageWhenNotCapped = U.directiveCodec('ids').statusMessage(U.decode('ids(a,b,a)').ext.ids);
const once = U.encode({ ext: { ids: ids } });
out.idsIdempotent = U.encode({ ext: { ids: U.decode(once).ext.ids } }) === once;
out.idsParam = JSON.stringify(U.directiveCodec('ids').toParams(ids));
const tricky = { ids: ['a,b', '100%;'], truncated: false };
const trickyBack = U.decode(U.encode({ ext: { ids: tricky } })).ext.ids;
out.idsSpecialCharsRoundTrip = JSON.stringify(trickyBack.ids) === JSON.stringify(tricky.ids);

// ---- live wire -------------------------------------------------------------------------------------------------
const window = env.window;
const histCalls = [];
window.history = { state: null, replaceState: function (s, t, url) { histCalls.push(url); } };
window.location = { pathname: '/rest/x', search: '?state=ids(p,q)', hash: '', origin: 'https://h' };
const listeners = {};
window.addEventListener = function (ev, fn) { (listeners[ev] = listeners[ev] || []).push(fn); };

// A fresh registry per scenario is impossible (page-global), so the live view reuses the registrations above by
// naming no builtins: prepareUrlStateDirectives only registers a name that is not yet registered.
const table = env.el('table');
table.dataset.juneauView = 'x';
env.document.body.appendChild(table);
let reloads = 0;
const dt = {
	columns: function () { return { every: function () {} }; },
	order: function () { return []; },
	draw: function () { return this; },
	on: function () { return this; },
	ajax: { reload: function () { reloads++; } }
};
const ctx = { table: table, viewDef: { id: 'x', urlState: { window: {}, ids: {} } }, dataTable: dt, optsColumns: [] };
table.__juneauCtx = ctx;

I.prepareUrlStateDirectives(table, ctx.viewDef, ctx);
out.seededFromUrl = JSON.stringify(ctx.urlExt.ids?.ids);
out.requestParamsSeeded = JSON.stringify(I.urlStateRequestParams(ctx));

I.wireShareableUrlState(table, ctx);
out.popstateListener = (listeners.popstate || []).length;

reloads = 0;
histCalls.length = 0;
const ok = NS.urlState.setDirective(table, 'window', { start: '2026-10-01T00:00:00Z', end: '2026-10-02T00:00:00Z' });
out.setDirectiveOk = ok;
out.setDirectiveReloaded = reloads;
out.addressBar = decodeURIComponent(String(histCalls.at(-1) ?? ''));
out.requestParamsAfterSet = JSON.stringify(I.urlStateRequestParams(ctx));

NS.urlState.setDirective(table, 'window', null);
out.requestParamsAfterClear = JSON.stringify(I.urlStateRequestParams(ctx));
out.addressBarAfterClear = decodeURIComponent(String(histCalls.at(-1) ?? ''));

logged.length = 0;
out.setDirectiveUnknown = NS.urlState.setDirective(table, 'nothere', 1) === false;

window.location.search = '?state=ids(z)';
reloads = 0;
(listeners.popstate || []).forEach(function (fn) { fn({}); });
out.popstateIds = JSON.stringify(ctx.urlExt.ids?.ids);
out.popstateReloaded = reloads;

process.stdout.write(JSON.stringify(out));
