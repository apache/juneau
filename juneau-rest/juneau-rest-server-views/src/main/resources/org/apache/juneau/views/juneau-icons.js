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
 * additional or overriding icons the APP DRAWS ITSELF at runtime via
 * window.JuneauViews.icons.registerIcon(name, svgMarkup) - that does not change a chrome stem.
 *
 * PAGE-LEVEL SPRITE CONFIGURATION
 * ------------------------------
 * Every chrome glyph the framework paints resolves through up to three sprite layers, in order:
 *
 *     per-icon override  ->  replacement  ->  shipped        (first hit wins)
 *
 * The SHIPPED layer is Juneau's own set (`juneau-symbols.svg`, injected once so hosts can
 * <use href="#juneau-sym-{stem}"/>).  An app may, ONCE per page BEFORE the chrome draws, point at a
 * REPLACEMENT sprite (its own set - may be partial) and/or an OVERRIDE sprite (a few names on top).
 * All three use the SAME `juneau-sym-{stem}` symbol ids.
 *
 * Documented registration is attributes on this script tag, read before first paint:
 *     <script src=".../juneau-icons.js"
 *             data-juneau-icon-replacement="/my-set.svg"
 *             data-juneau-icon-override="/my-overrides.svg">
 * (An inline copy of this file must never contain a literal closing script tag, even in a comment: the HTML
 * parser would end the inline script there.)
 * A JavaScript call before first paint is equivalent:
 *     JuneauViews.icons.sprites({ replacementUrl: "...", overrideUrl: "..." })
 *
 * `sprites(...)` returns a Promise that reports, per layer, whether it loaded / failed / was absent,
 * and, per stem, which layer won.  The page still paints if the app does not await it.  A layer whose
 * URL fails to fetch or parse (including a blocked cross-origin fetch) is skipped - the remaining layers
 * and the shipped set fill any name it would have carried; a layer that DID load still wins.  A call that
 * arrives after first paint is ignored (dev warns).  A name in none of the three layers draws nothing.
 *
 * Dev-mode warnings (failed load, unknown id, late registration) are emitted only when dev mode is on -
 * set `JuneauViews.dev = true` or add `data-juneau-dev` to this script tag.  Production is silent and
 * falls back to the shipped glyph with no user-facing error.
 *
 * The legacy `JuneauViews.icons.pack("material")` / `data-juneau-icon-pack="material"` selector loads the
 * Material Symbols Outlined sprite as the REPLACEMENT layer on top of the real shipped sprite (as if the app had
 * passed `replacementUrl` pointing at it).  Stems the Material pack lacks fall back to the shipped glyph.  An
 * explicit `replacementUrl` wins over the pack.  New apps point `replacementUrl` at the set they want instead.
 * `pack` / `registerIcon` do not rewrite chrome stems - chrome overrides go on the override sprite.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	const registry = NS._icons = NS._icons || {};

	// Author-facing icon name -> sprite stem id (the map U2 publishes; wrong ids fall through to shipped).
	const NAME_TO_STEM = {};

	const SPRITE_ID = "juneau-symbol-sprite";
	const SVG_NS = "http://www.w3.org/2000/svg";
	const STEM_PREFIX = "juneau-sym-";
	const PACK_ORIGINAL = "original";
	const PACK_MATERIAL = "material";
	const PACK_FILES = {};
	PACK_FILES[PACK_ORIGINAL] = "juneau-symbols.svg";
	PACK_FILES[PACK_MATERIAL] = "juneau-symbols-material.svg";
	let _pack = PACK_ORIGINAL;

	// Page-level layer configuration.  Recorded from the script tag at eval time and/or a sprites(...) call
	// before first paint; frozen once loadSymbolSprite() begins (that is "first paint" for this runtime).
	let _replacementUrl = null;
	let _overrideUrl = null;
	let _loadStarted = false;
	let _gen = 0;   // bumped by pack(); a load from an older generation is dropped when it settles
	let _spritePromise = null;
	let _report = null;
	let _shippedStems = null;
	let _mergedStems = null;

	/** True only when dev mode is explicitly on - NS.dev===true or a data-juneau-dev attr on the icons script. */
	function isDevMode() {
		if (NS.dev === true) return true;
		if (NS.dev === false) return false;
		if (typeof document === "undefined") return false;
		const scripts = document.getElementsByTagName("script");
		for (const script of scripts) {
			if (script.dataset.juneauDev != null && (script.src || "").includes("juneau-icons.js"))
				return true;
		}
		return false;
	}

	/** Dev-only console warning; a no-op in production so a failed layer never surfaces a user-facing error. */
	function warn(msg, err) {
		if (isDevMode() && window.console?.warn)
			console.warn(err != null ? msg + " " + err : msg);
	}

	function normalizePack(name) {
		if (name == null || name === "")
			return PACK_ORIGINAL;
		if (name === PACK_ORIGINAL || name === PACK_MATERIAL)
			return name;
		throw new TypeError("JuneauViews.icons.pack: unknown pack '" + name + "' (original|material)");
	}

	function iconScriptAttr(attr) {
		if (typeof document === "undefined")
			return null;
		const scripts = document.getElementsByTagName("script");
		for (const script of scripts) {
			const v = script.getAttribute(attr);
			if (v != null && (script.src || "").includes("juneau-icons.js"))
				return v;
		}
		for (const script of scripts) {
			const v = script.getAttribute(attr);
			if (v != null)
				return v;
		}
		return null;
	}

	function detectPackFromScript() {
		const attr = iconScriptAttr("data-juneau-icon-pack");
		return attr != null ? normalizePack(attr) : PACK_ORIGINAL;
	}

	_pack = detectPackFromScript();
	_replacementUrl = iconScriptAttr("data-juneau-icon-replacement") || null;
	_overrideUrl = iconScriptAttr("data-juneau-icon-override") || null;

	/**
	 * Selects the legacy symbol pack, or returns the current pack when called with no args.  Default is
	 * `"original"` (Juneau's own shipped set only).  `"material"` layers the Material Symbols Outlined set as the
	 * REPLACEMENT layer over the real shipped sprite, so stems Material lacks still draw the shipped glyph; an
	 * explicit `replacementUrl` wins over it.  Changing pack after boot reloads the sprite.  New apps should instead
	 * point `sprites({replacementUrl})` at the set they want; this selector does not rewrite chrome stems.
	 *
	 * @example
	 * // Legacy selector: layer the Material Symbols Outlined sprite over the shipped sprite.
	 * JuneauViews.icons.pack("material");
	 * JuneauViews.icons.pack();   // "material"
	 */
	function pack(name) {
		if (arguments.length === 0)
			return _pack;
		const next = normalizePack(name);
		if (next === _pack)
			return _spritePromise || Promise.resolve(_report || emptyReport());
		_pack = next;
		_gen++;   // any load still in flight belongs to the old pack and is dropped when it settles
		_spritePromise = null;
		// _loadStarted stays true: this is a reload, not a reopening of the pre-paint registration window.
		if (typeof document !== "undefined") {
			document.getElementById(SPRITE_ID)?.remove();
			return loadSymbolSprite();
		}
		return Promise.resolve(emptyReport());
	}

	/**
	 * Configures the page's optional replacement and/or override sprite URLs and (re)loads the sprite.  Must be
	 * called before first paint; a call after the sprite has begun loading is ignored and warns in dev.  Returns
	 * a Promise reporting, per layer, whether it loaded/failed/was absent, and per stem which layer won.
	 *
	 * @example
	 * // Before the chrome draws (or as data-juneau-icon-* attributes on the script tag):
	 * JuneauViews.icons.sprites({ replacementUrl: "/my/set.svg", overrideUrl: "/my/overrides.svg" })
	 *   .then(function (report) { console.log(report.layers, report.stems.search); });
	 */
	function sprites(opts) {
		opts = opts || {};
		if (_loadStarted) {
			warn("JuneauViews.icons.sprites(...) called after the sprite load began (first sprites() call or page boot); ignored.");
			return _spritePromise || Promise.resolve(_report || emptyReport());
		}
		if (Object.hasOwn(opts, "replacementUrl"))
			_replacementUrl = opts.replacementUrl || null;
		if (Object.hasOwn(opts, "overrideUrl"))
			_overrideUrl = opts.overrideUrl || null;
		return loadSymbolSprite();
	}

	/**
	 * Registers (or overrides) an icon the APP DRAWS ITSELF under `name`; does not change a chrome stem (chrome
	 * overrides go on the override sprite).
	 *
	 * @example
	 * JuneauViews.icons.registerIcon("myApp.logo", "<svg viewBox='0 0 24 24'><circle cx='12' cy='12' r='9'/></svg>");
	 * JuneauViews.icons.resolveIcon("myApp.logo");   // the markup above
	 */
	function registerIcon(name, svgMarkup) {
		registry[name] = svgMarkup;
		return registry[name];
	}

	/** Registers a chrome icon `name` as a <use> host of sprite stem `stem`, and records the name->stem map. */
	function reg(name, stem, extraClass) {
		registry[name] = host(stem, extraClass);
		NAME_TO_STEM[name] = stem;
		return registry[name];
	}

	/**
	 * Looks up an icon's markup by name.  `search` and the prefixed sprite id `juneau-sym-search` resolve to the
	 * same glyph (U3).  A stem present in a loaded layer but not explicitly registered still resolves to a host.
	 * Returns null for an unknown name (dev warns); callers then draw nothing rather than the raw name.
	 *
	 * @example
	 * JuneauViews.icons.resolveIcon("search");            // "<svg ...><use href=\"#juneau-sym-search\"/></svg>"
	 * JuneauViews.icons.resolveIcon("juneau-sym-search"); // the same markup (prefixed id equivalence)
	 * JuneauViews.icons.resolveIcon("no-such-icon");      // null: the caller draws nothing
	 */
	function resolveIcon(name) {
		if (name == null)
			return null;
		const stem = name.startsWith(STEM_PREFIX) ? name.slice(STEM_PREFIX.length) : name;
		// Every stem, sort included, is a <use> host: it resolves against whatever sprite is in the document when it
		// paints, so an override/replacement sprite wins even for a glyph drawn before the sprite loaded.
		if (Object.hasOwn(registry, name))
			return registry[name];
		if (stem !== name && Object.hasOwn(registry, stem))
			return registry[stem];
		if (_mergedStems?.includes(stem))
			return host(stem);
		warn("JuneauViews.icons.resolveIcon: unknown icon '" + name + "'.");
		return null;
	}

	/**
	 * The author-name -> sprite-stem map (U2).  Wrong ids fall through the layers to the shipped glyph.
	 *
	 * @example
	 * JuneauViews.icons.nameToStem().content_copy === "copy";   // true: the author name maps to the `copy` stem
	 */
	function nameToStem() {
		const out = {};
		for (const k in NAME_TO_STEM)
			if (Object.hasOwn(NAME_TO_STEM, k))
				out[k] = NAME_TO_STEM[k];
		return out;
	}

	/**
	 * The shipped stem names (U9).  After load this is the shipped catalog (a replacement that adds a stem does
	 * not grow it); before load, the registered stems.
	 *
	 * @example
	 * JuneauViews.icons.stems();   // ["cancel", "check", "chevrondown", ... "more", ... "search", ...]
	 */
	function stems() {
		if (_shippedStems)
			return _shippedStems.slice();
		const seen = {}, out = [];
		for (const k in NAME_TO_STEM) {
			if (Object.hasOwn(NAME_TO_STEM, k) && !seen[NAME_TO_STEM[k]]) {
				seen[NAME_TO_STEM[k]] = true;
				out.push(NAME_TO_STEM[k]);
			}
		}
		return out;
	}

	function emptyReport() {
		return { layers: { shipped: "absent", replacement: "absent", override: "absent" }, stems: {}, names: [] };
	}

	/** Resolves a sprite file next to this script (the cache-buster query is preserved). */
	function siblingUrl(file) {
		const scripts = document.getElementsByTagName("script");
		for (const script of scripts) {
			const src = script.src || "";
			const m = /^(.*)juneau-icons\.js(\?.*)?$/.exec(src);
			if (m) return m[1] + file + (m[2] || "");
		}
		return file;
	}

	/** Parses sprite SVG text; returns the root <svg> element, or null when the text is not parseable SVG. */
	function parseSvg(xml) {
		const doc = new DOMParser().parseFromString(xml, "image/svg+xml");
		const root = doc.documentElement;
		if (root?.nodeName.toLowerCase() !== "svg" || root.querySelector("parsererror"))
			return null;
		return root;
	}

	/** Extracts a { stem: <symbol> } map from a parsed sprite root, keyed by the `juneau-sym-` id suffix. */
	function symbolsOf(root) {
		const out = {};
		const syms = root.getElementsByTagName("symbol");
		for (const sym of syms) {
			const id = sym.getAttribute("id") || "";
			if (id.startsWith(STEM_PREFIX))
				out[id.slice(STEM_PREFIX.length)] = sym;
		}
		return out;
	}

	/** Fetches one layer's sprite; always resolves to { name, status, symbols } - a failed fetch is `failed`/{}. */
	function fetchLayer(url, layerName) {
		if (!url)
			return Promise.resolve({ name: layerName, status: "absent", symbols: {} });
		return fetch(url, { credentials: "same-origin" })
			.then(function (r) {
				if (!r.ok) throw new Error("HTTP " + r.status);
				return r.text();
			})
			.then(function (xml) {
				const root = parseSvg(xml);
				if (!root) throw new Error("parse failed");
				return { name: layerName, status: "loaded", symbols: symbolsOf(root) };
			})
			.catch(function (e) {
				warn("JuneauViews.icons: " + layerName + " sprite failed (" + url + ")", e);
				return { name: layerName, status: "failed", symbols: {} };
			});
	}

	/** Rebuilds and injects the single in-document sprite from the merged { stem: <symbol> } winners. */
	function injectMerged(merged) {
		document.getElementById(SPRITE_ID)?.remove();
		const svg = document.createElementNS ? document.createElementNS(SVG_NS, "svg") : document.createElement("svg");
		svg.setAttribute("id", SPRITE_ID);
		svg.setAttribute("display", "none");
		svg.setAttribute("aria-hidden", "true");
		for (const stem in merged)
			if (Object.hasOwn(merged, stem))
				svg.appendChild(document.importNode(merged[stem], true));
		document.documentElement.appendChild(svg);
	}

	/**
	 * Fetches the shipped sprite (plus the configured replacement / override layers) once, merges them per stem
	 * (override -> replacement -> shipped, first hit wins), injects the merged sprite, and resolves to the load
	 * report.  Always resolves - a failed layer is skipped, never a rejection.  Called automatically at boot;
	 * calling it again returns the same Promise.
	 *
	 * @example
	 * JuneauViews.icons.loadSymbolSprite().then(function (report) {
	 *   console.log(report.layers.shipped);   // "loaded"
	 * });
	 */
	function loadSymbolSprite() {
		if (_spritePromise) return _spritePromise;
		_loadStarted = true;
		if (typeof document === "undefined" || typeof fetch !== "function") {
			_report = emptyReport();
			_spritePromise = Promise.resolve(_report);
			return _spritePromise;
		}
		const gen = _gen;
		_spritePromise = Promise.all([
			fetchLayer(siblingUrl(PACK_FILES[PACK_ORIGINAL]), "shipped"),
			// pack("material") supplies the replacement layer unless the app named its own replacement URL.
			fetchLayer(_replacementUrl || (_pack === PACK_MATERIAL ? siblingUrl(PACK_FILES[PACK_MATERIAL]) : null), "replacement"),
			fetchLayer(_overrideUrl, "override")
		]).then(function (layers) {
			// A pack() switch while this load was in flight makes it stale: drop it without touching any state.
			if (gen !== _gen)
				return _spritePromise || emptyReport();
			const winner = {}, merged = {};
			// Applied shipped-first: a later layer overwrites, so override beats replacement beats shipped.
			layers.forEach(function (layer) {
				for (const stem in layer.symbols) {
					if (Object.hasOwn(layer.symbols, stem)) {
						merged[stem] = layer.symbols[stem];
						winner[stem] = layer.name;
					}
				}
			});
			injectMerged(merged);
			_shippedStems = Object.keys(layers[0].symbols);
			_mergedStems = Object.keys(merged);
			_report = {
				layers: { shipped: layers[0].status, replacement: layers[1].status, override: layers[2].status },
				stems: winner,
				names: _mergedStems.slice()
			};
			return _report;
		});
		return _spritePromise;
	}

	/**
	 * Host SVG referencing a sprite symbol.  `extraClass` is optional (the sort glyph's class, which the header
	 * order-state CSS uses to set the custom properties the sprite's two triangles paint with).
	 */
	function host(stem, extraClass) {
		const cls = extraClass ? " class=\"" + extraClass + "\"" : "";
		return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"" + cls
			+ " aria-hidden=\"true\"><use href=\"#juneau-sym-" + stem + "\"/></svg>";
	}

	NS.icons = {
		registerIcon: registerIcon,
		resolveIcon: resolveIcon,
		loadSymbolSprite: loadSymbolSprite,
		pack: pack,
		sprites: sprites,
		stems: stems,
		nameToStem: nameToStem
	};

	// Bundled names (ViewTable ribbon + paging pill + column chooser).  Each host is a <use> of
	// juneau-symbols.svg; reg() records the author-name -> stem map that resolveIcon/nameToStem publish.
	reg("content_copy", "copy");
	reg("copy", "copy");
	reg("csv", "csv");
	reg("table", "spreadsheet");
	reg("spreadsheet", "spreadsheet");
	reg("picture_as_pdf", "pdf");
	reg("pdf", "pdf");
	reg("print", "print");
	reg("refresh", "refresh");
	reg("manage_search", "toggle_column_search");
	reg("toggle_column_search", "toggle_column_search");
	reg("unfold_less", "collapse_all");
	reg("collapse_all", "collapse_all");
	reg("tune", "settings");
	reg("settings", "settings");
	reg("columns", "columns");
	reg("chevron_right", "chevronright");
	reg("chevronright", "chevronright");
	reg("chevron_left", "chevronleft");
	reg("chevronleft", "chevronleft");
	reg("chevron_up", "chevronup");
	reg("chevronup", "chevronup");
	reg("filter_alt", "filter");
	reg("filter", "filter");
	reg("expand_more", "chevrondown");
	reg("chevrondown", "chevrondown");
	reg("search", "search");
	reg("close", "close");
	reg("download", "download");
	reg("link", "link");
	reg("edit", "edit");
	reg("cancel", "cancel");
	reg("check", "check");
	reg("new", "new");
	reg("toggle-deleted", "toggle-deleted");
	reg("pause", "pause");
	reg("stop", "stop");
	reg("forceStop", "forceStop");
	reg("push", "push");
	reg("openPr", "openPr");
	// WORK-J0557 U1 — real sprite stems (no CSS-composed / flipped stand-ins).  sort is an ordinary <use> host;
	// the header asc/desc tint is driven by CSS custom properties on the host (they inherit into the <use> shadow tree).
	reg("first_page", "first_page");
	reg("last_page", "last_page");
	reg("more_vert", "more");
	reg("more", "more");
	reg("sort", "sort", "juneau-view-col-sort-glyph");

	if (typeof document !== "undefined") {
		// Defer the boot load past the point where an app's own sprites(...) call can still land: deferred and module
		// scripts run while readyState is "interactive", i.e. after this script but before DOMContentLoaded.
		if (document.readyState === "loading") {
			document.addEventListener("DOMContentLoaded", loadSymbolSprite);
		} else if (document.readyState === "interactive") {
			// DOMContentLoaded may already have fired (script injected late), so a 0ms timer is the backstop.
			document.addEventListener("DOMContentLoaded", loadSymbolSprite);
			setTimeout(loadSymbolSprite, 0);
		} else {
			Promise.resolve().then(loadSymbolSprite);
		}
	}
})();
