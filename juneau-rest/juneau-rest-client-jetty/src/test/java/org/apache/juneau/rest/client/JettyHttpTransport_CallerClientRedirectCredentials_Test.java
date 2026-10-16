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
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that a {@link JettyHttpTransport} built with a caller-supplied {@link HttpClient} does not forward
 * caller-set credential headers to a different origin when that client follows a redirect, while still forwarding
 * them on a same-origin redirect.
 *
 * <p>
 * Jetty's own request-copy step drops {@code Authorization}, {@code Cookie} and {@code Proxy-Authorization} on every
 * redirect, including a same-origin one, so a same-origin redirect keeps only {@code X-API-Key}; that is unchanged.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class JettyHttpTransport_CallerClientRedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static int portA;
	private static int portB;

	@BeforeAll
	static void startServers() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback; the two servers differ only by port, so they are different origins.
		serverB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", JettyHttpTransport_CallerClientRedirectCredentials_Test::echoCreds);
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", JettyHttpTransport_CallerClientRedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds", null));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, 302, "/echo-creds", null));
		serverA.createContext("/redirect-cross-307", exchange -> redirect(exchange, 307, "http://127.0.0.1:" + portB + "/echo-creds", null));
		serverA.createContext("/redirect-cross-308", exchange -> redirect(exchange, 308, "http://127.0.0.1:" + portB + "/echo-creds", null));
		serverA.createContext("/redirect-same-307", exchange -> redirect(exchange, 307, "/echo-creds", null));
		serverA.createContext("/redirect-cross-set-cookie", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds", "store=s-1; Path=/"));
		serverA.start();
	}

	@AfterAll
	static void stopServers() {
		if (serverA != null)
			serverA.stop(0);
		if (serverB != null)
			serverB.stop(0);
	}

	private static String all(HttpExchange exchange, String name) {
		var values = exchange.getRequestHeaders().get(name);
		return values == null || values.isEmpty() ? "none" : String.join(",", values);
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var body = ("auth=" + all(exchange, "Authorization") + "|cookie=" + all(exchange, "Cookie")
			+ "|key=" + all(exchange, "X-API-Key") + "|trace=" + all(exchange, "X-Trace")).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, int status, String location, String setCookie) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		if (setCookie != null)
			exchange.getResponseHeaders().add("Set-Cookie", setCookie);
		// Force connection close so Jetty opens a fresh connection for the redirected request instead of racing to reuse
		// this one, which com.sun.net.httpserver may already be tearing down.
		exchange.getResponseHeaders().set("Connection", "close");
		exchange.sendResponseHeaders(status, -1);
		exchange.close();
	}

	private static String get(String path) throws Exception {
		var transport = JettyHttpTransport.builder().httpClient(new HttpClient()).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get(path)
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.header("X-API-Key", "k-1")
					.header("X-Trace", "t-1")
					.run()) {
			assertEquals(200, response.getStatusCode());
			return response.getBodyAsString();
		}
	}

	@Test
	void a01_crossOrigin_callerClient_stripsCallerCredentials() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get("/redirect-cross"));
	}

	@Test
	void a02_sameOrigin_callerClient_keepsApiKey() throws Exception {
		assertEquals("auth=none|cookie=none|key=k-1|trace=t-1", get("/redirect-same"));
	}

	@Test
	void a03_crossOrigin_callerClient_cookieStoreStillApplies() throws Exception {
		// The cookie A sets on its redirect lives in the caller client's cookie store; a cookie's domain ignores the port, so
		// the store sends it on to B.  Only the caller-set Cookie value is dropped.
		assertEquals("auth=none|cookie=store=s-1|key=none|trace=t-1", get("/redirect-cross-set-cookie"));
	}

	@Test
	void a04_crossOrigin307_callerClient_stripsCallerCredentials() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get("/redirect-cross-307"));
	}

	@Test
	void a05_crossOrigin308_callerClient_stripsCallerCredentials() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get("/redirect-cross-308"));
	}

	@Test
	void a06_sameOrigin307_callerClient_keepsApiKey() throws Exception {
		assertEquals("auth=none|cookie=none|key=k-1|trace=t-1", get("/redirect-same-307"));
	}
}
