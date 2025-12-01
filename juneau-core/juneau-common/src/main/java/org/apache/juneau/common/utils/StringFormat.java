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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StateEnum.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;
import java.util.*;
import java.util.stream.*;
import java.util.MissingFormatArgumentException;

import org.apache.juneau.common.collections.*;

/**
 * Unified string formatter supporting both MessageFormat-style and printf-style formatting in the same pattern.
 *
 * <p>
 * This class provides a thread-safe, cacheable formatter that can handle mixed format styles in a single pattern.
 * It supports both MessageFormat syntax (<js>"{0}"</js>, <js>"{1,number}"</js>) and printf syntax (<js>"%s"</js>, <js>"%d"</js>)
 * within the same string.
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 *   <li><b>Dual Format Support:</b> Mix MessageFormat and printf-style placeholders in the same pattern</li>
 *   <li><b>Thread-Safe:</b> Immutable class, safe for concurrent use</li>
 *   <li><b>Cacheable:</b> Use {@link #of(String)} for cached instances</li>
 *   <li><b>Argument Sharing:</b> Both format styles share the same argument array</li>
 * </ul>
 *
 * <h5 class='section'>Format Style Detection:</h5>
 * <p>
 * The formatter automatically detects which style to use for each placeholder:
 * <ul>
 *   <li><b>MessageFormat style:</b> <js>"{0}"</js>, <js>"{1,number}"</js>, <js>"{2,date}"</js>, etc.</li>
 *   <li><b>Printf style:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, <js>"%1$s"</js>, etc.</li>
 * </ul>
 *
 * <h5 class='section'>Argument Mapping:</h5>
 * <p>
 * Arguments are processed in order of appearance:
 * <ul>
 *   <li><b>MessageFormat placeholders:</b> Use explicit indices (e.g., <js>"{0}"</js> uses <c>args[0]</c>)</li>
 *   <li><b>Printf placeholders:</b> Use sequential indices starting after the highest MessageFormat index</li>
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Mixed format styles</jc>
 * 	StringFormat <jv>fmt</jv> = StringFormat.<jsm>of</jsm>(<js>"Hello {0}, you have %d items"</js>);
 * 	String <jv>result</jv> = <jv>fmt</jv>.<jsm>format</jsm>(<js>"John"</js>, 5);
 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
 *
 * 	<jc>// MessageFormat with explicit indices, printf with sequential</jc>
 * 	StringFormat <jv>fmt2</jv> = StringFormat.<jsm>of</jsm>(<js>"User {0} has %s and {1} items"</js>);
 * 	String <jv>result2</jv> = <jv>fmt2</jv>.<jsm>format</jsm>(<js>"Alice"</js>, 10, <js>"admin"</js>);
 * 	<jc>// Returns: "User Alice has admin and 10 items"</jc>
 * 	<jc>// {0} -> "Alice", {1} -> 10, %s -> "admin"</jc>
 *
 * 	<jc>// Printf with explicit indices</jc>
 * 	StringFormat <jv>fmt3</jv> = StringFormat.<jsm>of</jsm>(<js>"%1$s loves %2$s, and {0} also loves %3$s"</js>);
 * 	String <jv>result3</jv> = <jv>fmt3</jv>.<jsm>format</jsm>(<js>"Alice"</js>, <js>"Bob"</js>, <js>"Charlie"</js>);
 * 	<jc>// Returns: "Alice loves Bob, and Alice also loves Charlie"</jc>
 * </p>
 *
 * <h5 class='section'>Caching:</h5>
 * <p>
 * Use {@link #of(String)} to get cached instances. The cache is thread-safe and limited to 1000 entries.
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
public final class StringFormat {

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
	 * MessageFormat-style token (e.g., {0}, {1,number}).
	 */
	private static final class MessageFormatToken extends Token {
		private final char format;
		private final String content; // null for simple tokens, normalized pattern like "{0,number}" for complex tokens
		private final int index; // 0-based index
		private final String placeholder; // Original placeholder text like "{0}" or "{0,number}"

		/**
		 * @param content - The variable content such as "{}" or "{0}", or "{0,number}"
		 * 	The content should have the curly-braces already removed so that we're only looking at the inner parts.
		 * @param index - The zero-based index of the variable in the message (used for sequential {} placeholders).
		 */
		MessageFormatToken(String content, int index) {
			if (content.isBlank()) {
				this.content = null;
				this.index = index;
				this.format = 's';
				this.placeholder = "{" + index + "}";
			} else if (content.indexOf(',') == -1) {
				this.content = null;
				this.index = parseIndexMF(content);
				this.format = 's';
				this.placeholder = "{" + this.index + "}";
			} else {
				var tokens = content.split(",", 2);
				this.index = parseIndexMF(tokens[0]);
				this.content = "{0," + tokens[1] + "}";
				this.format = 'o';
				this.placeholder = "{" + this.index + "," + tokens[1] + "}";
			}
		}

		@Override
		public String toString() {
			return "[M:" + format + index + (content == null ? "" : (':' + content)) + "]";
		}

		@Override
		void append(StringBuilder sb, Object[] args, Locale locale) {
			// MessageFormat inserts the placeholder text if argument is missing
			if (args == null || index >= args.length || index < 0) {
				sb.append(placeholder);
				return;
			}
			var o = args[index];
			var l = locale == null ? Locale.getDefault() : locale;
			switch (format) {
				case 's':
					if (o == null) {
						sb.append("null");
					} else if (o instanceof Number o2) {
						sb.append(NUMBER_FORMAT_CACHE.get(l).format(o2));
					} else if (o instanceof Date o2) {
						sb.append(DATE_FORMAT_CACHE.get(l).format(o2));
					} else {
						sb.append(o.toString());
					}
					break;
				default:
					// Use Cache2 with Locale and content as separate keys to avoid string concatenation
					var mf = MESSAGE_FORMAT_CACHE.get(l, content);
					sb.append(mf.format(a(o)));
					break;
			}
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
			var $ = content.indexOf('$');
			if ($ >= 0) {
				index = parseIndexSF(content.substring(0, $)) - 1;
				content = content.substring($ + 1);
			}
			this.format = content.length() == 1 ? content.charAt(content.length() - 1) : 'z';
			this.index = index;
			this.content = "%" + content;
		}

		@Override
		public String toString() {
			return "[S:" + format + index + ":" + content + "]";
		}

		@Override
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

	private static final CacheMode CACHE_MODE = CacheMode.parse(System.getProperty("juneau.StringFormat.caching", "FULL"));

	private static final Cache<String,StringFormat> CACHE = Cache.of(String.class, StringFormat.class).maxSize(1000).cacheMode(CACHE_MODE).build();
	private static final Cache2<Locale,String,MessageFormat> MESSAGE_FORMAT_CACHE = Cache2.of(Locale.class, String.class, MessageFormat.class).maxSize(100).threadLocal().cacheMode(CACHE_MODE)
		.supplier((locale, content) -> new MessageFormat(content, locale)).build();

	private static final Cache<Locale,NumberFormat> NUMBER_FORMAT_CACHE = Cache.of(Locale.class, NumberFormat.class).maxSize(50).threadLocal().cacheMode(CACHE_MODE).supplier(NumberFormat::getInstance).build();

	private static final Cache<Locale,DateFormat> DATE_FORMAT_CACHE = Cache.of(Locale.class, DateFormat.class).maxSize(50).threadLocal().cacheMode(CACHE_MODE)
		.supplier(locale -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)).build();

	private static final AsciiSet PRINTF_CONVERSION_CHARS = AsciiSet.of("bBhHsScCdoxXeEfgGaAtTn%");

	private static final AsciiSet PRINTF_FORMAT_CHARS = AsciiSet.of("-+ 0(#.*$");

	/**
	 * Formats a pattern string with the given arguments using the specified locale.
	 *
	 * <p>
	 * This is a convenience method that creates a StringFormat instance and formats it.
	 * If no arguments are passed in, the pattern is returned as-is.
	 *
	 * @param pattern The format pattern.
	 * @param locale The locale to use for formatting. If <jk>null</jk>, uses the default locale.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk> or format specifiers are invalid.
	 */
	public static String format(String pattern, Locale locale, Object...args) {
		if (args.length == 0)
			return pattern;
		return of(pattern).format(locale, args);
	}

	/**
	 * Formats a pattern string with the given arguments using the default locale.
	 *
	 * <p>
	 * This is a convenience method that creates a StringFormat instance and formats it.
	 * If no arguments are passed in, the pattern is simply returned as-is.
	 *
	 * @param pattern The format pattern.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk> or format specifiers are invalid.
	 */
	public static String format(String pattern, Object...args) {
		if (args.length == 0)
			return pattern;
		return of(pattern).format(args);
	}

	/**
	 * Returns a cached StringFormat instance for the given pattern.
	 *
	 * <p>
	 * This method uses a thread-safe cache to avoid recreating StringFormat instances for the same pattern.
	 * The cache is limited to 1000 entries.
	 *
	 * @param pattern The format pattern.
	 * @return A cached or new StringFormat instance.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk>.
	 */
	public static StringFormat of(String pattern) {
		assertArgNotNull("pattern", pattern);
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

	private static void mf(List<Token> tokens, String pattern, int start, int end, int index) {
		tokens.add(new MessageFormatToken(pattern.substring(start, end), index));
	}

	private static int parseIndexMF(String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new IllegalArgumentException("can't parse argument number: " + s);
		}
	}

	private static int parseIndexSF(String s) {
		return Integer.parseInt(s.trim());
	}

	/**
	 * Parses the pattern into a list of tokens.
	 */
	private static List<Token> parseTokens(String pattern) {
		var tokens = new ArrayList<Token>();
		var length = pattern.length();
		var i = 0;
		var sequentialIndex = 0; // 0-based index for sequential placeholders

		// Possible String.format variable formats:
		// %[argument_index$][flags][width][.precision]conversion
		// %[argument_index$][flags][width]conversion
		// %[flags][width]conversion

		// Possible MessageFormat variable formats:
		// {}
		// {#,formatType}
		// {#,formatType,formatStyle}

		// S1 - In literal, looking for %, {, or '
		// S2 - Found %, looking for conversion char or t or T
		// S3 - Found {, looking for }
		// S4 - Found ', in quoted section (MessageFormat single quotes escape special chars), looking for '
		var state = S1;

		var nestedBracketDepth = 0;

		var mark = 0;
		while (i < length) {
			var ch = pattern.charAt(i++);

			if (state == S1) {
				if (ch == '%') {
					lit(tokens, pattern, mark, i - 1);
					state = S2;
					mark = i;
				} else if (ch == '{') {
					lit(tokens, pattern, mark, i - 1);
					state = S3;
					mark = i - 1;
				} else if (ch == '\'') {
					lit(tokens, pattern, mark, i - 1);
					state = S4;
					mark = i;
				}
			} else if (state == S2) {
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
					sf(tokens, pattern, mark, i, sequentialIndex++);
					state = S1;
					mark = i;
				} else if (PRINTF_FORMAT_CHARS.contains(ch) || Character.isDigit(ch)) {
					// Do nothing.
				} else {
					// Unknown character - could be invalid conversion or end of format
					// Create StringFormatToken and let String.format() validate it
					// This allows String.format() to throw IllegalFormatException for invalid conversions like %F
					// printfStart is position after '%', so substring from printfStart-1 (the '%') to i (after the char)
					sf(tokens, pattern, mark, i, sequentialIndex++);
					state = S1;
					mark = i;
				}
			} else if (state == S3) {
				if (ch == '{') {
					nestedBracketDepth++;
				} else if (ch == '}') {
					if (nestedBracketDepth > 0) {
						nestedBracketDepth--;
					} else {
						mf(tokens, pattern, mark + 1, i - 1, sequentialIndex++);
						state = S1;
						mark = i;
					}
				}
			} else /* if (state == S4) */ {
				if (ch == '\'') {
					if (mark == i - 1) {
						lit(tokens, pattern, mark, i);  // '' becomes '
						state = S1;
						mark = i;
					} else {
						lit(tokens, pattern, mark, i - 1);
						state = S1;
						mark = i;
					}
				}
			}
		}

		// Process remaining content based on final state
		if (state == S1) {
			lit(tokens, pattern, mark);
		} else if (state == S2) {
			// Dangling '%' without conversion - throw exception to match String.format() behavior
			// UnknownFormatConversionException constructor takes just the conversion character
			throw new java.util.UnknownFormatConversionException("%");
		} else if (state == S3) {
			// Unmatched '{' - throw exception to match MessageFormat behavior
			throw new IllegalArgumentException("Unmatched braces in the pattern.");
		} else /* if (state == S4) */ {
			// Unmatched quote - MessageFormat treats it as ending the quoted section
			// Add the quoted content as literal (from mark to end of pattern)
			lit(tokens, pattern, mark);
		}

		return tokens;
	}

	private static void sf(List<Token> tokens, String pattern, int start, int end, int index) {
		tokens.add(new StringFormatToken(pattern.substring(start, end), index));
	}

	private static String sf(Locale l, String s, Object o) {
		return String.format(l, s, a(o));
	}

	private final String pattern;

	private final Token[] tokens;

	/**
	 * Creates a new StringFormat instance.
	 *
	 * @param pattern The format pattern. Can contain both MessageFormat and printf-style placeholders.
	 * @throws IllegalArgumentException If the pattern is <jk>null</jk>.
	 */
	public StringFormat(String pattern) {
		this.pattern = assertArgNotNull("pattern", pattern);
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
	 * The locale affects both MessageFormat and printf-style formatting:
	 * <ul>
	 *   <li><b>MessageFormat:</b> Locale-specific number, date, and time formatting</li>
	 *   <li><b>Printf:</b> Locale-specific number formatting (decimal separators, etc.)</li>
	 * </ul>
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
	 *   <li><b>MessageFormat tokens (simple):</b> <js>"[M:s0]"</js> - Simple MessageFormat placeholder (format='s', index=0)</li>
	 *   <li><b>MessageFormat tokens (complex):</b> <js>"[M:o0:{0,number,currency}]"</js> - Complex MessageFormat placeholder (format='o', index=0, content)</li>
	 *   <li><b>StringFormat tokens (simple):</b> <js>"[S:s0:%s]"</js> - Simple printf placeholder (format='s', index=0, content)</li>
	 *   <li><b>StringFormat tokens (complex):</b> <js>"[S:z0:%.2f]"</js> - Complex printf placeholder (format='z', index=0, content)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Token Format:</h5>
	 * <ul>
	 *   <li><b>L</b> = Literal token</li>
	 *   <li><b>M</b> = MessageFormat token</li>
	 *   <li><b>S</b> = StringFormat (printf) token</li>
	 *   <li><b>Format character:</b> 's' = simple, 'o' = other/complex, 'z' = complex printf</li>
	 *   <li><b>Index:</b> 0-based argument index</li>
	 *   <li><b>Content:</b> The format string content (for complex tokens)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	StringFormat <jv>fmt</jv> = StringFormat.<jsm>of</jsm>(<js>"Hello {0}, you have %d items"</js>);
	 * 	<jv>fmt</jv>.<jsm>toPattern</jsm>();
	 * 	<jc>// Returns: "[L:Hello ][M:s0][L:, you have ][S:d1:%d][L: items]"</jc>
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
