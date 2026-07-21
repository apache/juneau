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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-var-element">&lt;var&gt;</a>
 * element.
 *
 * <p>
 * The var element represents a variable. It is used to mark up variables in mathematical
 * expressions, programming code, or other contexts where a variable name needs to be
 * distinguished from regular text. The var element is typically rendered in italics and
 * is commonly used in documentation, tutorials, and technical writing to identify
 * variable names, function parameters, or mathematical variables.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple variable</jc>
 * 	Var <jv>simple</jv> = <jsm>var</jsm>(<js>"x"</js>);
 *
 * 	<jc>// Var with styling</jc>
 * 	Var <jv>styled</jv> = <jsm>var</jsm>(<js>"userName"</js>)
 * 		.class_(<js>"variable"</js>);
 *
 * 	<jc>// Var with complex content</jc>
 * 	Var <jv>complex</jv> = <jsm>var</jsm>(
 * 		<js>"The variable "</js>,
 * 		<jsm>strong</jsm>(<js>"count"</js>),
 * 		<js>" represents the number of items."</js>
 * 	);
 *
 * 	<jc>// Var with ID</jc>
 * 	Var <jv>withId</jv> = <jsm>var</jsm>(<js>"totalCount"</js>)
 * 		.id(<js>"variable-name"</js>);
 *
 * 	<jc>// Var with styling</jc>
 * 	Var <jv>styled2</jv> = <jsm>var</jsm>(<js>"customVariable"</js>)
 * 		.style(<js>"color: #0066cc; font-style: italic;"</js>)
 * 		.children("maxValue");
 *
 * 	// Var with multiple elements
 * 	Var multiple = var()
 * 		.children(
 * 			"The ",
 * 			var().children("x"),
 * 			" and ",
 * 			var().children("y"),
 * 			" variables are used in the equation."
 * 		);
 *
 * 	// Var with links
 * 	Var withLinks = var()
 * 		.children(
 * 			"See ",
 * 			a().href("/docs/variables").children("variable documentation"),
 * 			" for more information."
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#var() var()}
 * 		<li class='jm'>{@link HtmlBuilder#var(Object...) var(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "var")
public class Var extends HtmlElementMixed<Var> {

	/**
	 * Creates an empty {@link Var} element.
	 */
	public Var() {}

	/**
	 * Creates a {@link Var} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Var(Object...children) {
		children(children);
	}
}