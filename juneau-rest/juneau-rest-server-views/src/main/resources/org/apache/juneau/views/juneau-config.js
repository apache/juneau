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
 * juneau-config.js - opt-in column-configurator persistence + pure config-application layer (design doc #444).
 *
 * This file is loaded AFTER juneau-views.js (a template <script> include the consumer adds - NOT a dynamic
 * fetch; see design §4.1) and extends the SAME window.JuneauViews namespace.  A
 * non-configurable table never loads it and pays nothing.
 *
 * LANDED SLICES:
 *   - Slice 2: async persistence SPI, strict localStorage key codec (enc/dec), localStorage provider.
 *   - Slice 4: pure DOM-free config-application layer - computeEffectiveColumns / validateView / saved-view
 *     (de)serialization / dtIndex (the INDEX MODEL only).
 *   - Slice 5: resolveActiveView (awaited before first draw) + applyView (programmatic reinit entry point).
 *   - Slice 6: View-tab chooser UI, XSS textContent painting, mountChooser seam.
 *   - Slice 7: ribbon toggle persistence routes through this file's synchronous getItem/setItem
 *     (same exact keys as juneau-ribbon.js; never a Promise).
 *
 * Every provider implements the SAME seven-method async contract (Promise-based; design doc #444 §3.2/§3.3, refined by
 * the round-3 saveAndActivate addendum): list/load/save/saveAndActivate/setActive/delete/getActive.  Each method
 * takes the LIVE table element as its first argument (never a raw pageId/viewId string).  The localStorage provider
 * is the built-in implementation; a host may install its own through setPersistenceProvider.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM, no jQuery, no DataTables, no localStorage/fetch - plain data in, plain data out)
	// ==================================================================================================================

	/**
	 * Blob schema version every saved-view blob carries (§3.2) - lets both backends refuse an unknown/newer shape
	 * deterministically rather than guess at it.
	 */
	const CURRENT_SCHEMA_VERSION = 2;

	/** Decoded-name cap (§3.1) - named so JS and the slice-3 Java mixin reject at the exact same boundary. */
	const MAX_NAME_LEN = 128;

	/**
	 * Encoded key-SEGMENT cap (§3.1) - a DISTINCT number from {@link #MAX_NAME_LEN}: a 128-multibyte-char name can
	 * enc() to far more than 128 bytes, so the encoded cap must be named and checked independently of the decoded
	 * name cap.
	 */
	const MAX_ENCODED_SEGMENT_LEN = 512;

	/**
	 * The localStorage provider's per-(user,page,view)/per-blob/per-user bounds (§3.2).
	 */
	const LOCALSTORAGE_MAX_VIEWS_PER_SCOPE = 50;      // localStorage provider cap
	const LOCALSTORAGE_MAX_BLOB_BYTES = 64 * 1024;    // localStorage provider cap
	const LOCALSTORAGE_MAX_VIEWS_PER_USER = 500;      // localStorage provider cap

	/** The shell attribute a page host stamps the page id onto (§3.1) - read via closest(...). */
	const PAGE_ID_ATTR = "data-juneau-page";

	/** The attribute juneau-views.js reads a table's own stable view id off (reused here - no new attribute). */
	const VIEW_ID_ATTR = "data-juneau-view";

	const KEY_ROOT = "juneau.view.";

	/** Blank/absent/whitespace-only - mirrors juneau-views.js's own isBlankToken so the two files agree by construction. */
	function isBlank(v) {
		return v == null || String(v).trim() === "";
	}

	/** Builds a typed persistence-SPI error (§3.2: {code:'quota'|'unavailable'|'network'|'malformed', message}). */
	function typedError(code, message) {
		const e = new Error(message);
		e.code = code;
		return e;
	}

	function malformedError(message) { return typedError("malformed", message); }
	function quotaError(message) { return typedError("quota", message); }
	function unavailableError(message) { return typedError("unavailable", message); }

	/**
	 * Normalizes ANY thrown value into the typed {code,message} shape (§3.2 "typed failure, never silent
	 * success") - a value already carrying one of the four frozen codes passes through; a native localStorage
	 * quota/private-mode DOMException maps to 'quota'; anything else is 'unavailable' rather than swallowed.
	 */
	function toTypedError(e) {
		if (e?.code === "quota" || e?.code === "unavailable" || e?.code === "network" || e?.code === "malformed")
			return { code: e.code, message: e.message };
		if (e?.name === "QuotaExceededError" || e?.code === 22 || e?.code === 1014)
			return { code: "quota", message: "localStorage quota exceeded or blocked (private mode)" };
		return { code: "unavailable", message: e?.message ? e.message : String(e) };
	}

	/**
	 * Manual, dependency-free UTF-8 byte encoder (deliberately NOT TextEncoder - keeps this pure-logic function
	 * testable/portable with zero Web-API surface, matching the module's Option-B "no DOM/library" convention).
	 * Surrogate pairs are combined via codePointAt/advancing the loop index, so a >0xFFFF code point emits its
	 * correct 4-byte sequence rather than two separate (invalid) 3-byte ones.
	 */
	function utf8Encode(str) {
		const bytes = [];
		for (let i = 0; i < str.length; i++) {
			const code = str.codePointAt(i);
			if (code > 0xFFFF) i++;   // this code point consumed a trailing low surrogate too - skip it
			if (code < 0x80) {
				bytes.push(code);
			} else if (code < 0x800) {
				bytes.push(0xC0 | (code >> 6), 0x80 | (code & 0x3F));
			} else if (code < 0x10000) {
				bytes.push(0xE0 | (code >> 12), 0x80 | ((code >> 6) & 0x3F), 0x80 | (code & 0x3F));
			} else {
				bytes.push(0xF0 | (code >> 18), 0x80 | ((code >> 12) & 0x3F), 0x80 | ((code >> 6) & 0x3F), 0x80 | (code & 0x3F));
			}
		}
		return bytes;
	}

	/** The inverse of {@link #utf8Encode} - rejects (throws a 'malformed' error) any invalid UTF-8 byte sequence. */
	function utf8Decode(bytes) {
		let out = "";
		let i = 0;
		while (i < bytes.length) {
			const b0 = bytes[i];
			let cp, len;
			if (b0 < 0x80) { cp = b0; len = 1; }
			else if ((b0 & 0xE0) === 0xC0) { cp = b0 & 0x1F; len = 2; }
			else if ((b0 & 0xF0) === 0xE0) { cp = b0 & 0x0F; len = 3; }
			else if ((b0 & 0xF8) === 0xF0) { cp = b0 & 0x07; len = 4; }
			else throw malformedError("invalid UTF-8 lead byte in encoded segment");
			if (i + len > bytes.length) throw malformedError("truncated UTF-8 sequence in encoded segment");
			for (let j = 1; j < len; j++) {
				const b = bytes[i + j];
				if ((b & 0xC0) !== 0x80) throw malformedError("invalid UTF-8 continuation byte in encoded segment");
				cp = (cp << 6) | (b & 0x3F);
			}
			out += String.fromCodePoint(cp);
			i += len;
		}
		return out;
	}

	/** The exact allowed raw (never-escaped) segment alphabet (§3.1) - a single ASCII char test. */
	function isSafeSegmentChar(ch) {
		return (ch >= "a" && ch <= "z") || (ch >= "A" && ch <= "Z") || (ch >= "0" && ch <= "9") || ch === "_" || ch === "-";
	}

	function hex2Upper(b) {
		const h = b.toString(16).toUpperCase();
		return h.length < 2 ? "0" + h : h;
	}

	/**
	 * Strict segment encoder (§3.1 finding - Blocker: key codec).  Percent-encodes every UTF-8 byte OUTSIDE the
	 * alphabet [A-Za-z0-9_-] as "%HH" with UPPERCASE hex - deliberately NOT encodeURIComponent (which leaves
	 * ".", "!", "~", "*", "'", "(", ")" and space unescaped; an unescaped "." would climb this grammar's "."
	 * path separator).  This is the STORAGE-KEY encoder only - never a wire/query-string encoder.
	 */
	function encSegment(s) {
		const str = s == null ? "" : String(s);
		const bytes = utf8Encode(str);
		let out = "";
		for (const b of bytes) {
			const ch = b < 128 ? String.fromCodePoint(b) : null;
			out += (ch != null && isSafeSegmentChar(ch)) ? ch : ("%" + hex2Upper(b));
		}
		return out;
	}

	/**
	 * The exact inverse of {@link #encSegment} - throws a typed 'malformed' error (never a lenient best-effort
	 * decode) on: a "%" not followed by exactly two hex digits, lowercase hex digits (the canon is uppercase), or
	 * any RAW character outside the safe alphabet that was not percent-encoded (a spec-compliant encoder would
	 * never have left it raw, so accepting it here would silently accept a non-canonical key).
	 */
	function decSegment(s) {
		const str = s == null ? "" : String(s);
		const bytes = [];
		let i = 0;
		while (i < str.length) {
			const ch = str.charAt(i);
			if (ch === "%") {
				const hex = str.slice(i + 1, i + 3);
				if (!/^[0-9A-F]{2}$/.test(hex))
					throw malformedError("non-canonical percent-encoding at offset " + i + " in '" + str + "'");
				bytes.push(Number.parseInt(hex, 16));
				i += 3;
			} else {
				if (!isSafeSegmentChar(ch))
					throw malformedError("illegal raw character '" + ch + "' at offset " + i + " in '" + str + "'");
				bytes.push(ch.codePointAt(0));
				i += 1;
			}
		}
		return utf8Decode(bytes);
	}

	/**
	 * Builds the scope segment (§3.1): {@code enc(pageId)~enc(viewId)} for a page-embedded view, or bare
	 * {@code enc(viewId)} for a standalone one.  The "~" join is outside the allowed segment alphabet, so it can
	 * never appear INSIDE an encoded segment - two encoded segments joined by it can always be split back apart
	 * unambiguously.  Page-qualification is load-bearing (not "view-only for simplicity"): two different pages
	 * embedding a view with the same ViewDef.id would otherwise collide on one shared column-configuration namespace.
	 */
	function scopeKey(pageId, viewId) {
		return isBlank(pageId) ? encSegment(viewId) : (encSegment(pageId) + "~" + encSegment(viewId));
	}

	function viewsPrefixKey(scope) { return KEY_ROOT + scope + ".columns.views."; }
	function viewKeyFor(scope, encodedName) { return viewsPrefixKey(scope) + encodedName; }
	function activeKeyFor(scope) { return KEY_ROOT + scope + ".columns.active"; }

	/** "Default" (case-insensitive) is reserved - it IS the catalog defaults and can never be a saved-view name. */
	function isReservedName(name) {
		return String(name).trim().toLowerCase() === "default";
	}

	/**
	 * The wire-side name check (§3.1/§3.2): blank/reserved/too-long - applied to the raw DECODED name.  Deliberately does NOT touch encSegment/MAX_ENCODED_SEGMENT_LEN - that check is
	 * localStorage-key-specific (see validateNameForLocalStorage below).
	 */
	function validateNameBasic(name) {
		if (name == null) return { ok: false, code: "malformed", message: "saved-view name must not be null" };
		const s = String(name);
		if (s.trim().length === 0) return { ok: false, code: "malformed", message: "saved-view name must not be blank" };
		if (s.length > MAX_NAME_LEN)
			return { ok: false, code: "malformed", message: "saved-view name exceeds MAX_NAME_LEN (" + MAX_NAME_LEN + ")" };
		if (isReservedName(s))
			return { ok: false, code: "malformed", message: "'Default' is reserved and cannot be used as a saved-view name" };
		return { ok: true };
	}

	/**
	 * The localStorage-only name check: {@link #validateNameBasic} PLUS the encoded-segment length cap (§3.1) -
	 * the localStorage provider is the one place a name is actually enc()'d into a storage key, so it is the one
	 * place MAX_ENCODED_SEGMENT_LEN can be exceeded independently of MAX_NAME_LEN.
	 */
	function validateNameForLocalStorage(name) {
		const basic = validateNameBasic(name);
		if (!basic.ok) return basic;
		const encoded = encSegment(name);
		if (encoded.length > MAX_ENCODED_SEGMENT_LEN)
			return { ok: false, code: "malformed",
				message: "encoded saved-view name exceeds MAX_ENCODED_SEGMENT_LEN (" + MAX_ENCODED_SEGMENT_LEN + ")" };
		return { ok: true, encoded: encoded };
	}

	/**
	 * The dangling-active resolution rule (§3.2 should-fix), applied uniformly to EVERY provider's raw list()
	 * result by the public facade below (never duplicated per-provider): if `active` does not name any view
	 * actually present in `views`, the runtime treats it as Default (never an error, never a crash) and flags
	 * `dangling:true` so a caller can surface the required one-time notice.  Covers the concurrent hole "tab A
	 * writes blob X, tab B deletes X, tab A flips active to X": the flip is honored, but this resolution turns
	 * the now-missing X back into Default rather than an empty/broken table.
	 */
	function resolveActiveAgainstViews(active, views) {
		if (active == null) return { name: null, dangling: false };
		const found = (views || []).some(function (v) { return v?.name === active; });
		return found ? { name: active, dangling: false } : { name: null, dangling: true };
	}

	/** Rejects an unknown/newer blob schemaVersion as 'malformed' (§3.2 stale-load handling) - never crashes. */
	function assertSupportedSchema(blob) {
		if (blob?.schemaVersion !== CURRENT_SCHEMA_VERSION)
			throw malformedError("saved-view blob has an unsupported schemaVersion (expected " + CURRENT_SCHEMA_VERSION + ")");
		return blob;
	}

	/**
	 * Resolves the enclosing page id (§3.1): {@code table.closest('[data-juneau-page]')}'s OWN attribute value,
	 * falling back to null (standalone) when there is no page shell.  This is the CONTRACT, not a convenience
	 * read: a host that wants page-qualification must stamp this exact attribute rather than inventing a parallel
	 * one.  HTML-slot nav stamps it on {@code .juneau-page-nav}.
	 */
	function resolvePageId(table) {
		const host = table?.closest ? table.closest("[" + PAGE_ID_ATTR + "]") : null;
		const v = host ? host.dataset.juneauPage : null;
		return isBlank(v) ? null : v;
	}

	/** Resolves the table's own stable view id - reuses the SAME attribute juneau-views.js's initTable reads. */
	function resolveViewId(table) {
		const v = table ? table.dataset.juneauView : null;
		return isBlank(v) ? null : v;
	}

	// ==================================================================================================================
	// PURE CONFIG-APPLICATION LAYER  (§4.3 — Option-B testable; no DOM / jQuery / DataTables)
	// ==================================================================================================================

	/** True when `arr` contains the same value more than once (strict equality). */
	function hasDuplicateEntries(arr) {
		if (!arr || arr.length < 2) return false;
		const seen = Object.create(null);
		for (const v of arr) {
			const k = String(v);
			if (seen[k]) return true;
			seen[k] = true;
		}
		return false;
	}

	/** Catalog → Map<dataKey, column> (first wins if the author somehow duplicated a data key). */
	function catalogByData(catalog) {
		const map = Object.create(null);
		(catalog || []).forEach(function (c) {
			if (c?.data != null && map[c.data] == null) map[c.data] = c;
		});
		return map;
	}

	/** Shallow-copies the own-enumerable keys of `meta` into a fresh object, or returns `undefined` when absent. */
	function copyMeta(meta) {
		if (!meta) return undefined;
		const out = {};
		for (const k in meta) if (Object.hasOwn(meta, k)) out[k] = meta[k];
		return out;
	}

	/** One `popover.fields[]` entry, copied by {@link #copyPopover}. */
	function copyPopoverField(f) {
		if (!f || typeof f !== "object") return f;
		const g = { data: f.data };
		if (f.title != null) g.title = f.title;
		if (f.render != null) {
			if (typeof f.render === "string") {
				g.render = f.render;
			} else {
				g.render = { id: f.render.id };
				if (f.render.meta) g.render.meta = copyMeta(f.render.meta);
			}
		}
		return g;
	}

	function copyPopover(p) {
		if (!p || typeof p !== "object") return p;
		const out = {};
		if (p.title != null) out.title = p.title;
		if (p.fields) out.fields = p.fields.map(copyPopoverField);
		return out;
	}

	/**
	 * Shallow-copies a catalog column into an effective-column model.  Nested `render` / `formats` are copied so a
	 * later format swap cannot mutate the live catalog object the VIEW_META sidecar handed us.
	 */
	function copyCatalogColumn(col) {
		const out = {
			data: col.data,
			orderable: col.orderable,
			searchable: col.searchable,
			pinned: col.pinned,
			defaultVisible: col.defaultVisible
		};
		if (col.name != null) out.name = col.name;
		if (col.title != null) out.title = col.title;
		if (col.href != null) out.href = col.href;
		if (col.className != null) out.className = col.className;
		if (col.formats) out.formats = col.formats.slice();
		if (col.render != null) {
			if (typeof col.render === "string") {
				out.render = col.render;
			} else {
				out.render = { id: col.render.id };
				if (col.render.meta) out.render.meta = copyMeta(col.render.meta);
				if (col.render.popover) out.render.popover = copyPopover(col.render.popover);
			}
		}
		return out;
	}

	/**
	 * Format-swap helper (§4.3): replace ONLY the renderer id, keeping any existing `meta` (and leaving column
	 * `href` on the column itself — {@link #mergeMeta} in juneau-views.js still folds href into meta at display
	 * time).  Accepts both the wire object form `{id,meta}` and the compact string sugar `"id:field"`.
	 */
	function swapRenderId(render, newId) {
		if (render == null || render === "") return { id: newId };
		if (typeof render === "string") {
			const i = render.indexOf(":");
			if (i < 0) return { id: newId };
			return { id: newId, meta: { field: render.substring(i + 1) } };
		}
		const out = { id: newId };
		if (render.meta) out.meta = copyMeta(render.meta);
		if (render.popover) out.popover = copyPopover(render.popover);
		return out;
	}

	/**
	 * Default visible set when a saved view omits `visible` (§4.3): each catalog column contributes iff
	 * `defaultVisible !== false`, and pinned columns are always included regardless of defaultVisible.
	 */
	function defaultVisibleKeys(catalog) {
		const out = [];
		(catalog || []).forEach(function (c) {
			if (c?.data == null) return;
			if (c.pinned || c.defaultVisible !== false) out.push(c.data);
		});
		return out;
	}

	/** Resolves `view.order` (§4.3): explicit order with unknown ids dropped/catalog ids appended, or catalog order when absent. */
	function resolveOrder(view, byData, catalogOrder) {
		if (view.order == null) return { ok: true, order: catalogOrder.slice() };
		if (!Array.isArray(view.order))
			return { ok: false, code: "malformed", message: "saved-view order must be an array" };
		if (hasDuplicateEntries(view.order))
			return { ok: false, code: "malformed", message: "saved-view order contains duplicate column ids" };
		const order = [];
		view.order.forEach(function (id) {
			if (byData[id] != null) order.push(id);
		});
		catalogOrder.forEach(function (id) {
			if (order.indexOf(id) < 0) order.push(id);
		});
		return { ok: true, order: order };
	}

	/**
	 * Resolves `view.visible` (§4.3) before the pinned-column / at-least-one-visible repairs: explicit visible ids
	 * with unknowns dropped, or each column's {@code defaultVisible ?? true} when absent.
	 */
	function resolveVisible(view, byData, cols) {
		if (view.visible == null) return { ok: true, visible: defaultVisibleKeys(cols) };
		if (!Array.isArray(view.visible))
			return { ok: false, code: "malformed", message: "saved-view visible must be an array" };
		if (hasDuplicateEntries(view.visible))
			return { ok: false, code: "malformed", message: "saved-view visible contains duplicate column ids" };
		const visible = [];
		view.visible.forEach(function (id) {
			if (byData[id] != null) visible.push(id);
		});
		return { ok: true, visible: visible };
	}

	/** `view.labels` overrides for known columns only (unknown column ids are dropped). */
	function resolveLabels(view, byData) {
		const labels = {};
		if (view.labels && typeof view.labels === "object") {
			for (const k in view.labels) {
				if (Object.hasOwn(view.labels, k) && byData[k] != null)
					labels[k] = view.labels[k];
			}
		}
		return labels;
	}

	/** `view.formats` overrides for known columns, constrained to that column's declared `formats` list. */
	function resolveFormats(view, byData) {
		const formats = {};
		if (view.formats && typeof view.formats === "object") {
			for (const k in view.formats) {
				if (!Object.hasOwn(view.formats, k) || byData[k] == null) continue;
				const fmt = view.formats[k];
				const allowed = byData[k].formats;
				// Constrain to the column's declared formats list — drop (never apply) an undeclared override.
				if (Array.isArray(allowed) && allowed.indexOf(fmt) >= 0) formats[k] = fmt;
			}
		}
		return formats;
	}

	/**
	 * Validates / normalizes a saved-view blob against a column catalog (§4.3 hardening):
	 * <ul>
	 *   <li>pinned columns are always visible</li>
	 *   <li>≥1 visible column (repairs an all-hidden blob by forcing the first catalog column visible)</li>
	 *   <li>unknown column ids in visible/order/labels/formats are dropped</li>
	 *   <li>duplicate entries in `visible` OR `order` are rejected ({@code ok:false})</li>
	 *   <li>format overrides not in that column's declared `formats` list are dropped</li>
	 *   <li>absent `visible` ⇒ each column's {@code defaultVisible ?? true}; absent `order` ⇒ catalog order</li>
	 * </ul>
	 * Returns {@code {ok:true, view}} with a normalized blob, or {@code {ok:false, code, message}} on hard reject.
	 * {@code view == null} (the Default) is valid and returns {@code {ok:true, view:null}}.
	 */
	function validateView(view, catalog) {
		if (view == null) return { ok: true, view: null };
		if (typeof view !== "object")
			return { ok: false, code: "malformed", message: "saved view must be a plain object" };

		const cols = catalog || [];
		const byData = catalogByData(cols);
		const catalogOrder = cols.map(function (c) { return c.data; }).filter(function (d) { return d != null; });

		const orderResult = resolveOrder(view, byData, catalogOrder);
		if (!orderResult.ok) return orderResult;
		const order = orderResult.order;

		const visibleResult = resolveVisible(view, byData, cols);
		if (!visibleResult.ok) return visibleResult;
		let visible = visibleResult.visible;

		// Pinned columns are always visible (un-hideable but reorderable).
		cols.forEach(function (c) {
			if (c?.pinned && c.data != null && visible.indexOf(c.data) < 0) visible.push(c.data);
		});

		// ≥1 visible — repair an all-hidden blob rather than crash the table.
		if (visible.length === 0 && catalogOrder.length > 0)
			visible = [catalogOrder[0]];

		return {
			ok: true,
			view: {
				schemaVersion: view.schemaVersion != null ? view.schemaVersion : CURRENT_SCHEMA_VERSION,
				visible: visible,
				order: order,
				labels: resolveLabels(view, byData),
				formats: resolveFormats(view, byData)
			}
		};
	}

	/**
	 * Layers a (possibly null = Default) saved view over the catalog to produce the ordered, visibility-tagged,
	 * relabeled, reformatted effective column model (§4.1 / §4.3).  Runs {@link #validateView} first so unknown
	 * ids are dropped and format overrides are constrained; a hard-reject (duplicate ids) throws a typed
	 * {@code malformed} error for the load path to treat as Default.
	 *
	 * <p>Blank label overrides revert to the catalog {@code title}.  A format swap replaces only the renderer
	 * id — {@code render.meta} and column {@code href} are preserved.
	 */
	function computeEffectiveColumns(catalog, savedView) {
		const cols = catalog || [];
		const validated = validateView(savedView, cols);
		if (!validated.ok) throw malformedError(validated.message);
		const normalized = validated.view;   // null ⇒ Default

		const byData = catalogByData(cols);
		const order = normalized ? normalized.order : cols.map(function (c) { return c.data; });
		const visibleSet = Object.create(null);
		(normalized ? normalized.visible : defaultVisibleKeys(cols)).forEach(function (id) {
			visibleSet[id] = true;
		});
		cols.forEach(function (c) {
			if (c?.pinned && c.data != null) visibleSet[c.data] = true;
		});
		const visibleKeys = Object.keys(visibleSet);
		if (visibleKeys.length === 0 && order.length > 0) visibleSet[order[0]] = true;

		const out = [];
		order.forEach(function (dataKey) {
			const col = byData[dataKey];
			if (!col) return;
			const effective = copyCatalogColumn(col);

			// Label override — blank/whitespace reverts to the catalog title (persistence-only; no wire field).
			if (normalized?.labels && Object.hasOwn(normalized.labels, dataKey)) {
				const override = normalized.labels[dataKey];
				if (override != null && String(override).trim() !== "")
					effective.title = String(override);
				// else leave catalog title (blank-label revert)
			}

			// Format override — id swap only; meta + href preserved via copyCatalogColumn + swapRenderId.
			if (normalized?.formats && Object.hasOwn(normalized.formats, dataKey)) {
				effective.render = swapRenderId(col.render, normalized.formats[dataKey]);
			}

			effective.visible = !!visibleSet[dataKey];
			out.push(effective);
		});
		return out;
	}

	/**
	 * Deserializes a saved-view blob (object or JSON string) into a plain
	 * {@code {schemaVersion, visible, order, labels, formats}} (§3.1).  Unknown/newer {@code schemaVersion}
	 * rejects as typed {@code malformed} via {@link #assertSupportedSchema}.
	 */
	function deserializeSavedView(raw) {
		let blob = raw;
		if (typeof raw === "string") {
			try { blob = JSON.parse(raw); }
			catch (e) { throw malformedError("saved-view blob is not valid JSON"); } // NOSONAR javascript:S2486 -- the parse failure is rethrown as a typed malformed error
		}
		assertSupportedSchema(blob);
		return {
			schemaVersion: blob.schemaVersion,
			visible: blob.visible == null ? null : Array.prototype.slice.call(blob.visible),
			order: blob.order == null ? null : Array.prototype.slice.call(blob.order),
			labels: (blob.labels && typeof blob.labels === "object") ? { ...blob.labels } : {},
			formats: (blob.formats && typeof blob.formats === "object") ? { ...blob.formats } : {}
		};
	}

	/**
	 * Serializes a draft / normalized view into the persisted blob shape (§3.1):
	 * {@code {schemaVersion, visible, order, labels, formats}}.  Blank label overrides are omitted (they mean
	 * "use catalog title"); empty {@code labels}/{@code formats} objects are still emitted so the schema stays
	 * stable for round-trips.
	 */
	/** Non-blank label overrides only (a blank override means "use catalog title" and is omitted from the blob). */
	function serializeLabels(view) {
		const out = {};
		if (!view?.labels || typeof view.labels !== "object") return out;
		for (const k in view.labels) {
			if (!Object.hasOwn(view.labels, k)) continue;
			const v = view.labels[k];
			if (v != null && String(v).trim() !== "") out[k] = String(v);
		}
		return out;
	}

	/** Format overrides only (nullish values are omitted from the blob). */
	function serializeFormats(view) {
		const out = {};
		if (!view?.formats || typeof view.formats !== "object") return out;
		for (const k in view.formats)
			if (Object.hasOwn(view.formats, k) && view.formats[k] != null) out[k] = view.formats[k];
		return out;
	}

	function serializeSavedView(view) {
		return {
			schemaVersion: CURRENT_SCHEMA_VERSION,
			visible: Array.isArray(view?.visible) ? view.visible.slice() : [],
			order: Array.isArray(view?.order) ? view.order.slice() : [],
			labels: serializeLabels(view),
			formats: serializeFormats(view)
		};
	}

	/**
	 * Builds the ACTUAL DataTables {@code opts.columns} index space (§4.2):
	 * {@code [expander?] + [selection?] + effectiveColumns(including hidden, in order) + [actions?]}.
	 * Synthetic expander/selection/actions cells use {@code data:null} and a {@code _juneau} marker so they are never
	 * mistaken for a catalog column by {@link #dtIndex}.  Pure / DOM-free — slice 5 rewires consumers onto this.
	 */
	function buildOptsColumnSpace(effectiveColumns, options) {
		const cols = [];
		if (options?.hasRowDetail)
			cols.push({ data: null, _juneau: "detail" });
		if (options?.hasSelection)
			cols.push({ data: null, _juneau: "selection" });
		(effectiveColumns || []).forEach(function (c) {
			cols.push({
				data: c.data,
				visible: c.visible !== false,
				title: c.title
			});
		});
		if (options?.hasActions)
			cols.push({ data: null, _juneau: "actions" });
		return cols;
	}

	/**
	 * The single DataTables index function (§4.2): index of {@code dataKey} in the ACTUAL {@code opts.columns}
	 * array ({@link #buildOptsColumnSpace}).  Hidden columns stay in the array ({@code visible:false}), so this
	 * is NOT "Nth visible + selection offset".  Returns {@code -1} when the key is absent.
	 *
	 * <p>Load-bearing fixture: {@code [sel, A, B(hidden), C, actions]} → {@code dtIndex('C') === 3} (not 2).
	 */
	function dtIndex(dataKey, optsColumns) {
		if (!optsColumns) return -1;
		return optsColumns.findIndex(function (col) { return col?.data === dataKey; });
	}

	// ==================================================================================================================
	// LOCALSTORAGE PROVIDER  (the zero-config default)
	// ==================================================================================================================

	/**
	 * Builds the zero-config {@code localStorage} persistence provider (§3.3).  Wraps every operation in a
	 * try/catch that maps a native quota/private-mode failure to the typed 'quota' error, resolves in a
	 * microtask (so a configurable table's compute-before-first-draw handshake, §4.1, never has to show a
	 * loading placeholder for this provider), and listens for the {@code storage} event so an external tab's
	 * write can be reconciled (last-write-wins) - see {@code watchExternalChanges} below.
	 */
	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: every helper here is deliberately
	// unreachable outside `window.JuneauViews`, and hoisting it past the closing `})()` would leak it globally.
	function requireScope(table) {
		const viewId = resolveViewId(table);
		if (viewId == null)
			throw unavailableError("table has no " + VIEW_ID_ATTR + " id; cannot resolve a persistence scope");
		const pageId = resolvePageId(table);
		return { pageId: pageId, viewId: viewId, scope: scopeKey(pageId, viewId) };
	}

	/** Reads the raw active pointer.  A malformed (undecodable) stored value is passed through un-decoded -
	 *  it will simply never match a real (always-valid) view name, so it naturally resolves to the dangling/
	 *  Default path (resolveActiveAgainstViews) rather than needing a second failure mode here. */
	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function readActiveRaw(scope) {
		const raw = window.localStorage.getItem(activeKeyFor(scope));
		if (raw == null) return null;
		try { return decSegment(raw); } catch (e) { return raw; } // NOSONAR javascript:S2486 -- an undecodable segment falls back to the raw text
	}

	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function writeActiveRaw(scope, name) {
		const key = activeKeyFor(scope);
		if (name == null) window.localStorage.removeItem(key);
		else window.localStorage.setItem(key, encSegment(name));
	}

	/** Every saved view currently under `scope`, decoded - an undecodable key is silently skipped (never crashes). */
	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function listViews(scope) {
		const prefix = viewsPrefixKey(scope);
		const out = [];
		for (let i = 0; i < window.localStorage.length; i++) {
			const k = window.localStorage.key(i);
			if (k?.startsWith(prefix)) {
				try { out.push({ name: decSegment(k.slice(prefix.length)) }); } catch (e) { /* skip unreadable key */ } // NOSONAR javascript:S2486 -- an unreadable key is skipped
			}
		}
		return out;
	}

	/** The per-user AGGREGATE count across every (page,view) scope (§3.2 quota-bypass fix) - a substring scan,
	 *  never a regex, so an attacker-controlled scope/name segment cannot be mistaken for pattern syntax. */
	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function countAllViewsForThisUser() {
		let count = 0;
		for (let i = 0; i < window.localStorage.length; i++) {
			const k = window.localStorage.key(i);
			if (k?.startsWith(KEY_ROOT) && k.indexOf(".columns.views.") > 0) count++;
		}
		return count;
	}

	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function byteLength(str) { return utf8Encode(str).length; }

	/** Runs `fn` synchronously but always returns a settled-in-a-microtask Promise, typed-error on throw. */
	// NOSONAR javascript:S7721 -- must stay inside this file's module IIFE: hoisting past the closing `})()`
	// would leak it globally.
	function localStorageAsAsync(fn) {
		return new Promise(function (resolve, reject) {
			// NOSONAR javascript:S6671 -- toTypedError() deliberately returns the plain {code,message}
			// shape documented at its definition (§3.2 "typed failure" contract), not an Error subclass;
			// every consumer (in this file and the chooser UI) reads only `.code`/`.message`, and widening
			// this to an Error would risk changing enumerable-property/JSON-serialization behavior for
			// callers outside this file that we cannot fully audit.
			try { resolve(fn()); } catch (e) { reject(toTypedError(e)); } // NOSONAR javascript:S6671 -- typed {code,message} failure contract, not an Error subclass; see above
		});
	}

	/** Enforces the per-blob/per-scope/per-user bounds (§3.2) INSIDE the write op, never as a racy pre-flight. */
	function enforceBounds(scope, name, blob) {
		const json = JSON.stringify(blob);
		if (byteLength(json) > LOCALSTORAGE_MAX_BLOB_BYTES)
			throw quotaError("saved view exceeds the per-blob size cap (" + LOCALSTORAGE_MAX_BLOB_BYTES + " bytes)");
		const existing = listViews(scope);
		const isReplace = existing.some(function (v) { return v.name === name; });
		if (!isReplace && existing.length >= LOCALSTORAGE_MAX_VIEWS_PER_SCOPE)
			throw quotaError("scope already has " + LOCALSTORAGE_MAX_VIEWS_PER_SCOPE + " saved views (MAX_VIEWS_PER_SCOPE)");
		if (!isReplace && countAllViewsForThisUser() >= LOCALSTORAGE_MAX_VIEWS_PER_USER)
			throw quotaError("aggregate saved-view count reached MAX_VIEWS_PER_USER (" + LOCALSTORAGE_MAX_VIEWS_PER_USER + ")");
	}

	function createLocalStorageProvider() {

		function persistBlob(scope, name, blob) {
			const v = validateNameForLocalStorage(name);
			if (!v.ok) throw malformedError(v.message);
			assertSupportedSchema(blob);
			enforceBounds(scope, name, blob);
			window.localStorage.setItem(viewKeyFor(scope, v.encoded), JSON.stringify(blob));
		}

		return {

			list: function (table) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					return { active: readActiveRaw(ctx.scope), views: listViews(ctx.scope) };
				});
			},

			load: function (table, name) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					const v = validateNameForLocalStorage(name);
					if (!v.ok) throw malformedError(v.message);
					const raw = window.localStorage.getItem(viewKeyFor(ctx.scope, v.encoded));
					if (raw == null) return null;
					let blob;
					try { blob = JSON.parse(raw); } catch (e) { throw malformedError("stored saved-view blob is not valid JSON"); } // NOSONAR javascript:S2486 -- the parse failure is rethrown as a typed malformed error
					return assertSupportedSchema(blob);
				});
			},

			save: function (table, name, blob) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					persistBlob(ctx.scope, name, blob);
				});
			},

			/**
			 * Writes the blob AND flips the active pointer as the single seam method (§3.2/round-3 R3-B3) - JS is
			 * single-threaded, so within THIS tab no other script can interleave between the two localStorage
			 * writes below; across tabs, each write is simply the last one to land (last-write-wins), which is
			 * the documented localStorage consistency model.
			 */
			saveAndActivate: function (table, name, blob) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					persistBlob(ctx.scope, name, blob);
					writeActiveRaw(ctx.scope, name);
				});
			},

			setActive: function (table, name) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					if (name == null) { writeActiveRaw(ctx.scope, null); return; }
					const v = validateNameForLocalStorage(name);
					if (!v.ok) throw malformedError(v.message);
					writeActiveRaw(ctx.scope, name);
				});
			},

			"delete": function (table, name) {
				return localStorageAsAsync(function () {
					const ctx = requireScope(table);
					const v = validateNameForLocalStorage(name);
					if (!v.ok) throw malformedError(v.message);
					window.localStorage.removeItem(viewKeyFor(ctx.scope, v.encoded));
				});
			},

			/**
			 * Multi-tab reconcile (§3.2, localStorage-only): listens for the native `storage` event and invokes
			 * `onChange` whenever a key under THIS table's scope changed in another tab/window.  Returns an
			 * unsubscribe function.  A later slice's chooser wires this to a "changed in another tab" notice and
			 * a last-write-wins re-read; slice 2 lands only the wiring primitive itself.
			 */
			watchExternalChanges: function (table, onChange) {
				const ctx = requireScope(table);
				const scopePrefix = KEY_ROOT + ctx.scope + ".";
				function handler(e) {
					if (e.key?.startsWith(scopePrefix)) onChange({ key: e.key, oldValue: e.oldValue, newValue: e.newValue });
				}
				window.addEventListener("storage", handler);
				return function unwatch() { window.removeEventListener("storage", handler); };
			}
		};
	}

	// ==================================================================================================================
	// PUBLIC API  (the provider-selection seam + the uniform facade both providers are called through)
	// ==================================================================================================================

	let currentProvider = null;
	let lazyDefaultProvider = null;

	function activeProvider() {
		if (currentProvider) return currentProvider;
		if (!lazyDefaultProvider) lazyDefaultProvider = createLocalStorageProvider();
		return lazyDefaultProvider;
	}

	/** Swaps the active persistence provider (§3.2/§5) - e.g. `JuneauViews.setPersistenceProvider(myProvider)`. */
	NS.setPersistenceProvider = function (provider) {
		currentProvider = provider;
	};

	/** The provider-selection seam (§5): factory for the first-party provider this file ships. */
	NS.persistenceProviders = {
		localStorage: createLocalStorageProvider
	};

	/**
	 * The async persistence facade every consumer (the later chooser/config-application slices) calls through -
	 * delegates to whichever provider is currently active.  `list`/`getActive` uniformly apply the
	 * dangling-active resolution (resolveActiveAgainstViews) here, ONCE, rather than duplicating it inside each
	 * provider - both providers' own list() stays a bare {active,views} read.
	 */
	NS.persistence = {

		list: function (table) {
			return activeProvider().list(table).then(function (r) {
				const resolved = resolveActiveAgainstViews(r.active, r.views);
				return { active: resolved.name, views: r.views, dangling: resolved.dangling };
			});
		},

		load: function (table, name) { return activeProvider().load(table, name); },

		save: function (table, name, blob) { return activeProvider().save(table, name, blob); },

		saveAndActivate: function (table, name, blob) { return activeProvider().saveAndActivate(table, name, blob); },

		setActive: function (table, name) { return activeProvider().setActive(table, name); },

		"delete": function (table, name) { return activeProvider()["delete"](table, name); },

		/** Convenience: the resolved active-view NAME (or null for Default), derived uniformly from list(). */
		getActive: function (table) {
			return NS.persistence.list(table).then(function (r) { return { name: r.active, dangling: r.dangling }; });
		}
	};

	/**
	 * Synchronous localStorage get/set for ribbon toggle persistence.  Uses the SAME exact keys
	 * {@code juneau-ribbon.js} already documents ({@code juneau.view.<viewId>.ribbon.<optionId>}) — this is not
	 * the saved-view key codec, and these methods MUST stay synchronous (no Promise).  A missing/blocked
	 * store returns {@code null} / no-ops rather than throwing, so a ribbon click cannot become async-broken.
	 */
	NS.persistence.getItem = function (key) {
		try {
			return window.localStorage.getItem(key);
		} catch (e) { return null; /* quota / private mode — ribbon click path stays synchronous and must not throw */ } // NOSONAR javascript:S2486 -- storage quota or private mode must not throw on the synchronous ribbon click path
	};
	NS.persistence.setItem = function (key, value) {
		try {
			window.localStorage.setItem(key, String(value));
		} catch (e) { /* quota / private mode — ribbon click path stays synchronous and must not throw */ } // NOSONAR javascript:S2486 -- storage quota or private mode must not throw on the synchronous ribbon click path
	};

	// Exposed for the pure-logic/source-shape tests and for later slices (chooser UI, config-application layer).
	NS.config = NS.config || {};
	NS.config.CURRENT_SCHEMA_VERSION = CURRENT_SCHEMA_VERSION;
	NS.config.MAX_NAME_LEN = MAX_NAME_LEN;
	NS.config.MAX_ENCODED_SEGMENT_LEN = MAX_ENCODED_SEGMENT_LEN;
	NS.config.LOCALSTORAGE_MAX_VIEWS_PER_SCOPE = LOCALSTORAGE_MAX_VIEWS_PER_SCOPE;
	NS.config.LOCALSTORAGE_MAX_BLOB_BYTES = LOCALSTORAGE_MAX_BLOB_BYTES;
	NS.config.LOCALSTORAGE_MAX_VIEWS_PER_USER = LOCALSTORAGE_MAX_VIEWS_PER_USER;
	NS.config.PAGE_ID_ATTR = PAGE_ID_ATTR;
	NS.config.VIEW_ID_ATTR = VIEW_ID_ATTR;
	NS.config.encSegment = encSegment;
	NS.config.decSegment = decSegment;
	NS.config.scopeKey = scopeKey;
	NS.config.viewsPrefixKey = viewsPrefixKey;
	NS.config.viewKeyFor = viewKeyFor;
	NS.config.activeKeyFor = activeKeyFor;
	NS.config.isReservedName = isReservedName;
	NS.config.validateNameBasic = validateNameBasic;
	NS.config.validateNameForLocalStorage = validateNameForLocalStorage;
	NS.config.resolveActiveAgainstViews = resolveActiveAgainstViews;
	NS.config.assertSupportedSchema = assertSupportedSchema;
	NS.config.resolvePageId = resolvePageId;
	NS.config.resolveViewId = resolveViewId;
	NS.config.createLocalStorageProvider = createLocalStorageProvider;
	// Slice 4 — pure config-application layer (§4.3) + the dtIndex index model (§4.2 INDEX MODEL only).
	NS.config.validateView = validateView;
	NS.config.computeEffectiveColumns = computeEffectiveColumns;
	NS.config.deserializeSavedView = deserializeSavedView;
	NS.config.serializeSavedView = serializeSavedView;
	NS.config.buildOptsColumnSpace = buildOptsColumnSpace;
	NS.config.dtIndex = dtIndex;
	NS.config.swapRenderId = swapRenderId;
	NS.config.copyCatalogColumn = copyCatalogColumn;

	/**
	 * Loads the active saved-view blob for a configurable table (or {@code null} for Default).  Stale / unknown-
	 * schema blobs resolve as Default rather than rejecting.  Persistence failures reject so {@code initTable}
	 * can refuse the first draw (isDataTable stays false).
	 */
	function resolveActiveView(table, viewDef) {
		if (!NS.persistence) return Promise.resolve(null);
		return NS.persistence.getActive(table).then(function (r) {
			if (r?.name == null) return null;
			return NS.persistence.load(table, r.name).then(function (blob) {
				if (blob == null) return null;
				try { return deserializeSavedView(blob); }
				catch (e) { return null; /* stale/unknown-schema blob resolves as Default, per the doc above */ } // NOSONAR javascript:S2486 -- a stale or unknown-schema blob resolves as Default
			});
		});
	}

	/**
	 * Programmatic Apply entry point (no chooser UI).  Computes effective columns from {@code savedView} and
	 * runs the destroy/reinit transaction via {@code NS.init.buildTable}.  {@code overrides.defaultOrder}, when
	 * present, replaces {@code viewDef.defaultOrder} for this one rebuild (Gap 1 Sort restore - a default ORDER,
	 * not a sortability toggle, so this never touches any column's own {@code orderable} flag).
	 * {@code overrides.searchMembership}, when present, downgrades {@code searchable} on every effective column
	 * absent from it (Gap 1 Search restore), via {@link #applySearchMembershipToColumns}.
	 */
	function applyView(table, savedView, overrides) {
		const ctx = table?.__juneauCtx;
		if (!ctx?.viewDef) return { ok: false, reason: "not-initialized" };
		if (!NS.init || typeof NS.init.buildTable !== "function") return { ok: false, reason: "no-buildTable" };
		let effective;
		try {
			effective = computeEffectiveColumns(ctx.viewDef.columns || [], savedView);
		} catch (e) {
			return { ok: false, reason: "malformed", message: e?.message };
		}
		if (overrides?.searchMembership != null)
			effective = applySearchMembershipToColumns(effective, overrides.searchMembership);
		const viewDef = overrides?.defaultOrder
			? { ...ctx.viewDef, defaultOrder: overrides.defaultOrder }
			: ctx.viewDef;
		return NS.init.buildTable(table, viewDef, effective, ctx);
	}

	NS.config.resolveActiveView = resolveActiveView;
	NS.config.applyView = applyView;

	// ==================================================================================================================
	// VIEW SETTINGS DIALOG  (four tabs — View / Search / Sort / Options, spec §3)
	// ==================================================================================================================
	//
	// XSS HARD RULE: saved-view names and per-column label overrides are user-controlled and are painted into
	// the chooser AND into DataTables header titles.  This origin holds the CSRF token, so a stored-XSS →
	// token-theft → arbitrary-write chain is in scope.  Paint every user-controlled string with textContent /
	// input.value ONLY — never innerHTML / jQuery html(), including the DataTables column title path.

	const CHOOSER_BACKDROP_CLASS = "juneau-config-dialog-backdrop";
	const DEFAULT_VIEW_LABEL = "Default";

	/** Sets el.textContent; the ONLY sanctioned paint path for user-controlled strings. */
	function paintUserText(el, value) {
		if (!el) return;
		el.textContent = value == null ? "" : String(value);
	}

	/** Sets input.value; the ONLY sanctioned paint path for user-controlled strings in form controls. */
	function paintUserInput(el, value) {
		if (!el) return;
		el.value = value == null ? "" : String(value);
	}

	/**
	 * DataTables treats {@code columns.title} as HTML.  Blank every data-column title so a user label
	 * override is never parsed as markup; {@link #paintHeaderTitles} then writes the real label with
	 * {@code textContent}.
	 */
	function sanitizeColumnTitlesForDataTables(cols) {
		(cols || []).forEach(function (c) {
			if (c && !c._juneau)
				c.title = "";
		});
	}

	/**
	 * Paints DataTables header cells from the effective column model using {@code textContent} only.
	 * Synthetic expander / selection / actions headers are skipped (they are unlabeled by design).
	 */
	function paintHeaderTitles(table, effectiveColumns, ctx) {
		if (!table) return;
		const headRow = table.querySelector("thead tr");
		if (!headRow) return;
		const ths = headRow.children;
		let offset = 0;
		if (typeof headRow.querySelector === "function"
				&& (headRow.querySelector(".juneau-view-detail-th") || headRow.querySelector(".juneau-view-detail-control")))
			offset++;
		if (ctx?.selectionState) offset++;
		// DataTables removes a hidden column's header cell, so only the visible columns have a cell to paint.
		const shown = (effectiveColumns || []).filter(function (col) { return col?.visible !== false; });
		shown.forEach(function (col, i) {
			const th = ths[offset + i];
			if (!th) return;
			const label = (col.title != null && String(col.title).trim() !== "") ? String(col.title) : (col.data || "");
			// Paint into DataTables' own title element so its sort control (and the search icon) survive; a
			// header without one (a plain th) is painted whole.
			const titleEl = typeof th.querySelector === "function" ? th.querySelector(".dt-column-title") : null;
			paintUserText(titleEl || th, label);
		});
	}

	function catalogByDataLocal(catalog) {
		const m = Object.create(null);
		(catalog || []).forEach(function (c) { if (c?.data != null) m[c.data] = c; });
		return m;
	}

	// The Options controls (spec §3.4 + Gap 7): page size / text wrap / row density / auto-refresh interval.
	const DEFAULT_PAGE_SIZE = 25;
	const DEFAULT_DENSITY = "comfortable";
	const ALLOWED_DENSITIES = ["compact", "comfortable"];
	const ALLOWED_PAGE_SIZES = [10, 25, 50, 100];
	// 0 means "Off" (falsy, same convention viewDef.pollIntervalMs already uses in wireTablePolling).
	// IRS's exact interval set (decision, 2026-10-01): Off/30s/1m/5m/15m.
	const ALLOWED_AUTO_REFRESH_MS = [0, 30000, 60000, 300000, 900000];

	/** Human label for an {@code ALLOWED_AUTO_REFRESH_MS} value, for the Options tab's <select> (Gap 7). */
	function autoRefreshLabel(ms) {
		switch (ms) {
			case 30000: return "30s";
			case 60000: return "1m";
			case 300000: return "5m";
			case 900000: return "15m";
			default: return "Off";
		}
	}

	/** The Search tab's universe (spec §3.2): columns that CAN be searched — they carry T6 search metadata. */
	function searchCapableColumns(catalog) {
		return (catalog || []).filter(function (c) { return c?.data != null && c.search != null; });
	}

	/** The Sort tab's universe (spec §3.3): columns that CAN be sorted — {@code orderable !== false}. */
	function sortCapableColumns(catalog) {
		return (catalog || []).filter(function (c) { return c?.data != null && c.orderable !== false; });
	}

	/** Default search MEMBERSHIP (spec §3.2) — every search-capable column starts with its header popup on. */
	function defaultSearchMembership(catalog) {
		return searchCapableColumns(catalog).map(function (c) { return c.data; });
	}

	/**
	 * Downgrades an effective column's {@code searchable} flag to {@code false} when it is absent from
	 * {@code searchMembership} (Gap 1 restore / spec §3.2) - never UPGRADES a column the catalog itself marked
	 * {@code searchable: false}, so this can only turn a capable column's header search icon off, never turn an
	 * intrinsically incapable one on.  {@code searchMembership} is {@code null}/{@code undefined}-safe: a missing
	 * list leaves every column's {@code searchable} flag exactly as the catalog declared it (today's unrestricted
	 * behavior), so a caller with no persisted Search facet to restore can pass it through unchanged.
	 */
	function applySearchMembershipToColumns(effectiveColumns, searchMembership) {
		if (searchMembership == null) return effectiveColumns;
		const member = new Set(searchMembership);
		return (effectiveColumns || []).map(function (c) {
			if (c?.data == null || c.searchable === false || member.has(c.data)) return c;
			const copy = {};
			for (const k in c) if (Object.hasOwn(c, k)) copy[k] = c[k];
			copy.searchable = false;
			return copy;
		});
	}

	/**
	 * Intersects a persisted sort list with the given sort-capable key universe, preserving ORDER and each entry's
	 * `dir` (Gap 6 — IRS parity: the Sort tab is an ordered priority list, not an unordered membership set).
	 * Unknown columns and duplicate entries are dropped; a malformed `dir` (anything but `"desc"`) coerces to
	 * `"asc"`. A non-array `sortList` returns every capable key in catalog order, ascending — the same "absent
	 * means everything, in catalog order" default {@link #intersectMembership} uses for search.
	 */
	function intersectSortOrder(sortList, capableKeys) {
		if (!Array.isArray(sortList))
			return capableKeys.map(function (k) { return { column: k, dir: "asc" }; });
		const allowed = Object.create(null);
		capableKeys.forEach(function (k) { allowed[k] = true; });
		const out = [];
		const seen = Object.create(null);
		sortList.forEach(function (e) {
			if (e?.column == null) return;
			const key = String(e.column);
			if (!allowed[key] || seen[key]) return;
			seen[key] = true;
			out.push({ column: key, dir: e.dir === "desc" ? "desc" : "asc" });
		});
		return out;
	}

	/** Default sort ORDER (spec §3.3, Gap 6): every sort-capable column starts in catalog order, ascending. */
	function defaultSortOrder(catalog, defaultOrder) {
		const capable = sortCapableColumns(catalog).map(function (c) { return c.data; });
		// When the caller supplies the view's declared {@code defaultOrder} ([{data,dir}]), seed from exactly that
		// (intersected with the sort-capable columns) so an untouched Apply never replaces it with "every column".
		if (Array.isArray(defaultOrder)) {
			return intersectSortOrder(defaultOrder.map(function (e) {
				return { column: e?.data, dir: e?.dir };
			}), capable);
		}
		return intersectSortOrder(null, capable);
	}

	/** The view's declared {@code defaultOrder} for draft seeding: always an array (empty when the view declares none). */
	function ctxDefaultOrder(ctx) {
		const d = ctx?.viewDef?.defaultOrder;
		return Array.isArray(d) ? d : [];
	}

	/** Catalog-default draft for {@code ctx}, with the Sort facet seeded from {@code viewDef.defaultOrder}. */
	function defaultDraftForCtx(ctx) {
		return defaultDraftFromCatalog(currentCatalog(ctx), ctxDefaultOrder(ctx));
	}

	/** Framework default Options (spec §3.4 + Gap 7) — page size / wrap / density / auto-refresh, nothing else. */
	function defaultOptions() {
		return { pageSize: DEFAULT_PAGE_SIZE, wrap: false, density: DEFAULT_DENSITY, autoRefreshMs: 0 };
	}

	/** Coerces a persisted/edited Options blob to the exactly-four-field, in-range shape (unknown fields dropped). */
	function normalizeOptions(raw) {
		const out = defaultOptions();
		if (raw && typeof raw === "object") {
			if (ALLOWED_PAGE_SIZES.indexOf(raw.pageSize) >= 0) out.pageSize = raw.pageSize;
			if (typeof raw.wrap === "boolean") out.wrap = raw.wrap;
			if (ALLOWED_DENSITIES.indexOf(raw.density) >= 0) out.density = raw.density;
			if (ALLOWED_AUTO_REFRESH_MS.indexOf(raw.autoRefreshMs) >= 0) out.autoRefreshMs = raw.autoRefreshMs;
		}
		return out;
	}

	/** Intersects a persisted membership array with the given capable-column universe (unknown ids dropped). */
	function intersectMembership(membership, capableKeys) {
		if (!Array.isArray(membership)) return capableKeys.slice();
		const allowed = Object.create(null);
		capableKeys.forEach(function (k) { allowed[k] = true; });
		const out = [];
		membership.forEach(function (k) { if (allowed[k] && out.indexOf(k) < 0) out.push(k); });
		return out;
	}

	function defaultDraftFromCatalog(catalog, defaultOrder) {
		const cols = catalog || [];
		const order = [];
		const visible = [];
		cols.forEach(function (c) {
			if (c?.data == null) return;
			order.push(c.data);
			if (c.pinned || c.defaultVisible !== false) visible.push(c.data);
		});
		if (visible.length === 0 && order.length > 0) visible.push(order[0]);
		return {
			visible: visible,
			order: order,
			labels: {},
			formats: {},
			search: defaultSearchMembership(cols),
			sort: defaultSortOrder(cols, defaultOrder),
			options: defaultOptions()
		};
	}

	function draftFromSavedView(catalog, savedView, defaultOrder) {
		if (savedView == null) return defaultDraftFromCatalog(catalog, defaultOrder);
		const validated = validateView(savedView, catalog);
		if (!validated.ok || !validated.view) return defaultDraftFromCatalog(catalog, defaultOrder);
		// Saved-view blobs carry only the View facet; search/sort/options come from the page-state store, so a
		// draft built from a saved view starts those three at their catalog defaults.
		return {
			visible: validated.view.visible.slice(),
			order: validated.view.order.slice(),
			labels: { ...(validated.view.labels || {}) },
			formats: { ...(validated.view.formats || {}) },
			search: defaultSearchMembership(catalog),
			sort: defaultSortOrder(catalog, defaultOrder),
			options: defaultOptions()
		};
	}

	/**
	 * Overlays a persisted View Settings blob (§6.1, page-state store) onto a fresh catalog-default draft: the View
	 * facet is validated through {@link #validateView}, and search/sort membership + Options are intersected /
	 * normalized against the current catalog so a stale blob referencing dropped columns degrades gracefully.
	 */
	function draftFromViewSettings(catalog, settings, defaultOrder) {
		const base = defaultDraftFromCatalog(catalog, defaultOrder);
		if (!settings || typeof settings !== "object" || settings.schemaVersion !== CURRENT_SCHEMA_VERSION) return base;
		const validated = validateView(settings, catalog);
		if (validated.ok && validated.view) {
			base.visible = validated.view.visible.slice();
			base.order = validated.view.order.slice();
			base.labels = { ...(validated.view.labels || {}) };
			base.formats = { ...(validated.view.formats || {}) };
		}
		base.search = intersectMembership(settings.search, defaultSearchMembership(catalog));
		base.sort = Array.isArray(settings.sort)
			? intersectSortOrder(settings.sort, sortCapableColumns(catalog).map(function (c) { return c.data; }))
			: base.sort;
		base.options = normalizeOptions(settings.options);
		return base;
	}

	/**
	 * Reads and validates this table's persisted View Settings exactly once per table lifetime (Gap 1 / §6.1),
	 * memoizing the result on {@code ctx._lastAppliedViewSettings} so neither the dialog's seed block nor the
	 * construction-time restore path (`juneau-views.js` `go()`) re-reads storage, and so the Q1 one-time reset
	 * notice (readViewSettings deletes a version-mismatched blob the first time it is seen) cannot fire twice or
	 * get silently skipped by a second, independent read finding nothing left.  Returns {@code {draft, reset}};
	 * {@code draft} is {@code null} when no View Settings blob exists for this table, so the caller can fall back
	 * to its own named-saved-view handling in that case.
	 */
	function resolveLastAppliedViewSettings(table, ctx) {
		if (ctx._lastAppliedViewSettings !== undefined) return ctx._lastAppliedViewSettings;
		const read = readViewSettings(table);
		const result = {
			draft: read.settings != null ? draftFromViewSettings(currentCatalog(ctx), read.settings, ctxDefaultOrder(ctx)) : null,
			reset: read.reset
		};
		ctx._lastAppliedViewSettings = result;
		return result;
	}

	function snapshotDraft(draft) {
		return JSON.stringify({
			visible: draft.visible,
			order: draft.order,
			labels: draft.labels,
			formats: draft.formats,
			search: draft.search,
			sort: draft.sort,
			options: draft.options
		});
	}

	/** The View Settings blob persisted on Apply (§6.1) — all seven page-state facets, none of the URL facets. */
	function viewSettingsFromDraft(draft) {
		return {
			schemaVersion: CURRENT_SCHEMA_VERSION,
			visible: Array.isArray(draft?.visible) ? draft.visible.slice() : [],
			order: Array.isArray(draft?.order) ? draft.order.slice() : [],
			labels: { ...(draft?.labels || {}) },
			formats: { ...(draft?.formats || {}) },
			search: Array.isArray(draft?.search) ? draft.search.slice() : [],
			sort: Array.isArray(draft?.sort) ? draft.sort.map(function (e) { return { column: e.column, dir: e.dir === "desc" ? "desc" : "asc" }; }) : [],
			options: normalizeOptions(draft?.options)
		};
	}

	// ==================================================================================================================
	// PAGE-STATE STORE BRIDGE  (§6.1/§6.2 — View Settings persist per-table through juneau-pagestate.js, NOT the URL)
	// ==================================================================================================================

	/** The per-table page-state key View Settings live under (distinct from any page-level namespace). */
	const VIEW_SETTINGS_STATE_KEY = "viewSettings";

	/**
	 * The per-table page-state scope for this table, or {@code null} when the page-state store is absent or the
	 * table has no stable view id.  Keyed on the SAME {@code data-juneau-view} id the saved-view scope uses, so two
	 * tables on one page never clobber each other's View Settings (§6.2 per-table keying).
	 */
	function pageStateScopeForTable(table) {
		if (!NS.pageState || typeof NS.pageState.table !== "function") return null;
		const viewId = resolveViewId(table);
		if (viewId == null) return null;
		return NS.pageState.table(viewId);
	}

	/**
	 * Reads this table's last-applied View Settings blob (§6.1). A missing blob reads as {@code {settings:null,
	 * reset:false}}. A present blob whose {@code schemaVersion} does not match {@link #CURRENT_SCHEMA_VERSION} (or
	 * that is not itself a plain object) is discarded outright - deleted from the store, not merely ignored - and
	 * reads as {@code {settings:null, reset:true}}, so the caller can show a one-time reset notice (design §5 / Q1)
	 * instead of silently reverting to catalog defaults.
	 */
	function readViewSettings(table) {
		const scope = pageStateScopeForTable(table);
		if (!scope) return { settings: null, reset: false };
		const blob = scope.get(VIEW_SETTINGS_STATE_KEY);
		if (blob == null) return { settings: null, reset: false };
		if (typeof blob !== "object" || blob.schemaVersion !== CURRENT_SCHEMA_VERSION) {
			scope.remove(VIEW_SETTINGS_STATE_KEY);
			return { settings: null, reset: true };
		}
		return { settings: blob, reset: false };
	}

	/** Persists this table's committed View Settings blob on Apply (§6.1); a no-op when the store is unavailable. */
	function writeViewSettings(table, settings) {
		const scope = pageStateScopeForTable(table);
		if (scope) scope.set(VIEW_SETTINGS_STATE_KEY, settings);
	}

	/**
	 * Open precedence for the shareable URL facet (design §6.3 / T19): {@code ?state=} wins for tab / filters /
	 * sort over any store-held live snapshot.  View Settings never go through this path — they stay in the
	 * page-state store ({@link readViewSettings}/{@link writeViewSettings}) and are never encoded in the URL.
	 * Thin wrapper over {@code JuneauViews.urlState.resolveOpenState} so the views runtime and this config
	 * layer share one call site.
	 */
	function resolveShareableOpenState(urlState, storeLiveState) {
		if (NS.urlState && typeof NS.urlState.resolveOpenState === "function")
			return NS.urlState.resolveOpenState(urlState, storeLiveState);
		return (!urlState || NS.urlState?.isEmptyState?.(urlState))
			? (storeLiveState || null) : urlState;
	}

	/**
	 * Copy-link helper for a host (T17): builds the current shareable URL (always with {@code ?state=}, even under
	 * clean-address) and writes it to the clipboard.  The Copy link toolbar button itself is T20 / JRM — not this
	 * pass — but the live URL this would copy is the same one the address bar sync maintains.
	 */
	function copyShareLink(table) {
		if (typeof NS.init?.copyShareableUrl === "function")
			return NS.init.copyShareableUrl(table, table?.__juneauCtx);
		if (!NS.urlState || typeof NS.urlState.buildShareUrl !== "function")
			return Promise.resolve({ ok: false, url: "" });
		const url = NS.urlState.buildShareUrl(window.location, { tab: null, filters: [], sort: null });
		return NS.urlState.copy(window.navigator, url).then(function (ok) { return { ok: !!ok, url: url }; });
	}

	function visibleCount(draft) {
		return draft?.visible ? draft.visible.length : 0;
	}

	/**
	 * Whether {@code dataKey} may be unchecked.  Pinned columns never; the last remaining visible column never.
	 */
	function canHideColumn(draft, catalog, dataKey) {
		const byData = catalogByDataLocal(catalog);
		const col = byData[dataKey];
		if (col?.pinned) return false;
		if (!draft?.visible) return false;
		if (draft.visible.indexOf(dataKey) < 0) return true;
		return visibleCount(draft) > 1;
	}

	function moveColumn(draft, dataKey, delta) {
		if (!draft || !Array.isArray(draft.order)) return false;
		const i = draft.order.indexOf(dataKey);
		if (i < 0) return false;
		const j = i + delta;
		if (j < 0 || j >= draft.order.length) return false;
		const tmp = draft.order[i];
		draft.order[i] = draft.order[j];
		draft.order[j] = tmp;
		return true;
	}

	/** Moves a sort-list entry (keyed by column) by `delta` positions in `draft.sort`; mirrors {@link #moveColumn}. */
	function moveSortEntry(draft, dataKey, delta) {
		if (!draft || !Array.isArray(draft.sort)) return false;
		const i = draft.sort.findIndex(function (e) { return e?.column === dataKey; });
		if (i < 0) return false;
		const j = i + delta;
		if (j < 0 || j >= draft.sort.length) return false;
		const tmp = draft.sort[i];
		draft.sort[i] = draft.sort[j];
		draft.sort[j] = tmp;
		return true;
	}

	function markDirty(ctx) {
		ctx._configDirty = snapshotDraft(ctx._configDraft) !== ctx._configCleanSnapshot;
		refreshChooserDirty(ctx);
	}

	function refreshChooserDirty(ctx) {
		const el = ctx._configDirtyEl;
		if (!el) return;
		el.hidden = !ctx._configDirty;
		paintUserText(el, ctx._configDirty ? "Unsaved changes" : "");
	}

	function showChooserStatus(ctx, message, isError) {
		const el = ctx._configStatusEl;
		if (!el) return;
		paintUserText(el, message == null ? "" : message);
		el.hidden = !message;
		if (el.classList) el.classList.toggle("juneau-config-status-error", !!isError);
	}

	function closeChooserDialog(ctx) {
		const backdrop = ctx?._configBackdrop;
		backdrop?.remove();
		if (ctx) {
			ctx._configBackdrop = null;
			ctx._configDirtyEl = null;
			ctx._configStatusEl = null;
			ctx._configListEl = null;
			ctx._configSelectEl = null;
			ctx._configSearchListEl = null;
			ctx._configSortListEl = null;
			ctx._configTabButtons = null;
			ctx._configTabBodies = null;
			ctx._configVisibleTabs = null;
			ctx._configResetBtn = null;
		}
	}

	function currentCatalog(ctx) {
		return ctx?.viewDef?.columns ? ctx.viewDef.columns : [];
	}

	function renderChooserColumnList(ctx) {
		const list = ctx._configListEl;
		if (!list) return;
		while (list.firstChild) list.firstChild.remove();
		const catalog = currentCatalog(ctx);
		const byData = catalogByDataLocal(catalog);
		const draft = ctx._configDraft;
		(draft.order || []).forEach(function (dataKey) {
			const col = byData[dataKey];
			if (!col) return;
			list.appendChild(buildChooserRow(ctx, col, draft));
		});
	}

	function buildChooserRow(ctx, col, draft) {
		const row = document.createElement("div");
		row.className = "juneau-config-col-row";
		row.dataset.col = col.data;

		const vis = document.createElement("input");
		vis.type = "checkbox";
		vis.className = "juneau-config-col-vis";
		vis.checked = draft.visible.indexOf(col.data) >= 0;
		vis.disabled = !!col.pinned || (!canHideColumn(draft, currentCatalog(ctx), col.data) && vis.checked);
		vis.setAttribute("aria-label", "Show column " + (col.title || col.data));
		vis.addEventListener("change", function () {
			if (vis.checked) {
				if (draft.visible.indexOf(col.data) < 0) draft.visible.push(col.data);
			} else {
				if (!canHideColumn(draft, currentCatalog(ctx), col.data)) {
					vis.checked = true;
					return;
				}
				draft.visible = draft.visible.filter(function (id) { return id !== col.data; });
			}
			markDirty(ctx);
			renderChooserColumnList(ctx);
		});
		row.appendChild(vis);

		const name = document.createElement("span");
		name.className = "juneau-config-col-name";
		paintUserText(name, col.title || col.data);
		if (col.pinned) {
			const pin = document.createElement("span");
			pin.className = "juneau-config-col-pinned";
			paintUserText(pin, " pinned");
			name.appendChild(pin);
		}
		row.appendChild(name);

		const up = document.createElement("button");
		up.type = "button";
		up.className = "juneau-config-col-move";
		paintUserText(up, "Up");
		up.setAttribute("aria-label", "Move column up " + (col.title || col.data));
		up.disabled = draft.order.indexOf(col.data) === 0;
		up.addEventListener("click", function () {
			if (moveColumn(draft, col.data, -1)) {
				markDirty(ctx);
				renderChooserColumnList(ctx);
			}
		});
		row.appendChild(up);

		const down = document.createElement("button");
		down.type = "button";
		down.className = "juneau-config-col-move";
		paintUserText(down, "Down");
		down.setAttribute("aria-label", "Move column down " + (col.title || col.data));
		down.disabled = draft.order.indexOf(col.data) === draft.order.length - 1;
		down.addEventListener("click", function () {
			if (moveColumn(draft, col.data, 1)) {
				markDirty(ctx);
				renderChooserColumnList(ctx);
			}
		});
		row.appendChild(down);

		const label = document.createElement("input");
		label.type = "text";
		label.className = "juneau-config-col-label";
		label.setAttribute("aria-label", "Column label for " + (col.title || col.data));
		paintUserInput(label, draft.labels[col.data] || "");
		label.placeholder = col.title || col.data;
		label.addEventListener("input", function () {
			const v = label.value;
			if (v == null || String(v).trim() === "") delete draft.labels[col.data];
			else draft.labels[col.data] = String(v);
			markDirty(ctx);
		});
		row.appendChild(label);

		if (Array.isArray(col.formats) && col.formats.length) {
			const sel = document.createElement("select");
			sel.className = "juneau-config-col-format";
			sel.setAttribute("aria-label", "Column format for " + (col.title || col.data));
			const empty = document.createElement("option");
			empty.value = "";
			paintUserText(empty, "(default)");
			sel.appendChild(empty);
			col.formats.forEach(function (fmt) {
				const opt = document.createElement("option");
				opt.value = fmt;
				paintUserText(opt, fmt);
				sel.appendChild(opt);
			});
			sel.value = draft.formats[col.data] || "";
			sel.addEventListener("change", function () {
				if (!sel.value) delete draft.formats[col.data];
				else draft.formats[col.data] = sel.value;
				markDirty(ctx);
			});
			row.appendChild(sel);
		}

		return row;
	}

	function fillViewSelect(ctx, listing) {
		const sel = ctx._configSelectEl;
		if (!sel) return;
		while (sel.firstChild) sel.firstChild.remove();
		const defOpt = document.createElement("option");
		defOpt.value = "";
		paintUserText(defOpt, DEFAULT_VIEW_LABEL);
		sel.appendChild(defOpt);
		const views = listing?.views ? listing.views : [];
		views.forEach(function (v) {
			const n = (v?.name != null) ? String(v.name) : String(v);
			const opt = document.createElement("option");
			opt.value = n;
			paintUserText(opt, n);
			sel.appendChild(opt);
		});
		const active = ctx._configActiveName;
		sel.value = active == null ? "" : active;
	}

	function askSaveAsName() {
		if (typeof NS.config.askSaveAsName === "function")
			return NS.config.askSaveAsName();
		if (typeof window.prompt === "function")
			return window.prompt("Save view as:");
		return null;
	}

	function confirmDiscard(ctx) {
		if (!ctx._configDirty) return true;
		if (typeof NS.config.confirmDiscard === "function")
			return !!NS.config.confirmDiscard();
		if (typeof window.confirm === "function")
			return window.confirm("Discard unsaved changes?");
		return true;
	}

	function applyDraft(table, ctx) {
		const saved = {
			schemaVersion: CURRENT_SCHEMA_VERSION,
			visible: ctx._configDraft.visible.slice(),
			order: ctx._configDraft.order.slice(),
			labels: { ...ctx._configDraft.labels },
			formats: { ...ctx._configDraft.formats }
		};
		const draft = ctx._configDraft;
		const visible = visibleConfigTabs(ctx.viewDef);
		const tabOn = function (tab) { return visible.indexOf(tab) >= 0; };
		const result = applyView(table, saved, {
			defaultOrder: (tabOn("sort") && draft.sort?.length)
				? draft.sort.map(function (e) { return { data: e.column, dir: e.dir }; })
				: undefined,
			searchMembership: tabOn("search") ? draft.search : undefined
		});
		if (result?.ok) {
			// Apply COMMITS the View Settings to the page-state store (§6.1) so a reload restores them per-table,
			// AND (Gap 1) applies search/sort/options to the live grid itself, the same way construction-time
			// restore does - no more "nothing visibly changes until the next reload."
			if (tabOn("options") && typeof NS.init?.applyRestoredOptionsToLiveGrid === "function")
				NS.init.applyRestoredOptionsToLiveGrid(table, ctx, draft.options);
			writeViewSettings(table, viewSettingsFromDraft(ctx._configDraft));
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			refreshChooserDirty(ctx);
			showChooserStatus(ctx, "", false);
		} else if (result?.reason === "in-flight") {
			showChooserStatus(ctx, "Finish the in-progress action first.", true);
		} else if (result && !result.ok) {
			showChooserStatus(ctx, result.message || "Could not apply view.", true);
		}
		return result;
	}

	function persistDraft(table, ctx, name, activate) {
		const blob = serializeSavedView(ctx._configDraft);
		const op = activate ? NS.persistence.saveAndActivate : NS.persistence.save;
		return op(table, name, blob).then(function () {
			ctx._configActiveName = name;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			refreshChooserDirty(ctx);
			showChooserStatus(ctx, activate ? "Saved and applied." : "Saved.", false);
			return NS.persistence.list(table).then(function (listing) {
				fillViewSelect(ctx, listing);
			});
		}, function (e) {
			const typed = toTypedError(e);
			const msg = typed.code === "quota" ? "Storage quota exceeded." : (typed.message || "Save failed.");
			showChooserStatus(ctx, msg, true);
			throw e;
		});
	}

	function loadNamedView(table, ctx, name) {
		if (name == null || name === "") {
			ctx._configDraft = defaultDraftForCtx(ctx);
			ctx._configActiveName = null;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			renderChooserColumnList(ctx);
			refreshChooserDirty(ctx);
			return Promise.resolve();
		}
		return NS.persistence.load(table, name).then(function (blob) {
			ctx._configDraft = draftFromSavedView(currentCatalog(ctx), blob, ctxDefaultOrder(ctx));
			ctx._configActiveName = name;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			renderChooserColumnList(ctx);
			refreshChooserDirty(ctx);
		}, function (e) {
			showChooserStatus(ctx, (toTypedError(e).message) || "Load failed.", true);
		});
	}

	/** The four View Settings tabs (spec §3), in header order. */
	const CONFIG_TABS = ["view", "search", "sort", "options"];
	const CONFIG_TAB_LABELS = { view: "View", search: "Search", sort: "Sort", options: "Options" };

	/** Bumped once per {@code openChooser} call so each dialog's tab/panel ids are unique on the page (F3). */
	let configDialogSeq = 0;

	/** Public JSON key per internal tab id (Gap-g): `columnConfig.tabs` lists these public names. */
	const CONFIG_TAB_PUBLIC_KEYS = { view: "columns", search: "search", sort: "sort", options: "options" };

	/**
	 * Resolves which of the four tabs {@code viewDef.columnConfig} allows (Gap-g, IRS parity).
	 * {@code columnConfig: true} (or any other non-object truthy value - the pre-existing default) means "all four
	 * tabs", in {@link #CONFIG_TABS}'s fixed order regardless of the order names are given in. {@code columnConfig:
	 * {tabs: [...]}} restricts to the named public keys; unknown names are dropped, and an empty or all-unknown
	 * {@code tabs} array falls back to "all four tabs" rather than "no tabs" (an accidental {@code {tabs: []}} must
	 * not silently produce a chooser with nothing in it).
	 */
	function visibleConfigTabs(viewDef) {
		const cc = viewDef?.columnConfig;
		if (!cc || typeof cc !== "object" || !Array.isArray(cc.tabs)) return CONFIG_TABS.slice();
		const wanted = Object.create(null);
		cc.tabs.forEach(function (name) { wanted[name] = true; });
		const out = CONFIG_TABS.filter(function (t) { return wanted[CONFIG_TAB_PUBLIC_KEYS[t]]; });
		return out.length ? out : CONFIG_TABS.slice();
	}

	/**
	 * Shows one tab body and hides the rest, moves the active class onto its button, updates each tab button's
	 * {@code aria-selected} and roving {@code tabindex} (F3 - only the active tab is {@code tabindex="0"}; the
	 * rest are {@code tabindex="-1"}, per the WAI-ARIA APG tabs pattern), and reveals the View-tab-only Reset
	 * control (spec §3 footer: Apply / Cancel always; Reset to defaults on the View tab).
	 */
	function selectConfigTab(ctx, name) {
		ctx._configActiveTab = name;
		const buttons = ctx._configTabButtons || {};
		const bodies = ctx._configTabBodies || {};
		(ctx._configVisibleTabs || CONFIG_TABS).forEach(function (t) {
			const btn = buttons[t];
			if (btn?.classList) btn.classList.toggle("juneau-config-tab-active", t === name);
			if (btn) {
				btn.setAttribute("aria-selected", t === name ? "true" : "false");
				btn.tabIndex = t === name ? 0 : -1;
			}
			const body = bodies[t];
			if (body) body.hidden = t !== name;
		});
		if (ctx._configResetBtn) ctx._configResetBtn.hidden = name !== "view";
	}

	/**
	 * One membership checkbox row (spec §3.2/§3.3): a checkbox toggling {@code dataKey}'s presence in the draft's
	 * {@code facetKey} array, plus the (user-controlled) column label painted with {@code textContent} only.
	 */
	function buildMembershipRow(ctx, col, facetKey) {
		const row = document.createElement("div");
		row.className = "juneau-config-member-row";
		row.dataset.col = col.data;

		const cb = document.createElement("input");
		cb.type = "checkbox";
		cb.className = "juneau-config-member-toggle";
		const draftList = ctx._configDraft[facetKey] || (ctx._configDraft[facetKey] = []);
		cb.checked = draftList.indexOf(col.data) >= 0;
		cb.setAttribute("aria-label", (facetKey === "search" ? "Searchable " : "Sortable ") + (col.title || col.data));
		cb.addEventListener("change", function () {
			const list = ctx._configDraft[facetKey] || (ctx._configDraft[facetKey] = []);
			if (cb.checked) {
				if (list.indexOf(col.data) < 0) list.push(col.data);
			} else {
				ctx._configDraft[facetKey] = list.filter(function (id) { return id !== col.data; });
			}
			markDirty(ctx);
		});
		row.appendChild(cb);

		const name = document.createElement("span");
		name.className = "juneau-config-member-name";
		paintUserText(name, col.title || col.data);
		row.appendChild(name);
		return row;
	}

	/** Fills {@code listEl} with a membership row per capable column; a facet with no capable columns shows a note. */
	function renderMembershipList(ctx, listEl, capableColumns, facetKey) {
		if (!listEl) return;
		while (listEl.firstChild) listEl.firstChild.remove();
		if (!capableColumns.length) {
			const note = document.createElement("div");
			note.className = "juneau-config-empty-note";
			paintUserText(note, facetKey === "search" ? "No searchable columns." : "No sortable columns.");
			listEl.appendChild(note);
			return;
		}
		capableColumns.forEach(function (col) {
			listEl.appendChild(buildMembershipRow(ctx, col, facetKey));
		});
	}

	/**
	 * One ordered Sort-tab row (Gap 6): a checkbox toggling membership (unchecking removes it from `draft.sort`
	 * entirely - {@link #buildUnsortedRow} re-adds it at the tail when re-checked), an Up/Down reorder pair, and
	 * an Asc/Desc select - all draft-mutating, mirroring {@link #buildChooserRow}'s reorder convention.
	 */
	function buildSortRow(ctx, col, draft, index, count) {
		const row = document.createElement("div");
		row.className = "juneau-config-sort-row";
		row.dataset.col = col.data;

		const cb = document.createElement("input");
		cb.type = "checkbox";
		cb.className = "juneau-config-sort-toggle";
		cb.checked = true;
		cb.setAttribute("aria-label", "Sortable " + (col.title || col.data));
		cb.addEventListener("change", function () {
			if (!cb.checked) draft.sort = draft.sort.filter(function (e) { return e.column !== col.data; });
			markDirty(ctx);
			renderSortList(ctx);
		});
		row.appendChild(cb);

		const name = document.createElement("span");
		name.className = "juneau-config-sort-name";
		paintUserText(name, col.title || col.data);
		row.appendChild(name);

		const dirSel = document.createElement("select");
		dirSel.className = "juneau-config-sort-dir";
		dirSel.setAttribute("aria-label", "Sort direction for " + (col.title || col.data));
		["asc", "desc"].forEach(function (d) {
			const opt = document.createElement("option");
			opt.value = d;
			paintUserText(opt, d === "asc" ? "Ascending" : "Descending");
			dirSel.appendChild(opt);
		});
		const entry = draft.sort.find(function (e) { return e.column === col.data; });
		dirSel.value = entry ? entry.dir : "asc";
		dirSel.addEventListener("change", function () {
			const e = draft.sort.find(function (e2) { return e2.column === col.data; });
			if (e) { e.dir = dirSel.value === "desc" ? "desc" : "asc"; markDirty(ctx); }
		});
		row.appendChild(dirSel);

		const up = document.createElement("button");
		up.type = "button";
		up.className = "juneau-config-sort-move";
		paintUserText(up, "Up");
		up.setAttribute("aria-label", "Move sort priority up " + (col.title || col.data));
		up.disabled = index === 0;
		up.addEventListener("click", function () {
			if (moveSortEntry(draft, col.data, -1)) { markDirty(ctx); renderSortList(ctx); }
		});
		row.appendChild(up);

		const down = document.createElement("button");
		down.type = "button";
		down.className = "juneau-config-sort-move";
		paintUserText(down, "Down");
		down.setAttribute("aria-label", "Move sort priority down " + (col.title || col.data));
		down.disabled = index === count - 1;
		down.addEventListener("click", function () {
			if (moveSortEntry(draft, col.data, 1)) { markDirty(ctx); renderSortList(ctx); }
		});
		row.appendChild(down);

		return row;
	}

	/** A sort-capable column NOT currently in `draft.sort`: an unchecked row with no reorder/dir controls. */
	function buildUnsortedRow(ctx, col, draft) {
		const row = document.createElement("div");
		row.className = "juneau-config-sort-row juneau-config-sort-row-unsorted";
		row.dataset.col = col.data;

		const cb = document.createElement("input");
		cb.type = "checkbox";
		cb.className = "juneau-config-sort-toggle";
		cb.checked = false;
		cb.setAttribute("aria-label", "Sortable " + (col.title || col.data));
		cb.addEventListener("change", function () {
			if (cb.checked) draft.sort.push({ column: col.data, dir: "asc" });
			markDirty(ctx);
			renderSortList(ctx);
		});
		row.appendChild(cb);

		const name = document.createElement("span");
		name.className = "juneau-config-sort-name";
		paintUserText(name, col.title || col.data);
		row.appendChild(name);

		return row;
	}

	/**
	 * Fills the Sort tab (Gap 6 - IRS parity: an ORDERED priority list, not an unordered membership set) from the
	 * current draft: `draft.sort` entries first, in their current order, each with reorder + Asc/Desc controls;
	 * then any remaining sort-capable column not yet in the list, unchecked, appended to the tail of `draft.sort`
	 * the moment its checkbox is ticked.
	 */
	function renderSortList(ctx) {
		const list = ctx._configSortListEl;
		if (!list) return;
		while (list.firstChild) list.firstChild.remove();
		const draft = ctx._configDraft;
		const capable = sortCapableColumns(currentCatalog(ctx));
		if (!capable.length) {
			const note = document.createElement("div");
			note.className = "juneau-config-empty-note";
			paintUserText(note, "No sortable columns.");
			list.appendChild(note);
			return;
		}
		const byData = catalogByDataLocal(capable);
		const inList = draft.sort.filter(function (e) { return byData[e.column]; });
		const remaining = capable.filter(function (c) {
			return inList.every(function (e) { return e.column !== c.data; });
		});
		inList.forEach(function (e, i) {
			list.appendChild(buildSortRow(ctx, byData[e.column], draft, i, inList.length));
		});
		remaining.forEach(function (c) {
			list.appendChild(buildUnsortedRow(ctx, c, draft));
		});
	}

	/** The Options tab body (spec §3.4): EXACTLY four controls — page size, text wrap, row density, auto-refresh. */
	function buildOptionsTabBody(ctx) {
		const body = document.createElement("div");
		body.className = "juneau-config-options";
		const opts = ctx._configDraft.options || (ctx._configDraft.options = defaultOptions());

		const pageRow = document.createElement("label");
		pageRow.className = "juneau-config-opt juneau-config-opt-pagesize";
		const pageLbl = document.createElement("span");
		paintUserText(pageLbl, "Page size");
		pageRow.appendChild(pageLbl);
		const pageSel = document.createElement("select");
		pageSel.setAttribute("aria-label", "Page size");
		ALLOWED_PAGE_SIZES.forEach(function (n) {
			const opt = document.createElement("option");
			opt.value = String(n);
			paintUserText(opt, String(n));
			pageSel.appendChild(opt);
		});
		pageSel.value = String(opts.pageSize);
		pageSel.addEventListener("change", function () {
			const n = Number.parseInt(pageSel.value, 10);
			if (ALLOWED_PAGE_SIZES.indexOf(n) >= 0) { ctx._configDraft.options.pageSize = n; markDirty(ctx); }
		});
		pageRow.appendChild(pageSel);
		body.appendChild(pageRow);

		const wrapRow = document.createElement("label");
		wrapRow.className = "juneau-config-opt juneau-config-opt-wrap";
		const wrapCb = document.createElement("input");
		wrapCb.type = "checkbox";
		wrapCb.setAttribute("aria-label", "Text wrap");
		wrapCb.checked = !!opts.wrap;
		wrapCb.addEventListener("change", function () { ctx._configDraft.options.wrap = !!wrapCb.checked; markDirty(ctx); });
		wrapRow.appendChild(wrapCb);
		const wrapLbl = document.createElement("span");
		paintUserText(wrapLbl, "Text wrap");
		wrapRow.appendChild(wrapLbl);
		body.appendChild(wrapRow);

		const densRow = document.createElement("label");
		densRow.className = "juneau-config-opt juneau-config-opt-density";
		const densLbl = document.createElement("span");
		paintUserText(densLbl, "Row density");
		densRow.appendChild(densLbl);
		const densSel = document.createElement("select");
		densSel.setAttribute("aria-label", "Row density");
		ALLOWED_DENSITIES.forEach(function (d) {
			const opt = document.createElement("option");
			opt.value = d;
			paintUserText(opt, d.charAt(0).toUpperCase() + d.slice(1));
			densSel.appendChild(opt);
		});
		densSel.value = opts.density;
		densSel.addEventListener("change", function () {
			if (ALLOWED_DENSITIES.indexOf(densSel.value) >= 0) { ctx._configDraft.options.density = densSel.value; markDirty(ctx); }
		});
		densRow.appendChild(densSel);
		body.appendChild(densRow);

		const refreshRow = document.createElement("label");
		refreshRow.className = "juneau-config-opt juneau-config-opt-autorefresh";
		const refreshLbl = document.createElement("span");
		paintUserText(refreshLbl, "Auto-refresh");
		refreshRow.appendChild(refreshLbl);
		const refreshSel = document.createElement("select");
		refreshSel.setAttribute("aria-label", "Auto-refresh");
		ALLOWED_AUTO_REFRESH_MS.forEach(function (ms) {
			const opt = document.createElement("option");
			opt.value = String(ms);
			paintUserText(opt, autoRefreshLabel(ms));
			refreshSel.appendChild(opt);
		});
		refreshSel.value = String(opts.autoRefreshMs);
		refreshSel.addEventListener("change", function () {
			const ms = Number.parseInt(refreshSel.value, 10);
			if (ALLOWED_AUTO_REFRESH_MS.indexOf(ms) >= 0) { ctx._configDraft.options.autoRefreshMs = ms; markDirty(ctx); }
		});
		refreshRow.appendChild(refreshSel);
		body.appendChild(refreshRow);

		return body;
	}

	/** Rebuilds all three new tab bodies' contents from the current draft (called on open AND on Reset). */
	function renderConfigTabBodies(ctx) {
		renderChooserColumnList(ctx);
		const bodies = ctx._configTabBodies || {};
		renderMembershipList(ctx, ctx._configSearchListEl, searchCapableColumns(currentCatalog(ctx)), "search");
		renderSortList(ctx);
		// Options is a fresh control set each render so it reflects the current draft.options.
		const optHost = bodies.options;
		if (optHost) {
			while (optHost.firstChild) optHost.firstChild.remove();
			optHost.appendChild(buildOptionsTabBody(ctx));
		}
	}

	/** Reset to defaults (spec §3.1) — drafts the catalog defaults; Apply still commits, Cancel abandons. */
	function resetDraftToDefaults(ctx) {
		ctx._configDraft = defaultDraftForCtx(ctx);
		renderConfigTabBodies(ctx);
		markDirty(ctx);
	}

	function openChooser(table, ctx) {
		if (ctx._configBackdrop) {
			closeChooserDialog(ctx);
			return;
		}
		if (!ctx._configDraft) {
			// Seed the dialog from the last-applied View Settings (§6.1) when present, else catalog defaults.  A
			// schemaVersion mismatch (or missing/non-object blob) discards the whole blob and shows a one-time
			// notice (design §5 / Q1) rather than silently reverting; readViewSettings deleting the blob on
			// detection is what makes the notice "once" - a later re-open finds nothing stored, not a mismatch.
			// Routed through the shared helper (Gap 1) so this is the SAME read `go()`'s construction-time restore
			// already consumed - never a second independent read of the same blob.
			const resolved = resolveLastAppliedViewSettings(table, ctx);
			ctx._configDraft = resolved.draft != null
				? resolved.draft
				: draftFromSavedView(currentCatalog(ctx), null, ctxDefaultOrder(ctx));
			ctx._configResetNotice = resolved.reset;
		}
		if (ctx._configCleanSnapshot == null)
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
		ctx._configVisibleTabs = visibleConfigTabs(ctx.viewDef);

		const backdrop = document.createElement("div");
		backdrop.className = CHOOSER_BACKDROP_CLASS;
		backdrop.setAttribute("role", "presentation");

		const dialog = document.createElement("div");
		dialog.className = "juneau-config-dialog";
		dialog.setAttribute("role", "dialog");
		dialog.setAttribute("aria-labelledby", "juneau-config-title");

		const title = document.createElement("h2");
		title.id = "juneau-config-title";
		title.className = "juneau-config-title";
		paintUserText(title, "View Settings");
		dialog.appendChild(title);

		const dialogIdPrefix = "juneau-config-" + (++configDialogSeq) + "-";
		const tabIds = {};
		ctx._configVisibleTabs.forEach(function (t) {
			tabIds[t] = { tab: dialogIdPrefix + "tab-" + t, panel: dialogIdPrefix + "panel-" + t };
		});

		const tabs = document.createElement("div");
		tabs.className = "juneau-config-tabs";
		tabs.setAttribute("role", "tablist");
		ctx._configTabButtons = {};
		ctx._configTabBodies = {};
		ctx._configVisibleTabs.forEach(function (t) {
			const btn = document.createElement("button");
			btn.type = "button";
			btn.id = tabIds[t].tab;
			btn.className = "juneau-config-tab";
			btn.setAttribute("role", "tab");
			btn.setAttribute("aria-controls", tabIds[t].panel);
			btn.setAttribute("aria-selected", "false");
			btn.tabIndex = -1;
			btn.dataset.tab = t;
			paintUserText(btn, CONFIG_TAB_LABELS[t]);
			btn.addEventListener("click", function () { selectConfigTab(ctx, t); btn.focus(); });
			ctx._configTabButtons[t] = btn;
			tabs.appendChild(btn);
		});
		// F3 - Left/Right/Home/End roving-tabindex keyboard nav (WAI-ARIA APG tabs pattern), reusing the same
		// pure target-index function the detail-view ribbon strip's own tab keydown handler uses.  Gap-g - the
		// wraparound/indexing math runs over the VISIBLE tabs only, so a restricted dialog never lands on, or
		// wraps through, a tab it never built.
		tabs.addEventListener("keydown", function (e) {
			if (!NS.init || typeof NS.init.detailTabTargetIndex !== "function") return;
			const current = ctx._configVisibleTabs.indexOf(ctx._configActiveTab);
			const target = NS.init.detailTabTargetIndex(e.key, current, ctx._configVisibleTabs.length);
			if (target < 0) return;
			e.preventDefault?.();
			const name = ctx._configVisibleTabs[target];
			selectConfigTab(ctx, name);
			ctx._configTabButtons[name]?.focus();
		});
		dialog.appendChild(tabs);

		const bodies = document.createElement("div");
		bodies.className = "juneau-config-bodies";
		dialog.appendChild(bodies);

		const visibleTabs = ctx._configVisibleTabs;
		if (visibleTabs.indexOf("view") >= 0) {
			const viewBody = document.createElement("div");
			viewBody.className = "juneau-config-body juneau-config-body-view";
			viewBody.id = tabIds.view.panel;
			viewBody.setAttribute("role", "tabpanel");
			viewBody.setAttribute("aria-labelledby", tabIds.view.tab);
			ctx._configTabBodies.view = viewBody;
			bodies.appendChild(viewBody);

			const toolbar = document.createElement("div");
			toolbar.className = "juneau-config-saved-bar";

			const sel = document.createElement("select");
			sel.className = "juneau-config-view-select";
			sel.setAttribute("aria-label", "Saved view");
			ctx._configSelectEl = sel;
			sel.addEventListener("change", function () {
				if (!confirmDiscard(ctx)) {
					sel.value = ctx._configActiveName == null ? "" : ctx._configActiveName;
					return;
				}
				const name = sel.value === "" ? null : sel.value;
				loadNamedView(table, ctx, name).then(function () {
					NS.persistence.setActive(table, name).catch(function () { /* quota / private mode — the
						chooser selection itself already applied; persisting it across reloads is best-effort */ });
				});
			});
			toolbar.appendChild(sel);

			const saveBtn = document.createElement("button");
			saveBtn.type = "button";
			paintUserText(saveBtn, "Save");
			saveBtn.addEventListener("click", function () {
				if (ctx._configActiveName == null) {
					showChooserStatus(ctx, "Use Save as… to name a new view.", true);
					return;
				}
				persistDraft(table, ctx, ctx._configActiveName, true);
			});
			toolbar.appendChild(saveBtn);

			const saveAsBtn = document.createElement("button");
			saveAsBtn.type = "button";
			paintUserText(saveAsBtn, "Save as…");
			saveAsBtn.addEventListener("click", function () {
				const name = askSaveAsName();
				if (name == null || String(name).trim() === "") return;
				const basic = validateNameBasic(name);
				if (!basic.ok) {
					showChooserStatus(ctx, basic.message, true);
					return;
				}
				persistDraft(table, ctx, String(name).trim(), true);
			});
			toolbar.appendChild(saveAsBtn);

			const delBtn = document.createElement("button");
			delBtn.type = "button";
			paintUserText(delBtn, "Delete");
			delBtn.addEventListener("click", function () {
				if (ctx._configActiveName == null) {
					showChooserStatus(ctx, "The Default view cannot be deleted.", true);
					return;
				}
				const name = ctx._configActiveName;
				NS.persistence["delete"](table, name).then(function () {
					return NS.persistence.setActive(table, null);
				}).then(function () {
					ctx._configActiveName = null;
					ctx._configDraft = defaultDraftForCtx(ctx);
					ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
					ctx._configDirty = false;
					renderChooserColumnList(ctx);
					refreshChooserDirty(ctx);
					return NS.persistence.list(table);
				}).then(function (listing) {
					fillViewSelect(ctx, listing);
					showChooserStatus(ctx, "Deleted.", false);
				}, function (e) {
					showChooserStatus(ctx, (toTypedError(e).message) || "Delete failed.", true);
				});
			});
			toolbar.appendChild(delBtn);

			const dirty = document.createElement("span");
			dirty.className = "juneau-config-dirty";
			dirty.hidden = true;
			ctx._configDirtyEl = dirty;
			toolbar.appendChild(dirty);

			viewBody.appendChild(toolbar);

			const list = document.createElement("div");
			list.className = "juneau-config-col-list";
			ctx._configListEl = list;
			viewBody.appendChild(list);
		}

		if (visibleTabs.indexOf("search") >= 0) {
			// Search tab (spec §3.2) — membership only; operator editing lives in the header popup (§4).
			const searchBody = document.createElement("div");
			searchBody.className = "juneau-config-body juneau-config-body-search";
			searchBody.id = tabIds.search.panel;
			searchBody.setAttribute("role", "tabpanel");
			searchBody.setAttribute("aria-labelledby", tabIds.search.tab);
			searchBody.hidden = true;
			const searchList = document.createElement("div");
			searchList.className = "juneau-config-member-list juneau-config-search-list";
			ctx._configSearchListEl = searchList;
			searchBody.appendChild(searchList);
			ctx._configTabBodies.search = searchBody;
			bodies.appendChild(searchBody);
		}

		if (visibleTabs.indexOf("sort") >= 0) {
			// Sort tab (spec §3.3) — an ordered, directional priority list (Gap 6).
			const sortBody = document.createElement("div");
			sortBody.className = "juneau-config-body juneau-config-body-sort";
			sortBody.id = tabIds.sort.panel;
			sortBody.setAttribute("role", "tabpanel");
			sortBody.setAttribute("aria-labelledby", tabIds.sort.tab);
			sortBody.hidden = true;
			const sortList = document.createElement("div");
			sortList.className = "juneau-config-member-list juneau-config-sort-list";
			ctx._configSortListEl = sortList;
			sortBody.appendChild(sortList);
			ctx._configTabBodies.sort = sortBody;
			bodies.appendChild(sortBody);
		}

		if (visibleTabs.indexOf("options") >= 0) {
			// Options tab (spec §3.4) — page size / wrap / density only.
			const optionsBody = document.createElement("div");
			optionsBody.className = "juneau-config-body juneau-config-body-options";
			optionsBody.id = tabIds.options.panel;
			optionsBody.setAttribute("role", "tabpanel");
			optionsBody.setAttribute("aria-labelledby", tabIds.options.tab);
			optionsBody.hidden = true;
			ctx._configTabBodies.options = optionsBody;
			bodies.appendChild(optionsBody);
		}

		const status = document.createElement("div");
		status.className = "juneau-config-status";
		status.setAttribute("role", "status");
		status.setAttribute("aria-live", "polite");
		status.hidden = true;
		ctx._configStatusEl = status;
		dialog.appendChild(status);

		const actions = document.createElement("div");
		actions.className = "juneau-config-actions";

		const applyBtn = document.createElement("button");
		applyBtn.type = "button";
		applyBtn.className = "juneau-config-apply";
		paintUserText(applyBtn, "Apply");
		applyBtn.addEventListener("click", function () { applyDraft(table, ctx); });
		actions.appendChild(applyBtn);

		const resetBtn = document.createElement("button");
		resetBtn.type = "button";
		resetBtn.className = "juneau-config-reset";
		paintUserText(resetBtn, "Reset to defaults");
		resetBtn.addEventListener("click", function () { resetDraftToDefaults(ctx); });
		ctx._configResetBtn = resetBtn;
		actions.appendChild(resetBtn);

		const closeBtn = document.createElement("button");
		closeBtn.type = "button";
		paintUserText(closeBtn, "Cancel");
		closeBtn.addEventListener("click", function () {
			if (!confirmDiscard(ctx)) return;
			closeChooserDialog(ctx);
		});
		actions.appendChild(closeBtn);

		dialog.appendChild(actions);
		backdrop.appendChild(dialog);
		backdrop.addEventListener("click", function (e) {
			if (e.target === backdrop) {
				if (!confirmDiscard(ctx)) return;
				closeChooserDialog(ctx);
			}
		});
		document.body.appendChild(backdrop);
		ctx._configBackdrop = backdrop;

		renderConfigTabBodies(ctx);
		selectConfigTab(ctx, ctx._configVisibleTabs[0]);
		refreshChooserDirty(ctx);
		if (ctx._configResetNotice) {
			showChooserStatus(ctx, "Saved view settings were reset because the table changed.", false);
			ctx._configResetNotice = false;
		}
		NS.persistence.list(table).then(function (listing) {
			if (ctx._configActiveName === undefined)
				ctx._configActiveName = listing.active;
			fillViewSelect(ctx, listing);
		}, function () {
			fillViewSelect(ctx, { views: [] });
		});
	}

	/** Resolves the toolbar host the chooser button mounts into, or `null` when there is nowhere to mount it. */
	function resolveChooserHost(table, toolbarRow) {
		if (toolbarRow)
			return toolbarRow.querySelector(".juneau-view-toolbar-right") || toolbarRow;
		const wrapper = table?.parentNode;
		if (!wrapper) return null;
		return wrapper.querySelector(".juneau-view-toolbar-right") || wrapper;
	}

	/**
	 * Replaces `btn`'s text content with the parsed `<svg>` markup, falling back to (leaving) the text label when
	 * `markup` is absent/unparseable - mirrors the "unregistered icon -> render title as text" convention.
	 */
	function paintChooserIcon(btn, markup) {
		if (markup == null || typeof DOMParser !== "function") return;
		try {
			const doc = new DOMParser().parseFromString(markup, "image/svg+xml");
			const svg = doc.documentElement;
			if (svg?.tagName?.toLowerCase() === "svg") {
				btn.textContent = "";
				btn.appendChild(document.importNode ? document.importNode(svg, true) : svg);
			}
		} catch (e) { /* text fallback already applied */ } // NOSONAR javascript:S2486 -- best-effort; text label already painted
	}

	function stampChromeTip(el, text) {
		const t = text == null ? "" : String(text);
		if (t !== "") {
			el.setAttribute("data-jc-tip", t); // NOSONAR javascript:S7761 -- el may be a minimal stub without dataset; the attribute name is pinned by CSS selectors
			el.setAttribute("aria-label", t);
		} else if (typeof el.removeAttribute === "function") {
			el.removeAttribute("data-jc-tip"); // NOSONAR javascript:S7761 -- el may be a minimal stub without dataset; the attribute name is pinned by CSS selectors
		}
		if (typeof el.removeAttribute === "function") el.removeAttribute("title");
		el.title = "";
	}

	/**
	 * Wires the Columns affordance onto the table toolbar when {@code columnConfig} is present.  Called from
	 * {@code constructTable} on first init AND every Apply rebuild.
	 */
	function mountChooser(table, ctx, toolbarRow) {
		if (!ctx?.viewDef?.columnConfig) return;
		const host = resolveChooserHost(table, toolbarRow);
		if (!host) return;
		if (host.querySelector?.(".juneau-config-chooser-btn")) return;

		const btn = document.createElement("button");
		btn.type = "button";
		btn.className = "juneau-view-ribbon-btn juneau-config-chooser-btn";
		stampChromeTip(btn, "Columns");
		paintUserText(btn, "Columns");
		const markup = typeof NS.icons?.resolveIcon === "function" ? NS.icons.resolveIcon("tune") : null;
		paintChooserIcon(btn, markup);
		btn.addEventListener("click", function () { openChooser(table, ctx); });
		host.appendChild(btn);

		if (ctx._configDraft == null) {
			// Single seeding point: the last-applied View Settings (memoized read shared with go()'s restore), else
			// catalog defaults.  The one-time schemaVersion-reset notice is armed here and shown by openChooser.
			const resolved = resolveLastAppliedViewSettings(table, ctx);
			ctx._configDraft = resolved.draft != null ? resolved.draft : defaultDraftForCtx(ctx);
			ctx._configResetNotice = !!resolved.reset;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
		}
	}

	NS.config.sanitizeColumnTitlesForDataTables = sanitizeColumnTitlesForDataTables;
	NS.config.paintHeaderTitles = paintHeaderTitles;
	NS.config.paintUserText = paintUserText;
	NS.config.paintUserInput = paintUserInput;
	NS.config.canHideColumn = canHideColumn;
	NS.config.moveColumn = moveColumn;
	NS.config.defaultDraftFromCatalog = defaultDraftFromCatalog;
	NS.config.draftFromSavedView = draftFromSavedView;
	NS.config.mountChooser = mountChooser;
	NS.config.openChooser = openChooser;
	NS.config.closeChooserDialog = closeChooserDialog;
	NS.config.applyDraft = applyDraft;
	NS.config.CHOOSER_BACKDROP_CLASS = CHOOSER_BACKDROP_CLASS;
	// T12 — four-tab View Settings pure layer + page-state bridge (§3/§6.1).
	NS.config.CONFIG_TABS = CONFIG_TABS;
	NS.config.searchCapableColumns = searchCapableColumns;
	NS.config.sortCapableColumns = sortCapableColumns;
	NS.config.defaultSearchMembership = defaultSearchMembership;
	NS.config.applySearchMembershipToColumns = applySearchMembershipToColumns;
	NS.config.resolveLastAppliedViewSettings = resolveLastAppliedViewSettings;
	NS.config.defaultSortOrder = defaultSortOrder;
	NS.config.intersectSortOrder = intersectSortOrder;
	NS.config.moveSortEntry = moveSortEntry;
	NS.config.visibleConfigTabs = visibleConfigTabs;
	NS.config.CONFIG_TAB_PUBLIC_KEYS = CONFIG_TAB_PUBLIC_KEYS;
	NS.config.defaultOptions = defaultOptions;
	NS.config.normalizeOptions = normalizeOptions;
	NS.config.intersectMembership = intersectMembership;
	NS.config.draftFromViewSettings = draftFromViewSettings;
	NS.config.viewSettingsFromDraft = viewSettingsFromDraft;
	NS.config.readViewSettings = readViewSettings;
	NS.config.writeViewSettings = writeViewSettings;
	NS.config.resolveShareableOpenState = resolveShareableOpenState;
	NS.config.copyShareLink = copyShareLink;
	NS.config.selectConfigTab = selectConfigTab;
	NS.config.resetDraftToDefaults = resetDraftToDefaults;
	// Tables built before this file registered (a shell-first page) are rebuilt now that the chooser exists.
	if (typeof NS.init?.flushLateConfigRebuilds === "function") NS.init.flushLateConfigRebuilds();
})();
