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
 * Top-level ATOM feed object.
 * 
 * <p>
 * Represents an <code>atomFeed</code> construct in the RFC4287 specification.
 * 
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode'>
 * 	atomFeed =
 * 		element atom:feed {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			 &amp; atomCategory*
 * 			 &amp; atomContributor*
 * 			 &amp; atomGenerator?
 * 			 &amp; atomIcon?
 * 			 &amp; atomId
 * 			 &amp; atomLink*
 * 			 &amp; atomLogo?
 * 			 &amp; atomRights?
 * 			 &amp; atomSubtitle?
 * 			 &amp; atomTitle
 * 			 &amp; atomUpdated
 * 			 &amp; extensionElement*),
 * 			atomEntry*
 * 		}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Atom'>Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
@Bean(typeName="feed")
public class Feed extends CommonEntry {

	private Generator generator;  // atomGenerator?
	private Icon icon;            // atomIcon?
	private Logo logo;            // atomLogo?
	private Text subtitle;        // atomSubtitle?
	private Entry[] entries;      // atomEntry*

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

	/**
	 * Normal constructor.
	 * 
	 * @param id The feed identifier.
	 * @param title The feed title.
	 * @param updated The feed updated timestamp.
	 */
	public Feed(String id, String title, String updated) {
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
	@BeanProperty("generator")
	public Feed generator(Generator generator) {
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
	@BeanProperty("icon")
	public Feed icon(Icon icon) {
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
	@BeanProperty("logo")
	public Feed logo(Logo logo) {
		this.logo = logo;
		return this;
	}

	/**
	 * Returns the feed subtitle.
	 * 
	 * @return The feed subtitle.
	 */
	@BeanProperty("subtitle")
	public Text getSubTitle() {
		return subtitle;
	}

	/**
	 * Sets the feed subtitle.
	 * 
	 * @param subtitle The feed subtitle.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("subtitle")
	public Feed subtitle(Text subtitle) {
		this.subtitle = subtitle;
		return this;
	}

	/**
	 * Sets the feed subtitle.
	 * 
	 * @param subtitle The feed subtitle.
	 * @return This object (for method chaining).
	 */
	public Feed subtitle(String subtitle) {
		this.subtitle = new Text(subtitle);
		return this;
	}

	/**
	 * Returns the entries in the feed.
	 * 
	 * @return The entries in the feed.
	 */
	@Xml(format=COLLAPSED)
	public Entry[] getEntries() {
		return entries;
	}

	/**
	 * Sets the entries in the feed.
	 * 
	 * @param entries The entries in the feed.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("entries")
	public Feed entries(Entry...entries) {
		this.entries = entries;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Feed authors(Person...authors) {
		super.authors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed categories(Category...categories) {
		super.categories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Feed contributors(Person...contributors) {
		super.contributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Feed id(Id id) {
		super.id(id);
		return this;
	}

	@Override /* CommonEntry */
	public Feed links(Link...links) {
		super.links(links);
		return this;
	}

	@Override /* CommonEntry */
	public Feed rights(Text rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Feed rights(String rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Feed title(Text title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Feed title(String title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Feed updated(Calendar updated) {
		super.updated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Feed updated(String updated) {
		super.updated(updated);
		return this;
	}

	@Override /* Common */
	public Feed base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Feed lang(String lang) {
		super.lang(lang);
		return this;
	}
}
