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

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-ruby-element">&lt;ruby&gt;</a>
 * element.
 *
 * <p>
 * The ruby element represents a ruby annotation. It is used to provide pronunciation or
 * translation information for text in languages that use complex writing systems, such as
 * Japanese, Chinese, or Korean. The ruby element typically contains rb (ruby base) elements
 * for the base text and rt (ruby text) elements for the annotations. It can also contain
 * rp (ruby parenthesis) elements for fallback display when ruby annotations are not supported.
 * Ruby annotations are commonly used to provide furigana (pronunciation guides) for Japanese text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ruby annotation</jc>
 * 	Ruby <jv>simple</jv> = <jsm>ruby</jsm>(
 * 		<jsm>rb</jsm>(<js>"漢字"</js>),
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>)
 * 	);
 *
 * 	<jc>// Ruby with styling</jc>
 * 	Ruby <jv>styled</jv> = <jsm>ruby</jsm>(
 * 		<jsm>rb</jsm>(<js>"日本語"</js>),
 * 		<jsm>rt</jsm>(<js>"にほんご"</js>)
 * 	).class_(<js>"ruby-annotation"</js>);
 *
 * 	<jc>// Ruby with complex content</jc>
 * 	Ruby <jv>complex</jv> = <jsm>ruby</jsm>(
 * 		<jsm>rb</jsm>(<js>"複雑な漢字"</js>),
 * 		<jsm>rt</jsm>(<js>"ふくざつなかんじ"</js>),
 * 		<jsm>rp</jsm>(<js>"("</js>),
 * 		<jsm>rp</jsm>(<js>")"</js>)
 * 	);
 *
 * 	<jc>// Ruby with ID</jc>
 * 	Ruby <jv>withId</jv> = <jsm>ruby</jsm>(
 * 		<jsm>rb</jsm>(<js>"漢字"</js>),
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>)
 * 	).id(<js>"ruby-annotation-1"</js>);
 *
 * 	<jc>// Ruby with styling</jc>
 * 	Ruby <jv>styled2</jv> = <jsm>ruby</jsm>(
 * 		.style("font-size: 1.2em; line-height: 1.5;")
 * 		.children(
 * 			new Rb().children("漢字"),
 * 			new Rt().children("かんじ")
 * 		);
 *
 * 	// Ruby with multiple elements
 * 	Ruby multiple = new Ruby()
 * 		.children(
 * 			new Rb().children("複雑な"),
 * 			new Rt().children("ふくざつな"),
 * 			new Rb().children("漢字"),
 * 			new Rt().children("かんじ")
 * 		);
 *
 * 	// Ruby with links
 * 	Ruby withLinks = new Ruby()
 * 		.children(
 * 			new Rb().children("漢字"),
 * 			new Rt().children(
 * 				new A().href("/dictionary/kanji").children("かんじ")
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "ruby")
public class Ruby extends HtmlElementMixed<Ruby> {

	/**
	 * Creates an empty {@link Ruby} element.
	 */
	public Ruby() {}

	/**
	 * Creates a {@link Ruby} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Ruby(Object...children) {
		children(children);
	}
}