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
package org.apache.juneau.uon;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing UON-encoded text.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
public final class UonWriter extends SerializerWriter {

	private final UonSerializerSession session;
	private final boolean encodeChars, plainTextParams;
	private final char quoteChar;

	// Characters that do not need to be URL-encoded in strings.
	private static final AsciiSet unencodedChars = AsciiSet.create().ranges("a-z","A-Z","0-9").chars(";/?:@-_.!*'$(),~=").build();

	// Characters that do not need to be URL-encoded in attribute names.
	// Identical to unencodedChars, but excludes '='.
	private static final AsciiSet unencodedCharsAttrName = AsciiSet.create().ranges("a-z","A-Z","0-9").chars(";/?:@-_.!*'$(),~").build();

	// Characters that need to be preceded with an escape character.
	private static final AsciiSet escapedChars = AsciiSet.create("~'");

	private static final AsciiSet noChars = AsciiSet.create("");

	private static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Constructor.
	 *
	 * @param session The session that created this writer.
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param encodeChars If <jk>true</jk>, special characters should be encoded.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param plainTextParams If <jk>true</jk>, don't use UON notation for values.
	 * @param quoteChar The quote character to use.  If <c>0</c>, defaults to <js>'\''</js>.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	protected UonWriter(UonSerializerSession session, Writer out, boolean useWhitespace, int maxIndent,
			boolean encodeChars, boolean trimStrings, boolean plainTextParams, char quoteChar, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
		this.session = session;
		this.encodeChars = encodeChars;
		this.plainTextParams = plainTextParams;
		this.quoteChar = quoteChar;
	}

	/**
	 * Serializes the specified simple object as a UON string value.
	 *
	 * @param o The object being serialized.
	 * @param isTopAttrName If this is a top-level attribute name we're serializing.
	 * @return This object.
	 */
	public final UonWriter appendObject(Object o, boolean isTopAttrName) {

		if (o instanceof Boolean)
			return appendBoolean(o);
		if (o instanceof Number)
			return appendNumber(o);
		if (o == null)
			return append("null");

		String s = session.toString(o);

		boolean needsQuotes = (! plainTextParams) && UonUtils.needsQuotes(s);

		AsciiSet unenc = (isTopAttrName ? unencodedCharsAttrName : unencodedChars);
		AsciiSet esc = plainTextParams ? noChars : escapedChars;

		if (needsQuotes)
			w(quoteChar);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (esc.contains(c))
				w('~');
			if ((!encodeChars) || unenc.contains(c))
				w(c);
			else {
				if (c == ' ')
					w('+');
				else {
					int p = s.codePointAt(i);
					if (p < 0x0080)
						appendHex(p);
					else if (p < 0x0800) {
						int p1=p>>>6;
						appendHex(p1+192).appendHex((p&63)+128);
					} else if (p < 0x10000) {
						int p1=p>>>6, p2=p1>>>6;
						appendHex(p2+224).appendHex((p1&63)+128).appendHex((p&63)+128);
					} else {
						i++;  // Two-byte codepoint...skip past surrogate pair lower byte.
						int p1=p>>>6, p2=p1>>>6, p3=p2>>>6;
						appendHex(p3+240).appendHex((p2&63)+128).appendHex((p1&63)+128).appendHex((p&63)+128);
					}
				}
			}
		}
		if (needsQuotes)
			w(quoteChar);

		return this;
	}

	/**
	 * Appends a boolean value to the output.
	 *
	 * @param o The boolean value to append to the output.
	 * @return This object.
	 */
	protected UonWriter appendBoolean(Object o) {
		append(o.toString());
		return this;
	}

	/**
	 * Appends a numeric value to the output.
	 *
	 * @param o The numeric value to append to the output.
	 * @return This object.
	 */
	protected UonWriter appendNumber(Object o) {
		append(o.toString());
		return this;
	}

	/**
	 * Prints out a two-byte %xx sequence for the given byte value.
	 */
	private UonWriter appendHex(int b) {
		if (b > 255)
			throw new BasicRuntimeException("Invalid value passed to appendHex.  Must be in the range 0-255.  Value={0}", b);
		w('%').w(hexArray[b>>>4]).w(hexArray[b&0x0F]);
		return this;
	}

	/**
	 * Appends a URI to the output.
	 *
	 * @param uri The URI to append to the output.
	 * @return This object.
	 */
	@Override
	public SerializerWriter appendUri(Object uri) {
		return appendObject(uriResolver.resolve(uri), false);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* SerializerWriter */
	public UonWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter q() {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendIf(boolean b, String text) {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendIf(boolean b, char c) {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(char c) {
		super.append(c);
		return this;
	}

	// </FluentSetters>
}
