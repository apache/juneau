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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-optgroup-element">&lt;optgroup&gt;</a>
 * element.
 *
 * <p>
 * The optgroup element represents a group of option elements within a select element. It provides
 * a way to organize related options into logical groups, making it easier for users to find and
 * select the desired option. The label attribute provides the group heading.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple option group</jc>
 * 	Optgroup <jv>optgroup1</jv> = <jsm>optgroup</jsm>(<js>"Fruits"</js>,
 * 		<jsm>option</jsm>(<js>"apple"</js>, <js>"Apple"</js>),
 * 		<jsm>option</jsm>(<js>"banana"</js>, <js>"Banana"</js>),
 * 		<jsm>option</jsm>(<js>"orange"</js>, <js>"Orange"</js>)
 * 	);
 *
 * 	<jc>// Disabled option group</jc>
 * 	Optgroup <jv>optgroup2</jv> = <jsm>optgroup</jsm>(<js>"Vegetables"</js>,
 * 		<jsm>option</jsm>(<js>"carrot"</js>, <js>"Carrot"</js>),
 * 		<jsm>option</jsm>(<js>"broccoli"</js>, <js>"Broccoli"</js>)
 * 	).disabled(<jk>true</jk>);
 *
 * 	<jc>// Multiple option groups in a select</jc>
 * 	Select <jv>select1</jv> = <jsm>select</jsm>(<js>"food"</js>,
 * 		<jsm>optgroup</jsm>(<js>"Fruits"</js>,
 * 			<jsm>option</jsm>(<js>"apple"</js>, <js>"Apple"</js>),
 * 			<jsm>option</jsm>(<js>"banana"</js>, <js>"Banana"</js>)
 * 		),
 * 		<jsm>optgroup</jsm>(<js>"Vegetables"</js>,
 * 			<jsm>option</jsm>(<js>"carrot"</js>, <js>"Carrot"</js>),
 * 			<jsm>option</jsm>(<js>"broccoli"</js>, <js>"Broccoli"</js>)
 * 		)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#optgroup() optgroup()}
 * 		<li class='jm'>{@link HtmlBuilder#optgroup(Object...) optgroup(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "optgroup")
public class Optgroup extends HtmlElementContainer<Optgroup> {

	/**
	 * Creates an empty {@link Optgroup} element.
	 */
	public Optgroup() {}

	/**
	 * Creates an {@link Optgroup} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Optgroup(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-optgroup-disabled">disabled</a> attribute.
	 *
	 * <p>
	 * Whether the form control is disabled.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"disabled"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Optgroup disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-optgroup-label">label</a> attribute.
	 *
	 * <p>
	 * Specifies the user-visible label for the option group. This label is displayed
	 * in the select element to group related options together.
	 *
	 * <p>
	 * The label should be descriptive and help users understand the grouping of options.
	 *
	 * @param value The user-visible label for the option group. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Optgroup label(String value) {
		attr("label", value);
		return this;
	}

}