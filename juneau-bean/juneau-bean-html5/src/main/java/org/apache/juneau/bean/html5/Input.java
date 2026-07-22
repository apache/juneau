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

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-input-element">&lt;input&gt;</a>
 * element.
 *
 * <p>
 * The input element represents a form control that allows users to input data. It is a void element
 * that can take many different forms depending on the type attribute, including text fields, checkboxes,
 * radio buttons, file uploads, and more. The input element is one of the most versatile and commonly
 * used form controls in HTML.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Text input field</jc>
 * 	Input <jv>input1</jv> = <jsm>input</jsm>(<js>"text"</js>)
 * 		.name(<js>"username"</js>)
 * 		.placeholder(<js>"Enter your username"</js>)
 * 		.required(<jk>true</jk>);
 *
 * 	<jc>// Email input with validation</jc>
 * 	Input <jv>input2</jv> = <jsm>input</jsm>(<js>"email"</js>)
 * 		.name(<js>"email"</js>)
 * 		.placeholder(<js>"your@email.com"</js>)
 * 		.autocomplete(<js>"email"</js>);
 *
 * 	<jc>// File upload input</jc>
 * 	Input <jv>input3</jv> = <jsm>input</jsm>(<js>"file"</js>)
 * 		.name(<js>"avatar"</js>)
 * 		.accept(<js>"image/*"</js>)
 * 		.multiple(<jk>true</jk>);
 *
 * 	<jc>// Checkbox input</jc>
 * 	Input <jv>input4</jv> = <jsm>input</jsm>(<js>"checkbox"</js>)
 * 		.name(<js>"subscribe"</js>)
 * 		.value(<js>"yes"</js>)
 * 		.checked(<jk>true</jk>);
 *
 * 	<jc>// Password input with pattern</jc>
 * 	Input <jv>input5</jv> = <jsm>input</jsm>(<js>"password"</js>)
 * 		.name(<js>"password"</js>)
 * 		.pattern(<js>".{8,}"</js>)
 * 		.title(<js>"Password must be at least 8 characters"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#input() input()}
 * 		<li class='jm'>{@link HtmlBuilder#input(String) input(String)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "input")
public class Input extends HtmlElementVoid<Input> {

	/**
	 * Creates an empty {@link Input} element.
	 */
	public Input() {}

	/**
	 * Creates an {@link Input} element with the specified {@link Input#type(String)} attribute.
	 *
	 * @param type The {@link Input#type(String)} attribute. Can be <jk>null</jk> to unset the attribute.
	 */
	public Input(String type) {
		type(type);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-accept">accept</a> attribute.
	 *
	 * <p>
	 * Specifies which file types the file input should accept. Used with <c>type="file"</c>.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li><js>"image/*"</js> - Accept all image files</li>
	 * 	<li><js>".pdf,.doc,.docx"</js> - Accept specific file extensions</li>
	 * 	<li><js>"image/png,image/jpeg"</js> - Accept specific MIME types</li>
	 * </ul>
	 *
	 * @param value File type restrictions for file uploads. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input accept(String value) {
		attr("accept", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-alt">alt</a> attribute.
	 *
	 * <p>
	 * Alternative text for image submit buttons. Used with <c>type="image"</c> to provide
	 * accessible text when the image cannot be displayed.
	 *
	 * @param value Alternative text for image submit buttons. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input alt(String value) {
		attr("alt", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autocomplete">autocomplete</a> attribute.
	 *
	 * <p>
	 * Controls whether the browser can automatically complete the input field.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"on"</js> - Allow autocomplete (default)</li>
	 * 	<li><js>"off"</js> - Disable autocomplete</li>
	 * 	<li><js>"name"</js> - Full name</li>
	 * 	<li><js>"email"</js> - Email address</li>
	 * 	<li><js>"username"</js> - Username or login</li>
	 * 	<li><js>"current-password"</js> - Current password</li>
	 * 	<li><js>"new-password"</js> - New password</li>
	 * 	<li><js>"tel"</js> - Telephone number</li>
	 * 	<li><js>"url"</js> - URL</li>
	 * 	<li><js>"address-line1"</js> - Street address</li>
	 * 	<li><js>"country"</js> - Country name</li>
	 * 	<li><js>"postal-code"</js> - Postal code</li>
	 * </ul>
	 *
	 * @param value Autocomplete hint for the input field. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input autocomplete(String value) {
		attr("autocomplete", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 *
	 * <p>
	 * Automatically focuses the form control when the page loads.
	 * Only one element per page should have this attribute.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input autofocus(Object value) {
		attr("autofocus", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-checked">checked</a> attribute.
	 *
	 * <p>
	 * Whether the command or control is checked.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"checked"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input checked(Object value) {
		attr("checked", deminimize(value, "checked"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-dirname">dirname</a> attribute.
	 *
	 * <p>
	 * Specifies the name of a hidden form field that will be submitted along with the input value,
	 * containing the text direction (ltr or rtl) of the input content.
	 *
	 * <p>
	 * This is useful for forms that need to preserve text direction information when submitted.
	 * The hidden field will contain either "ltr" or "rtl" based on the input's direction.
	 *
	 * @param value The name of the hidden field for directionality information. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input dirname(String value) {
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
	public Input disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the input with a form element by specifying the form's ID. This allows the input
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this input. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formaction">formaction</a> attribute.
	 *
	 * <p>
	 * URL to use for form submission.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input formaction(String value) {
		attr("formaction", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formenctype">formenctype</a> attribute.
	 *
	 * <p>
	 * Form data set encoding type to use for form submission.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input formenctype(String value) {
		attr("formenctype", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formmethod">formmethod</a> attribute.
	 *
	 * <p>
	 * HTTP method to use for form submission.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input formmethod(String value) {
		attr("formmethod", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formnovalidate">formnovalidate</a>
	 * attribute.
	 *
	 * <p>
	 * Bypass form control validation for form submission.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input formnovalidate(String value) {
		attr("formnovalidate", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formtarget">formtarget</a> attribute.
	 *
	 * <p>
	 * Browsing context for form submission.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input formtarget(String value) {
		attr("formtarget", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height">height</a>
	 * attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input height(Object value) {
		attr("height", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-input-inputmode">inputmode</a>
	 * attribute.
	 *
	 * <p>
	 * Provides a hint to browsers about the type of virtual keyboard to display on mobile devices.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"none"</js> - No virtual keyboard</li>
	 * 	<li><js>"text"</js> - Standard text keyboard (default)</li>
	 * 	<li><js>"tel"</js> - Numeric keypad for telephone numbers</li>
	 * 	<li><js>"url"</js> - Keyboard optimized for URLs</li>
	 * 	<li><js>"email"</js> - Keyboard optimized for email addresses</li>
	 * 	<li><js>"numeric"</js> - Numeric keypad</li>
	 * 	<li><js>"decimal"</js> - Numeric keypad with decimal point</li>
	 * 	<li><js>"search"</js> - Keyboard optimized for search</li>
	 * </ul>
	 *
	 * @param value The input modality hint for mobile keyboards. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input inputmode(String value) {
		attr("inputmode", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-list">list</a> attribute.
	 *
	 * <p>
	 * References a <c>&lt;datalist&gt;</c> element that provides predefined options
	 * for the input field. Creates a dropdown with autocomplete suggestions.
	 *
	 * @param value The ID of a datalist element (without the # prefix). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input list(String value) {
		attr("list", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-max">max</a> attribute.
	 *
	 * <p>
	 * Maximum value.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input max(Object value) {
		attr("max", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-maxlength">maxlength</a> attribute.
	 * Maximum length of value.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public Input maxlength(Object value) {
		attr("maxlength", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-min">min</a> attribute.
	 *
	 * <p>
	 * Minimum value.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input min(Object value) {
		attr("min", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-minlength">minlength</a> attribute.
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
	public Input minlength(Object value) {
		attr("minlength", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-multiple">multiple</a> attribute.
	 *
	 * <p>
	 * Whether to allow multiple values.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"multiple"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input multiple(Object value) {
		attr("multiple", deminimize(value, "multiple"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Name of form control to use for form submission and in the form.elements API.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-pattern">pattern</a> attribute.
	 *
	 * <p>
	 * Specifies a regular expression that the input's value must match for the form to be valid.
	 * Works with the <c>title</c> attribute to provide user feedback.
	 *
	 * @param value A regular expression pattern (e.g., <js>"[0-9]{3}-[0-9]{3}-[0-9]{4}"</js> for phone numbers). Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input pattern(String value) {
		attr("pattern", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-placeholder">placeholder</a> attribute.
	 *
	 * <p>
	 * Provides a hint to the user about what to enter in the input field.
	 * The placeholder text appears when the field is empty and disappears when the user starts typing.
	 *
	 * @param value Hint text to display in the empty input field. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input placeholder(String value) {
		attr("placeholder", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">readonly</a> attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param value If <jk>true</jk>, adds <c>readonly="readonly"</c>. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input readonly(boolean value) {
		if (value)
			value("value");
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">readonly</a> attribute.
	 *
	 * <p>
	 * Makes the input field read-only, preventing user modification while still allowing
	 * the value to be submitted with the form.
	 *
	 * @param value If <jk>true</jk>, makes the input read-only. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input readonly(Object value) {
		attr("readonly", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">required</a> attribute.
	 *
	 * <p>
	 * Indicates that the input field must be filled out before the form can be submitted.
	 * Browsers will show validation messages for empty required fields.
	 *
	 * @param value If <jk>true</jk>, makes the input required for form submission. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input required(Object value) {
		attr("required", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-size">size</a> attribute.
	 *
	 * <p>
	 * Size of the control.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input size(Object value) {
		attr("size", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-src">src</a> attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input src(Object value) {
		attr("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-step">step</a> attribute.
	 *
	 * <p>
	 * Granularity to be matched by the form control's value.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input step(String value) {
		attr("step", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the type of form control to display.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"text"</js> - Single-line text input (default)</li>
	 * 	<li><js>"password"</js> - Password input (characters are masked)</li>
	 * 	<li><js>"email"</js> - Email address input with validation</li>
	 * 	<li><js>"number"</js> - Numeric input with spinner controls</li>
	 * 	<li><js>"tel"</js> - Telephone number input</li>
	 * 	<li><js>"url"</js> - URL input with validation</li>
	 * 	<li><js>"search"</js> - Search input field</li>
	 * 	<li><js>"date"</js> - Date picker</li>
	 * 	<li><js>"time"</js> - Time picker</li>
	 * 	<li><js>"datetime-local"</js> - Date and time picker</li>
	 * 	<li><js>"checkbox"</js> - Checkbox input</li>
	 * 	<li><js>"radio"</js> - Radio button input</li>
	 * 	<li><js>"file"</js> - File upload input</li>
	 * 	<li><js>"submit"</js> - Submit button</li>
	 * 	<li><js>"button"</js> - Generic button</li>
	 * 	<li><js>"reset"</js> - Reset form button</li>
	 * 	<li><js>"hidden"</js> - Hidden input field</li>
	 * </ul>
	 *
	 * @param value The input type for the form control. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input type(String value) {
		attr("type", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-value">value</a> attribute.
	 *
	 * <p>
	 * Value of the form control.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input value(Object value) {
		attr("value", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width">width</a> attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Input width(Object value) {
		attr("width", value);
		return this;
	}
}