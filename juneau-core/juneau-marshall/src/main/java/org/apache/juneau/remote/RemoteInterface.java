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
package org.apache.juneau.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.http.remote.*;

/**
 * Identifies a remote proxy REST interface.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.restRPC}
 * </ul>
 *
 * @deprecated Use {@link Remote}
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
@Deprecated
public @interface RemoteInterface {

	/**
	 * REST service path.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>An absolute URL.
	 * 	<li>A relative URL interpreted as relative to the root URL defined on the <c>RestClient</c>
	 * 	<li>No path interpreted as the class name (e.g. <js>"http://localhost/root-url/org.foo.MyInterface"</js>)
	 * </ul>
	 */
	String path() default "";
}
