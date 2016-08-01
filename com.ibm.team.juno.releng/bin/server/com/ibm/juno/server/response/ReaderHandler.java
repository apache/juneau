/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.response;

import java.io.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;

/**
 * Response handler for {@link Reader} objects.
 * <p>
 * Simply pipes the contents of the {@link Reader} to {@link RestResponse#getNegotiatedWriter()}.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ReaderHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof Reader) {
			Writer w = res.getNegotiatedWriter();
			IOPipe.create(output, w).closeOut().run();
			return true;
		}
		return false;
	}
}

