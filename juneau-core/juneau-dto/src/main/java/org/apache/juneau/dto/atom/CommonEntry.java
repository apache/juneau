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

import org.apache.juneau.xml.annotation.*;

/**
 * Parent class of {@link Entry}, {@link Feed}, and {@link Source}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
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
		id(id).title(title).updated(updated);
	}

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this object.
	 * @param title The title of this object.
	 * @param updated The updated timestamp of this object.
	 */
	public CommonEntry(String id, String title, String updated) {
		id(id).title(title).updated(updated);
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
	 */
	public void setAuthors(Person[] value) {
		this.authors = value;
	}

	/**
	 * Bean property fluent getter:  <property>authors</property>.
	 *
	 * <p>
	 * The list of authors for this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Person[]> authors() {
		return Optional.ofNullable(authors);
	}

	/**
	 * Bean property fluent setter:  <property>authors</property>.
	 *
	 * <p>
	 * The list of authors for this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public CommonEntry authors(Person...value) {
		setAuthors(value);
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
	 */
	public void setCategories(Category[] value) {
		this.categories = value;
	}

	/**
	 * Bean property fluent getter:  <property>categories</property>.
	 *
	 * <p>
	 * The list of categories of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Category[]> categories() {
		return Optional.ofNullable(categories);
	}

	/**
	 * Bean property fluent setter:  <property>categories</property>.
	 *
	 * <p>
	 * The list of categories of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public CommonEntry categories(Category...value) {
		setCategories(value);
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
	 */
	public void setContributors(Person[] value) {
		this.contributors = value;
	}

	/**
	 * Bean property fluent getter:  <property>contributors</property>.
	 *
	 * <p>
	 * The list of contributors of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Person[]> contributors() {
		return Optional.ofNullable(contributors);
	}

	/**
	 * Bean property fluent setter:  <property>contributors</property>.
	 *
	 * <p>
	 * The list of contributors of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public CommonEntry contributors(Person...value) {
		setContributors(value);
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
	 */
	public void setId(Id value) {
		this.id = value;
	}

	/**
	 * Bean property fluent getter:  <property>id</property>.
	 *
	 * <p>
	 * The ID of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Id> id() {
		return Optional.ofNullable(id);
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
	public CommonEntry id(Id value) {
		setId(value);
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
	public CommonEntry id(String value) {
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
	 */
	public void setLinks(Link[] value) {
		this.links = value;
	}

	/**
	 * Bean property fluent getter:  <property>links</property>.
	 *
	 * <p>
	 * The list of links of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Link[]> links() {
		return Optional.ofNullable(links);
	}

	/**
	 * Bean property fluent setter:  <property>links</property>.
	 *
	 * <p>
	 * The list of links of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public CommonEntry links(Link...value) {
		setLinks(value);
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
	 */
	public void setRights(Text value) {
		this.rights = value;
	}

	/**
	 * Bean property fluent getter:  <property>rights</property>.
	 *
	 * <p>
	 * The rights statement of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Text> rights() {
		return Optional.ofNullable(rights);
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
	public CommonEntry rights(Text value) {
		setRights(value);
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
	public CommonEntry rights(String value) {
		setRights(new Text().text(value));
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
	 */
	public void setTitle(Text value) {
		this.title = value;
	}

	/**
	 * Bean property fluent getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Text> title() {
		return Optional.ofNullable(title);
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
	public CommonEntry title(Text value) {
		setTitle(value);
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
	public CommonEntry title(String value) {
		setTitle(new Text().text(value));
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
	 */
	public void setUpdated(Calendar value) {
		this.updated = value;
	}

	/**
	 * Bean property fluent getter:  <property>updated</property>.
	 *
	 * <p>
	 * The update timestamp of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Calendar> updated() {
		return Optional.ofNullable(updated);
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
	public CommonEntry updated(Calendar value) {
		setUpdated(value);
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
	public CommonEntry updated(String value) {
		setUpdated(parseDateTime(value));
		return this;
	}
}
