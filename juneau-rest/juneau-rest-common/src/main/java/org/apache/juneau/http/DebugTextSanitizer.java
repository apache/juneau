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
package org.apache.juneau.http;

/**
 * Neutralizes log-forging (log-injection) in client-controlled strings before they are written to a log line.
 *
 * <p>
 * A remote client can place CR/LF (or Unicode line/paragraph separators, bidi controls, or other control characters)
 * into a request URI, header name/value, or body, and — if that text is interpolated verbatim into a log record — forge
 * additional physical log lines (e.g. a fake status banner) or corrupt a downstream log parser/SIEM. This utility
 * replaces every such character with a <b>visible, inert escape</b> (<c>\r</c>, <c>\n</c>, or <c>\\uXXXX</c>) so the
 * value's information is preserved (a reader can still see there <i>was</i> a newline) but it can no longer terminate
 * the current line or start a new one.
 *
 * <p>
 * The transform:
 * <ul>
 * 	<li>CR (<c>\\u000D</c>) &rarr; <c>\r</c>, LF (<c>\\u000A</c>) &rarr; <c>\n</c>.
 * 	<li>All other C0 controls (<c>\\u0000</c>&ndash;<c>\\u001F</c>) <b>except TAB</b> (<c>\\u0009</c>) &rarr; <c>\\uXXXX</c>.
 * 	<li>DEL and C1 controls (<c>\\u007F</c>&ndash;<c>\\u009F</c>, which includes NEL <c>\\u0085</c>) &rarr; <c>\\uXXXX</c>.
 * 	<li>Line separator (<c>\\u2028</c>), paragraph separator (<c>\\u2029</c>), and bidi controls
 * 		(<c>\\u202A</c>&ndash;<c>\\u202E</c>, <c>\\u2066</c>&ndash;<c>\\u2069</c>) &rarr; <c>\\uXXXX</c>.
 * 	<li>TAB (<c>\\u0009</c>) is <b>preserved</b> &mdash; it cannot forge a line.
 * </ul>
 *
 * <p>
 * When a length cap is supplied, truncation is applied <b>after</b> escaping, counts <b>sanitized</b> (character) output,
 * appends a <c>&hellip;[truncated]</c> marker, and never cuts an escape sequence or a surrogate pair in half. A fast path
 * returns the original reference unchanged when the string contains no character requiring an escape and is within the
 * cap, so well-behaved clients incur no allocation.
 *
 * <p>
 * All methods are thread-safe (the utility is stateless).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class DebugTextSanitizer {

	/** Marker appended when a value is truncated by the length cap. */
	static final String TRUNCATED_MARKER = "\u2026[truncated]";

	private DebugTextSanitizer() {}

	/**
	 * Escapes every log-forging control character in the given string, with no length cap.
	 *
	 * @param value The string to sanitize. Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The sanitized string, or the original reference if nothing needed escaping. <jk>null</jk> if the input was
	 * 	<jk>null</jk>.
	 */
	public static String sanitize(String value) {
		return sanitize(value, Integer.MAX_VALUE);
	}

	/**
	 * Escapes every log-forging control character in the given string, then caps the <i>sanitized</i> output to
	 * {@code maxLen} characters.
	 *
	 * <p>
	 * The cap counts the escaped (rendered) length, appends {@code …[truncated]} when it clips, and never splits an
	 * escape sequence or a surrogate pair.
	 *
	 * @param value The string to sanitize. Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @param maxLen The maximum number of sanitized characters to keep (excluding the truncation marker).
	 * @return The sanitized (and possibly truncated) string, or the original reference if nothing needed escaping and it
	 * 	was within the cap. <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String sanitize(String value, int maxLen) {
		if (value == null)
			return null;

		// Fast path: nothing to escape and already within the cap → return the original reference (no allocation).
		var clean = value.length() <= maxLen;
		if (clean) {
			for (var i = 0; i < value.length(); i++) {
				if (needsEscape(value.charAt(i))) {
					clean = false;
					break;
				}
			}
		}
		if (clean)
			return value;

		var sb = new StringBuilder(Math.min(value.length(), Math.max(maxLen, 0)) + 16);
		var truncated = false;
		var n = value.length();
		var i = 0;
		while (i < n) {
			var c = value.charAt(i);
			String token;
			int consumed;
			if (needsEscape(c)) {
				token = escape(c);
				consumed = 1;
			} else if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(value.charAt(i + 1))) {
				// Keep a surrogate pair together so the cap never cuts a code point in half.
				token = value.substring(i, i + 2);
				consumed = 2;
			} else {
				token = String.valueOf(c);
				consumed = 1;
			}
			if (sb.length() + token.length() > maxLen) {
				truncated = true;
				break;
			}
			sb.append(token);
			i += consumed;
		}
		if (truncated)
			sb.append(TRUNCATED_MARKER);
		return sb.toString();
	}

	private static boolean needsEscape(char c) {
		if (c == '\t')
			return false;
		if (c <= '\u001F')  // C0 controls (CR/LF included).
			return true;
		if (c >= '\u007F' && c <= '\u009F')  // DEL + C1 controls (includes NEL \u0085).
			return true;
		if (c == '\u2028' || c == '\u2029')  // Line / paragraph separators.
			return true;
		if (c >= '\u202A' && c <= '\u202E')  // Bidi embedding/override controls.
			return true;
		return c >= '\u2066' && c <= '\u2069';  // Bidi isolate controls.
	}

	private static String escape(char c) {
		if (c == '\r')
			return "\\r";
		if (c == '\n')
			return "\\n";
		return String.format("\\u%04X", (int) c);
	}
}
