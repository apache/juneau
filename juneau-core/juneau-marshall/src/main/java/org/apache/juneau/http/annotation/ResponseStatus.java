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

/**
 * REST response status annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response status code.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments of server-side <ja>@RestMethod</ja>-annotated methods.
 * 	<li>Methods and return types of server-side and client-side <ja>@Response</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments of server-side <ja>@RestMethod</ja>-annotated methods</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters to identify them as an HTTP response value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>
 * 	<jk>public void</jk> addPet(<ja>@Body</ja> Pet pet, <ja>@ResponseStatus</ja> Value&lt;Integer&gt; status) {
 * 		<jsm>addPet</jsm>(pet);
 * 		status.set(200);
 * 	}
 * </p>
 *
 * <p>
 * The parameter type must be {@link Value} with a parameterized type of {@link Integer}.
 *
 * <h5 class='topic'>Public methods of <ja>@Response</ja>-annotated types</h5>
 *
 *
 * <p>
 * On {@link Response @Response}-annotated classes, this method can be used to denote an HTTP status code on a response.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>
 * 	<jk>public</jk> Success addPet() {
 * 		<jsm>addPet</jsm>(pet);
 * 		<jk>return new</jk> Success();
 * 	}
 * </p>
 *
 * <p class='bcode w800'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> Success {
 *
 * 		<ja>@ResponseStatus</ja>
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.ResponseStatus}
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Response-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.Response}
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies.Response}
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface ResponseStatus {
}