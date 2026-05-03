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
package org.apache.juneau.bean.mcp;

import static org.apache.juneau.commons.utils.CollectionUtils.list;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@code juneau-bean-mcp} wire beans.
 */
class McpBeans_RoundTrip_Test {

	private static final JsonSerializer MCP_JSON =
		JsonSerializer.create()
			.addBeanTypes()
			.typePropertyName(Content.class, "type")
			.typePropertyName(ResourceContents.class, "type")
			.build();

	private static final JsonParser MCP_JSON_PARSER =
		JsonParser.create()
			.typePropertyName(Content.class, "type")
			.typePropertyName(ResourceContents.class, "type")
			.build();

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = MCP_JSON.serialize(bean);
		var copy = MCP_JSON_PARSER.parse(j1, type);
		var j2 = MCP_JSON.serialize(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	private static void assertJsonRoundTripPlain(Object bean, Class<?> type) {
		var ser = JsonSerializer.DEFAULT;
		var par = JsonParser.DEFAULT;
		var j1 = ser.serialize(bean);
		var copy = par.parse(j1, type);
		var j2 = ser.serialize(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Test
	void jsonRpc_stringId_roundTrip() {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId("abc")
			.setMethod(McpMethods.TOOLS_LIST)
			.setParams(null);
		assertJsonRoundTrip(req, JsonRpcRequest.class);
	}

	@Test
	void jsonRpc_intId_roundTrip() {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(42)
			.setMethod(McpMethods.PING)
			.setParams(list(JsonMap.of("x", 1)));
		assertJsonRoundTrip(req, JsonRpcRequest.class);
	}

	@Test
	void jsonRpc_errorResponse_roundTrip() {
		var res = new JsonRpcResponse()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(7)
			.setError(new JsonRpcError().setCode(-32600).setMessage("Invalid Request").setData(JsonMap.of("detail", "x")));
		assertJsonRoundTrip(res, JsonRpcResponse.class);
	}

	@Test
	void callToolResult_mixedContent_roundTrip() {
		var emb = new EmbeddedResourceContent()
			.setResource(new TextResourceContents().setUri("file:///x").setMimeType("text/plain").setText("hi"));
		var result = new CallToolResult()
			.setIsError(true)
			.setContent(list(
				new TextContent().setText("err"),
				new ImageContent().setData("AAA=").setMimeType("image/png"),
				emb
			));
		assertJsonRoundTrip(result, CallToolResult.class);
	}

	@Test
	void readResourceResult_mixedContents_roundTrip() {
		var rr = new ReadResourceResult()
			.setContents(list(
				new TextResourceContents().setUri("u1").setText("alpha"),
				new BlobResourceContents().setUri("u2").setMimeType("application/octet-stream").setBlob("Qk09")
			));
		assertJsonRoundTrip(rr, ReadResourceResult.class);
	}

	@Test
	void initializeResult_roundTrip() {
		var init = new InitializeResult()
			.setProtocolVersion(McpProtocol.VERSION_2025_06_18)
			.setInstructions("Be concise.")
			.setServerInfo(new Implementation().setName("srv").setVersion("1.0.0"))
			.setCapabilities(
				new ServerCapabilities()
					.setTools(new ToolCapability().setListChanged(true))
					.setPrompts(new PromptCapability().setListChanged(false))
					.setResources(new ResourceCapability().setSubscribe(true))
					.setLogging(new LoggingCapability().setLevel("info"))
			);
		assertJsonRoundTrip(init, InitializeResult.class);
	}

	@Test
	void getPromptResult_withRole_roundTrip() {
		var pr = new GetPromptResult()
			.setDescription("d")
			.setMessages(list(
				new PromptMessage().setRole(Role.USER).setContent(new TextContent().setText("hello")),
				new PromptMessage().setRole(Role.ASSISTANT).setContent(new TextContent().setText("world"))
			));
		assertJsonRoundTrip(pr, GetPromptResult.class);
	}

	@Test
	void jsonSchema_defs_roundTrip() {
		var nested = new JsonSchema().setType("string");
		var props = new LinkedHashMap<String, JsonSchema>();
		props.put("id", nested);
		var defs = new LinkedHashMap<String, JsonSchema>();
		defs.put("IdString", nested);
		var root = new JsonSchema()
			.setType("object")
			.setRequired(list("id"))
			.setProperties(props)
			.setDefs(defs)
			.setItems(new JsonSchema().setType("string"))
			.setAdditionalProperties(false);
		assertJsonRoundTrip(root, JsonSchema.class);
	}

	@Test
	void jsonRpc_successResponse_roundTrip() {
		var tools = new ListToolsResult()
			.setNextCursor("c1")
			.setTools(list(
				new Tool()
					.setName("t1")
					.setDescription("d1")
					.setInputSchema(new JsonSchema().setType("object").setAdditionalProperties(true))
			));
		var res = new JsonRpcResponse()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId("r1")
			.setResult(tools);
		assertJsonRoundTripPlain(res, JsonRpcResponse.class);
	}

	@Test
	void initializeRequest_roundTrip() {
		var roots = new RootsCapability().setListChanged(true);
		var caps = new ClientCapabilities()
			.setRoots(roots)
			.setSampling(JsonMap.of("supported", true))
			.setExperimental(JsonMap.of("x", 1));
		var req = new InitializeRequest()
			.setProtocolVersion(McpProtocol.VERSION_2025_06_18)
			.setClientInfo(new Implementation().setName("cli").setVersion("0.0.1"))
			.setCapabilities(caps);
		assertJsonRoundTripPlain(req, InitializeRequest.class);
	}

	@Test
	void listPromptsResult_roundTrip() {
		var arg = new PromptArgument().setName("a1").setDescription("ad").setRequired(true);
		var prompt = new Prompt().setName("p1").setDescription("pd").setArguments(list(arg));
		var lr = new ListPromptsResult().setPrompts(list(prompt)).setNextCursor("n");
		assertJsonRoundTripPlain(lr, ListPromptsResult.class);
	}

	@Test
	void listResourcesResult_roundTrip() {
		var r = new Resource()
			.setUri("file:///r")
			.setName("n")
			.setTitle("t")
			.setDescription("desc")
			.setMimeType("text/plain")
			.setSize(99L);
		var lr = new ListResourcesResult().setResources(list(r)).setNextCursor("z");
		assertJsonRoundTripPlain(lr, ListResourcesResult.class);
	}

	@Test
	void callToolRequest_getPromptRequest_readResourceRequest_roundTrip() {
		assertJsonRoundTripPlain(
			new CallToolRequest().setName("tool").setArguments(JsonMap.of("q", "v")),
			CallToolRequest.class
		);
		assertJsonRoundTripPlain(
			new GetPromptRequest().setName("prompt1").setArguments(JsonMap.of("k", "v")),
			GetPromptRequest.class
		);
		assertJsonRoundTripPlain(new ReadResourceRequest().setUri("file:///doc"), ReadResourceRequest.class);
	}

	@Test
	void initializeResult_experimentalAndResourceListChanged_roundTrip() {
		var caps = new ServerCapabilities()
			.setTools(new ToolCapability().setListChanged(true))
			.setResources(new ResourceCapability().setListChanged(true).setSubscribe(false))
			.setExperimental(JsonMap.of("flag", 1));
		var init = new InitializeResult()
			.setProtocolVersion(McpProtocol.VERSION_2025_06_18)
			.setServerInfo(new Implementation().setName("s").setVersion("1"))
			.setCapabilities(caps);
		assertJsonRoundTrip(init, InitializeResult.class);
	}

	@Test
	void role_toWire_allValues() {
		for (var r : Role.values()) {
			assertEquals(r.toString(), r.toWire());
		}
	}

	@Test
	void mcpException_toJsonRpcError() {
		var ex = new McpException(-32000, "Tool failed", JsonMap.of("tool", "t1"));
		var err = ex.toJsonRpcError();
		assertEquals(-32000, err.getCode());
		assertEquals("Tool failed", err.getMessage());
		assertNotNull(err.getData());
	}

	@Test
	void mcpException_twoArgConstructorAndSetters() {
		var ex = new McpException(1, "m");
		assertNull(ex.getData());
		ex.setCode(2).setData(JsonMap.of("a", 1));
		assertEquals(2, ex.getCode());
		assertNotNull(ex.getData());
		var err = ex.toJsonRpcError();
		assertEquals(2, err.getCode());
		assertEquals("m", err.getMessage());
	}

	@Test
	void getPromptResult_systemAndToolRoles_roundTrip() {
		var pr = new GetPromptResult()
			.setMessages(list(
				new PromptMessage().setRole(Role.SYSTEM).setContent(new TextContent().setText("sys")),
				new PromptMessage().setRole(Role.TOOL).setContent(new TextContent().setText("tool"))
			));
		assertJsonRoundTrip(pr, GetPromptResult.class);
	}
}
