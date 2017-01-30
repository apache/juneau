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

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a href='https://www.w3.org/TR/html5/embedded-content-0.html#the-audio-element'>&lt;audio&gt;</a> element.
 * <p>
 */
@Bean(typeName="audio")
public class Audio extends HtmlElementContainer {

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-autoplay'>autoplay</a> attribute.
	 * Hint that the media resource can be started automatically when the page is loaded.
	 * @param autoplay - The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Audio autoplay(Object autoplay) {
		attr("autoplay", autoplay);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-controls'>controls</a> attribute.
	 * Show user agent controls.
	 * @param controls - The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Audio controls(Object controls) {
		attr("controls", controls);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-crossorigin'>crossorigin</a> attribute.
	 * How the element handles crossorigin requests.
	 * @param crossorigin - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Audio crossorigin(String crossorigin) {
		attr("crossorigin", crossorigin);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-loop'>loop</a> attribute.
	 * Whether to loop the media resource.
	 * @param loop - The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Audio loop(Object loop) {
		attr("loop", loop);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-mediagroup'>mediagroup</a> attribute.
	 * Groups media elements together with an implicit MediaController.
	 * @param mediagroup - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Audio mediagroup(String mediagroup) {
		attr("mediagroup", mediagroup);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-muted'>muted</a> attribute.
	 * Whether to mute the media resource by default.
	 * @param muted - The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Audio muted(Object muted) {
		attr("muted", muted);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-preload'>preload</a> attribute.
	 * Hints how much buffering the media resource will likely need.
	 * @param preload - The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Audio preload(Object preload) {
		attr("preload", preload);
		return this;
	}

	/**
	 * <a class='doclink' href='https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-src'>src</a> attribute.
	 * Address of the resource.
	 * @param src - The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Audio src(Object src) {
		attr("src", src);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Audio _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Audio id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Audio children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Audio child(Object child) {
		super.child(child);
		return this;
	}
}
