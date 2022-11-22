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

/**
 * Identifies a method that gets called right before we exit the servlet service method.
 *
 * <p>
 * At this point, the output has been written and flushed.
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
 * <p>
 * The following attributes are set on the {@link HttpServletRequest} object that can be useful for logging purposes:
 * <ul>
 * 	<li><js>"Exception"</js> - Any exceptions thrown during the request.
 * 	<li><js>"ExecTime"</js> - Execution time of the request.
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(...)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Log the time it took to execute the request.</jc>
 * 		<ja>@RestEndCall
 * 		<jk>public void</jk> onEndCall(HttpServletRequest <jv>req</jv>, Logger <jv>logger</jv>) {
 * 			Exception <jv>exception</jv> = (Exception)<jv>req</jv>.getAttribute(<js>"Exception"</js>);
 * 			Long <jv>execTime</jv> = (Long)<jv>req</jv>.getAttribute(<js>"ExecTime"</js>);
 * 			<jk>if</jk> (<jv>exception</jv> != <jk>null</jk>)
 * 				<jv>logger</jv>.warn(<jv>exception</jv>, <js>"Request failed in {0}ms."</js>, <jv>execTime</jv>);
 * 			<jk>else</jk>
 * 				<jv>logger</jv>.fine(<js>"Request finished in {0}ms."</js>, <jv>execTime</jv>);
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
 * 		Multiple END_CALL methods can be defined on a class.
 * 		<br>END_CALL methods on parent classes are invoked before END_CALL methods on child classes.
 * 		<br>The order of END_CALL method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		The method can throw any exception, although at this point it is too late to set an HTTP error status code.
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
@Repeatable(RestEndCallAnnotation.Array.class)
public @interface RestEndCall {

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
