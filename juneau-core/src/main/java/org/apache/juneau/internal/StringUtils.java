/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.internal;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import javax.xml.bind.*;

import org.apache.juneau.parser.*;

/**
 * Reusable string utility methods.
 */
public final class StringUtils {

	private static final AsciiSet numberChars = new AsciiSet("-xX.+-#pP0123456789abcdefABCDEF");
	private static final AsciiSet firstNumberChars = new AsciiSet("+-.#0123456789");
	private static final AsciiSet octChars = new AsciiSet("01234567");
	private static final AsciiSet decChars = new AsciiSet("0123456789");
	private static final AsciiSet hexChars = new AsciiSet("0123456789abcdefABCDEF");

	// Maps 6-bit nibbles to BASE64 characters.
	private static final char[] base64m1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

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
	 * @param type The number type to created. <br>
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
	 * @param type The number type to created. <br>
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
	 * @return The parsed number.
	 * @throws ParseException
	 */
	public static Number parseNumber(String s, Class<? extends Number> type) throws ParseException {

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
				if (isAutoDetect && (! isDecimal) && d >= -Float.MAX_VALUE && d <= Float.MAX_VALUE)
					return d.floatValue();
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
			throw new ParseException("Could not convert string ''{0}'' to class ''{1}''", s, type.getName()).initCause(e);
		}
	}

   private final static Pattern fpRegex = Pattern.compile(
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
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		pw.close();
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
	 * Splits a character-delimited string into a string array.
	 * Does not split on escaped-delimiters (e.g. "\,");
	 * Resulting tokens are trimmed of whitespace.
	 * NOTE:  This behavior is different than the Jakarta equivalent.
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
	 * @return The tokens.
	 */
	public static String[] split(String s, char c) {

		char[] unEscapeChars = new char[]{'\\', c};

		if (s == null)
			return null;
		if (isEmpty(s))
			return new String[0];

		List<String> l = new LinkedList<String>();
		char[] sArray = s.toCharArray();
		int x1 = 0, escapeCount = 0;
		for (int i = 0; i < sArray.length; i++) {
			if (sArray[i] == '\\') escapeCount++;
			else if (sArray[i]==c && escapeCount % 2 == 0) {
				String s2 = new String(sArray, x1, i-x1);
				String s3 = unEscapeChars(s2, unEscapeChars);
				l.add(s3.trim());
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
		List<String> l = new LinkedList<String>();
		for (String ss : s) {
			if (ss == null || ss.indexOf(c) == -1)
				l.add(ss);
			else
				l.addAll(Arrays.asList(split(ss, c)));
		}
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
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or it's {@link #toString()} method returns an empty string.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if specified string is <jk>null</jk> or it's {@link #toString()} method returns an empty string.
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
	 * Shortcut for calling <code>base64Decode(String)</code> and converting the
	 * 	result to a UTF-8 encoded string.
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
	 * Characters are composed of lower-case ASCII letters and numbers only.
	 * This method conforms to the restrictions for hostnames as specified in <a href='https://tools.ietf.org/html/rfc952'>RFC 952</a>
	 * Since each character has 36 possible values, the square approximation formula for
	 * 	the number of generated IDs that would produce a 50% chance of collision is:
	 * <code>sqrt(36^N)</code>.
	 * Dividing this number by 10 gives you an approximation of the number of generated IDs
	 * 	needed to produce a <1% chance of collision.
	 * For example, given 5 characters, the number of generated IDs need to produce a <1% chance of
	 * 	collision would be:
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
	 * @param date The date string.
	 * @return The parsed date.
	 * @throws IllegalArgumentException
	 */
	@SuppressWarnings("nls")
	public static Date parseISO8601Date(String date) throws IllegalArgumentException {
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
	 * Simple utility for replacing variables of the form <js>"{key}"</js> with values
	 * 	in the specified map.
	 * <p>
	 * Nested variables are supported in both the input string and map values.
	 * <p>
	 * If the map does not contain the specified value, the variable is not replaced.
	 *	<p>
	 *	<jk>null</jk> values in the map are treated as blank strings.
	 *
	 * @param s The string containing variables to replace.
	 * @param m The map containing the variable values.
	 * @return The new string with variables replaced, or the original string if it didn't have variables in it.
	 */
	public static String replaceVars(String s, Map<String,Object> m) {

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
	 * <p>
	 * Examples:
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
	 * <p>
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
	 * Returns the specified field in a delimited string without splitting the string.
	 * <p>
	 * Equivalent to the following:
	 * <p class='bcode'>
	 * 	String in = <js>"0,1,2"</js>;
	 * 	String[] parts = in.split(<js>","</js>);
	 * 	String p1 = (parts.<jk>length</jk> > 1 ? parts[1] : <js>""</js>);
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
}
