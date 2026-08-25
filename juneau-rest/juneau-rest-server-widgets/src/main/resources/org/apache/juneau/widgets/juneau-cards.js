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
 * juneau-cards.js - opt-in client runtime for the Apache Juneau card-layout widget (CardGrid / Card / CardFieldList).
 *
 * A SECOND served bundle in the same sense as juneau-pages.js (namespace-scoped as window.JuneauCards, no ES import).
 * It does NOT refactor, extract from, or modify juneau-views.js; where it needs the same behavior (poll-interval
 * clamp, staleness-age label) it COPIES the small pure helper rather than importing it - the two files stay
 * independent.  Load order: juneau-icons.js -> juneau-cards.js (the refresh button's glyph is resolved from the icon
 * registry), and juneau-views.js before juneau-cards.js if the page also carries view tables that add a body type.
 *
 * On DOMContentLoaded it scans [data-juneau-card] and, for a card carrying a refresh endpoint, performs a fail-loud
 * contract-version handshake against the baked-in JUNEAU_CARDS_CONTRACT_VERSION before wiring the built-in refresh
 * button and (when data-juneau-card-poll-ms is present) an own per-card poll loop.  It is ALSO the enhancement owner
 * for a card's declared action catalog, which needs no refresh wire at all - so a static card with actions is
 * enhanced too, while a static card without them is left exactly as the server rendered it.  Card actions reuse the
 * app-header action vocabulary, so their wiring is delegated to juneau-chrome.js's helpers (which are clients of the
 * ONE shared views layer stack); this file stands up no popup owner and no layer stack of its own.
 *
 * The "PURE LOGIC LAYER" is DOM-free (plain data in, plain data out) and independently node-testable; the "DOM
 * BINDING LAYER" is the thin shim that scans, handshakes, and binds.
 */
(function () {
	"use strict";

	// Contract-version handshake: MUST equal CardFieldList.CONTRACT_VERSION on the server (the refresh-envelope
	// contract), surfaced as ViewsMixin.CARDS_CONTRACT_VERSION - deliberately NOT aliased to the VIEW_META
	// (ViewDef) contract, so a card-envelope revision can never force a view-sidecar bump and vice-versa.
	const JUNEAU_CARDS_CONTRACT_VERSION = "1";

	// DOM attribute names - MUST equal CardGridTable's constants of the same names on the server.
	const GRID_MARKER = "data-juneau-card-grid";
	const CARD_MARKER = "data-juneau-card";
	const CARD_CONTRACT_ATTR = "data-juneau-card-contract";
	const CARD_REFRESH_ATTR = "data-juneau-card-refresh";
	const CARD_POLL_ATTR = "data-juneau-card-poll-ms";
	const CARD_STATUS_ATTR = "data-juneau-card-status";
	const CARD_BANNER_ATTR = "data-juneau-card-banner";
	const CARD_BODY_ATTR = "data-juneau-card-body";
	const CARD_FIELD_ATTR = "data-juneau-card-field";
	const CARD_REFRESH_TRIGGER_ATTR = "data-juneau-card-refresh-trigger";

	/**
	 * Per-card action attributes - MUST equal AppHeaderTable's constants of the same names on the server, because a
	 * card action IS a header action: `Card.actions` reuses that vocabulary rather than minting a second one.  This
	 * runtime is the ENHANCEMENT OWNER for them (juneau-chrome.js only scans headers/bar slots at DOMContentLoaded,
	 * and a card is neither), but it owns no popup machinery: MENU wiring is delegated to the chrome helpers, which
	 * are themselves clients of the ONE shared views layer stack.
	 */
	const ACTION_MARKER = "data-juneau-header-action";

	/**
	 * The minimum honored polling interval, in milliseconds.  COPIED from juneau-views.js (which mirrors
	 * ViewDef.MIN_POLL_INTERVAL_MS / SafePathTemplate.MIN_POLL_INTERVAL_MS on the server) rather than imported - the
	 * server is the authoritative clamp (data-juneau-card-poll-ms already arrives floored); this client copy is
	 * defense-in-depth so a hand-edited attribute can't push this runtime below the floor either.
	 */
	const MIN_POLL_INTERVAL_MS = 5000;

	/** Upper bound of the random start-jitter (ms) spread across cards so an N-card dashboard avoids a synchronized poll burst. */
	const POLL_JITTER_MS = 1000;

	/** A 1s render tick keeps the staleness-age label honest between polls. */
	const STALENESS_TICK_MS = 1000;

	const NS = window.JuneauCards = window.JuneauCards || {};
	NS.CONTRACT_VERSION = JUNEAU_CARDS_CONTRACT_VERSION;

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==================================================================================================================

	/** Clamps a declared poll interval up to MIN_POLL_INTERVAL_MS (copy of the juneau-views.js helper). */
	function clampPollInterval(ms) {
		return Math.max(ms, MIN_POLL_INTERVAL_MS);
	}

	/**
	 * Formats an elapsed-time duration (ms) as a short staleness-age label ("just now", "5s ago", "2m ago",
	 * "1h ago").  Pure - the caller supplies the already-computed elapsed ms - so it is testable without faking the
	 * clock.  Copy of the juneau-views.js helper (behavior parity with the table staleness chip).
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

	/** Coerces a refresh-envelope field value to a display scalar; objects/arrays/null render as "" (never [object Object]). */
	function scalarFieldValue(v) {
		if (v == null) return "";
		const t = typeof v;
		if (t === "string") return v;
		if (t === "number" || t === "boolean") return String(v);
		return "";
	}

	/**
	 * Client-side endpoint re-check before any fetch: same-origin AND non-templated - rejects an absolute URL
	 * ("scheme://"), a protocol-relative "//host", any "scheme:" prefix, a ".." path segment, and any "{...}"
	 * template placeholder.  Matches the Java CardFieldList.validate() rule (SafePathTemplate.isNonTemplatedPath) -
	 * this is NOT the row-detail isSafeDetailUrl, which deliberately permits "{id}".
	 */
	function isSafeCardEndpoint(path) {
		if (typeof path !== "string" || path.length === 0) return false;
		if (path.indexOf("{") >= 0) return false;                 // no template placeholder (a field-list is not row-scoped)
		if (path.indexOf("://") >= 0) return false;               // absolute URL
		if (path.startsWith("//")) return false;                  // protocol-relative
		const colon = path.indexOf(":");
		const slash = path.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;   // "scheme:" before any slash (e.g. javascript:, servlet:)
		if (path === ".." || path.indexOf("../") >= 0 || path.indexOf("/..") >= 0) return false;
		return true;
	}

	/** Fail-loud handshake predicate: the refresh envelope's contractVersion must equal the baked-in expected value. */
	function envelopeContractOk(env, expected) {
		return typeof env === "object" && env?.contractVersion === expected;
	}

	/**
	 * The next poll delay for a card: the clamped interval plus a deterministic start-jitter derived from a supplied
	 * random in [0,1).  Pure (the caller supplies the random) so the stampede-avoidance spread is unit-checkable; the
	 * DOM layer calls it with Math.random().
	 */
	function nextPollDelay(intervalMs, rnd) {
		const r = typeof rnd === "number" && rnd >= 0 && rnd < 1 ? rnd : 0;
		return clampPollInterval(intervalMs) + Math.floor(r * POLL_JITTER_MS);
	}

	// ==================================================================================================================
	// DOM BINDING LAYER
	// ==================================================================================================================

	/**
	 * Fills a card body's [data-juneau-card-field] slots from a refresh envelope's `fields` map, via textContent
	 * only (never innerHTML): unknown keys are dropped, a missing key clears to "", and non-scalar values render as
	 * "".  A thin card-scoped analogue of juneau-views.js fillDetailSlots (not a reuse - different slot attribute,
	 * no render/markdown dispatch).
	 */
	function fillCardFields(body, fields) {
		if (!body || !body.querySelectorAll) return;
		const map = fields && typeof fields === "object" ? fields : {};
		const slots = body.querySelectorAll("[" + CARD_FIELD_ATTR + "]");
		for (const slot of slots) {
			const key = slot.getAttribute(CARD_FIELD_ATTR);
			slot.textContent = Object.hasOwn(map, key) ? scalarFieldValue(map[key]) : "";
		}
	}

	/** Fills a card's [data-juneau-card-banner] host (textContent, unhide) and leaves the server-rendered card otherwise as-is. */
	function showCardBanner(card, message) {
		const banner = card.querySelector("[" + CARD_BANNER_ATTR + "]");
		if (!banner) return;
		banner.textContent = message;
		banner.hidden = false;
	}

	/** True when `cls` is present in the element's space-separated class attribute (classList-free so it also works against the Node harness DOM). */
	function hasClass(n, cls) {
		if (!n || !n.getAttribute) return false;
		return (" " + (n.getAttribute("class") || "") + " ").indexOf(" " + cls + " ") >= 0;
	}

	/**
	 * True when the card is hidden by ANY of: its own `hidden` / inline `display:none`; an enclosing pages tab panel
	 * (`.jc-panel` / `.jc-subpanel`) that LACKS `.jc-active` - juneau-pages.js toggles `.jc-active` on the ancestor
	 * panel and juneau-views.css keeps an inactive panel at `display:none`, and that toggle never touches this card's
	 * own subtree; or a computed `display:none` on any ancestor (defense for other CSS-hidden cases, skipped where
	 * getComputedStyle is absent, e.g. the Node harness).  In-page tab hiding is NOT document.hidden, so it is
	 * checked here rather than via the visibilitychange path.
	 */
	function isElementHidden(node) {
		let n = node;
		while (n?.nodeType === 1) {
			if (n.hidden === true) return true;
			const style = n.getAttribute?.("style");
			if (style && /display\s*:\s*none/i.test(style)) return true;
			if ((hasClass(n, "jc-panel") || hasClass(n, "jc-subpanel")) && !hasClass(n, "jc-active")) return true;
			if (window.getComputedStyle) {
				const cs = window.getComputedStyle(n);
				if (cs?.display === "none") return true;
			}
			n = n.parentNode;
		}
		return false;
	}

	/** The ancestor pages tab panels (`.jc-panel` / `.jc-subpanel`) above a node, nearest first - the elements juneau-pages.js toggles `.jc-active` on. */
	function ancestorPanels(node) {
		const panels = [];
		let n = node ? node.parentNode : null;
		while (n?.nodeType === 1) {
			if (hasClass(n, "jc-panel") || hasClass(n, "jc-subpanel")) panels.push(n);
			n = n.parentNode;
		}
		return panels;
	}

	/** Renders the header status chip: an "ok" staleness age or a distinct "error" state that does NOT reset the last-success clock. */
	function renderStatus(status, state, nowMs) {
		if (!status) return;
		if (state.errored) {
			status.dataset.state = "error";
			status.textContent = "Refresh failed";
			status.hidden = false;
			return;
		}
		delete status.dataset.state;
		if (state.lastSuccessAt == null) {
			status.hidden = true;
			return;
		}
		status.textContent = "Updated " + formatStalenessAge(nowMs - state.lastSuccessAt);
		status.hidden = false;
	}

	/** Clears a card's poll + render-tick timers (teardown on hide/removal). */
	function stopTimers(state) {
		if (state.pollTimer != null) { clearTimeout(state.pollTimer); state.pollTimer = null; }
		if (state.tickTimer != null) { clearInterval(state.tickTimer); state.tickTimer = null; }
	}

	/**
	 * Resolves the chrome action helpers (window.JuneauChrome.init) or null when that bundle is not loaded.  This
	 * runtime deliberately implements no menu/popup logic of its own: it delegates to the helpers that already speak
	 * the shared views layer stack, so a card menu shares one z-index, one Escape depth and one light-dismiss owner
	 * with every other popup on the page.
	 */
	function chromeActions() {
		const chrome = window.JuneauChrome?.init;
		return typeof chrome?.wireMenus === "function" && typeof chrome?.wireSafeActions === "function"
			? chrome : null;
	}

	/** True when a card declares at least one action (a static, action-less card needs no enhancement at all). */
	function hasCardActions(card) {
		return !!card?.querySelector?.("[" + ACTION_MARKER + "]");
	}

	/**
	 * Enhances one card's declared actions, scoped to that card: icon hydration, SAFE host-dispatch wiring, and MENU
	 * triggers bound to the shared layer stack.  Idempotent (the chrome helpers mark what they wire).  With chrome
	 * absent, LINK actions still navigate natively and the rest stay inert rather than half-wired.
	 */
	function enhanceCardActions(card) {
		if (!hasCardActions(card)) return false;
		const chrome = chromeActions();
		if (!chrome) return false;
		if (typeof chrome.hydrateIcons === "function") chrome.hydrateIcons(card);
		chrome.wireSafeActions(card);
		chrome.wireMenus(card);
		return true;
	}

	/** Fail-loud handshake gate for one card: contract version, then the client-side endpoint re-check. */
	function cardRefreshRefused(card, refreshUrl, declared) {
		if (declared !== JUNEAU_CARDS_CONTRACT_VERSION) {
			window.console?.error?.(
				"juneau-cards.js: card contract mismatch (card '" + declared + "' != runtime '"
				+ JUNEAU_CARDS_CONTRACT_VERSION + "'); refusing to enhance card.");
			showCardBanner(card, "This card was built for a different runtime version and was not enhanced.");
			return true;
		}
		if (!isSafeCardEndpoint(refreshUrl)) {
			window.console?.error?.(
				"juneau-cards.js: unsafe refresh endpoint '" + refreshUrl + "'; refusing to enhance card.");
			showCardBanner(card, "This card's refresh endpoint is not a safe same-origin path; it was not enhanced.");
			return true;
		}
		return false;
	}

	/** Wires the refresh trigger's accessible label (from the card's titled heading) and its icon-registry glyph. */
	function wireRefreshButton(card, btn) {
		const labelledby = card.getAttribute("aria-labelledby");
		const titleEl = labelledby ? card.querySelector?.("#" + labelledby) : null;
		const titleText = titleEl ? titleEl.textContent : "";
		btn.setAttribute("aria-label", titleText ? "Refresh " + titleText : "Refresh");
		const icons = window.JuneauViews?.icons;
		const glyph = icons?.resolveIcon?.("refresh") ?? null;
		if (glyph) btn.innerHTML = glyph;                         // registry markup only - never user data
	}

	/** Applies a successful refresh envelope to a card's fields, or flags the error state on a failed handshake. */
	function applyCardRefreshEnvelope(body, state, env) {
		if (!envelopeContractOk(env, JUNEAU_CARDS_CONTRACT_VERSION)) { state.errored = true; return; }
		fillCardFields(body, env.fields);
		state.lastSuccessAt = Date.now();
		state.errored = false;
	}

	/** Finalizes a refresh attempt (success or failure alike): clears in-flight/busy state, repaints the status chip. */
	function finishCardRefresh(card, btn, status, state) {
		state.inFlight = false;
		if (btn) btn.disabled = false;
		card.removeAttribute("aria-busy");
		renderStatus(status, state, Date.now());
	}

	/** Performs one refresh fetch for a card, applying the envelope on success and flagging state.errored on any failure. */
	function fetchCardRefresh(refreshUrl, card, btn, body, status, state) {
		return window.fetch(refreshUrl, { method: "GET", credentials: "same-origin", cache: "no-store" })
			.then(function (r) { return r.json(); })
			.then(function (env) { applyCardRefreshEnvelope(body, state, env); })
			.catch(function () { state.errored = true; })         // error state does NOT reset lastSuccessAt (S8)
			.then(function () { finishCardRefresh(card, btn, status, state); });
	}

	/** Re-arms a card's poll loop for another tick, unless ctl.stop() has since flipped ctl.running off. */
	function continuePolling(ctl) {
		if (ctl.running) schedulePoll(ctl);
	}

	/** Runs one poll tick's refresh, then re-arms the loop once that refresh settles. */
	function runPollTick(ctl) {
		ctl.refresh().then(function () { continuePolling(ctl); });
	}

	/** Arms a card's next poll tick after its own jittered delay (nextPollDelay's Math.random() is UI polling jitter
	 *  to stagger an N-card dashboard's refreshes - not security/crypto-sensitive). */
	function schedulePoll(ctl) {
		ctl.state.pollTimer = setTimeout(function () { runPollTick(ctl); }, nextPollDelay(ctl.intervalMs, Math.random()));
	}

	/** Wires one refreshable card: contract handshake, endpoint re-check, refresh button, optional poll loop. */
	function initCard(card) {
		const refreshUrl = card.getAttribute(CARD_REFRESH_ATTR);
		if (!refreshUrl) return;                                  // static card - nothing to enhance

		const declared = card.getAttribute(CARD_CONTRACT_ATTR);
		if (cardRefreshRefused(card, refreshUrl, declared)) return;

		const body = card.querySelector("[" + CARD_BODY_ATTR + "]");
		const btn = card.querySelector("[" + CARD_REFRESH_TRIGGER_ATTR + "]");
		const status = card.querySelector("[" + CARD_STATUS_ATTR + "]");

		if (status) { status.setAttribute("role", "status"); status.setAttribute("aria-live", "polite"); }
		if (btn) wireRefreshButton(card, btn);

		const state = { inFlight: false, lastSuccessAt: null, errored: false, pollTimer: null, tickTimer: null };

		function refresh() {
			if (state.inFlight) return Promise.resolve();         // coalesce concurrent clicks / poll ticks
			state.inFlight = true;
			if (btn) btn.disabled = true;
			card.setAttribute("aria-busy", "true");
			return fetchCardRefresh(refreshUrl, card, btn, body, status, state);
		}

		btn?.addEventListener?.("click", refresh);

		const pollAttr = card.getAttribute(CARD_POLL_ATTR);
		const intervalMs = pollAttr ? clampPollInterval(Number.parseInt(pollAttr, 10) || 0) : 0;

		const ctl = { card: card, state: state, refresh: refresh, status: status, intervalMs: intervalMs, running: false };
		ctl.start = function () {
			if (!ctl.intervalMs || ctl.running || isElementHidden(card)) return;
			ctl.running = true;
			schedulePoll(ctl);
			state.tickTimer = setInterval(function () { renderStatus(status, state, Date.now()); }, STALENESS_TICK_MS);
		};
		ctl.stop = function () { ctl.running = false; stopTimers(state); };
		if (intervalMs) ctl.start();
		return ctl;
	}

	/**
	 * Installs a MutationObserver so a card's poll timers stop when it is hidden/removed and restart on re-show (no
	 * juneau-pages.js hook).  It watches the grid subtree (own `hidden` / `style` / `class`) AND every ancestor pages
	 * tab panel's `class`: a pages tab hide toggles `.jc-active` on an ancestor `.jc-panel` / `.jc-subpanel` that sits
	 * ABOVE the grid, so a grid-subtree-only observer would never see it.  With no panel ancestor the grid watch is the
	 * sole fallback (a card hidden by its own `hidden` / inline `style`).
	 */
	function observeGrid(grid, controls) {
		if (window.MutationObserver === undefined) return null;
		const obs = new window.MutationObserver(function () {
			for (const c of controls) {
				if (!c || !c.intervalMs) continue;
				if (isElementHidden(c.card) || window.document?.hidden) c.stop();
				else c.start();
			}
		});
		obs.observe(grid, { attributes: true, attributeFilter: ["hidden", "style", "class"], subtree: true, childList: true });
		const panels = ancestorPanels(grid);
		for (const panel of panels)
			obs.observe(panel, { attributes: true, attributeFilter: ["class"] });
		return obs;
	}

	/** Resolves (creating if absent) the group record for a card's enclosing grid, or the card itself when grid-less. */
	function groupFor(card, groups) {
		const grid = typeof card.closest === "function" ? card.closest("[" + GRID_MARKER + "]") : null;
		const root = grid || card;
		for (const group of groups)
			if (group.root === root) return group;
		const g = { root: root, controls: [] };
		groups.push(g);
		return g;
	}

	/** Enhances one card (refresh handshake + declared actions); a refreshable card also joins its group's controls. */
	function enhanceOneCard(card, groups) {
		const refreshable = !!card.getAttribute(CARD_REFRESH_ATTR);
		const ctl = refreshable ? initCard(card) : null;
		if (refreshable && !ctl) return;                          // refused at the handshake - enhance nothing
		enhanceCardActions(card);
		if (ctl) groupFor(card, groups).controls.push(ctl);
	}

	/**
	 * DOMContentLoaded entry: enhance EVERY card, then install the poll observer for the refreshable ones.
	 *
	 * <p>The scan is per-card rather than per-grid because neither prerequisite of the old scan holds any more: a
	 * declared action catalog needs no refresh wire (so an action-only card must be enhanced too), and a card can be
	 * served on its own without a grid shell.  A refreshable card that fails its handshake is left entirely
	 * un-enhanced, actions included - it was built by a different runtime.  Cards are grouped by their enclosing
	 * grid (or, grid-less, by themselves) so the observer still watches the shell the pages runtime toggles.
	 */
	function initAll() {
		const cards = window.document.querySelectorAll("[" + CARD_MARKER + "]");
		const groups = [];

		for (const card of cards)
			enhanceOneCard(card, groups);

		for (const group of groups)
			if (group.controls.length) observeGrid(group.root, group.controls);
	}

	NS.init = {
		JUNEAU_CARDS_CONTRACT_VERSION: JUNEAU_CARDS_CONTRACT_VERSION,
		MIN_POLL_INTERVAL_MS: MIN_POLL_INTERVAL_MS,
		clampPollInterval: clampPollInterval,
		formatStalenessAge: formatStalenessAge,
		scalarFieldValue: scalarFieldValue,
		isSafeCardEndpoint: isSafeCardEndpoint,
		envelopeContractOk: envelopeContractOk,
		nextPollDelay: nextPollDelay,
		fillCardFields: fillCardFields,
		showCardBanner: showCardBanner,
		isElementHidden: isElementHidden,
		renderStatus: renderStatus,
		hasCardActions: hasCardActions,
		enhanceCardActions: enhanceCardActions,
		initCard: initCard,
		observeGrid: observeGrid,
		initAll: initAll
	};

	if (window.document?.readyState === "loading") {
		window.document.addEventListener("DOMContentLoaded", initAll);
	} else if (window.document) {
		initAll();
	}
})();
