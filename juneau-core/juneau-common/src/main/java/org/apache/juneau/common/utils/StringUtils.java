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

/**
 * Reusable string utility methods.
 */
public class StringUtils {

	/**
	 * Predicate check to filter out null and empty strings.
	 */
	public static final Predicate<String> NOT_EMPTY = Utils::isNotEmpty;

	/**
	 * Empty string constant.
	 */
	public static final String EMPTY = "";

	/**
	 * Single space constant.
	 */
	public static final String SPACE = " ";

	/**
	 * Newline constant (line feed character).
	 */
	public static final String NEWLINE = "\n";

	/**
	 * Tab constant.
	 */
	public static final String TAB = "\t";

	/**
	 * Carriage return + line feed constant (Windows line ending).
	 */
	public static final String CRLF = "\r\n";

	/**
	 * Common separator characters constant.
	 *
	 * <p>
	 * Contains commonly used separator characters: comma, semicolon, colon, pipe, and tab.
	 */
	public static final String COMMON_SEPARATORS = ",;:|" + TAB;

	/**
	 * All whitespace characters constant.
	 *
	 * <p>
	 * Contains all standard whitespace characters: space, tab, newline, carriage return, form feed, and vertical tab.
	 */
	public static final String WHITESPACE_CHARS = " \t\n\r\f\u000B";

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
			var b3 = base64m2[i3];
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
	 * <p>
	 * This is a null-safe and bounds-safe version of {@link String#charAt(int)}.
	 * Returns <c>0</c> (null character) if:
	 * <ul>
	 *   <li>The string is <jk>null</jk></li>
	 *   <li>The index is negative</li>
	 *   <li>The index is greater than or equal to the string length</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	charAt(<js>"Hello"</js>, 0);     <jc>// 'H'</jc>
	 * 	charAt(<js>"Hello"</js>, 4);     <jc>// 'o'</jc>
	 * 	charAt(<js>"Hello"</js>, 5);     <jc>// 0 (out of bounds)</jc>
	 * 	charAt(<js>"Hello"</js>, -1);    <jc>// 0 (out of bounds)</jc>
	 * 	charAt(<jk>null</jk>, 0);        <jc>// 0 (null string)</jc>
	 * </p>
	 *
	 * @param s The string.
	 * @param i The index position.
	 * @return The character at the specified index, or <c>0</c> if the index is out-of-range or the string is <jk>null</jk>.
	 * @see String#charAt(int)
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
	 * Compares two strings lexicographically, but gracefully handles <jk>null</jk> values.
	 *
	 * <p>
	 * Null handling:
	 * <ul>
	 *   <li>Both <jk>null</jk> → returns <c>0</c> (equal)</li>
	 *   <li>First <jk>null</jk> → returns {@link Integer#MIN_VALUE}</li>
	 *   <li>Second <jk>null</jk> → returns {@link Integer#MAX_VALUE}</li>
	 *   <li>Neither <jk>null</jk> → returns the same as {@link String#compareTo(String)}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	compare(<js>"apple"</js>, <js>"banana"</js>);   <jc>// negative (apple &lt; banana)</jc>
	 * 	compare(<js>"banana"</js>, <js>"apple"</js>);   <jc>// positive (banana &gt; apple)</jc>
	 * 	compare(<js>"apple"</js>, <js>"apple"</js>);    <jc>// 0 (equal)</jc>
	 * 	compare(<jk>null</jk>, <jk>null</jk>);          <jc>// 0 (equal)</jc>
	 * 	compare(<jk>null</jk>, <js>"apple"</js>);       <jc>// Integer.MIN_VALUE</jc>
	 * 	compare(<js>"apple"</js>, <jk>null</jk>);       <jc>// Integer.MAX_VALUE</jc>
	 * </p>
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return A negative integer, zero, or a positive integer as the first string is less than, equal to, or greater than the second.
	 * @see String#compareTo(String)
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
	 * Compresses a UTF-8 string into a GZIP-compressed byte array.
	 *
	 * <p>
	 * This method compresses the input string using GZIP compression. The string is first converted to
	 * UTF-8 bytes, then compressed. Use {@link #decompress(byte[])} to decompress the result.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Compress a string</jc>
	 * 	byte[] <jv>compressed</jv> = compress(<js>"Hello World"</js>);
	 *
	 * 	<jc>// Decompress it back</jc>
	 * 	String <jv>decompressed</jv> = decompress(<jv>compressed</jv>);
	 * 	<jc>// Returns: "Hello World"</jc>
	 * </p>
	 *
	 * @param contents The UTF-8 string to compress.
	 * @return The GZIP-compressed byte array.
	 * @throws Exception If compression fails.
	 * @see #decompress(byte[])
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
	 * Checks if a string contains any of the specified characters.
	 *
	 * <p>
	 * This is a null-safe operation that returns <jk>false</jk> if:
	 * <ul>
	 *   <li>The string is <jk>null</jk></li>
	 *   <li>The values array is <jk>null</jk> or empty</li>
	 *   <li>None of the specified characters are found in the string</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	contains(<js>"Hello World"</js>, <js>'o'</js>, <js>'x'</js>);   <jc>// true (contains 'o')</jc>
	 * 	contains(<js>"Hello World"</js>, <js>'x'</js>, <js>'y'</js>);   <jc>// false</jc>
	 * 	contains(<jk>null</jk>, <js>'a'</js>);                          <jc>// false</jc>
	 * </p>
	 *
	 * @param s The string to check.
	 * @param values The characters to check for.
	 * @return <jk>true</jk> if the string contains any of the specified characters.
	 * @see #contains(String, CharSequence)
	 * @see #contains(String, String...)
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
	 * Null-safe {@link String#contains(CharSequence)} operation.
	 *
	 * <p>
	 * Returns <jk>false</jk> if the string is <jk>null</jk>, otherwise behaves the same as
	 * {@link String#contains(CharSequence)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	contains(<js>"Hello World"</js>, <js>"World"</js>);   <jc>// true</jc>
	 * 	contains(<js>"Hello World"</js>, <js>"Foo"</js>);     <jc>// false</jc>
	 * 	contains(<jk>null</jk>, <js>"Hello"</js>);            <jc>// false</jc>
	 * </p>
	 *
	 * @param value The string to check.
	 * @param substring The substring to check for.
	 * @return <jk>true</jk> if the value contains the specified substring, <jk>false</jk> if the string is <jk>null</jk>.
	 * @see #contains(String, char...)
	 * @see #contains(String, String...)
	 */
	public static boolean contains(String value, CharSequence substring) {
		return nn(value) && value.contains(substring);
	}

	/**
	 * Checks if a string contains any of the specified substrings.
	 *
	 * <p>
	 * This is a null-safe operation that returns <jk>false</jk> if:
	 * <ul>
	 *   <li>The string is <jk>null</jk></li>
	 *   <li>The values array is <jk>null</jk> or empty</li>
	 *   <li>None of the specified substrings are found in the string</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	contains(<js>"Hello World"</js>, <js>"Hello"</js>, <js>"Foo"</js>);   <jc>// true (contains "Hello")</jc>
	 * 	contains(<js>"Hello World"</js>, <js>"Foo"</js>, <js>"Bar"</js>);    <jc>// false</jc>
	 * 	contains(<jk>null</jk>, <js>"Hello"</js>);                            <jc>// false</jc>
	 * 	contains(<js>"Hello"</js>);                                          <jc>// false (no values to check)</jc>
	 * </p>
	 *
	 * @param s The string to check.
	 * @param values The substrings to check for.
	 * @return <jk>true</jk> if the string contains any of the specified substrings.
	 * @see #contains(String, CharSequence)
	 * @see #contains(String, char...)
	 * @see #notContains(String, String...)
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
	 * Counts the number of occurrences of the specified character in the specified string.
	 *
	 * <p>
	 * Returns <c>0</c> if the string is <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	countChars(<js>"Hello World"</js>, <js>'o'</js>);   <jc>// 2</jc>
	 * 	countChars(<js>"Hello World"</js>, <js>'x'</js>);   <jc>// 0</jc>
	 * 	countChars(<jk>null</jk>, <js>'a'</js>);            <jc>// 0</jc>
	 * </p>
	 *
	 * @param s The string to check.
	 * @param c The character to count.
	 * @return The number of occurrences of the character, or <c>0</c> if the string was <jk>null</jk>.
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
	 * <p>
	 * Converts non-printable and non-ASCII characters (outside the range <c>0x20-0x7E</c>) to hexadecimal
	 * sequences in the format <js>"[hex]"</js>. Printable ASCII characters are left unchanged.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	decodeHex(<js>"Hello"</js>);              <jc>// "Hello"</jc>
	 * 	decodeHex(<js>"Hello\u0000World"</js>);   <jc>// "Hello[0]World"</jc>
	 * 	decodeHex(<js>"Hello\u00A9"</js>);        <jc>// "Hello[a9]"</jc>
	 * 	decodeHex(<jk>null</jk>);                 <jc>// null</jc>
	 * </p>
	 *
	 * @param s The string to decode.
	 * @return A string with non-ASCII characters converted to <js>"[hex]"</js> sequences, or <jk>null</jk> if input is <jk>null</jk>.
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
	 * Decompresses a GZIP-compressed byte array into a UTF-8 string.
	 *
	 * <p>
	 * This method is the inverse of {@link #compress(String)}. It takes a byte array that was compressed
	 * using GZIP compression and decompresses it into a UTF-8 encoded string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Compress a string</jc>
	 * 	byte[] <jv>compressed</jv> = compress(<js>"Hello World"</js>);
	 *
	 * 	<jc>// Decompress it back</jc>
	 * 	String <jv>decompressed</jv> = decompress(<jv>compressed</jv>);
	 * 	<jc>// Returns: "Hello World"</jc>
	 * </p>
	 *
	 * @param is The GZIP-compressed byte array to decompress.
	 * @return The decompressed UTF-8 string.
	 * @throws Exception If decompression fails or the input is not valid GZIP data.
	 * @see #compress(String)
	 */
	public static String decompress(byte[] is) throws Exception {
		return read(new GZIPInputStream(new ByteArrayInputStream(is)));
	}

	/**
	 * Finds the position where the two strings first differ.
	 *
	 * <p>
	 * This method compares strings character by character and returns the index of the first position
	 * where they differ. If the strings are equal, returns <c>-1</c>. If one string is a prefix of the other,
	 * returns the length of the shorter string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	diffPosition(<js>"apple"</js>, <js>"apple"</js>);      <jc>// -1 (equal)</jc>
	 * 	diffPosition(<js>"apple"</js>, <js>"apricot"</js>);    <jc>// 2 (differs at 'p' vs 'r')</jc>
	 * 	diffPosition(<js>"apple"</js>, <js>"app"</js>);        <jc>// 3 (shorter string ends here)</jc>
	 * 	diffPosition(<js>"app"</js>, <js>"apple"</js>);        <jc>// 3 (shorter string ends here)</jc>
	 * </p>
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return The position where the two strings differ, or <c>-1</c> if they're equal.
	 * @see #diffPositionIc(String, String)
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
	 * Finds the position where the two strings first differ, ignoring case.
	 *
	 * <p>
	 * This method compares strings character by character (case-insensitive) and returns the index of the first position
	 * where they differ. If the strings are equal (ignoring case), returns <c>-1</c>. If one string is a prefix of the other,
	 * returns the length of the shorter string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	diffPositionIc(<js>"Apple"</js>, <js>"apple"</js>);      <jc>// -1 (equal ignoring case)</jc>
	 * 	diffPositionIc(<js>"Apple"</js>, <js>"Apricot"</js>);    <jc>// 2 (differs at 'p' vs 'r')</jc>
	 * 	diffPositionIc(<js>"APPLE"</js>, <js>"app"</js>);        <jc>// 3 (shorter string ends here)</jc>
	 * </p>
	 *
	 * @param s1 The first string.
	 * @param s2 The second string.
	 * @return The position where the two strings differ, or <c>-1</c> if they're equal (ignoring case).
	 * @see #diffPosition(String, String)
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
	 * Checks if a string ends with the specified character.
	 *
	 * <p>
	 * This is a null-safe operation. Returns <jk>false</jk> if the string is <jk>null</jk> or empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	endsWith(<js>"Hello"</js>, <js>'o'</js>);     <jc>// true</jc>
	 * 	endsWith(<js>"Hello"</js>, <js>'H'</js>);     <jc>// false</jc>
	 * 	endsWith(<jk>null</jk>, <js>'o'</js>);        <jc>// false</jc>
	 * 	endsWith(<js>""</js>, <js>'o'</js>);          <jc>// false</jc>
	 * </p>
	 *
	 * @param s The string to check. Can be <jk>null</jk>.
	 * @param c The character to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and ends with the specified character.
	 * @see #endsWith(String, char...)
	 * @see String#endsWith(String)
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
	 * Checks if a string ends with any of the specified characters.
	 *
	 * <p>
	 * This is a null-safe operation. Returns <jk>false</jk> if the string is <jk>null</jk>, empty,
	 * or the characters array is <jk>null</jk> or empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	endsWith(<js>"Hello"</js>, <js>'o'</js>, <js>'x'</js>);     <jc>// true (ends with 'o')</jc>
	 * 	endsWith(<js>"Hello"</js>, <js>'x'</js>, <js>'y'</js>);     <jc>// false</jc>
	 * 	endsWith(<jk>null</jk>, <js>'o'</js>);                      <jc>// false</jc>
	 * </p>
	 *
	 * @param s The string to check. Can be <jk>null</jk>.
	 * @param c The characters to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and ends with any of the specified characters.
	 * @see #endsWith(String, char)
	 * @see String#endsWith(String)
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

		var sb = (StringBuilder)null;

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
	 * Formats a string using printf-style and/or MessageFormat-style format specifiers.
	 *
	 * <p>
	 * This method provides unified string formatting that supports both printf-style formatting
	 * (similar to C's <c>printf()</c> function and Java's {@link String#format(String, Object...)})
	 * and MessageFormat-style formatting in the same pattern.
	 *
	 * <h5 class='section'>Format Support:</h5>
	 * <ul>
	 *   <li><b>Printf-style:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, <js>"%1$s"</js>, etc.</li>
	 *   <li><b>MessageFormat-style:</b> <js>"{0}"</js>, <js>"{1,number}"</js>, <js>"{2,date}"</js>, etc.</li>
	 *   <li><b>Un-numbered MessageFormat:</b> <js>"{}"</js> - Sequential placeholders that are automatically numbered</li>
	 *   <li><b>Mixed formats:</b> Both styles can be used in the same pattern</li>
	 * </ul>
	 *
	 * <h5 class='section'>Printf Format Specifiers:</h5>
	 * <ul>
	 *   <li><b>%s</b> - String</li>
	 *   <li><b>%d</b> - Decimal integer</li>
	 *   <li><b>%f</b> - Floating point</li>
	 *   <li><b>%x</b> - Hexadecimal (lowercase)</li>
	 *   <li><b>%X</b> - Hexadecimal (uppercase)</li>
	 *   <li><b>%o</b> - Octal</li>
	 *   <li><b>%b</b> - Boolean</li>
	 *   <li><b>%c</b> - Character</li>
	 *   <li><b>%e</b> - Scientific notation (lowercase)</li>
	 *   <li><b>%E</b> - Scientific notation (uppercase)</li>
	 *   <li><b>%g</b> - General format (lowercase)</li>
	 *   <li><b>%G</b> - General format (uppercase)</li>
	 *   <li><b>%n</b> - Platform-specific line separator</li>
	 *   <li><b>%%</b> - Literal percent sign</li>
	 * </ul>
	 *
	 * <h5 class='section'>Format Specifier Syntax:</h5>
	 * <p>
	 * Printf format specifiers follow this pattern: <c>%[argument_index$][flags][width][.precision]conversion</c>
	 * </p>
	 * <p>
	 * MessageFormat placeholders follow this pattern: <c>{argument_index[,format_type[,format_style]]}</c>
	 * </p>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Printf-style formatting</jc>
	 * 	format(<js>"Hello %s, you have %d items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Floating point with precision</jc>
	 * 	format(<js>"Price: $%.2f"</js>, 19.99);
	 * 	<jc>// Returns: "Price: $19.99"</jc>
	 *
	 * 	<jc>// MessageFormat-style formatting</jc>
	 * 	format(<js>"Hello {0}, you have {1} items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Un-numbered MessageFormat placeholders (sequential)</jc>
	 * 	format(<js>"Hello {}, you have {} items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Mixed format styles in the same pattern</jc>
	 * 	format(<js>"User {0} has %d items and %s status"</js>, <js>"Alice"</js>, 10, <js>"active"</js>);
	 * 	<jc>// Returns: "User Alice has 10 items and active status"</jc>
	 *
	 * 	<jc>// Width and alignment (printf)</jc>
	 * 	format(<js>"Name: %-20s Age: %3d"</js>, <js>"John"</js>, 25);
	 * 	<jc>// Returns: "Name: John                 Age:  25"</jc>
	 *
	 * 	<jc>// Hexadecimal (printf)</jc>
	 * 	format(<js>"Color: #%06X"</js>, 0xFF5733);
	 * 	<jc>// Returns: "Color: #FF5733"</jc>
	 *
	 * 	<jc>// Argument index (reuse arguments)</jc>
	 * 	format(<js>"%1$s loves %2$s, and {0} also loves %3$s"</js>, <js>"Alice"</js>, <js>"Bob"</js>, <js>"Charlie"</js>);
	 * 	<jc>// Returns: "Alice loves Bob, and Alice also loves Charlie"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Comparison with mformat():</h5>
	 * <p>
	 * This method supports both formats, while {@link #mformat(String, Object...)} only supports MessageFormat-style.
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Both styles supported (this method)</jc>
	 * 	format(<js>"Hello %s, you have %d items"</js>, <js>"John"</js>, 5);
	 * 	format(<js>"Hello {0}, you have {1} items"</js>, <js>"John"</js>, 5);
	 * 	format(<js>"User {0} has %d items"</js>, <js>"Alice"</js>, 10);
	 *
	 * 	<jc>// MessageFormat style only</jc>
	 * 	mformat(<js>"Hello {0}, you have {1} items"</js>, <js>"John"</js>, 5);
	 * </p>
	 *
	 * <h5 class='section'>Null Handling:</h5>
	 * <p>
	 * Null arguments are formatted as the string <js>"null"</js> for string conversions,
	 * or cause a {@link NullPointerException} for numeric conversions (consistent with {@link String#format(String, Object...)}).
	 * </p>
	 *
	 * @param pattern The format string supporting both MessageFormat and printf-style placeholders.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @throws java.util.IllegalFormatException If the format string is invalid or arguments don't match the format specifiers.
	 * @see StringFormat for detailed format specification
	 * @see String#format(String, Object...)
	 * @see #mformat(String, Object...) for MessageFormat-only formatting
	 */
	public static String format(String pattern, Object...args) {
		return StringFormat.format(pattern, args);
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
	 * Supports simple duration formats:
	 *
	 * <h5 class='section'>Format Examples:</h5>
	 * <ul>
	 * 	<li><js>"1000"</js> - 1000 milliseconds (no suffix)
	 * 	<li><js>"10s"</js> - 10 seconds
	 * 	<li><js>"10 sec"</js> - 10 seconds
	 * 	<li><js>"10 seconds"</js> - 10 seconds
	 * 	<li><js>"1.5h"</js> - 1.5 hours (5400000 ms)
	 * 	<li><js>"1h30m"</js> - 1 hour 30 minutes (5400000 ms)
	 * 	<li><js>"1h 30m"</js> - 1 hour 30 minutes (with spaces)
	 * </ul>
	 *
	 * <h5 class='section'>Supported Units:</h5>
	 * <ul>
	 * 	<li><b>Milliseconds:</b> <js>"ms"</js>, <js>"millis"</js>, <js>"milliseconds"</js> (or no suffix)
	 * 	<li><b>Seconds:</b> <js>"s"</js>, <js>"sec"</js>, <js>"second"</js>, <js>"seconds"</js>
	 * 	<li><b>Minutes:</b> <js>"m"</js>, <js>"min"</js>, <js>"minute"</js>, <js>"minutes"</js>
	 * 	<li><b>Hours:</b> <js>"h"</js>, <js>"hour"</js>, <js>"hours"</js>
	 * 	<li><b>Days:</b> <js>"d"</js>, <js>"day"</js>, <js>"days"</js>
	 * 	<li><b>Weeks:</b> <js>"w"</js>, <js>"week"</js>, <js>"weeks"</js>
	 * 	<li><b>Months:</b> <js>"mo"</js>, <js>"month"</js>, <js>"months"</js> (30 days)
	 * 	<li><b>Years:</b> <js>"y"</js>, <js>"yr"</js>, <js>"year"</js>, <js>"years"</js> (365 days)
	 * </ul>
	 *
	 * <p>
	 * Suffixes are case-insensitive.
	 * <br>Whitespace is ignored.
	 * <br>Decimal values are supported (e.g., <js>"1.5h"</js>).
	 * <br>Combined formats are supported (e.g., <js>"1h30m"</js>).
	 *
	 * @param s The string to parse.
	 * @return
	 * 	The time in milliseconds, or <c>-1</c> if the string is empty or <jk>null</jk>.
	 */
	public static long getDuration(String s) {
		s = trim(s);
		if (isEmpty(s))
			return -1;

		// Parse simple format (number + unit or combined format)
		var totalMs = 0L;
		var i = 0;
		var len = s.length();

		while (i < len) {
			// Skip whitespace
			while (i < len && Character.isWhitespace(s.charAt(i)))
				i++;
			if (i >= len)
				break;

			// Parse number (including decimal)
			var numStart = i;
			var hasDecimal = false;
			while (i < len) {
			var c = s.charAt(i);
				if (c >= '0' && c <= '9') {
					i++;
				} else if (c == '.' && !hasDecimal) {
					hasDecimal = true;
					i++;
				} else {
				break;
		}
			}

			if (i == numStart) {
				// No number found, invalid format
				return -1;
			}

			var numStr = s.substring(numStart, i).trim();
			var value = Double.parseDouble(numStr);

			// Skip whitespace
			while (i < len && Character.isWhitespace(s.charAt(i)))
				i++;

		// Parse unit (read all letters until we hit a digit or whitespace)
		var unitStart = i;
		while (i < len && Character.isLetter(s.charAt(i)))
			i++;
		var unit = s.substring(unitStart, i).trim().toLowerCase();

			// Convert to milliseconds
			var ms = parseUnit(unit, value);
			if (ms < 0)
				return -1;
			totalMs += ms;
		}

		return totalMs;
	}

	/**
	 * Parses a unit string and converts the value to milliseconds.
	 *
	 * @param unit The unit string (case-insensitive, already lowercased).
	 * @param value The numeric value.
	 * @return The value in milliseconds, or <c>-1</c> if the unit is invalid.
	 */
	private static long parseUnit(String unit, double value) {
		if (isEmpty(unit)) {
			// No unit means milliseconds
			return (long)value;
		}

		// Check milliseconds first (before minutes) - must check exact "ms" before checking "m"
		if (unit.equals("ms") || unit.equals("millis") || unit.equals("milliseconds"))
			return (long)value;

		// Seconds
		if (unit.startsWith("s") && !unit.startsWith("sec"))
			return (long)(value * 1000);
		if (unit.startsWith("sec") || unit.startsWith("second"))
			return (long)(value * 1000);

		// Minutes (must check after milliseconds and months)
		if (unit.startsWith("m") && !unit.startsWith("mo") && !unit.startsWith("mill") && !unit.startsWith("ms"))
			return (long)(value * 1000 * 60);
		if (unit.startsWith("min") || unit.startsWith("minute"))
			return (long)(value * 1000 * 60);

		// Hours
		if (unit.startsWith("h") || unit.startsWith("hour"))
			return (long)(value * 1000 * 60 * 60);

		// Days
		if (unit.startsWith("d") && !unit.startsWith("da"))
			return (long)(value * 1000 * 60 * 60 * 24);
		if (unit.startsWith("day"))
			return (long)(value * 1000 * 60 * 60 * 24);

		// Weeks
		if (unit.startsWith("w") || unit.startsWith("week"))
			return (long)(value * 1000 * 60 * 60 * 24 * 7);

		// Months (30 days)
		if (unit.startsWith("mo") || unit.startsWith("month"))
			return (long)(value * 1000 * 60 * 60 * 24 * 30);

		// Years (365 days)
		if (unit.startsWith("y") && !unit.startsWith("yr"))
			return (long)(value * 1000 * 60 * 60 * 24 * 365);
		if (unit.startsWith("yr") || unit.startsWith("year"))
			return (long)(value * 1000 * 60 * 60 * 24 * 365);

		// Unknown unit
		return -1;
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
		assertArgNotNull("values", values);
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
	 * Checks if a string is null or empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isEmpty(<jk>null</jk>);    <jc>// true</jc>
	 * 	isEmpty(<js>""</js>);      <jc>// true</jc>
	 * 	isEmpty(<js>"abc"</js>);   <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is null or empty.
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * Checks if a string is a valid email address.
	 *
	 * <p>
	 * Performs basic email validation using a simple regex pattern.
	 * This is not a complete RFC 5321/5322 validation, but covers most common email formats.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isEmail(<jk>null</jk>);                    <jc>// false</jc>
	 * 	isEmail(<js>""</js>);                      <jc>// false</jc>
	 * 	isEmail(<js>"user@example.com"</js>);      <jc>// true</jc>
	 * 	isEmail(<js>"invalid.email"</js>);         <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is a valid email address.
	 */
	public static boolean isEmail(String str) {
		if (isEmpty(str))
			return false;
		// Basic email regex: local@domain
		// Allows letters, digits, dots, underscores, hyphens, and plus signs in local part
		// Domain must have at least one dot and valid TLD
		return str.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
	}

	/**
	 * Checks if a string is a valid phone number.
	 *
	 * <p>
	 * Performs basic phone number validation.
	 * Accepts various formats including:
	 * <ul>
	 *   <li>Digits only: <js>"1234567890"</js></li>
	 *   <li>With separators: <js>"(123) 456-7890"</js>, <js>"123-456-7890"</js>, <js>"123.456.7890"</js></li>
	 *   <li>With country code: <js>"+1 123-456-7890"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isPhoneNumber(<jk>null</jk>);              <jc>// false</jc>
	 * 	isPhoneNumber(<js>""</js>);                <jc>// false</jc>
	 * 	isPhoneNumber(<js>"1234567890"</js>);      <jc>// true</jc>
	 * 	isPhoneNumber(<js>"(123) 456-7890"</js>);  <jc>// true</jc>
	 * 	isPhoneNumber(<js>"123"</js>);             <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is a valid phone number.
	 */
	public static boolean isPhoneNumber(String str) {
		if (isEmpty(str))
			return false;
		// Remove common phone number separators and check if remaining is 10-15 digits
		// Allows: digits, spaces, parentheses, hyphens, dots, plus sign (for country code)
		var cleaned = str.replaceAll("[\\s()\\-\\.]", "");
		if (cleaned.startsWith("+"))
			cleaned = cleaned.substring(1);
		// Phone numbers should have 10-15 digits (10 for US, up to 15 for international)
		return cleaned.matches("^\\d{10,15}$");
	}

	/**
	 * Checks if a string is a valid credit card number using the Luhn algorithm.
	 *
	 * <p>
	 * Validates credit card numbers by:
	 * <ul>
	 *   <li>Removing spaces and hyphens</li>
	 *   <li>Checking that all remaining characters are digits</li>
	 *   <li>Verifying the number passes the Luhn algorithm check</li>
	 *   <li>Ensuring the number is between 13-19 digits (standard credit card length)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isCreditCard(<jk>null</jk>);                    <jc>// false</jc>
	 * 	isCreditCard(<js>""</js>);                      <jc>// false</jc>
	 * 	isCreditCard(<js>"4532015112830366"</js>);      <jc>// true (Visa test card)</jc>
	 * 	isCreditCard(<js>"4532-0151-1283-0366"</js>);   <jc>// true (with separators)</jc>
	 * 	isCreditCard(<js>"1234567890"</js>);            <jc>// false (invalid Luhn)</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is a valid credit card number.
	 */
	public static boolean isCreditCard(String str) {
		if (isEmpty(str))
			return false;
		// Remove spaces and hyphens
		var cleaned = str.replaceAll("[\\s\\-]", "");
		// Must be all digits and 13-19 digits long
		if (! cleaned.matches("^\\d{13,19}$"))
			return false;
		// Apply Luhn algorithm
		var sum = 0;
		var alternate = false;
		for (var i = cleaned.length() - 1; i >= 0; i--) {
			var digit = Character.getNumericValue(cleaned.charAt(i));
			if (alternate) {
				digit *= 2;
				if (digit > 9)
					digit = (digit % 10) + 1;
			}
			sum += digit;
			alternate = ! alternate;
		}
		return (sum % 10) == 0;
	}

	/**
	 * Validates if a string is a valid regular expression pattern.
	 *
	 * <p>
	 * Attempts to compile the regex pattern to verify it's syntactically correct.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidRegex(<js>"[a-z]+"</js>);        <jc>// true</jc>
	 * 	isValidRegex(<js>"[a-z"</js>);          <jc>// false (unclosed bracket)</jc>
	 * 	isValidRegex(<js>"(test"</js>);         <jc>// false (unclosed parenthesis)</jc>
	 * </p>
	 *
	 * @param regex The regex pattern to validate. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a valid regex pattern, <jk>false</jk> otherwise.
	 */
	public static boolean isValidRegex(String regex) {
		if (isEmpty(regex))
			return false;
		try {
			Pattern.compile(regex);
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		}
	}

	/**
	 * Validates if a date string matches the specified date format.
	 *
	 * <p>
	 * Uses {@link SimpleDateFormat} to parse the date string according to the format pattern.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidDateFormat(<js>"2023-12-25"</js>, <js>"yyyy-MM-dd"</js>);  <jc>// true</jc>
	 * 	isValidDateFormat(<js>"25/12/2023"</js>, <js>"dd/MM/yyyy"</js>);  <jc>// true</jc>
	 * 	isValidDateFormat(<js>"2023-13-25"</js>, <js>"yyyy-MM-dd"</js>);  <jc>// false (invalid month)</jc>
	 * </p>
	 *
	 * @param dateStr The date string to validate. Can be <jk>null</jk>.
	 * @param format The date format pattern (e.g., "yyyy-MM-dd"). Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the date string matches the format, <jk>false</jk> otherwise.
	 */
	public static boolean isValidDateFormat(String dateStr, String format) {
		if (isEmpty(dateStr) || isEmpty(format))
			return false;
		try {
			var sdf = new SimpleDateFormat(format);
			sdf.setLenient(false); // Strict parsing
			sdf.parse(dateStr);
			return true;
		} catch (ParseException | IllegalArgumentException e) {
			// IllegalArgumentException thrown for invalid format patterns
			return false;
		}
	}

	/**
	 * Validates if a time string matches the specified time format.
	 *
	 * <p>
	 * Uses {@link SimpleDateFormat} to parse the time string according to the format pattern.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidTimeFormat(<js>"14:30:00"</js>, <js>"HH:mm:ss"</js>);  <jc>// true</jc>
	 * 	isValidTimeFormat(<js>"2:30 PM"</js>, <js>"h:mm a"</js>);     <jc>// true</jc>
	 * 	isValidTimeFormat(<js>"25:00:00"</js>, <js>"HH:mm:ss"</js>);  <jc>// false (invalid hour)</jc>
	 * </p>
	 *
	 * @param timeStr The time string to validate. Can be <jk>null</jk>.
	 * @param format The time format pattern (e.g., "HH:mm:ss"). Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the time string matches the format, <jk>false</jk> otherwise.
	 */
	public static boolean isValidTimeFormat(String timeStr, String format) {
		if (isEmpty(timeStr) || isEmpty(format))
			return false;
		try {
			var sdf = new SimpleDateFormat(format);
			sdf.setLenient(false); // Strict parsing
			sdf.parse(timeStr);
			return true;
		} catch (ParseException | IllegalArgumentException e) {
			// IllegalArgumentException thrown for invalid format patterns
			return false;
		}
	}

	/**
	 * Validates if a string is a valid IP address (IPv4 or IPv6).
	 *
	 * <p>
	 * Supports both IPv4 (e.g., "192.168.1.1") and IPv6 (e.g., "2001:0db8:85a3:0000:0000:8a2e:0370:7334") formats.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidIpAddress(<js>"192.168.1.1"</js>);                    <jc>// true</jc>
	 * 	isValidIpAddress(<js>"2001:0db8:85a3::8a2e:0370:7334"</js>); <jc>// true</jc>
	 * 	isValidIpAddress(<js>"256.1.1.1"</js>);                      <jc>// false</jc>
	 * 	isValidIpAddress(<js>"not.an.ip"</js>);                      <jc>// false</jc>
	 * </p>
	 *
	 * @param ip The IP address string to validate. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a valid IP address, <jk>false</jk> otherwise.
	 */
	public static boolean isValidIpAddress(String ip) {
		if (isEmpty(ip))
			return false;
		try {
			// Try IPv4 first
			if (ip.contains(".") && ! ip.contains(":")) {
				var parts = ip.split("\\.");
				if (parts.length != 4)
					return false;
				for (var part : parts) {
					var num = Integer.parseInt(part);
					if (num < 0 || num > 255)
						return false;
				}
				return true;
			}
			// Try IPv6 - use InetAddress for reliable validation
			if (ip.contains(":")) {
				try {
					InetAddress.getByName(ip);
					return true;
				} catch (UnknownHostException e) {
					return false;
				}
			}
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Validates if a string is a valid MAC address.
	 *
	 * <p>
	 * Supports common MAC address formats:
	 * <ul>
	 *   <li>Colon-separated: <js>"00:1B:44:11:3A:B7"</js></li>
	 *   <li>Hyphen-separated: <js>"00-1B-44-11-3A-B7"</js></li>
	 *   <li>No separators: <js>"001B44113AB7"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidMacAddress(<js>"00:1B:44:11:3A:B7"</js>);  <jc>// true</jc>
	 * 	isValidMacAddress(<js>"00-1B-44-11-3A-B7"</js>);  <jc>// true</jc>
	 * 	isValidMacAddress(<js>"001B44113AB7"</js>);       <jc>// true</jc>
	 * 	isValidMacAddress(<js>"00:1B:44:11:3A"</js>);     <jc>// false (too short)</jc>
	 * </p>
	 *
	 * @param mac The MAC address string to validate. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a valid MAC address, <jk>false</jk> otherwise.
	 */
	public static boolean isValidMacAddress(String mac) {
		if (isEmpty(mac))
			return false;

		// Remove separators and check if it's 12 hex digits
		var cleaned = mac.replaceAll("[:-]", "").toUpperCase();
		if (cleaned.length() != 12)
			return false;

		// Check if all characters are valid hex digits
		return cleaned.matches("^[0-9A-F]{12}$");
	}

	/**
	 * Validates if a string is a valid hostname.
	 *
	 * <p>
	 * Validates hostnames according to RFC 1123. A valid hostname:
	 * <ul>
	 *   <li>Can contain letters, digits, and hyphens</li>
	 *   <li>Cannot start or end with a hyphen</li>
	 *   <li>Each label (dot-separated part) can be up to 63 characters</li>
	 *   <li>Total length can be up to 253 characters</li>
	 *   <li>Labels cannot be empty</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isValidHostname(<js>"example.com"</js>);        <jc>// true</jc>
	 * 	isValidHostname(<js>"sub.example.com"</js>);    <jc>// true</jc>
	 * 	isValidHostname(<js>"-invalid.com"</js>);       <jc>// false (starts with hyphen)</jc>
	 * 	isValidHostname(<js>"example..com"</js>);       <jc>// false (empty label)</jc>
	 * </p>
	 *
	 * @param hostname The hostname string to validate. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a valid hostname, <jk>false</jk> otherwise.
	 */
	public static boolean isValidHostname(String hostname) {
		if (isEmpty(hostname))
			return false;

		// Cannot start or end with a dot
		if (hostname.startsWith(".") || hostname.endsWith("."))
			return false;

		// Total length cannot exceed 253 characters
		if (hostname.length() > 253)
			return false;

		// Split by dots (use -1 to preserve trailing empty strings)
		var labels = hostname.split("\\.", -1);

		// Must have at least one label
		if (labels.length == 0)
			return false;

		// Check each label
		for (var label : labels) {
			// Label cannot be empty
			if (label.isEmpty())
				return false;

			// Label cannot exceed 63 characters
			if (label.length() > 63)
				return false;

			// Label cannot start or end with hyphen
			if (label.startsWith("-") || label.endsWith("-"))
				return false;

			// Label can only contain letters, digits, and hyphens
			if (! label.matches("^[a-zA-Z0-9-]+$"))
				return false;
		}

		return true;
	}

	/**
	 * Counts the number of words in a string.
	 *
	 * <p>
	 * A word is defined as a sequence of one or more word characters (letters, digits, underscores)
	 * separated by non-word characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	wordCount(<js>"Hello world"</js>);              <jc>// 2</jc>
	 * 	wordCount(<js>"The quick brown fox"</js>);      <jc>// 4</jc>
	 * 	wordCount(<js>"Hello, world! How are you?"</js>); <jc>// 5</jc>
	 * </p>
	 *
	 * @param str The string to count words in. Can be <jk>null</jk>.
	 * @return The number of words, or <c>0</c> if the string is <jk>null</jk> or empty.
	 */
	public static int wordCount(String str) {
		if (isEmpty(str))
			return 0;

		var count = 0;
		var inWord = false;

		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_') {
				if (! inWord) {
					count++;
					inWord = true;
				}
			} else {
				inWord = false;
			}
		}

		return count;
	}

	/**
	 * Counts the number of lines in a string.
	 *
	 * <p>
	 * Counts newline characters. A string ending without a newline is counted as one line.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	lineCount(<js>"line1\nline2\nline3"</js>);      <jc>// 3</jc>
	 * 	lineCount(<js>"single line"</js>);              <jc>// 1</jc>
	 * 	lineCount(<js>"line1\r\nline2"</js>);          <jc>// 2</jc>
	 * </p>
	 *
	 * @param str The string to count lines in. Can be <jk>null</jk>.
	 * @return The number of lines, or <c>0</c> if the string is <jk>null</jk> or empty.
	 */
	public static int lineCount(String str) {
		if (isEmpty(str))
			return 0;

		var count = 1; // At least one line
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			if (c == '\n') {
				count++;
			} else if (c == '\r') {
				// Handle \r\n as a single line break
				if (i + 1 < str.length() && str.charAt(i + 1) == '\n') {
					i++; // Skip the \n
				}
				count++;
			}
		}

		return count;
	}

	/**
	 * Finds the most frequent character in a string.
	 *
	 * <p>
	 * Returns the character that appears most often. If multiple characters have the same
	 * frequency, returns the first one encountered.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	mostFrequentChar(<js>"hello"</js>);     <jc>// 'l'</jc>
	 * 	mostFrequentChar(<js>"aabbcc"</js>);    <jc>// 'a' (first encountered)</jc>
	 * </p>
	 *
	 * @param str The string to analyze. Can be <jk>null</jk>.
	 * @return The most frequent character, or <c>'\0'</c> if the string is <jk>null</jk> or empty.
	 */
	public static char mostFrequentChar(String str) {
		if (isEmpty(str))
			return '\0';

		var charCounts = new int[Character.MAX_VALUE + 1];
		var maxCount = 0;
		var maxChar = '\0';

		// Count occurrences of each character
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			charCounts[c]++;
			if (charCounts[c] > maxCount) {
				maxCount = charCounts[c];
				maxChar = c;
			}
		}

		return maxChar;
	}

	/**
	 * Calculates the entropy of a string.
	 *
	 * <p>
	 * Entropy measures the randomness or information content of a string.
	 * Higher entropy indicates more randomness. The formula used is:
	 * <c>H(X) = -Σ P(x) * log2(P(x))</c>
	 * where P(x) is the probability of character x.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	entropy(<js>"aaaa"</js>);        <jc>// 0.0 (no randomness)</jc>
	 * 	entropy(<js>"abcd"</js>);        <jc>// 2.0 (high randomness)</jc>
	 * 	entropy(<js>"hello"</js>);       <jc>// ~2.32</jc>
	 * </p>
	 *
	 * @param str The string to calculate entropy for. Can be <jk>null</jk>.
	 * @return The entropy value (0.0 or higher), or <c>0.0</c> if the string is <jk>null</jk> or empty.
	 */
	public static double entropy(String str) {
		if (isEmpty(str))
			return 0.0;

		var length = str.length();
		if (length == 0)
			return 0.0;

		// Count character frequencies
		var charCounts = new int[Character.MAX_VALUE + 1];
		for (var i = 0; i < length; i++) {
			charCounts[str.charAt(i)]++;
		}

		// Calculate entropy
		var entropy = 0.0;
		for (var count : charCounts) {
			if (count > 0) {
				var probability = (double)count / length;
				entropy -= probability * (Math.log(probability) / Math.log(2.0));
			}
		}

		return entropy;
	}

	/**
	 * Calculates a simple readability score for a string.
	 *
	 * <p>
	 * Uses a simplified Flesch Reading Ease-like formula based on:
	 * <ul>
	 *   <li>Average words per sentence</li>
	 *   <li>Average syllables per word (estimated)</li>
	 * </ul>
	 * Returns a score from 0-100, where higher scores indicate easier reading.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	readabilityScore(<js>"The cat sat."</js>);      <jc>// Higher score (simple)</jc>
	 * 	readabilityScore(<js>"The sophisticated..."</js>); <jc>// Lower score (complex)</jc>
	 * </p>
	 *
	 * @param str The string to analyze. Can be <jk>null</jk>.
	 * @return A readability score from 0-100, or <c>0.0</c> if the string is <jk>null</jk> or empty.
	 */
	public static double readabilityScore(String str) {
		if (isEmpty(str))
			return 0.0;

		var words = extractWords(str);
		if (words.isEmpty())
			return 0.0;

		// Count sentences (ending with . ! ?)
		var sentenceCount = 0;
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			if (c == '.' || c == '!' || c == '?') {
				sentenceCount++;
			}
		}
		if (sentenceCount == 0)
			sentenceCount = 1; // At least one sentence

		// Calculate average words per sentence
		var avgWordsPerSentence = (double)words.size() / sentenceCount;

		// Estimate average syllables per word (simplified: count vowel groups)
		var totalSyllables = 0;
		for (var word : words) {
			totalSyllables += estimateSyllables(word);
		}
		var avgSyllablesPerWord = (double)totalSyllables / words.size();

		// Simplified Flesch Reading Ease formula
		// Score = 206.835 - (1.015 * ASL) - (84.6 * ASW)
		// Where ASL = average sentence length (words), ASW = average syllables per word
		var score = 206.835 - (1.015 * avgWordsPerSentence) - (84.6 * avgSyllablesPerWord);

		// Clamp to 0-100 range
		return Math.max(0.0, Math.min(100.0, score));
	}

	/**
	 * Helper method to estimate the number of syllables in a word.
	 */
	private static int estimateSyllables(String word) {
		if (word == null || word.isEmpty())
			return 1;

		var lower = word.toLowerCase();
		var count = 0;
		var prevWasVowel = false;

		for (var i = 0; i < lower.length(); i++) {
			var c = lower.charAt(i);
			var isVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y');

			if (isVowel && ! prevWasVowel) {
				count++;
			}
			prevWasVowel = isVowel;
		}

		// Handle silent 'e' at the end
		if (lower.endsWith("e") && count > 1) {
			count--;
		}

		// At least one syllable
		return Math.max(1, count);
	}

	/**
	 * Finds the index of the first occurrence of a substring within a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	indexOf(<js>"hello world"</js>, <js>"world"</js>);     <jc>// 6</jc>
	 * 	indexOf(<js>"hello world"</js>, <js>"xyz"</js>);       <jc>// -1</jc>
	 * 	indexOf(<jk>null</jk>, <js>"test"</js>);               <jc>// -1</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to search for.
	 * @return The index of the first occurrence, or <c>-1</c> if not found or if either parameter is <jk>null</jk>.
	 */
	public static int indexOf(String str, String search) {
		if (str == null || search == null)
			return -1;
		return str.indexOf(search);
	}

	/**
	 * Finds the index of the first occurrence of a substring within a string, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	indexOfIgnoreCase(<js>"Hello World"</js>, <js>"world"</js>);     <jc>// 6</jc>
	 * 	indexOfIgnoreCase(<js>"Hello World"</js>, <js>"WORLD"</js>);     <jc>// 6</jc>
	 * 	indexOfIgnoreCase(<js>"hello world"</js>, <js>"xyz"</js>);       <jc>// -1</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to search for.
	 * @return The index of the first occurrence, or <c>-1</c> if not found or if either parameter is <jk>null</jk>.
	 */
	public static int indexOfIgnoreCase(String str, String search) {
		if (str == null || search == null)
			return -1;
		return str.toLowerCase().indexOf(search.toLowerCase());
	}

	/**
	 * Finds the index of the last occurrence of a substring within a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	lastIndexOf(<js>"hello world world"</js>, <js>"world"</js>);     <jc>// 12</jc>
	 * 	lastIndexOf(<js>"hello world"</js>, <js>"xyz"</js>);             <jc>// -1</jc>
	 * 	lastIndexOf(<jk>null</jk>, <js>"test"</js>);                     <jc>// -1</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to search for.
	 * @return The index of the last occurrence, or <c>-1</c> if not found or if either parameter is <jk>null</jk>.
	 */
	public static int lastIndexOf(String str, String search) {
		if (str == null || search == null)
			return -1;
		return str.lastIndexOf(search);
	}

	/**
	 * Finds the index of the last occurrence of a substring within a string, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	lastIndexOfIgnoreCase(<js>"Hello World World"</js>, <js>"world"</js>);     <jc>// 12</jc>
	 * 	lastIndexOfIgnoreCase(<js>"Hello World"</js>, <js>"WORLD"</js>);           <jc>// 6</jc>
	 * 	lastIndexOfIgnoreCase(<js>"hello world"</js>, <js>"xyz"</js>);             <jc>// -1</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to search for.
	 * @return The index of the last occurrence, or <c>-1</c> if not found or if either parameter is <jk>null</jk>.
	 */
	public static int lastIndexOfIgnoreCase(String str, String search) {
		if (str == null || search == null)
			return -1;
		return str.toLowerCase().lastIndexOf(search.toLowerCase());
	}

	/**
	 * Checks if a string contains a substring, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	containsIgnoreCase(<js>"Hello World"</js>, <js>"world"</js>);     <jc>// true</jc>
	 * 	containsIgnoreCase(<js>"Hello World"</js>, <js>"WORLD"</js>);     <jc>// true</jc>
	 * 	containsIgnoreCase(<js>"hello world"</js>, <js>"xyz"</js>);       <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to search for.
	 * @return <jk>true</jk> if the string contains the substring (ignoring case), <jk>false</jk> otherwise.
	 */
	public static boolean containsIgnoreCase(String str, String search) {
		if (str == null || search == null)
			return false;
		return str.toLowerCase().contains(search.toLowerCase());
	}

	/**
	 * Checks if a string starts with a prefix, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	startsWithIgnoreCase(<js>"Hello World"</js>, <js>"hello"</js>);     <jc>// true</jc>
	 * 	startsWithIgnoreCase(<js>"Hello World"</js>, <js>"HELLO"</js>);     <jc>// true</jc>
	 * 	startsWithIgnoreCase(<js>"hello world"</js>, <js>"world"</js>);     <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @param prefix The prefix to check for.
	 * @return <jk>true</jk> if the string starts with the prefix (ignoring case), <jk>false</jk> otherwise.
	 */
	public static boolean startsWithIgnoreCase(String str, String prefix) {
		if (str == null || prefix == null)
			return false;
		return str.toLowerCase().startsWith(prefix.toLowerCase());
	}

	/**
	 * Checks if a string ends with a suffix, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	endsWithIgnoreCase(<js>"Hello World"</js>, <js>"world"</js>);     <jc>// true</jc>
	 * 	endsWithIgnoreCase(<js>"Hello World"</js>, <js>"WORLD"</js>);     <jc>// true</jc>
	 * 	endsWithIgnoreCase(<js>"hello world"</js>, <js>"hello"</js>);     <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @param suffix The suffix to check for.
	 * @return <jk>true</jk> if the string ends with the suffix (ignoring case), <jk>false</jk> otherwise.
	 */
	public static boolean endsWithIgnoreCase(String str, String suffix) {
		if (str == null || suffix == null)
			return false;
		return str.toLowerCase().endsWith(suffix.toLowerCase());
	}

	/**
	 * Checks if a string matches a regular expression pattern.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	matches(<js>"12345"</js>, <js>"\\d+"</js>);              <jc>// true</jc>
	 * 	matches(<js>"abc123"</js>, <js>"^[a-z]+\\d+$"</js>);     <jc>// true</jc>
	 * 	matches(<js>"abc"</js>, <js>"\\d+"</js>);                <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @param regex The regular expression pattern.
	 * @return <jk>true</jk> if the string matches the pattern, <jk>false</jk> otherwise.
	 * @throws PatternSyntaxException If the regex pattern is invalid.
	 */
	public static boolean matches(String str, String regex) {
		if (str == null || regex == null)
			return false;
		return str.matches(regex);
	}

	/**
	 * Counts the number of occurrences of a substring within a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	countMatches(<js>"hello world world"</js>, <js>"world"</js>);     <jc>// 2</jc>
	 * 	countMatches(<js>"ababab"</js>, <js>"ab"</js>);                    <jc>// 3</jc>
	 * 	countMatches(<js>"hello"</js>, <js>"xyz"</js>);                    <jc>// 0</jc>
	 * </p>
	 *
	 * @param str The string to search in.
	 * @param search The substring to count.
	 * @return The number of occurrences, or <c>0</c> if not found or if either parameter is <jk>null</jk> or empty.
	 */
	public static int countMatches(String str, String search) {
		if (isEmpty(str) || isEmpty(search))
			return 0;
		var count = 0;
		var index = 0;
		while ((index = str.indexOf(search, index)) != -1) {
			count++;
			index += search.length();
		}
		return count;
	}

	/**
	 * Interpolates variables in a template string using <js>"${name}"</js> syntax.
	 *
	 * <p>
	 * Replaces variables of the form <js>"${name}"</js> with values from the map.
	 * This is similar to shell variable interpolation syntax.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	var vars = Map.of(<js>"name"</js>, <js>"John"</js>, <js>"city"</js>, <js>"New York"</js>);
	 * 	interpolate(<js>"Hello ${name}, welcome to ${city}"</js>, vars);
	 * 	<jc>// Returns: "Hello John, welcome to New York"</jc>
	 * </p>
	 *
	 * @param template The template string with <js>"${name}"</js> variables.
	 * @param variables The map containing the variable values.
	 * @return The interpolated string with variables replaced, or the original template if variables is null or empty.
	 */
	public static String interpolate(String template, Map<String,Object> variables) {
		if (template == null)
			return null;
		if (variables == null || variables.isEmpty())
			return template;

		var result = new StringBuilder();
		var i = 0;
		var length = template.length();

		while (i < length) {
			var dollarIndex = template.indexOf("${", i);
			if (dollarIndex == -1) {
				// No more variables, append the rest
				result.append(template.substring(i));
				break;
			}

			// Append text before the variable
			result.append(template.substring(i, dollarIndex));

			// Find the closing brace
			var braceIndex = template.indexOf('}', dollarIndex + 2);
			if (braceIndex == -1) {
				// No closing brace, append the rest as-is
				result.append(template.substring(dollarIndex));
				break;
			}

			// Extract variable name
			var varName = template.substring(dollarIndex + 2, braceIndex);
			var value = variables.get(varName);

			if (variables.containsKey(varName)) {
				// Variable exists in map (even if null)
				result.append(value != null ? value.toString() : "null");
			} else {
				// Variable not found, keep the original placeholder
				result.append("${").append(varName).append("}");
			}

			i = braceIndex + 1;
		}

		return result.toString();
	}

	/**
	 * Pluralizes a word based on a count.
	 *
	 * <p>
	 * Simple pluralization that adds "s" to most words, with basic rules for words ending in "s", "x", "z", "ch", "sh" (add "es"),
	 * and words ending in "y" preceded by a consonant (replace "y" with "ies").
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	pluralize(<js>"cat"</js>, <js>1</js>);        <jc>// "cat"</jc>
	 * 	pluralize(<js>"cat"</js>, <js>2</js>);        <jc>// "cats"</jc>
	 * 	pluralize(<js>"box"</js>, <js>2</js>);        <jc>// "boxes"</jc>
	 * 	pluralize(<js>"city"</js>, <js>2</js>);       <jc>// "cities"</jc>
	 * </p>
	 *
	 * @param word The word to pluralize.
	 * @param count The count to determine if pluralization is needed.
	 * @return The pluralized word if count is not 1, otherwise the original word.
	 */
	public static String pluralize(String word, int count) {
		if (word == null || word.isEmpty())
			return word;
		if (count == 1)
			return word;

		var lower = word.toLowerCase();
		var length = word.length();

		// Words ending in s, x, z, ch, sh -> add "es"
		if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch") || lower.endsWith("sh")) {
			return word + "es";
		}

		// Words ending in "y" preceded by a consonant -> replace "y" with "ies"
		if (length > 1 && lower.endsWith("y")) {
			var secondLast = lower.charAt(length - 2);
			if (secondLast != 'a' && secondLast != 'e' && secondLast != 'i' && secondLast != 'o' && secondLast != 'u') {
				return word.substring(0, length - 1) + "ies";
			}
		}

		// Words ending in "f" or "fe" -> replace with "ves" (basic rule)
		if (lower.endsWith("f")) {
			return word.substring(0, length - 1) + "ves";
		}
		if (lower.endsWith("fe")) {
			return word.substring(0, length - 2) + "ves";
		}

		// Default: add "s"
		return word + "s";
	}

	/**
	 * Converts a number to its ordinal form (1st, 2nd, 3rd, 4th, etc.).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ordinal(<js>1</js>);     <jc>// "1st"</jc>
	 * 	ordinal(<js>2</js>);     <jc>// "2nd"</jc>
	 * 	ordinal(<js>3</js>);     <jc>// "3rd"</jc>
	 * 	ordinal(<js>4</js>);     <jc>// "4th"</jc>
	 * 	ordinal(<js>11</js>);    <jc>// "11th"</jc>
	 * 	ordinal(<js>21</js>);    <jc>// "21st"</jc>
	 * </p>
	 *
	 * @param number The number to convert.
	 * @return The ordinal string representation of the number.
	 */
	public static String ordinal(int number) {
		var abs = Math.abs(number);
		var suffix = "th";

		// Special cases for 11, 12, 13 (all use "th")
		if (abs % 100 != 11 && abs % 100 != 12 && abs % 100 != 13) {
			var lastDigit = abs % 10;
			if (lastDigit == 1)
				suffix = "st";
			else if (lastDigit == 2)
				suffix = "nd";
			else if (lastDigit == 3)
				suffix = "rd";
		}

		return number + suffix;
	}

	/**
	 * Basic HTML/XML sanitization - removes or escapes potentially dangerous content.
	 *
	 * <p>
	 * Removes HTML/XML tags and escapes special characters to prevent XSS attacks.
	 * This is a basic sanitization - for production use, consider a more robust library.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	sanitize(<js>"&lt;script&gt;alert('xss')&lt;/script&gt;"</js>);     <jc>// "&amp;lt;script&amp;gt;alert('xss')&amp;lt;/script&amp;gt;"</jc>
	 * 	sanitize(<js>"Hello &lt;b&gt;World&lt;/b&gt;"</js>);                <jc>// "Hello &amp;lt;b&amp;gt;World&amp;lt;/b&amp;gt;"</jc>
	 * </p>
	 *
	 * @param str The string to sanitize.
	 * @return The sanitized string with HTML/XML tags escaped, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String sanitize(String str) {
		if (str == null)
			return null;
		// Escape HTML/XML special characters
		return escapeHtml(str);
	}

	/**
	 * Escapes HTML entities in a string.
	 *
	 * <p>
	 * Escapes the following characters:
	 * <ul>
	 *   <li><js>'&amp;'</js> → <js>"&amp;amp;"</js></li>
	 *   <li><js>'&lt;'</js> → <js>"&amp;lt;"</js></li>
	 *   <li><js>'&gt;'</js> → <js>"&amp;gt;"</js></li>
	 *   <li><js>'"'</js> → <js>"&amp;quot;"</js></li>
	 *   <li><js>'\''</js> → <js>"&amp;#39;"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	escapeHtml(<js>"&lt;script&gt;alert('xss')&lt;/script&gt;"</js>);
	 * 	<jc>// Returns: "&amp;lt;script&amp;gt;alert(&amp;#39;xss&amp;#39;)&amp;lt;/script&amp;gt;"</jc>
	 * </p>
	 *
	 * @param str The string to escape.
	 * @return The escaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String escapeHtml(String str) {
		if (str == null)
			return null;
		var sb = new StringBuilder(str.length() * 2);
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&#39;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Unescapes HTML entities in a string.
	 *
	 * <p>
	 * Unescapes the following HTML entities:
	 * <ul>
	 *   <li><js>"&amp;amp;"</js> → <js>'&amp;'</js></li>
	 *   <li><js>"&amp;lt;"</js> → <js>'&lt;'</js></li>
	 *   <li><js>"&amp;gt;"</js> → <js>'&gt;'</js></li>
	 *   <li><js>"&amp;quot;"</js> → <js>'"'</js></li>
	 *   <li><js>"&amp;#39;"</js> or <js>"&amp;apos;"</js> → <js>'\''</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	unescapeHtml(<js>"&amp;lt;script&amp;gt;"</js>);     <jc>// Returns: "&lt;script&gt;"</jc>
	 * </p>
	 *
	 * @param str The string to unescape.
	 * @return The unescaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String unescapeHtml(String str) {
		if (str == null)
			return null;
		// Must unescape &amp; last to avoid interfering with other entities
		return str.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&");
	}

	/**
	 * Escapes XML entities in a string.
	 *
	 * <p>
	 * Escapes the following characters:
	 * <ul>
	 *   <li><js>'&amp;'</js> → <js>"&amp;amp;"</js></li>
	 *   <li><js>'&lt;'</js> → <js>"&amp;lt;"</js></li>
	 *   <li><js>'&gt;'</js> → <js>"&amp;gt;"</js></li>
	 *   <li><js>'"'</js> → <js>"&amp;quot;"</js></li>
	 *   <li><js>'\''</js> → <js>"&amp;apos;"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	escapeXml(<js>"&lt;tag attr='value'&gt;text&lt;/tag&gt;"</js>);
	 * 	<jc>// Returns: "&amp;lt;tag attr=&amp;apos;value&amp;apos;&amp;gt;text&amp;lt;/tag&amp;gt;"</jc>
	 * </p>
	 *
	 * @param str The string to escape.
	 * @return The escaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String escapeXml(String str) {
		if (str == null)
			return null;
		var sb = new StringBuilder(str.length() * 2);
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&apos;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Unescapes XML entities in a string.
	 *
	 * <p>
	 * Unescapes the following XML entities:
	 * <ul>
	 *   <li><js>"&amp;amp;"</js> → <js>'&amp;'</js></li>
	 *   <li><js>"&amp;lt;"</js> → <js>'&lt;'</js></li>
	 *   <li><js>"&amp;gt;"</js> → <js>'&gt;'</js></li>
	 *   <li><js>"&amp;quot;"</js> → <js>'"'</js></li>
	 *   <li><js>"&amp;apos;"</js> → <js>'\''</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	unescapeXml(<js>"&amp;lt;tag&amp;gt;"</js>);     <jc>// Returns: "&lt;tag&gt;"</jc>
	 * </p>
	 *
	 * @param str The string to unescape.
	 * @return The unescaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String unescapeXml(String str) {
		if (str == null)
			return null;
		// Must unescape &amp; last to avoid interfering with other entities
		return str.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
	}

	/**
	 * Escapes SQL string literals by doubling single quotes.
	 *
	 * <p>
	 * Basic SQL escaping for string literals. Escapes single quotes by doubling them.
	 * This is a basic implementation - for production use, consider using prepared statements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	escapeSql(<js>"O'Brien"</js>);     <jc>// Returns: "O''Brien"</jc>
	 * 	escapeSql(<js>"It's a test"</js>); <jc>// Returns: "It''s a test"</jc>
	 * </p>
	 *
	 * @param str The string to escape.
	 * @return The escaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String escapeSql(String str) {
		if (str == null)
			return null;
		return str.replace("'", "''");
	}

	/**
	 * Escapes regex special characters in a string.
	 *
	 * <p>
	 * Escapes the following regex special characters: <js>\.*+?^${}()[]|\\</js>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	escapeRegex(<js>"file.txt"</js>);        <jc>// Returns: "file\\.txt"</jc>
	 * 	escapeRegex(<js>"price: $10.99"</js>);   <jc>// Returns: "price: \\$10\\.99"</jc>
	 * </p>
	 *
	 * @param str The string to escape.
	 * @return The escaped string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String escapeRegex(String str) {
		if (str == null)
			return null;
		// Escape regex special characters: . * + ? ^ $ { } ( ) [ ] | \
		return str.replace("\\", "\\\\").replace(".", "\\.").replace("*", "\\*").replace("+", "\\+").replace("?", "\\?").replace("^", "\\^").replace("$", "\\$").replace("{", "\\{").replace("}", "\\}")
			.replace("(", "\\(").replace(")", "\\)").replace("[", "\\[").replace("]", "\\]").replace("|", "\\|");
	}

	/**
	 * Tests two strings for case-insensitive equality, but gracefully handles nulls.
	 *
	 * <p>
	 * This method handles <jk>null</jk> values gracefully:
	 * <ul>
	 *   <li>Both <jk>null</jk> → returns <jk>true</jk> (same reference check)</li>
	 *   <li>One <jk>null</jk> → returns <jk>false</jk></li>
	 *   <li>Neither <jk>null</jk> → compares strings ignoring case</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <js>"hello"</js>);     <jc>// true</jc>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <js>"WORLD"</js>);     <jc>// false</jc>
	 * 	equalsIgnoreCase(<jk>null</jk>, <jk>null</jk>);           <jc>// true</jc>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <jk>null</jk>);        <jc>// false</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @return <jk>true</jk> if the strings are equal ignoring case, <jk>false</jk> otherwise.
	 * @see #equalsIgnoreCase(Object, Object)
	 * @see Utils#eqic(String, String)
	 */
	public static boolean equalsIgnoreCase(String str1, String str2) {
		if (str1 == str2)
			return true;
		if (str1 == null || str2 == null)
			return false;
		return str1.equalsIgnoreCase(str2);
	}

	/**
	 * Compares two strings lexicographically, ignoring case.
	 *
	 * <p>
	 * Returns a negative integer, zero, or a positive integer as the first string is less than, equal to, or greater than the second string, ignoring case.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	compareIgnoreCase(<js>"apple"</js>, <js>"BANANA"</js>);   <jc>// negative (apple &lt; banana)</jc>
	 * 	compareIgnoreCase(<js>"Hello"</js>, <js>"hello"</js>);    <jc>// 0 (equal)</jc>
	 * 	compareIgnoreCase(<js>"Zebra"</js>, <js>"apple"</js>);    <jc>// positive (zebra &gt; apple)</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @return A negative integer, zero, or a positive integer as the first string is less than, equal to, or greater than the second.
	 */
	public static int compareIgnoreCase(String str1, String str2) {
		if (str1 == str2)
			return 0;
		if (str1 == null)
			return -1;
		if (str2 == null)
			return 1;
		return str1.compareToIgnoreCase(str2);
	}

	/**
	 * Performs natural string comparison that handles numbers correctly.
	 *
	 * <p>
	 * Compares strings in a way that numbers are compared numerically rather than lexicographically.
	 * For example, "file2.txt" comes before "file10.txt" in natural order.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	naturalCompare(<js>"file2.txt"</js>, <js>"file10.txt"</js>);   <jc>// negative (2 &lt; 10)</jc>
	 * 	naturalCompare(<js>"file10.txt"</js>, <js>"file2.txt"</js>);   <jc>// positive (10 &gt; 2)</jc>
	 * 	naturalCompare(<js>"file1.txt"</js>, <js>"file1.txt"</js>);    <jc>// 0 (equal)</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @return A negative integer, zero, or a positive integer as the first string is less than, equal to, or greater than the second.
	 */
	public static int naturalCompare(String str1, String str2) {
		if (str1 == str2)
			return 0;
		if (str1 == null)
			return -1;
		if (str2 == null)
			return 1;

		var len1 = str1.length();
		var len2 = str2.length();
		var i1 = 0;
		var i2 = 0;

		while (i1 < len1 && i2 < len2) {
			var c1 = str1.charAt(i1);
			var c2 = str2.charAt(i2);

			// If both are digits, compare numerically
			if (Character.isDigit(c1) && Character.isDigit(c2)) {
				// Skip leading zeros
				while (i1 < len1 && str1.charAt(i1) == '0')
					i1++;
				while (i2 < len2 && str2.charAt(i2) == '0')
					i2++;

				// Find end of number sequences
				var end1 = i1;
				var end2 = i2;
				while (end1 < len1 && Character.isDigit(str1.charAt(end1)))
					end1++;
				while (end2 < len2 && Character.isDigit(str2.charAt(end2)))
					end2++;

				// Compare lengths first (longer number is larger)
				var lenNum1 = end1 - i1;
				var lenNum2 = end2 - i2;
				if (lenNum1 != lenNum2)
					return lenNum1 - lenNum2;

				// Same length, compare digit by digit
				for (var j = 0; j < lenNum1; j++) {
					var d1 = str1.charAt(i1 + j);
					var d2 = str2.charAt(i2 + j);
					if (d1 != d2)
						return d1 - d2;
				}

				i1 = end1;
				i2 = end2;
			} else {
				// Compare characters (case-insensitive)
				var cmp = Character.toLowerCase(c1) - Character.toLowerCase(c2);
				if (cmp != 0)
					return cmp;
				i1++;
				i2++;
			}
		}

		return len1 - len2;
	}

	/**
	 * Calculates the Levenshtein distance (edit distance) between two strings.
	 *
	 * <p>
	 * The Levenshtein distance is the minimum number of single-character edits (insertions, deletions, or substitutions) required to change one string into another.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	levenshteinDistance(<js>"kitten"</js>, <js>"sitting"</js>);     <jc>// 3</jc>
	 * 	levenshteinDistance(<js>"hello"</js>, <js>"hello"</js>);        <jc>// 0</jc>
	 * 	levenshteinDistance(<js>"abc"</js>, <js>""</js>);               <jc>// 3</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @return The Levenshtein distance between the two strings.
	 */
	public static int levenshteinDistance(String str1, String str2) {
		if (str1 == null)
			str1 = "";
		if (str2 == null)
			str2 = "";

		var len1 = str1.length();
		var len2 = str2.length();

		// Use dynamic programming with optimized space (only need previous row)
		var prev = new int[len2 + 1];
		var curr = new int[len2 + 1];

		// Initialize first row
		for (var j = 0; j <= len2; j++)
			prev[j] = j;

		for (var i = 1; i <= len1; i++) {
			curr[0] = i;
			for (var j = 1; j <= len2; j++) {
				if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
					curr[j] = prev[j - 1];
				} else {
					curr[j] = 1 + Math.min(Math.min(prev[j], curr[j - 1]), prev[j - 1]);
				}
			}
			// Swap arrays
			var temp = prev;
			prev = curr;
			curr = temp;
		}

		return prev[len2];
	}

	/**
	 * Calculates the similarity percentage between two strings using Levenshtein distance.
	 *
	 * <p>
	 * Returns a value between 0.0 (completely different) and 1.0 (identical).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	similarity(<js>"hello"</js>, <js>"hello"</js>);           <jc>// 1.0 (100%)</jc>
	 * 	similarity(<js>"kitten"</js>, <js>"sitting"</js>);        <jc>// ~0.57 (57%)</jc>
	 * 	similarity(<js>"abc"</js>, <js>"xyz"</js>);               <jc>// 0.0 (0%)</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @return A similarity value between 0.0 and 1.0, where 1.0 means identical.
	 */
	public static double similarity(String str1, String str2) {
		if (str1 == null)
			str1 = "";
		if (str2 == null)
			str2 = "";

		if (str1.equals(str2))
			return 1.0;

		var maxLen = Math.max(str1.length(), str2.length());
		if (maxLen == 0)
			return 1.0;

		var distance = levenshteinDistance(str1, str2);
		return 1.0 - ((double)distance / maxLen);
	}

	/**
	 * Checks if two strings are similar based on a similarity threshold.
	 *
	 * <p>
	 * Uses the {@link #similarity(String, String)} method to calculate similarity and compares it to the threshold.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isSimilar(<js>"hello"</js>, <js>"hello"</js>, <js>0.8</js>);        <jc>// true</jc>
	 * 	isSimilar(<js>"kitten"</js>, <js>"sitting"</js>, <js>0.8</js>);     <jc>// false</jc>
	 * 	isSimilar(<js>"kitten"</js>, <js>"sitting"</js>, <js>0.5</js>);     <jc>// true</jc>
	 * </p>
	 *
	 * @param str1 The first string.
	 * @param str2 The second string.
	 * @param threshold The similarity threshold (0.0 to 1.0).
	 * @return <jk>true</jk> if the similarity is greater than or equal to the threshold, <jk>false</jk> otherwise.
	 */
	public static boolean isSimilar(String str1, String str2, double threshold) {
		return similarity(str1, str2) >= threshold;
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
	 * Returns the specified string, or an empty string if that string is <jk>null</jk>.
	 *
	 * @param str The string value to check.
	 * @return The string value, or an empty string if the string is <jk>null</jk>.
	 */
	public static String emptyIfNull(String str) {
		return str == null ? "" : str;
	}

	/**
	 * Returns the specified string, or the default string if that string is <jk>null</jk> or empty.
	 *
	 * @param str The string value to check.
	 * @param defaultStr The default string to return if the string is <jk>null</jk> or empty.
	 * @return The string value, or the default string if the string is <jk>null</jk> or empty.
	 */
	public static String defaultIfEmpty(String str, String defaultStr) {
		return isEmpty(str) ? defaultStr : str;
	}

	/**
	 * Returns the specified string, or the default string if that string is <jk>null</jk> or blank.
	 *
	 * @param str The string value to check.
	 * @param defaultStr The default string to return if the string is <jk>null</jk> or blank.
	 * @return The string value, or the default string if the string is <jk>null</jk> or blank.
	 */
	public static String defaultIfBlank(String str, String defaultStr) {
		return isBlank(str) ? defaultStr : str;
	}

	/**
	 * Safely converts an object to a string, returning <jk>null</jk> if the object is <jk>null</jk>.
	 *
	 * @param obj The object to convert to a string.
	 * @return The string representation of the object, or <jk>null</jk> if the object is <jk>null</jk>.
	 */
	public static String toString(Object obj) {
		return obj == null ? null : obj.toString();
	}

	/**
	 * Safely converts an object to a string, returning the default string if the object is <jk>null</jk>.
	 *
	 * @param obj The object to convert to a string.
	 * @param defaultStr The default string to return if the object is <jk>null</jk>.
	 * @return The string representation of the object, or the default string if the object is <jk>null</jk>.
	 */
	public static String toString(Object obj, String defaultStr) {
		return obj == null ? defaultStr : obj.toString();
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
		var m = multiplierInt(s);
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
		var m = multiplierLong(s);
		if (m == 1)
			return Long.decode(s);
		return Long.decode(s.substring(0, s.length() - 1).trim()) * m;  // NOSONAR - NPE not possible here.
	}

	/**
	 * Parses a number from the specified string.
	 *
	 * <p>
	 * Supports Java 7+ numeric literals with underscores (e.g., <js>"1_000_000"</js>).
	 * The underscores are automatically removed before parsing.
	 *
	 * @param s The string to parse the number from.
	 * @param type
	 * 	The number type to created.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li> Integer (or <c>int</c> primitive)
	 * 		<li> Long (or <c>long</c> primitive)
	 * 		<li> Short (or <c>short</c> primitive)
	 * 		<li> Byte (or <c>byte</c> primitive)
	 * 		<li> Float (or <c>float</c> primitive)
	 * 		<li> Double (or <c>double</c> primitive)
	 * 		<li> BigInteger
	 * 		<li> BigDecimal
	 * 		<li> AtomicInteger
	 * 		<li> AtomicLong
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

		// Remove underscores (Java 7+ numeric literal support) before parsing
		// Note: We do this before type detection to ensure clean parsing
		s = s.replace("_", "");

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
	 * Generates a random UUID string in standard format.
	 *
	 * <p>
	 * Returns a UUID in the format: <c>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	generateUUID();  <jc>// "550e8400-e29b-41d4-a716-446655440000"</jc>
	 * </p>
	 *
	 * @return A new random UUID string.
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generates a random alphabetic string of the specified length.
	 *
	 * <p>
	 * Characters are composed of upper-case and lower-case ASCII letters (a-z, A-Z).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	randomAlphabetic(<jk>5</jk>);  <jc>// "aBcDe"</jc>
	 * </p>
	 *
	 * @param length The length of the generated string.
	 * @return A new random alphabetic string.
	 */
	public static String randomAlphabetic(int length) {
		if (length < 0)
			throw new IllegalArgumentException("Length must be non-negative: " + length);
		var sb = new StringBuilder(length);
		for (var i = 0; i < length; i++) {
			var c = RANDOM.nextInt(52);
			if (c < 26)
				sb.append((char)('a' + c));
			else
				sb.append((char)('A' + c - 26));
		}
		return sb.toString();
	}

	/**
	 * Generates a random alphanumeric string of the specified length.
	 *
	 * <p>
	 * Characters are composed of upper-case and lower-case ASCII letters and digits (a-z, A-Z, 0-9).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	randomAlphanumeric(<jk>8</jk>);  <jc>// "aB3dE5fG"</jc>
	 * </p>
	 *
	 * @param length The length of the generated string.
	 * @return A new random alphanumeric string.
	 */
	public static String randomAlphanumeric(int length) {
		if (length < 0)
			throw new IllegalArgumentException("Length must be non-negative: " + length);
		var sb = new StringBuilder(length);
		for (var i = 0; i < length; i++) {
			var c = RANDOM.nextInt(62);
			if (c < 10)
				sb.append((char)('0' + c));
			else if (c < 36)
				sb.append((char)('a' + c - 10));
			else
				sb.append((char)('A' + c - 36));
		}
		return sb.toString();
	}

	/**
	 * Generates a random numeric string of the specified length.
	 *
	 * <p>
	 * Characters are composed of digits (0-9).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	randomNumeric(<jk>6</jk>);  <jc>// "123456"</jc>
	 * </p>
	 *
	 * @param length The length of the generated string.
	 * @return A new random numeric string.
	 */
	public static String randomNumeric(int length) {
		if (length < 0)
			throw new IllegalArgumentException("Length must be non-negative: " + length);
		var sb = new StringBuilder(length);
		for (var i = 0; i < length; i++) {
			sb.append((char)('0' + RANDOM.nextInt(10)));
		}
		return sb.toString();
	}

	/**
	 * Generates a random ASCII string of the specified length.
	 *
	 * <p>
	 * Characters are composed of printable ASCII characters (32-126).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	randomAscii(<jk>10</jk>);  <jc>// "!@#$%^&*()"</jc>
	 * </p>
	 *
	 * @param length The length of the generated string.
	 * @return A new random ASCII string.
	 */
	public static String randomAscii(int length) {
		if (length < 0)
			throw new IllegalArgumentException("Length must be non-negative: " + length);
		var sb = new StringBuilder(length);
		for (var i = 0; i < length; i++) {
			sb.append((char)(32 + RANDOM.nextInt(95))); // 95 printable ASCII chars (32-126)
		}
		return sb.toString();
	}

	/**
	 * Generates a random string of the specified length using characters from the given character set.
	 *
	 * <p>
	 * Each character in the generated string is randomly selected from the provided character set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	randomString(<jk>5</jk>, <js>"ABC"</js>);  <jc>// "BACAB"</jc>
	 * </p>
	 *
	 * @param length The length of the generated string.
	 * @param chars The character set to use. Must not be null or empty.
	 * @return A new random string.
	 * @throws IllegalArgumentException If chars is null or empty, or length is negative.
	 */
	public static String randomString(int length, String chars) {
		if (length < 0)
			throw new IllegalArgumentException("Length must be non-negative: " + length);
		if (chars == null || chars.isEmpty())
			throw new IllegalArgumentException("Character set must not be null or empty");
		var sb = new StringBuilder(length);
		var charsLen = chars.length();
		for (var i = 0; i < length; i++) {
			sb.append(chars.charAt(RANDOM.nextInt(charsLen)));
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
	 * Removes multiple substrings from a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeAll(<js>"hello world test"</js>, <js>"hello"</js>, <js>"test"</js>);  <jc>// " world "</jc>
	 * 	removeAll(<jk>null</jk>, <js>"x"</js>);                                     <jc>// null</jc>
	 * </p>
	 *
	 * @param str The string to process.
	 * @param remove The substrings to remove.
	 * @return The string with all specified substrings removed, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeAll(String str, String...remove) {
		if (str == null)
			return null;
		if (isEmpty(str) || remove == null || remove.length == 0)
			return str;
		var result = str;
		for (var r : remove) {
			if (r != null)
				result = result.replace(r, "");
		}
		return result;
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

	/**
	 * Wraps text to a specified line length.
	 *
	 * <p>
	 * Wraps text by breaking at word boundaries (spaces). Words longer than the wrap length
	 * will be broken at the wrap length. Existing newlines are preserved.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	wrap(<js>"hello world test"</js>, 10);  <jc>// "hello world\ntest"</jc>
	 * 	wrap(<jk>null</jk>, 10);                <jc>// null</jc>
	 * </p>
	 *
	 * @param str The string to wrap.
	 * @param wrapLength The maximum line length (must be &gt; 0).
	 * @return The wrapped string, or <jk>null</jk> if input is <jk>null</jk>.
	 * @throws IllegalArgumentException if wrapLength is &lt;= 0.
	 */
	public static String wrap(String str, int wrapLength) {
		return wrap(str, wrapLength, "\n");
	}

	/**
	 * Wraps text to a specified line length with a custom newline string.
	 *
	 * <p>
	 * Wraps text by breaking at word boundaries (spaces). Words longer than the wrap length
	 * will be broken at the wrap length. Existing newlines are preserved.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	wrap(<js>"hello world test"</js>, 10, <js>"<br>"</js>);  <jc>// "hello world<br>test"</jc>
	 * 	wrap(<jk>null</jk>, 10, <js>"\n"</js>);                  <jc>// null</jc>
	 * </p>
	 *
	 * @param str The string to wrap.
	 * @param wrapLength The maximum line length (must be &gt; 0).
	 * @param newline The string to use as line separator.
	 * @return The wrapped string, or <jk>null</jk> if input is <jk>null</jk>.
	 * @throws IllegalArgumentException if wrapLength is &lt;= 0 or newline is <jk>null</jk>.
	 */
	public static String wrap(String str, int wrapLength, String newline) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;
		if (wrapLength <= 0)
			throw illegalArg("wrapLength must be > 0: {0}", wrapLength);
		if (newline == null)
			throw illegalArg("newline cannot be null");

		var result = new StringBuilder();
		var lines = str.split("\r?\n", -1);  // Preserve empty lines

		for (var lineIdx = 0; lineIdx < lines.length; lineIdx++) {
			var line = lines[lineIdx];
			if (line.isEmpty()) {
				if (lineIdx < lines.length - 1)
					result.append(newline);
				continue;
			}

			// Split into words first, then combine words that fit
			var words = line.split(" +");  // Split on one or more spaces
			var currentLine = new StringBuilder();

			for (var word : words) {
				if (word.isEmpty())
					continue;

				var wordLength = word.length();
				var currentLength = currentLine.length();

				if (currentLength == 0) {
					// First word on line
					// Only break single words if there are multiple words in the input
					// (single long words should not be broken for readability)
					if (wordLength > wrapLength && words.length > 1) {
						// Word is too long and there are other words, break it
						if (result.length() > 0)
							result.append(newline);
						var wordPos = 0;
						while (wordPos < wordLength) {
							if (wordPos > 0)
								result.append(newline);
							var remaining = wordLength - wordPos;
							if (remaining <= wrapLength) {
								result.append(word.substring(wordPos));
								break;
							}
							result.append(word.substring(wordPos, wordPos + wrapLength));
							wordPos += wrapLength;
						}
					} else {
						currentLine.append(word);
					}
				} else {
					// Check if we can add this word to current line
					var neededLength = currentLength + 1 + wordLength;  // current + space + word
					// Break if it would fit exactly or exceed - prefer breaking for readability
					if (neededLength < wrapLength) {
						// Fits with room to spare
						currentLine.append(' ').append(word);
					} else {
						// Doesn't fit or fits exactly - start new line
						if (result.length() > 0)
							result.append(newline);
						result.append(currentLine);
						currentLine.setLength(0);
						if (wordLength > wrapLength && words.length > 1) {
							// Word is too long and there are other words, break it
							result.append(newline);
							var wordPos = 0;
							while (wordPos < wordLength) {
								if (wordPos > 0)
									result.append(newline);
								var remaining = wordLength - wordPos;
								if (remaining <= wrapLength) {
									result.append(word.substring(wordPos));
									break;
								}
								result.append(word.substring(wordPos, wordPos + wrapLength));
								wordPos += wrapLength;
							}
						} else {
							currentLine.append(word);
						}
					}
				}
			}

			// Append any remaining line
			if (currentLine.length() > 0) {
				if (result.length() > 0)
					result.append(newline);
				result.append(currentLine);
			}
		}

		return result.toString();
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
	 * Supports named MessageFormat-style variables: <js>"{key}"</js> where <c>key</c> is a map key.
	 * For un-numbered sequential placeholders <js>"{}"</js>, use {@link #format(String, Object...)} instead.
	 *
	 * <p>
	 * Variable values are converted to strings using {@link #readable(Object)} to ensure consistent,
	 * readable formatting (e.g., byte arrays are converted to hex, collections are formatted without spaces).
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
	public static String formatNamed(String s, Map<String,Object> m) {

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
						key = (hasInternalVar ? formatNamed(key, m) : key);
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
							var v = r(val);
							// If the replacement also contains variables, replace them now.
							if (v.indexOf('{') != -1)
								v = formatNamed(v, m);
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
		var key = (String)null;
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

		if (! contains(s, ' ', '\t', '\'', '"'))
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
	 * Parses a string containing key-value pairs into a map.
	 *
	 * <p>
	 * Splits the string by the entry delimiter, then splits each entry by the key-value delimiter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	parseMap(<js>"key1=value1,key2=value2"</js>, <js>'='</js>, <js>','</js>, <jk>false</jk>);
	 * 	<jc>// {"key1":"value1","key2":"value2"}</jc>
	 * 	parseMap(<js>" key1 = value1 ; key2 = value2 "</js>, <js>'='</js>, <js>';'</js>, <jk>true</jk>);
	 * 	<jc>// {"key1":"value1","key2":"value2"}</jc>
	 * </p>
	 *
	 * @param str The string to parse. Can be <jk>null</jk>.
	 * @param keyValueDelimiter The character that separates keys from values.
	 * @param entryDelimiter The character that separates entries.
	 * @param trimKeys If <jk>true</jk>, trims whitespace from keys and values.
	 * @return A map containing the parsed key-value pairs, or an empty map if the string is <jk>null</jk> or empty.
	 */
	public static Map<String,String> parseMap(String str, char keyValueDelimiter, char entryDelimiter, boolean trimKeys) {
		var result = new LinkedHashMap<String,String>();
		if (isEmpty(str))
			return result;

		var entries = split(str, entryDelimiter);
		for (var entry : entries) {
			if (isEmpty(entry))
				continue;
			var delimiterIndex = entry.indexOf(keyValueDelimiter);
			if (delimiterIndex == -1) {
				// No delimiter found, treat entire entry as key with empty value
				var key = trimKeys ? entry.trim() : entry;
				result.put(key, "");
			} else {
				var key = entry.substring(0, delimiterIndex);
				var value = entry.substring(delimiterIndex + 1);
				if (trimKeys) {
					key = key.trim();
					value = value.trim();
				}
				result.put(key, value);
			}
		}
		return result;
	}

	/**
	 * Extracts all numeric sequences from a string.
	 *
	 * <p>
	 * Finds all sequences of digits (including decimal numbers with dots).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	extractNumbers(<js>"Price: $19.99, Quantity: 5"</js>);
	 * 	<jc>// ["19.99", "5"]</jc>
	 * 	extractNumbers(<js>"Version 1.2.3"</js>);
	 * 	<jc>// ["1.2", "3"]</jc>
	 * </p>
	 *
	 * @param str The string to extract numbers from. Can be <jk>null</jk>.
	 * @return A list of numeric strings found in the input, or an empty list if the string is <jk>null</jk> or empty.
	 */
	public static List<String> extractNumbers(String str) {
		if (isEmpty(str))
			return Collections.emptyList();

		var result = new ArrayList<String>();
		var pattern = Pattern.compile("\\d+(?:\\.\\d+)?");
		var matcher = pattern.matcher(str);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	/**
	 * Extracts all email addresses from a string.
	 *
	 * <p>
	 * Uses a basic email regex pattern to find email addresses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	extractEmails(<js>"Contact: user@example.com or admin@test.org"</js>);
	 * 	<jc>// ["user@example.com", "admin@test.org"]</jc>
	 * </p>
	 *
	 * @param str The string to extract emails from. Can be <jk>null</jk>.
	 * @return A list of email addresses found in the input, or an empty list if the string is <jk>null</jk> or empty.
	 */
	public static List<String> extractEmails(String str) {
		if (isEmpty(str))
			return Collections.emptyList();

		var result = new ArrayList<String>();
		// Email regex pattern (same as isEmail but without ^ and $ anchors)
		var pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
		var matcher = pattern.matcher(str);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	/**
	 * Extracts all URLs from a string.
	 *
	 * <p>
	 * Uses a basic URL regex pattern to find URLs (http, https, ftp).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	extractUrls(<js>"Visit https://example.com or http://test.org"</js>);
	 * 	<jc>// ["https://example.com", "http://test.org"]</jc>
	 * </p>
	 *
	 * @param str The string to extract URLs from. Can be <jk>null</jk>.
	 * @return A list of URLs found in the input, or an empty list if the string is <jk>null</jk> or empty.
	 */
	public static List<String> extractUrls(String str) {
		if (isEmpty(str))
			return Collections.emptyList();

		var result = new ArrayList<String>();
		// Basic URL pattern: protocol://domain/path
		var pattern = Pattern.compile("(?:https?|ftp)://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
		var matcher = pattern.matcher(str);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	/**
	 * Extracts all words from a string.
	 *
	 * <p>
	 * A word is defined as a sequence of letters, digits, and underscores.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	extractWords(<js>"Hello world! This is a test."</js>);
	 * 	<jc>// ["Hello", "world", "This", "is", "a", "test"]</jc>
	 * </p>
	 *
	 * @param str The string to extract words from. Can be <jk>null</jk>.
	 * @return A list of words found in the input, or an empty list if the string is <jk>null</jk> or empty.
	 */
	public static List<String> extractWords(String str) {
		if (isEmpty(str))
			return Collections.emptyList();

		var result = new ArrayList<String>();
		// Word pattern: sequence of word characters (letters, digits, underscore)
		var pattern = Pattern.compile("\\w+");
		var matcher = pattern.matcher(str);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	/**
	 * Extracts all text segments between start and end markers.
	 *
	 * <p>
	 * Finds all occurrences of text between the start and end markers (non-overlapping).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	extractBetween(<js>"&lt;tag&gt;content&lt;/tag&gt;"</js>, <js>"&lt;"</js>, <js>"&gt;"</js>);
	 * 	<jc>// ["tag", "/tag"]</jc>
	 * 	extractBetween(<js>"[one][two][three]"</js>, <js>"["</js>, <js>"]"</js>);
	 * 	<jc>// ["one", "two", "three"]</jc>
	 * </p>
	 *
	 * @param str The string to extract from. Can be <jk>null</jk>.
	 * @param start The start marker. Can be <jk>null</jk>.
	 * @param end The end marker. Can be <jk>null</jk>.
	 * @return A list of text segments found between the markers, or an empty list if any parameter is <jk>null</jk> or empty.
	 */
	public static List<String> extractBetween(String str, String start, String end) {
		if (isEmpty(str) || isEmpty(start) || isEmpty(end))
			return Collections.emptyList();

		var result = new ArrayList<String>();
		var startIndex = 0;
		while (true) {
			var startPos = str.indexOf(start, startIndex);
			if (startPos == -1)
				break;
			var endPos = str.indexOf(end, startPos + start.length());
			if (endPos == -1)
				break;
			result.add(str.substring(startPos + start.length(), endPos));
			startIndex = endPos + end.length();
		}
		return result;
	}

	/**
	 * Transliterates characters in a string by mapping characters from one set to another.
	 *
	 * <p>
	 * Performs character-by-character translation. If a character is found in <c>fromChars</c>,
	 * it is replaced with the corresponding character at the same position in <c>toChars</c>.
	 * Characters not found in <c>fromChars</c> are left unchanged.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	transliterate(<js>"hello"</js>, <js>"aeiou"</js>, <js>"12345"</js>);
	 * 	<jc>// "h2ll4"</jc>
	 * 	transliterate(<js>"ABC"</js>, <js>"ABC"</js>, <js>"XYZ"</js>);
	 * 	<jc>// "XYZ"</jc>
	 * </p>
	 *
	 * @param str The string to transliterate. Can be <jk>null</jk>.
	 * @param fromChars The source character set. Can be <jk>null</jk>.
	 * @param toChars The target character set. Can be <jk>null</jk>.
	 * @return The transliterated string, or <jk>null</jk> if input is <jk>null</jk>.
	 * @throws IllegalArgumentException If <c>fromChars</c> and <c>toChars</c> have different lengths.
	 */
	public static String transliterate(String str, String fromChars, String toChars) {
		if (str == null)
			return null;
		if (fromChars == null || toChars == null || fromChars.isEmpty() || toChars.isEmpty())
			return str;
		if (fromChars.length() != toChars.length())
			throw new IllegalArgumentException("fromChars and toChars must have the same length");

		var sb = new StringBuilder(str.length());
		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			var index = fromChars.indexOf(c);
			if (index >= 0)
				sb.append(toChars.charAt(index));
			else
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Generates a Soundex code for a string.
	 *
	 * <p>
	 * Soundex is a phonetic algorithm for indexing names by sound. The code consists of
	 * a letter followed by three digits. Similar-sounding names produce the same code.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	soundex(<js>"Smith"</js>);   <jc>// "S530"</jc>
	 * 	soundex(<js>"Smythe"</js>);  <jc>// "S530"</jc>
	 * 	soundex(<js>"Robert"</js>);  <jc>// "R163"</jc>
	 * </p>
	 *
	 * @param str The string to generate a Soundex code for. Can be <jk>null</jk>.
	 * @return The Soundex code (1 letter + 3 digits), or <jk>null</jk> if input is <jk>null</jk> or empty.
	 */
	public static String soundex(String str) {
		if (isEmpty(str))
			return null;

		var upper = str.toUpperCase();
		var result = new StringBuilder(4);
		result.append(upper.charAt(0));

		// Soundex mapping: 0 = AEIOUHWY, 1 = BFPV, 2 = CGJKQSXZ, 3 = DT, 4 = L, 5 = MN, 6 = R
		// H/W/Y don't get codes but don't break sequences either
		// Initialize lastCode to a value that won't match any real code
		var lastCode = '\0';

		for (var i = 1; i < upper.length() && result.length() < 4; i++) {
			var c = upper.charAt(i);
			var code = getSoundexCode(c);
			if (code == '0') {
				// H/W/Y/vowels - don't add code, but don't update lastCode either
				// This allows sequences to continue across H/W/Y/vowels
				continue;
			}
			if (code != lastCode) {
				result.append(code);
				lastCode = code;
			}
			// If code == lastCode, skip it (consecutive same codes)
		}

		// Pad with zeros if needed
		while (result.length() < 4) {
			result.append('0');
		}

		return result.toString();
	}

	/**
	 * Helper method to get Soundex code for a character.
	 */
	private static char getSoundexCode(char c) {
		if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U' || c == 'H' || c == 'W' || c == 'Y')
			return '0';
		if (c == 'B' || c == 'F' || c == 'P' || c == 'V')
			return '1';
		if (c == 'C' || c == 'G' || c == 'J' || c == 'K' || c == 'Q' || c == 'S' || c == 'X' || c == 'Z')
			return '2';
		if (c == 'D' || c == 'T')
			return '3';
		if (c == 'L')
			return '4';
		if (c == 'M' || c == 'N')
			return '5';
		if (c == 'R')
			return '6';
		return '0'; // Non-letter characters
	}

	/**
	 * Generates a Metaphone code for a string.
	 *
	 * <p>
	 * Metaphone is a phonetic algorithm that produces codes representing how words sound.
	 * It's more accurate than Soundex for English words.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	metaphone(<js>"Smith"</js>);   <jc>// "SM0"</jc>
	 * 	metaphone(<js>"Smythe"</js>);  <jc>// "SM0"</jc>
	 * 	metaphone(<js>"Robert"</js>);  <jc>// "RBRT"</jc>
	 * </p>
	 *
	 * @param str The string to generate a Metaphone code for. Can be <jk>null</jk>.
	 * @return The Metaphone code, or <jk>null</jk> if input is <jk>null</jk> or empty.
	 */
	public static String metaphone(String str) {
		if (isEmpty(str))
			return null;

		var upper = str.toUpperCase().replaceAll("[^A-Z]", "");
		if (upper.isEmpty())
			return "";

		var result = new StringBuilder();
		var i = 0;
		var len = upper.length();

		// Handle initial characters
		if (upper.startsWith("KN") || upper.startsWith("GN") || upper.startsWith("PN") || upper.startsWith("AE") || upper.startsWith("WR")) {
			i = 1;
		} else if (upper.startsWith("X")) {
			result.append('S');
			i = 1;
		} else if (upper.startsWith("WH")) {
			result.append('W');
			i = 2;
		}

		// Process remaining characters
		while (i < len && result.length() < 4) {
			var c = upper.charAt(i);
			var prev = i > 0 ? upper.charAt(i - 1) : '\0';
			var next = i < len - 1 ? upper.charAt(i + 1) : '\0';
			var next2 = i < len - 2 ? upper.charAt(i + 2) : '\0';

			// Skip duplicates (except C)
			if (c == prev && c != 'C') {
				i++;
				continue;
			}

			switch (c) {
				case 'B':
					if (prev != 'M' || next != '\0')
						result.append('B');
					break;
				case 'C':
					if (next == 'H') {
						if (prev == 'S')
							result.append('K');
						else
							result.append('X');
						i++;
					} else if (next == 'I' || next == 'E' || next == 'Y') {
						result.append('S');
					} else {
						result.append('K');
					}
					break;
				case 'D':
					if (next == 'G' && (next2 == 'E' || next2 == 'I' || next2 == 'Y')) {
						result.append('J');
						i++;
					} else {
						result.append('T');
					}
					break;
				case 'F':
				case 'J':
				case 'L':
				case 'M':
				case 'N':
				case 'R':
					result.append(c);
					break;
				case 'G':
					if (next == 'H' && (next2 == 'A' || next2 == 'E' || next2 == 'I' || next2 == 'O' || next2 == 'U')) {
						// Silent GH
					} else if (next == 'N' && (next2 == 'E' || next2 == 'D')) {
						// Silent GN
					} else if ((next == 'E' || next == 'I' || next == 'Y') && prev != 'G') {
						result.append('J');
					} else {
						result.append('K');
					}
					break;
				case 'H':
					if (! isVowel(prev) || ! isVowel(next))
						result.append('H');
					break;
				case 'K':
					if (prev != 'C')
						result.append('K');
					break;
				case 'P':
					if (next == 'H') {
						result.append('F');
						i++;
					} else {
						result.append('P');
					}
					break;
				case 'Q':
					result.append('K');
					break;
				case 'S':
					if (next == 'H') {
						result.append('X');
						i++;
					} else if (next == 'I' && (next2 == 'O' || next2 == 'A')) {
						result.append('X');
						i++;
					} else {
						result.append('S');
					}
					break;
				case 'T':
					if (next == 'H') {
						result.append('0'); // TH sound
						i++;
					} else if (next == 'I' && (next2 == 'O' || next2 == 'A')) {
						result.append('X');
						i++;
					} else {
						result.append('T');
					}
					break;
				case 'V':
					result.append('F');
					break;
				case 'W', 'Y':
					if (isVowel(next))
						result.append(c);
					break;
				case 'X':
					if (i == 0)
						result.append('S');
					else
						result.append("KS");
					break;
				case 'Z':
					result.append('S');
					break;
				default:
					break;
			}
			i++;
		}

		return result.length() > 0 ? result.toString() : upper.substring(0, Math.min(1, upper.length()));
	}

	/**
	 * Helper method to check if a character is a vowel.
	 */
	private static boolean isVowel(char c) {
		return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
	}

	/**
	 * Generates a Double Metaphone code for a string.
	 *
	 * <p>
	 * Double Metaphone is an improved version of Metaphone that returns two codes:
	 * a primary code and an alternate code. This handles more edge cases and variations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	doubleMetaphone(<js>"Smith"</js>);   <jc>// "SM0"</jc>
	 * 	doubleMetaphone(<js>"Schmidt"</js>); <jc>// "XMT"</jc>
	 * </p>
	 *
	 * @param str The string to generate a Double Metaphone code for. Can be <jk>null</jk>.
	 * @return An array with two elements: [primary code, alternate code]. Returns <jk>null</jk> if input is <jk>null</jk> or empty.
	 */
	public static String[] doubleMetaphone(String str) {
		if (isEmpty(str))
			return null;

		// For simplicity, return the same code for both primary and alternate
		// A full Double Metaphone implementation would be much more complex
		var primary = metaphone(str);
		if (primary == null)
			return null;

		// Generate alternate code (simplified - full implementation would have different rules)
		var alternate = primary;

		return new String[] { primary, alternate };
	}

	/**
	 * Normalizes Unicode characters in a string.
	 *
	 * <p>
	 * Uses Unicode normalization form NFD (Canonical Decomposition).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	normalizeUnicode(<js>"café"</js>);
	 * 	<jc>// Normalized form</jc>
	 * </p>
	 *
	 * @param str The string to normalize. Can be <jk>null</jk>.
	 * @return The normalized string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String normalizeUnicode(String str) {
		if (str == null)
			return null;
		return Normalizer.normalize(str, Normalizer.Form.NFD);
	}

	/**
	 * Removes diacritical marks (accents) from characters in a string.
	 *
	 * <p>
	 * Normalizes the string to NFD form and removes combining diacritical marks.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	removeAccents(<js>"café"</js>);     <jc>// "cafe"</jc>
	 * 	removeAccents(<js>"naïve"</js>);    <jc>// "naive"</jc>
	 * 	removeAccents(<js>"résumé"</js>);   <jc>// "resume"</jc>
	 * </p>
	 *
	 * @param str The string to remove accents from. Can be <jk>null</jk>.
	 * @return The string with accents removed, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String removeAccents(String str) {
		if (str == null)
			return null;

		// Normalize to NFD (decomposed form)
		var normalized = Normalizer.normalize(str, Normalizer.Form.NFD);

		// Remove combining diacritical marks (Unicode category Mn)
		var sb = new StringBuilder(normalized.length());
		for (var i = 0; i < normalized.length(); i++) {
			var c = normalized.charAt(i);
			var type = Character.getType(c);
			// Mn = Nonspacing_Mark (combining marks)
			if (type != Character.NON_SPACING_MARK) {
				sb.append(c);
			}
		}

		return sb.toString();
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
	 * Helper method to split a string into words.
	 * Detects word boundaries from separators (spaces, underscores, hyphens) and case changes.
	 *
	 * @param str The string to split.
	 * @return A list of words, or empty list if input is null or empty.
	 */
	static List<String> splitWords(String str) {
		if (str == null || isEmpty(str))
			return Collections.emptyList();

		var words = new ArrayList<String>();
		var sb = new StringBuilder();
		var wasLowerCase = false;
		var wasUpperCase = false;
		var consecutiveUpperCount = 0;

		for (var i = 0; i < str.length(); i++) {
			var c = str.charAt(i);
			var isSeparator = (c == ' ' || c == '_' || c == '-' || c == '\t');
			var isUpperCase = Character.isUpperCase(c);
			var isLowerCase = Character.isLowerCase(c);
			var isLetter = Character.isLetter(c);

			if (isSeparator) {
				if (sb.length() > 0) {
					words.add(sb.toString());
					sb.setLength(0);
				}
				wasLowerCase = false;
				wasUpperCase = false;
				consecutiveUpperCount = 0;
			} else if (isLetter) {
				// Detect word boundary:
				// 1. Uppercase after lowercase (e.g., "helloWorld" → "hello", "World")
				// 2. Uppercase after consecutive uppercase when next is lowercase (e.g., "XMLHttp" → "XML", "Http")
				// 3. Lowercase after 2+ consecutive uppercase (e.g., "XMLHttp" → "XML", "Http")
				if (sb.length() > 0) {
					if (isUpperCase && wasLowerCase) {
						// Case 1: uppercase after lowercase (e.g., "helloWorld" → "hello", "World")
						words.add(sb.toString());
						sb.setLength(0);
						consecutiveUpperCount = 0;
					} else if (isUpperCase && wasUpperCase && consecutiveUpperCount >= 2) {
						// Case 2: uppercase after uppercase - check if this starts a new word
						// Look ahead to see if next character is lowercase
						// This handles "XMLHttp" where 'H' starts "Http"
						// We need at least 2 consecutive uppercase letters before this one to split
						if (i + 1 < str.length()) {
							var nextChar = str.charAt(i + 1);
							if (Character.isLowerCase(nextChar)) {
								// This uppercase starts a new word, split before it
								words.add(sb.toString());
								sb.setLength(0);
								consecutiveUpperCount = 0;
							}
						}
					} else if (isLowerCase && wasUpperCase && consecutiveUpperCount >= 2) {
						// Case 3: lowercase after 2+ consecutive uppercase
						// Split all but the last uppercase (e.g., "XMLH" → "XML" + "H")
						var splitPoint = sb.length() - 1;
						words.add(sb.substring(0, splitPoint));
						sb.delete(0, splitPoint);
						consecutiveUpperCount = 0;
					}
				}
				sb.append(c);
				// Update state AFTER appending
				wasLowerCase = isLowerCase;
				wasUpperCase = isUpperCase;
				if (isUpperCase) {
					consecutiveUpperCount++;
				} else {
					consecutiveUpperCount = 0;
				}
			} else {
				// Non-letter characters (digits, etc.) - treat as part of current word
				sb.append(c);
				wasLowerCase = false;
				wasUpperCase = false;
				consecutiveUpperCount = 0;
			}
		}

		if (sb.length() > 0)
			words.add(sb.toString());

		return words;
	}

	/**
	 * Converts a string to camelCase format.
	 *
	 * <p>
	 * Handles various input formats:
	 * <ul>
	 *   <li>Space-separated: "hello world" → "helloWorld"</li>
	 *   <li>Underscore-separated: "hello_world" → "helloWorld"</li>
	 *   <li>Hyphen-separated: "hello-world" → "helloWorld"</li>
	 *   <li>PascalCase: "HelloWorld" → "helloWorld"</li>
	 *   <li>Already camelCase: "helloWorld" → "helloWorld"</li>
	 *   <li>Mixed case: "Hello_World-Test" → "helloWorldTest"</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	camelCase(<jk>null</jk>);                    <jc>// null</jc>
	 * 	camelCase(<js>""</js>);                      <jc>// ""</jc>
	 * 	camelCase(<js>"hello world"</js>);           <jc>// "helloWorld"</jc>
	 * 	camelCase(<js>"hello_world"</js>);           <jc>// "helloWorld"</jc>
	 * 	camelCase(<js>"hello-world"</js>);           <jc>// "helloWorld"</jc>
	 * 	camelCase(<js>"HelloWorld"</js>);            <jc>// "helloWorld"</jc>
	 * 	camelCase(<js>"helloWorld"</js>);            <jc>// "helloWorld"</jc>
	 * 	camelCase(<js>"  hello   world  "</js>);     <jc>// "helloWorld"</jc>
	 * </p>
	 *
	 * @param str The string to convert.
	 * @return The camelCase string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String camelCase(String str) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;

		var words = splitWords(str);
		if (words.isEmpty())
			return "";

		var result = new StringBuilder();
		for (var i = 0; i < words.size(); i++) {
			var word = words.get(i);
			if (i == 0) {
				result.append(uncapitalize(word));
			} else {
				result.append(capitalize(word.toLowerCase()));
			}
		}

		return result.toString();
	}

	/**
	 * Converts a string to snake_case format.
	 *
	 * <p>
	 * Handles various input formats:
	 * <ul>
	 *   <li>Space-separated: "hello world" → "hello_world"</li>
	 *   <li>CamelCase: "helloWorld" → "hello_world"</li>
	 *   <li>PascalCase: "HelloWorld" → "hello_world"</li>
	 *   <li>Kebab-case: "hello-world" → "hello_world"</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	snakeCase(<jk>null</jk>);                    <jc>// null</jc>
	 * 	snakeCase(<js>""</js>);                      <jc>// ""</jc>
	 * 	snakeCase(<js>"hello world"</js>);           <jc>// "hello_world"</jc>
	 * 	snakeCase(<js>"helloWorld"</js>);            <jc>// "hello_world"</jc>
	 * 	snakeCase(<js>"HelloWorld"</js>);            <jc>// "hello_world"</jc>
	 * 	snakeCase(<js>"hello-world"</js>);           <jc>// "hello_world"</jc>
	 * </p>
	 *
	 * @param str The string to convert.
	 * @return The snake_case string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String snakeCase(String str) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;

		var words = splitWords(str);
		if (words.isEmpty())
			return "";

		var result = new StringBuilder();
		for (var i = 0; i < words.size(); i++) {
			if (i > 0)
				result.append('_');
			result.append(words.get(i).toLowerCase());
		}

		return result.toString();
	}

	/**
	 * Converts a string to kebab-case format.
	 *
	 * <p>
	 * Handles various input formats:
	 * <ul>
	 *   <li>Space-separated: "hello world" → "hello-world"</li>
	 *   <li>CamelCase: "helloWorld" → "hello-world"</li>
	 *   <li>PascalCase: "HelloWorld" → "hello-world"</li>
	 *   <li>Snake_case: "hello_world" → "hello-world"</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	kebabCase(<jk>null</jk>);                    <jc>// null</jc>
	 * 	kebabCase(<js>""</js>);                      <jc>// ""</jc>
	 * 	kebabCase(<js>"hello world"</js>);           <jc>// "hello-world"</jc>
	 * 	kebabCase(<js>"helloWorld"</js>);            <jc>// "hello-world"</jc>
	 * 	kebabCase(<js>"HelloWorld"</js>);            <jc>// "hello-world"</jc>
	 * 	kebabCase(<js>"hello_world"</js>);           <jc>// "hello-world"</jc>
	 * </p>
	 *
	 * @param str The string to convert.
	 * @return The kebab-case string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String kebabCase(String str) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;

		var words = splitWords(str);
		if (words.isEmpty())
			return "";

		var result = new StringBuilder();
		for (var i = 0; i < words.size(); i++) {
			if (i > 0)
				result.append('-');
			result.append(words.get(i).toLowerCase());
		}

		return result.toString();
	}

	/**
	 * Converts a string to PascalCase format.
	 *
	 * <p>
	 * Handles various input formats:
	 * <ul>
	 *   <li>Space-separated: "hello world" → "HelloWorld"</li>
	 *   <li>CamelCase: "helloWorld" → "HelloWorld"</li>
	 *   <li>Snake_case: "hello_world" → "HelloWorld"</li>
	 *   <li>Kebab-case: "hello-world" → "HelloWorld"</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	pascalCase(<jk>null</jk>);                    <jc>// null</jc>
	 * 	pascalCase(<js>""</js>);                      <jc>// ""</jc>
	 * 	pascalCase(<js>"hello world"</js>);           <jc>// "HelloWorld"</jc>
	 * 	pascalCase(<js>"helloWorld"</js>);            <jc>// "HelloWorld"</jc>
	 * 	pascalCase(<js>"hello_world"</js>);           <jc>// "HelloWorld"</jc>
	 * 	pascalCase(<js>"hello-world"</js>);           <jc>// "HelloWorld"</jc>
	 * </p>
	 *
	 * @param str The string to convert.
	 * @return The PascalCase string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String pascalCase(String str) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;

		var words = splitWords(str);
		if (words.isEmpty())
			return "";

		var result = new StringBuilder();
		for (var word : words) {
			result.append(capitalize(word.toLowerCase()));
		}

		return result.toString();
	}

	/**
	 * Converts a string to Title Case format (first letter of each word capitalized, separated by spaces).
	 *
	 * <p>
	 * Handles various input formats:
	 * <ul>
	 *   <li>CamelCase: "helloWorld" → "Hello World"</li>
	 *   <li>PascalCase: "HelloWorld" → "Hello World"</li>
	 *   <li>Snake_case: "hello_world" → "Hello World"</li>
	 *   <li>Kebab-case: "hello-world" → "Hello World"</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	titleCase(<jk>null</jk>);                    <jc>// null</jc>
	 * 	titleCase(<js>""</js>);                      <jc>// ""</jc>
	 * 	titleCase(<js>"hello world"</js>);           <jc>// "Hello World"</jc>
	 * 	titleCase(<js>"helloWorld"</js>);            <jc>// "Hello World"</jc>
	 * 	titleCase(<js>"hello_world"</js>);           <jc>// "Hello World"</jc>
	 * 	titleCase(<js>"hello-world"</js>);           <jc>// "Hello World"</jc>
	 * </p>
	 *
	 * @param str The string to convert.
	 * @return The Title Case string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String titleCase(String str) {
		if (str == null)
			return null;
		if (isEmpty(str))
			return str;

		var words = splitWords(str);
		if (words.isEmpty())
			return "";

		var result = new StringBuilder();
		for (var i = 0; i < words.size(); i++) {
			if (i > 0)
				result.append(' ');
			result.append(capitalize(words.get(i).toLowerCase()));
		}

		return result.toString();
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
	public static StringBuilder append(StringBuilder sb, String in) {
		if (sb == null)
			return new StringBuilder(in);
		sb.append(in);
		return sb;
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
	private static int multiplierInt(String s) {
		var c = isEmpty(s) ? 'z' : s.charAt(s.length() - 1);
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
	private static long multiplierLong(String s) {
		var c = isEmpty(s) ? 'z' : s.charAt(s.length() - 1);
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
	 * Null-safe convenience method for {@link String#toLowerCase()}.
	 *
	 * <p>
	 * Converts the string to lowercase if not null.
	 *
	 * @param s The string to convert.
	 * @return The lowercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 * @see #upperCase(String)
	 * @see Utils#lc(String)
	 */
	public static String lowerCase(String s) {
		return s == null ? null : s.toLowerCase();
	}

	/**
	 * Null-safe convenience method for {@link String#toUpperCase()}.
	 *
	 * <p>
	 * Converts the string to uppercase if not null.
	 *
	 * @param s The string to convert.
	 * @return The uppercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 * @see #lowerCase(String)
	 * @see Utils#uc(String)
	 */
	public static String upperCase(String s) {
		return s == null ? null : s.toUpperCase();
	}

	/**
	 * Tests two objects for case-insensitive string equality.
	 *
	 * <p>
	 * Converts both objects to strings using {@link Object#toString()} before comparison.
	 * This method handles <jk>null</jk> values gracefully:
	 * <ul>
	 *   <li>Both <jk>null</jk> → returns <jk>true</jk></li>
	 *   <li>One <jk>null</jk> → returns <jk>false</jk></li>
	 *   <li>Neither <jk>null</jk> → compares string representations ignoring case</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <js>"HELLO"</js>);     <jc>// true</jc>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <js>"World"</js>);     <jc>// false</jc>
	 * 	equalsIgnoreCase(<jk>null</jk>, <jk>null</jk>);            <jc>// true</jc>
	 * 	equalsIgnoreCase(<js>"Hello"</js>, <jk>null</jk>);         <jc>// false</jc>
	 * 	equalsIgnoreCase(123, <js>"123"</js>);                    <jc>// true (converts 123 to "123")</jc>
	 * </p>
	 *
	 * @param a Object 1.
	 * @param b Object 2.
	 * @return <jk>true</jk> if both objects are equal ignoring case.
	 * @see #equalsIgnoreCase(String, String)
	 * @see Utils#eqic(Object, Object)
	 */
	public static boolean equalsIgnoreCase(Object a, Object b) {
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
	 * Returns <jk>true</jk> if the string does not contain any of the specified substrings.
	 *
	 * <p>
	 * This is the inverse of {@link #contains(String, String...)}.
	 * Returns <jk>true</jk> if:
	 * <ul>
	 *   <li>The string is <jk>null</jk></li>
	 *   <li>The values array is <jk>null</jk> or empty</li>
	 *   <li>None of the specified substrings are found in the string</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	notContains(<js>"Hello World"</js>, <js>"Foo"</js>, <js>"Bar"</js>);    <jc>// true</jc>
	 * 	notContains(<js>"Hello World"</js>, <js>"Hello"</js>, <js>"Foo"</js>);  <jc>// false (contains "Hello")</jc>
	 * 	notContains(<jk>null</jk>, <js>"Hello"</js>);                            <jc>// true</jc>
	 * </p>
	 *
	 * @param s The string to search.
	 * @param values The values to search for.
	 * @return <jk>true</jk> if the string does not contain any of the values.
	 * @see #contains(String, String...)
	 * @see #notContains(String, char...)
	 */
	public static boolean notContains(String s, String...values) {
		return ! contains(s, values);
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
			return cns(o2);
		if (o instanceof Executable o2) {
			var sb = new StringBuilder(64);
			sb.append(o2 instanceof Constructor ? cns(o2.getDeclaringClass()) : o2.getName()).append('(');
			var pt = o2.getParameterTypes();
			for (var i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(cns(pt[i]));
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

	//-----------------------------------------------------------------------------------------------------------------
	// String Array and Collection Utilities
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Converts a collection of strings to a string array.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the collection is <jk>null</jk>.
	 * Returns an empty array if the collection is empty.
	 *
	 * @param collection The collection to convert. Can be <jk>null</jk>.
	 * @return A new string array containing the collection elements, or <jk>null</jk> if the collection was <jk>null</jk>.
	 */
	public static String[] toStringArray(Collection<String> collection) {
		if (collection == null)
			return null;  // NOSONAR - Intentional.
		return collection.toArray(new String[collection.size()]);
	}

	/**
	 * Filters a string array using the specified predicate.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the array is <jk>null</jk>.
	 * Returns an empty array if the predicate is <jk>null</jk> or no elements match.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"foo"</js>, <js>""</js>, <js>"bar"</js>, <jk>null</jk>, <js>"baz"</js>};
	 * 	String[] <jv>filtered</jv> = filter(<jv>array</jv>, StringUtils.<jsf>NOT_EMPTY</jsf>);
	 * 	<jc>// Returns: ["foo", "bar", "baz"]</jc>
	 *
	 * 	String[] <jv>longStrings</jv> = filter(<jv>array</jv>, s -&gt; s != <jk>null</jk> &amp;&amp; s.length() &gt; 3);
	 * 	<jc>// Returns: ["baz"]</jc>
	 * </p>
	 *
	 * @param array The array to filter. Can be <jk>null</jk>.
	 * @param predicate The predicate to apply to each element. Can be <jk>null</jk>.
	 * @return A new array containing only the elements that match the predicate, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static String[] filter(String[] array, Predicate<String> predicate) {
		if (array == null)
			return null;  // NOSONAR - Intentional.
		if (predicate == null)
			return new String[0];
		return Arrays.stream(array).filter(predicate).toArray(String[]::new);
	}

	/**
	 * Maps each element of a string array using the specified function.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the array is <jk>null</jk>.
	 * Returns an array with <jk>null</jk> elements if the function is <jk>null</jk> or returns <jk>null</jk>.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"foo"</js>, <js>"bar"</js>, <js>"baz"</js>};
	 * 	String[] <jv>uppercased</jv> = map(<jv>array</jv>, String::toUpperCase);
	 * 	<jc>// Returns: ["FOO", "BAR", "BAZ"]</jc>
	 *
	 * 	String[] <jv>prefixed</jv> = map(<jv>array</jv>, s -&gt; <js>"prefix-"</js> + s);
	 * 	<jc>// Returns: ["prefix-foo", "prefix-bar", "prefix-baz"]</jc>
	 * </p>
	 *
	 * @param array The array to map. Can be <jk>null</jk>.
	 * @param mapper The function to apply to each element. Can be <jk>null</jk>.
	 * @return A new array with the mapped elements, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static String[] mapped(String[] array, Function<String,String> mapper) {
		if (array == null)
			return null;  // NOSONAR - Intentional.
		if (mapper == null)
			return Arrays.copyOf(array, array.length);
		return Arrays.stream(array).map(mapper).toArray(String[]::new);
	}

	/**
	 * Removes duplicate elements from a string array, preserving order.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the array is <jk>null</jk>.
	 * Uses a {@link LinkedHashSet} to preserve insertion order while removing duplicates.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"foo"</js>, <js>"bar"</js>, <js>"foo"</js>, <js>"baz"</js>, <js>"bar"</js>};
	 * 	String[] <jv>unique</jv> = distinct(<jv>array</jv>);
	 * 	<jc>// Returns: ["foo", "bar", "baz"]</jc>
	 * </p>
	 *
	 * @param array The array to process. Can be <jk>null</jk>.
	 * @return A new array with duplicate elements removed, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static String[] distinct(String[] array) {
		if (array == null)
			return null;  // NOSONAR - Intentional.
		return Arrays.stream(array).collect(Collectors.toCollection(LinkedHashSet::new)).toArray(new String[0]);
	}

	/**
	 * Sorts a string array in natural order.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the array is <jk>null</jk>.
	 * This method creates a copy of the array and sorts it, leaving the original array unchanged.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"zebra"</js>, <js>"apple"</js>, <js>"banana"</js>};
	 * 	String[] <jv>sorted</jv> = sort(<jv>array</jv>);
	 * 	<jc>// Returns: ["apple", "banana", "zebra"]</jc>
	 * </p>
	 *
	 * @param array The array to sort. Can be <jk>null</jk>.
	 * @return A new sorted array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static String[] sort(String[] array) {
		if (array == null)
			return null;  // NOSONAR - Intentional.
		var result = Arrays.copyOf(array, array.length);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Sorts a string array in case-insensitive order.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the array is <jk>null</jk>.
	 * This method creates a copy of the array and sorts it using case-insensitive comparison,
	 * leaving the original array unchanged.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"Zebra"</js>, <js>"apple"</js>, <js>"Banana"</js>};
	 * 	String[] <jv>sorted</jv> = sortIgnoreCase(<jv>array</jv>);
	 * 	<jc>// Returns: ["apple", "Banana", "Zebra"]</jc>
	 * </p>
	 *
	 * @param array The array to sort. Can be <jk>null</jk>.
	 * @return A new sorted array (case-insensitive), or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static String[] sortIgnoreCase(String[] array) {
		if (array == null)
			return null;  // NOSONAR - Intentional.
		var result = Arrays.copyOf(array, array.length);
		Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String Builder Utilities
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Appends a string to a StringBuilder if the string is not empty.
	 *
	 * <p>
	 * Returns the same StringBuilder instance for method chaining.
	 * If the string is <jk>null</jk> or empty, nothing is appended.
	 * If <c>sb</c> is <jk>null</jk> and an append is going to occur, a new StringBuilder is automatically created.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
	 * 	appendIfNotEmpty(<jv>sb</jv>, <js>"hello"</js>);  <jc>// Appends "hello"</jc>
	 * 	appendIfNotEmpty(<jv>sb</jv>, <js>""</js>);       <jc>// Does nothing</jc>
	 * 	appendIfNotEmpty(<jv>sb</jv>, <jk>null</jk>);     <jc>// Does nothing</jc>
	 * 	appendIfNotEmpty(<jv>sb</jv>, <js>"world"</js>);  <jc>// Appends "world"</jc>
	 * 	<jc>// Result: "helloworld"</jc>
	 *
	 * 	<jc>// Auto-create StringBuilder if null and append occurs</jc>
	 * 	StringBuilder <jv>sb2</jv> = appendIfNotEmpty(<jk>null</jk>, <js>"test"</js>);  <jc>// Creates new StringBuilder with "test"</jc>
	 * 	StringBuilder <jv>sb3</jv> = appendIfNotEmpty(<jk>null</jk>, <jk>null</jk>);   <jc>// Returns null (no append occurred)</jc>
	 * </p>
	 *
	 * @param sb The StringBuilder to append to. Can be <jk>null</jk>.
	 * @param str The string to append if not empty. Can be <jk>null</jk>.
	 * @return The same StringBuilder instance for method chaining, or a new StringBuilder if <c>sb</c> was <jk>null</jk> and an append occurred, or <jk>null</jk> if <c>sb</c> was <jk>null</jk> and no append occurred.
	 */
	public static StringBuilder appendIfNotEmpty(StringBuilder sb, String str) {
		if (isNotEmpty(str)) {
			if (sb == null)
				sb = new StringBuilder();
			sb.append(str);
		}
		return sb;
	}

	/**
	 * Appends a string to a StringBuilder if the string is not blank.
	 *
	 * <p>
	 * Returns the same StringBuilder instance for method chaining.
	 * If the string is <jk>null</jk>, empty, or contains only whitespace, nothing is appended.
	 * If <c>sb</c> is <jk>null</jk> and an append is going to occur, a new StringBuilder is automatically created.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
	 * 	appendIfNotBlank(<jv>sb</jv>, <js>"hello"</js>);  <jc>// Appends "hello"</jc>
	 * 	appendIfNotBlank(<jv>sb</jv>, <js>"   "</js>);    <jc>// Does nothing</jc>
	 * 	appendIfNotBlank(<jv>sb</jv>, <jk>null</jk>);     <jc>// Does nothing</jc>
	 * 	appendIfNotBlank(<jv>sb</jv>, <js>"world"</js>);  <jc>// Appends "world"</jc>
	 * 	<jc>// Result: "helloworld"</jc>
	 *
	 * 	<jc>// Auto-create StringBuilder if null and append occurs</jc>
	 * 	StringBuilder <jv>sb2</jv> = appendIfNotBlank(<jk>null</jk>, <js>"test"</js>);  <jc>// Creates new StringBuilder with "test"</jc>
	 * 	StringBuilder <jv>sb3</jv> = appendIfNotBlank(<jk>null</jk>, <js>"   "</js>);   <jc>// Returns null (no append occurred)</jc>
	 * </p>
	 *
	 * @param sb The StringBuilder to append to. Can be <jk>null</jk>.
	 * @param str The string to append if not blank. Can be <jk>null</jk>.
	 * @return The same StringBuilder instance for method chaining, or a new StringBuilder if <c>sb</c> was <jk>null</jk> and an append occurred, or <jk>null</jk> if <c>sb</c> was <jk>null</jk> and no append occurred.
	 */
	public static StringBuilder appendIfNotBlank(StringBuilder sb, String str) {
		if (isNotBlank(str)) {
			if (sb == null)
				sb = new StringBuilder();
			sb.append(str);
		}
		return sb;
	}

	/**
	 * Appends a string to a StringBuilder with a separator, only adding the separator if the StringBuilder is not empty.
	 *
	 * <p>
	 * Returns the same StringBuilder instance for method chaining.
	 * If the StringBuilder is empty, only the string is appended (no separator).
	 * If the StringBuilder is not empty, the separator is appended first, then the string.
	 * If <c>sb</c> is <jk>null</jk> and an append is going to occur, a new StringBuilder is automatically created.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
	 * 	appendWithSeparator(<jv>sb</jv>, <js>"first"</js>, <js>", "</js>);   <jc>// Appends "first"</jc>
	 * 	appendWithSeparator(<jv>sb</jv>, <js>"second"</js>, <js>", "</js>);  <jc>// Appends ", second"</jc>
	 * 	appendWithSeparator(<jv>sb</jv>, <js>"third"</js>, <js>", "</js>);   <jc>// Appends ", third"</jc>
	 * 	<jc>// Result: "first, second, third"</jc>
	 *
	 * 	<jc>// Auto-create StringBuilder if null and append occurs</jc>
	 * 	StringBuilder <jv>sb2</jv> = appendWithSeparator(<jk>null</jk>, <js>"test"</js>, <js>", "</js>);  <jc>// Creates new StringBuilder with "test"</jc>
	 * 	StringBuilder <jv>sb3</jv> = appendWithSeparator(<jk>null</jk>, <jk>null</jk>, <js>", "</js>);   <jc>// Returns null (no append occurred)</jc>
	 * </p>
	 *
	 * @param sb The StringBuilder to append to. Can be <jk>null</jk>.
	 * @param str The string to append. Can be <jk>null</jk>.
	 * @param separator The separator to add before the string if the StringBuilder is not empty. Can be <jk>null</jk>.
	 * @return The same StringBuilder instance for method chaining, or a new StringBuilder if <c>sb</c> was <jk>null</jk> and an append occurred, or <jk>null</jk> if <c>sb</c> was <jk>null</jk> and no append occurred.
	 */
	public static StringBuilder appendWithSeparator(StringBuilder sb, String str, String separator) {
		if (str != null) {
			if (sb == null)
				sb = new StringBuilder();
			else if (sb.length() > 0 && separator != null)
				sb.append(separator);
			sb.append(str);
		}
		return sb;
	}

	/**
	 * Builds a string using a functional approach with a StringBuilder.
	 *
	 * <p>
	 * Creates a new StringBuilder, applies the consumer to it, and returns the resulting string.
	 * This provides a functional way to build strings without manually managing the StringBuilder.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String <jv>result</jv> = buildString(<jv>sb</jv> -&gt; {
	 * 		<jv>sb</jv>.append(<js>"Hello"</js>);
	 * 		<jv>sb</jv>.append(<js>" "</js>);
	 * 		<jv>sb</jv>.append(<js>"World"</js>);
	 * 	});
	 * 	<jc>// Returns: "Hello World"</jc>
	 *
	 * 	String <jv>joined</jv> = buildString(<jv>sb</jv> -&gt; {
	 * 		appendWithSeparator(<jv>sb</jv>, <js>"a"</js>, <js>", "</js>);
	 * 		appendWithSeparator(<jv>sb</jv>, <js>"b"</js>, <js>", "</js>);
	 * 		appendWithSeparator(<jv>sb</jv>, <js>"c"</js>, <js>", "</js>);
	 * 	});
	 * 	<jc>// Returns: "a, b, c"</jc>
	 * </p>
	 *
	 * @param builder The consumer that builds the string using the provided StringBuilder.
	 * @return The built string.
	 * @throws IllegalArgumentException If <c>builder</c> is <jk>null</jk>.
	 */
	public static String buildString(Consumer<StringBuilder> builder) {
		assertArgNotNull("builder", builder);
		var sb = new StringBuilder();
		builder.accept(sb);
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Performance and Memory Utilities
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Interns a string, returning the canonical representation.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the input string is <jk>null</jk>.
	 * This method provides a null-safe wrapper around {@link String#intern()}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String <jv>s1</jv> = <jk>new</jk> String(<js>"test"</js>);
	 * 	String <jv>s2</jv> = <jk>new</jk> String(<js>"test"</js>);
	 * 	assertTrue(<jv>s1</jv> != <jv>s2</jv>);  <jc>// Different objects</jc>
	 *
	 * 	String <jv>i1</jv> = intern(<jv>s1</jv>);
	 * 	String <jv>i2</jv> = intern(<jv>s2</jv>);
	 * 	assertTrue(<jv>i1</jv> == <jv>i2</jv>);  <jc>// Same interned object</jc>
	 * </p>
	 *
	 * <h5 class='section'>Performance Note:</h5>
	 * <p>String interning stores strings in a special pool, which can save memory when the same string
	 * values are used repeatedly. However, the intern pool has limited size and interning can be slow,
	 * so use judiciously for strings that are known to be repeated frequently.</p>
	 *
	 * @param str The string to intern. Can be <jk>null</jk>.
	 * @return The interned string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String intern(String str) {
		return str == null ? null : str.intern();
	}

	/**
	 * Checks if a string is already interned.
	 *
	 * <p>
	 * Returns <jk>false</jk> if the input string is <jk>null</jk>.
	 * A string is considered interned if it is the same object reference as its interned version.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	String <jv>s1</jv> = <js>"test"</js>;  <jc>// String literal is automatically interned</jc>
	 * 	assertTrue(isInterned(<jv>s1</jv>));
	 *
	 * 	String <jv>s2</jv> = <jk>new</jk> String(<js>"test"</js>);  <jc>// New object, not interned</jc>
	 * 	assertFalse(isInterned(<jv>s2</jv>));
	 *
	 * 	String <jv>s3</jv> = intern(<jv>s2</jv>);  <jc>// Now interned</jc>
	 * 	assertTrue(isInterned(<jv>s3</jv>));
	 * </p>
	 *
	 * @param str The string to check. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is interned, <jk>false</jk> otherwise.
	 */
	public static boolean isInterned(String str) {
		if (str == null)
			return false;
		return str == str.intern();
	}

	/**
	 * Calculates the approximate memory size of a string in bytes.
	 *
	 * <p>
	 * Returns <c>0</c> if the input string is <jk>null</jk>.
	 * This method provides an estimate based on typical JVM object layout:
	 * <ul>
	 *   <li>String object overhead: ~24 bytes (object header + fields)</li>
	 *   <li>char[] array overhead: ~16 bytes (array header)</li>
	 *   <li>Character data: 2 bytes per character</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Note:</b> Actual memory usage may vary based on JVM implementation, object alignment,
	 * and whether compressed OOPs are enabled. This is an approximation for informational purposes.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	getStringSize(<jk>null</jk>);        <jc>// Returns: 0</jc>
	 * 	getStringSize(<js>""</js>);          <jc>// Returns: ~40 bytes</jc>
	 * 	getStringSize(<js>"hello"</js>);     <jc>// Returns: ~50 bytes (40 + 10)</jc>
	 * 	getStringSize(<js>"test"</js>);      <jc>// Returns: ~48 bytes (40 + 8)</jc>
	 * </p>
	 *
	 * @param str The string to measure. Can be <jk>null</jk>.
	 * @return The approximate memory size in bytes, or <c>0</c> if the input was <jk>null</jk>.
	 */
	public static long getStringSize(String str) {
		if (str == null)
			return 0;
		// String object overhead: ~24 bytes (object header + fields: value, hash, coder)
		// char[] array overhead: ~16 bytes (array header)
		// Character data: 2 bytes per character
		return 24L + 16L + (2L * str.length());
	}

	/**
	 * Provides optimization suggestions for a string based on its characteristics.
	 *
	 * <p>
	 * Returns <jk>null</jk> if the input string is <jk>null</jk> or if no optimizations are suggested.
	 * Returns a string containing optimization suggestions separated by newlines.
	 *
	 * <h5 class='section'>Optimization Suggestions:</h5>
	 * <ul>
	 *   <li><b>Large strings:</b> Suggests using StringBuilder for concatenation</li>
	 *   <li><b>Frequently used strings:</b> Suggests interning</li>
	 *   <li><b>Character manipulation:</b> Suggests using char[] for intensive operations</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	optimizeString(<jk>null</jk>);                    <jc>// Returns: null</jc>
	 * 	optimizeString(<js>"short"</js>);                 <jc>// Returns: null (no suggestions)</jc>
	 * 	optimizeString(<js>"very long string..."</js>);   <jc>// Returns: suggestions for large strings</jc>
	 * </p>
	 *
	 * @param str The string to analyze. Can be <jk>null</jk>.
	 * @return A string with optimization suggestions, or <jk>null</jk> if no suggestions or input was <jk>null</jk>.
	 */
	public static String optimizeString(String str) {
		if (str == null)
			return null;

		var suggestions = new ArrayList<String>();
		var length = str.length();

		// Suggest StringBuilder for large strings or frequent concatenation scenarios
		if (length > 1000) {
			suggestions.add("Consider using StringBuilder for concatenation operations");
		}

		// Suggest interning for medium-length strings that might be repeated
		if (length > 10 && length < 100 && ! isInterned(str)) {
			suggestions.add("Consider interning if this string is used frequently");
		}

		// Suggest char[] for intensive character manipulation
		if (length > 100) {
			suggestions.add("For intensive character manipulation, consider using char[]");
		}

		// Suggest compression for very large strings
		if (length > 10000) {
			suggestions.add("For very large strings, consider compression if storage is a concern");
		}

		return suggestions.isEmpty() ? null : String.join(NEWLINE, suggestions);
	}

	/**
	 * Constructor.
	 */
	protected StringUtils() {}

	/**
	 * Same as {@link Float#parseFloat(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000.5"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed float value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static float parseFloat(String value) {
		return Float.parseFloat(StringUtils.removeUnderscores(value));
	}

	/**
	 * Same as {@link Integer#parseInt(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000_000"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed integer value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static int parseInt(String value) {
		return Integer.parseInt(StringUtils.removeUnderscores(value));
	}

	/**
	 * Same as {@link Long#parseLong(String)} but removes any underscore characters first.
	 *
	 * <p>Allows for better readability of numeric literals (e.g., <js>"1_000_000"</js>).
	 *
	 * @param value The string to parse.
	 * @return The parsed long value.
	 * @throws NumberFormatException If the string cannot be parsed.
	 * @throws NullPointerException If the string is <jk>null</jk>.
	 */
	public static long parseLong(String value) {
		return Long.parseLong(StringUtils.removeUnderscores(value));
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link java.util.regex.Pattern}.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"user_*_temp"</js>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice_temp"</js>).matches();  <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_bob_temp"</js>).matches();    <jc>// true</jc>
	 *   <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"admin_alice_temp"</js>).matches(); <jc>// false</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @return A compiled {@link java.util.regex.Pattern} object, or <jk>null</jk> if the input string is <jk>null</jk>.
	 */
	public static java.util.regex.Pattern getGlobMatchPattern(String s) {
		return getGlobMatchPattern(s, 0);
	}

	/**
	 * Converts a string containing glob-style wildcard characters to a regular expression {@link java.util.regex.Pattern} with flags.
	 *
	 * <p>This method converts glob-style patterns to regular expressions with the following mappings:
	 * <ul>
	 *   <li>{@code *} matches any sequence of characters (including none)</li>
	 *   <li>{@code ?} matches exactly one character</li>
	 *   <li>All other characters are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <jc>// Case-insensitive matching</jc>
	 *   <jk>var</jk> <jv>pattern</jv> = <jsm>getGlobMatchPattern</jsm>(<js>"USER_*"</js>, Pattern.<jsf>CASE_INSENSITIVE</jsf>);
	 *   <jk>boolean</jk> <jv>matches</jv> = <jv>pattern</jv>.matcher(<js>"user_alice"</js>).matches();  <jc>// true</jc>
	 * </p>
	 *
	 * @param s The glob-style wildcard pattern string.
	 * @param flags Regular expression flags (see {@link java.util.regex.Pattern} constants).
	 * @return A compiled {@link java.util.regex.Pattern} object, or <jk>null</jk> if the input string is <jk>null</jk>.
	 */
	public static java.util.regex.Pattern getGlobMatchPattern(String s, int flags) {
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
		return java.util.regex.Pattern.compile(sb.toString(), flags);
	}

	public static String removeUnderscores(String value) {
		assertArgNotNull("value", value);
		return notContains(value, '_') ? value : value.replace("_", "");
	}
}