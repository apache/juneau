/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.processor;

import java.io.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;

/**
 * Response handler for {@link HttpResponseMessage} objects (transport-neutral replacement for {@code HttpResponse}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class HttpResponseProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		var res = opSession.getResponse();
		var r = res.getContent(HttpResponseMessage.class);

		if (r == null) {
			var raw = res.getContent(Object.class);
			if (LegacyHttpResponseAdapter.isLegacyResponse(raw))
				r = LegacyHttpResponseAdapter.adapt(raw);
			else
				return NEXT;
		}

		opSession.status(r.getStatusLine());

		var body = r.getBody();
		if (body != null) {
			var ct = body.getContentType();
			if (ct != null)
				res.setHeader(ContentType.of(ct));
			var contentLength = body.getContentLength();
			if (contentLength >= 0)
				res.setHeader(ContentLength.of(contentLength));
		}

		r.getHeaders().forEach(res::addHeader);

		if (body != null) {
			try (var os = res.getNegotiatedOutputStream()) {
				body.writeTo(os);
				os.flush();
			}
		}

		return FINISHED;
	}
}
