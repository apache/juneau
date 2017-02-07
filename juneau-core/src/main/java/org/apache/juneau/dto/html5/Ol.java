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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-ol-element">&lt;ol&gt;</a> element.
 * <p>
 */
@Bean(typeName="ol")
public class Ol extends HtmlElementContainer {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-reversed">reversed</a> attribute.
	 * Number the list backwards..
	 * @param reversed - The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Ol reversed(Object reversed) {
		attr("reversed", reversed);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-start">start</a> attribute.
	 * Ordinal value of the first item.
	 * @param start - The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Ol start(Object start) {
		attr("start", start);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-type">type</a> attribute.
	 * Kind of list marker..
	 * @param type - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Ol type(String type) {
		attr("type", type);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
