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
 * On DOMContentLoaded it scans [data-juneau-card][data-juneau-card-refresh] (refreshable cards only) and, per card,
 * performs a fail-loud contract-version handshake against the baked-in JUNEAU_CARDS_CONTRACT_VERSION before wiring
 * the built-in refresh button and (when data-juneau-card-poll-ms is present) an own per-card poll loop.  A static
 * card (no refresh endpoint) is left exactly as the server rendered it.
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
		if (path.charAt(0) === "/" && path.charAt(1) === "/") return false;   // protocol-relative
		const colon = path.indexOf(":");
		const slash = path.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;   // "scheme:" before any slash (e.g. javascript:, servlet:)
		if (path === ".." || path.indexOf("../") >= 0 || path.indexOf("/..") >= 0) return false;
		return true;
	}

	/** Fail-loud handshake predicate: the refresh envelope's contractVersion must equal the baked-in expected value. */
	function envelopeContractOk(env, expected) {
		return !!env && typeof env === "object" && env.contractVersion === expected;
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
		for (let i = 0; i < slots.length; i++) {
			const slot = slots[i];
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

	/** True when the element is hidden by its own `hidden`/`display:none` or by an enclosing pages tab that is (in-page tab hiding is not document.hidden). */
	function isElementHidden(node) {
		let n = node;
		while (n && n.nodeType === 1) {
			if (n.hidden === true) return true;
			const style = n.getAttribute && n.getAttribute("style");
			if (style && /display\s*:\s*none/i.test(style)) return true;
			n = n.parentNode;
		}
		return false;
	}

	/** Renders the header status chip: an "ok" staleness age or a distinct "error" state that does NOT reset the last-success clock. */
	function renderStatus(status, state, nowMs) {
		if (!status) return;
		if (state.errored) {
			status.setAttribute("data-state", "error");
			status.textContent = "Refresh failed";
			status.hidden = false;
			return;
		}
		status.removeAttribute("data-state");
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

	/** Wires one refreshable card: contract handshake, endpoint re-check, refresh button, optional poll loop. */
	function initCard(card) {
		const refreshUrl = card.getAttribute(CARD_REFRESH_ATTR);
		if (!refreshUrl) return;                                  // static card - nothing to enhance

		const declared = card.getAttribute(CARD_CONTRACT_ATTR);
		if (declared !== JUNEAU_CARDS_CONTRACT_VERSION) {
			(window.console && console.error) && console.error(
				"juneau-cards.js: card contract mismatch (card '" + declared + "' != runtime '"
				+ JUNEAU_CARDS_CONTRACT_VERSION + "'); refusing to enhance card.");
			showCardBanner(card, "This card was built for a different runtime version and was not enhanced.");
			return;
		}
		if (!isSafeCardEndpoint(refreshUrl)) {
			(window.console && console.error) && console.error(
				"juneau-cards.js: unsafe refresh endpoint '" + refreshUrl + "'; refusing to enhance card.");
			showCardBanner(card, "This card's refresh endpoint is not a safe same-origin path; it was not enhanced.");
			return;
		}

		const body = card.querySelector("[" + CARD_BODY_ATTR + "]");
		const btn = card.querySelector("[" + CARD_REFRESH_TRIGGER_ATTR + "]");
		const status = card.querySelector("[" + CARD_STATUS_ATTR + "]");

		if (status) { status.setAttribute("role", "status"); status.setAttribute("aria-live", "polite"); }
		if (btn) {
			const labelledby = card.getAttribute("aria-labelledby");
			const titleEl = labelledby && card.querySelector ? card.querySelector("#" + labelledby) : null;
			const titleText = titleEl ? titleEl.textContent : "";
			btn.setAttribute("aria-label", titleText ? "Refresh " + titleText : "Refresh");
			const icons = window.JuneauViews && window.JuneauViews.icons;
			const glyph = icons && icons.resolveIcon ? icons.resolveIcon("refresh") : null;
			if (glyph) btn.innerHTML = glyph;                     // registry markup only - never user data
		}

		const state = { inFlight: false, lastSuccessAt: null, errored: false, pollTimer: null, tickTimer: null };

		function refresh() {
			if (state.inFlight) return;                           // coalesce concurrent clicks / poll ticks
			state.inFlight = true;
			if (btn) btn.disabled = true;
			card.setAttribute("aria-busy", "true");
			return window.fetch(refreshUrl, { method: "GET", credentials: "same-origin", cache: "no-store" })
				.then(function (r) { return r.json(); })
				.then(function (env) {
					if (!envelopeContractOk(env, JUNEAU_CARDS_CONTRACT_VERSION)) { state.errored = true; return; }
					fillCardFields(body, env.fields);
					state.lastSuccessAt = Date.now();
					state.errored = false;
				})
				.catch(function () { state.errored = true; })     // error state does NOT reset lastSuccessAt (S8)
				.then(function () {
					state.inFlight = false;
					if (btn) btn.disabled = false;
					card.removeAttribute("aria-busy");
					renderStatus(status, state, Date.now());
				});
		}

		if (btn && btn.addEventListener) btn.addEventListener("click", refresh);

		const pollAttr = card.getAttribute(CARD_POLL_ATTR);
		const intervalMs = pollAttr ? clampPollInterval(parseInt(pollAttr, 10) || 0) : 0;

		const ctl = { card: card, state: state, refresh: refresh, status: status, intervalMs: intervalMs, running: false };
		ctl.start = function () {
			if (!ctl.intervalMs || ctl.running || isElementHidden(card)) return;
			ctl.running = true;
			function schedule() {
				state.pollTimer = setTimeout(function () {
					const p = refresh();
					const after = function () { if (ctl.running) schedule(); };
					if (p && p.then) p.then(after); else after();
				}, nextPollDelay(ctl.intervalMs, Math.random()));
			}
			schedule();
			state.tickTimer = setInterval(function () { renderStatus(status, state, Date.now()); }, STALENESS_TICK_MS);
		};
		ctl.stop = function () { ctl.running = false; stopTimers(state); };
		if (intervalMs) ctl.start();
		return ctl;
	}

	/** Installs a MutationObserver on the grid so a card's poll timers stop when it is hidden/removed and restart on re-show (no juneau-pages.js hook). */
	function observeGrid(grid, controls) {
		if (typeof window.MutationObserver === "undefined") return null;
		const obs = new window.MutationObserver(function () {
			for (let i = 0; i < controls.length; i++) {
				const c = controls[i];
				if (!c || !c.intervalMs) continue;
				if (isElementHidden(c.card) || (window.document && window.document.hidden)) c.stop();
				else c.start();
			}
		});
		obs.observe(grid, { attributes: true, attributeFilter: ["hidden", "style", "class"], subtree: true, childList: true });
		return obs;
	}

	/** DOMContentLoaded entry: enhance every refreshable card, grid by grid. */
	function initAll() {
		const grids = window.document.querySelectorAll("[" + GRID_MARKER + "]");
		for (let g = 0; g < grids.length; g++) {
			const grid = grids[g];
			const cards = grid.querySelectorAll("[" + CARD_MARKER + "][" + CARD_REFRESH_ATTR + "]");
			const controls = [];
			for (let i = 0; i < cards.length; i++) {
				const ctl = initCard(cards[i]);
				if (ctl) controls.push(ctl);
			}
			if (controls.length) observeGrid(grid, controls);
		}
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
		initCard: initCard,
		observeGrid: observeGrid,
		initAll: initAll
	};

	if (window.document && window.document.readyState === "loading") {
		window.document.addEventListener("DOMContentLoaded", initAll);
	} else if (window.document) {
		initAll();
	}
})();
