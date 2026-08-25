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
 * dialog-form-browser.cjs - real-browser prober for the dialog FormDef painter + inline validation.
 *
 * Never runs in a default build.  Driven by DialogForm_BrowserTest under `mvn -Pjs-tests`; see that class's javadoc
 * and the profile comment in this module's pom.xml.
 *
 *   Usage:  node dialog-form-browser.cjs <page.html>
 *
 * Loads <page.html> (the real served juneau-views.js) in headless Chromium, then inside the page paints a 6-type
 * dialog form, and measures - as a user would experience them - the real focus trap, keyboard-operable select/switch,
 * the confirm-blocked-on-invalid + focus-first-invalid path, the boolean-string submit body, and the nested
 * type=action button opening a SECOND stacked dialog.  Prints ONE JSON object to stdout; every assertion is in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS?.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));
	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page and
	// break every caller below.
	function rendered(el) { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; }
	function drain() { while (init.topLayer()) init.popLayer(); }

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page and
	// break every caller below.
	function makeRow(rowId) {
		const table = document.createElement('table');
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		if (rowId != null) tr.dataset.juneauRowId = rowId;
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, tr: tr, td: td };
	}

	// A view ctx whose rowActions catalog makes the nested type=action button openable (confirm-only "esc").
	const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate', confirm: 'Escalate?' }] } };
	const action = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST', present: 'dialog' };

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page and
	// break every caller below.
	function sixTypeForm() {
		return {
			title: 'Acknowledge?',
			idempotencyKey: 'key-ack',
			form: {
				contractVersion: '1',
				fields: [
					{ name: 'notes', label: 'Notes', type: 'textarea', required: true, value: '' },
					{ name: 'title', label: 'Title', type: 'text', value: 'T' },
					{ name: 'agree', label: 'Agree', type: 'checkbox' },
					{ name: 'notify', label: 'Notify', type: 'toggle', value: 'true' },
					{ name: 'sev', label: 'Severity', type: 'select', value: 'warning',
						options: [{ value: 'critical', label: 'Critical' }, { value: 'warning', label: 'Warning' }] },
					{ name: 'esc', label: 'Escalate', type: 'action', actionId: 'esc' }
				]
			}
		};
	}

	// ---- Block A: paint the 6 types + real focus trap ----
	async function blockPaint() {
		const dom = makeRow('INC-1');
		init.showActionDialog(sixTypeForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const backdrop = document.querySelector('.juneau-view-dialog-backdrop');
		const formEl = backdrop ? backdrop.querySelector('.juneau-view-dialog-form') : null;
		const toggle = formEl ? formEl.querySelector('[data-juneau-form-field="notify"]') : null;
		const select = formEl ? formEl.querySelector('select[data-juneau-form-field="sev"]') : null;
		const escBtn = formEl ? formEl.querySelector('button[data-juneau-form-field="esc"]') : null;
		out.paint = {
			formVisible: rendered(formEl),
			hasTextarea: !!formEl?.querySelector('textarea[data-juneau-form-field="notes"]'),
			hasText: !!formEl?.querySelector('input[data-juneau-form-field="title"]'),
			hasCheckbox: !!formEl?.querySelector('input[data-juneau-form-field="agree"]'),
			toggleRoleSwitch: toggle ? toggle.getAttribute('role') === 'switch' : false,
			toggleCheckedFromToken: toggle ? !!toggle.checked : false,
			selectOptionCount: select ? select.querySelectorAll('option').length : -1,
			selectPrefill: select ? select.value : null,
			actionEnabled: escBtn ? !escBtn.disabled : false,
			focusTrappedIntoDialog: !!backdrop?.contains(document.activeElement)
		};
		// Real Tab from the last focusable wraps to the first (focus stays inside the trapping layer).
		const focusables = Array.from(backdrop.querySelectorAll(
			'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]'
		)).filter(n => n.getAttribute('tabindex') !== '-1');
		const first = focusables[0], last = focusables.at(-1);
		last?.focus?.();
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }));
		out.paint.tabWrapsToFirst = document.activeElement === first;
		out.paint.tabKeepsFocusInDialog = backdrop.contains(document.activeElement);
		drain();
	}

	// ---- Block B: confirm on an invalid form is blocked and focuses the first invalid control ----
	async function blockInvalid() {
		const dom = makeRow('INC-2');
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (u, o) { fetchCalls.push({ u: u, o: o }); return Promise.resolve({ ok: true, status: 200, headers: { get: () => null }, text: () => Promise.resolve('{}') }); };
		const ui = init.showActionDialog(sixTypeForm(), action, dom.table, dom.tr, ctx);   // notes required + empty
		await tick();
		dom.table.dataset.juneauCsrf = 'tok';
		ui.confirmBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		const notes = document.querySelector('textarea[data-juneau-form-field="notes"]');
		out.invalid = {
			submitBlocked: fetchCalls.length === 0,
			notesAriaInvalid: notes ? notes.getAttribute('aria-invalid') === 'true' : false,
			focusOnFirstInvalid: document.activeElement === notes,
			dialogStillOpen: !!document.querySelector('.juneau-view-dialog-backdrop')
		};
		drain();
	}

	// ---- Block C: a valid form submits with boolean-string field values (toggle off -> "false", checkbox on -> "true") ----
	async function blockSubmit() {
		const dom = makeRow('INC-3');
		const ui = init.showActionDialog(sixTypeForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const backdrop = document.querySelector('.juneau-view-dialog-backdrop');
		backdrop.querySelector('textarea[data-juneau-form-field="notes"]').value = 'looks fine';
		backdrop.querySelector('input[data-juneau-form-field="agree"]').checked = true;
		backdrop.querySelector('[data-juneau-form-field="notify"]').checked = false;
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (u, o) { fetchCalls.push({ u: u, o: o }); return Promise.resolve({ ok: true, status: 200, headers: { get: () => null }, text: () => Promise.resolve(JSON.stringify({ contractVersion: '1', outcome: 'success' })) }); };
		dom.table.dataset.juneauCsrf = 'tok';
		ui.confirmBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		out.submit = { issued: fetchCalls.length > 0, body: fetchCalls.length ? String(fetchCalls[0].o.body) : null };
		drain();
	}

	// ---- Block D: the nested type=action button opens a SECOND stacked dialog without closing the first ----
	async function blockNested() {
		const dom = makeRow('INC-4');
		init.showActionDialog(sixTypeForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const before = init.dialogLayerCount();
		const escBtn = document.querySelector('button[data-juneau-form-field="esc"]');
		if (escBtn) {
			escBtn.click();
		}
		await tick();
		out.nested = {
			before: before,
			after: init.dialogLayerCount(),
			twoBackdrops: document.querySelectorAll('.juneau-view-dialog-backdrop').length
		};
		drain();
	}

	await blockPaint();
	await blockInvalid();
	await blockSubmit();
	await blockNested();

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node dialog-form-browser.cjs <page.html>\n'); process.exit(2); }
	if (!fs.existsSync(fixture)) throw new Error('fixture not found: ' + fixture);
	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => { process.stderr.write(String(e?.stack || e) + '\n'); process.exit(1); });
