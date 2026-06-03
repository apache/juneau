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
package org.apache.juneau.commons.svl.functions;

import java.util.*;

/**
 * Tiny inline JSON parser used by {@link JsonFunctions}.
 *
 * <p>
 * Handles object, array, string (with {@code \\}-escapes including {@code \\uXXXX}), number,
 * boolean, and {@code null}. Insertion-ordered for objects (uses {@link LinkedHashMap}).
 * Returns Java {@link Map} / {@link List} / {@link String} / {@link Number} / {@link Boolean} /
 * {@code null}.
 *
 * <p>
 * This is intentionally a small inline implementation — depending on the full Juneau JSON parser
 * here would create a circular module dependency
 * (juneau-commons → juneau-marshall).
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity: small recursive-descent parser.
	"java:S6541", // Brain method: state machine for JSON tokenization.
})
final class MiniJson {

	private final String src;
	private int pos;

	private MiniJson(String src) { this.src = src; }

	/** Parse a JSON value. Returns {@code null} for empty input. */
	static Object parse(String s) {
		if (s == null) return null;
		var p = new MiniJson(s);
		p.skipWs();
		if (p.pos >= p.src.length()) return null;
		var result = p.parseValue();
		p.skipWs();
		if (p.pos < p.src.length())
			throw new IllegalArgumentException("Trailing characters at offset " + p.pos);
		return result;
	}

	private Object parseValue() {
		skipWs();
		if (pos >= src.length()) throw err("Unexpected end of input");
		var c = src.charAt(pos);
		if (c == '{') return parseObject();
		if (c == '[') return parseArray();
		if (c == '"' || c == '\'') return parseString();
		if (c == 't' || c == 'f') return parseBool();
		if (c == 'n') return parseNull();
		if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
		throw err("Unexpected character '" + c + "'");
	}

	private LinkedHashMap<String, Object> parseObject() {
		pos++; // consume '{'
		var out = new LinkedHashMap<String, Object>();
		skipWs();
		if (peek() == '}') { pos++; return out; }
		while (true) {
			skipWs();
			var key = parseString();
			skipWs();
			expect(':');
			skipWs();
			var value = parseValue();
			out.put(key, value);
			skipWs();
			var c = peek();
			if (c == ',') { pos++; continue; }
			if (c == '}') { pos++; return out; }
			throw err("Expected ',' or '}'");
		}
	}

	private List<Object> parseArray() {
		pos++; // consume '['
		var out = new ArrayList<Object>();
		skipWs();
		if (peek() == ']') { pos++; return out; }
		while (true) {
			skipWs();
			out.add(parseValue());
			skipWs();
			var c = peek();
			if (c == ',') { pos++; continue; }
			if (c == ']') { pos++; return out; }
			throw err("Expected ',' or ']'");
		}
	}

	private String parseString() {
		var quote = src.charAt(pos);
		if (quote != '"' && quote != '\'') throw err("Expected string");
		pos++;
		var sb = new StringBuilder();
		while (pos < src.length()) {
			var c = src.charAt(pos);
			if (c == '\\' && pos + 1 < src.length()) {
				var next = src.charAt(pos + 1);
				switch (next) {
					case 'n': sb.append('\n'); pos += 2; continue;
					case 't': sb.append('\t'); pos += 2; continue;
					case 'r': sb.append('\r'); pos += 2; continue;
					case 'b': sb.append('\b'); pos += 2; continue;
					case 'f': sb.append('\f'); pos += 2; continue;
					case '/': sb.append('/'); pos += 2; continue;
					case 'u':
						if (pos + 5 >= src.length()) throw err("Truncated \\u escape");
						var hex = src.substring(pos + 2, pos + 6);
						sb.append((char) Integer.parseInt(hex, 16));
						pos += 6;
						continue;
					default:
						sb.append(next);
						pos += 2;
						continue;
				}
			}
			if (c == quote) { pos++; return sb.toString(); }
			sb.append(c);
			pos++;
		}
		throw err("Unterminated string");
	}

	private Boolean parseBool() {
		if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
		if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
		throw err("Expected true/false");
	}

	private Object parseNull() {
		if (src.startsWith("null", pos)) { pos += 4; return null; }
		throw err("Expected null");
	}

	private Number parseNumber() {
		var start = pos;
		if (src.charAt(pos) == '-') pos++;
		while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
		var isFloating = false;
		if (pos < src.length() && src.charAt(pos) == '.') {
			isFloating = true;
			pos++;
			while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
		}
		if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
			isFloating = true;
			pos++;
			if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
			while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
		}
		var text = src.substring(start, pos);
		if (isFloating) return Double.parseDouble(text);
		try { return Long.parseLong(text); } catch (@SuppressWarnings("unused") NumberFormatException e) { return Double.parseDouble(text); }
	}

	private char peek() {
		return pos < src.length() ? src.charAt(pos) : '\0';
	}

	private void expect(char c) {
		if (pos >= src.length() || src.charAt(pos) != c) throw err("Expected '" + c + "'");
		pos++;
	}

	private void skipWs() {
		while (pos < src.length()) {
			var c = src.charAt(pos);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
			else break;
		}
	}

	private IllegalArgumentException err(String msg) {
		return new IllegalArgumentException(msg + " at offset " + pos);
	}

	/**
	 * Render a parsed JSON value back as a JSON-compatible string for SVL interop.
	 *
	 * <p>
	 * Strings are rendered without surrounding quotes (so {@code get(...)} on an object property
	 * returns the bare string value); container types (Map / List) are re-encoded.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	static String render(Object value) {
		if (value == null) return "";
		if (value instanceof String s) return s;
		if (value instanceof Map<?, ?> m) return renderMap((Map<String, Object>) m);
		if (value instanceof List<?> l) return renderList((List<Object>) l);
		return String.valueOf(value);
	}

	private static String renderMap(Map<String, Object> m) {
		var sb = new StringBuilder();
		sb.append('{');
		var first = true;
		for (var e : m.entrySet()) {
			if (!first) sb.append(',');
			appendString(sb, e.getKey());
			sb.append(':');
			renderInto(sb, e.getValue());
			first = false;
		}
		sb.append('}');
		return sb.toString();
	}

	private static String renderList(List<Object> l) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < l.size(); i++) {
			if (i > 0) sb.append(',');
			renderInto(sb, l.get(i));
		}
		sb.append(']');
		return sb.toString();
	}

	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	private static void renderInto(StringBuilder sb, Object v) {
		if (v == null) { sb.append("null"); return; }
		if (v instanceof String s) { appendString(sb, s); return; }
		if (v instanceof Boolean || v instanceof Number) { sb.append(v); return; }
		if (v instanceof Map<?, ?> m) { sb.append(renderMap((Map<String, Object>) m)); return; }
		if (v instanceof List<?> l) { sb.append(renderList((List<Object>) l)); return; }
		appendString(sb, String.valueOf(v));
	}

	private static void appendString(StringBuilder sb, String s) {
		sb.append('"');
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '"' || c == '\\') sb.append('\\');
			sb.append(c);
		}
		sb.append('"');
	}
}
