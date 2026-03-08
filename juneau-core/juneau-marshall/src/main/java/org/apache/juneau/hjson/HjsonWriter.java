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
package org.apache.juneau.hjson;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing Hjson format.
 *
 * <p>
 * Extends {@link SerializerWriter} with Hjson-specific methods for quoteless strings,
 * multiline strings, and formatting options.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S135",  // Multiple break/continue necessary for isNumber char validation loop
	"resource"    // Writer resource managed by calling code
})
public class HjsonWriter extends SerializerWriter {

	private static final AsciiSet QUOTE_KEY_CHARS = AsciiSet.of(" \t\n\r{},:[]\"'#/");
	private static final AsciiSet QUOTE_VALUE_CHARS = AsciiSet.of("\n\r{},:\"'#");
	private static final AsciiSet ENCODED_CHARS = AsciiSet.of("\n\t\b\f\r\"\\");

	/** Use newlines instead of commas between members. */
	protected final boolean useNewlineSeparators;

	/** Use quoteless strings for simple values. */
	protected final boolean useQuotelessStrings;

	/** Use quoteless keys when safe. */
	protected final boolean useQuotelessKeys;

	/** Omit root braces. */
	protected final boolean omitRootBraces;

	/** Use multiline ''' for strings with newlines. */
	protected final boolean useMultilineStrings;

	/** Whether to add space after colon. */
	protected final boolean spacedKeyValueSeparator;

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, use indentation and newlines.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, trim strings before serialization.
	 * @param useNewlineSeparators Use newlines instead of commas.
	 * @param useQuotelessStrings Use quoteless strings for simple values.
	 * @param useQuotelessKeys Use quoteless keys when safe.
	 * @param omitRootBraces Omit root object braces.
	 * @param useMultilineStrings Use ''' for multiline strings.
	 * @param spacedKeyValueSeparator Add space after colon.
	 * @param uriResolver The URI resolver.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor requires many parameters for Hjson writer configuration
	})
	protected HjsonWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings,
			boolean useNewlineSeparators, boolean useQuotelessStrings, boolean useQuotelessKeys,
			boolean omitRootBraces, boolean useMultilineStrings, boolean spacedKeyValueSeparator,
			UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '"', uriResolver);
		this.useNewlineSeparators = useNewlineSeparators;
		this.useQuotelessStrings = useQuotelessStrings;
		this.useQuotelessKeys = useQuotelessKeys;
		this.omitRootBraces = omitRootBraces;
		this.useMultilineStrings = useMultilineStrings;
		this.spacedKeyValueSeparator = spacedKeyValueSeparator;
	}

	/**
	 * Writes member separator (newline or comma).
	 *
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HjsonWriter memberSeparator(int depth) {
		if (useNewlineSeparators) {
			nl(depth);
		} else {
			w(',');
		}
		return this;
	}

	/**
	 * Writes key-value separator (: or : ).
	 *
	 * @return This object.
	 */
	public HjsonWriter keyValueSeparator() {
		if (spacedKeyValueSeparator)
			w(": ");
		else
			w(':');
		return this;
	}

	/**
	 * Writes object start, optionally omitting braces.
	 *
	 * @param omitBraces If <jk>true</jk>, omit the opening brace.
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HjsonWriter objectStart(boolean omitBraces, int depth) {
		if (!omitBraces)
			w('{');
		return this;
	}

	/**
	 * Writes object end, optionally omitting braces.
	 *
	 * @param omitBraces If <jk>true</jk>, omit the closing brace.
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HjsonWriter objectEnd(boolean omitBraces, int depth) {
		if (!omitBraces)
			w('}');
		return this;
	}

	/**
	 * Writes array start.
	 *
	 * @return This object.
	 */
	public HjsonWriter arrayStart() {
		w('[');
		return this;
	}

	/**
	 * Writes array end.
	 *
	 * @return This object.
	 */
	public HjsonWriter arrayEnd() {
		w(']');
		return this;
	}

	/**
	 * Returns whether the key needs quoting.
	 *
	 * @param key The key to check.
	 * @return <jk>true</jk> if the key must be quoted.
	 */
	public boolean needsQuoting(String key) {
		if (key == null || key.isEmpty())
			return true;
		if (!useQuotelessKeys)
			return true;
		for (var i = 0; i < key.length(); i++) {
			if (QUOTE_KEY_CHARS.contains(key.charAt(i)))
				return true;
		}
		return "true".equals(key) || "false".equals(key) || "null".equals(key) || isNumber(key);
	}

	/**
	 * Returns whether the value can be written as quoteless.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the value can be quoteless.
	 */
	public boolean isSimpleValue(String value) {
		if (value == null || value.isEmpty())
			return false;
		if (!useQuotelessStrings)
			return false;
		if (value.startsWith("{") || value.startsWith("[") || value.startsWith("\"") || value.startsWith("'"))
			return false;
		if (value.startsWith("#") || value.startsWith("//") || value.startsWith("/*"))
			return false;
		if (value.contains("\n") || value.contains("\r"))
			return false;
		for (var i = 0; i < value.length(); i++) {
			if (QUOTE_VALUE_CHARS.contains(value.charAt(i)))
				return false;
		}
		if ("true".equals(value) || "false".equals(value) || "null".equals(value))
			return false;
		return !isNumber(value);
	}

	private static boolean isNumber(String s) {
		if (s == null || s.isEmpty())
			return false;
		var i = s.startsWith("-") ? 1 : 0;
		if (i >= s.length())
			return false;
		if (s.charAt(i) == '0' && s.length() > i + 1 && s.charAt(i + 1) != '.')
			return false;
		for (; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c >= '0' && c <= '9')
				continue;
			if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-')
				continue;
			return false;
		}
		try {
			Double.parseDouble(s);
			return true;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Writes a quoteless string value.
	 *
	 * @param value The value to write.
	 * @return This object.
	 */
	public HjsonWriter quotelessString(String value) {
		if (value != null)
			w(value);
		return this;
	}

	/**
	 * Writes a quoted string with JSON escaping.
	 *
	 * @param value The value to write.
	 * @return This object.
	 */
	public HjsonWriter quotedString(String value) {
		if (value == null) {
			w("null");
			return this;
		}
		w('"');
		for (var i = 0; i < value.length(); i++) {
			var c = value.charAt(i);
			if (ENCODED_CHARS.contains(c)) {
				w('\\');
				w(switch (c) {
					case '\n' -> 'n';
					case '\t' -> 't';
					case '\b' -> 'b';
					case '\f' -> 'f';
					case '\r' -> 'r';
					case '"' -> '"';
					case '\\' -> '\\';
					default -> c;
				});
			} else {
				w(c);
			}
		}
		w('"');
		return this;
	}

	/**
	 * Writes a multiline string using ''' syntax.
	 *
	 * @param value The value to write.
	 * @return This object.
	 */
	public HjsonWriter multilineString(String value) {
		if (value == null) {
			w("null");
			return this;
		}
		w("'''");
		w(value.replace("\r\n", "\n").replace("\r", "\n"));
		w("'''");
		return this;
	}

	// Override return types for chaining
	@Override public HjsonWriter append(char c) { super.append(c); return this; }
	@Override public HjsonWriter append(char[] value) { super.append(value); return this; }
	@Override public HjsonWriter append(int indent, char c) { super.append(indent, c); return this; }
	@Override public HjsonWriter append(int indent, String text) { super.append(indent, text); return this; }
	@Override public HjsonWriter append(Object text) { super.append(text); return this; }
	@Override public HjsonWriter append(String text) { super.append(text); return this; }
	@Override public HjsonWriter appendIf(boolean b, char c) { super.appendIf(b, c); return this; }
	@Override public HjsonWriter appendIf(boolean b, String text) { super.appendIf(b, text); return this; }
	@Override public HjsonWriter appendln(int indent, String text) { super.appendln(indent, text); return this; }
	@Override public HjsonWriter appendln(String text) { super.appendln(text); return this; }
	@Override public HjsonWriter appendUri(Object value) { super.appendUri(value); return this; }
	@Override public HjsonWriter cr(int depth) { super.cr(depth); return this; }
	@Override public HjsonWriter cre(int depth) { super.cre(depth); return this; }
	@Override public HjsonWriter i(int indent) { super.i(indent); return this; }
	@Override public HjsonWriter ie(int indent) { super.ie(indent); return this; }
	@Override public HjsonWriter nl(int indent) { super.nl(indent); return this; }
	@Override public HjsonWriter nlIf(boolean flag, int indent) { super.nlIf(flag, indent); return this; }
	@Override public HjsonWriter q() { super.q(); return this; }
	@Override public HjsonWriter s() { super.s(); return this; }
	@Override public HjsonWriter sIf(boolean flag) { super.sIf(flag); return this; }
	@Override public HjsonWriter w(char value) { super.w(value); return this; }
	@Override public HjsonWriter w(String value) { super.w(value); return this; }
}
