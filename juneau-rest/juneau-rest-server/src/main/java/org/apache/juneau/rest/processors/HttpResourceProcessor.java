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
package org.apache.juneau.rest.processors;

import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.http.*;
import org.apache.juneau.http.resource.*;

/**
 * Response handler for {@link HttpResource} objects.
 */
public final class HttpResourceProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestCall call) throws IOException {

		RestResponse res = call.getRestResponse();
		HttpResource r = res.getOutput(HttpResource.class);

		if (r == null)
			return NEXT;

		res.setHeader(r.getContentType());
		res.setHeader(r.getContentEncoding());
		long contentLength = r.getContentLength();
		if (contentLength >= 0)
			res.setHeader(contentLength(contentLength));

		List<Header> allHeaders = r.getAllHeaders();
		for (int i = 0; i < allHeaders.size() ; i++) // Avoids Iterator creation.
			res.addHeader(allHeaders.get(i));

		try (OutputStream os = res.getNegotiatedOutputStream()) {
			r.writeTo(os);
			os.flush();
		}

		return FINISHED;
	}
}

