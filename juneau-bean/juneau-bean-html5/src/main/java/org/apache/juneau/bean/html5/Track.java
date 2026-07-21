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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-track-element">&lt;track&gt;</a>
 * element.
 *
 * <p>
 * The track element is used to specify timed text tracks for media elements (audio and video).
 * It allows you to add subtitles, captions, descriptions, chapters, or metadata to media content,
 * making it more accessible and user-friendly.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Subtitles track</jc>
 * 	Track <jv>subtitles</jv> = <jsm>track</jsm>()
 * 		.kind(<js>"subtitles"</js>)
 * 		.src(<js>"/media/subtitles-en.vtt"</js>)
 * 		.srclang(<js>"en"</js>)
 * 		.label(<js>"English"</js>);
 *
 * 	<jc>// Captions track (for deaf/hard of hearing)</jc>
 * 	Track <jv>captions</jv> = <jsm>track</jsm>()
 * 		.kind(<js>"captions"</js>)
 * 		.src(<js>"/media/captions-en.vtt"</js>)
 * 		.srclang(<js>"en"</js>)
 * 		.label(<js>"English Captions"</js>)
 * 		.default_(<jk>true</jk>);
 *
 * 	<jc>// Descriptions track (for audio descriptions)</jc>
 * 	Track <jv>descriptions</jv> = <jsm>track</jsm>()
 * 		.kind(<js>"descriptions"</js>)
 * 		.src(<js>"/media/descriptions-en.vtt"</js>)
 * 		.srclang(<js>"en"</js>)
 * 		.label(<js>"English Descriptions"</js>);
 *
 * 	<jc>// Chapters track</jc>
 * 	Track <jv>chapters</jv> = <jsm>track</jsm>()
 * 		.kind(<js>"chapters"</js>)
 * 		.src(<js>"/media/chapters.vtt"</js>)
 * 		.srclang(<js>"en"</js>)
 * 		.label(<js>"Chapters"</js>);
 *
 * 	<jc>// Video with multiple tracks</jc>
 * 	Video <jv>video</jv> = <jsm>video</jsm>()
 * 		.src(<js>"/media/movie.mp4"</js>)
 * 		.controls(<jk>true</jk>)
 * 		.children(
 * 			<jsm>track</jsm>(<js>"/media/subtitles-en.vtt"</js>, <js>"subtitles"</js>).srclang(<js>"en"</js>).label(<js>"English"</js>).default_(<jk>true</jk>),
 * 			<jsm>track</jsm>(<js>"/media/subtitles-es.vtt"</js>, <js>"subtitles"</js>).srclang(<js>"es"</js>).label(<js>"Español"</js>),
 * 			<jsm>track</jsm>(<js>"/media/subtitles-fr.vtt"</js>, <js>"subtitles"</js>).srclang(<js>"fr"</js>).label(<js>"Français"</js>)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#track() track()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "track")
public class Track extends HtmlElementVoid<Track> {

	/**
	 * Creates an empty {@link Track} element.
	 */
	public Track() {}

	/**
	 * Creates a {@link Track} element with the specified {@link Track#src(Object)} and {@link Track#kind(String)}
	 * attributes.
	 *
	 * @param src The {@link Track#src(Object)} attribute. Can be <jk>null</jk>.
	 * @param kind The {@link Track#kind(String)} attribute. Can be <jk>null</jk>.
	 */
	public Track(Object src, String kind) {
		src(src).kind(kind);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-default">default</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies that this track should be enabled by default if no other text track is more suitable.
	 * This is useful for providing fallback captions or subtitles.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Track is not enabled by default (default)</li>
	 * 	<li><jk>true</jk> - Track is enabled by default</li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value Whether this track should be enabled by default. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S100" // Method name uses underscore suffix to avoid Java keyword conflict
	})
	public Track default_(Object value) {
		attr("default", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-kind">kind</a> attribute.
	 *
	 * <p>
	 * Specifies the type of text track. This determines how the track is used by the media element.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"subtitles"</js> - Subtitles for the deaf and hard-of-hearing</li>
	 * 	<li><js>"captions"</js> - Captions for the deaf and hard-of-hearing</li>
	 * 	<li><js>"descriptions"</js> - Text descriptions of the video content</li>
	 * 	<li><js>"chapters"</js> - Chapter titles for navigation</li>
	 * 	<li><js>"metadata"</js> - Metadata for the media</li>
	 * </ul>
	 *
	 * @param value The type of text track. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Track kind(String value) {
		attr("kind", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-label">label</a> attribute.
	 *
	 * <p>
	 * Specifies a user-visible label for the text track. This label is displayed in the media
	 * player's track selection menu.
	 *
	 * <p>
	 * The label should be descriptive and help users identify the track (e.g., "English Subtitles",
	 * "Spanish Captions").
	 *
	 * @param value The user-visible label for the text track. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Track label(String value) {
		attr("label", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-src">src</a> attribute.
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
	 * 	Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Track src(Object value) {
		attrUri("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-track-srclang">srclang</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the language of the text track. This helps browsers and media players
	 * determine which track to use based on the user's language preferences.
	 *
	 * <p>
	 * The value should be a valid language code (e.g., "en", "es", "fr", "de").
	 *
	 * @param value The language code of the text track. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Track srclang(String value) {
		attr("srclang", value);
		return this;
	}

}