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

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.utils.*;

/**
 * Annotation that can be applied to parameters and types to denote them as an HTTP response status.
 * 
 * <p>
 * This can only be applied to parameters and subclasses of the {@link Value} class with an {@link Integer} type.
 * <br>The {@link Value} object is mean to be a place-holder for the set value.
 * 
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/user/login"</js>)
 * 	<jk>public void</jk> login(String username, String password, 
 * 			<ja>@Status</ja>(code=401, description=<js>"Invalid user/pw"</js>) Value&lt;Integer&gt; status) {
 * 		<jk>if</jk> (! isValid(username, password))
 * 			status.set(401);
 * 	}
 * </p>
 * 
 * <p>
 * The {@link Responses @Responses} annotation can be used to represent multiple possible response types.
 * 
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/user/login"</js>)
 * 	<jk>public void</jk> login(String username, String password, 
 * 			<ja>@ResponseStatus</ja>{
 * 				<ja>@Status</ja>(200)
 * 				<ja>@Status</ja>(code=401, description=<js>"Invalid user/pw"</js>)
 *			}
 * 			Value&lt;Integer&gt; status) {
 * 
 * 		<jk>if</jk> (! isValid(username, password))
 * 			status.set(401);
 * 		<jk>else</jk>
 * 			status.set(200);
 * 	}
 * </p>
 * 
 * <p>
 * The other option is to apply this annotation to a subclass of {@link Value} which often leads to a cleaner
 * REST method:
 * 
 * <p class='bcode'>
 * 	<ja>@ResponseStatus</ja>{
 * 		<ja>@Status</ja>(200)
 * 		<ja>@Status</ja>(code=401, description=<js>"Invalid user/pw"</js>)
 *	}
 * 	<jk>public class</jk> LoginStatus <jk>extends</jk> Value&lt;Integer&gt; {}
 * 	
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/user/login"</js>)
 * 	<jk>public void</jk> login(String username, String password, LoginStatus status) { 
 * 		<jk>if</jk> (! isValid(username, password))
 * 			status.set(401);
 * 		<jk>else</jk>
 * 			status.set(200);
 * 	}
 * </p>
 * 
 * <p>
 * The attributes on this annotation are used to populate the generated Swagger for the method.
 * <br>In this case, the Swagger is populated with the following:
 * 
 * <p class='bcode'>
 * 	<js>'/user/login'</js>: {
 * 		get: {
 * 			responses: {
 * 				200: {
 * 					description: <js>'OK'</js>
 * 				},
 * 				401: {
 * 					description: <js>'Invalid user/pw'</js>
 * 				}
 * 			}
 * 		}
 * 	}
 * </p>
 */
@Documented
@Target({})
@Retention(RUNTIME)
@Inherited
public @interface Status {
	
	/**
	 * The HTTP status of the response.
	 */
	int code() default 0;
	
	/**
	 * A synonym to {@link #code()}.
	 * 
	 * <p>
	 * Useful if you only want to specify a code only.
	 * 
	 * <p class='bcode'>
	 * 	<ja>@Status</ja>(200)
	 * </p>
	 */
	int value() default 0;

	/**
	 * Defines the swagger value <code>/paths/{path}/{method}/responses/{status-code}/description</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format of the value is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};
}
