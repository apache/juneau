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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-sub-and-sup-elements">&lt;sub&gt;</a>
 * element.
 *
 * <p>
 * The sub element represents subscript text. It is used to display text that should be rendered
 * below the baseline, typically in a smaller font size. The sub element is commonly used for
 * mathematical formulas, chemical formulas, footnotes, and other annotations that need to be
 * displayed as subscript.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Chemical formula</jc>
 * 	Sub <jv>chemical</jv> = <jsm>sub</jsm>(<js>"H"</js>, <jsm>sub</jsm>(<js>"2"</js>), <js>"O"</js>);
 *
 * 	<jc>// Mathematical formula</jc>
 * 	Sub <jv>math</jv> = <jsm>sub</jsm>(
 * 		<js>"x"</js>, <jsm>sub</jsm>(<js>"2"</js>),
 * 		<js>" + y"</js>, <jsm>sub</jsm>(<js>"2"</js>),
 * 		<js>" = z"</js>, <jsm>sub</jsm>(<js>"2"</js>)
 * 	);
 *
 * 	<jc>// Footnote reference</jc>
 * 	Sub <jv>footnote</jv> = <jsm>sub</jsm>(<js>"1"</js>);
 *
 * 	<jc>// Subscript with styling</jc>
 * 	Sub <jv>styled</jv> = <jsm>sub</jsm>(<js>"n"</js>)
 * 		.class_(<js>"subscript"</js>);
 *
 * 	<jc>// Multiple subscripts</jc>
 * 	Sub <jv>multiple</jv> = <jsm>sub</jsm>(
 * 		<js>"A"</js>, <jsm>sub</jsm>(<js>"i,j"</js>),
 * 		<js>" = B"</js>, <jsm>sub</jsm>(<js>"k"</js>)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "sub")
public class Sub extends HtmlElementMixed<Sub> {

	/**
	 * Creates an empty {@link Sub} element.
	 */
	public Sub() {}

	/**
	 * Creates a {@link Sub} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Sub(Object...children) {
		children(children);
	}
}