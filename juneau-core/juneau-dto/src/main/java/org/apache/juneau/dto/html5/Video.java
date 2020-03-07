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
 * DTO for an HTML {@doc HTML5.embedded-content-0#the-video-element <video>}
 * element.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.HTML5}
 * </ul>
 */
@Bean(typeName="video")
public class Video extends HtmlElementContainer {

	/**
	 * Creates an empty {@link Video} element.
	 */
	public Video() {}

	/**
	 * Creates a {@link Video} element with the specified {@link Video#src(Object)} attribute.
	 *
	 * @param src The {@link Video#src(Object)} attribute.
	 */
	public Video(Object src) {
		src(src);
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-autoplay autoplay}
	 * attribute.
	 *
	 * <p>
	 * Hint that the media resource can be started automatically when the page is loaded.
	 *
	 * @param autoplay
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Video autoplay(Object autoplay) {
		attr("autoplay", deminimize(autoplay, "autoplay"));
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-controls controls}
	 * attribute.
	 *
	 * <p>
	 * Show user agent controls.
	 *
	 * @param controls
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Video controls(Object controls) {
		attr("controls", deminimize(controls, "controls"));
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-crossorigin crossorigin}
	 * attribute.
	 *
	 * <p>
	 * How the element handles cross-origin requests.
	 *
	 * @param crossorigin The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Video crossorigin(String crossorigin) {
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
	public final Video height(Object height) {
		attr("height", height);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-loop loop} attribute.
	 *
	 * <p>
	 * Whether to loop the media resource.
	 *
	 * @param loop
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Video loop(Object loop) {
		attr("loop", loop);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-mediagroup mediagroup}
	 * attribute.
	 *
	 * <p>
	 * Groups media elements together with an implicit MediaController.
	 *
	 * @param mediagroup The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Video mediagroup(String mediagroup) {
		attr("mediagroup", mediagroup);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-muted muted}
	 * attribute.
	 *
	 * <p>
	 * Whether to mute the media resource by default.
	 *
	 * @param muted
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Video muted(Object muted) {
		attr("muted", muted);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-video-poster poster}
	 * attribute.
	 *
	 * <p>
	 * Poster frame to show prior to video playback.
	 *
	 * @param poster The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Video poster(String poster) {
		attr("poster", poster);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-preload preload}
	 * attribute.
	 *
	 * <p>
	 * Hints how much buffering the media resource will likely need.
	 *
	 * @param preload The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Video preload(String preload) {
		attr("preload", preload);
		return this;
	}

	/**
	 * {@doc HTML5.embedded-content-0#attr-media-src src} attribute.
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
	public final Video src(Object src) {
		attrUri("src", src);
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
	public final Video width(Object width) {
		attr("width", width);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Video _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Video id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Video style(String style) {
		super.style(style);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Video children(Object...children) {
		super.children(children);
		return this;
	}

	@Override /* HtmlElementContainer */
	public final Video child(Object child) {
		super.child(child);
		return this;
	}
}
