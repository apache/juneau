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
 * juneau-renders.js - dependency-free cell-renderer registry for the Apache Juneau rich-view toolkit.
 *
 * A pure, library-free registry mapping render ids (per the VIEW_META §6.6 grammar) to cell-render functions.  Each
 * renderer is an object with up to five orthogonal facets DataTables asks a column for:
 *
 *     { display(cellData, rowData, meta), sort(...), filter(...), type(...), class(cellData, rowData, meta) }
 *
 * In the MVP SERVER data mode only `display` + `class` are live - the server does sort/filter/type via
 * DataTablesQueryProtocol, so those facets are inert (they become live only in client-side mode).  The registry
 * contract still defines all four for forward-compatibility.
 *
 * Everything in the "PURE LOGIC LAYER" below is a pure (cellData, rowData, meta) -> string/class function with NO
 * dependency on jQuery, DataTables, or the DOM - so it is unit-checkable (Option B: jsdom + node:test) and
 * Option-B-portable without pulling in the caller-provided libs.  Generic renderers use only built-in
 * Intl.DateTimeFormat / Date - NO date library (no moment/dayjs/luxon), bundled or caller-provided.
 *
 * The `tag` renderer emits the shared `.tag.<domain>.<value>` class-name contract that console-ui OWNS; this module
 * ships only the dependency-free base `.tag` chip (juneau-views.css, neutral/no colors) and takes NO dependency on
 * console-ui - its palette themes the same classes when present.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables)
	// ==================================================================================================================

	const registry = NS._renderers = NS._renderers || {};

	/**
	 * Registers (or overrides) a renderer under `id`.  `def` is `{display?, sort?, filter?, type?, class?}`; a bare
	 * function is treated as the `display` facet.  Apps extend the registry via window.JuneauViews.registerRenderer.
	 */
	function registerRenderer(id, def) {
		registry[id] = (typeof def === "function") ? { display: def } : (def || {});
		return registry[id];
	}

	/** Looks up a renderer by id; returns null when unknown (callers warn + fall back to the raw value). */
	function resolveRenderer(id) {
		return Object.hasOwn(registry, id) ? registry[id] : null;
	}

	/**
	 * Parses a render reference into the canonical `{id, meta?}` form (VIEW_META §6.6).  Accepts either the already
	 * canonical object, or the `"id:field"` string sugar where everything after the FIRST colon becomes meta.field.
	 */
	function parseRenderId(spec) {
		if (spec == null) return null;
		if (typeof spec === "object") return spec;
		const i = spec.indexOf(":");
		if (i < 0) return { id: spec };
		return { id: spec.substring(0, i), meta: { field: spec.substring(i + 1) } };
	}

	// --- escaping helpers (pure) ---------------------------------------------------------------------------------

	function escHtml(s) {
		return String(s)
			.replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll('"', "&quot;")
			.replaceAll("'", "&#39;");
	}

	function escAttr(s) {
		return escHtml(s);
	}

	/** Interpolates a `{property}` URL template against a row, URL-encoding each substituted value. */
	function interpolateHref(template, rowData) {
		return String(template).replace(/\{([^}]+)\}/g, function (m, key) {
			const v = rowData ? rowData[key] : undefined;
			return v == null ? "" : encodeURIComponent(String(v));
		});
	}

	/**
	 * Normalizes a raw string to the `.tag.<domain>.<value>` CSS token, mirroring console-ui's
	 * `org.apache.juneau.rest.server.console.Tag#normalize` lowercasing step exactly: plain (locale-insensitive)
	 * lowercasing, equivalent to Java's `raw.toLowerCase(Locale.ROOT)`.  `Tag#normalize` then fail-closed REJECTs
	 * (`IllegalArgumentException`) unless the lowercased result full-string-matches `^[a-z0-9_-]+$`; this renderer
	 * cannot reject a data value outright (a cell must always render *something*), so instead of throwing, any
	 * run of characters outside `[a-z0-9_-]` (e.g. whitespace) collapses to a single "-", with leading/trailing
	 * hyphens trimmed - keeping the emitted token inside the same `^[a-z0-9_-]+$` shape the server enforces.
	 * KEEP THIS IN SYNC WITH `Tag#normalize` / `TagHtmlRender#getContent`'s token algorithm.
	 */
	function normalizeTagToken(raw) {
		return String(raw)
			.toLowerCase()
			.replace(/[^a-z0-9_-]+/g, "-")
			.replace(/^-+|-+$/g, "");
	}

	/** Coerces a cell value to a Date, accepting epoch millis, numeric strings, and ISO/parseable date strings. */
	function toDate(cellData) {
		if (cellData == null || cellData === "") return null;
		if (cellData instanceof Date) return Number.isNaN(cellData.getTime()) ? null : cellData;
		if (typeof cellData === "number") { const dn = new Date(cellData); return Number.isNaN(dn.getTime()) ? null : dn; }
		const s = String(cellData);
		if (/^\d+$/.test(s)) { const de = new Date(Number(s)); return Number.isNaN(de.getTime()) ? null : de; }
		const d = new Date(s);
		return Number.isNaN(d.getTime()) ? null : d;
	}

	const CALIFORNIA_TZ = "America/Los_Angeles";

	/**
	 * UTC cell text: {@code MM/DD/YYYY HH:MMZ} (24-hour, seconds dropped, Z glued to the minutes).  The date/time
	 * gap is U+00A0 so the value does not wrap mid-cell.
	 */
	function formatUtcZulu(d) {
		const utc = new Intl.DateTimeFormat("en-US", {
			year: "numeric", month: "2-digit", day: "2-digit",
			hour: "2-digit", minute: "2-digit", hour12: false, timeZone: "UTC"
		}).format(d);
		return utc.replace(", ", "\u00a0") + "Z";
	}

	/**
	 * Browser-local companion: {@code h:mm am|pm} when the local calendar date matches UTC, otherwise
	 * {@code MM/DD/YYYY h:mm am|pm} so a day-rollover is not ambiguous.
	 */
	function formatLocalTime(d) {
		const time = new Intl.DateTimeFormat("en-US", {
			hour: "numeric", minute: "2-digit", hour12: true
		}).format(d).toLowerCase();
		const dateOpts = { year: "numeric", month: "2-digit", day: "2-digit" };
		const localDate = new Intl.DateTimeFormat("en-US", dateOpts).format(d);
		const utcDate = new Intl.DateTimeFormat("en-US", { ...dateOpts, timeZone: "UTC" }).format(d);
		return localDate === utcDate ? time : localDate + " " + time;
	}

	/**
	 * California ({@code America/Los_Angeles}) text: {@code MM/DD/YYYY hh:mm am|pm} plus a DST-correct
	 * {@code PDT}/{@code PST} suffix.  Hour is zero-padded ({@code 01:11 pm}).
	 */
	function formatCalifornia(d) {
		const base = new Intl.DateTimeFormat("en-US", {
			year: "numeric", month: "2-digit", day: "2-digit",
			hour: "2-digit", minute: "2-digit", hour12: true, timeZone: CALIFORNIA_TZ
		}).format(d).replace(", ", "\u00a0").toLowerCase();
		const tzPart = new Intl.DateTimeFormat("en-US", { timeZone: CALIFORNIA_TZ, timeZoneName: "short" })
			.formatToParts(d).find(function (p) { return p.type === "timeZoneName"; });
		return tzPart ? base + "\u00a0" + tzPart.value : base;
	}

	function popupLines(d) {
		return { local: "Local time: " + formatLocalTime(d), california: "California: " + formatCalifornia(d) };
	}

	/** {@code meta.popup} of {@code off}/{@code false}/{@code 0}/{@code no} disables; any other value enables. */
	function popupOn(meta, defaultOn) {
		if (!meta || meta.popup == null || meta.popup === "") return defaultOn;
		const v = String(meta.popup).toLowerCase();
		return v !== "off" && v !== "false" && v !== "0" && v !== "no";
	}

	function zuluDisplay(cellData, withPopup) {
		const d = toDate(cellData);
		if (!d) return cellData == null ? "" : escHtml(cellData);
		const text = formatUtcZulu(d);
		if (!withPopup) return escHtml(text);
		return '<span class="juneau-ts" tabindex="0" data-juneau-ts="' + escAttr(d.toISOString()) + '">'
			+ escHtml(text) + "</span>";
	}

	// --- generic renderers (pure) --------------------------------------------------------------------------------
	// SERVER mode: display + class only.  Unknown-id policy (skip + warn) lives in the consumer (juneau-views.js).

	registerRenderer("date", {
		display: function (cellData) {
			const d = toDate(cellData);
			if (!d) return cellData == null ? "" : escHtml(cellData);
			return escHtml(new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "2-digit" }).format(d));
		},
		sort: function (cellData) { const d = toDate(cellData); return d ? d.getTime() : cellData; }
	});

	registerRenderer("datetime", {
		// Locale cell by default.  {@code meta.popup=on} opts into the same Zulu cell + local/California popup as
		// {@code ts-zulu} (Column.render(Render.of("datetime").meta("popup", "on"))).
		display: function (cellData, rowData, meta) {
			if (popupOn(meta, false)) return zuluDisplay(cellData, true);
			const d = toDate(cellData);
			if (!d) return cellData == null ? "" : escHtml(cellData);
			return escHtml(new Intl.DateTimeFormat(undefined, {
				year: "numeric", month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit"
			}).format(d));
		},
		sort: function (cellData) { const d = toDate(cellData); return d ? d.getTime() : cellData; }
	});

	registerRenderer("ts-zulu", {
		// UTC {@code MM/DD/YYYY HH:MMZ} cell; hover/focus popup of browser-local + California time (default on).
		// Column.render("ts-zulu"); disable with Render.of("ts-zulu").meta("popup", "off").
		display: function (cellData, rowData, meta) {
			return zuluDisplay(cellData, popupOn(meta, true));
		},
		sort: function (cellData) { const d = toDate(cellData); return d ? d.getTime() : cellData; }
	});

	registerRenderer("bool", {
		display: function (cellData) {
			if (cellData == null || cellData === "") return "";
			const truthy = (cellData === true || cellData === 1 || cellData === "true" || cellData === "1" || cellData === "yes");
			return truthy ? "Yes" : "No";
		}
	});

	registerRenderer("linked", {
		// Consumes the column's `href` {property} template (merged into meta by the initializer); no template -> text.
		display: function (cellData, rowData, meta) {
			if (cellData == null) return "";
			const text = String(cellData);
			const href = meta && meta.href;
			if (!href) return escHtml(text);
			return '<a href="' + escAttr(interpolateHref(href, rowData)) + '">' + escHtml(text) + "</a>";
		}
	});

	registerRenderer("truncate", {
		display: function (cellData, rowData, meta) {
			if (cellData == null) return "";
			const s = String(cellData);
			const max = meta && meta.length ? Number.parseInt(meta.length, 10) : 64;
			if (max <= 0 || s.length <= max) return escHtml(s);
			return '<span title="' + escAttr(s) + '">' + escHtml(s.substring(0, max)) + "\u2026</span>";
		}
	});

	registerRenderer("json", {
		display: function (cellData) {
			if (cellData == null) return "";
			let s;
			try { s = JSON.stringify(cellData); } catch (e) { s = String(cellData); }
			return "<code>" + escHtml(s) + "</code>";
		}
	});

	registerRenderer("decimal", {
		display: function (cellData, rowData, meta) {
			if (cellData == null || cellData === "") return "";
			const n = Number(cellData);
			if (Number.isNaN(n)) return escHtml(cellData);
			const places = meta && meta.places != null ? Number.parseInt(meta.places, 10) : 2;
			return escHtml(n.toFixed(places >= 0 ? places : 2));
		},
		sort: function (cellData) { const n = Number(cellData); return Number.isNaN(n) ? cellData : n; }
	});

	registerRenderer("tag", {
		// Emits the shared `.tag.<domain>.<value>` chip; <domain> = meta.field (the column key), <value> = the
		// cell.  Both CSS tokens are normalized via normalizeTagToken() to mirror console-ui's
		// Tag#normalize/TagHtmlRender token algorithm (lowercase, non-[a-z0-9_-] runs -> "-") so themed
		// chrome.css rules (e.g. `.tag.status.released`) match; the RAW value is kept as the display text.
		display: function (cellData, rowData, meta) {
			if (cellData == null || cellData === "") return "";
			const value = String(cellData);
			const domain = meta && meta.field ? String(meta.field) : "";
			const domainToken = normalizeTagToken(domain);
			const valueToken = normalizeTagToken(value);
			const cls = "tag" + (domainToken ? " " + domainToken : "") + (valueToken ? " " + valueToken : "");
			return '<span class="' + escAttr(cls) + '">' + escHtml(value) + "</span>";
		},
		"class": function () { return "tag-cell"; }
	});

	function progressUnknown(cellData) {
		if (cellData == null) return true;
		if (typeof cellData === "string" && cellData.trim() === "") return true;
		if (typeof cellData !== "number" && typeof cellData !== "string") return true;
		return !Number.isFinite(Number(cellData));
	}

	function progressMax(meta) {
		if (!meta || meta.max == null) return { ok: true, value: 100 };
		if (String(meta.max).trim() === "") return { ok: false };
		const n = Number(meta.max);
		if (!Number.isFinite(n) || n <= 0) return { ok: false };
		return { ok: true, value: n };
	}

	function progressThreshold(meta, key) {
		if (!meta || meta[key] == null || meta[key] === "") return null;
		const n = Number(meta[key]);
		return Number.isFinite(n) ? n : null;
	}

	function progressStateClass(actual, max, warn, exceeds) {
		if (actual > max) return "is-exceeds";
		if (exceeds != null && actual >= exceeds) return "is-exceeds";
		if (warn != null && actual >= warn) return "is-warn";
		return "is-ok";
	}

	function progressBarWidth(pct) {
		if (pct < 0) return 0;
		if (pct > 100) return 100;
		return pct;
	}

	registerRenderer("progress", {
		display: function (cellData, rowData, meta) {
			const fieldToken = meta && meta.field ? normalizeTagToken(meta.field) : "";
			const outerClass = "jc-progress" + (fieldToken ? " progress " + fieldToken : "");
			const maxParsed = progressMax(meta);
			if (progressUnknown(cellData) || !maxParsed.ok)
				return '<span class="' + escAttr(outerClass) + '"></span>';
			const actual = Number(cellData);
			const max = maxParsed.value;
			const pct = Math.round((actual / max) * 100);
			const barWidth = progressBarWidth(pct);
			const warn = progressThreshold(meta, "warn");
			const exceeds = progressThreshold(meta, "exceeds");
			const state = progressStateClass(actual, max, warn, exceeds);
			let labelMode = meta && meta.label != null && meta.label !== "" ? String(meta.label) : "percent";
			if (labelMode !== "none" && labelMode !== "value" && labelMode !== "percent")
				labelMode = "percent";
			const bar = '<span class="jc-progress-bar ' + state + '" style="width:' + barWidth + '%"></span>';
			let label = "";
			if (labelMode !== "none") {
				const text = labelMode === "value" ? String(cellData) : String(pct) + "%";
				label = '<span class="jc-progress-label">' + escHtml(text) + "</span>";
			}
			return '<span class="' + escAttr(outerClass) + '">' + bar + label + "</span>";
		},
		sort: function (cellData) {
			const n = Number(cellData);
			return Number.isFinite(n) ? n : cellData;
		},
		"class": function () { return "progress-cell"; }
	});

	const frozenBuiltins = Object.create(null);
	const BUILTIN_RENDER_IDS = [
		"bool", "date", "datetime", "decimal", "json", "linked", "progress", "tag", "truncate", "ts-zulu"
	];
	(function snapshotFrozenBuiltins() {
		for (let i = 0; i < BUILTIN_RENDER_IDS.length; i++) {
			const id = BUILTIN_RENDER_IDS[i];
			const def = registry[id];
			if (!def) continue;
			frozenBuiltins[id] = Object.freeze(Object.assign({}, def));
		}
	})();

	/** Sink-only lookup: built-in functions frozen at load; `registerRenderer` cannot replace these. */
	function resolveSinkRenderer(id) {
		return Object.hasOwn(frozenBuiltins, id) ? frozenBuiltins[id] : null;
	}

	// --- pill renderer (TODO-445k) -------------------------------------------------------------------------------
	// A status chip = leading dot + label, themed by the same shared `.tag.<domain>.<value>` classes the `tag`
	// renderer emits (so consumer palettes theme pills for free) plus one structural `.jc-pill-dot` span.  Display
	// only by default; `meta.action` (a ViewDef.rowActions id) opts into action-binding: the renderer stamps
	// `role="button" tabindex="0" data-juneau-action="<id>"` and the EXISTING table-level row-action handler
	// (juneau-views.js) dispatches - no handler is bound here, no second action protocol.
	//
	// DELIBERATELY registered on the cell-render path ONLY and NOT added to BUILTIN_RENDER_IDS / frozenBuiltins:
	// `pill` is not a fill-sink built-in this slice (k2 / review B3), so `resolveSinkRenderer("pill")` stays null
	// and the frozen id set stays the current 10.  The display facet returns an escaped string (like `tag`); it
	// never assigns innerHTML.
	registerRenderer("pill", {
		display: function (cellData, rowData, meta) {
			if (cellData == null || cellData === "") return "";
			const value = String(cellData);
			const domain = meta && meta.field ? String(meta.field) : "";
			const domainToken = normalizeTagToken(domain);
			const valueToken = normalizeTagToken(value);
			const cls = "jc-pill tag" + (domainToken ? " " + domainToken : "") + (valueToken ? " " + valueToken : "");
			// Tone class only for the three views progress tokens; neutral/absent/unknown inherit currentColor.
			const tone = meta && meta.tone ? String(meta.tone) : "";
			const toneClass = (tone === "ok" || tone === "warn" || tone === "exceeds") ? " is-" + tone : "";
			const dot = (meta && meta.dot === "off")
				? ""
				: '<span class="jc-pill-dot' + toneClass + '" aria-hidden="true"></span>';
			const action = meta && meta.action ? String(meta.action) : "";
			const actionAttrs = action
				? ' role="button" tabindex="0" data-juneau-action="' + escAttr(action) + '"'
				: "";
			return '<span class="' + escAttr(cls) + '" data-juneau-pill' + actionAttrs + '>' + dot + escHtml(value) + "</span>";
		},
		"class": function () { return "pill-cell"; }
	});

	// ==================================================================================================================
	// TIMESTAMP POPUP  (datetime-renderer-owned; 445c CellPopover can generalize later)
	// Two-line floating box: local browser time + California.  Built with createElement/textContent only — never
	// innerHTML — so a hostile ISO/label cannot become markup.  Delegated on document so DataTables re-renders
	// keep working.  Cursor-follows on hover; keyboard focus anchors under the cell.
	// ==================================================================================================================

	const TS_POPUP_ID = "juneau-ts-popup";
	const TS_POPUP_OFFSET = 12;

	function tsHost(t) {
		return (t && typeof t.closest === "function") ? t.closest("[data-juneau-ts]") : null;
	}

	function tsPopupEl() {
		if (typeof document === "undefined" || typeof document.getElementById !== "function") return null;
		let el = document.getElementById(TS_POPUP_ID);
		if (!el && document.body) {
			el = document.createElement("div");
			el.id = TS_POPUP_ID;
			el.className = "juneau-ts-popup";
			el.setAttribute("role", "tooltip");
			el.style.display = "none";
			document.body.appendChild(el);
		}
		return el || null;
	}

	function tsPopupFill(el, d) {
		el.replaceChildren();
		const lines = popupLines(d);
		const local = document.createElement("div");
		local.textContent = lines.local;
		const california = document.createElement("div");
		california.textContent = lines.california;
		el.appendChild(local);
		el.appendChild(california);
	}

	function tsPopupPosition(el, x, y) {
		if (!el) return;
		const vw = (typeof window !== "undefined" && window.innerWidth) ? window.innerWidth : 1024;
		const vh = (typeof window !== "undefined" && window.innerHeight) ? window.innerHeight : 768;
		const w = el.offsetWidth || 0;
		const h = el.offsetHeight || 0;
		let left = x + TS_POPUP_OFFSET;
		let top = y + TS_POPUP_OFFSET;
		if (left + w > vw - 4) left = Math.max(4, x - TS_POPUP_OFFSET - w);
		if (top + h > vh - 4) top = Math.max(4, y - TS_POPUP_OFFSET - h);
		el.style.left = left + "px";
		el.style.top = top + "px";
	}

	function tsPopupShow(host, x, y) {
		const d = toDate(host.getAttribute("data-juneau-ts"));
		if (!d) return;
		const el = tsPopupEl();
		if (!el) return;
		tsPopupFill(el, d);
		el.style.display = "block";
		tsPopupPosition(el, x, y);
	}

	function tsPopupHide() {
		const el = (typeof document !== "undefined" && typeof document.getElementById === "function")
			? document.getElementById(TS_POPUP_ID) : null;
		if (el) el.style.display = "none";
	}

	function initTsPopup() {
		if (typeof document === "undefined" || typeof document.addEventListener !== "function") return;
		if (document._juneauTsPopupBound) return;
		document._juneauTsPopupBound = true;
		document.addEventListener("mouseover", function (e) {
			const host = tsHost(e.target);
			if (host) tsPopupShow(host, e.clientX, e.clientY);
		});
		document.addEventListener("mousemove", function (e) {
			const el = document.getElementById(TS_POPUP_ID);
			if (!el || el.style.display === "none") return;
			if (tsHost(e.target)) tsPopupPosition(el, e.clientX, e.clientY);
			else tsPopupHide();
		});
		document.addEventListener("mouseout", function (e) {
			const host = tsHost(e.target);
			if (!host) return;
			const to = e.relatedTarget;
			if (to && typeof host.contains === "function" && host.contains(to)) return;
			tsPopupHide();
		});
		document.addEventListener("focusin", function (e) {
			const host = tsHost(e.target);
			if (!host || typeof host.getBoundingClientRect !== "function") return;
			const rect = host.getBoundingClientRect();
			tsPopupShow(host, rect.left, rect.bottom);
		});
		document.addEventListener("focusout", function (e) {
			const host = tsHost(e.target);
			if (!host) return;
			const to = e.relatedTarget;
			if (to && typeof host.contains === "function" && host.contains(to)) return;
			tsPopupHide();
		});
		document.addEventListener("keydown", function (e) {
			if (e.key === "Escape") tsPopupHide();
		});
	}

	if (typeof document !== "undefined") initTsPopup();

	// ==================================================================================================================
	// PUBLIC API
	// ==================================================================================================================

	NS.registerRenderer = registerRenderer;
	NS.resolveRenderer = resolveRenderer;
	NS.resolveSinkRenderer = resolveSinkRenderer;
	NS.parseRenderId = parseRenderId;
	// Exposed for reuse by the initializer + Option-B unit tests.
	NS._render = NS._render || {};
	NS._render.escHtml = escHtml;
	NS._render.escAttr = escAttr;
	NS._render.interpolateHref = interpolateHref;
	NS._render.toDate = toDate;
	NS._render.normalizeTagToken = normalizeTagToken;
	NS._render.formatUtcZulu = formatUtcZulu;
	NS._render.formatLocalTime = formatLocalTime;
	NS._render.formatCalifornia = formatCalifornia;
	NS._render.popupLines = popupLines;
	NS._render.frozenBuiltinIds = BUILTIN_RENDER_IDS.slice();
})();
