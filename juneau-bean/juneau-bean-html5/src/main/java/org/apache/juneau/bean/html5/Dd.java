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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-dd-element">&lt;dd&gt;</a>
 * element.
 *
 * <p>
 * The dd element represents the description, definition, or value, part of a term-description group
 * in a description list (dl element). It is used to provide the definition or description for the
 * term that precedes it in a dt element. The dd element can contain any flow content and is typically
 * used within a dl element to create definition lists, glossaries, or other term-description pairs.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple definition</jc>
 * 	Dd <jv>simple</jv> = <jsm>dd</jsm>(<js>"A markup language used to create web pages."</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dd <jv>styled</jv> = <jsm>dd</jsm>(<js>"A programming language for web development."</js>)
 * 		.class_(<js>"definition"</js>);
 *
 * 	<jc>// Definition with complex content</jc>
 * 	Dd <jv>complex</jv> = <jsm>dd</jsm>(
 * 		<js>"A "</js>,
 * 		<jsm>strong</jsm>(<js>"hypertext"</js>),
 * 		<js>" markup language used to create "</js>,
 * 		<jsm>em</jsm>(<js>"web pages"</js>),
 * 		<js>"."</js>
 * 	);
 *
 * 	<jc>// Definition with multiple paragraphs</jc>
 * 	Dd <jv>multiple</jv> = <jsm>dd</jsm>(
 * 		<jsm>p</jsm>(<js>"A programming language that runs in web browsers."</js>),
 * 		<jsm>p</jsm>(<js>"It is commonly used for creating interactive web applications."</js>)
 * 	);
 *
 * 	<jc>// Definition with links</jc>
 * 	Dd <jv>withLinks</jv> = <jsm>dd</jsm>(
 * 		<js>"A "</js>,
 * 		<jsm>a</jsm>(<js>"/css"</js>, <js>"styling language"</js>),
 * 		<js>" used to describe the presentation of "</js>,
 * 		<jsm>a</jsm>(<js>"/html"</js>, <js>"HTML"</js>),
 * 		<js>" documents."</js>
 * 	);
 *
 * 	<jc>// Definition with ID</jc>
 * 	Dd <jv>withId</jv> = <jsm>dd</jsm>(<js>"A styling language for web documents."</js>)
 * 		.id(<js>"css-definition"</js>)
 * 		.children(<js>"Cascading Style Sheets - a language for describing the presentation of web pages."</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dd <jv>styled2</jv> = <jsm>dd</jsm>(<js>"A server-side scripting language for web development."</js>)
 * 		.style(<js>"margin-left: 20px; color: #666;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#dd() dd()}
 * 		<li class='jm'>{@link HtmlBuilder#dd(Object...) dd(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "dd")
public class Dd extends HtmlElementMixed<Dd> {

	/**
	 * Creates an empty {@link Dd} element.
	 */
	public Dd() {}

	/**
	 * Creates a {@link Dd} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Dd(Object...children) {
		children(children);
	}

}