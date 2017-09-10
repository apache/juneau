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
import java.util.*;

import org.apache.juneau.transform.*;

/**
 * Associates {@link PojoSwap} and {@link Surrogate} classes with POJOs and bean properties.
 *
 * <p>
 * A typical example is for rendering {@link Date Dates} and {@link Calendar Calendars} as a formatted string:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jk>public class</jk> MyClass {
 *
 * 		<jc>// During serialization, convert to ISO8601 date-time string.</jc>
 * 		<ja>@Swap</ja>(CalendarSwap.ISO8601DT.<jk>class</jk>)
 * 		<jk>public</jk> Calendar getTime();
 * 	}
 * </p>
 *
 * <p>
 * This annotation can be used in the following locations:
 * <ul>
 * 	<li>Classes.
 * 	<li>Bean getters/setters/fields.
 * 	<li>Inside the {@link Swaps @Swaps} annotation.
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
	 * A synonym for {@link #impl()}.
	 */
	Class<?> value() default Null.class;

	/**
	 * The {@link PojoSwap} and {@link Surrogate} class.
	 *
	 * <p>
	 * A synonym for {@link #value()}.
	 */
	Class<?> impl() default Null.class;


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
	 * <p class='bcode'>
	 * 	<jc>// Our abstracted templated swap class.</jc>
	 * 	<jk>public abstract class</jk> FreeMarkerSwap <jk>extends</jk> PojoSwap&lt;Object,Reader&gt; {
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
	 * <p class='bcode'>
	 * 	<ja>@Swap</ja>(impl=FreeMarkerSwap.<jk>class</jk>, template=<js>"MyPojo.div.ftl"</js>)
	 * 	<jk>public class</jk> MyPojo {}
	 * </p>
	 */
	String template() default "";

	/**
	 * Identifies the media types that this swap is applicable for.
	 *
	 * <p>
	 * In the following example, the swap is only invoked by the JSON serializer:
	 *
	 * <p class='bcode'>
	 * 	<ja>@Swap</ja>(impl=ToStringSwap.<jk>class</jk>, mediaTypes=<js>"&#42;/json"</js>)
	 * 	<jk>public class</jk> MyBean { ... }
	 *
	 * 	<jk>public class</jk> ToStringSwap <jk>extends</jk> PojoSwap&lt;Object,String&gt; {
	 * 			<jk>public</jk> String swap(BeanSession session, Object o) <jk>throws</jk> Exception {
	 * 				<jk>return</jk> o.toString();
	 * 			}
	 * 		}
	 * </p>
	 */
	String[] mediaTypes() default {};
}