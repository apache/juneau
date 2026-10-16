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
package org.apache.juneau.rest.client.apachehttpclient50;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.hc.client5.http.*;
import org.apache.hc.client5.http.protocol.*;
import org.apache.hc.core5.http.*;
import org.apache.juneau.rest.client.*;

/**
 * Per-request context that removes the caller's credential headers from a redirect hop to another origin, without
 * changing the client or its redirect handling.  The transport sends every ordinary request with one, on the client it
 * creates itself (whose redirect strategy then allows such a hop, so it is followed without the headers) and on a
 * caller-supplied client.
 *
 * <p>
 * HttpClient's redirect executor copies the original request's headers onto each follow-up request.  The client's
 * {@code RedirectStrategy} decides whether a redirect is followed at all: {@code DefaultRedirectStrategy} refuses a
 * cross-origin redirect that would replay a sensitive header, but {@code LaxRedirectStrategy} or a custom strategy may
 * not.  This context strips {@link RedirectSecurity#stripOnCrossOrigin()} from a hop whose origin (scheme, host and
 * effective port, with the semantics of {@link RedirectSecurity#shouldStripCredentials(java.net.URI, java.net.URI)})
 * differs from the first request's, whatever strategy made that hop, and from every later hop of the exchange: the
 * redirect executor copies each hop from the unstripped one before it, so a redirect back to the first origin would
 * otherwise carry the credentials again.  A hop whose origin cannot be determined counts as cross-origin.
 *
 * <p>
 * HttpClient's main exec stage records each request it sends in the context ({@code setRequest}), once per send, after
 * the protocol stage has added any {@code Authorization} from the client's {@code CredentialsProvider} and before it runs
 * the client's own request interceptors.  So a {@code Cookie} that the client's {@code CookieStore} adds is still sent,
 * and an {@code Authorization} from the client's {@code CredentialsProvider} on a cross-origin hop is removed with the
 * caller's.  Every record is inspected, including a resend of the same request object after an authentication
 * challenge, for which the protocol stage resets the headers to the unstripped copy.  A {@code CONNECT} tunnel request
 * would not be inspected: it carries no caller headers, and HttpClient 5.6 sends it without recording it here.
 *
 * <p>
 * On a proxied route that is not tunnelled (a plain {@code http} target through an HTTP proxy), {@code Proxy-Authorization}
 * is kept on a cross-origin hop: every hop goes through the same proxy, and the header authenticates to that proxy, so
 * removing it would make the proxy answer {@code 407} again.  On a direct or tunnelled route it is removed with the rest.
 *
 * <p>
 * This relies on HttpClient's internal call order, as the HttpClient 4.5 transport's guard does; the transport's tests
 * pin it.  A client that replaces the per-request context it is given (for example by wrapping it as the parent of a
 * context of its own) is not guarded.  Like every {@link HttpClientContext}, an instance serves one request exchange
 * on one thread.
 */
final class CredentialGuardingContext extends HttpClientContext {

	private boolean started;
	private HttpHost origin;
	private boolean stripping;

	@Override /* HttpCoreContext */
	public void setRequest(HttpRequest request) {
		super.setRequest(request);
		if (request == null || Method.CONNECT.isSame(request.getMethod()))
			return;
		var target = originOf(request);
		if (! started) {
			started = true;
			origin = target;
			return;
		}
		if (! stripping && (origin == null || target == null || ! sameOrigin(origin, target)))
			stripping = true;
		if (stripping) {
			var keepProxyAuthorization = isPlainProxyRoute(getHttpRoute());
			for (var name : RedirectSecurity.stripOnCrossOrigin())
				if (! (keepProxyAuthorization && eqic(name, HttpHeaders.PROXY_AUTHORIZATION)))
					request.removeHeaders(name);
		}
	}

	// The main exec stage records the route in the context just before the request.  On a proxied route that is not
	// tunnelled, every hop goes through the same proxy, and Proxy-Authorization authenticates to that proxy.
	private static boolean isPlainProxyRoute(RouteInfo route) {
		return route != null && route.getProxyHost() != null && ! route.isTunnelled();
	}

	// The protocol stage sets the scheme and authority on every request before the main stage records it.
	private static HttpHost originOf(HttpRequest request) {
		var scheme = request.getScheme();
		var authority = request.getAuthority();
		if (scheme == null || authority == null || authority.getHostName() == null)
			return null;
		return new HttpHost(scheme, authority.getHostName(), authority.getPort());
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
