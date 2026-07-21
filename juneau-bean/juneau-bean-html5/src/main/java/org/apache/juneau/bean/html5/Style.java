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

import static org.apache.juneau.commons.utils.StringUtils.*;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-style-element">&lt;style&gt;</a>
 * element.
 *
 * <p>
 * The style element allows authors to embed CSS style information in their documents. It contains
 * CSS rules that apply to the document. The style element is typically placed in the head section
 * of the document, but can also be used inline. The CSS contained within the style element is
 * processed by the browser and applied to the document.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Basic CSS styles</jc>
 * 	Style <jv>basic</jv> = <jsm>style</jsm>()
 * 		.text(<js>"body { font-family: Arial, sans-serif; }"</js>);
 *
 * 	<jc>// CSS with media query</jc>
 * 	Style <jv>responsive</jv> = <jsm>style</jsm>()
 * 		.media(<js>"screen and (max-width: 600px)"</js>)
 * 		.text(<js>"body { font-size: 14px; }"</js>);
 *
 * 	<jc>// Multiple CSS rules</jc>
 * 	Style <jv>multiple</jv> = <jsm>style</jsm>()
 * 		.text(
 * 			<js>"h1 { color: blue; }"</js>,
 * 			<js>"p { margin: 10px; }"</js>,
 * 			<js>".highlight { background-color: yellow; }"</js>
 * 		);
 *
 * 	<jc>// CSS with type specification</jc>
 * 	Style <jv>typed</jv> = <jsm>style</jsm>()
 * 		.type(<js>"text/css"</js>)
 * 		.text(<js>".button { padding: 10px; background: #007bff; }"</js>);
 *
 * 	<jc>// Print-specific styles</jc>
 * 	Style <jv>print</jv> = <jsm>style</jsm>()
 * 		.media(<js>"print"</js>)
 * 		.text(<js>"body { color: black; background: white; }"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#style() style()}
 * 		<li class='jm'>{@link HtmlBuilder#style(Object) style(Object)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "style")
public class Style extends HtmlElementRawText<Style> {

	/**
	 * Creates an empty {@link Style} element.
	 */
	public Style() {}

	/**
	 * Creates a {@link Style} element with the specified {@link Style#text(Object)} node.
	 *
	 * @param text The {@link Style#text(Object)} node. Can be <jk>null</jk>.
	 */
	public Style(Object text) {
		text(text);
	}

	/**
	 * Creates a {@link Style} element with the specified inner text.
	 *
	 * @param text
	 * 	The contents of the style element.
	 * 	<br>Values will be concatenated with newlines.
	 */
	public Style(String...text) {
		text(joinnl(text));
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-style-media">media</a> attribute.
	 *
	 * <p>
	 * Specifies the media types for which the stylesheet applies. This allows you to target
	 * specific devices or media types.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 * 	<li><js>"all"</js> - All media types (default)</li>
	 * 	<li><js>"screen"</js> - Computer screens</li>
	 * 	<li><js>"print"</js> - Printers and print preview</li>
	 * 	<li><js>"handheld"</js> - Handheld devices</li>
	 * 	<li><js>"projection"</js> - Projectors</li>
	 * 	<li><js>"tv"</js> - Television</li>
	 * </ul>
	 *
	 * @param value The media types for which the stylesheet applies. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Style media(String value) {
		attr("media", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#attr-style-type">type</a> attribute.
	 *
	 * <p>
	 * Type of embedded resource.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Style type(String value) {
		attr("type", value);
		return this;
	}
}
