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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-source-element">&lt;source&gt;</a>
 * element.
 *
 * <p>
 * The source element specifies multiple media resources for media elements like audio and video.
 * It allows browsers to choose the most appropriate source based on format support, bandwidth,
 * and other factors. The source element is used inside audio and video elements to provide
 * fallback options for different media formats.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Video with multiple sources</jc>
 * 	Video <jv>video</jv> = <jsm>video</jsm>()
 * 		.controls(<jk>true</jk>)
 * 		.children(
 * 			<jsm>source</jsm>().src(<js>"movie.mp4"</js>).type(<js>"video/mp4"</js>),
 * 			<jsm>source</jsm>().src(<js>"movie.webm"</js>).type(<js>"video/webm"</js>),
 * 			<jsm>source</jsm>().src(<js>"movie.ogg"</js>).type(<js>"video/ogg"</js>)
 * 		);
 *
 * 	<jc>// Audio with multiple sources</jc>
 * 	Audio <jv>audio</jv> = <jsm>audio</jsm>()
 * 		.controls(<jk>true</jk>)
 * 		.children(
 * 			<jsm>source</jsm>().src(<js>"sound.mp3"</js>).type(<js>"audio/mpeg"</js>),
 * 			<jsm>source</jsm>().src(<js>"sound.ogg"</js>).type(<js>"audio/ogg"</js>),
 * 			<jsm>source</jsm>().src(<js>"sound.wav"</js>).type(<js>"audio/wav"</js>)
 * 		);
 *
 * 	<jc>// Picture with multiple sources</jc>
 * 	Picture <jv>picture</jv> = <jsm>picture</jsm>(
 * 		<jsm>source</jsm>().src(<js>"image.webp"</js>).type(<js>"image/webp"</js>),
 * 		<jsm>source</jsm>().src(<js>"image.jpg"</js>).type(<js>"image/jpeg"</js>)
 * 	);
 *
 * 	<jc>// Source with media query</jc>
 * 	Source <jv>responsive</jv> = <jsm>source</jsm>()
 * 		.src(<js>"large-image.jpg"</js>)
 * 		.media(<js>"(min-width: 800px)"</js>)
 * 		.type(<js>"image/jpeg"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#source() source()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "source")
public class Source extends HtmlElementVoid<Source> {

	/**
	 * Creates an empty {@link Source} element.
	 */
	public Source() {}

	/**
	 * Creates a {@link Source} element with the specified {@link Source#src(Object)} and {@link Source#type(String)}
	 * attributes.
	 *
	 * @param src The {@link Source#src(Object)} attribute. Can be <jk>null</jk>.
	 * @param type The {@link Source#type(String)} attribute. Can be <jk>null</jk>.
	 */
	public Source(Object src, String type) {
		src(src).type(type);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-source-src">src</a> attribute.
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
	public Source src(Object value) {
		attrUri("src", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-source-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the MIME type of the media resource. Helps browsers determine if they can play the media
	 * and which source to use when multiple sources are provided.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"video/mp4"</js> - MP4 video</li>
	 * 	<li><js>"video/webm"</js> - WebM video</li>
	 * 	<li><js>"audio/mp3"</js> - MP3 audio</li>
	 * 	<li><js>"audio/ogg"</js> - OGG audio</li>
	 * 	<li><js>"audio/wav"</js> - WAV audio</li>
	 * </ul>
	 *
	 * @param value The MIME type of the media resource. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Source type(String value) {
		attr("type", value);
		return this;
	}
}