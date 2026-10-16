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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.hc.client5.http.*;
import org.apache.hc.client5.http.RouteInfo.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link CredentialGuardingContext}: which hops it treats as another origin, and which records of a hop
 * it inspects.  HttpClient itself drives it in {@code ApacheHc5Transport_CallerClientRedirectCredentials_Test}.
 */
class CredentialGuardingContext_Test {

	private static HttpRequest request(String method, String uri) {
		var request = new BasicHttpRequest(method, java.net.URI.create(uri));
		request.addHeader("Authorization", "Bearer t");
		request.addHeader("Cookie", "s=1");
		request.addHeader("Proxy-Authorization", "Basic p");
		request.addHeader("X-API-Key", "k");
		request.addHeader("X-Trace", "t-1");
		return request;
	}

	private static HttpRequest request(String uri) {
		return request("GET", uri);
	}

	private static List<String> names(HttpRequest request) {
		var names = new ArrayList<String>();
		for (var h : request.getHeaders())
			names.add(h.getName());
		return names;
	}

	private static final List<String> ALL = List.of("Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "X-Trace");
	private static final List<String> STRIPPED = List.of("X-Trace");

	// Records the first request, then the hop, and returns the hop's remaining header names.
	private static List<String> hop(String from, String to) {
		var context = new CredentialGuardingContext();
		context.setRequest(request(from));
		var hop = request(to);
		context.setRequest(hop);
		return names(hop);
	}

	// Same as hop(from, to), with the route the main exec stage records in the context before the hop.
	private static List<String> hop(String from, String to, HttpRoute route) {
		var context = new CredentialGuardingContext();
		context.setRequest(request(from));
		context.setRoute(route);
		var hop = request(to);
		context.setRequest(hop);
		return names(hop);
	}

	private static final HttpHost TARGET_B = new HttpHost("http", "b.example", 8080);
	private static final HttpHost PROXY = new HttpHost("http", "proxy.example", 3128);

	@Test
	void a01_firstRequest_keepsCredentials() {
		var context = new CredentialGuardingContext();
		var first = request("http://a.example:8080/x");
		context.setRequest(first);
		assertEquals(ALL, names(first));
	}

	@Test
	void a02_sameOrigin_keepsCredentials() {
		assertEquals(ALL, hop("http://a.example:8080/x", "http://a.example:8080/y"));
	}

	@Test
	void a03_hostCaseDiffers_sameOrigin() {
		assertEquals(ALL, hop("http://A.Example:8080/x", "http://a.example:8080/y"));
	}

	@Test
	void a04_defaultPortExplicitOrNot_sameOrigin() {
		assertEquals(ALL, hop("http://a.example/x", "http://a.example:80/y"));
		assertEquals(ALL, hop("https://a.example:443/x", "https://a.example/y"));
	}

	@Test
	void a05_otherHost_stripped() {
		assertEquals(STRIPPED, hop("http://a.example:8080/x", "http://b.example:8080/y"));
	}

	@Test
	void a06_otherPort_stripped() {
		assertEquals(STRIPPED, hop("http://a.example:8080/x", "http://a.example:8081/y"));
	}

	@Test
	void a07_httpsToHttpDowngrade_samePort_stripped() {
		assertEquals(STRIPPED, hop("https://a.example:8443/x", "http://a.example:8443/y"));
	}

	@Test
	void a08_httpToHttps_samePort_stripped() {
		assertEquals(STRIPPED, hop("http://a.example:8443/x", "https://a.example:8443/y"));
	}

	@Test
	void a09_hopWithoutAuthority_stripped() {
		var context = new CredentialGuardingContext();
		context.setRequest(request("http://a.example/x"));
		var hop = new BasicHttpRequest("GET", "/y");
		hop.addHeader("Authorization", "Bearer t");
		hop.addHeader("X-Trace", "t-1");
		context.setRequest(hop);
		assertEquals(STRIPPED, names(hop));
	}

	@Test
	void a10_firstRequestWithoutAuthority_laterHopStripped() {
		var context = new CredentialGuardingContext();
		context.setRequest(new BasicHttpRequest("GET", "/x"));
		var hop = request("http://a.example/y");
		context.setRequest(hop);
		assertEquals(STRIPPED, names(hop));
	}

	@Test
	void a11_backToFirstOrigin_staysStripped() {
		var context = new CredentialGuardingContext();
		context.setRequest(request("http://a.example/x"));
		context.setRequest(request("http://b.example/y"));
		var back = request("http://a.example/z");
		context.setRequest(back);
		assertEquals(STRIPPED, names(back));
	}

	@Test
	void a12_resendOfSameHop_inspectedAgain() {
		// After an authentication challenge the protocol stage resets the hop's headers to its unstripped copy and the main
		// stage records the same request object again; that record must strip again.
		var context = new CredentialGuardingContext();
		context.setRequest(request("http://a.example/x"));
		var hop = request("http://b.example/y");
		context.setRequest(hop);
		hop.addHeader("Authorization", "Bearer t");
		context.setRequest(hop);
		assertEquals(STRIPPED, names(hop));
	}

	@Test
	void a13_connectRequest_notInspectedAndNotTheFirstOrigin() {
		var context = new CredentialGuardingContext();
		var connect = request("CONNECT", "http://proxy.example:3128/");
		context.setRequest(connect);
		assertEquals(ALL, names(connect));
		var first = request("https://a.example/x");
		context.setRequest(first);
		assertEquals(ALL, names(first));
	}

	@Test
	void a14_recordsTheRequest() {
		var context = new CredentialGuardingContext();
		var first = request("http://a.example/x");
		context.setRequest(first);
		assertSame(first, context.getRequest());
	}

	@Test
	void a15_proxiedPlainRoute_keepsProxyAuthorization() {
		// Proxy-Authorization on a plain (not tunnelled) proxied route authenticates to the proxy, which every hop goes
		// through; stripping it would make the proxy answer 407 again.
		var route = new HttpRoute(TARGET_B, null, PROXY, false, TunnelType.PLAIN, LayerType.PLAIN);
		assertEquals(List.of("Proxy-Authorization", "X-Trace"), hop("http://a.example:8080/x", "http://b.example:8080/y", route));
	}

	@Test
	void a16_tunnelledRoute_stripsProxyAuthorization() {
		var route = new HttpRoute(new HttpHost("https", "b.example", 8443), null, PROXY, true, TunnelType.TUNNELLED, LayerType.LAYERED);
		assertEquals(STRIPPED, hop("https://a.example:8443/x", "https://b.example:8443/y", route));
	}

	@Test
	void a17_directRoute_stripsProxyAuthorization() {
		assertEquals(STRIPPED, hop("http://a.example:8080/x", "http://b.example:8080/y", new HttpRoute(TARGET_B)));
	}

	@Test
	void a18_proxiedPlainRoute_sameOrigin_keepsCredentials() {
		var route = new HttpRoute(new HttpHost("http", "a.example", 8080), null, PROXY, false, TunnelType.PLAIN, LayerType.PLAIN);
		assertEquals(ALL, hop("http://a.example:8080/x", "http://a.example:8080/y", route));
	}

	@Test
	void a19_underscoreHostNames_comparedAsHosts() {
		// java.net.URI cannot parse a host name with an underscore; the context compares HttpClient's own host objects.
		assertEquals(ALL, underscoreHop("my_service", "my_service"));
		assertEquals(List.of("X-Trace"), underscoreHop("my_service", "other_service"));
	}

	private static List<String> underscoreHop(String fromHost, String toHost) {
		var context = new CredentialGuardingContext();
		context.setRequest(new BasicHttpRequest("GET", new HttpHost("http", fromHost, 8080), "/x"));
		var hop = new BasicHttpRequest("GET", new HttpHost("http", toHost, 8080), "/y");
		hop.addHeader("Authorization", "Bearer t");
		hop.addHeader("Cookie", "s=1");
		hop.addHeader("Proxy-Authorization", "Basic p");
		hop.addHeader("X-API-Key", "k");
		hop.addHeader("X-Trace", "t-1");
		context.setRequest(hop);
		return names(hop);
	}
}
