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
package org.apache.juneau.rest.client.apachehttpclient45;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.*;

/**
 * Request interceptor that removes caller-set credential headers before a request is replayed against a
 * different origin as the result of a {@code 3xx} redirect.
 *
 * <p>
 * Apache HttpClient's redirect executor copies the original request's headers &mdash; including
 * {@code Authorization} and {@code Cookie} &mdash; onto the follow-up request aimed at the {@code Location}
 * target.  When that target is a different scheme, host, or port (or an {@code https}&rarr;{@code http}
 * downgrade), forwarding those headers would disclose credentials to an unrelated origin.  This interceptor
 * runs on every outgoing request in an exchange: it records the origin of the first request and, on any
 * later request in the same exchange whose origin differs, strips the credential headers before the request
 * leaves the client.
 *
 * <p>
 * The origin is compared on HttpClient's own {@link HttpHost} (scheme, host and effective port), with the same
 * semantics as {@link RedirectSecurity#shouldStripCredentials(java.net.URI, java.net.URI)}.  {@link java.net.URI}
 * cannot be used here: it parses a host name that HttpClient accepts but RFC 2396 does not (for example
 * {@code my_service}, a typical Docker Compose service name) with a {@code null} host, which would make every
 * redirect from such a host fail.  The credential header set is delegated to {@link RedirectSecurity#stripOnCrossOrigin()}
 * so that all transports share it.
 */
final class Hc45RedirectCredentialGuard implements HttpRequestInterceptor {

	private static final String ATTR_ORIGIN = Hc45RedirectCredentialGuard.class.getName() + ".origin";

	@Override /* HttpRequestInterceptor */
	public void process(HttpRequest request, HttpContext context) {
		var target = HttpCoreContext.adapt(context).getTargetHost();
		if (target == null)
			return;
		var origin = (HttpHost)context.getAttribute(ATTR_ORIGIN);
		if (origin == null) {
			context.setAttribute(ATTR_ORIGIN, target);
			return;
		}
		if (! sameOrigin(origin, target))
			for (var name : RedirectSecurity.stripOnCrossOrigin())
				request.removeHeaders(name);
	}

	// HttpHost always has a scheme name and a host name; the port is -1 when none was given, so it is normalized to the
	// scheme default.  An https->http downgrade changes the scheme, so it is never same-origin.
	private static boolean sameOrigin(HttpHost from, HttpHost to) {
		return eqic(from.getSchemeName(), to.getSchemeName())
			&& eqic(from.getHostName(), to.getHostName())
			&& effectivePort(from) == effectivePort(to);
	}

	private static int effectivePort(HttpHost host) {
		var port = host.getPort();
		if (port != -1)
			return port;
		return switch (host.getSchemeName().toLowerCase(Locale.ROOT)) {
			case "https" -> 443;
			case "http" -> 80;
			default -> -1;
		};
	}
}
