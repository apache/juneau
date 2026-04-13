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
package org.apache.juneau.commons.http;

import java.util.*;

/**
 * Lightweight parser for HTTP header values as defined by RFC 2616.
 *
 * <p>
 * Parses comma-delimited header values into {@link HeaderElement} objects, where each element
 * has a name and optional semicolon-delimited parameters.
 *
 * <p>
 * For example, the header value <js>"text/html;charset=UTF-8;q=0.9, text/json"</js> is parsed into two
 * elements: <js>"text/html"</js> with parameters <js>"charset=UTF-8"</js> and <js>"q=0.9"</js>,
 * and <js>"text/json"</js> with no parameters.
 */
public final class HeaderValueParser {

	/**
	 * Parses an HTTP header value into an array of {@link HeaderElement} objects.
	 *
	 * <p>
	 * Elements are separated by commas. Each element may have parameters separated by semicolons.
	 * Quoted strings are respected (commas and semicolons inside quotes are not treated as delimiters).
	 *
	 * @param value The header value to parse. May be <jk>null</jk> or empty.
	 * @return An array of parsed elements, never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for parser logic
	})
	public static HeaderElement[] parseElements(String value) {
		if (value == null || value.isEmpty())
			return new HeaderElement[0];

		var elements = new ArrayList<HeaderElement>();
		var len = value.length();
		var pos = 0;

		while (pos < len) {
			pos = skipWhitespace(value, pos);
			if (pos >= len)
				break;

			var name = parseName(value, pos);
			pos += name.length();
			name = name.trim();

			var params = new ArrayList<NameValuePair>();

			while (pos < len && value.charAt(pos) == ';') {
				pos++;
				pos = skipWhitespace(value, pos);

				var paramName = parseToken(value, pos, '=', ';', ',');
				pos += paramName.length();
				paramName = paramName.trim();

				String paramValue = null;
				if (pos < len && value.charAt(pos) == '=') {
					pos++;
					pos = skipWhitespace(value, pos);
					if (pos < len && value.charAt(pos) == '"') {
						var q = parseQuotedString(value, pos);
						paramValue = q.text();
						pos = q.nextPos();
					} else {
						paramValue = parseToken(value, pos, ';', ',');
						pos += paramValue.length();
						paramValue = paramValue.trim();
					}
				}

				if (!paramName.isEmpty())
					params.add(new BasicNameValuePair(paramName, paramValue));
			}

			if (!name.isEmpty())
				elements.add(new HeaderElement(name, params.toArray(NameValuePair.EMPTY_ARRAY)));

			if (pos < len && value.charAt(pos) == ',')
				pos++;
		}

		return elements.toArray(new HeaderElement[0]);
	}

	private static String parseName(String value, int pos) {
		return parseToken(value, pos, ';', ',');
	}

	private static String parseToken(String value, int pos, char... delimiters) {
		var start = pos;
		var len = value.length();
		var inQuotes = false;

		while (pos < len) {
			var c = value.charAt(pos);
			if (c == '"')
				inQuotes = !inQuotes;
			if (!inQuotes) {
				for (var d : delimiters)
					if (c == d)
						return value.substring(start, pos);
			}
			pos++;
		}
		return value.substring(start, pos);
	}

	/**
	 * Parses a double-quoted header parameter value, honoring {@code \} escapes.
	 *
	 * @param value Full header string.
	 * @param openQuoteIndex Index of the opening {@code "} character.
	 * @return Parsed text and the index immediately after the closing {@code "}, or after the last character if
	 * 	the string is unterminated.
	 */
	private static QuotedString parseQuotedString(String value, int openQuoteIndex) {
		var sb = new StringBuilder();
		var pos = openQuoteIndex + 1;
		var len = value.length();
		while (pos < len) {
			var c = value.charAt(pos);
			if (c == '\\' && pos + 1 < len) {
				sb.append(value.charAt(pos + 1));
				pos += 2;
			} else if (c == '"') {
				return new QuotedString(sb.toString(), pos + 1);
			} else {
				sb.append(c);
				pos++;
			}
		}
		return new QuotedString(sb.toString(), pos);
	}

	private record QuotedString(String text, int nextPos) {}

	private static int skipWhitespace(String value, int pos) {
		while (pos < value.length() && (value.charAt(pos) == ' ' || value.charAt(pos) == '\t'))
			pos++;
		return pos;
	}
}
