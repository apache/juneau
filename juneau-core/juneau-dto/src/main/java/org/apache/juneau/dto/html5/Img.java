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

import java.net.*;
import java.net.URI;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML {@doc HTML5.embedded-content-0#the-img-element <img>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="img")
public class Img extends HtmlElementVoid {

	/**
	 * {@doc HTML5.embedded-content-0#attr-img-alt alt} attribute.
	 *
	 * <p>
	 * Replacement text for use when images are not available.
	 *
	 * @param alt The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img alt(String alt) {
		attr("alt", alt);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-img-crossorigin crossorigin}
	 * attribute.
	 *
	 * <p>
	 * How the element handles cross-origin requests.
	 *
	 * @param crossorigin The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img crossorigin(String crossorigin) {
		attr("crossorigin", crossorigin);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-dim-height height}
	 * attribute.
	 *
	 * <p>
	 * Vertical dimension.
	 *
	 * @param height
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Img height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-img-ismap ismap} attribute.
	 *
	 * <p>
	 * Whether the image is a server-side image map.
	 *
	 * @param ismap
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Img ismap(Object ismap) {
		attr("ismap", deminimize(ismap, "ismap"));
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-img-src src} attribute.
	 *
	 * <p>
	 * Address of the resource.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param src
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Img src(Object src) {
		attrUri("src", src);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-hyperlink-usemap usemap}
	 * attribute.
	 *
	 * <p>
	 * Name of image map to use.
	 *
	 * @param usemap The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Img usemap(String usemap) {
		attr("usemap", usemap);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-dim-width width} attribute.
	 *
	 * <p>
	 * Horizontal dimension.
	 *
	 * @param width
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Img width(Object width) {
		attr("width", width);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

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

	@Override /* HtmlElement */
	public final Img style(String style) {
		super.style(style);
		return this;
	}
}
