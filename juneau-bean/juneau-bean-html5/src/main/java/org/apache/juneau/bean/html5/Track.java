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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

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
 * 		._default(<jk>true</jk>);
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
 * 			<jsm>track</jsm>(<js>"/media/subtitles-en.vtt"</js>, <js>"subtitles"</js>).srclang(<js>"en"</js>).label(<js>"English"</js>)._default(<jk>true</jk>),
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
@Bean(typeName = "track")
public class Track extends HtmlElementVoid {

	/**
	 * Creates an empty {@link Track} element.
	 */
	public Track() {}

	/**
	 * Creates a {@link Track} element with the specified {@link Track#src(Object)} and {@link Track#kind(String)}
	 * attributes.
	 *
	 * @param src The {@link Track#src(Object)} attribute.
	 * @param kind The {@link Track#kind(String)} attribute.
	 */
	public Track(Object src, String kind) {
		src(src).kind(kind);
	}

	@Override /* Overridden from HtmlElement */
	public Track _class(String value) { // NOSONAR - Intentional naming.
		super._class(value);
		return this;
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
	 * @param value Whether this track should be enabled by default.
	 * @return This object.
	 */
	public Track _default(String value) { // NOSONAR - Intentional naming.
		attr("default", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track contenteditable(Object value) {
		super.contenteditable(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track id(String value) {
		super.id(value);
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
	 * @param value The type of text track.
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
	 * @param value The user-visible label for the text track.
	 * @return This object.
	 */
	public Track label(String value) {
		attr("label", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track lang(String value) {
		super.lang(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track onwaiting(String value) {
		super.onwaiting(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track spellcheck(Object value) {
		super.spellcheck(value);
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
	 * @param value The language code of the text track.
	 * @return This object.
	 */
	public Track srclang(String value) {
		attr("srclang", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Track translate(Object value) {
		super.translate(value);
		return this;
	}
}