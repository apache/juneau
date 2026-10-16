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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Verifies that {@link PolicyEnforcedRedirects} forwards caller-set credential headers
 * ({@link RedirectSecurity#stripOnCrossOrigin()}) only to a same-origin redirect target, and does not forward them
 * to a different origin or across an {@code https} &rarr; {@code http} downgrade.
 *
 * <p>
 * Drives {@link PolicyEnforcedRedirects#execute(TransportRequest, PolicyEnforcedRedirects.HopExecutor)} directly
 * with a recording {@link PolicyEnforcedRedirects.HopExecutor}, so no network access is involved.
 */
@SuppressWarnings({
	"resource" // TransportResponse objects returned by the scripted HopExecutor wrap in-memory streams and are not closed by the tests
})
class PolicyEnforcedRedirects_Test {

	/** Records every hop and answers each with the next scripted {@code Location} (302), then a final 200. */
	private static final class ScriptedHops implements PolicyEnforcedRedirects.HopExecutor {
		final List<TransportRequest> hops = new ArrayList<>();
		final Deque<String> locations;
		int status = 302;

		ScriptedHops(String...locations) {
			this.locations = new ArrayDeque<>(List.of(locations));
		}

		@Override
		public TransportResponse execute(TransportRequest hopRequest) {
			hops.add(hopRequest);
			var location = locations.poll();
			if (location == null)
				return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(new byte[0])).build();
			return TransportResponse.builder().statusCode(status).header("Location", location).build();
		}

		TransportRequest last() {
			return hops.get(hops.size() - 1);
		}
	}

	private static TransportRequest request(String uri) {
		return TransportRequest.builder()
			.method("GET")
			.uri(uri)
			.remoteUrlPolicy(true, false)
			.header("Authorization", "Bearer secret-token")
			.header("Cookie", "session=abc123")
			.header("Proxy-Authorization", "Basic cHJveHk6cHc=")
			.header("X-API-Key", "key-123")
			.header("Accept", "application/json")
			.header("X-Trace", "t-1")
			.build();
	}

	private static List<String> headerNames(TransportRequest r) {
		return r.getHeaders().stream().map(TransportHeader::name).toList();
	}

	private static ScriptedHops run(TransportRequest initial, String...locations) throws Exception {
		var hops = new ScriptedHops(locations);
		PolicyEnforcedRedirects.execute(initial, hops);
		return hops;
	}

	//====================================================================================================
	// a - Cross-origin redirects do not forward credentials
	//====================================================================================================

	@Test void a01_differentHost_dropsCredentialHeaders() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/echo");
		assertBean(hops.last(), "method,uri", "GET,http://b.example.com/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	@Test void a02_differentPort_dropsCredentialHeaders() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://a.example.com:8081/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	@Test void a03_httpsToHttpDowngrade_dropsCredentialHeaders() throws Exception {
		var hops = run(request("https://a.example.com/start"), "http://a.example.com/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	@Test void a04_credentialHeaderNamesMatchedCaseInsensitively() throws Exception {
		var initial = TransportRequest.builder()
			.method("GET")
			.uri("http://a.example.com/start")
			.remoteUrlPolicy(true, false)
			.header("authorization", "Bearer secret-token")
			.header("COOKIE", "session=abc123")
			.header("x-api-key", "key-123")
			.header("Accept", "text/plain")
			.build();
		var hops = run(initial, "http://b.example.com/echo");
		assertList(headerNames(hops.last()), "Accept");
	}

	@Test void a05_crossOriginThenBackToOrigin_credentialsRestored() throws Exception {
		// A hop that returns to the first origin gets the caller's credential headers back.
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/bounce", "http://a.example.com/echo");
		assertList(headerNames(hops.hops.get(1)), "Accept", "X-Trace");
		assertList(headerNames(hops.last()), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
	}

	@Test void a05b_aToBToCToA_restoredOnFinalHop() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/x", "http://c.example.com/y", "http://a.example.com/echo");
		assertList(headerNames(hops.hops.get(2)), "Accept", "X-Trace");
		assertList(headerNames(hops.last()), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
	}

	@Test void a05c_aToBToAToB_strippedAgainOnSecondB() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/x", "http://a.example.com/y", "http://b.example.com/echo");
		assertList(headerNames(hops.hops.get(2)), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	@Test void a05d_returnToSameHostDifferentPortOrScheme_staysStripped() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/x", "http://a.example.com:8080/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
		hops = run(request("https://a.example.com/start"), "http://b.example.com/x", "http://a.example.com/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	@Test void a05e_originEquality_caseAndDefaultPortNormalised_butNotIdnOrIpOrTrailingDot() throws Exception {
		var hops = run(request("https://a.example.com/start"), "http://b.example.com/x", "https://A.EXAMPLE.com:443/echo");
		assertList(headerNames(hops.last()), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
		for (var other : new String[]{"https://a.example.com./echo", "https://192.0.2.9/echo", "https://xn--a-example.com/echo", "https://a.example.com:8443/echo"}) {
			hops = run(request("https://a.example.com/start"), "http://b.example.com/x", other);
			assertList(headerNames(hops.last()), "Accept", "X-Trace");
		}
	}

	private static TransportRequest requestWithMethod(String method, String uri) {
		return TransportRequest.builder().method(method).uri(uri).remoteUrlPolicy(true, false)
			.header("Authorization", "Bearer secret-token").header("Accept", "application/json").build();
	}

	private static ScriptedHops runWithStatus(TransportRequest initial, int status, String...locations) throws Exception {
		var hops = new ScriptedHops(locations);
		hops.status = status;
		PolicyEnforcedRedirects.execute(initial, hops);
		return hops;
	}

	@Test void a05g_returnToFirstOrigin_307Post_staysStripped() throws Exception {
		var hops = runWithStatus(requestWithMethod("POST", "http://a.example.com/start"), 307, "http://b.example.com/x", "http://a.example.com/echo");
		assertEquals("POST", hops.last().getMethod());
		assertList(headerNames(hops.last()), "Accept");
	}

	@Test void a05h_returnToFirstOrigin_302PostBecomesGet_restored() throws Exception {
		var hops = runWithStatus(requestWithMethod("POST", "http://a.example.com/start"), 302, "http://b.example.com/x", "http://a.example.com/echo");
		assertEquals("GET", hops.last().getMethod());
		assertList(headerNames(hops.last()), "Authorization", "Accept");
	}

	@Test void a05i_returnToFirstOrigin_headRestored() throws Exception {
		var hops = runWithStatus(requestWithMethod("HEAD", "http://a.example.com/start"), 307, "http://b.example.com/x", "http://a.example.com/echo");
		assertList(headerNames(hops.last()), "Authorization", "Accept");
	}

	@Test void a05j_sameOrigin307Post_neverLeft_keepsCredentials() throws Exception {
		var hops = runWithStatus(requestWithMethod("POST", "http://a.example.com/start"), 307, "http://a.example.com/next");
		assertList(headerNames(hops.last()), "Authorization", "Accept");
	}

	@Test void a05f_locationWithUserInfo_isRefused() throws Exception {
		var ex = assertThrows(TransportException.class, () -> run(request("http://a.example.com/start"), "http://b.example.com/x", "http://user:pw@a.example.com/echo"));
		assertTrue(ex.getMessage().contains("user info"), ex.getMessage());
	}

	@Test void a06_firstHop_sendsAllHeaders() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://b.example.com/echo");
		assertList(headerNames(hops.hops.get(0)), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
	}

	//====================================================================================================
	// b - Same-origin redirects forward credentials
	//====================================================================================================

	@Test void b01_sameOriginAbsolute_keepsCredentialHeaders() throws Exception {
		var hops = run(request("http://a.example.com/start"), "http://A.example.com:80/echo");
		assertList(headerNames(hops.last()), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
	}

	@Test void b02_sameOriginRelative_keepsCredentialHeaders() throws Exception {
		var hops = run(request("https://a.example.com/start"), "/echo");
		assertBean(hops.last(), "uri", "https://a.example.com/echo");
		assertList(headerNames(hops.last()), "Authorization", "Cookie", "Proxy-Authorization", "X-API-Key", "Accept", "X-Trace");
	}

	@Test void b03_httpToHttpsSameHost_isCrossOrigin_dropsCredentialHeaders() throws Exception {
		// An upgrade still changes scheme and default port, so it is a different origin.
		var hops = run(request("http://a.example.com/start"), "https://a.example.com/echo");
		assertList(headerNames(hops.last()), "Accept", "X-Trace");
	}

	//====================================================================================================
	// c - A redirect that cannot be followed fails with TransportException
	//====================================================================================================

	@Test void c01_invalidLocation_failsWithTransportException() {
		var ex = assertThrows(TransportException.class, () -> run(request("http://a.example.com/start"), "http://a b/echo"));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("invalid Location header"), ex.getMessage());
	}

	@Test void c02_locationHostNotParsableByUri_failsWithTransportException() {
		// java.net.URI parses an underscore host name with a null host, so the origins cannot be compared.
		var ex = assertThrows(TransportException.class, () -> run(request("http://a.example.com/start"), "http://my_service/echo"));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("my_service"), ex.getMessage());
	}
}
