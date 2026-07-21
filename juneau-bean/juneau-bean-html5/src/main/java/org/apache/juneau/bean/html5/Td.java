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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-td-element">&lt;td&gt;</a>
 * element.
 *
 * <p>
 * The td element represents a data cell in a table. It is used to contain the actual data content
 * of a table row, as opposed to header cells (th) which contain column or row headers. The td
 * element can contain any flow content and supports attributes for spanning multiple columns or
 * rows, as well as associating with header cells for accessibility.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple data cell</jc>
 * 	Td <jv>simple</jv> = <jsm>td</jsm>(<js>"John Doe"</js>);
 *
 * 	<jc>// Data cell with styling</jc>
 * 	Td <jv>styled</jv> = <jsm>td</jsm>(<js>"Important Data"</js>)
 * 		.class_(<js>"highlight"</js>);
 *
 * 	<jc>// Data cell spanning multiple columns</jc>
 * 	Td <jv>colspan</jv> = <jsm>td</jsm>(<js>"Spans 2 columns"</js>)
 * 		.colspan(2);
 *
 * 	<jc>// Data cell spanning multiple rows</jc>
 * 	Td <jv>rowspan</jv> = <jsm>td</jsm>(<js>"Spans 3 rows"</js>)
 * 		.rowspan(3);
 *
 * 	<jc>// Data cell with headers association</jc>
 * 	Td <jv>headers</jv> = <jsm>td</jsm>(<js>"25"</js>)
 * 		.headers(<js>"name-header age-header"</js>);
 *
 * 	<jc>// Data cell with complex content</jc>
 * 	Td <jv>complex</jv> = <jsm>td</jsm>(
 * 		<jsm>strong</jsm>(<js>"Bold text"</js>),
 * 		<js>" and "</js>,
 * 		<jsm>em</jsm>(<js>"italic text"</js>)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "td")
public class Td extends HtmlElementMixed<Td> {

	/**
	 * Creates an empty {@link Td} element.
	 */
	public Td() {}

	/**
	 * Creates a {@link Td} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Td(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-colspan">colspan</a> attribute.
	 *
	 * <p>
	 * Number of columns that the cell is to span.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Td colspan(Object value) {
		attr("colspan", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-headers">headers</a> attribute.
	 *
	 * <p>
	 * Specifies the IDs of header cells that apply to this table cell. This creates a programmatic
	 * relationship between the cell and its headers for accessibility purposes.
	 *
	 * <p>
	 * Multiple IDs can be specified as a space-separated list.
	 *
	 * @param value The IDs of header cells that apply to this cell. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Td headers(String value) {
		attr("headers", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-tdth-rowspan">rowspan</a> attribute.
	 *
	 * <p>
	 * Number of rows that the cell is to span.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Td rowspan(Object value) {
		attr("rowspan", value);
		return this;
	}
}