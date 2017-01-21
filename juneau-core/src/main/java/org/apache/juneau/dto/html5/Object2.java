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
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/embedded-content-0.html#the-object-element'>&lt;object&gt;</a> element.
 * <p>
 */
@Bean(typeName="object")
@SuppressWarnings("hiding")
public class Object2 extends HtmlElementMixed {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-data'>data</a> attribute.
	 * Address of the resource.
	 * @param data - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 data(String data) {
		attrs.put("data", data);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/forms.html#attr-fae-form'>form</a> attribute.
	 * Associates the control with a form element.
	 * @param form - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 form(String form) {
		attrs.put("form", form);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height'>height</a> attribute.
	 * Vertical dimension.
	 * @param height - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 height(String height) {
		attrs.put("height", height);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-name'>name</a> attribute.
	 * Name of nested browsing context.
	 * @param name - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 name(String name) {
		attrs.put("name", name);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-type'>type</a> attribute.
	 * Type of embedded resource.
	 * @param type - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 type(String type) {
		attrs.put("type", type);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-object-typemustmatch'>typemustmatch</a> attribute.
	 * Whether the type attribute and the Content-Type value need to match for the resource to be used.
	 * @param typemustmatch - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 typemustmatch(String typemustmatch) {
		attrs.put("typemustmatch", typemustmatch);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-hyperlink-usemap'>usemap</a> attribute.
	 * Name of image map to use.
	 * @param usemap - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 usemap(String usemap) {
		attrs.put("usemap", usemap);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width'>width</a> attribute.
	 * Horizontal dimension.
	 * @param width - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Object2 width(String width) {
		attrs.put("width", width);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Object2 _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Object2 id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Object2 children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementMixed */
	public Object2 child(Object child) {
		this.children.add(child);
		return this;
	}
}
