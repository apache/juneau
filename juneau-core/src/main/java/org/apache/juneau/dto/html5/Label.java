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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-label-element">&lt;label&gt;</a>
 * element.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 		</ul>
 *		</li>
 * </ul>
 */
@Bean(typeName="label")
public class Label extends HtmlElementMixed {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-label-for">for</a> attribute.
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
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
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
