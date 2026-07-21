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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-u-element">&lt;u&gt;</a>
 * element.
 *
 * <p>
 * The u element represents a span of text with an unarticulated, though explicitly rendered,
 * non-textual annotation. It is used to mark up text that should be underlined, such as
 * proper names in Chinese text, misspelled words, or text that needs to be distinguished
 * from the surrounding content. The u element is typically rendered with an underline and
 * is commonly used for indicating proper names, misspellings, or other annotations.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple underlined text</jc>
 * 	U <jv>simple</jv> = <jsm>u</jsm>(<js>"This text is underlined"</js>);
 *
 * 	<jc>// U with styling</jc>
 * 	U <jv>styled</jv> = <jsm>u</jsm>(<js>"Styled underlined text"</js>)
 * 		.class_(<js>"underlined"</js>);
 *
 * 	<jc>// U with complex content</jc>
 * 	U <jv>complex</jv> = <jsm>u</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>strong</jsm>(<js>"proper name"</js>),
 * 		<js>" is underlined."</js>
 * 	);
 *
 * 	<jc>// U with ID</jc>
 * 	U <jv>withId</jv> = <jsm>u</jsm>(<js>"Text with ID"</js>)
 * 		.id(<js>"underlined-text"</js>);
 *
 * 	<jc>// U with styling</jc>
 * 	U <jv>styled2</jv> = <jsm>u</jsm>(<js>"Custom underlined text"</js>)
 * 		.style(<js>"color: #666; text-decoration: underline;"</js>)
 * 		.children("Custom styled underlined text");
 *
 * 	// U with multiple elements
 * 	U multiple = u()
 * 		.children(
 * 			"The ",
 * 			u().children("proper name"),
 * 			" is ",
 * 			u().children("underlined"),
 * 			" for emphasis."
 * 		);
 *
 * 	// U with links
 * 	U withLinks = u()
 * 		.children(
 * 			"See ",
 * 			a().href("/help/underline").children("underline guide"),
 * 			" for more information."
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#u() u()}
 * 		<li class='jm'>{@link HtmlBuilder#u(Object...) u(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "u")
public class U extends HtmlElementMixed<U> {

	/**
	 * Creates an empty {@link U} element.
	 */
	public U() {}

	/**
	 * Creates a {@link U} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public U(Object...children) {
		children(children);
	}
}