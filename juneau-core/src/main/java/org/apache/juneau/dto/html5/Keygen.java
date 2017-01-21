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
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/forms.html#the-keygen-element'>&lt;keygen&gt;</a> element.
 * <p>
 */
@Bean(typeName="keygen")
public class Keygen extends HtmlElementEmpty {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-fe-autofocus'>autofocus</a> attribute.
	 * Automatically focus the form control when the page is loaded.
	 * @param autofocus - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen autofocus(String autofocus) {
		attrs.put("autofocus", autofocus);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-keygen-challenge'>challenge</a> attribute.
	 * String to package with the generated and signed public key.
	 * @param challenge - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen challenge(String challenge) {
		attrs.put("challenge", challenge);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-fe-disabled'>disabled</a> attribute.
	 * Whether the form control is disabled.
	 * @param disabled - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen disabled(String disabled) {
		attrs.put("disabled", disabled);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-fae-form'>form</a> attribute.
	 * Associates the control with a form element.
	 * @param form - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen form(String form) {
		attrs.put("form", form);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-keygen-keytype'>keytype</a> attribute.
	 * The type of cryptographic key to generate.
	 * @param keytype - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen keytype(String keytype) {
		attrs.put("keytype", keytype);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-fe-name'>name</a> attribute.
	 * Name of form control to use for form submission and in the form.elements API.
	 * @param name - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Keygen name(String name) {
		attrs.put("name", name);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
}
