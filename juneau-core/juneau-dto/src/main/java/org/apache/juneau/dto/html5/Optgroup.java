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
 * DTO for an HTML {@doc ext.HTML5.forms#the-optgroup-element <optgroup>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="optgroup")
public class Optgroup extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Optgroup} element.
	 */
	public Optgroup() {}

	/**
	 * Creates an {@link Optgroup} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Optgroup(Object...children) {
		children(children);
	}

	/**
	 * {@doc ext.HTML5.forms#attr-optgroup-disabled disabled} attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * @param disabled
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object.
	 */
	public final Optgroup disabled(Object disabled) {
		attr("disabled", deminimize(disabled, "disabled"));
		return this;
	}

	/**
	 * {@doc ext.HTML5.forms#attr-optgroup-label label} attribute.
	 *
	 * <p>
	 * User-visible label.
	 *
	 * @param label The new value for this attribute.
	 * @return This object.
	 */
	public final Optgroup label(String label) {
		attr("label", label);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Optgroup _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Optgroup id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Optgroup style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Optgroup children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Optgroup child(Object child) {
		super.child(child);
		return this;
	}
}
