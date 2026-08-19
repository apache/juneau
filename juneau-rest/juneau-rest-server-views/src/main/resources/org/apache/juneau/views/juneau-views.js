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
	const JUNEAU_VIEW_CONTRACT_VERSION = "3";

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

	const NS = window.JuneauViews = window.JuneauViews || {};
	NS.CONTRACT_VERSION = JUNEAU_VIEW_CONTRACT_VERSION;
	NS.ACTION_RESULT_CONTRACT_VERSION = JUNEAU_ACTION_RESULT_CONTRACT_VERSION;
	NS.BULK_CONTRACT_VERSION = JUNEAU_BULK_CONTRACT_VERSION;

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
	 * Resolves `defaultOrder` [{data,dir}] to DataTables' positional `order` [[colIndex, dir]] by field name (m2) -
	 * indices are not pinned server-side, so client-side column reorder stays correct.  Unknown fields are skipped.
	 */
	function resolveOrder(viewDef) {
		const out = [];
		(viewDef.defaultOrder || []).forEach(function (e) {
			const idx = columnIndexOf(viewDef, e.data);
			if (idx >= 0) out.push([idx, e.dir]);
		});
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
	 * Projects a VIEW_META `details` field list ([{data,title}]) against one row's data into label/value pairs
	 * for the row-details expander (client-rendered from row data by default - this never issues a request of
	 * its own) - pure, DOM-free. A field whose row value is null/undefined renders as "" rather than
	 * "null"/"undefined", matching evaluateRowClassRules' null-safety above.
	 */
	function buildDetailFields(details, rowData) {
		const out = [];
		(details || []).forEach(function (d) {
			const v = rowData ? rowData[d.data] : undefined;
			out.push({ title: d.title || d.data, value: (v == null ? "" : String(v)) });
		});
		return out;
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

		if (col.render) {
			const spec = deps.parseRenderId(col.render);
			const renderer = deps.resolveRenderer(spec.id);
			if (!renderer) {
				deps.warn("Juneau view: unknown render id '" + spec.id + "' - falling back to raw value.");
			} else if (renderer.display) {
				const meta = mergeMeta(spec.meta, col);
				def.render = function (data, type, rowData) {
					if (type && type !== "display") return data;   // SERVER mode: sort/filter/type done server-side
					try { return renderer.display(data, rowData, meta); }
					catch (e) { return data == null ? "" : data; }
				};
			}
		}
		return def;
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
		opts.columns = (viewDef.columns || []).map(function (c) { return buildColumnDef(c, deps); });
		opts.order = resolveOrder(viewDef);
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

		opts.createdRow = function (rowEl, rowData) {
			evaluateRowClassRules(viewDef.rowClassRules, rowData).forEach(function (cls) {
				if (cls) rowEl.className += (rowEl.className ? " " : "") + cls;
			});
			// Marks every row as expandable when the view declares a details field list - initDetailsExpander
			// (below) delegates its click listener off this class rather than binding one handler per row.
			if (viewDef.details && viewDef.details.length) {
				rowEl.className += (rowEl.className ? " " : "") + "juneau-view-detail-row";
			}
		};

		// A declared rowActions list appends ONE synthetic, non-orderable/non-searchable trailing column whose
		// cell is the per-row action trigger.  initTable(...) appends the matching <th> to <thead> before booting
		// DataTables so column and header counts stay in step.  The action BEHAVIOR (modal/form/typed result) is a
		// later wave (TODO-416); this column only surfaces the menu trigger and routes its click to submitRowAction.
		if (viewDef.rowActions && viewDef.rowActions.length) {
			opts.columns.push({
				data: null,
				orderable: false,
				searchable: false,
				className: "juneau-view-actions-cell",
				defaultContent: "",
				title: "",
				render: function () { return actionTriggerMarkup(); }
			});
		}
		return opts;
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
		ctx.dataTable.on("draw.dt", refreshPillState);
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
	function buildColumnSearchRow(table, viewDef, dt) {
		const thead = table.querySelector("thead");
		if (!thead) return null;
		const row = document.createElement("tr");
		row.className = "juneau-view-columnsearch-row";
		row.setAttribute("data-testid", "col-search-row");
		row.style.display = "none";
		(viewDef.columns || []).forEach(function (col, idx) {
			const th = document.createElement("th");
			if (col.searchable !== false) {
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
	 * disabled).
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
	 * Builds the row-details expander's detail-body element (client-rendered from row data by default).  A
	 * plain `<dl>` of label/value pairs built with `textContent` only (never `innerHTML`) - the values are row
	 * data, not markup, so this stays safe by construction without needing an escaper (unlike a raw-markup
	 * panel content feature, which is a different, still-gated, question). There is no server-render path wired
	 * here; see the class comment atop `ViewDef.java`'s row-details-expander section for why that path is
	 * deferred rather than designed twice.
	 */
	function buildDetailPanel(fields) {
		const dl = document.createElement("dl");
		dl.className = "juneau-view-detail-panel";
		dl.setAttribute("data-testid", "detail-panel");
		fields.forEach(function (f) {
			const dtEl = document.createElement("dt");
			dtEl.textContent = f.title;
			const ddEl = document.createElement("dd");
			ddEl.textContent = f.value;
			dl.appendChild(dtEl);
			dl.appendChild(ddEl);
		});
		return dl;
	}

	/**
	 * Wires the row-details expander via DataTables' native child-row API.  ONE delegated click listener on
	 * `table` (rather than one per row) toggles a client-rendered detail panel, built from that row's OWN data
	 * (no extra network request), for whichever `.juneau-view-detail-row` `<tr>` was clicked; `createdRow` (see
	 * `buildOptions` above) is what applies that marker class.
	 *
	 * <p>"Collapse on redraw" needs no extra code here: DataTables' child-row API does not survive a `draw.dt` -
	 * a sort, page change, search, or poll tick rebuilds `<tbody>` (and any open child `<tr>` along with it),
	 * which IS the accepted behavior, not an oversight.
	 */
	function initDetailsExpander(table, dt, viewDef) {
		table.addEventListener("click", function (e) {
			const tr = e.target && e.target.closest ? e.target.closest("tr.juneau-view-detail-row") : null;
			if (!tr) return;
			const row = dt.row(tr);
			if (!row || !row.length) return;
			if (row.child.isShown()) {
				row.child.hide();
				tr.classList.remove("juneau-view-detail-open");
			} else {
				row.child(buildDetailPanel(buildDetailFields(viewDef.details, row.data()))).show();
				tr.classList.add("juneau-view-detail-open");
			}
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
	function initPolling(table, dt, viewDef, indicator) {
		const intervalMs = clampPollInterval(viewDef.pollIntervalMs);
		const state = { lastSuccessAt: Date.now(), failed: false };

		function render() {
			const age = formatStalenessAge(Date.now() - state.lastSuccessAt);
			indicator.setAttribute("data-state", state.failed ? "error" : "fresh");
			indicator.textContent = state.failed ? "Refresh failed - last updated " + age : "Updated " + age;
		}

		dt.on("draw.dt", function () { state.lastSuccessAt = Date.now(); state.failed = false; render(); });
		dt.on("error.dt", function () { state.failed = true; render(); });

		function poll() {
			if (document.hidden) return;
			if (hasInFlightRow(table)) return;
			dt.ajax.reload(null, false);
		}

		setInterval(poll, intervalMs);
		// The visible age ("5s ago" -> "6s ago" ...) must keep advancing between polls, independent of the data-
		// fetch cadence - a short, fixed, network-free tick keeps the label honest without any extra ajax cost.
		setInterval(render, 1000);
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
		if (!rowIdField) return;
		const id = rowIdOf(rowData, rowIdField);
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
	function initSelection(table, dt, selectionState, ctx) {
		function currentRowIds() {
			const ids = [];
			Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
				ids.push(tr.getAttribute(ROW_ID_ATTR));
			});
			return ids;
		}

		function refresh() {
			if (ctx && ctx.bulkToolbar) ctx.bulkToolbar.refresh(selectionState.selected.size);
		}

		table.addEventListener("change", function (e) {
			const cb = e.target && e.target.closest ? e.target.closest(".juneau-view-select-checkbox") : null;
			if (!cb) return;
			const tr = cb.closest("tr");
			const id = tr ? tr.getAttribute(ROW_ID_ATTR) : null;
			if (id == null) return;
			if (cb.checked) selectionState.selected.add(id); else selectionState.selected.delete(id);
			refresh();
		});

		if (table.getAttribute(SELECT_ALL_ATTR) === "1") {
			const th = table.querySelector(".juneau-view-select-th");
			if (th) {
				const allCb = document.createElement("input");
				allCb.type = "checkbox";
				allCb.className = "juneau-view-select-all-checkbox";
				allCb.setAttribute("aria-label", "Select all rows on this page");
				allCb.addEventListener("change", function () {
					Array.prototype.forEach.call(table.querySelectorAll("tbody tr[" + ROW_ID_ATTR + "]"), function (tr) {
						const id = tr.getAttribute(ROW_ID_ATTR);
						const cb = tr.querySelector(".juneau-view-select-checkbox");
						if (cb) cb.checked = allCb.checked;
						if (allCb.checked) selectionState.selected.add(id); else selectionState.selected.delete(id);
					});
					refresh();
				});
				th.appendChild(allCb);
			}
		}

		// The off-screen-id-drop rule (Q2/MED-11): every redraw prunes the live selection down to ids still
		// actually on screen, so a poll/sort/page tick can never leave a bulk mutate targeting a row the user can
		// no longer see.
		dt.on("draw.dt", function () {
			const pruned = pruneSelection(Array.from(selectionState.selected), currentRowIds());
			selectionState.selected = new Set(pruned);
			refresh();
		});
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
			setRowJobRunning(tr, false);
			clearJobProgress(tr);
			if (result) finishJobFromResult(action, table, tr, ctx, result);
			else renderActionOutcome(tr, fallback);
		}
		es.addEventListener("progress", function (e) {
			if (! st.settled) renderJobProgress(tr, e.data, started, table);
		});
		es.addEventListener("result", function (e) {
			finish(parseActionResult(e.data), { outcome: "unknown", message: "the job produced no readable result" });
		});
		es.addEventListener("error", function () {
			finish(null, { outcome: "unknown", message: "the progress stream was interrupted" });
		});
		if (ctx) ctx._jobSource = es;
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
		if (isBlankToken(action.form)) {
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
					showActionDialog(parseJsonSafe(text) || { title: action.confirm || action.label || action.id },
						action, table, tr, ctx);
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
	function buildDialogOverlay(modal, action) {
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

	/** Shows a dialog overlay for an action and wires its confirm (submit) / cancel (dismiss) buttons. */
	function showActionDialog(modal, action, table, tr, ctx) {
		const ui = buildDialogOverlay(modal, action);
		function close() { if (ui.backdrop.parentNode) ui.backdrop.parentNode.removeChild(ui.backdrop); }
		ui.cancelBtn.addEventListener("click", close);
		ui.confirmBtn.addEventListener("click", function () {
			close();
			submitActionDialog(modal, action, table, tr, ctx);
		});
		document.body.appendChild(ui.backdrop);
		return ui;
	}

	/**
	 * Issues the dialog's non-safe submit, carrying the server-minted idempotency key and the row's targetId so the
	 * server can check the key's `(action, targetId)` binding (HIGH-8) - a double-click / re-submit / browser retry
	 * therefore all carry the SAME key.  Delegates the fail-closed CSRF submit + in-flight marker + typed-result
	 * settling to submitRowAction(...).
	 */
	function submitActionDialog(modal, action, table, tr, ctx) {
		const extra = {};
		const targetId = (tr && tr.getAttribute) ? tr.getAttribute("data-juneau-row-id") : null;
		if (targetId != null) extra.targetId = targetId;
		if (modal && modal.idempotencyKey != null) extra.idempotencyKey = modal.idempotencyKey;
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

	/** Removes any open row-action menu under `table` (single-open invariant). */
	function closeRowActionMenus(table) {
		Array.prototype.forEach.call(table.querySelectorAll(".juneau-view-action-menu"), function (m) {
			if (m.parentNode) m.parentNode.removeChild(m);
		});
	}

	/**
	 * Wires the row-action menu via ONE delegated click listener on `table` (rather than one per row): a click on
	 * a row's `.juneau-view-action-trigger` toggles a menu of that view's rowActions for that row.  Mirrors the
	 * details-expander's delegated-listener pattern above.
	 */
	function initRowActions(table, dt, viewDef, ctx) {
		table.addEventListener("click", function (e) {
			const trigger = e.target && e.target.closest ? e.target.closest(".juneau-view-action-trigger") : null;
			if (!trigger) return;
			const tr = trigger.closest("tr");
			if (!tr) return;
			const existing = tr.querySelector(".juneau-view-action-menu");
			closeRowActionMenus(table);
			if (existing) return;   // second click on an open menu's trigger closes it
			const cell = trigger.closest(".juneau-view-actions-cell") || tr.lastElementChild;
			if (cell) cell.appendChild(buildRowActionMenu(viewDef, table, tr, ctx));
		});
	}

	// NOSONAR javascript:S3776 -- sequential wiring of one view table's DataTables instance, ribbon, paging pill,
	// column search, details expander, and polling; several of these steps and their exact call order are pinned
	// verbatim by the wiring canary tests below `functionBody(body, "function initTable(")`, so splitting them
	// into further helpers would reduce test/code locality without reducing real complexity.
	function initTable(table) {
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

		// FAIL-LOUD contract-version handshake (§6.2): a mismatch means the served JS is stale vs the JSON - refuse.
		if (viewDef.contractVersion !== JUNEAU_VIEW_CONTRACT_VERSION) {
			const m = "Juneau view '" + id + "': contract version mismatch (page='" + viewDef.contractVersion +
				"', runtime='" + JUNEAU_VIEW_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(table, m);
			return;   // refuse to init rather than silently mis-render
		}

		if (!$?.fn?.DataTable) {
			warn("Juneau view '" + id + "': jQuery/DataTables not present; cannot bind.");
			return;
		}
		if ($.fn.dataTable.isDataTable(table)) return;   // idempotent

		const activeState = NS.ribbon?.loadPersistedState ? NS.ribbon.loadPersistedState(viewDef) : {};

		const deps = {
			parseRenderId: NS.parseRenderId,
			resolveRenderer: NS.resolveRenderer,
			warn: warn,
			ribbonParams: function () {
				return NS.ribbon?.ribbonToQueryParams ? NS.ribbon.ribbonToQueryParams(viewDef, activeState) : {};
			}
		};

		const opts = buildOptions(viewDef, deps);

		// TODO-428: row selection is detected PURELY from the DOM attribute ViewTable stamped (SELECT_ATTR) -
		// never from viewDef/VIEW_META (R2 guard).  When present, a synthetic LEADING selection column is
		// unshifted (mirroring how a declared rowActions list appends a synthetic TRAILING one below) and every
		// created row is additionally stamped with its stable id.
		const selectionState = hasSelection(table)
			? { selected: new Set(), rowIdField: table.getAttribute(ROW_ID_FIELD_ATTR) }
			: null;
		if (selectionState) {
			opts.columns.unshift(buildSelectionColumnDef(selectionState));
			const priorCreatedRow = opts.createdRow;
			opts.createdRow = function (rowEl, rowData, index) {
				if (priorCreatedRow) priorCreatedRow(rowEl, rowData, index);
				stampRowId(rowEl, rowData, selectionState.rowIdField);
			};
		}

		// A declared rowActions list added a synthetic trailing column in buildOptions; append its matching <th>
		// BEFORE booting DataTables so header and column counts agree (DataTables errors on a mismatch).
		if (viewDef.rowActions && viewDef.rowActions.length) {
			appendActionsHeaderCell(table);
		}

		const dt = $(table).DataTable(opts);

		// A declared `details` field list makes every row expandable.
		if (viewDef.details && viewDef.details.length) {
			initDetailsExpander(table, dt, viewDef);
		}

		const pill = buildPagingPill(viewDef, { table: table, dataTable: dt });

		// Hoisted above the `NS.ribbon.build` call (rather than scoped inside it) because the columnSearchToggle
		// button's click handler reads `ctx.onColumnSearchToggle` at CLICK time, not at build time - as long as
		// this same object is later given that callback (below), the button already wired to it works correctly
		// regardless of which happens first.
		const ctx = {
			table: table,
			dataTable: dt,
			activeState: activeState,
			columnSearchOn: false,
			redraw: function () { dt.ajax ? dt.ajax.reload() : dt.draw(); }
		};

		// A declared rowActions list wires the per-row action menu + fail-closed CSRF submit (delegated listener).
		if (viewDef.rowActions && viewDef.rowActions.length) {
			initRowActions(table, dt, viewDef, ctx);
		}

		// TODO-428: selection wiring (checkbox toggle, select-all, off-screen-id-drop on every draw.dt) is wired
		// whenever selection was declared - completely independent of whether bulk mutation was ALSO declared.
		// The bulk toolbar itself (a SEPARATE, independent opt-in - hasBulk(...)) is only ever built INSIDE this
		// same `if (selectionState)` branch, so a selection-only table (e.g. selectable-for-export) has no code
		// path that can reach it (HIGH-5's compile/DOM-shape separability guarantee).
		if (selectionState) {
			initSelection(table, dt, selectionState, ctx);
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
					// Selection remains fully usable (e.g. for export) even when bulk mutation is withheld above -
					// only the toolbar this branch builds is gated on a healthy sidecar.
					ctx.bulkToolbar = buildBulkToolbar(bulkDef, table, ctx, selectionState);
				}
			}
		}

		const bar = NS.ribbon?.build ? NS.ribbon.build(viewDef, ctx) : null;

		const columnSearchRow = buildColumnSearchRow(table, viewDef, dt);
		ctx.onColumnSearchToggle = function (on) {
			if (!columnSearchRow) return;
			columnSearchRow.style.display = on ? "" : "none";
			if (!on) {
				Array.prototype.forEach.call(columnSearchRow.querySelectorAll("input"), function (inp) { inp.value = ""; });
				dt.columns().search("").draw();
			}
		};

		// One unified top-toolbar row (IRS reference layout) - LEFT the unified paging ribbon, RIGHT [search,
		// ribbon], all sitting ABOVE the table as a single row (buildToolbarRow(...) owns the left/right split).
		// Paging exists in exactly this one place - there is no second, right-side paging control any more.
		const wrapper = table.parentNode;
		const toolbarRow = wrapper ? buildToolbarRow(wrapper, pill, bar) : null;

		// The bulk toolbar (or its withheld-sidecar error) is inserted into the LEFT cluster, after the paging
		// pill, as a follow-up DOM step - mirroring the staleness-indicator insertion below, which is likewise a
		// post-hoc addition rather than a buildToolbarRow(...) parameter (keeping that function's own
		// signature/tests untouched).
		if (toolbarRow) {
			const leftCluster = toolbarRow.querySelector(".juneau-view-toolbar-left");
			if (leftCluster) {
				if (ctx.bulkToolbar) leftCluster.appendChild(ctx.bulkToolbar.el);
				else if (ctx._bulkError) renderInlineError(leftCluster, ctx._bulkError);
			}
		}

		// A declared pollIntervalMs gets its own per-table staleness chip, inserted at the front of the RIGHT
		// toolbar cluster (ahead of search/ribbon) without touching buildToolbarRow's own signature/tests.
		if (viewDef.pollIntervalMs && toolbarRow) {
			const staleness = buildStalenessIndicator();
			const rightCluster = toolbarRow.querySelector(".juneau-view-toolbar-right");
			if (rightCluster) rightCluster.insertBefore(staleness, rightCluster.firstChild);
			initPolling(table, dt, viewDef, staleness);
		}
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
		// on first tab activation).  Already idempotent (isDataTable guard below), so re-entry from the page
		// runtime after the DOMContentLoaded scan has already run is always safe.
		initTable: initTable,
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
		buildStalenessIndicator: buildStalenessIndicator,
		initPolling: initPolling,
		// Row-details expander - exposed for manual verification.
		buildDetailFields: buildDetailFields,
		buildDetailPanel: buildDetailPanel,
		initDetailsExpander: initDetailsExpander,
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
		showActionDialog: showActionDialog,
		submitActionDialog: submitActionDialog,
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
