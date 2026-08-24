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
 * juneau-chrome.js - opt-in client runtime for the Apache Juneau page-chrome widgets (AppHeaderDef app-header /
 * AvatarChip / BarSlot).  Namespace-scoped as window.JuneauChrome, no ES import - a SEPARATE served bundle in the
 * same sense as juneau-pages.js / juneau-cards.js.  It does NOT refactor, extract from, or modify juneau-views.js.
 * Load order: juneau-icons.js -> juneau-chrome.js (header action glyphs resolve from the icon registry), and
 * juneau-views.js before juneau-chrome.js if the page also wants the shared layer manager for menus (see below).
 *
 * On DOMContentLoaded it scans [data-juneau-app-header] and [data-juneau-bar-slot] regions and, per region, performs
 * a fail-loud contract-version handshake against the baked-in JUNEAU_HEADER_CONTRACT_VERSION /
 * JUNEAU_BAR_CONTRACT_VERSION before hydrating action glyphs, wiring an avatar image fallback, dispatching SAFE
 * host-events, and (only when the host explicitly calls refresh(root)) re-applying same-origin counts.
 *
 * Menus WAIT on [TODO-445h] (M1 B): a Behavior.MENU trigger arrives DISABLED with its list OMITTED, so this runtime
 * never opens one.  It also does NOT define a layer manager: pushLayer(...) here is a THIN FORWARD to
 * window.JuneauViews.init.pushLayer (445h/juneau-views.js) when present, else a no-op - it never competes with the
 * views layer stack.  Nothing internally calls it under M1 B.
 *
 * The "PURE LOGIC LAYER" is DOM-free (plain data in, plain data out) and independently node-testable; the "DOM
 * BINDING LAYER" is the thin shim that scans, handshakes, hydrates and binds.  There is NO poller: refresh is
 * demand-only, via the exported refresh(root) entry.
 */
(function () {
	"use strict";

	// Contract-version handshakes: each MUST equal its server constant (AppHeaderDef.CONTRACT_VERSION /
	// BarSlot.CONTRACT_VERSION), surfaced as ViewsMixin.HEADER_CONTRACT_VERSION / ViewsMixin.BAR_CONTRACT_VERSION.
	// Two distinct constants: a bar-envelope revision must never force a header-sidecar bump, or vice-versa.
	const JUNEAU_HEADER_CONTRACT_VERSION = "1";
	const JUNEAU_BAR_CONTRACT_VERSION = "1";

	// DOM attribute names - MUST equal AppHeaderTable / BarSlotTable constants of the same names on the server.
	const HEADER_MARKER = "data-juneau-app-header";
	const BAR_SLOT_MARKER = "data-juneau-bar-slot";
	const ACTION_MARKER = "data-juneau-header-action";
	const BEHAVIOR_ATTR = "data-juneau-behavior";
	const SAFE_ATTR = "data-juneau-safe";
	const ICON_ATTR = "data-juneau-icon";
	const BADGE_ATTR = "data-juneau-badge";
	const BADGE_MAX_ATTR = "data-juneau-badge-max";
	const AVATAR_MARKER = "data-juneau-avatar";
	const REFRESH_ATTR = "data-juneau-refresh";
	const HEADER_SIDECAR_PREFIX = "juneau-header:";
	const BAR_SIDECAR_PREFIX = "juneau-bar:";

	// The CustomEvent a SAFE control dispatches for the host to act on (no built-in navigation, no new token type).
	const SAFE_EVENT = "juneau:chrome-safe";

	// SAFE host-dispatch token format - MUST equal the server SAFE_TOKEN pattern (HeaderAction / MenuItem):
	// a lowercase letter then up to 63 more lowercase-alnum-or-hyphen chars.  Defense-in-depth re-check.
	const SAFE_TOKEN_RE = /^[a-z][a-z0-9-]{0,63}$/;

	const NS = window.JuneauChrome = window.JuneauChrome || {};
	NS.HEADER_CONTRACT_VERSION = JUNEAU_HEADER_CONTRACT_VERSION;
	NS.BAR_CONTRACT_VERSION = JUNEAU_BAR_CONTRACT_VERSION;

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==================================================================================================================

	/**
	 * Client-side endpoint re-check before any demand-refresh fetch: same-origin AND non-templated - rejects an
	 * absolute URL ("scheme://"), a protocol-relative "//host", any "scheme:" prefix, a ".." path segment, and any
	 * "{...}" template placeholder.  Matches the Java same-origin rule the chrome beans validate() with; a chrome
	 * region is never row-scoped, so a "{id}" template is meaningless here and is rejected.
	 */
	function isSafeChromeEndpoint(path) {
		if (typeof path !== "string" || path.length === 0) return false;
		if (path.indexOf("{") >= 0) return false;                 // no template placeholder
		if (path.indexOf("://") >= 0) return false;               // absolute URL
		if (path.charAt(0) === "/" && path.charAt(1) === "/") return false;   // protocol-relative
		const colon = path.indexOf(":");
		const slash = path.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;   // "scheme:" before any slash (javascript:, servlet:)
		if (path === ".." || path.indexOf("../") >= 0 || path.indexOf("/..") >= 0) return false;
		return true;
	}

	/** True when a SAFE host-dispatch token is format-valid (mirrors the server SAFE_TOKEN pattern). */
	function isSafeToken(token) {
		return typeof token === "string" && SAFE_TOKEN_RE.test(token);
	}

	/** Fail-loud handshake predicate: a sidecar/refresh envelope's contractVersion must equal the baked-in expected value. */
	function envelopeContractOk(env, expected) {
		return !!env && typeof env === "object" && env.contractVersion === expected;
	}

	/**
	 * Clamps a fresh count above `max` to "<max>+" (display text), exactly as the server AppHeaderTable.clampCount
	 * does - so a demand-refresh re-paints the same clamped label the server first rendered.  Pure.
	 */
	function clampCount(count, max) {
		if (typeof count !== "number") return "";
		if (typeof max === "number" && count > max) return max + "+";
		return String(count);
	}

	/** Extracts the {namespaced-id -> count} map from a sidecar/refresh envelope; returns {} for a shapeless envelope. */
	function envelopeBadges(env) {
		return env && typeof env === "object" && env.badges && typeof env.badges === "object" ? env.badges : {};
	}

	// ==================================================================================================================
	// DOM BINDING LAYER
	// ==================================================================================================================

	/** Resolves an icon glyph from the shared juneau-icons.js registry, or null when the registry/name is absent. */
	function resolveIconMarkup(name) {
		const icons = window.JuneauViews && window.JuneauViews.icons;
		return icons && icons.resolveIcon ? icons.resolveIcon(name) : null;
	}

	/**
	 * Hydrates every [data-juneau-icon] action's empty .jc-icon span with its trusted first-party registry SVG.  This
	 * is the ONE allow-listed innerHTML sink in this runtime: the markup is registry-owned (juneau-icons.js), never
	 * request/response/app text - every human string stays on the server-emitted, entity-escaped text path.
	 */
	function hydrateIcons(root) {
		if (!root || !root.querySelectorAll) return;
		const hosts = root.querySelectorAll("[" + ICON_ATTR + "]");
		for (let i = 0; i < hosts.length; i++) {
			const host = hosts[i];
			const glyph = resolveIconMarkup(host.getAttribute(ICON_ATTR));
			if (!glyph) continue;
			const iconSpan = host.querySelector(".jc-icon");
			if (iconSpan) iconSpan.innerHTML = glyph;             // trusted registry markup only - the ONE allow-listed sink
		}
	}

	/**
	 * Applies a fresh {namespaced-id -> count} map to a region's count badges via textContent only (never innerHTML):
	 * a badge whose id is absent from the map is left untouched, a non-numeric value is skipped, and a numeric value
	 * is re-clamped with the badge's own data-juneau-badge-max so it matches the server label.  Dot badges carry no
	 * count and are never in the map, so they are never rewritten.
	 */
	function applyCounts(root, counts) {
		if (!root || !root.querySelectorAll || !counts || typeof counts !== "object") return;
		const badges = root.querySelectorAll("[" + BADGE_ATTR + "]");
		for (let i = 0; i < badges.length; i++) {
			const b = badges[i];
			const id = b.getAttribute(BADGE_ATTR);
			if (!Object.hasOwn(counts, id)) continue;
			const raw = counts[id];
			if (typeof raw !== "number") continue;
			const maxAttr = b.getAttribute(BADGE_MAX_ATTR);
			const max = maxAttr != null ? parseInt(maxAttr, 10) : null;
			b.textContent = clampCount(raw, max);                 // textContent only - a fresh count never reaches innerHTML
		}
	}

	/** Reads and JSON-parses a region's data-only sidecar (by "<prefix><id>" element id); returns null when absent/unparseable. */
	function readSidecar(prefix, id) {
		if (!window.document || !window.document.getElementById) return null;
		const el = window.document.getElementById(prefix + id);
		if (!el) return null;
		try {
			return JSON.parse(el.textContent);
		} catch (e) {
			return null;
		}
	}

	/**
	 * THIN FORWARD to the shared 445h/juneau-views.js layer manager (window.JuneauViews.init.pushLayer) when present,
	 * else a no-op returning null.  This runtime deliberately does NOT define its own layer stack (m3 B): menus wait on
	 * 445h, so nothing here calls it yet - it exists only so a later menu build forwards to the ONE shared manager.
	 */
	function pushLayer(el, opts) {
		const views = window.JuneauViews && window.JuneauViews.init;
		if (views && typeof views.pushLayer === "function")
			return views.pushLayer(el, opts);
		return null;                                              // 445h layer manager absent - no-op (menus wait)
	}

	/** Wires an avatar's image so a broken same-origin image falls back to the hidden initials chip (no dead avatar). */
	function wireAvatarFallback(root) {
		if (!root || !root.querySelectorAll) return;
		const avatars = root.querySelectorAll("[" + AVATAR_MARKER + "]");
		for (let i = 0; i < avatars.length; i++) {
			const img = avatars[i].querySelector("img.jc-avatar-img");
			if (!img || !img.addEventListener) continue;
			img.addEventListener("error", function () {
				img.hidden = true;
				const initials = avatars[i].querySelector(".jc-avatar-initials");
				if (initials) initials.hidden = false;
			});
		}
	}

	/**
	 * Wires each fully-functional SAFE action to dispatch a bubbling CustomEvent the host listens for; the host does
	 * the work (there is no built-in navigation and no new token type).  A format-invalid token is refused loud - it
	 * can only be a hand-edited attribute, since the server already format-validates it.  MENU triggers arrive
	 * disabled (menus wait on 445h) so they are never wired; LINK actions are plain anchors needing no wiring.
	 */
	function wireSafeActions(root) {
		if (!root || !root.querySelectorAll) return;
		const actions = root.querySelectorAll("[" + ACTION_MARKER + "][" + BEHAVIOR_ATTR + "='safe']");
		for (let i = 0; i < actions.length; i++) {
			const el = actions[i];
			const token = el.getAttribute(SAFE_ATTR);
			if (!isSafeToken(token)) {
				(window.console && console.error) && console.error(
					"juneau-chrome.js: refusing to wire SAFE action with malformed token '" + token + "'.");
				continue;
			}
			if (!el.addEventListener) continue;
			el.addEventListener("click", function (ev) {
				if (ev && ev.preventDefault) ev.preventDefault();
				dispatchSafe(el, token, root);
			});
		}
	}

	/** Dispatches the SAFE CustomEvent (bubbling) carrying the token, action id and its region root. */
	function dispatchSafe(el, token, root) {
		if (typeof window.CustomEvent !== "function" || !el.dispatchEvent) return;
		el.dispatchEvent(new window.CustomEvent(SAFE_EVENT, {
			bubbles: true,
			detail: { token: token, actionId: el.getAttribute(ACTION_MARKER), root: root }
		}));
	}

	/**
	 * Demand-refresh (no poller): fetches a region's data-juneau-refresh endpoint once, and on a contract-OK envelope
	 * re-applies its counts.  Returns a Promise<boolean> (false when there is no endpoint, it is unsafe, the fetch
	 * fails, or the handshake fails - a bad envelope never paints stale/foreign data).
	 */
	function refresh(root) {
		if (!root || !root.getAttribute) return Promise.resolve(false);
		const url = root.getAttribute(REFRESH_ATTR);
		if (!url || !isSafeChromeEndpoint(url)) return Promise.resolve(false);
		const expected = root.getAttribute(HEADER_MARKER) != null
			? JUNEAU_HEADER_CONTRACT_VERSION : JUNEAU_BAR_CONTRACT_VERSION;
		return window.fetch(url, { method: "GET", credentials: "same-origin", cache: "no-store" })
			.then(function (r) { return r.json(); })
			.then(function (env) {
				if (!envelopeContractOk(env, expected)) return false;
				applyCounts(root, envelopeBadges(env));
				return true;
			})
			.catch(function () { return false; });
	}

	/** Enhances one app-header: handshake, icon hydration, avatar fallback, SAFE wiring, initial-count apply. */
	function initHeader(header) {
		const id = header.getAttribute(HEADER_MARKER);
		const sidecar = readSidecar(HEADER_SIDECAR_PREFIX, id);
		if (sidecar && !envelopeContractOk(sidecar, JUNEAU_HEADER_CONTRACT_VERSION)) {
			(window.console && console.error) && console.error(
				"juneau-chrome.js: app-header contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_HEADER_CONTRACT_VERSION + "'); leaving header un-enhanced.");
			return null;
		}
		hydrateIcons(header);
		wireAvatarFallback(header);
		wireSafeActions(header);
		if (sidecar) applyCounts(header, envelopeBadges(sidecar));
		return { root: header, refresh: function () { return refresh(header); } };
	}

	/** Enhances one bar slot: handshake + initial-count apply (bar widgets carry no icons or controls). */
	function initBarSlot(bar) {
		const id = bar.getAttribute(BAR_SLOT_MARKER);
		const sidecar = readSidecar(BAR_SIDECAR_PREFIX, id);
		if (sidecar && !envelopeContractOk(sidecar, JUNEAU_BAR_CONTRACT_VERSION)) {
			(window.console && console.error) && console.error(
				"juneau-chrome.js: bar-slot contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_BAR_CONTRACT_VERSION + "'); leaving bar slot un-enhanced.");
			return null;
		}
		if (sidecar) applyCounts(bar, envelopeBadges(sidecar));
		return { root: bar, refresh: function () { return refresh(bar); } };
	}

	/** DOMContentLoaded entry: enhance every app-header, then every bar slot (a bar id can repeat per sub-tab bar). */
	function initAll() {
		const out = { headers: [], bars: [] };
		const headers = window.document.querySelectorAll("[" + HEADER_MARKER + "]");
		for (let i = 0; i < headers.length; i++) {
			const ctl = initHeader(headers[i]);
			if (ctl) out.headers.push(ctl);
		}
		const bars = window.document.querySelectorAll("[" + BAR_SLOT_MARKER + "]");
		for (let i = 0; i < bars.length; i++) {
			const ctl = initBarSlot(bars[i]);
			if (ctl) out.bars.push(ctl);
		}
		return out;
	}

	NS.init = {
		JUNEAU_HEADER_CONTRACT_VERSION: JUNEAU_HEADER_CONTRACT_VERSION,
		JUNEAU_BAR_CONTRACT_VERSION: JUNEAU_BAR_CONTRACT_VERSION,
		SAFE_EVENT: SAFE_EVENT,
		isSafeChromeEndpoint: isSafeChromeEndpoint,
		isSafeToken: isSafeToken,
		envelopeContractOk: envelopeContractOk,
		clampCount: clampCount,
		envelopeBadges: envelopeBadges,
		resolveIconMarkup: resolveIconMarkup,
		hydrateIcons: hydrateIcons,
		applyCounts: applyCounts,
		readSidecar: readSidecar,
		pushLayer: pushLayer,
		wireAvatarFallback: wireAvatarFallback,
		wireSafeActions: wireSafeActions,
		dispatchSafe: dispatchSafe,
		refresh: refresh,
		initHeader: initHeader,
		initBarSlot: initBarSlot,
		initAll: initAll
	};

	if (window.document && window.document.readyState === "loading") {
		window.document.addEventListener("DOMContentLoaded", initAll);
	} else if (window.document) {
		initAll();
	}
})();
