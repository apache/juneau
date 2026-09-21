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
package org.apache.juneau.marshall.uon;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Utility methods for the UON and UrlEncoding serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UonSupport">UON Basics</a>
 * </ul>
 */
public class UonUtils {

	/**
	 * Prevents instantiation.
	 */
	private UonUtils() {}

	private static final AsciiSet needsQuoteChars = AsciiSet.of("),=\n\t\r\b\f ");
	private static final AsciiSet maybeNeedsQuotesFirstChar = AsciiSet.of("),=\n\t\r\b\f tfn+-.#0123456789");

	/**
	 * Returns <jk>true</jk> if the specified string needs to be quoted per UON notation.
	 *
	 * <p>
	 * For example, strings that start with '(' or '@' or look like boolean or numeric values need to be quoted.
	 *
	 * @param s The string to test.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the specified string needs to be quoted per UON notation.
	 */
	public static boolean needsQuotes(String s) {
		var c0 = s.isEmpty() ? 0 : s.charAt(0);
		// @formatter:off
		return (
			s.isEmpty()
			|| c0 == '@'
			|| c0 == '('
			|| needsQuoteChars.contains(s)
			|| (
				maybeNeedsQuotesFirstChar.contains(c0)
				&& (
					"true".equals(s)
					|| "false".equals(s)
					|| "null".equals(s)
					|| isNumeric(s)
				)
			)
		);
		// @formatter:on
	}

	/**
	 * Merges MULTI <c>key=uonValue</c> tokens into a new {@link JsonMap}.
	 *
	 * <p>
	 * Each token is one top-level pair (for example <c>environment=prod</c> or <c>flags=(a,b,c)</c>).
	 * A parenthesized UON object as a single token (<c>(a=1,b=2)</c>) is expanded.
	 * Later keys overwrite earlier ones.  Blank tokens are ignored.
	 * </p>
	 *
	 * @param tokens The raw tokens.  Can be <jk>null</jk>.
	 * @return A new map, never <jk>null</jk>.
	 * @throws ParseException Malformed UON value.
	 * @throws SchemaValidationException If a UON value fails schema validation.
	 */
	public static JsonMap mergePairs(String...tokens) throws ParseException, SchemaValidationException {
		var dest = JsonMap.create();
		if (tokens != null)
			for (var t : tokens)
				mergePair(dest, t);
		return dest;
	}

	/**
	 * Merges one MULTI <c>key=uonValue</c> token into <jv>dest</jv>.
	 *
	 * @param dest The destination map.  Must not be <jk>null</jk>.
	 * @param token The raw token.  Blank tokens are ignored.
	 * @throws ParseException Malformed UON value.
	 * @throws SchemaValidationException If a UON value fails schema validation.
	 */
	public static void mergePair(JsonMap dest, String token) throws ParseException, SchemaValidationException {
		if (isBlank(token))
			return;
		var t = token.trim();
		if (t.startsWith("(") && t.endsWith(")")) {
			for (var piece : splitTopLevelCommas(t.substring(1, t.length() - 1)))
				mergePair(dest, piece);
			return;
		}
		var eq = t.indexOf('=');
		if (eq <= 0)
			return;
		var key = t.substring(0, eq);
		var val = t.substring(eq + 1);
		dest.put(key, isEmpty(val) ? "" : parseValue(val));
	}

	/**
	 * Parses a UON value from the right-hand side of a <c>key=uonValue</c> pair.
	 *
	 * <p>
	 * List-shaped values authored as <c>(a,b,c)</c> (without the UON <c>@</c> array prefix)
	 * are treated as UON arrays.  Unquoted strings that the parser would otherwise coerce to
	 * a number are kept as strings unless they are a UON number literal.
	 * </p>
	 *
	 * @param raw The raw UON value.  Must not be <jk>null</jk>.
	 * @return The parsed value.
	 * @throws ParseException Malformed UON value.
	 * @throws SchemaValidationException If the UON value fails schema validation.
	 */
	public static Object parseValue(String raw) throws ParseException, SchemaValidationException {
		var original = raw.trim();
		var t = original;
		// UON arrays are "@(a,b,c)"; list-shaped values are authored without the "@".
		if (t.startsWith("(") && t.endsWith(")") && t.indexOf('=') < 0)
			t = "@" + t;
		var parsed = UonParser.DEFAULT.read(QUERY, HttpPartSchema.DEFAULT, t, Object.class);
		if (parsed instanceof Number && ! isUonNumberLiteral(original))
			return original;
		return parsed;
	}

	private static List<String> splitTopLevelCommas(String s) {
		List<String> out = l();
		var depth = 0;
		var start = 0;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '(')
				depth++;
			else if (c == ')')
				depth--;
			else if (c == ',' && depth == 0) {
				out.add(s.substring(start, i).trim());
				start = i + 1;
			}
		}
		out.add(s.substring(start).trim());
		out.removeIf(String::isEmpty);
		return out;
	}

	private static boolean isUonNumberLiteral(String s) {
		if (isEmpty(s))
			return false;
		var i = 0;
		if (s.charAt(0) == '-' || s.charAt(0) == '+')
			i = 1;
		var digits = 0;
		var dot = false;
		var exp = false;
		for (; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				digits++;
			} else if (c == '.' && ! dot && ! exp) {
				dot = true;
			} else if ((c == 'e' || c == 'E') && ! exp && digits > 0) {
				exp = true;
				if (i + 1 < s.length() && (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-'))
					i++;
			} else {
				return false;
			}
		}
		return digits > 0;
	}
}