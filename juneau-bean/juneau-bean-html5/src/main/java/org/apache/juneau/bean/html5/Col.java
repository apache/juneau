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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-col-element">&lt;col&gt;</a>
 * element.
 *
 * <p>
 * The col element represents one or more columns in a column group represented by its parent colgroup
 * element. It is used to define the structural columns of a table and can specify attributes that apply
 * to all cells in those columns. The col element is a void element that does not contain any content
 * and is typically used within a colgroup element to define column properties such as width, alignment,
 * and styling.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple column</jc>
 * 	Col <jv>simple</jv> = <jsm>col</jsm>();
 *
 * 	<jc>// Column spanning multiple columns</jc>
 * 	Col <jv>spanning</jv> = <jsm>col</jsm>().span(3);
 *
 * 	<jc>// Column with styling</jc>
 * 	Col <jv>styled</jv> = <jsm>col</jsm>()
 * 		.class_(<js>"highlight-column"</js>)
 * 		.style(<js>"background-color: #f0f0f0;"</js>);
 *
 * 	<jc>// Column with width</jc>
 * 	Col <jv>withWidth</jv> = <jsm>col</jsm>()
 * 		.style(<js>"width: 200px;"</js>);
 *
 * 	<jc>// Column with alignment</jc>
 * 	Col <jv>aligned</jv> = <jsm>col</jsm>()
 * 		.style(<js>"text-align: center;"</js>);
 *
 * 	<jc>// Column with ID</jc>
 * 	Col <jv>withId</jv> = <jsm>col</jsm>()
 * 		.id(<js>"name-column"</js>)
 * 		.span(2);
 *
 * 	<jc>// Column with multiple attributes</jc>
 * 	Col <jv>complex</jv> = <jsm>col</jsm>()
 * 		.span(2)
 * 		.class_(<js>"data-column"</js>)
 * 		.style(<js>"width: 150px; text-align: right;"</js>)
 * 		.title(<js>"Numeric data column"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#col() col()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "col")
public class Col extends HtmlElementVoid<Col> {

	/**
	 * Creates an empty {@link Col} element.
	 */
	public Col() {}

	/**
	 * Creates a {@link Col} element with the specified {@link Col#span(Object)} attribute.
	 *
	 * @param span The {@link Col#span(Object)} attribute. Can be <jk>null</jk> to unset the attribute.
	 */
	public Col(Number span) {
		span(span);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-col-span">span</a> attribute.
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
	public Col span(Object value) {
		attr("span", value);
		return this;
	}

}