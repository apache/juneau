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
package org.apache.juneau.json;

import java.io.*;

import org.apache.juneau.*;

import static org.apache.juneau.commons.utils.ThrowableUtils.rex;

/**
 * JSON writer for JCS (RFC 8785) canonical output.
 *
 * <p>
 * Extends {@link JsonWriter} with ECMAScript-compatible number serialization and JCS string
 * escaping (control characters, lone surrogate validation).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Writer resource managed by calling code
})
public class JcsWriter extends JsonWriter {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs and spaces will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param escapeSolidus If <jk>true</jk>, forward slashes should be escaped in the output.
	 * @param quoteChar The quote character to use (always <js>'"'</js> for JCS).
	 * @param simpleAttrs If <jk>true</jk>, JSON attributes will only be quoted when necessary.
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor requires 8 parameters for JSON writer configuration
	})
	protected JcsWriter(Writer out, boolean useWhitespace, int maxIndent, boolean escapeSolidus, char quoteChar, boolean simpleAttrs, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, escapeSolidus, quoteChar, simpleAttrs, trimStrings, uriResolver);
	}

	@Override /* Overridden from SerializerWriter */
	public JcsWriter append(Object value) {
		if (value == null) {
			w("null");
			return this;
		}
		if (value instanceof Number n)
			w(JcsSerializerSession.toEcmaNumber(n));
		else
			w(value.toString());
		return this;
	}

	@Override /* Overridden from JsonWriter */
	public JcsWriter stringValue(String s) {
		if (s == null)
			return this;
		validateNoLoneSurrogates(s);
		q();
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '"')
				w('\\').w('"');
			else if (c == '\\')
				w('\\').w('\\');
			else if (c == '\b')
				w('\\').w('b');
			else if (c == '\t')
				w('\\').w('t');
			else if (c == '\n')
				w('\\').w('n');
			else if (c == '\f')
				w('\\').w('f');
			else if (c == '\r')
				w('\\').w('r');
			else if (c >= 0 && c <= 0x1F)
				w(String.format("\\u%04x", (int) c));
			else
				w(c);
		}
		q();
		return this;
	}

	@SuppressWarnings({
		"java:S127" // Loop counter i++ needed to skip low surrogate after high surrogate pair
	})
	private static void validateNoLoneSurrogates(String s) {
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (Character.isHighSurrogate(c)) {
				if (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1)))
					throw rex("Lone high surrogate at index {0} in string", i);
				i++;
			} else if (Character.isLowSurrogate(c)) {
				throw rex("Lone low surrogate at index {0} in string", i);
			}
		}
	}
}
