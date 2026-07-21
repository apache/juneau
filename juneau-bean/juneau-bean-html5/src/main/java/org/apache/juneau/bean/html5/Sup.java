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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-sub-and-sup-elements">&lt;sup&gt;</a>
 * element.
 *
 * <p>
 * The sup element represents superscript text. It is used to display text that should be rendered
 * above the baseline, typically in a smaller font size. The sup element is commonly used for
 * mathematical formulas, footnotes, ordinal numbers, and other annotations that need to be
 * displayed as superscript.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Mathematical formula</jc>
 * 	Sup <jv>math</jv> = <jsm>sup</jsm>(
 * 		<js>"x"</js>, <jsm>sup</jsm>(<js>"2"</js>),
 * 		<js>" + y"</js>, <jsm>sup</jsm>(<js>"2"</js>),
 * 		<js>" = z"</js>, <jsm>sup</jsm>(<js>"2"</js>)
 * 	);
 *
 * 	<jc>// Ordinal numbers</jc>
 * 	Sup <jv>ordinal</jv> = <jsm>sup</jsm>(<js>"1st"</js>, <jsm>sup</jsm>(<js>"st"</js>));
 *
 * 	<jc>// Footnote reference</jc>
 * 	Sup <jv>footnote</jv> = <jsm>sup</jsm>(<js>"1"</js>);
 *
 * 	<jc>// Superscript with styling</jc>
 * 	Sup <jv>styled</jv> = <jsm>sup</jsm>(<js>"n"</js>)
 * 		.class_(<js>"superscript"</js>);
 *
 * 	<jc>// Multiple superscripts</jc>
 * 	Sup <jv>multiple</jv> = <jsm>sup</jsm>(
 * 		<js>"A"</js>, <jsm>sup</jsm>(<js>"i,j"</js>),
 * 		<js>" = B"</js>, <jsm>sup</jsm>(<js>"k"</js>)
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "sup")
public class Sup extends HtmlElementMixed<Sup> {

	/**
	 * Creates an empty {@link Sup} element.
	 */
	public Sup() {}

	/**
	 * Creates a {@link Sup} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Sup(Object...children) {
		children(children);
	}
}