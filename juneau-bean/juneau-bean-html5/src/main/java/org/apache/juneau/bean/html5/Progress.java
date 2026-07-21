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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#the-progress-element">&lt;progress&gt;</a>
 * element.
 *
 * <p>
 * The progress element represents the completion progress of a task. It is used to display
 * the progress of an operation, such as file uploads, downloads, or any other task that
 * has a defined completion state. The progress element can show both determinate progress
 * (when the total amount of work is known) and indeterminate progress (when the total amount
 * of work is unknown). It is typically rendered as a progress bar that fills up as the task
 * progresses.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple progress bar</jc>
 * 	Progress <jv>simple</jv> = <jsm>progress</jsm>()
 * 		.value(50)
 * 		.max(100);
 *
 * 	<jc>// Progress with styling</jc>
 * 	Progress <jv>styled</jv> = <jsm>progress</jsm>()
 * 		.class_(<js>"file-upload-progress"</js>)
 * 		.value(75)
 * 		.max(100);
 *
 * 	<jc>// Progress with complex content</jc>
 * 	Progress <jv>complex</jv> = <jsm>progress</jsm>(<js>"60% complete"</js>)
 * 		.value(60)
 * 		.max(100);
 *
 * 	<jc>// Progress with ID</jc>
 * 	Progress <jv>withId</jv> = <jsm>progress</jsm>()
 * 		.id(<js>"download-progress"</js>)
 * 		.value(30)
 * 		.max(100);
 *
 * 	<jc>// Progress with styling</jc>
 * 	Progress <jv>styled2</jv> = <jsm>progress</jsm>()
 * 		.style(<js>"width: 300px; height: 20px;"</js>)
 * 		.value(40)
 * 		.max(100);
 *
 * 	<jc>// Progress with multiple attributes</jc>
 * 	Progress <jv>multiple</jv> = <jsm>progress</jsm>(<js>"85% complete"</js>)
 * 		.value(85)
 * 		.max(100)
 * 		.title(<js>"Upload Progress: 85%"</js>);
 *
 * 	<jc>// Progress with form</jc>
 * 	Progress <jv>withForm</jv> = <jsm>progress</jsm>()
 * 		.form(<js>"upload-form"</js>)
 * 		.value(25)
 * 		.max(100);
 *
 * 	// Indeterminate progress
 * 	Progress indeterminate = new Progress()
 * 		.children("Loading...");
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "progress")
public class Progress extends HtmlElementMixed<Progress> {

	/**
	 * Creates an empty {@link Progress} element.
	 */
	public Progress() {}

	/**
	 * Creates a {@link Progress} element with the specified child nodes.
	 *
	 * @param children The child nodes. Must not be <jk>null</jk>.
	 */
	public Progress(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-progress-max">max</a> attribute.
	 *
	 * <p>
	 * Upper bound of range.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Progress max(Object value) {
		attr("max", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-progress-value">value</a> attribute.
	 *
	 * <p>
	 * Current value of the element.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Progress value(Object value) {
		attr("value", value);
		return this;
	}
}