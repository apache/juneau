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
package org.apache.juneau.rest;

import org.apache.juneau.rest.annotation.*;

/**
 * Class used for defining method-level matchers using the {@link RestMethod#matchers()} annotation.
 *
 * <p>
 * Matchers are used to allow multiple Java methods to handle requests assigned to the same URL path pattern, but
 * differing based on some request attribute, such as a specific header value.
 * For example, matchers can be used to provide two different methods for handling requests from two different client
 * versions.
 *
 * <p>
 * Java methods with matchers associated with them are always attempted before Java methods without matchers.
 * This allows a 'default' method to be defined to handle requests where no matchers match.
 *
 * <p>
 * When multiple matchers are specified on a method, only one matcher is required to match.
 * This is opposite from the {@link RestMethod#guards()} annotation, where all guards are required to match in order to
 * execute the method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foo"</js>, matchers=IsDNT.<jk>class</jk>)
 * 		<jk>public</jk> Object doGetWithDNT() {
 * 			<jc>// Handle request with Do-Not-Track specified</jc>
 * 		}
 *
 * 		<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foo"</js>)
 * 		<jk>public</jk> Object doGetWithoutDNT() {
 * 			<jc>// Handle request without Do-Not-Track specified</jc>
 * 		}
 * 	}
 *
 * 	<jk>public class</jk> IsDNT <jk>extends</jk> RestMatcher {
 * 		<ja>@Override</ja>
 * 		<jk>public boolean</jk> matches(RestRequest req) {
 * 			<jk>return</jk> req.getHeader(<jk>int</jk>.<jk>class</jk>, <js>"DNT"</js>, 0) == 1;
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * Instances must provide one of the following public constructors:
 * <ul>
 * 	<li>No-args.
 * 	<li>The following args: <code>Object resource, Method javaMethod</code>.
 * 		<br>This gives access to the servlet/resource and Java method it's applied to.
 * </ul>
 */
public abstract class RestMatcher {

	/**
	 * Returns <jk>true</jk> if the specified request matches this matcher.
	 *
	 * @param req The servlet request.
	 * @return <jk>true</jk> if the specified request matches this matcher.
	 */
	public abstract boolean matches(RestRequest req);

	/**
	 * Returns <jk>true</jk> if this matcher is required to match in order for the method to be invoked.
	 *
	 * <p>
	 * If <jk>false</jk>, then only one of the matchers must match.
	 *
	 * @return <jk>true</jk> if this matcher is required to match in order for the method to be invoked.
	 */
	public boolean mustMatch() {
		return false;
	}
}
