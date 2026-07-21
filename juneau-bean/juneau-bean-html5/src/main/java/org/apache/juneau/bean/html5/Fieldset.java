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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-fieldset-element">&lt;fieldset&gt;</a>
 * element.
 *
 * <p>
 * The fieldset element groups related form controls together. It provides a visual and semantic
 * grouping mechanism that helps organize complex forms and improves accessibility. The legend
 * element is typically used as the first child to provide a caption for the fieldset.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple fieldset with legend</jc>
 * 	Fieldset <jv>fieldset1</jv> = <jsm>fieldset</jsm>(
 * 		<jsm>legend</jsm>(<js>"Personal Information"</js>),
 * 		<jsm>input</jsm>(<js>"text"</js>).name(<js>"firstName"</js>).placeholder(<js>"First Name"</js>),
 * 		<jsm>input</jsm>(<js>"text"</js>).name(<js>"lastName"</js>).placeholder(<js>"Last Name"</js>)
 * 	);
 *
 * 	<jc>// Disabled fieldset</jc>
 * 	Fieldset <jv>fieldset2</jv> = <jsm>fieldset</jsm>(
 * 		<jsm>legend</jsm>(<js>"Disabled Section"</js>),
 * 		<jsm>input</jsm>(<js>"text"</js>).name(<js>"disabledField"</js>).value(<js>"Cannot edit"</js>)
 * 	).disabled(<jk>true</jk>);
 *
 * 	<jc>// Fieldset with custom styling</jc>
 * 	Fieldset <jv>fieldset3</jv> = <jsm>fieldset</jsm>(
 * 		<jsm>legend</jsm>(<js>"Contact Details"</js>),
 * 		<jsm>input</jsm>(<js>"email"</js>).name(<js>"email"</js>).placeholder(<js>"Email"</js>),
 * 		<jsm>input</jsm>(<js>"tel"</js>).name(<js>"phone"</js>).placeholder(<js>"Phone"</js>)
 * 	).class_(<js>"form-group"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#fieldset() fieldset()}
 * 		<li class='jm'>{@link HtmlBuilder#fieldset(Object...) fieldset(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "fieldset")
public class Fieldset extends HtmlElementMixed<Fieldset> {

	/**
	 * Creates an empty {@link Fieldset} element.
	 */
	public Fieldset() {}

	/**
	 * Creates a {@link Fieldset} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Fieldset(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fieldset-disabled">disabled</a> attribute.
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
	public Fieldset disabled(Object value) {
		attr("disabled", deminimize(value, "disabled"));
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the fieldset with a form element by specifying the form's ID. This allows the fieldset
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this fieldset. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Fieldset form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the fieldset. This name can be used to access the fieldset
	 * via the form.elements API and for form submission.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param value The name of the fieldset for API access and submission. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Fieldset name(String value) {
		attr("name", value);
		return this;
	}

}