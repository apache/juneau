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

import java.util.*;

/**
 * Represents metadata from the source feed when an entry is copied from one feed to another.
 *
 * <p>
 * When entries are aggregated, copied, or republished from their original feed, the source 
 * element preserves metadata about the original feed. This is crucial for proper attribution 
 * and maintaining provenance information.
 *
 * <p>
 * The source element is a child of entry and contains a subset of feed-level metadata that 
 * identifies where the entry originally came from. All child elements are optional, but 
 * including at minimum the source feed's ID, title, and updated timestamp is recommended.
 *
 * <p>
 * Common use cases:
 * <ul class='spaced-list'>
 * 	<li>Feed aggregation - Combining entries from multiple sources
 * 	<li>Content syndication - Republishing entries from other feeds
 * 	<li>Attribution - Crediting the original source
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomSource =
 * 		element atom:source {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			&amp; atomCategory*
 * 			&amp; atomContributor*
 * 			&amp; atomGenerator?
 * 			&amp; atomIcon?
 * 			&amp; atomId?
 * 			&amp; atomLink*
 * 			&amp; atomLogo?
 * 			&amp; atomRights?
 * 			&amp; atomSubtitle?
 * 			&amp; atomTitle?
 * 			&amp; atomUpdated?
 * 			&amp; extensionElement*)
 * 		}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Entry copied from another feed</jc>
 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(
 * 		<js>"tag:myaggregator.example.com,2024:entry1"</js>,
 * 		<js>"Interesting Article"</js>,
 * 		<js>"2024-01-15T12:00:00Z"</js>
 * 	)
 * 	.setSource(
 * 		<jk>new</jk> Source()
 * 			.setId(<js>"tag:originalblog.example.com,2024:feed"</js>)
 * 			.setTitle(<js>"Original Blog"</js>)
 * 			.setUpdated(<js>"2024-01-15T12:00:00Z"</js>)
 * 			.setLinks(
 * 				<jk>new</jk> Link(<js>"self"</js>, <js>"application/atom+xml"</js>, 
 * 					<js>"http://originalblog.example.com/feed.atom"</js>)
 * 			)
 * 	);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomSource</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.11">RFC 4287 - Section 4.2.11</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
public class Source extends CommonEntry {

	private Generator generator;
	private Icon icon;
	private Logo logo;
	private Text subtitle;


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Generator getGenerator() {
		return generator;
	}

	/**
	 * Bean property setter:  <property>generator</property>.
	 *
	 * <p>
	 * The generator info of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Source setGenerator(Generator value) {
		this.generator = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Bean property setter:  <property>icon</property>.
	 *
	 * <p>
	 * The icon of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Source setIcon(Icon value) {
		this.icon = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Logo getLogo() {
		return logo;
	}

	/**
	 * Bean property setter:  <property>logo</property>.
	 *
	 * <p>
	 * The logo of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Source setLogo(Logo value) {
		this.logo = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getSubtitle() {
		return subtitle;
	}

	/**
	 * Bean property setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Source setSubtitle(Text value) {
		this.subtitle = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * The subtitle of this source.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Source setSubtitle(String value) {
		setSubtitle(new Text(value));
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Source setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Source setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setRights(Text value) {
		super.setRights(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Source setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}
}