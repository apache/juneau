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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.marshall.html.HtmlFormat.*;
import static org.apache.juneau.marshall.xml.XmlFormat.*;

import java.net.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Superclass for all HTML elements.
 *
 * <p>
 * These are beans that when serialized using {@link HtmlSerializer} generate valid HTML5 elements.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>

 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@org.apache.juneau.marshall.html.Html(format = XML)
@SuppressWarnings("java:S119")  // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
public abstract class HtmlElement<SELF extends HtmlElement<SELF>> {

	private java.util.Map<String,Object> attrs;

	@SuppressWarnings("unchecked")
	protected final SELF self() {
		return (SELF) this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#classes">class</a> attribute.
	 *
	 * <p>
	 * Specifies one or more CSS class names for the element, separated by spaces.
	 * These classes can be used for styling and JavaScript selection.
	 *
	 * @param value Space-separated CSS class names (e.g., <js>"btn btn-primary"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S100" // Method name uses underscore prefix to match HTML attribute name
	})
	public SELF class_(String value) {
		attr("class", value);
		return self();
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
	 * @param value The keyboard shortcut character (e.g., <js>"a"</js>, <js>"1"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF accesskey(String value) {
		attr("accesskey", value);
		return self();
	}

	/**
	 * Adds an arbitrary attribute to this element.
	 *
	 * @param key The attribute name.
	 * @param val The attribute value. Can be <jk>null</jk> to remove the attribute.
	 * @return This object.
	 */
	public SELF attr(String key, Object val) {
		if (attrs == null)
			attrs = map();
		if (val == null)
			attrs.remove(key);
		else {
			if ("url".equals(key) || "href".equals(key) || key.endsWith("action"))
				val = toUri(val);
			attrs.put(key, val);
		}
		return self();
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
	 * @param val The attribute value. Can be <jk>null</jk>, in which case the attribute is added with a <jk>null</jk> value (unlike {@link #attr(String, Object)}, the key is not removed).
	 * @return This object.
	 */
	public SELF attrUri(String key, Object val) {
		if (attrs == null)
			attrs = map();
		attrs.put(key, toUri(val));
		return self();
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
	 * @param value The editability state of the element. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF contenteditable(Object value) {
		attr("contenteditable", value);
		return self();
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
	 * @param value The text direction for the element. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF dir(String value) {
		attr("dir", value);
		return self();
	}

	/**
	 * Returns the attribute with the specified name converted to the specified class type.
	 *
	 * @param <T> The class type to convert this class to.
	 * @param type
	 * 	The class type to convert this class to.
	 * 	See {@link BasicConverter} for a list of supported conversion types.
	 * @param key The attribute name.
	 * @return The attribute value, or <jk>null</jk> if the named attribute does not exist.
	 * @throws InvalidConversionException If the attribute value cannot be converted to the specified type.
	 */
	public <T> T getAttr(Class<T> type, String key) {
		return attrs == null ? null : BasicConverter.INSTANCE.to(attrs.get(key), type);
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
	 * The attributes of this element.
	 *
	 * @return The attributes of this element, or <jk>null</jk> if no attributes are set.
	 */
	@Xml(format = ATTRS)
	@BeanProp("a")
	public java.util.Map<String,Object> getAttrs() { return attrs; }

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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF hidden(Object value) {
		attr("hidden", deminimize(value, "hidden"));
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#the-id-attribute">id</a> attribute.
	 *
	 * <p>
	 * Specifies a unique identifier for the element. The ID must be unique within the document
	 * and can be used for CSS styling, JavaScript selection, and anchor links.
	 *
	 * @param value A unique identifier for the element (e.g., <js>"header"</js>, <js>"main-content"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF id(String value) {
		attr("id", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#attr-lang">lang</a> attribute.
	 *
	 * <p>
	 * Specifies the primary language of the element's content using a language tag.
	 * This helps with accessibility, search engines, and browser features like spell checking.
	 *
	 * @param value A language tag (e.g., <js>"en"</js>, <js>"en-US"</js>, <js>"es"</js>, <js>"fr-CA"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF lang(String value) {
		attr("lang", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onabort">onabort</a> attribute.
	 *
	 * <p>
	 * Event handler for when an operation is aborted (e.g., image loading is cancelled).
	 *
	 * @param value JavaScript code to execute when the abort event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onabort(String value) {
		attr("onabort", value);
		return self();
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
	 * @param value JavaScript code to execute when the element loses focus. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onblur(String value) {
		attr("onblur", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncancel">oncancel</a> attribute.
	 *
	 * <p>
	 * Event handler for when a dialog is cancelled.
	 *
	 * @param value JavaScript code to execute when the cancel event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oncancel(String value) {
		attr("oncancel", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncanplay">oncanplay</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media can start playing (enough data has been buffered).
	 *
	 * @param value JavaScript code to execute when the canplay event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oncanplay(String value) {
		attr("oncanplay", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncanplaythrough">oncanplaythrough</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the media can play through to the end without buffering.
	 *
	 * @param value JavaScript code to execute when the canplaythrough event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oncanplaythrough(String value) {
		attr("oncanplaythrough", value);
		return self();
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
	 * @param value JavaScript code to execute when the change event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onchange(String value) {
		attr("onchange", value);
		return self();
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
	 * @param value JavaScript code to execute when the click event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onclick(String value) {
		attr("onclick", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oncuechange">oncuechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when a text track cue changes.
	 *
	 * @param value JavaScript code to execute when the cuechange event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oncuechange(String value) {
		attr("oncuechange", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ondblclick">ondblclick</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element is double-clicked.
	 *
	 * @param value JavaScript code to execute when the dblclick event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF ondblclick(String value) {
		attr("ondblclick", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ondurationchange">ondurationchange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the duration of the media changes.
	 *
	 * @param value JavaScript code to execute when the durationchange event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF ondurationchange(String value) {
		attr("ondurationchange", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onemptied">onemptied</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media element becomes empty (e.g., network error).
	 *
	 * @param value JavaScript code to execute when the emptied event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onemptied(String value) {
		attr("onemptied", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onended">onended</a> attribute.
	 *
	 * <p>
	 * Event handler for when the media playback reaches the end.
	 *
	 * @param value JavaScript code to execute when the ended event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onended(String value) {
		attr("onended", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onerror">onerror</a> attribute.
	 *
	 * <p>
	 * Event handler for when an error occurs (e.g., failed resource loading).
	 *
	 * @param value JavaScript code to execute when the error event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onerror(String value) {
		attr("onerror", value);
		return self();
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
	 * @param value JavaScript code to execute when the focus event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onfocus(String value) {
		attr("onfocus", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oninput">oninput</a> attribute.
	 *
	 * <p>
	 * Event handler for when the value of an input element changes (fires on every keystroke).
	 *
	 * @param value JavaScript code to execute when the input event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oninput(String value) {
		attr("oninput", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-oninvalid">oninvalid</a> attribute.
	 *
	 * <p>
	 * Event handler for when form validation fails.
	 *
	 * @param value JavaScript code to execute when the invalid event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF oninvalid(String value) {
		attr("oninvalid", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeydown">onkeydown</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is pressed down.
	 *
	 * @param value JavaScript code to execute when the keydown event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onkeydown(String value) {
		attr("onkeydown", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeypress">onkeypress</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is pressed (deprecated, use onkeydown instead).
	 *
	 * @param value JavaScript code to execute when the keypress event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onkeypress(String value) {
		attr("onkeypress", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onkeyup">onkeyup</a> attribute.
	 *
	 * <p>
	 * Event handler for when a key is released.
	 *
	 * @param value JavaScript code to execute when the keyup event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onkeyup(String value) {
		attr("onkeyup", value);
		return self();
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
	 * @param value JavaScript code to execute when the load event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onload(String value) {
		attr("onload", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadeddata">onloadeddata</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the first frame of media has finished loading.
	 *
	 * @param value JavaScript code to execute when the loadeddata event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onloadeddata(String value) {
		attr("onloadeddata", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadedmetadata">onloadedmetadata</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when metadata (duration, dimensions, etc.) has been loaded.
	 *
	 * @param value JavaScript code to execute when the loadedmetadata event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onloadedmetadata(String value) {
		attr("onloadedmetadata", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onloadstart">onloadstart</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the browser starts loading the media.
	 *
	 * @param value JavaScript code to execute when the loadstart event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onloadstart(String value) {
		attr("onloadstart", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousedown">onmousedown</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when a mouse button is pressed down on the element.
	 *
	 * @param value JavaScript code to execute when the mousedown event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmousedown(String value) {
		attr("onmousedown", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseenter">onmouseenter</a> attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer enters the element (does not bubble).
	 *
	 * @param value JavaScript code to execute when the mouseenter event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmouseenter(String value) {
		attr("onmouseenter", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseleave">onmouseleave</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer leaves the element (does not bubble).
	 *
	 * @param value JavaScript code to execute when the mouseleave event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmouseleave(String value) {
		attr("onmouseleave", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousemove">onmousemove</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer moves over the element.
	 *
	 * @param value JavaScript code to execute when the mousemove event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmousemove(String value) {
		attr("onmousemove", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseout">onmouseout</a> attribute.
	 *
	 * <p>
	 * Event handler for when the mouse pointer moves out of the element (bubbles).
	 *
	 * @param value JavaScript code to execute when the mouseout event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmouseout(String value) {
		attr("onmouseout", value);
		return self();
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
	 * @param value JavaScript code to execute when the mouseover event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmouseover(String value) {
		attr("onmouseover", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmouseup">onmouseup</a> attribute.
	 *
	 * <p>
	 * Event handler for when a mouse button is released over the element.
	 *
	 * @param value JavaScript code to execute when the mouseup event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmouseup(String value) {
		attr("onmouseup", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onmousewheel">onmousewheel</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the mouse wheel is rotated over the element (deprecated, use onwheel).
	 *
	 * @param value JavaScript code to execute when the mousewheel event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onmousewheel(String value) {
		attr("onmousewheel", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onpause">onpause</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback is paused.
	 *
	 * @param value JavaScript code to execute when the pause event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onpause(String value) {
		attr("onpause", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onplay">onplay</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback starts.
	 *
	 * @param value JavaScript code to execute when the play event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onplay(String value) {
		attr("onplay", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onplaying">onplaying</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback starts after being paused or delayed.
	 *
	 * @param value JavaScript code to execute when the playing event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onplaying(String value) {
		attr("onplaying", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onprogress">onprogress</a> attribute.
	 *
	 * <p>
	 * Event handler for when the browser is downloading media data.
	 *
	 * @param value JavaScript code to execute when the progress event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onprogress(String value) {
		attr("onprogress", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onratechange">onratechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the playback rate of media changes.
	 *
	 * @param value JavaScript code to execute when the ratechange event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onratechange(String value) {
		attr("onratechange", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onreset">onreset</a> attribute.
	 *
	 * <p>
	 * Event handler for when a form is reset.
	 *
	 * @param value JavaScript code to execute when the reset event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onreset(String value) {
		attr("onreset", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onresize">onresize</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element is resized.
	 *
	 * @param value JavaScript code to execute when the resize event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onresize(String value) {
		attr("onresize", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onscroll">onscroll</a> attribute.
	 *
	 * <p>
	 * Event handler for when the element's scrollbar is scrolled.
	 *
	 * @param value JavaScript code to execute when the scroll event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onscroll(String value) {
		attr("onscroll", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onseeked">onseeked</a> attribute.
	 *
	 * <p>
	 * Event handler for when a seek operation completes.
	 *
	 * @param value JavaScript code to execute when the seeked event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onseeked(String value) {
		attr("onseeked", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onseeking">onseeking</a> attribute.
	 *
	 * <p>
	 * Event handler for when a seek operation begins.
	 *
	 * @param value JavaScript code to execute when the seeking event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onseeking(String value) {
		attr("onseeking", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onselect">onselect</a> attribute.
	 *
	 * <p>
	 * Event handler for when text is selected in the element.
	 *
	 * @param value JavaScript code to execute when the select event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onselect(String value) {
		attr("onselect", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onshow">onshow</a> attribute.
	 *
	 * <p>
	 * Event handler for when a context menu is shown.
	 *
	 * @param value JavaScript code to execute when the show event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onshow(String value) {
		attr("onshow", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onstalled">onstalled</a> attribute.
	 *
	 * <p>
	 * Event handler for when media loading is stalled.
	 *
	 * @param value JavaScript code to execute when the stalled event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onstalled(String value) {
		attr("onstalled", value);
		return self();
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
	 * @param value JavaScript code to execute when the submit event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onsubmit(String value) {
		attr("onsubmit", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onsuspend">onsuspend</a> attribute.
	 *
	 * <p>
	 * Event handler for when media loading is suspended.
	 *
	 * @param value JavaScript code to execute when the suspend event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onsuspend(String value) {
		attr("onsuspend", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ontimeupdate">ontimeupdate</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the current playback position changes.
	 *
	 * @param value JavaScript code to execute when the timeupdate event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF ontimeupdate(String value) {
		attr("ontimeupdate", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-ontoggle">ontoggle</a> attribute.
	 *
	 * <p>
	 * Event handler for when a details element is opened or closed.
	 *
	 * @param value JavaScript code to execute when the toggle event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF ontoggle(String value) {
		attr("ontoggle", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onvolumechange">onvolumechange</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the volume of media changes.
	 *
	 * @param value JavaScript code to execute when the volumechange event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onvolumechange(String value) {
		attr("onvolumechange", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-onwaiting">onwaiting</a> attribute.
	 *
	 * <p>
	 * Event handler for when media playback stops to buffer more data.
	 *
	 * @param value JavaScript code to execute when the waiting event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF onwaiting(String value) {
		attr("onwaiting", value);
		return self();
	}

	/**
	 * Sets the attributes for this element.
	 *
	 * @param value The new attributes for this element. Can be <jk>null</jk> to clear all attributes.
	 * @return This object.
	 */
	@BeanProp("a")
	public SELF setAttrs(java.util.Map<String,Object> value) {
		if (nn(value)) {
			value.entrySet().forEach(x -> {
				var key = x.getKey();
				if ("url".equals(key) || "href".equals(key) || key.endsWith("action"))
					x.setValue(toUri(x.getValue()));
			});
		}
		attrs = value;
		return self();
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
	 * @param value Whether spell checking should be enabled. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF spellcheck(Object value) {
		attr("spellcheck", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#the-style-attribute">style</a> attribute.
	 *
	 * <p>
	 * Specifies inline CSS styles for the element. The value should be valid CSS
	 * property-value pairs separated by semicolons.
	 *
	 * @param value Inline CSS styles (e.g., <js>"color: red; font-size: 14px;"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF style(String value) {
		attr("style", value);
		return self();
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
	 * @param value The tab order value for keyboard navigation. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF tabindex(Object value) {
		attr("tabindex", value);
		return self();
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/dom.html#attr-title">title</a> attribute.
	 *
	 * <p>
	 * Specifies additional information about the element, typically displayed as a tooltip
	 * when the user hovers over the element.
	 *
	 * @param value Tooltip text to display on hover (e.g., <js>"Click to submit form"</js>). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF title(String value) {
		attr("title", value);
		return self();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(this);
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
	 * @param value Whether the element content should be translated. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public SELF translate(Object value) {
		attr("translate", value);
		return self();
	}

	/**
	 * If the specified attribute is a boolean, it gets converted to the attribute name if <jk>true</jk> or <jk>null</jk> if <jk>false</jk>.
	 *
	 * @param value The attribute value. Can be <jk>null</jk> (returned as-is, since <jk>null</jk> is not a <c>Boolean</c>).
	 * @param attr The attribute name.
	 * @return The deminimized value, or the same value if the value wasn't a boolean, or <jk>null</jk> if the value was a boolean <jk>false</jk>.
	*/
	protected Object deminimize(Object value, String attr) {
		if (value instanceof Boolean value2) {
			if (isTrue(value2))
				return attr;
			return null;
		}
		return value;
	}
}