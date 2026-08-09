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

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.mcp.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Mock HttpTransport lambdas and the ok(...)/client(...) test-helper factories (@Owning; callers close via try-with-resources) are short-lived test fixtures.
})
class McpClient_Methods_Test {

	private static HttpTransport ok(String wireJson) {
		return tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(wireJson.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	private static McpClient client(HttpTransport transport) {
		return McpClient.builder()
			.endpoint("http://x/mcp")
			.transport(transport)
			.build();
	}

	@Test
	void a01_ping_returnsTypedResult() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"resultType\":\"complete\"}}";
		try (var c = client(ok(wire))) {
			var result = c.ping();
			assertNotNull(result);
			assertEquals("complete", result.getResultType());
		}
	}

	@Test
	void b01_listTools_returnsTypedTool() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"echo\",\"description\":\"Echoes\"}]}}";
		try (var c = client(ok(wire))) {
			var result = c.listTools();
			assertEquals(1, result.getTools().size());
			assertEquals("echo", result.getTools().get(0).getName());
		}
	}

	@Test
	void c01_callTool_parsesPolymorphicTextContent() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}}";
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(ok(wire)).build()) {
			var result = c.callTool("echo", Map.of("text", "hello"));
			assertEquals(1, result.getContent().size());
			assertEquals("hello", ((TextContent)result.getContent().get(0)).getText());
		}
	}

	@Test
	void c02_callToolText_returnsFirstTextContentText() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}}";
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(ok(wire)).build()) {
			assertEquals("hello", c.callToolText("echo", Map.of("text", "hello")));
		}
	}

	@Test
	void c03_callToolText_nonTextFirstBlock_returnsNull() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"audio\",\"data\":\"QUJD\",\"mimeType\":\"audio/wav\"}]}}";
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(ok(wire)).build()) {
			assertNull(c.callToolText("echo", Map.of()));
		}
	}

	@Test
	void c04_callToolText_nullResult_returnsNullNotNpe() throws Exception {
		// The server returning "result":null is a documented, legal callTool() outcome; callToolText must
		// not NPE dereferencing it.
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":null}";
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(ok(wire)).build()) {
			assertNull(c.callToolText("echo", Map.of()));
		}
	}

	@Test
	void d01_callTool_traceEchoEnabled_readsMetaPath() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"_meta\":{\"traceparent\":\"00-parent-01\",\"tracestate\":\"v=x\",\"baggage\":\"u=42\"},\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}}";
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(ok(wire)).build()) {
			var result = c.callTool("echo", Map.of());
			assertNotNull(result.getMeta());
			assertEquals("00-parent-01", result.getMeta().getTraceparent());
		}
	}

	@Test
	void e01_listPrompts_returnsTypedPrompt() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"prompts\":[{\"name\":\"greet\",\"description\":\"Greets\"}]}}";
		try (var c = client(ok(wire))) {
			var result = c.listPrompts();
			assertEquals(1, result.getPrompts().size());
			assertEquals("greet", result.getPrompts().get(0).getName());
		}
	}

	@Test
	void f01_getPrompt_returnsTypedMessage() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"description\":\"d\",\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"hi\"}}]}}";
		try (var c = client(ok(wire))) {
			var result = c.getPrompt("greet", Map.of("name", "Bob"));
			assertEquals("d", result.getDescription());
			assertEquals(Role.USER, result.getMessages().get(0).getRole());
			assertEquals("hi", ((TextContent)result.getMessages().get(0).getContent()).getText());
		}
	}

	@Test
	void g01_listResources_returnsTypedResource() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"resources\":[{\"uri\":\"file:///a.txt\",\"name\":\"a.txt\"}]}}";
		try (var c = client(ok(wire))) {
			var result = c.listResources();
			assertEquals(1, result.getResources().size());
			assertEquals("file:///a.txt", result.getResources().get(0).getUri());
		}
	}

	@Test
	void h01_listResourceTemplates_returnsTypedTemplate() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"resourceTemplates\":[{\"uriTemplate\":\"file:///{path}\",\"name\":\"files\"}]}}";
		try (var c = client(ok(wire))) {
			var result = c.listResourceTemplates();
			assertEquals(1, result.getResourceTemplates().size());
			assertEquals("file:///{path}", result.getResourceTemplates().get(0).getUriTemplate());
		}
	}

	@Test
	void i01_cache_readWrite_secondCallServedFromCache() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"ttlMs\":60000,\"cacheScope\":\"public\",\"tools\":[{\"name\":\"echo\"}]}}";
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
			var a = c.listTools();
			var b = c.listTools();
			assertEquals(1, calls.get());
			assertSame(a, b);
		}
	}
}
