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
 * copy-link.cjs - always-on Node harness for the Copy-link toolbar button (framework parity): isCopyLinkVisible's
 * visibility rule across one/several/explicit-primary tables, mountCopyLinkButton's idempotency, and that its
 * click handler is wired through NS.init.copyShareableUrl (not a closed-over reference).
 *
 *   Usage:  node copy-link.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [rendersJsPath, viewsJsPath] = process.argv.slice(2);
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node copy-link.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {};

const NS = env.window.JuneauViews;

// --- Scenario: with no URL-state module there is nothing to copy, so NO button is rendered ---------------------
(function noUrlStateNoButton() {
	const wrapper = env.el('div');
	env.body.appendChild(wrapper);
	const table = env.el('table');
	table.dataset.juneauView = 'nourl';
	wrapper.appendChild(table);
	const saved = NS.urlState;
	NS.urlState = undefined;
	I.mountCopyLinkButton(table, { viewDef: { primary: true } });
	NS.urlState = saved;
	out.noUrlStateNoButton = wrapper.querySelectorAll('.juneau-view-copylink-btn').length === 0;
	env.body.textContent = '';
})();

// The remaining scenarios mount the button, which needs the URL-state module; stub the two functions it uses.
NS.urlState = NS.urlState || { buildShareUrl: function () { return 'http://x/'; /* NOSONAR javascript:S5332 -- fixture share URL, never fetched */ }, copy: function () { return Promise.resolve(true); } };

function makeTable(id) {
	const t = env.el('table');
	t.dataset.juneauView = id;
	env.body.appendChild(t);
	return t;
}

// --- Scenario: unset primary + exactly one table on the page -> visible -------------------------------------
(function singleTableUnsetPrimary() {
	const table = makeTable('only');
	out.singleTableUnsetPrimary = I.isCopyLinkVisible(table, {});
})();

// --- Scenario: unset primary + several tables, none marked -> NOT "first wins", NO button on any -------------
(function severalTablesNonePrimary() {
	env.body.textContent = '';
	const a = makeTable('a');
	const b = makeTable('b');
	out.severalTablesNeitherVisible = I.isCopyLinkVisible(a, {}) === false && I.isCopyLinkVisible(b, {}) === false;
})();

// --- Scenario: explicit primary:true wins even with several tables on the page --------------------------------
(function explicitPrimaryWithSeveralTables() {
	env.body.textContent = '';
	const a = makeTable('a');
	makeTable('b');
	out.explicitPrimaryVisibleAmongSeveral = I.isCopyLinkVisible(a, { primary: true }) === true;
})();

// --- Scenario: explicit primary:false opts out even when it is the only table ----------------------------------
(function explicitPrimaryFalse() {
	env.body.textContent = '';
	const table = makeTable('only');
	out.primaryFalseHidesIt = I.isCopyLinkVisible(table, { primary: false }) === false;
})();

// --- Scenario: copyLink:false opts out a primary view that would otherwise show the button ---------------------
(function copyLinkOptOut() {
	env.body.textContent = '';
	const table = makeTable('only');
	out.copyLinkFalseHidesIt = I.isCopyLinkVisible(table, { primary: true, copyLink: false }) === false;
})();

// --- Scenario: mounting twice (first init + an Apply rebuild) never duplicates the button ----------------------
(function mountIsIdempotent() {
	env.body.textContent = '';
	const wrapper = env.el('div');
	env.body.appendChild(wrapper);
	const table = makeTable('only');
	wrapper.appendChild(table);
	const ctx = { viewDef: {} };
	I.mountCopyLinkButton(table, ctx);
	I.mountCopyLinkButton(table, ctx);
	out.mountedButtonCount = wrapper.querySelectorAll('.juneau-view-copylink-btn').length;
})();

// --- Scenario: the click handler calls NS.init.copyShareableUrl (the exported reference), not a closure --------
(function clickCallsExportedCopyShareableUrl() {
	env.body.textContent = '';
	const wrapper = env.el('div');
	env.body.appendChild(wrapper);
	const table = makeTable('only');
	wrapper.appendChild(table);
	const ctx = { viewDef: {} };
	I.mountCopyLinkButton(table, ctx);
	const btn = wrapper.querySelector('.juneau-view-copylink-btn');
	const realCopyShareableUrl = I.copyShareableUrl;
	let capturedArgs = null;
	I.copyShareableUrl = function (t, c) { capturedArgs = { t: t, c: c }; return Promise.resolve({ ok: true, url: 'x' }); };
	btn.dispatch('click', {});
	I.copyShareableUrl = realCopyShareableUrl;
	out.clickCalledExportedFnWithTableAndCtx = capturedArgs?.t === table && capturedArgs.c === ctx;
})();

// --- Scenario: a rejected copy announces the failure instead of leaving an unhandled rejection -----------------
(async function rejectedCopyAnnouncesFailure() {
	env.body.textContent = '';
	const wrapper = env.el('div');
	env.body.appendChild(wrapper);
	const table = makeTable('only');
	wrapper.appendChild(table);
	const ctx = { viewDef: {} };
	I.mountCopyLinkButton(table, ctx);
	const btn = wrapper.querySelector('.juneau-view-copylink-btn');
	const real = I.copyShareableUrl;
	I.copyShareableUrl = function () { return Promise.reject(new Error('denied')); };
	let unhandled = false;
	process.on('unhandledRejection', function () { unhandled = true; });
	btn.dispatch('click', {});
	I.copyShareableUrl = real;
	await new Promise(function (r) { setTimeout(r, 20); });
	const live = env.document.querySelector('.juneau-view-announcer');
	out.rejectedCopyHandled = !unhandled;
	out.rejectedCopyAnnounced = !!live && /Could not copy link/.test(live.textContent);
	process.stdout.write(JSON.stringify(out, null, 1));
})();
