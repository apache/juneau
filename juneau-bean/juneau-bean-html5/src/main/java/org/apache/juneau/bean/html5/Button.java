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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-button-element">&lt;button&gt;</a>
 * element.
 *
 * <p>
 * The button element represents a clickable button that can be used to submit forms, trigger actions,
 * or perform other interactive functions. Unlike input elements, buttons can contain rich content
 * including text, images, and other HTML elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple submit button</jc>
 * 	Button <jv>btn1</jv> = <jsm>button</jsm>().type(<js>"submit"</js>).text(<js>"Submit Form"</js>);
 *
 * 	<jc>// Button with custom styling and click handler</jc>
 * 	Button <jv>btn2</jv> = <jsm>button</jsm>()
 * 		.type(<js>"button"</js>)
 * 		._class(<js>"btn btn-primary"</js>)
 * 		.onclick(<js>"handleClick()"</js>)
 * 		.text(<js>"Click Me"</js>);
 *
 * 	<jc>// Button with form override attributes</jc>
 * 	Button <jv>btn3</jv> = <jsm>button</jsm>()
 * 		.type(<js>"submit"</js>)
 * 		.formaction(<js>"https://api.example.com/submit"</js>)
 * 		.formmethod(<js>"post"</js>)
 * 		.formtarget(<js>"_blank"</js>)
 * 		.text(<js>"Submit to API"</js>);
 *
 * 	<jc>// Button with icon and text</jc>
 * 	Button <jv>btn4</jv> = <jsm>button</jsm>()
 * 		.type(<js>"button"</js>)
 * 		.children(
 * 			<jsm>span</jsm>()._class(<js>"icon"</js>).text(<js>"📧"</js>),
 * 			<jsm>span</jsm>().text(<js>"Send Email"</js>)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#button() button()}
 * 		<li class='jm'>{@link HtmlBuilder#button(String, Object...) button(String, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName = "button")
public class Button extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Button} element.
	 */
	public Button() {}

	/**
	 * Creates a {@link Button} element with the specified {@link Button#type(String)} attribute.
	 *
	 * @param type The {@link Button#type(String)} attribute.
	 */
	public Button(String type) {
		type(type);
	}

	/**
	 * Creates a {@link Button} element with the specified {@link Button#type(String)} attribute and
	 * {@link Button#children(Object[])} nodes.
	 *
	 * @param type The {@link Button#type(String)} attribute.
	 * @param children The {@link Button#children(Object[])} nodes.
	 */
	public Button(String type, Object...children) {
		type(type).children(children);
	}

	@Override /* Overridden from HtmlElement */
	public Button _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Button autofocus(Object value) {
		attr("autofocus", value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Button child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementMixed */
	public Button children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button dir(String value) {
		super.dir(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"disabled"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Button disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the button with a form element by specifying the form's ID. This allows the button
	 * to be placed outside the form element while still being part of the form submission.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this button.
	 * @return This object.
	 */
	public Button form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formaction">formaction</a> attribute.
	 *
	 * <p>
	 * Specifies the URL where the form data will be submitted when this button is clicked.
	 * Overrides the form's action attribute.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value The URL where the form data will be submitted.
	 * @return This object.
	 */
	public Button formaction(String value) {
		attrUri("formaction", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formenctype">formenctype</a> attribute.
	 *
	 * <p>
	 * Specifies how form data should be encoded when submitted. Overrides the form's enctype attribute.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 *  	<li><js>"application/x-www-form-urlencoded"</js> - Default encoding (default)</li>
	 *  	<li><js>"multipart/form-data"</js> - For file uploads</li>
	 *  	<li><js>"text/plain"</js> - Plain text encoding</li>
	 * </ul>
	 *
	 * @param value The encoding type for form submission.
	 * @return This object.
	 */
	public Button formenctype(String value) {
		attr("formenctype", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formmethod">formmethod</a> attribute.
	 *
	 * <p>
	 * Specifies the HTTP method to use for form submission. Overrides the form's method attribute.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 *  	<li><js>"get"</js> - Form data is sent as URL parameters</li>
	 *  	<li><js>"post"</js> - Form data is sent in the request body (default)</li>
	 *  	<li><js>"dialog"</js> - Used for forms within dialog elements</li>
	 * </ul>
	 *
	 * @param value The HTTP method for form submission.
	 * @return This object.
	 */
	public Button formmethod(String value) {
		attr("formmethod", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formnovalidate">formnovalidate</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies that form validation should be bypassed when this button submits the form.
	 * Overrides the form's novalidate attribute.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 *  	<li><jk>false</jk> - Form validation is performed (default)</li>
	 *  	<li><jk>true</jk> - Form validation is bypassed</li>
	 *  	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value Whether to bypass form validation.
	 * @return This object.
	 */
	public Button formnovalidate(String value) {
		attr("formnovalidate", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formtarget">formtarget</a> attribute.
	 *
	 * <p>
	 * Specifies where to display the form response after submission. Overrides the form's target attribute.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 *  	<li><js>"_blank"</js> - Open in a new window/tab</li>
	 *  	<li><js>"_self"</js> - Open in the same frame (default)</li>
	 *  	<li><js>"_parent"</js> - Open in the parent frame</li>
	 *  	<li><js>"_top"</js> - Open in the full body of the window</li>
	 *  	<li><js>"framename"</js> - Open in a named frame</li>
	 * </ul>
	 *
	 * @param value Where to display the form response.
	 * @return This object.
	 */
	public Button formtarget(String value) {
		attr("formtarget", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-menu">menu</a> attribute.
	 *
	 * <p>
	 * Specifies the ID of a menu element that should be displayed as a pop-up menu
	 * when the button is activated.
	 *
	 * <p>
	 * The value should match the ID of a menu element in the same document.
	 *
	 * @param value The ID of the menu element to display as a pop-up.
	 * @return This object.
	 */
	public Button menu(String value) {
		attr("menu", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the button. This name is used when the form is submitted and
	 * can be used to access the element via the form.elements API.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param value The name of the button for submission and API access.
	 * @return This object.
	 */
	public Button name(String value) {
		attr("name", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Button translate(Object value) {
		super.translate(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-button-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the type of button and its behavior when clicked.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"submit"</js> - Submits the form (default)</li>
	 * 	<li><js>"reset"</js> - Resets the form to its initial state</li>
	 * 	<li><js>"button"</js> - Generic button with no default behavior</li>
	 * </ul>
	 *
	 * @param value The type of button and its behavior.
	 * @return This object.
	 */
	public Button type(String value) {
		attr("type", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-button-value">value</a> attribute.
	 *
	 * <p>
	 * Value to be used for form submission.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Button value(Object value) {
		attr("value", value);
		return this;
	}
}