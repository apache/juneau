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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/scripting-1.html#the-template-element">&lt;template&gt;</a>
 * element.
 *
 * <p>
 * The template element represents a template for a fragment of HTML that can be cloned and
 * inserted into the document by script. It is used to define reusable HTML content that
 * can be instantiated multiple times using JavaScript. The template element is not rendered
 * in the document until its content is cloned and inserted into the DOM. It is commonly
 * used for creating dynamic content, such as repeating elements in lists or generating
 * content based on data.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple template</jc>
 * 	Template <jv>simple</jv> = <jsm>template</jsm>(
 * 		<jsm>p</jsm>(<js>"This is a template paragraph."</js>)
 * 	);
 *
 * 	<jc>// Template with styling</jc>
 * 	Template <jv>styled</jv> = <jsm>template</jsm>(
 * 		<jsm>div</jsm>(
 * 			<jsm>h3</jsm>(<js>"Item Title"</js>),
 * 			<jsm>p</jsm>(<js>"Item description"</js>)
 * 		).class_(<js>"item"</js>)
 * 	).class_(<js>"item-template"</js>);
 *
 * 	<jc>// Template with complex content</jc>
 * 	Template <jv>complex</jv> = <jsm>template</jsm>(
 * 		<jsm>article</jsm>(
 * 			<jsm>header</jsm>(
 * 				<jsm>h2</jsm>(<js>"Article Title"</js>),
 * 				<jsm>p</jsm>(<js>"Article subtitle"</js>)
 * 			),
 * 			<jsm>p</jsm>(<js>"Article content goes here."</js>),
 * 			<jsm>footer</jsm>(<js>"Article footer"</js>)
 * 				)
 * 		);
 *
 * 	// Template with ID
 * 	Template withId = template()
 * 		.id("user-card-template")
 * 		.children(
 * 			div().class_("user-card")
 * 				.children(
 * 					img().src("/avatar.jpg").alt("User Avatar"),
 * 					h3().children("User Name"),
 * 					p().children("User Bio")
 * 				)
 * 		);
 *
 * 	// Template with styling
 * 	Template styled2 = template()
 * 		.style("display: none;")
 * 		.children(
 * 			div().class_("modal")
 * 				.children(
 * 					h2().children("Modal Title"),
 * 					p().children("Modal content")
 * 				)
 * 		);
 *
 * 	// Template with multiple elements
 * 	Template multiple = template()
 * 		.children(
 * 			div().class_("product-card")
 * 				.children(
 * 					img().src("/product.jpg").alt("Product Image"),
 * 					h3().children("Product Name"),
 * 					p().children("Product Description"),
 * 					span().class_("price").children("$99.99"),
 * 					button().children("Add to Cart")
 * 				)
 * 		);
 *
 * 	// Template with form
 * 	Template withForm = template()
 * 		.children(
 * 			form().class_("comment-form")
 * 				.children(
 * 					textarea().placeholder("Enter your comment"),
 * 					button().type("submit").children("Submit Comment")
 * 				)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#template() template()}
 * 		<li class='jm'>{@link HtmlBuilder#template(String, Object...) template(String, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "template")
public class Template extends HtmlElementMixed<Template> {

	/**
	 * Creates an empty {@link Template} element.
	 */
	public Template() {}

	/**
	 * Creates a {@link Template} element with the specified {@link Template#id(String)} attribute and child nodes.
	 *
	 * @param id The {@link Template#id(String)} attribute. Can be <jk>null</jk> to unset the attribute.
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Template(String id, Object...children) {
		id(id).children(children);
	}
}