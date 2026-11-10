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
 * urlstate-pagesource.cjs - always-on Node harness for page-level ?state= sources: registerPageSource, syncPage and
 * currentDirective.  Every assertion lives in the Java test.
 *
 *   Usage: node urlstate-pagesource.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [rendersJsPath, viewsJsPath, urlStateJsPath] = process.argv.slice(2);
if (!rendersJsPath || !viewsJsPath || !urlStateJsPath) {
	console.error('usage: node urlstate-pagesource.cjs <juneau-renders.js> <juneau-views.js> <juneau-urlstate.js>');
	process.exit(2);
}

const logged = [];
const sandboxConsole = { error: function (m) { logged.push(String(m)); }, warn: function () {}, log: function () {} };

const { env, NS } = loadViews(rendersJsPath, viewsJsPath);
vm.runInNewContext( // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	fs.readFileSync(path.resolve(urlStateJsPath), 'utf8'),
	{ window: env.window, console: sandboxConsole },
	{ filename: 'juneau-urlstate.js' }
);

const U = NS.urlState;
const window = env.window;
const out = {};
const histCalls = [];
window.history = { state: null, replaceState: function (s, t, url) { histCalls.push(url); } };
window.location = { pathname: '/rest/x', search: '', hash: '', origin: 'https://h' };
const lastUrl = function () { return decodeURIComponent(String(histCalls.at(-1) ?? '')); };

out.hasPageApi = typeof U.registerPageSource === 'function' && typeof U.syncPage === 'function' && typeof U.currentDirective === 'function';
if (!out.hasPageApi) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const plain = { parse: function (b) { return b; }, serialize: function (v) { return v; } };
U.registerDirective('zz', plain);
out.sourceRegistered = U.registerPageSource('zz', function () { return 'hello'; }) === true;
out.dupRejected = U.registerPageSource('zz', function () { return 'x'; }) === false && logged.some(function (l) { return /E-JS-67/.test(l); });
out.unregisteredNameRejected = U.registerPageSource('nope', function () { return 'x'; }) === false;

window.location.search = '?other=1';
out.syncReturned = U.syncPage();
out.written = lastUrl();
out.keepsOthers = /other=1/.test(out.written);

// empty value drops the directive
U.registerDirective('yy', plain);
U.registerPageSource('yy', function () { return ''; });
window.location.search = '?state=yy(old)';
U.syncPage();
out.emptyDropped = !/yy\(/.test(lastUrl());

// decode keeps '~' and ':' in a directive body (a stand-in parser here; the real sw parser is exercised by Switcher_Test)
U.registerDirective('sw', { parse: function (b) { const o = {}; b.split('~').forEach(function (p) { const i = p.indexOf(':'); o[p.slice(0, i)] = p.slice(i + 1); }); return o; }, serialize: function (v) { return v; } });
out.tildeColon = JSON.stringify(U.decode('sw(a:b.c~d:e)').ext.sw);

// currentDirective
window.location.search = '?state=sw(a:v2~b:v3.s2)';
out.current = JSON.stringify(U.currentDirective('sw'));
window.location.search = '';
out.currentAbsent = U.currentDirective('sw') === undefined;
out.currentUnregistered = U.currentDirective('unknown') === undefined;

process.stdout.write(JSON.stringify(out));
