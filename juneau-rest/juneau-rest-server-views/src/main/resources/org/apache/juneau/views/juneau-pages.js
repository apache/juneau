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
 * juneau-pages.js - opt-in tabs/sub-tabs page runtime for the Apache Juneau rich-view toolkit (TODO-399 Phase C).
 *
 * Separate, opt-in asset (Decision 2, RESOLVED (A)): a page composing multiple ViewDef views into tabs/sub-tabs
 * loads this file IN ADDITION to juneau-views.js; a plain single-view page never loads it and pays nothing for
 * hash-routing/lazy-init complexity.
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
	var JUNEAU_PAGE_CONTRACT_VERSION = "2";

	var NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==================================================================================================================

	/**
	 * Parses a `location.hash`-shaped string into `{pageId, tabId, subtabId}`.  Accepts the hash with or without its
	 * leading "#".  Returns `null` for an empty/absent hash (caller falls back to the first tab/subtab).  Segments
	 * beyond the third are ignored; a missing trailing segment yields `null` for that field (never `""`).
	 */
	function parseHash(hash) {
		var h = String(hash || "").replace(/^#/, "");
		if (!h) return null;
		var parts = h.split("/");
		return {
			pageId: parts[0] || null,
			tabId: parts[1] || null,
			subtabId: parts[2] || null
		};
	}

	/** Finds the entry in `list` whose `.id === id`, or `null` when absent/list is empty. */
	function findById(list, id) {
		if (!list) return null;
		for (var i = 0; i < list.length; i++)
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
		var tabs = (pageMeta && pageMeta.tabs) || [];
		if (!tabs.length) return null;

		var parsed = parseHash(hash);
		var tab = null;
		if (parsed && parsed.pageId === pageMeta.id && parsed.tabId)
			tab = findById(tabs, parsed.tabId);
		if (!tab) tab = tabs[0];

		var result = { tabId: tab.id, subtabId: null };
		if (tab.subtabs && tab.subtabs.length) {
			var sub = (parsed && parsed.subtabId) ? findById(tab.subtabs, parsed.subtabId) : null;
			if (!sub) sub = tab.subtabs[0];
			result.subtabId = sub.id;
		}
		return result;
	}

	/** Builds the deep-linkable hash for a (pageId, tabId, subtabId) triple; `subtabId` may be null/omitted. */
	function hashFor(pageId, tabId, subtabId) {
		var h = "#" + pageId + "/" + tabId;
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
		var b = document.createElement("div");
		b.className = "jc-page-error";
		b.textContent = message;
		root.insertBefore(b, root.firstChild);
	}

	/** Whether `panel`'s data-panel-tab/data-panel-subtab attributes match the resolved active (tabId, subtabId). */
	function panelMatches(panel, tabId, subtabId) {
		if (panel.getAttribute("data-panel-tab") !== tabId) return false;
		var panelSubtabId = panel.getAttribute("data-panel-subtab");
		return subtabId != null ? panelSubtabId === subtabId : !panelSubtabId;
	}

	/** Lazily inits (or, if already a DataTable, columns.adjust()s) every view table inside a just-shown panel. */
	function activatePanelViews(panel) {
		var tables = panel.querySelectorAll("table[data-juneau-view]");
		Array.prototype.forEach.call(tables, function (t) {
			var $ = window.jQuery;
			if ($ && $.fn && $.fn.dataTable && $.fn.dataTable.isDataTable(t)) {
				$(t).DataTable().columns.adjust();
			} else if (NS.init && NS.init.initTable) {
				NS.init.initTable(t);
			} else {
				warn("Juneau page: juneau-views.js not loaded (or too old to expose initTable); cannot lazy-init view '" +
					t.getAttribute("data-juneau-view") + "'.");
			}
		});
	}

	/** Applies the resolved (tabId, subtabId) to `root`: toggles active classes, shows/hides panels, lazy-inits. */
	function showActive(root, tabId, subtabId) {
		var tabs = root.querySelectorAll(".jc-tab");
		Array.prototype.forEach.call(tabs, function (el) {
			el.classList.toggle("jc-tab-active", el.getAttribute("data-tab-id") === tabId);
		});

		var subtabs = root.querySelectorAll(".jc-subtab");
		Array.prototype.forEach.call(subtabs, function (el) {
			var active = subtabId != null &&
				el.getAttribute("data-subtab-id") === subtabId &&
				el.getAttribute("data-parent-tab") === tabId;
			el.classList.toggle("jc-subtab-active", active);
		});

		var panels = root.querySelectorAll(".jc-panel, .jc-subpanel");
		Array.prototype.forEach.call(panels, function (p) {
			var active = panelMatches(p, tabId, subtabId);
			p.classList.toggle("jc-active", active);
			if (active) activatePanelViews(p);
		});
	}

	function initPage(root) {
		var id = root.getAttribute("data-juneau-page");
		var sidecar = document.getElementById("juneau-page:" + id);
		if (!sidecar) { error("Juneau page '" + id + "': missing PAGE_META sidecar; refusing to init."); return; }

		var pageMeta;
		try {
			pageMeta = JSON.parse(sidecar.textContent);
		} catch (e) {
			error("Juneau page '" + id + "': malformed PAGE_META sidecar; refusing to init.");
			renderBanner(root, "Juneau page '" + id + "': malformed configuration.");
			return;
		}

		if (pageMeta.contractVersion !== JUNEAU_PAGE_CONTRACT_VERSION) {
			var m = "Juneau page '" + id + "': contract version mismatch (page='" + pageMeta.contractVersion +
				"', runtime='" + JUNEAU_PAGE_CONTRACT_VERSION + "'). Refusing to init - reload to clear a stale cached script.";
			error(m);
			renderBanner(root, m);
			return;   // refuse to init rather than silently mis-render
		}

		function apply() {
			var active = resolveInitial(pageMeta, window.location.hash);
			if (active) showActive(root, active.tabId, active.subtabId);
		}

		window.addEventListener("hashchange", apply);
		apply();
	}

	function initAllPages() {
		var pages = document.querySelectorAll("[data-juneau-page]");
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
