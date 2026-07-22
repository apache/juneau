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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-param-element">&lt;param&gt;</a>
 * element.
 *
 * <p>
 * The param element defines parameters for an object element. It provides configuration data
 * to the embedded content, such as Flash applications or other plugins. The name attribute
 * specifies the parameter name, and the value attribute provides the parameter value.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple parameter</jc>
 * 	Param <jv>param1</jv> = <jsm>param</jsm>()
 * 		.name(<js>"autoplay"</js>)
 * 		.value(<js>"true"</js>);
 *
 * 	<jc>// Parameter with constructor</jc>
 * 	Param <jv>param2</jv> = <jk>new</jk> Param(<js>"quality"</js>, <js>"high"</js>);
 *
 * 	<jc>// Parameters in an object</jc>
 * 	Object_ <jv>object1</jv> = <jsm>object_</jsm>()
 * 		.data(<js>"video.swf"</js>)
 * 		.type(<js>"application/x-shockwave-flash"</js>)
 * 		.children(
 * 			<jk>new</jk> Param(<js>"autoplay"</js>, <js>"false"</js>),
 * 			<jk>new</jk> Param(<js>"loop"</js>, <js>"true"</js>),
 * 			<jk>new</jk> Param(<js>"quality"</js>, <js>"high"</js>)
 * 		);
 *
 * 	<jc>// Parameter with special characters</jc>
 * 	Param <jv>param3</jv> = <jsm>param</jsm>()
 * 		.name(<js>"config"</js>)
 * 		.value(<js>"width=800&amp;height=600&amp;theme=dark"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#param() param()}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "param")
public class Param extends HtmlElementVoid<Param> {

	/**
	 * Creates an empty {@link Param} element.
	 */
	public Param() {}

	/**
	 * Creates a {@link Param} element with the specified {@link Param#name(String)} and {@link Param#value(Object)}
	 * attributes.
	 *
	 * @param name The {@link Param#name(String)} attribute. Can be <jk>null</jk> to unset the attribute.
	 * @param value The {@link Param#value(Object)} attribute. Can be <jk>null</jk> to unset the attribute.
	 */
	public Param(String name, Object value) {
		name(name).value(value);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-name">name</a> attribute.
	 *
	 * <p>
	 * Specifies the name of the parameter. This name is used by the parent object element
	 * to identify the parameter and its associated value.
	 *
	 * <p>
	 * The name should be meaningful and correspond to the expected parameter name
	 * for the embedded content.
	 *
	 * @param value The name of the parameter. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Param name(String value) {
		attr("name", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-param-value">value</a>
	 * attribute.
	 *
	 * <p>
	 * Value of parameter.
	 *
	 * @param value
	 * 	The new value for this attribute.
	 * 	Typically a {@link Number} or {@link String}.
	 * 	Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Param value(Object value) {
		attr("value", value);
		return this;
	}
}