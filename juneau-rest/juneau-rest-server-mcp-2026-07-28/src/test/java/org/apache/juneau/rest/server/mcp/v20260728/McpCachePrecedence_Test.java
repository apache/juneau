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
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Coverage for {@link McpRevision}'s atomic cache-hint precedence on the list and read dispatch paths.
 */
class McpCachePrecedence_Test {

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

	private JsonRpcResponse send(McpServerConfig config, McpCacheConfig cache, JsonRpcRequest r, Map<String,String> headers) {
		return new McpRevision(null, cache).dispatch(new McpExchange(r, headers::get), config, ctx);
	}

	// -------- list precedence ---------

	private static McpServerConfig configFor(String method) {
		return new McpServerConfig()
			.addTool(tool("t"))
			.addPrompt(prompt("p"))
			.addResource(resource("file:///a"));
	}

	private static McpCacheConfig configuredFor(String method, McpCacheHint endpointHint, McpCacheHint defaultHint) {
		var cache = new McpCacheConfig().setDefaultHint(defaultHint);
		return switch (method) {
			case "tools/list" -> cache.setToolsList(endpointHint);
			case "prompts/list" -> cache.setPromptsList(endpointHint);
			case "resources/list" -> cache.setResourcesList(endpointHint);
			default -> throw new IllegalArgumentException(method);
		};
	}

	@ParameterizedTest
	@CsvSource({
		"tools/list,11", "prompts/list,12", "resources/list,13"
	})
	void a01_endpointOverridesDefault(String method, int ttl) {
		var cache = configuredFor(method, new McpCacheHint().setTtlMs(ttl),
			new McpCacheHint().setTtlMs(99).setCacheScope(McpCacheScope.PRIVATE));
		var result = (CacheableResult<?>)send(configFor(method), cache, req(1, method, null), hdrs(method, "")).getResult();
		assertEquals(ttl, result.getTtlMs());
		assertNull(result.getCacheScope());
	}

	@ParameterizedTest
	@ValueSource(strings = {"tools/list", "prompts/list", "resources/list"})
	void a02_defaultThenNone(String method) {
		var withDefault = (CacheableResult<?>)send(configFor(method),
			new McpCacheConfig().setDefaultHint(new McpCacheHint().setTtlMs(8).setCacheScope(McpCacheScope.PUBLIC)),
			req(1, method, null), hdrs(method, "")).getResult();
		assertEquals(8, withDefault.getTtlMs());
		assertEquals(McpCacheScope.PUBLIC, withDefault.getCacheScope());
		var none = (CacheableResult<?>)send(configFor(method), new McpCacheConfig(),
			req(2, method, null), hdrs(method, "")).getResult();
		assertNull(none.getTtlMs());
		assertNull(none.getCacheScope());
	}

	@Test void a03_emptyEndpointSuppressesDefaultWithoutMerge() {
		var cache = new McpCacheConfig()
			.setDefaultHint(new McpCacheHint().setTtlMs(99).setCacheScope(McpCacheScope.PRIVATE))
			.setToolsList(new McpCacheHint());
		var result = (ListToolsResult)send(configFor("tools/list"), cache,
			req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		assertNull(result.getTtlMs());
		assertNull(result.getCacheScope());
		assertFalse(Json.of(result).contains("cache"));
		assertFalse(Json.of(result).contains("ttlMs"));
	}

	// -------- read precedence ---------

	private static McpCacheConfig cache() {
		return new McpCacheConfig();
	}

	private static McpCacheHint hint(int ttl) {
		return new McpCacheHint().setTtlMs(ttl);
	}

	private ReadResourceResult read(McpCacheConfig cacheConfig, String uri) {
		var config = new McpServerConfig().addResource(resource(uri));
		return (ReadResourceResult)send(config, cacheConfig, req(1, "resources/read", JsonMap.of("uri", uri)), hdrs("resources/read", uri)).getResult();
	}

	private void assertReadTtl(McpCacheConfig cacheConfig, String uri, Integer expectedTtl) {
		assertEquals(expectedTtl, read(cacheConfig, uri).getTtlMs());
	}

	@Test void b01_readOverrideThenEndpointThenDefaultThenNone() {
		assertReadTtl(cache().addResourceReadOverride("file:///a", hint(1)).setResourcesRead(hint(2)).setDefaultHint(hint(3)), "file:///a", 1);
		assertReadTtl(cache().addResourceReadOverride("file:///a", null).setResourcesRead(hint(2)).setDefaultHint(hint(3)), "file:///a", 2);
		assertReadTtl(cache().setDefaultHint(hint(3)), "file:///a", 3);
		assertReadTtl(cache(), "file:///a", null);
	}

	@Test void b02_readMatchingIsExactWithNoNormalizationOrTemplates() {
		var cache = cache().addResourceReadOverride("file:///a", hint(1))
			.addResourceReadOverride("file:///{name}", hint(2)).setResourcesRead(hint(3));
		assertReadTtl(cache, "file:///A", 3);
		assertReadTtl(cache, "file:///a/", 3);
		assertReadTtl(cache, "file:///x", 3);
		assertReadTtl(cache, "file:///a", 1);
	}

	@Test void b03_readPartialOverrideDoesNotMerge() {
		var cache = cache().addResourceReadOverride("file:///a", new McpCacheHint().setTtlMs(5))
			.setResourcesRead(new McpCacheHint().setCacheScope(McpCacheScope.PRIVATE));
		var result = read(cache, "file:///a");
		assertEquals(5, result.getTtlMs());
		assertNull(result.getCacheScope());
	}
}
