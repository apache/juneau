/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import com.ibm.juno.server.annotation.*;

/**
 * Class used for defining method-level matchers using the {@link RestMethod#matchers()} annotation.
 * <p>
 * Matchers are used to allow multiple Java methods to handle requests assigned to the same
 * 	URL path pattern, but differing based on some request attribute, such as a specific header value.
 * For example, matchers can be used to provide two different methods for handling requests
 * 	from two different client versions.
 * <p>
 * Java methods with matchers associated with them are always attempted before Java methods
 * 	without matchers.
 * This allows a 'default' method to be defined to handle requests where no matchers match.
 * <p>
 * When multiple matchers are specified on a method, only one matcher is required to match.
 * This is opposite from the {@link RestMethod#guards()} annotation, where all guards
 * 	are required to match in order to execute the method.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foo"</js>, matchers=IsDNT.<jk>class</jk>)
 * 		<jk>public</jk> Object doGetWithDNT() {
 * 			<jc>// Handle request with Do-Not-Track specified</jc>
 * 		}
 *
 * 		<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foo"</js>)
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
 */
public abstract class RestMatcher {

	/**
	 * Returns <jk>true</jk> if the specified request matches this matcher.
	 *
	 * @param req The servlet request.
	 * @return <jk>true</jk> if the specified request matches this matcher.
	 */
	public abstract boolean matches(RestRequest req);
}
