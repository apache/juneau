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
	const JUNEAU_VIEW_CONTRACT_VERSION = "2";

	const NS = window.JuneauViews = window.JuneauViews || {};
	NS.CONTRACT_VERSION = JUNEAU_VIEW_CONTRACT_VERSION;

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
		initDetailsExpander: initDetailsExpander
	};

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAll);
	} else {
		initAll();
	}
})();
