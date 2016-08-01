/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import java.io.*;

import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Specialized writer for serializing UON-encoded text.
 * <p>
 * 	<b>Note:  This class is not intended for external use.</b>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class UonSerializerWriter extends SerializerWriter {

	private final boolean simpleMode, encodeChars;

	// Characters that do not need to be URL-encoded in strings.
	private static final AsciiSet unencodedChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;/?:@-_.!*'$(),~=");

	// Characters that do not need to be URL-encoded in attribute names.
	// Identical to unencodedChars, but excludes '='.
	private static final AsciiSet unencodedCharsAttrName = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;/?:@-_.!*'$(),~");

	// Characters that need to be preceeded with an escape character.
	private static final AsciiSet escapedChars = new AsciiSet(",()~=");

	// AsciiSet that maps no characters.
	private static final AsciiSet emptyCharSet = new AsciiSet("");

	private static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useIndentation If <jk>true</jk>, tabs will be used in output.
	 * @param simpleMode If <jk>true</jk>, type flags will not be generated in output.
	 * @param encodeChars If <jk>true</jk>, special characters should be encoded.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param relativeUriBase The base (e.g. <js>https://localhost:9443/contextPath"</js>) for relative URIs (e.g. <js>"my/path"</js>).
	 * @param absolutePathUriBase The base (e.g. <js>https://localhost:9443"</js>) for relative URIs with absolute paths (e.g. <js>"/contextPath/my/path"</js>).
	 */
	protected UonSerializerWriter(Writer out, boolean useIndentation, boolean simpleMode, boolean encodeChars, boolean trimStrings, String relativeUriBase, String absolutePathUriBase) {
		super(out, useIndentation, false, trimStrings, '\'', relativeUriBase, absolutePathUriBase);
		this.simpleMode = simpleMode;
		this.encodeChars = encodeChars;
	}

	/**
	 * Serializes the specified simple object as a UON string value.
	 *
	 * @param o The object being serialized.
	 * @param quoteEmptyStrings Special case where we're serializing an array containing an empty string.
	 * @param isTopAttrName If this is a top-level attribute name we're serializing.
	 * @param isTop If this is a top-level value we're serializing.
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	protected UonSerializerWriter appendObject(Object o, boolean quoteEmptyStrings, boolean isTopAttrName, boolean isTop) throws IOException {

		char typeFlag = 0;

		if (o == null)
			o = "\u0000";
		else if (o.equals("\u0000"))
			typeFlag = 's';

		String s = o.toString();
		if (trimStrings)
			s = s.trim();
		if (s.isEmpty()) {
			if (quoteEmptyStrings)
				typeFlag = 's';
		} else if (s.charAt(0) == '(' || s.charAt(0) == '$') {
			typeFlag = 's';
		} else if (useIndentation && (s.indexOf('\n') != -1 || (s.charAt(0) <= ' ' && s.charAt(0) != 0))) {
			// Strings containing newline characters must always be quoted so that they're not confused with whitespace.
			// Also, strings starting with whitespace must be quoted so that the contents are not ignored when whitespace is ignored.
			typeFlag = 's';
		} else if (! simpleMode) {
			if (o instanceof Boolean)
				typeFlag = 'b';
			else if (o instanceof Number)
				typeFlag = 'n';
		}

		if (typeFlag != 0)
			startFlag(typeFlag);

		AsciiSet unenc = (isTopAttrName ? unencodedCharsAttrName : unencodedChars);
		AsciiSet esc = (isTop && typeFlag == 0 ? emptyCharSet : escapedChars);

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

		if (typeFlag != 0)
			append(')');

		return this;
	}

	/**
	 * Prints <code>$f(</code> in normal mode, and <code>(</code> in simple mode.
	 *
	 * @param f The flag character.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	protected UonSerializerWriter startFlag(char f) throws IOException {
		if (f != 's' && ! simpleMode)
			append('$').append(f);
		append('(');
		return this;
	}

	/**
	 * Prints out a two-byte %xx sequence for the given byte value.
	 */
	private UonSerializerWriter appendHex(int b) throws IOException {
		if (b > 255)
			throw new IOException("Invalid value passed to appendHex.  Must be in the range 0-255.  Value=" + b);
		append('%').append(hexArray[b>>>4]).append(hexArray[b&0x0F]);
		return this;
	}

	/**
	 * Appends a URI to the output.
	 *
	 * @param uri The URI to append to the output.
	 * @param isTop If this is a top-level value we're serializing.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public SerializerWriter appendUri(Object uri, boolean isTop) throws IOException {
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
		return appendObject(s, false, false, isTop);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerWriter */
	public UonSerializerWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter nl() throws IOException {
		super.nl();
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter appendIf(boolean b, String text) throws IOException {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter appendIf(boolean b, char c) throws IOException {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public UonSerializerWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}
}
