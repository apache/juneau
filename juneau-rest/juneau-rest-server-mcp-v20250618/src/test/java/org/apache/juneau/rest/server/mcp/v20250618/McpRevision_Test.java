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
package org.apache.juneau.rest.server.mcp.v20250618;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpRevision}.
 */
class McpRevision_Test {

	private final BeanStore ctx = new BasicBeanStore();

	private static McpToolHandler tool(String name, java.util.function.Function<Map<String,Object>,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override
			public McpToolSpec descriptor() {
				return new McpToolSpec().setName(name).setDescription("desc:" + name);
			}

			@Override
			public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				return fn.apply(arguments);
			}
		};
	}

	private static McpPromptHandler prompt(String name, java.util.function.Function<Map<String,Object>,McpPromptOutcome> fn) {
		return new McpPromptHandler() {
			@Override
			public McpPromptSpec descriptor() {
				return new McpPromptSpec().setName(name);
			}

			@Override
			public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) {
				return fn.apply(arguments);
			}
		};
	}

	private static McpResourceHandler resource(String uri, java.util.function.Function<String,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override
			public McpResourceSpec descriptor() {
				return new McpResourceSpec().setUri(uri).setName("res");
			}

			@Override
			public McpResourceOutcome read(String u, BeanStore ctx) {
				return fn.apply(u);
			}
		};
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params);
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r) {
		var result = new McpRevision(null).dispatch(new McpExchange(r, n -> null), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	@Test
	void a01_initialize_default_capabilities() {
		var config = new McpServerConfig()
			.addTool(tool("a", a -> new McpToolOutcome()))
			.addPrompt(prompt("p", a -> new McpPromptOutcome()))
			.addResource(resource("r://x", u -> new McpResourceOutcome()));

		var resp = send(config, req(1, McpMethods.INITIALIZE, null));
		assertNotNull(resp);
		var result = (InitializeResult) resp.getResult();
		assertString(McpProtocol.VERSION_2025_06_18, result.getProtocolVersion());
		assertNotNull(result.getCapabilities().getTools());
		assertNotNull(result.getCapabilities().getPrompts());
		assertNotNull(result.getCapabilities().getResources());
		assertString(McpRevision.DEFAULT_SERVER_NAME, result.getServerInfo().getName());
	}

	@Test
	void a02_initialize_explicitServerInfoAndInstructions() {
		var config = new McpServerConfig().setName("custom").setVersion("9.9").setInstructions("hi");
		var resp = send(config, req(1, McpMethods.INITIALIZE, null));
		var result = (InitializeResult) resp.getResult();
		assertBean(result.getServerInfo(), "name,version", "custom,9.9");
		assertString("hi", result.getInstructions());
	}

	@Test
	void a03_ping_returns_emptyResult() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PING, null));
		assertTrue(resp.getResult() instanceof JsonMap);
	}

	@Test
	void a04_method_notFound() {
		var resp = send(new McpServerConfig(), req(1, "no/such/method", null));
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void a05_missing_method() {
		var resp = send(new McpServerConfig(), req(1, null, null));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void a06_empty_method_string() {
		var resp = send(new McpServerConfig(), req(1, "", null));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void a07_initialize_emptyConfig_synthesizesEmptyCapabilities() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.INITIALIZE, null));
		var result = (InitializeResult) resp.getResult();
		assertNotNull(result.getCapabilities());
		assertNull(result.getCapabilities().getTools());
		assertNull(result.getCapabilities().getPrompts());
		assertNull(result.getCapabilities().getResources());
	}

	@Test
	void b01_notification_runtimeException_returnsNullSilently() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new RuntimeException("boom");
		}));
		assertNull(send(config, req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "e"))));
	}

	@Test
	void b02_notification_mcpException_returnsNullSilently() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new McpException(-32000, "no");
		}));
		assertNull(send(config, req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "e"))));
	}

	@Test
	void b03_notification_returnsNullResponse() {
		var config = new McpServerConfig().addTool(tool("a", args -> new McpToolOutcome()));
		var notif = req(null, McpMethods.TOOLS_CALL, JsonMap.of("name", "a"));
		assertNull(send(config, notif));
	}

	@Test
	void b04_notification_methodNotFound_stillReturnsNull() {
		assertNull(send(new McpServerConfig(), req(null, "missing", null)));
	}

	@Test
	void b05_notification_invalidMethod_returnsNull() {
		assertNull(send(new McpServerConfig(), req(null, null, null)));
	}

	@Test
	void a08_nullEnvelope_returnsInvalidRequest() {
		var result = new McpRevision(null).dispatch(new McpExchange(null, n -> null), new McpServerConfig(), ctx);
		assertInstanceOf(McpResponseResult.class, result);
		assertEquals(McpRevision.CODE_INVALID_REQUEST, ((McpResponseResult) result).response().getError().getCode());
	}

	// -------- tools/list ---------

	@Test
	void c01_tools_list_singlePage() {
		var config = new McpServerConfig()
			.addTool(tool("a", args -> new McpToolOutcome()))
			.addTool(tool("b", args -> new McpToolOutcome()));
		var resp = send(config, req(1, McpMethods.TOOLS_LIST, null));
		var result = (ListToolsResult) resp.getResult();
		assertSize(2, result.getTools());
		assertNull(result.getNextCursor());
	}

	@Test
	void c02_tools_list_paged() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1))
			.addTool(tool("a", args -> new McpToolOutcome()))
			.addTool(tool("b", args -> new McpToolOutcome()));
		var first = (ListToolsResult) send(config, req(1, McpMethods.TOOLS_LIST, null)).getResult();
		assertSize(1, first.getTools());
		assertString("1", first.getNextCursor());
		var second = (ListToolsResult) send(config, req(2, McpMethods.TOOLS_LIST, JsonMap.of("cursor", "1"))).getResult();
		assertSize(1, second.getTools());
		assertNull(second.getNextCursor());
	}

	// -------- tools/call ---------

	@Test
	void d01_tools_call_routes_byName() {
		var config = new McpServerConfig().addTool(tool("echo", args -> {
			var outcome = new McpToolOutcome();
			outcome.setContent(List.of(McpContentBlock.text(String.valueOf(args.get("text")))));
			return outcome;
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"))));
		var ctr = (CallToolResult) resp.getResult();
		assertString("hi", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void d02_tools_call_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.TOOLS_CALL, JsonMap.of("arguments", JsonMap.of())));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void d03_tools_call_unknownTool_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "missing")));
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void d04_tools_call_argumentsNotObject_throwsInvalidParams() {
		var config = new McpServerConfig().addTool(tool("e", a -> new McpToolOutcome()));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e", "arguments", "string-not-map")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void d05_tools_call_paramsNotMap_invalidParams() {
		var config = new McpServerConfig().addTool(tool("e", a -> new McpToolOutcome()));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, "not-a-map"));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void d06_handler_throwingMcpException_propagatesCodeAndData() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new McpException(-32099, "nope", JsonMap.of("k", "v"));
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(-32099, resp.getError().getCode());
		assertString("nope", resp.getError().getMessage());
		assertNotNull(resp.getError().getData());
	}

	@Test
	void d07_handler_throwingRuntimeException_internalError() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new RuntimeException("boom");
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertString("boom", resp.getError().getMessage());
	}

	@Test
	void d08_handler_throwingRuntimeExceptionWithoutMessage_usesClassName() {
		var config = new McpServerConfig().addTool(tool("e", a -> {
			throw new IllegalStateException();
		}));
		var resp = send(config, req(1, McpMethods.TOOLS_CALL, JsonMap.of("name", "e")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertString("IllegalStateException", resp.getError().getMessage());
	}

	// -------- prompts ---------

	@Test
	void e01_prompts_list_and_get() {
		var config = new McpServerConfig().addPrompt(prompt("p", args -> new McpPromptOutcome().setDescription("ok")));
		var list = (ListPromptsResult) send(config, req(1, McpMethods.PROMPTS_LIST, null)).getResult();
		assertSize(1, list.getPrompts());
		var get = (GetPromptResult) send(config, req(1, McpMethods.PROMPTS_GET, JsonMap.of("name", "p"))).getResult();
		assertString("ok", get.getDescription());
	}

	@Test
	void e02_prompts_get_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PROMPTS_GET, JsonMap.of()));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void e03_prompts_get_unknown_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.PROMPTS_GET, JsonMap.of("name", "missing")));
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
	}

	// -------- resources ---------

	@Test
	void f01_resources_list_and_read() {
		var config = new McpServerConfig().addResource(resource("file://a", uri -> new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, null, "ok")))));
		var list = (ListResourcesResult) send(config, req(1, McpMethods.RESOURCES_LIST, null)).getResult();
		assertSize(1, list.getResources());
		var read = (ReadResourceResult) send(config, req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "file://a"))).getResult();
		assertSize(1, read.getContents());
	}

	@Test
	void f02_resources_read_missingUri_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.RESOURCES_READ, JsonMap.of()));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void f03_resources_read_unknown_resourceNotFound() {
		var resp = send(new McpServerConfig(), req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "ghost://")));
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test
	void f04_resources_read_fallsBackToRegisteredTemplateOnExactMiss() {
		// Full precedence/ranking/error-condition coverage for template-backed reads and the
		// resources/templates/list dispatch lives in McpResourceTemplate_Test; this is a single smoke test
		// proving the ordinary resources/read dispatch path also serves a registered template on an exact miss.
		var config = new McpServerConfig().addResourceTemplate(new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate("file:///items/{id}").setName("item");
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
				return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, null, "item-" + variables.get("id"))));
			}
		});
		var read = (ReadResourceResult) send(config, req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "file:///items/42"))).getResult();
		assertString("item-42", ((TextResourceContents) read.getContents().get(0)).getText());
	}

	// -------- pagination cursor passthrough ---------

	@Test
	void g01_cursor_paramsAreOptional() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1)).addPrompt(prompt("a", args -> new McpPromptOutcome())).addPrompt(prompt("b", args -> new McpPromptOutcome()));
		// params null
		var resp = (ListPromptsResult) send(config, req(1, McpMethods.PROMPTS_LIST, null)).getResult();
		assertSize(1, resp.getPrompts());
		assertString("1", resp.getNextCursor());
	}

	// -------- error code table ---------

	@Test
	void h01_errorCode_tableIsComplete() {
		var a = new McpRevision(null);
		assertEquals(-32600, a.errorCode(McpErrorKind.INVALID_REQUEST));
		assertEquals(-32601, a.errorCode(McpErrorKind.UNKNOWN_METHOD));
		assertEquals(-32601, a.errorCode(McpErrorKind.TOOL_NOT_FOUND), "known-wrong mapping, preserved deliberately");
		assertEquals(-32601, a.errorCode(McpErrorKind.PROMPT_NOT_FOUND), "known-wrong mapping, preserved deliberately");
		assertEquals(-32002, a.errorCode(McpErrorKind.RESOURCE_NOT_FOUND), "corrected per 2025-06-18 spec's missing-resource error code");
		assertEquals(-32602, a.errorCode(McpErrorKind.INVALID_PARAMS));
		assertEquals(-32603, a.errorCode(McpErrorKind.INTERNAL_ERROR));
		assertEquals(-32700, a.errorCode(McpErrorKind.PARSE_ERROR));
		assertEquals("2025-06-18", a.protocolVersion());
	}
}
