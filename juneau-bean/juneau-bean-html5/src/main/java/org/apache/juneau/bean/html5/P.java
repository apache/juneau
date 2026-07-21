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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-p-element">&lt;p&gt;</a>
 * element.
 *
 * <p>
 * The p element represents a paragraph of text. It is one of the most commonly used HTML elements
 * for structuring text content. Paragraphs are block-level elements that automatically add spacing
 * before and after the content, making them ideal for organizing textual information into readable sections.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple paragraph</jc>
 * 	P <jv>simple</jv> = <jsm>p</jsm>(<js>"This is a simple paragraph of text."</js>);
 *
 * 	<jc>// Paragraph with styling</jc>
 * 	P <jv>styled</jv> = <jsm>p</jsm>(<js>"This paragraph has custom styling."</js>)
 * 		.class_(<js>"lead"</js>);
 *
 * 	<jc>// Paragraph with mixed content</jc>
 * 	P <jv>mixed</jv> = <jsm>p</jsm>(
 * 		<js>"This paragraph contains "</js>,
 * 		<jsm>strong</jsm>(<js>"bold text"</js>),
 * 		<js>" and "</js>,
 * 		<jsm>em</jsm>(<js>"italic text"</js>),
 * 		<js>"."</js>
 * 	);
 *
 * 	<jc>// Paragraph with link</jc>
 * 	P <jv>withLink</jv> = <jsm>p</jsm>(
 * 		<js>"Visit our "</js>,
 * 		<jsm>a</jsm>(<js>"/help"</js>, <js>"help page"</js>),
 * 		<js>" for more information."</js>
 * 	);
 *
 * 	<jc>// Paragraph with ID and class</jc>
 * 	P <jv>withAttrs</jv> = <jsm>p</jsm>(<js>"Important notice"</js>)
 * 		.id(<js>"notice"</js>)
 * 		.class_(<js>"alert alert-warning"</js>);
 *
 * 	<jc>// Paragraph with inline styles</jc>
 * 	P <jv>withStyle</jv> = <jsm>p</jsm>(<js>"Styled paragraph"</js>)
 * 		.style(<js>"color: blue; font-size: 16px;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#p() p()}
 * 		<li class='jm'>{@link HtmlBuilder#p(Object...) p(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "p")
public class P extends HtmlElementMixed<P> {

	/**
	 * Creates an empty {@link P} element.
	 */
	public P() {}

	/**
	 * Creates a {@link P} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public P(Object...children) {
		children(children);
	}

}