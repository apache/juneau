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
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpPromptMessage;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceContents;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpRole;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code 2026-07-28} {@link McpRevision} prompt and resource operations, including the
 * distinct {@code PROMPT_NOT_FOUND} / {@code RESOURCE_NOT_FOUND} kinds that both map to {@code -32602}.
 */
class McpPromptResource_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpPromptHandler prompt(String name, Function<Map<String,Object>,McpPromptOutcome> fn) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpResourceHandler resource(String uri, Function<String,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return fn.apply(u); }
		};
	}

	private static Object validMeta() {
		return JsonMap.of(
			"protocolVersion", "2026-07-28",
			"clientInfo", JsonMap.of("name", "fixture-client", "version", "1.0"),
			"capabilities", JsonMap.of());
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params).setMeta(validMeta());
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

	// -------- prompts/list + prompts/get ---------

	@Test
	void a01_promptsList_singlePage() {
		var config = new McpServerConfig()
			.addPrompt(prompt("p", args -> new McpPromptOutcome()))
			.addPrompt(prompt("q", args -> new McpPromptOutcome()));
		var result = (ListPromptsResult) send(config, req(1, "prompts/list", null), hdrs("prompts/list", "")).getResult();
		assertEquals(2, result.getPrompts().size());
		assertNull(result.getNextCursor());
	}

	@Test
	void a02_promptsList_paged() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1))
			.addPrompt(prompt("p", args -> new McpPromptOutcome()))
			.addPrompt(prompt("q", args -> new McpPromptOutcome()));
		var first = (ListPromptsResult) send(config, req(1, "prompts/list", null), hdrs("prompts/list", "")).getResult();
		assertEquals(1, first.getPrompts().size());
		assertEquals("1", first.getNextCursor());
		var second = (ListPromptsResult) send(config, req(2, "prompts/list", JsonMap.of("cursor", "1")), hdrs("prompts/list", "")).getResult();
		assertEquals(1, second.getPrompts().size());
		assertNull(second.getNextCursor());
	}

	@Test
	void b01_promptsGet_success_mapsDescriptionAndNeutralContent() {
		var config = new McpServerConfig().addPrompt(prompt("greet", args -> new McpPromptOutcome()
			.setDescription("greeting")
			.setMessages(List.of(new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hello"))))));
		var result = (GetPromptResult) send(config, req(1, "prompts/get", JsonMap.of("name", "greet")), hdrs("prompts/get", "greet")).getResult();
		assertEquals("greeting", result.getDescription());
		var message = result.getMessages().get(0);
		assertEquals(Role.USER, message.getRole());
		assertEquals("hello", ((TextContent) message.getContent()).getText());
	}

	@Test
	void b02_promptsGet_missingName_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, "prompts/get", JsonMap.of()), hdrs("prompts/get", ""));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Missing prompt name", resp.getError().getMessage());
	}

	@Test
	void b03_promptsGet_missingPrompt_notFound_is32602() {
		var resp = send(new McpServerConfig(), req(1, "prompts/get", JsonMap.of("name", "ghost")), hdrs("prompts/get", "ghost"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Prompt not found: ghost", resp.getError().getMessage());
	}

	@Test
	void b04_promptsGet_nullMessages_omittedOnWire() {
		var config = new McpServerConfig().addPrompt(prompt("bare", args -> new McpPromptOutcome().setDescription("d")));
		var result = (GetPromptResult) send(config, req(1, "prompts/get", JsonMap.of("name", "bare")), hdrs("prompts/get", "bare")).getResult();
		assertNull(result.getMessages());
		assertFalse(Json.of(result).contains("messages"));
	}

	// -------- resources/list + resources/read ---------

	@Test
	void c01_resourcesList_singlePage() {
		var config = new McpServerConfig()
			.addResource(resource("file:///a", u -> new McpResourceOutcome()))
			.addResource(resource("file:///b", u -> new McpResourceOutcome()));
		var result = (ListResourcesResult) send(config, req(1, "resources/list", null), hdrs("resources/list", "")).getResult();
		assertEquals(2, result.getResources().size());
		assertNull(result.getNextCursor());
	}

	@Test
	void c02_resourcesList_paged() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1))
			.addResource(resource("file:///a", u -> new McpResourceOutcome()))
			.addResource(resource("file:///b", u -> new McpResourceOutcome()));
		var first = (ListResourcesResult) send(config, req(1, "resources/list", null), hdrs("resources/list", "")).getResult();
		assertEquals(1, first.getResources().size());
		assertEquals("1", first.getNextCursor());
		var second = (ListResourcesResult) send(config, req(2, "resources/list", JsonMap.of("cursor", "1")), hdrs("resources/list", "")).getResult();
		assertEquals(1, second.getResources().size());
		assertNull(second.getNextCursor());
	}

	@Test
	void d01_resourcesRead_success_mapsNeutralContents() {
		var config = new McpServerConfig().addResource(resource("file:///a", u -> new McpResourceOutcome()
			.setContents(List.of(McpResourceContents.text(u, "text/plain", "body")))));
		var result = (ReadResourceResult) send(config, req(1, "resources/read", JsonMap.of("uri", "file:///a")), hdrs("resources/read", "file:///a")).getResult();
		assertEquals(1, result.getContents().size());
		var contents = (TextResourceContents) result.getContents().get(0);
		assertEquals("body", contents.getText());
		assertEquals("file:///a", contents.getUri());
	}

	@Test
	void d02_resourcesRead_missingUri_invalidParams() {
		var resp = send(new McpServerConfig(), req(1, "resources/read", JsonMap.of()), hdrs("resources/read", ""));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Missing resource uri", resp.getError().getMessage());
	}

	@Test
	void d03_resourcesRead_missingResource_notFound_is32602() {
		var resp = send(new McpServerConfig(), req(1, "resources/read", JsonMap.of("uri", "ghost://x")), hdrs("resources/read", "ghost://x"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("Resource not found: ghost://x", resp.getError().getMessage());
	}

	@Test
	void d04_resourcesRead_nullContents_omittedOnWire() {
		var config = new McpServerConfig().addResource(resource("file:///empty", u -> new McpResourceOutcome()));
		var result = (ReadResourceResult) send(config, req(1, "resources/read", JsonMap.of("uri", "file:///empty")), hdrs("resources/read", "file:///empty")).getResult();
		assertNull(result.getContents());
		assertFalse(Json.of(result).contains("contents"));
	}
}
