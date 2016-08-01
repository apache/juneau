/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import static javax.servlet.http.HttpServletResponse.*;

import com.ibm.juno.server.annotation.*;

/**
 * REST method guard.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Implements a guard mechanism for REST method calls that allows requests to be
 * 		rejected before invocation of the REST method.
 * 	For example, guards can be used to ensure that only administrators can call certain methods.
 * <p>
 * 	Guards are applied to REST methods declaratively through the {@link RestResource#guards()} or {@link RestMethod#guards()} annotations.
 * <p>
 * 	If multiple guards are specified, ALL guards must pass in order for the request to proceed.
 *
 *
 * <h6 class='topic'>How to implement</h6>
 * <p>
 * 	Typically, guards will be used for permissions checking on the user making the request,
 * 		but it can also be used for other purposes like pre-call validation of a request.
 * <p>
 * 	Implementers should simply throw a {@link RestException} from the {@link #guard(RestRequest, RestResponse)}
 * 		method to abort processing on the current request.
 * <p>
 * 	Guards must implement a no-args constructor.
 *
 *
 * <h6 class='topic'>Example usage</h6>
 * <p class='bcode'>
 * 	<jk>public</jk> MyResource <jk>extends</jk> RestServlet {
 *
 * 		<jc>// Delete method with guard that only allows Billy to call it.</jc>
 * 		<ja>@RestMethod</ja>(name=<js>"DELETE"</js>, guards=BillyGuard.<jk>class</jk>)
 * 		<jk>public</jk> doDelete(RestRequest req, RestResponse res) <jk>throws</jk> Exception {...}
 * 	}
 * </p>
 *
 *
 * <h6 class='topic'>Example implementation</h6>
 * <p class='bcode'>
 * 	<jc>// Define a guard that only lets Billy make a request</jc>
 * 	<jk>public</jk> BillyGuard <jk>extends</jk> RestGuard {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public boolean</jk> isRequestAllowed(RestRequest req) {
 * 			return req.getUserPrincipal().getName().contains(<js>"Billy"</js>);
 * 		}
 * 	}
 * </p>
 */
public abstract class RestGuard {

	/**
	 * Checks the current HTTP request and throws a {@link RestException} if the guard
	 * 	does not permit the request.
	 * <p>
	 * 	By default, throws an <jsf>SC_FORBIDDEN</jsf> exception if {@link #isRequestAllowed(RestRequest)}
	 * 	returns <jk>false</jk>.
	 * <p>
	 * 	Subclasses are free to override this method to tailor the behavior of how to handle unauthorized
	 * 	requests.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @throws RestException Thrown to abort processing on current request.
	 * @return <jk>true</jk> if request can proceed.
	 * 	Specify <jk>false</jk> if you're doing something like a redirection to a login page.
	 */
	public boolean guard(RestRequest req, RestResponse res) throws RestException {
		if (! isRequestAllowed(req))
			throw new RestException(SC_FORBIDDEN, "Access denied by guard");
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
