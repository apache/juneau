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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies a remote proxy interface against a REST interface.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../overview-summary.html#juneau-rest-server.Remoteable'>Remoteable Proxies</a>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Interface proxies against 3rd-party REST interfaces</a>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.remoteable</a>
 * </ul>
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Remoteable {

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
	 *
	 * <p>
	 * The options are:
	 * <ul>
	 * 	<li><js>"DECLARED"</js> (default) - Only methods declared on the immediate interface/class are exposed.
	 * 		Methods on parent interfaces/classes are ignored.
	 * 	<li><js>"ANNOTATED"</js> - Only methods annotated with {@link RemoteMethod} are exposed.
	 * 	<li><js>"ALL"</js> - All methods defined on the interface or class are exposed.
	 * </ul>
	 */
	String expose() default "DECLARED";

	/**
	 * Defines the methodology to use for the path names of the methods when not explicitly defined via
	 * {@link RemoteMethod#path() @RemoteMethod.path()}.
	 *
	 * <p>
	 * The options are:
	 * <ul>
	 * 	<li><js>"NAME"</js> (default) - Use the method name (e.g. "myMethod").
	 * 	<li><js>"SIGNATURE"</js> - Use the method signature (e.g. "myMethod(int,boolean,java.lang.String,int[][][])").
	 * </ul>
	 * <p>
	 * Note that if you use <js>"NAME"</js>, method names must be unique in the interface.
	 */
	String methodPaths() default "NAME";
}
