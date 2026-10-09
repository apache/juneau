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

/**
 * Juneau console count badges. Mounts the `header.badges[]` entries of a page contract (see
 * juneau-page.schema.json): each badge polls a same-origin JSON endpoint returning {total, mine?, items?[]},
 * renders a labelled count, hides itself at zero, and lists the first items in a popover.
 *
 * Badges are presentation only: the endpoint must still authorize the caller. A 401/403 removes the badge and
 * stops polling for it; any other failure marks it stale and retries with exponential backoff.
 *
 * @example
 * JuneauConsoleBadges.mount([{ id: 'pending', src: '/rest/changes/pending', href: '/ui/changes', refreshMs: 60000 }]);
 *
 * Error codes: E-JS-65 (fetch failed, or 401/403), E-JS-66 (response has no numeric `total`).
 */
(function () {
	"use strict";

	const NS = window.JuneauConsoleBadges = window.JuneauConsoleBadges || {};

	const MIN_REFRESH_MS = 5000;
	const DEFAULT_REFRESH_MS = 60000;
	const DEFAULT_TOOLTIP_MAX = 10;
	const MAX_BACKOFF_MS = 5 * 60 * 1000;
	const KNOWN_OPS = ["eq", "ne", "present", "absent", "in", "contains"];
	// The contract's `danger` tone paints with the shared `error` status tone; the other two names match.
	const TONE_TOKEN = { info: "info", warning: "warning", danger: "error" };

	function fmt(msg, ...args) {
		let i = 0;
		return msg.replace(/%s/g, () => String(args[i++]));
	}

	function logError(code, msg, ...args) {
		console.error("[juneau-badges] " + code + ": " + fmt(msg, ...args));
	}

	let mounted = {}; // badge id -> state

	// JuneauViews.bus wrap: badge:<id> on every successful poll, cmd:<cardId> {op:'refresh'} on a drain.  The bus is
	// resolved lazily, so script order cannot matter; without it, render/detectDrain behave exactly as before.
	function badgeOwner(st) {
		const bus = window.JuneauViews && window.JuneauViews.bus;
		if (!bus || st.busOff) return null;
		if (!st.owner) {
			const owner = bus.owner("badge:" + st.def.id);
			// Another owner already holds badge:<id> (the bus reports the E-JS-49): the wrap steps aside.
			try { owner.claim("badge:" + st.def.id); } catch { st.busOff = true; return null; }
			st.owner = owner;
		}
		return st.owner;
	}

	function disposeOwner(st) {
		if (st.owner) st.owner.dispose();
		st.owner = null;
	}

	/**
	 * Supplies the page context a badge is scoped and gated by: `{activeNav: [ids], tableTab: {cardId: tabId},
	 * facts: {...}}`. The default reads the mounted JuneauConsole contract and the `?state=` tab.
	 */
	function defaultContext() {
		const jc = window.JuneauConsole;
		const contract = jc && typeof jc.contract === "function" ? jc.contract() : null;
		const ctx = { activeNav: (contract && contract.activeNav) || [], facts: (contract && contract.facts) || {}, tableTab: {} };
		const urlState = window.JuneauViews && window.JuneauViews.urlState;
		const loc = window.location;
		if (urlState && typeof urlState.decode === "function" && loc && loc.search) {
			const raw = new window.URLSearchParams(loc.search).get("state");
			const tab = raw ? urlState.decode(raw).tab : null;
			if (tab) ctx.tableTab = new Proxy({}, { get: () => tab });
		}
		return ctx;
	}

	let contextProvider = defaultContext;
	NS.setContextProvider = function (fn) { contextProvider = typeof fn === "function" ? fn : defaultContext; };

	/**
	 * Mounts badge definitions (the `header.badges[]` wire shape).
	 * @param {Array<Object>} defs
	 */
	NS.mount = function (defs) {
		for (const def of defs || []) mountOne(def);
	};

	/** Unmounts every badge and stops polling. */
	NS.unmountAll = function () {
		for (const id of Object.keys(mounted)) {
			const st = mounted[id];
			if (st.timer) clearTimeout(st.timer);
			disposeOwner(st);
			closeTooltip(st);
			if (st.el && st.el.parentNode) st.el.parentNode.removeChild(st.el);
		}
		mounted = {};
	};

	// ---- visibleWhen (same semantics as the shell's card visibility: a missing fact fails closed except `absent`) ----

	function factAt(facts, path) {
		let cur = facts;
		for (const part of String(path || "").split(".")) {
			if (cur == null || typeof cur !== "object" || !(part in cur)) return { missing: true };
			cur = cur[part];
		}
		return { value: cur };
	}

	function ruleMatches(rule, facts) {
		const f = factAt(facts, rule && rule.field);
		const blank = f.missing || f.value == null || (typeof f.value === "string" && f.value.trim() === "");
		switch (rule && rule.op) {
			case "eq": return !f.missing && f.value === rule.value;
			case "ne": return !f.missing && f.value !== rule.value;
			case "present": return !blank;
			case "absent": return blank;
			case "in": return !f.missing && Array.isArray(rule.value) && rule.value.includes(f.value);
			case "contains": return !f.missing && Array.isArray(f.value) && f.value.includes(rule.value);
			default:
				console.error("[juneau-badges] " + fmt("visibleWhen rule on field '%s' has unknown op '%s' (known: '%s')",
					rule && rule.field, rule && rule.op, KNOWN_OPS.join(", ")));
				return false;
		}
	}

	function visible(def) {
		if (!def.visibleWhen) return true;
		const rules = Array.isArray(def.visibleWhen) ? def.visibleWhen : [def.visibleWhen];
		const facts = (contextProvider() || {}).facts || {};
		return rules.every(r => ruleMatches(r, facts));
	}

	// ---- polling ----

	function mountOne(def) {
		if (!def || !def.id || mounted[def.id]) return;
		if (!visible(def)) return;
		const st = { def: def, seenIds: null, backoffMs: 0, timer: null, el: null, popover: null, lastTotal: null, lastBody: null };
		mounted[def.id] = st;
		poll(st);
	}

	function resolveScope(def) {
		if (!def.scope) return {};
		const ctx = contextProvider() || {};
		const by = def.scope.by || "nav";
		const key = by === "view"
			? (ctx.tableTab && ctx.tableTab[def.table])
			: (ctx.activeNav && ctx.activeNav[ctx.activeNav.length - 1]);
		if (def.scope.values) {
			const vals = key != null ? def.scope.values[key] : null;
			if (!vals || !vals.length) return null; // no scope values for this page: hide, keep polling
			return { param: def.scope.param, values: vals };
		}
		return {};
	}

	function enc(v) { return encodeURIComponent(String(v)); }

	function pollUrl(def, scope) {
		const params = [];
		if (def.params) for (const k of Object.keys(def.params)) params.push(enc(k) + "=" + enc(def.params[k]));
		if (scope && scope.param) params.push(enc(scope.param) + "=" + scope.values.map(enc).join(","));
		return params.length ? def.src + (def.src.indexOf("?") >= 0 ? "&" : "?") + params.join("&") : def.src;
	}

	function interval(def) { return Math.max(def.refreshMs || DEFAULT_REFRESH_MS, MIN_REFRESH_MS); }

	function scheduleNext(st, ms) {
		if (st.timer) clearTimeout(st.timer);
		st.timer = setTimeout(() => poll(st), Math.max(ms, MIN_REFRESH_MS));
	}

	function backoffAndRetry(st) {
		st.backoffMs = st.backoffMs ? Math.min(st.backoffMs * 2, MAX_BACKOFF_MS) : interval(st.def);
		scheduleNext(st, st.backoffMs);
	}

	function poll(st) {
		if (mounted[st.def.id] !== st) return;
		const def = st.def;
		const scope = resolveScope(def);
		if (scope === null) {
			if (st.el) st.el.hidden = true;
			scheduleNext(st, interval(def));
			return;
		}
		fetch(pollUrl(def, scope), { cache: "no-store", credentials: "same-origin" }).then(res => {
			if (res.status === 401 || res.status === 403) {
				logError("E-JS-65", "badge '%s' fetch failed: '%s'", def.id, res.status);
				removeBadge(st);
				return undefined;
			}
			return res.json().then(body => {
				if (mounted[def.id] !== st) return;
				if (!body || typeof body.total !== "number") {
					logError("E-JS-66", "badge '%s' response has no numeric 'total'", def.id);
					markStale(st);
					backoffAndRetry(st);
					return;
				}
				st.backoffMs = 0;
				render(st, body);
				const owner = badgeOwner(st);
				if (owner) owner.publish("badge:" + def.id, { schemaVersion: 1, badgeId: def.id, total: body.total });
				detectDrain(st, body);
				scheduleNext(st, interval(def));
			});
		}).catch(err => {
			if (mounted[def.id] !== st) return;
			logError("E-JS-65", "badge '%s' fetch failed: '%s'", def.id, err && err.message);
			markStale(st);
			backoffAndRetry(st);
		});
	}

	// ---- rendering ----

	function findHost(def) {
		const doc = window.document;
		if (def.placement === "toolbar" && def.table) {
			const card = doc.getElementById(def.table);
			return card ? (card.querySelector(".jc-ribbon") || card) : null;
		}
		let host = doc.querySelector(".jc-header-badges");
		if (!host) {
			const parent = doc.querySelector(".jc-header-actions") || doc.querySelector(".jc-header");
			if (parent) {
				host = doc.createElement("div");
				host.className = "jc-header-badges";
				parent.appendChild(host);
			}
		}
		return host;
	}

	function ensureEl(st) {
		if (st.el) return st.el;
		const def = st.def;
		const doc = window.document;
		const el = doc.createElement("button");
		el.type = "button";
		el.className = "jc-count-badge";
		el.setAttribute("role", "status");
		el.setAttribute("aria-live", "polite");
		el.setAttribute("data-juneau-badge-tone", TONE_TOKEN[def.tone] || "warning");
		el.setAttribute("data-juneau-badge-id", def.id);
		el.hidden = true;
		el.addEventListener("click", () => onClick(st));
		el.addEventListener("mouseenter", () => openTooltip(st));
		el.addEventListener("focus", () => openTooltip(st));
		el.addEventListener("mouseleave", () => closeTooltip(st));
		el.addEventListener("blur", () => closeTooltip(st));
		const host = findHost(def);
		if (host) host.appendChild(el);
		st.el = el;
		return el;
	}

	function prefersReducedMotion() {
		return !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
	}

	function render(st, body) {
		const el = ensureEl(st);
		const label = st.def.label || {};
		const prev = st.lastTotal;
		st.lastTotal = body.total;
		st.lastBody = body;
		el.classList.remove("jc-count-badge--stale");
		el.removeAttribute("aria-label");
		if (body.total === 0) {
			el.hidden = true;
			return;
		}
		el.hidden = false;
		el.setAttribute("data-jc-total", String(body.total));
		const tmpl = body.total === 1 ? (label.one || "{total} change pending") : (label.other || "{total} changes pending");
		let text = tmpl.replace(/\{total\}/g, body.total);
		if (body.mine > 0) text += (label.mine || " ({mine} yours)").replace(/\{mine\}/g, body.mine);
		el.textContent = text;
		if (prev != null && body.total > prev) {
			el.classList.remove("jc-count-badge--pulse");
			if (!prefersReducedMotion()) el.classList.add("jc-count-badge--pulse");
		}
	}

	function markStale(st) {
		const el = ensureEl(st);
		el.hidden = false;
		el.classList.add("jc-count-badge--stale");
		el.textContent = "!";
		el.setAttribute("aria-label", "Could not refresh " + st.def.id + " (retrying)");
	}

	function removeBadge(st) {
		if (st.timer) clearTimeout(st.timer);
		disposeOwner(st);
		closeTooltip(st);
		if (st.el && st.el.parentNode) st.el.parentNode.removeChild(st.el);
		if (mounted[st.def.id] === st) delete mounted[st.def.id];
	}

	// ---- drain detection: an item that left the response means linked cards are stale ----

	function reloadCard(cardId) {
		const jc = window.JuneauConsole;
		try {
			if (jc && typeof jc.refreshCard === "function") {
				const p = jc.refreshCard(cardId);
				if (p && typeof p.catch === "function") p.catch(() => {});
				return;
			}
		} catch (e) {
			// card not mounted on this page: nothing to refresh
			return;
		}
		const card = window.JuneauViews && window.JuneauViews.card;
		if (card && typeof card.reload === "function") card.reload(cardId);
	}

	function detectDrain(st, body) {
		const ids = (body.items || []).map(it => it.id);
		if (st.seenIds && st.def.refreshes && st.seenIds.some(id => ids.indexOf(id) < 0)) {
			const owner = badgeOwner(st);
			for (const cardId of st.def.refreshes) {
				if (owner) owner.publish("cmd:" + cardId, { schemaVersion: 1, op: "refresh" });
				else reloadCard(cardId);
			}
		}
		st.seenIds = ids;
	}

	// ---- tooltip popover ----

	function openTooltip(st) {
		const body = st.lastBody;
		if (!body || !body.items || !body.items.length || !st.el || st.el.hidden) return;
		closeTooltip(st);
		const doc = window.document;
		const def = st.def;
		const max = def.tooltipMax != null ? Math.max(0, Math.min(50, def.tooltipMax)) : DEFAULT_TOOLTIP_MAX;
		if (max === 0) return;
		const pop = doc.createElement("div");
		pop.className = "jc-count-badge-popover";
		pop.id = "jc-count-badge-pop-" + def.id;
		pop.setAttribute("role", "tooltip");
		const shown = body.items.slice(0, max);
		for (const it of shown) {
			const row = doc.createElement(it.href ? "a" : "div");
			row.className = "jc-count-badge-popover-item" + (it.mine ? " jc-count-badge-popover-item--mine" : "");
			if (it.href) row.setAttribute("href", it.href);
			row.textContent = it.label || it.id;
			pop.appendChild(row);
		}
		if (body.items.length > shown.length) {
			const more = doc.createElement("div");
			more.className = "jc-count-badge-popover-more";
			more.textContent = "and " + (body.items.length - shown.length) + " more";
			pop.appendChild(more);
		}
		doc.body.appendChild(pop);
		st.el.setAttribute("aria-describedby", pop.id);
		st.popover = pop;
	}

	function closeTooltip(st) {
		if (st.popover && st.popover.parentNode) st.popover.parentNode.removeChild(st.popover);
		st.popover = null;
		if (st.el) st.el.removeAttribute("aria-describedby");
	}

	window.document.addEventListener("keydown", ev => {
		if (ev.key !== "Escape") return;
		for (const id of Object.keys(mounted)) closeTooltip(mounted[id]);
	});

	// ---- click navigation ----

	function onClick(st) {
		const def = st.def;
		if (!def.href) return;
		const scope = resolveScope(def);
		let dest = def.href;
		const urlState = window.JuneauViews && window.JuneauViews.urlState;
		if (def.stateFilter && scope && scope.values && scope.values.length && urlState && typeof urlState.encode === "function") {
			const expr = scope.values.length === 1
				? "$eq(" + scope.values[0] + ")"
				: "$in(" + scope.values.join(",") + ")";
			const state = urlState.encode({ filters: [{ column: def.stateFilter.column || def.stateFilter, expr: expr }] });
			dest = def.href + (def.href.indexOf("?") >= 0 ? "&" : "?") + "state=" + enc(state);
		}
		window.location.assign(dest);
	}

	// ---- auto-mount from the console contract ----

	/** Mounts `header.badges` of the contract the console shell has mounted, if any. */
	NS.mountFromContract = function () {
		const jc = window.JuneauConsole;
		const contract = jc && typeof jc.contract === "function" ? jc.contract() : null;
		const defs = contract && contract.header && contract.header.badges;
		if (defs && defs.length) NS.mount(defs);
	};

	NS.mountFromContract();
})();
