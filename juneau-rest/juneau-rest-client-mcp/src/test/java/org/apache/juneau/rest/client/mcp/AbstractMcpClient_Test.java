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
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link AbstractMcpClient#send(JsonRpcRequest)}.
 */
class AbstractMcpClient_Test {

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

	@Test
	void a01_send_successResult_returnsResponseEnvelope() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"ok\":true}}";
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping");
			var res = client.send(req);
			assertEquals("1", res.getId());
			assertNull(res.getError());
			assertNotNull(res.getResult());
		}
	}

	@Test
	void a02_send_errorResult_returnsResponseEnvelopeWithError_doesNotThrow() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}";
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("2").setMethod("bogus/method");
			var res = client.send(req);
			assertNull(res.getResult());
			assertNotNull(res.getError());
			assertEquals(-32601, res.getError().getCode());
		}
	}

	@Test
	void a03_send_notification_returnsNull() throws Exception {
		var called = new AtomicInteger();
		HttpTransport transport = tReq -> {
			called.incrementAndGet();
			return TransportResponse.builder().statusCode(202).build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setMethod("notifications/x");
			var res = client.send(req);
			assertNull(res);
			assertEquals(1, called.get());
		}
	}

	@Test
	void a04_send_postsToConfiguredEndpoint() throws Exception {
		var seenUri = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenUri.set(tReq.getUri().toString());
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"9\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("9").setMethod("ping"));
			assertEquals("http://x/mcp", seenUri.get());
		}
	}

	@Test
	void a05_send_serializesRequestBodyAsJson() throws Exception {
		var seenContentType = new AtomicReference<String>();
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("Content-Type");
			seenContentType.set(h == null ? null : h.value());
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				seenBody.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping"));
			assertNotNull(seenContentType.get());
			assertTrue(seenContentType.get().contains("json"));
			assertNotNull(seenBody.get());
			assertTrue(seenBody.get().contains("\"jsonrpc\":\"2.0\""));
			assertTrue(seenBody.get().contains("\"id\":\"1\""));
			assertTrue(seenBody.get().contains("\"method\":\"ping\""));
		}
	}

	@Test
	void a06_send_closesTransportResponse_onAllPaths() throws Exception {
		var successClosed = new AtomicBoolean();
		HttpTransport successTransport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(() -> successClosed.set(true))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(successTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping"));
		}
		assertTrue(successClosed.get());

		var errorClosed = new AtomicBoolean();
		HttpTransport errorTransport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"error\":{\"code\":-32000,\"message\":\"boom\"}}".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(() -> errorClosed.set(true))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(errorTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("2").setMethod("ping"));
		}
		assertTrue(errorClosed.get());

		var notificationClosed = new AtomicBoolean();
		HttpTransport notificationTransport = tReq -> TransportResponse.builder()
			.statusCode(202)
			.closeCallback(() -> notificationClosed.set(true))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(notificationTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setMethod("notifications/x"));
		}
		assertTrue(notificationClosed.get());

		var parseFailureClosed = new AtomicBoolean();
		HttpTransport parseFailureTransport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(() -> parseFailureClosed.set(true))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(parseFailureTransport).build()) {
			assertThrows(IOException.class, () -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("3").setMethod("ping")));
		}
		assertTrue(parseFailureClosed.get());
	}

	@Test
	void a07_send_non2xxWithNonEnvelope_throwsStatusDiagnostic() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.header("Content-Type", "text/plain")
			.body(new ByteArrayInputStream("server down".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var e = assertThrows(IOException.class, () -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("7").setMethod("ping")));
			assertEquals("MCP server returned HTTP 500 and the body was not a JSON-RPC envelope.", e.getMessage());
		}
	}

	@Test
	void a08_send_non2xxWithJsonRpcErrorEnvelope_returnsEnvelope() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"8\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("8").setMethod("ping");
			var res = client.send(req);
			assertNotNull(res);
			assertNotNull(res.getError());
			assertEquals(-32603, res.getError().getCode());
		}
	}

	@Test
	void a09_send_nonNotificationWithoutBody_throwsIOException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("9").setMethod("ping");
			var e = assertThrows(IOException.class, () -> client.send(req));
			assertEquals("No response body received for JSON-RPC request id '9' (HTTP 200).", e.getMessage());
		}
	}

	@Test
	void a10_close_closesWrappedTransportOnce() throws Exception {
		var closeCount = new AtomicInteger();
		HttpTransport transport = new HttpTransport() {
			@Override /* HttpTransport */
			public TransportResponse execute(TransportRequest request) {
				return TransportResponse.builder().statusCode(200).build();
			}

			@Override /* Closeable */
			public void close() {
				closeCount.incrementAndGet();
			}
		};
		var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build();
		client.close();
		assertEquals(1, closeCount.get());
	}

	@Test
	void a11_send_2xxWithUnparseableBody_rethrowsOriginalParseError() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("not json {{{".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("11").setMethod("ping");
			var e = assertThrows(IOException.class, () -> client.send(req));
			assertFalse(e.getMessage().contains("MCP server returned HTTP"));
			assertNotEquals("No response body received for JSON-RPC request id '11' (HTTP 200).", e.getMessage());
		}
	}

	@Test
	void a12_send_sub200StatusWithNonEnvelope_throwsStatusDiagnostic() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(199)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("not json {{{".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("12").setMethod("ping");
			var e = assertThrows(IOException.class, () -> client.send(req));
			assertTrue(e.getMessage().contains("MCP server returned HTTP 199"));
		}
	}

	@Test
	void a13_send_withHttpHeaders_setsHeadersOnTransportRequest() throws Exception {
		var seen = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("X-Foo");
			seen.set(h == null ? null : h.value());
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"13\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("13").setMethod("ping");
			var res = client.send(req, Map.of("X-Foo", "bar"));
			assertNotNull(res);
			assertEquals("bar", seen.get());
		}
	}

	@Test
	void a14_send_nullHttpHeaders_behavesLikeOriginalSendPath() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"14\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("14").setMethod("ping");
			var res = client.send(req, null);
			assertNotNull(res);
			assertEquals("14", res.getId());
		}
	}

	@Test
	void b01_constructor_nullBuilder_throwsIllegalArgumentException() {
		var e = assertThrows(IllegalArgumentException.class, () -> new TestClient(null));
		assertEquals("Argument 'builder' cannot be null.", e.getMessage());
	}

	@Test
	void b02_builder_endpoint_null_throwsIllegalArgumentException() {
		var e = assertThrows(IllegalArgumentException.class, () -> TestClient.builder().endpoint(null));
		assertEquals("Argument 'endpoint' cannot be null.", e.getMessage());
	}

	@Test
	void b03_builder_noEndpoint_throwsIllegalArgumentException() {
		var e = assertThrows(IllegalArgumentException.class, () -> TestClient.builder().build());
		assertEquals("Argument 'endpoint' cannot be null.", e.getMessage());
	}

	@Test
	void b04_builder_endpoint_blank_throwsIllegalArgumentException() {
		var e = assertThrows(IllegalArgumentException.class, () -> TestClient.builder().endpoint(""));
		assertEquals("Argument 'endpoint' cannot be blank.", e.getMessage());
	}

	@Test
	void b05_send_nullRequest_throwsIllegalArgumentException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var e = assertThrows(IllegalArgumentException.class, () -> client.send(null));
			assertEquals("Argument 'request' cannot be null.", e.getMessage());
		}
	}

	@Test
	void b06_send_invalidJson_throwsIOException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("3").setMethod("ping");
			assertThrows(IOException.class, () -> client.send(req));
		}
	}

	@Test
	void b07_builder_interceptor_allowsFluentChaining() throws Exception {
		var called = new AtomicBoolean();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
			.build();
		var interceptor = new RestCallInterceptor() {
			@Override
			public void onInit(RestRequest req) {
				called.set(true);
			}
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).interceptor(interceptor).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping"));
			assertTrue(called.get());
		}
	}

	@Test
	void b08_builder_interceptor_null_throwsIllegalArgumentException() {
		var e = assertThrows(IllegalArgumentException.class, () -> TestClient.builder().interceptor(null));
		assertEquals("Argument 'interceptor' cannot be null.", e.getMessage());
	}

}
