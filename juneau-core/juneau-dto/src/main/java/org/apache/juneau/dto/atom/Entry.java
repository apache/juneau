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

/**
 * Represents an <c>atomEntry</c> construct in the RFC4287 specification.
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
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
	 * Bean property getter:  <property>content</property>.
	 *
	 * <p>
	 * The content of this entry.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Content getContent() {
		return content;
	}

	/**
	 * Bean property setter:  <property>content</property>.
	 *
	 * <p>
	 * The content of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setContent(Content value) {
		this.content = value;
	}

	/**
	 * Bean property fluent getter:  <property>content</property>.
	 *
	 * <p>
	 * The content of this entry.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Content> content() {
		return Optional.ofNullable(content);
	}

	/**
	 * Bean property fluent setter:  <property>content</property>.
	 *
	 * <p>
	 * The content of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry content(Content value) {
		setContent(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>published</property>.
	 *
	 * <p>
	 * The publish timestamp of this entry.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Calendar getPublished() {
		return published;
	}

	/**
	 * Bean property setter:  <property>published</property>.
	 *
	 * <p>
	 * The publish timestamp of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setPublished(Calendar value) {
		this.published = value;
	}

	/**
	 * Bean property fluent getter:  <property>published</property>.
	 *
	 * <p>
	 * The publish timestamp of this entry.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Calendar> published() {
		return Optional.ofNullable(published);
	}

	/**
	 * Bean property fluent setter:  <property>published</property>.
	 *
	 * <p>
	 * The publish timestamp of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry published(Calendar value) {
		setPublished(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>published</property>.
	 *
	 * <p>
	 * The publish timestamp of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry published(String value) {
		setPublished(parseDateTime(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>source</property>.
	 *
	 * <p>
	 * The source of this entry.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Source getSource() {
		return source;
	}

	/**
	 * Bean property setter:  <property>source</property>.
	 *
	 * <p>
	 * The source of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setSource(Source value) {
		this.source = value;
	}

	/**
	 * Bean property fluent getter:  <property>source</property>.
	 *
	 * <p>
	 * The source of this entry.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Source> source() {
		return Optional.ofNullable(source);
	}

	/**
	 * Bean property fluent setter:  <property>source</property>.
	 *
	 * <p>
	 * The source of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry source(Source value) {
		setSource(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>summary</property>.
	 *
	 * <p>
	 * The summary of this entry.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getSummary() {
		return summary;
	}

	/**
	 * Bean property setter:  <property>summary</property>.
	 *
	 * <p>
	 * The summary of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setSummary(Text value) {
		this.summary = value;
	}

	/**
	 * Bean property fluent getter:  <property>summary</property>.
	 *
	 * <p>
	 * The summary of this entry.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Text> summary() {
		return Optional.ofNullable(summary);
	}

	/**
	 * Bean property fluent setter:  <property>summary</property>.
	 *
	 * <p>
	 * The summary of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry summary(Text value) {
		setSummary(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>summary</property>.
	 *
	 * <p>
	 * The summary of this entry.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry summary(String value) {
		setSummary(new Text(value));
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