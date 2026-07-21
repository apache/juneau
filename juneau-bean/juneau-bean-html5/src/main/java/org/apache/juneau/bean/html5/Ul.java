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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-ul-element">&lt;ul&gt;</a>
 * element.
 *
 * <p>
 * The ul element represents an unordered list of items. It is used to group a collection of items that
 * do not have a numerical ordering and whose order in the list is not meaningful. The ul element contains
 * li (list item) elements, and is commonly used for navigation menus, feature lists, and other collections
 * where the order of items is not important.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple unordered list</jc>
 * 	Ul <jv>simple</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(<js>"First item"</js>),
 * 		<jsm>li</jsm>(<js>"Second item"</js>),
 * 		<jsm>li</jsm>(<js>"Third item"</js>)
 * 	);
 *
 * 	<jc>// Navigation menu</jc>
 * 	Ul <jv>navigation</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(<jsm>a</jsm>(<js>"/home"</js>, <js>"Home"</js>)),
 * 		<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 		<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>))
 * 	).class_(<js>"nav-menu"</js>);
 *
 * 	<jc>// Feature list</jc>
 * 	Ul <jv>features</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(<js>"Fast performance"</js>),
 * 		<jsm>li</jsm>(<js>"Easy to use"</js>),
 * 		<jsm>li</jsm>(<js>"24/7 support"</js>)
 * 	).class_(<js>"feature-list"</js>);
 *
 * 	<jc>// List with styling</jc>
 * 	Ul <jv>styled</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(<js>"Styled item 1"</js>),
 * 		<jsm>li</jsm>(<js>"Styled item 2"</js>)
 * 	).class_(<js>"custom-list"</js>).style(<js>"list-style-type: square;"</js>);
 *
 * 	<jc>// Nested list</jc>
 * 	Ul <jv>nested</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(<js>"Main item 1"</js>),
 * 		<jsm>li</jsm>(
 * 			<js>"Main item 2"</js>,
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<js>"Sub item 1"</js>),
 * 				<jsm>li</jsm>(<js>"Sub item 2"</js>)
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// List with complex content</jc>
 * 	Ul <jv>complex</jv> = <jsm>ul</jsm>(
 * 		<jsm>li</jsm>(
 * 			<jsm>strong</jsm>(<js>"Important"</js>),
 * 			<js>" item with "</js>,
 * 			<jsm>em</jsm>(<js>"emphasis"</js>)
 * 		),
 * 		<jsm>li</jsm>(<js>"Simple item"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#ul() ul()}
 * 		<li class='jm'>{@link HtmlBuilder#ul(Object...) ul(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "ul")
public class Ul extends HtmlElementContainer<Ul> {

	/**
	 * Creates an empty {@link Ul} element.
	 */
	public Ul() {}

	/**
	 * Creates a {@link Ul} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Ul(Object...children) {
		children(children);
	}

}