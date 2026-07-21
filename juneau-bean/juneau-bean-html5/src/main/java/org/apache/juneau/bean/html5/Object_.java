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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-object-element">&lt;object&gt;</a>
 * element.
 *
 * <p>
 * The object element represents an external resource, which can be treated as an image, a nested browsing context,
 * or a resource to be handled by a plugin. It is commonly used to embed multimedia content such as Flash applications,
 * PDFs, images, videos, or other HTML documents. While historically used for plugins, modern web development often
 * prefers more specific elements like img, video, audio, or iframe when appropriate.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// PDF document</jc>
 * 	Object_ <jv>pdf</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/documents/manual.pdf"</js>)
 * 		.type(<js>"application/pdf"</js>)
 * 		.width(<js>"600"</js>)
 * 		.height(<js>"800"</js>)
 * 		.children(<jsm>p</jsm>(<js>"Your browser doesn't support PDF viewing."</js>));
 *
 * 	<jc>// SVG image</jc>
 * 	Object_ <jv>svg</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/images/diagram.svg"</js>)
 * 		.type(<js>"image/svg+xml"</js>)
 * 		.width(<js>"400"</js>)
 * 		.height(<js>"300"</js>);
 *
 * 	<jc>// Embedded HTML page</jc>
 * 	Object_ <jv>html</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/external/page.html"</js>)
 * 		.type(<js>"text/html"</js>)
 * 		.width(<js>"100%"</js>)
 * 		.height(<js>"500"</js>);
 *
 * 	<jc>// Flash content (legacy)</jc>
 * 	Object_ <jv>flash</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/media/animation.swf"</js>)
 * 		.type(<js>"application/x-shockwave-flash"</js>)
 * 		.width(<js>"800"</js>)
 * 		.height(<js>"600"</js>)
 * 		.children(
 * 			<jsm>param</jsm>().name(<js>"quality"</js>).value(<js>"high"</js>),
 * 			<jsm>param</jsm>().name(<js>"bgcolor"</js>).value(<js>"#ffffff"</js>),
 * 			<jsm>p</jsm>(<js>"Flash is not supported by your browser."</js>)
 * 		);
 *
 * 	<jc>// Image with fallback</jc>
 * 	Object_ <jv>image</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/images/photo.jpg"</js>)
 * 		.type(<js>"image/jpeg"</js>)
 * 		.width(<js>"640"</js>)
 * 		.height(<js>"480"</js>)
 * 		.children(<jsm>img</jsm>().src(<js>"/images/fallback.jpg"</js>).alt(<js>"Photo"</js>));
 *
 * 	<jc>// With usemap for image map</jc>
 * 	Object_ <jv>mapped</jv> = <jsm>object</jsm>()
 * 		.data(<js>"/images/map.jpg"</js>)
 * 		.type(<js>"image/jpeg"</js>)
 * 		.usemap(<js>"#imagemap"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#object() object()}
 * 		<li class='jm'>{@link HtmlBuilder#object(Object...) object(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "object")
@SuppressWarnings({
	"java:S100", // Class name uses underscore suffix to avoid conflict with Object
	"java:S101" // Class name uses underscore suffix to avoid conflict with Object
})
public class Object_ extends HtmlElementMixed<Object_> {

	/**
	 * Creates an empty {@link Object_} element.
	 */
	public Object_() {}

	/**
	 * Creates an {@link Object_} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Object_(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-data">data</a> attribute.
	 *
	 * <p>
	 * Specifies the URL of the resource to be embedded. This is the primary source of content
	 * for the object element.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value The URL of the resource to be embedded. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Object_ data(String value) {
		attr("data", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the control with a form element.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Object_ form(String value) {
		attr("form", value);
		return this;
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
	public Object_ height(Object value) {
		attr("height", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-name">name</a> attribute.
	 *
	 * <p>
	 * Name of nested browsing context.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Object_ name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-type">type</a> attribute.
	 *
	 * <p>
	 * Type of embedded resource.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Object_ type(String value) {
		attr("type", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-typemustmatch">typemustmatch</a>
	 * attribute.
	 *
	 * <p>
	 * Whether the type attribute and the Content-Type value need to match for the resource to be used.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Object_ typemustmatch(Object value) {
		attr("typemustmatch", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-hyperlink-usemap">usemap</a>
	 * attribute.
	 *
	 * <p>
	 * Name of image map to use.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Object_ usemap(String value) {
		attr("usemap", value);
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
	public Object_ width(Object value) {
		attr("width", value);
		return this;
	}
}