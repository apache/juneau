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
package org.apache.juneau.rest.response;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Response handler for {@link InputStream} objects.
 * <p>
 * Simply pipes the contents of the {@link InputStream} to {@link RestResponse#getNegotiatedOutputStream()}.
 * <p>
 * Sets the <code>Content-Type</code> response header to whatever was set via {@link RestResponse#setContentType(String)}.
 * <p>
 * This handler is registered by default on {@link RestServlet RestServlets} via the
 * 	default implementation of the {@link RestServlet#createResponseHandlers} method.
 */
public final class InputStreamHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof InputStream) {
			res.setHeader("Content-Type", res.getContentType());
			OutputStream os = res.getNegotiatedOutputStream();
			IOPipe.create(output, os).closeOut().run();
			return true;
		}
		return false;
	}
}
