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
import java.net.URI;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML {@doc HTML5.forms#the-button-element <button>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="button")
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
	public final Button autofocus(Object autofocus) {
		attr("autofocus", autofocus);
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
	public final Button disabled(Object disabled) {
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
	public final Button form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-formaction formaction} attribute.
	 *
	 * <p>
	 * URL to use for form submission.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param formaction The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button formaction(String formaction) {
		attrUri("formaction", formaction);
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
	public final Button formenctype(String formenctype) {
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
	public final Button formmethod(String formmethod) {
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
	public final Button formnovalidate(String formnovalidate) {
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
	public final Button formtarget(String formtarget) {
		attr("formtarget", formtarget);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-fs-menu menu} attribute.
	 *
	 * <p>
	 * Specifies the element's designated pop-up menu.
	 *
	 * @param menu The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button menu(String menu) {
		attr("menu", menu);
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
	public final Button name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-button-type type} attribute.
	 *
	 * <p>
	 * Type of button.
	 *
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Button type(String type) {
		attr("type", type);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-button-value value} attribute.
	 *
	 * <p>
	 * Value to be used for form submission.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Button value(Object value) {
		attr("value", value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

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

	@Override /* HtmlElement */
	public final Button style(String style) {
		super.style(style);
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
