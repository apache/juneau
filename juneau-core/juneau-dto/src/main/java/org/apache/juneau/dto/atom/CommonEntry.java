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

import static org.apache.juneau.dto.atom.Utils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Parent class of {@link Entry}, {@link Feed}, and {@link Source}.
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-dto.jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class="doclink" href="package-summary.html#TOC">package-summary.html</a>
 * </ul>
 */
@FluentSetters
public class CommonEntry extends Common {

	private Person[] authors;
	private Category[] categories;
	private Person[] contributors;
	private Id id;
	private Link[] links;
	private Text rights;
	private Text title;
	private Calendar updated;


	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this object.
	 * @param title The title of this object.
	 * @param updated The updated timestamp of this object.
	 */
	public CommonEntry(Id id, Text title, Calendar updated) {
		setId(id).setTitle(title).setUpdated(updated);
	}

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this object.
	 * @param title The title of this object.
	 * @param updated The updated timestamp of this object.
	 */
	public CommonEntry(String id, String title, String updated) {
		setId(id).setTitle(title).setUpdated(updated);
	}

	/** Bean constructor. */
	public CommonEntry() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>authors</property>.
	 *
	 * <p>
	 * The list of authors for this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED, childName="author")
	public Person[] getAuthors() {
		return authors;
	}

	/**
	 * Bean property setter:  <property>authors</property>.
	 *
	 * <p>
	 * The list of authors for this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setAuthors(Person...value) {
		this.authors = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>categories</property>.
	 *
	 * <p>
	 * The list of categories of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED, childName="category")
	public Category[] getCategories() {
		return categories;
	}

	/**
	 * Bean property setter:  <property>categories</property>.
	 *
	 * <p>
	 * The list of categories of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setCategories(Category...value) {
		this.categories = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>contributors</property>.
	 *
	 * <p>
	 * The list of contributors of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED, childName="contributor")
	public Person[] getContributors() {
		return contributors;
	}

	/**
	 * Bean property setter:  <property>contributors</property>.
	 *
	 * <p>
	 * The list of contributors of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setContributors(Person...value) {
		this.contributors = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * <p>
	 * The ID of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Id getId() {
		return id;
	}

	/**
	 * Bean property setter:  <property>id</property>.
	 *
	 * <p>
	 * The ID of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setId(Id value) {
		this.id = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>id</property>.
	 *
	 * <p>
	 * The ID of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@FluentSetter
	public CommonEntry setId(String value) {
		setId(new Id(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>links</property>.
	 *
	 * <p>
	 * The list of links of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED)
	public Link[] getLinks() {
		return links;
	}

	/**
	 * Bean property setter:  <property>links</property>.
	 *
	 * <p>
	 * The list of links of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setLinks(Link...value) {
		this.links = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>rights</property>.
	 *
	 * <p>
	 * The rights statement of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getRights() {
		return rights;
	}

	/**
	 * Bean property setter:  <property>rights</property>.
	 *
	 * <p>
	 * The rights statement of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setRights(Text value) {
		this.rights = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>rights</property>.
	 *
	 * <p>
	 * The rights statement of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@FluentSetter
	public CommonEntry setRights(String value) {
		setRights(new Text().setText(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setTitle(Text value) {
		this.title = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>title</property>.
	 *
	 * <p>
	 * The title of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@FluentSetter
	public CommonEntry setTitle(String value) {
		setTitle(new Text().setText(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>updated</property>.
	 *
	 * <p>
	 * The update timestamp of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Calendar getUpdated() {
		return updated;
	}

	/**
	 * Bean property setter:  <property>updated</property>.
	 *
	 * <p>
	 * The update timestamp of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@FluentSetter
	public CommonEntry setUpdated(Calendar value) {
		this.updated = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>updated</property>.
	 *
	 * <p>
	 * The update timestamp of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@FluentSetter
	public CommonEntry setUpdated(String value) {
		setUpdated(parseDateTime(value));
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public CommonEntry setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public CommonEntry setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}
