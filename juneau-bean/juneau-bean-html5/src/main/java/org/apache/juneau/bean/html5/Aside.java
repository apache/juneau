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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-aside-element">&lt;aside&gt;</a>
 * element.
 *
 * <p>
 * The aside element represents a section of a page that consists of content that is tangentially
 * related to the content around the aside element, and which could be considered separate from
 * that content. It is commonly used for sidebars, pull quotes, advertisements, navigation links,
 * or other content that is related but not essential to the main content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Sidebar with related links</jc>
 * 	Aside <jv>sidebar</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"Related Articles"</js>),
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article1"</js>, <js>"Article 1"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article2"</js>, <js>"Article 2"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/article3"</js>, <js>"Article 3"</js>))
 * 		)
 * 	).class_(<js>"sidebar"</js>);
 *
 * 	<jc>// Pull quote</jc>
 * 	Aside <jv>pullQuote</jv> = <jsm>aside</jsm>(
 * 		<jsm>blockquote</jsm>(
 * 			<js>"The best way to predict the future is to create it."</js>,
 * 			<jsm>footer</jsm>(<js>"— Peter Drucker"</js>)
 * 		)
 * 	).class_(<js>"pull-quote"</js>);
 *
 * 	<jc>// Advertisement</jc>
 * 	Aside <jv>advertisement</jv> = <jsm>aside</jsm>(
 * 		<jsm>h4</jsm>(<js>"Sponsored Content"</js>),
 * 		<jsm>p</jsm>(<js>"Check out our latest product!"</js>),
 * 		<jsm>a</jsm>(<js>"/product"</js>, <js>"Learn More"</js>)
 * 	).class_(<js>"advertisement"</js>);
 *
 * 	<jc>// Author bio</jc>
 * 	Aside <jv>authorBio</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"About the Author"</js>),
 * 		<jsm>p</jsm>(<js>"John Doe is a web developer with 10 years of experience..."</js>),
 * 		<jsm>a</jsm>(<js>"/author/john-doe"</js>, <js>"Read more articles"</js>)
 * 	).class_(<js>"author-bio"</js>);
 *
 * 	<jc>// Navigation menu</jc>
 * 	Aside <jv>navigation</jv> = <jsm>aside</jsm>(
 * 		<jsm>nav</jsm>(
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>))
 * 			)
 * 		)
 * 	).class_(<js>"navigation"</js>);
 *
 * 	<jc>// Glossary or definitions</jc>
 * 	Aside <jv>glossary</jv> = <jsm>aside</jsm>(
 * 		<jsm>h3</jsm>(<js>"Key Terms"</js>),
 * 		<jsm>dl</jsm>(
 * 			<jsm>dt</jsm>(<js>"HTML"</js>),
 * 			<jsm>dd</jsm>(<js>"HyperText Markup Language"</js>),
 * 			<jsm>dt</jsm>(<js>"CSS"</js>),
 * 			<jsm>dd</jsm>(<js>"Cascading Style Sheets"</js>)
 * 		)
 * 	).class_(<js>"glossary"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#aside() aside()}
 * 		<li class='jm'>{@link HtmlBuilder#aside(Object...) aside(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "aside")
public class Aside extends HtmlElementMixed<Aside> {

	/**
	 * Creates an empty {@link Aside} element.
	 */
	public Aside() {}

	/**
	 * Creates an {@link Aside} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Aside(Object...children) {
		children(children);
	}

}