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

import org.apache.juneau.rest.client.*;

import okhttp3.*;

/**
 * Network interceptor that removes caller-set credential headers before a request is sent to a different
 * origin as the result of a {@code 3xx} redirect.
 *
 * <p>
 * OkHttp's built-in redirect follower drops the {@code Authorization} header only when the target host
 * changes, but it keeps other credential headers (notably a caller-set {@code Cookie}) and does not account
 * for a differing port or an {@code https}&rarr;{@code http} downgrade.  Registered as a network interceptor,
 * this class runs for every hop of an exchange: it compares the hop's target against the origin of the
 * original call and strips the credential headers whenever they differ.
 *
 * <p>
 * The forward/strip decision and the header set are delegated to {@link RedirectSecurity} so that all
 * transports share one policy.
 */
final class OkHttpRedirectCredentialGuard implements Interceptor {

	@Override /* Interceptor */
	@SuppressWarnings("resource") // returned Response is owned and closed by OkHttp's call chain, not by this interceptor.
	public Response intercept(Chain chain) throws IOException {
		var request = chain.request();
		var from = chain.call().request().url().uri();
		var to = request.url().uri();
		if (shouldStrip(from, to)) {
			var b = request.newBuilder();
			for (var name : RedirectSecurity.stripOnCrossOrigin())
				b.removeHeader(name);
			request = b.build();
		}
		return chain.proceed(request);
	}

	private static boolean shouldStrip(URI from, URI to) {
		try {
			return RedirectSecurity.shouldStripCredentials(from, to);
		} catch (RuntimeException e) {
			// If the origins cannot be compared, err on the side of not forwarding credentials.
			return true;
		}
	}
}
