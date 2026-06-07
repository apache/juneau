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

import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.server.*;

/**
 * Response handler for {@link HttpBody} objects (transport-neutral replacement for the historical
 * Apache HttpClient 4.5 {@code HttpEntity}-keyed processor).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class HttpBodyProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		var res = opSession.getResponse();
		var e = res.getContent(HttpBody.class);

		if (e == null) {
			var raw = res.getContent(Object.class);
			if (LegacyHttpResponseAdapter.isLegacyEntity(raw)) {
				e = LegacyHttpResponseAdapter.adaptEntity(raw);
				LegacyHttpResponseAdapter.copyResourceHeaders(raw, res::addHeader);
			} else {
				return NEXT;
			}
		}

		var ct = e.getContentType();
		if (ct != null)
			res.setHeader(ContentType.of(ct));
		var contentLength = e.getContentLength();
		if (contentLength >= 0)
			res.setHeader(ContentLength.of(contentLength));

		try (var os = res.getNegotiatedOutputStream()) {
			e.writeTo(os);
			os.flush();
		}

		return FINISHED;
	}
}
