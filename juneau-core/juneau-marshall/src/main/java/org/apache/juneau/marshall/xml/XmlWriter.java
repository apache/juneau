/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.xml;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Specialized writer for serializing XML.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlSupport">XML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"resource", // Writer resource managed by calling code
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class XmlWriter<SELF extends XmlWriter<SELF>> extends SerializerWriter<SELF> {

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
	@SuppressWarnings({
		"java:S107" // Constructor requires 8 parameters for XML writer configuration
	})
	protected XmlWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar, UriResolver uriResolver, boolean enableNs, Namespace defaultNamespace) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
		this.enableNs = enableNs;
		this.defaultNsPrefix = defaultNamespace == null ? null : defaultNamespace.name;
	}

	/**
	 * Copy constructor.
	 *
	 * @param w Writer being copied.
	 */
	protected XmlWriter(XmlWriter<?> w) {
		super(w);
		this.enableNs = w.enableNs;
		this.defaultNsPrefix = w.defaultNsPrefix;
	}

	/**
	 * Same as {@link #attr(String, String, Object)}, except pass in a {@link Namespace} object for the namespace.
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public SELF attr(Namespace ns, String name, Object value) {
		return oAttr(ns == null ? null : ns.name, name).q().attrValue(value, false).q();
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object.
	 */
	public SELF attr(String name, Object value) {
		return attr((String)null, name, value);
	}

	/**
	 * Shortcut for <code>attr(<jk>null</jk>, name, value, <jk>false</jk>);</code>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @param valNeedsEncoding If <jk>true</jk>, attribute name will be encoded.
	 * @return This object.
	 */
	public SELF attr(String name, Object value, boolean valNeedsEncoding) {
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
	public SELF attr(String ns, String name, Object value) {
		return oAttr(ns, name).q().attrValue(value, false).q();
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
	public SELF attr(String ns, String name, Object value, boolean valNeedsEncoding) {
		return oAttr(ns, name).q().attrValue(value, valNeedsEncoding).q();
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <c>toString()</c>
	 * @return This object.
	 */
	public SELF attrUri(Namespace ns, String name, Object value) {
		return attr(ns, name, uriResolver.resolve(value));
	}

	/**
	 * Append an attribute with a URI value.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.  Can be any object whose <c>toString()</c> method returns a URI.
	 * @return This object.
	 */
	public SELF attrUri(String name, Object value) {
		return attrUri((String)null, name, value);
	}

	/**
	 * Writes an attribute with a URI value to the output:  <code><xa>ns:name</xa>=<xs>'uri-value'</xs></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @param value The attribute value, convertible to a URI via <c>toString()</c>
	 * @return This object.
	 */
	public SELF attrUri(String ns, String name, Object value) {
		return attr(ns, name, uriResolver.resolve(value), true);
	}

	/**
	 * Closes an empty tag.
	 *
	 * <p>
	 * Shortcut for <code>append(<js>'/'</js>).append(<js>'-&gt;'</js>);</code>
	 *
	 * @return This object.
	 */
	public SELF ceTag() {
		w('/').w('>');
		return self();
	}

	/**
	 * Closes a tag.
	 *
	 * <p>
	 * Shortcut for <code>append(<js>'-&gt;'</js>);</code>
	 *
	 * @return This object.
	 */
	public SELF cTag() {
		w('>');
		return self();
	}

	/**
	 * Shortcut for <code>i(indent).eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF eTag(int indent, String name) {
		return i(indent).eTag(name);
	}

	/**
	 * Shortcut for <code>i(indent).eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF eTag(int indent, String ns, String name) {
		return i(indent).eTag(ns, name, false);
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
	public SELF eTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).eTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>eTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF eTag(String name) {
		return eTag(null, name);
	}

	/**
	 * Shortcut for <code>eTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF eTag(String ns, String name) {
		return eTag(ns, name, false);
	}

	/**
	 * Writes an end tag to the output:  <code><xt>&lt;/ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public SELF eTag(String ns, String name, boolean needsEncoding) {
		w('<').w('/');
		if (enableNs && nn(ns) && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		w('>');
		return self();
	}

	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object.
	 */
	public SELF oAttr(Namespace ns, String name) {
		return oAttr(ns == null ? null : ns.name, name);
	}

	/**
	 * Writes an open-ended attribute to the output:  <code><xa>ns:name</xa>=</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The attribute name.
	 * @return This object.
	 */
	public SELF oAttr(String ns, String name) {
		w(' ');
		if (enableNs && nn(ns) && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		w(name).w('=');
		return self();
	}

	/**
	 * Shortcut for <code>i(indent).oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF oTag(int indent, String name) {
		return i(indent).oTag(null, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).oTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF oTag(int indent, String ns, String name) {
		return i(indent).oTag(ns, name, false);
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
	public SELF oTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).oTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>oTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF oTag(String name) {
		return oTag(null, name, false);
	}

	/**
	 * Shortcut for <code>oTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF oTag(String ns, String name) {
		return oTag(ns, name, false);
	}

	/**
	 * Writes an opening tag to the output:  <code><xt>&lt;ns:name</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public SELF oTag(String ns, String name, boolean needsEncoding) {
		w('<');
		if (enableNs && nn(ns) && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			append(name);
		return self();
	}

	/**
	 * Shortcut for <code>i(indent).sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF sTag(int indent, String name) {
		return i(indent).sTag(null, name, false);
	}

	/**
	 * Shortcut for <code>i(indent).sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF sTag(int indent, String ns, String name) {
		return i(indent).sTag(ns, name, false);
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
	public SELF sTag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).sTag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>sTag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF sTag(String name) {
		return sTag(null, name);
	}

	/**
	 * Shortcut for <code>sTag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF sTag(String ns, String name) {
		return sTag(ns, name, false);
	}

	/**
	 * Writes a start tag to the output:  <code><xt>&lt;ns:name&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public SELF sTag(String ns, String name, boolean needsEncoding) {
		oTag(ns, name, needsEncoding).w('>');
		return self();
	}

	/**
	 * Shortcut for <code>i(indent).tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF tag(int indent, String name) {
		return i(indent).tag(name);
	}

	/**
	 * Shortcut for <code>i(indent).tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param indent The number of prefix tabs to add.
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF tag(int indent, String ns, String name) {
		return i(indent).tag(ns, name);
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
	public SELF tag(int indent, String ns, String name, boolean needsEncoding) {
		return i(indent).tag(ns, name, needsEncoding);
	}

	/**
	 * Shortcut for <code>tag(<jk>null</jk>, name, <jk>false</jk>);</code>
	 *
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF tag(String name) {
		return tag(null, name, false);
	}

	/**
	 * Shortcut for <code>tag(ns, name, <jk>false</jk>);</code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @return This object.
	 */
	public SELF tag(String ns, String name) {
		return tag(ns, name, false);
	}

	/**
	 * Writes a closed tag to the output:  <code><xt>&lt;ns:name/&gt;</xt></code>
	 *
	 * @param ns The namespace.  Can be <jk>null</jk>.
	 * @param name The element name.
	 * @param needsEncoding If <jk>true</jk>, element name will be encoded.
	 * @return This object.
	 */
	public SELF tag(String ns, String name, boolean needsEncoding) {
		w('<');
		if (enableNs && nn(ns) && ! (ns.isEmpty() || ns.equals(defaultNsPrefix)))
			w(ns).w(':');
		if (needsEncoding)
			XmlUtils.encodeElementName(out, name);
		else
			w(name);
		w('/').w('>');
		return self();
	}

	/**
	 * Shortcut for calling <code>text(o, <jk>false</jk>);</code>
	 *
	 * @param value The object being serialized.
	 * @return This object.
	 */
	public SELF text(Object value) {
		text(value, false);
		return self();
	}

	/**
	 * Serializes and encodes the specified object as valid XML text.
	 *
	 * @param value The object being serialized.
	 * @param preserveWhitespace
	 * 	If <jk>true</jk>, then we're serializing {@link XmlFormat#MIXED_PWS} or {@link XmlFormat#TEXT_PWS} content.
	 * @return This object.
	 */
	public SELF text(Object value, boolean preserveWhitespace) {
		XmlUtils.encodeText(this, value, trimStrings, preserveWhitespace);
		return self();
	}

	/**
	 * Same as {@link #text(Object)} but treats the value as a URL to resolved then serialized.
	 *
	 * @param value The object being serialized.
	 * @return This object.
	 */
	public SELF textUri(Object value) {
		text(uriResolver.resolve(value), false);
		return self();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return out.toString();
	}

	protected SELF attrValue(Object value, boolean needsEncoding) {
		if (needsEncoding)
			XmlUtils.encodeAttrValue(out, value, this.trimStrings);
		else if (value instanceof URI || value instanceof URL)
			append(uriResolver.resolve(value));
		else
			append(value);
		return self();
	}
}