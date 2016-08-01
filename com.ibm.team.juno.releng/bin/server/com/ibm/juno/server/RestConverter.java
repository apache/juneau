/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.converters.*;

/**
 * REST method response converter.
 * <p>
 * 	Implements a filter mechanism for REST method calls that allows response objects to be
 * 	converted to some other POJO after invocation of the REST method.
 * <p>
 * 	Converters are associated with REST methods through the {@link RestMethod#converters()} annotation.
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jk>public class</jk> RequestEchoResource <jk>extends</jk> RestServlet {
 *
 * 		<jc>// GET request handler</jc>
 * 		<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/*"</js>, converters={Queryable.<jk>class</jk>,Traversable.<jk>class</jk>})
 * 		<jk>public</jk> HttpServletRequest doGet(RestRequest req) {
 * 			res.setTitle(<js>"Contents of HttpServletRequest object"</js>);
 * 			<jk>return</jk> req;
 * 		}
 * 	}
 * </p>
 * <p>
 * 	Converters can also be associated at the servlet level using the {@link RestResource#converters()} annotation.
 * 	Applying converters at the resource level is equivalent to applying converters to each resource method individually.
 *
 * <h6 class='topic'>How to implement</h6>
 * <p>
 * 	Implementers should simply implement the {@link #convert(RestRequest, Object, ClassMeta)} and
 * 		return back a 'converted' object.
 * 	It's up to the implementer to decide what this means.
 * <p>
 * 	Converters must implement a no-args constructor.
 *
 * <h6 class='topic'>Predefined converters</h6>
 * <p>
 * 	The following converters are available by default.
 * <ul>
 * 	<li>{@link Traversable} - Allows URL additional path info to address individual elements in a POJO tree.
 * 	<li>{@link Queryable} - Allows query/view/sort functions to be performed on POJOs.
 * 	<li>{@link Introspectable} - Allows Java public methods to be invoked on the returned POJOs.
 * </ul>
 */
public interface RestConverter {

	/**
	 * Performs post-call conversion on the specified response object.
	 *
	 * @param req The servlet request.
	 * @param res The response object set by the REST method through the {@link RestResponse#setOutput(Object)} method.
	 * @param cm The {@link ClassMeta} on the object from the bean context of the servlet.
	 * 	Can be used to check if the object has any filters.
	 * @return The converted object.
	 * @throws RestException Thrown if any errors occur during conversion.
	 * @throws SerializeException
	 */
	public Object convert(RestRequest req, Object res, ClassMeta<?> cm) throws RestException, SerializeException;
}
