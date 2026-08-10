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

import org.apache.juneau.http.entity.*;
import org.apache.juneau.rest.client.jetty.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link JettyHttpTransport} transparently retries a provably-idempotent request once when a pooled
 * keep-alive connection is torn down by the server before any response is received, while never replaying a
 * non-idempotent request (e.g. {@code POST}) under the same condition.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
class JettyHttpTransport_StaleConnectionRetry_Test {

	// =================================================================================================================
	// A — Idempotent request is retried once on a pre-response stale-connection failure
	// =================================================================================================================

	@Test
	void a01_get_retriedOnceAfterStaleConnection() throws Exception {
		try (var server = new A01_StaleServer(1)) {
			var transport = JettyHttpTransport.create();
			try (var client = RestClient.builder().transport(transport).rootUrl(server.rootUrl()).build()) {
				try (var response = client.get("/x").run()) {
					assertEquals(200, response.getStatusCode());
					assertEquals("OK", response.getBodyAsString());
				}
			}
			// One stale connection + one successful retry connection.
			assertEquals(2, server.connectionCount());
		}
	}

	// =================================================================================================================
	// B — Non-idempotent request is NOT retried on the same failure
	// =================================================================================================================

	@Test
	void b01_post_notRetriedOnStaleConnection() throws Exception {
		try (var server = new A01_StaleServer(1)) {
			var transport = JettyHttpTransport.create();
			try (var client = RestClient.builder().transport(transport).rootUrl(server.rootUrl()).build()) {
				assertThrows(TransportException.class, () -> client.post("/x").body(StringBody.of("data", "text/plain")).run());
			}
			// Only the initial connection — the POST must not be replayed.
			assertEquals(1, server.connectionCount());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Test double: a server that tears down the first N connections after reading the request (no response), then
	// responds 200 on subsequent connections — reproducing a stale pooled-connection reuse failure.
	// -----------------------------------------------------------------------------------------------------------------

	private static final class A01_StaleServer implements AutoCloseable {

		private final ServerSocket serverSocket;
		private final int staleConnections;
		private final AtomicInteger connectionCount = new AtomicInteger();

		A01_StaleServer(int staleConnections) throws IOException {
			this.staleConnections = staleConnections;
			this.serverSocket = new ServerSocket(0);
			var acceptThread = new Thread(this::acceptLoop, "jetty-stale-server");
			acceptThread.setDaemon(true);
			acceptThread.start();
		}

		String rootUrl() {
			return "http://localhost:" + serverSocket.getLocalPort();
		}

		int connectionCount() {
			return connectionCount.get();
		}

		private void acceptLoop() {
			while (! serverSocket.isClosed()) {
				try (var socket = serverSocket.accept()) {
					var n = connectionCount.incrementAndGet();
					consumeRequestHeaders(socket.getInputStream());
					if (n > staleConnections) {
						var body = "OK".getBytes(StandardCharsets.UTF_8);
						var out = socket.getOutputStream();
						out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
						out.write(body);
						out.flush();
					}
					// Otherwise close without responding (try-with-resources) to simulate a torn-down pooled connection.
				} catch (IOException e) {
					return; // Server socket closed during shutdown.
				}
			}
		}

		// Reads through the end of the request headers ("\r\n\r\n") so the client has fully written its request
		// before the connection is closed, guaranteeing a pre-response failure rather than a mid-write reset.
		private static void consumeRequestHeaders(InputStream in) throws IOException {
			var state = 0;
			int b;
			while ((b = in.read()) != -1) {
				var expected = (state == 0 || state == 2) ? '\r' : '\n';
				if (b == expected) {
					if (++state == 4)
						return;
				} else {
					state = (b == '\r') ? 1 : 0;
				}
			}
		}

		@Override
		public void close() throws IOException {
			serverSocket.close();
		}
	}
}
