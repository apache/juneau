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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h6&gt;</a>
 * element.
 *
 * <p>
 * The h6 element represents the lowest level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h5 elements. The h6 element is typically
 * used to organize content into the smallest subsections and is important for creating a logical
 * document structure. It is typically rendered in the smallest font size among all heading
 * elements and is often used for fine-grained content organization.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple lowest-level heading</jc>
 * 	H6 <jv>simple</jv> = <jsm>h6</jsm>(<js>"Connection Timeout"</js>);
 *
 * 	<jc>// H6 with styling</jc>
 * 	H6 <jv>styled</jv> = <jsm>h6</jsm>(<js>"Retry Attempts"</js>)
 * 		.class_(<js>"micro-heading"</js>);
 *
 * 	<jc>// H6 with complex content</jc>
 * 	H6 <jv>complex</jv> = <jsm>h6</jsm>(
 * 		<js>"6.1 "</js>,
 * 		<jsm>strong</jsm>(<js>"Connection Pool Size"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Default: 5)"</js>)
 * 	);
 *
 * 	<jc>// H6 with ID</jc>
 * 	H6 <jv>withId</jv> = <jsm>h6</jsm>(<js>"Keep-Alive Settings"</js>)
 * 		.id(<js>"keep-alive"</js>);
 *
 * 	<jc>// H6 with styling</jc>
 * 	H6 <jv>styled2</jv> = <jsm>h6</jsm>(<js>"Advanced Options"</js>)
 * 		.style(<js>"color: #bbb; font-size: 0.8em; font-weight: normal;"</js>);
 *
 * 	<jc>// H6 with multiple elements</jc>
 * 	H6 <jv>multiple</jv> = <jsm>h6</jsm>(
 * 		<js>"6.1.1 "</js>,
 * 		<jsm>span</jsm>().class_(<js>"micro-title"</js>).children(<js>"Max Retries"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Range: 1-10)"</js>)
 * 	);
 *
 * 	<jc>// H6 with links</jc>
 * 	H6 <jv>withLinks</jv> = <jsm>h6</jsm>(
 * 		<js>"Ref: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/connection"</js>).children(<js>"Connection Guide"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h6() h6()}
 * 		<li class='jm'>{@link HtmlBuilder#h6(Object...) h6(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h6")
public class H6 extends HtmlElementMixed<H6> {

	/**
	 * Creates an empty {@link H6} element.
	 */
	public H6() {}

	/**
	 * Creates an {@link H6} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H6(Object...children) {
		children(children);
	}

}