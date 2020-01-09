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
 * Identifies examples for POJOs.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Static method that returns an example of the POJO.
 * 	<li>Static field that contains an example of the POJO.
 * 	<li>On a class.
 * </ul>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// On a static method.</jc>
 * 	<jk>public class</jk> A {
 *
 * 		<ja>@Example</ja>
 * 		<jk>public static</jk> A example() {
 * 			<jk>return new</jk> A().foo(<js>"bar"</js>).baz(123);
 * 		}
 *
 * 		...
 * 	}
 *
 * 	<jc>// On a static field.</jc>
 * 	<jk>public class</jk> B {
 *
 * 		<ja>@Example</ja>
 * 		<jk>public static</jk> B EXAMPLE = <jk>new</jk> B().foo(<js>"bar"</js>).baz(123);
 *
 * 		...
 * 	}
 *
 * 	<jc>// On a class.</jc>
 * 	<ja>@Example</js>(<js>"{foo:'bar',baz:123}"</js>)
 * 	<jk>public class</jk> C {...}
 * </p>
 */
@Documented
@Target({FIELD,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Example {

	/**
	 * Defines which classes/methods/fields this annotation applies to.
	 *
	 * <p>
	 * Used in conjunction with the {@link BeanConfig#applyExample()}.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * The format can be any of the following:
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

	/**
	 * An example of a POJO class.
	 *
	 * <p>
	 * Format is Lax-JSON.
	 *
	 * <p>
	 * This value is only used when the annotation is used on a type.
	 */
	String value() default "";
}