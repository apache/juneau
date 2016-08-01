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
 * Response handler for {@link Redirect} objects.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RedirectHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof Redirect) {
			Redirect r = (Redirect)output;
			String uri = r.toUrl(res.getUrlEncodingSerializer());
			if (StringUtils.isEmpty(uri))
				uri = req.getServletURI();
			else {
				char c = (uri.length() > 0 ? uri.charAt(0) : 0);
				if (c != '/' && uri.indexOf("://") == -1)
					uri = req.getServletURIBuilder().append('/').append(uri).toString();
			}
			int rc = r.getHttpResponseCode();
			if (rc != 0)
				res.setStatus(rc);   // TODO - This may get ignored by the call below.
			res.sendRedirect(uri);
			return true;
		}
		return false;
	}
}
