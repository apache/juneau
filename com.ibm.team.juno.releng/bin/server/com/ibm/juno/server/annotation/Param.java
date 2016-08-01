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
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify it as a URL query parameter converted to a POJO.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 				<ja>@Param</ja>(<js>"p1"</js>) <jk>int</jk> p1, <ja>@Param</ja>(<js>"p2"</js>) String p2, <ja>@Param</ja>(<js>"p3"</js>) UUID p3) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>int</jk> p1 = req.getParam(<jk>int</jk>.<jk>class</jk>, <js>"p1"</js>, 0);
 * 		String p2 = req.getParam(String.<jk>class</jk>, <js>"p2"</js>);
 * 		UUID p3 = req.getParam(UUID.<jk>class</jk>, <js>"p3"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <h6 class='topic'>Important note concerning FORM posts</h6>
 * <p>
 * This annotation should not be combined with the {@link Content @Content} annotation or {@link RestRequest#getInput(Class)} method
 * 	for <code>application/x-www-form-urlencoded POST</code> posts, since it will trigger the underlying servlet
 * 	API to parse the body content as key-value pairs resulting in empty content.
 * <p>
 * The {@link QParam @QParam} annotation can be used to retrieve a URL parameter
 * 	in the URL string without triggering the servlet to drain the body content.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Param {

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
