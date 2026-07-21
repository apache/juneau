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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-thead-element">&lt;thead&gt;</a>
 * element.
 *
 * <p>
 * The thead element represents a group of rows that consist of the column labels (headers) for
 * the parent table element. It is used to group header rows of a table, separating them from the
 * body (tbody) and footer (tfoot) sections. The thead element can contain multiple tr elements
 * and is typically used to display column headers and other header information.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table header</jc>
 * 	Thead <jv>simple</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Name"</js>),
 * 			<jsm>th</jsm>(<js>"Age"</js>),
 * 			<jsm>th</jsm>(<js>"City"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Table header with styling</jc>
 * 	Thead <jv>styled</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Product"</js>),
 * 			<jsm>th</jsm>(<js>"Price"</js>),
 * 			<jsm>th</jsm>(<js>"Stock"</js>)
 * 		)
 * 	).class_(<js>"table-header"</js>);
 *
 * 	<jc>// Table header with multiple rows</jc>
 * 	Thead <jv>multiple</jv> = <jsm>thead</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>th</jsm>(<js>"Contact Information"</js>).colspan(2),
 * 			<jsm>th</jsm>(<js>"Address"</js>).colspan(2)
 * 		),
 * 			new Tr()
 * 				.children(
 * 					new Th().children("Name"),
 * 					new Th().children("Phone"),
 * 					new Th().children("Street"),
 * 					new Th().children("City")
 * 				)
 * 		);
 *
 * 	// Table header with sorting
 * 	Thead sortable = new Thead()
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Th().sorted("asc").children("Name"),
 * 					new Th().sorted("desc").children("Date"),
 * 					new Th().children("Status")
 * 				)
 * 		);
 *
 * 	// Table header with accessibility
 * 	Thead accessible = new Thead()
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Th().scope("col").children("ID"),
 * 					new Th().scope("col").children("Description"),
 * 					new Th().scope("col").children("Amount")
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "thead")
public class Thead extends HtmlElementContainer<Thead> {

	/**
	 * Creates an empty {@link Thead} element.
	 */
	public Thead() {}

	/**
	 * Creates a {@link Thead} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Thead(Object...children) {
		children(children);
	}

}