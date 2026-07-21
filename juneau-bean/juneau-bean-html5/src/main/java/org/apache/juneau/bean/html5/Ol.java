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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-ol-element">&lt;ol&gt;</a>
 * element.
 *
 * <p>
 * The ol element represents an ordered list of items. It contains li elements that represent
 * individual list items. The list items are typically numbered automatically by the browser,
 * and the numbering can be customized using the type and start attributes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ordered list</jc>
 * 	Ol <jv>ol1</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"First item"</js>),
 * 		<jsm>li</jsm>(<js>"Second item"</js>),
 * 		<jsm>li</jsm>(<js>"Third item"</js>)
 * 	);
 *
 * 	<jc>// Ordered list with custom numbering</jc>
 * 	Ol <jv>ol2</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Item A"</js>),
 * 		<jsm>li</jsm>(<js>"Item B"</js>),
 * 		<jsm>li</jsm>(<js>"Item C"</js>)
 * 	).type(<js>"A"</js>).start(1);
 *
 * 	<jc>// Reversed ordered list</jc>
 * 	Ol <jv>ol3</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Last item"</js>),
 * 		<jsm>li</jsm>(<js>"Middle item"</js>),
 * 		<jsm>li</jsm>(<js>"First item"</js>)
 * 	).reversed(<jk>true</jk>);
 *
 * 	<jc>// Nested ordered list</jc>
 * 	Ol <jv>ol4</jv> = <jsm>ol</jsm>(
 * 		<jsm>li</jsm>(<js>"Main item 1"</js>),
 * 		<jsm>li</jsm>(
 * 			<jsm>ol</jsm>(
 * 				<jsm>li</jsm>(<js>"Sub item 1.1"</js>),
 * 				<jsm>li</jsm>(<js>"Sub item 1.2"</js>)
 * 			)
 * 		),
 * 		<jsm>li</jsm>(<js>"Main item 2"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#ol() ol()}
 * 		<li class='jm'>{@link HtmlBuilder#ol(Object...) ol(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "ol")
public class Ol extends HtmlElementContainer<Ol> {

	/**
	 * Creates an empty {@link Ol} element.
	 */
	public Ol() {}

	/**
	 * Creates an {@link Ol} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Ol(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-reversed">reversed</a>
	 * attribute.
	 *
	 * <p>
	 * Number the list backwards..
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"reversed"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Ol reversed(Object value) {
		attr("reversed", deminimize(value, "reversed"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-start">start</a> attribute.
	 *
	 * <p>
	 * Ordinal value of the first item.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Ol start(Object value) {
		attr("start", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#attr-ol-type">type</a> attribute.
	 *
	 * <p>
	 * Specifies the type of numbering to use for the ordered list items.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"1"</js> - Decimal numbers (1, 2, 3, ...) - default</li>
	 * 	<li><js>"a"</js> - Lowercase letters (a, b, c, ...)</li>
	 * 	<li><js>"A"</js> - Uppercase letters (A, B, C, ...)</li>
	 * 	<li><js>"i"</js> - Lowercase Roman numerals (i, ii, iii, ...)</li>
	 * 	<li><js>"I"</js> - Uppercase Roman numerals (I, II, III, ...)</li>
	 * </ul>
	 *
	 * @param value The type of numbering for the ordered list. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Ol type(String value) {
		attr("type", value);
		return this;
	}
}