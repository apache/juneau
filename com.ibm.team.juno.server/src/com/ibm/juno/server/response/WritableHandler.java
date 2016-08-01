/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.response;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.server.*;

/**
 * Response handler for {@link Writable} and {@link ReaderResource} objects.
 * <p>
 * Uses the {@link Writable#writeTo(Writer)} method to send the contents to the {@link RestResponse#getNegotiatedWriter()} writer.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class WritableHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof Writable) {
			if (output instanceof ReaderResource) {
				ReaderResource r = (ReaderResource)output;
				String mediaType = r.getMediaType();
				if (mediaType != null)
					res.setContentType(mediaType);
				for (Map.Entry<String,String> h : r.getHeaders().entrySet())
					res.setHeader(h.getKey(), h.getValue());
			}
			Writer w = res.getNegotiatedWriter();
			((Writable)output).writeTo(w);
			w.flush();
			w.close();
			return true;
		}
		return false;
	}
}

