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

import java.io.*;
import java.lang.annotation.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify it as the HTTP request body converted to a POJO.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(RestRequest req, RestResponse res, <ja>@Content</ja> Person person) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(RestRequest req, RestResponse res) {
 * 		Person person = req.getInput(Person.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 * <p>
 * 	{@link Reader Readers} and {@link InputStream InputStreams} can also be specified as content parameters.
 * 	When specified, any registered parsers are bypassed.
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(<ja>@Header</ja> String mediaType, <ja>@Content</ja> InputStream input) {
 * 		...
 * 	}
 * </p>
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Content {}
