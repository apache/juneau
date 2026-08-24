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
 * dialog-validation.cjs - always-on Node harness for the dialog's advisory client-side validation (TODO-445h):
 * required-empty / pattern-mismatch / maxLength-exceeded block a confirm submit and mark aria-invalid; a confirm
 * paints role=alert and concatenates aria-describedby (help + error) and focuses the first invalid control; an
 * advisory (non-confirm) pass leaves role off; a valid form submits; and a Java-only pattern that throws in
 * `new RegExp` fails OPEN (does not block).
 *
 *   Usage:  node dialog-validation.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-validation.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(I && typeof I.appendDialogForm === 'function' && typeof I.validateDialogForm === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const table = env.el('table');
const tr = env.el('tr');
const ctx = { viewDef: { rowActions: [] } };

const dialog = env.el('div');
I.appendDialogForm(dialog, {
	fields: [
		{ name: 'req', type: 'text', label: 'Req', required: true, help: 'need it' },
		{ name: 'pat', type: 'text', label: 'Pat', pattern: '^[0-9]+$' },
		{ name: 'lim', type: 'text', label: 'Lim', maxLength: 3 },
		{ name: 'chk', type: 'checkbox', label: 'Chk', required: true }
	]
}, table, tr, ctx, 1);

const req = dialog.querySelector('[data-juneau-form-field="req"]');
const pat = dialog.querySelector('[data-juneau-form-field="pat"]');
const lim = dialog.querySelector('[data-juneau-form-field="lim"]');
const chk = dialog.querySelector('[data-juneau-form-field="chk"]');

// --- confirm on an invalid form: blocks submit, marks all invalid, role=alert, describedby concat, focus first --
req.value = '';        // required-empty
pat.value = 'abc';     // pattern mismatch
lim.value = 'abcd';    // exceeds maxLength 3
chk.checked = false;   // required checkbox unchecked
out.invalid_blocksSubmit = I.validateDialogForm(dialog, true) === false;
out.req_ariaInvalid = req.getAttribute('aria-invalid') === 'true';
out.pat_ariaInvalid = pat.getAttribute('aria-invalid') === 'true';
out.lim_ariaInvalid = lim.getAttribute('aria-invalid') === 'true';
out.chk_ariaInvalid = chk.getAttribute('aria-invalid') === 'true';
const reqErr = dialog.querySelector('[data-juneau-error-for="req"]');
out.confirm_roleAlert = reqErr.getAttribute('role') === 'alert';
out.confirm_errorTextSet = !!reqErr.textContent && reqErr.textContent.length > 0;
out.describedby_concatHelpAndError = (function () {
	const d = req.getAttribute('aria-describedby') || '';
	return d.indexOf('-help') >= 0 && d.indexOf('-error') >= 0;
})();
out.focus_firstInvalid = env.getActive() === req;

// --- advisory (non-confirm) pass leaves role off ------------------------------------------------------------
req.value = 'ok';   // fix the first field so `pat` is the leading invalid one
I.validateDialogForm(dialog, false);
out.advisory_noRoleAlert = dialog.querySelector('[data-juneau-error-for="pat"]').getAttribute('role') == null;

// --- a fully valid form submits and clears all invalid marks ------------------------------------------------
req.value = 'ok'; pat.value = '123'; lim.value = 'ab'; chk.checked = true;
out.valid_allowsSubmit = I.validateDialogForm(dialog, true) === true;
out.valid_noInvalidMarks = req.getAttribute('aria-invalid') == null && pat.getAttribute('aria-invalid') == null
	&& lim.getAttribute('aria-invalid') == null && chk.getAttribute('aria-invalid') == null;

// --- a Java-only pattern that throws in new RegExp fails OPEN (does not block) -------------------------------
const dialogFO = env.el('div');
I.appendDialogForm(dialogFO, { fields: [{ name: 'fo', type: 'text', label: 'FO', pattern: '[' }] }, table, tr, ctx, 2);
const fo = dialogFO.querySelector('[data-juneau-form-field="fo"]');
fo.value = 'anything';
out.pattern_failOpen = I.validateDialogForm(dialogFO, true) === true;

process.stdout.write(JSON.stringify(out));
