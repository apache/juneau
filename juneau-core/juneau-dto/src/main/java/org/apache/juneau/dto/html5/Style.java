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
 * DTO for an HTML {@doc ext.HTML5.document-metadata#the-style-element <style>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * </ul>
 */
@Bean(typeName="style")
public class Style extends HtmlElementRawText {

	/**
	 * Creates an empty {@link Style} element.
	 */
	public Style() {}

	/**
	 * Creates a {@link Style} element with the specified {@link Style#text(Object)} node.
	 *
	 * @param text The {@link Style#text(Object)} node.
	 */
	public Style(Object text) {
		text(text);
	}

	/**
	 * Creates a {@link Style} element with the specified inner text.
	 *
	 * @param text
	 * 	The contents of the style element.
	 * 	<br>Values will be concatenated with newlines.
	 */
	public Style(String...text) {
		text(joinnl(text));
	}

	/**
	 * {@doc ext.HTML5.document-metadata#attr-style-media media} attribute.
	 *
	 * <p>
	 * Applicable media.
	 *
	 * @param media The new value for this attribute.
	 * @return This object.
	 */
	public final Style media(String media) {
		attr("media", media);
		return this;
	}

	/**
	 * {@doc ext.HTML5.document-metadata#attr-style-type type} attribute.
	 *
	 * <p>
	 * Type of embedded resource.
	 *
	 * @param type The new value for this attribute.
	 * @return This object.
	 */
	public final Style type(String type) {
		attr("type", type);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Style _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Style style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElement */
	public final Style id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementText */
	public final Style text(Object text) {
		super.text(text);
		return this;
	}
}
