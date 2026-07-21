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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-wbr-element">&lt;wbr&gt;</a>
 * element.
 *
 * <p>
 * The wbr element represents a word break opportunity. It is used to indicate where a line
 * break may occur in text, allowing the browser to break long words or URLs at appropriate
 * points when necessary. The wbr element is a void element that does not contain any content
 * and is typically used within long words, URLs, or other text that might overflow their
 * container. It provides a hint to the browser about where it's acceptable to break the text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple word break</jc>
 * 	Wbr <jv>simple</jv> = <jsm>wbr</jsm>();
 *
 * 	<jc>// Wbr with styling</jc>
 * 	Wbr <jv>styled</jv> = <jsm>wbr</jsm>()
 * 		.class_(<js>"word-break"</js>);
 *
 * 	<jc>// Wbr with ID</jc>
 * 	Wbr <jv>withId</jv> = <jsm>wbr</jsm>()
 * 		.id(<js>"word-break-1"</js>);
 *
 * 	<jc>// Wbr with styling</jc>
 * 	Wbr <jv>styled2</jv> = <jsm>wbr</jsm>()
 * 		.style(<js>"display: inline;"</js>);
 *
 * 	<jc>// Wbr in long word</jc>
 * 	P <jv>longWord</jv> = <jsm>p</jsm>(
 * 		<js>"This is a very long word: "</js>,
 * 		<js>"supercalifragilisticexpialidocious"</js>,
 * 		<jsm>wbr</jsm>(),
 * 		<js>" that might need to break."</js>
 * 	);
 *
 * 	<jc>// Wbr in URL</jc>
 * 	P longUrl = p()
 * 		.children(
 * 			"Visit: ",
 * 			"https://www.example.com/very/long/path/to/resource",
 * 			wbr(),
 * 			" for more information."
 * 		);
 *
 * 	// Wbr in code
 * 	P code = p()
 * 		.children(
 * 			"Function: ",
 * 			"veryLongFunctionName",
 * 			wbr(),
 * 			"()"
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#wbr() wbr()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "wbr")
public class Wbr extends HtmlElementVoid<Wbr> {

	/**
	 * Creates an empty {@link Wbr} element.
	 */
	public Wbr() { /* Empty constructor. */ }

}