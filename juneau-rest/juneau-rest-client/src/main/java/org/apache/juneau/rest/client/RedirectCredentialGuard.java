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

import java.util.*;

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
 * The forward/strip decision follows {@link RedirectSecurity}: only an exact scheme/host/port match forwards, and an
 * {@code https}&rarr;{@code http} downgrade strips.  Origins are compared directly on the {@link HttpHost}, so an
 * unusual host name (for example one containing an underscore) never causes an exception; when the host cannot be
 * determined the guard fails closed and strips.  The header set is {@link RedirectSecurity#stripOnCrossOrigin()}
 * plus any names supplied at construction.
 */
final class RedirectCredentialGuard implements HttpRequestInterceptor {

	private static final String ATTR_ORIGIN = RedirectCredentialGuard.class.getName() + ".origin";

	private final Set<String> stripHeaders;

	/**
	 * Constructor.
	 *
	 * @param additionalHeaders Additional header names to strip on a cross-origin redirect.  Can be <jk>null</jk>.
	 */
	RedirectCredentialGuard(Collection<String> additionalHeaders) {
		var l = new LinkedHashSet<>(RedirectSecurity.stripOnCrossOrigin());
		if (additionalHeaders != null)
			l.addAll(additionalHeaders);
		stripHeaders = Collections.unmodifiableSet(l);
	}

	/**
	 * Starts a new exchange on a possibly reused {@link HttpContext}.
	 *
	 * <p>
	 * Clears the origin recorded by a previous call so the next request is treated as the start of a fresh exchange.
	 *
	 * @param context The context.  Can be <jk>null</jk>.
	 * @return The previously recorded origin (to be passed to {@link #exit(HttpContext, Object)}), or <jk>null</jk>.
	 */
	static Object enter(HttpContext context) {
		if (context == null)
			return null;
		var previous = context.getAttribute(ATTR_ORIGIN);
		context.removeAttribute(ATTR_ORIGIN);
		return previous;
	}

	/**
	 * Ends an exchange started with {@link #enter(HttpContext)}, restoring the origin that was recorded before it.
	 *
	 * @param context The context.  Can be <jk>null</jk>.
	 * @param previous The value returned by {@link #enter(HttpContext)}.
	 */
	static void exit(HttpContext context, Object previous) {
		if (context == null)
			return;
		if (previous == null)
			context.removeAttribute(ATTR_ORIGIN);
		else
			context.setAttribute(ATTR_ORIGIN, previous);
	}

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
		if (shouldStrip(origin, target))
			for (var name : stripHeaders)
				request.removeHeaders(name);
	}

	private static boolean shouldStrip(HttpHost from, HttpHost to) {
		var fromHost = from.getHostName();
		var toHost = to.getHostName();
		if (fromHost == null || toHost == null)
			return true;  // Cannot establish the origin; fail closed.
		var fromScheme = from.getSchemeName();
		var toScheme = to.getSchemeName();
		if ("https".equalsIgnoreCase(fromScheme) && "http".equalsIgnoreCase(toScheme))
			return true;
		return ! (fromScheme.equalsIgnoreCase(toScheme) && fromHost.equalsIgnoreCase(toHost) && port(from) == port(to));
	}

	private static int port(HttpHost h) {
		var p = h.getPort();
		if (p != -1)
			return p;
		var scheme = h.getSchemeName();
		if ("https".equalsIgnoreCase(scheme))
			return 443;
		if ("http".equalsIgnoreCase(scheme))
			return 80;
		return -1;
	}
}
