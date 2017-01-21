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

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Superclass for all HTML elements.
 * <p>
 * These are beans that when serialized using {@link HtmlSerializer} generate valid HTML5 elements.
 */
@org.apache.juneau.html.annotation.Html(asXml=true)
public abstract class HtmlElement {

	final LinkedHashMap<String,Object> attrs = new LinkedHashMap<String,Object>();

	/**
	 * The attributes of this element.
	 * @return The attributes of this element.
	 */
	@Xml(format=ATTRS)
	public LinkedHashMap<String,Object> getAttrs() {
		return attrs;
	}

	/**
	 * Adds an arbitrary attribute to this element.
	 * @param key The attribute name.
	 * @param val The attribute value.
	 *
	 * @return This object (for method chaining).
	 */
	public HtmlElement attr(String key, Object val) {
		this.attrs.put(key, val);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/editing.html#the-accesskey-attribute'>accesskey</a> attribute.
	 * @param accesskey - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement accesskey(String accesskey) {
		attrs.put("accesskey", accesskey);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#classes'>class</a> attribute.
	 * @param _class - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public HtmlElement _class(String _class) {
		attrs.put("class", _class);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/editing.html#attr-contenteditable'>contenteditable</a> attribute.
	 * @param contenteditable - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement contenteditable(String contenteditable) {
		attrs.put("contenteditable", contenteditable);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#the-dir-attribute'>dir</a> attribute.
	 * @param dir - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public HtmlElement dir(String dir) {
		attrs.put("dir", dir);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/editing.html#the-hidden-attribute'>hidden</a> attribute.
	 * @param hidden - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement hidden(String hidden) {
		attrs.put("hidden", hidden);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#the-id-attribute'>id</a> attribute.
	 * @param id - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public HtmlElement id(String id) {
		attrs.put("id", id);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#attr-lang'>lang</a> attribute.
	 * @param lang - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement lang(String lang) {
		attrs.put("lang", lang);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onabort'>onabort</a> attribute.
	 * @param onabort - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onabort(String onabort) {
		attrs.put("onabort", onabort);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onblur'>onblur</a> attribute.
	 * @param onblur - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onblur(String onblur) {
		attrs.put("onblur", onblur);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oncancel'>oncancel</a> attribute.
	 * @param oncancel - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oncancel(String oncancel) {
		attrs.put("oncancel", oncancel);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oncanplay'>oncanplay</a> attribute.
	 * @param oncanplay - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oncanplay(String oncanplay) {
		attrs.put("oncanplay", oncanplay);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oncanplaythrough'>oncanplaythrough</a> attribute.
	 * @param oncanplaythrough - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oncanplaythrough(String oncanplaythrough) {
		attrs.put("oncanplaythrough", oncanplaythrough);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onchange'>onchange</a> attribute.
	 * @param onchange - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onchange(String onchange) {
		attrs.put("onchange", onchange);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onclick'>onclick</a> attribute.
	 * @param onclick - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onclick(String onclick) {
		attrs.put("onclick", onclick);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oncuechange'>oncuechange</a> attribute.
	 * @param oncuechange - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oncuechange(String oncuechange) {
		attrs.put("oncuechange", oncuechange);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-ondblclick'>ondblclick</a> attribute.
	 * @param ondblclick - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement ondblclick(String ondblclick) {
		attrs.put("ondblclick", ondblclick);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-ondurationchange'>ondurationchange</a> attribute.
	 * @param ondurationchange - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement ondurationchange(String ondurationchange) {
		attrs.put("ondurationchange", ondurationchange);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onemptied'>onemptied</a> attribute.
	 * @param onemptied - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onemptied(String onemptied) {
		attrs.put("onemptied", onemptied);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onended'>onended</a> attribute.
	 * @param onended - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onended(String onended) {
		attrs.put("onended", onended);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onerror'>onerror</a> attribute.
	 * @param onerror - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onerror(String onerror) {
		attrs.put("onerror", onerror);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onfocus'>onfocus</a> attribute.
	 * @param onfocus - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onfocus(String onfocus) {
		attrs.put("onfocus", onfocus);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oninput'>oninput</a> attribute.
	 * @param oninput - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oninput(String oninput) {
		attrs.put("oninput", oninput);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-oninvalid'>oninvalid</a> attribute.
	 * @param oninvalid - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement oninvalid(String oninvalid) {
		attrs.put("oninvalid", oninvalid);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onkeydown'>onkeydown</a> attribute.
	 * @param onkeydown - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onkeydown(String onkeydown) {
		attrs.put("onkeydown", onkeydown);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onkeypress'>onkeypress</a> attribute.
	 * @param onkeypress - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onkeypress(String onkeypress) {
		attrs.put("onkeypress", onkeypress);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onkeyup'>onkeyup</a> attribute.
	 * @param onkeyup - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onkeyup(String onkeyup) {
		attrs.put("onkeyup", onkeyup);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onload'>onload</a> attribute.
	 * @param onload - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onload(String onload) {
		attrs.put("onload", onload);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onloadeddata'>onloadeddata</a> attribute.
	 * @param onloadeddata - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onloadeddata(String onloadeddata) {
		attrs.put("onloadeddata", onloadeddata);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onloadedmetadata'>onloadedmetadata</a> attribute.
	 * @param onloadedmetadata - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onloadedmetadata(String onloadedmetadata) {
		attrs.put("onloadedmetadata", onloadedmetadata);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onloadstart'>onloadstart</a> attribute.
	 * @param onloadstart - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onloadstart(String onloadstart) {
		attrs.put("onloadstart", onloadstart);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmousedown'>onmousedown</a> attribute.
	 * @param onmousedown - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmousedown(String onmousedown) {
		attrs.put("onmousedown", onmousedown);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmouseenter'>onmouseenter</a> attribute.
	 * @param onmouseenter - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmouseenter(String onmouseenter) {
		attrs.put("onmouseenter", onmouseenter);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmouseleave'>onmouseleave</a> attribute.
	 * @param onmouseleave - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmouseleave(String onmouseleave) {
		attrs.put("onmouseleave", onmouseleave);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmousemove'>onmousemove</a> attribute.
	 * @param onmousemove - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmousemove(String onmousemove) {
		attrs.put("onmousemove", onmousemove);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmouseout'>onmouseout</a> attribute.
	 * @param onmouseout - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmouseout(String onmouseout) {
		attrs.put("onmouseout", onmouseout);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmouseover'>onmouseover</a> attribute.
	 * @param onmouseover - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmouseover(String onmouseover) {
		attrs.put("onmouseover", onmouseover);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmouseup'>onmouseup</a> attribute.
	 * @param onmouseup - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmouseup(String onmouseup) {
		attrs.put("onmouseup", onmouseup);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onmousewheel'>onmousewheel</a> attribute.
	 * @param onmousewheel - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onmousewheel(String onmousewheel) {
		attrs.put("onmousewheel", onmousewheel);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onpause'>onpause</a> attribute.
	 * @param onpause - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onpause(String onpause) {
		attrs.put("onpause", onpause);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onplay'>onplay</a> attribute.
	 * @param onplay - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onplay(String onplay) {
		attrs.put("onplay", onplay);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onplaying'>onplaying</a> attribute.
	 * @param onplaying - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onplaying(String onplaying) {
		attrs.put("onplaying", onplaying);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onprogress'>onprogress</a> attribute.
	 * @param onprogress - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onprogress(String onprogress) {
		attrs.put("onprogress", onprogress);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onratechange'>onratechange</a> attribute.
	 * @param onratechange - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onratechange(String onratechange) {
		attrs.put("onratechange", onratechange);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onreset'>onreset</a> attribute.
	 * @param onreset - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onreset(String onreset) {
		attrs.put("onreset", onreset);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onresize'>onresize</a> attribute.
	 * @param onresize - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onresize(String onresize) {
		attrs.put("onresize", onresize);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onscroll'>onscroll</a> attribute.
	 * @param onscroll - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onscroll(String onscroll) {
		attrs.put("onscroll", onscroll);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onseeked'>onseeked</a> attribute.
	 * @param onseeked - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onseeked(String onseeked) {
		attrs.put("onseeked", onseeked);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onseeking'>onseeking</a> attribute.
	 * @param onseeking - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onseeking(String onseeking) {
		attrs.put("onseeking", onseeking);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onselect'>onselect</a> attribute.
	 * @param onselect - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onselect(String onselect) {
		attrs.put("onselect", onselect);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onshow'>onshow</a> attribute.
	 * @param onshow - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onshow(String onshow) {
		attrs.put("onshow", onshow);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onstalled'>onstalled</a> attribute.
	 * @param onstalled - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onstalled(String onstalled) {
		attrs.put("onstalled", onstalled);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onsubmit'>onsubmit</a> attribute.
	 * @param onsubmit - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onsubmit(String onsubmit) {
		attrs.put("onsubmit", onsubmit);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onsuspend'>onsuspend</a> attribute.
	 * @param onsuspend - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onsuspend(String onsuspend) {
		attrs.put("onsuspend", onsuspend);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-ontimeupdate'>ontimeupdate</a> attribute.
	 * @param ontimeupdate - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement ontimeupdate(String ontimeupdate) {
		attrs.put("ontimeupdate", ontimeupdate);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-ontoggle'>ontoggle</a> attribute.
	 * @param ontoggle - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement ontoggle(String ontoggle) {
		attrs.put("ontoggle", ontoggle);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onvolumechange'>onvolumechange</a> attribute.
	 * @param onvolumechange - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onvolumechange(String onvolumechange) {
		attrs.put("onvolumechange", onvolumechange);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/webappapis.html#handler-onwaiting'>onwaiting</a> attribute.
	 * @param onwaiting - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement onwaiting(String onwaiting) {
		attrs.put("onwaiting", onwaiting);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/editing.html#attr-spellcheck'>spellcheck</a> attribute.
	 * @param spellcheck - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement spellcheck(String spellcheck) {
		attrs.put("spellcheck", spellcheck);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#the-style-attribute'>style</a> attribute.
	 * @param style - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement style(String style) {
		attrs.put("style", style);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/editing.html#attr-tabindex'>tabindex</a> attribute.
	 * @param tabindex - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement tabindex(String tabindex) {
		attrs.put("tabindex", tabindex);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#attr-title'>title</a> attribute.
	 * @param title - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public HtmlElement title(String title) {
		attrs.put("title", title);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/dom.html#attr-translate'>translate</a> attribute.
	 * @param translate - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final HtmlElement translate(String translate) {
		attrs.put("translate", translate);
		return this;
	}

}
