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

import java.io.*;
import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * REST response body annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response body.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Methods and return types of server-side and client-side <ja>@Response</ja>-annotated interfaces.
 * </ul>
 *
 *
 * <h5 class='topic'>Public methods of <ja>@Response</ja>-annotated methods</h5>
 * <p>
 * On {@link Response @Response}-annotated classes, this method can be used to denote a POJO to use as the response.
 *
 * <p>
 * The method must be public and be one of the following:
 * <ul>
 * 	<li>A public no-arg method with a POJO return type.
 * 	<li>A public one-arg method with a <jk>void</jk> return type that takes in a {@link Reader} or {@link OutputStream}.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestPost</ja>
 * 	<jk>public</jk> AddPetSuccess addPet(Pet <jv>pet</jv>) {
 * 		<jsm>addPet</jsm>(<jv>pet</jv>);
 * 		<jk>return new</jk> AddPetSuccess(...);
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> AddPetSuccess {
 *
 * 		<ja>@ResponseBody</ja>
 * 		<jk>public</jk> Pet getPet() {...}
 * 	}
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> MyCustomJsonResponse {
 *
 * 		<ja>@ResponseHeader</ja>(<js>"Content-Type"</js>)
 * 		<jk>public</jk> String getContentType() {
 * 			<jk>return</jk> <js>"application/json"</js>;
 * 		}
 *
 * 		<ja>@ResponseBody</ja>
 * 		<jk>public void</jk> writeTo(Writer <jv>out</jv>) {
 * 			<jv>out</jv>.write(<js>"{'foo':'bar'}"</js>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Response-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestResponseAnnotation}
 * 	<li class='link'>{@doc RestcResponse}
 * </ul>
 */
@Documented
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(ResponseBodyAnnotation.Array.class)
@ContextApply(ResponseBodyAnnotation.Applier.class)
public @interface ResponseBody {
	/**
	 * Dynamically apply this annotation to the specified classes.
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
}
