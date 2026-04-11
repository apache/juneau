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

import org.apache.juneau.ng.http.HttpBody;
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

	// =================================================================================================================
	// H — Transport discovery and getTransport()
	// =================================================================================================================

	@Test
	void h01_create_autoDiscoversTransport() throws Exception {
		// NgRestClient.create() should find at least one transport (HC45 is on the test classpath)
		try (var client = NgRestClient.create()) {
			assertNotNull(client.getTransport(), "Expected a transport to be auto-discovered");
		}
	}

	@Test
	void h02_getTransport_returnsConfiguredTransport() throws Exception {
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder().transport(transport).build()) {
			assertSame(transport, client.getTransport());
		}
	}

	@Test
	void h03_request_absoluteUrl_bypassesRootUrl() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			// URL already contains "://" — rootUrl should NOT be prepended
			try (var r = client.request("GET", "http://other.com/path").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var uri = transport.getRecordedRequests().get(0).getUri();
		assertEquals("http://other.com/path", uri.toString());
	}

	// =================================================================================================================
	// I — CollectionFormat
	// =================================================================================================================

	@Test
	void i01_collectionFormat_comma() {
		var result = CollectionFormat.COMMA.join(java.util.List.of("red", "green", "blue"));
		assertEquals("red,green,blue", result);
	}

	@Test
	void i02_collectionFormat_pipe() {
		var result = CollectionFormat.PIPE.join(java.util.List.of("a", "b", "c"));
		assertEquals("a|b|c", result);
	}

	@Test
	void i03_collectionFormat_space() {
		var result = CollectionFormat.SPACE.join(java.util.List.of("x", "y"));
		assertEquals("x y", result);
	}

	@Test
	void i04_collectionFormat_tab() {
		var result = CollectionFormat.TAB.join(java.util.List.of("1", "2"));
		assertEquals("1\t2", result);
	}

	@Test
	void i05_collectionFormat_repeated_joinThrows() {
		assertThrows(IllegalArgumentException.class,
			() -> CollectionFormat.REPEATED.join(java.util.List.of("a")));
	}

	@Test
	void i06_collectionFormat_getDelimiter() {
		assertEquals(",", CollectionFormat.COMMA.getDelimiter());
		assertNull(CollectionFormat.REPEATED.getDelimiter());
	}

	@Test
	void i07_collectionFormat_join_singleElement() {
		var result = CollectionFormat.COMMA.join(java.util.List.of("only"));
		assertEquals("only", result);
	}

	// =================================================================================================================
	// J — NgRestCallException
	// =================================================================================================================

	@Test
	void j01_ngRestCallException_statusCode() {
		var ex = new NgRestCallException(404, "Not Found");
		assertEquals(404, ex.getStatusCode());
		assertEquals("Not Found", ex.getMessage());
	}

	@Test
	void j02_ngRestCallException_withCause() {
		var cause = new IOException("io error");
		var ex = new NgRestCallException(500, "Internal Server Error", cause);
		assertEquals(500, ex.getStatusCode());
		assertSame(cause, ex.getCause());
	}

	@Test
	void j03_ngRestCallException_noStatusCode() {
		var cause = new IOException("parse error");
		var ex = new NgRestCallException("Deserialization failed", cause);
		assertEquals(-1, ex.getStatusCode());
	}

	// =================================================================================================================
	// K — assertOk / assertStatus edge cases
	// =================================================================================================================

	@Test
	void k02_assertOk_throws_for_1xx() {
		// assertOk() should throw for status < 200
		var transport = MockHttpTransport.of(100, null);
		assertThrows(NgRestCallException.class, () -> {
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				try (var r = client.get("/continue").run()) {
					r.assertOk();
				}
			}
		});
	}

	@Test
	void k03_assertStatus_throws_on_mismatch() {
		var transport = MockHttpTransport.of(400, null);
		assertThrows(NgRestCallException.class, () -> {
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				try (var r = client.get("/bad").run()) {
					r.assertStatus(200);
				}
			}
		});
	}

	// =================================================================================================================
	// L — TransportException constructors
	// =================================================================================================================

	@Test
	void l01_transportException_message() throws Exception {
		var thrown = assertThrows(TransportException.class, () -> {
			var transport = MockHttpTransport.builder()
				.fallback(req -> { throw new TransportException("connection refused"); })
				.build();
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				client.get("/foo").run();
			}
		});
		assertEquals("connection refused", thrown.getMessage());
	}

	@Test
	void l02_transportException_withCause() throws Exception {
		var cause = new IOException("timeout");
		var thrown = assertThrows(TransportException.class, () -> {
			var transport = MockHttpTransport.builder()
				.fallback(req -> { throw new TransportException("transport failed", cause); })
				.build();
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				client.get("/foo").run();
			}
		});
		assertSame(cause, thrown.getCause());
	}

	@Test
	void l03_transportException_causeOnly() throws Exception {
		var cause = new IOException("connection reset");
		var thrown = assertThrows(TransportException.class, () -> {
			var transport = MockHttpTransport.builder()
				.fallback(req -> { throw new TransportException(cause); })
				.build();
			try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
				client.get("/foo").run();
			}
		});
		assertSame(cause, thrown.getCause());
	}

	// =================================================================================================================
	// M — HttpTransportProvider default method
	// =================================================================================================================

	@Test
	void m01_httpTransportProvider_defaultPriority() {
		// Create an anonymous provider to test the default getPriority() method
		HttpTransportProvider provider = new HttpTransportProvider() {
			@Override public boolean isAvailable() { return true; }
			@Override public HttpTransport create() { return MockHttpTransport.of(200, null); }
		};
		assertEquals(100, provider.getPriority());
	}

	// =================================================================================================================
	// N — NgRestRequest edge cases
	// =================================================================================================================

	@Test
	void n01_bodyWithNullContentType() throws Exception {
		// Body with null content-type should not add Content-Type header
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		var noTypeBody = new HttpBody() {
			@Override public String getContentType() { return null; }
			@Override public long getContentLength() { return 4; }
			@Override public void writeTo(java.io.OutputStream out) throws java.io.IOException { out.write("data".getBytes()); }
			@Override public boolean isRepeatable() { return true; }
		};
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.post("/upload").body(noTypeBody).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertNull(req.getFirstHeader("Content-Type"));
	}

	@Test
	void n02_pathData_nullValue_usesEmpty() throws Exception {
		// Null path variable value should be replaced with empty string
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/users/{id}").pathData("id", (String)null).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertEquals("/users/", req.getUri().getPath());
	}

	@Test
	void n03_urlWithExistingQueryString() throws Exception {
		// When baseUrl already contains '?', additional query params should be appended with '&'
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.get("/search?existing=yes").queryData("extra", "val").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var uri = transport.getRecordedRequests().get(0).getUri().toString();
		assertTrue(uri.contains("existing=yes"), "Expected existing=yes in: " + uri);
		assertTrue(uri.contains("extra=val"), "Expected extra=val in: " + uri);
		assertTrue(uri.contains("existing=yes&extra=val") || uri.contains("extra=val&existing=yes"),
			"Expected & separator in: " + uri);
	}

	@Test
	void n04_formData_nullValue_omitted() throws Exception {
		// Form data with null value should be omitted
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.post("/form")
					.formData("present", "yes")
					.formData("missing", (String)null)
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		var encoded = baos.toString(StandardCharsets.UTF_8);
		assertTrue(encoded.contains("present=yes"), "Expected present=yes in: " + encoded);
		assertFalse(encoded.contains("missing"), "Did not expect 'missing' in: " + encoded);
	}

	@Test
	void n05_headerWithNullValue_omitted() throws Exception {
		// Header with null supplier value should be omitted from the transport request
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		// Use Supplier<String> that returns null to cover the null-value branch
		java.util.function.Supplier<String> nullSupplier = () -> null;
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://example.com")
				.header("X-Api-Key", nullSupplier)
				.build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertNull(req.getFirstHeader("X-Api-Key"));
	}

	@Test
	void n06_formDataAndBodyBothSet_bodyWins() throws Exception {
		// When both formData() and body() are called, body takes precedence over formData
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://example.com").build()) {
			try (var r = client.post("/upload")
					.formData("ignored", "form-value")
					.body(StringBody.of("raw body", "text/plain"))
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("raw body", baos.toString(StandardCharsets.UTF_8));
		assertEquals("text/plain", req.getFirstHeader("Content-Type").value());
	}

	@Test
	void n07_noRootUrl_absoluteUrlRequest() throws Exception {
		// Client without rootUrl should pass the URL as-is to transport
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).build()) {
			try (var r = client.get("http://myhost.example.com/api/resource").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var uri = transport.getRecordedRequests().get(0).getUri();
		assertEquals("http://myhost.example.com/api/resource", uri.toString());
	}

	// =================================================================================================================
	// O — MockHttpTransport coverage
	// =================================================================================================================

	@Test
	void o01_routeMethodMismatch_fallsThrough() throws Exception {
		// When a route is registered for GET but POST is sent, the route should not match
		var transport = MockHttpTransport.builder()
			.on("GET", "/only-get", req -> TransportResponse.builder().statusCode(200).reasonPhrase("Matched GET").build())
			.fallback(req -> TransportResponse.builder().statusCode(404).reasonPhrase("No match").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/only-get").run()) {
				assertEquals(404, r.getStatusCode()); // route did not match (method mismatch)
			}
		}
	}

	@Test
	void o02_routePathMismatch_fallsThrough() throws Exception {
		// When a route is registered for /foo but /bar is requested, the route should not match
		var transport = MockHttpTransport.builder()
			.on("GET", "/foo", req -> TransportResponse.builder().statusCode(200).reasonPhrase("Matched /foo").build())
			.fallback(req -> TransportResponse.builder().statusCode(404).reasonPhrase("No match").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/bar").run()) {
				assertEquals(404, r.getStatusCode()); // route did not match (path mismatch)
			}
		}
	}

	@Test
	void o03_getRecordedRequests_recordingDisabled() throws Exception {
		// When recording is not enabled, getRecordedRequests() should return empty list
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		assertEquals(0, transport.getRecordedRequests().size());
	}

	@Test
	void o04_clearRecordedRequests_recordingDisabled() throws Exception {
		// When recording is not enabled, clearRecordedRequests() should be a no-op
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		transport.clearRecordedRequests(); // should not throw
	}

	@Test
	void o05_noRoutes_noFallback_returns404() throws Exception {
		// When no routes and no fallback are registered, unmatched requests should return 404
		var transport = MockHttpTransport.builder()
			.on("GET", "/other", req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/missing").run()) {
				assertEquals(404, r.getStatusCode());
			}
		}
	}

	@Test
	void o06_routeWithNullMethod_matchesAnyMethod() throws Exception {
		// A route registered with null method should match any HTTP method
		var transport = MockHttpTransport.builder()
			.on(null, "/wildcard-method", req -> TransportResponse.builder().statusCode(200).reasonPhrase("Matched").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/wildcard-method").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}

	@Test
	void o07_routeWithNullPath_matchesAnyPath() throws Exception {
		// A route registered with null path should match any path for its method
		var transport = MockHttpTransport.builder()
			.on("GET", null, req -> TransportResponse.builder().statusCode(200).reasonPhrase("Matched").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/any/path/at/all").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}
}

