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
package org.apache.juneau.rest.guard;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.response.*;

/**
 * REST method guard.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Implements a guard mechanism for REST method calls that allows requests to be rejected before invocation of the REST
 * method.
 * For example, guards can be used to ensure that only administrators can call certain methods.
 *
 * <p>
 * Guards are applied to REST methods declaratively through the {@link Rest#guards() @Rest(guards)} or
 * {@link RestOp#guards() @RestOp(guards)} annotations.
 *
 * <p>
 * If multiple guards are specified, ALL guards must pass in order for the request to proceed.
 *
 * <h5 class='topic'>How to implement</h5>
 *
 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used for
 * other purposes like pre-call validation of a request.
 *
 * <p>
 * Implementers should simply throw a {@link BasicHttpException} from the {@link #guard(RestRequest, RestResponse)}
 * method to abort processing on the current request.
 *
 * <p>
 * Guards must implement a no-args constructor.
 *
 * <h5 class='topic'>Example usage:</h5>
 * <p class='bjava'>
 * 	<jk>public</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Delete method with guard that only allows Billy to call it.</jc>
 * 		<ja>@RestDelete</ja>(guards=BillyGuard.<jk>class</jk>)
 * 		<jk>public</jk> doDelete(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {...}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Example implementation:</h5>
 * <p class='bjava'>
 * 	<jc>// Define a guard that only lets Billy make a request</jc>
 * 	<jk>public</jk> BillyGuard <jk>extends</jk> RestGuard {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public boolean</jk> isRequestAllowed(RestRequest <jv>req</jv>) {
 * 			<jk>return</jk> <jv>req</jv>.getUserPrincipal().getName().contains(<js>"Billy"</js>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Guards">Guards</a>
 * </ul>
 */
public abstract class RestGuard {

	/**
	 * Checks the current HTTP request and throws a {@link BasicHttpException} if the guard does not permit the request.
	 *
	 * <p>
	 * By default, throws an <jsf>SC_FORBIDDEN</jsf> exception if {@link #isRequestAllowed(RestRequest)} returns
	 * <jk>false</jk>.
	 *
	 * <p>
	 * Subclasses are free to override this method to tailor the behavior of how to handle unauthorized requests.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @throws BasicHttpException Thrown to abort processing on current request.
	 * @return
	 * 	<jk>true</jk> if request can proceed.
	 * 	Specify <jk>false</jk> if you're doing something like a redirection to a login page.
	 */
	public boolean guard(RestRequest req, RestResponse res) throws BasicHttpException {
		if (! isRequestAllowed(req))
			throw new Forbidden("Access denied by guard");
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the specified request can pass through this guard.
	 *
	 * @param req The servlet request.
	 * @return <jk>true</jk> if the specified request can pass through this guard.
	 */
	public abstract boolean isRequestAllowed(RestRequest req);
}
