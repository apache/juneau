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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-rp-element">&lt;rp&gt;</a>
 * element.
 *
 * <p>
 * The rp element represents a fallback parenthesis for browsers that do not support ruby annotations.
 * It is used within a ruby element to provide fallback text that will be displayed when ruby
 * annotations are not supported. The rp element is typically used to wrap parentheses around
 * ruby text (rt elements) to provide a fallback display format. It is part of the ruby annotation
 * system and helps ensure that ruby annotations are displayed correctly across different browsers.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple ruby parenthesis</jc>
 * 	Rp <jv>simple</jv> = <jsm>rp</jsm>(<js>"("</js>);
 *
 * 	<jc>// Rp with styling</jc>
 * 	Rp <jv>styled</jv> = <jsm>rp</jsm>(<js>")"</js>)
 * 		.class_(<js>"ruby-paren"</js>);
 *
 * 	<jc>// Rp with complex content</jc>
 * 	Rp <jv>complex</jv> = <jsm>rp</jsm>(
 * 		<js>"("</js>,
 * 		<jsm>strong</jsm>(<js>"注"</js>),
 * 		<js>")"</js>
 * 	);
 *
 * 	<jc>// Rp with ID</jc>
 * 	Rp <jv>withId</jv> = <jsm>rp</jsm>(<js>"("</js>)
 * 		.id(<js>"ruby-paren-1"</js>);
 *
 * 	<jc>// Rp with styling</jc>
 * 	Rp <jv>styled2</jv> = <jsm>rp</jsm>(<js>")"</js>)
 * 		.style(<js>"color: #999; font-size: 0.8em;"</js>);
 *
 * 	<jc>// Rp with multiple elements</jc>
 * 	Rp <jv>multiple</jv> = <jsm>rp</jsm>(
 * 		<js>"("</js>,
 * 		<jsm>rp</jsm>(<js>"注"</js>),
 * 		<js>")"</js>
 * 	);
 *
 * 	// Rp with links
 * 	Rp withLinks = new Rp()
 * 		.children(
 * 			"(",
 * 			new A().href("/help/ruby").children("注"),
 * 			")"
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "rp")
public class Rp extends HtmlElementMixed<Rp> {

	/**
	 * Creates an empty {@link Rp} element.
	 */
	public Rp() {}

	/**
	 * Creates a {@link Rp} element with the specified {@link Rp#children(Object[])} nodes.
	 *
	 * @param children The {@link Rp#children(Object[])} nodes. Must not be <jk>null</jk>.
	 */
	public Rp(Object...children) {
		children(children);
	}
}