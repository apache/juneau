// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import javax.xml.bind.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;

/**
 * Reusable string utility methods.
 */
public final class StringUtils {

	private static final AsciiSet numberChars = AsciiSet.create("-xX.+-#pP0123456789abcdefABCDEF");
	private static final AsciiSet firstNumberChars =AsciiSet.create("+-.#0123456789");
	private static final AsciiSet octChars = AsciiSet.create("01234567");
	private static final AsciiSet decChars = AsciiSet.create("0123456789");
	private static final AsciiSet hexChars = AsciiSet.create("0123456789abcdefABCDEF");

	// Maps 6-bit nibbles to BASE64 characters.
	private static final char[] base64m1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	// Characters that do not need to be URL-encoded
	private static final AsciiSet unencodedChars = AsciiSet.create().ranges("a-z","A-Z","0-9").chars("-_.!~*'()\\").build();

	// Characters that really do not need to be URL-encoded
	private static final AsciiSet unencodedCharsLax = unencodedChars.copy()
		.chars(":@$,")  // reserved, but can't be confused in a query parameter.
		.chars("{}|\\^[]`")  // unwise characters.
		.build();

	// Maps BASE64 characters to 6-bit nibbles.
	private static final byte[] base64m2 = new byte[128];
	static {
		for (int i = 0; i < 64; i++)
			base64m2[base64m1[i]] = (byte)i;
	}

	/**
	 * Parses a number from the specified reader stream.
	 * 
	 * @param r The reader to parse the string from.
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
	 * 	If <jk>null</jk>, uses the best guess.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @return The parsed number.
	 * @throws Exception
	 */
	public static Number parseNumber(ParserReader r, Class<? extends Number> type) throws Exception {
		return parseNumber(parseNumberString(r), type);
	}

	/**
	 * Reads a numeric string from the specified reader.
	 * 
	 * @param r The reader to read form.
	 * @return The parsed number string.
	 * @throws Exception
	 */
	public static String parseNumberString(ParserReader r) throws Exception {
		r.mark();
		int c = 0;
		while (true) {
			c = r.read();
			if (c == -1)
				break;
			if (! numberChars.contains((char)c)) {
				r.unread();
				break;
			}
		}
		return r.getMarked();
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
	 * 	If <jk>null</jk> or <code>Number</code>, uses the best guess.
	 * @return The parsed number, or <jk>null</jk> if the string was null.
	 * @throws ParseException
	 */
	public static Number parseNumber(String s, Class<? extends Number> type) throws ParseException {
		if (s == null)
			return null;
		if (s.isEmpty())
			s = "0";
		if (type == null)
			type = Number.class;

		try {
			// Determine the data type if it wasn't specified.
			boolean isAutoDetect = (type == Number.class);
			boolean isDecimal = false;
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
				}
				else if (isFloat(s))
					type = Double.class;
				else
					throw new NumberFormatException(s);
			}

			if (type == Double.class || type == Double.TYPE) {
				Double d = Double.valueOf(s);
				Float f = Float.valueOf(s);
				if (isAutoDetect && (!isDecimal) && d.toString().equals(f.toString()))
					return f;
				return d;
			}
			if (type == Float.class || type == Float.TYPE)
				return Float.valueOf(s);
			if (type == BigDecimal.class)
				return new BigDecimal(s);
			if (type == Long.class || type == Long.TYPE || type == AtomicLong.class) {
				try {
					Long l = Long.decode(s);
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
			throw new ParseException("Unsupported Number type: {0}", type.getName());
		} catch (NumberFormatException e) {
			throw new ParseException(e, "Invalid number: ''{0}'', class=''{1}''", s, type.getSimpleName());
		}
	}

	private static final Pattern fpRegex = Pattern.compile(
		"[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*"
	);

	/**
	 * Returns <jk>true</jk> if this string can be parsed by {@link #parseNumber(String, Class)}.
	 * 
	 * @param s The string to check.
	 * @return <jk>true</jk> if this string can be parsed without causing an exception.
	 */
	public static boolean isNumeric(String s) {
		if (s == null || s.isEmpty())
			return false;
		if (! isFirstNumberChar(s.charAt(0)))
			return false;
		return isDecimal(s) || isFloat(s);
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
		int i = 0;
		int length = s.length();
		char c = s.charAt(0);
		if (c == '+' || c == '-')
			i++;
		if (i == length)
			return false;
		c = s.charAt(i++);
		if (c == '.' || decChars.contains(c)) {
			return fpRegex.matcher(s).matches();
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified string is numeric.
	 * 
	 * @param s The string to check.
	 * @return <jk>true</jk> if the specified string is numeric.
	 */
	public static boolean isDecimal(String s) {
		if (s == null || s.isEmpty())
			return false;
		if (! firstNumberChars.contains(s.charAt(0)))
			return false;
		int i = 0;
		int length = s.length();
		char c = s.charAt(0);
		boolean isPrefixed = false;
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
				for (int j = i; j < length; j++) {
					if (! hexChars.contains(s.charAt(j)))
						return false;
				}
			} else if (octChars.contains(c)) {
				for (int j = i; j < length; j++)
					if (! octChars.contains(s.charAt(j)))
						return false;
			} else {
				return false;
			}
		} else if (c == '#') {
			for (int j = i; j < length; j++) {
				if (! hexChars.contains(s.charAt(j)))
					return false;
			}
		} else if (decChars.contains(c)) {
			for (int j = i; j < length; j++)
				if (! decChars.contains(s.charAt(j)))
					return false;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Convenience method for getting a stack trace as a string.
	 * 
	 * @param t The throwable to get the stack trace from.
	 * @return The same content that would normally be rendered via <code>t.printStackTrace()</code>
	 */
	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			t.printStackTrace(pw);
		}
		return sw.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 * 
	 * @param tokens The tokens to join.
	 * @param separator The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, String separator) {
		if (tokens == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(separator);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(int[] tokens, String d) {
		if (tokens == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, String d) {
		if (tokens == null)
			return null;
		return join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Joins the specified tokens into a delimited string and writes the output to the specified string builder.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <code>sb</code>.
	 */
	public static StringBuilder join(Collection<?> tokens, String d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (Iterator<?> iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
		}
		return sb;
	}

	/**
	 * Joins the specified tokens into a delimited string.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Object[] tokens, char d) {
		if (tokens == null)
			return null;
		return join(tokens, d, new StringBuilder()).toString();
	}

	/**
	 * Join the specified tokens into a delimited string and writes the output to the specified string builder.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @param sb The string builder to append the response to.
	 * @return The same string builder passed in as <code>sb</code>.
	 */
	public static StringBuilder join(Object[] tokens, char d, StringBuilder sb) {
		if (tokens == null)
			return sb;
		for (int i = 0; i < tokens.length; i++) {
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
	 * @param d The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(int[] tokens, char d) {
		if (tokens == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.length; i++) {
			if (i > 0)
				sb.append(d);
			sb.append(tokens[i]);
		}
		return sb.toString();
	}

	/**
	 * Join the specified tokens into a delimited string.
	 * 
	 * @param tokens The tokens to join.
	 * @param d The delimiter.
	 * @return The delimited string.  If <code>tokens</code> is <jk>null</jk>, returns <jk>null</jk>.
	 */
	public static String join(Collection<?> tokens, char d) {
		if (tokens == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (Iterator<?> iter = tokens.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(d);
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
	 * Shortcut for calling <code>split(s, <js>','</js>)</code>
	 * 
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] split(String s) {
		return split(s, ',');
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
	 * split("a,b,c",',') -> {"a","b","c"}
	 * split("a, b ,c ",',') -> {"a","b","c"}
	 * split("a,,c",',') -> {"a","","c"}
	 * split(",,",',') -> {"","",""}
	 * split("",',') -> {}
	 * split(null,',') -> null
	 * split("a,b\,c,d", ',', false) -> {"a","b\,c","d"}
	 * split("a,b\\,c,d", ',', false) -> {"a","b\","c","d"}
	 * split("a,b\,c,d", ',', true) -> {"a","b,c","d"}
	 * 
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] split(String s, char c) {
		return split(s, c, Integer.MAX_VALUE);
	}
	
	/**
	 * Same as {@link #split(String, char)} but limits the number of tokens returned.
	 * 
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @param limit The maximum number of tokens to return.
	 * @return The tokens, or <jk>null</jk> if the string was null.
	 */
	public static String[] split(String s, char c, int limit) {

		char[] unEscapeChars = new char[]{'\\', c};

		if (s == null)
			return null;
		if (isEmpty(s))
			return new String[0];
		if (s.indexOf(c) == -1)
			return new String[]{s};

		List<String> l = new LinkedList<>();
		char[] sArray = s.toCharArray();
		int x1 = 0, escapeCount = 0;
		limit--;
		for (int i = 0; i < sArray.length && limit > 0; i++) {
			if (sArray[i] == '\\') escapeCount++;
			else if (sArray[i]==c && escapeCount % 2 == 0) {
				String s2 = new String(sArray, x1, i-x1);
				String s3 = unEscapeChars(s2, unEscapeChars);
				l.add(s3.trim());
				limit--;
				x1 = i+1;
			}
			if (sArray[i] != '\\') escapeCount = 0;
		}
		String s2 = new String(sArray, x1, sArray.length-x1);
		String s3 = unEscapeChars(s2, unEscapeChars);
		l.add(s3.trim());

		return l.toArray(new String[l.size()]);
	}

	/**
	 * Same as {@link #split(String, char)} except splits all strings in the input and returns a single result.
	 * 
	 * @param s The string to split.  Can be <jk>null</jk>.
	 * @param c The character to split on.
	 * @return The tokens.
	 */
	public static String[] split(String[] s, char c) {
		if (s == null)
			return null;
		List<String> l = new LinkedList<>();
		for (String ss : s) {
			if (ss == null || ss.indexOf(c) == -1)
				l.add(ss);
			else
				l.addAll(Arrays.asList(split(ss, c)));
		}
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Splits a list of key-value pairs into an ordered map.
	 * 
	 * <p>
	 * Example:
	 * <p class='bcode'>
	 * 	String in = <js>"foo=1;bar=2"</js>;
	 * 	Map m = StringUtils.<jsm>splitMap</jsm>(in, <js>';'</js>, <js>'='</js>, <jk>true</jk>);
	 * </p>
	 * 
	 * @param s The string to split.
	 * @param delim The delimiter between the key-value pairs.
	 * @param eq The delimiter between the key and value.
	 * @param trim Trim strings after parsing.
	 * @return The parsed map.  Never <jk>null</jk>.
	 */
	public static Map<String,String> splitMap(String s, char delim, char eq, boolean trim) {

		char[] unEscapeChars = new char[]{'\\', delim, eq};

		if (s == null)
			return null;
		if (isEmpty(s))
			return Collections.EMPTY_MAP;

		Map<String,String> m = new LinkedHashMap<>();

		int
			S1 = 1,  // Found start of key, looking for equals.
			S2 = 2;  // Found equals, looking for delimiter (or end).

		int state = S1;

		char[] sArray = s.toCharArray();
		int x1 = 0, escapeCount = 0;
		String key = null;
		for (int i = 0; i < sArray.length + 1; i++) {
			char c = i == sArray.length ? delim : sArray[i];
			if (c == '\\')
				escapeCount++;
			if (escapeCount % 2 == 0) {
				if (state == S1) {
					if (c == eq) {
						key = s.substring(x1, i);
						if (trim)
							key = trim(key);
						key = unEscapeChars(key, unEscapeChars);
						state = S2;
						x1 = i+1;
					} else if (c == delim) {
						key = s.substring(x1, i);
						if (trim)
							key = trim(key);
						key = unEscapeChars(key, unEscapeChars);
						m.put(key, "");
						state = S1;
						x1 = i+1;
					}
				} else if (state == S2) {
					if (c == delim) {
						String val = s.substring(x1, i);
						if (trim)
							val = trim(val);
						val = unEscapeChars(val, unEscapeChars);
						m.put(key, val);
						key = null;
						x1 = i+1;
						state = S1;
					}
				}
			}
			if (c != '\\') escapeCount = 0;
		}

		return m;
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
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			for (char c2 : chars) 
				if (c == c2)
					return true;
		}
		return false;
	}
	
	/**
	 * Splits a space-delimited string with optionally quoted arguments.
	 * 
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"foo"</js> =&gt; <code>["foo"]</code>
	 * 	<li><js>" foo "</js> =&gt; <code>["foo"]</code>
	 * 	<li><js>"foo bar baz"</js> =&gt; <code>["foo","bar","baz"]</code>
	 * 	<li><js>"foo 'bar baz'"</js> =&gt; <code>["foo","bar baz"]</code>
	 * 	<li><js>"foo \"bar baz\""</js> =&gt; <code>["foo","bar baz"]</code>
	 * 	<li><js>"foo 'bar\'baz'"</js> =&gt; <code>["foo","bar'baz"]</code>
	 * </ul>
	 * 
	 * @param s The input string.
	 * @return 
	 * 	The results, or <jk>null</jk> if the input was <jk>null</jk>.
	 * 	<br>An empty string results in an empty array.
	 */
	public static String[] splitQuoted(String s) {
		char[] unEscapeChars = new char[]{'\\', '\'', '"'};

		if (s == null)
			return null;
		
		s = s.trim();
		
		if (isEmpty(s))
			return new String[0];
		
		if (! containsAny(s, ' ', '\t', '\'', '"'))
			return new String[]{s};

		int 
			S1 = 1,  // Looking for start of token.
			S2 = 2,  // Found ', looking for end '
			S3 = 3,  // Found ", looking for end "
			S4 = 4;  // Found non-whitespace, looking for end whitespace.
		
		int state = S1;
		
		boolean isInEscape = false, needsUnescape = false;
		int mark = 0;
			
		List<String> l = new ArrayList<>();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			
			if (state == S1) {
				if (c == '\'') {
					state = S2;
					mark = i+1;
				} else if (c == '"') {
					state = S3;
					mark = i+1;
				} else if (c != ' ' && c != '\t') {
					state = S4;
					mark = i;
				}
			} else if (state == S2 || state == S3) {
				if (c == '\\') {
					isInEscape = ! isInEscape;
					needsUnescape = true;
				} else if (! isInEscape) {
					if (c == (state == S2 ? '\'' : '"')) {
						String s2 = s.substring(mark, i);
						if (needsUnescape)
							s2 = unEscapeChars(s2, unEscapeChars, '\\');
						l.add(s2);
						state = S1;
						isInEscape = needsUnescape = false;
					}
				} else {
					isInEscape = false;
				}
			} else if (state == S4) {
				if (c == ' ' || c == '\t') {
					l.add(s.substring(mark, i));
					state = S1;
				}
			}
		}
		if (state == S4)
			l.add(s.substring(mark));
		else if (state == S2 || state == S3)
			throw new RuntimeException("Unmatched string quotes: " + s);
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or empty.
	 * 
	 * @param s The string to check.
	 * @return <jk>true</jk> if specified string is <jk>null</jk> or empty.
	 */
	public static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or it's {@link #toString()} method returns an empty
	 * string.
	 * 
	 * @param s The string to check.
	 * @return
	 * 	<jk>true</jk> if specified string is <jk>null</jk> or it's {@link #toString()} method returns an empty string.
	 */
	public static boolean isEmpty(Object s) {
		return s == null || s.toString().isEmpty();
	}

	/**
	 * Returns <jk>null</jk> if the specified string is <jk>null</jk> or empty.
	 * 
	 * @param s The string to check.
	 * @return <jk>null</jk> if the specified string is <jk>null</jk> or empty, or the same string if not.
	 */
	public static String nullIfEmpty(String s) {
		if (s == null || s.isEmpty())
			return null;
		return s;
	}

	/**
	 * Returns an empty string if the specified string is <jk>null</jk>.
	 * 
	 * @param s The string to check.
	 * @return An empty string if the specified string is <jk>null</jk>, or the same string otherwise.
	 */
	public static String emptyIfNull(String s) {
		if (s == null)
			return "";
		return s;
	}

	/**
	 * Removes escape characters (\) from the specified characters.
	 * 
	 * @param s The string to remove escape characters from.
	 * @param toEscape The characters escaped.
	 * @return A new string if characters were removed, or the same string if not or if the input was <jk>null</jk>.
	 */
	public static String unEscapeChars(String s, char[] toEscape) {
		return unEscapeChars(s, toEscape, '\\');
	}

	/**
	 * Removes escape characters (specified by escapeChar) from the specified characters.
	 * 
	 * @param s The string to remove escape characters from.
	 * @param toEscape The characters escaped.
	 * @param escapeChar The escape character.
	 * @return A new string if characters were removed, or the same string if not or if the input was <jk>null</jk>.
	 */
	public static String unEscapeChars(String s, char[] toEscape, char escapeChar) {
		if (s == null) return null;
		if (s.length() == 0 || toEscape == null || toEscape.length == 0 || escapeChar == 0) return s;
		StringBuffer sb = new StringBuffer(s.length());
		char[] sArray = s.toCharArray();
		for (int i = 0; i < sArray.length; i++) {
			char c = sArray[i];

			if (c == escapeChar) {
				if (i+1 != sArray.length) {
					char c2 = sArray[i+1];
					boolean isOneOf = false;
					for (int j = 0; j < toEscape.length && ! isOneOf; j++)
						isOneOf = (c2 == toEscape[j]);
					if (isOneOf) {
						i++;
					} else if (c2 == escapeChar) {
						sb.append(escapeChar);
						i++;
					}
				}
			}
			sb.append(sArray[i]);
		}
		return sb.toString();
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
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (c < ' ' || c > '~')
				sb.append("["+Integer.toHexString(c)+"]");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * An efficient method for checking if a string starts with a character.
	 * 
	 * @param s The string to check.  Can be <jk>null</jk>.
	 * @param c The character to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and starts with the specified character.
	 */
	public static boolean startsWith(String s, char c) {
		if (s != null) {
			int i = s.length();
			if (i > 0)
				return s.charAt(0) == c;
		}
		return false;
	}

	/**
	 * An efficient method for checking if a string ends with a character.
	 * 
	 * @param s The string to check.  Can be <jk>null</jk>.
	 * @param c The character to check for.
	 * @return <jk>true</jk> if the specified string is not <jk>null</jk> and ends with the specified character.
	 */
	public static boolean endsWith(String s, char c) {
		if (s != null) {
			int i = s.length();
			if (i > 0)
				return s.charAt(i-1) == c;
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
		if (s != null) {
			int i = s.length();
			if (i > 0) {
				char c2 = s.charAt(i-1);
				for (char cc : c)
					if (c2 == cc)
						return true;
			}
		}
		return false;
	}

	/**
	 * Converts the specified number into a 2 hexadecimal characters.
	 * 
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[2]</code> containing the specified characters.
	 */
	public static final char[] toHex2(int num) {
		if (num < 0 || num > 255)
			throw new NumberFormatException("toHex2 can only be used on numbers between 0 and 255");
		char[] n = new char[2];
		int a = num%16;
		n[1] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		a = (num/16)%16;
		n[0] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		return n;
	}

	/**
	 * Converts the specified number into a 4 hexadecimal characters.
	 * 
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[4]</code> containing the specified characters.
	 */
	public static final char[] toHex4(int num) {
		char[] n = new char[4];
		int a = num%16;
		n[3] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		int base = 16;
		for (int i = 1; i < 4; i++) {
			a = (num/base)%16;
			base <<= 4;
			n[3-i] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		}
		return n;
	}

	/**
	 * Converts the specified number into a 8 hexadecimal characters.
	 * 
	 * @param num The number to convert to hex.
	 * @return A <code><jk>char</jk>[8]</code> containing the specified characters.
	 */
	public static final char[] toHex8(long num) {
		char[] n = new char[8];
		long a = num%16;
		n[7] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		int base = 16;
		for (int i = 1; i < 8; i++) {
			a = (num/base)%16;
			base <<= 4;
			n[7-i] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		}
		return n;
	}

	/**
	 * Tests two strings for equality, but gracefully handles nulls.
	 * 
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean isEquals(String s1, String s2) {
		if (s1 == null)
			return s2 == null;
		if (s2 == null)
			return false;
		return s1.equals(s2);
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
		return base64Encode(in.getBytes(IOUtils.UTF8));
	}

	/**
	 * BASE64-encodes the specified byte array.
	 * 
	 * @param in The input byte array to convert.
	 * @return The byte array converted to a BASE-64 encoded string.
	 */
	public static String base64Encode(byte[] in) {
		int outLength = (in.length * 4 + 2) / 3;   // Output length without padding
		char[] out = new char[((in.length + 2) / 3) * 4];  // Length includes padding.
		int iIn = 0;
		int iOut = 0;
		while (iIn < in.length) {
			int i0 = in[iIn++] & 0xff;
			int i1 = iIn < in.length ? in[iIn++] & 0xff : 0;
			int i2 = iIn < in.length ? in[iIn++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			int o3 = i2 & 0x3F;
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
	 * Shortcut for calling <code>base64Decode(String)</code> and converting the result to a UTF-8 encoded string.
	 * 
	 * @param in The BASE-64 encoded string to decode.
	 * @return The decoded string.
	 */
	public static String base64DecodeToString(String in) {
		byte[] b = base64Decode(in);
		if (b == null)
			return null;
		return new String(b, IOUtils.UTF8);
	}

	/**
	 * BASE64-decodes the specified string.
	 * 
	 * @param in The BASE-64 encoded string.
	 * @return The decoded byte array.
	 */
	public static byte[] base64Decode(String in) {
		if (in == null)
			return null;

		byte bIn[] = in.getBytes(IOUtils.UTF8);

		if (bIn.length % 4 != 0)
			illegalArg("Invalid BASE64 string length.  Must be multiple of 4.");

		// Strip out any trailing '=' filler characters.
		int inLength = bIn.length;
		while (inLength > 0 && bIn[inLength - 1] == '=')
			inLength--;

		int outLength = (inLength * 3) / 4;
		byte[] out = new byte[outLength];
		int iIn = 0;
		int iOut = 0;
		while (iIn < inLength) {
			int i0 = bIn[iIn++];
			int i1 = bIn[iIn++];
			int i2 = iIn < inLength ? bIn[iIn++] : 'A';
			int i3 = iIn < inLength ? bIn[iIn++] : 'A';
			int b0 = base64m2[i0];
			int b1 = base64m2[i1];
			int b2 = base64m2[i2];
			int b3 = base64m2[i3];
			int o0 = (b0 << 2) | (b1 >>> 4);
			int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
			int o2 = ((b2 & 3) << 6) | b3;
			out[iOut++] = (byte)o0;
			if (iOut < outLength)
				out[iOut++] = (byte)o1;
			if (iOut < outLength)
				out[iOut++] = (byte)o2;
		}
		return out;
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
	 * <code>sqrt(36^N)</code>.
	 * Dividing this number by 10 gives you an approximation of the number of generated IDs needed to produce a
	 * &lt;1% chance of collision.
	 * 
	 * <p>
	 * For example, given 5 characters, the number of generated IDs need to produce a &lt;1% chance of collision would
	 * be:
	 * <code>sqrt(36^5)/10=777</code>
	 * 
	 * @param numchars The number of characters in the generated UUID.
	 * @return A new random UUID.
	 */
	public static String generateUUID(int numchars) {
		Random r = new Random();
		StringBuilder sb = new StringBuilder(numchars);
		for (int i = 0; i < numchars; i++) {
			int c = r.nextInt(36) + 97;
			if (c > 'z')
				c -= ('z'-'0'+1);
			sb.append((char)c);
		}
		return sb.toString();
	}

	/**
	 * Same as {@link String#trim()} but prevents <code>NullPointerExceptions</code>.
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
	 * Parses an ISO8601 string into a date.
	 * 
	 * <p>
	 * Supports any of the following formats:
	 * <br><code>yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS</code>
	 * 
	 * @param date The date string.
	 * @return The parsed date.
	 * @throws IllegalArgumentException
	 */
	public static Date parseIsoDate(String date) throws IllegalArgumentException {
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
		return DatatypeConverter.parseDateTime(date).getTime();
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

		if (s.indexOf('{') == -1)
			return s;

		int S1 = 1;	   // Not in variable, looking for {
		int S2 = 2;    // Found {, Looking for }

		int state = S1;
		boolean hasInternalVar = false;
		int x = 0;
		int depth = 0;
		int length = s.length();
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
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
						String key = s.substring(x+1, i);
						key = (hasInternalVar ? replaceVars(key, m) : key);
						hasInternalVar = false;
						if (! m.containsKey(key))
							out.append('{').append(key).append('}');
						else {
							Object val = m.get(key);
							if (val == null)
								val = "";
							String v = val.toString();
							// If the replacement also contains variables, replace them now.
							if (v.indexOf('{') != -1)
								v = replaceVars(v, m);
							out.append(v);
						}
						state = 1;
					}
				}
			}
		}
		return out.toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified path string is prefixed with the specified prefix.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	pathStartsWith(<js>"foo"</js>, <js>"foo"</js>);  <jc>// true</jc>
	 * 	pathStartsWith(<js>"foo/bar"</js>, <js>"foo"</js>);  <jc>// true</jc>
	 * 	pathStartsWith(<js>"foo2"</js>, <js>"foo"</js>);  <jc>// false</jc>
	 * 	pathStartsWith(<js>"foo2"</js>, <js>""</js>);  <jc>// false</jc>
	 * </p>
	 * 
	 * @param path The path to check.
	 * @param pathPrefix The prefix.
	 * @return <jk>true</jk> if the specified path string is prefixed with the specified prefix.
	 */
	public static boolean pathStartsWith(String path, String pathPrefix) {
		if (path == null || pathPrefix == null)
			return false;
		if (path.startsWith(pathPrefix))
			return path.length() == pathPrefix.length() || path.charAt(pathPrefix.length()) == '/';
		return false;
	}

	/**
	 * Same as {@link #pathStartsWith(String, String)} but returns <jk>true</jk> if at least one prefix matches.
	 * 
	 * @param path The path to check.
	 * @param pathPrefixes The prefixes.
	 * @return <jk>true</jk> if the specified path string is prefixed with any of the specified prefixes.
	 */
	public static boolean pathStartsWith(String path, String[] pathPrefixes) {
		for (String p : pathPrefixes)
			if (pathStartsWith(path, p))
				return true;
		return false;
	}

	/**
	 * Replaces <js>"\\uXXXX"</js> character sequences with their unicode characters.
	 * 
	 * @param s The string to replace unicode sequences in.
	 * @return A string with unicode sequences replaced.
	 */
	public static String replaceUnicodeSequences(String s) {
		if (s.indexOf('\\') == -1)
			return s;
		Pattern p = Pattern.compile("\\\\u(\\p{XDigit}{4})");
		Matcher m = p.matcher(s);
		StringBuffer sb = new StringBuffer(s.length());
		while (m.find()) {
			String ch = String.valueOf((char) Integer.parseInt(m.group(1), 16));
			m.appendReplacement(sb, Matcher.quoteReplacement(ch));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Creates an escaped-unicode sequence (e.g. <js>"\\u1234"</js>) for the specified character.
	 * 
	 * @param c The character to create a sequence for.
	 * @return An escaped-unicode sequence.
	 */
	public static String unicodeSequence(char c) {
		StringBuilder sb = new StringBuilder(6);
		sb.append('\\').append('u');
		for (char cc : toHex4(c))
			sb.append(cc);
		return sb.toString();
	}
	
	/**
	 * Returns the specified field in a delimited string without splitting the string.
	 * 
	 * <p>
	 * Equivalent to the following:
	 * <p class='bcode'>
	 * 	String in = <js>"0,1,2"</js>;
	 * 	String[] parts = in.split(<js>","</js>);
	 * 	String p1 = (parts.<jk>length</jk> > 1 ? parts[1] : <js>""</js>);
	 * </p>
	 * 
	 * @param fieldNum The field number.  Zero-indexed.
	 * @param s The input string.
	 * @param delim The delimiter character.
	 * @return The field entry in the string, or a blank string if it doesn't exist or the string is null.
	 */
	public static String getField(int fieldNum, String s, char delim) {
		return getField(fieldNum, s, delim, "");
	}

	/**
	 * Same as {@link #getField(int, String, char)} except allows you to specify the default value.
	 * 
	 * @param fieldNum The field number.  Zero-indexed.
	 * @param s The input string.
	 * @param delim The delimiter character.
	 * @param def The default value if the field does not exist.
	 * @return The field entry in the string, or the default value if it doesn't exist or the string is null.
	 */
	public static String getField(int fieldNum, String s, char delim, String def) {
		if (s == null || fieldNum < 0)
			return def;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == delim) {
				fieldNum--;
				if (fieldNum == 0)
					start = i+1;
			}
			if (fieldNum < 0)
				return s.substring(start, i);
		}
		if (start == 0)
			return def;
		return s.substring(start);
	}

	/**
	 * Calls {@link #toString()} on the specified object if it's not null.
	 * 
	 * @param o The object to convert to a string.
	 * @return The object converted to a string, or <jk>null</jk> if the object was null.
	 */
	public static String asString(Object o) {
		return (o == null ? null : o.toString());
	}
	
	/**
	 * Converts an array of objects to an array of strings.
	 * 
	 * @param o The array of objects to convert to strings.
	 * @return A new array of objects converted to strings.
	 */
	public static String[] asStrings(Object...o) {
		if (o == null)
			return null;
		if (o instanceof String[])
			return (String[])o;
		String[] s = new String[o.length];
		for (int i = 0; i < o.length; i++)
			s[i] = asString(o[i]);
		return s;
	}

	/**
	 * Converts a hexadecimal byte stream (e.g. "34A5BC") into a UTF-8 encoded string.
	 * 
	 * @param hex The hexadecimal string.
	 * @return The UTF-8 string.
	 */
	public static String fromHexToUTF8(String hex) {
		ByteBuffer buff = ByteBuffer.allocate(hex.length()/2);
		for (int i = 0; i < hex.length(); i+=2)
			buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
		buff.rewind();
		Charset cs = Charset.forName("UTF-8");
		return cs.decode(buff).toString();
	}

	/**
	 * Converts a space-deliminted hexadecimal byte stream (e.g. "34 A5 BC") into a UTF-8 encoded string.
	 * 
	 * @param hex The hexadecimal string.
	 * @return The UTF-8 string.
	 */
	public static String fromSpacedHexToUTF8(String hex) {
		ByteBuffer buff = ByteBuffer.allocate((hex.length()+1)/3);
		for (int i = 0; i < hex.length(); i+=3)
			buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
		buff.rewind();
		Charset cs = Charset.forName("UTF-8");
		return cs.decode(buff).toString();
	}

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	/**
	 * Converts a byte array into a simple hexadecimal character string.
	 * 
	 * @param bytes The bytes to convert to hexadecimal.
	 * @return A new string consisting of hexadecimal characters.
	 */
	public static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
		}
		return sb.toString();
	}

	/**
	 * Same as {@link #toHex(byte[])} but puts spaces between the byte strings.
	 * 
	 * @param bytes The bytes to convert to hexadecimal.
	 * @return A new string consisting of hexadecimal characters.
	 */
	public static String toSpacedHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int j = 0; j < bytes.length; j++) {
			if (j > 0)
				sb.append(' ');
			int v = bytes[j] & 0xFF;
			sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
		}
		return sb.toString();
	}

	/**
	 * Converts a hexadecimal character string to a byte array.
	 * 
	 * @param hex The string to convert to a byte array.
	 * @return A new byte array.
	 */
	public static byte[] fromHex(String hex) {
		ByteBuffer buff = ByteBuffer.allocate(hex.length()/2);
		for (int i = 0; i < hex.length(); i+=2)
			buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
		buff.rewind();
		return buff.array();
	}

	/**
	 * Same as {@link #fromHex(String)} except expects spaces between the byte strings.
	 * 
	 * @param hex The string to convert to a byte array.
	 * @return A new byte array.
	 */
	public static byte[] fromSpacedHex(String hex) {
		ByteBuffer buff = ByteBuffer.allocate((hex.length()+1)/3);
		for (int i = 0; i < hex.length(); i+=3)
			buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
		buff.rewind();
		return buff.array();
	}

	/**
	 * Creates a repeated pattern.
	 * 
	 * @param count The number of times to repeat the pattern.
	 * @param pattern The pattern to repeat.
	 * @return A new string consisting of the repeated pattern.
	 */
	public static String repeat(int count, String pattern) {
		StringBuilder sb = new StringBuilder(pattern.length() * count);
		for (int i = 0; i < count; i++)
			sb.append(pattern);
		return sb.toString();
	}

	/**
	 * Trims whitespace characters from the beginning of the specified string.
	 * 
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public static String trimStart(String s) {
		if (s != null)
			while (s.length() > 0 && Character.isWhitespace(s.charAt(0)))
				s = s.substring(1);
		return s;
	}

	/**
	 * Trims whitespace characters from the end of the specified string.
	 * 
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public static String trimEnd(String s) {
		if (s != null)
			while (s.length() > 0 && Character.isWhitespace(s.charAt(s.length()-1)))
				s = s.substring(0, s.length()-1);
		return s;
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
		for (int i = 0; i < values.length; i++)
			if (StringUtils.isEquals(s, values[i]))
				return true;
		return false;
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
		while (endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		while (s.length() > 0 && s.charAt(0) == '/')
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
			s = s.substring(0, s.length()-1);
		return s;
	}

	/**
	 * Trims <js>'/'</js> characters from the end of the specified string.
	 * 
	 * @param s The string to trim.
	 * @return The same string buffer.
	 */
	public static StringBuffer trimTrailingSlashes(StringBuffer s) {
		if (s == null)
			return null;
		while (s.length() > 0 && s.charAt(s.length()-1) == '/')
			s.setLength(s.length()-1);
		return s;
	}

	/**
	 * Shortcut for calling <code>URLEncoder.<jsm>encode</jsm>(o.toString(), <js>"UTF-8"</js>)</code>.
	 * 
	 * @param o The object to encode.
	 * @return The URL encoded string, or <jk>null</jk> if the object was null.
	 */
	public static String urlEncode(Object o) {
		try {
			if (o != null)
				return URLEncoder.encode(o.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		return null;
	}

	private static final AsciiSet URL_ENCODE_PATHINFO_VALIDCHARS = 
		AsciiSet.create().ranges("a-z","A-Z","0-9").chars("-_.*/()").build();
	
	/**
	 * Similar to {@link #urlEncode(Object)} but doesn't encode <js>"/"</js> characters.
	 * 
	 * @param o The object to encode.
	 * @return The URL encoded string, or <jk>null</jk> if the object was null.
	 */
	public static String urlEncodePath(Object o) {
		if (o == null)
			return null;
		String s = asString(o);

		boolean needsEncode = false;
		for (int i = 0; i < s.length() && ! needsEncode; i++) 
			needsEncode = URL_ENCODE_PATHINFO_VALIDCHARS.contains(s.charAt(i));
		if (! needsEncode)
			return s;
		
		StringBuilder sb = new StringBuilder();
		CharArrayWriter caw = new CharArrayWriter();
		int caseDiff = ('a' - 'A');
		
		for (int i = 0; i < s.length();) {
			char c = s.charAt(i);
			if (URL_ENCODE_PATHINFO_VALIDCHARS.contains(c)) {
				sb.append(c);
				i++;
			} else {
				if (c == ' ') {
					sb.append('+');
					i++;
				} else {
					do {
						caw.write(c);
						if (c >= 0xD800 && c <= 0xDBFF) {
							if ( (i+1) < s.length()) {
								int d = s.charAt(i+1);
								if (d >= 0xDC00 && d <= 0xDFFF) {
									caw.write(d);
									i++;
								}
							}
						}
						i++;
					} while (i < s.length() && !URL_ENCODE_PATHINFO_VALIDCHARS.contains((c = s.charAt(i))));
					
					caw.flush();
					String s2 = new String(caw.toCharArray());
					byte[] ba = s2.getBytes(IOUtils.UTF8);
					for (int j = 0; j < ba.length; j++) {
						sb.append('%');
						char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
						if (Character.isLetter(ch)) {
							ch -= caseDiff;
						}
						sb.append(ch);
						ch = Character.forDigit(ba[j] & 0xF, 16);
						if (Character.isLetter(ch)) {
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
	 * Decodes a <code>application/x-www-form-urlencoded</code> string using <code>UTF-8</code> encoding scheme.
	 * 
	 * @param s The string to decode.
	 * @return The decoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlDecode(String s) {
		if (s == null)
			return s;
		boolean needsDecode = false;
		for (int i = 0; i < s.length() && ! needsDecode; i++) {
			char c = s.charAt(i);
			if (c == '+' || c == '%')
				needsDecode = true;
		}
		if (needsDecode) {
			try {
				return URLDecoder.decode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {/* Won't happen */}
		}
		return s;
	}

	/**
	 * Encodes a <code>application/x-www-form-urlencoded</code> string using <code>UTF-8</code> encoding scheme.
	 * 
	 * @param s The string to encode.
	 * @return The encoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlEncode(String s) {
		if (s == null)
			return null;
		boolean needsEncode = false;
		for (int i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode |= (! unencodedChars.contains(s.charAt(i)));
		if (needsEncode) {
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {/* Won't happen */}
		}
		return s;
	}
	
	/**
	 * Same as {@link #urlEncode(String)} except only excapes characters that absolutely need to be escaped.
	 * 
	 * @param s The string to escape.
	 * @return The encoded string, or <jk>null</jk> if input is <jk>null</jk>.
	 */
	public static String urlEncodeLax(String s) {
		if (s == null)
			return null;
		boolean needsEncode = false;
		for (int i = 0; i < s.length() && ! needsEncode; i++)
			needsEncode |= (! unencodedCharsLax.contains(s.charAt(i)));
		if (needsEncode) {
			StringBuilder sb = new StringBuilder(s.length()*2);
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (unencodedCharsLax.contains(c))
					sb.append(c);
				else if (c == ' ')
					sb.append("+");
				else if (c <= 127)
					sb.append('%').append(toHex2(c));
				else
					try {
						sb.append(URLEncoder.encode(""+c, "UTF-8"));  // Yuck.
					} catch (UnsupportedEncodingException e) {
						// Not possible.
					} 
			}
			s = sb.toString();
		}
		return s;
	}

	/**
	 * Splits a string into equally-sized parts.
	 * 
	 * @param s The string to split.
	 * @param size The token sizes.
	 * @return The tokens, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static List<String> splitEqually(String s, int size) {
		if (s == null)
			return null;
		if (size <= 0) 
			return Collections.singletonList(s);
		
		List<String> l = new ArrayList<>((s.length() + size - 1) / size);

		for (int i = 0; i < s.length(); i += size) 
			l.add(s.substring(i, Math.min(s.length(), i + size)));

		return l;
	}

	/**
	 * Returns the first non-whitespace character in the string.
	 * 
	 * @param s The string to check.
	 * @return
	 * 	The first non-whitespace character, or <code>0</code> if the string is <jk>null</jk>, empty, or composed
	 * 	of only whitespace.
	 */
	public static char firstNonWhitespaceChar(String s) {
		if (s != null)
			for (int i = 0; i < s.length(); i++)
				if (! Character.isWhitespace(s.charAt(i)))
					return s.charAt(i);
		return 0;
	}

	/**
	 * Returns the last non-whitespace character in the string.
	 * 
	 * @param s The string to check.
	 * @return
	 * 	The last non-whitespace character, or <code>0</code> if the string is <jk>null</jk>, empty, or composed
	 * 	of only whitespace.
	 */
	public static char lastNonWhitespaceChar(String s) {
		if (s != null)
			for (int i = s.length()-1; i >= 0; i--)
				if (! Character.isWhitespace(s.charAt(i)))
					return s.charAt(i);
		return 0;
	}

	/**
	 * Returns the character at the specified index in the string without throwing exceptions.
	 * 
	 * @param s The string.
	 * @param i The index position.
	 * @return
	 * 	The character at the specified index, or <code>0</code> if the index is out-of-range or the string
	 * 	is <jk>null</jk>.
	 */
	public static char charAt(String s, int i) {
		if (s == null)
			return 0;
		if (i < 0 || i >= s.length())
			return 0;
		return s.charAt(i);
	}

	/**
	 * Efficiently determines whether a URL is of the pattern "xxx://xxx"
	 * 
	 * @param s The string to test.
	 * @return <jk>true</jk> if it's an absolute path.
	 */
	public static boolean isAbsoluteUri(String s) {

		if (isEmpty(s))
			return false;

		// Use a state machine for maximum performance.

		int S1 = 1;  // Looking for http
		int S2 = 2;  // Found http, looking for :
		int S3 = 3;  // Found :, looking for /
		int S4 = 4;  // Found /, looking for /
		int S5 = 5;  // Found /, looking for x

		int state = S1;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
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
			} else if (state == S3) {
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
	 * Efficiently determines whether a URL is of the pattern "xxx:/xxx".
	 * 
	 * <p>
	 * The pattern matched is: <code>[a-z]{2,}\:\/.*</code>
	 * 
	 * <p>
	 * Note that this excludes filesystem paths such as <js>"C:/temp"</js>.
	 * 
	 * @param s The string to test.
	 * @return <jk>true</jk> if it's an absolute path.
	 */
	public static boolean isUri(String s) {

		if (isEmpty(s))
			return false;

		// Use a state machine for maximum performance.

		int S1 = 1;  // Looking for protocol char 1
		int S2 = 2;  // Found protocol char 1, looking for protocol char 2
		int S3 = 3;  // Found protocol char 2, looking for :
		int S4 = 4;  // Found :, looking for /


		int state = S1;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
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
			} else if (state == S3) {
				if (c == ':')
					state = S4;
				else if (c < 'a' || c > 'z')
					return false;
			} else if (state == S4) {
				if (c == '/')
					return true;
				return false;
			}
		}
		return false;
	}

	/**
	 * Given an absolute URI, returns just the authority portion (e.g. <js>"http://hostname:port"</js>)
	 * 
	 * @param s The URI string.
	 * @return Just the authority portion of the URI.
	 */
	public static String getAuthorityUri(String s) {

		// Use a state machine for maximum performance.

		int S1 = 1;  // Looking for http
		int S2 = 2;  // Found http, looking for :
		int S3 = 3;  // Found :, looking for /
		int S4 = 4;  // Found /, looking for /
		int S5 = 5;  // Found /, looking for x
		int S6 = 6;  // Found x, looking for /

		int state = S1;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
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
			} else if (state == S3) {
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
				if (c == '/')
					return s.substring(0, i);
			}
		}
		return s;
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
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the first non-null, non-empty string in the list.
	 * 
	 * @param s The strings to test.
	 * @return The first non-empty string in the list, or <jk>null</jk> if they were all <jk>null</jk> or empty.
	 */
	public static String firstNonEmpty(String...s) {
		for (String ss : s)
			if (! isEmpty(ss))
				return ss;
		return null;
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
		for (int i = 0; i < s.length(); i++) {
			char c2 = s.charAt(i);
			for (char cc : c)
				if (c2 == cc)
					return i;
		}
		return -1;
	}

	/**
	 * Similar to {@link MessageFormat#format(String, Object...)} except allows you to specify POJO arguments.
	 * 
	 * @param pattern The string pattern.
	 * @param args The arguments.
	 * @return The formatted string.
	 */
	public static String format(String pattern, Object...args) {
		if (args == null || args.length == 0)
			return pattern;
		for (int i = 0; i < args.length; i++)
			args[i] = convertToReadable(args[i]);
		return MessageFormat.format(pattern, args);
	}

	private static Object convertToReadable(Object o) {
		if (o == null)
			return null;
		if (o instanceof ClassMeta)
			return ((ClassMeta<?>)o).getReadableName();
		if (BeanContext.DEFAULT == null)
			return o.toString();
		ClassMeta<?> cm = BeanContext.DEFAULT.getClassMetaForObject(o);
		if (cm.isMapOrBean() || cm.isCollectionOrArray())
			return JsonSerializer.DEFAULT_LAX.toString(o);
		if (cm.isClass())
			return ((Class<?>)o).getName();
		if (cm.isMethod())
			return ClassUtils.toString((Method)o);
		return o.toString();
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
	 * </ul>
	 * 
	 * @param s The string to parse.
	 * @return The parsed value.
	 */
	public static int parseIntWithSuffix(String s) {
		assertFieldNotNull(s, "s");
		int m = 1;
		if (s.endsWith("G")) {
			m = 1024*1024*1024;
			s = s.substring(0, s.length()-1).trim();
		} else if (s.endsWith("M")) {
			m = 1024*1024;
			s = s.substring(0, s.length()-1).trim();
		} else if (s.endsWith("K")) {
			m = 1024;
			s = s.substring(0, s.length()-1).trim();
		}
		return Integer.decode(s) * m;
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
	 * </ul>
	 * 
	 * @param s The string to parse.
	 * @return The parsed value.
	 */
	public static long parseLongWithSuffix(String s) {
		assertFieldNotNull(s, "s");
		int m = 1;
		if (s.endsWith("G")) {
			m = 1024*1024*1024;
			s = s.substring(0, s.length()-1).trim();
		} else if (s.endsWith("M")) {
			m = 1024*1024;
			s = s.substring(0, s.length()-1).trim();
		} else if (s.endsWith("K")) {
			m = 1024;
			s = s.substring(0, s.length()-1).trim();
		}
		return Long.decode(s) * m;
	}
	
	/**
	 * Same as {@link String#contains(CharSequence)} except returns <jk>null</jk> if the value is null.
	 * 
	 * @param value The string to check.
	 * @param substring The value to check for.
	 * @return <jk>true</jk> if the value contains the specified substring.
	 */
	public static boolean contains(String value, CharSequence substring) {
		return value == null ? false : value.contains(substring);
	}
	
	/**
	 * Returns <jk>true</jk> if the specified string appears to be an JSON array.
	 * 
	 * @param o The object to test.
	 * @param ignoreWhitespaceAndComments If <jk>true</jk>, leading and trailing whitespace and comments will be ignored.
	 * @return <jk>true</jk> if the specified string appears to be a JSON array.
	 */
	public static boolean isObjectList(Object o, boolean ignoreWhitespaceAndComments) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			if (! ignoreWhitespaceAndComments)
				return (s.startsWith("[") && s.endsWith("]"));
			if (firstRealCharacter(s) != '[')
				return false;
			int i = s.lastIndexOf(']');
			if (i == -1)
				return false;
			s = s.substring(i+1);
			if (firstRealCharacter(s) != -1)
				return false;
			return true;
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
	public static boolean isObjectMap(Object o, boolean ignoreWhitespaceAndComments) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			if (! ignoreWhitespaceAndComments)
				return (s.startsWith("{") && s.endsWith("}"));
			if (firstRealCharacter(s) != '{')
				return false;
			int i = s.lastIndexOf('}');
			if (i == -1)
				return false;
			s = s.substring(i+1);
			if (firstRealCharacter(s) != -1)
				return false;
			return true;
		}
		return false;
	}
	
	private static int firstRealCharacter(String s) {
		try (StringReader r = new StringReader(s)) {
			int c = 0;
			while ((c = r.read()) != -1) {
				if (! Character.isWhitespace(c)) {
					if (c == '/') {
						skipComments(r);
					} else {
						return c;
					}
				}
			}
			return -1;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private static void skipComments(StringReader r) throws IOException {
		int c = r.read();
		//  "/* */" style comments
		if (c == '*') {
			while (c != -1)
				if ((c = r.read()) == '*')
					if ((c = r.read()) == '/')
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
		String[] lines = s.split("[\r\n]+");
		final int digits = String.valueOf(lines.length).length();
		if (start < 1)
			start = 1;
		if (end > lines.length)
			end = lines.length;
		StringBuilder sb = new StringBuilder();
		for (String l :  Arrays.asList(lines).subList(start-1, end)) 
			sb.append(String.format("%0"+digits+"d", start++)).append(": ").append(l).append("\n");
		return sb.toString();
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
	 * Returns the first character in the specified string.
	 * 
	 * @param s The string to check.
	 * @return The first character in the string, or <code>0</code> if the string is <jk>null</jk> or empty.
	 */
	public static char firstChar(String s) {
		if (s == null || s.length() == 0)
			return 0;
		return s.charAt(0);
	}
}
