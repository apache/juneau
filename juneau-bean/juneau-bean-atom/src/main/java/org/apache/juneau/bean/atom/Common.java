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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Base class for all Atom elements, providing common attributes.
 *
 * <p>
 * This abstract class defines attributes that can appear on any Atom element. Per RFC 4287,
 * all Atom elements may have <c>xml:base</c> and <c>xml:lang</c> attributes for managing
 * URIs and language context.
 *
 * <p>
 * Common attributes:
 * <ul class='spaced-list'>
 * 	<li><b>xml:base</b> - Establishes a base URI for resolving relative references
 * 	<li><b>xml:lang</b> - Indicates the natural language of the element's content
 * </ul>
 *
 * <p>
 * This class is extended by all Atom bean classes ({@link Feed}, {@link Entry}, {@link Link},
 * {@link Person}, {@link Category}, {@link Text}, etc.).
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomCommonAttributes =
 * 		attribute xml:base { atomUri }?,
 * 		attribute xml:lang { atomLanguageTag }?,
 * 		undefinedAttribute*
 * </p>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Set base URI for relative link resolution</jc>
 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
 * 		.setBase(<js>"http://example.org/"</js>);
 *
 * 	<jc>// Set language</jc>
 * 	Text <jv>title</jv> = <jk>new</jk> Text(<js>"text"</js>)
 * 		.setText(<js>"My Feed"</js>)
 * 		.setLang(<js>"en"</js>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents <c>atomCommonAttributes</c> in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-2">RFC 4287 - Section 2</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
public abstract class Common {

	private URI base;
	private String lang;

	/**
	 * Bean property getter:  <property>base</property>.
	 *
	 * <p>
	 * Returns the base URI for resolving relative URI references (xml:base attribute).
	 *
	 * <p>
	 * This attribute, defined by XML Base, establishes a base URI for resolving any
	 * relative references within the scope of this element. This is particularly useful
	 * when aggregating content from multiple sources.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(prefix = "xml", format = ATTR)
	public URI getBase() { return base; }

	/**
	 * Bean property getter:  <property>lang</property>.
	 *
	 * <p>
	 * Returns the natural language of the element's content (xml:lang attribute).
	 *
	 * <p>
	 * The language tag should be a language identifier as defined by RFC 3066. This attribute
	 * is inherited by child elements, so it need only be specified on the highest-level
	 * element where it applies.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(prefix = "xml", format = ATTR)
	public String getLang() { return lang; }

	/**
	 * Bean property setter:  <property>base</property>.
	 *
	 * <p>
	 * Sets the base URI for resolving relative URI references (xml:base attribute).
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setBase(<js>"http://example.org/"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Common setBase(Object value) {
		this.base = toURI(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>lang</property>.
	 *
	 * <p>
	 * Sets the natural language of the element's content (xml:lang attribute).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Text <jv>title</jv> = <jk>new</jk> Text(<js>"text"</js>)
	 * 		.setText(<js>"Mon Blog"</js>)
	 * 		.setLang(<js>"fr"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property (e.g., "en", "fr", "de", "en-US").
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Common setLang(String value) {
		this.lang = value;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return XmlSerializer.DEFAULT_SQ.toString(this);
	}
}