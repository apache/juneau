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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/tabular-data.html#the-tr-element">&lt;tr&gt;</a>
 * element.
 *
 * <p>
 * The tr element represents a row of cells in a table. It is used to group table cells (td and th elements)
 * into horizontal rows. The tr element can contain multiple td (data cell) or th (header cell) elements,
 * and is typically used within thead, tbody, or tfoot elements to organize table structure.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple table row with data cells</jc>
 * 	Tr <jv>simple</jv> = <jsm>tr</jsm>(
 * 		<jsm>td</jsm>(<js>"John"</js>),
 * 		<jsm>td</jsm>(<js>"25"</js>),
 * 		<jsm>td</jsm>(<js>"New York"</js>)
 * 	);
 *
 * 	<jc>// Table row with header cells</jc>
 * 	Tr <jv>header</jv> = <jsm>tr</jsm>(
 * 		<jsm>th</jsm>(<js>"Name"</js>),
 * 		<jsm>th</jsm>(<js>"Age"</js>),
 * 		<jsm>th</jsm>(<js>"City"</js>)
 * 	);
 *
 * 	<jc>// Table row with styling</jc>
 * 	Tr <jv>styled</jv> = <jsm>tr</jsm>(
 * 		<jsm>td</jsm>(<js>"Product A"</js>),
 * 		<jsm>td</jsm>(<js>"100"</js>),
 * 		<jsm>td</jsm>(<js>"$10.00"</js>)
 * 	).class_(<js>"highlight-row"</js>);
 *
 * 	<jc>// Table row with click handler</jc>
 * 	Tr <jv>clickable</jv> = <jsm>tr</jsm>(
 * 		<jsm>td</jsm>(<js>"Clickable Row"</js>)
 * 	).onclick(<js>"selectRow(this)"</js>);
 *
 * 	<jc>// Table row with mixed cell types</jc>
 * 	Tr <jv>mixed</jv> = <jsm>tr</jsm>(
 * 		<jsm>th</jsm>(<js>"Total"</js>),
 * 		<jsm>td</jsm>(<js>"$1,000"</js>),
 * 		<jsm>td</jsm>(<js>"$2,000"</js>)
 * 	);
 *
 * 	<jc>// Table row with complex content</jc>
 * 	Tr <jv>complex</jv> = <jsm>tr</jsm>(
 * 		<jsm>td</jsm>(
 * 			<jsm>strong</jsm>(<js>"Important"</js>),
 * 			<js>" data"</js>
 * 		),
 * 		<jsm>td</jsm>(<js>"Value"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#tr() tr()}
 * 		<li class='jm'>{@link HtmlBuilder#tr(Object...) tr(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "tr")
public class Tr extends HtmlElementContainer<Tr> {

	/**
	 * Creates an empty {@link Tr} element.
	 */
	public Tr() {}

	/**
	 * Creates a {@link Tr} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Tr(Object...children) {
		children(children);
	}

}