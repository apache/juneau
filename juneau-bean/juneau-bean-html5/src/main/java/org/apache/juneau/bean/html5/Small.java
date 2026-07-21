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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-small-element">&lt;small&gt;</a>
 * element.
 *
 * <p>
 * The small element represents side comments such as small print. It is typically used for
 * disclaimers, caveats, legal restrictions, or copyrights. The small element does not "de-emphasize"
 * or lower the importance of text; it represents side comments.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Copyright notice</jc>
 * 	Small <jv>copyright</jv> = <jsm>small</jsm>(<js>"Copyright 2024 My Company. All rights reserved."</js>);
 *
 * 	<jc>// Legal disclaimer</jc>
 * 	Small <jv>disclaimer</jv> = <jsm>small</jsm>(<js>"This information is provided as-is without warranty."</js>);
 *
 * 	<jc>// Terms and conditions</jc>
 * 	Small <jv>terms</jv> = <jsm>small</jsm>(<js>"By using this service, you agree to our terms and conditions."</js>);
 *
 * 	<jc>// Price with small print</jc>
 * 	Small <jv>price</jv> = <jsm>small</jsm>(
 * 		<js>"$99.99"</js>,
 * 		<jsm>small</jsm>(<js>"*plus tax and shipping"</js>)
 * 	);
 *
 * 	<jc>// Form with small print</jc>
 * 	Small <jv>formNote</jv> = <jsm>small</jsm>(<js>"Required fields are marked with an asterisk (*)"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "small")
public class Small extends HtmlElementMixed<Small> {

	/**
	 * Creates an empty {@link Small} element.
	 */
	public Small() {}

	/**
	 * Creates a {@link Small} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Small(Object...children) {
		children(children);
	}
}