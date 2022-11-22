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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h6&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="h6")
@FluentSetters
public class H6 extends HtmlElementMixed {

	/**
	 * Creates an empty {@link H6} element.
	 */
	public H6() {}

	/**
	 * Creates an {@link H6} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H6(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 accesskey(String accesskey) {
		super.accesskey(accesskey);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 contenteditable(Object contenteditable) {
		super.contenteditable(contenteditable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 hidden(Object hidden) {
		super.hidden(hidden);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 id(String id) {
		super.id(id);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 lang(String lang) {
		super.lang(lang);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onabort(String onabort) {
		super.onabort(onabort);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onblur(String onblur) {
		super.onblur(onblur);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oncancel(String oncancel) {
		super.oncancel(oncancel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oncanplay(String oncanplay) {
		super.oncanplay(oncanplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oncanplaythrough(String oncanplaythrough) {
		super.oncanplaythrough(oncanplaythrough);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onchange(String onchange) {
		super.onchange(onchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onclick(String onclick) {
		super.onclick(onclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oncuechange(String oncuechange) {
		super.oncuechange(oncuechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 ondblclick(String ondblclick) {
		super.ondblclick(ondblclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 ondurationchange(String ondurationchange) {
		super.ondurationchange(ondurationchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onemptied(String onemptied) {
		super.onemptied(onemptied);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onended(String onended) {
		super.onended(onended);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onerror(String onerror) {
		super.onerror(onerror);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onfocus(String onfocus) {
		super.onfocus(onfocus);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oninput(String oninput) {
		super.oninput(oninput);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 oninvalid(String oninvalid) {
		super.oninvalid(oninvalid);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onkeydown(String onkeydown) {
		super.onkeydown(onkeydown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onkeypress(String onkeypress) {
		super.onkeypress(onkeypress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onkeyup(String onkeyup) {
		super.onkeyup(onkeyup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onload(String onload) {
		super.onload(onload);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onloadeddata(String onloadeddata) {
		super.onloadeddata(onloadeddata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onloadedmetadata(String onloadedmetadata) {
		super.onloadedmetadata(onloadedmetadata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onloadstart(String onloadstart) {
		super.onloadstart(onloadstart);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmousedown(String onmousedown) {
		super.onmousedown(onmousedown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmouseenter(String onmouseenter) {
		super.onmouseenter(onmouseenter);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmouseleave(String onmouseleave) {
		super.onmouseleave(onmouseleave);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmousemove(String onmousemove) {
		super.onmousemove(onmousemove);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmouseout(String onmouseout) {
		super.onmouseout(onmouseout);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmouseover(String onmouseover) {
		super.onmouseover(onmouseover);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmouseup(String onmouseup) {
		super.onmouseup(onmouseup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onmousewheel(String onmousewheel) {
		super.onmousewheel(onmousewheel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onpause(String onpause) {
		super.onpause(onpause);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onplay(String onplay) {
		super.onplay(onplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onplaying(String onplaying) {
		super.onplaying(onplaying);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onprogress(String onprogress) {
		super.onprogress(onprogress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onratechange(String onratechange) {
		super.onratechange(onratechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onreset(String onreset) {
		super.onreset(onreset);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onresize(String onresize) {
		super.onresize(onresize);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onscroll(String onscroll) {
		super.onscroll(onscroll);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onseeked(String onseeked) {
		super.onseeked(onseeked);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onseeking(String onseeking) {
		super.onseeking(onseeking);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onselect(String onselect) {
		super.onselect(onselect);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onshow(String onshow) {
		super.onshow(onshow);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onstalled(String onstalled) {
		super.onstalled(onstalled);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onsubmit(String onsubmit) {
		super.onsubmit(onsubmit);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onsuspend(String onsuspend) {
		super.onsuspend(onsuspend);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 ontimeupdate(String ontimeupdate) {
		super.ontimeupdate(ontimeupdate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 ontoggle(String ontoggle) {
		super.ontoggle(ontoggle);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onvolumechange(String onvolumechange) {
		super.onvolumechange(onvolumechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 onwaiting(String onwaiting) {
		super.onwaiting(onwaiting);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 spellcheck(Object spellcheck) {
		super.spellcheck(spellcheck);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 style(String style) {
		super.style(style);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 tabindex(Object tabindex) {
		super.tabindex(tabindex);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 title(String title) {
		super.title(title);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public H6 translate(Object translate) {
		super.translate(translate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementMixed */
	public H6 child(Object child) {
		super.child(child);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementMixed */
	public H6 children(Object...children) {
		super.children(children);
		return this;
	}

	// </FluentSetters>
}
