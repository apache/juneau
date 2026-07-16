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
package org.apache.juneau.commons.lang;

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.utils.*;

/**
 * Printf-style string formatter with a small cache and lenient argument handling.
 *
 * <p>
 * This class provides a thread-safe, cacheable formatter for printf-style ({@link java.util.Formatter}) patterns
 * (<js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, <js>"%1$s"</js>, etc.). MessageFormat-style (<js>"{0}"</js>)
 * patterns are <b>not</b> recognized here — <c>{</c> and <c>'</c> are treated as literal text. For MessageFormat
 * rendering use {@link StringUtils#mformat(String, Object...)} / {@code Shorts.mf(...)}/{@code Shorts.mfs(...)}
 * (backed by {@link java.text.MessageFormat}).
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 *   <li><b>Printf grammar:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, <js>"%1$s"</js>, etc.</li>
 *   <li><b>Thread-Safe:</b> Immutable class, safe for concurrent use</li>
 *   <li><b>Cacheable:</b> Use {@link #ofPrintf(String)} for cached instances</li>
 *   <li><b>Lenient null handling:</b> <jk>null</jk> arguments render as <js>"null"</js> for string conversions
 *       (rather than throwing) and a fast path avoids {@link java.util.Formatter} for simple <js>"%s"</js> tokens</li>
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple printf</jc>
 * 	StringFormat <jv>fmt</jv> = StringFormat.<jsm>ofPrintf</jsm>(<js>"Hello %s, you have %d items"</js>);
 * 	String <jv>result</jv> = <jv>fmt</jv>.<jsm>format</jsm>(<js>"John"</js>, 5);
 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
 *
 * 	<jc>// Argument index (reuse arguments)</jc>
 * 	StringFormat <jv>fmt2</jv> = StringFormat.<jsm>ofPrintf</jsm>(<js>"%1$s loves %2$s, and %1$s also loves %3$s"</js>);
 * 	String <jv>result2</jv> = <jv>fmt2</jv>.<jsm>format</jsm>(<js>"Alice"</js>, <js>"Bob"</js>, <js>"Charlie"</js>);
 * 	<jc>// Returns: "Alice loves Bob, and Alice also loves Charlie"</jc>
 * </p>
 *
 * <h5 class='section'>Caching:</h5>
 * <p>
 * Use {@link #ofPrintf(String)} to get cached instances. The cache is thread-safe and limited to 1000 entries.
 * For uncached instances, use the constructor directly.
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and thread-safe. Multiple threads can safely use the same instance concurrently.
 * </p>
 *
 * @see StringUtils#format(String, Object...)
 * @see StringUtils#mformat(String, Object...)
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public final class StringFormat {

	// Argument name constants for assertArgNotNull
	private static final String ARG_pattern = "pattern";

	/**
	 * Literal text token.
	 */
	private static final class LiteralToken extends Token {
		private final String text;

		LiteralToken(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return "[L:" + text + "]";
		}

		@Override
		void append(StringBuilder sb, Object[] args, Locale locale) {
			sb.append(text);
		}
	}

	/**
	 * Printf-style token (e.g., %s, %d, %.2f).
	 */
	private static final class StringFormatToken extends Token {
		private final char format; // 's' = simple (handle directly), 'o' = other (use String.format)
		private final String content; // The format string to pass to String.format (null for simple formats)
		private final int index; // 0-based index

	StringFormatToken(String content, int index) {
		// content is everything after '%' (e.g., "s", "1$s", "d", ".2f", "1$.2f")
		var dollarIndex = content.indexOf('$');
		if (dollarIndex >= 0) {
			index = parseIndex(content.substring(0, dollarIndex)) - 1;
			content = content.substring(dollarIndex + 1);
		}
			this.format = content.length() == 1 ? content.charAt(content.length() - 1) : 'z';
			this.index = index;
			this.content = "%" + content;
		}

		/**
		 * Parses the index from a StringFormat content string.
		 *
		 * @param s The content string (e.g., "1", "2").
		 * @return The parsed index.
		 */
		private static int parseIndex(String s) {
			return Integer.parseInt(s);
		}

		/**
		 * Creates a StringFormatToken and adds it to the tokens list.
		 *
		 * @param tokens The list of tokens to add to.
		 * @param pattern The pattern string.
		 * @param start The start index in the pattern.
		 * @param end The end index in the pattern.
		 * @param index The token index.
		 */
		static void create(List<Token> tokens, String pattern, int start, int end, int index) {
			tokens.add(new StringFormatToken(pattern.substring(start, end), index));
		}

		@Override
		public String toString() {
			return "[S:" + format + index + ":" + content + "]";
		}

		@Override
		@SuppressWarnings({
			"java:S3776", // Cognitive complexity acceptable for this specific logic
			"java:S6541", // StringBuilder used in local scope; no synchronization needed
		})
		void append(StringBuilder sb, Object[] args, Locale locale) {
			// String.format() throws MissingFormatArgumentException when argument is missing
			if (args == null || index >= args.length || index < 0) {
				throw new MissingFormatArgumentException(content);
			}
			var o = args[index];
			var l = locale == null ? Locale.getDefault() : locale;
			var dl = locale == null || locale.equals(Locale.getDefault());
			switch (format) {
				case 'b':
					// String.format() with %b converts:
					// - null -> "false"
					// - Boolean -> toString()
					// - Any other non-null value -> "true"
					if (o == null) {
						sb.append("false");
					} else if (o instanceof Boolean) {
						sb.append(o.toString());
					} else {
						sb.append("true");
					}
					return;
				case 'B':
					// String.format() with %B converts:
					// - null -> "FALSE"
					// - Boolean -> toString().toUpperCase()
					// - Any other non-null value -> "TRUE"
					if (o == null) {
						sb.append("FALSE");
					} else if (o instanceof Boolean) {
						sb.append(o.toString().toUpperCase());
					} else {
						sb.append("TRUE");
					}
					return;
				case 's':
					if (o == null) {
						sb.append("null");
						return;
					}
					sb.append(o.toString());
					return;
				case 'S':
					if (o == null) {
						sb.append("NULL");
						return;
					}
					sb.append(o.toString().toUpperCase());
					return;
				case 'd':
					if (o == null) {
						sb.append("null");
						return;
					}
					if (o instanceof Number o2) {
						if (dl) {
							if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Short) {
								sb.append(o);
							} else {
								// For other Number types (BigDecimal, BigInteger, etc.), convert to long
								sb.append(o2.longValue());
							}
							return;
						}
						// For non-default locales, use String.format to ensure printf-style consistency
						sb.append(sf(l, "%d", o));
						return;
					}
					break;
				case 'x':
					if (o == null) {
						sb.append("null");
						return;
					}
					if (o instanceof Integer o2) {
						sb.append(Integer.toHexString(o2));
						return;
					} else if (o instanceof Long o2) {
						sb.append(Long.toHexString(o2));
						return;
					}
					break;
				case 'X':
					if (o == null) {
						sb.append("NULL");
						return;
					}
					if (o instanceof Integer o2) {
						sb.append(Integer.toHexString(o2).toUpperCase());
						return;
					} else if (o instanceof Long o2) {
						sb.append(Long.toHexString(o2).toUpperCase());
						return;
					}
					break;
				case 'o':
					if (o == null) {
						sb.append("null");
						return;
					}
					if (o instanceof Integer o2) {
						sb.append(Integer.toOctalString(o2));
						return;
					} else if (o instanceof Long o2) {
						sb.append(Long.toOctalString(o2));
						return;
					}
					break;
				case 'c':
					if (o == null) {
						sb.append("null");
						return;
					}
					if (o instanceof Character) {
						sb.append(o);
						return;
					} else if (o instanceof Integer o2) {
						sb.append((char)o2.intValue());
						return;
					}
					break;
				case 'C':
					if (o == null) {
						sb.append("NULL");
						return;
					}
					if (o instanceof Character o2) {
						sb.append(Character.toUpperCase(o2));
						return;
					} else if (o instanceof Integer o2) {
						sb.append(Character.toUpperCase((char)o2.intValue()));
						return;
					}
					break;
				case 'f':
					if (o == null) {
						sb.append("null");
						return;
					}
					// Always use String.format() to match exact behavior (precision, etc.)
					if (o instanceof Number) {
						sb.append(sf(l, "%f", o));
						return;
					}
					break;
				default:
					break;
			}

			// Fallback to String.format for any other simple format
			sb.append(sf(l, content, o));
		}
	}

	/**
	 * Base class for format tokens.
	 */
	private abstract static class Token {

		/**
		 * Appends the formatted content to the StringBuilder.
		 *
		 * @param sb The StringBuilder to append to.
		 * @param args The arguments array.
		 * @param locale The locale for formatting (can be null for default).
		 */
		abstract void append(StringBuilder sb, Object[] args, Locale locale);
	}

	private static final CacheMode CACHE_MODE = env("juneau.StringFormat.caching", CacheMode.FULL);

	private static final Cache<String,StringFormat> CACHE = Cache.of(String.class, StringFormat.class).maxSize(1000).cacheMode(CACHE_MODE).build();

	private static final AsciiSet PRINTF_CONVERSION_CHARS = AsciiSet.of("bBhHsScCdoxXeEfgGaAtTn%");

	private static final AsciiSet PRINTF_FORMAT_CHARS = AsciiSet.of("-+ 0(#.*$");

	/**
	 * Formats a pattern string with the given arguments using printf-style semantics.
	 *
	 * <p>
	 * <c>{</c> and <c>'</c> are literal text, matching {@link java.util.Formatter} semantics.
	 * If no arguments are passed in, the pattern is returned as-is.
	 *
	 * @param pattern The format pattern.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk> or format specifiers are invalid.
	 */
	public static String formatPrintf(String pattern, Object...args) {
		if (args.length == 0)
			return pattern;
		return ofPrintf(pattern).format(args);
	}

	/**
	 * Formats a pattern string with the given arguments using printf-style semantics and the specified locale.
	 *
	 * @param pattern The format pattern.
	 * @param locale The locale to use for formatting. If <jk>null</jk>, uses the default locale.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk> or format specifiers are invalid.
	 */
	public static String formatPrintf(String pattern, Locale locale, Object...args) {
		if (args.length == 0)
			return pattern;
		return ofPrintf(pattern).format(locale, args);
	}

	/**
	 * Returns a cached printf StringFormat instance for the given pattern.
	 *
	 * <p>
	 * This method uses a thread-safe cache to avoid recreating StringFormat instances for the same pattern.
	 * The cache is limited to 1000 entries. <c>{</c> and <c>'</c> are treated as literal text (genuine
	 * {@link java.util.Formatter} semantics); only printf-style placeholders are recognized.
	 *
	 * @param pattern The format pattern.
	 * @return A cached or new StringFormat instance.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk>.
	 */
	public static StringFormat ofPrintf(String pattern) {
		assertArgNotNull(ARG_pattern, pattern);
		return CACHE.get(pattern, () -> new StringFormat(pattern));
	}

	private static void lit(List<Token> tokens, String pattern, int start) {
		if (start == pattern.length())
			return;
		tokens.add(new LiteralToken(pattern.substring(start)));
	}

	private static void lit(List<Token> tokens, String pattern, int start, int end) {
		if (start == end)
			return;
		tokens.add(new LiteralToken(pattern.substring(start, end)));
	}

	/**
	 * Parses the pattern into a list of tokens.
	 */
	@SuppressWarnings({
		"java:S125", // S125: state-machine/docs comments
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541", // Single-threaded context; synchronization unnecessary
		"java:S1871" // else-branch intentionally duplicates known-conversion branch; semantics differ (valid vs unknown format char)
	})
	private static List<Token> parseTokens(String pattern) {
		var tokens = new ArrayList<Token>();
		var length = pattern.length();
		var i = 0;
		var sequentialIndex = 0; // 0-based index for sequential placeholders

		// Possible String.format variable formats:
		// %[argument_index$][flags][width][.precision]conversion
		// %[argument_index$][flags][width]conversion
		// %[flags][width]conversion

		// S1 - In literal, looking for %
		// S2 - Found %, looking for conversion char or t or T
		var state = S1;

		var mark = 0;
		while (i < length) {
			var ch = pattern.charAt(i++);

			if (state == S1) {
				if (ch == '%') {
					lit(tokens, pattern, mark, i - 1);
					state = S2;
					mark = i;
				}
			} else /* if (state == S2) */ {
				if (ch == '%') {
					tokens.add(new LiteralToken("%"));
					state = S1;
					mark = i;
				} else if (ch == 'n') {
					// %n is special - it doesn't consume an argument, so handle it as a literal token
					tokens.add(new LiteralToken(System.lineSeparator()));
					state = S1;
					mark = i;
				} else if (ch == 't' || ch == 'T') {
					// Do nothing.  Part of 2-character time conversion.
				} else if (PRINTF_CONVERSION_CHARS.contains(ch)) {
					StringFormatToken.create(tokens, pattern, mark, i, sequentialIndex++);
					state = S1;
					mark = i;
				} else if (PRINTF_FORMAT_CHARS.contains(ch) || Character.isDigit(ch)) {
					// Do nothing.
				} else {
					// Unknown character - could be invalid conversion or end of format
					// Create StringFormatToken and let String.format() validate it
					// This allows String.format() to throw IllegalFormatException for invalid conversions like %F
					// printfStart is position after '%', so substring from printfStart-1 (the '%') to i (after the char)
					StringFormatToken.create(tokens, pattern, mark, i, sequentialIndex++);
					state = S1;
					mark = i;
				}
			}
		}

		// Process remaining content based on final state
		if (state == S1) {
			lit(tokens, pattern, mark);
		} else /* if (state == S2) */ {
			// Dangling '%' without conversion - throw exception to match String.format() behavior
			// UnknownFormatConversionException constructor takes just the conversion character
			throw new UnknownFormatConversionException("%");
		}

		return tokens;
	}


	@SuppressWarnings({
		"java:S3398" // sf() is a generic String.format wrapper; keeping it at outer-class scope alongside other utility methods is intentional
	})
	private static String sf(Locale l, String s, Object o) {
		return String.format(l, s, a(o));
	}

	private final String pattern;

	private final Token[] tokens;

	/**
	 * Creates a new printf StringFormat instance.
	 *
	 * <p>
	 * <c>{</c> and <c>'</c> are treated as literal text (genuine {@link java.util.Formatter} semantics); only
	 * printf-style placeholders are recognized.
	 *
	 * @param pattern The format pattern.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk>.
	 */
	public StringFormat(String pattern) {
		this.pattern = assertArgNotNull(ARG_pattern, pattern);
		this.tokens = parseTokens(pattern).toArray(Token[]::new);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof StringFormat o2 && eq(this, o2, (x, y) -> eq(x.pattern, y.pattern));
	}

	/**
	 * Formats the pattern with the given arguments using the specified locale.
	 *
	 * <p>
	 * The locale affects locale-specific printf number formatting (decimal separators, etc.).
	 *
	 * @param locale The locale to use for formatting. If <jk>null</jk>, uses the default locale.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If format specifiers are invalid or arguments don't match.
	 */
	public String format(Locale locale, Object...args) {
		var sb = new StringBuilder(pattern.length() + 64);
		for (var token : tokens) {
			token.append(sb, args, locale);
		}
		return sb.toString();
	}

	/**
	 * Formats the pattern with the given arguments using the default locale.
	 *
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If format specifiers are invalid or arguments don't match.
	 */
	public String format(Object...args) {
		return format(Locale.getDefault(), args);
	}

	@Override
	public int hashCode() {
		return pattern.hashCode();
	}

	/**
	 * Returns a debug representation of the parsed pattern showing the token structure.
	 *
	 * <p>
	 * This method is useful for debugging and understanding how a pattern was parsed.
	 * It returns a string showing each token in the format:
	 * <ul>
	 *   <li><b>Literal tokens:</b> <js>"[L:text]"</js> - Literal text</li>
	 *   <li><b>StringFormat tokens (simple):</b> <js>"[S:s0:%s]"</js> - Simple printf placeholder (format='s', index=0, content)</li>
	 *   <li><b>StringFormat tokens (complex):</b> <js>"[S:z0:%.2f]"</js> - Complex printf placeholder (format='z', index=0, content)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Token Format:</h5>
	 * <ul>
	 *   <li><b>L</b> = Literal token</li>
	 *   <li><b>S</b> = StringFormat (printf) token</li>
	 *   <li><b>Format character:</b> 's' = simple, 'z' = complex printf</li>
	 *   <li><b>Index:</b> 0-based argument index</li>
	 *   <li><b>Content:</b> The format string content (for complex tokens)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	StringFormat <jv>fmt</jv> = StringFormat.<jsm>ofPrintf</jsm>(<js>"Hello %s, you have %d items"</js>);
	 * 	<jv>fmt</jv>.<jsm>toPattern</jsm>();
	 * 	<jc>// Returns: "[L:Hello ][S:s0:%s][L:, you have ][S:d1:%d][L: items]"</jc>
	 * </p>
	 *
	 * @return A debug string showing the parsed token structure.
	 */
	public String toPattern() {
		return Arrays.stream(tokens).map(Object::toString).collect(Collectors.joining());
	}

	@Override
	public String toString() {
		return pattern;
	}
}
