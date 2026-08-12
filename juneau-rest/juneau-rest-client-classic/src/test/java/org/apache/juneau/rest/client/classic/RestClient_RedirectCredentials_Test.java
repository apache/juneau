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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that the classic {@link RestClient} does not forward caller-set credential headers when a request
 * is transparently replayed against a different origin as the result of a redirect, while still forwarding
 * them on a same-origin redirect.
 */
@SuppressWarnings({
	"resource" // Client instances are short-lived test fixtures.
})
class RestClient_RedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static ExecutorService executor;
	private static int portA;
	private static int portB;

	@BeforeAll
	static void startServers() throws IOException {
		// Without an explicit executor, exchanges run on each HttpServer's single internal dispatch thread,
		// which starves under -T1C reactor-level parallel test load and can fail with "server failed to
		// respond". One shared pool is enough since both servers only ever field short-lived test requests.
		executor = Executors.newCachedThreadPool();

		serverB = HttpServer.create(new InetSocketAddress(0), 0);
		portB = serverB.getAddress().getPort();
		serverB.setExecutor(executor);
		serverB.createContext("/echo-creds", RestClient_RedirectCredentials_Test::echoCreds);
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress(0), 0);
		portA = serverA.getAddress().getPort();
		serverA.setExecutor(executor);
		serverA.createContext("/echo-creds", RestClient_RedirectCredentials_Test::echoCreds);
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
		if (executor != null)
			executor.shutdownNow();
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var auth = exchange.getRequestHeaders().getFirst("Authorization");
		var cookie = exchange.getRequestHeaders().getFirst("Cookie");
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
		try (var client = RestClient.create().rootUrl("http://localhost:" + portA).build()) {
			try (var response = client.get("/redirect-cross")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=none|cookie=none", response.getContent().asString());
			}
		}
	}

	@Test
	void a02_sameOrigin_forwardsCredentials() throws Exception {
		try (var client = RestClient.create().rootUrl("http://localhost:" + portA).build()) {
			try (var response = client.get("/redirect-same")
					.header("Authorization", "Bearer secret-token")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=Bearer secret-token|cookie=none", response.getContent().asString());
			}
		}
	}
}
