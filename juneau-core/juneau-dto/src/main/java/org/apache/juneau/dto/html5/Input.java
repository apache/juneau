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

import java.net.*;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML {@doc HTML5.forms#the-input-element <input>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="input")
public class Input extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Input} element.
	 */
	public Input() {}

	/**
	 * Creates an {@link Input} element with the specified {@link Input#type(String)} attribute.
	 *
	 * @param type The {@link Input#type(String)} attribute.
	 */
	public Input(String type) {
		type(type);
	}

	/**
	 * {@doc HTML5.forms#attr-input-accept accept} attribute.
	 *
	 * <p>
	 * Hint for expected file type in file upload controls.
	 *
	 * @param accept The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input accept(String accept) {
		attr("accept", accept);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-alt alt} attribute.
	 *
	 * <p>
	 * Replacement text for use when images are not available.
	 *
	 * @param alt The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input alt(String alt) {
		attr("alt", alt);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-autocomplete autocomplete} attribute.
	 *
	 * <p>
	 * Hint for form auto-fill feature.
	 *
	 * @param autocomplete The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input autocomplete(String autocomplete) {
		attr("autocomplete", autocomplete);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fe-autofocus autofocus} attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param autofocus The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input autofocus(String autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-checked checked} attribute.
	 *
	 * <p>
	 * Whether the command or control is checked.
	 *
	 * @param checked
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input checked(Object checked) {
		attr("checked", deminimize(checked, "checked"));
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
	public final Input dirname(String dirname) {
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
	public final Input disabled(Object disabled) {
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
	public final Input form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formaction formaction} attribute.
	 *
	 * <p>
	 * URL to use for form submission.
	 *
	 * @param formaction The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input formaction(String formaction) {
		attr("formaction", formaction);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formenctype formenctype} attribute.
	 *
	 * <p>
	 * Form data set encoding type to use for form submission.
	 *
	 * @param formenctype The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input formenctype(String formenctype) {
		attr("formenctype", formenctype);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formmethod formmethod} attribute.
	 *
	 * <p>
	 * HTTP method to use for form submission.
	 *
	 * @param formmethod The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input formmethod(String formmethod) {
		attr("formmethod", formmethod);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formnovalidate formnovalidate}
	 * attribute.
	 *
	 * <p>
	 * Bypass form control validation for form submission.
	 *
	 * @param formnovalidate The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input formnovalidate(String formnovalidate) {
		attr("formnovalidate", formnovalidate);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formtarget formtarget} attribute.
	 *
	 * <p>
	 * Browsing context for form submission.
	 *
	 * @param formtarget The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input formtarget(String formtarget) {
		attr("formtarget", formtarget);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-dim-height height}
	 * attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param height
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-input-inputmode inputmode}
	 * attribute.
	 * Hint for selecting an input modality.
	 *
	 * @param inputmode The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input inputmode(String inputmode) {
		attr("inputmode", inputmode);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-list list} attribute.
	 *
	 * <p>
	 * List of auto-complete options.
	 *
	 * @param list The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input list(String list) {
		attr("list", list);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-max max} attribute.
	 *
	 * <p>
	 * Maximum value.
	 *
	 * @param max
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input max(Object max) {
		attr("max", max);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-maxlength maxlength} attribute.
	 * Maximum length of value.
	 *
	 * @param maxlength The new value for this attribute.
	 * Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input maxlength(Object maxlength) {
		attr("maxlength", maxlength);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-min min} attribute.
	 *
	 * <p>
	 * Minimum value.
	 *
	 * @param min
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input min(Object min) {
		attr("min", min);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-minlength minlength} attribute.
	 *
	 * <p>
	 * Minimum length of value.
	 *
	 * @param minlength
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input minlength(Object minlength) {
		attr("minlength", minlength);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-multiple multiple} attribute.
	 *
	 * <p>
	 * Whether to allow multiple values.
	 *
	 * @param multiple
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input multiple(Object multiple) {
		attr("multiple", deminimize(multiple, "multiple"));
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
	public final Input name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-pattern pattern} attribute.
	 *
	 * <p>
	 * Pattern to be matched by the form control's value.
	 *
	 * @param pattern The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input pattern(String pattern) {
		attr("pattern", pattern);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-placeholder placeholder} attribute.
	 *
	 * <p>
	 * User-visible label to be placed within the form control.
	 *
	 * @param placeholder The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input placeholder(String placeholder) {
		attr("placeholder", placeholder);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-readonly readonly} attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input readonly(Object readonly) {
		attr("readonly", readonly);
		return this;
	}


	/**
	 * {@doc HTML5.forms#attr-input-readonly readonly} attribute.
	 *
	 * <p>
	 * Whether to allow the value to be edited by the user.
	 *
	 * @param readonly If <jk>true</jk>, adds <c>readonly="readonly"</c>.
	 * @return This object (for method chaining).
	 */
	public final Input readonly(boolean readonly) {
		if (readonly)
			readonly("readonly");
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-readonly required} attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param required
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input required(Object required) {
		attr("required", required);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-size size} attribute.
	 *
	 * <p>
	 * Size of the control.
	 *
	 * @param size
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input size(Object size) {
		attr("size", size);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-src src} attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * @param src
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input src(Object src) {
		attr("src", src);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-step step} attribute.
	 *
	 * <p>
	 * Granularity to be matched by the form control's value.
	 *
	 * @param step The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input step(String step) {
		attr("step", step);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-type type} attribute.
	 *
	 * <p>
	 * Type of form control.
	 *
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Input type(String type) {
		attr("type", type);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-input-value value} attribute.
	 *
	 * <p>
	 * Value of the form control.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input value(Object value) {
		attr("value", value);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-dim-width width} attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Input width(Object width) {
		attr("width", width);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Input _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Input id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Input style(String style) {
		super.style(style);
		return this;
	}
}
