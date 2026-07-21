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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-bdi-element">&lt;bdi&gt;</a>
 * element.
 *
 * <p>
 * The bdi element represents a span of text that is to be isolated from its surroundings for the
 * purposes of bidirectional text formatting. It is used to handle text that might be in a different
 * direction than the surrounding text, such as user-generated content in a different language or
 * script. The bdi element ensures that the text inside it is formatted correctly regardless of the
 * directionality of the surrounding content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// User comment in Arabic</jc>
 * 	Bdi <jv>arabic</jv> = <jsm>bdi</jsm>(<js>"مرحبا بالعالم"</js>);
 *
 * 	<jc>// Hebrew text in English context</jc>
 * 	Bdi <jv>hebrew</jv> = <jsm>bdi</jsm>(<js>"שלום עולם"</js>);
 *
 * 	<jc>// Mixed direction text</jc>
 * 	Bdi <jv>mixed</jv> = <jsm>bdi</jsm>(<js>"Hello "</js>, <jsm>bdi</jsm>(<js>"مرحبا"</js>), <js>" World"</js>);
 *
 * 	<jc>// User-generated content</jc>
 * 	Bdi <jv>userContent</jv> = <jsm>bdi</jsm>(<js>"This is a comment in Arabic: مرحبا"</js>)
 * 		.class_(<js>"user-comment"</js>);
 *
 * 	<jc>// Names in different scripts</jc>
 * 	Bdi <jv>name</jv> = <jsm>bdi</jsm>(<js>"محمد أحمد"</js>);
 *
 * 	<jc>// Numbers in different scripts</jc>
 * 	Bdi <jv>numbers</jv> = <jsm>bdi</jsm>(<js>"١٢٣٤٥"</js>);
 *
 * 	<jc>// Styled bidirectional text</jc>
 * 	Bdi <jv>styled</jv> = <jsm>bdi</jsm>(<js>"نص باللغة العربية"</js>)
 * 		.class_(<js>"bidi-text"</js>)
 * 		.style(<js>"direction: rtl;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#bdi() bdi()}
 * 		<li class='jm'>{@link HtmlBuilder#bdi(Object) bdi(Object)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "bdi")
public class Bdi extends HtmlElementText<Bdi> {

	/**
	 * Creates an empty {@link Bdi} element.
	 */
	public Bdi() {}

	/**
	 * Creates a {@link Bdi} element with the specified {@link Bdi#text(Object)} node.
	 *
	 * @param text The {@link Bdi#text(Object)} node.
	 */
	public Bdi(Object text) {
		text(text);
	}
}
