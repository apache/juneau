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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-button-element">&lt;button&gt;</a> element.
 * <p>
 */
@Bean(typeName="button")
public class Button extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus">autofocus</a> attribute.
	 * Automatically focus the form control when the page is loaded.
	 * @param autofocus The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Button autofocus(Object autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-disabled">disabled</a> attribute.
	 * Whether the form control is disabled.
	 * @param disabled The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Button disabled(Object disabled) {
		attr("disabled", disabled);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 * Associates the control with a form element.
	 * @param form The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formaction">formaction</a> attribute.
	 * URL to use for form submission.
	 * @param formaction The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formaction(String formaction) {
		attr("formaction", formaction);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formenctype">formenctype</a> attribute.
	 * Form data set encoding type to use for form submission.
	 * @param formenctype The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formenctype(String formenctype) {
		attr("formenctype", formenctype);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formmethod">formmethod</a> attribute.
	 * HTTP method to use for form submission.
	 * @param formmethod The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formmethod(String formmethod) {
		attr("formmethod", formmethod);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formnovalidate">formnovalidate</a> attribute.
	 * Bypass form control validation for form submission.
	 * @param formnovalidate The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formnovalidate(String formnovalidate) {
		attr("formnovalidate", formnovalidate);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-formtarget">formtarget</a> attribute.
	 * Browsing context for form submission.
	 * @param formtarget The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formtarget(String formtarget) {
		attr("formtarget", formtarget);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fs-menu">menu</a> attribute.
	 * Specifies the element's designated pop-up menu.
	 * @param menu The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button menu(String menu) {
		attr("menu", menu);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 * Name of form control to use for form submission and in the form.elements API.
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-button-type">type</a> attribute.
	 * Type of button.
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button type(String type) {
		attr("type", type);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-button-value">value</a> attribute.
	 * Value to be used for form submission.
	 * @param value The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Button value(Object value) {
		attr("value", value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Button _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Button id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Button children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Button child(Object child) {
		super.child(child);
		return this;
	}
}
