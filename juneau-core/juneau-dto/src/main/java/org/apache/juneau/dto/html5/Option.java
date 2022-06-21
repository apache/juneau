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
 * DTO for an HTML {@doc ext.HTML5.forms#the-option-element <option>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="option")
public class Option extends HtmlElementText {

	/**
	 * Creates an empty {@link Option} element.
	 */
	public Option() {}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#text(Object)} attribute.
	 *
	 * @param text The {@link Option#text(Object)} attribute.
	 */
	public Option(Object text) {
		text(text);
	}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#value(Object)} attribute and
	 * {@link Option#text(Object)} node.
	 *
	 * @param value The {@link Option#value(Object)} attribute.
	 * @param text The {@link Option#text(Object)} node.
	 */
	public Option(Object value, Object text) {
		value(value).text(text);
	}

	/**
	 * {@doc ext.HTML5.forms#attr-option-disabled disabled} attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Option disabled(Object disabled) {
		attr("disabled", deminimize(disabled, "disabled"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-option-label label} attribute.
	 *
	 * <p>
	 * User-visible label.
	 *
	 * @param label The new value for this attribute.
	 * @return This object.
	 */
	public final Option label(String label) {
		attr("label", label);
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-option-selected selected} attribute.
	 *
	 * <p>
	 * Whether the option is selected by default.
	 *
	 * @param selected
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Option selected(Object selected) {
		attr("selected", deminimize(selected, "selected"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-option-value value} attribute.
	 *
	 * <p>
	 * Value to be used for form submission.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Option value(Object value) {
		attr("value", value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Option _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Option id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Option style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementText */
	public Option text(Object text) {
		super.text(text);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
