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
package org.apache.juneau.hocon;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing HOCON format.
 *
 * <p>
 * Extends {@link SerializerWriter} with HOCON-specific methods for unquoted strings,
 * triple-quoted strings ("""), equals sign separator, and formatting options.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S135",  // Multiple break/continue necessary for isNumber char validation loop
	"resource"    // Writer resource managed by calling code
})
public class HoconWriter extends SerializerWriter {

	private static final AsciiSet QUOTE_KEY_CHARS = AsciiSet.of(" \t\n\r{},:[]=\"'#/");
	private static final AsciiSet QUOTE_VALUE_CHARS = AsciiSet.of(" \t\n\r{},:\"'#");
	private static final AsciiSet ENCODED_CHARS = AsciiSet.of("\n\t\b\f\r\"\\");

	/** Use newlines instead of commas between members. */
	protected final boolean useNewlineSeparators;

	/** Use unquoted strings for simple values. */
	protected final boolean useUnquotedStrings;

	/** Use unquoted keys when safe. */
	protected final boolean useUnquotedKeys;

	/** Omit root braces. */
	protected final boolean omitRootBraces;

	/** Use triple-quoted """ for strings with newlines. */
	protected final boolean useMultilineStrings;

	/** Use = (not :) for key-value separator. */
	protected final boolean useEqualsSign;

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, use indentation and newlines.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, trim strings before serialization.
	 * @param useNewlineSeparators Use newlines instead of commas.
	 * @param useUnquotedStrings Use unquoted strings for simple values.
	 * @param useUnquotedKeys Use unquoted keys when safe.
	 * @param omitRootBraces Omit root object braces.
	 * @param useMultilineStrings Use """ for multiline strings.
	 * @param useEqualsSign Use = for key-value separator.
	 * @param uriResolver The URI resolver.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor requires many parameters for HOCON writer configuration
	})
	protected HoconWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings,
			boolean useNewlineSeparators, boolean useUnquotedStrings, boolean useUnquotedKeys,
			boolean omitRootBraces, boolean useMultilineStrings, boolean useEqualsSign,
			UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '"', uriResolver);
		this.useNewlineSeparators = useNewlineSeparators;
		this.useUnquotedStrings = useUnquotedStrings;
		this.useUnquotedKeys = useUnquotedKeys;
		this.omitRootBraces = omitRootBraces;
		this.useMultilineStrings = useMultilineStrings;
		this.useEqualsSign = useEqualsSign;
	}

	/**
	 * Writes the key (unquoted or quoted).
	 *
	 * @param key The key to write.
	 * @return This object.
	 */
	public HoconWriter key(String key) {
		if (key == null)
			key = "";
		if (needsQuoting(key))
			quotedString(key);
		else
			unquotedString(key);
		return this;
	}

	/**
	 * Writes the key-value separator (= or :).
	 *
	 * @return This object.
	 */
	public HoconWriter equalsSign() {
		if (useEqualsSign)
			w(" = ");
		else
			w(": ");
		return this;
	}

	/**
	 * Writes object start.
	 *
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HoconWriter objectStart(int depth) {
		w('{');
		if (useWhitespace)
			nl(depth + 1);
		return this;
	}

	/**
	 * Writes object end.
	 *
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HoconWriter objectEnd(int depth) {
		if (useWhitespace)
			nl(depth);
		w('}');
		return this;
	}

	/**
	 * Writes array start.
	 *
	 * @return This object.
	 */
	public HoconWriter arrayStart() {
		w('[');
		return this;
	}

	/**
	 * Writes array end.
	 *
	 * @return This object.
	 */
	public HoconWriter arrayEnd() {
		w(']');
		return this;
	}

	/**
	 * Writes member separator (newline or comma).
	 *
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public HoconWriter separator(int depth) {
		if (useNewlineSeparators)
			nl(depth);
		else
			w(',');
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
		if (!useUnquotedKeys)
			return true;
		for (var i = 0; i < key.length(); i++) {
			if (QUOTE_KEY_CHARS.contains(key.charAt(i)))
				return true;
		}
		return "true".equals(key) || "false".equals(key) || "null".equals(key) || isNumber(key);
	}

	/**
	 * Returns whether the value can be written as unquoted.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the value can be unquoted.
	 */
	public boolean isSimpleValue(String value) {
		if (value == null || value.isEmpty())
			return false;
		if (!useUnquotedStrings)
			return false;
		if (value.startsWith("{") || value.startsWith("[") || value.startsWith("\"") || value.startsWith("'"))
			return false;
		if (value.startsWith("#") || value.startsWith("//") || value.startsWith("$"))
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
	 * Writes an unquoted string value.
	 *
	 * @param value The value to write.
	 * @return This object.
	 */
	public HoconWriter unquotedString(String value) {
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
	public HoconWriter quotedString(String value) {
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
	 * Writes a triple-quoted string using """ syntax.
	 *
	 * @param value The value to write.
	 * @return This object.
	 */
	public HoconWriter tripleQuotedString(String value) {
		if (value == null) {
			w("null");
			return this;
		}
		w("\"\"\"");
		w(value.replace("\r\n", "\n").replace("\r", "\n"));
		w("\"\"\"");
		return this;
	}

	// Override return types for chaining
	@Override public HoconWriter append(char c) { super.append(c); return this; }
	@Override public HoconWriter append(char[] value) { super.append(value); return this; }
	@Override public HoconWriter append(int indent, char c) { super.append(indent, c); return this; }
	@Override public HoconWriter append(int indent, String text) { super.append(indent, text); return this; }
	@Override public HoconWriter append(Object text) { super.append(text); return this; }
	@Override public HoconWriter append(String text) { super.append(text); return this; }
	@Override public HoconWriter appendIf(boolean b, char c) { super.appendIf(b, c); return this; }
	@Override public HoconWriter appendIf(boolean b, String text) { super.appendIf(b, text); return this; }
	@Override public HoconWriter appendln(int indent, String text) { super.appendln(indent, text); return this; }
	@Override public HoconWriter appendln(String text) { super.appendln(text); return this; }
	@Override public HoconWriter appendUri(Object value) { super.appendUri(value); return this; }
	@Override public HoconWriter cr(int depth) { super.cr(depth); return this; }
	@Override public HoconWriter cre(int depth) { super.cre(depth); return this; }
	@Override public HoconWriter i(int indent) { super.i(indent); return this; }
	@Override public HoconWriter ie(int indent) { super.ie(indent); return this; }
	@Override public HoconWriter nl(int indent) { super.nl(indent); return this; }
	@Override public HoconWriter nlIf(boolean flag, int indent) { super.nlIf(flag, indent); return this; }
	@Override public HoconWriter q() { super.q(); return this; }
	@Override public HoconWriter s() { super.s(); return this; }
	@Override public HoconWriter sIf(boolean flag) { super.sIf(flag); return this; }
	@Override public HoconWriter w(char value) { super.w(value); return this; }
	@Override public HoconWriter w(String value) { super.w(value); return this; }
}
