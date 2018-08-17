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
 * DTO for an HTML {@doc HTML5.forms#the-label-element <label>}
 * element.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="label")
public class Label extends HtmlElementMixed {

	/**
	 * {@doc HTML5.forms#attr-label-for for} attribute.
	 *
	 * <p>
	 * Associate the label with form control.
	 *
	 * @param _for The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Label _for(String _for) {
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
	public final Label form(String form) {
		attr("form", form);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Label _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Label id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Label style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Label children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Label child(Object child) {
		super.child(child);
		return this;
	}
}
