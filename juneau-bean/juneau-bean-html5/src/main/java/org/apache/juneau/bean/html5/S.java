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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-s-element">&lt;s&gt;</a>
 * element.
 *
 * <p>
 * The s element represents contents that are no longer accurate or no longer relevant. It is
 * used to mark up text that has been struck through or crossed out, indicating that the content
 * is outdated, incorrect, or no longer applicable. The s element is typically rendered with
 * a line through the text (strikethrough) and is commonly used for showing price changes,
 * outdated information, or content that has been superseded.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple strikethrough text</jc>
 * 	S <jv>simple</jv> = <jsm>s</jsm>(<js>"This text is no longer accurate"</js>);
 *
 * 	<jc>// S with styling</jc>
 * 	S <jv>styled</jv> = <jsm>s</jsm>(<js>"This information is outdated"</js>)
 * 		.class_(<js>"outdated"</js>);
 *
 * 	<jc>// S with complex content</jc>
 * 	S <jv>complex</jv> = <jsm>s</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>strong</jsm>(<js>"old price"</js>),
 * 		<js>" was $100."</js>
 * 	);
 *
 * 	<jc>// S with ID</jc>
 * 	S <jv>withId</jv> = <jsm>s</jsm>(<js>"Text with ID"</js>)
 * 		.id(<js>"strikethrough-text"</js>);
 *
 * 	<jc>// S with styling</jc>
 * 	S <jv>styled2</jv> = <jsm>s</jsm>(<js>"Custom styled strikethrough text"</js>)
 * 		.style(<js>"color: #999; text-decoration: line-through;"</js>);
 *
 * 	<jc>// S with multiple elements</jc>
 * 	S <jv>multiple</jv> = <jsm>s</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>s</jsm>(<js>"old version"</js>),
 * 		<js>" has been "</js>,
 * 		<jsm>s</jsm>(<js>"replaced"</js>),
 * 		<js>" by the new one."</js>
 * 		);
 *
 * 	// S with links
 * 	S withLinks = new S()
 * 		.children(
 * 			"See ",
 * 			new A().href("/old-version").children("old documentation"),
 * 			" (no longer maintained)"
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "s")
public class S extends HtmlElementMixed<S> {

	/**
	 * Creates an empty {@link S} element.
	 */
	public S() {}

	/**
	 * Creates an {@link S} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public S(Object...children) {
		children(children);
	}
}