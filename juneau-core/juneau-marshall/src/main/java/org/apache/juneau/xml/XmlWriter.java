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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a>
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
	 * @return This object.
	 */
	public XmlWriter oTag(String ns, String name, boolean needsEncoding) {
		w('<');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
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
	 * @return This object.
	 */
	public XmlWriter oTag(String ns, String name) {
		return oTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter oTag(String name) {
		return oTag(null, name, false);
	}

	/**
	 * Shortcut for <c>i(indent).oTag(ns, name, needsEncoding);</c>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter oTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).oTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter oTag(int indent, String ns, String name) {
		return i(indent).oTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter oTag(int indent, String name) {
		return i(indent).oTag(null, name, false);
	}

	/**
	 * Closes a tag.
	 *
	 * <p>
	 * Shortcut for <code>append(<js>'-&gt;'</js>);</code>
	 *
	 * @return This object.
	 */
	public XmlWriter cTag() {
		w('>');
		return this;
	}

	/**
	 * Closes an empty tag.
	 *
	 * <p>
	 * Shortcut for <code>append(<js>'/'</js>).append(<js>'-&gt;'</js>);</code>
	 *
	 * @return This object.
	 */
	public XmlWriter ceTag() {
		w('/').w('>');
		return this;
	}

	/**
	 * Writes a closed tag to the output:  <code><xt>&lt;ns:name/&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter tag(String ns, String name, boolean needsEncoding) {
		w('<');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			w(name);
		w('/').w('>');
		return this;
	}

	/**
	 * Shortcut for <code>tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter tag(String ns, String name) {
		return tag(ns, name, false);
	}

	/**
	 * Shortcut for <code>tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter tag(String name) {
		return tag(null, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter tag(int indent, String name) {
		return i(indent).tag(name);
	}

	/**
	 * Shortcut for <c>i(indent).tag(ns, name, needsEncoding);</c>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter tag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).tag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter tag(int indent, String ns, String name) {
		return i(indent).tag(ns, name);
	}


	/**
	 * Writes a start tag to the output:  <code><xt>&lt;ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter sTag(String ns, String name, boolean needsEncoding) {
		oTag(ns, name, needsEncoding).w('>');
		return this;
	}

	/**
	 * Shortcut for <code>sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter sTag(String ns, String name) {
		return sTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter sTag(String name) {
		return sTag(null, name);
	}

	/**
	 * Shortcut for <c>i(indent).sTag(ns, name, needsEncoding);</c>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter sTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).sTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter sTag(int indent, String ns, String name) {
		return i(indent).sTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter sTag(int indent, String name) {
		return i(indent).sTag(null, name, false);
	}


	/**
	 * Writes an end tag to the output:  <code><xt>&lt;/ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter eTag(String ns, String name, boolean needsEncoding) {
		w('<').w('/');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		w('>');
		return this;
	}

	/**
	 * Shortcut for <code>eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter eTag(String ns, String name) {
		return eTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter eTag(String name) {
		return eTag(null, name);
	}

	/**
	 * Shortcut for <c>i(indent).eTag(ns, name, needsEncoding);</c>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public XmlWriter eTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).eTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter eTag(int indent, String ns, String name) {
		return i(indent).eTag(ns, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public XmlWriter eTag(int indent, String name) {
		return i(indent).eTag(name);
	}

	/**
	 * Writes an attribute to the output:  <code><xa>ns:name</xa>=<xs>'value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @param valNeedsEncoding If <jk>true</jk>, attribute name will be encoded.
	 * @return This object.
	 */
	public XmlWriter attr(String ns, String name, Object value, boolean valNeedsEncoding) {
		return oAttr(ns, name).q().attrValue(value, valNeedsEncoding).q();
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @param valNeedsEncoding If <jk>true</jk>, attribute name will be encoded.
	 * @return This object.
	 */
	public XmlWriter attr(String name, Object value, boolean valNeedsEncoding) {
		return attr(null, name, value, valNeedsEncoding);
	}

	/**
	 * Shortcut for <code>attr(ns, name, value, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public XmlWriter attr(String ns, String name, Object value) {
		return oAttr(ns, name).q().attrValue(value, false).q();
	}

	/**
	 * Same as {@link #attr(String, String, Object)}, except pass in a {@link Namespace} object for the namespace.
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public XmlWriter attr(Namespace ns, String name, Object value) {
		return oAttr(ns == null ? null : ns.name, name).q().attrValue(value, false).q();
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public XmlWriter attr(String name, Object value) {
		return attr((String)null, name, value);
	}


	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object.
	 */
	public XmlWriter oAttr(String ns, String name) {
		w(' ');
		if (enableNs && ns != null && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		w(name).w('=');
		return this;
	}

	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object.
	 */
	public XmlWriter oAttr(Namespace ns, String name) {
		return oAttr(ns == null ? null : ns.name, name);
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <c>toString()</c>
	 * @return This object.
	 */
	public XmlWriter attrUri(Namespace ns, String name, Object value) {
		return attr(ns, name, uriResolver.resolve(value));
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <c>toString()</c>
	 * @return This object.
	 */
	public XmlWriter attrUri(String ns, String name, Object value) {
		return attr(ns, name, uriResolver.resolve(value), true);
	}

	/**
	 * Append an attribute with a URI value.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.  Can be any object whose <c>toString()</c> method returns a URI.
	 * @return This object.
	 */
	public XmlWriter attrUri(String name, Object value) {
		return attrUri((String)null, name, value);
	}

	/**
	 * Shortcut for calling <code>text(o, <jk>false</jk>);</code>
	 *
	 * @param value The object being serialized.
	 * @return This object.
	 */
	public XmlWriter text(Object value) {
		text(value, false);
		return this;
	}

	/**
	 * Serializes and encodes the specified object as valid XML text.
	 *
	 * @param value The object being serialized.
	 * @param preserveWhitespace
	 * 	If <jk>true</jk>, then we're serializing {@link XmlFormat#MIXED_PWS} or {@link XmlFormat#TEXT_PWS} content.
	 * @return This object.
	 */
	public XmlWriter text(Object value, boolean preserveWhitespace) {
		XmlUtils.encodeText(this, value, trimStrings, preserveWhitespace);
		return this;
	}

	/**
	 * Same as {@link #text(Object)} but treats the value as a URL to resolved then serialized.
	 *
	 * @param value The object being serialized.
	 * @return This object.
	 */
	public XmlWriter textUri(Object value) {
		text(uriResolver.resolve(value), false);
		return this;
	}

	private XmlWriter attrValue(Object value, boolean needsEncoding) {
		if (needsEncoding)
			XmlUtils.encodeAttrValue(out, value, this.trimStrings);
		else if (value instanceof URI || value instanceof URL)
			append(uriResolver.resolve(value));
		else
			append(value);
		return this;
	}

	// <FluentSetters>

	@Override /* SerializerWriter */
	public XmlWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter s() {
		super.s();
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter q() {
		super.q();
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter ie(int indent) {
		super.ie(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter append(char c) {
		try {
			out.write(c);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter w(char c) {
		super.w(c);
		return this;
	}

	@Override /* SerializerWriter */
	public XmlWriter w(String s) {
		super.w(s);
		return this;
	}

	// </FluentSetters>

	@Override /* Object */
	public String toString() {
		return out.toString();
	}
}
