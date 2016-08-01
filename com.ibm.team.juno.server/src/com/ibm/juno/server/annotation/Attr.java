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
 * 	to identify it as a variable in a URL path pattern converted to a POJO.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Attr</ja> String foo, <ja>@Attr</ja> <jk>int</jk> bar, <ja>@Attr</ja> UUID baz) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	The <ja>@Attr</ja> annotation is optional if the parameters are specified immediately
 * 	following the <code>RestRequest</code> and <code>RestResponse</code> parameters,
 * 	and are specified in the same order as the variables in the URL path pattern.
 * 	The following example is equivalent to the previous example.
 * </p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			String foo, <jk>int</jk> bar, UUID baz) {
 * 		...
 * 	}
 * </p>
 * <p>
 * 	If the order of parameters is not the default order shown above, the
 * 	attribute names must be specified (since parameter names are lost during compilation).
 * 	The following example is equivalent to the previous example, except
 * 	the parameter order has been switched, requiring the use of the <ja>@Attr</ja>
 * 	annotations.
 * <p>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Attr</ja>(<js>"baz"</js>) UUID baz, <ja>@Attr</ja>(<js>"foo"</js>) String foo, <ja>@Attr</ja>(<js>"bar"</js>) <jk>int</jk> bar) {
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
public @interface Attr {

	/**
	 * URL variable name.
	 * <p>
	 * 	Optional if the attributes are specified in the same order as in the URL path pattern.
	 */
	String value() default "";
}
