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

import static java.lang.Character.*;
import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.StateEnum.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.nio.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import org.apache.juneau.common.collections.*;

/**
 * Reusable string utility methods.
 */
public class StringUtils {

	/**
	 * Predicate check to filter out null and empty strings.
	 */
	public static final Predicate<String> NOT_EMPTY = Utils::isNotEmpty;

	/**
	 * Thread-local cache of MessageFormat objects for improved performance.
	 *
	 * <p>MessageFormat objects are not thread-safe, so we use a ThreadLocal cache
	 * to ensure each thread has its own set of cached formatters. This avoids:
	 * <ul>
	 *   <li>Repeated parsing of the same patterns</li>
	 *   <li>Thread synchronization overhead</li>
	 *   <li>Object allocation for frequently used patterns</li>
	 * </ul>
	 */
	private static final ThreadLocal<Cache<String,MessageFormat>> MESSAGE_FORMAT_CACHE = ThreadLocal.withInitial(() -> Cache.of(String.class, MessageFormat.class).maxSize(100).build());

	private static final AsciiSet numberChars = AsciiSet.of("-xX.+-#pP0123456789abcdefABCDEF");

	private static final AsciiSet firstNumberChars = AsciiSet.of("+-.#0123456789");
	private static final AsciiSet octChars = AsciiSet.of("01234567");
	private static final AsciiSet decChars = AsciiSet.of("0123456789");
	private static final AsciiSet hexChars = AsciiSet.of("0123456789abcdefABCDEF");
	// Maps 6-bit nibbles to BASE64 characters.
	private static final char[] base64m1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	// Characters that do not need to be URL-encoded
	private static final AsciiSet unencodedChars = AsciiSet.create().ranges("a-z", "A-Z", "0-9").chars("-_.!~*'()\\").build();

	// Characters that really do not need to be URL-encoded
	private static final AsciiSet unencodedCharsLax = unencodedChars.copy().chars(":@$,")  // reserved, but can't be confused in a query parameter.
		.chars("{}|\\^[]`")  // unwise characters.
		.build();

	// Valid HTTP header characters (including quoted strings and comments).
	// @formatter:off
	private static final AsciiSet httpHeaderChars = AsciiSet
		.create()
		.chars("\t -")
		.ranges("!-[","]-}")
		.build();
	// @formatter:on

	// Maps BASE64 characters to 6-bit nibbles.
	private static final byte[] base64m2 = new byte[128];

	static {
		for (var i = 0; i < 64; i++)
			base64m2[base64m1[i]] = (byte)i;
	}
	private static final Random RANDOM = new Random();

	private static final Pattern fpRegex = Pattern.compile(
		"[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*"  // NOSONAR
	);

	static final Map<Character,AsciiSet> ESCAPE_SETS = new ConcurrentHashMap<>();

	static final AsciiSet MAP_ESCAPE_SET = AsciiSet.of(",=\\");

	static final AsciiSet QUOTE_ESCAPE_SET = AsciiSet.of("\"'\\");

	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	private static final Map<Class<?>,Function<Object,String>> PRIMITIVE_ARRAY_STRINGIFIERS = new HashMap<>();

	static {
		PRIMITIVE_ARRAY_STRINGIFIERS.put(boolean[].class, x -> Arrays.toString((boolean[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(byte[].class, x -> Arrays.toString((byte[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(char[].class, x -> Arrays.toString((char[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(double[].class, x -> Arrays.toString((double[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(float[].class, x -> Arrays.toString((float[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(int[].class, x -> Arrays.toString((int[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(long[].class, x -> Arrays.toString((long[])x));
		PRIMITIVE_ARRAY_STRINGIFIERS.put(short[].class, x -> Arrays.toString((short[])x));
	}

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	private static final AsciiSet URL_ENCODE_PATHINFO_VALIDCHARS = AsciiSet.create().ranges("a-z", "A-Z", "0-9").chars("-_.*/()").build();
	private static final AsciiSet URI_CHARS = AsciiSet.create().chars("?#+%;/:@&=+$,-_.!~*'()").range('0', '9').range('A', 'Z').range('a', 'z').build();

	/**
	 * Abbreviates a String using ellipses.
	 *
	 * @param in The input string.
	 * @param length The max length of the resulting string.
	 * @return The abbreviated string.
	 */
	public static String abbreviate(String in, int length) {
		if (in == null || in.length() <= length || in.length() <= 3)
			return in;
		return in.substring(0, length - 3) + "...";
	}

	/**
	 * BASE64-decodes the specified string.
	 *
	 * @param in The BASE-64 encoded string.
	 * @return The decoded byte array, or null if the input was <jk>null</jk>.
	 */
	public static byte[] base64Decode(String in) {
		if (in == null)
			return null;  // NOSONAR - Intentional.

		var bIn = in.getBytes(UTF8);

		assertArg(bIn.length % 4 == 0, "Invalid BASE64 string length.  Must be multiple of 4.");

		// Strip out any trailing '=' filler characters.
		var inLength = bIn.length;
		while (inLength > 0 && bIn[inLength - 1] == '=')
			inLength--;

		var outLength = (inLength * 3) / 4;
		var out = new byte[outLength];
		var iIn = 0;
		var iOut = 0;
		while (iIn < inLength) {
			var i0 = bIn[iIn++];
			var i1 = bIn[iIn++];
			var i2 = iIn < inLength ? bIn[iIn++] : 'A';
			var i3 = iIn < inLength ? bIn[iIn++] : 'A';
			var b0 = base64m2[i0];
			var b1 = base64m2[i1];
			var b2 = base64m2[i2];
			int b3 = base64m2[i3];
			var o0 = (b0 << 2) | (b1 >>> 4);
			var o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
			var o2 = ((b2 & 3) << 6) | b3;
			out[iOut++] = (byte)o0;
			if (iOut < outLength)
				out[iOut++] = (byte)o1;
			if (iOut < outLength)
				out[iOut++] = (byte)o2;
		}
		return out;
	}

	/**
	 * Shortcut for calling <c>base64Decode(String)</c> and converting the result to a UTF-8 encoded string.
	 *
	 * @param in The BASE-64 encoded string to decode.
	 * @return The decoded string.
	 */
	public static String base64DecodeToString(String in) {
		var b = base64Decode(in);
		if (b == null)
			return null;
		return new String(b, UTF8);
	}

	/**
	 * BASE64-encodes the specified byte array.
	 *
	 * @param in The input byte array to convert.
	 * @return The byte array converted to a BASE-64 encoded string.
	 */
	public static String base64Encode(byte[] in) {
		if (in == null)
			return null;
		var outLength = (in.length * 4 + 2) / 3;   // Output length without padding
		var out = new char[((in.length + 2) / 3) * 4];  // Length includes padding.
		var iIn = 0;
		var iOut = 0;
		while (iIn < in.length) {
			var i0 = in[iIn++] & 0xff;
			var i1 = iIn < in.length ? in[iIn++] & 0xff : 0;
			var i2 = iIn < in.length ? in[iIn++] & 0xff : 0;
			var o0 = i0 >>> 2;
			var o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			var o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			var o3 = i2 & 0x3F;
			out[iOut++] = base64m1[o0];
			out[iOut++] = base64m1[o1];
			out[iOut] = iOut < outLength ? base64m1[o2] : '=';
			iOut++;
			out[iOut] = iOut < outLength ? base64m1[o3] : '=';
			iOut++;
		}
		return new String(out);
	}

	/**
	 * Shortcut for calling <code>base64Encode(in.getBytes(<js>"UTF-8"</js>))</code>
	 *
	 * @param in The input string to convert.
	 * @return The string converted to BASE-64 encoding.
	 */
	public static String base64EncodeToString(String in) {
		if (in == null)
			return null;
		return base64Encode(in.getBytes(UTF8));
	}

	/**
	 * Capitalizes the first character of a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	capitalize(<jk>null</jk>);          <jc>// null</jc>
	 * 	capitalize(<js>""</js>);            <jc>// ""</jc>
	 * 	capitalize(<js>"hello"</js>);       <jc>// "Hello"</jc>
	 * 	capitalize(<js>"Hello"</js>);       <jc>// "Hello"</jc>
	 * 	capitalize(<js>"HELLO"</js>);       <jc>// "HELLO"</jc>
	 * </p>
	 *
	 * @param str The string to capitalize.
	 * @return The string with the first character capitalized, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String capitalize(String str) {
		if (isEmpty(str))
			return str;
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Returns the character at the specified index in the string without throwing exceptions.
	 *
	 * @param s The string.
	 * @param i The index position.
	 * @return
	 * 	The character at the specified index, or <c>0</c> if the index is out-of-range or the string
	 * 	is <jk>null</jk>.
	 */
	public static char charAt(String s, int i) {
		if (s == null || i < 0 || i >= s.length())
			return 0;
		return s.charAt(i);
	}

	/**
	 * Cleans a string by removing control characters and normalizing whitespace.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	clean(<js>"hello\u0000\u0001world"</js>);     <jc>// "hello world"</jc>
	 * 	clean(<js>"hello  \t\n  world"</js>);         <jc>// "hello world"</jc>
	 * </p>
	 *
	 * @param str The string to clean.
	 * @return The cleaned string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String clean(String str) {
		if (str == null)
			return null;
		str = removeControlChars(str);
		return normalizeWhitespace(str);
	}

	/**
	 * Compares two strings, but gracefully handles <jk>nulls</jk>.
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return The same as {@link String#compareTo(String)}.
	 */
	public static int compare(String s1, String s2) {
		if (s1 == null && s2 == null)
			return 0;
		if (s1 == null)
			return Integer.MIN_VALUE;
		if (s2 == null)
			return Integer.MAX_VALUE;
		return s1.compareTo(s2);
	}

	/**
	 * Converts string into a GZipped input stream.
	 *
	 * @param contents The contents to compress.
	 * @return The input stream converted to GZip.
	 * @throws Exception Exception occurred.
	 */
	public static byte[] compress(String contents) throws Exception {
		var baos = new ByteArrayOutputStream(contents.length() >> 1);
		try (var gos = new GZIPOutputStream(baos)) {
			gos.write(contents.getBytes());
			gos.finish();
			gos.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 *
	 * @param s The string to check.
	 * @param values The characters to check for.
	 * @return <jk>true</jk> if the string contains any of the specified characters.
	 */
	public static boolean contains(String s, char...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (var v : values) {
			if (s.indexOf(v) >= 0)
				return true;
		}
		return false;
	}

	/**
	 * Same as {@link String#contains(CharSequence)} except returns <jk>null</jk> if the value is null.
	 *
	 * @param value The string to check.
	 * @param substring The value to check for.
	 * @return <jk>true</jk> if the value contains the specified substring.
	 */
	public static boolean contains(String value, CharSequence substring) {
		return nn(value) && value.contains(substring);
	}

	/**
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 *
	 * @param s The string to check.
	 * @param values The substrings to check for.
	 * @return <jk>true</jk> if the string contains any of the specified substrings.
	 */
	public static boolean contains(String s, String...values) {
		if (s == null || values == null || values.length == 0)
			return false;
		for (var v : values) {
			if (s.contains(v))
				return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified string contains any of the specified characters.
	 *
	 * @param s The string to test.
	 * @param chars The characters to look for.
	 * @return
	 * 	<jk>true</jk> if the specified string contains any of the specified characters.
	 * 	<br><jk>false</jk> if the string is <jk>null</jk>.
	 */
	public static boolean containsAny(String s, char...chars) {
		if (s == null)
			return false;
		for (int i = 0, j = s.length(); i < j; i++) {
			var c = s.charAt(i);
			for (var c2 : chars)
				if (c == c2)
					return true;
		}
		return false;
	}

	/**
	 * Counts the number of the specified character in the specified string.
	 *
	 * @param s The string to check.
	 * @param c The character to check for.
	 * @return The number of those characters or zero if the string was <jk>null</jk>.
	 */
	public static int countChars(String s, char c) {
		var count = 0;
		if (s == null)
			return count;
		for (var i = 0; i < s.length(); i++)
			if (s.charAt(i) == c)
				count++;
		return count;
	}

	/**
	 * Debug method for rendering non-ASCII character sequences.
	 *
	 * @param s The string to decode.
	 * @return A string with non-ASCII characters converted to <js>"[hex]"</js> sequences.
	 */
	public static String decodeHex(String s) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		for (var c : s.toCharArray()) {
			if (c < ' ' || c > '~')
				sb.append("[").append(Integer.toHexString(c)).append("]");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Converts a GZipped input stream into a string.
	 *
	 * @param is The contents to decompress.
	 * @return The string.
	 * @throws Exception Exception occurred.
	 */
	public static String decompress(byte[] is) throws Exception {
		return read(new GZIPInputStream(new ByteArrayInputStream(is)));
	}

	/**
	 * Finds the position where the two strings differ.
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return The position where the two strings differ, or <c>-1</c> if they're equal.
	 */
	public static int diffPosition(String s1, String s2) {
		s1 = emptyIfNull(s1);
		s2 = emptyIfNull(s2);
		var i = 0;
		var len = Math.min(s1.length(), s2.length());
		while (i < len) {
			var j = s1.charAt(i) - s2.charAt(i);
			if (j != 0)
				return i;
			i++;
		}
		if (i == len && s1.length() == s2.length())
			return -1;
		return i;
	}

	/**
	 * Finds the position where the two strings differ ignoring case.
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return The position where the two strings differ, or <c>-1</c> if they're equal.
	 */
	public static int diffPositionIc(String s1, String s2) {
		s1 = emptyIfNull(s1);
		s2 = emptyIfNull(s2);
		var i = 0;
		var len = Math.min(s1.length(), s2.length());
		while (i < len) {
			var j = toLowerCase(s1.charAt(i)) - toLowerCase(s2.charAt(i));
			if (j != 0)
				return i;
			i++;
		}
		if (i == len && s1.length() == s2.length())
			return -1;
		return i;
	}

	/**
	 * An efficient method for checking if a string ends with a character.
	 *
	 * @param s The string to check.  Can be <jk>null</jk>.
	 * @param c The character to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and ends with the specified character.
	 */
	public static boolean endsWith(String s, char c) {
		if (nn(s)) {
			var i = s.length();
			if (i > 0)
				return s.charAt(i - 1) == c;
		}
		return false;
	}

	/**
	 * Same as {@link #endsWith(String, char)} except check for multiple characters.
	 *
	 * @param s The string to check.  Can be <jk>null</jk>.
	 * @param c The characters to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and ends with the specified character.
	 */
	public static boolean endsWith(String s, char...c) {
		if (nn(s)) {
			var i = s.length();
			if (i > 0) {
				var c2 = s.charAt(i - 1);
				for (var cc : c)
					if (c2 == cc)
						return true;
			}
		}
		return false;
	}

	/**
	 * Escapes the specified characters in the string.
	 *
	 * @param s The string with characters to escape.
	 * @param escaped The characters to escape.
	 * @return The string with characters escaped, or the same string if no escapable characters were found.
	 */
	public static String escapeChars(String s, AsciiSet escaped) {
		if (s == null || s.isEmpty())
			return s;

		var count = 0;
		for (var i = 0; i < s.length(); i++)
			if (escaped.contains(s.charAt(i)))
				count++;
		if (count == 0)
			return s;

		var sb = new StringBuffer(s.length() + count);
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (escaped.contains(c))
				sb.append('\\');
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Escapes a string for safe inclusion in Java source code.
	 *
	 * <p>This method converts special characters to their Java escape sequences and
	 * converts non-printable ASCII characters to Unicode escape sequences.
	 *
	 * <h5 class='section'>Escape mappings:</h5>
	 * <ul>
	 *   <li>{@code "} → {@code \"}</li>
	 *   <li>{@code \} → {@code \\}</li>
	 *   <li>{@code \n} → {@code \\n}</li>
	 *   <li>{@code \r} → {@code \\r}</li>
	 *   <li>{@code \t} → {@code \\t}</li>
	 *   <li>{@code \f} → {@code \\f}</li>
	 *   <li>{@code \b} → {@code \\b}</li>
	 *   <li>Non-printable characters → {@code \\uXXXX}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>escaped</jv> = <jsm>escapeForJava</jsm>(<js>"Hello\nWorld\"Test\""</js>);
	 *   <jc>// Returns: "Hello\\nWorld\\\"Test\\\""</jc>
	 * </p>
	 *
	 * @param s The string to escape.
	 * @return The escaped string safe for Java source code, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String escapeForJava(String s) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		for (var c : s.toCharArray()) {
			sb.append(switch (c) {
				case '\"' -> "\\\"";
				case '\\' -> "\\\\";
				case '\n' -> "\\n";
				case '\r' -> "\\r";
				case '\t' -> "\\t";
				case '\f' -> "\\f";
				case '\b' -> "\\b";
				default -> {
					if (c < 0x20 || c > 0x7E)
						yield String.format("\\u%04x", (int)c);
					yield String.valueOf(c);
				}
			});
		}
		return sb.toString();
	}

	/**
	 * Returns the first character in the specified string.
	 *
	 * @param s The string to check.
	 * @return The first character in the string, or <c>0</c> if the string is <jk>null</jk> or empty.
	 */
	public static char firstChar(String s) {
		if (s == null || s.isEmpty())
			return 0;
		return s.charAt(0);
	}

	/**
	 * Returns the first non-null, non-empty string in the list.
	 *
	 * @param s The strings to test.
	 * @return The first non-empty string in the list, or <jk>null</jk> if they were all <jk>null</jk> or empty.
	 */
	public static String firstNonEmpty(String...s) {
		for (var ss : s)
			if (isNotEmpty(ss))
				return ss;
		return null;
	}

	/**
	 * Returns the first non-whitespace character in the string.
	 *
	 * @param s The string to check.
	 * @return
	 * 	The first non-whitespace character, or <c>0</c> if the string is <jk>null</jk>, empty, or composed
	 * 	of only whitespace.
	 */
	public static char firstNonWhitespaceChar(String s) {
		if (nn(s))
			for (var i = 0; i < s.length(); i++)
				if (! isWhitespace(s.charAt(i)))
					return s.charAt(i);
		return 0;
	}

	/**
	 * Attempts to escape any invalid characters found in a URI.
	 *
	 * @param in The URI to fix.
	 * @return The fixed URI.
	 */
	@SuppressWarnings("null")
	public static String fixUrl(String in) {

		if (in == null)
			return null;

		StringBuilder sb = null;

		var m = 0;

		for (var i = 0; i < in.length(); i++) {
			var c = in.charAt(i);
			if (c <= 127 && ! URI_CHARS.contains(c)) {
				sb = append(sb, in.substring(m, i));
				if (c == ' ')
					sb.append("+");
				else
					sb.append('%').append(toHex2(c));
				m = i + 1;
			}
		}
		if (nn(sb)) {
			sb.append(in.substring(m));
			return sb.toString();
		}
		return in;

	}

	/**
	 * Similar to {@link MessageFormat#format(String, Object...)} except allows you to specify POJO arguments.
	 *
	 * <p>This method uses a thread-local cache of {@link MessageFormat} objects for improved performance
	 * when the same patterns are used repeatedly. The cache is limited to 100 entries per thread to prevent
	 * unbounded growth.
	 *
	 * <p>For arguments with format types (e.g., {@code {0,number,#.##}}), the original argument is preserved
	 * to allow proper formatting. For simple placeholders (e.g., {@code {0}}), arguments are converted to
	 * readable strings using {@link #convertToReadable(Object)}.
	 *
	 * @param pattern The string pattern.
	 * @param args The arguments.
	 * @return The formatted string.
	 */
	public static String format(String pattern, Object...args) {
		if (args == null || args.length == 0)
			return pattern;

		var c = countChars(pattern, '\'');
		if (c % 2 != 0)
			throw new AssertionError("Dangling single quote found in pattern: " + pattern);

		// Get or create a cached MessageFormat for this pattern (thread-safe via ThreadLocal)
		var cache = MESSAGE_FORMAT_CACHE.get();
		var mf = cache.get(pattern, () -> new MessageFormat(pattern));

		// Determine which arguments have format types and need to preserve their original type
		var formats = mf.getFormatsByArgumentIndex();

		var args2 = new Object[args.length];
		for (var i = 0; i < args.length; i++) {
			// If there's a Format specified for this index, keep the original argument
			// Otherwise, convert to readable string for better output
			var hasFormat = i < formats.length && nn(formats[i]);
			args2[i] = hasFormat ? args[i] : convertToReadable(args[i]);
		}

		return mf.format(args2);
	}

	/**
	 * Converts a hexadecimal character string to a byte array.
	 *
	 * @param hex The string to convert to a byte array.
	 * @return A new byte array.
	 */
	public static byte[] fromHex(String hex) {
		var buff = ByteBuffer.allocate(hex.length() / 2);
		for (var i = 0; i < hex.length(); i += 2)
			buff.put((byte)Integer.parseInt(hex.substring(i, i + 2), 16));
		buff.rewind();
		return buff.array();
	}

	/**
	 * Converts a hexadecimal byte stream (e.g. "34A5BC") into a UTF-8 encoded string.
	 *
	 * @param hex The hexadecimal string.
	 * @return The UTF-8 string.
	 */
	public static String fromHexToUTF8(String hex) {
		var buff = ByteBuffer.allocate(hex.length() / 2);
		for (var i = 0; i < hex.length(); i += 2)
			buff.put((byte)Integer.parseInt(hex.substring(i, i + 2), 16));
		buff.rewind();  // Fixes Java 11 issue.
		return UTF_8.decode(buff).toString();
	}

	/**
	 * Same as {@link #fromHex(String)} except expects spaces between the byte strings.
	 *
	 * @param hex The string to convert to a byte array.
	 * @return A new byte array.
	 */
	public static byte[] fromSpacedHex(String hex) {
		var buff = ByteBuffer.allocate((hex.length() + 1) / 3);
		for (var i = 0; i < hex.length(); i += 3)
			buff.put((byte)Integer.parseInt(hex.substring(i, i + 2), 16));
		buff.rewind();
		return buff.array();
	}

	/**
	 * Converts a space-deliminted hexadecimal byte stream (e.g. "34 A5 BC") into a UTF-8 encoded string.
	 *
	 * @param hex The hexadecimal string.
	 * @return The UTF-8 string.
	 */
	public static String fromSpacedHexToUTF8(String hex) {
		var buff = ByteBuffer.allocate((hex.length() + 1) / 3);
		for (var i = 0; i < hex.length(); i += 3)
			buff.put((byte)Integer.parseInt(hex.substring(i, i + 2), 16));
		buff.rewind();  // Fixes Java 11 issue.
		return UTF_8.decode(buff).toString();
	}

	/**
	 * Given an absolute URI, returns just the authority portion (e.g. <js>"http://hostname:port"</js>)
	 *
	 * @param s The URI string.
	 * @return Just the authority portion of the URI.
	 */
	public static String getAuthorityUri(String s) {  // NOSONAR - False positive.

		// Use a state machine for maximum performance.

		// S1: Looking for http
		// S2: Found http, looking for :
		// S3: Found :, looking for /
		// S4: Found /, looking for /
		// S5: Found /, looking for x
		// S6: Found x, looking for /

		var state = S1;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (state == S1) {
				if (c >= 'a' && c <= 'z')
					state = S2;
				else
					return s;
			} else if (state == S2) {
				if (c == ':')
					state = S3;
				else if (c < 'a' || c > 'z')
					return s;
			} else if (state == S3) {  // NOSONAR - False positive.
				if (c == '/')
					state = S4;
				else
					return s;
			} else if (state == S4) {
				if (c == '/')
					state = S5;
				else
					return s;
			} else if (state == S5) {
				if (c != '/')
					state = S6;
				else
					return s;
			} else if (state == S6) {
				if (c == '/')  // NOSONAR - Intentional.
					return s.substring(0, i);
			}
		}

		return s;
	}

	/**
	 * Parses a duration string.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"1000"</js> - 1000 milliseconds.
	 * 	<li><js>"10s"</js> - 10 seconds.
	 * 	<li><js>"10 sec"</js> - 10 seconds.
	 * 	<li><js>"10 seconds"</js> - 10 seconds.
	 * </ul>
	 *
	 * <p>
	 * Use any of the following suffixes:
	 * <ul>
	 * 	<li>None (time in milliseconds).
	 * 	<li><js>"s"</js>/<js>"sec"</js>/<js>"second"</js>/<js>"seconds"</js>
	 * 	<li><js>"m"</js>/<js>"min"</js>/<js>"minutes"</js>/<js>"seconds"</js>
	 * 	<li><js>"h"</js>/<js>"hour"</js>/<js>"hours"</js>
	 * 	<li><js>"d"</js>/<js>"day"</js>/<js>"days"</js>
	 * 	<li><js>"w"</js>/<js>"week"</js>/<js>"weeks"</js>
	 * </ul>
	 *
	 * <p>
	 * Suffixes are case-insensitive.
	 * <br>Whitespace is ignored.
	 *
	 * @param s The string to parse.
	 * @return
	 * 	The time in milliseconds, or <c>-1</c> if the string is empty or <jk>null</jk>.
	 */
	public static long getDuration(String s) {
		s = trim(s);
		if (isEmpty(s))
			return -1;
		int i;
		for (i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < '0' || c > '9')
				break;
		}
		long l;
		if (i == s.length())
			l = Long.parseLong(s);
		else {
			l = Long.parseLong(s.substring(0, i).trim());
			var r = s.substring(i).trim().toLowerCase();
			if (r.startsWith("s"))
				l *= 1000;
			else if (r.startsWith("m"))
				l *= 1000 * 60;
			else if (r.startsWith("h"))
				l *= 1000 * 60 * 60;
			else if (r.startsWith("d"))
				l *= 1000 * 60 * 60 * 24;
			else if (r.startsWith("w"))
				l *= 1000 * 60 * 60 * 24 * 7;
		}
		return l;
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern(String s) {
		return getMatchPattern(s, 0);
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @param flags Regular expression flags.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern(String s, int flags) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		sb.append("\\Q");
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '*')
				sb.append("\\E").append(".*").append("\\Q");
			else if (c == '?')
				sb.append("\\E").append(".").append("\\Q");
			else
				sb.append(c);
		}
		sb.append("\\E");
		return Pattern.compile(sb.toString(), flags);
	}

	/**
	 * Takes in a string, splits it by lines, and then prepends each line with line numbers.
	 *
	 * @param s The string.
	 * @return The string with line numbers added.
	 */
	public static String getNumberedLines(String s) {
		return getNumberedLines(s, 1, Integer.MAX_VALUE);
	}

	/**
	 * Same as {@link #getNumberedLines(String)} except only returns the specified lines.
	 *
	 * <p>
	 * Out-of-bounds values are allowed and fixed.
	 *
	 * @param s The string.
	 * @param start The starting line (1-indexed).
	 * @param end The ending line (1-indexed).
	 * @return The string with line numbers added.
	 */
	public static String getNumberedLines(String s, int start, int end) {
		if (s == null)
			return null;
		var lines = s.split("[\r\n]+");
		var digits = String.valueOf(lines.length).length();
		if (start < 1)
			start = 1;
		if (end < 0)
			end = Integer.MAX_VALUE;
		if (end > lines.length)
			end = lines.length;
		var sb = new StringBuilder();
		for (var l : l(lines).subList(start - 1, end))
			sb.append(String.format("%0" + digits + "d", start++)).append(": ").append(l).append("\n");  // NOSONAR - Intentional.
		return sb.toString();
	}

	/**
	 * Checks if a string has text (not null, not empty, and contains at least one non-whitespace character).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	hasText(<jk>null</jk>);       <jc>// false</jc>
	 * 	hasText(<js>""</js>);         <jc>// false</jc>
	 * 	hasText(<js>"   "</js>);      <jc>// false</jc>
	 * 	hasText(<js>"hello"</js>);    <jc>// true</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null, not empty, and contains at least one non-whitespace character.
	 */
	public static boolean hasText(String str) {
		return isNotBlank(str);
	}

	/**
	 * Same as {@link String#indexOf(int)} except allows you to check for multiple characters.
	 *
	 * @param s The string to check.
	 * @param c The characters to check for.
	 * @return The index into the string that is one of the specified characters.
	 */
	public static int indexOf(String s, char...c) {
		if (s == null)
			return -1;
		for (var i = 0; i < s.length(); i++) {
			var c2 = s.charAt(i);
			for (var cc : c)
				if (c2 == cc)
					return i;
		}
		return -1;
	}

	/**
	 * Efficiently determines whether a URL is of the pattern "xxx://xxx"
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if it's an absolute path.
	 */
	public static boolean isAbsoluteUri(String s) {  // NOSONAR - False positive.

		if (isEmpty(s))
			return false;

		// Use a state machine for maximum performance.

		// S1: Looking for http
		// S2: Found http, looking for :
		// S3: Found :, looking for /
		// S4: Found /, looking for /
		// S5: Found /, looking for x

		var state = S1;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (state == S1) {
				if (c >= 'a' && c <= 'z')
					state = S2;
				else
					return false;
			} else if (state == S2) {
				if (c == ':')
					state = S3;
				else if (c < 'a' || c > 'z')
					return false;
			} else if (state == S3) {  // NOSONAR - False positive.
				if (c == '/')
					state = S4;
				else
					return false;
			} else if (state == S4) {
				if (c == '/')
					state = S5;
				else
					return false;
			} else if (state == S5) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if a string contains only alphabetic characters (a-z, A-Z).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAlpha(<jk>null</jk>);         <jc>// false</jc>
	 * 	isAlpha(<js>""</js>);           <jc>// false</jc>
	 * 	isAlpha(<js>"abc"</js>);        <jc>// true</jc>
	 * 	isAlpha(<js>"abc123"</js>);     <jc>// false</jc>
	 * 	isAlpha(<js>"abc def"</js>);    <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null, not empty, and contains only alphabetic characters.
	 */
	public static boolean isAlpha(String str) {
		if (isEmpty(str))
			return false;
		for (var i = 0; i < str.length(); i++) {
			if (! Character.isLetter(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Checks if a string contains only alphanumeric characters (a-z, A-Z, 0-9).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAlphaNumeric(<jk>null</jk>);         <jc>// false</jc>
	 * 	isAlphaNumeric(<js>""</js>);           <jc>// false</jc>
	 * 	isAlphaNumeric(<js>"abc"</js>);        <jc>// true</jc>
	 * 	isAlphaNumeric(<js>"abc123"</js>);     <jc>// true</jc>
	 * 	isAlphaNumeric(<js>"abc def"</js>);    <jc>// false</jc>
	 * 	isAlphaNumeric(<js>"abc-123"</js>);    <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null, not empty, and contains only alphanumeric characters.
	 */
	public static boolean isAlphaNumeric(String str) {
		if (isEmpty(str))
			return false;
		for (var i = 0; i < str.length(); i++) {
			if (! Character.isLetterOrDigit(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Checks if a string is blank (null, empty, or whitespace only).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isBlank(<jk>null</jk>);       <jc>// true</jc>
	 * 	isBlank(<js>""</js>);         <jc>// true</jc>
	 * 	isBlank(<js>"   "</js>);      <jc>// true</jc>
	 * 	isBlank(<js>"hello"</js>);    <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is null, empty, or contains only whitespace characters.
	 */
	public static boolean isBlank(CharSequence str) {
		return str == null || str.toString().isBlank();
	}

	/**
	 * Returns <jk>true</jk> if the specified string is numeric.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if the specified string is numeric.
	 */
	public static boolean isDecimal(String s) {
		if (s == null || s.isEmpty() || ! firstNumberChars.contains(s.charAt(0)))
			return false;
		var i = 0;
		var length = s.length();
		var c = s.charAt(0);
		var isPrefixed = false;
		if (c == '+' || c == '-') {
			isPrefixed = true;
			i++;
		}
		if (i == length)
			return false;
		c = s.charAt(i++);
		if (c == '0' && length > (isPrefixed ? 2 : 1)) {
			c = s.charAt(i++);
			if (c == 'x' || c == 'X') {
				for (var j = i; j < length; j++) {
					if (! hexChars.contains(s.charAt(j)))
						return false;
				}
			} else if (octChars.contains(c)) {
				for (var j = i; j < length; j++)
					if (! octChars.contains(s.charAt(j)))
						return false;
			} else {
				return false;
			}
		} else if (c == '#') {
			for (var j = i; j < length; j++) {
				if (! hexChars.contains(s.charAt(j)))
					return false;
			}
		} else if (decChars.contains(c)) {
			for (var j = i; j < length; j++)
				if (! decChars.contains(s.charAt(j)))
					return false;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Checks if a string contains only digit characters (0-9).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isDigit(<jk>null</jk>);         <jc>// false</jc>
	 * 	isDigit(<js>""</js>);           <jc>// false</jc>
	 * 	isDigit(<js>"123"</js>);        <jc>// true</jc>
	 * 	isDigit(<js>"abc123"</js>);     <jc>// false</jc>
	 * 	isDigit(<js>"12.3"</js>);       <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null, not empty, and contains only digit characters.
	 */
	public static boolean isDigit(String str) {
		if (isEmpty(str))
			return false;
		for (var i = 0; i < str.length(); i++) {
			if (! Character.isDigit(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the specified character is a valid first character for a number.
	 *
	 * @param c The character to test.
	 * @return <jk>true</jk> if the specified character is a valid first character for a number.
	 */
	public static boolean isFirstNumberChar(char c) {
		return firstNumberChars.contains(c);
	}

	/**
	 * Returns <jk>true</jk> if the specified string is a floating point number.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if the specified string is a floating point number.
	 */
	public static boolean isFloat(String s) {
		if (s == null || s.isEmpty())
			return false;
		if (! firstNumberChars.contains(s.charAt(0)))
			return (s.equals("NaN") || s.equals("Infinity"));
		var i = 0;
		var length = s.length();
		var c = s.charAt(0);
		if (c == '+' || c == '-')
			i++;
		if (i == length)
			return false;
		c = s.charAt(i);
		if (c == '.' || decChars.contains(c)) {
			return fpRegex.matcher(s).matches();
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified string is valid JSON.
	 *
	 * <p>
	 * Leading and trailing spaces are ignored.
	 * <br>Leading and trailing comments are not allowed.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the specified string is valid JSON.
	 */
	public static boolean isJson(String s) {
		if (s == null)
			return false;
		var c1 = firstNonWhitespaceChar(s);
		var c2 = lastNonWhitespaceChar(s);
		if (c1 == '{' && c2 == '}' || c1 == '[' && c2 == ']' || c1 == '\'' && c2 == '\'')
			return true;
		return (isOneOf(s, "true", "false", "null") || isNumeric(s));
	}

	/**
	 * Returns <jk>true</jk> if the specified string appears to be an JSON array.
	 *
	 * @param o The object to test.
	 * @param ignoreWhitespaceAndComments If <jk>true</jk>, leading and trailing whitespace and comments will be ignored.
	 * @return <jk>true</jk> if the specified string appears to be a JSON array.
	 */
	public static boolean isJsonArray(Object o, boolean ignoreWhitespaceAndComments) {
		if (o instanceof CharSequence o2) {
			var s = o2.toString();
			if (! ignoreWhitespaceAndComments)
				return (s.startsWith("[") && s.endsWith("]"));
			if (firstRealCharacter(s) != '[')
				return false;
			var i = s.lastIndexOf(']');
			if (i == -1)
				return false;
			s = s.substring(i + 1);
			return firstRealCharacter(s) == -1;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified string appears to be a JSON object.
	 *
	 * @param o The object to test.
	 * @param ignoreWhitespaceAndComments If <jk>true</jk>, leading and trailing whitespace and comments will be ignored.
	 * @return <jk>true</jk> if the specified string appears to be a JSON object.
	 */
	public static boolean isJsonObject(Object o, boolean ignoreWhitespaceAndComments) {
		if (o instanceof CharSequence o2) {
			var s = o2.toString();
			if (! ignoreWhitespaceAndComments)
				return (s.startsWith("{") && s.endsWith("}"));
			if (firstRealCharacter(s) != '{')
				return false;
			var i = s.lastIndexOf('}');
			if (i == -1)
				return false;
			s = s.substring(i + 1);
			return firstRealCharacter(s) == -1;
		}
		return false;
	}

	/**
	 * Checks if a string is not blank (not null, not empty, and not whitespace only).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotBlank(<jk>null</jk>);       <jc>// false</jc>
	 * 	isNotBlank(<js>""</js>);         <jc>// false</jc>
	 * 	isNotBlank(<js>"   "</js>);      <jc>// false</jc>
	 * 	isNotBlank(<js>"hello"</js>);    <jc>// true</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null, not empty, and contains non-whitespace characters.
	 */
	public static boolean isNotBlank(CharSequence str) {
		return ! isBlank(str);
	}

	/**
	 * Checks if any of the provided strings are not empty (not null and not zero-length).
	 *
	 * <p>
	 * Returns <jk>true</jk> if at least one string is not null and has a length greater than zero.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAnyNotEmpty(<jk>null</jk>, <jk>null</jk>);             <jc>// false</jc>
	 * 	isAnyNotEmpty(<js>""</js>, <js>""</js>);                 <jc>// false</jc>
	 * 	isAnyNotEmpty(<jk>null</jk>, <js>"hello"</js>);          <jc>// true</jc>
	 * 	isAnyNotEmpty(<js>""</js>, <js>"   "</js>);              <jc>// true</jc>
	 * 	isAnyNotEmpty(<js>"hello"</js>, <js>"world"</js>);       <jc>// true</jc>
	 * </p>
	 *
	 * @param values The strings to check.
	 * @return <jk>true</jk> if at least one string is not null and not empty.
	 */
	public static boolean isAnyNotEmpty(CharSequence...values) {
		if (values == null)
			return false;
		for (CharSequence value : values)
			if (value != null && ! value.isEmpty())
				return true;
		return false;
	}

	/**
	 * Checks if any of the provided strings are not blank (not null, not empty, and not whitespace only).
	 *
	 * <p>
	 * Returns <jk>true</jk> if at least one string is not null, not empty, and contains non-whitespace characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAnyNotBlank(<jk>null</jk>, <jk>null</jk>);             <jc>// false</jc>
	 * 	isAnyNotBlank(<js>""</js>, <js>""</js>);                 <jc>// false</jc>
	 * 	isAnyNotBlank(<js>"   "</js>, <js>"   "</js>);           <jc>// false</jc>
	 * 	isAnyNotBlank(<jk>null</jk>, <js>"hello"</js>);          <jc>// true</jc>
	 * 	isAnyNotBlank(<js>""</js>, <js>"   "</js>, <js>"x"</js>);<jc>// true</jc>
	 * 	isAnyNotBlank(<js>"hello"</js>, <js>"world"</js>);       <jc>// true</jc>
	 * </p>
	 *
	 * @param values The strings to check.
	 * @return <jk>true</jk> if at least one string is not null, not empty, and contains non-whitespace characters.
	 */
	public static boolean isAnyNotBlank(CharSequence...values) {
		if (values == null)
			return false;
		for (CharSequence value : values)
			if (isNotBlank(value))
				return true;
		return false;
	}

	/**
	 * Checks if all of the provided strings are not empty (not null and not zero-length).
	 *
	 * <p>
	 * Returns <jk>true</jk> only if all strings are not null and have a length greater than zero.
	 * Returns <jk>false</jk> if the array is null or empty, or if any string is null or empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAllNotEmpty();                                     <jc>// false</jc>
	 * 	isAllNotEmpty(<jk>null</jk>);                        <jc>// false</jc>
	 * 	isAllNotEmpty(<jk>null</jk>, <jk>null</jk>);         <jc>// false</jc>
	 * 	isAllNotEmpty(<js>""</js>, <js>""</js>);             <jc>// false</jc>
	 * 	isAllNotEmpty(<jk>null</jk>, <js>"hello"</js>);      <jc>// false</jc>
	 * 	isAllNotEmpty(<js>""</js>, <js>"   "</js>);          <jc>// false</jc>
	 * 	isAllNotEmpty(<js>"hello"</js>);                     <jc>// true</jc>
	 * 	isAllNotEmpty(<js>"hello"</js>, <js>"world"</js>);   <jc>// true</jc>
	 * 	isAllNotEmpty(<js>"hello"</js>, <js>"   "</js>);     <jc>// true</jc>
	 * </p>
	 *
	 * @param values The strings to check.
	 * @return <jk>true</jk> if all strings are not null and not empty, <jk>false</jk> otherwise.
	 */
	public static boolean isAllNotEmpty(CharSequence...values) {
		if (values == null || values.length == 0)
			return false;
		for (CharSequence value : values)
			if (value == null || value.isEmpty())
				return false;
		return true;
	}

	/**
	 * Checks if all of the provided strings are not blank (not null, not empty, and not whitespace only).
	 *
	 * <p>
	 * Returns <jk>true</jk> only if all strings are not null, not empty, and contain non-whitespace characters.
	 * Returns <jk>false</jk> if the array is null or empty, or if any string is null, empty, or whitespace only.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isAllNotBlank();                                     <jc>// false</jc>
	 * 	isAllNotBlank(<jk>null</jk>);                        <jc>// false</jc>
	 * 	isAllNotBlank(<jk>null</jk>, <jk>null</jk>);         <jc>// false</jc>
	 * 	isAllNotBlank(<js>""</js>, <js>""</js>);             <jc>// false</jc>
	 * 	isAllNotBlank(<js>"   "</js>, <js>"   "</js>);       <jc>// false</jc>
	 * 	isAllNotBlank(<jk>null</jk>, <js>"hello"</js>);      <jc>// false</jc>
	 * 	isAllNotBlank(<js>""</js>, <js>"   "</js>);          <jc>// false</jc>
	 * 	isAllNotBlank(<js>"hello"</js>, <js>"   "</js>);     <jc>// false</jc>
	 * 	isAllNotBlank(<js>"hello"</js>);                     <jc>// true</jc>
	 * 	isAllNotBlank(<js>"hello"</js>, <js>"world"</js>);   <jc>// true</jc>
	 * </p>
	 *
	 * @param values The strings to check.
	 * @return <jk>true</jk> if all strings are not null, not empty, and not whitespace only, <jk>false</jk> otherwise.
	 */
	public static boolean isAllNotBlank(CharSequence...values) {
		if (values == null || values.length == 0)
			return false;
		for (CharSequence value : values)
			if (! isNotBlank(value))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the specified character is a valid number character.
	 *
	 * @param c The character to check.
	 * @return <jk>true</jk> if the specified character is a valid number character.
	 */
	public static boolean isNumberChar(char c) {
		return numberChars.contains(c);
	}

	/**
	 * Returns <jk>true</jk> if this string can be parsed by {@link #parseNumber(String, Class)}.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if this string can be parsed without causing an exception.
	 */
	public static boolean isNumeric(String s) {
		if (s == null || s.isEmpty() || ! isFirstNumberChar(s.charAt(0)))
			return false;
		return isDecimal(s) || isFloat(s);
	}

	/**
	 * Returns <jk>true</jk> if the specified string is one of the specified values.
	 *
	 * @param s
	 * 	The string to test.
	 * 	Can be <jk>null</jk>.
	 * @param values
	 * 	The values to test.
	 * 	Can contain <jk>null</jk>.
	 * @return <jk>true</jk> if the specified string is one of the specified values.
	 */
	public static boolean isOneOf(String s, String...values) {
		for (var value : values)
			if (eq(s, value))
				return true;
		return false;
	}

	/**
	 * Efficiently determines whether a URL is of the pattern "xxx:/xxx".
	 *
	 * <p>
	 * The pattern matched is: <c>[a-z]{2,}\:\/.*</c>
	 *
	 * <p>
	 * Note that this excludes filesystem paths such as <js>"C:/temp"</js>.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if it's an absolute path.
	 */
	public static boolean isUri(String s) {  // NOSONAR - False positive.

		if (isEmpty(s))
			return false;

		// Use a state machine for maximum performance.

		// S1: Looking for protocol char 1
		// S2: Found protocol char 1, looking for protocol char 2
		// S3: Found protocol char 2, looking for :
		// S4: Found :, looking for /

		var state = S1;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (state == S1) {
				if (c >= 'a' && c <= 'z')
					state = S2;
				else
					return false;
			} else if (state == S2) {
				if (c >= 'a' && c <= 'z')
					state = S3;
				else
					return false;
			} else if (state == S3) {  // NOSONAR - False positive.
				if (c == ':')
					state = S4;
				else if (c < 'a' || c > 'z')
					return false;
			} else if (state == S4) {
				return c == '/';
			}
		}
		return false;
	}

	/**
	 * Checks if a character is whitespace.
	 *
	 * @param c The character to check.
	 * @return <jk>true</jk> if the character is whitespace.
	 */
	public static boolean isWhitespace(int c) {
		return Character.isWhitespace(c);
	}

	/**
	 * Checks if a string contains only whitespace characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isWhitespace(<jk>null</jk>);         <jc>// false</jc>
	 * 	isWhitespace(<js>""</js>);           <jc>// true</jc>
	 * 	isWhitespace(<js>"   "</js>);        <jc>// true</jc>
	 * 	isWhitespace(<js>"\t\n"</js>);       <jc>// true</jc>
	 * 	isWhitespace(<js>" a "</js>);        <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is not null and contains only whitespace characters (or is empty).
	 */
	public static boolean isWhitespace(String str) {
		if (str == null)
			return false;
		if (str.isEmpty())
			return true;
		for (var i = 0; i < str.length(); i++) {
			if (! isWhitespace(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, String d) {
		if (tokens == null)
			return null;
		return StringUtils.join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Joins the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(Collection<?> tokens, String d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (var iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
		}
		return sb;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(int[] tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Joins an array of integers with a delimiter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	join(<jk>new int</jk>[]{1, 2, 3}, <js>","</js>);   <jc>// "1,2,3"</jc>
	 * 	join(<jk>new int</jk>[]{}, <js>","</js>);          <jc>// ""</jc>
	 * </p>
	 *
	 * @param array The array to join.
	 * @param delimiter The delimiter string.
	 * @return The joined string.
	 */
	public static String join(int[] array, String delimiter) {
		if (array == null || array.length == 0)
			return "";
		if (delimiter == null)
			delimiter = "";
		return Arrays.stream(array).mapToObj(String::valueOf).collect(Collectors.joining(delimiter));
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(List<?> tokens, char d) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (int i = 0, j = tokens.size(); i < j; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens.get(i));
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(List<?> tokens, String d) {
		if (tokens == null)
			return null;
		return StringUtils.join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Joins the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(List<?> tokens, String d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (int i = 0, j = tokens.size(); i < j; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens.get(i));
		}
		return sb;
	}

	/**
	 * Joins the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, char d) {
		if (tokens == null)
			return null;
		if (tokens.length == 1)
			return emptyIfNull(s(tokens[0]));
		return StringUtils.join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Join the specified tokens into a delimited string and writes the output to the specified string builder.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <c>sb</c>.
	 */
	public static StringBuilder join(Object[] tokens, char d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb;
	}

	/**
	 * Join the specified tokens into a delimited string.
	 *
	 * @param tokens The tokens to join.
	 * @param separator The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, String separator) {
		if (tokens == null)
			return null;
		var sb = new StringBuilder();
		for (var i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(separator);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Same as {@link StringUtils#join(Collection, char)} but escapes the delimiter if found in the tokens.
	 *
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <c>tokens</c> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String joine(List<?> tokens, char d) {
		if (tokens == null)
			return null;
		var as = getEscapeSet(d);
		var sb = new StringBuilder();
		for (int i = 0, j = tokens.size(); i < j; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(escapeChars(s(tokens.get(i)), as));
		}
		return sb.toString();
	}

	/**
	 * Joins tokens with newlines.
	 *
	 * @param tokens The tokens to concatenate.
	 * @return A string with the specified tokens contatenated with newlines.
	 */
	public static String joinnl(Object[] tokens) {
		return join(tokens, '\n');
	}

	/**
	 * Returns the last non-whitespace character in the string.
	 *
	 * @param s The string to check.
	 * @return
	 * 	The last non-whitespace character, or <c>0</c> if the string is <jk>null</jk>, empty, or composed
	 * 	of only whitespace.
	 */
	public static char lastNonWhitespaceChar(String s) {
		if (nn(s))
			for (var i = s.length() - 1; i >= 0; i--)
				if (! isWhitespace(s.charAt(i)))
					return s.charAt(i);
		return 0;
	}

	/**
	 * Returns the leftmost characters of a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	left(<jk>null</jk>, 3);          <jc>// null</jc>
	 * 	left(<js>""</js>, 3);            <jc>// ""</jc>
	 * 	left(<js>"hello"</js>, 3);       <jc>// "hel"</jc>
	 * 	left(<js>"hello"</js>, 10);      <jc>// "hello"</jc>
	 * </p>
	 *
	 * @param str The string to get characters from.
	 * @param len The number of characters to get.
	 * @return The leftmost characters, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String left(String str, int len) {
		if (str == null)
			return null;
		if (len < 0)
			return "";
		if (len >= str.length())
			return str;
		return str.substring(0, len);
	}

	/**
	 * Returns the middle characters of a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	mid(<jk>null</jk>, 1, 3);          <jc>// null</jc>
	 * 	mid(<js>""</js>, 1, 3);            <jc>// ""</jc>
	 * 	mid(<js>"hello"</js>, 1, 3);       <jc>// "ell"</jc>
	 * 	mid(<js>"hello"</js>, 1, 10);      <jc>// "ello"</jc>
	 * </p>
	 *
	 * @param str The string to get characters from.
	 * @param pos The starting position (0-based).
	 * @param len The number of characters to get.
	 * @return The middle characters, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String mid(String str, int pos, int len) {
		if (str == null)
			return null;
		if (pos < 0 || len < 0)
			return "";
		if (pos >= str.length())
			return "";
		int end = Math.min(pos + len, str.length());
		return str.substring(pos, end);
	}

	/**
	 * Normalizes all whitespace in a string to single spaces.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	normalizeWhitespace(<js>"hello  \t\n  world"</js>);   <jc>// "hello world"</jc>
	 * 	normalizeWhitespace(<js>"  hello  world  "</js>);     <jc>// "hello world"</jc>
	 * </p>
	 *
	 * @param str The string to normalize.
	 * @return The normalized string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String normalizeWhitespace(String str) {
		if (str == null)
			return null;
		return str.replaceAll("\\s+", " ").trim();
	}

	/**
	 * Null-safe string not-contains operation.
	 *
	 * @param s The string to check.
	 * @param values The characters to check for.
	 * @return <jk>true</jk> if the string does not contain any of the specified characters.
	 */
	public static boolean notContains(String s, char...values) {
		return ! contains(s, values);
	}

	/**
	 * Returns the specified string, or <jk>null</jk> if that string is <jk>null</jk> or empty.
	 *
	 * @param value The string value to check.
	 * @return The string value, or <jk>null</jk> if the string is <jk>null</jk> or empty.
	 */
	public static String nullIfEmpty(String value) {
		return isEmpty(value) ? null : value;
	}

	/**
	 * Returns an obfuscated version of the specified string.
	 *
	 * @param s The string to obfuscate.
	 * @return The obfuscated string with most characters replaced by asterisks.
	 */
	public static String obfuscate(String s) {
		if (s == null || s.length() < 2)
			return "*";
		return s.substring(0, 1) + s.substring(1).replaceAll(".", "*");  // NOSONAR
	}

	/**
	 * Center pads a string with a specified character.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	padCenter(<jk>null</jk>, 5, <js>' '</js>);          <jc>// "     "</jc>
	 * 	padCenter(<js>""</js>, 5, <js>' '</js>);            <jc>// "     "</jc>
	 * 	padCenter(<js>"hi"</js>, 6, <js>' '</js>);          <jc>// "  hi  "</jc>
	 * 	padCenter(<js>"hi"</js>, 7, <js>' '</js>);          <jc>// "   hi  "</jc>
	 * 	padCenter(<js>"hello"</js>, 3, <js>' '</js>);       <jc>// "hello"</jc>
	 * </p>
	 *
	 * @param str The string to pad.
	 * @param size The desired total string length.
	 * @param padChar The character to pad with.
	 * @return The center-padded string.
	 */
	public static String padCenter(String str, int size, char padChar) {
		if (str == null)
			str = "";
		int pads = size - str.length();
		if (pads <= 0)
			return str;
		int rightPads = pads / 2;
		int leftPads = pads - rightPads;
		return String.valueOf(padChar).repeat(leftPads) + str + String.valueOf(padChar).repeat(rightPads);
	}

	/**
	 * Left pads a string with a specified character.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	padLeft(<jk>null</jk>, 5, <js>' '</js>);          <jc>// "     "</jc>
	 * 	padLeft(<js>""</js>, 5, <js>' '</js>);            <jc>// "     "</jc>
	 * 	padLeft(<js>"hello"</js>, 8, <js>' '</js>);       <jc>// "   hello"</jc>
	 * 	padLeft(<js>"hello"</js>, 3, <js>' '</js>);       <jc>// "hello"</jc>
	 * 	padLeft(<js>"123"</js>, 5, <js>'0'</js>);         <jc>// "00123"</jc>
	 * </p>
	 *
	 * @param str The string to pad.
	 * @param size The desired total string length.
	 * @param padChar The character to pad with.
	 * @return The left-padded string.
	 */
	public static String padLeft(String str, int size, char padChar) {
		if (str == null)
			str = "";
		int pads = size - str.length();
		if (pads <= 0)
			return str;
		return String.valueOf(padChar).repeat(pads) + str;
	}

	/**
	 * Right pads a string with a specified character.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	padRight(<jk>null</jk>, 5, <js>' '</js>);          <jc>// "     "</jc>
	 * 	padRight(<js>""</js>, 5, <js>' '</js>);            <jc>// "     "</jc>
	 * 	padRight(<js>"hello"</js>, 8, <js>' '</js>);       <jc>// "hello   "</jc>
	 * 	padRight(<js>"hello"</js>, 3, <js>' '</js>);       <jc>// "hello"</jc>
	 * 	padRight(<js>"123"</js>, 5, <js>'0'</js>);         <jc>// "12300"</jc>
	 * </p>
	 *
	 * @param str The string to pad.
	 * @param size The desired total string length.
	 * @param padChar The character to pad with.
	 * @return The right-padded string.
	 */
	public static String padRight(String str, int size, char padChar) {
		if (str == null)
			str = "";
		int pads = size - str.length();
		if (pads <= 0)
			return str;
		return str + String.valueOf(padChar).repeat(pads);
	}

	/**
	 * Converts a <c>String</c> to a <c>Character</c>
	 *
	 * @param o The string to convert.
	 * @return The first character of the string if the string is of length 1, or <jk>null</jk> if the string is <jk>null</jk> or empty.
	 * @throws IllegalArgumentException If the string length is not 1.
	 */
	public static Character parseCharacter(Object o) {
		if (o == null)
			return null;
		var s = o.toString();
		if (s.isEmpty())
			return null;
		if (s.length() == 1)
			return s.charAt(0);
		throw illegalArg("Invalid character: ''{0}''", s);
	}

	/**
	 * Converts a string containing a possible multiplier suffix to an integer.
	 *
	 * <p>
	 * The string can contain any of the following multiplier suffixes:
	 * <ul>
	 * 	<li><js>"K"</js> - x 1024
	 * 	<li><js>"M"</js> - x 1024*1024
	 * 	<li><js>"G"</js> - x 1024*1024*1024
	 * 	<li><js>"k"</js> - x 1000
	 * 	<li><js>"m"</js> - x 1000*1000
	 * 	<li><js>"g"</js> - x 1000*1000*1000
	 * </ul>
	 *
	 * @param s The string to parse.
	 * @return The parsed value.
	 */
	public static int parseIntWithSuffix(String s) {
		assertArgNotNull("s", s);
		var m = multiplier(s);
		if (m == 1)
			return Integer.decode(s);
		return Integer.decode(s.substring(0, s.length() - 1).trim()) * m;  // NOSONAR - NPE not possible here.
	}

	// TODO: See if we can remove StringUtils.parseIsoCalendar.
	// Currently used by:
	//   - OpenApiParserSession.java for DATE/DATE_TIME format parsing
	//   - StringUtils.parseIsoDate() (which wraps this method)
	// Investigation needed: Can we replace this with java.time APIs or other standard date parsing?

	/**
	 * Parses an ISO8601 string into a calendar.
	 *
	 * <p>
	 * Supports any of the following formats:
	 * <br><c>yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS</c>
	 *
	 * @param date The date string.
	 * @return The parsed calendar.
	 * @throws IllegalArgumentException Value was not a valid date.
	 */
	public static Calendar parseIsoCalendar(String date) throws IllegalArgumentException {
		if (isEmpty(date))
			return null;
		date = date.trim().replace(' ', 'T');  // Convert to 'standard' ISO8601
		if (date.indexOf(',') != -1)  // Trim milliseconds
			date = date.substring(0, date.indexOf(','));
		if (date.matches("\\d{4}"))
			date += "-01-01T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}"))
			date += "-01T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}"))
			date += "T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}"))
			date += ":00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}\\:\\d{2}"))
			date += ":00";
		return DateUtils.fromIso8601Calendar(date);
	}

	/**
	 * Parses an ISO8601 string into a date.
	 *
	 * <p>
	 * Supports any of the following formats:
	 * <br><c>yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS</c>
	 *
	 * @param date The date string.
	 * @return The parsed date.
	 * @throws IllegalArgumentException Value was not a valid date.
	 */
	public static Date parseIsoDate(String date) throws IllegalArgumentException {
		if (isEmpty(date))
			return null;
		return parseIsoCalendar(date).getTime();  // NOSONAR - NPE not possible.
	}

	/**
	 * Converts a string containing a possible multiplier suffix to a long.
	 *
	 * <p>
	 * The string can contain any of the following multiplier suffixes:
	 * <ul>
	 * 	<li><js>"K"</js> - x 1024
	 * 	<li><js>"M"</js> - x 1024*1024
	 * 	<li><js>"G"</js> - x 1024*1024*1024
	 * 	<li><js>"T"</js> - x 1024*1024*1024*1024
	 * 	<li><js>"P"</js> - x 1024*1024*1024*1024*1024
	 * 	<li><js>"k"</js> - x 1000
	 * 	<li><js>"m"</js> - x 1000*1000
	 * 	<li><js>"g"</js> - x 1000*1000*1000
	 * 	<li><js>"t"</js> - x 1000*1000*1000*1000
	 * 	<li><js>"p"</js> - x 1000*1000*1000*1000*1000
	 * </ul>
	 *
	 * @param s The string to parse.
	 * @return The parsed value.
	 */
	public static long parseLongWithSuffix(String s) {
		assertArgNotNull("s", s);
		var m = multiplier2(s);
		if (m == 1)
			return Long.decode(s);
		return Long.decode(s.substring(0, s.length() - 1).trim()) * m;  // NOSONAR - NPE not possible here.
	}

	/**
	 * Parses a number from the specified string.
	 *
	 * @param s The string to parse the number from.
	 * @param type
	 * 	The number type to created.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li> Integer
	 * 		<li> Double
	 * 		<li> Float
	 * 		<li> Long
	 * 		<li> Short
	 * 		<li> Byte
	 * 		<li> BigInteger
	 * 		<li> BigDecimal
	 * 	</ul>
	 * 	If <jk>null</jk> or <c>Number</c>, uses the best guess.
	 * @return The parsed number, or <jk>null</jk> if the string was null.
	 */
	public static Number parseNumber(String s, Class<? extends Number> type) {
		if (s == null)
			return null;
		if (s.isEmpty())
			s = "0";
		if (type == null)
			type = Number.class;

		// Determine the data type if it wasn't specified.
		var isAutoDetect = (type == Number.class);
		var isDecimal = false;
		if (isAutoDetect) {
			// If we're auto-detecting, then we use either an Integer, Long, or Double depending on how
			// long the string is.
			// An integer range is -2,147,483,648 to 2,147,483,647
			// An long range is -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
			isDecimal = isDecimal(s);
			if (isDecimal) {
				if (s.length() > 20)
					type = Double.class;
				else if (s.length() >= 10)
					type = Long.class;
				else
					type = Integer.class;
			} else if (isFloat(s))
				type = Double.class;
			else
				throw new NumberFormatException(s);
		}

		if (type == Double.class || type == Double.TYPE) {
			var d = Double.valueOf(s);
			var f = Float.valueOf(s);
			if (isAutoDetect && (! isDecimal) && d.toString().equals(f.toString()))
				return f;
			return d;
		}
		if (type == Float.class || type == Float.TYPE)
			return Float.valueOf(s);
		if (type == BigDecimal.class)
			return new BigDecimal(s);
		if (type == Long.class || type == Long.TYPE || type == AtomicLong.class) {
			try {
				var l = Long.decode(s);
				if (type == AtomicLong.class)
					return new AtomicLong(l);
				if (isAutoDetect && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
					// This occurs if the string is 10 characters long but is still a valid integer value.
					return l.intValue();
				}
				return l;
			} catch (NumberFormatException e) {
				if (isAutoDetect) {
					// This occurs if the string is 20 characters long but still falls outside the range of a valid long.
					return Double.valueOf(s);
				}
				throw e;
			}
		}
		if (type == Integer.class || type == Integer.TYPE)
			return Integer.decode(s);
		if (type == Short.class || type == Short.TYPE)
			return Short.decode(s);
		if (type == Byte.class || type == Byte.TYPE)
			return Byte.decode(s);
		if (type == BigInteger.class)
			return new BigInteger(s);
		if (type == AtomicInteger.class)
			return new AtomicInteger(Integer.decode(s));
		throw new NumberFormatException("Unsupported Number type: " + type.getName());
	}

	/**
	 * Generated a random UUID with the specified number of characters.
	 *
	 * <p>
	 * Characters are composed of lower-case ASCII letters and numbers only.
	 *
	 * <p>
	 * This method conforms to the restrictions for hostnames as specified in <a class="doclink" href="https://tools.ietf.org/html/rfc952">RFC 952</a>
	 * Since each character has 36 possible values, the square approximation formula for the number of generated IDs
	 * that would produce a 50% chance of collision is:
	 * <c>sqrt(36^N)</c>.
	 * Dividing this number by 10 gives you an approximation of the number of generated IDs needed to produce a
	 * &lt;1% chance of collision.
	 *
	 * <p>
	 * For example, given 5 characters, the number of generated IDs need to produce a &lt;1% chance of collision would
	 * be:
	 * <c>sqrt(36^5)/10=777</c>
	 *
	 * @param numchars The number of characters in the generated UUID.
	 * @return A new random UUID.
	 */
	public static String random(int numchars) {
		var sb = new StringBuilder(numchars);
		for (var i = 0; i < numchars; i++) {
			var c = RANDOM.nextInt(36) + 97;
			if (c > 'z')
				c -= ('z' - '0' + 1);
			sb.append((char)c);
		}
		return sb.toString();
	}

	/**
	 * Removes all occurrences of a substring from a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	remove(<jk>null</jk>, <js>"x"</js>);              <jc>// null</jc>
	 * 	remove(<js>"hello"</js>, <jk>null</jk>);          <jc>// "hello"</jc>
	 * 	remove(<js>"hello world"</js>, <js>"o"</js>);     <jc>// "hell wrld"</jc>
	 * 	remove(<js>"hello world"</js>, <js>"xyz"</js>);   <jc>// "hello world"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @param remove The substring to remove.
	 * @return The string with all occurrences of the substring removed, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String remove(String str, String remove) {
		if (isEmpty(str) || isEmpty(remove))
			return str;
		return str.replace(remove, "");
	}

	/**
	 * Removes control characters from a string, replacing them with spaces.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeControlChars(<js>"hello\u0000\u0001world"</js>);   <jc>// "hello  world"</jc>
	 * 	removeControlChars(<js>"hello\nworld"</js>);             <jc>// "hello\nworld"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @return The string with control characters replaced by spaces (except whitespace control chars), or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeControlChars(String str) {
		if (str == null)
			return null;
		var sb = new StringBuilder();
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			if (Character.isISOControl(c) && ! Character.isWhitespace(c))
				sb.append(' ');
			else
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Removes a suffix from a string if present.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeEnd(<jk>null</jk>, <js>"x"</js>);              <jc>// null</jc>
	 * 	removeEnd(<js>"hello"</js>, <jk>null</jk>);          <jc>// "hello"</jc>
	 * 	removeEnd(<js>"hello world"</js>, <js>"world"</js>); <jc>// "hello "</jc>
	 * 	removeEnd(<js>"hello world"</js>, <js>"xyz"</js>);   <jc>// "hello world"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @param suffix The suffix to remove.
	 * @return The string with the suffix removed if present, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeEnd(String str, String suffix) {
		if (isEmpty(str) || isEmpty(suffix))
			return str;
		if (str.endsWith(suffix))
			return str.substring(0, str.length() - suffix.length());
		return str;
	}

	/**
	 * Removes non-printable characters from a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeNonPrintable(<js>"hello\u0000world"</js>);   <jc>// "helloworld"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @return The string with non-printable characters removed, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeNonPrintable(String str) {
		if (str == null)
			return null;
		return str.replaceAll("\\p{C}", "");
	}

	/**
	 * Removes a prefix from a string if present.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeStart(<jk>null</jk>, <js>"x"</js>);              <jc>// null</jc>
	 * 	removeStart(<js>"hello"</js>, <jk>null</jk>);          <jc>// "hello"</jc>
	 * 	removeStart(<js>"hello world"</js>, <js>"hello"</js>); <jc>// " world"</jc>
	 * 	removeStart(<js>"hello world"</js>, <js>"xyz"</js>);   <jc>// "hello world"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @param prefix The prefix to remove.
	 * @return The string with the prefix removed if present, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeStart(String str, String prefix) {
		if (isEmpty(str) || isEmpty(prefix))
			return str;
		if (str.startsWith(prefix))
			return str.substring(prefix.length());
		return str;
	}

	/**
	 * Creates a repeated pattern.
	 *
	 * @param count The number of times to repeat the pattern.
	 * @param pattern The pattern to repeat.
	 * @return A new string consisting of the repeated pattern.
	 */
	public static String repeat(int count, String pattern) {
		var sb = new StringBuilder(pattern.length() * count);
		for (var i = 0; i < count; i++)
			sb.append(pattern);
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String validation methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces <js>"\\uXXXX"</js> character sequences with their unicode characters.
	 *
	 * @param s The string to replace unicode sequences in.
	 * @return A string with unicode sequences replaced.
	 */
	public static String replaceUnicodeSequences(String s) {

		if (s.indexOf('\\') == -1)
			return s;

		var p = Pattern.compile("\\\\u(\\p{XDigit}{4})");
		var m = p.matcher(s);
		var sb = new StringBuffer(s.length());

		while (m.find()) {
			var ch = String.valueOf((char)Integer.parseInt(m.group(1), 16));
			m.appendReplacement(sb, Matcher.quoteReplacement(ch));
		}

		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Simple utility for replacing variables of the form <js>"{key}"</js> with values in the specified map.
	 *
	 * <p>
	 * Nested variables are supported in both the input string and map values.
	 *
	 * <p>
	 * If the map does not contain the specified value, the variable is not replaced.
	 *
	 * <p>
	 * <jk>null</jk> values in the map are treated as blank strings.
	 *
	 * @param s The string containing variables to replace.
	 * @param m The map containing the variable values.
	 * @return The new string with variables replaced, or the original string if it didn't have variables in it.
	 */
	public static String replaceVars(String s, Map<String,Object> m) {

		if (s == null)
			return null;

		if (m == null || m.isEmpty() || s.indexOf('{') == -1)
			return s;

		// S1: Not in variable, looking for '{'
		// S2: Found '{', Looking for '}'

		var state = S1;
		var hasInternalVar = false;
		var x = 0;
		var depth = 0;
		var length = s.length();
		var out = new StringBuilder();

		for (var i = 0; i < length; i++) {
			var c = s.charAt(i);
			if (state == S1) {
				if (c == '{') {
					state = S2;
					x = i;
				} else {
					out.append(c);
				}
			} else /* state == S2 */ {
				if (c == '{') {
					depth++;
					hasInternalVar = true;
				} else if (c == '}') {
					if (depth > 0) {
						depth--;
					} else {
						var key = s.substring(x + 1, i);
						key = (hasInternalVar ? replaceVars(key, m) : key);
						hasInternalVar = false;
						// JUNEAU-248: Check if key exists in map by attempting to get it
						// For regular maps: use containsKey() OR nn(get()) check
						// For BeanMaps: get() returns non-null for accessible properties (including hidden ones)
						var val = m.get(key);
						// Check if key actually exists: either containsKey is true, or val is non-null
						// This handles both regular maps and BeanMaps correctly
						var keyExists = m.containsKey(key) || nn(val);
						if (! keyExists)
							out.append('{').append(key).append('}');
						else {
							if (val == null)
								val = "";
							var v = val.toString();
							// If the replacement also contains variables, replace them now.
							if (v.indexOf('{') != -1)
								v = replaceVars(v, m);
							out.append(v);
						}
						state = S1;
					}
				}
			}
		}
		return out.toString();
	}

	/**
	 * Reverses a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	reverse(<jk>null</jk>);          <jc>// null</jc>
	 * 	reverse(<js>""</js>);            <jc>// ""</jc>
	 * 	reverse(<js>"hello"</js>);       <jc>// "olleh"</jc>
	 * </p>
	 *
	 * @param str The string to reverse.
	 * @return The reversed string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String reverse(String str) {
		if (str == null)
			return null;
		return new StringBuilder(str).reverse().toString();
	}

	/**
	 * Returns the rightmost characters of a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	right(<jk>null</jk>, 3);          <jc>// null</jc>
	 * 	right(<js>""</js>, 3);            <jc>// ""</jc>
	 * 	right(<js>"hello"</js>, 3);       <jc>// "llo"</jc>
	 * 	right(<js>"hello"</js>, 10);      <jc>// "hello"</jc>
	 * </p>
	 *
	 * @param str The string to get characters from.
	 * @param len The number of characters to get.
	 * @return The rightmost characters, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String right(String str, int len) {
		if (str == null)
			return null;
		if (len < 0)
			return "";
		if (len >= str.length())
			return str;
		return str.substring(str.length() - len);
	}

	/**
	 * Splits a comma-delimited list into a list of strings.
	 *
	 * @param s The string to split.
	 * @return A list of split strings, or an empty list if the input is <jk>null</jk>.
	 */
	public static List<String> split(String s) {
		return s == null ? Collections.emptyList() : split(s, ',');
	}

	/**
	 * Splits a character-delimited string into a string array.
	 *
	 * <p>
	 * Does not split on escaped-delimiters (e.g. "\,");
	 * Resulting tokens are trimmed of whitespace.
	 *
	 * <p>
	 * <b>NOTE:</b>  This behavior is different than the Jakarta equivalent.
	 * split("a,b,c",',') -&gt; {"a","b","c"}
	 * split("a, b ,c ",',') -&gt; {"a","b","c"}
	 * split("a,,c",',') -&gt; {"a","","c"}
	 * split(",,",',') -&gt; {"","",""}
	 * split("",',') -&gt; {}
	 * split(null,',') -&gt; null
	 * split("a,b\,c,d", ',', false) -&gt; {"a","b\,c","d"}
	 * split("a,b\\,c,d", ',', false) -&gt; {"a","b\","c","d"}
	 * split("a,b\,c,d", ',', true) -&gt; {"a","b,c","d"}
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static List<String> split(String s, char c) {
		return split(s, c, Integer.MAX_VALUE);
	}

	/**
	 * Same as {@link splita} but consumes the tokens instead of creating an array.
	 *
	 * @param s The string to split.
	 * @param c The character to split on.
	 * @param consumer The consumer of the tokens.
	 */
	public static void split(String s, char c, Consumer<String> consumer) {
		var escapeChars = getEscapeSet(c);

		if (isEmpty(s))
			return;
		if (s.indexOf(c) == -1) {
			consumer.accept(s);
			return;
		}

		var x1 = 0;
		var escapeCount = 0;

		for (var i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\\')
				escapeCount++;
			else if (s.charAt(i) == c && escapeCount % 2 == 0) {
				var s2 = s.substring(x1, i);
				var s3 = unEscapeChars(s2, escapeChars);
				consumer.accept(s3.trim());  // NOSONAR - NPE not possible.
				x1 = i + 1;
			}
			if (s.charAt(i) != '\\')
				escapeCount = 0;
		}
		var s2 = s.substring(x1);
		var s3 = unEscapeChars(s2, escapeChars);
		consumer.accept(s3.trim());  // NOSONAR - NPE not possible.
	}

	/**
	 * Same as {@link splita} but limits the number of tokens returned.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @param limit The maximum number of tokens to return.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static List<String> split(String s, char c, int limit) {

		var escapeChars = getEscapeSet(c);

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return Collections.emptyList();
		if (s.indexOf(c) == -1)
			return Collections.singletonList(s);

		var l = new LinkedList<String>();
		var sArray = s.toCharArray();
		var x1 = 0;
		var escapeCount = 0;
		limit--;
		for (var i = 0; i < sArray.length && limit > 0; i++) {
			if (sArray[i] == '\\')
				escapeCount++;
			else if (sArray[i] == c && escapeCount % 2 == 0) {
				var s2 = new String(sArray, x1, i - x1);
				var s3 = unEscapeChars(s2, escapeChars);
				l.add(s3.trim());
				limit--;
				x1 = i + 1;
			}
			if (sArray[i] != '\\')
				escapeCount = 0;
		}
		var s2 = new String(sArray, x1, sArray.length - x1);
		var s3 = unEscapeChars(s2, escapeChars);
		l.add(s3.trim());

		return l;
	}

	/**
	 * Same as {@link splita} but consumes the tokens instead of creating an array.
	 *
	 * @param s The string to split.
	 * @param consumer The consumer of the tokens.
	 */
	public static void split(String s, Consumer<String> consumer) {
		StringUtils.split(s, ',', consumer);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String manipulation methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Splits a comma-delimited list into an array of strings.
	 *
	 * @param s The string to split.
	 * @return An array of split strings.
	 */
	public static String[] splita(String s) {
		return splita(s, ',');
	}

	/**
	 * Splits a character-delimited string into a string array.
	 *
	 * <p>
	 * Does not split on escaped-delimiters (e.g. "\,");
	 * Resulting tokens are trimmed of whitespace.
	 *
	 * <p>
	 * <b>NOTE:</b>  This behavior is different than the Jakarta equivalent.
	 * split("a,b,c",',') -&gt; {"a","b","c"}
	 * split("a, b ,c ",',') -&gt; {"a","b","c"}
	 * split("a,,c",',') -&gt; {"a","","c"}
	 * split(",,",',') -&gt; {"","",""}
	 * split("",',') -&gt; {}
	 * split(null,',') -&gt; null
	 * split("a,b\,c,d", ',', false) -&gt; {"a","b\,c","d"}
	 * split("a,b\\,c,d", ',', false) -&gt; {"a","b\","c","d"}
	 * split("a,b\,c,d", ',', true) -&gt; {"a","b,c","d"}
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] splita(String s, char c) {
		return splita(s, c, Integer.MAX_VALUE);
	}

	/**
	 * Same as {@link #splita(String, char)} but limits the number of tokens returned.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @param limit The maximum number of tokens to return.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] splita(String s, char c, int limit) {
		var l = StringUtils.split(s, c, limit);
		return l == null ? null : l.toArray(new String[l.size()]);
	}

	/**
	 * Same as {@link #splita(String, char)} except splits all strings in the input and returns a single result.
	 *
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or null if the input array was null
	 */
	public static String[] splita(String[] s, char c) {
		if (s == null)
			return null;  // NOSONAR - Intentional.
		var l = new LinkedList<String>();
		for (var ss : s) {
			if (ss == null || ss.indexOf(c) == -1)
				l.add(ss);
			else
				Collections.addAll(l, splita(ss, c));
		}
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Splits a list of key-value pairs into an ordered map.
	 *
	 * <p>
	 * Example:
	 * <p class='bjava'>
	 * 	String <jv>in</jv> = <js>"foo=1;bar=2"</js>;
	 * 	Map <jv>map</jv> = StringUtils.<jsm>splitMap</jsm>(in, <js>';'</js>, <js>'='</js>, <jk>true</jk>);
	 * </p>
	 *
	 * @param s The string to split.
	 * @param trim Trim strings after parsing.
	 * @return The parsed map, or null if the string was null.
	 */
	public static Map<String,String> splitMap(String s, boolean trim) {

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return Collections.emptyMap();

		var m = new LinkedHashMap<String,String>();

		// S1: Found start of key, looking for equals.
		// S2: Found equals, looking for delimiter (or end).

		var state = S1;

		var sArray = s.toCharArray();
		var x1 = 0;
		var escapeCount = 0;
		String key = null;
		for (var i = 0; i < sArray.length + 1; i++) {
			var c = i == sArray.length ? ',' : sArray[i];
			if (c == '\\')
				escapeCount++;
			if (escapeCount % 2 == 0) {
				if (state == S1) {
					if (c == '=') {
						key = s.substring(x1, i);
						if (trim)
							key = trim(key);
						key = unEscapeChars(key, MAP_ESCAPE_SET);
						state = S2;
						x1 = i + 1;
					} else if (c == ',') {
						key = s.substring(x1, i);
						if (trim)
							key = trim(key);
						key = unEscapeChars(key, MAP_ESCAPE_SET);
						m.put(key, "");
						state = S1;
						x1 = i + 1;
					}
				} else if (state == S2) {
					if (c == ',') {  // NOSONAR - Intentional.
						var val = s.substring(x1, i);
						if (trim)
							val = trim(val);
						val = unEscapeChars(val, MAP_ESCAPE_SET);
						m.put(key, val);
						key = null;
						x1 = i + 1;
						state = S1;
					}
				}
			}
			if (c != '\\')
				escapeCount = 0;
		}

		return m;
	}

	/**
	 * Splits the method arguments in the signature of a method.
	 *
	 * @param s The arguments to split.
	 * @return The split arguments, or null if the input string is null.
	 */
	public static String[] splitMethodArgs(String s) {

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return new String[0];
		if (s.indexOf(',') == -1)
			return a(s);

		var l = new LinkedList<String>();
		var sArray = s.toCharArray();
		var x1 = 0;
		var paramDepth = 0;

		for (var i = 0; i < sArray.length; i++) {
			var c = s.charAt(i);
			if (c == '>')
				paramDepth++;
			else if (c == '<')
				paramDepth--;
			else if (c == ',' && paramDepth == 0) {
				var s2 = new String(sArray, x1, i - x1);
				l.add(s2.trim());
				x1 = i + 1;
			}
		}

		var s2 = new String(sArray, x1, sArray.length - x1);
		l.add(s2.trim());

		return l.toArray(new String[l.size()]);
	}

	/**
	 * Splits a comma-delimited list containing "nesting constructs".
	 *
	 * Nesting constructs are simple embedded "{...}" comma-delimted lists.
	 *
	 * Example:
	 * 	"a{b,c},d" -> ["a{b,c}","d"]
	 *
	 * Handles escapes and trims whitespace from tokens.
	 *
	 * @param s The input string.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static List<String> splitNested(String s) {
		var escapeChars = getEscapeSet(',');

		if (s == null)
			return null;  // NOSONAR - Intentional.
		if (isEmpty(s))
			return Collections.emptyList();
		if (s.indexOf(',') == -1)
			return Collections.singletonList(trim(s));

		var l = new LinkedList<String>();

		var x1 = 0;
		var inEscape = false;
		var depthCount = 0;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (inEscape) {
				if (c == '\\') {
					inEscape = false;
				}
			} else {
				if (c == '\\') {
					inEscape = true;
				} else if (c == '{') {
					depthCount++;
				} else if (c == '}') {
					depthCount--;
				} else if (c == ',' && depthCount == 0) {
					l.add(trim(unEscapeChars(s.substring(x1, i), escapeChars)));
					x1 = i + 1;
				}
			}
		}
		l.add(trim(unEscapeChars(s.substring(x1, s.length()), escapeChars)));

		return l;
	}

	/**
	 * Splits a nested comma-delimited list.
	 *
	 * Nesting constructs are simple embedded "{...}" comma-delimted lists.
	 *
	 * Example:
	 * 	"a{b,c{d,e}}" -> ["b","c{d,e}"]
	 *
	 * Handles escapes and trims whitespace from tokens.
	 *
	 * @param s The input string.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static List<String> splitNestedInner(String s) {
		assertArg(isNotNull(s), "String was null.");
		assertArg(isNotEmpty(s), "String was empty.");

		// S1: Looking for '{'
		// S2: Found '{', looking for '}'

		var start = -1;
		var end = -1;
		var state = S1;

		var depth = 0;
		var inEscape = false;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (inEscape) {
				if (c == '\\') {
					inEscape = false;
				}
			} else {
				if (c == '\\') {
					inEscape = true;
				} else if (state == S1) {
					if (c == '{') {
						start = i + 1;
						state = S2;
					}
				} else /* state == S2 */ {
					if (c == '{') {
						depth++;
					} else if (depth > 0 && c == '}') {
						depth--;
					} else if (c == '}') {
						end = i;
						break;
					}
				}
			}
		}

		if (start == -1)
			throw illegalArg("Start character '{' not found in string:  {0}", s);
		if (end == -1)
			throw illegalArg("End character '}' not found in string  {0}", s);
		return splitNested(s.substring(start, end));
	}

	/**
	 * Splits a space-delimited string with optionally quoted arguments.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"foo"</js> =&gt; <c>["foo"]</c>
	 * 	<li><js>" foo "</js> =&gt; <c>["foo"]</c>
	 * 	<li><js>"foo bar baz"</js> =&gt; <c>["foo","bar","baz"]</c>
	 * 	<li><js>"foo 'bar baz'"</js> =&gt; <c>["foo","bar baz"]</c>
	 * 	<li><js>"foo \"bar baz\""</js> =&gt; <c>["foo","bar baz"]</c>
	 * 	<li><js>"foo 'bar\'baz'"</js> =&gt; <c>["foo","bar'baz"]</c>
	 * </ul>
	 *
	 * @param s The input string.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static String[] splitQuoted(String s) {
		return splitQuoted(s, false);
	}

	/**
	 * Same as {@link StringUtils#splitQuoted(String)} but allows you to optionally keep the quote characters.
	 *
	 * @param s The input string.
	 * @param keepQuotes If <jk>true</jk>, quote characters are kept on the tokens.
	 * @return
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static String[] splitQuoted(String s, boolean keepQuotes) {

		if (s == null)
			return null;  // NOSONAR - Intentional.

		s = s.trim();

		if (isEmpty(s))
			return a();

		if (! containsAny(s, ' ', '\t', '\'', '"'))
			return a(s);

		// S1: Looking for start of token.
		// S2: Found ', looking for end '
		// S3: Found ", looking for end "
		// S4: Found non-whitespace, looking for end whitespace.

		var state = S1;

		var isInEscape = false;
		var needsUnescape = false;
		var mark = 0;

		var l = new ArrayList<String>();
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);

			if (state == S1) {
				if (c == '\'') {
					state = S2;
					mark = keepQuotes ? i : i + 1;
				} else if (c == '"') {
					state = S3;
					mark = keepQuotes ? i : i + 1;
				} else if (c != ' ' && c != '\t') {
					state = S4;
					mark = i;
				}
			} else if (state == S2 || state == S3) {
				if (c == '\\') {
					isInEscape = ! isInEscape;
					needsUnescape = ! keepQuotes;
				} else if (! isInEscape) {
					if (c == (state == S2 ? '\'' : '"')) {
						var s2 = s.substring(mark, keepQuotes ? i + 1 : i);
						if (needsUnescape)  // NOSONAR - False positive check.
							s2 = unEscapeChars(s2, QUOTE_ESCAPE_SET);
						l.add(s2);
						state = S1;
						isInEscape = needsUnescape = false;
					}
				} else {
					isInEscape = false;
				}
			} else /* state == S4 */ {
				if (c == ' ' || c == '\t') {
					l.add(s.substring(mark, i));
					state = S1;
				}
			}
		}
		if (state == S4)
			l.add(s.substring(mark));
		else if (state == S2 || state == S3)
			throw illegalArg("Unmatched string quotes: {0}", s);
		return l.toArray(new String[l.size()]);
	}

	/**
	 * An efficient method for checking if a string starts with a character.
	 *
	 * @param s The string to check.  Can be <jk>null</jk>.
	 * @param c The character to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and starts with the specified character.
	 */
	public static boolean startsWith(String s, char c) {
		if (nn(s)) {
			var i = s.length();
			if (i > 0)
				return s.charAt(0) == c;
		}
		return false;
	}

	/**
	 * Converts the specified array to a string.
	 *
	 * @param o The array to convert to a string.
	 * @return The array converted to a string, or <jk>null</jk> if the object was null.
	 */
	public static String stringifyDeep(Object o) {
		if (o == null)
			return null;
		if (! isArray(o))
			return o.toString();
		if (o.getClass().getComponentType().isPrimitive())
			return PRIMITIVE_ARRAY_STRINGIFIERS.get(o.getClass()).apply(o);
		return Arrays.deepToString((Object[])o);
	}

	/**
	 * Strips the first and last character from a string.
	 *
	 * @param s The string to strip.
	 * @return The striped string, or the same string if the input was <jk>null</jk> or less than length 2.
	 */
	public static String strip(String s) {
		if (s == null || s.length() <= 1)
			return s;
		return s.substring(1, s.length() - 1);
	}

	/**
	 * Strips invalid characters such as CTRL characters from a string meant to be encoded
	 * as an HTTP header value.
	 *
	 * @param s The string to strip chars from.
	 * @return The string with invalid characters removed.
	 */
	public static String stripInvalidHttpHeaderChars(String s) {

		if (s == null)
			return null;

		var needsReplace = false;
		for (var i = 0; i < s.length() && ! needsReplace; i++)
			needsReplace |= httpHeaderChars.contains(s.charAt(i));

		if (! needsReplace)
			return s;

		var sb = new StringBuilder(s.length());
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (httpHeaderChars.contains(c))
				sb.append(c);
		}

		return sb.toString();
	}

	/**
	 * Returns the substring after the first occurrence of a separator.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	substringAfter(<jk>null</jk>, <js>"."</js>);              <jc>// null</jc>
	 * 	substringAfter(<js>"hello.world"</js>, <jk>null</jk>);    <jc>// ""</jc>
	 * 	substringAfter(<js>"hello.world"</js>, <js>"."</js>);     <jc>// "world"</jc>
	 * 	substringAfter(<js>"hello.world"</js>, <js>"xyz"</js>);   <jc>// ""</jc>
	 * </p>
	 *
	 * @param str The string to get a substring from.
	 * @param separator The separator string.
	 * @return The substring after the first occurrence of the separator, or empty string if separator not found.
	 */
	public static String substringAfter(String str, String separator) {
		if (isEmpty(str))
			return str;
		if (separator == null)
			return "";
		var pos = str.indexOf(separator);
		if (pos == -1)
			return "";
		return str.substring(pos + separator.length());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String joining and splitting methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the substring before the first occurrence of a separator.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	substringBefore(<jk>null</jk>, <js>"."</js>);              <jc>// null</jc>
	 * 	substringBefore(<js>"hello.world"</js>, <jk>null</jk>);    <jc>// "hello.world"</jc>
	 * 	substringBefore(<js>"hello.world"</js>, <js>"."</js>);     <jc>// "hello"</jc>
	 * 	substringBefore(<js>"hello.world"</js>, <js>"xyz"</js>);   <jc>// "hello.world"</jc>
	 * </p>
	 *
	 * @param str The string to get a substring from.
	 * @param separator The separator string.
	 * @return The substring before the first occurrence of the separator, or the original string if separator not found.
	 */
	public static String substringBefore(String str, String separator) {
		if (isEmpty(str) || separator == null)
			return str;
		var pos = str.indexOf(separator);
		if (pos == -1)
			return str;
		return str.substring(0, pos);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String cleaning and sanitization methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the substring between two delimiters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	substringBetween(<jk>null</jk>, <js>"&lt;"</js>, <js>"&gt;"</js>);              <jc>// null</jc>
	 * 	substringBetween(<js>"&lt;hello&gt;"</js>, <js>"&lt;"</js>, <js>"&gt;"</js>);   <jc>// "hello"</jc>
	 * 	substringBetween(<js>"&lt;hello&gt;"</js>, <js>"["</js>, <js>"]"</js>);         <jc>// null</jc>
	 * </p>
	 *
	 * @param str The string to get a substring from.
	 * @param open The opening delimiter.
	 * @param close The closing delimiter.
	 * @return The substring between the delimiters, or <jk>null</jk> if delimiters not found.
	 */
	public static String substringBetween(String str, String open, String close) {
		if (str == null || open == null || close == null)
			return null;
		var start = str.indexOf(open);
		if (start == -1)
			return null;
		var end = str.indexOf(close, start + open.length());
		if (end == -1)
			return null;
		return str.substring(start + open.length(), end);
	}

	/**
	 * Swaps the case of all characters in a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	swapCase(<js>"Hello World"</js>);   <jc>// "hELLO wORLD"</jc>
	 * 	swapCase(<js>"ABC123xyz"</js>);     <jc>// "abc123XYZ"</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @return The string with case swapped, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String swapCase(String str) {
		if (str == null)
			return null;
		var sb = new StringBuilder(str.length());
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			if (Character.isUpperCase(c))
				sb.append(Character.toLowerCase(c));
			else if (Character.isLowerCase(c))
				sb.append(Character.toUpperCase(c));
			else
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Converts the specified object to a comma-delimited list.
	 *
	 * @param o The object to convert.
	 * @return The specified object as a comma-delimited list.
	 */
	public static String toCdl(Object o) {
		if (o == null)
			return null;
		if (isArray(o)) {
			var sb = new StringBuilder();
			for (int i = 0, j = Array.getLength(o); i < j; i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(Array.get(o, i));
			}
			return sb.toString();
		}
		if (o instanceof Collection<?> c)
			return join(c, ", ");
		return o.toString();
	}

	/**
	 * Converts the specified byte into a 2 hexadecimal characters.
	 *
	 * @param b The number to convert to hex.
	 * @return A <code><jk>char</jk>[2]</code> containing the specified characters.
	 */
	public static String toHex(byte b) {
		var c = new char[2];
		var v = b & 0xFF;
		c[0] = hexArray[v >>> 4];
		c[1] = hexArray[v & 0x0F];
		return new String(c);
	}

	/**
	 * Converts a byte array into a simple hexadecimal character string.
	 *
	 * @param bytes The bytes to convert to hexadecimal.
	 * @return A new string consisting of hexadecimal characters.
	 */
	public static String toHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (var element : bytes) {
			var v = element & 0xFF;
			sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
		}
		return sb.toString();
	}

	/**
	 * Converts the contents of the specified input stream to a hex string.
	 *
	 * @param is The input stream to convert.
	 * @return The hex string representation of the input stream contents, or <jk>null</jk> if the stream is <jk>null</jk>.
	 */
	public static String toHex(InputStream is) {
		return safe(() -> is == null ? null : toHex(readBytes(is)));
	}

	/**
	 * Converts the specified number into a 2 hexadecimal characters.
	 *
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[2]</code> containing the specified characters.
	 */
	public static char[] toHex2(int num) {
		if (num < 0 || num > 255)
			throw new NumberFormatException("toHex2 can only be used on numbers between 0 and 255");
		var n = new char[2];
		var a = num % 16;
		n[1] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		a = (num / 16) % 16;
		n[0] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		return n;
	}

	/**
	 * Converts the specified number into a 4 hexadecimal characters.
	 *
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[4]</code> containing the specified characters.
	 * @throws NumberFormatException If the number is negative.
	 */
	public static char[] toHex4(int num) {
		if (num < 0)
			throw new NumberFormatException("toHex4 can only be used on non-negative numbers");
		var n = new char[4];
		var a = num % 16;
		n[3] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		var base = 16;
		for (var i = 1; i < 4; i++) {
			a = (num / base) % 16;
			base <<= 4;
			n[3 - i] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		}
		return n;
	}

	/**
	 * Converts the specified number into a 8 hexadecimal characters.
	 *
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[8]</code> containing the specified characters.
	 * @throws NumberFormatException If the number is negative.
	 */
	public static char[] toHex8(long num) {
		if (num < 0)
			throw new NumberFormatException("toHex8 can only be used on non-negative numbers");
		var n = new char[8];
		var a = num % 16;
		n[7] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		var base = 16;
		for (var i = 1; i < 8; i++) {
			a = (num / base) % 16;
			base <<= 4;
			n[7 - i] = (char)(a > 9 ? 'A' + a - 10 : '0' + a);
		}
		return n;
	}

	/**
	 * Converts the specified object to an ISO8601 date string.
	 *
	 * @param c The object to convert.
	 * @return The converted object.
	 */
	public static String toIsoDate(Calendar c) {
		if (c == null) {
			return null;
		}
		// Convert Calendar to ZonedDateTime and format as ISO8601 date (YYYY-MM-DD)
		ZonedDateTime zdt = c.toInstant().atZone(c.getTimeZone().toZoneId());
		return zdt.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * Converts the specified object to an ISO8601 date-time string.
	 *
	 * @param c The object to convert.
	 * @return The converted object.
	 */
	public static String toIsoDateTime(Calendar c) {
		if (c == null) {
			return null;
		}
		// Convert Calendar to ZonedDateTime and format as ISO8601 date-time with timezone
		ZonedDateTime zdt = c.toInstant().atZone(c.getTimeZone().toZoneId());
		return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	/**
	 * Converts the specified bytes into a readable string.
	 *
	 * @param b The number to convert to hex.
	 * @return A <code><jk>char</jk>[2]</code> containing the specified characters.
	 */
	public static String toReadableBytes(byte[] b) {
		var sb = new StringBuilder();
		for (var b2 : b)
			sb.append((b2 < ' ' || b2 > 'z') ? String.format("[%02X]", b2) : (char)b2 + "   ");
		sb.append("\n");
		for (var b2 : b)
			sb.append(String.format("[%02X]", b2));
		return sb.toString();
	}

	/**
	 * Same as {@link #toHex(byte[])} but puts spaces between the byte strings.
	 *
	 * @param bytes The bytes to convert to hexadecimal.
	 * @return A new string consisting of hexadecimal characters.
	 */
	public static String toSpacedHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 3);
		for (var j = 0; j < bytes.length; j++) {
			if (j > 0)
				sb.append(' ');
			var v = bytes[j] & 0xFF;
			sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
		}
		return sb.toString();
	}

	/**
	 * Converts the specified object to a URI.
	 *
	 * @param o The object to convert to a URI.
	 * @return A new URI, or the same object if the object was already a URI, or
	 */
	public static URI toURI(Object o) {
		if (o == null || o instanceof URI)
			return (URI)o;
		try {
			return new URI(o.toString());
		} catch (URISyntaxException e) {
			throw toRex(e);
		}
	}

	/**
	 * Converts the specified byte array to a UTF-8 string.
	 *
	 * @param b The byte array to convert.
	 * @return The UTF-8 string representation, or <jk>null</jk> if the array is <jk>null</jk>.
	 */
	public static String toUtf8(byte[] b) {
		return b == null ? null : new String(b, UTF8);
	}

	/**
	 * Converts the contents of the specified input stream to a UTF-8 string.
	 *
	 * @param is The input stream to convert.
	 * @return The UTF-8 string representation of the input stream contents, or <jk>null</jk> if the stream is <jk>null</jk>.
	 */
	public static String toUtf8(InputStream is) {
		return safe(() -> is == null ? null : new String(readBytes(is), UTF8));
	}

	/**
	 * Same as {@link String#trim()} but prevents <c>NullPointerExceptions</c>.
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public static String trim(String s) {
		if (s == null)
			return null;
		return s.trim();
	}

	/**
	 * Trims whitespace characters from the end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public static String trimEnd(String s) {
		if (nn(s))
			while (isNotEmpty(s) && isWhitespace(s.charAt(s.length() - 1)))
				s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from the beginning of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimLeadingSlashes(String s) {
		if (s == null)
			return null;
		while (isNotEmpty(s) && s.charAt(0) == '/')
			s = s.substring(1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from both the start and end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimSlashes(String s) {
		if (s == null)
			return null;
		if (s.isEmpty())
			return s;
		while (endsWith(s, '/'))
			s = s.substring(0, s.length() - 1);
		while (isNotEmpty(s) && s.charAt(0) == '/')  // NOSONAR - NPE not possible here.
			s = s.substring(1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> and space characters from both the start and end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimSlashesAndSpaces(String s) {
		if (s == null)
			return null;
		while (isNotEmpty(s) && (s.charAt(s.length() - 1) == '/' || isWhitespace(s.charAt(s.length() - 1))))
			s = s.substring(0, s.length() - 1);
		while (isNotEmpty(s) && (s.charAt(0) == '/' || isWhitespace(s.charAt(0))))
			s = s.substring(1);
		return s;
	}

	/**
	 * Trims whitespace characters from the beginning of the specified string.
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public static String trimStart(String s) {
		if (nn(s))
			while (isNotEmpty(s) && isWhitespace(s.charAt(0)))
				s = s.substring(1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from the end of the specified string.
	 *
	 * @param s The string to trim.
	 * @return A new trimmed string, or the same string if no trimming was necessary.
	 */
	public static String trimTrailingSlashes(String s) {
		if (s == null)
			return null;
		while (endsWith(s, '/'))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * Uncapitalizes the first character of a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	uncapitalize(<jk>null</jk>);          <jc>// null</jc>
	 * 	uncapitalize(<js>""</js>);            <jc>// ""</jc>
	 * 	uncapitalize(<js>"Hello"</js>);       <jc>// "hello"</jc>
	 * 	uncapitalize(<js>"hello"</js>);       <jc>// "hello"</jc>
	 * 	uncapitalize(<js>"HELLO"</js>);       <jc>// "hELLO"</jc>
	 * </p>
	 *
	 * @param str The string to uncapitalize.
	 * @return The string with the first character uncapitalized, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String uncapitalize(String str) {
		if (isEmpty(str))
			return str;
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Removes escape characters from the specified characters.
	 *
	 * @param s The string to remove escape characters from.
	 * @param escaped The characters escaped.
	 * @return A new string if characters were removed, or the same string if not or if the input was <jk>null</jk>.
	 */
	public static String unEscapeChars(String s, AsciiSet escaped) {
		if (s == null || s.isEmpty())
			return s;
		var count = 0;
		for (var i = 0; i < s.length(); i++)
			if (escaped.contains(s.charAt(i)))
				count++;
		if (count == 0)
			return s;
		var sb = new StringBuffer(s.length() - count);
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);

			if (c == '\\') {
				if (i + 1 != s.length()) {  // NOSONAR - Intentional.
					var c2 = s.charAt(i + 1);
					if (escaped.contains(c2)) {
						i++;  // NOSONAR - Intentional.
					} else if (c2 == '\\') {
						sb.append('\\');
						i++;  // NOSONAR - Intentional.
					}
				}
			}
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	/**
	 * Creates an escaped-unicode sequence (e.g. <js>"\\u1234"</js>) for the specified character.
	 *
	 * @param c The character to create a sequence for.
	 * @return An escaped-unicode sequence.
	 */
	public static String unicodeSequence(char c) {
		var sb = new StringBuilder(6);
		sb.append('\\').append('u');
		for (var cc : toHex4(c))
			sb.append(cc);
		return sb.toString();
	}

	/**
	 * Decodes a <c>application/x-www-form-urlencoded</c> string using <c>UTF-8</c> encoding scheme.
	 *
	 * @param s The string to decode.
	 * @return The decoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlDecode(String s) {

		if (s == null)
			return s;

		var needsDecode = false;
		for (var i = 0; i < s.length() && ! needsDecode; i++) {
			var c = s.charAt(i);
			if (c == '+' || c == '%')
				needsDecode = true;
		}

		if (needsDecode) {
			try {
				return URLDecoder.decode(s, "UTF-8");
			} catch (@SuppressWarnings("unused") UnsupportedEncodingException e) {/* Won't happen */}
		}
		return s;
	}

	/**
	 * Encodes a <c>application/x-www-form-urlencoded</c> string using <c>UTF-8</c> encoding scheme.
	 *
	 * @param s The string to encode.
	 * @return The encoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlEncode(String s) {

		if (s == null)
			return null;

		var needsEncode = false;

		for (var i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode |= (! unencodedChars.contains(s.charAt(i)));

		if (needsEncode) {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (@SuppressWarnings("unused") UnsupportedEncodingException e) {/* Won't happen */}
		}

		return s;
	}

	/**
	 * Same as {@link #urlEncode(String)} except only escapes characters that absolutely need to be escaped.
	 *
	 * @param s The string to escape.
	 * @return The encoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlEncodeLax(String s) {
		if (s == null)
			return null;
		var needsEncode = false;
		for (var i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode |= (! unencodedCharsLax.contains(s.charAt(i)));
		if (needsEncode) {
			var sb = new StringBuilder(s.length() * 2);
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				if (unencodedCharsLax.contains(c))
					sb.append(c);
				else if (c == ' ')
					sb.append("+");
				else if (c <= 127)
					sb.append('%').append(toHex2(c));
				else
					try {
						sb.append(URLEncoder.encode("" + c, "UTF-8"));  // Yuck.
					} catch (@SuppressWarnings("unused") UnsupportedEncodingException e) {
						// Not possible.
					}
			}
			s = sb.toString();
		}
		return s;
	}

	/**
	 * Similar to {@link URLEncoder#encode(String, String)} but doesn't encode <js>"/"</js> characters.
	 *
	 * @param o The object to encode.
	 * @return The URL encoded string, or <jk>null</jk> if the object was null.
	 */
	public static String urlEncodePath(Object o) {

		if (o == null)
			return null;

		var s = s(o);

		var needsEncode = false;
		for (var i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode = URL_ENCODE_PATHINFO_VALIDCHARS.contains(s.charAt(i));
		if (! needsEncode)
			return s;

		var sb = new StringBuilder();
		var caw = new CharArrayWriter();
		var caseDiff = ('a' - 'A');

		for (var i = 0; i < s.length();) {
			var c = s.charAt(i);
			if (URL_ENCODE_PATHINFO_VALIDCHARS.contains(c)) {
				sb.append(c);
				i++;  // NOSONAR - Intentional.
			} else {
				if (c == ' ') {
					sb.append('+');
					i++;  // NOSONAR - Intentional.
				} else {
					do {
						caw.write(c);
						if (c >= 0xD800 && c <= 0xDBFF) {
							if ((i + 1) < s.length()) {  // NOSONAR - Intentional.
								int d = s.charAt(i + 1);
								if (d >= 0xDC00 && d <= 0xDFFF) {
									caw.write(d);
									i++;  // NOSONAR - Intentional.
								}
							}
						}
						i++;  // NOSONAR - Intentional.
					} while (i < s.length() && ! URL_ENCODE_PATHINFO_VALIDCHARS.contains((c = s.charAt(i))));   // NOSONAR - Intentional.

					caw.flush();
					var s2 = new String(caw.toCharArray());
					var ba = s2.getBytes(UTF8);
					for (var element : ba) {
						sb.append('%');
						var ch = forDigit((element >> 4) & 0xF, 16);
						if (isLetter(ch)) {
							ch -= caseDiff;
						}
						sb.append(ch);
						ch = forDigit(element & 0xF, 16);
						if (isLetter(ch)) {
							ch -= caseDiff;
						}
						sb.append(ch);
					}
					caw.reset();
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Appends a string to a StringBuilder, creating a new one if null.
	 *
	 * @param sb The StringBuilder to append to, or <jk>null</jk> to create a new one.
	 * @param in The string to append.
	 * @return The StringBuilder with the string appended.
	 */
	private static StringBuilder append(StringBuilder sb, String in) {
		if (sb == null)
			return new StringBuilder(in);
		sb.append(in);
		return sb;
	}

	/**
	 * Converts an array to a List, handling both primitive and object arrays.
	 *
	 * @param array The array to convert.
	 * @return A List containing the array elements.
	 */
	private static List<Object> arrayAsList(Object array) {
		if (array.getClass().getComponentType().isPrimitive()) {
			var l = new ArrayList<>(Array.getLength(array));
			for (var i = 0; i < Array.getLength(array); i++)
				l.add(Array.get(array, i));
			return l;
		}
		return l((Object[])array);
	}

	/**
	 * Converts an object to a readable string representation for formatting.
	 *
	 * @param o The object to convert.
	 * @return A readable string representation of the object.
	 */
	private static String convertToReadable(Object o) {
		if (o == null)
			return null;
		if (o instanceof Class<?> o2)
			return o2.getName();
		if (o instanceof Method o2)
			return o2.getName();
		if (isArray(o))
			return arrayAsList(o).stream().map(StringUtils::convertToReadable).collect(Collectors.joining(", ", "[", "]"));
		return o.toString();
	}

	/**
	 * Finds the first non-whitespace, non-comment character in a string.
	 *
	 * @param s The string to analyze.
	 * @return The first real character, or <c>-1</c> if none found.
	 */
	private static int firstRealCharacter(String s) {
		try (var r = new StringReader(s)) {
			var c = 0;
			while ((c = r.read()) != -1) {
				if (! isWhitespace(c)) {
					if (c == '/') {
						skipComments(r);
					} else {
						return c;
					}
				}
			}
			return -1;
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Determines the multiplier value based on the suffix character in a string.
	 *
	 * @param s The string to analyze for multiplier suffix.
	 * @return The multiplier value (1 if no valid suffix found).
	 */
	private static int multiplier(String s) {
		char c = isEmpty(s) ? null : s.charAt(s.length() - 1);  // NOSONAR - NPE not possible.
		if (c == 'G')
			return 1024 * 1024 * 1024;
		if (c == 'M')
			return 1024 * 1024;
		if (c == 'K')
			return 1024;
		if (c == 'g')
			return 1000 * 1000 * 1000;
		if (c == 'm')
			return 1000 * 1000;
		if (c == 'k')
			return 1000;
		return 1;
	}

	/**
	 * Determines the long multiplier value based on the suffix character in a string.
	 *
	 * @param s The string to analyze for multiplier suffix.
	 * @return The multiplier value (1 if no valid suffix found).
	 */
	private static long multiplier2(String s) {
		char c = isEmpty(s) ? null : s.charAt(s.length() - 1);  // NOSONAR - NPE not possible.
		if (c == 'P')
			return 1024 * 1024 * 1024 * 1024 * 1024l;
		if (c == 'T')
			return 1024 * 1024 * 1024 * 1024l;
		if (c == 'G')
			return 1024 * 1024 * 1024l;
		if (c == 'M')
			return 1024 * 1024l;
		if (c == 'K')
			return 1024l;
		if (c == 'p')
			return 1000 * 1000 * 1000 * 1000 * 1000l;
		if (c == 't')
			return 1000 * 1000 * 1000 * 1000l;
		if (c == 'g')
			return 1000 * 1000 * 1000l;
		if (c == 'm')
			return 1000 * 1000l;
		if (c == 'k')
			return 1000l;
		return 1;
	}

	/**
	 * Skips over comment sequences in a StringReader.
	 *
	 * @param r The StringReader positioned at the start of a comment.
	 * @throws IOException If an I/O error occurs.
	 */
	private static void skipComments(StringReader r) throws IOException {
		var c = r.read();
		//  "/* */" style comments
		if (c == '*') {
			while (c != -1)
				if ((c = r.read()) == '*')
					if ((c = r.read()) == '/')  // NOSONAR - Intentional.
						return;
			//  "//" style comments
		} else if (c == '/') {
			while (c != -1) {
				c = r.read();
				if (c == -1 || c == '\n')
					return;
			}
		}
	}

	/**
	 * Gets or creates an AsciiSet for escaping the specified character.
	 *
	 * @param c The character to create an escape set for.
	 * @return An AsciiSet containing the character and backslash.
	 */
	static AsciiSet getEscapeSet(char c) {
		return ESCAPE_SETS.computeIfAbsent(c, key -> AsciiSet.create().chars(key, '\\').build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Additional utility methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the string to lowercase if not null.
	 *
	 * @param s The string to convert.
	 * @return The lowercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String lc(String s) {
		return s == null ? null : s.toLowerCase();
	}

	/**
	 * Converts the string to uppercase if not null.
	 *
	 * @param s The string to convert.
	 * @return The uppercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String uc(String s) {
		return s == null ? null : s.toUpperCase();
	}

	/**
	 * Tests two objects for case-insensitive string equality.
	 *
	 * <p>Converts both objects to strings using {@link Object#toString()} before comparison.
	 *
	 * @param a Object 1.
	 * @param b Object 2.
	 * @return <jk>true</jk> if both objects are equal ignoring case.
	 */
	public static boolean eqic(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		return a.toString().equalsIgnoreCase(b.toString());
	}

	/**
	 * Adds the appropriate indefinite article ('a' or 'an') before a word.
	 *
	 * <p>Uses a simple vowel-based rule: 'an' if the word starts with a vowel, 'a' otherwise.
	 *
	 * @param subject The word to articlize.
	 * @return The word with 'a' or 'an' prepended.
	 */
	public static String articlized(String subject) {
		var vowels = AsciiSet.of("AEIOUaeiou");
		return (vowels.contains(subject.charAt(0)) ? "an " : "a ") + subject;
	}

	/**
	 * Returns the first non-blank string in the array.
	 *
	 * @param vals The strings to check.
	 * @return The first non-blank string, or <jk>null</jk> if all values were blank or <jk>null</jk>.
	 */
	public static String firstNonBlank(String...vals) {
		for (var v : vals) {
			if (isNotBlank(v))
				return v;
		}
		return null;
	}

	/**
	 * Converts a comma-delimited string to a list.
	 *
	 * @param s The comma-delimited string.
	 * @return A new modifiable list. Never <jk>null</jk>.
	 */
	public static List<String> cdlToList(String s) {
		return split(s);
	}

	/**
	 * Combines values into a simple comma-delimited string.
	 *
	 * @param values The values to join.
	 * @return A comma-delimited string.
	 */
	public static String join(String...values) {
		return join(values, ',');
	}

	/**
	 * Combines collection values into a simple comma-delimited string.
	 *
	 * @param values The values to join.
	 * @return A comma-delimited string.
	 */
	public static String join(Collection<?> values) {
		return joine(toList(values), ',');
	}

	/**
	 * Null-safe not-contains check for multiple string values.
	 *
	 * @param s The string to search.
	 * @param values The values to search for.
	 * @return <jk>true</jk> if the string does not contain any of the values.
	 */
	public static boolean notContains(String s, String...values) {
		return ! contains(s, values);
	}

	/**
	 * Converts a comma-delimited string to a set.
	 *
	 * @param s The comma-delimited string.
	 * @return A new {@link LinkedHashSet}. Never <jk>null</jk>.
	 */
	public static LinkedHashSet<String> cdlToSet(String s) {
		return split(s).stream().collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Takes a supplier of any type and returns a {@link Supplier}{@code <String>}.
	 *
	 * <p>Useful when passing arguments to loggers.
	 *
	 * @param s The supplier.
	 * @return A string supplier that calls {@link #readable(Object)} on the supplied value.
	 */
	public static Supplier<String> stringSupplier(Supplier<?> s) {
		return () -> readable(s.get());
	}

	/**
	 * Converts an arbitrary object to a readable string format suitable for debugging and testing.
	 *
	 * <p>This method provides intelligent formatting for various Java types, recursively processing
	 * nested structures to create human-readable representations. It's extensively used throughout
	 * the Juneau framework for test assertions and debugging output.</p>
	 *
	 * <h5 class='section'>Type-Specific Formatting:</h5>
	 * <ul>
	 * 	<li><b>null:</b> Returns <js>null</js></li>
	 * 	<li><b>Optional:</b> Recursively formats the contained value (or <js>null</js> if empty)</li>
	 * 	<li><b>Collections:</b> Formats as <js>"[item1,item2,item3]"</js> with comma-separated elements</li>
	 * 	<li><b>Maps:</b> Formats as <js>"{key1=value1,key2=value2}"</js> with comma-separated entries</li>
	 * 	<li><b>Map.Entry:</b> Formats as <js>"key=value"</js></li>
	 * 	<li><b>Arrays:</b> Converts to list format <js>"[item1,item2,item3]"</js></li>
	 * 	<li><b>Iterables/Iterators/Enumerations:</b> Converts to list and formats recursively</li>
	 * 	<li><b>GregorianCalendar:</b> Formats as ISO instant timestamp</li>
	 * 	<li><b>Date:</b> Formats as ISO instant string (e.g., <js>"2023-12-25T10:30:00Z"</js>)</li>
	 * 	<li><b>InputStream:</b> Converts to hexadecimal representation</li>
	 * 	<li><b>Reader:</b> Reads content and returns as string</li>
	 * 	<li><b>File:</b> Reads file content and returns as string</li>
	 * 	<li><b>byte[]:</b> Converts to hexadecimal representation</li>
	 * 	<li><b>Enum:</b> Returns the enum name via {@link Enum#name()}</li>
	 * 	<li><b>All other types:</b> Uses {@link Object#toString()}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Collections</jc>
	 * 	readable(List.of("a", "b", "c")) <jc>// Returns: "[a,b,c]"</jc>
	 * 	readable(Set.of(1, 2, 3)) <jc>// Returns: "[1,2,3]" (order may vary)</jc>
	 *
	 * 	<jc>// Maps</jc>
	 * 	readable(Map.of("foo", "bar", "baz", 123)) <jc>// Returns: "{foo=bar,baz=123}"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	readable(ints(1, 2, 3)) <jc>// Returns: "[1,2,3]"</jc>
	 * 	readable(new String[]{"a", "b"}) <jc>// Returns: "[a,b]"</jc>
	 *
	 * 	<jc>// Nested structures</jc>
	 * 	readable(List.of(Map.of("x", 1), Set.of("a", "b"))) <jc>// Returns: "[{x=1},[a,b]]"</jc>
	 *
	 * 	<jc>// Special types</jc>
	 * 	readable(Optional.of("test")) <jc>// Returns: "test"</jc>
	 * 	readable(Optional.empty()) <jc>// Returns: null</jc>
	 * 	readable(new Date(1640995200000L)) <jc>// Returns: "2022-01-01T00:00:00Z"</jc>
	 * 	readable(MyEnum.FOO) <jc>// Returns: "FOO"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Recursive Processing:</h5>
	 * <p>The method recursively processes nested structures, so complex objects containing
	 * collections, maps, and arrays are fully flattened into readable format. This makes it
	 * ideal for test assertions where you need to compare complex object structures.</p>
	 *
	 * <h5 class='section'>Error Handling:</h5>
	 * <p>IO operations (reading files, streams) are wrapped in safe() calls, converting
	 * any exceptions to RuntimeExceptions. Binary data (InputStreams, byte arrays) is
	 * converted to hexadecimal representation for readability.</p>
	 *
	 * @param o The object to convert to readable format. Can be <jk>null</jk>.
	 * @return A readable string representation of the object, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String readable(Object o) {
		if (o == null)
			return null;
		if (o instanceof Optional<?> o2)
			return readable(o2.orElse(null));
		if (o instanceof Collection<?> o2)
			return o2.stream().map(StringUtils::readable).collect(Collectors.joining(",", "[", "]"));
		if (o instanceof Map<?,?> o2)
			return o2.entrySet().stream().map(StringUtils::readable).collect(Collectors.joining(",", "{", "}"));
		if (o instanceof Map.Entry<?,?> o2)
			return readable(o2.getKey()) + '=' + readable(o2.getValue());
		if (o instanceof Iterable<?> o2)
			return readable(toList(o2));
		if (o instanceof Iterator<?> o2)
			return readable(toList(o2));
		if (o instanceof Enumeration<?> o2)
			return readable(toList(o2));
		if (o instanceof GregorianCalendar o2)
			return o2.toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
		if (o instanceof Date o2)
			return o2.toInstant().toString();
		if (o instanceof InputStream o2)
			return toHex(o2);
		if (o instanceof Reader o2)
			return safe(() -> read(o2));
		if (o instanceof File o2)
			return safe(() -> read(o2));
		if (o instanceof byte[] o2)
			return toHex(o2);
		if (o instanceof Enum o2)
			return o2.name();
		if (o instanceof Class o2)
			return scn(o2);
		if (o instanceof Executable o2) {
			var sb = new StringBuilder(64);
			sb.append(o2 instanceof Constructor ? scn(o2.getDeclaringClass()) : o2.getName()).append('(');
			var pt = o2.getParameterTypes();
			for (var i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(scn(pt[i]));
			}
			sb.append(')');
			return sb.toString();
		}
		if (isArray(o)) {
			var l = list();
			for (var i = 0; i < Array.getLength(o); i++) {
				l.add(Array.get(o, i));
			}
			return readable(l);
		}
		return o.toString();
	}

	/**
	 * Constructor.
	 */
	protected StringUtils() {}
}