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
@Bean(typeName="audio")
public class Audio extends HtmlElementMixed {

	/**
	 * Creates an empty {@link Audio} element.
	 */
	public Audio() {}

	/**
	 * Creates an {@link Audio} element with the specified {@link Audio#src(Object)} attribute.
	 *
	 * @param src The {@link Audio#src(Object)} attribute.
	 */
	public Audio(String src) {
		src(src);
	}

	@Override /* Overridden from HtmlElement */
	public Audio _class(String value) {  // NOSONAR - Intentional naming.
		super._class(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio accesskey(String value) {
		super.accesskey(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio attr(String key, Object val) {
		super.attr(key, val);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio attrUri(String key, Object val) {
		super.attrUri(key, val);
		return this;
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
	 * @return This object.
	 */
	public Audio autoplay(Object value) {
		attr("autoplay", deminimize(value, "autoplay"));
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Audio child(Object value) {
		super.child(value);
		return this;
	}

	@Override /* Overridden from HtmlElementContainer */
	public Audio children(Object...value) {
		super.children(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio contenteditable(Object value) {
		super.contenteditable(value);
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
	 * @param value How to handle cross-origin requests.
	 * @return This object.
	 */
	public Audio crossorigin(String value) {
		attr("crossorigin", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio dir(String value) {
		super.dir(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio hidden(Object value) {
		super.hidden(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio id(String value) {
		super.id(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio lang(String value) {
		super.lang(value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-media-loop">loop</a> attribute.
	 *
	 * <p>
	 * Causes the media to automatically restart from the beginning when it reaches the end.
	 *
	 * @param value If <jk>true</jk>, the media will loop continuously.
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
	 * @param value The name of the media group to join.
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
	 * @param value If <jk>true</jk>, the media will be muted by default.
	 * @return This object.
	 */
	public Audio muted(Object value) {
		attr("muted", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onabort(String value) {
		super.onabort(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onblur(String value) {
		super.onblur(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oncancel(String value) {
		super.oncancel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oncanplay(String value) {
		super.oncanplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oncanplaythrough(String value) {
		super.oncanplaythrough(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onchange(String value) {
		super.onchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onclick(String value) {
		super.onclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oncuechange(String value) {
		super.oncuechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio ondblclick(String value) {
		super.ondblclick(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio ondurationchange(String value) {
		super.ondurationchange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onemptied(String value) {
		super.onemptied(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onended(String value) {
		super.onended(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onerror(String value) {
		super.onerror(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onfocus(String value) {
		super.onfocus(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oninput(String value) {
		super.oninput(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio oninvalid(String value) {
		super.oninvalid(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onkeydown(String value) {
		super.onkeydown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onkeypress(String value) {
		super.onkeypress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onkeyup(String value) {
		super.onkeyup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onload(String value) {
		super.onload(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onloadeddata(String value) {
		super.onloadeddata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onloadedmetadata(String value) {
		super.onloadedmetadata(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onloadstart(String value) {
		super.onloadstart(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmousedown(String value) {
		super.onmousedown(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmouseenter(String value) {
		super.onmouseenter(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmouseleave(String value) {
		super.onmouseleave(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmousemove(String value) {
		super.onmousemove(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmouseout(String value) {
		super.onmouseout(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmouseover(String value) {
		super.onmouseover(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmouseup(String value) {
		super.onmouseup(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onmousewheel(String value) {
		super.onmousewheel(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onpause(String value) {
		super.onpause(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onplay(String value) {
		super.onplay(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onplaying(String value) {
		super.onplaying(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onprogress(String value) {
		super.onprogress(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onratechange(String value) {
		super.onratechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onreset(String value) {
		super.onreset(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onresize(String value) {
		super.onresize(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onscroll(String value) {
		super.onscroll(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onseeked(String value) {
		super.onseeked(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onseeking(String value) {
		super.onseeking(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onselect(String value) {
		super.onselect(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onshow(String value) {
		super.onshow(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onstalled(String value) {
		super.onstalled(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onsubmit(String value) {
		super.onsubmit(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onsuspend(String value) {
		super.onsuspend(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio ontimeupdate(String value) {
		super.ontimeupdate(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio ontoggle(String value) {
		super.ontoggle(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onvolumechange(String value) {
		super.onvolumechange(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio onwaiting(String value) {
		super.onwaiting(value);
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
	 * @param value How much of the media to preload.
	 * @return This object.
	 */
	public Audio preload(Object value) {
		attr("preload", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio spellcheck(Object value) {
		super.spellcheck(value);
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
	 * @return This object.
	 */
	public Audio src(Object value) {
		attrUri("src", value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio style(String value) {
		super.style(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio tabindex(Object value) {
		super.tabindex(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio title(String value) {
		super.title(value);
		return this;
	}

	@Override /* Overridden from HtmlElement */
	public Audio translate(Object value) {
		super.translate(value);
		return this;
	}
}