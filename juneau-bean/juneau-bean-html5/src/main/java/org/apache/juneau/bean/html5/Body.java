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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-body-element">&lt;body&gt;</a>
 * element.
 *
 * <p>
 * The body element represents the content of an HTML document. It contains all the visible content
 * of the page, including text, images, links, forms, and other elements. The body element is
 * typically the direct child of the html element and contains all the main content of the document.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple body with text content</jc>
 * 	Body <jv>body1</jv> = <jsm>body</jsm>().text(<js>"Welcome to our website!"</js>);
 *
 * 	<jc>// Body with structured content</jc>
 * 	Body <jv>body2</jv> = <jsm>body</jsm>()
 * 		.children(
 * 			<jsm>header</jsm>().children(
 * 				<jsm>h1</jsm>().text(<js>"Page Title"</js>)
 * 			),
 * 			<jsm>main</jsm>().children(
 * 				<jsm>p</jsm>().text(<js>"Main content goes here."</js>)
 * 			),
 * 			<jsm>footer</jsm>().text(<js>"Copyright 2024"</js>)
 * 		);
 *
 * 	<jc>// Body with event handlers</jc>
 * 	Body <jv>body3</jv> = <jsm>body</jsm>()
 * 		.onload(<js>"initializePage()"</js>)
 * 		.onbeforeunload(<js>"return confirm('Are you sure you want to leave?')"</js>)
 * 		.text(<js>"Page content"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#body() body()}
 * 		<li class='jm'>{@link HtmlBuilder#body(Object...) body(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "body")
public class Body extends HtmlElementMixed<Body> {

	/**
	 * Creates an empty {@link Body} element.
	 */
	public Body() {}

	/**
	 * Creates a {@link Body} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Body(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onafterprint">onafterprint</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the document has finished printing.
	 *
	 * @param value JavaScript code to execute when the afterprint event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body onafterprint(String value) {
		attr("onafterprint", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onbeforeunload">onbeforeunload</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the document is about to be unloaded (page refresh, navigation, etc.).
	 *
	 * @param value JavaScript code to execute when the beforeunload event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body onbeforeunload(String value) {
		attr("onbeforeunload", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onmessage">onmessage</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when a message is received from another window or worker.
	 *
	 * @param value JavaScript code to execute when the message event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body onmessage(String value) {
		attr("onmessage", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-ononline">ononline</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the browser has gone online.
	 *
	 * @param value JavaScript code to execute when the online event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body ononline(String value) {
		attr("ononline", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onpageshow">onpageshow</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the page is displayed (including when returning from back/forward cache).
	 *
	 * @param value JavaScript code to execute when the pageshow event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body onpageshow(String value) {
		attr("onpageshow", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/webappapis.html#handler-window-onstorage">onstorage</a>
	 * attribute.
	 *
	 * <p>
	 * Event handler for when the storage area (localStorage or sessionStorage) is modified.
	 *
	 * @param value JavaScript code to execute when the storage event occurs. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Body onstorage(String value) {
		attr("onstorage", value);
		return this;
	}

}