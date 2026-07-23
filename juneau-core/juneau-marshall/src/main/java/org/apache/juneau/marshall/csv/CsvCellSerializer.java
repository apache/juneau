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
package org.apache.juneau.marshall.csv;

import static org.apache.juneau.commons.utils.Shorts.*;
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

	private final CsvByteArrayCellFormat byteArrayFormat;
	private final String nullMarker;

	/**
	 * Creates a new serializer.
	 *
	 * @param byteArrayFormat Format for byte[] values.
	 * 	<br>Can be <jk>null</jk> (defaults to {@link CsvByteArrayCellFormat#BASE64}).
	 * @param nullMarker String to use for null values.
	 * 	<br>Can be <jk>null</jk> (defaults to {@code null}).
	 */
	public CsvCellSerializer(CsvByteArrayCellFormat byteArrayFormat, String nullMarker) {
		this.byteArrayFormat = or(byteArrayFormat, CsvByteArrayCellFormat.BASE64);
		this.nullMarker = or(nullMarker, "null");
	}

	/**
	 * Serializes a value to inline notation.
	 *
	 * @param value The value to serialize.
	 * 	<br>Can be <jk>null</jk> (serialized as the null marker).
	 * @param session The serializer session (for bean conversion, date formatting, etc.).
	 * 	<br>Must not be <jk>null</jk>.
	 * @return The serialized string.
	 */
	public String write(Object value, CsvSerializerSession session) {
		return writeValue(value, session);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for exhaustive CSV value type dispatch
	})
	private String writeValue(Object value, CsvSerializerSession session) {
		if (value == null)
			return nullMarker;
		if (value instanceof Map<?,?> value2)
			return writeMap(value2, session);
		if (value instanceof Collection<?> value2)
			return writeCollection(value2, session);
		if (value instanceof Object[] value2)
			return writeObjectArray(value2, session);
		if (value instanceof byte[] value2)
			return byteArrayFormat == CsvByteArrayCellFormat.SEMICOLON_DELIMITED ? formatByteArraySemicolon(value2) : base64Encode(value2);
		if (value instanceof int[] value2) return formatIntArray(value2);
		if (value instanceof long[] value2) return formatLongArray(value2);
		if (value instanceof double[] value2) return formatDoubleArray(value2);
		if (value instanceof float[] value2) return formatFloatArray(value2);
		if (value instanceof short[] value2) return formatShortArray(value2);
		if (value instanceof boolean[] value2) return formatBooleanArray(value2);
		if (value instanceof char[] value2) return formatCharArray(value2);
		// Bean or simple: use session to convert/format
		var prepared = session.prepareForInlineValue(value);
		if (prepared instanceof Map) return writeMap((Map<?,?>) prepared, session);
		if (prepared instanceof Collection) return writeCollection((Collection<?>) prepared, session);
		if (prepared instanceof Object[] prepared2) return writeObjectArray(prepared2, session);
		return escapeIfNeeded(prepared.toString());
	}

	private String writeMap(Map<?,?> m, CsvSerializerSession session) {
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
			sb.append(writeValue(v, session));
		}
		sb.append('}');
		return sb.toString();
	}

	private String writeCollection(Collection<?> c, CsvSerializerSession session) {
		var sb = new StringBuilder();
		sb.append('[');
		var first = true;
		for (var v : c) {
			if (!first) sb.append(';');
			first = false;
			sb.append(writeValue(v, session));
		}
		sb.append(']');
		return sb.toString();
	}

	private String writeObjectArray(Object[] a, CsvSerializerSession session) {
		var sb = new StringBuilder();
		sb.append('[');
		for (var i = 0; i < a.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(writeValue(a[i], session));
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
