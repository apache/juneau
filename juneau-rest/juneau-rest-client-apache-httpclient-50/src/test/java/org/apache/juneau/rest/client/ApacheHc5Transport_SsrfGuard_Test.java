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

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.apachehttpclient50.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the SSRF-guard (policy-covered {@code @Remote}) path of {@link ApacheHc5Transport} against a real
 * loopback server: a guarded request must always be pinned, even when an unguarded request on the same transport has
 * left an idle keep-alive connection to the same URL in the pool.
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc5Transport_SsrfGuard_Test {

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
		try (var transport = ApacheHc5Transport.create()) {
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a02_unguardedThenGuarded_sameTransport_guardedRejected() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
			// Fully consumed, so the keep-alive connection (opened without the pin) goes back to the pool.
			assertPlainOk(transport);
			// The guarded request must not ride that idle pooled connection; it goes through the pin and is refused.
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a03_guardedUnguardedGuarded_sameTransport_bothGuardedRejected() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
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
		try (var transport = ApacheHc5Transport.create()) {
			assertPinRefused(transport, TransportRequest.builder().method("GET").uri(numeric).remoteUrlPolicy(true, false).build(), "2130706433");
		}
	}

	// =================================================================================================================
	// B: supportsUrlPolicy
	// =================================================================================================================

	@Test
	void b01_supportsUrlPolicy_ownedClient_true() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
			assertTrue(transport.supportsUrlPolicy());
		}
	}

	@Test
	void b02_supportsUrlPolicy_callerSuppliedClient_false() throws Exception {
		try (var transport = ApacheHc5Transport.builder().httpClient(HttpClients.createDefault()).build()) {
			assertFalse(transport.supportsUrlPolicy(), "a caller-supplied client cannot be given pin-on-connect");
		}
	}

	// =================================================================================================================
	// B: per-client allowed address ranges (RestClient.Builder.allowAddressRanges)
	// =================================================================================================================

	private static final String LAB_URL = "http://224.0.0.1:1/x";
	private static final RemoteUrlPolicy.AllowedRanges LAB = RemoteUrlPolicy.AllowedRanges.of("224.0.0.0/4");

	private static TransportRequest ranged(String uri, RemoteUrlPolicy.AllowedRanges ranges) {
		return TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).allowedAddressRanges(ranges)
			.timeout(java.time.Duration.ofSeconds(2)).build();
	}

	@Test
	void b01_withRanges_specialPurposeTarget_notRefusedByPin_whileWithoutRangesIsRefused_sameTransport() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
			assertPinRefused(transport, ranged(LAB_URL, RemoteUrlPolicy.AllowedRanges.NONE), "224.0.0.1");
			try {
				transport.execute(ranged(LAB_URL, LAB)).close();
			} catch (TransportException e) {
				// The connect itself may fail (a multicast address cannot be connected to over TCP); it must not be the policy refusal.
				assertFalse(e.getMessage().contains("No allowed address"), e.getMessage());
			}
			// The allowed client's pool is separate: an unranged request to the same URL is still refused afterwards.
			assertPinRefused(transport, ranged(LAB_URL, RemoteUrlPolicy.AllowedRanges.NONE), "224.0.0.1");
		}
	}

	@Test
	void b02_withRanges_loopbackAndMetadata_stillRefused() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
			assertPinRefused(transport, ranged(url, LAB), "127.0.0.1");
			assertPinRefused(transport, ranged("http://169.254.169.254:1/x", LAB), "169.254.169.254");
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

	private static void assertPlainOk(ApacheHc5Transport transport) throws Exception {
		try (var response = transport.execute(plain())) {
			assertEquals(200, response.getStatusCode());
			assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	private static void assertRejectedLoopback(ApacheHc5Transport transport) {
		assertPinRefused(transport, guarded(), "127.0.0.1");
	}

	// A refused pin surfaces as the transport's documented TransportException, with the policy's IAE as its cause.
	private static void assertPinRefused(ApacheHc5Transport transport, TransportRequest request, String host) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(request).close());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("No allowed address for host '" + host + "'"), ex.getMessage());
	}
}
