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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-th-element">&lt;th&gt;</a>
 * element.
 *
 * <p>
 * The th element represents a header cell in a table. It is used to contain header information
 * for a column or row, providing context and meaning to the data cells (td) in the table. The th
 * element supports various attributes for accessibility, spanning multiple columns or rows, and
 * defining the relationship between header and data cells.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple header cell</jc>
 * 	Th <jv>simple</jv> = <jsm>th</jsm>(<js>"Name"</js>);
 *
 * 	<jc>// Header cell with scope</jc>
 * 	Th <jv>scoped</jv> = <jsm>th</jsm>(<js>"Age"</js>)
 * 		.scope(<js>"col"</js>);
 *
 * 	<jc>// Header cell spanning multiple columns</jc>
 * 	Th <jv>colspan</jv> = <jsm>th</jsm>(<js>"Contact Information"</js>)
 * 		.colspan(2);
 *
 * 	<jc>// Header cell with abbreviation</jc>
 * 	Th <jv>abbreviated</jv> = <jsm>th</jsm>(<js>"Quantity"</js>)
 * 		.abbr(<js>"Qty"</js>);
 *
 * 	<jc>// Header cell with sorting</jc>
 * 	Th <jv>sorted</jv> = <jsm>th</jsm>(<js>"Price"</js>)
 * 		.sorted(<js>"asc"</js>);
 *
 * 	<jc>// Header cell with styling</jc>
 * 	Th <jv>styled</jv> = <jsm>th</jsm>(<js>"Status"</js>)
 * 		.class_(<js>"header-cell"</js>);
 *
 * 	<jc>// Header cell with complex content</jc>
 * 	Th <jv>complex</jv> = <jsm>th</jsm>(
 * 		.children(
 * 			new Strong().children("Total"),
 * 			" ",
 * 			new Small().children("(USD)")
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "th")
public class Th extends HtmlElementMixed<Th> {

	/**
	 * Creates an empty {@link Th} element.
	 */
	public Th() {}

	/**
	 * Creates a {@link Th} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Th(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-abbr">abbr</a> attribute.
	 *
	 * <p>
	 * Specifies an alternative, abbreviated label for the header cell. This is used by screen readers
	 * and other assistive technologies when referencing the cell in other contexts.
	 *
	 * <p>
	 * The abbreviation should be shorter than the full header text but still meaningful.
	 *
	 * @param value The abbreviated label for the header cell. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Th abbr(String value) {
		attr("abbr", value);
		return this;
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
	public Th colspan(Object value) {
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
	public Th headers(String value) {
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
	public Th rowspan(Object value) {
		attr("rowspan", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-scope">scope</a> attribute.
	 *
	 * <p>
	 * Specifies which cells the header cell applies to. This helps define the relationship
	 * between header and data cells for accessibility.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 *  	<li><js>"row"</js> - Header applies to all cells in the same row</li>
	 *  	<li><js>"col"</js> - Header applies to all cells in the same column</li>
	 *  	<li><js>"rowgroup"</js> - Header applies to all cells in the same row group</li>
	 *  	<li><js>"colgroup"</js> - Header applies to all cells in the same column group</li>
	 * </ul>
	 *
	 * @param value Which cells the header cell applies to. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Th scope(String value) {
		attr("scope", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-th-sorted">sorted</a> attribute.
	 *
	 * <p>
	 * Specifies the sort direction and ordinality of the column. This indicates how the table
	 * is currently sorted and which column is the primary sort key.
	 *
	 * <p>
	 * Common values:
	 * <ul>
	 *  	<li><js>"asc"</js> - Column is sorted in ascending order</li>
	 *  	<li><js>"desc"</js> - Column is sorted in descending order</li>
	 *  	<li><js>"asc 1"</js> - Column is the primary sort key in ascending order</li>
	 *  	<li><js>"desc 1"</js> - Column is the primary sort key in descending order</li>
	 * </ul>
	 *
	 * @param value The sort direction and ordinality of the column. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Th sorted(String value) {
		attr("sorted", value);
		return this;
	}
}