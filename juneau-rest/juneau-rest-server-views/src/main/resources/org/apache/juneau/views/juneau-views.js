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
 * juneau-views.js - client initializer for the Apache Juneau rich-view toolkit (Task B.8).
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
	var JUNEAU_VIEW_CONTRACT_VERSION = "2";

	var NS = window.JuneauViews = window.JuneauViews || {};
	NS.CONTRACT_VERSION = JUNEAU_VIEW_CONTRACT_VERSION;

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables)
	// ==================================================================================================================

	/**
	 * Shared page-size vocabulary (visual-parity design doc §4.B) - a client-side default for this pass (not a new
	 * ViewDef wire field; see the non-goal in design doc §11).  Consumed by buildOptions' default pageLength below
	 * AND by the unified paging ribbon's page-size menu (buildPageSizeMenu, below).
	 */
	var PAGE_SIZE_OPTIONS = [
		{ value: 25, label: "25 rows" },
		{ value: 100, label: "100 rows" },
		{ value: -1, label: "All rows" }
	];

	/** Resolves a column `data` key to its zero-based index in the view (-1 when absent). */
	function columnIndexOf(viewDef, dataKey) {
		var cols = viewDef.columns || [];
		for (var i = 0; i < cols.length; i++)
			if (cols[i].data === dataKey) return i;
		return -1;
	}

	/**
	 * Resolves `defaultOrder` [{data,dir}] to DataTables' positional `order` [[colIndex, dir]] by field name (m2) -
	 * indices are not pinned server-side, so client-side column reorder stays correct.  Unknown fields are skipped.
	 */
	function resolveOrder(viewDef) {
		var out = [];
		(viewDef.defaultOrder || []).forEach(function (e) {
			var idx = columnIndexOf(viewDef, e.data);
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
		var out = [];
		(rules || []).forEach(function (r) {
			var v = rowData ? rowData[r.field] : undefined;
			var present = (v != null && v !== "");
			var match = false;
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

	/** Formats a non-negative integer with thousands separators (e.g. 1463 -> "1,463"), matching IRS's paging summary style. */
	function formatThousands(n) {
		var s = String(Math.trunc(Math.abs(n)));
		var out = "";
		for (var i = 0; i < s.length; i++) {
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
		var total = pageInfo.recordsDisplay != null ? pageInfo.recordsDisplay : pageInfo.recordsTotal;
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
		var page = pageInfo.page, pages = pageInfo.pages;
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
		var def = {
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
			var spec = deps.parseRenderId(col.render);
			var renderer = deps.resolveRenderer(spec.id);
			if (!renderer) {
				deps.warn("Juneau view: unknown render id '" + spec.id + "' - falling back to raw value.");
			} else if (renderer.display) {
				var meta = mergeMeta(spec.meta, col);
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
		var meta = {};
		if (renderMeta) for (var k in renderMeta) if (Object.prototype.hasOwnProperty.call(renderMeta, k)) meta[k] = renderMeta[k];
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
		var opts = {};
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
					var extra = deps.ribbonParams ? deps.ribbonParams() : {};
					for (var k in extra) if (Object.prototype.hasOwnProperty.call(extra, k)) d[k] = extra[k];
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
		};
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
		var b = document.createElement("button");
		b.type = "button";
		b.className = className;
		b.title = label;
		b.setAttribute("aria-label", label);
		var icons = window.JuneauViews && window.JuneauViews.icons;
		var markup = icons && icons.resolveIcon ? icons.resolveIcon(iconName) : null;
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
		var wrap = document.createElement("span");
		wrap.className = "juneau-view-pagingpill-menuwrap";

		var btn = document.createElement("button");
		btn.type = "button";
		btn.className = "juneau-view-pagingpill-menubtn";
		btn.title = "Rows per page";
		btn.setAttribute("aria-haspopup", "listbox");
		btn.setAttribute("aria-expanded", "false");

		var infoEl = document.createElement("span");
		infoEl.className = "juneau-view-pagingpill-info";
		btn.appendChild(infoEl);

		var icons = window.JuneauViews && window.JuneauViews.icons;
		var caretMarkup = icons && icons.resolveIcon ? icons.resolveIcon("expand_more") : null;
		var caretEl = document.createElement("span");
		caretEl.className = "juneau-view-pagingpill-caret";
		caretEl.setAttribute("aria-hidden", "true");
		if (caretMarkup != null) caretEl.innerHTML = caretMarkup;
		btn.appendChild(caretEl);

		var menuEl = document.createElement("ul");
		menuEl.className = "juneau-view-pagingpill-menu";
		menuEl.setAttribute("role", "listbox");
		menuEl.hidden = true;

		var options = NS.init.PAGE_SIZE_OPTIONS.map(function (o) {
			var optEl = document.createElement("li");
			optEl.className = "juneau-view-pagingpill-menu-option";
			optEl.setAttribute("role", "option");
			optEl.tabIndex = -1;
			optEl.textContent = o.label;
			optEl.addEventListener("click", function () { selectOption(o.value); });
			menuEl.appendChild(optEl);
			return { value: o.value, el: optEl };
		});

		function indexOfSelected() {
			for (var i = 0; i < options.length; i++) if (options[i].el.getAttribute("aria-selected") === "true") return i;
			return -1;
		}

		function indexOfFocused() {
			for (var i = 0; i < options.length; i++) if (options[i].el === document.activeElement) return i;
			return -1;
		}

		function openMenu() {
			menuEl.hidden = false;
			btn.setAttribute("aria-expanded", "true");
			var idx = indexOfSelected();
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
			var idx = indexOfFocused();
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
		var pill = document.createElement("div");
		pill.className = "juneau-view-pagingpill";
		pill.setAttribute("data-testid", "paging");

		var firstBtn = pagingPillButton("First page", "first_page", function () { ctx.dataTable.page("first").draw(); });
		var prevBtn = pagingPillButton("Previous page", "chevron_left", function () { ctx.dataTable.page("previous").draw(); });
		var sizeMenu = buildPageSizeMenu(ctx);
		var nextBtn = pagingPillButton("Next page", "chevron_right", function () { ctx.dataTable.page("next").draw(); });
		var lastBtn = pagingPillButton("Last page", "last_page", function () { ctx.dataTable.page("last").draw(); });
		pill.appendChild(firstBtn);
		pill.appendChild(prevBtn);
		pill.appendChild(sizeMenu.el);
		pill.appendChild(nextBtn);
		pill.appendChild(lastBtn);

		function refreshPillState() {
			var info = ctx.dataTable.page.info();
			var st = pillState(info, ctx.dataTable.page.len());
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
		var thead = table.querySelector("thead");
		if (!thead) return null;
		var row = document.createElement("tr");
		row.className = "juneau-view-columnsearch-row";
		row.setAttribute("data-testid", "col-search-row");
		row.style.display = "none";
		(viewDef.columns || []).forEach(function (col, idx) {
			var th = document.createElement("th");
			if (col.searchable !== false) {
				var input = document.createElement("input");
				input.type = "text";
				input.className = "juneau-view-columnsearch-input";
				var label = "Search " + (col.title || col.data || "column " + idx);
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
		var caption = table.createCaption ? table.createCaption() : null;
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
		var filterEl = wrapper.querySelector(".dataTables_filter, .dt-search");
		var row = document.createElement("div");
		row.className = "juneau-view-toolbar-row";

		var left = document.createElement("div");
		left.className = "juneau-view-toolbar-left";
		if (pill) left.appendChild(pill);

		var right = document.createElement("div");
		right.className = "juneau-view-toolbar-right";
		if (filterEl) right.appendChild(filterEl);
		if (bar) right.appendChild(bar);

		row.appendChild(left);
		row.appendChild(right);
		wrapper.insertBefore(row, wrapper.firstChild);
		return row;
	}

	function initTable(table) {
		var $ = window.jQuery;
		var id = table.getAttribute("data-juneau-view");
		var sidecar = document.getElementById("juneau-view:" + id);
		if (!sidecar) { error("Juneau view '" + id + "': missing JSON sidecar; refusing to init."); return; }

		var viewDef;
		try {
			viewDef = JSON.parse(sidecar.textContent);
		} catch (e) {
			error("Juneau view '" + id + "': malformed JSON sidecar; refusing to init.");
			renderBanner(table, "Juneau view '" + id + "': malformed configuration.");
			return;
		}

		// FAIL-LOUD contract-version handshake (§6.2): a mismatch means the served JS is stale vs the JSON - refuse.
		if (viewDef.contractVersion !== JUNEAU_VIEW_CONTRACT_VERSION) {
			var m = "Juneau view '" + id + "': contract version mismatch (page='" + viewDef.contractVersion +
				"', runtime='" + JUNEAU_VIEW_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(table, m);
			return;   // refuse to init rather than silently mis-render
		}

		if (!$ || !$.fn || !$.fn.DataTable) {
			warn("Juneau view '" + id + "': jQuery/DataTables not present; cannot bind.");
			return;
		}
		if ($.fn.dataTable.isDataTable(table)) return;   // idempotent

		var activeState = (NS.ribbon && NS.ribbon.loadPersistedState) ? NS.ribbon.loadPersistedState(viewDef) : {};

		var deps = {
			parseRenderId: NS.parseRenderId,
			resolveRenderer: NS.resolveRenderer,
			warn: warn,
			ribbonParams: function () {
				return (NS.ribbon && NS.ribbon.ribbonToQueryParams) ? NS.ribbon.ribbonToQueryParams(viewDef, activeState) : {};
			}
		};

		var opts = buildOptions(viewDef, deps);
		var dt = $(table).DataTable(opts);

		var pill = buildPagingPill(viewDef, { table: table, dataTable: dt });

		// Hoisted above the `NS.ribbon.build` call (rather than scoped inside it) because the columnSearchToggle
		// button's click handler reads `ctx.onColumnSearchToggle` at CLICK time, not at build time - as long as
		// this same object is later given that callback (below), the button already wired to it works correctly
		// regardless of which happens first.
		var ctx = {
			table: table,
			dataTable: dt,
			activeState: activeState,
			columnSearchOn: false,
			redraw: function () { dt.ajax ? dt.ajax.reload() : dt.draw(); }
		};

		var bar = (NS.ribbon && NS.ribbon.build) ? NS.ribbon.build(viewDef, ctx) : null;

		var columnSearchRow = buildColumnSearchRow(table, viewDef, dt);
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
		var wrapper = table.parentNode;
		if (wrapper) buildToolbarRow(wrapper, pill, bar);
	}

	/**
	 * Inits every table[data-juneau-view] on the page, EXCEPT one scoped inside a [data-juneau-page] shell (TODO-399
	 * Phase C seam): a page shell's juneau-pages.js runtime owns first-init for its own panels (lazy, on first tab
	 * activation - DataTables mis-sizes columns initialized inside a display:none panel), rather than the eager
	 * DOMContentLoaded scan below.  A standalone page with no page shell is unaffected - every one of its tables is
	 * still inited exactly as before.
	 */
	function initAll() {
		var tables = document.querySelectorAll("table[data-juneau-view]");
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
		// TODO-399 Phase C seam: previously private - exposed so juneau-pages.js can init one specific view's
		// table on demand (lazy, on first tab activation).  Already idempotent (isDataTable guard below), so
		// re-entry from the page runtime after the DOMContentLoaded scan has already run is always safe.
		initTable: initTable,
		// visual-parity pass: exposed for Option-A/manual verification.
		buildPagingPill: buildPagingPill,
		buildPageSizeMenu: buildPageSizeMenu,
		buildColumnSearchRow: buildColumnSearchRow,
		buildToolbarRow: buildToolbarRow
	};

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAll);
	} else {
		initAll();
	}
})();
