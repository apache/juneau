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
 * Identical to {@link HasParam @HasParam}, but only checks the existing of the parameter in the
 * 	URL string, not URL-encoded form posts.
 * <p>
 * Unlike {@link HasParam @HasParam}, using this annotation does not result in the servlet reading the contents
 * 	of URL-encoded form posts.
 * Therefore, this annotation can be used in conjunction with the {@link Content @Content} annotation
 * 	or {@link RestRequest#getInput(Class)} method for <code>application/x-www-form-urlencoded POST</code> calls.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doPost(<ja>@HasQParam</ja>(<js>"p1"</js>) <jk>boolean</jk> p1, <ja>@Content</ja> Bean myBean) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req) {
 * 		<jk>boolean</jk> p1 = req.hasQueryParameter(<js>"p1"</js>);
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
public @interface HasQParam {

	/**
	 * URL parameter name.
	 */
	String value();
}
