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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-colgroup-element">&lt;colgroup&gt;</a>
 * element.
 *
 * <p>
 * The colgroup element represents a group of one or more columns in a table. It is used to define
 * structural columns and can specify attributes that apply to all cells in those columns. The colgroup
 * element can contain col elements to define individual columns, or it can use the span attribute to
 * define a group of columns without explicitly listing each one. This element is typically placed
 * immediately after the opening table tag and before any thead, tbody, or tr elements.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple column group</jc>
 * 	Colgroup <jv>simple</jv> = <jsm>colgroup</jsm>();
 *
 * 	<jc>// Column group with span</jc>
 * 	Colgroup <jv>withSpan</jv> = <jsm>colgroup</jsm>().span(3);
 *
 * 	<jc>// Column group with individual columns</jc>
 * 	Colgroup <jv>withColumns</jv> = <jsm>colgroup</jsm>(
 * 		<jsm>col</jsm>().span(2),
 * 		<jsm>col</jsm>().span(1)
 * 	);
 *
 * 	<jc>// Column group with styling</jc>
 * 	Colgroup <jv>styled</jv> = <jsm>colgroup</jsm>()
 * 		.class_(<js>"header-columns"</js>)
 * 		.style(<js>"background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Column group with multiple columns</jc>
 * 	Colgroup <jv>multiple</jv> = <jsm>colgroup</jsm>(
 * 		<jsm>col</jsm>().class_(<js>"name-column"</js>).style(<js>"width: 200px;"</js>),
 * 		<jsm>col</jsm>().class_(<js>"age-column"</js>).style(<js>"width: 100px;"</js>),
 * 		<jsm>col</jsm>().class_(<js>"city-column"</js>).style(<js>"width: 150px;"</js>)
 * 	);
 *
 * 	<jc>// Column group with alignment</jc>
 * 	Colgroup <jv>aligned</jv> = <jsm>colgroup</jsm>(
 * 		<jsm>col</jsm>().style(<js>"text-align: left;"</js>),
 * 		<jsm>col</jsm>().style(<js>"text-align: center;"</js>),
 * 		<jsm>col</jsm>().style(<js>"text-align: right;"</js>)
 * 	);
 *
 * 	<jc>// Column group with ID</jc>
 * 	Colgroup <jv>withId</jv> = <jsm>colgroup</jsm>()
 * 		.id(<js>"data-columns"</js>)
 * 		.span(4);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#colgroup() colgroup()}
 * 		<li class='jm'>{@link HtmlBuilder#colgroup(Object...) colgroup(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "colgroup")
public class Colgroup extends HtmlElementContainer<Colgroup> {

	/**
	 * Creates an empty {@link Colgroup} element.
	 */
	public Colgroup() {}

	/**
	 * Creates a {@link Colgroup} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Colgroup(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-colgroup-span">span</a> attribute.
	 *
	 * <p>
	 * Number of columns spanned by the element.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Colgroup span(Object value) {
		attr("span", value);
		return this;
	}

}