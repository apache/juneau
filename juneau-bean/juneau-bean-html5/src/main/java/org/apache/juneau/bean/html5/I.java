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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-i-element">&lt;i&gt;</a>
 * element.
 *
 * <p>
 * The i element represents a span of text in an alternate voice or mood, or otherwise offset
 * from the normal prose in a manner indicating a different quality of text, such as a taxonomic
 * designation, a technical term, an idiomatic phrase from another language, a thought, or a
 * ship name in Western texts. It is typically rendered in italic text and is used to mark up
 * text that should be distinguished from the surrounding content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple italic text</jc>
 * 	I <jv>simple</jv> = <jsm>i</jsm>(<js>"This is italic text"</js>);
 *
 * 	<jc>// I with styling</jc>
 * 	I <jv>styled</jv> = <jsm>i</jsm>(<js>"Styled italic text"</js>)
 * 		.class_(<js>"emphasis"</js>);
 *
 * 	<jc>// I with complex content</jc>
 * 	I <jv>complex</jv> = <jsm>i</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>strong</jsm>(<js>"HMS Victory"</js>),
 * 		<js>" was a famous ship."</js>
 * 	);
 *
 * 	<jc>// I with ID</jc>
 * 	I <jv>withId</jv> = <jsm>i</jsm>(<js>"Text with ID"</js>)
 * 		.id(<js>"italic-text"</js>);
 *
 * 	<jc>// I with styling</jc>
 * 	I <jv>styled2</jv> = <jsm>i</jsm>(<js>"Custom styled italic text"</js>)
 * 		.style(<js>"color: #666; font-style: italic;"</js>);
 *
 * 	<jc>// I with multiple elements</jc>
 * 	I <jv>multiple</jv> = <jsm>i</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>i</jsm>(<js>"HMS Victory"</js>),
 * 		<js>" was a "</js>,
 * 		<jsm>i</jsm>(<js>"first-rate"</js>),
 * 		<js>" ship of the line."</js>
 * 	);
 *
 * 	<jc>// I with links</jc>
 * 	I <jv>withLinks</jv> = <jsm>i</jsm>(
 * 		<js>"See "</js>,
 * 		<jsm>a</jsm>(<js>"/ships"</js>).children(<js>"ship database"</js>),
 * 		<js>" for more information."</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#i() i()}
 * 		<li class='jm'>{@link HtmlBuilder#i(Object...) i(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "i")
public class I extends HtmlElementMixed<I> {

	/**
	 * Creates an empty {@link I} element.
	 */
	public I() {}

	/**
	 * Creates an {@link I} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public I(Object...children) {
		children(children);
	}

}