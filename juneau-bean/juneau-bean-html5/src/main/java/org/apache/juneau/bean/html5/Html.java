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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/semantics.html#the-html-element">&lt;html&gt;</a>
 * element.
 *
 * <p>
 * The html element represents the root of an HTML document. It contains all other HTML elements
 * and serves as the top-level container for the entire document. The lang attribute is commonly
 * used to specify the primary language of the document for accessibility and SEO purposes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Basic HTML document structure</jc>
 * 	Html <jv>html1</jv> = <jsm>html</jsm>(
 * 		<jsm>head</jsm>(
 * 			<jsm>title</jsm>(<js>"My Web Page"</js>),
 * 			<jsm>meta</jsm>().charset(<js>"utf-8"</js>)
 * 		),
 * 		<jsm>body</jsm>(<js>"Hello, World!"</js>)
 * 	).lang(<js>"en"</js>);
 *
 * 	<jc>// HTML with manifest for offline support</jc>
 * 	Html <jv>html2</jv> = <jsm>html</jsm>(
 * 		<jsm>head</jsm>(
 * 			<jsm>title</jsm>(<js>"Offline App"</js>)
 * 		),
 * 		<jsm>body</jsm>(<js>"This app works offline!"</js>)
 * 	).lang(<js>"en"</js>).manifest(<js>"app.manifest"</js>);
 *
 * 	<jc>// HTML with custom attributes</jc>
 * 	Html <jv>html3</jv> = <jsm>html</jsm>(
 * 		<jsm>head</jsm>(
 * 			<jsm>title</jsm>(<js>"Página en Español"</js>)
 * 		),
 * 		<jsm>body</jsm>(<js>"¡Hola, Mundo!"</js>)
 * 	).lang(<js>"es"</js>).class_(<js>"no-js"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#html() html()}
 * 		<li class='jm'>{@link HtmlBuilder#html(Object...) html(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "html")
public class Html extends HtmlElementContainer<Html> {

	/**
	 * Creates an empty {@link Html} element.
	 */
	public Html() {}

	/**
	 * Creates an {@link Html} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Html(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/semantics.html#attr-html-manifest">manifest</a> attribute.
	 *
	 * <p>
	 * Specifies the URL of the application cache manifest file. This enables offline functionality
	 * by allowing the browser to cache resources for offline use.
	 *
	 * <p>
	 * The manifest file should be a text file that lists resources to be cached.
	 *
	 * @param value The URL of the application cache manifest file. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Html manifest(String value) {
		attr("manifest", value);
		return this;
	}

}