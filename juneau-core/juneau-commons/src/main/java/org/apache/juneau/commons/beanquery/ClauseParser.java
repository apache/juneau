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

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * The clause grammar shared by the one {@link BeanQuery#getSearch() search} string and the one
 * {@link BeanQuery#getOpts() opts} string (design §5.3).
 *
 * <p>
 * A string is a list of {@code key=value} clauses:
 * <ul>
 * 	<li>Top-level commas (outside parentheses) separate clauses.  A comma <b>inside</b> parentheses is protected (so an
 * 		{@code $in(OPEN,CLOSED)} value survives intact).
 * 	<li>The <b>first unescaped</b> {@code =} in a clause separates the key from the value; the value keeps any further
 * 		{@code =} characters verbatim.
 * 	<li>Backslash escapes decode to literals: {@code \,} &rarr; comma, {@code \=} &rarr; equals, {@code \\} &rarr;
 * 		backslash, {@code \$} &rarr; dollar.  Any other backslash is left as-is.
 * </ul>
 *
 * <p>
 * For {@code search}, the key is a column name and the value is a raw {@code $}-expression handed to
 * {@link SearchExpressionParser}.  For {@code opts}, the key is an option name and the value is a plain string.  A
 * session decodes these strings when the query runs; the {@link BeanQuery} bean itself never parses them.
 *
 * @since 10.0.0
 */
public final class ClauseParser {

	private ClauseParser() {}

	/**
	 * Parses a search-or-opts string into an ordered {@code key}&rarr;{@code value} map.
	 *
	 * <p>
	 * Keys are stripped of surrounding whitespace; values are decoded but otherwise kept verbatim.  A blank input, or a
	 * <jk>null</jk> input, yields an empty map.  A later clause with a duplicate key overwrites an earlier one.
	 *
	 * @param s The raw string (a {@code search} or {@code opts} value).  Can be <jk>null</jk>.
	 * @return A new, mutable, insertion-ordered map (never <jk>null</jk>).
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for parser state machine
		"java:S6541" // Brain method acceptable for parser state machine
	})
	public static Map<String,String> parse(String s) {
		var m = new LinkedHashMap<String,String>();
		if (ib(s))
			return m;

		// S1: Reading key (looking for unescaped '=' or top-level ',' or end).
		// S2: Saw '\', next char is a key literal (\, / \= / \\ collapse; others keep the backslash).
		// S3: Reading value (looking for top-level ',' or end); '(' / ')' adjust depth.
		// S4: Saw '\', next char is a value literal.

		var state = S1;
		var key = new StringBuilder();
		var value = new StringBuilder();
		var depth = 0;
		var len = s.length();

		for (var i = 0; i <= len; i++) {
			var end = i == len;
			var c = end ? ',' : s.charAt(i); // Synthetic comma flushes the final clause.

			if (state == S1) {
				if (end || (c == ',' && depth == 0)) {
					putClause(m, key, value);
					key.setLength(0);
					value.setLength(0);
					depth = 0;
				} else if (c == '\\') {
					state = S2;
				} else if (c == '=') {
					state = S3;
				} else {
					if (c == '(')
						depth++;
					else if (c == ')' && depth > 0)
						depth--;
					key.append(c);
				}
			} else if (state == S2) {
				if (end) {
					key.append('\\');
					putClause(m, key, value);
				} else {
					appendUnescaped(key, c);
					state = S1;
				}
			} else if (state == S3) { // NOSONAR - State check necessary for state machine
				if (end || (c == ',' && depth == 0)) {
					putClause(m, key, value);
					key.setLength(0);
					value.setLength(0);
					depth = 0;
					state = S1;
				} else if (c == '\\') {
					state = S4;
				} else {
					if (c == '(')
						depth++;
					else if (c == ')' && depth > 0)
						depth--;
					value.append(c);
				}
			} else {  // S4 — the only remaining state.
				if (end) {
					value.append('\\');
					putClause(m, key, value);
				} else {
					appendUnescaped(value, c);
					state = S3;
				}
			}
		}

		return m;
	}

	/**
	 * Escapes a single key or value so it can be spliced into a clause string without changing its meaning.
	 *
	 * <p>
	 * Backslash, comma, and equals become {@code \\}, {@code \,}, and {@code \=} respectively.  A <b>leading</b>
	 * {@code $} additionally becomes {@code \$}, so an escaped column name can never be mistaken for a
	 * {@code $or(}/{@code $and(}/{@code $not(} search-grammar group prefix (design §4).  Note that a raw
	 * {@code $}-expression value must <b>not</b> be escaped &mdash; its commas live inside parentheses and its
	 * structure is the {@code $}-language's own, not this grammar's.
	 *
	 * @param s The raw key or value.  Can be <jk>null</jk>.
	 * @return The escaped text, or <jk>null</jk> if {@code s} is <jk>null</jk>.
	 */
	public static String escape(String s) {
		if (s == null)
			return null;
		var sb = new StringBuilder(s.length() + 4);
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if ((i == 0 && c == '$') || c == '\\' || c == ',' || c == '=')
				sb.append('\\');
			sb.append(c);
		}
		return sb.toString();
	}

	/** Emits one decoded clause when the key is non-blank after strip. */
	private static void putClause(Map<String,String> m, StringBuilder key, StringBuilder value) {
		var k = key.toString().strip();
		if (! k.isEmpty())
			m.put(k, value.toString());
	}

	/**
	 * Appends one escape sequence into {@code buf}: {@code \,}/{@code \=}/{@code \\}/{@code \$} collapse to the
	 * literal; any other following character keeps the backslash.
	 */
	private static void appendUnescaped(StringBuilder buf, char c) {
		if (c == ',' || c == '=' || c == '\\' || c == '$')
			buf.append(c);
		else
			buf.append('\\').append(c);
	}
}
