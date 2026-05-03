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
package org.apache.juneau.rest.mcp;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpDispatcher}.
 */
class McpDispatcher_Test {

	private final McpDispatcher dispatcher = new McpDispatcher();
	private final BasicBeanStore ctx = BasicBeanStore.create().build();

	private static McpToolHandler tool(String name, java.util.function.Function<Map<String, Object>, CallToolResult> fn) {
		return new McpToolHandler() {
			@Override
			public Tool descriptor() {
				return new Tool().setName(name).setDescription("desc:" + name);
			}

			@Override
			public CallToolResult call(Map<String, Object> arguments, BasicBeanStore ctx) {
				return fn.apply(arguments);
			}
		};
	}

	private static McpPromptHandler prompt(String name, java.util.function.Function<Map<String, Object>, GetPromptResult> fn) {
		return new McpPromptHandler() {
			@Override
			public Prompt descriptor() {
				return new Prompt().setName(name);
			}

			@Override
			public GetPromptResult get(Map<String, Object> arguments, BasicBeanStore ctx) {
				return fn.apply(arguments);
			}
		};
	}

	private static McpResourceHandler resource(String uri, java.util.function.Function<String, ReadResourceResult> fn) {
		return new McpResourceHandler() {
			@Override
			public Resource descriptor() {
				return new Resource().setUri(uri).setName("res");
			}

			@Override
			public ReadResourceResult read(String u, BasicBeanStore ctx) {
				return fn.apply(u);
			}
		};
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params);
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r) {
		return dispatcher.dispatch(r, config, ctx);
	}

	@Test
	void initialize_default_capabilities() {
		var config = new McpServerConfig()
			.addTool(tool("a", a -> new CallToolResult()))
			.addPrompt(prompt("p", a -> new GetPromptResult()))
			.addResource(resource("r://x", u -> new ReadResourceResult()));

		var resp = send(config, req(1, McpMethods.INITIALIZE, null));
		assertNotNull(resp);
		var result = (InitializeResult) resp.getResult();
		assertString(McpProtocol.VERSION_2025_06_18, result.getProtocolVersion());
		assertNotNull(result.getCapabilities().getTools());
		assertNotNull(result.getCapabilities().getPrompts());
		assertNotNull(result.getCapabilities().getResources());
		assertString(McpDispatcher.DEFAULT_SERVER_NAME, result.getServerInfo().getName());
	}

	@Test
	void initialize_explicit_capabilitiesAndServerInfo() {
		var caps = new ServerCapabilities().setLogging(new LoggingCapability());
		var info = new Implementation().setName("custom").setVersion("9.9");
		var config = new McpServerConfig().setCapabilities(caps).setServerInfo(info).setInstructions("hi");
		var resp = send(config, req(1, McpMethods.INITIALIZE, null));
		var result = (InitializeResult) resp.getResult();
		assertSame(caps, result.getCapabilities());
		assertSame(info, result.getServerInfo());
		assertString("hi", result.getInstructions());
	}

	@Test
	void ping_returns_emptyResult() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PING, null));
		assertTrue(resp.getResult() instanceof JsonMap);
	}

	@Test
	void method_notFound() {
		var resp = send(new McpServerConfig(), req(1, "no/such/method", null));
		assertEquals(McpDispatcher.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void missing_method() {
		var resp = send(new McpServerConfig(), req(1, null, null));
		assertEquals(McpDispatcher.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void empty_method_string() {
		var resp = send(new McpServerConfig(), req(1, "", null));
		assertEquals(McpDispatcher.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void initialize_emptyConfig_synthesizesEmptyCapabilities() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.INITIALIZE, null));
		var result = (InitializeResult) resp.getResult();
		assertNotNull(result.getCapabilities());
		assertNull(result.getCapabilities().getTools());
		assertNull(result.getCapabilities().getPrompts());
		assertNull(result.getCapabilities().getResources());
	}

	@Test
	void notification_runtimeException_returnsNullSilently() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new RuntimeException("boom");
		}));
		assertNull(send(config, req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "e"))));
	}

	@Test
	void notification_mcpException_returnsNullSilently() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new McpException(-32000, "no");
		}));
		assertNull(send(config, req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "e"))));
	}

	@Test
	void notification_returnsNullResponse() {
		var config = new McpServerConfig().addTool(tool("a", args -> new CallToolResult()));
		var notif = req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "a"));
		assertNull(send(config, notif));
	}

	@Test
	void notification_methodNotFound_stillReturnsNull() {
		assertNull(send(new McpServerConfig(), req(null, "missing", null)));
	}

	@Test
	void notification_invalidMethod_returnsNull() {
		assertNull(send(new McpServerConfig(), req(null, null, null)));
	}

	@Test
	void nullEnvelope_returnsInvalidRequest() {
		var resp = dispatcher.dispatch(null, new McpServerConfig(), ctx);
		assertEquals(McpDispatcher.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	// -------- tools/list ---------

	@Test
	void tools_list_singlePage() {
		var config = new McpServerConfig()
			.addTool(tool("a", args -> new CallToolResult()))
			.addTool(tool("b", args -> new CallToolResult()));
		var resp = send(config, req(1, McpMethods.TOOLS_LIST, null));
		var result = (ListToolsResult) resp.getResult();
		assertSize(2, result.getTools());
		assertNull(result.getNextCursor());
	}

	@Test
	void tools_list_paged() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1))
			.addTool(tool("a", args -> new CallToolResult()))
			.addTool(tool("b", args -> new CallToolResult()));
		var first = (ListToolsResult) send(config, req(1, McpMethods.TOOLS_LIST, null)).getResult();
		assertSize(1, first.getTools());
		assertString("1", first.getNextCursor());
		var second = (ListToolsResult) send(config, req(2, McpMethods.TOOLS_LIST, JsonMap.of("cursor", "1"))).getResult();
		assertSize(1, second.getTools());
		assertNull(second.getNextCursor());
	}

	// -------- tools/call ---------

	@Test
	void tools_call_routes_byName() {
		var config = new McpServerConfig().addTool(tool("echo", args -> {
			var ctr = new CallToolResult();
			ctr.setContent(List.of(new TextContent().setText(String.valueOf(args.get("text")))));
			return ctr;
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"))));
		var ctr = (CallToolResult) resp.getResult();
		assertString("hi", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void tools_call_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.TOOLS_CALL, JsonMap.of("arguments", JsonMap.of())));
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void tools_call_unknownTool_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "missing")));
		assertEquals(McpDispatcher.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void tools_call_argumentsNotObject_throwsInvalidParams() {
		var config = new McpServerConfig().addTool(tool("e", a -> new CallToolResult()));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e", "arguments", "string-not-map")));
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void tools_call_paramsNotMap_invalidParams() {
		var config = new McpServerConfig().addTool(tool("e", a -> new CallToolResult()));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, "not-a-map"));
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void handler_throwingMcpException_propagatesCodeAndData() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new McpException(-32099, "nope", JsonMap.of("k", "v"));
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(-32099, resp.getError().getCode());
		assertString("nope", resp.getError().getMessage());
		assertNotNull(resp.getError().getData());
	}

	@Test
	void handler_throwingRuntimeException_internalError() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new RuntimeException("boom");
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(McpDispatcher.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertString("boom", resp.getError().getMessage());
	}

	@Test
	void handler_throwingRuntimeExceptionWithoutMessage_usesClassName() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new IllegalStateException();
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(McpDispatcher.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertString("IllegalStateException", resp.getError().getMessage());
	}

	// -------- prompts ---------

	@Test
	void prompts_list_and_get() {
		var config = new McpServerConfig().addPrompt(prompt("p", args -> new GetPromptResult().setDescription("ok")));
		var list = (ListPromptsResult) send(config, req(1, McpMethods.PROMPTS_LIST, null)).getResult();
		assertSize(1, list.getPrompts());
		var get = (GetPromptResult) send(config, req(1, McpMethods.PROMPTS_GET, JsonMap.of("name", "p"))).getResult();
		assertString("ok", get.getDescription());
	}

	@Test
	void prompts_get_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PROMPTS_GET, JsonMap.of()));
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void prompts_get_unknown_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PROMPTS_GET, JsonMap.of("name", "missing")));
		assertEquals(McpDispatcher.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	// -------- resources ---------

	@Test
	void resources_list_and_read() {
		var config = new McpServerConfig().addResource(resource("file://a", uri -> new ReadResourceResult().setContents(List.of(new TextResourceContents().setUri(uri).setText("ok")))));
		var list = (ListResourcesResult) send(config, req(1, McpMethods.RESOURCES_LIST, null)).getResult();
		assertSize(1, list.getResources());
		var read = (ReadResourceResult) send(config, req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "file://a"))).getResult();
		assertSize(1, read.getContents());
	}

	@Test
	void resources_read_missingUri_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.RESOURCES_READ, JsonMap.of()));
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void resources_read_unknown_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "ghost://")));
		assertEquals(McpDispatcher.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	// -------- pagination cursor passthrough ---------

	@Test
	void cursor_paramsAreOptional() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1)).addPrompt(prompt("a", args -> new GetPromptResult())).addPrompt(prompt("b", args -> new GetPromptResult()));
		// params null
		var resp = (ListPromptsResult) send(config, req(1, McpMethods.PROMPTS_LIST, null)).getResult();
		assertSize(1, resp.getPrompts());
		assertString("1", resp.getNextCursor());
	}

	// -------- Mcp facade ---------

	@Test
	void facade_dispatches() {
		var config = new McpServerConfig();
		var resp = Mcp.handle(req(1, McpMethods.PING, null), config, ctx);
		assertNotNull(resp);
	}
}
