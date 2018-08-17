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

/**
 * Identifies a remote proxy interface against a REST interface.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-server.RemoteInterfaces}
 * 	<li class='link'>{@doc juneau-rest-client.RemoteResources}
 * </ul>
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface RemoteInterface {

	/**
	 * The absolute or relative path of the REST service.
	 *
	 * <p>
	 * When a relative path is specified, it's relative to the root-url defined on the <code>RestClient</code> used
	 * to instantiate the interface.
	 *
	 * <p>
	 * When no path is specified, the path is assumed to be the class name (e.g.
	 * <js>"http://localhost/root-url/org.foo.MyInterface"</js>)
	 */
	String path() default "";

	/**
	 * Identifies which methods on the interface should be exposed through the proxy.
	 */
	RemoteExpose expose() default RemoteExpose.DEFAULT;
}
