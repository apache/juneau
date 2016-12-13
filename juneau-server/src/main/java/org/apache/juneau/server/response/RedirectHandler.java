// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.server.response;

import java.io.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.server.*;

/**
 * Response handler for {@link Redirect} objects.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
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
