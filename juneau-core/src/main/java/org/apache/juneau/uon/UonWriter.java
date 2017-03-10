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

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing UON-encoded text.
 * <p>
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>This class is not intended for external use.
 * </ul>
 */
public final class UonWriter extends SerializerWriter {

	private final UonSerializerSession session;
	private final boolean encodeChars;

	// Characters that do not need to be URL-encoded in strings.
	private static final AsciiSet unencodedChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;/?:@-_.!*'$(),~=");

	// Characters that do not need to be URL-encoded in attribute names.
	// Identical to unencodedChars, but excludes '='.
	private static final AsciiSet unencodedCharsAttrName = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;/?:@-_.!*'$(),~");

	// Characters that need to be preceeded with an escape character.
	private static final AsciiSet escapedChars = new AsciiSet("~'");

	private static final AsciiSet needsQuoteChars = new AsciiSet("),=\n\t\r\b\f ");

	private static final AsciiSet maybeNeedsQuotesFirstChar = new AsciiSet("),=\n\t\r\b\f tfn+-.#0123456789");

	private static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Constructor.
	 *
	 * @param session The session that created this writer.
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs will be used in output.
	 * @param encodeChars If <jk>true</jk>, special characters should be encoded.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param relativeUriBase The base (e.g. <js>https://localhost:9443/contextPath"</js>) for relative URIs (e.g. <js>"my/path"</js>).
	 * @param absolutePathUriBase The base (e.g. <js>https://localhost:9443"</js>) for relative URIs with absolute paths (e.g. <js>"/contextPath/my/path"</js>).
	 */
	protected UonWriter(UonSerializerSession session, Writer out, boolean useWhitespace, boolean encodeChars, boolean trimStrings, String relativeUriBase, String absolutePathUriBase) {
		super(out, useWhitespace, trimStrings, '\'', relativeUriBase, absolutePathUriBase);
		this.session = session;
		this.encodeChars = encodeChars;
	}

	/**
	 * Serializes the specified simple object as a UON string value.
	 *
	 * @param o The object being serialized.
	 * @param isTopAttrName If this is a top-level attribute name we're serializing.
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	public final UonWriter appendObject(Object o, boolean isTopAttrName) throws IOException {

		if (o instanceof Boolean)
			return appendBoolean(o);
		if (o instanceof Number)
			return appendNumber(o);
		if (o == null)
			return append("null");

		String s = session.toString(o);
		char c0 = s.isEmpty() ? 0 : s.charAt(0);

		boolean needsQuotes =
			s.isEmpty()
			|| c0 == '@'
			|| c0 == '('
			|| needsQuoteChars.contains(s)
			|| (
				maybeNeedsQuotesFirstChar.contains(c0)
				&& (
					"true".equals(s)
					|| "false".equals(s)
					|| "null".equals(s)
					|| StringUtils.isNumeric(s)
				)
			)
		;

		AsciiSet unenc = (isTopAttrName ? unencodedCharsAttrName : unencodedChars);
		AsciiSet esc = escapedChars;

		if (needsQuotes)
			append('\'');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (esc.contains(c))
				append('~');
			if ((!encodeChars) || unenc.contains(c))
				append(c);
			else {
				if (c == ' ')
					append('+');
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
			append('\'');

		return this;
	}

	/**
	 * Appends a boolean value to the output.
	 *
	 * @param o The boolean value to append to the output.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	protected UonWriter appendBoolean(Object o) throws IOException {
		append(o.toString());
		return this;
	}

	/**
	 * Appends a numeric value to the output.
	 *
	 * @param o The numeric value to append to the output.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	protected UonWriter appendNumber(Object o) throws IOException {
		append(o.toString());
		return this;
	}

	/**
	 * Prints out a two-byte %xx sequence for the given byte value.
	 */
	private UonWriter appendHex(int b) throws IOException {
		if (b > 255)
			throw new IOException("Invalid value passed to appendHex.  Must be in the range 0-255.  Value=" + b);
		append('%').append(hexArray[b>>>4]).append(hexArray[b&0x0F]);
		return this;
	}

	/**
	 * Appends a URI to the output.
	 *
	 * @param uri The URI to append to the output.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	@Override
	public SerializerWriter appendUri(Object uri) throws IOException {
		String s = uri.toString();
		if (s.indexOf("://") == -1) {
			if (StringUtils.startsWith(s, '/')) {
				if (absolutePathUriBase != null)
					append(absolutePathUriBase);
			} else {
				if (relativeUriBase != null) {
					append(relativeUriBase);
					if (! relativeUriBase.equals("/"))
						append("/");
				}
			}
		}
		return appendObject(s, false);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerWriter */
	public UonWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter nl() throws IOException {
		super.nl();
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendIf(boolean b, String text) throws IOException {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter appendIf(boolean b, char c) throws IOException {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}
}
