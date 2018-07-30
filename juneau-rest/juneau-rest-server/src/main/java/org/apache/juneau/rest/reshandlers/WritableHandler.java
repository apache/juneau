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
package org.apache.juneau.rest.reshandlers;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;

/**
 * Response handler for {@link Writable} and {@link ReaderResource} objects.
 *
 * <p>
 * Uses the {@link Writable#writeTo(Writer)} method to send the contents to the {@link RestResponse#getNegotiatedWriter()} writer.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.RestMethod.MethodReturnTypes">Overview &gt; juneau-rest-server &gt; Method Return Types</a>
 * </ul>
 */
public final class WritableHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res) throws IOException, NotAcceptable, RestException {
		if (res.isOutputType(Writable.class)) {
			if (res.isOutputType(ReaderResource.class)) {
				ReaderResource r = res.getOutput(ReaderResource.class);
				MediaType mediaType = r.getMediaType();
				if (mediaType != null)
					res.setContentType(mediaType.toString());
				for (Map.Entry<String,Object> h : r.getHeaders().entrySet())
					res.setHeader(h.getKey(), asString(h.getValue()));
			}
			try (Writer w = res.getNegotiatedWriter()) {
				res.getOutput(Writable.class).writeTo(w);
			}
			return true;
		}
		return false;
	}
}

