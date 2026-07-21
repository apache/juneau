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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h3&gt;</a>
 * element.
 *
 * <p>
 * The h3 element represents a third-level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h2 elements. The h3 element is typically
 * used to organize content into smaller subsections and is important for creating a logical
 * document structure. It is typically rendered in a smaller font size than h2 but larger
 * than h4 elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple subsection heading</jc>
 * 	H3 <jv>simple</jv> = <jsm>h3</jsm>(<js>"Basic Configuration"</js>);
 *
 * 	<jc>// H3 with styling</jc>
 * 	H3 <jv>styled</jv> = <jsm>h3</jsm>(<js>"Advanced Settings"</js>)
 * 		.class_(<js>"subsection-title"</js>);
 *
 * 	<jc>// H3 with complex content</jc>
 * 	H3 <jv>complex</jv> = <jsm>h3</jsm>(
 * 		<js>"Step 3: "</js>,
 * 		<jsm>strong</jsm>(<js>"Database Setup"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Optional)"</js>)
 * 	);
 *
 * 	<jc>// H3 with ID</jc>
 * 	H3 <jv>withId</jv> = <jsm>h3</jsm>(<js>"Troubleshooting"</js>)
 * 		.id(<js>"troubleshooting"</js>);
 *
 * 	<jc>// H3 with styling</jc>
 * 	H3 <jv>styled2</jv> = <jsm>h3</jsm>(<js>"Common Issues"</js>)
 * 		.style(<js>"color: #888; margin-top: 20px;"</js>);
 *
 * 	<jc>// H3 with multiple elements</jc>
 * 	H3 <jv>multiple</jv> = <jsm>h3</jsm>(
 * 		<js>"3.1 "</js>,
 * 		<jsm>span</jsm>().class_(<js>"step-title"</js>).children(<js>"Installation"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(5 minutes)"</js>)
 * 	);
 *
 * 	<jc>// H3 with links</jc>
 * 	H3 <jv>withLinks</jv> = <jsm>h3</jsm>(
 * 		<js>"See also: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/faq"</js>).children(<js>"Frequently Asked Questions"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h3() h3()}
 * 		<li class='jm'>{@link HtmlBuilder#h3(Object...) h3(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h3")
public class H3 extends HtmlElementMixed<H3> {

	/**
	 * Creates an empty {@link H3} element.
	 */
	public H3() {}

	/**
	 * Creates an {@link H3} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H3(Object...children) {
		children(children);
	}

}