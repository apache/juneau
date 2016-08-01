/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import java.io.*;

import com.ibm.juno.core.xml.*;

/**
 * Specialized writer for serializing HTML.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class HtmlSerializerWriter extends XmlSerializerWriter {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useIndentation If <jk>true</jk>, tabs will be used in output.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param uriContext The web application context path (e.g. "/contextRoot").
	 * @param uriAuthority The web application URI authority (e.g. "http://hostname:9080")
	 */
	public HtmlSerializerWriter(Writer out, boolean useIndentation, char quoteChar, String uriContext, String uriAuthority) {
		super(out, useIndentation, quoteChar, uriContext, uriAuthority, false, null);
	}

	/**
	 * Append an attribute with a URI value.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.  Can be any object whose <code>toString()</code> method returns a URI.
	 * @return This object (for method chaining);
	 * @throws IOException If a problem occurred.
	 */
	public HtmlSerializerWriter attrUri(String name, Object value) throws IOException {
		super.attrUri((String)null, name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter encodeText(Object o) throws IOException {

		String s = o.toString();
		for (int i = 0; i < s.length(); i++) {
			char test = s.charAt(i);
			if (test == '&')
				append("&amp;");
			else if (test == '<')
				append("&lt;");
			else if (test == '>')
				append("&gt;");
			else if (test == '\n')
				append("<br/>");
			else if (test == '\f')
				append("<ff/>");
			else if (test == '\b')
				append("<bs/>");
			else if (test == '\t')
				append("<tb/>");
			else if (Character.isISOControl(test))
				append("&#" + (int) test + ";");
			else
				append(test);
		}

		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.oTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(String ns, String name) throws IOException {
		super.oTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(String name) throws IOException {
		super.oTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.oTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(int indent, String ns, String name) throws IOException {
		super.oTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oTag(int indent, String name) throws IOException {
		super.oTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(String ns, String name, boolean needsEncoding) throws IOException {
		super.tag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(String ns, String name) throws IOException {
		super.tag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(String name) throws IOException {
		super.tag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(int indent, String name) throws IOException {
		super.tag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.tag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter tag(int indent, String ns, String name) throws IOException {
		super.tag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(String ns, String name) throws IOException {
		super.sTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.sTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(int indent, String ns, String name) throws IOException {
		super.sTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(int indent, String name) throws IOException {
		super.sTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(String name) throws IOException {
		super.sTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter sTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.sTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(String ns, String name) throws IOException {
		super.eTag(ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(String ns, String name, boolean needsEncoding) throws IOException {
		super.eTag(ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(int indent, String ns, String name) throws IOException {
		super.eTag(indent, ns, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(int indent, String name) throws IOException {
		super.eTag(indent, name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(String name) throws IOException {
		super.eTag(name);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter eTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		super.eTag(indent, ns, name, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter attr(String name, Object value) throws IOException {
		super.attr(name, value);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter attr(String ns, String name, Object value) throws IOException {
		super.attr(ns, name, value);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter attr(String ns, String name, Object value, boolean needsEncoding) throws IOException {
		super.attr(ns, name, value, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter attr(String name, Object value, boolean needsEncoding) throws IOException {
		super.attr(null, name, value, needsEncoding);
		return this;
	}

	@Override /* XmlSerializerWriter */
	public HtmlSerializerWriter oAttr(String ns, String name) throws IOException {
		super.oAttr(ns, name);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter s() throws IOException {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter nl() throws IOException {
		super.nl();
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public HtmlSerializerWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}

}
