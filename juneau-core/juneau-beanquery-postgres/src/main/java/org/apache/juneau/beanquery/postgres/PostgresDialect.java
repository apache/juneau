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
package org.apache.juneau.beanquery.postgres;

import static java.util.regex.Pattern.*;
import static org.apache.juneau.commons.beanquery.SearchType.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.beanquery.sql.*;

/**
 * The PostgreSQL {@link SqlDialect}: the one dialect Juneau ships for the shared JDBC {@link SqlBeanQueryContext}.
 *
 * <p>
 * It renders the built-in {@code $}-operators (design §5.1) into Postgres predicates that mirror the in-memory
 * semantics: exact, case-sensitive {@code $eq}/{@code $in}/{@code $ne} on text/id/enum columns, with the
 * case-insensitive {@code $eqic} (and the case-insensitive {@code $contains}/{@code $prefix}) via {@code lower(...)}
 * and {@code ILIKE}; full-string {@code $regex} via {@code ~}/{@code ~*} with the same {@code i}/{@code m}/{@code s}
 * flag semantics as {@code java.util.regex}; dotted-tuple version comparison and {@code $prefix}; and typed binds for
 * numeric/boolean/timestamp comparisons.  A bare pattern maps {@code *} to {@code %} and {@code ?} to {@code _} for
 * {@code ILIKE}, unless the literal was quoted in the search string, in which case it is matched literally.
 *
 * <p>
 * Values arrive already typed (design #4 §4: {@link SearchType#parse(String)} runs during query resolution, before
 * this dialect ever sees a value), and are always bound as {@code ?} parameters, never concatenated into SQL text.
 *
 * <ul class='spaced-list'>
 * 	<li><b>ENUM columns</b> are compared as text: the column is cast ({@code col::text}) wherever this dialect applies a
 * 		text function or operator to it, so a native Postgres {@code enum} column behaves like a {@code text} one
 * 		(including bare {@code *}/{@code ?} wildcards, which match the whole constant name, case-insensitively).
 * 	<li><b>VERSION columns</b> are stored as dotted text (for example {@code 10.0.1}) and compared as a tuple of integers
 * 		({@code 10.9} sorts before {@code 10.10}), like the in-memory engine.  A stored value
 * 		that is not a dotted sequence of integers never matches.  Known, deliberate differences from the in-memory
 * 		parser ({@code Integer.parseInt} over {@code split("\\.")}): each part is limited to 9 digits here (so it always
 * 		fits a Postgres {@code int}; longer parts never match, where in-memory accepts up to the {@code int} range), and a
 * 		trailing empty part ({@code 10.}) or a signed part ({@code +1}, {@code -1}) is rejected here but accepted in-memory.
 * 	<li><b>Blank</b> ({@code $blank}, and the blank-matching {@code $ne}) means {@code NULL} or only ASCII whitespace
 * 		(space, tab, line breaks, vertical tab, form feed and the C0 separators), matching {@code String.isBlank()} for
 * 		ASCII; non-ASCII Unicode spaces are not trimmed.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Register Postgres as the dialect for a JDBC-backed context.</jc>
 * 	SqlBeanQueryContext&lt;Task&gt; <jv>ctx</jv> = SqlBeanQueryContext.<jsm>create</jsm>(Task.<jk>class</jk>)
 * 		.dialect(PostgresDialect.<jsf>INSTANCE</jsf>)
 * 		.table(<js>"tasks"</js>)
 * 		.column(<js>"name"</js>, SearchType.<jsf>TEXT</jsf>)
 * 		.column(<js>"status"</js>, SearchType.<jsf>ENUM</jsf>)
 * 		.connectionSupplier(<jv>supplier</jv>)
 * 		.build();
 *
 * 	<jc>// Render a bare value directly: the value is already typed, so a NUMERIC column gets a BigDecimal bind.</jc>
 * 	SqlFragment <jv>frag</jv> = PostgresDialect.<jsf>INSTANCE</jsf>.renderBare(<js>"\"age\""</js>, SearchType.<jsf>NUMERIC</jsf>, <jk>new</jk> BigDecimal(<js>"30"</js>), <jk>false</jk>);
 * 	<jc>// frag.sql() == "\"age\" = ?", frag.binds() == [30]</jc>
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated SQL fragments read more clearly inline than as constants.
})
public final class PostgresDialect implements SqlDialect {

	/** The singleton instance. */
	public static final PostgresDialect INSTANCE = new PostgresDialect();

	private final Map<String,SearchSqlRenderer> builtins = new HashMap<>();

	private PostgresDialect() {
		builtins.put("$eq", (d, col, type, args) -> eq(col, type, args.get(0)));
		builtins.put("$eqic", (d, col, type, args) -> eqicFrag(col, type, args.get(0)));
		builtins.put("$ne", (d, col, type, args) -> ne(col, type, args));
		builtins.put("$in", (d, col, type, args) -> in(col, type, args));
		builtins.put("$contains", (d, col, type, args) -> frag(col + " ILIKE ?", like("%", text(args.get(0)), "%")));
		builtins.put("$prefix", (d, col, type, args) -> prefix(col, type, args.get(0)));
		builtins.put("$regex", (d, col, type, args) -> regex(col, args));
		builtins.put("$blank", (d, col, type, args) -> SqlFragment.of(blank(col)));
		builtins.put("$gt", (d, col, type, args) -> cmp(col, " > ", type, args.get(0)));
		builtins.put("$gte", (d, col, type, args) -> cmp(col, " >= ", type, args.get(0)));
		builtins.put("$lt", (d, col, type, args) -> cmp(col, " < ", type, args.get(0)));
		builtins.put("$lte", (d, col, type, args) -> cmp(col, " <= ", type, args.get(0)));
		builtins.put("$between", (d, col, type, args) -> between(col, type, args.get(0), args.get(1)));
	}

	@Override /* SqlDialect */
	public String id() {
		return "postgres";
	}

	/** Postgres compares versions as {@code int[]} tuples (see {@link #versionArray}), matching the in-memory order. */
	@Override /* SqlDialect */
	public boolean supportsVersionOrdering() {
		return true;
	}

	@Override /* SqlDialect */
	public String quote(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

	@Override /* SqlDialect */
	public SearchSqlRenderer builtinRenderer(String operatorName) {
		return builtins.get(operatorName);
	}

	@Override /* SqlDialect */
	public String renderLimitOffset(Long limit, Long offset) {
		var sb = new StringBuilder();
		if (limit != null)
			sb.append(" LIMIT ").append(limit.longValue());
		if (offset != null && offset != 0L)
			sb.append(" OFFSET ").append(offset.longValue());
		return sb.toString();
	}

	@Override /* SqlDialect */
	public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) {
		switch (type) {
			case TEXT:
			case ID: {
				var token = text(value);
				if (quoted || ! hasWildcard(token))
					return frag(columnSql + " ILIKE ?", like("%", token, "%"));
				return frag(columnSql + " ILIKE ?", glob(token));
			}
			case ENUM: {
				// Same rule as the in-memory engine: a wildcard pattern is a case-insensitive whole-string glob over the
				// constant name; anything else (including a quoted literal) is case-insensitive equality.
				var token = text(value);
				if (quoted || ! hasWildcard(token))
					return eqicFrag(columnSql, type, token);
				return frag(textOf(columnSql, type) + " ILIKE ?", glob(token));
			}
			case VERSION:
				return prefix(columnSql, type, value);
			default:
				return frag(columnSql + " = ?", value);  // NUMERIC / BOOLEAN / TIMESTAMP: typed-exact; quoting is a no-op.
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Built-in renderers
	// -----------------------------------------------------------------------------------------------------------------

	/** Exact, case-sensitive equality (typed bind); used by {@code $eq}. */
	private static SqlFragment eq(String col, SearchType type, Object arg) {
		if (type == VERSION)
			return versionIn(col, List.of(arg));
		return frag(textOf(col, type) + " = ?", bind(type, arg));
	}

	/**
	 * Case-insensitive equality; used by {@code $eqic} and the bare-token default for enum.
	 *
	 * <p>
	 * Named {@code eqicFrag} (rather than {@code eqic}) to make clear that it renders a SQL fragment, as opposed to
	 * evaluating a boolean comparison like the in-memory engine's {@code eqic} helper.
	 */
	private static SqlFragment eqicFrag(String col, SearchType type, Object arg) {
		return frag("lower(" + textOf(col, type) + ") = lower(?)", text(arg));
	}

	private static SqlFragment ne(String col, SearchType type, List<Object> args) {
		if (type == VERSION) {
			// Mirrors InMemoryMatch: a blank cell matches; otherwise no listed version may equal the (parseable) cell.
			var in = versionIn(col, args);
			var sb = new StringBuilder(blankOr(col, col));
			if (in.binds().isEmpty())
				return SqlFragment.of(sb.append(" OR 1 = 1)").toString());
			sb.append(" OR NOT COALESCE(").append(in.sql()).append(", FALSE))");
			return SqlFragment.of(sb.toString(), in.binds());
		}
		var c = textOf(col, type);
		var sb = new StringBuilder(isCiText(type) ? blankOr(col, c) : "(" + col + " IS NULL");
		sb.append(" OR ").append(c).append(" NOT IN (");
		appendPlaceholders(sb, args.size());
		sb.append("))");
		return SqlFragment.of(sb.toString(), binds(type, args));
	}

	private static SqlFragment in(String col, SearchType type, List<Object> args) {
		if (type == VERSION)
			return versionIn(col, args);
		var sb = new StringBuilder(textOf(col, type)).append(" IN (");
		appendPlaceholders(sb, args.size());
		sb.append(')');
		return SqlFragment.of(sb.toString(), binds(type, args));
	}

	private static SqlFragment prefix(String col, SearchType type, Object arg) {
		if (type == VERSION) {
			var v = versionArg(arg);
			if (v == null)
				return NEVER;
			var n = v.split("\\.").length;
			// Array slice: a stored version with fewer parts than the prefix yields a shorter array, which never equals it.
			return frag(versionArray(col) + "[1:" + n + "] = " + "string_to_array(?, '.')::int[]", v);
		}
		return frag(col + " ILIKE ?", like("", text(arg), "%"));
	}

	/**
	 * {@code $regex}: a full-string match with the flags of {@code InMemoryMatch}/{@code java.util.regex}.
	 *
	 * <p>
	 * Case-insensitive unless a {@code flags=} argument is given without {@code i} (then {@code ~} instead of {@code ~*}).
	 * Java's {@code m} (multiline) and {@code s} (dotall) are mapped onto Postgres's embedded regex options, which bundle
	 * newline handling differently: neither flag &rarr; {@code (?p)} ({@code .} does not match a newline, {@code ^}/{@code $}
	 * only at the string ends), {@code s} &rarr; the default, {@code m} &rarr; {@code (?n)} and both &rarr; {@code (?w)}.
	 * The pattern is anchored with {@code \A...\Z} (string boundaries, unaffected by those options) rather than
	 * {@code ^...$}, so {@code m} cannot turn the full-string match into a per-line one.
	 *
	 * <p>
	 * The pattern is first compiled with {@link java.util.regex.Pattern} using the mapped flags; a pattern that is not a valid
	 * Java regex never matches (renders as an always-false predicate), exactly like the in-memory engine, and can
	 * therefore never break out of the surrounding {@code \A(?:...)\Z} anchors (for example {@code x)|(y}).
	 *
	 * <p>
	 * <b>Flavor differences:</b> this uses PostgreSQL's POSIX {@code ~}/{@code ~*} operators (Advanced Regular
	 * Expressions), which are not Java regex.  Java and PostgreSQL (ARE) regex syntax are similar but not identical (for example Java
	 * possessive quantifiers and {@code \p{L}} classes are not supported by PostgreSQL, and a Java-valid pattern may be
	 * rejected by the server, failing the query).  A pathological pattern can also be slow on the server, so configure a
	 * {@code statement_timeout} for connections that run user-supplied patterns.  A pattern that uses the {@code (?x)}
	 * comment flag with a trailing {@code #} comment is passed through unchanged and will be rejected by the server.
	 */
	private static SqlFragment regex(String col, List<Object> args) {
		var ci = true;  // Case-insensitive by default (design §5.1).
		var multiline = false;
		var dotall = false;
		for (var i = 1; i < args.size(); i++) {
			var a = text(args.get(i)).strip();
			if (a.startsWith("flags=")) {
				var f = a.substring(6);  // The last flags= argument wins, as in-memory.
				ci = f.indexOf('i') >= 0;
				multiline = f.indexOf('m') >= 0;
				dotall = f.indexOf('s') >= 0;
			}
		}
		var pattern = text(args.get(0));
		var javaFlags = 0;
		if (ci)
			javaFlags |= CASE_INSENSITIVE;
		if (multiline)
			javaFlags |= MULTILINE;
		if (dotall)
			javaFlags |= DOTALL;
		try {
			Pattern.compile(pattern, javaFlags);
		} catch (PatternSyntaxException e) {
			return NEVER;  // Invalid pattern = no match, like InMemoryMatch/RegexBudget; also keeps it from escaping the \A(?:...)\Z anchors.
		}
		String mods;
		if (multiline)
			mods = dotall ? "(?w)" : "(?n)";
		else
			mods = dotall ? "" : "(?p)";
		return frag(col + (ci ? " ~* ?" : " ~ ?"), mods + "\\A(?:" + pattern + ")\\Z");
	}

	private static SqlFragment cmp(String col, String operator, SearchType type, Object arg) {
		if (type == VERSION) {
			var v = versionArg(arg);
			return v == null ? NEVER : frag(versionArray(col) + operator + "string_to_array(?, '.')::int[]", v);
		}
		return frag(col + operator + "?", arg);
	}

	private static SqlFragment between(String col, SearchType type, Object lo, Object hi) {
		if (type == VERSION) {
			var l = versionArg(lo);
			var h = versionArg(hi);
			return (l == null || h == null) ? NEVER : frag(versionArray(col) + " BETWEEN " + "string_to_array(?, '.')::int[]" + " AND " + "string_to_array(?, '.')::int[]", l, h);
		}
		return frag(col + " BETWEEN ? AND ?", lo, hi);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// VERSION (stored as dotted text; compared as a tuple of integers, like InMemoryMatch.cmpVersion)
	// -----------------------------------------------------------------------------------------------------------------

	/** The predicate for a version that can never match (an unparseable argument, like in-memory). */
	private static final SqlFragment NEVER = SqlFragment.of("(1 = 0)");

	/**
	 * The column as an {@code int[]}, or {@code NULL} when the stored text is not a dotted sequence of integers.
	 * Postgres compares arrays element by element and then by length, which is exactly the in-memory dotted-tuple order.
	 *
	 * <p>
	 * The guard literal is an {@code E'...'} string with doubled backslashes, so its meaning does not depend on
	 * {@code standard_conforming_strings}, and it contains no {@code ?} that pgjdbc could mistake for a placeholder.
	 */
	@SuppressWarnings({
		"java:S6353" // Deliberately ASCII [0-9]: Postgres \\d may match non-ASCII digits in some locales, which would then fail the int cast.
	})
	private static String versionArray(String col) {
		return "(CASE WHEN " + col + " ~ E'^\\\\s*[0-9]{1,9}(\\\\s*\\\\.\\\\s*[0-9]{1,9})*\\\\s*$' THEN string_to_array(" + col + ", '.')::int[] END)";
	}

	/** A version argument normalized for {@link #VERSION_BIND}, or <jk>null</jk> if it is not a dotted sequence of integers. */
	private static String versionArg(Object arg) {
		// Hand-rolled scan of  ws* d{1,9} (ws* '.' ws* d{1,9})* ws*  (ASCII digits/whitespace), equivalent to that regex but
		// without a backtracking group.  At most 9 digits per part, so every part fits a Postgres int (no 22003 overflow).
		var s = text(arg);
		var n = s.length();
		var parts = new ArrayList<String>();
		var i = skipWs(s, 0);
		while (true) {
			var start = i;
			while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9')
				i++;
			if (i == start || i - start > 9)
				return null;
			parts.add(String.valueOf(Integer.parseInt(s.substring(start, i))));
			i = skipWs(s, i);
			if (i == n)
				return String.join(".", parts);
			if (s.charAt(i) != '.')
				return null;
			i = skipWs(s, i + 1);
		}
	}

	/** Index of the first non-whitespace char at or after {@code i}; whitespace is the regex {@code \s} set (ASCII). */
	private static int skipWs(String s, int i) {
		while (i < s.length() && " \t\n\u000B\f\r".indexOf(s.charAt(i)) >= 0)
			i++;
		return i;
	}

	/** Equality against any of the (parseable) versions; renders as never-true when none parses. */
	private static SqlFragment versionIn(String col, List<Object> args) {
		var sb = new StringBuilder();
		var binds = new ArrayList<>();
		for (var a : args) {
			var v = versionArg(a);
			if (v == null)
				continue;
			sb.append(binds.isEmpty() ? "" : " OR ").append(versionArray(col)).append(" = ").append("string_to_array(?, '.')::int[]");
			binds.add(v);
		}
		if (binds.isEmpty())
			return NEVER;
		return SqlFragment.of(binds.size() > 1 ? "(" + sb + ")" : sb.toString(), binds);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------------------------------------------

	/** The cell is {@code NULL} or ASCII-whitespace only, like the in-memory blank test. */
	private static String blank(String col) {
		return blankOr(col, col) + ")";
	}

	/** Opens a parenthesized disjunction with the blank test for {@code textExpr}; the caller appends the rest and closes it. */
	private static String blankOr(String col, String textExpr) {
		return "(" + col + " IS NULL OR btrim(" + textExpr + ", " + "E' \\t\\n\\x0B\\f\\r\\x1C\\x1D\\x1E\\x1F'" + ") = ''";
	}

	private static boolean isCiText(SearchType type) {
		return type == TEXT || type == ID || type == ENUM;
	}

	private static boolean hasWildcard(String token) {
		return token.indexOf('*') >= 0 || token.indexOf('?') >= 0;
	}

	/** A bare glob ({@code *} any run, {@code ?} one character) as a LIKE pattern, with LIKE metacharacters escaped first. */
	private static String glob(String token) {
		return escapeLike(token).replace("*", "%").replace("?", "_");
	}

	/** The column as text: an ENUM column may be a native Postgres enum, which has no text operators of its own. */
	private static String textOf(String col, SearchType type) {
		return type == ENUM ? col + "::text" : col;
	}

	/** Text-like columns bind strings; everything else binds the already-typed value. */
	private static Object bind(SearchType type, Object arg) {
		return isCiText(type) ? text(arg) : arg;
	}

	private static List<Object> binds(SearchType type, List<Object> args) {
		return args.stream().map(a -> bind(type, a)).toList();
	}

	private static void appendPlaceholders(StringBuilder sb, int count) {
		for (var i = 0; i < count; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append('?');
		}
	}

	/** Escapes LIKE/ILIKE metacharacters (backslash is the default Postgres LIKE escape). */
	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static String like(String pre, String s, String post) {
		return pre + escapeLike(s) + post;
	}

	/**
	 * Asserts that a value on a text-only path is a {@link String}.
	 *
	 * <p>
	 * Values for TEXT / ID / ENUM / VERSION columns are always {@link String}s (design #4 §4: {@link SearchType#parse}
	 * leaves them unchanged), so a non-string here is an engine bug, not a user error.
	 *
	 * @param o The value.
	 * @return The value as a string.
	 * @throws IllegalStateException If the value is not a {@link String}.
	 */
	private static String text(Object o) {
		if (o instanceof String s)
			return s;
		throw new IllegalStateException(String.format("Expected a String value for a text comparison but got '%s'.", o == null ? null : o.getClass().getName()));  // I believe we have a short for getting class name?
	}

	private static SqlFragment frag(String sql, Object...binds) {
		return SqlFragment.of(sql, binds);
	}
}
