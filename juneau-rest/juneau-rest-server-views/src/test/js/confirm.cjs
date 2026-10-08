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
 * confirm.cjs - always-on Node harness for JuneauViews.dialogs.confirm: a registered renderer decides the answer,
 * an unregistered renderer name rejects with E-JS-71, and the built-in modal lists/caps items, carries confirmLabel
 * and tone, focuses Cancel first for a danger confirm, runs a function `action` only on confirm, and resolves false on
 * cancel.  Every assertion lives in the Java test.
 *
 *   Usage: node confirm.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node confirm.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, NS } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
const seen = [];

// Silence the logged E-JS-71 so the report stays the only stdout; stderr is not parsed.
NS.rowActions.registerConfirmRenderer('stub-ok', function (modal, request) {
	seen.push({ title: modal.title, tone: modal.tone, confirmLabel: modal.confirmLabel, items: modal.items.length, sameRequest: request.title === modal.title });
	return true;
});
NS.rowActions.registerConfirmRenderer('stub-cancel', function () { return Promise.resolve(false); });
NS.rowActions.registerConfirmRenderer('stub-ok', function () { return false; }); // duplicate: the first registration stays

const q = function (sel) { return env.document.querySelector(sel); };
const tick = function () { return new Promise(function (r) { setImmediate(r); }); };

(async function () {
	out.surface = ['confirm', 'registerChrome', 'depth'].every(function (k) { return typeof NS.dialogs[k] === 'function'; })
		&& typeof NS.rowActions.registerConfirmRenderer === 'function';

	out.stubOk = await NS.dialogs.confirm({ title: 'T', items: ['a', 'b'], tone: 'danger', confirmLabel: 'Go', renderer: 'stub-ok' });
	out.stubCancel = await NS.dialogs.confirm({ title: 'T', renderer: 'stub-cancel' });
	out.duplicateKeptFirst = out.stubOk === true;
	out.rendererSaw = seen;
	out.unregistered = await NS.dialogs.confirm({ title: 'T', renderer: 'nope' }).then(function () { return 'resolved'; }, function (e) { return e.message; });
	out.actionObjectRenderer = await NS.dialogs.confirm({ title: 'T', action: { confirmRenderer: 'stub-ok' } });

	// Built-in modal: capped list, label, danger class, Cancel focused first, action callback on confirm.
	let ran = 0;
	const items = Array.from({ length: 25 }, function (_, i) { return 'item ' + i; });
	const p1 = NS.dialogs.confirm({ title: 'Delete?', body: 'Sure?', items: items, confirmLabel: 'Delete', tone: 'danger', renderer: 'list', action: function () { ran++; } });
	out.builtIn = {
		shown: !!q('.juneau-view-confirm-modal'),
		listItems: q('.juneau-view-confirm-list').childNodes.length,
		lastItem: q('.juneau-view-confirm-list').childNodes.at(-1).textContent,
		confirmText: q('.juneau-view-dialog-confirm').textContent,
		dangerClass: q('.juneau-view-dialog-confirm').className.includes('juneau-view-dialog-confirm-danger'),
		cancelFocused: env.getActive() === q('.juneau-view-dialog-cancel'),
		depthWhileOpen: NS.dialogs.depth()
	};
	q('.juneau-view-dialog-confirm').dispatch('click');
	out.builtIn.resolved = await p1;
	out.builtIn.actionRuns = ran;
	out.builtIn.goneAfter = !q('.juneau-view-confirm-modal');

	const p2 = NS.dialogs.confirm({ title: 'Plain', action: function () { ran++; } });
	out.defaultTonePlain = !q('.juneau-view-dialog-confirm').className.includes('danger');
	q('.juneau-view-dialog-cancel').dispatch('click');
	out.builtIn.cancelResolved = await p2;
	out.builtIn.actionRunsAfterCancel = ran;
	await tick();
	out.depthAfter = NS.dialogs.depth();

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
