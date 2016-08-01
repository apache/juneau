/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import java.io.*;

import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Specialized writer for serializing JSON.
 * <p>
 * 	<b>Note:  This class is not intended for external use.</b>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonSerializerWriter extends SerializerWriter {

	private final boolean laxMode, escapeSolidus;

	// Characters that trigger special handling of serializing attribute values.
	private static final AsciiSet
		encodedChars = new AsciiSet("\n\t\b\f\r'\"\\"),
		encodedChars2 = new AsciiSet("\n\t\b\f\r'\"\\/");

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
	private static final AsciiSet validAttrChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
	private static final AsciiSet validFirstAttrChars = new AsciiSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_");

	private final AsciiSet ec;

	/**
	 * Constructor.
	 * @param out The writer being wrapped.
	 * @param useIndentation If <jk>true</jk>, tabs will be used in output.
	 * @param useWhitespace If <jk>true</jk>, whitespace will be used in output.
	 * @param escapeSolidus If <jk>true</jk>, forward slashes should be escaped in the output.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param laxMode If <jk>true</jk>, JSON attributes will only be quoted when necessary.
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param relativeUriBase The base (e.g. <js>https://localhost:9443/contextPath"</js>) for relative URIs (e.g. <js>"my/path"</js>).
	 * @param absolutePathUriBase The base (e.g. <js>https://localhost:9443"</js>) for relative URIs with absolute paths (e.g. <js>"/contextPath/my/path"</js>).
	 */
	protected JsonSerializerWriter(Writer out, boolean useIndentation, boolean useWhitespace, boolean escapeSolidus, char quoteChar, boolean laxMode, boolean trimStrings, String relativeUriBase, String absolutePathUriBase) {
		super(out, useIndentation, useWhitespace, trimStrings, quoteChar, relativeUriBase, absolutePathUriBase);
		this.laxMode = laxMode;
		this.escapeSolidus = escapeSolidus;
		this.ec = escapeSolidus ? encodedChars2 : encodedChars;
	}

	/**
	 * Serializes the specified object as a JSON string value.
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	public JsonSerializerWriter stringValue(Object o) throws IOException {
		 /*
		  * Fixes up a Java string so that it can be used as a JSON string.<br>
		  * Does the following:<br>
		  * <ul>
		  *  <li> Replaces {@code \r?\n} with {@code \\n}<br>
		  *  <li> Replaces {@code \t} with {@code \\t}<br>
		  *  <li> Replaces {@code '} with {@code \\'}<br>
		  *  <li> Replaces {@code "} with {@code \\"}<br>
		  * </ul>
		  */
		if (o == null)
			return this;
		String s = o.toString();
		if (trimStrings)
			s = s.trim();
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
					else if (c == '/' && escapeSolidus)
						out.append('\\').append('/');
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
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException Should never happen.
	 */
	public JsonSerializerWriter attr(Object o) throws IOException {
		/*
		 * Converts a Java string to an acceptable JSON attribute name. If
		 * useStrictJson is false, then quotes will only be used if the attribute
		 * name consists of only alphanumeric characters.
		 */
		boolean doConvert = trimStrings || ! laxMode;		// Always convert when not in lax mode.

		String s = null;

		// If the attribute is null, it must always be printed as null without quotes.
		// Technically, this isn't part of the JSON spec, but it does allow for null key values.
		if (o == null) {
			s = "null";
			doConvert = false;

		} else {
			s = o.toString();

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

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerWriter */
	public JsonSerializerWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter s() throws IOException {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter nl() throws IOException {
		super.nl();
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter appendIf(boolean b, String text) throws IOException {
		super.appendIf(b, text);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter appendIf(boolean b, char c) throws IOException {
		super.appendIf(b, c);
		return this;
	}

	@Override /* SerializerWriter */
	public JsonSerializerWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}
}
