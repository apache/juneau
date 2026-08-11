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

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link AbstractMcpClient#send(JsonRpcRequest)}.
 */
@SuppressWarnings("resource") // mock transports/clients are in-memory no-op closeables; test bodies close what matters via try-with-resources.
class AbstractMcpClient_Test extends TestBase {

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

		SseEventReader openStream(JsonRpcRequest req) throws IOException {
			return openEventStream(req);
		}

		SseEventReader openStream(JsonRpcRequest req, Map<String,String> headers) throws IOException {
			return openEventStream(req, headers);
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
		var successClosed = Flag.create();
		HttpTransport successTransport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(successClosed::set)
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(successTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping"));
		}
		assertTrue(successClosed.isSet());

		var errorClosed = Flag.create();
		HttpTransport errorTransport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"error\":{\"code\":-32000,\"message\":\"boom\"}}".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(errorClosed::set)
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(errorTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("2").setMethod("ping"));
		}
		assertTrue(errorClosed.isSet());

		var notificationClosed = Flag.create();
		HttpTransport notificationTransport = tReq -> TransportResponse.builder()
			.statusCode(202)
			.closeCallback(notificationClosed::set)
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(notificationTransport).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setMethod("notifications/x"));
		}
		assertTrue(notificationClosed.isSet());

		var parseFailureClosed = Flag.create();
		HttpTransport parseFailureTransport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)))
			.closeCallback(parseFailureClosed::set)
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(parseFailureTransport).build()) {
			assertThrows(IOException.class, () -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("3").setMethod("ping")));
		}
		assertTrue(parseFailureClosed.isSet());
	}

	@Test
	void a07_send_non2xxWithNonEnvelope_throwsStatusDiagnostic() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(500)
			.header("Content-Type", "text/plain")
			.body(new ByteArrayInputStream("server down".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrowsWithMessage(IOException.class, "MCP server returned HTTP 500 and the body was not a JSON-RPC envelope.",
				() -> client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("7").setMethod("ping")));
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
			assertThrowsWithMessage(IOException.class, "No response body received for JSON-RPC request id '9' (HTTP 200).", () -> client.send(req));
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
			assertThrowsWithMessage(IOException.class, "MCP server returned HTTP 199", () -> client.send(req));
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
	void c01_openEventStream_withRequestBody_postsBodyAndOpensStream() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				seenBody.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			return TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream("data: {\"ok\":true}\n\n".getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			try (var reader = client.openStream(req)) {
				assertTrue(reader.hasNext());
				assertEquals("{\"ok\":true}", reader.next().getData());
			}
			assertNotNull(seenBody.get());
			assertTrue(seenBody.get().contains("\"method\":\"subscriptions/listen\""));
		}
	}

	@Test
	void c02_openEventStream_nullRequest_throwsIllegalArgumentException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'request' cannot be null.", () -> client.openStream(null));
		}
	}

	@Test
	void c03_openEventStream_withHeaders_setsHeadersOnTransportRequest() throws Exception {
		var seenMethod = new AtomicReference<String>();
		var seenName = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var m = tReq.getFirstHeader("Mcp-Method");
			var n = tReq.getFirstHeader("Mcp-Name");
			seenMethod.set(m == null ? null : m.value());
			seenName.set(n == null ? null : n.value());
			return TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream("data: {\"ok\":true}\n\n".getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			try (var reader = client.openStream(req, Map.of("Mcp-Method", "subscriptions/listen", "Mcp-Name", ""))) {
				assertTrue(reader.hasNext());
			}
			assertEquals("subscriptions/listen", seenMethod.get());
			assertEquals("", seenName.get());
		}
	}

	@Test
	void c04_openEventStream_nullHeaders_behavesLikeNoHeadersOverload() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream("data: {\"ok\":true}\n\n".getBytes(StandardCharsets.UTF_8))).build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			try (var reader = client.openStream(req, null)) {
				assertTrue(reader.hasNext());
			}
		}
	}

	// I4 follow-up: a real server commonly qualifies its Content-Type with a charset parameter
	// (e.g. "text/event-stream;charset=utf-8"). isEventStream()'s substring-contains check must still
	// recognize this as SSE and open the stream, not fall through to the JSON-RPC-error/IOException paths
	// that a non-SSE Content-Type would trigger.
	@Test
	void c09_openEventStream_eventStreamContentTypeWithCharsetParam_stillOpensStream() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "text/event-stream;charset=utf-8")
			.body(new ByteArrayInputStream("data: {\"ok\":true}\n\n".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			try (var reader = client.openStream(req)) {
				assertTrue(reader.hasNext());
				assertEquals("{\"ok\":true}", reader.next().getData());
			}
		}
	}

	// I4: the opening subscriptions/listen POST can return HTTP 200 with a JSON-RPC error envelope instead
	// of an SSE stream (e.g. a capability/Accept-header/over-limit gate rejecting the request). This must
	// surface as a synchronously-thrown McpException carrying the server's code/message, not be handed to
	// the SSE reader (which would see zero events and eventually look like a generic transport failure).
	@Test
	void c06_openEventStream_jsonRpcErrorEnvelope_throwsMcpExceptionWithServerCodeAndMessage() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(
				"{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32600,\"message\":\"subscriptions/listen requires Accept: text/event-stream\"}}"
					.getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			var e = assertThrowsWithMessage(McpException.class, "subscriptions/listen requires Accept: text/event-stream", () -> client.openStream(req));
			assertEquals(-32600, e.getCode());
		}
	}

	// I4 follow-up: the same JSON-RPC-error-instead-of-SSE shape must also be caught when the response
	// arrives with a non-2xx HTTP status (some gates might reject at the HTTP layer instead of 200+error).
	@Test
	void c07_openEventStream_jsonRpcErrorEnvelope_non2xxStatus_stillThrowsMcpException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(429)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(
				"{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32000,\"message\":\"Too many concurrent subscriptions\"}}"
					.getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			var e = assertThrowsWithMessage(McpException.class, "Too many concurrent subscriptions", () -> client.openStream(req));
			assertEquals(-32000, e.getCode());
		}
	}

	// I4: a non-SSE response that is ALSO not a JSON-RPC error envelope (e.g. an unexpected plain-text 500
	// from an intermediary proxy) must still fail loudly as an IOException, not silently masquerade as SSE.
	@Test
	void c08_openEventStream_non2xxNonEnvelopeNonEventStream_throwsIOException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(502)
			.header("Content-Type", "text/plain")
			.body(new ByteArrayInputStream("bad gateway".getBytes(StandardCharsets.UTF_8)))
			.build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var req = new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("subscriptions/listen");
			// assertThrows(IOException.class, ...) itself proves this wasn't McpException (a RuntimeException,
			// not an IOException) - a wrong-type throw would fail this assertion with "unexpected exception type".
			assertThrows(IOException.class, () -> client.openStream(req));
		}
	}

	@Test
	void c05_openEventStream_headersOverload_nullRequest_throwsIllegalArgumentException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'request' cannot be null.", () -> client.openStream(null, Map.of()));
		}
	}

	@Test
	void b01_constructor_nullBuilder_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'builder' cannot be null.", () -> new TestClient(null));
	}

	@Test
	void b02_builder_endpoint_null_throwsIllegalArgumentException() {
		var builder = TestClient.builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'endpoint' cannot be null.", () -> builder.endpoint(null));
	}

	@Test
	void b03_builder_noEndpoint_throwsIllegalArgumentException() {
		var builder = TestClient.builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'endpoint' cannot be null.", builder::build);
	}

	@Test
	void b04_builder_endpoint_blank_throwsIllegalArgumentException() {
		var builder = TestClient.builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'endpoint' cannot be blank.", () -> builder.endpoint(""));
	}

	/** A non-http(s)-scheme endpoint is rejected at construction, not just non-blank. */
	@Test
	void b08_build_ftpSchemeEndpoint_throwsIllegalArgumentException() {
		var builder = TestClient.builder().endpoint("ftp://x/mcp");
		assertThrowsWithMessage(IllegalArgumentException.class, "http or https", builder::build);
	}

	/** A scheme-less (relative) endpoint is rejected at construction. */
	@Test
	void b09_build_schemelessEndpoint_throwsIllegalArgumentException() {
		var builder = TestClient.builder().endpoint("x/mcp");
		assertThrowsWithMessage(IllegalArgumentException.class, "http or https", builder::build);
	}

	/** A syntactically malformed endpoint is rejected at construction with a clear message. */
	@Test
	void b10_build_malformedEndpoint_throwsIllegalArgumentException() {
		var builder = TestClient.builder().endpoint("http://x y/mcp");
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid MCP endpoint URL", builder::build);
	}

	/** https is accepted alongside http. */
	@Test
	void b11_build_httpsSchemeEndpoint_isAccepted() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).build();
		try (var client = TestClient.builder().endpoint("https://x/mcp").transport(transport).build()) {
			assertNotNull(client);
		}
	}

	@Test
	void b05_send_nullRequest_throwsIllegalArgumentException() throws Exception {
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200).build();
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'request' cannot be null.", () -> client.send(null));
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
		var called = Flag.create();
		HttpTransport transport = tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
			.build();
		var interceptor = new RestCallInterceptor() {
			@Override
			public void onInit(RestRequest req) {
				called.set();
			}
		};
		try (var client = TestClient.builder().endpoint("http://x/mcp").transport(transport).interceptor(interceptor).build()) {
			client.send(new JsonRpcRequest().setJsonrpc("2.0").setId("1").setMethod("ping"));
			assertTrue(called.isSet());
		}
	}

	@Test
	void b08_builder_interceptor_null_throwsIllegalArgumentException() {
		var builder = TestClient.builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'interceptor' cannot be null.", () -> builder.interceptor(null));
	}

}
