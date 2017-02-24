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

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-textarea-element">&lt;textarea&gt;</a> element.
 * <p>
 */
@Bean(typeName="textarea")
public class Textarea extends HtmlElementRawText {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autocomplete">autocomplete</a> attribute.
	 * Hint for form autofill feature.
	 * @param autocomplete The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea autocomplete(String autocomplete) {
		attr("autocomplete", autocomplete);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 * Automatically focus the form control when the page is loaded.
	 * @param autofocus The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea autofocus(Boolean autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-cols">cols</a> attribute.
	 * Maximum number of characters per line.
	 * @param cols The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea cols(Object cols) {
		attr("cols", cols);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-dirname">dirname</a> attribute.
	 * Name of form field to use for sending the element's directionality in form submission.
	 * @param dirname The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea dirname(String dirname) {
		attr("dirname", dirname);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 * Whether the form control is disabled.
	 * @param disabled The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea disabled(Object disabled) {
		attr("disabled", disabled);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 * Associates the control with a form element.
	 * @param form The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * <a class="doclink" href="-">inputmode</a> attribute.
	 * Hint for selecting an input modality.
	 * @param inputmode The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea inputmode(String inputmode) {
		attr("inputmode", inputmode);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-maxlength">maxlength</a> attribute.
	 * Maximum length of value.
	 * @param maxlength The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea maxlength(Object maxlength) {
		attr("maxlength", maxlength);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-minlength">minlength</a> attribute.
	 * Minimum length of value.
	 * @param minlength The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea minlength(Object minlength) {
		attr("minlength", minlength);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 * Name of form control to use for form submission and in the form.elements API.
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-placeholder">placeholder</a> attribute.
	 * User-visible label to be placed within the form control.
	 * @param placeholder The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea placeholder(String placeholder) {
		attr("placeholder", placeholder);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-readonly">readonly</a> attribute.
	 * Whether to allow the value to be edited by the user.
	 * @param readonly The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea readonly(Object readonly) {
		attr("readonly", readonly);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-required">required</a> attribute.
	 * Whether the control is required for form submission.
	 * @param required The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea required(Object required) {
		attr("required", required);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-rows">rows</a> attribute.
	 * Number of lines to show.
	 * @param rows The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea rows(Number rows) {
		attr("rows", rows);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-textarea-wrap">wrap</a> attribute.
	 * How the value of the form control is to be wrapped for form submission.
	 * @param wrap The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea wrap(String wrap) {
		attr("wrap", wrap);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Textarea _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Textarea id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Textarea style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementText */
	public Textarea text(Object text) {
		super.text(text);
		return this;
	}
}
