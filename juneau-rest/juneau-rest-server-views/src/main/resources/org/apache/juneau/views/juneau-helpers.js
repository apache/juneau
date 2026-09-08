/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  The ASF licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * juneau-helpers.js - the paint library for a `data-juneau-region` populator (WORK-J0522b).
 *
 * Namespaced `JuneauViews.helpers.*`, published onto the same `window.JuneauViews` namespace
 * `juneau-views.js` and `juneau-regions.js` create.  `ctx.helpers` (juneau-regions.js's `buildCtx`) is
 * a bare pass-through of `NS.helpers`, so a populator sees this object with no wiring of its own once this
 * script has loaded.
 *
 * LOAD ORDER IS A CONTRACT, NOT A PREFERENCE (see ViewsMixin#HELPERS_JS_PATH): this file MUST load after
 * juneau-views.js (whose format copiers, renderer-slot filler and async-status painter it reuses) and after
 * juneau-regions.js (a page of purely custom regions can load the bus/primitive without the paint library).
 *
 * TWELVE HELPERS:
 *   fieldGrid, kvTable, button, buttonRow, text, pill, icon         - promoted (export work)
 *   tabStrip                                                        - promoted AND WIDENED (lazy per-tab
 *                                                                     populate, fill-once, contained throws)
 *   dataPane, recordTable                                           - net-new
 *   dateRange, dropdown, filterBuilder                               - net-new controls
 *   editableField, toast                                            - net-new (editable detail leaf + error toast)
 *
 * INVARIANTS (design §9.2), enforced by this file's own shape as much as by test 29's source scan:
 *   - Pure data -> DOM.  No fetch (dataPane's `load` is an author-supplied thunk; this file never
 *     constructs a URL or calls `fetch` itself), no bus, no PAGE-GLOBAL state.
 *   - The one amendment (SD-3): a helper MAY hold per-instance state confined to the node it returned
 *     (tabStrip's fill-once bookkeeping; editableField's view/edit/dirty/error; fieldGrid's coordinator).
 *     That state lives in a closure captured by the returned node's event listeners - never in a
 *     module-level Map/Set/WeakMap/counter.
 *   - No HTML-string sink, ever (R16).  Every helper builds with `createElement`/`textContent`.  The one
 *     exception is `icon()`, which paints TRUSTED icon-registry sprite markup only, exactly as
 *     `resolveDetailHeaderIcon` already does in juneau-views.js.
 *   - Themable by class, not by inline style.  The one exception is `fieldGrid`'s `columns` option, which
 *     is a CSS CUSTOM PROPERTY (`--juneau-view-detail-columns`), not a `grid-template-columns` inline
 *     style - the mechanism fork F13 asks for.
 *   - The second named exception: `toast()` is the only helper that writes under `document.body`.  It finds
 *     or replaces a single `.jc-toast` via `document.querySelector`, appends that ephemeral node to
 *     `document.body`, and stores its dismiss timer on the node itself - never a module-level registry.
 */
(function () {
	"use strict";

	var NS = window.JuneauViews = window.JuneauViews || {};

	if (!NS.init) {
		// Loud rather than a blank container: a populator that reaches for ctx.helpers before this asset
		// has loaded gets a named error at the first call, not a silent `undefined.fieldGrid is not a
		// function` three stack frames away.
		throw new Error("JuneauViews.helpers: juneau-views.js must be loaded before juneau-helpers.js.");
	}

	var I = NS.init;

	// ================================================================================================
	// SMALL SHARED UTILITIES (pure; no module-level mutable state)
	// ================================================================================================

	function clear(el) {
		if (!el) return;
		if (typeof el.replaceChildren === "function") { el.replaceChildren(); return; }
		while (el.firstChild) el.firstChild.remove();
	}

	function isPlainObject(v) {
		return v != null && typeof v === "object" && !Array.isArray(v);
	}

	function copyPlain(src) {
		var out = {};
		if (!isPlainObject(src)) return out;
		var keys = Object.keys(src);
		for (var i = 0; i < keys.length; i++) out[keys[i]] = src[keys[i]];
		return out;
	}

	function hasClass(el, name) {
		return (" " + (el.className || "") + " ").indexOf(" " + name + " ") !== -1;
	}

	function setClass(el, name, on) {
		if (!el) return;
		if (on) {
			if (!hasClass(el, name)) el.className = el.className ? el.className + " " + name : name;
			return;
		}
		if (hasClass(el, name))
			el.className = (" " + el.className + " ").split(" " + name + " ").join(" ").replace(/^\s+|\s+$/g, "");
	}

	/** Interpolates a `{key}` URL template against a flat values map, then scheme-checks the result. */
	function substituteFieldHref(template, values) {
		if (template == null) return null;
		var map = isPlainObject(values) ? values : {};
		var url = String(template).replace(/\{([^}]+)\}/g, function (m, key) {
			var v = Object.hasOwn(map, key) ? map[key] : undefined;
			return v == null ? "" : encodeURIComponent(String(v));
		});
		return I.isSafeDetailUrl(url) ? url : null;
	}

	function defaultEmptyNode() {
		var p = document.createElement("p");
		p.className = "juneau-view-helper-empty";
		p.textContent = "No data.";
		return p;
	}

	/**
	 * Whether a `load`/`fetchDeclared`-style resolution counts as EMPTY (§8.5's shape table): null,
	 * undefined, an array of length 0, or an object with zero own enumerable keys.  A values map that
	 * legitimately has no keys and an envelope are different things, but `dataPane`'s contract forbids the
	 * latter outright (never a `{contractVersion,fields}` envelope), so this predicate is safe as written.
	 */
	function isEmptyResolution(data) {
		if (data == null) return true;
		if (Array.isArray(data)) return data.length === 0;
		if (typeof data === "object") return Object.keys(data).length === 0;
		return false;
	}

	/** Abort reasons are DOMExceptions named "AbortError" (the J0522a finding this file builds against). */
	function isAbortError(err) {
		return !!err && err.name === "AbortError";
	}

	/**
	 * Whether a rejection carries the framework's "empty" kind (§8.5/§12.3.3 rule 2b - a 404 is a
	 * REJECTION carrying this kind).  No declarative default has landed yet (that is WORK-J0522c's
	 * `fetchDeclared`), so there is no existing runtime convention for this to match; `dataPane`'s own
	 * contract is `err.kind === "empty"`, which an author's `load` thunk sets before rejecting (e.g. on a
	 * 404 response).  Documented here rather than assumed - see this child's build report.
	 */
	function isEmptyKindError(err) {
		return !!err && err.kind === "empty";
	}

	function appendAll(el, nodeOrNodes) {
		if (nodeOrNodes == null) return;
		if (Array.isArray(nodeOrNodes)) { for (var i = 0; i < nodeOrNodes.length; i++) appendAll(el, nodeOrNodes[i]); return; }
		el.appendChild(nodeOrNodes);
	}

	// ================================================================================================
	// text / pill / icon - leaf nodes
	// ================================================================================================

	function text(value) {
		return document.createTextNode(value == null ? "" : String(value));
	}

	// Closed tone vocabulary for this helper.  NOTE: this is NOT the same vocabulary as the existing
	// `pill` renderer's `meta.tone` (juneau-renders.js: info/success/warning/error/neutral) - see this
	// child's build report for the design-internal inconsistency (design §9's own worked example at
	// §11.5 uses ok/warn/error/idle, which this helper follows, while the "Replaces" column of the same
	// table cites the differently-vocabularied renderer).
	var PILL_TONES = { ok: 1, warn: 1, error: 1, idle: 1 };

	function pill(value, tone) {
		var span = document.createElement("span");
		span.className = "juneau-view-helper-pill";
		var dot = document.createElement("span");
		dot.className = "juneau-view-helper-pill-dot" + (tone && Object.hasOwn(PILL_TONES, tone) ? " juneau-view-helper-pill-dot--" + tone : "");
		dot.setAttribute("aria-hidden", "true");
		var label = document.createElement("span");
		label.className = "juneau-view-helper-pill-label";
		label.textContent = value == null ? "" : String(value);
		span.appendChild(dot);
		span.appendChild(label);
		return span;
	}

	function icon(name) {
		var span = document.createElement("span");
		span.className = "juneau-view-helper-icon";
		span.setAttribute("aria-hidden", "true");
		var icons = window.JuneauViews && window.JuneauViews.icons;
		var markup = icons && typeof icons.resolveIcon === "function" ? icons.resolveIcon(name) : null;
		if (!markup) { span.hidden = true; return span; }
		span.innerHTML = markup; // trusted icon-registry sprite markup only - never user data
		return span;
	}

	// ================================================================================================
	// button / buttonRow
	// ================================================================================================

	function button(spec) {
		spec = spec || {};
		var appearance = spec.appearance;
		if (appearance != null && appearance !== "" && appearance !== "chrome" && appearance !== "icon")
			throw new TypeError("JuneauViews.helpers: button(spec) appearance must be \"icon\" or \"chrome\" (or omitted).");
		var btn = document.createElement("button");
		btn.type = "button";
		btn.className = "juneau-view-helper-btn"
			+ (spec.tone ? " juneau-view-helper-btn--" + spec.tone : "")
			+ (appearance === "icon" ? " juneau-view-helper-btn--icon" : "");
		if (spec.id != null) btn.id = spec.id;
		var canClick = typeof spec.onClick === "function" && !spec.disabled;
		btn.disabled = !canClick;
		if (!canClick) btn.setAttribute("aria-disabled", "true");
		if (spec.icon) btn.appendChild(icon(spec.icon));
		var label = document.createElement("span");
		label.className = "juneau-view-helper-btn-label";
		label.textContent = spec.label == null ? "" : String(spec.label);
		btn.appendChild(label);
		if (canClick) btn.addEventListener("click", function (e) { spec.onClick(e); });
		return btn;
	}

	function buttonRow(specs) {
		if (!Array.isArray(specs))
			throw new TypeError("JuneauViews.helpers: buttonRow(specs) requires specs to be an array.");
		var row = document.createElement("div");
		row.className = "juneau-view-helper-btn-row";
		row.setAttribute("role", "group");
		for (var i = 0; i < specs.length; i++) row.appendChild(button(specs[i]));
		return row;
	}

	function buildFieldActionBar(actionsSpec, fieldData, onAction) {
		var items = Array.isArray(actionsSpec) ? actionsSpec : [];
		var bar = document.createElement("div");
		bar.className = "juneau-view-helper-field-actions";
		bar.setAttribute("role", "group");
		for (var i = 0; i < items.length; i++) {
			var a = items[i];
			bar.appendChild(button({
				id: a.id,
				label: a.label,
				appearance: a.appearance,
				onClick: typeof onAction === "function" ? function (id) { return function () { onAction(id, fieldData); }; }(a.id) : null
			}));
		}
		return bar;
	}

	// ================================================================================================
	// toast(message, opts) - the only helper that writes under document.body.  One ephemeral `.jc-toast`
	// node, found by class query and reused rather than stacked.  Dismiss timer lives on the node.
	// ================================================================================================

	function toast(message, opts) {
		opts = opts || {};
		var tone = opts.tone === "error" || opts.tone === "success" || opts.tone === "info" ? opts.tone : "info";
		var timeoutMs = opts.timeoutMs == null ? 4000 : Number(opts.timeoutMs);
		var text = message == null ? "" : String(message);
		if (!document.body) return;

		var node = document.querySelector(".jc-toast");
		if (!node) {
			node = document.createElement("div");
			node.addEventListener("click", function () { dismissToast(node); });
			node.addEventListener("keydown", function (e) {
				if (e.key === "Escape" || e.key === "Esc") dismissToast(node);
			});
		}
		node.className = "jc-toast" + (tone === "error" ? " is-error" : tone === "success" ? " is-success" : " is-info");
		node.setAttribute("role", tone === "error" ? "alert" : "status");
		node.textContent = text;
		if (!node.parentNode) document.body.appendChild(node);
		if (node._jcToastTimer) clearTimeout(node._jcToastTimer);
		if (timeoutMs > 0) {
			node._jcToastTimer = setTimeout(function () { dismissToast(node); }, timeoutMs);
		}
	}

	function dismissToast(node) {
		if (!node) return;
		if (node._jcToastTimer) {
			clearTimeout(node._jcToastTimer);
			node._jcToastTimer = null;
		}
		if (node.parentNode) node.parentNode.removeChild(node);
	}

	function saveErrorMessage(err) {
		if (err != null && typeof err === "object" && typeof err.message === "string" && err.message)
			return err.message;
		if (err == null) return "Save failed.";
		return String(err);
	}

	function selectOptionLabel(options, value) {
		if (!Array.isArray(options)) return value == null ? "" : String(value);
		for (var i = 0; i < options.length; i++) {
			if (String(options[i].value) === String(value))
				return options[i].label != null ? String(options[i].label) : String(options[i].value);
		}
		return value == null ? "" : String(value);
	}

	// ================================================================================================
	// editableField(opts) - one Detail Subview value as view-or-edit.  UI state lives in this closure.
	// Persistence is the consumer's `onSave` thunk; this helper never fetches.
	// ================================================================================================

	function editableField(opts) {
		opts = opts || {};
		if (typeof opts.onSave !== "function")
			throw new TypeError("JuneauViews.helpers: editableField(opts) requires opts.onSave to be a function.");
		var type = opts.type == null || opts.type === "" ? "text" : String(opts.type);
		if (type !== "text" && type !== "select" && type !== "checkbox")
			throw new TypeError("JuneauViews.helpers: editableField(opts) unknown type '" + type + "'.");
		if (type === "select" && (!Array.isArray(opts.options) || opts.options.length === 0))
			throw new TypeError("JuneauViews.helpers: editableField(opts) type 'select' requires a non-empty options array.");

		var options = opts.options;
		var multiline = type === "text" && !!opts.multiline;
		var persist = type === "checkbox" ? "blur" : (opts.persist === "explicit" ? "explicit" : "blur");
		var onSave = opts.onSave;
		var label = opts.label == null ? "" : String(opts.label);
		var name = opts.name == null ? "" : String(opts.name);
		var disabled = !!opts.disabled;
		var displayValue = opts.displayValue != null ? String(opts.displayValue) : null;
		var committed = type === "checkbox" ? !!opts.value : (opts.value == null ? "" : String(opts.value));

		var root = document.createElement("div");
		root.className = "jc-editable-field";
		var errorId = "jc-editable-field-error-" + Math.random().toString(36).slice(2, 10);
		var mode = "view";
		var control = null;
		var pencil = null;
		var errorEl = null;
		var saveBtn = null;
		var cancelBtn = null;
		var valueAtOpen = committed;
		var errorText = "";
		var saveGen = 0;
		var inFlight = null;

		function swallow(p) {
			if (p && typeof p.then === "function") p.then(function () {}, function () {});
			return p;
		}

		function viewLabel() {
			if (displayValue != null) return displayValue;
			if (type === "select") return selectOptionLabel(options, committed);
			return committed == null ? "" : String(committed);
		}

		function readControl() {
			if (!control) return committed;
			if (type === "checkbox") return !!control.checked;
			return control.value == null ? "" : String(control.value);
		}

		function isDirty() {
			var cur = readControl();
			if (type === "checkbox") return !!cur !== !!valueAtOpen;
			return String(cur) !== String(valueAtOpen);
		}

		function clearError() {
			errorText = "";
			setClass(root, "is-error", false);
			if (errorEl && errorEl.parentNode) errorEl.parentNode.removeChild(errorEl);
			errorEl = null;
			if (control) {
				control.removeAttribute("aria-invalid");
				control.removeAttribute("aria-describedby");
			}
		}

		function showError(msg) {
			errorText = msg;
			setClass(root, "is-error", true);
			if (!errorEl) {
				errorEl = document.createElement("div");
				errorEl.className = "jc-editable-field-error";
				errorEl.id = errorId;
				errorEl.setAttribute("role", "alert");
				root.appendChild(errorEl);
			}
			errorEl.textContent = msg;
			if (control) {
				control.setAttribute("aria-invalid", "true");
				control.setAttribute("aria-describedby", errorId);
			}
		}

		function setSaving(on) {
			if (on) {
				mode = "saving";
				setClass(root, "is-saving", true);
				root.setAttribute("aria-busy", "true");
				if (control) control.disabled = true;
				if (saveBtn) saveBtn.disabled = true;
				if (cancelBtn) cancelBtn.disabled = true;
				return;
			}
			setClass(root, "is-saving", false);
			root.removeAttribute("aria-busy");
			if (control) control.disabled = !!disabled;
			if (saveBtn) saveBtn.disabled = false;
			if (cancelBtn) cancelBtn.disabled = false;
			if (mode === "saving") mode = type === "checkbox" ? "view" : "edit";
		}

		function applySaved(submitted, result) {
			if (result == null) return type === "checkbox" ? !!submitted : String(submitted);
			if (type === "checkbox") return !!result;
			return String(result);
		}

		function startSave(next) {
			if (inFlight) return inFlight;
			saveGen++;
			var gen = saveGen;
			setSaving(true);
			inFlight = Promise.resolve()
				.then(function () { return onSave(next); })
				.then(function (result) {
					if (gen !== saveGen) return;
					inFlight = null;
					committed = applySaved(next, result);
					displayValue = null;
					clearError();
					if (type === "checkbox") {
						setSaving(false);
						mode = "view";
						control.checked = committed;
						if (control.focus) control.focus();
						return;
					}
					mode = "view";
					renderView();
					if (pencil && pencil.focus) pencil.focus();
				}, function (err) {
					if (gen !== saveGen) return Promise.reject(err);
					inFlight = null;
					var msg = saveErrorMessage(err);
					toast(msg, { tone: "error" });
					if (type === "checkbox") {
						control.checked = committed;
						setSaving(false);
						mode = "view";
						showError(msg);
						if (control.focus) control.focus();
						return Promise.reject(err);
					}
					setSaving(false);
					mode = "edit";
					showError(msg);
					if (control && control.focus) control.focus();
					return Promise.reject(err);
				});
			return inFlight;
		}

		function requestSaveFromControl() {
			if (mode === "saving") return inFlight || Promise.resolve();
			if (mode !== "edit") return Promise.resolve();
			if (!isDirty()) {
				cancelEdit();
				return Promise.resolve();
			}
			return startSave(readControl());
		}

		function cancelEdit() {
			if (type === "checkbox") return;
			if (mode === "saving") return;
			saveGen++;
			inFlight = null;
			clearError();
			mode = "view";
			renderView();
			if (pencil && pencil.focus) pencil.focus();
		}

		function onControlKeydown(e) {
			if (mode === "saving") return;
			var key = e.key;
			if (key === "Escape" || key === "Esc") {
				if (e.preventDefault) e.preventDefault();
				cancelEdit();
				return;
			}
			if (key !== "Enter") return;
			if (!control || control.tagName === "TEXTAREA" || control.tagName === "SELECT") return;
			if (e.preventDefault) e.preventDefault();
			swallow(requestSaveFromControl());
		}

		function onControlInput() {
			if (errorText) clearError();
		}

		function buildTextOrSelect(value) {
			var el;
			if (type === "select") {
				el = document.createElement("select");
				for (var i = 0; i < options.length; i++) {
					var o = options[i];
					var opt = document.createElement("option");
					opt.value = o.value == null ? "" : String(o.value);
					opt.textContent = o.label != null ? String(o.label) : String(o.value);
					el.appendChild(opt);
				}
				el.value = value == null ? "" : String(value);
			} else if (multiline) {
				el = document.createElement("textarea");
				el.value = value == null ? "" : String(value);
			} else {
				el = document.createElement("input");
				el.type = "text";
				el.value = value == null ? "" : String(value);
			}
			el.className = "jc-editable-field-input";
			el.setAttribute("aria-label", label);
			if (name) el.name = name;
			el.addEventListener("keydown", onControlKeydown);
			el.addEventListener("input", onControlInput);
			el.addEventListener("change", onControlInput);
			return el;
		}

		function enterEdit() {
			if (type === "checkbox" || disabled) return;
			if (mode === "edit" || mode === "saving") return;
			valueAtOpen = committed;
			mode = "edit";
			renderEdit();
			if (control && control.focus) control.focus();
		}

		function renderView() {
			clear(root);
			control = null;
			errorEl = null;
			saveBtn = null;
			cancelBtn = null;
			setClass(root, "is-editing", false);
			setClass(root, "is-saving", false);
			setClass(root, "is-error", false);
			root.removeAttribute("aria-busy");

			var view = document.createElement("div");
			view.className = "jc-editable-field-view";
			var valueNode = document.createElement("span");
			if (typeof opts.paintView === "function") opts.paintView(valueNode, committed);
			else valueNode.textContent = viewLabel();
			view.appendChild(valueNode);

			pencil = document.createElement("button");
			pencil.type = "button";
			pencil.className = "jc-editable-field-pencil";
			pencil.setAttribute("aria-label", "Edit " + label);
			pencil.title = "Edit " + label;
			pencil.disabled = !!disabled;
			pencil.appendChild(icon("edit"));
			pencil.addEventListener("click", function (e) {
				if (e && e.preventDefault) e.preventDefault();
				if (disabled) return;
				if (typeof opts.onRequestOpen === "function") swallow(opts.onRequestOpen(root));
				else enterEdit();
			});
			view.appendChild(pencil);
			root.appendChild(view);
		}

		function renderEdit() {
			clear(root);
			errorEl = null;
			setClass(root, "is-editing", true);
			setClass(root, "is-saving", false);
			setClass(root, "is-error", false);
			root.removeAttribute("aria-busy");

			control = buildTextOrSelect(committed);
			root.appendChild(control);

			if (persist === "explicit") {
				var actions = document.createElement("div");
				actions.className = "jc-editable-field-explicit-actions";
				saveBtn = button({ label: "Save", onClick: function () { swallow(requestSaveFromControl()); } });
				cancelBtn = button({ label: "Cancel", onClick: function () { cancelEdit(); } });
				actions.appendChild(saveBtn);
				actions.appendChild(cancelBtn);
				root.appendChild(actions);
			} else {
				saveBtn = null;
				cancelBtn = null;
			}
		}

		function renderCheckbox() {
			control = document.createElement("input");
			control.type = "checkbox";
			control.className = "jc-editable-field-input";
			control.checked = committed;
			control.disabled = !!disabled;
			control.setAttribute("aria-label", label);
			if (name) control.name = name;
			control.addEventListener("change", function () {
				if (disabled || mode === "saving") {
					control.checked = committed;
					return;
				}
				if (errorText) clearError();
				valueAtOpen = committed;
				swallow(startSave(!!control.checked));
			});
			root.appendChild(control);
		}

		root.addEventListener("focusout", function (e) {
			if (persist !== "blur" || type === "checkbox") return;
			if (mode !== "edit") return;
			var rel = e && e.relatedTarget;
			if (rel && root.contains(rel)) return;
			swallow(requestSaveFromControl());
		});

		root._jcEditable = {
			requestOpen: function () { enterEdit(); },
			requestCommitOrCancel: function () {
				if (type === "checkbox") return inFlight || Promise.resolve();
				if (mode === "view") return Promise.resolve();
				if (mode === "saving") return inFlight || Promise.resolve();
				return requestSaveFromControl();
			}
		};

		if (type === "checkbox") renderCheckbox();
		else renderView();
		return root;
	}

	// ================================================================================================
	// kvTable(pairs, opts) - an ad-hoc two-column table.  `pairs` accepts an array of [k,v] tuples, an
	// array of {key,value} objects, or a plain object map (own-enumerable insertion order).  Nothing
	// else - a loud argument error rather than the silent-empty-paint defect this was minted to close.
	// ================================================================================================

	function normalizePairs(pairs) {
		if (Array.isArray(pairs)) {
			return pairs.map(function (item) {
				if (Array.isArray(item) && item.length === 2) return [item[0], item[1]];
				if (isPlainObject(item) && Object.hasOwn(item, "key")) return [item.key, item.value];
				throw new TypeError("JuneauViews.helpers: kvTable(pairs, opts) - each array entry must be a "
					+ "[key, value] tuple or a {key, value} object.");
			});
		}
		if (isPlainObject(pairs)) return Object.keys(pairs).map(function (k) { return [k, pairs[k]]; });
		throw new TypeError("JuneauViews.helpers: kvTable(pairs, opts) requires an array of pairs or a plain object map.");
	}

	function kvTable(pairs, opts) {
		opts = opts || {};
		var rows = normalizePairs(pairs);
		var table = document.createElement("table");
		table.className = "juneau-view-helper-kv-table";
		if (opts.caption) {
			var caption = document.createElement("caption");
			caption.textContent = opts.caption;
			table.appendChild(caption);
		}
		var tbody = document.createElement("tbody");
		for (var i = 0; i < rows.length; i++) {
			var tr = document.createElement("tr");
			var th = document.createElement("th");
			th.scope = "row";
			th.textContent = rows[i][0] == null ? "" : String(rows[i][0]);
			var td = document.createElement("td");
			td.textContent = I.scalarFieldValue(rows[i][1]);
			tr.appendChild(th);
			tr.appendChild(td);
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		return table;
	}

	// ================================================================================================
	// fieldGrid(fields, opts) - the catalog+values join (§9.1.1).  `fields` is PROVENANCE-AGNOSTIC: a
	// projected declarative catalog and an author-declared JS literal (SD-3 row-detail) are the same
	// shape and this function does not know or care which it was handed.
	// ================================================================================================

	/**
	 * Resolves `field.render` with an optional PER-CALL override (`opts.renderers`), then paints through
	 * the SAME `fillRenderSlot` copier `paintDetailFieldSlot` uses today - so an overridden renderer gets
	 * the identical never-innerHTML sanitize-and-copy discipline as a globally-registered one, rather than
	 * a second, helper-local implementation of it.
	 *
	 * IMPLEMENTATION NOTE (not specified by the design; a judgment call made building this child):
	 * `fillRenderSlot` always resolves against the GLOBAL renderer registry and takes no resolver
	 * parameter, so a per-call override is applied by temporarily registering it under the same id via the
	 * existing public `NS.registerRenderer`/`NS.resolveRenderer` pair, then restoring whatever was there
	 * (or an inert `{}` stub when nothing was) in a `finally`.  This is synchronous and non-reentrant-safe
	 * for the ordinary case (a renderer's own `display` never itself calls back into `fieldGrid`), reuses
	 * only already-exported functions, and never assigns a `NS.*` member from this file - the swap is
	 * restored before this call returns, so no page-global state survives past a single fill.
	 */
	function fillRenderSlotWithOverride(slot, value, renderId, meta, href, values, renderers) {
		var override = renderers && Object.hasOwn(renderers, renderId) ? renderers[renderId] : null;
		if (!override) {
			I.fillRenderSlot(slot, value, renderId, meta, href, values);
			return;
		}
		var prior = NS.resolveRenderer(renderId);
		NS.registerRenderer(renderId, override);
		try {
			I.fillRenderSlot(slot, value, renderId, meta, href, values);
		} finally {
			NS.registerRenderer(renderId, prior);
		}
	}

	function paintFieldValue(slot, field, values, renderers) {
		var scalar = I.scalarFieldValue(Object.hasOwn(values, field.data) ? values[field.data] : undefined);
		if (field.render) {
			var renderHref = field.href ? substituteFieldHref(field.href, values) : null;
			fillRenderSlotWithOverride(slot, scalar, field.render, field.renderMeta || null, renderHref, values, renderers);
			return scalar;
		}
		if (field.format === "markdown") I.fillMarkdownSlot(slot, scalar);
		else if (field.format === "sanitizedHtml") I.fillSanitizedHtmlSlot(slot, scalar);
		else slot.textContent = scalar;
		return scalar;
	}

	function fieldGrid(fields, opts) {
		if (!Array.isArray(fields))
			throw new TypeError("JuneauViews.helpers: fieldGrid(fields, opts) requires fields to be an array of field descriptions.");
		opts = opts || {};
		var values = isPlainObject(opts.values) ? opts.values : {};

		var anyEditable = false;
		var fi;
		for (fi = 0; fi < fields.length; fi++) {
			if (!isPlainObject(fields[fi]) || fields[fi].data == null)
				throw new TypeError("JuneauViews.helpers: fieldGrid(fields, opts) - field " + fi + " requires a `data` key.");
			if (fields[fi].editable) anyEditable = true;
		}
		if (anyEditable && typeof opts.onFieldSave !== "function")
			throw new TypeError("JuneauViews.helpers: fieldGrid(fields, opts) requires opts.onFieldSave when a field is editable.");

		var gridValues = anyEditable ? copyPlain(values) : values;
		var editableLeaves = [];

		function openEditable(leaf) {
			var pending = [];
			for (var j = 0; j < editableLeaves.length; j++) {
				var other = editableLeaves[j];
				if (other === leaf) continue;
				if (!hasClass(other, "is-editing") && !hasClass(other, "is-saving")) continue;
				pending.push(other._jcEditable.requestCommitOrCancel());
			}
			if (pending.length === 0) {
				leaf._jcEditable.requestOpen();
				return Promise.resolve();
			}
			return Promise.all(pending).then(function () { leaf._jcEditable.requestOpen(); });
		}

		function paintViewSlot(slot, field, map) {
			clear(slot);
			paintFieldValue(slot, field, map, opts.renderers);
			if (!field.render && field.href) {
				var url = substituteFieldHref(field.href, map);
				if (url) {
					var a = document.createElement("a");
					a.href = url;
					while (slot.firstChild) a.appendChild(slot.firstChild);
					slot.appendChild(a);
				}
			}
		}

		var grid = document.createElement("dl");
		grid.className = "juneau-view-detail-fields juneau-view-helper-field-grid";
		if (opts.columns != null) grid.style.setProperty("--juneau-view-detail-columns", String(opts.columns));

		for (var i = 0; i < fields.length; i++) {
			var field = fields[i];

			var item = document.createElement("div");
			item.className = "juneau-view-detail-field" + (field.span ? " juneau-view-detail-field-span-" + field.span : "");

			var title = document.createElement("div");
			title.className = "juneau-view-detail-field-title";
			title.textContent = field.label != null ? field.label : field.data;
			item.appendChild(title);

			var valueWrap = document.createElement("dd");
			valueWrap.className = "juneau-view-detail-field-value";

			var slot = document.createElement("span");
			slot.className = "juneau-view-detail-field-value-slot";
			slot.setAttribute("data-juneau-field", field.data);

			if (field.editable) {
				var fieldType = field.type || "text";
				var raw = Object.hasOwn(gridValues, field.data) ? gridValues[field.data]
					: (fieldType === "checkbox" ? false : "");
				var leafOpts = {
					value: raw,
					type: fieldType,
					multiline: !!field.multiline,
					options: field.options,
					persist: field.persist || opts.persist || "blur",
					label: field.label != null ? field.label : String(field.data),
					onSave: (function (f) {
						return function (next) { return opts.onFieldSave(f.data, next, f); };
					})(field),
					onRequestOpen: function (leafNode) { return openEditable(leafNode); }
				};
				if (field.render || field.format || field.href) {
					leafOpts.paintView = (function (f) {
						return function (viewSlot, v) {
							gridValues[f.data] = v;
							paintViewSlot(viewSlot, f, gridValues);
						};
					})(field);
				}
				var leaf = editableField(leafOpts);
				slot.appendChild(leaf);
				editableLeaves.push(leaf);
				valueWrap.appendChild(slot);
			} else {
				paintFieldValue(slot, field, gridValues, opts.renderers);

				var valueNode = slot;
				if (!field.render && field.href) {
					var url = substituteFieldHref(field.href, gridValues);
					if (url) {
						var a = document.createElement("a");
						a.href = url;
						a.appendChild(slot);
						valueNode = a;
					}
				}
				valueWrap.appendChild(valueNode);
			}

			if (field.actions) valueWrap.appendChild(buildFieldActionBar(field.actions, field.data, opts.onAction));

			item.appendChild(valueWrap);
			grid.appendChild(item);
		}
		return grid;
	}

	// ================================================================================================
	// recordTable(catalog, rows, opts) - STATIC, READ-ONLY.  Not a DataTable - that boundary is F24.
	//
	// NOTE ON SCOPE: the design's own §9.1.2 text flags a per-row `actions` extension (F34, "routed to
	// opts.onAction") as an OPEN fork - "that fork is open and should be confirmed before build."  This
	// build does NOT implement it: doing so would mean presuming an answer to a question the design
	// itself says is still open, rather than a documented, test-normative call.  v1 ships catalog+rows
	// rendering, the empty state, and `opts.caption`; `actions`/`onAction` are left for whichever child
	// picks F34 up once it is confirmed.  Flagged in this child's build report.
	// ================================================================================================

	function recordTable(catalog, rows, opts) {
		if (!Array.isArray(catalog))
			throw new TypeError("JuneauViews.helpers: recordTable(catalog, rows, opts) requires catalog to be an array.");
		if (!Array.isArray(rows))
			throw new TypeError("JuneauViews.helpers: recordTable(catalog, rows, opts) requires rows to be an array.");
		opts = opts || {};

		if (rows.length === 0) return typeof opts.empty === "function" ? opts.empty() : defaultEmptyNode();

		var columns = catalog.filter(function (f) { return !f.span; });

		var table = document.createElement("table");
		table.className = "juneau-view-helper-record-table";
		if (opts.caption) {
			var caption = document.createElement("caption");
			caption.textContent = opts.caption;
			table.appendChild(caption);
		}

		var thead = document.createElement("thead");
		var headRow = document.createElement("tr");
		for (var c = 0; c < columns.length; c++) {
			var th = document.createElement("th");
			th.scope = "col";
			th.textContent = columns[c].label != null ? columns[c].label : columns[c].data;
			headRow.appendChild(th);
		}
		thead.appendChild(headRow);
		table.appendChild(thead);

		var tbody = document.createElement("tbody");
		for (var r = 0; r < rows.length; r++) {
			var rowValues = isPlainObject(rows[r]) ? rows[r] : {};
			var tr = document.createElement("tr");
			for (var ci = 0; ci < columns.length; ci++) {
				var field = columns[ci];
				var td = document.createElement("td");
				paintFieldValue(td, field, rowValues, null);
				if (!field.render && field.href) {
					var url = substituteFieldHref(field.href, rowValues);
					if (url) {
						var a = document.createElement("a");
						a.href = url;
						while (td.firstChild) a.appendChild(td.firstChild);
						td.appendChild(a);
					}
				}
				tr.appendChild(td);
			}
			tbody.appendChild(tr);
		}
		table.appendChild(tbody);
		return table;
	}

	// ================================================================================================
	// dataPane(spec) - §8.3's fetch -> status -> paint algorithm, factored out of the descriptor
	// (§9.1.2).  Returns a `populate` function: fn(paneEl, tabCtx) -> Promise<void>.
	// ================================================================================================

	function dataPane(spec) {
		if (!spec || typeof spec.load !== "function")
			throw new TypeError("JuneauViews.helpers: dataPane(spec) requires spec.load to be a function.");
		if (typeof spec.render !== "function")
			throw new TypeError("JuneauViews.helpers: dataPane(spec) requires spec.render to be a function.");

		return function (paneEl, tabCtx) {
			clear(paneEl);
			if (typeof spec.loading === "function") appendAll(paneEl, spec.loading());
			else I.renderAsyncStatus(paneEl, "loading");

			return Promise.resolve()
				.then(function () { return spec.load(tabCtx); })
				.then(
					function (data) {
						clear(paneEl);
						if (isEmptyResolution(data)) { appendAll(paneEl, typeof spec.empty === "function" ? spec.empty() : defaultEmptyNode()); return; }
						appendAll(paneEl, spec.render(data, tabCtx));
					},
					function (err) {
						if (isAbortError(err)) return; // silent: the row collapsed, nothing failed - paint nothing further
						clear(paneEl);
						if (isEmptyKindError(err)) { appendAll(paneEl, typeof spec.empty === "function" ? spec.empty() : defaultEmptyNode()); return; }
						if (typeof spec.error === "function") appendAll(paneEl, spec.error(err));
						else I.renderAsyncStatus(paneEl, "error", err && err.message ? String(err.message) : "Something went wrong.");
					}
				);
			// Never touches the REGION's own status (§8.5's rule for fetchDeclared, carried here): a pane is
			// not a region, and this function's whole DOM footprint is `paneEl` and nothing above it.
		};
	}

	// ================================================================================================
	// tabStrip(tabs, opts) - PROMOTED AND WIDENED (§9.1.2).  ONE node returned: the strip plus its pane
	// container.  Backward-compatible: `tabStrip([{id,label,pane}])` with no opts is unchanged.
	// ================================================================================================

	function validateTabs(tabs) {
		if (!Array.isArray(tabs) || tabs.length === 0)
			throw new TypeError("JuneauViews.helpers: tabStrip(tabs, opts) requires a non-empty array of tab entries.");
		var seen = {};
		for (var i = 0; i < tabs.length; i++) {
			var t = tabs[i];
			if (!t || t.id == null || t.label == null)
				throw new TypeError("JuneauViews.helpers: tabStrip entry " + i + " requires an id and a label.");
			var id = String(t.id);
			if (Object.hasOwn(seen, id))
				throw new TypeError("JuneauViews.helpers: tabStrip has a duplicate tab id \"" + id + "\".");
			seen[id] = true;
			var hasPane = Object.hasOwn(t, "pane") && t.pane != null;
			var hasPopulate = typeof t.populate === "function";
			if (hasPane === hasPopulate)
				throw new TypeError("JuneauViews.helpers: tabStrip entry \"" + id + "\" must supply EXACTLY ONE of `pane` or `populate`.");
		}
	}

	function firstEnabledIndex(tabs) {
		for (var i = 0; i < tabs.length; i++) if (!tabs[i].disabled) return i;
		return 0;
	}

	function tabStrip(tabs, opts) {
		validateTabs(tabs);
		opts = opts || {};
		var mode = opts.mode === "ribbon" ? "ribbon" : "tab";
		var signal = opts.signal || null;
		// A per-CALL random id, not a module-level counter: unique across co-existing strips without any
		// state that outlives this call (test 29's amended purity scan rejects a module-level counter
		// exactly like `detailStripSeq`).
		var instanceId = "juneau-helper-tabstrip-" + Math.random().toString(36).slice(2, 10);

		var strip = document.createElement("div");
		strip.className = "juneau-view-ribbon-group juneau-view-helper-tabstrip";
		strip.setAttribute("role", "tablist");
		strip.dataset.juneauStripMode = mode;

		var paneContainer = document.createElement("div");
		paneContainer.className = "juneau-view-helper-tabstrip-panes";

		var activeIndex = 0;
		if (opts.active != null) {
			for (var ai = 0; ai < tabs.length; ai++) if (String(tabs[ai].id) === String(opts.active) && !tabs[ai].disabled) { activeIndex = ai; break; }
		} else {
			activeIndex = firstEnabledIndex(tabs);
		}
		var activeId = String(tabs[activeIndex].id);

		var entries = tabs.map(function (t, i) {
			var id = String(t.id);
			var hasPane = Object.hasOwn(t, "pane") && t.pane != null;
			var hasPopulate = typeof t.populate === "function";

			var btn = document.createElement("button");
			btn.type = "button";
			btn.id = instanceId + "-tab-" + i;
			btn.setAttribute("role", "tab");
			btn.dataset.juneauStripTab = id;
			var isActive = id === activeId;
			btn.setAttribute("aria-selected", isActive ? "true" : "false");
			btn.tabIndex = isActive ? 0 : -1;
			if (t.disabled) { btn.disabled = true; btn.setAttribute("aria-disabled", "true"); }
			var labelSpan = document.createElement("span");
			labelSpan.textContent = t.label;
			btn.appendChild(labelSpan);

			var pane = document.createElement("div");
			pane.id = instanceId + "-panel-" + i;
			pane.setAttribute("role", "tabpanel");
			pane.setAttribute("aria-labelledby", btn.id);
			pane.className = "juneau-view-helper-tabstrip-pane";
			pane.hidden = !isActive;
			btn.setAttribute("aria-controls", pane.id);

			if (hasPane) pane.appendChild(t.pane);

			strip.appendChild(btn);
			paneContainer.appendChild(pane);

			return {
				id: id, btn: btn, pane: pane, disabled: !!t.disabled,
				filled: hasPane, populate: hasPopulate ? t.populate : null,
				lazy: hasPopulate ? (t.lazy !== false) : false
			};
		});

		function runPopulate(entry, reason) {
			if (entry.filled || !entry.populate) return;
			if (signal && signal.aborted) return; // rule 5: abort prevents any not-yet-run populate from running
			entry.filled = true; // set BEFORE awaiting: fill-once holds even if activation races the async populate
			var tabCtx = { tabId: entry.id, reason: reason, signal: signal };
			var result;
			try {
				result = entry.populate(entry.pane, tabCtx);
			} catch (err) {
				I.renderAsyncStatus(entry.pane, "error", err && err.message ? String(err.message) : "Something went wrong.");
				return;
			}
			if (result && typeof result.then === "function") {
				result.then(function () {}, function (err) {
					I.renderAsyncStatus(entry.pane, "error", err && err.message ? String(err.message) : "Something went wrong.");
				});
			}
		}

		// Build-time pass: the eagerly-opened tab (whichever is active) always populates now, with
		// reason:"initial" - and so does any OTHER tab whose author opted out of laziness (`lazy:false`).
		// Every remaining lazy, non-active tab waits for its first activation (reason:"activate").
		entries.forEach(function (entry, i) {
			if (entry.filled) return;
			if (entry.id === activeId || !entry.lazy) runPopulate(entry, "initial");
		});

		function activate(id) {
			var target = null;
			for (var i = 0; i < entries.length; i++) if (entries[i].id === id) { target = entries[i]; break; }
			if (!target || target.disabled) return;
			entries.forEach(function (e) {
				var isTarget = e === target;
				e.btn.setAttribute("aria-selected", isTarget ? "true" : "false");
				e.btn.tabIndex = isTarget ? 0 : -1;
				e.pane.hidden = !isTarget;
			});
			runPopulate(target, "activate");
			if (typeof opts.onActivate === "function") opts.onActivate(target.id, target.pane);
		}

		strip.addEventListener("click", function (e) {
			var btn = e.target && typeof e.target.closest === "function" ? e.target.closest("[role=\"tab\"]") : null;
			if (!btn || (typeof strip.contains === "function" && !strip.contains(btn)) || btn.disabled) return;
			activate(btn.dataset.juneauStripTab);
			if (typeof btn.focus === "function") btn.focus();
		});

		strip.addEventListener("keydown", function (e) {
			if (!e) return;
			var idx = -1;
			for (var i = 0; i < entries.length; i++) if (entries[i].btn.getAttribute("aria-selected") === "true") { idx = i; break; }
			var naive = I.detailTabTargetIndex(e.key, idx, entries.length);
			if (naive < 0) return;
			// Home/End land on a fixed end; ArrowLeft/Right land adjacent - either way, if that seat is
			// disabled, keep stepping in the SAME direction the key implies (wrapping) rather than declaring
			// defeat after one hop, so a disabled tab at either end of the strip can never trap the roving
			// tabindex on itself.
			var forward = (e.key === "ArrowRight" || e.key === "End");
			var next = naive;
			var guard = 0;
			while (entries[next].disabled && guard < entries.length) {
				next = forward ? (next + 1) % entries.length : (next - 1 + entries.length) % entries.length;
				guard++;
			}
			if (entries[next].disabled) return; // every tab in the strip is disabled - nothing to activate
			if (typeof e.preventDefault === "function") e.preventDefault();
			activate(entries[next].id);
			if (typeof entries[next].btn.focus === "function") entries[next].btn.focus();
		});

		var wrapper = document.createElement("div");
		wrapper.className = "juneau-view-helper-tabstrip-wrapper";
		wrapper.appendChild(strip);
		wrapper.appendChild(paneContainer);
		return wrapper;
	}

	// ================================================================================================
	// dateRange(spec, onChange) - net-new control
	// ================================================================================================

	function dateRange(spec, onChange) {
		spec = spec || {};
		var wrap = document.createElement("div");
		wrap.className = "juneau-view-helper-daterange";
		wrap.setAttribute("role", "group");
		if (spec.groupLabel) wrap.setAttribute("aria-label", String(spec.groupLabel));

		function makeField(labelText, key) {
			var label = document.createElement("label");
			label.className = "juneau-view-helper-daterange-field";
			var span = document.createElement("span");
			span.className = "juneau-view-helper-daterange-label";
			span.textContent = labelText;
			var input = document.createElement("input");
			input.type = "date";
			if (spec[key]) input.value = spec[key];
			if (spec.min) input.min = spec.min;
			if (spec.max) input.max = spec.max;
			label.appendChild(span);
			label.appendChild(input);
			return { label: label, input: input };
		}

		var fromField = makeField(spec.fromLabel || "From", "from");
		var toField = makeField(spec.toLabel || "To", "to");

		var status = document.createElement("p");
		status.className = "juneau-view-helper-daterange-status";
		status.setAttribute("role", "status");

		function fire() {
			var range = { from: fromField.input.value || null, to: toField.input.value || null };
			status.textContent = "";
			if (range.from && range.to && range.from > range.to) {
				status.textContent = "The from date must not be after the to date.";
				return;
			}
			if (typeof onChange === "function") onChange(range);
		}
		fromField.input.addEventListener("change", fire);
		toField.input.addEventListener("change", fire);

		wrap.appendChild(fromField.label);
		wrap.appendChild(toField.label);
		wrap.appendChild(status);
		return wrap;
	}

	// ================================================================================================
	// dropdown(spec, onChange) - net-new control
	// ================================================================================================

	function dropdown(spec, onChange) {
		spec = spec || {};
		if (!Array.isArray(spec.options))
			throw new TypeError("JuneauViews.helpers: dropdown(spec, onChange) requires spec.options to be an array.");
		var wrap = document.createElement("div");
		wrap.className = "juneau-view-helper-dropdown";
		var label = document.createElement("label");
		label.className = "juneau-view-helper-dropdown-label";
		var labelText = document.createElement("span");
		labelText.textContent = spec.label == null ? "" : String(spec.label);
		var select = document.createElement("select");
		if (spec.multiple) select.multiple = true;
		for (var i = 0; i < spec.options.length; i++) {
			var o = spec.options[i];
			var opt = document.createElement("option");
			opt.value = o.value;
			opt.textContent = o.label != null ? o.label : String(o.value);
			select.appendChild(opt);
		}
		if (!spec.multiple && spec.value != null) select.value = spec.value;
		select.addEventListener("change", function () {
			if (typeof onChange !== "function") return;
			onChange(select.value);
		});
		label.appendChild(labelText);
		label.appendChild(select);
		wrap.appendChild(label);
		return wrap;
	}

	// ================================================================================================
	// filterBuilder(spec, onChange) - net-new control
	// ================================================================================================

	function filterBuilder(spec, onChange) {
		spec = spec || {};
		if (!Array.isArray(spec.fields) || spec.fields.length === 0)
			throw new TypeError("JuneauViews.helpers: filterBuilder(spec, onChange) requires a non-empty spec.fields array.");

		var predicates = Array.isArray(spec.initial) ? spec.initial.slice() : [];

		function fieldOps(fieldData) {
			for (var i = 0; i < spec.fields.length; i++) {
				if (spec.fields[i].data === fieldData && Array.isArray(spec.fields[i].ops)) return spec.fields[i].ops;
			}
			return [{ value: "eq", label: "is" }, { value: "neq", label: "is not" }, { value: "contains", label: "contains" }];
		}

		var fieldSelect = document.createElement("select");
		for (var fi = 0; fi < spec.fields.length; fi++) {
			var fo = document.createElement("option");
			fo.value = spec.fields[fi].data;
			fo.textContent = spec.fields[fi].label || spec.fields[fi].data;
			fieldSelect.appendChild(fo);
		}

		var opSelect = document.createElement("select");
		function rebuildOps() {
			clear(opSelect);
			var ops = fieldOps(fieldSelect.value);
			for (var i = 0; i < ops.length; i++) {
				var oo = document.createElement("option");
				oo.value = ops[i].value;
				oo.textContent = ops[i].label;
				opSelect.appendChild(oo);
			}
		}
		rebuildOps();
		fieldSelect.addEventListener("change", rebuildOps);

		var valueInput = document.createElement("input");
		valueInput.type = "text";

		var chipRow = document.createElement("div");
		chipRow.className = "juneau-view-helper-filterbuilder-chips";

		function fieldLabelOf(fieldData) {
			for (var i = 0; i < spec.fields.length; i++) if (spec.fields[i].data === fieldData) return spec.fields[i].label || spec.fields[i].data;
			return fieldData;
		}
		function opLabelOf(fieldData, opValue) {
			var ops = fieldOps(fieldData);
			for (var i = 0; i < ops.length; i++) if (ops[i].value === opValue) return ops[i].label;
			return opValue;
		}

		function renderChips() {
			clear(chipRow);
			predicates.forEach(function (p, i) {
				var chip = document.createElement("span");
				chip.className = "juneau-view-helper-filterbuilder-chip";
				var textSpan = document.createElement("span");
				textSpan.textContent = fieldLabelOf(p.field) + " " + opLabelOf(p.field, p.op) + " " + p.value;
				chip.appendChild(textSpan);
				chip.appendChild(button({
					label: "Remove",
					onClick: function (idx) {
						return function () {
							predicates = predicates.filter(function (_, j) { return j !== idx; });
							renderChips();
							if (typeof onChange === "function") onChange(predicates.slice());
						};
					}(i)
				}));
				chipRow.appendChild(chip);
			});
		}
		renderChips();

		var addBtn = button({
			label: "Add filter",
			onClick: function () {
				predicates = predicates.concat([{ field: fieldSelect.value, op: opSelect.value, value: valueInput.value }]);
				renderChips();
				valueInput.value = "";
				if (typeof onChange === "function") onChange(predicates.slice());
			}
		});

		var row = document.createElement("div");
		row.className = "juneau-view-helper-filterbuilder-row";
		row.appendChild(fieldSelect);
		row.appendChild(opSelect);
		row.appendChild(valueInput);
		row.appendChild(addBtn);

		var wrap = document.createElement("div");
		wrap.className = "juneau-view-helper-filterbuilder";
		wrap.appendChild(row);
		wrap.appendChild(chipRow);
		return wrap;
	}

	// ================================================================================================
	// EXPORTS
	// ================================================================================================

	NS.helpers = {
		fieldGrid: fieldGrid,
		kvTable: kvTable,
		button: button,
		buttonRow: buttonRow,
		tabStrip: tabStrip,
		dataPane: dataPane,
		recordTable: recordTable,
		dateRange: dateRange,
		dropdown: dropdown,
		filterBuilder: filterBuilder,
		editableField: editableField,
		toast: toast,
		text: text,
		pill: pill,
		icon: icon
	};
})();
