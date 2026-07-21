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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-div-element">&lt;div&gt;</a>
 * element.
 *
 * <p>
 * The div element represents a generic container for flow content. It is used to group elements
 * together for styling purposes or to create layout structures. The div element has no semantic
 * meaning and is purely presentational, making it a versatile tool for organizing and styling
 * content. It is commonly used with CSS to create layouts, group related elements, or apply
 * styling to multiple elements at once.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple div</jc>
 * 	Div <jv>simple</jv> = <jsm>div</jsm>(<js>"Hello World"</js>);
 *
 * 	<jc>// Div with styling</jc>
 * 	Div <jv>styled</jv> = <jsm>div</jsm>()
 * 		.class_(<js>"container"</js>)
 * 		.style(<js>"padding: 20px; background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Div with multiple children</jc>
 * 	Div <jv>multiple</jv> = <jsm>div</jsm>(
 * 		<jsm>h1</jsm>(<js>"Title"</js>),
 * 		<jsm>p</jsm>(<js>"Content"</js>),
 * 		<jsm>p</jsm>(<js>"More content"</js>)
 * 	);
 *
 * 	<jc>// Div with complex content</jc>
 * 	Div <jv>complex</jv> = <jsm>div</jsm>(
 * 		<jsm>div</jsm>().class_(<js>"card-header"</js>).children(<js>"Card Title"</js>),
 * 		<jsm>div</jsm>().class_(<js>"card-body"</js>).children(<js>"Card content"</js>),
 * 		<jsm>div</jsm>().class_(<js>"card-footer"</js>).children(<js>"Card footer"</js>)
 * 	).class_(<js>"card"</js>);
 *
 * 	<jc>// Div with ID</jc>
 * 	Div <jv>withId</jv> = <jsm>div</jsm>(<js>"Main content area"</js>)
 * 		.id(<js>"main-content"</js>);
 *
 * 	<jc>// Div with styling</jc>
 * 	Div <jv>styled2</jv> = <jsm>div</jsm>(
 * 		<jsm>div</jsm>().class_(<js>"left-column"</js>).children(<js>"Left content"</js>),
 * 		<jsm>div</jsm>().class_(<js>"right-column"</js>).children(<js>"Right content"</js>)
 * 	).style(<js>"display: flex; justify-content: space-between;"</js>);
 *
 * 	<jc>// Div with event handlers</jc>
 * 	Div <jv>interactive</jv> = <jsm>div</jsm>(<js>"Interactive div"</js>)
 * 		.onclick(<js>"handleClick()"</js>)
 * 		.onmouseover(<js>"handleMouseOver()"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#div() div()}
 * 		<li class='jm'>{@link HtmlBuilder#div(Object...) div(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "div")
public class Div extends HtmlElementMixed<Div> {

	/**
	 * Creates an empty {@link Div} element.
	 */
	public Div() {}

	/**
	 * Creates a {@link Div} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Div(Object...children) {
		children(children);
	}

}