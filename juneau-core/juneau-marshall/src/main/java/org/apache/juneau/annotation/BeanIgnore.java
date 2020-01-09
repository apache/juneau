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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Ignore classes, fields, and methods from being interpreted as bean or bean components.
 *
 * <p>
 * Applied to classes that may look like beans, but you want to be treated as non-beans.
 * For example, if you want to force a bean to be converted to a string using the <c>toString()</c> method, use
 * this annotation on the class.
 *
 * <p>
 * Applies to fields that should not be interpreted as bean property fields.
 *
 * <p>
 * Applies to getters or setters that should not be interpreted as bean property getters or setters.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanIgnoreAnnotation}
 * </ul>
 */
@Documented
@Target({FIELD,METHOD,TYPE,CONSTRUCTOR})
@Retention(RUNTIME)
@Inherited
public @interface BeanIgnore {
	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields/constructors.
	 *
	 * <p>
	 * Used in conjunction with the {@link BeanConfig#annotateBeanIgnore()}.
	 * It is ignored when the annotation is applied directly to classes/methods/fields/constructors.
	 *
	 * The valid pattern matches are:
	 * <ul>
	 * 	<li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass"</js>
	 * 			<li>Fully qualified inner class: <js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 			<li>Simple: <js>"MyClass"</js>
	 * 			<li>Simple inner: <js>"MyClass$Inner1$Inner2"</js> or <js>"Inner1$Inner2"</js> or <js>"Inner2"</js>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args: <js>"com.foo.MyClass.myMethod(String,int)"</js> or <js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js> or <js>"com.foo.MyClass.myMethod()"</js>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass.myMethod"</js>
	 * 			<li>Simple with args: <js>"MyClass.myMethod(String,int)"</js> or <js>"MyClass.myMethod(java.lang.String,int)"</js> or <js>"MyClass.myMethod()"</js>
	 * 			<li>Simple: <js>"MyClass.myMethod"</js>
	 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myMethod"</js> or <js>"Inner1$Inner2.myMethod"</js> or <js>"Inner2.myMethod"</js>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass.myField"</js>
	 * 			<li>Simple: <js>"MyClass.muyField"</js>
	 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myField"</js> or <js>"Inner1$Inner2.myField"</js> or <js>"Inner2.myField"</js>
	 * 		</ul>
	 * 	<li>Constructors:
	 * 		<ul>
	 * 			<li>Fully qualified with args: <js>"com.foo.MyClass(String,int)"</js> or <js>"com.foo.MyClass(java.lang.String,int)"</js> or <js>"com.foo.MyClass()"</js>
	 * 			<li>Simple with args: <js>"MyClass(String,int)"</js> or <js>"MyClass(java.lang.String,int)"</js> or <js>"MyClass()"</js>
	 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2()"</js> or <js>"Inner1$Inner2()"</js> or <js>"Inner2()"</js>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String on() default "";
}

