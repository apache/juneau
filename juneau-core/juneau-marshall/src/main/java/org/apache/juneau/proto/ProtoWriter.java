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
package org.apache.juneau.proto;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing Protobuf Text Format.
 *
 * <p>
 * Extends {@link SerializerWriter} with protobuf-specific methods for field names,
 * scalar values, message blocks, and list syntax.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity acceptable for protobuf formatting methods
	"resource" // Writer resource managed by calling code
})
public class ProtoWriter extends SerializerWriter {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, use whitespace for formatting.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, trim strings before serialization.
	 * @param uriResolver The URI resolver.
	 */
	protected ProtoWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '"', uriResolver);
	}

	/**
	 * Writes a field name (bare identifier or quoted key).
	 *
	 * @param name The field name.
	 * @return This object.
	 */
	public ProtoWriter fieldName(String name) {
		if (isBareIdentifier(name))
			w(name);
		else
			w('"').w(escapeString(name)).w('"');
		return this;
	}

	/**
	 * Writes the start of a scalar field: <c>name: </c>.
	 *
	 * <p>
	 * Callers append the value using {@link #stringValue}, {@link #integerValue}, etc., then add newline.
	 *
	 * @param name The field name.
	 * @return This object.
	 */
	public ProtoWriter scalarField(String name) {
		fieldName(name);
		w(": ");
		return this;
	}

	/**
	 * Writes the start of a message field: <c>name {</c> or <c>name: {</c>.
	 *
	 * @param name The field name.
	 * @param useColon If <jk>true</jk>, include colon before brace.
	 * @return This object.
	 */
	public ProtoWriter messageStart(String name, boolean useColon) {
		fieldName(name);
		if (useColon)
			w(": ");
		w(" {");
		nl(1);
		return this;
	}

	/**
	 * Writes the end of a message: <c>}</c> with optional newline.
	 *
	 * @param depth The current indentation depth for the closing brace.
	 * @return This object.
	 */
	public ProtoWriter messageEnd(int depth) {
		i(depth).w('}');
		nl(depth);
		return this;
	}

	/**
	 * Writes a string value with C-style escaping.
	 *
	 * @param value The string value.
	 * @return This object.
	 */
	public ProtoWriter stringValue(String value) {
		if (trimStrings)
			value = trim(value);
		w('"').w(escapeString(value == null ? "" : value)).w('"');
		return this;
	}

	/**
	 * Writes an integer value.
	 *
	 * @param value The integer value.
	 * @return This object.
	 */
	public ProtoWriter integerValue(long value) {
		w(Long.toString(value));
		return this;
	}

	/**
	 * Writes a float value, handling special values.
	 *
	 * @param value The float value.
	 * @return This object.
	 */
	public ProtoWriter floatValue(double value) {
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

	/**
	 * Writes a boolean value.
	 *
	 * @param value The boolean value.
	 * @return This object.
	 */
	public ProtoWriter booleanValue(boolean value) {
		w(value ? "true" : "false");
		return this;
	}

	/**
	 * Writes an enum value as an unquoted identifier.
	 *
	 * @param name The enum constant name.
	 * @return This object.
	 */
	public ProtoWriter enumValue(String name) {
		w(name == null ? "" : name);
		return this;
	}

	/**
	 * Writes a byte array as a quoted hex-escaped string.
	 *
	 * @param data The byte array.
	 * @return This object.
	 */
	public ProtoWriter bytesValue(byte[] data) {
		if (data == null)
			return this;
		w('"');
		for (byte b : data) {
			w("\\x");
			String hex = Integer.toHexString(b & 0xFF);
			if (hex.length() == 1)
				w('0');
			w(hex);
		}
		w('"');
		return this;
	}

	/**
	 * Writes the start of a list: <c>[</c>.
	 *
	 * @return This object.
	 */
	public ProtoWriter listStart() {
		w('[');
		return this;
	}

	/**
	 * Writes the end of a list: <c>]</c>.
	 *
	 * @return This object.
	 */
	public ProtoWriter listEnd() {
		w(']');
		return this;
	}

	/**
	 * Writes a comment: <c># text</c> followed by newline.
	 *
	 * @param text The comment text.
	 * @return This object.
	 */
	public ProtoWriter comment(String text) {
		if (text != null && !text.isEmpty())
			w("# ").w(text).w('\n');
		return this;
	}

	/**
	 * Returns whether the name is a valid bare identifier (no quotes needed).
	 *
	 * <p>
	 * Protobuf identifiers: letter or underscore followed by letters, digits, or underscores.
	 *
	 * @param name The name to check.
	 * @return <jk>true</jk> if it can be written as a bare identifier.
	 */
	public boolean isBareIdentifier(String name) {
		if (name == null || name.isEmpty())
			return false;
		char c = name.charAt(0);
		if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'))
			return false;
		for (int i = 1; i < name.length(); i++) {
			c = name.charAt(i);
			if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_'))
				return false;
		}
		return true;
	}

	/**
	 * Escapes a string using C-style escape sequences for protobuf text format.
	 *
	 * <p>
	 * Handles: <c>\"</c> <c>\\</c> <c>\n</c> <c>\t</c> <c>\r</c> <c>\b</c> <c>\f</c> <c>\'</c>
	 *
	 * @param text The text to escape.
	 * @return The escaped string.
	 */
	public String escapeString(String text) {
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
				case '\'' -> sb.append("\\'");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	// Override return types for chaining
	@Override public ProtoWriter append(char c) { super.append(c); return this; }
	@Override public ProtoWriter append(char[] value) { super.append(value); return this; }
	@Override public ProtoWriter append(int indent, char c) { super.append(indent, c); return this; }
	@Override public ProtoWriter append(int indent, String text) { super.append(indent, text); return this; }
	@Override public ProtoWriter append(Object text) { super.append(text); return this; }
	@Override public ProtoWriter append(String text) { super.append(text); return this; }
	@Override public ProtoWriter appendIf(boolean b, char c) { super.appendIf(b, c); return this; }
	@Override public ProtoWriter appendIf(boolean b, String text) { super.appendIf(b, text); return this; }
	@Override public ProtoWriter appendln(int indent, String text) { super.appendln(indent, text); return this; }
	@Override public ProtoWriter appendln(String text) { super.appendln(text); return this; }
	@Override public ProtoWriter appendUri(Object value) { super.appendUri(value); return this; }
	@Override public ProtoWriter cr(int depth) { super.cr(depth); return this; }
	@Override public ProtoWriter cre(int depth) { super.cre(depth); return this; }
	@Override public ProtoWriter i(int indent) { super.i(indent); return this; }
	@Override public ProtoWriter ie(int indent) { super.ie(indent); return this; }
	@Override public ProtoWriter nl(int indent) { super.nl(indent); return this; }
	@Override public ProtoWriter nlIf(boolean flag, int indent) { super.nlIf(flag, indent); return this; }
	@Override public ProtoWriter q() { super.q(); return this; }
	@Override public ProtoWriter s() { super.s(); return this; }
	@Override public ProtoWriter sIf(boolean flag) { super.sIf(flag); return this; }
	@Override public ProtoWriter w(char value) { super.w(value); return this; }
	@Override public ProtoWriter w(String value) { super.w(value); return this; }
}
