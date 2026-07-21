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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-section-element">&lt;section&gt;</a>
 * element.
 *
 * <p>
 * The section element represents a generic section of a document or application. It is used
 * to group related content together and create a logical structure within a document. The
 * section element should have a heading (h1-h6) to identify the section's topic and is
 * typically used to divide content into thematic groups. It is important for creating
 * accessible document structure and helps screen readers understand the organization of content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple section</jc>
 * 	Section <jv>simple</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Introduction"</js>),
 * 		<jsm>p</jsm>(<js>"This is the introduction section."</js>)
 * 	);
 *
 * 	<jc>// Section with styling</jc>
 * 	Section <jv>styled</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Features"</js>),
 * 		<jsm>p</jsm>(<js>"Here are the key features of our product."</js>)
 * 	).class_(<js>"content-section"</js>);
 *
 * 	<jc>// Section with complex content</jc>
 * 	Section <jv>complex</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Getting Started"</js>),
 * 		<jsm>p</jsm>(<js>"Follow these steps to get started:"</js>),
 * 		<jsm>ol</jsm>(
 * 			<jsm>li</jsm>(<js>"Step 1: Install the software"</js>),
 * 			<jsm>li</jsm>(<js>"Step 2: Configure settings"</js>),
 * 			<jsm>li</jsm>(<js>"Step 3: Start using the application"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Section with ID</jc>
 * 	Section <jv>withId</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Main Content"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main content section."</js>)
 * 	).id(<js>"main-content"</js>);
 *
 * 	<jc>// Section with styling</jc>
 * 	Section <jv>styled2</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Styled Section"</js>),
 * 		<jsm>p</jsm>(<js>"This section has custom styling."</js>)
 * 	).style(<js>"background-color: #f9f9f9; padding: 20px; margin: 10px 0;"</js>);
 *
 * 	<jc>// Section with multiple elements</jc>
 * 	Section <jv>multiple</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Documentation"</js>),
 * 		<jsm>p</jsm>(<js>"This section contains documentation."</js>),
 * 		<jsm>ul</jsm>(
 * 			<jsm>li</jsm>(<js>"API Reference"</js>),
 * 			<jsm>li</jsm>(<js>"User Guide"</js>),
 * 			<jsm>li</jsm>(<js>"Examples"</js>)
 * 		),
 * 		<jsm>footer</jsm>(<js>"Last updated: January 2024"</js>)
 * 	);
 *
 * 	<jc>// Section with article</jc>
 * 	Section <jv>withArticle</jv> = <jsm>section</jsm>(
 * 		<jsm>h2</jsm>(<js>"Latest News"</js>),
 * 		<jsm>article</jsm>(
 * 			<jsm>h3</jsm>(<js>"News Article Title"</js>),
 * 			<jsm>p</jsm>(<js>"Article content goes here."</js>)
 * 		)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "section")
public class Section extends HtmlElementMixed<Section> {

	/**
	 * Creates an empty {@link Section} element.
	 */
	public Section() {}

	/**
	 * Creates a {@link Section} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Section(Object...children) {
		children(children);
	}
}