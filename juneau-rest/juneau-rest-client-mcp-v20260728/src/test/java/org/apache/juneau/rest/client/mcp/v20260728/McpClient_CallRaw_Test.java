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
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpClient#callRaw(String, RequestParams)}: the result-type-agnostic dispatch a
 * Multi-Round-Trip-Request (SEP-2322) caller needs to receive an {@code input_required} response and echo its
 * {@code requestState}/{@code inputResponses} on a resume call (plan Phase 5, Task 14).
 */
class McpClient_CallRaw_Test {

	private static HttpTransport ok(String wireJson) {
		return tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(wireJson.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	private static McpClient client(HttpTransport transport) {
		return McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
	}

	@Test
	void a01_callRaw_completeResult_returnsRawMapMatchingTypedCall() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}],\"resultType\":\"complete\"}}";
		try (var c = client(ok(wire))) {
			var raw = c.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("echo").setArguments(Map.of("text", "hello")));
			assertEquals("complete", raw.get("resultType"));
			assertNotNull(raw.get("content"));
		}
		// The same wire, consumed through the typed method, yields a CallToolResult with matching content --
		// proving callRaw returns the identical underlying JSON, only undiscriminated.
		try (var c = client(ok(wire))) {
			var typed = c.callTool("echo", Map.of("text", "hello"));
			assertEquals("hello", ((TextContent) typed.getContent().get(0)).getText());
		}
	}

	@Test
	void a02_callRaw_inputRequiredResult_exposesRequestStateAndInputRequests() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"inputRequests\":{\"q1\":{\"type\":\"elicitation\"}},\"requestState\":\"tok-abc\",\"resultType\":\"input_required\"}}";
		try (var c = client(ok(wire))) {
			var raw = c.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask"));
			assertEquals("input_required", raw.get("resultType"));
			assertEquals("tok-abc", raw.get("requestState"));
			assertInstanceOf(Map.class, raw.get("inputRequests"));
			assertTrue(((Map<?,?>) raw.get("inputRequests")).containsKey("q1"));
		}
	}

	@Test
	void a03_callRaw_jsonRpcError_throwsMcpExceptionLikeEveryTypedMethod() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32602,\"message\":\"bad params\"}}";
		try (var c = client(ok(wire))) {
			var e = assertThrows(McpException.class,
				() -> c.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask").setRequestState("stale")));
			assertEquals(-32602, e.getCode());
		}
	}

	/**
	 * Reproduces the mixed-API cache-key collision: a typed call caches a non-{@link Map} result bean under a key
	 * that {@code callRaw} would compute identically for the same method+params. {@code CallToolResult} (the
	 * {@code tools/call} result type) is never actually written to the cache because it doesn't implement
	 * {@link CacheableResult}, so {@code resources/read} (whose
	 * {@link ReadResourceResult} does implement it) is the type that is genuinely reachable end-to-end: prime the
	 * cache via the typed {@link McpClient#readResource} call, then issue {@link McpClient#callRaw} for the same
	 * {@code resources/read} URI. Before the fix, {@code callRaw}'s cache READ would return the cached
	 * {@code ReadResourceResult} and {@code Map.class.cast(...)} it, throwing {@link ClassCastException}.
	 */
	@Test
	void a04_callRaw_doesNotReadTypedResultFromSharedCacheKey() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			var n = calls.incrementAndGet();
			var wire = n == 1
				? "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"contents\":[{\"type\":\"resourceText\",\"uri\":\"file:///a\",\"text\":\"cached-body\"}],\"ttlMs\":60000}}"
				: "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"result\":{\"contents\":[{\"type\":\"resourceText\",\"uri\":\"file:///a\",\"text\":\"wire-body\"}]}}";
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder()
			.endpoint("http://x/mcp")
			.transport(transport)
			.responseCache(new InMemoryMcpResponseCache())
			.build()) {
			// Primes the cache with a typed ReadResourceResult under the resources/read + uri cache key.
			c.readResource("file:///a");
			assertEquals(1, calls.get());

			// callRaw on the same method+params must not retrieve the cached ReadResourceResult and must not throw
			// ClassCastException; it goes to the wire instead, proven by the second (distinct) wire body winning.
			var raw = assertDoesNotThrow(() -> c.callRaw(McpMethods.RESOURCES_READ, new ReadResourceRequest().setUri("file:///a")));
			assertEquals(2, calls.get());
			var contents = (List<?>) raw.get("contents");
			var content = (Map<?,?>) contents.get(0);
			assertEquals("wire-body", content.get("text"));
		}
	}
}
