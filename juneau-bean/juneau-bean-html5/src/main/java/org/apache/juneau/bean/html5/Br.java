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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-br-element">&lt;br&gt;</a>
 * element.
 *
 * <p>
 * The br element represents a line break. It is used to create a line break in text content,
 * forcing the text that follows it to start on a new line. The br element is a void element
 * that does not contain any content and is typically used within text content to create
 * line breaks where needed, such as in addresses, poetry, or other formatted text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple line break</jc>
 * 	Br <jv>simple</jv> = <jsm>br</jsm>();
 *
 * 	<jc>// Address with line breaks</jc>
 * 	P <jv>address</jv> = <jsm>p</jsm>(
 * 		<js>"John Doe"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"123 Main Street"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Anytown, ST 12345"</js>
 * 	);
 *
 * 	<jc>// Poetry with line breaks</jc>
 * 	P <jv>poetry</jv> = <jsm>p</jsm>(
 * 		<js>"Roses are red,"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Violets are blue,"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Sugar is sweet,"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"And so are you."</js>
 * 	);
 *
 * 	<jc>// Contact information</jc>
 * 	P <jv>contact</jv> = <jsm>p</jsm>(
 * 		<js>"Phone: (555) 123-4567"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Email: john@example.com"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Website: www.example.com"</js>
 * 	);
 *
 * 	<jc>// Styled line break</jc>
 * 	Br <jv>styled</jv> = <jsm>br</jsm>()
 * 		.class_(<js>"line-break"</js>)
 * 		.style(<js>"margin: 10px 0;"</js>);
 *
 * 	<jc>// Multiple line breaks</jc>
 * 	P <jv>multiple</jv> = <jsm>p</jsm>(
 * 		<js>"First line"</js>,
 * 		<jsm>br</jsm>(),
 * 		<jsm>br</jsm>(),
 * 		<js>"Third line (with extra space)"</js>
 * 	);
 *
 * 	<jc>// Line break in a list</jc>
 * 	Li <jv>listItem</jv> = <jsm>li</jsm>(
 * 		<js>"Item 1"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Additional details for item 1"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#br() br()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "br")
public class Br extends HtmlElementVoid<Br> {

	/**
	 * Creates an empty {@link Br} element.
	 */
	public Br() { /* Empty constructor. */ }

}