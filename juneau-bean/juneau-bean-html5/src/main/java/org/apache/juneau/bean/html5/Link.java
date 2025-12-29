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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-link-element">&lt;link&gt;</a>
 * element.
 *
 * <p>
 * The link element specifies relationships between the current document and an external resource.
 * It is most commonly used to link to stylesheets, but can also be used to establish site icons,
 * prefetch resources, define alternate versions of the document, and more. Link elements are typically
 * placed in the head section of an HTML document.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Stylesheet link</jc>
 * 	Link <jv>stylesheet</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"stylesheet"</js>)
 * 		.href(<js>"/css/styles.css"</js>);
 *
 * 	<jc>// Favicon link</jc>
 * 	Link <jv>favicon</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"icon"</js>)
 * 		.type(<js>"image/x-icon"</js>)
 * 		.href(<js>"/favicon.ico"</js>);
 *
 * 	<jc>// Preload resource</jc>
 * 	Link <jv>preload</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"preload"</js>)
 * 		.href(<js>"/fonts/myfont.woff2"</js>)
 * 		._as(<js>"font"</js>)
 * 		.type(<js>"font/woff2"</js>)
 * 		.crossorigin(<js>"anonymous"</js>);
 *
 * 	<jc>// Alternate language version</jc>
 * 	Link <jv>alternate</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"alternate"</js>)
 * 		.href(<js>"/es/page.html"</js>)
 * 		.hreflang(<js>"es"</js>);
 *
 * 	<jc>// Responsive stylesheet with media query</jc>
 * 	Link <jv>print</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"stylesheet"</js>)
 * 		.href(<js>"/css/print.css"</js>)
 * 		.media(<js>"print"</js>);
 *
 * 	<jc>// Canonical URL for SEO</jc>
 * 	Link <jv>canonical</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"canonical"</js>)
 * 		.href(<js>"https://example.com/page"</js>);
 *
 * 	<jc>// DNS prefetch for performance</jc>
 * 	Link <jv>dnsPrefetch</jv> = <jsm>link</jsm>()
 * 		.rel(<js>"dns-prefetch"</js>)
 * 		.href(<js>"https://cdn.example.com"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#link() link()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "link")
public class Link extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Link} element.
	 */
	public Link() {}

	/**
	 * Creates a {@link Link} element with the specified {@link Link#href(Object)} attribute.
	 *
	 * @param href The {@link Link#href(Object)} attribute.
	 */
	public Link(Object href) {
		href(href);
	}

	@Override /* Overridden from HtmlElement */
	public Link _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-crossorigin">crossorigin</a>
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
	public Link crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link hidden(Object value) {
		super.hidden(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-href">href</a> attribute.
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
	public Link href(Object value) {
		attrUri("href", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-hreflang">hreflang</a>
	 * attribute.
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
	public Link hreflang(String value) {
		attr("hreflang", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-media">media</a> attribute.
	 *
	 * <p>
	 * Specifies which media types the linked resource applies to. Used primarily with stylesheets.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"all"</js> - All media types (default)</li>
	 * 	<li><js>"screen"</js> - Computer screens</li>
	 * 	<li><js>"print"</js> - Printers and print preview</li>
	 * 	<li><js>"handheld"</js> - Handheld devices</li>
	 * 	<li><js>"projection"</js> - Projectors</li>
	 * 	<li><js>"(max-width: 768px)"</js> - Media queries</li>
	 * </ul>
	 *
	 * @param value The media types the linked resource applies to.
	 * @return This object.
	 */
	public Link media(String value) {
		attr("media", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-rel">rel</a> attribute.
	 *
	 * <p>
	 * Specifies the relationship between the current document and the linked resource.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"stylesheet"</js> - External CSS stylesheet</li>
	 * 	<li><js>"icon"</js> - Favicon or site icon</li>
	 * 	<li><js>"canonical"</js> - Canonical URL for SEO</li>
	 * 	<li><js>"alternate"</js> - Alternative version of the page</li>
	 * 	<li><js>"preload"</js> - Resource to preload</li>
	 * 	<li><js>"prefetch"</js> - Resource to prefetch</li>
	 * 	<li><js>"dns-prefetch"</js> - DNS lookup to prefetch</li>
	 * 	<li><js>"next"</js> - Next page in a sequence</li>
	 * 	<li><js>"prev"</js> - Previous page in a sequence</li>
	 * </ul>
	 *
	 * @param value The relationship between the document and linked resource.
	 * @return This object.
	 */
	public Link rel(String value) {
		attr("rel", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-link-sizes">sizes</a> attribute.
	 *
	 * <p>
	 * Specifies the sizes of icons for different device contexts. Used with <c>rel="icon"</c> or <c>rel="apple-touch-icon"</c>.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"16x16"</js> - Small favicon</li>
	 * 	<li><js>"32x32"</js> - Standard favicon</li>
	 * 	<li><js>"180x180"</js> - Apple touch icon</li>
	 * 	<li><js>"192x192"</js> - Android icon</li>
	 * 	<li><js>"512x512"</js> - Large icon</li>
	 * 	<li><js>"any"</js> - Any size</li>
	 * </ul>
	 *
	 * @param value The sizes of the linked icon resource.
	 * @return This object.
	 */
	public Link sizes(String value) {
		attr("sizes", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Link translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the MIME type of the linked resource. Helps browsers determine how to handle the resource.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"text/css"</js> - CSS stylesheet</li>
	 * 	<li><js>"text/javascript"</js> - JavaScript file</li>
	 * 	<li><js>"application/json"</js> - JSON data</li>
	 * 	<li><js>"image/png"</js> - PNG image</li>
	 * 	<li><js>"image/jpeg"</js> - JPEG image</li>
	 * 	<li><js>"image/svg+xml"</js> - SVG image</li>
	 * 	<li><js>"font/woff2"</js> - Web font</li>
	 * </ul>
	 *
	 * @param value The MIME type of the linked resource.
	 * @return This object.
	 */
	public Link type(String value) {
		attr("type", value);
		return this;
	}
}