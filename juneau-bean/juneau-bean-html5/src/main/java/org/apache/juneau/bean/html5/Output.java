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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-output-element">&lt;output&gt;</a>
 * element.
 *
 * <p>
 * The output element represents the result of a calculation or user action. It is typically used
 * to display the result of form calculations, such as the sum of two numbers or the result of
 * a mathematical operation. The for attribute can be used to associate the output with specific
 * form controls that contribute to the calculation.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple output element</jc>
 * 	Output <jv>output1</jv> = <jsm>output</jsm>(<js>"0"</js>)
 * 		.name(<js>"result"</js>);
 *
 * 	<jc>// Output with form association</jc>
 * 	Output <jv>output2</jv> = <jsm>output</jsm>(<js>"0"</js>)
 * 		.name(<js>"sum"</js>)
 * 		.for_(<js>"num1 num2"</js>);
 *
 * 	<jc>// Output in a calculation form</jc>
 * 	Form <jv>calcForm</jv> = <jsm>form</jsm>(
 * 		<jsm>input</jsm>(<js>"number"</js>).name(<js>"num1"</js>).id(<js>"num1"</js>).value(<js>"0"</js>),
 * 		<jsm>span</jsm>(<js>" + "</js>),
 * 		<jsm>input</jsm>(<js>"number"</js>).name(<js>"num2"</js>).id(<js>"num2"</js>).value(<js>"0"</js>),
 * 		<jsm>span</jsm>(<js>" = "</js>),
 * 		<jsm>output</jsm>(<js>"0"</js>).name(<js>"sum"</js>).for_(<js>"num1 num2"</js>)
 * 	);
 *
 * 	<jc>// Output with styling</jc>
 * 	Output <jv>output3</jv> = <jsm>output</jsm>(<js>"Ready for calculation"</js>)
 * 		.name(<js>"display"</js>)
 * 		.class_(<js>"result-display"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#output() output()}
 * 		<li class='jm'>{@link HtmlBuilder#output(String) output(String)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "output")
public class Output extends HtmlElementMixed<Output> {

	/**
	 * Creates an empty {@link Output} element.
	 */
	public Output() {}

	/**
	 * Creates an {@link Output} element with the specified {@link Output#name(String)} attribute.
	 *
	 * @param name The {@link Output#name(String)} attribute. Can be <jk>null</jk>.
	 */
	public Output(String name) {
		name(name);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-output-for">for</a> attribute.
	 *
	 * <p>
	 * Specifies the IDs of form controls from which the output value was calculated.
	 * This creates a programmatic relationship between the output and its source controls.
	 *
	 * <p>
	 * Multiple IDs can be specified as a space-separated list.
	 *
	 * @param value The IDs of the form controls that contribute to this output. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	@SuppressWarnings({
		"java:S100" // Method name uses underscore suffix to avoid Java keyword conflict
	})
	public Output for_(String value) {
		attr("for", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fae-form">form</a> attribute.
	 *
	 * <p>
	 * Associates the output element with a form element by specifying the form's ID. This allows the output
	 * to be placed outside the form element while still being part of the form.
	 *
	 * <p>
	 * The value should match the ID of a form element in the same document.
	 *
	 * @param value The ID of the form element to associate with this output. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Output form(String value) {
		attr("form", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-fe-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the output element. This name is used when the form is submitted and
	 * can be used to access the element via the form.elements API.
	 *
	 * <p>
	 * The name should be unique within the form and should not contain spaces or special characters.
	 *
	 * @param value The name of the output element for submission and API access. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Output name(String value) {
		attr("name", value);
		return this;
	}

}