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

/**
 * REST response body annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response body.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Public methods of <ja>@Response</ja>-annotated methods.
 * </ul>
 *
 *
 * <h5 class='topic'>Public methods of <ja>@Response</ja>-annotated methods</h5>
 * <p>
 * On {@link Response @Response}-annotated classes, this method can be used to denote a POJO to use as the response.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>
 * 	<jk>public</jk> AddPetSuccess addPet() {
 * 		<jsm>addPet</jsm>(pet);
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
 * <p>
 * The method being annotated must be public.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HttpPartAnnotations.Response">Overview &gt; juneau-rest-server &gt; @Response</a>
 * </ul>
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface ResponseBody {}
