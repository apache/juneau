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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-link-element">&lt;link&gt;</a>
 * element.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.HTML5'>Overview &gt; juneau-dto &gt; HTML5</a>
 * </ul>
 */
@Bean(typeName="link")
public class Link extends HtmlElementVoid {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-crossorigin">crossorigin</a>
	 * attribute.
	 * 
	 * <p>
	 * How the element handles cross-origin requests.
	 * 
	 * @param crossorigin The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link crossorigin(String crossorigin) {
		attr("crossorigin", crossorigin);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-href">href</a> attribute.
	 * 
	 * <p>
	 * Address of the hyperlink.
	 * 
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * 
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 * 
	 * @param href
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Link href(Object href) {
		attrUri("href", href);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-hreflang">hreflang</a>
	 * attribute.
	 * 
	 * <p>
	 * Language of the linked resource.
	 * 
	 * @param hreflang The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link hreflang(String hreflang) {
		attr("hreflang", hreflang);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-media">media</a> attribute.
	 * 
	 * <p>
	 * Applicable media.
	 * 
	 * @param media The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link media(String media) {
		attr("media", media);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-rel">rel</a> attribute.
	 * 
	 * <p>
	 * Relationship between the document containing the hyperlink and the destination resource.
	 * 
	 * @param rel The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link rel(String rel) {
		attr("rel", rel);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-link-sizes">sizes</a> attribute.
	 * 
	 * <p>
	 * Sizes of the icons (for rel="icon").
	 * 
	 * @param sizes The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link sizes(String sizes) {
		attr("sizes", sizes);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-link-type">type</a> attribute.
	 * 
	 * <p>
	 * Hint for the type of the referenced resource.
	 * 
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Link type(String type) {
		attr("type", type);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Link _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Link id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Link style(String style) {
		super.style(style);
		return this;
	}
}
