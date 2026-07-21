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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/document-metadata.html#the-head-element">&lt;head&gt;</a>
 * element.
 *
 * <p>
 * The head element represents a collection of metadata for the document. It is used to contain
 * information about the document that is not displayed as part of the document's content, such
 * as the title, links to stylesheets, scripts, and other metadata. The head element is typically
 * placed immediately after the opening html tag and before the body element. It can contain
 * elements like title, meta, link, style, script, and base.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple head with title</jc>
 * 	Head <jv>simple</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"My Website"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	);
 *
 * 	<jc>// Head with styling</jc>
 * 	Head <jv>styled</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Styled Page"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/style.css"</js>)
 * 	);
 *
 * 	<jc>// Head with complex content</jc>
 * 	Head <jv>complex</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Complete Page"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>meta</jsm>().name(<js>"viewport"</js>).content(<js>"width=device-width, initial-scale=1.0"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/main.css"</js>),
 * 		<jsm>link</jsm>().rel(<js>"icon"</js>).href(<js>"/favicon.ico"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/main.js"</js>)
 * 	);
 *
 * 	<jc>// Head with ID</jc>
 * 	Head <jv>withId</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Page with ID"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	).id(<js>"page-head"</js>);
 *
 * 	<jc>// Head with styling</jc>
 * 	Head <jv>styled2</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Styled Head"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>)
 * 	).style(<js>"background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Head with multiple elements</jc>
 * 	Head <jv>multiple</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Multi-Element Head"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>meta</jsm>().name(<js>"description"</js>).content(<js>"A comprehensive page"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/reset.css"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"/css/layout.css"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/jquery.js"</js>),
 * 		<jsm>script</jsm>().src(<js>"/js/app.js"</js>)
 * 	);
 *
 * 	<jc>// Head with base element</jc>
 * 	Head <jv>withBase</jv> = <jsm>head</jsm>(
 * 		<jsm>title</jsm>(<js>"Page with Base"</js>),
 * 		<jsm>meta</jsm>().charset(<js>"UTF-8"</js>),
 * 		<jsm>base</jsm>().href(<js>"https://example.com/"</js>),
 * 		<jsm>link</jsm>().rel(<js>"stylesheet"</js>).href(<js>"css/style.css"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#head() head()}
 * 		<li class='jm'>{@link HtmlBuilder#head(Object...) head(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "head")
public class Head extends HtmlElementContainer<Head> {

	/**
	 * Creates an empty {@link Head} element.
	 */
	public Head() {}

	/**
	 * Creates a {@link Head} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Head(Object...children) {
		children(children);
	}

}