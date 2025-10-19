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

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a reference from an entry or feed to a Web resource.
 *
 * <p>
 * Atom links define references to related resources such as alternate representations,
 * related documents, enclosures (for podcasts), and navigation links. Links are one of the
 * fundamental components of Atom and enable rich hypermedia relationships between resources.
 *
 * <p>
 * The link's <c>rel</c> attribute defines the relationship type, while <c>href</c> provides
 * the target URI. Common relationship types include:
 * <ul class='spaced-list'>
 * 	<li><c>alternate</c> - An alternate representation (e.g., HTML version of entry)
 * 	<li><c>self</c> - The feed/entry itself
 * 	<li><c>related</c> - A related resource
 * 	<li><c>enclosure</c> - A related resource to be downloaded (e.g., podcast audio)
 * 	<li><c>via</c> - The source of the information
 * </ul>
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomLink =
 * 		element atom:link {
 * 			atomCommonAttributes,
 * 			attribute href { atomUri },
 * 			attribute rel { atomNCName | atomUri }?,
 * 			attribute type { atomMediaType }?,
 * 			attribute hreflang { atomLanguageTag }?,
 * 			attribute title { text }?,
 * 			attribute length { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create links for an entry</jc>
 * 	Entry <jv>entry</jv> = <jk>new</jk> Entry(...)
 * 		.setLinks(
 * 			<jc>// Link to HTML version</jc>
 * 			<jk>new</jk> Link(<js>"alternate"</js>, <js>"text/html"</js>, <js>"http://example.org/post1.html"</js>)
 * 				.setHreflang(<js>"en"</js>),
 * 			<jc>// Podcast enclosure</jc>
 * 			<jk>new</jk> Link(<js>"enclosure"</js>, <js>"audio/mpeg"</js>, <js>"http://example.org/episode1.mp3"</js>)
 * 				.setLength(24986239)
 * 		);
 * </p>
 *
 * <h5 class='section'>Specification:</h5>
 * <p>
 * Represents an <c>atomLink</c> construct in the
 * <a class="doclink" href="https://tools.ietf.org/html/rfc4287#section-4.2.7">RFC 4287 - Section 4.2.7</a> specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanAtom">juneau-bean-atom</a>
 * 	<li class='extlink'><a class="doclink" href="https://tools.ietf.org/html/rfc4287">RFC 4287 - The Atom Syndication Format</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA Link Relations</a>
 * </ul>
 */
@Bean(typeName = "link")
public class Link extends Common {

	private String href;
	private String rel;
	private String type;
	private String hreflang;
	private String title;
	private Integer length;

	/** Bean constructor. */
	public Link() {}

	/**
	 * Normal constructor.
	 *
	 * @param rel The rel of the link.
	 * @param type The type of the link.
	 * @param href The URI of the link.
	 */
	public Link(String rel, String type, String href) {
		setRel(rel).setType(type).setHref(href);
	}

	/**
	 * Bean property getter:  <property>href</property>.
	 *
	 * <p>
	 * Returns the URI of the referenced resource.
	 *
	 * <p>
	 * This is the target address of the link and is a required attribute for all Atom links.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public String getHref() { return href; }

	/**
	 * Bean property getter:  <property>hreflang</property>.
	 *
	 * <p>
	 * Returns the language of the resource pointed to by the link.
	 *
	 * <p>
	 * The value should be a language tag as defined by RFC 3066.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public String getHreflang() { return hreflang; }

	/**
	 * Bean property getter:  <property>length</property>.
	 *
	 * <p>
	 * Returns an advisory size in bytes of the linked resource.
	 *
	 * <p>
	 * This is particularly useful for enclosures (podcast episodes, video files, etc.) to
	 * help clients decide whether to download the resource.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public Integer getLength() { return length; }

	/**
	 * Bean property getter:  <property>rel</property>.
	 *
	 * <p>
	 * Returns the link relation type.
	 *
	 * <p>
	 * The <c>rel</c> attribute indicates the type of relationship between the entry/feed and
	 * the linked resource. When not specified, the default is "alternate".
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public String getRel() { return rel; }

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * <p>
	 * Returns human-readable information about the link.
	 *
	 * <p>
	 * The title provides advisory information about the link, typically for display to users.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public String getTitle() { return title; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format = ATTR)
	public String getType() { return type; }

	@Override /* Overridden from Common */
	public Link setBase(Object value) {
		super.setBase(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>href</property>.
	 *
	 * <p>
	 * Sets the URI of the referenced resource (required).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Link <jv>link</jv> = <jk>new</jk> Link()
	 * 		.setHref(<js>"http://example.org/posts/1"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link setHref(String value) {
		this.href = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>hreflang</property>.
	 *
	 * <p>
	 * Sets the language of the resource pointed to by the link.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Link <jv>link</jv> = <jk>new</jk> Link(<js>"alternate"</js>, <js>"text/html"</js>, <js>"http://example.org/post1.html"</js>)
	 * 		.setHreflang(<js>"en-US"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property (e.g., "en", "fr", "de", "en-US").
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link setHreflang(String value) {
		this.hreflang = value;
		return this;
	}

	@Override /* Overridden from Common */
	public Link setLang(String value) {
		super.setLang(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>length</property>.
	 *
	 * <p>
	 * Sets an advisory size in bytes of the linked resource.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Podcast episode with file size</jc>
	 * 	Link <jv>link</jv> = <jk>new</jk> Link(<js>"enclosure"</js>, <js>"audio/mpeg"</js>, <js>"http://example.org/episode1.mp3"</js>)
	 * 		.setLength(24986239);  <jc>// ~24 MB</jc>
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property in bytes.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link setLength(Integer value) {
		this.length = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>rel</property>.
	 *
	 * <p>
	 * Sets the link relation type.
	 *
	 * <p>
	 * Common values include <js>"alternate"</js>, <js>"self"</js>, <js>"related"</js>,
	 * <js>"enclosure"</js>, <js>"via"</js>, <js>"first"</js>, <js>"last"</js>,
	 * <js>"previous"</js>, <js>"next"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Link <jv>link</jv> = <jk>new</jk> Link()
	 * 		.setRel(<js>"alternate"</js>)
	 * 		.setHref(<js>"http://example.org/post1.html"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property (defaults to "alternate").
	 * @return This object.
	 */
	public Link setRel(String value) {
		this.rel = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * <p>
	 * Sets human-readable information about the link.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Link <jv>link</jv> = <jk>new</jk> Link(<js>"related"</js>, <js>"text/html"</js>, <js>"http://example.org/related"</js>)
	 * 		.setTitle(<js>"Related Article"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Link setTitle(String value) {
		this.title = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The content type of the target of this link.
	 *
	 * <p>
	 * This should be a valid MIME media type as defined by RFC 4287.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><js>"text/html"</js>
	 * 	<li><js>"application/pdf"</js>
	 * 	<li><js>"image/jpeg"</js>
	 * 	<li><js>"application/atom+xml"</js>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Link setType(String value) {
		this.type = value;
		return this;
	}
}