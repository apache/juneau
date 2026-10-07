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
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that {@link TransportRequest#getTimeout()} is honored per request by {@code ApacheHc45Transport} (WORK-J0596).
 *
 * <p>
 * Uses an embedded server whose {@code /slow} endpoint delays 2s before responding.
 */
@SuppressWarnings({
	"java:S2925", // The server handler sleeps to simulate a slow upstream.
	"resource" // Transport/response instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc45Transport_Timeout_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback, not the wildcard address + "localhost": a wildcard bind can be
		// handed an ephemeral port another process already holds on 127.0.0.1 (SO_REUSEADDR), which then wins every "localhost" connection.
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2000);
				var body = "ok".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			} catch (InterruptedException | IOException e) {
				// Client gave up; nothing to do.
			} finally {
				exchange.close();
			}
		});
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/fast");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/fast", exchange -> {
			var body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		// A dedicated executor keeps a sleeping /slow handler from blocking /fast on the single default dispatcher thread.
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.start();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
		executor.shutdownNow();
	}

	private static TransportRequest request(String path, Duration timeout) {
		return TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + port + path).timeout(timeout).build();
	}

	@Test void a01_shortTimeout_timesOutQuickly() throws Exception {
		try (var transport = ApacheHc45Transport.create()) {
			var start = System.nanoTime();
			var e = assertThrows(TransportException.class, () -> transport.execute(request("/slow", Duration.ofMillis(200))));
			assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1800, "should time out well before the 2s server delay");
			assertNotNull(e.getCause());
		}
	}

	@Test void a02_noTimeout_waitsForResponse() throws Exception {
		try (var transport = ApacheHc45Transport.create(); var response = transport.execute(request("/slow", null))) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test void a03_longTimeout_succeeds() throws Exception {
		try (var transport = ApacheHc45Transport.create(); var response = transport.execute(request("/slow", Duration.ofSeconds(10)))) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test void a05_clientLevelRequestConfig_survivesPerRequestTimeout() throws Exception {
		// The client disables redirects; a per-request timeout must not reset that to the HttpClient default (enabled).
		var client = org.apache.http.impl.client.HttpClients.custom().setDefaultRequestConfig(org.apache.http.client.config.RequestConfig.custom().setRedirectsEnabled(false).build()).build();
		try (var transport = ApacheHc45Transport.builder().httpClient(client).build();
			var response = transport.execute(request("/redirect", Duration.ofSeconds(10)))) {
			assertEquals(302, response.getStatusCode());
		}
	}

	@Test void a04_shortTimeout_fastEndpoint_succeeds() throws Exception {
		try (var transport = ApacheHc45Transport.create(); var response = transport.execute(request("/fast", Duration.ofSeconds(1)))) {
			assertEquals(200, response.getStatusCode());
		}
	}
}
