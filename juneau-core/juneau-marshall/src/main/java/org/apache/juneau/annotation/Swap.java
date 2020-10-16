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

import org.apache.juneau.transform.*;

/**
 * Associates {@link PojoSwap} and {@link Surrogate} classes with POJOs and bean properties.
 *
 * <p>
 * This annotation can be used in the following locations:
 * <ul>
 * 	<li>Classes.
 * 	<li>Bean getters/setters/fields.
 * 	<li>Inside the {@link Swaps @Swaps} annotation.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc SwapAnnotation}
 * </ul>
 */
@Documented
@Target({TYPE,ANNOTATION_TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Swap {

	/**
	 * The {@link PojoSwap} and {@link Surrogate} class.
	 *
	 * <p>
	 * A synonym for {@link #value()}.
	 */
	Class<?> impl() default Null.class;

	/**
	 * Identifies the media types that this swap is applicable for.
	 *
	 * <p>
	 * In the following example, the swap is only invoked by the JSON serializer:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@Swap</ja>(impl=ToStringSwap.<jk>class</jk>, mediaTypes=<js>"&#42;/json"</js>)
	 * 	<jk>public class</jk> MyBean { ... }
	 *
	 * 	<jk>public class</jk> ToStringSwap <jk>extends</jk> PojoSwap&lt;Object,String&gt; {
	 * 			<jk>public</jk> String swap(BeanSession session, Object o) <jk>throws</jk> Exception {
	 * 				<jk>return</jk> o.toString();
	 * 			}
	 * 		}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc PerMediaTypePojoSwaps}
	 * </ul>
	 */
	String[] mediaTypes() default {};

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with the {@link BeanConfig#applySwap()}.
	 * It is ignored when the annotation is applied directly to classes.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	Class<?>[] onClass() default {};

	/**
	 * Identifies a template string along with this swap.
	 *
	 * <p>
	 * Template strings are arbitrary strings associated with swaps that help provide additional context information
	 * for the swap class.
	 * They're called 'templates' because their primary purpose is for providing template names, such as Apache FreeMarker
	 * template names.
	 *
	 * <p>
	 * The following is an example of a templated swap class used to serialize POJOs to HTML using FreeMarker:
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Our templated swap class.</jc>
	 * 	<jk>public class</jk> FreeMarkerSwap <jk>extends</jk> PojoSwap&lt;Object,Reader&gt; {
	 *
	 * 		<jk>public</jk> MediaType[] forMediaTypes() {
	 * 			<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/html"</js>);
	 * 		}
	 *
	 * 		<jk>public</jk> Reader swap(BeanSession session, Object o, String template) <jk>throws</jk> Exception {
	 * 			<jk>return</jk> getFreeMarkerReader(template, o);  <jc>// Some method that creates raw HTML.</jc>
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@Swap</ja>(impl=FreeMarkerSwap.<jk>class</jk>, template=<js>"MyPojo.div.ftl"</js>)
	 * 	<jk>public class</jk> MyPojo {}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc TemplatedSwaps}
	 * </ul>
	 */
	String template() default "";

	/**
	 * The {@link PojoSwap} and {@link Surrogate} class.
	 *
	 * <p>
	 * A synonym for {@link #impl()}.
	 */
	Class<?> value() default Null.class;
}