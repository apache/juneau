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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-caption-element">&lt;caption&gt;</a>
 * element.
 *
 * <p>
 * The caption element represents the title of a table. It is used to provide a brief description or
 * title for the table content, making it easier for users to understand what the table contains.
 * The caption element should be placed as the first child of a table element and is typically
 * rendered above the table by default.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table caption</jc>
 * 	Caption <jv>simple</jv> = <jsm>caption</jsm>(<js>"Monthly Sales Report"</js>);
 *
 * 	<jc>// Table caption with styling</jc>
 * 	Caption <jv>styled</jv> = <jsm>caption</jsm>(<js>"Employee Directory"</js>)
 * 		.class_(<js>"table-title"</js>);
 *
 * 	<jc>// Table caption with complex content</jc>
 * 	Caption <jv>complex</jv> = <jsm>caption</jsm>(
 * 		<js>"Sales Data for "</js>,
 * 		<jsm>strong</jsm>(<js>"Q1 2024"</js>),
 * 		<js>" - "</js>,
 * 		<jsm>em</jsm>(<js>"Preliminary Results"</js>)
 * 	);
 *
 * 	<jc>// Table caption with links</jc>
 * 	Caption <jv>withLinks</jv> = <jsm>caption</jsm>(
 * 		<js>"Product Inventory - "</js>,
 * 		<jsm>a</jsm>(<js>"/help/inventory"</js>, <js>"Help"</js>),
 * 		<js>" | "</js>,
 * 		<jsm>a</jsm>(<js>"/export/inventory"</js>, <js>"Export"</js>)
 * 	);
 *
 * 	<jc>// Table caption with icons</jc>
 * 	Caption <jv>withIcons</jv> = <jsm>caption</jsm>(
 * 		<js>"📊 "</js>,
 * 		<js>"Financial Summary"</js>,
 * 		<js>" "</js>,
 * 		<jsm>span</jsm>().class_(<js>"icon"</js>).children(<js>"💰"</js>)
 * 	);
 *
 * 	<jc>// Table caption with multiple lines</jc>
 * 	Caption <jv>multiLine</jv> = <jsm>caption</jsm>(
 * 		<js>"Customer Contact Information"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Last Updated: "</js>,
 * 		<jsm>time</jsm>(<js>"2024-01-15"</js>, <js>"January 15, 2024"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#caption() caption()}
 * 		<li class='jm'>{@link HtmlBuilder#caption(Object...) caption(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "caption")
public class Caption extends HtmlElementMixed<Caption> {

	/**
	 * Creates an empty {@link Caption} element.
	 */
	public Caption() {}

	/**
	 * Creates a {@link Caption} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Caption(Object...children) {
		children(children);
	}

}