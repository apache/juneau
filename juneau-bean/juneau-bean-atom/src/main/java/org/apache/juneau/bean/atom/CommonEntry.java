/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.atom;

import static org.apache.juneau.commons.utils.CopyUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.marshall.xml.XmlFormat.*;

import java.util.*;

import org.apache.juneau.commons.time.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Base class for feed-level and entry-level Atom elements.
 *
 * <p>
 * This abstract class contains properties common to {@link Feed}, {@link Entry}, and {@link Source}
 * elements. These elements share a common set of metadata properties including authors, contributors,
 * categories, links, and timestamps.
 *
 * <p>
 * Common properties include:
 * <ul class='spaced-list'>
 * 	<li><b>id</b> (required) - Permanent, unique identifier
 * 	<li><b>title</b> (required) - Human-readable title
 * 	<li><b>updated</b> (required) - Last modification timestamp
 * 	<li><b>authors</b> - Author information
 * 	<li><b>categories</b> - Classification/tagging information
 * 	<li><b>contributors</b> - Contributor information
 * 	<li><b>links</b> - Related resources
 * 	<li><b>rights</b> - Copyright/rights information
 * </ul>
 *
 * <p>
 * This class extends {@link Common}, inheriting the <c>xml:base</c> and <c>xml:lang</c> attributes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@SuppressWarnings("java:S119")  // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
public class CommonEntry<SELF extends CommonEntry<SELF>> extends Common {

	private Person[] authors;
	private Category[] categories;
	private Person[] contributors;
	private Id id;
	private Link[] links;
	private Text rights;
	private Text title;
	private Calendar updated;

	/** Bean constructor. */
	public CommonEntry() {}

	@SuppressWarnings("unchecked")
	private SELF self() {
		return (SELF) this;
	}

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this object.  Can be <jk>null</jk>.
	 * @param title The title of this object.  Can be <jk>null</jk>.
	 * @param updated The updated timestamp of this object.  Can be <jk>null</jk>.
	 */
	public CommonEntry(Id id, Text title, Calendar updated) {
		setId(id).setTitle(title).setUpdated(updated);
	}

	/**
	 * Normal constructor.
	 *
	 * @param id The ID of this object.  Can be <jk>null</jk>.
	 * @param title The title of this object.  Can be <jk>null</jk>.
	 * @param updated The updated timestamp of this object.  Can be <jk>null</jk>.
	 */
	public CommonEntry(String id, String title, String updated) {
		setId(id).setTitle(title).setUpdated(updated);
	}

	/**
	 * Bean property getter:  <property>authors</property>.
	 *
	 * <p>
	 * The list of authors for this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = COLLAPSED, childName = "author")
	public Person[] getAuthors() { return cp(authors); }

	/**
	 * Bean property getter:  <property>categories</property>.
	 *
	 * <p>
	 * The list of categories of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = COLLAPSED, childName = "category")
	public Category[] getCategories() { return cp(categories); }

	/**
	 * Bean property getter:  <property>contributors</property>.
	 *
	 * <p>
	 * The list of contributors of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = COLLAPSED, childName = "contributor")
	public Person[] getContributors() { return cp(contributors); }

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * <p>
	 * The ID of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Id getId() { return id; }

	/**
	 * Bean property getter:  <property>links</property>.
	 *
	 * <p>
	 * The list of links of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = COLLAPSED)
	public Link[] getLinks() { return cp(links); }

	/**
	 * Bean property getter:  <property>rights</property>.
	 *
	 * <p>
	 * The rights statement of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getRights() { return rights; }

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * The title of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getTitle() { return title; }

	/**
	 * Bean property getter:  <property>updated</property>.
	 *
	 * <p>
	 * The update timestamp of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Calendar getUpdated() { return cloneOf(updated); }

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
	public SELF setAuthors(Person...value) {
		authors = cp(value);
		return self();
	}

	@Override /* Overridden from Common */
	public SELF setBase(Object value) {
		super.setBase(value);
		return self();
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
	public SELF setCategories(Category...value) {
		categories = cp(value);
		return self();
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
	public SELF setContributors(Person...value) {
		contributors = cp(value);
		return self();
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
	public SELF setId(Id value) {
		id = value;
		return self();
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
	public SELF setId(String value) {
		setId(new Id(value));
		return self();
	}

	@Override /* Overridden from Common */
	public SELF setLang(String value) {
		super.setLang(value);
		return self();
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
	public SELF setLinks(Link...value) {
		links = cp(value);
		return self();
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
	public SELF setRights(String value) {
		setRights(new Text().setText(value));
		return self();
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
	public SELF setRights(Text value) {
		rights = value;
		return self();
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
	public SELF setTitle(String value) {
		setTitle(new Text().setText(value));
		return self();
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
	public SELF setTitle(Text value) {
		title = value;
		return self();
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
	public SELF setUpdated(Calendar value) {
		updated = cloneOf(value);
		return self();
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
	public SELF setUpdated(String value) {
		setUpdated(o(value).filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.of(value).getZonedDateTime()).map(GregorianCalendar::from).orElse(null));
		return self();
	}
}