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
 * Identifies a method that gets called immediately after servlet initialization.
 *
 * <p>
 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the {@link RestContext}
 * object has been created.
 *
 * <p>
 * The only valid parameter type for this method is {@link RestContext} which can be used to retrieve information
 * about the servlet.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
 * 	<li class='note'>
 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
 * 	<li class='note'>
 * 		Static methods can be used.
 * 	<li class='note'>
 * 		Multiple post-init methods can be defined on a class.
 * 		<br>Post-init methods on parent classes are invoked before post-init methods on child classes unless {@link #childFirst()} is specified.
 * 		<br>The order of Post-init method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		The method can throw any exception causing initialization of the servlet to fail.
 * 	<li class='note'>
 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
 * 		overridden by the child class.
 * </ul>
 */
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(RestPostInitAnnotation.Array.class)
public @interface RestPostInit {

	/**
	 * Execute in child-first order.
	 *
	 * <p>
	 * Use this annotation if you need to perform any kind of initialization on child resources before the parent resource.
	 *
	 * <p>
	 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the {@link RestContext}
	 * object has been created and after the non-child-first methods have been called.
	 *
	 * @return The annotation value.
	 */
	boolean childFirst() default false;

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
