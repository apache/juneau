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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-tbody-element">&lt;tbody&gt;</a>
 * element.
 *
 * <p>
 * The tbody element represents a group of rows that consist of a body of data for the parent table
 * element. It is used to group the main content rows of a table, separating them from the header
 * (thead) and footer (tfoot) sections. The tbody element can contain multiple tr elements and is
 * typically used to organize table data for styling and scripting purposes.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table body with data rows</jc>
 * 	Tbody <jv>simple</jv> = <jsm>tbody</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"John"</js>),
 * 			<jsm>td</jsm>(<js>"25"</js>),
 * 			<jsm>td</jsm>(<js>"New York"</js>)
 * 		),
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"Jane"</js>),
 * 			<jsm>td</jsm>(<js>"30"</js>),
 * 			<jsm>td</jsm>(<js>"Los Angeles"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Table body with styling</jc>
 * 	Tbody <jv>styled</jv> = <jsm>tbody</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"Product A"</js>),
 * 			<jsm>td</jsm>(<js>"100"</js>),
 * 			<jsm>td</jsm>(<js>"$10.00"</js>)
 * 		)
 * 	).class_(<js>"data-rows"</js>);
 *
 * 	<jc>// Table body with multiple rows</jc>
 * 	Tbody <jv>multiple</jv> = <jsm>tbody</jsm>(
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Td().children("Row 1, Col 1"),
 * 					new Td().children("Row 1, Col 2")
 * 				),
 * 			new Tr()
 * 				.children(
 * 					new Td().children("Row 2, Col 1"),
 * 					new Td().children("Row 2, Col 2")
 * 				)
 * 		);
 *
 * 	// Table body with event handlers
 * 	Tbody interactive = new Tbody()
 * 		.onclick("handleRowClick(event)")
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Td().children("Clickable Row")
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "tbody")
public class Tbody extends HtmlElementContainer<Tbody> {

	/**
	 * Creates an empty {@link Tbody} element.
	 */
	public Tbody() {}

	/**
	 * Creates a {@link Tbody} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Tbody(Object...children) {
		children(children);
	}

}