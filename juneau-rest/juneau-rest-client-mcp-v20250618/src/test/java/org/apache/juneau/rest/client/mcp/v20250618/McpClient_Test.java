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
package org.apache.juneau.rest.client.mcp.v20250618;

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link McpClient}.
 */
@SuppressWarnings("resource") // mock transports/clients are in-memory no-op closeables; test bodies close what matters via try-with-resources.
class McpClient_Test extends TestBase {

	private static String bodyOf(TransportRequest req) {
		try {
			var out = new ByteArrayOutputStream();
			req.getBody().writeTo(out);
			return out.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static McpClient client(HttpTransport transport) {
		return McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
	}

	private static HttpTransport ok(String wireJson) {
		return tReq -> TransportResponse.builder()
			.statusCode(200)
			.header("Content-Type", "application/json")
			.body(new ByteArrayInputStream(wireJson.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	// ==========================================================================
	// a — initialize()
	// ==========================================================================

	@Test
	void a01_initialize_returnsTypedResult() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2025-06-18\",\"serverInfo\":{\"name\":\"srv\",\"version\":\"1.0.0\"}}}";
		try (var client = client(ok(wire))) {
			var result = client.initialize();
			assertEquals("2025-06-18", result.getProtocolVersion());
			assertEquals("srv", result.getServerInfo().getName());
			assertEquals("1.0.0", result.getServerInfo().getVersion());
		}
	}

	@Test
	void a02_initialize_sendsInitializeMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.initialize();
		}
		assertTrue(seenBody.get().contains("\"method\":\"initialize\""));
		assertTrue(seenBody.get().contains("\"protocolVersion\":\"2025-06-18\""));
	}

	@Test
	void a03_initialize_errorResponse_throwsMcpException() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32601,\"message\":\"Method not found: initialize\"}}";
		try (var client = client(ok(wire))) {
			var e = assertThrowsWithMessage(McpException.class, "Method not found: initialize", client::initialize);
			assertEquals(-32601, e.getCode());
		}
	}

	@Test
	void a04_initialize_nullResult_returnsNull() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}";
		try (var client = client(ok(wire))) {
			assertNull(client.initialize());
		}
	}

	// ==========================================================================
	// b — ping()
	// ==========================================================================

	@Test
	void b01_ping_succeedsWithoutThrowing() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}";
		try (var client = client(ok(wire))) {
			assertDoesNotThrow(client::ping);
		}
	}

	@Test
	void b02_ping_sendsPingMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.ping();
		}
		assertTrue(seenBody.get().contains("\"method\":\"ping\""));
	}

	// ==========================================================================
	// c — listTools()
	// ==========================================================================

	@Test
	void c01_listTools_returnsRegisteredTool() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tools\":[{\"name\":\"echo\",\"description\":\"Echoes back\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.listTools();
			assertEquals(1, result.getTools().size());
			assertEquals("echo", result.getTools().get(0).getName());
		}
	}

	@Test
	void c02_listTools_sendsToolsListMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.listTools();
		}
		assertTrue(seenBody.get().contains("\"method\":\"tools/list\""));
	}

	// ==========================================================================
	// d — callTool(...)
	// ==========================================================================

	@Test
	void d01_callTool_returnsTypedContent() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.callTool("echo", Map.of("text", "hello"));
			assertEquals(1, result.getContent().size());
			assertEquals("hello", ((TextContent) result.getContent().get(0)).getText());
		}
	}

	@Test
	void d02_callTool_sendsNameAndArguments() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.callTool("echo", Map.of("text", "hello"));
		}
		assertTrue(seenBody.get().contains("\"method\":\"tools/call\""));
		assertTrue(seenBody.get().contains("\"name\":\"echo\""));
		assertTrue(seenBody.get().contains("\"text\":\"hello\""));
	}

	@Test
	void d03_callTool_unknownTool_throwsMcpException() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32601,\"message\":\"Tool not found: no-such-tool\"}}";
		try (var client = client(ok(wire))) {
			var e = assertThrowsWithMessage(McpException.class, "Tool not found: no-such-tool", () -> client.callTool("no-such-tool", Map.of()));
			assertEquals(-32601, e.getCode());
		}
	}

	// ==========================================================================
	// e — listPrompts()
	// ==========================================================================

	@Test
	void e01_listPrompts_returnsRegisteredPrompt() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"prompts\":[{\"name\":\"greet\",\"description\":\"Greets\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.listPrompts();
			assertEquals(1, result.getPrompts().size());
			assertEquals("greet", result.getPrompts().get(0).getName());
		}
	}

	@Test
	void e02_listPrompts_sendsPromptsListMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.listPrompts();
		}
		assertTrue(seenBody.get().contains("\"method\":\"prompts/list\""));
	}

	// ==========================================================================
	// f — getPrompt(...)
	// ==========================================================================

	@Test
	void f01_getPrompt_returnsTypedMessages() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"description\":\"d\",\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"hi\"}}]}}";
		try (var client = client(ok(wire))) {
			var result = client.getPrompt("greet", Map.of("name", "Bob"));
			assertEquals("d", result.getDescription());
			assertEquals(1, result.getMessages().size());
			assertEquals(Role.USER, result.getMessages().get(0).getRole());
			assertEquals("hi", ((TextContent) result.getMessages().get(0).getContent()).getText());
		}
	}

	@Test
	void f02_getPrompt_sendsNameAndArguments() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.getPrompt("greet", Map.of("name", "Bob"));
		}
		assertTrue(seenBody.get().contains("\"method\":\"prompts/get\""));
		assertTrue(seenBody.get().contains("\"name\":\"greet\""));
		assertTrue(seenBody.get().contains("\"Bob\""));
	}

	// ==========================================================================
	// g — listResources()
	// ==========================================================================

	@Test
	void g01_listResources_returnsRegisteredResource() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"resources\":[{\"uri\":\"file:///a.txt\",\"name\":\"a.txt\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.listResources();
			assertEquals(1, result.getResources().size());
			assertEquals("file:///a.txt", result.getResources().get(0).getUri());
		}
	}

	@Test
	void g02_listResources_sendsResourcesListMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.listResources();
		}
		assertTrue(seenBody.get().contains("\"method\":\"resources/list\""));
	}

	// ==========================================================================
	// h — readResource(...)
	// ==========================================================================

	@Test
	void h01_readResource_returnsTypedContents() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"contents\":[{\"type\":\"resourceText\",\"uri\":\"file:///a.txt\",\"text\":\"hi\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.readResource("file:///a.txt");
			assertEquals(1, result.getContents().size());
			assertEquals("hi", ((TextResourceContents) result.getContents().get(0)).getText());
		}
	}

	@Test
	void h02_readResource_sendsUri() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.readResource("file:///a.txt");
		}
		assertTrue(seenBody.get().contains("\"method\":\"resources/read\""));
		assertTrue(seenBody.get().contains("\"uri\":\"file:///a.txt\""));
	}

	// ==========================================================================
	// i — listResourceTemplates()
	// ==========================================================================

	@Test
	void i01_listResourceTemplates_returnsRegisteredTemplate() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"resourceTemplates\":[{\"uriTemplate\":\"file:///{path}\",\"name\":\"files\"}]}}";
		try (var client = client(ok(wire))) {
			var result = client.listResourceTemplates();
			assertEquals(1, result.getResourceTemplates().size());
			assertEquals("file:///{path}", result.getResourceTemplates().get(0).getUriTemplate());
		}
	}

	@Test
	void i02_listResourceTemplates_sendsResourcesTemplatesListMethod() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.listResourceTemplates();
		}
		assertTrue(seenBody.get().contains("\"method\":\"resources/templates/list\""));
	}

	// ==========================================================================
	// j — complete(...)
	// ==========================================================================

	@Test
	void j01_complete_returnsTypedCompletion() throws Exception {
		var wire = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"completion\":{\"values\":[\"apple\",\"apricot\"],\"total\":2,\"hasMore\":false}}}";
		try (var client = client(ok(wire))) {
			var result = client.complete(new PromptReference().setName("greet"), new CompletionArgument().setName("fruit").setValue("ap"));
			assertEquals(List.of("apple", "apricot"), result.getCompletion().getValues());
			assertEquals(2, result.getCompletion().getTotal());
			assertEquals(false, result.getCompletion().getHasMore());
		}
	}

	@Test
	void j02_complete_sendsRefAndArgument() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			seenBody.set(bodyOf(tReq));
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var client = client(transport)) {
			client.complete(new PromptReference().setName("greet"), new CompletionArgument().setName("fruit").setValue("ap"));
		}
		assertTrue(seenBody.get().contains("\"method\":\"completion/complete\""));
		assertTrue(seenBody.get().contains("\"type\":\"ref/prompt\""));
		assertTrue(seenBody.get().contains("\"name\":\"greet\""));
		assertTrue(seenBody.get().contains("\"fruit\""));
	}
}
