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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * The in-memory evaluation of a parsed {@link SearchExpression} against a cell value, plus the type-aware value
 * coercion and ordered comparison the in-memory context also uses for sorting.
 *
 * <p>
 * This holds the leaf semantics of the {@code $}-language (design §5.1) for the built-in operators; a custom operator
 * is evaluated through its {@link SearchOperator#predicate() predicate}.  It is the behavior reference the
 * {@code juneau-search.js} client engine mirrors.
 */
final class InMemoryMatch {

	private InMemoryMatch() {}

	// -----------------------------------------------------------------------------------------------------------------
	// Evaluation
	// -----------------------------------------------------------------------------------------------------------------

	static boolean matches(SearchExpression node, Object cell, SearchType type, RegexBudget budget) {
		if (node.isLiteral())
			return matchBare(node.typedValue(), node.isQuoted(), cell, type);
		var op = node.operator();
		if (op.isCombinator())
			return matchCombinator(node, cell, type, budget);
		if (op.isCustom()) {
			var p = op.predicate();
			if (p == null)
				throw iaex("Custom operator '%s' has no in-memory predicate; it cannot be evaluated in memory.", op.name());
			return p.test(cell, node.literalArgs());
		}
		return matchLeaf(node, cell, type, budget);
	}

	/**
	 * Convenience overload for callers that don't already have a {@link RegexBudget} for the call (notably tests):
	 * builds one with the default {@code $regex} timeout.  Production row-matching goes through
	 * {@link #matches(SearchExpression, Object, SearchType, RegexBudget)} via {@link FilterEvaluator}, reusing one
	 * budget across every row/column in the call.
	 */
	static boolean matches(SearchExpression node, Object cell, SearchType type) {
		return matches(node, cell, type, new RegexBudget(new QuerySettings().regexTimeout));
	}

	private static boolean matchCombinator(SearchExpression node, Object cell, SearchType type, RegexBudget budget) {
		var name = node.name();
		if ("$and".equals(name))
			return node.args().stream().allMatch(a -> matches(a, cell, type, budget));
		if ("$or".equals(name))
			return node.args().stream().anyMatch(a -> matches(a, cell, type, budget));
		return ! matches(node.args().get(0), cell, type, budget);  // $not — arity guaranteed 1 by the parser.
	}

	private static boolean matchLeaf(SearchExpression node, Object cell, SearchType type, RegexBudget budget) {
		var args = node.typedArgs();  // Value operators read typed values; text operators get the original strings (never typed).
		switch (node.operator().name()) {
			case "$eq":       return eqExact(cell, args.get(0), type);
			case "$eqic":     return eqCi(cell, args.get(0), type);
			case "$ne":       return isBlankCell(cell) || args.stream().noneMatch(a -> eqExact(cell, a, type));
			case "$in":       return args.stream().anyMatch(a -> eqExact(cell, a, type));
			case "$contains": return contains(cell, str(args.get(0)));
			case "$prefix":   return prefix(cell, str(args.get(0)), type);
			case "$regex":    { var v = str(cell); return v != null && budget.matches(node, v); }
			case "$blank":    return isBlankCell(cell);
			case "$gt":       { var c = cmp(cell, args.get(0), type); return c != null && c > 0; }
			case "$gte":      { var c = cmp(cell, args.get(0), type); return c != null && c >= 0; }
			case "$lt":       { var c = cmp(cell, args.get(0), type); return c != null && c < 0; }
			case "$lte":      { var c = cmp(cell, args.get(0), type); return c != null && c <= 0; }
			case "$between":  return between(cell, args, type);
			// SearchOperator.Builder#build() now rejects renaming a copy() of a built-in, so that route can no
			// longer reach this switch under an unrecognized name. The only way left to construct a custom=false
			// operator this switch doesn't recognize is the package-private SearchOperator#builtin(...) factory
			// used directly (see InMemoryMatch_Test's i01/i02) - fail clearly rather than silently misinterpreting
			// it as $between.
			default:          throw iaex("Unsupported operator '%s' for in-memory evaluation.", node.operator().name());
		}
	}

	/** A bare pattern token.  Quoted bare literals never get wildcard interpretation (§3 "Quoting rules"). */
	private static boolean matchBare(Object value, boolean quoted, Object cell, SearchType type) {
		var token = str(value);
		switch (type) {
			case TEXT:
			case ID:      return (quoted || ! hasWildcard(token)) ? contains(cell, token) : wildcardMatches(cell, token);
			case ENUM:    return (quoted || ! hasWildcard(token)) ? eqCi(cell, token, type) : wildcardMatches(cell, token);
			case VERSION: return prefix(cell, token, type);
			default:      return eqCi(cell, value, type);  // numeric / timestamp / boolean — not wildcards, quoting is a no-op.
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Leaf comparisons
	// -----------------------------------------------------------------------------------------------------------------

	/** Case-insensitive typed equality (bare-token default and {@code $eqic}). */
	private static boolean eqCi(Object cell, Object arg, SearchType type) {
		switch (type) {
			case TEXT:
			case ID:
			case ENUM:    { var v = str(cell); return v != null && eqic(v, str(arg)); }
			case BOOLEAN:  return boolEquals(cell, arg);
			default:      { var c = cmp(cell, arg, type); return c != null && c == 0; }  // numeric / timestamp / version
		}
	}

	/** Exact (case-sensitive) typed equality ({@code $eq} / {@code $ne} / {@code $in}, D2). */
	private static boolean eqExact(Object cell, Object arg, SearchType type) {
		switch (type) {
			case TEXT:
			case ID:
			case ENUM:    { var v = str(cell); return v != null && eq(v, str(arg)); }
			case BOOLEAN:  return boolEquals(cell, arg);
			default:      { var c = cmp(cell, arg, type); return c != null && c == 0; }  // numeric / timestamp / version
		}
	}

	private static boolean contains(Object cell, String arg) {
		var v = str(cell);
		return v != null && v.toLowerCase(Locale.ROOT).contains(arg.toLowerCase(Locale.ROOT));
	}

	private static boolean hasWildcard(String token) {
		return token.indexOf('*') >= 0 || token.indexOf('?') >= 0;
	}

	/** A bare text/id/enum token with a wildcard: a case-insensitive whole-string glob match. */
	private static boolean wildcardMatches(Object cell, String token) {
		var v = str(cell);
		if (v == null)
			return false;
		return globMatches(v.toLowerCase(Locale.ROOT), token.toLowerCase(Locale.ROOT));
	}

	/**
	 * Whole-string glob match (D1): {@code *} matches any sequence of characters (including none); {@code ?} matches
	 * exactly one character; every other character must match literally.  Case sensitivity is the caller's
	 * responsibility (fold both arguments first).
	 *
	 * <p>
	 * This is the classic greedy two-pointer glob algorithm, worst case {@code O(text.length() * pattern.length())}
	 * with no backtracking blowup — unlike a regex built from the same pattern (a run of {@code a*} segments can
	 * backtrack exponentially via {@link Pattern}), this always terminates in bounded time regardless of input.
	 */
	private static boolean globMatches(String text, String pattern) {
		var ti = 0;
		var pi = 0;
		var starIdx = -1;
		var starTi = 0;
		while (ti < text.length()) {
			if (pi < pattern.length() && pattern.charAt(pi) == '*') {
				starIdx = pi++;
				starTi = ti;
			} else if (pi < pattern.length() && (pattern.charAt(pi) == '?' || pattern.charAt(pi) == text.charAt(ti))) {
				pi++;
				ti++;
			} else if (starIdx >= 0) {
				pi = starIdx + 1;
				ti = ++starTi;
			} else {
				return false;
			}
		}
		while (pi < pattern.length() && pattern.charAt(pi) == '*')
			pi++;
		return pi == pattern.length();
	}

	private static boolean prefix(Object cell, String arg, SearchType type) {
		if (type == SearchType.VERSION)
			return versionPrefix(str(cell), arg);
		var v = str(cell);
		return v != null && v.toLowerCase(Locale.ROOT).startsWith(arg.toLowerCase(Locale.ROOT));
	}

	private static boolean between(Object cell, List<Object> args, SearchType type) {
		var lo = cmp(cell, args.get(0), type);
		var hi = cmp(cell, args.get(1), type);
		return lo != null && hi != null && lo >= 0 && hi <= 0;
	}

	/** Ordered comparison of {@code cell} vs {@code arg}, or <jk>null</jk> when either side is not comparable. */
	private static Integer cmp(Object cell, Object arg, SearchType type) {
		switch (type) {
			case NUMERIC: {
				var a = decimal(cell);
				var b = decimal(arg);
				return (a == null || b == null) ? null : a.compareTo(b);
			}
			case TIMESTAMP: {
				var a = millis(cell);
				var b = millis(arg);
				return (a == null || b == null) ? null : Long.compare(a, b);
			}
			case VERSION:
				return cmpVersion(str(cell), str(arg));
			default:
				return null;  // text / id / enum / boolean are not ordered.
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Sorting
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Compares two cell values for sorting under a value type: numeric/timestamp/version compare by their coerced
	 * value, everything else by case-insensitive string.  Nulls sort last.
	 *
	 * <p>
	 * For the coercible types (NUMERIC/TIMESTAMP/VERSION), a cell that fails to coerce is never compared as a raw
	 * string against one that does — doing so can contradict the value-based order and break the comparator's
	 * transitivity contract.  Instead, a non-coercible side unconditionally sorts <em>after</em> every
	 * coercible value for that type; the case-insensitive string fallback below is reached only when <em>both</em>
	 * sides fail to coerce (comparing two already-opaque strings is always safe, since there is no coerced value on
	 * either side for a lexical order to contradict).
	 */
	static int compareCells(Object a, Object b, SearchType type) {
		if (a == null && b == null)
			return 0;
		if (a == null)
			return 1;
		if (b == null)
			return -1;
		switch (type) {
			case NUMERIC: {
				var x = decimal(a);
				var y = decimal(b);
				if (x != null && y != null)
					return x.compareTo(y);
				if (x != null || y != null)  // Exactly one coerces: it sorts before the non-coercible side.
					return x != null ? -1 : 1;
				break;
			}
			case TIMESTAMP: {
				var x = millis(a);
				var y = millis(b);
				if (x != null && y != null)
					return Long.compare(x, y);
				if (x != null || y != null)  // Exactly one coerces: it sorts before the non-coercible side.
					return x != null ? -1 : 1;
				break;
			}
			case VERSION: {
				var x = version(str(a));
				var y = version(str(b));
				if (x != null && y != null)
					return cmpVersionParts(x, y);
				if (x != null || y != null)  // Exactly one coerces: it sorts before the non-coercible side.
					return x != null ? -1 : 1;
				break;
			}
			default:
				break;
		}
		return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Value coercion
	// -----------------------------------------------------------------------------------------------------------------

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private static boolean isBlankCell(Object cell) {
		var v = str(cell);
		return ib(v);
	}

	/** Both sides coerce to a boolean and are equal; a side that is not a boolean never matches. */
	private static boolean boolEquals(Object cell, Object arg) {
		var a = bool(cell);
		return a.isPresent() && eq(a, bool(arg));
	}

	private static Optional<Boolean> bool(Object o) {
		if (o instanceof Boolean b)
			return Optional.of(b);
		var v = str(o);
		if (v == null)
			return Optional.empty();
		v = v.strip();
		if (eqic(v, "true"))
			return Optional.of(Boolean.TRUE);
		if (eqic(v, "false"))
			return Optional.of(Boolean.FALSE);
		return Optional.empty();
	}

	private static BigDecimal decimal(Object o) {
		if (o instanceof Number n) {
			if (n instanceof Double d && (d.isNaN() || d.isInfinite()))
				return null;  // NaN/Infinity have no BigDecimal form: an unparseable cell.
			if (n instanceof Float f && (f.isNaN() || f.isInfinite()))
				return null;
			return new BigDecimal(n.toString());
		}
		var v = str(o);
		if (ib(v))
			return null;
		try {
			return new BigDecimal(v.strip());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long millis(Object o) {
		if (o instanceof Number n)
			return n.longValue();
		if (o instanceof Date d)
			return d.getTime();
		if (o instanceof Instant i)
			return i.toEpochMilli();
		if (o instanceof OffsetDateTime t)
			return t.toInstant().toEpochMilli();
		if (o instanceof ZonedDateTime t)
			return t.toInstant().toEpochMilli();
		if (o instanceof LocalDateTime t)
			return t.toInstant(ZoneOffset.UTC).toEpochMilli();
		if (o instanceof LocalDate d)
			return d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		var v = str(o);
		if (ib(v))
			return null;
		// Cells use the absolute-only parser (a stored "-24h" is never "now minus a day"); a String that does not parse is an unparseable cell.
		try {
			return millis(ValueParse.parseAbsolute(SearchType.TIMESTAMP, v));
		} catch (BeanQuerySyntaxException e) {
			return null;
		}
	}

	@SuppressWarnings({
		"java:S1168" // Private sentinel: null means "not a version", distinct from any parsed (possibly zero-length) value; hot sort path.
	})
	private static int[] version(String v) {
		if (ib(v))
			return null;
		var parts = v.strip().split("\\.");
		var out = new int[parts.length];
		for (var i = 0; i < parts.length; i++) {
			try {
				out[i] = Integer.parseInt(parts[i].strip());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return out;
	}

	private static Integer cmpVersion(String a, String b) {
		var x = version(a);
		var y = version(b);
		if (x == null || y == null)
			return null;
		return cmpVersionParts(x, y);
	}

	private static int cmpVersionParts(int[] x, int[] y) {
		var n = Math.min(x.length, y.length);
		for (var i = 0; i < n; i++)
			if (x[i] != y[i])
				return Integer.compare(x[i], y[i]);
		return Integer.compare(x.length, y.length);
	}

	private static boolean versionPrefix(String cell, String arg) {
		var c = version(cell);
		var a = version(arg);
		if (c == null || a == null || a.length > c.length)
			return false;
		for (var i = 0; i < a.length; i++)
			if (a[i] != c[i])
				return false;
		return true;
	}
}
