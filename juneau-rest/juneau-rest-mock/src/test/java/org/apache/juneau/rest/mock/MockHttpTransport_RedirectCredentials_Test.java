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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link MockHttpTransport}'s policy-covered redirect loop does not forward caller-set credential
 * headers to a different origin, while still forwarding them on a same-origin redirect.
 */
@SuppressWarnings({
	"resource" // Transport and TransportResponse instances are short-lived in-memory test fixtures.
})
class MockHttpTransport_RedirectCredentials_Test {

	private static MockHttpTransport transport() {
		return MockHttpTransport.builder()
			.recordRequests()
			.on("GET", "/cross", req -> redirect("http://b.example.com/echo"))
			.on("GET", "/downgrade", req -> redirect("http://a.example.com/echo"))
			.on("GET", "/same", req -> redirect("/echo"))
			.on("GET", "/echo", req -> TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(new byte[0])).build())
			.build();
	}

	private static TransportResponse redirect(String location) {
		return TransportResponse.builder().statusCode(302).header("Location", location).build();
	}

	private static TransportRequest request(String uri) {
		return TransportRequest.builder()
			.method("GET")
			.uri(uri)
			.remoteUrlPolicy(true, false)
			.header("Authorization", "Bearer secret-token")
			.header("Cookie", "session=abc123")
			.header("X-API-Key", "key-123")
			.header("X-Trace", "t-1")
			.build();
	}

	private static List<String> headerNames(TransportRequest r) {
		return r.getHeaders().stream().map(TransportHeader::name).toList();
	}

	@Test void a01_crossOriginRedirect_dropsCredentialHeaders() throws Exception {
		var t = transport();
		assertBean(t.execute(request("http://a.example.com/cross")), "statusCode", "200");
		var hops = t.getRecordedRequests();
		assertList(hops.stream().map(TransportRequest::getUri).toList(), "http://a.example.com/cross", "http://b.example.com/echo");
		assertList(headerNames(hops.get(1)), "X-Trace");
	}

	@Test void a02_httpsToHttpDowngrade_dropsCredentialHeaders() throws Exception {
		var t = transport();
		t.execute(request("https://a.example.com/downgrade"));
		assertList(headerNames(t.getRecordedRequests().get(1)), "X-Trace");
	}

	@Test void a03_sameOriginRedirect_keepsCredentialHeaders() throws Exception {
		var t = transport();
		t.execute(request("http://a.example.com/same"));
		var hops = t.getRecordedRequests();
		assertBean(hops.get(1), "uri", "http://a.example.com/echo");
		assertList(headerNames(hops.get(1)), "Authorization", "Cookie", "X-API-Key", "X-Trace");
	}

	@Test void a04_notPolicyCovered_noRedirectLoop() throws Exception {
		// Without the @Remote URL policy the mock transport dispatches once and returns the 302 to the caller.
		var t = transport();
		var r = TransportRequest.builder().method("GET").uri("http://a.example.com/cross").header("Authorization", "x").build();
		assertBean(t.execute(r), "statusCode", "302");
		assertList(t.getRecordedRequests().stream().map(TransportRequest::getUri).toList(), "http://a.example.com/cross");
	}
}
