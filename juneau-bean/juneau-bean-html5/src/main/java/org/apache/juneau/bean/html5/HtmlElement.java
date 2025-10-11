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

import static org.apache.juneau.html.annotation.HtmlFormat.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.common.utils.*;
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>

 * </ul>
 */
@org.apache.juneau.html.annotation.Html(format=XML)
@FluentSetters
public abstract class HtmlElement {

	private java.util.Map<String,Object> attrs;

	/**
	 * The attributes of this element.
	 *
	 * @return The attributes of this element.
	 */
	@Xml(format=ATTRS)
	@Beanp("a")
	public java.util.Map<String,Object> getAttrs() {
		return attrs;
	}

	/**
	 * Sets the attributes for this element.
	 *
	 * @param attrs The new attributes for this element.
	 * @return This object.
	 */
	@Beanp("a")
	public HtmlElement setAttrs(java.util.Map<String,Object> attrs) {
		if (attrs != null) {
			attrs.entrySet().forEach(x -> {
				var key = x.getKey();
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
	 * <a class="doclink" href="https://www.w3.org/TR/html5/editing.html#the-accesskey-attribute">accesskey</a>
	 * attribute.
	 *
	 * <p>
	 * Defines a keyboard shortcut to activate or focus an element.
	 * The value should be a single character that, when pressed with a modifier key (usually Alt),
	 * activates the element.
	 *
	 * @param accesskey The keyboard shortcut character (e.g., <js>"a"</js>, <js>"1"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement accesskey(String value) {
		attr("accesskey", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#classes">class</a> attribute.
	 *
	 * <p>
	 * Specifies one or more CSS class names for the element, separated by spaces.
	 * These classes can be used for styling and JavaScript selection.
	 *
	 * @param _class Space-separated CSS class names (e.g., <js>"btn btn-primary"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement _class(String value) {  // NOSONAR - Intentional naming.
		attr("class", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/editing.html#attr-contenteditable">contenteditable</a>
	 * attribute.
	 *
	 * <p>
	 * Indicates whether the element's content is editable by the user.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js> or empty string - Element content is editable</li>
	 * 	<li><js>"false"</js> - Element content is not editable</li>
	 * 	<li><js>"plaintext-only"</js> - Element content is editable, but rich text formatting is disabled</li>
	 * </ul>
	 *
	 * @param contenteditable The editability state of the element.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement contenteditable(Object value) {
		attr("contenteditable", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#the-dir-attribute">dir</a> attribute.
	 *
	 * <p>
	 * Specifies the text direction of the element's content.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"ltr"</js> - Left-to-right text direction</li>
	 * 	<li><js>"rtl"</js> - Right-to-left text direction</li>
	 * 	<li><js>"auto"</js> - Browser determines direction based on content</li>
	 * </ul>
	 *
	 * @param dir The text direction for the element.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement dir(String value) {
		attr("dir", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/editing.html#the-hidden-attribute">hidden</a> attribute.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"hidden"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param hidden
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement hidden(Object value) {
		attr("hidden", deminimize(value, "hidden"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#the-id-attribute">id</a> attribute.
	 *
	 * <p>
	 * Specifies a unique identifier for the element. The ID must be unique within the document
	 * and can be used for CSS styling, JavaScript selection, and anchor links.
	 *
	 * @param id A unique identifier for the element (e.g., <js>"header"</js>, <js>"main-content"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement id(String value) {
		attr("id", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#attr-lang">lang</a> attribute.
	 *
	 * <p>
	 * Specifies the primary language of the element's content using a language tag.
	 * This helps with accessibility, search engines, and browser features like spell checking.
	 *
	 * @param lang A language tag (e.g., <js>"en"</js>, <js>"en-US"</js>, <js>"es"</js>, <js>"fr-CA"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement lang(String value) {
		attr("lang", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onabort">onabort</a> attribute.
	 *
	 * <p>
	 * Event handler for when an operation is aborted (e.g., image loading is cancelled).
	 *
	 * @param onabort JavaScript code to execute when the abort event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onabort(String value) {
		attr("onabort", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onblur">onblur</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element loses focus.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onblur(<js>"validate(\"email\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onblur(<js>"validate('email')"</js>)</c>
	 * </ul>
	 *
	 * @param onblur JavaScript code to execute when the element loses focus.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onblur(String value) {
		attr("onblur", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncancel">oncancel</a> attribute.
	 *
	 * <p>
	 * Event handler for when a dialog is cancelled.
	 *
	 * @param oncancel JavaScript code to execute when the cancel event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncancel(String value) {
		attr("oncancel", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncanplay">oncanplay</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media can start playing (enough data has been buffered).
	 *
	 * @param oncanplay JavaScript code to execute when the canplay event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncanplay(String value) {
		attr("oncanplay", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncanplaythrough">oncanplaythrough</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the media can play through to the end without buffering.
	 *
	 * @param oncanplaythrough JavaScript code to execute when the canplaythrough event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncanplaythrough(String value) {
		attr("oncanplaythrough", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onchange">onchange</a> attribute.
	 *
	 * <p>
	 * Event handler for when the value of a form element changes and loses focus.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onchange(<js>"validate(\"field\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onchange(<js>"validate('field')"</js>)</c>
	 * </ul>
	 *
	 * @param onchange JavaScript code to execute when the change event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onchange(String value) {
		attr("onchange", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onclick">onclick</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element is clicked.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onclick(<js>"alert(\"Hello\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onclick(<js>"alert('Hello')"</js>)</c>
	 * </ul>
	 *
	 * @param onclick JavaScript code to execute when the click event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onclick(String value) {
		attr("onclick", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncuechange">oncuechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when a text track cue changes.
	 *
	 * @param oncuechange JavaScript code to execute when the cuechange event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oncuechange(String value) {
		attr("oncuechange", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ondblclick">ondblclick</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element is double-clicked.
	 *
	 * @param ondblclick JavaScript code to execute when the dblclick event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ondblclick(String value) {
		attr("ondblclick", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ondurationchange">ondurationchange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the duration of the media changes.
	 *
	 * @param ondurationchange JavaScript code to execute when the durationchange event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ondurationchange(String value) {
		attr("ondurationchange", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onemptied">onemptied</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media element becomes empty (e.g., network error).
	 *
	 * @param onemptied JavaScript code to execute when the emptied event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onemptied(String value) {
		attr("onemptied", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onended">onended</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media playback reaches the end.
	 *
	 * @param onended JavaScript code to execute when the ended event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onended(String value) {
		attr("onended", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onerror">onerror</a> attribute.
	 *
	 * <p>
	 * Event handler for when an error occurs (e.g., failed resource loading).
	 *
	 * @param onerror JavaScript code to execute when the error event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onerror(String value) {
		attr("onerror", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onfocus">onfocus</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element receives focus.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onfocus(<js>"highlight(\"field\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onfocus(<js>"highlight('field')"</js>)</c>
	 * </ul>
	 *
	 * @param onfocus JavaScript code to execute when the focus event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onfocus(String value) {
		attr("onfocus", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oninput">oninput</a> attribute.
	 *
	 * <p>
	 * Event handler for when the value of an input element changes (fires on every keystroke).
	 *
	 * @param oninput JavaScript code to execute when the input event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oninput(String value) {
		attr("oninput", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oninvalid">oninvalid</a> attribute.
	 *
	 * <p>
	 * Event handler for when form validation fails.
	 *
	 * @param oninvalid JavaScript code to execute when the invalid event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement oninvalid(String value) {
		attr("oninvalid", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeydown">onkeydown</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is pressed down.
	 *
	 * @param onkeydown JavaScript code to execute when the keydown event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeydown(String value) {
		attr("onkeydown", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeypress">onkeypress</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is pressed (deprecated, use onkeydown instead).
	 *
	 * @param onkeypress JavaScript code to execute when the keypress event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeypress(String value) {
		attr("onkeypress", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeyup">onkeyup</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is released.
	 *
	 * @param onkeyup JavaScript code to execute when the keyup event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onkeyup(String value) {
		attr("onkeyup", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onload">onload</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element and its resources have finished loading.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onload(<js>"init(\"config\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onload(<js>"init('config')"</js>)</c>
	 * </ul>
	 *
	 * @param onload JavaScript code to execute when the load event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onload(String value) {
		attr("onload", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadeddata">onloadeddata</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the first frame of media has finished loading.
	 *
	 * @param onloadeddata JavaScript code to execute when the loadeddata event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadeddata(String value) {
		attr("onloadeddata", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadedmetadata">onloadedmetadata</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when metadata (duration, dimensions, etc.) has been loaded.
	 *
	 * @param onloadedmetadata JavaScript code to execute when the loadedmetadata event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadedmetadata(String value) {
		attr("onloadedmetadata", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadstart">onloadstart</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the browser starts loading the media.
	 *
	 * @param onloadstart JavaScript code to execute when the loadstart event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onloadstart(String value) {
		attr("onloadstart", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousedown">onmousedown</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when a mouse button is pressed down on the element.
	 *
	 * @param onmousedown JavaScript code to execute when the mousedown event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousedown(String value) {
		attr("onmousedown", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseenter">onmouseenter</a> attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer enters the element (does not bubble).
	 *
	 * @param onmouseenter JavaScript code to execute when the mouseenter event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseenter(String value) {
		attr("onmouseenter", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseleave">onmouseleave</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer leaves the element (does not bubble).
	 *
	 * @param onmouseleave JavaScript code to execute when the mouseleave event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseleave(String value) {
		attr("onmouseleave", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousemove">onmousemove</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer moves over the element.
	 *
	 * @param onmousemove JavaScript code to execute when the mousemove event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousemove(String value) {
		attr("onmousemove", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseout">onmouseout</a> attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer moves out of the element (bubbles).
	 *
	 * @param onmouseout JavaScript code to execute when the mouseout event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseout(String value) {
		attr("onmouseout", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseover">onmouseover</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer moves over the element (bubbles).
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onmouseover(<js>"showTooltip(\"info\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onmouseover(<js>"showTooltip('info')"</js>)</c>
	 * </ul>
	 *
	 * @param onmouseover JavaScript code to execute when the mouseover event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseover(String value) {
		attr("onmouseover", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseup">onmouseup</a> attribute.
	 *
	 * <p>
	 * Event handler for when a mouse button is released over the element.
	 *
	 * @param onmouseup JavaScript code to execute when the mouseup event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmouseup(String value) {
		attr("onmouseup", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousewheel">onmousewheel</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse wheel is rotated over the element (deprecated, use onwheel).
	 *
	 * @param onmousewheel JavaScript code to execute when the mousewheel event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onmousewheel(String value) {
		attr("onmousewheel", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onpause">onpause</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback is paused.
	 *
	 * @param onpause JavaScript code to execute when the pause event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onpause(String value) {
		attr("onpause", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onplay">onplay</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback starts.
	 *
	 * @param onplay JavaScript code to execute when the play event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onplay(String value) {
		attr("onplay", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onplaying">onplaying</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback starts after being paused or delayed.
	 *
	 * @param onplaying JavaScript code to execute when the playing event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onplaying(String value) {
		attr("onplaying", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onprogress">onprogress</a> attribute.
	 *
	 * <p>
	 * Event handler for when the browser is downloading media data.
	 *
	 * @param onprogress JavaScript code to execute when the progress event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onprogress(String value) {
		attr("onprogress", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onratechange">onratechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the playback rate of media changes.
	 *
	 * @param onratechange JavaScript code to execute when the ratechange event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onratechange(String value) {
		attr("onratechange", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onreset">onreset</a> attribute.
	 *
	 * <p>
	 * Event handler for when a form is reset.
	 *
	 * @param onreset JavaScript code to execute when the reset event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onreset(String value) {
		attr("onreset", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onresize">onresize</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element is resized.
	 *
	 * @param onresize JavaScript code to execute when the resize event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onresize(String value) {
		attr("onresize", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onscroll">onscroll</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element's scrollbar is scrolled.
	 *
	 * @param onscroll JavaScript code to execute when the scroll event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onscroll(String value) {
		attr("onscroll", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onseeked">onseeked</a> attribute.
	 *
	 * <p>
	 * Event handler for when a seek operation completes.
	 *
	 * @param onseeked JavaScript code to execute when the seeked event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onseeked(String value) {
		attr("onseeked", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onseeking">onseeking</a> attribute.
	 *
	 * <p>
	 * Event handler for when a seek operation begins.
	 *
	 * @param onseeking JavaScript code to execute when the seeking event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onseeking(String value) {
		attr("onseeking", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onselect">onselect</a> attribute.
	 *
	 * <p>
	 * Event handler for when text is selected in the element.
	 *
	 * @param onselect JavaScript code to execute when the select event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onselect(String value) {
		attr("onselect", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onshow">onshow</a> attribute.
	 *
	 * <p>
	 * Event handler for when a context menu is shown.
	 *
	 * @param onshow JavaScript code to execute when the show event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onshow(String value) {
		attr("onshow", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onstalled">onstalled</a> attribute.
	 *
	 * <p>
	 * Event handler for when media loading is stalled.
	 *
	 * @param onstalled JavaScript code to execute when the stalled event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onstalled(String value) {
		attr("onstalled", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onsubmit">onsubmit</a> attribute.
	 *
	 * <p>
	 * Event handler for when a form is submitted.
	 *
	 * <h5 class='section'>Note:</h5>
	 * <p>
	 * If your HTML serializer is configured to use single quotes for attribute values, you should use double quotes
	 * in your JavaScript code, and vice versa. Otherwise, the quotes will be converted to HTML entities.
	 * For example:
	 * <ul>
	 * 	<li>If using single quotes for attributes: <c>onsubmit(<js>"return validate(\"form\")"</js>)</c>
	 * 	<li>If using double quotes for attributes: <c>onsubmit(<js>"return validate('form')"</js>)</c>
	 * </ul>
	 *
	 * @param onsubmit JavaScript code to execute when the submit event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onsubmit(String value) {
		attr("onsubmit", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onsuspend">onsuspend</a> attribute.
	 *
	 * <p>
	 * Event handler for when media loading is suspended.
	 *
	 * @param onsuspend JavaScript code to execute when the suspend event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onsuspend(String value) {
		attr("onsuspend", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ontimeupdate">ontimeupdate</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the current playback position changes.
	 *
	 * @param ontimeupdate JavaScript code to execute when the timeupdate event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ontimeupdate(String value) {
		attr("ontimeupdate", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ontoggle">ontoggle</a> attribute.
	 *
	 * <p>
	 * Event handler for when a details element is opened or closed.
	 *
	 * @param ontoggle JavaScript code to execute when the toggle event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement ontoggle(String value) {
		attr("ontoggle", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onvolumechange">onvolumechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the volume of media changes.
	 *
	 * @param onvolumechange JavaScript code to execute when the volumechange event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onvolumechange(String value) {
		attr("onvolumechange", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onwaiting">onwaiting</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback stops to buffer more data.
	 *
	 * @param onwaiting JavaScript code to execute when the waiting event occurs.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement onwaiting(String value) {
		attr("onwaiting", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/editing.html#attr-spellcheck">spellcheck</a> attribute.
	 *
	 * <p>
	 * Indicates whether the element should have its spelling and grammar checked.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"true"</js> - Enable spell checking for this element</li>
	 * 	<li><js>"false"</js> - Disable spell checking for this element</li>
	 * </ul>
	 *
	 * @param spellcheck Whether spell checking should be enabled.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement spellcheck(Object value) {
		attr("spellcheck", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#the-style-attribute">style</a> attribute.
	 *
	 * <p>
	 * Specifies inline CSS styles for the element. The value should be valid CSS
	 * property-value pairs separated by semicolons.
	 *
	 * @param style Inline CSS styles (e.g., <js>"color: red; font-size: 14px;"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement style(String value) {
		attr("style", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/editing.html#attr-tabindex">tabindex</a> attribute.
	 *
	 * <p>
	 * Specifies the tab order of the element when navigating with the keyboard.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>Positive integer - Element is focusable and participates in tab order</li>
	 * 	<li><js>"0"</js> - Element is focusable but not in tab order</li>
	 * 	<li>Negative integer - Element is not focusable</li>
	 * </ul>
	 *
	 * @param tabindex The tab order value for keyboard navigation.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement tabindex(Object value) {
		attr("tabindex", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#attr-title">title</a> attribute.
	 *
	 * <p>
	 * Specifies additional information about the element, typically displayed as a tooltip
	 * when the user hovers over the element.
	 *
	 * @param title Tooltip text to display on hover (e.g., <js>"Click to submit form"</js>).
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement title(String value) {
		attr("title", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#attr-translate">translate</a> attribute.
	 *
	 * <p>
	 * Specifies whether the element's content should be translated when the page is localized.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"yes"</js> - Content should be translated (default)</li>
	 * 	<li><js>"no"</js> - Content should not be translated</li>
	 * </ul>
	 *
	 * @param translate Whether the element content should be translated.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlElement translate(Object value) {
		attr("translate", value);
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
		if (value instanceof Boolean b) {
			if (Boolean.TRUE.equals(b))
				return attr;
			return null;
		}
		return value;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* Object */
	public String toString() {
		return HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(this);
	}
}