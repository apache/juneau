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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-ruby-element">&lt;ruby&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Html5">Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="ruby")
@FluentSetters
public class Ruby extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Ruby} element.
	 */
	public Ruby() {}

	/**
	 * Creates a {@link Ruby} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Ruby(Object...children) {
		children(children);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby accesskey(String accesskey) {
		super.accesskey(accesskey);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby contenteditable(Object contenteditable) {
		super.contenteditable(contenteditable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby dir(String dir) {
		super.dir(dir);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby hidden(Object hidden) {
		super.hidden(hidden);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby id(String id) {
		super.id(id);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby lang(String lang) {
		super.lang(lang);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onabort(String onabort) {
		super.onabort(onabort);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onblur(String onblur) {
		super.onblur(onblur);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oncancel(String oncancel) {
		super.oncancel(oncancel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oncanplay(String oncanplay) {
		super.oncanplay(oncanplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oncanplaythrough(String oncanplaythrough) {
		super.oncanplaythrough(oncanplaythrough);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onchange(String onchange) {
		super.onchange(onchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onclick(String onclick) {
		super.onclick(onclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oncuechange(String oncuechange) {
		super.oncuechange(oncuechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby ondblclick(String ondblclick) {
		super.ondblclick(ondblclick);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby ondurationchange(String ondurationchange) {
		super.ondurationchange(ondurationchange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onemptied(String onemptied) {
		super.onemptied(onemptied);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onended(String onended) {
		super.onended(onended);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onerror(String onerror) {
		super.onerror(onerror);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onfocus(String onfocus) {
		super.onfocus(onfocus);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oninput(String oninput) {
		super.oninput(oninput);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby oninvalid(String oninvalid) {
		super.oninvalid(oninvalid);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onkeydown(String onkeydown) {
		super.onkeydown(onkeydown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onkeypress(String onkeypress) {
		super.onkeypress(onkeypress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onkeyup(String onkeyup) {
		super.onkeyup(onkeyup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onload(String onload) {
		super.onload(onload);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onloadeddata(String onloadeddata) {
		super.onloadeddata(onloadeddata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onloadedmetadata(String onloadedmetadata) {
		super.onloadedmetadata(onloadedmetadata);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onloadstart(String onloadstart) {
		super.onloadstart(onloadstart);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmousedown(String onmousedown) {
		super.onmousedown(onmousedown);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmouseenter(String onmouseenter) {
		super.onmouseenter(onmouseenter);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmouseleave(String onmouseleave) {
		super.onmouseleave(onmouseleave);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmousemove(String onmousemove) {
		super.onmousemove(onmousemove);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmouseout(String onmouseout) {
		super.onmouseout(onmouseout);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmouseover(String onmouseover) {
		super.onmouseover(onmouseover);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmouseup(String onmouseup) {
		super.onmouseup(onmouseup);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onmousewheel(String onmousewheel) {
		super.onmousewheel(onmousewheel);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onpause(String onpause) {
		super.onpause(onpause);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onplay(String onplay) {
		super.onplay(onplay);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onplaying(String onplaying) {
		super.onplaying(onplaying);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onprogress(String onprogress) {
		super.onprogress(onprogress);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onratechange(String onratechange) {
		super.onratechange(onratechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onreset(String onreset) {
		super.onreset(onreset);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onresize(String onresize) {
		super.onresize(onresize);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onscroll(String onscroll) {
		super.onscroll(onscroll);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onseeked(String onseeked) {
		super.onseeked(onseeked);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onseeking(String onseeking) {
		super.onseeking(onseeking);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onselect(String onselect) {
		super.onselect(onselect);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onshow(String onshow) {
		super.onshow(onshow);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onstalled(String onstalled) {
		super.onstalled(onstalled);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onsubmit(String onsubmit) {
		super.onsubmit(onsubmit);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onsuspend(String onsuspend) {
		super.onsuspend(onsuspend);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby ontimeupdate(String ontimeupdate) {
		super.ontimeupdate(ontimeupdate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby ontoggle(String ontoggle) {
		super.ontoggle(ontoggle);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onvolumechange(String onvolumechange) {
		super.onvolumechange(onvolumechange);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby onwaiting(String onwaiting) {
		super.onwaiting(onwaiting);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby spellcheck(Object spellcheck) {
		super.spellcheck(spellcheck);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby style(String style) {
		super.style(style);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby tabindex(Object tabindex) {
		super.tabindex(tabindex);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby title(String title) {
		super.title(title);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElement */
	public Ruby translate(Object translate) {
		super.translate(translate);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementMixed */
	public Ruby child(Object child) {
		super.child(child);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.html5.HtmlElementMixed */
	public Ruby children(Object...children) {
		super.children(children);
		return this;
	}

	// </FluentSetters>
}
