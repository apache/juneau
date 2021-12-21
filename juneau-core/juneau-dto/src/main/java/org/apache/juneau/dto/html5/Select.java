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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML {@doc ext.HTML5.forms#the-select-element <select>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="select")
public class Select extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Select} element.
	 */
	public Select() {}

	/**
	 * Creates a {@link Select} element with the specified {@link Select#name(String)} attribute and child nodes.
	 *
	 * @param name The {@link Select#name(String)} attribute.
	 * @param children The child nodes.
	 */
	public Select(String name, Object...children) {
		name(name).children(children);
	}

	/**
	 * {@doc ext.HTML5.forms#attr-fe-autofocus autofocus} attribute.
	 *
	 * <p>
	 * Automatically focus the form control when the page is loaded.
	 *
	 * @param autofocus
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Select autofocus(Object autofocus) {
		attr("autofocus", autofocus);
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-fe-disabled disabled} attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Select disabled(Object disabled) {
		attr("disabled", deminimize(disabled, "disabled"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-fae-form form} attribute.
	 *
	 * <p>
	 * Associates the control with a form element.
	 *
	 * @param form The new value for this attribute.
	 * @return This object.
	 */
	public final Select form(String form) {
		attr("form", form);
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-select-multiple multiple} attribute.
	 *
	 * <p>
	 * Whether to allow multiple values.
	 *
	 * @param multiple
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Select multiple(Object multiple) {
		attr("multiple", deminimize(multiple, "multiple"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-fe-name name} attribute.
	 *
	 * <p>
	 * Name of form control to use for form submission and in the form.elements API.
	 *
	 * @param name The new value for this attribute.
	 * @return This object.
	 */
	public final Select name(String name) {
		attr("name", name);
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-select-required required} attribute.
	 *
	 * <p>
	 * Whether the control is required for form submission.
	 *
	 * @param required
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Select required(Object required) {
		attr("required", required);
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-select-size size} attribute.
	 *
	 * <p>
	 * Size of the control.
	 *
	 * @param size
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object.
	 */
	public final Select size(Object size) {
		attr("size", size);
		return this;
	}

	/**
	 * Convenience method for selecting a child {@link Option} after the options have already been populated.
	 *
	 * @param optionValue The option value.
	 * @return This object.
	 */
	public Select choose(Object optionValue) {
		if (optionValue != null) {
			for (Object o : getChildren()) {
				if (o instanceof Option) {
					Option o2 = (Option)o;
					if (eq(optionValue.toString(), o2.getAttr(String.class, "value")))
						o2.selected(true);
				}
			}
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Select _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Select id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Select style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Select children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Select child(Object child) {
		super.child(child);
		return this;
	}
}
