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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-nav-element">&lt;nav&gt;</a>
 * element.
 *
 * <p>
 * The nav element represents a section of a page that links to other pages or to parts within
 * the same page. It is used to contain navigation links and is typically used for site navigation,
 * table of contents, or pagination. The nav element should contain a list of links and is important
 * for accessibility as it helps screen readers identify the main navigation areas of a page.
 * It is commonly used with ul and li elements to create structured navigation menus.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple navigation</jc>
 * 	Nav <jv>simple</jv> = <jsm>nav</jsm>(
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>))
 * 		)
 * 	);
 *
 * 	<jc>// Nav with styling</jc>
 * 	Nav <jv>styled</jv> = <jsm>nav</jsm>(
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/products"</js>, <js>"Products"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/services"</js>, <js>"Services"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/support"</js>, <js>"Support"</js>))
 * 		)
 * 	).class_(<js>"main-navigation"</js>);
 *
 * 	<jc>// Nav with complex content</jc>
 * 	Nav <jv>complex</jv> = <jsm>nav</jsm>(
 * 		<jsm>h3</jsm>(<js>"Site Navigation"</js>),
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>))
 * 		)
 * 	);
 *
 * 	<jc>// Nav with ID</jc>
 * 	Nav <jv>withId</jv> = <jsm>nav</jsm>(
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>))
 * 		)
 * 	).id(<js>"main-nav"</js>);
 *
 * 	<jc>// Nav with styling</jc>
 * 	Nav <jv>styled2</jv> = <jsm>nav</jsm>(
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>))
 * 		)
 * 	).style(<js>"background-color: #333; padding: 10px;"</js>);
 *
 * 	<jc>// Nav with multiple elements</jc>
 * 	Nav <jv>multiple</jv> = <jsm>nav</jsm>(
 * 		<jsm>h3</jsm>(<js>"Navigation"</js>),
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>))
 * 		),
 * 		<jsm>p</jsm>(<js>"Use the links above to navigate the site."</js>)
 * 	);
 *
 * 	<jc>// Nav with breadcrumbs</jc>
 * 	Nav <jv>breadcrumbs</jv> = <jsm>nav</jsm>(
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/"</js>, <js>"Home"</js>)),
 * 			<jsm>li</jsm>(<js>" > "</js>),
 * 			<jsm>li</jsm>(<jsm>a</jsm>(<js>"/products"</js>, <js>"Products"</js>)),
 * 			<jsm>li</jsm>(<js>" > "</js>),
 * 			<jsm>li</jsm>(<js>"Current Page"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#nav() nav()}
 * 		<li class='jm'>{@link HtmlBuilder#nav(Object...) nav(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "nav")
public class Nav extends HtmlElementMixed<Nav> {

	/**
	 * Creates an empty {@link Nav} element.
	 */
	public Nav() {}

	/**
	 * Creates a {@link Nav} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Nav(Object...children) {
		children(children);
	}

}