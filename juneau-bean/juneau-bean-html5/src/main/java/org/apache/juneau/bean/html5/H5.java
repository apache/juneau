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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-h1,-h2,-h3,-h4,-h5,-and-h6-elements">&lt;h5&gt;</a>
 * element.
 *
 * <p>
 * The h5 element represents a fifth-level heading in a document or section. It is used to
 * mark up subsections that are hierarchically below h4 elements. The h5 element is typically
 * used to organize content into smaller subsections and is important for creating a logical
 * document structure. It is typically rendered in a smaller font size than h4 but larger
 * than h6 elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple sub-sub-subsection heading</jc>
 * 	H5 <jv>simple</jv> = <jsm>h5</jsm>(<js>"Connection Settings"</js>);
 *
 * 	<jc>// H5 with styling</jc>
 * 	H5 <jv>styled</jv> = <jsm>h5</jsm>(<js>"Timeout Configuration"</js>)
 * 		.class_(<js>"detail-heading"</js>);
 *
 * 	<jc>// H5 with complex content</jc>
 * 	H5 <jv>complex</jv> = <jsm>h5</jsm>(
 * 		<js>"5.1 "</js>,
 * 		<jsm>strong</jsm>(<js>"Connection Pool Settings"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Optional)"</js>)
 * 	);
 *
 * 	<jc>// H5 with ID</jc>
 * 	H5 <jv>withId</jv> = <jsm>h5</jsm>(<js>"SSL Configuration"</js>)
 * 		.id(<js>"ssl-config"</js>);
 *
 * 	<jc>// H5 with styling</jc>
 * 	H5 <jv>styled2</jv> = <jsm>h5</jsm>(<js>"Additional Parameters"</js>)
 * 		.style(<js>"color: #aaa; font-size: 0.9em;"</js>);
 *
 * 	<jc>// H5 with multiple elements</jc>
 * 	H5 <jv>multiple</jv> = <jsm>h5</jsm>(
 * 		<js>"5.1.1 "</js>,
 * 		<jsm>span</jsm>().class_(<js>"param-title"</js>).children(<js>"Max Connections"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(Default: 10)"</js>)
 * 	);
 *
 * 	<jc>// H5 with links</jc>
 * 	H5 <jv>withLinks</jv> = <jsm>h5</jsm>(
 * 		<js>"See: "</js>,
 * 		<jsm>a</jsm>(<js>"/docs/ssl"</js>).children(<js>"SSL Documentation"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#h5() h5()}
 * 		<li class='jm'>{@link HtmlBuilder#h5(Object...) h5(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "h5")
public class H5 extends HtmlElementMixed<H5> {

	/**
	 * Creates an empty {@link H5} element.
	 */
	public H5() {}

	/**
	 * Creates an {@link H5} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public H5(Object...children) {
		children(children);
	}

}