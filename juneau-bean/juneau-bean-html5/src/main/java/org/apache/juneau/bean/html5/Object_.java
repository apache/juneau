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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 		<li class='jm'>{@link HtmlBuilder#object(Object, Object...) object(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="object")
public class Object_ extends HtmlElementMixed {  // NOSONAR - Intentional naming.

	/**
	 * Creates an empty {@link Object_} element.
	 */
	public Object_() {}

	/**
	 * Creates an {@link Object_} element with the specified child nodes.
	 *
	 * @param children The child nodes.
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
	 * @param data The URL of the resource to be embedded.
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
	 * @param form The new value for this attribute.
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
	 * @param height
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
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
	 * @param name The new value for this attribute.
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
	 * @param type The new value for this attribute.
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
	 * @param typemustmatch
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
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
	 * @param usemap The new value for this attribute.
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
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Object_ width(Object value) {
		attr("width", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from HtmlElement */
	public Object_ _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Object_ translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Object_ child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Object_ children(Object...value) {
		super.children(value);
		return this;
	}
}