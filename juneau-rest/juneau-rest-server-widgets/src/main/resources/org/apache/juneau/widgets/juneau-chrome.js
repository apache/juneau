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
 * JUNEAU_BAR_CONTRACT_VERSION before hydrating action glyphs, wiring an avatar image fallback, wiring menu triggers,
 * dispatching SAFE host-events, and (only when the host explicitly calls refresh(root)) re-applying same-origin counts.
 *
 * Menus ride the ONE shared layer stack (window.JuneauViews.init) that 445h/juneau-views.js owns.  This runtime does
 * NOT define pushLayer/popLayer (Pass 5 M-P5-B1) and carries no competing popup-layer stack of its own: a Behavior.MENU trigger's
 * aria-controls'd .jc-menu list is portalled to document.body (position:fixed) as a kind:"menu" light-dismiss layer,
 * positioned under the trigger, and its Escape / outside-click dismissal + z-index depth are owned by that shared
 * stack - exactly as the harvested row-action menus do.  With the stack absent (views not loaded) a MENU trigger is
 * inert and its list stays display:none - never a fake <details>/role=menu disclosure.  Menus are single-level (445h
 * owns nested popups).
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
	// A menu trigger's aria-controls points at its .jc-menu list; a marker records that a trigger is already wired.
	const MENU_CLASS = "jc-menu";
	const MENU_ITEM_CLASS = "jc-menu-item";
	const MENU_WIRED_ATTR = "data-juneau-menu-wired";
	const MENU_ITEMS_WIRED_ATTR = "data-juneau-menu-items-wired";
	// The ONE marker wireSafeActions and initAll share, so re-running initAll (the enhance-on-insert path a cloned
	// row-detail bar slot needs) can never bind a second click handler onto an already-wired SAFE action.  The avatar
	// image fallback carries the same guard for the same reason.
	const SAFE_WIRED_ATTR = "data-juneau-safe-wired";
	const AVATAR_WIRED_ATTR = "data-juneau-avatar-wired";
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
		if (path.startsWith("//")) return false;                  // protocol-relative
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
		return typeof env === "object" && env?.contractVersion === expected;
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
		return typeof env === "object" && env?.badges && typeof env.badges === "object" ? env.badges : {};
	}

	// ==================================================================================================================
	// DOM BINDING LAYER
	// ==================================================================================================================

	/** Resolves an icon glyph from the shared juneau-icons.js registry, or null when the registry/name is absent. */
	function resolveIconMarkup(name) {
		const icons = window.JuneauViews?.icons;
		return icons?.resolveIcon?.(name) ?? null;
	}

	/**
	 * Hydrates every [data-juneau-icon] action's empty .jc-icon span with its trusted first-party registry SVG.  This
	 * is the ONE allow-listed innerHTML sink in this runtime: the markup is registry-owned (juneau-icons.js), never
	 * request/response/app text - every human string stays on the server-emitted, entity-escaped text path.
	 */
	function hydrateIcons(root) {
		if (!root || !root.querySelectorAll) return;
		const hosts = root.querySelectorAll("[" + ICON_ATTR + "]");
		for (const host of hosts) {
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
		for (const b of badges) {
			const id = b.getAttribute(BADGE_ATTR);
			if (!Object.hasOwn(counts, id)) continue;
			const raw = counts[id];
			if (typeof raw !== "number") continue;
			const maxAttr = b.getAttribute(BADGE_MAX_ATTR);
			const max = maxAttr != null ? Number.parseInt(maxAttr, 10) : null;
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
			return null;                                          // malformed/absent sidecar JSON - callers treat a null sidecar as "no sidecar"
		}
	}

	/**
	 * Resolves the ONE shared layer stack (window.JuneauViews.init, 445h/juneau-views.js) or null when views is not
	 * loaded.  This runtime NEVER defines pushLayer/popLayer of its own (Pass 5 M-P5-B1): it is strictly a client of
	 * the shared stack, so a chrome menu shares the same z-index / Escape / light-dismiss depth as views dialogs.
	 */
	function viewsLayerStack() {
		const views = window.JuneauViews?.init;
		return typeof views?.pushLayer === "function" && typeof views?.popLayer === "function" ? views : null;
	}

	/**
	 * chrome-local positioner: sets top/left (and shows) an ALREADY-portalled menu node under its trigger.  views'
	 * pushLayer already reparented it to body and set position:fixed; this only computes the cell-anchored offset (with
	 * a light viewport clamp), never re-parents.  positionCellPopover is not exported by views, so chrome carries this
	 * tiny helper rather than reaching into the views internals.
	 */
	function positionMenuUnderTrigger(menu, trigger) {
		if (!menu || !menu.style || !trigger || typeof trigger.getBoundingClientRect !== "function") return;
		const rect = trigger.getBoundingClientRect();
		const vw = (typeof window !== "undefined" && window.innerWidth) ? window.innerWidth : 1024;
		menu.style.display = "block";
		const w = menu.offsetWidth || 0;
		let left = rect.left;
		const top = rect.bottom + 4;
		if (left + w > vw - 4) left = Math.max(4, vw - w - 4);
		if (left < 4) left = 4;
		menu.style.left = left + "px";
		menu.style.top = top + "px";
	}

	/** Resolves a trigger's aria-controls'd .jc-menu list node (compared by id via getElementById, never interpolated). */
	function menuForTrigger(trigger) {
		const id = trigger?.getAttribute ? trigger.getAttribute("aria-controls") : null;
		if (!id || typeof document === "undefined" || typeof document.getElementById !== "function") return null;
		const el = document.getElementById(id);
		return el?.classList && Array.prototype.indexOf.call(el.classList, MENU_CLASS) >= 0 ? el : null;
	}

	// Single-open tracking: chrome menus are one-at-a-time (a new open closes any other), matching the row-action menus.
	let openMenuRec = null;

	/**
	 * Wires SAFE .jc-menu-item buttons in a list to dispatch the SAME host CustomEvent a top-level SAFE action uses
	 * (from the owning TRIGGER, which stays in the header, so the event bubbles through the header even though the list
	 * is portalled to body), then close the menu.  A format-invalid token is refused loud (hand-edited attribute only).
	 * LINK items are plain same-origin anchors (native navigation) - closing the menu on their click keeps no orphan
	 * open if navigation is intercepted.  Dividers are inert.  Idempotent per list.
	 */
	function wireMenuItems(menu, trigger, root) {
		if (!menu || !menu.querySelectorAll || menu.getAttribute(MENU_ITEMS_WIRED_ATTR) === "1") return;
		menu.setAttribute(MENU_ITEMS_WIRED_ATTR, "1");
		const safeItems = menu.querySelectorAll("." + MENU_ITEM_CLASS + "[" + SAFE_ATTR + "]");
		for (const item of safeItems) {
			const token = item.getAttribute(SAFE_ATTR);
			if (!isSafeToken(token)) {
				window.console?.error?.(
					"juneau-chrome.js: refusing to wire SAFE menu item with malformed token '" + token + "'.");
				continue;
			}
			if (!item.addEventListener) continue;
			item.addEventListener("click", function (ev) {
				ev?.preventDefault?.();
				dispatchSafe(trigger, token, root);
				closeMenu();
			});
		}
		const links = menu.querySelectorAll("a." + MENU_ITEM_CLASS);
		for (const link of links)
			if (link.addEventListener) link.addEventListener("click", function () { closeMenu(); });
	}

	/** Opens a trigger's menu on the shared views stack (no stack / no list -> inert, never a fake disclosure). */
	function openMenu(trigger, root) {
		const views = viewsLayerStack();
		const menu = menuForTrigger(trigger);
		if (!views || !menu) return;
		wireMenuItems(menu, trigger, root);
		views.pushLayer(menu, {
			kind: "menu", portal: true, lightDismiss: true, trapFocus: false, detachOnPop: false,
			returnFocusTo: trigger,
			onDismiss: function () {
				trigger.setAttribute?.("aria-expanded", "false");
				if (menu.style) menu.style.display = "none";
				if (openMenuRec?.menu === menu) openMenuRec = null;
			}
		});
		positionMenuUnderTrigger(menu, trigger);
		trigger.setAttribute?.("aria-expanded", "true");
		openMenuRec = { trigger: trigger, menu: menu };
	}

	/** Closes the open chrome menu (if any) by popping its layer off the shared views stack; onDismiss resets ARIA. */
	function closeMenu() {
		const views = viewsLayerStack();
		if (views && openMenuRec) views.popLayer(openMenuRec.menu);
	}

	/** Toggles a trigger's menu: a re-click on the open trigger closes it; opening one first closes any other. */
	function toggleMenu(trigger, root) {
		if (openMenuRec?.trigger === trigger) { closeMenu(); return; }
		if (openMenuRec) closeMenu();
		openMenu(trigger, root);
	}

	/**
	 * Wires each enabled Behavior.MENU trigger (header action or avatar chip) to toggle its .jc-menu list on the shared
	 * views layer stack.  Idempotent per trigger.  A trigger with no resolvable list / no views stack stays inert.
	 */
	function wireMenus(root) {
		if (!root || !root.querySelectorAll) return;
		const triggers = root.querySelectorAll("[" + BEHAVIOR_ATTR + "='menu']");
		for (const trigger of triggers) {
			if (trigger.getAttribute(MENU_WIRED_ATTR) === "1" || !trigger.addEventListener) continue;
			trigger.setAttribute(MENU_WIRED_ATTR, "1");
			(function (t) {
				t.addEventListener("click", function (ev) {
					ev?.preventDefault?.();
					toggleMenu(t, root);
				});
			})(trigger);
		}
	}

	/**
	 * Wires an avatar's image so a broken same-origin image falls back to the hidden initials chip (no dead avatar).
	 * Idempotent per image (AVATAR_WIRED_ATTR), so a re-scan cannot stack error handlers.
	 */
	function wireAvatarFallback(root) {
		if (!root || !root.querySelectorAll) return;
		const avatars = root.querySelectorAll("[" + AVATAR_MARKER + "]");
		for (const avatar of avatars) {
			const img = avatar.querySelector("img.jc-avatar-img");
			if (!img || !img.addEventListener) continue;
			if (img.getAttribute(AVATAR_WIRED_ATTR) === "1") continue;
			img.setAttribute(AVATAR_WIRED_ATTR, "1");
			img.addEventListener("error", function () {
				img.hidden = true;
				const initials = avatar.querySelector(".jc-avatar-initials");
				if (initials) initials.hidden = false;
			});
		}
	}

	/**
	 * Wires each fully-functional SAFE action to dispatch a bubbling CustomEvent the host listens for; the host does
	 * the work (there is no built-in navigation and no new token type).  A format-invalid token is refused loud - it
	 * can only be a hand-edited attribute, since the server already format-validates it.  This selects only top-level
	 * SAFE actions (data-juneau-header-action + behavior=safe); MENU triggers are wired separately by wireMenus, and
	 * SAFE items INSIDE a menu list by wireMenuItems.  LINK actions are plain anchors needing no wiring.
	 *
	 * Idempotent per action, guarded by SAFE_WIRED_ATTR - the marker this function SHARES with initAll.  initAll is
	 * now re-entrant on purpose (a row-detail bar slot cloned from a <template> is only enhanceable after insert), so
	 * without the shared marker a second scan would bind a second handler and one click would fire twice.
	 */
	function wireSafeActions(root) {
		if (!root || !root.querySelectorAll) return;
		const actions = root.querySelectorAll("[" + ACTION_MARKER + "][" + BEHAVIOR_ATTR + "='safe']");
		for (const el of actions) {
			const token = el.getAttribute(SAFE_ATTR);
			if (!isSafeToken(token)) {
				window.console?.error?.(
					"juneau-chrome.js: refusing to wire SAFE action with malformed token '" + token + "'.");
				continue;
			}
			if (!el.addEventListener) continue;
			if (el.getAttribute(SAFE_WIRED_ATTR) === "1") continue;
			el.setAttribute(SAFE_WIRED_ATTR, "1");
			el.addEventListener("click", function (ev) {
				ev?.preventDefault?.();
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
			window.console?.error?.(
				"juneau-chrome.js: app-header contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_HEADER_CONTRACT_VERSION + "'); leaving header un-enhanced.");
			return null;
		}
		hydrateIcons(header);
		wireAvatarFallback(header);
		wireSafeActions(header);
		wireMenus(header);
		if (sidecar) applyCounts(header, envelopeBadges(sidecar));
		return { root: header, refresh: function () { return refresh(header); } };
	}

	/** Enhances one bar slot: handshake + initial-count apply (bar widgets carry no icons or controls). */
	function initBarSlot(bar) {
		const id = bar.getAttribute(BAR_SLOT_MARKER);
		const sidecar = readSidecar(BAR_SIDECAR_PREFIX, id);
		if (sidecar && !envelopeContractOk(sidecar, JUNEAU_BAR_CONTRACT_VERSION)) {
			window.console?.error?.(
				"juneau-chrome.js: bar-slot contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_BAR_CONTRACT_VERSION + "'); leaving bar slot un-enhanced.");
			return null;
		}
		if (sidecar) applyCounts(bar, envelopeBadges(sidecar));
		return { root: bar, refresh: function () { return refresh(bar); } };
	}

	/**
	 * DOMContentLoaded entry: enhance every app-header, then every bar slot (a bar id can repeat per sub-tab bar).
	 *
	 * Also the ENHANCE-ON-INSERT entry: the scan is document-wide, so a bar slot cloned from a row-detail
	 * {@code <template>} and inserted after load is picked up by simply calling this again (juneau-views.js does,
	 * once per expanded row).  Re-entrancy is safe because every binding step is marker-guarded - SAFE_WIRED_ATTR for
	 * SAFE actions (shared with wireSafeActions), AVATAR_WIRED_ATTR, MENU_WIRED_ATTR - and the remaining work
	 * (handshake, icon hydration, count apply) is idempotent by construction.  Still no poller: this only ever runs
	 * when something asks it to.
	 */
	function initAll() {
		const out = { headers: [], bars: [] };
		const headers = window.document.querySelectorAll("[" + HEADER_MARKER + "]");
		for (const header of headers) {
			const ctl = initHeader(header);
			if (ctl) out.headers.push(ctl);
		}
		const bars = window.document.querySelectorAll("[" + BAR_SLOT_MARKER + "]");
		for (const bar of bars) {
			const ctl = initBarSlot(bar);
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
		wireAvatarFallback: wireAvatarFallback,
		wireSafeActions: wireSafeActions,
		wireMenus: wireMenus,
		openMenu: openMenu,
		closeMenu: closeMenu,
		positionMenuUnderTrigger: positionMenuUnderTrigger,
		dispatchSafe: dispatchSafe,
		refresh: refresh,
		initHeader: initHeader,
		initBarSlot: initBarSlot,
		initAll: initAll
	};

	if (window.document?.readyState === "loading") {
		window.document.addEventListener("DOMContentLoaded", initAll);
	} else if (window.document) {
		initAll();
	}
})();
