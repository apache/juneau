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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h2&gt;</a>
 * element.
 *
 * <p>
 * The h2 element represents a second-level heading in a document or section. It is used to
 * mark up subsections or secondary headings that are hierarchically below h1 elements. The h2
 * element is typically used to organize content into major sections and is important for
 * creating a logical document structure. It is typically rendered in a smaller font size than
 * h1 but larger than h3 elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple section heading</jc>
 * 	H2 <jv>simple</jv> = <jsm>h2</jsm>(<js>"Getting Started"</js>);
 *
 * 	<jc>// H2 with styling</jc>
 * 	H2 <jv>styled</jv> = <jsm>h2</jsm>(<js>"Product Features"</js>)
 * 		.class_(<js>"section-title"</js>);
 *
 * 	<jc>// H2 with complex content</jc>
 * 	H2 <jv>complex</jv> = <jsm>h2</jsm>(
 * 		<js>"Chapter 2: "</js>,
 * 		<jsm>strong</jsm>(<js>"Advanced Topics"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Expert Level)"</js>)
 * 	);
 *
 * 	<jc>// H2 with ID</jc>
 * 	H2 <jv>withId</jv> = <jsm>h2</jsm>(<js>"Installation Guide"</js>)
 * 		.id(<js>"installation"</js>);
 *
 * 	<jc>// H2 with styling</jc>
 * 	H2 <jv>styled2</jv> = <jsm>h2</jsm>(<js>"Configuration Options"</js>)
 * 		.style(<js>"color: #666; border-bottom: 2px solid #ccc; padding-bottom: 10px;"</js>);
 *
 * 	<jc>// H2 with multiple elements</jc>
 * 	H2 <jv>multiple</jv> = <jsm>h2</jsm>(
 * 		<js>"Section 2.1: "</js>,
 * 		<jsm>span</jsm>().class_(<js>"subsection"</js>).children(<js>"Basic Setup"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Required)"</js>)
 * 	);
 *
 * 	<jc>// H2 with links</jc>
 * 	H2 <jv>withLinks</jv> = <jsm>h2</jsm>(
 * 		<js>"API Reference: "</js>,
 * 		<jsm>a</jsm>(<js>"/api/users"</js>).children(<js>"User Management"</js>),
 * 		<js>" Endpoints"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h2() h2()}
 * 		<li class='jm'>{@link HtmlBuilder#h2(Object...) h2(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h2")
public class H2 extends HtmlElementMixed<H2> {

	/**
	 * Creates an empty {@link H2} element.
	 */
	public H2() {}

	/**
	 * Creates an {@link H2} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H2(Object...children) {
		children(children);
	}

}