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
import org.apache.http.client.protocol.*;
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

	@Test
	void f01_guardingContext_httpsToHttpSameHost_wireStageStripsStoreCookie() {
		// https -> http on the same host is a downgrade, hence a different origin.  Drive the context the way HttpClient does.
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		hopStages(context, new HttpHost("example.com", 443, "https"), new BasicHttpRequest("GET", "/"), false);
		var downgraded = new BasicHttpRequest("GET", "/");
		hopStages(context, new HttpHost("example.com", 80, "http"), downgraded, true);
		assertNull(downgraded.getFirstHeader("Cookie"));
		assertNull(downgraded.getFirstHeader("Authorization"));
	}

	// Protocol stage (target host, request), then the client's own interceptors add a store cookie and auth, then the wire stage.
	private static void hopStages(HttpClientContext context, HttpHost target, HttpRequest request, boolean expectStripped) {
		context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
		context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
		request.addHeader("Cookie", "store=s-1");
		request.addHeader("Authorization", "Basic x");
		context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
		if (! expectStripped) {
			assertNotNull(request.getFirstHeader("Cookie"));
			assertNotNull(request.getFirstHeader("Authorization"));
		}
	}

	@Test
	void g01_aToBToA_wireStageRestoresOnReturn() {
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		var a = new HttpHost("example.com", 443, "https");
		var b = new HttpHost("other.example.com", 443, "https");
		hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
		var viaB = new BasicHttpRequest("GET", "/");
		hopStages(context, b, viaB, true);
		assertNull(viaB.getFirstHeader("Cookie"));
		assertNull(viaB.getFirstHeader("Authorization"));
		var back = new BasicHttpRequest("GET", "/");
		hopStages(context, new HttpHost("EXAMPLE.com", -1, "HTTPS"), back, false);
		assertNotNull(back.getFirstHeader("Cookie"));
		assertNotNull(back.getFirstHeader("Authorization"));
	}

	@Test
	void g02_aToBToAToB_secondBStripped() {
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		var a = new HttpHost("a.example.com", 80, "http");
		var b = new HttpHost("b.example.com", 80, "http");
		hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
		hopStages(context, b, new BasicHttpRequest("GET", "/"), true);
		hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
		var secondB = new BasicHttpRequest("GET", "/");
		hopStages(context, b, secondB, true);
		assertNull(secondB.getFirstHeader("Cookie"));
		assertNull(secondB.getFirstHeader("Authorization"));
	}

	@Test
	void g03_aToBToCToA_restoredOnFinalHop() {
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		var a = new HttpHost("a.example.com", 80, "http");
		hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
		hopStages(context, new HttpHost("b.example.com", 80, "http"), new BasicHttpRequest("GET", "/"), true);
		hopStages(context, new HttpHost("c.example.com", 80, "http"), new BasicHttpRequest("GET", "/"), true);
		var back = new BasicHttpRequest("GET", "/");
		hopStages(context, a, back, false);
		assertNotNull(back.getFirstHeader("Cookie"));
	}

	@Test
	void g04_returnToLookAlikeOrigin_staysStripped() {
		// Canonical-origin equality: only scheme (case-insensitive), host (case-insensitive) and effective port may match.
		var first = new HttpHost("a.example.com", -1, "https");
		var lookAlikes = new HttpHost[]{
			new HttpHost("a.example.com", 8443, "https"),     // different port
			new HttpHost("a.example.com", 80, "http"),        // downgrade
			new HttpHost("a.example.com", 443, "http"),       // scheme differs, same port
			new HttpHost("a.example.com.", 443, "https"),     // trailing dot
			new HttpHost("192.0.2.1", 443, "https"),          // IP versus name
			new HttpHost("xn--a-example.com", 443, "https"),  // punycode spelling
			new HttpHost("a.example.com.evil.test", 443, "https"),
			new HttpHost("evil.test", 443, "https")
		};
		for (var target : lookAlikes) {
			var context = Hc45RedirectCredentialGuard.newGuardingContext();
			hopStages(context, first, new BasicHttpRequest("GET", "/"), false);
			var request = new BasicHttpRequest("GET", "/");
			hopStages(context, new HttpHost("b.example.com", 443, "https"), new BasicHttpRequest("GET", "/"), true);
			hopStages(context, target, request, true);
			assertNull(request.getFirstHeader("Cookie"), target.toString());
			assertNull(request.getFirstHeader("Authorization"), target.toString());
		}
	}

	@Test
	void g05_sameOrigin_everyHopKept() {
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		var a = new HttpHost("a.example.com", 80, "http");
		hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
		hopStages(context, new HttpHost("A.example.com", -1, "http"), new BasicHttpRequest("GET", "/"), false);
	}

	@Test
	void g06_nonAsciiLookAlikeHost_isNotSameOrigin() {
		// equalsIgnoreCase would fold U+0131 (dotless i) and U+017F onto "i" and "s"; the host must be compared as lower-cased ASCII.
		for (var lookAlike : new String[]{"log\u0131n.sample.com", "login.sample.com".replace("s", "\u017F"), "LOG\u0130N.sample.com"}) {
			var context = Hc45RedirectCredentialGuard.newGuardingContext();
			hopStages(context, new HttpHost(lookAlike.replace("\u017F", "s").replace("\u0131", "i").replace("\u0130", "I"), 443, "https"), new BasicHttpRequest("GET", "/"), false);
			var request = new BasicHttpRequest("GET", "/");
			hopStages(context, new HttpHost(lookAlike, 443, "https"), request, true);
			assertNull(request.getFirstHeader("Cookie"), lookAlike);
			assertNull(request.getFirstHeader("Authorization"), lookAlike);
		}
	}

	@Test
	void h01_returnToFirstOrigin_onlyGetAndHeadRestored() {
		var a = new HttpHost("a.example.com", 80, "http");
		var b = new HttpHost("b.example.com", 80, "http");
		for (var method : new String[]{"GET", "HEAD", "get"}) {
			var context = Hc45RedirectCredentialGuard.newGuardingContext();
			hopStages(context, a, new BasicHttpRequest("POST", "/"), false);
			hopStages(context, b, new BasicHttpRequest("POST", "/"), true);
			var back = new BasicHttpRequest(method, "/");
			hopStages(context, a, back, false);
			assertNotNull(back.getFirstHeader("Cookie"), method);
		}
		for (var method : new String[]{"POST", "PUT", "DELETE", "PATCH"}) {
			var context = Hc45RedirectCredentialGuard.newGuardingContext();
			hopStages(context, a, new BasicHttpRequest(method, "/"), false);
			hopStages(context, b, new BasicHttpRequest(method, "/"), true);
			var back = new BasicHttpRequest(method, "/");
			hopStages(context, a, back, true);
			assertNull(back.getFirstHeader("Cookie"), method);
			assertNull(back.getFirstHeader("Authorization"), method);
		}
	}

	@Test
	void h02_sameOriginPost_neverLeft_keepsCredentials() {
		var context = Hc45RedirectCredentialGuard.newGuardingContext();
		var a = new HttpHost("a.example.com", 80, "http");
		hopStages(context, a, new BasicHttpRequest("POST", "/"), false);
		hopStages(context, a, new BasicHttpRequest("POST", "/"), false);
	}

	@Test
	void h03_proxyAuthorization_exemptUnlessRouteTunnelled() {
		var a = new HttpHost("a.example.com", 443, "https");
		var proxy = new HttpHost("proxy.example.com", 3128, "http");
		for (var tunnelled : new boolean[]{false, true}) {
			var context = Hc45RedirectCredentialGuard.newGuardingContext();
			hopStages(context, a, new BasicHttpRequest("GET", "/"), false);
			var b = new HttpHost("b.example.com", 443, "https");
			context.setAttribute(HttpClientContext.HTTP_ROUTE, new org.apache.http.conn.routing.HttpRoute(b, null, new HttpHost[]{proxy}, true, tunnelled ? org.apache.http.conn.routing.RouteInfo.TunnelType.TUNNELLED : org.apache.http.conn.routing.RouteInfo.TunnelType.PLAIN, org.apache.http.conn.routing.RouteInfo.LayerType.PLAIN));
			var request = new BasicHttpRequest("GET", "/");
			request.addHeader("Proxy-Authorization", "Basic cHJveHk=");
			context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, b);
			context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
			request.addHeader("Proxy-Authorization", "Basic cHJveHk=");
			context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
			if (tunnelled)
				assertNull(request.getFirstHeader("Proxy-Authorization"));
			else
				assertNotNull(request.getFirstHeader("Proxy-Authorization"));
		}
	}
}
