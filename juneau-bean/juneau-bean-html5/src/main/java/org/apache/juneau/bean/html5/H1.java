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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h1&gt;</a>
 * element.
 *
 * <p>
 * The h1 element represents the highest level heading in a document or section. It is used to
 * mark up the main title or most important heading on a page. The h1 element should be used
 * only once per page and should describe the main topic or purpose of the page. It is typically
 * rendered in the largest font size among all heading elements and is important for both
 * accessibility and SEO.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple page title</jc>
 * 	H1 <jv>simple</jv> = <jsm>h1</jsm>(<js>"Welcome to Our Website"</js>);
 *
 * 	<jc>// H1 with styling</jc>
 * 	H1 <jv>styled</jv> = <jsm>h1</jsm>(<js>"About Our Company"</js>)
 * 		.class_(<js>"page-title"</js>);
 *
 * 	<jc>// H1 with complex content</jc>
 * 	H1 <jv>complex</jv> = <jsm>h1</jsm>(
 * 		<js>"Welcome to "</js>,
 * 		<jsm>strong</jsm>(<js>"Our Company"</js>),
 * 		<js>" - "</js>,
 * 		<jsm>em</jsm>(<js>"Innovation at its finest"</js>)
 * 	);
 *
 * 	<jc>// H1 with ID</jc>
 * 	H1 <jv>withId</jv> = <jsm>h1</jsm>(<js>"Product Documentation"</js>)
 * 		.id(<js>"main-title"</js>);
 *
 * 	<jc>// H1 with styling</jc>
 * 	H1 <jv>styled2</jv> = <jsm>h1</jsm>(<js>"User Guide"</js>)
 * 		.style(<js>"color: #333; text-align: center; margin-bottom: 30px;"</js>);
 *
 * 	<jc>// H1 with multiple elements</jc>
 * 	H1 <jv>multiple</jv> = <jsm>h1</jsm>(
 * 		<js>"Chapter 1: "</js>,
 * 		<jsm>span</jsm>().class_(<js>"chapter-title"</js>).children(<js>"Getting Started"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Beginner Level)"</js>)
 * 	);
 *
 * 	<jc>// H1 with links</jc>
 * 	H1 <jv>withLinks</jv> = <jsm>h1</jsm>(
 * 		<js>"Product: "</js>,
 * 		<jsm>a</jsm>(<js>"/products/widget"</js>, <js>"Amazing Widget"</js>),
 * 		<js>" v2.0"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h1() h1()}
 * 		<li class='jm'>{@link HtmlBuilder#h1(Object...) h1(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h1")
public class H1 extends HtmlElementMixed<H1> {

	/**
	 * Creates an empty {@link H1} element.
	 */
	public H1() {}

	/**
	 * Creates an {@link H1} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H1(Object...children) {
		children(children);
	}

}