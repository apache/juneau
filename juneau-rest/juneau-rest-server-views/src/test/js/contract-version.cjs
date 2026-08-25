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
 * contract-version.cjs - always-on Node harness for the dialog-form contract-version handshake (TODO-445h, h5):
 * a form-bearing modal opens ONLY when BOTH the modal top-level and the nested form contractVersion equal the ONE
 * baked-in literal; a wrong or missing version on either is a visible refusal and the dialog does not open.  A
 * confirm-only envelope (no form) - whether fetched or a local blank-form-token prompt - stays UNVERSIONED and
 * always opens.
 *
 * The three constants (ModalDef.CONTRACT_VERSION, FormDef.CONTRACT_VERSION, and the runtime literal) move in
 * lockstep, so a HALF-DONE bump is its own case here: a modal still stamped the previous version carrying a form
 * stamped the new one must produce the visible "dialog-form contract mismatch" refusal rather than opening.
 *
 *   Usage:  node contract-version.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node contract-version.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(I && typeof I.openActionDialog === 'function' && typeof I.dialogLayerCount === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const table = env.el('table');
function drain() { while (I.topLayer()) I.popLayer(); }
function flush() { return new Promise(function (r) { let n = 0; (function tick() { if (n++ >= 6) return r(); setTimeout(tick, 0); })(); }); }
function serve(body, opts) { env.setFetch(function () { return Promise.resolve(jsonResponse(body, opts)); }); }
function refusalState(tr) { const b = tr.querySelector('[data-testid="action-outcome"]'); return b ? b.getAttribute('data-state') : null; }
function refusalText(tr) { const b = tr.querySelector('[data-testid="action-outcome"]'); return b ? String(b.textContent || '') : ''; }

// The version the runtime under test bakes in, read back out of the bundle so this harness cannot drift from it.
const CURRENT = I.JUNEAU_DIALOG_FORM_CONTRACT_VERSION;
const STALE = '1';
out.currentVersion = CURRENT;
out.currentVersionIsNotStale = CURRENT !== STALE;

const FIELDS = [{ name: 'notes', type: 'textarea', label: 'Notes' }];
const FORM = { contractVersion: CURRENT, fields: FIELDS };

(async function main() {
	// Case 1: form-bearing, BOTH versions current -> the dialog opens.
	serve({ contractVersion: CURRENT, title: 'Ack', form: FORM });
	const tr1 = env.el('tr');
	I.openActionDialog({ id: 'ack', label: 'Ack', form: '/data/x/ack-form' }, table, tr1, {});
	await flush();
	out.bothCurrent_opens = I.dialogLayerCount() === 1;
	out.bothCurrent_noRefusal = refusalState(tr1) == null;
	drain();

	// Case 2: form-bearing, WRONG modal version -> visible refusal, no open.
	serve({ contractVersion: CURRENT + '9', title: 'Ack', form: FORM });
	const tr2 = env.el('tr');
	I.openActionDialog({ id: 'ack', label: 'Ack', form: '/data/x/ack-form' }, table, tr2, {});
	await flush();
	out.modalVersionWrong_noOpen = I.dialogLayerCount() === 0;
	out.modalVersionWrong_refusal = refusalState(tr2) === 'refusal';
	drain();

	// Case 3: form-bearing, MISSING nested-form version -> visible refusal, no open.
	serve({ contractVersion: CURRENT, title: 'Ack', form: { fields: FIELDS } });
	const tr3 = env.el('tr');
	I.openActionDialog({ id: 'ack', label: 'Ack', form: '/data/x/ack-form' }, table, tr3, {});
	await flush();
	out.formVersionMissing_noOpen = I.dialogLayerCount() === 0;
	out.formVersionMissing_refusal = refusalState(tr3) === 'refusal';
	drain();

	// Case 3b: the HALF-DONE lockstep bump - a modal still stamped the stale version carrying a form stamped the
	// current one.  This is what shipping only two of the three constants looks like on the wire, and it must be the
	// visible "dialog-form contract mismatch" refusal rather than an opened dialog.
	serve({ contractVersion: STALE, title: 'Ack', form: { contractVersion: CURRENT, fields: FIELDS } });
	const tr3b = env.el('tr');
	I.openActionDialog({ id: 'ack', label: 'Ack', form: '/data/x/ack-form' }, table, tr3b, {});
	await flush();
	out.staleModalCurrentForm_noOpen = I.dialogLayerCount() === 0;
	out.staleModalCurrentForm_refusal = refusalState(tr3b) === 'refusal';
	out.staleModalCurrentForm_message = refusalText(tr3b);
	out.staleModalCurrentForm_namesTheMismatch = refusalText(tr3b).indexOf('dialog-form contract mismatch') >= 0;
	// The message quotes all three so an operator can see which one is behind.
	out.staleModalCurrentForm_quotesAllThree = refusalText(tr3b).indexOf("modal='" + STALE + "'") >= 0
		&& refusalText(tr3b).indexOf("form='" + CURRENT + "'") >= 0
		&& refusalText(tr3b).indexOf("runtime='" + CURRENT + "'") >= 0;
	drain();

	// Case 3c: the mirror image - a form left on the stale version under a current modal.  Both halves of the
	// handshake are checked, so neither direction can slip through.
	serve({ contractVersion: CURRENT, title: 'Ack', form: { contractVersion: STALE, fields: FIELDS } });
	const tr3c = env.el('tr');
	I.openActionDialog({ id: 'ack', label: 'Ack', form: '/data/x/ack-form' }, table, tr3c, {});
	await flush();
	out.currentModalStaleForm_noOpen = I.dialogLayerCount() === 0;
	out.currentModalStaleForm_refusal = refusalState(tr3c) === 'refusal';
	drain();

	// Case 4: confirm-only FETCHED envelope (no form) -> unversioned, opens even with no contractVersion.
	serve({ title: 'Escalate?' });
	const tr4 = env.el('tr');
	I.openActionDialog({ id: 'esc', label: 'Escalate', form: '/data/x/esc-form' }, table, tr4, {});
	await flush();
	out.confirmOnlyFetched_opens = I.dialogLayerCount() === 1;
	out.confirmOnlyFetched_noRefusal = refusalState(tr4) == null;
	drain();

	// Case 5: confirm-only LOCAL (blank form token) -> no fetch, opens, never version-gated.
	env.setFetch(function () { return Promise.reject(new Error('confirm-only must not fetch')); });
	const tr5 = env.el('tr');
	I.openActionDialog({ id: 'esc', label: 'Escalate', confirm: 'Escalate this alert?' }, table, tr5, {});
	await flush();
	out.confirmOnlyLocal_opens = I.dialogLayerCount() === 1;
	drain();

	process.stdout.write(JSON.stringify(out));
})();
