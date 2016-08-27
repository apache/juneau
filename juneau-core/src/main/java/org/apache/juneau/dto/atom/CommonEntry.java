/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.dto.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Parent class of {@link Entry}, {@link Feed}, and {@link Source}
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings("hiding")
public class CommonEntry extends Common {

	private List<Person> authors;
	private List<Category> categories;
	private List<Person> contributors;
	private Id id;
	private List<Link> links;
	private Text rights;
	private Text title;
	private Calendar updated;


	/**
	 * Normal constructor.
	 * @param id The ID of this object.
	 * @param title The title of this object.
	 * @param updated The updated timestamp of this object.
	 */
	public CommonEntry(Id id, Text title, Calendar updated) {
		this.id = id;
		this.title = title;
		this.updated = updated;
	}

	/** Bean constructor. */
	public CommonEntry() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the list of authors for this object.
	 *
	 * @return The list of authors for this object.
	 */
	@Xml(format=COLLAPSED, childName="author")
	public List<Person> getAuthors() {
		return authors;
	}

	/**
	 * Sets the list of authors for this object.
	 *
	 * @param authors The list of authors for this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry setAuthors(List<Person> authors) {
		this.authors = authors;
		return this;
	}

	/**
	 * Adds one or more authors to the list of authors of this object.
	 *
	 * @param authors The author to add to the list.
	 * @return This object (for method chaining).
	 */
	public CommonEntry addAuthors(Person...authors) {
		if (this.authors == null)
			this.authors = new LinkedList<Person>();
		this.authors.addAll(Arrays.asList(authors));
		return this;
	}

	/**
	 * Returns the list of categories of this object.
	 *
	 * @return The list of categories of this object.
	 */
	@Xml(format=COLLAPSED, childName="category")
	public List<Category> getCatetories() {
		return categories;
	}

	/**
	 * Sets the list of categories of this object.
	 *
	 * @param categories The list of categories of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry setCategories(List<Category> categories) {
		this.categories = categories;
		return this;
	}

	/**
	 * Adds one or more categories to the list of categories of this object.
	 *
	 * @param categories The categories to add to the list.
	 * @return This object (for method chaining).
	 */
	public CommonEntry addCategories(Category...categories) {
		if (this.categories == null)
			this.categories = new LinkedList<Category>();
		this.categories.addAll(Arrays.asList(categories));
		return this;
	}

	/**
	 * Returns the list of contributors of this object.
	 *
	 * @return The list of contributors of this object.
	 */
	@Xml(format=COLLAPSED, childName="contributor")
	public List<Person> getContributors() {
		return contributors;
	}

	/**
	 * Sets the list of contributors of this object.
	 *
	 * @param contributors The list of contributors of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry setContributors(List<Person> contributors) {
		this.contributors = contributors;
		return this;
	}

	/**
	 * Adds one or more contributors to the list of contributors of this object.
	 *
	 * @param contributors The contributor to add to the list.
	 * @return This object (for method chaining).
	 */
	public CommonEntry addContributors(Person...contributors) {
		if (this.contributors == null)
			this.contributors = new LinkedList<Person>();
		this.contributors.addAll(Arrays.asList(contributors));
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
	public CommonEntry setId(Id id) {
		this.id = id;
		return this;
	}

	/**
	 * Returns the list of links of this object.
	 *
	 * @return The list of links of this object.
	 */
	@Xml(format=COLLAPSED)
	public List<Link> getLinks() {
		return links;
	}

	/**
	 * Sets the list of links of this object.
	 *
	 * @param links The list of links of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry setLinks(List<Link> links) {
		this.links = links;
		return this;
	}

	/**
	 * Adds one or more links to the list of links of this object.
	 *
	 * @param links The links to add to the list.
	 * @return This object (for method chaining).
	 */
	public CommonEntry addLinks(Link...links) {
		if (this.links == null)
			this.links = new LinkedList<Link>();
		this.links.addAll(Arrays.asList(links));
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
	public CommonEntry setRights(Text rights) {
		this.rights = rights;
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
	public CommonEntry setTitle(Text title) {
		this.title = title;
		return this;
	}

	/**
	 * Returns the update timestamp of this object.
	 *
	 * @return The update timestamp of this object.
	 */
	@BeanProperty(transform=CalendarSwap.ISO8601DT.class)
	public Calendar getUpdated() {
		return updated;
	}

	/**
	 * Sets the update timestamp of this object.
	 *
	 * @param updated The update timestamp of this object.
	 * @return This object (for method chaining).
	 */
	public CommonEntry setUpdated(Calendar updated) {
		this.updated = updated;
		return this;
	}
}
