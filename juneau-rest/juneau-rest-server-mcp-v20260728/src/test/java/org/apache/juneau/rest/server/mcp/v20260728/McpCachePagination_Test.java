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

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpErrorKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpResourceTemplateSpec;
import org.apache.juneau.rest.server.mcp.McpResponseResult;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Proves that cache-hint precedence is atomic and static per list method: every page of a given
 * list resolves to identical {@code ttlMs}/{@code cacheScope} values regardless of cursor/page/content,
 * and that cache decoration is confined to the top-level result object.
 */
class McpCachePagination_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpToolHandler tool(String name) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return new McpToolOutcome(); }
		};
	}

	private static McpPromptHandler prompt(String name) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceHandler resource(String uri) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return new McpResourceOutcome(); }
		};
	}

	private static McpResourceTemplateSpec template(String name) {
		return new McpResourceTemplateSpec().setUriTemplate("file:///" + name + "/{x}").setName(name);
	}

	private static McpServerConfig fullTwoItemConfig() {
		return new McpServerConfig()
			.addTool(tool("t1"), tool("t2"))
			.addPrompt(prompt("p1"), prompt("p2"))
			.addResource(resource("file:///a"), resource("file:///b"))
			.addResourceTemplate(template("r1"), template("r2"));
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

	private JsonRpcResponse send(McpServerConfig config, McpCacheConfig cache, String method, Object params) {
		var result = new McpRevision(null, cache).dispatch(new McpExchange(req(1, method, params), hdrs(method, "")::get), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	private static McpCacheConfig endpointHint(String method, McpCacheHint hint) {
		var cache = new McpCacheConfig();
		return switch (method) {
			case McpMethods.TOOLS_LIST -> cache.setToolsList(hint);
			case McpMethods.PROMPTS_LIST -> cache.setPromptsList(hint);
			case McpMethods.RESOURCES_LIST -> cache.setResourcesList(hint);
			case McpMethods.RESOURCES_TEMPLATES_LIST -> cache.setResourceTemplatesList(hint);
			default -> throw new IllegalArgumentException(method);
		};
	}

	private static CacheableResult<?> result(JsonRpcResponse response) {
		return (CacheableResult<?>) response.getResult();
	}

	private static String nextCursor(CacheableResult<?> result) {
		if (result instanceof ListToolsResult r)
			return r.getNextCursor();
		if (result instanceof ListPromptsResult r)
			return r.getNextCursor();
		if (result instanceof ListResourcesResult r)
			return r.getNextCursor();
		if (result instanceof ListResourceTemplatesResult r)
			return r.getNextCursor();
		throw new IllegalArgumentException(String.valueOf(result));
	}

	private static Object firstItem(CacheableResult<?> result) {
		if (result instanceof ListToolsResult r)
			return r.getTools().get(0);
		if (result instanceof ListPromptsResult r)
			return r.getPrompts().get(0);
		if (result instanceof ListResourcesResult r)
			return r.getResources().get(0);
		if (result instanceof ListResourceTemplatesResult r)
			return r.getResourceTemplates().get(0);
		throw new IllegalArgumentException(String.valueOf(result));
	}

	@ParameterizedTest
	@ValueSource(strings = {"tools/list", "prompts/list", "resources/list", "resources/templates/list"})
	void a01_bothPagesCarryIdenticalStaticHint(String method) {
		var config = fullTwoItemConfig().setCursor(McpCursor.fixedSize(1));
		var cache = endpointHint(method, new McpCacheHint().setTtlMs(41).setCacheScope(McpCacheScope.PRIVATE));
		var first = result(send(config, cache, method, null));
		var second = result(send(config, cache, method, JsonMap.of("cursor", "1")));
		assertEquals(41, first.getTtlMs());
		assertEquals(first.getTtlMs(), second.getTtlMs());
		assertEquals(McpCacheScope.PRIVATE, first.getCacheScope());
		assertEquals(first.getCacheScope(), second.getCacheScope());
		assertNotEquals(nextCursor(first), nextCursor(second));
		assertNotEquals(firstItem(first), firstItem(second));
	}

	@ParameterizedTest
	@ValueSource(strings = {"tools/list", "prompts/list", "resources/list", "resources/templates/list"})
	void a02_absenceIsAlsoIdenticalAcrossPages(String method) {
		var config = fullTwoItemConfig().setCursor(McpCursor.fixedSize(1));
		var cache = new McpCacheConfig();
		var first = result(send(config, cache, method, null));
		var second = result(send(config, cache, method, JsonMap.of("cursor", "1")));
		assertNull(first.getTtlMs());
		assertNull(second.getTtlMs());
		assertNull(first.getCacheScope());
		assertNull(second.getCacheScope());
	}

	@Test void b01_cacheFieldsAreTopLevelOnly() {
		var config = fullTwoItemConfig();
		var cache = endpointHint("tools/list", new McpCacheHint().setTtlMs(41));
		var result = result(send(config, cache, "tools/list", null));
		var json = Json.of(result);
		assertTrue(json.contains("\"ttlMs\":41"), json);
		assertFalse(json.contains("Cache-Control"));
		assertFalse(json.contains("\"cache\":{\""));
		// Cache hints are direct result properties, not nested inside the unrelated result._meta carrier
		// (which every success result now carries per common result finalization).
		assertNotNull(result.getMeta());
		assertFalse(Json.of(result.getMeta()).contains("ttlMs"));
	}

	@Test void b02_errorKindNamesUnchangedByC2() {
		var names = Arrays.stream(McpErrorKind.values()).map(Enum::name).toList();
		assertEquals(List.of("INVALID_REQUEST", "UNKNOWN_METHOD", "TOOL_NOT_FOUND", "PROMPT_NOT_FOUND",
			"RESOURCE_NOT_FOUND", "INVALID_PARAMS", "INTERNAL_ERROR", "PARSE_ERROR"), names);
	}
}
