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
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that {@link ApacheHc45Transport} does not forward caller-set credential headers when a request is
 * transparently replayed against a different origin as the result of a redirect, while still forwarding them
 * on a same-origin redirect.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc45Transport_RedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static int portA;
	private static int portB;
	private static final AtomicReference<String> firstHopAuth = new AtomicReference<>();
	private static final AtomicReference<String> firstHopCookie = new AtomicReference<>();
	private static final AtomicReference<String> firstHopKey = new AtomicReference<>();
	private static final AtomicReference<String> lastKey = new AtomicReference<>();

	@BeforeAll
	static void startServers() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback, not the wildcard address + "localhost": a wildcard bind can be
		// handed an ephemeral port another process already holds on 127.0.0.1 (SO_REUSEADDR), which then wins every "localhost" connection.
		serverB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", ApacheHc45Transport_RedirectCredentials_Test::echoCreds);
		serverB.createContext("/bounce-to-a", exchange -> redirect(exchange, "http://127.0.0.1:" + portA + "/echo-creds"));
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", ApacheHc45Transport_RedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, "http://127.0.0.1:" + portA + "/echo-creds"));
		serverA.createContext("/redirect-a-b-a", exchange -> {
			firstHopAuth.set(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
			firstHopCookie.set(String.valueOf(exchange.getRequestHeaders().getFirst("Cookie")));
			firstHopKey.set(String.valueOf(exchange.getRequestHeaders().getFirst("X-API-Key")));
			redirect(exchange, "http://127.0.0.1:" + portB + "/bounce-to-a");
		});
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
		var auth = exchange.getRequestHeaders().getFirst("Authorization");
		var cookie = exchange.getRequestHeaders().getFirst("Cookie");
		lastKey.set(String.valueOf(exchange.getRequestHeaders().getFirst("X-API-Key")));
		var body = ("auth=" + (auth != null ? auth : "none") + "|cookie=" + (cookie != null ? cookie : "none"))
			.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	@Test
	void a01_crossOrigin_stripsCredentials() throws Exception {
		var transport = ApacheHc45Transport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-cross")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=none|cookie=none", response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_sameOrigin_forwardsCredentials() throws Exception {
		var transport = ApacheHc45Transport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-same")
					.header("Authorization", "Bearer secret-token")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=Bearer secret-token|cookie=none", response.getBodyAsString());
			}
		}
	}

	@Test
	void a03_originBackToFirstOrigin_credentialsRestored() throws Exception {
		firstHopAuth.set(null);
		firstHopCookie.set(null);
		firstHopKey.set(null);
		lastKey.set(null);
		var transport = ApacheHc45Transport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-a-b-a")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.header("X-API-Key", "k-1")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("Bearer secret-token", firstHopAuth.get());
				assertEquals("session=abc123", firstHopCookie.get());
				assertEquals("k-1", firstHopKey.get());
				assertEquals("auth=Bearer secret-token|cookie=session=abc123", response.getBodyAsString());
				assertEquals("k-1", lastKey.get());
			}
		}
	}
}
