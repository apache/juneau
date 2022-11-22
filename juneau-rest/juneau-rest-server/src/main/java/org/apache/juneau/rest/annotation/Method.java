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

/**
 * Annotation that can be applied to a parameter of a {@link RestOp @RestOp} annotated method to identify it as the HTTP
 * method.
 *
 * <p>
 * Typically used for HTTP method handlers of type <js>"*"</js> (i.e. handle all requests).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestOp</ja>(method=<js>"*"</js>)
 * 	<jk>public void</jk> doAnything(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>, <ja>@Method</ja> String <jv>method</jv>) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bjava'>
 * 	<ja>@RestOp</ja>(method=<js>"*"</js>)
 * 	<jk>public void</jk> doAnything(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		String <jv>method</jv> = <jv>req</jv>.getMethod();
 * 		...
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Method {
}
