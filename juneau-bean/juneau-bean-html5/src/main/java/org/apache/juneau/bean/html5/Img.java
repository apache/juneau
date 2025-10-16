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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-img-element">&lt;img&gt;</a>
 * element.
 *
 * <p>
 * The img element represents an image in the document. It is a void element that embeds an image
 * into the page. The alt attribute is required for accessibility, providing alternative text
 * for screen readers and when images cannot be displayed.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple image with alt text</jc>
 * 	Img <jv>img1</jv> = <jsm>img</jsm>()
 * 		.src(<js>"photo.jpg"</js>)
 * 		.alt(<js>"A beautiful sunset over the mountains"</js>);
 *
 * 	<jc>// Image with dimensions and styling</jc>
 * 	Img <jv>img2</jv> = <jsm>img</jsm>()
 * 		.src(<js>"logo.png"</js>)
 * 		.alt(<js>"Company Logo"</js>)
 * 		.width(<js>"200"</js>)
 * 		.height(<js>"100"</js>)
 * 		._class(<js>"logo"</js>);
 *
 * 	<jc>// Image with CORS and image map</jc>
 * 	Img <jv>img3</jv> = <jsm>img</jsm>()
 * 		.src(<js>"https://example.com/image.jpg"</js>)
 * 		.alt(<js>"Interactive image"</js>)
 * 		.crossorigin(<js>"anonymous"</js>)
 * 		.usemap(<js>"#imagemap"</js>);
 *
 * 	<jc>// Responsive image with multiple sources</jc>
 * 	Img <jv>img4</jv> = <jsm>img</jsm>()
 * 		.src(<js>"image-800w.jpg"</js>)
 * 		.alt(<js>"Responsive image"</js>)
 * 		.sizes(<js>"(max-width: 600px) 100vw, 50vw"</js>)
 * 		.srcset(<js>"image-400w.jpg 400w, image-800w.jpg 800w"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#img() img()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="img")
public class Img extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Img} element.
	 */
	public Img() {}

	/**
	 * Creates an {@link Img} element with the specified {@link Img#src(Object)} attribute.
	 *
	 * @param src The {@link Img#src(Object)} attribute.
	 */
	public Img(Object src) {
		src(src);
	}

	@Override /* Overridden from HtmlElement */
	public Img _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-alt">alt</a> attribute.
	 *
	 * <p>
	 * Specifies alternative text for the image. This text is displayed when the image cannot be loaded
	 * and is used by screen readers for accessibility.
	 *
	 * <p>
	 * The alt text should be descriptive and convey the same information as the image.
	 *
	 * @param value Alternative text for the image.
	 * @return This object.
	 */
	public Img alt(String value) {
		attr("alt", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-crossorigin">crossorigin</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies how the element handles cross-origin requests for CORS (Cross-Origin Resource Sharing).
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"anonymous"</js> - Cross-origin requests are made without credentials</li>
	 * 	<li><js>"use-credentials"</js> - Cross-origin requests include credentials</li>
	 * </ul>
	 *
	 * @param value How to handle cross-origin requests.
	 * @return This object.
	 */
	public Img crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}
	@Override /* Overridden from HtmlElement */
	public Img dir(String value) {
		super.dir(value);
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
	 * @return This object.
	 */
	public Img height(Object value) {
		attr("height", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img id(String value) {
		super.id(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-ismap">ismap</a> attribute.
	 *
	 * <p>
	 * Whether the image is a server-side image map.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"ismap"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Img ismap(Object value) {
		attr("ismap", deminimize(value, "ismap"));
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-src">src</a> attribute.
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
	 * @return This object.
	 */
	public Img src(Object value) {
		attrUri("src", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Img translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-hyperlink-usemap">usemap</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the name of an image map to use with this image. The value should correspond to
	 * the name attribute of a map element that defines clickable areas on the image.
	 *
	 * <p>
	 * The value should start with "#" followed by the name of the map element.
	 *
	 * @param value The name of the image map to use (e.g., "#mymap").
	 * @return This object.
	 */
	public Img usemap(String value) {
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
	 * @return This object.
	 */
	public Img width(Object value) {
		attr("width", value);
		return this;
	}
}