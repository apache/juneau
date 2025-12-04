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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.utils.*;

/**
 * Represents an individual entry within an Atom feed or as a standalone Atom document.
 *
 * <p>
 * An Atom entry is a discrete item of content within a feed, such as a blog post, news article,
 * podcast episode, or other individual piece of content. Entries can exist within a feed or be
 * published as standalone Atom documents.
 *
 * <p>
 * Each entry contains metadata about the content (title, authors, timestamps) and optionally the
 * content itself or links to it. Entries are designed to be independently meaningful and may be
 * consumed separately from their containing feed.
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
 * <h5 class='section'>Required Elements:</h5>
 * <p>
 * Per RFC 4287, the following elements are required in an entry:
 * <ul class='spaced-list'>
 * 	<li><c>atom:id</c> - A permanent, universally unique identifier for the entry.
 * 	<li><c>atom:title</c> - A human-readable title for the entry.
 * 	<li><c>atom:updated</c> - The most recent instant in time when the entry was modified.
 * </ul>
 *
 * <h5 class='section'>Recommended Elements:</h5>
 * <p>
 * The following elements are recommended:
 * <ul class='spaced-list'>
 * 	<li><c>atom:author</c> - Authors of the entry (required if feed doesn't have authors).
 * 	<li><c>atom:content</c> or <c>atom:link[@rel="alternate"]</c> - Either content or alternate link.
 * 	<li><c>atom:summary</c> - Brief summary of the entry (required if content is not inline).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an entry</jc>
 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(
 * 		<js>"tag:example.org,2024:entry1"</js>,
 * 		<js>"My First Blog Post"</js>,
 * 		<js>"2024-01-15T12:00:00Z"</js>
 * 	)
 * 	.setAuthors(
 * 		<jk>new</jk> Person(<js>"Jane Doe"</js>)
 * 			.setEmail(<js>"jane@example.org"</js>)
 * 	)
 * 	.setContent(
 * 		<jk>new</jk> Content(<js>"html"</js>)
 * 			.setText(<js>"&lt;p&gt;This is my first blog post!&lt;/p&gt;"</js>)
 * 	)
 * 	.setSummary(<js>"An introduction to my new blog"</js>)
 * 	.setPublished(<js>"2024-01-15T10:00:00Z"</js>)
 * 	.setLinks(
 * 		<jk>new</jk> Link(<js>"alternate"</js>, <js>"text/html"</js>, <js>"http://example.org/posts/1"</js>)
 * 	);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomEntry</c> construct in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.1.2">RFC 4287 - Section 4.1.2</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * </ul>
 */
@Bean(typeName = "entry")
public class Entry extends CommonEntry {

	private Content content;
	private Calendar published;
	private Source source;
	private Text summary;

	/** Bean constructor. */
	public Entry() {}

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

	/**
	 * Bean property getter:  <property>content</property>.
	 *
	 * <p>
	 * Returns the content of this entry, or a link to it.
	 *
	 * <p>
	 * The content element contains or links to the complete content of the entry. It can contain
	 * text, HTML, XHTML, or other media types. When not present, the entry must have an alternate
	 * link pointing to the content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Content getContent() { return content; }

	/**
	 * Bean property getter:  <property>published</property>.
	 *
	 * <p>
	 * Returns the time when this entry was first published or made available.
	 *
	 * <p>
	 * This differs from the updated time in that it represents the original publication date,
	 * which typically doesn't change even when the entry is modified. The updated timestamp
	 * reflects the last modification time.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Calendar getPublished() { return published; }

	/**
	 * Bean property getter:  <property>source</property>.
	 *
	 * <p>
	 * Returns metadata about the source feed if this entry was copied from another feed.
	 *
	 * <p>
	 * When an entry is copied or aggregated from another feed, the source element preserves
	 * metadata from the original feed. This is useful for attribution and tracking the origin
	 * of syndicated content.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Source getSource() { return source; }

	/**
	 * Bean property getter:  <property>summary</property>.
	 *
	 * <p>
	 * Returns a short summary, abstract, or excerpt of the entry.
	 *
	 * <p>
	 * The summary is typically used in feed readers to give users a preview of the entry's
	 * content without loading the full content. It's especially useful when content is not
	 * inline or is very long.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Text getSummary() { return summary; }

	@Override /* Overridden from CommonEntry */
	public Entry setAuthors(Person...value) {
		super.setAuthors(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Entry setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setCategories(Category...value) {
		super.setCategories(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>content</property>.
	 *
	 * <p>
	 * Sets the content of this entry.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Plain text content</jc>
	 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
	 * 		.setContent(
	 * 			<jk>new</jk> Content(<js>"text"</js>)
	 * 				.setText(<js>"This is plain text content"</js>)
	 * 		);
	 *
	 * 	<jc>// HTML content</jc>
	 * 	Entry <jv>entry2</jv> = <jk>new</jk> Entry(...)
	 * 		.setContent(
	 * 			<jk>new</jk> Content(<js>"html"</js>)
	 * 				.setText(<js>"&lt;p&gt;This is &lt;strong&gt;HTML&lt;/strong&gt; content&lt;/p&gt;"</js>)
	 * 		);
	 *
	 * 	<jc>// Link to external content</jc>
	 * 	Entry <jv>entry3</jv> = <jk>new</jk> Entry(...)
	 * 		.setContent(
	 * 			<jk>new</jk> Content()
	 * 				.setType(<js>"video/mp4"</js>)
	 * 				.setSrc(<js>"http://example.org/video.mp4"</js>)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry setContent(Content value) {
		this.content = value;
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setContributors(Person...value) {
		super.setContributors(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setId(Id value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setId(String value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from Common */
	public Entry setLang(String value) {
		super.setLang(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setLinks(Link...value) {
		super.setLinks(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>published</property>.
	 *
	 * <p>
	 * Sets the time when this entry was first published or made available.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
	 * 		.setPublished(Calendar.<jsm>getInstance</jsm>());
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry setPublished(Calendar value) {
		this.published = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>published</property>.
	 *
	 * <p>
	 * Sets the time when this entry was first published using an ISO-8601 date string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
	 * 		.setPublished(<js>"2024-01-15T10:00:00Z"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property in ISO-8601 format.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry setPublished(String value) {
		setPublished(opt(value).filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.of(value).getZonedDateTime()).map(GregorianCalendar::from).orElse(null));
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setRights(String value) {
		super.setRights(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setRights(Text value) {
		super.setRights(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>source</property>.
	 *
	 * <p>
	 * Sets metadata about the source feed if this entry was copied from another feed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
	 * 		.setSource(
	 * 			<jk>new</jk> Source()
	 * 				.setId(<js>"tag:originalblog.example.com,2024:feed"</js>)
	 * 				.setTitle(<js>"Original Blog"</js>)
	 * 				.setUpdated(<js>"2024-01-15T12:00:00Z"</js>)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry setSource(Source value) {
		this.source = value;
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>summary</property>.
	 *
	 * <p>
	 * Sets a short summary, abstract, or excerpt of the entry as plain text.
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

	/**
	 * Bean property setter:  <property>summary</property>.
	 *
	 * <p>
	 * Sets a short summary, abstract, or excerpt of the entry.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
	 * 		.setSummary(
	 * 			<jk>new</jk> Text(<js>"text"</js>)
	 * 				.setText(<js>"This entry discusses the benefits of Atom feeds..."</js>)
	 * 		);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Entry setSummary(Text value) {
		this.summary = value;
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setTitle(Text value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setUpdated(Calendar value) {
		super.setUpdated(value);
		return this;
	}

	@Override /* Overridden from CommonEntry */
	public Entry setUpdated(String value) {
		super.setUpdated(value);
		return this;
	}
}