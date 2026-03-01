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

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

/**
 * Serializes nested objects and arrays to CSV cell inline notation:
 * {@code {key:val;key2:val2}} and {@code [val1;val2;val3]}.
 *
 * <p>
 * Used when {@code allowNestedStructures()} is enabled on the serializer.
 */
public final class CsvCellSerializer {

	private final ByteArrayFormat byteArrayFormat;
	private final String nullMarker;

	/**
	 * Creates a new serializer.
	 *
	 * @param byteArrayFormat Format for byte[] values.
	 * @param nullMarker String to use for null values.
	 */
	public CsvCellSerializer(ByteArrayFormat byteArrayFormat, String nullMarker) {
		this.byteArrayFormat = byteArrayFormat != null ? byteArrayFormat : ByteArrayFormat.BASE64;
		this.nullMarker = nullMarker != null ? nullMarker : "null";
	}

	/**
	 * Serializes a value to inline notation.
	 *
	 * @param value The value to serialize.
	 * @param session The serializer session (for bean conversion, date formatting, etc.).
	 * @return The serialized string.
	 */
	public String serialize(Object value, CsvSerializerSession session) {
		return serializeValue(value, session);
	}

	private String serializeValue(Object value, CsvSerializerSession session) {
		if (value == null)
			return nullMarker;
		if (value instanceof Map<?, ?> m)
			return serializeMap(m, session);
		if (value instanceof Collection<?> c)
			return serializeCollection(c, session);
		if (value instanceof Object[] a)
			return serializeObjectArray(a, session);
		if (value instanceof byte[] b)
			return byteArrayFormat == ByteArrayFormat.SEMICOLON_DELIMITED ? formatByteArraySemicolon(b) : base64Encode(b);
		if (value instanceof int[] a) return formatIntArray(a);
		if (value instanceof long[] a) return formatLongArray(a);
		if (value instanceof double[] a) return formatDoubleArray(a);
		if (value instanceof float[] a) return formatFloatArray(a);
		if (value instanceof short[] a) return formatShortArray(a);
		if (value instanceof boolean[] a) return formatBooleanArray(a);
		if (value instanceof char[] a) return formatCharArray(a);
		// Bean or simple: use session to convert/format
		var prepared = session.prepareForInlineValue(value);
		if (prepared instanceof Map) return serializeMap((Map<?, ?>) prepared, session);
		if (prepared instanceof Collection) return serializeCollection((Collection<?>) prepared, session);
		if (prepared instanceof Object[]) return serializeObjectArray((Object[]) prepared, session);
		return escapeIfNeeded(prepared.toString());
	}

	private String serializeMap(Map<?, ?> m, CsvSerializerSession session) {
		var sb = new StringBuilder();
		sb.append('{');
		var first = true;
		for (var e : m.entrySet()) {
			if (!first) sb.append(';');
			first = false;
			var k = e.getKey();
			var v = e.getValue();
			sb.append(escapeIfNeeded(k != null ? k.toString() : nullMarker));
			sb.append(':');
			sb.append(serializeValue(v, session));
		}
		sb.append('}');
		return sb.toString();
	}

	private String serializeCollection(Collection<?> c, CsvSerializerSession session) {
		var sb = new StringBuilder();
		sb.append('[');
		var first = true;
		for (var v : c) {
			if (!first) sb.append(';');
			first = false;
			sb.append(serializeValue(v, session));
		}
		sb.append(']');
		return sb.toString();
	}

	private String serializeObjectArray(Object[] a, CsvSerializerSession session) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(serializeValue(a[i], session));
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatIntArray(int[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatLongArray(long[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatDoubleArray(double[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatFloatArray(float[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatShortArray(short[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatBooleanArray(boolean[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatCharArray(char[] a) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append((int) a[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatByteArraySemicolon(byte[] b) {
		var sb = new StringBuilder();
		for (var i = 0; i < b.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(b[i] & 0xff);
		}
		return sb.toString();
	}

	private static String escapeIfNeeded(String s) {
		if (s == null)
			return "";
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == ';' || c == ':' || c == '{' || c == '}' || c == '[' || c == ']' || c == '"' || c == '\\')
				return quoted(s);
		}
		return s;
	}

	private static String quoted(String s) {
		var sb = new StringBuilder();
		sb.append('"');
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '"' || c == '\\')
				sb.append('\\');
			sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}
}
