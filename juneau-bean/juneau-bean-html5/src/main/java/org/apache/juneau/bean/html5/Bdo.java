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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-bdo-element">&lt;bdo&gt;</a>
 * element.
 *
 * <p>
 * The bdo element represents a span of text that is to be formatted in a different direction than
 * the surrounding text. It is used to override the bidirectional algorithm and explicitly set the
 * direction of text. The bdo element requires a dir attribute to specify the text direction, which
 * can be "ltr" (left-to-right) or "rtl" (right-to-left).
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Right-to-left text</jc>
 * 	Bdo <jv>rtl</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"مرحبا بالعالم"</js>);
 *
 * 	<jc>// Left-to-right text</jc>
 * 	Bdo <jv>ltr</jv> = <jsm>bdo</jsm>(<js>"ltr"</js>, <js>"Hello World"</js>);
 *
 * 	<jc>// Mixed direction text</jc>
 * 	Bdo <jv>mixed</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"Hello "</js>, <jsm>bdo</jsm>(<js>"ltr"</js>, <js>"World"</js>), <js>" مرحبا"</js>);
 *
 * 	<jc>// Hebrew text with explicit direction</jc>
 * 	Bdo <jv>hebrew</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"שלום עולם"</js>);
 *
 * 	<jc>// Arabic text with explicit direction</jc>
 * 	Bdo <jv>arabic</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"مرحبا بالعالم"</js>);
 *
 * 	<jc>// Styled bidirectional text</jc>
 * 	Bdo <jv>styled</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"نص باللغة العربية"</js>)
 * 		.class_(<js>"bidi-text"</js>)
 * 		.style(<js>"color: blue;"</js>);
 *
 * 	<jc>// Numbers with explicit direction</jc>
 * 	Bdo <jv>numbers</jv> = <jsm>bdo</jsm>(<js>"rtl"</js>, <js>"١٢٣٤٥"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#bdo() bdo()}
 * 		<li class='jm'>{@link HtmlBuilder#bdo(String, Object...) bdo(String, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "bdo")
public class Bdo extends HtmlElementMixed<Bdo> {

	/**
	 * Creates an empty {@link Bdo} element.
	 */
	public Bdo() {}

	/**
	 * Creates a {@link Bdo} element with the specified {@link Bdo#dir(String)} attribute and child nodes.
	 *
	 * @param dir The {@link Bdo#dir(String)} attribute.
	 * @param children The child nodes.
	 */
	public Bdo(String dir, Object...children) {
		dir(dir).children(children);
	}

}