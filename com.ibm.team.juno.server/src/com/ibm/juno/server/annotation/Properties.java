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

import com.ibm.juno.core.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method
 * 	to identify the request-duration properties object for the current request.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public Person</jk> doGetPerson(<ja>@Properties</ja> ObjectMap properties) {
 * 		properties.put(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 * <p>
 * 	This is functionally equivalent to the following code...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public Person</jk> doGetPerson(RestResponse res) {
 * 		ObjectMap properties = res.getProperties();
 * 		properties.put(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 * <p>
 * 	...or this...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public Person</jk> doGetPerson(RestResponse res) {
 * 		res.setProperty(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 * <p>
 * 	The parameter type can be one of the following:
 * 	<ul>
 * 		<li>{@link ObjectMap}
 * 		<li><code>Map&lt;String,Object&gt;</code>
 * 	</ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Properties {}
