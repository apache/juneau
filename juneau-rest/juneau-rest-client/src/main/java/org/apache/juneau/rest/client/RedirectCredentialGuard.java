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

import java.net.*;

import org.apache.http.*;
import org.apache.http.protocol.*;

/**
 * Request interceptor that removes caller-set credential headers before a request is replayed against a
 * different origin as the result of a {@code 3xx} redirect.
 *
 * <p>
 * The redirect executor copies the original request's headers &mdash; including {@code Authorization} and
 * {@code Cookie} &mdash; onto the follow-up request aimed at the {@code Location} target.  When that target is
 * a different scheme, host, or port (or an {@code https}&rarr;{@code http} downgrade), forwarding those
 * headers would disclose credentials to an unrelated origin.  This interceptor runs on every outgoing request
 * in an exchange: it records the origin of the first request and, on any later request in the same exchange
 * whose origin differs, strips the credential headers before the request leaves the client.
 *
 * <p>
 * The forward/strip decision and the header set are delegated to {@link RedirectSecurity}.
 */
final class RedirectCredentialGuard implements HttpRequestInterceptor {

	private static final String ATTR_ORIGIN = RedirectCredentialGuard.class.getName() + ".origin";

	@Override /* HttpRequestInterceptor */
	public void process(HttpRequest request, HttpContext context) {
		var target = HttpCoreContext.adapt(context).getTargetHost();
		if (target == null)
			return;
		var current = originOf(target);
		if (current == null)
			return;
		var origin = (URI)context.getAttribute(ATTR_ORIGIN);
		if (origin == null) {
			context.setAttribute(ATTR_ORIGIN, current);
			return;
		}
		if (RedirectSecurity.shouldStripCredentials(origin, current))
			for (var name : RedirectSecurity.stripOnCrossOrigin())
				request.removeHeaders(name);
	}

	private static URI originOf(HttpHost host) {
		try {
			return URI.create(host.toURI());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
