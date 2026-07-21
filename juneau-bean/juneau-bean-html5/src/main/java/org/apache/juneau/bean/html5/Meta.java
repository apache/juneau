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

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-meta-element">&lt;meta&gt;</a>
 * element.
 *
 * <p>
 * The meta element represents metadata about the document. It provides information about the document
 * that is not displayed to users but is used by browsers, search engines, and other web services.
 * Common uses include character encoding, viewport settings, SEO information, and social media tags.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	// Character encoding
 * 	Meta meta1 = meta().charset("utf-8");
 *
 * 	// Viewport for responsive design
 * 	Meta meta2 = meta()
 * 		.name("viewport")
 * 		.content("width=device-width, initial-scale=1.0");
 *
 * 	// SEO description
 * 	Meta meta3 = meta()
 * 		.name("description")
 * 		.content("This is a sample web page with meta information");
 *
 * 	// Open Graph tags for social media
 * 	Meta meta4 = meta()
 * 		.property("og:title")
 * 		.content("My Web Page");
 *
 * 	// HTTP-equiv for cache control
 * 	Meta meta5 = meta()
 * 		.httpequiv("Cache-Control")
 * 		.content("no-cache, no-store, must-revalidate");
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#meta() meta()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "meta")
public class Meta extends HtmlElementVoid<Meta> {

	/**
	 * Creates an empty {@link Meta} element.
	 */
	public Meta() { /* Empty constructor. */ }

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-meta-charset">charset</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the character encoding for the HTML document. Should be placed early in the document head.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"UTF-8"</js> - Unicode UTF-8 encoding (recommended)</li>
	 * 	<li><js>"ISO-8859-1"</js> - Latin-1 encoding</li>
	 * 	<li><js>"windows-1252"</js> - Windows-1252 encoding</li>
	 * </ul>
	 *
	 * @param value The character encoding for the document. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meta charset(String value) {
		attr("charset", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-meta-content">content</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the value associated with the name or http-equiv attribute.
	 * The content varies depending on the type of metadata being defined.
	 *
	 * @param value The metadata value (e.g., description text, viewport settings, etc.). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meta content(String value) {
		attr("content", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-meta-http-equiv">http-equiv</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies a pragma directive that simulates an HTTP header. Used with the content attribute.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"content-type"</js> - Document content type and character encoding</li>
	 * 	<li><js>"refresh"</js> - Page refresh or redirect timing</li>
	 * 	<li><js>"expires"</js> - Document expiration date</li>
	 * 	<li><js>"cache-control"</js> - Caching directives</li>
	 * 	<li><js>"pragma"</js> - Cache control (legacy)</li>
	 * 	<li><js>"set-cookie"</js> - Cookie settings</li>
	 * 	<li><js>"x-ua-compatible"</js> - Browser compatibility mode</li>
	 * </ul>
	 *
	 * @param value The HTTP header name to simulate. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meta httpequiv(String value) {
		attr("http-equiv", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-meta-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the metadata property. Used with the content attribute to define document metadata.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"description"</js> - Page description for search engines</li>
	 * 	<li><js>"keywords"</js> - Keywords for search engines</li>
	 * 	<li><js>"author"</js> - Page author</li>
	 * 	<li><js>"viewport"</js> - Viewport settings for mobile devices</li>
	 * 	<li><js>"robots"</js> - Instructions for search engine crawlers</li>
	 * </ul>
	 *
	 * @param value The name of the metadata property. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Meta name(String value) {
		attr("name", value);
		return this;
	}

}