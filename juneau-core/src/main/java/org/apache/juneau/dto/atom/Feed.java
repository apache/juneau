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

import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Top-level ATOM feed object.
 * <p>
 *  	Represents an <code>atomFeed</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomFeed =
 * 		element atom:feed {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			 & atomCategory*
 * 			 & atomContributor*
 * 			 & atomGenerator?
 * 			 & atomIcon?
 * 			 & atomId
 * 			 & atomLink*
 * 			 & atomLogo?
 * 			 & atomRights?
 * 			 & atomSubtitle?
 * 			 & atomTitle
 * 			 & atomUpdated
 * 			 & extensionElement*),
 * 			atomEntry*
 * 		}
 * </p>
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Xml(name="feed")
@SuppressWarnings("hiding")
public class Feed extends CommonEntry {

	private Generator generator;  // atomGenerator?
	private Icon icon;            // atomIcon?
	private Logo logo;            // atomLogo?
	private Text subtitle;        // atomSubtitle?
	private List<Entry> entries;  // atomEntry*

	/**
	 * Normal constructor.
	 *
	 * @param id The feed identifier.
	 * @param title The feed title.
	 * @param updated The feed updated timestamp.
	 */
	public Feed(Id id, Text title, Calendar updated) {
		super(id, title, updated);
	}

	/** Bean constructor. */
	public Feed() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns generator information on this feed.
	 *
	 * @return The generator information on this feed.
	 */
	public Generator getGenerator() {
		return generator;
	}

	/**
	 * Sets the generator information on this feed.
	 *
	 * @param generator The generator information on this feed.
	 * @return This object (for method chaining).
	 */
	public Feed setGenerator(Generator generator) {
		this.generator = generator;
		return this;
	}

	/**
	 * Returns the feed icon.
	 *
	 * @return The feed icon.
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Sets the feed icon.
	 *
	 * @param icon The feed icon.
	 * @return This object (for method chaining).
	 */
	public Feed setIcon(Icon icon) {
		this.icon = icon;
		return this;
	}

	/**
	 * Returns the feed logo.
	 *
	 * @return The feed logo.
	 */
	public Logo getLogo() {
		return logo;
	}

	/**
	 * Sets the feed logo.
	 *
	 * @param logo The feed logo.
	 * @return This object (for method chaining).
	 */
	public Feed setLogo(Logo logo) {
		this.logo = logo;
		return this;
	}

	/**
	 * Returns the feed subtitle.
	 *
	 * @return The feed subtitle.
	 */
	@BeanProperty(name="subtitle")
	public Text getSubTitle() {
		return subtitle;
	}

	/**
	 * Sets the feed subtitle.
	 *
	 * @param subtitle The feed subtitle.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="subtitle")
	public Feed setSubTitle(Text subtitle) {
		this.subtitle = subtitle;
		return this;
	}

	/**
	 * Returns the entries in the feed.
	 *
	 * @return The entries in the feed.
	 */
	@Xml(format=COLLAPSED)
	public List<Entry> getEntries() {
		return entries;
	}

	/**
	 * Sets the entries in the feed.
	 *
	 * @param entries The entries in the feed.
	 * @return This object (for method chaining).
	 */
	public Feed setEntries(List<Entry> entries) {
		this.entries = entries;
		return this;
	}

	/**
	 * Adds an entry to the list of entries in the feed.
	 *
	 * @param entries The entries to add to the list of entries in the feed.s
	 * @return This object (for method chaining).
	 */
	public Feed addEntries(Entry...entries) {
		if (this.entries == null)
			this.entries = new LinkedList<Entry>();
		this.entries.addAll(Arrays.asList(entries));
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Feed setAuthors(List<Person> authors) {
		super.setAuthors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed addAuthors(Person...authors) {
		super.addAuthors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setCategories(List<Category> categories) {
		super.setCategories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Feed addCategories(Category...categories) {
		super.addCategories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setContributors(List<Person> contributors) {
		super.setContributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed addContributors(Person...contributors) {
		super.addContributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setId(Id id) {
		super.setId(id);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setLinks(List<Link> links) {
		super.setLinks(links);
		return this;
	}

	@Override /* CommonEntry */
	public Feed addLinks(Link...links) {
		super.addLinks(links);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setRights(Text rights) {
		super.setRights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setTitle(Text title) {
		super.setTitle(title);
		return this;
	}

	@Override /* CommonEntry */
	public Feed setUpdated(Calendar updated) {
		super.setUpdated(updated);
		return this;
	}

	@Override /* Common */
	public Feed setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Feed setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
