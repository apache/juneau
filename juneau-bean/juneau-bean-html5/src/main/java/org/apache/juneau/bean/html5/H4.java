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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h4&gt;</a>
 * element.
 *
 * <p>
 * The h4 element represents a fourth-level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h3 elements. The h4 element is typically
 * used to organize content into smaller subsections and is important for creating a logical
 * document structure. It is typically rendered in a smaller font size than h3 but larger
 * than h5 elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple sub-subsection heading</jc>
 * 	H4 <jv>simple</jv> = <jsm>h4</jsm>(<js>"Configuration Options"</js>);
 *
 * 	<jc>// H4 with styling</jc>
 * 	H4 <jv>styled</jv> = <jsm>h4</jsm>(<js>"Environment Variables"</js>)
 * 		.class_(<js>"sub-subsection-title"</js>);
 *
 * 	<jc>// H4 with complex content</jc>
 * 	H4 <jv>complex</jv> = <jsm>h4</jsm>(
 * 		<js>"4.1 "</js>,
 * 		<jsm>strong</jsm>(<js>"Database Configuration"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Required)"</js>)
 * 	);
 *
 * 	<jc>// H4 with ID</jc>
 * 	H4 <jv>withId</jv> = <jsm>h4</jsm>(<js>"Performance Tuning"</js>)
 * 		.id(<js>"performance-tuning"</js>);
 *
 * 	<jc>// H4 with styling</jc>
 * 	H4 <jv>styled2</jv> = <jsm>h4</jsm>(<js>"Additional Notes"</js>)
 * 		.style(<js>"color: #999; font-weight: normal;"</js>);
 *
 * 	<jc>// H4 with multiple elements</jc>
 * 	H4 <jv>multiple</jv> = <jsm>h4</jsm>(
 * 		<js>"4.1.1 "</js>,
 * 		<jsm>span</jsm>().class_(<js>"detail-title"</js>).children(<js>"Connection Pool"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Advanced)"</js>)
 * 	);
 *
 * 	<jc>// H4 with links</jc>
 * 	H4 <jv>withLinks</jv> = <jsm>h4</jsm>(
 * 		<js>"Related: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/performance"</js>).children(<js>"Performance Guide"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h4() h4()}
 * 		<li class='jm'>{@link HtmlBuilder#h4(Object...) h4(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h4")
public class H4 extends HtmlElementMixed<H4> {

	/**
	 * Creates an empty {@link H4} element.
	 */
	public H4() {}

	/**
	 * Creates an {@link H4} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H4(Object...children) {
		children(children);
	}

}