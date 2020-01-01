// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.json.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation for specifying various JSON options for the JSON serializers and parsers.
 *
 * <p>
 * Can be applied to Java types.
 *
 * <p>
 * Can be used for the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Wrap bean instances inside wrapper object (e.g. <c>{'wrapperAttr':bean}</c>).
 * </ul>
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Json {

	/**
	 * Defines which classes/methods this annotation applies to.
	 *
	 * <p>
	 * Used in conjunction with the {@link JsonConfig#annotateJson()}.
	 * It is ignored when the annotation is applied directly to classes and methods.
	 *
	 * The format can be any of the following:
	 * <ul>
	 * 	<li>Full class name (e.g. <js>"com.foo.MyClass"</js>).
	 * 	<li>Simple class name (e.g. <js>"MyClass"</js>).
	 * 	<li>Full method name (e.g. <js>"com.foo.MyClass.myMethod"</js>).
	 * 	<li>Simple method name (e.g. <js>"MyClass.myMethod"</js>).
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.ClassMethodAnnotations}
	 * </ul>
	 */
	String on() default "";

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
	 * <p class='bcode w800'>
	 * 	<ja>@Json</ja>(wrapperAttr=<js>"myWrapper"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> f1 = 123;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Without the <ja>@Json</ja> annotations, serializing this bean as JSON would have produced the following...
	 * <p class='bcode w800'>
	 * 	{
	 * 		f1: 123
	 * 	}
	 * </p>
	 *
	 * <p>
	 * With the annotations, serializing this bean as JSON produces the following...
	 * <p class='bcode w800'>
	 * 	{
	 * 		myWrapper: {
	 * 			f1: 123
	 * 		}
	 * 	}
	 * </p>
	 */
	String wrapperAttr() default "";
}
