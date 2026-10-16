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
import org.apache.http.client.protocol.*;
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
 * later request in the same exchange whose origin differs (and on every request after one that did, even if it
 * returns to the first origin), strips the credential headers before the request
 * leaves the client.
 *
 * <p>
 * The origin is compared on HttpClient's own {@link HttpHost} (scheme, host and effective port), with the same
 * semantics as {@link RedirectSecurity#shouldStripCredentials(java.net.URI, java.net.URI)}.  {@link java.net.URI}
 * cannot be used here: it parses a host name that HttpClient accepts but RFC 2396 does not (for example
 * {@code my_service}, a typical Docker Compose service name) with a {@code null} host, which would make every
 * redirect from such a host fail.  The credential header set is delegated to {@link RedirectSecurity#stripOnCrossOrigin()}
 * so that all transports share it.
 *
 * <p>
 * A caller-supplied client is immutable once built, so this interceptor cannot be added to it.  For such a client the
 * transport passes {@code newGuardingContext()} with each request instead; see {@code GuardingContext}.
 */
final class Hc45RedirectCredentialGuard implements HttpRequestInterceptor {

	private static final String ATTR_ORIGIN = Hc45RedirectCredentialGuard.class.getName() + ".origin";
	private static final String ATTR_STRIPPED = Hc45RedirectCredentialGuard.class.getName() + ".stripped";

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
		// Sticky: HttpClient copies each hop's headers from the caller's original request, so once any hop left the first origin,
		// every later hop (including one that returns to it) must be stripped too.
		if (context.getAttribute(ATTR_STRIPPED) == null && sameOrigin(origin, target))
			return;
		context.setAttribute(ATTR_STRIPPED, Boolean.TRUE);
		for (var name : RedirectSecurity.stripOnCrossOrigin())
			request.removeHeaders(name);
	}

	/**
	 * Returns a new per-request context that applies this guard to every hop of the exchange it is used for.
	 *
	 * @return A new context. Never <jk>null</jk>.
	 */
	static HttpClientContext newGuardingContext() {
		return new GuardingContext();
	}

	/**
	 * Per-request context that applies the guard to a caller-supplied client without changing that client.
	 *
	 * <p>
	 * HttpClient's protocol stage records each pass over a hop in the context (its target host, then the request) before it
	 * runs the client's own request interceptors, so this context strips the credential headers at that point.  The
	 * headers on a redirect hop at that point are the ones the redirect executor copied from the original request, which
	 * are the caller's.  The client's interceptors and authenticator run afterwards, so a {@code Cookie} from the client's
	 * {@code CookieStore} and an {@code Authorization} from its {@code CredentialsProvider} are still sent to the new
	 * origin.
	 *
	 * <p>
	 * A hop can be passed more than once: HttpClient's retry executors ({@code RetryExec} after an I/O failure,
	 * {@code ServiceUnavailableRetryExec} after a 503) restore the headers they captured <em>before</em> the protocol stage
	 * ran -- the caller's -- and run the protocol stage again on the same request object.  So the guard must run on every
	 * pass, not once per request object.  A pass is recognised by the protocol stage setting the target host immediately
	 * before the request; the {@code MainClientExec} sets the request again just before sending (and for each auth
	 * challenge round), after the client's interceptors added their headers, and that one must not be stripped.  A
	 * {@code CONNECT} tunnel request carries no caller headers.
	 *
	 * <p>
	 * Headers the caller's client adds itself, such as {@code setDefaultHeaders(...)}, are client configuration rather than
	 * caller-set request headers and are deliberately left alone, like the cookie store and credentials provider.
	 */
	private static final class GuardingContext extends HttpClientContext {

		private final Hc45RedirectCredentialGuard guard = new Hc45RedirectCredentialGuard();
		private boolean protocolPass;

		@Override /* HttpCoreContext */
		public void setAttribute(String id, Object obj) {
			super.setAttribute(id, obj);
			if (HttpCoreContext.HTTP_TARGET_HOST.equals(id))
				protocolPass = true;
			else if (HttpCoreContext.HTTP_REQUEST.equals(id) && protocolPass && obj instanceof HttpRequest request) {
				protocolPass = false;
				guard.process(request, this);
			}
		}
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
