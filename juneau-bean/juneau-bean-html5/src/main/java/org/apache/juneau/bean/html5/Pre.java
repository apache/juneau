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

import static org.apache.juneau.marshall.xml.XmlFormat.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.xml.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-pre-element">&lt;pre&gt;</a>
 * element.
 *
 * <p>
 * The pre element represents a block of preformatted text, in which structure is represented
 * by typographic conventions rather than by elements. It is used to display text exactly as
 * it is written, preserving whitespace, line breaks, and formatting. The pre element is
 * typically rendered in a monospace font and is commonly used for displaying code snippets,
 * ASCII art, or any text where formatting and spacing are important.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple preformatted text</jc>
 * 	Pre <jv>simple</jv> = <jsm>pre</jsm>(<js>"This text preserves\n  all formatting\n    and spacing."</js>);
 *
 * 	<jc>// Pre with styling</jc>
 * 	Pre <jv>styled</jv> = <jsm>pre</jsm>(<js>"function hello() {\n  return 'Hello World';\n}"</js>)
 * 		.class_(<js>"code-block"</js>);
 *
 * 	<jc>// Pre with complex content</jc>
 * 	Pre <jv>complex</jv> = <jsm>pre</jsm>(
 * 		<js>"  "</js>,
 * 		<jsm>strong</jsm>(<js>"Bold text"</js>),
 * 		<js>" in preformatted content\n"</js>,
 * 		<js>"  "</js>,
 * 		<jsm>em</jsm>(<js>"Italic text"</js>),
 * 		<js>" with preserved formatting"</js>
 * 	);
 *
 * 	<jc>// Pre with ID</jc>
 * 	Pre <jv>withId</jv> = <jsm>pre</jsm>(<js>"console.log('Hello World');"</js>)
 * 		.id(<js>"code-example"</js>);
 *
 * 	<jc>// Pre with styling</jc>
 * 	Pre <jv>styled2</jv> = <jsm>pre</jsm>(<js>"This is styled preformatted text."</js>)
 * 		.style(<js>"background-color: #f4f4f4; padding: 10px; border: 1px solid #ddd;"</js>);
 *
 * 	<jc>// Pre with multiple elements</jc>
 * 	Pre <jv>multiple</jv> = <jsm>pre</jsm>(
 * 		<js>"Line 1: "</js>,
 * 		<jsm>span</jsm>(<js>"function"</js>).class_(<js>"keyword"</js>),
 * 			" ",
 * 			new Span().class_("function-name").children("example"),
 * 			"() {\n",
 * 			"Line 2:   ",
 * 			new Span().class_("keyword").children("return"),
 * 			" ",
 * 			new Span().class_("string").children("'Hello'"),
 * 			";\n",
 * 			"Line 3: }"
 * 		);
 *
 * 	// Pre with code
 * 	Pre withCode = new Pre()
 * 		.children(
 * 			new Code().children("const message = 'Hello World';\nconsole.log(message);")
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#pre() pre()}
 * 		<li class='jm'>{@link HtmlBuilder#pre(Object...) pre(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "pre")
public class Pre extends HtmlElementMixed<Pre> {

	/**
	 * Creates an empty {@link Pre} element.
	 */
	public Pre() {}

	/**
	 * Creates a {@link Pre} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Pre(Object...children) {
		children(children);
	}

	@Xml(format = MIXED_PWS)
	@BeanProp(name="c") @MarshalledProp(dictionary=HtmlBeanDictionary.class)
	@Override
	public List<Object> getChildren() { return super.getChildren(); }

}