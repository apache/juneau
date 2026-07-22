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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-option-element">&lt;option&gt;</a>
 * element.
 *
 * <p>
 * The option element represents an option in a select element or a suggestion in a datalist element.
 * It defines a choice that users can select from a dropdown menu or autocomplete list. The value
 * attribute specifies the value to be submitted, while the text content provides the display text.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple option</jc>
 * 	Option <jv>option1</jv> = <jsm>option</jsm>(<js>"red"</js>, <js>"Red"</js>);
 *
 * 	<jc>// Selected option</jc>
 * 	Option <jv>option2</jv> = <jsm>option</jsm>(<js>"blue"</js>, <js>"Blue"</js>)
 * 		.selected(<jk>true</jk>);
 *
 * 	<jc>// Disabled option</jc>
 * 	Option <jv>option3</jv> = <jsm>option</jsm>(<js>"gray"</js>, <js>"Gray"</js>)
 * 		.disabled(<jk>true</jk>);
 *
 * 	<jc>// Option with label</jc>
 * 	Option <jv>option4</jv> = <jsm>option</jsm>(<js>"green"</js>, <js>"Green"</js>)
 * 		.label(<js>"Green Color"</js>);
 *
 * 	<jc>// Options in a select</jc>
 * 	Select <jv>select1</jv> = <jsm>select</jsm>(<js>"color"</js>,
 * 		<jsm>option</jsm>(<js>"red"</js>, <js>"Red"</js>),
 * 		<jsm>option</jsm>(<js>"green"</js>, <js>"Green"</js>),
 * 		<jsm>option</jsm>(<js>"blue"</js>, <js>"Blue"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#option() option()}
 * 		<li class='jm'>{@link HtmlBuilder#option(Object) option(Object)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "option")
public class Option extends HtmlElementText<Option> {

	/**
	 * Creates an empty {@link Option} element.
	 */
	public Option() {}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#text(Object)} attribute.
	 *
	 * @param text The {@link Option#text(Object)} attribute. Can be <jk>null</jk> to leave the text unset.
	 */
	public Option(Object text) {
		text(text);
	}

	/**
	 * Creates an {@link Option} element with the specified {@link Option#value(Object)} attribute and
	 * {@link Option#text(Object)} node.
	 *
	 * @param value The {@link Option#value(Object)} attribute. Can be <jk>null</jk> to unset the attribute.
	 * @param text The {@link Option#text(Object)} node. Can be <jk>null</jk> to leave the text unset.
	 */
	public Option(Object value, Object text) {
		value(value).text(text);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-option-disabled">disabled</a> attribute.
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
	public Option disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-option-label">label</a> attribute.
	 *
	 * <p>
	 * Specifies the user-visible label for the option. This label is displayed in the select element
	 * and can be different from the option's value.
	 *
	 * <p>
	 * The label should be user-friendly and descriptive of what the option represents.
	 *
	 * @param value The user-visible label for the option. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Option label(String value) {
		attr("label", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-option-selected">selected</a> attribute.
	 *
	 * <p>
	 * Whether the option is selected by default.
	 *
	 * <p>
	 * This attribute uses deminimized values:
	 * <ul>
	 * 	<li><jk>false</jk> - Attribute is not added</li>
	 * 	<li><jk>true</jk> - Attribute is added as <js>"selected"</js></li>
	 * 	<li>Other values - Passed through as-is</li>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Boolean} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Option selected(Object value) {
		attr("selected", deminimize(value, "selected"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-option-value">value</a> attribute.
	 *
	 * <p>
	 * Value to be used for form submission.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Option value(Object value) {
		attr("value", value);
		return this;
	}
}
