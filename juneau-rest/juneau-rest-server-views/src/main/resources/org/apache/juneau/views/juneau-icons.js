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
 * juneau-icons.js - dependency-free icon registry for the Apache Juneau rich-view toolkit.
 *
 * Maps icon names to inline-SVG markup that references one in-document sprite
 * (<symbol id="juneau-sym-{stem}"> from juneau-symbols.svg).  Mirrors juneau-renders.js's
 * registerRenderer/resolveRenderer pattern (registerIcon/resolveIcon).  Apps can register
 * additional or overriding icons at runtime via window.JuneauViews.icons.registerIcon(name, svgMarkup).
 *
 * The sprite is fetched once at boot from the same directory as this script and injected
 * into the document so every <use href="#juneau-sym-{stem}"/> resolves with no per-icon
 * network request.  Bundled names cover the ViewTable ribbon, paging pill, column chooser,
 * and search-clear glyphs this runtime actually paints.
 */
(function () {
	"use strict";

	var NS = window.JuneauViews = window.JuneauViews || {};

	var registry = NS._icons = NS._icons || {};

	var SPRITE_ID = "juneau-symbol-sprite";
	var _spritePromise = null;

	/** Registers (or overrides) an icon's inline-SVG markup under `name`. */
	function registerIcon(name, svgMarkup) {
		registry[name] = svgMarkup;
		return registry[name];
	}

	/** Looks up an icon's markup by name; returns null when unknown (callers fall back to rendering raw text). */
	function resolveIcon(name) {
		return Object.prototype.hasOwnProperty.call(registry, name) ? registry[name] : null;
	}

	/** Resolves juneau-symbols.svg next to this script (same cache-buster query is ignored by the serving mixin). */
	function spriteUrl() {
		var scripts = document.getElementsByTagName("script");
		for (var i = 0; i < scripts.length; i++) {
			var src = scripts[i].src || "";
			var m = src.match(/^(.*)juneau-icons\.js(\?.*)?$/);
			if (m) return m[1] + "juneau-symbols.svg" + (m[2] || "");
		}
		return "juneau-symbols.svg";
	}

	/** Fetches the sprite once and injects it so <use href="#juneau-sym-{stem}"/> resolves. Always resolves. */
	function loadSymbolSprite() {
		if (_spritePromise) return _spritePromise;
		if (typeof document === "undefined" || typeof fetch !== "function") {
			_spritePromise = Promise.resolve();
			return _spritePromise;
		}
		_spritePromise = fetch(spriteUrl(), { credentials: "same-origin" })
			.then(function (r) {
				if (!r.ok) throw new Error("sprite HTTP " + r.status);
				return r.text();
			})
			.then(function (xml) {
				var doc = new DOMParser().parseFromString(xml, "image/svg+xml");
				var root = doc.documentElement;
				if (!root || root.nodeName.toLowerCase() !== "svg" || root.querySelector("parsererror"))
					throw new Error("sprite parse failed");
				if (!document.getElementById(SPRITE_ID)) {
					root.setAttribute("id", SPRITE_ID);
					root.setAttribute("display", "none");
					root.setAttribute("aria-hidden", "true");
					document.documentElement.appendChild(document.importNode(root, true));
				}
			})
			.catch(function (e) {
				if (window.console && console.error) console.error("JuneauViews.icons: sprite load failed", e);
			});
		return _spritePromise;
	}

	/**
	 * Host SVG referencing a sprite symbol.  `extraClass` is optional (used to CSS-rotate chevronright for
	 * left/first paging without shipping a second path).
	 */
	function host(stem, extraClass) {
		var cls = extraClass ? " class=\"" + extraClass + "\"" : "";
		return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"" + cls
			+ " aria-hidden=\"true\"><use href=\"#juneau-sym-" + stem + "\"/></svg>";
	}

	/** Two overlapped chevronright hosts (first/last paging), matching the doubled-chevron ribbon treatment. */
	function doubled(flip) {
		var cls = flip ? "juneau-sym-flip-x" : "";
		return "<span class=\"juneau-view-paging-double\">" + host("chevronright", cls) + host("chevronright", cls) + "</span>";
	}

	NS.icons = {
		registerIcon: registerIcon,
		resolveIcon: resolveIcon,
		loadSymbolSprite: loadSymbolSprite
	};

	// Bundled names (ViewTable ribbon + paging pill + column chooser).  Each host is a <use> of
	// juneau-symbols.svg; first/last paging compose two chevronright uses (no extra paths).
	registerIcon("content_copy", host("copy"));
	registerIcon("copy", host("copy"));
	registerIcon("csv", host("csv"));
	registerIcon("table", host("spreadsheet"));
	registerIcon("spreadsheet", host("spreadsheet"));
	registerIcon("picture_as_pdf", host("pdf"));
	registerIcon("pdf", host("pdf"));
	registerIcon("refresh", host("refresh"));
	registerIcon("manage_search", host("toggle_column_search"));
	registerIcon("toggle_column_search", host("toggle_column_search"));
	registerIcon("unfold_less", host("collapse_all"));
	registerIcon("collapse_all", host("collapse_all"));
	registerIcon("tune", host("settings"));
	registerIcon("settings", host("settings"));
	registerIcon("columns", host("columns"));
	registerIcon("first_page", doubled(true));
	registerIcon("chevron_left", host("chevronright", "juneau-sym-flip-x"));
	registerIcon("chevron_right", host("chevronright"));
	registerIcon("chevronright", host("chevronright"));
	registerIcon("last_page", doubled(false));
	registerIcon("filter_alt", host("filter"));
	registerIcon("filter", host("filter"));
	registerIcon("expand_more", host("chevrondown"));
	registerIcon("chevrondown", host("chevrondown"));
	registerIcon("search", host("search"));
	registerIcon("close", host("close"));
	registerIcon("download", host("download"));
	registerIcon("edit", host("edit"));
	registerIcon("cancel", host("cancel"));
	registerIcon("check", host("check"));
	registerIcon("new", host("new"));
	registerIcon("toggle-deleted", host("toggle-deleted"));

	if (typeof document !== "undefined") {
		if (document.readyState === "loading")
			document.addEventListener("DOMContentLoaded", loadSymbolSprite);
		else
			loadSymbolSprite();
	}
})();
