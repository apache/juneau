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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-code-element">&lt;code&gt;</a>
 * element.
 *
 * <p>
 * The code element represents a fragment of computer code. It is used to mark up inline code snippets,
 * variable names, function names, or any other computer code that appears within normal text. The code
 * element is typically rendered in a monospace font and is commonly used in documentation, tutorials,
 * and technical writing to distinguish code from regular text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple code snippet</jc>
 * 	Code <jv>simple</jv> = <jsm>code</jsm>(<js>"console.log('Hello World');"</js>);
 *
 * 	<jc>// Code with styling</jc>
 * 	Code <jv>styled</jv> = <jsm>code</jsm>(<js>"getElementById"</js>)
 * 		.class_(<js>"inline-code"</js>);
 *
 * 	<jc>// Code in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"Use the "</js>,
 * 		<jsm>code</jsm>(<js>"print()"</js>),
 * 		<js>" function to display output."</js>
 * 	);
 *
 * 	<jc>// Variable name</jc>
 * 	Code <jv>variable</jv> = <jsm>code</jsm>(<js>"userName"</js>);
 *
 * 	<jc>// Function call</jc>
 * 	Code <jv>function</jv> = <jsm>code</jsm>(<js>"calculateTotal(price, tax)"</js>);
 *
 * 	<jc>// Multiple code elements</jc>
 * 	P <jv>multiple</jv> = <jsm>p</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>code</jsm>(<js>"if"</js>),
 * 		<js>" statement checks the condition, and "</js>,
 * 		<jsm>code</jsm>(<js>"else"</js>),
 * 		<js>" provides an alternative."</js>
 * 	);
 *
 * 	<jc>// Code with complex content</jc>
 * 	Code <jv>complex</jv> = <jsm>code</jsm>(
 * 		<js>"const "</js>,
 * 		<jsm>strong</jsm>(<js>"result"</js>),
 * 		<js>" = "</js>,
 * 		<jsm>em</jsm>(<js>"calculate"</js>),
 * 		<js>"();"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#code() code()}
 * 		<li class='jm'>{@link HtmlBuilder#code(Object...) code(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "code")
public class Code extends HtmlElementMixed<Code> {

	/**
	 * Creates an empty {@link Code} element.
	 */
	public Code() {}

	/**
	 * Creates a {@link Code} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Code(Object...children) {
		children(children);
	}

}