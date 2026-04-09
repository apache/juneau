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
package org.apache.juneau.ng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.header.*;
import org.apache.juneau.ng.http.part.*;
import org.apache.juneau.ng.rest.client.*;
import org.apache.juneau.ng.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * First end-to-end tests for the next-generation REST client using {@link MockHttpTransport}.
 */
public class NgRestClient_Test {

	// =================================================================================================================
	// A — Basic GET / response reading
	// =================================================================================================================

	@Test
	void a01_get_basicResponse() throws Exception {
		var transport = MockHttpTransport.of(200, "Hello, World!");
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var response = client.get("/hello").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("Hello, World!", response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_get_routedResponse() throws Exception {
		var transport = MockHttpTransport.builder()
			.on("GET", "/greet", req -> TransportResponse.builder()
				.statusCode(200)
				.reasonPhrase("OK")
				.header("Content-Type", "text/plain")
				.body(new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)))
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var response = client.get("/greet").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("hi", response.getBodyAsString());
				var ct = response.getFirstHeader("Content-Type");
				assertNotNull(ct);
				assertEquals("text/plain", ct.value());
			}
		}
	}

	@Test
	void a03_get_unmatchedRouteReturns404() throws Exception {
		var transport = MockHttpTransport.builder().build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var response = client.get("/missing").run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// B — Query parameters
	// =================================================================================================================

	@Test
	void b01_queryData_string() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/search").queryData("q", "juneau").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var recorded = transport.getRecordedRequests();
		assertEquals(1, recorded.size());
		var uri = recorded.get(0).getUri();
		assertTrue(uri.toString().contains("q=juneau"), "Expected q=juneau in: " + uri);
	}

	@Test
	void b02_queryData_httpPart() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/items")
					.queryData(HttpPartBean.of("page", "2"), HttpPartBean.of("size", "10"))
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var uri = transport.getRecordedRequests().get(0).getUri().toString();
		assertTrue(uri.contains("page=2"), "Expected page=2 in: " + uri);
		assertTrue(uri.contains("size=10"), "Expected size=10 in: " + uri);
	}

	@Test
	void b03_nullQueryValue_omitted() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/items")
					.queryData("present", "yes")
					.queryData("absent", (String) null)
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var uri = transport.getRecordedRequests().get(0).getUri().toString();
		assertTrue(uri.contains("present=yes"), "Expected present=yes in: " + uri);
		assertFalse(uri.contains("absent"), "Did not expect 'absent' in: " + uri);
	}

	// =================================================================================================================
	// C — Default headers from builder
	// =================================================================================================================

	@Test
	void c01_defaultHeaders_appliedToEveryRequest() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://example.com")
				.header("X-Api-Version", "2")
				.build()) {
			try (var r = client.get("/a").run()) {
				assertEquals(200, r.getStatusCode());
			}
			try (var r = client.get("/b").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		for (var req : transport.getRecordedRequests()) {
			var h = req.getFirstHeader("X-Api-Version");
			assertNotNull(h, "Expected X-Api-Version header on " + req.getUri());
			assertEquals("2", h.value());
		}
	}

	@Test
	void c02_header_httpHeaderBean() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/a")
					.headers(HttpHeaderBean.of("Authorization", "Bearer token123"))
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var auth = req.getFirstHeader("Authorization");
		assertNotNull(auth);
		assertEquals("Bearer token123", auth.value());
	}

	// =================================================================================================================
	// D — POST with body
	// =================================================================================================================

	@Test
	void d01_post_stringBody() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(201).reasonPhrase("Created").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.post("/create")
					.body(StringBody.of("{\"name\":\"test\"}", "application/json"))
					.run()) {
				assertEquals(201, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var ct = req.getFirstHeader("Content-Type");
		assertNotNull(ct);
		assertEquals("application/json", ct.value());
		var body = req.getBody();
		assertNotNull(body);
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		assertEquals("{\"name\":\"test\"}", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void d02_post_formData() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.post("/login")
					.formData("username", "alice")
					.formData("password", "secret")
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var ct = req.getFirstHeader("Content-Type");
		assertNotNull(ct);
		assertEquals("application/x-www-form-urlencoded", ct.value());
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		var encoded = baos.toString(StandardCharsets.UTF_8);
		assertTrue(encoded.contains("username=alice"), "Expected username=alice in: " + encoded);
		assertTrue(encoded.contains("password=secret"), "Expected password=secret in: " + encoded);
	}

	// =================================================================================================================
	// E — Path substitution
	// =================================================================================================================

	@Test
	void e01_pathData_substitution() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/users/{id}")
					.pathData("id", "42")
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertEquals("/users/42", req.getUri().getPath());
	}

	@Test
	void e02_pathData_httpPart() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/orgs/{org}/repos/{repo}")
					.pathData(HttpPartBean.of("org", "apache"), HttpPartBean.of("repo", "juneau"))
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertEquals("/orgs/apache/repos/juneau", req.getUri().getPath());
	}

	// =================================================================================================================
	// F — assertOk / assertStatus
	// =================================================================================================================

	@Test
	void f01_assertOk_passes() throws Exception {
		var transport = MockHttpTransport.of(200, null);
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/ok").run().assertOk()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}

	@Test
	void f02_assertOk_throws() {
		var transport = MockHttpTransport.of(500, null);
		assertThrows(NgRestCallException.class, () -> {
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				try (var r = client.get("/error").run()) {
					r.assertOk();
				}
			}
		});
	}

	@Test
	void f03_assertStatus_matches() throws Exception {
		var transport = MockHttpTransport.of(204, null);
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.delete("/item/1").run().assertStatus(204)) {
				assertEquals(204, r.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// G — MockHttpTransport recording
	// =================================================================================================================

	@Test
	void g01_recordRequests_capturesAll() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			client.get("/a").run().close();
			client.post("/b").run().close();
			client.delete("/c").run().close();
		}
		assertEquals(3, transport.getRecordedRequests().size());
		assertEquals("GET", transport.getRecordedRequests().get(0).getMethod());
		assertEquals("POST", transport.getRecordedRequests().get(1).getMethod());
		assertEquals("DELETE", transport.getRecordedRequests().get(2).getMethod());
	}

	@Test
	void g02_clearRecordedRequests() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			client.get("/a").run().close();
			assertEquals(1, transport.getRecordedRequests().size());
			transport.clearRecordedRequests();
			assertEquals(0, transport.getRecordedRequests().size());
			client.get("/b").run().close();
			assertEquals(1, transport.getRecordedRequests().size());
		}
	}
}
