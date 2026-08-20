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
 * juneau-config.js - opt-in column-configurator persistence + pure config-application layer (TODO-444).
 *
 * This file is loaded AFTER juneau-views.js (a template <script> include the consumer adds, exactly like
 * juneau-pages.js - NOT a dynamic fetch; see design §4.1) and extends the SAME window.JuneauViews namespace.  A
 * non-configurable table never loads it and pays nothing.
 *
 * LANDED SLICES:
 *   - Slice 2: async persistence SPI, strict localStorage key codec (enc/dec), localStorage + server providers.
 *   - Slice 3: (Java) SavedViewsMixin + SavedViewStore; this file's server-provider HTTP mapping stays as-is.
 *   - Slice 4: pure DOM-free config-application layer - computeEffectiveColumns / validateView / saved-view
 *     (de)serialization / dtIndex (the INDEX MODEL only).
 *   - Slice 5: resolveActiveView (awaited before first draw) + applyView (programmatic reinit entry point).
 *   - Slice 6: View-tab chooser UI, XSS textContent painting, mountChooser seam.
 *   - Slice 7: ribbon toggle persistence routes through this file's synchronous getItem/setItem
 *     (same exact keys as juneau-ribbon.js; never a Promise).
 *
 * Every provider implements the SAME seven-method async contract (Promise-based; TODO-444 §3.2/§3.3, refined by
 * the round-3 saveAndActivate addendum): list/load/save/saveAndActivate/setActive/delete/getActive.  Each method
 * takes the LIVE table element as its first argument (never a raw pageId/viewId string) - both providers derive
 * their own scope from it, mirroring how the pageId is discovered (table.closest('[data-juneau-page]')).  This is
 * a deliberate refinement of the design doc's illustrative §3.2 pseudocode (a generic string-keyed load/save/
 * remove/list(prefix) KV interface): the server provider cannot honor a generic opaque string key - it keys on
 * the STRUCTURED tuple (principal, pageId, viewId, name) the REST endpoint expects (§3.3) - so both providers are
 * built against the domain-shaped SavedViewStore method set instead (list/load/save/saveAndActivate/setActive/
 * delete), with getActive added as a client-side convenience derived uniformly from list() (see the "PUBLIC API"
 * section below).
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
	const CURRENT_SCHEMA_VERSION = 1;

	/** Decoded-name cap (§3.1) - named so JS and the slice-3 Java mixin reject at the exact same boundary. */
	const MAX_NAME_LEN = 128;

	/**
	 * Encoded key-SEGMENT cap (§3.1) - a DISTINCT number from {@link #MAX_NAME_LEN}: a 128-multibyte-char name can
	 * enc() to far more than 128 bytes, so the encoded cap must be named and checked independently or JS/Java could
	 * drift on where a pathological name is actually rejected.  localStorage-only (the server keys on a structured
	 * tuple and never enc()s a name - see createServerProvider below).
	 */
	const MAX_ENCODED_SEGMENT_LEN = 512;

	/**
	 * localStorage's OWN copy of the default per-(user,page,view)/per-blob/per-user bounds (§3.2) - a deliberate,
	 * textually SEPARATE copy of the same three numbers the slice-3 Java SavedViewStore default will enforce.  Per
	 * the plan's should-fix: the client and server sides must never share one source-of-truth across the JS/Java
	 * boundary (there isn't one), so keeping two independently-named copies turns "these must stay equal" into a
	 * stated, testable invariant instead of an accidental one.  If you change one of these three numbers, change
	 * its Java mirror in the SAME commit.
	 */
	const LOCALSTORAGE_MAX_VIEWS_PER_SCOPE = 50;      // mirrors the server default MAX_VIEWS_PER_SCOPE
	const LOCALSTORAGE_MAX_BLOB_BYTES = 64 * 1024;    // mirrors the server default per-blob cap (64 KB)
	const LOCALSTORAGE_MAX_VIEWS_PER_USER = 500;      // mirrors the server default MAX_VIEWS_PER_USER (aggregate)

	/** The shell attribute juneau-pages.js/PageTable stamp the page id onto (§3.1) - read via closest(...). */
	const PAGE_ID_ATTR = "data-juneau-page";

	/**
	 * The shell attribute the slice-3 emitter (ViewTable/PageTable) stamps the resolved, context-path-aware
	 * saved-views REST base onto (§3.3 DECISION option (b)) - read via closest(...), mirroring PAGE_ID_ATTR
	 * exactly.  Absent/blank means the server-persisted provider is UNAVAILABLE for this table - never a
	 * hardcoded "/"-rooted fallback path.
	 */
	const SAVED_VIEWS_BASE_ATTR = "data-juneau-saved-views";

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
	function networkError(message) { return typedError("network", message); }

	/**
	 * Normalizes ANY thrown value into the typed {code,message} shape (§3.2 "typed failure, never silent
	 * success") - a value already carrying one of the four frozen codes passes through; a native localStorage
	 * quota/private-mode DOMException maps to 'quota'; anything else is 'unavailable' rather than swallowed.
	 */
	function toTypedError(e) {
		if (e && (e.code === "quota" || e.code === "unavailable" || e.code === "network" || e.code === "malformed"))
			return { code: e.code, message: e.message };
		if (e && (e.name === "QuotaExceededError" || e.code === 22 || e.code === 1014))
			return { code: "quota", message: "localStorage quota exceeded or blocked (private mode)" };
		return { code: "unavailable", message: (e && e.message) ? e.message : String(e) };
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
	 * path separator).  This is the STORAGE-KEY encoder only - never the wire/query-string encoder (see
	 * createServerProvider, which uses ordinary encodeURIComponent instead and never calls this function).
	 */
	function encSegment(s) {
		const str = s == null ? "" : String(s);
		const bytes = utf8Encode(str);
		let out = "";
		for (let i = 0; i < bytes.length; i++) {
			const b = bytes[i];
			const ch = b < 128 ? String.fromCharCode(b) : null;
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
				const hex = str.substr(i + 1, 2);
				if (!/^[0-9A-F]{2}$/.test(hex))
					throw malformedError("non-canonical percent-encoding at offset " + i + " in '" + str + "'");
				bytes.push(parseInt(hex, 16));
				i += 3;
			} else {
				if (!isSafeSegmentChar(ch))
					throw malformedError("illegal raw character '" + ch + "' at offset " + i + " in '" + str + "'");
				bytes.push(ch.charCodeAt(0));
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
	 * embedding a view with the same ViewDef.id would otherwise collide on one shared saved-views namespace.
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
	 * The wire-side name check (§3.1/§3.2): blank/reserved/too-long - shared by BOTH providers, since the server
	 * mixin (slice 3) will enforce the exact same MAX_NAME_LEN + reserved-word rule on the raw DECODED name it
	 * receives as a query param.  Deliberately does NOT touch encSegment/MAX_ENCODED_SEGMENT_LEN - that check is
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
		const found = (views || []).some(function (v) { return v && v.name === active; });
		return found ? { name: active, dangling: false } : { name: null, dangling: true };
	}

	/** Rejects an unknown/newer blob schemaVersion as 'malformed' (§3.2 stale-load handling) - never crashes. */
	function assertSupportedSchema(blob) {
		if (!blob || blob.schemaVersion !== CURRENT_SCHEMA_VERSION)
			throw malformedError("saved-view blob has an unsupported schemaVersion (expected " + CURRENT_SCHEMA_VERSION + ")");
		return blob;
	}

	/**
	 * Resolves the enclosing page id (§3.1): {@code table.closest('[data-juneau-page]')}'s OWN attribute value -
	 * the SAME shell attribute juneau-pages.js/PageTable stamp - falling back to null (standalone) when there is
	 * no page shell.  This is the CONTRACT, not a convenience read: a future non-juneau-pages.js host that wants
	 * page-qualification must stamp this exact attribute rather than inventing a parallel one.
	 */
	function resolvePageId(table) {
		const host = (table && table.closest) ? table.closest("[" + PAGE_ID_ATTR + "]") : null;
		const v = host ? host.getAttribute(PAGE_ID_ATTR) : null;
		return isBlank(v) ? null : v;
	}

	/** Resolves the table's own stable view id - reuses the SAME attribute juneau-views.js's initTable reads. */
	function resolveViewId(table) {
		const v = table ? table.getAttribute(VIEW_ID_ATTR) : null;
		return isBlank(v) ? null : v;
	}

	/**
	 * Resolves the server-persisted provider's context-path-aware REST base (§3.3 DECISION option (b)):
	 * {@code table.closest('[data-juneau-saved-views]')}'s own attribute value, mirroring resolvePageId exactly.
	 * Returns null (never a hardcoded "/"-rooted guess) when absent/blank - the caller MUST fail closed.
	 */
	function resolveSavedViewsBase(table) {
		const host = (table && table.closest) ? table.closest("[" + SAVED_VIEWS_BASE_ATTR + "]") : null;
		const v = host ? host.getAttribute(SAVED_VIEWS_BASE_ATTR) : null;
		return isBlank(v) ? null : v;
	}

	// ==================================================================================================================
	// PURE CONFIG-APPLICATION LAYER  (§4.3 — Option-B testable; no DOM / jQuery / DataTables)
	// ==================================================================================================================

	/** True when `arr` contains the same value more than once (strict equality). */
	function hasDuplicateEntries(arr) {
		if (!arr || arr.length < 2) return false;
		const seen = Object.create(null);
		for (let i = 0; i < arr.length; i++) {
			const k = String(arr[i]);
			if (seen[k]) return true;
			seen[k] = true;
		}
		return false;
	}

	/** Catalog → Map<dataKey, column> (first wins if the author somehow duplicated a data key). */
	function catalogByData(catalog) {
		const map = Object.create(null);
		(catalog || []).forEach(function (c) {
			if (c && c.data != null && map[c.data] == null) map[c.data] = c;
		});
		return map;
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
				if (col.render.meta) {
					out.render.meta = {};
					for (const k in col.render.meta)
						if (Object.hasOwn(col.render.meta, k)) out.render.meta[k] = col.render.meta[k];
				}
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
		if (render.meta) {
			out.meta = {};
			for (const k in render.meta) if (Object.hasOwn(render.meta, k)) out.meta[k] = render.meta[k];
		}
		return out;
	}

	/**
	 * Default visible set when a saved view omits `visible` (§4.3): each catalog column contributes iff
	 * `defaultVisible !== false`, and pinned columns are always included regardless of defaultVisible.
	 */
	function defaultVisibleKeys(catalog) {
		const out = [];
		(catalog || []).forEach(function (c) {
			if (!c || c.data == null) return;
			if (c.pinned || c.defaultVisible !== false) out.push(c.data);
		});
		return out;
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

		let order;
		if (view.order == null) {
			order = catalogOrder.slice();
		} else {
			if (!Array.isArray(view.order))
				return { ok: false, code: "malformed", message: "saved-view order must be an array" };
			if (hasDuplicateEntries(view.order))
				return { ok: false, code: "malformed", message: "saved-view order contains duplicate column ids" };
			order = [];
			view.order.forEach(function (id) {
				if (byData[id] != null) order.push(id);
			});
			catalogOrder.forEach(function (id) {
				if (order.indexOf(id) < 0) order.push(id);
			});
		}

		let visible;
		if (view.visible == null) {
			visible = defaultVisibleKeys(cols);
		} else {
			if (!Array.isArray(view.visible))
				return { ok: false, code: "malformed", message: "saved-view visible must be an array" };
			if (hasDuplicateEntries(view.visible))
				return { ok: false, code: "malformed", message: "saved-view visible contains duplicate column ids" };
			visible = [];
			view.visible.forEach(function (id) {
				if (byData[id] != null) visible.push(id);
			});
		}

		// Pinned columns are always visible (un-hideable but reorderable).
		cols.forEach(function (c) {
			if (c && c.pinned && c.data != null && visible.indexOf(c.data) < 0) visible.push(c.data);
		});

		// ≥1 visible — repair an all-hidden blob rather than crash the table.
		if (visible.length === 0 && catalogOrder.length > 0)
			visible = [catalogOrder[0]];

		const labels = {};
		if (view.labels && typeof view.labels === "object") {
			for (const k in view.labels) {
				if (Object.hasOwn(view.labels, k) && byData[k] != null)
					labels[k] = view.labels[k];
			}
		}

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

		return {
			ok: true,
			view: {
				schemaVersion: view.schemaVersion != null ? view.schemaVersion : CURRENT_SCHEMA_VERSION,
				visible: visible,
				order: order,
				labels: labels,
				formats: formats
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
			if (c && c.pinned && c.data != null) visibleSet[c.data] = true;
		});
		const visibleKeys = Object.keys(visibleSet);
		if (visibleKeys.length === 0 && order.length > 0) visibleSet[order[0]] = true;

		const out = [];
		order.forEach(function (dataKey) {
			const col = byData[dataKey];
			if (!col) return;
			const effective = copyCatalogColumn(col);

			// Label override — blank/whitespace reverts to the catalog title (persistence-only; no wire field).
			if (normalized && normalized.labels && Object.hasOwn(normalized.labels, dataKey)) {
				const override = normalized.labels[dataKey];
				if (override != null && String(override).trim() !== "")
					effective.title = String(override);
				// else leave catalog title (blank-label revert)
			}

			// Format override — id swap only; meta + href preserved via copyCatalogColumn + swapRenderId.
			if (normalized && normalized.formats && Object.hasOwn(normalized.formats, dataKey)) {
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
			catch (e) { throw malformedError("saved-view blob is not valid JSON"); }
		}
		assertSupportedSchema(blob);
		return {
			schemaVersion: blob.schemaVersion,
			visible: blob.visible == null ? null : Array.prototype.slice.call(blob.visible),
			order: blob.order == null ? null : Array.prototype.slice.call(blob.order),
			labels: (blob.labels && typeof blob.labels === "object") ? Object.assign({}, blob.labels) : {},
			formats: (blob.formats && typeof blob.formats === "object") ? Object.assign({}, blob.formats) : {}
		};
	}

	/**
	 * Serializes a draft / normalized view into the persisted blob shape (§3.1):
	 * {@code {schemaVersion, visible, order, labels, formats}}.  Blank label overrides are omitted (they mean
	 * "use catalog title"); empty {@code labels}/{@code formats} objects are still emitted so the schema stays
	 * stable for round-trips.
	 */
	function serializeSavedView(view) {
		const blob = {
			schemaVersion: CURRENT_SCHEMA_VERSION,
			visible: (view && Array.isArray(view.visible)) ? view.visible.slice() : [],
			order: (view && Array.isArray(view.order)) ? view.order.slice() : [],
			labels: {},
			formats: {}
		};
		if (view && view.labels && typeof view.labels === "object") {
			for (const k in view.labels) {
				if (!Object.hasOwn(view.labels, k)) continue;
				const v = view.labels[k];
				if (v != null && String(v).trim() !== "") blob.labels[k] = String(v);
			}
		}
		if (view && view.formats && typeof view.formats === "object") {
			for (const k in view.formats) {
				if (Object.hasOwn(view.formats, k) && view.formats[k] != null)
					blob.formats[k] = view.formats[k];
			}
		}
		return blob;
	}

	/**
	 * Builds the ACTUAL DataTables {@code opts.columns} index space (§4.2):
	 * {@code [selection?] + effectiveColumns(including hidden, in order) + [actions?]}.
	 * Synthetic selection/actions cells use {@code data:null} and a {@code _juneau} marker so they are never
	 * mistaken for a catalog column by {@link #dtIndex}.  Pure / DOM-free — slice 5 rewires consumers onto this.
	 */
	function buildOptsColumnSpace(effectiveColumns, options) {
		const cols = [];
		if (options && options.hasSelection)
			cols.push({ data: null, _juneau: "selection" });
		(effectiveColumns || []).forEach(function (c) {
			cols.push({
				data: c.data,
				visible: c.visible !== false,
				title: c.title
			});
		});
		if (options && options.hasActions)
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
		for (let i = 0; i < optsColumns.length; i++) {
			if (optsColumns[i] && optsColumns[i].data === dataKey) return i;
		}
		return -1;
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
	function createLocalStorageProvider() {

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
		function readActiveRaw(scope) {
			const raw = window.localStorage.getItem(activeKeyFor(scope));
			if (raw == null) return null;
			try { return decSegment(raw); } catch (e) { return raw; }
		}

		function writeActiveRaw(scope, name) {
			const key = activeKeyFor(scope);
			if (name == null) window.localStorage.removeItem(key);
			else window.localStorage.setItem(key, encSegment(name));
		}

		/** Every saved view currently under `scope`, decoded - an undecodable key is silently skipped (never crashes). */
		function listViews(scope) {
			const prefix = viewsPrefixKey(scope);
			const out = [];
			for (let i = 0; i < window.localStorage.length; i++) {
				const k = window.localStorage.key(i);
				if (k != null && k.indexOf(prefix) === 0) {
					try { out.push({ name: decSegment(k.slice(prefix.length)) }); } catch (e) { /* skip unreadable key */ }
				}
			}
			return out;
		}

		/** The per-user AGGREGATE count across every (page,view) scope (§3.2 quota-bypass fix) - a substring scan,
		 *  never a regex, so an attacker-controlled scope/name segment cannot be mistaken for pattern syntax. */
		function countAllViewsForThisUser() {
			let count = 0;
			for (let i = 0; i < window.localStorage.length; i++) {
				const k = window.localStorage.key(i);
				if (k != null && k.indexOf(KEY_ROOT) === 0 && k.indexOf(".columns.views.") > 0) count++;
			}
			return count;
		}

		function byteLength(str) { return utf8Encode(str).length; }

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

		function persistBlob(scope, name, blob) {
			const v = validateNameForLocalStorage(name);
			if (!v.ok) throw malformedError(v.message);
			assertSupportedSchema(blob);
			enforceBounds(scope, name, blob);
			window.localStorage.setItem(viewKeyFor(scope, v.encoded), JSON.stringify(blob));
		}

		/** Runs `fn` synchronously but always returns a settled-in-a-microtask Promise, typed-error on throw. */
		function asAsync(fn) {
			return new Promise(function (resolve, reject) {
				try { resolve(fn()); } catch (e) { reject(toTypedError(e)); }
			});
		}

		return {

			list: function (table) {
				return asAsync(function () {
					const ctx = requireScope(table);
					return { active: readActiveRaw(ctx.scope), views: listViews(ctx.scope) };
				});
			},

			load: function (table, name) {
				return asAsync(function () {
					const ctx = requireScope(table);
					const v = validateNameForLocalStorage(name);
					if (!v.ok) throw malformedError(v.message);
					const raw = window.localStorage.getItem(viewKeyFor(ctx.scope, v.encoded));
					if (raw == null) return null;
					let blob;
					try { blob = JSON.parse(raw); } catch (e) { throw malformedError("stored saved-view blob is not valid JSON"); }
					return assertSupportedSchema(blob);
				});
			},

			save: function (table, name, blob) {
				return asAsync(function () {
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
				return asAsync(function () {
					const ctx = requireScope(table);
					persistBlob(ctx.scope, name, blob);
					writeActiveRaw(ctx.scope, name);
				});
			},

			setActive: function (table, name) {
				return asAsync(function () {
					const ctx = requireScope(table);
					if (name == null) { writeActiveRaw(ctx.scope, null); return; }
					const v = validateNameForLocalStorage(name);
					if (!v.ok) throw malformedError(v.message);
					writeActiveRaw(ctx.scope, name);
				});
			},

			"delete": function (table, name) {
				return asAsync(function () {
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
					if (e.key != null && e.key.indexOf(scopePrefix) === 0) onChange({ key: e.key, oldValue: e.oldValue, newValue: e.newValue });
				}
				window.addEventListener("storage", handler);
				return function unwatch() { window.removeEventListener("storage", handler); };
			}
		};
	}

	// ==================================================================================================================
	// SERVER-PERSISTED PROVIDER  (client side of the SavedViewsMixin REST endpoint - slice 3 ships the server half)
	// ==================================================================================================================

	/**
	 * Builds the server-persisted persistence provider (§3.3).  EVERY method resolves the REST base fresh, per
	 * call, via {@code table.closest('[data-juneau-saved-views]')} and FAILS CLOSED (typed 'unavailable', no
	 * request issued) when it is absent/blank - never a hardcoded "/"-rooted path.  `page`/`view`/`name` are
	 * ordinary WIRE-encoded query-param values (plain {@code encodeURIComponent}) - this provider never calls
	 * encSegment/decSegment, because the server keys on the STRUCTURED tuple (principal,pageId,viewId,name), not
	 * a delimited string, so it needs no delimiter-safety codec (§3.3 "enc()/dec() is the localStorage KEY codec,
	 * NOT a server delimiter-safety mechanism").  Writes assemble the transport ENVELOPE directly (JSON content
	 * type + the CSRF header via the SAME resolveCsrfToken/resolveCsrfHeaderName/isBlankToken helpers
	 * juneau-views.js's row-action path uses, fail-closed on a blank token) - this deliberately does NOT call
	 * buildActionRequest, which refuses safe methods and hard-codes the body to {action:id} (neither fits a
	 * saved-views write).  GET (list/load) is a plain, CSRF-free fetch - never an EventSource/SSE.
	 */
	function createServerProvider() {

		function baseFor(table) {
			const base = resolveSavedViewsBase(table);
			if (base == null)
				throw unavailableError("no [" + SAVED_VIEWS_BASE_ATTR + "] shell found for this table; " +
					"the server-persisted provider is unavailable");
			return base;
		}

		function requireViewId(table) {
			const v = resolveViewId(table);
			if (v == null) throw unavailableError("table has no " + VIEW_ID_ATTR + " id");
			return v;
		}

		/** Ordinary wire-side query-string assembly - plain encodeURIComponent, deliberately never encSegment. */
		function buildQuery(params) {
			const parts = [];
			for (const k in params) {
				if (Object.hasOwn(params, k) && params[k] != null)
					parts.push(encodeURIComponent(k) + "=" + encodeURIComponent(params[k]));
			}
			return parts.length ? ("?" + parts.join("&")) : "";
		}

		function queryFor(table, extra) {
			const params = { view: requireViewId(table) };
			const pageId = resolvePageId(table);
			if (pageId != null) params.page = pageId;
			if (extra) for (const k in extra) if (Object.hasOwn(extra, k) && extra[k] != null) params[k] = extra[k];
			return buildQuery(params);
		}

		/** HTTP-status -> typed-error-code classification (flagged for slice 3 to lock down together - see the
		 *  implementer report: the plan does not pin exact statuses for a quota rejection vs. a plain bad request). */
		function classifyStatus(status) {
			if (status === 413 || status === 429 || status === 507) return "quota";
			if (status >= 500) return "network";
			if (status === 401 || status === 403) return "unavailable";
			return "malformed";
		}

		function httpError(status, bodyText) {
			let env = null;
			try { env = bodyText ? JSON.parse(bodyText) : null; } catch (e) { env = null; }
			const err = typedError(classifyStatus(status), (env && env.message) ? env.message : ("saved-views request failed (HTTP " + status + ")"));
			err.httpStatus = status;
			return err;
		}

		function doFetch(url, init) {
			let req;
			try { req = fetch(url, init); } catch (e) { return Promise.reject(networkError("the saved-views request could not be sent")); }
			return req.then(function (resp) {
				if (resp.ok) return resp;
				return resp.text().then(function (text) { throw httpError(resp.status, text); },
					function () { throw httpError(resp.status, null); });
			}, function () {
				throw networkError("the saved-views request failed (network error)");
			});
		}

		function readJsonBody(resp) {
			return resp.text().then(function (text) {
				if (text == null || text === "") return null;
				try { return JSON.parse(text); } catch (e) { throw malformedError("saved-views response was not valid JSON"); }
			});
		}

		function get(table, path, extraParams) {
			const url = baseFor(table) + path + queryFor(table, extraParams);
			return doFetch(url, { method: "GET", credentials: "same-origin" }).then(readJsonBody);
		}

		/**
		 * Assembles the fail-closed transport envelope for a non-safe write - JSON content type, the CSRF header
		 * (via juneau-views.js's own resolveCsrfToken/resolveCsrfHeaderName/isBlankToken, so the two files agree
		 * by construction), and `credentials:'same-origin'`.  The body is ALWAYS real JSON, never empty (a `{}`
		 * for a body-less DELETE/clear-active, per §3.2/§3.3).
		 */
		function writeRequest(table, method, url, body) {
			const init = NS.init;
			const token = init ? init.resolveCsrfToken(table) : null;
			if (!init || init.isBlankToken(token))
				return { refuse: true, reason: "missing-token" };
			const headerName = init.resolveCsrfHeaderName(table);
			const headers = { "Content-Type": "application/json" };
			headers[headerName] = token;
			return { url: url, method: method, headers: headers, body: JSON.stringify(body == null ? {} : body) };
		}

		function write(table, method, path, extraParams, body) {
			const url = baseFor(table) + path + queryFor(table, extraParams);
			const req = writeRequest(table, method, url, body);
			if (req.refuse)
				return Promise.reject(unavailableError("no CSRF token available for this table; the write was not sent"));
			return doFetch(req.url, { method: req.method, headers: req.headers, body: req.body, credentials: "same-origin" });
		}

		/** Runs `fn` (which may throw synchronously, e.g. baseFor's fail-closed check) as a typed-rejecting Promise. */
		function asAsync(fn) {
			try {
				return Promise.resolve(fn()).then(null, function (e) { throw toTypedError(e); });
			} catch (e) {
				return Promise.reject(toTypedError(e));
			}
		}

		return {

			list: function (table) {
				return asAsync(function () { return get(table, ""); });
			},

			load: function (table, name) {
				return asAsync(function () {
					const basic = validateNameBasic(name);
					if (!basic.ok) throw malformedError(basic.message);
					return get(table, "/item", { name: name }).then(
						function (blob) { return blob == null ? null : assertSupportedSchema(blob); },
						function (e) { if (e && e.httpStatus === 404) return null; throw e; });
				});
			},

			save: function (table, name, blob) {
				return asAsync(function () {
					const basic = validateNameBasic(name);
					if (!basic.ok) throw malformedError(basic.message);
					assertSupportedSchema(blob);
					return write(table, "PUT", "/item", { name: name }, blob).then(function () {});
				});
			},

			saveAndActivate: function (table, name, blob) {
				return asAsync(function () {
					const basic = validateNameBasic(name);
					if (!basic.ok) throw malformedError(basic.message);
					assertSupportedSchema(blob);
					// The save-vs-save+activate fork is this explicit ?activate=1 QUERY FLAG (§3.3 finding -
					// Blocker) - never a field inside the persisted blob, which would then wrongly persist into
					// every saved view.
					return write(table, "PUT", "/item", { name: name, activate: 1 }, blob).then(function () {});
				});
			},

			setActive: function (table, name) {
				return asAsync(function () {
					if (name != null) {
						const basic = validateNameBasic(name);
						if (!basic.ok) throw malformedError(basic.message);
					}
					// PUT .../active always sends a real JSON body - {name} to set, {} to clear - never empty.
					return write(table, "PUT", "/active", null, name == null ? {} : { name: name }).then(function () {});
				});
			},

			"delete": function (table, name) {
				return asAsync(function () {
					const basic = validateNameBasic(name);
					if (!basic.ok) throw malformedError(basic.message);
					return write(table, "DELETE", "/item", { name: name }, {}).then(function () {});
				});
			}

			// Deliberately NO watchExternalChanges: the `storage` event never fires for an HTTP write (§3.2).
			// The server provider's multi-tab story is "reload to see another tab's change" - nothing more; do
			// not imply a live reconcile exists here.
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

	/** Swaps the active persistence provider (§3.2/§5) - e.g. `JuneauViews.setPersistenceProvider(JuneauViews.persistenceProviders.server())`. */
	NS.setPersistenceProvider = function (provider) {
		currentProvider = provider;
	};

	/** The provider-selection seam (§5): factories for the two first-party providers this file ships. */
	NS.persistenceProviders = {
		localStorage: createLocalStorageProvider,
		server: createServerProvider
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
		} catch (e) { return null; }
	};
	NS.persistence.setItem = function (key, value) {
		try {
			window.localStorage.setItem(key, String(value));
		} catch (e) { /* quota / private mode — ribbon click path stays synchronous and must not throw */ }
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
	NS.config.SAVED_VIEWS_BASE_ATTR = SAVED_VIEWS_BASE_ATTR;
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
	NS.config.resolveSavedViewsBase = resolveSavedViewsBase;
	NS.config.createLocalStorageProvider = createLocalStorageProvider;
	NS.config.createServerProvider = createServerProvider;
	// Slice 4 — pure config-application layer (§4.3) + the dtIndex index model (§4.2 INDEX MODEL only).
	NS.config.validateView = validateView;
	NS.config.computeEffectiveColumns = computeEffectiveColumns;
	NS.config.deserializeSavedView = deserializeSavedView;
	NS.config.serializeSavedView = serializeSavedView;
	NS.config.buildOptsColumnSpace = buildOptsColumnSpace;
	NS.config.dtIndex = dtIndex;
	NS.config.swapRenderId = swapRenderId;

	/**
	 * Loads the active saved-view blob for a configurable table (or {@code null} for Default).  Stale / unknown-
	 * schema blobs resolve as Default rather than rejecting.  Persistence failures reject so {@code initTable}
	 * can refuse the first draw (isDataTable stays false).
	 */
	function resolveActiveView(table, viewDef) {
		if (!NS.persistence) return Promise.resolve(null);
		return NS.persistence.getActive(table).then(function (r) {
			if (!r || r.name == null) return null;
			return NS.persistence.load(table, r.name).then(function (blob) {
				if (blob == null) return null;
				try { return deserializeSavedView(blob); }
				catch (e) { return null; }
			});
		});
	}

	/**
	 * Programmatic Apply entry point (no chooser UI).  Computes effective columns from {@code savedView} and
	 * runs the destroy/reinit transaction via {@code NS.init.buildTable}.
	 */
	function applyView(table, savedView) {
		const ctx = table && table.__juneauCtx;
		if (!ctx || !ctx.viewDef) return { ok: false, reason: "not-initialized" };
		if (!NS.init || typeof NS.init.buildTable !== "function") return { ok: false, reason: "no-buildTable" };
		let effective;
		try {
			effective = computeEffectiveColumns(ctx.viewDef.columns || [], savedView);
		} catch (e) {
			return { ok: false, reason: "malformed", message: e && e.message };
		}
		return NS.init.buildTable(table, ctx.viewDef, effective, ctx);
	}

	NS.config.resolveActiveView = resolveActiveView;
	NS.config.applyView = applyView;

	// ==================================================================================================================
	// CHOOSER UI  (View tab only — Search/Sort/Options tabs are out of scope)
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
			if (c && c._juneau !== "selection" && c._juneau !== "actions")
				c.title = "";
		});
	}

	/**
	 * Paints DataTables header cells from the effective column model using {@code textContent} only.
	 * Selection / actions headers are skipped (they are unlabeled by design).
	 */
	function paintHeaderTitles(table, effectiveColumns, ctx) {
		if (!table) return;
		const headRow = table.querySelector("thead tr");
		if (!headRow) return;
		const ths = headRow.children;
		const offset = (ctx && ctx.selectionState) ? 1 : 0;
		(effectiveColumns || []).forEach(function (col, i) {
			const th = ths[offset + i];
			if (!th) return;
			const label = (col.title != null && String(col.title).trim() !== "") ? String(col.title) : (col.data || "");
			paintUserText(th, label);
		});
	}

	function catalogByDataLocal(catalog) {
		const m = Object.create(null);
		(catalog || []).forEach(function (c) { if (c && c.data != null) m[c.data] = c; });
		return m;
	}

	function defaultDraftFromCatalog(catalog) {
		const cols = catalog || [];
		const order = [];
		const visible = [];
		cols.forEach(function (c) {
			if (!c || c.data == null) return;
			order.push(c.data);
			if (c.pinned || c.defaultVisible !== false) visible.push(c.data);
		});
		if (visible.length === 0 && order.length > 0) visible.push(order[0]);
		return { visible: visible, order: order, labels: {}, formats: {} };
	}

	function draftFromSavedView(catalog, savedView) {
		if (savedView == null) return defaultDraftFromCatalog(catalog);
		const validated = validateView(savedView, catalog);
		if (!validated.ok || !validated.view) return defaultDraftFromCatalog(catalog);
		return {
			visible: validated.view.visible.slice(),
			order: validated.view.order.slice(),
			labels: Object.assign({}, validated.view.labels || {}),
			formats: Object.assign({}, validated.view.formats || {})
		};
	}

	function snapshotDraft(draft) {
		return JSON.stringify({
			visible: draft.visible,
			order: draft.order,
			labels: draft.labels,
			formats: draft.formats
		});
	}

	function visibleCount(draft) {
		return (draft && draft.visible) ? draft.visible.length : 0;
	}

	/**
	 * Whether {@code dataKey} may be unchecked.  Pinned columns never; the last remaining visible column never.
	 */
	function canHideColumn(draft, catalog, dataKey) {
		const byData = catalogByDataLocal(catalog);
		const col = byData[dataKey];
		if (col && col.pinned) return false;
		if (!draft || !draft.visible) return false;
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
		const backdrop = ctx && ctx._configBackdrop;
		if (backdrop && backdrop.parentNode) backdrop.parentNode.removeChild(backdrop);
		if (ctx) {
			ctx._configBackdrop = null;
			ctx._configDirtyEl = null;
			ctx._configStatusEl = null;
			ctx._configListEl = null;
			ctx._configSelectEl = null;
		}
	}

	function currentCatalog(ctx) {
		return (ctx && ctx.viewDef && ctx.viewDef.columns) ? ctx.viewDef.columns : [];
	}

	function renderChooserColumnList(ctx) {
		const list = ctx._configListEl;
		if (!list) return;
		while (list.firstChild) list.removeChild(list.firstChild);
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
		row.setAttribute("data-col", col.data);

		const vis = document.createElement("input");
		vis.type = "checkbox";
		vis.className = "juneau-config-col-vis";
		vis.checked = draft.visible.indexOf(col.data) >= 0;
		vis.disabled = !!col.pinned || (!canHideColumn(draft, currentCatalog(ctx), col.data) && vis.checked);
		vis.setAttribute("aria-label", "Show column");
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
		up.setAttribute("aria-label", "Move column up");
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
		down.setAttribute("aria-label", "Move column down");
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
		label.setAttribute("aria-label", "Column label");
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
			sel.setAttribute("aria-label", "Column format");
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
		while (sel.firstChild) sel.removeChild(sel.firstChild);
		const defOpt = document.createElement("option");
		defOpt.value = "";
		paintUserText(defOpt, DEFAULT_VIEW_LABEL);
		sel.appendChild(defOpt);
		const views = (listing && listing.views) ? listing.views : [];
		views.forEach(function (v) {
			const n = (v && v.name != null) ? String(v.name) : String(v);
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
			labels: Object.assign({}, ctx._configDraft.labels),
			formats: Object.assign({}, ctx._configDraft.formats)
		};
		const result = applyView(table, saved);
		if (result && result.ok) {
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			refreshChooserDirty(ctx);
			showChooserStatus(ctx, "", false);
		} else if (result && result.reason === "in-flight") {
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
			ctx._configDraft = defaultDraftFromCatalog(currentCatalog(ctx));
			ctx._configActiveName = null;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			renderChooserColumnList(ctx);
			refreshChooserDirty(ctx);
			return Promise.resolve();
		}
		return NS.persistence.load(table, name).then(function (blob) {
			ctx._configDraft = draftFromSavedView(currentCatalog(ctx), blob);
			ctx._configActiveName = name;
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);
			ctx._configDirty = false;
			renderChooserColumnList(ctx);
			refreshChooserDirty(ctx);
		}, function (e) {
			showChooserStatus(ctx, (toTypedError(e).message) || "Load failed.", true);
		});
	}

	function openChooser(table, ctx) {
		if (ctx._configBackdrop) {
			closeChooserDialog(ctx);
			return;
		}
		if (!ctx._configDraft)
			ctx._configDraft = draftFromSavedView(currentCatalog(ctx), null);
		if (ctx._configCleanSnapshot == null)
			ctx._configCleanSnapshot = snapshotDraft(ctx._configDraft);

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

		const tabs = document.createElement("div");
		tabs.className = "juneau-config-tabs";
		const viewTab = document.createElement("button");
		viewTab.type = "button";
		viewTab.className = "juneau-config-tab juneau-config-tab-active";
		paintUserText(viewTab, "View");
		tabs.appendChild(viewTab);
		dialog.appendChild(tabs);

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
				NS.persistence.setActive(table, name).catch(function () {});
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
				ctx._configDraft = defaultDraftFromCatalog(currentCatalog(ctx));
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

		dialog.appendChild(toolbar);

		const status = document.createElement("div");
		status.className = "juneau-config-status";
		status.hidden = true;
		ctx._configStatusEl = status;
		dialog.appendChild(status);

		const list = document.createElement("div");
		list.className = "juneau-config-col-list";
		ctx._configListEl = list;
		dialog.appendChild(list);

		const actions = document.createElement("div");
		actions.className = "juneau-config-actions";

		const applyBtn = document.createElement("button");
		applyBtn.type = "button";
		applyBtn.className = "juneau-config-apply";
		paintUserText(applyBtn, "Apply");
		applyBtn.addEventListener("click", function () { applyDraft(table, ctx); });
		actions.appendChild(applyBtn);

		const closeBtn = document.createElement("button");
		closeBtn.type = "button";
		paintUserText(closeBtn, "Close");
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

		renderChooserColumnList(ctx);
		refreshChooserDirty(ctx);
		NS.persistence.list(table).then(function (listing) {
			if (ctx._configActiveName === undefined)
				ctx._configActiveName = listing.active;
			fillViewSelect(ctx, listing);
		}, function () {
			fillViewSelect(ctx, { views: [] });
		});
	}

	/**
	 * Wires the Columns affordance onto the table toolbar when {@code columnConfig} is present.  Called from
	 * {@code constructTable} on first init AND every Apply rebuild.
	 */
	function mountChooser(table, ctx, toolbarRow) {
		if (!ctx || !ctx.viewDef || !ctx.viewDef.columnConfig) return;
		let host = null;
		if (toolbarRow)
			host = toolbarRow.querySelector(".juneau-view-toolbar-right") || toolbarRow;
		if (!host) {
			const wrapper = table && table.parentNode;
			if (!wrapper) return;
			host = wrapper.querySelector(".juneau-view-toolbar-right") || wrapper;
		}
		if (host.querySelector && host.querySelector(".juneau-config-chooser-btn")) return;

		const btn = document.createElement("button");
		btn.type = "button";
		btn.className = "juneau-view-ribbon-btn juneau-config-chooser-btn";
		btn.title = "Columns";
		btn.setAttribute("aria-label", "Columns");
		paintUserText(btn, "Columns");
		const markup = NS.icons && typeof NS.icons.resolveIcon === "function" ? NS.icons.resolveIcon("tune") : null;
		if (markup != null && typeof DOMParser === "function") {
			try {
				const doc = new DOMParser().parseFromString(markup, "image/svg+xml");
				const svg = doc.documentElement;
				if (svg && svg.tagName && svg.tagName.toLowerCase() === "svg") {
					btn.textContent = "";
					btn.appendChild(document.importNode ? document.importNode(svg, true) : svg);
				}
			} catch (e) { /* text fallback already applied */ }
		}
		btn.addEventListener("click", function () { openChooser(table, ctx); });
		host.appendChild(btn);

		if (ctx._configDraft == null) {
			ctx._configDraft = defaultDraftFromCatalog(currentCatalog(ctx));
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
})();
