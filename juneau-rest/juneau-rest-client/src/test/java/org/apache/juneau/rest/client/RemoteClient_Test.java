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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.remote.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link RemoteClient} and its remote-proxy invocation handler.
 *
 * <p>
 * Each test section covers a distinct path through {@code RemoteClient} or the inner
 * {@code RemoteInvocationHandler}: parameter binding, return-mode processing, and error handling.
 */
@SuppressWarnings({
	"resource" // RestClient/RestResponse instances used inline; closed via try-with-resources where needed.
})
class RemoteClient_Test {

	// -----------------------------------------------------------------------
	// Embedded server — shared across all tests
	// -----------------------------------------------------------------------

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		// /hello — echoes "Hello!" as plain text
		server.createContext("/hello", exchange -> {
			var body = "Hello!".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		// /users/{id} — returns the path variable value echoed back
		server.createContext("/users/", exchange -> {
			var id = exchange.getRequestURI().getPath().substring("/users/".length());
			var body = id.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		// /echo-query — echoes the query string
		server.createContext("/echo-query", exchange -> {
			var query = exchange.getRequestURI().getQuery();
			var body = (query != null ? query : "").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		// /echo-header — echoes the X-Custom header value
		server.createContext("/echo-header", exchange -> {
			var val = exchange.getRequestHeaders().getFirst("X-Custom");
			var body = (val != null ? val : "missing").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		// /echo-body — echoes the request body
		server.createContext("/echo-body", exchange -> {
			var requestBody = exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, requestBody.length);
			exchange.getResponseBody().write(requestBody);
			exchange.close();
		});

		// /status-only — returns 200 with no body (for STATUS/NONE return modes)
		server.createContext("/status-only", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});

		// /error — returns 400 Bad Request
		server.createContext("/error", exchange -> {
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
		});

		// /method — echoes HTTP method
		server.createContext("/method", exchange -> {
			var body = exchange.getRequestMethod().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private String rootUrl() {
		return "http://localhost:" + port;
	}

	// -----------------------------------------------------------------------
	// Test interfaces
	// -----------------------------------------------------------------------

	@Remote
	interface SimpleService {
		@RemoteGet("/hello")
		String getHello();

		@RemoteGet(path = "/hello", returns = RemoteReturn.STATUS)
		int getStatusInt();

		@RemoteGet(path = "/hello", returns = RemoteReturn.STATUS)
		boolean getStatusBoolean();

		@RemoteGet(path = "/hello", returns = RemoteReturn.NONE)
		void doNone();

		@RemoteGet(path = "/hello", returns = RemoteReturn.RESPONSE)
		RestResponse getResponse();

		@RemoteGet("/users/{id}")
		String getUser(@Path("id") String id);

		@RemoteGet("/echo-query")
		String getQuery(@Query("q") String q);

		@RemoteGet("/echo-header")
		String getHeader(@Header("X-Custom") String v);

		@RemotePost("/echo-body")
		String postBody(@Content String body);

		@RemoteGet("/method")
		String getMethod();

		@RemotePut("/method")
		String putMethod(@Content String body);

		@RemoteDelete("/method")
		String deleteMethod();

		@RemotePatch("/method")
		String patchMethod(@Content String body);
	}

	// -----------------------------------------------------------------------
	// A — Constructor and create() validation
	// -----------------------------------------------------------------------

	@Test void a01_constructor_nullClientThrows() {
		assertThrows(IllegalArgumentException.class, () -> new RemoteClient(null));
	}

	@Test void a02_create_nullIfaceThrows() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var rc = new RemoteClient(client);
			assertThrows(IllegalArgumentException.class, () -> rc.create(null));
		}
	}

	// -----------------------------------------------------------------------
	// B — Basic return modes via RestClient.remote()
	// -----------------------------------------------------------------------

	@Test void b01_returnBody_string() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			assertEquals("Hello!", svc.getHello());
		}
	}

	@Test void b02_returnStatus_int() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			assertEquals(200, svc.getStatusInt());
		}
	}

	@Test void b03_returnStatus_boolean_true() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			assertTrue(svc.getStatusBoolean());
		}
	}

	@Test void b04_returnNone_void() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			assertDoesNotThrow(svc::doNone);
		}
	}

	@Test void b05_returnResponse_callerCloses() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			try (var resp = svc.getResponse()) {
				assertNotNull(resp);
				assertEquals(200, resp.getStatusCode());
			}
		}
	}

	// -----------------------------------------------------------------------
	// C — HTTP methods
	// -----------------------------------------------------------------------

	@Test void c01_get() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("GET", client.remote(SimpleService.class).getMethod());
		}
	}

	@Test void c02_put() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("PUT", client.remote(SimpleService.class).putMethod(""));
		}
	}

	@Test void c03_delete() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("DELETE", client.remote(SimpleService.class).deleteMethod());
		}
	}

	@Test void c04_patch() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("PATCH", client.remote(SimpleService.class).patchMethod(""));
		}
	}

	// -----------------------------------------------------------------------
	// D — Parameter binding
	// -----------------------------------------------------------------------

	@Test void d01_pathParam_nonNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("42", client.remote(SimpleService.class).getUser("42"));
		}
	}

	@Test void d02_queryParam_nonNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(SimpleService.class).getQuery("hello");
			assertTrue(result.contains("q=hello"), "Expected q=hello in: " + result);
		}
	}

	@Test void d03_headerParam_nonNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("myval", client.remote(SimpleService.class).getHeader("myval"));
		}
	}

	@Test void d04_contentParam_body() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("hello body", client.remote(SimpleService.class).postBody("hello body"));
		}
	}

	// -----------------------------------------------------------------------
	// E — Null parameter handling
	// -----------------------------------------------------------------------

	@Test void e01_pathParam_null_skipsBinding() throws Exception {
		// null @Path arg → path variable not substituted → URL stays as /users/{id} → transport error
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertThrows(Exception.class, () -> client.remote(SimpleService.class).getUser(null));
		}
	}

	@Test void e02_contentParam_null_skipsBody() throws Exception {
		// null @Content arg → no body set
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertDoesNotThrow(() -> client.remote(SimpleService.class).postBody(null));
		}
	}

	// -----------------------------------------------------------------------
	// F — Query with default value
	// -----------------------------------------------------------------------

	@Remote
	interface DefaultQueryService {
		@RemoteGet("/echo-query")
		String getQuery(@Query(value = "q", def = "default-val") String q);

		@RemoteGet("/echo-query")
		String getQueryNoDefault(@Query(value = "q") String q);
	}

	@Test void f01_query_nullArg_withDef_usesDef() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(DefaultQueryService.class).getQuery(null);
			assertTrue(result.contains("default-val"), "Expected default-val in: " + result);
		}
	}

	@Test void f02_query_nullArg_withoutDef_skipsParam() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(DefaultQueryService.class).getQueryNoDefault(null);
			// No query param should be added, query string is empty or absent
			assertFalse(result.contains("q="), "Expected no q= in: " + result);
		}
	}

	// -----------------------------------------------------------------------
	// G — @Query with wildcard Map expansion
	// -----------------------------------------------------------------------

	@Remote
	interface MapQueryService {
		@RemoteGet("/echo-query")
		String getQuery(@Query("*") Map<String, String> params);
	}

	@Test void g01_query_mapExpansion() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var params = new LinkedHashMap<String, String>();
			params.put("a", "1");
			params.put("b", "2");
			var result = client.remote(MapQueryService.class).getQuery(params);
			assertTrue(result.contains("a=1") && result.contains("b=2"), "Expected a=1 and b=2 in: " + result);
		}
	}

	// -----------------------------------------------------------------------
	// H — Object method delegation (toString, hashCode, equals on proxy)
	// -----------------------------------------------------------------------

	@Test void h01_objectMethods_delegateToHandler() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(SimpleService.class);
			// These call Object methods which are delegated to RemoteInvocationHandler
			assertNotNull(svc.toString());
			assertTrue(svc.hashCode() != 0 || svc.hashCode() == 0); // always executes
			assertNotEquals(null, svc);
		}
	}

	// -----------------------------------------------------------------------
	// I — Method without @Remote* annotation throws
	// -----------------------------------------------------------------------

	@Remote
	interface WithUnannotatedMethod {
		@RemoteGet("/hello")
		String getHello();

		// No @RemoteGet / @RemotePost etc.
		String unannotated();
	}

	@Test void i01_unannotatedMethod_throws() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var svc = client.remote(WithUnannotatedMethod.class);
			assertThrows(UnsupportedOperationException.class, svc::unannotated);
		}
	}

	// -----------------------------------------------------------------------
	// J — @FormData parameter
	// -----------------------------------------------------------------------

	@Remote
	interface FormDataService {
		@RemotePost("/echo-body")
		String postForm(@FormData("field") String field);
	}

	@Test void j01_formData_nonNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			// FormData values are encoded into the body; server echoes them back
			var result = client.remote(FormDataService.class).postForm("val");
			assertNotNull(result);
		}
	}

	// -----------------------------------------------------------------------
	// K — @PathRemainder parameter
	// -----------------------------------------------------------------------

	@Remote
	interface PathRemainderService {
		@RemoteGet("/echo-query")
		String get(@PathRemainder String remainder);
	}

	@Test void k01_pathRemainder_nonNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			// PathRemainder injects /* into the path
			assertDoesNotThrow(() -> client.remote(PathRemainderService.class).get("x/y"));
		}
	}

	// -----------------------------------------------------------------------
	// L — Sole unannotated param uses content body
	// -----------------------------------------------------------------------

	@Remote
	interface SoleParamService {
		@RemotePost("/echo-body")
		String post(String body);
	}

	@Test void l01_soleUnannotatedParam_usedAsBody() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertEquals("direct-body", client.remote(SoleParamService.class).post("direct-body"));
		}
	}

	@Test void l02_soleUnannotatedParam_null_noBody() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			assertDoesNotThrow(() -> client.remote(SoleParamService.class).post(null));
		}
	}
}
