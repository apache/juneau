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

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomLink</code> construct in the RFC4287 specification.
 *
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
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
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Atom'>Atom</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * 	</li>
 * </ul>
 */
@Bean(typeName="link")
@SuppressWarnings("hiding")
public class Link extends Common {

	private String href;
	private String rel;
	private String type;
	private String hreflang;
	private String title;
	private Integer length;


	/**
	 * Normal constructor.
	 *
	 * @param rel The rel of the link.
	 * @param type The type of the link.
	 * @param href The URI of the link.
	 */
	public Link(String rel, String type, String href) {
		rel(rel).type(type).href(href);
	}

	/** Bean constructor. */
	public Link() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the href of the target of this link.
	 *
	 * @return The href of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getHref() {
		return href;
	}

	/**
	 * Sets the href of the target of this link.
	 *
	 * @param href The href of the target of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("href")
	public Link href(String href) {
		this.href = href;
		return this;
	}

	/**
	 * Returns the rel of this link.
	 *
	 * @return The rel of this link.
	 */
	@Xml(format=ATTR)
	public String getRel() {
		return rel;
	}

	/**
	 * Sets the rel of this link.
	 *
	 * @param rel The rel of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("rel")
	public Link rel(String rel) {
		this.rel = rel;
		return this;
	}

	/**
	 * Returns the content type of the target of this link.
	 *
	 * @return The content type of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Sets the content type of the target of this link.
	 *
	 * <p>
	 * Must be one of the following:
	 * <ul>
	 * 	<li><js>"text"</js>
	 * 	<li><js>"html"</js>
	 * 	<li><js>"xhtml"</js>
	 * 	<li><jk>null</jk> (defaults to <js>"text"</js>)
	 * </ul>
	 *
	 * @param type The content type of the target of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("type")
	public Link type(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Returns the language of the target of this link.
	 *
	 * @return The language of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getHreflang() {
		return hreflang;
	}

	/**
	 * Sets the language of the target of this link.
	 *
	 * @param hreflang The language of the target of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("hreflang")
	public Link hreflang(String hreflang) {
		this.hreflang = hreflang;
		return this;
	}

	/**
	 * Returns the title of the target of this link.
	 *
	 * @return The title of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of the target of this link.
	 *
	 * @param title The title of the target of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("title")
	public Link title(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Returns the length of the contents of the target of this link.
	 *
	 * @return The length of the contents of the target of this link.
	 */
	@Xml(format=ATTR)
	public Integer getLength() {
		return length;
	}

	/**
	 * Sets the length of the contents of the target of this link.
	 *
	 * @param length The length of the contents of the target of this link.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("length")
	public Link length(Integer length) {
		this.length = length;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Link base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Link lang(String lang) {
		super.lang(lang);
		return this;
	}
}
