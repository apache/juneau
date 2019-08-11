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
 * DTO for an HTML {@doc HTML5.forms#the-keygen-element <keygen>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="keygen")
public class Keygen extends HtmlElementVoid {

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
	public final Keygen autofocus(Object autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-keygen-challenge challenge} attribute.
	 *
	 * <p>
	 * String to package with the generated and signed public key.
	 *
	 * @param challenge The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen challenge(String challenge) {
		attr("challenge", challenge);
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
	public final Keygen disabled(Object disabled) {
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
	public final Keygen form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * {@doc HTML5.forms#attr-keygen-keytype keytype} attribute.
	 *
	 * <p>
	 * The type of cryptographic key to generate.
	 *
	 * @param keytype The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen keytype(String keytype) {
		attr("keytype", keytype);
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
	public final Keygen name(String name) {
		attr("name", name);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Keygen _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Keygen id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Keygen style(String style) {
		super.style(style);
		return this;
	}
}
