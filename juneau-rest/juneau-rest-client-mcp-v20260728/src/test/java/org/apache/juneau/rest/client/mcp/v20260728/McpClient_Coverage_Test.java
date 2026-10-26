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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.mcp.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.sun.net.httpserver.*;

/**
 * Coverage for the remaining {@link McpClient} paths: null/non-null argument handling, completion context,
 * trace-context stamping variants, cache write rules, subscription pump edge cases, and duplex-pump early exits.
 */
@SuppressWarnings({
	"resource" // Mock HttpTransport lambdas and the client(...) test-helper factory are short-lived test fixtures; clients are closed via try-with-resources where lifecycle matters.
})
class McpClient_Coverage_Test extends TestBase {

	private static final String EMPTY_RESULT = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}";

	private static HttpTransport recording(AtomicReference<String> seenBody, String wire) {
		return tReq -> {
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
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
	}

	private static HttpTransport sse(String inbound) {
		return tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	private static McpClient client(HttpTransport transport) {
		return McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
	}

	private static String notification(String method) {
		return "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\"}\n\n";
	}

	private static final String COMPLETE_FRAME = "data: {\"jsonrpc\":\"2.0\",\"id\":\"s\",\"result\":{\"resultType\":\"complete\"}}\n\n";

	//-----------------------------------------------------------------------------------------------------------------
	// a - connect(String) / null-argument handling / completion context
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_connect_withEndpointString_performsDiscoverAgainstRealHttpServer() throws Exception {
		var method = new AtomicReference<String>();
		var server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/mcp", ex -> {
			method.set(ex.getRequestHeaders().getFirst("Mcp-Method"));
			var body = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"supportedVersions\":[\"2026-07-28\"],\"capabilities\":{}}}".getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
		try (var c = McpClient.connect("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp")) {
			assertEquals("2026-07-28", c.discoveredServer().getSupportedVersions().get(0));
		} finally {
			server.stop(0);
		}
		assertEquals(McpMethods.SERVER_DISCOVER, method.get());
	}

	@Test
	void a02_callTool_nullArguments_sendsEmptyObject() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = client(recording(seen, EMPTY_RESULT))) {
			c.callTool("echo", null);
		}
		assertContains("\"arguments\":{}", seen.get());
	}

	@Test
	void a03_getPrompt_nullArguments_sendsEmptyObject() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = client(recording(seen, EMPTY_RESULT))) {
			c.getPrompt("p", null);
		}
		assertContains("\"arguments\":{}", seen.get());
	}

	@Test
	void a04_getPromptWithElicitation_nonNullArguments_sendsArguments() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = client(recording(seen, EMPTY_RESULT))) {
			c.getPromptWithElicitation("p", Map.of("k", "v1"), requests -> Map.of(), 2);
		}
		assertContains("\"k\":\"v1\"", seen.get());
	}

	@Test
	void a05_complete_withContextArguments_sendsContext() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = client(recording(seen, EMPTY_RESULT))) {
			c.complete(new PromptReference().setName("p"), "argName", "argVal", Map.of("ctxKey", "ctxVal"));
		}
		assertContains("\"context\"", seen.get());
		assertContains("\"ctxKey\":\"ctxVal\"", seen.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - trace-context / client-info stamping
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_clientInfo_isStampedIntoMeta() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = McpClient.builder().endpoint("http://x/mcp")
			.transport(recording(seen, EMPTY_RESULT))
			.clientInfo(new Implementation().setName("my-client").setVersion("9.8.7"))
			.build()) {
			c.listTools();
		}
		assertContains("\"name\":\"my-client\"", seen.get());
		assertContains("\"version\":\"9.8.7\"", seen.get());
	}

	@Test
	void b02_traceFieldsSupplier_returningNull_stampsNoTraceFields() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = McpClient.builder().endpoint("http://x/mcp")
			.transport(recording(seen, EMPTY_RESULT))
			.traceFieldsSupplier(() -> null)
			.build()) {
			c.listTools();
		}
		assertFalse(seen.get().contains("traceparent"), seen.get());
		assertFalse(seen.get().contains("tracestate"), seen.get());
		assertFalse(seen.get().contains("baggage"), seen.get());
	}

	@Test
	void b03_traceFieldsSupplier_onlyTracestate_stampsOnlyThatField() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = McpClient.builder().endpoint("http://x/mcp")
			.transport(recording(seen, EMPTY_RESULT))
			.traceFieldsSupplier(() -> Map.of(RequestMeta.KEY_TRACESTATE, "only=state"))
			.build()) {
			c.listTools();
		}
		assertContains("\"tracestate\":\"only=state\"", seen.get());
		assertFalse(seen.get().contains("traceparent"), seen.get());
		assertFalse(seen.get().contains("baggage"), seen.get());
	}

	@Test
	void b04_traceFieldsSupplier_onlyBaggage_stampsOnlyThatField() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = McpClient.builder().endpoint("http://x/mcp")
			.transport(recording(seen, EMPTY_RESULT))
			.traceFieldsSupplier(() -> Map.of(RequestMeta.KEY_BAGGAGE, "only=baggage"))
			.build()) {
			c.listTools();
		}
		assertContains("\"baggage\":\"only=baggage\"", seen.get());
		assertFalse(seen.get().contains("traceparent"), seen.get());
		assertFalse(seen.get().contains("tracestate"), seen.get());
	}

	@Test
	void b05_traceFieldsSupplier_onlyTraceparent_stampsOnlyThatField() throws Exception {
		var seen = new AtomicReference<String>();
		try (var c = McpClient.builder().endpoint("http://x/mcp")
			.transport(recording(seen, EMPTY_RESULT))
			.traceFieldsSupplier(() -> Map.of(RequestMeta.KEY_TRACEPARENT, "00-only-01"))
			.build()) {
			c.listTools();
		}
		assertContains("\"traceparent\":\"00-only-01\"", seen.get());
		assertFalse(seen.get().contains("tracestate"), seen.get());
		assertFalse(seen.get().contains("baggage"), seen.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - cache write rules
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_cacheableResultWithoutTtl_isNotCached() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"echo\"}]}}";
			return TransportResponse.builder().statusCode(200).header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).responseCache(new InMemoryMcpResponseCache()).build()) {
			c.listTools();
			c.listTools();
		}
		assertEquals(2, calls.get());
	}

	@Test
	void c02_publicScopeResult_isServedFromCacheOnSecondCall() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"ttlMs\":60000,\"cacheScope\":\"public\",\"tools\":[{\"name\":\"echo\"}]}}";
			return TransportResponse.builder().statusCode(200).header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).responseCache(new InMemoryMcpResponseCache()).build()) {
			var a = c.listTools();
			var b = c.listTools();
			assertSame(a, b);
		}
		assertEquals(1, calls.get());
	}

	@Test
	void c03_nonCacheableResult_withCacheConfigured_isNotCached() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}}";
			return TransportResponse.builder().statusCode(200).header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).responseCache(new InMemoryMcpResponseCache()).build()) {
			c.callTool("echo", null);
			c.callTool("echo", null);
		}
		assertEquals(2, calls.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - subscription pump edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_listChangedNotifications_andUnknownMethod_andNonDataEvents() throws Exception {
		var inbound =
			notification(McpMethods.NOTIFICATIONS_RESOURCES_LIST_CHANGED)
			+ notification(McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED)
			+ notification(McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED)
			+ notification("notifications/some/futureKind")
			+ "event: other\n\n"
			+ "data:\n\n"
			+ COMPLETE_FRAME;
		var events = new CopyOnWriteArrayList<String>();
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onListChanged(McpListChangedKind kind) { events.add(kind.name()); }
			@Override public void onComplete() { done.countDown(); }
			@Override public void onError(Throwable t) { events.add("error:" + t); done.countDown(); }
		};
		try (var c = client(sse(inbound))) {
			c.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS), "listener never completed: " + events);
		}
		assertEquals(List.of("RESOURCES", "PROMPTS", "TOOLS"), events);
	}

	@Test
	void d02_cancelFromWithinCallback_stopsPumpBeforeReadingNextFrame() throws Exception {
		var inbound =
			notification(McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED)
			+ notification(McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED)
			+ COMPLETE_FRAME;
		var handleRef = new AtomicReference<McpSubscriptionHandle>();
		var handleSet = new CountDownLatch(1);
		var events = new CopyOnWriteArrayList<String>();
		var listener = new McpSubscriptionListener() {
			@Override public void onListChanged(McpListChangedKind kind) {
				events.add(kind.name());
				try {
					assertTrue(handleSet.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				handleRef.get().cancel();
			}
			@Override public void onComplete() { events.add("complete"); }
			@Override public void onError(Throwable t) { events.add("error:" + t); }
		};
		try (var c = client(sse(inbound))) {
			var handle = c.listen(new SubscriptionFilter(), listener);
			handleRef.set(handle);
			handleSet.countDown();
			((McpClient.SubscriptionPump)handle).pumpThread.join(5000);
			assertFalse(((McpClient.SubscriptionPump)handle).pumpThread.isAlive());
			assertFalse(handle.isOpen());
		}
		assertEquals(List.of("TOOLS"), events);
	}

	@Test
	void d03_readerCloseThrowsIOException_isSwallowed() throws Exception {
		var closeCalled = new AtomicBoolean();
		var data = COMPLETE_FRAME.getBytes(StandardCharsets.UTF_8);
		var stream = new ByteArrayInputStream(data) {
			@Override public void close() throws IOException {
				closeCalled.set(true);
				throw new IOException("close failed");
			}
		};
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream").body(stream).build();
		var done = new CountDownLatch(1);
		var errors = new CopyOnWriteArrayList<Throwable>();
		var listener = new McpSubscriptionListener() {
			@Override public void onComplete() { done.countDown(); }
			@Override public void onError(Throwable t) { errors.add(t); done.countDown(); }
		};
		try (var c = client(transport)) {
			var handle = (McpClient.SubscriptionPump)c.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS));
			handle.pumpThread.join(5000);
			assertFalse(handle.pumpThread.isAlive());
		}
		assertTrue(closeCalled.get());
		assertTrue(errors.isEmpty(), errors.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - duplex pump early exits
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{index}] stream={0}")
	@ValueSource(strings = {
		"", // empty stream
		"event: other\n\n", // event without data
		"data:\n\n" // empty data event
	})
	void e01_pumpNextServerMessage_noDispatchableMessage_returnsWithoutDispatch(String body) throws Exception {
		var dispatched = new AtomicBoolean();
		try (var c = client(sse(body))) {
			c.setServerRequestHandler((request, ctx) -> { dispatched.set(true); return JsonMap.of(); });
			c.pumpNextServerMessage();
		}
		assertFalse(dispatched.get());
	}

	@Test
	void e04_pumpNextServerMessage_notification_dispatchesButPostsNoResult() throws Exception {
		var requestCount = new AtomicInteger();
		var dispatchedMethod = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			requestCount.incrementAndGet();
			return TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream("data: {\"jsonrpc\":\"2.0\",\"method\":\"sampling/createMessage\",\"params\":{}}\n\n".getBytes(StandardCharsets.UTF_8))).build();
		};
		try (var c = client(transport)) {
			c.setServerRequestHandler((request, ctx) -> { dispatchedMethod.set(request.getMethod()); return JsonMap.of(); });
			c.pumpNextServerMessage();
		}
		assertEquals("sampling/createMessage", dispatchedMethod.get());
		assertEquals(1, requestCount.get(), "a notification must not trigger a result post");
	}
}
