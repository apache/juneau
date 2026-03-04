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
package org.apache.juneau.ini;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing INI format.
 *
 * <p>
 * Extends {@link SerializerWriter} with INI-specific methods for sections, key-value pairs,
 * comments, and quoted strings.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Writer resource managed by calling code
})
public class IniWriter extends SerializerWriter {

	/** Key-value separator character. */
	protected final char kvSeparator;

	/** Whether to add spaces around the separator. */
	protected final boolean spacedSeparator;

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, use blank lines between sections.
	 * @param maxIndent The maximum indentation level (unused for INI).
	 * @param trimStrings If <jk>true</jk>, trim strings before serialization.
	 * @param kvSeparator The key-value separator ('=' or ':').
	 * @param spacedSeparator If <jk>true</jk>, add spaces around separator.
	 * @param uriResolver The URI resolver.
	 */
	protected IniWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char kvSeparator, boolean spacedSeparator, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '\'', uriResolver);
		this.kvSeparator = kvSeparator;
		this.spacedSeparator = spacedSeparator;
	}

	/**
	 * Writes a section header with optional preceding blank line.
	 *
	 * @param name The section name or path (e.g. "address" or "employment/company").
	 * @return This object.
	 */
	public IniWriter section(String name) {
		w('\n');
		w('[').w(name).w(']').w('\n');
		return this;
	}

	/**
	 * Writes a comment line.
	 *
	 * @param text The comment text.
	 * @return This object.
	 */
	public IniWriter comment(String text) {
		w("# ");
		if (text != null && !text.isEmpty()) {
			var lines = text.split("\n");
			for (var i = 0; i < lines.length; i++) {
				if (i > 0)
					w("\n# ");
				w(lines[i]);
			}
		}
		w('\n');
		return this;
	}

	/**
	 * Writes a key-value pair.
	 *
	 * @param key The key name.
	 * @param value The value (already formatted as string).
	 * @return This object.
	 */
	public IniWriter keyValue(String key, String value) {
		w(key);
		if (spacedSeparator)
			w(" ").w(kvSeparator).w(" ");
		else
			w(kvSeparator);
		w(value != null ? value : "null");
		w('\n');
		return this;
	}

	/**
	 * Writes a single-quoted string value with escaping.
	 *
	 * <p>
	 * Single quotes within the value are escaped as <c>''</c>.
	 *
	 * @param value The raw string value.
	 * @return This object.
	 */
	public IniWriter quotedString(String value) {
		if (value == null) {
			w("null");
			return this;
		}
		w('\'').w(value.replace("'", "''")).w('\'');
		return this;
	}

	/**
	 * Writes a blank line.
	 *
	 * @return This object.
	 */
	public IniWriter blankLine() {
		w('\n');
		return this;
	}

	// Override return types for chaining
	@Override public IniWriter append(char c) { super.append(c); return this; }
	@Override public IniWriter append(char[] value) { super.append(value); return this; }
	@Override public IniWriter append(int indent, char c) { super.append(indent, c); return this; }
	@Override public IniWriter append(int indent, String text) { super.append(indent, text); return this; }
	@Override public IniWriter append(Object text) { super.append(text); return this; }
	@Override public IniWriter append(String text) { super.append(text); return this; }
	@Override public IniWriter appendIf(boolean b, char c) { super.appendIf(b, c); return this; }
	@Override public IniWriter appendIf(boolean b, String text) { super.appendIf(b, text); return this; }
	@Override public IniWriter appendln(int indent, String text) { super.appendln(indent, text); return this; }
	@Override public IniWriter appendln(String text) { super.appendln(text); return this; }
	@Override public IniWriter appendUri(Object value) { super.appendUri(value); return this; }
	@Override public IniWriter cr(int depth) { super.cr(depth); return this; }
	@Override public IniWriter cre(int depth) { super.cre(depth); return this; }
	@Override public IniWriter i(int indent) { super.i(indent); return this; }
	@Override public IniWriter ie(int indent) { super.ie(indent); return this; }
	@Override public IniWriter nl(int indent) { super.nl(indent); return this; }
	@Override public IniWriter nlIf(boolean flag, int indent) { super.nlIf(flag, indent); return this; }
	@Override public IniWriter q() { super.q(); return this; }
	@Override public IniWriter s() { super.s(); return this; }
	@Override public IniWriter sIf(boolean flag) { super.sIf(flag); return this; }
	@Override public IniWriter w(char value) { super.w(value); return this; }
	@Override public IniWriter w(String value) { super.w(value); return this; }
}
