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

import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transforms.*;

/**
 * Represents an <code>atomEntry</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomEntry =
 * 		element atom:entry {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			& atomCategory*
 * 			& atomContent?
 * 			& atomContributor*
 * 			& atomId
 * 			& atomLink*
 * 			& atomPublished?
 * 			& atomRights?
 * 			& atomSource?
 * 			& atomSummary?
 * 			& atomTitle
 * 			& atomUpdated
 * 			& extensionElement*)
 * 		}
 * </p>
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(name="entry")
public class Entry extends CommonEntry {

	private Content content;
	private Calendar published;
	private Source source;
	private Text summary;

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this entry.
	 * @param title The title of this entry.
	 * @param updated The updated timestamp of this entry.
	 */
	public Entry(Id id, Text title, Calendar updated) {
		super(id, title, updated);
	}

	/** Bean constructor. */
	public Entry() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the content of this entry.
	 *
	 * @return The content of this entry.
	 */
	public Content getContent() {
		return content;
	}

	/**
	 * Sets the content of this entry.
	 *
	 * @param content The content of this entry.
	 * @return This object (for method chaining).
	 */
	public Entry setContent(Content content) {
		this.content = content;
		return this;
	}

	/**
	 * Returns the publish timestamp of this entry.
	 *
	 * @return The publish timestamp of this entry.
	 */
 	@BeanProperty(swap=CalendarSwap.ISO8601DT.class)
	public Calendar getPublished() {
 		return published;
 	}

	/**
	 * Sets the publish timestamp of this entry.
	 *
	 * @param published The publish timestamp of this entry.
	 * @return This object (for method chaining).
	 */
 	public Entry setPublished(Calendar published) {
 		this.published = published;
 		return this;
 	}

	/**
	 * Returns the source of this entry.
	 *
	 * @return The source of this entry.
	 */
	public Source getSource() {
		return source;
	}

	/**
	 * Sets the source of this entry.
	 *
	 * @param source The source of this entry.
	 * @return This object (for method chaining).
	 */
	public Entry setSource(Source source) {
		this.source = source;
		return this;
	}

	/**
	 * Returns the summary of this entry.
	 *
	 * @return The summary of this entry.
	 */
	public Text getSummary() {
		return summary;
	}

	/**
	 * Sets the summary of this entry.
	 *
	 * @param summary The summary of this entry.
	 * @return This object (for method chaining).
	 */
	public Entry setSummary(Text summary) {
		this.summary = summary;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Entry setAuthors(List<Person> authors) {
		super.setAuthors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry addAuthors(Person...authors) {
		super.addAuthors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setCategories(List<Category> categories) {
		super.setCategories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Entry addCategories(Category...categories) {
		super.addCategories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setContributors(List<Person> contributors) {
		super.setContributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry addContributors(Person...contributors) {
		super.addContributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setId(Id id) {
		super.setId(id);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setLinks(List<Link> links) {
		super.setLinks(links);
		return this;
	}

	@Override /* CommonEntry */
	public Entry addLinks(Link...links) {
		super.addLinks(links);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setRights(Text rights) {
		super.setRights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setTitle(Text title) {
		super.setTitle(title);
		return this;
	}

	@Override /* CommonEntry */
	public Entry setUpdated(Calendar updated) {
		super.setUpdated(updated);
		return this;
	}

	@Override /* Common */
	public Entry setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Entry setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}