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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-embed-element">&lt;embed&gt;</a>
 * element.
 *
 * <p>
 * The embed element represents an integration point for an external application or interactive content.
 * It is used to embed content such as Flash applications, PDF documents, or other multimedia content
 * that requires a plugin or external application to display.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Embed a PDF document</jc>
 * 	Embed <jv>embed1</jv> = <jsm>embed</jsm>()
 * 		.src(<js>"document.pdf"</js>)
 * 		.type(<js>"application/pdf"</js>)
 * 		.width(<js>"800"</js>)
 * 		.height(<js>"600"</js>);
 *
 * 	<jc>// Embed a Flash application</jc>
 * 	Embed <jv>embed2</jv> = <jsm>embed</jsm>()
 * 		.src(<js>"game.swf"</js>)
 * 		.type(<js>"application/x-shockwave-flash"</js>)
 * 		.width(<js>"640"</js>)
 * 		.height(<js>"480"</js>);
 *
 * 	<jc>// Embed with fallback content</jc>
 * 	Embed <jv>embed3</jv> = <jsm>embed</jsm>()
 * 		.src(<js>"interactive-content.swf"</js>)
 * 		.type(<js>"application/x-shockwave-flash"</js>)
 * 		.children(
 * 			<jsm>p</jsm>(<js>"Your browser does not support embedded content."</js>)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#embed() embed()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "embed")
public class Embed extends HtmlElementVoid<Embed> {

	/**
	 * Creates an empty {@link Embed} element.
	 */
	public Embed() {}

	/**
	 * Creates an {@link Embed} element with the specified {@link Embed#src(Object)} attribute.
	 *
	 * @param src The {@link Embed#src(Object)} attribute. Can be <jk>null</jk>.
	 */
	public Embed(Object src) {
		src(src);
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
	public Embed height(Object value) {
		attr("height", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-embed-src">src</a> attribute.
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
	public Embed src(Object value) {
		attrUri("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-embed-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the MIME type of the embedded resource. Helps browsers determine how to handle the resource.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"application/pdf"</js> - PDF document</li>
	 * 	<li><js>"application/x-shockwave-flash"</js> - Flash content</li>
	 * 	<li><js>"image/svg+xml"</js> - SVG image</li>
	 * 	<li><js>"video/mp4"</js> - MP4 video</li>
	 * 	<li><js>"audio/mp3"</js> - MP3 audio</li>
	 * </ul>
	 *
	 * @param value The MIME type of the embedded resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Embed type(String value) {
		attr("type", value);
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
	public Embed width(Object value) {
		attr("width", value);
		return this;
	}
}