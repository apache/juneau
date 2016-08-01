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
import java.util.*;

import com.ibm.juno.core.utils.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify it as the resource bundle for the request locale.
 * <p>
 * 	Parameter type must be either {@link ResourceBundle} or {@link SafeResourceBundle}.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public</jk> String doGet(<ja>@Messages</ja> ResourceBundle messages) {
 * 		<jk>return</jk> messages.getString(<js>"myLocalizedMessage"</js>);
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public</jk> String doGet(RestRequest req) {
 * 		<jk>return</jk> req.getMessage(<js>"myLocalizedMessage"</js>);
 * 	}
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Messages {}
