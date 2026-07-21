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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-li-element">&lt;li&gt;</a>
 * element.
 *
 * <p>
 * The li element represents a list item. It is used to mark up individual items within a list,
 * such as items in an unordered list (ul) or ordered list (ol). The li element can contain
 * any flow content and is typically rendered with a bullet point (for ul) or a number (for ol)
 * depending on the parent list type. The li element is essential for creating structured lists
 * in HTML documents.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple list item</jc>
 * 	Li <jv>simple</jv> = <jsm>li</jsm>(<js>"First item"</js>);
 *
 * 	<jc>// Li with styling</jc>
 * 	Li <jv>styled</jv> = <jsm>li</jsm>(<js>"Styled list item"</js>)
 * 		.class_(<js>"list-item"</js>);
 *
 * 	<jc>// Li with complex content</jc>
 * 	Li <jv>complex</jv> = <jsm>li</jsm>(
 * 		<js>"Item with "</js>,
 * 		<jsm>strong</jsm>(<js>"bold text"</js>),
 * 		<js>" and "</js>,
 * 		<jsm>em</jsm>(<js>"italic text"</js>)
 * 	);
 *
 * 	<jc>// Li with ID</jc>
 * 	Li <jv>withId</jv> = <jsm>li</jsm>(<js>"List item with ID"</js>)
 * 		.id(<js>"list-item-1"</js>);
 *
 * 	<jc>// Li with styling</jc>
 * 	Li <jv>styled2</jv> = <jsm>li</jsm>(<js>"Custom styled list item"</js>)
 * 		.style(<js>"color: #666; margin: 5px 0;"</js>);
 *
 * 	<jc>// Li with multiple elements</jc>
 * 	Li <jv>multiple</jv> = <jsm>li</jsm>(
 * 		<js>"Step 1: "</js>,
 * 		<jsm>span</jsm>(<js>"Complete the form"</js>).class_(<js>"step-title"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Required)"</js>)
 * 	);
 *
 * 	<jc>// Li with links</jc>
 * 	Li <jv>withLinks</jv> = <jsm>li</jsm>(
 * 		<js>"Visit "</js>,
 * 		<jsm>a</jsm>(<js>"/help"</js>, <js>"help page"</js>),
 * 		<js>" for assistance"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#li() li()}
 * 		<li class='jm'>{@link HtmlBuilder#li(Object...) li(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "li")
public class Li extends HtmlElementMixed<Li> {

	/**
	 * Creates an empty {@link Li} element.
	 */
	public Li() {}

	/**
	 * Creates an {@link Li} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Li(Object...children) {
		children(children);
	}

}