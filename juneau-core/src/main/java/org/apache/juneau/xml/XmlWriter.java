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
package org.apache.juneau.xml;

import java.io.*;
import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Specialized writer for serializing XML.
 *
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>This class is not intended for external use.
 * </ul>
 */
public class XmlWriter extends SerializerWriter {

	private String defaultNsPrefix;
	private boolean enableNs;

	/**
	 * Constructor.
	 *
	 * @param out The wrapped writer.
	 * @param useWhitespace If <jk>true</jk> XML elements will be indented.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param quoteChar The quote character to use for attributes.  Should be <js>'\''</js> or <js>'"'</js>.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 * @param enableNs Flag to indicate if XML namespaces are enabled.
	 * @param defaultNamespace The default namespace if XML namespaces are enabled.
	 */
	public XmlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar,
			UriResolver uriResolver, boolean enableNs, Namespace defaultNamespace) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
		this.enableNs = enableNs;
		this.defaultNsPrefix = defaultNamespace == null ? null : defaultNamespace.name;
	}

	/**
	 * Writes an opening tag to the output:  <code><xt>&lt;ns:name</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(String ns, String name, boolean needsEncoding) throws IOException {
		append('<');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			append(ns).append(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		return this;
	}

	/**
	 * Shortcut for <code>oTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(String ns, String name) throws IOException {
		return oTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(String name) throws IOException {
		return oTag(null, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(ns, name, needsEncoding);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		return i(indent).oTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(int indent, String ns, String name) throws IOException {
		return i(indent).oTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oTag(int indent, String name) throws IOException {
		return i(indent).oTag(null, name, false);
	}

	/**
	 * Closes a tag.
	 * Shortcut for <code>append(<js>'>'</js>);</code>
	 *
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public XmlWriter cTag() throws IOException {
		append('>');
		return this;
	}

	/**
	 * Closes an empty tag.
	 * Shortcut for <code>append(<js>'/'</js>).append(<js>'>'</js>);</code>
	 *
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public XmlWriter ceTag() throws IOException {
		append('/').append('>');
		return this;
	}

	/**
	 * Writes a closed tag to the output:  <code><xt>&lt;ns:name/&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(String ns, String name, boolean needsEncoding) throws IOException {
		append('<');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			append(ns).append(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		return append('/').append('>');
	}

	/**
	 * Shortcut for <code>tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(String ns, String name) throws IOException {
		return tag(ns, name, false);
	}

	/**
	 * Shortcut for <code>tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(String name) throws IOException {
		return tag(null, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(int indent, String name) throws IOException {
		return i(indent).tag(name);
	}

	/**
	 * Shortcut for <code>i(indent).tag(ns, name, needsEncoding);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		return i(indent).tag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter tag(int indent, String ns, String name) throws IOException {
		return i(indent).tag(ns, name);
	}


	/**
	 * Writes a start tag to the output:  <code><xt>&lt;ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(String ns, String name, boolean needsEncoding) throws IOException {
		return oTag(ns, name, needsEncoding).append('>');
	}

	/**
	 * Shortcut for <code>sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(String ns, String name) throws IOException {
		return sTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(String name) throws IOException {
		return sTag(null, name);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(ns, name, needsEncoding);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		return i(indent).sTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(int indent, String ns, String name) throws IOException {
		return i(indent).sTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter sTag(int indent, String name) throws IOException {
		return i(indent).sTag(null, name, false);
	}


	/**
	 * Writes an end tag to the output:  <code><xt>&lt;/ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(String ns, String name, boolean needsEncoding) throws IOException {
		append('<').append('/');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			append(ns).append(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		return append('>');
	}

	/**
	 * Shortcut for <code>eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(String ns, String name) throws IOException {
		return eTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(String name) throws IOException {
		return eTag(null, name);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(ns, name, needsEncoding);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(int indent, String ns, String name, boolean needsEncoding) throws IOException {
		return i(indent).eTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(int indent, String ns, String name) throws IOException {
		return i(indent).eTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter eTag(int indent, String name) throws IOException {
		return i(indent).eTag(name);
	}

	/**
	 * Writes an attribute to the output:  <code><xa>ns:name</xa>=<xs>'value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @param valNeedsEncoding If <jk>true</jk>, attribute name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attr(String ns, String name, Object value, boolean valNeedsEncoding) throws IOException {
		return oAttr(ns, name).q().attrValue(value, valNeedsEncoding).q();
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @param valNeedsEncoding If <jk>true</jk>, attribute name will be encoded.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attr(String name, Object value, boolean valNeedsEncoding) throws IOException {
		return attr(null, name, value, valNeedsEncoding);
	}

	/**
	 * Shortcut for <code>attr(ns, name, value, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attr(String ns, String name, Object value) throws IOException {
		return oAttr(ns, name).q().attrValue(value, false).q();
	}

	/**
	 * Same as {@link #attr(String, String, Object)}, except pass in a {@link Namespace} object for the namespace.
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attr(Namespace ns, String name, Object value) throws IOException {
		return oAttr(ns == null ? null : ns.name, name).q().attrValue(value, false).q();
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attr(String name, Object value) throws IOException {
		return attr((String)null, name, value);
	}


	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oAttr(String ns, String name) throws IOException {
		append(' ');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			append(ns).append(':');
		append(name).append('=');
		return this;
	}

	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter oAttr(Namespace ns, String name) throws IOException {
		return oAttr(ns == null ? null : ns.name, name);
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <code>toString()</code>
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attrUri(Namespace ns, String name, Object value) throws IOException {
		return attr(ns, name, uriResolver.resolve(value));
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <code>toString()</code>
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter attrUri(String ns, String name, Object value) throws IOException {
		return attr(ns, name, uriResolver.resolve(value), true);
	}

	/**
	 * Shortcut for calling <code>text(o, <jk>false</jk>);</code>
	 *
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public XmlWriter text(Object o) throws IOException {
		text(o, false);
		return this;
	}

	/**
	 * Serializes and encodes the specified object as valid XML text.
	 *
	 * @param o The object being serialized.
	 * @param preserveWhitespace If <jk>true</jk>, then we're serializing {@link XmlFormat#MIXED_PWS} or
	 * {@link XmlFormat#TEXT_PWS} content.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public XmlWriter text(Object o, boolean preserveWhitespace) throws IOException {
		XmlUtils.encodeText(this, o, trimStrings, preserveWhitespace);
		return this;
	}

	/**
	 * Same as {@link #text(Object)} but treats the value as a URL to resolved then serialized.
	 *
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public XmlWriter textUri(Object o) throws IOException {
		text(uriResolver.resolve(o), false);
		return this;
	}

	private XmlWriter attrValue(Object o, boolean needsEncoding) throws IOException {
		if (needsEncoding)
			XmlUtils.encodeAttrValue(out, o, this.trimStrings);
		else if (o instanceof URI || o instanceof URL)
			append(uriResolver.resolve(o));
		else
			append(o);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter cr(int depth) throws IOException {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter cre(int depth) throws IOException {
		super.cre(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter appendln(int indent, String text) throws IOException {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter appendln(String text) throws IOException {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(int indent, String text) throws IOException {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(int indent, char c) throws IOException {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter s() throws IOException {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter q() throws IOException {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter i(int indent) throws IOException {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter ie(int indent) throws IOException {
		super.ie(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter nl(int indent) throws IOException {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(Object text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(String text) throws IOException {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(char c) throws IOException {
		out.write(c);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return out.toString();
	}
}
