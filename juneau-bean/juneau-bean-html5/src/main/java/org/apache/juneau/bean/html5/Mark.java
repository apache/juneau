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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-mark-element">&lt;mark&gt;</a>
 * element.
 *
 * <p>
 * The mark element represents a run of text in one document marked or highlighted for reference
 * purposes, due to its relevance in another context. It is used to mark up text that should be
 * highlighted or emphasized, such as search results, important passages, or text that needs
 * attention. The mark element is typically rendered with a yellow background or other highlighting
 * to make the marked text stand out from the surrounding content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple marked text</jc>
 * 	Mark <jv>simple</jv> = <jsm>mark</jsm>(<js>"This text is highlighted"</js>);
 *
 * 	<jc>// Mark with styling</jc>
 * 	Mark <jv>styled</jv> = <jsm>mark</jsm>(<js>"Styled highlighted text"</js>)
 * 		.class_(<js>"highlight"</js>);
 *
 * 	<jc>// Mark with complex content</jc>
 * 	Mark <jv>complex</jv> = <jsm>mark</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>strong</jsm>(<js>"important"</js>),
 * 		<js>" information is highlighted."</js>
 * 	);
 *
 * 	<jc>// Mark with ID</jc>
 * 	Mark <jv>withId</jv> = <jsm>mark</jsm>(<js>"Text with ID"</js>)
 * 		.id(<js>"highlighted-text"</js>);
 *
 * 	<jc>// Mark with styling</jc>
 * 	Mark <jv>styled2</jv> = <jsm>mark</jsm>(<js>"Custom styled highlighted text"</js>)
 * 		.style(<js>"background-color: #ffeb3b; color: #000; padding: 2px 4px;"</js>);
 *
 * 	<jc>// Mark with multiple elements</jc>
 * 	Mark <jv>multiple</jv> = <jsm>mark</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>mark</jsm>(<js>"key points"</js>),
 * 		<js>" are "</js>,
 * 		<jsm>mark</jsm>(<js>"highlighted"</js>),
 * 		<js>" for emphasis."</js>
 * 	);
 *
 * 	<jc>// Mark with links</jc>
 * 	Mark <jv>withLinks</jv> = <jsm>mark</jsm>(
 * 		<js>"See "</js>,
 * 		<jsm>a</jsm>(<js>"/search"</js>, <js>"search results"</js>),
 * 		<js>" for more information."</js>
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "mark")
public class Mark extends HtmlElementMixed<Mark> {

	/**
	 * Creates an empty {@link Mark} element.
	 */
	public Mark() {}

	/**
	 * Creates a {@link Mark} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Mark(Object...children) {
		children(children);
	}

}