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
package org.apache.juneau.rest.client.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link AbstractMcpClient#send(JsonRpcRequest)} transparently retries a side-effect-free (read-only)
 * JSON-RPC method exactly once when a pooled keep-alive connection is torn down before any response is received,
 * while never replaying a mutating method (e.g. {@code tools/call}) under the same condition.
 *
 * <p>
 * Every MCP call is transported as an HTTP {@code POST}, so the underlying transport can never safely replay it;
 * this retry is keyed on JSON-RPC method idempotency instead, which is why it is verified at the client layer rather
 * than the transport layer.
 */
@SuppressWarnings({
	"resource" // Mock transports/clients are in-memory no-op closeables; test bodies close what matters via try-with-resources.
})
class AbstractMcpClient_StaleConnectionRetry_Test extends TestBase {

	/** Minimal concrete subclass so the abstract neutral core can be instantiated for testing. */
	static class TestClient extends AbstractMcpClient {

		TestClient(AbstractMcpClient.Builder<?> builder) {
			super(builder);
		}

		static class Builder extends AbstractMcpClient.Builder<Builder> {
			TestClient build() {
				return new TestClient(this);
			}
		}

		static Builder builder() {
			return new Builder();
		}
	}

	// A pre-response stale-connection failure, in the exact shape JavaHttpTransport surfaces it.
	private static TransportException staleFailure() {
		return new TransportException("HTTP transport error: HTTP/1.1 header parser received no bytes",
			new EOFException("EOF reached while reading"));
	}

	private static TransportResponse okEnvelope(String id) {
		return TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(("{\"jsonrpc\":\"2.0\",\"id\":\"" + id + "\",\"result\":{\"ok\":true}}").getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	// =================================================================================================================
	// A — Read-only method is retried once on a pre-response stale-connection failure
	// =================================================================================================================

	@Test
	void a01_readOnlyMethod_retriedOnceAfterStaleConnection() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			if (calls.incrementAndGet() == 1)
				throw staleFailure();
			return okEnvelope("1");
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var res = client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("resources/read"));
			assertNotNull(res);
			assertEquals("1", res.getId());
			assertNull(res.getError());
		}
		// One stale attempt + one successful retry.
		assertEquals(2, calls.get());
	}

	// =================================================================================================================
	// B — Mutating / non-retryable methods are NOT retried on the same failure
	// =================================================================================================================

	@Test
	void b01_toolsCall_notRetriedOnStaleConnection() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			throw staleFailure();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrows(TransportException.class, () -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("2").setMethod("tools/call")));
		}
		// Only the initial attempt — a mutating method must not be replayed.
		assertEquals(1, calls.get());
	}

	@Test
	void b02_readOnlyMethod_nonStaleError_notRetried() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			throw new TransportException("HTTP transport error: Connection refused", new ConnectException("Connection refused"));
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrows(TransportException.class, () -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("3").setMethod("resources/read")));
		}
		// A non-stale transport failure is not a retry trigger, even for a read-only method.
		assertEquals(1, calls.get());
	}
}
