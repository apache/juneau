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
 * async-job.cjs - real-browser prober for the juneau-views.js async-job + SSE-streaming contract (TODO-425).
 *
 * Never runs in a default build.  It is driven by AsyncJob_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node async-job.cjs <page.html>
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL served juneau-views.js - in
 * headless Chromium, then, entirely inside the page, drives an async row action through its full lifecycle against
 * a controllable FAKE EventSource (there is no server; the fixture is a file://): the start POST returns a job
 * pointer (not a terminal result), the row picks up the DISTINCT data-juneau-job affordance (NOT
 * data-juneau-inflight, so the table KEEPS polling - the load-bearing HIGH-9 fact), progress events update a
 * visible banner painted with textContent, and the single terminal `result` event settles the row to
 * success / cancelled / cancelled-after-effect, or a stream error settles it to a non-optimistic unknown.  Cancel
 * issues a fail-closed CSRF POST to the job's cancelUrl.  Prints ONE JSON object to stdout.
 *
 * DIVISION OF LABOUR (mirrors modal-result.cjs / row-actions.cjs): this script only OBSERVES; every assertion
 * lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/* Runs inside the page.  Async: the settle path reads the response body via a promise. */
const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS && NS.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	out.actionResultContractVersion = init.ACTION_RESULT_CONTRACT_VERSION;
	const V = init.ACTION_RESULT_CONTRACT_VERSION;
	const tick = () => new Promise(r => setTimeout(r, 0));

	function rendered(el) {
		if (!el) return false;
		const r = el.getBoundingClientRect();
		return r.width > 0 && r.height > 0;
	}

	// A minimal row with an actions cell and a trigger button (setRowJobRunning disables the trigger).
	function makeRow(rowId) {
		const table = document.createElement('table');
		table.setAttribute('data-juneau-view', 'v');
		table.setAttribute('data-juneau-csrf', 'tok-xyz');   // so the fail-closed cancel POST is armed
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

	// A CONTROLLABLE fake EventSource: startJobStream opens one, and the prober emits progress/result/error on it.
	// (There is no server; the fixture is a file:// page, so a real EventSource would just error immediately.)
	const esInstances = [];
	function FakeEventSource(url) {
		this.url = url;
		this.closed = false;
		this._listeners = {};
		esInstances.push(this);
	}
	FakeEventSource.prototype.addEventListener = function (type, fn) {
		(this._listeners[type] = this._listeners[type] || []).push(fn);
	};
	FakeEventSource.prototype.close = function () { this.closed = true; };
	FakeEventSource.prototype.emit = function (type, data) {
		(this._listeners[type] || []).slice().forEach(fn => fn({ data: data }));
	};
	window.EventSource = FakeEventSource;

	function lastEs() { return esInstances[esInstances.length - 1]; }
	function jobBanner(dom) { return dom.td.querySelector('.juneau-view-job-progress'); }
	function jobMsg(dom) {
		const b = jobBanner(dom);
		const m = b ? b.querySelector('.juneau-view-job-progress-msg') : null;
		return m ? m.textContent : null;
	}
	function outcomeOf(dom) {
		const b = dom.td.querySelector('.juneau-view-action-outcome');
		return {
			visible: rendered(b),
			state: b ? b.getAttribute('data-state') : null,
			role: b ? b.getAttribute('role') : null,
			text: b ? b.textContent : null
		};
	}

	// Start an async job by settling the start-POST response with a JOB POINTER (no `outcome` -> not a typed result).
	async function startJob(rowId) {
		const dom = makeRow(rowId || 'INC-1');
		init.setRowInFlight(dom.tr, true);   // the sync submit set the in-flight marker; settle must clear it FIRST
		const jobPointer = { jobId: 'cap-' + rowId, streamUrl: '/juneau-jobs/cap/stream', cancelUrl: '/juneau-jobs/cap/cancel' };
		init.settleActionResponse(resp({ ok: true, status: 200, body: JSON.stringify(jobPointer) }),
			{ id: 'ack', onSuccess: 'mergeRow' }, dom.table, dom.tr,
			{ mergeRow: function (tr, row) { dom._merged = row; } });
		await tick(); await tick(); await tick();
		return dom;
	}

	// ---- Scenario 1: a running job uses the DISTINCT marker and does NOT freeze polling (HIGH-9) ----
	{
		const dom = await startJob('INC-1');
		out.running = {
			hasJobMarker: dom.tr.hasAttribute('data-juneau-job'),
			hasInflightMarker: dom.tr.hasAttribute('data-juneau-inflight'),
			// The load-bearing HIGH-9 fact: polling is NOT frozen while the job runs.
			pollingFrozen: init.hasInFlightRow(dom.table),
			progressVisible: rendered(jobBanner(dom)),
			progressText: jobMsg(dom),
			triggerDisabled: !!dom.trigger.disabled,
			esOpened: esInstances.length > 0,
			esUrl: lastEs() ? lastEs().url : null
		};
		// A progress event updates the live banner (still not frozen, still no outcome yet).
		lastEs().emit('progress', 'step 2 of 3');
		await tick();
		out.progress = {
			text: jobMsg(dom),
			pollingFrozenDuringProgress: init.hasInFlightRow(dom.table),
			outcomeYet: !!dom.td.querySelector('.juneau-view-action-outcome')
		};
		// The terminal `result` event settles the row to success: banner cleared, marker removed, trigger re-enabled.
		lastEs().emit('result', JSON.stringify({ contractVersion: V, outcome: 'success', row: { id: 'INC-1', status: 'ack' } }));
		await tick(); await tick();
		out.settledSuccess = {
			esClosed: lastEs().closed,
			jobMarkerCleared: !dom.tr.hasAttribute('data-juneau-job'),
			progressCleared: !jobBanner(dom),
			triggerReEnabled: !dom.trigger.disabled,
			pollingFrozen: init.hasInFlightRow(dom.table),
			mergedRow: dom._merged,
			outcome: outcomeOf(dom)
		};
	}

	// ---- Scenario 2: Cancel issues a fail-closed CSRF POST; the SERVER-authoritative outcome arrives over SSE ----
	{
		const dom = await startJob('INC-2');
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (url, opts) { fetchCalls.push({ url: url, opts: opts }); return Promise.resolve(resp({ ok: true, status: 200, body: '' })); };
		const cancelBtn = jobBanner(dom).querySelector('.juneau-view-job-cancel');
		out.cancel = { cancelBtnVisible: rendered(cancelBtn) };
		cancelBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		out.cancel.postIssued = fetchCalls.length > 0;
		if (fetchCalls.length > 0) {
			out.cancel.url = fetchCalls[0].url;
			out.cancel.method = fetchCalls[0].opts.method;
			out.cancel.csrfHeader = (fetchCalls[0].opts.headers || {})['X-Csrf-Token'];
		}
		// The authoritative terminal outcome still arrives over the stream: cancelled.
		lastEs().emit('result', JSON.stringify({ contractVersion: V, outcome: 'cancelled' }));
		await tick(); await tick();
		out.cancel.outcome = outcomeOf(dom);
		out.cancel.jobMarkerCleared = !dom.tr.hasAttribute('data-juneau-job');
	}

	// ---- Scenario 3: cancelled-after-effect is a DISTINCT terminal outcome (Q4), rendered on its own ----
	{
		const dom = await startJob('INC-3');
		lastEs().emit('result', JSON.stringify({ contractVersion: V, outcome: 'cancelled-after-effect' }));
		await tick(); await tick();
		out.cancelledAfterEffect = outcomeOf(dom);
	}

	// ---- Scenario 4: a stream error is itself a non-optimistic terminal outcome; polling was never frozen ----
	{
		const dom = await startJob('INC-4');
		lastEs().emit('error', {});
		await tick(); await tick();
		out.streamError = {
			outcome: outcomeOf(dom),
			jobMarkerCleared: !dom.tr.hasAttribute('data-juneau-job'),
			pollingFrozen: init.hasInFlightRow(dom.table)
		};
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node async-job.cjs <page.html>\n');
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
