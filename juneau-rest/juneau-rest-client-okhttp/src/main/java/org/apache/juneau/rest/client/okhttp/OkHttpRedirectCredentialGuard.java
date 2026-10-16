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
package org.apache.juneau.rest.client.okhttp;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.rest.client.*;

import okhttp3.*;

/**
 * Network interceptor that removes caller-set credential headers before a request is sent to a different
 * origin as the result of a {@code 3xx} redirect.
 *
 * <p>
 * OkHttp's built-in redirect follower drops the {@code Authorization} header only when the target origin
 * changes, but it keeps other credential headers (notably a caller-set {@code Cookie}).  Registered as a network
 * interceptor, this class runs for every hop of an exchange: it compares the hop's target against the origin of the
 * first hop and, when they differ, strips the caller's credential header values.
 *
 * <p>
 * The first hop is the request as it leaves the client's application interceptors, recorded by
 * {@link #FIRST_HOP_RECORDER} (registered as the last application interceptor).  It is not {@link Call#request()}:
 * an application interceptor can rewrite the URL, and the rewritten URL is the origin the caller's credentials were
 * sent to.  Without a recorded first hop, {@link Call#request()} is used.
 *
 * <p>
 * The strip is sticky: once a hop of an exchange has left the first origin, every later hop of that exchange is
 * stripped too, including a hop back to the first origin ({@code A -> B -> A}).
 *
 * <p>
 * {@code Proxy-Authorization} authenticates to the proxy, not the origin, so it is the one credential header that can be
 * kept.  It is kept on a cross-origin hop only when the first hop and this hop are both plain HTTP (not an {@code https} target, which is reached
 * through a {@code CONNECT} tunnel) and goes through the same HTTP proxy as the first hop; stripping it there makes the
 * proxy answer {@code 407} again, and a {@link OkHttpClient#proxyAuthenticator() proxyAuthenticator} that re-adds it loops
 * until OkHttp's follow-up limit.  With no proxy, a different proxy, or an {@code https} target it is stripped.
 *
 * <p>
 * Only values the caller sent on the first hop are stripped.  A header value OkHttp itself supplies for the new origin
 * is kept: the target's own cookies loaded from the client's {@link CookieJar}, and credentials returned by the
 * client's {@link Authenticator} or proxy {@link Authenticator}, as long as they differ from the value the caller sent on
 * the first hop.
 *
 * <p>
 * Once a hop has been stripped, every later hop of the same call is stripped too, including one back to the first origin.
 * That state travels on a tag of the request, so a network {@link Interceptor} or an {@link Authenticator} that builds a
 * request from scratch (rather than from {@code request.newBuilder()}) must preserve the request's tags, or the sticky strip
 * is lost for a later hop back to the first origin.
 *
 * <p>
 * The origin is compared on OkHttp's own {@link HttpUrl} (scheme, host and effective port), with the same semantics
 * as {@link RedirectSecurity#shouldStripCredentials(java.net.URI, java.net.URI)}.  {@link java.net.URI} cannot be used
 * here: it parses a host name that OkHttp accepts but RFC 2396 does not (for example {@code my_service}, a typical
 * Docker Compose service name) with a {@code null} host, which would strip credentials from every request to that host.
 * The credential header set is delegated to {@link RedirectSecurity#stripOnCrossOrigin()} so that all transports share it.
 */
final class OkHttpRedirectCredentialGuard implements Interceptor {

	/**
	 * Application interceptor that tags the request leaving the application interceptors as the first hop.
	 *
	 * <p>
	 * OkHttp builds every retry and redirect hop with {@link Request#newBuilder()}, which carries the tag along.
	 */
	static final Interceptor FIRST_HOP_RECORDER = chain -> {
		var request = chain.request();
		return chain.proceed(request.newBuilder().tag(FirstHop.class, new FirstHop(request)).build());
	};

	// The first hop's request, plus (set once, on the first network pass) the address of the HTTP proxy it was routed
	// through, or null when it was not routed through an HTTP proxy.
	private static final class FirstHop {
		final Request request;
		boolean routeRecorded;
		SocketAddress proxyAddress;
		// Set once any hop of the chain left the first origin; later hops (including a return to the first origin) stay stripped.
		boolean stripped;

		FirstHop(Request request) {
			this.request = request;
		}
	}

	@Override /* Interceptor */
	@SuppressWarnings({
		"resource" // returned Response is owned and closed by OkHttp's call chain, not by this interceptor.
	})
	public Response intercept(Chain chain) throws IOException {
		var request = chain.request();
		var tag = request.tag(FirstHop.class);
		var first = tag != null ? tag.request : chain.call().request();
		var proxyAddress = httpProxyAddress(chain.connection());
		if (tag != null && ! tag.routeRecorded) {
			tag.routeRecorded = true;
			tag.proxyAddress = proxyAddress;
		}
		var crossOrigin = ! sameOrigin(first.url(), request.url());
		if (crossOrigin && tag != null)
			tag.stripped = true;
		if (crossOrigin || (tag != null && tag.stripped)) {
			// Sticky: OkHttp derives each follow-up from the pre-strip request, so a hop back to the first origin (A -> B -> A)
			// would otherwise be passed unmodified and re-send the caller's credentials.
			// Proxy-Authorization authenticates to the proxy, so it is kept only when this hop still goes to the proxy that
			// the first hop went through, and both hops are plain HTTP (an https target is reached through a CONNECT tunnel,
			// so the header would reach the origin; and a header that travelled inside a tunnel on the first hop must not be
			// downgraded to cleartext to the proxy).  Without a proxy, or when the proxy changed, it is stripped like the rest.
			var keepProxyAuth = tag != null && proxyAddress != null && ! first.isHttps() && ! request.isHttps() && proxyAddress.equals(tag.proxyAddress);
			Request.Builder b = null;
			for (var name : RedirectSecurity.stripOnCrossOrigin()) {
				if (keepProxyAuth && "Proxy-Authorization".equalsIgnoreCase(name))
					continue;
				var values = request.headers(name);
				var callerValues = first.headers(name);
				if (values.isEmpty() || callerValues.isEmpty())
					continue;
				var kept = new ArrayList<>(values);
				kept.removeAll(callerValues);
				if (kept.size() == values.size())
					continue;
				if (b == null)
					b = request.newBuilder();
				b.removeHeader(name);
				for (var v : kept)
					b.addHeader(name, v);
			}
			if (b != null)
				request = b.build();
		}
		return chain.proceed(request);
	}

	private static SocketAddress httpProxyAddress(Connection connection) {
		if (connection == null)
			return null;
		var proxy = connection.route().proxy();
		return proxy.type() == Proxy.Type.HTTP ? proxy.address() : null;
	}

	// HttpUrl always has a lower-case scheme (http/https), a non-null canonical (lower-case) host and an effective port
	// (the scheme default when none is given), so the comparison is total.  An https->http downgrade changes the scheme,
	// so it is never same-origin.
	private static boolean sameOrigin(HttpUrl from, HttpUrl to) {
		return from.scheme().equals(to.scheme()) && from.host().equals(to.host()) && from.port() == to.port();
	}
}
