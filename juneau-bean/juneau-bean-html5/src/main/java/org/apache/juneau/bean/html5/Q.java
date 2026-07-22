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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-q-element">&lt;q&gt;</a>
 * element.
 *
 * <p>
 * The q element represents a short inline quotation. It is used for short quotes that are part
 * of the surrounding text, as opposed to blockquote which is used for longer, standalone quotations.
 * The cite attribute can be used to provide a link to the source of the quotation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple inline quotation</jc>
 * 	Q <jv>q1</jv> = <jsm>q</jsm>(<js>"To be or not to be, that is the question."</js>);
 *
 * 	<jc>// Quotation with citation</jc>
 * 	Q <jv>q2</jv> = <jsm>q</jsm>(<js>"The only way to do great work is to love what you do."</js>)
 * 		.cite(<js>"https://example.com/source"</js>);
 *
 * 	<jc>// Quotation in a paragraph</jc>
 * 	P <jv>p1</jv> = <jsm>p</jsm>(
 * 		<js>"As Shakespeare once said, "</js>,
 * 		<jsm>q</jsm>(<js>"All the world's a stage"</js>),
 * 		<js>" and we are merely players."</js>
 * 	);
 *
 * 	<jc>// Quotation with styling</jc>
 * 	Q <jv>q3</jv> = <jsm>q</jsm>(<js>"Innovation distinguishes between a leader and a follower."</js>)
 * 		.class_(<js>"highlighted-quote"</js>)
 * 		.cite(<js>"https://example.com/author"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#q() q()}
 * 		<li class='jm'>{@link HtmlBuilder#q(Object...) q(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "q")
public class Q extends HtmlElementMixed<Q> {

	/**
	 * Creates an empty {@link Q} element.
	 */
	public Q() {}

	/**
	 * Creates a {@link Q} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Q(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#attr-q-cite">cite</a> attribute.
	 *
	 * <p>
	 * Specifies the URL of the source document or message from which the quotation was taken.
	 * This provides context and attribution for the quoted content.
	 *
	 * <p>
	 * The URL should point to the original source of the quoted material.
	 *
	 * @param value The URL of the source document for the quotation. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Q cite(String value) {
		attr("cite", value);
		return this;
	}
}