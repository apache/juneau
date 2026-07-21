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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-span-element">&lt;span&gt;</a>
 * element.
 *
 * <p>
 * The span element is a generic inline container for phrasing content. It has no inherent meaning
 * and is typically used to group inline elements for styling purposes or to apply attributes to
 * a portion of text. The span element is commonly used with CSS to apply styles to specific
 * portions of text or to mark up text for JavaScript manipulation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Text with highlighted portion</jc>
 * 	Span <jv>highlight</jv> = <jsm>span</jsm>(<js>"This is highlighted text"</js>)
 * 		.class_(<js>"highlight"</js>);
 *
 * 	<jc>// Text with multiple styled portions</jc>
 * 	Span <jv>styled</jv> = <jsm>span</jsm>(
 * 		<js>"Normal text "</js>,
 * 		<jsm>span</jsm>(<js>"bold text"</js>).class_(<js>"bold"</js>),
 * 		<js>" and "</js>,
 * 		<jsm>span</jsm>(<js>"italic text"</js>).class_(<js>"italic"</js>)
 * 	);
 *
 * 	<jc>// Text with clickable portion</jc>
 * 	Span <jv>clickable</jv> = <jsm>span</jsm>(
 * 		<js>"Click "</js>,
 * 		<jsm>span</jsm>(<js>"here"</js>).class_(<js>"link"</js>).onclick(<js>"showDetails()"</js>),
 * 		<js>" for more information"</js>
 * 	);
 *
 * 	<jc>// Text with tooltip</jc>
 * 	Span <jv>tooltip</jv> = <jsm>span</jsm>(<js>"Hover over this text"</js>)
 * 		.title(<js>"This is a tooltip"</js>);
 *
 * 	<jc>// Text with language specification</jc>
 * 	Span <jv>lang</jv> = <jsm>span</jsm>(<js>"Hola mundo"</js>)
 * 		.lang(<js>"es"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "span")
public class Span extends HtmlElementMixed<Span> {

	/**
	 * Creates an empty {@link Span} element.
	 */
	public Span() {}

	/**
	 * Creates a {@link Span} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Span(Object...children) {
		children(children);
	}
}