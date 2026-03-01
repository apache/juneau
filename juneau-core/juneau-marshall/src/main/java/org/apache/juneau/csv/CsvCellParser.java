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
package org.apache.juneau.csv;

import java.util.*;

import org.apache.juneau.parser.*;

/**
 * Parses CSV cell inline notation: {@code {key:val;key2:val2}} and {@code [val1;val2;val3]}.
 *
 * <p>
 * Grammar (CSV-specific, semicolon-separated):
 * <ul>
 *   <li>Object: {@code \{ (key ':' value (';' key ':' value)*)? \}}
 *   <li>Array: {@code \[ (value (';' value)*)? \]}
 *   <li>Values: object | array | quoted string | number | identifier | null
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Reserved characters: {@code ; : \{ \} [ ] " \}
 * </ul>
 */
public final class CsvCellParser {

	private final String input;
	private final String nullMarker;
	private int pos;
	private int length;

	private CsvCellParser(String input, String nullMarker) {
		this.input = input != null ? input.trim() : "";
		this.nullMarker = nullMarker != null ? nullMarker : "null";
		this.length = this.input.length();
	}

	/**
	 * Parses a cell value string into a Map, List, or scalar.
	 *
	 * @param cell The cell string (e.g. {@code {a:1;b:2}} or {@code [1;2;3]}).
	 * @param nullMarker The string that denotes null (e.g. {@code <NULL>} or {@code null}).
	 * @return Parsed object: Map, List, String, Number, Boolean, or null.
	 * @throws ParseException If the input is invalid.
	 */
	public static Object parse(String cell, String nullMarker) throws ParseException {
		if (cell == null || cell.isEmpty())
			return null;
		var p = new CsvCellParser(cell, nullMarker);
		try {
			return p.parseValue();
		} catch (Exception e) {
			throw new ParseException(e, "Invalid CSV cell notation at position {0}: ''{1}''", p.pos, cell);
		}
	}

	private void skipWhitespace() {
		while (pos < length && Character.isWhitespace(input.charAt(pos)))
			pos++;
	}

	private boolean hasMore() {
		skipWhitespace();
		return pos < length;
	}

	private char peek() {
		return pos < length ? input.charAt(pos) : '\0';
	}

	private char consume() {
		return pos < length ? input.charAt(pos++) : '\0';
	}

	private Object parseValue() throws ParseException {
		skipWhitespace();
		if (pos >= length)
			return null;
		var c = peek();
		if (c == '{')
			return parseObject();
		if (c == '[')
			return parseArray();
		if (c == '"')
			return parseQuotedString();
		return parseSimpleValue();
	}

	private Map<String, Object> parseObject() throws ParseException {
		if (consume() != '{')
			throw new ParseException("Expected opening brace");
		var m = new LinkedHashMap<String, Object>();
		skipWhitespace();
		if (peek() == '}') {
			consume();
			return m;
		}
		while (hasMore()) {
			var key = parseKey();
			skipWhitespace();
			if (consume() != ':')
				throw new ParseException("Expected ':' after key at position " + pos);
			var value = parseValue();
			m.put(key, value);
			skipWhitespace();
			var c = peek();
			if (c == '}') {
				consume();
				break;
			}
			if (c != ';')
				throw new ParseException("Expected ';' or '}' at position " + pos);
			consume(); // skip ';'
		}
		return m;
	}

	private String parseKey() throws ParseException {
		skipWhitespace();
		if (pos >= length)
			throw new ParseException("Unexpected end of input in key");
		if (peek() == '"')
			return parseQuotedString();
		return parseIdentifier();
	}

	private List<Object> parseArray() throws ParseException {
		if (consume() != '[')
			throw new ParseException("Expected opening bracket");
		var list = new ArrayList<>();
		skipWhitespace();
		if (peek() == ']') {
			consume();
			return list;
		}
		while (hasMore()) {
			list.add(parseValue());
			skipWhitespace();
			var c = peek();
			if (c == ']') {
				consume();
				break;
			}
			if (c != ';')
				throw new ParseException("Expected ';' or ']' at position " + pos);
			consume(); // skip ';'
		}
		return list;
	}

	private String parseQuotedString() throws ParseException {
		if (consume() != '"')
			throw new ParseException("Expected quote");
		var sb = new StringBuilder();
		while (pos < length) {
			var c = consume();
			if (c == '"')
				return sb.toString();
			if (c == '\\' && pos < length)
				sb.append(consume()); // escaped char
			else
				sb.append(c);
		}
		throw new ParseException("Unterminated quoted string");
	}

	private String parseIdentifier() {
		var start = pos;
		while (pos < length) {
			var c = input.charAt(pos);
			if (c == ';' || c == ':' || c == '{' || c == '}' || c == '[' || c == ']' || c == '"' || Character.isWhitespace(c))
				break;
			pos++;
		}
		return input.substring(start, pos);
	}

	private Object parseSimpleValue() throws ParseException {
		skipWhitespace();
		if (pos >= length || peek() == ';' || peek() == '}' || peek() == ']')
			return "";
		var id = parseIdentifier();
		if (id.isEmpty())
			throw new ParseException("Expected value at position " + pos);
		if (id.equalsIgnoreCase(nullMarker))
			return null;
		if (id.equals("true"))
			return Boolean.TRUE;
		if (id.equals("false"))
			return Boolean.FALSE;
		// Try number
		try {
			if (id.contains(".")) {
				var d = Double.parseDouble(id);
				if (d == (long) d)
					return Long.valueOf((long) d);
				return Double.valueOf(d);
			}
			return Long.valueOf(id);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			// Not a number, treat as string
			return id;
		}
	}
}
