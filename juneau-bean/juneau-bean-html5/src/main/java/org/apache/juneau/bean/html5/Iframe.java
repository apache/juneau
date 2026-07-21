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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-iframe-element">&lt;iframe&gt;</a>
 * element.
 *
 * <p>
 * The iframe element represents a nested browsing context, embedding another HTML page into the
 * current page. It is commonly used to embed external content such as videos, maps, or other
 * web applications. The sandbox attribute can be used to restrict the capabilities of the
 * embedded content for security purposes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple iframe embedding external content</jc>
 * 	Iframe <jv>iframe1</jv> = <jsm>iframe</jsm>()
 * 		.src(<js>"https://example.com/embed"</js>)
 * 		.width(<js>"800"</js>)
 * 		.height(<js>"600"</js>);
 *
 * 	<jc>// Iframe with sandbox restrictions</jc>
 * 	Iframe <jv>iframe2</jv> = <jsm>iframe</jsm>()
 * 		.src(<js>"https://example.com/untrusted"</js>)
 * 		.sandbox(<js>"allow-scripts allow-same-origin"</js>)
 * 		.width(<js>"400"</js>)
 * 		.height(<js>"300"</js>);
 *
 * 	<jc>// Iframe with inline content</jc>
 * 	Iframe <jv>iframe3</jv> = <jsm>iframe</jsm>()
 * 		.srcdoc(<js>"&lt;h1&gt;Inline Content&lt;/h1&gt;&lt;p&gt;This content is embedded directly.&lt;/p&gt;"</js>)
 * 		.width(<js>"500"</js>)
 * 		.height(<js>"200"</js>);
 *
 * 	<jc>// Iframe with name for targeting</jc>
 * 	Iframe <jv>iframe4</jv> = <jsm>iframe</jsm>()
 * 		.name(<js>"contentFrame"</js>)
 * 		.src(<js>"https://example.com/content"</js>)
 * 		.width(<js>"100%"</js>)
 * 		.height(<js>"400"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#iframe() iframe()}
 * 		<li class='jm'>{@link HtmlBuilder#iframe(Object...) iframe(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "iframe")
public class Iframe extends HtmlElementMixed<Iframe> {

	/**
	 * Creates an empty {@link Iframe} element.
	 */
	public Iframe() {}

	/**
	 * Creates an {@link Iframe} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Iframe(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height">height</a>
	 * attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Iframe height(Object value) {
		attr("height", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the iframe. This name can be used as the target for links
	 * and forms, allowing content to be loaded into the iframe.
	 *
	 * <p>
	 * The name should be unique within the document.
	 *
	 * @param value The name of the iframe for targeting. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Iframe name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-sandbox">sandbox</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies security restrictions for the iframe content. Multiple restrictions can be
	 * specified as a space-separated list.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"allow-scripts"</js> - Allow JavaScript execution</li>
	 * 	<li><js>"allow-same-origin"</js> - Allow same-origin requests</li>
	 * 	<li><js>"allow-forms"</js> - Allow form submission</li>
	 * 	<li><js>"allow-popups"</js> - Allow popup windows</li>
	 * 	<li><js>"allow-top-navigation"</js> - Allow navigation of top-level browsing context</li>
	 * </ul>
	 *
	 * @param value Security restrictions for the iframe content. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Iframe sandbox(String value) {
		attr("sandbox", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-src">src</a> attribute.
	 *
	 * <p>
	 * Address of the resource.
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
	 * 	Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Iframe src(Object value) {
		attrUri("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-iframe-srcdoc">srcdoc</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the HTML content to be displayed in the iframe. This content is rendered
	 * directly within the iframe without requiring a separate HTTP request.
	 *
	 * <p>
	 * The content should be valid HTML that will be displayed in the iframe.
	 *
	 * @param value The HTML content to display in the iframe. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Iframe srcdoc(String value) {
		attr("srcdoc", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width">width</a> attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Iframe width(Object value) {
		attr("width", value);
		return this;
	}
}