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
 * juneau-dashboard.js - the console dashboard chrome (contract.dashboard).  It moves the panel and card-section card
 * hosts into a grid and a rail, mounts the rail sections as bus owners of filter:<id>, restores state once (decoders,
 * then #s= share links, then saved state), and runs the status header, Refresh, Copy link and save.  It loads after
 * juneau-console.js, whose auto-mount has already put every card host in main.jc-main, and before juneau-charts.js.
 *
 *   JuneauDashboard.section.register(name, {mount(el, spec, ctx) -> {destroy()}?, validate(payload)?})
 *   JuneauDashboard.decoder.register(name, function (location) -> {<topic>: payload} | null)
 *   window.JuneauConsoleDashboard = [["section" | "decoder", name, impl], ...]     (the pre-load queue)
 *   JuneauViews.dashboard.initial(topic) -> the restored payload for topic, or null
 *
 * All DOM is built with createElement, textContent and setAttribute.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};
	if (NS.dashboard) return;

	// The same literal as BusWiringValidator.EXTENSION_NAME (ConsoleDashboard_Test a03 pins the two).
	const EXTENSION_NAME_RE = /^[a-z][a-zA-Z0-9]*(\.[a-z][a-zA-Z0-9-]*)*$/;
	const RAIL_MIN = 200, RAIL_MAX = 480, RAIL_DEFAULT = 280, RAIL_STEP = 16;
	const MSG = {
		"E-JS-97": "%s.register: name '%s' %s",   // the shared, generic registry format; the first %s is the registry path
		"E-JS-104": "share link rejected: %s; using saved state or defaults",
		"E-JS-105": "dashboard '%s' uses unknown rail section '%s'; registered: '%s'"
	};

	const registries = { section: new Map(), decoder: new Map() };
	let restored = null;   // set once restore has run 
	let dash = null;       // contract.dashboard
	let store = null;      // pageState.page(<ns>), or a no-op scope

	function fmt(t) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return t.replace(/%s/g, () => String(args[i++]));
	}

	function message(code) {
		return code + ": " + fmt.apply(null, [MSG[code]].concat(Array.prototype.slice.call(arguments, 1)));
	}

	function make(doc, tag, cls, text) {
		const n = doc.createElement(tag);
		if (cls) n.className = cls;
		if (text != null) n.textContent = text;
		return n;
	}

	function onReady(doc, fn) {
		if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", fn);
		else fn();
	}

	// ---- registries ----

	function register(kind, name, impl) {
		const reg = registries[kind];
		if (!reg) throw new TypeError("JuneauDashboard: unknown registry '" + kind + "'");
		const fail = reason => {
			const e = new Error(message("E-JS-97", "JuneauDashboard." + kind, typeof name === "string" ? name : String(name), reason));
			e.code = "E-JS-97";
			throw e;
		};
		if (typeof name !== "string" || !EXTENSION_NAME_RE.test(name)) fail("is not a valid name");
		if (name.indexOf(".") < 0) fail("is bare");
		if (reg.has(name)) fail("is already registered");
		if (kind === "decoder") {
			if (typeof impl !== "function") fail("is not a function");
			if (restored) fail("was registered after restore ran");
		} else if (!impl || typeof impl.mount !== "function") {
			fail("has no mount function");
		}
		reg.set(name, impl);
	}

	function names(kind) {
		return Array.from(registries[kind].keys()).sort();
	}

	function pushEntry(entry) {
		if (!Array.isArray(entry) || entry.length !== 3)
			throw new TypeError("JuneauConsoleDashboard entries are [registry, name, impl] triples");
		register(entry[0], entry[1], entry[2]);
	}

	// Entries queued before this file loaded are applied one by one; a bad entry is logged and the rest still apply.
	function drainQueue() {
		const existing = Array.isArray(window.JuneauConsoleDashboard) ? window.JuneauConsoleDashboard.slice() : [];
		window.JuneauConsoleDashboard = { push: pushEntry };
		for (const entry of existing) {
			try {
				pushEntry(entry);
			} catch (e) {
				console.error("[juneau-dashboard] " + (e && e.message || e));
			}
		}
	}

	// ---- layout ----

	function clampWidth(v, fallback) {
		if (typeof v !== "number" || !isFinite(v)) return fallback;
		return Math.min(RAIL_MAX, Math.max(RAIL_MIN, Math.round(v)));
	}

	function noopScope() {
		return { get: () => null, set: () => {}, remove: () => {} };
	}

	function pageScope(d) {
		const ns = d.persist ? d.persist.ns : "dashboard." + d.id;
		return NS.pageState && typeof NS.pageState.page === "function" ? NS.pageState.page(ns) : noopScope();
	}

	function defaultLegend(s) {
		if (s.label) return s.label;
		if (s.kind === "range") return "Time range";
		if (s.kind === "search") return "Search";
		if (s.kind === "choice") return s.dim;
		return null;   // card sections show the card's own title; custom sections have no default
	}

	// Builds the section, header, rail, splitter and grid, and moves the existing card hosts into them.
	function buildLayout(doc, d) {
		const root = make(doc, "section", "jc-dash");
		root.setAttribute("data-dashboard", d.id);
		root.setAttribute("aria-label", d.title || d.id);

		const header = make(doc, "header", "jc-dash-header");
		if (d.title) header.appendChild(make(doc, "h2", "jc-dash-title", d.title));
		const status = make(doc, "div", "jc-dash-status");
		const live = make(doc, "span", "jc-dash-live");
		live.setAttribute("aria-live", "polite");
		header.appendChild(status);
		header.appendChild(live);
		root.appendChild(header);

		const body = make(doc, "div", "jc-dash-body");
		const rail = d.rail || null;
		body.setAttribute("data-rail", rail ? rail.side : "none");
		const parts = { root, header, status, live, body, rail: null, splitter: null, sections: [] };

		if (rail) {
			const railId = "jc-dash-rail-" + d.id;
			const toggle = make(doc, "button", "jc-dash-filters", "Filters");
			toggle.type = "button";
			toggle.setAttribute("aria-expanded", "false");
			toggle.setAttribute("aria-controls", railId);
			toggle.addEventListener("click", () => {
				const open = toggle.getAttribute("aria-expanded") !== "true";
				toggle.setAttribute("aria-expanded", String(open));
				if (open) body.setAttribute("data-filters-open", "");
				else body.removeAttribute("data-filters-open");
			});
			body.appendChild(toggle);

			const aside = make(doc, "aside", "jc-dash-rail");
			aside.id = railId;
			aside.setAttribute("aria-label", "Filters");
			for (const s of rail.sections || []) {
				const fs = make(doc, "fieldset", "jc-dash-section");
				const legend = defaultLegend(s);
				if (legend) fs.appendChild(make(doc, "legend", null, legend));
				if (s.kind === "card") {
					const host = doc.getElementById(s.card);
					if (!host) continue;   // hidden by its visibleWhen rule: never mounted
					fs.appendChild(host);
				}
				aside.appendChild(fs);
				parts.sections.push({ spec: s, el: fs });
			}
			body.appendChild(aside);

			const splitter = make(doc, "div", "jc-dash-splitter");
			splitter.setAttribute("role", "separator");
			splitter.setAttribute("aria-orientation", "vertical");
			splitter.setAttribute("aria-label", "Resize filters");
			splitter.setAttribute("aria-controls", railId);
			splitter.setAttribute("aria-valuemin", String(RAIL_MIN));
			splitter.setAttribute("aria-valuemax", String(RAIL_MAX));
			splitter.tabIndex = 0;
			body.appendChild(splitter);
			parts.rail = aside;
			parts.splitter = splitter;
		}

		const grid = make(doc, "div", "jc-dash-grid");
		grid.setAttribute("data-cols", String(d.grid));
		for (const p of d.panels || []) {
			const host = doc.getElementById(p.card);
			if (!host) continue;
			const cell = make(doc, "div", "jc-dash-panel");
			cell.setAttribute("data-span", String(p.span));
			cell.appendChild(host);
			grid.appendChild(cell);
		}
		body.appendChild(grid);
		root.appendChild(body);
		return parts;
	}

	// The rail width lives in the page-state scope under railWidth.  Arrow keys move 16 px and save at once; a
	// pointer drag saves on pointer-up.
	function wireSplitter(parts, side, def) {
		const splitter = parts.splitter;
		const sign = side === "right" ? -1 : 1;
		let width = clampWidth(store.get("railWidth"), def);
		function setWidth(w) {
			width = w;
			parts.body.style.setProperty("--jc-dash-rail", w + "px");
			splitter.setAttribute("aria-valuenow", String(w));
		}
		setWidth(width);
		splitter.addEventListener("keydown", e => {
			const dir = e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0;
			if (!dir) return;
			e.preventDefault();
			setWidth(clampWidth(width + dir * sign * RAIL_STEP, width));
			store.set("railWidth", width);
		});
		splitter.addEventListener("pointerdown", e => {
			if (e.button !== 0) return;
			e.preventDefault();
			const x0 = e.clientX, w0 = width;
			if (splitter.setPointerCapture) splitter.setPointerCapture(e.pointerId);
			const move = ev => setWidth(clampWidth(w0 + (ev.clientX - x0) * sign, width));
			const up = () => {
				splitter.removeEventListener("pointermove", move);
				splitter.removeEventListener("pointerup", up);
				splitter.removeEventListener("pointercancel", up);
				store.set("railWidth", width);
			};
			splitter.addEventListener("pointermove", move);
			splitter.addEventListener("pointerup", up);
			splitter.addEventListener("pointercancel", up);
		});
	}

	// ---- restore ----

	const FRAGMENT_CAP = 8192, PAYLOAD_CAP = 4096, MAX_DEPTH = 64;
	const BANNED_KEYS = ["__proto__", "constructor", "prototype"];
	const FILTER_KEYS = ["schemaVersion", "viewId", "search", "columns", "options", "start", "end"];
	const ZOOM_KEYS = ["schemaVersion", "start", "end", "startValue", "endValue"];
	const LEGEND_KEYS = ["schemaVersion", "selected"];
	const REJECT = Object.freeze({});   // sanitize's "not plain JSON" marker

	function isObject(v) {
		return v !== null && typeof v === "object" && !Array.isArray(v);
	}

	function has(o, k) {
		return Object.prototype.hasOwnProperty.call(o, k);
	}

	function clip(s) {
		return s.length > 64 ? s.slice(0, 64) + "\u2026" : s;
	}

	// Never String(v) on untrusted values: a null-prototype object has no toString and would throw.
	function describe(v) {
		if (typeof v === "string") return "'" + clip(v) + "'";
		if (typeof v === "number" || typeof v === "boolean") return String(v);
		return v === null ? "null" : typeof v;
	}

	function utf8(s) {
		return new TextEncoder().encode(s);
	}

	function b64urlEncode(bytes) {
		let bin = "";
		for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
		return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
	}

	function b64urlDecode(s) {
		if (!/^[A-Za-z0-9_-]*$/.test(s) || s.length % 4 === 1) return null;
		let bin;
		try {
			bin = atob(s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4));
		} catch (e) { // NOSONAR javascript:S2486 -- an undecodable link is reported by the caller
			return null;
		}
		const out = new Uint8Array(bin.length);
		for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
		return out;
	}

	// JSON.parse reviver for #s= links: Object.create(null) objects, and a banned key anywhere fails the whole link.
	function reviver(key, value) {
		if (BANNED_KEYS.indexOf(key) >= 0) {
			const e = new Error("banned key");
			e.banned = key;
			throw e;
		}
		if (!isObject(value)) return value;
		const o = Object.create(null);
		for (const k of Object.keys(value)) o[k] = value[k];
		return o;
	}

	function tooDeep(v, depth) {
		if (depth > MAX_DEPTH) return true;
		if (v !== null && typeof v === "object")
			for (const k of Object.keys(v))
				if (tooDeep(v[k], depth + 1)) return true;
		return false;
	}

	// Deep-copies a decoder result or saved state into Object.create(null) objects.  A banned key, a function or
	// symbol, or nesting deeper than MAX_DEPTH (which also stops a cycle) rejects the whole value.
	function sanitize(v, depth) {
		depth = depth || 0;
		if (depth > MAX_DEPTH || typeof v === "function" || typeof v === "symbol" || typeof v === "bigint") return REJECT;
		if (Array.isArray(v)) {
			const a = [];
			for (const x of v) {
				const c = sanitize(x, depth + 1);
				if (c === REJECT) return REJECT;
				a.push(c);
			}
			return a;
		}
		if (v === null || typeof v !== "object") return v;
		const o = Object.create(null);
		for (const k of Object.keys(v)) {
			if (BANNED_KEYS.indexOf(k) >= 0) return REJECT;
			const c = sanitize(v[k], depth + 1);
			if (c === REJECT) return REJECT;
			o[k] = c;
		}
		return o;
	}

	function isFilter(p) {
		return typeof p.viewId === "string"
			&& (p.search === undefined || typeof p.search === "string")
			&& (p.columns === undefined || isObject(p.columns))
			&& (p.options === undefined || isObject(p.options))
			&& (p.start === undefined || typeof p.start === "string")
			&& (p.end === undefined || typeof p.end === "string");
	}

	function isPercent(x) {
		return x === undefined || x === null || (typeof x === "number" && isFinite(x) && x >= 0 && x <= 100);
	}

	function isAxisValue(x) {
		return x === undefined || x === null || typeof x === "string" || (typeof x === "number" && isFinite(x));
	}

	function isLegend(p) {
		return isObject(p.selected) && Object.keys(p.selected).every(k => typeof p.selected[k] === "boolean");
	}

	// Validation step 1: the dashboard's own check.  Returns a copy holding only the family's known keys
	// (unknown keys are ignored), or null when the payload fails.  The owner's own check (step 2) runs at mount.
	function checkPayload(topic, v) {
		const i = typeof topic === "string" ? topic.indexOf(":") : -1;
		if (i < 1 || !isObject(v) || v.schemaVersion !== 1) return null;
		let json;
		try {
			json = JSON.stringify(v);
		} catch (e) { // NOSONAR javascript:S2486 -- an unserializable payload simply fails validation
			return null;
		}
		if (json === undefined || utf8(json).length > PAYLOAD_CAP) return null;
		const family = topic.slice(0, i);
		const keys = family === "filter" ? FILTER_KEYS : family === "zoom" ? ZOOM_KEYS : family === "legend" ? LEGEND_KEYS : null;
		if (!keys) return null;
		const p = Object.create(null);
		for (const k of keys) if (has(v, k) && v[k] !== undefined) p[k] = v[k];
		if (family === "filter") return isFilter(p) ? p : null;
		if (family === "zoom") return isPercent(p.start) && isPercent(p.end) && isAxisValue(p.startValue) && isAxisValue(p.endValue) ? p : null;
		return isLegend(p) ? p : null;
	}

	// Keeps the listed topics whose payloads pass step 1; unknown topics are ignored.
	function pickTopics(persist, map) {
		const out = Object.create(null);
		for (const topic of persist.topics) {
			if (!has(map, topic)) continue;
			const v = checkPayload(topic, map[topic]);
			if (v) out[topic] = v;
		}
		return out;
	}

	// The current value of each persisted topic, as "#s=...".  A payload over 4 KiB is left out; null when the
	// whole fragment is still over 8 KiB.
	function encodeLink(persist, current) {
		const t = {};
		for (const topic of persist.topics) {
			if (!has(current, topic)) continue;
			let json;
			try {
				json = JSON.stringify(current[topic]);
			} catch (e) { // NOSONAR javascript:S2486 -- an unserializable payload is left out of the link
				continue;
			}
			if (json === undefined || utf8(json).length > PAYLOAD_CAP) continue;
			t[topic] = current[topic];
		}
		const frag = "#s=" + b64urlEncode(utf8(JSON.stringify({ v: persist.version, ns: persist.ns, t: t })));
		return frag.length > FRAGMENT_CAP ? null : frag;
	}

	// {t} on success, {error} naming why the whole link failed.
	function decodeShareLink(hash, persist) {
		if (hash.length > FRAGMENT_CAP) return { error: "the link is over " + FRAGMENT_CAP + " characters" };
		const bytes = b64urlDecode(hash.slice(3));
		if (!bytes) return { error: "the link is not base64url" };
		let text;
		try {
			text = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
		} catch (e) { // NOSONAR javascript:S2486 -- reported as the link's error
			return { error: "the link is not UTF-8" };
		}
		let obj;
		try {
			obj = JSON.parse(text, reviver);
		} catch (e) { // NOSONAR javascript:S2486 -- reported as the link's error
			return { error: e && e.banned ? "the link carries the key '" + e.banned + "'" : "the link is not JSON" };
		}
		if (tooDeep(obj, 0)) return { error: "the link is nested more than " + MAX_DEPTH + " deep" };
		if (!isObject(obj) || !isObject(obj.t)) return { error: "the link is not a dashboard link" };
		if (obj.ns !== persist.ns) return { error: "the link is for namespace " + describe(obj.ns) + ", not '" + persist.ns + "'" };
		if (obj.v !== persist.version) return { error: "the link is version " + describe(obj.v) + ", not " + persist.version };
		return { t: obj.t };
	}

	function thrown(e) {
		return e && typeof e.message === "string" ? clip(e.message) : describe(e);
	}

	// Restore, steps 1-3, as a pure function of env {persist, hash, saved, decoders, location, warn}.
	// Returns {topics, source, deleteSaved, stripFragment}; source is "decoder:<name>", "link", "saved" or null.
	function resolveInitial(env) {
		const p = env.persist;
		const out = { topics: null, source: null, deleteSaved: false, stripFragment: typeof env.hash === "string" && env.hash.indexOf("#s=") === 0 };
		for (const name of p.decoders || []) {
			const fn = env.decoders.get(name);
			if (typeof fn !== "function") {
				env.warn(message("E-JS-104", "decoder '" + name + "' is not registered"));
				continue;
			}
			let r;
			try {
				r = fn(env.location);
			} catch (e) {
				env.warn(message("E-JS-104", "decoder '" + name + "' threw: " + thrown(e)));
				continue;
			}
			if (!isObject(r) || Object.keys(r).length === 0) continue;   // "not mine"
			let clean;
			try {
				clean = sanitize(r);
			} catch (e) {   // a throwing getter on the decoder's result counts as "not plain JSON"
				clean = REJECT;
			}
			if (clean === REJECT) {
				env.warn(message("E-JS-104", "decoder '" + name + "' returned a value that is not plain JSON"));
				continue;
			}
			out.topics = pickTopics(p, clean);
			out.source = "decoder:" + name;
			return out;
		}
		if (p.shareLink && out.stripFragment) {
			const d = decodeShareLink(env.hash, p);
			if (!d.error) {
				out.topics = pickTopics(p, d.t);
				out.source = "link";
				return out;
			}
			env.warn(message("E-JS-104", d.error));
		}
		const s = env.saved;
		if (s != null) {
			const clean = isObject(s) && s.v === p.version && isObject(s.topics) ? sanitize(s.topics) : REJECT;
			if (clean !== REJECT) {
				out.topics = pickTopics(p, clean);
				out.source = "saved";
				return out;
			}
			out.deleteSaved = true;
		}
		return out;
	}

	// Runs once: on the first initial() call (a chart mounting) or at onReady, whichever is first.
	function ensureRestored() {
		if (restored) return restored;
		restored = { topics: Object.create(null), source: null };   // set first, so a decoder calling initial() cannot recurse
		const p = dash && dash.persist;
		if (!p) return restored;
		const loc = window.location;
		const r = resolveInitial({
			persist: p, hash: String(loc.hash || ""), saved: store.get("state"), decoders: registries.decoder, location: loc,
			warn: m => console.warn("[juneau-dashboard] " + m)
		});
		if (r.deleteSaved) store.remove("state");
		if (r.stripFragment && window.history && typeof window.history.replaceState === "function")
			window.history.replaceState(window.history.state, "", loc.pathname + loc.search);
		restored = { topics: r.topics || Object.create(null), source: r.source };
		return restored;
	}

	function initial(topic) {
		const r = ensureRestored();
		return has(r.topics, topic) ? r.topics[topic] : null;
	}

	// ---- sections ----

	const MINUTE = 60000, HOUR = 3600000, DAY = 86400000;
	const SEARCH_PAUSE_MS = 250;
	const PRESET_RE = /^([1-9][0-9]{0,3})([dh])$/;

	// Quick ranges are UTC, with now floored to the minute.  Nd and Nh end at that minute; ytd starts at Jan 1.
	function quickRange(preset, nowMs) {
		const end = Math.floor(nowMs / MINUTE) * MINUTE;
		let start;
		if (preset === "ytd") {
			start = Date.UTC(new Date(end).getUTCFullYear(), 0, 1);
		} else {
			const m = PRESET_RE.exec(preset);
			if (!m) return null;
			start = end - Number(m[1]) * (m[2] === "d" ? DAY : HOUR);
		}
		return { start: new Date(start).toISOString(), end: new Date(end).toISOString() };
	}

	// A datetime-local value, read as UTC.  NaN when it is not one.
	function parseUtcLocal(v) {
		if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,3})?)?$/.test(v)) return NaN;
		const t = Date.parse(v + "Z");
		if (!isFinite(t)) return NaN;
		// Date normalizes impossible values (Feb 30, 24:00) instead of rejecting them: require the round trip to match.
		const iso = new Date(t).toISOString();
		return iso.slice(0, 16) === v.slice(0, 16) && (v.length < 19 || iso.slice(17, 19) === v.slice(17, 19)) ? t : NaN;
	}

	function toUtcLocal(iso) {
		const t = Date.parse(iso);
		return isFinite(t) ? new Date(t).toISOString().slice(0, 16) : "";
	}

	function setInvalid(input, bad) {
		if (bad) input.setAttribute("aria-invalid", "true");
		else input.removeAttribute("aria-invalid");
	}

	function utcInput(doc, box, text) {
		const label = make(doc, "label", null, text + " ");
		const input = make(doc, "input");
		input.type = "datetime-local";
		label.appendChild(input);
		box.appendChild(label);
		return input;
	}

	const RANGE = {
		validate: function (p, s) {
			const range = p.options && p.options.range;
			if (range !== "custom") return s.presets.indexOf(range) >= 0;
			const a = Date.parse(p.start), b = Date.parse(p.end);
			return s.custom === true && isFinite(a) && isFinite(b) && a < b;
		},
		mount: function (el, s, ctx) {
			const doc = el.ownerDocument;
			const group = make(doc, "div", "jc-dash-presets");
			group.setAttribute("role", "group");
			group.setAttribute("aria-label", "Quick ranges");
			const buttons = new Map();
			const press = active => buttons.forEach((b, k) => b.setAttribute("aria-pressed", String(k === active)));
			const send = (range, start, end) => ctx.publish({ schemaVersion: 1, viewId: ctx.viewId, options: { range: range }, start: start, end: end });
			let clearError = () => {};
			const choose = preset => {
				clearError();
				const r = quickRange(preset, Date.now());
				press(preset);
				send(preset, r.start, r.end);
			};
			for (const preset of s.presets) {
				const b = make(doc, "button", null, preset === "ytd" ? "YTD" : preset);
				b.type = "button";
				b.addEventListener("click", () => choose(preset));
				buttons.set(preset, b);
				group.appendChild(b);
			}
			el.appendChild(group);
			let from = null, to = null;
			if (s.custom) {
				const box = make(doc, "div");
				from = utcInput(doc, box, "From (UTC)");
				to = utcInput(doc, box, "To (UTC)");
				const apply = make(doc, "button", null, "Apply");
				apply.type = "button";
				const msg = make(doc, "p", "jc-dash-error");
				msg.setAttribute("id", "jc-dash-" + ctx.id + "-error");
				msg.setAttribute("role", "alert");
				const describe = (input, bad) => {
					setInvalid(input, bad);
					if (bad) input.setAttribute("aria-describedby", msg.getAttribute("id"));
					else input.removeAttribute("aria-describedby");
				};
				clearError = () => {
					msg.textContent = "";
					describe(from, false);
					describe(to, false);
				};
				from.addEventListener("input", clearError);
				to.addEventListener("input", clearError);
				apply.addEventListener("click", () => {
					const a = parseUtcLocal(from.value), b = parseUtcLocal(to.value);
					if (!(isFinite(a) && isFinite(b) && a < b)) {   // nothing is published
						describe(from, !isFinite(a));
						describe(to, !isFinite(b) || (isFinite(a) && a >= b));
						msg.textContent = isFinite(a) && isFinite(b) ? "Start must be before end." : "Enter a valid start and end.";
						return;
					}
					describe(from, false);
					describe(to, false);
					msg.textContent = "";
					press(null);
					send("custom", new Date(a).toISOString(), new Date(b).toISOString());
				});
				box.appendChild(apply);
				box.appendChild(msg);
				el.appendChild(box);
			}
			const init = ctx.initial;
			if (init && init.options.range === "custom") {
				press(null);
				if (from) {
					from.value = toUtcLocal(init.start);
					to.value = toUtcLocal(init.end);
				}
				send("custom", new Date(Date.parse(init.start)).toISOString(), new Date(Date.parse(init.end)).toISOString());
			} else {
				choose(init ? init.options.range : s.defaultPreset);   // a restored preset is recomputed from now
			}
		}
	};

	const SEARCH = {
		validate: function (p) {
			return p.search === undefined || typeof p.search === "string";
		},
		mount: function (el, s, ctx) {
			const doc = el.ownerDocument;
			const input = make(doc, "input");
			input.type = "search";
			if (s.placeholder) input.placeholder = s.placeholder;
			input.setAttribute("aria-label", s.label || "Search");
			let timer = null, last = null;
			const send = () => {
				timer = null;
				if (input.value === last) return;
				last = input.value;
				ctx.publish({ schemaVersion: 1, viewId: ctx.viewId, search: last });
			};
			input.addEventListener("input", () => {
				if (timer) clearTimeout(timer);
				timer = setTimeout(send, SEARCH_PAUSE_MS);
			});
			input.addEventListener("keydown", e => {
				if (e.key !== "Enter") return;
				e.preventDefault();
				if (timer) clearTimeout(timer);
				send();
			});
			el.appendChild(input);
			input.value = ctx.initial && typeof ctx.initial.search === "string" ? ctx.initial.search : "";
			send();
		}
	};

	const CHOICE = {
		validate: function (p, s) {
			if (p.options !== undefined) {
				if (!isObject(p.options)) return false;
				const d = p.options.deselected;
				if (d !== undefined && !(Array.isArray(d) && d.every(v => typeof v === "string"))) return false;
			}
			if (p.columns === undefined) return true;
			const keys = Object.keys(p.columns);
			const list = p.columns[s.dim];
			return keys.length === 1 && keys[0] === s.dim && Array.isArray(list) && list.every(v => typeof v === "string");
		},
		mount: function (el, s, ctx) {
			const doc = el.ownerDocument;
			const list = make(doc, "div", "jc-dash-choice");
			el.appendChild(list);
			let selected = null;   // null: everything is selected (no filter)
			let values = [];       // [{value, count}] by count, descending; null when the dim has no dictionary
			let showAll = false;
			let version = -1;
			let boxes = new Map(), moreBtn = null, allBtn = null;   // the rebuilt controls, so focus can follow a re-render
			// Every value seen in a ready frame, plus the unticked values restored from saved state: an unticked value that
			// is not in the frame stays listed, so it can be ticked again.
			const known = new Set();
			const init = ctx.initial && ctx.initial.columns && ctx.initial.columns[s.dim];
			if (Array.isArray(init)) {
				selected = new Set(init);
				for (const v of init) known.add(v);
				const gone = ctx.initial.options && ctx.initial.options.deselected;
				if (Array.isArray(gone)) for (const v of gone) known.add(v);
			}

			const publish = () => {
				const p = { schemaVersion: 1, viewId: ctx.viewId };
				if (selected) {
					p.columns = { [s.dim]: Array.from(selected) };
					// The unticked values, so a reload can list them and bring them back.  Capped to keep the payload small.
					const all = new Set(known);
					if (values) for (const v of values) all.add(v.value);
					const gone = Array.from(all).filter(v => !selected.has(v)).sort();
					p.options = { deselected: gone.slice(0, s.max) };
					// Over the payload size cap the list shrinks from its end, then goes; the filter itself is what must survive.
					while (p.options.deselected.length > 0 && utf8(JSON.stringify(p)).length > PAYLOAD_CAP) p.options.deselected.pop();
					if (utf8(JSON.stringify(p)).length > PAYLOAD_CAP) delete p.options;
				}
				ctx.publish(p);
			};
			const toggle = (value, on) => {
				const present = values ? values.map(v => v.value) : [];
				if (!selected) {
					// A filter starts from the frame's values; what was seen before is forgotten with the old, unfiltered view.
					selected = new Set(present);
					known.clear();
					for (const v of present) known.add(v);
				}
				if (on) selected.add(value);
				else selected.delete(value);
				for (const v of present) known.add(v);
				if (Array.from(known).every(v => selected.has(v))) selected = null;
				render();
				publish();
			};
			const selectAll = () => {
				selected = null;
				render();
				publish();
			};
			const row = (value, count, missing) => {
				const label = make(doc, "label");
				if (missing) label.setAttribute("data-missing", "");
				const box = make(doc, "input");
				box.type = "checkbox";
				box.checked = !selected || selected.has(value);
				box.addEventListener("change", () => toggle(value, box.checked));
				boxes.set(value, box);
				label.appendChild(box);
				label.appendChild(doc.createTextNode(value));
				if (!missing) label.appendChild(make(doc, "span", "jc-dash-count", count.toLocaleString("en-US")));
				else label.appendChild(make(doc, "span", "jc-dash-missing-note", " (not in data)"));
				return label;
			};
			function render() {
				// Whatever holds focus (a value's box or a button) is found again in the rebuilt controls.
				const active = doc.activeElement;
				let focusValue = null, focusMore = false, focusAll = false;
				if (active) {
					if (active === moreBtn) focusMore = true;
					else if (active === allBtn) focusAll = true;
					else boxes.forEach((b, k) => { if (b === active) focusValue = k; });
				}
				while (list.firstChild) list.removeChild(list.firstChild);
				boxes = new Map();
				moreBtn = null;
				allBtn = null;
				build();
				// The Select all button is gone once it has been used: focus falls to the first box.
				const target = focusMore ? moreBtn : focusAll ? (allBtn || boxes.values().next().value) : focusValue !== null ? boxes.get(focusValue) : null;
				if (target) target.focus();
			}
			function build() {
				if (values === null) {
					list.appendChild(make(doc, "p", null, "'" + s.dim + "' is not a dictionary column"));
					return;
				}
				const present = new Set(values.map(v => v.value));
				// With a filter in force, a ticked value that has left the frame is always listed (greyed): the user chose it.
				// An unticked one is listed under the same cap as the frame's values, so a long list of departed values stays
				// bounded.  With no filter, only the frame's values are listed.
				const kept = [], dropped = [];
				if (selected) {
					for (const v of Array.from(selected).sort()) if (!present.has(v)) kept.push(v);
					for (const v of Array.from(known).sort()) if (!present.has(v) && !selected.has(v)) dropped.push(v);
				}
				const total = values.length + dropped.length;
				for (const v of showAll ? values : values.slice(0, s.max)) list.appendChild(row(v.value, v.count, false));
				for (const v of kept) list.appendChild(row(v, 0, true));
				for (const v of showAll ? dropped : dropped.slice(0, s.max)) list.appendChild(row(v, 0, true));
				if (values.length > s.max || dropped.length > s.max) {
					const more = make(doc, "button", null, showAll ? "Show fewer" : "Show all (" + total + ")");
					more.type = "button";
					more.addEventListener("click", () => {
						showAll = !showAll;
						render();
					});
					list.appendChild(more);
					moreBtn = more;
				}
				if (selected) {
					const all = make(doc, "button", null, "Select all");
					all.type = "button";
					all.addEventListener("click", selectAll);
					list.appendChild(all);
					allBtn = all;
				}
			}
			function read(handle) {
				const frame = NS.data && typeof NS.data.get === "function" ? NS.data.get(handle) : null;
				if (!frame) return;
				const col = frame.columns && frame.columns[s.dim];
				if (!col || !Array.isArray(col.dict) || !col.codes || typeof col.codes.length !== "number") {
					values = null;
					render();
					return;
				}
				const counts = new Array(col.dict.length).fill(0);
				const n = typeof frame.rowCount === "number" ? Math.min(frame.rowCount, col.codes.length) : col.codes.length;
				for (let i = 0; i < n; i++) {
					const c = col.codes[i];
					if (c >= 0 && c < counts.length) counts[c]++;
				}
				values = col.dict.map((v, i) => ({ value: String(v), count: counts[i] })).filter(x => x.count > 0)
					.sort((a, b) => b.count - a.count || (a.value < b.value ? -1 : a.value > b.value ? 1 : 0));
				for (const x of values) known.add(x.value);
				render();
			}
			// The list updates on every new ready version.
			ctx.subscribe("data:" + s.source, h => {
				if (!h || h.status !== "ready" || h.version === version) return;
				version = h.version;
				read(h);
			});
			render();
			publish();
		}
	};

	const BUILTIN = Object.freeze({ range: RANGE, search: SEARCH, choice: CHOICE });

	// Mounts one non-card section as the bus owner of its topic.  Returns {owner, handle}, or null for an unknown
	// custom name (painted as an E-JS-105 box; the rest of the dashboard still renders).
	function mountSection(el, s, dashId) {
		const doc = el.ownerDocument;
		const impl = s.kind === "custom" ? registries.section.get(s.name) : BUILTIN[s.kind];
		if (!impl) {
			const msg = message("E-JS-105", dashId, s.kind === "custom" ? s.name : s.kind, names("section").join(", "));
			console.error("[juneau-dashboard] " + msg);
			const box = make(doc, "div", "jc-card-error", msg);
			box.setAttribute("role", "alert");
			el.appendChild(box);
			return null;
		}
		const owner = NS.bus.owner(s.id);
		const ok = p => {
			try {
				return typeof impl.validate !== "function" || impl.validate(p, s) === true;
			} catch (e) { // NOSONAR javascript:S2486 -- a validate that throws rejects the value; the owner uses its default
				return false;
			}
		};
		const restoredValue = initial(s.topic);
		const ctx = Object.freeze({
			id: s.id, topic: s.topic, viewId: dashId, args: s.args || {},
			initial: restoredValue && ok(restoredValue) ? restoredValue : null,   // validation step 2: the owner's own check
			publish: payload => owner.publish(s.topic, payload),
			subscribe: (topic, fn) => owner.subscribe(topic, fn)
		});
		return { owner: owner, handle: impl.mount(el, s, ctx) };
	}

	function mountSections(parts) {
		const mounted = [];
		for (const entry of parts.sections) {
			if (entry.spec.kind === "card") continue;   // the card publishes what it publishes
			try {
				const m = mountSection(entry.el, entry.spec, dash.id);
				if (m) mounted.push(m);
			} catch (e) {
				console.error("[juneau-dashboard] rail section '" + entry.spec.id + "' failed to mount: " + thrown(e));
			}
		}
		return mounted;
	}

	// ---- header ----

	const SEP = " \u00b7 ";
	const STATUS_TICK_MS = 30000, ANNOUNCE_MS = 5000, SAVE_MS = 500, FLASH_MS = 2000;
	const COPY_LABEL = "Copy link";
	const NOT_CACHED_TITLE = "The browser would not store this data, so it is held in memory and reloads with the page.";

	// Under 1000 as is, then "412k", then "2M" / "1.5M".
	function compact(n) {
		if (n < 1000) return String(n);
		if (n < 1e6) return Math.round(n / 1000) + "k";
		return String(Math.round(n / 1e5) / 10) + "M";
	}

	function ago(ms) {
		if (ms < MINUTE) return "just now";
		if (ms < HOUR) return Math.floor(ms / MINUTE) + " min ago";
		if (ms < DAY) return Math.floor(ms / HOUR) + " h ago";
		return Math.floor(ms / DAY) + " d ago";
	}

	function count(n) {
		return (typeof n === "number" ? n : 0).toLocaleString("en-US");
	}

	// One source's header line from its data handle.
	function statusText(h, nowMs) {
		if (!h) return { text: "Waiting for data", tone: "normal", title: null };
		if (h.status === "error") {
			const e = h.error || {};
			return { text: "Load failed: " + (e.code || "error") + (e.message ? SEP + e.message : ""), tone: "error", title: null };
		}
		const bits = [];
		let tone = "normal", title = null;
		if (h.status === "loading") {
			const p = h.progress || {};
			const loaded = typeof p.loaded === "number" ? p.loaded : 0;
			const pct = typeof p.total === "number" && p.total > 0 ? " " + Math.floor(100 * loaded / p.total) + "%" : "";
			bits.push("Loading" + pct + " (" + compact(loaded) + " rows)");
		} else {
			bits.push(count(h.rowCount) + " rows");
			const at = Date.parse(h.loadedAt);
			if (isFinite(at)) bits.push("loaded " + ago(nowMs - at));
			if (h.status === "stale") {
				bits.push("refresh failed");
				tone = "warning";
			}
		}
		if (h.truncated === true) {
			bits.push("truncated at " + compact(typeof h.truncatedAt === "number" ? h.truncatedAt : h.rowCount));
			tone = "warning";
		}
		if (h.mode === "aggregate") bits.push("aggregated");
		if (h.cache === "memory-fallback") {
			bits.push("not cached");
			title = NOT_CACHED_TITLE;
		}
		return { text: bits.join(SEP), tone: tone, title: title };
	}

	// At most one write per 5 s; the latest text wins, on the trailing edge.
	function announcer(el, now) {
		let last = -Infinity, pending = null, timer = null;
		const write = () => {
			timer = null;
			last = now();
			el.textContent = pending;
		};
		return text => {
			pending = text;
			if (timer) return;
			const wait = last + ANNOUNCE_MS - now();
			if (wait <= 0) write();
			else timer = setTimeout(write, wait);
		};
	}

	function refreshButton(doc, sources) {
		const wrap = make(doc, "div", "jc-dash-refresh");
		const main = make(doc, "button", null, "Refresh");
		main.type = "button";
		const more = make(doc, "button", null, "\u25be");
		more.type = "button";
		more.setAttribute("aria-label", "Refresh options");
		more.setAttribute("aria-haspopup", "menu");
		more.setAttribute("aria-expanded", "false");
		const menu = make(doc, "div", "jc-dash-menu");
		menu.setAttribute("role", "menu");
		menu.hidden = true;
		const clear = make(doc, "button", null, "Clear cache");
		clear.type = "button";
		clear.setAttribute("role", "menuitem");
		menu.appendChild(clear);
		const send = clearCache => {
			for (const id of sources) {
				// A cmd payload is {schemaVersion, op, options, target?}, so options is always present.
				NS.bus.publish("cmd:" + id, { schemaVersion: 1, op: "refresh", options: { clearCache: clearCache } });
			}
		};
		const open = on => {
			menu.hidden = !on;
			more.setAttribute("aria-expanded", String(on));
			if (on) clear.focus();
		};
		main.addEventListener("click", () => send(false));
		more.addEventListener("click", () => open(menu.hidden));
		clear.addEventListener("click", () => {
			open(false);
			send(true);
		});
		menu.addEventListener("keydown", e => {
			if (e.key !== "Escape") return;
			open(false);
			more.focus();
		});
		const within = n => {
			for (; n; n = n.parentNode) if (n === wrap) return true;
			return false;
		};
		// An outside press or Tab-away closes the menu; no focus move, the user has already gone elsewhere.
		if (typeof doc.addEventListener === "function")
			doc.addEventListener("pointerdown", e => {
				if (!menu.hidden && !within(e && e.target)) open(false);
			});
		wrap.addEventListener("focusout", e => {
			if (!menu.hidden && e && e.relatedTarget && !within(e.relatedTarget)) open(false);
		});
		wrap.appendChild(main);
		wrap.appendChild(more);
		wrap.appendChild(menu);
		return wrap;
	}

	// Copy link: keeps location.search, so a ?state= table link rides along.
	function copyButton(doc, persist, current, actions, live) {
		const b = make(doc, "button", null, COPY_LABEL);
		b.type = "button";
		let field = null;
		let revert = null;
		let announce = null;
		const flash = text => {
			b.textContent = text;
			if (live) {   // the button text alone is rarely announced; clearing first makes a repeat of the same text announce again
				live.textContent = "";
				if (announce) clearTimeout(announce);
				announce = setTimeout(() => {
					announce = null;
					live.textContent = text;
				}, 0);
			}
			if (revert) clearTimeout(revert);
			revert = setTimeout(() => {
				revert = null;
				b.textContent = COPY_LABEL;
			}, FLASH_MS);
		};
		const fallback = url => {
			if (field) actions.removeChild(field);
			field = make(doc, "div", "jc-dash-copy-field");
			const input = make(doc, "input");
			input.readOnly = true;
			input.value = url;
			input.setAttribute("aria-label", "Share link");
			field.appendChild(input);
			field.appendChild(make(doc, "span", null, "Press Ctrl+C / Cmd+C"));
			actions.appendChild(field);
			input.focus();
			input.select();
		};
		b.addEventListener("click", () => {
			const frag = encodeLink(persist, current);
			if (!frag) {   // never produce a link the dashboard would reject
				console.warn("[juneau-dashboard] the share link is over " + FRAGMENT_CAP + " characters; it was not copied");
				flash("Link too long");
				return;
			}
			const loc = window.location;
			const url = loc.origin + loc.pathname + loc.search + frag;
			const clip = window.navigator && window.navigator.clipboard;
			if (!clip || typeof clip.writeText !== "function") {
				fallback(url);
				return;
			}
			Promise.resolve().then(() => clip.writeText(url)).then(() => flash("Copied"), () => fallback(url));
		});
		return b;
	}

	function wireHeader(doc, parts, d, current) {
		const sources = (d.status || []).map(t => t.slice("data:".length));
		const handles = Object.create(null);
		const announce = announcer(parts.live, () => Date.now());
		const render = () => {
			while (parts.status.firstChild) parts.status.removeChild(parts.status.firstChild);
			const lines = sources.map(id => {
				const st = statusText(has(handles, id) ? handles[id] : null, Date.now());
				st.text = (sources.length > 1 ? id + ": " : "") + st.text;
				return st;
			});
			for (const l of lines) {
				const span = make(doc, "span", "jc-dash-status-line", l.text);
				span.setAttribute("data-tone", l.tone);
				if (l.title) span.setAttribute("title", l.title);
				parts.status.appendChild(span);
			}
			return lines;
		};
		if (sources.length) {
			for (const id of sources)
				NS.bus.subscribe("data:" + id, h => {
					handles[id] = h;
					announce(render().map(l => l.text).join("; "));
				});
			render();
			const tick = () => {
				render();
				setTimeout(tick, STATUS_TICK_MS);
			};
			setTimeout(tick, STATUS_TICK_MS);
		}
		const actions = make(doc, "div", "jc-dash-actions");
		if (sources.length) actions.appendChild(refreshButton(doc, sources));
		if (d.persist && d.persist.shareLink) actions.appendChild(copyButton(doc, d.persist, current, actions, parts.live));
		if (actions.firstChild) parts.header.appendChild(actions);
	}

	// The latest payload of each persisted topic, saved 500 ms after the last change.
	function wireSave(d, scope) {
		const current = Object.create(null);
		const p = d.persist;
		if (!p || !NS.bus) return current;
		let timer = null;
		const save = () => {
			timer = null;
			scope.set("state", { v: p.version, topics: Object.assign({}, current) });
		};
		// A change made just before the page goes away is saved now rather than lost with the timer.
		const flushNow = () => {
			if (!timer) return;
			clearTimeout(timer);
			save();
		};
		if (typeof window.addEventListener === "function") window.addEventListener("pagehide", flushNow);
		// Pages backgrounded on mobile browsers may never fire pagehide; hidden is the last reliable moment.
		const pageDoc = window.document;
		if (pageDoc && typeof pageDoc.addEventListener === "function")
			pageDoc.addEventListener("visibilitychange", () => {
				if (pageDoc.visibilityState === "hidden") flushNow();
			});
		for (const topic of p.topics)
			NS.bus.subscribe(topic, (payload, meta) => {
				if (meta && meta.cleared) delete current[topic];
				else current[topic] = payload;
				if (timer) clearTimeout(timer);
				timer = setTimeout(save, SAVE_MS);
			});
		return current;
	}


	function start(doc, parts) {
		ensureRestored();
		if (!NS.bus) return;   // the shell has already reported E-JS-46
		const current = wireSave(dash, store);   // first, so the sections' first values are tracked
		mountSections(parts);
		wireHeader(doc, parts, dash, current);
	}

	// ---- boot ----

	function boot() {
		const doc = window.document;
		const api = window.JuneauConsole;
		const contract = api && typeof api.contract === "function" ? api.contract() : null;
		const d = contract && contract.dashboard && typeof contract.dashboard === "object" ? contract.dashboard : null;
		if (!doc || !d) return;
		const main = doc.querySelector("main.jc-main");
		if (!main) return;
		dash = d;
		store = pageScope(d);
		const parts = buildLayout(doc, d);
		main.insertBefore(parts.root, main.firstChild);
		if (parts.splitter) wireSplitter(parts, d.rail.side, clampWidth(d.rail.width, RAIL_DEFAULT));
		// The shell registered its own onReady handlers (bus start, card wiring) when it mounted, so they run first.
		onReady(doc, () => start(doc, parts));
	}

	NS.dashboard = Object.freeze({ initial: topic => initial(topic) });

	window.JuneauDashboard = Object.freeze({
		version: "1",
		section: Object.freeze({ register: (name, impl) => register("section", name, impl) }),
		decoder: Object.freeze({ register: (name, fn) => register("decoder", name, fn) }),
		// Test seams for ConsoleDashboard_Test (dashboard.cjs); not part of the app-facing contract.
		_internals: Object.freeze({ names, clampWidth, encodeLink, decodeShareLink, sanitize, checkPayload, resolveInitial,
			quickRange, parseUtcLocal, BUILTIN, mountSection, statusText, compact, ago, announcer, wireHeader, wireSave,
			EXTENSION_NAME_RE, MSG })
	});

	drainQueue();
	boot();
})();
