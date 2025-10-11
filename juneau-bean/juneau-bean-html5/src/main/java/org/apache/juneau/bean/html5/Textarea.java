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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-textarea-element">&lt;textarea&gt;</a>
 * element.
 *
 * <p>
 * The textarea element represents a multiline text input control. It allows users to enter and edit
 * text over multiple lines, making it suitable for longer text content such as comments, descriptions,
 * or messages. The textarea element supports various attributes for controlling its size, behavior,
 * and validation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 * 
 * 	<jc>// Basic textarea</jc>
 * 	Textarea <jv>basic</jv> = <jsm>textarea</jsm>(<js>"comments"</js>)
 * 		.rows(4)
 * 		.cols(50);
 * 
 * 	<jc>// Textarea with placeholder and validation</jc>
 * 	Textarea <jv>validated</jv> = <jsm>textarea</jsm>(<js>"description"</js>)
 * 		.placeholder(<js>"Enter a description..."</js>)
 * 		.required(<jk>true</jk>)
 * 		.minlength(10)
 * 		.maxlength(500);
 * 
 * 	<jc>// Textarea with initial content</jc>
 * 	Textarea <jv>withContent</jv> = <jsm>textarea</jsm>(<js>"message"</js>)
 * 		.text(<js>"Default message text"</js>);
 * 
 * 	<jc>// Textarea with styling and behavior</jc>
 * 	Textarea <jv>styled</jv> = <jsm>textarea</jsm>(<js>"feedback"</js>)
 * 		._class(<js>"large-textarea"</js>)
 * 		.rows(6)
 * 		.cols(60)
 * 		.placeholder(<js>"Please provide your feedback..."</js>)
 * 		.wrap(<js>"hard"</js>);
 * 
 * 	<jc>// Disabled textarea</jc>
 * 	Textarea <jv>disabled</jv> = <jsm>textarea</jsm>(<js>"readonly"</js>)
 * 		.disabled(true)
 * 		.text("This textarea is disabled");
 * 
 * 	// Textarea with form association
 * 	Textarea external = new Textarea()
 * 		.name="external"
 * 		.form="myForm"
 * 		.placeholder="This textarea is outside the form";
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#textarea() textarea()}
 * 		<li class='jm'>{@link HtmlBuilder#textarea(Object, Object...) textarea(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Bean(typeName="textarea")
@FluentSetters
public class Textarea extends HtmlElementRawText {

	/**
	 * Creates an empty {@link Textarea} element.
	 */
	public Textarea() {}

	/**
	 * Creates a {@link Textarea} element with the specified {@link Textarea#name(String)} attribute and
	 * {@link Textarea#text(Object)} node.
	 *
	 * @param name The {@link Textarea#name(String)} attribute.
	 * @param text The {@link Textarea#text(Object)} node.
	 */
	public Textarea(String name, String text) {
		name(name).text(text);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autocomplete">autocomplete</a> attribute.
	 *
	 * <p>
	 * Specifies whether the browser should automatically complete the form field based on user's previous input.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"on"</js> - Allow autocomplete (default)</li>
	 * 	<li><js>"off"</js> - Disable autocomplete</li>
	 * 	<li><js>"name"</js> - Autocomplete for name fields</li>
	 * 	<li><js>"email"</js> - Autocomplete for email fields</li>
	 * 	<li><js>"username"</js> - Autocomplete for username fields</li>
	 * 	<li><js>"current-password"</js> - Autocomplete for current password</li>
	 * 	<li><js>"new-password"</js> - Autocomplete for new password</li>
	 * </ul>
	 *
	 * @param autocomplete Autocomplete behavior for the form field.
	 * @return This object.
	 */
	public Textarea autocomplete(String value) {
		attr("autocomplete", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param autofocus
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Textarea autofocus(Object value) {
		attr("autofocus", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-cols">cols</a> attribute.
	 *
	 * <p>
	 * Specifies the visible width of the textarea in characters. This is a hint for the browser
	 * and may not be exactly followed depending on the font and styling.
	 *
	 * @param cols The visible width of the textarea in characters.
	 * @return This object.
	 */
	public Textarea cols(Object value) {
		attr("cols", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-dirname">dirname</a> attribute.
	 *
	 * <p>
	 * Specifies the name of a hidden form field that will be submitted along with the textarea value,
	 * containing the text direction (ltr or rtl) of the textarea content.
	 *
	 * <p>
	 * This is useful for forms that need to preserve text direction information when submitted.
	 * The hidden field will contain either "ltr" or "rtl" based on the textarea's direction.
	 *
	 * @param dirname The name of the hidden field for directionality information.
	 * @return This object.
	 */
	public Textarea dirname(String value) {
		attr("dirname", value);
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
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Textarea disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the textarea with a form element by specifying the form's ID. This allows the textarea
	 * to be placed outside the form element while still being part of the form submission.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param form The ID of the form element to associate with this textarea.
	 * @return This object.
	 */
	public Textarea form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#inputmode">inputmode</a> attribute.
	 *
	 * <p>
	 * Provides a hint to browsers about the type of virtual keyboard to display on mobile devices.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"none"</js> - No virtual keyboard</li>
	 * 	<li><js>"text"</js> - Standard text keyboard (default)</li>
	 * 	<li><js>"tel"</js> - Telephone number keyboard</li>
	 * 	<li><js>"url"</js> - URL keyboard with .com key</li>
	 * 	<li><js>"email"</js> - Email keyboard with @ key</li>
	 * 	<li><js>"numeric"</js> - Numeric keyboard</li>
	 * 	<li><js>"decimal"</js> - Decimal number keyboard</li>
	 * 	<li><js>"search"</js> - Search keyboard</li>
	 * </ul>
	 *
	 * @param inputmode The type of virtual keyboard to display.
	 * @return This object.
	 */
	public Textarea inputmode(String value) {
		attr("inputmode", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-maxlength">maxlength</a> attribute.
	 *
	 * <p>
	 * Maximum length of value.
	 *
	 * @param maxlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Textarea maxlength(Object value) {
		attr("maxlength", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-minlength">minlength</a> attribute.
	 *
	 * <p>
	 * Minimum length of value.
	 *
	 * @param minlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Textarea minlength(Object value) {
		attr("minlength", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the form control. This name is used when the form is submitted and
	 * can be used to access the element via the form.elements API.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param name The name of the form control for submission and API access.
	 * @return This object.
	 */
	public Textarea name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-placeholder">placeholder</a>
	 * attribute.
	 *
	 * <p>
	 * Provides a hint to the user about what to enter in the textarea. The placeholder text is displayed
	 * when the textarea is empty and disappears when the user starts typing.
	 *
	 * <p>
	 * The placeholder should be a brief, helpful description of the expected input.
	 *
	 * @param placeholder The placeholder text to display when the textarea is empty.
	 * @return This object.
	 */
	public Textarea placeholder(String value) {
		attr("placeholder", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-readonly">readonly</a> attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Textarea readonly(Object value) {
		attr("readonly", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-required">required</a> attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param required
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public Textarea required(Object value) {
		attr("required", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-rows">rows</a> attribute.
	 *
	 * <p>
	 * Specifies the visible height of the textarea in lines. This is a hint for the browser
	 * and may not be exactly followed depending on the font and styling.
	 *
	 * @param rows The visible height of the textarea in lines.
	 * @return This object.
	 */
	public Textarea rows(Number value) {
		attr("rows", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-wrap">wrap</a> attribute.
	 *
	 * <p>
	 * Specifies how the text in the textarea should be wrapped when the form is submitted.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"soft"</js> - Text is wrapped in the display but not in the submitted value (default)</li>
	 * 	<li><js>"hard"</js> - Text is wrapped in both display and submitted value</li>
	 * 	<li><js>"off"</js> - Text is not wrapped</li>
	 * </ul>
	 *
	 * @param wrap How the text should be wrapped for form submission.
	 * @return This object.
	 */
	public Textarea wrap(String value) {
		attr("wrap", value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea id(String value) {
		super.id(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea spellcheck(Object value) {
		super.spellcheck(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea style(String value) {
		super.style(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea title(String value) {
		super.title(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
	public Textarea translate(Object value) {
		super.translate(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElementRawText */
	public Textarea text(Object value) {
		super.text(value);
		return this;
	}

	// </FluentSetters>
}