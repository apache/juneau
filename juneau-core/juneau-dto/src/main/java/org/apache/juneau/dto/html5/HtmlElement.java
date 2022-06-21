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

import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Superclass for all HTML elements.
 *
 * <p>
 * These are beans that when serialized using {@link HtmlSerializer} generate valid HTML5 elements.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@org.apache.juneau.html.annotation.Html(format=XML)
@FluentSetters
public abstract class HtmlElement {

	private LinkedHashMap<String,Object> attrs;

	/**
	 * The attributes of this element.
	 *
	 * @return The attributes of this element.
	 */
	@Xml(format=ATTRS)
	@Beanp("a")
	public LinkedHashMap<String,Object> getAttrs() {
		return attrs;
	}

	/**
	 * Sets the attributes for this element.
	 *
	 * @param attrs The new attributes for this element.
	 * @return This object.
	 */
	@Beanp("a")
	public HtmlElement setAttrs(LinkedHashMap<String,Object> attrs) {
		if (attrs != null) {
			attrs.entrySet().forEach(x -> {
				String key = x.getKey();
				if ("url".equals(key) || "href".equals(key) || key.endsWith("action"))
					x.setValue(StringUtils.toURI(x.getValue()));
			});
		}
		this.attrs = attrs;
		return this;
	}

	/**
	 * Adds an arbitrary attribute to this element.
	 *
	 * @param key The attribute name.
	 * @param val The attribute value.
	 * @return This object.
	 */
	public HtmlElement attr(String key, Object val) {
		if (attrs == null)
			attrs = map();
		if (val == null)
			attrs.remove(key);
		else {
			if ("url".equals(key) || "href".equals(key) || key.endsWith("action"))
				val = StringUtils.toURI(val);
			attrs.put(key, val);
		}
		return this;
	}

	/**
	 * Adds an arbitrary URI attribute to this element.
	 *
	 * <p>
	 * Same as {@link #attr(String, Object)}, except if the value is a string that appears to be a URI
	 * (e.g. <js>"servlet:/xxx"</js>).
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param key The attribute name.
	 * @param val The attribute value.
	 * @return This object.
	 */
	public HtmlElement attrUri(String key, Object val) {
		if (attrs == null)
			attrs = map();
		attrs.put(key, StringUtils.toURI(val));
		return this;
	}

	/**
	 * Returns the attribute with the specified name.
	 *
	 * @param key The attribute name.
	 * @return The attribute value, or <jk>null</jk> if the named attribute does not exist.
	 */
	public String getAttr(String key) {
		return getAttr(String.class, key);
	}

	/**
	 * Returns the attribute with the specified name converted to the specified class type.
	 *
	 * @param <T> The class type to convert this class to.
	 * @param type
	 * 	The class type to convert this class to.
	 * 	See {@link ConverterUtils} for a list of supported conversion types.
	 * @param key The attribute name.
	 * @return The attribute value, or <jk>null</jk> if the named attribute does not exist.
	 */
	public <T> T getAttr(Class<T> type, String key) {
		return attrs == null ? null : ConverterUtils.toType(attrs.get(key), type);
	}

	/**
	 * {@doc ext.HTML5.editing#the-accesskey-attribute accesskey}
	 * attribute.
	 *
	 * @param accesskey The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement accesskey(String accesskey) {
		attr("accesskey", accesskey);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#classes class} attribute.
	 *
	 * @param _class The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement _class(String _class) {
		attr("class", _class);
		return this;
	}

	/**
	 * {@doc ext.HTML5.editing#attr-contenteditable contenteditable}
	 * attribute.
	 *
	 * @param contenteditable The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement contenteditable(Object contenteditable) {
		attr("contenteditable", contenteditable);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#the-dir-attribute dir} attribute.
	 *
	 * @param dir The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement dir(String dir) {
		attr("dir", dir);
		return this;
	}

	/**
	 * {@doc ext.HTML5.editing#the-hidden-attribute hidden} attribute.
	 *
	 * @param hidden
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement hidden(Object hidden) {
		attr("hidden", deminimize(hidden, "hidden"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#the-id-attribute id} attribute.
	 *
	 * @param id The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement id(String id) {
		attr("id", id);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#attr-lang lang} attribute.
	 *
	 * @param lang The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement lang(String lang) {
		attr("lang", lang);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onabort onabort} attribute.
	 *
	 * @param onabort The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onabort(String onabort) {
		attr("onabort", onabort);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onblur onblur} attribute.
	 *
	 * @param onblur The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onblur(String onblur) {
		attr("onblur", onblur);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oncancel oncancel} attribute.
	 *
	 * @param oncancel The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncancel(String oncancel) {
		attr("oncancel", oncancel);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oncanplay oncanplay} attribute.
	 *
	 * @param oncanplay The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncanplay(String oncanplay) {
		attr("oncanplay", oncanplay);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oncanplaythrough oncanplaythrough}
	 * attribute.
	 *
	 * @param oncanplaythrough The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncanplaythrough(String oncanplaythrough) {
		attr("oncanplaythrough", oncanplaythrough);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onchange onchange} attribute.
	 *
	 * @param onchange The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onchange(String onchange) {
		attr("onchange", onchange);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onclick onclick} attribute.
	 *
	 * @param onclick The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onclick(String onclick) {
		attr("onclick", onclick);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oncuechange oncuechange}
	 * attribute.
	 *
	 * @param oncuechange The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncuechange(String oncuechange) {
		attr("oncuechange", oncuechange);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-ondblclick ondblclick} attribute.
	 *
	 * @param ondblclick The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ondblclick(String ondblclick) {
		attr("ondblclick", ondblclick);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-ondurationchange ondurationchange}
	 * attribute.
	 *
	 * @param ondurationchange The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ondurationchange(String ondurationchange) {
		attr("ondurationchange", ondurationchange);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onemptied onemptied} attribute.
	 *
	 * @param onemptied The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onemptied(String onemptied) {
		attr("onemptied", onemptied);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onended onended} attribute.
	 *
	 * @param onended The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onended(String onended) {
		attr("onended", onended);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onerror onerror} attribute.
	 *
	 * @param onerror The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onerror(String onerror) {
		attr("onerror", onerror);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onfocus onfocus} attribute.
	 *
	 * @param onfocus The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onfocus(String onfocus) {
		attr("onfocus", onfocus);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oninput oninput} attribute.
	 *
	 * @param oninput The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oninput(String oninput) {
		attr("oninput", oninput);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-oninvalid oninvalid} attribute.
	 *
	 * @param oninvalid The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oninvalid(String oninvalid) {
		attr("oninvalid", oninvalid);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onkeydown onkeydown} attribute.
	 *
	 * @param onkeydown The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeydown(String onkeydown) {
		attr("onkeydown", onkeydown);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onkeypress onkeypress} attribute.
	 *
	 * @param onkeypress The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeypress(String onkeypress) {
		attr("onkeypress", onkeypress);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onkeyup onkeyup} attribute.
	 *
	 * @param onkeyup The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeyup(String onkeyup) {
		attr("onkeyup", onkeyup);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onload onload} attribute.
	 *
	 * @param onload The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onload(String onload) {
		attr("onload", onload);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onloadeddata onloadeddata}
	 * attribute.
	 *
	 * @param onloadeddata The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadeddata(String onloadeddata) {
		attr("onloadeddata", onloadeddata);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onloadedmetadata onloadedmetadata}
	 * attribute.
	 *
	 * @param onloadedmetadata The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadedmetadata(String onloadedmetadata) {
		attr("onloadedmetadata", onloadedmetadata);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onloadstart onloadstart}
	 * attribute.
	 *
	 * @param onloadstart The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadstart(String onloadstart) {
		attr("onloadstart", onloadstart);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmousedown onmousedown}
	 * attribute.
	 *
	 * @param onmousedown The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousedown(String onmousedown) {
		attr("onmousedown", onmousedown);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmouseenter onmouseenter} attribute.
	 *
	 * @param onmouseenter The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseenter(String onmouseenter) {
		attr("onmouseenter", onmouseenter);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmouseleave onmouseleave}
	 * attribute.
	 *
	 * @param onmouseleave The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseleave(String onmouseleave) {
		attr("onmouseleave", onmouseleave);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmousemove onmousemove}
	 * attribute.
	 *
	 * @param onmousemove The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousemove(String onmousemove) {
		attr("onmousemove", onmousemove);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmouseout onmouseout} attribute.
	 *
	 * @param onmouseout The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseout(String onmouseout) {
		attr("onmouseout", onmouseout);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmouseover onmouseover}
	 * attribute.
	 *
	 * @param onmouseover The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseover(String onmouseover) {
		attr("onmouseover", onmouseover);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmouseup onmouseup} attribute.
	 *
	 * @param onmouseup The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseup(String onmouseup) {
		attr("onmouseup", onmouseup);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onmousewheel onmousewheel}
	 * attribute.
	 *
	 * @param onmousewheel The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousewheel(String onmousewheel) {
		attr("onmousewheel", onmousewheel);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onpause onpause} attribute.
	 *
	 * @param onpause The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onpause(String onpause) {
		attr("onpause", onpause);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onplay onplay} attribute.
	 *
	 * @param onplay The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onplay(String onplay) {
		attr("onplay", onplay);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onplaying onplaying} attribute.
	 *
	 * @param onplaying The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onplaying(String onplaying) {
		attr("onplaying", onplaying);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onprogress onprogress} attribute.
	 *
	 * @param onprogress The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onprogress(String onprogress) {
		attr("onprogress", onprogress);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onratechange onratechange}
	 * attribute.
	 *
	 * @param onratechange The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onratechange(String onratechange) {
		attr("onratechange", onratechange);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onreset onreset} attribute.
	 *
	 * @param onreset The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onreset(String onreset) {
		attr("onreset", onreset);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onresize onresize} attribute.
	 *
	 * @param onresize The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onresize(String onresize) {
		attr("onresize", onresize);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onscroll onscroll} attribute.
	 *
	 * @param onscroll The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onscroll(String onscroll) {
		attr("onscroll", onscroll);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onseeked onseeked} attribute.
	 *
	 * @param onseeked The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onseeked(String onseeked) {
		attr("onseeked", onseeked);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onseeking onseeking} attribute.
	 *
	 * @param onseeking The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onseeking(String onseeking) {
		attr("onseeking", onseeking);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onselect onselect} attribute.
	 *
	 * @param onselect The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onselect(String onselect) {
		attr("onselect", onselect);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onshow onshow} attribute.
	 *
	 * @param onshow The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onshow(String onshow) {
		attr("onshow", onshow);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onstalled onstalled} attribute.
	 *
	 * @param onstalled The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onstalled(String onstalled) {
		attr("onstalled", onstalled);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onsubmit onsubmit} attribute.
	 *
	 * @param onsubmit The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onsubmit(String onsubmit) {
		attr("onsubmit", onsubmit);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onsuspend onsuspend} attribute.
	 *
	 * @param onsuspend The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onsuspend(String onsuspend) {
		attr("onsuspend", onsuspend);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-ontimeupdate ontimeupdate}
	 * attribute.
	 *
	 * @param ontimeupdate The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ontimeupdate(String ontimeupdate) {
		attr("ontimeupdate", ontimeupdate);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-ontoggle ontoggle} attribute.
	 *
	 * @param ontoggle The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ontoggle(String ontoggle) {
		attr("ontoggle", ontoggle);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onvolumechange onvolumechange}
	 * attribute.
	 *
	 * @param onvolumechange The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onvolumechange(String onvolumechange) {
		attr("onvolumechange", onvolumechange);
		return this;
	}

	/**
	 * {@doc ext.HTML5.webappapis#handler-onwaiting onwaiting} attribute.
	 *
	 * @param onwaiting The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onwaiting(String onwaiting) {
		attr("onwaiting", onwaiting);
		return this;
	}

	/**
	 * {@doc ext.HTML5.editing#attr-spellcheck spellcheck} attribute.
	 *
	 * @param spellcheck
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement spellcheck(Object spellcheck) {
		attr("spellcheck", spellcheck);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#the-style-attribute style} attribute.
	 *
	 * @param style The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement style(String style) {
		attr("style", style);
		return this;
	}

	/**
	 * {@doc ext.HTML5.editing#attr-tabindex tabindex} attribute.
	 *
	 * @param tabindex
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement tabindex(Object tabindex) {
		attr("tabindex", tabindex);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#attr-title title} attribute.
	 *
	 * @param title The new value for this attribute.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement title(String title) {
		attr("title", title);
		return this;
	}

	/**
	 * {@doc ext.HTML5.dom#attr-translate translate} attribute.
	 *
	 * @param translate
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement translate(Object translate) {
		attr("translate", translate);
		return this;
	}

	/**
	 * If the specified attribute is a boolean, it gets converted to the attribute name if <jk>true</jk> or <jk>null</jk> if <jk>false</jk>.
	 *
	 * @param value The attribute value.
	 * @param attr The attribute name.
	 * @return The deminimized value, or the same value if the value wasn't a boolean.
	 */
	protected Object deminimize(Object value, String attr) {
		if (value instanceof Boolean) {
			if ((Boolean)value)
				return attr;
			return null;
		}
		return value;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* Object */
	public String toString() {
		return HtmlSerializer.DEFAULT_SQ.toString(this);
	}
}
