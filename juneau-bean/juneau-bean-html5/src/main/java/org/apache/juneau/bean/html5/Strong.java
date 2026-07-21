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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-strong-element">&lt;strong&gt;</a>
 * element.
 *
 * <p>
 * The strong element represents strong importance, seriousness, or urgency for its contents.
 * It indicates that the content is of strong importance and should be emphasized. The strong
 * element is typically rendered in bold by browsers, but the visual styling should be controlled
 * with CSS rather than relying on the default browser styling.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Important warning</jc>
 * 	Strong <jv>warning</jv> = <jsm>strong</jsm>(<js>"Warning: This action cannot be undone!"</js>);
 *
 * 	<jc>// Important notice</jc>
 * 	Strong <jv>notice</jv> = <jsm>strong</jsm>(<js>"Important: Please read the terms and conditions."</js>);
 *
 * 	<jc>// Emphasis in text</jc>
 * 	Strong <jv>emphasis</jv> = <jsm>strong</jsm>(<js>"This is very important information."</js>);
 *
 * 	<jc>// Strong text with styling</jc>
 * 	Strong <jv>styled</jv> = <jsm>strong</jsm>(<js>"Critical system error detected!"</js>)
 * 		.class_(<js>"alert"</js>);
 *
 * 	<jc>// Strong text in a sentence</jc>
 * 	Strong <jv>sentence</jv> = <jsm>strong</jsm>(<js>"The deadline is tomorrow."</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "strong")
public class Strong extends HtmlElementMixed<Strong> {

	/**
	 * Creates an empty {@link Strong} element.
	 */
	public Strong() {}

	/**
	 * Creates a {@link Strong} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Strong(Object...children) {
		children(children);
	}
}