/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.converters;

import static javax.servlet.http.HttpServletResponse.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;

/**
 * Converter for enablement of {@link PojoIntrospector} support on response objects returned by a <code>@RestMethod</code> method.
 * <p>
 * 	When enabled, public methods can be called on objects returned through the {@link RestResponse#setOutput(Object)} method.
 * <p>
 * 	Note that opening up public methods for calling through a REST interface can be dangerous, and should
 * 	be done with caution.
 * <p>
 * 	Java methods are invoked by passing in the following URL parameters:
 * <ul>
 * 	<li><code>&amp;invokeMethod</code> - The Java method name, optionally with arguments if necessary to differentiate between methods.
 * 	<li><code>&amp;invokeArgs</code> - The arguments as a JSON array.
 * </ul>
 * <p>
 * 	See {@link PojoIntrospector} for additional information on introspecting POJO methods.
 */
public final class Introspectable implements RestConverter {

	@Override /* RestConverter */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Object convert(RestRequest req, Object o, ClassMeta cm) throws RestException {
		String method = req.getParameter("invokeMethod");
		String args = req.getParameter("invokeArgs");
		if (method == null)
			return o;
		try {
			if (cm.getPojoFilter() != null)
				o = cm.getPojoFilter().filter(o);
			return new PojoIntrospector(o, JsonParser.DEFAULT).invokeMethod(method, args);
		} catch (Exception e) {
			e.printStackTrace();
			return new RestException(SC_INTERNAL_SERVER_ERROR,
				"Error occurred trying to invoke method: {0}",
				e.getLocalizedMessage()
			).initCause(e);
		}
	}
}
