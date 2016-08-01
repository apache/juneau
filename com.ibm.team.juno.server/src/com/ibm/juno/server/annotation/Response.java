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
 * Annotation used in conjunction with {@link RestMethod#responses()} to identify possible responses by the method.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"*"</js>,
 * 		responses={
 * 			<ja>@Response</ja>(value=200,description=<js>"Everything was great."</js>),
 * 			<ja>@Response</ja>(value=404,description=<js>"File was not found."</js>)
 * 			<ja>@Response</ja>(500),
 * 		}
 * 	)
 * 	<jk>public void</jk> doAnything(RestRequest req, RestResponse res, <ja>@Method</ja> String method) {
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
public @interface Response {

	/**
	 * HTTP response code.
	 */
	int value();

	/**
	 * Optional description.
	 * <p>
	 * 	The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"myMethod.res.[code] = foo"</js> or <js>"MyServlet.myMethod.res.[code] = foo"</js>).
	 * <p>
	 * 	This field can contain variables (e.g. "$L{my.localized.variable}").
	 * 	See {@link RestServlet#createRequestVarResolver(RestRequest)}.
	 */
	String description() default "";

	/**
	 * Optional response variables.
	 * <p>
	 * 	Response variables can also be defined in the servlet resource bundle.
	 * 	(e.g. <js>"myMethod.res.[code].[category].[name] = foo"</js> or <js>"MyServlet.myMethod.res.[code].[category].[name] = foo"</js>).
	 */
	Var[] output() default {};
}
