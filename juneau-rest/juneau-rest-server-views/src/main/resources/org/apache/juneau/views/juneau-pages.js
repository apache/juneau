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
 * juneau-pages.js - opt-in tabs/sub-tabs page runtime for the Apache Juneau rich-view toolkit.
 *
 * Separate, opt-in asset: a page composing multiple ViewDef views into tabs/sub-tabs loads this file IN ADDITION
 * to juneau-views.js; a plain single-view page never loads it and pays nothing for hash-routing/lazy-init
 * complexity.
 *
 * On DOMContentLoaded it owns init for every [data-juneau-page] shell: it reads the shell's id, finds the matching
 * <script type="application/json" id="juneau-page:<id>"> PAGE_META sidecar, JSON.parses it, and - like
 * juneau-views.js's own VIEW_META handshake - performs a FAIL-LOUD contract-version check on PAGE_META's
 * contractVersion (a DISTINCT check from the per-view VIEW_META handshake, which juneau-views.js's initTable(...)
 * keeps running unchanged for each panel's view table - this runtime does not bypass or duplicate that one).
 *
 * Responsibilities (design doc §"Client page runtime"):
 *   1. Deep-linkable initial tab: parse location.hash (#<pageId>/<tabId>[/<subtabId>]), falling back to the first
 *      tab/subtab when absent or unknown.
 *   2. Hash-driven switching: tab/sub-tab links are plain <a href="#pageId/tabId[/subtabId]"> elements, so a click
 *      updates location.hash via the browser's native anchor behavior (back/forward + shareable, for free) - this
 *      runtime only needs a `hashchange` listener to reflect the new hash into the visible panel.
 *   3. Lazy init on first activation: a panel's table[data-juneau-view] is inited (via juneau-views.js's now-public
 *      NS.init.initTable, idempotent) the first time its panel becomes visible; on every subsequent show,
 *      DataTables' `columns.adjust()` is called instead to fix any sizing deferred while the panel was
 *      `display:none` (DataTables mis-sizes columns initialized inside a hidden container).
 *
 * SECURITY: hash-derived tabId/subtabId values are NEVER written into innerHTML/selectors built from them - they
 * are only compared (===) against existing elements' data-tab-id/data-subtab-id ATTRIBUTE VALUES (read via
 * getAttribute, not interpolated into a querySelector string), so a crafted location.hash cannot inject markup or
 * break out of a CSS selector.
 *
 * Everything in the "PURE LOGIC LAYER" is DOM-free (plain data in, plain data out) so it is unit-checkable
 * independent of a browser (mirrors the juneau-views.js/juneau-ribbon.js Option-B split); the
 * "DOM BINDING LAYER" is the thin shim that scans, parses, and wires hashchange + lazy init.
 */
(function () {
	"use strict";

	// Contract-version handshake for PAGE_META: MUST equal PageDef.CONTRACT_VERSION / ViewsMixin.CONTRACT_VERSION
	// (single source of truth on the server, itself reusing ViewDef.CONTRACT_VERSION).  Distinct from - and does
	// not replace - the per-view VIEW_META handshake juneau-views.js's initTable(...) already performs.
	const JUNEAU_PAGE_CONTRACT_VERSION = "4";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==================================================================================================================

	/**
	 * Parses a `location.hash`-shaped string into `{pageId, tabId, subtabId}`.  Accepts the hash with or without its
	 * leading "#".  Returns `null` for an empty/absent hash (caller falls back to the first tab/subtab).  Segments
	 * beyond the third are ignored; a missing trailing segment yields `null` for that field (never `""`).
	 */
	function parseHash(hash) {
		const h = String(hash || "").replace(/^#/, "");
		if (!h) return null;
		const parts = h.split("/");
		return {
			pageId: parts[0] || null,
			tabId: parts[1] || null,
			subtabId: parts[2] || null
		};
	}

	/** Finds the entry in `list` whose `.id === id`, or `null` when absent/list is empty. */
	function findById(list, id) {
		if (!list) return null;
		for (let i = 0; i < list.length; i++)
			if (list[i].id === id) return list[i];
		return null;
	}

	/**
	 * Resolves the tab/sub-tab that should be active for a given PAGE_META + `location.hash`, falling back to the
	 * first tab (and, when that tab has sub-tabs, its first sub-tab) whenever the hash is absent, targets a
	 * different page, or names an id that no longer exists (design doc §"Client page runtime" pt.1).  Returns
	 * `{tabId, subtabId}` (`subtabId` is `null` for a leaf tab) or `null` when `pageMeta` has no tabs at all.
	 */
	function resolveInitial(pageMeta, hash) {
		const tabs = (pageMeta && pageMeta.tabs) || [];
		if (!tabs.length) return null;

		const parsed = parseHash(hash);
		let tab = null;
		if (parsed && parsed.pageId === pageMeta.id && parsed.tabId)
			tab = findById(tabs, parsed.tabId);
		if (!tab) tab = tabs[0];

		const result = { tabId: tab.id, subtabId: null };
		if (tab.subtabs && tab.subtabs.length) {
			let sub = (parsed && parsed.subtabId) ? findById(tab.subtabs, parsed.subtabId) : null;
			if (!sub) sub = tab.subtabs[0];
			result.subtabId = sub.id;
		}
		return result;
	}

	/** Builds the deep-linkable hash for a (pageId, tabId, subtabId) triple; `subtabId` may be null/omitted. */
	function hashFor(pageId, tabId, subtabId) {
		let h = "#" + pageId + "/" + tabId;
		if (subtabId != null) h += "/" + subtabId;
		return h;
	}

	// ==================================================================================================================
	// DOM BINDING LAYER  (thin shim)
	// ==================================================================================================================

	function warn(msg) { if (window.console && console.warn) console.warn(msg); }
	function error(msg) { if (window.console && console.error) console.error(msg); }

	/** Renders the fail-loud, visible banner used on a PAGE_META contract-version mismatch (or a parse failure). */
	function renderBanner(root, message) {
		const b = document.createElement("div");
		b.className = "jc-page-error";
		b.textContent = message;
		root.insertBefore(b, root.firstChild);
	}

	// Every node whose visibility this runtime owns: a top-level tab panel, or a sub-tab panel nested inside one.
	const PANEL_SELECTOR = ".jc-panel, .jc-subpanel";

	/*
	 * Whether `panel` should be visible for the resolved active (tabId, subtabId).
	 *
	 * PANEL MARKUP CONTRACT (produced by the PageTable emitter, honored here - the two MUST agree):
	 *   - `data-panel-tab` scopes a panel to one top-level tab, and is present on EVERY panel.
	 *   - `data-panel-subtab` is OPTIONAL and only NARROWS a panel further, to one specific sub-tab.
	 *   - A panel that omits `data-panel-subtab` is therefore sub-tab-AGNOSTIC: it is shown whenever its tab is
	 *     active, whichever sub-tab that tab resolved to.
	 *
	 * That last rule is what makes a sub-tabbed tab render at all.  PageTable emits TWO nested levels for such a
	 * tab: an outer `.jc-panel` (tab-scoped only) wrapping the sub-tab bar plus one `.jc-subpanel` per sub-tab
	 * (tab- AND sub-tab-scoped).  Because juneau-views.css hides `.jc-panel` until it carries `.jc-active`,
	 * demanding an exact `data-panel-subtab` match here would leave that outer panel `display:none` - hiding the
	 * sub-tab bar and the active sub-panel nested inside it, i.e. rendering the entire tab blank.  The outer panel
	 * cannot fix this from the emitter side either: it must be visible for EVERY one of its sub-tabs, and a static
	 * attribute can only name one of them.
	 */
	function panelMatches(panel, tabId, subtabId) {
		if (panel.getAttribute("data-panel-tab") !== tabId) return false;
		const panelSubtabId = panel.getAttribute("data-panel-subtab");
		if (!panelSubtabId) return true;   // sub-tab-agnostic panel: tab match is sufficient
		return panelSubtabId === subtabId;
	}

	/*
	 * Lazily inits (or, if already a DataTable, columns.adjust()s) the view tables this panel OWNS.
	 *
	 * Tables sitting inside a DESCENDANT panel are skipped - a sub-tabbed tab's outer panel contains one
	 * `.jc-subpanel` per sub-tab, and those tables belong to their own sub-panel, which inits them when IT is
	 * activated.  Claiming them here would defeat lazy init (every sub-tab's ajax draw would fire on page load)
	 * and would size their columns while they are still `display:none`, the exact mis-sizing lazy init exists to
	 * avoid.
	 */
	function activatePanelViews(panel) {
		const tables = panel.querySelectorAll("table[data-juneau-view]");
		Array.prototype.forEach.call(tables, function (t) {
			if (t.closest(PANEL_SELECTOR) !== panel) return;
			const $ = window.jQuery;
			if (NS.init?.initTable) {
				Promise.resolve(NS.init.initTable(t)).then(function () {
					if ($?.fn?.dataTable?.isDataTable(t)) $(t).DataTable().columns.adjust();
				});
			} else {
				warn("Juneau page: juneau-views.js not loaded (or too old to expose initTable); cannot lazy-init view '" +
					t.getAttribute("data-juneau-view") + "'.");
			}
		});
	}

	/** Applies the resolved (tabId, subtabId) to `root`: toggles active classes, shows/hides panels, lazy-inits. */
	function showActive(root, tabId, subtabId) {
		const tabs = root.querySelectorAll(".jc-tab");
		Array.prototype.forEach.call(tabs, function (el) {
			el.classList.toggle("jc-tab-active", el.getAttribute("data-tab-id") === tabId);
		});

		const subtabs = root.querySelectorAll(".jc-subtab");
		Array.prototype.forEach.call(subtabs, function (el) {
			const active = subtabId != null &&
				el.getAttribute("data-subtab-id") === subtabId &&
				el.getAttribute("data-parent-tab") === tabId;
			el.classList.toggle("jc-subtab-active", active);
		});

		const panels = root.querySelectorAll(PANEL_SELECTOR);
		Array.prototype.forEach.call(panels, function (p) {
			const active = panelMatches(p, tabId, subtabId);
			p.classList.toggle("jc-active", active);
			if (active) activatePanelViews(p);
		});
	}

	function initPage(root) {
		const id = root.getAttribute("data-juneau-page");
		const sidecar = document.getElementById("juneau-page:" + id);
		if (!sidecar) { error("Juneau page '" + id + "': missing PAGE_META sidecar; refusing to init."); return; }

		let pageMeta;
		try {
			pageMeta = JSON.parse(sidecar.textContent);
		} catch (e) {
			error("Juneau page '" + id + "': malformed PAGE_META sidecar; refusing to init.");
			renderBanner(root, "Juneau page '" + id + "': malformed configuration.");
			return;
		}

		if (pageMeta.contractVersion !== JUNEAU_PAGE_CONTRACT_VERSION) {
			const m = "Juneau page '" + id + "': contract version mismatch (page='" + pageMeta.contractVersion +
				"', runtime='" + JUNEAU_PAGE_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(root, m);
			return;   // refuse to init rather than silently mis-render
		}

		function apply() {
			const active = resolveInitial(pageMeta, window.location.hash);
			if (active) showActive(root, active.tabId, active.subtabId);
		}

		window.addEventListener("hashchange", apply);
		apply();
	}

	function initAllPages() {
		const pages = document.querySelectorAll("[data-juneau-page]");
		Array.prototype.forEach.call(pages, function (p) { initPage(p); });
	}

	// ==================================================================================================================
	// PUBLIC API + bootstrap
	// ==================================================================================================================

	NS.pages = {
		CONTRACT_VERSION: JUNEAU_PAGE_CONTRACT_VERSION,
		parseHash: parseHash,
		findById: findById,
		resolveInitial: resolveInitial,
		hashFor: hashFor,
		initAllPages: initAllPages
	};

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initAllPages);
	} else {
		initAllPages();
	}
})();
