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
package org.apache.juneau.toml;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing TOML v1.0.0.
 *
 * <p>
 * Extends {@link SerializerWriter} with TOML-specific methods for tables, key-value pairs,
 * and typed values.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a href="https://toml.io/en/v1.0.0">TOML v1.0.0 Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Writer resource managed by calling code
})
public class TomlWriter extends SerializerWriter {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, use whitespace for formatting.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, trim strings before serialization.
	 * @param uriResolver The URI resolver.
	 */
	protected TomlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '"', uriResolver);
	}

	TomlWriter tableHeader(String path) {
		w('[').w(path).w(']').w('\n');
		return this;
	}

	TomlWriter arrayOfTablesHeader(String path) {
		w("[[").w(path).w("]]").w('\n');
		return this;
	}

	TomlWriter keyValue(String key, String value) {
		writeKey(key);
		w(" = ");
		stringValue(value);
		w('\n');
		return this;
	}

	TomlWriter bareKey(String key) {
		w(key);
		return this;
	}

	TomlWriter quotedKey(String key) {
		w('"').w(escapeBasicString(key)).w('"');
		return this;
	}

	private void writeKey(String key) {
		if (isBareKey(key))
			bareKey(key);
		else
			quotedKey(key);
	}

	TomlWriter stringValue(String value) {
		if (trimStrings)
			value = trim(value);
		w('"').w(escapeBasicString(value == null ? "" : value)).w('"');
		return this;
	}

	TomlWriter literalString(String value) {
		if (trimStrings)
			value = trim(value);
		w('\'').w(value == null ? "" : value.replace("'", "''")).w('\'');
		return this;
	}

	TomlWriter multiLineString(String value) {
		if (trimStrings)
			value = trim(value);
		w("\"\"\"").w('\n').w(value == null ? "" : value).w("\"\"\"");
		return this;
	}

	TomlWriter integerValue(long value) {
		w(Long.toString(value));
		return this;
	}

	TomlWriter floatValue(double value) {
		if (Double.isNaN(value))
			w("nan");
		else if (value == Double.POSITIVE_INFINITY)
			w("inf");
		else if (value == Double.NEGATIVE_INFINITY)
			w("-inf");
		else {
			String s = Double.toString(value);
			if (s.endsWith(".0"))
				s = s.substring(0, s.length() - 2);
			w(s);
		}
		return this;
	}

	TomlWriter booleanValue(boolean value) {
		w(value ? "true" : "false");
		return this;
	}

	TomlWriter dateTimeValue(Object value) {
		if (value instanceof TemporalAccessor ta) {
			if (ta instanceof Instant i)
				w(java.time.OffsetDateTime.ofInstant(i, ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			else if (ta instanceof OffsetDateTime odt)
				w(odt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			else if (ta instanceof ZonedDateTime zdt)
				w(zdt.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			else if (ta instanceof LocalDateTime ldt)
				w(ldt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			else if (ta instanceof LocalDate ld)
				w(ld.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
			else if (ta instanceof LocalTime lt)
				w(lt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME));
			else
				w(value.toString());
		} else {
			w(value.toString());
		}
		return this;
	}

	TomlWriter localDateTimeValue(Object value) {
		if (value instanceof LocalDateTime ldt)
			w(ldt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		else
			w(value.toString());
		return this;
	}

	TomlWriter localDateValue(Object value) {
		if (value instanceof LocalDate ld)
			w(ld.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
		else
			w(value.toString());
		return this;
	}

	TomlWriter localTimeValue(Object value) {
		if (value instanceof LocalTime lt)
			w(lt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME));
		else
			w(value.toString());
		return this;
	}

	TomlWriter arrayStart() {
		w('[');
		return this;
	}

	TomlWriter arrayEnd() {
		w(']');
		return this;
	}

	TomlWriter inlineTableStart() {
		w('{');
		return this;
	}

	TomlWriter inlineTableEnd() {
		w('}');
		return this;
	}

	TomlWriter comment(String text) {
		w("# ").w(text).w('\n');
		return this;
	}

	TomlWriter blankLine() {
		w('\n');
		return this;
	}

	String escapeBasicString(String text) {
		if (text == null)
			return "";
		var sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '"' -> sb.append("\\\"");
				case '\b' -> sb.append("\\b");
				case '\t' -> sb.append("\\t");
				case '\n' -> sb.append("\\n");
				case '\f' -> sb.append("\\f");
				case '\r' -> sb.append("\\r");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	boolean isBareKey(String key) {
		if (key == null || key.isEmpty())
			return false;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-'))
				return false;
		}
		return true;
	}

	// Override return types for chaining
	@Override public TomlWriter append(char c) { super.append(c); return this; }
	@Override public TomlWriter append(char[] value) { super.append(value); return this; }
	@Override public TomlWriter append(int indent, char c) { super.append(indent, c); return this; }
	@Override public TomlWriter append(int indent, String text) { super.append(indent, text); return this; }
	@Override public TomlWriter append(Object text) { super.append(text); return this; }
	@Override public TomlWriter append(String text) { super.append(text); return this; }
	@Override public TomlWriter appendIf(boolean b, char c) { super.appendIf(b, c); return this; }
	@Override public TomlWriter appendIf(boolean b, String text) { super.appendIf(b, text); return this; }
	@Override public TomlWriter appendln(int indent, String text) { super.appendln(indent, text); return this; }
	@Override public TomlWriter appendln(String text) { super.appendln(text); return this; }
	@Override public TomlWriter appendUri(Object value) { super.appendUri(value); return this; }
	@Override public TomlWriter cr(int depth) { super.cr(depth); return this; }
	@Override public TomlWriter cre(int depth) { super.cre(depth); return this; }
	@Override public TomlWriter i(int indent) { super.i(indent); return this; }
	@Override public TomlWriter ie(int indent) { super.ie(indent); return this; }
	@Override public TomlWriter nl(int indent) { super.nl(indent); return this; }
	@Override public TomlWriter nlIf(boolean flag, int indent) { super.nlIf(flag, indent); return this; }
	@Override public TomlWriter q() { super.q(); return this; }
	@Override public TomlWriter s() { super.s(); return this; }
	@Override public TomlWriter sIf(boolean flag) { super.sIf(flag); return this; }
	@Override public TomlWriter w(char value) { super.w(value); return this; }
	@Override public TomlWriter w(String value) { super.w(value); return this; }
}
