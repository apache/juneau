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
 * juneau-urlstate.js - the shareable "Copy link" URL-state codec (design section 6.3).
 *
 * A single, dependency-free codec for the ONE opaque query parameter named `state`, whose value uses the Juneau
 * directives tab(...), filter(...), and sort(...) plus any directive a page registers via registerDirective.  It attaches to window.JuneauViews.urlState
 * (mirroring the other dependency-free pre-views globals: the icon registry, the column-search engine, and the
 * page-state store), so it loads as a plain <script> BEFORE juneau-views.js and juneau-config.js, which consume it.
 *
 * What the URL carries (design section 6.3):
 *   - the live TAB, the primary table's live FILTERS, and that table's live SORT - primary table only.
 *   - Filter bodies are the RAW column-search language (the same $-DSL as the header search popup).
 * What the URL NEVER carries:
 *   - View Settings (visible columns, labels, format, order, searchable/sortable membership, page size, wrap,
 *     density) - those live in the browser page-state store (juneau-pagestate.js) only.
 *   - Nested / child tables - only the primary table is encoded.
 *
 * Representation (design section 6.3): one readable `?state=` parameter, directly in the address bar (no IRS
 * promote-`?h=`-into-a-hash round trip).  The parser tolerates a percent-encoded value too, so a link that some
 * intermediary encoded still restores.
 *
 * The runtime glue (writeToAddressBar / buildShareUrl / copy) takes the browser's history / location / navigator
 * as explicit arguments rather than reaching for the globals, so the whole module is exercisable without a DOM.
 */
(function () {
	'use strict';

	const NS = window.JuneauViews = window.JuneauViews || {};
	const STATE_PARAM = 'state';

	function isBlank(v) { return v == null || String(v) === ''; }

	// -------------------------------------------------------------------------------------------------------------
	// Directive registry: tab/filter/sort are built in; a page may add more (the built-ins `window` and `ids` below).
	// -------------------------------------------------------------------------------------------------------------

	const RESERVED_DIRECTIVES = { tab: true, filter: true, sort: true };
	const DIRECTIVE_NAME_RE = /^[a-z][a-zA-Z0-9]{0,31}$/;
	const registry = Object.create(null);
	const directiveOrder = []; // registration order, so encodeState appends ext directives deterministically

	/**
	 * Registers a `?state=` directive beyond the built-in tab/filter/sort.
	 *
	 * <p>
	 * `codec` is `{ parse(body) -> value, serialize(value) -> body, isEmpty?(value), toParams?(value) -> object,
	 * apply?(value, table) }`.  `parse` and `serialize` must be inverses; build bodies with {@link encodeArg} so a value
	 * carrying the grammar's own separators round-trips.  A reserved, malformed, or already-registered name logs
	 * E-JS-67 and the directive simply does not exist.
	 *
	 * @example
	 *   NS.urlState.registerDirective('view', { parse: String, serialize: String });
	 */
	function registerDirective(name, codec) {
		if (RESERVED_DIRECTIVES[name] || ! DIRECTIVE_NAME_RE.test(String(name)) || registry[name]
			|| ! codec || typeof codec.parse !== 'function' || typeof codec.serialize !== 'function') {
			console.error('[juneau-urlstate] E-JS-67: url-state directive \'' + name + '\' is reserved, malformed, or already registered');
			return false;
		}
		registry[name] = { codec: codec };
		directiveOrder.push(name);
		return true;
	}

	/** Whether a directive is registered under `name` (built-in tab/filter/sort are not registry entries). */
	function hasDirective(name) { return !! registry[name]; }

	/** The registered codec for `name`, or null. */
	function directiveCodec(name) { return registry[name] ? registry[name].codec : null; }

	/** The registered directive names, in registration order. */
	function directiveNames() { return directiveOrder.slice(); }

	/** Percent-encodes the characters the directive grammar is sensitive to, so a codec body always parses. */
	function encodeArg(s) {
		return String(s == null ? '' : s).replace(/[;(),=%]/g, function (c) { return '%' + c.charCodeAt(0).toString(16).toUpperCase(); });
	}

	/** Decodes an {@link encodeArg} value; a malformed %-sequence is kept as-is. */
	function decodeArg(s) {
		try { return decodeURIComponent(String(s)); } catch (e) { return String(s); } // NOSONAR javascript:S2486 -- a malformed %-sequence keeps the raw text
	}

	/** Whether a registered directive's value is empty (so it is omitted from the encoded state). */
	function extValueEmpty(codec, value) {
		return codec.isEmpty ? !! codec.isEmpty(value) : (value == null || value === '');
	}

	/** Built-in `window` directive: body `start=<iso>;end=<iso>`, either half optional; `params` renames the request params. */
	function windowDirectiveCodec(cfg) {
		const params = (cfg && cfg.params) || {};
		const startParam = params.start || 'start';
		const endParam = params.end || 'end';
		return {
			parse: function (body) {
				const out = {};
				for (const part of String(body || '').split(';')) {
					const eq = part.indexOf('=');
					if (eq <= 0) continue;
					const k = part.substring(0, eq);
					if (k === 'start' || k === 'end') out[k] = decodeArg(part.substring(eq + 1));
				}
				return out;
			},
			serialize: function (v) {
				const out = [];
				if (v?.start) out.push('start=' + encodeArg(v.start));
				if (v?.end) out.push('end=' + encodeArg(v.end));
				return out.join(';');
			},
			isEmpty: function (v) { return ! v || (! v.start && ! v.end); },
			toParams: function (v) {
				const out = {};
				if (v?.start) out[startParam] = v.start;
				if (v?.end) out[endParam] = v.end;
				return out;
			}
		};
	}

	/** Built-in `ids` directive: body `a,b,c`; trims, drops blanks, dedupes, and caps at `max` (default 200). */
	function idsDirectiveCodec(cfg) {
		const param = cfg?.param || 'ids';
		const max = cfg?.max || 200;
		function normalize(raw) {
			const seen = Object.create(null);
			const ids = [];
			for (const part of String(raw || '').split(',')) {
				const v = decodeArg(part).trim();
				if (v === '') continue;
				if (seen[v] || ids.length >= max) continue;
				seen[v] = true;
				ids.push(v);
			}
			return { ids: ids, truncated: hasDroppedBeyondCap(raw, ids, max) };
		}
		return {
			parse: function (body) { return normalize(body); },
			serialize: function (v) { return (v?.ids || []).map(encodeArg).join(','); },
			isEmpty: function (v) { return ! v?.ids || v.ids.length === 0; },
			toParams: function (v) {
				const out = {};
				out[param] = (v?.ids || []).join(',');
				return out;
			},
			statusMessage: function (v) { return v?.truncated ? ('Showing the first ' + max + ' ids') : null; }
		};
	}

	/** Whether any distinct non-blank id in `raw` was left out of `kept` purely because of the cap (dupes do not count). */
	function hasDroppedBeyondCap(raw, kept, max) {
		if (kept.length < max) return false;
		const keptSet = Object.create(null);
		for (const k of kept) keptSet[k] = true;
		for (const part of String(raw || '').split(',')) {
			const v = decodeArg(part).trim();
			if (v !== '' && ! keptSet[v]) return true;
		}
		return false;
	}

	// -------------------------------------------------------------------------------------------------------------
	// Codec: {tab, filters:[{column,expr}], sort:{column,dir}} <-> "tab(x);filter(col=expr);sort(col=dir)"
	// -------------------------------------------------------------------------------------------------------------

	/**
	 * Encodes a live-state object into the `state` value.  Empty/blank facets are omitted; an empty state -> "".
	 *
	 * <p>
	 * The column NAME (the clause key) is escaped via the shared ClauseParser grammar so a name carrying the grammar's
	 * own separators (comma or equals) round-trips; the filter EXPRESSION and the sort direction are the value and are
	 * spliced RAW - a raw $-expression must never be escaped (its commas live inside its own parentheses), exactly as
	 * {@code ClauseParser.escape}/{@code DataTablesQuery} treat the value server-side.
	 */
	function encodeState(state) {
		if (!state) return '';
		const parts = [];
		if (! isBlank(state.tab))
			parts.push('tab(' + state.tab + ')');
		const filters = state.filters || [];
		for (const f of filters) {
			if (f && ! isBlank(f.column) && ! isBlank(f.expr))
				parts.push('filter(' + clauseEscape(String(f.column)) + '=' + f.expr + ')');
		}
		const sort = state.sort;
		if (sort && ! isBlank(sort.column) && ! isBlank(sort.dir))
			parts.push('sort(' + clauseEscape(String(sort.column)) + '=' + sort.dir + ')');
		const ext = state.ext || {};
		for (const extName of directiveOrder) {
			if (! (extName in ext)) continue;
			const codec = registry[extName].codec;
			if (extValueEmpty(codec, ext[extName])) continue;
			// '%' is doubled so decodeState's one whole-string percent-decode leaves the codec's own %XX escapes intact.
			parts.push(extName + '(' + String(codec.serialize(ext[extName])).replace(/%/g, '%25') + ')');
		}
		return parts.join(';');
	}

	// -------------------------------------------------------------------------------------------------------------
	// ClauseParser grammar (JS mirror of org.apache.juneau.commons.beanquery.ClauseParser) - the SAME grammar the
	// one `search` string and the one `opts` string use server-side.  A filter body is a list of `key=value` clauses:
	//   - top-level commas (outside parentheses) separate clauses; a comma inside parens is protected ($in(A,B));
	//   - the FIRST UNESCAPED `=` in a clause splits key from value (later `=` stay verbatim in the value);
	//   - backslash escapes decode to literals: \, -> comma, \= -> equals, \\ -> backslash, \$ -> dollar (any other
	//     \ is left as-is); a LEADING $ in a key is escaped on encode so it is never misread as a function-call token.
	// This is deliberately NOT a second grammar: it is a byte-for-byte port of the Java rules so the client and the
	// server read a `?state=` filter body identically.
	// -------------------------------------------------------------------------------------------------------------

	/** Escapes a single key so it can be spliced into a clause without changing its meaning (mirror of ClauseParser.escape). */
	function clauseEscape(s) {
		if (s == null) return s;
		let sb = '';
		for (let i = 0; i < s.length; i++) {
			const c = s.charAt(i);
			// A leading '$' and any of the grammar's own separators (backslash, comma, equals) get one escaping backslash.
			if ((i === 0 && c === '$') || c === '\\' || c === ',' || c === '=') sb += '\\';
			sb += c;
		}
		return sb;
	}

	/** Appends one escape sequence: \, / \= / \\ / \$ collapse; any other following char keeps the backslash. */
	function appendUnescaped(buf, c) {
		if (c === ',' || c === '=' || c === '\\' || c === '$') return buf + c;
		return buf + '\\' + c;
	}

	/** Whether `s` contains an unescaped `=` (gate for sort bodies; mirrors the prior indexOfUnescapedEquals > 0 check). */
	function hasUnescapedEquals(s) {
		let escaped = false;
		for (let i = 0; i < s.length; i++) {
			const c = s.charAt(i);
			if (escaped) escaped = false;
			else if (c === '\\') escaped = true;
			else if (c === '=') return true;
		}
		return false;
	}

	/**
	 * ClauseParser state machine (mirror of Java ClauseParser.parse).  Returns an ordered list of
	 * `{ key, value }` (key stripped; blank keys omitted).
	 *
	 * S1: Reading key (looking for unescaped '=' or top-level ',' or end).
	 * S2: Saw '\', next char is a key literal.
	 * S3: Reading value (looking for top-level ',' or end); '(' / ')' adjust depth.
	 * S4: Saw '\', next char is a value literal.
	 */
	function parseClauses(s) { // NOSONAR javascript:S3776 -- URL state parsing; complexity is inherent
		const out = [];
		if (s == null || s === '') return out;
		const S1 = 1, S2 = 2, S3 = 3, S4 = 4;
		let state = S1, key = '', value = '', depth = 0;
		const len = s.length;

		function putClause() {
			const k = key.trim();
			if (k !== '') out.push({ key: k, value: value });
			key = '';
			value = '';
			depth = 0;
		}

		for (let i = 0; i <= len; i++) {
			const end = i === len;
			const c = end ? ',' : s.charAt(i); // Synthetic comma flushes the final clause.

			if (state === S1) {
				if (end || (c === ',' && depth === 0)) {
					putClause();
				} else if (c === '\\') {
					state = S2;
				} else if (c === '=') {
					state = S3;
				} else {
					if (c === '(') depth++;
					else if (c === ')' && depth > 0) depth--;
					key += c;
				}
			} else if (state === S2) {
				if (end) {
					key += '\\';
					putClause();
				} else {
					key = appendUnescaped(key, c);
					state = S1;
				}
			} else if (state === S3) {
				if (end || (c === ',' && depth === 0)) {
					putClause();
					state = S1;
				} else if (c === '\\') {
					state = S4;
				} else {
					if (c === '(') depth++;
					else if (c === ')' && depth > 0) depth--;
					value += c;
				}
			} else if (state === S4) {
				if (end) {
					value += '\\';
					putClause();
				} else {
					value = appendUnescaped(value, c);
					state = S3;
				}
			}
		}
		return out;
	}

	/**
	 * Decodes a `state` value into a live-state object.  An unknown directive (e.g. an IRS `subview`) is dropped, so
	 * nested-table or View-Settings state can never leak in.  Always returns an object (never null); read
	 * emptiness via {@link isEmptyState}.
	 *
	 * <p>
	 * State machine over the directive stream (top-level `;` separators; parentheses protect nested `;`):
	 * S1: Reading directive name (looking for '(' or top-level ';' / end).
	 * S2: Reading directive body inside balanced parentheses.
	 * S3: After a matching `)`, only whitespace is allowed until `;` / end (else the directive is dropped — same
	 *     as the prior {@code parseDirective} requirement that the token end with `)`).
	 * S4: Discarding until the next top-level `;` after a malformed directive.
	 */
	function decodeState(str) { // NOSONAR javascript:S3776 -- URL state parsing; complexity is inherent
		const state = { tab: null, filters: [], sort: null, ext: {} };
		if (str == null) return state;
		let raw = String(str);
		if (raw.indexOf('%') >= 0) {
			try { raw = decodeURIComponent(raw); } catch (e) { /* keep raw on a malformed %-sequence */ } // NOSONAR javascript:S2486 -- a malformed %-sequence keeps the raw text
		}

		const S1 = 1, S2 = 2, S3 = 3, S4 = 4;
		let st = S1, name = '', body = '', depth = 0, pendingName = '', pendingBody = '';
		const len = raw.length;

		function applyDirective(n, b) {
			n = n.trim();
			if (n === '') return;
			if (n === 'tab') {
				state.tab = b;
			} else if (n === 'filter') {
				// Filter body: ClauseParser clauses; keep only non-empty column+expr pairs (URL-state contract).
				const clauses = parseClauses(b);
				for (const clause of clauses) {
					const column = clause.key;
					const expr = clause.value;
					if (column !== '' && expr !== '') state.filters.push({ column: column, expr: expr });
				}
			} else if (n === 'sort') {
				// Require an unescaped '=' (prior indexOfUnescapedEquals > 0); bare `col` is not a sort directive.
				if (! hasUnescapedEquals(b)) return;
				const sortClauses = parseClauses(b);
				if (sortClauses.length > 0 && sortClauses[0].key !== '')
					state.sort = { column: sortClauses[0].key, dir: sortClauses[0].value };
			} else if (registry[n]) {
				try {
					state.ext[n] = registry[n].codec.parse(b);
				} catch (e) {
					// E-JS-68: the directive is dropped; tab/filter/sort already decoded are kept.
					console.error('[juneau-urlstate] E-JS-68: url-state directive \'' + n + '\' could not be parsed: ' + (e?.message || e));
				}
			}
			// An unregistered directive name is dropped (design section 6.3, extended: tab/filter/sort plus whatever the page registered).
		}

		function flushPending() {
			if (pendingName !== '' || pendingBody !== '')
				applyDirective(pendingName, pendingBody);
			pendingName = '';
			pendingBody = '';
		}

		function clearAll() {
			name = '';
			body = '';
			depth = 0;
			pendingName = '';
			pendingBody = '';
		}

		for (let i = 0; i <= len; i++) {
			const end = i === len;
			const c = end ? ';' : raw.charAt(i); // Synthetic ';' flushes a trailing directive.

			if (st === S1) {
				if (end || c === ';') {
					clearAll();
				} else if (c === '(') {
					st = S2;
					depth = 1;
					body = '';
				} else {
					name += c;
				}
			} else if (st === S2) {
				if (end) {
					// Unclosed paren - drop (prior parseDirective required a trailing ')').
					clearAll();
					st = S1;
				} else if (c === '(') {
					depth++;
					body += c;
				} else if (c === ')') {
					depth--;
					if (depth === 0) {
						pendingName = name;
						pendingBody = body;
						name = '';
						body = '';
						st = S3;
					} else {
						body += c;
					}
				} else {
					body += c;
				}
			} else if (st === S3) {
				if (end || c === ';') {
					flushPending();
					clearAll();
					st = S1;
				} else if (c === ' ' || c === '\t' || c === '\n' || c === '\r') {
					// Whitespace between ')' and ';' is tolerated the same way token.trim() was.
				} else {
					// Junk after ')' — drop the whole directive (prior parseDirective null).
					clearAll();
					st = S4;
				}
			} else if (st === S4) {
				if (end || c === ';') {
					clearAll();
					st = S1;
				}
			}
		}
		return state;
	}

	/** Whether a decoded state carries no tab, no filters, and no sort (i.e. nothing to restore); registered directives count too. */
	function isEmptyState(state) {
		if (! state) return true;
		if (! isBlank(state.tab) || (state.filters && state.filters.length > 0) || state.sort) return false;
		const ext = state.ext || {};
		for (const name of Object.keys(ext)) {
			const codec = registry[name]?.codec;
			if (codec && ! extValueEmpty(codec, ext[name])) return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------------------------------------------
	// Query-string plumbing (pure string work; no globals)
	// -------------------------------------------------------------------------------------------------------------

	/** Reads and decodes the `state` parameter out of a location.search string, or null when it is absent. */
	function readFromSearch(search) {
		if (search == null) return null;
		let s = String(search);
		if (s.startsWith('?')) s = s.substring(1);
		if (s === '') return null;
		const parts = s.split('&');
		const prefix = STATE_PARAM + '=';
		for (const part of parts) {
			if (part.startsWith(prefix)) return decodeState(part.substring(prefix.length));
		}
		return null;
	}

	/** Rebuilds a search string, replacing (or dropping, when `encoded` is empty) the `state` param and keeping the rest. */
	function buildSearch(existingSearch, encoded) {
		let s = existingSearch == null ? '' : String(existingSearch);
		if (s.startsWith('?')) s = s.substring(1);
		const kept = [];
		const prefix = STATE_PARAM + '=';
		if (s !== '') {
			const parts = s.split('&');
			for (const part of parts) {
				if (part !== '' && ! part.startsWith(prefix)) kept.push(part);
			}
		}
		if (encoded && encoded !== '') kept.push(prefix + encoded);
		return kept.length ? '?' + kept.join('&') : '';
	}

	// -------------------------------------------------------------------------------------------------------------
	// Runtime glue (history / location / navigator passed in, so this is testable without a browser)
	// -------------------------------------------------------------------------------------------------------------

	/**
	 * Syncs the live address bar to the current state via history.replaceState (design section 6.3 default).  A
	 * no-op (returning false) under the clean-address option, or when history/location are unusable - the
	 * address bar then stays clean while the user works, and only Copy link carries the state.
	 */
	function writeToAddressBar(history, location, state, opts) {
		if (opts?.clean) return false;
		if (typeof history?.replaceState !== 'function' || ! location) return false;
		const newSearch = buildSearch(location.search, encodeState(state));
		const path = (location.pathname || '') + newSearch + (location.hash || '');
		try { history.replaceState(history.state || null, '', path); } catch (e) { return false; } // NOSONAR javascript:S2486 -- replaceState can throw (sandboxed frames); report false
		return true;
	}

	/**
	 * Builds the full shareable URL for the current state - ALWAYS carrying `?state=` even under the clean-address
	 * option (that is the whole point of Copy link: the clipboard URL restores state even when the address bar did
	 * not show it).
	 */
	function buildShareUrl(location, state) {
		if (! location) return '';
		const newSearch = buildSearch(location.search, encodeState(state));
		return (location.origin || '') + (location.pathname || '') + newSearch + (location.hash || '');
	}

	/** Writes `text` to the clipboard via the async Clipboard API; resolves false (never throws) where it is absent/denied. */
	function copy(navigatorLike, text) {
		try {
			if (typeof navigatorLike?.clipboard?.writeText === 'function')
				return Promise.resolve(navigatorLike.clipboard.writeText(text)).then(function () { return true; }, function () { return false; });
		} catch (e) { /* fall through to the resolved-false path */ } // NOSONAR javascript:S2486 -- falls through to the resolved-false path
		return Promise.resolve(false);
	}

	/**
	 * Whether the primary datatable opted into the clean-address option (design section 6.3).  Read from the view's
	 * VIEW_META `cleanAddress` boolean, or the host table's `data-juneau-clean-address="true"` attribute.
	 */
	function cleanAddressEnabled(view, table) {
		if (view?.cleanAddress === true) return true;
		const attr = table?.dataset?.juneauCleanAddress ?? null;
		return attr === 'true' || attr === '1';
	}

	/**
	 * Open precedence (design section 6.3): `?state=` WINS over the browser store for tab / filters / sort.  It does
	 * NOT replace the recipient's saved View Settings - those are a separate store facet this codec never touches, so
	 * "wins" here means only "which tab/filter/sort object the caller applies to the live grid".
	 */
	function resolveOpenState(urlState, storeState) {
		return (! isEmptyState(urlState)) ? urlState : (storeState || null);
	}

	NS.urlState = {
		STATE_PARAM: STATE_PARAM,
		encode: encodeState,
		decode: decodeState,
		isEmptyState: isEmptyState,
		readFromSearch: readFromSearch,
		buildSearch: buildSearch,
		writeToAddressBar: writeToAddressBar,
		buildShareUrl: buildShareUrl,
		copy: copy,
		cleanAddressEnabled: cleanAddressEnabled,
		resolveOpenState: resolveOpenState,
		registerDirective: registerDirective,
		hasDirective: hasDirective,
		directiveCodec: directiveCodec,
		directiveNames: directiveNames,
		encodeArg: encodeArg,
		decodeArg: decodeArg,
		builtins: { window: windowDirectiveCodec, ids: idsDirectiveCodec },
		// Needs a live table, which this DOM-free module never has: delegates to juneau-views.js at call time, so
		// either script load order works.
		setDirective: function (tableEl, name, value) {
			const run = NS.init?.setUrlStateDirective;
			if (typeof run !== 'function') {
				console.error('[juneau-urlstate] E-JS-67: setDirective needs juneau-views.js loaded');
				return false;
			}
			return run(tableEl, name, value);
		}
	};
})();
