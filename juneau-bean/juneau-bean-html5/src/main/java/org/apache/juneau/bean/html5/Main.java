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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-main-element">&lt;main&gt;</a>
 * element.
 *
 * <p>
 * The main element represents the main content of the body of a document or application. It is
 * used to identify the primary content of the page, excluding content that is repeated across
 * multiple pages such as navigation, headers, footers, and sidebars. The main element should
 * be used only once per page and contains the central topic or functionality of the page. It
 * is important for accessibility and helps screen readers identify the main content area.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple main content</jc>
 * 	Main <jv>simple</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Welcome to Our Site"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main content of our page."</js>)
 * 	);
 *
 * 	<jc>// Main with styling</jc>
 * 	Main <jv>styled</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"About Us"</js>),
 * 		<jsm>p</jsm>(<js>"Learn more about our company and mission."</js>)
 * 	).class_(<js>"main-content"</js>);
 *
 * 	<jc>// Main with complex content</jc>
 * 	Main <jv>complex</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Product Catalog"</js>),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Featured Products"</js>),
 * 			<jsm>p</jsm>(<js>"Check out our latest offerings."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Categories"</js>),
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<js>"Electronics"</js>),
 * 				<jsm>li</jsm>(<js>"Clothing"</js>),
 * 				<jsm>li</jsm>(<js>"Books"</js>)
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Main with ID</jc>
 * 	Main <jv>withId</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Main Content"</js>),
 * 		<jsm>p</jsm>(<js>"This is the main content area."</js>)
 * 	).id(<js>"page-main"</js>);
 *
 * 	<jc>// Main with styling</jc>
 * 	Main <jv>styled2</jv> = <jsm>main</jsm>(
 * 		<jsm>h1</jsm>(<js>"Centered Content"</js>),
 * 		<jsm>p</jsm>(<js>"This content is centered and styled."</js>)
 * 	).style(<js>"max-width: 800px; margin: 0 auto; padding: 20px;"</js>);
 *
 * 	<jc>// Main with multiple sections</jc>
 * 	Main <jv>multiSection</jv> = <jsm>main</jsm>(
 * 		<jsm>section</jsm>(
 * 			<jsm>h1</jsm>(<js>"Page Title"</js>),
 * 			<jsm>p</jsm>(<js>"Introduction to the page content."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Content Section"</js>),
 * 			<jsm>p</jsm>(<js>"Detailed content goes here."</js>)
 * 		),
 * 		<jsm>section</jsm>(
 * 			<jsm>h2</jsm>(<js>"Conclusion"</js>),
 * 			<jsm>p</jsm>(<js>"Summary and closing thoughts."</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Main with article</jc>
 * 	Main <jv>withArticle</jv> = <jsm>main</jsm>(
 * 		<jsm>article</jsm>(
 * 			<jsm>h1</jsm>(<js>"Article Title"</js>),
 * 			<jsm>p</jsm>(<js>"Article content goes here."</js>),
 * 			<jsm>footer</jsm>(<js>"Article footer"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "main")
public class Main extends HtmlElementContainer<Main> {

	/**
	 * Creates an empty {@link Main} element.
	 */
	public Main() {}

	/**
	 * Creates a {@link Main} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Main(Object...children) {
		children(children);
	}

}