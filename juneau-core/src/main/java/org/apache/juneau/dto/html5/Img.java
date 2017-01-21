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
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/embedded-content-0.html#the-img-element'>&lt;img&gt;</a> element.
 */
@Bean(typeName="img")
public class Img extends HtmlElementEmpty {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-alt'>alt</a> attribute.
	 * Replacement text for use when images are not available.
	 * @param alt - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img alt(String alt) {
		attrs.put("alt", alt);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-crossorigin'>crossorigin</a> attribute.
	 * How the element handles crossorigin requests.
	 * @param crossorigin - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img crossorigin(String crossorigin) {
		attrs.put("crossorigin", crossorigin);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-height'>height</a> attribute.
	 * Vertical dimension.
	 * @param height - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img height(String height) {
		attrs.put("height", height);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-ismap'>ismap</a> attribute.
	 * Whether the image is a server-side image map.
	 * @param ismap - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img ismap(String ismap) {
		attrs.put("ismap", ismap);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-img-src'>src</a> attribute.
	 * Address of the resource.
	 * @param src - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img src(String src) {
		attrs.put("src", src);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-hyperlink-usemap'>usemap</a> attribute.
	 * Name of image map to use.
	 * @param usemap - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img usemap(String usemap) {
		attrs.put("usemap", usemap);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-dim-width'>width</a> attribute.
	 * Horizontal dimension.
	 * @param width - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img width(String width) {
		attrs.put("width", width);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Img _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Img id(String id) {
		super.id(id);
		return this;
	}
}
