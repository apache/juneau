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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a top-level Atom feed document.
 *
 * <p>
 * An Atom feed is a Web resource that contains metadata and optionally a set of entries. 
 * Feeds are the top-level container element in Atom documents and act as a manifest of metadata 
 * and data associated with a collection of related resources.
 *
 * <p>
 * The feed is the fundamental unit of syndication in Atom and is used to aggregate entries that 
 * share a common purpose, such as a blog, podcast channel, or news source.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
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
 * <h5 class='section'>Required Elements:</h5>
 * <p>
 * Per RFC 4287, the following elements are required in a feed:
 * <ul class='spaced-list'>
 * 	<li><c>atom:id</c> - A permanent, universally unique identifier for the feed.
 * 	<li><c>atom:title</c> - A human-readable title for the feed.
 * 	<li><c>atom:updated</c> - The most recent instant in time when the feed was modified.
 * </ul>
 *
 * <h5 class='section'>Recommended Elements:</h5>
 * <p>
 * The following elements are recommended but not required:
 * <ul class='spaced-list'>
 * 	<li><c>atom:author</c> - Authors of the feed (required if entries don't have authors).
 * 	<li><c>atom:link</c> - Links associated with the feed (should include a "self" link).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a feed using fluent-style setters</jc>
 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(
 * 		<js>"tag:example.org,2024:feed"</js>,
 * 		<js>"Example Feed"</js>,
 * 		<js>"2024-01-15T12:00:00Z"</js>
 * 	)
 * 	.setSubtitle(<js>"A sample Atom feed"</js>)
 * 	.setLinks(
 * 		<jk>new</jk> Link(<js>"self"</js>, <js>"application/atom+xml"</js>, <js>"http://example.org/feed.atom"</js>),
 * 		<jk>new</jk> Link(<js>"alternate"</js>, <js>"text/html"</js>, <js>"http://example.org"</js>)
 * 	)
 * 	.setAuthors(
 * 		<jk>new</jk> Person(<js>"John Doe"</js>).setEmail(<js>"john@example.org"</js>)
 * 	)
 * 	.setEntries(
 * 		<jk>new</jk> Entry(<js>"tag:example.org,2024:entry1"</js>, <js>"First Post"</js>, <js>"2024-01-15T12:00:00Z"</js>)
 * 			.setSummary(<js>"This is the first post"</js>)
 * 	);
 *
 * 	<jc>// Serialize to ATOM/XML</jc>
 * 	String <jv>atomXml</jv> = XmlSerializer.<jsf>DEFAULT_SQ_READABLE</jsf>.serialize(<jv>feed</jv>);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomFeed</c> construct in the 
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.1.1">RFC 4287 - Section 4.1.1</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
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


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>generator</property>.
	 *
	 * <p>
	 * Identifies the software agent used to generate the feed.
	 *
	 * <p>
	 * This is useful for debugging and analytics purposes, allowing consumers to identify the 
	 * software responsible for producing the feed.
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
	 * Identifies the software agent used to generate the feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setGenerator(
	 * 			<jk>new</jk> Generator(<js>"My Blog Software"</js>)
	 * 				.setUri(<js>"http://www.example.com/software"</js>)
	 * 				.setVersion(<js>"2.0"</js>)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setGenerator(Generator value) {
		this.generator = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>icon</property>.
	 *
	 * <p>
	 * Identifies a small image that provides iconic visual identification for the feed.
	 *
	 * <p>
	 * Icons should be square and small (typically 16x16 or similar). The image should have an 
	 * aspect ratio of 1 (horizontal) to 1 (vertical).
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
	 * Identifies a small image that provides iconic visual identification for the feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setIcon(<jk>new</jk> Icon(<js>"http://example.org/icon.png"</js>));
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setIcon(Icon value) {
		this.icon = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>logo</property>.
	 *
	 * <p>
	 * Identifies a larger image that provides visual identification for the feed.
	 *
	 * <p>
	 * Logos should be twice as wide as they are tall (aspect ratio of 2:1).
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
	 * Identifies a larger image that provides visual identification for the feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setLogo(<jk>new</jk> Logo(<js>"http://example.org/logo.png"</js>));
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setLogo(Logo value) {
		this.logo = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>subtitle</property>.
	 *
	 * <p>
	 * Returns a human-readable description or subtitle for the feed.
	 *
	 * <p>
	 * The subtitle provides additional context about the feed's purpose or content beyond 
	 * what is conveyed in the title.
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
	 * Sets a human-readable description or subtitle for the feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setSubtitle(
	 * 			<jk>new</jk> Text(<js>"html"</js>)
	 * 				.setText(<js>"A &lt;em&gt;comprehensive&lt;/em&gt; guide to Atom feeds"</js>)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setSubtitle(Text value) {
		this.subtitle = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>subtitle</property>.
	 *
	 * <p>
	 * Sets a human-readable description or subtitle for the feed as plain text.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setSubtitle(String value) {
		setSubtitle(new Text(value));
		return this;
	}

	/**
	 * Bean property getter:  <property>entries</property>.
	 *
	 * <p>
	 * Returns the individual entries contained within this feed.
	 *
	 * <p>
	 * Each entry represents a single item in the feed, such as a blog post, news article, 
	 * podcast episode, or other discrete piece of content. Entries contain their own metadata 
	 * including title, content, links, and timestamps.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=COLLAPSED)
	public Entry[] getEntries() {
		return entries;
	}

	/**
	 * Bean property setter:  <property>entries</property>.
	 *
	 * <p>
	 * Sets the individual entries contained within this feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Feed <jv>feed</jv> = <jk>new</jk> Feed(...)
	 * 		.setEntries(
	 * 			<jk>new</jk> Entry(
	 * 				<js>"tag:example.org,2024:entry1"</js>,
	 * 				<js>"First Post"</js>,
	 * 				<js>"2024-01-15T12:00:00Z"</js>
	 * 			)
	 * 			.setContent(
	 * 				<jk>new</jk> Content(<js>"html"</js>)
	 * 					.setText(<js>"&lt;p&gt;This is the content&lt;/p&gt;"</js>)
	 * 			),
	 * 			<jk>new</jk> Entry(
	 * 				<js>"tag:example.org,2024:entry2"</js>,
	 * 				<js>"Second Post"</js>,
	 * 				<js>"2024-01-16T12:00:00Z"</js>
	 * 			)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Feed setEntries(Entry...value) {
		this.entries = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from Common */
	public Feed setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Feed setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setRights(Text value) {
		super.setRights(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Feed setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}
}