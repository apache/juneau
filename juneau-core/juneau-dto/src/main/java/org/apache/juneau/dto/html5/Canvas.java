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
 * DTO for an HTML {@doc HTML5.scripting-1#the-canvas-element <canvas>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="canvas")
public class Canvas extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Canvas} element.
	 */
	public Canvas() {}

	/**
	 * Creates a {@link Canvas} element with the specified {@link Canvas#width(Object)} and
	 * {@link Canvas#height(Object)} attributes.
	 *
	 * @param width The {@link Canvas#width(Object)} attribute.
	 * @param height The {@link Canvas#height(Object)} attribute.
	 */
	public Canvas(Number width, Number height) {
		width(width).height(height);
	}

	/**
	 * {@doc HTML5.scripting-1#attr-canvas-height height} attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param height
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Canvas height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * {@doc HTML5.scripting-1#attr-canvas-width width} attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Canvas width(Object width) {
		attr("width", width);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Canvas _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Canvas id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Canvas style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Canvas children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Canvas child(Object child) {
		super.child(child);
		return this;
	}
}
