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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-noscript-element">&lt;noscript&gt;</a>
 * element.
 *
 * <p>
 * The noscript element represents nothing if scripting is enabled, and represents its children
 * if scripting is disabled. It is used to provide alternative content for users who have
 * JavaScript disabled or when JavaScript is not available. The noscript element can contain
 * any flow content and is typically used to display a message or alternative functionality
 * when JavaScript cannot be executed. It is commonly placed in the head or body of a document.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple noscript message</jc>
 * 	Noscript <jv>simple</jv> = <jsm>noscript</jsm>(<js>"Please enable JavaScript to use this application."</js>);
 *
 * 	<jc>// Noscript with styling</jc>
 * 	Noscript <jv>styled</jv> = <jsm>noscript</jsm>(<js>"JavaScript is required for this page to function properly."</js>)
 * 		.class_(<js>"noscript-message"</js>);
 *
 * 	<jc>// Noscript with complex content</jc>
 * 	Noscript <jv>complex</jv> = <jsm>noscript</jsm>(
 * 		<jsm>h2</jsm>(<js>"JavaScript Required"</js>),
 * 		<jsm>p</jsm>(<js>"This application requires JavaScript to function."</js>),
 * 		<jsm>p</jsm>(<js>"Please enable JavaScript in your browser and refresh the page."</js>)
 * 	);
 *
 * 	<jc>// Noscript with ID</jc>
 * 	Noscript <jv>withId</jv> = <jsm>noscript</jsm>(<js>"JavaScript is disabled."</js>)
 * 		.id(<js>"noscript-message"</js>);
 *
 * 	<jc>// Noscript with styling</jc>
 * 	Noscript <jv>styled2</jv> = <jsm>noscript</jsm>(<js>"JavaScript is required for this page."</js>)
 * 		.style(<js>"background-color: #f8d7da; color: #721c24; padding: 10px; border: 1px solid #f5c6cb;"</js>);
 *
 * 	<jc>// Noscript with multiple elements</jc>
 * 	Noscript <jv>multiple</jv> = <jsm>noscript</jsm>(
 * 		<jsm>h3</jsm>(<js>"JavaScript Disabled"</js>),
 * 		<jsm>p</jsm>(<js>"This page requires JavaScript to function properly."</js>),
 * 		<jsm>p</jsm>(<js>"Please enable JavaScript and refresh the page."</js>),
 * 		<jsm>a</jsm>(<js>"/help/javascript"</js>, <js>"Learn how to enable JavaScript"</js>)
 * 	);
 *
 * 	<jc>// Noscript with form</jc>
 * 	Noscript <jv>withForm</jv> = <jsm>noscript</jsm>(
 * 		<jsm>p</jsm>(<js>"JavaScript is disabled. Please use the form below:"</js>),
 * 		<jsm>form</jsm>().action(<js>"/submit"</js>).method(<js>"post"</js>).children(
 * 			<jsm>input</jsm>(<js>"text"</js>).name(<js>"name"</js>).placeholder(<js>"Name"</js>),
 * 			<jsm>input</jsm>(<js>"submit"</js>).value(<js>"Submit"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "noscript")
public class Noscript extends HtmlElementMixed<Noscript> {

	/**
	 * Creates an empty {@link Noscript} element.
	 */
	public Noscript() {}

	/**
	 * Creates a {@link Noscript} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Noscript(Object...children) {
		children(children);
	}

}