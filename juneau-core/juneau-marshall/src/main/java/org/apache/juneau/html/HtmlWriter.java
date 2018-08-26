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
package org.apache.juneau.html;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.*;

/**
 * Specialized writer for serializing HTML.
 */
public class HtmlWriter extends XmlWriter {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	public HtmlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar,
			UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver, false, null);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* XmlSerializerWriter */
	public HtmlWriter text(Object o, boolean preserveWhitespace) throws IOException {

		if (o == null) {
			append("<null/>");
			return this;
		}
		String s = o.toString();
		if (s.isEmpty()) {
			append("<sp/>");
			return this;
		}

		for (int i = 0; i < s.length(); i++) {
			char test = s.charAt(i);
			if (test == '&')
				append("&amp;");
			else if (test == '<')
				append("&lt;");
			else if (test == '>')
				append("&gt;");
			else if (test == '\n')
				append(preserveWhitespace ? "\n" : "<br/>");
			else if (test == '\f')  // XML 1.0 doesn't support formfeeds or backslashes, so we have to invent something.
				append(preserveWhitespace ? "\f" : "<ff/>");
			else if (test == '\b')
				append(preserveWhitespace ? "\b" : "<bs/>");
			else if (test == '\t')
				append(preserveWhitespace ? "\t" : "<sp>&#x2003;</sp>");
			else if ((i == 0 || i == s.length()-1) && Character.isWhitespace(test)) {
				if (preserveWhitespace)
					append(test);
				else if (test == ' ')
					append("<sp> </sp>");
				else
					append("<sp>&#x").append(toHex4(test)).append(";</sp>");
			}
			else if (Character.isISOControl(test))
				append("&#" + (int) test + ";");
			else
				append(test);
		}

		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.oTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(String ns, String name) throws IOException {
		super.oTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(String name) throws IOException {
		super.oTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.oTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(int indent, String ns, String name) throws IOException {
		super.oTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oTag(int indent, String name) throws IOException {
		super.oTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(String ns, String name, boolean needsEncoding) throws IOException {
		super.tag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(String ns, String name) throws IOException {
		super.tag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(String name) throws IOException {
		super.tag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(int indent, String name) throws IOException {
		super.tag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.tag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter tag(int indent, String ns, String name) throws IOException {
		super.tag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(String ns, String name) throws IOException {
		super.sTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.sTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(int indent, String ns, String name) throws IOException {
		super.sTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(int indent, String name) throws IOException {
		super.sTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(String name) throws IOException {
		super.sTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter sTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.sTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(String ns, String name) throws IOException {
		super.eTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.eTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(int indent, String ns, String name) throws IOException {
		super.eTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(int indent, String name) throws IOException {
		super.eTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(String name) throws IOException {
		super.eTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter eTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.eTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter attr(String name, Object value) throws IOException {
		super.attr(name, value);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter attr(String ns, String name, Object value) throws IOException {
		super.attr(ns, name, value);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter attr(String ns, String name, Object value, boolean valNeedsEncoding) throws IOException {
		super.attr(ns, name, value, valNeedsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter attr(String name, Object value, boolean valNeedsEncoding) throws IOException {
		super.attr(null, name, value, valNeedsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlWriter oAttr(String ns, String name) throws IOException {
		super.oAttr(ns, name);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter cr(int depth) throws IOException {
		if (depth > 0)
			super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter cre(int depth) throws IOException {
		if (depth > 0)
			super.cre(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter s() throws IOException {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter nl(int indent) throws IOException {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}
}
