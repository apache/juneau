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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-fieldset-element">&lt;fieldset&gt;</a>
 * element.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.HTML5'>Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
*/
@Bean(typeName="fieldset")
public class Fieldset extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fieldset-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Fieldset disabled(Boolean disabled) {
		attr("disabled", disabled);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the control with a form element.
	 *
	 * @param form The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Fieldset form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Name of form control to use for form submission and in the form.elements API.
	 *
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Fieldset name(String name) {
		attr("name", name);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Fieldset _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Fieldset id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Fieldset style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Fieldset children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Fieldset child(Object child) {
		super.child(child);
		return this;
	}
}
