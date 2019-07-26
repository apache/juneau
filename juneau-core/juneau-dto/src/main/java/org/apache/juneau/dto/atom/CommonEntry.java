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

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Parent class of {@link Entry}, {@link Feed}, and {@link Source}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
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
	 * Returns the list of authors for this object.
	 *
	 * @return The list of authors for this object.
	 */
	@Xml(format=COLLAPSED, childName="author")
	public Person[] getAuthors() {
		return authors;
	}

	/**
	 * Sets the list of authors for this object.
	 *
	 * @param authors The list of authors for this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("authors")
	public CommonEntry authors(Person...authors) {
		this.authors = authors;
		return this;
	}

	/**
	 * Returns the list of categories of this object.
	 *
	 * @return The list of categories of this object.
	 */
	@Xml(format=COLLAPSED, childName="category")
	public Category[] getCategories() {
		return categories;
	}

	/**
	 * Sets the list of categories of this object.
	 *
	 * @param categories The list of categories of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("categories")
	public CommonEntry categories(Category...categories) {
		this.categories = categories;
		return this;
	}

	/**
	 * Returns the list of contributors of this object.
	 *
	 * @return The list of contributors of this object.
	 */
	@Xml(format=COLLAPSED, childName="contributor")
	public Person[] getContributors() {
		return contributors;
	}

	/**
	 * Sets the list of contributors of this object.
	 *
	 * @param contributors The list of contributors of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("contributors")
	public CommonEntry contributors(Person...contributors) {
		this.contributors = contributors;
		return this;
	}

	/**
	 * Returns the ID of this object.
	 *
	 * @return The ID of this object.
	 */
	public Id getId() {
		return id;
	}

	/**
	 * Sets the ID of this object.
	 *
	 * @param id The ID of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("id")
	public CommonEntry id(Id id) {
		this.id = id;
		return this;
	}

	/**
	 * Sets the ID of this object.
	 *
	 * @param id The ID of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry id(String id) {
		this.id = new Id(id);
		return this;
	}

	/**
	 * Returns the list of links of this object.
	 *
	 * @return The list of links of this object.
	 */
	@Xml(format=COLLAPSED)
	public Link[] getLinks() {
		return links;
	}

	/**
	 * Sets the list of links of this object.
	 *
	 * @param links The list of links of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("links")
	public CommonEntry links(Link...links) {
		this.links = links;
		return this;
	}

	/**
	 * Returns the rights statement of this object.
	 *
	 * @return The rights statement of this object.
	 */
	public Text getRights() {
		return rights;
	}

	/**
	 * Sets the rights statement of this object.
	 *
	 * @param rights The rights statement of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("rights")
	public CommonEntry rights(Text rights) {
		this.rights = rights;
		return this;
	}

	/**
	 * Sets the rights statement of this object.
	 *
	 * @param rights The rights statement of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry rights(String rights) {
		this.rights = new Text().text(rights);
		return this;
	}

	/**
	 * Returns the title of this object.
	 *
	 * @return The title of this object.
	 */
	public Text getTitle() {
		return title;
	}

	/**
	 * Sets the title of this object.
	 *
	 * @param title The title of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("title")
	public CommonEntry title(Text title) {
		this.title = title;
		return this;
	}

	/**
	 * Sets the title of this object.
	 *
	 * @param title The title of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry title(String title) {
		this.title = new Text().text(title);
		return this;
	}

	/**
	 * Returns the update timestamp of this object.
	 *
	 * @return The update timestamp of this object.
	 */
	public Calendar getUpdated() {
		return updated;
	}

	/**
	 * Sets the update timestamp of this object.
	 *
	 * @param updated The update timestamp of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("updated")
	public CommonEntry updated(Calendar updated) {
		this.updated = updated;
		return this;
	}

	/**
	 * Sets the update timestamp of this object.
	 *
	 * @param updated The update timestamp of this object in ISO8601 format.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("updated")
	public CommonEntry updated(String updated) {
		this.updated = parseDateTime(updated);
		return this;
	}
}
