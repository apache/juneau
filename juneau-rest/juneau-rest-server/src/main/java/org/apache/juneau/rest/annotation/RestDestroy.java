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

import javax.servlet.*;

import org.apache.juneau.rest.*;

/**
 * Identifies a method that gets called during servlet destroy.
 *
 * <p>
 * This method is called from within the {@link Servlet#destroy()}.
 *
 * <p>
 * The only valid parameter type for this method is {@link RestContext}, although typically no arguments will
 * be specified.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(...)
 * 	<jk>public class</jk> PetStoreResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> BasicUniversalJenaConfig {
 *
 * 		<jc>// Our database.</jc>
 * 		<jk>private</jk> Map&lt;Integer,Pet&gt; <jf>petDB</jf>;
 *
 * 		<ja>@RestDestroy</ja>
 * 		<jk>public void</jk> onDestroy() {
 * 			<jf>petDB</jf> = <jk>null</jk>;
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
 * 		Multiple destroy methods can be defined on a class.
 * 		<br>Destroy methods on child classes are invoked before destroy methods on parent classes.
 * 		<br>The order of destroy method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		In general, destroy methods should not throw any exceptions, although if any are thrown, the stack trace will be
 * 		printed to <c>System.err</c>.
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
@Repeatable(RestDestroyAnnotation.Array.class)
public @interface RestDestroy {

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
