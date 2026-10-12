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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link Hc45RedirectCredentialGuard}'s edge cases that end-to-end redirect tests
 * ({@code ApacheHc45Transport_RedirectCredentials_Test}) never reach &mdash; a missing target host, and host names
 * that {@link java.net.URI} cannot parse (such as an underscore host name, which would need a resolvable name in an
 * end-to-end test).  They drive {@link Hc45RedirectCredentialGuard#process(HttpRequest, HttpContext)} directly
 * against a hand-built {@link HttpContext}, one call per request of an exchange.  In the same package as the guard
 * since it (and its members) are package-private.
 */
class Hc45RedirectCredentialGuard_Test {

	private final Hc45RedirectCredentialGuard guard = new Hc45RedirectCredentialGuard();

	@Test
	void a01_noTargetHostInContext_isNoOp() {
		var context = new BasicHttpContext();
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		assertDoesNotThrow(() -> guard.process(request, context));
		assertNotNull(request.getFirstHeader("Authorization"), "with no target host to compare against, credentials must be left untouched");
	}

	@Test
	void a02_hostNotParsableByUri_thenDifferentOrigin_stripsCredentials() {
		// "|" passes HttpHost's blanks-only host name validation but is rejected by java.net.URI, so the origin must be
		// compared on HttpHost itself; otherwise the first hop records no origin and the cross-origin hop keeps credentials.
		var context = new BasicHttpContext();
		assertNotNull(hop(context, new HttpHost("exa|mple.com", 80, "http")).getFirstHeader("Authorization"));
		assertNull(hop(context, new HttpHost("evil.example.com", 80, "http")).getFirstHeader("Authorization"));
	}

	@Test
	void a03_firstRequestInExchange_recordsOriginWithoutStrippingCredentials() {
		var context = new BasicHttpContext();
		HttpCoreContext.adapt(context).setTargetHost(new HttpHost("example.com", 80, "http"));
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		guard.process(request, context);
		assertNotNull(request.getFirstHeader("Authorization"), "the first request of an exchange only records the origin; nothing to compare against yet");
	}

	@Test
	void a04_underscoreHost_sameOriginRedirect_keepsCredentials() {
		// java.net.URI parses an underscore host name (e.g. a Docker Compose service name) with a null host.
		var context = new BasicHttpContext();
		hop(context, new HttpHost("my_service", 8080, "http"));
		var redirected = hop(context, new HttpHost("MY_SERVICE", 8080, "http"));
		assertNotNull(redirected.getFirstHeader("Authorization"));
		assertNotNull(redirected.getFirstHeader("Cookie"));
	}

	@Test
	void a05_underscoreHost_crossOriginRedirect_stripsCredentials() {
		var context = new BasicHttpContext();
		hop(context, new HttpHost("my_service", 8080, "http"));
		var redirected = hop(context, new HttpHost("other_service", 8080, "http"));
		assertNull(redirected.getFirstHeader("Authorization"));
		assertNull(redirected.getFirstHeader("Cookie"));
		assertNotNull(redirected.getFirstHeader("X-Other"));
	}

	@Test
	void a06_underscoreHost_differentPort_stripsCredentials() {
		var context = new BasicHttpContext();
		hop(context, new HttpHost("my_service", 8080, "http"));
		assertNull(hop(context, new HttpHost("my_service", 8081, "http")).getFirstHeader("Authorization"));
	}

	@Test
	void a07_defaultPortNormalized_sameOrigin_keepsCredentials() {
		var context = new BasicHttpContext();
		hop(context, new HttpHost("my_service", -1, "https"));
		assertNotNull(hop(context, new HttpHost("my_service", 443, "HTTPS")).getFirstHeader("Authorization"));
	}

	@Test
	void a08_httpsToHttpDowngrade_stripsCredentials() {
		var context = new BasicHttpContext();
		hop(context, new HttpHost("example.com", 443, "https"));
		assertNull(hop(context, new HttpHost("example.com", 443, "http")).getFirstHeader("Authorization"));
	}

	// Runs the guard for one request of an exchange aimed at target, as HttpClient's redirect executor does per hop.
	private HttpRequest hop(HttpContext context, HttpHost target) {
		HttpCoreContext.adapt(context).setTargetHost(target);
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		request.addHeader("Cookie", "session=abc");
		request.addHeader("X-Other", "o-1");
		guard.process(request, context);
		return request;
	}
}
