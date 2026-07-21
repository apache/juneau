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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-rtc-element">&lt;rtc&gt;</a>
 * element.
 *
 * <p>
 * The rtc element represents a ruby text container for a ruby annotation. It is used within
 * a ruby element to group multiple ruby text (rt) elements together. The rtc element is part
 * of the ruby annotation system and is used to provide multiple levels of annotations for
 * the same base text. It is typically used when you need to provide both pronunciation and
 * translation annotations for the same text, or when you need to group related ruby text
 * elements together.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ruby text container</jc>
 * 	Rtc <jv>simple</jv> = <jsm>rtc</jsm>(
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>),
 * 		<jsm>rt</jsm>(<js>"Chinese characters"</js>)
 * 	);
 *
 * 	<jc>// Rtc with styling</jc>
 * 	Rtc <jv>styled</jv> = <jsm>rtc</jsm>(
 * 		<jsm>rt</jsm>(<js>"にほんご"</js>),
 * 		<jsm>rt</jsm>(<js>"Japanese language"</js>)
 * 	).class_(<js>"ruby-text-container"</js>);
 *
 * 	<jc>// Rtc with complex content</jc>
 * 	Rtc <jv>complex</jv> = <jsm>rtc</jsm>(
 * 		<jsm>rt</jsm>(<js>"ふくざつな"</js>),
 * 		<jsm>rt</jsm>(<js>"complex"</js>),
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>),
 * 		<jsm>rt</jsm>(<js>"kanji"</js>)
 * 	);
 *
 * 	<jc>// Rtc with ID</jc>
 * 	Rtc <jv>withId</jv> = <jsm>rtc</jsm>(
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>),
 * 		<jsm>rt</jsm>(<js>"Chinese characters"</js>)
 * 	).id(<js>"ruby-text-container-1"</js>);
 *
 * 	<jc>// Rtc with styling</jc>
 * 	Rtc <jv>styled2</jv> = <jsm>rtc</jsm>(
 * 		.style("font-size: 0.8em; color: #666;")
 * 		.children(
 * 			new Rt().children("かんじ"),
 * 			new Rt().children("Chinese characters")
 * 		);
 *
 * 	// Rtc with multiple elements
 * 	Rtc multiple = new Rtc()
 * 		.children(
 * 			new Rt().children("ふくざつな"),
 * 			new Rt().children("complex"),
 * 			new Rt().children("かんじ"),
 * 			new Rt().children("kanji")
 * 		);
 *
 * 	// Rtc with links
 * 	Rtc withLinks = new Rtc()
 * 		.children(
 * 			new Rt().children("かんじ"),
 * 			new Rt().children(
 * 				new A().href("/dictionary/kanji").children("Chinese characters")
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "rtc")
public class Rtc extends HtmlElementMixed<Rtc> {

	/**
	 * Creates an empty {@link Rtc} element.
	 */
	public Rtc() {}

	/**
	 * Creates an {@link Rtc} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Rtc(Object...children) {
		children(children);
	}
}