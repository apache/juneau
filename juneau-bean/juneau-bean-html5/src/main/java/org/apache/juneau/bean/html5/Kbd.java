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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-kbd-element">&lt;kbd&gt;</a>
 * element.
 *
 * <p>
 * The kbd element represents user input, typically keyboard input but can also represent other types
 * of input such as voice commands or menu selections. It is commonly styled with a monospace font
 * to distinguish it from regular text and often appears in documentation, tutorials, and help pages.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple keyboard shortcut</jc>
 * 	Kbd <jv>simple</jv> = <jsm>kbd</jsm>(<js>"Ctrl+S"</js>);
 *
 * 	<jc>// Keyboard combination with nested kbd elements</jc>
 * 	P <jv>shortcut</jv> = <jsm>p</jsm>(
 * 		<js>"Press "</js>,
 * 		<jsm>kbd</jsm>(<js>"Ctrl"</js>),
 * 		<js>" + "</js>,
 * 		<jsm>kbd</jsm>(<js>"C"</js>),
 * 		<js>" to copy."</js>
 * 	);
 *
 * 	<jc>// Menu selection instruction</jc>
 * 	P <jv>menu</jv> = <jsm>p</jsm>(
 * 		<js>"Select "</js>,
 * 		<jsm>kbd</jsm>(<js>"File"</js>),
 * 		<js>" → "</js>,
 * 		<jsm>kbd</jsm>(<js>"Save As..."</js>)
 * 	);
 *
 * 	<jc>// Command line input</jc>
 * 	P <jv>command</jv> = <jsm>p</jsm>(
 * 		<js>"Type "</js>,
 * 		<jsm>kbd</jsm>(<js>"npm install"</js>),
 * 		<js>" to install dependencies."</js>
 * 	);
 *
 * 	<jc>// With styling</jc>
 * 	Kbd <jv>styled</jv> = <jsm>kbd</jsm>(<js>"Enter"</js>)
 * 		.class_(<js>"key-highlight"</js>)
 * 		.title(<js>"Press Enter to submit"</js>);
 *
 * 	<jc>// Multiple key sequence</jc>
 * 	P <jv>sequence</jv> = <jsm>p</jsm>(
 * 		<js>"To exit, press "</js>,
 * 		<jsm>kbd</jsm>(<js>"Esc"</js>),
 * 		<js>" or "</js>,
 * 		<jsm>kbd</jsm>(<js>"Ctrl"</js>),
 * 		<js>" + "</js>,
 * 		<jsm>kbd</jsm>(<js>"Q"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#kbd() kbd()}
 * 		<li class='jm'>{@link HtmlBuilder#kbd(Object...) kbd(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "kbd")
public class Kbd extends HtmlElementMixed<Kbd> {

	/**
	 * Creates an empty {@link Kbd} element.
	 */
	public Kbd() {}

	/**
	 * Creates a {@link Kbd} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Kbd(Object...children) {
		children(children);
	}

}