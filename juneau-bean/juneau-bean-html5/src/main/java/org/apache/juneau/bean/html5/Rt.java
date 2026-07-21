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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-rt-element">&lt;rt&gt;</a>
 * element.
 *
 * <p>
 * The rt element represents the ruby text component of a ruby annotation. It is used within
 * a ruby element to mark up the annotation text that provides pronunciation or translation
 * information for the base text. The rt element is part of the ruby annotation system and
 * is used to provide annotations above or below the base text in languages that use complex
 * writing systems, such as Japanese, Chinese, or Korean. It is typically used with rb (ruby
 * base) elements to provide complete ruby annotations.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ruby text</jc>
 * 	Rt <jv>simple</jv> = <jsm>rt</jsm>(<js>"かんじ"</js>);
 *
 * 	<jc>// Rt with styling</jc>
 * 	Rt <jv>styled</jv> = <jsm>rt</jsm>(<js>"にほんご"</js>)
 * 		.class_(<js>"ruby-text"</js>);
 *
 * 	<jc>// Rt with complex content</jc>
 * 	Rt <jv>complex</jv> = <jsm>rt</jsm>(
 * 		<js>"ふくざつな"</js>,
 * 		<jsm>strong</jsm>(<js>"かんじ"</js>),
 * 		<js>"のれい"</js>
 * 	);
 *
 * 	<jc>// Rt with ID</jc>
 * 	Rt <jv>withId</jv> = <jsm>rt</jsm>(<js>"かんじ"</js>)
 * 		.id(<js>"ruby-text-1"</js>);
 *
 * 	<jc>// Rt with styling</jc>
 * 	Rt <jv>styled2</jv> = <jsm>rt</jsm>(<js>"かんじ"</js>)
 * 		.style(<js>"font-size: 0.8em; color: #666;"</js>);
 *
 * 	<jc>// Rt with multiple elements</jc>
 * 	Rt <jv>multiple</jv> = <jsm>rt</jsm>(
 * 		<js>"ふくざつな"</js>,
 * 		<jsm>rt</jsm>(<js>"かんじ"</js>),
 * 		<js>"の"</js>,
 * 		<jsm>rt</jsm>(<js>"れい"</js>)
 * 		);
 *
 * 	// Rt with links
 * 	Rt withLinks = new Rt()
 * 		.children(
 * 			"かんじ",
 * 			new A().href("/dictionary/kanji").children("じしょ")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "rt")
public class Rt extends HtmlElementMixed<Rt> {

	/**
	 * Creates an empty {@link Rt} element.
	 */
	public Rt() {}

	/**
	 * Creates a {@link Rt} element with the specified {@link Rt#children(Object[])} nodes.
	 *
	 * @param children The {@link Rt#children(Object[])} nodes. Must not be <jk>null</jk>.
	 */
	public Rt(Object...children) {
		children(children);
	}
}