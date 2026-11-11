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
 * dialog-nav.cjs - always-on Node harness for the dialog frame's inner navigation: `data-nav` on `.juneau-view-dialog`
 * (pill by default, folder from ModalDef.nav, wizard from ModalDef.steps, an unknown nav silently pill), the wizard
 * step rail, per-step validation (Next checks only the current step; Finish checks every step and reveals the first
 * invalid one) and the single MAX_DIALOG_DEPTH slot a wizard consumes.  Every assertion lives in the Java test.
 *
 *   Usage: node dialog-nav.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-nav.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

// Count runtime warnings/errors: an unknown nav must fall back without either.
const logged = [];
const realWarn = console.warn;
const realError = console.error;
console.warn = function () { logged.push('warn:' + Array.from(arguments).join(' ')); };
console.error = function () { logged.push('error:' + Array.from(arguments).join(' ')); };

const { env, NS } = loadViews(rendersJsPath, viewsJsPath);
const out = {};
env.setFetch(function () { return Promise.resolve(jsonResponse({}, { status: 200 })); });

const table = env.el('table');
table.dataset.juneauView = 'v';
table.dataset.juneauCsrf = 'tok';
const tr = env.el('tr');
table.appendChild(tr);
env.body.appendChild(table);
const rc = { table: table, tr: tr };

const within = function (h, sel) { return h.dialog.querySelector(sel); };
const SECTIONED = { contractVersion: '3', sections: [
	{ id: 'a', label: 'A', fields: [{ name: 'one', label: 'One' }] },
	{ id: 'b', label: 'B', fields: [{ name: 'two', label: 'Two' }] }
] };
const navOf = function (modal) {
	const h = NS.dialogs.open(modal, rc);
	const r = { nav: h.dialog.dataset.nav, strip: !!within(h, '[data-testid="dialog-sections"]'), rail: !!within(h, '.juneau-view-dialog-steps') };
	h.close();
	return r;
};
const required = function (name) {
	const input = env.el('input');
	input.dataset.juneauFormField = name;
	input.required = true;
	return input;
};

(async function () {
	out.plain = navOf({ title: 'x' });
	out.pill = navOf({ title: 'x', nav: 'pill', form: SECTIONED });
	out.folder = navOf({ title: 'x', nav: 'folder', form: SECTIONED });
	logged.length = 0;
	out.unknown = navOf({ title: 'x', nav: 'tabs', form: SECTIONED });
	out.unknownLogged = logged.slice();
	out.wizardWinsOverNav = navOf({ title: 'x', nav: 'folder', steps: [{ id: 's1' }, { id: 's2' }] });

	// The rail: one numbered mark per step, the active one aria-current, completed ones a check mark.
	{
		const h = NS.dialogs.open({ title: 'W', steps: [{ id: 'one', title: 'Pick' }, { id: 'two' }, { id: 'three', title: 'Done' }] }, rc);
		const rail = within(h, '.juneau-view-dialog-steps');
		const items = function () { return rail.querySelectorAll('.juneau-view-dialog-step'); };
		const snap = function () {
			return items().map(function (li) {
				return { state: li.dataset.state, current: li.getAttribute('aria-current'),
					mark: li.querySelector('.juneau-view-dialog-step-mark').textContent,
					label: li.querySelector('.juneau-view-dialog-step-label').textContent };
			});
		};
		const r = { tag: rail.tagName, ariaLabel: rail.getAttribute('aria-label'), depth: NS.dialogs.depth(), at0: snap() };
		const kids = h.dialog.childNodes.map(function (c) { return c.className; });
		r.railAfterHeader = kids.indexOf('juneau-view-dialog-steps') === kids.indexOf('juneau-view-dialog-header') + 1;
		within(h, '.juneau-view-dialog-confirm').dispatch('click');
		r.at1 = snap();
		r.paneCount = h.dialog.querySelectorAll('[data-juneau-dialog-step]').length;
		r.visiblePanes = h.dialog.querySelectorAll('[data-juneau-dialog-step]').filter(function (p) { return !p.hidden; }).map(function (p) { return p.dataset.juneauDialogStep; });
		r.footer = within(h, '.juneau-view-dialog-actions').childNodes.map(function (c) { return c.className; });
		h.close();
		out.rail = r;
	}

	// Next validates only the current step; Finish validates every step and reveals the first invalid one.
	{
		const h = NS.dialogs.open({ title: 'V', steps: [{ id: 'one' }, { id: 'two' }] }, rc);
		const a = required('a');
		h.body().appendChild(a);
		h.setStep('two');
		const b = required('b');
		h.body().appendChild(b);
		h.setStep('one');
		const confirm = within(h, '.juneau-view-dialog-confirm');
		const v = {};
		confirm.dispatch('click');
		v.blockedStep = h.step();
		v.focusedA = env.getActive() === a;
		v.aInvalid = a.getAttribute('aria-invalid');
		v.bUntouched = b.getAttribute('aria-invalid') == null;
		a.value = 'x';
		confirm.dispatch('click');
		v.advancedStep = h.step();
		v.bUntouchedAfterNext = b.getAttribute('aria-invalid') == null;
		b.value = 'y';
		a.value = '';
		confirm.dispatch('click');
		v.finishRevealedStep = h.step();
		v.finishFocusedA = env.getActive() === a;
		v.paneOneShown = !a.parentNode.hidden;
		v.paneTwoHidden = b.parentNode.hidden === true;
		v.stillOpen = NS.dialogs.depth();
		a.value = 'x';
		h.setStep('two');
		confirm.dispatch('click');
		v.closed = await h.closed;
		out.validation = v;
	}

	console.warn = realWarn;
	console.error = realError;
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) { realError(e.stack || String(e)); process.exit(1); });
