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
package org.apache.juneau.dto.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomLink</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomLink =
 * 		element atom:link {
 * 			atomCommonAttributes,
 * 			attribute href { atomUri },
 * 			attribute rel { atomNCName | atomUri }?,
 * 			attribute type { atomMediaType }?,
 * 			attribute hreflang { atomLanguageTag }?,
 * 			attribute title { text }?,
 * 			attribute length { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-dto.jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class="doclink" href="package-summary.html#TOC">package-summary.html</a>
 * </ul>
 */
@Bean(typeName="link")
@FluentSetters
public class Link extends Common {

	private String href;
	private String rel;
	private String type;
	private String hreflang;
	private String title;
	private Integer length;


	/**
	 * Normal constructor.
	 *
	 * @param rel The rel of the link.
	 * @param type The type of the link.
	 * @param href The URI of the link.
	 */
	public Link(String rel, String type, String href) {
		setRel(rel).setType(type).setHref(href);
	}

	/** Bean constructor. */
	public Link() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>href</property>.
	 *
	 * <p>
	 * The href of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getHref() {
		return href;
	}

	/**
	 * Bean property setter:  <property>href</property>.
	 *
	 * <p>
	 * The href of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setHref(String value) {
		this.href = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>rel</property>.
	 *
	 * <p>
	 * The rel of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getRel() {
		return rel;
	}

	/**
	 * Bean property setter:  <property>rel</property>.
	 *
	 * <p>
	 * The rel of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setRel(String value) {
		this.rel = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
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
	public Link setType(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>hreflang</property>.
	 *
	 * <p>
	 * The language of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getHreflang() {
		return hreflang;
	}

	/**
	 * Bean property setter:  <property>hreflang</property>.
	 *
	 * <p>
	 * The language of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setHreflang(String value) {
		this.hreflang = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setTitle(String value) {
		this.title = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>length</property>.
	 *
	 * <p>
	 * The length of the contents of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public Integer getLength() {
		return length;
	}

	/**
	 * Bean property setter:  <property>length</property>.
	 *
	 * <p>
	 * The length of the contents of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setLength(Integer value) {
		this.length = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Link setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Link setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}
