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
 * juneau-views.js - client initializer for the Apache Juneau rich-view toolkit.
 *
 * On DOMContentLoaded it owns init for every table[data-juneau-view] element: it reads the table's id, finds the
 * matching <script type="application/json" id="juneau-view:<id>"> VIEW_META sidecar, JSON.parses it, and - CRUCIAL -
 * performs a FAIL-LOUD contract-version handshake: if the sidecar's `contractVersion` differs from the baked-in
 * JUNEAU_VIEW_CONTRACT_VERSION, it console.errors, renders a visible in-table banner, and REFUSES to init (rather
 * than silently mis-rendering).  Otherwise it builds the DataTables opts (columns[].render from the renderer
 * registry, serverSide/ajax per dataMode, order from defaultOrder field->index resolution, rowClassRules applied in
 * createdRow) and calls $(table).DataTable(opts), then wires the ribbon.
 *
 * The DataTables library itself (jQuery + DataTables JS/CSS) is NOT bundled - its license is not an ASF category-A
 * license - so it stays caller-provided (CDN or self-hosted).  The distinct `data-juneau-view` marker guarantees no
 * collision with juneau-datatables.js (which only touches data-juneau-datatable).
 *
 * Everything in the "PURE LOGIC LAYER" is DOM/jQuery/DataTables-free (plain data in, plain data out), per the
 * Option-B architecture check; the "DOM/JQUERY BINDING LAYER" is the thin shim that scans, parses, and binds.
 */
(function () {
	"use strict";

	// Contract-version handshake: MUST equal ViewDef.CONTRACT_VERSION / ViewsMixin.CONTRACT_VERSION (single source
	// of truth on the server).  The initializer fails loud when a sidecar's contractVersion differs.
	const JUNEAU_VIEW_CONTRACT_VERSION = "4";

	// The typed action-result contract version (ActionResult.CONTRACT_VERSION on the server).  This is a SEPARATE,
	// independently-versioned wire contract from VIEW_META - it is deliberately NOT aliased to
	// JUNEAU_VIEW_CONTRACT_VERSION, so the row-action submit result and the view sidecar version independently.  A
	// 2xx action-result whose contractVersion differs is rendered as a visible, non-optimistic UNKNOWN rather than
	// silently mis-read.
	const JUNEAU_ACTION_RESULT_CONTRACT_VERSION = "1";

	/**
	 * The bulk-mutate-actions contract version (BulkMutateDef.CONTRACT_VERSION on the server; {@code TODO-428}).
	 * A THIRD, independently-versioned wire contract - deliberately not aliased to either
	 * JUNEAU_VIEW_CONTRACT_VERSION or JUNEAU_ACTION_RESULT_CONTRACT_VERSION, so a bulk-actions-list revision can
	 * never force a VIEW_META (ViewDef) contract bump (R2/design-doc guard). A sidecar whose contractVersion
	 * differs is refused (fail-loud), leaving row selection itself fully functional - only the bulk toolbar is
	 * withheld, per the two-independent-opt-ins separability guarantee (HIGH-5).
	 */
	const JUNEAU_BULK_CONTRACT_VERSION = "1";

	/** Independently-versioned row-detail expand envelope ({@code RowDetailDef.CONTRACT_VERSION}). */
	const JUNEAU_ROW_DETAIL_CONTRACT_VERSION = "1";

	/**
	 * The nested-table shell contract version (NestedTableDef.CONTRACT_VERSION on the server).  A FOURTH,
	 * independently-versioned wire contract - deliberately NOT aliased to any of the three above, so a
	 * nested-table shell revision can never force a VIEW_META / row-detail / action-result / bulk contract bump.
	 * A nested-table wrapper whose data-juneau-nested-contract differs is refused (fail-loud), leaving the rest of
	 * the detail panel fully functional - only that one nested table is withheld.
	 */
	const JUNEAU_NESTED_CONTRACT_VERSION = "1";

	/**
	 * Nested-table DOM attribute names - MUST equal ViewTable's constants of the same names (NESTED_ATTR,
	 * NESTED_META_ATTR, NESTED_CONTRACT_ATTR, NESTED_SCOPE_PARAM_ATTR) on the server.  A nested table is a
	 * read-only DataTables view (columns/paging/sort/search only) inside a row-detail section, scoped to its parent
	 * row by merging ONE query parameter (named by NESTED_SCOPE_PARAM_ATTR) that carries the parent row id (stamped
	 * at instantiation into NESTED_PARENT_ID_ATTR).  None of this is part of the VIEW_META wire contract.
	 */
	const NESTED_ATTR = "data-juneau-nested";
	const NESTED_META_ATTR = "data-juneau-nested-meta";
	const NESTED_CONTRACT_ATTR = "data-juneau-nested-contract";
	const NESTED_SCOPE_PARAM_ATTR = "data-juneau-nested-scope-param";
	const NESTED_PARENT_ID_ATTR = "data-juneau-parent-id";
	const NESTED_INIT_ATTR = "data-juneau-nested-init";

	/**
	 * The declarative dialog-form contract version (TODO-445h): ModalDef.CONTRACT_VERSION / FormDef.CONTRACT_VERSION on
	 * the server, both "1".  Fail-loud ONLY when a form is present: a form-bearing modal-open envelope whose top-level
	 * contractVersion or nested form.contractVersion is missing or does not equal this baked-in value is a visible
	 * refusal and the dialog does NOT open.  A confirm-only modal (no form) is unversioned and is NEVER refused on a
	 * missing version (h5) - the naive {@code !== "1"} test must not run on that path.
	 */
	const JUNEAU_DIALOG_FORM_CONTRACT_VERSION = "1";

	/**
	 * TODO-428 selection/bulk DOM attribute names - MUST equal ViewTable's constants of the same names
	 * (SELECT_ATTR, ROW_ID_FIELD_ATTR, SELECT_ALL_ATTR, BULK_ATTR, BULK_SIDECAR_ID_PREFIX) on the server. Selection
	 * and bulk-mutation are two INDEPENDENT opt-ins (HIGH-5): a table carries SELECT_ATTR with or without
	 * BULK_ATTR, but never the reverse (ViewTable.of(..., BulkMutateDef) always stamps both, since a
	 * BulkMutateDef is only constructible against a SelectionDef it requires at compile time).  All of this is
	 * pure DOM-attribute signaling - none of it is part of the VIEW_META wire contract (R2 guard; see
	 * SelectionDef/BulkMutateDef's class javadocs on the server).
	 */
	const SELECT_ATTR = "data-juneau-select";
	const ROW_ID_ATTR = "data-juneau-row-id";
	const ROW_ID_FIELD_ATTR = "data-juneau-row-id-field";
	const SELECT_ALL_ATTR = "data-juneau-select-all";
	const BULK_ATTR = "data-juneau-bulk";
	const BULK_SIDECAR_ID_PREFIX = "juneau-view-bulk:";

	// TODO-445n table overflow discipline: the DT1 "Approach B" single-node wrap (the DT2 dogfood path uses the
	// CSS-only "Approach D" overflow box on the flex .dt-layout-cell instead - see juneau-views.css).
	const TABLE_SCROLL_CLASS = "juneau-view-table-scroll";
	// L12 A: a generic (Juneau-vocabulary) label applied to the scroll region ONLY when it actually overflows.
	const TABLE_SCROLL_LABEL = "Table, horizontally scrollable";

	const NS = window.JuneauViews = window.JuneauViews || {};
	NS.CONTRACT_VERSION = JUNEAU_VIEW_CONTRACT_VERSION;
	NS.ACTION_RESULT_CONTRACT_VERSION = JUNEAU_ACTION_RESULT_CONTRACT_VERSION;
	NS.BULK_CONTRACT_VERSION = JUNEAU_BULK_CONTRACT_VERSION;
	NS.ROW_DETAIL_CONTRACT_VERSION = JUNEAU_ROW_DETAIL_CONTRACT_VERSION;
	NS.NESTED_CONTRACT_VERSION = JUNEAU_NESTED_CONTRACT_VERSION;

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables)
	// ==================================================================================================================

	/**
	 * Shared page-size vocabulary (visual-parity design doc §4.B) - a client-side default for this pass (not a new
	 * ViewDef wire field; see the non-goal in design doc §11).  Consumed by buildOptions' default pageLength below
	 * AND by the unified paging ribbon's page-size menu (buildPageSizeMenu, below).
	 */
	const PAGE_SIZE_OPTIONS = [
		{ value: 25, label: "25 rows" },
		{ value: 100, label: "100 rows" },
		{ value: -1, label: "All rows" }
	];

	/** Resolves a column `data` key to its zero-based index in the view (-1 when absent). */
	function columnIndexOf(viewDef, dataKey) {
		const cols = viewDef.columns || [];
		for (let i = 0; i < cols.length; i++)
			if (cols[i].data === dataKey) return i;
		return -1;
	}

	/**
	 * The live DataTables index of `dataKey` in the ACTUAL `opts.columns` array.  Delegates to slice 4's
	 * {@code JuneauViews.config.dtIndex} when juneau-config.js is present; otherwise walks the same array
	 * (the no-config-js seam still has to fix the selection-offset off-by-one).  Returns -1 when absent.
	 */
	function liveDtIndex(dataKey, optsColumns) {
		if (NS.config && typeof NS.config.dtIndex === "function")
			return NS.config.dtIndex(dataKey, optsColumns);
		if (!optsColumns) return -1;
		for (let i = 0; i < optsColumns.length; i++)
			if (optsColumns[i] && optsColumns[i].data === dataKey) return i;
		return -1;
	}

	/**
	 * Resolves `defaultOrder` [{data,dir}] to DataTables' positional `order` [[colIndex, dir]] by field name (m2) -
	 * indices are not pinned server-side, so client-side column reorder stays correct.  Unknown fields are skipped.
	 * When `optsColumns` (the ACTUAL DataTables column array) is supplied, indices go through {@link #liveDtIndex}
	 * so a leading selection column cannot off-by-one the ordered field; a now-hidden ordered column falls back
	 * to the first visible orderable catalog column.
	 */
	function resolveOrder(viewDef, optsColumns) {
		const out = [];
		(viewDef.defaultOrder || []).forEach(function (e) {
			const idx = optsColumns ? liveDtIndex(e.data, optsColumns) : columnIndexOf(viewDef, e.data);
			if (idx < 0) return;
			if (optsColumns && optsColumns[idx] && optsColumns[idx].visible === false) return;
			out.push([idx, e.dir]);
		});
		if (out.length === 0 && optsColumns) {
			for (let i = 0; i < optsColumns.length; i++) {
				const c = optsColumns[i];
				if (!c || c.data == null || c.visible === false || c.orderable === false) continue;
				out.push([i, "asc"]);
				break;
			}
		}
		return out;
	}

	/** Loose value equality that also matches boolean true vs "true", 1 vs "1", etc. (JSON type coercion). */
	function valuesEqual(a, b) {
		return a === b || String(a) === String(b);
	}

	/**
	 * Evaluates a view's rowClassRules against one row, returning the list of CSS classes to add.  op grammar (§6.3):
	 * eq/ne compare row[field] to the rule value; present/absent test whether row[field] is non-null/non-empty.
	 */
	function evaluateRowClassRules(rules, rowData) {
		const out = [];
		(rules || []).forEach(function (r) {
			const v = rowData ? rowData[r.field] : undefined;
			const present = (v != null && v !== "");
			let match = false;
			switch (r.op) {
				case "eq": match = valuesEqual(v, r.value); break;
				case "ne": match = !valuesEqual(v, r.value); break;
				case "present": match = present; break;
				case "absent": match = !present; break;
				default: match = false;
			}
			if (match) out.push(r["class"]);
		});
		return out;
	}

	/**
	 * Whether `endpoint` is a same-origin path template: no `://`, no `//` prefix, no scheme colon-before-slash,
	 * and no `..` path segments.  Mirrors {@code RowDetailDef.isSafeDetailEndpoint}.
	 */
	function isSafeDetailUrl(url) {
		if (url == null || String(url).trim() === "") return false;
		const s = String(url);
		if (s.indexOf("://") >= 0) return false;
		if (s.indexOf("//") === 0) return false;
		const colon = s.indexOf(":");
		const slash = s.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;
		if (/(^|\/)\.\.(\/|$)/.test(s)) return false;
		return true;
	}

	/**
	 * Substitutes `{id}` via encodeURIComponent and re-checks the result is still a same-origin path.
	 * Returns null when the template or the substituted URL is unsafe.
	 */
	function substituteDetailUrl(template, id) {
		if (!isSafeDetailUrl(template) || id == null) return null;
		const encoded = encodeURIComponent(String(id));
		const url = String(template).split("{id}").join(encoded);
		return isSafeDetailUrl(url) ? url : null;
	}

	/** Coalesce key for an in-flight expand GET: row id + expander generation. */
	function detailCoalesceKey(rowId, generation) {
		return String(rowId) + ":" + generation;
	}

	/** Scalar expand-JSON values become strings; objects/arrays/undefined become "". */
	function scalarFieldValue(v) {
		if (v == null) return "";
		const t = typeof v;
		if (t === "string" || t === "number" || t === "boolean") return String(v);
		return "";
	}

	/** Loud-handshake check for the expand GET envelope. */
	function detailContractOk(body, expected) {
		return !!(body && typeof body === "object" && body.contractVersion === (expected || JUNEAU_ROW_DETAIL_CONTRACT_VERSION));
	}

	/** True when a settled GET must be discarded (child gone or generation mismatch). */
	function shouldDropDetailPayload(childShown, expectedGen, actualGen) {
		return !childShown || expectedGen !== actualGen;
	}

	/**
	 * Resolves a row's STABLE selection id (MED-11) from its data, via the `rowIdField` key the host declared in
	 * its {@code SelectionDef} (carried client-side only via the {@code ROW_ID_FIELD_ATTR} DOM attribute - never
	 * VIEW_META).  Deliberately NEVER a DOM/table index: a poll/sort/page tick reshuffles which data occupies
	 * which row index, but this always resolves to the SAME id for the SAME underlying record.  Returns
	 * `undefined` when `rowData`/`rowIdField` is absent, or when the row data has no such key - callers must
	 * treat that as "this row cannot be selected" rather than inventing a fallback identity (e.g. a DOM index).
	 */
	function rowIdOf(rowData, rowIdField) {
		return (rowData && rowIdField) ? rowData[rowIdField] : undefined;
	}

	/**
	 * The persistence rule (Q2/MED-11): given the currently-selected ids and the ids actually present in the
	 * latest draw (sort/page/poll), returns the subset of `selectedIds` that are STILL present - silently
	 * DROPPING any id that has left the current page/result set, so a selection can never be kept "live" for a
	 * row that is no longer on screen (closing the "bulk action hits the wrong target" case).  Pure - no DOM, no
	 * Set/Map identity assumptions on the caller's side (plain arrays in, plain array out); ids are compared as
	 * strings so a numeric id and its string form are never silently treated as different rows.
	 */
	function pruneSelection(selectedIds, currentIds) {
		const present = {};
		(currentIds || []).forEach(function (id) { if (id != null) present[String(id)] = true; });
		const out = [];
		(selectedIds || []).forEach(function (id) { if (id != null && Object.hasOwn(present, String(id))) out.push(id); });
		return out;
	}

	/**
	 * The default CSRF header name - MUST equal {@code LoopbackBoundary.DEFAULT_CSRF_HEADER} ("X-Csrf-Token") so the
	 * runtime and the server boundary agree by default.  A host may override the name per-table (see
	 * resolveCsrfHeaderName in the binding layer); absent an override the runtime sends this one.
	 */
	const DEFAULT_CSRF_HEADER = "X-Csrf-Token";

	/**
	 * The safe (non-state-changing) HTTP methods - mirrors {@code MethodSafety.SAFE_METHODS}.  A row action bound to
	 * one of these would skip Origin/CSRF/JSON at the server boundary (a CSRF-able write), so the runtime refuses to
	 * issue it.  The Java builder ({@code RowAction.Method}) already makes a safe method unexpressible; this is the
	 * client half of that same HIGH-7 refusal, for a hand-edited or stale sidecar.
	 */
	const SAFE_METHODS = { GET: 1, HEAD: 1, OPTIONS: 1, TRACE: 1 };

	/** Whether a method is safe (GET/HEAD/OPTIONS/TRACE) - null/absent is NOT safe (mirrors MethodSafety.isSafe). */
	function isSafeMethod(method) {
		return !!(method && Object.hasOwn(SAFE_METHODS, String(method).toUpperCase()));
	}

	/**
	 * The single, shared fail-closed token test.  Absent (null/undefined), present-but-empty, and
	 * present-but-WHITESPACE all count as blank - matching the actual control, {@code LoopbackBoundary.check}'s
	 * `presented == null || presented.isBlank()` (NOT `SynchronizerToken.matches`'s `isEmpty`, which would let a
	 * whitespace token through to a confusing server 403).  Client refusal here is defense-against-omission; the
	 * landed server-side {@code LoopbackBoundary} is the real security boundary.
	 */
	function isBlankToken(v) {
		return v == null || String(v).trim() === "";
	}

	/**
	 * Builds the fail-closed row-action request descriptor from a RowAction intent, a token, and a header name -
	 * pure, DOM/fetch-free (plain data in, plain data out) so the fail-closed contract is unit-testable without a
	 * browser.  Returns EITHER a `{refuse:true, reason}` marker (the caller renders a VISIBLE refusal) OR a
	 * ready-to-issue `{url, method, headers, body}`:
	 *   - a missing or SAFE method refuses (`reason:"safe-method"`) - HIGH-7;
	 *   - a blank/absent/whitespace token refuses (`reason:"missing-token"`) - HIGH-1 fail-closed;
	 *   - otherwise the body is JSON and the headers carry `Content-Type: application/json` (so the write passes
	 *     `LoopbackBoundary.isJson`) plus the CSRF token under `headerName` (defaulting to DEFAULT_CSRF_HEADER).
	 *
	 * The optional `extra` object is merged into the JSON body - the modal submit path (TODO-416) uses it to carry
	 * the server-minted `idempotencyKey` and the `targetId`, so a double-click/re-submit/browser-retry all carry the
	 * same key and the server can check the key's `(action, targetId)` binding.  A bare submit (no `extra`) sends
	 * exactly `{action}` as before.
	 */
	function buildActionRequest(action, token, headerName, extra) {
		if (!action || isSafeMethod(action.method) || !action.method)
			return { refuse: true, reason: "safe-method" };
		if (isBlankToken(token))
			return { refuse: true, reason: "missing-token" };
		const headers = { "Content-Type": "application/json" };
		headers[headerName || DEFAULT_CSRF_HEADER] = token;
		const payload = { action: action.id };
		if (extra) for (const k in extra) if (Object.hasOwn(extra, k) && extra[k] != null) payload[k] = extra[k];
		return {
			url: action.endpoint,
			method: action.method,
			headers: headers,
			body: JSON.stringify(payload)
		};
	}

	/** Maps a buildActionRequest refusal reason to the visible message shown in the row-action refusal banner. */
	function actionRefusalMessage(reason) {
		if (reason === "safe-method")
			return "action must use a non-safe method (POST/PUT/PATCH/DELETE)";
		if (reason === "missing-token")
			return "no CSRF token available - the page did not supply one, so the request was not sent";
		if (reason === "request-failed")
			return "the request could not be completed";
		return "the action was refused";
	}

	/**
	 * The frozen set of typed action-result outcome tokens (ActionResult.Outcome) - the four synchronous outcomes
	 * plus the two async terminal states reserved for TODO-425 (`cancelled`, `cancelled-after-effect`).  An outcome
	 * token not in this set is normalized to `unknown` (a visible, non-optimistic state - never an optimistic
	 * success).
	 */
	const ACTION_OUTCOMES = {
		"success": 1, "failure": 1, "refusal": 1, "unknown": 1, "cancelled": 1, "cancelled-after-effect": 1
	};

	/** JSON.parse guarded against a null/blank/malformed body - returns the parsed value or null, never throws. */
	function parseJsonSafe(text) {
		if (text == null || text === "") return null;
		try { return JSON.parse(text); } catch (e) { return null; }
	}

	/**
	 * Parses a 2xx action-submit body into a typed ActionResult, or null when the body is absent/malformed or does
	 * NOT carry the contract's load-bearing `outcome` discriminator.  A null return means "no typed result was
	 * carried" - the caller treats that as a bare success (the pre-416 behavior) rather than inventing an outcome.
	 */
	function parseActionResult(text) {
		const o = parseJsonSafe(text);
		if (!o || typeof o !== "object" || o.outcome == null) return null;
		return o;
	}

	/**
	 * Normalizes a typed ActionResult's outcome to a known token, mapping anything unrecognized to `unknown` - so an
	 * unknown/garbled outcome renders as a visible non-optimistic state, never as an optimistic success.
	 */
	function normalizeOutcome(result) {
		const o = result && result.outcome;
		return (o != null && Object.hasOwn(ACTION_OUTCOMES, o)) ? o : "unknown";
	}

	/**
	 * Parses a 2xx action-submit body into an ASYNC "job accepted" pointer (AsyncJobRef), or null when it is not
	 * one.  Whether an action is asynchronous is a property of the RESPONSE, not the declared RowAction (TODO-425):
	 * the same POST returns EITHER a terminal ActionResult (has `outcome`) OR this pointer (has `streamUrl`), so no
	 * new RowAction wire field is needed and the two shapes are disjoint.  A job pointer MUST carry a non-blank
	 * `streamUrl` (the SSE capability URL); a body without one is not a job pointer and falls through to the normal
	 * typed-result path.
	 */
	function parseJobStarted(text) {
		const o = parseJsonSafe(text);
		if (!o || typeof o !== "object") return null;
		if (isBlankToken(o.streamUrl)) return null;
		return o;
	}

	/**
	 * Builds the fail-closed cancel-request descriptor for an async job (a non-safe POST to the job's cancelUrl) -
	 * pure, DOM/fetch-free, mirroring buildActionRequest's fail-closed token contract.  Returns EITHER a
	 * `{refuse:true, reason}` marker OR a ready `{url, method:"POST", headers, body}`; the body is an empty JSON
	 * object so the write passes LoopbackBoundary.isJson, and the CSRF token rides `headerName`.  Server-side
	 * cancellation is authoritative (a client cannot be trusted to stop the work); this only asks for it.
	 */
	function buildJobCancelRequest(cancelUrl, token, headerName) {
		if (isBlankToken(cancelUrl))
			return { refuse: true, reason: "no-cancel-url" };
		if (isBlankToken(token))
			return { refuse: true, reason: "missing-token" };
		const headers = { "Content-Type": "application/json" };
		headers[headerName || DEFAULT_CSRF_HEADER] = token;
		return { url: cancelUrl, method: "POST", headers: headers, body: "{}" };
	}

	/**
	 * Builds a visible, non-optimistic transport-refusal classification for ANY non-2xx action response (HIGH-3),
	 * WITHOUT requiring the typed action-result schema.  Prefers the boundary's `X-Loopback-Boundary` reason header,
	 * then the small `{reason,message}` JSON envelope the LoopbackBoundaryFilter emits, then a status-derived
	 * fallback - so a 403/415/421 (or a missing/malformed body) always maps to a comprehensible named refusal rather
	 * than a generic/empty/silent failure on a security refusal.
	 */
	function transportRefusal(status, boundaryReason, envelope) {
		const code = ! isBlankToken(boundaryReason) ? boundaryReason
			: (envelope && envelope.reason ? envelope.reason : ("http:" + (status || 0)));
		let message = envelope && envelope.message ? envelope.message : null;
		if (isBlankToken(message)) message = transportStatusMessage(status);
		return { code: code, message: message };
	}

	/** A comprehensible fallback message for the transport statuses the LoopbackBoundary answers with. */
	function transportStatusMessage(status) {
		if (status === 403) return "the request was refused by the server boundary (403)";
		if (status === 415) return "the request body was not accepted as JSON (415)";
		if (status === 421) return "the request was misdirected - Host did not match (421)";
		return "the request was refused (" + (status == null ? "no status" : status) + ")";
	}

	/**
	 * The visible text for a settled action outcome - a pure (data-in/text-out) mapping over every outcome the typed
	 * result can carry AND the transport-refusal case, so no terminal state is silent.  `cls` is
	 * `{outcome, refusalCode, message, replay, transport}`.
	 */
	function actionOutcomeMessage(cls) {
		const replay = cls.replay ? " (replay of a previous attempt)" : "";
		const detail = cls.message ? ": " + cls.message : "";
		switch (cls.outcome) {
			case "success": return "Done" + detail + replay + ".";
			case "failure": return "Failed" + detail + ".";
			case "refusal": return (cls.transport ? "Request refused" : "Refused") + " (" + (cls.refusalCode || "unknown") + ")" + detail + ".";
			case "cancelled": return "Cancelled" + detail + ".";
			case "cancelled-after-effect": return "Cancelled after a partial effect" + detail + ".";
			default: return "Outcome unknown - the write may or may not have completed" + detail + ".";
		}
	}

	/** Whether an action is presented as a modal dialog (`present=dialog`) - the TODO-416 modal/form path. */
	function isDialogAction(action) {
		return !! (action && action.present === "dialog");
	}

	/**
	 * The minimum honored polling interval, in milliseconds - mirrors {@code ViewDef.MIN_POLL_INTERVAL_MS}.  The
	 * server is the authoritative clamp (a `pollIntervalMs` value already arriving in VIEW_META has already been
	 * floored there); this client-side copy is defense-in-depth only, so a hand-edited or otherwise-malformed
	 * sidecar can't push this runtime below the floor either.
	 */
	// Deliberately kept as `var` (not `const`): TablePolling_Wiring_Test#a01 pins this exact declaration text as
	// part of the server/client MIN_POLL_INTERVAL_MS parity contract; see that test before touching this line.
	var MIN_POLL_INTERVAL_MS = 5000;

	/** Clamps a declared poll interval up to {@link #MIN_POLL_INTERVAL_MS} (mirrors the server-side clamp). */
	function clampPollInterval(ms) {
		return Math.max(ms, MIN_POLL_INTERVAL_MS);
	}

	/**
	 * Formats an elapsed-time duration (milliseconds) as a short staleness-age label ("just now", "5s ago",
	 * "2m ago", "1h ago").  Pure - the caller supplies the already-computed elapsed `ms` rather than this
	 * function reading the clock itself, so it stays independently testable without faking `Date.now()`.
	 */
	function formatStalenessAge(ms) {
		if (ms < 1000) return "just now";
		const s = Math.floor(ms / 1000);
		if (s < 60) return s + "s ago";
		const m = Math.floor(s / 60);
		if (m < 60) return m + "m ago";
		const h = Math.floor(m / 60);
		return h + "h ago";
	}

	/** Formats a non-negative integer with thousands separators (e.g. 1463 -> "1,463"), matching IRS's paging summary style. */
	function formatThousands(n) {
		const s = String(Math.trunc(Math.abs(n)));
		let out = "";
		for (let i = 0; i < s.length; i++) {
			if (i > 0 && (s.length - i) % 3 === 0) out += ",";
			out += s.charAt(i);
		}
		return (n < 0 ? "-" : "") + out;
	}

	/**
	 * Builds the compact "n-n of n" paging-summary string (visual-parity design doc §4.C, item 3) from a
	 * DataTables `page.info()`-shaped object - pure, DOM/library-free (`pageInfo` is plain data).  Mirrors
	 * DataTables' own default `language.info` convention (1-based inclusive display range: `start+1` to `end`)
	 * against `recordsDisplay` (the FILTERED count - the same number that default info text's `_TOTAL_`
	 * placeholder shows), falling back to `recordsTotal` if `recordsDisplay` is absent.  An empty result set
	 * (`total` falsy) renders "0-0 of 0" rather than "1-0 of 0".
	 */
	function pagingSummaryText(pageInfo) {
		const total = pageInfo.recordsDisplay != null ? pageInfo.recordsDisplay : pageInfo.recordsTotal;
		if (!total) return "0-0 of 0";
		return (pageInfo.start + 1) + "-" + pageInfo.end + " of " + formatThousands(total);
	}

	/**
	 * Computes the unified paging pill's selected/disabled state from a DataTables `page.info()`-shaped object
	 * (visual-parity design doc §4.C) - pure, DOM/library-free (`pageInfo` is plain data, never the DataTables API
	 * object itself).  `first`/`previous` disable on the first page; `next`/`last` disable on the last page,
	 * INCLUDING the always-disabled-when-there-is-only-one-page case (`pages === 0`, e.g. an empty result set, is
	 * treated the same as "already on the last page").
	 */
	function pillState(pageInfo, pageLength) {
		const page = pageInfo.page, pages = pageInfo.pages;
		return {
			selectedLength: pageLength,
			firstDisabled: page === 0,
			prevDisabled: page === 0,
			nextDisabled: pages === 0 || page === pages - 1,
			lastDisabled: pages === 0 || page === pages - 1
		};
	}

	/**
	 * Builds one DataTables column definition from a VIEW_META Column, binding its render id to the registry.  In
	 * SERVER mode only the display facet runs (type !== "display" returns the raw value - the server did sort/filter).
	 * An unknown render id warns once and falls back to the raw value (never throws).
	 */
	function buildColumnDef(col, deps) {
		const def = {
			data: col.data,
			orderable: col.orderable !== false,
			searchable: col.searchable !== false,
			// The server's JSON serializer omits null-valued properties entirely, so a nullable column is simply
			// ABSENT (undefined) on some rows.  Per datatables.net/tn/4, DataTables' data accessor throws the
			// "Requested unknown parameter" warning for undefined/null cell data unless columns.defaultContent is
			// set - it's only consulted at that data-resolution step, so it never runs (and never suppresses a
			// renderer) when the column's data IS present; present-but-null values still flow into `render` below,
			// which already coerces null to "".
			defaultContent: ""
		};
		if (col.title != null) def.title = col.title;
		if (col.name != null) def.name = col.name;
		if (col.className != null) def.className = col.className;
		if (col.visible === false) def.visible = false;

		if (col.render) {
			const spec = deps.parseRenderId(col.render);
			const renderer = deps.resolveRenderer(spec.id);
			if (!renderer) {
				deps.warn("Juneau view: unknown render id '" + spec.id + "' - falling back to raw value.");
			}
			const meta = mergeMeta(spec.meta, col);
			const popover = spec && spec.popover;
			if ((renderer && renderer.display) || popover) {
				def.render = function (data, type, rowData) {
					if (type && type !== "display") return data;   // SERVER mode: sort/filter/type done server-side
					let html = data == null ? "" : String(data);
					if (renderer && renderer.display) {
						try { html = renderer.display(data, rowData, meta); }
						catch (e) { html = data == null ? "" : String(data); }
						if (html == null) html = "";
						else html = String(html);
					}
					return appendPopoverTrigger(html, col, spec);
				};
			}
		}
		return def;
	}

	function viewEscAttr(s) {
		return (NS._render && typeof NS._render.escAttr === "function") ? NS._render.escAttr(s) : String(s);
	}

	/**
	 * Appends a sibling popover trigger after the renderer's HTML.  Never wraps the renderer output (so a
	 * {@code linked} {@code <a>} is not nested in a button).  Dynamic bits go through escAttr.
	 */
	function appendPopoverTrigger(html, col, spec) {
		const popover = spec && spec.popover;
		if (!popover) return html;
		const name = popover.title != null && String(popover.title).trim() !== "" ? String(popover.title) : "Details";
		const colData = col && col.data != null ? String(col.data) : "";
		return html + '<button type="button" class="jc-cell-popover-trigger"'
			+ ' aria-expanded="false" aria-haspopup="dialog" aria-label="' + viewEscAttr(name) + '"'
			+ ' data-juneau-popover="1" data-juneau-popover-col="' + viewEscAttr(colData) + '"></button>';
	}

	/** Merges a column's render.meta with runtime column context (href is needed by the `linked` renderer). */
	function mergeMeta(renderMeta, col) {
		const meta = {};
		if (renderMeta) for (const k in renderMeta) if (Object.hasOwn(renderMeta, k)) meta[k] = renderMeta[k];
		if (col.href != null) meta.href = col.href;
		meta.column = col.data;
		return meta;
	}

	/**
	 * Builds the DataTables options object from a VIEW_META view.  `deps` supplies the pure renderer-registry hooks
	 * (parseRenderId/resolveRenderer/warn) and the ribbon param contributor (ribbonParams(activeState)).  The
	 * serverSide fork (§6.7): server -> serverSide:true + ajax{dataSrc:"data"} (with ribbon active-toggle params
	 * merged into the request); client -> serverSide:false + ajax{dataSrc:""}.
	 */
	function buildOptions(viewDef, deps) {
		const opts = {};
		// Catalog-column defs only.  Selection / actions / order are assembled AFTERWARDS from the full
		// `opts.columns` array (see assembleFullColumnArray) so a leading selection column cannot off-by-one
		// defaultOrder or the ribbon/search indices.  `deps.effectiveColumns` is the post-chooser model when
		// present; otherwise the sidecar catalog.
		const catalog = (deps && deps.effectiveColumns) || viewDef.columns || [];
		opts.columns = catalog.map(function (c) { return buildColumnDef(c, deps); });
		// Text polish (design doc §4.B): the native search input's label is blanked (searchPlaceholder replaces it
		// as the input's placeholder attribute); the native length-select's language.lengthMenu is deliberately NOT
		// set here - Task 9's paging pill hides and replaces that control entirely, so there is nothing left for it
		// to style (design doc §4.B's resolved ambiguity).
		opts.language = { search: "", searchPlaceholder: "Search" };
		opts.pageLength = PAGE_SIZE_OPTIONS[0].value;
		// Disable DataTables' own "Showing X to Y of Z entries" line at the source (rather than CSS-hiding it
		// after the fact) - the unified paging pill (buildPagingPill/buildToolbarRow below) fully replaces it.
		opts.info = false;

		if (viewDef.dataMode === "server") {
			opts.serverSide = true;
			opts.ajax = {
				url: viewDef.dataUrl,
				dataSrc: "data",
				data: function (d) {
					const extra = deps.ribbonParams ? deps.ribbonParams() : {};
					for (const k in extra) if (Object.hasOwn(extra, k)) d[k] = extra[k];
					return d;
				}
			};
		} else {
			opts.serverSide = false;
			opts.ajax = { url: viewDef.dataUrl, dataSrc: "" };
		}

		// Nested-table parent scoping (445-family read-only nested ajax).  `deps.nestedScope`, present ONLY for a
		// nested table, merges exactly ONE extra query parameter (its declared scope-param name -> the parent row id)
		// into the GET in BOTH data modes.  The parent id is read through a getter at REQUEST time (off the live
		// data-juneau-parent-id attribute) so the scope stays correct even if the nested table is re-init'd against a
		// different parent row.  This is deliberately nested-only: a top-level view never carries deps.nestedScope.
		if (deps && deps.nestedScope)
			applyNestedScope(opts, deps.nestedScope);

		opts.createdRow = function (rowEl, rowData) {
			evaluateRowClassRules(viewDef.rowClassRules, rowData).forEach(function (cls) {
				if (cls) rowEl.className += (rowEl.className ? " " : "") + cls;
			});
			// Marks every row as expandable when the view declares a details field list - initDetailsExpander
			// (below) delegates its click listener off this class rather than binding one handler per row.
			if (deps.hasRowDetail) {
				rowEl.className += (rowEl.className ? " " : "") + "juneau-view-detail-row";
			}
		};

		return opts;
	}

	/**
	 * Merges a nested table's parent-scope parameter into an already-built `opts.ajax`, wrapping any pre-existing
	 * `data` contributor (the server-mode ribbon-param merger) rather than clobbering it.  `scope` is
	 * `{param, parentId}` where `param` is the query-parameter name and `parentId` is a getter (or a plain value)
	 * read at request time.  A null/blank parent id contributes nothing (the request goes out unscoped rather than
	 * with an empty param) - a fail-closed caller withholds init entirely when the parent id is unknown, so this
	 * only guards against a transient blank.  Pure: mutates the passed `opts` and touches no DOM/jQuery.
	 */
	function applyNestedScope(opts, scope) {
		if (!opts || !opts.ajax || !scope || !scope.param) return;
		const prior = opts.ajax.data;
		opts.ajax.data = function (d) {
			let out = d;
			if (typeof prior === "function") { const r = prior(d); if (r !== undefined) out = r; }
			const pid = typeof scope.parentId === "function" ? scope.parentId() : scope.parentId;
			if (pid != null && String(pid) !== "") out[scope.param] = pid;
			return out;
		};
	}

	// ==================================================================================================================
	// DOM / JQUERY BINDING LAYER  (thin shim)
	// ==================================================================================================================

	function warn(msg) { if (window.console && console.warn) console.warn(msg); }
	function error(msg) { if (window.console && console.error) console.error(msg); }

	/**
	 * Builds one icon-only 32px toolbar button under the given `className` (visual-parity design doc §4.A/§4.C).
	 * Deliberately self-contained rather than reusing juneau-ribbon.js's button(...) helper (Design decision #3):
	 * this module's buttons are not RibbonActions and have no built-in-id/symbol-override duality to resolve, so
	 * they resolve their fixed glyph names directly via window.JuneauViews.icons.resolveIcon(...) - the two files
	 * share the icon mechanism without either depending on the other's toolbar-construction logic.  Falls back to
	 * rendering the label as text when the glyph isn't registered (same convention as juneau-ribbon.js's button()).
	 */
	function toolbarButton(className, label, iconName, onClick) {
		const b = document.createElement("button");
		b.type = "button";
		b.className = className;
		b.title = label;
		b.setAttribute("aria-label", label);
		const icons = window.JuneauViews && window.JuneauViews.icons;
		const markup = icons?.resolveIcon ? icons.resolveIcon(iconName) : null;
		if (markup != null) {
			b.innerHTML = markup;
		} else {
			b.textContent = b.title;
		}
		b.addEventListener("click", onClick);
		return b;
	}

	/** One paging-ribbon nav segment - see toolbarButton(...). */
	function pagingPillButton(label, iconName, onClick) {
		return toolbarButton("juneau-view-pagingpill-btn", label, iconName, onClick);
	}

	/**
	 * Builds the central "n-n of n" segment of the unified paging ribbon (visual-parity follow-up: consolidates
	 * the old standalone page-size <select> INTO this segment rather than keeping it as a separate control).  It
	 * is a WAI-ARIA "menu button": the button's own visible content IS the compact range summary (so it never
	 * shows a redundant "Page size" label), `aria-haspopup="listbox"` + `aria-expanded` mark it as a popup
	 * trigger, and the popup is a `role="listbox"` list with one `role="option"` per `PAGE_SIZE_OPTIONS` entry -
	 * the SAME option set/labels the old <select> offered.  Selecting an option calls the same DataTables
	 * `page.len(n).draw()` API the old <select>'s `change` handler used.
	 *
	 * <p>Keyboard: Enter/Space/ArrowDown on the button opens the popup (focusing its currently-selected option, or
	 * the first one); ArrowUp/ArrowDown move focus between options while open; Enter/Space on a focused option
	 * selects it; Escape closes the popup and returns focus to the button (also true of a plain option click, and
	 * of focus leaving the control entirely).
	 */
	function buildPageSizeMenu(ctx) {
		const wrap = document.createElement("span");
		wrap.className = "juneau-view-pagingpill-menuwrap";

		const btn = document.createElement("button");
		btn.type = "button";
		btn.className = "juneau-view-pagingpill-menubtn";
		btn.title = "Rows per page";
		btn.setAttribute("aria-haspopup", "listbox");
		btn.setAttribute("aria-expanded", "false");

		const infoEl = document.createElement("span");
		infoEl.className = "juneau-view-pagingpill-info";
		btn.appendChild(infoEl);

		const icons = window.JuneauViews && window.JuneauViews.icons;
		const caretMarkup = icons?.resolveIcon ? icons.resolveIcon("expand_more") : null;
		const caretEl = document.createElement("span");
		caretEl.className = "juneau-view-pagingpill-caret";
		caretEl.setAttribute("aria-hidden", "true");
		if (caretMarkup != null) caretEl.innerHTML = caretMarkup;
		btn.appendChild(caretEl);

		const menuEl = document.createElement("ul");
		menuEl.className = "juneau-view-pagingpill-menu";
		menuEl.setAttribute("role", "listbox");
		menuEl.hidden = true;

		const options = NS.init.PAGE_SIZE_OPTIONS.map(function (o) {
			const optEl = document.createElement("li");
			optEl.className = "juneau-view-pagingpill-menu-option";
			optEl.setAttribute("role", "option");
			optEl.tabIndex = -1;
			optEl.textContent = o.label;
			optEl.addEventListener("click", function () { selectOption(o.value); });
			menuEl.appendChild(optEl);
			return { value: o.value, el: optEl };
		});

		function indexOfSelected() {
			for (let i = 0; i < options.length; i++) if (options[i].el.getAttribute("aria-selected") === "true") return i;
			return -1;
		}

		function indexOfFocused() {
			for (let i = 0; i < options.length; i++) if (options[i].el === document.activeElement) return i;
			return -1;
		}

		function openMenu() {
			menuEl.hidden = false;
			btn.setAttribute("aria-expanded", "true");
			const idx = indexOfSelected();
			options[idx >= 0 ? idx : 0].el.focus();
		}

		function closeMenu(returnFocusToButton) {
			menuEl.hidden = true;
			btn.setAttribute("aria-expanded", "false");
			if (returnFocusToButton) btn.focus();
		}

		function selectOption(value) {
			ctx.dataTable.page.len(value).draw();
			closeMenu(true);
		}

		btn.addEventListener("click", function () {
			if (menuEl.hidden) openMenu(); else closeMenu(false);
		});
		btn.addEventListener("keydown", function (e) {
			if (e.key === "ArrowDown" || e.key === "Enter" || e.key === " ") {
				e.preventDefault();
				openMenu();
			}
		});
		menuEl.addEventListener("keydown", function (e) {
			const idx = indexOfFocused();
			if (e.key === "ArrowDown") {
				e.preventDefault();
				options[(idx + 1) % options.length].el.focus();
			} else if (e.key === "ArrowUp") {
				e.preventDefault();
				options[(idx - 1 + options.length) % options.length].el.focus();
			} else if (e.key === "Enter" || e.key === " ") {
				e.preventDefault();
				if (idx >= 0) selectOption(options[idx].value);
			} else if (e.key === "Escape") {
				e.preventDefault();
				closeMenu(true);
			}
		});
		// Closing on focus-out (rather than only on Escape/selection) covers a mouse click or Tab landing
		// anywhere outside the control - a real menu button must never leave its popup open once focus moves on.
		wrap.addEventListener("focusout", function (e) {
			if (!wrap.contains(e.relatedTarget)) closeMenu(false);
		});

		wrap.appendChild(btn);
		wrap.appendChild(menuEl);

		return {
			el: wrap,
			refresh: function (summaryText, selectedLength) {
				infoEl.textContent = summaryText;
				options.forEach(function (o) {
					o.el.setAttribute("aria-selected", String(o.value) === String(selectedLength) ? "true" : "false");
				});
			}
		};
	}

	/**
	 * Builds the unified paging ribbon - `[First] [Prev] [<range> + page-size menu] [Next] [Last]` - and wires it
	 * to the given DataTables instance (visual-parity follow-up).  Called unconditionally from initTable(...) -
	 * every view table gets one, regardless of whether it declares any ribbon actions.  Supersedes BOTH the old
	 * standalone page-size <select> (folded into buildPageSizeMenu's central segment above) AND the old
	 * right-side compact prev/next ribbon (removed entirely) - paging now exists in exactly ONE place.  Returns
	 * the bare pill element only - it no longer touches the DOM itself (buildToolbarRow(...) below owns all
	 * toolbar-row DOM insertion in one place).  There is no native ".dataTables_info"/".dt-info" node to move -
	 * buildOptions(...) sets `info: false`, so DataTables never creates that node in the first place.
	 */
	function buildPagingPill(viewDef, ctx) {
		const pill = document.createElement("div");
		pill.className = "juneau-view-pagingpill";
		pill.setAttribute("data-testid", "paging");

		const firstBtn = pagingPillButton("First page", "first_page", function () { ctx.dataTable.page("first").draw(); });
		const prevBtn = pagingPillButton("Previous page", "chevron_left", function () { ctx.dataTable.page("previous").draw(); });
		const sizeMenu = buildPageSizeMenu(ctx);
		const nextBtn = pagingPillButton("Next page", "chevron_right", function () { ctx.dataTable.page("next").draw(); });
		const lastBtn = pagingPillButton("Last page", "last_page", function () { ctx.dataTable.page("last").draw(); });
		pill.appendChild(firstBtn);
		pill.appendChild(prevBtn);
		pill.appendChild(sizeMenu.el);
		pill.appendChild(nextBtn);
		pill.appendChild(lastBtn);

		function refreshPillState() {
			const info = ctx.dataTable.page.info();
			const st = pillState(info, ctx.dataTable.page.len());
			firstBtn.disabled = st.firstDisabled;
			prevBtn.disabled = st.prevDisabled;
			nextBtn.disabled = st.nextDisabled;
			lastBtn.disabled = st.lastDisabled;
			sizeMenu.refresh(pagingSummaryText(info), st.selectedLength);
		}
		// Guard against a nested table's draw.dt bubbling up the DOM to this parent-table handler (a nested
		// DataTable lives inside the parent's expanded child row): only this table's own draw refreshes its pill.
		ctx.dataTable.on("draw.dt", function (e) { if (e && e.target !== ctx.table) return; refreshPillState(); });
		refreshPillState();   // correct initial disabled state before the first draw.dt fires

		return pill;
	}

	/**
	 * Builds and inserts the per-column search `<tr>` into `table`'s `<thead>` (visual-parity §4, item 4 - fixes
	 * the columnSearchToggle button, which toggled `ctx.columnSearchOn` but had nothing wired to
	 * `ctx.onColumnSearchToggle` to actually show/hide or filter anything).  One text input per SEARCHABLE column
	 * (a non-searchable column gets an empty cell, keeping column count/alignment intact); each input's `input`
	 * event applies a simple per-column text filter via `dt.column(idx).search(value).draw()`.  Starts hidden
	 * (`ctx.onColumnSearchToggle`, wired by the caller, toggles visibility).  Returns null when the table has no
	 * `<thead>` (defensive; every juneau view table has one).
	 */
	function buildColumnSearchRow(table, optsColumns, dt) {
		const thead = table.querySelector("thead");
		if (!thead) return null;
		const row = document.createElement("tr");
		row.className = "juneau-view-columnsearch-row";
		row.setAttribute("data-testid", "col-search-row");
		row.style.display = "none";
		(optsColumns || []).forEach(function (col, idx) {
			const th = document.createElement("th");
			const isSynthetic = !col || col.data == null;
			const isHidden = col && col.visible === false;
			if (isHidden) th.style.display = "none";
			if (!isSynthetic && !isHidden && col.searchable !== false) {
				const input = document.createElement("input");
				input.type = "text";
				input.className = "juneau-view-columnsearch-input";
				const label = "Search " + (col.title || col.data || "column " + idx);
				input.placeholder = label;
				input.setAttribute("aria-label", label);
				input.addEventListener("input", function () { dt.column(idx).search(input.value).draw(); });
				th.appendChild(input);
			}
			row.appendChild(th);
		});
		thead.appendChild(row);
		return row;
	}

	/** Renders the fail-loud, visible in-table banner used on a contract-version mismatch (or a parse failure). */
	function renderBanner(table, message) {
		let caption = table.createCaption ? table.createCaption() : null;
		if (!caption) {
			caption = document.createElement("caption");
			table.insertBefore(caption, table.firstChild);
		}
		caption.className = "juneau-view-error";
		caption.textContent = message;
	}

	/**
	 * The DataTables wrapper that owns native search/length/paging chrome AND is the correct parent for the
	 * unified toolbar row.  DT1 wraps the {@code <table>} directly in {@code .dataTables_wrapper}.  DT2 nests the
	 * table inside {@code .dt-layout-row.dt-layout-table > .dt-layout-cell}, with {@code .dt-search} living in a
	 * *sibling* layout row under {@code .dt-container} — so {@code table.parentNode} is the cell, not the
	 * wrapper, and a querySelector there never finds the search box (it stays on its own row above the paging
	 * pill).  Walks up via {@code Element.closest}; falls back to {@code parentNode} for DT1 / non-DOM test
	 * doubles that have no {@code closest}.
	 */
	function findViewWrapper(table) {
		if (!table) return null;
		if (typeof table.closest === "function") {
			const found = table.closest(".dt-container, .dataTables_wrapper");
			if (found) return found;
		}
		return table.parentNode;
	}

	/**
	 * Assembles ONE unified toolbar row and inserts it as the FIRST child of `wrapper`, i.e. ABOVE the table (IRS
	 * reference layout).  Per the control-row layout spec: a LEFT cluster (`.juneau-view-toolbar-left`) holding
	 * just the unified paging ribbon (nav + page-size, left-aligned - the only place paging exists), and a RIGHT
	 * cluster (`.juneau-view-toolbar-right`, right-aligned via the row's `space-between`) holding, in order: the
	 * native DataTables search box, then the ribbon bar (already internally grouped into filter-ribbon/copy-
	 * download-ribbon clusters per juneau-ribbon.js).  `pill`/`bar` are each optional (a view with no ribbon
	 * actions still gets its pill-only row; a pill is always built by initTable(...) so it is realistically
	 * always present).  Moves DataTables' native ".dataTables_filter"/".dt-search" search box into the right
	 * cluster rather than leaving it in its own DataTables-generated wrapper; degrades gracefully (right cluster
	 * still built from whichever of search/bar exist) when no native search box is found (e.g. searching
	 * disabled).  Caller must pass the real DT wrapper from {@code findViewWrapper(table)}, not
	 * {@code table.parentNode} (which is a nested layout cell under DT2).
	 */
	function buildToolbarRow(wrapper, pill, bar) {
		const filterEl = wrapper.querySelector(".dataTables_filter, .dt-search");
		const row = document.createElement("div");
		row.className = "juneau-view-toolbar-row";

		const left = document.createElement("div");
		left.className = "juneau-view-toolbar-left";
		if (pill) left.appendChild(pill);

		const right = document.createElement("div");
		right.className = "juneau-view-toolbar-right";
		if (filterEl) right.appendChild(filterEl);
		if (bar) right.appendChild(bar);

		row.appendChild(left);
		row.appendChild(right);
		wrapper.insertBefore(row, wrapper.firstChild);
		return row;
	}

	/**
	 * Whether `table` currently has a row marked in-flight - a row-action implementation is expected to set
	 * `data-juneau-inflight` (any truthy attribute value) on a `<tr>` while its write is pending.  A poll landing
	 * mid-write must not overwrite that row with the pre-write server view, or the UI would appear to undo the
	 * user's action and then redo it once the write's own result repaints it (design doc §9.1 B5) - so a poll
	 * tick skips its ENTIRE redraw whenever ANY row in this table carries the marker, leaving the table exactly
	 * as-is (stale, but honestly so) until that write settles.
	 *
	 * <p>Scoped to SYNCHRONOUS writes ONLY: it reads `data-juneau-inflight`, NOT the async job marker
	 * (`data-juneau-job`, set by setRowJobRunning).  A long-running TODO-425 job must NOT freeze the whole table's
	 * polling for up to the 120s hard timeout - so it uses the distinct marker this function deliberately ignores
	 * (HIGH-9).
	 */
	function hasInFlightRow(table) {
		return !!table.querySelector("tbody tr[data-juneau-inflight]");
	}

	/** Whether `table` currently has a row streaming an async job (`data-juneau-job` marker). */
	function hasJobRow(table) {
		return !!table.querySelector("tbody tr[data-juneau-job]");
	}

	/**
	 * Visible Apply-refused notice (teardown step 1).  Never queued; the user re-applies once in-flight / job
	 * markers clear.  Painted with {@code textContent} only.
	 */
	function renderReinitNotice(table, message) {
		const host = table.parentNode || table;
		let el = host.querySelector ? host.querySelector(".juneau-view-reinit-notice") : null;
		if (!el) {
			el = document.createElement("div");
			el.className = "juneau-view-reinit-notice";
			el.setAttribute("role", "alert");
			el.setAttribute("data-testid", "reinit-notice");
			if (table.parentNode) table.parentNode.insertBefore(el, table);
			else table.insertBefore(el, table.firstChild);
		}
		el.textContent = message;
	}

	function clearReinitNotice(table) {
		const host = table.parentNode || table;
		const el = host.querySelector ? host.querySelector(".juneau-view-reinit-notice") : null;
		if (el && el.parentNode) el.parentNode.removeChild(el);
	}

	/**
	 * Builds the per-table staleness-indicator chip (per-table, never a single page-level chip).  Starts in the
	 * neutral "fresh" state; {@link #initPolling} drives every subsequent update.
	 */
	function buildStalenessIndicator() {
		const el = document.createElement("span");
		el.className = "juneau-view-staleness";
		el.setAttribute("data-testid", "staleness");
		el.setAttribute("data-state", "fresh");
		return el;
	}

	/**
	 * Locates the server-emitted {@code <template data-juneau-row-detail>} sibling of `table`.
	 *
	 * <p>ViewTable emits the template next to the {@code <table>}. DataTables 2 then wraps the table in
	 * {@code .dt-container > .dt-layout-cell}, so {@code table.parentNode} is the cell and no longer contains
	 * the template. After wrap, the template is a sibling of {@code .dt-container}; {@link #findViewWrapper}
	 * plus its parent is that host. Before wrap (and in the Node harness), {@code table.parentNode} is the host.
	 */
	function findRowDetailTemplate(table) {
		if (!table) return null;
		const wrapper = findViewWrapper(table);
		const host = wrapper && wrapper.parentNode && wrapper !== table.parentNode
			? wrapper.parentNode
			: table.parentNode;
		return host && host.querySelector ? host.querySelector("template[data-juneau-row-detail]") : null;
	}

	/**
	 * Tag names (uppercase) the markdown slot copier will create.  Everything else is unwrapped (children kept)
	 * except the drop-with-children set in {@link #fillMarkdownSlot}.
	 */
	const MARKDOWN_ALLOWED_TAGS = {
		P: 1, BR: 1, H1: 1, H2: 1, H3: 1, H4: 1, H5: 1, H6: 1,
		UL: 1, OL: 1, LI: 1, PRE: 1, CODE: 1, EM: 1, STRONG: 1, A: 1, BLOCKQUOTE: 1, HR: 1,
		TABLE: 1, THEAD: 1, TBODY: 1, TR: 1, TH: 1, TD: 1, DEL: 1, SUP: 1, SUB: 1
	};

	/** Tags whose children are discarded, not unwrapped (script body must not become text). */
	const MARKDOWN_DROP_TAGS = {
		SCRIPT: 1, STYLE: 1, IFRAME: 1, OBJECT: 1, EMBED: 1, LINK: 1, META: 1, BASE: 1, FORM: 1, INPUT: 1, SVG: 1, IMG: 1
	};

	/**
	 * Whether `href` is safe to copy onto an {@code <a>}: http(s), mailto, same-origin path, fragment, or a
	 * scheme-less relative URL.  javascript:/data:/vbscript: and other schemes are rejected.
	 */
	function isSafeMarkdownHref(href) {
		if (href == null) return false;
		const t = String(href).trim();
		if (!t) return false;
		const lower = t.toLowerCase();
		if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:"))
			return false;
		if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:"))
			return true;
		if (t.charAt(0) === "#" || t.charAt(0) === "/")
			return true;
		return lower.indexOf(":") < 0;
	}

	function clearElementChildren(el) {
		if (!el) return;
		if (typeof el.replaceChildren === "function") {
			el.replaceChildren();
			return;
		}
		while (el.firstChild)
			el.removeChild(el.firstChild);
	}

	function copyAllowedMarkdownAttrs(from, to) {
		if (!from || !to || typeof to.setAttribute !== "function") return;
		if (from.tagName === "A") {
			const href = typeof from.getAttribute === "function" ? from.getAttribute("href") : null;
			if (isSafeMarkdownHref(href))
				to.setAttribute("href", href);
		}
	}

	function copySanitizedMarkdownChildren(from, to, doc) {
		if (!from || !to || !from.childNodes) return;
		const kids = from.childNodes;
		for (let i = 0; i < kids.length; i++) {
			const n = kids[i];
			if (!n) continue;
			if (n.nodeType === 3) {
				const text = n.nodeValue == null ? "" : String(n.nodeValue);
				if (doc && typeof doc.createTextNode === "function")
					to.appendChild(doc.createTextNode(text));
				continue;
			}
			if (n.nodeType !== 1) continue;
			const tag = n.tagName ? String(n.tagName).toUpperCase() : "";
			if (MARKDOWN_DROP_TAGS[tag])
				continue;
			if (!MARKDOWN_ALLOWED_TAGS[tag]) {
				copySanitizedMarkdownChildren(n, to, doc);
				continue;
			}
			const dest = doc.createElement(tag.toLowerCase());
			copyAllowedMarkdownAttrs(n, dest);
			copySanitizedMarkdownChildren(n, dest, doc);
			to.appendChild(dest);
		}
	}

	/**
	 * Paints a markdown field slot from sanitizing-markdown HTML.  Parses with {@code DOMParser} (does not
	 * execute script) and copies allowlisted nodes via {@code createElement}/{@code createTextNode}.  Never
	 * assigns {@code innerHTML}.  Missing {@code DOMParser} fails closed to {@code textContent}.
	 */
	function fillMarkdownSlot(el, html) {
		clearElementChildren(el);
		if (html == null || html === "") return;
		const src = String(html);
		const doc = typeof document !== "undefined" ? document : null;
		const Parser = typeof DOMParser !== "undefined" ? DOMParser
			: (typeof window !== "undefined" ? window.DOMParser : null);
		if (!Parser || !doc || typeof doc.createElement !== "function") {
			el.textContent = src;
			return;
		}
		let parsed;
		try {
			parsed = new Parser().parseFromString("<div>" + src + "</div>", "text/html");
		} catch (e) {
			el.textContent = src;
			return;
		}
		const wrap = parsed && parsed.body && parsed.body.firstChild;
		if (!wrap) return;
		copySanitizedMarkdownChildren(wrap, el, doc);
	}

	const RENDER_ALLOWED_TAGS = { SPAN: 1, A: 1, CODE: 1, DIV: 1 };

	function copyRenderStyle(from, to) {
		if (typeof from.getAttribute !== "function") return;
		const raw = from.getAttribute("style");
		if (raw == null || raw === "") return;
		const m = /^\s*width\s*:\s*([+-]?\d+)\s*%?\s*;?\s*$/i.exec(String(raw));
		if (!m) return;
		const n = Number(m[1]);
		if (!Number.isInteger(n) || n < 0 || n > 100) return;
		to.setAttribute("style", "width:" + n + "%");
	}

	function copyAllowedRenderAttrs(from, to) {
		if (!from || !to || typeof to.setAttribute !== "function") return;
		if (typeof from.getAttribute !== "function") return;
		const cls = from.getAttribute("class");
		if (cls) to.setAttribute("class", cls);
		if (from.tagName === "A") {
			const href = from.getAttribute("href");
			if (isSafeMarkdownHref(href))
				to.setAttribute("href", href);
		}
		const title = from.getAttribute("title");
		if (title != null && title !== "")
			to.setAttribute("title", title);
		copyRenderStyle(from, to);
		const tabindex = from.getAttribute("tabindex");
		if (tabindex === "0")
			to.setAttribute("tabindex", "0");
		const ts = from.getAttribute("data-juneau-ts");
		if (ts) {
			const toDate = NS._render && NS._render.toDate;
			if (typeof toDate === "function" && toDate(ts))
				to.setAttribute("data-juneau-ts", ts);
		}
	}

	function copySanitizedRenderChildren(from, to, doc) {
		if (!from || !to || !from.childNodes) return;
		const kids = from.childNodes;
		for (let i = 0; i < kids.length; i++) {
			const n = kids[i];
			if (!n) continue;
			if (n.nodeType === 3) {
				const text = n.nodeValue == null ? "" : String(n.nodeValue);
				if (doc && typeof doc.createTextNode === "function")
					to.appendChild(doc.createTextNode(text));
				continue;
			}
			if (n.nodeType !== 1) continue;
			const tag = n.tagName ? String(n.tagName).toUpperCase() : "";
			if (MARKDOWN_DROP_TAGS[tag])
				continue;
			if (!RENDER_ALLOWED_TAGS[tag]) {
				copySanitizedRenderChildren(n, to, doc);
				continue;
			}
			const dest = doc.createElement(tag.toLowerCase());
			copyAllowedRenderAttrs(n, dest);
			copySanitizedRenderChildren(n, dest, doc);
			to.appendChild(dest);
		}
	}

	function resolveFillRenderer(id) {
		const sink = typeof NS.resolveSinkRenderer === "function" ? NS.resolveSinkRenderer(id) : null;
		if (sink) return sink;
		const frozen = NS._render && NS._render.frozenBuiltinIds;
		if (frozen && frozen.indexOf(id) >= 0)
			return null;
		return typeof NS.resolveRenderer === "function" ? NS.resolveRenderer(id) : null;
	}

	/**
	 * Paints a named-renderer detail-field slot.  Resolves built-ins through the frozen sink lookup, rebuilds
	 * the escaped HTML string through a closed tag/attribute allowlist.  Never assigns innerHTML.
	 */
	function fillRenderSlot(slot, value, id, meta, href, fields) {
		clearElementChildren(slot);
		if (value == null || value === "") return;
		const renderer = resolveFillRenderer(id);
		if (!renderer || typeof renderer.display !== "function") {
			slot.textContent = value;
			return;
		}
		const m = {};
		if (meta && typeof meta === "object") {
			for (const k in meta) if (Object.hasOwn(meta, k)) m[k] = meta[k];
		}
		if (href != null) m.href = href;
		let html;
		try { html = renderer.display(value, fields, m); }
		catch (e) {
			slot.textContent = value;
			return;
		}
		if (html == null || html === "") return;
		const doc = typeof document !== "undefined" ? document : null;
		const Parser = typeof DOMParser !== "undefined" ? DOMParser
			: (typeof window !== "undefined" ? window.DOMParser : null);
		if (!Parser || !doc || typeof doc.createElement !== "function") {
			slot.textContent = value;
			return;
		}
		let parsed;
		try {
			parsed = new Parser().parseFromString("<div>" + String(html) + "</div>", "text/html");
		} catch (e) {
			slot.textContent = value;
			return;
		}
		const wrap = parsed && parsed.body && parsed.body.firstChild;
		if (!wrap) return;
		copySanitizedRenderChildren(wrap, slot, doc);
	}

	/**
	 * Fills `[data-juneau-field]` slots from an expand JSON `fields` map.  TEXT slots (the default, and any
	 * unknown format) use textContent only.  {@code data-juneau-field-format="markdown"} slots use
	 * fillMarkdownSlot.  Unknown keys are dropped; missing keys and non-scalars become empty.
	 */
	function fillDetailSlots(root, fields) {
		if (!root || !root.querySelectorAll) return;
		const map = fields && typeof fields === "object" ? fields : {};
		const slots = root.querySelectorAll("[data-juneau-field]");
		for (let i = 0; i < slots.length; i++) {
			const slot = slots[i];
			const key = slot.getAttribute("data-juneau-field");
			const value = Object.hasOwn(map, key) ? scalarFieldValue(map[key]) : "";
			const renderId = slot.getAttribute("data-juneau-field-render");
			if (renderId) {
				let meta = {};
				const metaRaw = slot.getAttribute("data-juneau-field-render-meta");
				if (metaRaw) {
					try {
						const parsed = JSON.parse(metaRaw);
						if (parsed && typeof parsed === "object") meta = parsed;
					} catch (e) { meta = {}; }
				}
				const href = slot.getAttribute("data-juneau-field-render-href");
				fillRenderSlot(slot, value, renderId, meta, href, map);
			} else if (slot.getAttribute("data-juneau-field-format") === "markdown")
				fillMarkdownSlot(slot, value);
			else
				slot.textContent = value;
		}
		const titles = root.querySelectorAll("[data-juneau-detail-title]");
		for (let t = 0; t < titles.length; t++) {
			const el = titles[t];
			const tmpl = el.getAttribute("data-juneau-detail-title-template") || "";
			el.textContent = tmpl.replace(/\{([A-Za-z0-9_]+)\}/g, function (_, key) {
				return Object.hasOwn(map, key) ? scalarFieldValue(map[key]) : "";
			});
		}
	}

	/**
	 * Resolves the header icon from the views icon registry (trusted sprite markup, same as ribbon buttons).
	 * Unknown names hide the slot.  Kept out of fillDetailSlots so that function stays a textContent-only sink.
	 */
	function resolveDetailHeaderIcon(root) {
		if (!root || typeof root.querySelector !== "function") return;
		const slot = root.querySelector("[data-juneau-detail-icon]");
		if (!slot) return;
		const name = slot.getAttribute("data-juneau-detail-icon");
		const icons = window.JuneauViews && window.JuneauViews.icons;
		const markup = icons && typeof icons.resolveIcon === "function" ? icons.resolveIcon(name) : null;
		if (!markup) {
			slot.hidden = true;
			slot.textContent = "";
			return;
		}
		slot.hidden = false;
		slot.innerHTML = markup;  // trusted icon-registry sprite markup only - never user data
	}

	/**
	 * Paints an action's message into the first field slot of the section that owns that action button
	 * (in-tab Diagnose findings).  TEXT slots use textContent; markdown slots use fillMarkdownSlot.
	 */
	function paintActionMessageIntoDetail(tr, actionId, message) {
		const panel = tr && tr._juneauDetailPanel;
		if (!panel || typeof panel.querySelectorAll !== "function") return;
		const want = String(actionId == null ? "" : actionId);
		if (!want) return;
		const buttons = panel.querySelectorAll("[data-juneau-action]");
		let btn = null;
		for (let i = 0; i < buttons.length; i++) {
			if (buttons[i].getAttribute("data-juneau-action") === want) { btn = buttons[i]; break; }
		}
		if (!btn) return;
		const section = btn.closest ? btn.closest("[data-juneau-detail-section]") : null;
		if (!section || typeof section.querySelector !== "function") return;
		const slot = section.querySelector("[data-juneau-field]");
		if (!slot) return;
		const value = message == null ? "" : String(message);
		if (slot.getAttribute("data-juneau-field-format") === "markdown")
			fillMarkdownSlot(slot, value);
		else
			slot.textContent = value;
	}

	// ==================================================================================================================
	// SHARED STRIP WIDGET - TAB-MODE (row-detail multi-section pane switcher)
	// ==================================================================================================================
	//
	// The row-detail template emits its sections as a stack of <section data-juneau-detail-section> blocks (all
	// visible).  For a MULTI-section detail (>= 2 sections) buildDetailStrip(...) converts that stack, client-side,
	// into ONE tab-mode strip of the shared ".juneau-view-ribbon-group" widget plus a single visible pane; a
	// single-section detail is left strip-less (title + body, no lone tab).  This is the same widget the toolbar/
	// paging use in ribbon-mode - here it carries data-juneau-strip-mode="tab" (visual) + role="tablist" (a11y),
	// keeping the visual mode decoupled from the a11y role.
	//
	// Switching tabs is VISIBILITY ONLY: it toggles the `hidden` flag on panes and never refetches (Juneau already
	// loads every field in one expand GET; fillDetailSlots still fills EVERY pane's slots, including hidden ones,
	// so switching shows already-populated content).  Labels are painted with textContent, never innerHTML.

	/** Monotonic id seed so simultaneously-expanded rows get unique tab/pane ids (DataTables allows N open rows). */
	let detailStripSeq = 0;

	/**
	 * Roving-tabindex keyboard target: given the pressed key, the current tab index and the tab count, returns the
	 * index to move selection to, or -1 for a key this widget does not handle.  Left/Right wrap; Home/End jump to
	 * the ends.  Pure - no DOM - so the arrow-key contract is unit-checkable.
	 */
	function detailTabTargetIndex(key, currentIndex, count) {
		if (count <= 0) return -1;
		if (key === "ArrowRight") return (currentIndex + 1) % count;
		if (key === "ArrowLeft") return (currentIndex - 1 + count) % count;
		if (key === "Home") return 0;
		if (key === "End") return count - 1;
		return -1;
	}

	/**
	 * Selects the tab whose section id === `sectionId` in a built strip: exactly one tab carries
	 * aria-selected="true" + tabindex 0 (the rest -1), and exactly one pane is visible (the rest `hidden`).  `tabs`
	 * is the [{btn, pane, id}] array buildDetailStrip closes over.  DOM-visibility only - never fetches.
	 */
	function activateDetailTab(tabs, sectionId) {
		if (!tabs) return;
		const want = String(sectionId);
		for (let i = 0; i < tabs.length; i++) {
			const t = tabs[i];
			const match = String(t.id) === want;
			t.btn.setAttribute("aria-selected", match ? "true" : "false");
			t.btn.tabIndex = match ? 0 : -1;
			t.pane.hidden = !match;
		}
	}

	/**
	 * Converts the stacked detail sections in a cloned detail `panel` into a tab-mode strip + one visible pane.
	 * No-op returning null for < 2 sections (single-section details stay strip-less).  Builds a
	 * ".juneau-view-ribbon-group[data-juneau-strip-mode=tab][role=tablist]" whose buttons are role="tab" +
	 * aria-selected + aria-controls (the pane id) + roving tabindex; each <section> becomes role="tabpanel" +
	 * aria-labelledby (the tab id), hidden unless it is the first (initially-selected) section.  Left/Right/Home/End
	 * move selection.  Returns the strip element (already inserted as the panel's first child), or null.
	 */
	function buildDetailStrip(panel, onActivate) {
		if (!panel || typeof panel.querySelectorAll !== "function") return null;
		const sections = panel.querySelectorAll("[data-juneau-detail-section]");
		if (!sections || sections.length < 2) return null;

		const seq = ++detailStripSeq;
		const strip = document.createElement("div");
		strip.className = "juneau-view-ribbon-group juneau-view-detail-tabs";
		strip.setAttribute("data-juneau-strip-mode", "tab");
		strip.setAttribute("role", "tablist");
		strip.setAttribute("data-testid", "detail-tabs");

		const tabs = [];
		for (let i = 0; i < sections.length; i++) {
			const sec = sections[i];
			const sid = sec.getAttribute("data-juneau-detail-section");
			const paneId = "juneau-detail-pane-" + seq + "-" + i;
			const tabId = "juneau-detail-tab-" + seq + "-" + i;
			const active = i === 0;

			// The <section> becomes the tabpanel; hide all but the initially-selected one (visibility only).
			sec.id = paneId;
			sec.setAttribute("role", "tabpanel");
			sec.setAttribute("aria-labelledby", tabId);
			sec.setAttribute("tabindex", "0");
			sec.hidden = !active;

			// The tab label is the section's own title; the stacked <h2> title is hidden (the tab replaces it).
			const titleEl = typeof sec.querySelector === "function"
				? sec.querySelector(".juneau-view-detail-section-title") : null;
			const label = titleEl && titleEl.textContent ? titleEl.textContent : sid;
			if (titleEl) titleEl.hidden = true;

			const btn = document.createElement("button");
			btn.type = "button";
			btn.className = "juneau-view-ribbon-btn";
			btn.id = tabId;
			btn.setAttribute("role", "tab");
			btn.setAttribute("data-juneau-strip-tab", sid);
			btn.setAttribute("aria-controls", paneId);
			btn.setAttribute("aria-selected", active ? "true" : "false");
			btn.tabIndex = active ? 0 : -1;
			btn.textContent = label;            // never innerHTML - labels are plain text
			strip.appendChild(btn);
			tabs.push({ btn: btn, pane: sec, id: sid });
		}

		// Fires the optional onActivate(sectionId, pane) after a tab becomes visible.  The row-detail expander uses
		// it to lazily init a newly-shown pane's nested table (a nested DataTable must never be constructed while its
		// pane is hidden - column widths would compute to zero).
		function notifyActivate(sid) {
			if (typeof onActivate !== "function") return;
			for (let i = 0; i < tabs.length; i++)
				if (String(tabs[i].id) === String(sid)) { onActivate(tabs[i].id, tabs[i].pane); return; }
		}

		strip.addEventListener("click", function (e) {
			const btn = e.target && e.target.closest ? e.target.closest("[role=\"tab\"]") : null;
			if (!btn || (strip.contains && !strip.contains(btn))) return;
			const sid = btn.getAttribute("data-juneau-strip-tab");
			activateDetailTab(tabs, sid);
			notifyActivate(sid);
			if (typeof btn.focus === "function") btn.focus();
		});
		strip.addEventListener("keydown", function (e) {
			if (!e) return;
			const focused = (typeof document !== "undefined") ? document.activeElement : null;
			let idx = -1;
			for (let i = 0; i < tabs.length; i++) {
				if (tabs[i].btn === focused) { idx = i; break; }
			}
			if (idx < 0) {
				for (let i = 0; i < tabs.length; i++) {
					if (tabs[i].btn.getAttribute("aria-selected") === "true") { idx = i; break; }
				}
				if (idx < 0) idx = 0;
			}
			const next = detailTabTargetIndex(e.key, idx, tabs.length);
			if (next < 0) return;
			if (typeof e.preventDefault === "function") e.preventDefault();
			const nextBtn = tabs[next].btn;
			const nsid = nextBtn.getAttribute("data-juneau-strip-tab");
			activateDetailTab(tabs, nsid);
			notifyActivate(nsid);
			if (typeof nextBtn.focus === "function") nextBtn.focus();
		});

		var header = typeof panel.querySelector === "function"
			? panel.querySelector(".juneau-view-detail-header") : null;
		if (header) {
			if (header.nextSibling)
				panel.insertBefore(strip, header.nextSibling);
			else
				panel.appendChild(strip);
		} else {
			panel.insertBefore(strip, panel.firstChild);
		}
		return strip;
	}

	/** Enables or disables ActionRef buttons; SafeAction.COLLAPSE is never touched. */
	function setActionRefEnabled(root, enabled) {
		if (!root || !root.querySelectorAll) return;
		const buttons = root.querySelectorAll("[data-juneau-action]");
		for (let i = 0; i < buttons.length; i++) {
			const b = buttons[i];
			b.disabled = !enabled;
			// A pill is a <span role="button">, where the .disabled property is inert - reflect the disabled state
			// via aria-disabled + an .is-disabled class instead (activatePillAction ignores either).
			if (b.getAttribute && b.getAttribute("role") === "button"
				&& (b.hasAttribute ? b.hasAttribute("data-juneau-pill") : b.getAttribute("data-juneau-pill") != null)) {
				if (enabled) {
					b.removeAttribute("aria-disabled");
					if (b.classList && b.classList.remove) b.classList.remove("is-disabled");
				} else {
					b.setAttribute("aria-disabled", "true");
					if (b.classList && b.classList.add) b.classList.add("is-disabled");
				}
			}
		}
	}

	/** Hides ActionRef buttons (404/500 / contract-fail closed) while leaving COLLAPSE in place. */
	function hideActionRefs(root) {
		if (!root || !root.querySelectorAll) return;
		const buttons = root.querySelectorAll("[data-juneau-action]");
		for (let i = 0; i < buttons.length; i++) {
			buttons[i].disabled = true;
			buttons[i].hidden = true;
		}
	}

	function findRowActionById(viewDef, id) {
		const actions = viewDef && viewDef.rowActions ? viewDef.rowActions : [];
		for (let i = 0; i < actions.length; i++)
			if (actions[i] && actions[i].id === id) return actions[i];
		return null;
	}

	/**
	 * Wires the row-details expander via DataTables' native child-row API.  ONE delegated click listener on
	 * `table` toggles expand, collapse, and ActionRef submit.  Expand GETs the stamped URL; ActionRef buttons
	 * stay disabled until 2xx + loud contract OK.  SafeAction.COLLAPSE works during loading and after failure.
	 *
	 * <p>"Collapse on redraw" needs no extra code here: DataTables' child-row API does not survive a `draw.dt`.
	 */
	function initDetailsExpander(table, ctx, viewDef) {
		if (!ctx._detailInflight) ctx._detailInflight = new Map();
		if (!ctx._detailGeneration) ctx._detailGeneration = new WeakMap();

		table.addEventListener("click", function (e) {
			const dt = ctx.dataTable;
			if (!dt) return;
			const tpl = findRowDetailTemplate(table);
			if (!tpl) return;

			const safeBtn = e.target && e.target.closest ? e.target.closest("[data-juneau-safe=\"collapse\"]") : null;
			if (safeBtn) {
				e.preventDefault();
				e.stopPropagation();
				const panel = safeBtn.closest(".juneau-view-detail-panel");
				const parentTr = panel && panel._juneauParentTr;
				if (!parentTr) return;
				const row = dt.row(parentTr);
				if (row && row.child && row.child.isShown()) {
					// Destroy any nested DataTables in this panel BEFORE the child row DOM is discarded (otherwise
					// their listeners/timers leak with the detached nodes).
					teardownNestedTables(panel);
					row.child.hide();
					parentTr.classList.remove("juneau-view-detail-open");
				}
				return;
			}

			const actionBtn = e.target && e.target.closest ? e.target.closest("[data-juneau-action]") : null;
			if (actionBtn) {
				// Only a detail-panel ActionRef is handled here.  A cell pill also carries [data-juneau-action] but
				// lives in a body <td>, not a panel - bail BEFORE preventDefault/stopPropagation so its own
				// table-level handler (initRowActions) still fires and the click is not swallowed.
				const panel = actionBtn.closest(".juneau-view-detail-panel");
				const parentTr = panel && panel._juneauParentTr;
				if (!parentTr) return;
				e.preventDefault();
				e.stopPropagation();
				if (actionBtn.disabled || actionBtn.hidden) return;
				const action = findRowActionById(viewDef, actionBtn.getAttribute("data-juneau-action"));
				if (!action) return;
				submitRowAction(action, table, parentTr, ctx);
				return;
			}

			const tr = e.target && e.target.closest ? e.target.closest("tr.juneau-view-detail-row") : null;
			if (!tr) return;
			const row = dt.row(tr);
			if (!row || !row.length) return;
			if (row.child.isShown()) {
				// Tear down nested DataTables before hiding (their child-row DOM is about to be detached).
				if (tr._juneauDetailPanel) teardownNestedTables(tr._juneauDetailPanel);
				row.child.hide();
				tr.classList.remove("juneau-view-detail-open");
				return;
			}
			expandDetailRow(table, ctx, viewDef, tpl, dt, tr, row);
		});
	}

	// ==================================================================================================================
	// popupLayerStack (TODO-445h): ONE shared registry for stacked light-dismiss / modal layers - dialogs, cell
	// popovers, and row-action menus.  Top-layer-only Escape (preventDefault) and outside-click; per-layer focus trap
	// for modals; focus restore on pop.  Cell-anchored layers portal to document.body as position:fixed; the page-size
	// menu (stays position:absolute in the paging pill) and the timestamp popup (initTsPopup / hideTsPopupIfPresent)
	// are deliberately NOT registered here.  The inline z-index (base + rawStackIndex*step) is the ONLY z source of
	// truth for a registered layer.
	// ==================================================================================================================

	/**
	 * The live layer records, bottom -> top.  Each record:
	 * {@code { el, onDismiss, trapFocus, lightDismiss, detachOnPop, returnFocusTo, kind }}.
	 */
	const popupLayerStack = [];

	/**
	 * The v1 modal-over-modal depth cap: an outer dialog plus ONE nested {@code type=action} dialog.  This counts
	 * {@code kind === "dialog"} records ONLY - a dialog + a popover is two stack entries but ONE dialog, and a nested
	 * dialog must still open over them.  Popovers, row-action menus, and the (off-stack) timestamp popup never consume
	 * the cap.  A third dialog push is a visible refusal inside the current top dialog.  Unlimited dialog depth is 10.0.
	 */
	const MAX_DIALOG_DEPTH = 2;

	/**
	 * A monotonic dialog sequence, incremented ONLY on a dialog-kind push, that namespaces the form field element ids
	 * ({@code juneau-dialog-field-<seq>-<name>}).  Stacked dialogs sharing a field name therefore never collide, and
	 * popover / row-action-menu pushes (which do not touch this counter) never shift a dialog's field ids (N-3).
	 */
	let dialogSeq = 0;

	/** Reads a numeric {@code --jc-*} token off {@code :root}; falls back when unavailable (Node shim / missing token). */
	function cssLayerNumber(name, fallback) {
		try {
			if (typeof window !== "undefined" && typeof window.getComputedStyle === "function"
				&& typeof document !== "undefined" && document.documentElement) {
				const v = window.getComputedStyle(document.documentElement).getPropertyValue(name);
				const n = v ? parseInt(String(v).trim(), 10) : NaN;
				if (! isNaN(n)) return n;
			}
		} catch (e) { /* fall through to the fallback */ }
		return fallback;
	}

	function layerZBase() { return cssLayerNumber("--jc-dialog-z", 1000); }
	function layerZStep() { return cssLayerNumber("--jc-layer-step", 10); }

	function topLayer() {
		return popupLayerStack.length ? popupLayerStack[popupLayerStack.length - 1] : null;
	}

	/** The number of {@code kind === "dialog"} layers currently open (the depth cap counts these, not stack.length). */
	function dialogLayerCount() {
		let n = 0;
		for (let i = 0; i < popupLayerStack.length; i++) if (popupLayerStack[i].kind === "dialog") n++;
		return n;
	}

	function focusablesIn(el) {
		if (! el || typeof el.querySelectorAll !== "function") return [];
		const sel = "a[href],area[href],button:not([disabled]),input:not([disabled]),select:not([disabled])," +
			"textarea:not([disabled]),[tabindex]";
		const out = [];
		Array.prototype.forEach.call(el.querySelectorAll(sel), function (n) {
			if (n && n.getAttribute && n.getAttribute("tabindex") === "-1") return;
			out.push(n);
		});
		return out;
	}

	/** Focuses the first focusable control in a layer; a form with no focusable control focuses the layer element. */
	function focusFirstInLayer(rec) {
		const f = focusablesIn(rec.el);
		const target = f.length ? f[0] : rec.el;
		if (target && typeof target.focus === "function") { try { target.focus(); } catch (e) { /* ignore */ } }
	}

	/**
	 * Registers a layer.  When {@code opts.portal !== false} the element is reparented to {@code document.body} and set
	 * {@code position:fixed} (cell-anchored surfaces; a dialog backdrop is already full-viewport).  Records the current
	 * {@code activeElement} (or {@code opts.returnFocusTo}) for focus-restore on pop, stamps the inline z-index and
	 * {@code data-juneau-layer} = raw stack index, and installs the focus trap when {@code opts.trapFocus}.
	 */
	function pushLayer(el, opts) {
		opts = opts || {};
		const rec = {
			el: el,
			onDismiss: typeof opts.onDismiss === "function" ? opts.onDismiss : null,
			trapFocus: !! opts.trapFocus,
			lightDismiss: !! opts.lightDismiss,
			detachOnPop: opts.detachOnPop !== false,
			kind: opts.kind || "layer",
			returnFocusTo: opts.returnFocusTo ||
				((typeof document !== "undefined") ? document.activeElement : null)
		};
		if (opts.portal !== false && typeof document !== "undefined" && document.body) {
			if (el.parentNode !== document.body) document.body.appendChild(el);
			if (el.style) el.style.position = "fixed";
		}
		popupLayerStack.push(rec);
		const idx = popupLayerStack.length - 1;
		if (el.style) el.style.zIndex = String(layerZBase() + idx * layerZStep());
		if (el.setAttribute) el.setAttribute("data-juneau-layer", String(idx));
		bindLayerStackDocumentListeners();
		if (rec.trapFocus) focusFirstInLayer(rec);
		return rec;
	}

	/**
	 * Pops the top layer, or - when {@code el} is given - that layer AND everything stacked above it (never a sibling
	 * BELOW it, so a dialog's backdrop pop does not remove another dialog's backdrop).  A dialog layer's element is the
	 * backdrop, so removing it takes exactly that dialog + its own backdrop.  Restores focus to the lowest removed
	 * layer's {@code returnFocusTo} if it is still in the document.
	 */
	function popLayer(el) {
		if (! popupLayerStack.length) return;
		let from = popupLayerStack.length - 1;
		if (el) {
			let idx = -1;
			for (let i = popupLayerStack.length - 1; i >= 0; i--) if (popupLayerStack[i].el === el) { idx = i; break; }
			if (idx < 0) return;   // not a registered layer
			from = idx;
		}
		const removed = popupLayerStack.splice(from);   // [from .. top]
		const restore = removed.length ? removed[0].returnFocusTo : null;
		for (let i = removed.length - 1; i >= 0; i--) {
			const rec = removed[i];
			if (rec.detachOnPop && rec.el && rec.el.parentNode) rec.el.parentNode.removeChild(rec.el);
			if (rec.onDismiss) { try { rec.onDismiss(); } catch (e) { /* ignore */ } }
		}
		if (restore && typeof restore.focus === "function"
			&& (typeof document === "undefined" || typeof document.contains !== "function" || document.contains(restore)))
			{ try { restore.focus(); } catch (e) { /* ignore */ } }
	}

	function handleLayerTab(e) {
		const top = topLayer();
		if (! top || ! top.trapFocus) return;
		const f = focusablesIn(top.el);
		if (! f.length) { e.preventDefault(); return; }
		const first = f[0], last = f[f.length - 1];
		const active = (typeof document !== "undefined") ? document.activeElement : null;
		const inLayer = top.el.contains && top.el.contains(active);
		if (e.shiftKey) {
			if (active === first || ! inLayer) { e.preventDefault(); if (typeof last.focus === "function") last.focus(); }
		} else {
			if (active === last || ! inLayer) { e.preventDefault(); if (typeof first.focus === "function") first.focus(); }
		}
	}

	let _layerListenersBound = false;
	function bindLayerStackDocumentListeners() {
		if (_layerListenersBound) return;
		if (typeof document === "undefined" || typeof document.addEventListener !== "function") return;
		_layerListenersBound = true;
		// ONE document keydown: Escape pops the TOP layer only (preventDefault); Tab cycles within a trapping top layer.
		document.addEventListener("keydown", function (e) {
			if (! popupLayerStack.length) return;
			if (e.key === "Escape") { e.preventDefault(); popLayer(); return; }
			if (e.key === "Tab") handleLayerTab(e);
		});
		// ONE document pointerdown: an outside click dismisses the TOP layer only, and only when it is light-dismiss.
		document.addEventListener("pointerdown", function (e) {
			const top = topLayer();
			if (! top || ! top.lightDismiss) return;
			const t = e.target;
			if (t && top.el && top.el.contains && top.el.contains(t)) return;
			popLayer();
		});
	}

	const CELL_POPOVER_ID = "juneau-cell-popover";

	function cellPopoverEl() {
		if (typeof document === "undefined" || typeof document.getElementById !== "function") return null;
		let el = document.getElementById(CELL_POPOVER_ID);
		if (!el && document.body) {
			el = document.createElement("div");
			el.id = CELL_POPOVER_ID;
			el.className = "jc-cell-popover";
			el.setAttribute("role", "dialog");
			el.setAttribute("tabindex", "-1");
			el.style.display = "none";
			el.style.position = "fixed";
			document.body.appendChild(el);
		}
		return el || null;
	}

	function hideTsPopupIfPresent() {
		const ts = (typeof document !== "undefined" && typeof document.getElementById === "function")
			? document.getElementById("juneau-ts-popup") : null;
		if (ts) ts.style.display = "none";
	}

	function currentPopoverTrigger() {
		if (typeof document === "undefined" || !document.querySelector) return null;
		return document.querySelector(".jc-cell-popover-trigger[aria-expanded=\"true\"]");
	}

	/** Hides the (reused) cell-popover element and resets its trigger's expanded state.  Does NOT detach the element. */
	function hidePopoverEl(el, trigger) {
		if (el) {
			el.style.display = "none";
			if (typeof el.replaceChildren === "function") el.replaceChildren();
			else while (el.firstChild) el.removeChild(el.firstChild);
		}
		if (trigger) {
			trigger.setAttribute("aria-expanded", "false");
			trigger.removeAttribute("aria-controls");
			if (typeof document.contains === "function" && document.contains(trigger)
				&& typeof trigger.focus === "function")
				trigger.focus();
		}
	}

	function closeCellPopover() {
		// The cell popover reuses ONE element; when registered as a layer, popLayer's onDismiss hides+resets it (the
		// element is kept, detachOnPop:false).  Fall back to a direct hide when it is not on the stack.
		for (let i = popupLayerStack.length - 1; i >= 0; i--) {
			if (popupLayerStack[i].kind === "popover") { popLayer(popupLayerStack[i].el); return; }
		}
		const el = (typeof document !== "undefined" && typeof document.getElementById === "function")
			? document.getElementById(CELL_POPOVER_ID) : null;
		hidePopoverEl(el, currentPopoverTrigger());
	}

	function positionCellPopover(el, trigger) {
		if (!el || !trigger || typeof trigger.getBoundingClientRect !== "function") return;
		const rect = trigger.getBoundingClientRect();
		const vw = (typeof window !== "undefined" && window.innerWidth) ? window.innerWidth : 1024;
		const vh = (typeof window !== "undefined" && window.innerHeight) ? window.innerHeight : 768;
		el.style.display = "block";
		const w = el.offsetWidth || 0;
		const h = el.offsetHeight || 0;
		let left = rect.left;
		let top = rect.bottom + 4;
		if (left + w > vw - 4) left = Math.max(4, vw - w - 4);
		if (top + h > vh - 4) top = Math.max(4, rect.top - h - 4);
		if (left < 4) left = 4;
		el.style.left = left + "px";
		el.style.top = top + "px";
	}

	function popoverFieldValue(rowData, key) {
		if (!rowData || key == null || !Object.hasOwn(rowData, key)) return "";
		const v = rowData[key];
		if (v == null) return "";
		return v;
	}

	function paintPopoverTextValue(cell, value) {
		cell.textContent = value == null || value === "" ? "" : String(value);
	}

	function paintPopoverRenderedValue(cell, field, value, rowData) {
		const spec = typeof NS.parseRenderId === "function" ? NS.parseRenderId(field.render) : field.render;
		const id = spec && spec.id;
		const renderer = typeof NS.resolveSinkRenderer === "function" ? NS.resolveSinkRenderer(id) : null;
		if (!renderer || typeof renderer.display !== "function") {
			paintPopoverTextValue(cell, value);
			return;
		}
		const meta = {};
		if (spec.meta) {
			for (const k in spec.meta) if (Object.hasOwn(spec.meta, k)) meta[k] = spec.meta[k];
		}
		meta.popup = "off";
		let html;
		try { html = renderer.display(value, rowData, meta); }
		catch (e) {
			paintPopoverTextValue(cell, value);
			return;
		}
		const Parser = typeof DOMParser !== "undefined" ? DOMParser
			: (typeof window !== "undefined" ? window.DOMParser : null);
		const doc = typeof document !== "undefined" ? document : null;
		if (!Parser || !doc) {
			paintPopoverTextValue(cell, value);
			return;
		}
		let parsed;
		try { parsed = new Parser().parseFromString(String(html == null ? "" : html), "text/html"); }
		catch (e) {
			paintPopoverTextValue(cell, value);
			return;
		}
		const body = parsed && parsed.body;
		if (!body) {
			paintPopoverTextValue(cell, value);
			return;
		}
		const kids = body.childNodes;
		for (let i = 0; i < kids.length; i++) {
			if (kids[i] && kids[i].nodeType === 1) {
				paintPopoverTextValue(cell, value);
				return;
			}
		}
		cell.textContent = body.textContent == null ? "" : String(body.textContent);
	}

	/**
	 * Fills the cell-popover dialog from row data using createElement/textContent only.  Named so the
	 * raw-HTML scanner can extract it.
	 */
	function fillCellPopover(el, popover, rowData) {
		if (!el) return;
		if (typeof el.replaceChildren === "function") el.replaceChildren();
		else while (el.firstChild) el.removeChild(el.firstChild);
		const doc = typeof document !== "undefined" ? document : null;
		if (!doc || typeof doc.createElement !== "function") return;
		if (popover.title != null && String(popover.title).trim() !== "") {
			const t = doc.createElement("div");
			t.className = "jc-cell-popover-title";
			t.id = CELL_POPOVER_ID + "-title";
			t.textContent = String(popover.title);
			el.appendChild(t);
			el.setAttribute("aria-labelledby", t.id);
		} else {
			el.setAttribute("aria-label", "Details");
			el.removeAttribute("aria-labelledby");
		}
		const fields = popover.fields || [];
		for (let i = 0; i < fields.length; i++) {
			const f = fields[i];
			if (!f || f.data == null) continue;
			const row = doc.createElement("div");
			row.className = "jc-cell-popover-row";
			const lab = doc.createElement("div");
			lab.className = "jc-cell-popover-label";
			lab.textContent = f.title != null && String(f.title) !== "" ? String(f.title) : String(f.data);
			const val = doc.createElement("div");
			val.className = "jc-cell-popover-value";
			const raw = popoverFieldValue(rowData, f.data);
			if (f.render)
				paintPopoverRenderedValue(val, f, raw, rowData);
			else
				paintPopoverTextValue(val, raw);
			row.appendChild(lab);
			row.appendChild(val);
			el.appendChild(row);
		}
	}

	function findPopoverDecl(ctx, viewDef, colData) {
		const cols = (ctx && ctx.effectiveColumns) || (viewDef && viewDef.columns) || [];
		for (let i = 0; i < cols.length; i++) {
			const c = cols[i];
			if (!c || c.data !== colData) continue;
			const spec = typeof NS.parseRenderId === "function" ? NS.parseRenderId(c.render) : c.render;
			return spec && spec.popover ? spec.popover : null;
		}
		return null;
	}

	function openCellPopover(btn, ctx, viewDef) {
		const dt = ctx && ctx.dataTable;
		if (!dt || !btn) return;
		const tr = btn.closest ? btn.closest("tr") : null;
		if (!tr || typeof dt.row !== "function") return;
		const row = dt.row(tr);
		const rowData = row && typeof row.data === "function" ? row.data() : null;
		const colData = btn.getAttribute("data-juneau-popover-col");
		const popover = findPopoverDecl(ctx, viewDef, colData);
		if (!popover) return;
		hideTsPopupIfPresent();
		// A previously-open popover is a layer; pop it (onDismiss resets its trigger) before opening the new one.
		closeCellPopover();
		const el = cellPopoverEl();
		if (!el) return;
		fillCellPopover(el, popover, rowData || {});
		btn.setAttribute("aria-expanded", "true");
		btn.setAttribute("aria-controls", CELL_POPOVER_ID);
		positionCellPopover(el, btn);
		if (typeof el.focus === "function") el.focus();
		// Register on the shared stack as a light-dismiss popover.  portal:false (the element already lives in body)
		// and detachOnPop:false (the element is reused): popLayer just hides+resets it via onDismiss.
		pushLayer(el, {
			kind: "popover", portal: false, detachOnPop: false, lightDismiss: true, trapFocus: false,
			returnFocusTo: btn,
			onDismiss: function () { hidePopoverEl(el, btn); }
		});
	}

	/**
	 * ONE delegated click listener on the stable table element.  Survives DataTables redraw.  Escape / outside-click
	 * dismissal is owned by the shared {@code popupLayerStack} (the popover registers as a light-dismiss layer on open),
	 * so there are no per-popover document listeners here.
	 */
	function initCellPopover(table, ctx, viewDef) {
		if (!table || typeof table.addEventListener !== "function") return;
		if (table._juneauCellPopoverBound) return;
		table._juneauCellPopoverBound = true;
		table.addEventListener("click", function (e) {
			const btn = e.target && e.target.closest ? e.target.closest("[data-juneau-popover]") : null;
			if (!btn) return;
			e.preventDefault();
			e.stopPropagation();
			if (btn.getAttribute("aria-expanded") === "true") {
				closeCellPopover();
				return;
			}
			openCellPopover(btn, ctx, viewDef);
		});
	}

	function expandDetailRow(table, ctx, viewDef, tpl, dt, tr, row) {
		const gen = (ctx._detailGeneration.get(tr) || 0) + 1;
		ctx._detailGeneration.set(tr, gen);
		const rowId = tr.getAttribute(ROW_ID_ATTR);
		const key = detailCoalesceKey(rowId, gen);

		const panel = document.createElement("div");
		panel.className = "juneau-view-detail-panel";
		panel.setAttribute("data-testid", "detail-panel");
		panel.setAttribute("data-juneau-detail-state", "loading");
		panel._juneauParentTr = tr;
		tr._juneauDetailPanel = panel;
		panel.appendChild(tpl.content.cloneNode(true));
		resolveDetailHeaderIcon(panel);
		// Multi-section details become a tab-mode strip + one visible pane (single-section stays strip-less).
		// Built now, before the field slots fill, so the strip is present during loading; fillDetailSlots still
		// fills EVERY pane's slots (including the hidden ones) so switching tabs shows populated content.
		//
		// The onActivate callback lazily inits a newly-shown pane's nested table - but ONLY after the parent detail
		// GET succeeded (state "ok").  A tab clicked while the panel is still loading (or has failed) inits nothing,
		// honoring "init nested after parent 2xx AND pane visible" (a failed parent expand yields no nested table).
		buildDetailStrip(panel, function (sid, pane) {
			if (panel.getAttribute("data-juneau-detail-state") !== "ok") return;
			activateNestedTablesInPane(pane, rowId);
		});
		const loading = document.createElement("p");
		loading.className = "juneau-view-detail-status";
		loading.textContent = "Loading…";
		panel.appendChild(loading);
		row.child(panel).show();
		tr.classList.add("juneau-view-detail-open");

		function stillCurrent() {
			return ctx._detailGeneration.get(tr) === gen && row.child.isShown();
		}

		function settleMap() {
			if (ctx._detailInflight) ctx._detailInflight.delete(key);
		}

		function failClosed(kind, message) {
			if (!stillCurrent()) { settleMap(); return; }
			hideActionRefs(panel);
			panel.setAttribute("data-juneau-detail-state", kind);
			loading.textContent = message;
			settleMap();
		}

		if (rowId == null || String(rowId) === "") {
			failClosed("error", "this row has no stable id");
			return;
		}

		const url = substituteDetailUrl(tpl.getAttribute("data-juneau-detail-url"), rowId);
		if (!url) {
			failClosed("error", "detail URL is not a same-origin path template");
			return;
		}

		function handleEnvelope(env) {
			if (!stillCurrent()) { settleMap(); return; }
			if (!env) {
				failClosed("error", "the detail request could not be completed");
				return;
			}
			if (env.status === 404) {
				failClosed("empty", "this row is gone");
				return;
			}
			if (!env.ok) {
				failClosed("error", "the detail request was refused (" + env.status + ")");
				return;
			}
			const body = parseJsonSafe(env.text);
			const expected = tpl.getAttribute("data-juneau-detail-contract") || JUNEAU_ROW_DETAIL_CONTRACT_VERSION;
			if (!detailContractOk(body, expected)) {
				const m = "Juneau view '" + (viewDef && viewDef.id ? viewDef.id : "") +
					"': row-detail contract version mismatch (page='" +
					(body && body.contractVersion) + "', runtime='" + expected + "').";
				error(m);
				renderBanner(table, m);
				failClosed("error", m);
				return;
			}
			if (shouldDropDetailPayload(row.child.isShown(), gen, ctx._detailGeneration.get(tr))) {
				settleMap();
				return;
			}
			fillDetailSlots(panel, body.fields);
			setActionRefEnabled(panel, true);
			panel.setAttribute("data-juneau-detail-state", "ok");
			if (loading.parentNode) loading.parentNode.removeChild(loading);
			// Now that the parent detail loaded (2xx + contract OK), init the nested tables that live in a currently
			// VISIBLE pane (in tab mode, only the initially-selected section is visible; a hidden pane's nested table
			// waits for its tab to be activated via the onActivate callback above).  Each nested table runs its OWN
			// independent GET, scoped to this parent row.
			initNestedTablesInVisiblePanes(panel, rowId);
			settleMap();
		}

		const inflight = ctx._detailInflight;
		let req = inflight.get(key);
		if (!req) {
			req = fetch(url, { method: "GET", credentials: "same-origin", headers: { "Accept": "application/json" } })
				.then(function (resp) {
					return readBodyText(resp).then(function (text) {
						return { status: resp.status, ok: resp.ok, text: text };
					});
				});
			inflight.set(key, req);
		}
		req.then(handleEnvelope).catch(function () {
			failClosed("error", "the detail request could not be completed");
		});
	}

	/**
	 * Wires a table's poll timer + its staleness indicator.  A plain interval fetch - deliberately not a
	 * streaming/SSE transport, which would be an independent mechanism.
	 *
	 * <p>Any successful DataTables draw resets the "last refreshed" clock - whether it was triggered by this
	 * timer, the refresh ribbon button, paging, or a search - because each one really did just complete a fresh
	 * server round trip. Only the timer's OWN tick additionally (a) skips entirely while the tab/page is hidden
	 * (Page Visibility API - no fetch, no cost, while backgrounded) and (b) skips entirely while
	 * {@link #hasInFlightRow} is true. A failed round trip (`error.dt`) flips the indicator to a distinct
	 * "error" state without touching the last-success timestamp, so a frozen clock and a broken poll never look
	 * identical to a healthy one (the whole point of this function).
	 */
	function initPolling(table, dt, viewDef, indicator, ctx) {
		const intervalMs = clampPollInterval(viewDef.pollIntervalMs);
		const state = { lastSuccessAt: Date.now(), failed: false };

		function render() {
			const age = formatStalenessAge(Date.now() - state.lastSuccessAt);
			indicator.setAttribute("data-state", state.failed ? "error" : "fresh");
			indicator.textContent = state.failed ? "Refresh failed - last updated " + age : "Updated " + age;
		}

		// Guard against a nested table's draw.dt/error.dt bubbling up to this parent-table poll indicator: a nested
		// table's own round trips must not reset (or fail) the parent's staleness clock.
		dt.on("draw.dt", function (e) { if (e && e.target !== table) return; state.lastSuccessAt = Date.now(); state.failed = false; render(); });
		dt.on("error.dt", function (e) { if (e && e.target !== table) return; state.failed = true; render(); });

		function poll() {
			if (document.hidden) return;
			if (hasInFlightRow(table)) return;
			dt.ajax.reload(null, false);
		}

		if (ctx && ctx._pollTimers) {
			ctx._pollTimers.forEach(function (id) { clearInterval(id); });
		}
		const pollId = setInterval(poll, intervalMs);
		// The visible age ("5s ago" -> "6s ago" ...) must keep advancing between polls, independent of the data-
		// fetch cadence - a short, fixed, network-free tick keeps the label honest without any extra ajax cost.
		const renderId = setInterval(render, 1000);
		if (ctx) ctx._pollTimers = [pollId, renderId];
		render();
	}

	/** The per-row action-menu trigger markup (returned by the synthetic actions column's render). */
	function actionTriggerMarkup() {
		const icons = window.JuneauViews && window.JuneauViews.icons;
		const glyph = icons?.resolveIcon ? icons.resolveIcon("more_vert") : null;
		const inner = glyph != null ? glyph : "\u22EF";   // horizontal ellipsis fallback when no glyph is registered
		return '<button type="button" class="juneau-view-action-trigger" aria-haspopup="menu" ' +
			'aria-label="Row actions">' + inner + '</button>';
	}

	/**
	 * Reads the auto-embedded CSRF token off the view table's `data-juneau-csrf` attribute (stamped by
	 * ViewTable from the LoopbackBoundaryFilter request attribute, or set by the host as an override/fallback).
	 * Returns the raw attribute value (possibly null/blank); isBlankToken(...) is what decides fail-closed, so a
	 * whitespace value is NOT normalized away here - it must reach the same blank test the server boundary uses.
	 */
	function resolveCsrfToken(table) {
		return table.getAttribute("data-juneau-csrf");
	}

	/** The per-table CSRF header-name override (`data-juneau-csrf-header`), else the framework default. */
	function resolveCsrfHeaderName(table) {
		const override = table.getAttribute("data-juneau-csrf-header");
		return isBlankToken(override) ? DEFAULT_CSRF_HEADER : override.trim();
	}

	/** Appends the actions column's header cell to <thead> (kept in step with buildOptions' synthetic column). */
	function appendActionsHeaderCell(table) {
		const headRow = table.querySelector("thead tr");
		if (!headRow) return;
		const th = document.createElement("th");
		th.className = "juneau-view-actions-th";
		th.setAttribute("aria-label", "Actions");
		headRow.appendChild(th);
	}

	// ==================================================================================================================
	// ROW SELECTION + BULK MUTATION (TODO-428) - two INDEPENDENT opt-ins (HIGH-5), detected purely from DOM
	// attributes ViewTable stamps (SELECT_ATTR / BULK_ATTR) - NEVER from VIEW_META/viewDef.  Enabling selection
	// alone can never surface a bulk-mutate control: hasBulk(...) is only ever consulted from within the
	// `hasSelection(table)` branch of initTable, and BULK_ATTR is only ever stamped by ViewTable when a
	// BulkMutateDef (which itself REQUIRES a WritePermit + a SelectionDef to construct) was supplied.
	// ==================================================================================================================

	/** Whether `table` declares row selection (SelectionDef was supplied to ViewTable.of(...)). */
	function hasSelection(table) {
		return table.getAttribute(SELECT_ATTR) === "1";
	}

	/** Whether `table` declares bulk mutation (BulkMutateDef was supplied to ViewTable.of(...)). */
	function hasBulk(table) {
		return table.getAttribute(BULK_ATTR) === "1";
	}

	/**
	 * Stamps the STABLE row id (MED-11) onto a just-created `<tr>`, read from that row's OWN data via the
	 * `rowIdField` key - never a DOM/table index. A no-op when `rowIdField` is absent (selection not declared) or
	 * the row has no such key (nothing to stamp; that row is simply unselectable). This is the ONLY place
	 * `ROW_ID_ATTR` is written; every reader elsewhere (selection wiring, bulk execution, the TODO-416 modal path's
	 * `submitActionDialog`) treats it as already-authoritative once stamped.
	 */
	function stampRowId(rowEl, rowData, rowIdField) {
		var id;
		if (rowIdField) id = rowIdOf(rowData, rowIdField);
		if (id == null && rowData) {
			if (rowData.id != null) id = rowData.id;
			else if (rowData.name != null) id = rowData.name;
		}
		if (id != null) rowEl.setAttribute(ROW_ID_ATTR, String(id));
	}

	/** The selection checkbox cell's markup - a bare, unlabeled-by-design checkbox (the row IS its own label). */
	function selectionCellMarkup(checked) {
		return '<input type="checkbox" class="juneau-view-select-checkbox" aria-label="Select row"' +
			(checked ? " checked" : "") + '>';
	}

	/**
	 * Builds the synthetic, non-orderable/non-searchable LEADING selection column (mirrors how a declared
	 * rowActions list appends a synthetic TRAILING column in buildOptions) - `selectionState.selected` is a
	 * live `Set` of currently-selected stable row ids, so a redraw always paints each row's checkbox from the
	 * CURRENT selection, never a stale snapshot.
	 */
	function buildSelectionColumnDef(selectionState) {
		return {
			data: null,
			orderable: false,
			searchable: false,
			className: "juneau-view-select-cell",
			defaultContent: "",
			title: "",
			render: function (data, type, rowData) {
				if (type && type !== "display") return "";
				const id = rowIdOf(rowData, selectionState.rowIdField);
				return selectionCellMarkup(id != null && selectionState.selected.has(String(id)));
			}
		};
	}

	/**
	 * Wires row selection: per-row checkbox toggle (delegated `change` listener - checkboxes are re-created on
	 * every draw, the table element is not), the optional select-all header checkbox (only when `SelectionDef`
	 * declared `selectAll`), and the off-screen-id-drop persistence rule (Q2/MED-11) on every `draw.dt` (sort,
	 * page, or a TODO-426 poll tick).  Select-all is scoped to the CURRENT draw's rows only (the ones actually on
	 * screen) - consistent with the drop rule, it can never reach into an off-screen page.
	 */
	function initSelection(table, ctx) {
		function currentRowIds() {
			const ids = [];
			Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
				ids.push(tr.getAttribute(ROW_ID_ATTR));
			});
			return ids;
		}

		function refresh() {
			if (ctx && ctx.bulkToolbar) ctx.bulkToolbar.refresh(ctx.selectionState.selected.size);
		}

		table.addEventListener("change", function (e) {
			const selectionState = ctx.selectionState;
			if (!selectionState) return;
			const allCb = e.target && e.target.closest ? e.target.closest(".juneau-view-select-all-checkbox") : null;
			if (allCb) {
				Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
					const id = tr.getAttribute(ROW_ID_ATTR);
					const cb = tr.querySelector(".juneau-view-select-checkbox");
					if (cb) cb.checked = allCb.checked;
					if (allCb.checked) selectionState.selected.add(id); else selectionState.selected.delete(id);
				});
				refresh();
				return;
			}
			const cb = e.target && e.target.closest ? e.target.closest(".juneau-view-select-checkbox") : null;
			if (!cb) return;
			const tr = cb.closest("tr");
			const id = tr ? tr.getAttribute(ROW_ID_ATTR) : null;
			if (id == null) return;
			if (cb.checked) selectionState.selected.add(id); else selectionState.selected.delete(id);
			refresh();
		});

		// Select-all checkbox element is (re)created per build in ensureSelectAllCheckbox - the listener above is
		// delegated off `table` so it is bound exactly once (listener-lifetime class (a)).
	}

	/**
	 * Per-instance off-screen-id-drop prune (listener-lifetime class (b)).  Re-registered against the freshly
	 * constructed DataTables instance on every buildTable; dies with {@code dt.destroy()}.
	 */
	function bindSelectionPrune(table, ctx) {
		const dt = ctx.dataTable;
		if (!dt || !ctx.selectionState) return;
		dt.on("draw.dt", function (e) {
			// Guard against a nested table's draw.dt bubbling up: the parent's selection prune must ignore it (and
			// must never scan the nested table's <tbody> rows, which the descendant query below would otherwise pick up).
			if (e && e.target !== table) return;
			const selectionState = ctx.selectionState;
			const ids = [];
			Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
				ids.push(tr.getAttribute(ROW_ID_ATTR));
			});
			const pruned = pruneSelection(Array.from(selectionState.selected), ids);
			selectionState.selected = new Set(pruned);
			if (ctx.bulkToolbar) ctx.bulkToolbar.refresh(selectionState.selected.size);
		});
	}

	/** Ensures the select-all checkbox exists in the (possibly restored) selection header cell.  No listener. */
	function ensureSelectAllCheckbox(table) {
		if (table.getAttribute(SELECT_ALL_ATTR) !== "1") return;
		const th = table.querySelector(".juneau-view-select-th");
		if (!th) return;
		if (th.querySelector(".juneau-view-select-all-checkbox")) return;
		const allCb = document.createElement("input");
		allCb.type = "checkbox";
		allCb.className = "juneau-view-select-all-checkbox";
		allCb.setAttribute("aria-label", "Select all rows on this page");
		th.appendChild(allCb);
	}

	/**
	 * Reads and JSON.parses the bulk-actions sidecar (`BULK_SIDECAR_ID_PREFIX + id`) - the independently-versioned
	 * {@code BulkMutateDef} contract, deliberately never merged into VIEW_META (R2 guard).  Returns `null` on a
	 * missing or malformed sidecar (the caller treats that as "no usable bulk config" and withholds the toolbar,
	 * rather than guessing).
	 */
	function readBulkDef(id) {
		const sidecar = document.getElementById(BULK_SIDECAR_ID_PREFIX + id);
		if (!sidecar) return null;
		return parseJsonSafe(sidecar.textContent);
	}

	/**
	 * Builds the bulk-actions toolbar: a live "N selected" count plus one button per declared bulk action, each
	 * disabled while the selection is empty (there is nothing for a bulk action to target).  A click drives
	 * executeBulkAction(...) - the per-target submit path, never an aggregate one.
	 */
	function buildBulkToolbar(bulkDef, table, ctx, selectionState) {
		const bar = document.createElement("div");
		bar.className = "juneau-view-bulk-toolbar";
		bar.setAttribute("data-testid", "bulk-toolbar");

		const countEl = document.createElement("span");
		countEl.className = "juneau-view-bulk-count";
		bar.appendChild(countEl);

		const buttons = (bulkDef.actions || []).map(function (action) {
			const btn = document.createElement("button");
			btn.type = "button";
			btn.className = "juneau-view-bulk-action-btn";
			btn.textContent = action.label || action.id;
			btn.disabled = true;
			btn.addEventListener("click", function () { executeBulkAction(action, table, ctx, selectionState); });
			bar.appendChild(btn);
			return btn;
		});

		return {
			el: bar,
			refresh: function (count) {
				countEl.textContent = count > 0 ? (count + " selected") : "";
				buttons.forEach(function (b) { b.disabled = count === 0; });
			}
		};
	}

	/**
	 * Executes ONE bulk action over the current selection as N INDEPENDENT per-row writes (HIGH-5) - it is a
	 * plain loop calling the SAME submitRowAction(...) the single-row action-menu uses, once per selected id, each
	 * carrying that row's stable id as `targetId` in the JSON body (the same `extra` convention the TODO-416 modal
	 * submit path already uses for `idempotencyKey`/`targetId`).  There is deliberately NO aggregate request and
	 * NO aggregate result: each row gets its own TODO-417 in-flight marker and its own typed ActionResult,
	 * rendered independently, so one target's failure/refusal/unknown can never be hidden behind an overall
	 * "success" - and each clears ITS OWN `data-juneau-inflight` on ITS OWN terminal outcome (MED-4), so a stuck
	 * target can never halt the whole table's polling.  A selected id whose row is no longer on screen (e.g. it
	 * left the page between the click and this loop running) is silently skipped - the persistence rule (MED-11)
	 * never lets an off-screen row become an actionable target.
	 */
	function executeBulkAction(action, table, ctx, selectionState) {
		const ids = Array.from(selectionState.selected);
		if (!ids.length) return;
		const byId = {};
		Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
			byId[tr.getAttribute(ROW_ID_ATTR)] = tr;
		});
		ids.forEach(function (id) {
			const tr = byId[id];
			if (!tr) return;
			submitRowAction(action, table, tr, ctx, { targetId: id });
		});
	}

	/** Renders a VISIBLE, non-blocking inline error line (anti-silent-degradation) into `container`. */
	function renderInlineError(container, message) {
		const el = document.createElement("div");
		el.className = "juneau-view-error";
		el.setAttribute("role", "alert");
		el.textContent = message;
		container.appendChild(el);
	}

	/**
	 * Issues one row action, FAIL-CLOSED.  Resolves the table's token + header name, asks the pure
	 * buildActionRequest(...) for a request descriptor, and:
	 *   - on a refusal marker (safe method, or blank/absent/whitespace token) renders a VISIBLE refusal and sends
	 *     NOTHING - no silent degradation, and never an empty-header request the server would 403;
	 *   - otherwise marks the row in-flight (TODO-417) and issues the JSON fetch with the CSRF header, then settles
	 *     the row from the typed ActionResult / transport refusal (TODO-416).
	 *
	 * This is the DIRECT submit path (no confirmation dialog); a `present=dialog` action goes through
	 * openActionDialog(...) instead.  The optional `extra` payload (idempotencyKey + targetId) is carried on the
	 * dialog path.  The client refusal is defense-against-omission; the landed server-side LoopbackBoundary is the
	 * real control.
	 */
	function submitRowAction(action, table, tr, ctx, extra) {
		const req = buildActionRequest(action, resolveCsrfToken(table), resolveCsrfHeaderName(table), extra);
		if (req.refuse) {
			renderRowActionRefusal(tr, action, req.reason);
			return;
		}
		setRowInFlight(tr, true);
		fetch(req.url, { method: req.method, headers: req.headers, body: req.body, credentials: "same-origin" })
			.then(function (resp) {
				settleActionResponse(resp, action, table, tr, ctx);
			})
			.catch(function () {
				// A network-level failure is itself a terminal outcome: clear the marker (so polling resumes) and
				// render a visible refusal rather than leaving the row stuck in-flight.
				setRowInFlight(tr, false);
				renderRowActionRefusal(tr, action, "request-failed");
			});
	}

	/**
	 * Whether `table` currently has a row marked in-flight (design doc §9.1 B5) - see hasInFlightRow above; this is
	 * the setter half.  Marks the `<tr>` in-flight for an OUTSTANDING SYNCHRONOUS WRITE and disables its action
	 * trigger so a double-click cannot issue a second write (TODO-417).  The marker is scoped to synchronous writes
	 * ONLY - a long-running async job (TODO-425) uses a distinct affordance that does not inhibit table polling,
	 * because initPolling skips the WHOLE table's poll while any row carries this marker.
	 */
	function setRowInFlight(tr, on) {
		if (!tr) return;
		if (on) tr.setAttribute("data-juneau-inflight", "1");
		else tr.removeAttribute("data-juneau-inflight");
		const trigger = tr.querySelector ? tr.querySelector(".juneau-view-action-trigger") : null;
		if (trigger) trigger.disabled = !!on;
	}

	/**
	 * Settles a row from an action-submit response - the TODO-416/417 join point.  It ALWAYS clears the in-flight
	 * marker and re-enables the action FIRST (on success, failure, refusal, unknown, AND a transport refusal), so a
	 * stuck marker can never freeze the whole table's polling (MED-4); then it renders the outcome:
	 *   - non-2xx  -> a visible TRANSPORT refusal built from `X-Loopback-Boundary` + the `{reason,message}` envelope,
	 *                 WITHOUT requiring the typed action-result schema (HIGH-3);
	 *   - 2xx + typed ActionResult -> success (merge/redraw the row from the authoritative payload), failure,
	 *                 named refusal, or a non-optimistic unknown - every outcome is rendered, none is silent;
	 *   - 2xx + no typed body -> a bare success (the pre-416 behavior: redraw for an onSuccess=redraw action).
	 */
	function settleActionResponse(resp, action, table, tr, ctx) {
		setRowInFlight(tr, false);   // EVERY terminal outcome clears the marker first - polling must always resume.
		if (! resp) { renderActionOutcome(tr, { outcome: "unknown" }); return; }

		if (! resp.ok) {
			const boundaryReason = (resp.headers && typeof resp.headers.get === "function")
				? resp.headers.get("X-Loopback-Boundary") : null;
			readBodyText(resp).then(function (text) {
				const t = transportRefusal(resp.status, boundaryReason, parseJsonSafe(text));
				renderActionOutcome(tr, { outcome: "refusal", transport: true, refusalCode: t.code, message: t.message });
			});
			return;
		}

		readBodyText(resp).then(function (text) {
			// TODO-425: an ASYNC action's start POST returns a job pointer (not a terminal result).  The in-flight
			// marker was ALREADY cleared above (first line of settle), so table polling has resumed BEFORE the long
			// job runs - the job then uses the DISTINCT job-running affordance (data-juneau-job), never
			// data-juneau-inflight, so hasInFlightRow/initPolling never freeze the whole table for the job's
			// duration (HIGH-9).
			const started = parseJobStarted(text);
			if (started) {
				startJobStream(started, action, table, tr, ctx);
				return;
			}
			const result = parseActionResult(text);
			if (! result) {                    // bare 2xx, no typed result -> pre-416 success behavior.
				applySuccessBehavior(action, table, tr, ctx, null);
				return;
			}
			if (result.contractVersion != null && result.contractVersion !== JUNEAU_ACTION_RESULT_CONTRACT_VERSION) {
				renderActionOutcome(tr, { outcome: "unknown",
					message: "action-result contract mismatch (page='" + result.contractVersion +
						"', runtime='" + JUNEAU_ACTION_RESULT_CONTRACT_VERSION + "')" });
				return;
			}
			const outcome = normalizeOutcome(result);
			if (outcome === "success") {
				applySuccessBehavior(action, table, tr, ctx, result);
				renderActionOutcome(tr, { outcome: "success", replay: !! result.replay, message: result.message });
			} else {
				renderActionOutcome(tr, {
					outcome: outcome, refusalCode: result.refusalCode, message: result.message, replay: !! result.replay
				});
			}
		});
	}

	/** Reads a fetch Response body as text, defensively (a stubbed/absent body resolves to "" rather than throwing). */
	function readBodyText(resp) {
		if (resp && typeof resp.text === "function") {
			try { return Promise.resolve(resp.text()); } catch (e) { return Promise.resolve(""); }
		}
		return Promise.resolve("");
	}

	/**
	 * Applies a successful action's onSuccess behavior: `mergeRow` re-renders the row from the result's authoritative
	 * payload (TODO-417), `redraw` reloads the table, `navigate` is left to the consumer.  A bare success (no typed
	 * result) with onSuccess=redraw still redraws, preserving the pre-416 direct-submit behavior.
	 */
	function applySuccessBehavior(action, table, tr, ctx, result) {
		if (result && result.message)
			paintActionMessageIntoDetail(tr, action && action.id, result.message);
		if (action.onSuccess === "mergeRow" && result && result.row != null) {
			mergeRowFromResult(tr, ctx, result.row);
		} else if (action.onSuccess === "redraw" && ctx && ctx.redraw) {
			ctx.redraw();
		}
	}

	/**
	 * Re-renders a row from the ActionResult's authoritative row payload (the `MERGE_ROW` case).  Prefers
	 * DataTables' native row API when present; otherwise fires an optional `ctx.mergeRow(tr, rowData)` hook.  Always
	 * stamps `data-juneau-row-merged` on the row so the re-render is observable (and so a host can style a
	 * just-updated row).
	 */
	function mergeRowFromResult(tr, ctx, rowData) {
		let mergedViaDt = false;
		try {
			if (ctx && ctx.dataTable && typeof ctx.dataTable.row === "function") {
				const row = ctx.dataTable.row(tr);
				if (row && typeof row.data === "function") {
					row.data(rowData);
					if (typeof row.draw === "function") row.draw(false);
					mergedViaDt = true;
				}
			}
		} catch (e) { mergedViaDt = false; }
		if (! mergedViaDt && ctx && typeof ctx.mergeRow === "function") ctx.mergeRow(tr, rowData);
		if (tr && tr.setAttribute) tr.setAttribute("data-juneau-row-merged", "1");
	}

	/**
	 * Renders a VISIBLE settled-outcome banner into the row's actions cell (anti-silent-degradation): success gets a
	 * `role=status`, everything else (failure, refusal, transport refusal, cancelled, unknown) gets a `role=alert`.
	 * Distinct from renderRowActionRefusal (which is the pre-flight fail-closed refusal); this is the post-response
	 * outcome.
	 */
	function renderActionOutcome(tr, cls) {
		const cell = tr.querySelector ? (tr.querySelector(".juneau-view-actions-cell") || tr.lastElementChild || tr) : tr;
		let banner = cell.querySelector ? cell.querySelector(".juneau-view-action-outcome") : null;
		if (! banner) {
			banner = document.createElement("div");
			banner.className = "juneau-view-action-outcome";
			banner.setAttribute("data-testid", "action-outcome");
			cell.appendChild(banner);
		}
		const state = cls.outcome || "unknown";
		banner.setAttribute("data-state", state);
		banner.setAttribute("role", state === "success" ? "status" : "alert");
		banner.textContent = actionOutcomeMessage(cls);
	}

	// ==================================================================================================================
	// ASYNC JOBS (TODO-425): a long-running job streamed over SSE, with a DISTINCT running affordance
	// ==================================================================================================================

	/**
	 * Marks a row as running a LONG async job (TODO-425) - deliberately a DIFFERENT attribute
	 * (`data-juneau-job`) from the synchronous in-flight marker (`data-juneau-inflight`).  This is the whole point
	 * of HIGH-9: hasInFlightRow (and therefore initPolling) freezes the ENTIRE table's polling while ANY row
	 * carries `data-juneau-inflight`, which is correct for a short synchronous write but catastrophic for a job
	 * that can run up to the server's 120s hard timeout - it would hide a resurrected incident on a PagerDuty
	 * table for the whole job.  A job therefore NEVER sets `data-juneau-inflight`; it sets this distinct marker,
	 * which hasInFlightRow does NOT read, so the table keeps polling for the entire duration of the job.  The
	 * action trigger is disabled while running so a second job cannot be started on the same row.
	 */
	function setRowJobRunning(tr, on) {
		if (!tr) return;
		if (on) tr.setAttribute("data-juneau-job", "1");
		else tr.removeAttribute("data-juneau-job");
		const trigger = tr.querySelector ? tr.querySelector(".juneau-view-action-trigger") : null;
		if (trigger) trigger.disabled = !!on;
	}

	/**
	 * Renders (and updates) the VISIBLE, live job-progress banner in the row's actions cell, plus a Cancel button
	 * wired to the job's cancelUrl.  The progress text is painted with `textContent` ONLY (never innerHTML) - the
	 * streamed content is customer-adjacent and this origin holds the CSRF token, so a typed/escaped path is the
	 * only safe one (same invariant as the modal fields, BLK-1/MED-9).  Distinct `data-testid`/class from the
	 * settled-outcome banner so a running job and a terminal outcome are never confused.
	 */
	function renderJobProgress(tr, text, started, table) {
		const cell = tr.querySelector ? (tr.querySelector(".juneau-view-actions-cell") || tr.lastElementChild || tr) : tr;
		let banner = cell.querySelector ? cell.querySelector(".juneau-view-job-progress") : null;
		if (! banner) {
			banner = document.createElement("div");
			banner.className = "juneau-view-job-progress";
			banner.setAttribute("data-testid", "job-progress");
			banner.setAttribute("role", "status");
			const msg = document.createElement("span");
			msg.className = "juneau-view-job-progress-msg";
			banner.appendChild(msg);
			if (started && ! isBlankToken(started.cancelUrl)) {
				const cancel = document.createElement("button");
				cancel.type = "button";
				cancel.className = "juneau-view-job-cancel";
				cancel.textContent = "Cancel";
				cancel.addEventListener("click", function () { cancelJob(started, table, tr); });
				banner.appendChild(cancel);
			}
			cell.appendChild(banner);
		}
		const msgEl = banner.querySelector(".juneau-view-job-progress-msg");
		if (msgEl) msgEl.textContent = (text == null ? "" : String(text));
	}

	/** Removes the live job-progress banner from a row (called when the job settles to a terminal outcome). */
	function clearJobProgress(tr) {
		const cell = tr.querySelector ? (tr.querySelector(".juneau-view-actions-cell") || tr.lastElementChild || tr) : tr;
		const banner = cell.querySelector ? cell.querySelector(".juneau-view-job-progress") : null;
		if (banner && banner.parentNode) banner.parentNode.removeChild(banner);
	}

	/**
	 * Opens the SSE progress stream for a started async job and drives the row through its lifecycle (TODO-425).
	 * The stream URL ITSELF is the capability (an unguessable >=128-bit token) - a browser EventSource CANNOT set
	 * an X-Csrf-Token header, so unguessability is the access control, not a CSRF header (HIGH-4).  `progress`
	 * events update the live banner; the single terminal `result` event carries the typed ActionResult and settles
	 * the row (reusing the same normalize/contract-check/render path as a synchronous result, so cancelled /
	 * cancelled-after-effect render via actionOutcomeMessage without any new UI).  A stream error is itself a
	 * non-optimistic terminal outcome.  Throughout, the row carries only the DISTINCT `data-juneau-job` marker, so
	 * the table keeps polling (HIGH-9).
	 */
	function startJobStream(started, action, table, tr, ctx) {
		if (typeof EventSource === "undefined") {
			// No SSE transport in this browser - a visible, non-optimistic outcome; polling was never frozen.
			renderActionOutcome(tr, { outcome: "unknown", message: "live progress is unavailable in this browser" });
			return null;
		}
		setRowJobRunning(tr, true);
		renderJobProgress(tr, "Working\u2026", started, table);
		const es = new EventSource(started.streamUrl);
		const st = { settled: false };
		function finish(result, fallback) {
			if (st.settled) return;
			st.settled = true;
			es.close();
			if (ctx && ctx._jobSources) ctx._jobSources.delete(es);
			setRowJobRunning(tr, false);
			clearJobProgress(tr);
			if (result) finishJobFromResult(action, table, tr, ctx, result);
			else renderActionOutcome(tr, fallback);
		}
		es.addEventListener("progress", function (e) {
			if (! st.settled) {
				renderJobProgress(tr, e.data, started, table);
				paintActionMessageIntoDetail(tr, action && action.id, e.data);
			}
		});
		es.addEventListener("result", function (e) {
			finish(parseActionResult(e.data), { outcome: "unknown", message: "the job produced no readable result" });
		});
		es.addEventListener("error", function () {
			finish(null, { outcome: "unknown", message: "the progress stream was interrupted" });
		});
		if (ctx) {
			if (!ctx._jobSources) ctx._jobSources = new Set();
			ctx._jobSources.add(es);
		}
		return es;
	}

	/**
	 * Settles a row from an async job's terminal ActionResult (the SSE `result` event) - the async twin of
	 * settleActionResponse's 2xx typed branch, reusing the SAME contract-version handshake, outcome normalization,
	 * success behavior and visible-outcome render.  cancelled / cancelled-after-effect are just outcomes here;
	 * actionOutcomeMessage already renders both.
	 */
	function finishJobFromResult(action, table, tr, ctx, result) {
		if (! result) { renderActionOutcome(tr, { outcome: "unknown" }); return; }
		if (result.contractVersion != null && result.contractVersion !== JUNEAU_ACTION_RESULT_CONTRACT_VERSION) {
			renderActionOutcome(tr, { outcome: "unknown",
				message: "action-result contract mismatch (page='" + result.contractVersion +
					"', runtime='" + JUNEAU_ACTION_RESULT_CONTRACT_VERSION + "')" });
			return;
		}
		const outcome = normalizeOutcome(result);
		if (outcome === "success") {
			applySuccessBehavior(action, table, tr, ctx, result);
			renderActionOutcome(tr, { outcome: "success", replay: !! result.replay, message: result.message });
		} else {
			renderActionOutcome(tr, {
				outcome: outcome, refusalCode: result.refusalCode, message: result.message, replay: !! result.replay
			});
		}
	}

	/**
	 * Requests cancellation of a running job (fail-closed CSRF POST to the job's cancelUrl).  The SERVER is
	 * authoritative - the terminal cancelled / cancelled-after-effect outcome still arrives over the SSE `result`
	 * event, so this only asks; a blank token or missing cancelUrl renders a visible refusal and sends nothing.
	 */
	function cancelJob(started, table, tr) {
		const req = buildJobCancelRequest(started && started.cancelUrl, resolveCsrfToken(table), resolveCsrfHeaderName(table));
		if (req.refuse) {
			renderRowActionRefusal(tr, { id: "cancel", label: "Cancel" }, req.reason === "missing-token" ? "missing-token" : "request-failed");
			return;
		}
		fetch(req.url, { method: req.method, headers: req.headers, body: req.body, credentials: "same-origin" })
			.catch(function () { renderRowActionRefusal(tr, { id: "cancel", label: "Cancel" }, "request-failed"); });
	}

	/**
	 * Opens a `present=dialog` action's modal overlay (TODO-416).  When the action declares a form-source URL, the
	 * modal-open confirmation is a READ-ONLY GET that returns the typed ModalDef JSON (confirmation fields + the
	 * server-minted idempotency key) - it never mutates (HIGH-7); its typed fields are painted with `textContent`
	 * (never `innerHTML`, never raw markup - BLK-1/MED-9).  With no form URL the dialog is a confirm-only prompt
	 * from the declared `confirm` text.  The mutation is the SEPARATE non-safe submit the confirm button issues.
	 */
	function openActionDialog(action, table, tr, ctx) {
		// v1 depth cap (counts dialog-kind layers): a third dialog is a visible refusal inside the current top dialog.
		if (dialogLayerCount() >= MAX_DIALOG_DEPTH) { renderDialogDepthRefusal(); return; }
		if (isBlankToken(action.form)) {
			// Confirm-only LOCAL path (no form-source URL): unversioned, and never fail-loud on a missing version.
			showActionDialog({ title: action.confirm || action.label || action.id }, action, table, tr, ctx);
			return;
		}
		fetch(action.form, { method: "GET", credentials: "same-origin", headers: { "Accept": "application/json" } })
			.then(function (resp) {
				if (! resp || ! resp.ok) {
					// A non-2xx on the read-only confirmation fetch is itself a visible transport refusal - the
					// modal never opens optimistically on a boundary rejection.
					const boundaryReason = (resp && resp.headers && typeof resp.headers.get === "function")
						? resp.headers.get("X-Loopback-Boundary") : null;
					return readBodyText(resp).then(function (text) {
						const t = transportRefusal(resp ? resp.status : 0, boundaryReason, parseJsonSafe(text));
						renderActionOutcome(tr, { outcome: "refusal", transport: true, refusalCode: t.code, message: t.message });
					});
				}
				return readBodyText(resp).then(function (text) {
					const payload = parseJsonSafe(text);
					if (! payload) {
						// A form-source GET that did not parse: do NOT optimistically open a title-only dialog once forms
						// exist (SF-6).  Treat as a form-bearing-expected failure -> visible refusal.
						renderRowActionRefusal(tr, action, "request-failed");
						return;
					}
					if (payload.form) {
						// Form present -> fail-loud handshake: BOTH the modal top-level and nested form contractVersion
						// must equal the baked-in "1", or a visible refusal and the dialog does not open (h5).
						if (payload.contractVersion !== JUNEAU_DIALOG_FORM_CONTRACT_VERSION
							|| payload.form.contractVersion !== JUNEAU_DIALOG_FORM_CONTRACT_VERSION) {
							renderActionOutcome(tr, { outcome: "refusal",
								message: "dialog-form contract mismatch (modal='" + payload.contractVersion +
									"', form='" + payload.form.contractVersion + "', runtime='" +
									JUNEAU_DIALOG_FORM_CONTRACT_VERSION + "')" });
							return;
						}
					}
					// Confirm-only fetched envelope (no form): unversioned; do NOT test contractVersion.
					showActionDialog(payload, action, table, tr, ctx);
				});
			})
			.catch(function () { renderRowActionRefusal(tr, action, "request-failed"); });
	}

	/**
	 * Builds a `present=dialog` overlay DOM node (a backdrop + a `role=dialog` box) from a ModalDef, painting the
	 * title and each typed confirmation field with `textContent` ONLY - never `innerHTML`, never raw markup, never a
	 * live-data HTML blob (BLK-1/MED-9): live/remote data is attacker-influenceable and this origin holds the CSRF
	 * token, so a typed/escaped path is the only safe one here.  Returns the pieces so the caller can wire buttons.
	 */
	function buildDialogOverlay(modal, action, table, tr, ctx, seq) {
		const backdrop = document.createElement("div");
		backdrop.className = "juneau-view-dialog-backdrop";
		backdrop.setAttribute("data-testid", "dialog-backdrop");

		const dialog = document.createElement("div");
		dialog.className = "juneau-view-dialog";
		dialog.setAttribute("role", "dialog");
		dialog.setAttribute("aria-modal", "true");

		const title = document.createElement("h2");
		title.className = "juneau-view-dialog-title";
		title.textContent = (modal && modal.title) || (action && (action.confirm || action.label || action.id)) || "Confirm";
		dialog.appendChild(title);

		if (modal && modal.fields && modal.fields.length) {
			const dl = document.createElement("dl");
			dl.className = "juneau-view-dialog-fields";
			dl.setAttribute("data-testid", "dialog-fields");
			modal.fields.forEach(function (f) {
				const dt = document.createElement("dt");
				dt.textContent = (f && f.label != null) ? String(f.label) : "";
				const dd = document.createElement("dd");
				dd.textContent = (f && f.value != null) ? String(f.value) : "";
				dl.appendChild(dt);
				dl.appendChild(dd);
			});
			dialog.appendChild(dl);
		}

		appendDialogForm(dialog, modal && modal.form, table, tr, ctx, seq);

		const actions = document.createElement("div");
		actions.className = "juneau-view-dialog-actions";
		const cancelBtn = document.createElement("button");
		cancelBtn.type = "button";
		cancelBtn.className = "juneau-view-dialog-cancel";
		cancelBtn.textContent = "Cancel";
		const confirmBtn = document.createElement("button");
		confirmBtn.type = "button";
		confirmBtn.className = "juneau-view-dialog-confirm";
		confirmBtn.textContent = (action && action.label) || "Confirm";
		actions.appendChild(cancelBtn);
		actions.appendChild(confirmBtn);
		dialog.appendChild(actions);

		backdrop.appendChild(dialog);
		return { backdrop: backdrop, dialog: dialog, confirmBtn: confirmBtn, cancelBtn: cancelBtn };
	}

	/**
	 * Whether `type` is a legal FormDef.Input token the client paints (the frozen v1 6-type allowlist: text, textarea,
	 * checkbox, toggle, select, action).  Anything else is skipped so a hostile type token cannot become an element.
	 */
	function isTypedFormInputType(type) {
		return type === "text" || type === "textarea" || type === "checkbox" || type === "toggle"
			|| type === "select" || type === "action";
	}

	/** Whether a checkbox/toggle prefill token means "checked": "true" (case-insensitive) or "on" (decision 9). */
	function isCheckedToken(v) {
		if (v == null) return false;
		const s = String(v).trim().toLowerCase();
		return s === "true" || s === "on";
	}

	/**
	 * Paints ONE typed FormDef.Input control into `row` via createElement (never innerHTML).  Dispatches on the frozen
	 * 6-type allowlist - text/textarea (native inputs), checkbox, toggle (checkbox + role=switch), select (native
	 * &lt;select&gt; with &lt;option&gt; text via textContent), action (a &lt;button type=button&gt; that opens a nested
	 * dialog for the named RowAction).  Prefills use `.value` / `.checked`.  Unknown types are skipped (returns null).
	 * A `type=action` whose id is absent from the enclosing view's `rowActions` catalog is painted DISABLED (a visible,
	 * fail-closed refusal at paint time; never a throw - SF-1 / H-P4-S1).
	 */
	function paintFormControl(row, f, id, table, tr, ctx) {
		const type = (f.type == null || f.type === "") ? "text" : String(f.type);
		if (! isTypedFormInputType(type)) return null;
		let control;
		if (type === "textarea") {
			control = document.createElement("textarea");
		} else if (type === "select") {
			control = document.createElement("select");
			const options = f.options || [];
			for (let i = 0; i < options.length; i++) {
				const o = options[i];
				if (! o || o.value == null) continue;
				const opt = document.createElement("option");
				opt.value = String(o.value);
				opt.textContent = o.label != null ? String(o.label) : String(o.value);
				control.appendChild(opt);
			}
		} else if (type === "checkbox" || type === "toggle") {
			control = document.createElement("input");
			control.type = "checkbox";
			if (type === "toggle") {
				control.setAttribute("role", "switch");
				control.className = "juneau-view-toggle";
			}
			const on = isCheckedToken(f.value);
			control.checked = on;
			control.setAttribute("aria-checked", on ? "true" : "false");
			control.addEventListener("change", function () {
				control.setAttribute("aria-checked", control.checked ? "true" : "false");
			});
		} else if (type === "action") {
			control = document.createElement("button");
			control.type = "button";
			control.className = "juneau-view-dialog-form-action";
			control.textContent = f.label != null ? String(f.label) : String(f.name);
			control.name = String(f.name);
			control.setAttribute("data-juneau-form-field", String(f.name));
			const actionId = f.actionId;
			if (! dialogActionIsOpenable(ctx, actionId)) {
				control.disabled = true;
				control.setAttribute("aria-disabled", "true");
				control.setAttribute("data-juneau-action-missing", "1");
			}
			control.addEventListener("click", function () { openFormActionDialog(actionId, table, tr, ctx); });
			row.appendChild(control);
			return control;
		} else {
			control = document.createElement("input");
			control.type = "text";
		}
		control.id = id;
		control.name = String(f.name);
		control.setAttribute("data-juneau-form-field", String(f.name));
		if (f.required) { control.required = true; control.setAttribute("aria-required", "true"); }
		if (type === "select" && f.value != null) control.value = String(f.value);
		if (type === "text" || type === "textarea") {
			if (f.value != null) control.value = String(f.value);
			if (f.maxLength != null) {
				const ml = parseInt(f.maxLength, 10);
				if (! isNaN(ml) && ml > 0) control.maxLength = ml;
			}
			if (f.pattern != null) control.setAttribute("data-juneau-pattern", String(f.pattern));
		}
		row.appendChild(control);
		return control;
	}

	/** Whether `actionId` names a present=dialog RowAction on the enclosing view (paint-time catalog check, SF-1). */
	function dialogActionIsOpenable(ctx, actionId) {
		const catalog = (ctx && ctx.viewDef && ctx.viewDef.rowActions) || [];
		for (let i = 0; i < catalog.length; i++)
			if (catalog[i] && catalog[i].id === actionId && isDialogAction(catalog[i])) return true;
		return false;
	}

	/**
	 * A `type=action` button click: opens a nested dialog for the named RowAction on the enclosing view WITHOUT
	 * closing the parent dialog (pushes a child layer).  A missing / non-dialog action is a visible refusal painted
	 * into the current top dialog - never a throw (SF-1).
	 */
	function openFormActionDialog(actionId, table, tr, ctx) {
		if (! dialogActionIsOpenable(ctx, actionId)) { renderDialogActionRefusal(actionId); return; }
		let target = null;
		const catalog = (ctx && ctx.viewDef && ctx.viewDef.rowActions) || [];
		for (let i = 0; i < catalog.length; i++) if (catalog[i] && catalog[i].id === actionId) { target = catalog[i]; break; }
		openActionDialog(target, table, tr, ctx);
	}

	/** Paints a visible fail-closed refusal into the current top dialog (used for a missing/non-dialog action id). */
	function renderDialogActionRefusal(actionId) {
		renderTopDialogNotice("juneau-view-dialog-action-refusal", "dialog-action-refusal",
			"Action '" + String(actionId) + "' is not available.");
	}

	/** Paints the depth-cap refusal into the CURRENT top dialog (H-P5-S5), not a row-cell banner behind two backdrops. */
	function renderDialogDepthRefusal() {
		renderTopDialogNotice("juneau-view-dialog-depth-refusal", "dialog-depth-refusal",
			"Only " + MAX_DIALOG_DEPTH + " stacked dialogs are supported.");
	}

	function renderTopDialogNotice(cls, testid, text) {
		const top = topLayer();
		const el = top && top.el;
		const host = (el && el.querySelector && el.querySelector(".juneau-view-dialog")) || el;
		if (! host || ! host.appendChild) return;
		let banner = host.querySelector ? host.querySelector("." + cls) : null;
		if (! banner) {
			banner = document.createElement("div");
			banner.className = cls;
			banner.setAttribute("role", "alert");
			banner.setAttribute("data-testid", testid);
			host.appendChild(banner);
		}
		banner.textContent = text;
	}

	/**
	 * Paints the typed FormDef inputs as native label+control rows via createElement (never innerHTML, never the
	 * `form.template` markup sink - that is a server-author reference the client ignores).  Each row: a label bound by
	 * `for` to the control id, the control (via paintFormControl), an optional help hint, and an (initially empty)
	 * per-control error sibling.  Field ids use a dialog-only sequence (`juneau-dialog-field-<seq>-<name>`) so stacked
	 * dialogs with the same field name do not collide and popover layers do not shift ids (N-3).
	 */
	function appendDialogForm(dialog, form, table, tr, ctx, seq) {
		if (!dialog || !form || !form.fields || !form.fields.length) return;
		const wrap = document.createElement("div");
		wrap.className = "juneau-view-dialog-form";
		wrap.setAttribute("data-testid", "dialog-form");
		form.fields.forEach(function (f) {
			if (!f || f.name == null || String(f.name) === "") return;
			const type = (f.type == null || f.type === "") ? "text" : String(f.type);
			if (! isTypedFormInputType(type)) return;
			const row = document.createElement("div");
			row.className = "juneau-view-dialog-form-row";
			const id = "juneau-dialog-field-" + String(seq == null ? 0 : seq) + "-" + String(f.name);
			if (type !== "action") {
				const label = document.createElement("label");
				label.setAttribute("for", id);
				label.textContent = f.label != null ? String(f.label) : String(f.name);
				row.appendChild(label);
			}
			const control = paintFormControl(row, f, id, table, tr, ctx);
			if (! control) return;   // unknown type skipped
			if (type !== "action") {
				let helpId = null;
				if (f.help != null && String(f.help) !== "") {
					const help = document.createElement("div");
					help.className = "juneau-view-dialog-form-help";
					help.id = id + "-help";
					help.setAttribute("data-juneau-help", "1");
					help.textContent = String(f.help);
					row.appendChild(help);
					helpId = help.id;
				}
				const err = document.createElement("div");
				err.className = "juneau-view-dialog-form-error";
				err.id = id + "-error";
				err.setAttribute("data-juneau-error-for", String(f.name));
				err.setAttribute("aria-live", "polite");
				row.appendChild(err);
				// Attach live references / ids for validation's aria-describedby concatenation (help present at paint;
				// error id added only when invalid - S5).
				control._juneauErrorEl = err;
				control._juneauHelpId = helpId;
				if (helpId) control.setAttribute("aria-describedby", helpId);
				bindControlValidation(dialog, control);
			}
			wrap.appendChild(row);
		});
		if (wrap.childNodes.length)
			dialog.appendChild(wrap);
	}

	/** Wires per-control blur/input revalidation (advisory, non-alert): keeps the error sibling fresh as the user types. */
	function bindControlValidation(dialog, control) {
		if (! control || typeof control.addEventListener !== "function") return;
		const revalidate = function () { applyControlValidity(control, validateOneControl(control), false); };
		control.addEventListener("blur", revalidate);
		control.addEventListener("input", revalidate);
		control.addEventListener("change", revalidate);
	}

	/**
	 * Client-side inline validation (advisory to the user - the server submit stays fully authoritative).  Per
	 * collectable control: required-empty (checkbox/toggle required = must be checked), `pattern` mismatch and
	 * `maxLength` exceeded.  A `new RegExp` that throws (a Java-only pattern) FAIL-OPENS that one rule (skip it; do not
	 * block submit).  On failure paints `aria-invalid`, refreshes the error sibling and concatenates `aria-describedby`
	 * (help + error); when triggered by confirm, focuses the first invalid control and sets role=alert on its error.
	 * Returns true when the form may submit.
	 */
	function validateDialogForm(dialog, fromConfirm) {
		if (!dialog || !dialog.querySelectorAll) return true;
		const nodes = dialog.querySelectorAll("[data-juneau-form-field]");
		let firstInvalid = null;
		for (let i = 0; i < nodes.length; i++) {
			const el = nodes[i];
			const tag = el.tagName ? String(el.tagName).toLowerCase() : "";
			if (tag === "button") continue;   // type=action buttons are not values
			const msg = validateOneControl(el);
			applyControlValidity(el, msg, fromConfirm);
			if (msg && ! firstInvalid) firstInvalid = el;
		}
		if (firstInvalid && fromConfirm && typeof firstInvalid.focus === "function") {
			try { firstInvalid.focus(); } catch (e) { /* ignore */ }
		}
		return ! firstInvalid;
	}

	/** Validates one control; returns an error message string, or null when valid. */
	function validateOneControl(el) {
		const tag = el.tagName ? String(el.tagName).toLowerCase() : "";
		const required = !! el.required || (el.getAttribute && el.getAttribute("aria-required") === "true");
		if (tag === "input" && String(el.type).toLowerCase() === "checkbox") {
			if (required && ! el.checked) return "This must be checked.";
			return null;
		}
		const value = el.value != null ? String(el.value) : "";
		if (required && value.trim() === "") return "This field is required.";
		if (value !== "") {
			const max = el.maxLength;
			if (typeof max === "number" && max > 0 && value.length > max)
				return "Must be at most " + max + " characters.";
			const pat = el.getAttribute ? el.getAttribute("data-juneau-pattern") : null;
			if (pat) {
				let re = null;
				try { re = new RegExp(pat); } catch (e) { re = null; }   // FAIL-OPEN on a Java-only pattern
				if (re && ! re.test(value)) return "Value is not in the expected format.";
			}
		}
		return null;
	}

	/** Applies (or clears) a control's validity: aria-invalid, the error sibling text/role, and aria-describedby. */
	function applyControlValidity(el, msg, fromConfirm) {
		const err = el._juneauErrorEl || null;
		const helpId = el._juneauHelpId || null;
		if (msg) {
			el.setAttribute("aria-invalid", "true");
			if (err) {
				err.textContent = msg;
				if (fromConfirm) err.setAttribute("role", "alert");
				else err.removeAttribute("role");
			}
			const ids = [];
			if (helpId) ids.push(helpId);
			if (err && err.id) ids.push(err.id);
			if (ids.length) el.setAttribute("aria-describedby", ids.join(" "));
		} else {
			el.removeAttribute("aria-invalid");
			if (err) { err.textContent = ""; err.removeAttribute("role"); }
			if (helpId) el.setAttribute("aria-describedby", helpId);
			else if (el.removeAttribute) el.removeAttribute("aria-describedby");
		}
	}

	/**
	 * Reads collectable typed form controls from a dialog into a `{ name: value }` map for the submit body: text /
	 * textarea / select via `.value`, checkbox / toggle via `.checked` -> "true"/"false" (unchecked always submits an
	 * explicit "false", never omitted - decision 9).  `type=action` buttons are skipped.  Never reads control
	 * textContent, never innerHTML.
	 */
	function collectDialogFormFields(dialog) {
		const out = {};
		if (!dialog || !dialog.querySelectorAll) return out;
		const nodes = dialog.querySelectorAll("[data-juneau-form-field]");
		for (let i = 0; i < nodes.length; i++) {
			const el = nodes[i];
			const name = el.getAttribute ? el.getAttribute("data-juneau-form-field") : null;
			if (name == null || name === "") continue;
			const tag = el.tagName ? String(el.tagName).toLowerCase() : "";
			if (tag === "button") continue;   // type=action buttons carry no submit value
			if (tag === "select" || tag === "textarea") {
				out[name] = el.value != null ? String(el.value) : "";
				continue;
			}
			if (tag === "input") {
				const itype = el.type ? String(el.type).toLowerCase() : "text";
				if (itype === "checkbox") { out[name] = el.checked ? "true" : "false"; continue; }
				out[name] = el.value != null ? String(el.value) : "";
			}
		}
		return out;
	}

	/**
	 * Shows a dialog overlay for an action as a modal layer on the shared {@code popupLayerStack} and wires its confirm
	 * (validate -> submit) / cancel (dismiss) buttons.  The backdrop is pushed as a {@code kind:"dialog"} focus-trapping
	 * layer (portal to body, no light-dismiss): Escape / z-order / focus-restore are owned by the stack.  A dialog-only
	 * {@code seq} namespaces the form field ids.  Confirm runs {@link validateDialogForm} first and only submits when
	 * the client-side form is valid (fail-loud, advisory to the authoritative server submit).  A push that would exceed
	 * the depth cap is a visible refusal inside the current top dialog rather than a new overlay (H-P5-S5).
	 */
	function showActionDialog(modal, action, table, tr, ctx) {
		if (dialogLayerCount() >= MAX_DIALOG_DEPTH) { renderDialogDepthRefusal(); return null; }
		const seq = ++dialogSeq;
		const ui = buildDialogOverlay(modal, action, table, tr, ctx, seq);
		function close() { popLayer(ui.backdrop); }
		ui.cancelBtn.addEventListener("click", close);
		ui.confirmBtn.addEventListener("click", function () {
			if (! validateDialogForm(ui.dialog, true)) return;   // fail-loud client validation before the submit
			const fields = collectDialogFormFields(ui.dialog);
			close();
			submitActionDialog(modal, action, table, tr, ctx, fields);
		});
		if (ctx) {
			if (! ctx._dialogStack) ctx._dialogStack = [];
			ctx._dialogStack.push(ui.backdrop);
			ctx._actionDialog = ui.backdrop;
		}
		pushLayer(ui.backdrop, {
			kind: "dialog", portal: true, trapFocus: true, lightDismiss: false, detachOnPop: true,
			onDismiss: function () {
				if (ctx && ctx._dialogStack) {
					const i = ctx._dialogStack.indexOf(ui.backdrop);
					if (i >= 0) ctx._dialogStack.splice(i, 1);
					ctx._actionDialog = ctx._dialogStack.length ? ctx._dialogStack[ctx._dialogStack.length - 1] : null;
				}
			}
		});
		return ui;
	}

	/**
	 * Issues the dialog's non-safe submit, carrying the server-minted idempotency key and the row's targetId so the
	 * server can check the key's `(action, targetId)` binding (HIGH-8) - a double-click / re-submit / browser retry
	 * therefore all carry the SAME key.  Typed FormDef values collected via `.value` ride in `extra.fields`.
	 * Delegates the fail-closed CSRF submit + in-flight marker + typed-result settling to submitRowAction(...).
	 */
	function submitActionDialog(modal, action, table, tr, ctx, fields) {
		const extra = {};
		const targetId = (tr && tr.getAttribute) ? tr.getAttribute("data-juneau-row-id") : null;
		if (targetId != null) extra.targetId = targetId;
		if (modal && modal.idempotencyKey != null) extra.idempotencyKey = modal.idempotencyKey;
		if (fields && typeof fields === "object") {
			const keys = Object.keys(fields);
			if (keys.length) extra.fields = fields;
		}
		submitRowAction(action, table, tr, ctx, extra);
	}

	/** Renders a VISIBLE row-action refusal (anti-silent-degradation) into the row's actions cell. */
	function renderRowActionRefusal(tr, action, reason) {
		const cell = tr.querySelector(".juneau-view-actions-cell") || tr.lastElementChild || tr;
		let banner = cell.querySelector ? cell.querySelector(".juneau-view-action-refusal") : null;
		if (!banner) {
			banner = document.createElement("div");
			banner.className = "juneau-view-action-refusal";
			banner.setAttribute("role", "alert");
			banner.setAttribute("data-testid", "action-refusal");
			cell.appendChild(banner);
		}
		const name = (action && (action.label || action.id)) || "action";
		banner.textContent = "Action '" + name + "' not sent: " + actionRefusalMessage(reason) + ".";
	}

	/**
	 * Builds the row-action menu element (`<ul role="menu">`) from a view's rowActions.  Each item's activation
	 * routes to submitRowAction(...); a safe-method or token-less action still renders (so the menu is honest
	 * about what is declared) but visibly refuses on activation rather than silently doing nothing.
	 */
	function buildRowActionMenu(viewDef, table, tr, ctx) {
		const menu = document.createElement("ul");
		menu.className = "juneau-view-action-menu";
		menu.setAttribute("role", "menu");
		menu.setAttribute("data-testid", "action-menu");
		(viewDef.rowActions || []).forEach(function (action) {
			const li = document.createElement("li");
			li.setAttribute("role", "none");
			const item = document.createElement("button");
			item.type = "button";
			item.className = "juneau-view-action-item";
			item.setAttribute("role", "menuitem");
			item.setAttribute("data-action-id", action.id);
			item.textContent = action.label || action.id;
			item.addEventListener("click", function () {
				closeRowActionMenus(table);
				// A present=dialog action opens the modal (confirmation + optional form) before its submit;
				// everything else is the direct fail-closed submit.
				if (isDialogAction(action)) openActionDialog(action, table, tr, ctx);
				else submitRowAction(action, table, tr, ctx);
			});
			li.appendChild(item);
			menu.appendChild(li);
		});
		return menu;
	}

	/**
	 * Removes any open row-action menu under `table` (single-open invariant).  A menu is a {@code kind:"menu"} light-
	 * dismiss layer (portalled to body), so pop those first (their onDismiss clears the ctx tracking); then defensively
	 * sweep both the table and the document for any menu appended directly to a cell (the buildRowActionMenu harness
	 * path) or otherwise orphaned.  Kept single-param to preserve the teardown call site's signature.
	 */
	function closeRowActionMenus(table) {
		const menus = [];
		for (let i = 0; i < popupLayerStack.length; i++)
			if (popupLayerStack[i].kind === "menu") menus.push(popupLayerStack[i].el);
		menus.forEach(function (el) { popLayer(el); });
		if (table && typeof table.querySelectorAll === "function")
			Array.prototype.forEach.call(table.querySelectorAll(".juneau-view-action-menu"), removeEl);
		if (typeof document !== "undefined" && typeof document.querySelectorAll === "function")
			Array.prototype.forEach.call(document.querySelectorAll(".juneau-view-action-menu"), removeEl);
	}

	/**
	 * Wires the row-action menu via ONE delegated click listener on `table` (rather than one per row): a click on a
	 * row's `.juneau-view-action-trigger` toggles a menu of that view's rowActions for that row.  The menu is portalled
	 * to body as a {@code kind:"menu"} light-dismiss layer (h4-C: cell-anchored surfaces render position:fixed on body)
	 * and positioned under its trigger; Escape / outside-click dismissal is owned by the shared stack.  Mirrors the
	 * details-expander's delegated-listener pattern above.
	 */
	/**
	 * Activates an action-bound cell pill ({@code <span data-juneau-pill role="button" data-juneau-action="<id>">}).
	 * Resolves the row from the pill's {@code <tr>} (as the row-action menu resolves it from the trigger's row), looks
	 * the action up via {@link #findRowActionById}, then takes the SAME confirm / {@code present=dialog} branch the
	 * menu takes ({@link #isDialogAction} → {@link #openActionDialog}, else {@link #submitRowAction}) - so a pill can
	 * never skip a confirmation or a form dialog.  A disabled pill (capability-gated or already in-flight) is ignored.
	 */
	function activatePillAction(pill, table, viewDef, ctx) {
		if (!pill) return;
		if (pill.getAttribute("aria-disabled") === "true"
			|| (pill.classList && pill.classList.contains && pill.classList.contains("is-disabled"))) return;
		const action = findRowActionById(viewDef, pill.getAttribute("data-juneau-action"));
		if (!action) return;
		const tr = pill.closest ? pill.closest("tr") : null;
		if (!tr) return;
		if (tr.getAttribute && tr.getAttribute("data-juneau-inflight")) return;   // in-flight guard (no double submit)
		if (isDialogAction(action)) openActionDialog(action, table, tr, ctx);
		else submitRowAction(action, table, tr, ctx);
	}

	function initRowActions(table, viewDef, ctx) {
		table.addEventListener("click", function (e) {
			// An action-bound cell pill dispatches here (NOT in initDetailsExpander, which binds only when a
			// row-detail template is present); bind no more broadly than [data-juneau-pill] so a menu/ActionBar
			// click is never stolen.
			const pill = e.target && e.target.closest ? e.target.closest("[data-juneau-pill][role=\"button\"]") : null;
			if (pill) { activatePillAction(pill, table, viewDef, ctx); return; }
			const trigger = e.target && e.target.closest ? e.target.closest(".juneau-view-action-trigger") : null;
			if (!trigger) return;
			const tr = trigger.closest("tr");
			if (!tr) return;
			const wasOpen = !! (ctx && ctx._actionMenuTrigger === trigger);
			closeRowActionMenus(table);
			if (wasOpen) return;   // second click on the open menu's trigger closes it (toggle)
			const menu = buildRowActionMenu(viewDef, table, tr, ctx);
			if (ctx) { ctx._actionMenu = menu; ctx._actionMenuTrigger = trigger; }
			pushLayer(menu, {
				kind: "menu", portal: true, lightDismiss: true, trapFocus: false, detachOnPop: true,
				returnFocusTo: trigger,
				onDismiss: function () {
					if (ctx && ctx._actionMenu === menu) { ctx._actionMenu = null; ctx._actionMenuTrigger = null; }
				}
			});
			positionCellPopover(menu, trigger);
		});
		// Keyboard parity for the (non-native) pill button: Enter/Space activate; Space must not scroll the page.
		table.addEventListener("keydown", function (e) {
			if (e.key !== "Enter" && e.key !== " " && e.key !== "Spacebar") return;
			const pill = e.target && e.target.closest ? e.target.closest("[data-juneau-pill][role=\"button\"]") : null;
			if (!pill) return;
			if (e.preventDefault) e.preventDefault();
			activatePillAction(pill, table, viewDef, ctx);
		});
	}

	function removeEl(el) {
		if (el && el.parentNode) el.parentNode.removeChild(el);
	}

	/**
	 * Tears down this ctx's whole dialog stack: snapshot {@code ctx._dialogStack} and pop each backdrop layer (bottom
	 * pop takes its nested children with it), then clear the tracking.  A defensive document sweep removes any orphaned
	 * backdrop that was never (or is no longer) a registered layer.
	 */
	function closeActionDialog(ctx) {
		if (ctx && ctx._dialogStack && ctx._dialogStack.length) {
			const snapshot = ctx._dialogStack.slice();
			for (let i = snapshot.length - 1; i >= 0; i--) popLayer(snapshot[i]);
			ctx._dialogStack = [];
		}
		const el = ctx && ctx._actionDialog;
		if (el && el.parentNode) el.parentNode.removeChild(el);
		if (ctx) ctx._actionDialog = null;
		Array.prototype.forEach.call(document.querySelectorAll(".juneau-view-dialog-backdrop"), removeEl);
	}

	function stripGeneratedDom(table) {
		const wrapper = findViewWrapper(table);
		if (wrapper) {
			Array.prototype.forEach.call(wrapper.querySelectorAll(".juneau-view-toolbar-row"), removeEl);
		}
		Array.prototype.forEach.call(table.querySelectorAll("thead tr.juneau-view-columnsearch-row"), removeEl);
		Array.prototype.forEach.call(table.querySelectorAll("thead th.juneau-view-actions-th"), removeEl);
		Array.prototype.forEach.call(table.querySelectorAll("thead th.juneau-view-select-th"), removeEl);
	}

	/**
	 * Restores the server-emitted header shell after destroy: re-create the leading selection {@code <th>}
	 * (ViewTable emitted it on first paint; the server will not re-run) then append the trailing actions
	 * {@code <th>} as JS does today.
	 */
	function restoreHeaderShell(table, ctx) {
		if (ctx.selectionState) {
			const headRow = table.querySelector("thead tr");
			if (headRow && !headRow.querySelector(".juneau-view-select-th")) {
				const selTh = document.createElement("th");
				selTh.className = "juneau-view-select-th";
				headRow.insertBefore(selTh, headRow.firstChild);
			}
		}
		if (ctx.viewDef.rowActions && ctx.viewDef.rowActions.length) {
			appendActionsHeaderCell(table);
		}
	}

	function teardownTable(table, ctx) {
		if (ctx._pollTimers) {
			ctx._pollTimers.forEach(function (id) { clearInterval(id); });
			ctx._pollTimers = [];
		}
		if (ctx._jobSources) {
			ctx._jobSources.forEach(function (es) {
				try { if (es && es.close) es.close(); } catch (e) { /* already closed */ }
			});
			ctx._jobSources.clear();
		}
		closeActionDialog(ctx);
		closeRowActionMenus(table);
		// TODO-445n: disconnect the scroll-region ResizeObserver before destroy (re-stamped on reconstruct).
		if (ctx._scrollRegionObserver) {
			try { ctx._scrollRegionObserver.disconnect(); } catch (e) { /* already gone */ }
			ctx._scrollRegionObserver = null;
		}
		if (ctx.dataTable) {
			try { ctx.dataTable.destroy(); } catch (e) { /* already destroyed */ }
			ctx.dataTable = null;
		}
		stripGeneratedDom(table);
		// TODO-445n: remove the DT1 wrap (INV-5) so a reconstruct cannot nest the toolbar inside the overflow box.
		unwrapTableScroll(table);
	}

	/**
	 * Catalog → effective columns.  When juneau-config.js is present, delegates to
	 * {@code computeEffectiveColumns}; otherwise the no-op seam (default catalog columns, all visible).
	 */
	function resolveEffectiveColumns(viewDef, savedView) {
		if (NS.config && typeof NS.config.computeEffectiveColumns === "function")
			return NS.config.computeEffectiveColumns(viewDef.columns || [], savedView);
		return (viewDef.columns || []).map(function (c) {
			const copy = {};
			for (const k in c) if (Object.hasOwn(c, k)) copy[k] = c[k];
			copy.visible = true;
			return copy;
		});
	}

	/**
	 * Builds the FULL {@code opts.columns} array FIRST =
	 * {@code [selection?] + effectiveColumns(including hidden, in order) + [actions?]}, THEN derives
	 * {@code opts.order} from {@link #liveDtIndex}.  Do not unshift selection after {@link #resolveOrder}.
	 */
	function assembleFullColumnArray(opts, viewDef, ctx) {
		const catalogCols = opts.columns || [];
		const cols = [];
		if (ctx.selectionState) {
			const sel = buildSelectionColumnDef(ctx.selectionState);
			sel._juneau = "selection";
			cols.push(sel);
		}
		catalogCols.forEach(function (c) { cols.push(c); });
		if (viewDef.rowActions && viewDef.rowActions.length) {
			cols.push({
				data: null,
				_juneau: "actions",
				orderable: false,
				searchable: false,
				className: "juneau-view-actions-cell",
				defaultContent: "",
				title: "",
				render: function () { return actionTriggerMarkup(); }
			});
		}
		opts.columns = cols;
		const priorCreatedRow = opts.createdRow;
		opts.createdRow = function (rowEl, rowData, index) {
			if (priorCreatedRow) priorCreatedRow(rowEl, rowData, index);
			const field = ctx.selectionState ? ctx.selectionState.rowIdField : null;
			stampRowId(rowEl, rowData, field);
		};
		opts.order = resolveOrder(viewDef, opts.columns);
	}

	/**
	 * TODO-445n table overflow discipline — DT1 "Approach B" single-node wrap.  Gives a DT1 {@code <table>} its own
	 * horizontal-scroll container ({@code .juneau-view-table-scroll}) so a table wider than its card scrolls inside
	 * its own region while the toolbar / paging-pill menu (a sibling lineage) and the page chrome stay put (INV-1 /
	 * INV-2).  No-op on the DT2 dogfood path: DataTables 2's flex {@code .dt-layout-cell} is ALREADY the scroll box
	 * (CSS-only "Approach D"), so wrapping there would nest a second scroll box inside it (INV-5 double scrollbar).
	 *
	 * <p>
	 * Skip-guard order matters (N-P5-B1): the {@code .dt-layout-cell} DT2 no-op MUST precede the already-wrapped
	 * skip, because a DT2 table's parent is the layout cell, not a {@code .juneau-view-table-scroll}.  Also skips a
	 * table already inside a scroll box so a nested {@code constructTable} (445g) does not double-wrap inner tables
	 * (N5).  Moves the existing {@code <table>} node (never clones — DataTables' bindings ride the live node) and
	 * calls {@code columns.adjust()} so header/body re-align against the now-constrained containing block (S5).
	 */
	function ensureTableScroll(table, ctx) {
		if (!table || typeof table.closest !== "function") return;
		// DT2 (Approach D): the flex .dt-layout-cell is the scroll box already — do not double-scroll.
		if (table.closest(".dt-layout-cell")) return;
		// Nested (445g) or a leftover wrap from a prior construct — already inside a scroll box.
		if (table.closest("." + TABLE_SCROLL_CLASS)) return;
		const parent = table.parentNode;
		if (!parent) return;
		const box = document.createElement("div");
		box.className = TABLE_SCROLL_CLASS;
		parent.insertBefore(box, table);
		box.appendChild(table);
		if (ctx && ctx.dataTable && ctx.dataTable.columns) {
			try { ctx.dataTable.columns.adjust(); } catch (e) { /* not yet drawable */ }
		}
	}

	/**
	 * Paired teardown for {@link #ensureTableScroll} (INV-5): removes the DT1 wrap and restores the {@code <table>}
	 * to the wrap's former parent, so a {@code destroy()} + reconstruct cannot leave the toolbar nested inside the
	 * overflow box.  No-op on the DT2 path (there is no JS-inserted wrap to remove).
	 */
	function unwrapTableScroll(table) {
		if (!table || !table.parentNode) return;
		const box = table.parentNode;
		if (!box.className || (" " + box.className + " ").indexOf(" " + TABLE_SCROLL_CLASS + " ") < 0) return;
		const grandparent = box.parentNode;
		if (!grandparent) return;
		grandparent.insertBefore(table, box);
		grandparent.removeChild(box);
	}

	/**
	 * Resolves the table-only scroll region for a11y stamping: the DT1 wrap or the DT2 flex layout cell (scoped —
	 * never a toolbar layout cell).  Returns {@code null} for a non-DOM test double or a bare table.
	 */
	function scrollRegionFor(table) {
		if (!table || typeof table.closest !== "function") return null;
		return table.closest("." + TABLE_SCROLL_CLASS)
			|| table.closest(".dt-layout-row.dt-layout-table > .dt-layout-cell")
			|| null;
	}

	/**
	 * L12 A: keyboard-reachable scroll region ONLY when it actually overflows (WCAG 2.1.1).  When
	 * {@code scrollWidth > clientWidth} the region gets {@code tabindex="0"} + a generic Juneau-vocabulary label;
	 * otherwise both are removed (an unconditional tab stop on a non-overflowing table is a false "scrollable"
	 * announcement, and a focus ring can inflate {@code documentElement.scrollWidth}).  A {@code ResizeObserver}
	 * keeps it honest; the observer is stored on {@code ctx} so {@code teardownTable} can disconnect it (S3/N-P5-S3).
	 */
	function applyScrollRegionA11y(table, ctx) {
		const region = scrollRegionFor(table);
		if (!region) return;
		const recheck = function () {
			const overflowing = (region.scrollWidth || 0) > (region.clientWidth || 0) + 1;
			if (overflowing) {
				if (!region.getAttribute("tabindex")) region.setAttribute("tabindex", "0");
				if (!region.getAttribute("aria-label")) region.setAttribute("aria-label", TABLE_SCROLL_LABEL);
			} else {
				region.removeAttribute("tabindex");
				region.removeAttribute("aria-label");
			}
		};
		recheck();
		if (ctx && ctx._scrollRegionObserver) {
			try { ctx._scrollRegionObserver.disconnect(); } catch (e) { /* already gone */ }
			ctx._scrollRegionObserver = null;
		}
		if (typeof window.ResizeObserver === "function") {
			try {
				const ro = new window.ResizeObserver(recheck);
				ro.observe(region);
				if (ctx) ctx._scrollRegionObserver = ro;
			} catch (e) { /* observer unavailable — the one-shot recheck above still ran */ }
		}
	}

	function constructTable(table, viewDef, effectiveColumns, ctx) {
		const $ = window.jQuery;
		restoreHeaderShell(table, ctx);
		clearReinitNotice(table);

		const deps = {
			parseRenderId: NS.parseRenderId,
			resolveRenderer: NS.resolveRenderer,
			warn: warn,
			effectiveColumns: effectiveColumns,
			hasRowDetail: !!findRowDetailTemplate(table),
			ribbonParams: function () {
				return NS.ribbon?.ribbonToQueryParams
					? NS.ribbon.ribbonToQueryParams(viewDef, ctx.activeState, ctx.optsColumns)
					: {};
			},
			// Present ONLY for a nested table (set by prepareNestedTable); a top-level view leaves it undefined so
			// buildOptions merges no parent-scope parameter.
			nestedScope: ctx.nestedScope
		};

		const opts = buildOptions(viewDef, deps);
		assembleFullColumnArray(opts, viewDef, ctx);
		ctx.optsColumns = opts.columns;
		ctx.effectiveColumns = effectiveColumns;

		// DataTables treats column.title as HTML.  When juneau-config.js is present, strip titles from the
		// opts array and paint header text with textContent after boot so a user label override cannot XSS.
		if (NS.config && typeof NS.config.sanitizeColumnTitlesForDataTables === "function")
			NS.config.sanitizeColumnTitlesForDataTables(opts.columns);

		ctx.dataTable = $(table).DataTable(opts);
		if (NS.config && typeof NS.config.paintHeaderTitles === "function")
			NS.config.paintHeaderTitles(table, effectiveColumns, ctx);
		if (ctx._detailInflight) {
			// Guard against a nested table's draw.dt bubbling up (see the paging/poll/prune guards): only this
			// parent table's own draw clears its in-flight detail set.
			ctx.dataTable.on("draw.dt", function (e) {
				if (e && e.target !== table) return;
				if (ctx._detailInflight) ctx._detailInflight.clear();
			});
			// Before a parent redraw destroys its child rows (sort/search/page/poll), tear down any nested tables
			// living inside still-open detail panels.  preDraw.dt fires while the child-row DOM still exists; draw.dt
			// is too late (DataTables has already discarded the child rows, so the nested DataTable instances would
			// leak their listeners/timers).  Guarded so a nested table's own preDraw never triggers this.
			ctx.dataTable.on("preDraw.dt", function (e) {
				if (e && e.target !== table) return;
				teardownNestedTables(table);
			});
		}
		ctx.redraw = function () {
			const d = ctx.dataTable;
			if (!d) return;
			if (d.ajax) d.ajax.reload(); else d.draw();
		};

		if (ctx.selectionState) {
			ensureSelectAllCheckbox(table);
			bindSelectionPrune(table, ctx);
			if (ctx._bulkDef)
				ctx.bulkToolbar = buildBulkToolbar(ctx._bulkDef, table, ctx, ctx.selectionState);
			else
				ctx.bulkToolbar = null;
		}

		const pill = buildPagingPill(viewDef, ctx);
		const bar = NS.ribbon?.build ? NS.ribbon.build(viewDef, ctx) : null;
		const columnSearchRow = buildColumnSearchRow(table, ctx.optsColumns, ctx.dataTable);
		ctx.onColumnSearchToggle = function (on) {
			if (!columnSearchRow) return;
			columnSearchRow.style.display = on ? "" : "none";
			if (!on) {
				Array.prototype.forEach.call(columnSearchRow.querySelectorAll("input"), function (inp) { inp.value = ""; });
				if (ctx.dataTable) ctx.dataTable.columns().search("").draw();
			}
		};

		const wrapper = findViewWrapper(table);
		const toolbarRow = wrapper ? buildToolbarRow(wrapper, pill, bar) : null;

		if (toolbarRow) {
			const leftCluster = toolbarRow.querySelector(".juneau-view-toolbar-left");
			if (leftCluster) {
				if (ctx.bulkToolbar) leftCluster.appendChild(ctx.bulkToolbar.el);
				else if (ctx._bulkError) renderInlineError(leftCluster, ctx._bulkError);
			}
		}

		if (viewDef.pollIntervalMs && toolbarRow) {
			const staleness = buildStalenessIndicator();
			const rightCluster = toolbarRow.querySelector(".juneau-view-toolbar-right");
			if (rightCluster) rightCluster.insertBefore(staleness, rightCluster.firstChild);
			initPolling(table, ctx.dataTable, viewDef, staleness, ctx);
		}

		// Chooser affordance: no-op when juneau-config.js is absent (v4 JS without the opt-in file).
		if (viewDef.columnConfig && NS.config && typeof NS.config.mountChooser === "function")
			NS.config.mountChooser(table, ctx, toolbarRow);

		// TODO-445n table overflow discipline: DT1 gets its own JS-inserted scroll box (no-op on DT2 — the flex
		// .dt-layout-cell already scrolls via CSS), then the scroll region gets an overflow-detected tabindex.
		ensureTableScroll(table, ctx);
		applyScrollRegionA11y(table, ctx);
	}

	/**
	 * Re-runnable DataTable constructor.  First construct skips teardown; Apply / programmatic rebuild runs the
	 * 9-step teardown first.  Single-slot latest-wins mutex: a second Apply while running replaces
	 * {@code ctx._reinitPending} and never starts a concurrent teardown.
	 */
	function buildTable(table, viewDef, effectiveColumns, ctx) {
		const $ = window.jQuery;
		if (!$?.fn?.DataTable) return { ok: false, reason: "no-datatables" };

		if (ctx._reinitRunning) {
			ctx._reinitPending = { viewDef: viewDef, effectiveColumns: effectiveColumns };
			return { ok: true, coalesced: true };
		}

		const already = !!( $?.fn?.dataTable?.isDataTable && $.fn.dataTable.isDataTable(table) );
		if (already) {
			if (hasInFlightRow(table) || hasJobRow(table)) {
				renderReinitNotice(table, "Finish the in-progress action first.");
				return { ok: false, reason: "in-flight" };
			}
			ctx._reinitRunning = true;
			teardownTable(table, ctx);
		}

		try {
			constructTable(table, viewDef, effectiveColumns, ctx);
			return { ok: true };
		} finally {
			if (already) {
				ctx._reinitRunning = false;
				if (ctx._reinitPending) {
					const pending = ctx._reinitPending;
					ctx._reinitPending = null;
					buildTable(table, pending.viewDef, pending.effectiveColumns, ctx);
				}
			}
		}
	}

	// ==================================================================================================================
	// NESTED TABLES  (read-only DataTables view inside a row-detail section, scoped to its parent row)
	// ==================================================================================================================

	/**
	 * Finds a nested table's VIEW_META sidecar within its wrapper.  Unlike a top-level view (whose sidecar carries a
	 * minted {@code id="juneau-view:<id>"} and is found via getElementById), a nested sidecar carries NO html id (a
	 * {@code <template>} clone would collide) and is instead a SIBLING of the nested {@code <table>} tagged with
	 * {@code data-juneau-nested-meta="<viewId>"}.  Matches on that attribute; falls back to the first meta node in
	 * the wrapper so a mint-time id/attr skew degrades to "wrong-but-present" rather than a silent no-init.
	 */
	function findNestedSidecar(wrap, id) {
		if (!wrap || typeof wrap.querySelectorAll !== "function") return null;
		const cands = wrap.querySelectorAll("[" + NESTED_META_ATTR + "]");
		for (let i = 0; i < cands.length; i++)
			if (cands[i].getAttribute(NESTED_META_ATTR) === id) return cands[i];
		return cands.length ? cands[0] : null;
	}

	/**
	 * Constructs ONE nested read-only DataTables view from its wrapper.  Idempotent: a wrapper already inited (its
	 * table carries {@code data-juneau-nested-init}) is skipped.  Fail-loud + fail-closed on a contract mismatch or a
	 * malformed/absent sidecar - it renders a banner (or logs) and withholds just this one table, never throwing
	 * into the caller's detail-panel flow.
	 *
	 * <p>Read-only is enforced defensively here (the server already rejects a nested {@code rowActions}/
	 * {@code columnConfig}/non-null {@code details}, and a bare {@link ViewDef} can carry neither selection nor
	 * bulk): any such field surviving on the wire is nulled before build, so the nested table can only ever run the
	 * columns/paging/sort/search feature set.  The parent-row scope is applied by seeding {@code ctx.nestedScope},
	 * which buildOptions merges into the ajax GET in both data modes.
	 */
	function prepareNestedTable(wrap, parentId) {
		if (!wrap || typeof wrap.querySelector !== "function") return;
		const table = wrap.querySelector("table[data-juneau-view]");
		if (!table || table.getAttribute(NESTED_INIT_ATTR) === "1") return;

		const nestedContract = wrap.getAttribute(NESTED_CONTRACT_ATTR);
		if (nestedContract !== JUNEAU_NESTED_CONTRACT_VERSION) {
			const m = "Juneau nested table: shell contract version mismatch (page='" + nestedContract +
				"', runtime='" + JUNEAU_NESTED_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(table, m);
			return;
		}

		const id = table.getAttribute("data-juneau-view");
		const sidecar = findNestedSidecar(wrap, id);
		if (!sidecar) { error("Juneau nested table '" + id + "': missing nested sidecar; refusing to init."); return; }

		let viewDef;
		try {
			viewDef = JSON.parse(sidecar.textContent);
		} catch (e) {
			error("Juneau nested table '" + id + "': malformed nested sidecar; refusing to init.");
			renderBanner(table, "Juneau nested table '" + id + "': malformed configuration.");
			return;
		}

		if (viewDef.contractVersion !== JUNEAU_VIEW_CONTRACT_VERSION) {
			const m = "Juneau nested table '" + id + "': view contract version mismatch (page='" +
				viewDef.contractVersion + "', runtime='" + JUNEAU_VIEW_CONTRACT_VERSION + "'). Refusing to init.";
			error(m);
			renderBanner(table, m);
			return;
		}

		const $ = window.jQuery;
		if (!$?.fn?.DataTable) {
			warn("Juneau nested table '" + id + "': jQuery/DataTables not present; cannot bind.");
			return;
		}

		// Read-only clamp (defensive - the server already forbids all of these on a nested view).
		viewDef.rowActions = null;
		viewDef.columnConfig = null;
		viewDef.pollIntervalMs = null;
		viewDef.details = null;

		table.setAttribute(NESTED_PARENT_ID_ATTR, parentId == null ? "" : String(parentId));
		const scopeParam = wrap.getAttribute(NESTED_SCOPE_PARAM_ATTR) || "parentId";

		const ctx = {
			table: table,
			viewDef: viewDef,
			dataTable: null,
			activeState: {},
			selectionState: null,
			columnSearchOn: false,
			nested: true,
			nestedScope: {
				param: scopeParam,
				// Read at REQUEST time off the live attribute so a re-init against a different parent stays correct.
				parentId: function () { return table.getAttribute(NESTED_PARENT_ID_ATTR); }
			},
			_jobSources: new Set(),
			_pollTimers: [],
			_reinitPending: null,
			_reinitRunning: false,
			redraw: function () {
				const d = ctx.dataTable;
				if (!d) return;
				if (d.ajax) d.ajax.reload(); else d.draw();
			}
		};
		table.__juneauCtx = ctx;
		table.setAttribute(NESTED_INIT_ATTR, "1");

		const effective = resolveEffectiveColumns(viewDef, null);
		buildTable(table, viewDef, effective, ctx);
	}

	/** columns.adjust() an already-inited nested table (needed after its pane transitions from hidden to visible). */
	function adjustNestedColumns(table) {
		const ctx = table && table.__juneauCtx;
		if (ctx && ctx.dataTable && ctx.dataTable.columns)
			try { ctx.dataTable.columns.adjust(); } catch (e) { /* not yet drawable */ }
	}

	/**
	 * Inits (or, if already inited, re-measures) every nested table within a single now-visible pane.  Called from
	 * the detail strip's tab-activation callback: a nested table constructed while its pane was hidden would compute
	 * zero-width columns, so a hidden pane's table waits here for its tab to be shown.
	 */
	function activateNestedTablesInPane(pane, parentId) {
		if (!pane || typeof pane.querySelectorAll !== "function") return;
		const wraps = pane.querySelectorAll("[" + NESTED_ATTR + "]");
		Array.prototype.forEach.call(wraps, function (wrap) {
			const table = wrap.querySelector("table[data-juneau-view]");
			if (table && table.getAttribute(NESTED_INIT_ATTR) === "1")
				adjustNestedColumns(table);
			else
				prepareNestedTable(wrap, parentId);
		});
	}

	/**
	 * Inits nested tables that live in a currently-VISIBLE detail pane (called once, right after the parent detail
	 * GET succeeds).  A nested table inside a hidden tab pane is deliberately left for its tab's activation.
	 */
	function initNestedTablesInVisiblePanes(panel, parentId) {
		if (!panel || typeof panel.querySelectorAll !== "function") return;
		const wraps = panel.querySelectorAll("[" + NESTED_ATTR + "]");
		Array.prototype.forEach.call(wraps, function (wrap) {
			const sec = typeof wrap.closest === "function" ? wrap.closest("[data-juneau-detail-section]") : null;
			if (sec && sec.hidden) return;   // hidden tab pane - defer to tab activation
			prepareNestedTable(wrap, parentId);
		});
	}

	/**
	 * Destroys every inited nested DataTable within {@code root} (a detail panel or a whole parent table subtree).
	 * Called before the parent's child-row DOM is discarded - on collapse, on re-expand, and (via preDraw.dt) on any
	 * parent redraw - so nested DataTable instances never leak their listeners with the detached nodes.  Clears the
	 * init marker so a subsequent re-expand rebuilds cleanly.
	 */
	function teardownNestedTables(root) {
		if (!root || typeof root.querySelectorAll !== "function") return;
		const tables = root.querySelectorAll("table[data-juneau-view][" + NESTED_INIT_ATTR + "]");
		Array.prototype.forEach.call(tables, function (t) {
			const ctx = t.__juneauCtx;
			if (ctx) {
				try { teardownTable(t, ctx); } catch (e) { /* already gone */ }
				t.__juneauCtx = null;
			}
			t.removeAttribute(NESTED_INIT_ATTR);
		});
	}

	function beginInitTable(table) {
		const $ = window.jQuery;
		const id = table.getAttribute("data-juneau-view");
		const sidecar = document.getElementById("juneau-view:" + id);
		if (!sidecar) { error("Juneau view '" + id + "': missing JSON sidecar; refusing to init."); return; }

		let viewDef;
		try {
			viewDef = JSON.parse(sidecar.textContent);
		} catch (e) {
			error("Juneau view '" + id + "': malformed JSON sidecar; refusing to init.");
			renderBanner(table, "Juneau view '" + id + "': malformed configuration.");
			return;
		}

		if (viewDef.contractVersion !== JUNEAU_VIEW_CONTRACT_VERSION) {
			const m = "Juneau view '" + id + "': contract version mismatch (page='" + viewDef.contractVersion +
				"', runtime='" + JUNEAU_VIEW_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(table, m);
			return;
		}

		if (!$?.fn?.DataTable) {
			warn("Juneau view '" + id + "': jQuery/DataTables not present; cannot bind.");
			return;
		}

		const activeState = NS.ribbon?.loadPersistedState ? NS.ribbon.loadPersistedState(viewDef) : {};
		const selectionState = hasSelection(table)
			? { selected: new Set(), rowIdField: table.getAttribute(ROW_ID_FIELD_ATTR) }
			: null;

		const ctx = {
			table: table,
			viewDef: viewDef,
			dataTable: null,
			activeState: activeState,
			selectionState: selectionState,
			columnSearchOn: false,
			_jobSources: new Set(),
			_pollTimers: [],
			_reinitPending: null,
			_reinitRunning: false,
			redraw: function () {
				const d = ctx.dataTable;
				if (!d) return;
				if (d.ajax) d.ajax.reload(); else d.draw();
			}
		};
		table.__juneauCtx = ctx;

		if (findRowDetailTemplate(table)) {
			initDetailsExpander(table, ctx, viewDef);
		}
		initCellPopover(table, ctx, viewDef);
		if (viewDef.rowActions && viewDef.rowActions.length) {
			initRowActions(table, viewDef, ctx);
		}

		if (selectionState) {
			initSelection(table, ctx);
			if (hasBulk(table)) {
				const bulkDef = readBulkDef(id);
				if (!bulkDef) {
					ctx._bulkError = "Juneau view '" + id + "': missing or malformed bulk-actions sidecar; bulk mutation withheld.";
					error(ctx._bulkError);
				} else if (bulkDef.contractVersion !== JUNEAU_BULK_CONTRACT_VERSION) {
					ctx._bulkError = "Juneau view '" + id + "': bulk-actions contract version mismatch (page='" +
						bulkDef.contractVersion + "', runtime='" + JUNEAU_BULK_CONTRACT_VERSION + "'); bulk mutation withheld.";
					error(ctx._bulkError);
				} else {
					ctx._bulkDef = bulkDef;
				}
			}
		}

		function go(saved) {
			const effective = resolveEffectiveColumns(viewDef, saved);
			buildTable(table, viewDef, effective, ctx);
		}

		if (viewDef.columnConfig && NS.config && typeof NS.config.resolveActiveView === "function")
			return NS.config.resolveActiveView(table, viewDef).then(go);
		go(null);
	}

	/**
	 * ALWAYS returns a thenable.  Overlapping calls while {@code data-juneau-init-pending} is set return the SAME
	 * in-flight promise (not a fresh resolve, not a second init).  Flag + stash are cleared on settle
	 * (success OR failure).
	 */
	function initTable(table) {
		if (table.__juneauInitPromise) return table.__juneauInitPromise;
		const $ = window.jQuery;
		if ($?.fn?.dataTable?.isDataTable(table)) return Promise.resolve();

		let proceed, fail;
		const p = new Promise(function (res, rej) { proceed = res; fail = rej; });
		table.__juneauInitPromise = p;
		table.setAttribute("data-juneau-init-pending", "1");

		function settleOk(v) {
			table.removeAttribute("data-juneau-init-pending");
			table.__juneauInitPromise = null;
			proceed(v);
		}
		function settleErr(err) {
			table.removeAttribute("data-juneau-init-pending");
			table.__juneauInitPromise = null;
			fail(err);
		}

		try {
			Promise.resolve(beginInitTable(table)).then(settleOk, settleErr);
		} catch (e) {
			settleErr(e);
		}
		return p;
	}

	/**
	 * Inits every table[data-juneau-view] on the page, EXCEPT one scoped inside a [data-juneau-page] shell: a
	 * page shell's juneau-pages.js runtime owns first-init for its own panels (lazy, on first tab activation -
	 * DataTables mis-sizes columns initialized inside a display:none panel), rather than the eager
	 * DOMContentLoaded scan below.  A standalone page with no page shell is unaffected - every one of its tables is
	 * still inited exactly as before.
	 */
	function initAll() {
		const tables = document.querySelectorAll("table[data-juneau-view]");
		Array.prototype.forEach.call(tables, function (t) {
			if (t.closest && t.closest("[data-juneau-page]")) return;
			// Skip nested tables (a read-only table inside a row-detail section).  They init lazily - after the
			// parent detail GET succeeds and their pane is visible (see prepareNestedTable) - never on this eager
			// page-load scan.  At DOMContentLoaded a nested table is still inert inside its <template> and is not
			// matched here anyway; this guard covers a nested table already cloned into an open panel.
			if (t.closest && (t.closest("[" + NESTED_ATTR + "]") || t.closest(".juneau-view-detail-panel"))) return;
			initTable(t);
		});
	}

	// ==================================================================================================================
	// PUBLIC API + bootstrap
	// ==================================================================================================================

	NS.init = {
		PAGE_SIZE_OPTIONS: PAGE_SIZE_OPTIONS,
		columnIndexOf: columnIndexOf,
		resolveOrder: resolveOrder,
		valuesEqual: valuesEqual,
		evaluateRowClassRules: evaluateRowClassRules,
		formatThousands: formatThousands,
		pagingSummaryText: pagingSummaryText,
		pillState: pillState,
		buildColumnDef: buildColumnDef,
		mergeMeta: mergeMeta,
		buildOptions: buildOptions,
		initAll: initAll,
		// Previously private - exposed so juneau-pages.js can init one specific view's table on demand (lazy,
		// on first tab activation).  Always returns a thenable; overlapping calls coalesce on the in-flight
		// promise (data-juneau-init-pending).  Already idempotent (isDataTable guard), so re-entry from the page
		// runtime after the DOMContentLoaded scan has already run is always safe.
		initTable: initTable,
		buildTable: buildTable,
		liveDtIndex: liveDtIndex,
		assembleFullColumnArray: assembleFullColumnArray,
		restoreHeaderShell: restoreHeaderShell,
		teardownTable: teardownTable,
		// TODO-445n table overflow discipline (DT1 "Approach B" wrap) - exposed for the Node harness.
		ensureTableScroll: ensureTableScroll,
		unwrapTableScroll: unwrapTableScroll,
		beginInitTable: beginInitTable,
		// visual-parity pass: exposed for manual verification.
		buildPagingPill: buildPagingPill,
		buildPageSizeMenu: buildPageSizeMenu,
		buildColumnSearchRow: buildColumnSearchRow,
		buildToolbarRow: buildToolbarRow,
		// Table polling + visible staleness indicator - exposed for manual verification.
		MIN_POLL_INTERVAL_MS: MIN_POLL_INTERVAL_MS,
		clampPollInterval: clampPollInterval,
		formatStalenessAge: formatStalenessAge,
		hasInFlightRow: hasInFlightRow,
		hasJobRow: hasJobRow,
		buildStalenessIndicator: buildStalenessIndicator,
		initPolling: initPolling,
		// Row-details expander - exposed for manual verification.
		JUNEAU_ROW_DETAIL_CONTRACT_VERSION: JUNEAU_ROW_DETAIL_CONTRACT_VERSION,
		isSafeDetailUrl: isSafeDetailUrl,
		substituteDetailUrl: substituteDetailUrl,
		scalarFieldValue: scalarFieldValue,
		isSafeMarkdownHref: isSafeMarkdownHref,
		fillMarkdownSlot: fillMarkdownSlot,
		fillRenderSlot: fillRenderSlot,
		fillDetailSlots: fillDetailSlots,
		resolveDetailHeaderIcon: resolveDetailHeaderIcon,
		paintActionMessageIntoDetail: paintActionMessageIntoDetail,
		detailTabTargetIndex: detailTabTargetIndex,
		activateDetailTab: activateDetailTab,
		buildDetailStrip: buildDetailStrip,
		findRowDetailTemplate: findRowDetailTemplate,
		detailCoalesceKey: detailCoalesceKey,
		detailContractOk: detailContractOk,
		shouldDropDetailPayload: shouldDropDetailPayload,
		setActionRefEnabled: setActionRefEnabled,
		hideActionRefs: hideActionRefs,
		initDetailsExpander: initDetailsExpander,
		// Nested read-only tables inside a row-detail section - exposed for the node harness + manual verification.
		JUNEAU_NESTED_CONTRACT_VERSION: JUNEAU_NESTED_CONTRACT_VERSION,
		applyNestedScope: applyNestedScope,
		findNestedSidecar: findNestedSidecar,
		prepareNestedTable: prepareNestedTable,
		adjustNestedColumns: adjustNestedColumns,
		activateNestedTablesInPane: activateNestedTablesInPane,
		initNestedTablesInVisiblePanes: initNestedTablesInVisiblePanes,
		teardownNestedTables: teardownNestedTables,
		fillCellPopover: fillCellPopover,
		initCellPopover: initCellPopover,
		closeCellPopover: closeCellPopover,
		appendPopoverTrigger: appendPopoverTrigger,
		// Row actions + fail-closed CSRF submit - exposed for manual verification and the fail-closed canary.
		DEFAULT_CSRF_HEADER: DEFAULT_CSRF_HEADER,
		isSafeMethod: isSafeMethod,
		isBlankToken: isBlankToken,
		buildActionRequest: buildActionRequest,
		actionRefusalMessage: actionRefusalMessage,
		resolveCsrfToken: resolveCsrfToken,
		resolveCsrfHeaderName: resolveCsrfHeaderName,
		buildRowActionMenu: buildRowActionMenu,
		submitRowAction: submitRowAction,
		initRowActions: initRowActions,
		activatePillAction: activatePillAction,
		findRowActionById: findRowActionById,
		// Declarative modal + typed action-result + in-flight lifecycle (TODO-416/417) - exposed for the canary
		// and manual verification.
		ACTION_RESULT_CONTRACT_VERSION: JUNEAU_ACTION_RESULT_CONTRACT_VERSION,
		parseActionResult: parseActionResult,
		normalizeOutcome: normalizeOutcome,
		transportRefusal: transportRefusal,
		actionOutcomeMessage: actionOutcomeMessage,
		isDialogAction: isDialogAction,
		setRowInFlight: setRowInFlight,
		settleActionResponse: settleActionResponse,
		mergeRowFromResult: mergeRowFromResult,
		renderActionOutcome: renderActionOutcome,
		openActionDialog: openActionDialog,
		buildDialogOverlay: buildDialogOverlay,
		appendDialogForm: appendDialogForm,
		collectDialogFormFields: collectDialogFormFields,
		isTypedFormInputType: isTypedFormInputType,
		showActionDialog: showActionDialog,
		submitActionDialog: submitActionDialog,
		closeActionDialog: closeActionDialog,
		closeRowActionMenus: closeRowActionMenus,
		// Complex forms + nested popups / shared layer stack (TODO-445h) - exposed for the canary and harnesses.
		JUNEAU_DIALOG_FORM_CONTRACT_VERSION: JUNEAU_DIALOG_FORM_CONTRACT_VERSION,
		MAX_DIALOG_DEPTH: MAX_DIALOG_DEPTH,
		paintFormControl: paintFormControl,
		isCheckedToken: isCheckedToken,
		validateDialogForm: validateDialogForm,
		openFormActionDialog: openFormActionDialog,
		dialogActionIsOpenable: dialogActionIsOpenable,
		renderDialogActionRefusal: renderDialogActionRefusal,
		renderDialogDepthRefusal: renderDialogDepthRefusal,
		pushLayer: pushLayer,
		popLayer: popLayer,
		topLayer: topLayer,
		dialogLayerCount: dialogLayerCount,
		// Async jobs + SSE streaming (TODO-425) - exposed for the canary and manual verification.  The job-running
		// affordance (setRowJobRunning) is DISTINCT from the synchronous in-flight marker: it never freezes polling.
		parseJobStarted: parseJobStarted,
		buildJobCancelRequest: buildJobCancelRequest,
		setRowJobRunning: setRowJobRunning,
		renderJobProgress: renderJobProgress,
		clearJobProgress: clearJobProgress,
		startJobStream: startJobStream,
		finishJobFromResult: finishJobFromResult,
		cancelJob: cancelJob,
		// Row selection + bulk mutation (TODO-428) - two independent opt-ins - exposed for the canary and manual
		// verification.  BULK_CONTRACT_VERSION is exposed above (NS.BULK_CONTRACT_VERSION) alongside the other two
		// independently-versioned contracts.
		SELECT_ATTR: SELECT_ATTR,
		ROW_ID_ATTR: ROW_ID_ATTR,
		ROW_ID_FIELD_ATTR: ROW_ID_FIELD_ATTR,
		SELECT_ALL_ATTR: SELECT_ALL_ATTR,
		BULK_ATTR: BULK_ATTR,
		BULK_SIDECAR_ID_PREFIX: BULK_SIDECAR_ID_PREFIX,
		rowIdOf: rowIdOf,
		pruneSelection: pruneSelection,
		hasSelection: hasSelection,
		hasBulk: hasBulk,
		stampRowId: stampRowId,
		selectionCellMarkup: selectionCellMarkup,
		buildSelectionColumnDef: buildSelectionColumnDef,
		initSelection: initSelection,
		bindSelectionPrune: bindSelectionPrune,
		ensureSelectAllCheckbox: ensureSelectAllCheckbox,
		readBulkDef: readBulkDef,
		buildBulkToolbar: buildBulkToolbar,
		executeBulkAction: executeBulkAction,
		renderInlineError: renderInlineError
	};

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAll);
	} else {
		initAll();
	}
})();
