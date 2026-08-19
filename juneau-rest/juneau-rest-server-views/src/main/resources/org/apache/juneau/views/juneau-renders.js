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
		display: function (cellData) {
			const d = toDate(cellData);
			if (!d) return cellData == null ? "" : escHtml(cellData);
			return escHtml(new Intl.DateTimeFormat(undefined, {
				year: "numeric", month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit"
			}).format(d));
		},
		sort: function (cellData) { const d = toDate(cellData); return d ? d.getTime() : cellData; }
	});

	registerRenderer("ts-zulu", {
		// Absolute UTC ("Zulu") ISO-8601 timestamp - stable, locale-independent, no date library.
		display: function (cellData) {
			const d = toDate(cellData);
			if (!d) return cellData == null ? "" : escHtml(cellData);
			return escHtml(d.toISOString().replace(/\.\d{3}Z$/, "Z"));
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

	// ==================================================================================================================
	// PUBLIC API
	// ==================================================================================================================

	NS.registerRenderer = registerRenderer;
	NS.resolveRenderer = resolveRenderer;
	NS.parseRenderId = parseRenderId;
	// Exposed for reuse by the initializer + Option-B unit tests.
	NS._render = NS._render || {};
	NS._render.escHtml = escHtml;
	NS._render.escAttr = escAttr;
	NS._render.interpolateHref = interpolateHref;
	NS._render.toDate = toDate;
	NS._render.normalizeTagToken = normalizeTagToken;
})();
