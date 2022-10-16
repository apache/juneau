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
import org.apache.juneau.internal.*;

/**
 * Represents an <c>atomEntry</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
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
@FluentSetters
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
	 * @return This object
	 */
	public Entry setContent(Content value) {
		this.content = value;
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
	 * @return This object
	 */
	public Entry setPublished(Calendar value) {
		this.published = value;
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
	public Entry setPublished(String value) {
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
	 * @return This object
	 */
	public Entry setSource(Source value) {
		this.source = value;
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
	 * @return This object
	 */
	public Entry setSummary(Text value) {
		this.summary = value;
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
	public Entry setSummary(String value) {
		setSummary(new Text(value));
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Entry setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Entry setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setRights(Text value) {
		super.setRights(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.CommonEntry */
	public Entry setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}

	// </FluentSetters>
}