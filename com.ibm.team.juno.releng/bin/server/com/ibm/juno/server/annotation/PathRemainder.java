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

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify it as the URL parameter remainder after a path pattern match.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foo/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res, <ja>@PathRemainder</ja> String remainder) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/foo/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		String remainder = req.getPathRemainder();
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
public @interface PathRemainder {}
