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
 * Builds the toolbar from viewDef.ribbon: export (feature-detected copy/csv via DataTables Buttons, with excel/pdf
 * lit up only when JSZip/pdfMake are present), refresh, columnSearchToggle, option/optionGroup server-query toggles
 * (with persisted state), and divider.
 *
 * CONSISTENCY REQUIREMENT (mirrors the server): when a column-scoped `option`/`optionGroup` toggle is ACTIVE, the
 * client contributes the SAME `columns[N][search][value]=<value>` request param that the server-side
 * RibbonAction.toQueryParams(ViewDef) produces (custom `param` options contribute `param=value` verbatim).  The
 * server maps unconditionally (it has no notion of "active"); the CLIENT owns active state, so ONLY active toggles
 * contribute here.  ribbonToQueryParams(viewDef, activeState) below is the pure counterpart of the Java mapping and
 * shares its fixtures so the two implementations cannot drift.
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
	 * refresh/columnSearchToggle/divider/export are not query-contributing and are skipped.
	 */
	function ribbonToQueryParams(viewDef, activeState, optsColumns) {
		const out = {};
		const state = activeState || {};
		(viewDef.ribbon || []).forEach(function (a) {
			if (a.type === "option") {
				if (state[a.id]) {
					const p = optionParam(viewDef, a, optsColumns);
					if (p) out[p.name] = p.value;
				}
			} else if (a.type === "optionGroup" && a.options) {
				const selected = state[a.id];
				a.options.forEach(function (o) {
					if (o.id === selected) {
						const p = optionParam(viewDef, o, optsColumns);
						if (p) out[p.name] = p.value;
					}
				});
			}
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
	 * cluster (one export action renders one button per resolved id) and by *action type* for refresh/
	 * columnSearchToggle (each renders exactly one button).  `collapse` is not wired to any RibbonAction.type today -
	 * it ships here purely for forward-compatibility with a possible future (deferred) row-expander "collapse all"
	 * affordance; inert until that action type exists.
	 */
	const DEFAULT_ICONS = {
		copy: "content_copy", csv: "csv", excel: "table", pdf: "picture_as_pdf",
		refresh: "refresh", columnSearchToggle: "manage_search", collapse: "unfold_less"
	};

	/**
	 * Resolves the icon NAME (not markup) for a ribbon button - pure, DOM-free (§4.A).  An explicit `symbol` on the
	 * action/Opt always wins; otherwise a `defaultKey` (the export button id, or the action `type`) resolves from
	 * DEFAULT_ICONS; a custom option/optionGroup member with neither falls back to the neutral "tune" glyph, never
	 * blank/unset.  Markup resolution (NS.icons.resolveIcon(name)) and the "unregistered name -> render title as
	 * text" fallback both happen in the DOM binding layer, since this pure layer has no access to the registry's
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
		while (kept.length && kept[kept.length - 1].type === "divider") kept.pop();
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
	 * element (or null when there is no ribbon).  `ctx` carries { table, dataTable (the DT api), activeState, redraw,
	 * columnSearchOn, onColumnSearchToggle }.
	 *
	 * <p>Adjacent actions sharing a non-null {@code group} id (visual-parity design doc §4.A, item 2/5) are
	 * clustered into ONE segmented {@code .juneau-view-ribbon-group} wrapper (shared borders, rounded only on the
	 * outer ends - see juneau-views.css) via the local {@code place(el, groupId)} helper below.  Actions with no
	 * explicit {@code group} (excluding {@code refresh}, see below) share the synthetic {@code __ungrouped} id so
	 * consecutive icon buttons — e.g. {@code columnSearchToggle} + copy/csv/excel/pdf — render as one connected
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
						ids.forEach(function (id) {
							place(button(id, resolveButtonIcon(null, id), function () {
								ctx.dataTable.button(id).trigger();
							}), exportGroupId);
						});
					} catch (e) { /* Buttons present but init failed - degrade silently */ }
				}
				return;
			}
			if (a.type === "refresh") {
				place(button(a.title || "Refresh", resolveButtonIcon(a, "refresh"), function () { ctx.redraw(); }), a.group || "__ungrouped");
				return;
			}
			if (a.type === "columnSearchToggle") {
				const csBtn = button(a.title || "Column search", resolveButtonIcon(a, "columnSearchToggle"), function () {
					csBtn.setAttribute("aria-pressed", toggleColumnSearch(viewDef, ctx) ? "true" : "false");
				});
				csBtn.setAttribute("aria-pressed", ctx.columnSearchOn ? "true" : "false");
				place(csBtn, a.group || "__ungrouped");
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

	/**
	 * Builds one icon-only 32px ribbon/pill button (visual-parity design doc §4.A/§2.2).  No visible text label -
	 * `label` becomes the button's native `title` (tooltip) and `aria-label` (screen-reader text) only.  Resolves
	 * `iconName` via the icon registry (`NS.icons.resolveIcon`); the markup assigned to `innerHTML` is ALWAYS a
	 * static, first-party, build-time-authored SVG string from that registry - never request-/app-supplied - so
	 * this is not an HTML-injection sink (design doc §7).  An unregistered icon name falls back to rendering the
	 * label as text (mirrors the "unknown render id -> warn once, fall back to raw value" convention already
	 * documented for juneau-renders.js).
	 */
	function button(label, iconName, onClick) {
		const b = document.createElement("button");
		b.type = "button";
		b.className = "juneau-view-ribbon-btn";
		b.title = label;
		b.setAttribute("aria-label", label);
		const markup = NS.icons?.resolveIcon ? NS.icons.resolveIcon(iconName) : null;
		if (markup != null) {
			b.innerHTML = markup;
		} else {
			b.textContent = b.title;
		}
		b.addEventListener("click", onClick);
		return b;
	}

	function optionToggle(viewDef, action, ctx) {
		const b = button(action.title || action.id, resolveButtonIcon(action, null), function () {
			ctx.activeState[action.id] = !ctx.activeState[action.id];
			b.setAttribute("aria-pressed", ctx.activeState[action.id] ? "true" : "false");
			if (action.persist) persist(viewDef, action.id, !!ctx.activeState[action.id]);
			ctx.redraw();
		});
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
			});
			wrap.appendChild(b);
		});
		return wrap;
	}

	/**
	 * Flips the shared per-column-search-visibility flag on `ctx` and notifies the DOM binding layer (juneau-
	 * views.js's initTable(...)) so it can show/hide the per-column search row it owns.  Returns the new state so
	 * the caller (the columnSearchToggle button's click handler above) can reflect it in `aria-pressed` - this
	 * function itself never touches a DOM node's attributes (kept callable/testable without a live button element).
	 */
	function toggleColumnSearch(viewDef, ctx) {
		ctx.columnSearchOn = !ctx.columnSearchOn;
		if (ctx.onColumnSearchToggle) ctx.onColumnSearchToggle(ctx.columnSearchOn);
		return ctx.columnSearchOn;
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
