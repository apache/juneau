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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#the-ins-element">&lt;ins&gt;</a>
 * element.
 *
 * <p>
 * The ins element represents a range of text that has been added to a document. It is used to
 * mark up content that has been inserted or added to the document, typically in the context of
 * document editing or version control. The ins element can contain any flow content and is
 * commonly used with the del element to show document changes. It is typically rendered with
 * an underline or other visual indication to show that the content has been added.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple inserted text</jc>
 * 	Ins <jv>simple</jv> = <jsm>ins</jsm>(<js>"This text was added"</js>);
 *
 * 	<jc>// Ins with styling</jc>
 * 	Ins <jv>styled</jv> = <jsm>ins</jsm>(<js>"Styled inserted text"</js>)
 * 		.class_(<js>"insertion"</js>);
 *
 * 	<jc>// Ins with complex content</jc>
 * 	Ins <jv>complex</jv> = <jsm>ins</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>strong</jsm>(<js>"new feature"</js>),
 * 		<js>" has been added to the system."</js>
 * 	);
 *
 * 	<jc>// Ins with ID</jc>
 * 	Ins <jv>withId</jv> = <jsm>ins</jsm>(<js>"Text with ID"</js>)
 * 		.id(<js>"inserted-text"</js>);
 *
 * 	<jc>// Ins with styling</jc>
 * 	Ins <jv>styled2</jv> = <jsm>ins</jsm>(<js>"Custom styled inserted text"</js>)
 * 		.style(<js>"background-color: #d4edda; color: #155724; text-decoration: underline;"</js>);
 *
 * 	<jc>// Ins with multiple elements</jc>
 * 	Ins <jv>multiple</jv> = <jsm>ins</jsm>(
 * 		<js>"The "</js>,
 * 		<jsm>ins</jsm>(<js>"new section"</js>),
 * 		<js>" has been "</js>,
 * 		<jsm>ins</jsm>(<js>"added"</js>),
 * 		<js>" to the document."</js>
 * 	);
 *
 * 	<jc>// Ins with links</jc>
 * 	Ins <jv>withLinks</jv> = <jsm>ins</jsm>(
 * 		<js>"See "</js>,
 * 		<jsm>a</jsm>(<js>"/changes"</js>, <js>"change log"</js>),
 * 		<js>" for more details."</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#ins() ins()}
 * 		<li class='jm'>{@link HtmlBuilder#ins(Object...) ins(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "ins")
public class Ins extends HtmlElementMixed<Ins> {

	/**
	 * Creates an empty {@link Ins} element.
	 */
	public Ins() {}

	/**
	 * Creates an {@link Ins} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Ins(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#attr-mod-cite">cite</a> attribute.
	 *
	 * <p>
	 * Link to the source of the quotation or more information about the edit.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Ins cite(String value) {
		attr("cite", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#attr-mod-datetime">datetime</a> attribute.
	 *
	 * <p>
	 * Date and (optionally) time of the change.
	 *
	 * @param value The new value for this attribute. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Ins datetime(String value) {
		attr("datetime", value);
		return this;
	}

}