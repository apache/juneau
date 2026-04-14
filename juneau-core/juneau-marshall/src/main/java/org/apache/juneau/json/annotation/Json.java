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
package org.apache.juneau.json.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation for specifying various JSON options for the JSON serializers and parsers.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link JsonApply @JsonApply}.
 * </ul>
 *
 * <p>
 * Can be used for the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Wrap bean instances inside wrapper object (e.g. <c>{'wrapperAttr':bean}</c>).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>

 * </ul>
 */
@Documented
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@Inherited
public @interface Json {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Wraps beans in a JSON object with the specified attribute name.
	 *
	 * <p>
	 * Applies only to {@link ElementType#TYPE}.
	 *
	 * <p>
	 * This annotation can be applied to beans as well as other objects serialized to other types (e.g. strings).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Json</ja>(wrapperAttr=<js>"myWrapper"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> <jf>f1</jf> = 123;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Without the <ja>@Json</ja> annotations, serializing this bean as JSON would have produced the following...
	 * <p class='bjson'>
	 * 	{
	 * 		f1: 123
	 * 	}
	 * </p>
	 *
	 * <p>
	 * With the annotations, serializing this bean as JSON produces the following...
	 * <p class='bjson'>
	 * 	{
	 * 		myWrapper: {
	 * 			f1: 123
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String wrapperAttr() default "";
}