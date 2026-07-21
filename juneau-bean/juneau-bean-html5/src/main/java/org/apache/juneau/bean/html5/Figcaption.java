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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-figcaption-element">&lt;figcaption&gt;</a>
 * element.
 *
 * <p>
 * The figcaption element represents a caption or legend for a figure. It is used to provide a
 * caption for the content of its parent figure element, such as images, diagrams, code snippets,
 * or other self-contained content. The figcaption element can be placed either before or after
 * the figure content and is typically used to provide context, attribution, or explanation for
 * the figure.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple figure caption</jc>
 * 	Figcaption <jv>simple</jv> = <jsm>figcaption</jsm>(<js>"A beautiful sunset over the mountains."</js>);
 *
 * 	<jc>// Figure caption with styling</jc>
 * 	Figcaption <jv>styled</jv> = <jsm>figcaption</jsm>(<js>"Chart showing sales data for Q1 2024."</js>)
 * 		.class_(<js>"image-caption"</js>);
 *
 * 	<jc>// Figure caption with complex content</jc>
 * 	Figcaption <jv>complex</jv> = <jsm>figcaption</jsm>(
 * 		<js>"Figure 1: "</js>,
 * 		<jsm>strong</jsm>(<js>"Web Development Process"</js>),
 * 		<js>" - A step-by-step guide to building websites."</js>
 * 	);
 *
 * 	<jc>// Figure caption with links</jc>
 * 	Figcaption <jv>withLinks</jv> = <jsm>figcaption</jsm>(
 * 		<js>"Source: "</js>,
 * 		<jsm>a</jsm>(<js>"/data"</js>, <js>"Company Data"</js>),
 * 		<js>" | "</js>,
 * 		<jsm>a</jsm>(<js>"/methodology"</js>, <js>"Methodology"</js>)
 * 	);
 *
 * 	<jc>// Figure caption with ID</jc>
 * 	Figcaption <jv>withId</jv> = <jsm>figcaption</jsm>(<js>"Interactive chart showing user engagement metrics."</js>)
 * 		.id(<js>"chart-caption"</js>);
 *
 * 	<jc>// Figure caption with styling</jc>
 * 	Figcaption <jv>styled2</jv> = <jsm>figcaption</jsm>(<js>"Photograph by John Doe, 2024"</js>)
 * 		.style(<js>"text-align: center; font-style: italic; color: #666;"</js>);
 *
 * 	<jc>// Figure caption with multiple elements</jc>
 * 	Figcaption <jv>multiple</jv> = <jsm>figcaption</jsm>(
 * 		<js>"Code Example: "</js>,
 * 		<jsm>code</jsm>(<js>"function hello() { return 'Hello World'; }"</js>),
 * 		<js>" - A simple JavaScript function."</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#figcaption() figcaption()}
 * 		<li class='jm'>{@link HtmlBuilder#figcaption(Object...) figcaption(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "figcaption")
public class Figcaption extends HtmlElementMixed<Figcaption> {

	/**
	 * Creates an empty {@link Figcaption} element.
	 */
	public Figcaption() {}

	/**
	 * Creates a {@link Figcaption} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Figcaption(Object...children) {
		children(children);
	}

}