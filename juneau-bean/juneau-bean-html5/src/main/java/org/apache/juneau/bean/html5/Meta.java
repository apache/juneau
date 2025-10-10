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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 	Meta meta1 = new Meta().charset("utf-8");
 * 
 * 	// Viewport for responsive design
 * 	Meta meta2 = new Meta()
 * 		.name("viewport")
 * 		.content("width=device-width, initial-scale=1.0");
 * 
 * 	// SEO description
 * 	Meta meta3 = new Meta()
 * 		.name("description")
 * 		.content("This is a sample web page with meta information");
 * 
 * 	// Open Graph tags for social media
 * 	Meta meta4 = new Meta()
 * 		.property("og:title")
 * 		.content("My Web Page");
 * 
 * 	// HTTP-equiv for cache control
 * 	Meta meta5 = new Meta()
 * 		.httpequiv("Cache-Control")
 * 		.content("no-cache, no-store, must-revalidate");
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="meta")
@FluentSetters
public class Meta extends HtmlElementVoid {

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
	 * @param charset The character encoding for the document.
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
	 * @param content The metadata value (e.g., description text, viewport settings, etc.).
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
	 * @param httpequiv The HTTP header name to simulate.
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
	 * @param name The name of the metadata property.
	 * @return This object.
	 */
	public Meta name(String value) {
		attr("name", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Meta translate(Object value) {
		super.translate(value);
		return this;
	}

	// </FluentSetters>
}