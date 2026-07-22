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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-title-element">&lt;title&gt;</a>
 * element.
 *
 * <p>
 * The title element represents the document's title or name. It is used to provide a title
 * for the document that appears in the browser's title bar, bookmarks, and search engine
 * results. The title element should be placed within the head element and should be descriptive
 * and unique for each page. It is important for SEO, accessibility, and user experience as
 * it helps users identify the page content and purpose.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple page title</jc>
 * 	Title <jv>simple</jv> = <jsm>title</jsm>(<js>"Welcome to My Website"</js>);
 *
 * 	<jc>// Title with dynamic content</jc>
 * 	Title <jv>dynamic</jv> = <jsm>title</jsm>(<js>"User Dashboard - "</js> + <jv>username</jv>);
 *
 * 	<jc>// Title with branding</jc>
 * 	Title <jv>branded</jv> = <jsm>title</jsm>(<js>"About Us | My Company"</js>);
 *
 * 	<jc>// Title with SEO keywords</jc>
 * 	Title <jv>seo</jv> = <jsm>title</jsm>(<js>"Best Coffee Shops in Seattle - 2024 Guide"</js>);
 *
 * 	<jc>// Title with separator</jc>
 * 	Title <jv>separated</jv> = <jsm>title</jsm>(<js>"Contact Us - Get in Touch"</js>);
 *
 * 	<jc>// Title with emoji</jc>
 * 	Title <jv>emoji</jv> = <jsm>title</jsm>(<js>"🎉 Welcome to Our Store!"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "title")
public class Title extends HtmlElementRawText<Title> {

	/**
	 * Creates an empty {@link Title} element.
	 */
	public Title() {}

	/**
	 * Creates a {@link Title} element with the specified {@link Title#text(Object)} node.
	 *
	 * @param text The {@link Title#text(Object)} node. Can be <jk>null</jk> to leave the text unset.
	 */
	public Title(String text) {
		text(text);
	}
}
