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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-tfoot-element">&lt;tfoot&gt;</a>
 * element.
 *
 * <p>
 * The tfoot element represents a group of rows that consist of the column summaries (footers) for
 * the parent table element. It is used to group footer rows of a table, separating them from the
 * header (thead) and body (tbody) sections. The tfoot element can contain multiple tr elements
 * and is typically used to display summary information, totals, or other footer content.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table footer with totals</jc>
 * 	Tfoot <jv>simple</jv> = <jsm>tfoot</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"Total"</js>),
 * 			<jsm>td</jsm>(<js>"$1,000"</js>),
 * 			<jsm>td</jsm>(<js>"$2,000"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Table footer with styling</jc>
 * 	Tfoot <jv>styled</jv> = <jsm>tfoot</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"Grand Total"</js>),
 * 			<jsm>td</jsm>(<js>"$3,000"</js>)
 * 		)
 * 	).class_(<js>"table-footer"</js>);
 *
 * 	<jc>// Table footer with multiple rows</jc>
 * 	Tfoot <jv>multiple</jv> = <jsm>tfoot</jsm>(
 * 		<jsm>tr</jsm>(
 * 			<jsm>td</jsm>(<js>"Subtotal"</js>),
 * 			<jsm>td</jsm>(<js>"$500"</js>)
 * 		),
 * 		<jsm>tr</jsm>(
 * 				.children(
 * 					new Td().children("Tax"),
 * 					new Td().children("$50")
 * 				),
 * 			new Tr()
 * 				.children(
 * 					new Td().children("Total"),
 * 					new Td().children("$550")
 * 				)
 * 		);
 *
 * 	// Table footer with summary information
 * 	Tfoot summary = new Tfoot()
 * 		.children(
 * 			new Tr()
 * 				.children(
 * 					new Td().colspan(3).children("Summary: 10 items, 3 categories")
 * 				)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "tfoot")
public class Tfoot extends HtmlElementContainer<Tfoot> {

	/**
	 * Creates an empty {@link Tfoot} element.
	 */
	public Tfoot() {}

	/**
	 * Creates a {@link Tfoot} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Tfoot(Object...children) {
		children(children);
	}

}