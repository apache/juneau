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
package org.apache.juneau.yaml.proto;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing JSON.
 * 
 * 
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>This class is not intended for external use.
 * </ul>
 */
public final class YamlWriter extends SerializerWriter {

	// Characters that trigger special handling of serializing attribute values.
	private static final AsciiSet
		encodedChars = new AsciiSet("\n\t\b\f\r'\"\\");

	private static final KeywordSet reservedWords = new KeywordSet(
		"y","Y","yes","Yes","YES","n","N","no","No","NO",
		 "true","True","TRUE","false","False","FALSE",
		 "on","On","ON","off","Off","OFF"
	);



	// Characters that represent attribute name characters that don't trigger quoting.
	// These are actually more strict than the actual Javascript specification, but
	// can be narrowed in the future if necessary.
	// For example, we quote attributes that start with $ even though we don't need to.
	private static final AsciiSet validAttrChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
	private static final AsciiSet validFirstAttrChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_");

	private final AsciiSet ec;

	/**
	 * Constructor.
	 * 
	 * @param out The writer being wrapped.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	protected YamlWriter(Writer out, char quoteChar, boolean trimStrings, UriResolver uriResolver) {
		super(out, true, Integer.MAX_VALUE, trimStrings, quoteChar, uriResolver);
		this.ec = encodedChars;
	}

	/**
	 * Serializes the specified object as a JSON string value.
	 * 
	 * @param s The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	public YamlWriter stringValue(String s) throws IOException {
		if (s == null)
			return this;
		boolean doConvert = false;
		for (int i = 0; i < s.length() && ! doConvert; i++) {
			char c = s.charAt(i);
			doConvert |= ec.contains(c);
		}
		q();
		if (! doConvert) {
			out.append(s);
		} else {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (ec.contains(c)) {
					if (c == '\n')
						out.append('\\').append('n');
					else if (c == '\t')
						out.append('\\').append('t');
					else if (c == '\b')
						out.append('\\').append('b');
					else if (c == '\f')
						out.append('\\').append('f');
					else if (c == quoteChar)
						out.append('\\').append(quoteChar);
					else if (c == '\\')
						out.append('\\').append('\\');
					else if (c != '\r')
						out.append(c);
				} else {
					out.append(c);
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
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	public YamlWriter attr(String s) throws IOException {
		/*
		 * Converts a Java string to an acceptable JSON attribute name. If
		 * useStrictJson is false, then quotes will only be used if the attribute
		 * name consists of only alphanumeric characters.
		 */
		boolean doConvert = false;		// Always convert when not in lax mode.

		// If the attribute is null, it must always be printed as null without quotes.
		// Technically, this isn't part of the JSON spec, but it does allow for null key values.
		if (s == null) {
			s = "~";
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
			out.append(s);

		return this;
	}

	/**
	 * Appends a URI to the output.
	 * 
	 * @param uri The URI to append to the output.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public SerializerWriter uriValue(Object uri) throws IOException {
		return stringValue(uriResolver.resolve(uri));
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerWriter */
	public YamlWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter cre(int depth) throws IOException {
		super.cre(depth);
		return this;
	}

	/**
	 * Performs an indentation only if we're currently past max indentation.
	 * 
	 * @param depth The current indentation depth.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public YamlWriter smi(int depth) throws IOException {
		if (depth > maxIndent)
			super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter s() throws IOException {
		super.s();
		return this;
	}

	/**
	 * Adds a space only if the current indentation level is below maxIndent.
	 * 
	 * @param indent
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public YamlWriter s(int indent) throws IOException {
		if (indent <= maxIndent)
			super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter nl(int indent) throws IOException {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter appendIf(boolean b, String text) throws IOException {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter appendIf(boolean b, char c) throws IOException {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public YamlWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}
}
