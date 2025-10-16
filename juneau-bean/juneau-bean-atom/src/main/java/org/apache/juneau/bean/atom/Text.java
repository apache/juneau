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
package org.apache.juneau.bean.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import org.apache.juneau.xml.annotation.*;

/**
 * Represents human-readable text in an Atom document.
 *
 * <p>
 * Text constructs are used throughout Atom for elements that contain human-readable text
 * such as titles, summaries, rights statements, and subtitles. They support three content types:
 *
 * <ul class='spaced-list'>
 * 	<li><c>"text"</c> - Plain text with no markup (default)
 * 	<li><c>"html"</c> - HTML markup, entity-escaped
 * 	<li><c>"xhtml"</c> - Well-formed XHTML in a div container
 * </ul>
 *
 * <p>
 * Text constructs are the base class for {@link Content} and are used directly for:
 * <ul class='spaced-list'>
 * 	<li><c>atom:title</c> - Entry and feed titles
 * 	<li><c>atom:subtitle</c> - Feed subtitles
 * 	<li><c>atom:summary</c> - Entry summaries
 * 	<li><c>atom:rights</c> - Copyright and rights statements
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomTextConstruct = atomPlainTextConstruct | atomXHTMLTextConstruct
 *
 * 	atomPlainTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "text" | "html" }?,
 * 		text
 *
 * 	atomXHTMLTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "xhtml" },
 * 		xhtmlDiv
 *
 * 	xhtmlDiv = element xhtml:div {
 * 		(attribute * { text }
 * 		| text
 * 		| anyXHTML)*
 * 	}
 * </p>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Plain text</jc>
 * 	Text <jv>t1</jv> = <jk>new</jk> Text(<js>"text"</js>)
 * 		.setText(<js>"Plain text title"</js>);
 *
 * 	<jc>// HTML (entity-escaped)</jc>
 * 	Text <jv>t2</jv> = <jk>new</jk> Text(<js>"html"</js>)
 * 		.setText(<js>"Title with &lt;em&gt;emphasis&lt;/em&gt;"</js>);
 *
 * 	<jc>// XHTML</jc>
 * 	Text <jv>t3</jv> = <jk>new</jk> Text(<js>"xhtml"</js>)
 * 		.setText(<js>"&lt;div xmlns='http://www.w3.org/1999/xhtml'&gt;&lt;p&gt;XHTML title&lt;/p&gt;&lt;/div&gt;"</js>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomTextConstruct</c> in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-3.1">RFC 4287 - Section 3.1</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
public class Text extends Common {

	private String type;
	private String text;  // NOSONAR - Intentional naming.

	/** Bean constructor. */
	public Text() {}

	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 */
	public Text(String type) {
		setType(type);
	}
	/**
	 * Bean property getter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=XMLTEXT)
	public String getText() {
		return text;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of this content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	@Override /* Overridden from Common */
	public Text setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Text setLang(String value) {
		super.setLang(value);
		return this;
	}
	/**
	 * Bean property setter:  <property>text</property>.
	 *
	 * <p>
	 * The content of this content.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Text setText(String value) {
		this.text = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of this content.
	 *
	 * <p>
	 * Must be one of the following:
	 * <ul>
	 * 	<li><js>"text"</js>
	 * 	<li><js>"html"</js>
	 * 	<li><js>"xhtml"</js>
	 * 	<li><jk>null</jk> (defaults to <js>"text"</js>)
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Text setType(String value) {
		this.type = value;
		return this;
	}
}