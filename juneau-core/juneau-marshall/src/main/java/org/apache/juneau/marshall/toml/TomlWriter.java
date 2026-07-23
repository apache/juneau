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
package org.apache.juneau.marshall.toml;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

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
public class TomlWriter extends SerializerWriter<TomlWriter> {

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
				w(OffsetDateTime.ofInstant(i, ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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
		if (value instanceof LocalDateTime value2)
			w(value2.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		else
			w(value.toString());
		return this;
	}

	TomlWriter localDateValue(Object value) {
		if (value instanceof LocalDate value2)
			w(value2.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
		else
			w(value.toString());
		return this;
	}

	TomlWriter localTimeValue(Object value) {
		if (value instanceof LocalTime value2)
			w(value2.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME));
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
		if (isEmpty(key))
			return false;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-'))
				return false;
		}
		return true;
	}
}
