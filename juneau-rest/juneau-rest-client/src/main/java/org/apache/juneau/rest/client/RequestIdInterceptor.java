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
package org.apache.juneau.rest.client;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;

/**
 * Built-in {@link RestCallInterceptor} that auto-sends and captures the {@code X-Request-Id} correlation id.
 *
 * <p>
 * Registered by default on every {@link RestClient} (opt out via {@link RestClient.Builder#sendRequestId(boolean)}):
 * <ul>
 * 	<li><b>{@code onInit}</b> &mdash; if the request already carries an {@code X-Request-Id} header (caller-supplied,
 * 		case-insensitive), it is left untouched and captured as the sent id; otherwise a fresh
 * 		{@linkplain Uuid7 version-7 UUID} is minted and set.  The sent id is stored on the request
 * 		({@link RestRequest#getRequestId()}) so debug emission can read it as a field &mdash; never via a live
 * 		log-context scope that may not survive to emit time.
 * 	<li><b>{@code onConnect}</b> &mdash; the server's echoed {@code X-Request-Id} header is captured on the response
 * 		({@link RestResponse#getRequestId()}) as the effective id for this exchange (it may differ from the sent id
 * 		if the server sanitized it).
 * </ul>
 *
 * @since 10.0.0
 */
class RequestIdInterceptor implements RestCallInterceptor {

	@Override /* Overridden from RestCallInterceptor */
	public void onInit(RestRequest req) {
		var existing = req.peekRequestIdHeader();
		if (existing != null && ! existing.isEmpty())
			req.setSentRequestId(existing);
		else
			req.requestId(Uuid7.createString());
	}

	@Override /* Overridden from RestCallInterceptor */
	public void onConnect(RestRequest req, RestResponse res) {
		if (res == null)
			return;
		var h = res.getFirstHeader(RequestIdConstants.HEADER);
		if (h != null) {
			var v = h.value();
			if (v != null && ! v.isEmpty())
				res.setEchoedRequestId(v);
		}
	}
}
