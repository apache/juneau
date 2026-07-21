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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-dl-element">&lt;dl&gt;</a>
 * element.
 *
 * <p>
 * The dl element represents a description list (also known as a definition list or association list).
 * It is used to group terms (dt elements) with their descriptions (dd elements), creating a list
 * of term-description pairs. The dl element is commonly used for glossaries, metadata lists, or
 * any other content where you need to associate terms with their definitions or descriptions.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple description list</jc>
 * 	Dl <jv>simple</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"HTML"</js>),
 * 		<jsm>dd</jsm>(<js>"HyperText Markup Language"</js>),
 * 		<jsm>dt</jsm>(<js>"CSS"</js>),
 * 		<jsm>dd</jsm>(<js>"Cascading Style Sheets"</js>)
 * 	);
 *
 * 	<jc>// Description list with styling</jc>
 * 	Dl <jv>styled</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"API"</js>),
 * 		<jsm>dd</jsm>(<js>"Application Programming Interface"</js>),
 * 		<jsm>dt</jsm>(<js>"DOM"</js>),
 * 		<jsm>dd</jsm>(<js>"Document Object Model"</js>)
 * 	).class_(<js>"glossary"</js>);
 *
 * 	<jc>// Description list with multiple descriptions</jc>
 * 	Dl <jv>multiple</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"JavaScript"</js>),
 * 		<jsm>dd</jsm>(<js>"A programming language for web development."</js>),
 * 		<jsm>dd</jsm>(<js>"It runs in web browsers and on servers."</js>),
 * 		<jsm>dt</jsm>(<js>"Python"</js>),
 * 		<jsm>dd</jsm>(<js>"A high-level programming language."</js>),
 * 		<jsm>dd</jsm>(<js>"Known for its simplicity and readability."</js>)
 * 	);
 *
 * 	<jc>// Description list with complex content</jc>
 * 	Dl <jv>complex</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"Web Standards"</js>),
 * 		<jsm>dd</jsm>(
 * 			<js>"Standards developed by the "</js>,
 * 			<jsm>a</jsm>(<js>"https://w3.org"</js>, <js>"W3C"</js>),
 * 			<js>" to ensure web compatibility."</js>
 * 		),
 * 		<jsm>dt</jsm>(<js>"Responsive Design"</js>),
 * 		<jsm>dd</jsm>(
 * 			<js>"Design approach that adapts to different "</js>,
 * 			<jsm>strong</jsm>(<js>"screen sizes"</js>),
 * 			<js>" and devices."</js>
 * 		)
 * 	);
 *
 * 	<jc>// Description list with ID</jc>
 * 	Dl <jv>withId</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"Framework"</js>),
 * 		<jsm>dd</jsm>(<js>"A collection of pre-written code for common tasks."</js>),
 * 		<jsm>dt</jsm>(<js>"Library"</js>),
 * 		<jsm>dd</jsm>(<js>"A collection of reusable code modules."</js>)
 * 	).id(<js>"tech-terms"</js>);
 *
 * 	<jc>// Description list with styling</jc>
 * 	Dl <jv>styled2</jv> = <jsm>dl</jsm>(
 * 		<jsm>dt</jsm>(<js>"Frontend"</js>),
 * 		<jsm>dd</jsm>(<js>"The client-side part of a web application."</js>),
 * 		<jsm>dt</jsm>(<js>"Backend"</js>),
 * 		<jsm>dd</jsm>(<js>"The server-side part of a web application."</js>)
 * 	).style(<js>"border: 1px solid #ccc; padding: 10px;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#dl() dl()}
 * 		<li class='jm'>{@link HtmlBuilder#dl(Object...) dl(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "dl")
public class Dl extends HtmlElementContainer<Dl> {

	/**
	 * Creates an empty {@link Dl} element.
	 */
	public Dl() {}

	/**
	 * Creates a {@link Dl} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Dl(Object...children) {
		children(children);
	}

}