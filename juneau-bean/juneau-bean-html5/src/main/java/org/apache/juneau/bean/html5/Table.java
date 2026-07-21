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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-table-element">&lt;table&gt;</a>
 * element.
 *
 * <p>
 * The table element represents data with more than one dimension, in the form of a table. It contains
 * rows and columns of data, with optional headers and footers. The table element is used to display
 * tabular data in a structured format, making it easy to read and understand. It can contain
 * caption, colgroup, thead, tbody, tfoot, and tr elements to organize the table structure.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table with headers and data</jc>
 * 	Table <jv>simple</jv> = <jsm>table</jsm>(
 * 		<jsm>thead</jsm>(
 * 			<jsm>tr</jsm>(
 * 				<jsm>th</jsm>(<js>"Name"</js>),
 * 				<jsm>th</jsm>(<js>"Age"</js>),
 * 				<jsm>th</jsm>(<js>"City"</js>)
 * 			)
 * 		),
 * 		<jsm>tbody</jsm>(
 * 			<jsm>tr</jsm>(
 * 				<jsm>td</jsm>(<js>"John"</js>),
 * 				<jsm>td</jsm>(<js>"25"</js>),
 * 				<jsm>td</jsm>(<js>"New York"</js>)
 * 			)
 * 		)
 * 	);
 *
 * 	<jc>// Table with caption and styling</jc>
 * 	Table <jv>styled</jv> = <jsm>table</jsm>(
 * 		<jsm>caption</jsm>(<js>"Employee Information"</js>),
 * 		<jsm>thead</jsm>(
 * 			<jsm>tr</jsm>(
 * 				<jsm>th</jsm>(<js>"ID"</js>),
 * 				<jsm>th</jsm>(<js>"Name"</js>),
 * 				<jsm>th</jsm>(<js>"Department"</js>)
 * 			)
 * 		)
 * 	).class_(<js>"data-table"</js>).border(1);
 *
 * 	<jc>// Table with multiple sections</jc>
 * 	Table complex = new Table()
 * 		.children(
 * 			new Caption().children("Sales Report"),
 * 			new Thead()
 * 				.children(
 * 					new Tr()
 * 						.children(
 * 							new Th().children("Product"),
 * 							new Th().children("Q1"),
 * 							new Th().children("Q2")
 * 						)
 * 				),
 * 			new Tbody()
 * 				.children(
 * 					new Tr()
 * 						.children(
 * 							new Td().children("Widget A"),
 * 							new Td().children("100"),
 * 							new Td().children("150")
 * 						)
 * 				),
 * 			new Tfoot()
 * 				.children(
 * 					new Tr()
 * 						.children(
 * 							new Td().children("Total"),
 * 							new Td().children("100"),
 * 							new Td().children("150")
 * 						)
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "table")
public class Table extends HtmlElementContainer<Table> {

	/**
	 * Creates an empty {@link Table} element.
	 */
	public Table() {}

	/**
	 * Creates a {@link Table} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Table(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#attr-table-border">border</a> attribute.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Table border(Object value) {
		attr("border", value);
		return this;
	}

}