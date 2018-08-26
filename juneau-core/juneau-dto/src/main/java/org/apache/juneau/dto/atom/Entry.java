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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transforms.*;

/**
 * Represents an <code>atomEntry</code> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomEntry =
 * 		element atom:entry {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			&amp; atomCategory*
 * 			&amp; atomContent?
 * 			&amp; atomContributor*
 * 			&amp; atomId
 * 			&amp; atomLink*
 * 			&amp; atomPublished?
 * 			&amp; atomRights?
 * 			&amp; atomSource?
 * 			&amp; atomSummary?
 * 			&amp; atomTitle
 * 			&amp; atomUpdated
 * 			&amp; extensionElement*)
 * 		}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
@Bean(typeName="entry")
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

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this entry.
	 * @param title The title of this entry.
	 * @param updated The updated timestamp of this entry.
	 */
	public Entry(String id, String title, String updated) {
		super(id, title, updated);
	}

	/** Bean constructor. */
	public Entry() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

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
	@BeanProperty("content")
	public Entry content(Content content) {
		this.content = content;
		return this;
	}

	/**
	 * Returns the publish timestamp of this entry.
	 *
	 * @return The publish timestamp of this entry.
	 */
	@Swap(CalendarSwap.ISO8601DT.class)
	public Calendar getPublished() {
		return published;
	}

	/**
	 * Sets the publish timestamp of this entry.
	 *
	 * @param published The publish timestamp of this entry.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("published")
	public Entry published(Calendar published) {
		this.published = published;
		return this;
	}

	/**
	 * Sets the publish timestamp of this entry.
	 *
	 * @param published The publish timestamp of this entry in ISO8601 format.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("published")
	public Entry published(String published) {
		this.published = parseDateTime(published);
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
	@BeanProperty("source")
	public Entry source(Source source) {
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
	@BeanProperty("summary")
	public Entry summary(Text summary) {
		this.summary = summary;
		return this;
	}

	/**
	 * Sets the summary of this entry.
	 *
	 * @param summary The summary of this entry.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("summary")
	public Entry summary(String summary) {
		this.summary = new Text(summary);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Entry authors(Person...authors) {
		super.authors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry categories(Category...categories) {
		super.categories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Entry contributors(Person...contributors) {
		super.contributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Entry id(Id id) {
		super.id(id);
		return this;
	}

	@Override /* CommonEntry */
	public Entry links(Link...links) {
		super.links(links);
		return this;
	}

	@Override /* CommonEntry */
	public Entry rights(Text rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Entry rights(String rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Entry title(Text title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Entry title(String title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Entry updated(Calendar updated) {
		super.updated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Entry updated(String updated) {
		super.updated(updated);
		return this;
	}

	@Override /* Common */
	public Entry base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Entry lang(String lang) {
		super.lang(lang);
		return this;
	}
}