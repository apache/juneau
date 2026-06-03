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
 * Tiny inline JSON-array-shortcut encoder used by string functions that return a {@code String[]}-
 * compatible result (e.g. {@link StringFunctions.Split}). Mirrors the parsing rules in
 * {@code ArgCoercer.parseStringArray(...)}.
 *
 * <p>
 * This is intentionally a small inline implementation — depending on the full Juneau JSON parser
 * here would create a circular module dependency
 * (juneau-commons → juneau-marshall).
 */
final class JsonShortcut {

	private JsonShortcut() {}

	/** Encode a list of strings as a JSON-array-shortcut literal: {@code ["a","b","c"]}. */
	static String encodeArray(List<String> parts) {
		if (parts.isEmpty()) return "[]";
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < parts.size(); i++) {
			if (i > 0) sb.append(',');
			appendQuoted(sb, parts.get(i));
		}
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Decode a JSON-array-shortcut string into a {@code String[]}. Returns a single-element
	 * array of the raw string when the input does not start with {@code [} / end with
	 * {@code ]}, mirroring {@code ArgCoercer.parseStringArray}.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity: small inline parser.
	})
	static String[] decodeArray(String s) {
		if (s == null) return new String[0];
		var t = s.trim();
		if (t.isEmpty()) return new String[0];
		if (!(t.startsWith("[") && t.endsWith("]")))
			return new String[]{s};
		var body = t.substring(1, t.length() - 1).trim();
		if (body.isEmpty()) return new String[0];
		var out = new ArrayList<String>();
		var i = 0;
		var len = body.length();
		while (i < len) {
			while (i < len && Character.isWhitespace(body.charAt(i))) i++;
			if (i >= len) break;
			var c = body.charAt(i);
			if (c == '"' || c == '\'') {
				var quote = c;
				i++;
				var sb = new StringBuilder();
				while (i < len) {
					var c2 = body.charAt(i);
					if (c2 == '\\' && i + 1 < len) {
						sb.append(body.charAt(i + 1));
						i += 2;
						continue;
					}
					if (c2 == quote) { i++; break; }
					sb.append(c2);
					i++;
				}
				out.add(sb.toString());
			} else {
				var start = i;
				while (i < len && body.charAt(i) != ',') i++;
				out.add(body.substring(start, i).trim());
			}
			while (i < len && Character.isWhitespace(body.charAt(i))) i++;
			if (i < len) {
				if (body.charAt(i) == ',') i++;
			}
		}
		return out.toArray(new String[0]);
	}

	private static void appendQuoted(StringBuilder sb, String s) {
		sb.append('"');
		if (s != null) {
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				if (c == '"' || c == '\\') sb.append('\\');
				sb.append(c);
			}
		}
		sb.append('"');
	}
}
