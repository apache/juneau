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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-datalist-element">&lt;datalist&gt;</a>
 * element.
 *
 * <p>
 * The datalist element represents a set of option elements that represent predefined options for other
 * controls. It is used to provide a list of suggested values for input elements, allowing users to
 * either select from the predefined options or enter their own custom value. The datalist element
 * is typically associated with an input element through the list attribute, and the options within
 * the datalist provide autocomplete suggestions.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple datalist</jc>
 * 	Datalist <jv>simple</jv> = <jsm>datalist</jsm>(<js>"colors"</js>,
 * 		<jsm>option</jsm>(<js>"red"</js>, <js>"Red"</js>),
 * 		<jsm>option</jsm>(<js>"green"</js>, <js>"Green"</js>),
 * 		<jsm>option</jsm>(<js>"blue"</js>, <js>"Blue"</js>)
 * 	);
 *
 * 	<jc>// Datalist with styling</jc>
 * 	Datalist <jv>styled</jv> = <jsm>datalist</jsm>(<js>"countries"</js>,
 * 		<jsm>option</jsm>(<js>"us"</js>, <js>"United States"</js>),
 * 		<jsm>option</jsm>(<js>"ca"</js>, <js>"Canada"</js>),
 * 		<jsm>option</jsm>(<js>"mx"</js>, <js>"Mexico"</js>)
 * 	).class_(<js>"country-list"</js>);
 *
 * 	<jc>// Datalist with multiple options</jc>
 * 	Datalist <jv>multiple</jv> = <jsm>datalist</jsm>(<js>"fruits"</js>,
 * 		<jsm>option</jsm>(<js>"apple"</js>, <js>"Apple"</js>),
 * 		<jsm>option</jsm>(<js>"banana"</js>, <js>"Banana"</js>),
 * 		<jsm>option</jsm>(<js>"cherry"</js>, <js>"Cherry"</js>),
 * 		<jsm>option</jsm>(<js>"date"</js>, <js>"Date"</js>),
 * 		<jsm>option</jsm>(<js>"elderberry"</js>, <js>"Elderberry"</js>)
 * 	);
 *
 * 	<jc>// Datalist with complex options</jc>
 * 	Datalist <jv>complex</jv> = <jsm>datalist</jsm>(<js>"products"</js>,
 * 		<jsm>option</jsm>(<js>"laptop-001"</js>, <js>"Laptop Pro 15\" - $1,299"</js>),
 * 		<jsm>option</jsm>(<js>"laptop-002"</js>, <js>"Laptop Air 13\" - $999"</js>),
 * 		<jsm>option</jsm>(<js>"tablet-001"</js>, <js>"Tablet 10\" - $499"</js>)
 * 	);
 *
 * 	<jc>// Datalist with ID</jc>
 * 	Datalist <jv>withId</jv> = <jsm>datalist</jsm>(<js>"cities"</js>,
 * 		<jsm>option</jsm>(<js>"new-york"</js>, <js>"New York"</js>),
 * 		<jsm>option</jsm>(<js>"los-angeles"</js>, <js>"Los Angeles"</js>),
 * 		<jsm>option</jsm>(<js>"chicago"</js>, <js>"Chicago"</js>)
 * 	);
 *
 * 	<jc>// Datalist with styling</jc>
 * 	Datalist <jv>styled2</jv> = <jsm>datalist</jsm>(<js>"sizes"</js>,
 * 		<jsm>option</jsm>(<js>"xs"</js>, <js>"Extra Small"</js>),
 * 		<jsm>option</jsm>(<js>"s"</js>, <js>"Small"</js>),
 * 		<jsm>option</jsm>(<js>"m"</js>, <js>"Medium"</js>),
 * 		<jsm>option</jsm>(<js>"l"</js>, <js>"Large"</js>),
 * 		<jsm>option</jsm>(<js>"xl"</js>, <js>"Extra Large"</js>)
 * 	).style(<js>"display: none;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#datalist() datalist()}
 * 		<li class='jm'>{@link HtmlBuilder#datalist(String, Option...) datalist(String, Option...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "datalist")
public class Datalist extends HtmlElementContainer<Datalist> {

	/**
	 * Creates an empty {@link Datalist} element.
	 */
	public Datalist() {}

	/**
	 * Creates a {@link Datalist} element with the specified {@link Datalist#id(String)} attribute and child nodes.
	 *
	 * @param id The {@link Datalist#id(String)} attribute.
	 * @param children The child nodes.
	 */
	public Datalist(String id, Option...children) {
		id(id).children((Object[])children);
	}

}