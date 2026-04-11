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
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;

import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.rest.client.*;
import org.apache.juneau.ng.rest.client.javahttpclient.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Integration tests for {@link JavaHttpTransport} against a real embedded HTTP server.
 */
public class JavaHttpTransport_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/hello", exchange -> {
			var body = "Hello, World!".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/echo-method", exchange -> {
			var body = exchange.getRequestMethod().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/echo-body", exchange -> {
			var requestBody = exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, requestBody.length);
			exchange.getResponseBody().write(requestBody);
			exchange.close();
		});

		server.createContext("/echo-header", exchange -> {
			var headerValue = exchange.getRequestHeaders().getFirst("X-Custom");
			var body = (headerValue != null ? headerValue : "missing").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/not-found", exchange -> {
			exchange.sendResponseHeaders(404, -1);
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

	// =================================================================================================================
	// A — Basic connectivity
	// =================================================================================================================

	@Test
	void a01_get_basicResponse() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/hello").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("Hello, World!", response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_get_statusCode404() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/not-found").run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	@Test
	void a03_get_responseHeader() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/hello").run()) {
				var ct = response.getFirstHeader("Content-Type");
				assertNotNull(ct);
				assertTrue(ct.value().startsWith("text/plain"), "Expected text/plain but got: " + ct.value());
			}
		}
	}

	// =================================================================================================================
	// B — HTTP methods
	// =================================================================================================================

	@Test
	void b01_post_echosMethod() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.post("/echo-method")
					.body(StringBody.of("", "text/plain"))
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("POST", response.getBodyAsString());
			}
		}
	}

	@Test
	void b02_put_echosMethod() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.put("/echo-method")
					.body(StringBody.of("", "text/plain"))
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("PUT", response.getBodyAsString());
			}
		}
	}

	@Test
	void b03_delete_echosMethod() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.delete("/echo-method").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("DELETE", response.getBodyAsString());
			}
		}
	}

	// =================================================================================================================
	// C — Request body
	// =================================================================================================================

	@Test
	void c01_post_stringBody() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.post("/echo-body")
					.body(StringBody.of("hello body", "text/plain"))
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("hello body", response.getBodyAsString());
			}
		}
	}

	@Test
	void c02_post_byteArrayBody() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			var bytes = "byte content".getBytes(StandardCharsets.UTF_8);
			try (var response = client.post("/echo-body")
					.body(ByteArrayBody.of(bytes, "application/octet-stream"))
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("byte content", response.getBodyAsString());
			}
		}
	}

	// =================================================================================================================
	// D — Request headers
	// =================================================================================================================

	@Test
	void d01_header_sentToServer() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/echo-header")
					.header("X-Custom", "my-value")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("my-value", response.getBodyAsString());
			}
		}
	}

	@Test
	void d02_missingHeader_returnsDefault() throws Exception {
		var transport = JavaHttpTransport.create();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/echo-header").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("missing", response.getBodyAsString());
			}
		}
	}

	// =================================================================================================================
	// E — Builder: explicit HttpClient
	// =================================================================================================================

	@Test
	void e01_builder_withExplicitHttpClient() throws Exception {
		var transport = JavaHttpTransport.builder()
			.httpClient(HttpClient.newHttpClient())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl(rootUrl()).build()) {
			try (var response = client.get("/hello").run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("Hello, World!", response.getBodyAsString());
			}
		}
	}

	// =================================================================================================================
	// F — Provider
	// =================================================================================================================

	@Test
	void f01_provider_isAvailable() {
		var provider = new JavaHttpTransportProvider();
		assertTrue(provider.isAvailable());
	}

	@Test
	void f02_provider_priority() {
		var provider = new JavaHttpTransportProvider();
		assertEquals(80, provider.getPriority());
	}

	@Test
	void f03_provider_create() throws Exception {
		var provider = new JavaHttpTransportProvider();
		try (var transport = provider.create()) {
			assertNotNull(transport);
		}
	}
}
