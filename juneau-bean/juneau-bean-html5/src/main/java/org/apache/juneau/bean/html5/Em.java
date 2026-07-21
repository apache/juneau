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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-em-element">&lt;em&gt;</a>
 * element.
 *
 * <p>
 * The em element represents stress emphasis of its contents. It is used to mark up text that should
 * be emphasized or stressed, typically rendered in italics by browsers. The em element indicates
 * that the text has semantic importance and should be read with emphasis, making it different from
 * purely stylistic italic text. It is commonly used to emphasize words or phrases within sentences
 * to change the meaning or add emphasis to the content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple emphasis</jc>
 * 	Em <jv>simple</jv> = <jsm>em</jsm>(<js>"important"</js>);
 *
 * 	<jc>// Emphasis with styling</jc>
 * 	Em <jv>styled</jv> = <jsm>em</jsm>(<js>"critical"</js>)
 * 		.class_(<js>"highlight"</js>);
 *
 * 	<jc>// Emphasis in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"This is "</js>,
 * 		<jsm>em</jsm>(<js>"very important"</js>),
 * 		<js>" information."</js>
 * 	);
 *
 * 	<jc>// Emphasis with complex content</jc>
 * 	Em <jv>complex</jv> = <jsm>em</jsm>(
 * 		<js>"must"</js>,
 * 		<jsm>span</jsm>().class_(<js>"not"</js>).children(<js>" not"</js>),
 * 		<js>" be ignored"</js>
 * 	);
 *
 * 	<jc>// Emphasis with links</jc>
 * 	Em <jv>withLinks</jv> = <jsm>em</jsm>(
 * 		<js>"Please read the "</js>,
 * 		<jsm>a</jsm>(<js>"/manual"</js>, <js>"manual"</js>),
 * 		<js>" carefully."</js>
 * 	);
 *
 * 	<jc>// Emphasis with ID</jc>
 * 	Em <jv>withId</jv> = <jsm>em</jsm>(<js>"Warning: This action cannot be undone."</js>)
 * 		.id(<js>"warning-text"</js>);
 *
 * 	<jc>// Emphasis with styling</jc>
 * 	Em <jv>styled2</jv> = <jsm>em</jsm>(<js>"Error: Invalid input"</js>)
 * 		.style(<js>"color: red; font-weight: bold;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#em() em()}
 * 		<li class='jm'>{@link HtmlBuilder#em(Object...) em(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "em")
public class Em extends HtmlElementMixed<Em> {

	/**
	 * Creates an empty {@link Em} element.
	 */
	public Em() {}

	/**
	 * Creates an {@link Em} element with the specified {@link Em#children(Object[])} nodes.
	 *
	 * @param children The {@link Em#children(Object[])} nodes.
	 */
	public Em(Object...children) {
		children(children);
	}

}