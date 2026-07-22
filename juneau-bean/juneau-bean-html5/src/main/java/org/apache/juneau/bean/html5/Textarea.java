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

import org.apache.juneau.marshall.*;

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
 * 		.class_(<js>"large-textarea"</js>)
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
 * 		<li class='jm'>{@link HtmlBuilder#textarea(String, String) textarea(String, String)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "textarea")
public class Textarea extends HtmlElementRawText<Textarea> {

	/**
	 * Creates an empty {@link Textarea} element.
	 */
	public Textarea() {}

	/**
	 * Creates a {@link Textarea} element with the specified {@link Textarea#name(String)} attribute and
	 * {@link Textarea#text(Object)} node.
	 *
	 * @param name The {@link Textarea#name(String)} attribute. Can be <jk>null</jk> to unset the attribute.
	 * @param text The {@link Textarea#text(Object)} node. Can be <jk>null</jk> to leave the text unset.
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
	 * @param value Autocomplete behavior for the form field. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The visible width of the textarea in characters. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The name of the hidden field for directionality information. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The ID of the form element to associate with this textarea. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The type of virtual keyboard to display. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The name of the form control for submission and API access. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The placeholder text to display when the textarea is empty. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
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
	 * @param value The visible height of the textarea in lines. Can be <jk>null</jk> to unset the attribute.
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
	 * @param value How the text should be wrapped for form submission. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Textarea wrap(String value) {
		attr("wrap", value);
		return this;
	}
}
