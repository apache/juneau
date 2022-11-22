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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.servlet.http.*;

import org.apache.juneau.http.response.*;

/**
 * Identifies a method that is called immediately after the <c>HttpServlet.service(HttpServletRequest, HttpServletResponse)</c>
 * method is called.
 *
 * <p>
 * Note that you only have access to the raw request and response objects at this point.
 *
 * <p>
 * The list of valid parameter types are as follows:
 * <ul>
 * 	<li>Servlet request/response objects:
 * 		<ul>
 * 			<li>{@link HttpServletRequest}
 * 			<li>{@link HttpServletResponse}
 * 		</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(...)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Add a request attribute to all incoming requests.</jc>
 * 		<ja>@RestStartCall</ja>
 * 		<jk>public void</jk> onStartCall(HttpServletRequest <jv>req</jv>) {
 * 			<jv>req</jv>.setAttribute(<js>"foobar"</js>, <jk>new</jk> FooBar());
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
 * 	<li class='note'>
 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
 * 	<li class='note'>
 * 		Static methods can be used.
 * 	<li class='note'>
 * 		Multiple start-call methods can be defined on a class.
 * 		<br>Start call methods on parent classes are invoked before start-call methods on child classes.
 * 		<br>The order of start-call method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		The method can throw any exception.
 * 		<br>{@link BasicHttpException BasicHttpExceptions} can be thrown to cause a particular HTTP error status code.
 * 		<br>All other exceptions cause an HTTP 500 error status code.
 * 	<li class='note'>
 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
 * 		overridden by the child class.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LifecycleHooks">Lifecycle Hooks</a>
 * </ul>
 */
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(RestStartCallAnnotation.Array.class)
public @interface RestStartCall {

	/**
	 * Dynamically apply this annotation to the specified methods.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};
}
