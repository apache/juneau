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

import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the SSRF-guard (policy-covered {@code @Remote}) path of {@link ApacheHc45Transport} against a real
 * loopback server: a guarded request must always be pinned, even when an unguarded request on the same transport has
 * left an idle keep-alive connection to the same URL in the pool.
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc45Transport_SsrfGuard_Test {

	private static HttpServer server;
	private static String url;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hello", exchange -> {
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hello";
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	// =================================================================================================================
	// A: guarded requests never reuse a connection that was opened without the pin
	// =================================================================================================================

	@Test
	void a01_guarded_freshTransport_rejected() throws Exception {
		try (var transport = ApacheHc45Transport.create()) {
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a02_unguardedThenGuarded_sameTransport_guardedRejected() throws Exception {
		try (var transport = ApacheHc45Transport.create()) {
			// Fully consumed, so the keep-alive connection (opened without the pin) goes back to the pool.
			assertPlainOk(transport);
			// The guarded request must not ride that idle pooled connection; it goes through the pin and is refused.
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a03_guardedUnguardedGuarded_sameTransport_bothGuardedRejected() throws Exception {
		try (var transport = ApacheHc45Transport.create()) {
			assertRejectedLoopback(transport);
			assertPlainOk(transport);
			assertRejectedLoopback(transport);
			// The guard does not carry over to ordinary requests either.
			assertPlainOk(transport);
		}
	}

	@Test
	void a04_guarded_numericLoopbackHost_rejected() throws Exception {
		// 2130706433 is 127.0.0.1 written as one 32-bit number; the pin resolves it to loopback and refuses it.
		var numeric = url.replace("127.0.0.1", "2130706433");
		try (var transport = ApacheHc45Transport.create()) {
			assertPinRefused(transport, TransportRequest.builder().method("GET").uri(numeric).remoteUrlPolicy(true, false).build(), "2130706433");
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static TransportRequest guarded() {
		return TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
	}

	private static TransportRequest plain() {
		return TransportRequest.builder().method("GET").uri(url).build();
	}

	private static void assertPlainOk(ApacheHc45Transport transport) throws Exception {
		try (var response = transport.execute(plain())) {
			assertEquals(200, response.getStatusCode());
			assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	private static void assertRejectedLoopback(ApacheHc45Transport transport) {
		assertPinRefused(transport, guarded(), "127.0.0.1");
	}

	// A refused pin surfaces as the transport's documented TransportException, with the policy's IAE as its cause.
	private static void assertPinRefused(ApacheHc45Transport transport, TransportRequest request, String host) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(request).close());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("No allowed address for host '" + host + "'"), ex.getMessage());
	}
}
