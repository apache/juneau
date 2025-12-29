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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-area-element">&lt;area&gt;</a>
 * element.
 *
 * <p>
 * The area element defines a clickable region within an image map. It is used in conjunction with
 * the map element to create interactive images with multiple clickable areas, each linking to
 * different destinations.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Rectangular clickable area</jc>
 * 	Area <jv>area1</jv> = <jsm>area</jsm>().shape(<js>"rect"</js>).coords(<js>"0,0,100,50"</js>).href(<js>"https://example.com/page1"</js>);
 *
 * 	<jc>// Circular clickable area</jc>
 * 	Area <jv>area2</jv> = <jsm>area</jsm>().shape(<js>"circle"</js>).coords(<js>"150,75,50"</js>).href(<js>"https://example.com/page2"</js>);
 *
 * 	<jc>// Area with alternative text and target</jc>
 * 	Area <jv>area3</jv> = <jsm>area</jsm>().shape(<js>"rect"</js>).coords(<js>"200,0,300,100"</js>).href(<js>"https://example.com/page3"</js>)
 * 		.alt(<js>"Click here for more info"</js>)
 * 		.target(<js>"_blank"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#area() area()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "area")
public class Area extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Area} element.
	 */
	public Area() {}

	/**
	 * Creates an {@link Area} element with the specified {@link Area#shape(String)}, {@link Area#coords(String)},
	 * and {@link Area#href(Object)} attributes.
	 *
	 * @param shape The {@link Area#shape(String)} attribute.
	 * @param coords The {@link Area#coords(String)} attribute.
	 * @param href The {@link Area#href(Object)} attribute.
	 */
	public Area(String shape, String coords, Object href) {
		shape(shape).coords(coords).href(href);
	}

	@Override /* Overridden from HtmlElement */
	public Area _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-alt">alt</a> attribute.
	 *
	 * <p>
	 * Specifies alternative text for the area. This text is displayed when the image map cannot be loaded
	 * and is used by screen readers for accessibility.
	 *
	 * <p>
	 * The alt text should be descriptive and convey the same information as the clickable area.
	 *
	 * @param value Alternative text for the area.
	 * @return This object.
	 */
	public Area alt(String value) {
		attr("alt", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-coords">coords</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the coordinates of the clickable area within the image map. The format depends on the shape:
	 *
	 * <p>
	 * Coordinate formats:
	 * <ul>
	 * 	<li><js>"rect"</js> - x1,y1,x2,y2 (top-left and bottom-right corners)</li>
	 * 	<li><js>"circle"</js> - x,y,radius (center point and radius)</li>
	 * 	<li><js>"poly"</js> - x1,y1,x2,y2,x3,y3,... (polygon vertices)</li>
	 * 	<li><js>"default"</js> - No coordinates needed</li>
	 * </ul>
	 *
	 * @param value The coordinates defining the clickable area.
	 * @return This object.
	 */
	public Area coords(String value) {
		attr("coords", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area dir(String value) {
		super.dir(value);
		return this;
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
	 * @return This object.
	 */
	public Area download(Object value) {
		attr("download", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area hidden(Object value) {
		super.hidden(value);
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
	 * @return This object.
	 */
	public Area href(Object value) {
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
	 * @param value The language code of the linked resource.
	 * @return This object.
	 */
	public Area hreflang(String value) {
		attr("hreflang", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area onwaiting(String value) {
		super.onwaiting(value);
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
	 *  	<li><js>"alternate"</js> - Alternative version of the page</li>
	 *  	<li><js>"author"</js> - Link to the author of the page</li>
	 *  	<li><js>"bookmark"</js> - Permalink for bookmarking</li>
	 *  	<li><js>"external"</js> - External link</li>
	 *  	<li><js>"help"</js> - Link to help documentation</li>
	 *  	<li><js>"license"</js> - Link to license information</li>
	 *  	<li><js>"next"</js> - Next page in a sequence</li>
	 *  	<li><js>"nofollow"</js> - Don't follow this link for SEO</li>
	 *  	<li><js>"noreferrer"</js> - Don't send referrer information</li>
	 *  	<li><js>"prev"</js> - Previous page in a sequence</li>
	 *  	<li><js>"search"</js> - Link to search functionality</li>
	 *  	<li><js>"tag"</js> - Tag for the current page</li>
	 * </ul>
	 *
	 * @param value The relationship between the document and linked resource.
	 * @return This object.
	 */
	public Area rel(String value) {
		attr("rel", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-shape">shape</a> attribute.
	 *
	 * <p>
	 * Specifies the shape of the clickable area in an image map.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 *  	<li><js>"rect"</js> - Rectangular area (default)</li>
	 *  	<li><js>"circle"</js> - Circular area</li>
	 *  	<li><js>"poly"</js> - Polygonal area</li>
	 *  	<li><js>"default"</js> - Entire image area</li>
	 * </ul>
	 *
	 * @param value The shape of the clickable area.
	 * @return This object.
	 */
	public Area shape(String value) {
		attr("shape", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-target">target</a> attribute.
	 *
	 * <p>
	 * Specifies where to open the linked resource when the area is clicked.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 *  	<li><js>"_blank"</js> - Open in a new window/tab</li>
	 *  	<li><js>"_self"</js> - Open in the same frame (default)</li>
	 *  	<li><js>"_parent"</js> - Open in the parent frame</li>
	 *  	<li><js>"_top"</js> - Open in the full body of the window</li>
	 *  	<li><js>"framename"</js> - Open in a named frame</li>
	 * </ul>
	 *
	 * @param value Where to open the linked resource.
	 * @return This object.
	 */
	public Area target(String value) {
		attr("target", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Area translate(Object value) {
		super.translate(value);
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
	 *  	<li><js>"text/html"</js> - HTML document</li>
	 *  	<li><js>"text/css"</js> - CSS stylesheet</li>
	 *  	<li><js>"application/pdf"</js> - PDF document</li>
	 *  	<li><js>"image/png"</js> - PNG image</li>
	 *  	<li><js>"application/zip"</js> - ZIP archive</li>
	 * </ul>
	 *
	 * @param value The MIME type of the linked resource.
	 * @return This object.
	 */
	public Area type(String value) {
		attr("type", value);
		return this;
	}
}