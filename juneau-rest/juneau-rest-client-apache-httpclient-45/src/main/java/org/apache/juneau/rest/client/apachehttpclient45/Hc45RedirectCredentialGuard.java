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

import java.util.*;

import org.apache.http.*;
import org.apache.http.client.protocol.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.*;

/**
 * Request interceptor that removes credential headers before a request is replayed against a
 * different origin as the result of a {@code 3xx} redirect.
 *
 * <p>
 * Apache HttpClient's redirect executor copies the original request's headers &mdash; including
 * {@code Authorization} and {@code Cookie} &mdash; onto the follow-up request aimed at the {@code Location}
 * target.  When that target is a different scheme, host, or port (or an {@code https}&rarr;{@code http}
 * downgrade), forwarding those headers would disclose credentials to an unrelated origin.  This interceptor
 * runs on every outgoing request in an exchange: it records the origin of the first request and strips the
 * credential headers from any later request in the same exchange whose origin differs from it.  A request that
 * returns to the first origin is not stripped, so it carries the original origin's credentials again.
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
 * This interceptor is not registered on any client.  The transport passes {@code newGuardingContext()} with every
 * request instead, so that transport-built and caller-supplied clients behave identically; see {@code GuardingContext}.
 * On a hop whose origin is not the first one every credential header is removed, including the cookie-store {@code Cookie} and the
 * credentials-provider or preemptive {@code Authorization} that HttpClient adds after this interceptor runs.
 */
final class Hc45RedirectCredentialGuard implements HttpRequestInterceptor {

	private static final String ATTR_ORIGIN = Hc45RedirectCredentialGuard.class.getName() + ".origin";
	private static final String ATTR_LEFT = Hc45RedirectCredentialGuard.class.getName() + ".left";
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
		// Always compared with the first origin, not the previous hop.  HttpClient copies each hop's headers from the caller's
		// original request (this only strips the hop's own copy), so a hop that returns to the first origin is left whole.
		// A hop that returns to the first origin gets the credentials back only if it is a GET or HEAD: a 307/308 replay of a
		// POST (or any other method) after leaving the origin would be a credentialed write the caller never asked A to see.
		if (sameOrigin(origin, target)) {
			if (context.getAttribute(ATTR_LEFT) == null || safeMethod(request)) {
				context.removeAttribute(ATTR_STRIPPED);
				return;
			}
		} else {
			context.setAttribute(ATTR_LEFT, Boolean.TRUE);
		}
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
	 * Per-request context that applies the guard without changing the client it is used with.
	 *
	 * <p>
	 * The transport sends every request with one of these, whether it built the client or the caller supplied it, so both
	 * behave identically.  On a hop that is not at the exchange's first origin (exact scheme, host and effective port), <em>every</em>
	 * credential header is removed from the request that goes out, including the ones HttpClient adds itself: a {@code Cookie} from the client's
	 * {@code CookieStore}, and an {@code Authorization} from its {@code CredentialsProvider}, its auth cache (preemptive
	 * auth) or the answer to a {@code 401} challenge.  The only exception is {@code Proxy-Authorization}, which is addressed
	 * to the proxy rather than the origin.  Headers the client adds that are not credentials, such as
	 * {@code setDefaultHeaders(...)}, are left alone.
	 *
	 * <p>
	 * A request passes through two points that matter, both visible as attributes set on the context.  HttpClient's protocol
	 * stage sets the target host and then the request just before it runs the client's own request interceptors; the guard
	 * strips the headers the redirect executor copied from the caller's original request there.  The {@code MainClientExec}
	 * then adds its authentication headers, and the connection sets the request one last time immediately before it is
	 * written to the wire; the guard strips again there, if the hop is not at the first origin.
	 *
	 * <p>
	 * The first origin is the one captured at the start of the exchange; each hop is compared with it, never with the previous
	 * hop.  A redirect chain that returns to the first origin (A&rarr;B&rarr;A, A&rarr;B&rarr;C&rarr;A) gets the original origin's credentials back
	 * on that hop, exactly as an unredirected request to it would: the caller's own headers (HttpClient rebuilds each hop from
	 * the original request), the cookie-store {@code Cookie} and the credentials-provider or preemptive {@code Authorization}.
	 * A returning hop gets them back only when its method is {@code GET} or {@code HEAD}: once the exchange has left the first origin, a hop back to it by any other method (a {@code 307}/{@code 308} replay of a {@code POST}) stays stripped; a hop that never left the origin is unaffected.  Every hop that is not at the first origin is stripped, so A&rarr;B&rarr;A&rarr;B strips the second B.  The comparison is exact
	 * and fails closed: a different port, an {@code http}/{@code https} change, or a different spelling of the host (an
	 * IP literal versus its name, a trailing dot, a punycode name) is a different origin.
	 *
	 * <p>
	 * A hop can be passed more than once: HttpClient's retry executors ({@code RetryExec} after an I/O failure,
	 * {@code ServiceUnavailableRetryExec} after a 503) restore the headers they captured <em>before</em> the protocol stage
	 * ran -- the caller's -- and run the protocol stage again on the same request object.  So the guard must run on every
	 * pass, not once per request object.  A protocol-stage pass is recognised by the stage setting the target host
	 * immediately before the request.  A client that wraps the context it is given in a child context bypasses this guard, and so does a legacy {@code AbstractHttpClient} subclass ({@code DefaultHttpClient}), which never sets the request again before it is written; the builder refuses such a client.  Proxy-Authorization is not removed at the wire stage unless the route is tunnelled (inside a tunnel the proxy never sees the request, so one would reach the origin).
	 *
	 * <p>
	 * On a {@code 307}/{@code 308} redirect of a non-{@code GET} request, HttpClient copies the previous hop's headers (as sent) rather than the
	 * original request's, so caller-set headers are not brought back on a return to the first origin; credentials HttpClient adds itself still are.  The {@code Host} header also carries over from the previous hop.  A {@code CONNECT} tunnel request is never stripped: it carries no caller headers.
	 */
	private static final class GuardingContext extends HttpClientContext {

		private final Hc45RedirectCredentialGuard guard = new Hc45RedirectCredentialGuard();
		private boolean protocolPass;

		@Override /* HttpCoreContext */
		@SuppressWarnings({
			"java:S3776" // Linear attribute-observation state machine; splitting it would scatter the pass tracking.
		})
		public void setAttribute(String id, Object obj) {
			super.setAttribute(id, obj);
			if (HttpCoreContext.HTTP_TARGET_HOST.equals(id))
				protocolPass = true;
			else if (HttpCoreContext.HTTP_REQUEST.equals(id) && obj instanceof HttpRequest request) {
				if (protocolPass) {
					protocolPass = false;
					guard.process(request, this);
				} else if (getAttribute(ATTR_STRIPPED) != null && ! "CONNECT".equalsIgnoreCase(request.getRequestLine().getMethod())) {
					// Proxy-Authorization is addressed to the proxy, except inside a tunnel, where the proxy never sees this request.
					var route = getHttpRoute();
					var keepProxyAuth = route == null || ! route.isTunnelled();
					for (var name : RedirectSecurity.stripOnCrossOrigin())
						if (! (keepProxyAuth && "proxy-authorization".equalsIgnoreCase(name)))
							request.removeHeaders(name);
				}
			}
		}
	}

	private static boolean safeMethod(HttpRequest request) {
		var method = request.getRequestLine().getMethod();
		return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
	}

	// HttpHost always has a scheme name and a host name; the port is -1 when none was given, so it is normalized to the
	// scheme default.  An https->http downgrade changes the scheme, so it is never same-origin.
	private static boolean sameOrigin(HttpHost from, HttpHost to) {
		return lower(from.getSchemeName()).equals(lower(to.getSchemeName()))
			&& lower(from.getHostName()).equals(lower(to.getHostName()))
			&& effectivePort(from) == effectivePort(to);
	}

	// Locale.ROOT lower-casing, not equalsIgnoreCase: that folds U+0131 (dotless i), U+0130 and U+017F onto ASCII letters, so a
	// non-ASCII host would otherwise compare equal to an ASCII one.
	private static String lower(String s) {
		return s.toLowerCase(Locale.ROOT);
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
