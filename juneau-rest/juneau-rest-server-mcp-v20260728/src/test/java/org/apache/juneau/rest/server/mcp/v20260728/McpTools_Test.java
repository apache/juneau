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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code 2026-07-28} {@link McpRevision} {@code tools/list} and {@code tools/call}
 * operations, including bounded input-schema validation and the v2 {@code -32602} tool errors.
 */
class McpTools_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpToolHandler tool(String name, Function<Map<String,Object>,McpToolOutcome> fn) {
		return tool(name, null, fn);
	}

	private static McpToolHandler tool(String name, McpSchema schema, Function<Map<String,Object>,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name).setInputSchema(schema); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpToolOutcome text(String value) {
		return new McpToolOutcome().setContent(List.of(McpContentBlock.text(value)));
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		var p = params instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(p);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		m.put("Mcp-Method", method);
		m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r, Map<String,String> headers) {
		return new McpRevision(null).dispatch(new McpExchange(r, headers::get), config, ctx);
	}

	// -------- tools/list ---------

	@Test
	void a01_toolsList_singlePage() {
		var config = new McpServerConfig()
			.addTool(tool("a", args -> new McpToolOutcome()))
			.addTool(tool("b", args -> new McpToolOutcome()));
		var result = (ListToolsResult) send(config, req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		assertEquals(2, result.getTools().size());
		assertNull(result.getNextCursor());
	}

	@Test
	void a02_toolsList_paged() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1))
			.addTool(tool("a", args -> new McpToolOutcome()))
			.addTool(tool("b", args -> new McpToolOutcome()));
		var first = (ListToolsResult) send(config, req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		assertEquals(1, first.getTools().size());
		assertEquals("1", first.getNextCursor());
		var second = (ListToolsResult) send(config, req(2, "tools/list", JsonMap.of("cursor", "1")), hdrs("tools/list", "")).getResult();
		assertEquals(1, second.getTools().size());
		assertNull(second.getNextCursor());
	}

	@Test
	void a03_toolsList_convertsDescriptorSchemaToJsonSchema() {
		var schema = McpSchema.of(JsonMap.of(
			"type", "object",
			"properties", JsonMap.of("id", JsonMap.of("type", "string")),
			"required", List.of("id")));
		var config = new McpServerConfig().addTool(tool("described", schema, args -> new McpToolOutcome()));
		var result = (ListToolsResult) send(config, req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		var wireTool = result.getTools().get(0);
		assertNotNull(wireTool.getInputSchema());
		var json = Json.of(wireTool.getInputSchema());
		assertTrue(json.contains("\"type\":\"object\""), json);
		assertTrue(json.contains("\"id\""), json);
	}

	// -------- tools/call success paths ---------

	@Test
	void b01_toolsCall_success_routesByNameAndMapsContent() {
		var config = new McpServerConfig().addTool(tool("echo", args -> text(String.valueOf(args.get("text")))));
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"));
		var result = (CallToolResult) send(config, req(1, "tools/call", params), hdrs("tools/call", "echo")).getResult();
		assertEquals("hi", ((TextContent) result.getContent().get(0)).getText());
	}

	@Test
	void b02_toolsCall_absentArguments_handlerReceivesEmptyMap() {
		var seen = new AtomicReference<Map<String,Object>>();
		var config = new McpServerConfig().addTool(tool("noargs", args -> { seen.set(args); return new McpToolOutcome(); }));
		var resp = send(config, req(1, "tools/call", JsonMap.of("name", "noargs")), hdrs("tools/call", "noargs"));
		assertNull(resp.getError());
		assertNotNull(seen.get());
		assertTrue(seen.get().isEmpty());
	}

	@Test
	void b03_toolsCall_applicationErrorFlag_isCarried() {
		var config = new McpServerConfig().addTool(tool("fail", args -> new McpToolOutcome().setError(true).setContent(List.of(McpContentBlock.text("nope")))));
		var result = (CallToolResult) send(config, req(1, "tools/call", JsonMap.of("name", "fail")), hdrs("tools/call", "fail")).getResult();
		assertEquals(Boolean.TRUE, result.getIsError());
	}

	// -------- tools/call error paths ---------

	@Test
	void c01_toolsCall_paramsNotMap_invalidParams() {
		var config = new McpServerConfig().addTool(tool("echo", args -> new McpToolOutcome()));
		var resp = send(config, req(1, "tools/call", "not-a-map"), hdrs("tools/call", ""));
		assertEquals(-32602, resp.getError().getCode());
	}

	@Test
	void c02_toolsCall_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, "tools/call", JsonMap.of("arguments", JsonMap.of())), hdrs("tools/call", ""));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Missing tool name", resp.getError().getMessage());
	}

	@Test
	void c03_toolsCall_missingTool_notFound_is32602() {
		var resp = send(new McpServerConfig(), req(1, "tools/call", JsonMap.of("name", "ghost")), hdrs("tools/call", "ghost"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Tool not found: ghost", resp.getError().getMessage());
	}

	@Test
	void c04_toolsCall_handlerRuntimeFailure_internalError() {
		var config = new McpServerConfig().addTool(tool("boom", args -> { throw new RuntimeException("kaboom"); }));
		var resp = send(config, req(1, "tools/call", JsonMap.of("name", "boom")), hdrs("tools/call", "boom"));
		assertEquals(-32603, resp.getError().getCode());
		assertEquals("kaboom", resp.getError().getMessage());
	}

	// -------- input-schema validation runs before the handler ---------

	@Test
	void d01_schemaValidationFailure_leavesHandlerUninvoked() {
		var counter = new AtomicInteger();
		var schema = McpSchema.of(JsonMap.of("type", "object", "required", List.of("id"), "properties", JsonMap.of("id", JsonMap.of("type", "string"))));
		var config = new McpServerConfig().addTool(tool("strict", schema, args -> { counter.incrementAndGet(); return new McpToolOutcome(); }));
		var params = JsonMap.of("name", "strict", "arguments", JsonMap.of());
		var resp = send(config, req(1, "tools/call", params), hdrs("tools/call", "strict"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, counter.get());
	}

	@Test
	void d02_overDeepArguments_leaveHandlerUninvoked() {
		var counter = new AtomicInteger();
		var schema = McpSchema.of(new JsonMap());
		var config = new McpServerConfig().addTool(tool("deep", schema, args -> { counter.incrementAndGet(); return new McpToolOutcome(); }));
		Map<String,Object> node = new LinkedHashMap<>();
		for (var i = 1; i < 65; i++) {  // 65 levels of nesting -> exceeds MAX_DEPTH=64
			var parent = new LinkedHashMap<String,Object>();
			parent.put("child", node);
			node = parent;
		}
		var params = JsonMap.of("name", "deep", "arguments", node);
		var resp = send(config, req(1, "tools/call", params), hdrs("tools/call", "deep"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, counter.get());
	}
}
