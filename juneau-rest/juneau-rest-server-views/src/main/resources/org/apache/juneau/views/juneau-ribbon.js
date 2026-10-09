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
 * juneau-ribbon.js - ribbon/toolbar runtime for the Apache Juneau rich-view toolkit.
 *
 * Builds the toolbar from viewDef.ribbon: export (feature-detected copy/csv/print via DataTables Buttons, with
 * a hard `buttons` entry whose JSZip/pdfMake is missing rendered disabled and logged as E-JS-69, never dropped, and
 * excel/pdf lit up only when JSZip/pdfMake are present), refresh, pausePolling, collapseAll,
 * dialog (a row-less, ribbon-hosted dialog, opened through juneau-views.js's ribbon-catalog resolver),
 * option/optionGroup server-query toggles (with persisted state), and divider.
 *
 * ENCODING CONTRACT: this file is the ONLY encoder of ribbon state.  ribbonColumnSearches(...) turns active
 * column-scoped option/optionGroup entries into per-column BeanQuery $-expressions (keyed by the live DataTables
 * column index); ribbonQueryParams(...) turns active param-scoped entries into URL query params;
 * mergeColumnSearches(...) folds them into the user's column searches ($and on a shared column).  Server mode sends
 * the merged values as the request body's columns[i].search.value, which Java DataTablesQuery decodes; client mode
 * evaluates them through clientRowFilter(...) and juneau-search.js.  ribbon-corpus.json (views test resources) is the
 * contract both sides are tested against - change the encoding and the corpus in the same change.
 *
 * RIBBON ENTRY SCHEMA (query-contributing entries):
 *   { type: 'option', id, title, column | param, value, persist?, default?: true }
 *   { type: 'optionGroup', id, persist?, deselectable?, default?: '<memberId>',
 *     options: [ { id, title, column | param, value? } ... ] }
 * `value` is a BeanQuery $-expression for column-scoped entries (never a regex).  `default` applies only when
 * nothing is persisted for that id; a stored choice always wins.  Example:
 *   { type: 'optionGroup', id: 'phase', default: 'pending', options: [
 *     { id: 'pending', column: 'phase', value: '$in(Waiting,"Partially reviewed")' },
 *     { id: 'done',    column: 'phase', value: '$in("Ready to push","Committed, not pushed",Completed)' } ] }
 *
 * Everything in the "PURE LOGIC LAYER" is DOM/jQuery/DataTables-free (feature-detection takes its environment as an
 * argument), so it is unit-checkable (Option B) and Option-B-portable.  The "DOM/JQUERY BINDING LAYER" is the thin
 * shim that renders the toolbar and wires it to a DataTables instance.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	/** One-line `%s`-replacer for this file's own error strings (each script keeps its own private copy). */
	function fmt(msg) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return String(msg).replace(/%s/g, function () { return i < args.length ? String(args[i++]) : "%s"; });
	}

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables)
	// ==================================================================================================================

	/** Resolves a column `data` key to its zero-based index in the view. */
	function columnIndex(viewDef, columnKey) {
		const cols = viewDef.columns || [];
		for (let i = 0; i < cols.length; i++)
			if (cols[i].data === columnKey) return i;
		return -1;
	}

	/** Live {@code dtIndex} when {@code optsColumns} is the actual DataTables array; else catalog index. */
	function indexForRibbonColumn(viewDef, columnKey, optsColumns) {
		if (optsColumns) {
			if (typeof NS.config?.dtIndex === "function")
				return NS.config.dtIndex(columnKey, optsColumns);
			for (let i = 0; i < optsColumns.length; i++)
				if (optsColumns[i]?.data === columnKey) return i;
			return -1;
		}
		return columnIndex(viewDef, columnKey);
	}

	/** The single-string, comma-joined request parameter: `search` (one search string on the wire). */
	const CLAUSE_JOIN_PARAMS = { search: true };

	/**
	 * The HTTP API carries exactly ONE `search` query parameter and ONE `opt` query parameter (design §5.3: no
	 * repeated params).  When more than one active toggle contributes to the SAME single-string parameter, the browser
	 * JOINS their clauses into that one string with a top-level comma - the very clause SEPARATOR the Java
	 * {@code ClauseParser} uses to fold clauses server-side - rather than the later contribution
	 * clobbering the earlier.  Each contributed value is an already-formed `key=value` clause and is spliced RAW (its
	 * own `=` and any `$in(A,B)` parens are the $-language's, not this grammar's).  Any other parameter name is
	 * single-valued (last contribution wins), exactly as before.
	 */
	function addQueryParam(out, name, value) {
		if (CLAUSE_JOIN_PARAMS[name] && out[name] != null && out[name] !== "")
			out[name] = out[name] + "," + value;
		else
			out[name] = value;
	}

	/**
	 * Shared walk (BeanQuery DataTables design §3.1) over every ACTIVE query-contributing ribbon entry: a top-level
	 * `option` whose `activeState[id]` is truthy, and the selected member of each `optionGroup`. Calls
	 * `fn(opt, ownerId)` once per active entry, in `viewDef.ribbon`'s own declared order — `ownerId` is the
	 * option's own id for a top-level option, or the enclosing group's id for a selected group member, which is
	 * the id a per-option diagnostic would key its messages by. Other
	 * ribbon action types (`export`, `refresh`, `divider`, ...) are skipped; they contribute no query state.
	 * `ribbonColumnSearches` and `ribbonQueryParams` both walk through this one function, so the two can never
	 * disagree about which options count as "active", and any future consumer has only one place
	 * to hook in.
	 * @param {object} viewDef - the VIEW_META view definition (`viewDef.ribbon`).
	 * @param {object} activeState - map keyed by option/group id; a top-level `option` contributes iff `activeState[option.id]` is truthy, and an `optionGroup` contributes its member whose id equals `activeState[group.id]`.
	 * @param {function(object, string)} fn - called once per active entry, as `fn(opt, ownerId)`.
	 * @example
	 *   // viewDef.ribbon = [{type:'optionGroup', id:'phase', options:[{id:'done', ...}]}]
	 *   forEachActiveOption(viewDef, {phase: 'done'}, function (opt, ownerId) { ... });   // ownerId === 'phase'
	 */
	function forEachActiveOption(viewDef, activeState, fn) {
		const state = activeState || {};
		(viewDef.ribbon || []).forEach(function (a) {
			if (a.type === "option") {
				if (state[a.id]) fn(a, a.id);
			} else if (a.type === "optionGroup" && a.options) {
				const selected = state[a.id];
				a.options.forEach(function (o) { if (o.id === selected) fn(o, a.id); });
			}
		});
	}

	/**
	 * The pure counterpart of the server-side column-scoped ribbon mapping (BeanQuery DataTables design §3.1):
	 * the column-scoped search expressions the ribbon's ACTIVE toggle state contributes, keyed by live `dtIndex`
	 * (the same index the outgoing `columns[]` array in a BeanQuery DataTables POST body uses) — never by a
	 * URL-shaped param name. This is the half of the ribbon-to-request mapping that MUST land in the JSON body:
	 * a POST/JSON server-mode endpoint (BeanQuery datatables design doc §3.1/D8) does not read URL query params at
	 * all, so the old behavior of putting every ribbon contribution on the URL silently dropped column-scoped
	 * filters once that switch happened (the bug this split fixes). Pair with `mergeColumnSearches` to AND a
	 * contribution onto the user's own typed search on the same column, and never with `ribbonQueryParams`'s
	 * output, which is the other, URL-legitimate half.
	 * @param {object} viewDef - the VIEW_META view definition (`viewDef.ribbon`, `viewDef.columns`).
	 * @param {object} activeState - map keyed by option/group id; a top-level `option` contributes iff `activeState[option.id]` is truthy, and an `optionGroup` contributes its member whose id equals `activeState[group.id]`.
	 * @param {Array} [optsColumns] - the live, post-chooser `columns[]` array (gives the live `dtIndex`); omitted,
	 *   falls back to the catalog order.
	 * @returns {object} map of `{<dtIndex>: "<$-expression>"}` — empty when no column-scoped option is active. Two
	 *   active options that target the SAME column combine as `$and(<first>,<second>)`, in `viewDef.ribbon`'s own
	 *   declared order — this is a genuine AND of both filters, never one silently overwriting the other.
	 * @example
	 *   // viewDef.ribbon = [{type:'option', id:'dropped-only', column:'status', value:'$eq(DROPPED)'}]
	 *   JuneauViews.ribbon.ribbonColumnSearches(viewDef, {'dropped-only': true}, optsColumns);
	 *   // -> {2: '$eq(DROPPED)'}   (if "status" is dtIndex 2)
	 * @example
	 *   // Two active options on the same column (dtIndex 0) combine, they do not overwrite:
	 *   JuneauViews.ribbon.ribbonColumnSearches({columns:[{data:'when'}],
	 *       ribbon:[{type:'option', id:'a', column:'when', value:'$eq(DROPPED)'},
	 *               {type:'option', id:'b', column:'when', value:'$gt(2026-01-01)'}]},
	 *     {a: true, b: true}, null);
	 *   // -> {0: '$and($eq(DROPPED),$gt(2026-01-01))'}
	 */
	function ribbonColumnSearches(viewDef, activeState, optsColumns) {
		const out = {};
		forEachActiveOption(viewDef, activeState, function (opt) {
			if (opt.value == null || opt.column == null) return;
			const idx = indexForRibbonColumn(viewDef, opt.column, optsColumns);
			if (idx < 0) return;
			out[idx] = out[idx] != null ? "$and(" + out[idx] + "," + opt.value + ")" : opt.value;
		});
		return out;
	}

	/**
	 * The pure counterpart of the server-side `param:`-scoped ribbon mapping: the URL query parameters the ribbon's
	 * ACTIVE toggle state contributes — every active option/member that is NOT column-scoped (`opt.column == null`,
	 * `opt.param != null`). Column-scoped contributions are `ribbonColumnSearches`'s job, never this function's (see
	 * that function's doc for why the split exists — BeanQuery DataTables design §3.1). Keeps the existing
	 * `search`/`opt` clause-joining behavior (design doc §5.3) via the shared `addQueryParam` helper: multiple
	 * active contributions to the SAME single-string parameter name are comma-joined rather than one clobbering
	 * another; any other parameter name is single-valued (last contribution wins).
	 * @param {object} viewDef - the VIEW_META view definition (`viewDef.ribbon`).
	 * @param {object} activeState - map keyed by option/group id; a top-level `option` contributes iff `activeState[option.id]` is truthy, and an `optionGroup` contributes its member whose id equals `activeState[group.id]`.
	 * @returns {object} map of `{<param>: "<value>"}` — empty when no `param:`-scoped option is active.
	 * @example
	 *   // viewDef.ribbon = [{type:'option', id:'mine', param:'owner', value:'me'}]
	 *   JuneauViews.ribbon.ribbonQueryParams(viewDef, {mine: true});   // -> {owner: 'me'}
	 */
	function ribbonQueryParams(viewDef, activeState) {
		const out = {};
		forEachActiveOption(viewDef, activeState, function (opt) {
			if (opt.value == null || opt.column != null || opt.param == null) return;
			addQueryParam(out, opt.param, opt.value);
		});
		return out;
	}

	/**
	 * Pure helper (BeanQuery DataTables design §3.1): merges a column-scoped ribbon contribution map
	 * (`ribbonColumnSearches`'s own output shape) with the per-column search values the user has already typed,
	 * keyed the same way (by `dtIndex`). When both sides set the same column, the ribbon narrows the user's search
	 * rather than replacing it: the merged value is `$and(<user>,<ribbon>)`. When only one side sets a
	 * column, that side's value passes through unchanged. A blank or whitespace-only user value is dropped before
	 * merging, never ANDed in as a literal blank clause (`$and( ,$eq(DROPPED))`) — a column search box the user
	 * cleared, or never touched, must behave exactly like no user search at all. Pure — returns a new map, mutates
	 * neither input.
	 * @param {object} userSearch - map of `{<dtIndex>: "<user's typed $-expression>"}`.
	 * @param {object} columnSearches - map of `{<dtIndex>: "<ribbon's $-expression>"}` (`ribbonColumnSearches`'s output).
	 * @returns {object} merged map of `{<dtIndex>: "<$-expression>"}`.
	 * @example
	 *   JuneauViews.ribbon.mergeColumnSearches({0: '$prefix(Ca)'}, {0: '$eq(DROPPED)'});
	 *   // -> {0: '$and($prefix(Ca),$eq(DROPPED))'}
	 * @example
	 *   // A blank/whitespace-only user value is dropped, not ANDed in as a literal blank clause:
	 *   JuneauViews.ribbon.mergeColumnSearches({0: '   '}, {0: '$eq(DROPPED)'});
	 *   // -> {0: '$eq(DROPPED)'}
	 */
	function mergeColumnSearches(userSearch, columnSearches) {
		const out = {};
		const user = userSearch || {};
		const ribbon = columnSearches || {};
		Object.keys(user).forEach(function (k) {
			const v = user[k];
			if (v != null && String(v).trim() !== "") out[k] = v;
		});
		Object.keys(ribbon).forEach(function (k) {
			out[k] = out[k] ? "$and(" + out[k] + "," + ribbon[k] + ")" : ribbon[k];
		});
		return out;
	}

	/**
	 * Builds the client-mode row predicate for merged per-column searches (the output of mergeColumnSearches).  Each
	 * key is a DataTables column index into {@code columns}; that column's {@code data} names the row property and its
	 * {@code type} (or {@code search.type}) the search type (default "text").  Evaluation goes through ONE
	 * juneau-search.js engine, so client mode matches exactly what the $-language means everywhere else.
	 * Returns null when there is nothing to filter (every row passes).
	 *
	 * @param {Object<string,string>} merged dtIndex -> $-expression.
	 * @param {Array<{data: ?string, type?: string, search?: {type?: string}}>} columns the live column array.
	 * @param {function(): object} searchEngineFactory JuneauViews.search.createEngine.
	 * @returns {?function(object): boolean}
	 * @example
	 *   var keep = clientRowFilter({ "1": "$eq(DROPPED)" }, [{ data: "id" }, { data: "status" }], JuneauViews.search.createEngine);
	 *   keep({ id: 2, status: "DROPPED" });   // true
	 *   keep({ id: 1, status: "ACTIVE" });    // false
	 */
	function clientRowFilter(merged, columns, searchEngineFactory) {
		const engine = searchEngineFactory();
		const terms = [];
		Object.keys(merged || {}).forEach(function (k) {
			const col = (columns || [])[Number(k)];
			const key = col?.data;
			if (key == null) return;
			engine.accessor(key, function (row) { return row == null ? null : row[key]; });
			terms.push({ column: key, type: col.type || col.search?.type || "text", expression: merged[k] });
		});
		if (!terms.length) return null;
		return function (row) { return engine.rows([row]).search(terms).length === 1; };
	}

	/**
	 * Feature-detects the caller-provided export extensions in the given environment (defaults to window).  Returns
	 * `{buttons, jszip, pdfmake}` booleans.  Export degrades gracefully: with no DataTables Buttons, no export button
	 * is offered at all; excel needs JSZip; pdf needs pdfMake.
	 */
	function detectExportFeatures(win) {
		win = win || (typeof window !== "undefined" ? window : {});
		const $ = win.jQuery;
		const hasButtons = !!$?.fn?.dataTable?.Buttons;
		return { buttons: hasButtons, jszip: !!win.JSZip, pdfmake: !!win.pdfMake };
	}

	/**
	 * Button id -> `features` key of the extra dependency it needs, for ids a caller may put in the HARD `buttons` list.
	 * `copy`/`csv`/`print`/`collapse` need nothing beyond the Buttons extension itself, so they have no entry here.
	 */
	const EXPORT_BUTTON_DEPS = { excel: "jszip", pdf: "pdfmake" };

	/** Message (and E-JS-69 log text) for a hard export button whose extra library is absent. */
	const EXPORT_MISSING_MSG = "ribbon export '%s' needs '%s'; load it before juneau-views.js or list the button in optional.";

	/** Human-readable library name for a `features` key, for the disabled-button tip and the E-JS-69 log. */
	const EXPORT_LIB_DISPLAY_NAME = { jszip: "JSZip", pdfmake: "pdfMake" };

	/**
	 * Given an `export` action and detected features, returns `{ok, missing}`.  `ok` is the button ids to render and
	 * register with DataTables Buttons.  `missing` is `{id, needs}` for every id in the HARD `buttons` list whose
	 * required extra dependency is absent: those are not silently dropped, the caller renders them disabled instead.
	 * An id in the opt-in `optional` list whose dependency is absent is simply omitted (that list exists to opt out of
	 * a hard requirement).  With no Buttons extension at all both arrays are empty (graceful degrade).
	 */
	function resolveExportButtons(action, features) {
		const ok = [];
		const missing = [];
		if (!features?.buttons) return { ok: ok, missing: missing };   // degrade gracefully - no export cluster at all
		for (const b of (action.buttons || [])) {
			const needs = EXPORT_BUTTON_DEPS[b];
			if (needs && !features[needs]) missing.push({ id: b, needs: needs });
			else ok.push(b);
		}
		for (const b of (action.optional || [])) {
			// Both branches merely gate on their own extra dep being present - combined rather than duplicated.
			if ((b === "excel" && features.jszip) || (b === "pdf" && features.pdfmake)) ok.push(b);
			// else: dep absent - omitted, not "missing" (opt-in degrade)
		}
		return { ok: ok, missing: missing };
	}

	/** localStorage key for a persisted ribbon toggle (VIEW_META §6.8: juneau.view.<viewId>.ribbon.<optionId>). */
	function ribbonStorageKey(viewId, optionId) {
		return "juneau.view." + viewId + ".ribbon." + optionId;
	}

	/**
	 * Default built-in id -> icon-name lookup (visual-parity design doc §4.A).  Keyed by *button id* for the export
	 * cluster (one export action renders one button per resolved id) and by *action type* for refresh
	 * (which renders exactly one button).  `print` is a button id like `copy`/`csv`/`excel`/`pdf` -
	 * a caller opts into it via an `export` ribbon action (`buttons: ["copy", "csv", "print"]`); unlike `excel`/`pdf` it needs no extra
	 * dependency (DataTables Buttons ships a native `print` button that opens the browser print dialog), so a
	 * caller may put it in the always-on `buttons` list rather than the feature-gated `optional` one. `collapse` IS
	 * now wired, to the `collapseAll` action type below (added alongside `print` for the same toolbar
	 * follow-up) - it no longer ships purely for forward-compatibility.
	 *
	 * <p>{@code pausePolling} maps to {@code cancel} - "stop the auto-refresh" - and NOT to the neutral "tune"
	 * fallback, which would be an outright bug rather than a cosmetic compromise: "tune" resolves to the settings
	 * gear, which is the very glyph the column chooser already paints (juneau-config.js's mountChooser).  A view
	 * declaring both {@code pausePolling} and {@code columnConfig} would show two identical gears in an icon-only
	 * ribbon, one pausing the poll and one opening the column list.
	 *
	 * <p>{@code cancel} is a reused existing stem, not new artwork: juneau-symbols.svg carries no dedicated pause
	 * glyph, and that sprite is provenance-guarded (juneau-symbols-provenance.md + SymbolSprite_Provenance_Test),
	 * so adding one is its own reviewed act rather than a detail of this feature.  A consumer wanting a true pause
	 * glyph registers one and passes {@code .symbol("...")}.
	 *
	 * <p>{@code dialog} maps to {@code new} for the same two reasons, and NAMING it is load-bearing rather than
	 * cosmetic: an unlisted key does not fall through to a text label, it falls through resolveButtonIcon's
	 * {@code "tune"} default - which resolves to the settings gear the column chooser already paints, i.e. the
	 * documented outright-bug case above, not a compromise.  {@code new} is already registered
	 * (juneau-icons.js's reg("new", ...)) and already backed by the existing {@code juneau-sym-new} sprite
	 * id, so this needs zero new artwork and never touches the provenance-guarded sprite.
	 */
	const DEFAULT_ICONS = {
		copy: "content_copy", csv: "csv", excel: "table", pdf: "picture_as_pdf", print: "print",
		refresh: "refresh", collapse: "unfold_less",
		pausePolling: "cancel", dialog: "new"
	};

	/**
	 * Resolves the icon NAME (not markup) for a ribbon button - pure, DOM-free (§4.A).  An explicit `symbol` on the
	 * action/Opt always wins; otherwise a `defaultKey` (the export button id, or the action `type`) resolves from
	 * DEFAULT_ICONS; a custom option/optionGroup member with neither falls back to the neutral "tune" glyph, never
	 * blank/unset.  Markup resolution (NS.icons.resolveIcon(name)) and the "unregistered name -> draw nothing"
	 * fallback both happen in the DOM binding layer, since this pure layer has no access to the registry's
	 * runtime contents (apps can register icons after page load).
	 */
	function resolveButtonIcon(actionOrOpt, defaultKey) {
		if (actionOrOpt?.symbol != null) return actionOrOpt.symbol;
		if (defaultKey != null && Object.hasOwn(DEFAULT_ICONS, defaultKey)) return DEFAULT_ICONS[defaultKey];
		return "tune";
	}

	/**
	 * Moves every ungrouped {@code refresh} action to the end of the list, in one trailing cluster keyed by the
	 * reserved synthetic group {@code __refresh} (mirroring this file's {@code __ungrouped} convention), so a
	 * refresh control reads as a self-contained control at the far right of the toolbar regardless of where a
	 * view declared it.  Pure and DOM-free; {@link #buildRibbon} is the only caller.
	 *
	 * <ul>
	 * 	<li>No {@code refresh} action - returns {@code actions} unchanged (identity no-op).
	 * 	<li>One or more ungrouped {@code refresh} actions - all of them are removed from their declared position(s)
	 * 		and re-appended at the end, in their original relative order, sharing the {@code __refresh} group id
	 * 		(so {@link #buildRibbon}'s adjacent-group clustering renders them as ONE cluster, not several).
	 * 	<li>A {@code refresh} action that already carries an explicit {@code group} opts OUT completely - it is
	 * 		left exactly where its neighbours put it, ungrouped by this function.  This is a consumer's escape
	 * 		hatch: a deliberate {@code .group(...)} on refresh means "I clustered this on purpose".
	 * 	<li>A {@code divider} left dangling in trailing position by the move (nothing left to divide once refresh
	 * 		is gone) is dropped rather than rendered as an empty seam.
	 * </ul>
	 *
	 * <p>As with {@code __ungrouped}, a consumer literally naming a group {@code "__refresh"} collides with this
	 * reserved key; that risk is tolerated here for the same reason it already is for {@code __ungrouped}.
	 */
	function normalizeRibbon(actions) {
		actions = actions || [];
		const moving = actions.filter(function (a) { return a.type === "refresh" && a.group == null; });
		if (!moving.length) return actions;
		const kept = actions.filter(function (a) { return !(a.type === "refresh" && a.group == null); });
		while (kept.length && kept.at(-1).type === "divider") kept.pop();
		const moved = moving.map(function (a) { return { ...a, group: "__refresh" }; });
		return kept.concat(moved);
	}

	// ==================================================================================================================
	// DOM / JQUERY BINDING LAYER  (thin shim; not exercised by the pure unit tests)
	// ==================================================================================================================

	function safeStorage() {
		// A SecurityError (private-mode/disabled storage) means no storage is available - not an error to surface.
		try { return window.localStorage; } catch (e) { return null; } // NOSONAR javascript:S2486 -- storage access can throw in privacy modes; treat as unavailable
	}

	/**
	 * Reads a ribbon-toggle value.  Prefers the slice-2 persistence SPI's synchronous {@code getItem}
	 * (same exact keys) when {@code juneau-config.js} is loaded; falls back to localStorage so a
	 * non-configurable table (no config.js) keeps working and the ribbon never becomes Promise-based.
	 */
	function storageGet(key) {
		if (typeof NS.persistence?.getItem === "function")
			return NS.persistence.getItem(key);
		const store = safeStorage();
		return store ? store.getItem(key) : null;
	}

	/** Writes a ribbon-toggle value; same SPI-or-localStorage split as {@link #storageGet}. */
	function storageSet(key, value) {
		if (typeof NS.persistence?.setItem === "function") {
			NS.persistence.setItem(key, value);
			return;
		}
		const store = safeStorage();
		if (store) store.setItem(key, String(value));
	}

	/**
	 * The ribbon's initial active state.  A persisted choice always wins (including an explicit "off" and a deselected group, stored as ""); otherwise an
	 * opt-in {@code default} applies: {@code default:true} on an {@code option}, {@code default:'<memberId>'} on an
	 * {@code optionGroup}.  A stored group value that names no member counts as not stored.  Nothing auto-selects without {@code default}.  A group default that names no member is a
	 * configuration bug: it is reported with console.error and ignored.
	 *
	 * @param {object} viewDef the view definition (id, ribbon).
	 * @returns {object} option id -> boolean, group id -> member id.
	 * @example
	 *   // nothing stored yet:
	 *   loadPersistedState({ id: "review", ribbon: [{ type: "optionGroup", id: "phase", persist: true, default: "pending",
	 *     options: [{ id: "pending", column: "phase", value: "$in(Waiting,\"Partially reviewed\")" }] }] });
	 *   // -> { phase: "pending" }
	 */
	function loadPersistedState(viewDef) {
		const state = {};
		(viewDef.ribbon || []).forEach(function (a) {
			if (a.type === "option") {
				const raw = a.persist ? storageGet(ribbonStorageKey(viewDef.id, a.id)) : null;
				if (raw != null) state[a.id] = (raw === "true");
				else if (a.default === true) state[a.id] = true;
			} else if (a.type === "optionGroup") {
				let sel = a.persist ? storageGet(ribbonStorageKey(viewDef.id, a.id)) : null;
				// A stored value that names no member (an old "null", a removed member) is not a choice: it falls through to default.
				if (sel && !(a.options || []).some(function (o) { return o.id === sel; })) sel = null;
				if (sel === "") state[a.id] = null;   // an explicit deselect stays deselected; it does not fall back to default
				else if (sel != null) state[a.id] = sel;
				else if (a.default != null) {
					if ((a.options || []).some(function (o) { return o.id === a.default; })) state[a.id] = a.default;
					else console.error("Juneau ribbon optionGroup '" + a.id + "': default '" + a.default + "' is not one of its option ids; ignored.");
				}
			}
		});
		return state;
	}

	function persist(viewDef, id, value) {
		storageSet(ribbonStorageKey(viewDef.id, id), String(value));
	}

	/**
	 * The ribbon's message channel (message bus addendum, spec 5.5).  `send` delivers one cmd:<target> payload: on a
	 * console page (`ctx.bus`) it is ALWAYS published - even to the ribbon's own table, so the click is on the trace;
	 * on a plain page an own-table command runs NS.tableBus.applyCmd directly and a cross-card target is refused
	 * loudly.  `onOptions` registers a painter fed by filter:<target> (or the own table's onFilter without a bus);
	 * `listen` subscribes the painters and remembers the unsubscribes on ctx so a rebuilt ribbon disposes the old ones.
	 *
	 * @example
	 * // A ribbon on card `side` toggling the `changes` table; both tables' ribbons paint from filter:changes.
	 * {type: 'option', id: 'openOnly', title: 'Open only', target: 'changes'}
	 * // ...publishes on click:
	 * JuneauViews.bus.publish('cmd:changes', {schemaVersion: 1, op: 'set-filter', options: {openOnly: true}});
	 */
	function ribbonBus(viewDef, ctx) {
		const own = ctx.bus ? ctx.bus.cardId : viewDef.id;
		const painters = {};   // target card id -> [fn(options)]
		const seen = {};       // target card id -> the last filter:<target> options seen
		function paint(target, options) {
			seen[target] = options || {};
			(painters[target] || []).forEach(function (p) { p(seen[target]); });
		}
		return {
			target: function (a) { return a.target == null ? own : a.target; },
			optionsOf: function (target) { return seen[target] || (target === own ? ctx.activeState : {}); },
			onOptions: function (target, fn) { (painters[target] = painters[target] || []).push(fn); },
			send: function (target, op, extra) {
				const cmd = Object.assign({ schemaVersion: 1, op: op }, extra);
				if (ctx.bus) {
					ctx.bus.publish("cmd:" + target, cmd);
				} else if (target !== own) {
					console.error("Juneau view '" + viewDef.id + "': ribbon item targets card '" + target +
						"', but this table is not on a console page (no message bus); ignored.");
				} else if (NS.tableBus?.applyCmd && ctx.table) {
					NS.tableBus.applyCmd(ctx.table, ctx, cmd);
				} else {
					applyCmdLocally(viewDef, ctx, cmd);
					paint(own, ctx.activeState);
				}
			},
			listen: function () {
				Object.keys(painters).forEach(function (target) {
					const fn = function (f) { if (f) paint(target, f.options); };
					let unsub = null;
					// echo: the own table's ribbon runs on the card that publishes filter:<id>, which the bus would
					// otherwise withhold from its owner (self-echo).
					if (ctx.bus) unsub = ctx.bus.subscribe("filter:" + target, fn, { echo: true });
					else if (target === own && NS.tableBus?.onFilter) unsub = NS.tableBus.onFilter(ctx, fn);
					if (typeof unsub === "function") ctx._ribbonUnsubs.push(unsub);
				});
			}
		};
	}

	/**
	 * Pre-bus behaviour, kept ONLY for a page that loaded juneau-ribbon.js without juneau-views.js (no NS.tableBus):
	 * the ribbon applies its own command to its own ctx.  Every real view page goes through NS.tableBus.applyCmd.
	 */
	function applyCmdLocally(viewDef, ctx, cmd) {
		if (cmd.op === "reload") {
			ctx.redraw();
		} else if (cmd.op === "collapse-all") {
			if (typeof ctx.collapseAllDetailRows === "function") ctx.collapseAllDetailRows();
		} else if (cmd.op === "pause-polling" || cmd.op === "resume-polling") {
			ctx._pollPaused = cmd.op === "pause-polling";
			if (typeof ctx._onPollPausedChange === "function") ctx._onPollPausedChange();
		} else if (cmd.op === "set-filter") {
			Object.keys(cmd.options || {}).forEach(function (id) {
				const item = (viewDef.ribbon || []).find(function (a) { return a.id === id; });
				ctx.activeState[id] = cmd.options[id];
				if (item?.persist) persist(viewDef, id, item.type === "option" ? !!cmd.options[id] : cmd.options[id]);
			});
			ctx.redraw();
		}
	}

	/**
	 * Builds the ribbon toolbar element for a view and wires it to its DataTables instance.  Returns the toolbar
	 * element (or null when there is no ribbon).  `ctx` carries { table, dataTable (the DT api), activeState, redraw }.
	 *
	 * <p>Adjacent actions sharing a non-null {@code group} id (visual-parity design doc §4.A, item 2/5) are
	 * clustered into ONE segmented {@code .juneau-view-ribbon-group} wrapper (shared borders, rounded only on the
	 * outer ends - see juneau-views.css) via the local {@code place(el, groupId)} helper below.  Actions with no
	 * explicit {@code group} (excluding {@code refresh}, see below) share the synthetic {@code __ungrouped} id so
	 * consecutive icon buttons — e.g. {@code collapseAll} + copy/csv/excel/pdf — render as one connected
	 * ribbon rather than orphan glyphs.  A {@code divider} always closes any open cluster; an explicit
	 * {@code group} id still splits clusters the way the caller declared.
	 *
	 * <p>Before any of that, {@link #normalizeRibbon} moves every ungrouped {@code refresh} action into its own
	 * trailing {@code __refresh} cluster at the far right, regardless of where the view declared it - a refresh
	 * control reads as self-contained rather than welded to whatever ungrouped buttons happen to sit beside it.
	 * A {@code refresh} with an explicit {@code group} opts out of that move entirely.
	 */
	// NOSONAR javascript:S3776 -- one dispatch branch per ribbon action type (design doc §4.A); each branch is a
	// few lines and several are pinned verbatim by the wiring canary tests below `functionBody(body, "function
	// buildRibbon(")`, so splitting them into further helpers would reduce test/code locality without reducing
	// real complexity.
	/**
	 * Whether a ribbon item is shown: one with no {@code visibleWhen} always is.  A rule list is tested against the
	 * page's top-level facts alone (a ribbon item has no row).  Presentation only; a page that somehow loaded this
	 * runtime without the rules runtime shows the item rather than hiding it.
	 */
	function ribbonItemVisible(a) {
		if (a?.visibleWhen == null) return true;
		const rules = NS.rules;
		return typeof rules?.test !== "function" || rules.test(a.visibleWhen, rules.facts());
	}

	function buildRibbon(viewDef, ctx) {
		(ctx._ribbonUnsubs || []).forEach(function (u) { u(); });   // a rebuilt ribbon drops the previous one's listeners
		ctx._ribbonUnsubs = [];
		ctx.activeState = ctx.activeState || {};
		const actions = normalizeRibbon(viewDef.ribbon || []).filter(ribbonItemVisible);
		if (!actions.length) return null;
		const rb = ribbonBus(viewDef, ctx);

		const $ = window.jQuery;
		const features = detectExportFeatures(window);
		const bar = document.createElement("div");
		bar.className = "juneau-view-ribbon";
		bar.dataset.testid = "ribbon";

		let openGroup = null;   // { id, el } - the currently-open adjacent-group wrapper, or null when ungrouped
		function place(el, groupId) {
			if (groupId == null) {
				openGroup = null;
				bar.appendChild(el);
				return;
			}
			if (!openGroup || openGroup.id !== groupId) {
				const wrap = document.createElement("span");
				wrap.className = "juneau-view-ribbon-group";
				bar.appendChild(wrap);
				openGroup = { id: groupId, el: wrap };
			}
			openGroup.el.appendChild(el);
		}

		actions.forEach(function (a) { // NOSONAR javascript:S3776 -- one dispatch branch per ribbon action type; complexity is inherent
			if (a.type === "divider") {
				openGroup = null;
				const d = document.createElement("span");
				d.className = "juneau-view-ribbon-divider";
				bar.appendChild(d);
				return;
			}
			if (a.type === "export") {
				const resolved = resolveExportButtons(a, features);
				const ids = resolved.ok;
				const exportGroupId = a.group ?? "__ungrouped";
				// A button in the HARD `buttons` list whose library is absent renders DISABLED with a tip naming the
				// library; the first miss on this ctx is logged once as E-JS-69.
				if (resolved.missing.length && !ctx.__exportLibWarned) {
					ctx.__exportLibWarned = true;
					const first = resolved.missing[0];
					console.error("[juneau-ribbon] E-JS-69: " + fmt(EXPORT_MISSING_MSG, first.id, EXPORT_LIB_DISPLAY_NAME[first.needs] || first.needs));
				}
				let registered = false;
				if (ids.length && ctx.dataTable && $?.fn?.dataTable?.Buttons) {
					try {
						// Still registers/feature-gates each button with DataTables Buttons - but renders our own
						// first-party icon buttons instead of delegating to Buttons' own DOM (design doc §4.A); a
						// click programmatically triggers the SAME, already-reviewed Buttons action (design doc §7).
						new $.fn.dataTable.Buttons(ctx.dataTable, {
							buttons: ids,
							exportOptions: { columns: ":visible" }
						});
						registered = true;
					} catch (e) { /* Buttons present but init failed - degrade silently */ } // NOSONAR javascript:S2486 -- documented best-effort degrade
				}
				// Declared order is kept: a missing-library button sits where it was listed, disabled.
				const declared = (a.buttons || []).concat(a.optional || []);
				const rendered = (registered ? ids : []).concat(resolved.missing.map(function (m) { return m.id; }));
				rendered.sort(function (x, y) { return declared.indexOf(x) - declared.indexOf(y); });
				for (const id of rendered) {
					const miss = resolved.missing.find(function (m) { return m.id === id; });
					if (miss) {
						const dead = button(id, resolveButtonIcon(null, id), function () { /* disabled: nothing to run */ }, a.appearance);
						dead.disabled = true;
						stampChromeTip(dead, fmt(EXPORT_MISSING_MSG, id, EXPORT_LIB_DISPLAY_NAME[miss.needs] || miss.needs));
						place(dead, exportGroupId);
					} else {
						place(button(id, resolveButtonIcon(null, id), function () {
							ctx.dataTable.button(id).trigger();
						}, a.appearance), exportGroupId);
					}
				}
				return;
			}
			if (a.type === "refresh") {
				place(button(a.title || "Refresh", resolveButtonIcon(a, "refresh"), function () {
					rb.send(rb.target(a), "reload");
				}, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "collapseAll") {
				place(button(a.title || "Collapse all", resolveButtonIcon(a, "collapse"), function () {
					rb.send(rb.target(a), "collapse-all");
				}, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "publish") {
				// A custom-topic (or cmd:<cardId>) publish (spec 5.5).  Topics exist only on a console page.
				if (!ctx.bus) {
					console.error("Juneau view '" + viewDef.id + "': ribbon publish item '" + (a.title || a.topic) +
						"' needs a console page (no message bus); not rendered.");
					return;
				}
				place(button(a.title || a.topic, resolveButtonIcon(a, null), function () {
					ctx.bus.publish(a.topic, JSON.parse(JSON.stringify(a.payload === undefined ? {} : a.payload)));
				}, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "dialog") {
				// The ninth type, and the only mutating one: a ribbon-hosted dialog with NO row behind it
				// The dialog machinery itself lives in juneau-views.js (openActionDialog and the
				// whole already-reviewed submit/settle path), so this branch only renders the trigger and hands the
				// action id to that module's ribbon-catalog resolver - the same one-way NS.<module> hop this file
				// already makes for NS.config/NS.persistence.  A page that somehow loaded the ribbon runtime without
				// the view runtime renders an inert button rather than throwing on click.
				place(button(a.title || a.id, resolveButtonIcon(a, "dialog"), function () {
					if (typeof NS.init?.openRibbonDialog === "function") NS.init.openRibbonDialog(a.id, ctx.table, ctx);
				}, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "pausePolling") {
				// A pause control on a view that never polls is a lie - wireTablePolling only calls initPolling when
				// viewDef.pollIntervalMs is set, so there would be no timer for the button to hold.  Skip it.  A
				// cross-card pause targets ANOTHER table's polling, so only an own-table pause needs this view to poll.
				if (a.target == null && ! viewDef.pollIntervalMs) return;
				// The name stays put across the toggle, as it does for Column search above: an accessible name that
				// swapped to "Resume auto-refresh" on press would move the state INTO the name, and a screen reader
				// reading the name and aria-pressed together would then announce it twice and contradict itself.
				// Naming the mode and letting aria-pressed carry the state is the toggle-button pattern.  Note the
				// name has to be the MODE ("Pause auto-refresh", pressed = the pause is on) and not the feature
				// ("Auto-refresh"), which would read as pressed = refreshing - the exact inverse of the truth.
				const ppBtn = button(a.title || "Pause auto-refresh", resolveButtonIcon(a, "pausePolling"), function () {
					// The button tracks what IT last asked for: the target table's notifyPollPausedChange repaints that
					// table's staleness pill; this ribbon only needs its own pressed state.
					const pause = ppBtn.getAttribute("aria-pressed") !== "true";
					rb.send(rb.target(a), pause ? "pause-polling" : "resume-polling");
					ppBtn.setAttribute("aria-pressed", pause ? "true" : "false");
				}, a.appearance);
				// Read the flag back rather than assume false: ctx survives a column-config Apply, so a view paused
				// before the rebuild stays paused - and its button has to come back already pressed to say so.
				ppBtn.setAttribute("aria-pressed", ctx._pollPaused ? "true" : "false");
				place(ppBtn, a.group || "__ungrouped");
				return;
			}
			if (a.type === "option") {
				place(optionToggle(viewDef, a, ctx, rb), a.group || "__ungrouped");
				return;
			}
			if (a.type === "optionGroup") {
				openGroup = null;
				bar.appendChild(optionGroup(viewDef, a, ctx, rb));
			}
		});
		rb.listen();
		return bar;
	}

	function stampChromeTip(el, text) {
		const t = text == null ? "" : String(text);
		if (t !== "") {
			el.dataset.jcTip = t;
			el.setAttribute("aria-label", t);
		} else {
			delete el.dataset.jcTip;
		}
		if (typeof el.removeAttribute === "function") el.removeAttribute("title");
		el.title = "";
	}

	/**
	 * Builds one icon-only 32px ribbon/pill button (visual-parity design doc §4.A/§2.2).  No visible text label -
	 * `label` becomes data-jc-tip (custom cursor tooltip) and aria-label only — never a native `title`
	 * (browsers cannot style that bubble).  Resolves
	 * `iconName` via the icon registry (`NS.icons.resolveIcon`); the markup assigned to `innerHTML` is ALWAYS a
	 * SVG `<use>` host string the icon registry builds from a sanitised sprite stem - never request-supplied markup;
	 * the sprite layers themselves may be app-supplied (replacement / override URLs), but they are fetched and
	 * parsed as SVG symbols, never assigned as HTML - so
	 * this is not an HTML-injection sink (design doc §7).  An icon name found in none of the sprite layers
	 * draws nothing (U11); the label remains the accessible name and tooltip.
	 */
	function button(label, iconName, onClick, appearance) {
		const b = document.createElement("button");
		b.type = "button";
		b.className = "juneau-view-ribbon-btn";
		stampChromeTip(b, label);
		const markup = NS.icons?.resolveIcon ? NS.icons.resolveIcon(iconName) : null;
		if (markup != null) b.innerHTML = markup;   // unknown in every layer: draw nothing (label stays in aria-label / tooltip)
		if (appearance === "icon") b.classList.add("juneau-view-ribbon-btn--icon");
		b.addEventListener("click", onClick);
		return b;
	}

	function optionToggle(viewDef, action, ctx, rb) {
		const target = rb.target(action);
		const b = button(action.title || action.id, resolveButtonIcon(action, null), function () {
			rb.send(target, "set-filter", { options: { [action.id]: !rb.optionsOf(target)[action.id] } });
		}, action.appearance);
		function paint(options) { b.setAttribute("aria-pressed", options[action.id] ? "true" : "false"); }
		rb.onOptions(target, paint);
		paint(rb.optionsOf(target));
		return b;
	}

	function optionGroup(viewDef, group, ctx, rb) {
		const target = rb.target(group);
		const wrap = document.createElement("span");
		wrap.className = "juneau-view-ribbon-group";
		(group.options || []).forEach(function (o) {
			const b = button(o.title || o.id, resolveButtonIcon(o, null), function () {
				const cur = rb.optionsOf(target)[group.id];
				rb.send(target, "set-filter", { options: { [group.id]: (cur === o.id && group.deselectable) ? null : o.id } });
			}, group.appearance);
			function paint(options) { b.setAttribute("aria-pressed", options[group.id] === o.id ? "true" : "false"); }
			rb.onOptions(target, paint);
			paint(rb.optionsOf(target));
			wrap.appendChild(b);
		});
		return wrap;
	}

	// ==================================================================================================================
	// PUBLIC API
	// ==================================================================================================================

	NS.ribbon = {
		// pure
		columnIndex: columnIndex,
		indexForRibbonColumn: indexForRibbonColumn,
		ribbonColumnSearches: ribbonColumnSearches,
		ribbonQueryParams: ribbonQueryParams,
		mergeColumnSearches: mergeColumnSearches,
		clientRowFilter: clientRowFilter,
		detectExportFeatures: detectExportFeatures,
		resolveExportButtons: resolveExportButtons,
		ribbonStorageKey: ribbonStorageKey,
		resolveButtonIcon: resolveButtonIcon,
		normalizeRibbon: normalizeRibbon,
		// binding
		loadPersistedState: loadPersistedState,
		persist: persist,
		build: buildRibbon
	};
})();
