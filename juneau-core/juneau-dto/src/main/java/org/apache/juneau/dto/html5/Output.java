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
 * DTO for an HTML {@doc HTML5.forms#the-output-element <output>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="output")
public class Output extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Output} element.
	 */
	public Output() {}

	/**
	 * Creates an {@link Output} element with the specified {@link Output#name(String)} attribute.
	 *
	 * @param name The {@link Output#name(String)} attribute.
	 */
	public Output(String name) {
		name(name);
	}

	/**
	 * {@doc HTML5.forms#attr-output-for for} attribute.
	 *
	 * <p>
	 * Specifies controls from which the output was calculated.
	 *
	 * @param _for The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Output _for(String _for) {
		attr("for", _for);
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
	public final Output form(String form) {
		attr("form", form);
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
	public final Output name(String name) {
		attr("name", name);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Output _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Output id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Output style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Output children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Output child(Object child) {
		super.child(child);
		return this;
	}
}
