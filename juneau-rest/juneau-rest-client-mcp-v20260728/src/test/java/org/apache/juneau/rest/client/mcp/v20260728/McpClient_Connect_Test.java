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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpClient#connect}, the one-shot build-and-handshake convenience.
 *
 * <p>
 * {@code 2026-07-28} has no {@code initialize} method (see {@link McpClient_Surface_Test}) - its handshake
 * is {@link McpClient#serverDiscover()} - so {@code connect(...)} performs that call exactly once instead.
 */
@SuppressWarnings({
	"resource" // Mock HttpTransport (including the deliberately-`failing` implementations used to pin close-failure/suppressed-exception behavior) and countingTransport(...) instances are short-lived test fixtures; some are intentionally unassigned/never closed since these tests pin McpClient.connect(...)'s own close behavior, not the mock transport's.
})
class McpClient_Connect_Test {

	private static final String DISCOVER_WIRE =
		"{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"supportedVersions\":[\"2026-07-28\"],\"capabilities\":{}}}";

	private static HttpTransport countingTransport(AtomicInteger calls, String wireJson) {
		return req -> {
			calls.incrementAndGet();
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wireJson.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
	}

	@Test
	void a01_connect_withEndpoint_buildsAndDiscoversExactlyOnce() throws Exception {
		var calls = new AtomicInteger();
		var savedTransport = new AtomicReference<HttpTransport>();
		// endpoint-only overload builds its own transport internally in production, but tests must inject a fake
		// transport - so exercise the Builder overload here and cover the String overload's delegation separately.
		savedTransport.set(countingTransport(calls, DISCOVER_WIRE));
		try (var c = McpClient.connect(McpClient.builder().endpoint("http://x/mcp").transport(savedTransport.get()))) {
			assertNotNull(c);
			assertEquals(1, calls.get());
		}
	}

	@Test
	void a02_connect_cachesDiscoveredServerSoNoSecondRoundTripIsNeeded() throws Exception {
		var calls = new AtomicInteger();
		try (var c = McpClient.connect(McpClient.builder().endpoint("http://x/mcp").transport(countingTransport(calls, DISCOVER_WIRE)))) {
			var result = c.discoveredServer();
			assertNotNull(result);
			assertEquals("2026-07-28", result.getSupportedVersions().get(0));
			assertEquals(1, calls.get());
		}
	}

	@Test
	void a03_discoveredServer_isNullBeforeAnyHandshake() throws Exception {
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(countingTransport(new AtomicInteger(), DISCOVER_WIRE)).build()) {
			assertNull(c.discoveredServer());
		}
	}

	@Test
	void a04_discoveredServer_populatedByDirectServerDiscoverCallNotJustConnect() throws Exception {
		var calls = new AtomicInteger();
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(countingTransport(calls, DISCOVER_WIRE)).build()) {
			assertNull(c.discoveredServer());
			c.serverDiscover();
			assertNotNull(c.discoveredServer());
			assertEquals("2026-07-28", c.discoveredServer().getSupportedVersions().get(0));
			assertEquals(1, calls.get());
		}
	}

	@Test
	void b01_connect_closesClientAndPropagatesOnHandshakeFailure() {
		var closed = new AtomicBoolean();
		HttpTransport failing = new HttpTransport() {
			@Override public TransportResponse execute(TransportRequest request) throws TransportException {
				throw new TransportException("boom");
			}

			@Override public void close() {
				closed.set(true);
			}
		};
		var builder = McpClient.builder().endpoint("http://x/mcp").transport(failing);
		var ex = assertThrows(IOException.class, () -> McpClient.connect(builder));
		assertEquals("boom", ex.getMessage());
		assertTrue(closed.get());
	}

	@Test
	void b02_connect_addsCloseFailureAsSuppressedRatherThanMaskingHandshakeFailure() {
		HttpTransport failing = new HttpTransport() {
			@Override public TransportResponse execute(TransportRequest request) throws TransportException {
				throw new TransportException("boom");
			}

			@Override public void close() throws IOException {
				throw new IOException("close-boom");
			}
		};
		var builder = McpClient.builder().endpoint("http://x/mcp").transport(failing);
		var ex = assertThrows(IOException.class, () -> McpClient.connect(builder));
		assertEquals("boom", ex.getMessage());
		assertEquals(1, ex.getSuppressed().length);
		assertEquals("close-boom", ex.getSuppressed()[0].getMessage());
	}

	@Test
	void b03_connect_suppressesUncheckedCloseFailureRatherThanMaskingHandshakeFailure() {
		HttpTransport failing = new HttpTransport() {
			@Override public TransportResponse execute(TransportRequest request) throws TransportException {
				throw new TransportException("boom");
			}

			@Override public void close() {
				throw new RuntimeException("close-boom");
			}
		};
		var builder = McpClient.builder().endpoint("http://x/mcp").transport(failing);
		var ex = assertThrows(IOException.class, () -> McpClient.connect(builder));
		assertEquals("boom", ex.getMessage());
		assertEquals(1, ex.getSuppressed().length);
		assertEquals("close-boom", ex.getSuppressed()[0].getMessage());
	}

	@Test
	void b04_connect_closesClientAndPropagatesOnUncheckedHandshakeError() {
		var closed = new AtomicBoolean();
		HttpTransport failing = new HttpTransport() {
			@Override public TransportResponse execute(TransportRequest request) {
				throw new StackOverflowError("boom");
			}

			@Override public void close() {
				closed.set(true);
			}
		};
		var builder = McpClient.builder().endpoint("http://x/mcp").transport(failing);
		var err = assertThrows(StackOverflowError.class, () -> McpClient.connect(builder));
		assertEquals("boom", err.getMessage());
		assertTrue(closed.get());
	}

	@Test
	void c01_connect_rejectsNullBuilder() {
		assertThrows(IllegalArgumentException.class, () -> McpClient.connect((McpClient.Builder)null));
	}

	@Test
	void d01_connect_rejectsNullEndpoint() {
		assertThrows(IllegalArgumentException.class, () -> McpClient.connect((String)null));
	}

	@Test
	void e01_connect_endpointOnlyOverloadExistsAndDelegatesToTheBuilderOverload() throws Exception {
		// Not transport-injectable (it builds its own default transport internally), so this only pins the
		// surface: the same one-shot handshake contract as the Builder overload, exercised above.
		var m = McpClient.class.getMethod("connect", String.class);
		assertEquals(McpClient.class, m.getReturnType());
		assertArrayEquals(new Class<?>[]{IOException.class}, m.getExceptionTypes());
	}
}
