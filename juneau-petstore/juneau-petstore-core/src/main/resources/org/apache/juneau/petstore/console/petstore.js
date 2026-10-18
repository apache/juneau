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
// The petstore's shared page script, loaded by base.ftlh on every console page after the toolkit.
(function() {
	// P4 Photo tab: submit the upload form as a PUT to the API's own photo endpoint, then reload; a failure stays on the page.
	// The upload carries the API's CSRF token (stamped on the form by the server) in the API's token header.
	document.addEventListener('submit', function(e) {
		const form = e.target.closest ? e.target.closest('form[data-petstore-upload]') : null;
		if (!form)
			return;
		e.preventDefault();
		const file = form.querySelector('input[type=file]').files[0];
		if (!file)
			return;
		const headers = { 'Content-Type': file.type, 'Accept': 'text/plain' };
		const csrf = form.dataset.petstoreCsrf;
		if (csrf !== undefined)
			headers['X-Csrf-Token'] = csrf;
		fetch(form.dataset.petstoreUpload, { method: 'PUT', headers: headers, body: file })
			.then(function(r) {
				if (r.ok)
					return window.location.reload();
				return r.text().then(function(t) { showError(form, 'Upload failed (' + r.status + '): ' + (t.trim() || r.statusText)); });
			})
			.catch(function(err) { showError(form, 'Upload failed: ' + err.message); });
	});

	// The toolkit's status line under the form (JuneauViews.status), or a plain alert paragraph without the toolkit.
	function showError(form, message) {
		const views = window.JuneauViews;
		if (views?.status) {
			views.status.render(form, 'error', message);
			return;
		}
		let p = form.querySelector('p[data-petstore-error]');
		if (!p) {
			p = document.createElement('p');
			p.dataset.petstoreError = '';
			p.setAttribute('role', 'alert');
			form.appendChild(p);
		}
		p.textContent = message;
	}

	// R10: a typed-confirmation renderer.  A renderer is fn(modal, request) and paints its own UI; it returns a Promise
	// of true to proceed.  The user types the expected value (the row id, or the selected count for an aggregate bulk
	// action) before Confirm enables.
	const views = window.JuneauViews;
	if (views?.rowActions?.registerConfirmRenderer) {
		views.rowActions.registerConfirmRenderer('typed', function(modal, request) {
			const rows = request.rows || [];
			const idField = request.rowIdField || 'id';
			// One row (a per-row or single-selection action): type its id.  Several: type the count.  A ribbon-hosted
			// action has no row, so it types the action's own id.
			let raw;
			if (rows.length > 1)
				raw = rows.length;
			else if (rows.length)
				raw = rows[0][idField];
			else
				raw = request.action?.id;
			// Fail closed: a missing, null or blank value (no action id, or a row without a usable id) would otherwise
			// become '' (an empty input satisfies the gate), 'undefined' or 'null' (a nonsense word to type).  A typed
			// gate with nothing meaningful to type is no gate, so refuse with a warning instead of opening a dialog.
			const expected = raw == null ? '' : String(raw);
			if (expected.trim() === '') {
				console.warn('[petstore] typed confirm refused: nothing to type (no usable row id or action id)');
				return Promise.resolve(false);
			}
			return new Promise(function(resolve) {
				const dlg = document.createElement('dialog');
				dlg.className = 'jc-dialog petstore-typed-confirm';
				const h = document.createElement('h2');
				h.textContent = modal.title;
				const p = document.createElement('p');
				p.textContent = modal.body || '';
				const input = document.createElement('input');
				input.type = 'text';
				input.className = 'jc-input';
				input.setAttribute('aria-label', 'Type ' + expected + ' to confirm');
				const ok = document.createElement('button');
				ok.type = 'button';
				ok.className = 'jc-button petstore-typed-confirm-ok';
				ok.textContent = modal.confirmLabel;
				ok.disabled = true;
				const cancel = document.createElement('button');
				cancel.type = 'button';
				cancel.className = 'jc-button petstore-typed-confirm-cancel';
				cancel.textContent = modal.cancelLabel;
				input.addEventListener('input', function() { ok.disabled = input.value !== expected; });
				function done(v) { dlg.close(); dlg.remove(); resolve(v); }
				ok.addEventListener('click', function() { done(true); });
				cancel.addEventListener('click', function() { done(false); });
				dlg.addEventListener('cancel', function() { done(false); });
				[h, p, input, ok, cancel].forEach(function(n) { dlg.appendChild(n); });
				document.body.appendChild(dlg);
				dlg.showModal();
				input.focus();
			});
		});
	}

	// Jobs page: a groom run's detail panel offers "Groom again".  The button runs through JuneauViews.jobs, which fetches
	// a key bound to this run from the key endpoint and fills {id} from the panel's parent row.
	if (views?.regions?.register) {
		views.regions.register('petstore-regroom', function(ctx, container) {
			container.appendChild(ctx.helpers.jobButton({
				label: 'Groom again',
				endpoint: '/console/ops/jobs/groom-runs/{id}/regroom',
				action: 'regroom',
				envelope: 'action',
				keyUrl: '/console/ops/jobs/groom-runs/{id}/key'
			}));
		});
	}

	// R10: a compact dialog chrome, selected by a modal's chrome:'compact'.  fn(parts, modal) restyles the dialog parts.
	// The new-order wizard (both steps) opts in with ModalDef.chrome("compact").
	if (views?.dialogs?.registerChrome) {
		views.dialogs.registerChrome('compact', function(parts) {
			parts.dialog.classList.add('petstore-dialog-compact');
		});
	}
})();
