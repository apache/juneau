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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/grouping-content.html#the-hr-element">&lt;hr&gt;</a>
 * element.
 *
 * <p>
 * The hr element represents a paragraph-level thematic break, such as a scene change in a story,
 * or a transition to another topic within a section of a reference book. It is used to create
 * a horizontal rule or line that visually separates content sections. The hr element is a void
 * element that does not contain any content and is typically rendered as a horizontal line
 * across the page.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple horizontal rule</jc>
 * 	Hr <jv>simple</jv> = <jsm>hr</jsm>();
 *
 * 	<jc>// HR with styling</jc>
 * 	Hr <jv>styled</jv> = <jsm>hr</jsm>()
 * 		.class_(<js>"section-divider"</js>)
 * 		.style(<js>"border: 2px solid #ccc; margin: 20px 0;"</js>);
 *
 * 	<jc>// HR with ID</jc>
 * 	Hr <jv>withId</jv> = <jsm>hr</jsm>()
 * 		.id(<js>"content-break"</js>);
 *
 * 	<jc>// HR with styling</jc>
 * 	Hr <jv>styled2</jv> = <jsm>hr</jsm>()
 * 		.style(<js>"border: none; border-top: 1px dashed #999; margin: 30px 0;"</js>);
 *
 * 	<jc>// HR with multiple attributes</jc>
 * 	Hr <jv>complex</jv> = <jsm>hr</jsm>()
 * 		.class_(<js>"fancy-divider"</js>)
 * 		.style(<js>"border: none; height: 3px; background: linear-gradient(to right, #ff6b6b, #4ecdc4); margin: 40px 0;"</js>);
 *
 * 	<jc>// HR with accessibility</jc>
 * 	Hr <jv>accessible</jv> = <jsm>hr</jsm>()
 * 		.title(<js>"Section break"</js>)
 * 		.style(<js>"border: 1px solid #ddd; margin: 25px 0;"</js>);
 *
 * 	<jc>// HR with custom styling</jc>
 * 	Hr <jv>custom</jv> = <jsm>hr</jsm>()
 * 		.class_(<js>"custom-hr"</js>)
 * 		.style(<js>"border: none; height: 1px; background-color: #e0e0e0; margin: 15px 0;"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#hr() hr()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "hr")
public class Hr extends HtmlElementVoid<Hr> {

	/**
	 * Creates an empty {@link Hr} element.
	 */
	public Hr() { /* Empty constructor. */ }

}