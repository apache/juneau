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
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 	// Simple image with alt text
 * 	Img img1 = new Img()
 * 		.src("photo.jpg")
 * 		.alt("A beautiful sunset over the mountains");
 * 
 * 	// Image with dimensions and styling
 * 	Img img2 = new Img()
 * 		.src("logo.png")
 * 		.alt("Company Logo")
 * 		.width("200")
 * 		.height("100")
 * 		._class("logo");
 * 
 * 	// Image with CORS and image map
 * 	Img img3 = new Img()
 * 		.src("https://example.com/image.jpg")
 * 		.alt("Interactive image")
 * 		.crossorigin("anonymous")
 * 		.usemap("#imagemap");
 * 
 * 	// Responsive image with multiple sources
 * 	Img img4 = new Img()
 * 		.src("image-800w.jpg")
 * 		.alt("Responsive image")
 * 		.sizes("(max-width: 600px) 100vw, 50vw")
 * 		.srcset("image-400w.jpg 400w, image-800w.jpg 800w");
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="img")
@FluentSetters
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
	 * @param alt Alternative text for the image.
	 * @return This object.
	 */
	public Img alt(String value) {
		attr("alt", value);
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
	 * @param crossorigin How to handle cross-origin requests.
	 * @return This object.
	 */
	public Img crossorigin(String value) {
		attr("crossorigin", value);
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
	public Img height(Object value) {
		attr("height", value);
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
	 * @param ismap
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Img ismap(Object value) {
		attr("ismap", deminimize(value, "ismap"));
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
	 * @param src
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object.
	 */
	public Img src(Object value) {
		attrUri("src", value);
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
	 * @param usemap The name of the image map to use (e.g., "#mymap").
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
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Img width(Object value) {
		attr("width", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Img translate(Object value) {
		super.translate(value);
		return this;
	}

	// </FluentSetters>
}