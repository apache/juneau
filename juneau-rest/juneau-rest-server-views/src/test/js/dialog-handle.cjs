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
 * dialog-handle.cjs - always-on Node harness for the DialogHandle returned by JuneauViews.dialogs.open: steps with
 * Back/Next, setStep, setConfirmLabel/setCancelLabel, onConfirm gating the submit (only an exact true proceeds),
 * onCancel, close() popping only its own layer, the chrome registry, and the depth cap.  A dialog opened through
 * dialogs.open has no action, so a proceeding confirm is observed as the dialog closing with `closed` resolving to
 * "submitted".  Every assertion lives in the Java test.
 *
 *   Usage: node dialog-handle.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-handle.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, NS } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
const fetches = [];
env.setFetch(function (url, opts) { fetches.push({ url: url, method: opts.method, body: JSON.parse(opts.body) }); return Promise.resolve(jsonResponse({}, { status: 200 })); });

const table = env.el('table');
table.dataset.juneauView = 'v';
table.dataset.juneauCsrf = 'tok';
const tr = env.el('tr');
table.appendChild(tr);
env.body.appendChild(table);
const rc = { table: table, tr: tr };

const backdrops = function () { return env.document.querySelectorAll('.juneau-view-dialog-backdrop').length; };
const within = function (h, sel) { return h.dialog.querySelector(sel); };
const tick = function () { return new Promise(function (r) { setImmediate(r); }); };
const WIZARD = {
	title: 'Wizard',
	steps: [
		{ id: 'edit', title: 'Edit', fields: [{ label: 'A', value: '1' }], confirmLabel: 'Next' },
		{ id: 'review', title: 'Review', fields: [{ label: 'A', value: '1' }], confirmLabel: 'Apply' }
	]
};

(async function () {
	out.handleSurface = (function () {
		const h = NS.dialogs.open({ title: 'x' }, rc);
		const keys = ['fields', 'body', 'step', 'setStep', 'setConfirmLabel', 'setCancelLabel', 'onConfirm', 'onCancel', 'submit', 'close'];
		const ok = keys.every(function (k) { return typeof h[k] === 'function'; }) && typeof h.closed?.then === 'function' && !!h.dialog;
		h.close();
		return ok;
	})();

	// Steps: Next advances, Back retreats, the last step's button submits.
	{
		const h = NS.dialogs.open(WIZARD, rc);
		const w = { step0: h.step(), label0: within(h, '.juneau-view-dialog-confirm').textContent, backHidden0: within(h, '.juneau-view-dialog-back').hidden };
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		w.step1 = h.step();
		w.label1 = within(h, '.juneau-view-dialog-confirm').textContent;
		w.title1 = within(h, '.juneau-view-dialog-title').textContent;
		w.backShown1 = !within(h, '.juneau-view-dialog-back').hidden;
		w.openAfterNext = backdrops();
		within(h, '.juneau-view-dialog-back').dispatch('click');
		w.stepBack = h.step();
		h.setStep('nope');
		w.stepAfterUnknown = h.step();
		h.setStep('review');
		w.stepAfterSet = h.step();
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		w.closed = await h.closed;
		w.openAfterSubmit = backdrops();
		out.wizard = w;
	}

	// Relabeling and onConfirm gating.
	{
		const h = NS.dialogs.open({ title: 'Gate' }, rc);
		h.setConfirmLabel('Review changes');
		h.setCancelLabel('Back out');
		const g = { confirmLabel: within(h, '.juneau-view-dialog-confirm').textContent, cancelLabel: within(h, '.juneau-view-dialog-cancel').textContent };
		let calls = 0;
		let verdict = false;
		h.onConfirm(function (fields) { calls++; g.fieldsIsObject = typeof fields === 'object'; return verdict; });
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		await tick();
		g.openAfterFalse = backdrops();
		verdict = Promise.resolve(false);
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		await tick();
		g.openAfterPromiseFalse = backdrops();
		verdict = 'yes';
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		await tick();
		g.openAfterTruthyNotTrue = backdrops();
		verdict = Promise.resolve(true);
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		g.closed = await h.closed;
		g.calls = calls;
		g.openAfterTrue = backdrops();
		out.gate = g;
	}

	// onCancel and close() pop only their own layer.
	{
		let cancelled = 0;
		const h1 = NS.dialogs.open({ title: 'one' }, rc);
		h1.onCancel(function () { cancelled++; });
		const h2 = NS.dialogs.open({ title: 'two' }, rc);
		const c = { depthTwo: NS.dialogs.depth(), third: NS.dialogs.open({ title: 'three' }, rc) };
		c.thirdRefused = c.third === null;
		h2.close();
		c.depthAfterInnerClose = NS.dialogs.depth();
		c.outerStillOpen = backdrops();
		c.innerClosed = await h2.closed;
		within(h1, '.juneau-view-dialog-cancel').dispatch('click');
		c.outerClosed = await h1.closed;
		c.onCancelRan = cancelled;
		c.depthEnd = NS.dialogs.depth();
		out.layers = c;
	}

	// Chrome registry.
	{
		const seen = [];
		NS.dialogs.registerChrome('plain', function (parts, modal) { seen.push({ keys: Object.keys(parts).sort(), chrome: modal.chrome }); });
		const before = NS.dialogs.depth();
		const unknown = NS.dialogs.open({ title: 'x', chrome: 'missing' }, rc);
		const h = NS.dialogs.open({ title: 'x', chrome: 'plain' }, rc);
		out.chrome = { unknownReturnsNull: unknown === null, depthUnchangedByUnknown: before === 0, seen: seen, opened: !!h };
		h.close();
	}

	// opts.action names what a confirming dialog submits: a 2-step flow sends nothing at Next and exactly one write at the end.
	{
		const action = { id: 'apply', label: 'Apply', endpoint: '/x/apply', method: 'POST' };
		const h = NS.dialogs.open(WIZARD, rc, { action: action });
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		const afterNext = fetches.length;
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		await h.closed;
		out.submit = { afterNext: afterNext, total: fetches.length, url: fetches[0]?.url, action: fetches[0]?.body.action };
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { console.error(e.stack || String(e)); process.exit(1); });
