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
package org.apache.juneau.rest.processor;

import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.http.*;

/**
 * Response handler for {@link HttpEntity} objects.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.ResponseProcessors}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class HttpEntityProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		RestResponse res = opSession.getResponse();
		HttpEntity e = res.getOutput(HttpEntity.class);

		if (e == null)
			return NEXT;

		res.setHeader(e.getContentType());
		res.setHeader(e.getContentEncoding());
		long contentLength = e.getContentLength();
		if (contentLength >= 0)
			res.setHeader(contentLength(contentLength));

		try (OutputStream os = res.getNegotiatedOutputStream()) {
			e.writeTo(os);
			os.flush();
		}

		return FINISHED;
	}
}

