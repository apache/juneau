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
 * dialog-form.cjs - always-on Node harness for the dialog FormDef painter: appendDialogForm builds
 * label+control rows via createElement for the frozen 6-type allowlist (text/textarea/checkbox/toggle/select/action),
 * unknown types are skipped, a type=action with a missing id is painted disabled, and collectDialogFormFields reads
 * text/textarea/select via .value and checkbox/toggle as explicit "true"/"false" while skipping action buttons.
 *
 *   Usage:  node dialog-form.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node dialog-form.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(typeof I?.appendDialogForm === 'function' && typeof I?.collectDialogFormFields === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const table = env.el('table');
const tr = env.el('tr');
const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate' }] } };

// --- appendDialogForm paints one row per known type; unknown types are dropped --------------------------------
const dialog = env.el('div');
const form = {
	fields: [
		{ name: 'notes', type: 'textarea', label: 'Notes', value: 'hi', maxLength: 500, help: 'help text', required: true },
		{ name: 'notify', type: 'toggle', label: 'Notify', value: 'true' },
		{ name: 'agree', type: 'checkbox', label: 'Agree', required: true },
		{ name: 'sev', type: 'select', label: 'Severity', value: 'warning', options: [{ value: 'critical', label: 'Critical' }, { value: 'warning', label: 'Warning' }] },
		{ name: 'title', type: 'text', label: 'Title', value: 'T', pattern: '^[A-Z].*' },
		{ name: 'esc', type: 'action', label: 'Escalate', actionId: 'esc' },
		{ name: 'weird', type: 'bogus', label: 'Weird' }
	]
};
I.appendDialogForm(dialog, form, table, tr, ctx, 1);

// textarea
const notes = dialog.querySelector('textarea');
out.notes_id = notes.id === 'juneau-dialog-field-1-notes';
out.notes_value = notes.value === 'hi';
out.notes_required = notes.required === true && notes.getAttribute('aria-required') === 'true';
out.notes_maxLength = notes.maxLength === 500;
out.notes_helpText = (function () { const h = dialog.querySelector('[data-juneau-help]'); return h != null && h.textContent === 'help text'; })();
out.notes_describedByHelp = (notes.getAttribute('aria-describedby') || '').indexOf('-help') >= 0;
out.notes_errorSibling = (function () { const e = dialog.querySelector('[data-juneau-error-for]'); return e != null && e.getAttribute('aria-live') === 'polite'; })();

// toggle
const notify = dialog.querySelector('[data-juneau-form-field="notify"]');
out.notify_isCheckboxInput = notify.tagName === 'INPUT' && String(notify.type) === 'checkbox';
out.notify_roleSwitch = notify.getAttribute('role') === 'switch';
out.notify_toggleClass = (notify.className || '').indexOf('juneau-view-toggle') >= 0;
out.notify_checkedFromToken = notify.checked === true && notify.getAttribute('aria-checked') === 'true';

// checkbox (unchecked, required)
const agree = dialog.querySelector('[data-juneau-form-field="agree"]');
out.agree_required = agree.required === true;
out.agree_unchecked = agree.checked === false;

// select
const sev = dialog.querySelector('select');
out.sev_optionCount = sev.querySelectorAll('option').length === 2;
out.sev_optionTextViaTextContent = sev.querySelector('option').textContent === 'Critical';
out.sev_prefillValue = sev.value === 'warning';

// text
const title = dialog.querySelector('[data-juneau-form-field="title"]');
out.title_value = title.value === 'T';
out.title_patternAttr = title.dataset.juneauPattern === '^[A-Z].*';

// action button (openable) - no bound label row
const esc = dialog.querySelector('button');
out.esc_isButton = esc.tagName === 'BUTTON' && String(esc.type) === 'button';
out.esc_enabled = esc.disabled === false;
out.esc_field = esc.dataset.juneauFormField === 'esc';
out.esc_noLabelForAction = dialog.querySelectorAll('label').length === 5;   // notes,notify,agree,sev,title

// unknown type skipped
out.weird_skipped = dialog.querySelector('[data-juneau-form-field="weird"]') === null;

// --- collectDialogFormFields: typed reads, checkbox/toggle as explicit booleans, action skipped -------------
const collected = I.collectDialogFormFields(dialog);
out.collect_notes = collected.notes === 'hi';
out.collect_notify_true = collected.notify === 'true';
out.collect_agree_false = collected.agree === 'false';    // unchecked -> explicit "false", never omitted
out.collect_sev = collected.sev === 'warning';
out.collect_title = collected.title === 'T';
out.collect_esc_skipped = !Object.hasOwn(collected, 'esc');
out.collect_weird_absent = !Object.hasOwn(collected, 'weird');

// --- a type=action whose id is not a dialog RowAction is painted DISABLED (fail-closed, no throw) -----------
const dialog2 = env.el('div');
I.appendDialogForm(dialog2, { fields: [{ name: 'x', type: 'action', label: 'X', actionId: 'nope' }] }, table, tr, ctx, 2);
const xbtn = dialog2.querySelector('button');
out.missing_disabled = xbtn.disabled === true;
out.missing_ariaDisabled = xbtn.getAttribute('aria-disabled') === 'true';
out.missing_marker = xbtn.dataset.juneauActionMissing === '1';

process.stdout.write(JSON.stringify(out));
