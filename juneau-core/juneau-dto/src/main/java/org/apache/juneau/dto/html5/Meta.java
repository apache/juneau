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
 * DTO for an HTML {@doc HTML5.document-metadata#the-meta-element <meta>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="meta")
public class Meta extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Meta} element.
	 */
	public Meta() {}

	/**
	 * {@doc HTML5.document-metadata#attr-meta-charset charset}
	 * attribute.
	 *
	 * <p>
	 * Character encoding declaration.
	 *
	 * @param charset The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Meta charset(String charset) {
		attr("charset", charset);
		return this;
	}

	/**
	 * {@doc HTML5.document-metadata#attr-meta-content content}
	 * attribute.
	 *
	 * <p>
	 * Value of the element.
	 *
	 * @param content The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Meta content(String content) {
		attr("content", content);
		return this;
	}

	/**
	 * {@doc HTML5.document-metadata#attr-meta-http-equiv http-equiv}
	 * attribute.
	 *
	 * <p>
	 * Pragma directive.
	 *
	 * @param httpequiv The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Meta httpequiv(String httpequiv) {
		attr("http-equiv", httpequiv);
		return this;
	}

	/**
	 * {@doc HTML5.document-metadata#attr-meta-name name} attribute.
	 *
	 * <p>
	 * Metadata name.
	 *
	 * @param name The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Meta name(String name) {
		attr("name", name);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Meta _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Meta id(String id) {
		super.id(id);
		return this;
	}
}
