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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomLink</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
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
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="link")
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
		rel(rel).type(type).href(href);
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
	 */
	public void setHref(String value) {
		this.href = value;
	}

	/**
	 * Bean property fluent getter:  <property>href</property>.
	 *
	 * <p>
	 * The href of the target of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> href() {
		return Optional.ofNullable(href);
	}

	/**
	 * Bean property fluent setter:  <property>href</property>.
	 *
	 * <p>
	 * The href of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link href(String value) {
		setHref(value);
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
	 */
	public void setRel(String value) {
		this.rel = value;
	}

	/**
	 * Bean property fluent getter:  <property>rel</property>.
	 *
	 * <p>
	 * The rel of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> rel() {
		return Optional.ofNullable(rel);
	}

	/**
	 * Bean property fluent setter:  <property>rel</property>.
	 *
	 * <p>
	 * The rel of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link rel(String value) {
		setRel(value);
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
	 */
	public void setType(String value) {
		this.type = value;
	}

	/**
	 * Bean property fluent getter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> type() {
		return Optional.ofNullable(type);
	}

	/**
	 * Bean property fluent setter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link type(String value) {
		setType(value);
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
	 */
	public void setHreflang(String value) {
		this.hreflang = value;
	}

	/**
	 * Bean property fluent getter:  <property>hreflang</property>.
	 *
	 * <p>
	 * The language of the target of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> hreflang() {
		return Optional.ofNullable(hreflang);
	}

	/**
	 * Bean property fluent setter:  <property>hreflang</property>.
	 *
	 * <p>
	 * The language of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link hreflang(String value) {
		setHreflang(value);
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
	 */
	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * Bean property fluent getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the target of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> title() {
		return Optional.ofNullable(title);
	}

	/**
	 * Bean property fluent setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link title(String value) {
		setTitle(value);
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
	 */
	public void setLength(Integer value) {
		this.length = value;
	}

	/**
	 * Bean property fluent getter:  <property>length</property>.
	 *
	 * <p>
	 * The length of the contents of the target of this link.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> length() {
		return Optional.ofNullable(length);
	}

	/**
	 * Bean property fluent setter:  <property>length</property>.
	 *
	 * <p>
	 * The length of the contents of the target of this link.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link length(Integer value) {
		setLength(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Common */
	public Link base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Link lang(String lang) {
		super.lang(lang);
		return this;
	}
}
