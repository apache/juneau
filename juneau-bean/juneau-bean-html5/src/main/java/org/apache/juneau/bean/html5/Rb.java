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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-rb-element">&lt;rb&gt;</a>
 * element.
 *
 * <p>
 * The rb element represents the base text component of a ruby annotation. It is used within
 * a ruby element to mark up the base text that is being annotated. The rb element is part of
 * the ruby annotation system and is used to provide pronunciation or translation information
 * for text in languages that use complex writing systems, such as Japanese, Chinese, or Korean.
 * It is typically used with rt (ruby text) elements to provide annotations above or below
 * the base text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ruby base</jc>
 * 	Rb <jv>simple</jv> = <jsm>rb</jsm>(<js>"漢字"</js>);
 *
 * 	<jc>// Rb with styling</jc>
 * 	Rb <jv>styled</jv> = <jsm>rb</jsm>(<js>"日本語"</js>)
 * 		.class_(<js>"ruby-base"</js>);
 *
 * 	<jc>// Rb with complex content</jc>
 * 	Rb <jv>complex</jv> = <jsm>rb</jsm>(
 * 		<js>"複雑な"</js>,
 * 		<jsm>strong</jsm>(<js>"漢字"</js>),
 * 		<js>"の例"</js>
 * 	);
 *
 * 	<jc>// Rb with ID</jc>
 * 	Rb <jv>withId</jv> = <jsm>rb</jsm>(<js>"漢字"</js>)
 * 		.id(<js>"ruby-base-1"</js>);
 *
 * 	<jc>// Rb with styling</jc>
 * 	Rb <jv>styled2</jv> = <jsm>rb</jsm>(<js>"漢字"</js>)
 * 		.style(<js>"font-size: 1.2em; color: #333;"</js>);
 *
 * 	<jc>// Rb with multiple elements</jc>
 * 	Rb <jv>multiple</jv> = <jsm>rb</jsm>(
 * 		<js>"複雑な"</js>,
 * 		<jsm>rb</jsm>(<js>"漢字"</js>),
 * 		<js>"の"</js>,
 * 		<jsm>rb</jsm>(<js>"例"</js>)
 * 		);
 *
 * 	// Rb with links
 * 	Rb withLinks = new Rb()
 * 		.children(
 * 			"漢字",
 * 			new A().href("/dictionary/kanji").children("辞書")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "rb")
public class Rb extends HtmlElementMixed<Rb> {

	/**
	 * Creates an empty {@link Rb} element.
	 */
	public Rb() {}

	/**
	 * Creates a {@link Rb} element with the specified {@link Rb#children(Object[])} nodes.
	 *
	 * @param children The {@link Rb#children(Object[])} nodes. Must not be <jk>null</jk>.
	 */
	public Rb(Object...children) {
		children(children);
	}
}