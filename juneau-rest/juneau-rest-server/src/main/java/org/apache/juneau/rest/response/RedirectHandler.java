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
import org.apache.juneau.rest.helper.*;

/**
 * Response handler for {@link Redirect} objects.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="juneau-rest-server &gt; " href="../../../../../overview-summary.html#juneau-rest-server.RestMethod.MethodReturnTypes">Overview &gt; Method Return Types</a>
 * </ul>
 */
public final class RedirectHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output instanceof Redirect) {
			Redirect r = (Redirect)output;
			String uri = req.getUriResolver().resolve(r.getURI());
			int rc = r.getHttpResponseCode();
			if (rc != 0)
				res.setStatus(rc);   // TODO - This may get ignored by the call below.
			res.sendRedirect(uri);
			return true;
		}
		return false;
	}
}
