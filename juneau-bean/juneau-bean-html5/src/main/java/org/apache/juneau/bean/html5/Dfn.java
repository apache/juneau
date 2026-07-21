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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-dfn-element">&lt;dfn&gt;</a>
 * element.
 *
 * <p>
 * The dfn element represents the defining instance of a term. It is used to mark up the first occurrence
 * of a term that is being defined in the document, making it clear to both users and search engines
 * that this is the definition of the term. The dfn element can contain the term being defined, and
 * the definition is typically provided in the surrounding context or in a related dd element.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple definition</jc>
 * 	Dfn <jv>simple</jv> = <jsm>dfn</jsm>(<js>"HTML"</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dfn <jv>styled</jv> = <jsm>dfn</jsm>(<js>"CSS"</js>)
 * 		.class_(<js>"term"</js>);
 *
 * 	<jc>// Definition in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>dfn</jsm>(<js>"DOM"</js>),
 * 		<js>" represents the structure of a web page."</js>
 * 	);
 *
 * 	<jc>// Definition with complex content</jc>
 * 	Dfn <jv>complex</jv> = <jsm>dfn</jsm>(
 * 		<js>"JavaScript"</js>,
 * 		<jsm>span</jsm>().class_(<js>"abbrev"</js>).children(<js>" (JS)"</js>)
 * 	);
 *
 * 	<jc>// Definition with title</jc>
 * 	Dfn <jv>withTitle</jv> = <jsm>dfn</jsm>(<js>"HTML"</js>)
 * 		.title(<js>"HyperText Markup Language"</js>);
 *
 * 	<jc>// Definition with ID</jc>
 * 	Dfn <jv>withId</jv> = <jsm>dfn</jsm>(<js>"CSS"</js>)
 * 		.id(<js>"css-term"</js>);
 *
 * 	<jc>// Definition with styling</jc>
 * 	Dfn <jv>styled2</jv> = <jsm>dfn</jsm>(<js>"API"</js>)
 * 		.style(<js>"font-style: italic; color: blue;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#dfn() dfn()}
 * 		<li class='jm'>{@link HtmlBuilder#dfn(Object...) dfn(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "dfn")
public class Dfn extends HtmlElementMixed<Dfn> {

	/**
	 * Creates an empty {@link Dfn} element.
	 */
	public Dfn() {}

	/**
	 * Creates a {@link Dfn} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Dfn(Object...children) {
		children(children);
	}

}