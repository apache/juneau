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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-samp-element">&lt;samp&gt;</a>
 * element.
 *
 * <p>
 * The samp element represents sample or quoted output from a computer program or system. It is
 * used to mark up text that represents the output of a program, command, or system, such as
 * console output, error messages, or program results. The samp element is typically rendered
 * in a monospace font and is commonly used in documentation, tutorials, and technical writing
 * to distinguish program output from regular text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple sample output</jc>
 * 	Samp <jv>simple</jv> = <jsm>samp</jsm>(<js>"Hello World"</js>);
 *
 * 	<jc>// Samp with styling</jc>
 * 	Samp <jv>styled</jv> = <jsm>samp</jsm>(<js>"$ npm install"</js>)
 * 		.class_(<js>"console-output"</js>);
 *
 * 	<jc>// Samp with complex content</jc>
 * 	Samp <jv>complex</jv> = <jsm>samp</jsm>(
 * 		<js>"Error: "</js>,
 * 		<jsm>strong</jsm>(<js>"File not found"</js>),
 * 		<js>" at line 42"</js>
 * 	);
 *
 * 	<jc>// Samp with ID</jc>
 * 	Samp <jv>withId</jv> = <jsm>samp</jsm>(<js>"Sample output with ID"</js>)
 * 		.id(<js>"sample-output"</js>);
 *
 * 	<jc>// Samp with styling</jc>
 * 	Samp <jv>styled2</jv> = <jsm>samp</jsm>(<js>"Custom styled sample output"</js>)
 * 		.style(<js>"background-color: #f4f4f4; padding: 5px; border: 1px solid #ddd;"</js>);
 *
 * 	<jc>// Samp with multiple elements</jc>
 * 	Samp <jv>multiple</jv> = <jsm>samp</jsm>(
 * 		<js>"$ "</js>,
 * 		<jsm>samp</jsm>(<js>"ls -la"</js>),
 * 		<js>"\n"</js>,
 * 		<jsm>samp</jsm>(<js>"total 24"</js>),
 * 		<js>"\n"</js>,
 * 			new Samp().children("drwxr-xr-x  5 user  staff  160 Jan 15 10:30 .")
 * 		);
 *
 * 	// Samp with links
 * 	Samp withLinks = new Samp()
 * 		.children(
 * 			"See ",
 * 			new A().href("/docs/output").children("output documentation"),
 * 			" for more details."
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "samp")
public class Samp extends HtmlElementMixed<Samp> {

	/**
	 * Creates an empty {@link Samp} element.
	 */
	public Samp() {}

	/**
	 * Creates a {@link Samp} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Samp(Object...children) {
		children(children);
	}
}