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
package org.apache.juneau.oapi.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.oapi.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by {@link OpenApiSerializer} and {@link OpenApiParser}.
 *
 * <ul class='seealso'>
 * </ul>
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface OpenApi {

	/**
	 * Defines which classes/methods this annotation applies to.
	 *
	 * <p>
	 * Used in conjunction with the {@link OpenApiConfig#applyOpenApi()}.
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
}
