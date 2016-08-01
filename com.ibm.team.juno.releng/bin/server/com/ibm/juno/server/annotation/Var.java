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
 * Annotation used in conjunction with {@link RestMethod#input()} and {@link Response#output()} to identify content and header descriptions
 * 	on specific method responses.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"*"</js>,
 * 		requestVars={
 * 				<ja>@Var</ja>(category=<js>"header"</js>,name=<js>"Range"</js>,description=<js>"$L{ContentRange.description}"</js>)
 * 		}
 * 		responses={
 * 			<ja>@Response</ja>(code=200,description=<js>"Everything was great."</js>,
 * 				responseVars={
 * 					<ja>@Var</ja>(category=<js>"header"</js>,name=<js>"Content-Range"</js>,description=<js>"$L{ContentRange.description}"</js>)
 * 				})
 * 			<ja>@Response</ja>(code=404,description=<js>"File was not found."</js>)
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
public @interface Var {

	/**
	 * Variable category (e.g. <js>"header"</js>, <js>"content"</js>).
	 * The {@link VarCategory} class contains predefined constants.
	 */
	String category();

	/**
	 * Variable name (e.g. <js>"Content-Range"</js>).
	 */
	String name() default "";

	/**
	 * Variable description (e.g. <js>"Indicates the range returned when Range header is present in the request"</js>).
	 * <p>
	 * 	The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"myMethod.res.[code].[category].[name] = foo"</js> or <js>"MyServlet.myMethod.res.[code].[category].[name] = foo"</js>).
	 */
	String description() default "";
}
