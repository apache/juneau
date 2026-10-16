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

import org.apache.juneau.rest.client.jetty.*;
import org.eclipse.jetty.client.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the SSRF-guard (policy-covered {@code @Remote}) path of {@link JettyHttpTransport}: pin-on-connect through
 * the transport's own {@code HttpClient}, and the {@link HttpTransport#supportsUrlPolicy()} capability flag.
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class JettyHttpTransport_SsrfGuard_Test {

	// =================================================================================================================
	// A: supportsUrlPolicy
	// =================================================================================================================

	@Test
	void a01_supportsUrlPolicy_ownedClient_true() throws Exception {
		try (var transport = JettyHttpTransport.create()) {
			assertTrue(transport.supportsUrlPolicy());
		}
	}

	@Test
	void a02_supportsUrlPolicy_callerSuppliedClient_false() throws Exception {
		try (var transport = JettyHttpTransport.builder().httpClient(new HttpClient()).build()) {
			assertFalse(transport.supportsUrlPolicy());
		}
	}

	// =================================================================================================================
	// B: policy-covered requests are pinned to an allowed address; others on the same client are unaffected
	// =================================================================================================================

	@Test
	void b01_policyActive_loopbackTarget_rejectedAtResolve() throws Exception {
		var server = startHelloServer();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hello";
			var guarded = TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
			var plain = TransportRequest.builder().method("GET").uri(url).build();

			try (var transport = JettyHttpTransport.create()) {
				// Policy-covered: the pinning resolver refuses the loopback address, so the connect never happens.
				assertRejectedLoopback(transport, guarded);

				// The guard does not carry over: the same transport still connects normally for an unguarded request.
				try (var response = transport.execute(plain)) {
					assertEquals(200, response.getStatusCode());
				}
			}

			// A guarded request on a fresh transport (no pooled connection to reuse) is rejected the same way.
			try (var fresh = JettyHttpTransport.create()) {
				assertRejectedLoopback(fresh, guarded);
			}
		} finally {
			server.stop(0);
		}
	}

	@Test
	void b02_unguardedThenGuarded_sameTransport_guardedNeverReusesUnpinnedConnection() throws Exception {
		var server = startHelloServer();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hello";
			var guarded = TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
			var plain = TransportRequest.builder().method("GET").uri(url).build();

			try (var transport = JettyHttpTransport.create()) {
				// Fully consumed, so the keep-alive connection (opened without the pin) goes back to the pool.
				assertPlainOk(transport, plain);

				// The guarded request must not ride that idle pooled connection; it goes through the pin and is refused.
				assertRejectedLoopback(transport, guarded);
			}
		} finally {
			server.stop(0);
		}
	}

	@Test
	void b03_guardedUnguardedGuarded_sameTransport_bothGuardedRejected() throws Exception {
		var server = startHelloServer();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hello";
			var guarded = TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
			var plain = TransportRequest.builder().method("GET").uri(url).build();

			try (var transport = JettyHttpTransport.create()) {
				assertRejectedLoopback(transport, guarded);
				assertPlainOk(transport, plain);
				assertRejectedLoopback(transport, guarded);
			}
		} finally {
			server.stop(0);
		}
	}

	@Test
	void b04_guarded_numericLoopbackHost_rejectedAtResolve() throws Exception {
		// 2130706433 is 127.0.0.1 written as one 32-bit number; the pin resolves it to loopback and refuses it.
		try (var transport = JettyHttpTransport.create()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(TransportRequest.builder().method("GET").uri("http://2130706433:1/x").remoteUrlPolicy(true, false).build()));
			assertInstanceOf(IllegalArgumentException.class, ex.getCause());
			assertTrue(ex.getMessage().contains("No allowed address for host '2130706433'"), ex.getMessage());
		}
	}

	// =================================================================================================================
	// C: a URI whose host java.net.URI cannot parse (Jetty refuses a null host) fails with TransportException
	// =================================================================================================================

	@Test
	void c01_underscoreHost_refusedWithTransportException() throws Exception {
		try (var transport = JettyHttpTransport.create()) {
			assertHostRefused(transport, TransportRequest.builder().method("GET").uri("http://my_service:1/x").build());
			assertHostRefused(transport, TransportRequest.builder().method("GET").uri("http://my_service:1/x").remoteUrlPolicy(true, false).build());
		}
	}

	private static void assertHostRefused(JettyHttpTransport transport, TransportRequest request) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(request));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("Invalid URI host"), ex.getMessage());
	}

	// =================================================================================================================
	// D: per-client allowed address ranges (RestClient.Builder.allowAddressRanges)
	// =================================================================================================================

	private static final String LAB_URL = "http://224.0.0.1:1/x";
	private static final org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges LAB = org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.of("224.0.0.0/4");

	private static TransportRequest ranged(String uri, org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges ranges) {
		return TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).allowedAddressRanges(ranges)
			.timeout(Duration.ofSeconds(2)).build();
	}

	private static void assertPinRefused(JettyHttpTransport transport, TransportRequest req, String host) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
		assertTrue(ex.getMessage().contains("No allowed address for host '" + host + "'"), ex.getMessage());
	}

	@Test
	void d01_withRanges_specialPurposeTarget_notRefusedByPin_whileWithoutRangesIsRefused_sameTransport() throws Exception {
		try (var transport = JettyHttpTransport.create()) {
			var none = ranged(LAB_URL, org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.NONE);
			assertPinRefused(transport, none, "224.0.0.1");
			try {
				transport.execute(ranged(LAB_URL, LAB)).close();
			} catch (TransportException e) {
				// The connect itself may fail (a multicast address cannot be connected to over TCP); it must not be the policy refusal.
				assertFalse(e.getMessage().contains("No allowed address"), e.getMessage());
			}
			assertPinRefused(transport, none, "224.0.0.1");
		}
	}

	@Test
	void d02_withRanges_loopbackAndMetadata_stillRefused() throws Exception {
		try (var transport = JettyHttpTransport.create()) {
			assertPinRefused(transport, ranged("http://127.0.0.1:1/x", LAB), "127.0.0.1");
			assertPinRefused(transport, ranged("http://169.254.169.254:1/x", LAB), "169.254.169.254");
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static HttpServer startHelloServer() throws IOException {
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hello", exchange -> {
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		return server;
	}

	private static void assertPlainOk(JettyHttpTransport transport, TransportRequest plain) throws Exception {
		try (var response = transport.execute(plain)) {
			assertEquals(200, response.getStatusCode());
			assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	// A refused pin surfaces as the transport's documented TransportException, with the policy's IAE as its cause.
	private static void assertRejectedLoopback(JettyHttpTransport transport, TransportRequest guarded) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(guarded));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getMessage().contains("No allowed address for host '127.0.0.1'"), ex.getMessage());
	}
}
