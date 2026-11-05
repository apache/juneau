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
package org.apache.juneau.commons.svl;

import java.util.*;

/**
 * The one scanner for SVL markers: finds the close of a <c>${...}</c> body or a <c>#{...}</c> expression, and the
 * top-level separators of a <c>${...}</c> body.  The template compiler, the expression lexer and the colon split all use
 * it, so they always agree on where a marker ends.
 *
 * <h5 class='section'>Rules inside a <c>${...}</c> body (body mode):</h5>
 * <ul>
 * 	<li>A backslash makes the next character literal (including another backslash, a quote or a brace).  The backslash
 * 		is <em>not</em> stripped here; callers do that.
 * 	<li>A part starts at the start of a body (just past <c>${</c>) and immediately after a <c>:</c>.  Only
 * 		there does a <c>'</c> or <c>"</c> open a quoted run, which closes at the next unescaped matching quote (a
 * 		backslash escapes inside it too).  Anywhere else an apostrophe or double quote is a plain character, so
 * 		<c>${RQ:don't:b}</c> has the parts <c>RQ</c>, <c>don't</c> and <c>b</c>.
 * 	<li><c>${</c> and a plain <c>{</c> open a nested body; <c>#{</c> opens a nested expression.  A <c>}</c> closes the
 * 		innermost one.
 * </ul>
 *
 * <h5 class='section'>Rules inside a <c>#{...}</c> expression (expression mode):</h5>
 * <ul>
 * 	<li>A <c>'</c> or <c>"</c> opens a SpEL string wherever it appears; a doubled quote (<c>''</c>, <c>""</c>) is an
 * 		escaped quote and there is <em>no</em> backslash escape (so <c>'a\'</c> is the string <c>a\</c>).
 * 	<li>Outside a string, a backslash makes the next character literal (as the template compiler always did, so an
 * 		escaped <c>\}</c> does not end the expression; the expression parser then rejects the backslash).
 * 	<li><c>${</c> opens a nested body (body mode); <c>#{</c> and a plain <c>{</c> open a nested expression.
 * </ul>
 *
 * <p>
 * A character is <em>top level</em> when it is in the outermost body and not quoted, escaped or nested.  Malformed
 * input is tolerated by the split helpers: a stray <c>}</c> at top level is ignored, and an unclosed <c>{</c> or
 * unterminated quote means the rest of the string is nested/quoted (left to the compiler's error path).  The scanner
 * is iterative, so arbitrarily deep nesting cannot overflow the stack.
 */
final class VarSplit {

	private static final int BODY = 0, EXPR = 1;

	/**
	 * Internal-only stand-in for a literal comma (an escaped {@code \,}) in the internal comma form handed to
	 * {@link DefaultingVar}, {@link MultipartVar} and {@link MultipartResolvingVar}.  A Unicode private-use character, so
	 * it cannot be produced by the grammar.  The var classes call {@link #restoreCommas(String)} after splitting.
	 */
	static final char COMMA = '\uE000';

	/**
	 * Turns {@link #COMMA} back into a real comma and {@link #BACKSLASH} into a backslash.
	 *
	 * @param s The text.  Can be <jk>null</jk>.
	 * @return The text with real commas.
	 */
	static String restoreCommas(String s) {
		if (s == null || (s.indexOf(COMMA) == -1 && s.indexOf(BACKSLASH) == -1))
			return s;
		return s.replace(COMMA, ',').replace(BACKSLASH, '\\');
	}

	/** Internal-only stand-in for the literal backslash of an escaped backslash; restored with {@link #COMMA}. */
	static final char BACKSLASH = '\uE001';

	private VarSplit() {}

	/**
	 * Returns the index of the first top-level occurrence of <c>c</c>, or <c>-1</c>.
	 *
	 * @param s The body to scan.
	 * @param c The separator character.
	 * @return The index, or <c>-1</c> if none.
	 */
	static int firstTopLevel(String s, char c) {
		var r = hits(s, c);
		return r.isEmpty() ? -1 : r.get(0);
	}

	/**
	 * Returns the index of the last top-level occurrence of <c>c</c>, or <c>-1</c>.
	 *
	 * @param s The body to scan.
	 * @param c The separator character.
	 * @return The index, or <c>-1</c> if none.
	 */
	static int lastTopLevel(String s, char c) {
		var r = hits(s, c);
		return r.isEmpty() ? -1 : r.get(r.size() - 1);
	}

	/**
	 * Splits at every top-level occurrence of <c>c</c>, keeping empty segments.
	 *
	 * @param s The body to split.
	 * @param c The separator character.
	 * @return A list with at least one element.
	 */
	static List<String> splitTopLevel(String s, char c) {
		var out = new ArrayList<String>();
		int start = 0;
		for (var i : hits(s, c)) {
			out.add(s.substring(start, i));
			start = i + 1;
		}
		out.add(s.substring(start));
		return out;
	}

	/**
	 * Returns the index of the <c>}</c> that closes a <c>${...}</c> body starting at {@code from}, or <c>-1</c>.
	 *
	 * @param s The text.
	 * @param from The index of the first body character (just past <c>${</c>).
	 * @return The index of the closing brace, or <c>-1</c> if the body is unterminated.
	 */
	static int closeOfBody(String s, int from) {
		return scan(s, from, BODY, false, (char)0, null);
	}

	/**
	 * Returns the index of the <c>}</c> that closes a <c>#{...}</c> expression starting at {@code from}, or <c>-1</c>.
	 *
	 * @param s The text.
	 * @param from The index of the first expression character (just past <c>#{</c>).
	 * @return The index of the closing brace, or <c>-1</c> if the expression is unterminated.
	 */
	static int closeOfExpr(String s, int from) {
		return scan(s, from, EXPR, false, (char)0, null);
	}

	private static List<Integer> hits(String s, char c) {
		var hits = new ArrayList<Integer>();
		scan(s, 0, BODY, true, c, hits);
		return hits;
	}

	/**
	 * Scans from {@code from} in the given mode.
	 *
	 * @param toEnd <jk>true</jk> to scan a whole body (stray top-level <c>}</c> ignored), <jk>false</jk> to stop at the
	 * 	closing brace.
	 * @param sep The separator to record at top level (only when {@code hits} is not <jk>null</jk>).
	 * @param hits Receives top-level separator indexes.  Can be <jk>null</jk>.
	 * @return The closing brace index, <c>s.length()</c> when {@code toEnd}, or <c>-1</c> if unterminated.
	 */
	private static int scan(String s, int from, int mode0, boolean toEnd, char sep, List<Integer> hits) {
		var n = s.length();
		var stack = new ArrayDeque<Integer>();
		stack.push(mode0);
		var partStart = mode0 == BODY;
		var i = from;
		while (i < n) {
			int mode = stack.peek();
			var c = s.charAt(i);
			if (mode == BODY) {
				if (c == '\\') {
					i += 2;
					partStart = false;
					continue;
				}
				if (partStart && (c == '\'' || c == '"')) {
					var j = i + 1;
					while (j < n && s.charAt(j) != c)
						j += s.charAt(j) == '\\' ? 2 : 1;
					if (j >= n)
						break;
					i = j + 1;
					partStart = false;
					continue;
				}
				partStart = c == ':';
			} else if (c == '\\') {
				i += 2;
				continue;
			} else if (c == '\'' || c == '"') {
				var j = i + 1;
				while (j < n && (s.charAt(j) != c || (j + 1 < n && s.charAt(j + 1) == c)))
					j += s.charAt(j) == c ? 2 : 1;
				if (j >= n)
					break;
				i = j + 1;
				continue;
			}
			if ((c == '$' || c == '#') && i + 1 < n && s.charAt(i + 1) == '{') {
				stack.push(c == '$' ? BODY : EXPR);
				partStart = c == '$';
				i += 2;
				continue;
			}
			if (c == '{') {
				stack.push(mode);
				partStart = false;
			} else if (c == '}') {
				if (stack.size() > 1) {
					stack.pop();
					partStart = false;
				} else if (! toEnd) {
					return i;
				}
			} else if (hits != null && c == sep && mode == BODY && stack.size() == 1) {
				hits.add(i);
			}
			i++;
		}
		return toEnd ? n : -1;
	}
}
