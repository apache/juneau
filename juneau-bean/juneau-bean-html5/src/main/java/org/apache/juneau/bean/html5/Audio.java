/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import java.net.*;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-audio-element">&lt;audio&gt;</a>
 * element.
 *
 * <p>
 * The audio element embeds sound content in documents. It can contain audio streams, audio files,
 * or other audio sources. The browser will choose the most appropriate source based on format
 * support and user preferences.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple audio with single source</jc>
 * 	Audio <jv>audio1</jv> = <jsm>audio</jsm>().src(<js>"audio.mp3"</js>).controls(<jk>true</jk>);
 *
 * 	<jc>// Audio with multiple sources for browser compatibility</jc>
 * 	Audio <jv>audio2</jv> = <jsm>audio</jsm>().controls(<jk>true</jk>)
 * 		.children(
 * 			<jsm>source</jsm>().src(<js>"audio.mp3"</js>).type(<js>"audio/mpeg"</js>),
 * 			<jsm>source</jsm>().src(<js>"audio.ogg"</js>).type(<js>"audio/ogg"</js>)
 * 		);
 *
 * 	<jc>// Autoplay audio with loop</jc>
 * 	Audio <jv>audio3</jv> = <jsm>audio</jsm>().src(<js>"background.mp3"</js>).autoplay(<jk>true</jk>).loop(<jk>true</jk>).muted(<jk>true</jk>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#audio() audio()}
 * 		<li class='jm'>{@link HtmlBuilder#audio(String) audio(String)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "audio")
public class Audio extends HtmlElementMixed<Audio> {

	/**
	 * Creates an empty {@link Audio} element.
	 */
	public Audio() {}

	/**
	 * Creates an {@link Audio} element with the specified {@link Audio#src(Object)} attribute.
	 *
	 * @param src The {@link Audio#src(Object)} attribute. Can be <jk>null</jk>, in which case the attribute is stored with a <jk>null</jk> value (not removed).
	 */
	public Audio(String src) {
		src(src);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-autoplay">autoplay</a>
	 * attribute.
	 *
	 * <p>
	 * Hint that the media resource can be started automatically when the page is loaded.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"autoplay"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio autoplay(Object value) {
		attr("autoplay", deminimize(value, "autoplay"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-controls">controls</a>
	 * attribute.
	 *
	 * <p>
	 * Show user agent controls.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"controls"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio controls(Object value) {
		attr("controls", deminimize(value, "controls"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-crossorigin">crossorigin</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies how the element handles cross-origin requests for CORS (Cross-Origin Resource Sharing).
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"anonymous"</js> - Cross-origin requests are made without credentials</li>
	 * 	<li><js>"use-credentials"</js> - Cross-origin requests include credentials</li>
	 * </ul>
	 *
	 * @param value How to handle cross-origin requests. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-loop">loop</a> attribute.
	 *
	 * <p>
	 * Causes the media to automatically restart from the beginning when it reaches the end.
	 *
	 * @param value If <jk>true</jk>, the media will loop continuously. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio loop(Object value) {
		attr("loop", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-mediagroup">mediagroup</a>
	 * attribute.
	 *
	 * <p>
	 * Groups multiple media elements together so they can be controlled as a single unit. All media elements
	 * with the same mediagroup value will share the same MediaController, allowing synchronized playback.
	 *
	 * <p>
	 * This is useful for creating synchronized audio/video presentations or multiple audio tracks.
	 *
	 * @param value The name of the media group to join. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio mediagroup(String value) {
		attr("mediagroup", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-muted">muted</a>
	 * attribute.
	 *
	 * <p>
	 * Mutes the audio output by default. Useful for autoplay videos where audio should be disabled initially.
	 *
	 * @param value If <jk>true</jk>, the media will be muted by default. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio muted(Object value) {
		attr("muted", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-preload">preload</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies how the browser should load the media resource.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"none"</js> - Do not preload the media</li>
	 * 	<li><js>"metadata"</js> - Preload only metadata (duration, dimensions, etc.)</li>
	 * 	<li><js>"auto"</js> - Preload the entire media file (default)</li>
	 * </ul>
	 *
	 * @param value How much of the media to preload. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Audio preload(Object value) {
		attr("preload", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-src">src</a> attribute.
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
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link URL} or {@link String}.
	 * 	Can be <jk>null</jk>, in which case the attribute is stored with a <jk>null</jk> value (not removed).
	 * @return This object.
	 */
	public Audio src(Object value) {
		attrUri("src", value);
		return this;
	}

}