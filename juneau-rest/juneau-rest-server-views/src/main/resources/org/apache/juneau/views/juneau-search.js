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
 * juneau-search.js - dependency-free client mirror of the server-side column-search engine.
 *
 * This is the JavaScript counterpart of the Java classes in org.apache.juneau.commons.beanquery:
 *   - SearchExpressionParser / SearchExpression                  -> parse(raw) -> a descriptor whose
 *     empty/complete/incomplete/invalid/valid flags follow the SAME truth table the Java doc pins
 *     (blank -> empty+complete+valid; mid-typing draft -> none; malformed -> complete but not valid;
 *     well-formed -> complete+valid).
 *   - SearchType / SearchOperator / SearchOperatorSet             -> SearchType/operators: the wire tokens,
 *     per-type operator applicability (design section 6), arity, and the popup help text (design section 4.5).
 *   - InMemoryMatch                                               -> createEngine(): rows/accessor/custom/search,
 *     with byte-identical leaf semantics per type so a client-mode grid filters exactly as the server would.
 *
 * The grammar is operator-agnostic and purely structural, matching SearchExpressionParser: a bare token (no leading
 * "$") is a quick-filter literal; "$name(arg,...)" is a function whose arguments are themselves parsed. Arity,
 * per-type applicability, and combinator-vs-leaf semantics are decided by the evaluator, not the parser -
 * malformed and mid-typing inputs are NEVER thrown to callers, they resolve to INVALID/INCOMPLETE state so an
 * in-progress draft does not blank the grid and a bad expression shows a reject rather than matching every row.
 *
 * It attaches to window.JuneauViews.search (mirroring juneau-icons.js's window.JuneauViews.icons), so it is a
 * plain browser IIFE with no DOM/jQuery/DataTables dependency; the always-on Node parity harness
 * (src/test/js/column-search.cjs) loads it into a vm sandbox and asserts it against the Java corpus.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// -----------------------------------------------------------------------------------------------------------------
	// SearchType (mirror of SearchType.java): wire tokens + case-insensitive fromWire.
	// -----------------------------------------------------------------------------------------------------------------

	const TYPES = ["text", "id", "numeric", "version", "timestamp", "enum", "boolean"];

	const SearchType = {
		TEXT: "text",
		ID: "id",
		NUMERIC: "numeric",
		VERSION: "version",
		TIMESTAMP: "timestamp",
		ENUM: "enum",
		BOOLEAN: "boolean",
		/** Resolves a lowercase wire token (case-insensitive) back to its type, or null when null/blank/unknown. */
		fromWire: function (wire) {
			if (wire == null)
				return null;
			const w = String(wire).trim().toLowerCase();
			return TYPES.find(function (t) { return t === w; }) ?? null;
		}
	};

	// -----------------------------------------------------------------------------------------------------------------
	// SearchOperator / SearchOperatorSet (mirror of SearchOperator.java + SearchOperatorSet.java): the built-in
	// operator catalog with arity, per-type applicability, and popup help text.  Combinators apply to every type.
	// -----------------------------------------------------------------------------------------------------------------

	const T_TEXT = SearchType.TEXT, T_ID = SearchType.ID, T_NUM = SearchType.NUMERIC, T_VER = SearchType.VERSION,
		T_TS = SearchType.TIMESTAMP, T_ENUM = SearchType.ENUM, T_BOOL = SearchType.BOOLEAN;

	/** Builds one operator-metadata record; a combinator applies to all types (its `types` list is ignored). */
	function op(name, minArgs, maxArgs, combinator, help, types) {
		const typeSet = combinator ? TYPES.slice() : (types || []);
		return {
			name: name,
			minArgs: minArgs,
			maxArgs: maxArgs,
			combinator: combinator,
			custom: false,
			help: help,
			types: typeSet,
			/** Whether this operator is offered for `type` (combinators apply to all types). */
			appliesTo: function (type) {
				return type != null && (this.combinator || this.types.includes(type));
			},
			/** Whether `count` args satisfy [minArgs, maxArgs] (upper bound ignored when maxArgs < 0). */
			acceptsArgCount: function (count) {
				return count >= this.minArgs && (this.maxArgs < 0 || count <= this.maxArgs);
			}
		};
	}

	// Canonical order matches SearchOperatorSet.java's standard() insertion order.
	const BUILTIN_LIST = [
		op("$eq", 1, 1, false, "Exact, case-sensitive match. Example: $eq(OPEN)", [T_TEXT, T_ID, T_NUM, T_VER, T_TS, T_ENUM, T_BOOL]),
		op("$eqic", 1, 1, false, "Case-insensitive match. Example: $eqic(open)", [T_TEXT, T_ID, T_ENUM]),
		op("$ne", 1, -1, false, "None of the given values, case-sensitive (1 or more). Blank cells are kept. Example: $ne(OPEN,CLOSED)", [T_TEXT, T_ID, T_NUM, T_VER, T_TS, T_ENUM, T_BOOL]),
		op("$in", 1, -1, false, "Any of the given values, case-sensitive (1 or more). Example: $in(OPEN,CLOSED)", [T_TEXT, T_ID, T_NUM, T_VER, T_TS, T_ENUM]),
		op("$and", 2, -1, true, "All of the given sub-expressions match (2 or more). Example: $and($gt(1),$lt(9))"),
		op("$or", 2, -1, true, "Any of the given sub-expressions matches (2 or more). Example: $or($eq(OPEN),$eq(CLOSED))"),
		op("$not", 1, 1, true, "Negates a single sub-expression. Example: $not($eq(OPEN))"),
		op("$contains", 1, 1, false, "Case-insensitive substring match. Example: $contains(err)", [T_TEXT, T_ID]),
		op("$prefix", 1, 1, false, "Prefix match (string prefix, or dotted-tuple prefix for versions). Example: $prefix(10.0)", [T_TEXT, T_ID, T_VER]),
		op("$regex", 1, 2, false, "Full-string regular expression, case-insensitive by default. Portable flags i/m/s. Example: $regex(err.*, flags=i)", [T_TEXT, T_ID]),
		op("$blank", 0, 0, false, "Matches an empty or whitespace-only cell. Takes no arguments. Example: $blank()", [T_TEXT, T_ID]),
		op("$gt", 1, 1, false, "Greater than. Example: $gt(100)", [T_NUM, T_VER, T_TS]),
		op("$gte", 1, 1, false, "Greater than or equal. Example: $gte(100)", [T_NUM, T_VER, T_TS]),
		op("$lt", 1, 1, false, "Less than. Example: $lt(100)", [T_NUM, T_VER, T_TS]),
		op("$lte", 1, 1, false, "Less than or equal. Example: $lte(100)", [T_NUM, T_VER, T_TS]),
		op("$between", 2, 2, false, "Inclusive range between two bounds. Example: $between(1, 100)", [T_NUM, T_VER, T_TS])
	];

	const BUILTINS = {};
	for (let bi = 0; bi < BUILTIN_LIST.length; bi++)
		BUILTINS[BUILTIN_LIST[bi].name.toLowerCase()] = BUILTIN_LIST[bi];

	const SearchOperators = {
		/** All built-in operators in canonical order. */
		builtins: function () {
			return BUILTIN_LIST.slice();
		},
		/** Looks up a built-in operator by "$"-name, case-insensitively (D6), or null if not a built-in. */
		get: function (name) {
			return name == null ? null : (BUILTINS[String(name).toLowerCase()] || null);
		},
		/** Whether the name is a built-in operator (case-insensitive, D6). */
		isBuiltin: function (name) {
			return name != null && Object.hasOwn(BUILTINS, String(name).toLowerCase());
		},
		/** Built-in operators offered for `type` (design section 6), combinators included, in canonical order. */
		forType: function (type) {
			const out = [];
			if (type != null)
				for (const builtin of BUILTIN_LIST)
					if (builtin.appliesTo(type))
						out.push(builtin);
			return out;
		}
	};

	// -----------------------------------------------------------------------------------------------------------------
	// Parser (mirror of SearchExpressionParser.java + SearchExpression.java).
	// -----------------------------------------------------------------------------------------------------------------

	// Internal control-flow signals, distinguished by their `state` (never surfaced to callers).
	const INCOMPLETE_SIGNAL = { signal: "INCOMPLETE" };
	const INVALID_SIGNAL = { signal: "INVALID" };

	// Quote-tracking states for parseFunc's/scanArgs' arg-scan loop (hoisted to module scope so scanArgs, shared by
	// parseFunc and parseFuncStrict, can reference them too).
	const S1 = 1, S2 = 2, S3 = 3;

	/** A literal (bare token or leaf argument) node: name===null, no args. */
	function literalNode(value, quoted) {
		return { name: null, value: value, args: [], quoted: !! quoted };
	}

	/** A function ("$"-operator invocation) node. */
	function funcNode(name, args) {
		return { name: name, value: null, args: args };
	}

	function isLiteral(node) {
		return node.name === null;
	}

	function argCount(node) {
		return node.args.length;
	}

	/** A function's arguments as literal strings (a nested-function arg contributes its "$"-name), like SearchNode.literalArgs(). */
	function literalArgs(node) {
		return node.args.map(function (a) { return isLiteral(a) ? a.value : a.name; });
	}

	// Unicode-aware letter/digit tests, matching Java's Character.isLetter/isLetterOrDigit (NOT ASCII-only
	// [A-Za-z0-9] - an operator name with a non-ASCII letter, e.g. "$éq", must be classified identically
	// by both engines or Java and JS disagree on whether it's a function call or a bare literal).
	const IS_LETTER = /^\p{L}$/u;
	const IS_LETTER_OR_DIGIT = /^[\p{L}\p{Nd}]$/u;

	/** A valid operator name is "$" followed by one or more letters/digits (leading "$" is a precondition). */
	function isValidName(name) {
		if (name.length < 2)
			return false;
		for (let i = 1; i < name.length; i++) {
			const c = name.charAt(i);
			if (! IS_LETTER_OR_DIGIT.test(c))
				return false;
		}
		return true;
	}

	/** True if `s` is wrapped in one matching pair of quote characters (`"…"` or `'…'`), length 2 or more. */
	function isQuotedToken(s) {
		if (s.length < 2)
			return false;
		const first = s.charAt(0);
		return (first === '"' || first === "'") && s.charAt(s.length - 1) === first;
	}

	/**
	 * True if `s` opens a quote (`"` or `'` as its first character) that its own last character does not close
	 * (mirror of Java SearchExpressionParser's `isUnterminatedQuote`). Used only by the strict/throwing path
	 * (`parseExprStrict`/`parseFuncStrict`) - the lenient, live-typing `parseExpr` deliberately has no such check,
	 * since a quote the user is still typing is a normal mid-draft state there, not an error.
	 */
	function isUnterminatedQuote(s) {
		const first = s.charAt(0);
		return (first === '"' || first === "'") && ! isQuotedToken(s);
	}

	/**
	 * Decodes one literal token: strips a matching outer quote pair (un-doubling a doubled quote character inside
	 * to one literal quote, mirroring the Java parser's `"…""…"` rule), then decodes `$$` to a literal `$`
	 * anywhere in the result (bare or quoted) per D1.
	 */
	function decodeLiteral(s) {
		let body;
		if (isQuotedToken(s)) {
			const q = s.charAt(0);
			const inner = s.substring(1, s.length - 1);
			let out = '';
			for (let i = 0; i < inner.length; i++) {
				if (inner.charAt(i) === q && inner.charAt(i + 1) === q) {
					out += q;
					i++;
				} else {
					out += inner.charAt(i);
				}
			}
			body = out;
		} else {
			body = s;
		}
		return body.split('$$').join('$');
	}

	/** Parses one (already trimmed, non-empty) token into a node. */
	function parseExpr(s) {
		// D1: "$" starts an operator call only when followed by a letter; "$100", "$-x", "$$", and a lone "$"
		// are literal text (after $$-decoding). A "$" + letter with no "(" still falls through to parseFunc's
		// own INCOMPLETE_SIGNAL (live-typing path) rather than being reclassified as a literal here.
		if (s.length > 1 && s.charAt(0) === '$' && IS_LETTER.test(s.charAt(1)))
			return parseFunc(s);
		return literalNode(decodeLiteral(s), isQuotedToken(s));
	}

	/**
	 * Scans `s` starting at `s.charAt(open) === '('` for the matching close paren, splitting top-level (paren
	 * depth 1, outside quotes) commas into `rawArgs` (mirror of Java SearchExpressionParser.parseFunc's single
	 * scan). Shared by `parseFunc` (signal-based, non-throwing) and `parseFuncStrict` (coded-error-based) so both
	 * grammars stay byte-for-byte identical.
	 *
	 * <p>
	 * S1: Outside quotes, tracking paren depth (looking for top-level ',' or matching ')').
	 * S2: Inside a double-quoted argument fragment (a doubled `""` stays inside the span - D1 doubled-quote rule).
	 * S3: Inside a single-quoted argument fragment (same doubling rule for `''`).
	 *
	 * @return {close, rawArgs, state, blankArg} where `close` is the matching ')' index (-1 if the string ran out
	 * first); `state` is `'balanced'` on a normal close, `'unterminatedQuote'` when the scan ended inside an open
	 * quote, or `'unbalanced'` when it ran out of input outside a quote; `blankArg` is true when some
	 * comma-separated argument slot was empty (a stray or trailing comma) - `$blank()`'s zero-argument call is
	 * NOT a blank arg.
	 */
	function scanArgs(s, open) {
		let state = S1, depth = 0, argStart = open + 1, blankArg = false;
		const rawArgs = [];

		for (let i = open; i < s.length; i++) {
			const c = s.charAt(i);
			if (state === S1) {
				if (c === '"') {
					state = S2;
				} else if (c === "'") {
					state = S3;
				} else if (c === "(") {
					depth++;
				} else if (c === ")") {
					if (--depth === 0) {
						if (! (argStart === i && rawArgs.length === 0)) {
							// Not "$blank()" (zero arguments) - record the final argument.
							const tail = s.substring(argStart, i);
							if (tail.trim() === "")
								blankArg = true;
							rawArgs.push(tail);
						}
						return { close: i, rawArgs: rawArgs, state: "balanced", blankArg: blankArg };
					}
				} else if (c === "," && depth === 1) {
					const part = s.substring(argStart, i);
					if (part.trim() === "")
						blankArg = true;
					rawArgs.push(part);
					argStart = i + 1;
				}
			} else if (state === S2) {
				if (c === '"') {
					if (s.charAt(i + 1) === '"')
						i++;  // doubled quote: one literal quote char, stays inside the quoted span
					else
						state = S1;
				}
			} else if (state === S3) {
				if (c === "'") {
					if (s.charAt(i + 1) === "'")
						i++;
					else
						state = S1;
				}
			}
		}
		return { close: -1, rawArgs: rawArgs, state: (state === S2 || state === S3) ? "unterminatedQuote" : "unbalanced", blankArg: blankArg };
	}

	/**
	 * Parses a "$name(args...)" token that must span the entire string `s` (mirror of Java
	 * SearchExpressionParser.parseFunc). Non-throwing-signal variant: live-typing drafts and malformed calls
	 * resolve to INCOMPLETE_SIGNAL/INVALID_SIGNAL rather than a coded error - see `parseFuncStrict` for the
	 * throwing counterpart that shares `scanArgs`.
	 */
	function parseFunc(s) {
		const open = s.indexOf("(");
		if (open < 0)
			throw INCOMPLETE_SIGNAL;  // Still typing the name/args (e.g. "$eq", "$").
		const name = s.substring(0, open);
		if (! isValidName(name))
			throw INVALID_SIGNAL;

		const info = scanArgs(s, open);
		if (info.close < 0)
			throw INCOMPLETE_SIGNAL;  // Unclosed paren, or an unterminated quote while still typing.
		if (info.blankArg)
			throw INVALID_SIGNAL;  // Stray/trailing comma.
		if (s.substring(info.close + 1).trim() !== "")
			throw INVALID_SIGNAL;  // Junk after the close paren.
		const args = [];
		for (const rawArg of info.rawArgs)
			args.push(parseExpr(rawArg.trim()));
		return funcNode(name, args);
	}

	/**
	 * Parses a raw column-search expression into a descriptor.  The descriptor exposes the empty/complete/
	 * incomplete/invalid/valid flags the header popup and grid use to decide whether to filter, keep the grid
	 * unchanged, or show a reject indicator (design section 5).  The parse tree itself (`root`) is present for the
	 * evaluator; it is intentionally not part of the cross-boundary contract.
	 */
	function parse(raw) {
		if (raw == null || String(raw).trim() === "")
			return descriptor("EMPTY", null, raw);
		try {
			return descriptor("VALID", parseExpr(String(raw).trim()), raw);
		} catch (e) {
			if (e === INCOMPLETE_SIGNAL)
				return descriptor("INCOMPLETE", null, raw);
			if (e === INVALID_SIGNAL)
				return descriptor("INVALID", null, raw);
			throw e;
		}
	}

	/** Builds the parse-result descriptor, mirroring SearchExpression's public accessors as flags. */
	function descriptor(state, root, raw) {
		return {
			raw: raw,
			root: root,
			empty: state === "EMPTY",
			complete: state !== "INCOMPLETE",
			incomplete: state === "INCOMPLETE",
			invalid: state === "INVALID",
			valid: state === "VALID" || state === "EMPTY"
		};
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Value coercion (mirror of InMemoryMatch.java's str/bool/decimal/millis/version helpers).
	// -----------------------------------------------------------------------------------------------------------------

	function strVal(o) {
		return o == null ? null : String(o);
	}

	function isBlankCell(cell) {
		const v = strVal(cell);
		return v == null || v.trim() === "";
	}

	function boolVal(o) {
		if (o === true || o === false)
			return o;
		let v = strVal(o);
		if (v == null)
			return null;
		v = v.trim().toLowerCase();
		if (v === "true")
			return true;
		if (v === "false")
			return false;
		return null;
	}

	const DECIMAL_RE = /^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$/;

	function decimalVal(o) {
		if (typeof o === "number")
			return Number.isFinite(o) ? o : null;
		let v = strVal(o);
		if (v == null || v.trim() === "")
			return null;
		v = v.trim();
		if (! DECIMAL_RE.test(v))
			return null;
		const n = Number(v);
		return Number.isNaN(n) ? null : n;
	}

	const INT_RE = /^[+-]?\d+$/;
	const ISO_INSTANT_RE = /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?(Z|[+-]\d\d:?\d\d)$/;
	const BARE_DATE_RE = /^\d{4}-\d\d-\d\d$/;
	// ISO date-time with optional seconds and optional offset (no offset = UTC, as in Java InMemoryMatch / SearchType.TIMESTAMP).
	const ISO_DATETIME_RE = /^(\d{4}-\d\d-\d\d)T(\d\d:\d\d)(:\d\d(?:\.\d{1,9})?)?(Z|[+-]\d\d:\d\d)?$/;
	const MILLIS_RE = /^-?\d+$/;

	function millisVal(o) {
		if (typeof o === "number")
			return Math.trunc(o);
		if (o instanceof Date)
			return o.getTime();
		let v = strVal(o);
		if (v == null || v.trim() === "")
			return null;
		v = v.trim();
		if (ISO_INSTANT_RE.test(v)) {
			const t = Date.parse(v);
			return Number.isNaN(t) ? null : t;
		}
		const dm = ISO_DATETIME_RE.exec(v);
		if (dm != null) {
			if (millisVal(dm[1]) == null)
				return null;  // Not a real calendar date.
			const dt2 = Date.parse(dm[1] + "T" + dm[2] + (dm[3] || ":00") + (dm[4] || "Z"));
			return Number.isNaN(dt2) ? null : dt2;
		}
		if (BARE_DATE_RE.test(v)) {
			// Date.parse(v + "T00:00:00Z") silently rolls an invalid calendar date (e.g. "2024-02-30") over into
			// the next valid day instead of rejecting it, unlike Java's LocalDate.parse. Validate the
			// year/month/day round-trip explicitly instead. A fixed non-two-digit-year reference (2000) avoids
			// Date.UTC's legacy quirk of treating a literal year 0-99 as 1900+year.
			const parts = v.split("-");
			const y = Number.parseInt(parts[0], 10), mo = Number.parseInt(parts[1], 10), d = Number.parseInt(parts[2], 10);
			const dt = new Date(Date.UTC(2000, mo - 1, d));
			dt.setUTCFullYear(y);
			if (dt.getUTCFullYear() !== y || dt.getUTCMonth() !== (mo - 1) || dt.getUTCDate() !== d)
				return null;
			return dt.getTime();
		}
		if (MILLIS_RE.test(v))
			return Number.parseInt(v, 10);
		return null;
	}

	/** Parses a dotted version tuple into an int array, or null when any part is not an integer. */
	function versionTuple(v) {
		if (v == null || v.trim() === "")
			return null;
		const parts = v.trim().split(".");
		const out = [];
		for (const part of parts) {
			const p = part.trim();
			if (! INT_RE.test(p))
				return null;
			out.push(Number.parseInt(p, 10));
		}
		return out;
	}

	/** Ordered version compare, or null when either side is unparseable (mirror of cmpVersion). */
	function cmpVersion(a, b) {
		const x = versionTuple(a), y = versionTuple(b);
		if (x == null || y == null)
			return null;
		const n = Math.min(x.length, y.length);
		for (let i = 0; i < n; i++)
			if (x[i] !== y[i])
				return x[i] < y[i] ? -1 : 1;
		return Math.sign(x.length - y.length);
	}

	function versionPrefix(cell, arg) {
		const c = versionTuple(cell), a = versionTuple(arg);
		if (c == null || a == null || a.length > c.length)
			return false;
		return a.every(function (part, i) { return part === c[i]; });
	}

	// Relative-duration literal "[+-]<n>(ms|s|m|h|d)" (mirror of ValueParse.DURATION): anchored, so a bare "-5" is a plain number.
	const DURATION_RE = /^([+-])(\d+)(ms|s|m|h|d)$/;
	// Signed ISO-8601 duration (mirror of ValueParse.isoDurationMillis / java.time.Duration.parse): one leading sign only,
	// days/hours/minutes/seconds (fractional seconds, '.' or ','), case-insensitive; years/months/weeks and per-component signs are invalid.
	const ISO_START_RE = /^[+-]?P/i;
	const ISO_RE = /^([+-])?P(?:(\d+)D)?(T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)(?:[.,](\d{0,9}))?S)?)?$/i;
	const UNIT_MS = { ms: 1, s: 1000, m: 60000, h: 3600000, d: 86400000 };
	const LONG_MAX = typeof BigInt === "function" ? BigInt("9223372036854775807") : null;

	/**
	 * The signed milliseconds of a relative-duration literal; null when `v` is not shaped like one; NaN when it is but
	 * its millisecond total overflows a Java long (Java: BAD_VALUE).
	 */
	function durationMillis(v) {
		if (ISO_START_RE.test(v))
			return isoDurationMillis(v);
		const m = DURATION_RE.exec(v);
		if (m == null)
			return null;
		const unit = UNIT_MS[m[3]];
		if (LONG_MAX != null) {
			const total = BigInt(m[2]) * BigInt(unit);
			if (total > LONG_MAX)
				return Number.NaN;
			return m[1] === "-" ? -Number(total) : Number(total);
		}
		const n = Number(m[2]) * unit;
		if (! Number.isFinite(n) || n > 9.2e18)
			return Number.NaN;
		return m[1] === "-" ? -n : n;
	}

	/** Signed milliseconds (fractions floored) of an ISO-8601 duration; NaN when it is malformed or its total overflows a Java long (Java: BAD_VALUE). */
	function isoDurationMillis(v) {
		const m = ISO_RE.exec(v);
		if (m == null || (m[3] === "T" && m[4] == null && m[5] == null && m[6] == null) || (m[2] == null && m[3] == null))
			return Number.NaN;
		const neg = m[1] === "-";
		if (LONG_MAX != null) {
			const frac = m[7] == null ? "" : m[7];
			const nanos = BigInt((frac + "000000000").substring(0, 9));
			const secs = BigInt(m[2] || 0) * BigInt(86400) + BigInt(m[4] || 0) * BigInt(3600) + BigInt(m[5] || 0) * BigInt(60) + BigInt(m[6] || 0);
			let total = secs * BigInt(1000000000) + nanos;
			if (neg)
				total = -total;
			let ms = total / BigInt(1000000);  // BigInt division truncates toward zero; Java's Duration.toMillis floors.
			if (total < BigInt(0) && total % BigInt(1000000) !== BigInt(0))
				ms -= BigInt(1);
			if (ms > LONG_MAX || ms < -LONG_MAX - BigInt(1))
				return Number.NaN;
			return Number(ms);
		}
		const secs = Number(m[2] || 0) * 86400 + Number(m[4] || 0) * 3600 + Number(m[5] || 0) * 60 + Number(m[6] || 0) + Number("0." + (m[7] || "0"));
		const n = Math.floor((neg ? -secs : secs) * 1000);
		return Number.isFinite(n) && Math.abs(n) <= 9.2e18 ? n : Number.NaN;
	}

	// The instant a relative-duration TIMESTAMP literal resolves against: captured ONCE per matches()/search() call
	// (mirror of QueryResolver's single clock read), so every literal in one query sees the identical instant.
	let activeRequestTime = null;

	function requestTimeMillis(rt) {
		if (Object.prototype.toString.call(rt) === "[object Date]")  // Cross-realm safe (iframes, vm sandboxes).
			return rt.getTime();
		return typeof rt === "number" && Number.isFinite(rt) ? rt : Date.now();
	}

	function withRequestTime(rt, fn) {
		const saved = activeRequestTime;
		activeRequestTime = requestTimeMillis(rt);
		try {
			return fn();
		} finally {
			activeRequestTime = saved;
		}
	}

	/** A NUMERIC argument's value: a relative-duration literal is plain signed milliseconds. */
	function argNum(arg) {
		const ms = typeof arg === "string" ? durationMillis(arg.trim()) : null;
		if (ms == null)
			return decimalVal(arg);
		return Number.isNaN(ms) ? null : ms;
	}

	/** Whether request time plus `ms` stays within a Java long of epoch milliseconds (Java: Math.addExact; else BAD_VALUE). */
	function tsDurationFits(ms) {
		const base = activeRequestTime ?? Date.now();
		if (LONG_MAX != null) {
			const sum = BigInt(Math.trunc(base)) + BigInt(ms);
			return sum <= LONG_MAX && sum >= -LONG_MAX - BigInt(1);
		}
		return Math.abs(base + ms) <= 9.2e18;
	}

	/** A TIMESTAMP argument's epoch millis: a relative-duration literal is request time plus the signed amount. */
	function argMs(arg) {
		const ms = typeof arg === "string" ? durationMillis(arg.trim()) : null;
		if (ms == null)
			return millisVal(arg);
		if (Number.isNaN(ms) || ! tsDurationFits(ms))
			return null;
		return (activeRequestTime ?? Date.now()) + ms;
	}

	/** Three-way numeric compare: -1, 0 or 1. */
	function compare3(a, b) {
		if (a < b) return -1;
		return a > b ? 1 : 0;
	}

	/** Ordered comparison of cell vs arg, or null when either side is not comparable for this type. */
	function cmp(cell, arg, type) {
		if (type === T_NUM) {
			const a = decimalVal(cell), b = argNum(arg);
			return (a == null || b == null) ? null : compare3(a, b);
		}
		if (type === T_TS) {
			const am = millisVal(cell), bm = argMs(arg);
			return (am == null || bm == null) ? null : compare3(am, bm);
		}
		if (type === T_VER)
			return cmpVersion(strVal(cell), arg);
		return null;  // text / id / enum / boolean are not ordered.
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Leaf comparisons (mirror of InMemoryMatch.java's eq/contains/prefix/regex/between).
	// -----------------------------------------------------------------------------------------------------------------

	/** Case-insensitive equals for TEXT/ID/ENUM; typed equals otherwise. Used for bare-token defaults and $eqic. */
	function eqCi(cell, arg, type) {
		if (type === T_TEXT || type === T_ID || type === T_ENUM) {
			const v = strVal(cell);
			return v != null && v.toLowerCase() === String(arg).toLowerCase();
		}
		return typedEquals(cell, arg, type);
	}

	/** Exact (case-sensitive) equals for TEXT/ID/ENUM; typed equals otherwise. Used for $eq/$ne/$in (D2). */
	function eqExact(cell, arg, type) {
		if (type === T_TEXT || type === T_ID || type === T_ENUM) {
			const v = strVal(cell);
			return v != null && v === String(arg);
		}
		return typedEquals(cell, arg, type);
	}

	/** The non-text-case-sensitivity branch shared identically by eqCi/eqExact: boolean / numeric / timestamp / version. */
	function typedEquals(cell, arg, type) {
		if (type === T_BOOL) {
			const a = boolVal(cell);
			return a != null && a === boolVal(arg);
		}
		const c = cmp(cell, arg, type);  // numeric / timestamp / version
		return c != null && c === 0;
	}

	/** True if `token` contains an unescaped wildcard character ("*" or "?"), per D1. */
	function hasWildcard(token) {
		return token.includes('*') || token.includes('?');
	}

	/**
	 * Whole-string glob match (D1): "*" matches any sequence of characters (including none); "?" matches exactly
	 * one character; every other character must match literally once both sides are case-folded.
	 *
	 * <p>
	 * This is the classic greedy two-pointer glob algorithm (mirror of Java's {@code InMemoryMatch.globMatches}),
	 * worst case {@code O(text.length * pattern.length)} with no backtracking blowup -- unlike a regex built from
	 * the same pattern (a run of {@code a*} segments can backtrack exponentially), this always terminates in
	 * bounded time regardless of input. A regex-based translation was tried first and rejected: it reproduces the
	 * exact catastrophic-backtracking hang the Java side's own doc comment warns against, reachable through the
	 * ordinary bare-pattern quick filter (no {@code $regex} needed).
	 */
	function globMatches(text, pattern) {
		let ti = 0, pi = 0, starIdx = -1, starTi = 0;
		while (ti < text.length) {
			if (pi < pattern.length && pattern.charAt(pi) === '*') {
				starIdx = pi++;
				starTi = ti;
			} else if (pi < pattern.length && (pattern.charAt(pi) === '?' || pattern.charAt(pi) === text.charAt(ti))) {
				pi++;
				ti++;
			} else if (starIdx >= 0) {
				pi = starIdx + 1;
				ti = ++starTi;
			} else {
				return false;
			}
		}
		while (pi < pattern.length && pattern.charAt(pi) === '*')
			pi++;
		return pi === pattern.length;
	}

	function wildcardMatches(cell, token) {
		if (cell == null)
			return false;
		return globMatches(String(cell).toLowerCase(), token.toLowerCase());
	}

	function contains(cell, arg) {
		const v = strVal(cell);
		return v != null && v.toLowerCase().includes(String(arg).toLowerCase());
	}

	function prefix(cell, arg, type) {
		if (type === T_VER)
			return versionPrefix(strVal(cell), arg);
		const v = strVal(cell);
		return v != null && v.toLowerCase().startsWith(String(arg).toLowerCase());
	}

	/** Translates the portable i/m/s flag chars into a JS RegExp flag string (design section 4.1). */
	function jsFlags(chars) {
		let f = "";
		for (const c of chars) {
			if ((c === "i" || c === "m" || c === "s") && ! f.includes(c))
				f += c;
		}
		return f;
	}

	/** The anchored RegExp a `$regex(pattern[, flags=...])` call evaluates, or null for a bad pattern. */
	function buildRegex(args) {
		let flags = "i";  // Default (case-insensitive) when no flags= given.
		for (let i = 1; i < args.length; i++) {
			const a = String(args[i]).trim();
			if (a.startsWith("flags="))
				flags = jsFlags(a.substring(6));
		}
		try {
			// Java Matcher.matches() is a full-string match; anchor the pattern to reproduce it.
			return new RegExp("^(?:" + args[0] + ")$", flags);
		} catch (e) {
			return null;  // Bad pattern matches nothing (PatternSyntaxException parity).
		}
	}

	function regex(cell, args) {
		const v = strVal(cell);
		if (v == null)
			return false;
		const re = buildRegex(args);
		return re != null && re.test(v);
	}

	/** `$regex` against a pattern `compile()` already built once (null = bad pattern, which matches nothing). */
	function matchCompiledRegex(re, cell) {
		const v = strVal(cell);
		return v != null && re != null && re.test(v);
	}

	function between(cell, args, type) {
		const lo = cmp(cell, args[0], type), hi = cmp(cell, args[1], type);
		return lo != null && hi != null && lo >= 0 && hi <= 0;
	}

	function matchLeaf(name, args, cell, type) {
		switch (name) {
			case "$eq":       return eqExact(cell, args[0], type);
			case "$eqic":     return eqCi(cell, args[0], type);
			case "$ne":       return isBlankCell(cell) || ! args.some(function (a) { return eqExact(cell, a, type); });
			case "$in":       return args.some(function (a) { return eqExact(cell, a, type); });
			case "$contains": return contains(cell, args[0]);
			case "$prefix":   return prefix(cell, args[0], type);
			case "$regex":    return regex(cell, args);
			case "$blank":    return isBlankCell(cell);
			case "$gt":       { const g = cmp(cell, args[0], type); return g != null && g > 0; }
			case "$gte":      { const ge = cmp(cell, args[0], type); return ge != null && ge >= 0; }
			case "$lt":       { const lt = cmp(cell, args[0], type); return lt != null && lt < 0; }
			case "$lte":      { const le = cmp(cell, args[0], type); return le != null && le <= 0; }
			default:          return between(cell, args, type);  // "$between"
		}
	}

	function matchBare(token, quoted, cell, type) {
		if (type === T_TEXT || type === T_ID) {
			if (! quoted && hasWildcard(token))
				return wildcardMatches(cell, token);
			return contains(cell, token);
		}
		if (type === T_ENUM) {
			if (! quoted && hasWildcard(token))
				return wildcardMatches(cell, token);
			return eqCi(cell, token, type);
		}
		if (type === T_VER)
			return prefix(cell, token, type);
		return eqCi(cell, token, type);  // numeric / timestamp / boolean
	}

	function matchCombinator(name, node, cell, type, customs) {
		if (name === "$and")
			return node.args.every(function (a) { return matchNode(a, cell, type, customs); });
		if (name === "$or")
			return node.args.some(function (a) { return matchNode(a, cell, type, customs); });
		return ! matchNode(node.args[0], cell, type, customs);  // $not - arity guaranteed 1.
	}

	function matchNode(node, cell, type, customs) {
		if (isLiteral(node))
			return matchBare(node.value, node.quoted, cell, type);
		// D6: lookup is case-insensitive; this parser is operator-agnostic (parse() takes no operator set, unlike
		// Java's parser), so canonicalization happens here, at evaluation time, not at parse time.
		const o = SearchOperators.get(node.name);
		if (o != null) {
			if (! o.acceptsArgCount(argCount(node)))
				return false;  // Malformed arity matches nothing rather than throwing.
			if (o.combinator)
				return matchCombinator(o.name, node, cell, type, customs);
			if (node.compiledRegex !== undefined)
				return matchCompiledRegex(node.compiledRegex, cell);
			return matchLeaf(o.name, literalArgs(node), cell, type);
		}
		const custom = customs[node.name];
		return custom != null && !! custom(cell, literalArgs(node));
	}

	/**
	 * Evaluates one already-parsed, valid expression against a single cell value - the client-mode grid's
	 * row-level predicate.  Blank/incomplete/invalid descriptors are treated as no constraint (match everything),
	 * exactly like InMemoryMatch.matches skips them.
	 *
	 * @param expr A descriptor from parse(), or a raw string (parsed here).
	 * @param cell The cell value.
	 * @param type The column SearchType wire token.
	 * @param customs Optional map of custom "$"-name -> function(cell, argStrings) -> boolean.
	 * @param requestTime Optional instant (epoch millis or Date) a relative-duration literal such as "-24h" on a
	 * 	timestamp column resolves against; defaults to the current time.
	 */
	function matches(expr, cell, type, customs, requestTime) {
		const d = (expr && typeof expr === "object" && "root" in expr) ? expr : parse(expr);
		if (d.empty || ! d.valid)
			return true;  // No constraint - never blanks the grid.
		return withRequestTime(requestTime, function () { return matchNode(d.root, cell, type, customs || {}); });
	}

	// -----------------------------------------------------------------------------------------------------------------
	// createEngine (mirror of InMemoryMatch.java): rows + per-column accessors + custom predicates -> search.
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an in-memory, client-side search engine mirroring InMemoryMatch.  Configure it with rows, a
	 * per-column accessor (how to read a cell value from a row), and any custom-operator predicates; then call
	 * search(terms) where each term is {column, type, expression}.  Multiple terms combine with `and`.
	 */
	function createEngine() {
		let rows = [];
		const accessors = {};
		const customs = {};
		let requestTime = null;
		const engine = {
			requestTime: function (value) {
				requestTime = value;
				return engine;
			},
			rows: function (value) {
				if (value == null)
					throw new TypeError("JuneauViews.search engine rows must not be null.");
				rows = value;
				return engine;
			},
			accessor: function (column, fn) {
				if (column == null || typeof fn !== "function")
					throw new TypeError("JuneauViews.search engine accessor requires a non-null column and function.");
				accessors[column] = fn;
				return engine;
			},
			custom: function (name, predicate) {
				if (name == null || typeof predicate !== "function")
					throw new TypeError("JuneauViews.search engine custom requires a non-null name and predicate.");
				customs[name] = predicate;
				return engine;
			},
			search: function (terms) {
				return withRequestTime(requestTime, function () { return searchTerms(terms); });
			}
		};

		function searchTerms(terms) {
			const active = [];
			for (const term of terms) {
				const d = parse(term.expression);
				if (d.empty || ! d.valid)
					continue;  // Blank, incomplete draft, or malformed -> no constraint.
				const accessor = accessors[term.column];
				if (accessor != null)
					active.push({ accessor: accessor, type: term.type, root: d.root });
			}
			const out = [];
			for (const row of rows) {
				if (matchesAll(active, row))
					out.push(row);
			}
			return out;
		}

		function matchesAll(active, row) {
			for (const p of active) {
				if (! matchNode(p.root, p.accessor(row), p.type, customs))
					return false;
			}
			return true;
		}

		return engine;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// parseExprStrict (mirror of SearchExpressionParser's throwing entry point): coded-error parity with Java's
	// BeanQuerySyntaxException.Code, for callers that want a thrown error instead of parse()'s non-throwing
	// descriptor.  Reuses parseFunc's scanArgs so both grammars stay byte-for-byte identical.
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Error codes mirror `BeanQuerySyntaxException.Code` (Java side) by name, so a caller can branch on `err.code`
	 * the same way on both engines. Only the codes this parser can itself raise are used here; pipeline-level
	 * codes (TOO_MANY_CLAUSES, UNKNOWN_COLUMN, REGEX_DISABLED, OPERATOR_TYPE, BAD_VALUE, …) come from the
	 * server-side resolver, which this file does not implement.
	 */
	function searchError(code, message) {
		const e = new Error(message);
		e.code = code;
		return e;
	}

	function regexHint(message, opName) {
		return opName === '$regex' ? message + ' Quote regex patterns that contain commas or parentheses: $regex("…").' : message;
	}

	/**
	 * Walks an already-parsed tree checking operator existence and arity (both of which `parse()`/`parseFunc`
	 * deliberately skip, since this parser is operator-agnostic - see the D6 comment on `matchNode`). `parse()`'s
	 * `valid` flag only means "syntactically complete", so a syntactically well-formed call to an unknown
	 * operator (`$nope(1)`) or a known operator with the wrong arg count (`$eq(1,2)`) is still `valid` and would
	 * otherwise slip past `parseExprStrict` unchecked; this closes that gap for the strict, throwing entry point.
	 *
	 * <p>
	 * Custom operators: with no `customs` table this consults only the static builtin {@link SearchOperators}
	 * catalog, so a custom "$name" throws `UNKNOWN_OPERATOR`.  Pass a table (as `compile()` does from the
	 * `registerCustom` registry) to accept the registered customs; no arity metadata exists for one on the client,
	 * so its argument count is not checked.
	 */
	function validateStrict(node, customs) {
		if (isLiteral(node))
			return;
		const o = SearchOperators.get(node.name);
		if (o == null) {
			const custom = lookupCustom(customs, node.name);
			if (custom == null)
				throw searchError('UNKNOWN_OPERATOR', 'Unknown search operator: \'' + node.name + '\'.');
			// A caller-registered custom operator: no arity metadata exists for it on the client, so any arg count
			// is accepted (its predicate decides); the name is canonicalized to the registered spelling.
			node.name = custom.name;
			for (const arg of node.args)
				validateStrict(arg, customs);
			return;
		}
		if (! o.acceptsArgCount(argCount(node)))
			throw searchError('BAD_ARG_COUNT', regexHint('Operator \'' + o.name + '\' does not accept ' + argCount(node) + ' argument(s).', o.name));
		// Java's SearchExpression.func() always stores the operator's canonical registered name (SearchOperatorSet:
		// "get(name) always exposes the operator's own canonical name, never the casing a caller looked it up
		// with"); parse()'s live-typing path preserves the raw-cased name since op resolution is deliberately
		// deferred there (D6), so parseExprStrict - the Java-parity, throwing entry point - canonicalizes it here.
		node.name = o.name;
		for (const arg of node.args)
			validateStrict(arg, customs);
	}

	/** The `{name, fn}` entry `customs` (a lowercase-"$name"-keyed table) holds for `name`, or null. */
	function lookupCustom(customs, name) {
		if (customs == null || name == null)
			return null;
		const key = String(name).toLowerCase();
		return Object.hasOwn(customs, key) ? customs[key] : null;
	}

	/**
	 * Parses one column's search expression the same way `parse()` does, but throws a coded `Error` (see
	 * `searchError`) instead of returning a descriptor — for callers that want Java-style error identity rather
	 * than the live-typing-friendly incomplete/invalid flags `parse()` returns. This parses a single `expression`
	 * string (today's whole grammar); it is not a multi-column parser.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *	<jk>try</jk> {
	 *		<jv>tree</jv> = JuneauViews.search.parseExprStrict(<jv>raw</jv>);
	 *	} <jk>catch</jk> (<jv>e</jv>) {
	 *		<jk>if</jk> (<jv>e</jv>.code === <js>'UNKNOWN_OPERATOR'</js>) { ... }
	 *	}
	 * </p>
	 */
	function parseExprStrict(raw, customs) {
		const d = parse(raw);
		if (d.empty)
			return literalNode('', false);
		if (d.valid) {
			// A bare literal (non-function-call) token is always "valid" per parse()'s lenient parseExpr, which
			// never checks quote termination for a bare token (that check is strict-path-only - mirrors Java's
			// SearchExpressionParser.parseExpr, which checks isUnterminatedQuote(s) before building a literal
			// node). Re-check it here, on the exact raw token this level parsed, before accepting the literal.
			if (isLiteral(d.root)) {
				const token = String(raw).trim();
				if (isUnterminatedQuote(token))
					throw searchError('UNTERMINATED_QUOTE', 'Unterminated quoted value in search expression: \'' + token + '\'.');
			}
			validateStrict(d.root, customs);  // Catches unknown-operator/bad-arg-count calls parse() left as "valid".
			return d.root;
		}
		// Re-walk with parseFuncStrict directly so a definite (non-live-typing) failure gets a real code instead
		// of the permissive INCOMPLETE/INVALID signal parse() swallows for the live-typing UI path.
		return parseFuncStrict(String(raw).trim(), customs);
	}

	/**
	 * `parseFunc`'s grammar, re-entered so every failure throws a coded Error instead of an internal signal.
	 * Mirrors Java's `SearchExpressionParser` throw sites one for one (MALFORMED_OPERATOR, INVALID_OPERATOR_NAME,
	 * UNKNOWN_OPERATOR, UNTERMINATED_QUOTE, UNBALANCED, EMPTY_ARGUMENT, TRAILING_TEXT, BAD_ARG_COUNT).
	 */
	function parseFuncStrict(s, customs) {
		if (s.length > 1 && s.charAt(0) === '$' && IS_LETTER.test(s.charAt(1))) {
			const open = s.indexOf('(');
			if (open < 0)
				throw searchError('MALFORMED_OPERATOR', 'Malformed search expression (operator name without arguments): \'' + s + '\'.');
			const name = s.substring(0, open);
			if (! isValidName(name))
				throw searchError('INVALID_OPERATOR_NAME', 'Invalid operator name: \'' + name + '\'.');
			const o = SearchOperators.get(name);
			const custom = o == null ? lookupCustom(customs, name) : null;
			if (o == null && custom == null)
				throw searchError('UNKNOWN_OPERATOR', 'Unknown search operator: \'' + name + '\'.');
			const opName = o != null ? o.name : custom.name;
			const info = scanArgs(s, open);
			// blankArg is checked first: Java's parseFunc throws EMPTY_ARGUMENT inline, the instant a top-level
			// comma (or the closing paren) finds a blank slot behind it - before it can ever notice the rest of
			// the string is *also* unterminated/unbalanced. scanArgs records the same fact inline (at the comma),
			// so this must win over the other two codes exactly like it does in Java, even though scanArgs
			// (unlike Java's addPart) can't throw until the whole scan finishes.
			if (info.blankArg)
				throw searchError('EMPTY_ARGUMENT', 'Empty argument (stray or trailing comma) in search expression: \'' + s + '\'.');
			if (info.state === 'unterminatedQuote')
				throw searchError('UNTERMINATED_QUOTE', 'Unterminated quoted value in search expression: \'' + s + '\'.');
			if (info.close < 0)
				throw searchError('UNBALANCED', regexHint('Unclosed parenthesis in search expression: \'' + s + '\'.', opName));
			if (s.substring(info.close + 1).length > 0)
				throw searchError('TRAILING_TEXT', 'Unexpected text after \')\' in search expression: \'' + s + '\'.');
			const argNodes = info.rawArgs.map(function (a) { return parseExprStrict(a.trim(), customs); });
			if (o != null && ! o.acceptsArgCount(argNodes.length))
				throw searchError('BAD_ARG_COUNT', regexHint('Operator \'' + o.name + '\' does not accept ' + argNodes.length + ' argument(s).', o.name));
			return funcNode(opName, argNodes);
		}
		// Bare-literal fallthrough. In the current dispatch this is only reached via parseExprStrict's direct
		// `parseFuncStrict(raw.trim())` call, which is itself only taken when the token starts with "$" + a
		// letter (see the `if` above) - so a non-"$name(..." bare literal never actually lands here today
		// (parseExprStrict's `d.valid` branch, which handles every other bare literal, carries the equivalent
		// isUnterminatedQuote check itself). Checked here too, for parity with Java's SearchExpressionParser
		// (which performs this check at every bare-literal site) and in case a future dispatch change ever
		// routes a bare literal through this path.
		if (isUnterminatedQuote(s))
			throw searchError('UNTERMINATED_QUOTE', 'Unterminated quoted value in search expression: \'' + s + '\'.');
		return literalNode(decodeLiteral(s), isQuotedToken(s));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Typed-argument validation (mirror of SearchType.parse(String) / ValueParse.java + QueryResolver's
	// checkTypes/typeTree): resolveStrict raises OPERATOR_TYPE, then BAD_VALUE, exactly where the Java resolver does.
	// -----------------------------------------------------------------------------------------------------------------

	// Mirrors ValueParse.MAX_SCALE / MAX_PRECISION (guard against absurd BigDecimal exponents / digit counts).
	const MAX_SCALE = 1000, MAX_PRECISION = 1000;

	// Java String.strip() trims Character.isWhitespace() characters (not NBSP, unlike JS trim()).
	const JAVA_WS = "\t\n\u000B\f\r\u001C-\u001F \u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000";
	const JAVA_WS_CHAR_RE = new RegExp("[" + JAVA_WS + "]");

	// Linear-time strip (a leading/trailing-run regex backtracks quadratically on long whitespace runs).
	function javaStrip(s) {
		let start = 0, end = s.length;
		while (start < end && JAVA_WS_CHAR_RE.test(s.charAt(start)))
			start++;
		while (end > start && JAVA_WS_CHAR_RE.test(s.charAt(end - 1)))
			end--;
		return s.substring(start, end);
	}

	const BIGDEC_RE = /^[+-]?(?:(\d+)(?:\.(\d*))?|\.(\d+))(?:[eE]([+-]?\d+))?$/;

	/** Whether `v` (already stripped) is accepted by `new BigDecimal(v)` within the scale/precision limits. */
	function isNumericValue(v) {
		const m = BIGDEC_RE.exec(v);
		if (m == null)
			return false;
		const intPart = m[1] ?? "";
		const frac = m[2] ?? m[3] ?? "";
		const exp = m[4] != null ? Number(m[4]) : 0;
		const digits = (intPart + frac).replace(/^0+/, "");
		const precision = Math.max(1, digits.length);
		const scale = frac.length - exp;
		return Number.isFinite(scale) && Math.abs(scale) <= MAX_SCALE && precision <= MAX_PRECISION;
	}

	function daysInMonth(y, mo) {
		if (mo === 2)
			return (y % 4 === 0 && (y % 100 !== 0 || y % 400 === 0)) ? 29 : 28;
		return (mo === 4 || mo === 6 || mo === 9 || mo === 11) ? 30 : 31;
	}

	function isLocalDate(y, mo, d) {
		return mo >= 1 && mo <= 12 && d >= 1 && d <= daysInMonth(y, mo);
	}

	const LOCAL_DATE_RE = /^(\d{4})-(\d\d)-(\d\d)$/;
	const LOCAL_DATETIME_RE = /^(\d{4})-(\d\d)-(\d\d)T(\d\d):(\d\d)(?::(\d\d)(?:\.\d{1,9})?)?(Z|[+-]\d\d:\d\d(?::\d\d)?)?$/;
	const LONG_RE = /^-?\d+$/;

	function isOffset(o) {
		if (o === "Z")
			return true;
		const h = Number.parseInt(o.substring(1, 3), 10), mi = Number.parseInt(o.substring(4, 6), 10), se = o.length > 6 ? Number.parseInt(o.substring(7, 9), 10) : 0;
		return mi <= 59 && se <= 59 && (h * 3600 + mi * 60 + se) <= 18 * 3600;
	}

	function isLong(v) {
		if (! LONG_RE.test(v))
			return false;
		if (typeof BigInt !== "function")
			return v.replace(/^-/, "").replace(/^0+/, "").length <= 18;
		const b = BigInt(v);
		return b >= BigInt("-9223372036854775808") && b <= BigInt("9223372036854775807");
	}

	/** Whether `v` (already stripped) is an ISO offset/local date-time, local date, or epoch milliseconds (optional '-', no '+'). */
	function isTimestampValue(v) {
		let m = LOCAL_DATETIME_RE.exec(v);
		if (m != null)
			return isLocalDate(Number.parseInt(m[1], 10), Number.parseInt(m[2], 10), Number.parseInt(m[3], 10))
				&& Number.parseInt(m[4], 10) <= 23 && Number.parseInt(m[5], 10) <= 59 && (m[6] == null || Number.parseInt(m[6], 10) <= 59)
				&& (m[7] == null || isOffset(m[7]));
		m = LOCAL_DATE_RE.exec(v);
		if (m != null)
			return isLocalDate(Number.parseInt(m[1], 10), Number.parseInt(m[2], 10), Number.parseInt(m[3], 10));
		return isLong(v);
	}

	/**
	 * Whether `text` parses for `type` (SearchType wire token), mirroring SearchType.parse(String) in Java: numeric,
	 * boolean and timestamp are validated; every other type always parses.  A relative-duration literal
	 * ("[+-]<n>(ms|s|m|h|d)", e.g. "-24h", or a signed ISO-8601 duration such as "-PT24H" / "P1D") is valid for numeric (signed milliseconds) and timestamp (request time
	 * plus the amount) columns only.
	 */
	function isValidValue(type, text) {
		const v = javaStrip(text == null ? "" : String(text));
		if (type === T_NUM || type === T_TS) {
			const ms = durationMillis(v);  // Relative duration: valid unless its millisecond total overflows a long.
			if (ms != null)
				return ! Number.isNaN(ms) && (type === T_NUM || tsDurationFits(ms));
			return type === T_NUM ? isNumericValue(v) : isTimestampValue(v);
		}
		if (type === T_BOOL)
			return v.toLowerCase() === "true" || v.toLowerCase() === "false";
		return true;
	}

	const VALUE_OPS = { "$eq": 1, "$ne": 1, "$in": 1, "$gt": 1, "$gte": 1, "$lt": 1, "$lte": 1, "$between": 1 };

	function checkTypes(node, type, column) {
		if (isLiteral(node))
			return;
		const o = SearchOperators.get(node.name);
		if (o != null && ! o.appliesTo(type))
			throw searchError('OPERATOR_TYPE', 'Operator ' + o.name + ' does not apply to ' + type + ' column \'' + column + '\'.');
		node.args.forEach(function (a) { checkTypes(a, type, column); });
	}

	function checkValue(literal, type, column) {
		if (! isValidValue(type, literal.value))
			throw searchError('BAD_VALUE', 'Value \'' + literal.value + '\' is not a valid ' + type + ' for column \'' + column + '\'.');
	}

	function checkValues(node, type, column) {
		if (isLiteral(node)) {
			checkValue(node, type, column);
			return;
		}
		const o = SearchOperators.get(node.name);
		if (o != null && o.combinator) {
			node.args.forEach(function (a) { checkValues(a, type, column); });  // Bare literals under a combinator are validated too (as in Java typeTree).
			return;
		}
		if (o != null && VALUE_OPS[o.name] === 1)
			node.args.forEach(function (a) { if (isLiteral(a)) checkValue(a, type, column); });
	}

	/**
	 * Parses one column's search expression like `parseExprStrict`, then applies the resolver's typed checks against
	 * the column's `type` (SearchType wire token, e.g. "numeric"): first OPERATOR_TYPE (an operator that does not apply
	 * to the type), then BAD_VALUE (a bare literal (including one directly inside $and/$or/$not) or a value-operator argument -- $eq/$ne/$in/$gt/$gte/$lt/$lte/$between --
	 * that does not parse as the column's numeric/boolean/timestamp type).  Returns the parsed tree.  Unparseable
	 * *cells* are unaffected: they still simply fail to match.  The lenient `parse()`/`matches()`/`createEngine()`
	 * live-typing path never throws.
	 *
	 * @param raw The column's expression (the text after "col=").
	 * @param type The column SearchType wire token.
	 * @param column Optional column name used in error messages.
	 * @param customs Optional custom-operator table (see `registerCustom`); a custom "$name" found there is accepted
	 * 	instead of raising UNKNOWN_OPERATOR.  Omitted, only the builtin catalog is consulted.
	 */
	function resolveStrict(raw, type, column, customs) {
		const tree = parseExprStrict(raw, customs);
		const t = SearchType.fromWire(type);
		if (t != null && ! (isLiteral(tree) && tree.value === '' && String(raw).trim() === '')) {
			checkTypes(tree, t, column ?? 'v');
			checkValues(tree, t, column ?? 'v');
		}
		return tree;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Client-side custom-operator registry + compile(): the client-mode grid's row predicate (WORK-J0612).
	// -----------------------------------------------------------------------------------------------------------------

	// Mirrors QuerySettings.MAX_REGEX_LENGTH: a longer $regex pattern is REGEX_TOO_LONG, exactly as the server rejects it.
	const MAX_REGEX_LENGTH = 256;

	// Page-level custom operators: lowercase "$name" -> {name, fn}.  Lookup is case-insensitive (D6), like builtins.
	const CUSTOM_REGISTRY = {};

	/**
	 * Registers a page-level predicate for a custom operator the server advertises in a column's
	 * `search.operators` (a `custom:true` entry built from `CardEnvelope.addSearchMeta`'s `customOperators`), so a
	 * client-filtered grid can evaluate it.  The predicate receives the raw cell value and the operator's literal
	 * argument strings, and returns whether the cell matches - the same `(cell, args)` contract as the Java
	 * `SearchOperator.predicate`.  A custom operator with no registered predicate is rejected on a client-filtered
	 * table rather than silently matching nothing.  Re-registering a name replaces its predicate.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *	JuneauViews.search.registerCustom(<js>'$startsCI'</js>, <jk>function</jk> (<jv>cell</jv>, <jv>args</jv>) {
	 *		<jk>return</jk> String(<jv>cell</jv> ?? <js>''</js>).toLowerCase().startsWith(String(<jv>args</jv>[0]).toLowerCase());
	 *	});
	 * </p>
	 *
	 * @param name The operator's "$"-name, e.g. "$startsCI" (must not shadow a builtin).
	 * @param fn The predicate `function (cell, args) -> boolean`.
	 */
	function registerCustom(name, fn) {
		if (name == null || typeof fn !== "function")
			throw new TypeError("JuneauViews.search.registerCustom requires a \"$\"-name and a predicate function.");
		const n = String(name).trim();
		if (n.charAt(0) !== "$" || ! isValidName(n) || ! IS_LETTER.test(n.charAt(1)))
			throw new TypeError("JuneauViews.search.registerCustom: invalid operator name '" + n + "'.");
		if (SearchOperators.isBuiltin(n))
			throw new TypeError("JuneauViews.search.registerCustom: '" + n + "' is a builtin operator.");
		CUSTOM_REGISTRY[n.toLowerCase()] = { name: n, fn: fn };
	}

	/** Whether a predicate is registered (via `registerCustom`) for the custom operator `name` (case-insensitive). */
	function hasCustom(name) {
		return lookupCustom(CUSTOM_REGISTRY, name) != null;
	}

	/** The registry merged with an optional `{"$name": fn}` map of call-local predicates (those win), lowercase-keyed. */
	function customTable(extra) {
		const out = {};
		for (const k of Object.keys(CUSTOM_REGISTRY))
			out[k] = CUSTOM_REGISTRY[k];
		if (extra != null)
			for (const k of Object.keys(extra))
				if (typeof extra[k] === "function")
					out[k.toLowerCase()] = { name: k, fn: extra[k] };
		return out;
	}

	/** Mirror of QueryResolver.checkRegex: REGEX_DISABLED when not allowed, REGEX_TOO_LONG past MAX_REGEX_LENGTH. */
	function checkRegexStrict(node, allowRegex) {
		if (isLiteral(node))
			return;
		if (node.name === "$regex") {
			if (! allowRegex)
				throw searchError('REGEX_DISABLED', 'Regex search is not enabled for this list.');
			if (literalArgs(node)[0].length > MAX_REGEX_LENGTH)
				throw searchError('REGEX_TOO_LONG', 'Regex pattern exceeds ' + MAX_REGEX_LENGTH + ' characters.');
		}
		for (const arg of node.args)
			checkRegexStrict(arg, allowRegex);
	}

	/** Builds each `$regex` node's RegExp once (S8/N3), so a draw never constructs one per row. */
	function precompileRegex(node) {
		if (isLiteral(node))
			return;
		if (node.name === "$regex")
			node.compiledRegex = buildRegex(literalArgs(node));
		for (const arg of node.args)
			precompileRegex(arg);
	}

	/**
	 * Validates one column expression with the server's rules and compiles it into a reusable row predicate - the
	 * client-filtered grid's single entry point (WORK-J0612).  Parsing, strict validation (`resolveStrict`: syntax,
	 * unknown operator, arity, OPERATOR_TYPE, BAD_VALUE, plus the server's regex checks) and `$regex` compilation
	 * all happen ONCE here; `test(cell, nowMs)` then evaluates the cached tree per row with no re-parse.
	 *
	 * <p>
	 * Returns `{ok, empty, incomplete, error, raw, test}`:
	 * <ul>
	 * 	<li>blank input - `ok:true, empty:true`, and `test` matches every row;
	 * 	<li>a still-being-typed draft (`$eq(`) - `ok:false, incomplete:true, error:null`;
	 * 	<li>an expression the server would reject - `ok:false`, `error` a coded Error (`error.code` is the Java
	 * 		`BeanQuerySyntaxException.Code` name; `error.message` mirrors the server's `X-BeanQuery-Error` text);
	 * 	<li>otherwise `ok:true` and `test(cell, nowMs)`, where `nowMs` anchors relative durations such as `-24h`.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *	<jk>const</jk> <jv>c</jv> = JuneauViews.search.compile(<js>'$in(Triaged,New)'</js>, <js>'enum'</js>, { column: <js>'status'</js> });
	 *	<jk>if</jk> (<jv>c</jv>.ok) <jv>rows</jv> = <jv>rows</jv>.filter(<jk>function</jk> (<jv>r</jv>) { <jk>return</jk> <jv>c</jv>.test(<jv>r</jv>.status, Date.now()); });
	 * </p>
	 *
	 * @param raw The column's expression.
	 * @param type The column SearchType wire token (an unknown type is rejected with UNKNOWN_TYPE).
	 * @param options Optional `{column, allowRegex, customs}`: the column name for error messages; whether
	 * 	`$regex` is enabled (default true); call-local `{"$name": fn}` custom predicates merged over the registry.
	 */
	function compile(raw, type, options) {
		const o = options || {};
		const text = raw == null ? "" : String(raw).trim();
		const d = parse(text);
		if (d.empty)
			return compiled(true, true, false, null, "", function () { return true; });
		if (d.incomplete)
			return compiled(false, false, true, null, text, null);
		const t = SearchType.fromWire(type);
		if (t == null)
			return compiled(false, false, false, searchError('UNKNOWN_TYPE', 'Unknown search type \'' + type + '\'.'), text, null);
		const customs = customTable(o.customs);
		let tree;
		try {
			tree = resolveStrict(text, t, o.column, customs);
			checkRegexStrict(tree, o.allowRegex !== false);
		} catch (e) {
			if (e?.code)
				return compiled(false, false, false, e, text, null);
			throw e;
		}
		precompileRegex(tree);
		const fns = {};
		for (const k of Object.keys(customs))
			fns[customs[k].name] = customs[k].fn;
		return compiled(true, false, false, null, text, function (cell, nowMs) {
			return withRequestTime(nowMs, function () { return matchNode(tree, cell, t, fns); });
		});
	}

	function compiled(ok, empty, incomplete, error, raw, test) {
		return { ok: ok, empty: empty, incomplete: incomplete, error: error, raw: raw, test: test };
	}

	NS.search = {
		SearchType: SearchType,
		SearchOperators: SearchOperators,
		parse: parse,
		matches: matches,
		createEngine: createEngine,
		parseExprStrict: parseExprStrict,
		resolveStrict: resolveStrict,
		isValidValue: isValidValue,
		compile: compile,
		registerCustom: registerCustom,
		hasCustom: hasCustom,
		MAX_REGEX_LENGTH: MAX_REGEX_LENGTH
	};
})();
