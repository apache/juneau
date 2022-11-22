// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.json;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing JSON.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public final class JsonWriter extends SerializerWriter {

	private final boolean simpleAttrs, escapeSolidus;

	// Characters that trigger special handling of serializing attribute values.
	private static final AsciiSet
		encodedChars = AsciiSet.create("\n\t\b\f\r'\"\\"),
		encodedChars2 = AsciiSet.create("\n\t\b\f\r'\"\\/");

	private static final KeywordSet reservedWords = new KeywordSet(
		"arguments","break","case","catch","class","const","continue","debugger","default","delete",
		"do","else","enum","eval","export","extends","false","finally","for","function","if",
		"implements","import","in","instanceof","interface","let","new","null","package",
		"private","protected","public","return","static","super","switch","this","throw",
		"true","try","typeof","var","void","while","with","undefined","yield"
	);


	// Characters that represent attribute name characters that don't trigger quoting.
	// These are actually more strict than the actual Javascript specification, but
	// can be narrowed in the future if necessary.
	// For example, we quote attributes that start with $ even though we don't need to.
	private static final AsciiSet validAttrChars = AsciiSet.create().ranges("a-z","A-Z","0-9").chars("_").build();
	private static final AsciiSet validFirstAttrChars = AsciiSet.create().ranges("a-z","A-Z").chars("_").build();

	private final AsciiSet ec;

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs and spaces will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param escapeSolidus If <jk>true</jk>, forward slashes should be escaped in the output.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param simpleAttrs If <jk>true</jk>, JSON attributes will only be quoted when necessary.
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	protected JsonWriter(Writer out, boolean useWhitespace, int maxIndent, boolean escapeSolidus, char quoteChar,
			boolean simpleAttrs, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
		this.simpleAttrs = simpleAttrs;
		this.escapeSolidus = escapeSolidus;
		this.ec = escapeSolidus ? encodedChars2 : encodedChars;
	}

	/**
	 * Serializes the specified object as a JSON string value.
	 *
	 * @param s The object being serialized.
	 * @return This object.
	 */
	public JsonWriter stringValue(String s) {
		if (s == null)
			return this;
		boolean doConvert = false;
		for (int i = 0; i < s.length() && ! doConvert; i++) {
			char c = s.charAt(i);
			doConvert |= ec.contains(c);
		}
		q();
		if (! doConvert) {
			w(s);
		} else {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (ec.contains(c)) {
					if (c == '\n')
						w('\\').w('n');
					else if (c == '\t')
						w('\\').w('t');
					else if (c == '\b')
						w('\\').w('b');
					else if (c == '\f')
						w('\\').w('f');
					else if (c == quoteChar)
						w('\\').w(quoteChar);
					else if (c == '\\')
						w('\\').w('\\');
					else if (c == '/' && escapeSolidus)
						w('\\').w('/');
					else if (c != '\r')
						w(c);
				} else {
					w(c);
				}
			}
		}
		q();
		return this;
	}

	/**
	 * Serializes the specified object as a JSON attribute name.
	 *
	 * @param s The object being serialized.
	 * @return This object.
	 */
	public JsonWriter attr(String s) {

		if (trimStrings)
			s = StringUtils.trim(s);

		/*
		 * Converts a Java string to an acceptable JSON attribute name. If
		 * simpleAttrs is true, then quotes will only be used if the attribute
		 * name consists of only alphanumeric characters.
		 */
		boolean doConvert = ! simpleAttrs;		// Always convert when not in lax mode.

		// If the attribute is null, it must always be printed as null without quotes.
		// Technically, this isn't part of the JSON spec, but it does allow for null key values.
		if (s == null) {
			s = "null";
			doConvert = false;

		} else {

			// Look for characters that would require the attribute to be quoted.
			// All possible numbers should be caught here.
			if (! doConvert) {
				for (int i = 0; i < s.length() && ! doConvert; i++) {
					char c = s.charAt(i);
					doConvert |= ! (i == 0 ? validFirstAttrChars.contains(c) : validAttrChars.contains(c));
				}
			}

			// Reserved words and blanks must be quoted.
			if (! doConvert) {
				if (s.isEmpty() || reservedWords.contains(s))
					doConvert = true;
			}
		}

		// If no conversion necessary, just print the attribute as-is.
		if (doConvert)
			stringValue(s);
		else
			w(s);

		return this;
	}

	/**
	 * Appends a URI to the output.
	 *
	 * @param uri The URI to append to the output.
	 * @return This object.
	 */
	public SerializerWriter uriValue(Object uri) {
		return stringValue(uriResolver.resolve(uri));
	}

	/**
	 * Performs an indentation only if we're currently past max indentation.
	 *
	 * @param depth The current indentation depth.
	 * @return This object.
	 */
	public JsonWriter smi(int depth) {
		if (depth > maxIndent)
			super.s();
		return this;
	}

	/**
	 * Adds a space only if the current indentation level is below maxIndent.
	 *
	 * @param indent The number of spaces to indent.
	 * @return This object.
	 */
	public JsonWriter s(int indent) {
		if (indent <= maxIndent)
			super.s();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* SerializerWriter */
	public JsonWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter s() {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter w(char value) {
		super.w(value);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter w(String value) {
		super.w(value);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter q() {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter appendIf(boolean b, String text) {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter appendIf(boolean b, char c) {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonWriter append(char c) {
		super.append(c);
		return this;
	}

	// </FluentSetters>
}
