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
 * Only values the caller sent on the first hop are stripped.  A header value OkHttp itself supplies for the new origin
 * is kept: the target's own cookies loaded from the client's {@link CookieJar}, and credentials returned by the
 * client's {@link Authenticator} or proxy {@link Authenticator}.
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

	private record FirstHop(Request request) {}

	@Override /* Interceptor */
	@SuppressWarnings({
		"resource" // returned Response is owned and closed by OkHttp's call chain, not by this interceptor.
	})
	public Response intercept(Chain chain) throws IOException {
		var request = chain.request();
		var tag = request.tag(FirstHop.class);
		var first = tag != null ? tag.request() : chain.call().request();
		if (! sameOrigin(first.url(), request.url())) {
			Request.Builder b = null;
			for (var name : RedirectSecurity.stripOnCrossOrigin()) {
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

	// HttpUrl always has a lower-case scheme (http/https), a non-null canonical (lower-case) host and an effective port
	// (the scheme default when none is given), so the comparison is total.  An https->http downgrade changes the scheme,
	// so it is never same-origin.
	private static boolean sameOrigin(HttpUrl from, HttpUrl to) {
		return from.scheme().equals(to.scheme()) && from.host().equals(to.host()) && from.port() == to.port();
	}
}
