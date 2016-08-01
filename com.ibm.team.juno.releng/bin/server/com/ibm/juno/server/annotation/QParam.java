/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.server.*;

/**
 * Identical to {@link Param @Param}, but only retrieves the parameter from the
 * 	URL string, not URL-encoded form posts.
 * <p>
 * Unlike {@link Param @Param}, using this annotation does not result in the servlet reading the contents
 * 	of URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Content @Content} annotation
 * 	or {@link RestRequest#getInput(Class)} method for <code>application/x-www-form-urlencoded POST</code> calls.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 				<ja>@QParam</ja>(<js>"p1"</js>) <jk>int</jk> p1, <ja>@QParam</ja>(<js>"p2"</js>) String p2, <ja>@QParam</ja>(<js>"p3"</js>) UUID p3) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>int</jk> p1 = req.getQueryParameter(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String p2 = req.getQueryParameter(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID p3 = req.getQueryParameter(UUID.<jk>class</jk>, <js>"p3"</js>);
 * 		...
 * 	}
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface QParam {

	/**
	 * URL parameter name.
	 */
	String value();

	/**
	 * Specify <jk>true</jk> if using multi-part parameters to represent collections and arrays.
	 * <p>
	 * 	Normally, we expect single parameters to be specified in UON notation for representing
	 * 	collections of values (e.g. <js>"&key=(1,2,3)"</js>.
	 * 	This annotation allows the use of multi-part parameters to represent collections
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js>.
	 * <p>
	 *		This setting should only be applied to Java parameters of type array or Collection.
	 */
	boolean multipart() default false;
}
