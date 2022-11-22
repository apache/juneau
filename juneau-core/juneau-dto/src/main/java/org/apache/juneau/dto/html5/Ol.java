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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-ol-element">&lt;ol&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="ol")
@FluentSetters
public class Ol extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Ol} element.
	 */
	public Ol() {}

	/**
	 * Creates an {@link Ol} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Ol(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-reversed">reversed</a>
	 * attribute.
	 *
	 * <p>
	 * Number the list backwards..
	 *
	 * @param reversed
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Ol reversed(Object reversed) {
		attr("reversed", deminimize(reversed, "reversed"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-start">start</a> attribute.
	 *
	 * <p>
	 * Ordinal value of the first item.
	 *
	 * @param start
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Ol start(Object start) {
		attr("start", start);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-type">type</a> attribute.
	 *
	 * <p>
	 * Kind of list marker.
	 *
	 * @param type The new value for this attribute.
	 * @return This object.
	 */
	public final Ol type(String type) {
		attr("type", type);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol accesskey(String accesskey) {
		super.accesskey(accesskey);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol contenteditable(Object contenteditable) {
		super.contenteditable(contenteditable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol hidden(Object hidden) {
		super.hidden(hidden);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol id(String id) {
		super.id(id);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol lang(String lang) {
		super.lang(lang);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onabort(String onabort) {
		super.onabort(onabort);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onblur(String onblur) {
		super.onblur(onblur);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oncancel(String oncancel) {
		super.oncancel(oncancel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oncanplay(String oncanplay) {
		super.oncanplay(oncanplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oncanplaythrough(String oncanplaythrough) {
		super.oncanplaythrough(oncanplaythrough);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onchange(String onchange) {
		super.onchange(onchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onclick(String onclick) {
		super.onclick(onclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oncuechange(String oncuechange) {
		super.oncuechange(oncuechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol ondblclick(String ondblclick) {
		super.ondblclick(ondblclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol ondurationchange(String ondurationchange) {
		super.ondurationchange(ondurationchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onemptied(String onemptied) {
		super.onemptied(onemptied);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onended(String onended) {
		super.onended(onended);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onerror(String onerror) {
		super.onerror(onerror);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onfocus(String onfocus) {
		super.onfocus(onfocus);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oninput(String oninput) {
		super.oninput(oninput);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol oninvalid(String oninvalid) {
		super.oninvalid(oninvalid);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onkeydown(String onkeydown) {
		super.onkeydown(onkeydown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onkeypress(String onkeypress) {
		super.onkeypress(onkeypress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onkeyup(String onkeyup) {
		super.onkeyup(onkeyup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onload(String onload) {
		super.onload(onload);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onloadeddata(String onloadeddata) {
		super.onloadeddata(onloadeddata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onloadedmetadata(String onloadedmetadata) {
		super.onloadedmetadata(onloadedmetadata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onloadstart(String onloadstart) {
		super.onloadstart(onloadstart);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmousedown(String onmousedown) {
		super.onmousedown(onmousedown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmouseenter(String onmouseenter) {
		super.onmouseenter(onmouseenter);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmouseleave(String onmouseleave) {
		super.onmouseleave(onmouseleave);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmousemove(String onmousemove) {
		super.onmousemove(onmousemove);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmouseout(String onmouseout) {
		super.onmouseout(onmouseout);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmouseover(String onmouseover) {
		super.onmouseover(onmouseover);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmouseup(String onmouseup) {
		super.onmouseup(onmouseup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onmousewheel(String onmousewheel) {
		super.onmousewheel(onmousewheel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onpause(String onpause) {
		super.onpause(onpause);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onplay(String onplay) {
		super.onplay(onplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onplaying(String onplaying) {
		super.onplaying(onplaying);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onprogress(String onprogress) {
		super.onprogress(onprogress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onratechange(String onratechange) {
		super.onratechange(onratechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onreset(String onreset) {
		super.onreset(onreset);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onresize(String onresize) {
		super.onresize(onresize);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onscroll(String onscroll) {
		super.onscroll(onscroll);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onseeked(String onseeked) {
		super.onseeked(onseeked);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onseeking(String onseeking) {
		super.onseeking(onseeking);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onselect(String onselect) {
		super.onselect(onselect);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onshow(String onshow) {
		super.onshow(onshow);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onstalled(String onstalled) {
		super.onstalled(onstalled);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onsubmit(String onsubmit) {
		super.onsubmit(onsubmit);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onsuspend(String onsuspend) {
		super.onsuspend(onsuspend);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol ontimeupdate(String ontimeupdate) {
		super.ontimeupdate(ontimeupdate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol ontoggle(String ontoggle) {
		super.ontoggle(ontoggle);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onvolumechange(String onvolumechange) {
		super.onvolumechange(onvolumechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol onwaiting(String onwaiting) {
		super.onwaiting(onwaiting);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol spellcheck(Object spellcheck) {
		super.spellcheck(spellcheck);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol style(String style) {
		super.style(style);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol tabindex(Object tabindex) {
		super.tabindex(tabindex);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol title(String title) {
		super.title(title);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ol translate(Object translate) {
		super.translate(translate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementContainer */
	public Ol child(Object child) {
		super.child(child);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementContainer */
	public Ol children(Object...children) {
		super.children(children);
		return this;
	}

	// </FluentSetters>
}
