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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-blockquote-element">&lt;blockquote&gt;</a>
 * element.
 *
 * <p>
 * The blockquote element represents a section that is quoted from another source. It is typically
 * rendered as an indented block of text to distinguish it from the surrounding content. The cite
 * attribute can be used to provide a link to the source of the quotation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Simple blockquote</jc>
 * 	Blockquote <jv>quote1</jv> = <jsm>blockquote</jsm>().text(<js>"The only way to do great work is to love what you do."</js>);
 *
 * 	<jc>// Blockquote with citation</jc>
 * 	Blockquote <jv>quote2</jv> = <jsm>blockquote</jsm>()
 * 		.cite(<js>"https://example.com/source"</js>)
 * 		.text(<js>"Innovation distinguishes between a leader and a follower."</js>);
 *
 * 	<jc>// Blockquote with nested content</jc>
 * 	Blockquote <jv>quote3</jv> = <jsm>blockquote</jsm>()
 * 		.cite(<js>"https://example.com/article"</js>)
 * 		.children(
 * 			<jsm>p</jsm>().text(<js>"This is a longer quotation that spans multiple paragraphs."</js>),
 * 			<jsm>p</jsm>().text(<js>"It can contain various HTML elements."</js>)
 * 		);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#blockquote() blockquote()}
 * 		<li class='jm'>{@link HtmlBuilder#blockquote(Object...) blockquote(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "blockquote")
public class Blockquote extends HtmlElementMixed<Blockquote> {

	/**
	 * Creates an empty {@link Blockquote} element.
	 */
	public Blockquote() {}

	/**
	 * Creates a {@link Blockquote} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Blockquote(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-blockquote-cite">cite</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the URL of the source document or message from which the quotation was taken.
	 * This provides context and attribution for the quoted content.
	 *
	 * <p>
	 * The URL should point to the original source of the quoted material.
	 *
	 * @param value The URL of the source document for the quotation. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public Blockquote cite(String value) {
		attr("cite", value);
		return this;
	}

}