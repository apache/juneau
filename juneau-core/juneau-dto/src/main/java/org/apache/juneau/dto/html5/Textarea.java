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
 * DTO for an HTML {@doc HTML5.forms#the-textarea-element <textarea>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="textarea")
public class Textarea extends HtmlElementRawText {

	/**
	 * {@doc HTML5.forms#attr-fe-autocomplete autocomplete} attribute.
	 *
	 * <p>
	 * Hint for form auto-fill feature.
	 *
	 * @param autocomplete The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea autocomplete(String autocomplete) {
		attr("autocomplete", autocomplete);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-autofocus autofocus} attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param autofocus
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea autofocus(Boolean autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-cols cols} attribute.
	 *
	 * <p>
	 * Maximum number of characters per line.
	 *
	 * @param cols
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea cols(Object cols) {
		attr("cols", cols);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-dirname dirname} attribute.
	 *
	 * <p>
	 * Name of form field to use for sending the element's directionality in form submission.
	 *
	 * @param dirname The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea dirname(String dirname) {
		attr("dirname", dirname);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-disabled disabled} attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea disabled(Object disabled) {
		attr("disabled", deminimize(disabled, "disabled"));
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fae-form form} attribute.
	 *
	 * <p>
	 * Associates the control with a form element.
	 *
	 * @param form The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * {@doc HTML5.forms#inputmode inputmode} attribute.
	 *
	 * <p>
	 * Hint for selecting an input modality.
	 *
	 * @param inputmode The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea inputmode(String inputmode) {
		attr("inputmode", inputmode);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-maxlength maxlength} attribute.
	 *
	 * <p>
	 * Maximum length of value.
	 *
	 * @param maxlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea maxlength(Object maxlength) {
		attr("maxlength", maxlength);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-minlength minlength} attribute.
	 *
	 * <p>
	 * Minimum length of value.
	 *
	 * @param minlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea minlength(Object minlength) {
		attr("minlength", minlength);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-name name} attribute.
	 *
	 * <p>
	 * Name of form control to use for form submission and in the form.elements API.
	 *
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-placeholder placeholder}
	 * attribute.
	 *
	 * <p>
	 * User-visible label to be placed within the form control.
	 *
	 * @param placeholder The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea placeholder(String placeholder) {
		attr("placeholder", placeholder);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-readonly readonly} attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea readonly(Object readonly) {
		attr("readonly", readonly);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-required required} attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param required
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea required(Object required) {
		attr("required", required);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-rows rows} attribute.
	 *
	 * <p>
	 * Number of lines to show.
	 *
	 * @param rows
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Textarea rows(Number rows) {
		attr("rows", rows);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-textarea-wrap wrap} attribute.
	 *
	 * <p>
	 * How the value of the form control is to be wrapped for form submission.
	 *
	 * @param wrap The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Textarea wrap(String wrap) {
		attr("wrap", wrap);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

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
