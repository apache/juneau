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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-legend-element">&lt;legend&gt;</a>
 * element.
 *
 * <p>
 * The legend element represents a caption for the content of its parent fieldset element.
 * It is used to provide a title or description for a group of form controls that are
 * contained within a fieldset. The legend element should be placed as the first child
 * of a fieldset element and is typically rendered above or to the side of the fieldset
 * content, often with special styling to distinguish it from the form controls.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple legend</jc>
 * 	Legend <jv>simple</jv> = <jsm>legend</jsm>(<js>"Personal Information"</js>);
 *
 * 	<jc>// Legend with styling</jc>
 * 	Legend <jv>styled</jv> = <jsm>legend</jsm>(<js>"Contact Details"</js>)
 * 		.class_(<js>"form-legend"</js>);
 *
 * 	<jc>// Legend with complex content</jc>
 * 	Legend <jv>complex</jv> = <jsm>legend</jsm>(
 * 		<js>"Step 1: "</js>,
 * 		<jsm>strong</jsm>(<js>"Basic Information"</js>),
 * 		<js>" "</js>,
 * 		<jsm>em</jsm>(<js>"(Required)"</js>)
 * 	);
 *
 * 	<jc>// Legend with ID</jc>
 * 	Legend <jv>withId</jv> = <jsm>legend</jsm>(<js>"Form Legend"</js>)
 * 		.id(<js>"form-legend"</js>);
 *
 * 	<jc>// Legend with styling</jc>
 * 	Legend <jv>styled2</jv> = <jsm>legend</jsm>(<js>"Styled Legend"</js>)
 * 		.style(<js>"color: #333; font-weight: bold; padding: 0 10px;"</js>);
 *
 * 	<jc>// Legend with multiple elements</jc>
 * 	Legend <jv>multiple</jv> = <jsm>legend</jsm>(
 * 		<js>"Section 1: "</js>,
 * 		<jsm>span</jsm>(<js>"User Details"</js>).class_(<js>"section-title"</js>),
 * 		<js>" "</js>,
 * 		<jsm>small</jsm>(<js>"(All fields required)"</js>)
 * 	);
 *
 * 	<jc>// Legend with links</jc>
 * 	Legend <jv>withLinks</jv> = <jsm>legend</jsm>(
 * 		<js>"Help: "</js>,
 * 		<jsm>a</jsm>(<js>"/help/forms"</js>, <js>"Form Guide"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#legend() legend()}
 * 		<li class='jm'>{@link HtmlBuilder#legend(Object...) legend(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "legend")
public class Legend extends HtmlElementMixed<Legend> {

	/**
	 * Creates an empty {@link Legend} element.
	 */
	public Legend() {}

	/**
	 * Creates a {@link Legend} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Legend(Object...children) {
		children(children);
	}

}