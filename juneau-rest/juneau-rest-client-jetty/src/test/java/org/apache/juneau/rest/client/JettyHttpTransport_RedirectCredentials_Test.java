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

import org.apache.juneau.rest.client.jetty.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that {@link JettyHttpTransport} does not forward caller-set credential headers when a request is
 * transparently replayed against a different origin as the result of a redirect, while still forwarding them
 * on a same-origin redirect.
 *
 * <p>
 * Jetty's request-copy step already drops {@code Authorization}, {@code Cookie}, and {@code Proxy-Authorization}
 * on any redirect, but it forwards other caller-set credential headers such as {@code X-API-Key}.  These tests
 * therefore use {@code X-API-Key} as the observable signal for the transport's origin-aware stripping.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
class JettyHttpTransport_RedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static int portA;
	private static int portB;

	@BeforeAll
	static void startServers() throws IOException {
		serverB = HttpServer.create(new InetSocketAddress(0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", JettyHttpTransport_RedirectCredentials_Test::echoCreds);
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress(0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", JettyHttpTransport_RedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, "http://localhost:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, "http://localhost:" + portA + "/echo-creds"));
		serverA.start();
	}

	@AfterAll
	static void stopServers() {
		if (serverA != null)
			serverA.stop(0);
		if (serverB != null)
			serverB.stop(0);
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var key = exchange.getRequestHeaders().getFirst("X-API-Key");
		var body = ("key=" + (key != null ? key : "none")).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		// Force connection close so Jetty's HttpClient always opens a fresh connection for the redirected
		// request instead of racing to reuse this connection, which com.sun.net.httpserver may already be
		// tearing down (observed as a same-origin-redirect EOFException on the reused socket).
		exchange.getResponseHeaders().set("Connection", "close");
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	@Test
	void a01_crossOrigin_stripsCredentials() throws Exception {
		var transport = JettyHttpTransport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://localhost:" + portA).build()) {
			try (var response = client.get("/redirect-cross")
					.header("X-API-Key", "secret-key")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("key=none", response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_sameOrigin_forwardsCredentials() throws Exception {
		var transport = JettyHttpTransport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://localhost:" + portA).build()) {
			try (var response = client.get("/redirect-same")
					.header("X-API-Key", "secret-key")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("key=secret-key", response.getBodyAsString());
			}
		}
	}
}
