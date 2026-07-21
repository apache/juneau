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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-header-element">&lt;header&gt;</a>
 * element.
 *
 * <p>
 * The header element represents introductory content for its nearest ancestor sectioning content
 * or sectioning root element. It is used to contain introductory content such as headings,
 * navigation, logos, or other introductory elements. The header element can contain any flow
 * content and is typically used at the top of a page, article, or section to provide context
 * and navigation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple page header</jc>
 * 	Header <jv>simple</jv> = <jsm>header</jsm>(
 * 		<jsm>h1</jsm>(<js>"My Website"</js>),
 * 		<jsm>p</jsm>(<js>"Welcome to our site"</js>)
 * 	);
 *
 * 	<jc>// Header with navigation</jc>
 * 	Header <jv>withNav</jv> = <jsm>header</jsm>(
 * 		<jsm>h1</jsm>(<js>"Company Name"</js>),
 * 		<jsm>nav</jsm>(
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>).children(<js>"Home"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>).children(<js>"About"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>).children(<js>"Contact"</js>))
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Header with styling</jc>
 * 	Header <jv>styled</jv> = <jsm>header</jsm>(
 * 		<jsm>h1</jsm>(<js>"Styled Header"</js>),
 * 		<jsm>p</jsm>(<js>"A beautifully styled header"</js>)
 * 	).class_(<js>"page-header"</js>);
 *
 * 	<jc>// Header with logo and navigation</jc>
 * 	Header <jv>withLogo</jv> = <jsm>header</jsm>(
 * 		<jsm>img</jsm>(<js>"/logo.png"</js>, <js>"Company Logo"</js>),
 * 		<jsm>h1</jsm>(<js>"Company Name"</js>),
 * 		<jsm>nav</jsm>(
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/products"</js>).children(<js>"Products"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/services"</js>).children(<js>"Services"</js>))
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Header with ID</jc>
 * 	Header <jv>withId</jv> = <jsm>header</jsm>(
 * 		<jsm>h1</jsm>(<js>"Main Header"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main header of the page"</js>)
 * 	).id(<js>"main-header"</js>);
 *
 * 	<jc>// Header with styling</jc>
 * 	Header <jv>styled2</jv> = <jsm>header</jsm>(
 * 		<jsm>h1</jsm>(<js>"Dark Header"</js>),
 * 		<jsm>p</jsm>(<js>"A header with dark styling"</js>)
 * 	).style(<js>"background-color: #333; color: white; padding: 20px;"</js>);
 *
 * 	<jc>// Header with multiple sections</jc>
 * 	Header <jv>multiSection</jv> = <jsm>header</jsm>(
 * 		<jsm>div</jsm>().class_(<js>"header-top"</js>).children(
 * 			<jsm>p</jsm>(<js>"Call us: (555) 123-4567"</js>),
 * 			<jsm>p</jsm>(<js>"Email: info@company.com"</js>)
 * 		),
 * 		<jsm>div</jsm>().class_(<js>"header-main"</js>).children(
 * 			<jsm>h1</jsm>(<js>"Company Name"</js>),
 * 			<jsm>p</jsm>(<js>"Your trusted partner"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#header() header()}
 * 		<li class='jm'>{@link HtmlBuilder#header(Object...) header(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "header")
public class Header extends HtmlElementMixed<Header> {

	/**
	 * Creates an empty {@link Header} element.
	 */
	public Header() {}

	/**
	 * Creates a {@link Header} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Header(Object...children) {
		children(children);
	}

}