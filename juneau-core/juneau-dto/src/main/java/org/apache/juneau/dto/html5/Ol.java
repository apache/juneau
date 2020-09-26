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
 * DTO for an HTML {@doc ExtHTML5.grouping-content#the-ol-element <ol>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoHtml5}
 * </ul>
 */
@Bean(typeName="ol")
public class Ol extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Ol} element.
	 */
	public Ol() {}

	/**
	 * Creates an {@link Ol} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Ol(Object...children) {
		children(children);
	}

	/**
	 * {@doc ExtHTML5.grouping-content#attr-ol-reversed reversed}
	 * attribute.
	 *
	 * <p>
	 * Number the list backwards..
	 *
	 * @param reversed
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Ol reversed(Object reversed) {
		attr("reversed", deminimize(reversed, "reversed"));
		return this;
	}

	/**
	 * {@doc ExtHTML5.grouping-content#attr-ol-start start} attribute.
	 *
	 * <p>
	 * Ordinal value of the first item.
	 *
	 * @param start
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Ol start(Object start) {
		attr("start", start);
		return this;
	}

	/**
	 * {@doc ExtHTML5.grouping-content#attr-ol-type type} attribute.
	 *
	 * <p>
	 * Kind of list marker.
	 *
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Ol type(String type) {
		attr("type", type);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Ol _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Ol id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Ol style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Ol children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Ol child(Object child) {
		super.child(child);
		return this;
	}
}
