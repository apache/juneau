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
 * modal-result.cjs - real-browser prober for the juneau-views.js declarative-modal + typed-action-result +
 * in-flight-row contract (TODO-416/417).
 *
 * Never runs in a default build.  It is driven by ModalResult_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node modal-result.cjs <page.html>
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL served juneau-views.js - in
 * headless Chromium, then, entirely inside the page, exercises the modal overlay, the typed-field confirmation
 * (painted with textContent, so an HTML-shaped field value must NOT become an element), every settled outcome
 * (success/failure/refusal/unknown) plus a non-2xx transport refusal, and the in-flight marker lifecycle.  Prints
 * ONE JSON object to stdout.
 *
 * DIVISION OF LABOUR (mirrors row-actions.cjs): this script only OBSERVES; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/* Runs inside the page.  Async: the settle/dialog paths read the response body via promises. */
const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS && NS.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	out.actionResultContractVersion = init.ACTION_RESULT_CONTRACT_VERSION;

	const tick = () => new Promise(r => setTimeout(r, 0));

	function rendered(el) {
		if (!el) return false;
		const r = el.getBoundingClientRect();
		return r.width > 0 && r.height > 0;
	}

	// A minimal row with an actions cell and a trigger button (setRowInFlight disables the trigger).
	function makeRow(rowId) {
		const table = document.createElement('table');
		table.setAttribute('data-juneau-view', 'v');
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		if (rowId != null) tr.setAttribute('data-juneau-row-id', rowId);
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		const trigger = document.createElement('button');
		trigger.className = 'juneau-view-action-trigger';
		td.appendChild(trigger);
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, tr: tr, td: td, trigger: trigger };
	}

	// A fake fetch Response with a synchronous headers.get and an async text().
	function resp(o) {
		return {
			ok: o.ok,
			status: o.status,
			headers: { get: n => (o.headers || {})[n] || null },
			text: () => Promise.resolve(o.body != null ? o.body : '')
		};
	}

	function outcomeOf(dom) {
		const b = dom.td.querySelector('.juneau-view-action-outcome');
		return {
			visible: rendered(b),
			state: b ? b.getAttribute('data-state') : null,
			role: b ? b.getAttribute('role') : null,
			text: b ? b.textContent : null,
			inflight: dom.tr.hasAttribute('data-juneau-inflight'),
			triggerDisabled: !!dom.trigger.disabled,
			pollingFrozen: init.hasInFlightRow(dom.table)
		};
	}

	// Drive settleActionResponse against a fabricated response, starting from an in-flight row.
	async function settle(r, action, ctx) {
		const dom = makeRow('INC-1');
		init.setRowInFlight(dom.tr, true);
		const before = outcomeOf(dom);
		init.settleActionResponse(r, action || {}, dom.table, dom.tr, ctx || {});
		await tick(); await tick(); await tick();
		const after = outcomeOf(dom);
		after.wasInflightBefore = before.inflight;
		return after;
	}

	const V = init.ACTION_RESULT_CONTRACT_VERSION;

	// ---- All four typed outcomes on a 2xx typed result ----
	out.success = await settle(resp({ ok: true, status: 200,
		body: JSON.stringify({ contractVersion: V, outcome: 'success', row: { id: 'INC-1', status: 'ack' } }) }),
		{ id: 'ack', onSuccess: 'mergeRow' },
		{ mergeRow: function (tr, row) { out.mergedRow = row; } });

	out.failure = await settle(resp({ ok: true, status: 200,
		body: JSON.stringify({ contractVersion: V, outcome: 'failure', message: 'nope' }) }));

	out.refusal = await settle(resp({ ok: true, status: 200,
		body: JSON.stringify({ contractVersion: V, outcome: 'refusal', refusalCode: 'write-guard:not-armed' }) }));

	out.unknown = await settle(resp({ ok: true, status: 200,
		body: JSON.stringify({ contractVersion: V, outcome: 'unknown' }) }));

	// ---- Contract-version mismatch on a 2xx -> visible UNKNOWN (never silently misread) ----
	out.contractMismatch = await settle(resp({ ok: true, status: 200,
		body: JSON.stringify({ contractVersion: '999', outcome: 'success', row: {} }) }));

	// ---- Non-2xx transport refusal: read X-Loopback-Boundary + {reason,message} envelope ----
	out.transport403 = await settle(resp({ ok: false, status: 403,
		headers: { 'X-Loopback-Boundary': 'CSRF_TOKEN_MISSING' },
		body: JSON.stringify({ reason: 'CSRF_TOKEN_MISSING', message: 'missing token' }) }));

	out.transport421 = await settle(resp({ ok: false, status: 421, body: '' }));

	// ---- Merge re-render marked the row ----
	{
		const dom = makeRow('INC-9');
		const ctx = { mergeRow: function (tr, row) { ctx._row = row; } };
		init.settleActionResponse(resp({ ok: true, status: 200,
			body: JSON.stringify({ contractVersion: V, outcome: 'success', row: { id: 'INC-9', status: 's' } }) }),
			{ id: 'ack', onSuccess: 'mergeRow' }, dom.table, dom.tr, ctx);
		await tick(); await tick();
		out.mergeMarkedRow = dom.tr.getAttribute('data-juneau-row-merged');
		out.mergeCtxRow = ctx._row;
	}

	// ---- Modal overlay + typed-field confirmation via textContent (XSS-safe) ----
	{
		const dom = makeRow('INC-1');
		const evil = '<img src=x onerror=alert(1)>';
		const modal = {
			title: 'Acknowledge this incident?',
			fields: [
				{ label: 'Incident', value: 'INC-1' },
				{ label: 'Title', value: evil }
			],
			idempotencyKey: 'key-abc'
		};
		const action = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST', present: 'dialog' };
		const ui = init.showActionDialog(modal, action, dom.table, dom.tr, {});
		await tick();
		const backdrop = document.querySelector('.juneau-view-dialog-backdrop');
		const fieldsEl = backdrop ? backdrop.querySelector('.juneau-view-dialog-fields') : null;
		const dds = fieldsEl ? Array.from(fieldsEl.querySelectorAll('dd')) : [];
		out.modal = {
			backdropVisible: rendered(backdrop),
			title: backdrop ? (backdrop.querySelector('.juneau-view-dialog-title') || {}).textContent : null,
			fieldCount: dds.length,
			evilFieldText: dds.length > 1 ? dds[1].textContent : null,
			// The XSS proof: the HTML-shaped value must NOT have become an <img> element anywhere in the dialog.
			injectedImgCount: backdrop ? backdrop.querySelectorAll('img').length : -1
		};
		// Confirm-button click submits with the idempotency key + target id (stub fetch to capture the body).
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (url, opts) {
			fetchCalls.push({ url: url, opts: opts });
			return Promise.resolve(resp({ ok: true, status: 200,
				body: JSON.stringify({ contractVersion: V, outcome: 'success' }) }));
		};
		dom.table.setAttribute('data-juneau-csrf', 'tok-xyz');
		ui.confirmBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		out.modal.submitIssued = fetchCalls.length > 0;
		if (fetchCalls.length > 0) {
			out.modal.submitBody = fetchCalls[0].opts.body;
			out.modal.backdropClosedAfterConfirm = !document.querySelector('.juneau-view-dialog-backdrop');
		}
	}

	// ---- FormDef inputs: typed textarea/text via createElement + .value; XSS prefill stays inert ----
	{
		const dom = makeRow('QABCDEF');
		const evil = '<img src=x onerror=alert(1)>';
		const modal = {
			title: 'Close this incident?',
			fields: [{ label: 'Incident', value: evil }],
			form: {
				template: '<img src=x onerror=alert(2)>',
				fields: [
					{ name: 'resolution', label: 'Resolution comment', type: 'textarea', required: true, value: evil },
					{ name: 'note', label: 'Note', type: 'text', value: 'ok' },
					{ name: 'skipme', label: 'Bad', type: 'password', value: 'secret' }
				]
			},
			idempotencyKey: 'key-close'
		};
		const action = { id: 'close', label: 'Close', endpoint: '/x/close', method: 'POST', present: 'dialog' };
		const ui = init.showActionDialog(modal, action, dom.table, dom.tr, {});
		await tick();
		const backdrop = document.querySelector('.juneau-view-dialog-backdrop');
		const formEl = backdrop ? backdrop.querySelector('.juneau-view-dialog-form') : null;
		const textarea = formEl ? formEl.querySelector('textarea[data-juneau-form-field="resolution"]') : null;
		const text = formEl ? formEl.querySelector('input[data-juneau-form-field="note"]') : null;
		const password = formEl ? formEl.querySelector('[data-juneau-form-field="skipme"]') : null;
		out.form = {
			formVisible: rendered(formEl),
			textareaPrefill: textarea ? textarea.value : null,
			notePrefill: text ? text.value : null,
			passwordSkipped: !password,
			injectedImgCount: backdrop ? backdrop.querySelectorAll('img').length : -1,
			templateNotInFormHtml: formEl ? formEl.innerHTML.indexOf('<img') < 0 : false
		};
		if (textarea) textarea.value = 'fixed in change';
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (url, opts) {
			fetchCalls.push({ url: url, opts: opts });
			return Promise.resolve(resp({ ok: true, status: 200,
				body: JSON.stringify({ contractVersion: V, outcome: 'success' }) }));
		};
		dom.table.setAttribute('data-juneau-csrf', 'tok-xyz');
		ui.confirmBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		out.form.submitIssued = fetchCalls.length > 0;
		if (fetchCalls.length > 0)
			out.form.submitBody = fetchCalls[0].opts.body;
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node modal-result.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

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
})().catch(e => {
	process.stderr.write(String((e && e.stack) || e) + '\n');
	process.exit(1);
});
