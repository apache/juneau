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

import org.apache.juneau.utils.*;

/**
 * REST response statuses annotation.
 *
 * <p>
 * Used to associate multiple {@link ResponseStatus @ResponseStatus} annotations to the same parameter or class.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/user/login"</js>)
 * 	<jk>public void</jk> login(String username, String password,
 * 			<ja>@ResponseStatuses</ja>{
 * 				<ja>@ResponseStatus</ja>(200)
 * 				<ja>@ResponseStatus</ja>(code=401, description=<js>"Invalid user/pw"</js>)
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
 * <p class='bcode w800'>
 * 	<ja>@ResponseStatuses</ja>{
 * 		<ja>@ResponseStatus</ja>(200)
 * 		<ja>@ResponseStatus</ja>(code=401, description=<js>"Invalid user/pw"</js>)
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
 * Since Juneau currently prereq's Java 1.7, we cannot take advantage of annotation duplication support in Java 8.
 * <br>This annotation overcomes that limitation.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HttpPartAnnotations.ResponseStatuses">Overview &gt; juneau-rest-server &gt; @ResponseStatuses</a>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Swagger">Overview &gt; juneau-rest-server &gt; OPTIONS pages and Swagger</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface ResponseStatuses {

	/**
	 * Specifies one or more {@link ResponseStatus @ResponseStatus} annotations to apply to the same parameter or class.
	 */
	ResponseStatus[] value() default {};
}