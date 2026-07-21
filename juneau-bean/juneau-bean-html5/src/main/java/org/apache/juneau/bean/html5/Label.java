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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-label-element">&lt;label&gt;</a>
 * element.
 *
 * <p>
 * The label element represents a caption for a form control. It provides a programmatic association
 * between the label and the form control, improving accessibility and user experience. When a label
 * is associated with a form control, clicking the label will focus or activate the control.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Label with explicit association</jc>
 * 	Label <jv>label1</jv> = <jsm>label</jsm>(<js>"Username:"</js>)
 * 		.for_(<js>"username"</js>);
 *
 * 	<jc>// Label wrapping form control</jc>
 * 	Label <jv>label2</jv> = <jsm>label</jsm>(
 * 		<jsm>input</jsm>(<js>"checkbox"</js>).name(<js>"agree"</js>),
 * 		<jsm>span</jsm>(<js>"I agree to the terms and conditions"</js>)
 * 	);
 *
 * 	<jc>// Label with form association</jc>
 * 	Label <jv>label3</jv> = <jsm>label</jsm>(<js>"Email Address:"</js>)
 * 		.for_(<js>"email"</js>)
 * 		.form(<js>"contactForm"</js>);
 *
 * 	<jc>// Label with styling</jc>
 * 	Label <jv>label4</jv> = <jsm>label</jsm>(<js>"Password:"</js>)
 * 		.for_(<js>"password"</js>)
 * 		.class_(<js>"required"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "label")
public class Label extends HtmlElementMixed<Label> {

	/**
	 * Creates an empty {@link Label} element.
	 */
	public Label() {}

	/**
	 * Creates a {@link Label} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Label(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-label-for">for</a> attribute.
	 *
	 * <p>
	 * Associates the label with a form control by specifying the control's ID. This creates
	 * a programmatic relationship between the label and the form control for accessibility.
	 *
	 * <p>
	 * The value should match the ID of a form control element in the same document.
	 *
	 * @param value The ID of the form control to associate with this label. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S100" // Method name uses underscore suffix to avoid Java keyword conflict
	})
	public Label for_(String value) {
		attr("for", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the label with a form element by specifying the form's ID. This allows the label
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this label. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Label form(String value) {
		attr("form", value);
		return this;
	}

}