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
 * excel/pdf lit up only when JSZip/pdfMake are present), refresh, pausePolling, collapseAll,
 * dialog (a row-less, ribbon-hosted dialog, opened through juneau-views.js's ribbon-catalog resolver),
 * option/optionGroup server-query toggles (with persisted state), and divider.
 *
 * CONSISTENCY REQUIREMENT (mirrors the server): when a column-scoped `option`/`optionGroup` toggle is ACTIVE, the
 * client contributes the SAME `columns[N][search][value]=<value>` request param that the server-side
 * RibbonAction.toQueryParams(ViewDef) produces (custom `param` options contribute `param=value` verbatim).  The
 * server maps unconditionally (it has no notion of "active"); the CLIENT owns active state, so ONLY active toggles
 * contribute here.  ribbonToQueryParams(viewDef, activeState) below is the pure counterpart of the Java mapping and
 * shares its fixtures so the two implementations cannot drift.  A SERVER-mode table (POST/JSON wire) does not send
 * these column-scoped params on the URL at all: ribbonColumnSearches + mergeColumnSearches below put them into the
 * JSON body's columns[N].search.value, and only `param` options ride the URL (ribbonQueryParams).
 *
 * Everything in the "PURE LOGIC LAYER" is DOM/jQuery/DataTables-free (feature-detection takes its environment as an
 * argument), so it is unit-checkable (Option B) and Option-B-portable.  The "DOM/JQUERY BINDING LAYER" is the thin
 * shim that renders the toolbar and wires it to a DataTables instance.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables)
	// ==================================================================================================================

	/** Resolves a column `data` key to its zero-based index in the view (mirrors RibbonAction.columnIndex). */
	function columnIndex(viewDef, columnKey) {
		const cols = viewDef.columns || [];
		for (let i = 0; i < cols.length; i++)
			if (cols[i].data === columnKey) return i;
		return -1;
	}

	/**
	 * Maps ONE option/opt to the {name, value} request param it contributes, or null.  Column-scoped options resolve
	 * to `columns[<index>][search][value]`; custom-param options contribute `param=value` verbatim; a valueless (or
	 * column+param-less) option contributes nothing.  Byte-for-byte identical to the Java addOptionParam(...) for
	 * the no-selection/no-reorder catalog index; when `optsColumns` is supplied, the index is the live
	 * {@code dtIndex} (selection offset + client reorder).
	 */
	function optionParam(viewDef, opt, optsColumns) {
		if (opt == null || opt.value == null) return null;
		if (opt.column != null) {
			const idx = indexForRibbonColumn(viewDef, opt.column, optsColumns);
			if (idx < 0) return null;
			return { name: "columns[" + idx + "][search][value]", value: opt.value };
		}
		if (opt.param != null) return { name: opt.param, value: opt.value };
		return null;
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

	/**
	 * The pure counterpart of RibbonAction.toQueryParams(ViewDef): the request params the ribbon contributes given the
	 * client's ACTIVE toggle state.  `activeState` is a map keyed by option/group id:
	 *   - a top-level `option` contributes iff activeState[option.id] is truthy;
	 *   - an `optionGroup` contributes its member whose id === activeState[group.id] (the selected radio value).
	 * refresh/pausePolling/divider/export are not query-contributing and are skipped.
	 */
	/** The single-string, comma-joined request parameters (design §5.3): `search` and `opt`. */
	const CLAUSE_JOIN_PARAMS = { search: true, opt: true };

	function ribbonToQueryParams(viewDef, activeState, optsColumns) {
		const out = {};
		const state = activeState || {};
		(viewDef.ribbon || []).forEach(function (a) {
			if (a.type === "option") {
				if (state[a.id]) {
					const p = optionParam(viewDef, a, optsColumns);
					if (p) addQueryParam(out, p.name, p.value);
				}
			} else if (a.type === "optionGroup" && a.options) {
				const selected = state[a.id];
				a.options.forEach(function (o) {
					if (o.id === selected) {
						const p = optionParam(viewDef, o, optsColumns);
						if (p) addQueryParam(out, p.name, p.value);
					}
				});
			}
		});
		return out;
	}

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
	 * the id a later per-option diagnostic (a regex-value guard) keys its messages by. Other
	 * ribbon action types (`export`, `refresh`, `divider`, ...) are skipped; they contribute no query state.
	 * `ribbonColumnSearches` and `ribbonQueryParams` both walk through this one function, so the two can never
	 * disagree about which options count as "active", and a future consumer (like a regex-value guard) only has one place
	 * to hook in.
	 * @param {object} viewDef - the VIEW_META view definition (`viewDef.ribbon`).
	 * @param {object} activeState - map keyed by option/group id; see `ribbonToQueryParams`'s own doc for the shape.
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
	 * URL-shaped param name. This is the half of `ribbonToQueryParams`'s mapping that MUST land in the JSON body:
	 * a POST/JSON server-mode endpoint (BeanQuery datatables design doc §3.1/D8) does not read URL query params at
	 * all, so the old behavior of putting every ribbon contribution on the URL silently dropped column-scoped
	 * filters once that switch happened (the bug this split fixes). Pair with `mergeColumnSearches` to AND a
	 * contribution onto the user's own typed search on the same column, and never with `ribbonQueryParams`'s
	 * output, which is the other, URL-legitimate half.
	 * @param {object} viewDef - the VIEW_META view definition (`viewDef.ribbon`, `viewDef.columns`).
	 * @param {object} activeState - map keyed by option/group id; see `ribbonToQueryParams`'s own doc for the shape.
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
	 * @param {object} activeState - map keyed by option/group id; see `ribbonToQueryParams`'s own doc for the shape.
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
	 * Given an `export` action and detected features, returns the button ids that should actually be offered.  With no
	 * Buttons extension the result is empty (graceful degrade); optional excel/pdf are included only when their extra
	 * dep is present, otherwise omitted.
	 */
	function resolveExportButtons(action, features) {
		const out = [];
		if (!features?.buttons) return out;   // degrade gracefully - no export cluster at all
		(action.buttons || []).forEach(function (b) { out.push(b); });
		(action.optional || []).forEach(function (b) {
			// Both branches merely gate on their own extra dep being present - combined rather than duplicated.
			if ((b === "excel" && features.jszip) || (b === "pdf" && features.pdfmake)) out.push(b);
			// else: dep absent - omit/grey (feature-detected)
		});
		return out;
	}

	/** localStorage key for a persisted ribbon toggle (VIEW_META §6.8: juneau.view.<viewId>.ribbon.<optionId>). */
	function ribbonStorageKey(viewId, optionId) {
		return "juneau.view." + viewId + ".ribbon." + optionId;
	}

	/**
	 * Default built-in id -> icon-name lookup (visual-parity design doc §4.A).  Keyed by *button id* for the export
	 * cluster (one export action renders one button per resolved id) and by *action type* for refresh
	 * (which renders exactly one button).  `print` is a button id like `copy`/`csv`/`excel`/`pdf` -
	 * a caller opts into it via `RibbonAction.export("copy", "csv", "print")`; unlike `excel`/`pdf` it needs no extra
	 * dependency (DataTables Buttons ships a native `print` button that opens the browser print dialog), so a
	 * caller may put it in the always-on `buttons` list rather than the feature-gated `optional` one. `collapse` IS
	 * now wired, to the `collapseAll` action type below (added alongside `print` for the same Foundry WORK-P0063
	 * toolbar follow-up, `WORK-J0507`) - it no longer ships purely for forward-compatibility.
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
		const moved = moving.map(function (a) { return Object.assign({}, a, { group: "__refresh" }); });
		return kept.concat(moved);
	}

	// ==================================================================================================================
	// DOM / JQUERY BINDING LAYER  (thin shim; not exercised by the pure unit tests)
	// ==================================================================================================================

	function safeStorage() {
		// A SecurityError (private-mode/disabled storage) means no storage is available - not an error to surface.
		try { return window.localStorage; } catch (e) { return null; }
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

	function loadPersistedState(viewDef) {
		const state = {};
		(viewDef.ribbon || []).forEach(function (a) {
			if (a.type === "option" && a.persist) {
				const raw = storageGet(ribbonStorageKey(viewDef.id, a.id));
				if (raw != null) state[a.id] = (raw === "true");
			} else if (a.type === "optionGroup" && a.persist) {
				const sel = storageGet(ribbonStorageKey(viewDef.id, a.id));
				if (sel != null) state[a.id] = sel;
			}
		});
		return state;
	}

	function persist(viewDef, id, value) {
		storageSet(ribbonStorageKey(viewDef.id, id), String(value));
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
	// NOSONAR javascript:S3776 -- one dispatch branch per RibbonAction.type (design doc §4.A); each branch is a
	// few lines and several are pinned verbatim by the wiring canary tests below `functionBody(body, "function
	// buildRibbon(")`, so splitting them into further helpers would reduce test/code locality without reducing
	// real complexity.
	function buildRibbon(viewDef, ctx) {
		const actions = normalizeRibbon(viewDef.ribbon || []);
		if (!actions.length) return null;

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

		actions.forEach(function (a) {
			if (a.type === "divider") {
				openGroup = null;
				const d = document.createElement("span");
				d.className = "juneau-view-ribbon-divider";
				bar.appendChild(d);
				return;
			}
			if (a.type === "export") {
				const ids = resolveExportButtons(a, features);
				if (ids.length && ctx.dataTable && $?.fn?.dataTable?.Buttons) {
					try {
						// Still registers/feature-gates each button with DataTables Buttons - but renders our own
						// first-party icon buttons instead of delegating to Buttons' own DOM (design doc §4.A); a
						// click programmatically triggers the SAME, already-reviewed Buttons action (design doc §7).
						new $.fn.dataTable.Buttons(ctx.dataTable, {
							buttons: ids,
							exportOptions: { columns: ":visible" }
						});
						const exportGroupId = a.group ?? "__ungrouped";
						for (const id of ids) {
							place(button(id, resolveButtonIcon(null, id), function () {
								ctx.dataTable.button(id).trigger();
							}, a.appearance), exportGroupId);
						}
					} catch (e) { /* Buttons present but init failed - degrade silently */ } // NOSONAR javascript:S2486 -- documented best-effort degrade
				}
				return;
			}
			if (a.type === "refresh") {
				place(button(a.title || "Refresh", resolveButtonIcon(a, "refresh"), function () { ctx.redraw(); }, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "collapseAll") {
				place(button(a.title || "Collapse all", resolveButtonIcon(a, "collapse"), function () {
					if (typeof ctx.collapseAllDetailRows === "function") ctx.collapseAllDetailRows();
				}, a.appearance), a.group || "__ungrouped");
				return;
			}
			if (a.type === "dialog") {
				// The ninth type, and the only mutating one: a ribbon-hosted dialog with NO row behind it
				// (WORK-J0512).  The dialog machinery itself lives in juneau-views.js (openActionDialog and the
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
				// viewDef.pollIntervalMs is set, so there would be no timer for the button to hold.  Skip it.
				if (! viewDef.pollIntervalMs) return;
				// The name stays put across the toggle, as it does for Column search above: an accessible name that
				// swapped to "Resume auto-refresh" on press would move the state INTO the name, and a screen reader
				// reading the name and aria-pressed together would then announce it twice and contradict itself.
				// Naming the mode and letting aria-pressed carry the state is the toggle-button pattern.  Note the
				// name has to be the MODE ("Pause auto-refresh", pressed = the pause is on) and not the feature
				// ("Auto-refresh"), which would read as pressed = refreshing - the exact inverse of the truth.
				const ppBtn = button(a.title || "Pause auto-refresh", resolveButtonIcon(a, "pausePolling"), function () {
					ctx._pollPaused = ! ctx._pollPaused;
					ppBtn.setAttribute("aria-pressed", ctx._pollPaused ? "true" : "false");
					// initPolling installs this so the staleness pill flips on the click; absent only if this ribbon
					// outlived its poll wiring, in which case there is nothing to repaint.
					if (typeof ctx._onPollPausedChange === "function") ctx._onPollPausedChange();
				}, a.appearance);
				// Read the flag back rather than assume false: ctx survives a column-config Apply, so a view paused
				// before the rebuild stays paused - and its button has to come back already pressed to say so.
				ppBtn.setAttribute("aria-pressed", ctx._pollPaused ? "true" : "false");
				place(ppBtn, a.group || "__ungrouped");
				return;
			}
			if (a.type === "option") {
				place(optionToggle(viewDef, a, ctx), a.group || "__ungrouped");
				return;
			}
			if (a.type === "optionGroup") {
				openGroup = null;
				bar.appendChild(optionGroup(viewDef, a, ctx));
			}
		});
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

	function optionToggle(viewDef, action, ctx) {
		const b = button(action.title || action.id, resolveButtonIcon(action, null), function () {
			ctx.activeState[action.id] = !ctx.activeState[action.id];
			b.setAttribute("aria-pressed", ctx.activeState[action.id] ? "true" : "false");
			if (action.persist) persist(viewDef, action.id, !!ctx.activeState[action.id]);
			ctx.redraw();
		}, action.appearance);
		b.setAttribute("aria-pressed", ctx.activeState[action.id] ? "true" : "false");
		return b;
	}

	function optionGroup(viewDef, group, ctx) {
		const wrap = document.createElement("span");
		wrap.className = "juneau-view-ribbon-group";
		(group.options || []).forEach(function (o) {
			const b = button(o.title || o.id, resolveButtonIcon(o, null), function () {
				ctx.activeState[group.id] = (ctx.activeState[group.id] === o.id && group.deselectable) ? null : o.id;
				if (group.persist) persist(viewDef, group.id, ctx.activeState[group.id]);
				ctx.redraw();
			}, group.appearance);
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
		optionParam: optionParam,
		indexForRibbonColumn: indexForRibbonColumn,
		ribbonToQueryParams: ribbonToQueryParams,
		ribbonColumnSearches: ribbonColumnSearches,
		ribbonQueryParams: ribbonQueryParams,
		mergeColumnSearches: mergeColumnSearches,
		detectExportFeatures: detectExportFeatures,
		resolveExportButtons: resolveExportButtons,
		ribbonStorageKey: ribbonStorageKey,
		resolveButtonIcon: resolveButtonIcon,
		normalizeRibbon: normalizeRibbon,
		// binding
		loadPersistedState: loadPersistedState,
		build: buildRibbon
	};
})();
