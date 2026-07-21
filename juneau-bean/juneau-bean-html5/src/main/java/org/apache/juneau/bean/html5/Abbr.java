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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-abbr-element">&lt;abbr&gt;</a>
 * element.
 *
 * <p>
 * The abbr element represents an abbreviation or acronym. It is used to mark up abbreviated text
 * and provide an optional expansion or explanation via the title attribute. This helps screen
 * readers and other assistive technologies understand the full meaning of abbreviations, and
 * provides tooltips for users when they hover over the abbreviated text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple abbreviation</jc>
 * 	Abbr <jv>simple</jv> = <jsm>abbr</jsm>(<js>"HyperText Markup Language"</js>, <js>"HTML"</js>);
 *
 * 	<jc>// Acronym with expansion</jc>
 * 	Abbr <jv>acronym</jv> = <jsm>abbr</jsm>(<js>"World Wide Web"</js>, <js>"WWW"</js>);
 *
 * 	<jc>// Technical abbreviation</jc>
 * 	Abbr <jv>technical</jv> = <jsm>abbr</jsm>(<js>"Cascading Style Sheets"</js>, <js>"CSS"</js>);
 *
 * 	<jc>// Date abbreviation</jc>
 * 	Abbr <jv>date</jv> = <jsm>abbr</jsm>(<js>"January"</js>, <js>"Jan"</js>);
 *
 * 	<jc>// Abbreviation with styling</jc>
 * 	Abbr <jv>styled</jv> = <jsm>abbr</jsm>(<js>"JavaScript Object Notation"</js>, <js>"JSON"</js>)
 * 		.class_(<js>"abbreviation"</js>);
 *
 * 	<jc>// Multiple abbreviations in text</jc>
 * 	Abbr <jv>multiple</jv> = <jsm>abbr</jsm>()
 * 		.children(
 * 			<js>"The "</js>,
 * 			<jsm>abbr</jsm>(<js>"World Wide Web Consortium"</js>, <js>"W3C"</js>),
 * 			<js>" defines "</js>,
 * 			<jsm>abbr</jsm>(<js>"HyperText Markup Language"</js>, <js>"HTML"</js>),
 * 			<js>" standards."</js>
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#abbr() abbr()}
 * 		<li class='jm'>{@link HtmlBuilder#abbr(String, Object...) abbr(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "abbr")
public class Abbr extends HtmlElementMixed<Abbr> {

	/**
	 * Creates an empty {@link Abbr} element.
	 */
	public Abbr() {}

	/**
	 * Creates an {@link Abbr} element with the specified {@link Abbr#title(String)} attribute and
	 * {@link Abbr#children(Object[])} nodes.
	 *
	 * @param title The {@link Abbr#title(String)} attribute.
	 * @param children The {@link Abbr#children(Object[])} nodes.
	 */
	public Abbr(String title, Object...children) {
		title(title).children(children);
	}

}