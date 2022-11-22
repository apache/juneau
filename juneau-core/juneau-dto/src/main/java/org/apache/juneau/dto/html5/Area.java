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
package org.apache.juneau.dto.html5;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-area-element">&lt;area&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="area")
@FluentSetters
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

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-alt">alt</a> attribute.
	 *
	 * <p>
	 * Replacement text for use when images are not available.
	 *
	 * @param alt The new value for this attribute.
	 * @return This object.
	 */
	public final Area alt(String alt) {
		attr("alt", alt);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-coords">coords</a>
	 * attribute.
	 *
	 * <p>
	 * Coordinates for the shape to be created in an image map.
	 *
	 * @param coords The new value for this attribute.
	 * @return This object.
	 */
	public final Area coords(String coords) {
		attr("coords", coords);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-download">download</a> attribute.
	 *
	 * <p>
	 * Whether to download the resource instead of navigating to it, and its file name if so.
	 *
	 * @param download
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Area download(Object download) {
		attr("download", download);
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
	 * @param href
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object.
	 */
	public final Area href(Object href) {
		attrUri("href", href);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-hreflang">hreflang</a> attribute.
	 *
	 * <p>
	 * Language of the linked resource.
	 *
	 * @param hreflang The new value for this attribute.
	 * @return This object.
	 */
	public final Area hreflang(String hreflang) {
		attr("hreflang", hreflang);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-rel">rel</a> attribute.
	 *
	 * <p>
	 * Relationship between the document containing the hyperlink and the destination resource.
	 *
	 * @param rel The new value for this attribute.
	 * @return This object.
	 */
	public final Area rel(String rel) {
		attr("rel", rel);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-shape">shape</a> attribute.
	 *
	 * <p>
	 * The kind of shape to be created in an image map.
	 *
	 * @param shape The new value for this attribute.
	 * @return This object.
	 */
	public final Area shape(String shape) {
		attr("shape", shape);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-target">target</a> attribute.
	 *
	 * <p>
	 * Browsing context for hyperlink navigation.
	 *
	 * @param target The new value for this attribute.
	 * @return This object.
	 */
	public final Area target(String target) {
		attr("target", target);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-type">type</a> attribute.
	 *
	 * <p>
	 * Hint for the type of the referenced resource.
	 *
	 * @param type The new value for this attribute.
	 * @return This object.
	 */
	public final Area type(String type) {
		attr("type", type);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area accesskey(String accesskey) {
		super.accesskey(accesskey);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area contenteditable(Object contenteditable) {
		super.contenteditable(contenteditable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area hidden(Object hidden) {
		super.hidden(hidden);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area id(String id) {
		super.id(id);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area lang(String lang) {
		super.lang(lang);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onabort(String onabort) {
		super.onabort(onabort);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onblur(String onblur) {
		super.onblur(onblur);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oncancel(String oncancel) {
		super.oncancel(oncancel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oncanplay(String oncanplay) {
		super.oncanplay(oncanplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oncanplaythrough(String oncanplaythrough) {
		super.oncanplaythrough(oncanplaythrough);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onchange(String onchange) {
		super.onchange(onchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onclick(String onclick) {
		super.onclick(onclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oncuechange(String oncuechange) {
		super.oncuechange(oncuechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area ondblclick(String ondblclick) {
		super.ondblclick(ondblclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area ondurationchange(String ondurationchange) {
		super.ondurationchange(ondurationchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onemptied(String onemptied) {
		super.onemptied(onemptied);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onended(String onended) {
		super.onended(onended);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onerror(String onerror) {
		super.onerror(onerror);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onfocus(String onfocus) {
		super.onfocus(onfocus);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oninput(String oninput) {
		super.oninput(oninput);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area oninvalid(String oninvalid) {
		super.oninvalid(oninvalid);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onkeydown(String onkeydown) {
		super.onkeydown(onkeydown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onkeypress(String onkeypress) {
		super.onkeypress(onkeypress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onkeyup(String onkeyup) {
		super.onkeyup(onkeyup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onload(String onload) {
		super.onload(onload);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onloadeddata(String onloadeddata) {
		super.onloadeddata(onloadeddata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onloadedmetadata(String onloadedmetadata) {
		super.onloadedmetadata(onloadedmetadata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onloadstart(String onloadstart) {
		super.onloadstart(onloadstart);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmousedown(String onmousedown) {
		super.onmousedown(onmousedown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmouseenter(String onmouseenter) {
		super.onmouseenter(onmouseenter);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmouseleave(String onmouseleave) {
		super.onmouseleave(onmouseleave);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmousemove(String onmousemove) {
		super.onmousemove(onmousemove);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmouseout(String onmouseout) {
		super.onmouseout(onmouseout);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmouseover(String onmouseover) {
		super.onmouseover(onmouseover);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmouseup(String onmouseup) {
		super.onmouseup(onmouseup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onmousewheel(String onmousewheel) {
		super.onmousewheel(onmousewheel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onpause(String onpause) {
		super.onpause(onpause);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onplay(String onplay) {
		super.onplay(onplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onplaying(String onplaying) {
		super.onplaying(onplaying);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onprogress(String onprogress) {
		super.onprogress(onprogress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onratechange(String onratechange) {
		super.onratechange(onratechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onreset(String onreset) {
		super.onreset(onreset);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onresize(String onresize) {
		super.onresize(onresize);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onscroll(String onscroll) {
		super.onscroll(onscroll);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onseeked(String onseeked) {
		super.onseeked(onseeked);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onseeking(String onseeking) {
		super.onseeking(onseeking);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onselect(String onselect) {
		super.onselect(onselect);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onshow(String onshow) {
		super.onshow(onshow);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onstalled(String onstalled) {
		super.onstalled(onstalled);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onsubmit(String onsubmit) {
		super.onsubmit(onsubmit);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onsuspend(String onsuspend) {
		super.onsuspend(onsuspend);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area ontimeupdate(String ontimeupdate) {
		super.ontimeupdate(ontimeupdate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area ontoggle(String ontoggle) {
		super.ontoggle(ontoggle);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onvolumechange(String onvolumechange) {
		super.onvolumechange(onvolumechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area onwaiting(String onwaiting) {
		super.onwaiting(onwaiting);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area spellcheck(Object spellcheck) {
		super.spellcheck(spellcheck);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area style(String style) {
		super.style(style);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area tabindex(Object tabindex) {
		super.tabindex(tabindex);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area title(String title) {
		super.title(title);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Area translate(Object translate) {
		super.translate(translate);
		return this;
	}

	// </FluentSetters>
}
