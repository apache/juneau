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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-data-element">&lt;data&gt;</a>
 * element.
 *
 * <p>
 * The data element represents its contents, along with a machine-readable form of those contents in
 * the value attribute. It is used to provide both human-readable and machine-readable versions of
 * the same data, making it easier for scripts and other automated systems to process the information
 * while still displaying meaningful content to users. The value attribute should contain the
 * machine-readable version of the data.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple data element</jc>
 * 	Data <jv>simple</jv> = <jsm>data</jsm>(<js>"12345"</js>, <js>"Product #12345"</js>);
 *
 * 	<jc>// Data with styling</jc>
 * 	Data <jv>styled</jv> = <jsm>data</jsm>(<js>"USD"</js>, <js>"US Dollar"</js>)
 * 		.class_(<js>"currency"</js>);
 *
 * 	<jc>// Data in a sentence</jc>
 * 	P <jv>sentence</jv> = <jsm>p</jsm>(
 * 		<js>"Price: "</js>,
 * 		<jsm>data</jsm>(<js>"29.99"</js>, <js>"$29.99"</js>),
 * 		<js>" per item"</js>
 * 	);
 *
 * 	<jc>// Data with complex content</jc>
 * 	Data <jv>complex</jv> = <jsm>data</jsm>(<js>"2024-01-15"</js>,
 * 		<js>"January 15, 2024"</js>,
 * 		<jsm>span</jsm>().class_(<js>"date"</js>).children(<js>" (Monday)"</js>)
 * 	);
 *
 * 	<jc>// Data with multiple attributes</jc>
 * 	Data <jv>multiple</jv> = <jsm>data</jsm>(<js>"SKU-12345"</js>, <js>"SKU: 12345"</js>)
 * 		.class_(<js>"product-sku"</js>)
 * 		.title(<js>"Product SKU"</js>);
 *
 * 	<jc>// Data with ID</jc>
 * 	Data <jv>withId</jv> = <jsm>data</jsm>(<js>"user-123"</js>, <js>"User ID: 123"</js>)
 * 		.id(<js>"user-id"</js>)
 * 		.children(<js>"User ID: 123"</js>);
 *
 * 	<jc>// Data with styling</jc>
 * 	Data <jv>styled2</jv> = <jsm>data</jsm>(<js>"active"</js>, <js>"Active"</js>)
 * 		.style(<js>"color: green; font-weight: bold;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#data() data()}
 * 		<li class='jm'>{@link HtmlBuilder#data(String, Object) data(String, Object)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "data")
public class Data extends HtmlElementMixed<Data> {

	/**
	 * Creates an empty {@link Data} element.
	 */
	public Data() {}

	/**
	 * Creates a {@link Data} element with the specified {@link Data#value(Object)} attribute and child node.
	 *
	 * @param value The {@link Data#value(Object)} attribute. Can be <jk>null</jk>.
	 * @param child The child node. Can be <jk>null</jk>.
	 */
	public Data(String value, Object child) {
		value(value).child(child);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#attr-data-value">value</a>
	 * attribute.
	 *
	 * <p>
	 * Machine-readable value.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Data value(Object value) {
		attr("value", value);
		return this;
	}
}