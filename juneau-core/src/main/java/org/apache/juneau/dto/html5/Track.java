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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-track-element">&lt;track&gt;</a> element.
 * <p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects (org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink' href='../../../../../overview-summary.html#DTOs.HTML5'>HTML5</a>
 * 	</ul>
 * </ul>
 */
@Bean(typeName="track")
public class Track extends HtmlElementVoid {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-default">default</a> attribute.
	 * Enable the track if no other text track is more suitable.
	 * @param _default The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Track _default(String _default) {
		attr("default", _default);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-kind">kind</a> attribute.
	 * The type of text track.
	 * @param kind The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Track kind(String kind) {
		attr("kind", kind);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-label">label</a> attribute.
	 * User-visible label.
	 * @param label The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Track label(String label) {
		attr("label", label);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-src">src</a> attribute.
	 * Address of the resource.
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param src The new value for this attribute.
	 * Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Track src(Object src) {
		attrUri("src", src);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-srclang">srclang</a> attribute.
	 * Language of the text track.
	 * @param srclang The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Track srclang(String srclang) {
		attr("srclang", srclang);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Track _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Track id(String id) {
		super.id(id);
		return this;
	}
}
