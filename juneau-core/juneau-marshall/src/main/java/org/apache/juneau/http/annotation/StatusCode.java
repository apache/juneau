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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * REST response status annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response status code.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Methods and return types of server-side and client-side <ja>@Response</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments of server-side <ja>@RestOp</ja>-annotated methods</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters to identify them as an HTTP response value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public void</jk> addPet(<ja>@Content</ja> Pet <jv>pet</jv>, <ja>@StatusCode</ja> Value&lt;Integer&gt; <jv>status</jv>) {
 * 		<jsm>addPet</jsm>(<jv>pet</jv>);
 * 		<jv>status</jv>.set(200);
 * 	}
 * </p>
 *
 * <p>
 * The parameter type must be {@link Value} with a parameterized type of {@link Integer}.
 *
 * <h5 class='topic'>Public methods of <ja>@Response</ja>-annotated types</h5>
 *
 * <p>
 * On {@link Response @Response}-annotated classes, this method can be used to denote an HTTP status code on a response.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public</jk> Success addPet(Pet <jv>pet</jv>) {
 * 		<jsm>addPet</jsm>(<jv>pet</jv>);
 * 		<jk>return new</jk> Success();
 * 	}
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> Success {
 *
 * 		<ja>@StatusCode</ja>
 * 		<jk>public int</jk> getStatus() {
 * 			<jk>return</jk> 201;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String toString() {
 * 			<jk>return</jk> <js>"Pet was successfully added"</js>;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The method being annotated must be public and return a numeric value.
 *
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Response-annotated interfaces</h5>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Response">@Response</a>
 * </ul>
 *
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(StatusCodeAnnotation.Array.class)
@ContextApply(StatusCodeAnnotation.Applier.class)
public @interface StatusCode {

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * The HTTP response codes.
	 *
	 * The default value is <c>500</c> for exceptions and <c>200</c> for return types.
	 *
	 * @return The annotation value.
	 */
	int[] value() default {};
}