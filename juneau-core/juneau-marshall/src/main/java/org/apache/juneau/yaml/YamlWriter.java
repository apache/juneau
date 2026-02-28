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
package org.apache.juneau.yaml;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing YAML.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/YamlBasics">YAML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Writer resource managed by calling code
})
public class YamlWriter extends SerializerWriter {

	private static final AsciiSet specialChars = AsciiSet.of(":#{}[],&*!|>'\"%@`");

	// @formatter:off
	private static final Set<String> reservedWords = new HashSet<>(Arrays.asList(
		"null", "Null", "NULL", "~",
		"true", "True", "TRUE",
		"false", "False", "FALSE",
		"yes", "Yes", "YES",
		"no", "No", "NO",
		"on", "On", "ON",
		"off", "Off", "OFF",
		".inf", ".Inf", ".INF",
		"-.inf", "-.Inf", "-.INF",
		".nan", ".NaN", ".NAN"
	));
	// @formatter:on

	private static final AsciiSet escapedChars = AsciiSet.of("\\\"\n\t\r\0\u0007\b\f\u001b");

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs and spaces will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	protected YamlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '"', uriResolver);
	}

	/**
	 * Writes a YAML mapping key.
	 *
	 * <p>
	 * If the key needs quoting (contains special YAML characters, starts/ends with whitespace,
	 * is empty, or is a YAML reserved word), it will be double-quoted with proper escaping.
	 * A colon-space separator is always appended after the key.
	 *
	 * @param s The key name.
	 * @return This object.
	 */
	public YamlWriter key(String s) {
		keyName(s);
		w(": ");
		return this;
	}

	/**
	 * Writes just the key name portion (with quoting if necessary) but without any separator.
	 *
	 * @param s The key name.
	 * @return This object.
	 */
	public YamlWriter keyName(String s) {
		if (trimStrings)
			s = trim(s);

		if (s == null) {
			w("null");
		} else if (needsQuoting(s)) {
			writeQuoted(s);
		} else {
			w(s);
		}
		return this;
	}

	/**
	 * Writes a YAML scalar text value.
	 *
	 * <p>
	 * If null, writes the literal <c>null</c>. If the string needs quoting (is empty,
	 * looks like a boolean/null/number, contains special characters, starts/ends with whitespace,
	 * or contains newlines), it will be double-quoted with proper YAML escaping.
	 *
	 * @param s The string value.
	 * @return This object.
	 */
	public YamlWriter textValue(String s) {
		if (trimStrings)
			s = trim(s);

		if (s == null) {
			w("null");
		} else if (needsQuoting(s)) {
			writeQuoted(s);
		} else {
			w(s);
		}
		return this;
	}

	/**
	 * Appends a URI value to the output.
	 *
	 * <p>
	 * Resolves the URI and writes it as a double-quoted YAML string.
	 *
	 * @param uri The URI to append to the output.
	 * @return This object.
	 */
	public YamlWriter uriValue(Object uri) {
		writeQuoted(uriResolver.resolve(uri));
		return this;
	}

	/**
	 * Writes YAML block indentation.
	 *
	 * <p>
	 * Writes a newline followed by {@code depth * 2} spaces of indentation.
	 *
	 * @param depth The indentation depth.
	 * @return This object.
	 */
	public YamlWriter yamlIndent(int depth) {
		w('\n');
		for (var i = 0; i < depth * 2; i++)
			w(' ');
		return this;
	}

	/**
	 * Writes a YAML list entry prefix.
	 *
	 * <p>
	 * Writes a newline followed by {@code depth * 2} spaces and a dash-space (<js>"- "</js>) prefix.
	 *
	 * @param depth The indentation depth.
	 * @return This object.
	 */
	public YamlWriter listEntry(int depth) {
		w('\n');
		for (var i = 0; i < depth * 2; i++)
			w(' ');
		w("- ");
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the specified string needs double-quoting in YAML.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if the string needs quoting.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for YAML quoting checks
	})
	private boolean needsQuoting(String s) {
		if (s.isEmpty())
			return true;

		if (Character.isWhitespace(s.charAt(0)) || Character.isWhitespace(s.charAt(s.length() - 1)))
			return true;

		if (reservedWords.contains(s))
			return true;

		if (looksLikeNumber(s))
			return true;

		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\n' || c == '\r')
				return true;
			if (specialChars.contains(c))
				return true;
			if (escapedChars.contains(c))
				return true;
		}

		return false;
	}

	/**
	 * Returns <jk>true</jk> if the string looks like a YAML number (integer, float, hex, or octal).
	 */
	private static boolean looksLikeNumber(String s) {
		if (s.isEmpty())
			return false;
		var c0 = s.charAt(0);
		if (c0 == '+' || c0 == '-')
			return s.length() > 1 && isDigitOrDot(s.charAt(1));
		if (c0 == '.')
			return s.length() > 1 && Character.isDigit(s.charAt(1));
		if (c0 == '0' && s.length() > 1) {
			var c1 = s.charAt(1);
			if (c1 == 'x' || c1 == 'X' || c1 == 'o' || c1 == 'O')
				return true;
		}
		return Character.isDigit(c0);
	}

	private static boolean isDigitOrDot(char c) {
		return Character.isDigit(c) || c == '.';
	}

	/**
	 * Writes a double-quoted YAML string with proper escaping.
	 */
	private void writeQuoted(String s) {
		w('"');
		if (s != null) {
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				if (escapedChars.contains(c)) {
					switch (c) {
						case '\\': w('\\').w('\\'); break;
						case '"':  w('\\').w('"'); break;
						case '\n': w('\\').w('n'); break;
						case '\t': w('\\').w('t'); break;
						case '\r': w('\\').w('r'); break;
						case '\0': w('\\').w('0'); break;
						case '\u0007': w('\\').w('a'); break;
						case '\b': w('\\').w('b'); break;
						case '\f': w('\\').w('f'); break;
						case '\u001b': w('\\').w('e'); break;
						default: w(c);
					}
				} else {
					w(c);
				}
			}
		}
		w('"');
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods - return type covariance for chaining
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(char c) {
		super.append(c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(char[] value) {
		super.append(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter appendIf(boolean b, char c) {
		super.appendIf(b, c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter appendIf(boolean b, String text) {
		super.appendIf(b, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter appendUri(Object value) {
		super.appendUri(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter ie(int indent) {
		super.ie(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter nlIf(boolean flag, int indent) {
		super.nlIf(flag, indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter q() {
		super.q();
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter s() {
		super.s();
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter sIf(boolean flag) {
		super.sIf(flag);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter w(char value) {
		super.w(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public YamlWriter w(String value) {
		super.w(value);
		return this;
	}
}
