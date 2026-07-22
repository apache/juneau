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
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-a-element">&lt;a&gt;</a>
 * element.
 *
 * <p>
 * The anchor element creates a hyperlink to other web pages, files, locations within the same page,
 * email addresses, or any other URL. It is one of the most fundamental elements for web navigation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple link to another page</jc>
 * 	A <jv>a1</jv> = <jsm>a</jsm>().href(<js>"https://example.com"</js>).text(<js>"Visit Example"</js>);
 *
 * 	<jc>// Link with target to open in new window</jc>
 * 	A <jv>a2</jv> = <jsm>a</jsm>().href(<js>"https://example.com"</js>).target(<js>"_blank"</js>).text(<js>"Open in New Window"</js>);
 *
 * 	<jc>// Email link</jc>
 * 	A <jv>a3</jv> = <jsm>a</jsm>().href(<js>"mailto:user@example.com"</js>).text(<js>"Send Email"</js>);
 *
 * 	<jc>// Link with relationship and language</jc>
 * 	A <jv>a4</jv> = <jsm>a</jsm>().href(<js>"https://example.com"</js>).rel(<js>"nofollow"</js>).hreflang(<js>"en"</js>).text(<js>"English Version"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#a() a()}
 * 		<li class='jm'>{@link HtmlBuilder#a(Object, Object...) a(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "a")
public class A extends HtmlElementMixed<A> {

	/**
	 * Creates an empty {@link A} element.
	 */
	public A() {}

	/**
	 * Creates an {@link A} element with the specified {@link A#href(Object)} attribute and {@link A#children(Object[])}
	 * nodes.
	 *
	 * @param href The {@link A#href(Object)} attribute. Can be <jk>null</jk>, in which case the attribute is stored with a <jk>null</jk> value (not removed).
	 * @param children The {@link A#children(Object[])} nodes. Must not be <jk>null</jk>.
	 */
	public A(Object href, Object[] children) {
		href(href).children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-download">download</a> attribute.
	 *
	 * <p>
	 * Whether to download the resource instead of navigating to it, and its file name if so.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public A download(Object value) {
		attr("download", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-href">href</a> attribute.
	 *
	 * <p>
	 * Address of the hyperlink.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * 	Can be <jk>null</jk>, in which case the attribute is stored with a <jk>null</jk> value (not removed).
	 * @return This object.
	 */
	public A href(Object value) {
		attrUri("href", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-hreflang">hreflang</a> attribute.
	 *
	 * <p>
	 * Specifies the language of the linked resource. Used for SEO and accessibility purposes.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"en"</js> - English</li>
	 * 	<li><js>"es"</js> - Spanish</li>
	 * 	<li><js>"fr"</js> - French</li>
	 * 	<li><js>"de"</js> - German</li>
	 * 	<li><js>"zh"</js> - Chinese</li>
	 * 	<li><js>"ja"</js> - Japanese</li>
	 * </ul>
	 *
	 * @param value The language code of the linked resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public A hreflang(String value) {
		attr("hreflang", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-rel">rel</a> attribute.
	 *
	 * <p>
	 * Specifies the relationship between the current document and the linked resource.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"alternate"</js> - Alternative version of the page</li>
	 * 	<li><js>"author"</js> - Link to the author of the page</li>
	 * 	<li><js>"bookmark"</js> - Permalink for bookmarking</li>
	 * 	<li><js>"external"</js> - External link</li>
	 * 	<li><js>"help"</js> - Link to help documentation</li>
	 * 	<li><js>"license"</js> - Link to license information</li>
	 * 	<li><js>"next"</js> - Next page in a sequence</li>
	 * 	<li><js>"nofollow"</js> - Don't follow this link for SEO</li>
	 * 	<li><js>"noreferrer"</js> - Don't send referrer information</li>
	 * 	<li><js>"prev"</js> - Previous page in a sequence</li>
	 * 	<li><js>"search"</js> - Link to search functionality</li>
	 * 	<li><js>"tag"</js> - Tag for the current page</li>
	 * </ul>
	 *
	 * @param value The relationship between the document and linked resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public A rel(String value) {
		attr("rel", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-target">target</a> attribute.
	 *
	 * <p>
	 * Specifies where to open the linked resource when the link is clicked.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"_blank"</js> - Open in a new window/tab</li>
	 * 	<li><js>"_self"</js> - Open in the same frame (default)</li>
	 * 	<li><js>"_parent"</js> - Open in the parent frame</li>
	 * 	<li><js>"_top"</js> - Open in the full body of the window</li>
	 * 	<li><js>"framename"</js> - Open in a named frame</li>
	 * </ul>
	 *
	 * @param value Where to open the linked resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public A target(String value) {
		attr("target", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the MIME type of the linked resource. Helps browsers determine how to handle the resource.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"text/html"</js> - HTML document</li>
	 * 	<li><js>"text/css"</js> - CSS stylesheet</li>
	 * 	<li><js>"application/pdf"</js> - PDF document</li>
	 * 	<li><js>"image/png"</js> - PNG image</li>
	 * 	<li><js>"application/zip"</js> - ZIP archive</li>
	 * </ul>
	 *
	 * @param value The MIME type of the linked resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public A type(String value) {
		attr("type", value);
		return this;
	}
}